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

import org.eclipse.collections.api.collection.primitive.MutableByteCollection;

class ByteCollectionBinaryWriter extends AbstractBinaryWriter
{
    private final MutableByteCollection byteCollection;

    ByteCollectionBinaryWriter(MutableByteCollection byteCollection)
    {
        this.byteCollection = byteCollection;
    }

    @Override
    public void close()
    {
        // Do nothing
    }

    @Override
    protected void write(byte b)
    {
        this.byteCollection.add(b);
    }

    @Override
    protected void write(byte[] bytes, int offset, int length)
    {
        if ((offset == 0) && (length == bytes.length))
        {
            this.byteCollection.addAll(bytes);
        }
        else
        {
            byte[] copy = new byte[length];
            System.arraycopy(bytes, offset, copy, 0, length);
            this.byteCollection.addAll(copy);
        }
    }
}
