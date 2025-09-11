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

import org.finos.legend.pure.m4.serialization.Writer;

import java.util.Objects;

/**
 * An abstract {@link Writer} which delegates to another Writer. This is intended to be subclassed by Writer
 * implementations that mostly want to delegate to another Writer but want to override a few methods. Note that any
 * synchronization is left to the delegate Writer.
 */
public abstract class DelegatingWriter implements Writer
{
    protected final Writer delegate;

    protected DelegatingWriter(Writer delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public void writeByte(byte b)
    {
        this.delegate.writeByte(b);
    }

    @Override
    public void writeBytes(byte[] bytes)
    {
        this.delegate.writeBytes(bytes);
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int len)
    {
        this.delegate.writeBytes(bytes, offset, len);
    }

    @Override
    public void writeByteArray(byte[] bytes)
    {
        this.delegate.writeByteArray(bytes);
    }

    @Override
    public void writeBoolean(boolean b)
    {
        this.delegate.writeBoolean(b);
    }

    @Override
    public void writeShort(short s)
    {
        this.delegate.writeShort(s);
    }

    @Override
    public void writeShortArray(short[] shorts)
    {
        this.delegate.writeShortArray(shorts);
    }

    @Override
    public void writeInt(int i)
    {
        this.delegate.writeInt(i);
    }

    @Override
    public void writeIntArray(int[] ints)
    {
        this.delegate.writeIntArray(ints);
    }

    @Override
    public void writeLong(long l)
    {
        this.delegate.writeLong(l);
    }

    @Override
    public void writeLongArray(long[] longs)
    {
        this.delegate.writeLongArray(longs);
    }

    @Override
    public void writeFloat(float f)
    {
        this.delegate.writeFloat(f);
    }

    @Override
    public void writeFloatArray(float[] floats)
    {
        this.delegate.writeFloatArray(floats);
    }

    @Override
    public void writeDouble(double d)
    {
        this.delegate.writeDouble(d);
    }

    @Override
    public void writeDoubleArray(double[] doubles)
    {
        this.delegate.writeDoubleArray(doubles);
    }

    @Override
    public void writeString(String string)
    {
        this.delegate.writeString(string);
    }

    @Override
    public void writeStringArray(String[] strings)
    {
        this.delegate.writeStringArray(strings);
    }

    @Override
    public void close()
    {
        this.delegate.close();
    }
}
