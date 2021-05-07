package org.finos.legend.pure.m4.serialization.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

public class ReadableByteChannelReader extends AbstractSimpleBinaryReader
{
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;

    private final ReadableByteChannel byteChannel;

    ReadableByteChannelReader(ReadableByteChannel byteChannel)
    {
        this.byteChannel = byteChannel;
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
            throw new RuntimeException(e);
        }
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
                return;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        // Otherwise, fall back to the default skip method
        int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, n);
        ByteBuffer buffer = ByteBuffer.wrap((size <= 8) ? this.eightBytes : new byte[size], 0, size);
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
                throw new RuntimeException(e);
            }
            if (read < 0)
            {
                throw new UnexpectedEndException(n, n - remaining);
            }
            remaining -= read;
        }
    }

    @Override
    protected byte readOneByte()
    {
        byte[] bytes = new byte[1];
        readNBytes(1, bytes, 0);
        return bytes[0];
    }

    @Override
    protected void readNBytes(int n, byte[] bytes, int offset)
    {
        if (n <= 0)
        {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, n);
        int totalRead = 0;
        int read;
        while (totalRead < n)
        {
            try
            {
                read = this.byteChannel.read(buffer);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            if (read < 0)
            {
                throw new UnexpectedEndException(n, totalRead);
            }
            totalRead += read;
        }
    }
}
