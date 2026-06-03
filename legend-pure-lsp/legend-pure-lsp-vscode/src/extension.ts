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
import { workspace, ExtensionContext, Uri, commands, window } from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';
import { LegendPureDebugAdapter, LegendPureDebugConfigurationProvider } from './debugAdapter';
import { PureFileSystemProvider } from './pureFileSystemProvider';
import { PurePackageTreeProvider } from './purePackageTree';

let client: LanguageClient | undefined;
let pureFs: PureFileSystemProvider | undefined;
let packageTree: PurePackageTreeProvider | undefined;
let goOutputChannel: import('vscode').OutputChannel | undefined;
const SERVER_MAIN_CLASS = 'org.finos.legend.pure.lsp.LegendPureLspServer';

let serverReady: Promise<void>;
let resolveServerReady: () => void;
resetServerReady();

interface LspStatus {
    state: string;
    repositoryCount: number;
    symbolCount: number;
    recoveryAttempts: number;
    recoveryInProgress: boolean;
    message?: string;
}

export function activate(context: ExtensionContext): void {
    const jarPath = resolveServerJar();
    console.log('[Legend Pure] Resolved server JAR:', jarPath);
    const jarSize = jarPath ? Math.round(fs.statSync(jarPath).size / 1024 / 1024) : 0;
    console.log(`[Legend Pure] JAR size: ${jarSize}MB; launching with explicit classpath`);
    if (!jarPath) {
        window.showErrorMessage(
            'Legend Pure LSP: server JAR not found. ' +
            'Set "legendPure.server.jarPath" in settings or build the server with Maven.'
        );
        return;
    }

    const extraClasspath = resolveExtraClasspath();
    if (!extraClasspath) {
        return;
    }
    const classpathRepositories = getConfiguredStringArray('server.classpathRepositories');
    const javaHome = getJavaExecutable();
    const serverClasspath = resolveServerClasspath(jarPath, extraClasspath);
    const serverArgs = buildServerArgs(serverClasspath);
    console.log('[Legend Pure] Server launch mode: classpath');
    console.log('[Legend Pure] Resolved server classpath entries:', serverClasspath);
    if (extraClasspath.length > 0) {
        console.log('[Legend Pure] Resolved extra classpath:', extraClasspath);
    }
    if (classpathRepositories.length > 0) {
        console.log('[Legend Pure] Classpath Pure repositories:', classpathRepositories);
    }

    const serverOptions: ServerOptions = {
        command: javaHome,
        args: serverArgs,
        options: { env: process.env },
    };

    const clientOptions: LanguageClientOptions = {
        initializationOptions: {
            classpathRepositories,
        },
        documentSelector: [
            { scheme: 'file', language: 'pure' },
            { scheme: 'pure', language: 'pure' },
        ],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.pure'),
        },
    };

    client = new LanguageClient(
        'legendPureLsp',
        'Legend Pure LSP',
        serverOptions,
        clientOptions
    );

    // Register the executeGo command
    context.subscriptions.push(
        commands.registerCommand('legend.executeGo', async () => {
            if (!client) {
                window.showErrorMessage('Pure LSP not started');
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
                const result: { success: boolean; error: string | null; output: string | null } =
                    await client.sendRequest('legend/executeGo');

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

            const reload = 'Reload Window';
            const choice = await window.showInformationMessage(
                'Legend Pure LSP server JAR path updated. Reload the window to restart the server with this JAR.',
                reload
            );
            if (choice === reload) {
                await commands.executeCommand('workbench.action.reloadWindow');
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

            const reload = 'Reload Window';
            const choice = await window.showInformationMessage(
                'Legend Pure LSP server classpath updated. Reload the window to restart the server with this classpath.',
                reload
            );
            if (choice === reload) {
                await commands.executeCommand('workbench.action.reloadWindow');
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
            createDebugAdapterDescriptor: () => new vscode.DebugAdapterInlineImplementation(
                new LegendPureDebugAdapter(() => client, serverReady)
            ),
        })
    );

    // Register LLM tools (VS Code LanguageModelTool API for Copilot/agents)
    registerLanguageModelTools(context);

    // Start the client and register providers once ready
    client.start().then(() => {
        if (client) {
            // Register pure:// filesystem provider
            pureFs = new PureFileSystemProvider(client);
            context.subscriptions.push(
                workspace.registerFileSystemProvider('pure', pureFs, {
                    isReadonly: true,
                    isCaseSensitive: true,
                })
            );
            context.subscriptions.push(pureFs);

            // Register Pure package tree view
            packageTree = new PurePackageTreeProvider(client);
            context.subscriptions.push(
                window.createTreeView('purePackageTree', {
                    treeDataProvider: packageTree,
                    showCollapseAll: true,
                })
            );

            // Refresh tree and clear caches on reindex
            context.subscriptions.push(
                commands.registerCommand('legend.refreshPackageTree', () => {
                    if (pureFs) {
                        pureFs.clearCache();
                    }
                    if (packageTree) {
                        packageTree.refresh();
                    }
                })
            );

            client.onNotification('legend/statusChanged', (status: LspStatus) => {
                const state = (status.state || '').toLowerCase();
                if (state === 'ready') {
                    console.log(
                        `[Legend Pure] Server ready (${status.repositoryCount} repos, ${status.symbolCount} symbols)`
                    );
                    resolveServerReady();
                    if (pureFs) {
                        pureFs.clearCache();
                    }
                    if (packageTree) {
                        packageTree.refresh();
                    }
                }
                if (state === 'initializing' || state === 'recovering') {
                    resetServerReady();
                }
            });
        }
    });
}

export function deactivate(): Thenable<void> | undefined {
    if (pureFs) {
        pureFs.dispose();
        pureFs = undefined;
    }
    if (!client) {
        return undefined;
    }
    return client.stop();
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
    if (fs.existsSync(serverTargetDir)) {
        const files = fs.readdirSync(serverTargetDir);
        const mainJar = files.find(
            (f) =>
                f.startsWith('legend-pure-lsp-server-') &&
                f.endsWith('.jar') &&
                !f.endsWith('-sources.jar') &&
                !f.endsWith('-javadoc.jar') &&
                !f.endsWith('-tests.jar')
        );
        if (mainJar) {
            return path.join(serverTargetDir, mainJar);
        }
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
            if (fs.existsSync(targetDir)) {
                const files = fs.readdirSync(targetDir);
                const mainJar = files.find(
                    (f) =>
                        f.startsWith('legend-pure-lsp-server-') &&
                        f.endsWith('.jar') &&
                        !f.endsWith('-sources.jar') &&
                        !f.endsWith('-javadoc.jar') &&
                        !f.endsWith('-tests.jar')
                );
                if (mainJar) {
                    return path.join(targetDir, mainJar);
                }
            }
        }
    }

    return undefined;
}

function resolveServerClasspath(jarPath: string, extraClasspath: string[]): string[] {
    const entries = [jarPath];
    const dependencyDir = path.join(path.dirname(jarPath), 'dependency');
    if (fs.existsSync(dependencyDir) && fs.statSync(dependencyDir).isDirectory()) {
        entries.push(path.join(dependencyDir, '*'));
    } else {
        console.log('[Legend Pure] Server dependency directory not found:', dependencyDir);
    }
    return uniqueStrings(entries.concat(extraClasspath));
}

function buildServerArgs(classpath: string[]): string[] {
    return ['-cp', classpath.join(path.delimiter), SERVER_MAIN_CLASS];
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
                const result: { success: boolean; error: string | null; output: string | null } =
                    await readyClient.sendRequest('legend/executeGo');
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
