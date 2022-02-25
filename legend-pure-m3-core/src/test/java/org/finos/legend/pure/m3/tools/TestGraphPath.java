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
                        "}\n" +
                        "\n" +
                        "Measure test::domain::RomanLength\n" +
                        "{\n" +
                        "   *Pes: x -> $x;\n" +
                        "   Cubitum: x -> $x * 1.5;\n" +
                        "   Passus: x -> $x * 5;\n" +
                        "   Actus: x -> $x * 120;\n" +
                        "   Stadium: x -> $x * 625;\n" +
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

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getDescription());
        Assert.assertEquals(
                "::.children['test']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assert.assertEquals(
                "::.children['test'].children['domain']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assert.assertEquals(
                "::.children['test'].children['domain'].children['ClassA']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getDescription());
        Assert.assertEquals(
                "Root.children['test']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assert.assertEquals(
                "Root.children['test'].children['domain']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assert.assertEquals(
                "Root.children['test'].children['domain'].children['ClassA']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getDescription());
        Assert.assertEquals(
                "Integer.generalizations",
                GraphPath.buildPath("Integer", "generalizations").getDescription());
        Assert.assertEquals(
                "Integer.generalizations.general",
                GraphPath.buildPath("Integer", "generalizations", "general").getDescription());
        Assert.assertEquals(
                "Integer.generalizations.general.rawType",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getDescription());

        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getDescription());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getDescription());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getDescription());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getDescription());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits['RomanLength~Actus']",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getDescription());
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

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getPureExpression());
        Assert.assertEquals(
                "::.children->get('test')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assert.assertEquals(
                "::.children->get('test')->toOne().children->get('domain')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assert.assertEquals(
                "::.children->get('test')->toOne().children->get('domain')->toOne().children->get('ClassA')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getPureExpression());
        Assert.assertEquals(
                "Root.children->get('test')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assert.assertEquals(
                "Root.children->get('test')->toOne().children->get('domain')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assert.assertEquals(
                "Root.children->get('test')->toOne().children->get('domain')->toOne().children->get('ClassA')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getPureExpression());
        Assert.assertEquals(
                "Integer.generalizations",
                GraphPath.buildPath("Integer", "generalizations").getPureExpression());
        Assert.assertEquals(
                "Integer.generalizations.general",
                GraphPath.buildPath("Integer", "generalizations", "general").getPureExpression());
        Assert.assertEquals(
                "Integer.generalizations.general.rawType",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getPureExpression());

        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getPureExpression());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getPureExpression());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getPureExpression());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getPureExpression());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits->get('RomanLength~Actus')->toOne()",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getPureExpression());
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

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getStartNodePath());
        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations").getStartNodePath());
        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations", "general").getStartNodePath());
        Assert.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getStartNodePath());

        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getStartNodePath());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getStartNodePath());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getStartNodePath());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getStartNodePath());
        Assert.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getStartNodePath());
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

        Assert.assertSame(
                runtime.getCoreInstance("::"),
                GraphPath.buildPath("::").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Root"),
                GraphPath.buildPath("Root").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().resolve(processorSupport));

        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer").resolve(processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolve(processorSupport));

        Assert.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").resolve(processorSupport));
        Assert.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").resolve(processorSupport));
        Assert.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Pes"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").resolve(processorSupport));
        Assert.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").resolve(processorSupport));
        Assert.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Actus"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().resolve(processorSupport));
    }

    @Test
    public void testResolveUpTo()
    {
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolve(processorSupport));

        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(0, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-3, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(1, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-2, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(2, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-1, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(3, processorSupport));
        Assert.assertEquals(
                "Index: 4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(4, processorSupport)).getMessage());
        Assert.assertEquals(
                "Index: -4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-4, processorSupport)).getMessage());


        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(0, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-3, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(1, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-2, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(2, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-1, processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(3, processorSupport));
        Assert.assertEquals(
                "Index: 4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(4, processorSupport)).getMessage());
        Assert.assertEquals(
                "Index: -4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-4, processorSupport)).getMessage());
    }

    @Test
    public void testSubpath()
    {
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(0));
        Assert.assertEquals(
                "Index: 1; size: 0",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(1)).getMessage());
        Assert.assertEquals(
                "Index: -1; size: 0",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(-1)).getMessage());

        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(0));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(-1));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(1));
        Assert.assertEquals(
                "Index: 2; size: 1",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(2)).getMessage());
        Assert.assertEquals(
                "Index: -2; size: 1",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(-2)).getMessage());

        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(0));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-4));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(1));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-3));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(2));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-2));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(3));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-1));
        Assert.assertEquals(
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build(),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(4));
        Assert.assertEquals(
                "Index: 5; size: 4",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(5)).getMessage());
        Assert.assertEquals(
                "Index: -5; size: 4",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-5)).getMessage());
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

        Assert.assertEquals(
                GraphPath.buildPath("::"),
                GraphPath.buildPath("::").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

        Assert.assertEquals(
                GraphPath.buildPath("Root"),
                GraphPath.buildPath("Root").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

        Assert.assertEquals(
                GraphPath.buildPath("Integer"),
                GraphPath.buildPath("Integer").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("Integer", "generalizations"),
                GraphPath.buildPath("Integer", "generalizations").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("Integer", "generalizations", "general"),
                GraphPath.buildPath("Integer", "generalizations", "general").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("Number", "name"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType", "name").reduce(processorSupport));

        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Pes"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Actus"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().reduce(processorSupport));
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
                "test::domain::ClassA.properties[name='prop2'].genericType.rawType",
                "::",
                "::.children['test']",
                "::.children['test'].children['domain']",
                "::.children['test'].children['domain'].children['ClassA']",
                "Root",
                "Root.children['test']",
                "Root.children['test'].children['domain']",
                "Root.children['test'].children['domain'].children['ClassA']",
                "Integer",
                "Integer.generalizations",
                "Integer.generalizations.general",
                "Integer.generalizations.general.rawType",
                "test::domain::RomanLength~Cubitum",
                "test::domain::RomanLength~Cubitum.measure",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits['RomanLength~Actus']");
        for (String description : descriptions)
        {
            Assert.assertEquals(description, GraphPath.parseDescription(description).getDescription());
        }
    }
}
