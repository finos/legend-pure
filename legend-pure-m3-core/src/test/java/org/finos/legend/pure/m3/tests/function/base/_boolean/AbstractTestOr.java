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

package org.finos.legend.pure.m3.tests.function.base._boolean;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestOr extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testBasicParse()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(true == or(true, true), |'');\n" +
                        "   assert(true == or(false, true), |'');\n" +
                        "   assert(true == or(true, false), |'');\n" +
                        "   assert(false == or(false, false), |'');\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }

    @Test
    public void testEvalParse()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(true, true), |'');\n" +
                        "   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(false, true), |'');\n" +
                        "   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(true, false), |'');\n" +
                        "   assert(false == or_Boolean_1__Boolean_1__Boolean_1_->eval(false, false), |'');\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }

    @Test
    public void testShortCircuit()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "    value:Any[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let a1 = ^A(value=^B(name='Claudius Ptolemy'));\n" +
                        "   let a2 = ^A(value=1);\n" +
                        "   assertFalse(!$a1.value->instanceOf(B) || ($a1.value->cast(@B).name != 'Claudius Ptolemy'));\n" +
                        "   assert(!$a2.value->instanceOf(B) || ($a2.value->cast(@B).name != 'Claudius Ptolemy'));\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }

    @Test
    public void testShortCircuitInDynamicEvaluation()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "    value:Any[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let fn1 = {|let a = ^A(value=^B(name='Claudius Ptolemy'));\n" +
                        "               !$a.value->instanceOf(B) || ($a.value->cast(@B).name != 'Claudius Ptolemy');};\n" +
                        "   let lambda1 = ^LambdaFunction<{->Boolean[1]}>(expressionSequence = $fn1.expressionSequence);\n" +
                        "   assertEquals(false, $lambda1->evaluate([]));\n" +
                        "   let fn2 = {|let a = ^A(value=1);\n" +
                        "               !$a.value->instanceOf(B) || ($a.value->cast(@B).name != 'Claudius Ptolemy');};\n" +
                        "   let lambda2 = ^LambdaFunction<{->Boolean[1]}>(expressionSequence = $fn2.expressionSequence);\n" +
                        "   assertEquals(true, $lambda2->evaluate([]));\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }
}
