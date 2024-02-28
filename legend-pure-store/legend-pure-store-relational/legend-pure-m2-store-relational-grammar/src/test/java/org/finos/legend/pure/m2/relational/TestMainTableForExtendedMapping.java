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

import org.eclipse.collections.api.block.predicate.Predicate2;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.junit.Assert;
import org.junit.Test;

public class TestMainTableForExtendedMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final Predicate2<SetImplementation, String> CLASS_MAPPING_BY_ID = new Predicate2<SetImplementation, String>()
    {
        @Override
        public boolean accept(SetImplementation classMapping, String id)
        {
            return classMapping._id().equals(id);
        }
    };

    private static final String MAIN_SOURCE_ID = "main.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MAIN_SOURCE_CODE = "###Pure\n" +
            "import test::*;\n" +
            "\n" +
            "Class test::Vehicle\n" +
            "{\n" +
            "   vehicleId : Integer[1];\n" +
            "   vehicleName : String[1];\n" +
            "   vehicleType : String[1];\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database test::VehicleDatabase\n" +
            "(\n" +
            "   Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20), vehicleType VARCHAR(20))\n" +
            ")\n" +
            "\n" +
            "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::VehicleMapping\n" +
            "(   \n" +
            "   Vehicle: Relational\n" +
            "   {\n" +
            "      vehicleId   : [VehicleDatabase]VehicleTable.vehicleId,\n" +
            "      vehicleName : [VehicleDatabase]VehicleTable.vehicleName,\n" +
            "      vehicleType : [VehicleDatabase]VehicleTable.vehicleType\n" +
            "   }\n" +
            ")\n";

    @Test
    public void testStableMainTableForSimpleMappingExtends()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n";
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForSimpleExtendedMapping()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n";

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testStableMainTableForNestedMappingExtends()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class test::Bicycle extends RoadVehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BicycleMapping\n" +
                "(   \n" +
                "   include RoadVehicleMapping\n" +
                "   \n" +
                "   Bicycle extends [test_RoadVehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n";
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForNestedExtendedMapping()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class test::Bicycle extends RoadVehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BicycleMapping\n" +
                "(   \n" +
                "   include RoadVehicleMapping\n" +
                "   \n" +
                "   Bicycle extends [test_RoadVehicle]: Relational\n" +
                "   {\n" +
                "   }\n" +
                ")\n";

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Mapping bicycleMapping = (Mapping)this.runtime.getCoreInstance("test::BicycleMapping");
        RootRelationalInstanceSetImplementation bicycleSetImplementation = (RootRelationalInstanceSetImplementation)bicycleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Bicycle").getFirst();

        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());

        Assert.assertEquals(roadVehicleSetImplementation._mainTableAlias()._relationalElement(), bicycleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertEquals(roadVehicleSetImplementation._mainTableAlias()._database(), bicycleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testUserDefinedMainTableNotAllowedForExtendedMapping()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "       ~mainTable[MainDatabase]VehicleTable\n" +
                "   }\n" +
                ")\n";

        this.verifyInValidMainTableForExtendedMapping("Cannot specify main table explicitly for extended mapping [test_RoadVehicle]", testSourceCode, 16, 4);
    }

    @Test
    public void testStableMainTableForSimpleExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::RoadVehicleDatabase\n" +
                "(\n" +
                "   include test::VehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "      vehicleName : concat(\'roadVehicle_\', [RoadVehicleDatabase]VehicleTable.vehicleName)\n" +
                "   }\n" +
                ")\n";
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForSimpleExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::RoadVehicleDatabase\n" +
                "(\n" +
                "   include test::VehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "      vehicleName : concat(\'roadVehicle_\', [RoadVehicleDatabase]VehicleTable.vehicleName)\n" +
                "   }\n" +
                ")\n";

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertNotEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testStableMainTableForNestedExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class test::Bicycle extends RoadVehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::RoadVehicleDatabase\n" +
                "(\n" +
                "   include test::VehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "Database test::BicycleDatabase\n" +
                "(\n" +
                "   include test::RoadVehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "      vehicleName : concat(\'roadVehicle_\', [RoadVehicleDatabase]VehicleTable.vehicleName)\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BicycleMapping\n" +
                "(   \n" +
                "   include RoadVehicleMapping[RoadVehicleDatabase->BicycleDatabase]\n" +
                "   \n" +
                "   Bicycle extends [test_RoadVehicle]: Relational\n" +
                "   {\n" +
                "      vehicleType : [BicycleDatabase]VehicleTable.vehicleType\n" +
                "   }\n" +
                ")\n";
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForNestedExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class test::Bicycle extends RoadVehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::RoadVehicleDatabase\n" +
                "(\n" +
                "   include test::VehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "Database test::BicycleDatabase\n" +
                "(\n" +
                "   include test::RoadVehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "      vehicleName : concat(\'roadVehicle_\', [RoadVehicleDatabase]VehicleTable.vehicleName)\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BicycleMapping\n" +
                "(   \n" +
                "   include RoadVehicleMapping[RoadVehicleDatabase->BicycleDatabase]\n" +
                "   \n" +
                "   Bicycle extends [test_RoadVehicle]: Relational\n" +
                "   {\n" +
                "      vehicleType : [BicycleDatabase]VehicleTable.vehicleType\n" +
                "   }\n" +
                ")\n";

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Mapping bicycleMapping = (Mapping)this.runtime.getCoreInstance("test::BicycleMapping");
        RootRelationalInstanceSetImplementation bicycleSetImplementation = (RootRelationalInstanceSetImplementation)bicycleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Bicycle").getFirst();

        Assert.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertNotEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());

        Assert.assertEquals(roadVehicleSetImplementation._mainTableAlias()._relationalElement(), bicycleSetImplementation._mainTableAlias()._relationalElement());
        Assert.assertNotEquals(roadVehicleSetImplementation._mainTableAlias()._database(), bicycleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testInvalidMainTableForExtendedMappingWithoutStoreSubstitution()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::RoadVehicle extends Vehicle\n" +
                "{\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::RoadVehicleDatabase\n" +
                "(\n" +
                "   include test::VehicleDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::RoadVehicleMapping\n" +
                "(   \n" +
                "   include VehicleMapping\n" +
                "   \n" +
                "   RoadVehicle extends [test_Vehicle]: Relational\n" +
                "   {\n" +
                "      vehicleName : concat(\'roadVehicle_\', [RoadVehicleDatabase]VehicleTable.vehicleName)\n" +
                "   }\n" +
                ")\n";
        this.verifyInValidMainTableForExtendedMapping("Can't find the main table for class 'RoadVehicle'. Inconsistent database definitions for the mapping",
                testSourceCode, 22, 4);
    }

    private void verifyValidMainTableForExtendedMapping(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
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

    private void verifyInValidMainTableForExtendedMapping(String errorMessage, String testSourceCode, int line, int column)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compileWithExpectedCompileFailure(errorMessage, TEST_SOURCE_ID, line, column)
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
