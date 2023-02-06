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

package org.finos.legend.pure.m3.tests.incremental.profile;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeTaggedValue extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testPureRuntimeProfileTaggedValueClass() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .createInMemorySource("userId.pure", "Class {testProfile.t1='bb'} A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 20)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeProfileTaggedValueClassError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .createInMemorySource("userId.pure", "Class {testProfile.t1='bb'} A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 20)
                        .createInMemorySource("sourceId.pure", "Profile testProfileXX{tags:[t1,t2];}")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 20)
                        .updateSource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeProfileStereotypeClassValueError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .createInMemorySource("userId.pure", "Class {testProfile.t1='bb'} A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 20)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t4,t2];}")
                        .compileWithExpectedCompileFailure("The tag 't1' can't be found in profile 'testProfile'", "userId.pure", 1, 20)
                        .updateSource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeProfileTaggedValueClassInverse() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class {testProfile.t1='bb'} A{}")
                        .createInMemorySource("userId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "Class {testProfile.t1='bb'} A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeProfileTaggedValueEnumeration() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .createInMemorySource("userId.pure", "Enum {testProfile.t1='bb'} A{ VAL1 }")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 19)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeProfileTaggedValueEnum() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .createInMemorySource("userId.pure", "Enum A{ {testProfile.t1='bb'} VAL1 }")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 22)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{tags:[t1,t2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
