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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public abstract class Index<K, V extends CoreInstance>
{
    protected final IndexSpecification<K> spec;

    private Index(IndexSpecification<K> spec)
    {
        this.spec = spec;
    }

    public IndexSpecification<K> getSpecification()
    {
        return this.spec;
    }

    public ListIterable<V> get(Object key)
    {
        ListIterable<V> values = tryGet(key);
        return (values == null) ? Lists.immutable.empty() : values;
    }

    public void add(Iterable<? extends V> values)
    {
        values.forEach(this::add);
    }

    public final void add(V value)
    {
        add(this.spec.getIndexKey(value), value);
    }

    public final void remove(Iterable<? extends V> values)
    {
        MutableMap<K, MutableSet<V>> toRemove = Maps.mutable.empty();
        values.forEach(v -> toRemove.getIfAbsentPut(this.spec.getIndexKey(v), Sets.mutable::empty).add(v));
        toRemove.forEachKeyValue(this::remove);
    }

    public final void remove(V value)
    {
        remove(this.spec.getIndexKey(value), value);
    }

    protected abstract ListIterable<V> tryGet(Object key);

    protected abstract void add(K key, V value);

    protected abstract void remove(K key, MutableSet<V> values);

    protected abstract void remove(K key, V value);

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec)
    {
        return newIndex(spec, false);
    }

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec, boolean concurrent)
    {
        return concurrent ? new ConcurrentIndex<>(spec) : new SimpleIndex<>(spec);
    }

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec, Iterable<? extends V> values)
    {
        return newIndex(spec, values, false);
    }

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec, Iterable<? extends V> values, boolean concurrent)
    {
        Index<K, V> index = newIndex(spec, concurrent);
        index.add(values);
        return index;
    }

    private static class SimpleIndex<K, V extends CoreInstance> extends Index<K, V>
    {
        private final MutableMap<K, MutableList<V>> index = Maps.mutable.empty();

        private SimpleIndex(IndexSpecification<K> spec)
        {
            super(spec);
        }

        @Override
        protected ListIterable<V> tryGet(Object key)
        {
            return this.index.get(key);
        }

        @Override
        protected void add(K key, V value)
        {
            this.index.getIfAbsentPut(key, Lists.mutable::empty).add(value);
        }

        @Override
        protected void remove(K key, MutableSet<V> values)
        {
            MutableList<V> currentValues = this.index.get(key);
            if (currentValues != null)
            {
                currentValues.removeIf(values::contains);
            }
        }

        @Override
        protected void remove(K key, V value)
        {
            MutableList<V> currentValues = this.index.get(key);
            if (currentValues != null)
            {
                currentValues.remove(value);
            }
        }
    }

    private static class ConcurrentIndex<K, V extends CoreInstance> extends Index<K, V>
    {
        private final ConcurrentMutableMap<K, ImmutableList<V>> index = ConcurrentHashMap.newMap();

        private ConcurrentIndex(IndexSpecification<K> spec)
        {
            super(spec);
        }

        @Override
        protected ListIterable<V> tryGet(Object key)
        {
            return this.index.get(key);
        }

        @Override
        public void add(Iterable<? extends V> values)
        {
            MutableMap<K, MutableList<V>> toAdd = Maps.mutable.empty();
            values.forEach(v -> toAdd.getIfAbsentPut(this.spec.getIndexKey(v), Lists.mutable::empty).add(v));
            toAdd.forEachKeyValue((k, v) -> this.index.updateValue(k, Functions0.nullValue(), current -> (current == null) ? v.toImmutable() : current.newWithAll(v)));
        }

        @Override
        protected void add(K key, V value)
        {
            this.index.updateValue(key, Functions0.nullValue(), values -> (values == null) ? Lists.immutable.with(value) : values.newWith(value));
        }

        @Override
        protected void remove(K key, MutableSet<V> values)
        {
            if (this.index.containsKey(key))
            {
                ImmutableList<V> updated = this.index.updateValue(key, Functions0.nullValue(), current -> (current == null) ? null : (current.anySatisfy(values::contains) ? current.newWithoutAll(values) : current));
                if ((updated == null) || updated.isEmpty())
                {
                    this.index.remove(key, updated);
                }
            }
        }

        @Override
        protected void remove(K key, V value)
        {
            if (this.index.containsKey(key))
            {
                ImmutableList<V> updated = this.index.updateValue(key, Functions0.nullValue(), current -> (current == null) ? null : current.newWithout(value));
                if ((updated == null) || updated.isEmpty())
                {
                    this.index.remove(key, updated);
                }
            }
        }
    }
}
