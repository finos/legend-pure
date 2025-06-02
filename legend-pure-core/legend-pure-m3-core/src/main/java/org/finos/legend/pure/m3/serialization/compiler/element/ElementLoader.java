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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceVisitor;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.PackageableElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdExtension;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ElementLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementLoader.class);

    private final MetadataIndex index;
    private final ElementBuilder builder;
    private final ReferenceIdResolvers referenceIds;
    private final BackReferenceFilter backRefFilter;
    private final ConcurrentMutableMap<String, AtomicReference<CoreInstance>> cache = ConcurrentHashMap.newMap();

    private ElementLoader(MetadataIndex index, ElementBuilder builder, ReferenceIdResolvers.Builder referenceIdsBuilder, BackReferenceFilter backRefFilter)
    {
        this.index = Objects.requireNonNull(index);
        this.builder = Objects.requireNonNull(builder);
        this.referenceIds = referenceIdsBuilder.withPackagePathResolver(this::loadElement).build();
        this.backRefFilter = backRefFilter;

        long start = System.nanoTime();
        LOGGER.debug("Initializing element builder");
        try
        {
            this.builder.initialize(this);
        }
        catch (Throwable t)
        {
            LOGGER.error("Error initializing element builder", t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished initializing element builder in {}s", (end - start) / 1_000_000_000.0);
        }
    }

    /**
     * Check if an element exists. This will not load the element.
     *
     * @param path package path of the element
     * @return whether the element exists
     */
    public boolean elementExists(String path)
    {
        return elementPresentInMetadata(path);
    }

    /**
     * Load an element. Returns null if the element does not exist. This method is thread safe, and a given element will
     * be loaded at most once. This is equivalent to calling {@code loadElement(path, false)}.
     *
     * @param path package path of the element
     * @return the loaded element, or null if it does not exist
     * @see #loadElement(String, boolean)
     */
    public CoreInstance loadElement(String path)
    {
        return loadElement(path, false);
    }

    /**
     * Load an element. Throws an exception if the element does not exist. This method is thread safe, and a given
     * element will be loaded at most once. This is equivalent to calling {@code loadElement(path, true)}.
     *
     * @param path package path of the element
     * @return the loaded element
     * @throws IllegalArgumentException if path is null or the element does not exist
     * @see #loadElement(String, boolean)
     */
    public CoreInstance loadElementStrict(String path)
    {
        return loadElement(path, true);
    }

    /**
     * Load an element. If errorIfNotFound is true, throws an exception if the element does not exist; otherwise, it
     * returns null. This method is thread safe, and a given element will be loaded at most once.
     *
     * @param path package path of the element
     * @param errorIfNotFound whether to throw an exception if the element does not exist
     * @return the loaded element, or null if it does not exist and errorIfNotFound is false
     * @throws IllegalArgumentException if errorIfNotFound is true and path is null or the element does not exist
     */
    public CoreInstance loadElement(String path, boolean errorIfNotFound)
    {
        if (path == null)
        {
            if (errorIfNotFound)
            {
                throw new IllegalArgumentException("path may not be null");
            }
            return null;
        }
        AtomicReference<CoreInstance> ref = this.cache.get(path);
        if (ref == null)
        {
            if (!elementPresentInMetadata(path))
            {
                if (errorIfNotFound)
                {
                    throw new IllegalArgumentException("Element not found: " + path);
                }
                return null;
            }
            ref = this.cache.getIfAbsentPut(path, new AtomicReference<>());
        }

        CoreInstance value = ref.get();
        if (value == null)
        {
            synchronized (ref)
            {
                if ((value = ref.get()) == null)
                {
                    ref.set(value = load(path));
                }
            }
        }
        return value;
    }

    public ReferenceIdResolvers getReferenceIdResolvers()
    {
        return this.referenceIds;
    }

    private boolean elementPresentInMetadata(String path)
    {
        return this.index.hasElement(path) || this.index.hasPackage(path);
    }

    private CoreInstance load(String path)
    {
        long start = System.nanoTime();
        try
        {
            ConcreteElementMetadata elementMetadata = this.index.getElement(path);
            if (elementMetadata != null)
            {
                LOGGER.debug("Loading concrete element {}", path);
                return this.builder.buildConcreteElement(elementMetadata, this.index, this.referenceIds, () -> deserialize(path), () -> deserializeBackReferences(path));
            }

            PackageableElementMetadata packageMetadata = this.index.getPackageMetadata(path);
            if (packageMetadata instanceof VirtualPackageMetadata)
            {
                LOGGER.debug("Loading virtual package {}", path);
                return this.builder.buildVirtualPackage(
                        (VirtualPackageMetadata) packageMetadata,
                        this.index,
                        this.referenceIds,
                        () -> deserializeBackReferences(path));
            }

            // This should not be possible, but just in case ...
            throw new IllegalStateException(path + " is neither a concrete element nor a virtual package");
        }
        catch (Throwable t)
        {
            LOGGER.error("Error loading {}", path, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished loading {} in {}ns", path, end - start);
        }
    }

    private DeserializedConcreteElement deserialize(String path)
    {
        long start = System.nanoTime();
        LOGGER.debug("Deserializing {}", path);
        try
        {
            return deserializeConcreteElement(path);
        }
        catch (Throwable t)
        {
            LOGGER.error("Error deserializing {}", path, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing {} in {}ns", path, end - start);
        }
    }

    private BackReferenceProvider deserializeBackReferences(String path)
    {
        long start = System.nanoTime();
        LOGGER.debug("Deserializing back references for {}", path);
        try
        {
            MutableMap<String, MutableList<BackReference>> map = Maps.mutable.empty();
            this.index.getAllModuleNames().forEach(moduleName ->
            {
                ElementBackReferenceMetadata backRefMetadata = deserializeBackReferences(moduleName, path);
                if (backRefMetadata != null)
                {
                    backRefMetadata.getInstanceBackReferenceMetadata().forEach(ibr ->
                    {
                        String instanceRefId = ibr.getInstanceReferenceId();
                        MutableList<BackReference> list = map.getIfAbsentPut(instanceRefId, Lists.mutable::empty);
                        if (this.backRefFilter == null)
                        {
                            list.addAll(ibr.getBackReferences().castToList());
                        }
                        else
                        {
                            ibr.getBackReferences().select(new BackReferenceFilterPredicate(this.backRefFilter, moduleName, path, instanceRefId), list);
                        }
                    });
                }
            });

            if (map.isEmpty())
            {
                return id -> Lists.fixedSize.empty();
            }

            map.forEachValue(ListHelper::sortAndRemoveDuplicates);
            return id -> map.getIfAbsentValue(id, Lists.fixedSize.empty());
        }
        catch (Throwable t)
        {
            LOGGER.error("Error deserializing back references for {}", path, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing back references for {} in {}ns", path, end - start);
        }
    }

    abstract DeserializedConcreteElement deserializeConcreteElement(String path);

    abstract ElementBackReferenceMetadata deserializeBackReferences(String moduleName, String path);

    private static class ClassLoaderElementLoader extends ElementLoader
    {
        private final FileDeserializer fileDeserializer;
        private final ClassLoader classLoader;

        private ClassLoaderElementLoader(MetadataIndex index, ElementBuilder builder, ReferenceIdResolvers.Builder referenceIdsBuilder, BackReferenceFilter backRefFilter, FileDeserializer fileDeserializer, ClassLoader classLoader)
        {
            super(index, builder, referenceIdsBuilder, backRefFilter);
            this.fileDeserializer = Objects.requireNonNull(fileDeserializer);
            this.classLoader = Objects.requireNonNull(classLoader);
        }

        @Override
        DeserializedConcreteElement deserializeConcreteElement(String path)
        {
            return this.fileDeserializer.deserializeElement(this.classLoader, path);
        }

        @Override
        ElementBackReferenceMetadata deserializeBackReferences(String moduleName, String path)
        {
            return this.fileDeserializer.deserializeModuleElementBackReferenceMetadataIfPresent(this.classLoader, moduleName, path);
        }
    }

    private static class DirectoryElementLoader extends ElementLoader
    {
        private final FileDeserializer fileDeserializer;
        private final Path directory;

        private DirectoryElementLoader(MetadataIndex index, ElementBuilder builder, ReferenceIdResolvers.Builder referenceIdsBuilder, BackReferenceFilter backRefFilter, FileDeserializer fileDeserializer, Path directory)
        {
            super(index, builder, referenceIdsBuilder, backRefFilter);
            this.fileDeserializer = Objects.requireNonNull(fileDeserializer);
            this.directory = Objects.requireNonNull(directory);
        }

        @Override
        DeserializedConcreteElement deserializeConcreteElement(String path)
        {
            return this.fileDeserializer.deserializeElement(this.directory, path);
        }

        @Override
        ElementBackReferenceMetadata deserializeBackReferences(String moduleName, String path)
        {
            return this.fileDeserializer.deserializeModuleElementBackReferenceMetadataIfPresent(this.directory, moduleName, path);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private MetadataIndex index;
        private ElementBuilder builder;
        private final ReferenceIdResolvers.Builder referenceIdsBuilder = ReferenceIdResolvers.builder();
        private BackReferenceFilter backRefFilter;
        private FileDeserializer fileDeserializer;
        private ClassLoader classLoader;
        private Path directory;

        private Builder()
        {
        }

        public Builder withMetadataIndex(MetadataIndex index)
        {
            this.index = index;
            return this;
        }

        public Builder withElementBuilder(ElementBuilder builder)
        {
            this.builder = builder;
            return this;
        }

        public Builder withFileDeserializer(FileDeserializer fileDeserializer)
        {
            this.fileDeserializer = fileDeserializer;
            return this;
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

        public Builder withReferenceIdExtension(ReferenceIdExtension extension)
        {
            this.referenceIdsBuilder.addExtension(extension);
            return this;
        }

        public Builder withReferenceIdExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            this.referenceIdsBuilder.addExtensions(extensions);
            return this;
        }

        public Builder withAvailableReferenceIdExtensions(ClassLoader classLoader)
        {
            this.referenceIdsBuilder.loadExtensions(classLoader);
            return this;
        }

        public Builder withDefaultReferenceIdVersion(Integer version)
        {
            this.referenceIdsBuilder.setDefaultVersion(version);
            return this;
        }

        public Builder withAvailableReferenceIdExtensions()
        {
            this.referenceIdsBuilder.loadExtensions();
            return this;
        }

        public Builder withBackReferenceFilter(BackReferenceFilter backRefFilter)
        {
            this.backRefFilter = backRefFilter;
            return this;
        }

        public ElementLoader build()
        {
            if ((this.classLoader == null) && (this.directory == null))
            {
                throw new IllegalStateException("Either class loader or directory must be provided");
            }
            if ((this.classLoader != null) && (this.directory != null))
            {
                throw new IllegalStateException("Only one of class loader or directory may be provided");
            }
            return (this.classLoader != null) ?
                   new ClassLoaderElementLoader(this.index, this.builder, this.referenceIdsBuilder, this.backRefFilter, this.fileDeserializer, this.classLoader) :
                   new DirectoryElementLoader(this.index, this.builder, this.referenceIdsBuilder, this.backRefFilter, this.fileDeserializer, this.directory);
        }
    }

    public interface BackReferenceFilter
    {
        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.Application application)
        {
            return true;
        }

        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.ModelElement modelElement)
        {
            return true;
        }

        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.PropertyFromAssociation propertyFromAssociation)
        {
            return true;
        }

        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
        {
            return true;
        }

        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.ReferenceUsage referenceUsage)
        {
            return true;
        }

        default boolean test(String module, String elementPath, String instanceReferenceId, BackReference.Specialization specialization)
        {
            return true;
        }
    }

    private static class BackReferenceFilterPredicate implements BackReferenceVisitor<Boolean>, Predicate<BackReference>
    {
        private final BackReferenceFilter filter;
        private final String module;
        private final String elementPath;
        private final String instanceRefId;

        private BackReferenceFilterPredicate(BackReferenceFilter filter, String module, String elementPath, String instanceRefId)
        {
            this.filter = filter;
            this.module = module;
            this.elementPath = elementPath;
            this.instanceRefId = instanceRefId;
        }

        @Override
        public boolean accept(BackReference backRef)
        {
            return backRef.visit(this);
        }

        @Override
        public Boolean visit(BackReference.Application application)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, application);
        }

        @Override
        public Boolean visit(BackReference.ModelElement modelElement)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, modelElement);
        }

        @Override
        public Boolean visit(BackReference.PropertyFromAssociation propertyFromAssociation)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, propertyFromAssociation);
        }

        @Override
        public Boolean visit(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, qualifiedPropertyFromAssociation);
        }

        @Override
        public Boolean visit(BackReference.ReferenceUsage referenceUsage)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, referenceUsage);
        }

        @Override
        public Boolean visit(BackReference.Specialization specialization)
        {
            return this.filter.test(this.module, this.elementPath, this.instanceRefId, specialization);
        }
    }
}
