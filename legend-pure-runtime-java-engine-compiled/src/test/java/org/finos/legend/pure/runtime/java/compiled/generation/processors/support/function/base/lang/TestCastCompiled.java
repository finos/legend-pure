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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestCast;
import org.finos.legend.pure.m3.tools.ThrowableTools;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.junit.Assert;

public class TestCastCompiled extends AbstractTestCast
{
    @Override
    protected FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    public void checkInvalidCastWithTypeParametersErrorMessage(Exception e)
    {
        Assert.assertEquals("Error executing test():Any[*]. Unexpected error executing function", e.getMessage());
        Throwable cause = ThrowableTools.findRootThrowable(e);
        Assert.assertNotSame(e, cause);
        String java7runtime = JavaPackageAndImportBuilder.rootPackage() + ".Root_X_Impl cannot be cast to " + JavaPackageAndImportBuilder.rootPackage() + ".Root_Y";
        String java11runtime = "class " + JavaPackageAndImportBuilder.rootPackage() + ".Root_X_Impl cannot be cast to class " + JavaPackageAndImportBuilder.rootPackage() + ".Root_Y";
        Assert.assertTrue(java7runtime.equals(cause.getMessage()) || cause.getMessage().startsWith(java11runtime));
    }

    @Override
    public void testPrimitiveConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Integer cannot be cast to String", 31, 10);
    }

    @Override
    public void testPrimitiveConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Number", 36, 13);
    }

    @Override
    public void testNonPrimitiveConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 41, 12);
    }

    @Override
    public void testNonPrimitiveConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 41, 12);
    }

    @Override
    public void testEnumToStringCastErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Month cannot be cast to String", 31, 10);
    }

    @Override
    public void testPrimitiveNonConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Integer cannot be cast to String", 51, 10);
    }

    @Override
    public void testNonPrimitiveNonConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 61, 12);
    }

    @Override
    public void testPrimitiveNonConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Number", 56, 13);
    }

    @Override
    public void testNonPrimitiveNonConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 61, 12);
    }

    @Override
    public void testStringToEnumCastErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Enum", 3, 17);
    }

}
