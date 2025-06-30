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

import org.eclipse.collections.api.list.ListIterable;

import java.util.function.Supplier;

class LazyResolutionImmutableArrayList<T> extends AbstractLazyResolutionImmutableList<T>
{
    final Object[] items;

    private LazyResolutionImmutableArrayList(Object[] items)
    {
        this.items = items;
    }

    @Override
    public int size()
    {
        return this.items.length;
    }

    @Override
    Object getRaw(int index)
    {
        return this.items[index];
    }

    static <T> LazyResolutionImmutableList<T> newList(ListIterable<? extends Supplier<? extends T>> suppliers)
    {
        Object[] items = new Object[suppliers.size()];
        suppliers.forEachWithIndex((supplier, i) -> items[i] = LazyResolver.fromSupplier(supplier));
        return newListRaw(items);
    }

    @SafeVarargs
    static <T> LazyResolutionImmutableList<T> newList(Supplier<? extends T>... suppliers)
    {
        int size = suppliers.length;
        Object[] items = new Object[size];
        for (int i = 0; i < size; i++)
        {
            items[i] = LazyResolver.fromSupplier(suppliers[i]);
        }
        return newListRaw(items);
    }

    static <T> LazyResolutionImmutableList<T> newListRaw(Object... items)
    {
        return new LazyResolutionImmutableArrayList<>(items);
    }
}
