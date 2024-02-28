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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Collection;

public abstract class IDIndex<K, V extends CoreInstance>
{
    protected final IndexSpecification<K> spec;
    protected final MutableMap<K, V> index;

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

    public void add(Iterable<? extends V> values) throws IDConflictException
    {
        for (V value : values)
        {
            add(value);
        }
    }

    public void add(V value) throws IDConflictException
    {
        K key = this.spec.getIndexKey(value);
        V old = this.index.getIfAbsentPut(key, value);
        if ((old != value) && !Comparators.nullSafeEquals(old, value))
        {
            throw new IDConflictException(key);
        }
    }

    public void remove(Iterable<? extends V> values)
    {
        for (V value : values)
        {
            remove(value);
        }
    }

    public abstract void remove(V value);

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec)
    {
        return newIDIndex(spec, false);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, boolean concurrent)
    {
        return concurrent ? new ConcurrentIDIndex<K, V>(spec) : new SimpleIDIndex<K, V>(spec);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values) throws IDConflictException
    {
        return newIDIndex(spec, values, false);
    }

    public static <K, V extends CoreInstance> IDIndex<K, V> newIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values, boolean concurrent) throws IDConflictException
    {
        return concurrent ? new ConcurrentIDIndex<>(spec, values) : new SimpleIDIndex<>(spec, values);
    }

    private static class SimpleIDIndex<K, V extends CoreInstance> extends IDIndex<K, V>
    {
        private SimpleIDIndex(IndexSpecification<K> spec)
        {
            super(spec, Maps.mutable.<K, V>empty());
        }

        private SimpleIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values) throws IDConflictException
        {
            super(spec, UnifiedMap.<K, V>newMap(IDIndex.getInitialSizeForMap(values)), values);
        }

        @Override
        public void remove(V value)
        {
            K key = this.spec.getIndexKey(value);
            V removed = this.index.remove(key);
            if ((removed != null) && !removed.equals(value))
            {
                this.index.put(key, removed);
            }
        }
    }

    private static class ConcurrentIDIndex<K, V extends CoreInstance> extends IDIndex<K, V>
    {
        private ConcurrentIDIndex(IndexSpecification<K> spec)
        {
            super(spec, ConcurrentHashMap.<K, V>newMap());
        }

        private ConcurrentIDIndex(IndexSpecification<K> spec, Iterable<? extends V> values) throws IDConflictException
        {
            super(spec, ConcurrentHashMap.<K, V>newMap(IDIndex.getInitialSizeForMap(values)), values);
        }

        @Override
        public void remove(V value)
        {
            K key = this.spec.getIndexKey(value);
            ((ConcurrentMutableMap<K, V>)this.index).remove(key, value);
        }
    }

    private static int getInitialSizeForMap(Iterable<?> values)
    {
        if (values instanceof Collection)
        {
            return ((Collection<?>)values).size();
        }
        if (values instanceof RichIterable)
        {
            return ((RichIterable<?>)values).size();
        }
        return 16;
    }
}
