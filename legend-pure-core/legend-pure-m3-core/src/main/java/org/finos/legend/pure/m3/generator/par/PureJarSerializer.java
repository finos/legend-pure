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

package org.finos.legend.pure.m3.generator.par;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
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

    public static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, CodeRepositorySet repositories, Log log) throws IOException
    {
        writePureRepositoryJars(outputDirectory, sourceDirectory, platformVersion, modelVersion, repositories, Thread.currentThread().getContextClassLoader(), log);
    }

    public static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, CodeRepositorySet repositories, ClassLoader classLoader, Log log) throws IOException
    {
        PureRuntime runtime;
        RichIterable<CodeRepository> repositoriesToSerialize;
        if (sourceDirectory == null)
        {
            log.info("    *Building code storage leveraging the class loader (as sourceDirectory is not specified)");
            ClassLoaderCodeStorage cs = new ClassLoaderCodeStorage(classLoader, repositories.getRepositories());
            log.info("      " + cs.getAllRepositories().collect(CodeRepository::getName) + " - " + cs.getUserFiles().size() + " files");
            MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(cs);

            Message message = getMessage(log, "      ");
            log.info("    *Starting file compilation");
            runtime = new PureRuntimeBuilder(codeStorage).withMessage(message).setTransactionalByDefault(false).buildAndInitialize(message);
            repositoriesToSerialize = repositories.getRepositories();
            log.info("      -> Finished compilation");
        }
        else
        {
            log.info("    *Building code storage leveraging the sourceDirectory " + sourceDirectory);

            MutableList<CodeRepository> repositoriesWithSource = Lists.mutable.empty();
            MutableList<CodeRepository> repositoriesWithoutSource = Lists.mutable.empty();
            repositories.getRepositories().forEach(r -> (Files.exists(sourceDirectory.resolve(r.getName())) ? repositoriesWithSource : repositoriesWithoutSource).add(r));

            // Code Storages
            MutableList<RepositoryCodeStorage> codeStoragesFromSource = repositoriesWithSource.collect(r -> new MutableFSCodeStorage(r, sourceDirectory.resolve(r.getName())));
            MutableList<RepositoryCodeStorage> allCodeStorage = Lists.mutable.withAll(codeStoragesFromSource).with(new ClassLoaderCodeStorage(classLoader, repositoriesWithoutSource));
            log.info("    *Loading the following repo from PARs: " + repositoriesWithoutSource.collect(CodeRepository::getName));

            // Build the runtime
            MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(allCodeStorage.toArray(new RepositoryCodeStorage[0]));
            runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).build();

            // Load the PARS
            Message message = getMessage(log, "    ");
            PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(repositoriesWithoutSource.collect(CodeRepository::getName), classLoader, message));
            GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
            loader.loadAll(message);

            // Compile Sources
            log.info("    *Starting file compilation");
            codeStoragesFromSource.forEach(r -> log.info("      " + r.getAllRepositories().collect(CodeRepository::getName) + " - " + r.getUserFiles().size() + " files (from: " + ((FSCodeStorage) r).getRoot() + ")"));
            repositoriesWithSource.forEach(repository ->
            {
                Path path = sourceDirectory.resolve(repository.getName());
                log.info("      Compiling repository '" + repository.getName() + "' in " + path);
                MutableFSCodeStorage fs = new MutableFSCodeStorage(repository, path);
                RichIterable<String> filesToCompile = fs.getUserFiles();
                log.info("        Found " + filesToCompile.size() + " files");
                runtime.loadAndCompile(filesToCompile);
                log.info("        -> Finished compiling repository '" + repository.getName() + "'");
            });

            repositoriesToSerialize = repositoriesWithSource;
            log.info("      -> Finished compilation");
        }

        Files.createDirectories(outputDirectory);
        log.info("    *Starting serialization");
        for (String repositoryName : repositoriesToSerialize.collect(CodeRepository::getName))
        {
            Path outputFile = outputDirectory.resolve("pure-" + repositoryName + "." + ARCHIVE_FILE_EXTENSION);
            log.info("      Writing " + outputFile);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile)))
            {
                BinaryModelRepositorySerializer.serialize(outputStream, platformVersion, modelVersion, repositoryName, runtime);
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
                if (!message.startsWith("Binding") && !message.startsWith("Parsing") && !message.startsWith("Unbinding"))
                {
                    log.info(prefix + message);
                }
            }
        };
    }
}
