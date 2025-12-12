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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.lazy.ReverseIterable;

import java.util.Objects;
import java.util.function.Predicate;

public interface LazyResolutionListIterable<T> extends ListIterable<T>
{
    @Override
    LazyResolutionListIterable<T> subList(int fromIndex, int toIndex);

    @Override
    default ReverseIterable<T> asReversed()
    {
        return ReverseIterable.adapt(this);
    }

    default int anyIndexOf(Object object)
    {
        return detectAnyIndex((object == null) ? Objects::isNull : object::equals);
    }

    int detectAnyIndex(Predicate<? super T> predicate);

    LazyIterable<T> resolvedOnly();

    default boolean isFullyResolved()
    {
        return !isAnyUnresolved();
    }

    default boolean isAnyUnresolved()
    {
        for (int i = 0, end = size(); i < end; i++)
        {
            if (isUnresolved(i))
            {
                return true;
            }
        }
        return false;
    }

    boolean isUnresolved(int i);
}
