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

import java.util.Objects;

public class Primitive implements RValue
{
    private final Object value;

    public Primitive(Object value)
    {
        this.value = value;
    }

    public Object getValue()
    {
        return this.value;
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) || ((other instanceof Primitive) && Objects.equals(this.value, ((Primitive) other).value));
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.value);
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.value);
    }

    @Override
    public <T> T visit(RValueVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
