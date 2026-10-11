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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

/**
 * The Java half of the apples-to-apples benchmark: the expressions of
 * {@code bench/expressions.pure}, one function each, compiled into the
 * interpreted and the compiled engine at run time and timed there, with no
 * Rust involved. The Rust half is the evaluator crate's
 * {@code examples/expressions.rs}, which builds the same expressions with the
 * builders and times them under cargo, with no JNI involved. Both check the
 * same expected result once, outside the timed region. Not part of the
 * default surefire run:
 * <pre>
 * mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator-api -Dtest=BenchmarkNativeExpressions -DfailIfNoTests=false
 * </pre>
 * Results: {@code target/benchmarks/expressions-java.csv} (expression, engine,
 * median, p90 and minimum in nanoseconds per call; each timed sample covers a
 * batch of calls). The timed unit on both sides is one call of the
 * zero-argument function, result delivered as a value. The compiled engine is
 * measured through its {@code start} entry and by invoking the generated
 * static method directly.
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

    private static final int BATCH = 32;

    private final int warmup = Integer.getInteger("legend.bench.warmup", 200);
    private final int runs = Integer.getInteger("legend.bench.runs", 1000);

    @Test
    public void benchmark() throws IOException
    {
        MapIterable<String, String> source = Maps.immutable.with("expressions.pure", read("/bench/expressions.pure"));
        StringBuilder csv = new StringBuilder("expression,engine,median_ns,p90_ns,min_ns\n");
        StringBuilder table = new StringBuilder(String.format(Locale.ROOT, "%-24s %14s %14s %16s%n", "expression", "interpreted ns", "compiled ns", "compiled-direct"));
        long[][] medians = new long[EXPRESSIONS.length][3];

        setUpRuntime(new FunctionExecutionInterpreted());
        runtime.createInMemoryAndCompile(source);
        measure("interpreted", csv, medians, 0);
        tearDownRuntime();

        setUpRuntime(new FunctionExecutionCompiledBuilder().build(), JavaModelFactoryRegistryLoader.loader());
        runtime.createInMemoryAndCompile(source);
        measure("compiled", csv, medians, 1);
        measureDirect(csv, medians, 2);
        tearDownRuntime();

        for (int i = 0; i < EXPRESSIONS.length; i++)
        {
            table.append(String.format(Locale.ROOT, "%-24s %14d %14d %16d%n", EXPRESSIONS[i][0], medians[i][0], medians[i][1], medians[i][2]));
        }
        Path dir = Paths.get("target", "benchmarks");
        Files.createDirectories(dir);
        Files.write(dir.resolve("expressions-java.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(table);
    }

    private void measure(String engine, StringBuilder csv, long[][] medians, int column)
    {
        FunctionExecution execution = functionExecution;
        ProcessorSupport ps = processorSupport;
        for (int i = 0; i < EXPRESSIONS.length; i++)
        {
            String name = EXPRESSIONS[i][0];
            CoreInstance function = ps.package_getByUserPath("bench::" + name + "__" + returnSignature(name));
            Assert.assertNotNull(name, function);
            ListIterable<? extends CoreInstance> noArguments = org.eclipse.collections.api.factory.Lists.immutable.empty();
            Assert.assertEquals(engine + " " + name, EXPRESSIONS[i][1], render(execution.start(function, noArguments), ps));
            for (int w = 0; w < this.warmup; w++)
            {
                execution.start(function, noArguments);
            }
            long[] times = new long[this.runs];
            for (int r = 0; r < this.runs; r++)
            {
                long start = System.nanoTime();
                for (int b = 0; b < BATCH; b++)
                {
                    execution.start(function, noArguments);
                }
                times[r] = (System.nanoTime() - start) / BATCH;
            }
            Arrays.sort(times);
            long median = times[times.length / 2];
            medians[i][column] = median;
            csv.append(name).append(',').append(engine).append(',').append(median).append(',')
                    .append(times[(int) (times.length * 0.9)]).append(',').append(times[0]).append('\n');
        }
    }

    /**
     * The compiled engine without its entry: the generated static method of
     * each function, resolved once and invoked with the execution support.
     */
    private void measureDirect(StringBuilder csv, long[][] medians, int column) throws IOException
    {
        FunctionExecutionCompiled execution = (FunctionExecutionCompiled) functionExecution;
        CompiledExecutionSupport es = execution.getExecutionSupport();
        ClassLoader loader = execution.getJavaCompiler().getClassLoader();
        ProcessorSupport ps = processorSupport;
        for (int i = 0; i < EXPRESSIONS.length; i++)
        {
            String name = EXPRESSIONS[i][0];
            CoreInstance function = ps.package_getByUserPath("bench::" + name + "__" + returnSignature(name));
            Method method;
            try
            {
                Class<?> owner = loader.loadClass("org.finos.legend.pure.generated." + IdBuilder.sourceToId(function.getSourceInformation()));
                method = owner.getMethod(FunctionProcessor.functionNameToJava(function), org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException(name, e);
            }
            try
            {
                Assert.assertEquals("compiled-direct " + name, EXPRESSIONS[i][1], String.valueOf(method.invoke(null, es)));
                for (int w = 0; w < this.warmup; w++)
                {
                    method.invoke(null, es);
                }
                long[] times = new long[this.runs];
                for (int r = 0; r < this.runs; r++)
                {
                    long start = System.nanoTime();
                    for (int b = 0; b < BATCH; b++)
                    {
                        method.invoke(null, es);
                    }
                    times[r] = (System.nanoTime() - start) / BATCH;
                }
                Arrays.sort(times);
                long median = times[times.length / 2];
                medians[i][column] = median;
                csv.append(name).append(",compiled-direct,").append(median).append(',')
                        .append(times[(int) (times.length * 0.9)]).append(',').append(times[0]).append('\n');
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException(name, e.getCause() == null ? e : e.getCause());
            }
        }
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
}
