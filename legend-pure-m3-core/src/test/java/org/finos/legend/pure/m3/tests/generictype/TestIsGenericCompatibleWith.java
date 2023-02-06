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

package org.finos.legend.pure.m3.tests.generictype;

import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

// WRITE IN MODELING A TEST WITH AN INSTANCE USING A PROPERTY NOT DEFINED IN THE CLASS
// "function func(emp:Employee[1]):HomeAddress[1]\n" + was compiling where it should be "function func(emp:Employee<Address>[1]):HomeAddress[1]\n"


public class TestIsGenericCompatibleWith extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
        try{
            runtime.compile();
        } catch (PureCompilationException e) {
            setUp();
        }
    }

    @Test
    public void testIsGenericCompatibleWith_TypeParamInClass()
    {
        compileTestSource("fromString.pure","Class Employee<T>\n" +
                            "{\n" +
                            "   address : T[1];\n" +
                            "}\n" +
                            "Class Address\n" +
                            "{\n" +
                            "}\n" +
                            "Class HomeAddress extends Address\n" +
                            "{\n" +
                            "}\n" +
                            "Class VacationAddress extends Address\n" +
                            "{\n" +
                            "}\n" +
                            "^Employee<Address> i1 (address = ^VacationAddress())\n" +
                            "^Employee<HomeAddress> i2 (address = ^HomeAddress())\n");
        CoreInstance i1 = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i1"), this.processorSupport);
        CoreInstance i2 = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i2"), this.processorSupport);
        Assert.assertFalse(GenericType.isGenericCompatibleWith(i1, i2, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(i2, i1, this.processorSupport));
    }

    @Test
    public void testIsGenericCompatibleWith_TypeParamInType()
    {
        compileTestSource("fromString.pure","Class Person" +
                        "{" +
                        "}" +
                        "Class Employee<Z> extends Person\n" +
                        "{\n" +
                        "   address : Z[1];\n" +
                        "}\n" +
                        "Class Address\n" +
                        "{\n" +
                        "}\n" +
                        "Class HomeAddress extends Address\n" +
                        "{\n" +
                        "}\n" +
                        "Class VacationAddress extends Address\n" +
                        "{\n" +
                        "}\n" +
                        "" +
                        "^Employee<Address> i3 (address = ^VacationAddress())\n" +
                        "function func(emp:Employee<Address>[1]):HomeAddress[1]\n" +
                        "{\n" +
                        "   ^HomeAddress();\n" +
                        "}\n" +
                        "function func2(emp:Person[1]):HomeAddress[1]\n" +
                        "{\n" +
                        "   ^HomeAddress();\n" +
                        "}\n");

        CoreInstance unresolvedPropertyGenericType = Instance.extractGenericTypeFromInstance(Instance.getValueForMetaPropertyToManyResolved(processorSupport.package_getByUserPath("Employee"), M3Properties.properties, this.processorSupport).getFirst(), this.processorSupport);
        CoreInstance resolvedPropertyGenericType = GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i1"), this.processorSupport), unresolvedPropertyGenericType, this.processorSupport);

        CoreInstance functionGenericType = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("func_Employee_1__HomeAddress_1_"), this.processorSupport);

        CoreInstance function2GenericType = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("func2_Person_1__HomeAddress_1_"), this.processorSupport);

        Assert.assertFalse(GenericType.isGenericCompatibleWith(resolvedPropertyGenericType, functionGenericType, this.processorSupport));
        // The following one is false because FunctionDefinition is not a subtype of a Property
        Assert.assertFalse(GenericType.isGenericCompatibleWith(functionGenericType, resolvedPropertyGenericType, this.processorSupport));
        Assert.assertFalse(GenericType.genericTypesEqual(functionGenericType, function2GenericType, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(function2GenericType, functionGenericType, this.processorSupport));
        Assert.assertFalse(GenericType.isGenericCompatibleWith(functionGenericType, function2GenericType, this.processorSupport));
    }

    @Test
    public void testGenericFunctionTypeCompatibleWithItself()
    {
        compileTestSource("fromString.pure","Class TestClass { fn : Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>[1]; }");
        CoreInstance prop = this.processorSupport.class_findPropertyUsingGeneralization(this.runtime.getCoreInstance("TestClass"), "fn");
        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(prop), M3Properties.returnType, this.processorSupport);

        // Test that we got the right generic type
        Assert.assertTrue(Instance.instanceOf(genericType, M3Paths.GenericType, this.processorSupport));
        Assert.assertEquals("Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(genericType, this.processorSupport));

        // Test that it is compatible with itself
        Assert.assertTrue(GenericType.genericTypesEqual(genericType, genericType, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(genericType, genericType, this.processorSupport));

        // Test that it is compatible with a copy of itself
        CoreInstance copy = GenericType.copyGenericType(genericType, this.processorSupport);
        Assert.assertTrue(GenericType.genericTypesEqual(genericType, copy, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(genericType, copy, this.processorSupport));
    }

    @Test
    public void testGenericLambdaFunctionTypeCompatibleWithGenericFunctionType()
    {
        compileTestSource("fromString.pure","Class TestClass\n" +
                "{\n" +
                "  fn : Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>[1];\n" +
                "  lfn : LambdaFunction<{Integer[1]->LambdaFunction<{Float[1]->Boolean[1]}>[1]}>[1];\n" +
                "}\n");
        MapIterable<String, CoreInstance> props = this.processorSupport.class_getSimplePropertiesByName(this.runtime.getCoreInstance("TestClass"));
        CoreInstance fnProp = props.get("fn");
        CoreInstance lfnProp = props.get("lfn");

        CoreInstance fnGenericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(fnProp), M3Properties.returnType, this.processorSupport);
        Assert.assertEquals("Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(fnGenericType, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(fnGenericType, fnGenericType, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(fnGenericType, GenericType.copyGenericType(fnGenericType, this.processorSupport), this.processorSupport));

        CoreInstance lfnGenericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(lfnProp), M3Properties.returnType, this.processorSupport);
        Assert.assertEquals("LambdaFunction<{Integer[1]->LambdaFunction<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(lfnGenericType, this.processorSupport));
        Assert.assertTrue(GenericType.isGenericCompatibleWith(lfnGenericType, GenericType.copyGenericType(lfnGenericType, this.processorSupport), this.processorSupport));

        Assert.assertTrue(GenericType.isGenericCompatibleWith(lfnGenericType, fnGenericType, this.processorSupport));
    }

    // Make sure we can model a Set of Function having any time of parameters (the type check should work)

}
