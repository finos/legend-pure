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

import org.finos.legend.pure.m4.serialization.Reader;

class EagerStringIndex extends StringIndex
{
    private final String[] otherStrings;

    private EagerStringIndex(String[] classifierIds, String[] otherStrings)
    {
        super(classifierIds);
        this.otherStrings = otherStrings;
    }

    @Override
    protected String getOtherString(int index)
    {
        return this.otherStrings[index];
    }

    static EagerStringIndex fromReader(Reader reader)
    {
        String[] classifierIds = readClassifierIds(reader);
        String[] otherStrings = reader.readStringArray();
        return new EagerStringIndex(classifierIds, otherStrings);
    }
}
