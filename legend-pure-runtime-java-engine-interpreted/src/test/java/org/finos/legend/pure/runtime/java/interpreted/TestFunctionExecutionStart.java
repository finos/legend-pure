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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionExecutionStart extends AbstractPureTestWithCoreCompiled
{
    static FunctionExecutionInterpreted functionExecution = new FunctionExecutionInterpreted();
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
    public void testStartWithNotEnoughArguments()
    {
        compileTestSource("fromString.pure","function testFn(s1:String[1], s2:String[1]):String[1] { $s1 + $s2 }");
        CoreInstance func = this.runtime.getFunction("testFn(String[1], String[1]):String[1]");
        Assert.assertNotNull(func);

        try
        {
            this.functionExecution.start(func, Lists.immutable.<CoreInstance>with());
        }
        catch (Exception e)
        {
            assertPureException(PureExecutionException.class, "Error executing the function:testFn(String[1], String[1]):String[1]. Mismatch between the number of function parameters (2) and the number of supplied arguments (0)\n", e);
        }

        try
        {
            this.functionExecution.start(func, Lists.immutable.with(ValueSpecificationBootstrap.newStringLiteral(this.repository, "string", this.processorSupport)));
        }
        catch (Exception e)
        {
            assertPureException(PureExecutionException.class, "Error executing the function:testFn(String[1], String[1]):String[1]. Mismatch between the number of function parameters (2) and the number of supplied arguments (1)\n" +
                                                              "Anonymous_StripedId instance InstanceValue\n" +
                                                              "    genericType(Property):\n" +
                                                              "        Anonymous_StripedId instance GenericType\n" +
                                                              "            rawType(Property):\n" +
                                                              "                String instance PrimitiveType\n" +
                                                              "    multiplicity(Property):\n" +
                                                              "        PureOne instance PackageableMultiplicity\n" +
                                                              "    values(Property):\n" +
                                                              "        string instance String", e);
        }
    }

    @Test
    public void testStartWithTooManyArguments()
    {
        compileTestSource("fromString.pure","function testFn():String[1] { 'the quick brown fox jumps over the lazy dog' }");
        CoreInstance func = this.runtime.getFunction("testFn():String[1]");
        Assert.assertNotNull(func);
        try
        {
            this.functionExecution.start(func, Lists.mutable.with(ValueSpecificationBootstrap.newStringLiteral(this.repository, "string", this.processorSupport)));
        }
        catch (Exception e)
        {
            assertPureException(PureExecutionException.class, "Error executing the function:testFn():String[1]. Mismatch between the number of function parameters (0) and the number of supplied arguments (1)\n" +
                                                              "Anonymous_StripedId instance InstanceValue\n" +
                                                              "    genericType(Property):\n" +
                                                              "        Anonymous_StripedId instance GenericType\n" +
                                                              "            rawType(Property):\n" +
                                                              "                String instance PrimitiveType\n" +
                                                              "    multiplicity(Property):\n" +
                                                              "        PureOne instance PackageableMultiplicity\n" +
                                                              "    values(Property):\n" +
                                                              "        string instance String", e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return functionExecution;
    }
}
