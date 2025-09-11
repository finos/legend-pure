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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tools.ListHelper;

import java.util.Arrays;
import java.util.Objects;

public class ElementExternalReferenceMetadata
{
    private final String path;
    private final ImmutableList<String> externalReferences;

    private ElementExternalReferenceMetadata(String path, ImmutableList<String> externalReferences)
    {
        this.path = path;
        this.externalReferences = externalReferences;
    }

    public String getElementPath()
    {
        return this.path;
    }

    public ImmutableList<String> getExternalReferences()
    {
        return this.externalReferences;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ElementExternalReferenceMetadata))
        {
            return false;
        }

        ElementExternalReferenceMetadata that = (ElementExternalReferenceMetadata) other;
        return this.path.equals(that.path) && this.externalReferences.equals(that.externalReferences);
    }

    @Override
    public int hashCode()
    {
        return this.path.hashCode();
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder("<").append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("path='").append(this.path).append("' refs=[");
        if (this.externalReferences.notEmpty())
        {
            this.externalReferences.forEach(ref -> builder.append('\'').append(ref).append("', "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']');
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int externalReferenceCount)
    {
        return new Builder(externalReferenceCount);
    }

    public static Builder builder(ElementExternalReferenceMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String path;
        private final MutableList<String> externalReferences;

        private Builder()
        {
            this.externalReferences = Lists.mutable.empty();
        }

        private Builder(int extRefCount)
        {
            this.externalReferences = Lists.mutable.withInitialCapacity(extRefCount);
        }

        private Builder(ElementExternalReferenceMetadata metadata)
        {
            this.path = metadata.path;
            this.externalReferences = Lists.mutable.withAll(metadata.externalReferences);
        }

        public void setElementPath(String path)
        {
            this.path = path;
        }

        public void addExternalReference(String reference)
        {
            this.externalReferences.add(Objects.requireNonNull(reference));
        }

        public void addExternalReferences(Iterable<? extends String> references)
        {
            references.forEach(this::addExternalReference);
        }

        public void addExternalReferences(String... references)
        {
            addExternalReferences(Arrays.asList(references));
        }

        public void addMetadata(ElementExternalReferenceMetadata metadata)
        {
            Objects.requireNonNull(metadata);
            if (this.path == null)
            {
                this.path = metadata.path;
            }
            else if (!this.path.equals(metadata.path))
            {
                throw new IllegalStateException("Cannot add metadata for element '" + metadata.path + "' to builder for element '" + this.path + "'");
            }
            this.externalReferences.addAll(metadata.externalReferences.castToList());
        }

        public Builder withElementPath(String path)
        {
            setElementPath(path);
            return this;
        }

        public Builder withExternalReference(String reference)
        {
            addExternalReference(reference);
            return this;
        }

        public Builder withExternalReferences(Iterable<? extends String> references)
        {
            addExternalReferences(references);
            return this;
        }

        public Builder withExternalReferences(String... references)
        {
            addExternalReferences(references);
            return this;
        }

        public ElementExternalReferenceMetadata build()
        {
            return new ElementExternalReferenceMetadata(Objects.requireNonNull(this.path, "path may not be null"), ListHelper.sortAndRemoveDuplicates(this.externalReferences).toImmutable());
        }
    }
}
