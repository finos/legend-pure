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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.lazy.generator.M3GeneratedLazyElementBuilder;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementLoader;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementLoader.BackReferenceFilter;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public abstract class PureCompilerLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureCompilerLoader.class);

    final ClassLoader classLoader;
    final FileDeserializer fileDeserializer;

    private PureCompilerLoader(ClassLoader classLoader)
    {
        this.classLoader = Objects.requireNonNull(classLoader);
        StringIndexer stringIndexer = StringIndexer.builder()
                .withLoadedExtensions(classLoader)
                .build();
        ConcreteElementDeserializer elementDeserializer = ConcreteElementDeserializer.builder()
                .withLoadedExtensions(classLoader)
                .withStringIndexer(stringIndexer)
                .build();
        ModuleMetadataSerializer moduleMetadataSerializer = ModuleMetadataSerializer.builder()
                .withLoadedExtensions(classLoader)
                .withStringIndexer(stringIndexer)
                .build();
        this.fileDeserializer = FileDeserializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions(classLoader).build())
                .withSerializers(elementDeserializer, moduleMetadataSerializer)
                .build();
    }

    /**
     * Return whether the given repository can be loaded.
     *
     * @param repository repository name
     * @return whether the given repository can be loaded
     */
    public boolean canLoad(String repository)
    {
        return moduleManifestExists(repository);
    }

    /**
     * Load the given repositories.
     *
     * @param runtime      Pure runtime
     * @param repositories repositories to load
     */
    public void load(PureRuntime runtime, Iterable<? extends String> repositories)
    {
        load(runtime, repositories, true);
    }

    /**
     * Load the given repositories.
     *
     * @param runtime                     Pure runtime
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     */
    public void load(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary)
    {
        load(runtime, repositories, initializeURLPatternLibrary, null);
    }

    /**
     * Load the given repositories.
     *
     * @param runtime                     Pure runtime
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @param backReferenceFilter         optional back reference loading filter
     */
    public void load(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary, BackReferenceFilter backReferenceFilter)
    {
        Set<? extends String> repoSet = (repositories instanceof Set) ? (Set<? extends String>) repositories : Sets.mutable.withAll(repositories);
        if (!repoSet.isEmpty())
        {
            load(runtime, Lists.mutable.withAll(repoSet), initializeURLPatternLibrary, backReferenceFilter);
        }
    }

    /**
     * Load as many of the given repositories as possible. Returns a (possibly empty) set of repositories that were
     * loaded.
     *
     * @param runtime      Pure runtime to initialize
     * @param repositories repositories to load
     * @return repositories that were loaded
     */
    public Set<String> loadIfPossible(PureRuntime runtime, Iterable<? extends String> repositories)
    {
        return loadIfPossible(runtime, repositories, false);
    }

    /**
     * Load as many of the given repositories as possible. Returns a (possibly empty) set of repositories that were
     * loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @return repositories that were loaded
     */
    public Set<String> loadIfPossible(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary)
    {
        return loadIfPossible(runtime, repositories, initializeURLPatternLibrary, null);
    }

    /**
     * Load as many of the given repositories as possible. Returns a (possibly empty) set of repositories that were
     * loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @param backReferenceFilter         optional back reference loading filter
     * @return repositories that were loaded
     */
    public Set<String> loadIfPossible(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary, BackReferenceFilter backReferenceFilter)
    {
        MutableSet<String> loadable = Sets.mutable.empty();
        LazyIterate.select(repositories, this::canLoad).forEach(loadable::add);
        if (loadable.notEmpty())
        {
            load(runtime, Lists.mutable.withAll(loadable), initializeURLPatternLibrary, backReferenceFilter);
        }
        return loadable;
    }

    /**
     * Load as many of the repositories in the runtime as possible. Returns a (possibly empty) set of repositories that
     * were loaded.
     *
     * @param runtime Pure runtime to initialize
     * @return repositories that were loaded
     */
    public Set<String> loadIfPossible(PureRuntime runtime)
    {
        return loadIfPossible(runtime, true);
    }

    /**
     * Load as many of the repositories in the runtime as possible. Returns a (possibly empty) set of repositories that
     * were loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @return repositories that were loaded
     */
    public Set<String> loadIfPossible(PureRuntime runtime, boolean initializeURLPatternLibrary)
    {
        return loadIfPossible(runtime, runtime.getCodeStorage().getAllRepositories().asLazy().collect(CodeRepository::getName), initializeURLPatternLibrary);
    }

    /**
     * Load all repositories in the runtime if possible; otherwise, load none. Returns a boolean indicating whether all
     * repositories were loaded.
     *
     * @param runtime Pure runtime to initialize
     * @return true if all repositories were loaded; false if none
     */
    public boolean loadAll(PureRuntime runtime)
    {
        return loadAll(runtime, true);
    }

    /**
     * Load all the given repositories if possible; otherwise, load none. Returns a boolean indicating whether all
     * repositories were loaded.
     *
     * @param runtime      Pure runtime to initialize
     * @param repositories repositories to load
     * @return true if all repositories were loaded; false if none
     */
    public boolean loadAll(PureRuntime runtime, Iterable<? extends String> repositories)
    {
        return loadAll(runtime, repositories, true);
    }

    /**
     * Load all repositories in the runtime if possible; otherwise, load none. Returns a boolean indicating whether all
     * repositories were loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @return true if all repositories were loaded; false if none
     */
    public boolean loadAll(PureRuntime runtime, boolean initializeURLPatternLibrary)
    {
        return loadAll(runtime, runtime.getCodeStorage().getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty()), initializeURLPatternLibrary);
    }

    /**
     * Load all the given repositories if possible; otherwise, load none. Returns a boolean indicating whether all
     * repositories were loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @return true if all repositories were loaded; false if none
     */
    public boolean loadAll(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary)
    {
        return loadAll(runtime, repositories, initializeURLPatternLibrary, null);
    }

    /**
     * Load all the given repositories if possible; otherwise, load none. Returns a boolean indicating whether all
     * repositories were loaded.
     *
     * @param runtime                     Pure runtime to initialize
     * @param repositories                repositories to load
     * @param initializeURLPatternLibrary whether to initialize the URL pattern library
     * @param backReferenceFilter         optional back reference loading filter
     * @return true if all repositories were loaded; false if none
     */
    public boolean loadAll(PureRuntime runtime, Iterable<? extends String> repositories, boolean initializeURLPatternLibrary, BackReferenceFilter backReferenceFilter)
    {
        Set<? extends String> repoSet = (repositories instanceof Set) ? (Set<? extends String>) repositories : Sets.mutable.withAll(repositories);
        return loadAll(runtime, Lists.mutable.withAll(repoSet), initializeURLPatternLibrary, backReferenceFilter);
    }

    private boolean loadAll(PureRuntime runtime, MutableList<? extends String> repositories, boolean initializeURLPatternLibrary, BackReferenceFilter backReferenceFilter)
    {
        if (!repositories.allSatisfy(this::canLoad))
        {
            // if we cannot load all repositories, do not load any
            return false;
        }

        load(runtime, repositories, initializeURLPatternLibrary, backReferenceFilter);
        return true;
    }

    private void load(PureRuntime runtime, MutableList<? extends String> repositories, boolean initializeURLPatternLibrary, BackReferenceFilter backReferenceFilter)
    {
        long start = System.nanoTime();
        LOGGER.debug("Loading repos {}", repositories);
        try
        {
            long metadataStart = System.nanoTime();
            LOGGER.debug("Loading metadata index");
            MetadataIndex metadataIndex = MetadataIndex.builder()
                    .withModules(LazyIterate.collect(repositories, this::loadModuleManifest))
                    .build();
            long metadataEnd = System.nanoTime();
            LOGGER.debug("Finished loading metadata index in {}ns", metadataEnd - metadataStart);

            long loaderStart = System.nanoTime();
            LOGGER.debug("Building element loader");
            ModelRepository repository = runtime.getModelRepository();
            ElementLoader elementLoader = ElementLoader.builder()
                    .withMetadataIndex(metadataIndex)
                    .withClassLoader(this.classLoader)
                    .withAvailableReferenceIdExtensions(this.classLoader)
                    .withFileDeserializer(this.fileDeserializer)
                    .withElementBuilder(M3GeneratedLazyElementBuilder.newElementBuilder(this.classLoader, repository))
                    .withBackReferenceFilter(backReferenceFilter)
                    .build();
            long loaderEnd = System.nanoTime();
            LOGGER.debug("Finished building element loader in {}ns", loaderEnd - loaderStart);

            // initialize top level elements
            long topLevelStart = System.nanoTime();
            LOGGER.debug("Initializing top level elements");
            GraphTools.getTopLevelNames().forEach(n -> repository.addTopLevel(elementLoader.loadElementStrict(n)));
            long topLevelEnd = System.nanoTime();
            LOGGER.debug("Finished initializing top level elements in {}ns", topLevelEnd - topLevelStart);

            // initialize source registry
            long srcRegStart = System.nanoTime();
            LOGGER.debug("Initializing the source registry");
            ParserLibrary parserLibrary = runtime.getIncrementalCompiler().getParserLibrary();
            repositories.forEach(repo -> loadModuleSourceMetadata(repo).forEachSource(sourceMetadata ->
            {
                String sourceId = sourceMetadata.getSourceId();
                runtime.loadSourceIfLoadable(sourceId);
                Source source = runtime.getSourceById(sourceId);
                if (source == null)
                {
                    throw new RuntimeException("Unknown source: " + sourceId);
                }

                MutableListMultimap<Parser, CoreInstance> instancesByParser = Multimaps.mutable.list.empty();
                sourceMetadata.getSections().forEach(section ->
                {
                    Parser parser = parserLibrary.getParser(section.getParser()).newInstance(parserLibrary);
                    MutableList<CoreInstance> elements = section.getElements().collect(elementLoader::loadElementStrict, Lists.mutable.ofInitialCapacity(section.getElements().size()));
                    instancesByParser.putAll(parser, elements);
                });

                source.setCompiled(true);
                source.linkInstances(instancesByParser);
            }));
            long srcRegEnd = System.nanoTime();
            LOGGER.debug("Finished initializing the source registry in {}ns", srcRegEnd - srcRegStart);

            // load functions by name
            long fnsByNameStart = System.nanoTime();
            LOGGER.debug("Loading functions by name");
            Context context = runtime.getContext();
            repositories.forEach(repo -> loadModuleFunctionsByName(repo).getFunctionsByName().forEach(fbn ->
            {
                String funcName = fbn.getFunctionName();
                ImmutableList<String> funcPaths = fbn.getFunctions();
                context.registerFunctionsByName(funcName, funcPaths.collect(elementLoader::loadElementStrict, Lists.mutable.ofInitialCapacity(funcPaths.size())));
            }));
            long fnsByNameEnd = System.nanoTime();
            LOGGER.debug("Finished loading functions by name in {}ns", fnsByNameEnd - fnsByNameStart);

            // update URL pattern library
            if (initializeURLPatternLibrary)
            {
                long patternLibStart = System.nanoTime();
                LOGGER.debug("Initializing the URL pattern library");
                CoreInstance serviceProfile = elementLoader.loadElement(M3Paths.service);
                if (serviceProfile != null)
                {
                    CoreInstance urlTag = Profile.findTag(serviceProfile, "url");
                    if (urlTag != null)
                    {
                        URLPatternLibrary patternLibrary = runtime.getURLPatternLibrary();
                        ProcessorSupport processorSupport = runtime.getProcessorSupport();
                        urlTag.getValueForMetaPropertyToMany(M3Properties.modelElements).forEach(elt ->
                        {
                            try
                            {
                                patternLibrary.possiblyRegister(elt, processorSupport);
                            }
                            catch (Exception e)
                            {
                                LOGGER.error("Error registering element with URL pattern library", e);
                            }
                        });
                    }
                }
                long patternLibEnd = System.nanoTime();
                LOGGER.debug("Finished initializing the URL pattern library in {}ns", patternLibEnd - patternLibStart);
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("Error loading repos {}", repositories, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished loading repos {} in {}ns", repositories, end - start);
        }
    }

    abstract boolean moduleManifestExists(String repository);

    abstract ModuleManifest loadModuleManifest(String repository);

    abstract ModuleSourceMetadata loadModuleSourceMetadata(String repository);

    abstract ModuleFunctionNameMetadata loadModuleFunctionsByName(String repository);

    public static PureCompilerLoader newLoader(ClassLoader classLoader)
    {
        return new ClassLoaderPureCompilerLoader(classLoader);
    }

    public static PureCompilerLoader newLoader(ClassLoader classLoader, Path directory)
    {
        return new DirectoryPureCompilerLoader(classLoader, directory);
    }

    private static class ClassLoaderPureCompilerLoader extends PureCompilerLoader
    {
        private ClassLoaderPureCompilerLoader(ClassLoader classLoader)
        {
            super(classLoader);
        }

        @Override
        boolean moduleManifestExists(String repository)
        {
            return this.fileDeserializer.moduleManifestExists(this.classLoader, repository);
        }

        @Override
        ModuleManifest loadModuleManifest(String repository)
        {
            return this.fileDeserializer.deserializeModuleManifest(this.classLoader, repository);
        }

        @Override
        ModuleSourceMetadata loadModuleSourceMetadata(String repository)
        {
            return this.fileDeserializer.deserializeModuleSourceMetadata(this.classLoader, repository);
        }

        @Override
        ModuleFunctionNameMetadata loadModuleFunctionsByName(String repository)
        {
            return this.fileDeserializer.deserializeModuleFunctionNameMetadata(this.classLoader, repository);
        }
    }

    private static class DirectoryPureCompilerLoader extends PureCompilerLoader
    {
        private final Path directory;

        private DirectoryPureCompilerLoader(ClassLoader classLoader, Path directory)
        {
            super(classLoader);
            this.directory = Objects.requireNonNull(directory);
        }

        @Override
        boolean moduleManifestExists(String repository)
        {
            return this.fileDeserializer.moduleManifestExists(this.directory, repository);
        }

        @Override
        ModuleManifest loadModuleManifest(String repository)
        {
            return this.fileDeserializer.deserializeModuleManifest(this.directory, repository);
        }

        @Override
        ModuleSourceMetadata loadModuleSourceMetadata(String repository)
        {
            return this.fileDeserializer.deserializeModuleSourceMetadata(this.directory, repository);
        }

        @Override
        ModuleFunctionNameMetadata loadModuleFunctionsByName(String repository)
        {
            return this.fileDeserializer.deserializeModuleFunctionNameMetadata(this.directory, repository);
        }
    }
}
