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

import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestIncludedMappingOwnerUnloaderUnbind extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MAIN_SOURCE_ID = "main.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MAIN_SOURCE_CODE = "###Pure\n" +
            "import test::*;\n" +
            "\n" +
            "Class test::Vehicle\n" +
            "{\n" +
            "   vehicleId : Integer[1];\n" +
            "   vehicleName : String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::RoadVehicle extends Vehicle\n" +
            "{\n" +
            "   \n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database test::MainDatabase\n" +
            "(\n" +
            "   Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20))\n" +
            "   Filter VehicleFilter(VehicleTable.vehicleId = 1)\n" +
            ")\n" +
            "\n" +
            "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::RoadVehicleMapping\n" +
            "(   \n" +
            "   include MainMapping\n" +
            "   \n" +
            "   RoadVehicle extends [test_Vehicle]: Relational\n" +
            "   {\n" +
            "      \n" +
            "   }\n" +
            ")\n";

    private static final String TEST_V1_SOURCE_CODE = "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::MainMapping\n" +
            "(   \n" +
            "   Vehicle: Relational\n" +
            "   {\n" +
            "      vehicleId : [MainDatabase]VehicleTable.vehicleId,\n" +
            "      vehicleName : [MainDatabase]VehicleTable.vehicleName\n" +
            "   }\n" +
            ")\n";

    private static final String TEST_V2_SOURCE_CODE = "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::MainMapping\n" +
            "(   \n" +
            "   Vehicle[newId]: Relational\n" +
            "   {\n" +
            "      vehicleId : [MainDatabase]VehicleTable.vehicleId,\n" +
            "      vehicleName : [MainDatabase]VehicleTable.vehicleName\n" +
            "   }\n" +
            ")\n";

    @Test
    public void testIncludedMappingUnloaderUnbind()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .createInMemorySource(TEST_SOURCE_ID, TEST_V1_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(TEST_SOURCE_ID, TEST_V2_SOURCE_CODE)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [test_RoadVehicle]", MAIN_SOURCE_ID, 29, 4)
                        .updateSource(TEST_SOURCE_ID, TEST_V1_SOURCE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
