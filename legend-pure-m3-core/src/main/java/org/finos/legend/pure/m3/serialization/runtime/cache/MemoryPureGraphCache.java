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

package org.finos.legend.pure.m3.serialization.runtime.cache;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.BinarySourceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MemoryPureGraphCache extends AbstractPureGraphCache
{
    private byte[] graph = null;
    private byte[] sources = null;

    public MemoryPureGraphCache()
    {
        initializeCacheState();
    }

    @Override
    protected boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message) throws IOException
    {
        if (message != null)
        {
            message.setMessage("Loading Graph Cache ...");
        }
        IntObjectMap<CoreInstance> instancesById;
        try (Reader reader = newReader(this.graph))
        {
            instancesById = BinaryRepositorySerializer.build(reader, modelRepository, Message.newMessageCallback(message));
        }
        if (message != null)
        {
            message.setMessage("Loading Sources Cache ...");
        }
        try (Reader reader = newReader(this.sources))
        {
            BinarySourceSerializer.build(reader, sources, instancesById, library, context);
        }
        return true;
    }

    @Override
    protected void clearCaches()
    {
        this.graph = null;
        this.sources = null;
    }

    @Override
    protected void writeCaches()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer writer = BinaryWriters.newBinaryWriter(newOutputStream(bytes)))
        {
            this.pureRuntime.getModelRepository().serialize(writer);
        }
        this.graph = bytes.toByteArray();
        bytes.reset();
        try (Writer writer = BinaryWriters.newBinaryWriter(newOutputStream(bytes)))
        {
            this.pureRuntime.getSourceRegistry().serialize(writer);
        }
        this.sources = bytes.toByteArray();
    }

    @Override
    protected boolean cacheExists()
    {
        return (this.graph != null) && (this.sources != null);
    }

    @Override
    protected long getCacheSize()
    {
        return (this.graph == null) ? -1L : this.graph.length;
    }

    protected OutputStream newOutputStream(ByteArrayOutputStream stream)
    {
        return stream;
    }

    protected Reader newReader(byte[] bytes)
    {
        return BinaryReaders.newBinaryReader(bytes);
    }
}
