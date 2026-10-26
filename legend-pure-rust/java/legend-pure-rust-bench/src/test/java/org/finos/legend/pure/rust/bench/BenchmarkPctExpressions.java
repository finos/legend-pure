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

package org.finos.legend.pure.rust.bench;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.PureRustFfmEvaluator;
import org.finos.legend.pure.rust.ffm.pct.RustFfmCodeRepositoryProvider;
import org.finos.legend.pure.rust.jni.pct.RustJniCodeRepositoryProvider;
import org.finos.legend.pure.rust.pct.PctExpressionsBenchmark;
import org.finos.legend.pure.rust.pct.PctExpressionsBenchmark.Engine;
import org.finos.legend.pure.rust.pct.PctExpressionsBenchmark.Mode;
import org.junit.Test;

public class BenchmarkPctExpressions
{
    @Test
    public void benchmark() throws Throwable
    {
        new PctExpressionsBenchmark(Lists.mutable.with(
                Mode.host(Engine.INTERPRETED, PlatformCodeRepositoryProvider.nativeAdapter),
                Mode.rust(Engine.INTERPRETED, RustJniCodeRepositoryProvider.rustJniAdapter, PureRustEvaluator::lastTimings),
                Mode.rust(Engine.INTERPRETED, RustFfmCodeRepositoryProvider.rustFfmAdapter, () -> PureRustFfmEvaluator.lastTimings().toArray()),
                Mode.host(Engine.COMPILED, PlatformCodeRepositoryProvider.nativeAdapter),
                Mode.rust(Engine.COMPILED, RustJniCodeRepositoryProvider.rustJniAdapter, PureRustEvaluator::lastTimings),
                Mode.rust(Engine.COMPILED, RustFfmCodeRepositoryProvider.rustFfmAdapter, () -> PureRustFfmEvaluator.lastTimings().toArray()))).run();
    }
}
