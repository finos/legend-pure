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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

import java.util.Collection;
import java.util.function.Supplier;

public interface LazyResolutionImmutableList<T> extends LazyResolutionListIterable<T>, ImmutableList<T>
{
    @Override
    LazyResolutionImmutableList<T> subList(int fromIndex, int toIndex);

    @Override
    default LazyResolutionImmutableList<T> tap(Procedure<? super T> procedure)
    {
        each(procedure);
        return this;
    }

    @Override
    default LazyResolutionImmutableList<T> toImmutable()
    {
        return this;
    }

    @Override
    default Collection<T> castToCollection()
    {
        return castToList();
    }

    @Override
    ImmutableList<T> newWithout(Object element);

    ImmutableList<T> newSetting(int index, T element);

    static <T> LazyResolutionImmutableList<T> newList(ListIterable<? extends Supplier<? extends T>> suppliers)
    {
        return (suppliers.size() == 1) ? newList(suppliers.get(0)) : LazyResolutionImmutableArrayList.newList(suppliers);
    }

    @SafeVarargs
    static <T> LazyResolutionImmutableList<T> newList(Supplier<? extends T>... suppliers)
    {
        return (suppliers.length == 1) ? newList(suppliers[0]) : LazyResolutionImmutableArrayList.newList(suppliers);
    }

    static <T> LazyResolutionImmutableList<T> newList(Supplier<? extends T> supplier)
    {
        return LazyResolutionImmutableSingletonList.newList(supplier);
    }

    static <T> LazyResolutionImmutableList<T> newList(LazyResolver<? extends T> resolver)
    {
        return LazyResolutionImmutableSingletonList.newList(resolver);
    }
}
