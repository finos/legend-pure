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

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAssertSize extends PureExpressionTest
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("expected size: 3, actual size: 2", 3, 9, "assertSize([1, 2], 3)",
                "function meta::pure::functions::asserts::assertSize(collection:Any[*], size:Integer[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertSize($collection, $size, 'expected size: %s, actual size: %s', [$size, $collection->size()]);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertSize(collection:Any[*], size:Integer[1], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertEq($size, $collection->size(), $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertSize(collection:Any[*], size:Integer[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assertEq($size, $collection->size(), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertSize(collection:Any[*], size:Integer[1], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertEq($size, $collection->size(), $message);\n" +
                        "}function meta::pure::functions::asserts::assertEq(expected:Any[1], actual:Any[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertEq($expected, $actual, '\\nexpected: %r\\nactual:   %r', [$expected, $actual]);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEq(expected:Any[1], actual:Any[1], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(eq($expected, $actual), $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEq(expected:Any[1], actual:Any[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(eq($expected, $actual), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertEq(expected:Any[1], actual:Any[1], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(eq($expected, $actual), $message);\n" +
                        "}\n"+
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, | format($formatString, $formatArgs));\n" +
                        "}");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
