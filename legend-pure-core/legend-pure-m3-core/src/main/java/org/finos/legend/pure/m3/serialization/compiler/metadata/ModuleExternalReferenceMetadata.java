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

import java.util.Comparator;
import java.util.Objects;

public class ModuleExternalReferenceMetadata
{
    private final String moduleName;
    private final int referenceIdVersion;
    private final ImmutableList<ElementExternalReferenceMetadata> elementExternalReferences;

    private ModuleExternalReferenceMetadata(String moduleName, int referenceIdVersion, ImmutableList<ElementExternalReferenceMetadata> elementExternalReferences)
    {
        this.moduleName = moduleName;
        this.referenceIdVersion = referenceIdVersion;
        this.elementExternalReferences = elementExternalReferences;
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    public int getReferenceIdVersion()
    {
        return this.referenceIdVersion;
    }

    public ImmutableList<ElementExternalReferenceMetadata> getExternalReferences()
    {
        return this.elementExternalReferences;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleExternalReferenceMetadata))
        {
            return false;
        }

        ModuleExternalReferenceMetadata that = (ModuleExternalReferenceMetadata) other;
        return (this.referenceIdVersion == that.referenceIdVersion) &&
                this.moduleName.equals(that.moduleName) &&
                this.elementExternalReferences.equals(that.elementExternalReferences);
    }

    @Override
    public int hashCode()
    {
        return this.moduleName.hashCode();
    }

    public String toString()
    {
        return appendString(new StringBuilder("<").append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("moduleName='").append(this.moduleName)
                .append("' referenceIdVersion=").append(this.referenceIdVersion)
                .append("' extRefs=[");
        if (this.elementExternalReferences.notEmpty())
        {
            this.elementExternalReferences.forEach(xr -> xr.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']');
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int elementExternalReferenceCount)
    {
        return new Builder(elementExternalReferenceCount);
    }

    public static Builder builder(ModuleExternalReferenceMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String moduleName;
        private Integer referenceIdVersion;
        private final MutableList<ElementExternalReferenceMetadata> elementExternalReferences;

        private Builder()
        {
            this.elementExternalReferences = Lists.mutable.empty();
        }

        private Builder(int elementExternalReferenceCount)
        {
            this.elementExternalReferences = Lists.mutable.withInitialCapacity(elementExternalReferenceCount);
        }

        private Builder(ModuleExternalReferenceMetadata metadata)
        {
            this.moduleName = metadata.moduleName;
            this.elementExternalReferences = Lists.mutable.withAll(metadata.elementExternalReferences);
        }

        public void setModuleName(String name)
        {
            this.moduleName = name;
        }

        public void setReferenceIdVersion(Integer version)
        {
            this.referenceIdVersion = version;
        }

        public void addElementExternalReferenceMetadata(ElementExternalReferenceMetadata elementExternalReference)
        {
            this.elementExternalReferences.add(Objects.requireNonNull(elementExternalReference));
        }

        public Builder withModuleName(String name)
        {
            setModuleName(name);
            return this;
        }

        public Builder withReferenceIdVersion(Integer version)
        {
            setReferenceIdVersion(version);
            return this;
        }

        public Builder withElementExternalReferenceMetadata(ElementExternalReferenceMetadata elementExternalReference)
        {
            addElementExternalReferenceMetadata(elementExternalReference);
            return this;
        }

        public ModuleExternalReferenceMetadata build()
        {
            Objects.requireNonNull(this.moduleName, "module name may not be null");
            Objects.requireNonNull(this.referenceIdVersion, "reference id version may not be null");
            return new ModuleExternalReferenceMetadata(this.moduleName, this.referenceIdVersion, buildElementExternalReferences());
        }

        private ImmutableList<ElementExternalReferenceMetadata> buildElementExternalReferences()
        {
            if (this.elementExternalReferences.size() > 1)
            {
                this.elementExternalReferences.sort(Comparator.comparing(ElementExternalReferenceMetadata::getElementPath));
                int index = 0;
                while (index < this.elementExternalReferences.size())
                {
                    int start = index++;
                    ElementExternalReferenceMetadata current = this.elementExternalReferences.get(start);
                    String currentPath = current.getElementPath();
                    while ((index < this.elementExternalReferences.size()) && currentPath.equals(this.elementExternalReferences.get(index).getElementPath()))
                    {
                        index++;
                    }
                    if (index > start + 1)
                    {
                        // Multiple ElementExternalReferenceMetadata objects for the same element: merge them
                        ElementExternalReferenceMetadata.Builder builder = ElementExternalReferenceMetadata.builder(current);
                        for (int i = start + 1; i < index; i++)
                        {
                            // merge and set to null to mark for removal
                            builder.addMetadata(this.elementExternalReferences.set(i, null));
                        }
                        this.elementExternalReferences.set(start, builder.build());
                    }
                }
                this.elementExternalReferences.removeIf(Objects::isNull);
            }
            return this.elementExternalReferences.toImmutable();
        }
    }
}
