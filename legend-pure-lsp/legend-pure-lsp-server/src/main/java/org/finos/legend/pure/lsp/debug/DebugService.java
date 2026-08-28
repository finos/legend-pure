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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.finos.legend.pure.lsp.ExecutionFailureFormatter;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.finos.legend.pure.lsp.runtime.PureRuntimeManager;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugService.class);

    private final PureRuntimeManager runtimeManager;
    private final RepositoryScanner repositoryScanner;
    private final UriMapper uriMapper;
    private final Supplier<Map<String, String>> openDocumentSourceSnapshot;

    private volatile LegendDebugSession debugSession;
    private int sessionGeneration;

    public DebugService(PureRuntimeManager runtimeManager, RepositoryScanner repositoryScanner,
                        UriMapper uriMapper, Supplier<Map<String, String>> openDocumentSourceSnapshot)
    {
        this.runtimeManager = runtimeManager;
        this.repositoryScanner = repositoryScanner;
        this.uriMapper = uriMapper;
        this.openDocumentSourceSnapshot = openDocumentSourceSnapshot;
    }

    public LegendDebug.Response start(LegendDebug.StartParams params)
    {
        LegendPureSession session = this.runtimeManager.getSession();
        if (session == null || !session.isInitialized())
        {
            return LegendDebug.Response.error("Runtime not initialized");
        }

        int startGeneration;
        LegendDebugSession previousSession;
        synchronized (this)
        {
            startGeneration = ++this.sessionGeneration;
            previousSession = this.debugSession;
            this.debugSession = null;
        }
        stopSession(previousSession);

        LegendDebug.ExecutionMode mode = params == null ? LegendDebug.ExecutionMode.SHARED : params.getMode();
        LegendDebugSession nextSession = null;
        try
        {
            nextSession = mode == LegendDebug.ExecutionMode.SHARED
                    ? LegendDebugSession.createShared(
                            session,
                            this.repositoryScanner,
                            this.uriMapper,
                            params == null ? null : params.getFunction(),
                            params == null ? Collections.emptyList() : params.getBreakpoints())
                    : LegendDebugSession.create(
                            session,
                            this.repositoryScanner,
                            this.uriMapper,
                            this.openDocumentSourceSnapshot.get(),
                            params == null ? null : params.getFunction(),
                            params == null ? Collections.emptyList() : params.getBreakpoints());
            if (!setActiveSession(startGeneration, nextSession))
            {
                nextSession.stop();
                return LegendDebug.Response.completed(null);
            }

            LegendDebug.Response response = nextSession.start();
            clearIfTerminal(nextSession, response);
            return response;
        }
        catch (Exception e)
        {
            LOGGER.error("Debug start failed", e);
            String capturedOutput = nextSession == null ? null : nextSession.snapshotCapturedOutput();
            ProcessorSupport processorSupport = nextSession == null ? null : nextSession.processorSupport();
            if (nextSession != null)
            {
                clearIfTerminal(nextSession, LegendDebug.Response.completed(null));
                nextSession.stop();
            }
            return LegendDebug.Response.error(ExecutionFailureFormatter.format(e, capturedOutput, processorSupport));
        }
    }

    /**
     * Pushes a fresh full breakpoint list into the active session, if any, so add/remove/condition-edit
     * requests that arrive after {@link #start} has already begun running take effect immediately
     * instead of only being picked up by a future session. A no-op before a session exists - the initial
     * breakpoint list is already correctly picked up by {@link #start}.
     */
    public void updateBreakpoints(List<LegendDebug.Breakpoint> breakpoints)
    {
        LegendDebugSession active = this.debugSession;
        if (active != null)
        {
            active.updateBreakpoints(breakpoints == null ? Collections.emptyList() : breakpoints);
        }
    }

    public LegendDebug.Response continueExecution()
    {
        LegendDebugSession active = this.debugSession;
        if (active == null)
        {
            return LegendDebug.Response.error("No active debug session");
        }
        LegendDebug.Response response = active.continueExecution();
        clearIfTerminal(active, response);
        return response;
    }

    public LegendDebug.Response stepIn()
    {
        LegendDebugSession active = this.debugSession;
        if (active == null)
        {
            return LegendDebug.Response.error("No active debug session");
        }
        LegendDebug.Response response = active.stepIn();
        clearIfTerminal(active, response);
        return response;
    }

    public LegendDebug.Response stepOver()
    {
        LegendDebugSession active = this.debugSession;
        if (active == null)
        {
            return LegendDebug.Response.error("No active debug session");
        }
        LegendDebug.Response response = active.stepOver();
        clearIfTerminal(active, response);
        return response;
    }

    public LegendDebug.Response stepOut()
    {
        LegendDebugSession active = this.debugSession;
        if (active == null)
        {
            return LegendDebug.Response.error("No active debug session");
        }
        LegendDebug.Response response = active.stepOut();
        clearIfTerminal(active, response);
        return response;
    }

    public LegendDebug.EvaluateResult evaluate(LegendDebug.EvaluateParams params)
    {
        LegendDebugSession active = this.debugSession;
        if (active == null || !active.isPaused())
        {
            return LegendDebug.EvaluateResult.error("Debug execution is not paused");
        }
        return active.evaluate(params == null ? "" : params.getExpression(), params == null ? 0 : params.getFrameId());
    }

    public List<LegendDebug.Variable> variables(LegendDebug.VariablesParams params)
    {
        LegendDebugSession active = this.debugSession;
        return active == null ? Collections.emptyList() : active.variables(params == null ? 1 : params.getVariablesReference());
    }

    public LegendDebug.Response stop()
    {
        LegendDebugSession active = clearActiveSession();
        return active == null ? LegendDebug.Response.completed(null) : active.stop();
    }

    public void shutdown()
    {
        stopActiveSession();
    }

    private synchronized boolean setActiveSession(int generation, LegendDebugSession session)
    {
        if (this.sessionGeneration != generation)
        {
            return false;
        }
        this.debugSession = session;
        return true;
    }

    private synchronized LegendDebugSession clearActiveSession()
    {
        this.sessionGeneration++;
        LegendDebugSession active = this.debugSession;
        this.debugSession = null;
        return active;
    }

    private synchronized void clearIfTerminal(LegendDebugSession session, LegendDebug.Response response)
    {
        if (this.debugSession == session && response != null && !"paused".equals(response.getState()))
        {
            this.debugSession = null;
        }
    }

    private void stopActiveSession()
    {
        stopSession(clearActiveSession());
    }

    private void stopSession(LegendDebugSession session)
    {
        if (session != null)
        {
            session.stop();
        }
    }
}
