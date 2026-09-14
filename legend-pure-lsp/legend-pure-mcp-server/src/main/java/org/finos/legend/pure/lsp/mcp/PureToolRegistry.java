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
