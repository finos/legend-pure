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

const { planTransport } = require('../out/serverTransport');
const { resolveRepoRoots } = require('../out/repoRoots');

const NO_SETTINGS = { connectOnly: false, connectPort: 0, launchPort: 0 };

function config(overrides)
{
    return Object.assign(
        { port: 0, repoRoots: [], pureOptions: {}, connectOnly: false, classpathRepositories: [] },
        overrides
    );
}

// ── planTransport: connectOnly > launchPort > sidecar port > stdio ──

test('no settings and no config falls back to stdio', () =>
{
    const plan = planTransport(NO_SETTINGS, undefined);
    assert.equal(plan.kind, 'stdio');
});

test('a config without a usable port falls back to stdio', () =>
{
    assert.equal(planTransport(NO_SETTINGS, config({ port: 0 })).kind, 'stdio');
    assert.equal(planTransport(NO_SETTINGS, config({ port: -1 })).kind, 'stdio');
});

test('connectOnly with a port wins over everything else', () =>
{
    const plan = planTransport(
        { connectOnly: true, connectPort: 9100, launchPort: 9200 },
        config({ port: 9300 })
    );
    assert.equal(plan.kind, 'connect');
    assert.equal(plan.port, 9100);
});

test('connectOnly without a port is ignored, not treated as connect-to-zero', () =>
{
    const plan = planTransport({ connectOnly: true, connectPort: 0, launchPort: 0 }, undefined);
    assert.equal(plan.kind, 'stdio');
});

test('launchPort beats the sidecar port', () =>
{
    const plan = planTransport(
        { connectOnly: false, connectPort: 0, launchPort: 9200 },
        config({ port: 9300 })
    );
    assert.equal(plan.kind, 'socket');
    assert.equal(plan.port, 9200);
});

test('the sidecar port is used when no setting overrides it', () =>
{
    const plan = planTransport(NO_SETTINGS, config({ port: 9300 }));
    assert.equal(plan.kind, 'socket');
    assert.equal(plan.port, 9300);
});

test('a connectOnly sidecar never launches', () =>
{
    const plan = planTransport(NO_SETTINGS, config({ port: 9300, connectOnly: true }));
    assert.equal(plan.kind, 'connect');
    assert.equal(plan.port, 9300);
});

test('a connectPort set without connectOnly does not by itself attach', () =>
{
    // connectPort is only meaningful together with connectOnly; on its own it must not silently
    // become a launch target.
    const plan = planTransport({ connectOnly: false, connectPort: 9100, launchPort: 0 }, undefined);
    assert.equal(plan.kind, 'stdio');
});

// ── resolveRepoRoots: configured roots > workspace folders ──

test('configured roots override the window folder list', () =>
{
    const roots = resolveRepoRoots(['/repos/pure', '/repos/engine'], ['/repos/engine']);
    assert.deepEqual(roots, ['/repos/pure', '/repos/engine']);
});

test('workspace folders are used when nothing is configured', () =>
{
    const roots = resolveRepoRoots([], ['/repos/engine', '/repos/pure']);
    assert.deepEqual(roots, ['/repos/engine', '/repos/pure']);
});

test('roots are de-duplicated and blanks dropped', () =>
{
    const roots = resolveRepoRoots(['/a', '  ', '/a', ' /b '], []);
    assert.deepEqual(roots, ['/a', '/b']);
});

test('no configuration and no folders yields no roots', () =>
{
    assert.deepEqual(resolveRepoRoots([], []), []);
});
