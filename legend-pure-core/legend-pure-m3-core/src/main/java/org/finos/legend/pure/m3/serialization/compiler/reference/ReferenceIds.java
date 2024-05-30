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

import java.util.Objects;
import java.util.ServiceLoader;

public abstract class ReferenceIds
{
    final ProcessorSupport processorSupport;
    final int defaultVersion;

    private ReferenceIds(ProcessorSupport processorSupport, int defaultVersion)
    {
        this.processorSupport = processorSupport;
        this.defaultVersion = defaultVersion;
    }

    public int getDefaultVersion()
    {
        return this.defaultVersion;
    }

    public abstract boolean isVersionAvailable(int version);

    public ReferenceIdExtension getExtension(int version)
    {
        return getExtensionCache(version).extension;
    }

    public ReferenceIdExtension getExtension(Integer version)
    {
        return getExtensionCache(version).extension;
    }

    public ReferenceIdExtension getDefaultExtension()
    {
        return getDefaultExtensionCache().extension;
    }

    public ReferenceIdProvider provider(int version)
    {
        return getExtensionCache(version).provider();
    }

    public ReferenceIdProvider provider(Integer version)
    {
        return getExtensionCache(version).provider();
    }

    public ReferenceIdProvider provider()
    {
        return getDefaultExtensionCache().provider();
    }

    public ReferenceIdResolver resolver(int version)
    {
        return getExtensionCache(version).resolver();
    }

    public ReferenceIdResolver resolver(Integer version)
    {
        return getExtensionCache(version).resolver();
    }

    public ReferenceIdResolver resolver()
    {
        return getDefaultExtensionCache().resolver();
    }

    private ExtensionCache getExtensionCache(Integer version)
    {
        return (version == null) ? getDefaultExtensionCache() : getExtensionCache(version.intValue());
    }

    ExtensionCache getDefaultExtensionCache()
    {
        return getExtensionCache(this.defaultVersion);
    }

    abstract ExtensionCache getExtensionCache(int version);

    class ExtensionCache
    {
        private final ReferenceIdExtension extension;
        private ReferenceIdProvider provider;
        private ReferenceIdResolver resolver;

        private ExtensionCache(ReferenceIdExtension extension)
        {
            this.extension = extension;
        }

        synchronized ReferenceIdProvider provider()
        {
            if (this.provider == null)
            {
                this.provider = this.extension.newProvider(ReferenceIds.this.processorSupport);
            }
            return this.provider;
        }

        synchronized ReferenceIdResolver resolver()
        {
            if (this.resolver == null)
            {
                this.resolver = this.extension.newResolver(ReferenceIds.this.processorSupport);
            }
            return this.resolver;
        }
    }

    private static class Single extends ReferenceIds
    {
        private final ExtensionCache extension;

        private Single(ProcessorSupport processorSupport, ReferenceIdExtension extension)
        {
            super(processorSupport, extension.version());
            this.extension = new ExtensionCache(extension);
        }

        public boolean isVersionAvailable(int version)
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

    private static class Sequence extends ReferenceIds
    {
        private final ExtensionCache[] extensions;
        private final int offset;

        private Sequence(ProcessorSupport processorSupport, int defaultVersion, ReferenceIdExtension[] extensions)
        {
            super(processorSupport, defaultVersion);
            this.extensions = new ExtensionCache[extensions.length];
            for (int i = 0; i < extensions.length; i++)
            {
                this.extensions[i] = new ExtensionCache(extensions[i]);
            }
            this.offset = extensions[0].version();
        }

        @Override
        public boolean isVersionAvailable(int version)
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

    private static class General extends ReferenceIds
    {
        private final MutableIntObjectMap<ExtensionCache> extensions;

        private General(ProcessorSupport processorSupport, int defaultVersion, IntObjectMap<ReferenceIdExtension> extensions)
        {
            super(processorSupport, defaultVersion);
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

    public static Builder builder(ProcessorSupport processorSupport)
    {
        return new Builder(processorSupport);
    }

    public static class Builder
    {
        private final ProcessorSupport processorSupport;
        private final MutableIntObjectMap<ReferenceIdExtension> extensions = IntObjectMaps.mutable.empty();
        private Integer defaultVersion;

        private Builder(ProcessorSupport processorSupport)
        {
            this.processorSupport = Objects.requireNonNull(processorSupport, "processor support is required");
        }

        public void addExtension(ReferenceIdExtension extension)
        {
            Objects.requireNonNull(extension, "extension may not be null");
            if (this.extensions.getIfAbsentPut(extension.version(), extension) != extension)
            {
                throw new IllegalArgumentException("There is already an extension for version " + extension.version());
            }
        }

        public Builder withExtension(ReferenceIdExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public void addExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            extensions.forEach(this::addExtension);
        }

        public Builder withExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public void loadExtensions(ClassLoader classLoader)
        {
            addExtensions(ServiceLoader.load(ReferenceIdExtension.class, classLoader));
        }

        public void loadExtensions()
        {
            addExtensions(ServiceLoader.load(ReferenceIdExtension.class));
        }

        public Builder withAvailableExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withAvailableExtensions()
        {
            loadExtensions();
            return this;
        }

        public void setDefaultVersion(int defaultVersion)
        {
            this.defaultVersion = defaultVersion;
        }

        public void clearDefaultVersion()
        {
            this.defaultVersion = null;
        }

        public Builder withDefaultVersion(Integer defaultVersion)
        {
            if (defaultVersion == null)
            {
                clearDefaultVersion();
            }
            else
            {
                setDefaultVersion(defaultVersion);
            }
            return this;
        }

        public ReferenceIds build()
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
                return new Single(this.processorSupport, this.extensions.getAny());
            }

            int minId;
            if (this.extensions.size() == (this.extensions.keySet().max() - (minId = this.extensions.keySet().min())))
            {
                ReferenceIdExtension[] extArray = new ReferenceIdExtension[this.extensions.size()];
                this.extensions.forEachKeyValue((id, ext) -> extArray[id - minId] = ext);
                return new Sequence(this.processorSupport, resolvedDefaultVersion, extArray);
            }

            return new General(this.processorSupport, resolvedDefaultVersion, this.extensions);
        }
    }
}
