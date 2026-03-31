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
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestModuleMetadataSerializer
{
    private final int MAX_EXTENSION_VERSION = 3;

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
        IntList expectedVersions = IntInterval.fromTo(1, MAX_EXTENSION_VERSION).toList();
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
                (s, m) -> v1Serialize.increment(),
                s ->
                {
                    v1Deserialize.increment();
                    return emptyMetadata;
                });
        ModuleMetadataSerializerExtension v2 = newExtension(2,
                (s, m) -> v2Serialize.increment(),
                s ->
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
        serializer.serializeManifest(stream, emptyMetadata, 1);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(0, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        serializer.deserializeManifest(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        stream.reset();
        serializer.serializeManifest(stream, emptyMetadata, 2);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        serializer.deserializeManifest(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        stream.reset();
        serializer.serializeManifest(stream, emptyMetadata);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        serializer.deserializeManifest(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(2, v2Deserialize.getCount());

        stream.reset();
        int unexpectedVersion = MAX_EXTENSION_VERSION + 1;
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> serializer.serializeManifest(stream, emptyMetadata, unexpectedVersion));
        Assert.assertEquals("Unknown extension: " + unexpectedVersion, e.getMessage());
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

    private ModuleMetadataSerializerExtension newExtension(int version, BiConsumer<? super OutputStream, ? super ModuleManifest> serializer, Function<? super InputStream, ? extends ModuleManifest> deserializer)
    {
        return new ModuleMetadataSerializerExtension()
        {
            @Override
            public int version()
            {
                return version;
            }

            @Override
            public void serializeManifest(OutputStream stream, ModuleManifest manifest, StringIndexer stringIndexer)
            {
                if (serializer == null)
                {
                    throw new UnsupportedOperationException();
                }
                serializer.accept(stream, manifest);
            }

            @Override
            public ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer)
            {
                if (deserializer == null)
                {
                    throw new UnsupportedOperationException();
                }
                return deserializer.apply(stream);
            }

            @Override
            public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer)
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
