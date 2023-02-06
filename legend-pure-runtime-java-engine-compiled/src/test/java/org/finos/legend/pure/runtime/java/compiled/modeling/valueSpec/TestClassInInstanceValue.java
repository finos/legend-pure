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

package org.finos.legend.pure.runtime.java.compiled.modeling.valueSpec;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClassInInstanceValue extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testListOfClassesValue()
    {
        compileTestSource("fromString.pure", "Class A\n" +
                          "{\n" +
                          "    test : String[1];\n" +
                          "}\n" +
                          "\n" +
                          "function test():Boolean[1]\n" +
                          "{" +
                          "   let classes = [A,A];\n" +
                          "   assertEquals('A', $classes.name->removeDuplicates());\n" +
                          "}\n");
        this.compileAndExecute("test():Boolean[1]");
    }

    @Test
    public void testListOfClassesValueAsParams()
    {
        compileTestSource("fromString.pure", "Class A\n" +
                          "{\n" +
                          "    test : String[1];\n" +
                          "}\n" +
                          "\n" +
                          "function test():Boolean[1]\n" +
                          "{" +
                          "   assertEquals('x', fu([A,A]));\n" +
                          "}" +
                          "" +
                          "function fu(a: Class<Any>[*]):Any[*]" +
                          "{" +
                          "     'x';" +
                          "}\n");
        this.compileAndExecute("test():Boolean[1]");
    }

    @Test
    public void testListOfClassesValueOneValueInList()
    {
        compileTestSource("fromString.pure", "Class A\n" +
                          "{\n" +
                          "    test : String[1];\n" +
                          "}\n" +
                          "\n" +
                          "function test():Boolean[1]\n" +
                          "{" +
                          "   assertEquals('x', fu([A]));\n" +
                          "}" +
                          "" +
                          "function fu(a: Class<Any>[*]):Any[*]" +
                          "{" +
                          "     'x';" +
                          "}\n");
        this.compileAndExecute("test():Boolean[1]");
    }

    @Test
    public void testListOfClassesWithCommonSupertype()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A {}\n" +
                "Class test::B extends A {}\n" +
                "Class test::C extends A {}\n" +
                "Class test::D extends A {}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "  let classes = [B, C, D];\n" +
                "  assert('test::B, test::C, test::D' == $classes->map(c | $c->elementToPath())->joinStrings(', '), |'');\n" +
                "}\n");
        compileAndExecute("test():Any[*]");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
