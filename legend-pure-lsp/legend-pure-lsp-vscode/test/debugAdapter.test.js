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

'use strict';

const assert = require('node:assert/strict');
const { test } = require('node:test');
const Module = require('node:module');

const originalLoad = Module._load;

class MockDebugAdapterServer
{
    constructor(port, host)
    {
        this.port = port;
        this.host = host;
    }
}

const vscodeMock = {
    DebugAdapterServer: MockDebugAdapterServer,
};

Module._load = function mockedLoad(request, parent, isMain)
{
    if (request === 'vscode')
    {
        return vscodeMock;
    }
    if (request === 'vscode-languageclient/node')
    {
        return {};
    }
    return originalLoad.apply(this, [request, parent, isMain]);
};

const {
    createLegendPureDebugAdapterDescriptor,
    LegendPureDebugConfigurationProvider,
} = require('../out/debugAdapter');

test('debug configuration defaults to go()', () =>
{
    const provider = new LegendPureDebugConfigurationProvider();
    const config = provider.resolveDebugConfiguration(undefined, {});

    assert.equal(config.type, 'legend-pure');
    assert.equal(config.request, 'launch');
    assert.equal(config.name, 'Debug Pure go()');
    assert.equal(config.function, 'go():Any[*]');
});

test('debug adapter descriptor resolves server DAP endpoint', async () =>
{
    const calls = [];
    const client = {
        sendRequest: async (method) =>
        {
            calls.push(method);
            assert.equal(method, 'legend/debug/dapEndpoint');
            return { host: '127.0.0.1', port: 45555 };
        },
    };

    const descriptor = await createLegendPureDebugAdapterDescriptor(() => client, Promise.resolve());

    assert.ok(descriptor instanceof MockDebugAdapterServer);
    assert.equal(descriptor.host, '127.0.0.1');
    assert.equal(descriptor.port, 45555);
    assert.deepEqual(calls, ['legend/debug/dapEndpoint']);
});

test('debug adapter descriptor fails when LSP client is unavailable', async () =>
{
    await assert.rejects(
        () => createLegendPureDebugAdapterDescriptor(() => undefined, Promise.resolve()),
        /Pure LSP not started/
    );
});
