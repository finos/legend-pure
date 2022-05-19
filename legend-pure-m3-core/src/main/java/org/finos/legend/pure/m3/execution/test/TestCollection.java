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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.execution.ExecutionPlatformRegistry;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;

public class TestCollection
{
    private static final Predicate<? super CoreInstance> DEFAULT_FILTER_PREDICATE = Predicates.alwaysTrue();
    private static final Predicate<TestCollection> HAS_TESTS = new Predicate<TestCollection>()
    {
        @Override
        public boolean accept(TestCollection testCollection)
        {
            return testCollection.hasTests();
        }
    };
    private static final Predicate<TestCollection> HAS_TO_FIXES = new Predicate<TestCollection>()
    {
        @Override
        public boolean accept(TestCollection testCollection)
        {
            return testCollection.hasToFix();
        }
    };
    private static final Predicate<TestCollection> HAS_TEST_CONTENT = new Predicate<TestCollection>()
    {
        @Override
        public boolean accept(TestCollection testCollection)
        {
            return testCollection.hasTestContent();
        }
    };

    private final CoreInstance pkg;
    private String testParameterizationId;
    private Object testFunctionParam;
    private CoreInstance testFunctionParamCustomizer;
    private final Predicate<? super CoreInstance> testFilter;
    private MutableList<CoreInstance> beforeFunctions = Lists.mutable.with();
    private final MutableList<CoreInstance> testFunctions = Lists.mutable.with();
    private MutableList<CoreInstance> afterFunctions = Lists.mutable.with();
    private final MutableList<CoreInstance> toFixFunctions = Lists.mutable.with();
    private final MutableList<CoreInstance> alloyOnlyFunctions = Lists.mutable.with();
    private final MutableList<TestCollection> subCollections = Lists.mutable.with();

    private TestCollection(CoreInstance testPackage, ProcessorSupport processorSupport, Function<CoreInstance, TestCollection> pureTestCollectionGenerator, Predicate<? super CoreInstance> testFilter, boolean getBeforeAfterFromParents)
    {
        this.pkg = testPackage;
        this.testFilter = testFilter;
        findPackageTests(processorSupport, pureTestCollectionGenerator);
        if (getBeforeAfterFromParents)
        {
            findBeforeAfterForParents(testPackage, processorSupport);
        }
        pruneBeforeAfters(processorSupport);
    }

    private TestCollection(CoreInstance testPackage)
    {
        this.pkg = testPackage;
        this.testFilter = DEFAULT_FILTER_PREDICATE;
    }

    /**
     * Return whether the collection has any tests (including
     * tests in sub-collections).
     *
     * @return whether the collection has any tests
     */
    public boolean hasTests()
    {
        return this.testFunctions.notEmpty() || this.subCollections.anySatisfy(HAS_TESTS);
    }

    /**
     * Return whether the collection has any to-fix tests (including
     * those in sub-collections).
     *
     * @return whether the collection has any to-fix tests
     */
    public boolean hasToFix()
    {
        return this.toFixFunctions.notEmpty() || this.subCollections.anySatisfy(HAS_TO_FIXES);
    }

    /**
     * Return whether the collection has any test content (tests
     * to-fix functions, before package functions, after package
     * functions), including those in sub-collections.
     *
     * @return whether the collection has any test content
     */
    public boolean hasTestContent()
    {
        return this.testFunctions.notEmpty() ||
                this.toFixFunctions.notEmpty() ||
                this.beforeFunctions.notEmpty() ||
                this.afterFunctions.notEmpty() ||
                this.alloyOnlyFunctions.notEmpty() ||
                this.subCollections.anySatisfy(HAS_TEST_CONTENT);
    }

    /**
     * Get the total count of tests, including tests in sub-collections.
     *
     * @return test count
     */
    public int getTestCount()
    {
        int count = this.testFunctions.size();
        for (TestCollection subCollection : this.subCollections)
        {
            count += subCollection.getTestCount();
        }
        return count;
    }

    /**
     * Get the total count of to-fix tests, including those in
     * sub-collections.
     *
     * @return to-fix test count
     */
    public int getToFixCount()
    {
        int count = this.toFixFunctions.size();
        for (TestCollection subCollection : this.subCollections)
        {
            count += subCollection.getToFixCount();
        }
        return count;
    }

    /**
     * Get all test functions, including those from sub-collections.
     *
     * @return all test functions
     */
    public MutableList<CoreInstance> getAllTestFunctions()
    {
        return getAllTestFunctions(false);
    }

    public MutableList<CoreInstance> getAllTestFunctions(boolean includeAlloyOnly)
    {
        MutableList<CoreInstance> tests = Lists.mutable.with();
        collectAllTests(tests, includeAlloyOnly);
        return tests;
    }

    public MutableList<Pair<CoreInstance, String>> getAllTestFunctionsWithParameterizations(boolean includeAlloyOnly)
    {
        MutableList<Pair<CoreInstance, String>> tests = Lists.mutable.with();
        collectAllTestsWithParameterizations(tests, includeAlloyOnly);
        return tests;
    }

    /**
     * Get all to-fix functions, including those from sub-collections.
     *
     * @return all to-fix functions
     */
    public MutableList<CoreInstance> getAllToFixFunctions()
    {
        MutableList<CoreInstance> toFixFunctions = Lists.mutable.with();
        collectAllToFix(toFixFunctions);
        return toFixFunctions;
    }

    /**
     * Get the package of this test collection.
     *
     * @return package
     */
    public CoreInstance getPackage()
    {
        return this.pkg;
    }

    /**
     * Get the before package functions for this collection.
     *
     * @return before package functions
     */
    public RichIterable<CoreInstance> getBeforeFunctions()
    {
        return this.beforeFunctions.asUnmodifiable();
    }

    /**
     * Get the test functions for this collection (not including
     * sub-collections).
     *
     * @return test functions
     */
    public RichIterable<CoreInstance> getTestFunctions()
    {
        return this.testFunctions.asUnmodifiable();
    }

    public RichIterable<CoreInstance> getTestFunctions(boolean includeAlloyOnly)
    {
        return includeAlloyOnly
                ? this.getPureAndAlloyOnlyFunctions()
                : this.testFunctions.asUnmodifiable();
    }

    /**
     * Get the to-fix functions for this collection (not
     * including sub-collections).
     *
     * @return to-fix functions
     */
    public RichIterable<CoreInstance> getToFixFunctions()
    {
        return this.toFixFunctions.asUnmodifiable();
    }

    public RichIterable<CoreInstance> getPureAndAlloyOnlyFunctions()
    {
        MutableList result = Lists.mutable.with();
        for (CoreInstance c: this.testFunctions) {result.add(c);}
        for (CoreInstance c: this.alloyOnlyFunctions) {result.add(c);}
        return result.asUnmodifiable();
    }

    /**
     * Get the after package functions for this collection.
     *
     * @return after package functions
     */
    public RichIterable<CoreInstance> getAfterFunctions()
    {
        return this.afterFunctions.asUnmodifiable();
    }

    /**
     * Get all direct sub-collections of this collection.
     *
     * @return sub-collections
     */
    public RichIterable<TestCollection> getSubCollections()
    {
        return this.subCollections.asUnmodifiable();
    }

    /**
     * Get id identifying the parameterization, in case test function params are provided
     *
     * @return test-parameterization-id
     */
    public String getTestParameterizationId()
    {
        return this.testParameterizationId;
    }

    /**
     * Get param to be passed to test functions while executing them
     *
     * @return test-function-param
     */
    public Object getTestFunctionParam()
    {
        return this.testFunctionParam;
    }

    /**
     * Get test function param customizer
     *
     * @return test-function-param-customizer
     */
    public CoreInstance getTestFunctionParamCustomizer()
    {
        return this.testFunctionParamCustomizer;
    }

    private void collectAllTests(MutableList<CoreInstance> tests, boolean includeAlloyOnly)
    {
        tests.addAll(this.testFunctions);
        if (includeAlloyOnly)
        {
            tests.addAll(alloyOnlyFunctions);
        }
        for (TestCollection subCollection : this.subCollections)
        {
            subCollection.collectAllTests(tests, includeAlloyOnly);
        }
    }

    private void collectAllTestsWithParameterizations(MutableList<Pair<CoreInstance, String>> tests, boolean includeAlloyOnly)
    {
        tests.addAll(this.testFunctions.collect(t -> Tuples.pair(t, this.testParameterizationId)));
        if (includeAlloyOnly)
        {
            tests.addAll(this.alloyOnlyFunctions.collect(t -> Tuples.pair(t, this.testParameterizationId)));
        }
        for (TestCollection subCollection : this.subCollections)
        {
            subCollection.collectAllTestsWithParameterizations(tests, includeAlloyOnly);
        }
    }

    private void collectAllToFix(MutableList<CoreInstance> toFixFunctions)
    {
        toFixFunctions.addAll(this.toFixFunctions);
        for (TestCollection subCollection : this.subCollections)
        {
            subCollection.collectAllToFix(toFixFunctions);
        }
    }

    private void findBeforeAfterForParents(CoreInstance node, ProcessorSupport processorSupport)
    {
        if (Instance.getValueForMetaPropertyToOneResolved(node, M3Properties._package, processorSupport) != null)
        {
            MutableList<CoreInstance> packages = PackageableElement.getUserObjectPathForPackageableElement(Instance.getValueForMetaPropertyToOneResolved(node, M3Properties._package, processorSupport));
            for (CoreInstance pkg : packages.toReversed())
            {
                for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(pkg, M3Properties.children, processorSupport))
                {
                    if (Instance.instanceOf(child, M3Paths.FunctionDefinition, processorSupport))
                    {
                        if (TestTools.hasBeforePackageStereotype(child, processorSupport))
                        {
                            this.beforeFunctions.add(child);
                        }
                        if (TestTools.hasAfterPackageStereotype(child, processorSupport))
                        {
                            this.afterFunctions.add(child);
                        }
                    }
                }
            }
            this.beforeFunctions.reverseThis();
        }
    }

    private void pruneBeforeAfters(final ProcessorSupport processorSupport)
    {
        Predicates<CoreInstance> isParentOfAnIncludedTest = new Predicates<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties._package, processorSupport);
                String packageName = PackageableElement.getUserPathForPackageableElement(pkg);
                return !containsTestInPackage(packageName, processorSupport);
            }
        };

        this.beforeFunctions = this.beforeFunctions.reject(isParentOfAnIncludedTest);
        this.afterFunctions = this.afterFunctions.reject(isParentOfAnIncludedTest);
    }

    private boolean containsTestInPackage(final String packageName, final ProcessorSupport processorSupport)
    {
        Predicates<CoreInstance> functionIsInOrBelowPackage = new Predicates<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance testFunction)
            {
                CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(testFunction, M3Properties._package, processorSupport);
                String functionPackageName = PackageableElement.getUserPathForPackageableElement(pkg);
                return functionPackageName.startsWith(packageName);
            }
        };

        Predicate<? super TestCollection> subcollectionContainTestInPackage = new Predicates<TestCollection>()
        {
            @Override
            public boolean accept(TestCollection child)
            {
                return child.containsTestInPackage(packageName, processorSupport);
            }
        };

        return this.testFunctions.anySatisfy(functionIsInOrBelowPackage) || this.alloyOnlyFunctions.anySatisfy(functionIsInOrBelowPackage)
                || this.subCollections.anySatisfy(subcollectionContainTestInPackage);
    }

    private void findPackageTests(ProcessorSupport processorSupport, Function<CoreInstance, TestCollection> pureTestCollectionGenerator)
    {
        for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(this.pkg, M3Properties.children, processorSupport))
        {
            if (Instance.instanceOf(child, M3Paths.FunctionDefinition, processorSupport))
            {
                if (TestTools.hasBeforePackageStereotype(child, processorSupport))
                {
                    this.beforeFunctions.add(child);
                }
                if (TestTools.hasAfterPackageStereotype(child, processorSupport))
                {
                    this.afterFunctions.add(child);
                }
                if (this.testFilter.accept(child))
                {
                    if (TestTools.hasToFixStereotype(child, processorSupport))
                    {
                        this.toFixFunctions.add(child);
                    }
                    else if (TestTools.hasAlloyOnlyStereotype(child, processorSupport))
                    {
                        this.alloyOnlyFunctions.add(child);
                    }
                    else if (TestTools.hasTestStereotype(child, processorSupport))
                    {
                        this.testFunctions.add(child);
                    }
                    else if (TestTools.hasTestCollectionStereotype(child, processorSupport) && pureTestCollectionGenerator != null)
                    {
                        this.subCollections.add(pureTestCollectionGenerator.apply(child));
                    }
                }
            }
            else if (Instance.instanceOf(child, M3Paths.Package, processorSupport))
            {
                TestCollection subCollection = new TestCollection(child, processorSupport, pureTestCollectionGenerator, this.testFilter, false);
                // only add the sub-collection if it has some test content
                if (subCollection.hasTestContent())
                {
                    this.subCollections.add(subCollection);
                }
            }
        }
    }

    public static TestCollection collectTestsFromPure(CoreInstance pureTestCollectionFn, FunctionExecution functionExecution)
    {
        ProcessorSupport processorSupport = functionExecution.getProcessorSupport();
        CoreInstance pureTestCollection = functionExecution.start(pureTestCollectionFn, Lists.mutable.empty());
        pureTestCollection = Instance.getValueForMetaPropertyToOneResolved(pureTestCollection, M3Properties.values, processorSupport);
        return collectTestsFromPureRecursive(pureTestCollection, processorSupport);
    }

    private static TestCollection collectTestsFromPureRecursive(CoreInstance pureTestCollection, ProcessorSupport processorSupport)
    {
        CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(pureTestCollection, TestTools.TEST_COLLECTION_PKG, processorSupport);
        TestCollection testCollection = new TestCollection(pkg);

        CoreInstance testParameterizationId = Instance.getValueForMetaPropertyToOneResolved(pureTestCollection, TestTools.TEST_COLLECTION_TEST_PARAMETERIZATION_ID, processorSupport);
        testCollection.testParameterizationId = testParameterizationId instanceof StringCoreInstance ? ((StringCoreInstance) testParameterizationId).getValue() : null;

        testCollection.testFunctionParam = Instance.getValueForMetaPropertyToOneResolved(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTION_PARAM, processorSupport);
        testCollection.testFunctionParamCustomizer = Instance.getValueForMetaPropertyToOneResolved(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTION_PARAM_CUSTOMIZER, processorSupport);

        for (CoreInstance testFunction: Instance.getValueForMetaPropertyToManyResolved(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTIONS, processorSupport))
        {
            testCollection.testFunctions.add(testFunction);
        }
        for (CoreInstance beforeFunction: Instance.getValueForMetaPropertyToManyResolved(pureTestCollection, TestTools.TEST_COLLECTION_BEFORE_FUNCTIONS, processorSupport))
        {
            testCollection.beforeFunctions.add(beforeFunction);
        }
        for (CoreInstance afterFunction: Instance.getValueForMetaPropertyToManyResolved(pureTestCollection, TestTools.TEST_COLLECTION_AFTER_FUNCTIONS, processorSupport))
        {
            testCollection.afterFunctions.add(afterFunction);
        }
        for (CoreInstance subCollection: Instance.getValueForMetaPropertyToManyResolved(pureTestCollection, TestTools.TEST_COLLECTION_SUB_COLLECTIONS, processorSupport))
        {
            testCollection.subCollections.add(collectTestsFromPureRecursive(subCollection, processorSupport));
        }
        return testCollection;
    }

    public static TestCollection collectTestsFromPure(CoreInstance pureTestCollectionFn, Function<CoreInstance, Object> functionExecution)
    {
        return collectTestsFromPureRecursive(functionExecution.apply(pureTestCollectionFn));
    }

    private static TestCollection collectTestsFromPureRecursive(Object pureTestCollection)
    {
        CoreInstance pkg = (CoreInstance) getField(pureTestCollection, TestTools.TEST_COLLECTION_PKG);
        TestCollection testCollection = new TestCollection(pkg);

        testCollection.testParameterizationId = (String) getField(pureTestCollection, TestTools.TEST_COLLECTION_TEST_PARAMETERIZATION_ID);
        testCollection.testFunctionParam = getField(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTION_PARAM);
        testCollection.testFunctionParamCustomizer = (CoreInstance) getField(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTION_PARAM_CUSTOMIZER);

        for (Object testFunction: (RichIterable) getField(pureTestCollection, TestTools.TEST_COLLECTION_TEST_FUNCTIONS))
        {
            testCollection.testFunctions.add((CoreInstance) testFunction);
        }
        for (Object beforeFunction: (RichIterable) getField(pureTestCollection, TestTools.TEST_COLLECTION_BEFORE_FUNCTIONS))
        {
            testCollection.beforeFunctions.add((CoreInstance) beforeFunction);
        }
        for (Object afterFunction: (RichIterable) getField(pureTestCollection, TestTools.TEST_COLLECTION_AFTER_FUNCTIONS))
        {
            testCollection.afterFunctions.add((CoreInstance) afterFunction);
        }
        for (Object subCollection: (RichIterable) getField(pureTestCollection, TestTools.TEST_COLLECTION_SUB_COLLECTIONS))
        {
            testCollection.subCollections.add(collectTestsFromPureRecursive(subCollection));
        }
        return testCollection;
    }

    private static Object getField(Object obj, String fieldName)
    {
        try
        {
            return obj.getClass().getMethod("_" + fieldName).invoke(obj);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Collect tests from the given package and all sub-packages.  If testFilter
     * is provided, it is used to select which tests to include in the collection.
     * If not provided, all tests within the relevant packages are included.
     *
     * @param pkg        test package
     * @param testFilter filter for tests
     * @return test collection
     */
    public static TestCollection collectTests(CoreInstance pkg, ProcessorSupport processorSupport, Function<CoreInstance, TestCollection> pureTestCollectionGenerator, Predicate<? super CoreInstance> testFilter)
    {
        if (pkg == null)
        {
            throw new NullPointerException("Test package cannot be null");
        }
        if (!Instance.instanceOf(pkg, M3Paths.Package, processorSupport))
        {
            throw new IllegalArgumentException("Initial node must be a package, got: " + pkg);
        }
        if (testFilter == null)
        {
            testFilter = DEFAULT_FILTER_PREDICATE;
        }
        return new TestCollection(pkg, processorSupport, pureTestCollectionGenerator, testFilter, true);
    }

    /**
     * Collect tests from the given package and all sub-packages.  If testFilter
     * is provided, it is used to select which tests to include in the collection.
     * If not provided, all tests within the relevant packages are included.
     *
     * @param pkg        test package
     * @param testFilter filter for tests
     * @return test collection
     */
    public static TestCollection collectTests(CoreInstance pkg, ProcessorSupport processorSupport, Predicate<? super CoreInstance> testFilter)
    {
        return collectTests(pkg, processorSupport, null, testFilter);
    }

    /**
     * Collect tests from the given package and all sub-packages.  Only tests
     * appropriate for the given execution platform are included.
     *
     * @param pkg                    test package
     * @param executionPlatformClass execution platform class
     * @return test collection
     */
    public static TestCollection collectTests(CoreInstance pkg, ProcessorSupport processorSupport, Class<? extends FunctionExecution> executionPlatformClass)
    {
        return collectTests(pkg, processorSupport, getFilterPredicateForExecutionPlatformClass(executionPlatformClass, processorSupport));
    }

    /**
     * Collect tests from the given package and all sub-packages.  Only tests
     * appropriate for the named execution platform are included.
     *
     * @param pkg                   test package
     * @param executionPlatformName execution platform name
     * @return test collection
     */
    public static TestCollection collectTests(CoreInstance pkg, ProcessorSupport processorSupport, String executionPlatformName)
    {
        return collectTests(pkg, processorSupport, getFilterPredicateForExecutionPlatform(executionPlatformName, processorSupport));
    }

    /**
     * Collect tests from the given package and all sub-packages.
     *
     * @param pkg     test package
     * @return test collection
     */
    public static TestCollection collectTests(CoreInstance pkg, ProcessorSupport processorSupport)
    {
        return collectTests(pkg, processorSupport, DEFAULT_FILTER_PREDICATE);
    }

    /**
     * Collect tests from the given package and all sub-packages.  If testFilter
     * is provided, it is used to select which tests to include in the collection.
     * If not provided, all tests within the relevant packages are included.
     *
     * @param pkgPath    test package path
     * @param testFilter filter for tests
     * @return test collection
     */
    public static TestCollection collectTests(String pkgPath, ProcessorSupport processorSupport, Function<CoreInstance, TestCollection> pureTestCollectionGenerator, Predicate<? super CoreInstance> testFilter)
    {
        return collectTests(processorSupport.package_getByUserPath(pkgPath), processorSupport, pureTestCollectionGenerator, testFilter);
    }

    /**
     * Collect tests from the given package and all sub-packages.  If testFilter
     * is provided, it is used to select which tests to include in the collection.
     * If not provided, all tests within the relevant packages are included.
     *
     * @param pkgPath    test package path
     * @param testFilter filter for tests
     * @return test collection
     */
    public static TestCollection collectTests(String pkgPath, ProcessorSupport processorSupport, Predicate<? super CoreInstance> testFilter)
    {
        return collectTests(processorSupport.package_getByUserPath(pkgPath), processorSupport, testFilter);
    }

    /**
     * Collect tests from the given package and all sub-packages.  Only tests
     * appropriate for the given execution platform are included.
     *
     * @param path                   test package path
     * @param executionPlatformClass execution platform class
     * @return test collection
     */
    public static TestCollection collectTests(String path, ProcessorSupport processorSupport, Class<? extends FunctionExecution> executionPlatformClass)
    {
        return collectTests(path, processorSupport, getFilterPredicateForExecutionPlatformClass(executionPlatformClass, processorSupport));
    }

    /**
     * Collect tests from the given package and all sub-packages.  Only tests
     * appropriate for the given execution platform are included.
     *
     * @param path                   test package path
     * @param executionPlatformClass execution platform class
     * @param extraPredicate         extra predicate
     * @return test collection
     */
    public static TestCollection collectTests(String path, ProcessorSupport processorSupport, final Class<? extends FunctionExecution> executionPlatformClass, final Predicate<CoreInstance> extraPredicate)
    {
        final Predicate<CoreInstance> basePredicate = getFilterPredicateForExecutionPlatformClass(executionPlatformClass, processorSupport);
        return collectTests(path, processorSupport, new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return basePredicate.accept(coreInstance) && extraPredicate.accept(coreInstance);
            }
        });
    }

    /**
     * Collect tests from the given package and all sub-packages.  Only tests
     * appropriate for the named execution platform are included.
     *
     * @param path                  test package path
     * @param executionPlatformName execution platform name
     * @return test collection
     */
    public static TestCollection collectTests(String path, ProcessorSupport processorSupport, String executionPlatformName)
    {
        return collectTests(path, processorSupport, getFilterPredicateForExecutionPlatform(executionPlatformName, processorSupport));
    }

    /**
     * Collect tests from the given package and all sub-packages.
     *
     * @param pkgPath    test package path
     * @return test collection
     */
    public static TestCollection collectTests(String pkgPath, ProcessorSupport processorSupport)
    {
        return collectTests(pkgPath, processorSupport, DEFAULT_FILTER_PREDICATE);
    }

    /**
     * Return a predicate which accepts tests which are not excluded for the
     * given execution platform.
     *
     * @param executionPlatformClass execution platform class
     * @return execution platform filter predicate
     */
    public static Predicate<CoreInstance> getFilterPredicateForExecutionPlatformClass(Class<?> executionPlatformClass, ProcessorSupport processorSupport)
    {
        if (!ExecutionPlatformRegistry.isExecutionPlatformClass(executionPlatformClass))
        {
            throw new IllegalArgumentException("Not an execution platform: " + executionPlatformClass);
        }
        String name = ExecutionPlatformRegistry.getExecutionPlatformName(executionPlatformClass);
        if (name == null)
        {
            throw new RuntimeException("Execution platform has no name: " + executionPlatformClass);
        }
        return getFilterPredicateForExecutionPlatform(name, processorSupport);
    }

    /**
     * Return a predicate which accepts tests which are not excluded for the
     * named execution platform.
     *
     * @param executionPlatformName execution platform name
     * @return execution platform filter predicate
     */
    private static Predicate<CoreInstance> getFilterPredicateForExecutionPlatform(final String executionPlatformName, final ProcessorSupport processorSupport)
    {
        if (executionPlatformName == null)
        {
            throw new IllegalArgumentException("Execution platform name cannot be null");
        }
        return new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance function)
            {
                return !TestTools.hasPlatformExclusionTaggedValue(function, executionPlatformName, processorSupport);
            }
        };
    }

    public static Predicate<? super CoreInstance> getFilterPredicateForAlloyExclusion(final ProcessorSupport processorSupport)
    {
        return (Predicate<? super CoreInstance>)(System.getProperty("alloy.test.server.host") != null || System.getProperty("legend.test.server.host") != null ? new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return !TestTools.hasExcludeAlloyStereotype(coreInstance, processorSupport);
            }
        } : DEFAULT_FILTER_PREDICATE);
    }

    public static Predicate<? super CoreInstance> getFilterPredicateForAlloyTextModeExclusion(final ProcessorSupport processorSupport)
    {
        return (Predicate<? super CoreInstance>)("text".equals(System.getProperty("legend.test.serializationKind")) &&
                (System.getProperty("alloy.test.server.host") != null || System.getProperty("legend.test.server.host") != null) ? new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return  !TestTools.hasExcludeAlloyTextModeStereotype(coreInstance, processorSupport);
            }
        } : DEFAULT_FILTER_PREDICATE);
    }
}
