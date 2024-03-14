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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.serialization.runtime.binary.*;
import org.finos.legend.pure.m4.ModelRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ForkJoinPool;

public class MemoryGraphLoaderPureGraphCache extends AbstractPureGraphCache
{
    private final ForkJoinPool forkJoinPool;
    private ListIterable<PureRepositoryJar> jars = null;
    private long size = -1L;

    public MemoryGraphLoaderPureGraphCache(ForkJoinPool forkJoinPool)
    {
        this.forkJoinPool = forkJoinPool;
    }

    public MemoryGraphLoaderPureGraphCache()
    {
        this(null);
    }

    @Override
    protected void clearCaches()
    {
        this.jars = null;
        this.size = -1L;
    }

    @Override
    protected void writeCaches()
    {
        long newSize = 0;
        MutableList<PureRepositoryJar> newJars = Lists.mutable.empty();
        RepositoryCodeStorage codeStorage = this.pureRuntime.getCodeStorage();
        RichIterable<String> repoNames = codeStorage.getAllRepositories().collect(CodeRepository::getName);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        for (String repoName : repoNames)
        {
            outStream.reset();
            try
            {
                BinaryModelRepositorySerializer.serialize(outStream, repoName, this.pureRuntime);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error writing cache for " + repoName, e);
            }
            newJars.add(PureRepositoryJars.get(outStream));
            newSize += outStream.size();
        }

        this.jars = newJars;
        this.size = newSize;
    }

    @Override
    protected boolean cacheExists()
    {
        return this.jars != null;
    }

    @Override
    protected long getCacheSize()
    {
        return this.size;
    }

    @Override
    protected boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(this.jars);
        GraphLoader loader = new GraphLoader(modelRepository, context, library, this.pureRuntime.getIncrementalCompiler().getDslLibrary(), sources, null, jarLibrary, this.forkJoinPool);
        loader.loadAll(message);
        updateCacheState();
        return true;
    }
}
