// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestIfCompiled extends AbstractPureTestWithCoreCompiled
{
    private static final List<StringJavaSource> capturedJavaSources = new ArrayList<>();

    private static final JavaCompilerEventObserver capturingObserver = new JavaCompilerEventObserver()
    {
        @Override
        public void startGeneratingJavaFiles(String compileGroup)
        {
        }

        @Override
        public void endGeneratingJavaFiles(String compileGroup, RichIterable<StringJavaSource> sources)
        {
            sources.forEach(capturedJavaSources::add);
        }

        @Override
        public void startCompilingJavaFiles(String compileGroup)
        {
        }

        @Override
        public void endCompilingJavaFiles(String compileGroup)
        {
        }
    };

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testUnAssignedIfInFuncExpression()
    {
        compileTestSource(
                "fromString.pure",
                "function meta::pure::functions::lang::tests::if::testUnAssignedIfInFuncExpression():String[1]\n" +
                        "{\n" +
                        "   let ifVar =  if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););\n" +
                        "   if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););\n" +
                        "   if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););\n" +
                        "   let iff = if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););\n" +
                        "   if(true, | let b = 'true'; if(true, | let b = 'true', | 'false'); let bb = 'bb';, | let c = 'see');\n" +
                        "   let a = 'be';\n" +
                        "   if(true, | let b = 'true', | 'false');\n" +
                        "}");
    }

    @Test
    public void testIfWithDifferentMultiplicities()
    {
        compileTestSource(
                "fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "  id : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "function testFn(ids:Integer[*]):A[*]\n" +
                        "{\n" +
                        "  if ($ids->isEmpty(),\n" +
                        "      | let id = -1;\n" +
                        "        ^A(id=$id);,\n" +
                        "      | let newIds = $ids->tail();\n" +
                        "        $ids->map(id | $newIds->testFn());)\n" +
                        "}");
    }

    @Test
    public void testIfWithFloatVersusInteger()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", "import test::*;\n" +
                "function test::testFn(test:Boolean[1]):Number[1]\n" +
                "{\n" +
                "    let result = if($test, |1.0, |3);\n" +
                "    $result;\n" +
                "}\n" +
                "\n" +
                "function test::testTrue():Any[*]\n" +
                "{\n" +
                "  let result = testFn(true);\n" +
                "  assert(1.0 == $result, |'');\n" +
                "  $result;\n" +
                "}\n" +
                "\n" +
                "function test::testFalse():Any[*]\n" +
                "{\n" +
                "  let result = testFn(false);\n" +
                "  assert(3 == $result, |'');\n" +
                "  $result;\n" +
                "}\n");

        CoreInstance testTrue = runtime.getFunction("test::testTrue():Any[*]");
        Assert.assertNotNull(testTrue);
        CoreInstance resultTrue = functionExecution.start(testTrue, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultTrue);
        InstanceValue trueInstanceValue = (InstanceValue) resultTrue;
        Verify.assertSize(1, trueInstanceValue._values());
        Object trueValue = trueInstanceValue._values().getFirst();
        Verify.assertInstanceOf(Double.class, trueValue);
        Assert.assertEquals(1.0d, trueValue);

        CoreInstance testFalse = runtime.getFunction("test::testFalse():Any[*]");
        Assert.assertNotNull(testFalse);
        CoreInstance resultFalse = functionExecution.start(testFalse, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultFalse);
        InstanceValue falseInstanceValue = (InstanceValue) resultFalse;
        Verify.assertSize(1, falseInstanceValue._values());
        Object falseValue = falseInstanceValue._values().getFirst();
        Verify.assertInstanceOf(Long.class, falseValue);
        Assert.assertEquals(3L, falseValue);
    }

    @Test
    public void testIfNullBranchToMany()
    {
        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "function test::ifNullBranch(flag:Boolean[1]):String[*]\n" +
                        "{\n" +
                        "    if($flag, |[], |['hello', 'world'])\n" +
                        "}\n" +
                        "\n" +
                        "function test::testTrue():Any[*]\n" +
                        "{\n" +
                        "  test::ifNullBranch(true);\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFalse():Any[*]\n" +
                        "{\n" +
                        "  test::ifNullBranch(false);\n" +
                        "}\n");

        CoreInstance testTrue = runtime.getFunction("test::testTrue():Any[*]");
        Assert.assertNotNull(testTrue);
        CoreInstance resultTrue = functionExecution.start(testTrue, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultTrue);
        InstanceValue trueInstanceValue = (InstanceValue) resultTrue;
        Verify.assertSize(0, trueInstanceValue._values());

        CoreInstance testFalse = runtime.getFunction("test::testFalse():Any[*]");
        Assert.assertNotNull(testFalse);
        CoreInstance resultFalse = functionExecution.start(testFalse, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultFalse);
        InstanceValue falseInstanceValue = (InstanceValue) resultFalse;
        Verify.assertSize(2, falseInstanceValue._values());
        Assert.assertEquals("hello", falseInstanceValue._values().toList().get(0));
        Assert.assertEquals("world", falseInstanceValue._values().toList().get(1));
    }

    @Test
    public void testIfNullBranchToManyWithClass()
    {
        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "Class test::Item { name: String[1]; }\n" +
                        "function test::ifNullBranchClass(flag:Boolean[1]):test::Item[*]\n" +
                        "{\n" +
                        "    if($flag, |[], |^test::Item(name='a'))\n" +
                        "}\n" +
                        "\n" +
                        "function test::testClassTrue():Any[*]\n" +
                        "{\n" +
                        "  test::ifNullBranchClass(true);\n" +
                        "}\n" +
                        "\n" +
                        "function test::testClassFalse():Any[*]\n" +
                        "{\n" +
                        "  test::ifNullBranchClass(false);\n" +
                        "}\n");

        CoreInstance testTrue = runtime.getFunction("test::testClassTrue():Any[*]");
        Assert.assertNotNull(testTrue);
        CoreInstance resultTrue = functionExecution.start(testTrue, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultTrue);
        InstanceValue trueInstanceValue = (InstanceValue) resultTrue;
        Verify.assertSize(0, trueInstanceValue._values());

        CoreInstance testFalse = runtime.getFunction("test::testClassFalse():Any[*]");
        Assert.assertNotNull(testFalse);
        CoreInstance resultFalse = functionExecution.start(testFalse, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultFalse);
        InstanceValue falseInstanceValue = (InstanceValue) resultFalse;
        Verify.assertSize(1, falseInstanceValue._values());
    }

    @Test
    public void testIfNonNullBranchesToMany()
    {
        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "function test::ifNonNull(flag:Boolean[1]):String[*]\n" +
                        "{\n" +
                        "    if($flag, |['a', 'b'], |['c', 'd'])\n" +
                        "}\n" +
                        "\n" +
                        "function test::testNonNullTrue():Any[*]\n" +
                        "{\n" +
                        "  test::ifNonNull(true);\n" +
                        "}\n" +
                        "\n" +
                        "function test::testNonNullFalse():Any[*]\n" +
                        "{\n" +
                        "  test::ifNonNull(false);\n" +
                        "}\n");

        CoreInstance testTrue = runtime.getFunction("test::testNonNullTrue():Any[*]");
        Assert.assertNotNull(testTrue);
        CoreInstance resultTrue = functionExecution.start(testTrue, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultTrue);
        InstanceValue trueInstanceValue = (InstanceValue) resultTrue;
        Verify.assertSize(2, trueInstanceValue._values());
        Assert.assertEquals("a", trueInstanceValue._values().toList().get(0));
        Assert.assertEquals("b", trueInstanceValue._values().toList().get(1));

        CoreInstance testFalse = runtime.getFunction("test::testNonNullFalse():Any[*]");
        Assert.assertNotNull(testFalse);
        CoreInstance resultFalse = functionExecution.start(testFalse, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultFalse);
        InstanceValue falseInstanceValue = (InstanceValue) resultFalse;
        Verify.assertSize(2, falseInstanceValue._values());
        Assert.assertEquals("c", falseInstanceValue._values().toList().get(0));
        Assert.assertEquals("d", falseInstanceValue._values().toList().get(1));
    }

    @Test
    public void testIfNullBranchToManyGeneratedCodePattern()
    {
        capturedJavaSources.clear();

        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "function test::ifNullBranchCodeGen(flag:Boolean[1]):String[*]\n" +
                        "{\n" +
                        "    if($flag, |[], |['hello', 'world'])\n" +
                        "}\n");

        String matchingCode = null;
        for (StringJavaSource source : capturedJavaSources)
        {
            String code = source.getCode();
            if (code.contains("ifNullBranchCodeGen"))
            {
                matchingCode = code;
                break;
            }
        }

        Assert.assertNotNull("Should find generated source containing ifNullBranchCodeGen", matchingCode);
        Assert.assertTrue("Generated code should use explicit type parameter for toPureCollection with null",
                matchingCode.contains("CompiledSupport.<") && matchingCode.contains(">toPureCollection(null)"));
        Assert.assertFalse("Generated code should not use (java.lang.Object)null cast",
                matchingCode.contains("(java.lang.Object)null"));
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().withJavaCompilerEventObserver(capturingObserver).build();
    }
}
