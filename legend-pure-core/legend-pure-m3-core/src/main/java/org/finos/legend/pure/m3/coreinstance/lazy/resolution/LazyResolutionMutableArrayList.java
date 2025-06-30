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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Supplier;

class LazyResolutionMutableArrayList<T> extends AbstractLazyResolutionMutableList<T>
{
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private Object[] items;
    private int size;

    private LazyResolutionMutableArrayList(Object[] items, int size)
    {
        this.items = items;
        this.size = size;
    }

    private LazyResolutionMutableArrayList(Object[] items)
    {
        this(items, items.length);
    }

    @Override
    public LazyResolutionMutableArrayList<T> clone()
    {
        LazyResolutionMutableArrayList<T> result = (LazyResolutionMutableArrayList<T>) super.clone();
        if (this.items.length > 0)
        {
            result.items = this.items.clone();
        }
        return result;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public boolean add(T t)
    {
        int newSize = this.size + 1;
        Object[] array = ensureCapacity(newSize);
        array[this.size] = t;
        this.size = newSize;
        return true;
    }

    @Override
    public void add(int index, T element)
    {
        checkBoundsForAdd(index);
        int newSize = this.size + 1;
        Object[] array = ensureCapacity(newSize);
        shiftTailBack(index, 1);
        array[index] = element;
        this.size = newSize;
    }

    @Override
    public T remove(int index)
    {
        checkBounds(index);
        T previous = getResolved(index);
        shiftTailForward(index, 1);
        this.size -= 1;
        return previous;
    }

    @Override
    public void clear()
    {
        int oldSize = this.size;
        this.size = 0;
        Object[] array = this.items;
        for (int i = 0; i < oldSize; i++)
        {
            array[i] = null;
        }
    }

    @Override
    public Object[] toArray()
    {
        int size = this.size;
        Object[] result = new Object[size];
        Object[] array = this.items;
        for (int i = 0; i < size; i++)
        {
            Object item = array[i];
            result[i] = (item instanceof LazyResolver) ? (array[i] = ((LazyResolver<?>) item).get()) : item;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a)
    {
        int size = this.size;
        E[] result = (a.length < size) ?
                     (E[]) Array.newInstance(a.getClass().getComponentType(), size) :
                     a;
        Object[] array = this.items;
        for (int i = 0; i < size; i++)
        {
            Object item = array[i];
            result[i] = (E) ((item instanceof LazyResolver) ? (array[i] = ((LazyResolver<?>) item).get()) : item);
        }
        if (result.length > size)
        {
            result[size] = null;
        }
        return result;
    }

    @Override
    Object getRaw(int index)
    {
        return this.items[index];
    }

    @Override
    int addIterable(int index, Iterable<? extends T> iterable)
    {
        return (iterable instanceof AbstractLazyResolutionListIterable) ?
               addLazyResolutionListIterable(index, (AbstractLazyResolutionListIterable<? extends T>) iterable) :
               addOrdinaryCollection(index, (iterable instanceof Collection) ? (Collection<? extends T>) iterable : Lists.mutable.withAll(iterable));
    }

    private int addLazyResolutionListIterable(int index, AbstractLazyResolutionListIterable<? extends T> list)
    {
        int lSize = list.size();
        if (lSize > 0)
        {
            int newSize = this.size + lSize;
            Object[] array = ensureCapacity(newSize);
            shiftTailBack(index, lSize);
            for (int i = 0; i < lSize; i++)
            {
                array[index + i] = unwrapIfResolved(list.getRaw(i));
            }
            this.size = newSize;
        }
        return lSize;
    }

    private int addOrdinaryCollection(int index, Collection<? extends T> collection)
    {
        int cSize = collection.size();
        if (cSize > 0)
        {
            int newSize = this.size + cSize;
            Object[] array = ensureCapacity(newSize);
            shiftTailBack(index, cSize);
            for (T item : collection)
            {
                array[index++] = item;
            }
            this.size = newSize;
        }
        return cSize;
    }

    @Override
    void setRaw(int index, Object value)
    {
        this.items[index] = value;
    }

    @Override
    void removeRange(int fromIndex, int toIndex)
    {
        int length = toIndex - fromIndex;
        shiftTailForward(toIndex, length);
        this.size -= length;
    }

    @Override
    int removeIf(java.util.function.Predicate<? super T> filter, int start, int end)
    {
        Object[] array = this.items;
        int writeIndex = start;
        for (int i = start; i < end; i++)
        {
            Object item = array[i];
            T value = resolve(item);
            if (!filter.test(value))
            {
                if ((writeIndex != i) || (value != item))
                {
                    array[writeIndex] = value;
                }
                writeIndex++;
            }
        }
        int removedCount = end - writeIndex;
        if (removedCount > 0)
        {
            shiftTailForward(writeIndex, removedCount);
            this.size -= removedCount;
        }
        return removedCount;
    }

    @SuppressWarnings("unchecked")
    @Override
    void sort(int start, int end, Comparator<? super T> comparator)
    {
        Object[] array = this.items;
        for (int i = start; i < end; i++)
        {
            array[i] = resolve(array[i]);
        }
        Arrays.sort((T[]) array, start, end, comparator);
    }

    private Object[] ensureCapacity(int minCapacity)
    {
        Object[] array = this.items;
        int currentLen = array.length;
        if (minCapacity <= currentLen)
        {
            return array;
        }

        int newCapacity = Math.max(minCapacity, computeNewSize(currentLen));
        Object[] newArray = new Object[newCapacity];
        for (int i = 0, end = this.size; i < end; i++)
        {
            newArray[i] = unwrapIfResolved(array[i]);
        }
        return this.items = newArray;
    }

    private int computeNewSize(int currentSize)
    {
        int newSize = currentSize + (currentSize >> 1) + 1;
        return (newSize < currentSize) ?
               MAX_ARRAY_SIZE : // overflow
               newSize;
    }

    private void shiftTailForward(int index, int length)
    {
        int size = this.size;
        Object[] array = this.items;
        for (int i = index + length; i < size; i++)
        {
            array[i - length] = unwrapIfResolved(array[i]);
        }
        for (int i = size - length; i < size; i++)
        {
            array[i] = null;
        }
    }

    private void shiftTailBack(int index, int length)
    {
        Object[] array = this.items;
        for (int i = this.size - 1; i >= index; i--)
        {
            array[i + length] = unwrapIfResolved(array[i]);
        }
    }

    static <T> LazyResolutionMutableList<T> newList(ListIterable<? extends Supplier<? extends T>> suppliers)
    {
        Object[] items = new Object[suppliers.size()];
        suppliers.forEachWithIndex((supplier, i) -> items[i] = LazyResolver.fromSupplier(supplier));
        return newListRaw(items);
    }

    @SafeVarargs
    static <T> LazyResolutionMutableList<T> newList(Supplier<? extends T>... suppliers)
    {
        int size = suppliers.length;
        Object[] items = new Object[size];
        for (int i = 0; i < size; i++)
        {
            items[i] = LazyResolver.fromSupplier(suppliers[i]);
        }
        return newListRaw(items);
    }

    static <T> LazyResolutionMutableList<T> newListRaw(Object... items)
    {
        return new LazyResolutionMutableArrayList<>(items);
    }
}
