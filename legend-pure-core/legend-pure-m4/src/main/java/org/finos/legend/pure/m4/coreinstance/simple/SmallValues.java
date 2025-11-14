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

package org.finos.legend.pure.m4.coreinstance.simple;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

final class SmallValues<V extends CoreInstance> implements Values<V>
{
    private static final int MAX_SIZE = Values.INDEXING_THRESHOLD + Values.INDEXING_TOLERANCE;

    private ImmutableList<V> values;

    SmallValues(ImmutableList<V> values)
    {
        this.values = values;
    }

    @Override
    public V getOneValue() throws OneValueException
    {
        throw new OneValueException(this.values.size());
    }

    @Override
    public ListIterable<V> getValues()
    {
        return this.values;
    }

    @Override
    public <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
    {
        V result = null;
        for (V value : this.values)
        {
            if (key.equals(indexSpec.getIndexKey(value)))
            {
                if (result != null)
                {
                    throw new IDConflictException(key);
                }
                result = value;
            }
        }
        return result;
    }

    @Override
    public <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        return this.values.select(v -> key.equals(indexSpec.getIndexKey(v)), Lists.mutable.empty());
    }

    @Override
    public int size()
    {
        return this.values.size();
    }

    @Override
    public Values<V> addValue(V value)
    {
        if (this.values.size() >= MAX_SIZE)
        {
            return new ValuesWithIndexing<>(Lists.mutable.<V>withInitialCapacity(this.values.size() + 1)
                    .withAll(this.values)
                    .with(value));
        }
        this.values = this.values.newWith(value);
        return this;
    }

    @Override
    public Values<V> addValues(ListIterable<V> values)
    {
        int newSize = this.values.size() + values.size();
        if (newSize > MAX_SIZE)
        {
            return new ValuesWithIndexing<>(Lists.mutable.<V>withInitialCapacity(newSize)
                    .withAll(this.values)
                    .withAll(values));
        }

        if (values.notEmpty())
        {
            this.values = this.values.newWithAll(values);
        }
        return this;
    }

    @Override
    public Values<V> removeValue(V value)
    {
        if (this.values.size() == 2)
        {
            int index = this.values.indexOf(value);
            return (index == -1) ? this : new SingleValue<>(this.values.get(1 - index));
        }
        this.values = this.values.newWithout(value);
        return this;
    }

    @Override
    public void setValue(int offset, V value)
    {
        MutableList<V> mutableValues = this.values.toList();
        mutableValues.set(offset, value);
        this.values = mutableValues.toImmutable();
    }

    @Override
    public Values<V> copy()
    {
        return new SmallValues<>(this.values);
    }
}
