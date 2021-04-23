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

package org.finos.legend.pure.maven.par;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.AbstractRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.EmptyCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.FSCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelRepositorySerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PureJarSerializer
{
    public static final String ARCHIVE_FILE_EXTENSION = "par";

    public static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, Iterable<CodeRepository> repositories, Log log) throws IOException
    {
        writePureRepositoryJars(outputDirectory, sourceDirectory, platformVersion, Sets.immutable.withAll(repositories), log);
    }

    private static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, RichIterable<CodeRepository> repositories, Log log) throws IOException
    {
        SetIterable<CodeRepository> repositoriesForCompilation = PureCodeStorage.getRepositoryDependencies(PureRepositoriesExternal.repositories(), repositories);
        PureRuntime runtime;
        RichIterable<CodeRepository> repositoriesToSerialize;

        if (null == sourceDirectory)
        {
            log.info("    *Building code storage leveraging the class loader (as sourceDirectory is not specified)");
            ClassLoaderCodeStorage cs = new ClassLoaderCodeStorage(repositoriesForCompilation);
            log.info("      " + cs.getRepositories().collect(CodeRepository::getName) + " - " + cs.getUserFiles().size() + " files");
            MutableCodeStorage codeStorage = new PureCodeStorage(null, cs);

            Message message = getMessage(log, "      ");
            log.info("    *Starting file compilation");
            runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).buildAndInitialize(message);
            repositoriesToSerialize = repositoriesForCompilation;
            log.info("      -> Finished compilation");
        }
        else
        {
            log.info("    *Building code storage leveraging the sourceDirectory "+sourceDirectory);

            PartitionIterable<CodeRepository> partition = repositories.partition(r -> Files.exists(sourceDirectory.resolve(r.getName())));
            RichIterable<CodeRepository> repositoriesWithSource = partition.getSelected();
            RichIterable<CodeRepository> repositoriesWithoutSource = partition.getRejected();

            // Code Storages
            MutableList<AbstractRepositoryCodeStorage> codeStoragesFromSource = Lists.mutable.withAll(repositoriesWithSource.collect(r -> new MutableFSCodeStorage(r, sourceDirectory.resolve(r.getName()))));
            MutableList<AbstractRepositoryCodeStorage> allCodeStorage = Lists.mutable.withAll(codeStoragesFromSource).withAll(repositoriesWithoutSource.collect(EmptyCodeStorage::new));
            log.info("    *Loading the following repo from PARs: "+repositoriesWithoutSource.collect(CodeRepository::getName));

            // Build the runtime
            MutableCodeStorage codeStorage = new PureCodeStorage(sourceDirectory, allCodeStorage.toArray(new RepositoryCodeStorage[0]));
            runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).build();

            // Load the PARS
            MutableList<String> namesOfRepoWithoutAvailableSource = repositoriesWithoutSource.collect(CodeRepository::getName).toList();
            Message message = getMessage(log, "    ");
            PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(namesOfRepoWithoutAvailableSource, PureJarSerializer.class.getClassLoader(), message));
            final GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
            loader.loadAll(message);

            // Compile Sources
            log.info("    *Starting file compilation");
            codeStoragesFromSource.forEach(r -> log.info("      " + r.getRepositories().collect(CodeRepository::getName) + " - " + r.getUserFiles().size() + " files (from: " + ((FSCodeStorage) r).getRoot() + ")"));
            for (CodeRepository repository : repositoriesWithSource)
            {
                Path path = sourceDirectory.resolve(repository.getName());
                log.info("      Compiling repository '" + repository.getName() + "' in " + path);
                MutableFSCodeStorage fs = new MutableFSCodeStorage(repository, path);
                log.info("        Found " + fs.getUserFiles().size() + " files");
                runtime.loadAndCompile(new PureCodeStorage(sourceDirectory, fs).getUserFiles());
                log.info("        -> Finished compiling repository '" + repository.getName() + "'");
            }

            repositoriesToSerialize = repositoriesWithSource;
            log.info("      -> Finished compilation");
        }

        Files.createDirectories(outputDirectory);
        log.info("    *Starting serialization");
        for (String repositoryName : repositoriesToSerialize.collect(CodeRepository::getName))
        {
            Path outputFile = outputDirectory.resolve("pure-" + repositoryName + "." + ARCHIVE_FILE_EXTENSION);
            log.info("      Writing "+outputFile);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile)))
            {
                BinaryModelRepositorySerializer.serialize(outputStream, platformVersion, null, repositoryName, runtime);
            }
        }
    }

    private static Message getMessage(Log log, String prefix)
    {
        return new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                log.info(prefix + message);
            }
        };
    }
}
