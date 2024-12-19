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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.eclipse.collections.api.list.ImmutableList;

import java.util.Objects;

public class DeserializedConcreteElement
{
    private final String path;
    private final int referenceIdVersion;
    private final ImmutableList<InstanceData> instanceData;

    private DeserializedConcreteElement(String path, int referenceIdVersion, ImmutableList<InstanceData> instanceData)
    {
        this.path = Objects.requireNonNull(path);
        this.referenceIdVersion = referenceIdVersion;
        this.instanceData = Objects.requireNonNull(instanceData);
        if (this.instanceData.isEmpty())
        {
            throw new IllegalArgumentException("instance data may not be empty");
        }
    }

    /**
     * Get the package path of the concrete element that was deserialized.
     *
     * @return concrete element package path
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Get the reference id version used when the element was serialized.
     *
     * @return reference id version
     */
    public int getReferenceIdVersion()
    {
        return this.referenceIdVersion;
    }

    /**
     * Get the instance data for all instances that were deserialized, including the concrete element itself and all
     * component instances. The concrete element itself will always be first in the list. (As a consequence, the list
     * will never be empty.)
     *
     * @return deserialized instance data
     */
    public ImmutableList<InstanceData> getInstanceData()
    {
        return this.instanceData;
    }

    /**
     * Get the instance data at the given index. This index is also its id as an internal instance.
     *
     * @param index instance data index
     * @return instance data at index
     */
    public InstanceData getInstanceData(int index)
    {
        return this.instanceData.get(index);
    }

    /**
     * Get the instance data for the concrete element itself. This is equivalent to {@code getInstanceData(0)}.
     *
     * @return concrete element instance data
     */
    public InstanceData getConcreteElementData()
    {
        return getInstanceData(0);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof DeserializedConcreteElement))
        {
            return false;
        }
        DeserializedConcreteElement that = (DeserializedConcreteElement) other;
        return (this.referenceIdVersion == that.referenceIdVersion) &&
                this.path.equals(that.path) &&
                this.instanceData.equals(that.instanceData);
    }

    @Override
    public int hashCode()
    {
        return (31 * getPath().hashCode()) + Integer.hashCode(getReferenceIdVersion());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(InstanceData.class.getSimpleName())
                .append("{path=").append(this.path)
                .append(" referenceIdVersion=").append(this.referenceIdVersion)
                .append(" instances=[");
        this.instanceData.forEach(id -> id.appendString(builder).append(", "));
        builder.setLength(builder.length() - 2);
        return builder.append("]}").toString();
    }

    public static DeserializedConcreteElement newDeserializedConcreteElement(String path, int referenceIdVersion, ImmutableList<InstanceData> instanceData)
    {
        return new DeserializedConcreteElement(path, referenceIdVersion, instanceData);
    }
}
