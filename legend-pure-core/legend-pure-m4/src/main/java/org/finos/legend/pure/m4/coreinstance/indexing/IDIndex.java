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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Collection;
import java.util.Objects;

public class IDIndex<K, V extends CoreInstance>
{
    private final IndexSpecification<K> spec;
    private final MutableMap<K, V> index;

    private IDIndex(IndexSpecification<K> spec, MutableMap<K, V> index)
    {
        this.spec = spec;
        this.index = index;
    }

    private IDIndex(IndexSpecification<K> spec, MutableMap<K, V> index, Iterable<? extends V> values) throws IDConflictException
    {
        this(spec, index);
        add(values);
    }

    public IndexSpecification<K> getSpecification()
    {
        return this.spec;
    }

    public V get(Object key)
    {
        return this.index.get(key);
    }

    public boolean tryAdd(Iterable<? extends V> values)
    {
        for (V value : values)
        {
            if (!tryAdd(value))
            {
                return false;
            }
        }
        return true;
    }

    public void add(Iterable<? extends V> values) throws IDConflictException
    {
        for (V value : values)
        {
            add(value);
        }
    }

    public boolean tryAdd(V value)
    {
        return tryAdd(this.spec.getIndexKey(value), value);
    }

    public void add(V value) throws IDConflictException
    {
        K key = this.spec.getIndexKey(value);
        if (!tryAdd(key, value))
        {
            throw new IDConflictException(key);
        }
    }

    private boolean tryAdd(K key, V value)
    {
        V old = this.index.getIfAbsentPut(key, value);
        return (old == value) || Objects.equals(old, value);
    }

    public void remove(Iterable<? extends V> values)
    {
        for (V value : values)
        {
            remove(value);
        }
    }

    public void remove(V value)
    {
        K key = this.spec.getIndexKey(value);
        this.index.remove(key, value);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec)
    {
        return newIDIndex(spec, false);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, boolean concurrent)
    {
        return new IDIndex<>(spec, concurrent ? ConcurrentHashMap.newMap() : Maps.mutable.empty());
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values) throws IDConflictException
    {
        return newIDIndex(spec, values, false);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values, boolean concurrent) throws IDConflictException
    {
        int initCapacity = getInitialSizeForMap(values);
        return new IDIndex<>(spec, concurrent ? ConcurrentHashMap.newMap(initCapacity) : Maps.mutable.ofInitialCapacity(initCapacity), values);
    }

    private static int getInitialSizeForMap(Iterable<?> values)
    {
        if (values instanceof Collection)
        {
            return ((Collection<?>) values).size();
        }
        if ((values instanceof RichIterable) && !(values instanceof LazyIterable))
        {
            return ((RichIterable<?>) values).size();
        }
        return 16;
    }
}
