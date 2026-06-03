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

package org.finos.legend.pure.lsp.debug;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.LspLog;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LegendDebugSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendDebugSession.class);
    private static final String DEFAULT_FUNCTION = "go():Any[*]";
    private static final String DEBUG_STATEMENT = "meta::pure::ide::debug();";
    private static final String DEBUG_CONSOLE_PREFIX = "Entering debug mode.  Use terminal to introspect debug state.";
    private static final String RESUME_CONSOLE_TEXT = "Resuming from debug point...";

    private final UriMapper uriMapper;
    private final LegendDebugFunctionExecution functionExecution;
    private final CoreInstance function;
    private final Map<String, LineMap> lineMaps;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Object executionLock = new Object();
    private final Object pauseStateLock = new Object();

    private volatile boolean stopped;
    private volatile LegendDebugState visiblePausedState;
    private int outputOffset;

    private LegendDebugSession(UriMapper uriMapper, LegendDebugFunctionExecution functionExecution,
                               CoreInstance function, Map<String, LineMap> lineMaps)
    {
        this.uriMapper = uriMapper;
        this.functionExecution = functionExecution;
        this.function = function;
        this.lineMaps = lineMaps;

        Console console = this.functionExecution.getConsole();
        console.setPrintStream(new PrintStream(this.output, true));
        console.setConsole(true);
    }

    static LegendDebugSession create(LegendPureSession mainSession, RepositoryScanner repositoryScanner,
                                     UriMapper uriMapper, Map<String, String> openDocuments,
                                     String functionName, List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<String, String> sources = snapshotSources(mainSession, repositoryScanner, openDocuments);
        Map<String, List<Integer>> breakpointsBySource = groupBreakpointsBySource(uriMapper, sources, breakpoints);
        Map<String, LineMap> lineMaps = new TreeMap<>();

        PureRuntime debugRuntime = LegendPureSession.newDebugRuntime(
                repositoryScanner, mainSession.getClasspathRepositoryNames());
        for (Map.Entry<String, String> entry : sources.entrySet())
        {
            lineMaps.put(entry.getKey(), lineMapForSource(entry.getValue(), breakpointsBySource.get(entry.getKey())));
            overlaySource(debugRuntime, entry.getKey(), entry.getValue());
        }
        debugRuntime.compile();

        LegendDebugFunctionExecution debugExecution = new LegendDebugFunctionExecution(lineMaps.keySet());
        debugExecution.init(debugRuntime, new Message(""));

        CoreInstance function = findZeroArgumentFunction(debugRuntime, functionName);
        if (function == null)
        {
            throw new IllegalArgumentException("No zero-argument function found for " + normalizeFunctionName(functionName));
        }

        return new LegendDebugSession(uriMapper, debugExecution, function, lineMaps);
    }

    private static void overlaySource(PureRuntime runtime, String sourceId, String content)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null && sourceId != null && sourceId.startsWith("/"))
        {
            try
            {
                runtime.loadSourceIfLoadable(sourceId);
                source = runtime.getSourceById(sourceId);
            }
            catch (Exception ignored)
            {
            }
        }

        if (source == null)
        {
            runtime.createInMemorySource(sourceId, content);
        }
        else if (!source.isImmutable())
        {
            runtime.modify(sourceId, content);
        }
    }

    LegendDebug.Response start()
    {
        return runUntilPauseOrCompletion(RunMode.CONTINUE, false);
    }

    LegendDebug.Response continueExecution()
    {
        return runUntilPauseOrCompletion(RunMode.CONTINUE, true);
    }

    LegendDebug.Response stepIn()
    {
        return runUntilPauseOrCompletion(RunMode.STEP_IN, true);
    }

    LegendDebug.Response stepOver()
    {
        return runUntilPauseOrCompletion(RunMode.STEP_OVER, true);
    }

    LegendDebug.Response stop()
    {
        this.stopped = true;
        clearVisiblePausedState();
        this.functionExecution.abortDebug();
        this.functionExecution.getConsole().setConsole(false);
        return LegendDebug.Response.completed(readNewUserOutput());
    }

    LegendDebug.EvaluateResult evaluate(String expression)
    {
        synchronized (this.pauseStateLock)
        {
            LegendDebugState state = this.visiblePausedState;
            if (state == null)
            {
                return LegendDebug.EvaluateResult.error("Debug execution is not paused");
            }
            try
            {
                return state.evaluate(expression == null ? "" : expression);
            }
            catch (Exception e)
            {
                LOGGER.debug("Debug evaluate failed", e);
                return LegendDebug.EvaluateResult.error(message(e));
            }
        }
    }

    List<LegendDebug.Variable> variables()
    {
        return variables(1);
    }

    List<LegendDebug.Variable> variables(int variablesReference)
    {
        synchronized (this.pauseStateLock)
        {
            LegendDebugState state = this.visiblePausedState;
            if (state == null)
            {
                return Collections.emptyList();
            }

            return state.variables(variablesReference);
        }
    }

    boolean isPaused()
    {
        return getVisiblePausedState() != null;
    }

    String debugSourceContent(String sourceId)
    {
        Source source = this.functionExecution.getPureRuntime().getSourceById(sourceId);
        return source == null ? null : source.getContent();
    }

    private LegendDebugState getVisiblePausedState()
    {
        synchronized (this.pauseStateLock)
        {
            return this.visiblePausedState;
        }
    }

    private void setVisiblePausedState(LegendDebugState state)
    {
        synchronized (this.pauseStateLock)
        {
            this.visiblePausedState = state;
        }
    }

    private void clearVisiblePausedState()
    {
        synchronized (this.pauseStateLock)
        {
            this.visiblePausedState = null;
        }
    }

    private LegendDebug.Response runUntilPauseOrCompletion(RunMode mode, boolean requirePaused)
    {
        synchronized (this.executionLock)
        {
            if (requirePaused && getVisiblePausedState() == null)
            {
                return LegendDebug.Response.error("Debug execution is not paused");
            }

            PauseLocation startLocation = currentPauseLocation();
            clearVisiblePausedState();
            StringBuilder visibleOutput = new StringBuilder();
            while (true)
            {
                if (this.stopped)
                {
                    clearVisiblePausedState();
                    visibleOutput.append(readNewUserOutput());
                    return LegendDebug.Response.completed(visibleOutput.toString());
                }

                try
                {
                    this.functionExecution.startDebug(this.function, FastList.newList());
                }
                catch (Exception e)
                {
                    this.functionExecution.getConsole().setConsole(false);
                    clearVisiblePausedState();
                    visibleOutput.append(readNewUserOutput());
                    if (this.stopped)
                    {
                        return LegendDebug.Response.completed(visibleOutput.toString());
                    }
                    LOGGER.debug("Debug execution failed", e);
                    return LegendDebug.Response.error(message(e));
                }

                visibleOutput.append(readNewUserOutput());
                LegendDebugState state = this.functionExecution.getDebugState();
                if (state == null)
                {
                    this.functionExecution.getConsole().setConsole(false);
                    clearVisiblePausedState();
                    return LegendDebug.Response.completed(visibleOutput.toString());
                }

                PauseLocation pauseLocation = currentPauseLocation(state);
                PauseDecision decision = pauseDecision(mode, startLocation, pauseLocation);
                if (decision.pause)
                {
                    setVisiblePausedState(state);
                    return LegendDebug.Response.paused(
                            visibleOutput.toString(),
                            stackFrames(state, pauseLocation),
                            decision.reason);
                }
            }
        }
    }

    private PauseDecision pauseDecision(RunMode mode, PauseLocation startLocation, PauseLocation pauseLocation)
    {
        if (pauseLocation == null)
        {
            return PauseDecision.resume();
        }
        if (pauseLocation.userBreakpoint)
        {
            return PauseDecision.pause("breakpoint");
        }
        if (pauseLocation.explicitDebug)
        {
            return PauseDecision.pause("pause");
        }
        switch (mode)
        {
            case STEP_IN:
                return sameLocation(startLocation, pauseLocation) ? PauseDecision.resume() : PauseDecision.pause("step");
            case STEP_OVER:
                return isStepOverTarget(startLocation, pauseLocation) ? PauseDecision.pause("step") : PauseDecision.resume();
            case CONTINUE:
            default:
                return PauseDecision.resume();
        }
    }

    private boolean isStepOverTarget(PauseLocation startLocation, PauseLocation pauseLocation)
    {
        if (startLocation == null || sameLocation(startLocation, pauseLocation))
        {
            return false;
        }
        if (pauseLocation.stackDepth > startLocation.stackDepth)
        {
            return false;
        }
        if (!Objects.equals(startLocation.sourceId, pauseLocation.sourceId))
        {
            return false;
        }
        if (startLocation.functionRange != null)
        {
            return startLocation.functionRange.contains(pauseLocation.originalLine)
                    && pauseLocation.originalLine > startLocation.originalLine;
        }
        return pauseLocation.originalLine > startLocation.originalLine;
    }

    private PauseLocation currentPauseLocation()
    {
        LegendDebugState state = this.functionExecution.getDebugState();
        return state == null ? null : currentPauseLocation(state);
    }

    private PauseLocation currentPauseLocation(LegendDebugState state)
    {
        SourceInformation sourceInformation = state.getCurrentSourceInformation();
        if (sourceInformation == null)
        {
            return null;
        }

        String sourceId = sourceInformation.getSourceId();
        LineMap lineMap = this.lineMaps.getOrDefault(sourceId, LineMap.identity());
        int debugLine = positiveOrDefault(sourceInformation.getLine(), sourceInformation.getStartLine(), 1);
        int originalLine = lineMap.toOriginalLineOneBased(debugLine);
        return new PauseLocation(
                sourceId,
                originalLine,
                lineMap.isUserBreakpoint(originalLine),
                lineMap.isExplicitDebug(originalLine),
                lineMap.functionRange(originalLine),
                state.getStackDepth());
    }

    private List<LegendDebug.StackFrame> stackFrames(LegendDebugState state, PauseLocation pauseLocation)
    {
        SourceInformation sourceInformation = state.getCurrentSourceInformation();
        if (sourceInformation == null || pauseLocation == null)
        {
            return Collections.emptyList();
        }

        String sourceId = sourceInformation.getSourceId();
        LineMap lineMap = this.lineMaps.getOrDefault(sourceId, LineMap.identity());
        int debugLine = positiveOrDefault(sourceInformation.getLine(), sourceInformation.getStartLine(), 1);
        int debugColumn = positiveOrDefault(sourceInformation.getColumn(), sourceInformation.getStartColumn(), 1);
        String uri = sourceId == null ? null : this.uriMapper.toUri(sourceId);

        return Collections.singletonList(new LegendDebug.StackFrame(
                1,
                state.getCurrentFrameName(),
                uri,
                pauseLocation.originalLine,
                lineMap.toOriginalColumnOneBased(debugLine, debugColumn)));
    }

    private String readNewUserOutput()
    {
        String value = new String(this.output.toByteArray(), StandardCharsets.UTF_8);
        if (this.outputOffset > value.length())
        {
            this.outputOffset = 0;
        }
        String delta = value.substring(this.outputOffset);
        this.outputOffset = value.length();
        return stripDebugConsoleText(delta);
    }

    private static String stripDebugConsoleText(String text)
    {
        String withoutResumeText = text.replace(RESUME_CONSOLE_TEXT, "");
        int debugSummaryStart = withoutResumeText.lastIndexOf(DEBUG_CONSOLE_PREFIX);
        return debugSummaryStart < 0 ? withoutResumeText : withoutResumeText.substring(0, debugSummaryStart);
    }

    private static int positiveOrDefault(int first, int second, int fallback)
    {
        if (first > 0)
        {
            return first;
        }
        return second > 0 ? second : fallback;
    }

    private static Map<String, String> snapshotSources(LegendPureSession mainSession, RepositoryScanner repositoryScanner,
                                                       Map<String, String> openDocuments)
    {
        Map<String, String> sources = new TreeMap<>();
        Set<String> workspaceRepositoryNames = repositoryScanner == null
                ? Collections.emptySet()
                : repositoryScanner.getWorkspaceRepoNames();
        Set<String> runtimeDependencyRepositoryNames = mainSession.getClasspathRepositoryNames();
        synchronized (mainSession)
        {
            for (Source source : mainSession.getPureRuntime().getSourceRegistry().getSources())
            {
                if (!isDebuggableSource(source, workspaceRepositoryNames, runtimeDependencyRepositoryNames))
                {
                    continue;
                }
                sources.put(source.getId(), source.getContent());
            }
        }
        if (openDocuments != null)
        {
            for (Map.Entry<String, String> entry : openDocuments.entrySet())
            {
                if (isDebuggableSourceId(entry.getKey(), workspaceRepositoryNames, runtimeDependencyRepositoryNames))
                {
                    sources.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return sources;
    }

    private static boolean isDebuggableSource(Source source, Set<String> workspaceRepositoryNames,
                                              Set<String> runtimeDependencyRepositoryNames)
    {
        if (source == null || source.isImmutable() || source.getContent() == null)
        {
            return false;
        }

        return source.isInMemory()
                || isDebuggableSourceId(source.getId(), workspaceRepositoryNames, runtimeDependencyRepositoryNames);
    }

    private static boolean isDebuggableSourceId(String sourceId, Set<String> workspaceRepositoryNames,
                                                Set<String> runtimeDependencyRepositoryNames)
    {
        if (sourceId == null)
        {
            return false;
        }
        if (!sourceId.startsWith("/"))
        {
            return true;
        }
        String repositoryName = repositoryName(sourceId);
        return !runtimeDependencyRepositoryNames.contains(repositoryName)
                && workspaceRepositoryNames.contains(repositoryName);
    }

    private static String repositoryName(String sourceId)
    {
        int nextSlash = sourceId.indexOf('/', 1);
        return nextSlash < 0 ? sourceId.substring(1) : sourceId.substring(1, nextSlash);
    }

    private static Map<String, List<Integer>> groupBreakpointsBySource(UriMapper uriMapper, Map<String, String> sources,
                                                                        List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<String, List<Integer>> grouped = new TreeMap<>();
        if (breakpoints == null)
        {
            return grouped;
        }

        for (LegendDebug.Breakpoint breakpoint : breakpoints)
        {
            if (breakpoint == null || breakpoint.getUri() == null || !isFilePureUri(breakpoint.getUri()))
            {
                continue;
            }

            String sourceId = uriMapper.toSourceId(breakpoint.getUri());
            if (!sources.containsKey(sourceId))
            {
                String alternate = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
                if (sources.containsKey(alternate))
                {
                    sourceId = alternate;
                }
                else
                {
                    LspLog.debug("Skipping breakpoint for unknown source: " + breakpoint.getUri());
                    continue;
                }
            }

            grouped.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(breakpoint.getLine());
        }
        return grouped;
    }

    private static boolean isFilePureUri(String uri)
    {
        try
        {
            URI parsed = URI.create(uri);
            if (!"file".equals(parsed.getScheme()))
            {
                return false;
            }
            Path path = Paths.get(parsed);
            return path.getFileName() != null && path.getFileName().toString().endsWith(".pure");
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static LineMap lineMapForSource(String content, List<Integer> breakpointLines)
    {
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = content.split(Pattern.quote(newline), -1);
        LineMap lineMap = new LineMap(functionRanges(lines));
        lineMap.addUserBreakpoints(validatedBreakpointLines(lines, breakpointLines));

        for (int originalLine = 0; originalLine < lines.length; originalLine++)
        {
            if (isExplicitDebugStatement(lines[originalLine]))
            {
                lineMap.addExplicitDebug(originalLine + 1);
            }
        }
        return lineMap;
    }

    private static Set<Integer> validatedBreakpointLines(String[] lines, List<Integer> breakpointLines)
    {
        Set<Integer> targets = new TreeSet<>();
        if (breakpointLines == null)
        {
            return targets;
        }
        for (Integer line : breakpointLines)
        {
            if (line != null && line >= 0 && line < lines.length && isBreakableFunctionLine(lines, line))
            {
                targets.add(line + 1);
            }
        }
        return targets;
    }

    private static List<FunctionRange> functionRanges(String[] lines)
    {
        List<FunctionRange> ranges = new ArrayList<>();
        boolean pendingFunction = false;
        boolean inFunction = false;
        int bodyStartLine = -1;
        int depth = 0;

        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++)
        {
            String code = stripLineComment(lines[lineNumber]);
            String trimmed = code.trim();
            if (!inFunction && trimmed.startsWith("function "))
            {
                pendingFunction = true;
            }

            for (int i = 0; i < code.length(); i++)
            {
                char c = code.charAt(i);
                if (c == '{')
                {
                    if (pendingFunction && !inFunction)
                    {
                        inFunction = true;
                        pendingFunction = false;
                        bodyStartLine = lineNumber + 1;
                        depth = 1;
                    }
                    else if (inFunction)
                    {
                        depth++;
                    }
                }
                else if (c == '}' && inFunction)
                {
                    depth--;
                    if (depth == 0)
                    {
                        ranges.add(new FunctionRange(bodyStartLine, lineNumber + 1));
                        inFunction = false;
                        bodyStartLine = -1;
                    }
                }
            }
        }
        return ranges;
    }

    private static boolean isBreakableFunctionLine(String[] lines, int targetLine)
    {
        if (!isStatementLine(lines[targetLine]))
        {
            return false;
        }

        for (FunctionRange range : functionRanges(lines))
        {
            if (range.contains(targetLine + 1) && targetLine + 1 > range.startLine && targetLine + 1 < range.endLine)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isStatementLine(String line)
    {
        String trimmed = line.trim();
        return !trimmed.isEmpty()
                && !trimmed.startsWith("//")
                && !trimmed.startsWith("/*")
                && !trimmed.startsWith("*")
                && !trimmed.startsWith("}")
                && !trimmed.startsWith("{")
                && !trimmed.startsWith("function ");
    }

    private static boolean isExplicitDebugStatement(String line)
    {
        return stripLineComment(line).contains(DEBUG_STATEMENT);
    }

    private static String stripLineComment(String line)
    {
        int idx = line.indexOf("//");
        return idx < 0 ? line : line.substring(0, idx);
    }

    private static CoreInstance findZeroArgumentFunction(PureRuntime runtime, String functionName)
    {
        String normalized = normalizeFunctionName(functionName);
        CoreInstance function = tryGetFunction(runtime, normalized);
        if (function != null)
        {
            return function;
        }

        String base = normalized;
        if (!base.contains("("))
        {
            base = base + "()";
        }
        if (!base.contains("):"))
        {
            for (String returnType : new String[] {"Any[*]", "String[*]", "String[1]", "Boolean[1]", "Integer[1]", "Nil[0]"})
            {
                function = tryGetFunction(runtime, base + ":" + returnType);
                if (function != null)
                {
                    return function;
                }
            }
        }
        return null;
    }

    private static CoreInstance tryGetFunction(PureRuntime runtime, String functionDescriptor)
    {
        try
        {
            return runtime.getFunction(functionDescriptor);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static String normalizeFunctionName(String functionName)
    {
        return (functionName == null || functionName.trim().isEmpty()) ? DEFAULT_FUNCTION : functionName.trim();
    }

    private static boolean sameLocation(PauseLocation first, PauseLocation second)
    {
        return first != null
                && second != null
                && first.originalLine == second.originalLine
                && Objects.equals(first.sourceId, second.sourceId);
    }

    private static String message(Exception e)
    {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private enum RunMode
    {
        CONTINUE,
        STEP_IN,
        STEP_OVER
    }

    private static class PauseDecision
    {
        private final boolean pause;
        private final String reason;

        private PauseDecision(boolean pause, String reason)
        {
            this.pause = pause;
            this.reason = reason;
        }

        private static PauseDecision pause(String reason)
        {
            return new PauseDecision(true, reason);
        }

        private static PauseDecision resume()
        {
            return new PauseDecision(false, null);
        }
    }

    private static class PauseLocation
    {
        private final String sourceId;
        private final int originalLine;
        private final boolean userBreakpoint;
        private final boolean explicitDebug;
        private final FunctionRange functionRange;
        private final int stackDepth;

        private PauseLocation(String sourceId, int originalLine, boolean userBreakpoint,
                              boolean explicitDebug, FunctionRange functionRange, int stackDepth)
        {
            this.sourceId = sourceId;
            this.originalLine = originalLine;
            this.userBreakpoint = userBreakpoint;
            this.explicitDebug = explicitDebug;
            this.functionRange = functionRange;
            this.stackDepth = stackDepth;
        }
    }

    private static class FunctionRange
    {
        private final int startLine;
        private final int endLine;

        private FunctionRange(int startLine, int endLine)
        {
            this.startLine = startLine;
            this.endLine = endLine;
        }

        private boolean contains(int line)
        {
            return line >= this.startLine && line <= this.endLine;
        }
    }

    private static class LineMap
    {
        private final Set<Integer> userBreakpointOriginalLines = new TreeSet<>();
        private final Set<Integer> explicitDebugOriginalLines = new TreeSet<>();
        private final List<FunctionRange> functionRanges;

        private LineMap(Collection<FunctionRange> functionRanges)
        {
            this.functionRanges = new ArrayList<>(functionRanges);
        }

        private static LineMap identity()
        {
            return new LineMap(Collections.emptyList());
        }

        private void addUserBreakpoints(Collection<Integer> originalLines)
        {
            this.userBreakpointOriginalLines.addAll(originalLines);
        }

        private void addExplicitDebug(int originalLineOneBased)
        {
            this.explicitDebugOriginalLines.add(originalLineOneBased);
        }

        private boolean isUserBreakpoint(int originalLineOneBased)
        {
            return this.userBreakpointOriginalLines.contains(originalLineOneBased);
        }

        private boolean isExplicitDebug(int originalLineOneBased)
        {
            return this.explicitDebugOriginalLines.contains(originalLineOneBased);
        }

        private FunctionRange functionRange(int originalLineOneBased)
        {
            for (FunctionRange range : this.functionRanges)
            {
                if (range.contains(originalLineOneBased))
                {
                    return range;
                }
            }
            return null;
        }

        private int toOriginalLineOneBased(int debugLineOneBased)
        {
            return Math.max(1, debugLineOneBased);
        }

        private int toOriginalColumnOneBased(int debugLineOneBased, int debugColumnOneBased)
        {
            return Math.max(1, debugColumnOneBased);
        }
    }
}
