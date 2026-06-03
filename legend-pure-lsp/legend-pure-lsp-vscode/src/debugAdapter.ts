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
import * as path from 'path';
import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

type RequestMessage = vscode.DebugProtocolMessage & {
    seq: number;
    type: string;
    command: string;
    arguments?: any;
};

type DebugBreakpoint = {
    uri: string;
    line: number;
};

type DebugStackFrame = {
    id: number;
    name: string;
    uri: string | null;
    line: number;
    column: number;
};

type DebugResponse = {
    success: boolean;
    state: 'paused' | 'completed' | 'error';
    reason?: string | null;
    message?: string | null;
    output?: string | null;
    stackFrames?: DebugStackFrame[];
};

type DebugVariable = {
    name: string;
    value: string;
    type: string;
    variablesReference?: number;
};

type DebugEvaluateResult = {
    success: boolean;
    result?: string | null;
    error?: string | null;
    variablesReference?: number;
};

type DebugExecutionState = 'idle' | 'running' | 'paused' | 'terminated';

export class LegendPureDebugConfigurationProvider implements vscode.DebugConfigurationProvider {
    resolveDebugConfiguration(
        folder: vscode.WorkspaceFolder | undefined,
        config: vscode.DebugConfiguration
    ): vscode.ProviderResult<vscode.DebugConfiguration> {
        if (!config.type && !config.request && !config.name) {
            config.type = 'legend-pure';
            config.name = 'Debug Pure go()';
            config.request = 'launch';
        }
        config.type = config.type || 'legend-pure';
        config.request = config.request || 'launch';
        config.name = config.name || 'Debug Pure';
        config.function = config.function || 'go():Any[*]';
        return config;
    }
}

export class LegendPureDebugAdapter implements vscode.DebugAdapter {
    // DAP variablesReference 1 is the protocol root for the Locals scope. The
    // LSP server resolves it through legend/debug/variables and allocates any
    // child references for expandable values.
    private static readonly LOCALS_VARIABLES_REFERENCE = 1;

    private readonly emitter = new vscode.EventEmitter<vscode.DebugProtocolMessage>();
    readonly onDidSendMessage = this.emitter.event;

    private nextSeq = 1;
    private nextBreakpointId = 1;
    private readonly breakpointsByUri = new Map<string, DebugBreakpoint[]>();
    private stackFrames: DebugStackFrame[] = [];
    private launchArgs: any | undefined;
    private configurationDone = false;
    private started = false;
    private debugOperationGeneration = 0;
    private executionState: DebugExecutionState = 'idle';

    constructor(
        private readonly clientProvider: () => LanguageClient | undefined,
        private readonly serverReady: Promise<void>
    ) {
    }

    handleMessage(message: vscode.DebugProtocolMessage): void {
        const request = message as RequestMessage;
        if (request.type !== 'request') {
            return;
        }

        switch (request.command) {
            case 'initialize':
                this.sendResponse(request, {
                    supportsConfigurationDoneRequest: true,
                    supportsEvaluateForHovers: true,
                    supportsInvalidatedEvent: true,
                    supportsTerminateRequest: true,
                });
                this.sendEvent('initialized');
                break;
            case 'setBreakpoints':
                this.handleSetBreakpoints(request);
                break;
            case 'launch':
                this.launchArgs = request.arguments || {};
                this.sendResponse(request);
                this.maybeStart();
                break;
            case 'configurationDone':
                this.configurationDone = true;
                this.sendResponse(request);
                this.maybeStart();
                break;
            case 'threads':
                this.sendResponse(request, { threads: [{ id: 1, name: 'Pure' }] });
                break;
            case 'stackTrace':
                this.sendResponse(request, {
                    stackFrames: this.stackFrames.map(frame => this.toDapStackFrame(frame)),
                    totalFrames: this.stackFrames.length,
                });
                break;
            case 'scopes':
                this.sendResponse(request, {
                    scopes: this.executionState === 'paused'
                        ? [{
                            name: 'Locals',
                            presentationHint: 'locals',
                            variablesReference: LegendPureDebugAdapter.LOCALS_VARIABLES_REFERENCE,
                            expensive: false,
                        }]
                        : [],
                });
                break;
            case 'variables':
                void this.handleVariables(request);
                break;
            case 'evaluate':
                void this.handleEvaluate(request);
                break;
            case 'continue':
                this.sendResponse(request, { allThreadsContinued: true });
                void this.resumeWithRequest('legend/debug/continue', true);
                break;
            case 'next':
                this.sendResponse(request, { allThreadsContinued: true });
                void this.resumeWithRequest('legend/debug/stepOver', true);
                break;
            case 'stepIn':
                this.sendResponse(request, { allThreadsContinued: true });
                void this.resumeWithRequest('legend/debug/stepIn', true);
                break;
            case 'stepOut':
                this.sendResponse(request, undefined, false, 'Step out is not supported by the current Pure debug runtime');
                break;
            case 'disconnect':
            case 'terminate':
                this.stopDebug(request);
                break;
            default:
                this.sendResponse(request);
                break;
        }
    }

    dispose(): void {
        this.emitter.dispose();
    }

    private handleSetBreakpoints(request: RequestMessage): void {
        const sourcePath = request.arguments?.source?.path as string | undefined;
        if (!sourcePath) {
            this.sendResponse(request, { breakpoints: [] });
            return;
        }

        const uri = vscode.Uri.file(sourcePath).toString();
        const requested = (request.arguments?.breakpoints || []).map((breakpoint: any) => Number(breakpoint.line));
        const sourceText = readCurrentSourceText(uri, sourcePath);
        const verifiedBreakpoints: DebugBreakpoint[] = [];
        const responseBreakpoints = requested.map((line: number) => {
            const zeroBasedLine = line - 1;
            const verification = verifyBreakpoint(uri, sourcePath, sourceText, zeroBasedLine);
            if (verification.verified) {
                verifiedBreakpoints.push({ uri, line: zeroBasedLine });
            }
            return {
                id: this.nextBreakpointId++,
                verified: verification.verified,
                line,
                message: verification.message,
            };
        });

        this.breakpointsByUri.set(uri, verifiedBreakpoints);
        this.sendResponse(request, { breakpoints: responseBreakpoints });
    }

    private async handleVariables(request: RequestMessage): Promise<void> {
        try {
            if (this.executionState !== 'paused') {
                this.sendResponse(request, { variables: [] });
                return;
            }

            const client = await this.getReadyClient();
            const variablesReference = Number(
                request.arguments?.variablesReference || LegendPureDebugAdapter.LOCALS_VARIABLES_REFERENCE
            );
            const variables = await client.sendRequest<DebugVariable[]>('legend/debug/variables', { variablesReference });
            this.sendResponse(request, {
                variables: (variables || []).map(variable => ({
                    name: variable.name,
                    value: variable.value || '',
                    type: variable.type || '',
                    variablesReference: variable.variablesReference || 0,
                })),
            });
        } catch (e: any) {
            this.sendResponse(request, { variables: [] }, false, e.message || String(e));
        }
    }

    private async handleEvaluate(request: RequestMessage): Promise<void> {
        try {
            if (this.executionState !== 'paused') {
                this.sendResponse(request, undefined, false, 'Debug execution is not paused');
                return;
            }

            const expression = String(request.arguments?.expression || '');
            const client = await this.getReadyClient();
            const result = await client.sendRequest<DebugEvaluateResult>('legend/debug/evaluate', { expression });
            if (result.success) {
                this.sendResponse(request, {
                    result: result.result || '',
                    variablesReference: result.variablesReference || 0,
                });
            } else {
                this.sendResponse(request, undefined, false, result.error || 'Evaluation failed');
            }
        } catch (e: any) {
            this.sendResponse(request, undefined, false, e.message || String(e));
        }
    }

    private maybeStart(): void {
        if (!this.configurationDone || !this.launchArgs || this.started) {
            return;
        }
        this.started = true;
        void this.startDebug();
    }

    private async startDebug(): Promise<void> {
        const operationGeneration = this.markRunning(false);
        try {
            const client = await this.getReadyClient();
            if (!this.isCurrentOperation(operationGeneration)) {
                return;
            }
            const breakpoints = Array.from(this.breakpointsByUri.values()).flat();
            const result = await client.sendRequest<DebugResponse>('legend/debug/start', {
                function: this.launchArgs?.function || 'go():Any[*]',
                breakpoints,
            });
            this.handleDebugResponse(result, operationGeneration);
        } catch (e: any) {
            if (this.isCurrentOperation(operationGeneration)) {
                this.sendOutput(`ERROR: ${e.message || e}\n`, 'stderr');
                this.terminateDebugSession();
            }
        }
    }

    private async resumeWithRequest(method: string, sendContinuedEvent: boolean): Promise<void> {
        const operationGeneration = this.markRunning(sendContinuedEvent);
        try {
            const client = await this.getReadyClient();
            if (!this.isCurrentOperation(operationGeneration)) {
                return;
            }
            const result = await client.sendRequest<DebugResponse>(method);
            this.handleDebugResponse(result, operationGeneration);
        } catch (e: any) {
            if (this.isCurrentOperation(operationGeneration)) {
                this.sendOutput(`ERROR: ${e.message || e}\n`, 'stderr');
                this.terminateDebugSession();
            }
        }
    }

    private stopDebug(request: RequestMessage): void {
        this.sendResponse(request);
        this.terminateDebugSession();

        const client = this.clientProvider();
        if (client) {
            void client.sendRequest<DebugResponse>('legend/debug/stop').catch(() => {
                // The adapter is shutting down; do not surface best-effort stop errors.
            });
        }
    }

    private handleDebugResponse(result: DebugResponse, operationGeneration: number): void {
        if (!this.isCurrentOperation(operationGeneration)) {
            return;
        }

        if (result.output) {
            this.sendOutput(result.output, 'console');
        }

        if (!result.success || result.state === 'error') {
            this.sendOutput(`ERROR: ${result.message || 'Debug execution failed'}\n`, 'stderr');
            this.terminateDebugSession();
            return;
        }

        if (result.state === 'paused') {
            this.markPaused(result.stackFrames || []);
            this.sendEvent('stopped', {
                reason: result.reason || 'breakpoint',
                threadId: 1,
                allThreadsStopped: true,
            });
            this.invalidateVariables();
        } else {
            this.terminateDebugSession();
        }
    }

    private markRunning(sendContinuedEvent: boolean): number {
        const operationGeneration = ++this.debugOperationGeneration;
        this.executionState = 'running';
        this.stackFrames = [];
        if (sendContinuedEvent) {
            this.sendEvent('continued', {
                threadId: 1,
                allThreadsContinued: true,
            });
            this.invalidateVariables();
        }
        return operationGeneration;
    }

    private markPaused(stackFrames: DebugStackFrame[]): void {
        // VS Code refreshes the Variables pane from scopes/variables requests
        // after stopped/invalidated events. The server owns the actual locals;
        // the adapter only mirrors pause state to avoid showing stale values.
        this.executionState = 'paused';
        this.stackFrames = stackFrames;
    }

    private terminateDebugSession(): void {
        const wasTerminated = this.executionState === 'terminated';
        this.debugOperationGeneration++;
        this.executionState = 'terminated';
        this.stackFrames = [];
        this.invalidateVariables();
        if (!wasTerminated) {
            this.sendEvent('terminated');
        }
    }

    private invalidateVariables(): void {
        this.sendEvent('invalidated', { areas: ['variables'] });
    }

    private isCurrentOperation(operationGeneration: number): boolean {
        return operationGeneration === this.debugOperationGeneration && this.executionState !== 'terminated';
    }

    private async getReadyClient(): Promise<LanguageClient> {
        let timeoutHandle: ReturnType<typeof setTimeout> | undefined;
        const timeout = new Promise<never>((_, reject) => {
            timeoutHandle = setTimeout(
                () => reject(new Error('Pure LSP runtime did not become ready in 120 seconds')),
                120_000
            );
        });
        try {
            await Promise.race([this.serverReady, timeout]);
        } finally {
            if (timeoutHandle) {
                clearTimeout(timeoutHandle);
            }
        }
        const client = this.clientProvider();
        if (!client) {
            throw new Error('Pure LSP not started');
        }
        return client;
    }

    private toDapStackFrame(frame: DebugStackFrame): any {
        const source = frame.uri ? sourceFromUri(frame.uri) : undefined;
        return {
            id: frame.id,
            name: frame.name || 'Pure debug point',
            source,
            line: Math.max(1, frame.line || 1),
            column: Math.max(1, frame.column || 1),
        };
    }

    private sendResponse(request: RequestMessage, body?: any, success = true, message?: string): void {
        const response: any = {
            seq: this.nextSeq++,
            type: 'response',
            request_seq: request.seq,
            command: request.command,
            success,
        };
        if (body !== undefined) {
            response.body = body;
        }
        if (message) {
            response.message = message;
        }
        this.emitter.fire(response);
    }

    private sendEvent(event: string, body?: any): void {
        const message: any = {
            seq: this.nextSeq++,
            type: 'event',
            event,
        };
        if (body !== undefined) {
            message.body = body;
        }
        this.emitter.fire(message);
    }

    private sendOutput(output: string, category: 'console' | 'stderr'): void {
        this.sendEvent('output', {
            category,
            output: output.endsWith('\n') ? output : output + '\n',
        });
    }
}

function readCurrentSourceText(uri: string, sourcePath: string): string | undefined {
    const openDocument = vscode.workspace.textDocuments.find(document => document.uri.toString() === uri);
    if (openDocument) {
        return openDocument.getText();
    }
    if (fs.existsSync(sourcePath)) {
        return fs.readFileSync(sourcePath, 'utf8');
    }
    return undefined;
}

function verifyBreakpoint(
    uri: string,
    sourcePath: string,
    sourceText: string | undefined,
    zeroBasedLine: number
): { verified: boolean; message?: string } {
    if (!uri.startsWith('file://') || !sourcePath.endsWith('.pure')) {
        return { verified: false, message: 'Only workspace .pure file breakpoints are supported' };
    }
    if (!isInsideWorkspace(sourcePath)) {
        return { verified: false, message: 'Breakpoint is outside the current workspace' };
    }
    if (!sourceText) {
        return { verified: false, message: 'Source content is not available' };
    }

    const lines = sourceText.split(/\r?\n/);
    if (zeroBasedLine < 0 || zeroBasedLine >= lines.length) {
        return { verified: false, message: 'Line is outside the source file' };
    }
    if (!isInjectableFunctionLine(lines, zeroBasedLine)) {
        return { verified: false, message: 'Breakpoints are supported only on statements inside function bodies' };
    }
    return { verified: true };
}

function isInsideWorkspace(sourcePath: string): boolean {
    const folders = vscode.workspace.workspaceFolders || [];
    const normalized = path.resolve(sourcePath);
    return folders.some(folder => {
        const root = path.resolve(folder.uri.fsPath);
        return normalized === root || normalized.startsWith(root + path.sep);
    });
}

function isInjectableFunctionLine(lines: string[], targetLine: number): boolean {
    if (!isStatementLine(lines[targetLine])) {
        return false;
    }

    return functionRanges(lines).some(range => {
        const line = targetLine + 1;
        return line > range.startLine && line < range.endLine;
    });
}

function functionRanges(lines: string[]): Array<{ startLine: number; endLine: number }> {
    const ranges: Array<{ startLine: number; endLine: number }> = [];
    let pendingFunction = false;
    let inFunction = false;
    let bodyStartLine = -1;
    let depth = 0;

    for (let lineNumber = 0; lineNumber < lines.length; lineNumber++) {
        const code = stripLineComment(lines[lineNumber]);
        const trimmed = code.trim();
        if (!inFunction && trimmed.startsWith('function ')) {
            pendingFunction = true;
        }

        for (const c of code) {
            if (c === '{') {
                if (pendingFunction && !inFunction) {
                    inFunction = true;
                    pendingFunction = false;
                    bodyStartLine = lineNumber + 1;
                    depth = 1;
                } else if (inFunction) {
                    depth++;
                }
            } else if (c === '}' && inFunction) {
                depth--;
                if (depth === 0) {
                    ranges.push({ startLine: bodyStartLine, endLine: lineNumber + 1 });
                    inFunction = false;
                    bodyStartLine = -1;
                }
            }
        }
    }
    return ranges;
}

function isStatementLine(line: string): boolean {
    const trimmed = line.trim();
    return trimmed.length > 0
        && !trimmed.startsWith('//')
        && !trimmed.startsWith('/*')
        && !trimmed.startsWith('*')
        && !trimmed.startsWith('}')
        && !trimmed.startsWith('{')
        && !trimmed.startsWith('function ');
}

function stripLineComment(line: string): string {
    const idx = line.indexOf('//');
    return idx < 0 ? line : line.substring(0, idx);
}

function sourceFromUri(uri: string): any {
    if (uri.startsWith('file://')) {
        const parsed = vscode.Uri.parse(uri);
        return {
            name: path.basename(parsed.fsPath),
            path: parsed.fsPath,
        };
    }
    return {
        name: uri.substring(uri.lastIndexOf('/') + 1) || uri,
        path: uri,
    };
}
