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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public class ListHelper
{
    public static <T> RichIterable<T> tail(RichIterable<T> source)
    {
        if (source.isEmpty())
        {
            return source;
        }
        if (source instanceof ListIterable)
        {
            int size = source.size();
            return (size == 1) ? Lists.immutable.empty() : ((ListIterable<T>) source).subList(1, size);
        }
        return LazyIterate.drop(source, 1);
    }

    public static <T> RichIterable<T> init(RichIterable<T> source)
    {
        int size = source.size();
        switch (size)
        {
            case 0:
            {
                return source;
            }
            case 1:
            {
                return Lists.immutable.empty();
            }
            default:
            {
                return (source instanceof ListIterable) ? ((ListIterable<T>) source).subList(0, size - 1) : LazyIterate.take(source, size - 1);
            }
        }
    }

    @Deprecated
    public static <T> ListIterable<T> subList(ListIterable<T> list, int start, int end)
    {
        return list.subList(start, end);
    }

    public static <T extends Collection<Long>> T boxLongIterable(LongIterable longs, T targetCollection)
    {
        longs.forEach(targetCollection::add);
        return targetCollection;
    }

    public static <T> ListIterable<T> wrapListIterable(Iterable<T> iterable)
    {
        return (iterable instanceof ListIterable) ? (ListIterable<T>) iterable : CollectionAdapter.wrapList(iterable);
    }


    /**
     * Sort the given list and remove duplicates. The list is sorted using natural ordering and duplicates are
     * removed using {@link Objects#equals(Object, Object)}. Sorting and duplicate removal is done in place, and the
     * same list is returned.
     *
     * @param list       list to sort and remove duplicates from
     * @param <T>        element type
     * @param <L>        list type
     * @return the given list, sorted and with duplicates removed
     */
    public static <T extends Comparable<? super T>, L extends List<T>> L sortAndRemoveDuplicates(L list)
    {
        return sortAndRemoveDuplicates(list, null, null);
    }

    /**
     * Sort the given list and remove duplicates. The list is sorted and duplicates are removed in place, and the same
     * list instance is returned. Generally, the comparator should be consistent with the equality test (i.e.,
     * {@code comparator.compare(a, b) == 0} if and only if {@code equals.test(a, b)}). This is not strictly required,
     * but the result can be strange if they are not consistent. If the comparator is null, the natural ordering is
     * used. If the equality test is null, then {@link Objects#equals(Object, Object)} is used.
     *
     * @param list       list to sort and remove duplicates from
     * @param comparator comparator to use for sorting (if null, the natural order is used)
     * @param equals     equality test to use for determining duplicates (if null, {@link Objects#equals(Object, Object)} is used)
     * @param <T>        element type
     * @param <L>        list type
     * @return the given list, sorted and with duplicates removed
     */
    @SuppressWarnings("unchecked")
    public static <T, L extends List<T>> L sortAndRemoveDuplicates(L list, Comparator<? super T> comparator, BiPredicate<? super T, ? super T> equals)
    {
        if (list.size() > 1)
        {
            list.sort(comparator);

            class ShouldRemove implements Predicate<T>
            {
                private boolean first = true;
                private T prev = null;

                @Override
                public boolean accept(T item)
                {
                    if (this.first)
                    {
                        this.first = false;
                        this.prev = item;
                        return false;
                    }

                    if ((equals == null) ? Objects.equals(this.prev, item) : equals.test(this.prev, item))
                    {
                        return true;
                    }

                    this.prev = item;
                    return false;
                }
            }

            if (list instanceof MutableCollection)
            {
                // optimized for Eclipse Collections classes
                ((MutableCollection<T>) list).removeIf(new ShouldRemove());
            }
            else
            {
                list.removeIf(new ShouldRemove());
            }
        }
        return list;
    }
}
