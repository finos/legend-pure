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

package org.finos.legend.pure.m3.generator.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.generator.Log;
import org.finos.legend.pure.m3.generator.LogToSystemOut;
import org.finos.legend.pure.m3.serialization.compiler.PureCompilerSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementLoader;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureCompilerLoader;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class PureCompilerBinaryGenerator
{
    public static void main(String[] args)
    {
        Path outputDir = Paths.get(args[0]);
        MutableSet<String> modules = Sets.mutable.ofInitialCapacity(args.length - 1);
        if (args.length > 1)
        {
            ArrayIterate.forEach(args, 1, args.length - 1, modules::add);
        }

        serializeModules(outputDir, modules);
    }

    public static void serializeModules(Path outputDirectory, Iterable<String> modules)
    {
        serializeModules(outputDirectory, null, modules, null, new LogToSystemOut());
    }

    public static void serializeModules(Path outputDirectory, ClassLoader classLoader, Iterable<String> modules, Iterable<String> excludedModules, Log log)
    {
        long start = System.nanoTime();
        SetIterable<String> moduleSet = (modules == null) ? Sets.immutable.empty() :
                                        ((modules instanceof SetIterable) ? (SetIterable<String>) modules :
                                         ((modules instanceof Set) ? SetAdapter.adapt((Set<String>) modules) :
                                          Sets.mutable.withAll(modules)));
        String modulesLogString = moduleSet.isEmpty() ? "all modules" : moduleSet.toSortedList().makeString(", ");
        log.info("Starting compilation of " + modulesLogString);
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        ClassLoader currentClassLoader = (classLoader == null) ? previousClassLoader : classLoader;
        if (classLoader != null)
        {
            currentThread.setContextClassLoader(classLoader);
        }
        try
        {
            FilePathProvider filePathProvider = FilePathProvider.builder().withLoadedExtensions(currentClassLoader).build();
            RepositoryInfo repositoryInfo = resolveRepositories(moduleSet, excludedModules, currentClassLoader, filePathProvider, log);
            PureRuntime runtime = compile(currentClassLoader, repositoryInfo.toCompile, log);
            serialize(outputDirectory, repositoryInfo.toSerialize, runtime, filePathProvider, log);
        }
        catch (Throwable t)
        {
            log.error("Error during compilation of " + modulesLogString, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            log.info(String.format("Finished compilation of %s in %fs", modulesLogString, (end - start) / 1_000_000_000.0));
            if (classLoader != null)
            {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        }
    }

    private static RepositoryInfo resolveRepositories(SetIterable<String> modules, Iterable<String> excludedModules, ClassLoader classLoader, FilePathProvider filePathProvider, Log log)
    {
        MutableList<CodeRepository> foundRepos = CodeRepositoryProviderHelper.findCodeRepositories(true).toList();
        log.debug(String.format("Found repositories: %s", foundRepos.asLazy().collect(CodeRepository::getName)));
        CodeRepositorySet.Builder builder = CodeRepositorySet.builder().withCodeRepositories(foundRepos);
        if (excludedModules != null)
        {
            builder.withoutCodeRepositories(excludedModules);
        }
        SetIterable<String> toSerialize;
        if (modules.isEmpty())
        {
            SetIterable<String> excludedSet = (excludedModules == null) ? Sets.immutable.empty() : Sets.mutable.withAll(excludedModules);
            toSerialize = foundRepos.collectIf(
                    repo -> !excludedSet.contains(repo.getName()) && classLoader.getResource(filePathProvider.getModuleManifestResourceName(repo.getName())) == null,
                    CodeRepository::getName,
                    Sets.mutable.empty());
            log.debug(String.format("Selected to serialize: %s", toSerialize.isEmpty() ? "<all>" : toSerialize));
        }
        else
        {
            toSerialize = modules;
        }
        if (toSerialize.notEmpty())
        {
            builder.subset(toSerialize);
        }
        CodeRepositorySet resolvedRepositories = builder.build();
        log.debug("Resolved repositories: " + resolvedRepositories.getRepositoryNames());
        return new RepositoryInfo(resolvedRepositories, toSerialize);
    }

    private static PureRuntime compile(ClassLoader classLoader, CodeRepositorySet codeRepositories, Log log)
    {
        long start = System.nanoTime();
        log.info("Starting compilation");
        try
        {
            CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(classLoader, codeRepositories.getRepositories()));
            PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                    .setTransactionalByDefault(false)
                    .build();

            PureCompilerLoader loader = PureCompilerLoader.newLoader(classLoader);
            MutableList<String> reposToLoad = Lists.mutable.empty();
            MutableList<String> reposToCompile = Lists.mutable.empty();
            codeRepositories.getRepositoryNames().forEach(r -> (loader.canLoad(r) ? reposToLoad : reposToCompile).add(r));

            if (reposToLoad.isEmpty())
            {
                long initStart = System.nanoTime();
                log.info("No repositories to load: initializing runtime");
                runtime.initialize();
                long initEnd = System.nanoTime();
                log.info(String.format("Finished initializing runtime in %fs", (initEnd - initStart) / 1_000_000_000.0));
                return runtime;
            }

            long initStart = System.nanoTime();
            log.info("Loading repositories: " + reposToLoad);
            loader.load(runtime, reposToLoad, false, new ElementLoader.BackReferenceFilter()
            {
                @Override
                public boolean test(String module, String elementPath, String instanceReferenceId, BackReference.Application application)
                {
                    return false;
                }

                @Override
                public boolean test(String module, String elementPath, String instanceReferenceId, BackReference.ModelElement modelElement)
                {
                    return false;
                }

                @Override
                public boolean test(String module, String elementPath, String instanceReferenceId, BackReference.ReferenceUsage referenceUsage)
                {
                    return false;
                }

                @Override
                public boolean test(String module, String elementPath, String instanceReferenceId, BackReference.Specialization specialization)
                {
                    return false;
                }
            });
            long initEnd = System.nanoTime();
            log.info(String.format("Finished loading repositories in %fs", (initEnd - initStart) / 1_000_000_000.0));

            if (reposToCompile.notEmpty())
            {
                long compileStart = System.nanoTime();
                log.info("Compiling repositories: " + reposToCompile);
                runtime.loadAndCompile(reposToCompile.asLazy().flatCollect(codeStorage::getFileOrFiles).select(CodeStorageTools::isPureFilePath));
                long compileEnd = System.nanoTime();
                log.info(String.format("Finished compiling repositories in %fs", (compileEnd - compileStart) / 1_000_000_000.0));
            }

            return runtime;
        }
        catch (PureCompilationException | PureParserException e)
        {
            log.error("Compilation failed", e);
            throw e;
        }
        catch (Throwable t)
        {
            log.error("Error during compilation", t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            log.info(String.format("Finished compilation in %fs", (end - start) / 1_000_000_000.0));
        }
    }

    private static void serialize(Path outputDirectory, SetIterable<String> modules, PureRuntime runtime, FilePathProvider filePathProvider, Log log)
    {
        long start = System.nanoTime();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        log.info("Starting serialization");
        try
        {
            ModuleMetadataGenerator moduleMetadataGenerator = ModuleMetadataGenerator.fromPureRuntime(runtime);
            ConcreteElementSerializer elementSerializer = ConcreteElementSerializer.builder(runtime.getProcessorSupport()).withLoadedExtensions(classLoader).build();
            ModuleMetadataSerializer moduleMetadataSerializer = ModuleMetadataSerializer.builder().withLoadedExtensions(classLoader).build();
            FileSerializer fileSerializer = FileSerializer.builder()
                    .withFilePathProvider(filePathProvider)
                    .withSerializers(elementSerializer, moduleMetadataSerializer)
                    .build();
            PureCompilerSerializer serializer = PureCompilerSerializer.builder()
                    .withFileSerializer(fileSerializer)
                    .withModuleMetadataGenerator(moduleMetadataGenerator)
                    .withProcessorSupport(runtime.getProcessorSupport())
                    .build();

            if (modules.isEmpty())
            {
                serializer.serializeAll(outputDirectory);
            }
            else
            {
                serializer.serializeModules(outputDirectory, modules);
            }
        }
        catch (Throwable t)
        {
            log.error("Error during serialization", t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            log.info(String.format("Finished serialization in %fs", (end - start) / 1_000_000_000.0));
        }
    }

    private static class RepositoryInfo
    {
        private final CodeRepositorySet toCompile;
        private final SetIterable<String> toSerialize;

        private RepositoryInfo(CodeRepositorySet toCompile, SetIterable<String> toSerialize)
        {
            this.toCompile = toCompile;
            this.toSerialize = toSerialize;
        }
    }
}
