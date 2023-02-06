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
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestPureRuntimeInlineEmbeddedMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String INITIAL_DATA = "import other::*;\n" +
            "Class other::Person\n" +
            "{\n" +
            "    name:String[1];\n" +
            "    firm:Firm[1];\n" +
            "}\n" +
            "Class other::Address\n" +
            "{\n" +
            "    name:String[1];\n" +
            "}\n" +
            "Class other::Firm\n" +
            "{\n" +
            "    legalName:String[1];\n" +
            "}\n" ;


    private static final String STORE =
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
                    ")\n" ;


    private static final String INITIAL_MAPPING =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        ) Inline [firm1] \n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_INVALID_INLINE =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        ) Inline [firm2] \n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_DELETED1_INLINE =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_DELETED_INLINE =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm () Inline[firm1] \n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_EMPTY_INLINE =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        ) Inline [] \n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_REMOVE_INLINE_KEYWORD =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        )\n" +
                    "    }\n" +
                    ")\n";


    private static final String MAPPING_CHANGE_INLINE_SETID =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Firm[firmNew]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        ) Inline [firm1] \n" +
                    "    }\n" +
                    ")\n";

    private static final String INITIAL_MAPPING_CHANGE_TO_EMBEDDED =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +

                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "            legalName : [db]employeeFirmDenormTable.legalName"+
                    "        )  \n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING_INVALID_TARGET_TYPE =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Address[address1]: Relational\n" +
                    "    {\n" +
                    "       name : [db]employeeFirmDenormTable.address1\n" +
                    "    }\n" +
                    "    Firm[firm1]: Relational\n" +
                    "    {\n" +
                    "       legalName : [db]employeeFirmDenormTable.name\n" +
                    "    }\n" +
                    "    Person[alias1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeFirmDenormTable.name,\n" +
                    "        firm(\n" +
                    "        ) Inline [address1] \n" +
                    "    }\n" +
                    ")\n";


    @Test
    public void testCreateAndDeleteInlineEmbeddedMappingFile()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("source4.pure", INITIAL_MAPPING)
                        .compile()
                        .deleteSource("source4.pure")
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCreateAndDeleteInlineEmbeddedMappingSameFile()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                        Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_DELETED1_INLINE)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeRootIDForInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_CHANGE_INLINE_SETID)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm1 does not exists.", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeInlineTargetMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_INVALID_INLINE)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm2 does not exists.", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testDeleteInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_DELETED_INLINE)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm1 does not exists.", "source4.pure", 9, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testEmptyInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_EMPTY_INLINE)
                        .compileWithExpectedParserFailure("expected: a valid identifier text; found: ']'", "source4.pure", 14, 19)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testRemoveInlineKeyword()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_REMOVE_INLINE_KEYWORD)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' mapping has not inline set defined, please use: firm() Inline[setid].", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeInlineEmbeddedMappingSameFileToNormalEmbedded()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", INITIAL_MAPPING_CHANGE_TO_EMBEDDED)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeToInvalidTarget()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_INVALID_TARGET_TYPE)
                        .compileWithExpectedCompileFailure("Mapping Error! The inlineSetImplementationId 'address1' is implementing the class 'Address' which is not a subType of 'Firm' (return type of the mapped property 'firm')", "source4.pure", 17, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }
}
