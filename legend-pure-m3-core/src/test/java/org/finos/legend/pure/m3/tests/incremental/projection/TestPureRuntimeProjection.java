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

package org.finos.legend.pure.m3.tests.incremental.projection;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tests.TrackingTransactionObserver;
import org.finos.legend.pure.m3.execution.VoidFunctionExecution;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestPureRuntimeProjection extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("projectionId.pure");
        runtime.delete("projections.pure");
        runtime.delete("association.pure");
        runtime.delete("associationProjection.pure");
        runtime.delete("function.pure");
    }

    @Test
    public void testPureRuntimeProperty()
    {
        String source = "Class A{version : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "Class AP projects #A{+[version]}#");
        this.runtime.compile();
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "A has not been defined!", "userId.pure", 1, 20);
    }

    @Test
    public void testPureRuntimePropertyChange()
    {
        String source = "Class A{version : Integer[1];}";
        String badSource = "Class A{vers : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "Class AP projects #A{+[version]}#");
        this.runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(sourceId, badSource)), "The property 'version' can't be found in the type 'A' (or any supertype).",
                "userId.pure", 1, 24);

    }

    @Test
    public void testPureRuntimeAssociationProjectionRemoveAssociation()
    {
        String source = "Class A{ } \n Class B{ }\n";
        String association = "Association AB{a:A[1]; b:B[1];}";
        String projections = "Class AP projects #A{*}# \n Class BP projects #B{*}#\n" +
                "Association ABP projects AB<AP,BP>";

        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("projections.pure", projections);
        this.runtime.createInMemorySource("association.pure", association);
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair("association.pure", association)), "AB has not been defined!",
                "projections.pure", 3, 26);
    }

    @Test
    public void testPureRuntimeAssociationProjectionRemoveClassProjection()
    {
        String source = "Class A{ } \n Class B{ }\n";
        String association = "Association AB{a:A[1]; b:B[1];}";
        String projections = "Class AP projects #A{*}# \n Class BP projects #B{*}#\n";
        String associationProjection = "Association ABP projects AB<AP,BP>";

        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("projections.pure", projections);
        this.runtime.createInMemorySource("association.pure", association);
        this.runtime.createInMemorySource("associationProjection.pure", associationProjection);
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair("projections.pure", projections)), "AP has not been defined!",
                "associationProjection.pure", 1, 29);
    }

    @Test
    public void testPureRuntimeAssociationProjectionRemoveAssociationProjection()
    {
        String source = "Class A{ } \n Class B{ }\n";
        String association = "Association AB{a:A[0..1]; b:B[0..1];}";
        String projections = "Class AP projects #A{*}# \n Class BP projects #B{*}#\n";
        String associationProjection = "Association ABP projects AB<AP,BP>";
        String function = "function func():Any[*] { ^AP(b=^BP())}";

        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("projections.pure", projections);
        this.runtime.createInMemorySource("association.pure", association);
        this.runtime.createInMemorySource("associationProjection.pure", associationProjection);
        this.runtime.createInMemorySource("function.pure", function);
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair("associationProjection.pure", associationProjection)), "The property 'b' can't be found in the type 'AP' or in its hierarchy.",
                "function.pure", 1, 30);
    }

    @Test
    public void testPureRuntimeWithQualifiedPropertyWithEnum()
    {
        String source = "Class EntityWithLocations\n" +
                "{\n" +
                "    locations : Location[*];\n" +
                "    locationsByType(types:GeographicEntityType[*])\n" +
                "    {\n" +
                "        $types\n" +
                "    }:GeographicEntityType[*];\n" +
                "}\n" +
                "Class Location\n" +
                "{\n" +
                "    type : GeographicEntityType[1];\n" +
                "}\n" +
                "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "Class AP projects #EntityWithLocations{+[locationsByType(GeographicEntityType[*])]}#");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "EntityWithLocations has not been defined!", "userId.pure", 1, 20);
    }

    @Test
    public void testProjectionWithTemporalTarget()
    {
        String source = "Class <<temporal.businesstemporal>> {doc.doc='projected class'} A{ name: String[1]; selfProp: A[*]; } \n";
        String projection = "Class <<doc.deprecated>> {doc.doc='projection class'} AP projects A{ * }";
        String sourceId = "sourceId.pure";
        String projectionId = "projectionId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource(projectionId, projection);
        this.runtime.compile();

        this.runtime.modify(sourceId, "//comment added\n" + source);
        this.runtime.modify(projectionId, "//comment added\n" + projection);
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "A has not been defined!",
                projectionId, 2, 67);
    }

    @Test
    public void testProjectionMemoryLeak() throws Exception
    {
        String sourceId = "sourceId.pure";
        String sampleTestModel = readFile("org/finos/legend/pure/m3/tests/incremental/projection/projectionTestModel.pure");
        this.runtime.createInMemorySource(sourceId, sampleTestModel);
        System.out.println("Compile Iteration #1");
        this.runtime.compile();
        byte[] before = runtime.getModelRepository().serialize();
        int repositorySize = before.length;

        String newSource = "////Some comment\n" + sampleTestModel;

        RuntimeTestScriptBuilder testSciptBuilder = new RuntimeTestScriptBuilder();

        testSciptBuilder.updateSource(sourceId, newSource)
                .compile().run(runtime, VoidFunctionExecution.VOID_FUNCTION_EXECUTION);

        byte[] after = runtime.getModelRepository().serialize();
        int sizeAfter = after.length;
        int delta = sizeAfter - repositorySize;
        if (sizeAfter != repositorySize)
        {
            TrackingTransactionObserver.compareBytes(before, after);
        }
        System.out.println((delta == 0 ? "PASS," : "FAIL,") + delta + "," + sourceId);
    }

    protected String readFile(String fileName) throws URISyntaxException, IOException
    {
        URL url = getClass().getClassLoader().getResource(fileName);
        return new String(Files.readAllBytes(Paths.get(url.toURI())), "UTF8");
    }
}
