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

package org.finos.legend.pure.m3.tests.incremental.association;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeAssociation_UseProperty extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testPureRuntimeAssociation_UseProperty() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Association a {a:test::A[0..1];b:test::B[0..1];}")
                        .createInMemorySource("userId.pure", "import test::*;\n" +
                                "Class test::A{}\n" +
                                "Class test::B{}\n" +
                                "function test():Any[*]{ let k = ^A(b=^B()); $k.b;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("Can't find the property 'b' in the class test::A", "userId.pure", 4, 48)
                        .createInMemorySource("sourceId.pure", "Association a {a:test::A[0..1];b:test::B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeAssociation_UsePropertyError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Association a {a:test::A[0..1];b:test::B[0..1];}")
                        .createInMemorySource("userId.pure", "import test::*;\n" +
                                "Class test::A{}\n" +
                                "Class test::B{}\n" +
                                "function test():Any[*]{ let k = ^A(b=^B()); $k.b;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("Can't find the property 'b' in the class test::A", "userId.pure", 4, 48)
                        .createInMemorySource("sourceId.pure", "Association a {xx:test::A[0..1];yy:test::B[0..1];}")
                        .compileWithExpectedCompileFailure("Can't find the property 'b' in the class test::A", "userId.pure", 4, 48)
                        .updateSource("sourceId.pure", "Association a {a:test::A[0..1];b:test::B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }
}
