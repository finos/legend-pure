# MCP Server Guide

## 1. What It Is

`legend-pure-mcp-server` is a standalone [Model Context Protocol](https://modelcontextprotocol.io/)
stdio server that exposes a Pure workspace's compile/execute/navigation
capabilities to AI coding agents (Claude Code, and any other MCP client). It
runs the same interpreted engine and `LegendPureSession` machinery as the
Legend Pure LSP server, but speaks MCP tool calls over stdin/stdout instead of
the Language Server Protocol.

**Disk is the source of truth.** Every tool call that touches the compiler
first syncs any changed `.pure` files from disk into the session, then
compiles. There is no separate "open file" / "save file" protocol step like
an editor LSP — an agent edits files with its normal file tools and the next
tool call picks the changes up automatically.

The server runs the **interpreted** engine only (not the ahead-of-time
compiled engine), so `pure_execute` behaves like running Pure in the IDE/dev
path described in the [Compiler Pipeline](../architecture/compiler-pipeline.md)
doc, not like the production compiled runtime.

## 2. Build

```bash
mvn install -DskipTests -pl legend-pure-lsp/legend-pure-mcp-server -am
```

This produces:

- `legend-pure-lsp/legend-pure-mcp-server/target/legend-pure-mcp-server-5.96.1-SNAPSHOT-standalone.jar`
  — a self-contained shaded jar (recommended for launching the server; built
  with the ServiceLoader `META-INF/services` files of all bundled jars
  correctly merged, which Pure's repository/parser discovery depends on)
- `legend-pure-lsp/legend-pure-mcp-server/target/legend-pure-mcp-server-5.96.1-SNAPSHOT.jar`
  — the thin jar, plus `target/dependency/` holding the runtime classpath
  (`maven-dependency-plugin` copies all runtime dependency JARs here during
  the build)

Use the standalone jar unless you have a reason to manage the classpath
yourself.

## 3. Configure in Claude Code

Add a `.mcp.json` at the Pure project root (the directory you want passed as
`--workspace`):

```json
{
  "mcpServers": {
    "legend-pure": {
      "command": "java",
      "args": [
        "-Xmx4g",
        "-jar",
        "/path/to/legend-pure/legend-pure-lsp/legend-pure-mcp-server/target/legend-pure-mcp-server-5.96.1-SNAPSHOT-standalone.jar",
        "--workspace",
        "."
      ]
    }
  }
}
```

Adjust the jar path to match the built `target/` directory from Step 2, and
`--workspace` to the directory containing your repositories (`.` if
`.mcp.json` lives at the workspace root). `-Xmx4g` is not optional in
practice — see [Caveats](#7-caveats).

To add extra jars to the runtime (for example legend-engine interpreted
extensions providing native function implementations), switch from `-jar` to
the classpath form — `java -jar` ignores `-cp`, but the standalone jar works
fine as a classpath entry:

```
java -Xmx4g -cp "/path/to/legend-pure-mcp-server-5.96.1-SNAPSHOT-standalone.jar:/path/to/extra/*" \
    org.finos.legend.pure.lsp.mcp.LegendPureMcpServer --workspace .
```

(The thin jar + `target/dependency/*` combination remains a valid classpath
form as well.)

`stdout` carries only the MCP protocol; all server logging (including init
progress and repository-scan warnings) goes to `stderr`.

## 4. Workspace Layout Requirement

The MCP server discovers Pure repositories exactly the way the LSP server
does: it walks the workspace root looking for `<name>.definition.json` files
under any `src/main/resources` directory, then treats the sibling directory
`<resources>/<name>/` as that repository's source root, recursively loading
every `.pure` file under it.

A minimal single-repository workspace looks like:

```
<workspace-root>/
  module/src/main/resources/
    my_repo.definition.json
    my_repo/
      model/main.pure
```

`my_repo.definition.json`:

```json
{
  "name": "my_repo",
  "pattern": "(Root|my::pkg)(::.*)?",
  "dependencies": ["platform"]
}
```

- `name` must match Pure's repository naming rule — the regex
  `[a-z]++(_[a-z0-9]++)*+`: lowercase letters, underscore-separated segments,
  with digits allowed only *after* the first segment. `test_e2e` is valid;
  `e2e_repo` is **not**, because its leading segment (`e2e`) contains digits.
- `pattern` is the set of packages this repository is allowed to declare
  code in. Include `Root` (as above) if you want to write unqualified
  top-level functions such as `go()` — see the caveat in
  [The Agent Loop](#6-the-agent-loop).
- `dependencies` lists other repositories (by name) this one may reference;
  `platform` is the Pure standard library and is almost always required.

## 5. The Tools

| Tool | Arguments | Returns |
|------|-----------|---------|
| `pure_compile` | none | Syncs all changed `.pure` files from disk and compiles the workspace. Compile errors with `file:line:column`, or a summary of files compiled. |
| `pure_execute` | `function` *(optional)* — full Pure path of a zero-argument function, e.g. `my::pkg::myFunc` or `my::pkg::myFunc():Any[*]`. Omit to run the workspace `go()` function. | Compiles the workspace, then runs the function on the interpreted engine. Returns console output on success, or the error plus Pure stack trace on failure. |
| `pure_find_element` | `path` — full Pure path, e.g. `my::pkg::MyClass` | The element's kind, source location, and definition text. |
| `pure_find_usages` | `path` — full Pure path | All usages of the element across the compiled workspace, each as `file:line:column`. |
| `pure_list_package` | `package` *(optional)* — package path, e.g. `my::pkg`; omit for the root package | The child subpackages and elements of that package. |
| `pure_search_symbols` | `query` — case-insensitive name fragment; `maxResults` *(optional, default 50)* | All compiled elements (classes, functions, enums, ...) whose name matches the fragment. |
| `pure_get_source` | `sourceId` — Pure source id, e.g. `/my_repo/model/File.pure` | The full text content of that source, including platform/library sources with no file on disk. |

**Overloaded functions:** Pure allows several functions to share one name in
the same package, disambiguated only by parameter types (the standard
library's `plus`, for example, has four overloads). If `path` in
`pure_find_element` or `pure_find_usages` resolves to more than one function,
both tools report **every** matching overload rather than guessing which one
you meant — `pure_find_element` lists each overload's signature and
location, `pure_find_usages` aggregates usages under a per-overload header.

## 6. The Agent Loop

The intended workflow, one iteration at a time:

1. Edit `.pure` files with normal file tools (Write/Edit/etc.) — the server
   does not need to be told about the change.
2. Call `pure_compile`. Fix any reported errors and repeat until it succeeds.
3. Write a scratch entry point:

   ```pure
   function go():Any[*]
   {
     print('hello', 1);
   }
   ```

4. Call `pure_execute` with no arguments to compile and run `go()`.

Two things about step 3–4 are easy to trip on:

- An unqualified `function go():Any[*] { ... }` compiles into the special
  `Root` package. The repository's `definition.json` `pattern` must allow
  `Root` (e.g. `"(Root|my::pkg)(::.*)?"`), or the default `go()` flow used by
  `pure_execute` cannot compile at all.
- `pure_execute`'s output surfaces **console output** — anything passed to
  `print(...)` — not the function's plain return value. A `go()` body that
  only returns a value and never prints will show `(no output)`; print
  whatever you want to inspect.

## 7. Caveats

- **First tool call blocks until runtime init completes.** `initialize` and
  `tools/list` respond immediately, but the first `tools/call` (any tool)
  waits on a background thread that scans repositories and builds the
  initial `LegendPureSession`. This can take anywhere from tens of seconds
  to a few minutes depending on workspace size — it is not a hang.
- **Invalid repository definitions fail silently at the WARN level.** If a
  `*.definition.json` has a name that doesn't match the repository naming
  regex, or is malformed JSON, the repository scan logs a `WARN` to stderr
  and simply omits that repository — the workspace still starts and compiles
  without it. If a tool can't find code you know is on disk, check the
  server's stderr log for a repository warning before assuming the code
  itself is wrong.
- **Give the JVM real heap.** The interpreted engine builds large in-memory
  graphs, same as the Maven build. `-Xmx4g` (as in the `.mcp.json` example
  above) is a reasonable floor; smaller workspaces can get away with less,
  larger ones may need more.
- **One session per server process.** The server holds one
  `LegendPureSession` for the lifetime of the process; there is no
  multi-workspace or multi-session mode.
- **Debug tools and shared-IDE-session mode are not yet available.** This is
  a compile/execute/navigate surface only — no breakpoints, stepping, or
  attaching to an already-running LSP session.

---

*See also: [Getting Started Guide](getting-started.md) · [Compiler
Pipeline](../architecture/compiler-pipeline.md)*
