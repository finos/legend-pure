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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers.ResultSetValueHandler;

import java.sql.SQLException;
import java.util.Calendar;

public class RelationalNativeImplementation
{
    /**
     * Read the current row of a result set.
     *
     * @param rs              result set, positioned on a row
     * @param columnTypes     a handler for each column, in column order
     * @param nullSqlInstance value for a column that is null in this row
     * @return the value of each column, in column order
     * @throws SQLException if the driver cannot read a column
     */
    public static MutableList<Object> processRow(java.sql.ResultSet rs, ListIterable<ResultSetValueHandler> columnTypes, CoreInstance nullSqlInstance) throws SQLException
    {
        int count = columnTypes.size();
        MutableList<Object> row = Lists.mutable.ofInitialCapacity(count);
        for (int i = 0; i < count; i++)
        {
            Object value = columnTypes.get(i).value(rs, i + 1);
            row.add((value == null) ? nullSqlInstance : value);
        }
        return row;
    }

    /**
     * Read the current row of a result set, reading a timestamp column as a wall clock kept in the
     * zone of the calendar, whatever zone the handlers were got for.
     *
     * @param rs              result set, positioned on a row
     * @param columnTypes     a handler for each column, in column order
     * @param nullSqlInstance value for a column that is null in this row
     * @param calendar        calendar in the zone the connection names
     * @return the value of each column, in column order
     * @throws SQLException if the driver cannot read a column
     * @deprecated Use {@link #processRow(java.sql.ResultSet, ListIterable, CoreInstance)}, with
     * handlers got for the zone the connection names. Retained temporarily, for callers that pass a
     * calendar.
     */
    @Deprecated
    public static MutableList<Object> processRow(java.sql.ResultSet rs, ListIterable<ResultSetValueHandler> columnTypes, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
    {
        int count = columnTypes.size();
        MutableList<Object> row = Lists.mutable.ofInitialCapacity(count);
        for (int i = 0; i < count; i++)
        {
            Object value = columnTypes.get(i).value(rs, i + 1, calendar);
            row.add((value == null) ? nullSqlInstance : value);
        }
        return row;
    }
}
