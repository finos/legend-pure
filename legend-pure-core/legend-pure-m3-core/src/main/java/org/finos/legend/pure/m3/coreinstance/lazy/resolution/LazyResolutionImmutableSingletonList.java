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

import java.util.Objects;
import java.util.function.Supplier;

class LazyResolutionImmutableSingletonList<T> extends AbstractLazyResolutionImmutableList<T>
{
    private final Object item;

    private LazyResolutionImmutableSingletonList(Object item)
    {
        this.item = item;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    Object getRaw(int index)
    {
        return this.item;
    }

    static <T> LazyResolutionImmutableList<T> newList(Supplier<? extends T> supplier)
    {
        return newList(LazyResolver.fromSupplier(supplier));
    }

    static <T> LazyResolutionImmutableList<T> newList(LazyResolver<? extends T> resolver)
    {
        return new LazyResolutionImmutableSingletonList<>(Objects.requireNonNull(resolver));
    }

    static <T> LazyResolutionImmutableList<T> newListRaw(Object item)
    {
        return new LazyResolutionImmutableSingletonList<>(item);
    }
}
