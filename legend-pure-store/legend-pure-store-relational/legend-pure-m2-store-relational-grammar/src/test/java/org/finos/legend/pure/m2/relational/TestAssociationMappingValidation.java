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

import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestAssociationMappingValidation extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String CLASSES = "import other::*;\n" +
            "\n" +
            "Class other::Person\n" +
            "{\n" +
            "    name:String[1];\n" +
            "}\n" +
            "Class other::Firm\n" +
            "{\n" +
            "    legalName:String[1];\n" +
            "}\n";
    private static final String STORE = "###Relational\n" +
            "Database mapping::db(\n" +
            "   Table employeeFirmDenormTable\n" +
            "   (\n" +
            "    id INT PRIMARY KEY,\n" +
            "    name VARCHAR(200),\n" +
            "    firmId INT,\n" +
            "    legalName VARCHAR(200)\n" +
            "   )\n" +
            "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
            ")\n";

    @Test
    public void testMappingAssociationToNonExistentAssociationFails()
    {
        try
        {

            compileTestSource("testFile.pure", CLASSES +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Firm_Person has not been defined!", e);
        }
    }


    //TODO: Main table alias

    @Test
    public void testMappingPropertyInClassAsWellAsAssociationFails()
    {
        try
        {

            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm: [db]@firmJoin\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Property 'firm' is mapped twice, once in Association mapping 'Firm_Person' and once in Class mapping 'Firm'. Only one mapping is allowed.", e);
        }
    }

    @Test
    public void testMappingAssociationWithoutClassMappingFails()
    {
        try
        {

            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,per1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Unable to find source class mapping (id:fir1) for property 'employees' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", e);
        }
    }

    @Test
    public void testMappingWithoutSourceIdFails()
    {
        try
        {

            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Unable to find source class mapping (id:other_Firm) for property 'employees' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", e);
        }
    }

    @Test
    public void testMappingWithInvalidTargetIdFails()
    {
        try
        {
            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,foo] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Unable to find target class mapping (id:foo) for property 'employees' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", "testFile.pure", 45, 12, e);
        }
    }

    @Test
    public void testMappingWithIncludesWithInvalidTargetIdFails()
    {
        compileTestSource("model.pure", CLASSES +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:Person[1];\n" +
                "}");
        compileTestSource("store.pure", STORE);
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::TopMapping\n" +
                        "(\n" +
                        "    Person[per1]: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n" +
                        "\n" +
                        "Mapping mappingPackage::MiddleMapping\n" +
                        "(\n" +
                        "    include mappingPackage::TopMapping\n" +
                        "    Firm[fir1]: Relational\n" +
                        "    {\n" +
                        "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                        "    }\n" +
                        ")");

        // This should work
        compileTestSource("mapping2.pure",
                "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::BottomMapping1\n" +
                        "(\n" +
                        "    include mappingPackage::MiddleMapping\n" +
                        "    Firm_Person: Relational\n" +
                        "    {\n" +
                        "        AssociationMapping\n" +
                        "        (\n" +
                        "           employees[fir1,per1] : [db]@firmJoin,\n" +
                        "           firm[per1,fir1] : [db]@firmJoin\n" +
                        "        )\n" +
                        "    }\n" +
                        ")");

        try
        {
            // This should not work
            compileTestSource("mapping3.pure",
                    "###Mapping\n" +
                            "import other::*;\n" +
                            "import mapping::*;\n" +
                            "Mapping mappingPackage::BottomMapping2\n" +
                            "(\n" +
                            "    include mappingPackage::MiddleMapping\n" +
                            "    Firm_Person: Relational\n" +
                            "    {\n" +
                            "        AssociationMapping\n" +
                            "        (\n" +
                            "           employees[fir1,foo] : [db]@firmJoin,\n" +
                            "           firm[per1,fir1] : [db]@firmJoin\n" +
                            "        )\n" +
                            "    }\n" +
                            ")");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Unable to find target class mapping (id:foo) for property 'employees' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", "mapping3.pure", 11, 12, e);
        }
    }

    @Test
    public void testMappingToIncorrectSourceTypeFails()
    {
        try
        {

            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[per1, per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Association mapping property 'employees' in Association mapping 'Firm_Person' is not a property of source class 'Person'. Make sure that you have specified a valid source id.", e);
        }
    }

    @Test
    public void testJoinWithWrongSourceTable()
    {
        compileTestSource("testClasses.pure", CLASSES);
        compileTestSource("testStore.pure",
                "###Relational\n" +
                        "Database other::db\n" +
                        "(\n" +
                        "  Table firmTable(id INT PRIMARY KEY, legalName VARCHAR(200), parentId INT)\n" +
                        "  Table employeeTable(id INT PRIMARY KEY, name VARCHAR(200), firmId INT)\n" +
                        "  Join Employer(employeeTable.firmId = firmTable.id)\n" +
                        "  Join FirmParent(firmTable.parentId = {target}.id)\n" +
                        ")\n");
        compileTestSource("testAssociations.pure",
                "import other::*;\n" +
                        "Association other::Employment\n" +
                        "{\n" +
                        "   employer : Firm[0..1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n" +
                        "\n");
        try
        {
            compileTestSource("testMapping.pure",
                    "###Mapping\n" +
                            "import other::*;\n" +
                            "Mapping other::mapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "  {\n" +
                            "    legalName : [db]firmTable.legalName\n" +
                            "  }\n" +
                            "  Person : Relational\n" +
                            "  {\n" +
                            "    name : [db]employeeTable.name\n" +
                            "  }\n" +
                            "  Employment : Relational\n" +
                            "  {\n" +
                            "    AssociationMapping\n" +
                            "    (\n" +
                            "      employer : [db]@FirmParent,\n" +
                            "      employees : [db]@Employer\n" +
                            "    )\n" +
                            "  }\n" +
                            ")");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@FirmParent does not contain the source table [db]employeeTable", "testMapping.pure", 17, 23, 17, 23, 17, 32, e);
        }
    }

    @Test
    public void testAttemptingToMapAnAssociationQualifiedPropertyFails()
    {
        try
        {
            compileTestSource("testFile.pure", CLASSES +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:Person[1];\n" +
                    "    employeeByName(dept: String[1]){$this.employees->filter(e|$e.name == $dept)->toOne()}:Person[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1, per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin,\n" +
                    "           employeeByName[fir1, per1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail("Expected validation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The property 'employeeByName' is unknown in the Element 'other::Firm_Person'", e);
        }
    }


    @Test
    public void testJoinWithWrongTargetTable()
    {
        compileTestSource("testClasses.pure", CLASSES);
        compileTestSource("testStore.pure",
                "###Relational\n" +
                        "Database other::db\n" +
                        "(\n" +
                        "  Table firmTable(id INT PRIMARY KEY, legalName VARCHAR(200), parentId INT)\n" +
                        "  Table employeeTable(id INT PRIMARY KEY, name VARCHAR(200), firmId INT)\n" +
                        "  Join Employer(employeeTable.firmId = firmTable.id)\n" +
                        "  Join FirmParent(firmTable.parentId = {target}.id)\n" +
                        ")\n");
        compileTestSource("testAssociations.pure",
                "import other::*;\n" +
                        "Association other::Employment\n" +
                        "{\n" +
                        "   employer : Firm[0..1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n" +
                        "\n");
        try
        {
            compileTestSource("testMapping.pure",
                    "###Mapping\n" +
                            "import other::*;\n" +
                            "Mapping other::mapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "  {\n" +
                            "    legalName : [db]firmTable.legalName\n" +
                            "  }\n" +
                            "  Person : Relational\n" +
                            "  {\n" +
                            "    name : [db]employeeTable.name\n" +
                            "  }\n" +
                            "  Employment : Relational\n" +
                            "  {\n" +
                            "    AssociationMapping\n" +
                            "    (\n" +
                            "      employer : [db]@Employer,\n" +
                            "      employees : [db]@FirmParent\n" +
                            "    )\n" +
                            "  }\n" +
                            ")");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@FirmParent does not connect from the source table [db]firmTable to the target table [db]employeeTable; instead it connects to [db]firmTable", "testMapping.pure", 18, 24, 18, 24, 18, 33, e);
        }
    }

    @Test
    public void testNonConnectingJoinSequence()
    {
        compileTestSource("testClasses.pure", CLASSES);
        compileTestSource("testStore.pure",
                "###Relational\n" +
                        "Database other::db\n" +
                        "(\n" +
                        "  Table firmTable(id INT PRIMARY KEY, legalName VARCHAR(200), parentId INT)\n" +
                        "  Table employeeTable(id INT PRIMARY KEY, name VARCHAR(200), firmId INT, otherFirmId INT)\n" +
                        "  Table otherFirmTable(id INT, legalName VARCHAR(200))\n" +
                        "  Join Employer(employeeTable.firmId = firmTable.id)\n" +
                        "  Join FirmParent(firmTable.parentId = {target}.id)\n" +
                        "  Join EmployeeOther(employeeTable.otherFirmId = otherFirmTable.id)" +
                        ")\n");
        compileTestSource("testAssociations.pure",
                "import other::*;\n" +
                        "Association other::Employment\n" +
                        "{\n" +
                        "   employer : Firm[0..1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n");
        try
        {
            compileTestSource("testMapping.pure",
                    "###Mapping\n" +
                            "import other::*;\n" +
                            "Mapping other::mapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "  {\n" +
                            "    legalName : [db]firmTable.legalName\n" +
                            "  }\n" +
                            "  Person : Relational\n" +
                            "  {\n" +
                            "    name : [db]employeeTable.name\n" +
                            "  }\n" +
                            "  Employment : Relational\n" +
                            "  {\n" +
                            "    AssociationMapping\n" +
                            "    (\n" +
                            "      employer : [db]@EmployeeOther > @Employer,\n" +
                            "      employees : [db]@Employer\n" +
                            "    )\n" +
                            "  }\n" +
                            ")");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@Employer does not contain the source table [db]otherFirmTable", "testMapping.pure", 17, 40, 17, 40, 17, 47, e);
        }
    }

    @Test
    public void testDuplicateAssociationMapping()
    {
        compileTestSource("testClasses.pure", CLASSES);
        compileTestSource("testStore.pure",
                "###Relational\n" +
                        "Database other::db\n" +
                        "(\n" +
                        "  Table firmTable(id INT PRIMARY KEY, legalName VARCHAR(200), parentId INT)\n" +
                        "  Table employeeTable(id INT PRIMARY KEY, name VARCHAR(200), firmId INT)\n" +
                        "  Join Employer(employeeTable.firmId = firmTable.id)\n" +
                        ")\n");
        compileTestSource("testAssociations.pure",
                "import other::*;\n" +
                        "Association other::Employment\n" +
                        "{\n" +
                        "   employer : Firm[0..1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n" +
                        "\n");
        try
        {
            compileTestSource("testMapping.pure",
                    "###Mapping\n" +
                            "import other::*;\n" +
                            "Mapping other::mapping\n" +
                            "(\n" +
                            "  Firm : Relational\n" +
                            "  {\n" +
                            "    legalName : [db]firmTable.legalName\n" +
                            "  }\n" +
                            "  Person : Relational\n" +
                            "  {\n" +
                            "    name : [db]employeeTable.name\n" +
                            "  }\n" +
                            "  Employment : Relational\n" +
                            "  {\n" +
                            "    AssociationMapping\n" +
                            "    (\n" +
                            "      employer : [db]@Employer,\n" +
                            "      employees : [db]@Employer\n" +
                            "    )\n" +
                            "  }\n" +
                            "  Employment : Relational\n" +
                            "  {\n" +
                            "    AssociationMapping\n" +
                            "    (\n" +
                            "      employer : [db]@Employer,\n" +
                            "      employees : [db]@Employer\n" +
                            "    )\n" +
                            "  }\n" +
                            ")");
            Assert.fail("Expected validation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'other_Employment' in mapping other::mapping", "testMapping.pure", 21, 3, 21, 3, 28, 3, e);
        }
    }


    @Test
    public void testMappingWithClassSuperTypeMappingSucceeds()
    {

        compileTestSource("testFile.pure", CLASSES +
                "Class other::MyPerson extends other::Person\n" +
                "{\n" +
                "}\n" +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:MyPerson[1];\n" +
                "}\n" +
                STORE +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                "\n" +
                "    MyPerson extends [other_Person] : Relational\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    Firm[fir1]: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                "\n" +
                "    Firm_Person: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           employees[fir1,other_MyPerson] : [db]@firmJoin,\n" +
                "           firm[other_MyPerson,fir1] : [db]@firmJoin\n" +
                "        )\n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void testMappingWithClassSuperTypeMappingWrongIdFails()
    {

        try
        {
            compileTestSource("testFile.pure", CLASSES +
                    "Class other::MyPerson extends other::Person\n" +
                    "{\n" +
                    "}\n" +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:MyPerson[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    "\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail();
        }
        catch (PureCompilationException e)
        {
            assertPureException(PureCompilationException.class, "Association mapping property 'firm' in Association mapping 'Firm_Person' is not a property of source class 'Person'. Make sure that you have specified a valid source id.", "testFile.pure", 49, 12, 49, 12, 49, 15, e);
        }

    }


    @Test
    public void testMappingWithClassSuperTypeMappingWrongIdFailsWithIncludes()
    {

        try
        {
            compileTestSource("testFile.pure", CLASSES +
                    "Class other::MyPerson extends other::Person\n" +
                    "{\n" +
                    "}\n" +
                    "Association other::Firm_Person\n" +
                    "{\n" +
                    "    firm:Firm[1];\n" +
                    "    employees:MyPerson[1];\n" +
                    "}\n" +
                    STORE +
                    "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    ")\n" +
                    "###Mapping \n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping2\n" +
                    "(\n" +
                    "    include mappingPackage::subMapping1\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,other_MyPerson] : [db]@firmJoin,\n" +
                    "           firm[other_MyPerson,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n");
            Assert.fail();
        }
        catch (PureCompilationException e)
        {
            assertPureException(PureCompilationException.class, "Unable to find source class mapping (id:other_MyPerson) for property 'firm' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", "testFile.pure", 55, 12, 55, 12, 55, 15, e);
        }

    }

    @Test
    public void testMappingWithClassSuperTypeMappingSucceedsDefaultIds()
    {
        compileTestSource("testFile.pure", CLASSES +
                "Class other::MyPerson extends other::Person\n" +
                "{\n" +
                "}\n" +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:MyPerson[1];\n" +
                "}\n" +
                STORE +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                "\n" +
                "    MyPerson extends [other_Person] : Relational\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    Firm: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                "\n" +
                "    Firm_Person: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           employees : [db]@firmJoin,\n" +
                "           firm : [db]@firmJoin\n" +
                "        )\n" +
                "    }\n" +
                ")\n");
    }
}
