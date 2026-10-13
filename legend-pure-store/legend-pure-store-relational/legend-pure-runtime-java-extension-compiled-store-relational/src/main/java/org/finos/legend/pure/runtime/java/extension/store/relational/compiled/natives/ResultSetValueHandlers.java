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
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.tools.time.TimeZoneResolution;

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
import java.time.ZoneId;
import java.util.Calendar;

public final class ResultSetValueHandlers
{
    private static final ResultSetValueHandler NULL = (rs, i) -> null;

    private static final ResultSetValueHandler STRING = ResultSet::getString;

    private static final ResultSetValueHandler BOOLEAN = (rs, i) ->
    {
        boolean bool = rs.getBoolean(i);
        return rs.wasNull() ? null : bool;
    };

    private static final ResultSetValueHandler LONG = (rs, i) ->
    {
        long val = rs.getLong(i);
        return rs.wasNull() ? null : val;
    };

    private static final ResultSetValueHandler DOUBLE = (rs, i) ->
    {
        double d = rs.getDouble(i);
        return rs.wasNull() ? null : d;
    };

    private static final ResultSetValueHandler DECIMAL = (rs, i) ->
    {
        BigDecimal bd = rs.getBigDecimal(i);
        return bd == null ? null : bd.doubleValue();
    };

    private static final ResultSetValueHandler BINARY = (rs, i) ->
    {
        byte[] bytes = rs.getBytes(i);
        return bytes == null ? null : BinaryUtils.encodeHex(bytes);
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
        /**
         * Read the value of a column in the current row of a result set.
         *
         * @param rs result set, positioned on a row
         * @param i  column index, counting from 1
         * @return the value, or null if the column is null in this row
         * @throws SQLException if the driver cannot read the column
         */
        Object value(ResultSet rs, int i) throws SQLException;

        /**
         * Read the value of a column in the current row of a result set, reading a timestamp
         * column as a wall clock kept in the zone of the calendar, whatever zone the handler was
         * got for. Only a timestamp column has a use for the calendar, so by default it is
         * ignored.
         *
         * @param rs       result set, positioned on a row
         * @param i        column index, counting from 1
         * @param calendar calendar in the zone the connection names
         * @return the value, or null if the column is null in this row
         * @throws SQLException if the driver cannot read the column
         * @deprecated Use {@link #value(ResultSet, int)}, with a handler got for the zone the
         * connection names. Retained temporarily, for the deprecated
         * {@code RelationalNativeImplementation.processRow} that takes a calendar.
         */
        @Deprecated
        default Object value(ResultSet rs, int i, Calendar calendar) throws SQLException
        {
            return value(rs, i);
        }
    }

    /**
     * Get a handler for each column of a result set. A timestamp column is read as a wall clock
     * the database keeps in the zone the connection names.
     *
     * <p>The handlers for a date or timestamp column remember what the driver can answer, so they
     * serve one result set, read on one thread.
     *
     * @param metaData   result set metadata
     * @param timeZone   name of the zone the connection names
     * @param resolution how to resolve the name
     * @return a handler for each column, in column order
     * @throws SQLException             if the driver cannot describe the columns
     * @throws IllegalArgumentException if the name is null, or if the resolution rejects names
     *                                  that stand for no zone and this is one
     */
    public static ListIterable<ResultSetValueHandler> getHandlers(ResultSetMetaData metaData, String timeZone, TimeZoneResolution resolution) throws SQLException
    {
        ConnectionTimeZone connectionTimeZone = new ConnectionTimeZone(timeZone, resolution);
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
                    handler = (timestampHandler == null) ? timestampHandler = newTimestampHandler(connectionTimeZone) : timestampHandler;
                    break;
                }
                case Types.TIMESTAMP_WITH_TIMEZONE:
                {
                    handler = (zonedTimestampHandler == null) ? zonedTimestampHandler = newZonedTimestampHandler(connectionTimeZone) : zonedTimestampHandler;
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
     * Get a handler for each column of a result set. A timestamp column is read as a wall clock
     * kept in GMT, the zone a connection naming no zone is read in, unless a calendar in another
     * zone is handed to {@link ResultSetValueHandler#value(ResultSet, int, Calendar)}.
     *
     * @param metaData result set metadata
     * @return a handler for each column, in column order
     * @throws SQLException if the driver cannot describe the columns
     * @deprecated Use {@link #getHandlers(ResultSetMetaData, String, TimeZoneResolution)}, naming
     * the zone the connection names. Retained temporarily, for callers of the deprecated
     * {@code RelationalNativeImplementation.processRow} that takes a calendar.
     */
    @Deprecated
    public static ListIterable<ResultSetValueHandler> getHandlers(ResultSetMetaData metaData) throws SQLException
    {
        return getHandlers(metaData, "GMT", TimeZoneResolution.STRICT);
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
            public Object value(ResultSet rs, int i) throws SQLException
            {
                if (this.readsLocalDate)
                {
                    try
                    {
                        LocalDate localDate = rs.getObject(i, LocalDate.class);
                        return (localDate == null) ? null : DateFunctions.fromLocalDate(localDate);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsLocalDate = false;
                    }
                }
                Date date = rs.getDate(i);
                return (date == null) ? null : StrictDate.fromSQLDate(date);
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
     * @param timeZone zone the connection names
     * @return handler for a timestamp column carrying no zone
     */
    private static ResultSetValueHandler newTimestampHandler(ConnectionTimeZone timeZone)
    {
        return new ResultSetValueHandler()
        {
            private boolean readsLocalDateTime = true;

            @Override
            public Object value(ResultSet rs, int i) throws SQLException
            {
                if (this.readsLocalDateTime)
                {
                    try
                    {
                        LocalDateTime dateTime = rs.getObject(i, LocalDateTime.class);
                        return (dateTime == null) ? null : DateFunctions.fromInstant(dateTime.atZone(timeZone.getZoneId()).toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsLocalDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, timeZone.getCalendar());
                return (timestamp == null) ? null : DateFunctions.fromSQLTimestamp(timestamp);
            }

            @Deprecated
            @Override
            public Object value(ResultSet rs, int i, Calendar calendar) throws SQLException
            {
                if (this.readsLocalDateTime)
                {
                    try
                    {
                        LocalDateTime dateTime = rs.getObject(i, LocalDateTime.class);
                        return (dateTime == null) ? null : DateFunctions.fromInstant(dateTime.atZone(calendar.getTimeZone().toZoneId()).toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsLocalDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, calendar);
                return (timestamp == null) ? null : DateFunctions.fromSQLTimestamp(timestamp);
            }
        };
    }

    /**
     * Create a handler for a timestamp column that carries a zone.
     *
     * <p>The column holds a moment already, so the zone the connection names has nothing to say
     * about it and nothing is shifted: only the moment is wanted, read in UTC.
     *
     * @param timeZone zone the connection names, for a driver that will not give an
     *                 {@link OffsetDateTime}
     * @return handler for a timestamp column carrying a zone
     */
    private static ResultSetValueHandler newZonedTimestampHandler(ConnectionTimeZone timeZone)
    {
        return new ResultSetValueHandler()
        {
            private boolean readsOffsetDateTime = true;

            @Override
            public Object value(ResultSet rs, int i) throws SQLException
            {
                if (this.readsOffsetDateTime)
                {
                    try
                    {
                        OffsetDateTime dateTime = rs.getObject(i, OffsetDateTime.class);
                        return (dateTime == null) ? null : DateFunctions.fromInstant(dateTime.toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsOffsetDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, timeZone.getCalendar());
                return (timestamp == null) ? null : DateFunctions.fromSQLTimestamp(timestamp);
            }

            @Deprecated
            @Override
            public Object value(ResultSet rs, int i, Calendar calendar) throws SQLException
            {
                if (this.readsOffsetDateTime)
                {
                    try
                    {
                        OffsetDateTime dateTime = rs.getObject(i, OffsetDateTime.class);
                        return (dateTime == null) ? null : DateFunctions.fromInstant(dateTime.toInstant(), 9);
                    }
                    catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
                    {
                        this.readsOffsetDateTime = false;
                    }
                }
                Timestamp timestamp = rs.getTimestamp(i, calendar);
                return (timestamp == null) ? null : DateFunctions.fromSQLTimestamp(timestamp);
            }
        };
    }

    /**
     * The zone a connection names, which the database keeps the wall clock of a timestamp column in.
     *
     * <p>The zone is resolved up front, since every timestamp read as a wall clock is shifted out of
     * it. A calendar in that zone is wanted only for a driver that will not give a wall clock, so it
     * is created the first time one is asked for. The handlers for one result set share one
     * instance, and with it one calendar, which is not safe to share between threads.
     */
    private static final class ConnectionTimeZone
    {
        private final String timeZone;
        private final TimeZoneResolution resolution;
        private final ZoneId zoneId;
        private Calendar calendar;

        private ConnectionTimeZone(String timeZone, TimeZoneResolution resolution)
        {
            this.timeZone = timeZone;
            this.resolution = resolution;
            this.zoneId = resolution.parse(timeZone);
        }

        ZoneId getZoneId()
        {
            return this.zoneId;
        }

        Calendar getCalendar()
        {
            if (this.calendar == null)
            {
                this.calendar = this.resolution.newCalendar(this.timeZone);
            }
            return this.calendar;
        }
    }
}
