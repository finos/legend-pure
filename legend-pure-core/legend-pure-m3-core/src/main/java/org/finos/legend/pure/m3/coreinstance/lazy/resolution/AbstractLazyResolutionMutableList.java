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
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
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
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.Predicates2;
import org.eclipse.collections.impl.block.factory.PrimitiveFunctions;
import org.eclipse.collections.impl.block.procedure.MutatingAggregationProcedure;
import org.eclipse.collections.impl.block.procedure.NonMutatingAggregationProcedure;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.list.mutable.MutableListIterator;
import org.eclipse.collections.impl.list.mutable.SynchronizedMutableList;
import org.eclipse.collections.impl.list.mutable.UnmodifiableMutableList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.partition.list.PartitionImmutableListImpl;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.internal.RandomAccessListIterate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

abstract class AbstractLazyResolutionMutableList<T> extends AbstractLazyResolutionListIterable<T> implements LazyResolutionMutableList<T>
{
    @Override
    public boolean equals(Object other)
    {
        return (other == this) ||
                ((other instanceof List) && ListIterate.equals(this, (List<?>) other));
    }

    @SuppressWarnings("unchecked")
    @Override
    public LazyResolutionMutableList<T> clone()
    {
        try
        {
            return (LazyResolutionMutableList<T>) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError(e);
        }
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            setRaw(i, operator.apply(resolve(getRaw(i))));
        }
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
        return removeIfWith(Predicates2.in(), collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
        return removeIfWith(Predicates2.notIn(), collection);
    }

    @Override
    public boolean removeIf(java.util.function.Predicate<? super T> predicate)
    {
        return removeIf(predicate, 0, size()) > 0;
    }

    @Override
    public boolean removeIf(Predicate<? super T> predicate)
    {
        return removeIf((java.util.function.Predicate<? super T>) predicate);
    }

    @Override
    public <P> boolean removeIfWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return removeIf(Predicates.bind(predicate, parameter));
    }

    @Override
    public boolean add(T t)
    {
        add(size(), t);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        return addIterable(c) > 0;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        return addIterable(index, c) > 0;
    }

    @Override
    public T set(int index, T element)
    {
        Object previous = getRaw(checkBounds(index));
        setRaw(index, element);
        return resolve(previous);
    }

    @Override
    public void clear()
    {
        removeRange(0, size());
    }

    @Override
    public void sort(Comparator<? super T> c)
    {
        sort(0, size(), c);
    }

    @Override
    public LazyResolutionMutableList<T> subList(int fromIndex, int toIndex)
    {
        checkSubListBounds(fromIndex, toIndex);
        return new LazyResolutionMutableSubList<>(this, fromIndex, toIndex - fromIndex);
    }

    @Override
    public ListIterator<T> listIterator(int index)
    {
        return new MutableListIterator<>(this, checkBounds(index));
    }

    @Override
    public MutableList<T> with(T element)
    {
        add(element);
        return this;
    }

    @Override
    public MutableList<T> without(T element)
    {
        remove(element);
        return this;
    }

    @Override
    public MutableList<T> withAll(Iterable<? extends T> elements)
    {
        addAllIterable(elements);
        return this;
    }

    @Override
    public MutableList<T> withoutAll(Iterable<? extends T> elements)
    {
        removeAllIterable(elements);
        return this;
    }

    @Override
    public MutableList<T> newEmpty()
    {
        return Lists.mutable.empty();
    }

    @Override
    public <P> Twin<MutableList<T>> selectAndRejectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        PartitionMutableList<T> partition = partitionWith(predicate, parameter);
        return Tuples.twin(partition.getSelected(), partition.getRejected());
    }

    @Override
    public PartitionMutableList<T> partition(Predicate<? super T> predicate)
    {
        MutableList<T> selected = Lists.mutable.empty();
        MutableList<T> rejected = Lists.mutable.empty();
        forEach(item -> (predicate.accept(item) ? selected : rejected).add(item));
        return newPartition(selected, rejected);
    }

    @Override
    public <P> PartitionMutableList<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return partition(Predicates.bind(predicate, parameter));
    }

    @Override
    public <S> MutableList<S> selectInstancesOf(Class<S> clazz)
    {
        return RandomAccessListIterate.selectInstancesOf(this, clazz);
    }

    @Override
    public MutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction)
    {
        return collectBoolean(booleanFunction, new BooleanArrayList(size()));
    }

    @Override
    public MutableByteList collectByte(ByteFunction<? super T> byteFunction)
    {
        return collectByte(byteFunction, new ByteArrayList(size()));
    }

    @Override
    public MutableCharList collectChar(CharFunction<? super T> charFunction)
    {
        return collectChar(charFunction, new CharArrayList(size()));
    }

    @Override
    public MutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction)
    {
        return collectDouble(doubleFunction, new DoubleArrayList(size()));
    }

    @Override
    public MutableFloatList collectFloat(FloatFunction<? super T> floatFunction)
    {
        return collectFloat(floatFunction, new FloatArrayList(size()));
    }

    @Override
    public MutableIntList collectInt(IntFunction<? super T> intFunction)
    {
        return collectInt(intFunction, new IntArrayList(size()));
    }

    @Override
    public MutableLongList collectLong(LongFunction<? super T> longFunction)
    {
        return collectLong(longFunction, new LongArrayList(size()));
    }

    @Override
    public MutableShortList collectShort(ShortFunction<? super T> shortFunction)
    {
        return collectShort(shortFunction, new ShortArrayList(size()));
    }

    @Override
    public <IV, P> IV injectIntoWith(IV injectValue, Function3<? super IV, ? super T, ? super P, ? extends IV> function, P parameter)
    {
        return RandomAccessListIterate.injectIntoWith(injectValue, this, function, parameter);
    }

    @Override
    public MutableList<T> distinct()
    {
        return RandomAccessListIterate.distinct(this);
    }

    @Override
    public MutableList<T> distinct(HashingStrategy<? super T> hashingStrategy)
    {
        return RandomAccessListIterate.distinct(this, hashingStrategy);
    }

    @Override
    public <V> MutableList<T> distinctBy(Function<? super T, ? extends V> function)
    {
        return distinct(HashingStrategies.fromFunction(function));
    }

    @Override
    public <V extends Comparable<? super V>> MutableList<T> sortThisBy(Function<? super T, ? extends V> function)
    {
        return sortThis(Comparator.comparing(function));
    }

    @Override
    public MutableList<T> sortThisByInt(IntFunction<? super T> function)
    {
        return sortThis(Functions.toIntComparator(function));
    }

    @Override
    public MutableList<T> sortThisByBoolean(BooleanFunction<? super T> function)
    {
        return sortThis(Functions.toBooleanComparator(function));
    }

    @Override
    public MutableList<T> sortThisByChar(CharFunction<? super T> function)
    {
        return sortThis(Functions.toCharComparator(function));
    }

    @Override
    public MutableList<T> sortThisByByte(ByteFunction<? super T> function)
    {
        return sortThis(Functions.toByteComparator(function));
    }

    @Override
    public MutableList<T> sortThisByShort(ShortFunction<? super T> function)
    {
        return sortThis(Functions.toShortComparator(function));
    }

    @Override
    public MutableList<T> sortThisByFloat(FloatFunction<? super T> function)
    {
        return sortThis(Functions.toFloatComparator(function));
    }

    @Override
    public MutableList<T> sortThisByLong(LongFunction<? super T> function)
    {
        return sortThis(Functions.toLongComparator(function));
    }

    @Override
    public MutableList<T> sortThisByDouble(DoubleFunction<? super T> function)
    {
        return sortThis(Functions.toDoubleComparator(function));
    }

    @Override
    public MutableList<T> asUnmodifiable()
    {
        return UnmodifiableMutableList.of(this);
    }

    @Override
    public MutableList<T> asSynchronized()
    {
        return SynchronizedMutableList.of(this);
    }

    @Override
    public <V> MutableObjectLongMap<V> sumByInt(Function<? super T, ? extends V> groupBy, IntFunction<? super T> function)
    {
        return injectInto(ObjectLongMaps.mutable.empty(), PrimitiveFunctions.sumByIntFunction(groupBy, function));
    }

    @Override
    public <V> MutableObjectDoubleMap<V> sumByFloat(Function<? super T, ? extends V> groupBy, FloatFunction<? super T> function)
    {
        return injectInto(ObjectDoubleMaps.mutable.empty(), PrimitiveFunctions.sumByFloatFunction(groupBy, function));
    }

    @Override
    public <V> MutableObjectLongMap<V> sumByLong(Function<? super T, ? extends V> groupBy, LongFunction<? super T> function)
    {
        return injectInto(ObjectLongMaps.mutable.empty(), PrimitiveFunctions.sumByLongFunction(groupBy, function));
    }

    @Override
    public <V> MutableObjectDoubleMap<V> sumByDouble(Function<? super T, ? extends V> groupBy, DoubleFunction<? super T> function)
    {
        return injectInto(ObjectDoubleMaps.mutable.empty(), PrimitiveFunctions.sumByDoubleFunction(groupBy, function));
    }

    @Override
    public <V> MutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function)
    {
        return RandomAccessListIterate.groupBy(this, function);
    }

    @Override
    public <V> MutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return RandomAccessListIterate.groupByEach(this, function);
    }

    @Override
    public <V> MutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function)
    {
        return RandomAccessListIterate.groupByUniqueKey(this, function);
    }

    @Override
    public <S> MutableList<Pair<T, S>> zip(Iterable<S> that)
    {
        return RandomAccessListIterate.zip(this, that);
    }

    @Override
    public MutableList<Pair<T, Integer>> zipWithIndex()
    {
        return RandomAccessListIterate.zipWithIndex(this);
    }

    @Override
    public boolean addAllIterable(Iterable<? extends T> iterable)
    {
        return addIterable(iterable) > 0;
    }

    @Override
    public boolean removeAllIterable(Iterable<?> iterable)
    {
        return removeAll(CollectionAdapter.wrapSet(iterable));
    }

    @Override
    public boolean retainAllIterable(Iterable<?> iterable)
    {
        return retainAll(CollectionAdapter.wrapSet(iterable));
    }

    @Override
    public <K, V> MutableMap<K, V> aggregateInPlaceBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Procedure2<? super V, ? super T> mutatingAggregator)
    {
        MutableMap<K, V> map = Maps.mutable.empty();
        forEach(new MutatingAggregationProcedure<>(map, groupBy, zeroValueFactory, mutatingAggregator));
        return map;
    }

    @Override
    public <K, V> MutableMap<K, V> aggregateBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Function2<? super V, ? super T, ? extends V> nonMutatingAggregator)
    {
        MutableMap<K, V> map = Maps.mutable.empty();
        forEach(new NonMutatingAggregationProcedure<>(map, groupBy, zeroValueFactory, nonMutatingAggregator));
        return map;
    }

    @Override
    public MutableList<T> take(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be greater than zero, but was: " + count);
        }
        return (count == 0) ? Lists.mutable.empty() : newMutable(0, Math.min(count, size()));
    }

    @Override
    public MutableList<T> takeWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        switch (index)
        {
            case -1:
            {
                return Lists.mutable.withAll(this);
            }
            case 0:
            {
                return Lists.mutable.empty();
            }
            default:
            {
                return newMutable(0, index);
            }
        }
    }

    @Override
    public MutableList<T> drop(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be greater than zero, but was: " + count);
        }
        int size = size();
        return (count >= size) ? Lists.mutable.empty() : newMutable(count, size);
    }

    @Override
    public MutableList<T> dropWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        return (index == -1) ? Lists.mutable.empty() : newMutable(index, size());
    }

    @Override
    public PartitionMutableList<T> partitionWhile(Predicate<? super T> predicate)
    {
        int index = detectIndex(Predicates.not(predicate));
        switch (index)
        {
            case -1:
            {
                return newPartition(newMutable(0, size()), Lists.mutable.empty());
            }
            case 0:
            {
                return newPartition(Lists.mutable.empty(), newMutable(0, size()));
            }
            default:
            {
                return newPartition(newMutable(0, index), newMutable(index, size()));
            }
        }
    }

    @Override
    public RichIterable<RichIterable<T>> chunk(int chunkSize)
    {
        if (chunkSize <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + chunkSize);
        }
        int listSize = size();
        if (listSize == 0)
        {
            return Lists.fixedSize.empty();
        }
        if (chunkSize >= listSize)
        {
            return Lists.fixedSize.with(this);
        }

        MutableList<RichIterable<T>> result = Lists.mutable.ofInitialCapacity((listSize / chunkSize) + 1);
        for (int start = 0; start < listSize; start += chunkSize)
        {
            result.add(newMutable(start, Math.min(start + chunkSize, listSize)));
        }
        return result;
    }

    @Override
    public <V> MutableBag<V> countByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return countByEach(function, Bags.mutable.empty());
    }

    @SuppressWarnings("unchecked")
    @Override
    T getResolved(int index)
    {
        Object item = getRaw(index);
        if (item instanceof LazyResolver)
        {
            T value = ((LazyResolver<? extends T>) item).get();
            setRaw(index, value);
            return value;
        }
        return (T) item;
    }

    int addIterable(Iterable<? extends T> iterable)
    {
        return addIterable(size(), iterable);
    }

    abstract int addIterable(int index, Iterable<? extends T> iterable);

    abstract void setRaw(int index, Object value);

    abstract void removeRange(int fromIndex, int toIndex);

    abstract int removeIf(java.util.function.Predicate<? super T> filter, int start, int end);

    abstract void sort(int start, int end, Comparator<? super T> comparator);

    int checkBoundsForAdd(int index)
    {
        int size = size();
        if ((index < 0) || (index > size))
        {
            throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
        }
        return index;
    }

    static <T> PartitionMutableList<T> newPartition(MutableList<T> selected, MutableList<T> rejected)
    {
        return new PartitionMutableList<T>()
        {
            @Override
            public MutableList<T> getSelected()
            {
                return selected;
            }

            @Override
            public MutableList<T> getRejected()
            {
                return rejected;
            }

            @Override
            public PartitionImmutableList<T> toImmutable()
            {
                return new PartitionImmutableListImpl<>(selected.toImmutable(), rejected.toImmutable());
            }
        };
    }

    static class LazyResolutionMutableSubList<T> extends AbstractLazyResolutionMutableList<T>
    {
        private final AbstractLazyResolutionMutableList<T> source;
        private final int offset;
        private int size;

        LazyResolutionMutableSubList(AbstractLazyResolutionMutableList<T> source, int offset, int size)
        {
            this.source = source;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public void add(int index, T element)
        {
            this.source.add(checkBoundsForAdd(index) + this.offset, element);
            this.size++;
        }

        @Override
        public T remove(int index)
        {
            T result = this.source.remove(checkBounds(index) + this.offset);
            this.size--;
            return result;
        }

        @Override
        T getResolved(int index)
        {
            return this.source.getResolved(index + this.offset);
        }

        @Override
        Object getRaw(int index)
        {
            return this.source.getRaw(index + this.offset);
        }

        @Override
        void setRaw(int index, Object value)
        {
            this.source.setRaw(index + this.offset, value);
        }

        @Override
        int addIterable(int index, Iterable<? extends T> iterable)
        {
            int count = this.source.addIterable(index + this.offset, iterable);
            this.size += count;
            return count;
        }

        @Override
        void removeRange(int fromIndex, int toIndex)
        {
            this.source.removeRange(fromIndex + this.offset, toIndex + this.offset);
            this.size -= (toIndex - fromIndex);
        }

        @Override
        int removeIf(java.util.function.Predicate<? super T> filter, int start, int end)
        {
            int removedCount = this.source.removeIf(filter, start + this.offset, end + this.offset);
            this.size -= removedCount;
            return removedCount;
        }

        @Override
        void sort(int start, int end, Comparator<? super T> comparator)
        {
            this.source.sort(start + this.offset, end + this.offset, comparator);
        }

        @Override
        ImmutableList<T> newImmutable(int start, int end)
        {
            return this.source.newImmutable(start + this.offset, end + this.offset);
        }

        @Override
        MutableList<T> newMutable(int start, int end)
        {
            return this.source.newMutable(start + this.offset, end + this.offset);
        }
    }
}
