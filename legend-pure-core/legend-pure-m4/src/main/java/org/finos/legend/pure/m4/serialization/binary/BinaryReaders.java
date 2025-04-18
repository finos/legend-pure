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

import org.eclipse.collections.api.list.primitive.ByteList;
import org.finos.legend.pure.m4.serialization.Reader;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class BinaryReaders
{
    private BinaryReaders()
    {
    }

    public static Reader newBinaryReader(ReadableByteChannel byteChannel)
    {
        return newBinaryReader(byteChannel, true);
    }

    public static Reader newBinaryReader(ReadableByteChannel byteChannel, boolean closeChannelOnClose)
    {
        return new ReadableByteChannelReader(byteChannel, closeChannelOnClose);
    }

    public static Reader newBinaryReader(InputStream stream)
    {
        return newBinaryReader(stream, true);
    }

    public static Reader newBinaryReader(InputStream stream, boolean closeStreamOnClose)
    {
        return new StreamBinaryReader(stream, closeStreamOnClose);
    }

    public static Reader newBinaryReader(ByteBuffer buffer)
    {
        return new ByteBufferBinaryReader(buffer);
    }

    public static Reader newBinaryReader(byte[] bytes, int offset, int length)
    {
        return newBinaryReader(ByteBuffer.wrap(bytes, offset, length));
    }

    public static Reader newBinaryReader(byte[] bytes)
    {
        return newBinaryReader(bytes, 0, bytes.length);
    }

    public static Reader newBinaryReader(ByteList bytes)
    {
        return new ByteListBinaryReader(bytes);
    }
}
