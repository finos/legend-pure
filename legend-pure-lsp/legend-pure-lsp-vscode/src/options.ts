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

import { ExtensionContext, QuickPickItem, window } from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import { WELL_KNOWN_OPTIONS, getOptions, setOption, status } from './protocol';

const RECENT_KEY = 'legendPure.recentCustomOptionNames';

interface OptionItem extends QuickPickItem {
    name: string;
}

/**
 * Toggles Pure runtime options (`isOptionSet('X')`) live. JVM-global, not per-connection —
 * the title warns when more than one client is attached.
 */
export async function manageOptions(
    context: ExtensionContext,
    client: LanguageClient
): Promise<void> {
    let active: string[];
    try {
        active = await getOptions(client);
    } catch (e: any) {
        window.showErrorMessage(`Legend Pure: could not read current options: ${e?.message || e}`);
        return;
    }
    const activeSet = new Set(active);

    const remembered = context.globalState.get<string[]>(RECENT_KEY, []);
    const names = dedupe([...WELL_KNOWN_OPTIONS, ...remembered, ...active]);

    const items: OptionItem[] = names.map((name) => ({
        name,
        label: name,
        description: WELL_KNOWN_OPTIONS.includes(name) ? undefined : 'custom',
        picked: activeSet.has(name),
    }));
    const CUSTOM: OptionItem = {
        name: '',
        label: '$(add) Set another option by name…',
        alwaysShow: true,
    };

    const shared = await connectedClientCount(client);
    const title =
        shared > 1
            ? `Pure runtime options — shared daemon, ${shared} clients connected (changes affect all)`
            : 'Pure runtime options';

    const picked = await window.showQuickPick([...items, CUSTOM], {
        canPickMany: true,
        title,
        placeHolder: 'Checked options are currently set (isOptionSet returns true)',
    });
    if (!picked) {
        return;
    }

    const selected = new Set(picked.filter((item) => item.name).map((item) => item.name));

    if (picked.some((item) => item === CUSTOM)) {
        const typed = await window.showInputBox({
            title: 'Pure runtime option name',
            prompt: "Name as passed to isOptionSet('…')",
            validateInput: (value) =>
                value && value.trim().length > 0 ? undefined : 'An option name is required',
        });
        if (typed && typed.trim()) {
            const name = typed.trim();
            selected.add(name);
            if (!WELL_KNOWN_OPTIONS.includes(name)) {
                await context.globalState.update(RECENT_KEY, dedupe([...remembered, name]));
            }
        }
    }

    const changes: { name: string; value: boolean }[] = [];
    for (const name of dedupe([...names, ...selected])) {
        const want = selected.has(name);
        if (want !== activeSet.has(name)) {
            changes.push({ name, value: want });
        }
    }
    if (changes.length === 0) {
        return;
    }

    const applied: string[] = [];
    for (const change of changes) {
        try {
            const result = await setOption(client, change.name, change.value);
            if (result.success) {
                applied.push(`${change.name}=${result.effective}`);
            } else {
                window.showErrorMessage(
                    `Legend Pure: could not set ${change.name}: ${result.error ?? 'unknown error'}`
                );
            }
        } catch (e: any) {
            window.showErrorMessage(
                `Legend Pure: could not set ${change.name}: ${e?.message || e}`
            );
        }
    }
    if (applied.length > 0) {
        window.showInformationMessage(`Legend Pure options updated: ${applied.join(', ')}`);
    }
}

async function connectedClientCount(client: LanguageClient): Promise<number> {
    try {
        const s = await status(client);
        return typeof s.connectedClientCount === 'number' ? s.connectedClientCount : 1;
    } catch {
        return 1;
    }
}

function dedupe(values: string[]): string[] {
    return [...new Set(values.filter((value) => value && value.trim().length > 0))];
}
