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

package org.finos.legend.pure.m3.serialization.compiler.strings.v2;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringWriter;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;

abstract class StringWriterV2 extends BaseStringIndex implements StringWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StringWriterV2.class);

    private final ObjectIntMap<String> stringIndex;

    private StringWriterV2(ObjectIntMap<String> stringIndex)
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

    private void serialize(Writer writer, String[] strings, MapIterable<String, StringInfo> stringInfo)
    {
        int len = strings.length;
        writer.writeInt(len);
        StringInfoConsumer consumer = new StringInfoConsumer()
        {
            @Override
            public void accept(SimpleStringInfo info)
            {
                serializeSimpleString(writer, info);
            }

            @Override
            public void accept(DelimitedStringInfo info)
            {
                serializeDelimitedString(writer, info);
            }

            @Override
            public void accept(SimpleBracketIndexedStringInfo info)
            {
                serializeSimpleBracketIndexedString(writer, info);
            }

            @Override
            public void accept(QuotedBracketIndexedStringInfo info)
            {
                serializeQuotedBracketIndexedString(writer, info);
            }

            @Override
            public void accept(KeyedBracketIndexedStringInfo info)
            {
                serializeKeyedBracketIndexedString(writer, info);
            }
        };
        ArrayIterate.forEach(strings, s -> stringInfo.get(s).accept(consumer));
    }

    private void serializeSimpleString(Writer writer, SimpleStringInfo stringInfo)
    {
        byte[] bytes = stringInfo.string.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        int lenType = getIntWidth(length);
        writer.writeByte((byte) (SIMPLE_STRING | lenType));
        writeIntOfWidth(writer, length, lenType);
        writer.writeBytes(bytes);
    }

    private void serializeDelimitedString(Writer writer, DelimitedStringInfo stringInfo)
    {
        writer.writeByte((byte) (DELIMITED_STRING | stringInfo.delimeterType));
        writeString(writer, stringInfo.prefix);
        writeString(writer, stringInfo.suffix);
    }

    private void serializeSimpleBracketIndexedString(Writer writer, SimpleBracketIndexedStringInfo stringInfo)
    {
        writer.writeByte((byte) (BRACKET_INDEXED_STRING | SIMPLE_BRACKET_INDEX));
        writeString(writer, stringInfo.prefix);
        writeString(writer, stringInfo.value);
    }

    private void serializeQuotedBracketIndexedString(Writer writer, QuotedBracketIndexedStringInfo stringInfo)
    {
        writer.writeByte((byte) (BRACKET_INDEXED_STRING | QUOTED_BRACKET_INDEX));
        writeString(writer, stringInfo.prefix);
        writeString(writer, stringInfo.value);
    }

    private void serializeKeyedBracketIndexedString(Writer writer, KeyedBracketIndexedStringInfo stringInfo)
    {
        writer.writeByte((byte) (BRACKET_INDEXED_STRING | KEYED_BRACKET_INDEX));
        writeString(writer, stringInfo.prefix);
        writeString(writer, stringInfo.key);
        writeString(writer, stringInfo.value);
    }

    private static class OneByte extends StringWriterV2
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

    private static class TwoBytes extends StringWriterV2
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

    private static class ThreeBytes extends StringWriterV2
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

    private static class FourBytes extends StringWriterV2
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
        MutableMap<String, StringInfo> stringInfo = processStrings(strings);

        // Create sorted array of strings
        String[] stringArray = stringInfo.keySet().toArray(new String[stringInfo.size()]);
        Arrays.sort(stringArray, Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));

        // Build string id index
        MutableObjectIntMap<String> stringIndex = ObjectIntMaps.mutable.ofInitialCapacity(stringArray.length);
        ArrayIterate.forEachWithIndex(stringArray, stringIndex::put);
        StringWriterV2 stringWriter = newStringWriter(stringIndex);

        long buildEnd = System.nanoTime();
        LOGGER.debug("Finished building string index with {} strings in {}s", stringArray.length, (buildEnd - start) / 1_000_000_000.0);

        // Serialize string index
        stringWriter.serialize(writer, stringArray, stringInfo);

        long end = System.nanoTime();
        LOGGER.debug("Finished serializing string index with {} strings in {}s", stringArray.length, (end - buildEnd) / 1_000_000_000.0);
        LOGGER.debug("Finished writing string index with {} strings in {}s", stringArray.length, (end - start) / 1_000_000_000.0);

        return stringWriter;
    }

    private static StringWriterV2 newStringWriter(ObjectIntMap<String> stringIndex)
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

    private static MutableMap<String, StringInfo> processStrings(Iterable<? extends String> strings)
    {
        Deque<String> deque = Iterate.addAllTo(strings, new ArrayDeque<>());
        MutableMap<String, StringInfo> stringInfo = Maps.mutable.ofInitialCapacity(deque.size());
        while (!deque.isEmpty())
        {
            String string = deque.pop();
            if (isSpecialString(string) || stringInfo.containsKey(string))
            {
                // either no need to process or already processed
                continue;
            }

            if (string.length() > 1)
            {
                int lastIndex = string.length() - 1;

                // Bracket indexed strings
                int index;
                if ((string.charAt(lastIndex) == ']') && ((index = string.lastIndexOf('[')) != -1))
                {
                    String prefix = string.substring(0, index);
                    deque.push(prefix);
                    if (((lastIndex - index) > 1) && (string.charAt(lastIndex - 1) == '\''))
                    {
                        if (string.charAt(index + 1) == '\'')
                        {
                            String value = string.substring(index + 2, lastIndex - 1);
                            deque.push(value);
                            stringInfo.put(string, new QuotedBracketIndexedStringInfo(prefix, value));
                            continue;
                        }

                        int equalIndex = string.indexOf('=', index + 1);
                        if ((equalIndex != -1) && ((lastIndex - equalIndex) > 1) && (string.charAt(equalIndex + 1) == '\''))
                        {
                            String key = string.substring(index + 1, equalIndex);
                            String value = string.substring(equalIndex + 2, lastIndex - 1);
                            deque.push(key);
                            deque.push(value);
                            stringInfo.put(string, new KeyedBracketIndexedStringInfo(prefix, key, value));
                            continue;
                        }
                    }

                    String value = string.substring(index + 1, lastIndex);
                    deque.push(value);
                    stringInfo.put(string, new SimpleBracketIndexedStringInfo(prefix, value));
                    continue;
                }

                // Delimited Strings
                index = findLastDelimiter(string, lastIndex);
                if (index != -1)
                {
                    String prefix = string.substring(0, index);
                    int type;
                    String suffix;
                    switch (string.charAt(index))
                    {
                        case '.':
                        {
                            type = DOT_DELIMITER;
                            suffix = string.substring(index + 1);
                            break;
                        }
                        case '/':
                        {
                            type = SLASH_DELIMITER;
                            suffix = string.substring(index + 1);
                            break;
                        }
                        default:
                        {
                            type = PACKAGE_DELIMITER;
                            suffix = string.substring(index + 2);
                        }
                    }
                    deque.push(prefix);
                    deque.push(suffix);
                    stringInfo.put(string, new DelimitedStringInfo(type, prefix, suffix));
                    continue;
                }
            }

            stringInfo.put(string, new SimpleStringInfo(string));
        }
        return stringInfo;
    }

    private static int findLastDelimiter(String string, int from)
    {
        for (int i = from; i >= 0; i--)
        {
            switch (string.charAt(i))
            {
                case '.':
                case '/':
                {
                    return i;
                }
                case ':':
                {
                    if ((i > 0) && (string.charAt(i - 1) == ':'))
                    {
                        return i - 1;
                    }
                }
            }
        }
        return -1;
    }

    private abstract static class StringInfo
    {
        protected abstract void accept(StringInfoConsumer consumer);
    }

    private static class SimpleStringInfo extends StringInfo
    {
        private final String string;

        private SimpleStringInfo(String string)
        {
            this.string = string;
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class DelimitedStringInfo extends StringInfo
    {
        final int delimeterType;
        final String prefix;
        final String suffix;

        DelimitedStringInfo(int delimeterType, String prefix, String suffix)
        {
            this.delimeterType = delimeterType;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private abstract static class BracketIndexedStringInfo extends StringInfo
    {
        final String prefix;
        final String value;

        BracketIndexedStringInfo(String prefix, String value)
        {
            this.prefix = prefix;
            this.value = value;
        }
    }

    private static class SimpleBracketIndexedStringInfo extends BracketIndexedStringInfo
    {
        SimpleBracketIndexedStringInfo(String prefix, String value)
        {
            super(prefix, value);
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class QuotedBracketIndexedStringInfo extends BracketIndexedStringInfo
    {
        QuotedBracketIndexedStringInfo(String prefix, String value)
        {
            super(prefix, value);
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class KeyedBracketIndexedStringInfo extends BracketIndexedStringInfo
    {
        final String key;

        KeyedBracketIndexedStringInfo(String prefix, String key, String value)
        {
            super(prefix, value);
            this.key = key;
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private interface StringInfoConsumer
    {
        void accept(SimpleStringInfo info);

        void accept(DelimitedStringInfo info);

        void accept(SimpleBracketIndexedStringInfo info);

        void accept(QuotedBracketIndexedStringInfo info);

        void accept(KeyedBracketIndexedStringInfo info);
    }
}
