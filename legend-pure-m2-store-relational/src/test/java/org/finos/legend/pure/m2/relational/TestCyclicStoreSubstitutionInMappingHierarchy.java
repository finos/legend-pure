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

import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.junit.Test;

public class TestCyclicStoreSubstitutionInMappingHierarchy extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";

    private static final String MODEL_SOURCE_CODE = "###Pure\n" +
            "\n" +
            "Class test::A\n" +
            "{\n" +
            "   id : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class test::C extends test::A\n" +
            "{\n" +
            "}\n";

    private static final String STORE_SOURCE_CODE = "###Relational\n" +
            "\n" +
            "Database test::ADatabase\n" +
            "(\n" +
            "   Table ATable(id INT PRIMARY KEY)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "\n" +
            "Database test::CDatabase\n" +
            "(\n" +
            "   include test::ADatabase\n" +
            ")\n";

    @Test
    public void testAcyclicStoreSubstitutionAllowedInMappingHierarchy()
    {
        String mappingSourceCode = "###Mapping\n" +
                "\n" +
                "Mapping test::AMapping\n" +
                "(      \n" +
                "   test::A : Relational\n" +
                "   {\n" +
                "      id : [test::ADatabase]ATable.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BMapping\n" +
                "(      \n" +
                "   include test::AMapping" +
                ")\n" +
                "\n" +
                "Mapping test::CMapping\n" +
                "(      \n" +
                "   include test::BMapping[test::ADatabase->test::CDatabase]\n" +
                "   test::C extends [test_A] : Relational\n" +
                "   { \n" +
                "   }\n" +
                ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingSourceCode)
                        .compile()
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testCyclicStoreSubstitutionNotAllowedInMappingHierarchy()
    {
        String mappingSourceCode = "###Mapping\n" +
                "\n" +
                "Mapping test::AMapping\n" +
                "(      \n" +
                "   test::A : Relational\n" +
                "   {\n" +
                "      id : [test::ADatabase]ATable.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::BMapping\n" +
                "(      \n" +
                "   include test::AMapping[test::ADatabase->test::CDatabase]\n" +
                ")\n" +
                "\n" +
                "Mapping test::CMapping\n" +
                "(      \n" +
                "   include test::BMapping[test::CDatabase->test::ADatabase]\n" +
                "   test::C extends [test_A] : Relational\n" +
                "   { \n" +
                "   }\n" +
                ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingSourceCode)
                        .compileWithExpectedCompileFailure("Cyclic Store Substitution for store [test::ADatabase] in mapping hierarchy", STORE_SOURCE_ID, 3, 16)
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
