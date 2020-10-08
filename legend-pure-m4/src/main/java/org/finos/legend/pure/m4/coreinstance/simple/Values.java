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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

public interface Values<V extends CoreInstance>
{
    int INDEXING_THRESHOLD = 9;
    int INDEXING_TOLERANCE = 2;

    V getOneValue() throws OneValueException;
    
    ListIterable<V> getValues();

    <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException;

    <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key);

    int size();

    Values<V> addValue(V value);

    Values<V> addValues(ListIterable<V> values);

    Values<V> removeValue(V value);

    void setValue(int offset, V value);

    Values<V> copy();
}
