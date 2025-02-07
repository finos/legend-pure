// Copyright 2023 Goldman Sachs
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

class ReadableByteChannelReader extends AbstractBinaryReader
{
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Math.max(Long.BYTES, Double.BYTES));
    private final ReadableByteChannel byteChannel;
    private final boolean closeChannelOnClose;

    ReadableByteChannelReader(ReadableByteChannel byteChannel, boolean closeChannelOnClose)
    {
        this.byteChannel = Objects.requireNonNull(byteChannel, "byteChannel may not be null");
        this.closeChannelOnClose = closeChannelOnClose;
    }

    @Override
    public synchronized void close()
    {
        if (this.closeChannelOnClose)
        {
            try
            {
                this.byteChannel.close();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public synchronized byte readByte()
    {
        this.byteBuffer.rewind().limit(Byte.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.get(0);
    }

    @Override
    public synchronized byte[] readBytes(byte[] bytes, int offset, int n)
    {
        checkByteArray(bytes, offset, n);
        fillBuffer(ByteBuffer.wrap(bytes, offset, n));
        return bytes;
    }

    @Override
    public synchronized short readShort()
    {
        this.byteBuffer.rewind().limit(Short.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.getShort(0);
    }

    @Override
    public synchronized int readInt()
    {
        this.byteBuffer.rewind().limit(Integer.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.getInt(0);
    }

    @Override
    public synchronized long readLong()
    {
        this.byteBuffer.rewind().limit(Long.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.getLong(0);
    }

    @Override
    public synchronized float readFloat()
    {
        this.byteBuffer.rewind().limit(Float.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.getFloat(0);
    }

    @Override
    public synchronized double readDouble()
    {
        this.byteBuffer.rewind().limit(Double.BYTES);
        fillBuffer(this.byteBuffer);
        return this.byteBuffer.getDouble(0);
    }

    @Override
    public synchronized void skipBytes(long n)
    {
        if (n <= 0)
        {
            return;
        }

        // If this is a SeekableByteChannel, there's an efficient method for skipping
        if (this.byteChannel instanceof SeekableByteChannel)
        {
            SeekableByteChannel seekableByteChannel = (SeekableByteChannel) this.byteChannel;
            try
            {
                long currentPosition = seekableByteChannel.position();
                long remaining = seekableByteChannel.size() - currentPosition;
                if (n > remaining)
                {
                    throw new UnexpectedEndException(n, remaining);
                }
                seekableByteChannel.position(currentPosition + n);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            return;
        }

        // Otherwise, fall back to the default skip method
        int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, n);
        ByteBuffer buffer = (size <= this.byteBuffer.capacity()) ? this.byteBuffer : ByteBuffer.allocate(size);
        long remaining = n;
        int read;
        while (remaining > 0L)
        {
            buffer.rewind().limit((int) Math.min(remaining, size));
            try
            {
                read = this.byteChannel.read(buffer);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            if (read < 0)
            {
                throw new UnexpectedEndException(n, n - remaining);
            }
            remaining -= read;
        }
    }

    private void fillBuffer(ByteBuffer buffer)
    {
        long start = buffer.position();
        while (buffer.hasRemaining())
        {
            int read;
            try
            {
                read = this.byteChannel.read(buffer);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            if (read < 0)
            {
                throw new UnexpectedEndException(buffer.limit() - start, buffer.position() - start);
            }
        }
    }
}
