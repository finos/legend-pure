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

package org.finos.legend.pure.m2.dsl.mapping;

import org.finos.legend.pure.m3.tests.AbstractImportGroupsTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestImportGroups_Mapping extends AbstractImportGroupsTest
{
    @BeforeClass
    public static void setUp()
    {
        initialize("platform_dsl_mapping");
    }

    @Test
    public void testOneSectionNoImports()
    {
        String sourceId = "/platform/mapping/mappingTestNoImports.pure";
        testImportGroups(
                sourceId,
                "###Mapping\n" +
                        "Mapping meta::test::TestMapping1\n" +
                        "(\n" +
                        ")\n",
                importGroup("import__platform_mapping_mappingTestNoImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_mapping_mappingTestNoImports_pure_2", sourceId, 1, 0, 1, 0)
        );
    }

    @Test
    public void testOneSectionWithImports()
    {
        String sourceId = "/platform/mapping/mappingTestWithImports.pure";
        testImportGroups(
                sourceId,
                "###Mapping\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Mapping meta::test::TestMapping1\n" +
                        "(\n" +
                        ")\n",
                importGroup("import__platform_mapping_mappingTestWithImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_mapping_mappingTestWithImports_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testManySections()
    {
        String sourceId = "/platform/mapping/mappingTestWithManySections.pure";
        testImportGroups(
                sourceId,
                "###Mapping\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Mapping meta::test::TestMapping1\n" +
                        "(\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Mapping meta::test::TestMapping2\n" +
                        "(\n" +
                        ")\n",
                importGroup("import__platform_mapping_mappingTestWithManySections_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_mapping_mappingTestWithManySections_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types"),
                importGroup("import__platform_mapping_mappingTestWithManySections_pure_3", sourceId, 10, 1, 10, 38, "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testMixedSections()
    {
        String sourceId = "/platform/mapping/mappingTestWithMixedSections.pure";
        testImportGroups(
                sourceId,
                "###Mapping\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Mapping meta::test::TestMapping1\n" +
                        "(\n" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Mapping meta::test::TestMapping2\n" +
                        "(\n" +
                        ")\n",
                importGroup("import__platform_mapping_mappingTestWithMixedSections_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_mapping_mappingTestWithMixedSections_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types"),
                importGroup("import__platform_mapping_mappingTestWithMixedSections_pure_3", sourceId, 10, 1, 10, 44, "meta::pure::metamodel::constraints"),
                importGroup("import__platform_mapping_mappingTestWithMixedSections_pure_4", sourceId, 17, 1, 17, 38, "meta::pure::metamodel::types")
        );
    }
}
