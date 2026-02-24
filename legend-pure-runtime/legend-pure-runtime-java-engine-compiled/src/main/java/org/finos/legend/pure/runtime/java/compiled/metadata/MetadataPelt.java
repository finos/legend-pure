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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementLoader;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.CompiledElementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;

public class MetadataPelt implements Metadata
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataPelt.class);

    private final MetadataIndex metadataIndex;
    private final ElementLoader elementLoader;
    private final ReferenceIdResolver refIdResolver;
    private final ConcurrentMutableMap<String, CoreInstance> instanceCache = ConcurrentHashMap.newMap();

    private MetadataPelt(MetadataIndex metadataIndex, ElementLoader elementLoader)
    {
        this.metadataIndex = metadataIndex;
        this.elementLoader = elementLoader;
        this.refIdResolver = elementLoader.getReferenceIdResolvers().resolver();
    }

    @Override
    public void startTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void commitTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void rollbackTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        // TODO should we validate the classifier?
        // for backward compatibility
        String resolvedId = id.startsWith("Root::") ? id.substring(6) : id;
        return getInstance(resolvedId);
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        if (M3Paths.Package.equals(classifier))
        {
            MutableMap<String, CoreInstance> elements = Maps.mutable.empty();
            this.metadataIndex.forEachPackage(p -> elements.put(p.getPath(), getElementByPath(p.getPath())));
            return elements;
        }
        ImmutableList<ConcreteElementMetadata> metadata = this.metadataIndex.getClassifierElements(classifier);
        if ((metadata == null) || metadata.isEmpty())
        {
            return Maps.immutable.empty();
        }
        MutableMap<String, CoreInstance> elements = Maps.mutable.ofInitialCapacity(metadata.size());
        metadata.forEach(md -> elements.put(md.getPath(), getElementByPath(md.getPath())));
        return elements;
    }

    @Override
    public RichIterable<CoreInstance> getClassifierInstances(String classifier)
    {
        if (M3Paths.Package.equals(classifier))
        {
            return LazyIterate.collect(this.metadataIndex.getAllPackagePaths(), this::getElementByPath);
        }
        ImmutableList<ConcreteElementMetadata> metadata = this.metadataIndex.getClassifierElements(classifier);
        return ((metadata == null) || metadata.isEmpty()) ?
               Lists.immutable.empty() :
               metadata.asLazy().collect(md -> getElementByPath(md.getPath()));
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        String enumId = enumerationName + "." + M3Properties.values + "['" + enumName + "']";
        return getInstance(enumId);
    }

    /**
     * Get an instance by its metadata id. An exception will be thrown if the id is not valid or cannot be resolved.
     *
     * @param id metadata id
     * @return the instance with the given id
     */
    public CoreInstance getInstance(String id)
    {
        return this.instanceCache.getIfAbsentPutWithKey(id, this.refIdResolver::resolveReference);
    }

    /**
     * Return whether the metadata has an element with the given package path.
     *
     * @param path package path
     * @return whether there is an element with the given path
     */
    public boolean hasElement(String path)
    {
        return this.elementLoader.elementExists(path);
    }

    /**
     * Get an element by its package path. Returns null if there is no such element.
     *
     * @param path package path of the element
     * @return element with the given path, or null if there is no such element
     */
    public CoreInstance getElementByPath(String path)
    {
        return this.elementLoader.loadElement(path);
    }

    public static MetadataPelt fromClassLoader(ClassLoader classLoader, String... repositories)
    {
        return fromClassLoader(classLoader, Arrays.asList(repositories));
    }

    public static MetadataPelt fromClassLoader(ClassLoader classLoader, Iterable<? extends String> repositories)
    {
        Objects.requireNonNull(classLoader, "class loader may not be null");
        return builder().withClassLoader(classLoader).withRepositories(repositories).build();
    }

    public static MetadataPelt fromDirectory(ClassLoader classLoader, Path directory, String... repositories)
    {
        return fromDirectory(classLoader, directory, Arrays.asList(repositories));
    }

    public static MetadataPelt fromDirectory(ClassLoader classLoader, Path directory, Iterable<? extends String> repositories)
    {
        Objects.requireNonNull(classLoader, "class loader may not be null");
        Objects.requireNonNull(directory, "directory may not be null");
        return builder().withClassLoader(classLoader).withDirectory(directory).withRepositories(repositories).build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final MutableSet<String> repositories = Sets.mutable.empty();
        private ClassLoader classLoader;
        private Path directory;

        private Builder()
        {
        }

        public Builder withClassLoader(ClassLoader classLoader)
        {
            this.classLoader = classLoader;
            return this;
        }

        public Builder withDirectory(Path directory)
        {
            this.directory = directory;
            return this;
        }

        public Builder withRepository(String repository)
        {
            this.repositories.add(Objects.requireNonNull(repository));
            return this;
        }

        public Builder withRepositories(Iterable<? extends String> repositories)
        {
            repositories.forEach(this::withRepository);
            return this;
        }

        public MetadataPelt build()
        {
            Objects.requireNonNull(this.classLoader, "class loader must be provided");
            if (this.repositories.isEmpty())
            {
                throw new IllegalStateException("At least one repository must be provided");
            }

            long start = System.nanoTime();
            LOGGER.debug("Building metadata for repositories: {}", this.repositories);
            try
            {
                StringIndexer stringIndexer = StringIndexer.builder()
                        .withLoadedExtensions(this.classLoader)
                        .build();
                ConcreteElementDeserializer elementDeserializer = ConcreteElementDeserializer.builder()
                        .withLoadedExtensions(this.classLoader)
                        .withStringIndexer(stringIndexer)
                        .build();
                ModuleMetadataSerializer moduleMetadataSerializer = ModuleMetadataSerializer.builder()
                        .withLoadedExtensions(this.classLoader)
                        .withStringIndexer(stringIndexer)
                        .build();
                FileDeserializer fileDeserializer = FileDeserializer.builder()
                        .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions(this.classLoader).build())
                        .withSerializers(elementDeserializer, moduleMetadataSerializer)
                        .build();

                MutableMap<String, ModuleManifest> manifestsByModule = Maps.mutable.empty();
                Deque<String> modulesToLoad = new ArrayDeque<>(this.repositories);
                while (!modulesToLoad.isEmpty())
                {
                    manifestsByModule.getIfAbsentPutWithKey(modulesToLoad.pollFirst(), m ->
                    {
                        ModuleManifest manifest = (this.directory == null) ?
                                fileDeserializer.deserializeModuleManifest(this.classLoader, m) :
                                fileDeserializer.deserializeModuleManifest(this.directory, m);
                        modulesToLoad.addAll(manifest.getDependencies().castToList());
                        return manifest;
                    });
                }
                MetadataIndex metadataIndex = MetadataIndex.builder().withModules(manifestsByModule.values()).build();
                ElementLoader.Builder elementLoaderBuilder = ElementLoader.builder()
                        .withMetadataIndex(metadataIndex)
                        .withFileDeserializer(fileDeserializer)
                        .withElementBuilder(CompiledElementBuilder.newElementBuilder(this.classLoader))
                        .withAvailableReferenceIdExtensions(this.classLoader)
                        .withDefaultReferenceIdVersion(1);
                if (this.directory == null)
                {
                    elementLoaderBuilder.withClassLoader(this.classLoader);
                }
                else
                {
                    elementLoaderBuilder.withDirectory(this.directory);
                }

                return new MetadataPelt(metadataIndex, elementLoaderBuilder.build());
            }
            finally
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished building metadata in {}s", (end - start) / 1_000_000_000.0);
            }
        }
    }
}
