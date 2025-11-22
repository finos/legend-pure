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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

public class ValueHolder
{
    private CoreInstance key;
    private ImmutableList<String> realKey;
    private Values<CoreInstance> values;

    private ValueHolder(CoreInstance key, ImmutableList<String> realKey, Values<CoreInstance> values)
    {
        this.key = key;
        this.realKey = realKey;
        this.values = values;
    }

    ValueHolder(CoreInstance key, ImmutableList<String> realKey, ListIterable<CoreInstance> values)
    {
        this(key, realKey, newValues(values));
    }

    boolean hasKey()
    {
        return this.key != null;
    }

    CoreInstance getKey()
    {
        return this.key;
    }

    void setKey(CoreInstance key)
    {
        this.key = key;
    }

    void possiblySetKey(CoreInstance key)
    {
        if (this.key != null)
        {
            this.key = key;
        }
    }

    boolean hasRealKey()
    {
        return this.realKey != null;
    }

    ImmutableList<String> getRealKey()
    {
        return this.realKey;
    }

    void setRealKey(ImmutableList<String> realKey)
    {
        this.realKey = realKey;
    }

    void possiblySetRealKey(ListIterable<String> realKey)
    {
        if ((this.realKey == null) && (realKey != null))
        {
            this.realKey = realKey.toImmutable();
        }
    }

    boolean hasValuesDefined()
    {
        return this.values != null;
    }

    CoreInstance getOneValue() throws OneValueException
    {
        return (this.values == null) ? null : this.values.getOneValue();
    }

    ListIterable<CoreInstance> getValues()
    {
        return (this.values == null) ? null : this.values.getValues();
    }

    public <K> CoreInstance getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
    {
        return (this.values == null) ? null : this.values.getValueByIDIndex(indexSpec, key);
    }

    public <K> ListIterable<CoreInstance> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
    {
        return (this.values == null) ? Lists.immutable.empty() : this.values.getValuesByIndex(indexSpec, key);
    }

    void setValues(CoreInstance value)
    {
        this.values = newValues(value);
    }

    void setValues(ListIterable<? extends CoreInstance> values)
    {
        this.values = newValues(values);
    }

    void addValue(CoreInstance value)
    {
        this.values = (this.values == null) ? newValues(value) : this.values.addValue(value);
    }

    void removeValue(CoreInstance value)
    {
        if (this.values != null)
        {
            this.values = this.values.removeValue(value);
        }
    }

    void setValue(int offset, CoreInstance value)
    {
        this.values.setValue(offset, value);
    }

    ValueHolder copy()
    {
        return new ValueHolder(this.key, this.realKey, (this.values == null) ? null : this.values.copy());
    }

    public static <T extends CoreInstance> Values<T> newValues(T value)
    {
        return new SingleValue<>(value);
    }

    public static <T extends CoreInstance> Values<T> newValues(RichIterable<? extends T> values)
    {
        return newValues((ListIterable<? extends T>) values);
    }

    public static <T extends CoreInstance> Values<T> newValues(ListIterable<? extends T> values)
    {
        if (values == null)
        {
            return null;
        }

        int size = values.size();
        if (size == 0)
        {
            return EmptyValues.emptyValues();
        }
        if (size == 1)
        {
            return new SingleValue<>(values.get(0));
        }
        if (size < Values.INDEXING_THRESHOLD)
        {
            return new SmallValues<>(Lists.immutable.withAll(values));
        }
        return new ValuesWithIndexing<>(Lists.mutable.withAll(values));
    }
}
