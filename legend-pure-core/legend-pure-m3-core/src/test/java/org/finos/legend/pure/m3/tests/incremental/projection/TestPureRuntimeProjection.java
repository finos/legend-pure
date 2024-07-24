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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.VoidFunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tests.TrackingTransactionObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestPureRuntimeProjection extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("projectionId.pure");
        runtime.delete("projections.pure");
        runtime.delete("association.pure");
        runtime.delete("associationProjection.pure");
        runtime.delete("function.pure");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeProperty()
    {
        String source = "Class A{version : Integer[1];}";
        String sourceId = "sourceId.pure";
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("userId.pure", "Class AP projects #A{+[version]}#");
        runtime.compile();
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "A has not been defined!", "userId.pure", 1, 20);
    }

    @Test
    public void testPureRuntimePropertyChange()
    {
        String source = "Class A{version : Integer[1];}";
        String badSource = "Class A{vers : Integer[1];}";
        String sourceId = "sourceId.pure";
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("userId.pure", "Class AP projects #A{+[version]}#");
        runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(runtime,
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
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("projections.pure", projections);
        runtime.createInMemorySource("association.pure", association);
        runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
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
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("projections.pure", projections);
        runtime.createInMemorySource("association.pure", association);
        runtime.createInMemorySource("associationProjection.pure", associationProjection);
        runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
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
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("projections.pure", projections);
        runtime.createInMemorySource("association.pure", association);
        runtime.createInMemorySource("associationProjection.pure", associationProjection);
        runtime.createInMemorySource("function.pure", function);
        runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
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
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource("userId.pure", "Class AP projects #EntityWithLocations{+[locationsByType(GeographicEntityType[*])]}#");
        runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "EntityWithLocations has not been defined!", "userId.pure", 1, 20);
    }

    @Test
    public void testProjectionWithTemporalTarget()
    {
        String source = "Class <<temporal.businesstemporal>> {doc.doc='projected class'} A{ name: String[1]; selfProp: A[*]; } \n";
        String projection = "Class <<doc.deprecated>> {doc.doc='projection class'} AP projects A{ * }";
        String sourceId = "sourceId.pure";
        String projectionId = "projectionId.pure";
        runtime.createInMemorySource(sourceId, source);
        runtime.createInMemorySource(projectionId, projection);
        runtime.compile();

        runtime.modify(sourceId, "//comment added\n" + source);
        runtime.modify(projectionId, "//comment added\n" + projection);
        runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "A has not been defined!",
                projectionId, 2, 67);
    }

    @Test
    public void testProjectionMemoryLeak()
    {
        String sourceId = "sourceId.pure";
        String sampleTestModel = readFile("org/finos/legend/pure/m3/tests/incremental/projection/projectionTestModel.pure");
        runtime.createInMemorySource(sourceId, sampleTestModel);
        runtime.compile();
        byte[] before = runtime.getModelRepository().serialize();
        int repositorySize = before.length;

        String newSource = "////Some comment\n" + sampleTestModel;

        RuntimeTestScriptBuilder testSciptBuilder = new RuntimeTestScriptBuilder();

        testSciptBuilder.updateSource(sourceId, newSource)
                .compile().run(runtime, VoidFunctionExecution.VOID_FUNCTION_EXECUTION);

        byte[] after = runtime.getModelRepository().serialize();
        int sizeAfter = after.length;
        if (sizeAfter != repositorySize)
        {
            TrackingTransactionObserver.compareBytes(before, after);
            Assert.assertEquals(sourceId + ", delta " + (sizeAfter - repositorySize), repositorySize, sizeAfter);
        }
    }

    protected String readFile(String fileName)
    {
        URL url = getClass().getClassLoader().getResource(fileName);
        if (url == null)
        {
            throw new RuntimeException("Cannot find file: " + fileName);
        }
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
        {
            char[] buffer = new char[8196];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                builder.append(buffer, 0, read);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return builder.toString();
    }
}
