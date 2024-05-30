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

import org.finos.legend.pure.m3.serialization.compiler.strings.StringReader;
import org.finos.legend.pure.m4.serialization.Reader;

abstract class StringReaderV1 extends BaseStringIndex implements StringReader
{
    private final String[] strings;

    private StringReaderV1(String[] strings)
    {
        this.strings = strings;
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

    private static class OneByte extends StringReaderV1
    {
        private OneByte(String[] strings)
        {
            super(strings);
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

    private static class TwoBytes extends StringReaderV1
    {
        private TwoBytes(String[] strings)
        {
            super(strings);
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

    private static class ThreeBytes extends StringReaderV1
    {
        private ThreeBytes(String[] strings)
        {
            super(strings);
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

    private static class FourBytes extends StringReaderV1
    {
        private FourBytes(String[] strings)
        {
            super(strings);
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

    private static class ByteId extends StringReaderV1
    {
        private ByteId(String[] strings)
        {
            super(strings);
        }

        @Override
        public void skipString(Reader reader)
        {
            reader.skipBytes(Byte.BYTES);
        }

        @Override
        public String[] readStringArray(Reader reader)
        {
            byte[] ids = reader.readByteArray();
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
            reader.skipByteArray();
        }

        @Override
        protected int readStringId(Reader reader)
        {
            return reader.readByte();
        }
    }

    private static class ShortId extends StringReaderV1
    {
        private ShortId(String[] strings)
        {
            super(strings);
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
                strings[i] = getString(ids[i]);
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
            return reader.readShort();
        }
    }

    private static class IntId extends StringReaderV1
    {
        private IntId(String[] strings)
        {
            super(strings);
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
        String[] strings = reader.readStringArray();
        int width = getStringIdByteWidth(strings.length);
        switch (width)
        {
            case 1:
            {
                return new OneByte(strings);
            }
            case 2:
            {
                return new TwoBytes(strings);
            }
            case 3:
            {
                return new ThreeBytes(strings);
            }
            case 4:
            {
                return new FourBytes(strings);
            }
            default:
            {
                throw new RuntimeException("Unsupported string id byte width: " + width);
            }
        }
    }
}
