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

public class TestPureRuntimeEnumerationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String TEST_ENUM_MODEL_SOURCE_ID = "testModel.pure";
    private static final String TEST_ENUMERATION_MAPPING_SOURCE_ID = "testMapping.pure";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
                    "Enum test::EmployeeType\n" +
                    "{\n" +
                    "    CONTRACT,\n" +
                    "    FULL_TIME\n" +
                    "}",
            TEST_ENUMERATION_MAPPING_SOURCE_ID,
                    "###Mapping\n" +
                    "Mapping test::employeeTestMapping\n" +
                    "(\n" +
                    "\n" +
                    "    test::EmployeeType: EnumerationMapping Foo\n" +
                    "    {\n" +
                    "        CONTRACT:  ['FTC', 'FTO'],\n" +
                    "        FULL_TIME: 'FTE'\n" +
                    "    }\n" +
                    ")\n"
    );

    private static final ImmutableMap<String, String> TEST_SOURCES_WITH_TYPO = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
                    "Enum test::EmployeeType\n" +
                    "{\n" +
                    "    CONTRCAT,\n" + //This is the TYPO
                    "    FULL_TIME\n" +
                    "}");

    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
        runtime.delete(TEST_ENUMERATION_MAPPING_SOURCE_ID);
        runtime.delete("modelCode.pure");
        runtime.delete("mappingCode.pure");
    }

    @Test
    public void testPureEnumerationMapping_EnumValueWithTypoShouldNotCompile() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            try
            {
                this.runtime.createInMemoryAndCompile(TEST_SOURCES_WITH_TYPO);
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The enum value 'CONTRACT' can't be found in the enumeration test::EmployeeType",
                        TEST_ENUMERATION_MAPPING_SOURCE_ID, 7, 9, 7, 9, 7, 16, e);
            }

            this.runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testDeleteEnumeration() throws Exception
    {
        this.runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            this.runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "test::EmployeeType has not been defined!", TEST_ENUMERATION_MAPPING_SOURCE_ID, 5, 11, 5, 11, 5, 22, e);
            }

            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPureEnumerationMapping_UnloadMapping() throws Exception
    {
        this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
        int size = this.repository.serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {

            this.runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUMERATION_MAPPING_SOURCE_ID, TEST_SOURCES.get(TEST_ENUMERATION_MAPPING_SOURCE_ID)));
            this.runtime.delete(TEST_ENUMERATION_MAPPING_SOURCE_ID);
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch at iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testDuplicateError() throws Exception
    {
        this.runtime.createInMemorySource(TEST_ENUMERATION_MAPPING_SOURCE_ID, "Enum OK {e_true,e_false}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap1(\n" +
                                                    "    OK: EnumerationMapping Foo\n" +
                                                    "    {\n" +
                                                    "        e_true:  ['FTC', 'FTO'],\n" +
                                                    "        e_false: 'FTE'\n" +
                                                    "    }\n" +
                                                    "    OK: EnumerationMapping Foo\n" +
                                                    "    {\n" +
                                                    "        e_true:  ['FTC', 'FTO'],\n" +
                                                    "        e_false: 'FTE'\n" +
                                                    "    }\n" +
                                                    ")\n");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'Foo' in mapping myMap1", 9, 5, e);
        }
    }

    @Test
    public void testStabilityOnDeletionForSimpleEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A, B\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      X : SourceEnum.A,\n" +
                "      Y : my::SourceEnum.B\n" +
                "   }\n" +
                ")\n";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForSimpleEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A, B\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      X : SourceEnum.A,\n" +
                "      Y : my::SourceEnum.B\n" +
                "   }\n" +
                ")\n";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'B' can't be found in the enumeration my::SourceEnum", "mappingCode.pure", 9, 26)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnDeletionForComplexEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A, B, C\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      X : SourceEnum.A,\n" +
                "      Y : [SourceEnum.B, my::SourceEnum.C]\n" +
                "   }\n" +
                ")\n";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForComplexEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A, B, C\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      X : SourceEnum.A,\n" +
                "      Y : [SourceEnum.B, my::SourceEnum.C]\n" +
                "   }\n" +
                ")\n";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum\n" +
                "{\n" +
                "   A, B\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   X, Y\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'C' can't be found in the enumeration my::SourceEnum", "mappingCode.pure", 9, 41)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnDeletionForHybridEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum1\n" +
                "{\n" +
                "   A, B, C, D\n" +
                "}\n" +
                "\n" +
                "Enum my::SourceEnum2\n" +
                "{\n" +
                "   P, Q, R, S\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   U, V, W, X, Y, Z\n" +
                "}\n" +
                "\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      U : SourceEnum2.P,\n" +
                "      V : my::SourceEnum2.P,\n" +
                "      W : [SourceEnum2.P, my::SourceEnum2.Q],\n" +
                "      X : [my::SourceEnum2.P, SourceEnum2.Q, SourceEnum2.S],\n" +
                "      Y : [SourceEnum2.R, SourceEnum2.S, SourceEnum2.Q],\n" +
                "      Z : SourceEnum2.Q\n" +
                "   }\n" +
                ")";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum1\n" +
                "{\n" +
                "   A, B, C, D\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   U, V, W, X, Y, Z\n" +
                "}\n" +
                "\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum2 has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForHybridEumToEnumMapping()
    {
        String modelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum1\n" +
                "{\n" +
                "   A, B, C, D\n" +
                "}\n" +
                "\n" +
                "Enum my::SourceEnum2\n" +
                "{\n" +
                "   P, Q, R, S\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   U, V, W, X, Y, Z\n" +
                "}\n" +
                "\n";

        String mappingCode = "###Mapping\n" +
                "import my::*;\n" +
                "\n" +
                "Mapping my::TestMapping\n" +
                "(\n" +
                "   TargetEnum : EnumerationMapping\n" +
                "   {\n" +
                "      U : my::SourceEnum2.P,\n" +
                "      V : SourceEnum2.P,\n" +
                "      W : [SourceEnum2.P, my::SourceEnum2.Q],\n" +
                "      X : [my::SourceEnum2.P, SourceEnum2.Q],\n" +
                "      Y : [SourceEnum2.R, SourceEnum2.Q, SourceEnum2.P],\n" +
                "      Z : SourceEnum2.P\n" +
                "   }\n" +
                ")";

        String updatedModelCode = "###Pure\n" +
                "\n" +
                "Enum my::SourceEnum1\n" +
                "{\n" +
                "   A, B, C\n" +
                "}\n" +
                "\n" +
                "Enum my::SourceEnum2\n" +
                "{\n" +
                "   P, Q\n" +
                "}\n" +
                "\n" +
                "Enum my::TargetEnum\n" +
                "{\n" +
                "   U, V, W, X, Y, Z\n" +
                "}\n" +
                "\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'R' can't be found in the enumeration my::SourceEnum2", "mappingCode.pure", 12, 24)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
