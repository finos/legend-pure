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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.Collection;
import java.util.ListIterator;

public class ListHelper
{
    public static <T> RichIterable<T> tail(RichIterable<T> source)
    {
        return source.isEmpty() ? source : LazyIterate.drop(source, 1);
    }

    public static <T> RichIterable<T> init(RichIterable<T> source)
    {
        int size = source.size();
        return (size == 0) ? source : LazyIterate.take(source, size - 1);
    }

    public static <T> ListIterable<T> subList(ListIterable<T> list, int start, int end)
    {
        int size = list.size();
        if (start < 0)
        {
            throw new IndexOutOfBoundsException("start = " + start);
        }
        if (end > size)
        {
            throw new IndexOutOfBoundsException("end (" + end + ") > size (" + size + ")");
        }
        if (start > end)
        {
            throw new IllegalArgumentException("start (" + start + ") > end (" + end + ')');
        }

        if ((start == 0) && (end == size))
        {
            return list;
        }

        if (list instanceof MutableList)
        {
            return ((MutableList<T>)list).subList(start, end);
        }
        if (list instanceof ImmutableList)
        {
            return ((ImmutableList<T>)list).subList(start, end);
        }

        MutableList<T> subList = FastList.newList(end - start);
        ListIterator<T> iterator = list.listIterator(start);
        while (iterator.nextIndex() < end)
        {
            subList.add(iterator.next());
        }
        return subList;
    }

    public static <T extends Collection<Long>> T boxLongIterable(LongIterable longs, T targetCollection)
    {
        LongIterator iterator = longs.longIterator();
        while (iterator.hasNext())
        {
            targetCollection.add(iterator.next());
        }
        return targetCollection;
    }

    public static <T> ListIterable<T> wrapListIterable(Iterable<T> iterable)
    {
        return (iterable instanceof ListIterable) ? (ListIterable<T>)iterable : CollectionAdapter.wrapList(iterable);
    }
}