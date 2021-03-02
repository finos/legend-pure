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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.ModelRepository;

import java.util.concurrent.ForkJoinPool;

public class ClassLoaderPureGraphCache implements PureGraphCache
{
    private final ClassLoader classLoader;
    private final ForkJoinPool forkJoinPool;
    private final CacheState state = new CacheState(false, -1L, true);
    private PureRuntime runtime;

    public ClassLoaderPureGraphCache(ClassLoader classLoader, ForkJoinPool forkJoinPool)
    {
        this.classLoader = (classLoader == null) ? ClassLoaderPureGraphCache.class.getClassLoader() : classLoader;
        this.forkJoinPool = forkJoinPool;
    }

    public ClassLoaderPureGraphCache(ClassLoader classLoader)
    {
        this(classLoader, null);
    }

    public ClassLoaderPureGraphCache(ForkJoinPool forkJoinPool)
    {
        this(null, forkJoinPool);
    }

    public ClassLoaderPureGraphCache()
    {
        this(null, null);
    }

    @Override
    public void deleteCache()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPureRuntime(PureRuntime pureRuntime)
    {
        this.runtime = pureRuntime;
    }

    @Override
    public void cacheRepoAndSources()
    {
        // Do nothing
    }

    @Override
    public boolean buildRepoAndSources(ModelRepository modelRepository, SourceRegistry sourceRegistry, ParserLibrary parserLibrary, Context context, ProcessorSupport processorSupport, Message message)
    {
        if (this.runtime == null)
        {
            return false;
        }

        try
        {
            CodeStorage codeStorage = this.runtime.getCodeStorage();
            MutableList<String> repoNames = codeStorage.getAllRepoNames().toSortedList(new RepositoryComparator(codeStorage.getAllRepositories()));
            PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(repoNames, this.classLoader, message));
            GraphLoader loader = new GraphLoader(modelRepository, context, parserLibrary, this.runtime.getIncrementalCompiler().getDslLibrary(), sourceRegistry, null, jarLibrary, this.forkJoinPool);
            for (String repoName : repoNames)
            {
                loader.loadRepository(repoName, message);
            }
            this.state.update(true, -1L, true, null);
            return true;
        }
        catch (Exception e)
        {
            modelRepository.clear();
            this.state.update(false, -1L, false, e);
            return false;
        }
        catch (Error e)
        {
            this.state.update(false, -1L, false, e);
            throw e;
        }
    }

    @Override
    public CacheState getCacheState()
    {
        return this.state;
    }
}
