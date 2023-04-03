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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tools.BinaryUtils;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

public final class ResultSetValueHandlers
{
    private static final ResultSetValueHandler NULL = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            return nullSqlInstance;
        }
    };

    private static final ResultSetValueHandler STRING = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            String str = rs.getString(i);
            return str == null ? nullSqlInstance : str;
        }
    };

    private static final ResultSetValueHandler BOOLEAN = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            boolean bool = rs.getBoolean(i);
            return rs.wasNull() ? nullSqlInstance : bool;
        }
    };

    private static final ResultSetValueHandler DATE = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            Date date = rs.getDate(i);
            return date == null ? nullSqlInstance : StrictDate.fromSQLDate(date);
        }
    };

    private static final ResultSetValueHandler TIMESTAMP = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            Timestamp timestamp = rs.getTimestamp(i, calendar);
            return timestamp == null ? nullSqlInstance : DateFunctions.fromSQLTimestamp(timestamp);
        }
    };

    private static final ResultSetValueHandler LONG = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            long val = rs.getLong(i);
            return rs.wasNull() ? nullSqlInstance : val;
        }
    };

    private static final ResultSetValueHandler FLOAT = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            float f = rs.getFloat(i);
            return rs.wasNull() ? nullSqlInstance : Double.valueOf(f);
        }
    };

    private static final ResultSetValueHandler DOUBLE = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            double d = rs.getDouble(i);
            return rs.wasNull() ? nullSqlInstance : d;
        }
    };

    private static final ResultSetValueHandler DECIMAL = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            BigDecimal bd = rs.getBigDecimal(i);
            return bd == null ? nullSqlInstance : bd.doubleValue();
        }
    };

    private static final ResultSetValueHandler BINARY = new ResultSetValueHandler()
    {
        @Override
        public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
        {
            byte[] bytes = rs.getBytes(i);
            return bytes == null ? nullSqlInstance : BinaryUtils.encodeHex(bytes);
        }
    };


    private static final MutableIntObjectMap<ResultSetValueHandler> HANDLERS = IntObjectHashMap.newMap();

    static
    {
        HANDLERS.put(Types.NULL, NULL);
        HANDLERS.put(Types.BIT, BOOLEAN);
        HANDLERS.put(Types.BOOLEAN, BOOLEAN);
        HANDLERS.put(Types.DATE, DATE);
        HANDLERS.put(Types.TIMESTAMP, TIMESTAMP);
        HANDLERS.put(Types.TIMESTAMP_WITH_TIMEZONE, TIMESTAMP);
        HANDLERS.put(Types.TINYINT, LONG);
        HANDLERS.put(Types.SMALLINT, LONG);
        HANDLERS.put(Types.INTEGER, LONG);
        HANDLERS.put(Types.BIGINT, LONG);
        HANDLERS.put(Types.FLOAT, FLOAT);
        HANDLERS.put(Types.REAL, FLOAT);
        HANDLERS.put(Types.DOUBLE, DOUBLE);
        HANDLERS.put(Types.DECIMAL, DECIMAL);
        HANDLERS.put(Types.NUMERIC, DECIMAL);
        HANDLERS.put(Types.CHAR, STRING);
        HANDLERS.put(Types.VARCHAR, STRING);
        HANDLERS.put(Types.LONGVARCHAR, STRING);
        HANDLERS.put(Types.NCHAR, STRING);
        HANDLERS.put(Types.NVARCHAR, STRING);
        HANDLERS.put(Types.LONGNVARCHAR, STRING);
        HANDLERS.put(Types.OTHER, STRING);
        HANDLERS.put(Types.BINARY, BINARY);
        HANDLERS.put(Types.VARBINARY, BINARY);
        HANDLERS.put(Types.LONGVARBINARY, BINARY);
    }


    private ResultSetValueHandlers()
    {
    }


    public interface ResultSetValueHandler
    {
        Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException;
    }

    public static ListIterable<ResultSetValueHandler> getHandlers(ResultSetMetaData metaData) throws SQLException
    {
        MutableList<ResultSetValueHandler> handlers = Lists.mutable.of();
        int count = metaData.getColumnCount();

        for (int i = 1; i <= count; i++)
        {
            ResultSetValueHandler handler = HANDLERS.get(metaData.getColumnType(i));
            if (handler == null)
            {
                throw new PureExecutionException("Unhandled SQL data type (java.sql.Types): " + metaData.getColumnType(i) + ", column: " + i + " " + metaData.getColumnName(i) + " " + metaData.getColumnTypeName(i));
            }
            handlers.add(handler);
        }

        return handlers;
    }
}
