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

package org.finos.legend.pure.m3.execution.test;

import java.net.URI;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

public class PureTestBuilder
{
    public static DynamicContainer buildTestContainer(TestCollection testCollection, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
    {
        return convertTestSuiteToDynamicContainer(buildSuite(testCollection, executor, executionSupport));
    }

    public static DynamicContainer convertTestSuiteToDynamicContainer(TestSuite testSuite)
    {
        Enumeration<Test> tests = testSuite.tests();

        List<DynamicNode> nodes = Lists.mutable.empty();

        while (tests.hasMoreElements())
        {
            Test test = tests.nextElement();

            if (test instanceof TestSuite)
            {
                nodes.add(convertTestSuiteToDynamicContainer((TestSuite) test));
            }
            else
            {
                PureTestCase testCase = (PureTestCase) test;
                SourceInformation sourceInformation = testCase.coreInstance.getSourceInformation();
                URI sourceUri = getSourceUri(sourceInformation);

                nodes.add(DynamicTest.dynamicTest(testCase.getName(), sourceUri, testCase));
            }
        }

        return DynamicContainer.dynamicContainer(testSuite.getName(), nodes);
    }

    private static URI getSourceUri(SourceInformation sourceInformation)
    {
        try
        {
            URL sourceInfoResource = Thread.currentThread().getContextClassLoader().getResource(sourceInformation.getSourceId().substring(1));

            String source;

            if (sourceInfoResource != null && sourceInfoResource.getProtocol().equals("file"))
            {
                source = sourceInfoResource.toString();
            }
            else
            {
                source = "classpath:" + sourceInformation.getSourceId();
            }
            return URI.create(source + "?line=" + sourceInformation.getLine() + "&column=" + sourceInformation.getColumn());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static TestSuite buildSuite(TestCollection testCollection, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = Lists.mutable.empty();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildSuite(collection, executor, executionSupport));
        }
        return buildSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                testCollection.getTestFunctionParam(),
                testCollection.getTestFunctionParamCustomizer(),
                testCollection.getTestParameterizationId(),
                subSuites,
                executor,
                executionSupport
        );
    }

    private static TestSuite buildSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                        RichIterable<CoreInstance> testFunctions, Object param, CoreInstance paramCustomizer, String parameterizationId,
                                        ListIterable<TestSuite> subSuites, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
    {
        TestSuite suite = new TestSuite(packageName + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
        beforeFunctions.collect(fn -> new PureTestCase(fn, param, paramCustomizer, parameterizationId, executor, executionSupport)).each(suite::addTest);
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new PureTestCase(testFunc, param, paramCustomizer, parameterizationId, executor, executionSupport);
            suite.addTest(theTest);
        }
        afterFunctions.collect(fn -> new PureTestCase(fn, param, paramCustomizer, parameterizationId, executor, executionSupport)).each(suite::addTest);
        return suite;
    }

    public static class PureTestCase extends TestCase implements Executable
    {
        CoreInstance coreInstance;
        Object param;
        CoreInstance paramCustomizer;
        ExecutionSupport executionSupport;
        F2<CoreInstance, MutableList<Object>, Object> executor;

        PureTestCase(CoreInstance coreInstance, Object param, CoreInstance paramCustomizer, String parameterizationId, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
        {
            super(PackageableElement.getUserPathForPackageableElement(coreInstance) + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
            this.coreInstance = coreInstance;
            this.param = param;
            this.paramCustomizer = paramCustomizer;
            this.executionSupport = executionSupport;
            this.executor = executor;
        }

        @Override
        public void execute() throws Throwable
        {
            this.runTest();
        }

        @Override
        protected void runTest() throws Throwable
        {
            Object customizedParam = this.param;
            if (this.param != null && this.paramCustomizer != null)
            {
                customizedParam = this.executor.value(this.paramCustomizer, Lists.mutable.with(this.coreInstance, this.param));
            }
            if (customizedParam != null)
            {
                this.executor.value(this.coreInstance, Lists.mutable.with(customizedParam));
            }
            else
            {
                this.executor.value(this.coreInstance, Lists.mutable.empty());
            }
        }
    }

    public static boolean satisfiesConditions(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport) &&
                !hasTaggedValue(node, "meta::pure::profiles::test", "excludePlatform", "Java compiled", processorSupport);
    }

    public static boolean satisfiesConditionsInterpreted(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport);
    }

    public static boolean satisfiesConditionsModular(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !hasStereotype(node, "meta::pure::profiles::test", "ExcludeModular", processorSupport) &&
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

    public interface F2<T1, T2, R>
    {
        R value(T1 var1, T2 var2) throws Throwable;
    }
}
