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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGraphStability extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeFunction()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{[]}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{[]}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionWithGeneric()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function testPkg::test():Class<Any>[0]{[]}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Class<Any>[0]{[]}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeFunctionWithBody()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("other.pure", "function testPkg::ok():Nil[0]{[]}")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{testPkg::ok()}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{testPkg::ok()}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeFunctionWithComplexBody()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{let i=1;['aaa','bbb']->at($i);[];}"
                        )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{let i = 1;['aaa','bbb']->at($i);[];}"
                        )
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeFunctionWithComplexBodyUsingLambdas()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{let i=1;['aaa','bbb']->filter(s|$s == 'aaa');[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Nil[0]{let i=1;['aaa','bbb']->filter(s|$s == 'aaa');[];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGenericType()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("other.pure", "Class testPkg::XX{}")
                        .createInMemorySource("sourceId.pure", "function test():Any[*]{@testPkg::XX}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("other.pure")
                        .compileWithExpectedCompileFailure("testPkg::XX has not been defined!", "sourceId.pure", 1, 34)
                        .createInMemorySource("other.pure", "Class testPkg::XX{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGenericTypeCast()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("other.pure", "Class testPkg::XX{}")
                        .createInMemorySource("sourceId.pure", "function testPkg::test():Any[*]{let a = []->cast(@testPkg::XX)}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("other.pure")
                        .compileWithExpectedCompileFailure("testPkg::XX has not been defined!", "sourceId.pure", 1, 60)
                        .createInMemorySource("other.pure", "Class testPkg::XX{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testImport()
    {
        CoreInstance systemImports = this.runtime.getCoreInstance("system::imports");
        Assert.assertNotNull(systemImports);
        String sourceId = "/system/source.pure";
        String source = "import meta::testPkg::*;\n" +
                "\n" +
                "function meta::testPkg::testFn(i:Integer[1]):Integer[1]\n" +
                "{\n" +
                "   if ($i <= 0, |$i, |testFn($i - 1))\n" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(sourceId, source)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(sourceId)
                        .createInMemorySource(sourceId, source)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
