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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Calendar;

public final class ResultSetValueHandlers
{
    private static final ResultSetValueHandler NULL = (rs, i, nullSqlInstance, calendar) -> nullSqlInstance;

    private static final ResultSetValueHandler STRING = (rs, i, nullSqlInstance, calendar) ->
    {
        String str = rs.getString(i);
        return str == null ? nullSqlInstance : str;
    };

    private static final ResultSetValueHandler BOOLEAN = (rs, i, nullSqlInstance, calendar) ->
    {
        boolean bool = rs.getBoolean(i);
        return rs.wasNull() ? nullSqlInstance : bool;
    };

    private static final ResultSetValueHandler LONG = (rs, i, nullSqlInstance, calendar) ->
    {
        long val = rs.getLong(i);
        return rs.wasNull() ? nullSqlInstance : val;
    };

    private static final ResultSetValueHandler DOUBLE = (rs, i, nullSqlInstance, calendar) ->
    {
        double d = rs.getDouble(i);
        return rs.wasNull() ? nullSqlInstance : d;
    };

    private static final ResultSetValueHandler DECIMAL = (rs, i, nullSqlInstance, calendar) ->
    {
        BigDecimal bd = rs.getBigDecimal(i);
        return bd == null ? nullSqlInstance : bd.doubleValue();
    };

    private static final ResultSetValueHandler BINARY = (rs, i, nullSqlInstance, calendar) ->
    {
        byte[] bytes = rs.getBytes(i);
        return bytes == null ? nullSqlInstance : BinaryUtils.encodeHex(bytes);
    };

    private static final ImmutableIntObjectMap<ResultSetValueHandler> HANDLERS = IntObjectMaps.mutable.<ResultSetValueHandler>empty()
            .withKeyValue(Types.NULL, NULL)
            .withKeyValue(Types.BIT, BOOLEAN)
            .withKeyValue(Types.BOOLEAN, BOOLEAN)
            .withKeyValue(Types.TINYINT, LONG)
            .withKeyValue(Types.SMALLINT, LONG)
            .withKeyValue(Types.INTEGER, LONG)
            .withKeyValue(Types.BIGINT, LONG)
            .withKeyValue(Types.FLOAT, DOUBLE)
            .withKeyValue(Types.REAL, DOUBLE)
            .withKeyValue(Types.DOUBLE, DOUBLE)
            .withKeyValue(Types.DECIMAL, DECIMAL)
            .withKeyValue(Types.NUMERIC, DECIMAL)
            .withKeyValue(Types.CHAR, STRING)
            .withKeyValue(Types.VARCHAR, STRING)
            .withKeyValue(Types.LONGVARCHAR, STRING)
            .withKeyValue(Types.NCHAR, STRING)
            .withKeyValue(Types.NVARCHAR, STRING)
            .withKeyValue(Types.LONGNVARCHAR, STRING)
            .withKeyValue(Types.OTHER, STRING)
            .withKeyValue(Types.BINARY, BINARY)
            .withKeyValue(Types.VARBINARY, BINARY)
            .withKeyValue(Types.LONGVARBINARY, BINARY)
            .toImmutable();
    private static final ImmutableMap<String, ResultSetValueHandler> DB_SPECIFIC_HANDLERS = Maps.immutable.with("HUGEINT", LONG); // DuckDb

    private ResultSetValueHandlers()
    {
    }

    public interface ResultSetValueHandler
    {
        Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException;
    }

    public static ListIterable<ResultSetValueHandler> getHandlers(ResultSetMetaData metaData) throws SQLException
    {
        int count = metaData.getColumnCount();
        MutableList<ResultSetValueHandler> handlers = Lists.mutable.ofInitialCapacity(count);
        ResultSetValueHandler dateHandler = null;
        ResultSetValueHandler timestampHandler = null;
        ResultSetValueHandler zonedTimestampHandler = null;
        for (int i = 1; i <= count; i++)
        {
            int columnType = metaData.getColumnType(i);
            String columnTypeName = metaData.getColumnTypeName(i);
            ResultSetValueHandler handler;
            switch (columnType)
            {
                case Types.DATE:
                {
                    handler = (dateHandler == null) ? dateHandler = newDateHandler() : dateHandler;
                    break;
                }
                case Types.TIMESTAMP:
                {
                    handler = (timestampHandler == null) ? timestampHandler = newTimestampHandler() : timestampHandler;
                    break;
                }
                case Types.TIMESTAMP_WITH_TIMEZONE:
                {
                    handler = (zonedTimestampHandler == null) ? zonedTimestampHandler = newZonedTimestampHandler() : zonedTimestampHandler;
                    break;
                }
                default:
                {
                    handler = DB_SPECIFIC_HANDLERS.getIfAbsent(columnTypeName, () -> HANDLERS.get(columnType));
                }
            }
            if (handler == null)
            {
                throw new PureExecutionException("Unhandled SQL data type (java.sql.Types): " + columnType + ", column: " + i + " " + metaData.getColumnName(i) + " " + columnTypeName);
            }
            handlers.add(handler);
        }
        return handlers;
    }

    /**
     * Create a handler for a date column.
     *
     * <p>A date column carries a day and no zone, and {@link ResultSet#getObject(int, Class)}
     * for a {@link LocalDate} hands that day over as it stands. Every other reading runs
     * through a {@link java.sql.Date}, which carries an instant, and an instant yields a day
     * only once a zone is chosen to read it in -- while drivers do not agree on the zone they
     * built it in. H2 uses the zone of the calendar it is given; DuckDB uses the JVM default
     * zone whatever calendar it is given. No one choice reads both correctly, so the day is
     * asked for instead.
     *
     * <p>A driver need not answer that, and DuckDB 1.0.0 does not. The way back is the reading
     * that stood before: take the {@link java.sql.Date} and read it as java.util built it,
     * which is what a driver of that vintage did. A driver is asked once rather than once a
     * row, so a result set costs at most the one exception.
     *
     * @return handler for a date column
     */
    private static ResultSetValueHandler newDateHandler()
    {
        return new ResultSetValueHandler()
        {
            private boolean readsLocalDate = true;

            @Override
            public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
            {
                if (this.readsLocalDate)
                {
                    try
                    {
                        LocalDate localDate = rs.getObject(i, LocalDate.class);
                        return (localDate == null) ? nullSqlInstance : DateFunctions.fromLocalDate(localDate);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsLocalDate = false;
                    }
                }
                Date date = rs.getDate(i);
                return (date == null) ? nullSqlInstance : StrictDate.fromSQLDate(date);
            }
        };
    }

    /**
     * Create a handler for a timestamp column that carries no zone.
     *
     * <p>The column holds a wall clock the database keeps in the zone the connection names, and
     * a Pure date is that moment in UTC, so the wall clock is read and then shifted. Reading it
     * through {@link ResultSet#getObject(int, Class)} for a {@link LocalDateTime} and shifting
     * it here leaves a driver nothing to differ over. Handing a driver a calendar and asking it
     * to shift does not: DuckDB 1.0.0 ignores the calendar and answers as though the connection
     * named UTC, so a connection naming any other zone reads its timestamps off by that zone.
     *
     * @return handler for a timestamp column carrying no zone
     */
    private static ResultSetValueHandler newTimestampHandler()
    {
        return new ResultSetValueHandler()
        {
            private boolean readsLocalDateTime = true;

            @Override
            public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
            {
                if (this.readsLocalDateTime)
                {
                    try
                    {
                        LocalDateTime wallClock = rs.getObject(i, LocalDateTime.class);
                        return (wallClock == null) ?
                               nullSqlInstance :
                               DateFunctions.fromInstant(wallClock.atZone(calendar.getTimeZone().toZoneId()).toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsLocalDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, calendar);
                return (timestamp == null) ? nullSqlInstance : DateFunctions.fromSQLTimestamp(timestamp);
            }
        };
    }

    /**
     * Create a handler for a timestamp column that carries a zone.
     *
     * <p>The column holds a moment already, so the zone the connection names has nothing to say
     * about it and nothing is shifted: only the moment is wanted, read in UTC.
     *
     * @return handler for a timestamp column carrying a zone
     */
    private static ResultSetValueHandler newZonedTimestampHandler()
    {
        return new ResultSetValueHandler()
        {
            private boolean readsOffsetDateTime = true;

            @Override
            public Object value(ResultSet rs, int i, CoreInstance nullSqlInstance, Calendar calendar) throws SQLException
            {
                if (this.readsOffsetDateTime)
                {
                    try
                    {
                        OffsetDateTime moment = rs.getObject(i, OffsetDateTime.class);
                        return (moment == null) ? nullSqlInstance : DateFunctions.fromInstant(moment.toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsOffsetDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, calendar);
                return (timestamp == null) ? nullSqlInstance : DateFunctions.fromSQLTimestamp(timestamp);
            }
        };
    }
}
