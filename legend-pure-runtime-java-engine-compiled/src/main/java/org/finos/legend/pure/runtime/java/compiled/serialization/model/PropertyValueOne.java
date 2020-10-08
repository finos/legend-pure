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

import org.eclipse.collections.impl.block.factory.Comparators;

public class PropertyValueOne implements PropertyValue
{
    private final String property;
    private final RValue value;

    public PropertyValueOne(String property, RValue value)
    {
        this.property = property;
        this.value = value;
    }

    @Override
    public String getProperty()
    {
        return this.property;
    }

    public RValue getValue()
    {
        return this.value;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PropertyValueOne))
        {
            return false;
        }

        PropertyValueOne that = (PropertyValueOne)other;
        return this.property.equals(that.property) && Comparators.nullSafeEquals(this.value, that.value);
    }

    @Override
    public int hashCode()
    {
        int hashCode = this.property.hashCode();
        if (this.value != null)
        {
            hashCode += 31 * this.value.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        return this.property + "=" + this.value;
    }

    @Override
    public Object visit(PropertyValueVisitor visitor)
    {
        return visitor.accept(this);
    }
}
