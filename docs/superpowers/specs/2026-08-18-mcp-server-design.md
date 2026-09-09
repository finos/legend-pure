# Design: `legend-pure-mcp-server` — MCP access to the Pure LSP session

Date: 2026-08-18
Status: Approved for implementation planning

## Goal

Let AI coding agents (Claude Code and other MCP clients) work with Pure code the way
the LSP server lets an IDE work with it: compile a workspace, read structured
diagnostics, execute functions on the interpreted engine, and navigate the compiled
graph — enabling a fast edit → compile → run loop without a Maven build.

## Context and constraints

- `legend-pure-lsp/legend-pure-lsp-server` already contains an LSP-independent core:
  `LegendPureSession` (runtime init, `applyBulkChangesAndCompile`, `executeGo`,
  `executeFunction`, graph read/write locks), `RepositoryScanner` (workspace scan,
  sourceId ↔ path mapping), `UriMapper`, and providers (`NavigationProvider`,
  `ReferencesProvider`, `WorkspaceSymbolProvider`, `PackageTreeProvider`,
  `DocumentOutlineProvider`, `DiagnosticService`). The MCP layer sits directly on
  these — it does NOT tunnel through the LSP protocol.
- The repo compiles with `maven.compiler.release=8` and must build on JDK 11/17/21/25.
  The official MCP Java SDK requires Java 17+, so it cannot be used. The MCP stdio
  protocol (JSON-RPC 2.0, newline-delimited) is hand-rolled with gson, which is
  already a dependency.
- lsp4j's jsonrpc library is NOT reusable for this: it frames messages with LSP-style
  `Content-Length` headers; MCP stdio uses newline-delimited JSON.
- Disk is the source of truth. Agents edit `.pure` files with their own file tools;
  MCP tools sync changed files from disk before compiling/executing. There is no
  write-through-MCP edit path in v1.
- Deployment model v1: the agent owns its session — it launches the MCP server per
  project over stdio. The design must also allow phase 2: the same tool layer hosted
  inside a running LSP server JVM (shared session with an IDE) over an HTTP
  transport. This is achieved by keeping the tool registry transport-neutral.

## Module & packaging

New Maven module `legend-pure-lsp/legend-pure-mcp-server`:

- Artifact `legend-pure-mcp-server`, parent `legend-pure-lsp`, added to the
  `legend-pure-lsp` aggregator pom.
- Depends on `legend-pure-lsp-server` (compile scope) and inherits the same runtime
  dependency set (interpreted engine + DSL grammar/pure/extension modules) so the
  runtime behaves identically to the LSP server's.
- Packaging mirrors `legend-pure-lsp-server`: jar with `mainClass` manifest
  (`org.finos.legend.pure.lsp.mcp.LegendPureMcpServer`) plus
  `maven-dependency-plugin` `copy-dependencies` into `target/dependency`.
- Launch (configured once in `.mcp.json` / Claude Code MCP settings, stdio
  transport):

  ```
  java -cp "legend-pure-mcp-server.jar:dependency/*" \
      org.finos.legend.pure.lsp.mcp.LegendPureMcpServer --workspace /path/to/pure/project
  ```

- No new third-party dependencies. gson, Eclipse Collections, and SLF4J are already
  available.

Java package: `org.finos.legend.pure.lsp.mcp`.

## Architecture

Three components, strictly layered:

### 1. `McpStdioServer` — transport

A loop reading newline-delimited JSON-RPC 2.0 messages from stdin and writing
responses to stdout, using gson. Implements the minimal MCP server method set:

- `initialize` — returns the negotiated protocol version, a `tools` capability, and
  `serverInfo` (name `legend-pure-mcp-server`, project version).
- `notifications/initialized` — accepted, no-op.
- `tools/list` — tool names, descriptions, and input JSON schemas from the registry.
- `tools/call` — dispatches to the registry; returns MCP text content.
- `ping` — returns an empty result.
- Any other method → JSON-RPC `method not found` error.

stdout is protocol-only. At process start, `System.out`/`System.err` are redirected
to stderr (copying the discipline of `LegendPureLspServer.main`), and the transport
holds the one real stdout stream. All logging is SLF4J → stderr.

Requests are handled serially in the read loop. MCP clients rarely pipeline, and
`LegendPureSession`'s read/write locks already make the underlying operations safe
if concurrency is added later. A `notifications/cancelled` message may be ignored
in v1.

A throwable escaping a tool handler is caught, logged, and returned as an error tool
result — it never kills the read loop. The process exits when stdin reaches EOF.

### 2. `PureToolRegistry` + `McpTool` — transport-neutral tool layer

- `McpTool`: name, description, input JSON schema (built as a gson `JsonObject`),
  and a handler `JsonObject args → ToolResult`.
- `ToolResult`: text payload + `isError` flag, mapped by the transport to MCP
  `content: [{type: "text", ...}]` with `isError`.
- `PureToolRegistry`: constructed from a `LegendPureSession`, `RepositoryScanner`,
  `WorkspaceSync`, and the providers. Exposes `list()` and
  `call(name, args)`. It knows nothing about stdio or process lifecycle.

Phase 2 (out of scope, but the reason for this split): `LegendPureLspServer` can
construct a `PureToolRegistry` over its own session and expose it via an HTTP
(streamable) transport, giving agents and the IDE one shared compiled graph.

### 3. `WorkspaceSync` — disk-as-source-of-truth

- Seeded at startup from `RepositoryScanner.getMappings()` with a content hash per
  sourceId.
- On demand (`sync()`), rescans the workspace roots for `.pure` files, compares
  hashes, and produces `List<LegendPureSession.FileChange>` covering modified, new,
  and deleted files; applies them with `session.applyBulkChangesAndCompile(...)`.
- No file watching. Sync happens at the top of every compile/execute tool call,
  which matches the agent's call pattern exactly (edit files → call tool).
- New files get sourceIds via `RepositoryScanner.deriveSourceIdFromPath`.

### Startup sequence

1. Parse args: `--workspace <dir>` (default: current working directory). Unknown
   args → usage message on stderr, exit non-zero.
2. Start the stdio transport immediately: `initialize` and `tools/list` respond at
   once so the client handshake never times out.
3. In the background, scan the workspace (`RepositoryScanner`), initialize the
   session (`LegendPureSession.initialize(scanner)`), and build the workspace symbol
   index.
4. A `tools/call` arriving before initialization completes blocks until the runtime
   is ready, then executes. If initialization failed, every tool call returns an
   error result describing the failure.

## Tool surface (v1)

All results are MCP text content formatted for direct agent consumption: compact,
human-readable text (not raw JSON dumps). Locations are reported as
`<absolute path>:<line>:<column>` plus the sourceId. Tool descriptions carry the key
workflow hints — e.g. `pure_execute`'s description explains "write a
`function go():Any[*] { ... }` in any workspace `.pure` file, then call this tool
with no arguments to run it".

| Tool | Input schema | Behavior / output |
|---|---|---|
| `pure_compile` | (none) | Sync from disk, compile. Success: list of (re)compiled sourceIds. Failure: structured diagnostics — sourceId, path, line, column, message — one per line. `isError` is true when compilation fails. |
| `pure_execute` | `function` (string, optional) | Sync + compile first (a broken workspace returns the compile diagnostics instead of executing). Then: no `function` → `executeGo()`; otherwise `executeFunction(function)` (accepts bare path, signature form, or mangled id — existing session semantics). Output: console output on success; error message + Pure stack trace on failure. |
| `pure_find_element` | `path` (string, required) | Resolve a graph element by user path (e.g. `my::pkg::MyClass`). Output: classifier/kind, sourceId + absolute path, line:column, and the element's definition text extracted from its source. Unknown path → error result listing near-miss suggestions from the symbol index when available. |
| `pure_find_usages` | `path` (string, required) | Resolve the element, then reuse `ReferencesProvider` at the element's declaration position. Output: one location per line with a source-line excerpt. |
| `pure_list_package` | `package` (string, optional; default `::` root) | Children of the package with their kinds, via `PackageTreeProvider.getChildren`. |
| `pure_search_symbols` | `query` (string, required), `maxResults` (integer, optional, default 50) | Fuzzy symbol search via `WorkspaceSymbolProvider.search`. Output: symbol, kind, location per line. |
| `pure_get_source` | `sourceId` (string, required) | Content of a source by id (works for platform/library sources that are not workspace files), via the session's code storage — same behavior as the LSP `legend/getSourceContent` endpoint. |

Naming: `pure_` prefix, lowercase snake case, per MCP conventions.

## Error handling

- Tool-level failures (compile errors, execution errors, unresolvable paths) are
  returned as tool results with `isError: true` and the diagnostic text in the
  content — the agent is expected to read them and fix the code.
- JSON-RPC error responses are reserved for protocol problems: malformed JSON,
  unknown method, unknown tool name, args failing basic validation.
- Malformed input line (unparseable JSON): respond with a JSON-RPC parse error where
  an id is recoverable; otherwise log and continue reading.

## Testing

JUnit 4, no mocking framework, following existing module patterns:

- **Transport tests** (fast, no runtime): drive `McpStdioServer` through piped
  in/out streams with raw JSON-RPC lines; assert on initialize handshake,
  tools/list, tools/call dispatch, unknown-method errors, malformed-line recovery,
  and EOF shutdown. The registry is a hand-written stub registry (allowed — stub,
  not mock framework).
- **WorkspaceSync tests**: `TemporaryFolder` workspace; assert modified/new/deleted
  detection and the produced `FileChange` lists.
- **Tool tests** (runtime-backed, slow): one shared `LegendPureSession` over a
  `TemporaryFolder` workspace fixture, exercising each tool including the full agent
  loop: write file on disk → `pure_compile` (see diagnostics) → fix file →
  `pure_compile` (success) → `pure_execute` (output). Grouped in a suite class
  (mirroring `LspTestSuite`) that surefire is configured to run, so the runtime is
  initialized once.

## Out of scope (v1)

- Debug tools (breakpoints, stepping, evaluate).
- HTTP / streamable transport and embedding in the LSP server JVM (phase 2; enabled
  by the registry/transport split).
- MCP `resources` and `prompts` capabilities.
- Editing sources through MCP tools (disk is the only write path).
- File watching (sync is on-demand).

## Docs to update alongside implementation

- `docs/README.md` index entry for the new module.
- A short user guide (how to configure Claude Code / `.mcp.json`) under
  `docs/guides/`.
