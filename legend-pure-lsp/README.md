# Legend Pure LSP

This module owns the Pure language server and VS Code client. The server is kept language-agnostic: it lives in `legend-pure`, starts with Pure platform support, and loads additional languages or stores from the process classpath.

## Server

Build the server jar:

```bash
mvn -pl legend-pure-lsp/legend-pure-lsp-server -am package -DskipTests
```

The server jar is intentionally thin. Always launch it with `-cp` so dependencies and host language extensions remain outside the server artifact:

```bash
java -cp "legend-pure-lsp-server/target/legend-pure-lsp-server-<version>.jar:legend-pure-lsp-server/target/dependency/*:<extension jars/classes>" \
  org.finos.legend.pure.lsp.LegendPureLspServer
```

`target/dependency/*` is produced during `package`; it keeps runtime dependencies separate from the server jar so host repositories can add language/store extensions dynamically.

The LSP initialization options may include:

```json
{
  "classpathRepositories": ["core_functions_unclassified", "core_relational"]
}
```

Platform repositories, including Pure DSL/store platform repos on the classpath, are loaded automatically. Non-platform extension repositories must be named explicitly so a host repository can choose which classpath repositories participate in a session.

## VS Code Client

The VS Code extension resolves the server jar from `legend-pure-lsp-server/target` by default. For host repositories such as `legend-engine`, configure:

- `legendPure.server.jarPath`: path to the built Pure LSP server jar
- `legendPure.server.extraClasspath`: jars, classes directories, or wildcard directories for host extensions
- `legendPure.server.classpathRepositories`: non-platform repository names to load from that classpath
