// Copyright 2025 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.PureCompilerSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureCompilerLoader;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m4.ModelRepository;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;

public class FSPeltPureGraphCache extends AbstractFSDirectoryPureGraphCache
{
    private static final String ROOT_REPOSITORY_NAME = "root";

    private final boolean allowBuildingFromRepoSubset;

    public FSPeltPureGraphCache(Path cacheDirectory, boolean allowBuildingFromRepoSubset, Message message)
    {
        super(cacheDirectory);
        this.allowBuildingFromRepoSubset = allowBuildingFromRepoSubset;
        initializeCacheState(message);
    }

    public FSPeltPureGraphCache(Path cacheDirectory, Message message)
    {
        this(cacheDirectory, false, message);
    }

    public FSPeltPureGraphCache(Path cacheDirectory)
    {
        this(cacheDirectory, false, null);
    }

    @Override
    public boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        RepositoryCodeStorage codeStorage = this.pureRuntime.getCodeStorage();
        MutableList<String> repoNames = codeStorage.getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty());
        if (shouldAddRootRepo())
        {
            repoNames.add(ROOT_REPOSITORY_NAME);
        }

        PureCompilerLoader loader = PureCompilerLoader.newLoader(Thread.currentThread().getContextClassLoader(), getCacheLocation());
        boolean success = this.allowBuildingFromRepoSubset ?
                          !loader.loadIfPossible(this.pureRuntime).isEmpty() :
                          loader.loadAll(this.pureRuntime);
        updateCacheState(success);
        return success;
    }

    @Override
    protected void writeCaches()
    {
        // TODO only serialize changed items
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ModuleMetadataGenerator moduleMetadataGenerator = ModuleMetadataGenerator.fromPureRuntime(this.pureRuntime);
        ConcreteElementSerializer elementSerializer = ConcreteElementSerializer.builder(this.pureRuntime.getProcessorSupport()).withLoadedExtensions(classLoader).build();
        ModuleMetadataSerializer moduleMetadataSerializer = ModuleMetadataSerializer.builder().withLoadedExtensions(classLoader).build();
        FilePathProvider filePathProvider = FilePathProvider.builder().withLoadedExtensions(classLoader).build();
        FileSerializer fileSerializer = FileSerializer.builder()
                .withFilePathProvider(filePathProvider)
                .withSerializers(elementSerializer, moduleMetadataSerializer)
                .build();
        PureCompilerSerializer serializer = PureCompilerSerializer.builder()
                .withFileSerializer(fileSerializer)
                .withModuleMetadataGenerator(moduleMetadataGenerator)
                .withProcessorSupport(this.pureRuntime.getProcessorSupport())
                .build();
        serializer.serializeAll(getCacheLocation(), shouldAddRootRepo());
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

    @Override
    protected long getCacheSize()
    {
        if (Files.notExists(getCacheLocation()))
        {
            return -1L;
        }

        try (Stream<Path> stream = Files.walk(getCacheLocation()))
        {
            return stream.map(FileTools::getBasicFileAttributes)
                    .filter(Objects::nonNull)
                    .filter(BasicFileAttributes::isRegularFile)
                    .mapToLong(BasicFileAttributes::size)
                    .sum();
        }
        catch (Exception ignore)
        {
            return -1L;
        }
    }

    private boolean shouldAddRootRepo()
    {
        return this.pureRuntime.getCodeStorage().isFile(WelcomeCodeStorage.WELCOME_FILE_PATH);
    }
}
