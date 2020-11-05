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

package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLambdaAsInstanceValue extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
//        try {
//            runtime.compile();
//        } catch(PureCompilationException e) {
//            setUp();
//        }
    }

    @Test
    public void testLambdaWithNoArgsAsInstanceValue()
    {
        testLambdaWithNArgsAsInstanceValue(0);
    }

    @Test
    public void testLambdaWithOneArgAsInstanceValue()
    {
        testLambdaWithNArgsAsInstanceValue(1);
    }

    @Test
    public void testLambdaWithTwoArgsAsInstanceValue()
    {
        testLambdaWithNArgsAsInstanceValue(2);
    }

    @Test
    public void testLambdaWithThreeArgsAsInstanceValue()
    {
        testLambdaWithNArgsAsInstanceValue(3);
    }

    @Test
    public void testLambdaWithFourArgsAsInstanceValue()
    {
        testLambdaWithNArgsAsInstanceValue(4);
    }

    private void testLambdaWithNArgsAsInstanceValue(int n)
    {
        compileTestSource("/test/testSource1.pure",
                "function test::testGetFunctionName(f:Function<Any>[1]):String[1]\n" +
                        "{\n" +
                        "  let name = $f.functionName;\n" +
                        "  if($name->isEmpty(), |'LAMBDA', |$name->toOne());\n" +
                        "}");

        StringBuilder lambda = new StringBuilder("{");
        for (int i = 1; i <= n; i++)
        {
            if (i > 1)
            {
                lambda.append(", ");
            }
            lambda.append("arg");
            lambda.append(i);
            lambda.append(":String[1]");
        }
        lambda.append(" | 'the quick brown fox'}");
        compileTestSource("/test/testSource2.pure",
                "import test::*;\n" +
                        "function test::testFn():Any[1]\n" +
                        "{\n" +
                        "  testGetFunctionName(" + lambda + ")" +
                        "}");
        CoreInstance test = this.runtime.getFunction("test::testFn():Any[1]");
        CoreInstance result = this.functionExecution.start(test, Lists.immutable.<CoreInstance>empty());
        Assert.assertEquals("LAMBDA", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}