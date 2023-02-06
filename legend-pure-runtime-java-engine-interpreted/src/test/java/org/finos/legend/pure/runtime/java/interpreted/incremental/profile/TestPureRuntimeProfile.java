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

package org.finos.legend.pure.runtime.java.interpreted.incremental.profile;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeProfile extends AbstractPureTestWithCoreCompiled
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
    public void testPureRuntimeProfilePointer() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}");
        this.runtime.createInMemorySource("userId.pure", "function a():Profile[1]{testProfile}" +
                                               "function go():Nil[0]{a();[];}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;

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
                assertPureException(PureCompilationException.class, "testProfile has not been defined!", "userId.pure", 1, 25, e);
            }

            this.runtime.createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
        }
    }


    @Test
    public void testPureRuntimeProfilePointerError() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}");
        this.runtime.createInMemorySource("userId.pure", "function a():Profile[1]{testProfile}" +
                                               "function go():Nil[0]{a();[];}");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;

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
                assertPureException(PureCompilationException.class, "testProfile has not been defined!", "userId.pure", 1, 25, e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "Profile testOtherProfile{stereotypes:[s1,s2];}");
                this.runtime.compile();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "testProfile has not been defined!", "userId.pure", 1, 25, e);
            }
        }
        this.runtime.modify("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
