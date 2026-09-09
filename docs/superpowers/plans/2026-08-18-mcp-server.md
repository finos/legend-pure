# legend-pure-mcp-server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A standalone MCP (Model Context Protocol) stdio server that gives AI coding agents compile / execute / navigate access to a Pure workspace via the existing `LegendPureSession`.

**Architecture:** New Maven module `legend-pure-lsp/legend-pure-mcp-server` with three layers: a hand-rolled newline-delimited JSON-RPC 2.0 stdio transport (`McpStdioServer`), a transport-neutral tool registry (`PureToolRegistry` / `McpTool` / `ToolResult`), and tool implementations (`PureTools`) over `LegendPureSession` plus a disk-as-source-of-truth sync component (`WorkspaceSync`). Runtime initialization happens on a background thread; tool calls block on an `InitGate` until ready.

**Tech Stack:** Java (release 8), gson, Eclipse Collections, SLF4J, JUnit 4. Reuses `legend-pure-lsp-server` classes: `LegendPureSession`, `RepositoryScanner`, `UriMapper`, `PackageTreeProvider`, `ReferencesProvider`, `WorkspaceSymbolProvider`.

**Spec:** `docs/superpowers/specs/2026-08-18-mcp-server-design.md`

## Global Constraints

- **JDK setup:** `source /home/aziem/bin/jdk11.sh` before EVERY `mvn` command (the `.bashrc` JAVA_HOME is broken). All `mvn` commands below assume this prefix.
- **Prereq:** `legend-pure-lsp-server` 5.96.1-SNAPSHOT must be in the local Maven repo. If a build fails with unresolvable `legend-pure-lsp-server`, first run: `mvn install -DskipTests -pl legend-pure-lsp/legend-pure-lsp-server -am` (from repo root; slow — 15+ min cold).
- **Java language level 8** (`maven.compiler.release=8`): no `var`, no `List.of(...)`, no text blocks, no `Optional.isEmpty()`.
- **JUnit 4 only** (`junit:junit`). No mocking framework — hand-written stubs and real objects only.
- **No new third-party dependencies.** Only gson, lsp4j (transitive, for `Location`/`SymbolInformation` types), Eclipse Collections, SLF4J — all already on the `legend-pure-lsp-server` classpath.
- **Checkstyle (enforced at `verify`, fails on warnings):** every `.java` and `.xml` file starts with the Apache 2.0 header exactly as in existing files (`// Copyright 2026 Goldman Sachs ...` for Java, XML comment for XML — copy from `legend-pure-lsp-server` files); spaces not tabs; opening braces on a new line; empty catch blocks only with variable named `ignored` or `expected`.
- **Logging:** SLF4J only. NEVER `System.out` — stdout is the MCP protocol channel. (`main` redirects `System.out` to stderr defensively, same as `LegendPureLspServer.main`.)
- **stdout discipline:** only `McpStdioServer` writes to the protocol output stream, one JSON object per line, no other bytes.
- **Package:** all new code in `org.finos.legend.pure.lsp.mcp` under `legend-pure-lsp/legend-pure-mcp-server/src/{main,test}/java`.
- **Line/column conventions:** Pure `SourceInformation` is 1-based; lsp4j `Range`/`Position` are 0-based. All MCP tool output uses **1-based** lines/columns (convert lsp4j values with `+1`).
- All test commands run from the repo root `/home/aziem/pure/legend-pure-lsp-mcp`.

---

### Task 1: Module scaffold

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/pom.xml`
- Modify: `legend-pure-lsp/pom.xml` (add module)

**Interfaces:**
- Consumes: parent pom `org.finos.legend.pure:legend-pure-lsp:5.96.1-SNAPSHOT` (defines `${gson.version}`).
- Produces: module `legend-pure-mcp-server` that later tasks put code into. Main class named in the manifest is `org.finos.legend.pure.lsp.mcp.LegendPureMcpServer` (created in Task 7 — the jar plugin does not validate its existence before then). Surefire runs only `**/McpTestSuite.java` (created in Task 2).

- [ ] **Step 1: Create the module pom**

Write `legend-pure-lsp/legend-pure-mcp-server/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright 2026 Goldman Sachs

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.finos.legend.pure</groupId>
        <artifactId>legend-pure-lsp</artifactId>
        <version>5.96.1-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>legend-pure-mcp-server</artifactId>
    <packaging>jar</packaging>
    <name>Legend Pure - LSP - MCP Server</name>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>org.finos.legend.pure.lsp.mcp.LegendPureMcpServer</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-runtime-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <includeScope>runtime</includeScope>
                            <outputDirectory>${project.build.directory}/dependency</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <includes>
                        <include>**/McpTestSuite.java</include>
                    </includes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-lsp-server</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register the module in the aggregator**

In `legend-pure-lsp/pom.xml`, change:

```xml
    <modules>
        <module>legend-pure-lsp-server</module>
    </modules>
```

to:

```xml
    <modules>
        <module>legend-pure-lsp-server</module>
        <module>legend-pure-mcp-server</module>
    </modules>
```

- [ ] **Step 3: Create the source directories**

```bash
mkdir -p legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp
mkdir -p legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp
```

- [ ] **Step 4: Verify the module builds**

Run: `source /home/aziem/bin/jdk11.sh && mvn install -DskipTests -pl legend-pure-lsp/legend-pure-mcp-server`
Expected: BUILD SUCCESS (empty jar). If `legend-pure-lsp-server` is unresolvable, run the prereq install from Global Constraints first.

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/pom.xml legend-pure-lsp/legend-pure-mcp-server/pom.xml
git commit -m "Add legend-pure-mcp-server module scaffold"
```

---

### Task 2: Tool registry (`ToolResult`, `McpTool`, `PureToolRegistry`) + test suite class

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/ToolResult.java`
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/McpTool.java`
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/PureToolRegistry.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/PureToolRegistryTest.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/McpTestSuite.java`

**Interfaces:**
- Consumes: gson (`JsonObject`).
- Produces (used by Tasks 3, 5, 6, 7):
  - `ToolResult.ok(String text)`, `ToolResult.error(String text)`, `String getText()`, `boolean isError()`
  - `McpTool(String name, String description, JsonObject inputSchema, McpTool.Handler handler)` with `interface Handler { ToolResult execute(JsonObject arguments) throws Exception; }`, getters `getName()`, `getDescription()`, `getInputSchema()`
  - `PureToolRegistry`: `void register(McpTool tool)` (throws `IllegalArgumentException` on duplicate name), `List<McpTool> list()`, `McpTool get(String name)` (null if unknown), `ToolResult call(String name, JsonObject arguments)` (never throws: handler exceptions become `ToolResult.error(...)`; unknown name throws `IllegalArgumentException` — the transport checks `get()` first and maps unknown names to a protocol error)

Every `.java` file in this and later tasks starts with the standard header (copy verbatim from `legend-pure-lsp-server/src/main/java/org/finos/legend/pure/lsp/LegendPureSession.java` lines 1–13).

- [ ] **Step 1: Write the failing test**

`PureToolRegistryTest.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class PureToolRegistryTest
{
    private static McpTool echoTool()
    {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        return new McpTool("echo", "Echoes the message argument", schema,
                arguments -> ToolResult.ok("echo: " + arguments.get("message").getAsString()));
    }

    @Test
    public void registerListAndCall()
    {
        PureToolRegistry registry = new PureToolRegistry();
        registry.register(echoTool());

        Assert.assertEquals(1, registry.list().size());
        Assert.assertEquals("echo", registry.list().get(0).getName());
        Assert.assertNotNull(registry.get("echo"));
        Assert.assertNull(registry.get("nope"));

        JsonObject args = new JsonObject();
        args.addProperty("message", "hi");
        ToolResult result = registry.call("echo", args);
        Assert.assertFalse(result.isError());
        Assert.assertEquals("echo: hi", result.getText());
    }

    @Test
    public void duplicateRegistrationRejected()
    {
        PureToolRegistry registry = new PureToolRegistry();
        registry.register(echoTool());
        try
        {
            registry.register(echoTool());
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void handlerExceptionBecomesErrorResult()
    {
        PureToolRegistry registry = new PureToolRegistry();
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        registry.register(new McpTool("boom", "Always throws", schema, arguments ->
        {
            throw new IllegalStateException("kapow");
        }));

        ToolResult result = registry.call("boom", new JsonObject());
        Assert.assertTrue(result.isError());
        Assert.assertTrue("Should contain exception message, got: " + result.getText(),
                result.getText().contains("kapow"));
    }

    @Test
    public void callUnknownToolThrows()
    {
        PureToolRegistry registry = new PureToolRegistry();
        try
        {
            registry.call("nope", new JsonObject());
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }
}
```

`McpTestSuite.java` (grows in later tasks):

```java
package org.finos.legend.pure.lsp.mcp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PureToolRegistryTest.class
})
public class McpTestSuite
{
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolRegistryTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (`ToolResult`, `McpTool`, `PureToolRegistry` don't exist).

- [ ] **Step 3: Write the implementation**

`ToolResult.java`:

```java
package org.finos.legend.pure.lsp.mcp;

public class ToolResult
{
    private final String text;
    private final boolean error;

    private ToolResult(String text, boolean error)
    {
        this.text = text;
        this.error = error;
    }

    public static ToolResult ok(String text)
    {
        return new ToolResult(text, false);
    }

    public static ToolResult error(String text)
    {
        return new ToolResult(text, true);
    }

    public String getText()
    {
        return this.text;
    }

    public boolean isError()
    {
        return this.error;
    }
}
```

`McpTool.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import com.google.gson.JsonObject;

public class McpTool
{
    public interface Handler
    {
        ToolResult execute(JsonObject arguments) throws Exception;
    }

    private final String name;
    private final String description;
    private final JsonObject inputSchema;
    private final Handler handler;

    public McpTool(String name, String description, JsonObject inputSchema, Handler handler)
    {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.handler = handler;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public JsonObject getInputSchema()
    {
        return this.inputSchema;
    }

    Handler getHandler()
    {
        return this.handler;
    }
}
```

`PureToolRegistry.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PureToolRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureToolRegistry.class);

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public void register(McpTool tool)
    {
        if (this.tools.containsKey(tool.getName()))
        {
            throw new IllegalArgumentException("Tool already registered: " + tool.getName());
        }
        this.tools.put(tool.getName(), tool);
    }

    public List<McpTool> list()
    {
        return new ArrayList<>(this.tools.values());
    }

    public McpTool get(String name)
    {
        return this.tools.get(name);
    }

    public ToolResult call(String name, JsonObject arguments)
    {
        McpTool tool = this.tools.get(name);
        if (tool == null)
        {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        try
        {
            return tool.getHandler().execute(arguments);
        }
        catch (Throwable t)
        {
            LOGGER.error("Tool '{}' failed", name, t);
            StringWriter stack = new StringWriter();
            t.printStackTrace(new PrintWriter(stack));
            return ToolResult.error("Tool '" + name + "' failed: " + t + "\n" + stack);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolRegistryTest -DfailIfNoTests=false`
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add MCP tool registry (ToolResult, McpTool, PureToolRegistry)"
```

---

### Task 3: MCP stdio transport (`McpStdioServer`)

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/McpStdioServer.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/McpStdioServerTest.java`
- Modify: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/McpTestSuite.java` (add `McpStdioServerTest.class`)

**Interfaces:**
- Consumes: `PureToolRegistry.list()/get()/call()` from Task 2.
- Produces (used by Task 7): `McpStdioServer(PureToolRegistry registry, InputStream in, OutputStream out, String serverVersion)` and `void run() throws IOException` (loops until stdin EOF).

MCP protocol facts this transport implements (JSON-RPC 2.0, one JSON object per `\n`-terminated line, requests carry `id`, notifications don't; notifications never get a response):
- `initialize` request → result `{"protocolVersion": <echo client's, or "2025-06-18" if absent>, "capabilities": {"tools": {}}, "serverInfo": {"name": "legend-pure-mcp-server", "version": <serverVersion>}}`
- `notifications/initialized`, `notifications/cancelled` → ignored
- `ping` → result `{}`
- `tools/list` → result `{"tools": [{"name","description","inputSchema"}, ...]}`
- `tools/call` with params `{"name": ..., "arguments": {...}}` → result `{"content": [{"type": "text", "text": ...}], "isError": <bool>}`; missing/unknown tool name → JSON-RPC error `-32602`
- any other method with an `id` → error `-32601`; unparseable line → error `-32700` with `id: null`; errors as `{"jsonrpc":"2.0","id":...,"error":{"code":...,"message":...}}`

- [ ] **Step 1: Write the failing test**

`McpStdioServerTest.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;

public class McpStdioServerTest
{
    private static PureToolRegistry echoRegistry()
    {
        PureToolRegistry registry = new PureToolRegistry();
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        registry.register(new McpTool("echo", "Echoes the message argument", schema,
                arguments -> ToolResult.ok("echo: " + arguments.get("message").getAsString())));
        registry.register(new McpTool("boom", "Always fails", schema,
                arguments -> ToolResult.error("it broke")));
        return registry;
    }

    private static List<JsonObject> serve(String... requestLines) throws IOException
    {
        StringBuilder input = new StringBuilder();
        for (String line : requestLines)
        {
            input.append(line).append('\n');
        }
        ByteArrayInputStream in = new ByteArrayInputStream(input.toString().getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new McpStdioServer(echoRegistry(), in, out, "1.2.3").run();

        List<JsonObject> responses = new ArrayList<>();
        for (String line : new String(out.toByteArray(), StandardCharsets.UTF_8).split("\n"))
        {
            if (!line.trim().isEmpty())
            {
                responses.add(JsonParser.parseString(line).getAsJsonObject());
            }
        }
        return responses;
    }

    @Test
    public void initializeHandshake() throws IOException
    {
        List<JsonObject> responses = serve(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"0\"}}}",
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

        Assert.assertEquals("Notification must not get a response", 1, responses.size());
        JsonObject result = responses.get(0).getAsJsonObject("result");
        Assert.assertEquals(1, responses.get(0).get("id").getAsInt());
        Assert.assertEquals("2025-03-26", result.get("protocolVersion").getAsString());
        Assert.assertTrue(result.getAsJsonObject("capabilities").has("tools"));
        Assert.assertEquals("legend-pure-mcp-server",
                result.getAsJsonObject("serverInfo").get("name").getAsString());
        Assert.assertEquals("1.2.3", result.getAsJsonObject("serverInfo").get("version").getAsString());
    }

    @Test
    public void toolsListAndCall() throws IOException
    {
        List<JsonObject> responses = serve(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"message\":\"hi\"}}}",
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"boom\",\"arguments\":{}}}");

        Assert.assertEquals(3, responses.size());

        JsonObject listResult = responses.get(0).getAsJsonObject("result");
        Assert.assertEquals(2, listResult.getAsJsonArray("tools").size());
        JsonObject firstTool = listResult.getAsJsonArray("tools").get(0).getAsJsonObject();
        Assert.assertEquals("echo", firstTool.get("name").getAsString());
        Assert.assertTrue(firstTool.has("description"));
        Assert.assertTrue(firstTool.has("inputSchema"));

        JsonObject callResult = responses.get(1).getAsJsonObject("result");
        Assert.assertFalse(callResult.get("isError").getAsBoolean());
        Assert.assertEquals("echo: hi", callResult.getAsJsonArray("content")
                .get(0).getAsJsonObject().get("text").getAsString());

        JsonObject errorResult = responses.get(2).getAsJsonObject("result");
        Assert.assertTrue(errorResult.get("isError").getAsBoolean());
        Assert.assertEquals("it broke", errorResult.getAsJsonArray("content")
                .get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    public void protocolErrors() throws IOException
    {
        List<JsonObject> responses = serve(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"no/such/method\"}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"nope\",\"arguments\":{}}}",
                "this is not json",
                "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"ping\"}");

        Assert.assertEquals(4, responses.size());
        Assert.assertEquals(-32601, responses.get(0).getAsJsonObject("error").get("code").getAsInt());
        Assert.assertEquals(-32602, responses.get(1).getAsJsonObject("error").get("code").getAsInt());
        Assert.assertEquals(-32700, responses.get(2).getAsJsonObject("error").get("code").getAsInt());
        Assert.assertTrue("Loop must survive a malformed line and answer ping",
                responses.get(3).has("result"));
        Assert.assertEquals(4, responses.get(3).get("id").getAsInt());
    }
}
```

Add `McpStdioServerTest.class` to `McpTestSuite`'s `@Suite.SuiteClasses` array.

- [ ] **Step 2: Run test to verify it fails**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=McpStdioServerTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (`McpStdioServer` doesn't exist).

- [ ] **Step 3: Write the implementation**

`McpStdioServer.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal MCP server over stdio: newline-delimited JSON-RPC 2.0 supporting
 * initialize / tools/list / tools/call / ping. Hand-rolled (no MCP SDK) because the
 * official Java SDK requires Java 17 and this repo targets language level 8.
 * The output stream is the protocol channel: nothing else may write to it.
 */
public class McpStdioServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(McpStdioServer.class);
    private static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";
    private static final int PARSE_ERROR = -32700;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;

    private final PureToolRegistry registry;
    private final BufferedReader in;
    private final PrintWriter out;
    private final String serverVersion;
    private final Gson gson = new Gson();

    public McpStdioServer(PureToolRegistry registry, InputStream in, OutputStream out, String serverVersion)
    {
        this.registry = registry;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new PrintWriter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8), true);
        this.serverVersion = serverVersion;
    }

    public void run() throws IOException
    {
        String line;
        while ((line = this.in.readLine()) != null)
        {
            if (!line.trim().isEmpty())
            {
                handleLine(line);
            }
        }
        LOGGER.info("stdin closed; MCP server loop exiting");
    }

    private void handleLine(String line)
    {
        JsonObject message;
        try
        {
            message = JsonParser.parseString(line).getAsJsonObject();
        }
        catch (Exception e)
        {
            writeError(JsonNull.INSTANCE, PARSE_ERROR, "Parse error: not a JSON-RPC message");
            return;
        }

        String method = message.has("method") ? message.get("method").getAsString() : null;
        JsonElement id = message.get("id");
        boolean isNotification = (id == null || id.isJsonNull());

        if (method == null)
        {
            if (!isNotification)
            {
                writeError(id, METHOD_NOT_FOUND, "Missing method");
            }
            return;
        }

        if (method.startsWith("notifications/"))
        {
            LOGGER.debug("Ignoring notification: {}", method);
            return;
        }
        if (isNotification)
        {
            LOGGER.debug("Ignoring notification with request method: {}", method);
            return;
        }

        JsonObject params = message.has("params") && message.get("params").isJsonObject()
                ? message.getAsJsonObject("params")
                : new JsonObject();
        switch (method)
        {
            case "initialize":
                writeResult(id, initializeResult(params));
                break;
            case "ping":
                writeResult(id, new JsonObject());
                break;
            case "tools/list":
                writeResult(id, toolsListResult());
                break;
            case "tools/call":
                handleToolCall(id, params);
                break;
            default:
                writeError(id, METHOD_NOT_FOUND, "Method not supported: " + method);
                break;
        }
    }

    private JsonObject initializeResult(JsonObject params)
    {
        String protocolVersion = params.has("protocolVersion") && params.get("protocolVersion").isJsonPrimitive()
                ? params.get("protocolVersion").getAsString()
                : DEFAULT_PROTOCOL_VERSION;
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", protocolVersion);
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "legend-pure-mcp-server");
        serverInfo.addProperty("version", this.serverVersion);
        result.add("serverInfo", serverInfo);
        return result;
    }

    private JsonObject toolsListResult()
    {
        JsonArray tools = new JsonArray();
        for (McpTool tool : this.registry.list())
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", tool.getName());
            entry.addProperty("description", tool.getDescription());
            entry.add("inputSchema", tool.getInputSchema());
            tools.add(entry);
        }
        JsonObject result = new JsonObject();
        result.add("tools", tools);
        return result;
    }

    private void handleToolCall(JsonElement id, JsonObject params)
    {
        String name = params.has("name") && params.get("name").isJsonPrimitive()
                ? params.get("name").getAsString()
                : null;
        if (name == null || this.registry.get(name) == null)
        {
            writeError(id, INVALID_PARAMS, "Unknown tool: " + name);
            return;
        }
        JsonObject arguments = params.has("arguments") && params.get("arguments").isJsonObject()
                ? params.getAsJsonObject("arguments")
                : new JsonObject();
        ToolResult toolResult = this.registry.call(name, arguments);

        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", toolResult.getText());
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject result = new JsonObject();
        result.add("content", contents);
        result.addProperty("isError", toolResult.isError());
        writeResult(id, result);
    }

    private void writeResult(JsonElement id, JsonObject result)
    {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        writeLine(response);
    }

    private void writeError(JsonElement id, int code, String errorMessage)
    {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", errorMessage);
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", (id == null) ? JsonNull.INSTANCE : id);
        response.add("error", error);
        writeLine(response);
    }

    private void writeLine(JsonObject response)
    {
        this.out.println(this.gson.toJson(response));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=McpStdioServerTest -DfailIfNoTests=false`
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add MCP stdio transport (newline-delimited JSON-RPC 2.0)"
```

---

### Task 4: `WorkspaceSync` — disk-as-source-of-truth change detection

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/WorkspaceSync.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/WorkspaceSyncTest.java`
- Modify: `McpTestSuite.java` (add `WorkspaceSyncTest.class`)

**Interfaces:**
- Consumes: `RepositoryScanner.getMappings()` (`Map<String, Path>` repoName → `.../src/main/resources` root; the repo's sources live under `<resourcesRoot>/<repoName>/`), `RepositoryScanner.deriveSourceIdFromPath(Path)` (→ `"/repoName/rel/path.pure"`), `LegendPureSession.FileChange(String sourceId, String content, FileChangeType)` with `FileChangeType.CREATE_OR_MODIFY` / `FileChangeType.DELETE`, and getters `getSourceId()`, `getContent()`, `getType()`.
- Produces (used by Tasks 5, 7):
  - `WorkspaceSync(RepositoryScanner scanner)`
  - `void seed() throws IOException` — record current disk state as already-known (call once after session init)
  - `List<LegendPureSession.FileChange> computeChanges() throws IOException` — modified/new files as `CREATE_OR_MODIFY` (content = current file text, UTF-8), vanished files as `DELETE` (content null); does NOT mutate the known state
  - `void markApplied(List<LegendPureSession.FileChange> changes)` — fold applied changes into the known state (call only after a successful compile, so failed compiles are retried on the next sync)

- [ ] **Step 1: Write the failing test**

`WorkspaceSyncTest.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorkspaceSyncTest
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path repoDir;
    private RepositoryScanner scanner;
    private WorkspaceSync sync;

    @Before
    public void setUp() throws IOException
    {
        Path workspaceRoot = this.tmp.getRoot().toPath();
        Path resourcesDir = workspaceRoot.resolve("module/src/main/resources");
        this.repoDir = resourcesDir.resolve("sync_test_repo");
        Files.createDirectories(this.repoDir.resolve("model"));
        Files.write(resourcesDir.resolve("sync_test_repo.definition.json"),
                ("{\"name\":\"sync_test_repo\","
                        + "\"pattern\":\"(test::sync)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));
        writePure("model/A.pure", "Class test::sync::A\n{\n  name: String[1];\n}\n");

        this.scanner = new RepositoryScanner();
        this.scanner.scan(Collections.singletonList(workspaceRoot));
        this.sync = new WorkspaceSync(this.scanner);
        this.sync.seed();
    }

    private void writePure(String relativePath, String content) throws IOException
    {
        Path file = this.repoDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void noChangesAfterSeed() throws IOException
    {
        Assert.assertTrue(this.sync.computeChanges().isEmpty());
    }

    @Test
    public void modifiedFileDetected() throws IOException
    {
        String newContent = "Class test::sync::A\n{\n  name: String[1];\n  age: Integer[1];\n}\n";
        writePure("model/A.pure", newContent);

        List<LegendPureSession.FileChange> changes = this.sync.computeChanges();
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("/sync_test_repo/model/A.pure", changes.get(0).getSourceId());
        Assert.assertEquals(newContent, changes.get(0).getContent());
        Assert.assertEquals(LegendPureSession.FileChangeType.CREATE_OR_MODIFY, changes.get(0).getType());
    }

    @Test
    public void newAndDeletedFilesDetected() throws IOException
    {
        writePure("model/B.pure", "Class test::sync::B\n{\n}\n");
        Files.delete(this.repoDir.resolve("model/A.pure"));

        List<LegendPureSession.FileChange> changes = this.sync.computeChanges();
        Assert.assertEquals(2, changes.size());

        LegendPureSession.FileChange created = null;
        LegendPureSession.FileChange deleted = null;
        for (LegendPureSession.FileChange change : changes)
        {
            if (change.getType() == LegendPureSession.FileChangeType.CREATE_OR_MODIFY)
            {
                created = change;
            }
            else
            {
                deleted = change;
            }
        }
        Assert.assertNotNull(created);
        Assert.assertEquals("/sync_test_repo/model/B.pure", created.getSourceId());
        Assert.assertNotNull(deleted);
        Assert.assertEquals("/sync_test_repo/model/A.pure", deleted.getSourceId());
    }

    @Test
    public void computeChangesIsIdempotentUntilMarkApplied() throws IOException
    {
        writePure("model/A.pure", "Class test::sync::A\n{\n  renamed: String[1];\n}\n");

        List<LegendPureSession.FileChange> first = this.sync.computeChanges();
        Assert.assertEquals("computeChanges must not mutate state", 1, this.sync.computeChanges().size());

        this.sync.markApplied(first);
        Assert.assertTrue("After markApplied the change is known", this.sync.computeChanges().isEmpty());
    }
}
```

Add `WorkspaceSyncTest.class` to `McpTestSuite`.

- [ ] **Step 2: Run test to verify it fails**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=WorkspaceSyncTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (`WorkspaceSync` doesn't exist).

- [ ] **Step 3: Write the implementation**

`WorkspaceSync.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disk is the source of truth: agents edit .pure files with their own tools, and this
 * class detects what changed since the last successful sync so the session can be
 * updated with one bulk compile. No file watching - sync is computed on demand.
 */
public class WorkspaceSync
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceSync.class);

    private final RepositoryScanner scanner;
    private final Map<String, String> knownHashes = new HashMap<>();

    public WorkspaceSync(RepositoryScanner scanner)
    {
        this.scanner = scanner;
    }

    public void seed() throws IOException
    {
        this.knownHashes.clear();
        this.knownHashes.putAll(scanDisk());
        LOGGER.info("WorkspaceSync seeded with {} sources", this.knownHashes.size());
    }

    public List<LegendPureSession.FileChange> computeChanges() throws IOException
    {
        Map<String, String> onDisk = scanDisk();
        List<LegendPureSession.FileChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> entry : onDisk.entrySet())
        {
            String sourceId = entry.getKey();
            if (!entry.getValue().equals(this.knownHashes.get(sourceId)))
            {
                Path file = this.scanner.resolve(sourceId);
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                changes.add(new LegendPureSession.FileChange(
                        sourceId, content, LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
            }
        }
        for (String knownId : this.knownHashes.keySet())
        {
            if (!onDisk.containsKey(knownId))
            {
                changes.add(new LegendPureSession.FileChange(
                        knownId, null, LegendPureSession.FileChangeType.DELETE));
            }
        }
        return changes;
    }

    public void markApplied(List<LegendPureSession.FileChange> changes)
    {
        for (LegendPureSession.FileChange change : changes)
        {
            if (change.getType() == LegendPureSession.FileChangeType.DELETE)
            {
                this.knownHashes.remove(change.getSourceId());
            }
            else
            {
                this.knownHashes.put(change.getSourceId(), hash(change.getContent()));
            }
        }
    }

    private Map<String, String> scanDisk() throws IOException
    {
        Map<String, String> hashes = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : this.scanner.getMappings().entrySet())
        {
            Path repoDir = entry.getValue().resolve(entry.getKey());
            if (!Files.isDirectory(repoDir))
            {
                continue;
            }
            Files.walkFileTree(repoDir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if (file.getFileName().toString().endsWith(".pure"))
                    {
                        String sourceId = WorkspaceSync.this.scanner.deriveSourceIdFromPath(file);
                        if (sourceId != null)
                        {
                            hashes.put(sourceId,
                                    hash(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return hashes;
    }

    private static String hash(String content)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
            {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=WorkspaceSyncTest -DfailIfNoTests=false`
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add WorkspaceSync for disk-as-source-of-truth change detection"
```

---

### Task 5: `InitGate` + compile/execute tools (`PureTools` part 1) with runtime-backed tests

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/InitGate.java`
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/PureTools.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/PureToolsIntegrationTest.java`
- Modify: `McpTestSuite.java` (add `PureToolsIntegrationTest.class`)

**Interfaces:**
- Consumes (from `legend-pure-lsp-server`): `LegendPureSession` (`applyBulkChangesAndCompile(List<FileChange>) → CompileResult`, `executeGo() → ExecuteResult`, `executeFunction(String) → ExecuteResult`, `withGraphReadLock(Supplier<T>)`, `getPureRuntime()`, `resolveSourceId(String)`); `CompileResult.isSuccess()/getError()` (an `Exception`); `ExecuteResult.isSuccess()/getError()/getOutput()`; `org.finos.legend.pure.m4.exception.PureException.findPureException(Throwable)` with `getSourceInformation()/getInfo()/getMessage()`; `SourceInformation.getSourceId()/getStartLine()/getStartColumn()/getEndLine()`; `RepositoryScanner.resolve(String sourceId) → Path` (null if not on disk); Task 2's registry types; Task 4's `WorkspaceSync`.
- Produces (used by Tasks 6, 7):
  - `InitGate`: `void ready()`, `void fail(String message)`, `String await()` (blocks until ready/failed; returns null when ready, else the failure message)
  - `PureTools.buildRegistry(LegendPureSession session, RepositoryScanner scanner, WorkspaceSync sync, UriMapper uriMapper, WorkspaceSymbolProvider symbols, InitGate gate) → PureToolRegistry` — registers all tools (this task: `pure_compile`, `pure_execute`; Task 6 adds the rest inside the same method)
  - Package-private helpers on `PureTools` reused by Task 6: `static String requiredString(JsonObject arguments, String name)` (returns trimmed value or throws `IllegalArgumentException`), `static JsonObject objectSchema(JsonObject properties, String... required)`, `static String describeLocation(SourceInformation si, RepositoryScanner scanner)` (formats `<abs-path-or-sourceId>:<line>:<col>`)

**Note on test cost:** this test class initializes a real Pure runtime once (`@BeforeClass`, tens of seconds). All compile/execute/navigation assertions share that one session. Task 6 adds its tests to this same class.

- [ ] **Step 1: Write the failing test**

`PureToolsIntegrationTest.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import com.google.gson.JsonObject;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PureToolsIntegrationTest
{
    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();

    private static Path repoDir;
    private static PureToolRegistry registry;

    @BeforeClass
    public static void initSession() throws IOException
    {
        Path workspaceRoot = tmp.getRoot().toPath();
        Path resourcesDir = workspaceRoot.resolve("module/src/main/resources");
        repoDir = resourcesDir.resolve("mcp_test_repo");
        Files.createDirectories(repoDir.resolve("model"));
        Files.write(resourcesDir.resolve("mcp_test_repo.definition.json"),
                ("{\"name\":\"mcp_test_repo\","
                        + "\"pattern\":\"(test::mcp)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));
        writePure("model/Person.pure",
                "Class test::mcp::McpPerson\n{\n  fullName: String[1];\n}\n");

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(Collections.singletonList(workspaceRoot));

        LegendPureSession session = new LegendPureSession();
        session.initialize(scanner);
        Assert.assertTrue(session.isInitialized());

        UriMapper uriMapper = new UriMapper();
        uriMapper.setRepositoryScanner(scanner);
        uriMapper.setPureRuntime(session.getPureRuntime());

        WorkspaceSymbolProvider symbols = new WorkspaceSymbolProvider();
        symbols.buildIndex(session.getPureRuntime());

        WorkspaceSync sync = new WorkspaceSync(scanner);
        sync.seed();

        InitGate gate = new InitGate();
        gate.ready();

        registry = PureTools.buildRegistry(session, scanner, sync, uriMapper, symbols, gate);
    }

    static void writePure(String relativePath, String content) throws IOException
    {
        Path file = repoDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    static ToolResult call(String tool, String... keyValues)
    {
        JsonObject arguments = new JsonObject();
        for (int i = 0; i < keyValues.length; i += 2)
        {
            arguments.addProperty(keyValues[i], keyValues[i + 1]);
        }
        return registry.call(tool, arguments);
    }

    @Test
    public void compileReportsNoChangesWhenClean()
    {
        ToolResult result = call("pure_compile");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue(result.getText().contains("No source changes"));
    }

    @Test
    public void editCompileFixExecuteLoop() throws IOException
    {
        // 1. Introduce a broken file on disk, as an agent's file edit would
        writePure("model/broken_loop.pure",
                "Class test::mcp::LoopHolder\n{\n  bad: NoSuchTypeXyz[1];\n}\n");
        ToolResult broken = call("pure_compile");
        Assert.assertTrue("Broken code must produce an error result", broken.isError());
        Assert.assertTrue("Diagnostics must carry the source location, got: " + broken.getText(),
                broken.getText().contains("broken_loop.pure"));
        Assert.assertTrue("Diagnostics must mention the unknown type, got: " + broken.getText(),
                broken.getText().contains("NoSuchTypeXyz"));

        // 2. Fix the file and add a go() entry point
        writePure("model/broken_loop.pure",
                "Class test::mcp::LoopHolder\n{\n  good: String[1];\n}\n"
                        + "\n"
                        + "function go():Any[*]\n{\n  print('loop-complete', 1);\n}\n");
        ToolResult fixed = call("pure_compile");
        Assert.assertFalse("Fixed code must compile, got: " + fixed.getText(), fixed.isError());
        Assert.assertTrue(fixed.getText().contains("/mcp_test_repo/model/broken_loop.pure"));

        // 3. Execute go() on the interpreted engine
        ToolResult executed = call("pure_execute");
        Assert.assertFalse("go() must execute, got: " + executed.getText(), executed.isError());
        Assert.assertTrue("Output must contain the printed text, got: " + executed.getText(),
                executed.getText().contains("loop-complete"));

        // 4. Clean up for other tests: remove go() again
        writePure("model/broken_loop.pure",
                "Class test::mcp::LoopHolder\n{\n  good: String[1];\n}\n");
        ToolResult cleaned = call("pure_compile");
        Assert.assertFalse(cleaned.isError());
    }

    @Test
    public void executeNamedFunction() throws IOException
    {
        writePure("model/named_fn.pure",
                "function test::mcp::mcpNamedEntry():String[1]\n{\n  'named-entry-ran'\n}\n");
        ToolResult compiled = call("pure_compile");
        Assert.assertFalse("Got: " + compiled.getText(), compiled.isError());

        ToolResult executed = call("pure_execute", "function", "test::mcp::mcpNamedEntry");
        Assert.assertFalse("Got: " + executed.getText(), executed.isError());
        Assert.assertTrue("Got: " + executed.getText(), executed.getText().contains("named-entry-ran"));
    }

    @Test
    public void executeSurfacesPureStackTraceOnFailure() throws IOException
    {
        writePure("model/failing_fn.pure",
                "function test::mcp::mcpFailingEntry():Any[*]\n{\n  fail('mcp-boom');\n}\n");
        ToolResult compiled = call("pure_compile");
        Assert.assertFalse("Got: " + compiled.getText(), compiled.isError());

        ToolResult executed = call("pure_execute", "function", "test::mcp::mcpFailingEntry");
        Assert.assertTrue("Failure must be an error result", executed.isError());
        Assert.assertTrue("Must include the failure text, got: " + executed.getText(),
                executed.getText().contains("mcp-boom"));
    }

    @Test
    public void executeWithBrokenWorkspaceReturnsCompileDiagnostics() throws IOException
    {
        writePure("model/broken_exec.pure",
                "Class test::mcp::BrokenExec\n{\n  bad: NoSuchTypeAbc[1];\n}\n");
        ToolResult executed = call("pure_execute", "function", "test::mcp::mcpNamedEntry");
        Assert.assertTrue("Broken workspace must fail before executing", executed.isError());
        Assert.assertTrue("Must report the compile problem, got: " + executed.getText(),
                executed.getText().contains("NoSuchTypeAbc"));

        Files.delete(repoDir.resolve("model/broken_exec.pure"));
        ToolResult cleaned = call("pure_compile");
        Assert.assertFalse("Got: " + cleaned.getText(), cleaned.isError());
    }
}
```

Add `PureToolsIntegrationTest.class` to `McpTestSuite`.

- [ ] **Step 2: Run test to verify it fails**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolsIntegrationTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (`InitGate`, `PureTools` don't exist).

- [ ] **Step 3: Write the implementation**

`InitGate.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.util.concurrent.CountDownLatch;

/**
 * Runtime initialization runs on a background thread so the MCP handshake responds
 * immediately; tool handlers block here until the session is usable (or init failed).
 */
public class InitGate
{
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile String failure;

    public void ready()
    {
        this.latch.countDown();
    }

    public void fail(String message)
    {
        this.failure = message;
        this.latch.countDown();
    }

    /**
     * Blocks until initialization finishes. Returns null when the session is ready,
     * otherwise a message describing why initialization failed.
     */
    public String await()
    {
        try
        {
            this.latch.await();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "Interrupted while waiting for Pure runtime initialization";
        }
        return this.failure;
    }
}
```

`PureTools.java` (this task registers `pure_compile` and `pure_execute`; Task 6 extends `buildRegistry` in place):

```java
package org.finos.legend.pure.lsp.mcp;

import java.nio.file.Path;
import java.util.List;
import com.google.gson.JsonObject;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;

/**
 * The MCP tool surface over a LegendPureSession. Transport-neutral: the registry
 * built here can be served over stdio (this module) or embedded in the LSP server
 * later. Disk is the source of truth - compile/execute sync changed files first.
 */
public final class PureTools
{
    private PureTools()
    {
    }

    public static PureToolRegistry buildRegistry(LegendPureSession session, RepositoryScanner scanner,
                                                 WorkspaceSync sync, UriMapper uriMapper,
                                                 WorkspaceSymbolProvider symbols, InitGate gate)
    {
        PureToolRegistry registry = new PureToolRegistry();

        registry.register(new McpTool(
                "pure_compile",
                "Sync all .pure files in the workspace from disk and compile them. Call after "
                        + "editing .pure files. Returns compile errors with file:line:column on failure.",
                objectSchema(new JsonObject()),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    StringBuilder report = new StringBuilder();
                    String compileError = syncAndCompile(session, scanner, sync, symbols, report);
                    if (compileError != null)
                    {
                        return ToolResult.error(compileError);
                    }
                    return ToolResult.ok(report.toString());
                }));

        JsonObject executeProps = new JsonObject();
        executeProps.add("function", stringProp(
                "Pure path of a zero-argument function to run, e.g. 'my::pkg::myFunc' or "
                        + "'my::pkg::myFunc():Any[*]'. Omit to run the workspace go() function."));
        registry.register(new McpTool(
                "pure_execute",
                "Compile the workspace (syncing from disk first), then run a zero-argument function "
                        + "on the interpreted engine. Typical fast loop: write 'function go():Any[*] "
                        + "{ ... }' in any workspace .pure file, then call this tool with no arguments. "
                        + "Returns console output, or the error plus Pure stack trace on failure.",
                objectSchema(executeProps),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    StringBuilder report = new StringBuilder();
                    String compileError = syncAndCompile(session, scanner, sync, symbols, report);
                    if (compileError != null)
                    {
                        return ToolResult.error(compileError);
                    }

                    String function = optionalString(arguments, "function");
                    LegendPureSession.ExecuteResult result = (function == null)
                            ? session.executeGo()
                            : session.executeFunction(function);
                    if (!result.isSuccess())
                    {
                        StringBuilder error = new StringBuilder("Execution failed: ").append(result.getError());
                        if (result.getOutput() != null && !result.getOutput().isEmpty())
                        {
                            error.append('\n').append(result.getOutput());
                        }
                        return ToolResult.error(error.toString());
                    }
                    String output = result.getOutput();
                    return ToolResult.ok((output == null || output.isEmpty()) ? "(no output)" : output);
                }));

        return registry;
    }

    /**
     * Sync disk changes into the session and compile. Returns null on success (appending a
     * summary to report), or the formatted compile diagnostics on failure. On failure the
     * sync state is NOT advanced, so the next call re-detects and re-compiles the same files.
     */
    private static String syncAndCompile(LegendPureSession session, RepositoryScanner scanner,
                                         WorkspaceSync sync, WorkspaceSymbolProvider symbols,
                                         StringBuilder report) throws Exception
    {
        List<LegendPureSession.FileChange> changes = sync.computeChanges();
        if (changes.isEmpty())
        {
            report.append("No source changes since last sync; workspace already compiled.");
            return null;
        }
        LegendPureSession.CompileResult result = session.applyBulkChangesAndCompile(changes);
        if (!result.isSuccess())
        {
            return formatCompileError(result.getError(), scanner);
        }
        sync.markApplied(changes);
        symbols.buildIndex(session.getPureRuntime());
        report.append("Compiled ").append(changes.size()).append(" changed file(s):");
        for (LegendPureSession.FileChange change : changes)
        {
            report.append("\n - ").append(change.getSourceId());
            if (change.getType() == LegendPureSession.FileChangeType.DELETE)
            {
                report.append(" (deleted)");
            }
        }
        return null;
    }

    static String formatCompileError(Exception e, RepositoryScanner scanner)
    {
        PureException pureException = PureException.findPureException(e);
        if (pureException == null)
        {
            return "Compile failed: " + ((e.getMessage() != null) ? e.getMessage() : e.toString());
        }
        String message = pureException.getInfo();
        if (message == null || message.isEmpty())
        {
            message = pureException.getMessage();
        }
        SourceInformation si = pureException.getSourceInformation();
        if (si == null)
        {
            return "Compile failed: " + message;
        }
        return "Compile failed:\n" + describeLocation(si, scanner) + " " + message;
    }

    static String describeLocation(SourceInformation si, RepositoryScanner scanner)
    {
        Path onDisk = scanner.resolve(si.getSourceId());
        String where = (onDisk != null) ? onDisk.toString() : si.getSourceId();
        return where + ":" + si.getStartLine() + ":" + si.getStartColumn();
    }

    static JsonObject objectSchema(JsonObject properties, String... required)
    {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        if (required.length > 0)
        {
            com.google.gson.JsonArray requiredArray = new com.google.gson.JsonArray();
            for (String name : required)
            {
                requiredArray.add(name);
            }
            schema.add("required", requiredArray);
        }
        return schema;
    }

    static JsonObject stringProp(String description)
    {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        prop.addProperty("description", description);
        return prop;
    }

    static String requiredString(JsonObject arguments, String name)
    {
        String value = optionalString(arguments, name);
        if (value == null)
        {
            throw new IllegalArgumentException("Missing required argument: " + name);
        }
        return value;
    }

    static String optionalString(JsonObject arguments, String name)
    {
        if (arguments == null || !arguments.has(name) || !arguments.get(name).isJsonPrimitive())
        {
            return null;
        }
        String value = arguments.get(name).getAsString().trim();
        return value.isEmpty() ? null : value;
    }
}
```

Note: `McpTool.Handler.execute` declares `throws Exception`, so `syncAndCompile`'s `throws Exception` (from `computeChanges()`'s `IOException`) propagates into the lambda fine; `PureToolRegistry.call` converts anything thrown into an error result.

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolsIntegrationTest -DfailIfNoTests=false`
Expected: 5 tests PASS (slow — one real runtime init).

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add pure_compile and pure_execute MCP tools over LegendPureSession"
```

---

### Task 6: Navigation tools (`PureTools` part 2)

**Files:**
- Modify: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/PureTools.java` (extend `buildRegistry`)
- Modify (add tests): `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/PureToolsIntegrationTest.java`

**Interfaces:**
- Consumes (all from `legend-pure-lsp-server` / lsp4j, in addition to Task 5's list): `PureRuntime.getCoreInstance(String userPath) → CoreInstance` (null/throws for unknown paths; `"::"` is the root package), `CoreInstance.getClassifier().getName()`, `CoreInstance.getSourceInformation()`; `org.finos.legend.pure.lsp.PackageTreeProvider.getChildren(PureRuntime, UriMapper, String packagePath) → List<PackageChildInfo>` (`getName()/getQualifiedPath()/getKind()/getIsPackage()/getChildCount()/getLine()`); `org.finos.legend.pure.lsp.ReferencesProvider.references(PureRuntime, UriMapper, String sourceId, int line, int column, boolean includeDeclaration) → List<org.eclipse.lsp4j.Location>` (`getUri()`, `getRange().getStart().getLine()/getCharacter()` — 0-based); `WorkspaceSymbolProvider.search(UriMapper, String query, int maxResults) → List<org.eclipse.lsp4j.SymbolInformation>` (`getName()/getKind()/getLocation()/getContainerName()`); `PureRuntime.getSourceById(String).getContent()`; `LegendPureSession.resolveSourceId(String)`.
- Produces: five more tools registered inside `PureTools.buildRegistry`: `pure_find_element`, `pure_find_usages`, `pure_list_package`, `pure_search_symbols`, `pure_get_source`.

- [ ] **Step 1: Add the failing tests**

Append to `PureToolsIntegrationTest.java` (inside the class):

```java
    @Test
    public void findElementReturnsLocationAndDefinition()
    {
        ToolResult result = call("pure_find_element", "path", "test::mcp::McpPerson");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue("Must name the kind, got: " + result.getText(),
                result.getText().contains("Class"));
        Assert.assertTrue("Must include the location, got: " + result.getText(),
                result.getText().contains("Person.pure"));
        Assert.assertTrue("Must include the definition text, got: " + result.getText(),
                result.getText().contains("fullName: String[1]"));
    }

    @Test
    public void findElementUnknownPathIsError()
    {
        ToolResult result = call("pure_find_element", "path", "test::mcp::DoesNotExist");
        Assert.assertTrue(result.isError());
        Assert.assertTrue("Should suggest pure_search_symbols, got: " + result.getText(),
                result.getText().contains("pure_search_symbols"));
    }

    @Test
    public void findElementWrongPackageSuggestsNearMiss()
    {
        ToolResult result = call("pure_find_element", "path", "test::wrongpkg::McpPerson");
        Assert.assertTrue(result.isError());
        Assert.assertTrue("Should offer a near-miss from the symbol index, got: " + result.getText(),
                result.getText().contains("Did you mean"));
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("McpPerson"));
    }

    @Test
    public void findUsagesFindsFunctionCallSites() throws IOException
    {
        writePure("model/usages.pure",
                "function test::mcp::mcpUsedFunction():String[1]\n{\n  'used'\n}\n"
                        + "\n"
                        + "function test::mcp::mcpCallerFunction():String[1]\n{\n  test::mcp::mcpUsedFunction()\n}\n");
        ToolResult compiled = call("pure_compile");
        Assert.assertFalse("Got: " + compiled.getText(), compiled.isError());

        ToolResult result = call("pure_find_usages", "path", "test::mcp::mcpUsedFunction");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue("Usage location should be in usages.pure, got: " + result.getText(),
                result.getText().contains("usages.pure"));
    }

    @Test
    public void listPackageShowsChildren()
    {
        ToolResult result = call("pure_list_package", "package", "test::mcp");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("McpPerson"));
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("Class"));
    }

    @Test
    public void searchSymbolsFindsClassByFragment()
    {
        ToolResult result = call("pure_search_symbols", "query", "McpPerson");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("McpPerson"));
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("Person.pure"));
    }

    @Test
    public void getSourceReturnsContent()
    {
        ToolResult result = call("pure_get_source", "sourceId", "/mcp_test_repo/model/Person.pure");
        Assert.assertFalse("Got: " + result.getText(), result.isError());
        Assert.assertTrue("Got: " + result.getText(), result.getText().contains("Class test::mcp::McpPerson"));
    }

    @Test
    public void getSourceUnknownIdIsError()
    {
        ToolResult result = call("pure_get_source", "sourceId", "/mcp_test_repo/model/Nope.pure");
        Assert.assertTrue(result.isError());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolsIntegrationTest -DfailIfNoTests=false`
Expected: the 8 new tests FAIL with `IllegalArgumentException: Unknown tool: pure_find_element` (etc.); the 5 Task-5 tests still PASS.

- [ ] **Step 3: Implement the five tools**

Add to `PureTools.buildRegistry(...)` before `return registry;` (new imports: `java.util.List` already present; add `org.eclipse.lsp4j.Location`, `org.eclipse.lsp4j.SymbolInformation`, `org.finos.legend.pure.lsp.PackageChildInfo`, `org.finos.legend.pure.lsp.PackageTreeProvider`, `org.finos.legend.pure.lsp.ReferencesProvider`, `org.finos.legend.pure.m3.serialization.runtime.PureRuntime`, `org.finos.legend.pure.m3.serialization.runtime.Source`, `org.finos.legend.pure.m4.coreinstance.CoreInstance`):

```java
        JsonObject pathProps = new JsonObject();
        pathProps.add("path", stringProp("Full Pure path of the element, e.g. 'my::pkg::MyClass'."));
        registry.register(new McpTool(
                "pure_find_element",
                "Resolve a Pure element by its full path and return its kind, source location, "
                        + "and definition text.",
                objectSchema(pathProps, "path"),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    String path = requiredString(arguments, "path");
                    return session.withGraphReadLock(() ->
                    {
                        CoreInstance element = findElement(session.getPureRuntime(), path);
                        if (element == null)
                        {
                            return ToolResult.error(unknownElementMessage(path, symbols, uriMapper));
                        }
                        String kind = (element.getClassifier() != null)
                                ? element.getClassifier().getName() : "Unknown";
                        StringBuilder text = new StringBuilder(kind).append(' ').append(path);
                        SourceInformation si = element.getSourceInformation();
                        if (si == null)
                        {
                            text.append("\n(no source information available)");
                            return ToolResult.ok(text.toString());
                        }
                        text.append('\n').append(describeLocation(si, scanner))
                                .append("  (sourceId: ").append(si.getSourceId()).append(')');
                        Source source = session.getPureRuntime().getSourceById(si.getSourceId());
                        if (source != null)
                        {
                            text.append("\n\n").append(extractLines(
                                    source.getContent(), si.getStartLine(), si.getEndLine()));
                        }
                        return ToolResult.ok(text.toString());
                    });
                }));

        registry.register(new McpTool(
                "pure_find_usages",
                "Find all usages of a Pure element (given by full path) across the compiled workspace.",
                objectSchema(pathProps, "path"),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    String path = requiredString(arguments, "path");
                    return session.withGraphReadLock(() ->
                    {
                        PureRuntime runtime = session.getPureRuntime();
                        CoreInstance element = findElement(runtime, path);
                        if (element == null)
                        {
                            return ToolResult.error(unknownElementMessage(path, symbols, uriMapper));
                        }
                        SourceInformation si = element.getSourceInformation();
                        if (si == null)
                        {
                            return ToolResult.error("Element '" + path + "' has no source information.");
                        }
                        List<Location> locations = ReferencesProvider.references(runtime, uriMapper,
                                si.getSourceId(), si.getStartLine(), si.getStartColumn(), false);
                        if (locations.isEmpty())
                        {
                            // The declaration's start position may point at a keyword rather than the
                            // element's name token; retry at the name's column on the same line.
                            int nameColumn = findNameColumn(runtime, si, path);
                            if (nameColumn > 0)
                            {
                                locations = ReferencesProvider.references(runtime, uriMapper,
                                        si.getSourceId(), si.getStartLine(), nameColumn, false);
                            }
                        }
                        if (locations.isEmpty())
                        {
                            return ToolResult.ok("No usages found for '" + path + "'.");
                        }
                        StringBuilder text = new StringBuilder("Usages of ").append(path).append(':');
                        for (Location location : locations)
                        {
                            text.append("\n - ").append(formatLspLocation(location));
                        }
                        return ToolResult.ok(text.toString());
                    });
                }));

        JsonObject packageProps = new JsonObject();
        packageProps.add("package", stringProp(
                "Package path to list, e.g. 'my::pkg'. Omit for the root package."));
        registry.register(new McpTool(
                "pure_list_package",
                "List the children (subpackages and elements) of a Pure package.",
                objectSchema(packageProps),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    String packagePath = optionalString(arguments, "package");
                    String effectivePath = (packagePath == null) ? "::" : packagePath;
                    return session.withGraphReadLock(() ->
                    {
                        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                                session.getPureRuntime(), uriMapper, effectivePath);
                        if (children.isEmpty())
                        {
                            return ToolResult.ok("Package '" + effectivePath
                                    + "' has no children (or does not exist).");
                        }
                        StringBuilder text = new StringBuilder("Children of ").append(effectivePath).append(':');
                        for (PackageChildInfo child : children)
                        {
                            text.append("\n - ").append(child.getKind()).append(' ')
                                    .append(child.getQualifiedPath());
                            if (child.getIsPackage())
                            {
                                text.append(" (").append(child.getChildCount()).append(" children)");
                            }
                        }
                        return ToolResult.ok(text.toString());
                    });
                }));

        JsonObject searchProps = new JsonObject();
        searchProps.add("query", stringProp("Case-insensitive name fragment to search for."));
        JsonObject maxResultsProp = new JsonObject();
        maxResultsProp.addProperty("type", "integer");
        maxResultsProp.addProperty("description", "Maximum results to return (default 50).");
        searchProps.add("maxResults", maxResultsProp);
        registry.register(new McpTool(
                "pure_search_symbols",
                "Search all compiled elements (classes, functions, enums, ...) by name fragment.",
                objectSchema(searchProps, "query"),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    String query = requiredString(arguments, "query");
                    int maxResults = (arguments.has("maxResults") && arguments.get("maxResults").isJsonPrimitive())
                            ? arguments.get("maxResults").getAsInt()
                            : 50;
                    List<SymbolInformation> results = symbols.search(uriMapper, query, maxResults);
                    if (results.isEmpty())
                    {
                        return ToolResult.ok("No symbols match '" + query + "'.");
                    }
                    StringBuilder text = new StringBuilder("Symbols matching '").append(query).append("':");
                    for (SymbolInformation symbol : results)
                    {
                        text.append("\n - ").append(symbol.getKind()).append(' ');
                        if (symbol.getContainerName() != null && !symbol.getContainerName().isEmpty())
                        {
                            text.append(symbol.getContainerName()).append("::");
                        }
                        text.append(symbol.getName())
                                .append("  ").append(formatLspLocation(symbol.getLocation()));
                    }
                    return ToolResult.ok(text.toString());
                }));

        JsonObject sourceProps = new JsonObject();
        sourceProps.add("sourceId", stringProp(
                "Pure source id, e.g. '/my_repo/model/File.pure'. Works for platform/library "
                        + "sources that have no file on disk."));
        registry.register(new McpTool(
                "pure_get_source",
                "Get the full content of a Pure source by its source id.",
                objectSchema(sourceProps, "sourceId"),
                arguments ->
                {
                    String initError = gate.await();
                    if (initError != null)
                    {
                        return ToolResult.error(initError);
                    }
                    String sourceId = requiredString(arguments, "sourceId");
                    String id = sourceId.startsWith("pure://")
                            ? sourceId.substring("pure://".length())
                            : sourceId;
                    return session.withGraphReadLock(() ->
                    {
                        String resolvedId = session.resolveSourceId(id);
                        Source source = (resolvedId == null)
                                ? null
                                : session.getPureRuntime().getSourceById(resolvedId);
                        if (source == null)
                        {
                            return ToolResult.error("Unknown source id: " + sourceId);
                        }
                        return ToolResult.ok(source.getContent());
                    });
                }));
```

And add these private helpers to `PureTools`:

```java
    private static CoreInstance findElement(PureRuntime runtime, String path)
    {
        try
        {
            return runtime.getCoreInstance(path);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    /**
     * Spec: unknown paths return near-miss suggestions from the symbol index when available.
     */
    private static String unknownElementMessage(String path, WorkspaceSymbolProvider symbols,
                                                UriMapper uriMapper)
    {
        StringBuilder text = new StringBuilder("No element found at path '").append(path)
                .append("'. Use pure_search_symbols to locate elements by name fragment.");
        int separator = path.lastIndexOf("::");
        String simpleName = (separator < 0) ? path : path.substring(separator + 2);
        List<SymbolInformation> nearMisses = symbols.search(uriMapper, simpleName, 5);
        if (!nearMisses.isEmpty())
        {
            text.append("\nDid you mean:");
            for (SymbolInformation symbol : nearMisses)
            {
                text.append("\n - ");
                if (symbol.getContainerName() != null && !symbol.getContainerName().isEmpty())
                {
                    text.append(symbol.getContainerName()).append("::");
                }
                text.append(symbol.getName());
            }
        }
        return text.toString();
    }

    /**
     * 1-based column of the element's simple name on its declaration's first line,
     * or -1 if not found.
     */
    private static int findNameColumn(PureRuntime runtime, SourceInformation si, String path)
    {
        Source source = runtime.getSourceById(si.getSourceId());
        if (source == null)
        {
            return -1;
        }
        String[] lines = source.getContent().split("\n", -1);
        if (si.getStartLine() < 1 || si.getStartLine() > lines.length)
        {
            return -1;
        }
        int separator = path.lastIndexOf("::");
        String simpleName = (separator < 0) ? path : path.substring(separator + 2);
        int index = lines[si.getStartLine() - 1].indexOf(simpleName);
        return (index < 0) ? -1 : (index + 1);
    }

    private static String extractLines(String content, int startLine, int endLine)
    {
        String[] lines = content.split("\n", -1);
        int start = Math.max(startLine, 1);
        int end = Math.min(Math.max(endLine, start), lines.length);
        StringBuilder text = new StringBuilder();
        for (int i = start; i <= end; i++)
        {
            if (text.length() > 0)
            {
                text.append('\n');
            }
            text.append(lines[i - 1]);
        }
        return text.toString();
    }

    private static String formatLspLocation(Location location)
    {
        String uri = location.getUri();
        String where = uri.startsWith("file://") ? uri.substring("file://".length()) : uri;
        return where + ":" + (location.getRange().getStart().getLine() + 1)
                + ":" + (location.getRange().getStart().getCharacter() + 1);
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest=PureToolsIntegrationTest -DfailIfNoTests=false`
Expected: all 13 tests PASS. If `findUsagesFindsFunctionCallSites` fails because `references(...)` returns empty even with the name-column fallback, debug by checking what `source.navigate(line, column, processorSupport)` returns at the declaration position (see `ReferencesProvider.references` internals) — adjust `findNameColumn` (e.g. search subsequent lines up to `si.getEndLine()`), not the test.

- [ ] **Step 5: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add MCP navigation tools: find_element, find_usages, list_package, search_symbols, get_source"
```

---

### Task 7: `LegendPureMcpServer` main class + end-to-end test

**Files:**
- Create: `legend-pure-lsp/legend-pure-mcp-server/src/main/java/org/finos/legend/pure/lsp/mcp/LegendPureMcpServer.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/LegendPureMcpServerTest.java`
- Test: `legend-pure-lsp/legend-pure-mcp-server/src/test/java/org/finos/legend/pure/lsp/mcp/McpEndToEndTest.java`
- Modify: `McpTestSuite.java` (add both new test classes)

**Interfaces:**
- Consumes: everything from Tasks 2–6.
- Produces:
  - `public static void main(String[] args)` — parses `--workspace <dir>` (default: current working directory), redirects `System.out`/`System.err` to stderr, serves MCP on real stdin/stdout
  - `static java.nio.file.Path resolveWorkspace(String[] args)` — package-private, unit-testable; throws `IllegalArgumentException` on `--workspace` without a value or a non-directory path
  - `static void serve(java.nio.file.Path workspace, InputStream in, OutputStream out) throws IOException` — the testable composition seam: wires scanner/session/sync/uriMapper/symbols/gate/registry, starts background init, runs the stdio loop until EOF

- [ ] **Step 1: Write the failing tests**

`LegendPureMcpServerTest.java` (fast, no runtime):

```java
package org.finos.legend.pure.lsp.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LegendPureMcpServerTest
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void workspaceArgIsParsed()
    {
        Path workspace = this.tmp.getRoot().toPath();
        Assert.assertEquals(workspace.toAbsolutePath().normalize(),
                LegendPureMcpServer.resolveWorkspace(new String[]{"--workspace", workspace.toString()}));
    }

    @Test
    public void defaultWorkspaceIsCwd()
    {
        Assert.assertEquals(Paths.get("").toAbsolutePath().normalize(),
                LegendPureMcpServer.resolveWorkspace(new String[]{}));
    }

    @Test
    public void missingWorkspaceValueRejected()
    {
        try
        {
            LegendPureMcpServer.resolveWorkspace(new String[]{"--workspace"});
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void nonDirectoryWorkspaceRejected()
    {
        try
        {
            LegendPureMcpServer.resolveWorkspace(
                    new String[]{"--workspace", "/no/such/dir/anywhere"});
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }
}
```

`McpEndToEndTest.java` (slow — full agent conversation over piped streams; the `initialize` response must arrive while the runtime is still initializing in the background, which is guaranteed here because responses are only read after requests are written, but the `tools/call` blocking on `InitGate` is genuinely exercised):

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class McpEndToEndTest
{
    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void fullAgentConversation() throws Exception
    {
        Path workspaceRoot = tmp.getRoot().toPath();
        Path resourcesDir = workspaceRoot.resolve("module/src/main/resources");
        Path repoDir = resourcesDir.resolve("e2e_repo");
        Files.createDirectories(repoDir.resolve("model"));
        Files.write(resourcesDir.resolve("e2e_repo.definition.json"),
                ("{\"name\":\"e2e_repo\","
                        + "\"pattern\":\"(test::e2e)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));
        Files.write(repoDir.resolve("model/main.pure"),
                ("Class test::e2e::E2eGreeting\n{\n  text: String[1];\n}\n"
                        + "\n"
                        + "function go():Any[*]\n{\n  print('e2e-hello', 1);\n}\n")
                        .getBytes(StandardCharsets.UTF_8));

        PipedOutputStream clientToServer = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientToServer);
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        Thread serverThread = new Thread(() ->
        {
            try
            {
                LegendPureMcpServer.serve(workspaceRoot, serverIn, serverOut);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }, "mcp-e2e-server");
        serverThread.setDaemon(true);
        serverThread.start();

        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(clientToServer, StandardCharsets.UTF_8), true);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientIn, StandardCharsets.UTF_8));

        writer.println("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"e2e\",\"version\":\"0\"}}}");
        JsonObject initResponse = JsonParser.parseString(reader.readLine()).getAsJsonObject();
        Assert.assertEquals("legend-pure-mcp-server", initResponse.getAsJsonObject("result")
                .getAsJsonObject("serverInfo").get("name").getAsString());

        writer.println("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

        writer.println("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        JsonObject listResponse = JsonParser.parseString(reader.readLine()).getAsJsonObject();
        Assert.assertEquals(7, listResponse.getAsJsonObject("result").getAsJsonArray("tools").size());

        // Blocks until background init completes, then compiles (no changes) and runs go()
        writer.println("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"pure_execute\",\"arguments\":{}}}");
        JsonObject executeResponse = JsonParser.parseString(reader.readLine()).getAsJsonObject();
        JsonObject executeResult = executeResponse.getAsJsonObject("result");
        String executeText = executeResult.getAsJsonArray("content").get(0)
                .getAsJsonObject().get("text").getAsString();
        Assert.assertFalse("Got: " + executeText, executeResult.get("isError").getAsBoolean());
        Assert.assertTrue("Got: " + executeText, executeText.contains("e2e-hello"));

        // Edit on disk, then compile through MCP
        Files.write(repoDir.resolve("model/extra.pure"),
                "Class test::e2e::E2eExtra\n{\n  n: Integer[1];\n}\n".getBytes(StandardCharsets.UTF_8));
        writer.println("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"pure_compile\",\"arguments\":{}}}");
        JsonObject compileResponse = JsonParser.parseString(reader.readLine()).getAsJsonObject();
        JsonObject compileResult = compileResponse.getAsJsonObject("result");
        String compileText = compileResult.getAsJsonArray("content").get(0)
                .getAsJsonObject().get("text").getAsString();
        Assert.assertFalse("Got: " + compileText, compileResult.get("isError").getAsBoolean());
        Assert.assertTrue("Got: " + compileText, compileText.contains("/e2e_repo/model/extra.pure"));

        // EOF shuts the server loop down
        writer.close();
        serverThread.join(30000);
        Assert.assertFalse("Server loop must exit on stdin EOF", serverThread.isAlive());
    }
}
```

Add `LegendPureMcpServerTest.class` and `McpEndToEndTest.class` to `McpTestSuite`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest="LegendPureMcpServerTest,McpEndToEndTest" -DfailIfNoTests=false`
Expected: COMPILATION ERROR (`LegendPureMcpServer` doesn't exist).

- [ ] **Step 3: Write the implementation**

`LegendPureMcpServer.java`:

```java
package org.finos.legend.pure.lsp.mcp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone MCP stdio server for a Pure workspace. Launched by an MCP client
 * (e.g. Claude Code) as:
 *   java -cp "legend-pure-mcp-server.jar:dependency/*" \
 *       org.finos.legend.pure.lsp.mcp.LegendPureMcpServer --workspace /path/to/project
 * stdout carries the MCP protocol; ALL logging goes to stderr.
 */
public class LegendPureMcpServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureMcpServer.class);

    public static void main(String[] args) throws IOException
    {
        PrintStream protocolOut = new PrintStream(
                new java.io.BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), true);
        PrintStream stderrOut = new PrintStream(
                new java.io.BufferedOutputStream(new FileOutputStream(FileDescriptor.err)), true);
        // Anything printing to System.out (Pure console defaults, stray libraries) must not
        // corrupt the protocol stream - same discipline as LegendPureLspServer.main.
        System.setOut(stderrOut);
        System.setErr(stderrOut);

        Path workspace;
        try
        {
            workspace = resolveWorkspace(args);
        }
        catch (IllegalArgumentException e)
        {
            stderrOut.println("Usage: LegendPureMcpServer [--workspace <dir>]");
            stderrOut.println(e.getMessage());
            System.exit(1);
            return;
        }

        serve(workspace, System.in, protocolOut);
    }

    static Path resolveWorkspace(String[] args)
    {
        Path workspace = Paths.get("");
        for (int i = 0; i < args.length; i++)
        {
            if ("--workspace".equals(args[i]))
            {
                if ((i + 1) >= args.length)
                {
                    throw new IllegalArgumentException("--workspace requires a directory argument");
                }
                workspace = Paths.get(args[i + 1]);
                i++;
            }
        }
        Path normalized = workspace.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized))
        {
            throw new IllegalArgumentException("Workspace is not a directory: " + normalized);
        }
        return normalized;
    }

    static void serve(Path workspace, InputStream in, OutputStream out) throws IOException
    {
        LOGGER.info("Starting legend-pure-mcp-server for workspace {}", workspace);

        LegendPureSession session = new LegendPureSession();
        RepositoryScanner scanner = new RepositoryScanner();
        UriMapper uriMapper = new UriMapper();
        WorkspaceSymbolProvider symbols = new WorkspaceSymbolProvider();
        WorkspaceSync sync = new WorkspaceSync(scanner);
        InitGate gate = new InitGate();

        Thread initThread = new Thread(() ->
        {
            try
            {
                scanner.scan(Collections.singletonList(workspace));
                session.initialize(scanner);
                uriMapper.setRepositoryScanner(scanner);
                uriMapper.setPureRuntime(session.getPureRuntime());
                symbols.buildIndex(session.getPureRuntime());
                sync.seed();
                gate.ready();
                LOGGER.info("Pure runtime ready; MCP tools are live");
            }
            catch (Throwable t)
            {
                LOGGER.error("Pure runtime initialization failed", t);
                gate.fail("Pure runtime initialization failed: " + t);
            }
        }, "pure-mcp-init");
        initThread.setDaemon(true);
        initThread.start();

        PureToolRegistry registry = PureTools.buildRegistry(session, scanner, sync, uriMapper, symbols, gate);
        new McpStdioServer(registry, in, out, serverVersion()).run();
    }

    private static String serverVersion()
    {
        String version = LegendPureMcpServer.class.getPackage().getImplementationVersion();
        return (version != null) ? version : "dev";
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `source /home/aziem/bin/jdk11.sh && mvn test -pl legend-pure-lsp/legend-pure-mcp-server -Dtest="LegendPureMcpServerTest,McpEndToEndTest" -DfailIfNoTests=false`
Expected: 5 tests PASS (E2E is slow — one runtime init).

- [ ] **Step 5: Run the whole module suite + checkstyle**

Run: `source /home/aziem/bin/jdk11.sh && mvn verify -pl legend-pure-lsp/legend-pure-mcp-server`
Expected: BUILD SUCCESS — `McpTestSuite` runs all test classes; checkstyle passes with zero warnings. Fix any checkstyle violations (headers, brace placement, tabs) before committing.

- [ ] **Step 6: Smoke-test the packaged jar manually**

```bash
source /home/aziem/bin/jdk11.sh
cd legend-pure-lsp/legend-pure-mcp-server
printf '%s\n%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"smoke","version":"0"}}}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  | java -cp "target/legend-pure-mcp-server-5.96.1-SNAPSHOT.jar:target/dependency/*" \
      org.finos.legend.pure.lsp.mcp.LegendPureMcpServer --workspace "$(pwd)" 2>/dev/null
cd ../..
```

Expected: exactly two JSON lines on stdout — the initialize result and a tools list with 7 tools. (Stderr is discarded; the workspace has no Pure repos, which is fine — no tool is called.)

- [ ] **Step 7: Commit**

```bash
git add legend-pure-lsp/legend-pure-mcp-server/src
git commit -m "Add LegendPureMcpServer main class with background init and E2E test"
```

---

### Task 8: Documentation

**Files:**
- Create: `docs/guides/mcp-server-guide.md`
- Modify: `docs/README.md` (add index entry)

**Interfaces:**
- Consumes: the shipped behavior of Tasks 1–7 (launch command, tool names/semantics).
- Produces: user-facing docs; no code.

- [ ] **Step 1: Write the guide**

Create `docs/guides/mcp-server-guide.md` covering (in this order, with real commands/JSON — check `docs/guides/` for the established doc style and match it):

1. **What it is** — MCP stdio server exposing compile/execute/navigation over a Pure workspace to AI coding agents; interpreted engine; disk is the source of truth.
2. **Build** — `mvn install -DskipTests -pl legend-pure-lsp/legend-pure-mcp-server -am` produces `legend-pure-lsp/legend-pure-mcp-server/target/legend-pure-mcp-server-<version>.jar` + `target/dependency/`.
3. **Configure in Claude Code** — `.mcp.json` at the Pure project root:

```json
{
  "mcpServers": {
    "legend-pure": {
      "command": "java",
      "args": [
        "-Xmx4g",
        "-cp",
        "/path/to/legend-pure/legend-pure-lsp/legend-pure-mcp-server/target/legend-pure-mcp-server-5.96.1-SNAPSHOT.jar:/path/to/legend-pure/legend-pure-lsp/legend-pure-mcp-server/target/dependency/*",
        "org.finos.legend.pure.lsp.mcp.LegendPureMcpServer",
        "--workspace",
        "."
      ]
    }
  }
}
```

4. **Workspace layout requirement** — repos are discovered via `<name>.definition.json` files under `src/main/resources/`, sources under `<resources>/<repoName>/**.pure` (same discovery as the LSP server).
5. **The tools** — one row per tool (name, arguments, what it returns), taken from the Task 5/6 descriptions.
6. **The agent loop** — edit `.pure` files with normal file tools → `pure_compile` → fix errors → write `function go():Any[*] { ... }` → `pure_execute`.
7. **Caveats** — first tool call blocks until runtime init completes (tens of seconds); one session per server process; debug tools and shared-IDE-session mode are not yet available.

- [ ] **Step 2: Add the index entry**

In `docs/README.md`, add under the guides section (match the file's existing list format):

```markdown
- [MCP server guide](guides/mcp-server-guide.md) — exposing Pure compile/execute/navigation to AI coding agents.
```

- [ ] **Step 3: Verify docs render and links resolve**

Run: `ls docs/guides/mcp-server-guide.md && grep -n "mcp-server-guide" docs/README.md`
Expected: both hits present.

- [ ] **Step 4: Commit**

```bash
git add docs/guides/mcp-server-guide.md docs/README.md
git commit -m "Add MCP server user guide and docs index entry"
```
