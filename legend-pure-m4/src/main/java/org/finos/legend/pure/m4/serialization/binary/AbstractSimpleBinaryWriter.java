package org.finos.legend.pure.m4.serialization.binary;

import java.nio.ByteBuffer;

public abstract class AbstractSimpleBinaryWriter extends AbstractBinaryWriter
{
    private final byte[] eightBytes = new byte[8];
    private final ByteBuffer eightByteBuffer = ByteBuffer.wrap(this.eightBytes);

    @Override
    public synchronized void writeByte(byte b)
    {
        write(b);
    }

    @Override
    public synchronized void writeBytes(byte[] bytes)
    {
        write(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void writeShort(short s)
    {
        this.eightByteBuffer.putShort(0, s);
        write(this.eightBytes, 0, Short.BYTES);
    }

    @Override
    public synchronized void writeInt(int i)
    {
        this.eightByteBuffer.putInt(0, i);
        write(this.eightBytes, 0, Integer.BYTES);
    }

    @Override
    public synchronized void writeLong(long l)
    {
        this.eightByteBuffer.putLong(0, l);
        write(this.eightBytes, 0, Long.BYTES);
    }

    @Override
    public synchronized void writeFloat(float f)
    {
        this.eightByteBuffer.putFloat(0, f);
        write(this.eightBytes, 0, Float.BYTES);
    }

    @Override
    public synchronized void writeDouble(double d)
    {
        this.eightByteBuffer.putDouble(0, d);
        write(this.eightBytes, 0, Double.BYTES);
    }

    protected abstract void write(byte b);

    protected abstract void write(byte[] bytes, int offset, int length);
}
