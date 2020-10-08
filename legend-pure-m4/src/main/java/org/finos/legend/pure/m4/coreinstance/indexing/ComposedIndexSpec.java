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

package org.finos.legend.pure.m4.coreinstance.indexing;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class ComposedIndexSpec<K> extends IndexSpecification<K>
{
    private final IndexSpecification<? extends CoreInstance> first;
    private final IndexSpecification<K> second;

    ComposedIndexSpec(IndexSpecification<? extends CoreInstance> first, IndexSpecification<K> second)
    {
        if ((first == null) || (second == null))
        {
            throw new IllegalArgumentException("Composed functions must not be null");
        }
        this.first = first;
        this.second = second;
    }

    @Override
    public K getIndexKey(CoreInstance value)
    {
        CoreInstance firstResult = this.first.getIndexKey(value);
        return (firstResult == null) ? null : this.second.getIndexKey(firstResult);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (this.getClass() != other.getClass())
        {
            return false;
        }

        ComposedIndexSpec<?> that = (ComposedIndexSpec<?>)other;
        return this.first.equals(that.first) && this.second.equals(that.second);
    }

    @Override
    public int hashCode()
    {
        return this.first.hashCode() ^ this.second.hashCode();
    }
}
