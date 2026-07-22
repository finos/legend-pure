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

import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

type DapEndpoint = {
    host: string;
    port: number;
};

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

export async function createLegendPureDebugAdapterDescriptor(
    clientProvider: () => LanguageClient | undefined,
    serverReady: Promise<void>
): Promise<vscode.DebugAdapterDescriptor> {
    await waitForServerReady(serverReady);
    const client = clientProvider();
    if (!client) {
        throw new Error('Pure LSP not started');
    }
    const endpoint = await client.sendRequest<DapEndpoint>('legend/debug/dapEndpoint');
    if (!endpoint || !endpoint.host || !endpoint.port) {
        throw new Error('Pure LSP did not provide a DAP endpoint');
    }
    return new vscode.DebugAdapterServer(endpoint.port, endpoint.host);
}

async function waitForServerReady(serverReady: Promise<void>): Promise<void> {
    let timeoutHandle: ReturnType<typeof setTimeout> | undefined;
    const timeout = new Promise<never>((_, reject) => {
        timeoutHandle = setTimeout(
            () => reject(new Error('Pure LSP runtime did not become ready in 120 seconds')),
            120_000
        );
    });
    try {
        await Promise.race([serverReady, timeout]);
    } finally {
        if (timeoutHandle) {
            clearTimeout(timeoutHandle);
        }
    }
}
