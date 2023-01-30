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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestCanReactivateDynamically extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testBasicInstanceValue()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(true == canReactivateDynamically({|1}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testSimpleFuncExpressionParams()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(true == canReactivateDynamically({|1->map(s|$s->toString())->joinStrings('')->map(x|'*' + $x + '*')}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testEval()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(true == canReactivateDynamically_ValueSpecification_1__Boolean_1_->eval({|1}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testNonReactivatableFunction()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == canReactivateDynamically({s:String[1]|$s}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testNonReactivatableNestedFunction()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == canReactivateDynamically({s:String[1]|$s + 'hi'}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");

        runtime.delete("fromString.pure");
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == canReactivateDynamically({s:Any[1]|instanceOf($s, String) && true}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");

        runtime.delete("fromString.pure");
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   prop: B[1];\n" +
                        "}\n" +
                        "Class B\n" +
                        "{\n" +
                        "   prop: String[1];\n" +
                        "}\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == canReactivateDynamically({s:String[1]|^A(prop = ^B(prop = $s))}->evaluateAndDeactivate().expressionSequence->toOne()), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testNonReactivatableNestedFunctionWithSomeVariablesMissing()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == canReactivateDynamically({s:String[1], y:String[1]|$s + $y}->evaluateAndDeactivate().expressionSequence->toOne(), newMap(^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')))), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testSimpleFuncExpressionReactivationInScopeOfParams()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(canReactivateDynamically({s:String[1]|$s}->evaluateAndDeactivate().expressionSequence->toOne(), newMap(^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')))), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testNestedFuncExpressionReactivationInScopeOfParams()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(canReactivateDynamically({s:String[1]|$s + 'hi'}->evaluateAndDeactivate().expressionSequence->toOne(), newMap(^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')))), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");

        runtime.delete("fromString.pure");
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(canReactivateDynamically({s:String[1], y:String[1]|$s + $y}->evaluateAndDeactivate().expressionSequence->toOne(), newMap([^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')), ^Pair<String, List<Any>>(first = 'y', second = ^List<Any>(values = 'dummy'))])), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");

        runtime.delete("fromString.pure");
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(canReactivateDynamically({s:Any[1]|instanceOf($s, String) && true}->evaluateAndDeactivate().expressionSequence->toOne(), newMap(^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')))), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");

        runtime.delete("fromString.pure");
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   prop: B[1];" +
                        "}\n" +
                        "Class B\n" +
                        "{\n" +
                        "   prop: String[1];" +
                        "}\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(canReactivateDynamically({s:String[1]|^A(prop = ^B(prop = $s))}->evaluateAndDeactivate().expressionSequence->toOne(), newMap(^Pair<String, List<Any>>(first = 's', second = ^List<Any>(values = 'dummy')))), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }
}
