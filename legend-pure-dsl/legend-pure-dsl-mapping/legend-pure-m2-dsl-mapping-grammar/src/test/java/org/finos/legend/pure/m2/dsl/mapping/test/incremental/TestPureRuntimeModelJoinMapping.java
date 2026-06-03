// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.test.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m2.dsl.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class TestPureRuntimeModelJoinMapping extends AbstractPureMappingTestWithCoreCompiled
{
    public static String model =
            "Class Firm\n" +
                    "{\n" +
                    "   id : String[1];\n" +
                    "   legalName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class Person\n" +
                    "{\n" +
                    "   firmId : String[1];\n" +
                    "   lastName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Association Firm_Person\n" +
                    "{\n" +
                    "   firm : Firm[1];\n" +
                    "   employees : Person[*];\n" +
                    "}\n";

    public static String relational =
            "###Pure\n" +
                    "Class SrcFirm\n" +
                    "{\n" +
                    "   _id : String[1];\n" +
                    "   _legalName : String[1];\n" +
                    "}\n" +
                    "Class SrcPerson\n" +
                    "{\n" +
                    "   _firmId : String[1];\n" +
                    "   _lastName : String[1];\n" +
                    "}\n";

    public static String coreMapping =
            "   Firm[f1] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcFirm\n" +
                    "      id : $src._id,\n" +
                    "      legalName : $src._legalName\n" +
                    "   }\n" +
                    "   \n" +
                    "   Person[e] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcPerson\n" +
                    "      firmId : $src._firmId,\n" +
                    "      lastName : $src._lastName\n" +
                    "   }\n";

    public static String assoMapping =
            "   Firm_Person : ModelJoin\n" +
                    "   {\n" +
                    "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                    "   }\n";

    public static String initialMapping = "###Mapping\nMapping FirmMapping\n(" + coreMapping + ")\n";

    public static String mappingWithAssociation = "###Mapping\nMapping FirmMapping\n(" + coreMapping + assoMapping + ")\n";

    public static String baseMapping = "###Mapping\nMapping ModelMapping\n(" + coreMapping + ")\n";

    public static String baseMappingEmpty = "###Mapping\nMapping ModelMapping\n()\n";

    public static String mainMapping = "###Mapping\nMapping FirmMapping\n(\ninclude ModelMapping\n" + assoMapping + ")\n";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("source1.pure");
        runtime.delete("source3.pure");
        runtime.delete("source4.pure");
        runtime.delete("source5.pure");
        runtime.delete("functionSourceId.pure");
        runtime.delete("a_model.pure");
        runtime.delete("z_assoc.pure");
        runtime.delete("m_mapping.pure");
    }

    @Test
    public void testCreateAndDeleteModelJoinMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", initialMapping, "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", mappingWithAssociation).compile().updateSource("source3.pure", initialMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteModelJoinMappingWithInclude()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source5.pure").compile().createInMemorySource("source5.pure", mainMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinMappingErrorDeleteParent()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", baseMappingEmpty).compileWithExpectedCompileFailure("ModelJoin: no class mapping found for type 'Firm' in mapping", "source5.pure", 5, 4).updateSource("source3.pure", baseMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinMappingErrorDeleteAssociation()
    {
        String modelWithoutAssociation =
                "Class Firm\n" +
                        "{\n" +
                        "   id : String[1];\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Person\n" +
                        "{\n" +
                        "   firmId : String[1];\n" +
                        "   lastName : String[1];\n" +
                        "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", mappingWithAssociation, "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source1.pure", modelWithoutAssociation).compileWithExpectedCompileFailure("Firm_Person has not been defined!", "source3.pure", 16, 4).updateSource("source1.pure", model).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinMappingAddSubtypeClassMapping()
    {
        String modelWithSubtype =
                "###Pure\n" +
                "Class SrcFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "Class SrcPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "   _lastName : String[1];\n" +
                "}\n" +
                "Class SrcEmployee extends SrcPerson\n" +
                "{\n" +
                "}\n";

        String modelExtended =
                "Class Firm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class Person\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class Employee extends Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association Firm_Person\n" +
                "{\n" +
                "   firm : Firm[1];\n" +
                "   employees : Person[*];\n" +
                "}\n";

        String coreMappingWithSubtype =
                "   Firm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcFirm\n" +
                "      id : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   \n" +
                "   Person[e] : Pure\n" +
                "   {\n" +
                "      ~src SrcPerson\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   Employee[emp1] : Pure\n" +
                "   {\n" +
                "      ~src SrcEmployee\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n";

        String mappingWithAssociationAndSubtype = "###Mapping\nMapping FirmMapping\n(" + coreMappingWithSubtype + assoMapping + ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", mappingWithAssociation, "source4.pure", relational))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", modelExtended)
                        .updateSource("source4.pure", modelWithSubtype)
                        .updateSource("source3.pure", mappingWithAssociationAndSubtype)
                        .compile()
                        .updateSource("source1.pure", model)
                        .updateSource("source4.pure", relational)
                        .updateSource("source3.pure", mappingWithAssociation)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    // ---------------------------------------------------------------------
    // Delete-and-reload sanity tests.
    //
    // For each source (or pair of sources) in the fixture, delete it,
    // recompile (expecting a transient compile failure where the deletion
    // breaks a dependency), recreate the source, recompile, and assert that
    // the runtime state is byte-identical to the pre-delete snapshot.
    // RuntimeVerifier runs the script three times by default, so any
    // per-iteration drift in the reachable graph (classifier counts,
    // serialised repo bytes, source count, context size) is caught.
    //
    // ---------------------------------------------------------------------

    private static final ImmutableMap<String, String> BASIC_SOURCES = Maps.immutable.with(
            "source1.pure", model,
            "source3.pure", mappingWithAssociation,
            "source4.pure", relational);

    private static final ImmutableMap<String, String> INCLUDE_SOURCES = Maps.immutable.with(
            "source1.pure", model,
            "source3.pure", baseMapping,
            "source4.pure", relational,
            "source5.pure", mainMapping);

    // A trivial probe function that touches the mapping (via classMappings->size()) and the
    // association property mappings (via associationMappings->size()). Its presence guarantees
    // that deleting ANY source in either fixture cascades into a compile failure on the
    // post-delete compile step — which is what
    // RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable requires.
    private static final String FUNCTION_SOURCE_ID = "functionSourceId.pure";
    private static final String PROBE_FUNCTION =
            "###Pure\n" +
                    "function test::probe():Boolean[1]\n" +
                    "{\n" +
                    "    assert(2 == FirmMapping.classMappings->size(), |'');\n" +
                    "    assert(1 == FirmMapping.associationMappings->size(), |'');\n" +
                    "}\n";

    @Test
    public void testDeleteAndReloadEachSource()
    {
        deleteAndReloadEachSource(BASIC_SOURCES);
    }

    @Test
    public void testDeleteAndReloadEachSourceWithInclude()
    {
        deleteAndReloadEachSource(INCLUDE_SOURCES);
    }

    @Test
    public void testDeleteAndReloadSourcePairs()
    {
        deleteAndReloadSourcePairs(BASIC_SOURCES);
    }

    @Test
    public void testDeleteAndReloadSourcePairsWithInclude()
    {
        deleteAndReloadSourcePairs(INCLUDE_SOURCES);
    }

    private void deleteAndReloadEachSource(ImmutableMap<String, String> sources)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            try
            {
                new RuntimeTestScriptBuilder()
                        .createInMemorySources(sources)
                        .createInMemorySource(FUNCTION_SOURCE_ID, PROBE_FUNCTION)
                        .compile()
                        .run(runtime, functionExecution);

                RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                        runtime, functionExecution,
                        Lists.fixedSize.of(source),
                        Lists.fixedSize.of());
            }
            catch (Throwable t)
            {
                throw new AssertionError("Failure while delete/reload of '" + source.getOne() + "': " + t.getMessage(), t);
            }
            finally
            {
                // Reset so the next iteration starts from a clean runtime.
                setUpRuntime();
            }
        }
    }

    private void deleteAndReloadSourcePairs(ImmutableMap<String, String> sources)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            List<Pair<String, String>> remaining = sources.keyValuesView().toList();
            remaining.remove(source);

            for (Pair<String, String> secondSource : remaining)
            {
                try
                {
                    new RuntimeTestScriptBuilder()
                            .createInMemorySources(sources)
                            .createInMemorySource(FUNCTION_SOURCE_ID, PROBE_FUNCTION)
                            .compile()
                            .run(runtime, functionExecution);

                    RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                            runtime, functionExecution,
                            Lists.fixedSize.of(source, secondSource),
                            Lists.fixedSize.of());
                }
                catch (Throwable t)
                {
                    throw new AssertionError("Failure while delete/reload of {'" + source.getOne() + "', '" + secondSource.getOne() + "'}: " + t.getMessage(), t);
                }
                finally
                {
                    // Reset so the next iteration starts from a clean runtime.
                    setUpRuntime();
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Local mapping properties tests
    // ------------------------------------------------------------------------------------------

    @Test
    public void testModelJoinWithLocalMappingProperties()
    {
        // Classes do NOT have id/firmId — those are local mapping properties
        String localModel =
                "Class Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Person\n" +
                        "{\n" +
                        "   lastName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association Firm_Person\n" +
                        "{\n" +
                        "   firm : Firm[1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n";

        String localRelational =
                "###Pure\n" +
                        "Class SrcFirm\n" +
                        "{\n" +
                        "   _id : String[1];\n" +
                        "   _legalName : String[1];\n" +
                        "}\n" +
                        "Class SrcPerson\n" +
                        "{\n" +
                        "   _firmId : String[1];\n" +
                        "   _lastName : String[1];\n" +
                        "}\n";

        // Mapping uses +id and +firmId as local mapping properties
        String localMapping =
                "###Mapping\nMapping FirmMapping\n(\n" +
                        "   Firm[f1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcFirm\n" +
                        "      +id : String[1] : $src._id,\n" +
                        "      legalName : $src._legalName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcPerson\n" +
                        "      +firmId : String[1] : $src._firmId,\n" +
                        "      lastName : $src._lastName\n" +
                        "   }\n" +
                        "   Firm_Person : ModelJoin\n" +
                        "   {\n" +
                        "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                        "   }\n" +
                        ")\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", localModel, "source3.pure", localMapping, "source4.pure", localRelational)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source3.pure").compile().createInMemorySource("source3.pure", localMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinWithLocalMappingPropertiesAddRemove()
    {
        // Classes do NOT have id/firmId
        String localModel =
                "Class Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Person\n" +
                        "{\n" +
                        "   lastName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association Firm_Person\n" +
                        "{\n" +
                        "   firm : Firm[1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n";

        String localRelational =
                "###Pure\n" +
                        "Class SrcFirm\n" +
                        "{\n" +
                        "   _id : String[1];\n" +
                        "   _legalName : String[1];\n" +
                        "}\n" +
                        "Class SrcPerson\n" +
                        "{\n" +
                        "   _firmId : String[1];\n" +
                        "   _lastName : String[1];\n" +
                        "}\n";

        String localMappingNoAssoc =
                "###Mapping\nMapping FirmMapping\n(\n" +
                        "   Firm[f1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcFirm\n" +
                        "      +id : String[1] : $src._id,\n" +
                        "      legalName : $src._legalName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcPerson\n" +
                        "      +firmId : String[1] : $src._firmId,\n" +
                        "      lastName : $src._lastName\n" +
                        "   }\n" +
                        ")\n";

        String localMappingWithAssoc =
                "###Mapping\nMapping FirmMapping\n(\n" +
                        "   Firm[f1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcFirm\n" +
                        "      +id : String[1] : $src._id,\n" +
                        "      legalName : $src._legalName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcPerson\n" +
                        "      +firmId : String[1] : $src._firmId,\n" +
                        "      lastName : $src._lastName\n" +
                        "   }\n" +
                        "   Firm_Person : ModelJoin\n" +
                        "   {\n" +
                        "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                        "   }\n" +
                        ")\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", localModel, "source3.pure", localMappingNoAssoc, "source4.pure", localRelational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", localMappingWithAssoc).compile().updateSource("source3.pure", localMappingNoAssoc).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    // ------------------------------------------------------------------------------------------
    // Parse-order tests — verifies the parser does NOT require the association to be resolved
    // at parse time. The parser uses the lambda's parameter names as the property names, so
    // any ordering of association and mapping is supported.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testModelJoinSameFileAssociationBeforeMapping()
    {
        String allInOne =
                "Class Firm { id : String[1]; legalName : String[1]; }\n" +
                        "Class Person { firmId : String[1]; lastName : String[1]; }\n" +
                        "Association Firm_Person { firm : Firm[1]; employees : Person[*]; }\n" +
                        "###Pure\n" +
                        "Class SrcFirm { _id : String[1]; _legalName : String[1]; }\n" +
                        "Class SrcPerson { _firmId : String[1]; _lastName : String[1]; }\n" +
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Pure { ~src SrcFirm id : $src._id, legalName : $src._legalName }\n" +
                        "   Person[e] : Pure { ~src SrcPerson firmId : $src._firmId, lastName : $src._lastName }\n" +
                        "   Firm_Person : ModelJoin { {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId} }\n" +
                        ")\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", allInOne)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source1.pure").compile().createInMemorySource("source1.pure", allInOne).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinSameFileMappingBeforeAssociation()
    {
        // Mapping appears BEFORE association in the same file — works because the parser
        // extracts property names from lambda params (no parse-time association lookup).
        String allInOne =
                "###Pure\n" +
                        "Class SrcFirm { _id : String[1]; _legalName : String[1]; }\n" +
                        "Class SrcPerson { _firmId : String[1]; _lastName : String[1]; }\n" +
                        "Class Firm { id : String[1]; legalName : String[1]; }\n" +
                        "Class Person { firmId : String[1]; lastName : String[1]; }\n" +
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Pure { ~src SrcFirm id : $src._id, legalName : $src._legalName }\n" +
                        "   Person[e] : Pure { ~src SrcPerson firmId : $src._firmId, lastName : $src._lastName }\n" +
                        "   Firm_Person : ModelJoin { {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId} }\n" +
                        ")\n" +
                        "###Pure\n" +
                        "Association Firm_Person { firm : Firm[1]; employees : Person[*]; }\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", allInOne)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source1.pure").compile().createInMemorySource("source1.pure", allInOne).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinAssociationInLaterFile()
    {
        // Mapping file may be parsed before the association file — works for the same reason.
        String model =
                "Class Firm { id : String[1]; legalName : String[1]; }\n" +
                        "Class Person { firmId : String[1]; lastName : String[1]; }\n" +
                        "###Pure\n" +
                        "Class SrcFirm { _id : String[1]; _legalName : String[1]; }\n" +
                        "Class SrcPerson { _firmId : String[1]; _lastName : String[1]; }\n";
        String mappingFile = "###Mapping\nMapping FirmMapping\n(" + coreMapping + assoMapping + ")\n";
        String associationFile = "Association Firm_Person { firm : Firm[1]; employees : Person[*]; }\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with(
                        "a_model.pure", model,
                        "z_assoc.pure", associationFile,
                        "m_mapping.pure", mappingFile)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("m_mapping.pure").compile().createInMemorySource("m_mapping.pure", mappingFile).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinLambdaParamNameMustMatchAssociationProperty()
    {
        // Param names 'a' and 'b' do NOT match association property names 'firm'/'employees'.
        // PropertyMappingProcessor fails because the parser uses param names as property names.
        String allInOne =
                "Class Firm { id : String[1]; legalName : String[1]; }\n" +
                        "Class Person { firmId : String[1]; lastName : String[1]; }\n" +
                        "Association Firm_Person { firm : Firm[1]; employees : Person[*]; }\n" +
                        "###Pure\n" +
                        "Class SrcFirm { _id : String[1]; _legalName : String[1]; }\n" +
                        "Class SrcPerson { _firmId : String[1]; _lastName : String[1]; }\n" +
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Pure { ~src SrcFirm id : $src._id, legalName : $src._legalName }\n" +
                        "   Person[e] : Pure { ~src SrcPerson firmId : $src._firmId, lastName : $src._lastName }\n" +
                        "   Firm_Person : ModelJoin { {a:Firm[1], b:Person[1]|$a.id == $b.firmId} }\n" +
                        ")\n";
        runtime.createInMemorySource("source1.pure", allInOne);
        try
        {
            runtime.compile();
            org.junit.Assert.fail("Expected compilation failure for mismatched lambda param names");
        }
        catch (org.finos.legend.pure.m4.exception.PureCompilationException e)
        {
            org.junit.Assert.assertTrue(
                    "Expected error about unknown property 'a', got: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("'a' is unknown in the Element 'Firm_Person'"));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Milestoned association tests — verifies that ModelJoin works correctly when one or both
    // sides of the association are temporal. The milestoning processor reorders
    // association._properties() (appends edge-point, removes original), which previously broke
    // resolveContext's positional class derivation. These tests would have caught that bug.
    // ------------------------------------------------------------------------------------------

    private static final String MILESTONED_MODEL =
            "Class <<meta::pure::profiles::temporal.businesstemporal>> MJMilOrder\n" +
                    "{\n" +
                    "   description : String[1];\n" +
                    "}\n" +
                    "Class MJMilProduct\n" +
                    "{\n" +
                    "   code : String[1];\n" +
                    "}\n" +
                    "Association MJMilOrder_MJMilProduct\n" +
                    "{\n" +
                    "   order : MJMilOrder[1];\n" +
                    "   product : MJMilProduct[1];\n" +
                    "}\n";

    private static final String MILESTONED_SRC =
            "###Pure\n" +
                    "Class SrcMJMilOrder\n" +
                    "{\n" +
                    "   _description : String[1];\n" +
                    "   _productId : Integer[1];\n" +
                    "}\n" +
                    "Class SrcMJMilProduct\n" +
                    "{\n" +
                    "   _code : String[1];\n" +
                    "   _id : Integer[1];\n" +
                    "}\n";

    private static final String MILESTONED_MAPPING_WITH_ASSOC =
            "###Mapping\nMapping MJMilMapping\n(\n" +
                    "   MJMilOrder[o] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcMJMilOrder\n" +
                    "      description : $src._description,\n" +
                    "      +productId : Integer[1] : $src._productId\n" +
                    "   }\n" +
                    "   MJMilProduct[p] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcMJMilProduct\n" +
                    "      code : $src._code,\n" +
                    "      +id : Integer[1] : $src._id\n" +
                    "   }\n" +
                    "   MJMilOrder_MJMilProduct : ModelJoin\n" +
                    "   {\n" +
                    "      {order:MJMilOrder[1], product:MJMilProduct[1]|$order.productId == $product.id}\n" +
                    "   }\n" +
                    ")\n";

    private static final String MILESTONED_MAPPING_NO_ASSOC =
            "###Mapping\nMapping MJMilMapping\n(\n" +
                    "   MJMilOrder[o] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcMJMilOrder\n" +
                    "      description : $src._description,\n" +
                    "      +productId : Integer[1] : $src._productId\n" +
                    "   }\n" +
                    "   MJMilProduct[p] : Pure\n" +
                    "   {\n" +
                    "      ~src SrcMJMilProduct\n" +
                    "      code : $src._code,\n" +
                    "      +id : Integer[1] : $src._id\n" +
                    "   }\n" +
                    ")\n";

    @Test
    public void testModelJoinMilestonedOneSideBusinessTemporal()
    {
        // Order is businesstemporal, Product is not. This triggers MilestoningPropertyProcessor
        // to reorder association._properties() — previously broke resolveContext.
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with(
                        "source1.pure", MILESTONED_MODEL,
                        "source3.pure", MILESTONED_MAPPING_WITH_ASSOC,
                        "source4.pure", MILESTONED_SRC)).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", MILESTONED_MAPPING_NO_ASSOC).compile()
                        .updateSource("source3.pure", MILESTONED_MAPPING_WITH_ASSOC).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinMilestonedDeleteAndReload()
    {
        // Delete the mapping file and recreate — ensures unbind/rebind handles the milestoned
        // association edge-point properties correctly across incremental cycles.
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with(
                        "source1.pure", MILESTONED_MODEL,
                        "source3.pure", MILESTONED_MAPPING_WITH_ASSOC,
                        "source4.pure", MILESTONED_SRC)).compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure").compile()
                        .createInMemorySource("source3.pure", MILESTONED_MAPPING_WITH_ASSOC).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testModelJoinMilestonedBothSidesBusinessTemporal()
    {
        // Both sides businesstemporal — milestoning rewrites properties on BOTH ends.
        String bothMilModel =
                "Class <<meta::pure::profiles::temporal.businesstemporal>> MJBothMilA\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "Class <<meta::pure::profiles::temporal.businesstemporal>> MJBothMilB\n" +
                        "{\n" +
                        "   label : String[1];\n" +
                        "}\n" +
                        "Association MJBothMilA_MJBothMilB\n" +
                        "{\n" +
                        "   sideA : MJBothMilA[1];\n" +
                        "   sideB : MJBothMilB[1];\n" +
                        "}\n";

        String bothMilSrc =
                "###Pure\n" +
                        "Class SrcMJBothMilA { _name : String[1]; _key : String[1]; }\n" +
                        "Class SrcMJBothMilB { _label : String[1]; _key : String[1]; }\n";

        String bothMilMapping =
                "###Mapping\nMapping MJBothMilMapping\n(\n" +
                        "   MJBothMilA[a1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcMJBothMilA\n" +
                        "      name : $src._name,\n" +
                        "      +key : String[1] : $src._key\n" +
                        "   }\n" +
                        "   MJBothMilB[b1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcMJBothMilB\n" +
                        "      label : $src._label,\n" +
                        "      +key : String[1] : $src._key\n" +
                        "   }\n" +
                        "   MJBothMilA_MJBothMilB : ModelJoin\n" +
                        "   {\n" +
                        "      {sideA:MJBothMilA[1], sideB:MJBothMilB[1]|$sideA.key == $sideB.key}\n" +
                        "   }\n" +
                        ")\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with(
                        "source1.pure", bothMilModel,
                        "source3.pure", bothMilMapping,
                        "source4.pure", bothMilSrc)).compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure").compile()
                        .createInMemorySource("source3.pure", bothMilMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    // ---------------------------------------------------------------------
    // C1 — lambda parameter order opposite to association declaration order,
    //      stress-tested through an unbind/rebind cycle.
    //
    // The processor's resolveContext binds parameters to classes by name (not
    // position), so first-pass compilation succeeds. The unbinder must restore
    // the user's original parameter names so the next compile still binds. If
    // the unbinder restores names by lambda position rather than by user→
    // property mapping, the subsequent recompile will mismatch and fail.
    // ---------------------------------------------------------------------

    public static String reverseOrderAssoMapping =
            "   Firm_Person : ModelJoin\n" +
                    "   {\n" +
                    "      {employees:Person[1], firm:Firm[1]|$firm.id == $employees.firmId}\n" +
                    "   }\n";

    public static String reverseOrderMapping =
            "###Mapping\nMapping FirmMapping\n(" + coreMapping + reverseOrderAssoMapping + ")\n";

    @Test
    public void testModelJoinReverseParamOrderIncremental()
    {
        // Toggle source3 between "no ModelJoin" and "ModelJoin with reversed param order".
        // Each updateSource round-trips the ModelJoin through unbind / rebind. The unbinder
        // must restore the user's original param names — `employees` at position 0 and
        // `firm` at position 1 — even though the association declares them in the opposite
        // order. If unbind restored by association-property position, the next compile
        // would fail with "lambda parameter names {firm, employees} do not match …".
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with(
                        "source1.pure", model,
                        "source3.pure", initialMapping,
                        "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", reverseOrderMapping).compile()
                        .updateSource("source3.pure", initialMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
