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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeClass_InCast extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
    }

    @Test
    public void testPureRuntimeClassUsedInCast() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];} Class B extends A{}")
                        .createInMemorySource("userId.pure", "function test():String[1]{let b = ^B(name='OMG!')->cast(@A);'ok';}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 36)
                        .createInMemorySource("sourceId.pure", "Class A{name:String[1];} Class B extends A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassUsedInCastError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];} Class B extends A{}")
                        .createInMemorySource("userId.pure", "function test():String[1]{let b = ^B(name='OMG!')->cast(@A);'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 36)
                        .createInMemorySource("sourceId.pure", "Class B{name:String[1];}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 58)
                        .updateSource("sourceId.pure", "Class A{name:String[1];} Class B extends A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }
}
