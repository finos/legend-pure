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
 * The shared boundary cases ({@link RustEvaluatorBoundaryCases}) over the C
 * ABI through FFM, and the FFM transport's own.
 */
public class TestRustFfmEvaluatorBoundary extends RustEvaluatorBoundaryCases
{
    @Override
    protected RustEvaluator newEvaluator()
    {
        return new PureRustFfmEvaluator();
    }

    @Override
    protected RustEvaluator over(RustEngineHost host)
    {
        return PureRustFfmEvaluator.over(host);
    }

    @Override
    protected long[] lastTimings()
    {
        return PureRustFfmEvaluator.lastTimings().toArray();
    }

    /**
     * The library is loaded with native access granted, so neither the load
     * nor a restricted method prints the JEP 472 warning.
     */
    @Test
    public void nativeAccessIsEnabledForTheModule()
    {
        Assert.assertTrue(PureRustFfmEvaluator.class.getModule().isNativeAccessEnabled());
    }
}
