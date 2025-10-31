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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.RepositoryComparator;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelRepositorySerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarTools;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.forkjoin.ForkJoinTools;
import org.finos.legend.pure.m4.ModelRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;

public class FSGraphLoaderPureGraphCache extends AbstractFSDirectoryPureGraphCache
{
    private static final String ROOT_REPOSITORY_NAME = "root";

    private final boolean allowBuildingFromRepoSubset;
    private final ForkJoinPool forkJoinPool;

    public FSGraphLoaderPureGraphCache(Path cacheDirectory, boolean allowBuildingFromRepoSubset, ForkJoinPool forkJoinPool, Message message)
    {
        super(cacheDirectory);
        this.allowBuildingFromRepoSubset = allowBuildingFromRepoSubset;
        this.forkJoinPool = forkJoinPool;
        initializeCacheState(message);
    }

    public FSGraphLoaderPureGraphCache(Path cacheDirectory, boolean allowBuildingFromRepoSubset, Message message)
    {
        this(cacheDirectory, allowBuildingFromRepoSubset, null, message);
    }

    public FSGraphLoaderPureGraphCache(Path cacheDirectory, Message message)
    {
        this(cacheDirectory, false, null, message);
    }

    public FSGraphLoaderPureGraphCache(Path cacheDirectory)
    {
        this(cacheDirectory, false, null, null);
    }

    @Override
    public boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        RepositoryCodeStorage codeStorage = this.pureRuntime.getCodeStorage();
        MutableList<String> repoNames = codeStorage.getAllRepositories().collect(CodeRepository::getName).toSortedList(new RepositoryComparator(codeStorage.getAllRepositories()));
        if (shouldAddRootRepo())
        {
            repoNames.add(ROOT_REPOSITORY_NAME);
        }
        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibraryFromDirectory(getCacheLocation());
        GraphLoader loader = new GraphLoader(modelRepository, context, library, this.pureRuntime.getIncrementalCompiler().getDslLibrary(), sources, null, jarLibrary, this.forkJoinPool);
        if (this.allowBuildingFromRepoSubset)
        {
            repoNames.removeIf(repoName -> !loader.isKnownRepository(repoName));
        }
        repoNames.forEach(repoName -> loader.loadRepository(repoName, message));
        updateCacheState();
        return true;
    }

    @Override
    protected void writeCaches()
    {
        RichIterable<String> repoNames = this.pureRuntime.getCodeStorage().getAllRepositories().collect(CodeRepository::getName);
        if (shouldAddRootRepo())
        {
            repoNames = repoNames.toList().with(null);
        }
        Procedure<String> serializeRepo = repoName ->
        {
            Path repoJarPath = getRepositoryJarPath(repoName);
            try (OutputStream stream = Files.newOutputStream(repoJarPath))
            {
                BinaryModelRepositorySerializer.serialize(stream, repoName, this.pureRuntime);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error writing cache for " + repoName, e);
            }
        };
        if (this.forkJoinPool == null)
        {
            repoNames.forEach(serializeRepo);
        }
        else
        {
            ForkJoinTools.forEach(this.forkJoinPool, ListHelper.wrapListIterable(repoNames), serializeRepo, 1);
        }
    }

    @Override
    protected boolean cacheExists()
    {
        if (Files.notExists(getCacheLocation()))
        {
            return false;
        }

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(getCacheLocation()))
        {
            return dirStream.iterator().hasNext();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean shouldAddRootRepo()
    {
        return this.pureRuntime.getCodeStorage().isFile(WelcomeCodeStorage.WELCOME_FILE_PATH);
    }

    private Path getRepositoryJarPath(String repositoryName)
    {
        return getCacheLocation().resolve(resolveRepositoryName(repositoryName) + PureRepositoryJarTools.PURE_JAR_EXTENSION);
    }

    private String resolveRepositoryName(String repositoryName)
    {
        return (repositoryName == null) ? ROOT_REPOSITORY_NAME : repositoryName;
    }
}
