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

package org.finos.legend.pure.code.core.compiled.test;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Ignore;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Objects;
import java.util.ServiceLoader;

public class PureTestBuilderHelper extends TestSuite
{
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
                return executeFn(f, executionSupport);
            }
            catch (Throwable e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public static TestSuite buildSuite(TestCollection testCollection, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = Lists.mutable.empty();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildSuite(collection, executionSupport));
        }
        return buildSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                testCollection.getTestFunctionParam(),
                testCollection.getTestFunctionParamCustomizer(),
                testCollection.getTestParameterizationId(),
                subSuites,
                executionSupport
        );
    }

    private static TestSuite buildSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                        RichIterable<CoreInstance> testFunctions, Object param, CoreInstance paramCustomizer, String parameterizationId,
                                        org.eclipse.collections.api.list.ListIterable<TestSuite> subSuites, ExecutionSupport executionSupport)
    {
        TestSuite suite = new TestSuite();
        suite.setName(packageName + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
        beforeFunctions.collect(fn -> new PureTestCase(fn, param, paramCustomizer, parameterizationId, executionSupport)).each(suite::addTest);
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new PureTestCase(testFunc, param, paramCustomizer, parameterizationId, executionSupport);
            suite.addTest(theTest);
        }
        afterFunctions.collect(fn -> new PureTestCase(fn, param, paramCustomizer, parameterizationId, executionSupport)).each(suite::addTest);
        return suite;
    }

    @Ignore
    public static class PureTestCase extends TestCase
    {
        CoreInstance coreInstance;
        Object param;
        CoreInstance paramCustomizer;
        ExecutionSupport executionSupport;

        public PureTestCase()
        {
        }

        PureTestCase(CoreInstance coreInstance, Object param, CoreInstance paramCustomizer, String parameterizationId, ExecutionSupport executionSupport)
        {
            super(coreInstance.getValueForMetaPropertyToOne("functionName").getName() + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
            this.coreInstance = coreInstance;
            this.param = param;
            this.paramCustomizer = paramCustomizer;
            this.executionSupport = executionSupport;
        }

        @Override
        protected void runTest() throws Throwable
        {
            Object customizedParam = this.param;
            if (this.param != null && this.paramCustomizer != null)
            {
                customizedParam = executeFn(this.paramCustomizer, this.coreInstance, this.param, this.executionSupport);
            }
            if (customizedParam != null)
            {
                executeFn(this.coreInstance, customizedParam, this.executionSupport);
            }
            else
            {
                executeFn(this.coreInstance, this.executionSupport);
            }
        }
    }

    public static Object executeFn(CoreInstance coreInstance, Object... params) throws Throwable
    {
        Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(coreInstance.getSourceInformation()));
        String methodName = FunctionProcessor.functionNameToJava(coreInstance);
        Method method = params.length == 1 ? _class.getMethod(methodName, ExecutionSupport.class)
                : ArrayIterate.detect(_class.getMethods(), m -> methodName.equals(m.getName()));
        // NOTE: mock out the global tracer for test
        // See https://github.com/opentracing/opentracing-java/issues/170
        // See https://github.com/opentracing/opentracing-java/issues/364
        GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
        try
        {
            return method.invoke(null, params);
        }
        catch (InvocationTargetException e)
        {
            throw e.getCause();
        }
    }

    public static boolean satisfiesConditions(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport) &&
                !hasTaggedValue(node, "meta::pure::profiles::test", "excludePlatform", "Java compiled", processorSupport);
    }

    private static boolean hasStereotype(CoreInstance node, String profile, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        if (stereotypes.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath(profile);
            for (CoreInstance stereotype : stereotypes)
            {
                if ((testProfile == Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.profile, processorSupport)) &&
                        value.equals(Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.value, processorSupport).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTaggedValue(CoreInstance node, String profile, String tag, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance profileObject = processorSupport.package_getByUserPath(profile);
            for (CoreInstance taggedValue : taggedValues)
            {
                if ((taggedValue.getValueForMetaPropertyToOne("tag").getValueForMetaPropertyToOne("profile") == profileObject)
                        && (taggedValue.getValueForMetaPropertyToOne("tag").getName().equals(tag)))
                {
                    String foundValue = Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName();
                    if (value.equals(foundValue))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
        RichIterable<CodeRepository> filtered = CodeRepositoryProviderHelper.findCodeRepositories().select(r -> !r.getName().equals("test_generic_repository") && !r.getName().equals("other_test_generic_repository"));
        MutableList<CodeRepository> codeRepos = Lists.mutable.of(CodeRepository.newPlatformCodeRepository()).withAll(filtered);
        ClassLoader classLoader = PureTestBuilderHelper.class.getClassLoader();
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader, filtered.collect(CodeRepository::getName)), Sets.mutable.empty()),
                        null,
                        new PureCodeStorage(null, new ClassLoaderCodeStorage(classLoader, codeRepos)),
                        null,
                        null,
                        new ConsoleCompiled(),
                        new FunctionCache(),
                        new ClassCache(classLoader),
                        null,
                        Sets.mutable.empty()
                );
    }
}