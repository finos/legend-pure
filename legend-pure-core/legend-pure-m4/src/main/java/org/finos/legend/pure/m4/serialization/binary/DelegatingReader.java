// Copyright 2024 Goldman Sachs
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

import org.finos.legend.pure.m4.serialization.Reader;

import java.util.Objects;

/**
 * An abstract {@link Reader} which delegates to another Reader. This is intended to be subclassed by Reader
 * implementations that mostly want to delegate to another Reader but want to override a few methods. Note that any
 * synchronization is left to the delegate Reader.
 */
public abstract class DelegatingReader implements Reader
{
    protected final Reader delegate;

    protected DelegatingReader(Reader delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public byte readByte()
    {
        return this.delegate.readByte();
    }

    @Override
    public byte[] readBytes(int n)
    {
        return this.delegate.readBytes(n);
    }

    @Override
    public byte[] readBytes(byte[] bytes)
    {
        return this.delegate.readBytes(bytes);
    }

    @Override
    public byte[] readBytes(byte[] bytes, int offset, int n)
    {
        return this.delegate.readBytes(bytes, offset, n);
    }

    @Override
    public void skipBytes(long n)
    {
        this.delegate.skipBytes(n);
    }

    @Override
    public byte[] readByteArray()
    {
        return this.delegate.readByteArray();
    }

    @Override
    public void skipByteArray()
    {
        this.delegate.skipByteArray();
    }

    @Override
    public boolean readBoolean()
    {
        return this.delegate.readBoolean();
    }

    @Override
    public void skipBoolean()
    {
        this.delegate.skipBoolean();
    }

    @Override
    public short readShort()
    {
        return this.delegate.readShort();
    }

    @Override
    public void skipShort()
    {
        this.delegate.skipShort();
    }

    @Override
    public short[] readShortArray()
    {
        return this.delegate.readShortArray();
    }

    @Override
    public void skipShortArray()
    {
        this.delegate.skipShortArray();
    }

    @Override
    public int readInt()
    {
        return this.delegate.readInt();
    }

    @Override
    public void skipInt()
    {
        this.delegate.skipInt();
    }

    @Override
    public int[] readIntArray()
    {
        return this.delegate.readIntArray();
    }

    @Override
    public void skipIntArray()
    {
        this.delegate.skipIntArray();
    }

    @Override
    public long readLong()
    {
        return this.delegate.readLong();
    }

    @Override
    public void skipLong()
    {
        this.delegate.skipLong();
    }

    @Override
    public long[] readLongArray()
    {
        return this.delegate.readLongArray();
    }

    @Override
    public void skipLongArray()
    {
        this.delegate.skipLongArray();
    }

    @Override
    public float readFloat()
    {
        return this.delegate.readFloat();
    }

    @Override
    public void skipFloat()
    {
        this.delegate.skipFloat();
    }

    @Override
    public float[] readFloatArray()
    {
        return this.delegate.readFloatArray();
    }

    @Override
    public void skipFloatArray()
    {
        this.delegate.skipFloatArray();
    }

    @Override
    public double readDouble()
    {
        return this.delegate.readDouble();
    }

    @Override
    public void skipDouble()
    {
        this.delegate.skipDouble();
    }

    @Override
    public double[] readDoubleArray()
    {
        return this.delegate.readDoubleArray();
    }

    @Override
    public void skipDoubleArray()
    {
        this.delegate.skipDoubleArray();
    }

    @Override
    public String readString()
    {
        return this.delegate.readString();
    }

    @Override
    public void skipString()
    {
        this.delegate.skipString();
    }

    @Override
    public String[] readStringArray()
    {
        return this.delegate.readStringArray();
    }

    @Override
    public void skipStringArray()
    {
        this.delegate.skipStringArray();
    }

    @Override
    public void close()
    {
        this.delegate.close();
    }
}
