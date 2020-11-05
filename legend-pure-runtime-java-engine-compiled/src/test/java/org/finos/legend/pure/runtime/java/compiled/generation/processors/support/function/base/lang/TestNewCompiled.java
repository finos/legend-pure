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

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestNewAtRuntime;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestNewCompiled extends AbstractTestNewAtRuntime
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }
    @After
    public void cleanRuntime() {
        runtime.delete("fromString.pure");
        runtime.delete("/test/testModel.pure");
        try{
            runtime.compile();
        } catch (PureCompilationException e) {
            setUp();
        }
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected void assertNewNilException(Exception e)
    {
        assertOriginatingPureException(PureExecutionException.class, "Cannot instantiate meta::pure::metamodel::type::Nil", e);
    }

    @Override
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.car->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::FastCar extends test::Car\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "}" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   car  : test::Car[1];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            assertEquals("'0'", result);
        }
        catch (Exception e)
        {
            fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Override
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.cars->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::FastCar extends test::Car\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "}" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   cars  : test::Car[1..*];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            assertEquals("'0'", result);
        }
        catch (Exception e)
        {
            fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }
}
