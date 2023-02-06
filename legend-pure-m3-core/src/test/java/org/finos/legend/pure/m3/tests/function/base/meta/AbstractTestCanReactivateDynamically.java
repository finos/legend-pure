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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
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

}
