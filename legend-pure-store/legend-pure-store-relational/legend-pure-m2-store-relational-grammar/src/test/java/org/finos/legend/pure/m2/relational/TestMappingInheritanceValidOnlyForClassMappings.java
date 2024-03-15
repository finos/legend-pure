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

public class TestMappingInheritanceValidOnlyForClassMappings extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MODEL_SOURCE_CODE = "###Pure\n" +
            "import test::*;\n" +
            "\n" +
            "Class test::Person\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   vehicleId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Vehicle\n" +
            "{\n" +
            "   vehicleId : Integer[1];\n" +
            "   vehicleName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association test::PersonVehicle\n" +
            "{\n" +
            "   person : Person[1];\n" +
            "   vehicles : Vehicle[*];\n" +
            "}\n";

    private static final String STORE_SOURCE_CODE = "###Relational\n" +
            "Database test::MainDatabase\n" +
            "(\n" +
            "   Table PersonTable(personId INT PRIMARY KEY, personName VARCHAR(20), vehicleId INT)\n" +
            "   Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20))\n" +
            "   Join PersonVehicle(PersonTable.vehicleId = VehicleTable.vehicleId)\n" +
            ")\n";

    private static final String MAPPING_SOURCE_CODE = "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::MainMapping\n" +
            "(  \n" +
            "   Person: Relational\n" +
            "   {\n" +
            "      scope([MainDatabase]PersonTable)\n" +
            "      (\n" +
            "        personId: personId,\n" +
            "        personName: personName,\n" +
            "        vehicleId: vehicleId\n" +
            "      )\n" +
            "   }\n" +
            "   \n" +
            "   Vehicle: Relational\n" +
            "   {\n" +
            "      scope([MainDatabase]VehicleTable)\n" +
            "      (\n" +
            "        vehicleId: vehicleId,\n" +
            "        vehicleName: vehicleName\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    @Test
    public void testMappingInheritanceValidForClassMapping()
    {
        String testSourceCode = "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   include test::MainMapping\n" +
                "   \n" +
                "   Person[person1] extends [test_Person] : Relational\n" +
                "   {\n" +
                "      \n" +
                "   }\n" +
                ")\n";
        this.verifyValidMappingInheritance(testSourceCode);
    }

    @Test
    public void testMappingInheritanceInValidForAssociationMapping()
    {
        String testSourceCode = "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   include test::MainMapping\n" +
                "   \n" +
                "   PersonVehicle extends [test_Person] : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         person: [MainDatabase]@PersonVehicle,\n" +
                "         vehicles: [MainDatabase]@PersonVehicle\n" +
                "      )\n" +
                "   }\n" +
                ")\n";
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Association Mappings.", 10, 7);
    }

    @Test
    public void testMappingInheritanceInValidForOperationMapping()
    {
        String testSourceCode = "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   include test::MainMapping\n" +
                "   \n" +
                "   *Person extends [test_Person] : Operation\n" +
                "   {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(test_Person, test_Firm)\n" +
                "   }\n" +
                ")\n";
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Operation Mappings.", 8, 36);
    }

    @Test
    public void testMappingInheritanceInValidForModelToModelMapping()
    {
        String testSourceCode = "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   include MainMapping\n" +
                "   \n" +
                "   Person extends [test_Firm] : Pure\n" +
                "   {\n" +
                "      ~src Firm\n" +
                "      personId : $src.firmId,\n" +
                "      personName : $src.firmName,\n" +
                "      firmId : $src.firmId,\n" +
                "   }\n" +
                ")\n";
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Model to Model Pure Mappings.", 8, 33);
    }

    private void verifyValidMappingInheritance(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .createInMemorySource(MAPPING_SOURCE_ID, MAPPING_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compile()
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    private void verifyInValidMappingInheritance(String testSourceCode, String errorMessage, int line, int column)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .createInMemorySource(MAPPING_SOURCE_ID, MAPPING_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compileWithExpectedParserFailure(errorMessage, TEST_SOURCE_ID, line, column)
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
