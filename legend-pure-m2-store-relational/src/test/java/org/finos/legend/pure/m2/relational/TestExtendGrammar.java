// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;


import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestExtendGrammar extends AbstractPureRelationalTestWithCoreCompiled
{

    private RelationalGraphWalker graphWalker;

    @Before
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(this.runtime, this.processorSupport);
    }


    @Test
    public void testExtend()
    {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +

                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                        "    *Person[superClass]: Relational\n" +
                        "    {\n" +
                        "       name : [db]employeeTable.name \n" +
                        "    }\n" +
                        "    Person[p_subclass] extends [superClass]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMappingById(mapping, "superClass");
        Assert.assertNotNull(personMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance personSubMapping = this.graphWalker.getClassMappingById(mapping, "p_subclass");
        Assert.assertNotNull(personSubMapping);
        Assert.assertFalse(PrimitiveUtilities.getBooleanValue(personSubMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personSubMapping)));
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(personSubMapping).size());
        Assert.assertEquals("superClass", personSubMapping.getValueForMetaPropertyToOne(M2MappingProperties.superSetImplementationId).getName());
    }


    @Test
    public void testExtendEmptySubtype()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    otherInfo:String[1];\n" +
                        "}\n" +
                        "Class other::MyPerson extends other::Person\n" +
                        "{\n" +
                        "}\n" +

                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    otherInfo VARCHAR(200)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    *Person[superClass]: Relational\n" +
                        "    {\n" +
                        "       name : [db]employeeTable.name,\n" +
                        "       otherInfo: [db]employeeTable.otherInfo\n" +
                        "    }\n" +
                        "    MyPerson[p_subclass] extends [superClass]: Relational\n" +
                        "    {\n" +
                        "    }\n" +
                        ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMappingById(mapping, "superClass");
        Assert.assertNotNull(personMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance personSubMapping = this.graphWalker.getClassMappingById(mapping, "p_subclass");
        Assert.assertNotNull(personSubMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(personSubMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personSubMapping)));
        Assert.assertEquals(0, this.graphWalker.getClassMappingImplementationPropertyMappings(personSubMapping).size());
        Assert.assertEquals("superClass", personSubMapping.getValueForMetaPropertyToOne(M2MappingProperties.superSetImplementationId).getName());

    }


    @Test
    public void testInvalidExtendSubtype()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "Class other::MyPerson extends other::Person\n" +
                            "{\n" +
                            "    title:String[1];\n" +
                            "}\n" +

                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    *Person[superClass]: Relational\n" +
                            "    {\n" +
                            "       name : [db]employeeTable.name,\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            "    MyPerson[p_subclass] extends [superClass]: Relational\n" +
                            "    {\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid extends mapping. Class [MyPerson] has properties of it's own. Extends mappings are only allowed for subtypes when the subtype has no simple properties of it's own", "fromString.pure", 31, 5, e);
        }
    }

    @Test
    public void testExtendInvalidSet()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    *Person[superClass]: Relational\n" +
                            "    {\n" +
                            "       name : [db]employeeTable.name \n" +
                            "    }\n" +
                            "    Person[p_subclass] extends [badId]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
            Assert.fail(" this should not compile!");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 28, 5, e);
        }

    }

    @Test
    public void testExtendWithInclude()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    otherInfo:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firm VARCHAR(200),\n" +
                        "    otherInfo VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    *Person[superClass]: Relational\n" +
                        "    {\n" +
                        "       name : [db]employeeTable.name \n" +
                        "    }\n" +
                        ")\n" +
                        "Mapping mappingPackage::myMappingWithIncludes\n" +
                        "(\n" +
                        "    include mappingPackage::myMapping\n" +
                        "    Person[p_subclass] extends [superClass]: Relational\n" +
                        "    {\n" +
                        "       otherInfo: [db]employeeTable.otherInfo\n" +
                        "    }\n" +
                        ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();


    }

    @Test
    public void testExtendInvalidIdWithInclude()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    *Person[superClass]: Relational\n" +
                            "    {\n" +
                            "       name : [db]employeeTable.name \n" +
                            "    }\n" +
                            ")\n" +
                            "Mapping mappingPackage::myMappingWithIncludes\n" +
                            "(\n" +
                            "    include mappingPackage::myMapping\n" +
                            "    Person[p_subclass] extends [badId]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 32, 5, e);
        }

    }

    @Test
    public void testExtendInvalidSetCannotBeSelf()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    Person[p_subclass] extends [p_subclass]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
            Assert.fail("this should not compile");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Extend mapping id cannot reference self 'p_subclass'", "fromString.pure", 24, 5, e);

        }

    }


    @Test
    public void testExtendInvalidSetCannotBeEmpty()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    Person[p_subclass] extends []: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
            Assert.fail("this should not parse");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: a valid identifier text; found: ']'", "fromString.pure", 24, 33, e);

        }

    }

    @Test
    public void testExtendInvalidSetCannotBeDifferentMappingTypes()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "   *Person[pure]: Pure\n" +
                            "    {\n" +
                            "       name :'Test'\n" +
                            "    }\n" +
                            "    Person[p_subclass] extends [pure]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();

            Assert.fail();


        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 28, 5, e);

        }

    }

    @Test
    public void testExtendInvalidSetCannotBeDifferentClassTypes()
    {
        try
        {
            Loader.parseM3(
                    "import other::*;\n" +
                            "\n" +
                            "Class other::Person\n" +
                            "{\n" +
                            "    name:String[1];\n" +
                            "    otherInfo:String[1];\n" +
                            "}\n" +
                            "Class other::Firm\n" +
                            "{\n" +
                            "    legalName:String[1];\n" +
                            "    otherInformation:String[1];\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database mapping::db(\n" +
                            "   Table employeeTable\n" +
                            "   (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(200),\n" +
                            "    firm VARCHAR(200),\n" +
                            "    otherInfo VARCHAR(200),\n" +
                            "    postcode VARCHAR(10)\n" +
                            "   )\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::myMapping\n" +
                            "(\n" +
                            "    Firm[f1]: Relational\n" +
                            "    {\n" +
                            "       legalName : [db]employeeTable.firm \n" +
                            "    }\n" +
                            "    Person[p_subclass] extends [f1]: Relational\n" +
                            "    {\n" +
                            "       otherInfo: [db]employeeTable.otherInfo\n" +
                            "    }\n" +
                            ")\n", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            this.runtime.compile();
            Assert.fail("this should not compile");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class [Person] != [Firm], when [p_subclass] extends [ 'f1'] they must map the same class", "fromString.pure", 33, 5, e);
        }

    }
}