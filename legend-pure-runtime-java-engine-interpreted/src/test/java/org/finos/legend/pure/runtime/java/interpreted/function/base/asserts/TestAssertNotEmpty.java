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

public class TestAssertNotEmpty extends PureExpressionTest
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }
    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("Expected non-empty collection", 3, 9, "assertNotEmpty([1, 2, 3]->filter(x | $x == 5))",
                "function meta::pure::functions::asserts::assertNotEmpty(collection:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assertNotEmpty($collection, 'Expected non-empty collection');\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEmpty(collection:Any[*], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertFalse($collection->isEmpty(), $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEmpty(collection:Any[*], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assertFalse($collection->isEmpty(), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEmpty(collection:Any[*], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assertFalse($collection->isEmpty(), $message);\n" +
                        "}\n" +
                        "function meta::pure::functions::asserts::assertFalse(condition:Boolean[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!$condition);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertFalse(condition:Boolean[1], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!$condition, $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertFalse(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!$condition, $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertFalse(condition:Boolean[1], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!$condition, $message);\n" +
                        "}" +
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, 'Assert failed');\n" +
                        "}\n" +
                        "\n" +
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

