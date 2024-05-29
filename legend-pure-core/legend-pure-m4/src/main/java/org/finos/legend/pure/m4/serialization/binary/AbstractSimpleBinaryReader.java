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

import java.nio.ByteBuffer;

public abstract class AbstractSimpleBinaryReader extends AbstractBinaryReader
{
    protected final byte[] bytes = new byte[Math.max(Long.BYTES, Double.BYTES)];
    protected final ByteBuffer byteBuffer = ByteBuffer.wrap(this.bytes);

    @Override
    public synchronized short readShort()
    {
        readBytes(this.bytes, 0, Short.BYTES);
        return this.byteBuffer.getShort(0);
    }

    @Override
    public synchronized int readInt()
    {
        readBytes(this.bytes, 0, Integer.BYTES);
        return this.byteBuffer.getInt(0);
    }

    @Override
    public synchronized long readLong()
    {
        readBytes(this.bytes, 0, Long.BYTES);
        return this.byteBuffer.getLong(0);
    }

    @Override
    public synchronized float readFloat()
    {
        readBytes(this.bytes, 0, Float.BYTES);
        return this.byteBuffer.getFloat(0);
    }

    @Override
    public synchronized double readDouble()
    {
        readBytes(this.bytes, 0, Double.BYTES);
        return this.byteBuffer.getDouble(0);
    }
}
