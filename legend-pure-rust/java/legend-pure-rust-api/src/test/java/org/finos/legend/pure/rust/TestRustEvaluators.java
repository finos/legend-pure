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

package org.finos.legend.pure.rust;

import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.interpreted.InterpretedHost;
import org.junit.Assert;
import org.junit.Test;

import java.lang.ref.WeakReference;

/**
 * The evaluators the natives keep, over evaluators with no native context:
 * one per scope, a scope never kept alive by the holder, an evaluator closed
 * when its scope is reclaimed or replaced and discarded after a panic.
 */
public class TestRustEvaluators
{
    /**
     * The compiled natives' holder does not keep an execution support alive:
     * once nothing else reaches it, the garbage collector reclaims it, and its
     * evaluator is closed on the holder's next call, under another support.
     */
    @Test
    public void anExecutionSupportNothingReachesIsReclaimedAndItsEvaluatorClosed() throws InterruptedException
    {
        RustEvaluators<ExecutionSupport, Stub> evaluators = RustEvaluators.perScope(support -> Stub.bound());
        ExecutionSupport kept = new Support();
        Stub keptEvaluator = evaluators.call(kept, Stub::self);
        ExecutionSupport dropped = new Support();
        Stub droppedEvaluator = evaluators.call(dropped, Stub::self);
        Assert.assertNotSame(keptEvaluator, droppedEvaluator);
        Assert.assertSame(droppedEvaluator, evaluators.call(dropped, Stub::self));
        Assert.assertEquals(2, evaluators.size());

        WeakReference<ExecutionSupport> reclaimed = new WeakReference<>(dropped);
        dropped = null;
        for (int i = 0; (i < 100) && (reclaimed.get() != null); i++)
        {
            System.gc();
            Thread.sleep(10);
        }
        Assert.assertNull("the holder keeps an execution support alive", reclaimed.get());

        Assert.assertSame(keptEvaluator, evaluators.call(kept, Stub::self));
        Assert.assertEquals(1, droppedEvaluator.frees);
        Assert.assertFalse(droppedEvaluator.isFullyInitializedForExecution());
        Assert.assertEquals(0, keptEvaluator.frees);
        Assert.assertEquals(1, evaluators.size());
    }

    /**
     * The interpreted natives' holder keeps one evaluator, for the latest
     * runtime: a call under another runtime closes it before the next one is
     * created.
     */
    @Test
    public void anotherRuntimeReplacesTheEvaluatorAndClosesTheOldOne()
    {
        RustEvaluators<Object, Stub> evaluators = RustEvaluators.latest(runtime -> Stub.bound());
        Object first = new Object();
        Stub firstEvaluator = evaluators.call(first, Stub::self);
        Assert.assertSame(firstEvaluator, evaluators.call(first, Stub::self));
        Stub secondEvaluator = evaluators.call(new Object(), Stub::self);
        Assert.assertNotSame(firstEvaluator, secondEvaluator);
        Assert.assertEquals(1, firstEvaluator.frees);
        Assert.assertEquals(0, secondEvaluator.frees);
        Assert.assertEquals(1, evaluators.size());
    }

    /**
     * A typed failure leaves the evaluator in place; a panic closes it, and
     * the next call under the same scope gets a new one.
     */
    @Test
    public void aPanicDiscardsTheEvaluatorAndATypedFailureKeepsIt()
    {
        RustEvaluators<Object, Stub> evaluators = RustEvaluators.perScope(scope -> Stub.bound());
        Object scope = new Object();
        Stub evaluator = evaluators.call(scope, Stub::self);
        Assert.assertThrows(PureRustEvaluationException.class, () -> evaluators.call(scope, Stub::failTyped));
        Assert.assertSame(evaluator, evaluators.call(scope, Stub::self));
        Assert.assertEquals(0, evaluator.frees);
        Assert.assertThrows(PureRustException.class, () -> evaluators.call(scope, Stub::panic));
        Assert.assertEquals(1, evaluator.frees);
        Assert.assertNotSame(evaluator, evaluators.call(scope, Stub::self));
        Assert.assertEquals(1, evaluators.size());
    }

    private static final class Support implements ExecutionSupport
    {
    }

    /**
     * An evaluator bound to a host with no graph and no native context; it counts the frees of its context.
     */
    private static final class Stub extends RustEvaluator
    {
        private int frees;

        static Stub bound()
        {
            Stub stub = new Stub();
            stub.attach(new InterpretedHost(null, null));
            return stub;
        }

        Stub self()
        {
            return this;
        }

        Stub failTyped()
        {
            throw PureRustEvaluationException.unsupported("a typed failure");
        }

        Stub panic()
        {
            throw new PureRustException("a panic");
        }

        @Override
        protected NodeBridge open(RustEngineHost host)
        {
            return new NodeBridge(host, this);
        }

        @Override
        protected CoreInstance[] run(CoreInstance function, CoreInstance[][] arguments)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected CoreInstance allocate(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected CoreInstance[] objectPropertyValues(long handle, String key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String[] objectPropertyNames(long handle)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void releaseHandle(long handle)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected long liveHandles()
        {
            return 0L;
        }

        @Override
        protected Crossing crossingOf(CoreInstance node)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void free()
        {
            this.frees++;
        }
    }
}
