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

class ByteCollectionBinaryWriter extends AbstractSimpleBinaryWriter
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
    public synchronized void writeByte(byte b)
    {
        this.byteCollection.add(b);
    }

    @Override
    public synchronized void writeBytes(byte[] bytes, int offset, int length)
    {
        checkByteArray(bytes, offset, length);
        if ((offset == 0) && (length == bytes.length))
        {
            this.byteCollection.addAll(bytes);
        }
        else
        {
            for (int i = 0; i < length; i++)
            {
                this.byteCollection.add(bytes[offset + i]);
            }
        }
    }
}
