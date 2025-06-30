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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

public interface PropertyValue<T>
{
    boolean hasValue();

    T getValue();

    CoreInstance getCoreInstanceValue();

    ListIterable<T> getValues();

    ListIterable<? extends CoreInstance> getCoreInstanceValues();

    <K> CoreInstance getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException;

    <K> ListIterable<CoreInstance> getValuesByIndex(IndexSpecification<K> indexSpec, K key);

    void setValues(RichIterable<? extends T> values);

    void setCoreInstanceValues(RichIterable<? extends CoreInstance> values);

    default void setValue(T value)
    {
        setValues((value == null) ? null : Lists.immutable.with(value));
    }

    default void setCoreInstanceValue(CoreInstance value)
    {
        setCoreInstanceValues((value == null) ? null : Lists.immutable.with(value));
    }

    void setValue(int offset, T value);

    void setCoreInstanceValue(int offset, CoreInstance value);

    void addValue(T value);

    void addCoreInstanceValue(CoreInstance value);

    boolean removeValue(Object value);

    default void removeAllValues()
    {
        setValues(null);
    }

    PropertyValue<T> copy();

    boolean isFullyResolved();
}
