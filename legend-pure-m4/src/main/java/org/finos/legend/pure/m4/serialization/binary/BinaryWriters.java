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
import org.finos.legend.pure.m4.serialization.Writer;

import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

public class BinaryWriters
{
    private BinaryWriters()
    {
    }

    public static Writer newBinaryWriter(WritableByteChannel byteChannel)
    {
        return new WritableByteChannelWriter(byteChannel);
    }

    public static Writer newBinaryWriter(OutputStream stream)
    {
        return new StreamBinaryWriter(stream);
    }

    public static Writer newBinaryWriter(MutableByteCollection byteCollection)
    {
        return new ByteCollectionBinaryWriter(byteCollection);
    }
}
