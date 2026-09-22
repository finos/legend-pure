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

import * as fs from 'fs';

/**
 * Optional JSON sidecar describing how to reach (or launch) the LSP daemon — see
 * `legendPure.server.configPath` in package.json for the file format and examples.
 *
 * `repoRoots` only takes effect for the first client to reach the daemon: the server latches its
 * roots once and ignores every later client's.
 */
export interface PureLspServerConfig {
    port: number;
    repoRoots: string[];
    pureOptions: Record<string, string>;
    debugExecutionMode?: string;
    connectOnly: boolean;
    autoSyncWorkspace?: boolean;
    classpathRepositories: string[];
}

const EMPTY: PureLspServerConfig = {
    port: 0,
    repoRoots: [],
    pureOptions: {},
    connectOnly: false,
    classpathRepositories: [],
};

/**
 * Returns undefined (and logs why) if `path` is blank, missing, unreadable, or not valid JSON —
 * a broken sidecar must degrade to "no sidecar" rather than breaking activation outright.
 */
export function loadServerConfig(path: string): PureLspServerConfig | undefined {
    if (!path || !path.trim()) {
        return undefined;
    }
    let raw: string;
    try {
        if (!fs.existsSync(path) || !fs.statSync(path).isFile()) {
            console.log(`[Legend Pure] LSP config not found: ${path}`);
            return undefined;
        }
        raw = fs.readFileSync(path, { encoding: 'utf8' });
    } catch (e: any) {
        console.log(`[Legend Pure] Could not read LSP config ${path}: ${e?.message || e}`);
        return undefined;
    }

    let parsed: any;
    try {
        parsed = JSON.parse(raw);
    } catch (e: any) {
        console.log(`[Legend Pure] LSP config is not valid JSON (${path}): ${e?.message || e}`);
        return undefined;
    }
    if (!parsed || typeof parsed !== 'object') {
        console.log(`[Legend Pure] LSP config is empty: ${path}`);
        return undefined;
    }

    return {
        ...EMPTY,
        port: typeof parsed.port === 'number' ? parsed.port : 0,
        repoRoots: toStringArray(parsed.repoRoots),
        pureOptions: toStringMap(parsed.pureOptions),
        debugExecutionMode:
            typeof parsed.debugExecutionMode === 'string' ? parsed.debugExecutionMode : undefined,
        connectOnly: parsed.connectOnly === true,
        autoSyncWorkspace:
            typeof parsed.autoSyncWorkspace === 'boolean' ? parsed.autoSyncWorkspace : undefined,
        classpathRepositories: toStringArray(parsed.classpathRepositories),
    };
}

function toStringArray(value: unknown): string[] {
    if (!Array.isArray(value)) {
        return [];
    }
    return value
        .filter((entry): entry is string => typeof entry === 'string')
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);
}

function toStringMap(value: unknown): Record<string, string> {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        return {};
    }
    const result: Record<string, string> = {};
    for (const [key, raw] of Object.entries(value as Record<string, unknown>)) {
        if (raw !== null && raw !== undefined) {
            result[key] = String(raw);
        }
    }
    return result;
}
