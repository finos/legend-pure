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
        Path repoDir = resourcesDir.resolve("test_e2e");
        Files.createDirectories(repoDir.resolve("model"));
        Files.write(resourcesDir.resolve("test_e2e.definition.json"),
                ("{\"name\":\"test_e2e\","
                        + "\"pattern\":\"(Root|test::e2e)(::.*)?\","
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
        Assert.assertTrue("Got: " + compileText, compileText.contains("/test_e2e/model/extra.pure"));

        // EOF shuts the server loop down
        writer.close();
        serverThread.join(30000);
        Assert.assertFalse("Server loop must exit on stdin EOF", serverThread.isAlive());
    }
}
