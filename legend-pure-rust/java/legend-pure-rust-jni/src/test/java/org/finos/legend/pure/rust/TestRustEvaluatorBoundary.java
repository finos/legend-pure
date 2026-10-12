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

import org.junit.Assert;
import org.junit.Test;

/**
 * The shared boundary cases ({@link RustEvaluatorBoundaryCases}) over JNI,
 * and the JNI transport's own.
 */
public class TestRustEvaluatorBoundary extends RustEvaluatorBoundaryCases
{
    @Override
    protected RustEvaluator newEvaluator()
    {
        return new PureRustEvaluator();
    }

    @Override
    protected RustEvaluator over(RustEngineHost host)
    {
        return PureRustEvaluator.over(host);
    }

    @Override
    protected long[] lastTimings()
    {
        return PureRustEvaluator.lastTimings();
    }

    /**
     * A Java exception raised while the bridge serves the Rust side is
     * taken and cleared before any other JNI call, arrives as a typed
     * failure carrying its text, and leaves the evaluator usable.
     */
    @Test
    public void aJavaFailureInsideTheBridgeArrivesTypedAndLeavesTheEvaluatorUsable()
    {
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class, () -> evaluator().crossing(null));
        Assert.assertEquals(PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("ExecutionError: describe: java.lang.NullPointerException"));
        Assert.assertEquals("42", roundTrip(repository.newIntegerCoreInstance(42L)).getName());
    }
}
