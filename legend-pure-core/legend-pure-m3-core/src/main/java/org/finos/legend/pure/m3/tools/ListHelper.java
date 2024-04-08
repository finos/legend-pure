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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.Collection;

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
}
