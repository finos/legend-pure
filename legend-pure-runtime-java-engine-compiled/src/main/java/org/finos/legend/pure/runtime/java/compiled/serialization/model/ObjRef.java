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

import org.finos.legend.pure.m4.ModelRepository;

public class ObjRef implements RValue
{
    private final String classifierId;
    private final String id;

    public ObjRef(String classifierId, String id)
    {
        if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(classifierId))
        {
            throw new RuntimeException("No primitives for ObjRef");
        }
        this.classifierId = classifierId;
        this.id = id;
    }

    public String getClassifierId()
    {
        return this.classifierId;
    }

    public String getId()
    {
        return this.id;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ObjRef))
        {
            return false;
        }

        ObjRef that = (ObjRef) other;
        return this.id.equals(that.id) && this.classifierId.equals(that.classifierId);
    }

    @Override
    public int hashCode()
    {
        return this.classifierId.hashCode() + (31 * this.id.hashCode());
    }

    @Override
    public <T> T visit(RValueVisitor<T> visitor)
    {
        return visitor.accept(this);
    }
}
