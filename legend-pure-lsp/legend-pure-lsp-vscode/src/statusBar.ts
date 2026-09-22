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

import {
    Disposable,
    MarkdownString,
    StatusBarAlignment,
    StatusBarItem,
    ThemeColor,
    window,
} from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import { LockContentionEvent, LspStatus, status as requestStatus } from './protocol';

const POLL_INTERVAL_MS = 5_000;

/** Status bar summary: state, repo/symbol counts, transport, and connected-client count. */
export class PureStatusBar implements Disposable {
    private readonly item: StatusBarItem;
    private readonly clientProvider: () => LanguageClient | undefined;
    private timer: ReturnType<typeof setInterval> | undefined;
    private last: LspStatus | undefined;
    private contention: LockContentionEvent | undefined;
    private polling = false;

    constructor(clientProvider: () => LanguageClient | undefined) {
        this.clientProvider = clientProvider;
        this.item = window.createStatusBarItem(StatusBarAlignment.Left, 100);
        this.item.name = 'Legend Pure LSP';
        this.render();
        this.item.show();
    }

    dispose(): void {
        this.stopPolling();
        this.item.dispose();
    }

    startPolling(): void {
        if (this.timer) {
            return;
        }
        this.timer = setInterval(() => void this.poll(), POLL_INTERVAL_MS);
        void this.poll();
    }

    stopPolling(): void {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = undefined;
        }
    }

    /** Fed by the legend/statusChanged notification, so the bar updates without waiting for a poll. */
    onStatus(status: LspStatus): void {
        this.last = status;
        this.render();
    }

    onLockContention(event: LockContentionEvent): void {
        this.contention = event.active ? event : undefined;
        this.render();
    }

    onDisconnected(): void {
        this.last = undefined;
        this.contention = undefined;
        this.render();
    }

    private async poll(): Promise<void> {
        const client = this.clientProvider();
        if (!client) {
            return;
        }
        if (this.polling) {
            return;
        }
        this.polling = true;
        try {
            this.last = await requestStatus(client);
            this.render();
        } catch {
            // ignored: notifications and client state cover real disconnects
        } finally {
            this.polling = false;
        }
    }

    private render(): void {
        const client = this.clientProvider();
        if (!client) {
            this.item.text = '$(circle-slash) Pure LSP';
            this.item.tooltip = 'Legend Pure LSP is not running. Click to start or connect.';
            this.item.command = 'legend.startOrConnect';
            this.item.backgroundColor = undefined;
            return;
        }

        if (this.contention) {
            const reason = this.contention.reason ? ` — ${this.contention.reason}` : '';
            this.item.text = '$(watch) Pure LSP: waiting';
            this.item.tooltip =
                `Another Pure LSP client is holding the runtime (${this.contention.lockType} lock` +
                `, ${this.contention.pendingCount} request(s) queued)${reason}.\n\n` +
                'This daemon is shared; the request will proceed once the other client releases it.';
            this.item.command = 'legend.showServerLog';
            this.item.backgroundColor = new ThemeColor('statusBarItem.warningBackground');
            return;
        }

        this.item.backgroundColor = undefined;
        this.item.command = 'legend.showStatus';

        const state = (this.last?.state || 'starting').toLowerCase();
        const icon = ICONS[state] ?? '$(question)';
        const counts = this.last
            ? ` ${this.last.repositoryCount}r/${this.last.symbolCount}s`
            : '';
        this.item.text = `${icon} Pure LSP: ${state}${counts}`;
        this.item.tooltip = this.buildTooltip();
        if (state === 'failed') {
            this.item.backgroundColor = new ThemeColor('statusBarItem.errorBackground');
        }
    }

    private buildTooltip(): MarkdownString {
        const md = new MarkdownString();
        md.isTrusted = false;
        const s = this.last;
        if (!s) {
            md.appendMarkdown('**Legend Pure LSP** — starting up.');
            return md;
        }
        md.appendMarkdown(`**Legend Pure LSP** — ${s.state}\n\n`);
        if (s.message) {
            md.appendMarkdown(`${s.message}\n\n`);
        }
        md.appendMarkdown(`- Workspace repos: ${s.repositoryCount}\n`);
        md.appendMarkdown(`- Symbols: ${s.symbolCount}\n`);
        if (s.transport) {
            const where = s.port && s.port > 0 ? `${s.transport} :${s.port}` : s.transport;
            md.appendMarkdown(`- Transport: ${where}\n`);
        }
        if (typeof s.connectedClientCount === 'number') {
            md.appendMarkdown(`- Connected clients: ${s.connectedClientCount}\n`);
        }
        if (typeof s.requestPoolSize === 'number') {
            md.appendMarkdown(`- Request pool: ${s.requestPoolSize}\n`);
        }
        if (s.repoRoots && s.repoRoots.length > 0) {
            md.appendMarkdown(`- Roots:\n`);
            for (const root of s.repoRoots) {
                md.appendMarkdown(`  - ${root}\n`);
            }
        }
        return md;
    }
}

const ICONS: Record<string, string> = {
    ready: '$(check)',
    initializing: '$(sync~spin)',
    reindexing: '$(sync~spin)',
    recovering: '$(sync~spin)',
    degraded: '$(warning)',
    failed: '$(error)',
    created: '$(circle-outline)',
};
