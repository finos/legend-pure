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
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IDIndex;
import org.finos.legend.pure.m4.coreinstance.indexing.Index;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

final class ValuesWithIndexing<V extends CoreInstance> implements Values<V>
{
    private static final int MIN_SIZE = INDEXING_THRESHOLD - INDEXING_TOLERANCE;

    private final MutableList<V> values;
    private MutableMap<IndexSpecification<?>, IDIndex<?, V>> idIndexes;
    private MutableMap<IndexSpecification<?>, Index<?, V>> indexes;

    ValuesWithIndexing(MutableList<V> values)
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
        return this.values.asUnmodifiable();
    }

    @Override
    public <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
    {
        if (this.idIndexes == null)
        {
            this.idIndexes = Maps.mutable.empty();
        }
        IDIndex<?, V> idIndex = this.idIndexes.get(indexSpec);
        if (idIndex == null)
        {
            idIndex = IDIndex.newIDIndex(indexSpec, this.values);
            this.idIndexes.put(indexSpec, idIndex);
        }
        return idIndex.get(key);
    }

    @Override
    public <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        if (this.indexes == null)
        {
            this.indexes = Maps.mutable.empty();
        }
        Index<?, V> index = this.indexes.get(indexSpec);
        if (index == null)
        {
            index = Index.newIndex(indexSpec, this.values);
            this.indexes.put(indexSpec, index);
        }
        return index.get(key);
    }

    @Override
    public int size()
    {
        return this.values.size();
    }

    @Override
    public Values<V> addValue(V value)
    {
        this.values.add(value);
        addToIndexes(value);
        return this;
    }

    @Override
    public Values<V> addValues(ListIterable<V> values)
    {
        this.values.addAllIterable(values);
        addToIndexes(values);
        return this;
    }

    @Override
    public Values<V> removeValue(V value)
    {
        if (this.values.remove(value))
        {
            if (this.values.size() < MIN_SIZE)
            {
                return new SmallValues<>(this.values.toImmutable());
            }
            removeFromIndexes(value);
        }
        return this;
    }

    @Override
    public void setValue(int offset, V value)
    {
        V oldValue = this.values.set(offset, value);
        if (oldValue != value)
        {
            removeFromIndexes(oldValue);
            addToIndexes(value);
        }
    }

    @Override
    public Values<V> copy()
    {
        return new ValuesWithIndexing<>(this.values.toList());
    }

    private void addToIndexes(V value)
    {
        if (this.idIndexes != null)
        {
            MutableList<IDIndex<?, V>> invalidIdIndexes = Lists.mutable.empty();
            for (IDIndex<?, V> idIndex : this.idIndexes.valuesView())
            {
                try
                {
                    idIndex.add(value);
                }
                catch (IDConflictException e)
                {
                    invalidIdIndexes.add(idIndex);
                }
            }
            for (IDIndex<?, V> invalidIdIndex : invalidIdIndexes)
            {
                this.idIndexes.remove(invalidIdIndex.getSpecification());
            }
        }
        if (this.indexes != null)
        {
            for (Index<?, V> index : this.indexes.valuesView())
            {
                index.add(value);
            }
        }
    }

    private void addToIndexes(Iterable<? extends V> values)
    {
        if (this.idIndexes != null)
        {
            MutableList<IDIndex<?, V>> invalidIdIndexes = Lists.mutable.empty();
            for (IDIndex<?, V> idIndex : this.idIndexes.valuesView())
            {
                try
                {
                    idIndex.add(values);
                }
                catch (IDConflictException e)
                {
                    invalidIdIndexes.add(idIndex);
                }
            }
            for (IDIndex<?, V> invalidIdIndex : invalidIdIndexes)
            {
                this.idIndexes.remove(invalidIdIndex.getSpecification());
            }
        }
        if (this.indexes != null)
        {
            for (Index<?, V> index : this.indexes.valuesView())
            {
                index.add(values);
            }
        }
    }

    private void removeFromIndexes(V value)
    {
        if (this.idIndexes != null)
        {
            for (IDIndex<?, V> idIndex : this.idIndexes.valuesView())
            {
                idIndex.remove(value);
            }
        }
        if (this.indexes != null)
        {
            for (Index<?, V> index : this.indexes.valuesView())
            {
                index.remove(value);
            }
        }
    }
}
