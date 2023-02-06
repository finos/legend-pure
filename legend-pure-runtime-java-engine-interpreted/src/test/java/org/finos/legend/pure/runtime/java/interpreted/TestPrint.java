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

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPrint extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFunctionPrint()
    {
        this.runtime.createInMemoryAndCompile(Tuples.pair("testSource.pure",
                "function testFunction():String[1]\n" +
                        "{\n" +
                        "   'Test'\n" +
                        "}\n" +
                "function testFunction2():String[1]\n" +
                        "{\n" +
                        "   testFunction()\n" +
                        "}\n"),
                Tuples.pair(
                "testSource2.pure",
                "function go():Nil[0]\n" +
                        "{\n" +
                        "   print(testFunction__String_1_,0);\n" +
                        "}"
        ));
        this.execute("go():Nil[0]");
        Assert.assertEquals("testFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                "    applications(Property):\n" +
                "        [>0] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "    classifierGenericType(Property):\n" +
                "        [>0] Anonymous_StripedId instance GenericType\n" +
                "    expressionSequence(Property):\n" +
                "        [>0] Anonymous_StripedId instance InstanceValue\n" +
                "    functionName(Property):\n" +
                "        [>0] testFunction instance String\n" +
                "    name(Property):\n" +
                "        [>0] testFunction__String_1_ instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package\n" +
                "    referenceUsages(Property):\n" +
                "        [>0] Anonymous_StripedId instance ReferenceUsage", this.functionExecution.getConsole().getLine(0));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
