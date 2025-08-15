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

package org.finos.legend.pure.m3.navigation.graph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.graph.GraphPath.EdgeConsumer;
import org.finos.legend.pure.m3.navigation.graph.GraphPath.EdgeVisitor;
import org.finos.legend.pure.m3.navigation.graph.GraphPath.ToManyPropertyAtIndexEdge;
import org.finos.legend.pure.m3.navigation.graph.GraphPath.ToManyPropertyWithStringKeyEdge;
import org.finos.legend.pure.m3.navigation.graph.GraphPath.ToOnePropertyEdge;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.function.Function;

public class TestGraphPath extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(
                getFunctionExecution(),
                new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository(), GenericCodeRepository.build("test", "test(::.*)?", "platform"))),
                getFactoryRegistryOverride(),
                getOptions(),
                Tuples.pair(
                        "/test/_testModel.pure",
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
                                "  4prop : Date[*];\n" +
                                "}\n")
        );
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
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties['name with escaped text, \\'\\n\\b\\\\, and other unusual characters, \"#$%^.'].genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "name with escaped text, '\n\b\\, and other unusual characters, \"#$%^.").addToOneProperties("genericType", "rawType").build().getDescription());
        Assert.assertEquals(
                "test::domain::ClassA.properties[functionName='prop2'].genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "functionName", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getDescription());
        Assert.assertEquals(
                "::.children['test']",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assert.assertEquals(
                "::.children['test'].children['domain']",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assert.assertEquals(
                "::.children['test'].children['domain'].children['ClassA']",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getDescription());
        Assert.assertEquals(
                "Root.children['test']",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assert.assertEquals(
                "Root.children['test'].children['domain']",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assert.assertEquals(
                "Root.children['test'].children['domain'].children['ClassA']",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

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
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").getDescription());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum']",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").getDescription());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").getDescription());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").getDescription());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").getDescription());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure.nonCanonicalUnits['Actus']",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().getDescription());
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
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.properties->find(x | $x.name == 'prop2')->toOne().genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assert.assertEquals(
                "test::domain::ClassA.properties->find(x | $x.name == 'prop2')->toOne().genericType.rawType",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getPureExpression());
        Assert.assertEquals(
                "::.children->find(x | $x.name == 'test')->toOne()",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assert.assertEquals(
                "::.children->find(x | $x.name == 'test')->toOne().children->find(x | $x.name == 'domain')->toOne()",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assert.assertEquals(
                "::.children->find(x | $x.name == 'test')->toOne().children->find(x | $x.name == 'domain')->toOne().children->find(x | $x.name == 'ClassA')->toOne()",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getPureExpression());
        Assert.assertEquals(
                "Root.children->find(x | $x.name == 'test')->toOne()",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assert.assertEquals(
                "Root.children->find(x | $x.name == 'test')->toOne().children->find(x | $x.name == 'domain')->toOne()",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assert.assertEquals(
                "Root.children->find(x | $x.name == 'test')->toOne().children->find(x | $x.name == 'domain')->toOne().children->find(x | $x.name == 'ClassA')->toOne()",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

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
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").getPureExpression());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits->find(x | $x.name == 'Cubitum')->toOne()",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").getPureExpression());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits->find(x | $x.name == 'Cubitum')->toOne().measure",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").getPureExpression());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits->find(x | $x.name == 'Cubitum')->toOne().measure.canonicalUnit",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").getPureExpression());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits->find(x | $x.name == 'Cubitum')->toOne().measure.canonicalUnit.measure",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").getPureExpression());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits->find(x | $x.name == 'Cubitum')->toOne().measure.canonicalUnit.measure.nonCanonicalUnits->find(x | $x.name == 'Actus')->toOne()",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().getPureExpression());
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
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assert.assertEquals(
                "test::domain::ClassA",
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());

        Assert.assertEquals(
                "::",
                GraphPath.buildPath("::").getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assert.assertEquals(
                "::",
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

        Assert.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assert.assertEquals(
                "Root",
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

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
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").getStartNodePath());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").build().getStartNodePath());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").build().getStartNodePath());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").build().getStartNodePath());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").build().getStartNodePath());
        Assert.assertEquals(
                "meta::pure::functions::meta::tests::model::RomanLength",
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().getStartNodePath());
    }

    @Test
    public void testResolve()
    {
        testResolve(processorSupport::package_getByUserPath);
        testResolve(path -> _Package.getByUserPath(path, repository::getTopLevel));
    }
    
    private void testResolve(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToOne("classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.Class),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                ImportStub.withImportStubByPass(GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().resolve(packagePathResolver), processorSupport));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").build().resolve(packagePathResolver));

        Assert.assertSame(
                runtime.getCoreInstance("::"),
                GraphPath.buildPath("::").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Root"),
                GraphPath.buildPath("Root").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build().resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().resolve(packagePathResolver));

        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer").resolve(packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolve(packagePathResolver));

        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").resolve(packagePathResolver));
        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").build().resolve(packagePathResolver));
        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").build().resolve(packagePathResolver));
        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueForMetaPropertyToOne("canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").build().resolve(packagePathResolver));
        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").build().resolve(packagePathResolver));
        Assert.assertEquals(
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Actus"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().resolve(packagePathResolver));
    }

    @Test
    public void testResolveUpTo()
    {
        testResolveUpTo(processorSupport::package_getByUserPath);
        testResolveUpTo(path -> _Package.getByUserPath(path, repository::getTopLevel));
    }

    private void testResolveUpTo(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        Assert.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolveUpTo(3, packagePathResolver));

        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(0, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-3, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(1, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-2, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(2, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-1, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(3, packagePathResolver));
        Assert.assertEquals(
                "Index: 4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(4, packagePathResolver)).getMessage());
        Assert.assertEquals(
                "Index: -4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-4, packagePathResolver)).getMessage());


        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(0, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-3, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(1, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-2, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(2, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-1, packagePathResolver));
        Assert.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(3, packagePathResolver));
        Assert.assertEquals(
                "Index: 4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(4, packagePathResolver)).getMessage());
        Assert.assertEquals(
                "Index: -4; size: 3",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-4, packagePathResolver)).getMessage());
    }

    @Test
    public void testResolveFully()
    {

    }

    private void testResolveFully(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"),
                runtime.getCoreInstance("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToOne("classifierGenericType"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType"),
                runtime.getCoreInstance("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToOne("classifierGenericType"),
                runtime.getCoreInstance(M3Paths.Class));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build(),
                runtime.getCoreInstance("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToMany("properties").get(0),
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToMany("properties").get(0).getValueForMetaPropertyToOne("genericType"),
                runtime.getCoreInstance(M3Paths.String));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build(),
                runtime.getCoreInstance("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").build(),
                runtime.getCoreInstance("test::domain::ClassA"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                runtime.getCoreInstance("test::domain::ClassB"));

        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("::"),
                runtime.getCoreInstance("::"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("Root"),
                runtime.getCoreInstance("Root"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build(),
                runtime.getCoreInstance("::"),
                runtime.getCoreInstance("test"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build(),
                runtime.getCoreInstance("::"),
                runtime.getCoreInstance("test"),
                runtime.getCoreInstance("test::domain"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build(),
                runtime.getCoreInstance("::"),
                runtime.getCoreInstance("test"),
                runtime.getCoreInstance("test::domain"),
                runtime.getCoreInstance("test::domain::ClassA"));

        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("Integer"),
                runtime.getCoreInstance("Integer"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType"),
                runtime.getCoreInstance("Integer"),
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                runtime.getCoreInstance("Number"));

        assertResolveFully(
                packagePathResolver,
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").build(),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").build(),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").build(),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueForMetaPropertyToOne("canonicalUnit"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").build(),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueForMetaPropertyToOne("canonicalUnit"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"));
        assertResolveFully(
                packagePathResolver,
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build(),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Cubitum"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueForMetaPropertyToOne("canonicalUnit"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength"),
                runtime.getCoreInstance("meta::pure::functions::meta::tests::model::RomanLength").getValueInValueForMetaPropertyToMany("nonCanonicalUnits", "Actus"));
    }

    private void assertResolveFully(Function<? super String, ? extends CoreInstance> packagePathResolver, GraphPath path, CoreInstance... nodes)
    {
        Assert.assertEquals(new ResolvedGraphPath(path, Lists.immutable.with(nodes)), path.resolveFully(packagePathResolver));
    }

    @Test
    public void testSubpath()
    {
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").subpath(0));
        Assert.assertEquals(
                "Index: 1; size: 0",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").subpath(1)).getMessage());
        Assert.assertEquals(
                "Index: -1; size: 0",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").subpath(-1)).getMessage());

        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit").subpath(0));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(0));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(-1));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(1));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(2));
        Assert.assertEquals(
                "Index: 3; size: 2",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(3)).getMessage());
        Assert.assertEquals(
                "Index: -3; size: 2",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure").subpath(-3)).getMessage());

        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(0));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-5));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(1));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-4));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(2));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-3));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure", "canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(3));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure", "canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-2));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure", "canonicalUnit", "measure"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(4));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit", "measure", "canonicalUnit", "measure"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-1));
        Assert.assertEquals(
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build(),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(5));
        Assert.assertEquals(
                "Index: 6; size: 5",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(6)).getMessage());
        Assert.assertEquals(
                "Index: -6; size: 5",
                Assert.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToOneProperties("canonicalUnit", "measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().subpath(-6)).getMessage());
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
                GraphPath.builder("test::domain::ClassB").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath(M3Paths.Class, "name"),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode", "classifierGenericType", "rawType", "name").build().reduce(processorSupport));

        Assert.assertEquals(
                GraphPath.buildPath("::"),
                GraphPath.buildPath("::").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.builder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

        Assert.assertEquals(
                GraphPath.buildPath("Root"),
                GraphPath.buildPath("Root").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.builder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

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
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength").reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").build(),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperty("measure").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength", "canonicalUnit"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.buildPath("meta::pure::functions::meta::tests::model::RomanLength"),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").build().reduce(processorSupport));
        Assert.assertEquals(
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build(),
                GraphPath.builder("meta::pure::functions::meta::tests::model::RomanLength").addToManyPropertyValueWithName("nonCanonicalUnits", "Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "Actus").build().reduce(processorSupport));
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
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.builder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build());
    }

    @Test
    public void testParse()
    {
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA").build(),
                GraphPath.parse("test::domain::ClassA"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA").build(),
                GraphPath.parse("prefix_test::domain::ClassA_suffix", 7, 27));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA.properties[0].genericType.rawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueWithName("properties", "prop2")
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA.properties['prop2'].genericType.rawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueWithKey("properties", "name", "prop2")
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA.properties[name='prop2'].genericType.rawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueWithKey("properties", "name", "name with escaped text, '\n\b\\][, and other unusual characters, \"#$%^.")
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA.properties[name='name with escaped text, \\'\\n\\b\\\\][, and other unusual characters, \"#$%^.'].genericType.rawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueWithKey("properties", "name", "name with escaped text, '\n\b\\, and other unusual characters, \"#$%^.")
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("something_test::domain::ClassA.properties[name='name with escaped text, \\'\\n\\b\\\\, and other unusual characters, \"#$%^.'].genericType.rawType_somethingElse", 10,  140));

        // with excess whitespace
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse(" \t  test::domain::ClassA.properties[0].genericType.rawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA.properties[0].genericType.rawType   \n"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("\ttest::domain::ClassA.properties[0].genericType.rawType\n"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA\n\t.properties[0]\n\t.genericType.\n\trawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("test::domain::ClassA\r\n\t.properties[0]\r\n\t.genericType.\r\n\trawType"));
        Assert.assertEquals(
                GraphPath.builder("test::domain::ClassA")
                        .addToManyPropertyValueAtIndex("properties", 0)
                        .addToOneProperties("genericType", "rawType")
                        .build(),
                GraphPath.parse("prefix_test::domain::ClassA\r\n\t.properties[0]\r\n\t.genericType.\r\n\trawType_suffix", 7, 70));
    }

    @Test
    public void testParseDescriptionRoundTrip()
    {
        ArrayAdapter.adapt(
                        "test::domain::ClassA",
                        "test::domain::ClassB",
                        "test::domain::ClassA.properties[0].genericType.rawType",
                        "test::domain::ClassA.properties['prop2'].genericType.rawType",
                        "test::domain::ClassA.properties['name with escaped text, \\'\\n\\b\\\\][][, and other unusual characters, \"#$%^.'].genericType.rawType",
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
                        "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum']",
                        "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure",
                        "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit",
                        "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure",
                        "meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure.nonCanonicalUnits['Actus']")
                .forEach(s -> Assert.assertEquals(s, GraphPath.parse(s).getDescription()));

        // excess whitespace is not present when generating the description
        Assert.assertEquals("test::domain::ClassA", GraphPath.parse("\t\ttest::domain::ClassA\n\n").getDescription());
        Assert.assertEquals("meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure.nonCanonicalUnits['Actus']", GraphPath.parse("meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum']\n\t.measure\n\t.canonicalUnit\n\t.measure.nonCanonicalUnits[    'Actus'    ]\n").getDescription());
    }

    @Test
    public void testDoesNotParse()
    {
        ArrayAdapter.adapt(
                        "",
                        "        ",
                        "\t\n",
                        "the quick brown fox jumped over the lazy dog",
                        "@#$%!@#$%",
                        ",",
                        "test::domain::ClassA!",
                        "#test::domain::ClassA",
                        "test::domain::ClassA.properties/0/",
                        "test::domain::ClassA.properties(0)",
                        "test::domain::ClassA.properties[invalid key='value']",
                        "test::domain.model::ClassA",
                        "test::domain::model::ClassA::",
                        "Integer.generalizations.general.rawType.",
                        "!Integer.generalizations.general.rawType",
                        "Integer.generalizations.general.rawType   etc...",
                        "meta::pure::functions::meta::tests::model::RomanLength~Cubitum",
                        "meta::pure::functions::meta::tests::model::RomanLength~Cubitum.measure")
                .forEach(s ->
                {
                    IllegalArgumentException e = Assert.assertThrows("'" + StringEscape.escape(s) + "'", IllegalArgumentException.class, () -> GraphPath.parse(s));
                    String expectedPrefix = "Invalid GraphPath description '" + StringEscape.escape(s) + "'";
                    String message = e.getMessage();
                    if (!message.startsWith(expectedPrefix))
                    {
                        Assert.assertEquals(expectedPrefix, e.getMessage());
                    }
                });
    }

    @Test
    public void testStartNodePath()
    {
        Assert.assertEquals("start node path may not be null", Assert.assertThrows(NullPointerException.class, () -> GraphPath.builder().withStartNodePath(null)).getMessage());
        ArrayAdapter.adapt(
                        "",
                        "        ",
                        "\t\n",
                        "the quick brown fox jumped over the lazy dog",
                        "@#$%!@#$%",
                        ",",
                        "test::domain::ClassA!",
                        "#test::domain::ClassA",
                        "test::domain::ClassA.properties/0/",
                        "test::domain::ClassA.properties(0)",
                        "test::domain::ClassA.properties[invalid key='value']",
                        "test::domain.model::ClassA",
                        "test::domain::model::ClassA::",
                        "test::domain::model::ClassA.properties[0]",
                        "Integer.generalizations.general.rawType.",
                        "!Integer.generalizations.general.rawType",
                        "Integer.generalizations.general.rawType   etc...",
                        "meta::pure::functions::meta::tests::model::RomanLength~Cubitum",
                        "meta::pure::functions::meta::tests::model::RomanLength~Cubitum.measure")
                .forEach(s ->
                {
                    IllegalArgumentException e = Assert.assertThrows("'" + StringEscape.escape(s) + "'", IllegalArgumentException.class, () -> GraphPath.builder().setStartNodePath(s));
                    String expectedPrefix = "Invalid start node path '" + StringEscape.escape(s) + "'";
                    String message = e.getMessage();
                    if (!message.startsWith(expectedPrefix))
                    {
                        Assert.assertEquals(expectedPrefix, e.getMessage());
                    }
                });
    }

    @Test
    public void testPropertyName()
    {
        GraphPath.Builder builder = GraphPath.builder().withStartNodePath("test::domain::ClassA");
        Assert.assertEquals("property may not be null", Assert.assertThrows(NullPointerException.class, () -> builder.addToOneProperty(null)).getMessage());
        ArrayAdapter.adapt(
                        "",
                        "        ",
                        "\t\n",
                        "the quick brown fox jumped over the lazy dog",
                        "@#$%!@#$%",
                        ",",
                        "pro%perty")
                .forEach(s ->
                {
                    IllegalArgumentException e = Assert.assertThrows("'" + StringEscape.escape(s) + "'", IllegalArgumentException.class, () -> builder.addToOneProperty(s));
                    String expectedPrefix = "Invalid property name '" + StringEscape.escape(s) + "'";
                    String message = e.getMessage();
                    if (!message.startsWith(expectedPrefix))
                    {
                        Assert.assertEquals(expectedPrefix, e.getMessage());
                    }
                });
    }

    @Test
    public void testPropertyKeyName()
    {
        GraphPath.Builder builder = GraphPath.builder().withStartNodePath("test::domain::ClassA");
        Assert.assertEquals("key property may not be null", Assert.assertThrows(NullPointerException.class, () -> builder.addToManyPropertyValueWithKey("properties", null, "key")).getMessage());
        ArrayAdapter.adapt(
                        "",
                        "        ",
                        "\t\n",
                        "the quick brown fox jumped over the lazy dog",
                        "@#$%!@#$%",
                        ",",
                        "pro%perty")
                .forEach(s ->
                {
                    IllegalArgumentException e = Assert.assertThrows("'" + StringEscape.escape(s) + "'", IllegalArgumentException.class, () -> builder.addToManyPropertyValueWithKey("properties", s, "key"));
                    String expectedPrefix = "Invalid key property name '" + StringEscape.escape(s) + "'";
                    String message = e.getMessage();
                    if (!message.startsWith(expectedPrefix))
                    {
                        Assert.assertEquals(expectedPrefix, e.getMessage());
                    }
                });
    }

    @Test
    public void testEdgeVisitor()
    {
        EdgeVisitor<String> visitor = new EdgeVisitor<String>()
        {
            @Override
            public String visit(ToOnePropertyEdge edge)
            {
                return edge.getProperty();
            }

            @Override
            public String visit(ToManyPropertyAtIndexEdge edge)
            {
                return edge.getProperty() + " / " + edge.getIndex();
            }

            @Override
            public String visit(ToManyPropertyWithStringKeyEdge edge)
            {
                return edge.getProperty() + " / " + edge.getKeyProperty() + " / " + edge.getKey();
            }
        };
        Assert.assertEquals(
                Lists.mutable.with("properties / 0", "genericType", "rawType"),
                GraphPath.parse("test::domain::ClassA.properties[0].genericType.rawType").getEdges().collect(e -> e.visit(visitor)));
        Assert.assertEquals(
                Lists.mutable.with("properties / name / prop2", "genericType", "rawType"),
                GraphPath.parse("test::domain::ClassA.properties['prop2'].genericType.rawType").getEdges().collect(e -> e.visit(visitor)));
        Assert.assertEquals(
                Lists.mutable.with("nonCanonicalUnits / name / Cubitum", "measure", "canonicalUnit", "measure", "nonCanonicalUnits / name / Actus"),
                GraphPath.parse("meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure.nonCanonicalUnits['Actus']").getEdges().collect(e -> e.visit(visitor)));
    }

    @Test
    public void testEdgeConsumer()
    {
        MutableList<ToOnePropertyEdge> toOneEdges = Lists.mutable.empty();
        MutableList<ToManyPropertyAtIndexEdge> toManyIndexEdges = Lists.mutable.empty();
        MutableList<ToManyPropertyWithStringKeyEdge> toManyKeyEdges = Lists.mutable.empty();
        EdgeConsumer consumer = new EdgeConsumer()
        {
            @Override
            protected void accept(ToOnePropertyEdge edge)
            {
                toOneEdges.add(edge);
            }

            @Override
            protected void accept(ToManyPropertyAtIndexEdge edge)
            {
                toManyIndexEdges.add(edge);
            }

            @Override
            protected void accept(ToManyPropertyWithStringKeyEdge edge)
            {
                toManyKeyEdges.add(edge);
            }
        };

        GraphPath.parse("test::domain::ClassA.properties[0].genericType.rawType").forEachEdge(consumer);
        Assert.assertEquals(Lists.mutable.with(new ToOnePropertyEdge("genericType"), new ToOnePropertyEdge("rawType")), toOneEdges);
        Assert.assertEquals(Lists.mutable.with(new ToManyPropertyAtIndexEdge("properties", 0)), toManyIndexEdges);
        Assert.assertEquals(Lists.mutable.empty(), toManyKeyEdges);

        toOneEdges.clear();
        toManyIndexEdges.clear();
        toManyKeyEdges.clear();
        GraphPath.parse("test::domain::ClassA.properties['prop2'].genericType.rawType").forEachEdge(consumer);
        Assert.assertEquals(Lists.mutable.with(new ToOnePropertyEdge("genericType"), new ToOnePropertyEdge("rawType")), toOneEdges);
        Assert.assertEquals(Lists.mutable.empty(), toManyIndexEdges);
        Assert.assertEquals(Lists.mutable.with(new ToManyPropertyWithStringKeyEdge("properties", "name", "prop2")), toManyKeyEdges);

        toOneEdges.clear();
        toManyIndexEdges.clear();
        toManyKeyEdges.clear();
        GraphPath.parse("meta::pure::functions::meta::tests::model::RomanLength.nonCanonicalUnits['Cubitum'].measure.canonicalUnit.measure.nonCanonicalUnits['Actus']").forEachEdge(consumer);
        Assert.assertEquals(Lists.mutable.with(new ToOnePropertyEdge("measure"), new ToOnePropertyEdge("canonicalUnit"), new ToOnePropertyEdge("measure")), toOneEdges);
        Assert.assertEquals(Lists.mutable.empty(), toManyIndexEdges);
        Assert.assertEquals(Lists.mutable.with(new ToManyPropertyWithStringKeyEdge("nonCanonicalUnits", "name", "Cubitum"), new ToManyPropertyWithStringKeyEdge("nonCanonicalUnits", "name", "Actus")), toManyKeyEdges);
    }
}
