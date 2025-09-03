// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.runtime.dynamicTypePropagation;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionWithTypeParamAsFuncParam extends AbstractPureTestWithCoreCompiled
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
        runtime.compile();
    }

    @Test
    public void testSimple()
    {
        compileTestSource("fromString.pure",
                "function x::test<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                "{\n" +
                "  true;\n" +
                "}\n" +
                "function x::sql<X|o>(f:Function<{->X[o]}>[1]):X[o]\n" +
                "{\n" +
                "   $f->eval()\n" +
                "}\n" +
                "function a():Any[*]\n" +
                "{\n" +
                "   x::test(x::sql_Function_1__X_o_)\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   a();\n" +
                "}");

        execute("go():Any[*]");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
