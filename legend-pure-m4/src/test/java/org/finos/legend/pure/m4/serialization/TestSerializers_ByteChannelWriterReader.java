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
