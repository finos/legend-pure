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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLambdaAsInstanceValue extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
        runtime.compile();
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
        CoreInstance test = runtime.getFunction("test::testFn():Any[1]");
        CoreInstance result = functionExecution.start(test, Lists.immutable.empty());
        Assert.assertEquals("LAMBDA", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository system = GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME);
        CodeRepository test = GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system");
        repositories.add(system);
        repositories.add(test);
        return repositories;
    }
}