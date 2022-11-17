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

package org.finos.legend.pure.m3.tests.function.base.multiplicity;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestToOneMany extends PureExpressionTest
{
    @Test
    public void testToOneManyError()
    {
        assertExpressionRaisesPureException("Cannot cast a collection of size 0 to multiplicity [1..*]", 3, 13, "[]->toOneMany(); 1;");
        assertExpressionRaisesPureException("Something wrong", 3, 13, "[]->toOneMany('Something wrong'); 1;");
    }

    @Test
    public void testBasic()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == [1]->toOneMany(), |'');\n" +
                        "   assert([1,2,3] == [1,2,3]->toOneMany(), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testEval()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == toOneMany_T_MANY__T_$1_MANY$_->eval([1]), |'');\n" +
                        "   assert([1,2,3] == toOneMany_T_MANY__T_$1_MANY$_->eval([1,2,3]), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testWithMessage()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == [1]->toOneMany(), |'');\n" +
                        "   assert([1,2,3] == [1,2,3]->toOneMany('Something wrong'), |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }
}
