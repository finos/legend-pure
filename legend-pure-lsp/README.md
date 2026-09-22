# Legend Pure LSP

This module owns the Pure language server and VS Code client. The server is kept language-agnostic: it lives in `legend-pure`, starts with Pure platform support, and loads additional languages or stores from the process classpath.

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the current implementation state, rationale, and production direction.

## Runtime and Workspace Model

The LSP uses one transactional `PureRuntime` per server session. Workspace repositories discovered on disk are loaded through an overlay-backed filesystem storage:

- reads come from the real repository files on disk
- PureRuntime writes, deletes, creates, and failed-compile rollbacks go to an in-memory overlay
- the LSP never writes old content back to the physical `.pure` files
- open editor buffers take precedence over file-watcher events
- background disk changes for closed files are compiled from the disk content

This matters when an IDE, an AI tool, or another background process edits a `.pure` file while the LSP is running. If the changed content does not compile, the file remains exactly as it is on disk, diagnostics are published for the bad content, and the runtime is restored to the last good source state. When the content is later fixed, the overlay is cleared and the runtime accepts the disk version.

The overlay is sparse. The server does not load a second runtime and does not copy every workspace file into memory; it stores only sources touched by LSP mutations or rollback handling.

## Server

Build the server jar:

```bash
mvn -pl legend-pure-lsp/legend-pure-lsp-server -am package -DskipTests
```

The server jar is intentionally thin. Always launch it with an explicit classpath so dependencies and host language extensions remain outside the server artifact:

```bash
java -cp "legend-pure-lsp-server/target/legend-pure-lsp-server-<version>.jar:legend-pure-lsp-server/target/dependency/*:<extension jars/classes>" \
  org.finos.legend.pure.lsp.LegendPureLspServer
```

`target/dependency/*` is produced during `package`; it keeps runtime dependencies separate from the server jar so host repositories can add language/store extensions dynamically.

### Shaded Jar

`package` also produces a self-contained jar at `legend-pure-lsp-server/target/legend-pure-lsp-server-<version>-shaded.jar`, with every runtime dependency and a `Main-Class` manifest entry:

```bash
java -jar legend-pure-lsp-server/target/legend-pure-lsp-server-<version>-shaded.jar
```

Use it where a single artifact is easier to distribute than a jar plus a dependency directory. To add host language or store extensions on top of it, put it on an explicit classpath instead of using `-jar`:

```bash
java -cp "legend-pure-lsp-server/target/legend-pure-lsp-server-<version>-shaded.jar:<extension jars/classes>" \
  org.finos.legend.pure.lsp.LegendPureLspServer
```

The thin jar remains the default: the VS Code extension ignores `-shaded.jar` when it auto-discovers a server jar in `target`, so point `legendPure.server.jarPath` at the shaded jar explicitly if you want the extension to use it.

Workspace repositories are discovered from `*.definition.json` files under the opened workspace. Runtime extensions and any classpath repositories not already represented by the workspace are discovered from the Java classpath.

Add `--socket <port>` (or `-Dlegend.lsp.socketPort=<port>`) to run as a standalone TCP daemon instead of over stdio. The size of the thread pool that serves every LSP request (executions, compiles, hover, completion, ...) is configurable at startup via `-Dlegend.lsp.requestPoolSize=<N>` (default `12`) - raise it on a box with more available cores to let more concurrent executions/tests actually run in parallel instead of queuing.

### Sharing one daemon between clients

In socket mode the process is one server instance holding one warm `PureRuntime`, and connections are accepted and served concurrently. An IDE window, a second IDE window, and separate CLI/agent tooling can all hold a live connection at once. Three consequences are worth knowing before wiring up a client:

- **A client's `shutdown`/`exit` ends only its own connection.** The daemon outlives any one client, which is what makes reconnecting to a warm session possible. Only killing the process stops it.
- **Server-initiated messages are broadcast.** Diagnostics, `legend/statusChanged`, `legend/logOutput`, `legend/workspaceDriftDetected` and `legend/lockContention` go to every connected client, so a compile triggered in one window publishes diagnostics in all of them.
- **Workspace roots are latched once.** Whichever arrives first - a launcher calling `preconfigureAndWarm`, or the first client's `initialize` - fixes the repository set for the daemon's lifetime. Later clients' `workspaceFolders` are logged and ignored, and `workspace/didChangeWorkspaceFolders` is not implemented. Plan for the process that starts the daemon to own its scope.

Shared state that is *not* per-connection: Pure runtime options (`legend/setOption` writes `pure.options.*` system properties for the whole JVM), and the debug session, which is a single slot - two clients racing a debug start will have one preempt the other.

Because the daemon's identity is just its loopback port, "connect to this specific daemon" means "connect to this port". A client should distinguish *attaching* to a daemon it must not manage from *launching* one it owns, and expose that distinction to the user.

## VS Code Packaging

Build the server and package the extension:

```bash
mvn -pl legend-pure-lsp/legend-pure-lsp-server -DskipTests package
cd legend-pure-lsp/legend-pure-lsp-vscode
npm install
npm run bundle
npx @vscode/vsce package
```

Install the generated `.vsix` from the VS Code command line or the Extensions view. After installing the VSIX, reload the VS Code window. After changing classpath settings, restart the LSP server so the Java process starts with the new classpath.

The extension also contributes `Legend Pure: Restart LSP Server`. Use it after changing `legendPure.server.jarPath`, `legendPure.server.classpathFile`, `legendPure.server.extraClasspath`, or `legendPure.java.home` to stop the current Java process and start a new one from the current settings.

The VS Code extension resolves the server jar from `legend-pure-lsp-server/target` by default when it is run from this repository. For host repositories such as `legend-engine`, configure:

- `legendPure.server.jarPath`: absolute path to the built Pure LSP server jar
- `legendPure.server.classpathFile`: path to a Maven-generated classpath file for host runtime dependencies
- `legendPure.server.extraClasspath`: optional jars, class directories, or wildcard directories for ad hoc additions
- `legendPure.java.home`: optional Java home; if unset, `java` from `PATH` is used

The extension writes a generated Java argfile and launches `java @argfile` internally. Users do not need to maintain that argfile. For large host repositories, provide a Maven-generated `server.classpathFile`; the extension combines it with the thin LSP server jar and the server dependencies found next to that jar.

### Connection Modes

By default the extension spawns its own stdio child process, one per window. On a large workspace that means one multi-GB JVM per window and no sharing with any other client. The following settings select a socket connection instead, in this order of precedence - the first that applies wins:

| Setting | Behaviour |
|---|---|
| `legendPure.server.connectOnly` + `legendPure.server.connectPort` | Only ever attach to that port. No classpath resolution, no process management. Use when something else owns the daemon's lifecycle. |
| `legendPure.server.launchPort` | Socket mode this window may own: if a daemon is already listening it attaches, otherwise it launches one. |
| `port` in the JSON sidecar at `legendPure.server.configPath` | Same as `launchPort`, or attach-only when the sidecar also sets `"connectOnly": true`. |
| *(none of the above)* | stdio child process, as before. |

Only loopback is supported; a genuinely remote daemon needs an SSH tunnel exposed as a local port first.

Attaching is much cheaper than launching, so `legendPure.server.autoStart` (default `false`) is worth turning on for a workspace configured to attach, and worth leaving off for one that would launch. When it is off, start the server from the status bar item or **Legend Pure: Start / Connect LSP Server**. **Stop / Disconnect** ends only this window's connection when the daemon is externally owned.

### The JSON sidecar

`legendPure.server.configPath` points at a file describing how to reach the daemon. Keeping it out of editor-specific settings lets several clients share one description of the same daemon:

```json
{
  "port": 9100,
  "repoRoots": ["/path/to/legend-pure", "/path/to/legend-engine"],
  "pureOptions": { "pure.options.ForceInterpreted": "true" },
  "connectOnly": true,
  "autoSyncWorkspace": true
}
```

`repoRoots` is the workspace scope sent on `initialize`, overriding this window's own folder list; it has an effect only when the extension is the first client to reach the daemon (see *Sharing one daemon between clients*). `pureOptions` and `legendPure.server.jvmArgs` become `-D` arguments and JVM flags on a server the extension launches, and are ignored when it attaches to one it did not start.

Workspace-level settings and a multi-root `.code-workspace` are the natural home for all of this: the repos open as folders, and the extension attaches to the daemon that already spans them.

### Verifying With `legend-engine`

First build the LSP server:

```bash
cd <legend-pure>
mvn -pl legend-pure-lsp/legend-pure-lsp-server -DskipTests package
```

Generate a host classpath file from the Pure IDE Light module:

```bash
cd <legend-engine>

IDE_LIGHT=legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server
IDE_TARGET="$PWD/$IDE_LIGHT/target"
ENGINE_CP=$IDE_TARGET/legend-pure-lsp-engine.classpath

mvn -pl "$IDE_LIGHT" \
  -DskipTests -Dskip.yarn=true \
  dependency:build-classpath \
  -Dmdep.includeScope=runtime \
  -Dmdep.pathSeparator=: \
  -Dmdep.outputFile="$ENGINE_CP"
```

If the selected reactor artifacts are missing or stale, rebuild them first. This is expensive and should be skipped when the local Maven repository already has the current snapshots:

```bash
mvn -pl "$IDE_LIGHT" -am \
  -DskipTests -Dskip.yarn=true \
  clean install
```

When `server.classpathFile` is configured, the extension excludes `legend-pure-*` jars from the LSP server dependency folder. This lets the host repository provide its own Pure/runtime jars, avoiding duplicate Pure repositories from two different versions on the same Java classpath.

Open the `legend-engine` repository as the VS Code workspace and add workspace settings similar to:

```json
{
  "legendPure.server.jarPath": "<legend-pure>/legend-pure-lsp/legend-pure-lsp-server/target/legend-pure-lsp-server-5.89.3-SNAPSHOT.jar",
  "legendPure.server.classpathFile": "<legend-engine>/legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/target/legend-pure-lsp-engine.classpath"
}
```

Do not add the Pure IDE Light `target/classes` directory or the LSP server `target/dependency` folder for normal `legend-engine` verification. Pure IDE Light is only used as a dependency aggregator here; the LSP uses the dependencies listed in `server.classpathFile`, not the Pure IDE Light server classes.

To verify the server is using `legend-engine`:

1. Install the VSIX and open `<legend-engine>` in VS Code.
2. Reload the VS Code window.
3. Open `Output > Legend Pure LSP` or the extension host log and wait for a ready message with engine-scale counts. A working engine session should report roughly `134` repositories and `39391` symbols in the current local checkout.
4. Open or search for `/core_functions_unclassified/meta/type/function/functionDescriptorToId.pure`; workspace symbol search for `functionDescriptorToId` should resolve to the file under `legend-engine`.
5. Edit a `.pure` file to introduce a type error. The LSP should publish diagnostics, but it must not rewrite or revert the file on disk.

If the workspace has a compilation error before the LSP reaches ready, the server reports status `failed` and publishes a diagnostic to the offending file when the compiler exception contains source information. Full language features still require a successfully initialized runtime.

## Debugger and DAP Architecture

The debug implementation is owned by `legend-pure-lsp`. It does not modify `legend-pure-runtime-java-engine-interpreted` or any other shared runtime module.

The server exposes two debug surfaces:

- legacy JSON-RPC requests under `legend/debug/*`
- a DAP socket endpoint returned by `legend/debug/dapEndpoint`

The DAP endpoint is the preferred client integration point. On startup, the LSP server opens a local DAP socket on `127.0.0.1` using an ephemeral port. A client asks the LSP server for the endpoint, then connects with the standard Debug Adapter Protocol. VS Code now uses this path; other clients can use the same endpoint without reimplementing Pure-specific debug semantics.

The VS Code extension no longer contains a custom in-process debug adapter implementation. Its debug adapter descriptor factory waits for the LSP server to be ready, sends `legend/debug/dapEndpoint`, and returns a `DebugAdapterServer` pointing at the Java process. The Java server owns breakpoint handling, launch, continue, step in, step over, step out, stack trace, scopes, variables, evaluate, terminate, and output events.

### Debug Runtime

Each debug launch creates a separate debug `PureRuntime` from the main LSP runtime snapshot. This keeps debug execution isolated from the main language-server runtime:

- the main LSP runtime remains available while debug execution is paused
- open editor buffers are included in the debug source snapshot
- workspace sources are overlaid into the debug runtime without instrumenting or rewriting disk files
- classpath/dependency repositories are not instrumented
- debug console evaluation is executed with pause suppression so evaluating an expression does not recursively stop the debugger

The debug runtime uses `LegendDebugFunctionExecution`, an LSP-local subclass of `FunctionExecutionInterpreted`. This subclass owns the debugger behavior by overriding public interpreted execution entry points used by the debug runtime. The base interpreted runtime remains unchanged.

### Breakpoints and Stepping

The debug executor records source execution locations from runtime `SourceInformation`, not from text rewriting or synthetic source offsets. This is the intended direction for stable line behavior:

- red-dot breakpoints are matched against original one-based source lines
- breakpoints on function-body expression sequence entries are supported, including bare variable and literal expressions
- step in, step over, and step out operate from runtime execution locations and stack depth
- user breakpoints take precedence over step mode

The debugger maintains a debug-only active-frame stack. At each pause, the server snapshots frames with server-owned `variablesReference` values. Stack traces can include multiple Pure frames, each frame has its own locals scope, and evaluate requests can target a selected frame by `frameId`.

Current scope is intentionally interpreted-only. The implementation does not support conditional breakpoints, watchpoints, reverse debugging, compiled execution, or breakpoints inside platform/JAR dependency sources.

### Client Contract

DAP clients should:

1. start the LSP server normally
2. wait for the server to be ready
3. request `legend/debug/dapEndpoint`
4. connect to the returned `{ "host": "127.0.0.1", "port": <port> }`
5. use standard DAP requests for launch, breakpoints, stack trace, scopes, variables, evaluate, stepping, and termination

The launch configuration requires a zero-argument Pure function signature. If omitted, the server defaults to `go():Any[*]`.

## VS Code Features

The client contributes syntax highlighting, semantic tokens, hover, completion, go-to-definition, references, diagnostics, document symbols, workspace symbols, package tree browsing, `pure://` source browsing, `go()` execution, and Pure debug launch support.

It also surfaces the daemon's shared-session behaviour:

- A **status bar item** showing state, repository and symbol counts, transport and port, and how many clients are connected.
- **Run / Debug CodeLenses** above compiled `<<test.Test>>` functions, with *Run with adapter…* for PCT tests and *Run with setup/teardown* where a `<<test.BeforePackage>>`/`<<test.AfterPackage>>` function applies. These come from `legend/testFunctions`, a semantic check against the compiled graph, so a lens never appears on something the session does not actually have.
- **Legend Pure: Manage Pure Runtime Options** to toggle `ForceInterpreted`, `ExecPlan`, `PlanLocal`, `FullInteractiveExec`, `ExecDebug`, `ShowLocalPlan` and custom names live. The picker says when more than one client is attached, because these are JVM-global.
- A **Legend Pure LSP Server** output channel fed by `legend/logOutput` - the daemon's own log, including work done for other clients.
- **Workspace drift** handling: `.pure` files changed on disk outside the editor are applied automatically (`legendPure.server.autoSyncWorkspace`, the default) or raise a Sync All / Review / Dismiss prompt.
- A **lock-contention indicator**: when another client is holding the runtime's read/write lock, the status bar says so instead of the window appearing to freeze.

Note that the daemon has a single debug slot; two clients starting a debug session concurrently will have one preempt the other.
