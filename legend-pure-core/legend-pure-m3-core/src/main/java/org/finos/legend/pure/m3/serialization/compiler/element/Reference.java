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

import java.util.Objects;

public abstract class Reference extends ValueOrReference
{
    private Reference()
    {
    }

    public static ExternalReference newExternalReference(String id)
    {
        return new ExternalReference(id);
    }

    public static InternalReference newInternalReference(int id)
    {
        return new InternalReference(id);
    }

    public static class ExternalReference extends Reference
    {
        private final String id;

        private ExternalReference(String id)
        {
            this.id = Objects.requireNonNull(id);
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof ExternalReference) && this.id.equals(((ExternalReference) other).id));
        }

        @Override
        public int hashCode()
        {
            return this.id.hashCode();
        }

        public String getId()
        {
            return this.id;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        StringBuilder appendString(StringBuilder builder)
        {
            return builder.append("ExternalReference{id=").append(this.id).append('}');
        }
    }

    public static class InternalReference extends Reference
    {
        private final int id;

        private InternalReference(int id)
        {
            this.id = id;
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof InternalReference) && (this.id == ((InternalReference) other).id));
        }

        @Override
        public int hashCode()
        {
            return this.id;
        }

        public int getId()
        {
            return this.id;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        StringBuilder appendString(StringBuilder builder)
        {
            return builder.append("InternalReference{id=").append(this.id).append('}');
        }
    }
}
