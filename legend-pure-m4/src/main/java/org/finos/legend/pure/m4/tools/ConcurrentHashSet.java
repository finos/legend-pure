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

package org.finos.legend.pure.m4.tools;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.ParallelUnsortedSetIterable;
import org.eclipse.collections.api.set.UnsortedSetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.set.mutable.AbstractMutableSet;
import org.eclipse.collections.impl.set.mutable.SetAdapter;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;

public class ConcurrentHashSet<T> extends AbstractMutableSet<T>
{
    private static final Object PRESENT = new Object();

    private final ConcurrentMutableMap<T, Object> map;
    private final UnsortedSetIterable<T> keySet;

    private ConcurrentHashSet(ConcurrentMutableMap<T, Object> map)
    {
        this.map = map;
        this.keySet = SetAdapter.adapt(this.map.keySet());
    }

    public ConcurrentHashSet()
    {
        this(ConcurrentHashMap.<T, Object>newMap());
    }

    public ConcurrentHashSet(int initialSize)
    {
        this(ConcurrentHashMap.<T, Object>newMap(initialSize));
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) || this.keySet.equals(other);
    }

    @Override
    public int hashCode()
    {
        return this.keySet.hashCode();
    }

    @Override
    public boolean contains(Object object)
    {
        return this.map.containsKey(object);
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        return this.keySet.containsAllIterable(source);
    }

    @Override
    public boolean add(T element)
    {
        return this.map.putIfAbsent(element, PRESENT) == null;
    }

    @Override
    public boolean remove(Object element)
    {
        return this.map.remove(element) == PRESENT;
    }

    @Override
    public MutableSet<T> with(T element)
    {
        add(element);
        return this;
    }

    @Override
    public MutableSet<T> without(T element)
    {
        remove(element);
        return this;
    }

    @Override
    public MutableSet<T> withAll(Iterable<? extends T> elements)
    {
        addAllIterable(elements);
        return this;
    }

    @Override
    public MutableSet<T> withoutAll(Iterable<? extends T> elements)
    {
        removeAllIterable(elements);
        return this;
    }

    @Override
    public ParallelUnsortedSetIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        return this.keySet.asParallel(executorService, batchSize);
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    @Override
    public T getFirst()
    {
        return this.keySet.getFirst();
    }

    @Override
    public T getLast()
    {
        return this.keySet.getLast();
    }

    @Override
    public void each(Procedure<? super T> procedure)
    {
        this.keySet.each(procedure);
    }

    @Override
    public Iterator<T> iterator()
    {
        return this.keySet.iterator();
    }

    @Override
    public ConcurrentHashSet<T> clone()
    {
        return new ConcurrentHashSet<>(ConcurrentHashMap.newMap(this.map));
    }

    public static <NT> ConcurrentHashSet<NT> newSet()
    {
        return new ConcurrentHashSet<>();
    }

    public static <NT> ConcurrentHashSet<NT> newSet(int initialSize)
    {
        return new ConcurrentHashSet<>(initialSize);
    }
}
