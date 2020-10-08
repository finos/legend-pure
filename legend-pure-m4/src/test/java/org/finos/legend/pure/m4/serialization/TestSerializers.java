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

package org.finos.legend.pure.m4.serialization;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public abstract class TestSerializers
{
    @Test
    public void testWriteReadByte() throws IOException
    {
        for (int expected = Byte.MIN_VALUE; expected <= Byte.MAX_VALUE; expected++)
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeByte((byte)expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                byte actual = reader.readByte();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteReadBytes() throws IOException
    {
        byte[] expected = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, -1, 55};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeBytes(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            byte[] actual = reader.readBytes(expected.length);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipBytes() throws IOException
    {
        byte[] array1 = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, -1, 55};
        byte[] array2 = {5, 2, -23, 1};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeBytes(array1);
            writer.writeBytes(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipBytes(array1.length);
            byte[] actual = reader.readBytes(array2.length);
            Assert.assertArrayEquals(array2, actual);
        }
    }

    @Test
    public void testWriteReadByteArray() throws IOException
    {
        byte[] expected = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, -1, 55};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeByteArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            byte[] actual = reader.readByteArray();
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipByteArray() throws IOException
    {
        byte[] array1 = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, -1, 55};
        byte[] array2 = {5, 2, -23, 1};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeByteArray(array1);
            writer.writeByteArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipByteArray();
            byte[] actual = reader.readByteArray();
            Assert.assertArrayEquals(array2, actual);
        }
    }

    @Test
    public void testWriteReadBoolean() throws IOException
    {
        for (boolean expected : new boolean[] {true, false})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeBoolean(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                boolean actual = reader.readBoolean();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteSkipBoolean() throws IOException
    {
        for (boolean expected : new boolean[] {true, false})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeBoolean(!expected);
                writer.writeBoolean(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipBoolean();
                boolean actual = reader.readBoolean();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteReadShort() throws IOException
    {
        for (short expected : new short[]{Short.MIN_VALUE, Short.MAX_VALUE, 0, -1, 10, -23498, 3429})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeShort(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                short actual = reader.readShort();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteSkipShort() throws IOException
    {
        short expected = 7;
        for (short toSkip : new short[]{Short.MIN_VALUE, Short.MAX_VALUE, 0, -1, 10, -23498, 3429})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeShort(toSkip);
                writer.writeShort(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipShort();
                short actual = reader.readShort();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteReadShortArray() throws IOException
    {
        short[] expected = {Short.MIN_VALUE, Short.MAX_VALUE, 0, -1, 10, -23498, 3429};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeShortArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            short[] actual = reader.readShortArray();
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipShortArray() throws IOException
    {
        short[] array1 = {Short.MIN_VALUE, Short.MAX_VALUE, 0, -1, 10, -23498, 3429};
        short[] array2 = {5, 2, -2347, 1};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeShortArray(array1);
            writer.writeShortArray(array2);
        }

        try (Reader reader = writerReader.getReader())
        {
            reader.skipShortArray();
            short[] actual = reader.readShortArray();
            Assert.assertArrayEquals(array2, actual);
        }
    }

    @Test
    public void testWriteReadInt() throws IOException
    {
        for (int expected : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 34298735})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeInt(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                int actual = reader.readInt();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteSkipInt() throws IOException
    {
        int expected = 17;
        for (int toSkip : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 34298735})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeInt(toSkip);
                writer.writeInt(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipInt();
                int actual = reader.readInt();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteReadIntArray() throws IOException
    {
        int[] expected = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 34298735};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeIntArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            int[] actual = reader.readIntArray();
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipIntArray() throws IOException
    {
        int[] array1 = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 34298735};
        int[] array2 = {5, 2, -234789235, 1};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeIntArray(array1);
            writer.writeIntArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipIntArray();
            int[] actual = reader.readIntArray();
            Assert.assertArrayEquals(array2, actual);
        }
    }

    @Test
    public void testWriteReadLong() throws IOException
    {
        for (long expected : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 342987352323434L})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeLong(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                long actual = reader.readLong();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteSkipLong() throws IOException
    {
        long expected = -31;
        for (long toSkip : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 342987352323434L})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeLong(toSkip);
                writer.writeLong(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipLong();
                long actual = reader.readLong();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteReadLongArray() throws IOException
    {
        long[] expected = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 342987352323434L};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeLongArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            long[] actual = reader.readLongArray();
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipLongArray() throws IOException
    {
        long[] array1 = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 10, -234987973, 342987352323434L};
        long[] array2 = {5, 2, -234789235, 1};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeLongArray(array1);
            writer.writeLongArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipLongArray();
            long[] actual = reader.readLongArray();
            Assert.assertArrayEquals(array2, actual);
        }
    }

    @Test
    public void testWriteReadFloat() throws IOException
    {
        for (float expected : new float[]{Float.MIN_VALUE, Float.MAX_VALUE, 0.0f, -0.0f, -1.39f, 10.0f, -234987973e23f, 3.4298735f})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeFloat(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                float actual = reader.readFloat();
                Assert.assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
            }
        }
    }

    @Test
    public void testWriteSkipFloat() throws IOException
    {
        float expected = -3.14159f;
        for (float toSkip : new float[]{Float.MIN_VALUE, Float.MAX_VALUE, 0.0f, -0.0f, -1.39f, 10.0f, -234987973e23f, 3.4298735f})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeFloat(toSkip);
                writer.writeFloat(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipFloat();
                float actual = reader.readFloat();
                Assert.assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
            }
        }
    }

    @Test
    public void testWriteReadFloatArray() throws IOException
    {
        float[] expected = {Float.MIN_VALUE, Float.MAX_VALUE, 0.0f, -0.0f, -1.39f, 10.0f, -234987973e23f, 3.4298735f};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeFloatArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            float[] actual = reader.readFloatArray();
            Assert.assertArrayEquals(convertFloatArrayToIntBits(expected), convertFloatArrayToIntBits(actual));
        }
    }

    @Test
    public void testWriteSkipFloatArray() throws IOException
    {
        float[] array1 = {Float.MIN_VALUE, Float.MAX_VALUE, 0.0f, -0.0f, -1.39f, 10.0f, -234987973e23f, 3.4298735f};
        float[] array2 = {5.7782413f, 2.0f, -234789235.000000000000000009f, -0.0f};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeFloatArray(array1);
            writer.writeFloatArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipFloatArray();
            float[] actual = reader.readFloatArray();
            Assert.assertArrayEquals(convertFloatArrayToIntBits(array2), convertFloatArrayToIntBits(actual));
        }
    }

    private int[] convertFloatArrayToIntBits(float[] floats)
    {
        int length = floats.length;
        int[] intBits = new int[length];
        for (int i = 0; i < length; i++)
        {
            intBits[i] = Float.floatToRawIntBits(floats[i]);
        }
        return intBits;
    }

    @Test
    public void testWriteReadDouble() throws IOException
    {
        for (double expected : new double[]{Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, -1.39, 10.0, -234987973e234, 3.4298735})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeDouble(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                double actual = reader.readDouble();
                Assert.assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
            }
        }
    }

    @Test
    public void testWriteSkipDouble() throws IOException
    {
        double expected = 3.14159;
        for (double toSkip : new double[]{Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, -1.39, 10.0, -234987973e234, 3.4298735})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeDouble(toSkip);
                writer.writeDouble(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                reader.skipDouble();
                double actual = reader.readDouble();
                Assert.assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
            }
        }
    }

    @Test
    public void testWriteReadDoubleArray() throws IOException
    {
        double[] expected = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, -1.39, 10.0, -234987973e234, 3.4298735};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeDoubleArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            double[] actual = reader.readDoubleArray();
            Assert.assertArrayEquals(convertDoubleArrayToLongBits(expected), convertDoubleArrayToLongBits(actual));
        }
    }

    @Test
    public void testWriteSkipDoubleArray() throws IOException
    {
        double[] array1 = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, -1.39, 10.0, -234987973e234, 3.4298735};
        double[] array2 = {5.7782413, 2.0, -234789235.000000000000000009, -0.0};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeDoubleArray(array1);
            writer.writeDoubleArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipDoubleArray();
            double[] actual = reader.readDoubleArray();
            Assert.assertArrayEquals(convertDoubleArrayToLongBits(array2), convertDoubleArrayToLongBits(actual));
        }
    }

    private long[] convertDoubleArrayToLongBits(double[] doubles)
    {
        int length = doubles.length;
        long[] longBits = new long[length];
        for (int i = 0; i < length; i++)
        {
            longBits[i] = Double.doubleToRawLongBits(doubles[i]);
        }
        return longBits;
    }

    @Test
    public void testWriteReadString() throws IOException
    {
        for (String expected : new String[]{"", "the quick brown fox", "JUMPS OVER THE LAZY DOG", "hello\u2022world"})
        {
            WriterReader writerReader = newWriterReader();
            try (Writer writer = writerReader.getWriter())
            {
                writer.writeString(expected);
            }
            try (Reader reader = writerReader.getReader())
            {
                String actual = reader.readString();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteSkipString() throws IOException
    {
        String string1 = "the quick\u2022brown fox";
        String string2 = "JUMPS OVER THE\u2015LAZY DOG";
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeString(string1);
            writer.writeString(string2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipString();
            String actual = reader.readString();
            Assert.assertEquals(string2, actual);
        }
    }

    @Test
    public void testWriteReadStringArray() throws IOException
    {
        String[] expected = {"", "the quick brown fox", "JUMPS OVER THE LAZY DOG", "hello\u2022world"};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeStringArray(expected);
        }
        try (Reader reader = writerReader.getReader())
        {
            String[] actual = reader.readStringArray();
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testWriteSkipStringArray() throws IOException
    {
        String[] array1 = {"", "the quick brown fox", "JUMPS OVER THE LAZY DOG", "hello\u2022world"};
        String[] array2 = {"a different", "array", "of strings", "but still\u2015with unicode"};
        WriterReader writerReader = newWriterReader();
        try (Writer writer = writerReader.getWriter())
        {
            writer.writeStringArray(array1);
            writer.writeStringArray(array2);
        }
        try (Reader reader = writerReader.getReader())
        {
            reader.skipStringArray();
            String[] actual = reader.readStringArray();
            Assert.assertArrayEquals(array2, actual);
        }
    }

    protected abstract WriterReader newWriterReader() throws IOException;

    protected interface WriterReader
    {
        Writer getWriter() throws IOException;
        Reader getReader() throws IOException;
    }
}
