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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.junit.Test;

public class TestPureRuntimeFunction_All extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testPureRuntimeFunctionAllClass() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{print(A.all(),1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 28)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionAllFunctionsIncludingLambda() throws Exception
    {
        String source = "Profile PR1\n" +
                "{\n" +
                "   tags : [Contract, DataSet, ValidationRule];\n" +
                "}\n" +
                "\n" +
                "function {PR1.Contract='eee'} go():Nil[0]\n" +
                "{\n" +
                "   print(ConcreteFunctionDefinition.all()->filter(f|!$f.name->isEmpty() && isContract($f)).name,3);\n" +
                "}\n" +
                "\n" +
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
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeFunctionAllFunctionsIncludingLambdaInNew() throws Exception
    {
        String source = "Class U{}" +
                "Class T{val:U[*];}\n" +
                "function go():Nil[0]\n" +
                "{\n" +
                "   print(ConcreteFunctionDefinition.all()->map(c|^T(val=[1,2]->map(c|^U()))),1);\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", source)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeFunctionLambda() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{A.all()->filter(c|$c.version == 2);[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 22)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeFunctionLambdaWithinIf() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{if(true, |if(false,|A.all()->filter(c|$c.version == 2);[];,|[]),|[])}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 42)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionLambdaWithinIf2() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{if(true, |if(false,| let v = 5+A.all()->filter(c|$c.version == 2)->toOne().version+2;[];,|[]),|[])}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 53)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionWithTypeParameter() throws Exception
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
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
