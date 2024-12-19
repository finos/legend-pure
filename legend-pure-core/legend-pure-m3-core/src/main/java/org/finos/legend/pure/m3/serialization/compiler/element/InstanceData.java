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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class InstanceData
{
    private final String name;
    private final String classifierPath;
    private final SourceInformation sourceInfo;
    private final String referenceId;
    private final int compileStateBitSet;
    private final ListIterable<? extends PropertyValues> propertyValues;

    private InstanceData(String name, String classifierPath, SourceInformation sourceInfo, String referenceId, int compileStateBitSet, ListIterable<? extends PropertyValues> propertyValues)
    {
        this.name = name;
        this.classifierPath = Objects.requireNonNull(classifierPath);
        this.sourceInfo = sourceInfo;
        this.referenceId = referenceId;
        this.compileStateBitSet = compileStateBitSet;
        this.propertyValues = Objects.requireNonNull(propertyValues);
    }

    public String getName()
    {
        return this.name;
    }

    public String getClassifierPath()
    {
        return this.classifierPath;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInfo;
    }

    public String getReferenceId()
    {
        return this.referenceId;
    }

    public int getCompileStateBitSet()
    {
        return this.compileStateBitSet;
    }

    public ListIterable<? extends PropertyValues> getPropertyValues()
    {
        return this.propertyValues;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof InstanceData))
        {
            return false;
        }

        InstanceData that = (InstanceData) other;
        return (this.compileStateBitSet == that.compileStateBitSet) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.classifierPath, that.classifierPath) &&
                Objects.equals(this.sourceInfo, that.sourceInfo) &&
                Objects.equals(this.referenceId, that.referenceId) &&
                this.propertyValues.equals(that.propertyValues);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.name, this.classifierPath, this.sourceInfo, this.referenceId, this.compileStateBitSet);
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder(128)).toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append(getClass().getSimpleName()).append("{classifier=").append(this.classifierPath);
        if (this.referenceId != null)
        {
            builder.append(" referenceId='").append(this.referenceId).append('\'');
        }
        if (this.name != null)
        {
            builder.append(" name='").append(this.name).append('\'');
        }
        if (this.sourceInfo != null)
        {
            this.sourceInfo.appendMessage(builder.append(" sourceInfo="));
        }
        builder.append(" compiledStateBitSet=").append(this.compileStateBitSet)
                .append(" propertyValues=[");
        if (this.propertyValues.notEmpty())
        {
            this.propertyValues.forEach(pv ->
            {
                builder.append(pv.getPropertyName());
                ListIterable<ValueOrReference> values = pv.getValues();
                switch (values.size())
                {
                    case 0:
                    {
                        builder.append("=[]");
                        break;
                    }
                    case 1:
                    {
                        values.get(0).appendString(builder.append('='));
                        break;
                    }
                    default:
                    {
                        builder.append("=[");
                        values.forEach(v -> v.appendString(builder).append(", "));
                        builder.setLength(builder.length() - 2);
                        builder.append(']');
                    }
                }
                builder.append(", ");
            });
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]}");
    }

    public static InstanceData newInstanceData(String name, String classifierPath, SourceInformation sourceInfo, String referenceId, int compileStateBitSet, ListIterable<? extends PropertyValues> propertyValues)
    {
        return new InstanceData(name, classifierPath, sourceInfo, referenceId, compileStateBitSet, propertyValues);
    }
}
