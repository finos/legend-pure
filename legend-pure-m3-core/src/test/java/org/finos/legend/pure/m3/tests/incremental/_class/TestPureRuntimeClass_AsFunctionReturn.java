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

package org.finos.legend.pure.m3.tests.incremental._class;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestPureRuntimeClass_AsFunctionReturn extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testPureRuntimeClassAsFunctionReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function q(a:A[0]):Nil[0]{[];}" +
                                "function test():Nil[0]{q(f()->cast(@A));}")
                        .createInMemorySource("other.pure", "function f():Nil[0]{[]}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsFunctionReturnFunctionTypeFunctionType()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .createInMemorySource(
                                "/test/testFileB.pure",
                                "import test::*;\n" +
                                        "function test::testFn():Function<{FunctionDefinition<{->TestClassA[1]}>[1]->String[1]}>[0..1]\n" +
                                        "{\n" +
                                        "    []\n" +
                                        "}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/testFileA.pure")
                        .compileWithExpectedCompileFailure("TestClassA has not been defined!", "/test/testFileB.pure", 2, 57)
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaFunctionReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function execute<T>(p:Function<{->T[*]}>[1]):T[*]{$p->eval()}" +
                                "function test():A[*]{execute(|A.all())}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 78)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsFunctionReturnError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function q(a:A[0]):Nil[0]{[];}" +
                                "function test():Nil[0]{q(f());}")
                        .createInMemorySource("other.pure", "function f():A[0]{[]}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f(^B(a=^A()).a);}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f(^B(a=^A()).a);}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class C{} Class B{a:A[1];}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .updateSource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnCrossFiles()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "Class B{a:A[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 11)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnTypeArgumentCrossFiles()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .createInMemorySource(
                                "/test/testFileB.pure",
                                "import test::*;\n" +
                                        "Class test::TestClassB\n" +
                                        "{\n" +
                                        "    prop:List<TestClassA>[1];\n" +
                                        "}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/testFileA.pure")
                        .compileWithExpectedCompileFailure("TestClassA has not been defined!", "/test/testFileB.pure", 4, 15)
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnFunctionTypeCrossFiles()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .createInMemorySource(
                                "/test/testFileB.pure",
                                "import test::*;\n" +
                                        "Class test::TestClassB\n" +
                                        "{\n" +
                                        "    prop:Function<{->TestClassA[1]}>[0..1];\n" +
                                        "}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/testFileA.pure")
                        .compileWithExpectedCompileFailure("TestClassA has not been defined!", "/test/testFileB.pure", 4, 22)
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnFunctionTypeFunctionTypeCrossFiles()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .createInMemorySource(
                                "/test/testFileB.pure",
                                "import test::*;\n" +
                                        "Class test::TestClassB\n" +
                                        "{\n" +
                                        "    prop:Function<{FunctionDefinition<{->TestClassA[1]}>[1]->String[1]}>[0..1];\n" +
                                        "}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/testFileA.pure")
                        .compileWithExpectedCompileFailure("TestClassA has not been defined!", "/test/testFileB.pure", 4, 42)
                        .createInMemorySource(
                                "/test/testFileA.pure",
                                "Class test::TestClassA\n" +
                                        "{\n" +
                                        "}\n")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyReturnCrossFilesError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "Class B{a:A[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 11)
                        .createInMemorySource("sourceId.pure", "Class C{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 11)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsPropertyWithInheritanceReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} Class C{a:A[1];} Class B extends C{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f(^B(a=^A()).a);}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{} Class C{a:A[1];} Class B extends C{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

        Assert.assertEquals("B instance Class\n" +
                            "    classifierGenericType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            [... >0]\n" +
                            "    generalizations(Property):\n" +
                            "        Anonymous_StripedId instance Generalization\n" +
                            "            [... >0]\n" +
                            "    name(Property):\n" +
                            "        B instance String\n" +
                            "    package(Property):\n" +
                            "        Root instance Package\n" +
                            "    referenceUsages(Property):\n" +
                            "        Anonymous_StripedId instance ReferenceUsage\n" +
                            "            [... >0]", this.runtime.getCoreInstance("B").printWithoutDebug("",0));
    }

    @Test
    public void testPureRuntimeClassAsPropertyWithInheritanceReturnError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} Class C{a:A[1];} Class B extends C{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f(^B(a=^A()).a);}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{} Class C{} Class B extends C{}")
                        .compileWithExpectedCompileFailure("Can't find the property 'a' in the class B", "userId.pure", 1, 37)
                        .updateSource("sourceId.pure", "Class A{} Class C{a:A[1];} Class B extends C{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsQualifiedPropertyReturn() throws Exception
    {
        ImmutableMap<String, String> sources = Maps.immutable.with(
                "source1.pure", "Class A{}",
                "source2.pure", "Class B{prop(){^A()}:A[1];}"
        );
        this.runtime.createInMemorySource("source1.pure", sources.get("source1.pure"));
        this.runtime.createInMemorySource("source2.pure", sources.get("source2.pure"));
        this.runtime.compile();

        int size = this.repository.serialize().length;
        CoreInstance classA = this.processorSupport.package_getByUserPath("A");
        CoreInstance classB = this.processorSupport.package_getByUserPath("B");
        CoreInstance prop = _Class.getQualifiedPropertiesByName(classB, this.processorSupport).get("prop()");
        Assert.assertSame(classA, Instance.getValueForMetaPropertyToOneResolved(prop, M3Properties.genericType, M3Properties.rawType, this.processorSupport));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("source1.pure");
            try
            {
                this.runtime.compile();
                Assert.fail("Expected a compile exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", "source2.pure", e);
            }

            this.runtime.createInMemorySource("source1.pure", sources.get("source1.pure"));
            this.runtime.compile();

            classA = this.processorSupport.package_getByUserPath("A");
            classB = this.processorSupport.package_getByUserPath("B");
            prop = _Class.getQualifiedPropertiesByName(classB, this.processorSupport).get("prop()");
            Assert.assertSame(classA, Instance.getValueForMetaPropertyToOneResolved(prop, M3Properties.genericType, M3Properties.rawType, this.processorSupport));

            Assert.assertEquals(size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureRuntimeClassAsLambdaReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({|^A()}->eval());}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaReturnError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({|^A()}->eval());}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .createInMemorySource("sourceId.pure", "Class C{} Class B{a:A[1];}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "other.pure", 1, 14)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassAsFunctionReturnUsingParam()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function q(a:A[1]):Nil[0]{[];}" +
                                "function test():Nil[0]{let p=f(); q($p);}")
                        .createInMemorySource("other.pure", "function f():A[1]{^A()}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassAsFunctionReturnUsingParamError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function q(a:A[1]):Nil[0]{[];}" +
                                "function test():Nil[0]{let p=f(); q($p);}")
                        .createInMemorySource("other.pure", "function f():A[1]{^A()}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }
}
