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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;


public class TestMappingGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    RelationalGraphWalker graphWalker;

    @Before
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(this.runtime, this.processorSupport);
    }


    @Test
    public void testColumnWithNoTable()
    {
        compileTestSource("model.pure",
                "Class Firm\n" +
                        "{\n" +
                        "  legalName : String[1];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database FirmDb\n" +
                        "(\n" +
                        "  Table FirmTable (legal_name VARCHAR(200))\n" +
                        ")");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping FirmMapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "         {\n" +
                            "            legalName : legal_name\n" +
                            "         }\n" +
                            ")");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing table or alias for column: legal_name", "mapping.pure", 6, 25, 6, 25, 6, 34, e);
        }
    }

    @Test
    public void testCombinationOfDistinctWithEmbeddedPropertyMappings()
    {
        compileTestSource("model.pure",
                "Class Firm\n" +
                        "{\n" +
                        "  legalName : String[1];\n" +
                        "  details : FirmDetails[0..1];\n"+
                        "}\n"+
                        "Class FirmDetails\n" +
                        "{\n" +
                        "  taxLocation : String[1];\n" +
                        "  extraDetails : FirmExtraDetails[1];\n" +
                        "}"+
                        "Class FirmExtraDetails\n" +
                        "{\n" +
                        "  employeeCount : Integer[1];\n" +
                        "  taxLocation : String[1];\n" +
                        "}");

        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database FirmDb\n" +
                        "(\n" +
                        "  Table FirmTable (legal_name VARCHAR(200) PRIMARY KEY, tax_location VARCHAR(100), employee_count INTEGER)\n" +
                        ")");

        compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping FirmMapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "         {\n" +
                            "            ~distinct\n"+
                            "            scope([FirmDb]FirmTable)\n"+
                            "            (\n"+
                            "               legalName : legal_name,\n" +
                            "               details(taxLocation : tax_location, extraDetails(employeeCount : employee_count, taxLocation : tax_location))\n"+
                            "            )\n"+
                            "         }\n" +
                            ")");
        CoreInstance mapping = this.graphWalker.getMapping("FirmMapping");
        CoreInstance firmSetImpl = mapping.getValueForMetaPropertyToMany(M2MappingProperties.classMappings).getFirst();
        ListIterable<? extends CoreInstance> primaryKeys = firmSetImpl.getValueForMetaPropertyToMany(M2RelationalProperties.primaryKey);
        Assert.assertEquals(3, primaryKeys.size());
        Assert.assertFalse(primaryKeys.contains(null));
    }

    @Test
    public void testMappingWithIncludeError() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "  name:String[1];\n" +
                        "  firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "  name:String[1];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database subDb\n" +
                        "(\n" +
                        "  Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                        "  Table firmTb(name VARCHAR(200))\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "  include subDb\n" +
                        ")");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping myMap\n" +
                            "(\n" +
                            "    Firm[m1]: Relational\n" +
                            "              {\n" +
                            "                 name : [db]firmTb.name\n" +
                            "              }\n" +
                            "    Firm[m2]: Relational\n" +
                            "              {\n" +
                            "                 name : [db]firmTb.name\n" +
                            "              }\n" +
                            ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The class 'Firm' is mapped by 2 set implementations and has 0 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "mapping.pure", 2, 9, e);
        }
    }


    @Test
    public void testMappingWithIncludeErrorTooMany() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "  name:String[1];\n" +
                        "  firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "  name:String[1];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database subDb\n" +
                        "(\n" +
                        "  Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                        "  Table firmTb(name VARCHAR(200))\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "  include subDb\n" +
                        ")");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping myMap\n" +
                            "(\n" +
                            "    *Firm[m1]: Relational\n" +
                            "               {\n" +
                            "                  name : [db]firmTb.name\n" +
                            "               }\n" +
                            "    *Firm[m2]: Relational\n" +
                            "               {\n" +
                            "                  name : [db]firmTb.name\n" +
                            "               }\n" +
                            ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The class 'Firm' is mapped by 2 set implementations and has 2 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "mapping.pure", 2, 9, e);
        }
    }

    @Test
    public void testMappingDataTypeShouldNotMapToJoinError() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                "{" +
                "   bla : String[1];\n" +
                "   name : String[1];\n" +
                "}\n");
        compileTestSource("store.pure",
                "###Relational\n" +
                "Database db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200),firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                ")");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                    "Mapping myMap\n" +
                    "(\n" +
                    "    Person: Relational\n" +
                    "            {\n" +
                    "                bla : [db]personTb.name," +
                    "                name : [db]@myJoin\n" +
                    "            }\n" +
                    ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: The property 'name' returns a data type. However it's mapped to a Join.", "mapping.pure", 4, 5, e);
        }
    }

    @Test
    public void testMappingNonDataTypeShouldNotMapToColumn() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   other:Other[1];\n" +
                "}\n" +
                "Class Other\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}");

        compileTestSource("store.pure",
                "###Relational\n" +
                "Database db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200),firm VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                ")\n");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                    "Mapping myMap\n" +
                    "(\n" +
                    "    Person: Relational\n" +
                    "            {\n" +
                    "                other:[db]@myJoin|otherTb.name,\n" +
                    "                name:[db]personTb.name\n" +
                    "            }\n" +
                    ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: The property 'other' doesn't return a data type. However it's mapped to a column or a function.", "mapping.pure", 4, 5, e);
        }
    }

    @Test
    public void testMappingJoinError() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "   firm:Firm[1];\n" +
                        "   other:Other[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Class Other\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200),firm VARCHAR(200))\n" +
                        "   Table firmTb(name VARCHAR(200))\n" +
                        "   Table otherTb(name VARCHAR(200))\n" +
                        "   Join myJoin(personTb.firm = otherTb.name)\n" +
                        ")\n");

//        compileTestSource("mapping.pure",
//                "###Mapping\n" +
//                        "Mapping myMap(" +
//                        "    Firm[targetId]: Relational" +
//                        "          {" +
//                        "             name : [db]firmTb.name" +
//                        "          }" +
//                        "    Other: Relational" +
//                        "          {" +
//                        "             name : [db]otherTb.name" +
//                        "          }" +
//                        "    Person: Relational" +
//                        "            {" +
//                        "                firm[targetId]:[db]@myJoin," +
//                        "                other:[db]@myJoin," +
//                        "                name:[db]personTb.name" +
//                        "            }" +
//                        ")\n" +
//                "###Pure\n" +
//                "function test():Boolean[1]" +
//                "{" +
//                "   assertEquals('targetId', myMap.classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id);" +
//                "}");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping myMap\n" +
                            "(\n" +
                            "    Firm[targetId]: Relational\n" +
                            "          {\n" +
                            "             name : [db]firmTb.name\n" +
                            "          }\n" +
                            "    Other: Relational\n" +
                            "          {\n" +
                            "             name : [db]otherTb.name\n" +
                            "          }\n" +
                            "    Person: Relational\n" +
                            "            {\n" +
                            "                firm[targetId]:[db]@myJoin,\n" +
                            "                other:[db]@myJoin,\n" +
                            "                name:[db]personTb.name\n" +
                            "            }\n" +
                            ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@myJoin does not connect from the source table [db]personTb to the target table [db]firmTb; instead it connects to [db]otherTb", "mapping.pure", 14, 37, e);
        }
    }

    @Test
    public void testMappingMultipleJoins() throws Exception
    {
        compileTestSource("mapping.pure",
                "Class Person{name:String[1];firm:Firm[1];other:Other[1];}" +
                        "Class Firm{name:String[1];}" +
                        "Class Other{name:String[1];}\n" +
                        "###Relational\n" +
                        "Database db(Table personTb(name VARCHAR(200),firm VARCHAR(200))" +
                        "            Table firmTb(name VARCHAR(200))" +
                        "            Table otherTb(name VARCHAR(200))" +
                        "            Join myJoin(personTb.firm = otherTb.name)\n" +
                        "            Join otherJoin(otherTb.name = firmTb.name))\n" +
                        "###Mapping\n" +
                        "Mapping myMap(" +
                        "    Firm: Relational\n" +
                        "          {" +
                        "             name : [db]firmTb.name\n" +
                        "          }\n" +
                        "    Person: Relational" +
                        "            {" +
                        "                firm:[db]@myJoin > @otherJoin,\n" +
                        "                name:[db]personTb.name\n" +
                        "            }\n" +
                        ")\n");
        // TODO add asserts
        CoreInstance mapping = this.graphWalker.getMapping("myMap");
        Assert.assertNotNull(mapping);
        Assert.assertNotNull(mapping.getSourceInformation());
        Assert.assertEquals(6,mapping.getSourceInformation().getStartLine());
        Assert.assertEquals(12,mapping.getSourceInformation().getEndLine());
        CoreInstance classMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assert.assertNotNull(classMapping);
        Assert.assertNotNull(classMapping.getSourceInformation());
        Assert.assertEquals(6,classMapping.getSourceInformation().getStartLine());
        Assert.assertEquals(8,classMapping.getSourceInformation().getEndLine());
    }

    @Test
    public void testMappingMultipleJoinsError() throws Exception
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "   firm:Firm[1];\n" +
                        "   other:Other[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Class Other\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                        "   Table firmTb(name VARCHAR(200))\n" +
                        "   Table otherTb(name VARCHAR(200))\n" +
                        "   Table otherTb2(name VARCHAR(200))\n" +
                        "   Join myJoin(personTb.firm = otherTb.name)\n" +
                        "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping myMap\n" +
                            "(\n" +
                            "    Firm: Relational\n" +
                            "          {\n" +
                            "             name : [db]firmTb.name\n" +
                            "          }\n" +
                            "    Person: Relational\n" +
                            "            {\n" +
                            "                firm:[db]@myJoin > @otherJoin,\n" +
                            "                name:[db]personTb.name\n" +
                            "            }\n" +
                            ")\n");
            Assert.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@otherJoin does not contain the source table [db]otherTb", "mapping.pure", 10, 37, e);
        }
    }

    @Test
    public void testErrorInCrossMapping()
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Association aa\n" +
                        "{\n" +
                        "   firm:Firm[1];\n" +
                        "   employees:Person[*];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200),firmId INT)\n" +
                        "   Table firmTb(id INT, name VARCHAR(200))\n" +
                        "   Table otherTb(id INT, name VARCHAR(200))\n" +
                        "   Join myJoin(personTb.firmId = otherTb.id)\n" +
                        ")");
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "Mapping myMap1\n" +
                            "(\n" +
                            "    Firm: Relational\n" +
                            "          {\n" +
                            "             name : [db]firmTb.name,\n" +
                            "             employees : [db]@myJoin\n" +
                            "          }\n" +
                            ")\n" +
                            "Mapping myMap2\n" +
                            "(\n" +
                            "    Person: Relational\n" +
                            "            {\n" +
                            "                firm:[db]@myJoin,\n" +
                            "                name:[db]personTb.name\n" +
                            "            }\n" +
                            ")\n" +
                            "###Mapping\n" +
                            "Mapping myMap3(" +
                            "  include myMap1" +
                            "  include myMap2" +
                            ")");
            Assert.fail("Expected compile exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@myJoin does not contain the source table [db]firmTb", "mapping.pure", 7, 31, e);
        }
    }

    @Test
    public void testGoodCrossMapping()
    {
        compileTestSource("model.pure",
                "Class Person\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}\n" +
                        "Association aa\n" +
                        "{\n" +
                        "   firm:Firm[1];" +
                        "   employees:Person[*];\n" +
                        "}");
        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200),firmId INT)\n" +
                        "   Table firmTb(id INT, name VARCHAR(200))\n" +
                        "   Join myJoin(personTb.firmId = firmTb.id)\n" +
                        ")");
        compileTestSource("mapping.pure",
                "###Mapping\n" +
                        "Mapping myMap1\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "          {\n" +
                        "             name : [db]firmTb.name,\n" +
                        "             employees : [db]@myJoin\n" +
                        "          }\n" +
                        ")\n" +
                        "Mapping myMap2\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "            {\n" +
                        "                firm:[db]@myJoin,\n" +
                        "                name:[db]personTb.name\n" +
                        "            }\n" +
                        ")\n" +
                        "Mapping myMap3\n" +
                        "(\n" +
                        "  include myMap1\n" +
                        "  include myMap2\n" +
                        ")\n");

        CoreInstance myMap1 = this.runtime.getCoreInstance("myMap1");
        Assert.assertNotNull(myMap1);

        CoreInstance myMap2 = this.runtime.getCoreInstance("myMap2");
        Assert.assertNotNull(myMap2);

        CoreInstance myMap3 = this.runtime.getCoreInstance("myMap3");
        Assert.assertNotNull(myMap3);

        ListIterable<? extends CoreInstance> myMap3Includes = Instance.getValueForMetaPropertyToManyResolved(myMap3, M3Properties.includes, this.processorSupport);
        Verify.assertSize(2, myMap3Includes);
        Assert.assertSame(myMap1, Instance.getValueForMetaPropertyToOneResolved(myMap3Includes.get(0), M3Properties.included, this.processorSupport));
        Assert.assertSame(myMap2, Instance.getValueForMetaPropertyToOneResolved(myMap3Includes.get(1), M3Properties.included, this.processorSupport));
    }

    @Test
    public void testIncludeSelf()
    {
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "\n" +
                            "Mapping test::A\n" +
                            "(\n" +
                            "  include A\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Circular include in mapping test::A: test::A -> test::A", "mapping.pure", 4, 15, e);
        }
    }

    @Test
    public void testIncludeLoop()
    {
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "\n" +
                            "Mapping test::A\n" +
                            "(\n" +
                            "  include C\n" +
                            ")\n" +
                            "Mapping test::B\n" +
                            "(\n" +
                            "  include A\n" +
                            ")\n" +
                            "Mapping test::C\n" +
                            "(\n" +
                            "  include B\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, Pattern.compile("Circular include in mapping ((test::A: test::A -> test::C -> test::B -> test::A)|(test::B: test::B -> test::A -> test::C -> test::B)|(test::C: test::C -> test::B -> test::A -> test::C))"), "mapping.pure", e);
        }
    }

    @Test
    public void testDoubleInclude()
    {
        try
        {
            compileTestSource("mapping.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "\n" +
                            "Mapping test::A\n" +
                            "(\n" +
                            ")\n" +
                            "Mapping test::B\n" +
                            "(\n" +
                            "  include A\n" +
                            "  include A\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping test::A is included multiple times in test::B", "mapping.pure", 7, 15, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitution()
    {
        compileTestSource("model.pure",
                "###Pure\n" +
                        "\n" +
                        "Class class1\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class class2\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n"
        );

        compileTestSource("store.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db2\n" +
                        "(\n" +
                        "  Table T2 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db3\n" +
                        "(\n" +
                        "  include test::db1\n" +
                        "  include test::db2\n" +
                        ")\n");

        CoreInstance db1 = this.runtime.getCoreInstance("test::db1");
        Assert.assertNotNull(db1);

        CoreInstance db2 = this.runtime.getCoreInstance("test::db2");
        Assert.assertNotNull(db2);

        CoreInstance db3 = this.runtime.getCoreInstance("test::db3");
        Assert.assertNotNull(db3);

        compileTestSource("mapping.pure",
                "###Mapping\n" +
                        "import test::*;\n" +
                        "\n" +
                        "Mapping test::mapping12\n" +
                        "(\n" +
                        "   class1 : Relational\n" +
                        "   {\n" +
                        "      scope([db1]T1)\n" +
                        "      (\n" +
                        "         id : id\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   class2 : Relational\n" +
                        "   {\n" +
                        "      scope([db2]T2)\n" +
                        "      (\n" +
                        "         id : id\n" +
                        "      )\n" +
                        "   }\n" +
                        ")\n" +
                        "\n" +
                        "Mapping test::mapping3\n" +
                        "(\n" +
                        "   include mapping12[db1 -> db3, db2 -> db3]\n" +
                        ")\n");

        CoreInstance mapping12 = this.runtime.getCoreInstance("test::mapping12");
        Assert.assertNotNull(mapping12);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(mapping12, M3Properties.includes, this.processorSupport));

        CoreInstance mapping3 = this.runtime.getCoreInstance("test::mapping3");
        Assert.assertNotNull(mapping3);

        ListIterable<? extends CoreInstance> includes = Instance.getValueForMetaPropertyToManyResolved(mapping3, M3Properties.includes, this.processorSupport);
        Verify.assertSize(1, includes);
        CoreInstance include = includes.get(0);
        Assert.assertSame(mapping12, Instance.getValueForMetaPropertyToOneResolved(include, M3Properties.included, this.processorSupport));

        ListIterable<? extends CoreInstance> storeSubstitutions = Instance.getValueForMetaPropertyToManyResolved(include, M2MappingProperties.storeSubstitutions, this.processorSupport);
        Verify.assertSize(2, storeSubstitutions);
        CoreInstance storeSub1 = storeSubstitutions.get(0);
        Assert.assertSame(db1, Instance.getValueForMetaPropertyToOneResolved(storeSub1, M2MappingProperties.original, this.processorSupport));
        Assert.assertSame(db3, Instance.getValueForMetaPropertyToOneResolved(storeSub1, M2MappingProperties.substitute, this.processorSupport));

        CoreInstance storeSub2 = storeSubstitutions.get(1);
        Assert.assertSame(db2, Instance.getValueForMetaPropertyToOneResolved(storeSub2, M2MappingProperties.original, this.processorSupport));
        Assert.assertSame(db3, Instance.getValueForMetaPropertyToOneResolved(storeSub2, M2MappingProperties.substitute, this.processorSupport));
    }

    @Test
    public void testIncludeWithStoreSubstitutionToNonStore()
    {
        compileTestSource("stores.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class test::db2\n" +
                        "{\n" +
                        "}");
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::mapping1\n" +
                        "(\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::mapping2\n" +
                            "(\n" +
                            "   include mapping1[db1 -> db2]\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 is not a Store", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionFromNonStore()
    {
        compileTestSource("stores.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class test::db2\n" +
                        "{\n" +
                        "}");
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::mapping1\n" +
                        "(\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::mapping2\n" +
                            "(\n" +
                            "   include mapping1[db2 -> db1]\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 is not a Store", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionWithoutStoreInclude()
    {
        compileTestSource("stores.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db2\n" +
                        "(\n" +
                        "  Table T2 (id INT)\n" +
                        ")");
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::mapping1\n" +
                        "(\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::mapping2\n" +
                            "(\n" +
                            "   include mapping1[db1 -> db2]\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 does not include test::db1", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithMultipleSubstitutionsForOneStore()
    {
        compileTestSource("stores.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db2\n" +
                        "(\n" +
                        "  include test::db1\n" +
                        "  Table T2 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db3\n" +
                        "(\n" +
                        "  include test::db1\n" +
                        "  Table T3 (id INT)\n" +
                        ")");
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::mapping1\n" +
                        "(\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::mapping2\n" +
                            "(\n" +
                            "   include mapping1[db1 -> db2, db1 -> db3]\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: multiple substitutions for test::db1", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionsForSubstitutedStore()
    {
        compileTestSource("stores.pure",
                "###Relational\n" +
                        "Database test::db1\n" +
                        "(\n" +
                        "  Table T1 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db2\n" +
                        "(\n" +
                        "  include test::db1\n" +
                        "  Table T2 (id INT)\n" +
                        ")\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database test::db3\n" +
                        "(\n" +
                        "  include test::db2\n" +
                        "  Table T3 (id INT)\n" +
                        ")");
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::mapping1\n" +
                        "(\n" +
                        ")\n");
        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::mapping2\n" +
                            "(\n" +
                            "   include mapping1[db1 -> db2, db2 -> db3]\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 appears both as an original and a substitute", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testFiltersAreAvailableFromIncludedStore()
    {
        compileTestSource("stores.pure","###Relational\n" +
                                        "Database test::db1\n" +
                                        "(\n" +
                                        "  Table T1 (id INT)\n" +
                                        "  Filter idFilter(T1.id = 0)" +
                                        ")\n" +
                                        "###Relational\n" +
                                        "Database test::db2\n" +
                                        "(\n" +
                                        "  include test::db1\n" +
                                        "  Table T2 (id INT)\n" +
                                        "  Filter idFilter(T1.id != 0)" +
                                        ")\n" +
                                        "###Relational\n" +
                                        "Database test::db3\n" +
                                        "(\n" +
                                        "  include test::db2\n" +
                                        ")");
        compileTestSource("domain.pure", "###Pure\n" +
                                         "Class T1{id:Integer[1];}");
        compileTestSource("mapping1.pure","###Mapping\n"+
                                          "import test::*;\n"+
                                          "Mapping test::mapping\n" +
                                          "(\n" +
                                          " T1[db3]: Relational{" +
                                          " ~filter [db3]idFilter" +
                                          "~mainTable[db3]T1"+
                                          " id : [db3]T1.id"+
                                          "}" +
                                          ")\n");
        CoreInstance testMapping = this.runtime.getCoreInstance("test::mapping");
        CoreInstance classMapping = Instance.getValueForMetaPropertyToOneResolved(testMapping, M2MappingProperties.classMappings, this.processorSupport);
        CoreInstance mainTableAlias = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.mainTableAlias, this.processorSupport);
        CoreInstance filterMapping = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.filter, this.processorSupport);
        CoreInstance op = Instance.getValueForMetaPropertyToOneResolved(filterMapping, M2RelationalProperties.filter, M2RelationalProperties.operation, this.processorSupport);
        Assert.assertTrue(Instance.instanceOf(op, "meta::relational::metamodel::DynaFunction", this.processorSupport));



    }


    @Test
        public void testDefaultMainTableForIncludedStore()
        {
            compileTestSource("stores.pure","###Relational\n" +
                                            "Database test::db1\n" +
                                            "(\n" +
                                            "  Table T1 (id INT)\n" +
                                            "  Filter idFilter(T1.id = 0)" +
                                            ")\n" +
                                            "###Relational\n" +
                                            "Database test::db2\n" +
                                            "(\n" +
                                            "  include test::db1\n" +
                                            "  Table T2 (id INT)\n" +
                                            "  Filter idFilter(T1.id != 0)" +
                                            ")\n" +
                                            "###Relational\n" +
                                            "Database test::db3\n" +
                                            "(\n" +
                                            "  include test::db2\n" +
                                            ")");
            compileTestSource("domain.pure", "###Pure\n" +
                                             "Class T1{id:Integer[1];}");
            compileTestSource("mapping1.pure","###Mapping\n"+
                                              "import test::*;\n"+
                                              "Mapping test::mapping\n" +
                                              "(\n" +
                                              " T1[db3]: Relational{" +
                                              " id : [db3]T1.id"+
                                              "}" +
                                              ")\n");
            CoreInstance testMapping = this.runtime.getCoreInstance("test::mapping");
            CoreInstance classMapping = Instance.getValueForMetaPropertyToOneResolved(testMapping, M2MappingProperties.classMappings, this.processorSupport);
            CoreInstance mainTableAlias = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.mainTableAlias, this.processorSupport);

            CoreInstance db3 = this.runtime.getCoreInstance("test::db3");
            CoreInstance database = Instance.getValueForMetaPropertyToOneResolved(mainTableAlias, M2RelationalProperties.database, this.processorSupport);

            Assert.assertEquals(db3, database);


        }

}
