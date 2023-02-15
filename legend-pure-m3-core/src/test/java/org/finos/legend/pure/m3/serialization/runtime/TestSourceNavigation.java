// Copyright 2022 Goldman Sachs
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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSourceNavigation extends AbstractPureTestWithCoreCompiledPlatform
{
    @Before
    public void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testNavigateForPropertyReference()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                "Class test::ParentNode\n" +
                        "{\n" +
                        "    children: test::ChildNode[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::ChildNode\n" +
                        "{\n" +
                        "    id: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::NodeInfo {\n" +
                        "    name: String[1];\n" +
                        "    alias: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::AdvChildNode extends test::ChildNode\n" +
                        "{\n" +
                        "    info: test::NodeInfo[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::getChildNodeName(node:test::ParentNode[0..1]):String[1]\n" +
                        "{\n" +
                        "  if($node.children->isEmpty(),|'',|$node->match([\n" +
                        "    a:test::AdvChildNode[1]| $a.info.name + '(' + $a.info.alias + ')',\n" +
                        "    j:test::ChildNode[1]| 'generic'\n" +
                        "  ]));\n" +
                        "}"
        );
        runtime.compile();

        CoreInstance found = source.navigate(24, 35, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("info", ((Property<?, ?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(18, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 41, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("name", ((Property<?, ?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(12, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 63, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("alias", ((Property<?, ?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(13, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForClassOrAssociationProperty()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                "Class model::C1 {\n" +
                        "}\n" +
                        "\n" +
                        "Class model::C2 {\n" +
                        "  name: String[1];\n" +
                        "  al(){''}: String[1];  \n" +
                        "}\n" +
                        "\n" +
                        "Association model::Assoc\n" +
                        "{\n" +
                        "  prop3: model::C2[1];\n" +
                        "  prop2: model::C1[1];\n" +
                        "}"
        );
        runtime.compile();

        CoreInstance found = source.navigate(5, 3, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("name", ((Property<?, ?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(5, found.getSourceInformation().getLine());
        Assert.assertEquals(3, found.getSourceInformation().getColumn());

        found = source.navigate(6, 3, processorSupport);
        Assert.assertTrue(found instanceof QualifiedProperty);
        Assert.assertEquals("al", ((QualifiedProperty<?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(6, found.getSourceInformation().getLine());
        Assert.assertEquals(3, found.getSourceInformation().getColumn());

        found = source.navigate(11, 3, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("prop3", ((Property<?, ?>) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(11, found.getSourceInformation().getLine());
        Assert.assertEquals(3, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateEnumValue()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                "Enum model::TestEnum {\n" +
                        "  VAL1,\n" +
                        "  VAL2\n" +
                        "}\n" +
                        "\n" +
                        "function doSomething(): Any[*]\n" +
                        "{\n" +
                        "  model::TestEnum.VAL1->toString();\n" +
                        "}"
        );
        runtime.compile();

        // parameter
        CoreInstance found = source.navigate(8, 20, processorSupport);
        Assert.assertTrue(found instanceof EnumInstance);
        Assert.assertEquals("VAL1", ((EnumInstance) found)._name());
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(2, found.getSourceInformation().getLine());
        Assert.assertEquals(3, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateVariableAndParameter()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                "function doSomething(param: String[1]): Any[*]\n" +
                        "{\n" +
                        "  let var = 1;\n" +
                        "  $var->toString();\n" +
                        "  let var_lambda1 = var: String[1]|$var->toString();\n" +
                        "  let var_lambda2 = {x: String[1]| let var = 1; $var->toString();};\n" +
                        "  let var_lambda3 = {x: String[1]| $var->toString();};\n" +
                        "  $param->toString();\n" +
                        "  let param_lambda1 = param: String[1]|$param->toString();\n" +
                        "  let param_lambda2 = {x: String[1]| let param = 1; $param->toString();};\n" +
                        "  let param_lambda3 = {x: String[1]| $param->toString();};\n" +
                        "  let param_lambda4 = {x: String[1]| [1,2,3]->map(y|$param->toString());};\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance found = source.navigate(3, 7, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(3, found.getSourceInformation().getLine());
        Assert.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(4, 4, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(3, found.getSourceInformation().getLine());
        Assert.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(5, 21, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(5, found.getSourceInformation().getLine());
        Assert.assertEquals(21, found.getSourceInformation().getColumn());

        found = source.navigate(5, 37, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(5, found.getSourceInformation().getLine());
        Assert.assertEquals(21, found.getSourceInformation().getColumn());

        found = source.navigate(6, 40, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(6, found.getSourceInformation().getLine());
        Assert.assertEquals(40, found.getSourceInformation().getColumn());

        found = source.navigate(6, 50, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(6, found.getSourceInformation().getLine());
        Assert.assertEquals(40, found.getSourceInformation().getColumn());

        found = source.navigate(7, 37, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(3, found.getSourceInformation().getLine());
        Assert.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(8, 4, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(1, found.getSourceInformation().getLine());
        Assert.assertEquals(22, found.getSourceInformation().getColumn());

        found = source.navigate(9, 23, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(9, found.getSourceInformation().getLine());
        Assert.assertEquals(23, found.getSourceInformation().getColumn());

        found = source.navigate(9, 41, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(9, found.getSourceInformation().getLine());
        Assert.assertEquals(23, found.getSourceInformation().getColumn());

        found = source.navigate(10, 42, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(10, found.getSourceInformation().getLine());
        Assert.assertEquals(42, found.getSourceInformation().getColumn());

        found = source.navigate(10, 54, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(10, found.getSourceInformation().getLine());
        Assert.assertEquals(42, found.getSourceInformation().getColumn());

        found = source.navigate(11, 39, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(1, found.getSourceInformation().getLine());
        Assert.assertEquals(22, found.getSourceInformation().getColumn());

        found = source.navigate(12, 54, processorSupport);
        Assert.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(1, found.getSourceInformation().getLine());
        Assert.assertEquals(22, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateFunctionDescriptor()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                "function doSomething(param: String[1]): Any[*]\n" +
                        "{\n" +
                        "  [\n" +
                        "    print_Any_MANY__Integer_1__Nil_0_\n" +
                        "  ];\n" +
                        "  print_Any_MANY__Integer_1__Nil_0_;\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance found = source.navigate(4, 7, processorSupport);
        Assert.assertEquals("/platform/pure/basics/io/print.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(15, found.getSourceInformation().getLine());
        Assert.assertEquals(44, found.getSourceInformation().getColumn());

        found = source.navigate(6, 7, processorSupport);
        Assert.assertEquals("/platform/pure/basics/io/print.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(15, found.getSourceInformation().getLine());
        Assert.assertEquals(44, found.getSourceInformation().getColumn());
    }
}
