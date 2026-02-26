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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.util.Objects;

public class ModuleBackReferenceMetadata
{
    private final String moduleName;
    private final int referenceIdVersion;
    private final ImmutableList<ElementBackReferenceMetadata> elementBackReferences;

    private ModuleBackReferenceMetadata(String moduleName, int referenceIdVersion, ImmutableList<ElementBackReferenceMetadata> elementBackReferences)
    {
        this.moduleName = moduleName;
        this.referenceIdVersion = referenceIdVersion;
        this.elementBackReferences = elementBackReferences;
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    public int getReferenceIdVersion()
    {
        return this.referenceIdVersion;
    }

    public ImmutableList<ElementBackReferenceMetadata> getBackReferences()
    {
        return this.elementBackReferences;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleBackReferenceMetadata))
        {
            return false;
        }

        ModuleBackReferenceMetadata that = (ModuleBackReferenceMetadata) other;
        return (this.referenceIdVersion == that.referenceIdVersion) &&
                this.moduleName.equals(that.moduleName) &&
                this.elementBackReferences.equals(that.elementBackReferences);
    }

    @Override
    public int hashCode()
    {
        return this.moduleName.hashCode();
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder("<").append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("moduleName='").append(this.moduleName)
                .append("' referenceIdVersion=").append(this.referenceIdVersion)
                .append("' backRefs=[");
        if (this.elementBackReferences.notEmpty())
        {
            this.elementBackReferences.forEach(xr -> xr.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']');
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(ModuleBackReferenceMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String moduleName;
        private Integer referenceIdVersion;
        private final MutableMap<String, ElementBackReferenceMetadata.Builder> elementBackReferences;

        private Builder()
        {
            this.elementBackReferences = Maps.mutable.empty();
        }

        private Builder(ModuleBackReferenceMetadata metadata)
        {
            this.moduleName = metadata.moduleName;
            this.referenceIdVersion = metadata.referenceIdVersion;
            this.elementBackReferences = Maps.mutable.ofInitialCapacity(metadata.elementBackReferences.size());
            metadata.elementBackReferences.forEach(er -> this.elementBackReferences.put(er.getElementPath(), ElementBackReferenceMetadata.builder(er)));
        }

        public void setModuleName(String name)
        {
            this.moduleName = name;
        }

        public void setReferenceIdVersion(Integer version)
        {
            this.referenceIdVersion = version;
        }

        public void addBackReferences(String elementPath, String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            Objects.requireNonNull(elementPath, "element path may not be null");
            Objects.requireNonNull(instanceReferenceId, "instance reference id may not be null");
            Objects.requireNonNull(backReferences, "back references may not be null");
            this.elementBackReferences.getIfAbsentPutWithKey(elementPath, ElementBackReferenceMetadata::builder).addInstanceBackReferenceMetadata(instanceReferenceId, backReferences);
        }

        public void addBackReferences(String elementPath, String instanceReferenceId, BackReference... backReferences)
        {
            Objects.requireNonNull(elementPath, "element path may not be null");
            Objects.requireNonNull(instanceReferenceId, "instance reference id may not be null");
            Objects.requireNonNull(backReferences, "back references may not be null");
            this.elementBackReferences.getIfAbsentPutWithKey(elementPath, ElementBackReferenceMetadata::builder).addInstanceBackReferenceMetadata(instanceReferenceId, backReferences);
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

        public Builder withBackReferences(String elementPath, String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            addBackReferences(elementPath, instanceReferenceId, backReferences);
            return this;
        }

        public Builder withBackReferences(String elementPath, String instanceReferenceId, BackReference... backReferences)
        {
            addBackReferences(elementPath, instanceReferenceId, backReferences);
            return this;
        }

        public ModuleBackReferenceMetadata build()
        {
            Objects.requireNonNull(this.moduleName, "module name may not be null");
            Objects.requireNonNull(this.referenceIdVersion, "reference id version may not be null");
            return new ModuleBackReferenceMetadata(this.moduleName, this.referenceIdVersion, buildElementBackReferences());
        }

        private ImmutableList<ElementBackReferenceMetadata> buildElementBackReferences()
        {
            return this.elementBackReferences.valuesView()
                    .collect(b -> b.withReferenceIdVersion(this.referenceIdVersion).build(), Lists.mutable.ofInitialCapacity(this.elementBackReferences.size()))
                    .sortThisBy(ElementBackReferenceMetadata::getElementPath)
                    .toImmutable();
        }
    }
}
