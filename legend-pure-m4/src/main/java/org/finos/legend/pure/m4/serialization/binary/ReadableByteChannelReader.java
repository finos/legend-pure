package org.finos.legend.pure.m4.serialization.binary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

class ReadableByteChannelReader extends AbstractBinaryReader
{
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;

    private final ByteBuffer eightByteBuffer = ByteBuffer.allocate(8);
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
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized byte readByte()
    {
        this.eightByteBuffer.rewind().limit(Byte.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.get(0);
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
        this.eightByteBuffer.rewind().limit(Short.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.getShort(0);
    }

    @Override
    public synchronized int readInt()
    {
        this.eightByteBuffer.rewind().limit(Integer.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.getInt(0);
    }

    @Override
    public synchronized long readLong()
    {
        this.eightByteBuffer.rewind().limit(Long.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.getLong(0);
    }

    @Override
    public synchronized float readFloat()
    {
        this.eightByteBuffer.rewind().limit(Float.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.getFloat(0);
    }

    @Override
    public synchronized double readDouble()
    {
        this.eightByteBuffer.rewind().limit(Double.BYTES);
        fillBuffer(this.eightByteBuffer);
        return this.eightByteBuffer.getDouble(0);
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
        ByteBuffer buffer = (size <= this.eightByteBuffer.capacity()) ? this.eightByteBuffer : ByteBuffer.allocate(size);
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
