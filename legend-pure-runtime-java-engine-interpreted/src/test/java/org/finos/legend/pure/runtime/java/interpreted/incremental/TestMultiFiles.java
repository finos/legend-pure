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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMultiFiles extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testPureRuntimeFunctionParamDependencies() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function simple():String[1]{'theFunc';}");
        this.runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(simple(),1);}");
        this.compileAndExecute("go():Nil[0]");
        Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("userId.pure");
            this.runtime.delete("sourceId.pure");

            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertEquals("The function 'go():Nil[0]' can't be found", e.getMessage());
            }

            this.runtime.createInMemorySource("sourceId.pure", "function simple():String[1]{'theFunc';}");
            this.runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(simple(),1);}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("'theFunc'", this.functionExecution.getConsole().getLine(0));
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
