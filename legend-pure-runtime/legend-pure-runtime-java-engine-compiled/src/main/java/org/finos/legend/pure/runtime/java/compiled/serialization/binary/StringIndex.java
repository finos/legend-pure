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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m4.serialization.Reader;

public abstract class StringIndex extends StringCacheOrIndex
{
    private final String[] classifierIds;

    protected StringIndex(String[] classifierIds)
    {
        this.classifierIds = classifierIds;
    }

    public String getString(int id)
    {
        // id < 0: classifier ids
        if (id < 0)
        {
            int index = classifierIdStringIdToIndex(id);
            try
            {
                return this.classifierIds[index];
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new RuntimeException("Unknown string id: " + id);
            }
        }

        // id > 0: other strings
        if (id > 0)
        {
            int index = otherStringIdToIndex(id);
            try
            {
                return getOtherString(index);
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new RuntimeException("Unknown string id: " + id);
            }
        }

        // id == 0: null string
        return null;
    }

    @Override
    public RichIterable<String> getClassifierIds()
    {
        return ArrayAdapter.adapt(this.classifierIds).asUnmodifiable();
    }

    protected abstract String getOtherString(int index);

    protected static String[] readClassifierIds(Reader reader)
    {
        String[] strings = reader.readStringArray();
        for (int i = 0; i < strings.length; i++)
        {
            strings[i] = strings[i].intern();
        }
        return strings;
    }
}
