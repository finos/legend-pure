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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Discovers what a legend/executeTests run should execute, as a tree of {@link Node}s mirroring the
 * Pure package hierarchy. Delegates to {@link TestCollection} - the same walker the Maven/JUnit side
 * uses - so before/after-package hook scoping and inheritance stay in lockstep with a surefire run
 * instead of being re-derived here. File scope reuses the same walk with a source-id filter added;
 * {@link TestTools#findNearestBeforePackageFunction} is deliberately not used here since it stops at
 * the first hook up the chain, which would silently drop inherited hooks in a batch run.
 */
public class TestDiscovery
{
    private TestDiscovery()
    {
    }

    /**
     * One function to run, with the {@code String[1]} literals to invoke it with and a display name.
     */
    public static final class Invocation
    {
        private final CoreInstance function;
        private final List<String> arguments;
        private final String label;

        public Invocation(CoreInstance function, List<String> arguments, String label)
        {
            this.function = function;
            this.arguments = arguments == null ? Collections.emptyList() : arguments;
            this.label = label;
        }

        public static Invocation of(CoreInstance function)
        {
            return new Invocation(function, Collections.emptyList(), null);
        }

        public CoreInstance getFunction()
        {
            return this.function;
        }

        public List<String> getArguments()
        {
            return this.arguments;
        }

        /** Null when the function's own name identifies the entry. */
        public String getLabel()
        {
            return this.label;
        }
    }

    /**
     * One package node of the run. {@code children}/{@code tests} are sorted to match
     * {@code PureTestBuilder#buildSuite}'s ordering, so a run here matches an equivalent surefire run.
     */
    public static final class Node
    {
        private final String packagePath;
        private final List<CoreInstance> beforeFunctions;
        private final List<CoreInstance> afterFunctions;
        private final List<Invocation> tests;
        private final List<CoreInstance> skipped;
        private final List<Node> children;

        Node(String packagePath, List<CoreInstance> beforeFunctions, List<CoreInstance> afterFunctions,
             List<Invocation> tests, List<CoreInstance> skipped, List<Node> children)
        {
            this.packagePath = packagePath;
            this.beforeFunctions = beforeFunctions;
            this.afterFunctions = afterFunctions;
            this.tests = tests;
            this.skipped = skipped;
            this.children = children;
        }

        public String getPackagePath()
        {
            return this.packagePath;
        }

        public List<CoreInstance> getBeforeFunctions()
        {
            return this.beforeFunctions;
        }

        public List<CoreInstance> getAfterFunctions()
        {
            return this.afterFunctions;
        }

        public List<Invocation> getTests()
        {
            return this.tests;
        }

        /**
         * {@code <<test.ToFix>>} functions, which TestCollection keeps out of the executable set.
         * Reported as skipped rather than dropped so they stay visible in a client's test tree.
         */
        public List<CoreInstance> getSkipped()
        {
            return this.skipped;
        }

        public List<Node> getChildren()
        {
            return this.children;
        }

        /** Total entries this node and its subtree will report, hooks included. */
        public int totalEntries()
        {
            int count = this.beforeFunctions.size() + this.afterFunctions.size()
                    + this.tests.size() + this.skipped.size();
            for (Node child : this.children)
            {
                count += child.totalEntries();
            }
            return count;
        }

        public boolean isEmpty()
        {
            return totalEntries() == 0;
        }
    }

    /** A node holding an explicit, already-resolved set of invocations - no discovery, no hooks. */
    public static Node flat(String label, List<Invocation> invocations)
    {
        return new Node(label, Collections.emptyList(), Collections.emptyList(), invocations,
                Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Discovers every test under {@code packagePath}. When {@code recursive} is false only the
     * package's own tests are kept, though its hooks (including inherited ones) still apply.
     *
     * @throws IllegalArgumentException if the package does not exist in the compiled graph
     */
    public static Node forPackage(PureRuntime runtime, String packagePath, boolean recursive,
                                  boolean includeVanilla, boolean includePct)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        TestCollection collection = collect(processorSupport, packagePath,
                candidate -> matchesKind(candidate, processorSupport, includeVanilla, includePct));
        return toNode(collection, processorSupport, recursive);
    }

    /**
     * Discovers every test defined in {@code sourceId}. Rooted at the deepest common ancestor package
     * of the file's tests, so inherited hooks are still picked up but the traversal stays bounded.
     */
    public static Node forSource(PureRuntime runtime, String sourceId, boolean includeVanilla, boolean includePct)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return emptyNode(sourceId);
        }
        ListIterable<? extends CoreInstance> instances = source.getNewInstances();
        if (instances == null || instances.isEmpty())
        {
            return emptyNode(sourceId);
        }

        Set<String> packages = new LinkedHashSet<>();
        for (CoreInstance instance : instances)
        {
            if (matchesKind(instance, processorSupport, includeVanilla, includePct)
                    || TestTools.hasToFixStereotype(instance, processorSupport))
            {
                CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(
                        instance, M3Properties._package, processorSupport);
                if (pkg != null)
                {
                    packages.add(PackageableElement.getUserPathForPackageableElement(pkg));
                }
            }
        }
        if (packages.isEmpty())
        {
            return emptyNode(sourceId);
        }

        String root = commonAncestorPackage(packages);
        TestCollection collection = collect(processorSupport, root,
                candidate -> inSource(candidate, sourceId)
                        && matchesKind(candidate, processorSupport, includeVanilla, includePct));
        return toNode(collection, processorSupport, true);
    }

    private static TestCollection collect(ProcessorSupport processorSupport, String packagePath,
                                          Predicate<CoreInstance> filter)
    {
        try
        {
            return TestCollection.collectTests(packagePath, processorSupport, filter);
        }
        catch (RuntimeException e)
        {
            // collectTests raises a bare RuntimeException("Cannot find package: x") for an unknown
            // package; translate it so the handler can report it as a bad request rather than an
            // internal error.
            throw new IllegalArgumentException("Cannot find package '" + packagePath + "' in compiled sources", e);
        }
    }

    /**
     * The vanilla/PCT narrowing, plus {@link PureTestBuilder#satisfiesConditionsInterpreted} so a
     * scoped run's test set matches what a Maven interpreted run of the same package would produce.
     * ToFix classification is left to TestCollection itself, which routes it to the skipped list.
     */
    private static boolean matchesKind(CoreInstance candidate, ProcessorSupport processorSupport,
                                       boolean includeVanilla, boolean includePct)
    {
        if (!PureTestBuilder.satisfiesConditionsInterpreted(candidate, processorSupport))
        {
            return false;
        }
        if (includeVanilla && includePct)
        {
            return true;
        }
        boolean isPct = PCTTools.isPCTTest(candidate, processorSupport);
        return isPct ? includePct : includeVanilla;
    }

    private static boolean inSource(CoreInstance candidate, String sourceId)
    {
        SourceInformation si = candidate.getSourceInformation();
        return si != null && sourceId.equals(si.getSourceId());
    }

    /**
     * Deepest package that is a prefix of every given package path, e.g. {a::b::c, a::b::d} -> a::b.
     * Falls back to the single path when there is only one.
     */
    static String commonAncestorPackage(Set<String> packagePaths)
    {
        List<String> prefix = null;
        for (String path : packagePaths)
        {
            String[] parts = path.split("::");
            if (prefix == null)
            {
                prefix = new ArrayList<>(Arrays.asList(parts));
                continue;
            }
            int limit = Math.min(prefix.size(), parts.length);
            int shared = 0;
            while (shared < limit && prefix.get(shared).equals(parts[shared]))
            {
                shared++;
            }
            prefix = new ArrayList<>(prefix.subList(0, shared));
        }
        if (prefix == null || prefix.isEmpty())
        {
            return "::";
        }
        return String.join("::", prefix);
    }

    private static Node toNode(TestCollection collection, ProcessorSupport processorSupport, boolean recursive)
    {
        List<Node> children = new ArrayList<>();
        if (recursive)
        {
            List<TestCollection> subCollections = new ArrayList<>(collection.getSubCollections().toList());
            subCollections.sort(Comparator.comparing(c -> c.getPackage().getName()));
            for (TestCollection sub : subCollections)
            {
                Node child = toNode(sub, processorSupport, true);
                if (!child.isEmpty())
                {
                    children.add(child);
                }
            }
        }

        // getPureAndAlloyOnlyFunctions(), not getTestFunctions() - this is the set PureTestBuilder
        // puts in the suite.
        List<CoreInstance> testFunctions = new ArrayList<>(collection.getPureAndAlloyOnlyFunctions().toList());
        testFunctions.sort(Comparator.comparing(CoreInstance::getName));
        List<Invocation> tests = new ArrayList<>(testFunctions.size());
        for (CoreInstance test : testFunctions)
        {
            tests.add(Invocation.of(test));
        }

        List<CoreInstance> skipped = new ArrayList<>(collection.getToFixFunctions().toList());
        skipped.sort(Comparator.comparing(CoreInstance::getName));

        return new Node(
                PackageableElement.getUserPathForPackageableElement(collection.getPackage()),
                new ArrayList<>(collection.getBeforeFunctions().toList()),
                new ArrayList<>(collection.getAfterFunctions().toList()),
                tests,
                skipped,
                children);
    }

    private static Node emptyNode(String label)
    {
        return new Node(label, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
}
