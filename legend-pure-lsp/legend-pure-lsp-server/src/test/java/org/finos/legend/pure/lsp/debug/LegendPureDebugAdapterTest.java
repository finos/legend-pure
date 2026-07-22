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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.junit.Assert;
import org.junit.Test;

public class LegendPureDebugAdapterTest
{
    @Test(timeout = 10_000)
    public void dapInitializeSendsInitializedEventAsynchronouslyAfterInitializeResponse() throws Exception
    {
        RecordingDebugService service = new RecordingDebugService();
        LegendPureDebugAdapter adapter = new LegendPureDebugAdapter(service);
        RecordingDebugClient client = new RecordingDebugClient();
        adapter.connect(client);
        try
        {
            CompletableFuture<Capabilities> response = adapter.initialize(new InitializeRequestArguments());
            Assert.assertTrue(response.isDone());
            Assert.assertFalse(client.initialized.get());
            waitUntil(client.initialized::get);
        }
        finally
        {
            adapter.disconnect(null).get();
        }
    }

    @Test(timeout = 10_000)
    public void dapAdapterUsesServerDebugServiceAndFrameScopedRequests() throws Exception
    {
        RecordingDebugService service = new RecordingDebugService();
        LegendPureDebugAdapter adapter = new LegendPureDebugAdapter(service);
        try
        {
            Capabilities capabilities = adapter.initialize(new InitializeRequestArguments()).get();
            Assert.assertEquals(Boolean.TRUE, capabilities.getSupportsConfigurationDoneRequest());
            Assert.assertEquals(Boolean.TRUE, capabilities.getSupportsEvaluateForHovers());
            Assert.assertEquals(Boolean.TRUE, capabilities.getSupportsTerminateRequest());
            Assert.assertEquals(Boolean.FALSE, capabilities.getSupportsConditionalBreakpoints());

            Source source = new Source();
            source.setPath("/workspace/debug_dap.pure");
            SourceBreakpoint breakpoint = new SourceBreakpoint();
            breakpoint.setLine(7);
            SetBreakpointsArguments breakpointsArgs = new SetBreakpointsArguments();
            breakpointsArgs.setSource(source);
            breakpointsArgs.setBreakpoints(new SourceBreakpoint[] {breakpoint});
            SetBreakpointsResponse breakpointsResponse = adapter.setBreakpoints(breakpointsArgs).get();

            Breakpoint[] breakpoints = breakpointsResponse.getBreakpoints();
            Assert.assertEquals(1, breakpoints.length);
            Assert.assertTrue(breakpoints[0].isVerified());
            Assert.assertEquals(Integer.valueOf(7), breakpoints[0].getLine());

            Map<String, Object> launchArgs = new HashMap<>();
            launchArgs.put("function", "demo::go():Any[*]");
            adapter.launch(launchArgs).get();
            adapter.configurationDone(new ConfigurationDoneArguments()).get();

            StackTraceResponse stackTrace = waitForStackTrace(adapter);
            Assert.assertEquals(2, stackTrace.getStackFrames().length);
            Assert.assertEquals("demo::inner()", stackTrace.getStackFrames()[0].getName());
            Assert.assertEquals("debug_dap.pure", stackTrace.getStackFrames()[0].getSource().getName());
            Assert.assertEquals(10, stackTrace.getStackFrames()[0].getLine());

            Assert.assertNotNull(service.startParams);
            Assert.assertEquals("demo::go():Any[*]", service.startParams.getFunction());
            Assert.assertEquals(1, service.startParams.getBreakpoints().size());
            Assert.assertEquals(6, service.startParams.getBreakpoints().get(0).getLine());

            ScopesArguments scopesArgs = new ScopesArguments();
            scopesArgs.setFrameId(2);
            ScopesResponse scopesResponse = adapter.scopes(scopesArgs).get();
            Scope[] scopes = scopesResponse.getScopes();
            Assert.assertEquals(1, scopes.length);
            Assert.assertEquals(22, scopes[0].getVariablesReference());

            VariablesArguments variablesArgs = new VariablesArguments();
            variablesArgs.setVariablesReference(22);
            VariablesResponse variablesResponse = adapter.variables(variablesArgs).get();
            Variable[] variables = variablesResponse.getVariables();
            Assert.assertEquals(1, variables.length);
            Assert.assertEquals("outer", variables[0].getName());
            Assert.assertEquals(22, service.variablesParams.getVariablesReference());

            EvaluateArguments evaluateArgs = new EvaluateArguments();
            evaluateArgs.setFrameId(2);
            evaluateArgs.setExpression("$outer");
            EvaluateResponse evaluateResponse = adapter.evaluate(evaluateArgs).get();
            Assert.assertEquals("outer value", evaluateResponse.getResult());
            Assert.assertEquals(2, service.evaluateParams.getFrameId());
            Assert.assertEquals("$outer", service.evaluateParams.getExpression());
        }
        finally
        {
            adapter.disconnect(null).get();
        }
    }

    @Test(timeout = 10_000)
    public void dapAdapterHonorsClientZeroBasedLineAndColumnConvention() throws Exception
    {
        RecordingDebugService service = new RecordingDebugService();
        LegendPureDebugAdapter adapter = new LegendPureDebugAdapter(service);
        try
        {
            InitializeRequestArguments initializeArgs = new InitializeRequestArguments();
            initializeArgs.setLinesStartAt1(Boolean.FALSE);
            initializeArgs.setColumnsStartAt1(Boolean.FALSE);
            adapter.initialize(initializeArgs).get();

            Map<String, Object> launchArgs = new HashMap<>();
            launchArgs.put("function", "demo::go():Any[*]");
            adapter.launch(launchArgs).get();
            adapter.configurationDone(new ConfigurationDoneArguments()).get();

            StackTraceResponse stackTrace = waitForStackTrace(adapter);
            Assert.assertEquals(9, stackTrace.getStackFrames()[0].getLine());
            Assert.assertEquals(4, stackTrace.getStackFrames()[0].getColumn());
            Assert.assertEquals(Integer.valueOf(9), stackTrace.getStackFrames()[0].getEndLine());
            Assert.assertEquals(Integer.valueOf(14), stackTrace.getStackFrames()[0].getEndColumn());
        }
        finally
        {
            adapter.disconnect(null).get();
        }
    }

    @Test(timeout = 10_000)
    public void dapAdapterWaitsForBreakpointConfigurationBeforeStarting() throws Exception
    {
        RecordingDebugService service = new RecordingDebugService();
        LegendPureDebugAdapter adapter = new LegendPureDebugAdapter(service);
        try
        {
            adapter.initialize(new InitializeRequestArguments()).get();

            Map<String, Object> launchArgs = new HashMap<>();
            launchArgs.put("function", "demo::go():Any[*]");
            adapter.launch(launchArgs).get();
            adapter.configurationDone(new ConfigurationDoneArguments()).get();

            Assert.assertNull(service.startParams);

            Source source = new Source();
            source.setPath("/workspace/debug_delayed_breakpoints.pure");
            SourceBreakpoint breakpoint = new SourceBreakpoint();
            breakpoint.setLine(3);
            SetBreakpointsArguments breakpointsArgs = new SetBreakpointsArguments();
            breakpointsArgs.setSource(source);
            breakpointsArgs.setBreakpoints(new SourceBreakpoint[] {breakpoint});
            adapter.setBreakpoints(breakpointsArgs).get();

            waitUntil(() -> service.startParams != null);
            Assert.assertEquals(1, service.startParams.getBreakpoints().size());
            Assert.assertEquals(2, service.startParams.getBreakpoints().get(0).getLine());
        }
        finally
        {
            adapter.disconnect(null).get();
        }
    }

    private static StackTraceResponse waitForStackTrace(LegendPureDebugAdapter adapter) throws Exception
    {
        StackTraceArguments args = new StackTraceArguments();
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline)
        {
            StackTraceResponse response = adapter.stackTrace(args).get();
            if (response.getStackFrames().length > 0)
            {
                return response;
            }
            Thread.sleep(10);
        }
        Assert.fail("DAP adapter did not publish stack frames after launch");
        return null;
    }

    private static void waitUntil(Check check) throws Exception
    {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline)
        {
            if (check.get())
            {
                return;
            }
            Thread.sleep(10);
        }
        Assert.fail("Condition was not satisfied");
    }

    private interface Check
    {
        boolean get();
    }

    private static class RecordingDebugClient implements IDebugProtocolClient
    {
        private final AtomicBoolean initialized = new AtomicBoolean();

        @Override
        public void initialized()
        {
            this.initialized.set(true);
        }
    }

    private static class RecordingDebugService extends DebugService
    {
        private volatile LegendDebug.StartParams startParams;
        private volatile LegendDebug.EvaluateParams evaluateParams;
        private volatile LegendDebug.VariablesParams variablesParams;

        private RecordingDebugService()
        {
            super(null, null, null, Collections::emptyMap);
        }

        @Override
        public LegendDebug.Response start(LegendDebug.StartParams params)
        {
            this.startParams = params;
            List<LegendDebug.StackFrame> frames = Arrays.asList(
                    new LegendDebug.StackFrame(1, "demo::inner()", "file:///workspace/debug_dap.pure", 10, 5, 10, 14, 11),
                    new LegendDebug.StackFrame(2, "demo::go()", "file:///workspace/debug_dap.pure", 7, 3, 7, 12, 22));
            return LegendDebug.Response.paused(null, frames, "breakpoint");
        }

        @Override
        public List<LegendDebug.Variable> variables(LegendDebug.VariablesParams params)
        {
            this.variablesParams = params;
            return Collections.singletonList(new LegendDebug.Variable("outer", "outer value", "String"));
        }

        @Override
        public LegendDebug.EvaluateResult evaluate(LegendDebug.EvaluateParams params)
        {
            this.evaluateParams = params;
            return LegendDebug.EvaluateResult.success("outer value");
        }

        @Override
        public LegendDebug.Response stop()
        {
            return LegendDebug.Response.completed(null);
        }
    }
}
