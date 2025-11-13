// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import java.nio.file.Paths;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import static org.finos.legend.pure.m3.pct.shared.PCTTools.isPCTTest;
import static org.junit.Assert.fail;

public class PureTestBuilderInterpreted
{
    public static TestSuite buildSuite(String... all)
    {
        FunctionExecutionInterpreted functionExecution = getFunctionExecutionInterpreted();

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = getTestExecutor(functionExecution, Maps.mutable.empty(), null);

        TestSuite suite = new TestSuite();
        ArrayIterate.forEach(all, (path) ->
                {
                    TestCollection col = TestCollection.collectTests(path, functionExecution.getProcessorSupport(), ci -> !isPCTTest(ci, functionExecution.getProcessorSupport()) && PureTestBuilder.satisfiesConditionsInterpreted(ci, functionExecution.getProcessorSupport()));
                    suite.addTest(PureTestBuilder.buildSuite(col, testExecutor, new ExecutionSupport()));
                }
        );
        return suite;
    }

    public static TestSuite buildPCTTestSuite(ReportScope reportScope, MutableList<ExclusionSpecification> expectedFailures, Adapter adapter)
    {
        FunctionExecutionInterpreted functionExecutionInterpreted = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();
        MutableMap<String, String> explodedExpectedFailures = PCTReportConfiguration.explodeExpectedFailures(expectedFailures, functionExecutionInterpreted.getProcessorSupport());
        return PureTestBuilderInterpreted.buildPCTTestSuite(
                TestCollection.buildPCTTestCollection(reportScope._package, reportScope.filePath, functionExecutionInterpreted.getProcessorSupport()),
                explodedExpectedFailures,
                adapter.function,
                functionExecutionInterpreted
        );
    }

    public static TestSuite buildPCTTestSuite(TestCollection testCollection, MutableMap<String, String> expectedFailures, String adapter, FunctionExecutionInterpreted functionExecution)
    {
        TestCollection.validateExclusions(testCollection, expectedFailures);
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = getTestExecutor(functionExecution, expectedFailures, adapter);
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilder.buildSuite(testCollection, testExecutor, new ExecutionSupport()));
        return suite;
    }

    private static PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> getTestExecutor(FunctionExecutionInterpreted functionExecution, MutableMap<String, String> expectedFailures, String PCTExecutor)
    {
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) ->
        {
            MutableList<CoreInstance> params = Lists.mutable.empty();
            if (isPCTTest(a, functionExecution.getProcessorSupport()))
            {
                CoreInstance adapter = _Package.getByUserPath(PCTExecutor, functionExecution.getProcessorSupport());
                if (adapter == null)
                {
                    throw new RuntimeException("The adapter " + PCTExecutor + " can't be found in the graph");
                }
                params.add(ValueSpecificationBootstrap.wrapValueSpecification(adapter, true, functionExecution.getProcessorSupport()));
            }
            try
            {
                Object ret = functionExecution.start(a, params);

                String message = expectedFailures.get(PackageableElement.getUserPathForPackageableElement(a, "::"));
                if (message != null)
                {
                    PCTTools.displayExpectedErrorFailMessage(message, a, PCTExecutor);
                }

                return ret;
            }
            catch (Exception e)
            {
                // Check if the error was expected
                String message = expectedFailures.get(PackageableElement.getUserPathForPackageableElement(a, "::"));
                if (message != null && PCTTools.getMessageFromError(e).contains(message))
                {
                    return null;
                }
                else
                {
                    PCTTools.displayErrorMessage(message, a, PCTExecutor, functionExecution.getProcessorSupport(), e);
                    if (e.getCause() instanceof PureAssertFailException)
                    {
                        fail(e.getCause().getMessage());
                    }
                    if (e.getCause() != null)
                    {
                        throw e.getCause();
                    }
                    else
                    {
                        throw e;
                    }
                }
            }
        };
        return testExecutor;
    }

    public static FunctionExecutionInterpreted getFunctionExecutionInterpreted()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> codeRepositories = builder.build().getRepositories();
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(codeRepositories));

        FunctionExecutionInterpreted functionExecution;

        if (System.getProperty("functionExecutionInterpretedCoverageDirectory") != null)
        {
            functionExecution = new FunctionExecutionInterpretedWithCodeCoverage(Paths.get(System.getProperty("functionExecutionInterpretedCoverageDirectory")));
        }
        else
        {
            functionExecution = new FunctionExecutionInterpreted();
        }

        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).build();
        Message message = new Message("");
        functionExecution.init(runtime, message);
        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(codeRepositories.select(c -> c.getName() != null && (c.getName().startsWith("platform") || c.getName().startsWith("core"))).collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
        GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
        loader.loadAll(message);
        return functionExecution;
    }

}
