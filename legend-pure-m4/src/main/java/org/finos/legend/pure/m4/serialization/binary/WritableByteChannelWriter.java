package org.finos.legend.pure.m4.serialization.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class WritableByteChannelWriter extends AbstractBinaryWriter
{
    private final WritableByteChannel byteChannel;

    WritableByteChannelWriter(WritableByteChannel byteChannel)
    {
        this.byteChannel = byteChannel;
    }

    @Override
    public void close()
    {
        try
        {
            this.byteChannel.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void write(byte b)
    {
        write(new byte[]{b}, 0, 1);
    }

    @Override
    protected void write(byte[] bytes, int offset, int length)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
        try
        {
            this.byteChannel.write(byteBuffer);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
