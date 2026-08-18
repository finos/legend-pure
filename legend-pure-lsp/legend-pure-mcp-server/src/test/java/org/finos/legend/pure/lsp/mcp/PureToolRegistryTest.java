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
