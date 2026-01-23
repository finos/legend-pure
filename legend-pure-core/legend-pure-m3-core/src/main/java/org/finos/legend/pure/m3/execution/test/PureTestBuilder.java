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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Ignore;

import java.util.Comparator;

public class PureTestBuilder
{
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
        TestSuite suite = new TestSuite();
        suite.setName(packageName + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
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

    @Ignore
    public static class PureTestCase extends TestCase
    {
        CoreInstance coreInstance;
        Object param;
        CoreInstance paramCustomizer;
        ExecutionSupport executionSupport;
        F2<CoreInstance, MutableList<Object>, Object> executor;

        public PureTestCase()
        {
        }

        PureTestCase(CoreInstance coreInstance, Object param, CoreInstance paramCustomizer, String parameterizationId, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
        {
            super(coreInstance.getValueForMetaPropertyToOne("functionName").getName() + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
            this.coreInstance = coreInstance;
            this.param = param;
            this.paramCustomizer = paramCustomizer;
            this.executionSupport = executionSupport;
            this.executor = executor;
        }

        public CoreInstance getCoreInstance()
        {
            return coreInstance;
        }

        public ExecutionSupport getExecutionSupport()
        {
            return executionSupport;
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
        return !Profile.hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !Profile.hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport) &&
                !Profile.hasTaggedValue(node, "meta::pure::profiles::test", "excludePlatform", "Java compiled", processorSupport);
    }

    public static boolean satisfiesConditionsInterpreted(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !Profile.hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !Profile.hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport);
    }

    public static boolean satisfiesConditionsModular(CoreInstance node, ProcessorSupport processorSupport, String... exclusions)
    {
        return !Profile.hasStereotype(node, "meta::pure::profiles::test", "AlloyOnly", processorSupport) &&
                !Profile.hasStereotype(node, "meta::pure::profiles::test", "ExcludeModular", processorSupport) &&
                !Profile.hasStereotype(node, "meta::pure::profiles::temporaryLazyExclusion", "exclude", processorSupport) &&
                !Profile.hasTaggedValue(node, "meta::pure::profiles::test", "excludePlatform", "Java compiled", processorSupport);
    }

    public interface F2<T1, T2, R>
    {
        R value(T1 var1, T2 var2) throws Throwable;
    }
}
