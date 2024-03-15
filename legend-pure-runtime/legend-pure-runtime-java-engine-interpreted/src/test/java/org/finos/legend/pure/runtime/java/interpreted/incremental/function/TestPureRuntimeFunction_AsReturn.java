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

package org.finos.legend.pure.runtime.java.interpreted.incremental.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_AsReturn extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
    }

    @Test
    public void testPureRuntimeFunctionAsReturn() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", "function test():Function<{->String[1]}>[1]{sourceFunction__String_1_}" +
                "function go():Nil[0]{print(test()->eval(),1)}\n");
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 44, e);
            }

            runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'the New Func!'}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("'the New Func!'", functionExecution.getConsole().getLine(0));
        }
        runtime.delete("sourceId.pure");
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, repository.serialize().length);
    }


    @Test
    public void testPureRuntimeFunctionAsReturnWithError() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", "function test():Function<{->String[1]}>[1]{sourceFunction__String_1_}" +
                "function go():Nil[0]{print(test()->eval(),1)}\n");
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 44, e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 44, e);
            }
        }
        runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, repository.serialize().length);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
