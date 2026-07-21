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

Workspace repositories are discovered from `*.definition.json` files under the opened workspace. Runtime extensions and any classpath repositories not already represented by the workspace are discovered from the Java classpath.

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
