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


import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.OperationParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.CompiledStateIntegrityTestTools;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.Loader.LoaderException;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class TestEmbeddedGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    private RelationalGraphWalker graphWalker;

    @Before
    @Override
    public void _setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(GenericCodeRepository.build("test", "((test)|(meta))(::.*)?", PlatformCodeRepository.NAME));
        return repositories;
    }

    @Test
    public void duplicatePropertyMappingInEmbeddedMappingCausesError()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName,\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertOriginatingPureException("Duplicate mappings found for the property 'legalName' (targetId: ?) in the embedded mapping for 'firm' in the mapping for class Person, the property should have one mapping.", 35, 13, e);
    }

    @Test
    public void duplicateSetImplementationIdWithInEmbeddedMappingCausesError()
    {
        compileTestSource("/test/testModel.pure",
                "import test::other::*;\n" +
                        "\n" +
                        "Class test::other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class test::other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n");
        compileTestSource("/test/testStore.pure",
                "###Relational\n" +
                        "Database test::mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testMapping.pure",
                "###Mapping\n" +
                        "import test::other::*;\n" +
                        "import test::mapping::*;\n" +
                        "Mapping test::mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[k]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm[k]\n" +
                        "        (\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'k' in mapping test::mappingPackage::myMapping", "/test/testMapping.pure", 6, 5, e);
    }

    @Test
    public void duplicateSetImplementationIdBetweenEmbeddedMappingsCausesError()
    {
        compileTestSource("/test/testModel.pure",
                "import test::other::*;\n" +
                        "\n" +
                        "Class test::other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class test::other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n");
        compileTestSource("/test/testStore.pure",
                "###Relational\n" +
                        "Database test::mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Table firmEmployeeDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    employeeId INT,\n" +
                        "    name VARCHAR(200)\n" +
                        "   )\n" +
                        ")\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testMapping.pure",
                "###Mapping\n" +
                        "import test::other::*;\n" +
                        "import test::mapping::*;\n" +
                        "Mapping test::mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm[k]\n" +
                        "        (\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "        legalName : [db]firmEmployeeDenormTable.legalName,\n" +
                        "        employees[k]\n" +
                        "        (\n" +
                        "            name : [db]firmEmployeeDenormTable.name\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'k' in mapping test::mappingPackage::myMapping", "/test/testMapping.pure", 17, 9, e);
    }

    @Test
    public void duplicateSetImplementationIdWithInEmbeddedMappingMultiple()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[k]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    " +
                        "* Person[kk]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void testEmbeddedMapping()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "    address:Address[0..1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "    address:Address[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address VARCHAR(200)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName,\n" +
                        "            address\n" +
                        "            (\n" +
                        "                line1: [db]employeeFirmDenormTable.address\n" +
                        "            )\n" +
                        "        ),\n" +
                        "        address\n" +
                        "        (\n" +
                        "            line1: [db]employeeFirmDenormTable.address\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(4, this.graphWalker.getClassMappings(mapping).size());
        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);

        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(3, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance namePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "name");
        Assert.assertNotNull(namePropMapping);
        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);

        CoreInstance firmProp = firmPropMapping.getValueForMetaPropertyToOne(M3Properties.property);
        ListIterable<? extends CoreInstance> refUsages = firmProp.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertEquals(1, refUsages.size());
        Assert.assertEquals(firmPropMapping, refUsages.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));

        CoreInstance owner = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, owner);
        Assert.assertEquals(personMapping, firmPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));

        ListIterable<? extends CoreInstance> pks = firmPropMapping.getValueForMetaPropertyToMany(M2RelationalProperties.primaryKey);
        CoreInstance pk = pks.getFirst();
        Assert.assertEquals(personMapping, pk.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner));

        CoreInstance legalNamePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmPropMapping, "legalName");
        CoreInstance nameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(legalNamePropMapping);
        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getTableAliasColumnAliasName(nameColumnAlias));
        Assert.assertEquals("legalName", this.graphWalker.getTableAliasColumnColumnName(nameColumnAlias));

        CoreInstance firmAddressPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmPropMapping, "address");
        Assert.assertEquals("other_Person_firm_address", firmAddressPropMapping.getValueForMetaPropertyToOne(M3Properties.id).getName());
        CoreInstance ownerFirmAddress = firmAddressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, ownerFirmAddress);
        Assert.assertEquals(firmPropMapping, firmAddressPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));

        CoreInstance addressLine1PropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmAddressPropMapping, "line1");
        CoreInstance addressLine1ColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(addressLine1PropMapping);
        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getTableAliasColumnAliasName(addressLine1ColumnAlias));
        Assert.assertEquals("address", this.graphWalker.getTableAliasColumnColumnName(addressLine1ColumnAlias));

        CoreInstance addressPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "address");
        Assert.assertEquals("other_Person_address", addressPropMapping.getValueForMetaPropertyToOne(M3Properties.id).getName());
        CoreInstance ownerAddress = addressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, ownerAddress);
        Assert.assertEquals(personMapping, addressPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));
    }


    @Test
    public void testEmbeddedMappingWithIds()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "import meta::pure::mapping::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "    address:Address[0..1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "    address:Address[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "}\n" +
                        "   function meta::pure::router::operations::union(o:OperationSetImplementation[1]):SetImplementation[*]\n" +
                        "   {\n" +
                        "       $o.parameters.setImplementation;\n" +
                        "   }\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address VARCHAR(200)\n" +
                        "   )\n" +
                        "   Table employeeFirmDenormTable2\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address VARCHAR(200)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[per1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName,\n" +
                        "            address\n" +
                        "            (\n" +
                        "                line1: [db]employeeFirmDenormTable.address\n" +
                        "            )\n" +
                        "        ),\n" +
                        "        address\n" +
                        "        (\n" +
                        "            line1: [db]employeeFirmDenormTable.address\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Person[per2]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable2.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable2.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable2.legalName,\n" +
                        "            address\n" +
                        "            (\n" +
                        "                line1: [db]employeeFirmDenormTable2.address\n" +
                        "            )\n" +
                        "        ),\n" +
                        "        address\n" +
                        "        (\n" +
                        "            line1: [db]employeeFirmDenormTable2.address\n" +
                        "        )\n" +
                        "    }\n" +
                        "    *Person : Operation\n" +
                        "    {\n" +
                        "               meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_( per1, per2 )   \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser(), new OperationParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(9, this.graphWalker.getClassMappings(mapping).size());
        CoreInstance personMapping = this.graphWalker.getClassMappingById(mapping, "per1");
        validatePersonMapping(personMapping, "employeeFirmDenormTable");

        CoreInstance personMapping2 = this.graphWalker.getClassMappingById(mapping, "per2");
        validatePersonMapping(personMapping2, "employeeFirmDenormTable2");
    }

    private void validatePersonMapping(CoreInstance personMapping, String tableName)
    {
        Assert.assertNotNull(personMapping);

        Assert.assertEquals(tableName, this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(3, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance namePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "name");
        Assert.assertNotNull(namePropMapping);
        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);

        CoreInstance firmProp = firmPropMapping.getValueForMetaPropertyToOne(M3Properties.property);
        ListIterable<? extends CoreInstance> refUsages = firmProp.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertEquals(2, refUsages.size());
        Verify.assertContains(firmPropMapping, (Collection<?>) refUsages.collect(object -> object.getValueForMetaPropertyToOne(M3Properties.owner)));

        CoreInstance owner = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, owner);
        Assert.assertEquals(personMapping, firmPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));

        ListIterable<? extends CoreInstance> pks = firmPropMapping.getValueForMetaPropertyToMany(M2RelationalProperties.primaryKey);
        CoreInstance pk = pks.getFirst();
        Assert.assertEquals(personMapping, pk.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner));

        CoreInstance legalNamePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmPropMapping, "legalName");
        CoreInstance nameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(legalNamePropMapping);
        Assert.assertEquals(tableName, this.graphWalker.getTableAliasColumnAliasName(nameColumnAlias));
        Assert.assertEquals("legalName", this.graphWalker.getTableAliasColumnColumnName(nameColumnAlias));

        CoreInstance firmAddressPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmPropMapping, "address");
        CoreInstance ownerFirmAddress = firmAddressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, ownerFirmAddress);
        Assert.assertEquals(firmPropMapping, firmAddressPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));

        CoreInstance addressLine1PropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmAddressPropMapping, "line1");
        CoreInstance addressLine1ColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(addressLine1PropMapping);
        Assert.assertEquals(tableName, this.graphWalker.getTableAliasColumnAliasName(addressLine1ColumnAlias));
        Assert.assertEquals("address", this.graphWalker.getTableAliasColumnColumnName(addressLine1ColumnAlias));

        CoreInstance addressPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "address");
        CoreInstance ownerAddress = addressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, ownerAddress);
        Assert.assertEquals(personMapping, addressPropMapping.getValueForMetaPropertyToOne(M3Properties.owner));
    }

    @Test
    public void embeddedMappingsCanBeReferenced()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[alias2]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +

                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        ),\n" +
                        "        address () Inline [alias2]\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(4, this.graphWalker.getClassMappings(mapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);

        CoreInstance addressMapping = this.graphWalker.getClassMapping(mapping, "Address");
        Assert.assertNotNull(addressMapping);

        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(addressMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(addressMapping).size());

        CoreInstance addressLine1PropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(addressMapping, "line1");
        CoreInstance addressLine1ColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(addressLine1PropMapping);
        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getTableAliasColumnAliasName(addressLine1ColumnAlias));
        Assert.assertEquals("address1", this.graphWalker.getTableAliasColumnColumnName(addressLine1ColumnAlias));


        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(3, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);

        CoreInstance firmProp = firmPropMapping.getValueForMetaPropertyToOne(M3Properties.property);
        ListIterable<? extends CoreInstance> refUsages = firmProp.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertEquals(1, refUsages.size());
        Assert.assertEquals(firmPropMapping, refUsages.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertNotNull(firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.propertyMappings));

        CoreInstance owner = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, owner);

        ListIterable<? extends CoreInstance> pks = firmPropMapping.getValueForMetaPropertyToMany(M2RelationalProperties.primaryKey);
        CoreInstance pk = pks.getFirst();
        Assert.assertEquals(personMapping, pk.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner));

        CoreInstance addressPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "address");
        CoreInstance ownerAddress = addressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, ownerAddress);

        Assert.assertNull(addressPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.propertyMappings));
    }

    @Test
    public void embeddedMappingsWithOtherwise()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm[firm1]: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(3, this.graphWalker.getClassMappings(mapping).size());


        CoreInstance firmMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assert.assertNotNull(firmMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(firmMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("FirmInfoTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(firmMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(firmMapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);
        Assert.assertFalse(PrimitiveUtilities.getBooleanValue(firmPropMapping.getValueForMetaPropertyToOne(M3Properties.root)));

        CoreInstance owner = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, owner);

        CoreInstance firmProp = firmPropMapping.getValueForMetaPropertyToOne(M3Properties.property);
        ListIterable<? extends CoreInstance> refUsages = firmProp.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertEquals(1, refUsages.size());
        Assert.assertEquals(firmPropMapping, refUsages.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertNotNull(firmPropMapping.getValueForMetaPropertyToOne(M2MappingProperties.propertyMappings));


        CoreInstance otherwiseMapping = firmPropMapping.getValueForMetaPropertyToOne(M2MappingProperties.otherwisePropertyMapping);
        Assert.assertNotNull(otherwiseMapping);
        CompiledStateIntegrityTestTools.testPropertyIntegrity(otherwiseMapping, processorSupport);
        Assert.assertEquals("alias1", PrimitiveUtilities.getStringValue(otherwiseMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId), null));
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(firmPropMapping).size());
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationOtherwisePropertyMapping(firmPropMapping).size());

        CoreInstance alias = otherwiseMapping.getValueForMetaPropertyToOne(M2RelationalProperties.relationalOperationElement)
                .getValueForMetaPropertyToOne(M2RelationalProperties.joinTreeNode)
                .getValueForMetaPropertyToOne(M2RelationalProperties.alias);
        Assert.assertNotNull(alias);
    }

    @Test
    public void embeddedMappingsWithOtherwise2()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm[f1]: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "       firm[f1]:[db]@PersonFirmJoin \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);

        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);
        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);
        CoreInstance opElement = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.relationalOperationElement)
                .getValueForMetaPropertyToOne(M2RelationalProperties.joinTreeNode).getValueForMetaPropertyToOne(M2RelationalProperties.alias);

        Assert.assertNotNull(opElement);
    }


    @Test
    public void embeddedMappingsWithOtherwiseInClassMappingWithoutId()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm[firm1]: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(3, this.graphWalker.getClassMappings(mapping).size());


        CoreInstance firmMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assert.assertNotNull(firmMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(firmMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("FirmInfoTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(firmMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(firmMapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assert.assertEquals("employeeFirmDenormTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);
        Assert.assertFalse(PrimitiveUtilities.getBooleanValue(firmPropMapping.getValueForMetaPropertyToOne(M3Properties.root)));

        CoreInstance owner = firmPropMapping.getValueForMetaPropertyToOne(M2RelationalProperties.setMappingOwner);
        Assert.assertEquals(personMapping, owner);

        CoreInstance firmProp = firmPropMapping.getValueForMetaPropertyToOne(M3Properties.property);
        ListIterable<? extends CoreInstance> refUsages = firmProp.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertEquals(1, refUsages.size());
        Assert.assertEquals(firmPropMapping, refUsages.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertNotNull(firmPropMapping.getValueForMetaPropertyToOne(M2MappingProperties.propertyMappings));


        CoreInstance otherwiseMapping = firmPropMapping.getValueForMetaPropertyToOne(M2MappingProperties.otherwisePropertyMapping);
        Assert.assertNotNull(otherwiseMapping);
        Assert.assertEquals("other_Person", PrimitiveUtilities.getStringValue(otherwiseMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId), null));
        CompiledStateIntegrityTestTools.testPropertyIntegrity(otherwiseMapping, processorSupport);
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(firmPropMapping).size());
        Assert.assertEquals(1, this.graphWalker.getClassMappingImplementationOtherwisePropertyMapping(firmPropMapping).size());

        CoreInstance alias = otherwiseMapping.getValueForMetaPropertyToOne(M2RelationalProperties.relationalOperationElement)
                .getValueForMetaPropertyToOne(M2RelationalProperties.joinTreeNode)
                .getValueForMetaPropertyToOne(M2RelationalProperties.alias);
        Assert.assertNotNull(alias);
    }


    @Test
    public void otherwiseMappingsWithInvalidTargetId()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        ) Otherwise ([firm1]:[db]@PersonFirmJoin) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid Otherwise mapping found for 'firm' property, targetId firm1 does not exists.", "fromString.pure", 45, 9, e.getCause());
    }

    @Test
    public void redundantOtherwiseMappingsWithTargetId()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "        ) Otherwise ([firm1]:[db]@PersonFirmJoin) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid Otherwise mapping found: 'firm' property has no embedded mappings defined, please use a property mapping with Join instead.", "fromString.pure", 45, 9, e.getCause());
    }

    @Test
    public void embeddedMappingsCanBeReferencedInAssociationMappingAsSource()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Address\n" +
                        "{\n" +
                        "    address:Address[0..1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[a1]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                        "    }\n" +
                        "    Person[p1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm_Address: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           address[p1_firm,a1] : [db]@firmAddress\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

    }

    @Test
    public void inlinedEmbeddedMappingsCanBeReferencedInAssociationMappingAsSource()
    {
        Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Address\n" +
                        "{\n" +
                        "    address:Address[0..1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[a1]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                        "    }\n" +
                        "    Person[p1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm[f1]: Relational\n" +
                        "    {\n" +
                        "         legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "    }\n" +
                        "    Firm_Address: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           address[p1_firm,a1] : [db]@firmAddress\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void inlinedEmbeddedMappingsCanBeReferencedInAssociationMappingAsSourceInvalidId()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Address\n" +
                        "{\n" +
                        "    address:Address[0..1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[a1]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                        "    }\n" +
                        "    Person[p1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm[f1]: Relational\n" +
                        "    {\n" +
                        "         legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "    }\n" +
                        "    Firm_Address: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           address[p1_f1,a1] : [db]@firmAddress\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Unable to find source class mapping (id:p1_f1) for property 'address' in Association mapping 'Firm_Address'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", "fromString.pure", 63, 12, e.getCause());
    }

    @Test
    public void inlinedEmbeddedMappingsCanNotBeReferencedInAssociationMappingAsTarget()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Address\n" +
                        "{\n" +
                        "    address:Address[0..1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[a1]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                        "    }\n" +
                        "    Person[p1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm[f1]: Relational\n" +
                        "    {\n" +
                        "         legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "    }\n" +
                        "    Firm_Address: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           address[p1_firm,a1] : [db]@firmAddress,\n" +
                        "           firm[a1,p1_firm] : [db]@firmAddress\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid target class mapping for property 'firm' in Association mapping 'Firm_Address'. Target 'p1_firm' is an embedded class mapping, embedded mappings are only allowed to be the source in an Association Mapping.", "fromString.pure", 64, 12, e.getCause());
    }

    @Test
    public void embeddedMappingsCannotBeReferencedInAssociationMappingAsTarget()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    address:Address[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    line1:String[1];\n" +
                        "    postcode:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Address\n" +
                        "{\n" +
                        "    address:Address[0..1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Address[a1]: Relational\n" +
                        "    {\n" +
                        "       line1: [db]employeeFirmDenormTable.address1,\n" +
                        "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                        "    }\n" +
                        "    Person[p1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        )\n" +
                        "    }\n" +
                        "    Firm_Address: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           firm[a1,p1_firm] : [db]@firmAddress\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid target class mapping for property 'firm' in Association mapping 'Firm_Address'. Target 'p1_firm' is an embedded class mapping, embedded mappings are only allowed to be the source in an Association Mapping.", "fromString.pure", 59, 12, e.getCause());
    }

    @Test
    public void testOtherwiseEmbeddedWithNoJoin()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                        "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "        ) Otherwise () \n" + //
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureParserException.class, "expected: '[' found: ')'", "fromString.pure", 49, 22, e.getCause());
    }

    @Test
    public void testOtherwiseEmbeddedWithNoJoinOrPropertyMappings()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "        ) Otherwise () \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureParserException.class, "expected: '[' found: ')'", "fromString.pure", 36, 22, e.getCause());
    }

    @Test
    public void testIncorrectInlineMapping()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +

                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm()\n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid Inline mapping found: 'firm' mapping has not inline set defined, please use: firm() Inline[setid].", "fromString.pure", 33, 9, e.getCause());
    }

    @Test
    public void testIncorrectSetIdInlineMapping()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "        ) Inline[] \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: ']'", "fromString.pure", 36, 18, e.getCause());
    }

    @Test
    public void testOtherwiseEmbeddedWithIncorrectJoin()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "        ) Otherwise ([firm1]:[db]@InvalidPersonFirmJoin) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "The join 'InvalidPersonFirmJoin' has not been found in the database 'db'", "fromString.pure", 47, 35, e.getCause());
    }

    @Test
    public void testOtherwiseEmbeddedWithIncorrectOtherwisePropertyMapping()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm[firm1]: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "          legalName : [db]employeeFirmDenormTable.name \n" +
                        "        ) Otherwise ([firm1]:[db]employeeFirmDenormTable.name) \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureParserException.class, "expected: '@' found: 'employeeFirmDenormTable'", "fromString.pure", 48, 34, e.getCause());
    }

    @Test
    public void testInlineEmbeddeedInvalidTarget()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    otherInformation:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        "   Table FirmInfoTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    other VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]FirmInfoTable.name ,\n" +
                        "       otherInformation: [db]FirmInfoTable.other\n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm\n" +
                        "        (\n" +
                        "        ) Inline [firm1] \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Invalid Inline mapping found: 'firm' property, inline set id firm1 does not exists.", "fromString.pure", 45, 9, e.getCause());
    }

    @Test
    public void testIncorrectTypeSetIdInlineMapping()
    {
        LoaderException e = Assert.assertThrows(LoaderException.class, () -> Loader.parseM3(
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "    address:Address[1];\n" +
                        "}\n" +
                        "Class other::Address\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200),\n" +
                        "    address1 VARCHAR(200),\n" +
                        "    postcode VARCHAR(10)\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Firm[firm1]: Relational\n" +
                        "    {\n" +
                        "       legalName : [db]employeeFirmDenormTable.legalName \n" +
                        "    }\n" +
                        "    Person[alias1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        address()Inline[firm1] \n" +
                        "    }\n" +
                        ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context));
        assertPureException(PureCompilationException.class, "Mapping Error! The inlineSetImplementationId 'firm1' is implementing the class 'Firm' which is not a subType of 'Address' (return type of the mapped property 'address')", "fromString.pure", 41, 9, e.getCause());
    }
}