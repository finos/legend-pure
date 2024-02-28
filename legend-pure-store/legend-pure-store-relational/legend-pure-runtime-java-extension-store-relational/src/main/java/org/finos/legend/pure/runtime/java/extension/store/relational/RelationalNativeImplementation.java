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

package org.finos.legend.pure.runtime.java.extension.store.relational;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers;

import java.sql.SQLException;
import java.util.Calendar;

public class RelationalNativeImplementation
{
    public static MutableList<Object> processRow(java.sql.ResultSet rs, ListIterable<ResultSetValueHandlers.ResultSetValueHandler> columnTypes, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
    {
        int count = columnTypes.size();
        MutableList<Object> row = FastList.newList(count);
        for (int i = 0; i < count; i++)
        {
            int rsIndex = i + 1;
            row.add(columnTypes.get(i).value(rs, rsIndex, nullSqlInstance, calendar));
        }
        return row;
    }
}
