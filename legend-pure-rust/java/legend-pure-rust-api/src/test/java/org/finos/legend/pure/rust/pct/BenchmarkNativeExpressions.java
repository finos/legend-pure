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

package org.finos.legend.pure.rust.pct;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Java half of the apples-to-apples benchmark: the expressions of
 * {@code bench/expressions.pure}, one function each, compiled into the
 * interpreted and the compiled engine at run time and timed there, with no
 * Rust involved. The Rust half is the evaluator crate's
 * {@code examples/expressions.rs}, which builds the same expressions with the
 * builders and times them under cargo, with no host involved. Both check the
 * same expected result once, outside the timed region. Not part of the
 * default surefire run:
 * <pre>
 * mvn test -pl legend-pure-rust/java/legend-pure-rust-api -Dtest=BenchmarkNativeExpressions -DfailIfNoTests=false
 * </pre>
 * Results: {@code target/benchmarks/expressions-java.csv} (expression, mode,
 * median, p90 and minimum in nanoseconds per call, and for a mode that crosses
 * into Rust the medians of the Rust side's split of the call: lowering, run,
 * host reads during each; each timed sample covers a batch of calls). The
 * timed unit is one call of the zero-argument function, result delivered as a
 * value. The compiled engine is measured through its {@code start} entry and
 * by invoking the generated static method directly. A transport module
 * extends this class and adds its modes through {@link #transportModes}, so
 * every mode runs in one JVM.
 */
public class BenchmarkNativeExpressions extends AbstractPureTestWithCoreCompiled
{
    private static final String[][] EXPRESSIONS = {
            {"e01_plus", "3"},
            {"e02_arithmetic", "20"},
            {"e03_let", "25"},
            {"e04_if", "yes"},
            {"e05_map_fold", "30"},
            {"e06_range_filter", "50"},
            {"e07_sort_at", "1"},
            {"e08_format", "hello world, 42"},
            {"e09_date", "2020-04-30"},
            {"e10_match", "6"},
            {"e11_remove_duplicates", "3"},
            {"e12_join_strings", "<a, b, c>"},
    };

    /**
     * Calls per timed sample: the clock ticks every 41.7 ns on Apple Silicon,
     * so one call would be quantised to that.
     */
    private static final int BATCH = 32;

    private static final String BASELINE = "compiled-direct";

    private final int warmup = Integer.getInteger("legend.bench.warmup", 200);
    private final int runs = Integer.getInteger("legend.bench.runs", 1000);
    private final List<Column> columns = new ArrayList<>();

    /**
     * The Java engine whose runtime is set up when {@link #transportModes} runs.
     */
    protected enum Engine
    {
        INTERPRETED,
        COMPILED
    }

    /**
     * One call of an expression, the unit a mode times.
     */
    protected interface Call
    {
        Object call() throws Throwable;
    }

    /**
     * The call a mode makes for an expression, built once before it is timed.
     */
    protected interface Calls
    {
        /**
         * @param name     the expression's name, {@code e01_plus} …
         * @param function the expression's function in the runtime's graph
         * @return the call to time
         * @throws Throwable when the call cannot be built
         */
        Call of(String name, CoreInstance function) throws Throwable;
    }

    @Test
    public void benchmark() throws Throwable
    {
        MapIterable<String, String> source = Maps.immutable.with("expressions.pure", read("/bench/expressions.pure"));

        setUpRuntime(new FunctionExecutionInterpreted());
        runtime.createInMemoryAndCompile(source);
        FunctionExecution interpreted = functionExecution;
        ProcessorSupport ips = processorSupport;
        measure("interpreted", (name, function) -> () -> interpreted.start(function, Lists.immutable.empty()), result -> render((CoreInstance) result, ips), null);
        transportModes(Engine.INTERPRETED);
        tearDownRuntime();

        setUpRuntime(new FunctionExecutionCompiledBuilder().build(), JavaModelFactoryRegistryLoader.loader());
        runtime.createInMemoryAndCompile(source);
        FunctionExecutionCompiled compiled = (FunctionExecutionCompiled) functionExecution;
        ProcessorSupport cps = processorSupport;
        measure("compiled", (name, function) -> () -> compiled.start(function, Lists.immutable.empty()), result -> render((CoreInstance) result, cps), null);
        CompiledExecutionSupport es = compiled.getExecutionSupport();
        ClassLoader loader = compiled.getJavaCompiler().getClassLoader();
        measure(BASELINE, (name, function) -> direct(generatedMethod(loader, function), es), String::valueOf, null);
        transportModes(Engine.COMPILED);
        tearDownRuntime();

        write();
    }

    /**
     * Where a transport module adds its modes, each through {@link #measure}:
     * called once with the interpreted engine's runtime set up and the
     * expressions compiled into it, once with the compiled engine's.
     *
     * @param engine the engine whose runtime is set up
     * @throws Throwable when a mode fails
     */
    protected void transportModes(Engine engine) throws Throwable
    {
    }

    /**
     * Time one mode over every expression: the result checked once, then
     * warm-up, then batches of calls; for a mode that crosses into Rust, the
     * Rust side's split of the last call of each batch is read after the
     * batch, outside the timed region.
     *
     * @param mode   the mode's label
     * @param calls  how the mode calls an expression's function
     * @param render the rendering of one call's result, compared with the expected text
     * @param split  {@code [loweringNanos, runNanos, readsDuringRun, readsDuringLowering]} of the last call on this
     *               thread, or {@code null} for a mode that does not cross
     * @throws Throwable when a call fails or a result differs
     */
    protected final void measure(String mode, Calls calls, Function<Object, String> render, Supplier<long[]> split) throws Throwable
    {
        Column column = new Column(mode, split != null);
        for (int i = 0; i < EXPRESSIONS.length; i++)
        {
            String name = EXPRESSIONS[i][0];
            CoreInstance function = processorSupport.package_getByUserPath(functionPath(name));
            Assert.assertNotNull(name, function);
            Call call = calls.of(name, function);
            Assert.assertEquals(mode + " " + name, EXPRESSIONS[i][1], render.apply(call.call()));
            for (int w = 0; w < this.warmup; w++)
            {
                call.call();
            }
            long[] times = new long[this.runs];
            long[][] splits = new long[4][this.runs];
            for (int r = 0; r < this.runs; r++)
            {
                long start = System.nanoTime();
                for (int b = 0; b < BATCH; b++)
                {
                    call.call();
                }
                times[r] = (System.nanoTime() - start) / BATCH;
                if (split != null)
                {
                    long[] last = split.get();
                    for (int k = 0; k < 4; k++)
                    {
                        splits[k][r] = last[k];
                    }
                }
            }
            Arrays.sort(times);
            column.median[i] = times[times.length / 2];
            column.p90[i] = times[(int) (times.length * 0.9)];
            column.min[i] = times[0];
            for (int k = 0; k < 4; k++)
            {
                Arrays.sort(splits[k]);
                column.split[k][i] = splits[k][this.runs / 2];
            }
        }
        this.columns.add(column);
    }

    /**
     * The path of an expression's function, as {@code package_getByUserPath} finds it.
     *
     * @param name the expression's name
     * @return {@code bench::<name>__<return type>_1_}
     */
    protected static String functionPath(String name)
    {
        return "bench::" + name + "__" + returnSignature(name);
    }

    private static Method generatedMethod(ClassLoader loader, CoreInstance function) throws ReflectiveOperationException
    {
        Class<?> owner = loader.loadClass("org.finos.legend.pure.generated." + IdBuilder.sourceToId(function.getSourceInformation()));
        return owner.getMethod(FunctionProcessor.functionNameToJava(function), ExecutionSupport.class);
    }

    private static Call direct(Method method, CompiledExecutionSupport es)
    {
        return () ->
        {
            try
            {
                return method.invoke(null, es);
            }
            catch (InvocationTargetException e)
            {
                throw e.getCause();
            }
        };
    }

    private void write() throws IOException
    {
        StringBuilder csv = new StringBuilder("expression,mode,median_ns,p90_ns,min_ns,rust_lower_ns,rust_run_ns,rust_reads_in_run,rust_reads_in_lowering\n");
        for (Column column : this.columns)
        {
            for (int i = 0; i < EXPRESSIONS.length; i++)
            {
                csv.append(EXPRESSIONS[i][0]).append(',').append(column.mode).append(',')
                        .append(column.median[i]).append(',').append(column.p90[i]).append(',').append(column.min[i]);
                for (int k = 0; k < 4; k++)
                {
                    csv.append(',').append(column.rust ? String.valueOf(column.split[k][i]) : "");
                }
                csv.append('\n');
            }
        }
        Path dir = Paths.get("target", "benchmarks");
        Files.createDirectories(dir);
        Files.write(dir.resolve("expressions-java.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));

        Column baseline = this.columns.stream().filter(c -> BASELINE.equals(c.mode)).findFirst().orElseThrow(IllegalStateException::new);
        StringBuilder table = new StringBuilder();
        table.append(String.format(Locale.ROOT, "JVM: %s %s (%s), %s; warm-up %d, runs %d, batch %d; median ns per call (ratio to %s)%n",
                System.getProperty("java.vm.vendor"), System.getProperty("java.vm.name"), System.getProperty("java.runtime.version"),
                System.getProperty("os.arch"), this.warmup, this.runs, BATCH, BASELINE));
        table.append(String.format(Locale.ROOT, "%-24s", "expression"));
        for (Column column : this.columns)
        {
            table.append(String.format(Locale.ROOT, " %24s", column.mode));
        }
        table.append(System.lineSeparator());
        for (int i = 0; i < EXPRESSIONS.length; i++)
        {
            table.append(String.format(Locale.ROOT, "%-24s", EXPRESSIONS[i][0]));
            for (Column column : this.columns)
            {
                table.append(String.format(Locale.ROOT, " %14d (%6.2fx)", column.median[i], column.median[i] / (double) Math.max(1, baseline.median[i])));
            }
            table.append(System.lineSeparator());
        }
        for (Column column : this.columns)
        {
            if (column.rust)
            {
                table.append(System.lineSeparator()).append(column.mode).append(": Rust side's split, median ns of the last call of each batch (lowering, run, reads in run, reads in lowering)")
                        .append(System.lineSeparator());
                for (int i = 0; i < EXPRESSIONS.length; i++)
                {
                    table.append(String.format(Locale.ROOT, "%-24s %10d %10d %6d %6d%n", EXPRESSIONS[i][0],
                            column.split[0][i], column.split[1][i], column.split[2][i], column.split[3][i]));
                }
            }
        }
        System.out.println(table);
    }

    private static String returnSignature(String name)
    {
        switch (name)
        {
            case "e04_if":
            case "e08_format":
            case "e12_join_strings":
                return "String_1_";
            case "e09_date":
                return "Date_1_";
            default:
                return "Integer_1_";
        }
    }

    private static String render(CoreInstance result, ProcessorSupport ps)
    {
        CoreInstance value = Instance.instanceOf(result, M3Paths.InstanceValue, ps)
                ? Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, ps)
                : result;
        return value.getName();
    }

    private static String read(String resource) throws IOException
    {
        try (InputStream in = BenchmarkNativeExpressions.class.getResourceAsStream(resource))
        {
            Assert.assertNotNull(resource, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * One mode's results, per expression.
     */
    private static final class Column
    {
        private final String mode;
        private final boolean rust;
        private final long[] median = new long[EXPRESSIONS.length];
        private final long[] p90 = new long[EXPRESSIONS.length];
        private final long[] min = new long[EXPRESSIONS.length];
        private final long[][] split = new long[4][EXPRESSIONS.length];

        private Column(String mode, boolean rust)
        {
            this.mode = mode;
            this.rust = rust;
        }
    }
}
