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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.lsp.ExecutionFailureFormatter;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.LspLog;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
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
    private final UriMapper uriMapper;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Object executionLock = new Object();
    private final Object pauseStateLock = new Object();
    // Non-null only for a SHARED-mode session: the main session's graph read lock, held from creation
    // until this session goes terminal (completed/error) or is explicitly stopped - see terminal(...)
    // and stop(). Held across an in-between pause so main-session compiles block while paused; that is
    // the deliberate tradeoff of SHARED mode (see LegendDebug.ExecutionMode).
    private final LegendPureSession.LockHandle heldMainSessionReadLock;
    private final java.util.concurrent.atomic.AtomicBoolean sharedLockReleased = new java.util.concurrent.atomic.AtomicBoolean(false);

    private volatile boolean stopped;
    private volatile LegendDebugState visiblePausedState;
    private int outputOffset;

    private LegendDebugSession(LegendDebugFunctionExecution functionExecution, CoreInstance function,
                               Map<String, LineMap> lineMaps, UriMapper uriMapper)
    {
        this(functionExecution, function, lineMaps, uriMapper, null);
    }

    private LegendDebugSession(LegendDebugFunctionExecution functionExecution, CoreInstance function,
                               Map<String, LineMap> lineMaps, UriMapper uriMapper,
                               LegendPureSession.LockHandle heldMainSessionReadLock)
    {
        this.functionExecution = functionExecution;
        this.function = function;
        this.lineMaps = lineMaps;
        this.uriMapper = uriMapper;
        this.heldMainSessionReadLock = heldMainSessionReadLock;

        Console console = this.functionExecution.getConsole();
        console.setPrintStream(new PrintStream(this.output, true));
        console.setConsole(true);
    }

    static LegendDebugSession create(LegendPureSession mainSession, RepositoryScanner repositoryScanner,
                                     UriMapper uriMapper, Map<String, String> openDocuments,
                                     String functionName, List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<String, String> sources = snapshotSources(mainSession, repositoryScanner, openDocuments);
        Map<String, List<LegendDebug.Breakpoint>> breakpointsBySource = groupBreakpointsBySource(uriMapper, sources, breakpoints);
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

        LegendDebugFunctionExecution debugExecution = LegendPureSession.initializeFunctionExecution(
                new LegendDebugFunctionExecution(lineMaps.keySet(), uriMapper),
                debugRuntime,
                new Message(""));

        CoreInstance function = findZeroArgumentFunction(debugRuntime, functionName);
        if (function == null)
        {
            throw new IllegalArgumentException("No zero-argument function found for " + normalizeFunctionName(functionName));
        }

        return new LegendDebugSession(debugExecution, function, lineMaps, uriMapper);
    }

    /**
     * SHARED mode: attach directly to the main session's already-compiled PureRuntime under its graph
     * read lock instead of building/compiling a second runtime - mirrors how normal (non-debug)
     * execution attaches a fresh FunctionExecutionInterpreted to the shared runtime
     * (LegendPureSession#executeFunctionByCandidates). No unsaved-editor-buffer overlay is possible here
     * (only what's already compiled into the live graph is debuggable), and the read lock is held for
     * this session's whole lifetime, released in terminal(...)/stop().
     */
    static LegendDebugSession createShared(LegendPureSession mainSession, RepositoryScanner repositoryScanner,
                                           UriMapper uriMapper, String functionName,
                                           List<LegendDebug.Breakpoint> breakpoints)
    {
        LegendPureSession.LockHandle readLock = mainSession.acquireGraphReadLock();
        boolean releaseOnFailure = true;
        try
        {
            PureRuntime runtime = mainSession.getPureRuntime();
            Set<String> workspaceRepositoryNames = repositoryScanner == null
                    ? Collections.emptySet()
                    : repositoryScanner.getWorkspaceRepoNames();
            Set<String> runtimeDependencyRepositoryNames = mainSession.getClasspathRepositoryNames();
            Map<String, String> sources = new TreeMap<>();
            for (Source source : runtime.getSourceRegistry().getSources())
            {
                if (isDebuggableSource(source, workspaceRepositoryNames, runtimeDependencyRepositoryNames))
                {
                    sources.put(source.getId(), source.getContent());
                }
            }

            Map<String, List<LegendDebug.Breakpoint>> breakpointsBySource = groupBreakpointsBySource(uriMapper, sources, breakpoints);
            Map<String, LineMap> lineMaps = new TreeMap<>();
            for (Map.Entry<String, String> entry : sources.entrySet())
            {
                lineMaps.put(entry.getKey(), lineMapForSource(entry.getValue(), breakpointsBySource.get(entry.getKey())));
            }
            LspLog.info("Shared debug session attached to main runtime (" + sources.size()
                    + " debuggable source(s)); requested " + breakpointCount(breakpoints)
                    + " breakpoint(s); mapped " + groupedBreakpointCount(breakpointsBySource)
                    + " breakpoint(s) across " + breakpointsBySource.size() + " source(s)");

            LegendDebugFunctionExecution debugExecution = LegendPureSession.initializeFunctionExecution(
                    new LegendDebugFunctionExecution(lineMaps.keySet(), uriMapper),
                    runtime,
                    new Message(""));

            CoreInstance function = findZeroArgumentFunction(runtime, functionName);
            if (function == null)
            {
                throw new IllegalArgumentException("No zero-argument function found for " + normalizeFunctionName(functionName));
            }

            LegendDebugSession session = new LegendDebugSession(debugExecution, function, lineMaps, uriMapper, readLock);
            releaseOnFailure = false;
            return session;
        }
        finally
        {
            if (releaseOnFailure)
            {
                readLock.close();
            }
        }
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

    /**
     * Re-derives every known source's breakpoint lines (and conditions) from a fresh full breakpoint
     * list, so a client's add/remove/edit while this session is already running takes effect on the
     * very next line check - {@link LineMap} swaps its breakpoint map atomically, so this is safe to
     * call concurrently with an in-flight {@link #runUntilPauseOrCompletion}. A source not known to this
     * session (e.g. opened only after the session started) is left alone; {@link #groupBreakpointsBySource}
     * already logs that case.
     */
    void updateBreakpoints(List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<String, String> knownSources = new TreeMap<>();
        for (Map.Entry<String, LineMap> entry : this.lineMaps.entrySet())
        {
            String content = debugSourceContent(entry.getKey());
            if (content != null)
            {
                knownSources.put(entry.getKey(), content);
            }
        }

        Map<String, List<LegendDebug.Breakpoint>> breakpointsBySource = groupBreakpointsBySource(this.uriMapper, knownSources, breakpoints);
        for (Map.Entry<String, String> entry : knownSources.entrySet())
        {
            String[] lines = entry.getValue().split("\\R", -1);
            this.lineMaps.get(entry.getKey()).replace(validatedBreakpointLines(lines, breakpointsBySource.get(entry.getKey())));
        }
        LspLog.info("Updated debug breakpoints: requested " + breakpointCount(breakpoints)
                + " breakpoint(s); mapped " + groupedBreakpointCount(breakpointsBySource)
                + " breakpoint(s) across " + breakpointsBySource.size() + " source(s)");
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
        releaseSharedRuntimeLockIfHeld();
        return LegendDebug.Response.completed(readNewUserOutput());
    }

    /**
     * Releases the SHARED-mode main-session read lock exactly once, whether reached via an explicit
     * stop() or via a terminal (non-paused) response falling out of runUntilPauseOrCompletion - both
     * paths can race (e.g. stop() called from another thread while the run loop is mid-flight), so this
     * must be idempotent.
     */
    private void releaseSharedRuntimeLockIfHeld()
    {
        if (this.heldMainSessionReadLock != null && this.sharedLockReleased.compareAndSet(false, true))
        {
            this.heldMainSessionReadLock.close();
        }
    }

    private LegendDebug.Response terminal(LegendDebug.Response response)
    {
        releaseSharedRuntimeLockIfHeld();
        return response;
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

    /**
     * Same captured-output text {@link #readNewUserOutput()} would return, but as a peek: it does not
     * advance {@link #outputOffset}, since a caller inspecting output after a failed
     * {@link #runUntilPauseOrCompletion} (e.g. {@code DebugService#start}, which has no local
     * {@code visibleOutput} of its own) must not disturb what a subsequent real read would see.
     */
    String snapshotCapturedOutput()
    {
        String value = new String(this.output.toByteArray(), StandardCharsets.UTF_8);
        int offset = this.outputOffset > value.length() ? 0 : this.outputOffset;
        return stripDebugConsoleText(value.substring(offset));
    }

    ProcessorSupport processorSupport()
    {
        return this.functionExecution.getProcessorSupport();
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
                return terminal(LegendDebug.Response.error("Debug execution is not paused"));
            }

            PauseLocation startLocation = currentPauseLocation();
            RunMode effectiveMode = effectiveRunMode(mode, startLocation);
            clearVisiblePausedState();
            StringBuilder visibleOutput = new StringBuilder();
            while (true)
            {
                if (this.stopped)
                {
                    clearVisiblePausedState();
                    visibleOutput.append(readNewUserOutput());
                    return terminal(LegendDebug.Response.completed(visibleOutput.toString()));
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
                        return terminal(LegendDebug.Response.completed(visibleOutput.toString()));
                    }
                    LOGGER.debug("Debug execution failed", e);
                    String formatted = ExecutionFailureFormatter.format(e, visibleOutput.toString(), this.functionExecution.getProcessorSupport());
                    return terminal(LegendDebug.Response.error(formatted));
                }

                visibleOutput.append(readNewUserOutput());
                LegendDebugState state = this.functionExecution.getDebugState();
                if (state == null)
                {
                    this.functionExecution.getConsole().setConsole(false);
                    clearVisiblePausedState();
                    return terminal(LegendDebug.Response.completed(visibleOutput.toString()));
                }

                PauseLocation pauseLocation = currentPauseLocation(state);
                PauseDecision decision = pauseDecision(effectiveMode, startLocation, pauseLocation, state);
                if (decision.note != null)
                {
                    visibleOutput.append(decision.note);
                }
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

    private PauseDecision pauseDecision(RunMode mode, PauseLocation startLocation, PauseLocation pauseLocation, LegendDebugState state)
    {
        if (pauseLocation == null)
        {
            return PauseDecision.resume();
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
                if (!pauseLocation.userBreakpoint)
                {
                    return PauseDecision.resume();
                }
                return breakpointDecision(pauseLocation, state);
        }
    }

    /**
     * A blank condition always fires. A non-blank condition is evaluated in the paused frame via the
     * already-suppressed-pauses {@link LegendDebugState#evaluate(String, int)} mini-REPL; a broken
     * condition fails OPEN (pauses anyway, and says why on the console) since silently running past a
     * breakpoint the user is relying on is worse than a spurious pause, matching IntelliJ's Java
     * debugger. A breakpoint carrying a log message is a DAP logpoint: it logs and keeps running.
     */
    private PauseDecision breakpointDecision(PauseLocation pauseLocation, LegendDebugState state)
    {
        BreakpointSpec spec = pauseLocation.breakpointSpec;
        String condition = spec == null ? null : spec.condition;
        String logMessage = spec == null ? null : spec.logMessage;

        if (condition != null)
        {
            LegendDebug.EvaluateResult result;
            try
            {
                result = state.evaluate(condition, 0);
            }
            catch (Exception e)
            {
                LOGGER.warn("Breakpoint condition '" + condition + "' threw; pausing", e);
                return PauseDecision.pause("breakpoint", conditionFailureNote(condition, e.toString()));
            }

            if (!result.isSuccess())
            {
                LOGGER.warn("Breakpoint condition '{}' failed to evaluate ({}); pausing", condition, result.getError());
                return PauseDecision.pause("breakpoint", conditionFailureNote(condition, result.getError()));
            }
            if (!"true".equals(result.getResult()))
            {
                return PauseDecision.resume();
            }
        }

        return logMessage == null
                ? PauseDecision.pause("breakpoint")
                : PauseDecision.resume(interpolateLogMessage(logMessage, pauseLocation, state));
    }

    private static String conditionFailureNote(String condition, String error)
    {
        return "Breakpoint condition \"" + condition + "\" could not be evaluated ("
                + (error == null ? "unknown error" : error) + "); pausing.\n";
    }

    /**
     * Substitutes each {@code {expression}} segment with its value evaluated in the paused frame,
     * leaving the placeholder text in place (rather than aborting the whole line) if one fails - a
     * logpoint that partly resolves is more useful than no output at all.
     */
    private String interpolateLogMessage(String logMessage, PauseLocation pauseLocation, LegendDebugState state)
    {
        StringBuilder rendered = new StringBuilder();
        int index = 0;
        while (index < logMessage.length())
        {
            int open = logMessage.indexOf('{', index);
            int close = open < 0 ? -1 : logMessage.indexOf('}', open + 1);
            if (open < 0 || close < 0)
            {
                rendered.append(logMessage, index, logMessage.length());
                break;
            }
            rendered.append(logMessage, index, open);
            String expression = logMessage.substring(open + 1, close).trim();
            rendered.append(expression.isEmpty() ? "" : evaluateForLog(expression, state));
            index = close + 1;
        }
        String location = pauseLocation.location == null
                ? ""
                : " (" + pauseLocation.location.getSourceId() + ":" + pauseLocation.location.getLine() + ")";
        return rendered.toString() + location + "\n";
    }

    private String evaluateForLog(String expression, LegendDebugState state)
    {
        try
        {
            LegendDebug.EvaluateResult result = state.evaluate(expression, 0);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "{" + expression + "=?}";
        }
        catch (Exception e)
        {
            LOGGER.debug("Logpoint expression evaluation failed", e);
            return "{" + expression + "=?}";
        }
    }

    private RunMode effectiveRunMode(RunMode mode, PauseLocation startLocation)
    {
        // A red-dot breakpoint stops before its expression executes. Stepping out
        // from that state should first move past the breakpoint expression.
        return mode == RunMode.STEP_OUT
                && startLocation != null
                && startLocation.userBreakpoint
                ? RunMode.STEP_OVER
                : mode;
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
        if (location == null)
        {
            return null;
        }
        LineMap lineMap = this.lineMaps.get(location.getSourceId());
        boolean userBreakpoint = lineMap != null && lineMap.isUserBreakpoint(location.getLine());
        BreakpointSpec spec = lineMap == null ? null : lineMap.specForLine(location.getLine());
        return new PauseLocation(location, userBreakpoint, spec);
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
        try (LegendPureSession.LockHandle ignored = mainSession.acquireGraphReadLock())
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

    private static Map<String, List<LegendDebug.Breakpoint>> groupBreakpointsBySource(UriMapper uriMapper, Map<String, String> sources,
                                                                                       List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<String, List<LegendDebug.Breakpoint>> grouped = new TreeMap<>();
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
            if (sourceId == null)
            {
                // A client can set a breakpoint in any .pure-suffixed file it has open, including a
                // genuine non-module fixture (see UriMapper#deriveSourceId) - sources is a TreeMap, whose
                // natural-ordering containsKey/put reject a null key outright, so this must be filtered
                // before it ever reaches that map.
                LspLog.info("Skipping debug breakpoint for non-module source: " + breakpoint.getUri());
                continue;
            }
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

            grouped.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(breakpoint);
        }
        return grouped;
    }

    private static int breakpointCount(List<LegendDebug.Breakpoint> breakpoints)
    {
        return breakpoints == null ? 0 : breakpoints.size();
    }

    private static int groupedBreakpointCount(Map<String, List<LegendDebug.Breakpoint>> breakpointsBySource)
    {
        int count = 0;
        for (List<LegendDebug.Breakpoint> sourceBreakpoints : breakpointsBySource.values())
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

    private static LineMap lineMapForSource(String content, List<LegendDebug.Breakpoint> breakpoints)
    {
        String[] lines = content == null ? new String[0] : content.split("\\R", -1);
        return new LineMap(validatedBreakpointLines(lines, breakpoints));
    }

    private static Map<Integer, BreakpointSpec> validatedBreakpointLines(String[] lines, List<LegendDebug.Breakpoint> breakpoints)
    {
        Map<Integer, BreakpointSpec> targets = new TreeMap<>();
        if (breakpoints == null)
        {
            return targets;
        }
        for (LegendDebug.Breakpoint breakpoint : breakpoints)
        {
            int line = breakpoint == null ? -1 : breakpoint.getLine();
            if (line >= 0 && line < lines.length)
            {
                targets.put(line + 1, new BreakpointSpec(
                        blankToNull(breakpoint.getCondition()),
                        blankToNull(breakpoint.getLogMessage())));
            }
        }
        return targets;
    }

    private static String blankToNull(String condition)
    {
        return condition == null || condition.trim().isEmpty() ? null : condition.trim();
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
        /** Console text to surface with this decision (logpoint output, or why a condition failed open). */
        private final String note;

        private PauseDecision(boolean pause, String reason, String note)
        {
            this.pause = pause;
            this.reason = reason;
            this.note = note;
        }

        private static PauseDecision pause(String reason)
        {
            return new PauseDecision(true, reason, null);
        }

        private static PauseDecision pause(String reason, String note)
        {
            return new PauseDecision(true, reason, note);
        }

        private static PauseDecision resume()
        {
            return new PauseDecision(false, null, null);
        }

        private static PauseDecision resume(String note)
        {
            return new PauseDecision(false, null, note);
        }
    }

    /** Per-line breakpoint settings; a null condition always fires, a non-null logMessage never suspends. */
    private static class BreakpointSpec
    {
        private final String condition;
        private final String logMessage;

        private BreakpointSpec(String condition, String logMessage)
        {
            this.condition = condition;
            this.logMessage = logMessage;
        }
    }

    private static class PauseLocation
    {
        private final DebugExecutionLocation location;
        private final boolean userBreakpoint;
        private final BreakpointSpec breakpointSpec;
        private final int stackDepth;

        private PauseLocation(DebugExecutionLocation location, boolean userBreakpoint, BreakpointSpec breakpointSpec)
        {
            this.location = location;
            this.userBreakpoint = userBreakpoint;
            this.breakpointSpec = breakpointSpec;
            this.stackDepth = location == null ? 0 : location.getStackDepth();
        }
    }

    /**
     * Line -> settings; presence of a key is what makes a line a breakpoint at all. The map is swapped
     * as a whole, immutable snapshot so a live breakpoint update (see {@link #updateBreakpoints}) is safe
     * to race against the interpreter thread's line checks without any extra locking.
     */
    private static class LineMap
    {
        private volatile Map<Integer, BreakpointSpec> breakpointsByLine;

        private LineMap(Map<Integer, BreakpointSpec> breakpointsByLine)
        {
            this.breakpointsByLine = snapshot(breakpointsByLine);
        }

        private void replace(Map<Integer, BreakpointSpec> breakpointsByLine)
        {
            this.breakpointsByLine = snapshot(breakpointsByLine);
        }

        private boolean isUserBreakpoint(int originalLineOneBased)
        {
            return this.breakpointsByLine.containsKey(originalLineOneBased);
        }

        private BreakpointSpec specForLine(int originalLineOneBased)
        {
            return this.breakpointsByLine.get(originalLineOneBased);
        }

        private static Map<Integer, BreakpointSpec> snapshot(Map<Integer, BreakpointSpec> source)
        {
            return source == null || source.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new TreeMap<>(source));
        }
    }
}
