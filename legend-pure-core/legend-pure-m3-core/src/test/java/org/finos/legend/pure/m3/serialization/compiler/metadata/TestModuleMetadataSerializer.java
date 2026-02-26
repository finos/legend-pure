// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestModuleMetadataSerializer
{
    @Test
    public void testExtensionRequired()
    {
        IllegalStateException e = Assert.assertThrows(IllegalStateException.class, () -> ModuleMetadataSerializer.builder().build());
        Assert.assertEquals("At least one extension is required", e.getMessage());
    }

    @Test
    public void testConflictingExtensions()
    {
        ModuleMetadataSerializerExtension v1 = newExtension(1);

        ModuleMetadataSerializer.Builder builder = ModuleMetadataSerializer.builder().withExtension(v1);
        // Adding the identical extension again works
        builder.withExtension(v1);
        // Adding an extension with a different version works
        builder.withExtension(newExtension(2));
        // Adding an extension with a conflicting version throws
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> builder.withExtension(newExtension(1)));
        Assert.assertEquals("There is already an extension for version 1", e.getMessage());
    }

    @Test
    public void testNoNullExtensions()
    {
        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> ModuleMetadataSerializer.builder().withExtension(null));
        Assert.assertEquals("extension may not be null", e.getMessage());
    }

    @Test
    public void testLoadingFromClassLoader()
    {
        IntList expectedVersions = IntLists.mutable.with(1, 2);
        ModuleMetadataSerializer serializer = ModuleMetadataSerializer.builder().withLoadedExtensions().build();
        MutableIntList foundVersions = IntLists.mutable.empty();
        serializer.forEachVersion(foundVersions::add);
        Assert.assertEquals(expectedVersions, foundVersions.sortThis());
        Assert.assertEquals(expectedVersions.max(), serializer.getDefaultVersion());
    }

    @Test
    public void testSerializeByVersion()
    {
        Counter v1Serialize = new Counter(0);
        Counter v1Deserialize = new Counter(0);
        Counter v2Serialize = new Counter(0);
        Counter v2Deserialize = new Counter(0);

        ModuleManifest emptyMetadata = ModuleMetadata.builder("empty_module").withReferenceIdVersion(1).build().getManifest();

        ModuleMetadataSerializerExtension v1 = newExtension(1,
                (w, m) -> v1Serialize.increment(),
                r ->
                {
                    v1Deserialize.increment();
                    return emptyMetadata;
                });
        ModuleMetadataSerializerExtension v2 = newExtension(2,
                (w, m) -> v2Serialize.increment(),
                r ->
                {
                    v2Deserialize.increment();
                    return emptyMetadata;
                });

        ModuleMetadataSerializer serializer = ModuleMetadataSerializer.builder().withExtensions(v1, v2).withStringIndexer(StringIndexer.nullStringIndexer()).build();
        Assert.assertEquals(2, serializer.getDefaultVersion());
        Assert.assertTrue(serializer.isVersionAvailable(1));
        Assert.assertTrue(serializer.isVersionAvailable(2));

        Assert.assertEquals(0, v1Serialize.getCount());
        Assert.assertEquals(0, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        serializer.serializeManifest(BinaryWriters.newBinaryWriter(stream), emptyMetadata, 1);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(0, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        serializer.deserializeManifest(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        stream.reset();
        serializer.serializeManifest(BinaryWriters.newBinaryWriter(stream), emptyMetadata, 2);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        serializer.deserializeManifest(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        stream.reset();
        serializer.serializeManifest(BinaryWriters.newBinaryWriter(stream), emptyMetadata);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        serializer.deserializeManifest(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(2, v2Deserialize.getCount());

        stream.reset();
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> serializer.serializeManifest(BinaryWriters.newBinaryWriter(stream), emptyMetadata, 3));
        Assert.assertEquals("Unknown extension: 3", e.getMessage());
        Assert.assertEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(2, v2Deserialize.getCount());
    }

    private ModuleMetadataSerializerExtension newExtension(int version)
    {
        return newExtension(version, null, null);
    }

    private ModuleMetadataSerializerExtension newExtension(int version, BiConsumer<? super Writer, ? super ModuleManifest> serializer, Function<? super Reader, ? extends ModuleManifest> deserializer)
    {
        return new ModuleMetadataSerializerExtension()
        {
            @Override
            public int version()
            {
                return version;
            }

            @Override
            public void serializeManifest(Writer writer, ModuleManifest manifest)
            {
                if (serializer == null)
                {
                    throw new UnsupportedOperationException();
                }
                serializer.accept(writer, manifest);
            }

            @Override
            public ModuleManifest deserializeManifest(Reader reader)
            {
                if (deserializer == null)
                {
                    throw new UnsupportedOperationException();
                }
                return deserializer.apply(reader);
            }

            @Override
            public void serializeSourceMetadata(Writer writer, ModuleSourceMetadata sourceMetadata)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleSourceMetadata deserializeSourceMetadata(Reader reader)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeExternalReferenceMetadata(Writer writer, ModuleExternalReferenceMetadata externalReferenceMetadata)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(Reader reader)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeBackReferenceMetadata(Writer writer, ElementBackReferenceMetadata backReferenceMetadata)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ElementBackReferenceMetadata deserializeBackReferenceMetadata(Reader reader)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeFunctionNameMetadata(Writer writer, ModuleFunctionNameMetadata functionNameMetadata)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(Reader reader)
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
