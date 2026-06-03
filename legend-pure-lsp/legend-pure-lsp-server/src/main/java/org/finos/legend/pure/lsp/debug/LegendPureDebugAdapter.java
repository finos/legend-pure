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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.ContinuedEventArguments;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LegendPureDebugAdapter implements IDebugProtocolServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureDebugAdapter.class);
    private static final int THREAD_ID = 1;
    private static final String DEFAULT_FUNCTION = "go():Any[*]";
    private static final long BREAKPOINT_CONFIGURATION_GRACE_MS = 250L;

    private final DebugService debugService;
    private final ExecutorService runExecutor = Executors.newSingleThreadExecutor(r ->
    {
        Thread thread = new Thread(r, "legend-pure-dap-run");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService protocolExecutor = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread thread = new Thread(r, "legend-pure-dap-protocol");
        thread.setDaemon(true);
        return thread;
    });
    private final List<LegendDebug.Breakpoint> breakpoints = new ArrayList<>();

    private volatile IDebugProtocolClient client;
    private volatile String function = DEFAULT_FUNCTION;
    private volatile boolean configurationDone;
    private volatile boolean launched;
    private volatile boolean started;
    private volatile boolean startScheduled;
    private volatile boolean receivedBreakpoints;
    private volatile boolean breakpointConfigurationGraceElapsed;
    private volatile boolean clientLinesStartAt1 = true;
    private volatile boolean clientColumnsStartAt1 = true;
    private volatile State state = State.IDLE;
    private volatile List<LegendDebug.StackFrame> stackFrames = Collections.emptyList();

    LegendPureDebugAdapter(DebugService debugService)
    {
        this.debugService = debugService;
    }

    void connect(IDebugProtocolClient client)
    {
        this.client = client;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args)
    {
        this.clientLinesStartAt1 = args == null || !Boolean.FALSE.equals(args.getLinesStartAt1());
        this.clientColumnsStartAt1 = args == null || !Boolean.FALSE.equals(args.getColumnsStartAt1());

        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(Boolean.TRUE);
        capabilities.setSupportsEvaluateForHovers(Boolean.TRUE);
        capabilities.setSupportsTerminateRequest(Boolean.TRUE);
        capabilities.setSupportsSteppingGranularity(Boolean.FALSE);
        capabilities.setSupportsConditionalBreakpoints(Boolean.FALSE);
        capabilities.setSupportsFunctionBreakpoints(Boolean.FALSE);
        capabilities.setSupportsDataBreakpoints(Boolean.FALSE);
        IDebugProtocolClient currentClient = this.client;
        if (currentClient != null)
        {
            this.protocolExecutor.schedule(currentClient::initialized, 10L, TimeUnit.MILLISECONDS);
        }
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args)
    {
        Object configuredFunction = args == null ? null : args.get("function");
        this.function = configuredFunction == null || configuredFunction.toString().trim().isEmpty()
                ? DEFAULT_FUNCTION
                : configuredFunction.toString().trim();
        this.launched = true;
        LOGGER.info("Legend Pure DAP launch requested for {}", this.function);
        maybeStart();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args)
    {
        this.configurationDone = true;
        LOGGER.info("Legend Pure DAP configuration complete");
        maybeStart();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args)
    {
        Source source = args == null ? null : args.getSource();
        String path = source == null ? null : source.getPath();
        String uri = path == null ? null : fileUri(path);
        List<LegendDebug.Breakpoint> accepted = new ArrayList<>();
        SourceBreakpoint[] requested = args == null ? null : args.getBreakpoints();
        Breakpoint[] responseBreakpoints = requested == null ? new Breakpoint[0] : new Breakpoint[requested.length];
        for (int i = 0; i < responseBreakpoints.length; i++)
        {
            SourceBreakpoint requestedBreakpoint = requested[i];
            int oneBasedLine = requestedBreakpoint == null ? 1 : requestedBreakpoint.getLine();
            boolean verified = uri != null && path != null && path.endsWith(".pure");
            if (verified)
            {
                accepted.add(new LegendDebug.Breakpoint(uri, Math.max(0, oneBasedLine - 1)));
            }
            Breakpoint breakpoint = new Breakpoint();
            breakpoint.setVerified(verified);
            breakpoint.setSource(source);
            breakpoint.setLine(oneBasedLine);
            if (!verified)
            {
                breakpoint.setMessage("Only workspace .pure file breakpoints are supported");
            }
            responseBreakpoints[i] = breakpoint;
        }
        synchronized (this.breakpoints)
        {
            this.receivedBreakpoints = true;
            this.breakpoints.removeIf(existing -> uri != null && uri.equals(existing.getUri()));
            this.breakpoints.addAll(accepted);
        }
        LOGGER.info("Legend Pure DAP registered {} verified breakpoint(s) for {}", accepted.size(), path);
        maybeStart();
        SetBreakpointsResponse response = new SetBreakpointsResponse();
        response.setBreakpoints(responseBreakpoints);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads()
    {
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setId(THREAD_ID);
        thread.setName("Pure");
        ThreadsResponse response = new ThreadsResponse();
        response.setThreads(new org.eclipse.lsp4j.debug.Thread[] {thread});
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args)
    {
        List<LegendDebug.StackFrame> frames = this.state == State.PAUSED ? this.stackFrames : Collections.emptyList();
        StackFrame[] dapFrames = frames.stream().map(this::toDapStackFrame).toArray(StackFrame[]::new);
        StackTraceResponse response = new StackTraceResponse();
        response.setStackFrames(dapFrames);
        response.setTotalFrames(dapFrames.length);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args)
    {
        if (this.state != State.PAUSED)
        {
            ScopesResponse empty = new ScopesResponse();
            empty.setScopes(new Scope[0]);
            return CompletableFuture.completedFuture(empty);
        }

        int frameId = args == null ? 1 : args.getFrameId();
        int variablesReference = this.stackFrames.stream()
                .filter(frame -> frame.getId() == frameId)
                .findFirst()
                .map(LegendDebug.StackFrame::getVariablesReference)
                .orElse(1);

        Scope locals = new Scope();
        locals.setName("Locals");
        locals.setPresentationHint("locals");
        locals.setVariablesReference(variablesReference);
        locals.setExpensive(false);
        ScopesResponse response = new ScopesResponse();
        response.setScopes(new Scope[] {locals});
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args)
    {
        int reference = args == null ? 1 : args.getVariablesReference();
        LegendDebug.VariablesParams params = new LegendDebug.VariablesParams();
        params.setVariablesReference(reference);
        List<LegendDebug.Variable> variables = this.state == State.PAUSED
                ? this.debugService.variables(params)
                : Collections.emptyList();

        VariablesResponse response = new VariablesResponse();
        response.setVariables(variables.stream().map(this::toDapVariable).toArray(Variable[]::new));
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args)
    {
        LegendDebug.EvaluateParams params = new LegendDebug.EvaluateParams();
        params.setExpression(args == null ? "" : args.getExpression());
        params.setFrameId(args == null || args.getFrameId() == null ? 0 : args.getFrameId());
        LegendDebug.EvaluateResult result = this.debugService.evaluate(params);
        if (!result.isSuccess())
        {
            return failed(result.getError() == null ? "Evaluation failed" : result.getError());
        }
        EvaluateResponse response = new EvaluateResponse();
        response.setResult(result.getResult() == null ? "" : result.getResult());
        response.setVariablesReference(result.getVariablesReference());
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args)
    {
        resume("continue", this.debugService::continueExecution);
        ContinueResponse response = new ContinueResponse();
        response.setAllThreadsContinued(Boolean.TRUE);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args)
    {
        resume("next", this.debugService::stepOver);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args)
    {
        resume("stepIn", this.debugService::stepIn);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args)
    {
        resume("stepOut", this.debugService::stepOut);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args)
    {
        stop();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args)
    {
        stop();
        return CompletableFuture.completedFuture(null);
    }

    private void maybeStart()
    {
        if (!this.launched || !this.configurationDone || this.started)
        {
            return;
        }
        if (!this.receivedBreakpoints && !this.breakpointConfigurationGraceElapsed && !this.startScheduled)
        {
            this.startScheduled = true;
            this.protocolExecutor.schedule(() ->
            {
                this.startScheduled = false;
                this.breakpointConfigurationGraceElapsed = true;
                maybeStart();
            }, BREAKPOINT_CONFIGURATION_GRACE_MS, TimeUnit.MILLISECONDS);
            return;
        }
        this.started = true;
        this.state = State.RUNNING;
        LegendDebug.StartParams params = startParams();
        LOGGER.info("Legend Pure DAP starting {} with {} breakpoint(s)",
                params.getFunction(),
                params.getBreakpoints() == null ? 0 : params.getBreakpoints().size());
        this.runExecutor.submit(() -> handleResponse(this.debugService.start(params)));
    }

    private LegendDebug.StartParams startParams()
    {
        LegendDebug.StartParams params = new LegendDebug.StartParams();
        params.setFunction(this.function);
        synchronized (this.breakpoints)
        {
            params.setBreakpoints(new ArrayList<>(this.breakpoints));
        }
        return params;
    }

    private void resume(String command, DebugCommand debugCommand)
    {
        if (this.state != State.PAUSED)
        {
            return;
        }
        this.state = State.RUNNING;
        this.stackFrames = Collections.emptyList();
        IDebugProtocolClient currentClient = this.client;
        if (currentClient != null)
        {
            ContinuedEventArguments continued = new ContinuedEventArguments();
            continued.setThreadId(THREAD_ID);
            continued.setAllThreadsContinued(Boolean.TRUE);
            currentClient.continued(continued);
        }
        this.runExecutor.submit(() -> handleResponse(debugCommand.run()));
    }

    private void handleResponse(LegendDebug.Response response)
    {
        IDebugProtocolClient currentClient = this.client;
        if (response == null)
        {
            this.state = State.TERMINATED;
            sendTerminated(currentClient);
            return;
        }

        if (response.getOutput() != null && !response.getOutput().isEmpty())
        {
            sendOutput(currentClient, response.getOutput(), "console");
        }

        if (!response.isSuccess() || "error".equals(response.getState()))
        {
            sendOutput(currentClient, "ERROR: " + (response.getMessage() == null ? "Debug execution failed" : response.getMessage()) + "\n", "stderr");
            this.state = State.TERMINATED;
            sendTerminated(currentClient);
            return;
        }

        if ("paused".equals(response.getState()))
        {
            this.stackFrames = response.getStackFrames() == null ? Collections.emptyList() : response.getStackFrames();
            this.state = State.PAUSED;
            StoppedEventArguments stopped = new StoppedEventArguments();
            stopped.setReason(response.getReason() == null ? "breakpoint" : response.getReason());
            stopped.setThreadId(THREAD_ID);
            stopped.setAllThreadsStopped(Boolean.TRUE);
            if (currentClient != null)
            {
                currentClient.stopped(stopped);
            }
            return;
        }

        this.state = State.TERMINATED;
        sendTerminated(currentClient);
    }

    private void stop()
    {
        if (this.state == State.TERMINATED)
        {
            return;
        }
        this.debugService.stop();
        this.state = State.TERMINATED;
        this.stackFrames = Collections.emptyList();
        sendTerminated(this.client);
        this.runExecutor.shutdownNow();
        this.protocolExecutor.shutdownNow();
    }

    private void sendOutput(IDebugProtocolClient currentClient, String output, String category)
    {
        if (currentClient == null)
        {
            return;
        }
        OutputEventArguments event = new OutputEventArguments();
        event.setCategory(category);
        event.setOutput(output.endsWith("\n") ? output : output + "\n");
        currentClient.output(event);
    }

    private void sendTerminated(IDebugProtocolClient currentClient)
    {
        if (currentClient != null)
        {
            currentClient.terminated(new TerminatedEventArguments());
        }
    }

    private StackFrame toDapStackFrame(LegendDebug.StackFrame frame)
    {
        StackFrame dap = new StackFrame();
        dap.setId(frame.getId());
        dap.setName(frame.getName() == null ? "Pure debug point" : frame.getName());
        dap.setSource(toDapSource(frame.getUri()));
        dap.setLine(toClientLine(frame.getLine()));
        dap.setColumn(toClientColumn(frame.getColumn()));
        dap.setEndLine(toClientLine(frame.getEndLine()));
        dap.setEndColumn(toClientEndColumn(frame.getEndColumn()));
        return dap;
    }

    private int toClientLine(int pureOneBasedLine)
    {
        int oneBased = Math.max(1, pureOneBasedLine);
        return this.clientLinesStartAt1 ? oneBased : oneBased - 1;
    }

    private int toClientColumn(int pureOneBasedColumn)
    {
        int oneBased = Math.max(1, pureOneBasedColumn);
        return this.clientColumnsStartAt1 ? oneBased : oneBased - 1;
    }

    private int toClientEndColumn(int pureOneBasedEndColumn)
    {
        int oneBased = Math.max(1, pureOneBasedEndColumn);
        // Pure end columns are inclusive. LSP/DAP ranges are presented better
        // with the same convention used by SourceInfoUtil: start shifts to
        // zero-based when requested, end remains the exclusive boundary.
        return this.clientColumnsStartAt1 ? oneBased : oneBased;
    }

    private Source toDapSource(String uri)
    {
        if (uri == null)
        {
            return null;
        }
        Source source = new Source();
        try
        {
            URI parsed = URI.create(uri);
            if ("file".equals(parsed.getScheme()))
            {
                File file = new File(parsed);
                source.setName(file.getName());
                source.setPath(file.getAbsolutePath());
                return source;
            }
        }
        catch (Exception ignored)
        {
        }
        source.setName(uri.substring(uri.lastIndexOf('/') + 1));
        source.setPath(uri);
        return source;
    }

    private Variable toDapVariable(LegendDebug.Variable variable)
    {
        Variable dap = new Variable();
        dap.setName(variable.getName());
        dap.setValue(variable.getValue() == null ? "" : variable.getValue());
        dap.setType(variable.getType() == null ? "" : variable.getType());
        dap.setVariablesReference(variable.getVariablesReference());
        return dap;
    }

    private static String fileUri(String path)
    {
        try
        {
            return new File(path).toURI().toString();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static <T> CompletableFuture<T> failed(String message)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException(message));
        return future;
    }

    private enum State
    {
        IDLE,
        RUNNING,
        PAUSED,
        TERMINATED
    }

    private interface DebugCommand
    {
        LegendDebug.Response run();
    }
}
