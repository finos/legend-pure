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

public interface Reader extends Closeable
{
    byte readByte();

    byte[] readBytes(int n);

    void skipBytes(long n);

    byte[] readByteArray();

    void skipByteArray();

    boolean readBoolean();

    void skipBoolean();

    short readShort();

    void skipShort();

    short[] readShortArray();

    void skipShortArray();

    int readInt();

    void skipInt();

    int[] readIntArray();

    void skipIntArray();

    long readLong();

    void skipLong();

    long[] readLongArray();

    void skipLongArray();

    float readFloat();

    void skipFloat();

    float[] readFloatArray();

    void skipFloatArray();

    double readDouble();

    void skipDouble();

    double[] readDoubleArray();

    void skipDoubleArray();

    String readString();

    void skipString();

    String[] readStringArray();

    void skipStringArray();

    @Override
    void close();
}
