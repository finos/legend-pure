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
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FSZipPureGraphCache extends AbstractFSZipPureGraphCache
{
    private static final String DEFAULT_CACHE_FILENAME = "cache.zip";
    private static final String GRAPH_CACHE_FILENAME = "graph.bin";
    private static final String SOURCES_CACHE_FILENAME = "sources.bin";

    private static final int DEFAULT_COMPRESSION_LEVEL = 3;

    public FSZipPureGraphCache(Path cacheDirectory, String zipName, int compressionLevel, Message message)
    {
        super(cacheDirectory.resolve((zipName == null) ? DEFAULT_CACHE_FILENAME : zipName), compressionLevel);
        initializeCacheState(message);
    }

    public FSZipPureGraphCache(Path cacheDirectory, int compressionLevel, Message message)
    {
        this(cacheDirectory, null, compressionLevel, message);
    }

    public FSZipPureGraphCache(Path cacheDirectory, String zipName, int compressionLevel)
    {
        this(cacheDirectory, zipName, compressionLevel, null);
    }

    public FSZipPureGraphCache(Path cacheDirectory, int compressionLevel)
    {
        this(cacheDirectory, null, compressionLevel, null);
    }

    public FSZipPureGraphCache(Path cacheDirectory, String zipName, Message message)
    {
        this(cacheDirectory, zipName, DEFAULT_COMPRESSION_LEVEL, message);
    }

    public FSZipPureGraphCache(Path cacheDirectory, Message message)
    {
        this(cacheDirectory, null, DEFAULT_COMPRESSION_LEVEL, message);
    }

    public FSZipPureGraphCache(Path cacheDirectory, String zipName)
    {
        this(cacheDirectory, zipName, DEFAULT_COMPRESSION_LEVEL, null);
    }

    public FSZipPureGraphCache(Path cacheDirectory)
    {
        this(cacheDirectory, null, DEFAULT_COMPRESSION_LEVEL, null);
    }

    @Override
    protected boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message) throws IOException
    {
        try (ZipInputStream stream = newZipInputStream())
        {
            // build graph
            ZipEntry graphEntry = stream.getNextEntry();
            if (!GRAPH_CACHE_FILENAME.equals(graphEntry.getName()))
            {
                throw new RuntimeException("Invalid cache archive: expected " + GRAPH_CACHE_FILENAME + ", got " + graphEntry.getName());
            }
            if (message != null)
            {
                message.setMessage("Loading Graph Cache ...");
            }
            IntObjectMap<CoreInstance> instancesById = BinaryRepositorySerializer.build(stream, modelRepository, Message.newMessageCallback(message));

            // build sources
            ZipEntry sourcesEntry = stream.getNextEntry();
            if (!SOURCES_CACHE_FILENAME.equals(sourcesEntry.getName()))
            {
                throw new RuntimeException("Invalid cache archive: expected " + SOURCES_CACHE_FILENAME + ", got " + graphEntry.getName());
            }
            if (message != null)
            {
                message.setMessage("Loading Sources Cache ...");
            }
            BinarySourceSerializer.build(stream, sources, instancesById, library, context);

            // TODO should we check that there are no more entries?

            return true;
        }
    }

    @Override
    protected void writeCaches()
    {
        try (ZipOutputStream stream = newZipOutputStream())
        {
            stream.putNextEntry(new ZipEntry(GRAPH_CACHE_FILENAME));
            this.pureRuntime.getModelRepository().serialize(stream);
            stream.closeEntry();

            stream.putNextEntry(new ZipEntry(SOURCES_CACHE_FILENAME));
            this.pureRuntime.getSourceRegistry().serialize(stream);
            stream.closeEntry();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error writing caches", e);
        }
    }
}
