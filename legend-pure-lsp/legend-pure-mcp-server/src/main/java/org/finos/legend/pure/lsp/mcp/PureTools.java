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
