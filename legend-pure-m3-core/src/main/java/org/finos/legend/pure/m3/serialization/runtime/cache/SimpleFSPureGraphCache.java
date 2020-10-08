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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleFSPureGraphCache extends AbstractFSDirectoryPureGraphCache
{
    private static final String GRAPH_CACHE_FILENAME = "graph.bin";
    private static final String SOURCES_CACHE_FILENAME = "sources.bin";

    public SimpleFSPureGraphCache(Path cacheDirectory, Message message)
    {
        super(cacheDirectory);
        initializeCacheState(message);
    }

    public SimpleFSPureGraphCache(Path cacheDirectory)
    {
        this(cacheDirectory, null);
    }

    @Override
    protected boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message) throws IOException
    {
        if (message != null)
        {
            message.setMessage("Loading Graph Cache File...");
        }
        IntObjectMap<CoreInstance> instancesById;
        try (InputStream inStream = newInputStream(getGraphCachePath()))
        {
            instancesById = BinaryRepositorySerializer.build(inStream, modelRepository, Message.newMessageCallback(message));
        }

        if (message != null)
        {
            message.setMessage("Loading Sources Cache File...");
        }
        try (InputStream inStream = newInputStream(getSourcesCachePath()))
        {
            BinarySourceSerializer.build(inStream, sources, instancesById, library, context);
        }

        if (message != null)
        {
            message.setMessage("Finished Loading Cache Files...");
        }

        return true;
    }

    @Override
    protected void writeCaches()
    {
        try
        {
            try (OutputStream stream = newOutputStream(getGraphCachePath()))
            {
                this.pureRuntime.getModelRepository().serialize(stream);
            }
            try (OutputStream stream = newOutputStream(getSourcesCachePath()))
            {
                this.pureRuntime.getSourceRegistry().serialize(stream);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error writing caches", e);
        }
    }

    @Override
    protected boolean cacheExists()
    {
        return Files.exists(getGraphCachePath());
    }

    @Override
    protected long getCacheSize()
    {
        Path path = getGraphCachePath();
        if (Files.notExists(path))
        {
            return -1L;
        }

        try
        {
            return Files.size(path);
        }
        catch (Exception e)
        {
            return -1L;
        }
    }

    protected Path getGraphCachePath()
    {
        return getCacheLocation().resolve(GRAPH_CACHE_FILENAME);
    }

    protected Path getSourcesCachePath()
    {
        return getCacheLocation().resolve(SOURCES_CACHE_FILENAME);
    }
}
