// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import * as net from 'net';
import { ChildProcess, spawn } from 'child_process';
import { StreamInfo } from 'vscode-languageclient/node';
import { PureLspServerConfig } from './serverConfig';

/** The server only binds loopback in socket mode. */
export const LOOPBACK = '127.0.0.1';

/** Stderr marker the server prints once its socket is bound. */
const SOCKET_READY_MARKER = 'SOCKET_READY';

/** Preflight connect timeout: long enough for a loopback probe, short enough not to stall startup. */
export const PROBE_TIMEOUT_MS = 300;

export interface TransportSettings {
    connectOnly: boolean;
    connectPort: number;
    launchPort: number;
}

export type TransportPlan =
    /** Attach to a daemon whose lifecycle we must never own. Never probes, never spawns. */
    | { kind: 'connect'; port: number; reason: string }
    /** Socket mode we may own: probe first, attach if something is already listening. */
    | { kind: 'socket'; port: number; reason: string }
    /** Classic one-child-process-per-window stdio. The default when nothing is configured. */
    | { kind: 'stdio'; reason: string };

/** Precedence: connectOnly > launchPort > sidecar port > stdio. Pure, for unit testing. */
export function planTransport(
    settings: TransportSettings,
    config: PureLspServerConfig | undefined
): TransportPlan {
    if (settings.connectOnly && settings.connectPort > 0) {
        return {
            kind: 'connect',
            port: settings.connectPort,
            reason: `settings connectOnly=true, port ${settings.connectPort}`,
        };
    }
    if (settings.launchPort > 0) {
        return {
            kind: 'socket',
            port: settings.launchPort,
            reason: `settings launchPort ${settings.launchPort}`,
        };
    }
    if (!config || config.port <= 0) {
        return { kind: 'stdio', reason: 'no socket port configured' };
    }
    if (config.connectOnly) {
        return {
            kind: 'connect',
            port: config.port,
            reason: `config connectOnly=true, port ${config.port}`,
        };
    }
    return { kind: 'socket', port: config.port, reason: `config port ${config.port}` };
}

/** True if a daemon is already listening on `port`. */
export function probePort(port: number, timeoutMs = PROBE_TIMEOUT_MS): Promise<boolean> {
    return new Promise((resolve) => {
        const socket = new net.Socket();
        let settled = false;
        const finish = (listening: boolean) => {
            if (settled) {
                return;
            }
            settled = true;
            socket.destroy();
            resolve(listening);
        };
        socket.setTimeout(timeoutMs);
        socket.once('connect', () => finish(true));
        socket.once('timeout', () => finish(false));
        socket.once('error', () => finish(false));
        socket.connect(port, LOOPBACK);
    });
}

/** Opens the JSON-RPC duplex the LanguageClient will speak over. */
export function connectStream(port: number): Promise<StreamInfo> {
    return new Promise((resolve, reject) => {
        const socket = new net.Socket();
        const onError = (e: Error) => {
            socket.destroy();
            reject(
                new Error(
                    `Could not connect to the Legend Pure LSP daemon on ${LOOPBACK}:${port} ` +
                        `(${e.message}). Start one first, or clear ` +
                        `"legendPure.server.connectOnly" to let the extension launch its own.`
                )
            );
        };
        socket.once('error', onError);
        socket.connect(port, LOOPBACK, () => {
            socket.removeListener('error', onError);
            socket.setNoDelay(true);
            resolve({ reader: socket, writer: socket });
        });
    });
}

export interface SpawnSpec {
    javaExe: string;
    /** Path to the `@argfile` holding -D args, -cp, the main class and `--socket <port>`. */
    argFile: string;
    port: number;
    /** Receives the daemon's stderr line by line, for the server-log output channel. */
    onStderrLine?: (line: string) => void;
    readyTimeoutMs?: number;
}

export interface SpawnedDaemon {
    process: ChildProcess;
    stream: StreamInfo;
}

/**
 * Launches a socket-mode daemon and connects once SOCKET_READY appears on stderr — a connect-loop
 * would mask a slow classpath scan or a crash as a plain timeout.
 */
export function spawnSocketDaemon(spec: SpawnSpec): Promise<SpawnedDaemon> {
    const readyTimeoutMs = spec.readyTimeoutMs ?? 600_000;
    const child = spawn(spec.javaExe, [`@${spec.argFile}`], {
        stdio: ['ignore', 'pipe', 'pipe'],
    });

    return new Promise<SpawnedDaemon>((resolve, reject) => {
        let settled = false;
        const recentStderr: string[] = [];
        let pending = '';

        const timer = setTimeout(() => {
            fail(
                new Error(
                    `Legend Pure LSP daemon did not report ${SOCKET_READY_MARKER} within ` +
                        `${Math.round(readyTimeoutMs / 1000)}s`
                )
            );
        }, readyTimeoutMs);

        const cleanup = () => {
            clearTimeout(timer);
            child.stderr?.removeListener('data', onStderr);
            child.removeListener('exit', onExit);
            child.removeListener('error', fail);
        };

        const fail = (e: Error) => {
            if (settled) {
                return;
            }
            settled = true;
            cleanup();
            try {
                child.kill();
            } catch {
                // already gone
            }
            const tail = recentStderr.slice(-20).join('\n');
            reject(new Error(tail ? `${e.message}\n${tail}` : e.message));
        };

        const onExit = (code: number | null, signal: string | null) => {
            fail(
                new Error(
                    `Legend Pure LSP daemon exited before it was ready ` +
                        `(code ${code ?? 'null'}, signal ${signal ?? 'null'})`
                )
            );
        };

        const onReady = () => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timer);
            child.removeListener('exit', onExit);
            // stderr stays attached: it is the daemon's log, surfaced via onStderrLine.
            connectStream(spec.port).then(
                (stream) => resolve({ process: child, stream }),
                (e) => {
                    try {
                        child.kill();
                    } catch {
                        // already gone
                    }
                    reject(e);
                }
            );
        };

        const onStderr = (chunk: Buffer) => {
            pending += chunk.toString('utf8');
            const lines = pending.split(/\r?\n/);
            pending = lines.pop() ?? '';
            for (const line of lines) {
                recentStderr.push(line);
                if (recentStderr.length > 200) {
                    recentStderr.shift();
                }
                spec.onStderrLine?.(line);
                if (!settled && line.includes(SOCKET_READY_MARKER)) {
                    onReady();
                }
            }
        };

        child.stderr?.on('data', onStderr);
        child.once('exit', onExit);
        child.once('error', fail);
    });
}
