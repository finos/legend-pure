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

package org.finos.legend.pure.runtime.java.interpreted.function.base.asserts;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAssertEquals extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getExtra());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("\nexpected: 1\nactual:   2", 3, 9, "assertEquals(1, 2)");
    }

    @Test
    public void testFailureWithCollections()
    {
        assertExpressionRaisesPureException("\nexpected: [1, 3, 2]\nactual:   [2, 4, 1, 5]", 3, 9, "assertEquals([1, 3, 2], [2, 4, 1, 5])");
        assertExpressionRaisesPureException("\nexpected: [1, 2]\nactual:   [2, 1]", 3, 9, "assertEquals([1, 2], [2, 1])");
        assertExpressionRaisesPureException("\nexpected: ['aaa', 2]\nactual:   [2, 'aaa']", 3, 9, "assertEquals(['aaa', 2], [2, 'aaa'])");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "testAssertEquals.pure",
                "function meta::pure::functions::asserts::assertEquals(expected:Any[*], actual:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    if(eq($expected->size(), 1) && eq($actual->size(), 1),\n" +
                        "       | assertEquals($expected, $actual, '\\nexpected: %r\\nactual:   %r', [$expected->toOne(), $actual->toOne()]),\n" +
                        "       | assertEquals($expected, $actual, '\\nexpected: %s\\nactual:   %s', [$expected->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']'), $actual->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']')]));\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEquals(expected:Any[*], actual:Any[*], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(equal($expected, $actual), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEquals(expected:Any[*], actual:Any[*], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(equal($expected, $actual), $message);\n" +
                        "}" +
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, | format($formatString, $formatArgs));\n" +
                        "}\n");
    }
}
