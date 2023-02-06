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

public class TestPureRuntimeClass_InNew extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeClassUsedInNew() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[1]{^a::b::c::A(name='OMG!');'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("a::b::c::A has not been defined!", "userId.pure", 1, 37)
                        .createInMemorySource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassUsedInNewWithWrongProperty() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[1]{^a::b::c::A(name='OMG!');'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("a::b::c::A has not been defined!", "userId.pure", 1, 37)
                        .createInMemorySource("sourceId.pure", "Class a::b::c::A{}")
                        .compileWithExpectedCompileFailure("The property 'name' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 39)
                        .updateSource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassUsedInNewInLambda() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[1]{if(true,|^a::b::c::A(name='OMG!'),|^a::b::c::A(name='!GMO'));'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("a::b::c::A has not been defined!", "userId.pure", 1, 46)
                        .createInMemorySource("sourceId.pure", "Class a::b::c::A{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassUsedInNewInLambdaNestedWithVariable() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class B{c:B[0..1];}")
                        .createInMemorySource("userId.pure", "Class A{name:String[1];b:B[1];} function test():String[1]{let b = ^B();if(true,|^A(name='OMG!',b=^B(c=$b));,|[]);'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 26)
                        .createInMemorySource("sourceId.pure", "Class B{c:B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassUsedInNewWithGeneric() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class a::b::c::Cont<T>{c:T[1];}")
                        .createInMemorySource("userId.pure", "function test():String[1]{^a::b::c::Cont<String>(c='ok').c}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("a::b::c::Cont has not been defined!", "userId.pure", 1, 37)
                        .createInMemorySource("sourceId.pure", "Class a::b::c::Cont<T>{c:T[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

}
