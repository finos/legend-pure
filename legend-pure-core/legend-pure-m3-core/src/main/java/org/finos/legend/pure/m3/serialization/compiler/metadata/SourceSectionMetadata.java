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

import java.util.Arrays;
import java.util.Objects;

public class SourceSectionMetadata
{
    private final String parser;
    private final ImmutableList<String> elements;

    private SourceSectionMetadata(String parser, ImmutableList<String> elements)
    {
        this.parser = parser;
        this.elements = elements;
    }

    public String getParser()
    {
        return this.parser;
    }

    public ImmutableList<String> getElements()
    {
        return this.elements;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof SourceSectionMetadata))
        {
            return false;
        }

        SourceSectionMetadata that = (SourceSectionMetadata) other;
        return this.parser.equals(that.parser) && this.elements.equals(that.elements);
    }

    @Override
    public int hashCode()
    {
        return this.parser.hashCode() + 33 * this.elements.hashCode();
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder()).toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append('<').append(getClass().getSimpleName())
                .append("parser=").append(this.parser);
        this.elements.appendString(builder, " elements=[", ", ", "]>");
        return builder;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int elementCount)
    {
        return new Builder(elementCount);
    }

    public static class Builder
    {
        private String parser;
        private final MutableList<String> elements;

        private Builder()
        {
            this.elements = Lists.mutable.empty();
        }

        private Builder(int elementCount)
        {
            this.elements = Lists.mutable.ofInitialCapacity(elementCount);
        }

        public Builder withParser(String parser)
        {
            this.parser = parser;
            return this;
        }

        public Builder withElement(String elementPath)
        {
            this.elements.add(Objects.requireNonNull(elementPath, "elementPath may not be null"));
            return this;
        }

        public Builder withElements(Iterable<String> elementPaths)
        {
            elementPaths.forEach(this::withElement);
            return this;
        }

        public Builder withElements(String... elementPaths)
        {
            return withElements(Arrays.asList(elementPaths));
        }

        public SourceSectionMetadata build()
        {
            Objects.requireNonNull(this.parser, "parser is required");
            return new SourceSectionMetadata(this.parser, this.elements.toImmutable());
        }
    }
}
