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

package org.finos.legend.pure.m3.tests.incremental.enumeration;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractPureRuntimeEnumerationTest extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeEnumerationAsReference()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Any[1]{myEnum.VAL1;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 24)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReferenceValueError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Any[1]{test::myEnum.VAL1;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 30)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{XXX, VAL2}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL1' can't be found in the enumeration test::myEnum", "userId.pure", 1, 37)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReferenceTypeError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Any[1]{myEnum.VAL1;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 24)
                        .createInMemorySource("sourceId.pure", "Enum myErrorEnum{VAL1, VAL2}")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 24)
                        .updateSource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReturn()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func():myEnum[1]{myEnum.VAL2}" +
                                "function test():Any[1]{func();}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReturnError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func():test::myEnum[1]{test::myEnum.VAL2}\n" +
                                "function test():Any[1]{func();}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 23)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL2' can't be found in the enumeration test::myEnum", "userId.pure", 1, 46)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeEnumerationAsParameter()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func(f:myEnum[1]):Boolean[1]{true}" +
                                "function test():Any[1]{func(myEnum.VAL1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeEnumerationAsParameterError()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func(f:test::myEnum[1]):Boolean[1]{true}\n" +
                                "function test():Any[1]{func(test::myEnum.VAL1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 23)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL2}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL1' can't be found in the enumeration test::myEnum", "userId.pure", 2, 42)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeDeleteEnumeration()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Any[1]{myEnum.VAL1;}")
                        .compile()
                        .deleteSource("sourceId.pure")
                        .deleteSource("userId.pure")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeModifyEnumerationWithTaggedValue()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{ {testProfile.t1='bb'} VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "////Some comment\n" +
                                "Enum myEnum{ {testProfile.t1='bb'} VAL1, VAL2}")
                        .compile(),

                runtime, functionExecution, this.getAdditionalVerifiers());
    }
}
