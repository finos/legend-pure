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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LegendDebugSessionTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(timeout = 60_000)
    public void debugRuntimeStopsAtBreakpointExposesVariablesEvaluatesAndContinues()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_breakpoint_eval_go.pure";
        String uri = "file:///workspace/debug_breakpoint_eval_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'hello';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));

        LegendDebug.EvaluateResult evaluated = debug.evaluate("$x");
        Assert.assertTrue("Evaluate should succeed: " + evaluated.getError(), evaluated.isSuccess());

        LegendDebug.Response completed = debug.continueExecution();
        Assert.assertTrue(completed.isSuccess());
        Assert.assertEquals("completed", completed.getState());
    }

    @Test(timeout = 60_000)
    public void redDotBreakpointUsesUnchangedDebugCopyAndRealSourceLine()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_breakpoint_go.pure";
        String uri = "file:///workspace/debug_breakpoint_go.pure";
        String code =
                "function helper():Any[*]\n" +
                        "{\n" +
                        "  let x = 'red';\n" +
                        "  print($x, 1);\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  helper();\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);

        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 3)));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertEquals("Breakpoint line should use the original source line",
                4, paused.getStackFrames().get(0).getLine());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));
        Assert.assertEquals("Main runtime source must not be modified by debugger startup",
                code, session.getPureRuntime().getSourceById(sourceId).getContent());
        Assert.assertEquals("Debug runtime source must not be instrumented",
                code, debug.debugSourceContent(sourceId));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void debugRuntimeUsesUnsavedOpenDocumentSnapshot()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_unsaved_go.pure";
        String uri = "file:///workspace/debug_unsaved_go.pure";
        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);

        String unsavedCode =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'unsaved';\n" +
                        "  $x;\n" +
                        "}\n";
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.singletonMap(sourceId, unsavedCode),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(unsavedCode, "  $x;"))));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));
        Assert.assertNull("Unsaved debug source should not be added to the main runtime",
                session.getPureRuntime().getSourceById(sourceId));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void debugRuntimeDoesNotInstrumentClasspathRepositorySources()
    {
        String classpathSourceId = "/platform/pure/essential/tests/fail.pure";
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        Assert.assertNotNull(session.getPureRuntime().getSourceById(classpathSourceId));
        String classpathSourceContent = session.getPureRuntime().getSourceById(classpathSourceId).getContent();
        String sourceId = "debug_with_classpath_dependencies_go.pure";
        String uri = "file:///workspace/debug_with_classpath_dependencies_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'classpath';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue("Debug start should not reparse instrumented classpath sources: " + paused.getMessage(), paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));
        Assert.assertEquals("Classpath source content must remain unchanged in the main runtime",
                classpathSourceContent,
                session.getPureRuntime().getSourceById(classpathSourceId).getContent());
        Assert.assertEquals("Classpath source content must remain unchanged in the debug runtime",
                classpathSourceContent,
                debug.debugSourceContent(classpathSourceId));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void debugRuntimeDoesNotInstrumentConfiguredDependencyReposFoundInWorkspace() throws IOException
    {
        Path resourcesDir = this.tempFolder.getRoot().toPath().resolve("configured-dependency/src/main/resources");
        Path repoDir = resourcesDir.resolve("debug_dependency_repo/debugdep");
        Files.createDirectories(repoDir);
        Files.write(resourcesDir.resolve("debug_dependency_repo.definition.json"),
                ("{\"name\":\"debug_dependency_repo\","
                        + "\"pattern\":\"(debugdep)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));
        String dependencyCode =
                "function debugdep::dependency():Any[*]\n" +
                        "{\n" +
                        "  print(\n" +
                        "    'dependency',\n" +
                        "    1);\n" +
                        "}\n";
        Files.write(repoDir.resolve("dependency.pure"), dependencyCode.getBytes(StandardCharsets.UTF_8));

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(Collections.singletonList(this.tempFolder.getRoot().toPath()));
        LegendPureSession session = new LegendPureSession();
        session.initialize(scanner, Collections.singleton("debug_dependency_repo"));
        String sourceId = "debug_configured_dependency_go.pure";
        String uri = "file:///workspace/debug_configured_dependency_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'configured dependency';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                scanner,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue("Configured dependency repo sources must not be instrumented: " + paused.getMessage(), paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void debugRuntimeOverlaysWorkspaceRepositorySources() throws IOException
    {
        Path resourcesDir = this.tempFolder.getRoot().toPath().resolve("module/src/main/resources");
        Path repoDir = resourcesDir.resolve("core_relational_memsql/debug");
        Files.createDirectories(repoDir);
        Files.write(resourcesDir.resolve("core_relational_memsql.definition.json"),
                ("{\"name\":\"core_relational_memsql\","
                        + "\"pattern\":\"(debug)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));

        String sourceId = "/core_relational_memsql/debug/go.pure";
        Path sourceFile = repoDir.resolve("go.pure");
        String code =
                "function debug::go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'repo';\n" +
                        "  print($x, 1);\n" +
                        "}\n";
        Files.write(sourceFile, code.getBytes(StandardCharsets.UTF_8));

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(Collections.singletonList(this.tempFolder.getRoot().toPath()));
        LegendPureSession session = new LegendPureSession();
        session.initialize(scanner);
        Assert.assertNotNull(session.getPureRuntime().getSourceById(sourceId));

        UriMapper uriMapper = new UriMapper();
        String uri = sourceFile.toUri().toString();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                scanner,
                uriMapper,
                Collections.emptyMap(),
                "debug::go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 3)));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertEquals(4, paused.getStackFrames().get(0).getLine());
        Assert.assertEquals(code, session.getPureRuntime().getSourceById(sourceId).getContent());
        Assert.assertEquals(code, debug.debugSourceContent(sourceId));
        Assert.assertEquals(code, new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void mainSessionStillWorksWhileDebugExecutionIsPaused()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_paused_go.pure";
        String uri = "file:///workspace/debug_paused_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'done';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));
        LegendDebug.Response paused = debug.start();
        Assert.assertEquals("paused", paused.getState());

        LegendPureSession.CompileResult compile = session.modifyAndCompile(
                "debug_main_still_live.pure",
                "Class test::debug::StillLive\n{\n  name: String[1];\n}\n");
        Assert.assertTrue("Main session should compile while debug runtime is paused: " + errorMessage(compile), compile.isSuccess());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void sharedModeAttachesToMainRuntimeAndDebugsAlreadyCompiledFunction()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_shared_go.pure";
        String uri = "file:///workspace/debug_shared_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'shared';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.createShared(
                session,
                null,
                uriMapper,
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertTrue(debug.variables().stream().anyMatch(variable -> "x".equals(variable.getName())));

        LegendDebug.Response completed = debug.continueExecution();
        Assert.assertTrue(completed.isSuccess());
        Assert.assertEquals("completed", completed.getState());
    }

    @Test(timeout = 60_000)
    public void sharedModeBlocksMainSessionCompileWhilePausedAndReleasesOnStop() throws Exception
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_shared_paused_go.pure";
        String uri = "file:///workspace/debug_shared_paused_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'done';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.createShared(
                session,
                null,
                uriMapper,
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, "  $x;"))));
        LegendDebug.Response paused = debug.start();
        Assert.assertEquals("paused", paused.getState());

        java.util.concurrent.CountDownLatch compileFinished = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<LegendPureSession.CompileResult> compileResult = new java.util.concurrent.atomic.AtomicReference<>();
        Thread compileThread = new Thread(() ->
        {
            compileResult.set(session.modifyAndCompile(
                    "debug_shared_main_blocked.pure",
                    "Class test::debug::SharedBlocked\n{\n  name: String[1];\n}\n"));
            compileFinished.countDown();
        });
        compileThread.start();
        try
        {
            Assert.assertFalse("Main session compile should be blocked while a SHARED debug session is paused",
                    compileFinished.await(500, java.util.concurrent.TimeUnit.MILLISECONDS));

            debug.stop();

            Assert.assertTrue("Main session compile should complete once the SHARED debug session releases the graph lock",
                    compileFinished.await(30, java.util.concurrent.TimeUnit.SECONDS));
            Assert.assertTrue("Expected compile success: " + errorMessage(compileResult.get()), compileResult.get().isSuccess());
        }
        finally
        {
            compileThread.join(30_000);
        }
    }

    @Test(timeout = 60_000)
    public void stepOverStaysInTheCurrentFunction()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_step_over_go.pure";
        String uri = "file:///workspace/debug_step_over_go.pure";
        assertCompiled(session.modifyAndCompile(sourceId, steppingCode()));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 8)));

        Assert.assertEquals(9, debug.start().getStackFrames().get(0).getLine());
        LegendDebug.Response stepped = debug.stepOver();
        Assert.assertEquals("step", stepped.getReason());
        Assert.assertEquals(10, stepped.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void stepInStopsInsideNestedPureFunction()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_step_in_go.pure";
        String uri = "file:///workspace/debug_step_in_go.pure";
        assertCompiled(session.modifyAndCompile(sourceId, steppingCode()));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 8)));

        Assert.assertEquals(9, debug.start().getStackFrames().get(0).getLine());
        LegendDebug.Response stepped = debug.stepIn();
        Assert.assertEquals("step", stepped.getReason());
        Assert.assertEquals(3, stepped.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void stepInEntersLambdaBodyPassedToNativeHigherOrderFunction()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_step_in_lambda_go.pure";
        String uri = "file:///workspace/debug_step_in_lambda_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = [1, 2];\n" +
                        "  let y = $x->map(i |\n" +
                        "    print('mapping', 1);\n" +
                        "    $i + 1;);\n" +
                        "  print($y, 1);\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 3)));

        Assert.assertEquals("let y = ...", 4, debug.start().getStackFrames().get(0).getLine());

        LegendDebug.Response steppedIntoLambda = debug.stepIn();
        Assert.assertEquals("step", steppedIntoLambda.getReason());
        Assert.assertEquals("step into map's lambda should stop on its first statement",
                5, steppedIntoLambda.getStackFrames().get(0).getLine());

        LegendDebug.Response steppedToSecondStatement = debug.stepIn();
        Assert.assertEquals("step", steppedToSecondStatement.getReason());
        Assert.assertEquals("stepping again should advance to the lambda's second statement",
                6, steppedToSecondStatement.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void stepInEntersEachBranchLambdaOfIfAndFollowsLetStatements()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_step_in_if_lambda_go.pure";
        String uri = "file:///workspace/debug_step_in_if_lambda_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let flag = true;\n" +
                        "  if($flag,\n" +
                        "     | let a = 1;\n" +
                        "       let b = $a + 1;\n" +
                        "       print($b, 1);,\n" +
                        "     | print('false', 1););\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 3)));

        Assert.assertEquals(4, debug.start().getStackFrames().get(0).getLine());

        LegendDebug.Response first = debug.stepIn();
        Assert.assertEquals("step into the true-branch lambda should stop on its first (let) statement",
                5, first.getStackFrames().get(0).getLine());

        LegendDebug.Response second = debug.stepIn();
        Assert.assertEquals("stepping should follow subsequent let statements inside the lambda",
                6, second.getStackFrames().get(0).getLine());

        LegendDebug.Response third = debug.stepIn();
        Assert.assertEquals("stepping should reach the lambda's final statement",
                7, third.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void stepOutFromBreakpointStepsPastTheBreakpointExpression()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_step_out_from_breakpoint_go.pure";
        String uri = "file:///workspace/debug_step_out_from_breakpoint_go.pure";
        String code =
                "function failLater():Any[*]\n" +
                        "{\n" +
                        "  fail('stepOut should stop before this executes');\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'start';\n" +
                        "  print($x, 1);\n" +
                        "  failLater();\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, 7)));

        Assert.assertEquals(8, debug.start().getStackFrames().get(0).getLine());
        LegendDebug.Response stepped = debug.stepOut();
        Assert.assertTrue("stepOut from a breakpoint should not run the next failing statement: " + stepped.getMessage(), stepped.isSuccess());
        Assert.assertEquals("step", stepped.getReason());
        Assert.assertEquals(9, stepped.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void breakpointsStopOnCommonFunctionExecutionStatementBoundaries()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_common_statement_breakpoints_go.pure";
        String uri = "file:///workspace/debug_common_statement_breakpoints_go.pure";
        String code =
                "function helper():String[1]\n" +
                        "{\n" +
                        "  'helper';\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = helper();\n" +
                        "  print($x, 1);\n" +
                        "  'abc'->toString();\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        assertBreakpointLine(session, sourceId, uri, 6, 7);
        assertBreakpointLine(session, sourceId, uri, 7, 8);
        assertBreakpointLine(session, sourceId, uri, 8, 9);
    }

    @Test(timeout = 60_000)
    public void evaluateAcceptsImplicitLocalReferencesAndFormatsResults()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_implicit_local_eval.pure";
        String uri = "file:///workspace/debug_implicit_local_eval.pure";
        String code = debugLocalsCode();
        assertCompiled(session.modifyAndCompile(sourceId, code));

        LegendDebugSession debug = debugAtBreakpoint(session, sourceId, uri, code, "  $name;");

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());

        LegendDebug.EvaluateResult implicit = debug.evaluate("routedFunction");
        Assert.assertTrue("Implicit local evaluate should succeed: " + implicit.getError(), implicit.isSuccess());
        Assert.assertTrue(implicit.getResult(), implicit.getResult().contains("test::debug::routed():String[1]"));
        Assert.assertTrue("Function evaluate result should be expandable", implicit.getVariablesReference() > 0);

        LegendDebug.EvaluateResult explicit = debug.evaluate("$routedFunction");
        Assert.assertTrue("Explicit local evaluate should still succeed: " + explicit.getError(), explicit.isSuccess());
        Assert.assertEquals(implicit.getResult(), explicit.getResult());

        LegendDebug.EvaluateResult property = debug.evaluate("routedFunction.expressionSequence");
        Assert.assertTrue("Property evaluate should succeed: " + property.getError(), property.isSuccess());
        Assert.assertFalse("Formatted value should hide raw anonymous ids: " + property.getResult(),
                property.getResult().contains("@_"));

        LegendDebug.EvaluateResult pipeline = debug.evaluate("numbers->size()");
        Assert.assertTrue("Pipeline local evaluate should succeed: " + pipeline.getError(), pipeline.isSuccess());
        Assert.assertEquals("2", pipeline.getResult());

        LegendDebug.EvaluateResult functionCall = debug.evaluate("test::debug::fullName($person)");
        Assert.assertTrue("Function call on local should succeed: " + functionCall.getError(), functionCall.isSuccess());
        Assert.assertEquals("Ada Lovelace", functionCall.getResult());

        LegendDebug.EvaluateResult sourceImport = debug.evaluate("sourceImportedName($person)");
        Assert.assertTrue("Evaluate should inherit paused source imports: " + sourceImport.getError(), sourceImport.isSuccess());
        Assert.assertEquals("Ada", sourceImport.getResult());

        LegendDebug.EvaluateResult importOnly = debug.evaluate("import test::debug::console::*;");
        Assert.assertTrue("Import-only evaluate should succeed: " + importOnly.getError(), importOnly.isSuccess());
        Assert.assertEquals("Imported test::debug::console::*", importOnly.getResult());

        LegendDebug.EvaluateResult sessionImport = debug.evaluate("consoleImportedName($person)");
        Assert.assertTrue("Evaluate should reuse Debug Console imports: " + sessionImport.getError(), sessionImport.isSuccess());
        Assert.assertEquals("Lovelace", sessionImport.getResult());

        LegendDebug.EvaluateResult inlineImport = debug.evaluate("import test::debug::inline::*\ninlineImportedName($person)");
        Assert.assertTrue("Evaluate should accept leading import lines: " + inlineImport.getError(), inlineImport.isSuccess());
        Assert.assertEquals("Ada Lovelace", inlineImport.getResult());

        LegendDebug.EvaluateResult invalidImport = debug.evaluate("import test::debug::console::* nope");
        Assert.assertFalse(invalidImport.isSuccess());
        Assert.assertTrue(invalidImport.getError(), invalidImport.getError().contains("Invalid import"));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void variablesPanelShowsReadableExpandableValues()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_readable_locals.pure";
        String uri = "file:///workspace/debug_readable_locals.pure";
        String code = debugLocalsCode();
        assertCompiled(session.modifyAndCompile(sourceId, code));

        LegendDebugSession debug = debugAtBreakpoint(session, sourceId, uri, code, "  $name;");

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());

        List<LegendDebug.Variable> locals = debug.variables(1);
        Assert.assertEquals("Ada", variable(locals, "name").getValue());
        Assert.assertEquals("42", variable(locals, "answer").getValue());
        Assert.assertTrue(variable(locals, "numbers").getValue(), variable(locals, "numbers").getValue().contains("[2]"));
        Assert.assertTrue(variable(locals, "numbers").getVariablesReference() > 0);
        Assert.assertTrue(variable(locals, "person").getValue(), variable(locals, "person").getValue().contains("test::debug::Person"));
        Assert.assertFalse(variable(locals, "person").getValue(), variable(locals, "person").getValue().contains("@_"));
        Assert.assertTrue(variable(locals, "routedFunction").getValue(),
                variable(locals, "routedFunction").getValue().contains("test::debug::routed():String[1]"));
        Assert.assertTrue(variable(locals, "mapping").getValue(),
                variable(locals, "mapping").getValue().contains("test::debug::DebugMapping"));
        Assert.assertTrue(variable(locals, "runtime").getValue(),
                variable(locals, "runtime").getValue().contains("test::debug::DebugRuntime"));
        Assert.assertFalse(variable(locals, "runtime").getValue(),
                variable(locals, "runtime").getValue().contains("@_"));

        int numbersReference = variable(locals, "numbers").getVariablesReference();
        List<LegendDebug.Variable> firstExpansion = debug.variables(numbersReference);
        List<LegendDebug.Variable> secondExpansion = debug.variables(numbersReference);
        Assert.assertEquals(2, firstExpansion.size());
        Assert.assertEquals("[0]", firstExpansion.get(0).getName());
        Assert.assertEquals("1", firstExpansion.get(0).getValue());
        Assert.assertEquals("Child references should be stable for repeated expansion",
                firstExpansion.get(0).getVariablesReference(),
                secondExpansion.get(0).getVariablesReference());

        int personReference = variable(locals, "person").getVariablesReference();
        List<LegendDebug.Variable> personChildren = debug.variables(personReference);
        Assert.assertEquals("Ada", variable(personChildren, "firstName").getValue());
        Assert.assertEquals("Lovelace", variable(personChildren, "lastName").getValue());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void variablesPanelSupportsMultiLevelNestedExpansion()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_multi_level_nested_locals.pure";
        String uri = "file:///workspace/debug_multi_level_nested_locals.pure";
        String code =
                "###Pure\n" +
                        "Class test::debug::nested::Address\n" +
                        "{\n" +
                        "  city: String[1];\n" +
                        "}\n" +
                        "Class test::debug::nested::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  address: test::debug::nested::Address[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  let address = ^test::debug::nested::Address(city='London');\n" +
                        "  let person = ^test::debug::nested::Person(name='Ada', address=$address);\n" +
                        "  $person;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        LegendDebugSession debug = debugAtBreakpoint(session, sourceId, uri, code, "  $person;");

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue(paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());

        List<LegendDebug.Variable> locals = debug.variables(1);
        LegendDebug.Variable person = variable(locals, "person");
        Assert.assertTrue("A Person instance should be expandable", person.getVariablesReference() > 0);

        List<LegendDebug.Variable> personChildren = debug.variables(person.getVariablesReference());
        Assert.assertEquals("Ada", variable(personChildren, "name").getValue());
        LegendDebug.Variable address = variable(personChildren, "address");
        Assert.assertTrue("A nested Address instance should itself be expandable, not flattened to a leaf",
                address.getVariablesReference() > 0);

        List<LegendDebug.Variable> addressChildren = debug.variables(address.getVariablesReference());
        Assert.assertEquals("Real grandchild data should be reachable, not just the top level",
                "London", variable(addressChildren, "city").getValue());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void localsAppearOnlyAfterAssignmentHasExecuted()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_assignment_timing.pure";
        String uri = "file:///workspace/debug_assignment_timing.pure";
        String code =
                "function makeClusters():String[*]\n" +
                        "{\n" +
                        "  ['a', 'b'];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  let clusters = makeClusters();\n" +
                        "  print($clusters->size()->toString(), 1);\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        LegendDebugSession debug = debugAtBreakpoint(session, sourceId, uri, code, "  let clusters = makeClusters();");

        Assert.assertEquals("paused", debug.start().getState());
        Assert.assertNull(variableOrNull(debug.variables(), "clusters"));
        LegendDebug.EvaluateResult beforeAssignment = debug.evaluate("$clusters");
        Assert.assertFalse(beforeAssignment.isSuccess());
        Assert.assertTrue(beforeAssignment.getError(), beforeAssignment.getError().contains("`clusters` is not in scope yet"));
        Assert.assertTrue(beforeAssignment.getError(), beforeAssignment.getError().contains("available locals"));

        LegendDebug.Response afterAssignment = debug.stepOver();
        Assert.assertTrue(afterAssignment.isSuccess());
        Assert.assertEquals("paused", afterAssignment.getState());
        Assert.assertNotNull("clusters should be visible after stepping past its assignment",
                variableOrNull(debug.variables(), "clusters"));
        LegendDebug.EvaluateResult size = debug.evaluate("clusters->size()");
        Assert.assertTrue("clusters->size() should evaluate after assignment: " + size.getError(), size.isSuccess());
        Assert.assertEquals("2", size.getResult());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void breakpointOnVariableOnlyExpressionRequiresPureValueSpecificationHook()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_variable_only_breakpoint_go.pure";
        String uri = "file:///workspace/debug_variable_only_breakpoint_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  let x = 'value';\n" +
                        "  $x;\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        assertBreakpointLine(session, sourceId, uri, 3, 4);
    }

    @Test(timeout = 60_000)
    public void updatingBreakpointsToRemoveMidSessionStopsFurtherPausesOnThatLine()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_update_breakpoints_remove_go.pure";
        String uri = "file:///workspace/debug_update_breakpoints_remove_go.pure";
        String code = mapPrintCode(new int[] {1, 2});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine)));

        LegendDebug.Response firstPause = debug.start();
        Assert.assertEquals("paused", firstPause.getState());
        Assert.assertEquals("breakpoint", firstPause.getReason());
        Assert.assertEquals("1", variable(debug.variables(), "n").getValue());

        debug.updateBreakpoints(Collections.emptyList());

        LegendDebug.Response completed = debug.continueExecution();
        Assert.assertTrue(completed.isSuccess());
        Assert.assertEquals("Second iteration should not re-pause once the breakpoint was removed mid-session",
                "completed", completed.getState());
    }

    @Test(timeout = 60_000)
    public void updatingBreakpointsToAddMidSessionPausesOnTheNewLine()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_update_breakpoints_add_go.pure";
        String uri = "file:///workspace/debug_update_breakpoints_add_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  print('start', 1);\n" +
                        "  let numbers = [1, 2];\n" +
                        "  $numbers->map(n |\n" +
                        "    print($n, 1);\n" +
                        "    $n;\n" +
                        "  );\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int startBreakpointLine = zeroBasedLine(code, "  print('start', 1);");
        int loopBreakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, startBreakpointLine)));

        LegendDebug.Response firstPause = debug.start();
        Assert.assertEquals("paused", firstPause.getState());
        Assert.assertEquals(startBreakpointLine + 1, firstPause.getStackFrames().get(0).getLine());

        debug.updateBreakpoints(Arrays.asList(
                new LegendDebug.Breakpoint(uri, startBreakpointLine),
                new LegendDebug.Breakpoint(uri, loopBreakpointLine)));

        LegendDebug.Response secondPause = debug.continueExecution();
        Assert.assertEquals("Newly-added breakpoint should pause once execution reaches it",
                "paused", secondPause.getState());
        Assert.assertEquals("breakpoint", secondPause.getReason());
        Assert.assertEquals(loopBreakpointLine + 1, secondPause.getStackFrames().get(0).getLine());

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointOnlyPausesWhenConditionIsTrue()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_go.pure";
        String code = mapPrintCode(new int[] {1, 2, 3});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$n == 2")));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue("Expected a conditional pause: " + paused.getMessage(), paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertEquals("2", variable(debug.variables(), "n").getValue());

        LegendDebug.Response completed = debug.continueExecution();
        Assert.assertTrue(completed.isSuccess());
        Assert.assertEquals("Only the iteration matching the condition should pause",
                "completed", completed.getState());
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointThatIsAlwaysFalseNeverPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_always_false_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_always_false_go.pure";
        String code = mapPrintCode(new int[] {1, 2});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "false")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A breakpoint with condition `false` should never pause, on either iteration",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointOnNestedCallLineThatIsAlwaysFalseNeverPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_nested_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_nested_go.pure";
        // Two FunctionExpression nodes on one line (toString, then print) - per Phase 1, each
        // nested call is its own pause-checkpoint, so this line yields 2 condition evaluations
        // per loop iteration, not 1.
        String code = "function go():Any[*]\n" +
                "{\n" +
                "  let numbers = [1, 2];\n" +
                "  $numbers->map(n |\n" +
                "    print($n->toString(), 1);\n" +
                "  );\n" +
                "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n->toString(), 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "false")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A breakpoint with condition `false` on a line with nested calls should never pause",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointWithVariableConditionNeverTrueNeverPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_never_true_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_never_true_go.pure";
        String code = mapPrintCode(new int[] {1, 2, 3, 4, 5});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$n == 99")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A never-true condition must not pause on any of the 5 iterations",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointInFunctionCalledTwiceNeverPausesWhenFalse()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_two_calls_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_two_calls_go.pure";
        // A line hit twice via two separate calls (two distinct frames), rather than a loop.
        String code = "function helper(x:Integer[1]):Any[*]\n" +
                "{\n" +
                "  print($x, 1);\n" +
                "}\n" +
                "\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  helper(1);\n" +
                "  helper(2);\n" +
                "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "  print($x, 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$x == 99")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A never-true condition must not pause on either call",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void sharedModeConditionalBreakpointThatIsAlwaysFalseNeverPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_shared_conditional_false_go.pure";
        String uri = "file:///workspace/debug_shared_conditional_false_go.pure";
        String code = mapPrintCode(new int[] {1, 2});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        // SHARED is what the IntelliJ client actually launches with, and unlike FORKED it evaluates
        // conditions against the live main runtime while holding its graph read lock.
        LegendDebugSession debug = LegendDebugSession.createShared(
                session,
                null,
                uriMapper,
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "false")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A `false` condition must not pause on either iteration in SHARED mode",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void sharedModeConditionalBreakpointWithVariableConditionNeverTrueNeverPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_shared_conditional_never_true_go.pure";
        String uri = "file:///workspace/debug_shared_conditional_never_true_go.pure";
        String code = mapPrintCode(new int[] {1, 2, 3});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebugSession debug = LegendDebugSession.createShared(
                session,
                null,
                uriMapper,
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$n == 99")));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A never-true condition must not pause on any iteration in SHARED mode",
                "completed", result.getState());
    }

    @Test(timeout = 60_000)
    public void logpointLogsInterpolatedMessageWithoutPausing()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_logpoint_go.pure";
        String uri = "file:///workspace/debug_logpoint_go.pure";
        String code = mapPrintCode(new int[] {1, 2});
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "    print($n, 1);");
        LegendDebug.Breakpoint logpoint = new LegendDebug.Breakpoint(uri, breakpointLine, null, "n is {$n}");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(logpoint));

        LegendDebug.Response result = debug.start();
        Assert.assertTrue("Expected the run to complete: " + result.getMessage(), result.isSuccess());
        Assert.assertEquals("A logpoint must never suspend", "completed", result.getState());
        Assert.assertTrue("Expected the interpolated logpoint output, got: " + result.getOutput(),
                result.getOutput() != null && result.getOutput().contains("n is 1"));
        Assert.assertTrue("Expected the logpoint to fire on every iteration, got: " + result.getOutput(),
                result.getOutput().contains("n is 2"));
    }

    @Test(timeout = 60_000)
    public void breakpointWithUnevaluableConditionExplainsWhyItPaused()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_condition_failure_note_go.pure";
        String uri = "file:///workspace/debug_condition_failure_note_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  print('value', 1);\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "  print('value', 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$nope == 2")));

        LegendDebug.Response paused = debug.start();
        Assert.assertEquals("paused", paused.getState());
        Assert.assertTrue("A fail-open pause must say why on the console, got: " + paused.getOutput(),
                paused.getOutput() != null && paused.getOutput().contains("could not be evaluated"));

        debug.stop();
    }

    @Test(timeout = 60_000)
    public void conditionalBreakpointWithMalformedConditionFailsOpenAndPauses()
    {
        LegendPureSession session = newInitializedSession();
        String sourceId = "debug_conditional_breakpoint_malformed_go.pure";
        String uri = "file:///workspace/debug_conditional_breakpoint_malformed_go.pure";
        String code =
                "function go():Any[*]\n" +
                        "{\n" +
                        "  print('value', 1);\n" +
                        "}\n";
        assertCompiled(session.modifyAndCompile(sourceId, code));

        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        int breakpointLine = zeroBasedLine(code, "  print('value', 1);");
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLine, "$undefinedVariable == 2")));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue("A broken condition should fail open and still pause: " + paused.getMessage(), paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());

        debug.stop();
    }

    private static String mapPrintCode(int[] values)
    {
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                literal.append(", ");
            }
            literal.append(values[i]);
        }
        return "function go():Any[*]\n" +
                "{\n" +
                "  let numbers = [" + literal + "];\n" +
                "  $numbers->map(n |\n" +
                "    print($n, 1);\n" +
                "    $n;\n" +
                "  );\n" +
                "}\n";
    }

    private static String steppingCode()
    {
        return "function helper():Any[*]\n" +
                "{\n" +
                "  print('inside', 1);\n" +
                "}\n" +
                "\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let x = 'start';\n" +
                "  helper();\n" +
                "  print('after', 1);\n" +
                "}\n";
    }

    private static String debugLocalsCode()
    {
        return "###Pure\n" +
                "import test::debug::helpers::*;\n" +
                "Class test::debug::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "Class test::debug::DebugRuntime\n" +
                "{\n" +
                "  mappings: meta::pure::mapping::Mapping[*];\n" +
                "}\n" +
                "function test::debug::routed():String[1]\n" +
                "{\n" +
                "  'routed';\n" +
                "}\n" +
                "function test::debug::fullName(person: test::debug::Person[1]):String[1]\n" +
                "{\n" +
                "  $person.firstName + ' ' + $person.lastName;\n" +
                "}\n" +
                "function test::debug::helpers::sourceImportedName(person: test::debug::Person[1]):String[1]\n" +
                "{\n" +
                "  $person.firstName;\n" +
                "}\n" +
                "function test::debug::console::consoleImportedName(person: test::debug::Person[1]):String[1]\n" +
                "{\n" +
                "  $person.lastName;\n" +
                "}\n" +
                "function test::debug::inline::inlineImportedName(person: test::debug::Person[1]):String[1]\n" +
                "{\n" +
                "  $person.firstName + ' ' + $person.lastName;\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let name = 'Ada';\n" +
                "  let answer = 42;\n" +
                "  let numbers = [1, 2];\n" +
                "  let person = ^test::debug::Person(firstName='Ada', lastName='Lovelace');\n" +
                "  let routedFunction = 'test::debug::routed__String_1_'->pathToElement()->cast(@Function<Any>);\n" +
                "  let mapping = 'test::debug::DebugMapping'->pathToElement()->cast(@meta::pure::mapping::Mapping);\n" +
                "  let runtime = ^test::debug::DebugRuntime(mappings=[$mapping]);\n" +
                "  $name;\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::debug::DebugMapping ()\n";
    }

    private static LegendDebug.Variable variable(List<LegendDebug.Variable> variables, String name)
    {
        LegendDebug.Variable variable = variableOrNull(variables, name);
        Assert.assertNotNull("Expected variable " + name + " in " + variables, variable);
        return variable;
    }

    private static LegendDebug.Variable variableOrNull(List<LegendDebug.Variable> variables, String name)
    {
        return variables.stream()
                .filter(variable -> name.equals(variable.getName()))
                .findFirst()
                .orElse(null);
    }

    private static void assertBreakpointLine(LegendPureSession session, String sourceId, String uri,
                                             int breakpointLineZeroBased, int expectedLineOneBased)
    {
        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        LegendDebugSession debug = LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, breakpointLineZeroBased)));

        LegendDebug.Response paused = debug.start();
        Assert.assertTrue("Expected breakpoint pause: " + paused.getMessage(), paused.isSuccess());
        Assert.assertEquals("paused", paused.getState());
        Assert.assertEquals("breakpoint", paused.getReason());
        Assert.assertEquals(expectedLineOneBased, paused.getStackFrames().get(0).getLine());
        debug.stop();
    }

    private static LegendDebugSession debugAtBreakpoint(LegendPureSession session, String sourceId, String uri, String code, String line)
    {
        UriMapper uriMapper = new UriMapper();
        uriMapper.register(uri, sourceId);
        return LegendDebugSession.create(
                session,
                null,
                uriMapper,
                Collections.emptyMap(),
                "go():Any[*]",
                Collections.singletonList(new LegendDebug.Breakpoint(uri, zeroBasedLine(code, line))));
    }

    private static int zeroBasedLine(String content, String expectedLine)
    {
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++)
        {
            if (expectedLine.equals(lines[i]))
            {
                return i;
            }
        }
        throw new AssertionError("Could not find line: " + expectedLine + " in:\n" + content);
    }

    private static LegendPureSession newInitializedSession()
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        return session;
    }

    private static void assertCompiled(LegendPureSession.CompileResult result)
    {
        Assert.assertTrue("Expected compile success: " + errorMessage(result), result.isSuccess());
    }

    private static String errorMessage(LegendPureSession.CompileResult result)
    {
        return result.getError() == null ? "" : result.getError().getMessage();
    }
}
