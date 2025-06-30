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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.finos.legend.pure.m3.coreinstance.lazy.resolution.LazyResolutionListIterable;
import org.finos.legend.pure.m3.coreinstance.lazy.resolution.LazyResolutionLists;
import org.finos.legend.pure.m3.coreinstance.lazy.resolution.LazyResolutionMutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IDIndex;
import org.finos.legend.pure.m4.coreinstance.indexing.Index;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ManyValues<T> extends AbstractPropertyValue<T>
{
    private static final int MAX_NON_INDEXING_SIZE = 10;
    private static final int MIN_INDEXING_SIZE = 6;

    private volatile ListIterable<T> values;
    private Indexes indexes;

    private ManyValues(ListIterable<T> values)
    {
        this.values = values;
        this.indexes = null;
    }

    private ManyValues(ManyValues<T> src)
    {
        this.values = src.values.toImmutable();
        this.indexes = null;
    }

    @Override
    public boolean hasValue()
    {
        return this.values.notEmpty();
    }

    @Override
    public T getValue()
    {
        ListIterable<T> local = this.values;
        int size = local.size();
        switch (size)
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return local.get(0);
            }
            default:
            {
                throw new IllegalStateException("Expected at most 1 value, found " + size);
            }
        }
    }

    @Override
    public ListIterable<T> getValues()
    {
        ListIterable<T> local = this.values;
        if (!(local instanceof ImmutableList))
        {
            synchronized (this)
            {
                if (!((local = this.values) instanceof ImmutableList))
                {
                    this.values = local = this.values.toImmutable();
                }
            }
        }
        return local;
    }

    @Override
    public ListIterable<? extends CoreInstance> getCoreInstanceValues()
    {
        return toCoreInstances(getValues());
    }

    @Override
    public <K> CoreInstance getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
    {
        synchronized (this)
        {
            ListIterable<T> local = this.values;
            if (this.indexes == null)
            {
                if (local.size() <= MAX_NON_INDEXING_SIZE)
                {
                    CoreInstance result = null;
                    for (T value : local)
                    {
                        CoreInstance coreInstance = toCoreInstance(value);
                        if (key.equals(indexSpec.getIndexKey(coreInstance)))
                        {
                            if (result != null)
                            {
                                throw new IDConflictException(key);
                            }
                            result = coreInstance;
                        }
                    }
                    return result;
                }
                this.indexes = new Indexes();
            }
            return this.indexes.getValueByIDIndex(indexSpec, key);
        }
    }

    @Override
    public <K> ListIterable<CoreInstance> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        synchronized (this)
        {
            ListIterable<T> local = this.values;
            if (this.indexes == null)
            {
                if (local.size() <= MAX_NON_INDEXING_SIZE)
                {
                    MutableList<CoreInstance> result = Lists.mutable.empty();
                    local.forEach(v ->
                    {
                        CoreInstance coreInstance = toCoreInstance(v);
                        if (key.equals(indexSpec.getIndexKey(coreInstance)))
                        {
                            result.add(coreInstance);
                        }
                    });
                    return result;
                }
                this.indexes = new Indexes();
            }
            return this.indexes.getValuesByIndex(indexSpec, key);
        }
    }

    @Override
    public void setValues(RichIterable<? extends T> values)
    {
        synchronized (this)
        {
            this.values = (values == null) ? Lists.immutable.empty() : Lists.immutable.withAll(values);
            this.indexes = null;
        }
    }

    @Override
    public void setCoreInstanceValues(RichIterable<? extends CoreInstance> values)
    {
        setValues((values == null) ? null : fromCoreInstances(values));
    }

    @Override
    public void setValue(int offset, T value)
    {
        synchronized (this)
        {
            ListIterable<T> local = this.values;
            T previous;
            if (local instanceof MutableList)
            {
                previous = ((MutableList<T>) local).set(offset, value);
            }
            else
            {
                MutableList<T> mutable = local.toList();
                previous = mutable.set(offset, value);
                this.values = mutable;
            }
            if (this.indexes != null)
            {
                this.indexes.replaceValue(previous, value);
            }
        }
    }

    @Override
    public void addValue(T value)
    {
        synchronized (this)
        {
            ListIterable<T> local = this.values;
            if (local instanceof MutableList)
            {
                ((MutableList<T>) local).add(value);
            }
            else
            {
                MutableList<T> mutable = local.toList();
                mutable.add(value);
                this.values = mutable;
            }
            if (this.indexes != null)
            {
                this.indexes.addValue(value);
            }
        }
    }

    public void addValues(Iterable<? extends T> values)
    {
        MutableList<? extends T> toAdd = CollectionAdapter.wrapList(values);
        if (toAdd.notEmpty())
        {
            synchronized (this)
            {
                ListIterable<T> local = this.values;
                if (local instanceof MutableList)
                {
                    ((MutableList<T>) local).addAll(toAdd);
                }
                else
                {
                    MutableList<T> mutable = local.toList();
                    mutable.addAll(toAdd);
                    this.values = mutable;
                }
                if (this.indexes != null)
                {
                    this.indexes.addValues(toAdd);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeValue(Object value)
    {
        if (value == null)
        {
            return false;
        }

        synchronized (this)
        {
            ListIterable<T> local = this.values;
            boolean removed;
            if (local instanceof MutableList)
            {
                removed = (local instanceof LazyResolutionMutableList) ?
                          ((LazyResolutionMutableList<?>) local).removeAny(value) :
                          ((MutableList<?>) local).remove(value);
            }
            else
            {
                int index = (local instanceof LazyResolutionListIterable) ?
                            ((LazyResolutionListIterable<?>) local).anyIndexOf(value) :
                            local.indexOf(value);
                removed = (index >= 0);
                if (removed)
                {
                    MutableList<T> mutable = local.toList();
                    mutable.remove(index);
                    this.values = local = mutable;
                }
            }
            if (removed && (this.indexes != null))
            {
                if (local.size() < MIN_INDEXING_SIZE)
                {
                    this.indexes = null;
                }
                else
                {
                    this.indexes.removeValue((T) value);
                }
            }
            return removed;
        }
    }

    @Override
    public void removeAllValues()
    {
        synchronized (this)
        {
            this.values = Lists.immutable.empty();
            this.indexes = null;
        }
    }

    @Override
    public boolean isFullyResolved()
    {
        ListIterable<T> local = this.values;
        return !(local instanceof LazyResolutionListIterable) || ((LazyResolutionListIterable<?>) local).isFullyResolved();
    }

    @Override
    public abstract ManyValues<T> copy();

    ListIterable<? extends CoreInstance> toCoreInstances(ListIterable<? extends T> values)
    {
        return values.collect(this::toCoreInstance);
    }

    RichIterable<? extends T> fromCoreInstances(RichIterable<? extends CoreInstance> values)
    {
        return values.collect(this::fromCoreInstance);
    }

    public static <V extends CoreInstance> ManyValues<V> fromValues(ListIterable<? extends V> propertyValues)
    {
        return fromValues(propertyValues, null);
    }

    public static <V extends CoreInstance> ManyValues<V> fromValues(ListIterable<? extends V> propertyValues, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        ListIterable<V> values = (propertyValues == null) ? Lists.immutable.empty() : Lists.immutable.withAll(propertyValues);
        return (fromCoreInstanceFn == null) ? new ManyValuesCI<>(values) : new ManyValuesF<>(values, fromCoreInstanceFn);
    }

    public static <V> ManyValues<V> fromValues(ListIterable<? extends V> propertyValues, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return new ManyValuesTF<>((propertyValues == null) ? Lists.immutable.empty() : Lists.immutable.withAll(propertyValues), toCoreInstanceFn, fromCoreInstanceFn);
    }

    public static <V extends CoreInstance> ManyValues<V> fromSuppliers(ListIterable<? extends Supplier<? extends V>> suppliers)
    {
        return fromSuppliers(suppliers, null);
    }

    public static <V extends CoreInstance> ManyValues<V> fromSuppliers(ListIterable<? extends Supplier<? extends V>> suppliers, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        ListIterable<V> values = ((suppliers == null) || suppliers.isEmpty()) ? Lists.immutable.empty() : LazyResolutionLists.newImmutable(suppliers);
        return (fromCoreInstanceFn == null) ? new ManyValuesCI<>(values) : new ManyValuesF<>(values, fromCoreInstanceFn);
    }

    public static <V> ManyValues<V> fromSuppliers(ListIterable<? extends Supplier<? extends V>> suppliers, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return new ManyValuesTF<>(((suppliers == null) || suppliers.isEmpty()) ? Lists.immutable.empty() : LazyResolutionLists.newImmutable(suppliers), toCoreInstanceFn, fromCoreInstanceFn);
    }

    private class Indexes
    {
        private MutableMap<IndexSpecification<?>, IDIndex<?, CoreInstance>> idIndexes;
        private MutableMap<IndexSpecification<?>, Index<?, CoreInstance>> indexes;

        <K> CoreInstance getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
        {
            if (this.idIndexes == null)
            {
                this.idIndexes = Maps.mutable.empty();
            }
            IDIndex<?, CoreInstance> idIndex = this.idIndexes.get(indexSpec);
            if (idIndex == null)
            {
                idIndex = IDIndex.newIDIndex(indexSpec, getCoreInstanceValues());
                this.idIndexes.put(indexSpec, idIndex);
            }
            return idIndex.get(key);
        }

        <K> ListIterable<CoreInstance> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
        {
            if (this.indexes == null)
            {
                this.indexes = Maps.mutable.empty();
            }
            Index<?, CoreInstance> index = this.indexes.getIfAbsentPut(indexSpec, () -> Index.newIndex(indexSpec, getCoreInstanceValues()));
            return index.get(key).toImmutable();
        }

        void addValue(T value)
        {
            CoreInstance toAdd = toCoreInstance(value);
            if (this.idIndexes != null)
            {
                this.idIndexes.removeIf((key, idIndex) -> !idIndex.tryAdd(toAdd));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.add(toAdd));
            }
        }

        void addValues(ListIterable<? extends T> values)
        {
            ListIterable<? extends CoreInstance> toAdd = toCoreInstances(values);
            if (this.idIndexes != null)
            {
                this.idIndexes.removeIf((key, idIndex) -> !idIndex.tryAdd(toAdd));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.add(toAdd));
            }
        }

        void removeValue(T value)
        {
            CoreInstance toRemove = toCoreInstance(value);
            if (this.idIndexes != null)
            {
                this.idIndexes.forEachValue(idIndex -> idIndex.remove(toRemove));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.remove(toRemove));
            }
        }

        void removeValues(ListIterable<? extends T> values)
        {
            ListIterable<? extends CoreInstance> toRemove = toCoreInstances(values);
            if (this.idIndexes != null)
            {
                this.idIndexes.forEachValue(idIndex -> idIndex.remove(toRemove));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.remove(toRemove));
            }
        }

        void replaceValue(T oldValue, T newValue)
        {
            CoreInstance toRemove = toCoreInstance(oldValue);
            CoreInstance toAdd = toCoreInstance(newValue);
            if (this.idIndexes != null)
            {
                this.idIndexes.removeIf((key, idIndex) ->
                {
                    idIndex.remove(toRemove);
                    return !idIndex.tryAdd(toAdd);
                });
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index ->
                {
                    index.remove(toRemove);
                    index.add(toAdd);
                });
            }
        }
    }

    private static class ManyValuesCI<T extends CoreInstance> extends ManyValues<T>
    {
        private ManyValuesCI(ListIterable<T> values)
        {
            super(values);
        }

        private ManyValuesCI(ManyValuesCI<T> src)
        {
            super(src);
        }

        @Override
        public CoreInstance getCoreInstanceValue()
        {
            return getValue();
        }

        @Override
        public ListIterable<? extends CoreInstance> getCoreInstanceValues()
        {
            return getValues();
        }

        @Override
        public ManyValues<T> copy()
        {
            synchronized (this)
            {
                return new ManyValuesCI<>(this);
            }
        }

        @Override
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @Override
        ListIterable<? extends CoreInstance> toCoreInstances(ListIterable<? extends T> values)
        {
            return values;
        }

        @SuppressWarnings("unchecked")
        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return (T) value;
        }

        @SuppressWarnings("unchecked")
        @Override
        RichIterable<? extends T> fromCoreInstances(RichIterable<? extends CoreInstance> values)
        {
            return (RichIterable<? extends T>) values;
        }
    }

    private static class ManyValuesF<T extends CoreInstance> extends ManyValues<T>
    {
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private ManyValuesF(ListIterable<T> values, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(values);
            this.fromCoreInstanceFn = Objects.requireNonNull(fromCoreInstanceFn);
        }

        private ManyValuesF(ManyValuesF<T> src)
        {
            super(src);
            this.fromCoreInstanceFn = src.fromCoreInstanceFn;
        }

        @Override
        public CoreInstance getCoreInstanceValue()
        {
            return getValue();
        }

        @Override
        public ListIterable<? extends CoreInstance> getCoreInstanceValues()
        {
            return getValues();
        }

        @Override
        public ManyValues<T> copy()
        {
            synchronized (this)
            {
                return new ManyValuesF<>(this);
            }
        }

        @Override
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @Override
        ListIterable<? extends CoreInstance> toCoreInstances(ListIterable<? extends T> values)
        {
            return values;
        }

        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return this.fromCoreInstanceFn.apply(value);
        }
    }

    private static class ManyValuesTF<T> extends ManyValues<T>
    {
        private final Function<? super T, ? extends CoreInstance> toCoreInstanceFn;
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private ManyValuesTF(ListIterable<T> values, Function<? super T, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(values);
            this.toCoreInstanceFn = Objects.requireNonNull(toCoreInstanceFn);
            this.fromCoreInstanceFn = Objects.requireNonNull(fromCoreInstanceFn);
        }

        private ManyValuesTF(ManyValuesTF<T> src)
        {
            super(src);
            this.toCoreInstanceFn = src.toCoreInstanceFn;
            this.fromCoreInstanceFn = src.fromCoreInstanceFn;
        }

        @Override
        public ManyValues<T> copy()
        {
            synchronized (this)
            {
                return new ManyValuesTF<>(this);
            }
        }

        @Override
        CoreInstance toCoreInstance(T value)
        {
            return this.toCoreInstanceFn.apply(value);
        }

        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return this.fromCoreInstanceFn.apply(value);
        }
    }
}
