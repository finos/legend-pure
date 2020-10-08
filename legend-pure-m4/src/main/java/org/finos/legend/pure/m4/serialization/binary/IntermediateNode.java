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

package org.finos.legend.pure.m4.serialization.binary;

import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

class IntermediateNode
{
    private final int id;
    private final int classifierId;
    private final String name;
    private final int compileState;
    private ListIterable<String> realKey;
    private final MutableIntObjectMap<MutableIntList> keyValues = IntObjectHashMap.newMap();
    private final SourceInformation sourceInformation;

    public IntermediateNode(int id, int classifierId, String name, int compileState, SourceInformation sourceInformation)
    {
        this.id = id;
        this.classifierId = classifierId;
        this.name = name;
        this.compileState = compileState;
        this.sourceInformation = sourceInformation;
    }

    public void put(int id, MutableIntList values)
    {
        this.keyValues.put(id, values);
    }

    @Override
    public String toString()
    {
        final StringBuilder result = new StringBuilder(64);
        result.append(this.id);
        result.append(": ");
        result.append(this.name);
        result.append(" is a ");
        result.append(this.classifierId);
        result.append('\n');
        this.keyValues.forEachKeyValue(new IntObjectProcedure<MutableIntList>()
        {
            @Override
            public void value(int i, MutableIntList mutableIntList)
            {
                result.append("   ");
                result.append(i);
                result.append(" ");
                mutableIntList.appendString(result, "[", ", ", "]");
                result.append("\n");
            }
        });
        return result.toString();
    }

    public int getId()
    {
        return this.id;
    }

    public int getClassifierId()
    {
        return this.classifierId;
    }

    public String getName()
    {
        return this.name;
    }

    public int getCompileState()
    {
        return this.compileState;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public ListIterable<String> getRealKey()
    {
        return this.realKey;
    }

    public MutableIntObjectMap<MutableIntList> getKeyValues()
    {
        return this.keyValues;
    }

    public void setRealKey(ListIterable<String> realKey)
    {
        this.realKey = realKey;
    }
}
