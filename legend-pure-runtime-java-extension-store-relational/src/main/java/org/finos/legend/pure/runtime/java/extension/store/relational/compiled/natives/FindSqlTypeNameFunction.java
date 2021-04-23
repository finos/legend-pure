// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.JavaSqlTypeNames;

public class FindSqlTypeNameFunction implements Function<ListIterable<Object>, String>
{
    @Override
    public String valueOf(ListIterable<Object> values)
    {
        int dataTypeInt = ((Long)values.get(4)).intValue();
        String typeName = JavaSqlTypeNames.SqlTypeNames.get(dataTypeInt);
        if (typeName == null)
        {
            throw new RuntimeException("No compatible SQL type found (java.sql.Types): " + dataTypeInt);
        }
        return typeName;
    }
}
