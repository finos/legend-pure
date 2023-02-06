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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_InExpressionSequence extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");

//        try {
//            runtime.compile();
//        } catch(PureCompilationException e) {
//            setUp();
//        }
    }

    @Test
    public void testPureRuntimeFunctionBodyDependencies() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFunc',1);}");
        this.runtime.createInMemorySource("userId.pure", "function go():Nil[0]{sourceFunction();}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The system can't find a match for the function: sourceFunction()", "userId.pure", 1, 22, e);
            }

            this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFuncYeah!', 1);}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("'theFuncYeah!'", this.functionExecution.getConsole().getLine(0));
        }
        this.runtime.delete("sourceId.pure");
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFunc', 1);}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    @Test
    public void testPureRuntimeFunctionBodyDependenciesError() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        this.runtime.createInMemorySource("userId.pure", "function test():String[1]{sourceFunction()}" +
                                               "function go():Nil[0]{print(test(),1);}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The system can't find a match for the function: sourceFunction()", "userId.pure", 1, 27, e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Return type error in function 'test'; found: Integer; expected: String", "userId.pure", 1, 27, e);
            }
        }
        this.runtime.delete("sourceId.pure");
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
