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

package org.finos.legend.pure.m3.tests.incremental.function;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestPureRuntimeFunction_Constraint extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testPureRuntimeFunctionConstraint() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function f(p:String[1], z:String[2]):String[1] [$p == $z->joinStrings(',') + 'kk', $return == t()] {'l'}")
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("userId.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: t()", "sourceId.pure", 1, 95)
                        .createInMemorySource("userId.pure", "function t():String[1]{'test'}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeFunctionConstraintCompilationErrorDeleteDependencies() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "function f(p:String[1], z:String[2]):String[1] [$p == t(), $return ==  $z->at(0) + 'kk'] {'l'}");
            this.runtime.createInMemorySource("userId.pure", "function t():String[1]{'test'}");
            this.runtime.compile();
            this.runtime.delete("userId.pure");
            this.runtime.compile();
            Assert.fail("This code should not compile");
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "The system can't find a match for the function: t()", "sourceId.pure", 1, 55, 1, 55, 1, 55, e);
        }
    }

    @Test
    public void testPureRuntimeFunctionConstraintCompilationErrorDeleteDependenciesTwo() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "function f(p:String[1], z:String[1]):String[1] [$p == ($z + 'kk') , $return == t() ] {'l'}");
            this.runtime.createInMemorySource("userId.pure", "function t():String[1]{'test'}");
            this.runtime.compile();
            this.runtime.delete("userId.pure");
            this.runtime.compile();
            Assert.fail("This code should not compile");
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "The system can't find a match for the function: t()", "sourceId.pure", 1, 80, 1, 80, 1, 80, e);
        }
    }


    @Test
    public void testPureRuntimeFunctionConstraintCompilationError() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "function f(p:String[1], z:String[2]):String[1] [$p == $z + 'kk', $return == t()] {'l'}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "The system can't find a match for the function: t()", "sourceId.pure", 1, 77, 1, 77, 1, 77, e);
        }
    }

    @Test
    public void testPureRuntimeFunctionConstraintError() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("sourceId.pure", "function f(p:String[1], z:String[2]):String[1] [$p == $zw + 'kk', $return == t()] {'l'}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:sourceId.pure line:1 column:56), \"The variable 'zw' is unknown!\"", e.getMessage());
        }
    }

    @Test
    public void testPureRuntimeFunctionConstraintErrorType() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("/test/testSource.pure",
                    "function f(p:String[1], z:String[2]):String[1]\n" +
                            "[$return == true, 1]\n" +
                            "{\n" +
                            "   'l'\n" +
                            "}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", "/test/testSource.pure", 2, 19, 2, 19, 2, 19, e);
        }
    }

    @Test
    public void testPureRuntimeFunctionConstraintErrorMul() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("/test/testSource.pure",
                    "function f(p:String[1], z:String[2]):String[1]\n" +
                            "[$return == true, [true,true]]\n" +
                            "{\n" +
                            "   'l'\n" +
                            "}");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", "/test/testSource.pure", 2, 19, 2, 19, 2, 29, e);
        }
    }
}
