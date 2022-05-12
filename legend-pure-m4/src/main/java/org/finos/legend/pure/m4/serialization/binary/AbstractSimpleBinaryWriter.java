package org.finos.legend.pure.m4.serialization.binary;

import java.nio.ByteBuffer;

public abstract class AbstractSimpleBinaryWriter extends AbstractBinaryWriter
{
    private final byte[] eightBytes = new byte[8];
    private final ByteBuffer eightByteBuffer = ByteBuffer.wrap(this.eightBytes);

    @Override
    public synchronized void writeShort(short s)
    {
        this.eightByteBuffer.putShort(0, s);
        writeBytes(this.eightBytes, 0, Short.BYTES);
    }

    @Override
    public synchronized void writeInt(int i)
    {
        this.eightByteBuffer.putInt(0, i);
        writeBytes(this.eightBytes, 0, Integer.BYTES);
    }

    @Override
    public synchronized void writeLong(long l)
    {
        this.eightByteBuffer.putLong(0, l);
        writeBytes(this.eightBytes, 0, Long.BYTES);
    }

    @Override
    public synchronized void writeFloat(float f)
    {
        this.eightByteBuffer.putFloat(0, f);
        writeBytes(this.eightBytes, 0, Float.BYTES);
    }

    @Override
    public synchronized void writeDouble(double d)
    {
        this.eightByteBuffer.putDouble(0, d);
        writeBytes(this.eightBytes, 0, Double.BYTES);
    }
}
