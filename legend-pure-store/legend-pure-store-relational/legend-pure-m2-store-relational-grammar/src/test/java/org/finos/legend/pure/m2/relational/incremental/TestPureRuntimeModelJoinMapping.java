// Copyright 2024 Goldman Sachs
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

public class TestPureRuntimeModelJoinMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    public static String model =
            "Class Firm\n" +
                    "{\n" +
                    "   id : Integer[1];\n" +
                    "   legalName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class Person\n" +
                    "{\n" +
                    "   firmId : Integer[1];\n" +
                    "   lastName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Association Firm_Person\n" +
                    "{\n" +
                    "   firm : Firm[1];\n" +
                    "   employees : Person[*];\n" +
                    "}\n";

    public static String coreMapping =
            "   Firm[f1] : Relational\n" +
                    "   {\n" +
                    "      +id:Integer[1] : [db]FirmTable.id,\n" +
                    "      legalName : [db]FirmTable.legal_name\n" +
                    "   }\n" +
                    "   \n" +
                    "   Person[e] : Relational\n" +
                    "   {\n" +
                    "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                    "      lastName : [db]PersonTable.lastName\n" +
                    "   }\n";

    public static String assoMapping =
            "   Firm_Person : ModelJoin\n" +
                    "   {\n" +
                    "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                    "   }\n";

    public static String initialMapping = "###Mapping\nMapping FirmMapping\n(" + coreMapping + ")";

    public static String mappingWithAssociation = "###Mapping\nMapping FirmMapping\n(" + coreMapping + assoMapping + ")\n";

    public static String baseMapping = "###Mapping\nMapping ModelMapping\n(" + coreMapping + ")\n";

    public static String mainMapping = "###Mapping\nMapping FirmMapping\n(\ninclude ModelMapping\n" + assoMapping + ")\n";

    public static String relational =
            "###Relational\n" +
                    "Database db\n" +
                    "(\n" +
                    "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                    "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                    ")";

    @Test
    public void testCreateAndDeleteModelJoinRelational()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", initialMapping, "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", mappingWithAssociation).compile().updateSource("source3.pure", initialMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteModelJoinRelationalWithInclude()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source5.pure").compile().createInMemorySource("source5.pure", mainMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinWithRelationalClassMappings()
    {
        String fullSource = model +
                "###Relational\n" +
                "Database db\n" +
                "(\n" +
                "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping FirmMapping\n" +
                "(\n" +
                coreMapping +
                assoMapping +
                ")\n";
        runtime.createInMemorySource("source1.pure", fullSource);
        runtime.compile();
    }
}
