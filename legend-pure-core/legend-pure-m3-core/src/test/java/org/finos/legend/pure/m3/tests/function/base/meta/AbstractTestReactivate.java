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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractTestReactivate extends AbstractPureTestWithCoreCompiled
{

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    protected void compileAndExecuteVariableScopeFailure()
    {
        compileTestSource("testSource.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = 7;  \n" +
                        "   let t = {|{| $a + 3};};\n" +
                        "   \n" +
                        "   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);\n" +
                        "   \n" +
                        "   assert('a' == $t->openVariableValues()->keys()->first(), |'');\n" +
                        "   assert(0 == $l->openVariableValues()->keys()->size(), |'');\n" +
                        "   let z = r($l);\n" +
                        "   print($z, 1);\n" +
                        "   false;\n" +
                        "}\n" +
                        "\n" +
                        "function r(l:LambdaFunction<Any>[1]):Any[*]\n" +
                        "{\n" +
                        "   $l.expressionSequence->at(0)->reactivate(^Map<String, List<Any>>());\n" +
                        "}"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeSuccess()
    {
        compileTestSource("testSource.pure",
                "Class meta::pure::executionPlan::PlanVarPlaceHolder\n" +
                        "{\n" +
                        "    name : String[1];\n" +
                        "}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = 7;  \n" +
                        "   let t = {|{| $a + 3};};\n" +
                        "   \n" +
                        "   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);\n" +
                        "   \n" +
                        "   assert('a' == $t->openVariableValues()->keys()->first(), |'');\n" +
                        "   assert(0 == $l->openVariableValues()->keys()->size(), |'');\n" +
                        "   let z = r($l, $t->openVariableValues());\n" +
                        "   assert(10 == $z, |'');\n" +
                        "   false;\n" +
                        "}\n" +
                        "\n" +
                        "function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]\n" +
                        "{\n" +
                        "   $l.expressionSequence->at(0)->reactivate($vars);\n" +
                        "}"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeSuccessWithList()
    {
        compileTestSource("testSource.pure",
                "Class meta::pure::executionPlan::PlanVarPlaceHolder\n" +
                        "{\n" +
                        "    name : String[1];\n" +
                        "}" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Integer[*]):Integer[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = [7,3];  \n" +
                        "   let t = {|{| $a->sum() + 3};};\n" +
                        "   \n" +
                        "   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);\n" +
                        "   \n" +
                        "   assert('a' == $t->openVariableValues()->keys()->first(), |'');\n" +
                        "   assert(0 == $l->openVariableValues()->keys()->size(), |'');\n" +
                        "   let z = r($l, $t->openVariableValues());\n" +
                        "   assert(13 == $z, |'');\n" +
                        "   false;\n" +
                        "}\n" +
                        "\n" +
                        "function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]\n" +
                        "{\n" +
                        "   $l.expressionSequence->at(0)->reactivate($vars);\n" +
                        "}"
        );
        execute("go():Any[*]");
    }


    @Test
    public void testVariableScopeWithEmpty()
    {
        compileTestSource("testSource.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = []->cast(@Integer);  \n" +
                        "   let t = {|{| $a->sum() + 3};};\n" +
                        "   \n" +
                        "   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);\n" +
                        "   \n" +
                        "   assert('a' == $t->openVariableValues()->keys()->first(), |'');\n" +
                        "   assert(0 == $l->openVariableValues()->keys()->size(), |'');\n" +
                        "   let z = r($l, $t->openVariableValues());\n" +
                        "   assert(3 == $z, |'');\n" +
                        "   false;\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Integer[*]):Integer[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]\n" +
                        "{\n" +
                        "   $l.expressionSequence->at(0)->reactivate($vars);\n" +
                        "}"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeWithEmptyNested()
    {
        compileTestSource("testSource.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = []->cast(@Integer);  \n" +
                        "   let t = {|{| [1,2,3]->filter(b|true && $a->contains(2))}};\n" +
                        "   \n" +
                        "   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);\n" +
                        "   \n" +
                        "   assert('a' == $t->openVariableValues()->keys()->first(), |'');\n" +
                        "   assert(0 == $l->openVariableValues()->keys()->size(), |'');\n" +
                        "   let z = r($l, $t->openVariableValues());\n" +
                        "   assert([] == $z, |'');\n" +
                        "   false;\n" +
                        "}\n" +
                        "\n" +
                        "function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]\n" +
                        "{\n" +
                        "   $l.expressionSequence->at(0)->reactivate($vars);\n" +
                        "}"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testReactivateFunctionExpressionWithPackageArg()
    {
        compileTestSource("testSource.pure",
                "function test::pkg1::test():Any[1]\n" +
                        "{\n" +
                        "  test::pkg1->map(p | $p)->deactivate()->reactivate(^Map<String, List<Any>>())->toOne()\n" +
                        "}\n");
        Assert.assertEquals(runtime.getCoreInstance("test::pkg1"), ((InstanceValue) execute("test::pkg1::test():Any[1]"))._values().getOnly());
    }

    @Test
    public void testReactivateNewDefaultValues()
    {
        compileTestSource("testSource.pure",
                "Class test::A\n" +
                        "{\n" +
                        "  a:Boolean[1] = true;\n" +
                        "  i:Integer[1] = 10;\n" +
                        "  enumProperty:test::EnumWithDefault[1] = test::EnumWithDefault.DefaultValue;\n" +
                        "}\n" +
                        "\n" +
                        "Enum test::EnumWithDefault\n" +
                        "{\n" +
                        "   DefaultValue,\n" +
                        "   AnotherValue\n" +
                        "}\n" +
                        "\n" +
                        "function test::f():Boolean[1]\n" +
                        "{\n" +
                        "  let l = {| ^test::A()};\n" +
                        "  let a = $l.expressionSequence->evaluateAndDeactivate()->toOne()->reactivate()->cast(@test::A)->toOne();\n" +
                        "  assert($a.a, | 'Default value for property a set to wrong value');\n" +
                        "  assertEquals($a.i, 10);\n" +
                        "  assertEquals($a.enumProperty, test::EnumWithDefault.DefaultValue);\n" +
                        "}\n");
        execute("test::f():Boolean[1]");
    }

    @Test
    public void testReactivateNewOverrideDefaultValues()
    {
        compileTestSource("testSourceDefaultOverride.pure",
                "Class test::A1\n" +
                        "{\n" +
                        "  a:Boolean[1] = true;\n" +
                        "  i:Integer[1] = 10;\n" +
                        "  enumProperty:test::EnumWithDefault1[1] = test::EnumWithDefault1.DefaultValue;\n" +
                        "}\n" +
                        "\n" +
                        "Enum test::EnumWithDefault1\n" +
                        "{\n" +
                        "   DefaultValue,\n" +
                        "   AnotherValue\n" +
                        "}\n" +
                        "\n" +
                        "function test::f1():Boolean[1]\n" +
                        "{\n" +
                        "  let l = {| ^test::A1(a=false)};\n" +
                        "  let a = $l.expressionSequence->evaluateAndDeactivate()->toOne()->reactivate()->cast(@test::A1)->toOne();\n" +
                        "  assert(!$a.a, | 'Default value for property a set to wrong value');\n" +
                        "  assertEquals($a.i, 10);\n" +
                        "  assertEquals($a.enumProperty, test::EnumWithDefault1.DefaultValue);\n" +
                        "}\n");
        execute("test::f1():Boolean[1]");
    }

    @Test
    public void testReactivateNewDefaultValueInheritance()
    {
        compileTestSource("testSource.pure",
                "Class test::A\n" +
                        "{\n" +
                        "  a:Boolean[1] = true;\n" +
                        "}\n" +
                        "\n" +
                        "Class test::B extends test::A\n" +
                        "{\n" +
                        "  b:Boolean[1] = true;\n" +
                        "}\n" +
                        "function test::f():Boolean[1]\n" +
                        "{\n" +
                        "  let l = {| ^test::B()};\n" +
                        "  let b = $l.expressionSequence->evaluateAndDeactivate()->toOne()->reactivate()->cast(@test::B)->toOne();\n" +
                        "  assert($b.a, | 'Default value for property b set to wrong value');\n" +
                        "}\n");
        execute("test::f():Boolean[1]");
    }

    @ToFix
    public void testReactivateNewDefaultValueAssociation()
    {
        compileTestSource("testSource.pure",
                "import test::*;\n" +
                        "Class test::K\n" +
                        "{\n" +
                        "  i : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::L\n" +
                        "{\n" +
                        "  j : Integer[1];\n" +
                        "}\n" +
                        "Association test::KL\n" +
                        "{\n" +
                        "   a : K[1];\n" +
                        "   b : L[1] = ^test::L(j=1);\n" +
                        "}\n" +
                        "function test::f2():Boolean[1]\n" +
                        "{\n" +
                        "  let l = {| ^test::K(i=1)};\n" +
                        "  let a = $l.expressionSequence->evaluateAndDeactivate()->toOne()->reactivate()->cast(@test::K)->toOne();\n" +
                        "  assertEquals(1, $a.b.j ,| 'Default value for property b set to wrong value');\n" +
                        "}\n");
        execute("test::f2():Boolean[1]");
    }
}
