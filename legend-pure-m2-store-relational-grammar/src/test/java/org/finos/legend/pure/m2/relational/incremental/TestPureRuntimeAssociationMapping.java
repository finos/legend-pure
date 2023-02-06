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

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestPureRuntimeAssociationMapping extends AbstractPureRelationalTestWithCoreCompiled
{

    private static final String INITIAL_DATA = "import other::*;\n" +
            "\n" +
            "Class other::Person\n" +
            "{\n" +
            "    name:String[1];\n" +
            "}\n" +
            "Class other::Firm\n" +
            "{\n" +
            "    legalName:String[1];\n" +
            "}\n";

    private static final String ASSOCIATION = "import other::*;\n" +
            "Association other::Firm_Person\n" +
            "{\n" +
            "    firm:Firm[1];\n" +
            "    employees:Person[1];\n" +
            "}\n";

    private static final String STORE =
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
                    ")\n";

    private static final String INITIAL_MAPPING =
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
                    ")\n";

    private static final String MAPPING_WITH_ASSOCIATION =
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
                    ")\n";


    private static final String MAPPING1 =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping1\n" +
                    "(\n" +
                    "    Person[per1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING2 =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping2\n" +
                    "(\n" +
                    "    Firm[fir1]: Relational\n" +
                    "    {\n" +
                    "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING3 =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::subMapping3\n" +
                    "(\n" +
                    "    include mappingPackage::subMapping1\n" +
                    "    include mappingPackage::subMapping2\n" +
                    "    Firm_Person: Relational\n" +
                    "    {\n" +
                    "        AssociationMapping\n" +
                    "        (\n" +
                    "           employees[fir1,per1] : [db]@firmJoin,\n" +
                    "           firm[per1,fir1] : [db]@firmJoin\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n";

    @Test
    public void testCreateAndDeleteAssociationMappingSameFile() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                        Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_WITH_ASSOCIATION)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testDeleteAssociation() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                        Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                "source3.pure", STORE, "source4.pure", MAPPING_WITH_ASSOCIATION))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source2.pure")
                        .compileWithExpectedCompileFailure("Firm_Person has not been defined!", "source4.pure", 15, 5)
                        .createInMemorySource("source2.pure", ASSOCIATION)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteAssociationMappingWithIncludes() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                        Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                "source3.pure", STORE, "source4.pure", MAPPING1).withKeyValue("source5.pure", MAPPING2))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("mapping3.pure", MAPPING3)
                        .compile()
                        .deleteSource("mapping3.pure")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testDeleteAndRecreateStore() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                        Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                "source3.pure", STORE, "source4.pure", MAPPING1).withKeyValue("source5.pure", MAPPING2).withKeyValue("mapping3.pure", MAPPING3))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure")
                        .compileWithExpectedCompileFailure(null, null, 0, 0)
                        .createInMemorySource("source3.pure", STORE)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

}
