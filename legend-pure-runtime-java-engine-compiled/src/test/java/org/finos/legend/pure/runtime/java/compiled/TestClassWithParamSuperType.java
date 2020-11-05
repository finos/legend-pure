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

package org.finos.legend.pure.runtime.java.compiled;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClassWithParamSuperType extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testTypeParamsAndExtends()
    {
        compileTestSource("fromString.pure","Class A<P>\n" +
                          "{\n" +
                          "    test : P[1];\n" +
                          "}" +
                          "" +
                          "Class B<K> extends A<K>" +
                          "{" +
                          "}\n" +
                          "\n" +
                          "function test():Any[*]\n" +
                          "{" +
                          "  ^B<String>(test = 'eeee');" +
                          "  ^B<Integer>(test = 2);" +
                          "}\n");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testTypeParamsMulParamsAndExtends()
    {
        compileTestSource("fromString.pure","Class A<P|m>\n" +
                          "{\n" +
                          "    test : P[m];\n" +
                          "}" +
                          "" +
                          "Class B<K|z> extends A<K|z>" +
                          "{" +
                          "}\n" +
                          "\n" +
                          "function test():Any[*]\n" +
                          "{" +
                          "  ^B<String|1>(test = 'eeee');" +
                          "  ^B<Integer|*>(test = [2,3]);" +
                          "}\n");
        this.compileAndExecute("test():Any[*]");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
