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

import java.util.Comparator;
import java.util.Objects;

/**
 * Back reference metadata for an element. This includes back references for the element itself and its component instances.
 */
public class ElementBackReferenceMetadata
{
    private final String path;
    private final int referenceIdVersion;
    private final ImmutableList<InstanceBackReferenceMetadata> instanceBackReferences;

    private ElementBackReferenceMetadata(String path, int referenceIdVersion, ImmutableList<InstanceBackReferenceMetadata> instanceBackReferences)
    {
        this.path = path;
        this.referenceIdVersion = referenceIdVersion;
        this.instanceBackReferences = instanceBackReferences;
    }

    public String getElementPath()
    {
        return this.path;
    }

    public int getReferenceIdVersion()
    {
        return this.referenceIdVersion;
    }

    public ImmutableList<InstanceBackReferenceMetadata> getInstanceBackReferenceMetadata()
    {
        return this.instanceBackReferences;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ElementBackReferenceMetadata))
        {
            return false;
        }

        ElementBackReferenceMetadata that = (ElementBackReferenceMetadata) other;
        return (this.referenceIdVersion == that.referenceIdVersion) &&
                this.path.equals(that.path) &&
                this.instanceBackReferences.equals(that.instanceBackReferences);
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
        builder.append("path='").append(this.path)
                .append("' referenceIdVersion=").append(this.referenceIdVersion)
                .append("' instanceBackRefs=[");
        if (this.instanceBackReferences.notEmpty())
        {
            this.instanceBackReferences.forEach(ibr -> ibr.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']');
    }

    /**
     * Back references for an instance. These back references indicate external references to the instance from other
     * instances.
     */
    public static class InstanceBackReferenceMetadata
    {
        private final String instanceRefId;
        private final ImmutableList<BackReference> backReferences;

        private InstanceBackReferenceMetadata(String instanceRefId, ImmutableList<BackReference> backReferences)
        {
            this.instanceRefId = instanceRefId;
            this.backReferences = backReferences;
        }

        public String getInstanceReferenceId()
        {
            return this.instanceRefId;
        }

        public ImmutableList<BackReference> getBackReferences()
        {
            return this.backReferences;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof InstanceBackReferenceMetadata))
            {
                return false;
            }

            InstanceBackReferenceMetadata that = (InstanceBackReferenceMetadata) other;
            return this.instanceRefId.equals(that.instanceRefId) && this.backReferences.equals(that.backReferences);
        }

        @Override
        public int hashCode()
        {
            return this.instanceRefId.hashCode();
        }

        @Override
        public String toString()
        {
            return appendString(new StringBuilder("<").append(getClass().getSimpleName()).append(' ')).append('>').toString();
        }

        StringBuilder appendString(StringBuilder builder)
        {
            builder.append("instanceRefId='").append(this.instanceRefId).append("' backRefs=[");
            if (this.backReferences.notEmpty())
            {
                this.backReferences.forEach(br -> br.appendString(builder).append(", "));
                builder.setLength(builder.length() - 2);
            }
            return builder.append(']');
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(String elementPath)
    {
        return builder().withElementPath(elementPath);
    }

    public static Builder builder(int instanceBackRefCount)
    {
        return new Builder(instanceBackRefCount);
    }

    public static Builder builder(ElementBackReferenceMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String path;
        private Integer referenceIdVersion;
        private final MutableList<InstanceBackReferenceMetadata> instanceBackReferences;

        private Builder()
        {
            this.instanceBackReferences = Lists.mutable.empty();
        }

        private Builder(int instanceBackRefCount)
        {
            this.instanceBackReferences = Lists.mutable.withInitialCapacity(instanceBackRefCount);
        }

        private Builder(ElementBackReferenceMetadata metadata)
        {
            this.path = metadata.path;
            this.instanceBackReferences = Lists.mutable.withAll(metadata.instanceBackReferences);
        }

        public void setElementPath(String path)
        {
            this.path = path;
        }

        public void setReferenceIdVersion(Integer version)
        {
            this.referenceIdVersion = version;
        }

        public void addInstanceBackReferenceMetadata(String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            addInstanceBackReferenceMetadata(instanceReferenceId, Lists.mutable.withAll(backReferences));
        }

        public void addInstanceBackReferenceMetadata(String instanceReferenceId, BackReference... backReferences)
        {
            addInstanceBackReferenceMetadata(instanceReferenceId, Lists.mutable.with(backReferences));
        }

        private void addInstanceBackReferenceMetadata(String instanceReferenceId, MutableList<BackReference> backReferences)
        {
            if (backReferences.notEmpty())
            {
                Objects.requireNonNull(instanceReferenceId, "instance reference id may not be null");
                if (backReferences.anySatisfy(Objects::isNull))
                {
                    throw new NullPointerException("back references may not be null");
                }
                this.instanceBackReferences.add(new InstanceBackReferenceMetadata(instanceReferenceId, ListHelper.sortAndRemoveDuplicates(backReferences).toImmutable()));
            }
        }

        public void addMetadata(ElementBackReferenceMetadata metadata)
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
            this.instanceBackReferences.addAll(metadata.instanceBackReferences.castToList());
        }

        public Builder withElementPath(String path)
        {
            setElementPath(path);
            return this;
        }

        public Builder withReferenceIdVersion(Integer version)
        {
            setReferenceIdVersion(version);
            return this;
        }

        public Builder withInstanceBackReferenceMetadata(String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            addInstanceBackReferenceMetadata(instanceReferenceId, backReferences);
            return this;
        }

        public Builder withInstanceBackReferenceMetadata(String instanceReferenceId, BackReference... backReferences)
        {
            addInstanceBackReferenceMetadata(instanceReferenceId, backReferences);
            return this;
        }

        public ElementBackReferenceMetadata build()
        {
            Objects.requireNonNull(this.path, "path must be specified");
            Objects.requireNonNull(this.referenceIdVersion, "reference id version must be specified");
            return new ElementBackReferenceMetadata(this.path, this.referenceIdVersion, buildInstanceBackRefs());
        }

        private ImmutableList<InstanceBackReferenceMetadata> buildInstanceBackRefs()
        {
            if (this.instanceBackReferences.size() > 1)
            {
                this.instanceBackReferences.sort(Comparator.comparing(InstanceBackReferenceMetadata::getInstanceReferenceId));
                int index = 0;
                while (index < this.instanceBackReferences.size())
                {
                    int start = index++;
                    InstanceBackReferenceMetadata current = this.instanceBackReferences.get(start);
                    String currentId = current.getInstanceReferenceId();
                    while ((index < this.instanceBackReferences.size()) && currentId.equals(this.instanceBackReferences.get(index).getInstanceReferenceId()))
                    {
                        index++;
                    }
                    if (index > start + 1)
                    {
                        // Multiple InstanceBackReferenceMetadata objects for the same reference: merge them
                        MutableList<BackReference> mergedBackRefs = Lists.mutable.withAll(current.getBackReferences());
                        for (int i = start + 1; i < index; i++)
                        {
                            // merge back refs and set to null to mark for removal
                            mergedBackRefs.addAll(this.instanceBackReferences.set(i, null).getBackReferences().castToList());
                        }
                        this.instanceBackReferences.set(start, new InstanceBackReferenceMetadata(currentId, ListHelper.sortAndRemoveDuplicates(mergedBackRefs).toImmutable()));
                    }
                }
                this.instanceBackReferences.removeIf(Objects::isNull);
            }
            return this.instanceBackReferences.toImmutable();
        }
    }
}
