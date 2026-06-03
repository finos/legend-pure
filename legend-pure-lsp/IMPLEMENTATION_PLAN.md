# Legend Pure LSP Implementation Plan

This document captures the current LSP state, the decisions made so far, and the direction for making the artifact production ready.

## Goal

The Pure LSP should live in `legend-pure`, not `legend-engine`, and should remain language agnostic. It should provide a thin server artifact that can run in `legend-pure`, `legend-engine`, or another host repository by discovering language and store extensions from the Java process classpath.

The server must also be safe to run beside IDEs, file watchers, and AI editing tools. In particular, it must not revert or rewrite `.pure` files on disk when compilation fails or when a background process edits files.

## Current State

### Repository Placement

- The LSP module is in `legend-pure` under `legend-pure-lsp`.
- The server jar is intentionally thin. Runtime dependencies are copied separately into the server module's `target/dependency` during packaging.
- The LSP module does not depend on `legend-engine` at build time. `legend-engine` is treated as a host repository used for verification and extension loading.

### Runtime Model

- The server uses one transactional `PureRuntime` per LSP session.
- Workspace repositories are discovered from `*.definition.json` files under the opened workspace.
- Classpath repositories are discovered dynamically from the Java classpath.
- If a repository exists in the workspace, the workspace version wins over the classpath version.
- Runtime extension discovery comes from the Java classpath, so host repositories can provide additional grammars, stores, compiled/interpreted runtime extensions, and Pure repositories without changing the LSP server jar.
- If initial runtime compilation fails, the server reports status `failed` and publishes a diagnostic to the offending source when the compiler exception includes source information. Full language features still require a successfully initialized runtime.

### Filesystem Safety

- Workspace repositories are loaded through overlay-backed code storage.
- Reads come from disk unless an in-memory overlay source exists.
- Runtime writes, deletes, creates, and rollback mutations go to the overlay, not to physical files.
- Open editor buffers take precedence over file watcher events.
- Background disk changes for closed files are compiled from disk content.
- If changed content fails to compile, diagnostics are published while the file remains unchanged on disk.
- The runtime is restored to its last good source state after failed edits.
- Scratch files remain in-memory and do not use workspace disk storage.

This fixes the disruptive behavior where the old mutable filesystem storage could write old source content back to disk during rollback.

### Classpath Contract

The intended user-facing VS Code contract is:

```json
{
  "legendPure.server.jarPath": "<legend-pure>/legend-pure-lsp/legend-pure-lsp-server/target/legend-pure-lsp-server-<version>.jar",
  "legendPure.server.classpathFile": "<host-repo>/<module>/target/legend-pure-lsp-host.classpath"
}
```

`legendPure.server.extraClasspath` remains available, but it is for ad hoc additions. It should not be required for the normal `legend-engine` flow.

The VS Code client internally writes a Java argfile and launches `java @argfile`. Users should not have to maintain this argfile. This avoids OS command-line length limits while keeping the configuration understandable.

The VS Code client also contributes `Legend Pure: Restart LSP Server`. Restart stops the current Java process, rebuilds the launch arguments from current settings, and starts a new Java process. This is required to pick up server jar, Java home, or classpath changes without reloading the whole VS Code window.

### Host Classpath File

For `legend-engine`, Pure IDE Light is used only as a dependency aggregator. The LSP does not use or run Pure IDE Light classes.

Generate the host dependency classpath with:

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

The generated file contains the dependency jars for the selected module. It does not copy those jars into another directory.

When `server.classpathFile` is configured, the VS Code launcher includes:

- the thin LSP server jar from `server.jarPath`
- non-Pure dependencies from the LSP server's sibling `target/dependency`
- the host dependency jars from `server.classpathFile`
- any optional `server.extraClasspath` entries

The launcher filters `legend-pure-*` jars from the LSP server dependency folder when a host classpath file is configured. This lets the host repository own the Pure runtime version and avoids loading duplicate Pure repositories from two different versions.

## Why These Choices

### Thin Jar Instead Of Shaded Jar

A shaded all-in-one jar hides the extension classpath and makes development against host repositories harder. A thin jar keeps the server artifact small and lets the user decide which language/store extensions are present by changing the Java classpath.

### Classpath Discovery Instead Of Hardcoded Engine Dependencies

The LSP belongs in `legend-pure` and must be language agnostic. Adding `legend-engine` dependencies to the LSP module would make the Pure LSP tied to one host repository. Classpath discovery keeps the server reusable across repositories.

### `classpathFile` Instead Of Copied Dependency Folder

`dependency:copy-dependencies` for Pure IDE Light produced a large copied jar tree. `dependency:build-classpath` produces a small text file and avoids duplicating hundreds of jars. The VS Code client converts that file into an internal Java argfile only at launch time.

### Generated Argfile Instead Of User Argfile

An argfile is useful for Java process launch because the complete classpath can exceed OS command-line limits. However, an argfile is an implementation detail. Users should configure the server jar and the host classpath file; the extension should generate the Java argfile.

### Overlay Storage Instead Of Mutable Filesystem Storage

The LSP must never overwrite user files as a side effect of runtime rollback. Overlay storage lets the runtime mutate source state transactionally while preserving disk content. It also allows diagnostics for invalid disk edits without reverting those edits.

### One Runtime Instead Of Multiple Runtimes

Runtime initialization is expensive. The current direction keeps one runtime and restores it after failed compilation by using source mutation rollback plus overlay storage. A second runtime would increase startup and memory cost and would still need a reconciliation model for diagnostics and source indexing.

## Verified Behavior

The current implementation has been verified with:

- LSP server test suite: `173` tests run, `0` failures, `0` errors, `1` skipped.
- VS Code client test suite: passed.
- VS Code bundle and VSIX packaging: passed.
- Java-process verification from `legend-engine` using `server.jarPath` plus `server.classpathFile`, without Pure IDE Light `target/classes`.

The `legend-engine` verification reached ready with engine-scale discovery:

- `134` repositories
- `39391` symbols
- successful source lookup for `/core_functions_unclassified/meta/type/function/functionDescriptorToId.pure`
- successful workspace symbol lookup for `functionDescriptorToId`

## Production Direction

### Short Term

- Keep the user configuration surface to `server.jarPath`, `server.classpathFile`, optional `server.extraClasspath`, and optional `java.home`.
- Keep the restart command as the supported way to pick up classpath and server jar changes during development.
- Keep `server.classpathFile` as the preferred host dependency configuration for large repositories.
- Keep LSP server dependency resolution based on the selected server jar's sibling `target/dependency`.
- Keep filtering server-side `legend-pure-*` jars when a host classpath file is configured.
- Document that Pure IDE Light is only a dependency aggregator for `legend-engine` verification.
- Keep startup compile failure behavior explicit: publish the compiler diagnostic when possible, but do not claim the runtime is usable until initialization succeeds.

### Medium Term

- Add a VS Code command to generate the host classpath file from a configured Maven module path.
- Consider a small checked-in script for common hosts such as `legend-engine`.
- Add a launch diagnostic that reports which Pure version owns the runtime and whether duplicate Pure repository resources were detected.
- Add better telemetry/logging around runtime initialization phases, repository counts, classpath repo counts, and symbol indexing time.
- Add tests for `server.classpathFile` parsing and generated argfile behavior in the VS Code client.

### Long Term

- Define a stable host-extension contract independent of `legend-engine`.
- Keep the LSP artifact language agnostic and avoid hardcoded host modules.
- Support other IDE clients with the same server jar and classpath contract.
- Continue reducing runtime startup cost without introducing duplicate runtime state unless there is a strong correctness reason.

## Build Guidance

Avoid broad `legend-engine` reactor builds when possible. A selected `-am clean install -DskipTests` build can take about an hour locally. Prefer:

- module-scoped Maven commands
- `dependency:build-classpath` for dependency discovery
- reusing installed local snapshots when they are current
- avoiding `dependency:copy-dependencies` unless a copied jar directory is explicitly needed

Use `-am` only when the selected module's required reactor artifacts are missing or stale.
