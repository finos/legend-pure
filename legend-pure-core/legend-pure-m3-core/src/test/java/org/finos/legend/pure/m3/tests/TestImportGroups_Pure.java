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

package org.finos.legend.pure.m3.tests;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestImportGroups_Pure extends AbstractImportGroupsTest
{
    @BeforeClass
    public static void setUp()
    {
        initialize("platform");
    }

    @Test
    public void testImplicitSectionNoImports()
    {
        String sourceId = "/platform/pure/pureTestNoImports.pure";
        testImportGroups(
                sourceId,
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_pure_pureTestNoImports_pure_1", sourceId, 0, 0, 0, 0)
        );
    }

    @Test
    public void testImplicitSectionWithImports()
    {
        String sourceId = "/platform/pure/pureTestWithImports.pure";
        testImportGroups(
                sourceId,
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_pure_pureTestWithImports_pure_1", sourceId, 1, 1, 2, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testOneSectionNoImports()
    {
        String sourceId = "/platform/pure/pureTestNoImports.pure";
        testImportGroups(
                sourceId,
                "###Pure\n" +
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_pure_pureTestNoImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_pure_pureTestNoImports_pure_2", sourceId, 1, 0, 1, 0)
        );
    }

    @Test
    public void testOneSectionWithImports()
    {
        String sourceId = "/platform/pure/pureTestWithImports.pure";
        testImportGroups(
                sourceId,
                "###Pure\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_pure_pureTestWithImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_pure_pureTestWithImports_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testManySections()
    {
        String sourceId = "/platform/pure/pureTestWithImports.pure";
        testImportGroups(
                sourceId,
                "###Pure\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass1\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "###Pure\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass2\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_pure_pureTestWithImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_pure_pureTestWithImports_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types"),
                importGroup("import__platform_pure_pureTestWithImports_pure_3", sourceId, 10, 1, 10, 38, "meta::pure::metamodel::types")
        );
    }
}
