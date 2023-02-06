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

package org.finos.legend.pure.m3.tests.incremental.function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_All extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository system = GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME, "platform_functions");
        CodeRepository test = GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system", "platform_functions");
        repositories.add(system);
        repositories.add(test);
        return repositories;
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("/test/model.pure");
        runtime.delete("/test/function.pure");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeFunctionAllClass()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Any[*]{A.all();}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 22)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionAllFunctionsIncludingLambda()
    {
        String source = "Profile PR1\n" +
                "{\n" +
                "   tags : [Contract, DataSet, ValidationRule];\n" +
                "}\n" +
                "\n" +
                "function {PR1.Contract='eee'} go():Any[*]\n" +
                "{\n" +
                "   ConcreteFunctionDefinition.all()->filter(f|!$f.name->isEmpty() && isContract($f)).name;\n" +
                "}\n" +
                "function meta::pure::functions::meta::tag(profile:Profile[1], str:String[1]):Tag[1]" +
                "{" +
                "   $profile.p_tags->at(0);" +
                "}\n" +
                "function isContract(f:ConcreteFunctionDefinition<Any>[1]):Boolean[1]\n" +
                "{\n" +
                "   $f.taggedValues->filter(t|$t.tag == PR1->tag('Contract'))->size() > 0;\n" +
                "}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", source)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeFunctionAllFunctionsIncludingLambdaInNew()
    {
        String source = "Class U{}" +
                "Class T{val:U[*];}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   ConcreteFunctionDefinition.all()->map(c|^T(val=[1,2]->map(c|^U())));\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", source)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeFunctionLambda()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{A.all()->filter(c|$c.version == 2);[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 22)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeFunctionLambdaWithinIf()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{if(true, |if(false,|A.all()->filter(c|$c.version == 2);[];,|[]),|[])}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 42)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionLambdaWithinIf2()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{if(true, |if(false,| let v = 5+A.all()->filter(c|$c.version == 2)->toOne().version+2;[];,|[]),|[])}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 53)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionWithTypeParameter()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(
                                "/test/model.pure",
                                "Class test::TestClass1\n" +
                                        "{\n" +
                                        "  prop : String[1];\n" +
                                        "}\n")
                        .createInMemorySource(
                                "/test/function.pure",
                                "import test::*;\n" +
                                        "\n" +
                                        "Class test::TestClass2<T>\n" +
                                        "{\n" +
                                        "    func : Function<{T[1]->String[1]}>[1];\n" +
                                        "}\n" +
                                        "\n" +
                                        "function test::testFn<S>(s:S[1], tc2:TestClass2<S>[1]):String[1]\n" +
                                        "{\n" +
                                        "    let mc = ^TestClass1(prop=$tc2.func->eval($s));\n" +
                                        "    $mc.prop;" +
                                        "}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/model.pure")
                        .compileWithExpectedCompileFailure("TestClass1 has not been defined!", "/test/function.pure", 10, 15)
                        .createInMemorySource(
                                "/test/model.pure",
                                "Class test::TestClass1\n" +
                                        "{\n" +
                                        "  prop : String[1];\n" +
                                        "}\n")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }
}
