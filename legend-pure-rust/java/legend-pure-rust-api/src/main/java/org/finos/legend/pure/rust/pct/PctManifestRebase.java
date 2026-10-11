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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regenerates a Rust adapter's manifest from a run of the surveyor: the
 * manifests are never edited by hand. Each transport module invokes it from
 * a test class of its own, one call per engine and scope.
 *
 * <p>The surveyor runs every PCT test of the scope under the committed
 * manifest, exactly as the suite does, and the new manifest is derived from
 * its report: a FAIL or ERROR becomes an exclusion recording the first line
 * of its message; a PASS that was an expected failure keeps its committed
 * entry; a test that was excluded and now passes is dropped; a SKIP stays
 * out. The file is written under the directory named by the system property
 * {@link #MANIFEST_DIR_PROPERTY}, which the calling module's surefire
 * configuration sets to its test resources.
 */
public final class PctManifestRebase
{
    /**
     * System property naming the test resources directory the manifests are written under.
     */
    public static final String MANIFEST_DIR_PROPERTY = "legend.pct.rust.manifestDir";

    private static final String SURVEYOR = "meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_";
    private static final String EXPECTED_FAILURE = "Expected failure: ";
    private static final String NOW_PASSES = " was expected to fail but now PASSES";
    private static final String MISMATCH = "PCT expected-failure mismatch for ";
    private static final String ACTUAL = "  actual   : ";

    private PctManifestRebase()
    {
    }

    /**
     * Rebase the manifest of a scope on the interpreted engine.
     *
     * @param adapter      the Rust adapter
     * @param scope        the corpus
     * @param manifestPath the classpath path of the manifest, also its path under the manifest directory
     * @throws IOException when the manifest cannot be written
     */
    public static void interpreted(Adapter adapter, ReportScope scope, String manifestPath) throws IOException
    {
        FunctionExecutionInterpreted fe = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();
        ProcessorSupport ps = fe.getProcessorSupport();
        ModelRepository repository = fe.getPureRuntime().getModelRepository();
        CoreInstance surveyor = _Package.getByUserPath(SURVEYOR, ps);
        Assert.assertNotNull(SURVEYOR, surveyor);

        CoreInstance report = fe.start(surveyor, Lists.mutable.with(
                ValueSpecificationBootstrap.newStringLiteral(repository, scope._package, ps),
                ValueSpecificationBootstrap.newStringLiteral(repository, scope.filePath, ps),
                ValueSpecificationBootstrap.newStringLiteral(repository, manifestPath, ps)));
        if (Instance.instanceOf(report, M3Paths.InstanceValue, ps))
        {
            report = Instance.getValueForMetaPropertyToOneResolved(report, "values", ps);
        }
        derive(adapter, scope, manifestPath, "", report, ps);
    }

    /**
     * Rebase the manifest of a scope on the compiled engine.
     *
     * @param adapter      the Rust adapter
     * @param scope        the corpus
     * @param manifestPath the classpath path of the manifest, also its path under the manifest directory
     * @throws Throwable when the surveyor or the write fails
     */
    public static void compiled(Adapter adapter, ReportScope scope, String manifestPath) throws Throwable
    {
        CompiledExecutionSupport es = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        ProcessorSupport ps = es.getProcessorSupport();
        CoreInstance surveyor = ps.package_getByUserPath(SURVEYOR);
        Assert.assertNotNull(SURVEYOR, surveyor);
        CoreInstance report = (CoreInstance) PureTestBuilderCompiled.executeFn(surveyor, null, Maps.mutable.empty(), es,
                Lists.mutable.with(scope._package, scope.filePath, manifestPath));
        derive(adapter, scope, manifestPath, " (compiled)", report, ps);
    }

    private static void derive(Adapter adapter, ReportScope scope, String manifestPath, String platformTag, CoreInstance report, ProcessorSupport ps) throws IOException
    {
        String manifestDir = System.getProperty(MANIFEST_DIR_PROPERTY);
        Assert.assertNotNull("system property " + MANIFEST_DIR_PROPERTY + " (set by the module's surefire configuration)", manifestDir);

        PCTManifest committed = PCTManifestLoader.loadFromClasspath(manifestPath);
        MutableMap<String, String> committedExclusions = committed.toExclusionMap();

        TreeMap<String, String> exclusions = new TreeMap<>();
        int passes = 0;
        int skipped = 0;
        ListIterable<? extends CoreInstance> results = Instance.getValueForMetaPropertyToManyResolved(report, "results", ps);
        for (CoreInstance result : results)
        {
            String fqn = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(result, "fqn", ps));
            String status = Instance.getValueForMetaPropertyToOneResolved(result, "status", ps).getName();
            CoreInstance messageNode = Instance.getValueForMetaPropertyToOneResolved(result, "message", ps);
            String message = (messageNode == null) ? "" : PrimitiveUtilities.getStringValue(messageNode);
            switch (status)
            {
                case "PASS":
                {
                    if (message.startsWith(EXPECTED_FAILURE))
                    {
                        String expectedError = committedExclusions.get(fqn);
                        Assert.assertNotNull("expected failure without a committed exclusion: " + fqn, expectedError);
                        Assert.assertFalse("empty expectedError committed for " + fqn + "; delete the entry and rebase", expectedError.trim().isEmpty());
                        exclusions.put(fqn, expectedError);
                    }
                    else
                    {
                        passes++;
                    }
                    break;
                }
                case "FAIL":
                case "ERROR":
                {
                    if (message.contains(NOW_PASSES))
                    {
                        passes++;
                    }
                    else
                    {
                        exclusions.put(fqn, actualError(message));
                    }
                    break;
                }
                case "SKIP":
                {
                    skipped++;
                    break;
                }
                default:
                {
                    throw new IllegalStateException("unknown test status " + status + " for " + fqn);
                }
            }
        }

        Path target = Paths.get(manifestDir, manifestPath);
        write(target, committed.adapter, exclusions);
        System.out.println(adapter.name + " " + scope.module + platformTag + ": " + passes + " / " + (results.size() - skipped)
                + (skipped == 0 ? "" : " (" + skipped + " skipped)") + " -> " + target);
    }

    /**
     * The first line of the error the surveyor reports: the {@code actual}
     * line of its expected-failure mismatch message, else the message's
     * first non-blank line.
     */
    private static String actualError(String message)
    {
        if (message.startsWith(MISMATCH))
        {
            for (String line : message.split("\n"))
            {
                if (line.startsWith(ACTUAL))
                {
                    return line.substring(ACTUAL.length());
                }
            }
        }
        return firstLine(message);
    }

    /**
     * The first non-blank line of a message.
     */
    private static String firstLine(String message)
    {
        for (String line : message.split("\n"))
        {
            if (!line.trim().isEmpty())
            {
                return line;
            }
        }
        return message;
    }

    private static void write(Path target, String adapter, Map<String, String> exclusions) throws IOException
    {
        Files.createDirectories(target.getParent());
        try (JsonGenerator generator = new JsonFactory().createGenerator(target.toFile(), JsonEncoding.UTF8))
        {
            generator.setPrettyPrinter(new ManifestPrettyPrinter());
            generator.writeStartObject();
            generator.writeStringField("adapter", adapter);
            generator.writeArrayFieldStart("exclusions");
            for (Map.Entry<String, String> exclusion : exclusions.entrySet())
            {
                generator.writeStartObject();
                generator.writeStringField("test", exclusion.getKey());
                generator.writeStringField("expectedError", exclusion.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeRaw('\n');
        }
    }

    /**
     * Two-space indentation for objects and arrays, {@code ": "} between a
     * field and its value: the layout of the platform's committed manifests.
     */
    private static final class ManifestPrettyPrinter extends DefaultPrettyPrinter
    {
        private ManifestPrettyPrinter()
        {
            DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
            indentObjectsWith(indenter);
            indentArraysWith(indenter);
            this._objectFieldValueSeparatorWithSpaces = ": ";
        }

        @Override
        public DefaultPrettyPrinter createInstance()
        {
            return new ManifestPrettyPrinter();
        }
    }
}
