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

package org.finos.legend.pure.m3.execution.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.finos.legend.pure.m3.pct.shared.PCTTools.isPCTTest;

public class TestRunner implements Runnable
{
    private static final Function<TestCollection, Comparable> TEST_COLLECTION_SORT_KEY = new Function<TestCollection, Comparable>()
    {
        @Override
        public Comparable valueOf(TestCollection testCollection)
        {
            return testCollection.getPackage().getName();
        }
    };

    private static final Function<CoreInstance, Comparable> TEST_SORT_KEY = new Function<CoreInstance, Comparable>()
    {
        @Override
        public Comparable valueOf(CoreInstance test)
        {
            return test.getName();
        }
    };

    private final TestCollection tests;
    private final boolean includeAlloyOnlyTests;
    private final String pctAdapter;
    private final FunctionExecution functionExecution;
    private final TestCallBack testCallBack;
    private final boolean shuffle;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    protected UnifiedSet<String> passedTests;
    protected UnifiedSet<String> failedTests;

    public TestRunner(TestCollection tests, FunctionExecution functionExecution, TestCallBack callBack, boolean shuffle)
    {
        this(tests, false, functionExecution, callBack, shuffle);
    }

    public TestRunner(TestCollection tests, boolean includeAlloyOnlyTests, FunctionExecution functionExecution, TestCallBack callBack, boolean shuffle)
    {
        this(tests, includeAlloyOnlyTests, functionExecution, callBack, shuffle, null);
    }

    // TODO: consider refactoring this to use builder pattern
    public TestRunner(TestCollection tests, boolean includeAlloyOnlyTests, FunctionExecution functionExecution, TestCallBack callBack, boolean shuffle, String pctAdapter)
    {
        this.tests = tests;
        this.includeAlloyOnlyTests = includeAlloyOnlyTests;
        this.pctAdapter = pctAdapter == null ? "meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_" : pctAdapter;
        this.functionExecution = functionExecution;
        this.testCallBack = callBack;
        this.shuffle = shuffle;
        callBack.foundTests(this.tests.getAllTestFunctions(includeAlloyOnlyTests));
        this.passedTests = new UnifiedSet<>();
        this.failedTests = new UnifiedSet<>();
    }

    public TestRunner(TestCollection tests, FunctionExecution functionExecution, TestCallBack callBack)
    {
        this(tests, false, functionExecution, callBack, false);
    }

    public TestRunner(TestCollection tests, boolean includeAlloyOnlyTests, FunctionExecution functionExecution, TestCallBack callBack)
    {
        this(tests, includeAlloyOnlyTests, functionExecution, callBack, false);
    }

    public TestRunner(TestCollection tests, boolean includeAlloyOnlyTests, FunctionExecution functionExecution, TestCallBack callBack, String pctAdapter)
    {
        this(tests, includeAlloyOnlyTests, functionExecution, callBack, false, pctAdapter);
    }

    public TestRunner(String path, FunctionExecution functionExecution, ProcessorSupport processorSupport, TestCallBack callBack, boolean shuffle)
    {
        this(TestCollection.collectTests(path, processorSupport, functionExecution.getClass()), functionExecution, callBack, shuffle);
    }

    public TestRunner(String path, FunctionExecution functionExecution, ProcessorSupport processorSupport, TestCallBack callBack)
    {
        this(path, functionExecution, processorSupport, callBack, false);
    }

    public TestRunner(String path, PureRuntime runtime, FunctionExecution functionExecution, TestCallBack callBack, boolean shuffle)
    {
        this(path, functionExecution, runtime.getProcessorSupport(), callBack, shuffle);
    }

    @Override
    public void run()
    {
        try
        {
            Console console = this.functionExecution.getConsole();
            console.setConsole(true);
            runTestsFromCollection(this.tests, console);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void stop()
    {
        this.stopped.set(true);
    }

    public TestCollection getTestCollection()
    {
        return this.tests;
    }

    private void runTestsFromCollection(TestCollection testCollection, Console console) throws IOException
    {
        // Execute before functions
        for (CoreInstance before : testCollection.getBeforeFunctions())
        {
            if (this.stopped.get())
            {
                return;
            }
            try
            {
                executeTestFunc(before, testCollection.getTestFunctionParam(), testCollection.getTestFunctionParamCustomizer());
            }
            catch (Throwable t)
            {
                // One of the set-up functions failed, so we fail all the tests in this collection and sub-collections
                failTestsFromCollectionWithErrorStatus(testCollection, console, new ErrorTestStatus(t));
                return;
            }
        }

        // Execute tests for subcollections
        for (TestCollection subCollection : getSubCollections(testCollection))
        {
            if (this.stopped.get())
            {
                return;
            }
            runTestsFromCollection(subCollection, console);
        }

        // Execute tests
        for (CoreInstance test : getTests(testCollection))
        {
            if (this.stopped.get())
            {
                return;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(stream);
            console.setPrintStream(ps);
            try
            {
                executeTestFunc(test, testCollection.getTestFunctionParam(), testCollection.getTestFunctionParamCustomizer());
                this.testCallBack.executedTest(test, testCollection.getTestParameterizationId(), stream.toString(), TestStatus.SUCCESS);
                this.passedTests.add(PackageableElement.getUserPathForPackageableElement(test) + (testCollection.getTestParameterizationId() == null ? "" : "[" + testCollection.getTestParameterizationId() + "]"));
            }
            catch (Throwable t)
            {
                PureException exception = PureException.findPureException(t);
                if ((exception != null) && (exception instanceof PureAssertFailException))
                {
                    this.testCallBack.executedTest(test, testCollection.getTestParameterizationId(), stream.toString(), new AssertFailTestStatus((PureAssertFailException) exception));
                }
                else
                {
                    this.testCallBack.executedTest(test, testCollection.getTestParameterizationId(), stream.toString(), new ErrorTestStatus(t));
                }
                this.failedTests.add(PackageableElement.getUserPathForPackageableElement(test) + (testCollection.getTestParameterizationId() == null ? "" : "[" + testCollection.getTestParameterizationId() + "]"));
            }
            stream.flush();
        }

        // Execute after functions
        for (CoreInstance after : testCollection.getAfterFunctions())
        {
            if (this.stopped.get())
            {
                return;
            }
            try
            {
                executeTestFunc(after, testCollection.getTestFunctionParam(), testCollection.getTestFunctionParamCustomizer());
            }
            catch (Throwable t)
            {
                // TODO what should we do with this
            }
        }
    }

    private void executeTestFunc(CoreInstance testFunc, Object testFunctionParam, CoreInstance testFunctionParamCustomizer)
    {
        ListIterable<? extends CoreInstance> args = Lists.mutable.empty();

        if (isPCTTest(testFunc, functionExecution.getProcessorSupport()))
        {
            String adapterLocation = this.pctAdapter;
            testFunctionParam = _Package.getByUserPath(adapterLocation, functionExecution.getProcessorSupport());
            if (testFunctionParam == null)
            {
                throw new RuntimeException("The adapter " + adapterLocation + " can't be found in the graph");
            }
        }

        if (testFunctionParam != null)
        {
            ProcessorSupport processorSupport = this.functionExecution.getProcessorSupport();
            if (testFunctionParamCustomizer != null)
            {
                CoreInstance customizedParam = this.functionExecution.start(testFunctionParamCustomizer,
                        Lists.mutable.with(
                                ValueSpecificationBootstrap.wrapValueSpecification(testFunc, false, processorSupport),
                                ValueSpecificationBootstrap.wrapValueSpecification((CoreInstance) testFunctionParam, false, processorSupport)
                        ));
                customizedParam = Instance.getValueForMetaPropertyToOneResolved(customizedParam, M3Properties.values, processorSupport);
                args = Lists.mutable.with(ValueSpecificationBootstrap.wrapValueSpecification(customizedParam, false, processorSupport));
            }
            else
            {
                args = Lists.mutable.with(ValueSpecificationBootstrap.wrapValueSpecification((CoreInstance) testFunctionParam, false, processorSupport));
            }
        }
        this.functionExecution.start(testFunc, args);
    }

    private void failTestsFromCollectionWithErrorStatus(TestCollection testCollection, Console console, ErrorTestStatus status) throws IOException
    {
        // Fail tests for subcollections
        for (TestCollection subCollection : getSubCollections(testCollection))
        {
            if (this.stopped.get())
            {
                return;
            }
            failTestsFromCollectionWithErrorStatus(subCollection, console, status);
        }

        // Fail tests
        for (CoreInstance test : getTests(testCollection))
        {
            if (this.stopped.get())
            {
                return;
            }
            this.testCallBack.executedTest(test, testCollection.getTestParameterizationId(), "", status);
        }
    }

    private ListIterable<TestCollection> getSubCollections(TestCollection testCollection)
    {
        MutableList<TestCollection> subCollections = Lists.mutable.withAll(testCollection.getSubCollections());
        if (this.shuffle)
        {
            Collections.shuffle(subCollections);
        }
        else
        {
            subCollections.sortThisBy(TEST_COLLECTION_SORT_KEY);
        }
        return subCollections;
    }

    private ListIterable<CoreInstance> getTests(TestCollection testCollection)
    {
        MutableList<CoreInstance> testFunctions = Lists.mutable.withAll(testCollection.getTestFunctions(includeAlloyOnlyTests));
        if (this.shuffle)
        {
            Collections.shuffle(testFunctions);
        }
        else
        {
            testFunctions.sortThisBy(TEST_SORT_KEY);
        }
        return testFunctions;
    }
}
