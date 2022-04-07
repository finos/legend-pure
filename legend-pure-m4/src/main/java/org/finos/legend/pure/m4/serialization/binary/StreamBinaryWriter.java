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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

class StreamBinaryWriter extends AbstractSimpleBinaryWriter
{
    private final OutputStream stream;

    StreamBinaryWriter(OutputStream stream)
    {
        this.stream = stream;
    }

    @Override
    public synchronized void writeByte(byte b)
    {
        try
        {
            this.stream.write(b);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized void writeBytes(byte[] bytes, int offset, int length)
    {
        checkByteArray(bytes, offset, length);
        try
        {
            this.stream.write(bytes, offset, length);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
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
            throw new UncheckedIOException(e);
        }
    }
}
