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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestResolveClassTypeParameter extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testResolveClassTypeParameter()
    {
        compileTestSource("fromString.pure", "Class A<T>{}" +
                                                   "Class D<T> extends A<T>{}" +
                                                   "Class B extends D<C>{}" +
                                                   "Class C{}");
        CoreInstance genericType = GenericType.resolveClassTypeParameterUsingInheritance(Type.wrapGenericType(this.runtime.getCoreInstance("B"), this.processorSupport), Type.wrapGenericType(this.runtime.getCoreInstance("A"), this.processorSupport), this.processorSupport).getArgumentsByParameterName().get("T");
        Assert.assertEquals("Anonymous_StripedId instance GenericType\n" +
                            "    rawType(Property):\n" +
                            "        Anonymous_StripedId instance ImportStub\n" +
                            "            idOrPath(Property):\n" +
                            "                C instance String\n" +
                            "            importGroup(Property):\n" +
                            "                import_fromString_pure_1 instance ImportGroup\n" +
                            "            resolvedNode(Property):\n" +
                            "                C instance Class", genericType.printWithoutDebug(""));
    }

    @Test
    public void testResolveClassStartingFromANonResolvedGenericType()
    {
        try
        {
            compileTestSource("fromString.pure", "Class A<T>{}" +
                    "Class D<T> extends A<T>{}" +
                    "Class B<Z> extends D<C>{}" +
                    "Class C{}");
            GenericType.resolveClassTypeParameterUsingInheritance(Type.wrapGenericType(this.runtime.getCoreInstance("B"), this.processorSupport), Type.wrapGenericType(this.runtime.getCoreInstance("A"), this.processorSupport), this.processorSupport).getArgumentsByParameterName().get("T");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Type argument mismatch for B<Z>; got: B", e.getMessage());
        }
    }

    @Test
    public void testResolveClassTypeWithFunctionTypes()
    {
        compileTestSource("fromString.pure", "Class A<T>{}" +
                "Class D<T,X> extends A<{T[1]->{X[1]->C[1]}[1]}>{}" +
                "Class B<X> extends D<X,C>{}" +
                "Class C{}" +
                "Class E{}" +
                "^B<E> k()");
        CoreInstance genericType = GenericType.resolveClassTypeParameterUsingInheritance(Instance.getValueForMetaPropertyToOneResolved(this.runtime.getCoreInstance("k"), M3Properties.classifierGenericType, this.processorSupport), Type.wrapGenericType(this.runtime.getCoreInstance("A"), this.processorSupport), this.processorSupport).getArgumentsByParameterName().get("T");
        Assert.assertEquals("Anonymous_StripedId instance GenericType\n" +
                            "    rawType(Property):\n" +
                            "        Anonymous_StripedId instance FunctionType\n" +
                            "            parameters(Property):\n" +
                            "                Anonymous_StripedId instance VariableExpression\n" +
                            "                    genericType(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            rawType(Property):\n" +
                            "                                Anonymous_StripedId instance ImportStub\n" +
                            "                                    idOrPath(Property):\n" +
                            "                                        E instance String\n" +
                            "                                    importGroup(Property):\n" +
                            "                                        import_fromString_pure_1 instance ImportGroup\n" +
                            "                                    resolvedNode(Property):\n" +
                            "                                        E instance Class\n" +
                            "                    multiplicity(Property):\n" +
                            "                        PureOne instance PackageableMultiplicity\n" +
                            "                    name(Property):\n" +
                            "                         instance String\n" +
                            "            returnMultiplicity(Property):\n" +
                            "                PureOne instance PackageableMultiplicity\n" +
                            "            returnType(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Anonymous_StripedId instance FunctionType\n" +
                            "                            parameters(Property):\n" +
                            "                                Anonymous_StripedId instance VariableExpression\n" +
                            "                                    genericType(Property):\n" +
                            "                                        Anonymous_StripedId instance GenericType\n" +
                            "                                            rawType(Property):\n" +
                            "                                                Anonymous_StripedId instance ImportStub\n" +
                            "                                                    idOrPath(Property):\n" +
                            "                                                        C instance String\n" +
                            "                                                    importGroup(Property):\n" +
                            "                                                        import_fromString_pure_1 instance ImportGroup\n" +
                            "                                                    resolvedNode(Property):\n" +
                            "                                                        C instance Class\n" +
                            "                                    multiplicity(Property):\n" +
                            "                                        PureOne instance PackageableMultiplicity\n" +
                            "                                    name(Property):\n" +
                            "                                         instance String\n" +
                            "                            returnMultiplicity(Property):\n" +
                            "                                PureOne instance PackageableMultiplicity\n" +
                            "                            returnType(Property):\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                                    rawType(Property):\n" +
                            "                                        Anonymous_StripedId instance ImportStub\n" +
                            "                                            idOrPath(Property):\n" +
                            "                                                C instance String\n" +
                            "                                            importGroup(Property):\n" +
                            "                                                import_fromString_pure_1 instance ImportGroup\n" +
                            "                                            resolvedNode(Property):\n" +
                            "                                                C instance Class", genericType.printWithoutDebug("", 10));
    }
}
