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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Exercises the graphLock model directly: arbitrary-function execution (legend/execute) and the
 * mutation-vs-execution concurrency guarantee. Graph MUTATION takes the write lock (exclusive) and
 * function EXECUTION takes the read lock (concurrent), so a compile can never interleave with an
 * in-flight execution - but independent executions/compiles must still make progress without
 * deadlocking.
 */
public class LegendPureSessionConcurrencyTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void initSession()
    {
        session = new LegendPureSession();
        session.initialize();
        Assert.assertTrue("Session should be initialized", session.isInitialized());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    // -- executeFunction: run an arbitrary zero-arg function (not just go()) --

    @Test
    public void executeFunction_bySignature_runsNamedFunction()
    {
        session.reinitialize();
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "named_fn.pure",
                "function greet():Any[*]\n{\n  print('named signature', 1)\n}\n");
        Assert.assertTrue("named function should compile, got: "
                + (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());

        LegendPureSession.ExecuteResult result = session.executeFunction("greet():Any[*]");
        Assert.assertTrue("executeFunction by signature should succeed, got: " + result.getError(),
                result.isSuccess());
        Assert.assertTrue("Should capture console output, got: " + result.getOutput(),
                result.getOutput().contains("named signature"));
    }

    @Test
    public void executeFunction_byBarePath_runsNamedFunction()
    {
        session.reinitialize();
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "named_fn.pure",
                "function greet():Any[*]\n{\n  print('bare path', 1)\n}\n");
        Assert.assertTrue("named function should compile", r.isSuccess());

        // No signature/mangling: the bare path resolves through the common zero-arg return shapes.
        LegendPureSession.ExecuteResult result = session.executeFunction("greet");
        Assert.assertTrue("executeFunction by bare path should succeed, got: " + result.getError(),
                result.isSuccess());
        Assert.assertTrue("Should capture console output, got: " + result.getOutput(),
                result.getOutput().contains("bare path"));
    }

    @Test
    public void executeFunction_byMangledId_runsNamedFunction()
    {
        session.reinitialize();
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "named_fn.pure",
                "function greet():Any[*]\n{\n  print('mangled id', 1)\n}\n");
        Assert.assertTrue("named function should compile", r.isSuccess());

        LegendPureSession.ExecuteResult result = session.executeFunction("greet__Any_MANY_");
        Assert.assertTrue("executeFunction by mangled id should succeed, got: " + result.getError(),
                result.isSuccess());
        Assert.assertTrue("Should capture console output, got: " + result.getOutput(),
                result.getOutput().contains("mangled id"));
    }

    @Test
    public void executeFunction_unknownFunction_returnsError()
    {
        session.reinitialize();
        LegendPureSession.ExecuteResult result = session.executeFunction("doesNotExist999");
        Assert.assertFalse("Unknown function should fail", result.isSuccess());
        Assert.assertTrue("Error should name the missing function, got: " + result.getError(),
                result.getError().contains("doesNotExist999"));
    }

    @Test
    public void executeFunction_blankPath_returnsError()
    {
        session.reinitialize();
        LegendPureSession.ExecuteResult result = session.executeFunction("   ");
        Assert.assertFalse("Blank function path should fail", result.isSuccess());
    }

    // -- Concurrency: compiles (write lock) and executions (read lock) must not corrupt or deadlock --

    @Test
    public void concurrentCompileAndExecute_makeProgressWithoutErrorsOrDeadlock() throws Exception
    {
        session.reinitialize();
        LegendPureSession.CompileResult seed = session.modifyAndCompile(
                "conc_go.pure", "function go():Any[*]\n{\n  print('go', 1)\n}\n");
        Assert.assertTrue("seed go() should compile", seed.isSuccess());

        int threads = 4;
        int iterations = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        AtomicInteger executeSuccesses = new AtomicInteger();
        AtomicInteger compileSuccesses = new AtomicInteger();

        for (int t = 0; t < threads; t++)
        {
            boolean writer = (t % 2) == 0;
            int id = t;
            futures.add(pool.submit(() ->
            {
                try
                {
                    for (int i = 0; i < iterations; i++)
                    {
                        if (writer)
                        {
                            // Each writer owns its own source, so writers never collide on content;
                            // the point is that the compile write-lock excludes the readers below.
                            LegendPureSession.CompileResult r = session.modifyAndCompile(
                                    "conc_writer_" + id + ".pure",
                                    "Class conc::C" + id + "_" + i + "\n{\n  v: Integer[1];\n}\n");
                            Assert.assertTrue("writer compile should be ready", r.isReady());
                            if (r.isSuccess())
                            {
                                compileSuccesses.incrementAndGet();
                            }
                        }
                        else
                        {
                            LegendPureSession.ExecuteResult r = session.executeGo();
                            if (r.isSuccess())
                            {
                                executeSuccesses.incrementAndGet();
                            }
                        }
                    }
                }
                catch (Throwable e)
                {
                    firstError.compareAndSet(null, e);
                }
            }));
        }

        for (Future<?> f : futures)
        {
            // A deadlock would surface here as a TimeoutException and fail the test.
            f.get(120, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        if (firstError.get() != null)
        {
            throw new AssertionError("Concurrent compile/execute raised an error", firstError.get());
        }
        Assert.assertTrue("Concurrent compiles should have made progress", compileSuccesses.get() > 0);
        Assert.assertTrue("Concurrent executions should have made progress", executeSuccesses.get() > 0);

        // The session must still be usable and consistent after the concurrent hammering.
        LegendPureSession.ExecuteResult after = session.executeGo();
        Assert.assertTrue("go() should still execute cleanly after concurrent load, got: " + after.getError(),
                after.isSuccess());
    }
}
