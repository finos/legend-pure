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
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_AsFunctionExpressionParam extends AbstractPureTestWithCoreCompiled
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
    }

    @Test
    public void testPureRuntimeFunctionParamDependencies() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        this.runtime.createInMemorySource("userId.pure", "function fix(s:String[1]):String[1]{$s}\n" +
                                               "function go():Nil[0]{print(fix(sourceFunction()),1);}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 2, 32, e);
            }

            this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFuncYeah!';}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("'theFuncYeah!'", this.functionExecution.getConsole().getLine(0));

            this.runtime.delete("sourceId.pure");
            this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureRuntimeFunctionParamDependenciesError() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.createInMemorySource("userId.pure", "function fix(s:String[1]):String[1]{$s}\n" +
                                               "function go():Nil[0]{print(fix(sourceFunction()),1);}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 2, 32, e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "fix(_:Integer[1])\n" +
                        PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                        "\tfix(String[1]):String[1]\n" +
                        PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 2, 28, e);
            }
        }

        this.runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
   }

    @Test
    public void testPureRuntimeFunctionParamDependenciesTypeInference() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.createInMemorySource("userId.pure", "function fix(s:String[1]):String[1]{$s}\n" +
                                               "function myFunction<T>(p:T[1]):T[1]{$p}\n" +
                                               "function go():Nil[0]{print(fix(myFunction(sourceFunction())),1)}\n");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 3, 43, e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "fix(_:Integer[1])\n" +
                        PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                        "\tfix(String[1]):String[1]\n" +
                        PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 3, 28, e);
            }
        }
        this.runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
