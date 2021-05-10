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

import java.nio.ByteBuffer;

public abstract class AbstractSimpleBinaryReader extends AbstractBinaryReader
{
    protected final byte[] eightBytes = new byte[8];
    protected final ByteBuffer eightByteBuffer = ByteBuffer.wrap(this.eightBytes);

    @Override
    public synchronized byte readByte()
    {
        return readOneByte();
    }

    @Override
    public synchronized byte[] readBytes(int n)
    {
        byte[] bytes = new byte[n];
        readNBytes(n, bytes, 0);
        return bytes;
    }

    @Override
    public synchronized short readShort()
    {
        readNBytes(Short.BYTES, this.eightBytes, 0);
        return this.eightByteBuffer.getShort(0);
    }

    @Override
    public synchronized int readInt()
    {
        readNBytes(Integer.BYTES, this.eightBytes, 0);
        return this.eightByteBuffer.getInt(0);
    }

    @Override
    public synchronized long readLong()
    {
        readNBytes(Long.BYTES, this.eightBytes, 0);
        return this.eightByteBuffer.getLong(0);
    }

    @Override
    public synchronized float readFloat()
    {
        readNBytes(Float.BYTES, this.eightBytes, 0);
        return this.eightByteBuffer.getFloat(0);
    }

    @Override
    public synchronized double readDouble()
    {
        readNBytes(Double.BYTES, this.eightBytes, 0);
        return this.eightByteBuffer.getDouble(0);
    }

    /**
     * Read and return a single byte.  This must throw an
     * exception if a byte cannot be read.
     *
     * @return byte
     */
    protected abstract byte readOneByte();

    /**
     * Read the given number of bytes into the given array
     * starting at the given offset.  This must read exactly
     * the requested number of bytes or throw an exception.
     *
     * @param n      number of bytes to read
     * @param bytes  array to read bytes into
     * @param offset array offset
     */
    protected abstract void readNBytes(int n, byte[] bytes, int offset);
}
