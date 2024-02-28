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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
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

    public abstract ListIterable<V> get(Object key);

    public abstract void add(Iterable<? extends V> values);

    public abstract void add(V value);

    public abstract void remove(Iterable<? extends V> values);

    public abstract void remove(V value);

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec)
    {
        return newIndex(spec, false);
    }

    public static <K, V extends CoreInstance> Index<K, V> newIndex(IndexSpecification<K> spec, boolean concurrent)
    {
        return concurrent ? new ConcurrentIndex<K, V>(spec) : new SimpleIndex<K, V>(spec);
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
        private final MutableListMultimap<Object, V> index = Multimaps.mutable.list.empty();

        private SimpleIndex(IndexSpecification<K> spec)
        {
            super(spec);
        }

        @Override
        public ListIterable<V> get(Object key)
        {
            return this.index.get(key);
        }

        @Override
        public void add(Iterable<? extends V> values)
        {
            for (V value : values)
            {
                add(value);
            }
        }

        @Override
        public void add(V value)
        {
            K key = this.spec.getIndexKey(value);
            this.index.put(key, value);
        }

        @Override
        public void remove(Iterable<? extends V> values)
        {
            for (V value : values)
            {
                remove(value);
            }
        }

        @Override
        public void remove(V value)
        {
            K key = this.spec.getIndexKey(value);
            this.index.remove(key, value);
        }
    }

    private static class ConcurrentIndex<K, V extends CoreInstance> extends Index<K, V>
    {
        private final ConcurrentMutableMap<K, ImmutableList<V>> index = ConcurrentHashMap.newMap();

        private final Function2<ImmutableList<V>, V, ImmutableList<V>> addValue = new Function2<ImmutableList<V>, V, ImmutableList<V>>()
        {
            @Override
            public ImmutableList<V> value(ImmutableList<V> values, V newValue)
            {
                return (values == null) ? Lists.immutable.with(newValue) : values.newWith(newValue);
            }
        };

        private final Function2<ImmutableList<V>, Iterable<? extends V>, ImmutableList<V>> addValues = new Function2<ImmutableList<V>, Iterable<? extends V>, ImmutableList<V>>()
        {
            @Override
            public ImmutableList<V> value(ImmutableList<V> values, Iterable<? extends V> newValues)
            {
                return (values == null) ? Lists.immutable.withAll(newValues) : values.newWithAll(newValues);
            }
        };

        private final Function2<ImmutableList<V>, V, ImmutableList<V>> removeValue = new Function2<ImmutableList<V>, V, ImmutableList<V>>()
        {
            @Override
            public ImmutableList<V> value(ImmutableList<V> values, V newValue)
            {
                return (values == null) ? null : values.newWithout(newValue);
            }
        };

        private final Function2<ImmutableList<V>, Iterable<? extends V>, ImmutableList<V>> removeValues = new Function2<ImmutableList<V>, Iterable<? extends V>, ImmutableList<V>>()
        {
            @Override
            public ImmutableList<V> value(ImmutableList<V> values, Iterable<? extends V> newValues)
            {
                return (values == null) ? null : values.newWithoutAll(newValues);
            }
        };

        private ConcurrentIndex(IndexSpecification<K> spec)
        {
            super(spec);
        }

        @Override
        public ListIterable<V> get(Object key)
        {
            return this.index.get(key);
        }

        @Override
        public void add(Iterable<? extends V> values)
        {
            Iterate.groupBy(values, this.spec).forEachKeyMultiValues(new Procedure2<K, Iterable<? extends V>>()
            {
                @Override
                public void value(K key, Iterable<? extends V> keyValues)
                {
                    ConcurrentIndex.this.index.updateValueWith(key, Functions0.<ImmutableList<V>>nullValue(), ConcurrentIndex.this.addValues, keyValues);
                }
            });
        }

        @Override
        public void add(V value)
        {
            K key = this.spec.getIndexKey(value);
            this.index.updateValueWith(key, Functions0.<ImmutableList<V>>nullValue(), this.addValue, value);
        }

        @Override
        public void remove(Iterable<? extends V> values)
        {
            Iterate.groupBy(values, this.spec).forEachKeyMultiValues(new Procedure2<K, Iterable<? extends V>>()
            {
                @Override
                public void value(K key, Iterable<? extends V> keyValues)
                {
                    ImmutableList<V> newValues = ConcurrentIndex.this.index.updateValueWith(key, Functions0.<ImmutableList<V>>nullValue(), ConcurrentIndex.this.removeValues, keyValues);
                    if (newValues == null)
                    {
                        ConcurrentIndex.this.index.remove(key, null);
                    }
                }
            });
        }

        @Override
        public void remove(V value)
        {
            K key = this.spec.getIndexKey(value);
            if (this.index.containsKey(key))
            {
                ImmutableList<V> newValues = this.index.updateValueWith(key, Functions0.<ImmutableList<V>>nullValue(), this.removeValue, value);
                if (newValues == null)
                {
                    this.index.remove(key, null);
                }
            }
        }
    }
}
