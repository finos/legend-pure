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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

import java.util.Objects;

public abstract class PropertyValues
{
    public abstract String getPropertyName();

    public abstract String getPropertySourceType();

    public abstract ListIterable<ValueOrReference> getValues();

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PropertyValues))
        {
            return false;
        }

        PropertyValues that = (PropertyValues) other;
        return this.getPropertyName().equals(that.getPropertyName()) &&
                this.getPropertySourceType().equals(that.getPropertySourceType()) &&
                this.getValues().equals(that.getValues());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getPropertyName(), getPropertySourceType(), getValues());
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder(128)).toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("PropertyValues{property=").append(getPropertyName())
                .append(", propertySourceType=").append(getPropertySourceType())
                .append(" values=[");
        getValues().forEach(v -> v.appendString(builder).append(", "));
        builder.setLength(builder.length() - 2);
        return builder.append("]}");
    }

    public static PropertyValues newPropertyValues(String propertyName, String propertySourceType, ValueOrReference value)
    {
        return newPropertyValues(propertyName, propertySourceType, Lists.immutable.with(value));
    }

    public static PropertyValues newPropertyValues(String propertyName, String propertySourceType, ValueOrReference... values)
    {
        return newPropertyValues(propertyName, propertySourceType, Lists.immutable.with(values));
    }

    public static PropertyValues newPropertyValues(String propertyName, String propertySourceType, ListIterable<ValueOrReference> values)
    {
        Objects.requireNonNull(propertyName);
        Objects.requireNonNull(propertySourceType);
        Objects.requireNonNull(values);
        return new PropertyValues()
        {
            @Override
            public String getPropertyName()
            {
                return propertyName;
            }

            @Override
            public String getPropertySourceType()
            {
                return propertySourceType;
            }

            @Override
            public ListIterable<ValueOrReference> getValues()
            {
                return values;
            }
        };
    }
}
