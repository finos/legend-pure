package org.finos.legend.pure.m4.serialization.binary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

class WritableByteChannelWriter extends AbstractBinaryWriter
{
    private final ByteBuffer eightByteBuffer = ByteBuffer.allocate(8);
    private final WritableByteChannel byteChannel;

    WritableByteChannelWriter(WritableByteChannel byteChannel)
    {
        this.byteChannel = byteChannel;
    }

    @Override
    public synchronized void writeByte(byte b)
    {
        this.eightByteBuffer.rewind().limit(Byte.BYTES);
        this.eightByteBuffer.put(b).rewind();
        writeByteBuffer(this.eightByteBuffer);
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
        this.eightByteBuffer.rewind().limit(Short.BYTES);
        this.eightByteBuffer.putShort(s).rewind();
        writeByteBuffer(this.eightByteBuffer);
    }

    @Override
    public synchronized void writeInt(int i)
    {
        this.eightByteBuffer.rewind().limit(Integer.BYTES);
        this.eightByteBuffer.putInt(i).rewind();
        writeByteBuffer(this.eightByteBuffer);
    }

    @Override
    public synchronized void writeLong(long l)
    {
        this.eightByteBuffer.rewind().limit(Long.BYTES);
        this.eightByteBuffer.putLong(l).rewind();
        writeByteBuffer(this.eightByteBuffer);
    }

    @Override
    public synchronized void writeFloat(float f)
    {
        this.eightByteBuffer.rewind().limit(Float.BYTES);
        this.eightByteBuffer.putFloat(f).rewind();
        writeByteBuffer(this.eightByteBuffer);
    }

    @Override
    public synchronized void writeDouble(double d)
    {
        this.eightByteBuffer.rewind().limit(Double.BYTES);
        this.eightByteBuffer.putDouble(d).rewind();
        writeByteBuffer(this.eightByteBuffer);
    }

    @Override
    public synchronized void close()
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
