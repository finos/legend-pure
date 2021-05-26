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

package org.finos.legend.pure.m4.serialization.binary;

import org.finos.legend.pure.m4.serialization.Writer;

public abstract class AbstractBinaryWriter extends AbstractBinaryReaderWriter implements Writer
{
    @Override
    public synchronized void writeByteArray(byte[] bytes)
    {
        writeInt(bytes.length);
        writeBytes(bytes);
    }

    @Override
    public synchronized void writeBoolean(boolean b)
    {
        writeByte(b ? TRUE_BYTE : FALSE_BYTE);
    }

    @Override
    public synchronized void writeShortArray(short[] shorts)
    {
        int length = shorts.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeShort(shorts[i]);
        }
    }

    @Override
    public synchronized void writeIntArray(int[] ints)
    {
        int length = ints.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeInt(ints[i]);
        }
    }

    @Override
    public synchronized void writeLongArray(long[] longs)
    {
        int length = longs.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeLong(longs[i]);
        }
    }

    @Override
    public synchronized void writeFloatArray(float[] floats)
    {
        int length = floats.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeFloat(floats[i]);
        }
    }

    @Override
    public synchronized void writeDoubleArray(double[] doubles)
    {
        int length = doubles.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeDouble(doubles[i]);
        }
    }

    @Override
    public synchronized void writeString(String string)
    {
        writeByteArray(stringToByteArray(string));
    }

    @Override
    public synchronized void writeStringArray(String[] strings)
    {
        int length = strings.length;
        writeInt(length);
        for (int i = 0; i < length; i++)
        {
            writeString(strings[i]);
        }
    }
}
