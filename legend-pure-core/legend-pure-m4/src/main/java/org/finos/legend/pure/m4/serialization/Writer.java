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

import java.io.Closeable;

public interface Writer extends Closeable
{
    void writeByte(byte b);

    default void writeBytes(byte[] bytes)
    {
        writeBytes(bytes, 0, bytes.length);
    }

    void writeBytes(byte[] bytes, int offset, int len);

    void writeByteArray(byte[] bytes);

    void writeBoolean(boolean b);

    void writeShort(short s);

    void writeShortArray(short[] shorts);

    void writeInt(int i);

    void writeIntArray(int[] ints);

    void writeLong(long l);

    void writeLongArray(long[] longs);

    void writeFloat(float f);

    void writeFloatArray(float[] floats);

    void writeDouble(double d);

    void writeDoubleArray(double[] doubles);

    void writeString(String string);

    void writeStringArray(String[] strings);

    @Override
    void close();
}
