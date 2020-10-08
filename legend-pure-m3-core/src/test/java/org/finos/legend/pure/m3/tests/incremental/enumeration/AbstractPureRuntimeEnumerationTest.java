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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.junit.Test;

public abstract class AbstractPureRuntimeEnumerationTest extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testPureRuntimeEnumerationAsReference() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{print(myEnum.VAL1,1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 30)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReferenceValueError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{print(test::myEnum.VAL1,1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 36)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{XXX, VAL2}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL1' can't be found in the enumeration test::myEnum", "userId.pure", 1, 43)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReferenceTypeError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{print(myEnum.VAL1,1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 30)
                        .createInMemorySource("sourceId.pure", "Enum myErrorEnum{VAL1, VAL2}")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 30)
                        .updateSource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReturn() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func():myEnum[1]{myEnum.VAL2}" +
                                "function test():Nil[0]{print(func(),1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeEnumerationAsReturnError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func():test::myEnum[1]{test::myEnum.VAL2}\n" +
                                "function test():Nil[0]{print(func(),1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 23)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL2' can't be found in the enumeration test::myEnum", "userId.pure", 1, 46)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeEnumerationAsParameter() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func(f:myEnum[1]):Boolean[1]{true}" +
                                "function test():Nil[0]{print(func(myEnum.VAL1),1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("myEnum has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeEnumerationAsParameterError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function func(f:test::myEnum[1]):Boolean[1]{true}\n" +
                                "function test():Nil[0]{print(func(test::myEnum.VAL1),1);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("test::myEnum has not been defined!", "userId.pure", 1, 23)
                        .createInMemorySource("sourceId.pure", "Enum test::myEnum{VAL2}")
                        .compileWithExpectedCompileFailure("The enum value 'VAL1' can't be found in the enumeration test::myEnum", "userId.pure", 2, 48)
                        .updateSource("sourceId.pure", "Enum test::myEnum{VAL1, VAL2}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeDeleteEnumeration() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", "Enum myEnum{VAL1, VAL2}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{print(myEnum.VAL1,1);}")
                        .compile()
                        .deleteSource("sourceId.pure")
                        .deleteSource("userId.pure")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeModifyEnumerationWithTaggedValue() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Enum myEnum{ {testProfile.t1='bb'} VAL1, VAL2}")
                .createInMemorySource("userId.pure", "Profile testProfile{tags:[t1,t2];}")
                .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "////Some comment\n" +
                                "Enum myEnum{ {testProfile.t1='bb'} VAL1, VAL2}")
                        .compile(),

                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }
}
