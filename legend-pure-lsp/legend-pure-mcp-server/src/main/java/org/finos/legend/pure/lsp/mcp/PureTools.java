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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.google.gson.JsonObject;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.PackageChildInfo;
import org.finos.legend.pure.lsp.PackageTreeProvider;
import org.finos.legend.pure.lsp.ReferencesProvider;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
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
                        String output = result.getOutput();
                        // On execution failure, LegendPureSession.executeFunctionByCandidates returns the
                        // SAME string for both getError() and getOutput() - only append it once.
                        if (output != null && !output.isEmpty() && !output.equals(result.getError()))
                        {
                            error.append('\n').append(output);
                        }
                        return ToolResult.error(error.toString());
                    }
                    String output = result.getOutput();
                    return ToolResult.ok((output == null || output.isEmpty()) ? "(no output)" : output);
                }));

        JsonObject pathProps = new JsonObject();
        pathProps.add("path", stringProp("Full Pure path of the element, e.g. 'my::pkg::MyClass'."));
        registry.register(new McpTool(
                "pure_find_element",
                "Resolve a Pure element by its full path and return its kind, source location, "
                        + "and definition text. If the path is overloaded (multiple functions "
                        + "sharing the same name in a package), every overload is returned.",
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
                        List<CoreInstance> elements = findElements(session.getPureRuntime(), path);
                        if (elements.isEmpty())
                        {
                            return ToolResult.error(unknownElementMessage(path, symbols, uriMapper));
                        }
                        if (elements.size() == 1)
                        {
                            return ToolResult.ok(describeElement(elements.get(0), path, session, scanner, true));
                        }
                        StringBuilder text = new StringBuilder()
                                .append(elements.size()).append(" elements match '").append(path).append("':");
                        for (CoreInstance element : elements)
                        {
                            text.append("\n\n").append(describeElement(element, path, session, scanner, false));
                        }
                        return ToolResult.ok(text.toString());
                    });
                }));

        registry.register(new McpTool(
                "pure_find_usages",
                "Find all usages of a Pure element (given by full path) across the compiled "
                        + "workspace. If the path is overloaded (multiple functions sharing the "
                        + "same name in a package), usages are aggregated across every overload, "
                        + "each listed under its own 'Overload' header so none are silently omitted.",
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
                        List<CoreInstance> elements = findElements(runtime, path);
                        if (elements.isEmpty())
                        {
                            return ToolResult.error(unknownElementMessage(path, symbols, uriMapper));
                        }
                        if (elements.size() == 1 && elements.get(0).getSourceInformation() == null)
                        {
                            return ToolResult.error("Element '" + path + "' has no source information.");
                        }
                        boolean multipleOverloads = elements.size() > 1;
                        StringBuilder text = new StringBuilder("Usages of ").append(path).append(':');
                        boolean anyUsages = false;
                        for (CoreInstance element : elements)
                        {
                            SourceInformation si = element.getSourceInformation();
                            if (si == null)
                            {
                                continue;
                            }
                            if (multipleOverloads)
                            {
                                text.append("\n\nOverload ").append(element.getName()).append("  ")
                                        .append(describeLocation(si, scanner)).append(':');
                            }
                            List<Location> locations = ReferencesProvider.references(runtime, uriMapper,
                                    si.getSourceId(), si.getStartLine(), si.getStartColumn(), false);
                            if (locations.isEmpty())
                            {
                                // The declaration's start position may point at a keyword rather than
                                // the element's name token; retry at the name's column on the same line.
                                int nameColumn = findNameColumn(runtime, si, path);
                                if (nameColumn > 0)
                                {
                                    locations = ReferencesProvider.references(runtime, uriMapper,
                                            si.getSourceId(), si.getStartLine(), nameColumn, false);
                                }
                            }
                            if (locations.isEmpty())
                            {
                                if (multipleOverloads)
                                {
                                    text.append("\n (no usages found for this overload)");
                                }
                                continue;
                            }
                            anyUsages = true;
                            for (Location location : locations)
                            {
                                text.append("\n - ").append(formatUsageLocation(location, runtime, uriMapper));
                            }
                        }
                        if (!anyUsages)
                        {
                            return ToolResult.ok("No usages found for '" + path + "'.");
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
                    int maxResults = 50;
                    if (arguments.has("maxResults") && arguments.get("maxResults").isJsonPrimitive()
                            && arguments.get("maxResults").getAsJsonPrimitive().isNumber())
                    {
                        maxResults = Math.max(1, arguments.get("maxResults").getAsInt());
                    }
                    // No graph read lock needed here: WorkspaceSymbolProvider.search reads only its
                    // own CopyOnWriteArrayList index of primitive fields, never a CoreInstance.
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

    /**
     * Resolve a Pure path to every element it names. Almost always a single element, but Pure
     * allows function overloading - multiple functions sharing one name in the same package,
     * disambiguated only by parameter types (e.g. the standard library's
     * meta::pure::functions::math::plus has 4 overloads) - so a bare functional path can be
     * genuinely ambiguous. Callers must handle a multi-element result rather than assuming the
     * first match is "the" answer, or usages/definitions for the other overloads go missing
     * silently.
     */
    private static List<CoreInstance> findElements(PureRuntime runtime, String path)
    {
        try
        {
            CoreInstance direct = runtime.getCoreInstance(path);
            if (direct != null)
            {
                return Collections.singletonList(direct);
            }
        }
        catch (Exception ignored)
        {
            // Fall through to the qualified-path walk below.
        }
        // PureRuntime.getCoreInstance() resolves by matching each path segment against the
        // raw CoreInstance name of a package's children. That works for classes/associations/
        // enums (whose name IS their simple name) but not for functions, whose child name is
        // the classifier-mangled id (e.g. "myFunc_String_1_"), not the bare simple name used in
        // a Pure path - and it cannot return more than one match anyway. Walk the package tree
        // ourselves, matching each segment against the *display* name (the function name for
        // functions, the raw name otherwise) so that 'my::pkg::myFunc' also resolves, and so
        // that every overload sharing that display name in the final package is returned.
        return findByQualifiedPathWalk(runtime, path);
    }

    private static List<CoreInstance> findByQualifiedPathWalk(PureRuntime runtime, String path)
    {
        CoreInstance current = runtime.getCoreInstance("::");
        if (!(current instanceof Package))
        {
            return Collections.emptyList();
        }
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("::"))
        {
            if (!segment.isEmpty())
            {
                segments.add(segment);
            }
        }
        if (segments.isEmpty())
        {
            return Collections.emptyList();
        }
        // All but the last segment are packages: names are unique there, so take the sole/first
        // match and keep walking down. Only the final segment can legitimately fan out to
        // multiple matches (function overloads), so that is the one we return in full.
        for (int i = 0; i < segments.size() - 1; i++)
        {
            List<CoreInstance> matches = findChildrenByDisplayName(current, segments.get(i));
            if (matches.isEmpty())
            {
                return Collections.emptyList();
            }
            current = matches.get(0);
        }
        return findChildrenByDisplayName(current, segments.get(segments.size() - 1));
    }

    private static List<CoreInstance> findChildrenByDisplayName(CoreInstance parent, String name)
    {
        ListIterable<? extends CoreInstance> children = parent.getValueForMetaPropertyToMany(M3Properties.children);
        if (children == null)
        {
            return Collections.emptyList();
        }
        List<CoreInstance> matches = new ArrayList<>();
        for (CoreInstance child : children)
        {
            if (name.equals(elementDisplayName(child)))
            {
                matches.add(child);
            }
        }
        return matches;
    }

    private static String elementDisplayName(CoreInstance element)
    {
        String classifierName = (element.getClassifier() != null) ? element.getClassifier().getName() : null;
        if ("ConcreteFunctionDefinition".equals(classifierName) || "NativeFunction".equals(classifierName))
        {
            CoreInstance functionName = element.getValueForMetaPropertyToOne(M3Properties.functionName);
            if (functionName != null)
            {
                return functionName.getName();
            }
        }
        return element.getName();
    }

    /**
     * Kind + source location + definition text for one element. {@code fullBody} extracts the
     * whole declaration (used when the path resolved to a single, unambiguous element); when
     * false, only the declaration's first source line is extracted - enough to distinguish
     * overload signatures from each other without dumping every overload's full body into one
     * response.
     */
    private static String describeElement(CoreInstance element, String path, LegendPureSession session,
                                          RepositoryScanner scanner, boolean fullBody)
    {
        String kind = (element.getClassifier() != null) ? element.getClassifier().getName() : "Unknown";
        StringBuilder text = new StringBuilder(kind).append(' ').append(path);
        SourceInformation si = element.getSourceInformation();
        if (si == null)
        {
            text.append("\n(no source information available)");
            return text.toString();
        }
        text.append('\n').append(describeLocation(si, scanner))
                .append("  (sourceId: ").append(si.getSourceId()).append(')');
        Source source = session.getPureRuntime().getSourceById(si.getSourceId());
        if (source != null)
        {
            int endLine = fullBody ? si.getEndLine() : si.getStartLine();
            text.append("\n\n").append(extractLines(source.getContent(), si.getStartLine(), endLine));
        }
        return text.toString();
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

    /**
     * Spec: pure_find_usages reports "one location per line with a source-line excerpt", locations
     * carrying the sourceId. Appends the sourceId and, when the source is available, the trimmed
     * text of the usage's line so an agent can see the call site without a follow-up
     * pure_get_source round trip.
     */
    private static String formatUsageLocation(Location location, PureRuntime runtime, UriMapper uriMapper)
    {
        StringBuilder text = new StringBuilder(formatLspLocation(location));
        String sourceId = uriMapper.toSourceId(location.getUri());
        if (sourceId == null)
        {
            return text.toString();
        }
        text.append("  (sourceId: ").append(sourceId).append(')');
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return text.toString();
        }
        int lineNumber = location.getRange().getStart().getLine();
        String[] lines = source.getContent().split("\n", -1);
        if (lineNumber >= 0 && lineNumber < lines.length)
        {
            String lineText = lines[lineNumber].trim();
            if (!lineText.isEmpty())
            {
                text.append(" - ").append(lineText);
            }
        }
        return text.toString();
    }
}
