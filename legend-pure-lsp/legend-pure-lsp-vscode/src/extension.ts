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

import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import * as vscode from 'vscode';
import { ChildProcess } from 'child_process';
import { workspace, ExtensionContext, Uri, commands, window } from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';
import { createLegendPureDebugAdapterDescriptor, LegendPureDebugConfigurationProvider } from './debugAdapter';
import { PureFileSystemProvider } from './pureFileSystemProvider';
import { PurePackageTreeProvider } from './purePackageTree';
import { PureLanguageClient } from './pureLanguageClient';
import { loadServerConfig, PureLspServerConfig } from './serverConfig';
import { resolveRepoRoots } from './repoRoots';
import {
    planTransport,
    probePort,
    connectStream,
    spawnSocketDaemon,
    TransportPlan,
} from './serverTransport';
import { ServerLogChannel, WorkspaceDriftHandler } from './notifications';
import { PureStatusBar } from './statusBar';
import { manageOptions } from './options';
import { PureCodeLensProvider } from './codeLens';
import { buildServerArgFileContent, toJavaArgFileReference } from './serverArgs';
import {
    LegendLogEvent,
    LockContentionEvent,
    LspStatus,
    NOTIF_LOCK_CONTENTION,
    NOTIF_LOG_OUTPUT,
    NOTIF_STATUS_CHANGED,
    NOTIF_WORKSPACE_DRIFT,
    PCTAdapterInfo,
    SetupTeardownInfo,
    WorkspaceDriftEvent,
    executeGo as requestExecuteGo,
    executeFunction as requestExecuteFunction,
    getPCTAdapters,
    getSetupTeardown,
    status as requestStatus,
} from './protocol';

let client: LanguageClient | undefined;
let clientSubscriptions: vscode.Disposable[] = [];
/** Set only when this window spawned the daemon; only then may stopping kill it. */
let ownedProcess: ChildProcess | undefined;
/** True when attached to a daemon this window did not start. */
let externallyOwned = false;
let pureFs: PureFileSystemProvider | undefined;
let packageTree: PurePackageTreeProvider | undefined;
let serverLog: ServerLogChannel | undefined;
let driftHandler: WorkspaceDriftHandler | undefined;
let statusBar: PureStatusBar | undefined;
let codeLensProvider: PureCodeLensProvider | undefined;
let goOutputChannel: vscode.OutputChannel | undefined;
let starting: Promise<void> | undefined;

let serverReady: Promise<void>;
let resolveServerReady: () => void;
resetServerReady();

export function activate(context: ExtensionContext): void {
    serverLog = new ServerLogChannel();
    context.subscriptions.push(serverLog);

    statusBar = new PureStatusBar(() => client);
    context.subscriptions.push(statusBar);

    driftHandler = new WorkspaceDriftHandler(
        () => client,
        () => resolveAutoSync(),
        serverLog
    );
    context.subscriptions.push(driftHandler);

    pureFs = new PureFileSystemProvider(() => client);
    context.subscriptions.push(
        workspace.registerFileSystemProvider('pure', pureFs, {
            isReadonly: true,
            isCaseSensitive: true,
        })
    );
    context.subscriptions.push(pureFs);

    packageTree = new PurePackageTreeProvider(() => client);
    context.subscriptions.push(
        window.createTreeView('purePackageTree', {
            treeDataProvider: packageTree,
            showCollapseAll: true,
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.executeGo', async () => {
            const active = await requireClient();
            if (!active) {
                return;
            }
            if (!goOutputChannel) {
                goOutputChannel = window.createOutputChannel('Pure Go');
            }
            const out = goOutputChannel;
            out.clear();
            out.show(true);
            out.appendLine('Executing go()...');

            try {
                const result = await requestExecuteGo(active);
                if (result.success) {
                    out.appendLine(result.output || '(no output)');
                    out.appendLine('\n--- Execution complete ---');
                } else {
                    out.appendLine(result.output || result.error || 'Unknown error');
                }
            } catch (e: any) {
                out.appendLine('ERROR: ' + (e.message || e));
            }
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.startOrConnect', async () => {
            if (client) {
                const status = await currentState();
                // Avoid churning a healthy shared session for every other connected client.
                if (UP_OR_STARTING.has(status)) {
                    window.showInformationMessage(
                        `Legend Pure LSP is already ${status}.`
                    );
                    return;
                }
            }
            await startLegendClient(context);
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.stopOrDisconnect', async () => {
            if (!client) {
                window.showInformationMessage('Legend Pure LSP is not running.');
                return;
            }
            const wasExternal = externallyOwned;
            await stopLegendClient();
            window.showInformationMessage(
                wasExternal
                    ? 'Disconnected from the Legend Pure LSP daemon. It keeps running for other clients.'
                    : 'Legend Pure LSP stopped.'
            );
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.restartServer', async () => {
            await stopLegendClient();
            await startLegendClient(context);
            if (client) {
                window.showInformationMessage('Legend Pure LSP restarted.');
            }
        })
    );

    // Do not register 'legend.reindexWorkspace' here — it is a SERVER_OWNED_COMMAND (see protocol.ts).

    context.subscriptions.push(
        commands.registerCommand('legend.manageOptions', async () => {
            const active = await requireClient();
            if (active) {
                await manageOptions(context, active);
            }
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.showServerLog', () => serverLog?.show())
    );

    codeLensProvider = new PureCodeLensProvider(() => client);
    context.subscriptions.push(codeLensProvider);
    context.subscriptions.push(
        vscode.languages.registerCodeLensProvider(
            [
                { scheme: 'file', language: 'pure' },
                { scheme: 'pure', language: 'pure' },
            ],
            codeLensProvider
        )
    );

    // CodeLens-only commands; not in package.json since they need a function path argument.
    context.subscriptions.push(
        commands.registerCommand('legend.runFunction', (functionPath: string) =>
            runPureFunction(functionPath, {})
        )
    );

    context.subscriptions.push(
        commands.registerCommand('legend.debugFunction', async (functionPath: string) => {
            await vscode.debug.startDebugging(undefined, {
                type: 'legend-pure',
                request: 'launch',
                name: `Debug ${functionPath}`,
                function: functionPath,
            });
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.runFunctionWithAdapter', async (functionPath: string) => {
            const active = await requireClient();
            if (!active) {
                return;
            }
            let adapters: PCTAdapterInfo[];
            try {
                adapters = await getPCTAdapters(active);
            } catch (e: any) {
                window.showErrorMessage(`Legend Pure: could not list PCT adapters: ${e?.message || e}`);
                return;
            }
            if (!adapters || adapters.length === 0) {
                window.showWarningMessage('Legend Pure: no PCT adapters are available in this session.');
                return;
            }
            const picked = await window.showQuickPick(
                adapters.map((adapter) => ({
                    label: adapter.name,
                    description: adapter.path,
                    adapter,
                })),
                { title: `Run ${functionPath} against which PCT adapter?` }
            );
            if (!picked) {
                return;
            }
            await runPureFunction(functionPath, { pctAdapterPath: picked.adapter.path });
        })
    );

    context.subscriptions.push(
        commands.registerCommand(
            'legend.runFunctionWithSetupTeardown',
            async (functionPath: string) => {
                const active = await requireClient();
                if (!active) {
                    return;
                }
                let bracket: SetupTeardownInfo;
                try {
                    bracket = await getSetupTeardown(active, functionPath);
                } catch (e: any) {
                    window.showErrorMessage(
                        `Legend Pure: could not resolve setup/teardown: ${e?.message || e}`
                    );
                    return;
                }
                if (!bracket?.beforeFunctionPath && !bracket?.afterFunctionPath) {
                    window.showWarningMessage(
                        `Legend Pure: no <<test.BeforePackage>>/<<test.AfterPackage>> function ` +
                            `applies to ${functionPath}.`
                    );
                    return;
                }
                await runPureFunction(functionPath, {
                    beforeFunctionPath: bracket.beforeFunctionPath ?? undefined,
                    afterFunctionPath: bracket.afterFunctionPath ?? undefined,
                });
            }
        )
    );

    context.subscriptions.push(
        commands.registerCommand('legend.showStatus', async () => {
            const active = client;
            if (!active) {
                await commands.executeCommand('legend.startOrConnect');
                return;
            }
            try {
                const s: LspStatus = await requestStatus(active);
                const lines = [
                    `State: ${s.state}`,
                    `Workspace repos: ${s.repositoryCount}, symbols: ${s.symbolCount}`,
                    `Transport: ${s.transport ?? 'unknown'}${s.port && s.port > 0 ? ` :${s.port}` : ''}`,
                    `Connected clients: ${s.connectedClientCount ?? 'unknown'}`,
                    `Ownership: ${externallyOwned ? 'attached (external)' : 'owned by this window'}`,
                ];
                const choice = await window.showInformationMessage(
                    lines.join('  |  '),
                    'Show Server Log'
                );
                if (choice === 'Show Server Log') {
                    serverLog?.show();
                }
            } catch (e: any) {
                window.showErrorMessage(`Legend Pure: could not read status: ${e?.message || e}`);
            }
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.setServerJarPath', async () => {
            const config = workspace.getConfiguration('legendPure');
            const configuredPath = config.get<string>('server.jarPath') || '';
            const expandedPath = configuredPath ? expandConfiguredPath(configuredPath) : undefined;
            const defaultUri = expandedPath && fs.existsSync(expandedPath)
                ? Uri.file(path.dirname(expandedPath))
                : workspace.workspaceFolders?.[0]?.uri;

            const selected = await window.showOpenDialog({
                title: 'Select Legend Pure LSP Server JAR',
                defaultUri,
                canSelectFiles: true,
                canSelectFolders: false,
                canSelectMany: false,
                filters: {
                    'JAR files': ['jar'],
                },
            });

            const jarUri = selected?.[0];
            if (!jarUri) {
                return;
            }

            const target = workspace.workspaceFolders
                ? vscode.ConfigurationTarget.Workspace
                : vscode.ConfigurationTarget.Global;
            await config.update('server.jarPath', jarUri.fsPath, target);

            const reload = 'Restart LSP';
            const choice = await window.showInformationMessage(
                'Legend Pure LSP server JAR path updated. Restart the LSP to use this JAR.',
                reload
            );
            if (choice === reload) {
                await commands.executeCommand('legend.restartServer');
            }
        })
    );

    context.subscriptions.push(
        commands.registerCommand('legend.addServerClasspathEntry', async () => {
            const selected = await window.showOpenDialog({
                title: 'Select Legend Pure LSP Server Classpath Entries',
                defaultUri: workspace.workspaceFolders?.[0]?.uri,
                canSelectFiles: true,
                canSelectFolders: true,
                canSelectMany: true,
                filters: {
                    'JAR files': ['jar'],
                },
            });

            if (!selected || selected.length === 0) {
                return;
            }

            const config = workspace.getConfiguration('legendPure');
            const existing = getConfiguredStringArray('server.extraClasspath');
            const next = uniqueStrings(existing.concat(selected.map((uri) => uri.fsPath)));
            const target = workspace.workspaceFolders
                ? vscode.ConfigurationTarget.Workspace
                : vscode.ConfigurationTarget.Global;
            await config.update('server.extraClasspath', next, target);

            const reload = 'Restart LSP';
            const choice = await window.showInformationMessage(
                'Legend Pure LSP server classpath updated. Restart the LSP to use this classpath.',
                reload
            );
            if (choice === reload) {
                await commands.executeCommand('legend.restartServer');
            }
        })
    );

    context.subscriptions.push(
        vscode.debug.registerDebugConfigurationProvider(
            'legend-pure',
            new LegendPureDebugConfigurationProvider()
        )
    );
    context.subscriptions.push(
        vscode.debug.registerDebugAdapterDescriptorFactory('legend-pure', {
            createDebugAdapterDescriptor: () => createLegendPureDebugAdapterDescriptor(() => client, serverReady),
        })
    );

    registerLanguageModelTools(context);

    context.subscriptions.push(
        commands.registerCommand('legend.refreshPackageTree', () => refreshPureViews())
    );

    // Off by default: starting can mean spawning a large JVM, which should be opt-in.
    if (workspace.getConfiguration('legendPure').get<boolean>('server.autoStart', false)) {
        startLegendClient(context).catch((e) => {
            window.showErrorMessage('Legend Pure LSP failed to start: ' + (e?.message || e));
        });
    }
}

export async function deactivate(): Promise<void> {
    await stopLegendClient();
}

// ── Client lifecycle ───────────────────────────────────────────────

async function startLegendClient(context: ExtensionContext): Promise<void> {
    if (starting) {
        return starting;
    }
    if (client) {
        return;
    }
    starting = doStartLegendClient(context).finally(() => {
        starting = undefined;
    });
    return starting;
}

async function doStartLegendClient(context: ExtensionContext): Promise<void> {
    const settings = readTransportSettings();
    const config = loadServerConfig(
        settings.configPath ? expandConfiguredPath(settings.configPath) : ''
    );
    const plan = planTransport(settings, config);

    let serverOptions: ServerOptions | undefined;
    externallyOwned = false;

    if (plan.kind === 'stdio') {
        console.log(`[Legend Pure] Transport: stdio (${plan.reason})`);
        serverOptions = resolveStdioServerOptions(context, config);
    } else {
        const listening = await probePort(plan.port);
        if (plan.kind === 'connect') {
            if (!listening) {
                window.showErrorMessage(
                    `Legend Pure LSP: nothing is listening on 127.0.0.1:${plan.port} and ` +
                        '"legendPure.server.connectOnly" is set, so the extension will not start one. ' +
                        'Start the daemon first.'
                );
                return;
            }
            externallyOwned = true;
            console.log(`[Legend Pure] Transport: connect-only to :${plan.port} (${plan.reason})`);
            serverOptions = () => connectStream(plan.port);
        } else {
            externallyOwned = listening;
            console.log(
                `[Legend Pure] Transport: socket :${plan.port} (${plan.reason}) — ` +
                    (listening ? 'daemon already listening, connecting only' : 'nothing listening, will launch')
            );
            serverOptions = listening
                ? () => connectStream(plan.port)
                : buildSpawningServerOptions(context, config, plan.port);
        }
    }

    if (!serverOptions) {
        return;
    }

    const roots = resolveConfiguredRoots(config);
    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'pure' },
            { scheme: 'pure', language: 'pure' },
        ],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.pure'),
        },
    };

    const nextClient = new PureLanguageClient(
        'legendPureLsp',
        'Legend Pure LSP',
        serverOptions,
        clientOptions,
        roots,
        config?.classpathRepositories ?? []
    );

    // Register before start() so a statusChanged fired right after initialize is not missed.
    registerNotificationHandlers(nextClient);

    client = nextClient;
    driftHandler?.reset();
    resetServerReady();
    try {
        await nextClient.start();
    } catch (e) {
        if (client === nextClient) {
            client = undefined;
        }
        disposeClientSubscriptions();
        killOwnedProcess();
        statusBar?.onDisconnected();
        throw e;
    }
    if (client !== nextClient) {
        await nextClient.stop(5000).catch(() => undefined);
        return;
    }
    statusBar?.startPolling();
    refreshPureViews();
}

async function stopLegendClient(): Promise<void> {
    const previous = client;
    client = undefined;
    resetServerReady();
    disposeClientSubscriptions();
    statusBar?.stopPolling();
    statusBar?.onDisconnected();
    driftHandler?.reset();
    refreshPureViews();

    if (previous) {
        try {
            // Ends only this connection; the daemon (if shared) keeps running for other clients.
            await previous.stop(5000);
        } catch (e: any) {
            console.log('[Legend Pure] Failed to stop the LSP client:', e?.message || e);
        }
    }
    killOwnedProcess();
    externallyOwned = false;
}

function killOwnedProcess(): void {
    if (!ownedProcess) {
        return;
    }
    try {
        ownedProcess.kill();
    } catch {
        // already gone
    }
    ownedProcess = undefined;
}

function registerNotificationHandlers(target: LanguageClient): void {
    disposeClientSubscriptions();
    clientSubscriptions.push(
        target.onNotification(NOTIF_STATUS_CHANGED, (s: LspStatus) => {
            const state = (s.state || '').toLowerCase();
            statusBar?.onStatus(s);
            if (state === 'ready') {
                console.log(
                    `[Legend Pure] Server ready (${s.repositoryCount} repos, ${s.symbolCount} symbols)`
                );
                resolveServerReady();
                refreshPureViews();
            }
            if (state === 'initializing' || state === 'recovering' || state === 'reindexing') {
                resetServerReady();
            }
            if (state === 'failed') {
                console.log(`[Legend Pure] Server failed: ${s.message || 'unknown error'}`);
            }
        })
    );
    clientSubscriptions.push(
        target.onNotification(NOTIF_LOG_OUTPUT, (event: LegendLogEvent) => {
            serverLog?.onLogOutput(event);
        })
    );
    clientSubscriptions.push(
        target.onNotification(NOTIF_WORKSPACE_DRIFT, (event: WorkspaceDriftEvent) => {
            void driftHandler?.onDrift(event);
        })
    );
    clientSubscriptions.push(
        target.onNotification(NOTIF_LOCK_CONTENTION, (event: LockContentionEvent) => {
            statusBar?.onLockContention(event);
        })
    );
}

function disposeClientSubscriptions(): void {
    for (const subscription of clientSubscriptions) {
        subscription.dispose();
    }
    clientSubscriptions = [];
}

async function currentState(): Promise<string> {
    if (!client) {
        return 'stopped';
    }
    try {
        const s = await requestStatus(client);
        return (s.state || 'unknown').toLowerCase();
    } catch {
        return 'unknown';
    }
}

const UP_OR_STARTING = new Set(['ready', 'initializing', 'reindexing', 'recovering']);

async function requireClient(): Promise<LanguageClient | undefined> {
    if (client) {
        return client;
    }
    const choice = await window.showErrorMessage(
        'Legend Pure LSP is not running.',
        'Start / Connect'
    );
    if (choice === 'Start / Connect') {
        await commands.executeCommand('legend.startOrConnect');
    }
    return client;
}

function refreshPureViews(): void {
    pureFs?.clearCache();
    packageTree?.refresh();
    codeLensProvider?.refresh();
}

interface RunOptions {
    pctAdapterPath?: string;
    beforeFunctionPath?: string;
    afterFunctionPath?: string;
}

/** Shared by every Run lens: same output channel and error shape as the go() command. */
async function runPureFunction(functionPath: string, options: RunOptions): Promise<void> {
    const active = await requireClient();
    if (!active) {
        return;
    }
    if (!goOutputChannel) {
        goOutputChannel = window.createOutputChannel('Pure Go');
    }
    const out = goOutputChannel;
    out.clear();
    out.show(true);
    out.appendLine(`Executing ${functionPath}...`);
    if (options.beforeFunctionPath) {
        out.appendLine(`  setup:    ${options.beforeFunctionPath}`);
    }
    if (options.afterFunctionPath) {
        out.appendLine(`  teardown: ${options.afterFunctionPath}`);
    }
    if (options.pctAdapterPath) {
        out.appendLine(`  adapter:  ${options.pctAdapterPath}`);
    }
    try {
        const result = await requestExecuteFunction(active, {
            function: functionPath,
            pctAdapterPath: options.pctAdapterPath,
            beforeFunctionPath: options.beforeFunctionPath,
            afterFunctionPath: options.afterFunctionPath,
        });
        if (result.success) {
            out.appendLine(result.output || '(no output)');
            out.appendLine('\n--- Execution complete ---');
        } else {
            out.appendLine(result.output || result.error || 'Unknown error');
        }
    } catch (e: any) {
        out.appendLine('ERROR: ' + (e.message || e));
    }
}

// ── Settings ───────────────────────────────────────────────────────

function readTransportSettings() {
    const config = workspace.getConfiguration('legendPure');
    return {
        connectOnly: config.get<boolean>('server.connectOnly', false),
        connectPort: config.get<number>('server.connectPort', 0),
        launchPort: config.get<number>('server.launchPort', 0),
        configPath: config.get<string>('server.configPath', ''),
    };
}

/** Sidecar roots win over the setting; both fall back to open workspace folders. */
function resolveConfiguredRoots(config: PureLspServerConfig | undefined): string[] {
    const fromConfig = (config?.repoRoots ?? []).map(expandConfiguredPath);
    const fromSettings = getConfiguredStringArray('server.repoRoots').map(expandConfiguredPath);
    const configured = fromConfig.length > 0 ? fromConfig : fromSettings;
    const folders = (workspace.workspaceFolders ?? []).map((folder) => folder.uri.fsPath);
    return resolveRepoRoots(configured, folders);
}

function resolveAutoSync(): boolean {
    const settings = readTransportSettings();
    const config = loadServerConfig(
        settings.configPath ? expandConfiguredPath(settings.configPath) : ''
    );
    if (config && typeof config.autoSyncWorkspace === 'boolean') {
        return config.autoSyncWorkspace;
    }
    return workspace.getConfiguration('legendPure').get<boolean>('server.autoSyncWorkspace', true);
}

function resolvePureOptions(config: PureLspServerConfig | undefined): Record<string, string> {
    const fromSettings = workspace
        .getConfiguration('legendPure')
        .get<Record<string, unknown>>('server.pureOptions', {});
    const merged: Record<string, string> = {};
    for (const [key, value] of Object.entries(fromSettings ?? {})) {
        if (value !== null && value !== undefined) {
            merged[key] = String(value);
        }
    }
    // Sidecar overrides a same-named setting.
    for (const [key, value] of Object.entries(config?.pureOptions ?? {})) {
        merged[key] = value;
    }
    return merged;
}

function resolveJvmArgs(): string[] {
    return getConfiguredStringArray('server.jvmArgs');
}

// ── Launching ──────────────────────────────────────────────────────

function resolveStdioServerOptions(
    context: ExtensionContext,
    config: PureLspServerConfig | undefined
): ServerOptions | undefined {
    const spec = resolveLaunchSpec(context, config, 0);
    if (!spec) {
        return undefined;
    }
    return {
        command: spec.javaExe,
        args: [toJavaArgFileReference(spec.argFile)],
        options: { env: process.env },
    };
}

function buildSpawningServerOptions(
    context: ExtensionContext,
    config: PureLspServerConfig | undefined,
    port: number
): ServerOptions | undefined {
    const spec = resolveLaunchSpec(context, config, port);
    if (!spec) {
        return undefined;
    }
    return async () => {
        const daemon = await spawnSocketDaemon({
            javaExe: spec.javaExe,
            argFile: spec.argFile,
            port,
            onStderrLine: (line) => serverLog?.append('INFO', line),
        });
        ownedProcess = daemon.process;
        return daemon.stream;
    };
}

interface LaunchSpec {
    javaExe: string;
    argFile: string;
}

/** Resolves the java binary, classpath and argfile for a launch. `socketPort > 0` = daemon mode. */
function resolveLaunchSpec(
    context: ExtensionContext,
    config: PureLspServerConfig | undefined,
    socketPort: number
): LaunchSpec | undefined {
    const javaExe = getJavaExecutable();
    const jarPath = resolveServerJar();
    console.log('[Legend Pure] Resolved server JAR:', jarPath);
    if (!jarPath) {
        window.showErrorMessage(
            'Legend Pure LSP: server JAR not found. ' +
            'Set "legendPure.server.jarPath" in settings or build the server with Maven.'
        );
        return undefined;
    }
    const jarSize = Math.round(fs.statSync(jarPath).size / 1024 / 1024);
    console.log(`[Legend Pure] JAR size: ${jarSize}MB; launching with generated argfile`);

    const extraClasspath = resolveExtraClasspath();
    if (!extraClasspath) {
        return undefined;
    }
    const classpathFileEntries = resolveClasspathFileEntries();
    if (!classpathFileEntries) {
        return undefined;
    }

    const hostClasspath = uniqueStrings(extraClasspath.concat(classpathFileEntries));
    const serverClasspath = resolveServerClasspath(jarPath, hostClasspath, classpathFileEntries.length > 0);

    let argFile: string;
    try {
        argFile = writeServerArgFile(context, serverClasspath, resolvePureOptions(config), resolveJvmArgs(), socketPort);
    } catch (e: any) {
        window.showErrorMessage(
            'Legend Pure LSP: failed to write Java argfile: ' + (e?.message || e)
        );
        return undefined;
    }

    console.log('[Legend Pure] Java argfile:', argFile);
    console.log('[Legend Pure] Resolved server classpath entries:', serverClasspath.length);
    if (classpathFileEntries.length > 0) {
        console.log(`[Legend Pure] Resolved classpath file entries: ${classpathFileEntries.length}`);
    }

    return { javaExe, argFile };
}

function resolveServerJar(): string | undefined {
    // 1. Check user configuration
    const config = workspace.getConfiguration('legendPure');
    const configuredPath = config.get<string>('server.jarPath');
    if (configuredPath && configuredPath.trim()) {
        const expandedPath = expandConfiguredPath(configuredPath);
        if (fs.existsSync(expandedPath) && fs.statSync(expandedPath).isFile()) {
            return expandedPath;
        }
        window.showErrorMessage(
            `Legend Pure LSP: configured server JAR does not exist or is not a file: ${expandedPath}`
        );
        return undefined;
    }

    // 2. Look for the JAR relative to this extension (sibling Maven module)
    const extensionDir = path.resolve(__dirname, '..');
    const serverTargetDir = path.resolve(
        extensionDir,
        '..',
        'legend-pure-lsp-server',
        'target'
    );
    const fromSibling = findServerJar(serverTargetDir);
    if (fromSibling) {
        return fromSibling;
    }

    // 3. Look relative to workspace folders
    const workspaceFolders = workspace.workspaceFolders;
    if (workspaceFolders) {
        for (const folder of workspaceFolders) {
            const targetDir = path.join(
                folder.uri.fsPath,
                'legend-pure-lsp',
                'legend-pure-lsp-server',
                'target'
            );
            const found = findServerJar(targetDir);
            if (found) {
                return found;
            }
        }
    }

    return undefined;
}

function findServerJar(targetDir: string): string | undefined {
    if (!fs.existsSync(targetDir)) {
        return undefined;
    }
    const files = fs.readdirSync(targetDir);
    const mainJar = files.find(
        (f) =>
            f.startsWith('legend-pure-lsp-server-') &&
            f.endsWith('.jar') &&
            !f.endsWith('-sources.jar') &&
            !f.endsWith('-javadoc.jar') &&
            !f.endsWith('-tests.jar') &&
            !f.endsWith('-shaded.jar')
    );
    return mainJar ? path.join(targetDir, mainJar) : undefined;
}

function resolveServerClasspath(jarPath: string, hostClasspath: string[], hostRuntimeClasspathConfigured: boolean): string[] {
    const entries = [jarPath];
    const dependencyDir = path.join(path.dirname(jarPath), 'dependency');
    if (fs.existsSync(dependencyDir) && fs.statSync(dependencyDir).isDirectory()) {
        if (hostRuntimeClasspathConfigured) {
            const serverDependencies = fs.readdirSync(dependencyDir)
                .filter((fileName) => fileName.endsWith('.jar'))
                .filter((fileName) => !fileName.startsWith('legend-pure-'))
                .sort()
                .map((fileName) => path.join(dependencyDir, fileName));
            entries.push(...serverDependencies);
        } else {
            entries.push(path.join(dependencyDir, '*'));
        }
    } else {
        console.log('[Legend Pure] Server dependency directory not found:', dependencyDir);
    }
    return uniqueStrings(entries.concat(hostClasspath));
}

function resolveClasspathFileEntries(): string[] | undefined {
    const config = workspace.getConfiguration('legendPure');
    const configuredPath = config.get<string>('server.classpathFile');
    if (!configuredPath || !configuredPath.trim()) {
        return [];
    }

    const expandedPath = expandConfiguredPath(configuredPath);
    if (fs.existsSync(expandedPath) && fs.statSync(expandedPath).isFile()) {
        const classpathFileDir = path.dirname(expandedPath);
        const content = fs.readFileSync(expandedPath, { encoding: 'utf8' });
        const entries = content
            .split(/\r?\n/)
            .flatMap((line) => line.split(path.delimiter))
            .map((entry) => entry.trim())
            .filter((entry) => entry.length > 0)
            .map((entry) => resolveClasspathFileEntry(entry, classpathFileDir));

        for (const entry of entries) {
            if (isClasspathWildcard(entry)) {
                const parent = entry.slice(0, -2);
                if (!fs.existsSync(parent) || !fs.statSync(parent).isDirectory()) {
                    window.showErrorMessage(
                        `Legend Pure LSP: configured classpath file contains a wildcard with a missing parent directory: ${parent}`
                    );
                    return undefined;
                }
                continue;
            }
            if (!fs.existsSync(entry)) {
                window.showErrorMessage(
                    `Legend Pure LSP: configured classpath file contains a missing entry: ${entry}`
                );
                return undefined;
            }
        }

        return uniqueStrings(entries);
    }

    window.showErrorMessage(
        `Legend Pure LSP: configured classpath file does not exist or is not a file: ${expandedPath}`
    );
    return undefined;
}

function resolveClasspathFileEntry(entry: string, classpathFileDir: string): string {
    const expanded = entry === '~' || entry.startsWith('~/')
        ? expandConfiguredPath(entry)
        : entry;
    return path.isAbsolute(expanded)
        ? expanded
        : path.resolve(classpathFileDir, expanded);
}

function writeServerArgFile(
    context: ExtensionContext,
    classpath: string[],
    pureOptions: Record<string, string>,
    jvmArgs: string[],
    socketPort: number
): string {
    const storageDir = context.globalStorageUri.fsPath;
    fs.mkdirSync(storageDir, { recursive: true });
    // Per-port filename: avoids two windows racing a launch onto the same argfile.
    const suffix = socketPort > 0 ? `-${socketPort}` : '';
    const argFile = path.join(storageDir, `legend-pure-lsp-server${suffix}.args`);
    fs.writeFileSync(
        argFile,
        buildServerArgFileContent(classpath, pureOptions, jvmArgs, socketPort),
        { encoding: 'utf8' }
    );
    return argFile;
}

function expandConfiguredPath(configuredPath: string): string {
    let expanded = configuredPath.trim();
    if (expanded === '~') {
        expanded = os.homedir();
    } else if (expanded.startsWith('~/')) {
        expanded = path.join(os.homedir(), expanded.slice(2));
    }

    const firstWorkspaceFolder = workspace.workspaceFolders?.[0]?.uri.fsPath;
    if (firstWorkspaceFolder) {
        expanded = expanded.replace(/\$\{workspaceFolder\}/g, firstWorkspaceFolder);
    }

    return path.isAbsolute(expanded)
        ? expanded
        : path.resolve(firstWorkspaceFolder || process.cwd(), expanded);
}

function resolveExtraClasspath(): string[] | undefined {
    const configuredEntries = getConfiguredStringArray('server.extraClasspath');
    const resolvedEntries: string[] = [];

    for (const configuredEntry of configuredEntries) {
        if (isClasspathWildcard(configuredEntry)) {
            const parent = expandConfiguredPath(configuredEntry.slice(0, -2));
            if (!fs.existsSync(parent) || !fs.statSync(parent).isDirectory()) {
                window.showErrorMessage(
                    `Legend Pure LSP: configured classpath wildcard parent does not exist or is not a directory: ${parent}`
                );
                return undefined;
            }
            resolvedEntries.push(path.join(parent, '*'));
            continue;
        }

        const expandedEntry = expandConfiguredPath(configuredEntry);
        if (!fs.existsSync(expandedEntry)) {
            window.showErrorMessage(
                `Legend Pure LSP: configured classpath entry does not exist: ${expandedEntry}`
            );
            return undefined;
        }

        const stat = fs.statSync(expandedEntry);
        if (stat.isDirectory()) {
            resolvedEntries.push(expandedEntry);
            resolvedEntries.push(path.join(expandedEntry, '*'));
        } else if (stat.isFile() && expandedEntry.toLowerCase().endsWith('.jar')) {
            resolvedEntries.push(expandedEntry);
        } else {
            window.showErrorMessage(
                `Legend Pure LSP: configured classpath entry must be a JAR file, directory, or directory wildcard: ${expandedEntry}`
            );
            return undefined;
        }
    }

    return uniqueStrings(resolvedEntries);
}

function getConfiguredStringArray(section: string): string[] {
    const value = workspace.getConfiguration('legendPure').get<unknown>(section);
    if (!Array.isArray(value)) {
        return [];
    }
    return value
        .filter((entry): entry is string => typeof entry === 'string')
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);
}

function isClasspathWildcard(entry: string): boolean {
    return entry.endsWith('/*') || entry.endsWith('\\*');
}

function uniqueStrings(entries: string[]): string[] {
    return Array.from(new Set(entries));
}

function resetServerReady(): void {
    serverReady = new Promise(r => { resolveServerReady = r; });
}

// ── LLM Tool Registration ──────────────────────────────────────────

function registerLanguageModelTools(context: ExtensionContext): void {
    // Guard: vscode.lm.registerTool requires VS Code 1.99+
    if (!vscode.lm || typeof vscode.lm.registerTool !== 'function') {
        console.log('[Legend Pure] vscode.lm.registerTool not available — skipping tool registration');
        return;
    }

    console.log('[Legend Pure] Registering LLM tools...');

    /** Wait for both client and PureRuntime to be ready */
    async function ensureReady(): Promise<LanguageClient | string> {
        if (!client) { return 'Pure LSP not started'; }
        // Wait up to 120s for PureRuntime initialization
        const timeout = new Promise<void>(r => setTimeout(r, 120_000));
        await Promise.race([serverReady, timeout]);
        // Check again after waiting
        if (!client) { return 'Pure LSP not started'; }
        return client;
    }

    // Tool 1: Search Pure symbols
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-search-symbols', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<{ query: string }>, token: vscode.CancellationToken) {
                const readyClient = await ensureReady();
                if (typeof readyClient === 'string') {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(readyClient),
                    ]);
                }
                const symbols: any[] = await readyClient.sendRequest(
                    'workspace/symbol',
                    { query: options.input.query }
                );
                if (!symbols || symbols.length === 0) {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(`No symbols found for "${options.input.query}"`),
                    ]);
                }
                const lines = symbols.slice(0, 50).map((s: any) => {
                    const kind = symbolKindName(s.kind);
                    const uri = s.location?.uri || '';
                    const line = s.location?.range?.start?.line;
                    const loc = line != null ? `${uri}#L${line + 1}` : uri;
                    return `${s.name} (${kind}) — ${loc}`;
                });
                const text = `Found ${symbols.length} symbol(s):\n${lines.join('\n')}`;
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(text),
                ]);
            },
            async prepareInvocation(options: vscode.LanguageModelToolInvocationPrepareOptions<{ query: string }>) {
                return { invocationMessage: `Searching Pure symbols for "${options.input.query}"...` };
            },
        })
    );

    // Tool 2: Execute go()
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-execute-go', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<Record<string, never>>, token: vscode.CancellationToken) {
                const readyClient = await ensureReady();
                if (typeof readyClient === 'string') {
                    return new vscode.LanguageModelToolResult([new vscode.LanguageModelTextPart(readyClient)]);
                }
                const result = await requestExecuteGo(readyClient);
                const text = result.success
                    ? (result.output || '(no output)')
                    : (result.output || result.error || 'Unknown error');
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(text),
                ]);
            },
            async prepareInvocation() {
                return { invocationMessage: 'Executing Pure go() function...' };
            },
        })
    );

    // Tool 3: Get source content
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-get-source', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<{ sourceId: string }>, token: vscode.CancellationToken) {
                const readyClient = await ensureReady();
                if (typeof readyClient === 'string') {
                    return new vscode.LanguageModelToolResult([new vscode.LanguageModelTextPart(readyClient)]);
                }
                const content: string | null = await readyClient.sendRequest(
                    'legend/getSourceContent',
                    options.input.sourceId
                );
                if (content == null) {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(`Source not found: ${options.input.sourceId}`),
                    ]);
                }
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(content),
                ]);
            },
            async prepareInvocation(options: vscode.LanguageModelToolInvocationPrepareOptions<{ sourceId: string }>) {
                return { invocationMessage: `Reading ${options.input.sourceId}...` };
            },
        })
    );

    console.log('[Legend Pure] 3 LLM tools registered');
}

function symbolKindName(kind: number): string {
    const kinds: Record<number, string> = {
        1: 'File', 2: 'Module', 3: 'Namespace', 4: 'Package', 5: 'Class',
        6: 'Method', 7: 'Property', 8: 'Field', 9: 'Constructor', 10: 'Enum',
        11: 'Interface', 12: 'Function', 13: 'Variable', 14: 'Constant',
        15: 'String', 16: 'Number', 17: 'Boolean', 18: 'Array', 19: 'Object',
        20: 'Key', 21: 'Null', 22: 'EnumMember', 23: 'Struct', 24: 'Event',
        25: 'Operator', 26: 'TypeParameter',
    };
    return kinds[kind] || `Kind(${kind})`;
}

function getJavaExecutable(): string {
    const config = workspace.getConfiguration('legendPure');
    const javaHome = config.get<string>('java.home');
    if (javaHome) {
        const javaBin = path.join(javaHome, 'bin', 'java');
        if (fs.existsSync(javaBin)) {
            return javaBin;
        }
    }
    return 'java';
}
