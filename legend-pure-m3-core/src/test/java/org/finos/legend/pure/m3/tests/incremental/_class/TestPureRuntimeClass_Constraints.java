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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeClass_Constraints extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("/test/testModel.pure");
    }

    @Test
    public void testPureRuntimeClassConstraintError() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "Class test::A[$this.nam == 'ee']{name:String[1];}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:sourceId.pure line:1 column:21), \"Can't find the property 'nam' in the class test::A\"", e.getMessage());
        }
    }

    @Test
    public void testPureRuntimeClassConstraintOne() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A[$this.b.name == 'ee']{b:B[1];name:String[1];}")
                        .createInMemorySource("userId.pure", "Class B{name:String[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "sourceId.pure", 1, 33)
                        .createInMemorySource("userId.pure", "Class B{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintErrorTwo() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "Class test::A[$this.name == a()]{name:String[1];}");
            this.runtime.createInMemorySource("userId.pure", "function a():String[1]{'ee'}");
            this.runtime.compile();
            this.runtime.delete("userId.pure");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:sourceId.pure line:1 column:29), \"The system can't find a match for the function: a()\"", e.getMessage());
        }
    }


    @Test
    public void testPureRuntimeClassConstraintTwo() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A[$this.name == a()]{name:String[1];}")
                        .createInMemorySource("userId.pure", "function a():String[1]{'ee';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: a()", "sourceId.pure", 1, 23)
                        .createInMemorySource("userId.pure", "function a():String[1]{'ee';}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassConstraintTypeErrorType() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("/test/testModel.pure",
                    "Class A\n" +
                            "[22]\n" +
                            "{\n" +
                            "  name:String[1];\n" +
                            "}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", "/test/testModel.pure", 2, 2, 2, 2, 2, 3, e);
        }
    }

    @Test
    public void testPureRuntimeClassConstraintTypeErrorMultiplicity() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("/test/testModel.pure",
                    "Class A\n" +
                            "[ [true,true] ]\n" +
                            "{\n" +
                            "   name:String[1];\n" +
                            "}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", "/test/testModel.pure", 2, 3, 2, 3, 2, 13, e);
        }
    }


    @Test
    public void testPureRuntimeClassConstraintFunction() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("/test/testModel.pure",
                    "Class A\n" +
                            "[ $this.name ==  t() ]\n" +
                            "{\n" +
                            "   name:String[1];\n" +
                            "}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "The system can't find a match for the function: t()", "/test/testModel.pure", 2, 18, 2, 18, 2, 18, e);
        }
    }

    @Test
    public void testPureRuntimeClassConstraintFunctionUnbind() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure",
                    "Class A\n" +
                            "[ $this.name ==  t() ]\n" +
                            "{\n" +
                            "   name:String[1];\n" +
                            "}");
            this.runtime.createInMemorySource("userId.pure", "function t():String[1]{'test'}");
            this.runtime.compile();
            this.runtime.delete("userId.pure");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "The system can't find a match for the function: t()", "sourceId.pure", 2, 18, 2, 18, 2, 18, e);
        }
    }



    @Test
    public void testPureRuntimeClassConstraintUnbind() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [$this.name ==  t()] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: t()", "sourceId.pure", 1, 25)
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintUsageContext1() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [ c1(~function : $this.name ==  t())] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: t()", "sourceId.pure", 1, 41)
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintUsageContext2() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty() ~message : $this.name + t())] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: t()", "sourceId.pure", 1, 73)
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintAddAndRemoveMessage() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty() ~message : $this.name + t())] {name:String[1];}")
                        .compile()
                        .updateSource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintAddAndRemoveConstraint() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty() ~message : $this.name + t()), c2(~function : !!$this.name->isEmpty())] {name:String[1];}")
                        .compile()
                        .updateSource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassConstraintAddAndRemoveOthers() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "Class A [ c1( ~owner: Finance ~externalId: 'My_ID@1' ~function : !$this.name->isEmpty() ~enforcementLevel: Warn ~message : $this.name + t()), c2(~function : !!$this.name->isEmpty())] {name:String[1];}")
                        .compile()
                        .updateSource("sourceId.pure", "Class A [ c1(~function : !$this.name->isEmpty())] {name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
