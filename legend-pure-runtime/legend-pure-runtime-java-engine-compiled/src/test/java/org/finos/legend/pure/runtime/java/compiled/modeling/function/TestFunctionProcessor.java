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

package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionProcessor extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testProcessFunctionDefinitionContent()
    {
        compileTestSource("function pkg::f1():Boolean[1]\n" +
                "{\n" +
                "   true;\n" +
                "   true;\n" +
                "}\n");

        compileTestSource("function pkg::f2():Boolean[1]\n" +
                "{\n" +
                "   1;\n" +
                "   true;\n" +
                "}\n");

        compileTestSource("function pkg::f3():Boolean[1]\n" +
                "{\n" +
                "   [];\n" +
                "   true;\n" +
                "}\n");

        compileTestSource("function pkg::f4():String[1]\n" +
                "{\n" +
                "   [true, false];\n" +
                "   'string';\n" +
                "}\n");

        compileTestSource("function pkg::f5():String[1]\n" +
                "{\n" +
                "   let a = 99;\n" +
                "   $a;\n" +
                "   'string';\n" +
                "}\n");

        compileTestSource("function pkg::f6():String[1]\n" +
                "{\n" +
                "   [];\n" +
                "   'string';\n" +
                "}\n");

        compileTestSource("function pkg::f7():Boolean[1]\n" +
                "{\n" +
                "   1==1;\n" +
                "   2==2;\n" +
                "}\n");

        compileTestSource("function pkg::f8():Boolean[1]\n" +
                "{\n" +
                "   if(true, |true;true;, |1==2;3==4;);\n" +
                "   if(false, |true;true;, |1==2;3==4;);\n" +
                "   false;\n" +
                "}\n");

        compileTestSource("Class pkg::C{}\n" +
                "function pkg::f9():Boolean[1]\n" +
                "{\n" +
                "   ^pkg::C();\n" +
                "   true;\n" +
                "}\n");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
