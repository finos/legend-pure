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

import * as os from 'os';
import * as path from 'path';

export const SERVER_MAIN_CLASS = 'org.finos.legend.pure.lsp.LegendPureLspServer';

/**
 * Builds the server's argv. Kept free of editor APIs so it is directly unit-testable —
 * `--socket`/`-D` ordering here must match what the server expects.
 *
 * `socketPort > 0` appends `--socket <port>`, running the server as a reconnectable daemon
 * instead of a stdio child.
 */
export function buildServerArgs(
    classpath: string[],
    pureOptions: Record<string, string> = {},
    jvmArgs: string[] = [],
    socketPort = 0
): string[] {
    const args: string[] = [];
    for (const [key, value] of Object.entries(pureOptions)) {
        args.push(`-D${key}=${value}`);
    }
    args.push(...jvmArgs);
    args.push('-cp', classpath.join(path.delimiter), SERVER_MAIN_CLASS);
    if (socketPort > 0) {
        args.push('--socket', String(socketPort));
    }
    return args;
}

/** Engine-scale classpaths run to hundreds of KB, well past what a single argv entry can hold. */
export function buildServerArgFileContent(
    classpath: string[],
    pureOptions: Record<string, string> = {},
    jvmArgs: string[] = [],
    socketPort = 0
): string {
    return buildServerArgs(classpath, pureOptions, jvmArgs, socketPort)
        .map(quoteJavaArgFileArgument)
        .join(os.EOL) + os.EOL;
}

export function quoteJavaArgFileArgument(argument: string): string {
    if (/^[A-Za-z0-9_.$:/\\\-*=]+$/.test(argument)) {
        return argument;
    }
    return '"' + argument.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
}

export function toJavaArgFileReference(argFile: string): string {
    return '@' + argFile;
}
