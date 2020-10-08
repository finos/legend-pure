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

public class EnumRef implements RValue
{
    private final String enumerationId;
    private final String enumName;

    public EnumRef(String enumerationId, String enumName)
    {
        this.enumerationId = enumerationId;
        this.enumName = enumName;
    }

    public String getEnumerationId()
    {
        return this.enumerationId;
    }

    public String getEnumName()
    {
        return this.enumName;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof EnumRef))
        {
            return false;
        }

        EnumRef that = (EnumRef)other;
        return this.enumerationId.equals(that.enumerationId) && this.enumName.equals(that.enumName);
    }

    @Override
    public int hashCode()
    {
        return this.enumerationId.hashCode() + (31 * this.enumName.hashCode());
    }

    @Override
    public Object visit(RValueVisitor visitor)
    {
        return visitor.accept(this);
    }
}
