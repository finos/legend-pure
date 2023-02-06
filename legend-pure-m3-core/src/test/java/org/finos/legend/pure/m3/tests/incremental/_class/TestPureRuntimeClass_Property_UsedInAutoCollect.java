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

public class TestPureRuntimeClass_Property_UsedInAutoCollect extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeClass_Property() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[*]{A.all().name}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 27)
                        .createInMemorySource("sourceId.pure", "Class A{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClass_QualifiedProperty() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class my::A{name(prefix:String[1]){ $prefix + ' Bob';}:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[*]{my::A.all().name('Mr')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .createInMemorySource("userId.pure", "function test():String[*]{my::A.all().name('Mr')}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClass_QualifiedPropertyOpenVariable() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class my::A{name(prefix:String[1]){ $prefix + ' Bob';}:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[*]{ let b = 'Mr'; my::A.all().name($b);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .createInMemorySource("userId.pure", "function test():String[*]{ let b = 'Mr'; my::A.all().name($b);}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClass_QualifiedPropertyOpenVariableForceUnbindAndReprocess() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class my::A{name(prefix:String[1]){ $prefix + ' Bob';}:String[1];}")
                        .createInMemorySource("userId.pure", "function test():String[*]{ let b = 'Mr'; my::A.all().name($b);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "Class my::A{name(prefix:String[1]){ $prefix + ' Bob';}:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

}
