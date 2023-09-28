// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.function.relation;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;

public class SimpleTest extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

//    @org.junit.Test
//    public void testToOneError()
//    {
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{\n" +
//                        "   let tds = #TDS\n" +
//                        "                value, str\n" +
//                        "                1, a\n" +
//                        "                3, ewe\n" +
//                        "                4, qw\n" +
//                        "                5, wwe\n" +
//                        "                5, weq\n" +
//                        "              #->groupBy(['value'])->map(x|$x.value);" +
//                        "   print($tds, 1);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//    }


}
