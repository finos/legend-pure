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

package org.finos.legend.pure.runtime.java.interpreted.runtime.api;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionExecutionStart extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(new FunctionExecutionInterpreted());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testStartWithNotEnoughArguments()
    {
        compileTestSource("fromString.pure", "function testFn(s1:String[1], s2:String[1]):String[1] { $s1 + $s2 }");
        CoreInstance func = runtime.getFunction("testFn(String[1], String[1]):String[1]");
        Assert.assertNotNull(func);

        PureExecutionException e1 = Assert.assertThrows(PureExecutionException.class, () -> functionExecution.start(func, Lists.immutable.with()));
        assertPureException(PureExecutionException.class, "Error executing the function:testFn(String[1], String[1]):String[1]. Mismatch between the number of function parameters (2) and the number of supplied arguments (0)\n", e1);

        PureExecutionException e2 = Assert.assertThrows(PureExecutionException.class, () -> functionExecution.start(func, Lists.immutable.with(ValueSpecificationBootstrap.newStringLiteral(repository, "string", processorSupport))));
        assertPureException(PureExecutionException.class, "Error executing the function:testFn(String[1], String[1]):String[1]. Mismatch between the number of function parameters (2) and the number of supplied arguments (1)\n" +
                "Anonymous_StripedId instance InstanceValue\n" +
                "    genericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                String instance PrimitiveType\n" +
                "    multiplicity(Property):\n" +
                "        PureOne instance PackageableMultiplicity\n" +
                "    values(Property):\n" +
                "        string instance String", e2);
    }

    @Test
    public void testStartWithTooManyArguments()
    {
        compileTestSource("fromString.pure", "function testFn():String[1] { 'the quick brown fox jumps over the lazy dog' }");
        CoreInstance func = runtime.getFunction("testFn():String[1]");
        Assert.assertNotNull(func);

        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> functionExecution.start(func, Lists.mutable.with(ValueSpecificationBootstrap.newStringLiteral(repository, "string", processorSupport))));
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

    @Test
    public void testSimplePure()
    {
        compileTestSource("fromString.pure", "###Pure\n" +
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
        Assert.assertEquals("'yeah!'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testCallStackInExecutionExceptionPure()
    {
        compileTestSource("fromString.pure",
                "###Pure\n" +
                        "   function go():Boolean[1]\n" +
                        "   {\n" +
                        "       nest();\n" +
                        "       true;\n" +
                        "   }\n" +
                        "function nest():Boolean[1]" +
                        "{" +
                        "   fail();" +
                        "}");

        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("go():Boolean[1]"));
        String trace = e.printPureStackTrace(new StringBuilder(), "   ", functionExecution.getProcessorSupport()).toString();
        Assert.assertEquals(
                "   1: resource:/platform/pure/essential/tests/assert.pure line:26 column:5\n" +
                        "\n" +
                "   Full stacktrace:\n" +
                "     at assert(Boolean[1], Function<{->String[1]}>[1]):Boolean[1] (resource:/platform/pure/essential/tests/assert.pure line:26 column:5)\n" +
                "     at assert(Boolean[1], String[1]):Boolean[1] (resource:/platform/pure/essential/tests/assert.pure line:31 column:5)\n" +
                "     at assert(Boolean[1]):Boolean[1] (resource:/platform/pure/essential/tests/fail.pure line:19 column:5)\n" +
                "     at fail():Boolean[1] (resource:fromString.pure line:7 column:31)\n" +
                "     at nest():Boolean[1] (resource:fromString.pure line:4 column:8)", trace);
    }
}
