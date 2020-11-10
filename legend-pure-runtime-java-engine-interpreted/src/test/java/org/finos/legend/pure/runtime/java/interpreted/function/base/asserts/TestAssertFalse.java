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

public class TestAssertFalse extends PureExpressionTest
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution(), extra);
    }

    public static Pair<String, String> extra = Tuples.pair("/system/extra.pure","function meta::pure::functions::asserts::assertFalse(condition:Boolean[1]):Boolean[1]\n" +
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
        "}\n" +
        "function meta::pure::functions::asserts::assert(condition:Boolean[1]):Boolean[1]\n" +
        "{\n" +
        "    assert($condition, 'Assert failed');\n" +
        "}"+
        "function meta::pure::functions::asserts::assert(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
        "{\n" +
        "    assert($condition, | format($formatString, $formatArgs));\n" +
        "}");

    @Test
    public void testFailWithoutMessage()
    {
        assertExpressionRaisesPureException("Assert failed", 3, 9, "assertFalse(true)");
        assertExpressionRaisesPureException("Assert failed", 3, 9, "assertFalse(2 == 2)");
    }

    @Test
    public void testFailWithMessageString()
    {
        assertExpressionRaisesPureException("Test message", 3, 9, "assertFalse(true, 'Test message')");
        assertExpressionRaisesPureException("Test message", 3, 9, "assertFalse(2 == 2, 'Test message')");
    }

    @Test
    public void testFailWithFormattedMessage()
    {
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(true, 'Test message: %d', 2 + 3)");
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(2 == 2, 'Test message: %d', 2 + 3)");
    }

    @Test
    public void testFailWithMessageFunction()
    {
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(true, |format('Test message: %d', 2 + 3))");
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(2 == 2, |format('Test message: %d', 2 + 3))");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
