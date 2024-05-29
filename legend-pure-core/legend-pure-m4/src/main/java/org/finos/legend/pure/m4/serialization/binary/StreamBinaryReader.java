// Copyright 2020 Goldman Sachs
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

class StreamBinaryReader extends AbstractSimpleBinaryReader
{
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;

    private final InputStream stream;

    StreamBinaryReader(InputStream stream)
    {
        this.stream = Objects.requireNonNull(stream, "stream may not be null");
    }

    @Override
    public synchronized byte readByte()
    {
        int b;
        try
        {
            b = this.stream.read();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        if (b == -1)
        {
            throw new UnexpectedEndException(1, 0);
        }
        return (byte) b;
    }

    @Override
    public synchronized byte[] readBytes(byte[] bytes, int offset, int n)
    {
        checkByteArray(bytes, offset, n);
        int totalRead = 0;
        while (totalRead < n)
        {
            int read;
            try
            {
                read = this.stream.read(bytes, offset + totalRead, n - totalRead);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            if (read < 0)
            {
                throw new UnexpectedEndException(n, totalRead);
            }
            totalRead += read;
        }
        return bytes;
    }

    @Override
    public synchronized void close()
    {
        try
        {
            if (this.stream instanceof ZipInputStream)
            {
                ((ZipInputStream) this.stream).closeEntry();
            }
            else
            {
                this.stream.close();
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized void skipBytes(long n)
    {
        if (n <= 0)
        {
            return;
        }

        if (streamSkipIsSafe())
        {
            long skipped;
            try
            {
                skipped = this.stream.skip(n);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            if (skipped < n)
            {
                throw new UnexpectedEndException(n, skipped);
            }
            return;
        }

        // Fall back to default skip method
        int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, n);
        byte[] buffer = (size <= this.bytes.length) ? this.bytes : new byte[size];
        long remaining = n;
        while (remaining > 0L)
        {
            int read;
            try
            {
                read = this.stream.read(buffer, 0, (int) Math.min(size, remaining));
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

    private boolean streamSkipIsSafe()
    {
        // We know that these types of streams have safe skip methods
        return (this.stream instanceof ByteArrayInputStream) || (this.stream instanceof InflaterInputStream);
    }
}
