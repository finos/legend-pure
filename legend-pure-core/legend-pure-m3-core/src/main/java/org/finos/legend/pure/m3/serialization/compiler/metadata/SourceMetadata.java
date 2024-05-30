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

public class SourceMetadata
{
    private final String sourceId;
    private final ImmutableList<SourceSectionMetadata> sections;

    private SourceMetadata(String sourceId, ImmutableList<SourceSectionMetadata> sections)
    {
        this.sourceId = sourceId;
        this.sections = sections;
    }

    public String getSourceId()
    {
        return this.sourceId;
    }

    public ImmutableList<SourceSectionMetadata> getSections()
    {
        return this.sections;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof SourceMetadata))
        {
            return false;
        }

        SourceMetadata that = (SourceMetadata) other;
        return this.sourceId.equals(that.sourceId) && this.sections.equals(that.sections);
    }

    @Override
    public int hashCode()
    {
        return this.sourceId.hashCode();
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder().append('<').append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("sourceId='").append(this.sourceId).append("' sections=[");
        if (this.sections.notEmpty())
        {
            this.sections.forEach(s -> s.appendString(builder).append(", "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']');
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int sectionCount)
    {
        return new Builder(sectionCount);
    }

    public static class Builder
    {
        private String sourceId;
        private final MutableList<SourceSectionMetadata> sections;

        private Builder()
        {
            this.sections = Lists.mutable.empty();
        }

        private Builder(int sectionCount)
        {
            this.sections = Lists.mutable.ofInitialCapacity(sectionCount);
        }

        public Builder withSourceId(String sourceId)
        {
            this.sourceId = sourceId;
            return this;
        }

        public Builder withSection(SourceSectionMetadata section)
        {
            this.sections.add(Objects.requireNonNull(section, "section may not be null"));
            return this;
        }

        public Builder withSection(String parser, Iterable<String> elements)
        {
            return withSection(SourceSectionMetadata.builder().withParser(parser).withElements(elements).build());
        }

        public Builder withSection(String parser, String... elements)
        {
            return withSection(SourceSectionMetadata.builder().withParser(parser).withElements(elements).build());
        }

        public Builder withSections(Iterable<? extends SourceSectionMetadata> sections)
        {
            sections.forEach(this::withSection);
            return this;
        }

        public Builder withSections(SourceSectionMetadata... sections)
        {
            return withSections(Arrays.asList(sections));
        }

        public SourceMetadata build()
        {
            Objects.requireNonNull(this.sourceId, "sourceId is required");
            return new SourceMetadata(this.sourceId, this.sections.toImmutable());
        }
    }
}
