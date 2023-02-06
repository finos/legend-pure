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

package org.finos.legend.pure.runtime.java.compiled.modeling.valueSpec;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNilTypeCompiled extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testNilCastToIntegerReturnValueMany() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testFn1():Integer[*]\n" +
                "{\n" +
                "    []\n" +
                "}\n");

        this.assertNilType(this.execute("test::testFn1():Integer[*]"));
    }

    @Test
    public void testNilCastToIntegerReturnValueZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testFn2():Integer[0..1]\n" +
                "{\n" +
                "    []\n" +
                "}\n");

        this.assertNilType(this.execute("test::testFn2():Integer[0..1]"));
    }

    @Test
    public void testNilVariableCastToStringReturnValueMany() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testFn3():String[*]\n" +
                "{\n" +
                "    let x = [];\n" +
                "    $x;\n" +
                "}\n");

        this.assertNilType(this.execute("test::testFn3():String[*]"));
    }

    @Test
    public void testNilVariableCastToStringReturnValueZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testFn4():String[0..1]\n" +
                "{\n" +
                "    let x = [];\n" +
                "    $x;\n" +
                "}\n");

        this.assertNilType(this.execute("test::testFn4():String[0..1]"));
    }

    @Test
    public void testNilAsStringParamMany() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper1(strings:String[*]):String[1]\n" +
                "{\n" +
                "    joinStrings($strings, '')\n" +
                "}\n" +
                "\n" +
                "function test::testFn5():String[1]\n" +
                "{\n" +
                "    test::testHelper1([])\n" +
                "}\n");

        this.assertValue("", this.execute("test::testFn5():String[1]"));
    }

    @Test
    public void testNilVariableAsIntegerParamMany() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper2(nums:Integer[*]):Integer[1]\n" +
                "{\n" +
                "    plus($nums)\n" +
                "}\n" +
                "\n" +
                "function test::testFn6():Integer[1]\n" +
                "{\n" +
                "    let x = [];\n" +
                "    test::testHelper2($x);\n" +
                "}\n");

        this.assertValue(0L, this.execute("test::testFn6():Integer[1]"));
    }

    @Test
    public void testNilAsPairParamZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper3(y:Pair<Integer,Any>[0..1]):Integer[1]\n" +
                "{\n" +
                "    if($y->isEmpty(), |7, |$y->toOne().first)\n" +
                "}\n" +
                "\n" +
                "function test::testFn7():Integer[1]\n" +
                "{\n" +
                "    test::testHelper3([])\n" +
                "}\n");

        this.assertValue(7L, this.execute("test::testFn7():Integer[1]"));
    }

    @Test
    public void testNilVariableAsPairParamZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper3(y:Pair<Integer,Any>[0..1]):Integer[1]\n" +
                "{\n" +
                "    if($y->isEmpty(), |7, |$y->toOne().first)\n" +
                "}\n" +
                "\n" +
                "function test::testFn8():Integer[1]\n" +
                "{\n" +
                "    let x = [];\n" +
                "    test::testHelper3($x);\n" +
                "}\n");

        this.assertValue(7L, this.execute("test::testFn8():Integer[1]"));
    }

    @Test
    public void testNilVariableAsParam() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper1(strings:String[*]):String[1]\n" +
                "{\n" +
                "    joinStrings($strings, '')\n" +
                "}\n" +
                "\n" +
                "function test::testHelper2(nums:Integer[*]):Integer[1]\n" +
                "{\n" +
                "    plus($nums)\n" +
                "}\n" +
                "" +
                "function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1]\n" +
                "{\n" +
                "   ^Pair<U,V>(first=$first, second=$second);\n" +
                "}\n" +
                "function test::testFn9():Pair<Number,String>[1]\n" +
                "{\n" +
                "    let x = [];\n" +
                "    let y = test::testHelper2($x);\n" +
                "    let z = test::testHelper1($x);\n" +
                "    pair($y, $z);\n" +
                "}\n");
        Pair resultPair = (Pair) Iterate.getFirst(((InstanceValue) this.execute("test::testFn9():Pair[1]"))._values());
        Assert.assertEquals(0L, resultPair._first());
        Assert.assertEquals("", resultPair._second());
    }

    @Test
    public void testNilTypeAsReturnValue() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testHelper4(strings:String[*]): Nil[0]\n" +
                "{\n" +
                "   [];\n" +
                "}\n" +
                "\n" +
                "function test::testFn10():String[0..1]\n" +
                "{\n" +
                "   let x = [];\n" +
                "   test::testHelper4($x);\n" +
                "}\n");

        this.assertNilType(this.execute("test::testFn10():String[0..1]"));
    }

    @Test
    public void testNilWithAddInMap() throws Exception
    {
        compileTestSource("fromString.pure", "function test::testFn():String[1]\n" +
                "{\n" +
                "  let dummy = [];\n" +
                "  ['a', 'b', 'c', 'd']->map(s | add($dummy, $s))->joinStrings(', ');\n" +
                "}\n");
        assertValue("a, b, c, d", this.execute("test::testFn():String[1]"));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    private void assertNilType(CoreInstance instance)
    {
        Assert.assertTrue(this.processorSupport.valueSpecification_instanceOf(instance, M3Paths.Nil));
    }

    private void assertValue(Object expected, CoreInstance instance)
    {
        Assert.assertEquals(expected, Iterate.getFirst(((InstanceValue) instance)._values()));
    }
}
