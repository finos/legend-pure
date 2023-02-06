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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m2.relational.RelationalGraphWalker;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestPureRuntimeClassMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String CLASS_SOURCE_ID = "classSourceId.pure";
    private static final String CLASS_PERSON = "Class Person{name:String[1];}\n";

    private static final String RELATIONAL_DB_SOURCE_ID = "dbSourceId.pure";
    private static final String RELATIONAL = "###Relational\n";
    private static final String DATABASE = "Database db(Table myTable(name VARCHAR(200)))\n";
    private static final String RELATIONAL_DATABASE = RELATIONAL + DATABASE;

    private static final String CLASS_MAPPING_SOURCE_ID = "classMappingSourceId.pure";
    private static final String MAPPING = "###Mapping\n";
    private static final String CLASS_MAPPING_PERSON_RELATIONAL = "Mapping myMap(Person:Relational{name : [db]myTable.name})";
    private static final String CLASS_MAPPING = MAPPING + CLASS_MAPPING_PERSON_RELATIONAL;

    private static final String FUNCTION_TEST_CLASS_MAPPINGS_SIZE = "###Pure\n" +
            "function test():Boolean[1]{assert(1 == myMap.classMappings->size(), |'');}";

    private static final String CLASS_ORG = "Class Org\n" +
            "{\n" +
            "    name : String[1];\n" +
            "    parent: Org[0..1];\n" +
            "    childOrgEmployeeCount: Integer[0..1];\n" +
            "    children: Org[*];\n" +
            "}\n" +
            "Association OrgSelf\n" +
            "{\n" +
            "   top : Org[1];\n" +
            "   bottom : Org[*];\n" +
            "}";

    private static final String ORG_TABLE =
            "Table orgTable\n" +
                    "    (\n" +
                    "        id INT,\n" +
                    "        filterVal INT,\n" +
                    "        parentId INT,\n" +
                    "        name VARCHAR(200),\n" +
                    "        employeeCount INT\n" +
                    "    )\n";

    private static final String ORG_TABLE_WITHOUT_EMPLOYEE_COUNT =
            "Table orgTable\n" +
                    "    (\n" +
                    "        id INT,\n" +
                    "        filterVal INT,\n" +
                    "        parentId INT,\n" +
                    "        name VARCHAR(200)\n" +
                    "    )\n";

    private static String getComplexDatabase(String orgTable)
    {
        return RELATIONAL + "Database myDB\n" +
                "(\n" +
                orgTable +
                "    \n" +
                "    Table otherTable\n" +
                "    (\n" +
                "       orgTableId INT,\n" +
                "       filterVal INT\n" +
                "    )\n" +
                "    \n" +
                "    Join OrgOrgParent\n" +
                "    (\n" +
                "       orgTable.parentId = {target}.id\n" +
                "    )\n" +
                "    Join OrgParentOrg\n" +
                "    (\n" +
                "       orgTable.id = {target}.parentId\n" +
                "    )\n" +
                "    Join OrgTableOtherTable\n" +
                "    (\n" +
                "       orgTable.id = otherTable.orgTableId\n" +
                "    )\n" +
                "    Filter myFilter(orgTable.filterVal <= 4)\n" +
                "    Filter myFilter2(otherTable.filterVal <= 4)\n" +
                ")";
    }

    private static String getComplexMapping(MutableList<String> extraAttributes)
    {
        return MAPPING + "Mapping myMap\n" +
                "(\n" +
                "    Org: Relational\n" +
                "    {\n" +
                "        ~filter [myDB]@OrgTableOtherTable|[myDB]myFilter2\n" +
                "        name: [myDB]orgTable.name,\n" +
                "        parent : [myDB]@OrgOrgParent,\n" +
                "        children : [myDB]@OrgParentOrg\n" +
                (extraAttributes.isEmpty() ? "" : extraAttributes.makeString("\t\t,", "", "\n")) +
                "    }\n" +
                ")";
    }

    private static final String childOrgEmployeeCountAttr = "childOrgEmployeeCount : [myDB]@OrgParentOrg | max([myDB]orgTable.employeeCount)";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(
            CLASS_MAPPING_SOURCE_ID, CLASS_MAPPING,
            CLASS_SOURCE_ID, CLASS_PERSON,
            RELATIONAL_DB_SOURCE_ID, RELATIONAL_DATABASE);

    private static final ImmutableMap<String, String> COMPLEX_TEST_SOURCES = Maps.immutable.with(
            CLASS_MAPPING_SOURCE_ID, getComplexMapping(Lists.mutable.with(childOrgEmployeeCountAttr)),
            CLASS_SOURCE_ID, CLASS_ORG,
            RELATIONAL_DB_SOURCE_ID, getComplexDatabase(ORG_TABLE));

    private static final String CLASS_PERSON2 = "Class Person\n" +
            "{\n" +
            "    fullName(){$this.lastName+', '+$this.firstName}:String[1];\n" +
            "    firstName : String[1];\n" +
            "    lastName: String[1];\n" +
            "    displayName : String[1];\n" +
            "}\n";

    private static final String CLASS_MAPPING_PERSON_WITH_TRANSFORM = MAPPING +
            "Mapping myMap\n" +
            "(\n" +
            "   Person : Relational\n" +
            "            {\n" +
            "               ~primaryKey ([myDB]personTable.ID)" +
            "               scope([myDB]default.personTable)\n" +
            "               (\n" +
            "                  firstName : substring(FULLNAME, 0, sub(position(',', FULLNAME), 1)),\n" +
            "                  displayName : FULLNAME \n" +
            "               )\n" +
            "               ,\n" +
            "               lastName : substring([myDB]default.personTable.FULLNAME, add(position(',', [myDB]default.personTable.FULLNAME),2))\n" +
            "            }\n" +
            ")";

    private static final String DB_FOR_TRANSFORM = RELATIONAL +
            "Database myDB\n" +
            "(\n" +
            "    Table personTable (ID INT, FULLNAME VARCHAR(200))\n" +
            "    Table personTableExtension (ID INT, FULLNAME_PART2 VARCHAR(200))\n" +
            "    Join personExtension (personTable.ID = personTableExtension.ID)\n" +
            ")\n";

    private static final ImmutableMap<String, String> TEST_SOURCES_WITH_MAPPING_TRANSFORM = Maps.immutable.with(
            CLASS_MAPPING_SOURCE_ID, CLASS_MAPPING_PERSON_WITH_TRANSFORM,
            CLASS_SOURCE_ID, CLASS_PERSON2,
            RELATIONAL_DB_SOURCE_ID, DB_FOR_TRANSFORM);

    @Test
    public void testDeleteAndReloadEachSource()
    {
        testDeleteAndReloadEachSource(TEST_SOURCES, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);
    }

    @Test
    public void testDeleteAndReloadEachComplexSource()
    {
        testDeleteAndReloadEachSource(COMPLEX_TEST_SOURCES, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);
    }

    @Test
    public void testDeleteAndReloadEachSourceWithTransform()
    {
        testDeleteAndReloadEachSource(TEST_SOURCES_WITH_MAPPING_TRANSFORM, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadEachSource(ImmutableMap<String, String> sources, String testFunctionSource)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
//            System.out.println("Deleting " + source.getOne());
            new RuntimeTestScriptBuilder().createInMemorySources(sources)
                    .createInMemorySource("functionSourceId.pure", testFunctionSource)
                    .compile().run(runtime, functionExecution);

            RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                    Lists.fixedSize.of(source), this.getAdditionalVerifiers());

            //reset so that the next iteration has a clean environment
            setUpRuntime();
        }
    }

    @Test
    public void testDeleteAndReloadSourcePairs()
    {
        this.testDeleteAndReloadTwoSources(
                TEST_SOURCES, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);
    }

    @Test
    public void testDeleteAndReloadComplexSourcePairs()
    {
        this.testDeleteAndReloadTwoSources(
                COMPLEX_TEST_SOURCES, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadTwoSources(ImmutableMap<String, String> sources,
                                              String testFunctionSource)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            List<Pair<String, String>> sourcesClone = sources.keyValuesView().toList();
            sourcesClone.remove(source);

            for (Pair<String, String> secondSource : sourcesClone)
            {
                new RuntimeTestScriptBuilder().createInMemorySources(sources)
                        .createInMemorySource("functionSourceId.pure", testFunctionSource)
                        .compile().run(runtime, functionExecution);

                RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                        Lists.fixedSize.of(source, secondSource),
                        this.getAdditionalVerifiers());

                //reset so that the next iteration has a clean environment
                setUpRuntime();
            }
        }
    }

    @Test
    public void testEmbeddedUnbind()
    {
        String classSource = "import other::*;\n" +
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
                "    address:Address[1];\n" +
                "}\n" +
                "Class other::Address\n" +
                "{\n" +
                "    line1:String[1];\n" +
                "}\n";
        String dbSource = "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address VARCHAR(200)\n" +
                "   )\n" +
                ")\n";
        String mappingSource =
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
                        "        )\n" +
                        "    }\n" +
                        ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of("1.pure", classSource, "2.pure", dbSource, "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == mappingPackage::myMapping.classMappings->size(), |'');}");
    }

    @Test
    public void testOtherwiseEmbeddedUnbind()
    {

        String classSource = "import other::*;\n" +
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
                "}\n";

        String dbSource = "###Relational\n" +
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
                ")\n";


        String mappingSource = "###Mapping\n" +
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
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                "    }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of("1.pure", classSource, "2.pure", dbSource, "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(2 == mappingPackage::myMapping.classMappings->size(), |'');}");
    }

    @Test
    public void testUnbindingOfRelationalOperationElementWithJoin()
    {
        new RuntimeTestScriptBuilder().createInMemorySources(COMPLEX_TEST_SOURCES)
                .compile().run(runtime, functionExecution);

        RelationalGraphWalker graphWalker = new RelationalGraphWalker(runtime, processorSupport);

        CoreInstance childOrgEmployeeCountsAttr = getPropertyMapping(graphWalker, "myMap", "Org", "childOrgEmployeeCount");
        Assert.assertNotNull(childOrgEmployeeCountsAttr);

        new RuntimeTestScriptBuilder().deleteSource(CLASS_MAPPING_SOURCE_ID).createInMemorySource(CLASS_MAPPING_SOURCE_ID, getComplexMapping(Lists.mutable.empty())).compile().run(runtime, functionExecution);
        CoreInstance childOrgEmployeeCountsAttr2 = getPropertyMapping(graphWalker, "myMap", "Org", "childOrgEmployeeCount");
        Assert.assertNull(childOrgEmployeeCountsAttr2);
    }

    @Test
    public void testBindUnbindOfColumnMappedToInputParamOfDynaFunction()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(COMPLEX_TEST_SOURCES)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(RELATIONAL_DB_SOURCE_ID)
                        .createInMemorySource(RELATIONAL_DB_SOURCE_ID, getComplexDatabase(ORG_TABLE_WITHOUT_EMPLOYEE_COUNT))
                        .compileWithExpectedCompileFailure("The column 'employeeCount' can't be found in the table 'orgTable'", null, 10, 69)
                        .deleteSource(RELATIONAL_DB_SOURCE_ID)
                        .createInMemorySource(RELATIONAL_DB_SOURCE_ID, getComplexDatabase(ORG_TABLE))
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testViewUnbind()
    {
        String classSource =
                "import other::*;\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "    addressLine1:String[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[*];\n" +
                        "}";
        String dbSource = "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT\n" +
                "   )\n" +
                "   Table firmTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Table addressTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    employeeId INT,\n" +
                "    line VARCHAR(200)\n" +
                "   )\n" +
                "   View employeeAddressView   \n" +
                "   (\n" +
                "    employeeId : employeeTable.id,\n" +
                "    employeeName : employeeTable.name,\n" +
                "    employeeFirmId : employeeTable.firmId,\n" +
                "    addressLine :  @employee_address | addressTable.line\n" +
                "   )\n" +
                "   Join employee_address(employeeTable.id=addressTable.id)\n " +
                "   Join firm_employees(firmTable.id=employeeAddressView.employeeFirmId)\n" +
                ")";
        String mappingSource =
                "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeAddressView.employeeName,\n" +
                        "         addressLine1 : [db]employeeAddressView.addressLine" +
                        "    }\n" +
                        "    Firm: Relational\n" +
                        "    {\n" +
                        "        legalName : [db]firmTable.legalName,\n" +
                        "        employees : [db]@firm_employees\n" +
                        "    }\n" +
                        ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of("1.pure", classSource, "2.pure", dbSource, "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == mappingPackage::myMapping.classMappings->size(), |'');}");
    }

    @Test
    public void testMilestoningMappingUnbindStability()
    {
        String modelTrade = "Class my::Trade{\n" +
                "   id:Integer[1];\n" +
                "   product:my::Product[1];\n" +
                "}";
        String modelProduct = "Class my::Product{\n" +
                "   id:Integer[1];\n" +
                "}";
        String modelProductTemporal = "Class <<temporal.businesstemporal>> my::Product{\n" +
                "   id:Integer[1];\n" +
                "}";
        String storeAndMapping = "###Mapping\n" +
                "import meta::relational::tests::*;\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping myMapping\n" +
                "(\n" +
                "   Trade : Relational {id : [myDB] tradeTable.ID, product : [myDB] @trade_product} \n" +
                "   Product : Relational { id : [myDB] productTable.ID}\n" +
                ")\n" +
                "###Relational\n" +
                "Database myDB\n" +
                "(\n" +
                "   Table tradeTable(ID INT PRIMARY KEY, PRODID INT)\n" +
                "   Table productTable(ID INT PRIMARY KEY)\n" +
                "   \n" +
                "   Join trade_product(tradeTable.PRODID = productTable.ID)\n" +
                ")";
        String f = "function f():Any[0..1]{let m = myMapping}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelTrade.pure", modelTrade)
                        .createInMemorySource("modelProductTemporal.pure", modelProductTemporal)
                        .createInMemorySource("storeAndMapping.pure", storeAndMapping)
                        .createInMemorySource("f.pure", f)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("modelProductTemporal.pure")
                        .createInMemorySource("modelProduct.pure", modelProduct)
                        .compile()
                        .deleteSource("modelProduct.pure")
                        .createInMemorySource("modelProductTemporal.pure", modelProductTemporal)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    private CoreInstance getPropertyMapping(RelationalGraphWalker graphWalker, String mappingName, String className, String attributeName)
    {
        CoreInstance mapping = graphWalker.getMapping(mappingName);
        CoreInstance orgMapping = graphWalker.getClassMapping(mapping, className);
        return graphWalker.getClassMappingImplementationPropertyMapping(orgMapping, attributeName);
    }

    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.of();
    }
}
