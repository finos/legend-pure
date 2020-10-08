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
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.SynchronizedRichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceMutableState;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

class SimpleCoreInstanceMutableState extends AbstractCoreInstanceMutableState
{
    private static final Function0<ValueHolder> NEW_VALUE_HOLDER = new Function0<ValueHolder>()
    {
        @Override
        public ValueHolder value()
        {
            return new ValueHolder(null, null, null);
        }
    };

    private final MutableMap<String, ValueHolder> state = UnifiedMap.newMap();

    RichIterable<String> getKeys()
    {
        synchronized (this.state)
        {
            return SynchronizedRichIterable.of(this.state.keysView(), this.state);
        }
    }

    CoreInstance getKeyByName(String name, SimpleCoreInstance owner)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(name);
            if (valueHolder == null)
            {
                throw new RuntimeException("No real key can be found for '" + name + "' in\n" + owner.getName()+" ("+owner+")");
            }
            if (!valueHolder.hasKey())
            {
                if (!valueHolder.hasRealKey())
                {
                    throw new RuntimeException("No real key can be found for '" + name + "' in\n" + owner.getName()+" ("+owner+")");
                }
                valueHolder.setKey(owner.getRepository().resolve(valueHolder.getRealKey()));
                if (!valueHolder.hasKey())
                {
                    throw new RuntimeException("Error " + name + " has no key - " + owner.getName());
                }
            }
            return valueHolder.getKey();
        }
    }

    ImmutableList<String> getRealKeyByName(String keyName)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder == null) ? null : valueHolder.getRealKey();
        }
    }

    boolean hasValuesDefined(String keyName)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder != null) && valueHolder.hasValuesDefined();
        }
    }

    CoreInstance getOneValue(String keyName) throws OneValueException
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder == null) ? null : valueHolder.getOneValue();
        }
    }

    ListIterable<CoreInstance> getValues(String keyName)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder == null) ? null : valueHolder.getValues();
        }
    }

    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex) throws IDConflictException
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder == null) ? null : valueHolder.getValueByIDIndex(indexSpec, keyInIndex);
        }
    }

    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            return (valueHolder == null) ? Lists.immutable.<CoreInstance>empty() : valueHolder.getValuesByIndex(indexSpec, keyInIndex);
        }
    }

    void setValues(ListIterable<String> key, CoreInstance value)
    {
        String name = key.getLast();
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.getIfAbsentPut(name, NEW_VALUE_HOLDER);
            valueHolder.possiblySetRealKey(key);
            valueHolder.setValues(value);
        }
    }

    void setValues(ListIterable<String> key, ListIterable<? extends CoreInstance> values)
    {
        String name = key.getLast();
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.getIfAbsentPut(name, NEW_VALUE_HOLDER);
            valueHolder.possiblySetRealKey(key);
            valueHolder.setValues(values);
        }
    }

    void addValue(ListIterable<String> key, CoreInstance value)
    {
        addValue(key.getLast(), key, value);
    }

    void addValue(String keyName, CoreInstance value)
    {
        addValue(keyName, null, value);
    }

    private void addValue(String keyName, ListIterable<String> key, CoreInstance value)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.getIfAbsentPut(keyName, NEW_VALUE_HOLDER);
            valueHolder.possiblySetRealKey(key);
            valueHolder.addValue(value);
        }
    }

    void addKeyWithNoValues(ListIterable<String> key)
    {
        setValues(key, Lists.immutable.<CoreInstance>empty());
    }

    void modifyValues(String keyName, int offset, CoreInstance value)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            if ((valueHolder == null) || !valueHolder.hasValuesDefined())
            {
                throw new RuntimeException("No values for key: " + keyName);
            }
            valueHolder.setValue(offset, value);
        }
    }

    void removeValue(String keyName, CoreInstance value)
    {
        synchronized (this.state)
        {
            ValueHolder valueHolder = this.state.get(keyName);
            if (valueHolder != null)
            {
                valueHolder.removeValue(value);
            }
        }
    }

    void removeKey(String key)
    {
        synchronized (this.state)
        {
            this.state.remove(key);
        }
    }

    SimpleCoreInstanceMutableState copy()
    {
        final SimpleCoreInstanceMutableState copy = new SimpleCoreInstanceMutableState();
        synchronized (this.state)
        {
            this.state.forEachKeyValue(new Procedure2<String, ValueHolder>()
            {
                @Override
                public void value(String key, ValueHolder value)
                {
                    copy.state.put(key, value.copy());
                }
            });
            copy.setCompileStateBitSet(getCompileStateBitSet());
        }
        return copy;
    }
}
