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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.Supplier;

public class LazyResolutionLists
{
    private LazyResolutionLists()
    {
    }

    public static <T> MutableList<T> newMutable(ListIterable<? extends Supplier<? extends T>> suppliers)
    {
        return suppliers.isEmpty() ? Lists.mutable.empty() : LazyResolutionMutableArrayList.newList(suppliers);
    }

    @SafeVarargs
    public static <T> MutableList<T> newMutable(Supplier<? extends T>... suppliers)
    {
        return (suppliers.length == 0) ? Lists.mutable.empty() : LazyResolutionMutableArrayList.newList(suppliers);
    }

    public static <T> ImmutableList<T> newImmutable(ListIterable<? extends Supplier<? extends T>> suppliers)
    {
        switch (suppliers.size())
        {
            case 0:
            {
                return Lists.immutable.empty();
            }
            case 1:
            {
                return LazyResolutionImmutableSingletonList.newList(suppliers.get(0));
            }
            default:
            {
                return LazyResolutionImmutableArrayList.newList(suppliers);
            }
        }
    }

    @SafeVarargs
    public static <T> ImmutableList<T> newImmutable(Supplier<? extends T>... suppliers)
    {
        switch (suppliers.length)
        {
            case 0:
            {
                return Lists.immutable.empty();
            }
            case 1:
            {
                return LazyResolutionImmutableSingletonList.newList(suppliers[0]);
            }
            default:
            {
                return LazyResolutionImmutableArrayList.newList(suppliers);
            }
        }
    }

    public static <T> ImmutableList<T> newImmutable(Supplier<? extends T> supplier)
    {
        return LazyResolutionImmutableSingletonList.newList(supplier);
    }
}
