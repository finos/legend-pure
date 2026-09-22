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
    CancellationToken,
    CodeLens,
    CodeLensProvider,
    Disposable,
    EventEmitter,
    Position,
    Range,
    TextDocument,
} from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import { TestFunctionInfo, testFunctions } from './protocol';

/** Run/Debug lenses above compiled `<<test.Test>>` functions, sourced from legend/testFunctions. */
export class PureCodeLensProvider implements CodeLensProvider, Disposable {
    private readonly clientProvider: () => LanguageClient | undefined;
    private readonly changed = new EventEmitter<void>();
    readonly onDidChangeCodeLenses = this.changed.event;

    constructor(clientProvider: () => LanguageClient | undefined) {
        this.clientProvider = clientProvider;
    }

    dispose(): void {
        this.changed.dispose();
    }

    /** Called when the session becomes ready, or recompiles, so stale lenses do not linger. */
    refresh(): void {
        this.changed.fire();
    }

    async provideCodeLenses(
        document: TextDocument,
        token: CancellationToken
    ): Promise<CodeLens[]> {
        const client = this.clientProvider();
        if (!client) {
            return [];
        }

        let functions: TestFunctionInfo[];
        try {
            functions = await testFunctions(client, document.uri.toString());
        } catch {
            return [];
        }
        if (token.isCancellationRequested || !functions || functions.length === 0) {
            return [];
        }

        const lenses: CodeLens[] = [];
        for (const fn of functions) {
            if (fn.isBeforeFunction || fn.isAfterFunction) {
                continue; // offered as a modifier on the test they bracket, not standalone
            }
            const line = Math.max(0, (fn.line || 1) - 1);
            const range = new Range(new Position(line, 0), new Position(line, 0));

            lenses.push(
                new CodeLens(range, {
                    title: '$(play) Run',
                    command: 'legend.runFunction',
                    arguments: [fn.functionPath],
                })
            );
            lenses.push(
                new CodeLens(range, {
                    title: '$(debug-alt) Debug',
                    command: 'legend.debugFunction',
                    arguments: [fn.functionPath],
                })
            );
            if (fn.isPCTTest) {
                lenses.push(
                    new CodeLens(range, {
                        title: 'Run with adapter…',
                        command: 'legend.runFunctionWithAdapter',
                        arguments: [fn.functionPath],
                    })
                );
            }
            lenses.push(
                new CodeLens(range, {
                    title: 'Run with setup/teardown',
                    command: 'legend.runFunctionWithSetupTeardown',
                    arguments: [fn.functionPath],
                })
            );
        }
        return lenses;
    }
}
