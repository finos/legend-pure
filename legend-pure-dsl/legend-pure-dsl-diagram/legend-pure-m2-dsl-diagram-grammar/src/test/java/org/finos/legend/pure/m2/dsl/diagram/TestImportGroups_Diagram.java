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

package org.finos.legend.pure.m2.dsl.diagram;

import org.finos.legend.pure.m3.tests.AbstractImportGroupsTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestImportGroups_Diagram extends AbstractImportGroupsTest
{
    @BeforeClass
    public static void setUp()
    {
        initialize("platform_dsl_diagram");
    }

    @Test
    public void testOneSectionNoImports()
    {
        String sourceId = "/platform/diagram/diagramTestNoImports.pure";
        testImportGroups(
                sourceId,
                "###Diagram\n" +
                        "Diagram meta::test::TestDiagram1(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_diagram_diagramTestNoImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_diagram_diagramTestNoImports_pure_2", sourceId, 1, 0, 1, 0)
        );
    }

    @Test
    public void testOneSectionWithImports()
    {
        String sourceId = "/platform/diagram/diagramTestWithImports.pure";
        testImportGroups(
                sourceId,
                "###Diagram\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Diagram meta::test::TestDiagram1(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_diagram_diagramTestWithImports_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_diagram_diagramTestWithImports_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testManySections()
    {
        String sourceId = "/platform/diagram/diagramTestWithManySections.pure";
        testImportGroups(
                sourceId,
                "###Diagram\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Diagram meta::test::TestDiagram1(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "###Diagram\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Diagram meta::test::TestDiagram2(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_diagram_diagramTestWithManySections_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_diagram_diagramTestWithManySections_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types"),
                importGroup("import__platform_diagram_diagramTestWithManySections_pure_3", sourceId, 10, 1, 10, 38, "meta::pure::metamodel::types")
        );
    }

    @Test
    public void testMixedSections()
    {
        String sourceId = "/platform/diagram/diagramTestWithMixedSections.pure";
        testImportGroups(
                sourceId,
                "###Diagram\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Diagram meta::test::TestDiagram1(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "###Pure\n" +
                        "import meta::pure::metamodel::constraints::*;\n" +
                        "\n" +
                        "Class meta::test::TestClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "###Diagram\n" +
                        "import meta::pure::metamodel::types::*;\n" +
                        "\n" +
                        "Diagram meta::test::TestDiagram2(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "}\n",
                importGroup("import__platform_diagram_diagramTestWithMixedSections_pure_1", sourceId, 0, 0, 0, 0),
                importGroup("import__platform_diagram_diagramTestWithMixedSections_pure_2", sourceId, 2, 1, 3, 38, "meta::pure::metamodel::constraints", "meta::pure::metamodel::types"),
                importGroup("import__platform_diagram_diagramTestWithMixedSections_pure_3", sourceId, 10, 1, 10, 44, "meta::pure::metamodel::constraints"),
                importGroup("import__platform_diagram_diagramTestWithMixedSections_pure_4", sourceId, 17, 1, 17, 38, "meta::pure::metamodel::types")
        );
    }
}
