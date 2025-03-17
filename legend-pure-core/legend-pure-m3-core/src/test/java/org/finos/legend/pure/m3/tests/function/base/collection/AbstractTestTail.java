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

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestTail extends PureExpressionTest
{
    @Test
    public void testBasic()
    {
        compileSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert([2,3] == [1,2,3]->tail(), |'');\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }

    @Test
    public void testEval()
    {
        compileSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert([2,3] == tail_T_MANY__T_MANY_->eval([1,2,3]), |'');\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }
}
