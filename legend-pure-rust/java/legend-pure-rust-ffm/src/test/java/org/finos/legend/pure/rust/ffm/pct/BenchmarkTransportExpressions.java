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

package org.finos.legend.pure.rust.ffm.pct;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.PureRustFfmEvaluator;
import org.finos.legend.pure.rust.RustEngineHost;
import org.finos.legend.pure.rust.compiled.CompiledHost;
import org.finos.legend.pure.rust.interpreted.InterpretedHost;
import org.finos.legend.pure.rust.pct.BenchmarkNativeExpressions;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.junit.Assert;

/**
 * The twelve expressions of {@link BenchmarkNativeExpressions} under every
 * Java mode and both Rust transports, in one JVM: after each engine's own
 * modes, the expression's function node is handed to
 * {@link PureRustEvaluator#evaluate} (JNI) and to
 * {@link PureRustFfmEvaluator#evaluate} (the C ABI through FFM), each over a
 * host of that engine's graph, with no arguments; the result is checked
 * against the same expected text and the call timed in the same batches. The
 * function element is lowered on the first call and resolved by path from the
 * grown Rust model on every later call, so no mode here re-lowers per call.
 * Modes {@code rust-jni} and {@code rust-ffm} run over the interpreted
 * engine's graph, {@code rust-jni/compiled-host} and
 * {@code rust-ffm/compiled-host} over the compiled engine's. Needs both
 * cdylibs in {@code legend.rust.libdir}; not in the default run:
 * <pre>
 * mvn test -pl legend-pure-rust/java/legend-pure-rust-api,legend-pure-rust/java/legend-pure-rust-jni,legend-pure-rust/java/legend-pure-rust-ffm \
 *   -Dtest=BenchmarkTransportExpressions -Dsurefire.failIfNoSpecifiedTests=false -Dlegend.rust.libdir=.../legend-pure-rust/target/release
 * </pre>
 * Results: {@code target/benchmarks/expressions-java.csv} of this module.
 */
public class BenchmarkTransportExpressions extends BenchmarkNativeExpressions
{
    private static final CoreInstance[][] NO_ARGUMENTS = new CoreInstance[0][];

    @Override
    protected void transportModes(Engine engine) throws Throwable
    {
        String suffix = (engine == Engine.INTERPRETED) ? "" : "/compiled-host";
        try (PureRustEvaluator jni = PureRustEvaluator.over(host(engine)))
        {
            measure("rust-jni" + suffix, (name, function) -> () -> jni.evaluate(function, NO_ARGUMENTS),
                    BenchmarkTransportExpressions::render, PureRustEvaluator::lastTimings);
        }
        try (PureRustFfmEvaluator ffm = PureRustFfmEvaluator.over(host(engine)))
        {
            measure("rust-ffm" + suffix, (name, function) -> () -> ffm.evaluate(function, NO_ARGUMENTS),
                    BenchmarkTransportExpressions::render, () -> PureRustFfmEvaluator.lastTimings().toArray());
        }
    }

    private static RustEngineHost host(Engine engine)
    {
        return (engine == Engine.INTERPRETED)
                ? new InterpretedHost(repository, processorSupport)
                : new CompiledHost(((FunctionExecutionCompiled) functionExecution).getExecutionSupport().getProcessorSupport());
    }

    private static String render(Object result)
    {
        CoreInstance[] values = (CoreInstance[]) result;
        Assert.assertEquals(1, values.length);
        return values[0].getName();
    }
}
