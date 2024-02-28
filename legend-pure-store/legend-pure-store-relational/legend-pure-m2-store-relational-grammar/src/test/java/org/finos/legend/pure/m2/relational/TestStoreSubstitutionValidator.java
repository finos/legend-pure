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

import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestStoreSubstitutionValidator extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";

    private static final String MODEL_CODE = "###Pure\n" +
            "import a::*;\n" +
            "\n" +
            "Class a::Person1\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class a::Firm1\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association a::PersonFirm1\n" +
            "{\n" +
            "   person1 : Person1[*];\n" +
            "   firm1 : Firm1[0..1];\n" +
            "}\n" +
            "\n" +
            "Class a::Person2\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class a::Firm2\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association a::PersonFirm2\n" +
            "{\n" +
            "   person2 : Person2[*];\n" +
            "   firm2 : Firm2[0..1];\n" +
            "}\n" +
            "\n" +
            "Class a::Person3\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class a::Firm3\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association a::PersonFirm3\n" +
            "{\n" +
            "   person3 : Person3[*];\n" +
            "   firm3 : Firm3[0..1];\n" +
            "}\n" +
            "\n" +
            "Class a::Person4\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class a::Firm4\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association a::PersonFirm4\n" +
            "{\n" +
            "   person4 : Person4[*];\n" +
            "   firm4 : Firm4[0..1];\n" +
            "}\n" +
            "\n" +
            "Class a::Person5\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class a::Firm5\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association a::PersonFirm5\n" +
            "{\n" +
            "   person5 : Person5[*];\n" +
            "   firm5 : Firm5[0..1];\n" +
            "}\n";

    private static final String STORE_CODE = "###Relational\n" +
            "Database a::PersonFirmDatabase1\n" +
            "(\n" +
            "   include a::PersonFirmDatabase2\n" +
            "   include a::PersonFirmDatabase3\n" +
            "   include a::PersonFirmDatabase4\n" +
            "   Table PersonTable1(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)\n" +
            "   Table FirmTable1(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join PersonFirm1(PersonTable1.firmId = FirmTable1.firmId)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database a::PersonFirmDatabase2\n" +
            "(\n" +
            "   include a::PersonFirmDatabase3\n" +
            "   include a::PersonFirmDatabase4\n" +
            "   Table PersonTable2(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)\n" +
            "   Table FirmTable2(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join PersonFirm2(PersonTable2.firmId = FirmTable2.firmId)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database a::PersonFirmDatabase3\n" +
            "(\n" +
            "   include a::PersonFirmDatabase5\n" +
            "   Table PersonTable3(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)\n" +
            "   Table FirmTable3(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join PersonFirm3(PersonTable3.firmId = FirmTable3.firmId)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database a::PersonFirmDatabase4\n" +
            "(\n" +
            "   Table PersonTable4(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)\n" +
            "   Table FirmTable4(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join PersonFirm4(PersonTable4.firmId = FirmTable4.firmId)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database a::PersonFirmDatabase5\n" +
            "(\n" +
            "   Table PersonTable5(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)\n" +
            "   Table FirmTable5(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join PersonFirm5(PersonTable5.firmId = FirmTable5.firmId)\n" +
            ")\n";

    @Test
    public void testValidNoStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping2\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping2\n" +
                "(  \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidDirectStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping23[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping23\n" +
                "(  \n" +
                "   include a::PersonFirmMapping3\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm3 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person3: [PersonFirmDatabase3]@PersonFirm3,\n" +
                "         firm3: [PersonFirmDatabase3]@PersonFirm3\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping3\n" +
                "(  \n" +
                "   Person3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]PersonTable3)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]FirmTable3)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidDoubleStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping2[PersonFirmDatabase2->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping2\n" +
                "(  \n" +
                "   include a::PersonFirmMapping3[PersonFirmDatabase3->PersonFirmDatabase2]\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping3\n" +
                "( \n" +
                "   Person3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]PersonTable3)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]FirmTable3)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm3 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person3: [PersonFirmDatabase3]@PersonFirm3,\n" +
                "         firm3: [PersonFirmDatabase3]@PersonFirm3\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidNestedStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping23[PersonFirmDatabase3->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping23\n" +
                "(  \n" +
                "   include a::PersonFirmMapping3\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping3\n" +
                "( \n" +
                "   Person3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]PersonTable3)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm3: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase3]FirmTable3)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm3 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person3: [PersonFirmDatabase3]@PersonFirm3,\n" +
                "         firm3: [PersonFirmDatabase3]@PersonFirm3\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidHybridStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping234\n" +
                "(  \n" +
                "   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping45\n" +
                "( \n" +
                "   Person4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]PersonTable4)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]FirmTable4)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm4 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person4: [PersonFirmDatabase4]@PersonFirm4,\n" +
                "         firm4: [PersonFirmDatabase4]@PersonFirm4\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Person5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]PersonTable5)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]FirmTable5)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm5 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person5: [PersonFirmDatabase5]@PersonFirm5,\n" +
                "         firm5: [PersonFirmDatabase5]@PersonFirm5\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testInValidDirectStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping234[PersonFirmDatabase5->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping234\n" +
                "(  \n" +
                "   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping45\n" +
                "( \n" +
                "   Person4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]PersonTable4)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]FirmTable4)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm4 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person4: [PersonFirmDatabase4]@PersonFirm4,\n" +
                "         firm4: [PersonFirmDatabase4]@PersonFirm4\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Person5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]PersonTable5)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]FirmTable5)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm5 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person5: [PersonFirmDatabase5]@PersonFirm5,\n" +
                "         firm5: [PersonFirmDatabase5]@PersonFirm5\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyInvalidSubstitution(mappingCode);
    }

    @Test
    public void testInValidDoubleStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase5->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping234\n" +
                "(  \n" +
                "   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping45\n" +
                "( \n" +
                "   Person4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]PersonTable4)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]FirmTable4)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm4 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person4: [PersonFirmDatabase4]@PersonFirm4,\n" +
                "         firm4: [PersonFirmDatabase4]@PersonFirm4\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Person5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]PersonTable5)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]FirmTable5)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm5 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person5: [PersonFirmDatabase5]@PersonFirm5,\n" +
                "         firm5: [PersonFirmDatabase5]@PersonFirm5\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyInvalidSubstitution(mappingCode);
    }

    @Test
    public void testInValidNestedStoreSubstitution()
    {
        String mappingCode = "###Mapping\n" +
                "import a::*;\n" +
                "\n" +
                "Mapping a::PersonFirmMapping1\n" +
                "(  \n" +
                "   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase5->PersonFirmDatabase1]\n" +
                "   \n" +
                "   Person1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]PersonTable1)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm1: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase1]FirmTable1)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm1 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person1: [PersonFirmDatabase1]@PersonFirm1,\n" +
                "         firm1: [PersonFirmDatabase1]@PersonFirm1\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping234\n" +
                "(  \n" +
                "   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]\n" +
                "   \n" +
                "   Person2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]PersonTable2)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm2: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase2]FirmTable2)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm2 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person2: [PersonFirmDatabase2]@PersonFirm2,\n" +
                "         firm2: [PersonFirmDatabase2]@PersonFirm2\n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping a::PersonFirmMapping45\n" +
                "( \n" +
                "   Person4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]PersonTable4)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm4: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase4]FirmTable4)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm4 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person4: [PersonFirmDatabase4]@PersonFirm4,\n" +
                "         firm4: [PersonFirmDatabase4]@PersonFirm4\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Person5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]PersonTable5)\n" +
                "      (\n" +
                "        personId: personId,\n" +
                "        personName: personName,\n" +
                "        firmId: firmId\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Firm5: Relational\n" +
                "   {\n" +
                "      scope([PersonFirmDatabase5]FirmTable5)\n" +
                "      (\n" +
                "        firmId: firmId,\n" +
                "        firmName: firmName\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   PersonFirm5 : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person5: [PersonFirmDatabase5]@PersonFirm5,\n" +
                "         firm5: [PersonFirmDatabase5]@PersonFirm5\n" +
                "      )\n" +
                "   }\n" +
                ")\n";

        this.verifyInvalidSubstitution(mappingCode);
    }

    private void verifyValidSubstitution(String mappingCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingCode)
                        .compile()
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    private void verifyInvalidSubstitution(String mappingCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingCode)
                        .compileWithExpectedCompileFailure("Store Substitution Error in mapping [a::PersonFirmMapping1] as [a::PersonFirmDatabase5] does not exist in included mapping [a::PersonFirmMapping234]", MAPPING_SOURCE_ID, 6, 15)
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
