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

package org.finos.legend.pure.m4.coreinstance.simple;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

final class EmptyValues<T extends CoreInstance> implements Values<T>
{
    private static final EmptyValues<? extends CoreInstance> EMPTY_VALUES = new EmptyValues<>();

    private EmptyValues()
    {
        // Singleton
    }

    @Override
    public T getOneValue()
    {
        return null;
    }

    @Override
    public ListIterable<T> getValues()
    {
        return Lists.immutable.empty();
    }

    @Override
    public <K> T getValueByIDIndex(IndexSpecification<K> indexSpec, K key)
    {
        return null;
    }

    @Override
    public <K> ListIterable<T> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        return Lists.immutable.empty();
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public Values<T> addValue(T value)
    {
        return new SingleValue<>(value);
    }

    @Override
    public Values<T> addValues(ListIterable<T> values)
    {
        return ValueHolder.newValues(values);
    }

    @Override
    public Values<T> removeValue(T value)
    {
        return this;
    }

    @Override
    public void setValue(int offset, T value)
    {
        throw new IndexOutOfBoundsException("Index: " + offset + " Size: 0");
    }

    @Override
    public Values<T> copy()
    {
        return this;
    }

    @SuppressWarnings("unchecked")
    static <T extends CoreInstance> Values<T> emptyValues()
    {
        return (Values<T>) EMPTY_VALUES;
    }
}
