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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Objects;

/**
 * @deprecated This is a compatibility shim over {@link MetadataPelt}, which it delegates to
 * entirely. Retained temporarily so that existing consumers keep working; use
 * {@link MetadataPelt} directly instead.
 */
@Deprecated
public abstract class MetadataLazy implements Metadata
{
    public static MetadataLazy fromClassLoader(ClassLoader classLoader, String metadataName)
    {
        Objects.requireNonNull(classLoader, "class loader may not be null");
        return fromClassLoader(classLoader, Lists.fixedSize.with(metadataName));
    }

    public static MetadataLazy fromClassLoader(ClassLoader classLoader, String metadataName, String... moreMetadataNames)
    {
        Objects.requireNonNull(classLoader, "class loader may not be null");
        return fromClassLoader(classLoader, Sets.mutable.with(moreMetadataNames).with(metadataName));
    }

    public static MetadataLazy fromClassLoader(ClassLoader classLoader, Iterable<String> metadataNames)
    {
        Objects.requireNonNull(classLoader, "class loader may not be null");
        Objects.requireNonNull(metadataNames, "metadataNames may not be null");
        MetadataPelt metadataPelt = MetadataPelt.fromClassLoader(classLoader, metadataNames);
        return new PeltMetadataLazy(metadataPelt);
    }

    private static class PeltMetadataLazy extends MetadataLazy
    {
        private final MetadataPelt metadataPelt;

        private PeltMetadataLazy(MetadataPelt metadataPelt)
        {
            this.metadataPelt = metadataPelt;
        }

        @Override
        public void startTransaction()
        {
            this.metadataPelt.startTransaction();
        }

        @Override
        public void commitTransaction()
        {
            this.metadataPelt.commitTransaction();
        }

        @Override
        public void rollbackTransaction()
        {
            this.metadataPelt.rollbackTransaction();
        }

        @Override
        public CoreInstance getMetadata(String classifier, String id)
        {
            return this.metadataPelt.getMetadata(classifier, id);
        }

        @Override
        public MapIterable<String, CoreInstance> getMetadata(String classifier)
        {
            return this.metadataPelt.getMetadata(classifier);
        }

        @Override
        public RichIterable<CoreInstance> getClassifierInstances(String classifier)
        {
            return this.metadataPelt.getClassifierInstances(classifier);
        }

        @Override
        public CoreInstance getEnum(String enumerationName, String enumName)
        {
            return this.metadataPelt.getEnum(enumerationName, enumName);
        }

        @Override
        public boolean supportsElementByPath()
        {
            return this.metadataPelt.supportsElementByPath();
        }

        @Override
        public boolean hasElement(String path)
        {
            return this.metadataPelt.hasElement(path);
        }

        @Override
        public CoreInstance getElementByPath(String path)
        {
            return this.metadataPelt.getElementByPath(path);
        }
    }
}
