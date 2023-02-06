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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestIncrementalCompiler extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    /**
     * This is not supported or intended to be supported in PURE at the moment, but leaving this to document that.
     */
    @Ignore
    @Test
    public void testInstanceDefinedBeforeClassWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1", false, false, "^my::Table instance\n" +
                "(\n" +
                "    name = 'Hello'\n" +
                ")"));
        sources.add(new Source("2", false, false, "Class my::Table\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n"));
        runtime.getIncrementalCompiler().compile(sources);

        Assert.assertEquals("instance instance Table\n" +
                "    name(Property):\n" +
                "        Hello instance String", runtime.getCoreInstance("instance").printWithoutDebug(""));
    }

    @Test
    public void testClassInstanceUsedBeforeClassWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1.pure", false, false, "function my::tableName():String[1]\n" +
                "{\n" +
                "    let t = ^my::Table(name = 'Hello');\n" +
                "    $t.name;\n" +
                "}\n"));
        sources.add(new Source("2.pure", false, false, "Class my::Table\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n"));
        runtime.getIncrementalCompiler().compile(sources);

        Assert.assertEquals("tableName__String_1_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    [... >1]\n" +
                "    expressionSequence(Property):\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                letFunction_String_1__T_m__T_m_ instance NativeFunction\n" +
                "            functionName(Property):\n" +
                "                letFunction instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    [... >1]\n" +
                "            importGroup(Property):\n" +
                "                import_1_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                PureOne instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    [... >1]\n" +
                "                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                    [... >1]\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    [... >1]\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                name instance Property\n" +
                "                    [... >1]\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    [... >1]\n" +
                "            importGroup(Property):\n" +
                "                import_1_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                PureOne instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance VariableExpression\n" +
                "                    [... >1]\n" +
                "            propertyName(Property):\n" +
                "                my$tableName$1$system$imports$import_1_pure_1$0 instance InstanceValue\n" +
                "                    [... >1]\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    [... >1]\n" +
                "    functionName(Property):\n" +
                "        tableName instance String\n" +
                "    name(Property):\n" +
                "        tableName__String_1_ instance String\n" +
                "    package(Property):\n" +
                "        my instance Package", runtime.getFunction("my::tableName():String[1]").printWithoutDebug("", 1));
    }

    @Test
    public void testInstanceWithPropertyReferencingEnumBeforeEnumDefinedWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1.pure", false, false, "Class my::myClass\n" +
                "{\n" +
                "    value : my::myEnum[1];\n" +
                "}\n" +
                "^my::myClass instance\n" +
                "(\n" +
                "    value = my::myEnum.VAL1\n" +
                ")"));
        sources.add(new Source("2.pure", false, false, "Enum my::myEnum\n" +
                "{\n" +
                "    VAL1, VAL2\n" +
                "}\n"));
        runtime.getIncrementalCompiler().compile(sources);

        Assert.assertEquals("instance instance myClass\n" +
                "    value(Property):\n" +
                "        Anonymous_StripedId instance EnumStub\n" +
                "            enumName(Property):\n" +
                "                VAL1 instance String\n" +
                "            enumeration(Property):\n" +
                "                Anonymous_StripedId instance ImportStub\n" +
                "                    idOrPath(Property):\n" +
                "                        my::myEnum instance String\n" +
                "                    importGroup(Property):\n" +
                "                        import_1_pure_1 instance ImportGroup", runtime.getCoreInstance("instance").printWithoutDebug("", 1));
    }
}
