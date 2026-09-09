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
