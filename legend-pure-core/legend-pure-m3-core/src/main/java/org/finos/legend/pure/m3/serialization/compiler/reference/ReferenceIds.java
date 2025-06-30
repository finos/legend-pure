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

package org.finos.legend.pure.m3.serialization.compiler.reference;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public abstract class ReferenceIds
{
    final ExtensionManager extensionManager;

    ReferenceIds(ExtensionManager extensionManager)
    {
        this.extensionManager = extensionManager;
    }

    public int getDefaultVersion()
    {
        return this.extensionManager.getDefaultVersion();
    }

    public boolean isVersionAvailable(int version)
    {
        return this.extensionManager.isVersionAvailable(version);
    }

    public ReferenceIdExtension getExtension(int version)
    {
        return this.extensionManager.getExtensionCache(version).extension;
    }

    public ReferenceIdExtension getExtension(Integer version)
    {
        return this.extensionManager.getExtensionCache(version).extension;
    }

    public ReferenceIdExtension getDefaultExtension()
    {
        return this.extensionManager.getDefaultExtensionCache().extension;
    }

    static class ExtensionCache
    {
        private final ReferenceIdExtension extension;
        private ReferenceIdProvider provider;
        private ReferenceIdResolver resolver;

        private ExtensionCache(ReferenceIdExtension extension)
        {
            this.extension = extension;
        }

        synchronized ReferenceIdProvider provider(ProcessorSupport processorSupport)
        {
            if (this.provider == null)
            {
                this.provider = this.extension.newProvider(Objects.requireNonNull(processorSupport, "processor support is required for a provider"));
            }
            return this.provider;
        }

        synchronized ReferenceIdResolver resolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            if (this.resolver == null)
            {
                this.resolver = this.extension.newResolver(Objects.requireNonNull(packagePathResolver, "package path resolver is required for a resolver"));
            }
            return this.resolver;
        }
    }

    abstract static class ExtensionManager
    {
        final int defaultVersion;

        ExtensionManager(int defaultVersion)
        {
            this.defaultVersion = defaultVersion;
        }

        int getDefaultVersion()
        {
            return this.defaultVersion;
        }

        abstract boolean isVersionAvailable(int version);

        ExtensionCache getDefaultExtensionCache()
        {
            return getExtensionCache(this.defaultVersion);
        }

        ExtensionCache getExtensionCache(Integer version)
        {
            return (version == null) ? getDefaultExtensionCache() : getExtensionCache(version.intValue());
        }

        abstract ExtensionCache getExtensionCache(int version);
    }

    private static class SingleExtensionManager extends ExtensionManager
    {
        private final ExtensionCache extension;

        private SingleExtensionManager(ReferenceIdExtension extension)
        {
            super(extension.version());
            this.extension = new ExtensionCache(extension);
        }

        @Override
        boolean isVersionAvailable(int version)
        {
            return version == this.defaultVersion;
        }

        @Override
        ExtensionCache getDefaultExtensionCache()
        {
            return this.extension;
        }

        @Override
        ExtensionCache getExtensionCache(int version)
        {
            if (!isVersionAvailable(version))
            {
                throw new IllegalArgumentException("Unknown extension: " + version);
            }
            return this.extension;
        }
    }

    private static class SequenceExtensionManager extends ExtensionManager
    {
        private final ExtensionCache[] extensions;
        private final int offset;

        private SequenceExtensionManager(int defaultVersion, ReferenceIdExtension[] extensions)
        {
            super(defaultVersion);
            this.extensions = new ExtensionCache[extensions.length];
            for (int i = 0; i < extensions.length; i++)
            {
                this.extensions[i] = new ExtensionCache(extensions[i]);
            }
            this.offset = extensions[0].version();
        }

        @Override
        boolean isVersionAvailable(int version)
        {
            int index = getIndex(version);
            return (0 <= index) && (index < this.extensions.length);
        }

        @Override
        ExtensionCache getExtensionCache(int version)
        {
            try
            {
                return this.extensions[getIndex(version)];
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new IllegalArgumentException("Unknown extension: " + version);
            }
        }

        private int getIndex(int version)
        {
            return version - this.offset;
        }
    }

    private static class GeneralExtensionManager extends ExtensionManager
    {
        private final MutableIntObjectMap<ExtensionCache> extensions;

        private GeneralExtensionManager(int defaultVersion, IntObjectMap<ReferenceIdExtension> extensions)
        {
            super(defaultVersion);
            this.extensions = IntObjectMaps.mutable.ofInitialCapacity(extensions.size());
            extensions.forEachKeyValue((version, extension) -> this.extensions.put(version, new ExtensionCache(extension)));
        }

        public boolean isVersionAvailable(int version)
        {
            return this.extensions.containsKey(version);
        }

        @Override
        ExtensionCache getExtensionCache(int version)
        {
            ExtensionCache extension = this.extensions.get(version);
            if (extension == null)
            {
                throw new IllegalArgumentException("Unknown extension: " + version);
            }
            return extension;
        }
    }

    public abstract static class AbstractBuilder<T extends ReferenceIds>
    {
        private final MutableIntObjectMap<ReferenceIdExtension> extensions = IntObjectMaps.mutable.empty();
        private Integer defaultVersion;

        protected AbstractBuilder()
        {
        }

        public void addExtension(ReferenceIdExtension extension)
        {
            Objects.requireNonNull(extension, "extension may not be null");
            if (this.extensions.getIfAbsentPut(extension.version(), extension) != extension)
            {
                throw new IllegalArgumentException("There is already an extension for version " + extension.version());
            }
        }

        public void addExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            extensions.forEach(this::addExtension);
        }

        public void loadExtensions(ClassLoader classLoader)
        {
            addExtensions(ServiceLoader.load(ReferenceIdExtension.class, classLoader));
        }

        public void loadExtensions()
        {
            addExtensions(ServiceLoader.load(ReferenceIdExtension.class));
        }

        public void setDefaultVersion(Integer defaultVersion)
        {
            this.defaultVersion = defaultVersion;
        }

        public void clearDefaultVersion()
        {
            setDefaultVersion(null);
        }

        public T build()
        {
            if (this.extensions.isEmpty())
            {
                throw new IllegalStateException("At least one extension is required");
            }
            int resolvedDefaultVersion;
            if (this.defaultVersion == null)
            {
                resolvedDefaultVersion = this.extensions.keySet().max();
            }
            else if (this.extensions.containsKey(this.defaultVersion))
            {
                resolvedDefaultVersion = this.defaultVersion;
            }
            else
            {
                throw new IllegalArgumentException("Default version " + this.defaultVersion + " is unknown");
            }

            if (this.extensions.size() == 1)
            {
                return build(new SingleExtensionManager(this.extensions.getAny()));
            }

            int minId;
            if (this.extensions.size() == (this.extensions.keySet().max() - (minId = this.extensions.keySet().min())))
            {
                ReferenceIdExtension[] extArray = new ReferenceIdExtension[this.extensions.size()];
                this.extensions.forEachKeyValue((id, ext) -> extArray[id - minId] = ext);
                return build(new SequenceExtensionManager(resolvedDefaultVersion, extArray));
            }

            return build(new GeneralExtensionManager(resolvedDefaultVersion, this.extensions));
        }

        abstract T build(ExtensionManager extensionManager);

        // To remove

        public AbstractBuilder<T> withExtension(ReferenceIdExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public AbstractBuilder<T> withAvailableExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public AbstractBuilder<T> withAvailableExtensions()
        {
            loadExtensions();
            return this;
        }

        public void setPackagePathResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            throw new UnsupportedOperationException();
        }

        public AbstractBuilder<T> withPackagePathResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            throw new UnsupportedOperationException();
        }
    }
}
