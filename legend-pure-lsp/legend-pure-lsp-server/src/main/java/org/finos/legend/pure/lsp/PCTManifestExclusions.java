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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

/**
 * The expected failures of one legend/executeTests run: for each PCT test, the exclusion recorded in
 * the manifest that the session's &lt;&lt;PCT.manifestResolver&gt;&gt; function picks for the test's
 * source and the run's adapter - the same manifest a surefire PCT suite names explicitly. The
 * platform defines no resolver: the code base that owns the manifests supplies one, as a function
 * (filter:String[1], adapter:Function&lt;Any&gt;[1]):String[1] that fails when it can't tell. Tests
 * whose manifest can't be identified or loaded run without expected failures, and why is reported as
 * a warning rather than failing the run.
 */
final class PCTManifestExclusions
{
    static final PCTManifestExclusions NONE = new PCTManifestExclusions(Collections.emptyMap(), Collections.emptyList());

    private final Map<String, String> expectedErrors;
    private final List<String> warnings;

    private PCTManifestExclusions(Map<String, String> expectedErrors, List<String> warnings)
    {
        this.expectedErrors = expectedErrors;
        this.warnings = warnings;
    }

    /**
     * @return the error the test is expected to fail with, or null if it is expected to pass
     */
    String expectedError(String testPath)
    {
        return this.expectedErrors.get(testPath);
    }

    List<String> getWarnings()
    {
        return this.warnings;
    }

    /**
     * Caller must hold the graph read lock.
     */
    static PCTManifestExclusions discover(PureRuntime runtime, TestDiscovery.Node root, CoreInstance adapter)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        Map<String, List<String>> testsBySource = new LinkedHashMap<>();
        collectPCTTestsBySource(root, processorSupport, testsBySource);
        if (testsBySource.isEmpty())
        {
            return NONE;
        }

        ListIterable<? extends CoreInstance> resolvers = findResolvers(processorSupport);
        if (resolvers.size() != 1)
        {
            return new PCTManifestExclusions(Collections.emptyMap(), Collections.singletonList(resolvers.isEmpty()
                    ? "No PCT expected failures applied: no function in this session carries <<PCT.manifestResolver>>"
                    : "No PCT expected failures applied: several functions carry <<PCT.manifestResolver>> - "
                    + resolvers.collect(PackageableElement::getUserPathForPackageableElement).makeString(", ")));
        }

        String adapterPath = PackageableElement.getUserPathForPackageableElement(adapter);
        Map<String, String> expectedErrors = new HashMap<>();
        Set<String> warnings = new LinkedHashSet<>();
        Map<String, Map<String, String>> manifests = new HashMap<>();
        FunctionExecutionInterpreted execution = LegendPureSession.initializeFunctionExecution(
                new FunctionExecutionInterpreted(), runtime, new Message(""));
        for (Map.Entry<String, List<String>> source : testsBySource.entrySet())
        {
            String manifestPath;
            try
            {
                manifestPath = resolveManifestPath(resolvers.getFirst(), execution, runtime, source.getKey(), adapter);
            }
            catch (Exception e)
            {
                warnings.add("No PCT expected failures applied to " + source.getKey() + ": " + failureMessage(e));
                continue;
            }

            Map<String, String> manifest = manifests.computeIfAbsent(manifestPath, path -> loadExclusions(runtime, path, adapterPath, warnings));
            for (String test : source.getValue())
            {
                String expectedError = manifest.get(test);
                if (expectedError != null)
                {
                    expectedErrors.put(test, expectedError);
                }
            }
        }
        return new PCTManifestExclusions(expectedErrors, new ArrayList<>(warnings));
    }

    /**
     * The failure's own message, as the surveyor's executePCTTest natives report it.
     */
    static String failureInfo(Exception e)
    {
        if (e instanceof PureAssertFailException)
        {
            return ((PureAssertFailException) e).getInfo();
        }
        return PCTTools.getMessageFromError(PCTTools.unwrapExecutionError(e));
    }

    /**
     * Whether a failure is the one its manifest expects. Like the compiled runner, the full message
     * is matched as well as the info: manifests recorded from compiled runs quote an assertion's info,
     * as in {@code Assert failure at (...), "<info>"}.
     */
    static boolean isExpectedFailure(String expectedError, String failureInfo, String failureMessage)
    {
        return !PCTTools.isGenericExecutionErrorMessage(expectedError)
                && ((failureInfo != null && failureInfo.contains(expectedError)) || (failureMessage != null && failureMessage.contains(expectedError)));
    }

    private static void collectPCTTestsBySource(TestDiscovery.Node node, ProcessorSupport processorSupport, Map<String, List<String>> testsBySource)
    {
        for (TestDiscovery.Invocation test : node.getTests())
        {
            CoreInstance function = test.getFunction();
            if (PCTTools.isPCTTest(function, processorSupport) && function.getSourceInformation() != null)
            {
                testsBySource.computeIfAbsent(function.getSourceInformation().getSourceId(), source -> new ArrayList<>())
                        .add(PackageableElement.getUserPathForPackageableElement(function));
            }
        }
        for (TestDiscovery.Node child : node.getChildren())
        {
            collectPCTTestsBySource(child, processorSupport, testsBySource);
        }
    }

    private static ListIterable<? extends CoreInstance> findResolvers(ProcessorSupport processorSupport)
    {
        CoreInstance stereotype = Profile.findStereotype(PCTTools.PCT_PROFILE, "manifestResolver", processorSupport);
        return stereotype == null
                ? Lists.immutable.<CoreInstance>empty()
                : Instance.getValueForMetaPropertyToManyResolved(stereotype, M3Properties.modelElements, processorSupport);
    }

    private static String resolveManifestPath(CoreInstance resolver, FunctionExecutionInterpreted execution, PureRuntime runtime, String sourceId, CoreInstance adapter)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        ModelRepositoryTransaction transaction = runtime.getModelRepository().newTransaction(false);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            CoreInstance result = execution.start(resolver, Lists.immutable.with(
                    ValueSpecificationBootstrap.wrapValueSpecification(runtime.getModelRepository().newStringCoreInstance(sourceId), true, processorSupport),
                    ValueSpecificationBootstrap.wrapValueSpecification(adapter, true, processorSupport)));
            return PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, processorSupport));
        }
        finally
        {
            transaction.rollback();
        }
    }

    private static Map<String, String> loadExclusions(PureRuntime runtime, String manifestPath, String adapterPath, Set<String> warnings)
    {
        PCTManifest manifest;
        try (InputStream content = runtime.getCodeStorage().getContent(manifestPath))
        {
            if (content == null)
            {
                warnings.add("No PCT expected failures applied: manifest " + manifestPath + " not found");
                return Collections.emptyMap();
            }
            manifest = PCTManifestLoader.loadFromStream(content, manifestPath);
        }
        catch (Exception e)
        {
            warnings.add("No PCT expected failures applied: could not load manifest " + manifestPath + ": " + e.getMessage());
            return Collections.emptyMap();
        }
        if (!adapterPath.equals(manifest.adapter))
        {
            warnings.add("No PCT expected failures applied: manifest " + manifestPath + " is for adapter " + manifest.adapter + ", not " + adapterPath);
            return Collections.emptyMap();
        }
        return manifest.toExclusionMap();
    }

    private static String failureMessage(Exception e)
    {
        PureException pureException = PureException.findPureException(e);
        return pureException == null ? String.valueOf(e.getMessage()) : pureException.getOriginatingPureException().getInfo();
    }
}
