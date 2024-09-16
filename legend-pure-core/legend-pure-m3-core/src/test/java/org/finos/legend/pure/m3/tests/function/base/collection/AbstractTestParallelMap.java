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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestParallelMap extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("classes.pure");
        runtime.compile();
    }

    @Test
    public void testParallelMapWithMultiplicityInferencePropertyOwner()
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
                        "    print($f->parallelMap(e|$e.prop, 3), 1);\n" +
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
    public void testParallelMapWithMultiplicityInferenceFunctionWhichIsNotAProperty()
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
                        "    print([^List<String>(values='a'), ^List<String>(values=['b','c']), ^List<String>(values='c')]->parallelMap(i|f($i.values), 2), 1);\n" +
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
    public void testParallelMapWithVariableThisAsParameter()
    {
        compileTestSource(
                "fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   func(valueFunc:Function<{A[1]->Float[1]}>[1])\n" +
                        "   {\n" +
                        "       if(true, |$this->parallelMap($valueFunc, 4), |1.0);\n" +
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
    public void testParallelMapWithEvalOnFunctionPointer()
    {
        compileTestSource(
                "fromString.pure",
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(parallelMap_T_m__Function_1__Integer_1__V_m_->eval([1, 2, 3], x:Integer[1]|$x+1, 2), 1)" +
                        "}\n");
        execute("test():Nil[0]");
        Assert.assertEquals(
                "[\n" +
                        "   2\n" +
                        "   3\n" +
                        "   4\n" +
                        "]",
                functionExecution.getConsole().getLine(0));
    }
}
