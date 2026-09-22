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

import { Disposable, OutputChannel, QuickPickItem, Uri, window, workspace } from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import {
    LegendLogEvent,
    WorkspaceDriftEntry,
    WorkspaceDriftEvent,
    syncWorkspace,
} from './protocol';

/** The daemon's own log, streamed over legend/logOutput — distinct from the client trace channel. */
export class ServerLogChannel implements Disposable {
    private readonly channel: OutputChannel;

    constructor() {
        this.channel = window.createOutputChannel('Legend Pure LSP Server');
    }

    dispose(): void {
        this.channel.dispose();
    }

    show(): void {
        this.channel.show(true);
    }

    onLogOutput(event: LegendLogEvent): void {
        this.append(event.level, event.message);
    }

    /** Also used for the spawned daemon's raw stderr. */
    append(level: string, message: string): void {
        if (!message) {
            return;
        }
        this.channel.appendLine(`[${(level || 'INFO').toUpperCase()}] ${message}`);
    }
}

/**
 * Handles legend/workspaceDriftDetected — .pure files changed on disk outside the editor.
 * Batches into one Sync All / Review / Dismiss prompt; autoSyncWorkspace skips the prompt entirely.
 */
export class WorkspaceDriftHandler implements Disposable {
    private readonly clientProvider: () => LanguageClient | undefined;
    private readonly autoSyncProvider: () => boolean;
    private readonly log: ServerLogChannel;
    private pending = new Map<string, WorkspaceDriftEntry>();
    private prompting = false;

    constructor(
        clientProvider: () => LanguageClient | undefined,
        autoSyncProvider: () => boolean,
        log: ServerLogChannel
    ) {
        this.clientProvider = clientProvider;
        this.autoSyncProvider = autoSyncProvider;
        this.log = log;
    }

    dispose(): void {
        this.pending.clear();
    }

    reset(): void {
        this.pending.clear();
    }

    async onDrift(event: WorkspaceDriftEvent): Promise<void> {
        const entries = event?.entries ?? [];
        if (entries.length === 0) {
            return;
        }
        for (const entry of entries) {
            if (entry?.uri) {
                this.pending.set(entry.uri, entry);
            }
        }

        if (this.autoSyncProvider()) {
            await this.sync();
            return;
        }
        await this.prompt();
    }

    private async prompt(): Promise<void> {
        if (this.prompting) {
            return;
        }
        this.prompting = true;
        try {
            const count = this.pending.size;
            const choice = await window.showWarningMessage(
                `Legend Pure: ${count} .pure file(s) changed on disk outside the editor and are out ` +
                    'of sync with the LSP session.',
                'Sync All',
                'Review',
                'Dismiss'
            );
            if (choice === 'Sync All') {
                await this.sync();
            } else if (choice === 'Review') {
                await this.review();
            } else {
                this.pending.clear();
            }
        } finally {
            this.prompting = false;
        }
    }

    private async review(): Promise<void> {
        const entries = [...this.pending.values()];
        const items: (QuickPickItem & { uri: string })[] = entries.map((entry) => ({
            label: shortLabel(entry.uri),
            description: entry.changeType,
            detail: entry.uri,
            uri: entry.uri,
            picked: true,
        }));
        const picked = await window.showQuickPick(items, {
            canPickMany: true,
            title: 'Legend Pure: sync which changed files into the LSP session?',
        });
        if (!picked || picked.length === 0) {
            return;
        }
        await this.sync(picked.map((item) => item.uri));
    }

    private async sync(uris?: string[]): Promise<void> {
        const client = this.clientProvider();
        if (!client) {
            return;
        }
        try {
            const result = await syncWorkspace(client, uris);
            if (!result.success) {
                window.showErrorMessage(
                    `Legend Pure: workspace sync failed: ${result.error ?? 'unknown error'}`
                );
                return;
            }
            const total = result.created + result.modified + result.deleted;
            if (total === 0) {
                this.log.append('INFO', 'Workspace sync: no changes to apply');
            } else {
                this.log.append(
                    'INFO',
                    `Workspace sync: ${result.created} created, ${result.modified} modified, ` +
                        `${result.deleted} deleted`
                );
            }
            if (uris && uris.length > 0) {
                for (const uri of uris) {
                    this.pending.delete(uri);
                }
            } else {
                this.pending.clear();
            }
        } catch (e: any) {
            window.showErrorMessage(
                `Legend Pure: workspace sync failed: ${e?.message || e}`
            );
        }
    }
}

function shortLabel(uri: string): string {
    try {
        const fsPath = Uri.parse(uri).fsPath;
        const relative = workspace.asRelativePath(fsPath, true);
        return relative || fsPath;
    } catch {
        return uri;
    }
}
