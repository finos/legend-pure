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

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.Test;

public class TestReferenceUsagesForDatabase extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final Predicate NULL_SOURCE_INFORMATION = new Predicate<ReferenceUsage>()
    {
        @Override
        public boolean accept(ReferenceUsage referenceUsage)
        {
            return referenceUsage.getSourceInformation() == null;
        }
    };

    private static final HashingStrategy COMPARE_SOURCE_ID_LINE_COLUMN = new HashingStrategy<ReferenceUsage>()
    {
        @Override
        public int computeHashCode(ReferenceUsage referenceUsage)
        {
            return 0;
        }

        @Override
        public boolean equals(ReferenceUsage object1, ReferenceUsage object2)
        {
            return object1.getSourceInformation().getSourceId().equals(object2.getSourceInformation().getSourceId()) &&
                    object1.getSourceInformation().getLine() == object2.getSourceInformation().getLine() &&
                    object1.getSourceInformation().getColumn() == object2.getSourceInformation().getColumn();
        }
    };

    private static void createAndCompileSourceCode(PureRuntime runtime, String sourceId, String sourceCode)
    {
        runtime.delete(sourceId);
        runtime.createInMemorySource(sourceId, sourceCode);
        runtime.compile();
    }

    private static void assertDatabaseReferenceUsages(PureRuntime runtime, String sourceCode, String dbName, int dbCount)
    {
        String[] lines = sourceCode.split("\n");
        Database database = (Database)runtime.getCoreInstance("my::" + dbName);
        MutableList<? extends ReferenceUsage> databaseReferenceUsageList = Lists.mutable.ofAll(database._referenceUsages()).reject(NULL_SOURCE_INFORMATION).distinct(COMPARE_SOURCE_ID_LINE_COLUMN);
        Assert.assertEquals(dbCount, databaseReferenceUsageList.size());
        for (ReferenceUsage referenceUsage : databaseReferenceUsageList)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assert.assertEquals(dbName, lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + dbName.length() - 1));
        }
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithNoDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "Database my::mainDb\n" +
                "( \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson(PersonTable.firmId = FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnRightHandSide()
    {
        String sourceCode = "###Relational\n" +
                "Database my::mainDb\n" +
                "( \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson(PersonTable.firmId = [my::mainDb]FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnLeftHandSide()
    {
        String sourceCode = "###Relational\n" +
                "Database my::mainDb\n" +
                "( \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnBothSides()
    {
        String sourceCode = "###Relational\n" +
                "Database my::mainDb\n" +
                "( \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 2);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinAcrossDatabasesWithDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::db1\n" +
                "(\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::db2\n" +
                "(\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join EmploymentJoin([my::db1]PersonTable.firmId = [my::db2]FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 1);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinAcrossDatabasesWithNoDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::db1\n" +
                "(\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::db2\n" +
                "(\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join EmploymentJoin([my::db1]PersonTable.firmId = FirmTable.id)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 1);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithAllFourDatabaseMarkers()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and \n" +
                "                   [my::mainDb]PersonTable.lastName = [my::mainDb]FirmTable.legalName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 4);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithThreeOutOfFourDatabaseMarkers()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and \n" +
                "                   [my::mainDb]PersonTable.lastName = FirmTable.legalName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithTwoOutOfFourDatabaseMarkers()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and \n" +
                "                   PersonTable.lastName = FirmTable.legalName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 2);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithOneOutOfFourDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson([my::mainDb]PersonTable.firmId = FirmTable.id and \n" +
                "                   PersonTable.lastName = FirmTable.legalName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithNoDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson(PersonTable.firmId = FirmTable.id and \n" +
                "                   PersonTable.lastName = FirmTable.legalName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithSelfJoinWithDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Join DummySelfJoin([my::mainDb]PersonTable.firstName = {target}.lastName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithSelfJoinWithNoDatabaseMarker()
    {
        String sourceCode = "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(  \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Join DummySelfJoin(PersonTable.firstName = {target}.lastName)\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMapping()
    {
        String sourceCode = "Class my::Firm\n" +
                "{\n" +
                "   id :  Integer[1];\n" +
                "   legalName : String[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class my::Person\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   lastName : String[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::mainMap\n" +
                "(\n" +
                "   Person : Relational\n" +
                "   {\n" +
                "      scope([mainDb]PersonTable)\n" +
                "      (\n" +
                "         firstName : firstName,\n" +
                "         lastName : lastName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm : Relational\n" +
                "   {\n" +
                "      id : [mainDb]FirmTable.id,\n" +
                "      legalName : [mainDb]FirmTable.legalName\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsageForDatabaseWithMappingWithMainTable()
    {
        String sourceCode = "Class my::Firm\n" +
                "{\n" +
                "   id :  Integer[1];\n" +
                "   legalName : String[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class my::Person\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   lastName : String[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::mainMap\n" +
                "(\n" +
                "   Person : Relational\n" +
                "   {\n" +
                "      ~mainTable [mainDb] PersonTable\n" +
                "      scope([mainDb]PersonTable)\n" +
                "      (\n" +
                "         firstName : firstName,\n" +
                "         lastName : lastName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm : Relational\n" +
                "   {\n" +
                "      id : [mainDb]FirmTable.id,\n" +
                "      legalName : [mainDb]FirmTable.legalName\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 4);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMappingWithFilter()
    {
        String sourceCode = "Class my::Person\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   lastName : String[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))\n" +
                "   Filter PersonFilter(PersonTable.firstName = 'Utkarsh')\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::mainMap\n" +
                "(\n" +
                "   Person : Relational\n" +
                "   {\n" +
                "      ~filter [mainDb] PersonFilter\n" +
                "      firstName : [mainDb]PersonTable.firstName,\n" +
                "      lastName : [mainDb]PersonTable.lastName\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMappingWithInclude()
    {
        String sourceCode = "Class my::Firm\n" +
                "{\n" +
                "   id :  Integer[1];\n" +
                "   legalName : String[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class my::Person\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   lastName : String[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::db1\n" +
                "(\n" +
                "   include my::db2\n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::db2\n" +
                "(\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::map1\n" +
                "(\n" +
                "   include map2[db2->db1]\n" +
                "   Person : Relational\n" +
                "   {\n" +
                "      scope([db1]PersonTable)\n" +
                "      (\n" +
                "         firstName : firstName,\n" +
                "         lastName : lastName\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping my::map2   \n" +
                "(\n" +
                "   Firm : Relational\n" +
                "   {\n" +
                "      id : [db2]FirmTable.id,\n" +
                "      legalName : [db2]FirmTable.legalName\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 2);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 4);
    }

    @Test
    public void testReferenceUsageForDatabaseWithAll()
    {
        String sourceCode = "import my::*;\n" +
                "\n" +
                "Class my::Firm\n" +
                "{\n" +
                "   legalName : String[1];\n" +
                "   employees : Person[*];\n" +
                "}\n" +
                "\n" +
                "Class my::Person\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   lastName : String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class my::Salary\n" +
                "{\n" +
                "   firstName : String[0..1];\n" +
                "   salary : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::mainDb\n" +
                "(\n" +
                "   include my::subDb\n" +
                "   \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                "   Table FirmTable(id INTEGER, legalName VARCHAR(200))\n" +
                "   Join FirmPerson(PersonTable.firmId = FirmTable.id and [my::mainDb]PersonTable.lastName = [my::mainDb]FirmTable.legalName)\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::subDb\n" +
                "(\n" +
                "   Table SalaryTable(firstName VARCHAR(200), salary INTEGER)\n" +
                "   Filter SalaryFilter(SalaryTable.salary > 100000)\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database my::tempDb\n" +
                "(\n" +
                "   Table TempTable(string1 VARCHAR(200), string2 VARCHAR(200))\n" +
                "   Join SelfJoin([my::tempDb]TempTable.string1 = {target}.string2)\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::mainMap\n" +
                "(\n" +
                "   include subMap[subDb->mainDb]\n" +
                "   Person : Relational\n" +
                "   {\n" +
                "      ~mainTable [mainDb] PersonTable\n" +
                "      scope([mainDb]PersonTable)\n" +
                "      (\n" +
                "         firstName : firstName,\n" +
                "         lastName : lastName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm : Relational\n" +
                "   {\n" +
                "      legalName : [mainDb]FirmTable.legalName,\n" +
                "      employees : [mainDb]@FirmPerson\n" +
                "      \n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping my::subMap\n" +
                "(\n" +
                "   Salary : Relational\n" +
                "   {\n" +
                "      ~filter [subDb] SalaryFilter\n" +
                "      firstName : [subDb]SalaryTable.firstName,\n" +
                "      salary : [subDb]SalaryTable.salary\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 7);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "subDb", 5);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "tempDb", 1);
    }
}
