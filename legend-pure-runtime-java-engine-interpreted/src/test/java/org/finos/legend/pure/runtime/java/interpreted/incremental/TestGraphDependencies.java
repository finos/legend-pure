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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestGraphDependencies extends AbstractPureTestWithCoreCompiled
{
//    @Test
//    public void testProperty()
//    {
//        compileTestSource("Class A\n" +
//                "{\n" +
//                "   prop:String[1];\n" +
//                "}\n" +
//                "\n" +
//                "function go():Nil[0]\n" +
//                "{\n" +
//                "   let b = ^A(prop = 'ok');\n" +
//                "   print($b.prop);\n" +
//                "   print(A.property('prop'));\n" +
//                "}");
//        this.execute("go():Nil[0]");
//        Assert.assertEquals("hello__String_1_ instance SimpleFunctionDefinition\n", this.functionExecution.getConsole().getLine(1));
//    }

    @Ignore
    @Test
    public void testFunctionApplicationsAndFunctionExpressionContext()
    {
        compileTestSource("Class A\n" +
                          "{\n" +
                          "   b(){'3';hello();'1';}:String[1];\n" +
                          "}\n" +
                          "\n" +
                          "function other(s:String[1]):String[1]\n" +
                          "{\n" +
                          "   $s\n" +
                          "}\n" +
                          "\n" +
                          "function hello():String[1]\n" +
                          "{\n" +
                          "   'a';\n" +
                          "}\n" +
                          "\n" +
                          "function omg():Nil[0]\n" +
                          "{\n" +
                          "   2;\n" +
                          "   5;\n" +
                          "   other(hello());\n" +
                          "   hello();\n" +
                          "   [];\n" +
                          "}\n" +
                          "\n" +
                          "function go():Boolean[1]\n" +
                          "{\n" +
                          "    hello();\n" +
                          "    let r = hello__String_1_;\n" +
                          "    assertEq(4, $r.applications->size());\n" +
                          "    assertEq(1, $r.referenceUsages->size());\n" +
                          "    let set = $r.applications->evaluateAndDeactivate().usageContext->map(u|$u->match([\n" +
                          "                                                                                         a:ExpressionSequenceValueSpecificationContext[1]|$a.functionDefinition.functionName->toOne()+'[expr]',\n" +
                          "                                                                                         a:ParameterValueSpecificationContext[1]|$a.functionExpression.func.functionName->toOne()+'[param]'\n" +
                          "                                                                                         ])\n" +
                          "                                                                            );\n" +
                          "    assertEq('b[expr],go[expr],omg[expr],other[param]', $set->sort({a,b|$a->compare($b)})->makeString(','));\n" +
                          "}");
        this.execute("go():Boolean[1]");
    }

}