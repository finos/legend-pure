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

package org.finos.legend.pure.m3.serialization.compiler.strings;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TestStringIndexer
{
    @Test
    public void testExtensionRequired()
    {
        IllegalStateException e = Assert.assertThrows(IllegalStateException.class, () -> StringIndexer.builder().build());
        Assert.assertEquals("At least one extension is required", e.getMessage());
    }

    @Test
    public void testConflictingExtensions()
    {
        StringIndexerExtension v1 = newExtension(1);

        StringIndexer.Builder builder = StringIndexer.builder().withExtension(v1);
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
        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> StringIndexer.builder().withExtension(null));
        Assert.assertEquals("extension may not be null", e.getMessage());
    }

    @Test
    public void testLoadingFromClassLoader()
    {
        testStringIndexerHasVersions(StringIndexer.builder().withLoadedExtensions().build(), 0, 1, 2, 3);
    }

    @Test
    public void testNullStringIndexer()
    {
        testStringIndexerHasVersions(StringIndexer.nullStringIndexer(), 0);
    }

    @Test
    public void testDefaultStringIndexer()
    {
        testStringIndexerHasVersions(StringIndexer.defaultStringIndexer(), 0, 1, 2, 3);
    }

    private void testStringIndexerHasVersions(StringIndexer stringIndexer, int... expectedVersions)
    {
        MutableIntList expectedList = IntLists.mutable.with(expectedVersions).sortThis();
        MutableIntList foundVersions = IntLists.mutable.empty();
        stringIndexer.forEachVersion(foundVersions::add);
        Assert.assertEquals(expectedList, foundVersions.sortThis());
        Assert.assertEquals(expectedList.getLast(), stringIndexer.getDefaultVersion());
    }

    @Test
    public void testSerializeByVersion()
    {
        Counter v1Serialize = new Counter(0);
        Counter v1Deserialize = new Counter(0);
        Counter v2Serialize = new Counter(0);
        Counter v2Deserialize = new Counter(0);

        ImmutableList<String> stringList = Lists.immutable.with("the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog");

        StringIndexerExtension v1 = newExtension(1,
                (writer, strings) ->
                {
                    v1Serialize.increment();
                    return new TrivialStringWriter();
                },
                reader ->
                {
                    v1Deserialize.increment();
                    return new TrivialStringReader();
                });
        StringIndexerExtension v2 = newExtension(2,
                (writer, strings) ->
                {
                    v2Serialize.increment();
                    return new TrivialStringWriter();
                },
                reader ->
                {
                    v2Deserialize.increment();
                    return new TrivialStringReader();
                });

        StringIndexer stringIndexer = StringIndexer.builder().withExtensions(v1, v2).build();
        Assert.assertEquals(2, stringIndexer.getDefaultVersion());
        Assert.assertTrue(stringIndexer.isVersionAvailable(1));
        Assert.assertTrue(stringIndexer.isVersionAvailable(2));

        Assert.assertEquals(0, v1Serialize.getCount());
        Assert.assertEquals(0, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stringIndexer.writeStringIndex(BinaryWriters.newBinaryWriter(stream), stringList, 1);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(0, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        stringIndexer.readStringIndex(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(0, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        stream.reset();
        stringIndexer.writeStringIndex(BinaryWriters.newBinaryWriter(stream), stringList, 2);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(0, v2Deserialize.getCount());

        stringIndexer.readStringIndex(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(1, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        stream.reset();
        stringIndexer.writeStringIndex(BinaryWriters.newBinaryWriter(stream), stringList);
        Assert.assertNotEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(1, v2Deserialize.getCount());

        stringIndexer.readStringIndex(BinaryReaders.newBinaryReader(stream.toByteArray()));
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(2, v2Deserialize.getCount());

        stream.reset();
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> stringIndexer.writeStringIndex(BinaryWriters.newBinaryWriter(stream), stringList, 3));
        Assert.assertEquals("Unknown extension: 3", e.getMessage());
        Assert.assertEquals(0, stream.size());
        Assert.assertEquals(1, v1Serialize.getCount());
        Assert.assertEquals(1, v1Deserialize.getCount());
        Assert.assertEquals(2, v2Serialize.getCount());
        Assert.assertEquals(2, v2Deserialize.getCount());
    }

    private StringIndexerExtension newExtension(int version)
    {
        return newExtension(
                version,
                (writer, strings) -> new TrivialStringWriter(),
                reader -> new TrivialStringReader());
    }

    private StringIndexerExtension newExtension(int version, BiFunction<Writer, Iterable<String>, StringWriter> writeStringIndex, Function<Reader, StringReader> readStringIndex)
    {
        return new StringIndexerExtension()
        {
            @Override
            public int version()
            {
                return version;
            }

            @Override
            public StringWriter writeStringIndex(Writer writer, Iterable<String> strings)
            {
                return writeStringIndex.apply(writer, strings);
            }

            @Override
            public StringReader readStringIndex(Reader reader)
            {
                return readStringIndex.apply(reader);
            }
        };
    }

    private static class TrivialStringWriter implements StringWriter
    {
        @Override
        public void writeString(Writer writer, String string)
        {
            writer.writeString(string);
        }

        @Override
        public void writeStringArray(Writer writer, String[] strings)
        {
            writer.writeStringArray(strings);
        }
    }

    private static class TrivialStringReader implements StringReader
    {
        @Override
        public String readString(Reader reader)
        {
            return reader.readString();
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipString();
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            return reader.readStringArray();
        }

        @Override
        public void skipStringArray(Reader reader)
        {
            reader.skipStringArray();
        }
    }
}
