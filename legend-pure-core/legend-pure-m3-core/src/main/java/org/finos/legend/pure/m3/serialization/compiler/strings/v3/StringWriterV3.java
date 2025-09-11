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

package org.finos.legend.pure.m3.serialization.compiler.strings.v3;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
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

abstract class StringWriterV3 extends BaseStringIndex implements StringWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StringWriterV3.class);

    private final ObjectIntMap<String> stringIndex;

    private StringWriterV3(ObjectIntMap<String> stringIndex)
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
            public void accept(SourcePathStringInfo info)
            {
                serializeSourcePathString(writer, info);
            }

            @Override
            public void accept(PackagePathStringInfo info)
            {
                serializePackagePathString(writer, info);
            }

            @Override
            public void accept(DotDelimitedStringInfo info)
            {
                serializeDotDelimitedString(writer, info);
            }

            @Override
            public void accept(ImportGroupStringInfo info)
            {
                serializeImportGroupString(writer, info);
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
        int lenWidth = getIntWidth(length);
        writer.writeByte((byte) (SIMPLE_STRING | lenWidth));
        writeIntOfWidth(writer, length, lenWidth);
        writer.writeBytes(bytes);
    }

    private void serializeSourcePathString(Writer writer, SourcePathStringInfo stringInfo)
    {
        serializeDelimitedString(writer, stringInfo, SOURCE_PATH_STRING);
    }

    private void serializePackagePathString(Writer writer, PackagePathStringInfo stringInfo)
    {
        serializeDelimitedString(writer, stringInfo, PACKAGE_PATH_STRING);
    }

    private void serializeDotDelimitedString(Writer writer, DotDelimitedStringInfo stringInfo)
    {
        serializeDelimitedString(writer, stringInfo, DOT_DELIMITED_STRING);
    }

    private void serializeImportGroupString(Writer writer, ImportGroupStringInfo stringInfo)
    {
        serializeDelimitedString(writer, stringInfo, IMPORT_GROUP_STRING);
    }

    private void serializeDelimitedString(Writer writer, DelimitedStringInfo stringInfo, int typeCode)
    {
        int size = stringInfo.strings.size();
        int sizeWidth = getIntWidth(size);
        writer.writeByte((byte) (typeCode | sizeWidth));
        writeIntOfWidth(writer, size, sizeWidth);
        stringInfo.strings.forEach(s -> writeString(writer, s));
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

    private static class OneByte extends StringWriterV3
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

    private static class TwoBytes extends StringWriterV3
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

    private static class ThreeBytes extends StringWriterV3
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

    private static class FourBytes extends StringWriterV3
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
        StringWriterV3 stringWriter = newStringWriter(stringIndex);

        long buildEnd = System.nanoTime();
        LOGGER.debug("Finished building string index with {} strings in {}s", stringArray.length, (buildEnd - start) / 1_000_000_000.0);

        // Serialize string index
        stringWriter.serialize(writer, stringArray, stringInfo);

        long end = System.nanoTime();
        LOGGER.debug("Finished serializing string index with {} strings in {}s", stringArray.length, (end - buildEnd) / 1_000_000_000.0);
        LOGGER.debug("Finished writing string index with {} strings in {}s", stringArray.length, (end - start) / 1_000_000_000.0);

        return stringWriter;
    }

    private static StringWriterV3 newStringWriter(ObjectIntMap<String> stringIndex)
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
                if (string.charAt(0) == '/')
                {
                    ListIterable<String> parts = splitOnDelimiter(string, '/', 1);
                    possiblyPushAll(deque, stringInfo, parts);
                    stringInfo.put(string, new SourcePathStringInfo(parts));
                    continue;
                }

                if (string.startsWith("import") && (string.indexOf('_', "import".length()) != -1))
                {
                    ListIterable<String> parts = splitOnDelimiter(string, '_');
                    possiblyPushAll(deque, stringInfo, parts);
                    stringInfo.put(string, new ImportGroupStringInfo(parts));
                    continue;
                }

                if (string.indexOf('.') != -1)
                {
                    ListIterable<String> parts = splitOnDelimiter(string, '.');
                    possiblyPushAll(deque, stringInfo, parts);
                    stringInfo.put(string, new DotDelimitedStringInfo(parts));
                    continue;
                }

                if (string.contains("::"))
                {
                    ListIterable<String> parts = PackageableElement.splitUserPath(string);
                    possiblyPushAll(deque, stringInfo, parts);
                    stringInfo.put(string, new PackagePathStringInfo(parts));
                    continue;
                }

                int lastIndex = string.length() - 1;

                // Bracket indexed strings
                int index;
                if ((string.charAt(lastIndex) == ']') && ((index = string.lastIndexOf('[')) != -1))
                {
                    String prefix = string.substring(0, index);
                    possiblyPush(deque, stringInfo, prefix);
                    if (((lastIndex - index) > 1) && (string.charAt(lastIndex - 1) == '\''))
                    {
                        if (string.charAt(index + 1) == '\'')
                        {
                            String value = string.substring(index + 2, lastIndex - 1);
                            possiblyPush(deque, stringInfo, value);
                            stringInfo.put(string, new QuotedBracketIndexedStringInfo(prefix, value));
                            continue;
                        }

                        int equalIndex = string.indexOf('=', index + 1);
                        if ((equalIndex != -1) && ((lastIndex - equalIndex) > 1) && (string.charAt(equalIndex + 1) == '\''))
                        {
                            String key = string.substring(index + 1, equalIndex);
                            String value = string.substring(equalIndex + 2, lastIndex - 1);
                            possiblyPush(deque, stringInfo, key);
                            possiblyPush(deque, stringInfo, value);
                            stringInfo.put(string, new KeyedBracketIndexedStringInfo(prefix, key, value));
                            continue;
                        }
                    }

                    String value = string.substring(index + 1, lastIndex);
                    possiblyPush(deque, stringInfo, value);
                    stringInfo.put(string, new SimpleBracketIndexedStringInfo(prefix, value));
                    continue;
                }
            }

            stringInfo.put(string, new SimpleStringInfo(string));
        }
        return stringInfo;
    }

    private static void possiblyPushAll(Deque<String> deque, MapIterable<String, StringInfo> stringInfo, Iterable<? extends String> strings)
    {
        strings.forEach(s -> possiblyPush(deque, stringInfo, s));
    }

    private static void possiblyPush(Deque<String> deque, MapIterable<String, StringInfo> stringInfo, String string)
    {
        if (!isSpecialString(string) && !stringInfo.containsKey(string))
        {
            deque.push(string);
        }
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

    private abstract static class DelimitedStringInfo extends StringInfo
    {
        final ListIterable<String> strings;

        DelimitedStringInfo(ListIterable<String> strings)
        {
            this.strings = strings;
        }
    }

    private static class SourcePathStringInfo extends DelimitedStringInfo
    {
        SourcePathStringInfo(ListIterable<String> strings)
        {
            super(strings);
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class PackagePathStringInfo extends DelimitedStringInfo
    {
        PackagePathStringInfo(ListIterable<String> strings)
        {
            super(strings);
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class DotDelimitedStringInfo extends DelimitedStringInfo
    {
        DotDelimitedStringInfo(ListIterable<String> strings)
        {
            super(strings);
        }

        @Override
        protected void accept(StringInfoConsumer consumer)
        {
            consumer.accept(this);
        }
    }

    private static class ImportGroupStringInfo extends DelimitedStringInfo
    {
        ImportGroupStringInfo(ListIterable<String> strings)
        {
            super(strings);
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

        void accept(SourcePathStringInfo info);

        void accept(PackagePathStringInfo info);

        void accept(DotDelimitedStringInfo info);

        void accept(ImportGroupStringInfo info);

        void accept(SimpleBracketIndexedStringInfo info);

        void accept(QuotedBracketIndexedStringInfo info);

        void accept(KeyedBracketIndexedStringInfo info);
    }

    private static ListIterable<String> splitOnDelimiter(String string, char delimiter)
    {
        return splitOnDelimiter(string, delimiter, 0);
    }

    private static ListIterable<String> splitOnDelimiter(String string, char delimiter, int start)
    {
        MutableList<String> strings = Lists.mutable.empty();
        int index;
        while ((index = string.indexOf(delimiter, start)) != -1)
        {
            strings.add(string.substring(start, index));
            start = index + 1;
        }
        strings.add(string.substring(start));
        return strings;
    }
}
