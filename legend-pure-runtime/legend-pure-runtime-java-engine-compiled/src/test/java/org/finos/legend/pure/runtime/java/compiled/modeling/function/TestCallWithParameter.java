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

package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCallWithParameter extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testCallWithString()
    {
        compileTestSource("fromString.pure", "function testWithParam(val:String[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals('Hello', $val);\n" +
                "}\n");
        this.compileAndExecute("testWithParam(String[1]):Boolean[1]",
                ValueSpecificationBootstrap.wrapValueSpecification(this.processorSupport.newCoreInstance("Hello", M3Paths.String, null), true, this.processorSupport));
    }

    @Test
    public void testCallWithInteger()
    {
        compileTestSource("fromString.pure", "function testWithParam(val:Integer[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals(1, $val);\n" +
                "}\n");
        this.compileAndExecute("testWithParam(Integer[1]):Boolean[1]",
                ValueSpecificationBootstrap.wrapValueSpecification(this.processorSupport.newCoreInstance("1", M3Paths.Integer, null), true, this.processorSupport));
    }

    @Test
    public void testCallWithFloat()
    {
        compileTestSource("fromString.pure", "function testWithParam(val:Float[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals(1.23, $val);\n" +
                "}\n");
        this.compileAndExecute("testWithParam(Float[1]):Boolean[1]",
                ValueSpecificationBootstrap.wrapValueSpecification(this.processorSupport.newCoreInstance("1.23", M3Paths.Float, null), true, this.processorSupport));
    }


    @Test
    public void testCallWithBoolean()
    {
        compileTestSource("fromString.pure", "function testWithParam(val:Boolean[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals(true, $val);\n" +
                "}\n");
        this.compileAndExecute("testWithParam(Boolean[1]):Boolean[1]", ValueSpecificationBootstrap.wrapValueSpecification(this.processorSupport.newCoreInstance("true", M3Paths.Boolean, null), true, this.processorSupport));
    }


    @Test
    public void testCallWithDate()
    {
        compileTestSource("fromString.pure", "function testWithParam(val:Date[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals('2014-12-01', $val->toString());\n" +
                "}\n");
        this.compileAndExecute("testWithParam(Date[1]):Boolean[1]", ValueSpecificationBootstrap.wrapValueSpecification(this.processorSupport.newCoreInstance(DateFunctions.newPureDate(2014, 12, 1).toString(), M3Paths.StrictDate, null), true, this.processorSupport));
    }

    @Test
    public void testCallWithLatestDate()
    {
        compileTestSource("fromString.pure", "Class Order{" +
                "    p:Product[0..1];\n" +
                "    latestProduct(){\n" +
                "                     $this.p(%latest);\n" +
                "                   }:Product[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> Product{}");
    }

    @Test
    public void testCallWithEnumParameter()
    {
        compileTestSource("fromString.pure", "Enum HELLO\n" +
                "{\n" +
                "     DUDE\n" +
                "}\n" +
                "function testWithParam(val:HELLO[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals('DUDE', $val.name);\n" +
                "}\n");

        CoreInstance _enum = processorSupport.package_getByUserPath("HELLO").getValueForMetaPropertyToOne("values");
        this.compileAndExecute("testWithParam(HELLO[1]):Boolean[1]", ValueSpecificationBootstrap.wrapValueSpecification(_enum, true, this.functionExecution.getProcessorSupport()));
    }


    @Test
    public void testCallWithClassParameter()
    {
        compileTestSource("fromString.pure", "Class A\n" +
                "{\n" +
                "     name:String[1];\n" +
                "}\n" +
                "function testWithParam(val:Class<Any>[1]):Boolean[1]\n" +
                "{\n" +
                "    assertEquals('A', $val.name);\n" +
                "}\n");
        CoreInstance classA = this.functionExecution.getProcessorSupport().package_getByUserPath("A");
        this.compileAndExecute("testWithParam(Class[1]):Boolean[1]", ValueSpecificationBootstrap.wrapValueSpecification(classA, true, this.functionExecution.getProcessorSupport()));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
