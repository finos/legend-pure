// Copyright 2026 Goldman Sachs
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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.time.TimeZones;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.RelationalNativeImplementation;
import org.h2.util.DateTimeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for how {@link ResultSetValueHandlers} reads a column, against a database rather than
 * against a hand built {@link java.sql.Date} or {@link java.sql.Timestamp}, since what the driver
 * puts in one of those is the whole of the question.
 */
public class TestResultSetValueHandlers
{
    private static final AtomicInteger DATABASE_COUNT = new AtomicInteger();
    private static final String H2 = "h2";
    private static final String DUCK_DB = "duckdb";

    /**
     * A date column carries a day and no zone, and has to be read as that day whatever zone the
     * JVM is running in and whatever zone the connection names.
     *
     * <p>Every reading that runs through a java.sql.Date has to choose a zone to read its
     * instant in, and the drivers here do not build that instant alike: H2 works a day into one
     * through java.time in the zone of the calendar it is given, DuckDB through java.util in the
     * JVM default zone whatever calendar it is given. No one choice of zone reads both. So the
     * day is asked for directly, and a java.sql.Date is only the way back for a driver that will
     * not give one.
     *
     * <p>Both drivers resolved here answer {@link ResultSet#getObject(int, Class)} for a
     * {@link java.time.LocalDate}, so both take the first path and the way back is not exercised.
     * DuckDB 1.0.0 was the driver that refused, and the reason the way back is there at all; it
     * has answered since 1.3.0.0.
     */
    @Test
    public void testDateColumnIsIndependentOfTheDefaultTimeZone() throws SQLException
    {
        ImmutableList<String> days = Lists.immutable.with("1582-10-04", "1753-12-31", "1900-01-01", "2014-03-10");
        ImmutableList<String> zoneIds = Lists.immutable.with(
                "GMT",
                "America/New_York",   // -04:56:02 in 1753 by java.time, -05:00:00 by java.util
                "America/Chicago",    // -05:50:36 against -06:00:00
                "Europe/Paris",       // +00:09:21 against +01:00:00, the disagreement the other way
                "Asia/Tokyo"          // +09:18:59 against +09:00:00
        );
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try
        {
            for (String zoneId : zoneIds)
            {
                setDefaultTimeZone(zoneId);
                Assert.assertEquals("H2, default " + zoneId, days, readDays(H2, days));
                Assert.assertEquals("DuckDB, default " + zoneId, days, readDays(DUCK_DB, days));
            }
        }
        finally
        {
            setDefaultTimeZone(defaultTimeZone.getID());
        }
    }

    /**
     * H2 reads the JVM default zone once and holds on to it, so setting the default without telling
     * H2 leaves it converting in whatever zone it first saw, and a test sweeping zones would sweep
     * nothing.
     */
    private static void setDefaultTimeZone(String zoneId)
    {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        DateTimeUtils.resetCalendar();
    }

    /**
     * Read the days back through the handlers, with a connection zone deliberately not GMT: a date
     * column must not be read in it.
     */
    private static MutableList<String> readDays(String database, ListIterable<String> days) throws SQLException
    {
        try (Connection connection = connect(database);
             Statement statement = connection.createStatement())
        {
            statement.execute("CREATE TABLE dates (recorded_on DATE)");
            for (String day : days)
            {
                statement.execute("INSERT INTO dates VALUES (DATE '" + day + "')");
            }

            MutableList<String> read = Lists.mutable.ofInitialCapacity(days.size());
            try (ResultSet resultSet = statement.executeQuery("SELECT recorded_on FROM dates ORDER BY recorded_on"))
            {
                ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSet.getMetaData());
                Calendar calendar = TimeZones.newCalendar("America/New_York");
                while (resultSet.next())
                {
                    read.add(RelationalNativeImplementation.processRow(resultSet, handlers, null, calendar).get(0).toString());
                }
            }
            return read;
        }
    }

    /**
     * A timestamp column carries a wall clock the database keeps in the zone the connection
     * names, and a Pure date is that moment in UTC, so reading one shifts it out of that zone.
     * Asking the driver to do the shifting, by handing it a calendar, does not do that for every
     * driver: DuckDB 1.0.0 ignored the calendar and answered as though the connection named UTC,
     * so every timestamp came back short by the connection zone. Reading the wall clock and
     * shifting it here leaves nothing for a driver to differ over, whether or not the driver
     * resolved today is one that would have got it right.
     */
    @Test
    public void testTimestampColumnIsShiftedOutOfTheConnectionZone() throws SQLException
    {
        ImmutableList<String> stored = Lists.immutable.with("1753-12-31 00:00:00", "2014-03-10 13:07:44");
        ImmutableList<String> connectionZoneIds = Lists.immutable.with("GMT", "America/New_York", "Asia/Tokyo");
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try
        {
            for (String defaultZoneId : Lists.immutable.with("GMT", "Asia/Tokyo"))
            {
                setDefaultTimeZone(defaultZoneId);
                for (String connectionZoneId : connectionZoneIds)
                {
                    MutableList<String> want = stored.collect(s -> LocalDateTime.parse(s.replace(' ', 'T'))
                            .atZone(ZoneId.of(connectionZoneId)).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.000000000+0000"))).toList();
                    String where = ", default " + defaultZoneId + ", connection " + connectionZoneId;
                    Assert.assertEquals("H2" + where, want, readMoments(H2, connectionZoneId, stored));
                    Assert.assertEquals("DuckDB" + where, want, readMoments(DUCK_DB, connectionZoneId, stored));
                }
            }
        }
        finally
        {
            setDefaultTimeZone(defaultTimeZone.getID());
        }
    }

    private static MutableList<String> readMoments(String database, String connectionZoneId, ListIterable<String> stored) throws SQLException
    {
        try (Connection connection = connect(database);
             Statement statement = connection.createStatement())
        {
            statement.execute("CREATE TABLE moments (recorded_at TIMESTAMP)");
            for (String moment : stored)
            {
                statement.execute("INSERT INTO moments VALUES (TIMESTAMP '" + moment + "')");
            }

            MutableList<String> read = Lists.mutable.ofInitialCapacity(stored.size());
            try (ResultSet resultSet = statement.executeQuery("SELECT recorded_at FROM moments ORDER BY recorded_at"))
            {
                ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSet.getMetaData());
                Calendar calendar = TimeZones.newCalendar(connectionZoneId);
                while (resultSet.next())
                {
                    read.add(RelationalNativeImplementation.processRow(resultSet, handlers, null, calendar).get(0).toString());
                }
            }
            return read;
        }
    }

    /**
     * A driver can answer {@link ResultSet#getObject(int, Class)} for a java.time class and still
     * not answer it soundly: the DB2 driver reads a null column by throwing a
     * {@link NullPointerException}. A driver that fails is read as one that will not answer, the
     * way columns were read before, and is not asked again, so a result set costs one exception
     * for each column here whether its first null comes before any value or after one.
     */
    @Test
    public void testDriverThatFailsOnANullIsReadAsBefore() throws SQLException
    {
        ImmutableList<ImmutableList<String>> stored = Lists.immutable.with(
                Lists.immutable.with("DATE '2014-03-10'", "TIMESTAMP '2014-03-10 13:07:44'", "TIMESTAMP WITH TIME ZONE '2014-03-10 13:07:44+05:00'"),
                Lists.immutable.with("NULL", "NULL", "NULL"),
                Lists.immutable.with("DATE '2015-07-21'", "TIMESTAMP '2015-07-21 09:30:00'", "TIMESTAMP WITH TIME ZONE '2015-07-21 09:30:00-04:00'"),
                Lists.immutable.with("NULL", "NULL", "NULL"));
        ImmutableList<String> nulls = Lists.immutable.with(null, null, null);
        // the timestamps without a zone are kept in Tokyo, nine hours ahead of UTC
        ImmutableList<ImmutableList<String>> want = Lists.immutable.with(
                Lists.immutable.with("2014-03-10", "2014-03-10T04:07:44.000000000+0000", "2014-03-10T08:07:44.000000000+0000"),
                nulls,
                Lists.immutable.with("2015-07-21", "2015-07-21T00:30:00.000000000+0000", "2015-07-21T13:30:00.000000000+0000"),
                nulls);
        assertReadThroughDriverFailingOnNull("value first", stored, "ASC", want);
        assertReadThroughDriverFailingOnNull("null first", stored, "DESC", want.toReversed());
    }

    private static void assertReadThroughDriverFailingOnNull(String description, ListIterable<? extends ListIterable<String>> stored, String order, ListIterable<? extends ListIterable<String>> want) throws SQLException
    {
        CoreInstance sqlNull = new ModelRepository().newStringCoreInstance("SQLNull");
        try (Connection connection = connect(H2);
             Statement statement = connection.createStatement())
        {
            statement.execute("CREATE TABLE moments (id INTEGER, recorded_on DATE, recorded_at TIMESTAMP, recorded_at_zoned TIMESTAMP WITH TIME ZONE)");
            for (int id = 0; id < stored.size(); id++)
            {
                statement.execute(stored.get(id).makeString("INSERT INTO moments VALUES (" + id + ", ", ", ", ")"));
            }

            AtomicInteger failures = new AtomicInteger();
            MutableList<ListIterable<String>> read = Lists.mutable.ofInitialCapacity(stored.size());
            try (ResultSet resultSet = failingOnNull(statement.executeQuery("SELECT recorded_on, recorded_at, recorded_at_zoned FROM moments ORDER BY id " + order), failures))
            {
                ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSet.getMetaData());
                Calendar calendar = TimeZones.newCalendar("Asia/Tokyo");
                while (resultSet.next())
                {
                    ListIterable<Object> row = RelationalNativeImplementation.processRow(resultSet, handlers, sqlNull, calendar);
                    read.add(row.collect(value -> (value == sqlNull) ? null : value.toString()));
                }
            }
            Assert.assertEquals(description, want, read);
            Assert.assertEquals(description + ", exceptions", 3, failures.get());
        }
    }

    /**
     * Wrap a result set so that it reads a null column the way the DB2 driver does when asked for a
     * java.time class, by throwing a {@link NullPointerException}, and counts the times it does.
     */
    private static ResultSet failingOnNull(ResultSet resultSet, AtomicInteger failures)
    {
        return (ResultSet) Proxy.newProxyInstance(TestResultSetValueHandlers.class.getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, args) ->
        {
            Object value;
            try
            {
                value = method.invoke(resultSet, args);
            }
            catch (InvocationTargetException e)
            {
                throw e.getCause();
            }
            if ((value == null) && "getObject".equals(method.getName()) && (args.length == 2) && (args[1] instanceof Class))
            {
                failures.incrementAndGet();
                throw new NullPointerException();
            }
            return value;
        });
    }

    private static Connection connect(String database) throws SQLException
    {
        if (H2.equals(database))
        {
            return DriverManager.getConnection("jdbc:h2:mem:testResultSetValueHandlers" + DATABASE_COUNT.incrementAndGet(), "sa", "");
        }
        return DriverManager.getConnection("jdbc:duckdb:");
    }
}
