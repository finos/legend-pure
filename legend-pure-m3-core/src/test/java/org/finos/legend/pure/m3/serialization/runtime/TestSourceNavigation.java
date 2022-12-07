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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
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
    public void testNavigateForProperty()
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
    public void testNavigateVariable()
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

//        Source source = runtime.createInMemorySource(
//                "test.pure",
//                "function doSomething(param: String[1]): Any[*]\n" +
//                        "{\n" +
//                        "  let var = 1;\n" +
//                        "  $var->toString();\n" +
//                        "  $param->toString();\n" +
//                        "}"
//        );
//        runtime.compile();

        // variable
//        CoreInstance found = source.navigate(4, 4, processorSupport);
//        CoreInstance found = source.navigate(3, 7, processorSupport);

        // parameter
//        CoreInstance found = source.navigate(5, 4, processorSupport);
//        CoreInstance found = source.navigate(11, 44, processorSupport);
//        CoreInstance found = source.navigate(11, 44, processorSupport);
        CoreInstance found = source.navigate(5, 37, processorSupport);
        System.out.println("");
    }
}
