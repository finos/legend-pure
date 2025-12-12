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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.ParallelListIterable;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.AbstractRichIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.lazy.parallel.list.ListIterableParallelIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.OrderedIterate;
import org.finos.legend.pure.m4.tools.AbstractLazySpliterable;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

abstract class AbstractLazyResolutionListIterable<T> extends AbstractRichIterable<T> implements LazyResolutionListIterable<T>, RandomAccess
{
    @Override
    public int hashCode()
    {
        int hash = 1;
        for (int i = 0, end = size(); i < end; i++)
        {
            hash = (31 * hash) + Objects.hashCode(getResolved(i));
        }
        return hash;
    }

    @Override
    public boolean contains(Object o)
    {
        return anySatisfy((o == null) ? Objects::isNull : o::equals);
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        switch (source.size())
        {
            case 0:
            {
                return true;
            }
            case 1:
            {
                return contains(Iterate.getFirst(source));
            }
            default:
            {
                return containsAllInternalSet(Sets.mutable.withAll(source));
            }
        }
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        if (source instanceof Collection)
        {
            return containsAll((Collection<?>) source);
        }

        MutableSet<Object> set = Sets.mutable.withAll(source);
        switch (set.size())
        {
            case 0:
            {
                return true;
            }
            case 1:
            {
                return contains(set.getAny());
            }
            default:
            {
                return containsAllInternalSet(set);
            }
        }
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        switch (elements.length)
        {
            case 0:
            {
                return true;
            }
            case 1:
            {
                return contains(elements[0]);
            }
            default:
            {
                return containsAllInternalSet(Sets.mutable.with(elements));
            }
        }
    }

    private boolean containsAllInternalSet(MutableSet<Object> set)
    {
        return anySatisfy(item -> set.remove(item) && set.isEmpty());
    }

    @Override
    public T get(int index)
    {
        return getResolved(checkBounds(index));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getAny()
    {
        int size = size();
        if (size == 0)
        {
            return null;
        }
        for (int i = 0; i < size; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<? extends T> resolver = (LazyResolver<? extends T>) item;
                if (resolver.isResolved())
                {
                    return resolver.getResolvedValue();
                }
            }
            else
            {
                return (T) item;
            }
        }
        return getResolved(0);
    }

    @Override
    public T getFirst()
    {
        return isEmpty() ? null : getResolved(0);
    }

    @Override
    public T getLast()
    {
        int size = size();
        return (size == 0) ? null : getResolved(size - 1);
    }

    @Override
    public int indexOf(Object object)
    {
        for (int i = 0, size = size(); i < size; i++)
        {
            if (Objects.equals(getResolved(i), object))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            if (Objects.equals(getResolved(i), o))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Iterator<T> iterator()
    {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator()
    {
        return listIterator(0);
    }

    @Override
    public Spliterator<T> spliterator()
    {
        return new LazyResolutionListSpliterator();
    }

    public Stream<T> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<T> parallelStream()
    {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public MutableStack<T> toStack()
    {
        return Stacks.mutable.withAll(this);
    }

    @Override
    public void forEach(Consumer<? super T> consumer)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            consumer.accept(getResolved(i));
        }
    }

    @Override
    public void each(Procedure<? super T> procedure)
    {
        forEach((Consumer<? super T>) procedure);
    }

    @Override
    public void forEach(int startIndex, int endIndex, Procedure<? super T> procedure)
    {
        ListIterate.rangeCheck(startIndex, endIndex, size());
        if (startIndex <= endIndex)
        {
            for (int i = startIndex; i <= endIndex; i++)
            {
                procedure.value(getResolved(i));
            }
        }
        else
        {
            for (int i = startIndex; i >= endIndex; i--)
            {
                procedure.value(getResolved(i));
            }
        }
    }

    @Override
    public void forEachWithIndex(int fromIndex, int toIndex, ObjectIntProcedure<? super T> procedure)
    {
        ListIterate.rangeCheck(fromIndex, toIndex, size());
        if (fromIndex <= toIndex)
        {
            for (int i = fromIndex; i <= toIndex; i++)
            {
                procedure.value(getResolved(i), i);
            }
        }
        else
        {
            for (int i = fromIndex; i >= toIndex; i--)
            {
                procedure.value(getResolved(i), i);
            }
        }
    }

    @Override
    public T detect(Predicate<? super T> predicate)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            T item = getResolved(i);
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return null;
    }

    @Override
    public Optional<T> detectOptional(Predicate<? super T> predicate)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            T item = getResolved(i);
            if (predicate.accept(item))
            {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public int detectIndex(Predicate<? super T> predicate)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            if (predicate.accept(getResolved(i)))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int detectLastIndex(Predicate<? super T> predicate)
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            if (predicate.accept(getResolved(i)))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean anySatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, true, true, false);
    }

    @Override
    public boolean allSatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, false, false, true);
    }

    @Override
    public boolean noneSatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, true, false, true);
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return anySatisfy(Predicates.bind(predicate, parameter));
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return allSatisfy(Predicates.bind(predicate, parameter));
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return noneSatisfy(Predicates.bind(predicate, parameter));
    }

    @Override
    public <S> boolean corresponds(OrderedIterable<S> other, Predicate2<? super T, ? super S> predicate)
    {
        return OrderedIterate.corresponds(this, other, predicate);
    }

    @Override
    public ParallelListIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        return new ListIterableParallelIterable<>(this, executorService, batchSize);
    }

    @Override
    public ImmutableList<T> toImmutable()
    {
        return newImmutable(0, size());
    }

    @Override
    public MutableList<T> toList()
    {
        return newMutable(0, size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public int detectAnyIndex(java.util.function.Predicate<? super T> predicate)
    {
        int end = size();
        SkipSet skipped = new SkipSet();
        for (int i = 0; i < end; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<? extends T> resolver = (LazyResolver<? extends T>) item;
                if (resolver.isResolved())
                {
                    if (predicate.test(resolver.getResolvedValue()))
                    {
                        return i;
                    }
                }
                else
                {
                    skipped.skip(i);
                }
            }
            else if (predicate.test((T) item))
            {
                return i;
            }
        }
        for (int i = skipped.firstSkipped(); i != -1; i = skipped.nextSkipped(i + 1))
        {
            if (predicate.test(getResolved(i)))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Object[] toArray()
    {
        int size = size();
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = getResolved(i);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a)
    {
        int size = size();
        E[] result = (a.length < size) ?
                     (E[]) Array.newInstance(a.getClass().getComponentType(), size) :
                     a;
        for (int i = 0; i < size; i++)
        {
            result[i] = (E) getResolved(i);
        }
        if (result.length > size)
        {
            result[size] = null;
        }
        return result;
    }

    @Override
    public LazyIterable<T> resolvedOnly()
    {
        return new ResolvedOnlyIterable();
    }

    @Override
    public boolean isAnyUnresolved()
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            if (isUnresolved(getRaw(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUnresolved(int i)
    {
        checkBounds(i);
        return isUnresolved(getRaw(i));
    }

    T getResolved(int index)
    {
        return resolve(getRaw(index));
    }

    abstract Object getRaw(int index);

    boolean isUnresolved(Object item)
    {
        return (item instanceof LazyResolver) && !((LazyResolver<?>) item).isResolved();
    }

    @SuppressWarnings("unchecked")
    T resolve(Object item)
    {
        return (item instanceof LazyResolver) ? ((LazyResolver<? extends T>) item).get() : (T) item;
    }

    Object unwrapIfResolved(Object item)
    {
        if (item instanceof LazyResolver)
        {
            LazyResolver<?> resolver = (LazyResolver<?>) item;
            if (resolver.isResolved())
            {
                return resolver.getResolvedValue();
            }
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    ImmutableList<T> newImmutable(int start, int end)
    {
        int size = end - start;
        Object[] array = new Object[size];
        boolean anyUnresolved = false;
        for (int i = start, j = 0; i < end; i++, j++)
        {
            Object item = getRaw(i);
            LazyResolver<?> resolver;
            if (!(item instanceof LazyResolver))
            {
                array[i] = item;
            }
            else if ((resolver = (LazyResolver<?>) item).isResolved())
            {
                array[j] = resolver.getResolvedValue();
            }
            else
            {
                array[j] = item;
                anyUnresolved = true;
            }
        }
        return anyUnresolved ? LazyResolutionImmutableArrayList.newListRaw(array) : Lists.immutable.with((T[]) array);
    }

    @SuppressWarnings("unchecked")
    MutableList<T> newMutable(int start, int end)
    {
        int size = end - start;
        Object[] array = new Object[size];
        boolean anyUnresolved = false;
        for (int i = start, j = 0; i < end; i++, j++)
        {
            Object item = getRaw(i);
            LazyResolver<?> resolver;
            if (!(item instanceof LazyResolver))
            {
                array[i] = item;
            }
            else if ((resolver = (LazyResolver<?>) item).isResolved())
            {
                array[j] = resolver.getResolvedValue();
            }
            else
            {
                array[j] = item;
                anyUnresolved = true;
            }
        }
        return anyUnresolved ? LazyResolutionMutableArrayList.newListRaw(array) : Lists.mutable.with((T[]) array);
    }

    @SuppressWarnings("unchecked")
    private boolean shortCircuit(Predicate<? super T> predicate, boolean expected, boolean onShortCircuit, boolean atEnd)
    {
        int end = size();
        SkipSet skipped = new SkipSet();
        for (int i = 0; i < end; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<? extends T> resolver = (LazyResolver<? extends T>) item;
                if (resolver.isResolved())
                {
                    T value = resolver.getResolvedValue();
                    if (predicate.accept(value) == expected)
                    {
                        return onShortCircuit;
                    }
                }
                else
                {
                    skipped.skip(i);
                }
            }
            else if (predicate.accept((T) item) == expected)
            {
                return onShortCircuit;
            }
        }
        for (int i = skipped.firstSkipped(); i != -1; i = skipped.nextSkipped(i + 1))
        {
            if (predicate.accept(getResolved(i)) == expected)
            {
                return onShortCircuit;
            }
        }
        return atEnd;
    }

    int checkBounds(int index)
    {
        if ((index < 0) || (index >= size()))
        {
            throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size());
        }
        return index;
    }

    void checkSubListBounds(int fromIndex, int toIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size())
        {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex)
        {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ')');
        }
    }

    @SuppressWarnings("unchecked")
    void forEachResolved(Consumer<? super T> consumer)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                ((LazyResolver<? extends T>) item).ifResolved(consumer);
            }
            else
            {
                consumer.accept((T) item);
            }
        }
    }

    @SuppressWarnings("unchecked")
    T detectResolved(Predicate<? super T> predicate)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            Object item = getRaw(i);
            T value;
            if (item instanceof LazyResolver)
            {
                LazyResolver<? extends T> resolver = (LazyResolver<? extends T>) item;
                if (resolver.isResolved() && predicate.accept(value = resolver.getResolvedValue()))
                {
                    return value;
                }
            }
            else if (predicate.accept(value = (T) item))
            {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    boolean shortCircuitResolved(java.util.function.Predicate<? super T> predicate, boolean expected, boolean onShortCircuit, boolean atEnd)
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            Object item = getRaw(i);
            if (item instanceof LazyResolver)
            {
                LazyResolver<? extends T> resolver = (LazyResolver<? extends T>) item;
                if (resolver.isResolved() && (predicate.test(resolver.getResolvedValue()) == expected))
                {
                    return onShortCircuit;
                }
            }
            else if (predicate.test((T) item) == expected)
            {
                return onShortCircuit;
            }
        }
        return atEnd;
    }

    private class LazyResolutionListSpliterator implements Spliterator<T>
    {
        private int current;
        private final int end;

        private LazyResolutionListSpliterator(int start, int end)
        {
            this.current = start;
            this.end = end;
        }

        private LazyResolutionListSpliterator()
        {
            this(0, size());
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action)
        {
            if (this.current < this.end)
            {
                T item = getResolved(this.current++);
                action.accept(item);
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit()
        {
            int start = this.current;
            int mid = (start + this.end) >>> 1;
            if (mid <= start)
            {
                return null;
            }
            this.current = mid;
            return new LazyResolutionListSpliterator(start, mid);
        }

        @Override
        public long estimateSize()
        {
            return this.end - this.current;
        }

        @Override
        public int characteristics()
        {
            return ORDERED | SIZED | SUBSIZED | ((AbstractLazyResolutionListIterable.this instanceof ImmutableList) ? IMMUTABLE : 0);
        }
    }

    private class ResolvedOnlyIterable extends AbstractLazySpliterable<T>
    {
        @Override
        public void forEach(Consumer<? super T> consumer)
        {
            forEachResolved(consumer);
        }

        @Override
        public boolean contains(Object object)
        {
            return shortCircuit((object == null) ? Objects::isNull : object::equals, true, true, false);
        }

        @Override
        public T detect(Predicate<? super T> predicate)
        {
            return detectResolved(predicate);
        }

        @Override
        public Spliterator<T> spliterator()
        {
            return new ResolvedOnlySpliterator(0, size());
        }

        @Override
        protected boolean shortCircuit(java.util.function.Predicate<? super T> predicate, boolean expected, boolean onShortCircuit, boolean atEnd)
        {
            return shortCircuitResolved(predicate, expected, onShortCircuit, atEnd);
        }
    }

    private class ResolvedOnlySpliterator implements Spliterator<T>
    {
        private int current;
        private final int end;

        private ResolvedOnlySpliterator(int start, int end)
        {
            this.current = start;
            this.end = end;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean tryAdvance(Consumer<? super T> action)
        {
            while (this.current < this.end)
            {
                Object item = getRaw(this.current++);
                if (!(item instanceof LazyResolver))
                {
                    action.accept((T) item);
                    return true;
                }
                else if (((LazyResolver<? extends T>) item).ifResolved(action))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit()
        {
            int start = this.current;
            int mid = (start + this.end) >>> 1;
            if (mid <= start)
            {
                return null;
            }
            this.current = mid;
            return new ResolvedOnlySpliterator(start, mid);
        }

        @Override
        public long estimateSize()
        {
            return this.end - this.current;
        }

        @Override
        public int characteristics()
        {
            return ORDERED | ((AbstractLazyResolutionListIterable.this instanceof ImmutableList) ? IMMUTABLE : 0);
        }
    }

    private static class SkipSet
    {
        private int intervalStart = -1;
        private int intervalEnd = -1;
        private int additionalOffset = -1;
        private BitSet additional = null;

        void skip(int index)
        {
            if (this.intervalStart == -1)
            {
                this.intervalStart = index;
                this.intervalEnd = index + 1;
            }
            else if (this.additional != null)
            {
                this.additional.set(index - this.additionalOffset);
            }
            else if (index == this.intervalEnd)
            {
                this.intervalEnd++;
            }
            else
            {
                this.additionalOffset = index;
                this.additional = new BitSet();
                this.additional.set(0);
            }
        }

        int firstSkipped()
        {
            return this.intervalStart;
        }

        int nextSkipped(int from)
        {
            if (this.intervalStart == -1)
            {
                return -1;
            }
            if (from <= this.intervalStart)
            {
                return this.intervalStart;
            }
            if (from < this.intervalEnd)
            {
                return from;
            }
            if (this.additional != null)
            {
                int offset = this.additionalOffset;
                int next = this.additional.nextSetBit((from < offset) ? 0 : (from - offset));
                if (next >= 0)
                {
                    return next + offset;
                }
            }
            return -1;
        }
    }
}
