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

public class TestPureRuntimeClass_InCopy extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testPureRuntimeClassUsedInCopy() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];} function getA():A[1]{^A(name='ok')}")
                        .createInMemorySource("userId.pure", "function test():String[1]{let a = getA(); ^$a(name='test'); 'tst';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: getA()", "userId.pure", 1, 35)
                        .createInMemorySource("sourceId.pure", "Class A{name:String[1];} function getA():A[1]{^A(name='ok')}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

   }


    @Test
    public void testPureRuntimeClassUsedInCopyWithWrongProperty() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];} function getA():A[1]{^A(name='ok')}")
                        .createInMemorySource("userId.pure", "function test():String[1]{let a = getA();^$a(name='test'); 'tst';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: getA()", "userId.pure", 1, 35)
                        .createInMemorySource("sourceId.pure", "Class A{nameProp:String[1];} function getA():A[1]{^A(nameProp='ok')}")
                        .compileWithExpectedCompileFailure("The property 'name' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 46)
                        .updateSource("sourceId.pure", "Class A{name:String[1];} function getA():A[1]{^A(name='ok')}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

}
