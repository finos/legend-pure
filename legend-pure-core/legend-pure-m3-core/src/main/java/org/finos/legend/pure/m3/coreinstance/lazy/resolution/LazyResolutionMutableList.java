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

import org.eclipse.collections.api.list.MutableList;

import java.util.Collections;
import java.util.Comparator;

public interface LazyResolutionMutableList<T> extends LazyResolutionListIterable<T>, MutableList<T>
{
    @Override
    default boolean remove(Object object)
    {
        return remove(object, true) >= 0;
    }

    default int remove(Object object, boolean requireFirst)
    {
        int index = requireFirst ? indexOf(object) : anyIndexOf(object);
        if (index >= 0)
        {
            remove(index);
        }
        return index;
    }

    default boolean removeAny(Object object)
    {
        return remove(object, false) >= 0;
    }

    @Override
    LazyResolutionMutableList<T> subList(int fromIndex, int toIndex);

    @Override
    default int binarySearch(T key, Comparator<? super T> comparator)
    {
        return Collections.binarySearch(this, key, comparator);
    }
}
