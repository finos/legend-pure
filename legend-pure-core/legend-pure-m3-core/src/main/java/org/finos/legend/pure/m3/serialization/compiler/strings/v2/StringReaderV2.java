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

import org.finos.legend.pure.m3.serialization.compiler.strings.StringReader;
import org.finos.legend.pure.m4.serialization.Reader;

import java.nio.charset.StandardCharsets;

abstract class StringReaderV2 extends BaseStringIndex implements StringReader
{
    private final String[] strings;

    private StringReaderV2(String[] strings)
    {
        this.strings = strings;
    }

    private StringReaderV2(int length)
    {
        this(new String[length]);
    }

    @Override
    public String readString(Reader reader)
    {
        int id = readStringId(reader);
        return getString(id);
    }

    protected abstract int readStringId(Reader reader);

    protected String getString(int id)
    {
        if (isSpecialStringId(id))
        {
            return getSpecialString(id);
        }
        if ((id < 0) || (id >= this.strings.length))
        {
            throw new IllegalArgumentException("Unknown string: " + id);
        }
        return this.strings[id];
    }

    private void deserialize(Reader reader)
    {
        for (int i = 0, len = this.strings.length; i < len; i++)
        {
            int code = reader.readByte();
            switch (code & STRING_TYPE_MASK)
            {
                case SIMPLE_STRING:
                {
                    int length = readIntOfWidth(reader, code);
                    byte[] bytes = reader.readBytes(length);
                    this.strings[i] = new String(bytes, StandardCharsets.UTF_8);
                    break;
                }
                case DELIMITED_STRING:
                {
                    String prefix = readString(reader);
                    String suffix = readString(reader);
                    String string;
                    switch (code & DELIMITER_TYPE_MASK)
                    {
                        case PACKAGE_DELIMITER:
                        {
                            string = prefix + "::" + suffix;
                            break;
                        }
                        case SLASH_DELIMITER:
                        {
                            string = prefix + '/' + suffix;
                            break;
                        }
                        case DOT_DELIMITER:
                        {
                            string = prefix + '.' + suffix;
                            break;
                        }
                        default:
                        {
                            throw new RuntimeException(String.format("Unknown string delimiter type code: %02x", code & DELIMITER_TYPE_MASK));
                        }
                    }
                    this.strings[i] = string;
                    break;
                }
                case BRACKET_INDEXED_STRING:
                {
                    String prefix = readString(reader);
                    String string;
                    switch (code & BRACKET_INDEX_TYPE_MASK)
                    {
                        case SIMPLE_BRACKET_INDEX:
                        {
                            String value = readString(reader);
                            string = prefix + '[' + value + ']';
                            break;
                        }
                        case QUOTED_BRACKET_INDEX:
                        {
                            String value = readString(reader);
                            string = prefix + "['" + value + "']";
                            break;
                        }
                        case KEYED_BRACKET_INDEX:
                        {
                            String key = readString(reader);
                            String value = readString(reader);
                            string = prefix + '[' + key + "='" + value + "']";
                            break;
                        }
                        default:
                        {
                            throw new RuntimeException(String.format("Unknown bracket indexed string type code: %02x", code & BRACKET_INDEX_TYPE_MASK));
                        }
                    }
                    this.strings[i] = string;
                    break;
                }
                default:
                {
                    throw new RuntimeException(String.format("Unknown string type code: %02x", code & STRING_TYPE_MASK));
                }
            }
        }
    }

    private static class OneByte extends StringReaderV2
    {
        private OneByte(int length)
        {
            super(length);
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipBytes(1);
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            byte[] ids = reader.readByteArray();
            int length = ids.length;
            String[] strings = new String[length];
            for (int i = 0; i < length; i++)
            {
                strings[i] = getString(oneByteToId(ids[i]));
            }
            return strings;
        }

        @Override
        public void skipStringArray(Reader reader)
        {
            reader.skipByteArray();
        }

        @Override
        protected int readStringId(Reader reader)
        {
            return oneByteToId(reader.readByte());
        }
    }

    private static class TwoBytes extends StringReaderV2
    {
        private TwoBytes(int length)
        {
            super(length);
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipShort();
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            short[] ids = reader.readShortArray();
            int length = ids.length;
            String[] strings = new String[length];
            for (int i = 0; i < length; i++)
            {
                strings[i] = getString(twoBytesToId(ids[i]));
            }
            return strings;
        }

        @Override
        public void skipStringArray(Reader reader)
        {
            reader.skipShortArray();
        }

        @Override
        protected int readStringId(Reader reader)
        {
            return twoBytesToId(reader.readShort());
        }
    }

    private static class ThreeBytes extends StringReaderV2
    {
        private ThreeBytes(int length)
        {
            super(length);
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipBytes(3);
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            byte[] bytes = reader.readByteArray();
            int length = bytes.length / 3;
            String[] strings = new String[length];
            for (int i = 0, j = 0; i < length; i++, j += 3)
            {
                strings[i] = getString(threeBytesToId(bytes, j));
            }
            return strings;
        }

        @Override
        public void skipStringArray(Reader reader)
        {
            reader.skipByteArray();
        }

        @Override
        protected int readStringId(Reader reader)
        {
            return threeBytesToId(reader.readBytes(3));
        }
    }

    private static class FourBytes extends StringReaderV2
    {
        private FourBytes(int length)
        {
            super(length);
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipInt();
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            int[] ids = reader.readIntArray();
            int length = ids.length;
            String[] strings = new String[length];
            for (int i = 0; i < length; i++)
            {
                strings[i] = getString(ids[i]);
            }
            return strings;
        }

        @Override
        public void skipStringArray(Reader reader)
        {
            reader.skipIntArray();
        }

        @Override
        protected int readStringId(Reader reader)
        {
            return reader.readInt();
        }
    }

    static StringReader readStringIndex(Reader reader)
    {
        int count = reader.readInt();
        StringReaderV2 stringReader = newStringReader(count);
        stringReader.deserialize(reader);
        return stringReader;
    }

    private static StringReaderV2 newStringReader(int count)
    {
        int width = getStringIdByteWidth(count);
        switch (width)
        {
            case 1:
            {
                return new OneByte(count);
            }
            case 2:
            {
                return new TwoBytes(count);
            }
            case 3:
            {
                return new ThreeBytes(count);
            }
            case 4:
            {
                return new FourBytes(count);
            }
            default:
            {
                throw new RuntimeException("Unsupported string id byte width: " + width);
            }
        }
    }
}
