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

package org.finos.legend.pure.m4.coreinstance.primitive.date;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tests for {@link DateFunctions}, other than
 * {@link DateFunctions#compare(PureDate, PureDate)} and
 * {@link DateFunctions#dateDifference(PureDate, PureDate, String)}, which are covered by
 * {@link TestDateCompare} and {@link TestDateDifference}.
 */
public class TestDateFunctions
{
    private static final long MILLIS_2014_03_10T16_12_35 = LocalDateTime.of(2014, 3, 10, 16, 12, 35).toInstant(ZoneOffset.UTC).toEpochMilli();
    private static final long MILLIS_2014_07_10T16_12_35 = LocalDateTime.of(2014, 7, 10, 16, 12, 35).toInstant(ZoneOffset.UTC).toEpochMilli();

    // Factories

    @Test
    public void testNewPureDateGranularities()
    {
        PureDate year = DateFunctions.newPureDate(2014);
        assertGranularity(year, false, false, false, false, false, false);
        Assert.assertEquals("2014", year.toString());
        Assert.assertEquals(2014, year.getYear());

        PureDate yearMonth = DateFunctions.newPureDate(2014, 3);
        assertGranularity(yearMonth, true, false, false, false, false, false);
        Assert.assertEquals("2014-03", yearMonth.toString());
        Assert.assertEquals(3, yearMonth.getMonth());

        PureDate strictDate = DateFunctions.newPureDate(2014, 3, 10);
        assertGranularity(strictDate, true, true, false, false, false, false);
        Assert.assertEquals("2014-03-10", strictDate.toString());
        Assert.assertEquals(10, strictDate.getDay());

        PureDate withHour = DateFunctions.newPureDate(2014, 3, 10, 16);
        assertGranularity(withHour, true, true, true, false, false, false);
        Assert.assertEquals("2014-03-10T16", withHour.toString());
        Assert.assertEquals(16, withHour.getHour());

        PureDate withMinute = DateFunctions.newPureDate(2014, 3, 10, 16, 12);
        assertGranularity(withMinute, true, true, true, true, false, false);
        Assert.assertEquals("2014-03-10T16:12+0000", withMinute.toString());
        Assert.assertEquals(12, withMinute.getMinute());

        PureDate withSecond = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35);
        assertGranularity(withSecond, true, true, true, true, true, false);
        Assert.assertEquals("2014-03-10T16:12:35+0000", withSecond.toString());
        Assert.assertEquals(35, withSecond.getSecond());

        PureDate withSubsecond = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        assertGranularity(withSubsecond, true, true, true, true, true, true);
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", withSubsecond.toString());
        Assert.assertEquals("070004235", withSubsecond.getSubsecond());
    }

    /**
     * Components a date does not have read back as -1 (or null for the subsecond), which is what
     * keeps {@link AbstractPureDate#equals} consistent with
     * {@link DateFunctions#compare(PureDate, PureDate)}.
     */
    @Test
    public void testAbsentComponentsReadBackAsMinusOne()
    {
        PureDate year = DateFunctions.newPureDate(2014);
        Assert.assertEquals(-1, year.getMonth());
        Assert.assertEquals(-1, year.getDay());
        Assert.assertEquals(-1, year.getHour());
        Assert.assertEquals(-1, year.getMinute());
        Assert.assertEquals(-1, year.getSecond());
        Assert.assertNull(year.getSubsecond());

        Assert.assertNotEquals(DateFunctions.newPureDate(2014), DateFunctions.newPureDate(2014, 1));
        Assert.assertNotEquals(DateFunctions.newPureDate(2014, 1, 1), DateFunctions.newPureDate(2014, 1, 1, 0));
    }

    /**
     * A subsecond is a run of digits that the rest of the class reads back as digits, so it has to
     * be written in the digits it will be read in. Some locales number in something other than
     * 0 to 9 - Persian and any locale asking for a numbering system, as the tags below do - and a
     * subsecond written in those is not merely printed oddly: incrementing one does arithmetic on
     * the characters, so the answer comes back wrong rather than failing.
     */
    @Test
    public void testSubsecondsAreWrittenInASCIIDigitsWhateverTheLocale()
    {
        Locale before = Locale.getDefault();
        try
        {
            for (String tag : new String[]{"en-US", "fa-IR", "hi-IN-u-nu-deva", "ar-EG-u-nu-arab", "bn-IN-u-nu-beng"})
            {
                Locale.setDefault(Locale.forLanguageTag(tag));

                // built from an instant, where the subsecond is written straight into the date
                PureDate fromInstant = DateFunctions.fromInstant(Instant.parse("2015-08-15T10:20:30.123456789Z"), 9);
                Assert.assertEquals(tag, "123456789", fromInstant.getSubsecond());
                Assert.assertEquals(tag, "2015-08-15T10:20:30.123456789+0000", fromInstant.toString());

                // and from a calendar, which keeps only milliseconds
                GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                calendar.clear();
                calendar.set(2015, Calendar.AUGUST, 15, 10, 20, 30);
                calendar.set(Calendar.MILLISECOND, 7);
                Assert.assertEquals(tag, "007", DateFunctions.fromCalendar(calendar).getSubsecond());

                // incrementing reads the digits back, so a subsecond written in any other numbering
                // system would come out wrong here rather than throwing
                PureDate base = DateFunctions.parsePureDate("2015-08-15T10:20:30.000");
                Assert.assertEquals(tag, "2015-08-15T10:20:30.001+0000", base.addMilliseconds(1).toString());
                Assert.assertEquals(tag, "2015-08-15T10:20:29.999+0000", base.addMilliseconds(-1).toString());

                PureDate micros = DateFunctions.parsePureDate("2015-08-15T10:20:30.000000");
                Assert.assertEquals(tag, "2015-08-15T10:20:30.000001+0000", micros.addMicroseconds(1).toString());

                PureDate nanos = DateFunctions.parsePureDate("2015-08-15T10:20:30.000000000");
                Assert.assertEquals(tag, "2015-08-15T10:20:30.000000001+0000", nanos.addNanoseconds(1).toString());
            }
        }
        finally
        {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testNewPureDateValidation()
    {
        Assert.assertEquals(
                String.format(Locale.ROOT, "Invalid year (valid [%,d, %,d]): %,d", java.time.Year.MIN_VALUE, java.time.Year.MAX_VALUE, java.time.Year.MAX_VALUE + 1L),
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(java.time.Year.MAX_VALUE + 1)).getMessage());
        Assert.assertEquals("Invalid month: 13", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 13)).getMessage());
        Assert.assertEquals("Invalid month: 0", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 0)).getMessage());
        Assert.assertEquals("Invalid day: 0", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 0)).getMessage());
        Assert.assertEquals("Invalid day: 2014-2-29", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 2, 29)).getMessage());
        Assert.assertEquals("Invalid hour: 24", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 10, 24)).getMessage());
        Assert.assertEquals("Invalid minute: 60", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 10, 16, 60)).getMessage());
        Assert.assertEquals("Invalid second: 60", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 10, 16, 12, 60)).getMessage());
        Assert.assertEquals("Invalid subsecond value: \"\"", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "")).getMessage());
        Assert.assertEquals("Invalid subsecond value: null", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, null)).getMessage());

        // February 29 is fine in a leap year
        Assert.assertEquals("2016-02-29", DateFunctions.newPureDate(2016, 2, 29).toString());
    }

    // Calendar and Gregorian utilities

    @Test
    public void testIsLeapYear()
    {
        Assert.assertTrue(DateFunctions.isLeapYear(2016));
        Assert.assertTrue(DateFunctions.isLeapYear(2000));
        Assert.assertTrue(DateFunctions.isLeapYear(1600));
        Assert.assertFalse(DateFunctions.isLeapYear(2014));
        Assert.assertFalse(DateFunctions.isLeapYear(1900));
        Assert.assertFalse(DateFunctions.isLeapYear(2100));
    }

    @Test
    public void testGetDaysInMonth()
    {
        int[] nonLeapYearDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int month = 1; month <= 12; month++)
        {
            Assert.assertEquals("2014-" + month, nonLeapYearDays[month - 1], DateFunctions.getDaysInMonth(2014, month));
        }
        Assert.assertEquals(29, DateFunctions.getDaysInMonth(2016, 2));
        Assert.assertEquals(28, DateFunctions.getDaysInMonth(1900, 2));

        Assert.assertEquals("Invalid month: 13", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.getDaysInMonth(2014, 13)).getMessage());
        Assert.assertEquals("Invalid month: 0", Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.getDaysInMonth(2014, 0)).getMessage());
    }

    @Test
    public void testGetYearDays()
    {
        Assert.assertEquals(365, DateFunctions.getYearDays(2014));
        Assert.assertEquals(366, DateFunctions.getYearDays(2016));
        Assert.assertEquals(365, DateFunctions.getYearDays(1900));
        Assert.assertEquals(366, DateFunctions.getYearDays(2000));
    }

    @Test
    public void testFromCalendarWithEachPrecision()
    {
        GregorianCalendar calendar = new GregorianCalendar(DateFunctions.GMT_TIME_ZONE);
        calendar.setTimeInMillis(MILLIS_2014_03_10T16_12_35 + 70L);

        Assert.assertEquals("2014", DateFunctions.fromCalendar(calendar, Calendar.YEAR).toString());
        Assert.assertEquals("2014-03", DateFunctions.fromCalendar(calendar, Calendar.MONTH).toString());
        Assert.assertEquals("2014-03-10", DateFunctions.fromCalendar(calendar, Calendar.DAY_OF_MONTH).toString());
        Assert.assertEquals("2014-03-10", DateFunctions.fromCalendar(calendar, Calendar.DAY_OF_YEAR).toString());
        Assert.assertEquals("2014-03-10T16", DateFunctions.fromCalendar(calendar, Calendar.HOUR_OF_DAY).toString());
        Assert.assertEquals("2014-03-10T16:12+0000", DateFunctions.fromCalendar(calendar, Calendar.MINUTE).toString());
        Assert.assertEquals("2014-03-10T16:12:35+0000", DateFunctions.fromCalendar(calendar, Calendar.SECOND).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromCalendar(calendar, Calendar.MILLISECOND).toString());

        // millisecond precision is the default
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromCalendar(calendar).toString());

        Assert.assertEquals(
                "Unsupported calendar precision: " + Calendar.ERA,
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.fromCalendar(calendar, Calendar.ERA)).getMessage());
    }

    /**
     * {@code fromCalendar} reads the instant the calendar holds, in UTC, whatever zone the calendar
     * is in. The zone only decides whether the fields can be read as they stand or the calendar has
     * to be rebuilt in GMT first; it never shifts the instant.
     */
    @Test
    public void testFromCalendarIsIndependentOfTheCalendarTimeZone()
    {
        String[] zoneIds = {
                "GMT",              // read as is
                "UTC",              // zero offset, but not equal to the GMT TimeZone
                "Etc/GMT-5",        // fixed positive offset
                "Etc/GMT+7",        // fixed negative offset
                "America/New_York"  // negative raw offset, and on daylight saving time in July
        };
        for (String zoneId : zoneIds)
        {
            GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(zoneId));
            calendar.setTimeInMillis(MILLIS_2014_07_10T16_12_35);
            Assert.assertEquals(zoneId, "2014-07-10T16:12:35+0000", DateFunctions.fromCalendar(calendar, Calendar.SECOND).toString());

            // the argument is left untouched
            Assert.assertEquals(zoneId, MILLIS_2014_07_10T16_12_35, calendar.getTimeInMillis());
            Assert.assertEquals(zoneId, TimeZone.getTimeZone(zoneId), calendar.getTimeZone());
        }
    }

    /**
     * A zone whose raw offset is zero can still be an hour off UTC while daylight saving time is in
     * force, so the decision cannot be made from the raw offset (or from the zone's identity) alone.
     * Europe/London is UTC+01:00 in July and UTC+00:00 in January.
     */
    @Test
    public void testFromCalendarWithDaylightSavingOnAZeroRawOffsetZone()
    {
        TimeZone london = TimeZone.getTimeZone("Europe/London");
        Assert.assertEquals(0, london.getRawOffset());

        GregorianCalendar summer = new GregorianCalendar(london);
        summer.setTimeInMillis(MILLIS_2014_07_10T16_12_35);
        Assert.assertEquals(0, summer.get(Calendar.ZONE_OFFSET));
        Assert.assertEquals(60 * 60 * 1000, summer.get(Calendar.DST_OFFSET));
        Assert.assertEquals("2014-07-10T16:12:35+0000", DateFunctions.fromCalendar(summer, Calendar.SECOND).toString());

        GregorianCalendar winter = new GregorianCalendar(london);
        winter.setTimeInMillis(MILLIS_2014_03_10T16_12_35);
        Assert.assertEquals(0, winter.get(Calendar.ZONE_OFFSET));
        Assert.assertEquals(0, winter.get(Calendar.DST_OFFSET));
        Assert.assertEquals("2014-03-10T16:12:35+0000", DateFunctions.fromCalendar(winter, Calendar.SECOND).toString());
    }

    /**
     * {@link PureDate#getCalendar()} and {@code fromCalendar} are inverse at every granularity, down
     * to the millisecond precision a {@link GregorianCalendar} can carry.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testFromCalendarRoundTripsGetCalendar()
    {
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014), Calendar.YEAR);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3), Calendar.MONTH);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3, 10), Calendar.DAY_OF_MONTH);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3, 10, 16), Calendar.HOUR_OF_DAY);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3, 10, 16, 12), Calendar.MINUTE);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35), Calendar.SECOND);
        assertRoundTripsThroughCalendar(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070"), Calendar.MILLISECOND);

        // a date coarser than its target precision expands to the start of the span it covers
        Assert.assertEquals("2014-01-01T00:00:00.000+0000", DateFunctions.fromCalendar(DateFunctions.newPureDate(2014).getCalendar()).toString());
        Assert.assertEquals("2014-03-01", DateFunctions.fromCalendar(DateFunctions.newPureDate(2014, 3).getCalendar(), Calendar.DAY_OF_MONTH).toString());
    }

    // Conversions

    @Test
    public void testFromDate()
    {
        Date date = new Date(MILLIS_2014_03_10T16_12_35 + 70L);
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromDate(date).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.formatDate(date));
    }

    @Test
    public void testFromSQLDate()
    {
        // A SQL date is normalized to midnight in the default zone, which is how a driver hands
        // one over and what java.sql.Date.valueOf builds.
        java.sql.Date sqlDate = java.sql.Date.valueOf("2014-03-10");
        StrictDate date = DateFunctions.fromSQLDate(sqlDate);
        Assert.assertEquals("2014-03-10", date.toString());
        Assert.assertEquals(date, DateFunctions.fromDate(sqlDate));

        // formatDate delegates to java.sql.Date.toString for SQL dates
        Assert.assertEquals(sqlDate.toString(), DateFunctions.formatDate(sqlDate));
    }

    /**
     * A SQL date names a day and no zone, and so does a {@link StrictDate}, so the conversion is a
     * copy of the year, month, and day: the same SQL date has to give the same Pure date whatever
     * zone the JVM is running in. Reading the instant the {@link java.sql.Date} wraps back in GMT
     * would not, since a SQL date sits at midnight in the default zone and midnight east of GMT
     * belongs to the day before.
     */
    @Test
    public void testFromSQLDateIsIndependentOfTheDefaultTimeZone()
    {
        String[] zoneIds = {
                "GMT",
                "America/New_York",   // behind GMT, where reading the instant in GMT happens to work
                "Asia/Tokyo",         // +09:00, far enough ahead of GMT that midnight is the day before
                "Pacific/Kiritimati", // +14:00, the furthest ahead of GMT there is
                "America/Sao_Paulo"   // 2018-11-04 had no midnight there: the clocks sprang forward
        };
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try
        {
            for (String zoneId : zoneIds)
            {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
                assertFromSQLDate("2014-03-10", zoneId);
                assertFromSQLDate("2018-11-04", zoneId);
                assertFromSQLDate("1900-01-01", zoneId);
            }
        }
        finally
        {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    private static void assertFromSQLDate(String day, String zoneId)
    {
        Assert.assertEquals(zoneId, day, DateFunctions.fromSQLDate(java.sql.Date.valueOf(day)).toString());
    }

    @Test
    public void testFromSQLTimestamp()
    {
        java.sql.Timestamp timestamp = new java.sql.Timestamp(MILLIS_2014_03_10T16_12_35);
        timestamp.setNanos(70004235);
        DateTime date = DateFunctions.fromSQLTimestamp(timestamp);
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", date.toString());
        Assert.assertEquals(date, DateFunctions.fromDate(timestamp));
    }

    /**
     * A Pure date holds a {@link java.time.LocalDate}, which is proleptic Gregorian: the Gregorian
     * rules run backwards through the reform of 1582 and on to year one. So a moment has to be read
     * into one the same way, and not through a {@link GregorianCalendar}, which reverts to the
     * Julian calendar at the reform and would file a moment in the year 1000 five days early.
     */
    @Test
    public void testConversionsBeforeTheGregorianReform()
    {
        assertConvertsInstant("2014-03-10T16:12:35Z", "2014-03-10T16:12:35");
        assertConvertsInstant("1582-10-15T00:00:00Z", "1582-10-15T00:00:00");  // the first Gregorian day
        assertConvertsInstant("1582-10-04T12:00:00Z", "1582-10-04T12:00:00");  // the last Julian day
        assertConvertsInstant("1000-01-01T00:00:00Z", "1000-01-01T00:00:00");
        assertConvertsInstant("0001-01-01T00:00:00Z", "1-01-01T00:00:00");
    }

    private static void assertConvertsInstant(String instantText, String expected)
    {
        Instant instant = Instant.parse(instantText);
        Assert.assertEquals(instantText, expected + ".000000000+0000", DateFunctions.fromInstant(instant).toString());
        Assert.assertEquals(instantText, expected + ".000000000+0000", DateFunctions.fromSQLTimestamp(java.sql.Timestamp.from(instant)).toString());
        Assert.assertEquals(instantText, expected + ".000+0000", DateFunctions.fromDate(new Date(instant.toEpochMilli())).toString());
    }

    @Test
    public void testFromInstant()
    {
        Instant instant = LocalDateTime.of(2014, 3, 10, 16, 12, 35, 70004235).toInstant(ZoneOffset.UTC);
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", DateFunctions.fromInstant(instant).toString());
        Assert.assertEquals("2014-03-10T16:12:35.0+0000", DateFunctions.fromInstant(instant, 1).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromInstant(instant, 3).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", DateFunctions.fromInstant(instant, 9).toString());

        // a precision of 0 drops to second granularity
        Assert.assertEquals("2014-03-10T16:12:35+0000", DateFunctions.fromInstant(instant, 0).toString());

        Assert.assertEquals(
                "Invalid subsecond precision: 10",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.fromInstant(instant, 10)).getMessage());
        Assert.assertEquals(
                "Invalid subsecond precision: -1",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.fromInstant(instant, -1)).getMessage());
    }

    // java.time conversions

    @Test
    public void testFromYear()
    {
        PureDate date = DateFunctions.fromYear(java.time.Year.of(2014));
        assertGranularity(date, false, false, false, false, false, false);
        Assert.assertEquals(2014, date.getYear());
        Assert.assertEquals("2014", date.toString());
        Assert.assertEquals(ModelRepository.DATE_TYPE_NAME, DateFunctions.datePrimitiveType(date));
        Assert.assertEquals(DateFunctions.newPureDate(2014), date);

        // the whole supported range, including year zero and negative years
        Assert.assertEquals(DateFunctions.newPureDate(-1), DateFunctions.fromYear(java.time.Year.of(-1)));
        Assert.assertEquals(DateFunctions.newPureDate(0), DateFunctions.fromYear(java.time.Year.of(0)));
        Assert.assertEquals(DateFunctions.newPureDate(java.time.Year.MIN_VALUE), DateFunctions.fromYear(java.time.Year.of(java.time.Year.MIN_VALUE)));
        Assert.assertEquals(DateFunctions.newPureDate(java.time.Year.MAX_VALUE), DateFunctions.fromYear(java.time.Year.of(java.time.Year.MAX_VALUE)));
    }

    @Test
    public void testFromYearMonth()
    {
        PureDate date = DateFunctions.fromYearMonth(java.time.YearMonth.of(2014, 3));
        assertGranularity(date, true, false, false, false, false, false);
        Assert.assertEquals(2014, date.getYear());
        Assert.assertEquals(3, date.getMonth());
        Assert.assertEquals("2014-03", date.toString());
        Assert.assertEquals(ModelRepository.DATE_TYPE_NAME, DateFunctions.datePrimitiveType(date));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), date);

        Assert.assertEquals(DateFunctions.newPureDate(2014, 1), DateFunctions.fromYearMonth(java.time.YearMonth.of(2014, 1)));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 12), DateFunctions.fromYearMonth(java.time.YearMonth.of(2014, 12)));
    }

    @Test
    public void testFromLocalDate()
    {
        StrictDate date = DateFunctions.fromLocalDate(LocalDate.of(2014, 3, 10));
        assertGranularity(date, true, true, false, false, false, false);
        Assert.assertEquals("2014-03-10", date.toString());
        Assert.assertEquals(ModelRepository.STRICT_DATE_TYPE_NAME, DateFunctions.datePrimitiveType(date));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), date);

        // leap day
        Assert.assertEquals(DateFunctions.newPureDate(2016, 2, 29), DateFunctions.fromLocalDate(LocalDate.of(2016, 2, 29)));
    }

    @Test
    public void testFromLocalDateTime()
    {
        LocalDateTime dateTime = LocalDateTime.of(2014, 3, 10, 16, 12, 35, 70004235);

        DateTime date = DateFunctions.fromLocalDateTime(dateTime);
        assertGranularity(date, true, true, true, true, true, true);
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", date.toString());
        Assert.assertEquals(ModelRepository.DATETIME_TYPE_NAME, DateFunctions.datePrimitiveType(date));

        // a LocalDateTime carries no zone, so its fields are taken as they stand
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235"), date);
        Assert.assertEquals(date, DateFunctions.fromInstant(dateTime.toInstant(ZoneOffset.UTC)));

        Assert.assertEquals("2014-03-10T16:12:35.0+0000", DateFunctions.fromLocalDateTime(dateTime, 1).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromLocalDateTime(dateTime, 3).toString());
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", DateFunctions.fromLocalDateTime(dateTime, 9).toString());

        // a whole number of seconds still gets a subsecond, zero padded to the requested precision
        Assert.assertEquals("2014-03-10T16:12:35.000000000+0000", DateFunctions.fromLocalDateTime(LocalDateTime.of(2014, 3, 10, 16, 12, 35)).toString());
        Assert.assertEquals("2014-03-10T16:12:35.000+0000", DateFunctions.fromLocalDateTime(LocalDateTime.of(2014, 3, 10, 16, 12, 35), 3).toString());

        // digits beyond the requested precision are dropped, not rounded
        Assert.assertEquals("2014-03-10T16:12:35.9+0000", DateFunctions.fromLocalDateTime(LocalDateTime.of(2014, 3, 10, 16, 12, 35, 999999999), 1).toString());

        // a precision of 0 drops to second granularity
        Assert.assertEquals("2014-03-10T16:12:35+0000", DateFunctions.fromLocalDateTime(dateTime, 0).toString());

        Assert.assertEquals(
                "Invalid subsecond precision: 10",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.fromLocalDateTime(dateTime, 10)).getMessage());
        Assert.assertEquals(
                "Invalid subsecond precision: -1",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.fromLocalDateTime(dateTime, -1)).getMessage());
    }

    /**
     * Pure dates are always understood as UTC, so an offset is applied rather than discarded.
     */
    @Test
    public void testFromOffsetDateTime()
    {
        DateTime expected = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");

        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 16, 12, 35, 70004235, ZoneOffset.UTC)));
        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 11, 12, 35, 70004235, ZoneOffset.ofHours(-5))));
        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 17, 12, 35, 70004235, ZoneOffset.ofHours(1))));
        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 16, 42, 35, 70004235, ZoneOffset.ofHoursMinutes(0, 30))));

        // an offset can carry the instant into the previous or next day
        Assert.assertEquals(
                DateFunctions.newPureDate(2014, 3, 10, 23, 30, 0, "000"),
                DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 11, 4, 30, 0, 0, ZoneOffset.ofHours(5)), 3));

        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 11, 12, 35, 70004235, ZoneOffset.ofHours(-5)), 3).toString());
    }

    /**
     * As for offsets, but the offset comes from what the zone was doing at that instant, so daylight
     * saving time is taken into account.
     */
    @Test
    public void testFromZonedDateTime()
    {
        DateTime expected = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        ZoneId newYork = ZoneId.of("America/New_York");

        // 2014-03-10 is after that year's daylight saving change, so New York is UTC-04:00
        Assert.assertEquals(expected, DateFunctions.fromZonedDateTime(ZonedDateTime.of(LocalDateTime.of(2014, 3, 10, 12, 12, 35, 70004235), newYork)));
        // in January it is UTC-05:00
        Assert.assertEquals(
                DateFunctions.newPureDate(2014, 1, 10, 16, 12, 35, "070004235"),
                DateFunctions.fromZonedDateTime(ZonedDateTime.of(LocalDateTime.of(2014, 1, 10, 11, 12, 35, 70004235), newYork)));

        Assert.assertEquals(expected, DateFunctions.fromZonedDateTime(ZonedDateTime.of(LocalDateTime.of(2014, 3, 10, 16, 12, 35, 70004235), ZoneId.of("UTC"))));
        Assert.assertEquals("2014-03-10T16:12:35.070+0000", DateFunctions.fromZonedDateTime(ZonedDateTime.of(LocalDateTime.of(2014, 3, 10, 12, 12, 35, 70004235), newYork), 3).toString());
    }

    /**
     * A subsecond precision of 0 means no subsecond at all, so the result is a date of second
     * granularity. {@link DateWithSubsecond} does not accept 0 itself, since a date with no
     * subsecond is not a {@link DateWithSubsecond}.
     */
    @Test
    public void testSubsecondPrecisionZeroGivesSecondGranularity()
    {
        LocalDateTime dateTime = LocalDateTime.of(2014, 3, 10, 16, 12, 35, 70004235);
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        DateTime expected = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35);

        Assert.assertEquals(expected, DateFunctions.fromLocalDateTime(dateTime, 0));
        Assert.assertEquals(expected, DateFunctions.fromInstant(instant, 0));
        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.of(2014, 3, 10, 11, 12, 35, 70004235, ZoneOffset.ofHours(-5)), 0));
        Assert.assertEquals(expected, DateFunctions.fromZonedDateTime(ZonedDateTime.of(dateTime, ZoneId.of("UTC")), 0));

        // equality alone would not catch a subsecond date carrying a null subsecond, so check the
        // granularity and the rendering too
        DateTime date = DateFunctions.fromInstant(instant, 0);
        Assert.assertTrue(date.getClass().getName(), date instanceof DateWithSecond);
        assertGranularity(date, true, true, true, true, true, false);
        Assert.assertNull(date.getSubsecond());
        Assert.assertEquals("2014-03-10T16:12:35+0000", date.toString());
        Assert.assertEquals(ModelRepository.DATETIME_TYPE_NAME, DateFunctions.datePrimitiveType(date));
        Assert.assertEquals(0, DateFunctions.compare(date, expected));

        // DateWithSubsecond rejects a precision of 0, as it does any other precision outside 1-9
        Assert.assertEquals(
                "Invalid subsecond precision: 0",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateWithSubsecond.fromInstant(instant, 0)).getMessage());
        Assert.assertEquals(
                "Invalid subsecond precision: -1",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateWithSubsecond.fromInstant(instant, -1)).getMessage());
        Assert.assertEquals(
                "Invalid subsecond precision: 10",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateWithSubsecond.fromInstant(instant, 10)).getMessage());
    }

    /**
     * Every route into a Pure date must agree on the same instant, whichever Java type it starts
     * from and whatever zone or offset that type expresses the instant in.
     */
    @Test
    public void testJavaTimeConversionsAgreeWithEachOther()
    {
        Instant instant = LocalDateTime.of(2014, 3, 10, 16, 12, 35, 70004235).toInstant(ZoneOffset.UTC);
        DateTime expected = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");

        Assert.assertEquals(expected, DateFunctions.fromInstant(instant));
        Assert.assertEquals(expected, DateFunctions.fromLocalDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC)));
        Assert.assertEquals(expected, DateFunctions.fromOffsetDateTime(OffsetDateTime.ofInstant(instant, ZoneOffset.ofHours(-5))));
        Assert.assertEquals(expected, DateFunctions.fromZonedDateTime(ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Tokyo"))));
        Assert.assertEquals(expected, DateFunctions.parsePureDate("2014-03-10T16:12:35.070004235"));

        // and the coarser granularities line up with the components of that instant
        Assert.assertEquals(DateFunctions.fromYear(java.time.Year.of(2014)), DateFunctions.parsePureDate("2014"));
        Assert.assertEquals(DateFunctions.fromYearMonth(java.time.YearMonth.of(2014, 3)), DateFunctions.parsePureDate("2014-03"));
        Assert.assertEquals(DateFunctions.fromLocalDate(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate()), DateFunctions.parsePureDate("2014-03-10"));
    }

    @Test
    public void testToday()
    {
        StrictDate today = DateFunctions.today();
        Assert.assertTrue(today.hasDay());
        Assert.assertFalse(today.hasHour());
        Assert.assertEquals(ModelRepository.STRICT_DATE_TYPE_NAME, DateFunctions.datePrimitiveType(today));

        // allow a day either side so that the test cannot fail if the UTC date rolls over mid-test
        LocalDate now = LocalDate.now(java.time.Clock.systemUTC());
        Assert.assertTrue(
                today.toString(),
                today.equals(DateFunctions.newPureDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth())) ||
                        today.equals(DateFunctions.newPureDate(now.minusDays(1).getYear(), now.minusDays(1).getMonthValue(), now.minusDays(1).getDayOfMonth())) ||
                        today.equals(DateFunctions.newPureDate(now.plusDays(1).getYear(), now.plusDays(1).getMonthValue(), now.plusDays(1).getDayOfMonth())));
    }

    @Test
    public void testDatePrimitiveType()
    {
        Assert.assertEquals(ModelRepository.DATE_TYPE_NAME, DateFunctions.datePrimitiveType(DateFunctions.newPureDate(2014)));
        Assert.assertEquals(ModelRepository.DATE_TYPE_NAME, DateFunctions.datePrimitiveType(DateFunctions.newPureDate(2014, 3)));
        Assert.assertEquals(ModelRepository.STRICT_DATE_TYPE_NAME, DateFunctions.datePrimitiveType(DateFunctions.newPureDate(2014, 3, 10)));
        Assert.assertEquals(ModelRepository.DATETIME_TYPE_NAME, DateFunctions.datePrimitiveType(DateFunctions.newPureDate(2014, 3, 10, 16)));
        Assert.assertEquals(ModelRepository.DATETIME_TYPE_NAME, DateFunctions.datePrimitiveType(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070")));
        Assert.assertEquals(ModelRepository.LATEST_DATE_TYPE_NAME, DateFunctions.datePrimitiveType(LatestDate.instance));
    }

    // Parsing

    @Test
    public void testParsePureDate()
    {
        Assert.assertEquals(DateFunctions.newPureDate(2014), DateFunctions.parsePureDate("2014"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), DateFunctions.parsePureDate("2014-03"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFunctions.parsePureDate("2014-03-10"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16), DateFunctions.parsePureDate("2014-03-10T16"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12), DateFunctions.parsePureDate("2014-03-10T16:12"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35), DateFunctions.parsePureDate("2014-03-10T16:12:35"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235"), DateFunctions.parsePureDate("2014-03-10T16:12:35.070004235"));

        // the Pure date prefix and surrounding whitespace are tolerated
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFunctions.parsePureDate("%2014-03-10"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFunctions.parsePureDate("  2014-03-10  "));

        // negative years
        Assert.assertEquals(DateFunctions.newPureDate(-1), DateFunctions.parsePureDate("-1"));

        // time zone offsets are normalized to UTC
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35), DateFunctions.parsePureDate("2014-03-10T11:12:35-0500"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12), DateFunctions.parsePureDate("2014-03-10T17:12+0100"));
    }

    @Test
    public void testParsePureDateRoundTrip()
    {
        String[] strings = {"2014", "2014-03", "2014-03-10", "2014-03-10T16", "2014-03-10T16:12+0000", "2014-03-10T16:12:35+0000", "2014-03-10T16:12:35.070004235+0000"};
        for (String string : strings)
        {
            Assert.assertEquals(string, string, DateFunctions.parsePureDate(string).toString());
        }
    }

    // Helpers

    @SuppressWarnings("deprecation")
    private static void assertRoundTripsThroughCalendar(PureDate date, int precision)
    {
        Assert.assertEquals(date, DateFunctions.fromCalendar(date.getCalendar(), precision));
    }

    private static void assertGranularity(PureDate date, boolean month, boolean day, boolean hour, boolean minute, boolean second, boolean subsecond)
    {
        Assert.assertEquals(date + " month", month, date.hasMonth());
        Assert.assertEquals(date + " day", day, date.hasDay());
        Assert.assertEquals(date + " hour", hour, date.hasHour());
        Assert.assertEquals(date + " minute", minute, date.hasMinute());
        Assert.assertEquals(date + " second", second, date.hasSecond());
        Assert.assertEquals(date + " subsecond", subsecond, date.hasSubsecond());
    }
}
