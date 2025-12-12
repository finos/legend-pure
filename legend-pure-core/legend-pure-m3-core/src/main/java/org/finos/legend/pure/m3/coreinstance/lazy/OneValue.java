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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.lazy.resolution.LazyResolutionLists;
import org.finos.legend.pure.m3.coreinstance.lazy.resolution.LazyResolver;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class OneValue<T> extends AbstractPropertyValue<T>
{
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<OneValue, Object> UPDATER = AtomicReferenceFieldUpdater.newUpdater(OneValue.class, Object.class, "value");

    protected volatile T value;

    private OneValue()
    {
    }

    @Override
    public boolean hasValue()
    {
        return this.value != null;
    }

    @Override
    public T getValue()
    {
        return this.value;
    }

    @Override
    public ListIterable<T> getValues()
    {
        T local = this.value;
        return (local == null) ? Lists.immutable.empty() : Lists.immutable.with(local);
    }

    @Override
    public ListIterable<? extends CoreInstance> getCoreInstanceValues()
    {
        T local = getValue();
        return (local == null) ? Lists.immutable.empty() : Lists.immutable.with(toCoreInstance(local));
    }

    @Override
    public <K> CoreInstance getValueByIDIndex(IndexSpecification<K> indexSpec, K key)
    {
        T local = getValue();
        if (local != null)
        {
            CoreInstance coreInstance = toCoreInstance(local);
            if (key.equals(indexSpec.getIndexKey(coreInstance)))
            {
                return coreInstance;
            }
        }
        return null;
    }

    @Override
    public <K> ListIterable<CoreInstance> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        T local = getValue();
        if (local != null)
        {
            CoreInstance coreInstance = toCoreInstance(local);
            if (key.equals(indexSpec.getIndexKey(coreInstance)))
            {
                return Lists.immutable.with(coreInstance);
            }
        }
        return Lists.immutable.empty();
    }

    @Override
    public void setValue(T newValue)
    {
        this.value = newValue;
    }

    @Override
    public void setValues(RichIterable<? extends T> values)
    {
        int size = (values == null) ? 0 : values.size();
        switch (size)
        {
            case 0:
            {
                setValue(null);
                return;
            }
            case 1:
            {
                setValue(values.getAny());
                return;
            }
            default:
            {
                throw new IllegalArgumentException("Cannot set multiple values for a to-one property: " + size + " values provided");
            }
        }
    }

    @Override
    public void setCoreInstanceValues(RichIterable<? extends CoreInstance> values)
    {
        int size = (values == null) ? 0 : values.size();
        switch (size)
        {
            case 0:
            {
                setValue(null);
                return;
            }
            case 1:
            {
                setCoreInstanceValue(values.getAny());
                return;
            }
            default:
            {
                throw new IllegalArgumentException("Cannot set multiple values for a to-one property: " + size + " values provided");
            }
        }
    }

    @Override
    public void setValue(int offset, T value)
    {
        if (offset != 0)
        {
            throw new IllegalArgumentException("Cannot modify value at offset " + offset + " for to-one property");
        }
        setValue(value);
    }

    @Override
    public void setCoreInstanceValue(int offset, CoreInstance value)
    {
        if (offset != 0)
        {
            throw new IllegalArgumentException("Cannot modify value at offset " + offset + " for to-one property");
        }
        setCoreInstanceValue(value);
    }

    @Override
    public void addValue(T value)
    {
        if (!UPDATER.compareAndSet(this, null, value))
        {
            throw new IllegalStateException("Cannot add value to to-one property: value already present");
        }
    }

    @Override
    public boolean removeValue(Object value)
    {
        if (value != null)
        {
            T currentValue;
            while (((currentValue = this.value) != null) && currentValue.equals(value))
            {
                if (UPDATER.compareAndSet(this, currentValue, null))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void removeAllValues()
    {
        setValue(null);
    }

    @Override
    public abstract OneValue<T> copy();

    public static <V extends CoreInstance> OneValue<V> fromValue(V value)
    {
        return new SimpleOneValueCI<>(value);
    }

    public static <V extends CoreInstance> OneValue<V> fromValue(V value, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return (fromCoreInstanceFn == null) ? fromValue(value) : new SimpleOneValueF<>(value, fromCoreInstanceFn);
    }

    public static <V> OneValue<V> fromValue(V value, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return new SimpleOneValueTF<>(value, toCoreInstanceFn, fromCoreInstanceFn);
    }

    public static <V extends CoreInstance> OneValue<V> fromSupplier(Supplier<? extends V> supplier)
    {
        return (supplier == null) ? fromValue(null) : new LazyOneValueCI<>(LazyResolver.fromSupplier(supplier));
    }

    public static <V extends CoreInstance> OneValue<V> fromSupplier(Supplier<? extends V> supplier, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return (supplier == null) ?
               fromValue(null, fromCoreInstanceFn) :
               ((fromCoreInstanceFn == null) ? fromSupplier(supplier) : new LazyOneValueF<>(LazyResolver.fromSupplier(supplier), fromCoreInstanceFn));
    }

    public static <V> OneValue<V> fromSupplier(Supplier<? extends V> supplier, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return (supplier == null) ? fromValue(null, toCoreInstanceFn, fromCoreInstanceFn) : new LazyOneValueTF<>(LazyResolver.fromSupplier(supplier), toCoreInstanceFn, fromCoreInstanceFn);
    }

    private abstract static class SimpleOneValue<T> extends OneValue<T>
    {
        private SimpleOneValue(T value)
        {
            this.value = value;
        }

        @Override
        public boolean isFullyResolved()
        {
            return true;
        }
    }

    private static class SimpleOneValueCI<T extends CoreInstance> extends SimpleOneValue<T>
    {
        private SimpleOneValueCI(T value)
        {
            super(value);
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
        public OneValue<T> copy()
        {
            return new SimpleOneValueCI<>(this.value);
        }

        @Override
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @SuppressWarnings("unchecked")
        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return (T) value;
        }
    }

    private static class SimpleOneValueF<T extends CoreInstance> extends SimpleOneValue<T>
    {
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private SimpleOneValueF(T value, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(value);
            this.fromCoreInstanceFn = Objects.requireNonNull(fromCoreInstanceFn);
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
        public OneValue<T> copy()
        {
            return new SimpleOneValueF<>(this.value, this.fromCoreInstanceFn);
        }

        @Override
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return this.fromCoreInstanceFn.apply(value);
        }
    }

    private static class SimpleOneValueTF<T> extends SimpleOneValue<T>
    {
        private final Function<? super T, ? extends CoreInstance> toCoreInstanceFn;
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private SimpleOneValueTF(T value, Function<? super T, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(value);
            this.toCoreInstanceFn = Objects.requireNonNull(toCoreInstanceFn);
            this.fromCoreInstanceFn = Objects.requireNonNull(fromCoreInstanceFn);
        }

        @Override
        public OneValue<T> copy()
        {
            return new SimpleOneValueTF<>(this.value, this.toCoreInstanceFn, this.fromCoreInstanceFn);
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

    private abstract static class LazyOneValue<T> extends OneValue<T>
    {
        private volatile LazyResolver<? extends T> initializer;

        private LazyOneValue(LazyResolver<? extends T> initializer)
        {
            this.initializer = initializer;
        }

        @Override
        public boolean hasValue()
        {
            return (this.initializer != null) || (this.value != null);
        }

        @Override
        public T getValue()
        {
            if (this.initializer != null)
            {
                synchronized (this)
                {
                    LazyResolver<? extends T> local = this.initializer;
                    if (local != null)
                    {
                        T result = this.value = local.get();
                        this.initializer = null;
                        return result;
                    }
                }
            }
            return super.getValue();
        }

        @Override
        public ListIterable<T> getValues()
        {
            LazyResolver<? extends T> local = this.initializer;
            if (local != null)
            {
                if (!local.isResolved())
                {
                    return LazyResolutionLists.newImmutable(local);
                }
                synchronized (this)
                {
                    if (this.initializer != null)
                    {
                        T v = this.value = local.getResolvedValue();
                        this.initializer = null;
                        return Lists.immutable.with(v);
                    }
                }
            }
            return super.getValues();
        }

        @Override
        public void setValue(T newValue)
        {
            if (this.initializer == null)
            {
                this.value = newValue;
            }
            else
            {
                synchronized (this)
                {
                    this.value = newValue;
                    this.initializer = null;
                }
            }
        }

        @Override
        public void addValue(T value)
        {
            if (this.initializer != null)
            {
                throw new IllegalStateException("Cannot add value to to-one property: value already present");
            }
            super.addValue(value);
        }

        @Override
        public boolean removeValue(Object value)
        {
            if ((value != null) && (this.initializer != null))
            {
                synchronized (this)
                {
                    LazyResolver<? extends T> local = this.initializer;
                    if (local != null)
                    {
                        T result = local.get();
                        boolean equal = result.equals(value);
                        if (!equal)
                        {
                            this.value = result;
                        }
                        this.initializer = null;
                        return equal;
                    }
                }
            }
            return super.removeValue(value);
        }

        @Override
        public OneValue<T> copy()
        {
            LazyResolver<? extends T> local = this.initializer;
            if (local != null)
            {
                if (!local.isResolved())
                {
                    return copyFromInitializer(local);
                }
                synchronized (this)
                {
                    if (this.initializer != null)
                    {
                        T v = this.value = local.getResolvedValue();
                        this.initializer = null;
                        return copyFromValue(v);
                    }
                }
            }
            return copyFromValue(this.value);
        }

        @Override
        public boolean isFullyResolved()
        {
            return this.initializer == null;
        }

        abstract OneValue<T> copyFromValue(T value);

        abstract OneValue<T> copyFromInitializer(LazyResolver<? extends T> initializer);
    }

    private static class LazyOneValueCI<T extends CoreInstance> extends LazyOneValue<T>
    {
        private LazyOneValueCI(LazyResolver<? extends T> initializer)
        {
            super(initializer);
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
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @SuppressWarnings("unchecked")
        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return (T) value;
        }

        @Override
        OneValue<T> copyFromValue(T value)
        {
            return new SimpleOneValueCI<>(value);
        }

        @Override
        OneValue<T> copyFromInitializer(LazyResolver<? extends T> initializer)
        {
            return new LazyOneValueCI<>(initializer);
        }
    }

    private static class LazyOneValueF<T extends CoreInstance> extends LazyOneValue<T>
    {
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private LazyOneValueF(LazyResolver<? extends T> initializer, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(initializer);
            this.fromCoreInstanceFn = fromCoreInstanceFn;
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
        CoreInstance toCoreInstance(T value)
        {
            return value;
        }

        @Override
        T fromCoreInstance(CoreInstance value)
        {
            return this.fromCoreInstanceFn.apply(value);
        }

        @Override
        OneValue<T> copyFromValue(T value)
        {
            return new SimpleOneValueF<>(value, this.fromCoreInstanceFn);
        }

        @Override
        OneValue<T> copyFromInitializer(LazyResolver<? extends T> initializer)
        {
            return new LazyOneValueF<>(initializer, this.fromCoreInstanceFn);
        }
    }

    private static class LazyOneValueTF<T> extends LazyOneValue<T>
    {
        private final Function<? super T, ? extends CoreInstance> toCoreInstanceFn;
        private final Function<? super CoreInstance, ? extends T> fromCoreInstanceFn;

        private LazyOneValueTF(LazyResolver<? extends T> initializer, Function<? super T, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends T> fromCoreInstanceFn)
        {
            super(initializer);
            this.toCoreInstanceFn = Objects.requireNonNull(toCoreInstanceFn);
            this.fromCoreInstanceFn = fromCoreInstanceFn;
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

        @Override
        OneValue<T> copyFromValue(T value)
        {
            return new SimpleOneValueTF<>(value, this.toCoreInstanceFn, this.fromCoreInstanceFn);
        }

        @Override
        OneValue<T> copyFromInitializer(LazyResolver<? extends T> initializer)
        {
            return new LazyOneValueTF<>(initializer, this.toCoreInstanceFn, this.fromCoreInstanceFn);
        }
    }
}
