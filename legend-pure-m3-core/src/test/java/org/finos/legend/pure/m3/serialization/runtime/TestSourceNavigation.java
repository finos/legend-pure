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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSourceNavigation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterClass
    public static void _tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testNavigateForProperty() throws Exception
    {
        Source source = runtime.createInMemorySource(
                "simplePropertyChain.pure",
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
        Assert.assertEquals("simplePropertyChain.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(18, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 41, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("name", ((Property<?, ?>) found)._name());
        Assert.assertEquals("simplePropertyChain.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(12, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 63, processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("alias", ((Property<?, ?>) found)._name());
        Assert.assertEquals("simplePropertyChain.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(13, found.getSourceInformation().getLine());
        Assert.assertEquals(5, found.getSourceInformation().getColumn());
    }
}
