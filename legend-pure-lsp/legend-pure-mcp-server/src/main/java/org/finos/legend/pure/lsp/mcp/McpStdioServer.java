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
