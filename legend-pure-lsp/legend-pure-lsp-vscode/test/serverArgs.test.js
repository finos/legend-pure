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
const os = require('node:os');
const { buildServerArgs, buildServerArgFileContent } = require('../out/serverArgs');

test('stdio launch produces the classic -cp + main class argv', () =>
{
    const args = buildServerArgs(['/a.jar', '/b.jar']);
    assert.deepEqual(args, [
        '-cp',
        ['/a.jar', '/b.jar'].join(require('node:path').delimiter),
        'org.finos.legend.pure.lsp.LegendPureLspServer',
    ]);
});

test('a socket port appends --socket after the main class', () =>
{
    const args = buildServerArgs(['/a.jar'], {}, [], 9100);
    assert.equal(args[args.length - 2], '--socket');
    assert.equal(args[args.length - 1], '9100');
    assert.equal(args[args.length - 3], 'org.finos.legend.pure.lsp.LegendPureLspServer');
});

test('pure options and jvm args precede -cp, verbatim', () =>
{
    const args = buildServerArgs(
        ['/a.jar'],
        { 'pure.options.ForceInterpreted': 'true' },
        ['-Xmx8g'],
        9100
    );
    assert.deepEqual(args.slice(0, 3), [
        '-Dpure.options.ForceInterpreted=true',
        '-Xmx8g',
        '-cp',
    ]);
});

test('argfile entries with spaces are quoted, plain ones are not', () =>
{
    const content = buildServerArgFileContent(['/no spaces?/a.jar'], {}, [], 0);
    const lines = content.split(os.EOL);
    assert.equal(lines[0], '-cp');
    assert.equal(lines[1], '"/no spaces?/a.jar"');
    assert.equal(lines[2], 'org.finos.legend.pure.lsp.LegendPureLspServer');
});

test('a -D argument containing = is not needlessly quoted', () =>
{
    const content = buildServerArgFileContent(['/a.jar'], { 'legend.lsp.requestPoolSize': '12' });
    assert.ok(content.startsWith('-Dlegend.lsp.requestPoolSize=12'));
});
