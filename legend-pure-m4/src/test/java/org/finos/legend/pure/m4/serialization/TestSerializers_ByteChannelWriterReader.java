package org.finos.legend.pure.m4.serialization;

import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestSerializers_ByteChannelWriterReader extends TestSerializers
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private WritableByteChannel writableByteChannel;
    private ReadableByteChannel readableByteChannel;

    @After
    public void cleanUpWritableByteChannel() throws IOException
    {
        closeWritableByteChannel();
    }

    @After
    public void cleanUpReadableByteChannel() throws IOException
    {
        closeReadableByteChannel();
    }

    @Override
    protected WriterReader newWriterReader() throws IOException
    {
        Path tmpFile = this.tempFolder.newFile().toPath();
        return new WriterReader()
        {
            @Override
            public Writer getWriter() throws IOException
            {
                return BinaryWriters.newBinaryWriter(openWritableByteChannel(tmpFile));
            }

            @Override
            public Reader getReader() throws IOException
            {
                closeWritableByteChannel();
                return BinaryReaders.newBinaryReader(openReadableByteChannel(tmpFile));
            }
        };
    }

    private WritableByteChannel openWritableByteChannel(Path file) throws IOException
    {
        this.writableByteChannel = Files.newByteChannel(file, StandardOpenOption.WRITE);
        return this.writableByteChannel;
    }

    private void closeWritableByteChannel() throws IOException
    {
        if (this.writableByteChannel != null)
        {
            this.writableByteChannel.close();
        }
    }

    private ReadableByteChannel openReadableByteChannel(Path file) throws IOException
    {
        this.readableByteChannel = Files.newByteChannel(file, StandardOpenOption.READ);
        return this.readableByteChannel;
    }

    private void closeReadableByteChannel() throws IOException
    {
        if (this.readableByteChannel != null)
        {
            this.readableByteChannel.close();
        }
    }
}
