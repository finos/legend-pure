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

package org.finos.legend.pure.m3.serialization.compiler.strings.v1;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringWriter;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;

abstract class StringWriterV1 extends BaseStringIndex implements StringWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StringWriterV1.class);

    private final ObjectIntMap<String> stringIndex;

    private StringWriterV1(ObjectIntMap<String> stringIndex)
    {
        this.stringIndex = stringIndex;
    }

    @Override
    public void writeString(Writer writer, String string)
    {
        int id = getStringId(string);
        writeStringId(writer, id);
    }

    protected int getStringId(String string)
    {
        int id = getSpecialStringId(string);
        if (id == 0)
        {
            id = this.stringIndex.getIfAbsent(string, -1);
            if (id == -1)
            {
                throw new IllegalArgumentException("Unknown string: '" + StringEscape.escape(string) + "'");
            }
        }
        return id;
    }

    protected abstract void writeStringId(Writer writer, int id);

    private void serialize(Writer writer, String[] strings)
    {
        writer.writeStringArray(strings);
    }

    private static class OneByte extends StringWriterV1
    {
        private OneByte(ObjectIntMap<String> stringIndex)
        {
            super(stringIndex);
        }

        @Override
        public void writeStringArray(Writer writer, String[] strings)
        {
            int length = strings.length;
            byte[] ids = new byte[length];
            for (int i = 0; i < length; i++)
            {
                ids[i] = idToOneByte(getStringId(strings[i]));
            }
            writer.writeByteArray(ids);
        }

        @Override
        protected void writeStringId(Writer writer, int id)
        {
            writer.writeByte(idToOneByte(id));
        }
    }

    private static class TwoBytes extends StringWriterV1
    {
        private TwoBytes(ObjectIntMap<String> stringIndex)
        {
            super(stringIndex);
        }

        @Override
        public void writeStringArray(Writer writer, String[] strings)
        {
            int length = strings.length;
            short[] ids = new short[length];
            for (int i = 0; i < length; i++)
            {
                ids[i] = idToTwoBytes(getStringId(strings[i]));
            }
            writer.writeShortArray(ids);
        }

        @Override
        protected void writeStringId(Writer writer, int id)
        {
            writer.writeShort(idToTwoBytes(id));
        }
    }

    private static class ThreeBytes extends StringWriterV1
    {
        private ThreeBytes(ObjectIntMap<String> stringIndex)
        {
            super(stringIndex);
        }

        @Override
        public void writeStringArray(Writer writer, String[] strings)
        {
            int length = strings.length;
            byte[] ids = new byte[length * 3];
            for (int i = 0, j = 0; i < length; i++, j += 3)
            {
                idToThreeBytes(getStringId(strings[i]), ids, j);
            }
            writer.writeByteArray(ids);
        }

        @Override
        protected void writeStringId(Writer writer, int id)
        {
            writer.writeBytes(idToThreeBytes(id));
        }
    }

    private static class FourBytes extends StringWriterV1
    {
        private FourBytes(ObjectIntMap<String> stringIndex)
        {
            super(stringIndex);
        }

        @Override
        public void writeStringArray(Writer writer, String[] strings)
        {
            int length = strings.length;
            int[] ids = new int[length];
            for (int i = 0; i < length; i++)
            {
                ids[i] = getStringId(strings[i]);
            }
            writer.writeIntArray(ids);
        }

        @Override
        protected void writeStringId(Writer writer, int id)
        {
            writer.writeInt(id);
        }
    }

    static StringWriter writeStringIndex(Writer writer, Iterable<String> strings)
    {
        long start = System.nanoTime();
        LOGGER.debug("Starting writing string index");

        // Prepare string set
        MutableSet<String> stringSet = Iterate.reject(strings, BaseStringIndex::isSpecialString, Sets.mutable.empty());

        // Create sorted array of strings
        String[] stringArray = stringSet.toArray(new String[stringSet.size()]);
        Arrays.sort(stringArray, Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));

        // Build string id index
        MutableObjectIntMap<String> stringIndex = ObjectIntMaps.mutable.ofInitialCapacity(stringArray.length);
        ArrayIterate.forEachWithIndex(stringArray, stringIndex::put);
        StringWriterV1 stringWriter = newStringWriter(stringIndex);

        long buildEnd = System.nanoTime();
        LOGGER.debug("Finished building string index with {} strings in {}s", stringArray.length, (buildEnd - start) / 1_000_000_000.0);

        // Serialize string index
        stringWriter.serialize(writer, stringArray);

        long end = System.nanoTime();
        LOGGER.debug("Finished serializing string index with {} strings in {}s", stringArray.length, (end - buildEnd) / 1_000_000_000.0);
        LOGGER.debug("Finished writing string index with {} strings in {}s", stringArray.length, (end - start) / 1_000_000_000.0);
        return stringWriter;
    }

    private static StringWriterV1 newStringWriter(ObjectIntMap<String> stringIndex)
    {
        int width = getStringIdByteWidth(stringIndex.size());
        LOGGER.debug("String id byte width: {}", width);
        switch (width)
        {
            case 1:
            {
                return new OneByte(stringIndex);
            }
            case 2:
            {
                return new TwoBytes(stringIndex);
            }
            case 3:
            {
                return new ThreeBytes(stringIndex);
            }
            case 4:
            {
                return new FourBytes(stringIndex);
            }
            default:
            {
                throw new RuntimeException("Unsupported id byte width: " + width);
            }
        }
    }
}
