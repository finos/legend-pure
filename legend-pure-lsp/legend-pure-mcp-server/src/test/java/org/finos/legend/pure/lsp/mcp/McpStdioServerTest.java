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

    @Test
    public void nonStringMethodDoesNotKillLoop() throws IOException
    {
        List<JsonObject> responses = serve(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":null}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":{\"x\":1}}",
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\"}");

        Assert.assertEquals(-32601, responses.get(0).getAsJsonObject("error").get("code").getAsInt());
        Assert.assertEquals(-32601, responses.get(1).getAsJsonObject("error").get("code").getAsInt());
        JsonObject last = responses.get(responses.size() - 1).getAsJsonObject();
        Assert.assertTrue("Loop must survive and answer ping", last.has("result"));
        Assert.assertEquals(3, last.get("id").getAsInt());
    }
}
