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
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_NoDependencies extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testPureRuntimeFunctionNoDependencies() throws Exception
    {
        this.runtime.createInMemorySource("myId.pure", "function myFunction():Nil[0]{print('yeah!', 1);}");
        this.compileAndExecute("myFunction():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("'yeah!'", this.functionExecution.getConsole().getLine(0));
        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("myId.pure");
            try
            {
                this.compileAndExecute("myFunction():Nil[0]");
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertEquals("The function 'myFunction():Nil[0]' can't be found", e.getMessage());
            }
            this.runtime.createInMemorySource("myId.pure", "function myFunction():Nil[0]{print('yeah22!', 1);}");
            this.compileAndExecute("myFunction():Nil[0]");
            Assert.assertEquals("'yeah22!'", this.functionExecution.getConsole().getLine(0));
        }
        this.runtime.delete("myId.pure");
        this.runtime.createInMemorySource("myId.pure", "function myFunction():Nil[0]{print('yeah!', 1);}");
        this.compileAndExecute("myFunction():Nil[0]");
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
