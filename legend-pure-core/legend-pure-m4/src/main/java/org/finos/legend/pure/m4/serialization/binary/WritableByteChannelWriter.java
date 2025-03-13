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
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

class WritableByteChannelWriter extends AbstractBinaryWriter
{
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Math.max(Long.BYTES, Double.BYTES));
    private final WritableByteChannel byteChannel;
    private final boolean closeChannelOnClose;

    WritableByteChannelWriter(WritableByteChannel byteChannel, boolean closeChannelOnClose)
    {
        this.byteChannel = Objects.requireNonNull(byteChannel, "byteChannel may not be null");
        this.closeChannelOnClose = closeChannelOnClose;
    }

    @Override
    public synchronized void writeByte(byte b)
    {
        this.byteBuffer.rewind().limit(Byte.BYTES);
        this.byteBuffer.put(b).rewind();
        writeByteBuffer(this.byteBuffer);
    }

    @Override
    public synchronized void writeBytes(byte[] bytes, int offset, int length)
    {
        checkByteArray(bytes, offset, length);
        writeByteBuffer(ByteBuffer.wrap(bytes, offset, length));
    }

    @Override
    public synchronized void writeShort(short s)
    {
        this.byteBuffer.rewind().limit(Short.BYTES);
        this.byteBuffer.putShort(s).rewind();
        writeByteBuffer(this.byteBuffer);
    }

    @Override
    public synchronized void writeInt(int i)
    {
        this.byteBuffer.rewind().limit(Integer.BYTES);
        this.byteBuffer.putInt(i).rewind();
        writeByteBuffer(this.byteBuffer);
    }

    @Override
    public synchronized void writeLong(long l)
    {
        this.byteBuffer.rewind().limit(Long.BYTES);
        this.byteBuffer.putLong(l).rewind();
        writeByteBuffer(this.byteBuffer);
    }

    @Override
    public synchronized void writeFloat(float f)
    {
        this.byteBuffer.rewind().limit(Float.BYTES);
        this.byteBuffer.putFloat(f).rewind();
        writeByteBuffer(this.byteBuffer);
    }

    @Override
    public synchronized void writeDouble(double d)
    {
        this.byteBuffer.rewind().limit(Double.BYTES);
        this.byteBuffer.putDouble(d).rewind();
        writeByteBuffer(this.byteBuffer);
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

    private void writeByteBuffer(ByteBuffer buffer)
    {
        try
        {
            this.byteChannel.write(buffer);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
