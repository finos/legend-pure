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

package org.finos.legend.pure.m3.coreinstance.lazy.resolution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.ImmutableShortList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.PrimitiveFunctions;
import org.eclipse.collections.impl.block.procedure.MutatingAggregationProcedure;
import org.eclipse.collections.impl.block.procedure.NonMutatingAggregationProcedure;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.list.immutable.ImmutableListIterator;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.partition.list.PartitionImmutableListImpl;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.internal.RandomAccessListIterate;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

abstract class AbstractLazyResolutionImmutableList<T> extends AbstractLazyResolutionListIterable<T> implements LazyResolutionImmutableList<T>, List<T>
{
    @Override
    public boolean equals(Object other)
    {
        return (other == this) ||
                ((other instanceof List) && ListIterate.equals(this, (List<?>) other));
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public boolean add(T t)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public void add(int index, T element)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public boolean addAll(Collection<? extends T> collection)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public T set(int index, T element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public T remove(int index)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator)
    {
        throw new UnsupportedOperationException("Cannot call replaceAll() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public void sort(Comparator<? super T> c)
    {
        throw new UnsupportedOperationException("Cannot call sort() on " + LazyResolutionImmutableList.class.getSimpleName());
    }

    @Override
    public LazyResolutionImmutableSubList<T> subList(int fromIndex, int toIndex)
    {
        checkSubListBounds(fromIndex, toIndex);
        return new LazyResolutionImmutableSubList<>(this, fromIndex, toIndex - fromIndex);
    }

    @Override
    public ImmutableList<T> select(Predicate<? super T> predicate)
    {
        return select(predicate, Lists.mutable.empty()).toImmutable();
    }

    @Override
    public <P> ImmutableList<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return select(Predicates.bind(predicate, parameter));
    }

    @Override
    public ImmutableList<T> reject(Predicate<? super T> predicate)
    {
        return reject(predicate, Lists.mutable.empty()).toImmutable();
    }

    @Override
    public <P> ImmutableList<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return reject(Predicates.bind(predicate, parameter));
    }

    @Override
    public PartitionImmutableList<T> partition(Predicate<? super T> predicate)
    {
        return RandomAccessListIterate.partition(this, predicate).toImmutable();
    }

    @Override
    public <P> PartitionImmutableList<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return RandomAccessListIterate.partitionWith(this, predicate, parameter).toImmutable();
    }

    @Override
    public <S> ImmutableList<S> selectInstancesOf(Class<S> clazz)
    {
        return RandomAccessListIterate.selectInstancesOf(this, clazz).toImmutable();
    }

    @Override
    public <V> ImmutableList<V> collect(Function<? super T, ? extends V> function)
    {
        return collect(function, Lists.mutable.<V>ofInitialCapacity(size())).toImmutable();
    }

    @Override
    public ImmutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction)
    {
        return collectBoolean(booleanFunction, new BooleanArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableByteList collectByte(ByteFunction<? super T> byteFunction)
    {
        return collectByte(byteFunction, new ByteArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableCharList collectChar(CharFunction<? super T> charFunction)
    {
        return collectChar(charFunction, new CharArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction)
    {
        return collectDouble(doubleFunction, new DoubleArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableFloatList collectFloat(FloatFunction<? super T> floatFunction)
    {
        return collectFloat(floatFunction, new FloatArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableIntList collectInt(IntFunction<? super T> intFunction)
    {
        return collectInt(intFunction, new IntArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableLongList collectLong(LongFunction<? super T> longFunction)
    {
        return collectLong(longFunction, new LongArrayList(size())).toImmutable();
    }

    @Override
    public ImmutableShortList collectShort(ShortFunction<? super T> shortFunction)
    {
        return collectShort(shortFunction, new ShortArrayList(size())).toImmutable();
    }

    @Override
    public <P, V> ImmutableList<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter)
    {
        return collect(Functions.bind(function, parameter), Lists.mutable.<V>ofInitialCapacity(size())).toImmutable();
    }

    @Override
    public <V> ImmutableList<V> collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function)
    {
        return collectIf(predicate, function, Lists.mutable.<V>empty()).toImmutable();
    }

    @Override
    public <V> ImmutableList<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
    {
        return flatCollect(function, Lists.mutable.empty()).toImmutable();
    }

    @Override
    public <V> ImmutableObjectLongMap<V> sumByInt(Function<? super T, ? extends V> groupBy, IntFunction<? super T> function)
    {
        return injectInto(ObjectLongMaps.mutable.<V>empty(), PrimitiveFunctions.sumByIntFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V> ImmutableObjectDoubleMap<V> sumByFloat(Function<? super T, ? extends V> groupBy, FloatFunction<? super T> function)
    {
        return injectInto(ObjectDoubleMaps.mutable.<V>empty(), PrimitiveFunctions.sumByFloatFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V> ImmutableObjectLongMap<V> sumByLong(Function<? super T, ? extends V> groupBy, LongFunction<? super T> function)
    {
        return injectInto(ObjectLongMaps.mutable.<V>empty(), PrimitiveFunctions.sumByLongFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V> ImmutableObjectDoubleMap<V> sumByDouble(Function<? super T, ? extends V> groupBy, DoubleFunction<? super T> function)
    {
        return injectInto(ObjectDoubleMaps.mutable.<V>empty(), PrimitiveFunctions.sumByDoubleFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V> ImmutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function)
    {
        return groupBy(function, Multimaps.mutable.list.<V, T>empty()).toImmutable();
    }

    @Override
    public <V> ImmutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return groupByEach(function, Multimaps.mutable.list.empty()).toImmutable();
    }

    @Override
    public <V> ImmutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function)
    {
        return groupByUniqueKey(function, Maps.mutable.<V, T>ofInitialCapacity(size())).toImmutable();
    }

    @Override
    public ImmutableList<T> distinct()
    {
        return RandomAccessListIterate.distinct(this).toImmutable();
    }

    @Override
    public ImmutableList<T> distinct(HashingStrategy<? super T> hashingStrategy)
    {
        return RandomAccessListIterate.distinct(this, hashingStrategy).toImmutable();
    }

    @Override
    public <V> ImmutableList<T> distinctBy(Function<? super T, ? extends V> function)
    {
        return distinct(HashingStrategies.fromFunction(function));
    }

    @Override
    public <S> ImmutableList<Pair<T, S>> zip(Iterable<S> that)
    {
        return RandomAccessListIterate.zip(this, that).toImmutable();
    }

    @Override
    public ImmutableList<Pair<T, Integer>> zipWithIndex()
    {
        return RandomAccessListIterate.zipWithIndex(this).toImmutable();
    }

    @Override
    public <K, V> ImmutableMap<K, V> aggregateInPlaceBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Procedure2<? super V, ? super T> mutatingAggregator)
    {
        MutableMap<K, V> map = Maps.mutable.empty();
        forEach(new MutatingAggregationProcedure<>(map, groupBy, zeroValueFactory, mutatingAggregator));
        return map.toImmutable();
    }

    @Override
    public <K, V> ImmutableMap<K, V> aggregateBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Function2<? super V, ? super T, ? extends V> nonMutatingAggregator)
    {
        MutableMap<K, V> map = Maps.mutable.empty();
        forEach(new NonMutatingAggregationProcedure<>(map, groupBy, zeroValueFactory, nonMutatingAggregator));
        return map.toImmutable();
    }

    @Override
    public RichIterable<RichIterable<T>> chunk(int chunkSize)
    {
        if (chunkSize <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + chunkSize);
        }
        if (isEmpty())
        {
            return Lists.immutable.empty();
        }
        int listSize = size();
        if (chunkSize >= listSize)
        {
            return Lists.immutable.with(this);
        }

        MutableList<RichIterable<T>> result = Lists.mutable.ofInitialCapacity((listSize / chunkSize) + 1);
        for (int start = 0; start < listSize; start += chunkSize)
        {
            result.add(new LazyResolutionImmutableSubList<>(this, start, Math.min(start + chunkSize, listSize)));
        }
        return result.toImmutable();
    }

    @Override
    public ImmutableList<T> take(int count)
    {
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        if (count >= size())
        {
            return this;
        }
        return subList(0, count);
    }

    @Override
    public ImmutableList<T> takeWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        switch (index)
        {
            case -1:
            {
                return this;
            }
            case 0:
            {
                return Lists.immutable.empty();
            }
            default:
            {
                return subList(0, index);
            }
        }
    }

    @Override
    public ImmutableList<T> drop(int count)
    {
        if (count == 0)
        {
            return this;
        }
        if (count >= size())
        {
            return Lists.immutable.empty();
        }
        return subList(count, size());
    }

    @Override
    public ImmutableList<T> dropWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        switch (index)
        {
            case -1:
            {
                return Lists.immutable.empty();
            }
            case 0:
            {
                return this;
            }
            default:
            {
                return subList(index, size());
            }
        }
    }

    @Override
    public PartitionImmutableList<T> partitionWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        switch (index)
        {
            case -1:
            {
                return new PartitionImmutableListImpl<>(this, Lists.immutable.empty());
            }
            case 0:
            {
                return new PartitionImmutableListImpl<>(Lists.immutable.empty(), this);
            }
            default:
            {
                return new PartitionImmutableListImpl<>(subList(0, index), subList(index, size()));
            }
        }
    }

    @Override
    public <V> ImmutableBag<V> countByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return countByEach(function, Bags.mutable.empty()).toImmutable();
    }

    @Override
    public List<T> castToList()
    {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableList<T> toReversed()
    {
        int size = size();
        Object[] reversed = new Object[size];
        boolean anyUnresolved = false;
        for (int i = 0; i < size; i++)
        {
            int targetIndex = size - i - 1;
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<?> supplier = (LazyResolver<?>) item;
                if (supplier.isResolved())
                {
                    reversed[targetIndex] = supplier.getResolvedValue();
                }
                else
                {
                    anyUnresolved = true;
                    reversed[targetIndex] = item;
                }
            }
            else
            {
                reversed[targetIndex] = item;
            }
        }
        return anyUnresolved ?
               newList(reversed) :
               Lists.immutable.with((T[]) reversed);
    }

    @Override
    public LazyResolutionImmutableList<T> toImmutable()
    {
        return this;
    }

    @Override
    public int binarySearch(T key, Comparator<? super T> comparator)
    {
        return Collections.binarySearch(this, key, comparator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableList<T> newWith(T element)
    {
        int size = size();
        Object[] newItems = new Object[size + 1];
        boolean anyUnresolved = false;
        for (int i = 0; i < size; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<?> supplier = (LazyResolver<?>) item;
                if (supplier.isResolved())
                {
                    newItems[i] = supplier.getResolvedValue();
                }
                else
                {
                    anyUnresolved = true;
                    newItems[i] = item;
                }
            }
            else
            {
                newItems[i] = item;
            }
        }
        newItems[size] = element;
        return anyUnresolved ?
               newList(newItems) :
               Lists.immutable.with((T[]) newItems);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableList<T> newWithout(Object element)
    {
        int index = anyIndexOf(element);
        if (index == -1)
        {
            return this;
        }

        int size = size();
        Object[] newItems = new Object[size - 1];
        boolean anyUnresolved = false;
        for (int i = 0, j = 0; i < size; i++, j++)
        {
            if (i == index)
            {
                j--;
                continue;
            }
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<?> supplier = (LazyResolver<?>) item;
                if (supplier.isResolved())
                {
                    newItems[j] = supplier.getResolvedValue();
                }
                else
                {
                    anyUnresolved = true;
                    newItems[j] = item;
                }
            }
            else
            {
                newItems[j] = item;
            }
        }
        return anyUnresolved ?
               newList(newItems) :
               Lists.immutable.with((T[]) newItems);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableList<T> newWithAll(Iterable<? extends T> elements)
    {
        int oldSize = size();
        int newSize = oldSize + Iterate.sizeOf(elements);
        if (newSize == oldSize)
        {
            return this;
        }

        Object[] newItems = new Object[newSize];
        boolean anyUnresolved = false;
        int i = 0;
        while (i < oldSize)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<?> supplier = (LazyResolver<?>) item;
                if (supplier.isResolved())
                {
                    newItems[i] = supplier.getResolvedValue();
                }
                else
                {
                    anyUnresolved = true;
                    newItems[i] = item;
                }
            }
            else
            {
                newItems[i] = item;
            }
            i++;
        }
        for (T newElement : elements)
        {
            newItems[i++] = newElement;
        }
        return anyUnresolved ?
               newList(newItems) :
               Lists.immutable.with((T[]) newItems);
    }

    @Override
    public ImmutableList<T> newWithoutAll(Iterable<? extends T> elements)
    {
        MutableSet<? extends T> toRemove = CollectionAdapter.wrapSet(elements);
        MutableList<T> copy = null;
        for (int i = 0, size = size(); i < size; i++)
        {
            T item = getResolved(i);
            if (toRemove.contains(item))
            {
                if (copy == null)
                {
                    if (i > 0)
                    {
                        copy = Lists.mutable.ofInitialCapacity(i);
                        for (int j = 0; j < i; j++)
                        {
                            copy.add(getResolved(j));
                        }
                    }
                    else
                    {
                        copy = Lists.mutable.empty();
                    }
                }
            }
            else if (copy != null)
            {
                copy.add(item);
            }
        }
        return (copy == null) ? this : copy.toImmutable();
    }

    @Override
    public ListIterator<T> listIterator(int index)
    {
        return new ImmutableListIterator<>(this, index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableList<T> newSetting(int index, T element)
    {
        checkBounds(index);
        int size = size();
        Object[] newItems = new Object[size];
        boolean anyUnresolved = false;
        for (int i = 0; i < size; i++)
        {
            if (i == index)
            {
                newItems[i] = element;
            }
            else
            {
                Object item = getRaw(i);
                if (item instanceof LazyResolver)
                {
                    LazyResolver<?> supplier = (LazyResolver<?>) item;
                    if (supplier.isResolved())
                    {
                        newItems[i] = supplier.getResolvedValue();
                    }
                    else
                    {
                        anyUnresolved = true;
                        newItems[i] = item;
                    }
                }
                else
                {
                    newItems[i] = item;
                }
            }
        }
        return anyUnresolved ?
               newList(newItems) :
               Lists.immutable.with((T[]) newItems);
    }

    @Override
    ImmutableList<T> newImmutable(int start, int end)
    {
        return ((start == 0) && (end == size())) ? this : subList(start, end);
    }

    static class LazyResolutionImmutableSubList<T> extends AbstractLazyResolutionImmutableList<T>
    {
        private final AbstractLazyResolutionImmutableList<T> original;
        private final int offset;
        private final int size;

        private LazyResolutionImmutableSubList(AbstractLazyResolutionImmutableList<T> original, int offset, int size)
        {
            this.original = original;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public LazyResolutionImmutableSubList<T> subList(int fromIndex, int toIndex)
        {
            checkSubListBounds(fromIndex, toIndex);
            return new LazyResolutionImmutableSubList<>(this.original, fromIndex + this.offset, toIndex - fromIndex);
        }

        @Override
        Object getRaw(int index)
        {
            return this.original.getRaw(index + this.offset);
        }
    }

    private static <T> ImmutableList<T> newList(Object... items)
    {
        switch (items.length)
        {
            case 0:
            {
                return Lists.immutable.empty();
            }
            case 1:
            {
                return LazyResolutionImmutableSingletonList.newListRaw(items[0]);
            }
            default:
            {
                return LazyResolutionImmutableArrayList.newListRaw(items);
            }
        }
    }
}
