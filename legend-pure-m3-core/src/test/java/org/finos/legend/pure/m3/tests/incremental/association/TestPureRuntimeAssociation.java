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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeAssociation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeAssociation()
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
                runtime, functionExecution, getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeAssociationError()
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
                runtime, functionExecution, getAdditionalVerifiers());
*/

        runtime.createInMemorySource("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}");
        runtime.createInMemorySource("userId.pure", "Class A{}" +
                "Class B{}" +
                "function test():Nil[0]{ let k = ^A(b=^B()); let j = ^B(a=^A()); [];}");
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The property 'b' can't be found in the type 'A' or in its hierarchy.", "userId.pure", 1, 54, e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "Association a {xx:A[0..1];yy:B[0..1];}");
                runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertTrue("Compilation error at (resource:userId.pure line:1 column:54), \"The property 'b' can't be found in the type 'A' or in its hierarchy.\"".equals(e.getMessage()) ||
                        "Compilation error at (resource:userId.pure line:1 column:74), \"The property 'a' can't be found in the type 'B' or in its hierarchy.\"".equals(e.getMessage()));
            }
        }

        runtime.modify("sourceId.pure", "Association a {a:A[0..1];b:B[0..1];}");
        runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, repository.serialize().length);
    }


    @Test
    public void testPureRuntimeAssociationAggregation()
    {
        runtime.createInMemorySource("sourceId.pure", "Class A{} Class B{} Association a {(composite) a:A[0..1];b:B[0..1];}");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeAssociationWithQualifiedPropertyAssociationRebuild()
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
                runtime, functionExecution, getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeAssociationWithQualifiedPropertyAssociationForceProcessUnbindCycle()
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
                runtime, functionExecution, getAdditionalVerifiers());
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
                runtime, functionExecution, getAdditionalVerifiers());
    }
}
