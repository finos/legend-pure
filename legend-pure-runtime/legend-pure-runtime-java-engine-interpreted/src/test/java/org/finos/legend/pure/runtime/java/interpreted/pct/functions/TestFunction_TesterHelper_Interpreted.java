// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.pct.functions;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunction_TesterHelper_Interpreted extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        FunctionExecution f = getFunctionExecution();
        setUpRuntime(f);
        f.getConsole().setPrintStream(System.out);
        f.getConsole().enable();
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
    public void testNativeFunctionTesterHelperBeforeAddingToPCT()
    {
        compileTestSource("fromString.pure",
                "function test():Any[*]\n" +
                        "{" +
                        "   print('1', 0);" +
                        "}");
        this.execute("test():Any[*]");
        runtime.delete("fromString.pure");
    }

    @Test
    public void testX()
    {
        compileTestSource("fromString.pure",
                "Class MyClass(x:Integer[1])" +
                        "[" +
                        "   wx(~function:$this.text->size() < $x ~message:'ee'+$x->toString())" +
                        "]" +
                        "{" +
                        "   res(){'1'+$x->toString()}:String[1];" +
                        "   res(z:String[1]){'1'+$x->toString()+$z}:String[1];" +
                        "   text : String[*];" +
                        "}" +
                        "function test():Any[*]\n" +
                        "{" +
                        "   print(^MyClass(10)(text = ['a', 'b']).res()+'\\n',1);" +
                        "   print(^MyClass(10)(text = ['a', 'b']).res('z')+'\\n',1);" +
                        "   print('1', 0);" +
                        "}");
        this.execute("test():Any[*]");
        runtime.delete("fromString.pure");
    }

    @Test
    public void testWX()
    {
        compileTestSource("fromString.pure",
                "Primitive XX extends Integer" +
                        "[" +
                        "   $this < 10" +
                        "]" +
                        "function test():Any[*]\n" +
                        "{" +
                        "   1->cast(@XX);" +
                        "   print('1', 0);" +
                        "}");
        this.execute("test():Any[*]");
        runtime.delete("fromString.pure");
    }

}


