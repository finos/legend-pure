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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeAssociation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");

        try
        {
            runtime.compile();
        } catch (PureCompilationException e) {
            setUp();
        }
    }

    @Test
    public void testPureRuntimeAssociation() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}")
                        .createInMemorySource("userId.pure", "Class A{}" +
                                "Class B{}" +
                                "function test():Nil[0]{ let k = ^A(b=^B()); let j = ^B(a=^A()); [];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("The property 'b' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 54)
                        .createInMemorySource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeAssociationError() throws Exception
    {
/*
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}")
                        .createInMemorySource("userId.pure", "Class A{}" +
                                "Class B{}" +
                                "function test():Nil[0]{ let k = ^A(b=^B()); let j = ^B(a=^A()); [];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("The property 'b' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 54)
                        .createInMemorySource("sourceId.pure", "Association a {xx:A[0..1];yy:B[0..1];}")
                        .compileWithExpectedCompileFailure("The property 'b' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 54)
                        .compileWithExpectedCompileFailure("The property 'a' can't be found in the type 'B' or in its hierarchy.", "userId.pure", 1, 74)
                        .updateSource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
*/

        this.runtime.createInMemorySource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}");
        this.runtime.createInMemorySource("userId.pure", "Class A{}" +
                                               "Class B{}" +
                                               "function test():Nil[0]{ let k = ^A(b=^B()); let j = ^B(a=^A()); [];}");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The property 'b' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 54, e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "Association a {xx:A[0..1];yy:B[0..1];}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertTrue("Compilation error at (resource:userId.pure line:1 column:54), \"The property 'b' can't be found in the type 'A' or in its hierarchy.\"".equals(e.getMessage()) ||
                                  "Compilation error at (resource:userId.pure line:1 column:74), \"The property 'a' can't be found in the type 'B' or in its hierarchy.\"".equals(e.getMessage()));
            }
        }

        this.runtime.modify("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }


    @Test
    public void testPureRuntimeAssociationAggregation() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Class A{} Class B{} Association a {(composite) a:A[0..1];b:B[0..1];}");
        this.runtime.compile();
    }

    @Test
    public void testPureRuntimeAssociationWithQualifiedPropertyAssociationRebuild() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Association a {a:A[0..1]; b:B[*]; bSubset(){$this.b->filter(b|$b.name=='')->first()}:B[0..1];}")
                        .createInMemorySource("userId.pure", "Class A{}" +
                                "Class B{name : String[0..1];}" +
                                "function test():Nil[0]{ let a = ^A(); let b = $a.bSubset(); [];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: bSubset(_:A[1])", "userId.pure", 1, 88)
                        .createInMemorySource("sourceId.pure", "Association a {a:A[0..1]; b:B[*]; bSubset(){$this.b->filter(b|$b.name=='')->first()}:B[0..1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeAssociationWithQualifiedPropertyAssociationForceProcessUnbindCycle() throws Exception
    {
        String sourceId = "Association a {a:A[0..1]; b:B[*]; bSubset(){$this.b->filter(b|$b.name=='')->first()}:B[0..1];}";
        String userId = "Class B{name : String[0..1];} Class A{}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", sourceId)
                        .createInMemorySource("userId.pure", userId)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "sourceId.pure", 1, 18)
                        .createInMemorySource("userId.pure", userId)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeAssociationUnload()
    {
        String classSourceId = "/test/myClass.pure";
        String classSource = "Class test::Class1 {}\n" +
                "Class test::Class2 {}\n";
        String assocSourceId = "/test/myAssociation.pure";
        String assocSource = "Association test::Assoc\n" +
                "{\n" +
                "    p1 : test::Class1[*];\n" +
                "    p2 : test::Class2[*];\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(classSourceId, classSource)
                        .createInMemorySource(assocSourceId, assocSource)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(classSourceId)
                        .deleteSource(assocSourceId)
                        .compile()
                        .createInMemorySource(classSourceId, classSource)
                        .createInMemorySource(assocSourceId, assocSource)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
