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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeClass_AsPointer extends AbstractPureTestWithCoreCompiledPlatform
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
    }

    @Test
    public void testPureRuntimeClassPointer() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f():Class<Any>[1]{A}" +
                                "function test():Boolean[1]{assert(Class == f()->genericType().rawType, |'')}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 28)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassPointerInArray() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f():Class<Any>[*]{[A,A]}" +
                                "function test():Boolean[1]{assert(Class == f()->genericType().rawType, |'')}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 29)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassPointerError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f():Class<Any>[1]{A}" +
                                "function test():Boolean[1]{assert(Class == f()->genericType().rawType, |'')}")

                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 28)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 28)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassReferenceUsageCleanUp() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function f():Class<Any>[1]{A}")
                        .createInMemorySource("userId.pure", "Class A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "function f():Class<Any>[1]{A}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

        Assert.assertEquals("A instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            [... >0]\n" +
                "    generalizations(Property):\n" +
                "        Anonymous_StripedId instance Generalization\n" +
                "            [... >0]\n" +
                "    name(Property):\n" +
                "        A instance String\n" +
                "    package(Property):\n" +
                "        Root instance Package\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            [... >0]", this.runtime.getCoreInstance("A").printWithoutDebug("", 0));
    }
}
