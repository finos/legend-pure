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

public class TestAssertEqWithinTolerance extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getExtra());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("\nexpected: 1\nactual:   0", 3, 9, "assertEqWithinTolerance(1, 0, 0)");
        assertExpressionRaisesPureException("\nexpected: 2.718271828459045\nactual:   2.718281828459045", 3, 9, "assertEqWithinTolerance(2.718271828459045, 2.718281828459045, 0.000000001)");
        assertExpressionRaisesPureException("\nexpected: 2.718281828459045\nactual:   2.7182818284590455", 3, 9, "assertEqWithinTolerance(2.718281828459045, 2.7182818284590455, 0.0000000000000001)");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "testAssertEqWithTolerance.pure",
                "function meta::pure::functions::asserts::assertEqWithinTolerance(expected:Number[1], actual:Number[1], delta:Number[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertEqWithinTolerance($expected, $actual, $delta, '\\nexpected: %r\\nactual:   %r', [$expected, $actual]);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEqWithinTolerance(expected:Number[1], actual:Number[1], delta:Number[1], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(abs($expected - $actual) <= abs($delta), $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEqWithinTolerance(expected:Number[1], actual:Number[1], delta:Number[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(abs($expected - $actual) <= abs($delta), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEqWithinTolerance(expected:Number[1], actual:Number[1], delta:Number[1], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(abs($expected - $actual) <= abs($delta), $message);\n" +
                        "}" +
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, | format($formatString, $formatArgs));\n" +
                        "}");
    }
}
