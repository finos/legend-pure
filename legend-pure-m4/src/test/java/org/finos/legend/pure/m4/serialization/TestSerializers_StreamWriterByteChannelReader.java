package org.finos.legend.pure.m4.serialization;

import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestSerializers_StreamWriterByteChannelReader extends TestSerializers
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private OutputStream outputStream;
    private ReadableByteChannel readableByteChannel;

    @After
    public void cleanUpOutputStream() throws IOException
    {
        closeOutputStream();
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
                return BinaryWriters.newBinaryWriter(openOutputStream(tmpFile));
            }

            @Override
            public Reader getReader() throws IOException
            {
                closeOutputStream();
                return BinaryReaders.newBinaryReader(openReadableByteChannel(tmpFile));
            }
        };
    }

    private OutputStream openOutputStream(Path file) throws IOException
    {
        this.outputStream = new BufferedOutputStream(Files.newOutputStream(file));
        return this.outputStream;
    }

    private void closeOutputStream() throws IOException
    {
        if (this.outputStream != null)
        {
            this.outputStream.close();
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
