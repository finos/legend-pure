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

import { LanguageClient } from 'vscode-languageclient/node';

/**
 * The server's custom `legend/*` surface, mirroring its @JsonRequest/@JsonNotification methods.
 * Names and payload shapes must match LegendPureLspServer and LegendLanguageClient exactly.
 */

// ── Requests ───────────────────────────────────────────────────────

export const REQ_STATUS = 'legend/status';
export const REQ_EXECUTE_GO = 'legend/executeGo';
export const REQ_EXECUTE = 'legend/execute';
export const REQ_CHECK_BATCH = 'legend/checkBatch';
export const REQ_SYNC_WORKSPACE = 'legend/syncWorkspace';
export const REQ_SET_OPTION = 'legend/setOption';
export const REQ_GET_OPTIONS = 'legend/getOptions';
export const REQ_DELETE_FILE = 'legend/deleteFile';
export const REQ_TEST_FUNCTIONS = 'legend/testFunctions';
export const REQ_PCT_ADAPTERS = 'legend/getPCTAdapters';
export const REQ_SETUP_TEARDOWN = 'legend/getSetupTeardown';
export const REQ_RESOLVE_SOURCE_URI = 'legend/resolveSourceUri';
export const REQ_GET_SOURCE_CONTENT = 'legend/getSourceContent';
export const REQ_PACKAGE_CHILDREN = 'legend/getPackageChildren';
export const REQ_DAP_ENDPOINT = 'legend/debug/dapEndpoint';

// ── Notifications ──────────────────────────────────────────────────

export const NOTIF_STATUS_CHANGED = 'legend/statusChanged';
export const NOTIF_LOG_OUTPUT = 'legend/logOutput';
export const NOTIF_WORKSPACE_DRIFT = 'legend/workspaceDriftDetected';
export const NOTIF_LOCK_CONTENTION = 'legend/lockContention';

// ── Payloads ───────────────────────────────────────────────────────

export interface LspStatus {
    state: string;
    repositoryCount: number;
    symbolCount: number;
    recoveryAttempts?: number;
    recoveryInProgress?: boolean;
    message?: string;
    compiledRepositories?: number;
    totalRepositories?: number;
    /** Server/session observability added alongside socket-daemon mode. */
    connectedClientCount?: number;
    port?: number;
    transport?: string;
    requestPoolSize?: number;
    repoRoots?: string[];
    jvmArgs?: string[];
    recentErrors?: string[];
    lockContended?: boolean;
    lockContentionReason?: string;
}

export interface FileEntry {
    uri: string;
    content: string;
}

export interface ExecuteGoResult {
    success: boolean;
    error: string | null;
    output: string | null;
    errorUri?: string | null;
}

export interface ExecuteFunctionParams {
    function: string;
    pctAdapterPath?: string | null;
    beforeFunctionPath?: string | null;
    afterFunctionPath?: string | null;
    files?: FileEntry[];
}

export interface CheckBatchResult {
    success: boolean;
    error?: string | null;
    errorUri?: string | null;
    modifiedFiles?: string[];
}

export interface SyncWorkspaceResult {
    success: boolean;
    created: number;
    modified: number;
    deleted: number;
    error?: string | null;
}

export interface SetOptionResult {
    success: boolean;
    name?: string | null;
    effective: boolean;
    error?: string | null;
}

export interface TestFunctionInfo {
    functionPath: string;
    name: string;
    line: number;
    isPCTTest: boolean;
    isBeforeFunction: boolean;
    isAfterFunction: boolean;
}

export interface PCTAdapterInfo {
    name: string;
    path: string;
}

export interface SetupTeardownInfo {
    beforeFunctionPath?: string | null;
    beforeFunctionName?: string | null;
    afterFunctionPath?: string | null;
    afterFunctionName?: string | null;
}

export interface LegendLogEvent {
    level: string;
    message: string;
}

export interface WorkspaceDriftEntry {
    uri: string;
    changeType: string;
}

export interface WorkspaceDriftEvent {
    entries: WorkspaceDriftEntry[];
}

export interface LockContentionEvent {
    active: boolean;
    lockType: string;
    reason?: string | null;
    pendingCount: number;
}

export interface DapEndpoint {
    host: string;
    port: number;
}

// ── Typed helpers ──────────────────────────────────────────────────

export function status(client: LanguageClient): Promise<LspStatus> {
    return client.sendRequest(REQ_STATUS);
}

export function executeGo(client: LanguageClient, files?: FileEntry[]): Promise<ExecuteGoResult> {
    return client.sendRequest(REQ_EXECUTE_GO, files && files.length > 0 ? { files } : {});
}

export function executeFunction(
    client: LanguageClient,
    params: ExecuteFunctionParams
): Promise<ExecuteGoResult> {
    return client.sendRequest(REQ_EXECUTE, params);
}

export function checkBatch(client: LanguageClient, files: FileEntry[]): Promise<CheckBatchResult> {
    return client.sendRequest(REQ_CHECK_BATCH, { files });
}

/** Applies disk drift the server detected. Empty/omitted `uris` syncs everything dirty. */
export function syncWorkspace(
    client: LanguageClient,
    uris?: string[]
): Promise<SyncWorkspaceResult> {
    return client.sendRequest(REQ_SYNC_WORKSPACE, { uris: uris && uris.length > 0 ? uris : null });
}

/** Sets or clears a Pure runtime option. Scoped to the daemon JVM, not this connection. */
export function setOption(
    client: LanguageClient,
    name: string,
    value: boolean
): Promise<SetOptionResult> {
    return client.sendRequest(REQ_SET_OPTION, { name, value });
}

export function getOptions(client: LanguageClient): Promise<string[]> {
    return client.sendRequest(REQ_GET_OPTIONS);
}

export function deleteFile(client: LanguageClient, uri: string): Promise<unknown> {
    return client.sendRequest(REQ_DELETE_FILE, { uri });
}

export function testFunctions(
    client: LanguageClient,
    uri: string
): Promise<TestFunctionInfo[]> {
    return client.sendRequest(REQ_TEST_FUNCTIONS, { uri });
}

export function getPCTAdapters(client: LanguageClient): Promise<PCTAdapterInfo[]> {
    return client.sendRequest(REQ_PCT_ADAPTERS);
}

export function getSetupTeardown(
    client: LanguageClient,
    functionPath: string
): Promise<SetupTeardownInfo> {
    return client.sendRequest(REQ_SETUP_TEARDOWN, { functionPath });
}

export function resolveSourceUri(
    client: LanguageClient,
    sourceId: string
): Promise<{ uri: string | null }> {
    return client.sendRequest(REQ_RESOLVE_SOURCE_URI, { sourceId });
}

/**
 * Command ids the server advertises via `executeCommandProvider`. vscode-languageclient registers
 * these itself, so the extension must never register the same id — doing so throws during
 * initialization and the connection never comes up. Keep in sync with the server.
 */
export const SERVER_OWNED_COMMANDS = ['legend.reindexWorkspace'];

/** Pure runtime options common enough to offer as a fixed list; any other name can still be set. */
export const WELL_KNOWN_OPTIONS = [
    'ForceInterpreted',
    'ExecPlan',
    'PlanLocal',
    'FullInteractiveExec',
    'ExecDebug',
    'ShowLocalPlan',
];
