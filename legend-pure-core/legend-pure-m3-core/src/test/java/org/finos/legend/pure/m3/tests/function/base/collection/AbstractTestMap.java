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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestMap extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("classes.pure");
        runtime.compile();
    }

    @Test
    public void testMapWithMultiplicityInferencePropertyOwner()
    {
        compileTestSource(
                "fromString.pure",
                "Class Employee<|m>\n" +
                        "{\n" +
                        "    prop:String[m];\n" +
                        "}\n" +
                        "\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    let f = [^Employee<|*>(prop=['a','b']), ^Employee<|1>(prop='b')];\n" +
                        "    print($f->map(e|$e.prop), 1);\n" +
                        "}\n");
        execute("test():Nil[0]");
        Assert.assertEquals(
                "[\n" +
                        "   'a'\n" +
                        "   'b'\n" +
                        "   'b'\n" +
                        "]",
                functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithMultiplicityInferenceFunctionWhichIsNotAProperty()
    {
        compileTestSource(
                "fromString.pure",
                "function f<|m>(s:String[m]):String[m]\n" +
                        "{\n" +
                        "    $s\n" +
                        "}\n" +
                        "\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print([^List<String>(values='a'), ^List<String>(values=['b','c']), ^List<String>(values='c')]->map(i|f($i.values)), 1);\n" +
                        "}\n");
        execute("test():Nil[0]");
        Assert.assertEquals(
                "[\n" +
                        "   'a'\n" +
                        "   'b'\n" +
                        "   'c'\n" +
                        "   'c'\n" +
                        "]",
                functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithVariableThisAsParameter()
    {
        compileTestSource(
                "fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   func(valueFunc:Function<{A[1]->Float[1]}>[1])\n" +
                        "   {\n" +
                        "       if(true, |$this->map($valueFunc), |1.0);\n" +
                        "   }:Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(^A().func(a | 2.0), 1);\n" +
                        "}\n");
        execute("test():Nil[0]");
        Assert.assertEquals("2.0", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testAutoMapWithZeroToOne()
    {
        compileTestSource(
                "classes.pure",
                "Class A\n" +
                        "{\n" +
                        "    b: B[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    name: String[1];\n" +
                        "}\n");
        compileTestSource(
                "fromString.pure",
                "function test(a:A[1]):Any[*]\n" +
                        "{\n" +
                        "    $a.b.name;\n" +
                        "}\n");
        CoreInstance autoMap = Automap.getAutoMapExpressionSequence(Instance.getValueForMetaPropertyToManyResolved(runtime.getCoreInstance("test_A_1__Any_MANY_"), M3Properties.expressionSequence, processorSupport).getFirst());
        Assert.assertNotNull(autoMap);
    }

    @Test
    public void testAutoMapWithZeroToOneInEvaluate()
    {
        compileTestSource(
                "fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "    b:B[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "    assertEquals('Akbar the Great', ^A(b=^B(name='Akbar the Great')).b.name);\n" +
                        "    assertEmpty(^A().b.name);\n" +
                        "    let fn1 = {|^A(b=^B(name='Akbar the Great')).b.name};\n" +
                        "    let lambda1 = ^LambdaFunction<{->String[0..1]}>(expressionSequence = $fn1.expressionSequence);\n" +
                        "    assertEquals('Akbar the Great', $lambda1->evaluate([]));\n" +
                        "    let fn2 = {|^A().b.name};\n" +
                        "    let lambda2 = ^LambdaFunction<{->String[0..1]}>(expressionSequence = $fn2.expressionSequence);\n" +
                        "    assertEmpty($lambda2->evaluate([]));\n" +
                        "}\n");
        execute("test():Any[*]");
    }
}
