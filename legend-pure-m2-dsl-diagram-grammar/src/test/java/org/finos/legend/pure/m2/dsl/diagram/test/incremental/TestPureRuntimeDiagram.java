// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.diagram.test.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeDiagram extends AbstractPureTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;
    private static final String TEST_MODEL_SOURCE_ID = "testModel.pure";
    private static final String TEST_DIAGRAM_SOURCE_ID = "testDiagram.pure";
    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(
            TEST_MODEL_SOURCE_ID,
            "import model::test::*;\n" +
            "Class model::test::A\n" +
            "{\n" +
            "  prop:model::test::B[0..1];\n" +
            "}\n" +
            "Class model::test::B extends A {}\n" +
            "Association model::test::A2B\n" +
            "{\n" +
            "  a : A[1];\n" +
            "  b : B[*];\n" +
            "}\n",
            TEST_DIAGRAM_SOURCE_ID,
            "###Diagram\n" +
            "import model::test::*;" +
            "\n" +
            "Diagram model::test::TestDiagram(width=5000.3, height=2700.6)\n" +
            "{\n" +
            "    TypeView A(type=model::test::A, stereotypesVisible=true, attributesVisible=true,\n" +
            "               attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
            "               color=#FFFFCC, lineWidth=1.0,\n" +
            "               position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
            "    TypeView B(type=model::test::B, stereotypesVisible=true, attributesVisible=true,\n" +
            "               attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
            "               color=#FFFFCC, lineWidth=1.0,\n" +
            "               position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
            "    AssociationView A2B(association=model::test::A2B, stereotypesVisible=true, nameVisible=false,\n" +
            "                        color=#000000, lineWidth=1.0,\n" +
            "                        lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
            "                        label='A to B',\n" +
            "                        source=A,\n" +
            "                        target=B,\n" +
            "                        sourcePropertyPosition=(132.5, 76.2),\n" +
            "                        sourceMultiplicityPosition=(132.5, 80.0),\n" +
            "                        targetPropertyPosition=(155.2, 76.2),\n" +
            "                        targetMultiplicityPosition=(155.2, 80.0))\n" +
            "    PropertyView A_prop(property=A.prop, stereotypesVisible=true, nameVisible=false,\n" +
            "                        color=#000000, lineWidth=1.0,\n" +
            "                        lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
            "                        label='A.prop',\n" +
            "                        source=A,\n" +
            "                        target=B,\n" +
            "                        propertyPosition=(132.5, 76.2),\n" +
            "                        multiplicityPosition=(132.5, 80.0))\n" +
            "    GeneralizationView B_A(color=#000000, lineWidth=1.0,\n" +
            "                           lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
            "                           label='',\n" +
            "                           source=B,\n" +
            "                           target=A)\n" +
            "}\n"
    );

    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_DIAGRAM_SOURCE_ID);
        runtime.delete(TEST_MODEL_SOURCE_ID);
    }

    @Test
    public void testPureRuntimeDiagram_UnloadModel() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_MODEL_SOURCE_ID);
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureRuntimeDiagram_UnloadDiagram() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(TEST_SOURCES)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(TEST_DIAGRAM_SOURCE_ID)
                        .compile()
                        .createInMemorySource(TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID))
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeDiagram_LoadUnloadDiagram() throws Exception
    {
        this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID)));
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID)));
            this.runtime.compile();

            this.runtime.delete(TEST_DIAGRAM_SOURCE_ID);
            this.runtime.compile();

            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }
}
