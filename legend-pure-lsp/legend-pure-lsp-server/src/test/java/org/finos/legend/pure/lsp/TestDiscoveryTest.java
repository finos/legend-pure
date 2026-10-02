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

import java.util.List;
import java.util.stream.Collectors;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsParams;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsResult;
import org.finos.legend.pure.lsp.protocol.TestResult;
import org.finos.legend.pure.lsp.protocol.TestStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers the part of legend/executeTests most likely to drift from the Maven/JUnit side: nested
 * setup/teardown bracketing across subpackages, pinned against the real emitted order rather than
 * re-derived, so a regression in the walk shows up here instead of as a mismatch against surefire.
 */
public class TestDiscoveryTest
{
    private static final String SOURCE_ID = "test_discovery.pure";
    private static final String EXTRA_SOURCE_ID = "test_discovery_extra.pure";
    private static final String FAILING_SOURCE_ID = "test_discovery_failing.pure";
    private static final String SLOW_SOURCE_ID = "test_discovery_slow.pure";
    private static final String LONG_SOURCE_ID = "test_discovery_long.pure";

    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        // test::td            - own hooks + own test
        //   test::td::alpha   - its OWN hooks + two tests
        //   test::td::beta    - no hooks; one test + one <<test.ToFix>>
        compile(SOURCE_ID,
                "function <<test.BeforePackage>> test::td::setUpRoot(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "function <<test.AfterPackage>> test::td::tearDownRoot(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::td::rootTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n" +
                "\n" +
                "function <<test.BeforePackage>> test::td::alpha::setUpAlpha(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "function <<test.AfterPackage>> test::td::alpha::tearDownAlpha(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::td::alpha::alphaTest1(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::td::alpha::alphaTest2(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::td::beta::betaTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n" +
                "\n" +
                "function <<test.ToFix>> test::td::beta::brokenTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(false, |'not fixed yet')\n" +
                "}\n");

        // A second file contributing one more test into an EXISTING package, so file scope has
        // something to narrow away from.
        compile(EXTRA_SOURCE_ID,
                "function <<test.Test>> test::td::alpha::extraAlphaTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n");

        // Deliberately slow enough that a cancel lands mid-run rather than after it. Each test folds
        // over a large range, which the interpreter evaluates node by node - the same value-spec loop
        // that polls FunctionExecutionInterpreted's cancel flag, so an abort is near-immediate.
        StringBuilder slow = new StringBuilder(
                "function <<test.BeforePackage>> test::tdslow::setUpSlow(): Boolean[1]\n{\n  true\n}\n\n" +
                "function <<test.AfterPackage>> test::tdslow::tearDownSlow(): Boolean[1]\n{\n  true\n}\n\n");
        for (int i = 1; i <= 8; i++)
        {
            slow.append("function <<test.Test>> test::tdslow::slowTest").append(i).append("(): Boolean[1]\n")
                .append("{\n  assert(range(0, 400000)->fold({a, b| $a + $b}, 0) >= 0, |'')\n}\n\n");
        }
        compile(SLOW_SOURCE_ID, slow.toString());

        // ONE test, long enough that waiting it out is clearly distinguishable from aborting it.
        //
        // The range is kept modest on purpose. Cancellation is cooperative: the flag is only polled
        // by the interpreter (FunctionExecutionInterpreted#executeFunction and
        // #findValueSpecificationExecutor), so time spent inside a single native call is not
        // interruptible. A huge range() would spend most of its time allocating the collection in
        // Java - untouchable by cancel - and this test would then be measuring that, not the abort.
        // A mid-size range with a lambda per element keeps the cost in interpreted fold callbacks,
        // which is where the checkpoints are.
        compile(LONG_SOURCE_ID,
                "function <<test.Test>> test::tdlong::veryLongTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(range(0, 3000000)->fold({a, b| $a + $b}, 0) >= 0, |'')\n" +
                "}\n");

        compile(FAILING_SOURCE_ID,
                "function <<test.BeforePackage>> test::tdfail::setUpBroken(): Boolean[1]\n" +
                "{\n" +
                "  assert(false, |'setup blew up')\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::tdfail::survivingTest(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n");
    }

    private static void compile(String sourceId, String code)
    {
        LegendPureSession.CompileResult result = session.modifyAndCompile(sourceId, code);
        Assert.assertTrue("Fixture " + sourceId + " should compile: "
                + (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    private static ExecuteTestsResult runPackage(String packagePath, boolean recursive)
    {
        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setPackagePath(packagePath);
        params.setRecursive(recursive);
        return session.executeTests(params, null);
    }

    private static List<String> names(ExecuteTestsResult result)
    {
        return result.getTests().stream().map(TestResult::getName).collect(Collectors.toList());
    }

    @Test
    public void executeTests_walksTheTreeInPureTestBuilderOrder()
    {
        ExecuteTestsResult result = runPackage("test::td", true);

        // before(root) -> subpackages sorted by package name (alpha, then beta), each bracketed by
        // its OWN hooks -> root's own tests -> after(root).
        Assert.assertEquals(
                java.util.Arrays.asList(
                        "setUpRoot",
                        "setUpAlpha", "alphaTest1", "alphaTest2", "extraAlphaTest", "tearDownAlpha",
                        "betaTest", "brokenTest",
                        "rootTest",
                        "tearDownRoot"),
                names(result));
    }

    @Test
    public void executeTests_runsEachSubpackageHookExactlyOnceAroundItsOwnSubtree()
    {
        ExecuteTestsResult result = runPackage("test::td", true);
        List<String> ordered = names(result);

        Assert.assertEquals("alpha's setup must run once, not once per test in alpha",
                1, java.util.Collections.frequency(ordered, "setUpAlpha"));
        Assert.assertEquals("root setup must run once for the whole run",
                1, java.util.Collections.frequency(ordered, "setUpRoot"));

        // alpha's hooks must bracket only alpha's own tests.
        int setUpAlpha = ordered.indexOf("setUpAlpha");
        int tearDownAlpha = ordered.indexOf("tearDownAlpha");
        Assert.assertTrue(setUpAlpha < ordered.indexOf("alphaTest1"));
        Assert.assertTrue(ordered.indexOf("alphaTest2") < tearDownAlpha);
        Assert.assertTrue("beta's test must fall outside alpha's bracket",
                ordered.indexOf("betaTest") > tearDownAlpha);
        Assert.assertTrue("root's own test must fall outside alpha's bracket",
                ordered.indexOf("rootTest") > tearDownAlpha);
    }

    @Test
    public void executeTests_marksHooksAsHooksAndTestsAsTests()
    {
        ExecuteTestsResult result = runPackage("test::td", true);

        TestResult setUpAlpha = find(result, "setUpAlpha");
        Assert.assertTrue("a BeforePackage function must be flagged as a hook", setUpAlpha.isHook());
        Assert.assertEquals("before", setUpAlpha.getHookKind());

        TestResult tearDownRoot = find(result, "tearDownRoot");
        Assert.assertTrue(tearDownRoot.isHook());
        Assert.assertEquals("after", tearDownRoot.getHookKind());

        TestResult alphaTest1 = find(result, "alphaTest1");
        Assert.assertFalse(alphaTest1.isHook());
        Assert.assertNull(alphaTest1.getHookKind());
    }

    @Test
    public void executeTests_reportsToFixAsSkippedAndKeepsItOutOfPassFailTallies()
    {
        ExecuteTestsResult result = runPackage("test::td", true);

        TestResult broken = find(result, "brokenTest");
        Assert.assertEquals(TestStatus.SKIPPED.getProtocolValue(), broken.getStatus());
        Assert.assertEquals(1, result.getSkipped());
        Assert.assertEquals("a skipped ToFix test must not be counted as a failure", 0, result.getFailed());
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getTests().size(),
                result.getPassed() + result.getFailed() + result.getSkipped());
    }

    @Test
    public void executeTests_nonRecursive_keepsOnlyTheRootPackagesOwnTests()
    {
        ExecuteTestsResult result = runPackage("test::td", false);

        Assert.assertEquals(java.util.Arrays.asList("setUpRoot", "rootTest", "tearDownRoot"), names(result));
    }

    @Test
    public void executeTests_failingHookIsReportedButDoesNotAbortTheRun()
    {
        ExecuteTestsResult result = runPackage("test::tdfail", true);

        TestResult hook = find(result, "setUpBroken");
        Assert.assertEquals(TestStatus.FAILED.getProtocolValue(), hook.getStatus());
        Assert.assertTrue("the failure message should name it as setup, not as a plain test",
                hook.getMessage().contains("Setup function"));

        TestResult surviving = find(result, "survivingTest");
        Assert.assertEquals("a failing hook must not stop the rest of the suite - JUnit carries on too",
                TestStatus.PASSED.getProtocolValue(), surviving.getStatus());
        Assert.assertFalse("the run as a whole failed, because the hook failed", result.isSuccess());
    }

    @Test
    public void executeTests_fileScope_narrowsTestsButKeepsTheInheritedHookChain()
    {
        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setUri("pure://" + EXTRA_SOURCE_ID);
        ExecuteTestsResult result = session.executeTests(params, EXTRA_SOURCE_ID);

        // Only this file's test runs - alphaTest1/alphaTest2 live in the same package but a
        // different file - yet it still gets alpha's hooks AND the inherited root hooks, outermost
        // setup first and innermost teardown first.
        Assert.assertEquals(
                java.util.Arrays.asList("setUpRoot", "setUpAlpha", "extraAlphaTest", "tearDownAlpha", "tearDownRoot"),
                names(result));
    }

    /**
     * Runs {@code packagePath} on another thread and cancels it once the run has genuinely started
     * executing, rather than after a fixed sleep or as soon as it is merely registered.
     * <p>
     * That distinction matters: a run is registered (and therefore cancellable) a moment before the
     * walk begins, so cancelling on registration tests "cancel a run that never started" - a
     * different, much easier case. Latching on a real test event guarantees the walk is under way.
     *
     * @param awaitEntries how many entries must finish before cancelling (0 = cancel once the first
     *                     entry has been *started*, for a scope with a single long test)
     */
    private static ExecuteTestsResult runAndCancel(String packagePath, String runId, int awaitEntries,
                                                   long[] unwindMsOut) throws Exception
    {
        return runAndCancel(packagePath, runId, awaitEntries, unwindMsOut, false);
    }

    private static ExecuteTestsResult runAndCancel(String packagePath, String runId, int awaitEntries,
                                                   long[] unwindMsOut, boolean parallel) throws Exception
    {
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(Math.max(awaitEntries, 1));
        session.setClient(new TestEventLatchClient(started, finished));
        try
        {
            ExecuteTestsParams params = new ExecuteTestsParams();
            params.setPackagePath(packagePath);
            params.setRunId(runId);
            params.setParallel(parallel);

            java.util.concurrent.atomic.AtomicReference<ExecuteTestsResult> outcome = new java.util.concurrent.atomic.AtomicReference<>();
            Thread runner = new Thread(() -> outcome.set(session.executeTests(params, null)), "cancel-runner-" + runId);
            runner.start();

            if (awaitEntries > 0)
            {
                Assert.assertTrue("run never got as far as finishing an entry",
                    finished.await(60, java.util.concurrent.TimeUnit.SECONDS));
            }
            else
            {
                Assert.assertTrue("run never started an entry",
                    started.await(60, java.util.concurrent.TimeUnit.SECONDS));
                // TEST_STARTED fires just before the executor is registered as in-flight; give it a
                // moment to actually get there so this exercises the mid-flight abort path.
                Thread.sleep(500);
            }

            long cancelledAt = System.currentTimeMillis();
            session.cancelTests(runId, false);
            runner.join(60_000);
            if (unwindMsOut != null)
            {
                unwindMsOut[0] = System.currentTimeMillis() - cancelledAt;
            }
            Assert.assertFalse("the run did not unwind after cancellation", runner.isAlive());
            return outcome.get();
        }
        finally
        {
            session.setClient(null);
        }
    }

    /** Latches on the run's streamed events so a cancel can be timed against real progress. */
    private static final class TestEventLatchClient extends NoOpLegendLanguageClient
    {
        private final java.util.concurrent.CountDownLatch started;
        private final java.util.concurrent.CountDownLatch finished;

        TestEventLatchClient(java.util.concurrent.CountDownLatch started, java.util.concurrent.CountDownLatch finished)
        {
            this.started = started;
            this.finished = finished;
        }

        @Override
        public void testEvent(org.finos.legend.pure.lsp.protocol.TestEvent event)
        {
            if ("testStarted".equals(event.getKind()))
            {
                this.started.countDown();
            }
            else if ("testFinished".equals(event.getKind()))
            {
                this.finished.countDown();
            }
        }
    }

    @Test
    public void cancelTests_stopsTheRunButStillRunsTeardown() throws Exception
    {
        // A cancelled run must not strand what its <<test.BeforePackage>> set up - for a relational
        // suite that would leave H2 tables behind and break the NEXT run's setup. Cancel only after
        // two entries have completed, so setUp has definitely run and there is something to tear down.
        ExecuteTestsResult result = runAndCancel("test::tdslow", "cancel-me", 2, null);

        Assert.assertNotNull(result);
        Assert.assertTrue("result must be flagged cancelled", result.isCancelled());
        Assert.assertFalse("a cancelled run is not a success - it did not finish", result.isSuccess());
        Assert.assertTrue("teardown must still run so it does not strand setup state",
            result.getTests().stream().anyMatch(t -> "tearDownSlow".equals(t.getName())));
        // Count what actually EXECUTED, not the number of entries: a cancelled run still reports the
        // tests it skipped, so the entry count stays at 8 either way.
        Assert.assertTrue("the run must stop early - " + result.getPassed() + " of 8 tests still ran",
            result.getPassed() < 8);
        Assert.assertTrue("the tests it never got to must be reported as skipped",
            result.getTests().stream().anyMatch(t ->
                TestStatus.SKIPPED.getProtocolValue().equals(t.getStatus())
                        && t.getMessage() != null && t.getMessage().contains("cancelled")));
    }

    @Test
    public void cancelTests_abortsATestThatIsAlreadyMidFlight() throws Exception
    {
        // The case that matters most in practice: the run is inside ONE long test, so stopping at the
        // next entry boundary is not good enough - the in-flight executor has to be aborted. A single
        // test here, so there is no "next entry" to fall back on.
        long[] unwindMs = new long[1];
        ExecuteTestsResult result = runAndCancel("test::tdlong", "cancel-midflight", 0, unwindMs);

        Assert.assertTrue(result.isCancelled());
        Assert.assertTrue("unwinding took " + unwindMs[0] + "ms - the in-flight executor was not aborted",
            unwindMs[0] < 15_000);
    }

    @Test
    public void onLastClientDisconnected_cancelsInFlightRuns() throws Exception
    {
        // The safety net for a client that dies without cancelling: without it the daemon keeps
        // executing, holding the graph read lock, with nobody waiting on the result.
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        session.setClient(new TestEventLatchClient(started, new java.util.concurrent.CountDownLatch(1)));
        try
        {
            ExecuteTestsParams params = new ExecuteTestsParams();
            params.setPackagePath("test::tdlong");
            params.setRunId("disconnect-me");

            java.util.concurrent.atomic.AtomicReference<ExecuteTestsResult> outcome = new java.util.concurrent.atomic.AtomicReference<>();
            Thread runner = new Thread(() -> outcome.set(session.executeTests(params, null)), "disconnect-runner");
            runner.start();
            Assert.assertTrue(started.await(60, java.util.concurrent.TimeUnit.SECONDS));
            Thread.sleep(500);

            session.onLastClientDisconnected();

            runner.join(60_000);
            Assert.assertFalse("a disconnect must stop the run, not leave it burning CPU", runner.isAlive());
            Assert.assertTrue(outcome.get().isCancelled());
        }
        finally
        {
            session.setClient(null);
        }
    }

    @Test
    public void cancelTests_unknownRunId_isNotAnError()
    {
        // Normal when the run already finished; callers (a CLI's Ctrl-C handler) race with completion.
        Assert.assertTrue(session.cancelTests("no-such-run", false).isEmpty());
    }

    @Test
    public void executeTests_unknownPackage_failsWithAClearMessage()
    {
        ExecuteTestsResult result = runPackage("test::no::such::package", true);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue("message should name the missing package, got: " + result.getError(),
                result.getError().contains("test::no::such::package"));
    }

    @Test
    public void executeTests_parallelWithinANodeProducesTheSameOrderedResultsAsSerial()
    {
        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setPackagePath("test::td");
        params.setParallel(true);
        ExecuteTestsResult parallel = session.executeTests(params, null);

        Assert.assertEquals("parallel fan-out must not change the reported order",
                names(runPackage("test::td", true)), names(parallel));
        Assert.assertEquals(0, parallel.getFailed());
    }

    /**
     * test::tdslow has more tests in one node than the execution pool has threads, so cancelling
     * mid-run always lands with tasks still queued. Those queued tasks are what used to be dropped
     * without being completed, parking the walk in Future.get() for good - with the graph read lock
     * still held, which is why the symptom showed up later as compiles silently never happening.
     */
    @Test
    public void executeTests_cancellingAParallelRunUnwindsAndReleasesTheGraphLock() throws Exception
    {
        long[] unwindMs = new long[1];
        ExecuteTestsResult result = runAndCancel("test::tdslow", "cancel-parallel", 1, unwindMs, true);

        Assert.assertNotNull("the cancelled parallel run never produced a result", result);
        Assert.assertTrue("a cancelled run must report itself as cancelled", result.isCancelled());
        // Deliberately far below ENTRY_WAIT_BACKSTOP: the backstop also ends this wait eventually, so
        // only a time bound distinguishes "cancellation worked" from "the backstop bailed us out".
        Assert.assertTrue("unwinding took " + unwindMs[0] + "ms - queued tasks were abandoned rather "
                + "than cancelled, so the walk sat in Future.get() until the backstop fired",
            unwindMs[0] < 15_000);
        Assert.assertFalse("the graph lock was still held after the run unwound", session.isGraphLockHeld());
    }

    @Test
    public void executeTests_theGraphIsStillCompilableAfterCancellingAParallelRun() throws Exception
    {
        runAndCancel("test::tdslow", "cancel-parallel-then-compile", 1, null, true);

        // The user-visible symptom of the leaked read lock: edits stop landing, silently. A compile
        // that returns is the only proof the lock actually came back.
        compile(EXTRA_SOURCE_ID,
                "function <<test.Test>> test::td::alpha::extraAlphaTest(): Boolean[1]\n{\n  true\n}\n");
        Assert.assertFalse(session.isGraphLockHeld());
    }

    private static TestResult find(ExecuteTestsResult result, String name)
    {
        return result.getTests().stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry named '" + name + "' in " + names(result)));
    }
}
