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

package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

public class PropertyValueMany implements PropertyValue
{
    private final String property;
    private final ListIterable<RValue> values;

    public PropertyValueMany(String property, ListIterable<RValue> values)
    {
        this.property = property;
        this.values = (values == null) ? Lists.immutable.empty() : values;
    }

    @Override
    public String getProperty()
    {
        return this.property;
    }

    public ListIterable<RValue> getValues()
    {
        return this.values;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PropertyValueMany))
        {
            return false;
        }

        PropertyValueMany that = (PropertyValueMany) other;
        return this.property.equals(that.property) && this.values.equals(that.values);
    }

    @Override
    public int hashCode()
    {
        return this.property.hashCode() + (53 * this.values.hashCode());
    }

    @Override
    public String toString()
    {
        return this.property + "=" + this.values;
    }

    @Override
    public <T> T visit(PropertyValueVisitor<T> visitor)
    {
        return visitor.accept(this);
    }
}
