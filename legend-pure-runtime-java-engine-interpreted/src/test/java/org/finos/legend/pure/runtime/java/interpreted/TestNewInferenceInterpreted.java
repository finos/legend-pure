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

package org.finos.legend.pure.runtime.java.interpreted;

import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestNewInferenceAtRuntime;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNewInferenceInterpreted extends AbstractTestNewInferenceAtRuntime
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

    @Test
    public void newMapRuntimeResolution() throws Exception
    {
        compileTestSource("fromString.pure",
                "Class A{}\n" +
                        "Class B{}" +
                        "function gb<U,K>(set:U[*], f:Function<{U[1]->K[1]}>[1]):Map<K,List<U>>[1]\n" +
                        "{\n" +
                        "   ^Map<K,List<U>>();\n" +
                        "}\n" +
                        "\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   let a = [A,B]->gb(a|$a->elementToPath());\n" +
                        "   assert(String == $a.classifierGenericType.typeArguments->at(0).rawType, |'');\n" +
                        "   assert(Class == $a.classifierGenericType.typeArguments->at(1).typeArguments->at(0).rawType, |'');\n" +
                        "}");
        this.compileAndExecute("test():Any[*]");
    }
}
