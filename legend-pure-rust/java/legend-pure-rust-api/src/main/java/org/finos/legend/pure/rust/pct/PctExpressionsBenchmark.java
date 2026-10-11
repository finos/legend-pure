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
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

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
import java.util.function.Supplier;

/**
 * Times every PCT test of the platform under a list of modes: a Java engine
 * with an adapter, the engine's own or a Rust transport's. Each transport
 * module invokes it from a test class of its own with the modes it adds.
 * System properties: {@code legend.bench.warmup} (default 5),
 * {@code legend.bench.runs} (default 20), {@code legend.bench.batch} (calls
 * per timed sample, default 1), {@code legend.bench.filter} (a substring of
 * the test's path). Results: {@code target/benchmarks/pct-expressions.csv}
 * (one row per test and mode: status, first run, median, p90 and minimum in
 * nanoseconds, and for a Rust mode the medians of the Rust side's split in
 * nanoseconds and reads) and {@code pct-expressions.md} (the summary per
 * mode, over the tests every mode completed).
 *
 * <p>A test's time is the whole test function: the adapter call, the
 * expression under it and its assertions. The first run is reported apart
 * from the rest; a timed sample is a batch of calls divided by its size.
 */
public final class PctExpressionsBenchmark
{
    /**
     * The Java engine a mode runs the tests on.
     */
    public enum Engine
    {
        INTERPRETED,
        COMPILED
    }

    /**
     * One way of running every test: an engine and the adapter it hands the
     * test, with, for a Rust adapter, where the Rust side's split of the last
     * call is read ({@code [loweringNanos, runNanos, readsDuringRun, readsDuringLowering]}).
     */
    public static final class Mode
    {
        private final Engine engine;
        private final Adapter adapter;
        private final Supplier<long[]> rustTimings;
        private final String label;

        private Mode(Engine engine, Adapter adapter, Supplier<long[]> rustTimings)
        {
            this.engine = engine;
            this.adapter = adapter;
            this.rustTimings = rustTimings;
            this.label = engine.name().toLowerCase(Locale.ROOT) + "/" + adapter.name;
        }

        /**
         * The engine with one of its own adapters.
         *
         * @param engine  the engine
         * @param adapter the adapter
         * @return the mode
         */
        public static Mode host(Engine engine, Adapter adapter)
        {
            return new Mode(engine, adapter, null);
        }

        /**
         * The engine with a Rust transport's adapter.
         *
         * @param engine      the engine
         * @param adapter     the adapter
         * @param rustTimings the transport's split of the last call on this thread
         * @return the mode
         */
        public static Mode rust(Engine engine, Adapter adapter, Supplier<long[]> rustTimings)
        {
            return new Mode(engine, adapter, rustTimings);
        }

        private boolean isRust()
        {
            return this.rustTimings != null;
        }
    }

    private final List<Mode> modes;
    private final int warmup = Integer.getInteger("legend.bench.warmup", 5);
    private final int runs = Integer.getInteger("legend.bench.runs", 20);
    private final int batch = Math.max(1, Integer.getInteger("legend.bench.batch", 1));
    private final String filter = System.getProperty("legend.bench.filter", "");

    /**
     * @param modes the modes, in the order they are reported; the first is the baseline of the ratios, and each
     *              Rust mode is set against the first host mode of its engine
     */
    public PctExpressionsBenchmark(List<Mode> modes)
    {
        this.modes = modes;
    }

    /**
     * Run every test under every mode and write the results.
     *
     * @throws Throwable when an engine cannot be built or a result cannot be written
     */
    public void run() throws Throwable
    {
        List<ReportScope> scopes = Arrays.asList(PlatformCodeRepositoryProvider.essentialFunctions, PlatformCodeRepositoryProvider.grammarFunctions);
        Map<String, Sample[]> samples = new TreeMap<>();

        if (this.modes.stream().anyMatch(m -> m.engine == Engine.INTERPRETED))
        {
            FunctionExecutionInterpreted fe = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();
            ProcessorSupport ips = fe.getProcessorSupport();
            for (ReportScope scope : scopes)
            {
                for (CoreInstance test : tests(scope, ips))
                {
                    Sample[] row = samples.computeIfAbsent(PackageableElement.getUserPathForPackageableElement(test, "::"), k -> new Sample[this.modes.size()]);
                    for (int i = 0; i < this.modes.size(); i++)
                    {
                        Mode mode = this.modes.get(i);
                        if (mode.engine == Engine.INTERPRETED)
                        {
                            CoreInstance adapter = adapterValue(mode.adapter.function, ips);
                            row[i] = time(() -> fe.start(test, Lists.immutable.with(adapter)), mode);
                        }
                    }
                }
            }
        }

        if (this.modes.stream().anyMatch(m -> m.engine == Engine.COMPILED))
        {
            CompiledExecutionSupport es = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
            ProcessorSupport cps = es.getProcessorSupport();
            for (ReportScope scope : scopes)
            {
                for (CoreInstance test : tests(scope, cps))
                {
                    Sample[] row = samples.computeIfAbsent(PackageableElement.getUserPathForPackageableElement(test, "::"), k -> new Sample[this.modes.size()]);
                    Method method = compiledMethod(test);
                    for (int i = 0; i < this.modes.size(); i++)
                    {
                        Mode mode = this.modes.get(i);
                        if (mode.engine == Engine.COMPILED)
                        {
                            CoreInstance adapter = _Package.getByUserPath(mode.adapter.function, cps);
                            row[i] = time(() -> invoke(method, adapter, es), mode);
                        }
                    }
                }
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
            if (Instance.instanceOf(child, M3Paths.Package, ps))
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
     * Time a run: the first run apart, then warm-up, then the timed samples,
     * each a batch of runs. For a Rust mode the Rust side's own split of the
     * last run of each batch is read back, outside the timed region: the
     * lowering, the run of the lowered closure (the evaluator alone when no
     * bridge read happened during it), and the reads made during each.
     */
    private Sample time(Run run, Mode mode)
    {
        Sample sample = new Sample();
        try
        {
            long start = System.nanoTime();
            run.run();
            sample.firstNs = System.nanoTime() - start;
            for (int i = 0; i < this.warmup; i++)
            {
                run.run();
            }
            long[] times = new long[this.runs];
            long[][] split = new long[4][this.runs];
            for (int i = 0; i < this.runs; i++)
            {
                start = System.nanoTime();
                for (int b = 0; b < this.batch; b++)
                {
                    run.run();
                }
                times[i] = (System.nanoTime() - start) / this.batch;
                if (mode.isRust())
                {
                    long[] last = mode.rustTimings.get();
                    for (int k = 0; k < 4; k++)
                    {
                        split[k][i] = last[k];
                    }
                }
            }
            Arrays.sort(times);
            for (long[] part : split)
            {
                Arrays.sort(part);
            }
            sample.medianNs = times[times.length / 2];
            sample.p90Ns = times[(int) (times.length * 0.9)];
            sample.minNs = times[0];
            sample.lowerNs = split[0][this.runs / 2];
            sample.runNs = split[1][this.runs / 2];
            sample.runReads = split[2][this.runs / 2];
            sample.lowerReads = split[3][this.runs / 2];
            sample.status = "ok";
        }
        catch (Throwable t)
        {
            sample.status = t.getClass().getSimpleName();
        }
        return sample;
    }

    private void writeCsv(Path target, Map<String, Sample[]> samples) throws IOException
    {
        StringBuilder csv = new StringBuilder("test,mode,status,first_ns,median_ns,p90_ns,min_ns,rust_lower_ns,rust_run_ns,rust_reads_in_run,rust_reads_in_lowering\n");
        for (Map.Entry<String, Sample[]> entry : samples.entrySet())
        {
            for (int i = 0; i < this.modes.size(); i++)
            {
                Sample s = entry.getValue()[i];
                if (s == null)
                {
                    continue;
                }
                csv.append(entry.getKey()).append(',').append(this.modes.get(i).label).append(',').append(s.status).append(',')
                        .append(s.firstNs).append(',').append(s.medianNs).append(',').append(s.p90Ns).append(',').append(s.minNs).append(',')
                        .append(s.lowerNs).append(',').append(s.runNs).append(',').append(s.runReads).append(',').append(s.lowerReads).append('\n');
            }
        }
        Files.write(target, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The index of the first host mode on the same engine as a Rust mode,
     * or -1.
     */
    private int hostModeOf(int rustMode)
    {
        Engine engine = this.modes.get(rustMode).engine;
        for (int i = 0; i < this.modes.size(); i++)
        {
            Mode mode = this.modes.get(i);
            if (!mode.isRust() && (mode.engine == engine))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Per mode, over the tests every mode completed: the sum and the median of
     * their medians, the medians of their p90s, minimums and first runs, the
     * geometric mean of the ratio to the first mode, and for a Rust mode the
     * medians of its split; then the ten tests where the Rust modes lose the
     * most time against their host modes. Times in microseconds, the split in
     * nanoseconds.
     */
    private String summary(Map<String, Sample[]> samples)
    {
        String baseline = this.modes.get(0).label;
        StringBuilder md = new StringBuilder();
        md.append("# PCT expressions: time per test by mode\n\n");
        md.append(String.format(Locale.ROOT, "%s %s (%s), %s; ", System.getProperty("java.vm.vendor"), System.getProperty("java.vm.name"),
                System.getProperty("java.runtime.version"), System.getProperty("os.arch")));
        md.append("warm-up ").append(this.warmup).append(", timed samples ").append(this.runs).append(", batch ").append(this.batch)
                .append("; test times in microseconds, the Rust split in nanoseconds; every column but the first over the ")
                .append(countAllOk(samples)).append(" tests every mode completed.\n\n");
        md.append("| Mode | Tests ok | Sum of medians (ms) | Median of medians | Median of p90s | Median of minimums | Median first run | Geomean ratio to ")
                .append(baseline).append(" | Rust lowering (ns) | Rust run (ns) | Rust run, no host read (ns) | Host reads in run | Evaluator-alone geomean ratio |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (int i = 0; i < this.modes.size(); i++)
        {
            int ok = 0;
            List<Long> medians = new ArrayList<>();
            List<Long> p90s = new ArrayList<>();
            List<Long> mins = new ArrayList<>();
            List<Long> firsts = new ArrayList<>();
            List<Long> lowers = new ArrayList<>();
            List<Long> runTimes = new ArrayList<>();
            List<Long> alone = new ArrayList<>();
            List<Long> reads = new ArrayList<>();
            double logSum = 0;
            double evaluatorLogSum = 0;
            int ratioCount = 0;
            int evaluatorRatioCount = 0;
            boolean rust = this.modes.get(i).isRust();
            for (Sample[] row : samples.values())
            {
                Sample s = row[i];
                if ((s != null) && "ok".equals(s.status))
                {
                    ok++;
                }
                if (!allOk(row))
                {
                    continue;
                }
                medians.add(s.medianNs);
                p90s.add(s.p90Ns);
                mins.add(s.minNs);
                firsts.add(s.firstNs);
                logSum += Math.log(Math.max(1, s.medianNs) / (double) Math.max(1, row[0].medianNs));
                ratioCount++;
                if (rust)
                {
                    lowers.add(s.lowerNs);
                    runTimes.add(s.runNs);
                    reads.add(s.runReads);
                    if (s.runReads == 0)
                    {
                        alone.add(s.runNs);
                        evaluatorLogSum += Math.log(Math.max(1, s.runNs) / (double) Math.max(1, row[0].medianNs));
                        evaluatorRatioCount++;
                    }
                }
            }
            md.append("| ").append(this.modes.get(i).label).append(" | ").append(ok).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", sum(medians) / 1e6)).append(" | ")
                    .append(micros(median(medians))).append(" | ").append(micros(median(p90s))).append(" | ")
                    .append(micros(median(mins))).append(" | ").append(micros(median(firsts))).append(" | ")
                    .append(ratioCount == 0 ? "n/a" : String.format(Locale.ROOT, "%.2fx", Math.exp(logSum / ratioCount))).append(" | ")
                    .append(rust ? String.valueOf(median(lowers)) : "").append(" | ")
                    .append(rust ? String.valueOf(median(runTimes)) : "").append(" | ")
                    .append(rust ? median(alone) + " (" + alone.size() + " tests)" : "").append(" | ")
                    .append(rust ? String.valueOf(median(reads)) : "").append(" | ")
                    .append(rust && (evaluatorRatioCount > 0) ? String.format(Locale.ROOT, "%.3fx", Math.exp(evaluatorLogSum / evaluatorRatioCount)) : "").append(" |\n");
        }
        md.append("\n## Where Rust loses the most against its host engine (median, warm, microseconds)\n\n| Test |");
        StringBuilder rule = new StringBuilder("|---|");
        for (Mode mode : this.modes)
        {
            md.append(' ').append(mode.label).append(" |");
            rule.append("---|");
        }
        md.append('\n').append(rule).append('\n');
        Map<String, Long> loss = new LinkedHashMap<>();
        for (Map.Entry<String, Sample[]> entry : samples.entrySet())
        {
            Sample[] row = entry.getValue();
            if (allOk(row))
            {
                long total = 0;
                for (int i = 0; i < this.modes.size(); i++)
                {
                    int host = this.modes.get(i).isRust() ? hostModeOf(i) : -1;
                    if (host >= 0)
                    {
                        total += row[i].medianNs - row[host].medianNs;
                    }
                }
                loss.put(entry.getKey(), total);
            }
        }
        loss.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(10).forEach(e ->
        {
            Sample[] row = samples.get(e.getKey());
            md.append("| ").append(e.getKey().substring(e.getKey().lastIndexOf("::") + 2)).append(" |");
            for (Sample s : row)
            {
                md.append(' ').append(micros(s.medianNs)).append(" |");
            }
            md.append('\n');
        });
        return md.toString();
    }

    private static String micros(long nanos)
    {
        return String.format(Locale.ROOT, "%.1f", nanos / 1000.0);
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
        long firstNs;
        long medianNs;
        long p90Ns;
        long minNs;
        long lowerNs;
        long runNs;
        long runReads;
        long lowerReads;
    }
}
