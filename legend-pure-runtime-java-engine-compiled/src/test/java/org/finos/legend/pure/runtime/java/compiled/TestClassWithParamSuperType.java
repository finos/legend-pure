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
    public void testPropertiesWithTypeParamsAndExtends()
    {
        compileTestSource("fromString.pure",
                // class with concrete field that has type arguments - i.e Pair
                "Class A<K,V | m>\n" +
                "{\n" +
                "    test : Pair<K, V>[1];\n" +
                "    withMult: String[m];\n" +
                "}\n" +
                "\n" +
                // class that binds ALL type arguments of generalization
                "Class B extends A<Integer, String | 0..1>" +
                "{\n" +
                "}\n" +
                "\n" +
                // class that binds SOME type arguments of generalization
                "Class C<X|k> extends A<String, X | k>" +
                "{\n" +
                "}\n" +
                "\n" +
                // class that have multiple generalizations with type arguments bind at diff levels
                "Class D extends C<String | 1>" +
                "{\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "  ^D(test = pair('eeee', 'ffff'), withMult = 'aaaa');\n" +
                "  ^C<Float|*>(test = pair('eeee', 1.2), withMult = ['aaaa', 'aaaa']);\n" +
                "  ^B(test = pair(2, 'eeee'),  withMult = []);\n" +
                "  ^A<Integer, String|1>(test = pair(2, 'eeee'), withMult = 'aaaa');\n" +
                "}\n");
        this.compileAndExecute("test():Any[*]");
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
