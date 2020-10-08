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
import java.util.zip.InflaterInputStream;

class StreamBinaryReader extends AbstractSimpleBinaryReader
{
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;

    private final InputStream stream;

    StreamBinaryReader(InputStream stream)
    {
        this.stream = stream;
    }

    @Override
    public synchronized void close()
    {
        try
        {
            this.stream.close();
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

        try
        {
            // We know that ByteArrayInputStream and InflaterInputStream have safe skip methods
            if ((this.stream instanceof ByteArrayInputStream) || (this.stream instanceof InflaterInputStream))
            {
                long skipped = this.stream.skip(n);
                if (skipped < n)
                {
                    throw new UnexpectedEndException(n, skipped);
                }
                return;
            }

            // Fall back to default skip method
            int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, n);
            byte[] buffer = (size <= 8) ? this.eightBytes : new byte[size];
            long remaining = n;
            int read;
            while (remaining > 0L)
            {
                read = this.stream.read(buffer, 0, (int)Math.min(size, remaining));
                if (read < 0)
                {
                    throw new UnexpectedEndException(n, n - remaining);
                }
                remaining -= read;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected byte readOneByte()
    {
        try
        {
            int b = this.stream.read();
            if (b == -1)
            {
                throw new UnexpectedEndException(1, 0);
            }
            return (byte)b;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void readNBytes(int n, byte[] bytes, int offset)
    {
        if (n <= 0)
        {
            return;
        }

        try
        {
            int totalRead = 0;
            int read;
            while (totalRead < n)
            {
                read = this.stream.read(bytes, offset + totalRead, n - totalRead);
                if (read < 0)
                {
                    throw new UnexpectedEndException(n, totalRead);
                }
                totalRead += read;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
