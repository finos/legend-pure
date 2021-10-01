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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class BinaryGraphSerializer
{
    public static void serialize(Serialized serialized, OutputStream stream)
    {
        Writer writer = BinaryWriters.newBinaryWriter(stream);
        SimpleStringCache stringCache = SimpleStringCache.fromSerialized(serialized);
        BinaryObjSerializer serializer = new BinaryObjSerializerWithStringCache(stringCache);

        // Write string cache
        stringCache.write(writer);

        // Write objects
        ListIterable<Obj> objs = serialized.getObjects();
        writer.writeInt(objs.size());
        for (Obj obj : objs)
        {
            serializer.serializeObj(writer, obj);
        }

        // Write links
        ListIterable<Pair<Obj, Obj>> packageLinks = serialized.getPackageLinks();
        writer.writeInt(packageLinks.size());
        for (Pair<Obj, Obj> link : packageLinks)
        {
            serializer.serializeObj(writer, link.getOne());
            serializer.serializeObj(writer, link.getTwo());
        }
    }

    public static Serialized buildSerialized(InputStream inputStream)
    {
        Reader reader = BinaryReaders.newBinaryReader(inputStream);
        StringIndex stringIndex = EagerStringIndex.fromReader(reader);
        BinaryObjDeserializer deserializer = new BinaryObjDeserializerWithStringIndex(stringIndex);

        int objCount = reader.readInt();
        MutableList<Obj> objs = FastList.newList(objCount);
        for (int i = 0; i < objCount; i++)
        {
            Obj obj = deserializer.deserialize(reader);
            objs.add(obj);
        }

        int linkCount = reader.readInt();
        MutableList<Pair<Obj, Obj>> packageLinks = FastList.newList(linkCount);
        for (int i = 0; i < linkCount; i++)
        {
            Obj first = deserializer.deserialize(reader);
            Obj second = deserializer.deserialize(reader);
            packageLinks.add(Tuples.pair(first, second));
        }

        return new Serialized(objs, packageLinks);
    }
}
