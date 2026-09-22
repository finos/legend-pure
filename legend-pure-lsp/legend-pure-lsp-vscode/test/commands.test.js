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
const fs = require('node:fs');
const path = require('node:path');

const { SERVER_OWNED_COMMANDS } = require('../out/protocol');

const SRC = path.join(__dirname, '..', 'src');
const PACKAGE_JSON = path.join(__dirname, '..', 'package.json');

/** Every `commands.registerCommand('id', ...)` literal in the extension source. */
function registeredCommandIds()
{
    const ids = [];
    for (const file of fs.readdirSync(SRC).filter((f) => f.endsWith('.ts')))
    {
        const source = fs.readFileSync(path.join(SRC, file), 'utf8');
        for (const match of source.matchAll(/registerCommand\(\s*'([^']+)'/g))
        {
            ids.push(match[1]);
        }
    }
    return ids;
}

// Regression guard: registering a server-owned command id throws during client init and the
// LSP connection never comes up. See SERVER_OWNED_COMMANDS in protocol.ts.
test('the extension never registers a command the server owns', () =>
{
    const overlap = registeredCommandIds().filter((id) => SERVER_OWNED_COMMANDS.includes(id));
    assert.deepEqual(
        overlap,
        [],
        `these ids are registered by vscode-languageclient from the server's executeCommandProvider ` +
        `and must not be registered by the extension: ${overlap.join(', ')}`
    );
});

test('no command id is registered twice', () =>
{
    const ids = registeredCommandIds();
    const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
    assert.deepEqual(duplicates, [], `duplicate registerCommand ids: ${duplicates.join(', ')}`);
});

test('every contributed command is either registered here or owned by the server', () =>
{
    const contributed = JSON.parse(fs.readFileSync(PACKAGE_JSON, 'utf8'))
        .contributes.commands.map((command) => command.command);
    const known = new Set([...registeredCommandIds(), ...SERVER_OWNED_COMMANDS]);
    const orphans = contributed.filter((id) => !known.has(id));
    assert.deepEqual(
        orphans,
        [],
        `contributed in package.json but nothing handles them: ${orphans.join(', ')}`
    );
});
