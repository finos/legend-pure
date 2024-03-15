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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

final class SingleValue<V extends CoreInstance> implements Values<V>
{
    private V value;

    SingleValue(V value)
    {
        this.value = value;
    }

    @Override
    public V getOneValue() throws OneValueException
    {
        return this.value;
    }

    @Override
    public ListIterable<V> getValues()
    {
        return Lists.immutable.with(this.value);
    }

    @Override
    public <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key)
    {
        return Comparators.nullSafeEquals(key, indexSpec.getIndexKey(this.value)) ? this.value : null;
    }

    @Override
    public <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        return Comparators.nullSafeEquals(key, indexSpec.getIndexKey(this.value)) ? Lists.immutable.with(this.value) : Lists.immutable.<V>empty();
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public Values<V> addValue(V value)
    {
        return new SmallValues<>(Lists.immutable.with(this.value, value));
    }

    @Override
    public Values<V> addValues(ListIterable<V> values)
    {
        if (values.isEmpty())
        {
            return this;
        }

        MutableList<V> newValues = FastList.newList(values.size() + 1);
        newValues.add(this.value);
        newValues.addAllIterable(values);
        return ValueHolder.newValues(newValues);
    }

    @Override
    public Values<V> removeValue(V value)
    {
        return this.value.equals(value) ? (Values<V>)EmptyValues.EMPTY_VALUES : this;
    }

    @Override
    public void setValue(int offset, V value)
    {
        if (offset != 0)
        {
            throw new IndexOutOfBoundsException("Index: " + offset + " Size: 1");
        }
        this.value = value;
    }

    @Override
    public Values<V> copy()
    {
        return new SingleValue<>(this.value);
    }
}
