// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.testHelper;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PrintPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class PureTestBuilderCompiled extends TestSuite
{
    public static TestSuite buildSuite(TestCollection testCollection, ExecutionSupport executionSupport)
    {
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> p = (a, b) -> executeFn(a, "meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_", Maps.mutable.empty(), executionSupport, b);
        return PureTestBuilder.buildSuite(testCollection, p, executionSupport);
    }

    public static TestSuite buildSuite(boolean eagerMetadata, String... paths)
    {
        return buildSuite(eagerMetadata, Arrays.asList(paths));
    }

    public static TestSuite buildSuite(String... paths)
    {
        return buildSuite(false, paths);
    }

    private static TestSuite buildSuite(boolean eagerMetadata, Iterable<String> paths)
    {
        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport(eagerMetadata);
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> p = (a, b) -> executeFn(a, null, Maps.fixedSize.empty(), executionSupport, b);
        TestSuite suite = new TestSuite();
        paths.forEach((path) ->
        {
            TestCollection col = TestCollection.collectTests(path, executionSupport.getProcessorSupport(),
                    ci -> !PCTTools.isPCTTest(ci, executionSupport.getProcessorSupport()) && PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport()));
            suite.addTest(PureTestBuilder.buildSuite(col, p, executionSupport));
        });
        return suite;
    }

    public static TestSuite buildPCTTestSuite(TestCollection collection, MutableMap<String, String> exclusions, String executor, CompiledExecutionSupport executionSupport)
    {
        TestCollection.validateExclusions(collection, exclusions);
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) -> executeFn(a, executor, exclusions, executionSupport, b);
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilder.buildSuite(collection, testExecutor, executionSupport));
        return suite;
    }

    public static TestSuite buildPCTTestSuite(ReportScope reportScope, MutableList<ExclusionSpecification> expectedFailures, Adapter adapter)
    {
        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader());
        MutableMap<String, String> explodedExpectedFailures = PCTReportConfiguration.explodeExpectedFailures(expectedFailures, executionSupport.getProcessorSupport());

        return PureTestBuilderCompiled.buildPCTTestSuite(
                TestCollection.buildPCTTestCollection(reportScope._package, reportScope.filePath, executionSupport.getProcessorSupport()),
                explodedExpectedFailures,
                adapter.function,
                executionSupport
        );
    }


    public static Object executeFn(CoreInstance coreInstance, String PCTExecutor, MutableMap<String, String> exclusions, ExecutionSupport executionSupport, MutableList<Object> paramList) throws Throwable
    {
        Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(coreInstance.getSourceInformation()));
        if (PCTTools.isPCTTest(coreInstance, ((CompiledExecutionSupport) executionSupport).getProcessorSupport()))
        {
            CoreInstance executor = _Package.getByUserPath(PCTExecutor, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
            if (executor == null)
            {
                throw new RuntimeException(PCTExecutor + "can't be found in the graph");
            }
            paramList.add(executor);
        }
        paramList = paramList.with(executionSupport);
        Object[] params = paramList.toArray();
        String methodName = FunctionProcessor.functionNameToJava(coreInstance);
        Method method = params.length == 1 ? _class.getMethod(methodName, ExecutionSupport.class)
                                           : ArrayIterate.detect(_class.getMethods(), m -> methodName.equals(m.getName()));

        // NOTE: mock out the global tracer for test
        // See https://github.com/opentracing/opentracing-java/issues/170
        // See https://github.com/opentracing/opentracing-java/issues/364
        GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
        try
        {
            Object res = method.invoke(null, params);
            // Ensure we didn't expect an error
            String message = exclusions.get(PackageableElement.getUserPathForPackageableElement(coreInstance, "::"));
            if (message != null)
            {
                PCTTools.displayExpectedErrorFailMessage(message, coreInstance, PCTExecutor);
            }
            return res;
        }
        catch (InvocationTargetException e)
        {
            // Check if the error was expected
            String message = exclusions.get(PackageableElement.getUserPathForPackageableElement(coreInstance, "::"));
            Throwable cause = e.getCause();
            Throwable thrown = cause.getMessage() != null && cause.getMessage().contains("Unexpected error executing function with params") && cause.getCause() != null ? cause.getCause() : cause;
            if (message != null && PCTTools.getMessageFromError(thrown).contains(message))
            {
                return null;
            }
            else
            {
                PCTTools.displayErrorMessage(message, coreInstance, PCTExecutor, ((CompiledExecutionSupport) executionSupport).getProcessorSupport(), thrown);
                if (thrown instanceof PureAssertFailException)
                {
                    Assert.fail(thrown.getMessage());
                }
                throw thrown;
            }
        }
        finally
        {
            try
            {
                // HACK since GlobalTracer api doesnt provide a way to reset the tracer which is needed for testing
                Field tracerField = GlobalTracer.get().getClass().getDeclaredField("isRegistered");
                tracerField.setAccessible(true);
                tracerField.set(GlobalTracer.get(), false);
                Assert.assertFalse(GlobalTracer.isRegistered());
            }
            catch (Exception ignored)
            {
            }
        }
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
        return getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader());
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport(ClassLoader classLoader)
    {
        return getClassLoaderExecutionSupport(classLoader, false);
    }

    private static CompiledExecutionSupport getClassLoaderExecutionSupport(boolean eagerMetadata)
    {
        return getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader(), eagerMetadata);
    }

    private static CompiledExecutionSupport getClassLoaderExecutionSupport(ClassLoader classLoader, boolean eagerMetadata)
    {
        MutableList<CodeRepository> codeRepos = CodeRepositoryProviderHelper.findCodeRepositories(classLoader)
                .reject(r -> "test_generic_repository".equals(r.getName()) || "other_test_generic_repository".equals(r.getName()), Lists.mutable.empty());
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(classLoader, codeRepos));
        Metadata metadata = buildMetadata(classLoader, codeStorage, eagerMetadata);
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, metadata, Sets.mutable.empty()),
                null,
                codeStorage,
                null,
                null,
                new ConsoleCompiled(),
                null,
                Sets.mutable.empty(),
                CompiledExtensionLoader.extensions()
        );
    }

    private static Metadata buildMetadata(ClassLoader classLoader, CompositeCodeStorage codeStorage, boolean eager)
    {
        if (eager)
        {
            PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                    .withCache(new ClassLoaderPureGraphCache(classLoader))
                    .withFactoryRegistryOverride(JavaModelFactoryRegistryLoader.loader())
                    .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                    .buildAndInitialize();
            if (!runtime.isInitialized())
            {
                StringBuilder builder = new StringBuilder("Pure runtime not initialized");
                CacheState cacheState = runtime.getCache().getCacheState();
                if (cacheState.getLastErrorMessage() != null)
                {
                    builder.append(": ").append(cacheState.getLastErrorMessage());
                }
                if (cacheState.getLastStackTrace() != null)
                {
                    builder.append('\n').append(cacheState.getLastStackTrace());
                }
                throw new RuntimeException(builder.toString());
            }
            return MetadataBuilder.indexAll(runtime.getModelRepository().getTopLevels(), IdBuilder.newIdBuilder(runtime.getProcessorSupport()), runtime.getProcessorSupport());
        }
        return MetadataLazy.fromClassLoader(classLoader, codeStorage.getAllRepositories().asLazy().collect(CodeRepository::getName));
    }

    public static MutableSet<CoreInstance> buildExclusionList(ProcessorSupport processorSupport, String... exclusions)
    {
        return ArrayIterate.collect(exclusions, e -> Objects.requireNonNull(processorSupport.package_getByUserPath(e), e), Sets.mutable.ofInitialCapacity(exclusions.length));
    }

    public static TestCollection generatePureTestCollection(CoreInstance fn, ExecutionSupport executionSupport)
    {
        return TestCollection.collectTestsFromPure(fn, f ->
        {
            try
            {
                return executeFn(f, null, Maps.mutable.empty(), executionSupport, Lists.mutable.empty());
            }
            catch (Error | RuntimeException e)
            {
                throw e;
            }
            catch (Throwable e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}
