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
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestPureRuntimeClass_InGeneralization extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testPureRuntimeClassGeneralization() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class B{}")
                        .createInMemorySource("userId.pure", "Class A extends B{}" +
                                "function test():Boolean[1]{assert('A' == A->id(), |'');}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    @ToFix
    @Ignore
    public void testPureRuntimeClassRemoveSpecialization()
    {
        String generalSourceId = "/test/generalModel.pure";
        String generalSource = "Class test::MoreGeneral {}";
        String specificSourceId = "/test/specificModel.pure";
        String specificSource = "Class test::MoreSpecific extends test::MoreGeneral {}";


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(generalSourceId, generalSource)
                        .createInMemorySource(specificSourceId, specificSource)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(specificSourceId)
                        .compile()
                        .createInMemorySource(specificSourceId, specificSource)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGeneralizationError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class B{}")
                        .createInMemorySource("userId.pure", "Class A extends B{}" +
                                "function test():Boolean[1]{assert('A' == A->id(), |'');}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 17)
                        .createInMemorySource("sourceId.pure", "Class C{}")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 17)
                        .updateSource("sourceId.pure", "Class B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGeneralizationPropertyError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class test::B{property:String[1];}")
                        .createInMemorySource("userId.pure", "import test::*;\n" +
                                "Class test::A extends B{}\n" +
                                "function test():Boolean[1]{assert('test' == ^A(property='test').property, |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 2, 23)
                        .createInMemorySource("sourceId.pure", "Class test::B{}")
                        .compileWithExpectedCompileFailure("Can't find the property 'property' in the class test::A", "userId.pure", 3, 65)
                        .updateSource("sourceId.pure", "Class test::B{property:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassGeneralizationFunctionMatchingParameter() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A extends B{}")
                        .createInMemorySource("userId.pure", "Class B{}" +
                                "function myFunc(b:B[1]):String[1]{'ok'}" +
                                "function test():Boolean[1]{assert('ok' == myFunc(^A()), |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 99)
                        .createInMemorySource("sourceId.pure", "Class A extends B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGeneralizationFunctionMatchingParameterError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A extends B{}")
                        .createInMemorySource("userId.pure", "Class B{}" +
                                "function myFunc(b:B[1]):String[1]{'ok'}" +
                                "function test():Boolean[1]{assert('ok' == myFunc(^A()), |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 99)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compileWithExpectedCompileFailure(PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "myFunc(_:A[1])\n" +
                                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                                "\tmyFunc(B[1]):String[1]\n" +
                                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 1, 91)
                        .updateSource("sourceId.pure", "Class A extends B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassGeneralizationFunctionMatchingReturn() throws Exception
    {

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class B extends C{}")
                        .createInMemorySource("userId.pure", "Class D{}" +
                                "Class C extends D{}" +
                                "Class A extends B{}" +
                                "function myFunc():D[1]{^A()}" +
                                "function test():Boolean[1]{assert(A == myFunc()->genericType().rawType, |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 45)
                        .createInMemorySource("sourceId.pure", "Class B extends C{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeClassGeneralizationFunctionMatchingOtherReturnError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class B extends C{}")
                        .createInMemorySource("userId.pure", "Class D{}" +
                                "Class C extends D{}" +
                                "Class A extends B{}" +
                                "function myFunc():D[1]{^A()}" +
                                "function test():Boolean[1]{assert(A == myFunc()->genericType().rawType, |'')}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 45)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("Return type error in function 'myFunc'; found: A; expected: D", "userId.pure", 1, 71)
                        .updateSource("sourceId.pure", "Class B extends C{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
