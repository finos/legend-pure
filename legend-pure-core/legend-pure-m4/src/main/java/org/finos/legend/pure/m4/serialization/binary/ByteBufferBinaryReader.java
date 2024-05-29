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

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

class ByteBufferBinaryReader extends AbstractBinaryReader
{
    private final ByteBuffer buffer;

    ByteBufferBinaryReader(ByteBuffer buffer)
    {
        this.buffer = Objects.requireNonNull(buffer, "buffer may not be null");
    }

    @Override
    public synchronized byte readByte()
    {
        try
        {
            return this.buffer.get();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Byte.BYTES, 0L);
        }
    }

    @Override
    public synchronized byte[] readBytes(byte[] bytes, int offset, int n)
    {
        checkByteArray(bytes, offset, n);
        try
        {
            this.buffer.get(bytes, offset, n);
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(n, this.buffer.remaining());
        }
        return bytes;
    }

    @Override
    public synchronized void skipBytes(long n)
    {
        if (n <= 0)
        {
            return;
        }

        if (this.buffer.remaining() < n)
        {
            throw new UnexpectedEndException(n, this.buffer.remaining());
        }

        int newPosition = this.buffer.position() + (int) n;
        ((Buffer) this.buffer).position(newPosition);
    }

    @Override
    public synchronized short readShort()
    {
        try
        {
            return this.buffer.getShort();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Short.BYTES, this.buffer.remaining());
        }
    }

    @Override
    public synchronized int readInt()
    {
        try
        {
            return this.buffer.getInt();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Integer.BYTES, this.buffer.remaining());
        }
    }

    @Override
    public synchronized long readLong()
    {
        try
        {
            return this.buffer.getLong();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Long.BYTES, this.buffer.remaining());
        }
    }

    @Override
    public synchronized float readFloat()
    {
        try
        {
            return this.buffer.getFloat();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Float.BYTES, this.buffer.remaining());
        }
    }

    @Override
    public synchronized double readDouble()
    {
        try
        {
            return this.buffer.getDouble();
        }
        catch (BufferUnderflowException e)
        {
            throw new UnexpectedEndException(Double.BYTES, this.buffer.remaining());
        }
    }

    @Override
    public synchronized String readString()
    {
        if (!this.buffer.hasArray())
        {
            return byteArrayToString(readByteArray());
        }

        int length = readInt();
        if (this.buffer.remaining() < length)
        {
            throw new UnexpectedEndException(length, this.buffer.remaining());
        }
        int offset = this.buffer.position();
        String string = byteArrayToString(this.buffer.array(), offset, length);
        ((Buffer) this.buffer).position(offset + length);
        return string;
    }

    @Override
    public synchronized void close()
    {
        // Do nothing
    }
}
