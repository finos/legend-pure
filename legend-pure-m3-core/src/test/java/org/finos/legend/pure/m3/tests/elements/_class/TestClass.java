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

package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClass extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testClass()
    {
        compileTestSource("fromString.pure", "Class Table\n" +
                "{\n" +
                "    name : String[1];\n" +
                "    columns : Column[*];\n" +
                "}\n" +
                "Class Column\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n");

        Assert.assertEquals("Table instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                Class instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Table instance Class\n" +
                "    generalizations(Property):\n" +
                "        Anonymous_StripedId instance Generalization\n" +
                "            general(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Any instance Class\n" +
                "            specific(Property):\n" +
                "                Table instance Class\n" +
                "    name(Property):\n" +
                "        Table instance String\n" +
                "    package(Property):\n" +
                "        Root instance Package\n" +
                "    properties(Property):\n" +
                "        name instance Property\n" +
                "            aggregation(Property):\n" +
                "                None instance AggregationKind\n" +
                "                    name(Property):\n" +
                "                        None instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    multiplicityArguments(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    rawType(Property):\n" +
                "                        Property instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                name instance Property\n" +
                "                                    [... >3]\n" +
                "                            propertyName(Property):\n" +
                "                                classifierGenericType instance String\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance ImportStub\n" +
                "                                    [... >3]\n" +
                "                            referenceUsages(Property):\n" +
                "                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                    [... >3]\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                String instance PrimitiveType\n" +
                "                            referenceUsages(Property):\n" +
                "                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                    [... >3]\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        String instance PrimitiveType\n" +
                "            multiplicity(Property):\n" +
                "                PureOne instance PackageableMultiplicity\n" +
                "            name(Property):\n" +
                "                name instance String\n" +
                "            owner(Property):\n" +
                "                Table instance Class\n" +
                "        columns instance Property\n" +
                "            aggregation(Property):\n" +
                "                None instance AggregationKind\n" +
                "                    name(Property):\n" +
                "                        None instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    multiplicityArguments(Property):\n" +
                "                        ZeroMany instance PackageableMultiplicity\n" +
                "                    rawType(Property):\n" +
                "                        Property instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                columns instance Property\n" +
                "                                    [... >3]\n" +
                "                            propertyName(Property):\n" +
                "                                classifierGenericType instance String\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance ImportStub\n" +
                "                                    [... >3]\n" +
                "                            referenceUsages(Property):\n" +
                "                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                    [... >3]\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance ImportStub\n" +
                "                                    [... >3]\n" +
                "                            referenceUsages(Property):\n" +
                "                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                    [... >3]\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance ImportStub\n" +
                "                            idOrPath(Property):\n" +
                "                                Column instance String\n" +
                "                            importGroup(Property):\n" +
                "                                import_fromString_pure_1 instance ImportGroup\n" +
                "                            resolvedNode(Property):\n" +
                "                                Column instance Class\n" +
                "            multiplicity(Property):\n" +
                "                ZeroMany instance PackageableMultiplicity\n" +
                "            name(Property):\n" +
                "                columns instance String\n" +
                "            owner(Property):\n" +
                "                Table instance Class\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance ImportStub\n" +
                "                            idOrPath(Property):\n" +
                "                                Table instance String\n" +
                "                            importGroup(Property):\n" +
                "                                import_fromString_pure_1 instance ImportGroup\n" +
                "                            resolvedNode(Property):\n" +
                "                                Table instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    [... >3]\n" +
                "                            propertyName(Property):\n" +
                "                                typeArguments instance String\n" +
                "            propertyName(Property):\n" +
                "                rawType instance String\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance ImportStub\n" +
                "                            idOrPath(Property):\n" +
                "                                Table instance String\n" +
                "                            importGroup(Property):\n" +
                "                                import_fromString_pure_1 instance ImportGroup\n" +
                "                            resolvedNode(Property):\n" +
                "                                Table instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    [... >3]\n" +
                "                            propertyName(Property):\n" +
                "                                typeArguments instance String\n" +
                "            propertyName(Property):\n" +
                "                rawType instance String", this.runtime.getCoreInstance("Table").printWithoutDebug("", 3));

        Assert.assertEquals("Column instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                Class instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Column instance Class\n" +
                "    generalizations(Property):\n" +
                "        Anonymous_StripedId instance Generalization\n" +
                "            general(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Any instance Class\n" +
                "            specific(Property):\n" +
                "                Column instance Class\n" +
                "    name(Property):\n" +
                "        Column instance String\n" +
                "    package(Property):\n" +
                "        Root instance Package\n" +
                "    properties(Property):\n" +
                "        name instance Property\n" +
                "            aggregation(Property):\n" +
                "                None instance AggregationKind\n" +
                "                    name(Property):\n" +
                "                        None instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    multiplicityArguments(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    rawType(Property):\n" +
                "                        Property instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            [... >2]\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            [... >2]\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            [... >2]\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        String instance PrimitiveType\n" +
                "            multiplicity(Property):\n" +
                "                PureOne instance PackageableMultiplicity\n" +
                "            name(Property):\n" +
                "                name instance String\n" +
                "            owner(Property):\n" +
                "                Column instance Class\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance ImportStub\n" +
                "                            [... >2]\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            [... >2]\n" +
                "            propertyName(Property):\n" +
                "                rawType instance String\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance ImportStub\n" +
                "                            [... >2]\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            [... >2]\n" +
                "            propertyName(Property):\n" +
                "                rawType instance String", this.runtime.getCoreInstance("Column").printWithoutDebug("", 2));
    }

    @Test
    public void testInstance()
    {
        compileTestSource("fromString.pure", "Class Table\n" +
                "{\n" +
                "    name : String[1];\n" +
                "    columns : Column[*];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class Column\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "^Table instance\n" +
                "(\n" +
                "    name = 'Hello',\n" +
                "    columns = [\n" +
                "        ^Column\n" +
                "            (\n" +
                "                name='Test'\n" +
                "            ),\n" +
                "        ^Column\n" +
                "            (\n" +
                "\n" +
                "            )\n" +
                "        ]\n" +
                ")");
        Assert.assertEquals("instance instance Table\n" +
                "    columns(Property):\n" +
                "        Anonymous_StripedId instance Column\n" +
                "            name(Property):\n" +
                "                Test instance String\n" +
                "        Anonymous_StripedId instance Column\n" +
                "    name(Property):\n" +
                "        Hello instance String", this.runtime.getCoreInstance("instance").printWithoutDebug(""));
    }

    @Test
    public void testGetProperties()
    {
        compileTestSource("test.pure",
                "Class test::A extends test::C {\n" +
                        "  propA: String[1];\n" +
                        "  qpropA(){''}: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::B {\n" +
                        "  propB: String[1];\n" +
                        "  qpropB(){''}: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::C {\n" +
                        "  propC: String[1];\n" +
                        "  qpropC(){''}: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::D {\n" +
                        "  propD: String[1];\n" +
                        "  qpropD(){''}: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::AB\n" +
                        "{\n" +
                        "  propAB_A: test::A[1];\n" +
                        "  propAB_B: test::B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::CD\n" +
                        "{\n" +
                        "  propCD_C: test::C[1];\n" +
                        "  propCD_D: test::D[1];\n" +
                        "}");

        Assert.assertEquals(_Class.getQualifiedProperties(runtime.getCoreInstance("test::A"), runtime.getProcessorSupport()).size(), 2);
        Assert.assertEquals(_Class.getSimpleProperties(runtime.getCoreInstance("test::A"), runtime.getProcessorSupport()).size(), 6);
    }
}
