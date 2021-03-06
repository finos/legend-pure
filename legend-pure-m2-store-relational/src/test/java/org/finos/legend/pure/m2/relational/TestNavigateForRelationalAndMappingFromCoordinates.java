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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.FilterInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestNavigateForRelationalAndMappingFromCoordinates extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testNavigateForFilter() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "filterMappingSample.pure",
                "Class a::b::Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::b::db\n" +
                        "(\n" +
                        "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT)\n" +
                        "    Filter GoldmanSachsFilter(firmTable.LEGALNAME = 'Goldman Sachs')\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping a::b::simpleRelationalMappingWithFilter\n" +
                        "(\n" +
                        "   a::b::Firm : Relational\n" +
                        "          {\n" +
                        "             ~filter [a::b::db] GoldmanSachsFilter\n" +
                        "             legalName : [a::b::db]firmTable.LEGALNAME\n" +
                        "          }\n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(18, 33, this.processorSupport);
        Assert.assertTrue(found instanceof Filter);
        Assert.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assert.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(10, found.getSourceInformation().getLine());
        Assert.assertEquals(12, found.getSourceInformation().getColumn());

        found = source.navigate(18, 40, this.processorSupport);
        Assert.assertTrue(found instanceof Filter);
        Assert.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assert.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(10, found.getSourceInformation().getLine());
        Assert.assertEquals(12, found.getSourceInformation().getColumn());

        found = source.navigate(18, 50, this.processorSupport);
        Assert.assertTrue(found instanceof Filter);
        Assert.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assert.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(10, found.getSourceInformation().getLine());
        Assert.assertEquals(12, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForJoin() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "joinSample.pure",
                "Class a::b::Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "   address  : a::b::Address[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::b::db\n" +
                        "(\n" +
                        "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT, FLAG INT)\n" +
                        "    Table addressTable(ID INT PRIMARY KEY, TYPE INT, NAME VARCHAR(200), STREET VARCHAR(100), COMMENTS VARCHAR(100))\n" +
                        "    Join Address_Firm(addressTable.ID = firmTable.ADDRESSID)        \n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping a::b::chainedJoinsInner\n" +
                        "(\n" +
                        "   a::b::Firm : Relational\n" +
                        "          {\n" +
                        "             legalName : [a::b::db]firmTable.LEGALNAME,\n" +
                        "             address(\n" +
                        "                name : [a::b::db] case(equal(@Address_Firm |addressTable.ID, 1), 'UK', 'Europe') \n" +
                        "             )\n" +
                        "          }\n" +
                        "   \n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(27, 47, this.processorSupport);
        Assert.assertTrue(found instanceof Join);
        Assert.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assert.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(17, found.getSourceInformation().getLine());
        Assert.assertEquals(10, found.getSourceInformation().getColumn());

        found = source.navigate(27, 50, this.processorSupport);
        Assert.assertTrue(found instanceof Join);
        Assert.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assert.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(17, found.getSourceInformation().getLine());
        Assert.assertEquals(10, found.getSourceInformation().getColumn());

        found = source.navigate(27, 58, this.processorSupport);
        Assert.assertTrue(found instanceof Join);
        Assert.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assert.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(17, found.getSourceInformation().getLine());
        Assert.assertEquals(10, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForAssociationMapping() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "associationMappingSample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "   firmId : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::Firm\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association a::Person_Firm\n" +
                        "{\n" +
                        "   person : Person[1];\n" +
                        "   firm : Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonFirmDatabase\n" +
                        "(\n" +
                        "   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)\n" +
                        "   Table firm(ID INT, NAME VARCHAR(200))\n" +
                        "   Join person_firm(person.FIRM_ID = firm.ID)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonFirmMappin\n" +
                        "(\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonFirmDatabase]person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name : NAME,\n" +
                        "         firmId : FIRM_ID\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      scope([PersonFirmDatabase]firm)\n" +
                        "      (\n" +
                        "        id : ID,\n" +
                        "        name : NAME\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person_Firm : Relational\n" +
                        "   {\n" +
                        "      AssociationMapping\n" +
                        "      (\n" +
                        "         firm[personAlias,a_Firm] : [PersonFirmDatabase]@person_firm,\n" +
                        "         person[a_Firm,personAlias] : [PersonFirmDatabase]@person_firm\n" +
                        "      )\n" +
                        "   }\n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(59, 14, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(59, 15, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 20, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 25, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 26, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(59, 27, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 30, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 32, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 33, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(59, 34, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(60, 16, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(60, 17, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 20, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 22, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(46, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 23, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(60, 24, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 30, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 34, this.processorSupport);
        Assert.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(36, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 35, this.processorSupport);
        Assert.assertNull(found);

        found = source.navigate(60, 36, this.processorSupport);
        Assert.assertNull(found);
    }

    @Test
    public void testNavigateForProperty() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "propertySample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "   firmId : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::Firm\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association a::Person_Firm\n" +
                        "{\n" +
                        "   person : Person[1];\n" +
                        "   firm : Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonFirmDatabase\n" +
                        "(\n" +
                        "   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)\n" +
                        "   Table firm(ID INT, NAME VARCHAR(200))\n" +
                        "   Join person_firm(person.FIRM_ID = firm.ID)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonFirmMappin\n" +
                        "(\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonFirmDatabase]person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name : NAME,\n" +
                        "         firmId : FIRM_ID\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      scope([PersonFirmDatabase]firm)\n" +
                        "      (\n" +
                        "        id : ID,\n" +
                        "        name : NAME\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person_Firm : Relational\n" +
                        "   {\n" +
                        "      AssociationMapping\n" +
                        "      (\n" +
                        "         firm[personAlias,a_Firm] : [PersonFirmDatabase]@person_firm,\n" +
                        "         person[a_Firm,personAlias] : [PersonFirmDatabase]@person_firm\n" +
                        "      )\n" +
                        "   }\n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(59, 10, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("firm", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 11, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("firm", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 13, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("firm", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 10, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("person", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(19, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 12, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("person", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(19, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 15, this.processorSupport);
        Assert.assertTrue(found instanceof PropertyInstance);
        Assert.assertEquals("person", ((PropertyInstance)found)._name());
        Assert.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(19, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForView() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "viewSample.pure",
                "###Pure\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "   firmId : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::Firm\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonFirmDatabase\n" +
                        "(\n" +
                        "   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)\n" +
                        "   Table firm(ID INT, NAME VARCHAR(200))\n" +
                        "   View person_firm_view(personId : person.ID, personName : person.NAME, firmId : person.FIRM_ID)\n" +
                        "   Join person_firm(person_firm_view.firmId = firm.ID)\n" +
                        ")\n"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(21, 21, this.processorSupport);
        Assert.assertTrue(found instanceof View);
        Assert.assertEquals("person_firm_view", ((View)found)._name());
        Assert.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(9, found.getSourceInformation().getColumn());

        found = source.navigate(21, 30, this.processorSupport);
        Assert.assertTrue(found instanceof View);
        Assert.assertEquals("person_firm_view", ((View)found)._name());
        Assert.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(9, found.getSourceInformation().getColumn());

        found = source.navigate(21, 36, this.processorSupport);
        Assert.assertTrue(found instanceof View);
        Assert.assertEquals("person_firm_view", ((View)found)._name());
        Assert.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(20, found.getSourceInformation().getLine());
        Assert.assertEquals(9, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForEmbeddedRelationalInstanceSetImplementation() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "embeddedRelationalInstanceSetImplementationSample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "Class a::Name\n" +
                        "{\n" +
                        "   first : String[1];\n" +
                        "   last : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : Name[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonDatabase\n" +
                        "(\n" +
                        "   Table person (ID INT, FIRST_NAME VARCHAR(200), LAST_NAME VARCHAR(200))\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonMapping\n" +
                        "(\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonDatabase]person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name\n" +
                        "         (last: LAST_NAME,first: FIRST_NAME)\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(31, 10, this.processorSupport);
        Assert.assertTrue(found instanceof Property);
        Assert.assertEquals("name", ((Property)found)._name());
        Assert.assertEquals("embeddedRelationalInstanceSetImplementationSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(12, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForTableAliasWithSchema() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "tableAliasSample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonDatabase\n" +
                        "(\n" +
                        "   Schema personSchema (\n" +
                        "       Table person (ID INT, NAME VARCHAR(200))\n" +
                        "   )\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonMapping\n" +
                        "(\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonDatabase]personSchema.person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name : NAME\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(25, 42, this.processorSupport);
        Assert.assertTrue(found instanceof Table);
        Assert.assertEquals("person", ((Table)found)._name());
        Assert.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(14, found.getSourceInformation().getLine());
        Assert.assertEquals(14, found.getSourceInformation().getColumn());

        found = source.navigate(25, 29, this.processorSupport);
        Assert.assertTrue(found instanceof Schema);
        Assert.assertEquals("personSchema", ((Schema)found)._name());
        Assert.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(13, found.getSourceInformation().getLine());
        Assert.assertEquals(11, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForTableAliasWithoutSchema() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "tableAliasSample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonDatabase\n" +
                        "(\n" +
                        "   Table person (ID INT, NAME VARCHAR(200))\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonMapping\n" +
                        "(\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonDatabase]person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name : NAME\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(23, 29, this.processorSupport);
        Assert.assertTrue(found instanceof Table);
        Assert.assertEquals("person", ((Table)found)._name());
        Assert.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(13, found.getSourceInformation().getLine());
        Assert.assertEquals(10, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForEnumerationMapping() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "enumerationMappingSample.pure",
                "###Pure\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Enum a::Gender\n" +
                        "{\n" +
                        "    FEMALE,\n" +
                        "    \n" +
                        "    MALE\n" +
                        "}\n" +
                        "\n" +
                        "Class a::Person\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   name : String[1];\n" +
                        "   gender : Gender[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database a::PersonDatabase\n" +
                        "(\n" +
                        "   Schema personSchema (\n" +
                        "       Table person (ID INT, NAME VARCHAR(200), GENDER CHAR(1))\n" +
                        "   )\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import a::*;\n" +
                        "\n" +
                        "Mapping a::PersonMapping\n" +
                        "(\n" +
                        "   Gender: EnumerationMapping GenderMapping\n" +
                        "   {\n" +
                        "        FEMALE:  'F',\n" +
                        "        MALE:    'M' \n" +
                        "   }\n" +
                        "\n" +
                        "   Person[personAlias] : Relational\n" +
                        "   {\n" +
                        "      scope([PersonDatabase]personSchema.person)\n" +
                        "      (\n" +
                        "         id : ID,\n" +
                        "         name : NAME,\n" +
                        "         gender : EnumerationMapping GenderMapping: GENDER\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        ")"
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(43, 39, this.processorSupport);
        Assert.assertTrue(found instanceof EnumerationMapping);
        Assert.assertEquals("GenderMapping", ((EnumerationMapping)found)._name());
        Assert.assertEquals("enumerationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assert.assertEquals(31, found.getSourceInformation().getLine());
        Assert.assertEquals(4, found.getSourceInformation().getColumn());

    }
}
