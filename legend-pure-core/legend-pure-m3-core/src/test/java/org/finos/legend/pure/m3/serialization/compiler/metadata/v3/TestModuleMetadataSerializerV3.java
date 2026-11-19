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

package org.finos.legend.pure.m3.serialization.compiler.metadata.v3;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.compiler.metadata.AbstractTestModuleMetadataSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.metadata.v2.ModuleMetadataSerializerV2;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class TestModuleMetadataSerializerV3 extends AbstractTestModuleMetadataSerializerExtension
{
    // the default size of a BufferedOutputStream's buffer
    private static final int STREAM_BUFFER_SIZE = 8192;

    // an element in another module that every element of the large module refers to
    private static final String HUB = "model::hub::Hub";

    private final ModuleMetadataSerializerV2 v2 = new ModuleMetadataSerializerV2();
    private final ModuleMetadataSerializerV3 v3 = new ModuleMetadataSerializerV3();
    private final StringIndexer stringIndexer = StringIndexer.defaultStringIndexer();

    @Override
    protected ModuleMetadataSerializerExtension getExtension()
    {
        return new ModuleMetadataSerializerV3();
    }

    @Test
    public void testLargeModule()
    {
        testModuleMetadataSerializes(getLargeModule());
    }

    @Test
    public void testPayloadsAreDeflatedV2Payloads()
    {
        // Each V3 payload is the V2 payload as raw deflate at level 7. How the V2 payload is fed to the deflater must
        // not change the output, since files written earlier must stay byte-identical.
        assertPayloadsAreDeflatedV2Payloads(getEmptyModuleMetadata(), false);
        assertPayloadsAreDeflatedV2Payloads(getSimpleModuleWithOneSource(), false);
        assertPayloadsAreDeflatedV2Payloads(getSimpleModuleWithMultipleSources(), false);
        assertPayloadsAreDeflatedV2Payloads(getSimpleModuleWithDependencies(), false);
        assertPayloadsAreDeflatedV2Payloads(getLargeModule(), true);
    }

    private void assertPayloadsAreDeflatedV2Payloads(ModuleMetadata metadata, boolean large)
    {
        String name = metadata.getName();
        assertPayloadIsDeflatedV2Payload(name + " manifest", metadata.getManifest(), ModuleMetadataSerializerExtension::serializeManifest, large);
        assertPayloadIsDeflatedV2Payload(name + " sources", metadata.getSourceMetadata(), ModuleMetadataSerializerExtension::serializeSourceMetadata, large);
        assertPayloadIsDeflatedV2Payload(name + " external references", metadata.getExternalReferenceMetadata(), ModuleMetadataSerializerExtension::serializeExternalReferenceMetadata, large);
        assertPayloadIsDeflatedV2Payload(name + " function names", metadata.getFunctionNameMetadata(), ModuleMetadataSerializerExtension::serializeFunctionNameMetadata, large);
        assertPayloadIsDeflatedV2Payload(name + " back reference index", ModuleBackReferenceIndex.fromBackReferenceMetadata(metadata.getBackReferenceMetadata()), ModuleMetadataSerializerExtension::serializeBackReferenceIndex, large);
        for (ElementBackReferenceMetadata backReferences : metadata.getBackReferenceMetadata().getBackReferences())
        {
            // in the large module, only the hub's back references are large
            boolean largeBackReferences = large && HUB.equals(backReferences.getElementPath());
            assertPayloadIsDeflatedV2Payload(name + " back references for " + backReferences.getElementPath(), backReferences, ModuleMetadataSerializerExtension::serializeBackReferenceMetadata, largeBackReferences);
        }
    }

    private <M> void assertPayloadIsDeflatedV2Payload(String description, M metadata, MetadataSerializer<M> serializer, boolean large)
    {
        ByteArrayOutputStream v2Stream = new ByteArrayOutputStream();
        serializer.serialize(this.v2, v2Stream, metadata, this.stringIndexer);
        byte[] v2Payload = v2Stream.toByteArray();

        ByteArrayOutputStream v3Stream = new ByteArrayOutputStream();
        serializer.serialize(this.v3, v3Stream, metadata, this.stringIndexer);

        Assert.assertArrayEquals(description, deflate(v2Payload), v3Stream.toByteArray());
        if (large)
        {
            // the payload must be larger than a stream buffer for the test to cover more than one buffer's worth
            Assert.assertTrue(description + ": " + v2Payload.length + " bytes", v2Payload.length > STREAM_BUFFER_SIZE);
        }
    }

    private ModuleMetadata getLargeModule()
    {
        ModuleMetadata.Builder builder = ModuleMetadata.builder("large_module")
                .withReferenceIdVersion(1)
                .withDependencies("platform", "hub_module");
        for (int s = 0; s < 20; s++)
        {
            String pkg = "model::classes" + s;
            String sourceId = "/large_module/model/classes" + s + ".pure";
            MutableList<String> paths = Lists.mutable.empty();
            for (int c = 0; c < 100; c++)
            {
                String path = pkg + "::LargeClass" + c;
                String hubProperty = path + ".properties['hub'].classifierGenericType.typeArguments[1]";
                int line = (c * 10) + 1;
                paths.add(path);
                builder.withElement(newClass(path, sourceId, line, 1, line + 8, 1))
                        .withExternalReferences(path, HUB, HUB + ".properties['spoke" + c + "']")
                        .withBackReferences(HUB, HUB, refUsage(hubProperty, "rawType", 0, sourceId, line + 2, 5, line + 2, 19))
                        .withBackReferences(path, path, specialization(pkg + "::LargeSubclass" + c))
                        .withFunctionByName("largeFunc" + c, pkg + "::largeFunc" + c + "_LargeClass" + c + "_1__String_1_");
            }
            builder.withSource(newSource(sourceId, newSourceSection("Pure", paths.toArray(new String[0]))));
        }
        return builder.build();
    }

    private interface MetadataSerializer<M>
    {
        void serialize(ModuleMetadataSerializerExtension extension, OutputStream stream, M metadata, StringIndexer stringIndexer);
    }

    private static byte[] deflate(byte[] bytes)
    {
        Deflater deflater = new Deflater(7, true);
        try
        {
            deflater.setInput(bytes);
            deflater.finish();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            while (!deflater.finished())
            {
                int count = deflater.deflate(buffer);
                stream.write(buffer, 0, count);
            }
            return stream.toByteArray();
        }
        finally
        {
            deflater.end();
        }
    }
}
