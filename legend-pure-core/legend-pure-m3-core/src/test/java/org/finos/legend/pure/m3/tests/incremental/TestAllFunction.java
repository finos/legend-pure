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

package org.finos.legend.pure.m3.tests.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAllFunction extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
    }

    @Test
    public void testPureRuntimeClassAllStability() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == Class.all()->filter(c|$c.name == 'A')->size(), |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeSimpleFunctionDefinitionAllStability() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function f():Nil[0]{[]}")
                        .createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == ConcreteFunctionDefinition.all()->filter(c|$c.name == 'f__Nil_0_')->size(), |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function f():Nil[0]{[]}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeSimpleFunctionDefinitionLambdaAllStability() throws Exception
    {
        int size = this.repository.serialize().length;

        int oldValue = 0;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "Class A\n" +
                    "{\n" +
                    "     b:String[*];\n" +
                    "     a(s:String[1]){$this.b->filter(a|true)->at(0)}:String[1];\n" +
                    "}\n" +
                    "function test():Integer[1]\n" +
                    "{\n" +
                    "     ConcreteFunctionDefinition.all()->size();\n" +
                    "}\n");
            this.runtime.compile();
            int newValue = this.context.getClassifierInstances(this.runtime.getCoreInstance(M3Paths.ConcreteFunctionDefinition)).size();
            if (oldValue != 0 && oldValue != newValue)
            {
                throw new RuntimeException(oldValue + " != " + newValue);
            }
            oldValue = newValue;
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals(size, this.repository.serialize().length);
        }
    }
}
