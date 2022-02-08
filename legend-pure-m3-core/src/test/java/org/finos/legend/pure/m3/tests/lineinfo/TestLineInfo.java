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

package org.finos.legend.pure.m3.tests.lineinfo;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLineInfo extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testLineInfoForStaticInstanceGeneratedM3() throws Exception
    {
        MutableList<CoreInstance> parsed = Lists.mutable.with();
        new M3AntlrParser().parse("Class Person\n" +
                             "{\n" +
                             "   lastName:String[1];\n" +
                             "}\n" +
                             "^Person p ?[a/b/c/file.txt:1,3,2,2,4,5]? (lastName='last')\n", "fromString.pure", false, 0, this.repository, parsed, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context, 1, null);
        this.repository.validate(new VoidM4StateListener());
        PostProcessor.process(parsed, this.repository, new ParserLibrary(), new InlineDSLLibrary(), null, this.context, this.processorSupport, null, null);
        Validator.validateM3(parsed, ValidationType.DEEP, new ParserLibrary(), new InlineDSLLibrary(), this.runtime.getCodeStorage(), this.repository, this.context, this.processorSupport);

        CoreInstance p = this.runtime.getCoreInstance("p");
        Assert.assertNotNull(p);
        assertSourceInformation("a/b/c/file.txt", 1, 3, 2, 2, 4, 5, p.getSourceInformation());

        CoreInstance person = this.runtime.getCoreInstance("Person");
        Assert.assertNotNull(person);
        Assert.assertNull(person.getSourceInformation()); // Person doesn't have debug info because we use 'false' for addLines in the parse method!
    }

    @Test
    public void testLineInfoForDynamicInstanceGeneratedM3() throws Exception
    {
        this.compileTestSource("fromString.pure",
                          "Class pkg1::Person\n" +
                          "{\n" +
                          "   lastName:String[1];\n" +
                          "   other:pkg1::Person[0..1];\n" +
                          "   doubleLastName()\n" +
                          "   {\n" +
                          "      $this.lastName + ' ' + $this.lastName\n" +
                          "   }:String[1];\n" +
                          "}\n" +
                          "function pkg1::pkg2::printSomething():Any[1]\n" +
                          "{\n" +
                          "   let a = ^pkg1::Person klp (lastName = 'hello', other = ^pkg1::Person(lastName = 'otherOne'));\n" +
                          "}\n");
        // Test class
        CoreInstance person = this.runtime.getCoreInstance("pkg1::Person");
        Assert.assertNotNull(person);
        assertSourceInformation("fromString.pure", 1, 1, 1, 13, 9, 1, person.getSourceInformation());

        MapIterable<String, CoreInstance> propertiesByName = this.processorSupport.class_getSimplePropertiesByName(person);
        MapIterable<String, CoreInstance> qualifiedPropertiesByName = _Class.getQualifiedPropertiesByName(person, this.processorSupport);
        Assert.assertEquals(4, propertiesByName.size());

        CoreInstance lastName = propertiesByName.get("lastName");
        Assert.assertNotNull(lastName);
        assertSourceInformation("fromString.pure", 3, 4, 3, 4, 3, 22, lastName.getSourceInformation());

        CoreInstance other = propertiesByName.get("other");
        Assert.assertNotNull(other);
        assertSourceInformation("fromString.pure", 4, 4, 4, 4, 4, 28, other.getSourceInformation());

        CoreInstance doubleLastName = qualifiedPropertiesByName.get("doubleLastName()");
        Assert.assertNotNull(doubleLastName);
        assertSourceInformation("fromString.pure", 5, 4, 5, 4, 8, 15, doubleLastName.getSourceInformation());

        CoreInstance classifierGenericType = propertiesByName.get("classifierGenericType");
        Assert.assertNotNull(classifierGenericType);
        // Note: these source coordinates may change if m3.pure changes
        assertSourceInformation("/platform/pure/m3.pure", 965, 13, 965, 126, 972, 17, classifierGenericType.getSourceInformation());

        // Test function
        CoreInstance func = this.runtime.getCoreInstance("pkg1::pkg2::printSomething__Any_1_");
        Assert.assertNotNull(func);
        assertSourceInformation("fromString.pure", 10, 1, 10, 22, 13, 1, func.getSourceInformation());

        ListIterable<? extends CoreInstance> funcExpressions = Instance.getValueForMetaPropertyToManyResolved(func, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(1, funcExpressions.size());
        CoreInstance expression = funcExpressions.get(0);
        // TODO end column should be 95, not 6
        assertSourceInformation("fromString.pure", 12, 4, 12, 4, 12, 6, expression.getSourceInformation());

        CoreInstance newKlp = Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.parametersValues, this.processorSupport).get(1);
        assertSourceInformation("fromString.pure", 12, 12, 12, 26, 12, 95, newKlp.getSourceInformation());

        CoreInstance funcType = func.getClassifier();
        Assert.assertNotNull(funcType);
        Assert.assertEquals(M3Paths.ConcreteFunctionDefinition, PackageableElement.getUserPathForPackageableElement(funcType, "::"));
        // Note: these source coordinates may change if m3.pure changes
        assertSourceInformation("/platform/pure/m3.pure", 2156, 1, 2156, 88, 2170, 1, funcType.getSourceInformation());
    }

    @Test
    public void testLineInfoForImportGroup() throws Exception
    {
        this.compileTestSource("testImportGroup.pure", "import apps::something::*;\n" +
                                             "//comment\n" +
                                             "import   app3::something::*;\n" +
                                             "//comment\n" +
                                             "import app2::something::*;\n" +
                                             "\n" +
                                             "Class my::test::testClass\n" +
                                             "{\n" +
                                             "   name:my::test::testClass[1];\n" +
                                             "}\n" +
                                             "\n" +
                                             "###Pure\n" +
                                             "//comment\n" +
                                             "import   my::test::*;\n" +
                                             "//comment\n" +
                                             "import apps::somethingelse::*;\n" +
                                             "\n" +
                                             "function my::test::getTestClass():Any[*]\n" +
                                             "{\n" +
                                             "   testClass;\n" +
                                             "}\n");
        CoreInstance testInstance1 = this.runtime.getCoreInstance("my::test::testClass");
        CoreInstance testInstance2 = this.runtime.getCoreInstance("my::test::getTestClass__Any_MANY_");
        CoreInstance testImportGroupInstance1 = testInstance1.getValueForMetaPropertyToOne("properties").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType").getValueForMetaPropertyToOne("importGroup");
        CoreInstance testImportGroupInstance2 = testInstance2.getValueForMetaPropertyToOne("expressionSequence").getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("importGroup");

        assertSourceInformation("testImportGroup.pure", 1, 1, 1, 1, 5, 25, testImportGroupInstance1.getSourceInformation());
        assertSourceInformation("testImportGroup.pure", 14, 1, 14, 1, 16, 29, testImportGroupInstance2.getSourceInformation());

        MutableList<? extends CoreInstance> importsInstance1 = testImportGroupInstance1.getValueForMetaPropertyToMany("imports").toList();
        assertSourceInformation("testImportGroup.pure", 1, 1, 1, 8, 1, 25, importsInstance1.get(0).getSourceInformation());
        assertSourceInformation("testImportGroup.pure", 3, 1, 3, 10, 3, 27, importsInstance1.get(1).getSourceInformation());
        assertSourceInformation("testImportGroup.pure", 5, 1, 5, 8, 5, 25, importsInstance1.get(2).getSourceInformation());

        MutableList<? extends CoreInstance> importsInstance2 = testImportGroupInstance2.getValueForMetaPropertyToMany("imports").toList();
        assertSourceInformation("testImportGroup.pure", 14, 1, 14, 10, 14, 20, importsInstance2.get(0).getSourceInformation());
        assertSourceInformation("testImportGroup.pure", 16, 1, 16, 8, 16, 29, importsInstance2.get(1).getSourceInformation());
    }
}
