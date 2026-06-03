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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LegendDebugSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendDebugSession.class);
    private static final String DEFAULT_FUNCTION = "go():Any[*]";
    private static final String DEBUG_CONSOLE_PREFIX = "Entering debug mode.  Use terminal to introspect debug state.";
    private static final String RESUME_CONSOLE_TEXT = "Resuming from debug point...";

    private final LegendDebugFunctionExecution functionExecution;
    private final CoreInstance function;
    private final Map<String, LineMap> lineMaps;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Object executionLock = new Object();
    private final Object pauseStateLock = new Object();

    private volatile boolean stopped;
    private volatile LegendDebugState visiblePausedState;
    private int outputOffset;

    private LegendDebugSession(LegendDebugFunctionExecution functionExecution, CoreInstance function, Map<String, LineMap> lineMaps)
    {
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
        LspLog.info("Debug snapshot contains " + sources.size()
                + " source(s); requested " + breakpointCount(breakpoints)
                + " breakpoint(s); mapped " + groupedBreakpointCount(breakpointsBySource)
                + " breakpoint(s) across " + breakpointsBySource.size() + " source(s)");

        PureRuntime debugRuntime = LegendPureSession.newDebugRuntime(
                repositoryScanner, mainSession.getClasspathRepositoryNames());
        for (Map.Entry<String, String> entry : sources.entrySet())
        {
            lineMaps.put(entry.getKey(), lineMapForSource(entry.getValue(), breakpointsBySource.get(entry.getKey())));
            overlaySource(debugRuntime, entry.getKey(), entry.getValue());
        }
        debugRuntime.compile();

        LegendDebugFunctionExecution debugExecution = new LegendDebugFunctionExecution(lineMaps.keySet(), uriMapper);
        debugExecution.init(debugRuntime, new Message(""));

        CoreInstance function = findZeroArgumentFunction(debugRuntime, functionName);
        if (function == null)
        {
            throw new IllegalArgumentException("No zero-argument function found for " + normalizeFunctionName(functionName));
        }

        return new LegendDebugSession(debugExecution, function, lineMaps);
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

    LegendDebug.Response stepOut()
    {
        return runUntilPauseOrCompletion(RunMode.STEP_OUT, true);
    }

    LegendDebug.Response stop()
    {
        this.stopped = true;
        clearVisiblePausedState();
        this.functionExecution.abortDebug();
        this.functionExecution.getConsole().setConsole(false);
        return LegendDebug.Response.completed(readNewUserOutput());
    }

    LegendDebug.EvaluateResult evaluate(String expression, int frameId)
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
                return state.evaluate(expression == null ? "" : expression, frameId);
            }
            catch (Exception e)
            {
                LOGGER.debug("Debug evaluate failed", e);
                return LegendDebug.EvaluateResult.error(message(e));
            }
        }
    }

    LegendDebug.EvaluateResult evaluate(String expression)
    {
        return evaluate(expression, 0);
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
                            stackFrames(state),
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
            case STEP_OUT:
                return isStepOutTarget(startLocation, pauseLocation) ? PauseDecision.pause("step") : PauseDecision.resume();
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
        return !pauseLocation.location.sameLine(startLocation.location);
    }

    private boolean isStepOutTarget(PauseLocation startLocation, PauseLocation pauseLocation)
    {
        return startLocation != null
                && pauseLocation != null
                && pauseLocation.stackDepth < startLocation.stackDepth
                && !sameLocation(startLocation, pauseLocation);
    }

    private PauseLocation currentPauseLocation()
    {
        LegendDebugState state = this.functionExecution.getDebugState();
        return state == null ? null : currentPauseLocation(state);
    }

    private PauseLocation currentPauseLocation(LegendDebugState state)
    {
        DebugExecutionLocation location = state.getCurrentLocation();
        return location == null ? null : new PauseLocation(location, isUserBreakpoint(location), location.isExplicitDebug());
    }

    private List<LegendDebug.StackFrame> stackFrames(LegendDebugState state)
    {
        List<LegendDebug.StackFrame> result = new ArrayList<>();
        for (DebugFrameSnapshot frame : state.getFrames())
        {
            DebugExecutionLocation location = frame.getLocation();
            if (location == null)
            {
                continue;
            }
            result.add(new LegendDebug.StackFrame(
                    frame.getId(),
                    frame.getName(),
                    location.getUri(),
                    location.getLine(),
                    location.getColumn(),
                    location.getEndLine(),
                    location.getEndColumn(),
                    frame.getVariablesReference()));
        }
        return result;
    }

    private boolean isUserBreakpoint(DebugExecutionLocation location)
    {
        LineMap lineMap = location == null ? null : this.lineMaps.get(location.getSourceId());
        return lineMap != null && lineMap.isUserBreakpoint(location.getLine());
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
                    LspLog.info("Skipping debug breakpoint for unknown source: "
                            + breakpoint.getUri() + " (derived sourceId=" + sourceId + ")");
                    continue;
                }
            }

            grouped.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(breakpoint.getLine());
        }
        return grouped;
    }

    private static int breakpointCount(List<LegendDebug.Breakpoint> breakpoints)
    {
        return breakpoints == null ? 0 : breakpoints.size();
    }

    private static int groupedBreakpointCount(Map<String, List<Integer>> breakpointsBySource)
    {
        int count = 0;
        for (List<Integer> sourceBreakpoints : breakpointsBySource.values())
        {
            count += sourceBreakpoints.size();
        }
        return count;
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
        String[] lines = content == null ? new String[0] : content.split("\\R", -1);
        LineMap lineMap = new LineMap();
        lineMap.addUserBreakpoints(validatedBreakpointLines(lines, breakpointLines));
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
            if (line != null && line >= 0 && line < lines.length)
            {
                targets.add(line + 1);
            }
        }
        return targets;
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
                && first.location.sameRange(second.location);
    }

    private static String message(Exception e)
    {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private enum RunMode
    {
        CONTINUE,
        STEP_IN,
        STEP_OVER,
        STEP_OUT
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
        private final DebugExecutionLocation location;
        private final boolean userBreakpoint;
        private final boolean explicitDebug;
        private final int stackDepth;

        private PauseLocation(DebugExecutionLocation location, boolean userBreakpoint, boolean explicitDebug)
        {
            this.location = location;
            this.userBreakpoint = userBreakpoint;
            this.explicitDebug = explicitDebug;
            this.stackDepth = location == null ? 0 : location.getStackDepth();
        }
    }

    private static class LineMap
    {
        private final Set<Integer> userBreakpointOriginalLines = new TreeSet<>();

        private LineMap()
        {
        }

        private void addUserBreakpoints(Collection<Integer> originalLines)
        {
            this.userBreakpointOriginalLines.addAll(originalLines);
        }

        private boolean isUserBreakpoint(int originalLineOneBased)
        {
            return this.userBreakpointOriginalLines.contains(originalLineOneBased);
        }
    }
}
