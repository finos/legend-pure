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
                        + "\"pattern\":\"(Root|test::mcp)(::.*)?\","
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
                "function test::mcp::mcpNamedEntry():String[1]\n{\n  print('named-entry-ran', 1);\n  "
                        + "'named-entry-ran';\n}\n");
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
        Assert.assertEquals("Failure text must not be duplicated", executed.getText().indexOf("mcp-boom"),
                executed.getText().lastIndexOf("mcp-boom"));
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
        Assert.assertTrue("Usage location should carry the sourceId, got: " + result.getText(),
                result.getText().contains("(sourceId: /mcp_test_repo/model/usages.pure)"));
        Assert.assertTrue("Usage location should include the calling line's text, got: " + result.getText(),
                result.getText().contains("test::mcp::mcpUsedFunction()"));
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

    @Test
    public void findElementAndUsagesHandleOverloadedFunctions() throws IOException
    {
        writePure("model/overloads.pure",
                "function test::mcp::mcpOverloaded(s:String[1]):String[1]\n{\n  'string:' + $s\n}\n"
                        + "\n"
                        + "function test::mcp::mcpOverloaded(i:Integer[1]):String[1]\n{\n  'int:' + $i->toString()\n}\n"
                        + "\n"
                        + "function test::mcp::mcpOverloadCaller():String[1]\n{\n"
                        + "  let a = test::mcp::mcpOverloaded('x');\n"
                        + "  let b = test::mcp::mcpOverloaded(1);\n"
                        + "  $a + $b;\n}\n");
        ToolResult compiled = call("pure_compile");
        Assert.assertFalse("Got: " + compiled.getText(), compiled.isError());

        // pure_find_element must list both overloads, not silently pick one.
        ToolResult found = call("pure_find_element", "path", "test::mcp::mcpOverloaded");
        Assert.assertFalse("Got: " + found.getText(), found.isError());
        Assert.assertTrue("Should report both overloads, got: " + found.getText(),
                found.getText().contains("2 elements match"));
        Assert.assertTrue("Should show the String overload's parameter, got: " + found.getText(),
                found.getText().contains("s:String[1]"));
        Assert.assertTrue("Should show the Integer overload's parameter, got: " + found.getText(),
                found.getText().contains("i:Integer[1]"));

        // pure_find_usages must aggregate usages across every overload, not just one.
        ToolResult usages = call("pure_find_usages", "path", "test::mcp::mcpOverloaded");
        Assert.assertFalse("Got: " + usages.getText(), usages.isError());
        int firstHeader = usages.getText().indexOf("Overload ");
        int secondHeader = usages.getText().indexOf("Overload ", firstHeader + 1);
        Assert.assertTrue("Should have a header for each overload, got: " + usages.getText(),
                firstHeader >= 0 && secondHeader > firstHeader);
        Assert.assertTrue("Both call sites should be reported in overloads.pure, got: " + usages.getText(),
                usages.getText().contains("overloads.pure"));
    }
}
