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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Times every PCT test of the platform under four modes: each Java engine
 * with its own adapter ({@code In-Memory}) and with the Rust evaluator
 * ({@code Rust_JNI}). Not part of the default surefire run; invoke with
 * <pre>
 * mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator -Dtest=BenchmarkPctExpressions -DfailIfNoTests=false
 * </pre>
 * after {@code cargo build --release --workspace} (and
 * {@code -Dlegend.rust.libdir=.../target/release}) for a Rust side that is
 * optimised. System properties: {@code legend.bench.warmup} (default 5),
 * {@code legend.bench.runs} (default 20), {@code legend.bench.filter} (a
 * substring of the test's path). Results: {@code target/benchmarks/pct-expressions.csv}
 * (one row per test and mode: status, first run, median and minimum in
 * microseconds) and {@code pct-expressions.md} (the summary per mode).
 *
 * <p>A test's time is the whole test function: the adapter call, the
 * expression under it and its assertions. The first run is reported apart
 * from the rest because it carries what happens once: for the Rust modes,
 * the lowering of every function and class the expression reaches.
 */
public class BenchmarkPctExpressions
{
    private static final String IN_MEMORY = "meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_";
    private static final String RUST_JNI = "meta::pure::test::pct::testAdapterForRustExecution_Function_1__X_o_";
    private static final String[] MODES = {"interpreted/In-Memory", "interpreted/Rust_JNI", "compiled/In-Memory", "compiled/Rust_JNI"};

    private final int warmup = Integer.getInteger("legend.bench.warmup", 5);
    private final int runs = Integer.getInteger("legend.bench.runs", 20);
    private final String filter = System.getProperty("legend.bench.filter", "");

    @Test
    public void benchmark() throws Throwable
    {
        List<ReportScope> scopes = Arrays.asList(PlatformCodeRepositoryProvider.essentialFunctions, PlatformCodeRepositoryProvider.grammarFunctions);
        Map<String, Sample[]> samples = new TreeMap<>();

        FunctionExecutionInterpreted fe = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();
        ProcessorSupport ips = fe.getProcessorSupport();
        for (ReportScope scope : scopes)
        {
            for (CoreInstance test : tests(scope, ips))
            {
                String fqn = PackageableElement.getUserPathForPackageableElement(test, "::");
                Sample[] row = samples.computeIfAbsent(fqn, k -> new Sample[MODES.length]);
                row[0] = time(() -> fe.start(test, Lists.immutable.with(adapterValue(IN_MEMORY, ips))), false);
                row[1] = time(() -> fe.start(test, Lists.immutable.with(adapterValue(RUST_JNI, ips))), true);
            }
        }

        CompiledExecutionSupport es = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        ProcessorSupport cps = es.getProcessorSupport();
        for (ReportScope scope : scopes)
        {
            for (CoreInstance test : tests(scope, cps))
            {
                String fqn = PackageableElement.getUserPathForPackageableElement(test, "::");
                Sample[] row = samples.computeIfAbsent(fqn, k -> new Sample[MODES.length]);
                Method method = compiledMethod(test);
                CoreInstance inMemory = _Package.getByUserPath(IN_MEMORY, cps);
                CoreInstance rust = _Package.getByUserPath(RUST_JNI, cps);
                row[2] = time(() -> invoke(method, inMemory, es), false);
                row[3] = time(() -> invoke(method, rust, es), true);
            }
        }

        Path dir = Paths.get("target", "benchmarks");
        Files.createDirectories(dir);
        writeCsv(dir.resolve("pct-expressions.csv"), samples);
        String summary = summary(samples);
        Files.write(dir.resolve("pct-expressions.md"), summary.getBytes(StandardCharsets.UTF_8));
        System.out.println(summary);
    }

    private List<CoreInstance> tests(ReportScope scope, ProcessorSupport ps)
    {
        List<CoreInstance> out = new ArrayList<>();
        collect(_Package.getByUserPath(scope._package, ps), scope.filePath, ps, out);
        out.sort((a, b) -> PackageableElement.getUserPathForPackageableElement(a, "::").compareTo(PackageableElement.getUserPathForPackageableElement(b, "::")));
        return out;
    }

    private void collect(CoreInstance pkg, String sourceFilter, ProcessorSupport ps, List<CoreInstance> out)
    {
        for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(pkg, M3Properties.children, ps))
        {
            if (Instance.instanceOf(child, "Package", ps))
            {
                collect(child, sourceFilter, ps, out);
            }
            else if (PCTTools.isPCTTest(child, ps)
                    && (child.getSourceInformation() != null)
                    && child.getSourceInformation().getSourceId().startsWith(sourceFilter)
                    && PackageableElement.getUserPathForPackageableElement(child, "::").contains(this.filter))
            {
                out.add(child);
            }
        }
    }

    private static CoreInstance adapterValue(String path, ProcessorSupport ps)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification(_Package.getByUserPath(path, ps), true, ps);
    }

    private static Method compiledMethod(CoreInstance test) throws ClassNotFoundException
    {
        Class<?> owner = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(test.getSourceInformation()));
        String name = FunctionProcessor.functionNameToJava(test);
        for (Method method : owner.getMethods())
        {
            if (name.equals(method.getName()) && (method.getParameterCount() == 2))
            {
                return method;
            }
        }
        throw new IllegalStateException("no compiled method for " + name);
    }

    private static void invoke(Method method, CoreInstance adapter, CompiledExecutionSupport es) throws Throwable
    {
        try
        {
            method.invoke(null, adapter, es);
        }
        catch (InvocationTargetException e)
        {
            throw e.getCause();
        }
    }

    /**
     * Time a run: the first run apart, then warm-up, then the timed runs.
     * For a Rust mode the Rust side's own split of each timed run is read
     * back: the lowering, the run of the lowered closure (the evaluator
     * alone when no JNI read happened during it), and those reads.
     */
    private Sample time(Run run, boolean rust)
    {
        Sample sample = new Sample();
        try
        {
            long start = System.nanoTime();
            run.run();
            sample.firstUs = (System.nanoTime() - start) / 1000L;
            for (int i = 0; i < this.warmup; i++)
            {
                run.run();
            }
            long[] times = new long[this.runs];
            long[] lower = new long[this.runs];
            long[] evaluator = new long[this.runs];
            long[] reads = new long[this.runs];
            for (int i = 0; i < this.runs; i++)
            {
                start = System.nanoTime();
                run.run();
                times[i] = (System.nanoTime() - start) / 1000L;
                if (rust)
                {
                    long[] split = PureRustEvaluator.lastTimings();
                    lower[i] = split[0] / 1000L;
                    evaluator[i] = split[1] / 1000L;
                    reads[i] = split[2];
                }
            }
            Arrays.sort(times);
            Arrays.sort(lower);
            Arrays.sort(evaluator);
            Arrays.sort(reads);
            sample.medianUs = times[times.length / 2];
            sample.minUs = times[0];
            sample.lowerUs = lower[lower.length / 2];
            sample.evaluatorUs = evaluator[evaluator.length / 2];
            sample.runReads = reads[reads.length / 2];
            sample.status = "ok";
        }
        catch (Throwable t)
        {
            sample.status = t.getClass().getSimpleName();
        }
        return sample;
    }

    private static void writeCsv(Path target, Map<String, Sample[]> samples) throws IOException
    {
        StringBuilder csv = new StringBuilder("test,mode,status,first_us,median_us,min_us,rust_lower_us,rust_evaluator_us,jni_reads_in_run\n");
        for (Map.Entry<String, Sample[]> entry : samples.entrySet())
        {
            for (int i = 0; i < MODES.length; i++)
            {
                Sample s = entry.getValue()[i];
                if (s == null)
                {
                    continue;
                }
                csv.append(entry.getKey()).append(',').append(MODES[i]).append(',').append(s.status).append(',')
                        .append(s.firstUs).append(',').append(s.medianUs).append(',').append(s.minUs).append(',')
                        .append(s.lowerUs).append(',').append(s.evaluatorUs).append(',').append(s.runReads).append('\n');
            }
        }
        Files.write(target, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Per mode: how many tests ran, the sum and the median of their medians,
     * and the geometric mean of the ratio to the interpreted engine's own
     * adapter over the tests every mode completed; then the ten tests where
     * the Rust evaluator loses the most time against the host engine.
     */
    private String summary(Map<String, Sample[]> samples)
    {
        StringBuilder md = new StringBuilder();
        md.append("# PCT expressions: time per test by mode\n\n");
        md.append("Warm-up ").append(this.warmup).append(", timed runs ").append(this.runs).append(", times in microseconds; ")
                .append("ratios over the ").append(countAllOk(samples)).append(" tests every mode completed.\n\n");
        md.append("| Mode | Tests ok | Sum of medians (ms) | Median of medians | Median first run | Geomean ratio to interpreted/In-Memory | Rust lowering (median) | Rust evaluator alone (median) | Evaluator-alone geomean ratio |\n|---|---|---|---|---|---|---|---|---|\n");
        for (int i = 0; i < MODES.length; i++)
        {
            List<Long> medians = new ArrayList<>();
            List<Long> firsts = new ArrayList<>();
            List<Long> lowers = new ArrayList<>();
            List<Long> evaluators = new ArrayList<>();
            double logSum = 0;
            double evaluatorLogSum = 0;
            int ratioCount = 0;
            int evaluatorRatioCount = 0;
            boolean rust = MODES[i].endsWith("Rust_JNI");
            for (Sample[] row : samples.values())
            {
                Sample s = row[i];
                if ((s != null) && "ok".equals(s.status))
                {
                    medians.add(s.medianUs);
                    firsts.add(s.firstUs);
                    if (rust)
                    {
                        lowers.add(s.lowerUs);
                        if (s.runReads == 0)
                        {
                            evaluators.add(s.evaluatorUs);
                        }
                    }
                }
                if (allOk(row) && (row[0].medianUs > 0))
                {
                    logSum += Math.log(Math.max(1, row[i].medianUs) / (double) Math.max(1, row[0].medianUs));
                    ratioCount++;
                    if (rust && (row[i].runReads == 0))
                    {
                        evaluatorLogSum += Math.log(Math.max(1, row[i].evaluatorUs) / (double) Math.max(1, row[0].medianUs));
                        evaluatorRatioCount++;
                    }
                }
            }
            md.append("| ").append(MODES[i]).append(" | ").append(medians.size()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", sum(medians) / 1000.0)).append(" | ")
                    .append(median(medians)).append(" | ").append(median(firsts)).append(" | ")
                    .append(ratioCount == 0 ? "n/a" : String.format(Locale.ROOT, "%.2fx", Math.exp(logSum / ratioCount))).append(" | ")
                    .append(rust ? String.valueOf(median(lowers)) : "").append(" | ")
                    .append(rust ? median(evaluators) + " (" + evaluators.size() + " tests with no JNI read in the run)" : "").append(" | ")
                    .append(rust && (evaluatorRatioCount > 0) ? String.format(Locale.ROOT, "%.2fx", Math.exp(evaluatorLogSum / evaluatorRatioCount)) : "").append(" |\n");
        }
        md.append("\n## Where Rust loses the most against its host engine (median, warm)\n\n| Test | interpreted In-Memory | interpreted Rust_JNI | compiled In-Memory | compiled Rust_JNI |\n|---|---|---|---|---|\n");
        Map<String, Long> loss = new LinkedHashMap<>();
        for (Map.Entry<String, Sample[]> entry : samples.entrySet())
        {
            Sample[] row = entry.getValue();
            if (allOk(row))
            {
                loss.put(entry.getKey(), (row[1].medianUs - row[0].medianUs) + (row[3].medianUs - row[2].medianUs));
            }
        }
        loss.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(10).forEach(e ->
        {
            Sample[] row = samples.get(e.getKey());
            md.append("| ").append(e.getKey().substring(e.getKey().lastIndexOf("::") + 2)).append(" | ")
                    .append(row[0].medianUs).append(" | ").append(row[1].medianUs).append(" | ")
                    .append(row[2].medianUs).append(" | ").append(row[3].medianUs).append(" |\n");
        });
        return md.toString();
    }

    private static boolean allOk(Sample[] row)
    {
        for (Sample s : row)
        {
            if ((s == null) || !"ok".equals(s.status))
            {
                return false;
            }
        }
        return true;
    }

    private static int countAllOk(Map<String, Sample[]> samples)
    {
        int n = 0;
        for (Sample[] row : samples.values())
        {
            if (allOk(row))
            {
                n++;
            }
        }
        return n;
    }

    private static long sum(List<Long> values)
    {
        long total = 0;
        for (long v : values)
        {
            total += v;
        }
        return total;
    }

    private static long median(List<Long> values)
    {
        if (values.isEmpty())
        {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(null);
        return sorted.get(sorted.size() / 2);
    }

    private interface Run
    {
        void run() throws Throwable;
    }

    private static final class Sample
    {
        String status = "not run";
        long firstUs;
        long medianUs;
        long minUs;
        long lowerUs;
        long evaluatorUs;
        long runReads;
    }
}
