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

package org.finos.legend.pure.m3.tests.elements.function.functionMatching;

import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMatching extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
        runtime.delete("fromString2.pure");
    }

    @Test
    public void testSimpleMatching()
    {
        this.runtime.createInMemorySource("fromString.pure","function func(v:String[1]):Integer[1]\n" +
                "{\n" +
                "    $v->length();\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    func('wasp');\n" +
                "}\n");
        this.runtime.compile();

        CoreInstance func = this.runtime.getCoreInstance("func_String_1__Integer_1_");
        Assert.assertNotNull(func);

        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(1, expressions.size());

        assertFunctionExpressionFunction(func, expressions.get(0));
    }

    @Test
    public void testMatchingWithMultipleMatches()
    {
        this.runtime.createInMemorySource("fromString.pure", "function func(v:String[1]):Integer[1]\n" +
                "{\n" +
                "    $v->length();\n" +
                "}\n" +
                "function func(v:Any[1]):Integer[1]\n" +
                "{\n" +
                "    0;\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    func('wasp');\n" +
                "    func(10);\n" +
                "}\n");
        this.runtime.compile();

        CoreInstance func1 = this.runtime.getCoreInstance("func_String_1__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance func2 = this.runtime.getCoreInstance("func_Any_1__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(2, expressions.size());

        assertFunctionExpressionFunction(func1, expressions.get(0));
        assertFunctionExpressionFunction(func2, expressions.get(1));
    }

    @Test
    public void testMatchingWithDisjointMultiplicities()
    {
        this.runtime.createInMemorySource("fromString.pure","function func(v:String[1]):Integer[1]\n" +
                "{\n" +
                "    $v->length();\n" +
                "}\n" +
                "function func(v:String[2..*]):Integer[1]\n" +
                "{\n" +
                "    $v->at(1)->length();\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    func('wasp');\n" +
                "    func(['wasp', 'bee', 'hornet']);\n" +
                "}\n");
        this.runtime.compile();

        CoreInstance func1 = this.runtime.getCoreInstance("func_String_1__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance func2 = this.runtime.getCoreInstance("func_String_$2_MANY$__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(2, expressions.size());

        assertFunctionExpressionFunction(func1, expressions.get(0));
        assertFunctionExpressionFunction(func2, expressions.get(1));
    }

    @Test
    public void testMatchingWithOverlappingMultiplicities()
    {
        this.runtime.createInMemorySource("fromString.pure", "function func(v:String[1]):Integer[1]\n" +
                "{\n" +
                "    $v->length();\n" +
                "}\n" +
                "function func(v:String[*]):Integer[1]\n" +
                "{\n" +
                "    $v->at(1)->length();\n" +
                "}\n" +
                "" +
                "function test():Any[*]\n" +
                "{\n" +
                "    func('wasp');\n" +
                "    func(['wasp', 'bee', 'hornet']);\n" +
                "}\n");
        this.runtime.compile();

        CoreInstance func1 = this.runtime.getCoreInstance("func_String_1__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance func2 = this.runtime.getCoreInstance("func_String_MANY__Integer_1_");
        Assert.assertNotNull(func1);

        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(2, expressions.size());

        assertFunctionExpressionFunction(func1, expressions.get(0));
        assertFunctionExpressionFunction(func2, expressions.get(1));
    }

    @Test
    public void testMatchingWithEmptySetsAndTypeParameters()
    {
        compileTestSource("fromString.pure","function func(v:Pair<String,String>[*]):Integer[1]\n" +
                "{\n" +
                "    1;\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    func([]);" +
                "}\n");
        CoreInstance func = this.runtime.getCoreInstance("func_Pair_MANY__Integer_1_");
        Assert.assertNotNull(func);

        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(1, expressions.size());

        assertFunctionExpressionFunction(func, expressions.get(0));
    }

    @Test
    public void testMatchingErrorMultiplicity()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function func(v:String[1]):Nil[0]\n" +
                            "{\n" +
                            "    [];\n" +
                            "}\n" +
                            "function testCollectRelationshipFromManyToMany():Nil[0]\n" +
                            "{\n" +
                            "    func(['test','ok']);\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "func(_:String[2])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tfunc(String[1]):Nil[0]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "fromString.pure", 7, 5, 7, 5, 7, 8, e);
        }
    }

    @Test
    public void testMatchingWithNonMatchingTypeParameters()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function func(v:Pair<String,String>[1]):Integer[1]\n" +
                            "{\n" +
                            "    1;\n" +
                            "}\n" +
                            "function test():Integer[1]\n" +
                            "{\n" +
                            "    func(^Pair<String,Integer>(first='hello', second=5));\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "func(_:Pair<String, Integer>[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tfunc(Pair[1]):Integer[1]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "fromString.pure", 7, 5, 7, 5, 7, 8, e);
        }
    }

    @Test
    public void testMatchingWithNotEnoughTypeParameters()
    {
        try
        {
            this.runtime.createInMemorySource("fromString.pure",
                    "function func(c:Class<Any>[1]):Integer[1]\n" +
                            "{\n" +
                            "    $c.name->toOne()->length();\n" +
                            "}\n" +
                            "function test():Integer[1]\n" +
                            "{\n" +
                            "    func(Pair->cast(@Class));\n" +
                            "}\n");
            this.runtime.compile();

            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error finding match for function 'func': Type argument mismatch for Class<T>; got: Class", "fromString.pure", 7, 5, 7, 5, 7, 8, e);
        }
    }

    @Test
    public void testMatchingMap()
    {
        CoreInstance mapOne = this.runtime.getCoreInstance("meta::pure::functions::collection::map_T_m__Function_1__V_m_");
        CoreInstance mapMany = this.runtime.getCoreInstance("meta::pure::functions::collection::map_T_MANY__Function_1__V_MANY_");
        Assert.assertNotNull(mapOne);
        Assert.assertNotNull(mapMany);
        Assert.assertNotEquals(mapOne, mapMany);

        compileTestSource("fromString.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    [1, 2, 3, 4]->map(i | $i * 2);\n" +
                        "    [1, 2, 3, 4]->map(i | [$i, $i, $i]);\n" +
                        "}");
        CoreInstance testFn = this.runtime.getCoreInstance("test__Any_MANY_");
        Assert.assertNotNull(testFn);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(2, expressions.size());

        assertFunctionExpressionFunction(mapOne, expressions.get(0));
        assertFunctionExpressionFunction(mapMany, expressions.get(1));
    }

    @Test
    public void testMatchingWithFullyQualifiedReference()
    {
        this.runtime.createInMemorySource("fromString.pure",
                "import test::pkg2::*;\n" +
                        "\n" +
                        "function test::pkg1::splitPackageName(packageName:String[1]):String[*]\n" +
                        "{\n" +
                        "  $packageName->split('::')\n" +
                        "}\n" +
                        "\n" +
                        "function test::pkg2::splitPackageName(packageName:String[1], separator:String[1]):String[*]\n" +
                        "{\n" +
                        "  $packageName->split($separator)\n" +
                        "}\n" +
                        "\n" +
                        "function test::pkg2::splitPackageName(packageName:String[1]):String[*]\n" +
                        "{\n" +
                        "  splitPackageName($packageName, '::')\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  test::pkg1::splitPackageName('meta::pure::functions');" +
                        "}");
        this.runtime.compile();

        CoreInstance splitPackageNameFn_pkg1 = this.runtime.getFunction("test::pkg1::splitPackageName(String[1]):String[*]");
        CoreInstance splitPackageNameFn_pkg2 = this.runtime.getFunction("test::pkg2::splitPackageName(String[1]):String[*]");
        CoreInstance testFn = this.runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(splitPackageNameFn_pkg1);
        Assert.assertNotNull(splitPackageNameFn_pkg2);
        Assert.assertNotNull(testFn);
        Assert.assertNotEquals(splitPackageNameFn_pkg1, splitPackageNameFn_pkg2);

        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(testFn, M3Properties.expressionSequence, this.processorSupport);
        assertFunctionExpressionFunction(splitPackageNameFn_pkg1, expressions.get(0));
    }

    @Test
    public void testInvalidMatchWithFullyQualifiedReference()
    {
        try
        {
            this.runtime.createInMemorySource("fromString.pure",
                    "import test::pkg2::*;\n" +
                            "\n" +
                            "function test::pkg1::splitPackageName(packageName:String[1]):String[*]\n" +
                            "{\n" +
                            "  $packageName->split('::')\n" +
                            "}\n" +
                            "\n" +
                            "function test::pkg2::splitPackageName(packageName:String[1], separator:String[1]):String[*]\n" +
                            "{\n" +
                            "  $packageName->split($separator)\n" +
                            "}\n" +
                            "\n" +
                            "function test::pkg2::splitPackageName(packageName:String[1]):String[*]\n" +
                            "{\n" +
                            "  splitPackageName($packageName, '::')\n" +
                            "}\n" +
                            "\n" +
                            "function test::testFn():Any[*]\n" +
                            "{\n" +
                            "  test::pkg3::splitPackageName('meta::pure::functions');" +
                            "}");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "test::pkg3::splitPackageName(_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\ttest::pkg2::splitPackageName(String[1]):String[*]\n" +
                    "\ttest::pkg2::splitPackageName(String[1], String[1]):String[*]\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\ttest::pkg1::splitPackageName(String[1]):String[*]\n", "fromString.pure", 20, 15, 20, 15, 20, 30, e);
        }
    }

    @Test
    public void testTooManyMatches()
    {
        compileTestSource("fromString.pure",
                "function test::pkg1::func(i:Integer[1]):Integer[1]\n" +
                        "{\n" +
                        "  $i * 2\n" +
                        "}\n" +
                        "\n" +
                        "function test::pkg2::func(i:Integer[1]):Integer[1]\n" +
                        "{\n" +
                        "  $i * 3\n" +
                        "}");
        try
        {
            compileTestSource("fromString2.pure",
                    "import test::pkg1::*;\n" +
                            "import test::pkg2::*;\n" +
                            "function test::pkg3::testFn():Any[*]\n" +
                            "{\n" +
                            "  func(5)\n" +
                            "}\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Too many matches for func(_:Integer[1]):\n" +
                    "\ttest::pkg1::func(Integer[1]):Integer[1]\n" +
                    "\ttest::pkg2::func(Integer[1]):Integer[1]", "fromString2.pure", 5, 3, 5, 3, 5, 6, e);
        }
    }

    @Test
    public void testFunctionExpressionFunctionMatching() throws Exception
    {
        runtime.createInMemorySource("fromString.pure",
                "function myAdd(a:String[1], b:String[1]):String[1]\n" +
                        "{\n" +
                        "   'aa';\n" +
                        "}\n" +
                        "\n" +
                        "function func(a:Any[*]):Nil[0]\n" +
                        "{\n" +
                        "   print(myAdd('b','c'),2);\n" +
                        "   print('z',1);\n" +
                        "}");
        runtime.compile();
        this.repository.validate(new VoidM4StateListener());
    }

    protected void assertFunctionExpressionFunction(CoreInstance expectedFunction, CoreInstance functionExpression)
    {
        CoreInstance func = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, this.processorSupport);
        Assert.assertSame(expectedFunction, func);
    }
}
