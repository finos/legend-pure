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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureModelMapping extends AbstractPureMappingTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String TEST_MODEL_SOURCE_ID1 = "testModel1.pure";
    private static final String TEST_MODEL_SOURCE_ID2 = "testModel2.pure";
    private static final String TEST_MAPPING_SOURCE_ID1 = "testMapping1.pure";
    private static final String TEST_MAPPING_SOURCE_ID2 = "testMapping2.pure";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_MODEL_SOURCE_ID1,
                    "Class Firm\n" +
                    "{\n" +
                    "  legalName : String[1];" +
                    "  other : Integer[1];\n" +
                    "  other1 : Float[1];\n" +
                    "}",
            TEST_MODEL_SOURCE_ID2,
                    "Enum MyEnum\n" +
                    "{\n" +
                    "   a,b\n" +
                    "}\n" +
                    "Class SourceFirm\n" +
                    "{\n" +
                    "  name : String[1];" +
                    "  other2 : MyEnum[1];\n" +
                    "}",
            TEST_MAPPING_SOURCE_ID1,
            "###Mapping\n" +
                    "Mapping FirmMapping\n" +
                    "(\n" +
                    "  Firm : Pure\n" +
                    "         {\n" +
                    "            legalName : ['a','b']->map(k|$k+'Yeah!')->joinStrings(',') ,\n" +
                    "            other : 1+2,\n" +
                    "            other1 : 1.0+2.0\n" +
                    "         }\n" +
                    ")",
            TEST_MAPPING_SOURCE_ID2,
            "###Mapping\n" +
                    "Mapping FirmMapping2\n" +
                    "(\n" +
                    "  Firm : Pure\n" +
                    "         {\n" +
                    "            ~src SourceFirm\n" +
                    "            ~filter $src.other2 == MyEnum.b\n" +
                    "            legalName : $src.name,\n" +
                    "            other : $src.name->length(),\n" +
                    "            other1 : 3.14\n" +
                    "         }\n" +
                    ")"
    );

    private static final ImmutableMap<String, String> TEST_SOURCES_WITH_TYPO = Maps.immutable.with(TEST_MODEL_SOURCE_ID1,
            "Class Firm\n" +
                    "{\n" +
                    "  legalNameX : String[1];\n" +
                    "  other : String[1];\n" +
                    "  other1 : String[1];\n" +
                    "}");

    private static final ImmutableMap<String, String> TEST_MAPPING_SOURCE_WITH_ERROR = Maps.immutable.with(TEST_MAPPING_SOURCE_ID1,
            "###Mapping\n" +
                    "Mapping FirmMapping\n" +
                    "(\n" +
                    "  Firm : Pure\n" +
                    "         {\n" +
                    "            legalName : ['a','b']->maXp(k|$src->toString() + 'Yeah!') ,\n" +
                    "            other : 'ok' + 'op',\n" +
                    "            other1 : ['o','e']" +
                    "         }\n" +
                    ")");

    private static final ImmutableMap<String, String> TEST_MAPPING_SOURCE_NO_SOURCE__ERROR = Maps.immutable.with(TEST_MAPPING_SOURCE_ID2,
            "###Mapping\n" +
                    "Mapping FirmMapping2\n" +
                    "(\n" +
                    "  Firm : Pure\n" +
                    "         {\n" +
                    "            legalName : $src.name\n" +
                    "         }\n" +
                    ")"
);

    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_MAPPING_SOURCE_ID1);
        runtime.delete(TEST_MAPPING_SOURCE_ID2);
        runtime.delete(TEST_MODEL_SOURCE_ID1);
        runtime.delete(TEST_MODEL_SOURCE_ID2);
        runtime.delete("modelCode.pure");
        runtime.delete("modelMappingCode.pure");
        runtime.delete("enumerationMappingCode.pure");
    }

    @Test
    public void testPureModelMapping_ModelWithDiffPropertiesShouldNotCompile() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        this.runtime.delete(TEST_MAPPING_SOURCE_ID2);
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_MODEL_SOURCE_ID1);
            try
            {
                this.runtime.createInMemoryAndCompile(TEST_SOURCES_WITH_TYPO);
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The property 'legalName' is unknown in the Element 'Firm'",
                        TEST_MAPPING_SOURCE_ID1, 6, 13, 6, 13, 6, 21, e);
            }

            this.runtime.delete(TEST_MODEL_SOURCE_ID1);
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID1, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID1)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }


    @Test
    public void testPureModelMapping_WithErrorShouldNotCompile() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_MAPPING_SOURCE_ID1);
            try
            {
                this.runtime.createInMemoryAndCompile(TEST_MAPPING_SOURCE_WITH_ERROR);
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The system can't find a match for the function: maXp(_:String[2],_:LambdaFunction<{NULL[NULL]->NULL[NULL]}>[1])",
                        TEST_MAPPING_SOURCE_ID1, 6, 36, 6, 36, 6, 39, e);
            }

            this.runtime.delete(TEST_MAPPING_SOURCE_ID1);
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MAPPING_SOURCE_ID1, TEST_SOURCES.get(TEST_MAPPING_SOURCE_ID1)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureModelMapping_WithErrorNoSourceShouldNotCompile() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_MAPPING_SOURCE_ID2);
            try
            {
                this.runtime.createInMemoryAndCompile(TEST_MAPPING_SOURCE_NO_SOURCE__ERROR);
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The variable 'src' is unknown!",
                        TEST_MAPPING_SOURCE_ID2, 6, 26, 6, 26, 6, 28, e);
            }

            this.runtime.delete(TEST_MAPPING_SOURCE_ID2);
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MAPPING_SOURCE_ID2, TEST_SOURCES.get(TEST_MAPPING_SOURCE_ID2)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureModelMapping_WithErrorWrongSourceShouldNotCompile() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_MODEL_SOURCE_ID2);
            try
            {
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "SourceFirm has not been defined!",
                        TEST_MAPPING_SOURCE_ID2, 6, 18, 6, 18, 6, 27, e);
            }

            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID2, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID2)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPropertyMappingValueSpecificationContext()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class A {a: String[1];}")
                        .createInMemorySource("s2.pure", "Class B {b: String[1];} ")
                        .createInMemorySource("s3.pure", "###Mapping\n Mapping map(A : Pure {~src B\na: $src.bc.c})")
                        .createInMemorySource("s4.pure", "Class C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile()
                ,
                new RuntimeTestScriptBuilder()
                        .updateSource("s4.pure", "Class\n C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile()
                        .updateSource("s4.pure", "Class C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPropertyMappingStabilityOnEnumerationMappingUpdation()
    {
        String modelCode = "###Pure\n" +
                "import my::*;\n" +
                "\n" +
                "Class my::SourceProduct\n" +
                "{\n" +
                "   id : Integer[1];\n" +
                "   state : String[1];\n" +
                "}\n" +
                "\n" +
                "Class my::TargetProduct\n" +
                "{\n" +
                "   id : Integer[1];\n" +
                "   state : State[1];\n" +
                "}\n" +
                "\n" +
                "Enum my::State\n" +
                "{\n" +
                "   ACTIVE,\n" +
                "   INACTIVE\n" +
                "}\n";

        String modelMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::modelMapping\n" +
                "(\n" +
                "   include enumerationMapping\n" +
                "\n" +
                "   TargetProduct : Pure\n" +
                "   {\n" +
                "      ~src SourceProduct\n" +
                "      id : $src.id,\n" +
                "      state : EnumerationMapping StateMapping : $src.state\n" +
                "   }\n" +
                ")\n";

        String enumerationMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::enumerationMapping\n" +
                "(\n" +
                "   State : EnumerationMapping StateMapping\n" +
                "   {\n" +
                "      ACTIVE : 1,\n" +
                "      INACTIVE : 0\n" +
                "   }\n" +
                ")\n";

        String updatedEnumerationMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::enumerationMapping\n" +
                "(\n" +
                "   State : EnumerationMapping StateMapping\n" +
                "   {\n" +
                "      ACTIVE : 'ACTIVE',\n" +
                "      INACTIVE : 'INACTIVE'\n" +
                "   }\n" +
                ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("modelMappingCode.pure", modelMappingCode)
                        .createInMemorySource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("enumerationMappingCode.pure", updatedEnumerationMappingCode)
                        .compile()
                        .updateSource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testPropertyMappingStabilityOnEnumerationMappingDeletion()
    {
        String modelCode = "###Pure\n" +
                "import my::*;\n" +
                "\n" +
                "Class my::SourceProduct\n" +
                "{\n" +
                "   id : Integer[1];\n" +
                "   state : String[1];\n" +
                "}\n" +
                "\n" +
                "Class my::TargetProduct\n" +
                "{\n" +
                "   id : Integer[1];\n" +
                "   state : State[1];\n" +
                "}\n" +
                "\n" +
                "Enum my::State\n" +
                "{\n" +
                "   ACTIVE,\n" +
                "   INACTIVE\n" +
                "}\n";

        String modelMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::modelMapping\n" +
                "(\n" +
                "   include enumerationMapping\n" +
                "\n" +
                "   TargetProduct : Pure\n" +
                "   {\n" +
                "      ~src SourceProduct\n" +
                "      id : $src.id,\n" +
                "      state : EnumerationMapping StateMapping : $src.state\n" +
                "   }\n" +
                ")\n";

        String enumerationMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::enumerationMapping\n" +
                "(\n" +
                "   State : EnumerationMapping StateMapping\n" +
                "   {\n" +
                "      ACTIVE : 1,\n" +
                "      INACTIVE : 0\n" +
                "   }\n" +
                ")\n";

        String updatedEnumerationMappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::enumerationMapping\n" +
                "(\n" +
                ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("modelMappingCode.pure", modelMappingCode)
                        .createInMemorySource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("enumerationMappingCode.pure", updatedEnumerationMappingCode)
                        .compileWithExpectedCompileFailure("The transformer 'StateMapping' is unknown or is not of type EnumerationMapping in the Mapping 'my::modelMapping' for property state", "modelMappingCode.pure", 12, 7)
                        .updateSource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
