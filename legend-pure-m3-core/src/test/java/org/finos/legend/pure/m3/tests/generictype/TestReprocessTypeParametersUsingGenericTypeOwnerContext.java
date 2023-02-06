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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReprocessTypeParametersUsingGenericTypeOwnerContext extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testReprocessTypeParametersUsingGenericTypeOwnerContext()
    {
        CoreInstance property = Instance.getValueForMetaPropertyToManyResolved(this.runtime.getCoreInstance(M3Paths.Class), M3Properties.properties, this.processorSupport).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                return M3Properties.properties.equals(instance.getName());
            }
        });
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        CoreInstance genericType = Instance.extractGenericTypeFromInstance(this.runtime.getCoreInstance(M3Paths.Multiplicity), this.processorSupport);
        Assert.assertEquals("Anonymous_StripedId instance GenericType\n" +
                            "    rawType(Property):\n" +
                            "        Anonymous_StripedId instance FunctionType\n" +
                            "            parameters(Property):\n" +
                            "                Anonymous_StripedId instance VariableExpression\n" +
                            "                    genericType(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            rawType(Property):\n" +
                            "                                Class instance Class\n" +
                            "                            typeArguments(Property):\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                                    [... >3]\n" +
                            "                    multiplicity(Property):\n" +
                            "                        PureOne instance PackageableMultiplicity\n" +
                            "                    name(Property):\n" +
                            "                        object instance String\n" +
                            "            returnMultiplicity(Property):\n" +
                            "                ZeroMany instance PackageableMultiplicity\n" +
                            "            returnType(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    multiplicityArguments(Property):\n" +
                            "                        ZeroMany instance PackageableMultiplicity\n" +
                            "                    rawType(Property):\n" +
                            "                        Property instance Class\n" +
                            "                    typeArguments(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            rawType(Property):\n" +
                            "                                Multiplicity instance Class\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            rawType(Property):\n" +
                            "                                Any instance Class",
                GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(genericType, Type.wrapGenericType(functionType, this.processorSupport), this.processorSupport).printWithoutDebug("",3));
    }
}
