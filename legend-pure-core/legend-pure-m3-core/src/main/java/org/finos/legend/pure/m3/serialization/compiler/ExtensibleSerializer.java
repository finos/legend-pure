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

package org.finos.legend.pure.m3.serialization.compiler;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract class ExtensibleSerializer<T extends SerializerExtension>
{
    private final IntObjectMap<T> extensions;
    private final int defaultVersion;

    protected ExtensibleSerializer(Iterable<? extends T> extensions, int defaultVersion)
    {
        this.extensions = indexExtensions(extensions);
        this.defaultVersion = defaultVersion;
        if (!this.extensions.containsKey(this.defaultVersion))
        {
            throw new IllegalArgumentException("Default version " + this.defaultVersion + " is unknown");
        }
    }

    public int getDefaultVersion()
    {
        return this.defaultVersion;
    }

    public boolean isVersionAvailable(int version)
    {
        return this.extensions.containsKey(version);
    }

    public T getExtension(int version)
    {
        T extension = this.extensions.get(version);
        if (extension == null)
        {
            throw new IllegalArgumentException("Unknown extension: " + version);
        }
        return extension;
    }

    public T getDefaultExtension()
    {
        return getExtension(getDefaultVersion());
    }

    public Iterable<T> getExtensions()
    {
        return this.extensions.asLazy();
    }

    public void forEachVersion(IntConsumer consumer)
    {
        this.extensions.forEachKey(consumer::accept);
    }

    public void forEachExtension(Consumer<T> consumer)
    {
        this.extensions.forEachValue(consumer::accept);
    }

    private static <T extends SerializerExtension> IntObjectMap<T> indexExtensions(Iterable<? extends T> extensions)
    {
        MutableIntObjectMap<T> index = (extensions instanceof Collection<?>) ? IntObjectMaps.mutable.ofInitialCapacity(((Collection<?>) extensions).size()) : IntObjectMaps.mutable.empty();
        extensions.forEach(ext ->
        {
            Objects.requireNonNull(ext, "extension may not be null");
            if (index.getIfAbsentPut(ext.version(), ext) != ext)
            {
                throw new IllegalArgumentException("Multiple extensions for version " + ext.version());
            }
        });
        return index;
    }

    public abstract static class AbstractBuilder<E extends SerializerExtension, S extends ExtensibleSerializer<E>>
    {
        private final MutableIntObjectMap<E> extensions = IntObjectMaps.mutable.empty();
        private Integer defaultVersion;

        public void addExtension(E extension)
        {
            Objects.requireNonNull(extension, "extension may not be null");
            if (this.extensions.getIfAbsentPut(extension.version(), extension) != extension)
            {
                throw new IllegalArgumentException("There is already an extension for version " + extension.version());
            }
        }

        public void addExtensions(Iterable<? extends E> extensions)
        {
            extensions.forEach(this::addExtension);
        }

        public void loadExtensions(ClassLoader classLoader)
        {
            addExtensions(ServiceLoader.load(getExtensionClass(), classLoader));
        }

        public void loadExtensions()
        {
            addExtensions(ServiceLoader.load(getExtensionClass()));
        }

        public void setDefaultVersion(int defaultVersion)
        {
            this.defaultVersion = defaultVersion;
        }

        public void clearDefaultVersion()
        {
            this.defaultVersion = null;
        }

        public final S build()
        {
            if (this.extensions.isEmpty())
            {
                throw new IllegalStateException("At least one extension is required");
            }
            return build(Collections.unmodifiableCollection(this.extensions.values()), (this.defaultVersion == null) ? this.extensions.keySet().max() : this.defaultVersion);
        }

        protected abstract S build(Iterable<E> extensions, int defaultVersion);

        protected abstract Class<E> getExtensionClass();
    }
}
