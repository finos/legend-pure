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

import org.eclipse.collections.api.list.primitive.ByteList;

class ByteListBinaryReader extends AbstractSimpleBinaryReader
{
    private final ByteList bytes;
    private int current;

    ByteListBinaryReader(ByteList bytes)
    {
        if (bytes == null)
        {
            throw new IllegalArgumentException("ByteList may not be null");
        }
        this.bytes = bytes;
        this.current = 0;
    }

    @Override
    public synchronized byte readByte()
    {
        try
        {
            return this.bytes.get(this.current++);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new UnexpectedEndException(1, 0);
        }
    }

    @Override
    public synchronized byte[] readBytes(byte[] bytes, int offset, int n)
    {
        checkByteArray(bytes, offset, n);

        int remaining = this.bytes.size() - this.current;
        if (remaining < n)
        {
            throw new UnexpectedEndException(n, remaining);
        }

        for (int i = 0; i < n; i++)
        {
            bytes[offset + i] = this.bytes.get(this.current++);
        }
        return bytes;
    }

    @Override
    public void close()
    {
        // Do nothing
    }

    @Override
    public synchronized void skipBytes(long n)
    {
        if (n <= 0)
        {
            return;
        }

        long newPosition = this.current + n;
        if (newPosition >= this.bytes.size())
        {
            throw new UnexpectedEndException(n, (long)this.bytes.size() - this.current);
        }
        this.current = (int)newPosition;
    }
}
