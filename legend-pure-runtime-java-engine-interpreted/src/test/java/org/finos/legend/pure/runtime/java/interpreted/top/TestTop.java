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

package org.finos.legend.pure.runtime.java.interpreted.top;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTop extends AbstractPureTestWithCoreCompiled
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
    public void testSimplePure()
    {
        compileTestSource("fromString.pure","###Pure\n" +
                "   Class Employee\n" +
                "   {\n" +
                "       s:String[1];\n" +
                "   }\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "   function go():Nil[0]\n" +
                "   {\n" +
                "       print('yeah!', 1);\n" +
                "   }\n");
        this.execute("go():Nil[0]");
        Assert.assertEquals("'yeah!'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testSimplePureWithError() throws Exception
    {
        try
        {
            compileTestSource("fromString.pure","###Pure\n" +
                    "   Class Employee\n" +
                    "   {\n" +
                    "       s:String[1];\n" +
                    "   }\n" +
                    "\n" +
                    "\n" +
                    "###Pure\n" +
                    "   function go():Nil[0]\n" +
                    "   {\n" +
                    "       printNotWorking('yeah!');\n" +
                    "   }\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The system can't find a match for the function: printNotWorking(_:String[1])", 11, 8, e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
