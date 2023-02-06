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

package org.finos.legend.pure.m3.tests.function;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGetFunctionType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testGetFunctionType()
    {
        CoreInstance property = Instance.getValueForMetaPropertyToManyResolved(this.runtime.getCoreInstance(M3Paths.Class), M3Properties.properties, this.processorSupport).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                return M3Properties.properties.equals(instance.getName());
            }
        });
        CoreInstance functionType = this.processorSupport.function_getFunctionType(property);
        Assert.assertEquals("Anonymous_StripedId instance FunctionType\n" +
                            "    parameters(Property):\n" +
                            "        Anonymous_StripedId instance VariableExpression\n" +
                            "            genericType(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Class instance Class\n" +
                            "                    typeArguments(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            referenceUsages(Property):\n" +
                            "                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                    [... >3]\n" +
                            "                            typeParameter(Property):\n" +
                            "                                Anonymous_StripedId instance TypeParameter\n" +
                            "                                    [... >3]\n" +
                            "            multiplicity(Property):\n" +
                            "                PureOne instance PackageableMultiplicity\n" +
                            "            name(Property):\n" +
                            "                object instance String\n" +
                            "    returnMultiplicity(Property):\n" +
                            "        ZeroMany instance PackageableMultiplicity\n" +
                            "    returnType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            multiplicityArguments(Property):\n" +
                            "                ZeroMany instance PackageableMultiplicity\n" +
                            "            rawType(Property):\n" +
                            "                Property instance Class\n" +
                            "            typeArguments(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    referenceUsages(Property):\n" +
                            "                        Anonymous_StripedId instance ReferenceUsage\n" +
                            "                            offset(Property):\n" +
                            "                                0 instance Integer\n" +
                            "                            owner(Property):\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                                    [... >3]\n" +
                            "                            propertyName(Property):\n" +
                            "                                typeArguments instance String\n" +
                            "                    typeParameter(Property):\n" +
                            "                        Anonymous_StripedId instance TypeParameter\n" +
                            "                            name(Property):\n" +
                            "                                T instance String\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Any instance Class", functionType.printWithoutDebug("",3));
    }




    @Test
    public void testGetFunctionTypeForProperty()
    {
        this.runtime.createInMemorySource("fromString.pure", "Class Person {prop:String[1];}");
        this.runtime.compile();
        CoreInstance property = this.processorSupport.class_findPropertyUsingGeneralization(this.runtime.getCoreInstance("Person"), "prop");
        CoreInstance functionType = this.processorSupport.function_getFunctionType(property);
        Assert.assertEquals("Anonymous_StripedId instance FunctionType\n" +
                            "    parameters(Property):\n" +
                            "        Anonymous_StripedId instance VariableExpression\n" +
                            "            genericType(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Anonymous_StripedId instance ImportStub\n" +
                            "                            idOrPath(Property):\n" +
                            "                                Person instance String\n" +
                            "                            importGroup(Property):\n" +
                            "                                import_fromString_pure_1 instance ImportGroup\n" +
                            "                            resolvedNode(Property):\n" +
                            "                                Person instance Class\n" +
                            "            multiplicity(Property):\n" +
                            "                PureOne instance PackageableMultiplicity\n" +
                            "            name(Property):\n" +
                            "                object instance String\n" +
                            "    returnMultiplicity(Property):\n" +
                            "        PureOne instance PackageableMultiplicity\n" +
                            "    returnType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            rawType(Property):\n" +
                            "                String instance PrimitiveType", functionType.printWithoutDebug("", 10));
    }



    @Test
    public void testGetFunctionTypeForPropertiesUsingInheritance()
    {
        this.runtime.createInMemorySource("fromString.pure", "Class Person<T> {prop:T[*];}" +
                                                   "Class Employee extends Person<String>{}");
        this.runtime.compile();
        CoreInstance property = this.processorSupport.class_findPropertyUsingGeneralization(this.runtime.getCoreInstance("Employee"), "prop");
        CoreInstance functionType = this.processorSupport.function_getFunctionType(property);
        Assert.assertEquals("Anonymous_StripedId instance FunctionType\n" +
                            "    parameters(Property):\n" +
                            "        Anonymous_StripedId instance VariableExpression\n" +
                            "            genericType(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Anonymous_StripedId instance ImportStub\n" +
                            "                            idOrPath(Property):\n" +
                            "                                Person instance String\n" +
                            "                            importGroup(Property):\n" +
                            "                                import_fromString_pure_1 instance ImportGroup\n" +
                            "                            resolvedNode(Property):\n" +
                            "                                Person instance Class\n" +
                            "                    typeArguments(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            referenceUsages(Property):\n" +
                            "                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                    offset(Property):\n" +
                            "                                        0 instance Integer\n" +
                            "                                    owner(Property):\n" +
                            "                                        Anonymous_StripedId instance GenericType\n" +
                            "                                            rawType(Property):\n" +
                            "                                                Anonymous_StripedId instance ImportStub\n" +
                            "                                                    idOrPath(Property):\n" +
                            "                                                        Person instance String\n" +
                            "                                                    importGroup(Property):\n" +
                            "                                                        import_fromString_pure_1 instance ImportGroup\n" +
                            "                                                    resolvedNode(Property):\n" +
                            "                                                        Person instance Class\n" +
                            "                                            referenceUsages(Property):\n" +
                            "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                                    offset(Property):\n" +
                            "                                                        0 instance Integer\n" +
                            "                                                    owner(Property):\n" +
                            "                                                        Anonymous_StripedId instance GenericType\n" +
                            "                                                            multiplicityArguments(Property):\n" +
                            "                                                                ZeroMany instance PackageableMultiplicity\n" +
                            "                                                            rawType(Property):\n" +
                            "                                                                Property instance Class\n" +
                            "                                                            referenceUsages(Property):\n" +
                            "                                                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                                                    offset(Property):\n" +
                            "                                                                        0 instance Integer\n" +
                            "                                                                    owner(Property):\n" +
                            "                                                                        prop instance Property\n" +
                            "                                                                            aggregation(Property):\n" +
                            "                                                                                None instance AggregationKind\n" +
                            "                                                                                    name(Property):\n" +
                            "                                                                                        None instance String\n" +
                            "                                                                            classifierGenericType(Property):\n" +
                            "                                                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                                            genericType(Property):\n" +
                            "                                                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                                                    typeParameter(Property):\n" +
                            "                                                                                        Anonymous_StripedId instance TypeParameter\n" +
                            "                                                                                            [... >10]\n" +
                            "                                                                            multiplicity(Property):\n" +
                            "                                                                                ZeroMany instance PackageableMultiplicity\n" +
                            "                                                                            name(Property):\n" +
                            "                                                                                prop instance String\n" +
                            "                                                                            owner(Property):\n" +
                            "                                                                                Person instance Class\n" +
                            "                                                                    propertyName(Property):\n" +
                            "                                                                        classifierGenericType instance String\n" +
                            "                                                            typeArguments(Property):\n" +
                            "                                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                                    referenceUsages(Property):\n" +
                            "                                                                        Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                                                            offset(Property):\n" +
                            "                                                                                1 instance Integer\n" +
                            "                                                                            owner(Property):\n" +
                            "                                                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                                            propertyName(Property):\n" +
                            "                                                                                typeArguments instance String\n" +
                            "                                                                    typeParameter(Property):\n" +
                            "                                                                        Anonymous_StripedId instance TypeParameter\n" +
                            "                                                                            name(Property):\n" +
                            "                                                                                T instance String\n" +
                            "                                                    propertyName(Property):\n" +
                            "                                                        typeArguments instance String\n" +
                            "                                            typeArguments(Property):\n" +
                            "                                                Anonymous_StripedId instance GenericType\n" +
                            "                                    propertyName(Property):\n" +
                            "                                        typeArguments instance String\n" +
                            "                            typeParameter(Property):\n" +
                            "                                Anonymous_StripedId instance TypeParameter\n" +
                            "                                    name(Property):\n" +
                            "                                        T instance String\n" +
                            "            multiplicity(Property):\n" +
                            "                PureOne instance PackageableMultiplicity\n" +
                            "            name(Property):\n" +
                            "                object instance String\n" +
                            "    returnMultiplicity(Property):\n" +
                            "        ZeroMany instance PackageableMultiplicity\n" +
                            "    returnType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            referenceUsages(Property):\n" +
                            "                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                    offset(Property):\n" +
                            "                        1 instance Integer\n" +
                            "                    owner(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            multiplicityArguments(Property):\n" +
                            "                                ZeroMany instance PackageableMultiplicity\n" +
                            "                            rawType(Property):\n" +
                            "                                Property instance Class\n" +
                            "                            referenceUsages(Property):\n" +
                            "                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                    offset(Property):\n" +
                            "                                        0 instance Integer\n" +
                            "                                    owner(Property):\n" +
                            "                                        prop instance Property\n" +
                            "                                            aggregation(Property):\n" +
                            "                                                None instance AggregationKind\n" +
                            "                                                    name(Property):\n" +
                            "                                                        None instance String\n" +
                            "                                            classifierGenericType(Property):\n" +
                            "                                                Anonymous_StripedId instance GenericType\n" +
                            "                                            genericType(Property):\n" +
                            "                                                Anonymous_StripedId instance GenericType\n" +
                            "                                                    typeParameter(Property):\n" +
                            "                                                        Anonymous_StripedId instance TypeParameter\n" +
                            "                                                            name(Property):\n" +
                            "                                                                T instance String\n" +
                            "                                            multiplicity(Property):\n" +
                            "                                                ZeroMany instance PackageableMultiplicity\n" +
                            "                                            name(Property):\n" +
                            "                                                prop instance String\n" +
                            "                                            owner(Property):\n" +
                            "                                                Person instance Class\n" +
                            "                                    propertyName(Property):\n" +
                            "                                        classifierGenericType instance String\n" +
                            "                            typeArguments(Property):\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                                    rawType(Property):\n" +
                            "                                        Anonymous_StripedId instance ImportStub\n" +
                            "                                            idOrPath(Property):\n" +
                            "                                                Person instance String\n" +
                            "                                            importGroup(Property):\n" +
                            "                                                import_fromString_pure_1 instance ImportGroup\n" +
                            "                                            resolvedNode(Property):\n" +
                            "                                                Person instance Class\n" +
                            "                                    referenceUsages(Property):\n" +
                            "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                            offset(Property):\n" +
                            "                                                0 instance Integer\n" +
                            "                                            owner(Property):\n" +
                            "                                                Anonymous_StripedId instance GenericType\n" +
                            "                                            propertyName(Property):\n" +
                            "                                                typeArguments instance String\n" +
                            "                                    typeArguments(Property):\n" +
                            "                                        Anonymous_StripedId instance GenericType\n" +
                            "                                            referenceUsages(Property):\n" +
                            "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                                    offset(Property):\n" +
                            "                                                        0 instance Integer\n" +
                            "                                                    owner(Property):\n" +
                            "                                                        Anonymous_StripedId instance GenericType\n" +
                            "                                                    propertyName(Property):\n" +
                            "                                                        typeArguments instance String\n" +
                            "                                            typeParameter(Property):\n" +
                            "                                                Anonymous_StripedId instance TypeParameter\n" +
                            "                                                    name(Property):\n" +
                            "                                                        T instance String\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                    propertyName(Property):\n" +
                            "                        typeArguments instance String\n" +
                            "            typeParameter(Property):\n" +
                            "                Anonymous_StripedId instance TypeParameter\n" +
                            "                    name(Property):\n" +
                            "                        T instance String", functionType.printWithoutDebug("", 10));
    }
}
