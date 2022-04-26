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

public class TestFail extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getExtra());
    }

    @Test
    public void testFail()
    {
        assertExpressionRaisesPureException("Assert failed", 3, 9, "fail()");
    }

    @Test
    public void testFailWithMessage()
    {
        assertExpressionRaisesPureException("Error Here", 3, 9, "fail('Error Here')");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair("testFail.pure",
                "function meta::pure::functions::asserts::fail():Boolean[1]\n" +
                        "{\n" +
                        "    assert(false);\n" +
                        "}\n" +
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, 'Assert failed');\n" +
                        "}"
        );
    }
}
