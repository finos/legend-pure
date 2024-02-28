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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

class IntermediateNode
{
    private final int id;
    private final int classifierId;
    private final String name;
    private final int compileState;
    private ListIterable<String> realKey;
    private final MutableIntObjectMap<MutableIntList> keyValues = IntObjectMaps.mutable.empty();
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
        StringBuilder result = new StringBuilder(64).append(this.id).append(": ").append(this.name).append(" is a ").append(this.classifierId).append('\n');
        this.keyValues.forEachKeyValue((i, mutableIntList) ->
        {
            result.append("   ").append(i).append(" ");
            mutableIntList.appendString(result, "[", ", ", "]");
            result.append("\n");
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
