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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGraphPath extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
        compileTestSource("/test/testModel.pure",
                "import test::domain::*;\n" +
                        "Class test::domain::ClassA\n" +
                        "{\n" +
                        "  prop1 : String[1];\n" +
                        "  prop2 : ClassB[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::domain::ClassB\n" +
                        "{\n" +
                        "  prop3 : String[0..1];\n" +
                        "}\n");
    }

    @Test
    public void testGetDescription()
    {
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.classifierGenericType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.classifierGenericType.rawType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties[0].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties[name='prop2'].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());
    }

    @Test
    public void testGetPureExpression()
    {
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.classifierGenericType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.classifierGenericType.rawType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.properties->at(0).genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.properties->get('prop2')->toOne().genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.properties->filter(x | $x.name == 'prop2')->toOne().genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());
    }

    @Test
    public void testGetStartNodePath()
    {
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());
    }

    @Test
    public void testResolve()
    {
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToOne("classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.Class),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                ImportStub.withImportStubByPass(GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().resolve(processorSupport), processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").build().resolve(processorSupport));
    }

    @Test
    public void testReduce()
    {
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath(M3Paths.Class),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath(M3Paths.Class, "name"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType", "name").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.newPathBuilder("test::domain::ClassB").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath(M3Paths.Class, "name"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode", "classifierGenericType", "rawType", "name").build().reduce(processorSupport));
    }

    @Test
    public void testEquals()
    {
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA"));
        Assert.assertNotEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassB"));

        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"));
        Assert.assertEquals(
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build());
    }

    @Test
    public void testParse()
    {
        ListIterable<String> descriptions = Lists.immutable.with(
                "test::domain::ClassA",
                "test::domain::ClassB",
                "test::domain::ClassA.properties[0].genericType.rawType",
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                "test::domain::ClassA.properties[name='prop2'].genericType.rawType");
        for (String description : descriptions)
        {
            Assert.assertEquals(description, GraphPath.parseDescription(description).getDescription());
        }
    }
}
