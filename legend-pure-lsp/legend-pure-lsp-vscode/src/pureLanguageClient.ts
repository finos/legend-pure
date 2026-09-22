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

import { Uri } from 'vscode';
import * as path from 'path';
import {
    InitializeParams,
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';

/**
 * LanguageClient that can declare a workspace scope independent of this window's open folders.
 * Only matters when this client boots the daemon — an already-warm daemon ignores it.
 */
export class PureLanguageClient extends LanguageClient {
    private readonly repoRoots: readonly string[];
    private readonly classpathRepositories: readonly string[];

    constructor(
        id: string,
        name: string,
        serverOptions: ServerOptions,
        clientOptions: LanguageClientOptions,
        repoRoots: readonly string[],
        classpathRepositories: readonly string[]
    ) {
        super(id, name, serverOptions, clientOptions);
        this.repoRoots = repoRoots;
        this.classpathRepositories = classpathRepositories;
    }

    protected fillInitializeParams(params: InitializeParams): void {
        super.fillInitializeParams(params);

        if (this.repoRoots.length > 0) {
            params.workspaceFolders = this.repoRoots.map((root) => ({
                uri: Uri.file(root).toString(),
                name: path.basename(root),
            }));
            // Deprecated fallback; keep in sync with workspaceFolders for older servers.
            params.rootUri = Uri.file(this.repoRoots[0]).toString();
        }

        if (this.classpathRepositories.length > 0) {
            params.initializationOptions = {
                ...(params.initializationOptions ?? {}),
                classpathRepositories: [...this.classpathRepositories],
            };
        }
    }
}
