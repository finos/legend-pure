'use strict';

const assert = require('node:assert/strict');
const { test } = require('node:test');
const Module = require('node:module');
const { pathToFileURL, fileURLToPath } = require('node:url');

const originalLoad = Module._load;

class MockEventEmitter
{
    constructor()
    {
        this.listeners = [];
        this.event = (listener) =>
        {
            this.listeners.push(listener);
            return {
                dispose: () =>
                {
                    this.listeners = this.listeners.filter(current => current !== listener);
                },
            };
        };
    }

    fire(message)
    {
        for (const listener of this.listeners)
        {
            listener(message);
        }
    }

    dispose()
    {
        this.listeners = [];
    }
}

const vscodeMock = {
    EventEmitter: MockEventEmitter,
    workspace: {
        textDocuments: [],
        workspaceFolders: [],
    },
    Uri: {
        file: filePath => ({
            fsPath: filePath,
            toString: () => pathToFileURL(filePath).toString(),
        }),
        parse: uri => ({
            fsPath: fileURLToPath(uri),
        }),
    },
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

const { LegendPureDebugAdapter } = require('../out/debugAdapter');

let nextRequestSeq = 1;

function createDeferred()
{
    let resolve;
    let reject;
    const promise = new Promise((promiseResolve, promiseReject) =>
    {
        resolve = promiseResolve;
        reject = promiseReject;
    });
    return { promise, resolve, reject };
}

function createClient(responders)
{
    const calls = [];
    return {
        calls,
        sendRequest: (method, params) =>
        {
            calls.push({ method, params });
            const responder = responders[method];
            if (!responder)
            {
                return Promise.resolve(undefined);
            }
            return responder(params);
        },
    };
}

function createAdapter(client, serverReady = Promise.resolve())
{
    const adapter = new LegendPureDebugAdapter(() => client, serverReady);
    const messages = [];
    adapter.onDidSendMessage(message => messages.push(message));
    return { adapter, messages };
}

function sendRequest(adapter, command, args)
{
    adapter.handleMessage({
        seq: nextRequestSeq++,
        type: 'request',
        command,
        arguments: args || {},
    });
}

async function flushAsyncWork()
{
    await Promise.resolve();
    await new Promise(resolve => setImmediate(resolve));
    await Promise.resolve();
}

function latestResponse(messages, command)
{
    return messages.filter(message => message.type === 'response' && message.command === command).at(-1);
}

function events(messages, event)
{
    return messages.filter(message => message.type === 'event' && message.event === event);
}

function pausedResponse(reason = 'breakpoint', name = 'go():Any[*]')
{
    return {
        success: true,
        state: 'paused',
        reason,
        stackFrames: [{
            id: 1,
            name,
            uri: null,
            line: 12,
            column: 5,
        }],
    };
}

async function launchAndPause(adapter, startDeferred)
{
    sendRequest(adapter, 'launch', { function: 'go():Any[*]' });
    sendRequest(adapter, 'configurationDone');
    await flushAsyncWork();
    startDeferred.resolve(pausedResponse());
    await flushAsyncWork();
}

test('paused debug response exposes Locals and resolves root variables', async () =>
{
    const start = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/variables': params => Promise.resolve([{
            name: '$tree',
            value: 'GraphFetchTree',
            type: 'GraphFetchTree',
            variablesReference: 7,
        }]),
    });
    const { adapter, messages } = createAdapter(client);

    await launchAndPause(adapter, start);

    assert.equal(events(messages, 'stopped').length, 1);
    assert.equal(events(messages, 'invalidated').length, 1);

    sendRequest(adapter, 'scopes', { frameId: 1 });
    assert.deepEqual(latestResponse(messages, 'scopes').body.scopes, [{
        name: 'Locals',
        presentationHint: 'locals',
        variablesReference: 1,
        expensive: false,
    }]);

    sendRequest(adapter, 'variables', { variablesReference: 1 });
    await flushAsyncWork();

    assert.deepEqual(
        client.calls.find(call => call.method === 'legend/debug/variables').params,
        { variablesReference: 1 }
    );
    assert.deepEqual(latestResponse(messages, 'variables').body.variables, [{
        name: '$tree',
        value: 'GraphFetchTree',
        type: 'GraphFetchTree',
        variablesReference: 7,
    }]);
});

test('paused evaluate forwards current-frame expression to the server', async () =>
{
    const start = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/evaluate': params => Promise.resolve({
            success: true,
            result: 'Ada Lovelace',
            variablesReference: 0,
        }),
    });
    const { adapter, messages } = createAdapter(client);

    await launchAndPause(adapter, start);

    sendRequest(adapter, 'evaluate', {
        expression: 'test::debug::fullName($person)',
        context: 'repl',
        frameId: 1,
    });
    await flushAsyncWork();

    assert.deepEqual(
        client.calls.find(call => call.method === 'legend/debug/evaluate').params,
        { expression: 'test::debug::fullName($person)' }
    );
    assert.deepEqual(latestResponse(messages, 'evaluate').body, {
        result: 'Ada Lovelace',
        variablesReference: 0,
    });
});

test('continue clears stale stack frames and variables until the next pause', async () =>
{
    const start = createDeferred();
    const continued = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/continue': () => continued.promise,
        'legend/debug/variables': () => Promise.resolve([{
            name: '$value',
            value: '1',
            type: 'Integer',
            variablesReference: 0,
        }]),
    });
    const { adapter, messages } = createAdapter(client);

    await launchAndPause(adapter, start);
    assert.equal(events(messages, 'stopped').length, 1);

    sendRequest(adapter, 'continue');
    await flushAsyncWork();

    assert.equal(events(messages, 'continued').length, 1);
    assert.equal(events(messages, 'invalidated').length, 2);

    sendRequest(adapter, 'stackTrace');
    assert.deepEqual(latestResponse(messages, 'stackTrace').body, {
        stackFrames: [],
        totalFrames: 0,
    });

    sendRequest(adapter, 'scopes', { frameId: 1 });
    assert.deepEqual(latestResponse(messages, 'scopes').body.scopes, []);

    const variableCallsBeforeRunningRequest = client.calls.filter(call => call.method === 'legend/debug/variables').length;
    sendRequest(adapter, 'variables', { variablesReference: 1 });
    await flushAsyncWork();
    assert.deepEqual(latestResponse(messages, 'variables').body.variables, []);
    assert.equal(
        client.calls.filter(call => call.method === 'legend/debug/variables').length,
        variableCallsBeforeRunningRequest
    );

    continued.resolve(pausedResponse('step'));
    await flushAsyncWork();

    assert.equal(events(messages, 'stopped').length, 2);
    sendRequest(adapter, 'scopes', { frameId: 1 });
    assert.equal(latestResponse(messages, 'scopes').body.scopes[0].name, 'Locals');
});

test('running evaluate fails locally instead of waiting for the next pause', async () =>
{
    const start = createDeferred();
    const continued = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/continue': () => continued.promise,
        'legend/debug/evaluate': () => Promise.resolve({
            success: true,
            result: 'should not be used',
            variablesReference: 0,
        }),
    });
    const { adapter, messages } = createAdapter(client);

    await launchAndPause(adapter, start);

    sendRequest(adapter, 'continue');
    await flushAsyncWork();

    const evaluateCallsBeforeRunningRequest = client.calls.filter(call => call.method === 'legend/debug/evaluate').length;
    sendRequest(adapter, 'evaluate', {
        expression: 'test::debug::fullName($person)',
        context: 'repl',
        frameId: 1,
    });
    await flushAsyncWork();

    assert.equal(latestResponse(messages, 'evaluate').success, false);
    assert.equal(latestResponse(messages, 'evaluate').message, 'Debug execution is not paused');
    assert.equal(
        client.calls.filter(call => call.method === 'legend/debug/evaluate').length,
        evaluateCallsBeforeRunningRequest
    );

    continued.resolve(pausedResponse('step'));
    await flushAsyncWork();
});

test('terminate responds immediately without waiting for server readiness', async () =>
{
    const neverReady = new Promise(() => {});
    const { adapter, messages } = createAdapter(undefined, neverReady);

    sendRequest(adapter, 'terminate');
    await flushAsyncWork();

    assert.equal(latestResponse(messages, 'terminate').success, true);
    assert.equal(events(messages, 'terminated').length, 1);
    assert.equal(events(messages, 'invalidated').length, 1);
});

test('late paused start response after terminate is ignored', async () =>
{
    const start = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/stop': () => Promise.resolve({ success: true, state: 'completed' }),
    });
    const { adapter, messages } = createAdapter(client);

    sendRequest(adapter, 'launch', { function: 'go():Any[*]' });
    sendRequest(adapter, 'configurationDone');
    await flushAsyncWork();

    assert.equal(client.calls.filter(call => call.method === 'legend/debug/start').length, 1);

    sendRequest(adapter, 'terminate');
    await flushAsyncWork();
    assert.equal(events(messages, 'terminated').length, 1);

    start.resolve(pausedResponse());
    await flushAsyncWork();

    assert.equal(events(messages, 'stopped').length, 0);
    assert.equal(events(messages, 'terminated').length, 1);

    sendRequest(adapter, 'scopes', { frameId: 1 });
    assert.deepEqual(latestResponse(messages, 'scopes').body.scopes, []);
});

test('stale resume response is ignored after a newer operation starts', async () =>
{
    const start = createDeferred();
    const continued = createDeferred();
    const stepped = createDeferred();
    const client = createClient({
        'legend/debug/start': () => start.promise,
        'legend/debug/continue': () => continued.promise,
        'legend/debug/stepOver': () => stepped.promise,
    });
    const { adapter, messages } = createAdapter(client);

    await launchAndPause(adapter, start);
    assert.equal(events(messages, 'stopped').length, 1);

    sendRequest(adapter, 'continue');
    sendRequest(adapter, 'next');
    await flushAsyncWork();

    continued.resolve(pausedResponse('breakpoint', 'stale'));
    await flushAsyncWork();
    assert.equal(events(messages, 'stopped').length, 1);

    stepped.resolve(pausedResponse('step', 'current'));
    await flushAsyncWork();
    assert.equal(events(messages, 'stopped').length, 2);
    assert.equal(events(messages, 'stopped').at(-1).body.reason, 'step');
});
