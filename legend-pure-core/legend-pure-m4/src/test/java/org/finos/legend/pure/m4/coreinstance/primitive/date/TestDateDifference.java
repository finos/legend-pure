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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DateFunctions#dateDifference(PureDate, PureDate, String)}, which backs the Pure
 * function {@code meta::pure::functions::date::dateDiff} on both engines.
 *
 * <h2>What the units mean</h2>
 *
 * <p>A Pure date covers a span of time rather than naming an instant, so it is read here as the
 * first instant of that span: a date with no month starts in January, one with no day starts on the
 * first, and one with no time starts at midnight. The units then fall into two groups.
 *
 * <p><b>Calendar units count boundaries crossed, ignoring everything finer.</b> YEARS is the
 * difference of the two year numbers, MONTHS the difference of the two months counted from year
 * zero, and DAYS the difference of the two calendar days. So 2015-12-31T23:59:59 to
 * 2016-01-01T00:00:01 is one YEAR even though only two seconds passed, and 2016-02-01 to 2016-02-29
 * is zero MONTHS even though almost a month passed.
 *
 * <p><b>Time units count elapsed time, truncated toward zero.</b> HOURS, MINUTES, SECONDS,
 * MILLISECONDS, MICROSECONDS, and NANOSECONDS all measure how much time actually passed and drop
 * the remainder, so 13:00:00 to 13:01:01 is one MINUTE and 23:00:00 to 01:59:59 the next day is two
 * HOURS.
 *
 * <p><b>WEEKS counts Sundays passed</b>, and is the one unit that is not symmetric. Counting
 * forwards, a Sunday is counted if it falls after the first date and on or before the second.
 * Counting backwards, it is counted if it falls on or after the second date and before the first.
 * So Saturday to the following Sunday is 1, while that Sunday back to the Saturday is 0 rather than
 * -1. This is longstanding behavior pinned by the PCT tests.
 *
 * <p>The sign follows the direction: the result is positive when the second date is the later of
 * the two, negative when it is the earlier, and zero when neither is.
 *
 * <h2>Relationship to the PCT tests</h2>
 *
 * <p>{@link #testContractYears()} through {@link #testContractDifferentTimeZones()} mirror, case for
 * case, the {@code <<PCT.test>>} functions in
 * {@code platform/pure/essential/date/operation/dateDiff.pure}. Those run against both engines and
 * are the real contract; these run in milliseconds and fail here first. Keep the two in step.
 */
public class TestDateDifference
{
    private static final ListIterable<String> UNITS = Lists.immutable.with(
            "YEARS", "MONTHS", "WEEKS", "DAYS", "HOURS", "MINUTES", "SECONDS", "MILLISECONDS", "MICROSECONDS", "NANOSECONDS");

    // The contract, mirroring dateDiff.pure

    @Test
    public void testContractYears()
    {
        assertDateDifference(1, "2015", "2016", "YEARS");
        assertDateDifference(-1, "2016", "2015", "YEARS");
        assertDateDifference(20, "2000", "2020", "YEARS");
        assertDateDifference(-20, "2020", "2000", "YEARS");
        assertDateDifference(0, "2015", "2015", "YEARS");
        assertDateDifference(0, "2015-01-01T00:00:00", "2015-12-31T23:59:59", "YEARS");
        assertDateDifference(1, "2015-12-31T23:59:59", "2016-01-01T00:00:01", "YEARS");
    }

    @Test
    public void testContractMonths()
    {
        assertDateDifference(0, "2016-02-01", "2016-02-01", "MONTHS");
        assertDateDifference(0, "2016-02-01", "2016-02-29", "MONTHS");
        assertDateDifference(1, "2016-02-01", "2016-03-01", "MONTHS");
        assertDateDifference(-1, "2016-03-01", "2016-02-01", "MONTHS");
        assertDateDifference(12, "2015-01-29", "2016-01-29", "MONTHS");
        assertDateDifference(14, "2015-01-29", "2016-03-29", "MONTHS");
        assertDateDifference(-14, "2016-03-29", "2015-01-29", "MONTHS");
        assertDateDifference(0, "2014-12-01T00:00:00", "2014-12-01T23:59:59", "MONTHS");
        assertDateDifference(11, "2016-01-01", "2016-12-31", "MONTHS");
        assertDateDifference(11, "2016-01-31", "2016-12-31", "MONTHS");
        assertDateDifference(12, "2016-01-01", "2017-01-01", "MONTHS");
    }

    @Test
    public void testContractWeeks()
    {
        assertDateDifference(0, "2015-07-05", "2015-07-05", "WEEKS");
        assertDateDifference(0, "2015-07-03", "2015-07-04", "WEEKS");
        assertDateDifference(1, "2015-07-04", "2015-07-05", "WEEKS");
        assertDateDifference(0, "2015-07-05", "2015-07-04", "WEEKS");
        assertDateDifference(1, "2015-07-05", "2015-07-12", "WEEKS");
        assertDateDifference(-1, "2015-07-12", "2015-07-05", "WEEKS");
        assertDateDifference(0, "2015-07-12", "2015-07-06", "WEEKS");
        assertDateDifference(4, "2015-07-05", "2015-08-02", "WEEKS");
        assertDateDifference(-4, "2015-08-02", "2015-07-05", "WEEKS");
        assertDateDifference(-3, "2015-08-02", "2015-07-06", "WEEKS");
        assertDateDifference(1, "2014-12-28", "2015-01-04", "WEEKS");
        assertDateDifference(52, "2015-01-01", "2016-01-01", "WEEKS");
        assertDateDifference(52, "2016-01-01", "2016-12-31", "WEEKS");
        assertDateDifference(53, "2016-01-01", "2017-01-01", "WEEKS");
    }

    @Test
    public void testContractDays()
    {
        assertDateDifference(0, "2015-07-07", "2015-07-07", "DAYS");
        assertDateDifference(1, "2015-07-07", "2015-07-08", "DAYS");
        assertDateDifference(-1, "2015-07-08", "2015-07-07", "DAYS");
        assertDateDifference(365, "2015-01-1", "2016-01-01", "DAYS");
        assertDateDifference(366, "2016-01-1", "2017-01-01", "DAYS");
        assertDateDifference(394, "2014-01-31", "2015-03-01", "DAYS");
        assertDateDifference(395, "2016-01-31", "2017-03-01", "DAYS");
        assertDateDifference(-395, "2017-03-01", "2016-01-31", "DAYS");
        assertDateDifference(7, "2014-12-28", "2015-01-04", "DAYS");
        assertDateDifference(-7, "2015-01-04", "2014-12-28", "DAYS");
    }

    @Test
    public void testContractHours()
    {
        assertDateDifference(0, "2015-07-07T13:00:00", "2015-07-07T13:00:00", "HOURS");
        assertDateDifference(1, "2015-07-07T13:00:00", "2015-07-07T14:00:00", "HOURS");
        assertDateDifference(-1, "2015-07-07T14:00:00", "2015-07-07T13:00:00", "HOURS");
        assertDateDifference(2, "2015-07-07T23:00:00", "2015-07-08T01:00:00", "HOURS");
        assertDateDifference(2, "2015-07-07T23:00:00", "2015-07-08T01:59:59", "HOURS");
        assertDateDifference(24, "2014-12-31T23:00:00", "2015-01-01T23:00:00", "HOURS");
        assertDateDifference(0, "2014-12-01T00:00:00", "2014-12-01T00:59:59", "HOURS");
    }

    @Test
    public void testContractMinutes()
    {
        assertDateDifference(0, "2015-07-07T13:00:00", "2015-07-07T13:00:00", "MINUTES");
        assertDateDifference(1, "2015-07-07T13:00:00", "2015-07-07T13:01:00", "MINUTES");
        assertDateDifference(-1, "2015-07-07T13:01:00", "2015-07-07T13:00:00", "MINUTES");
        assertDateDifference(1, "2015-07-07T13:00:00", "2015-07-07T13:01:01", "MINUTES");
        assertDateDifference(61, "2015-07-07T13:00:00", "2015-07-07T14:01:00", "MINUTES");
        assertDateDifference(120, "2015-07-07T23:00:00", "2015-07-08T01:00:00", "MINUTES");
        assertDateDifference(0, "2014-12-01T00:00:00", "2014-12-01T00:00:59", "MINUTES");
    }

    @Test
    public void testContractSeconds()
    {
        assertDateDifference(0, "2015-07-07T13:00:00", "2015-07-07T13:00:00", "SECONDS");
        assertDateDifference(1, "2015-07-07T13:00:00", "2015-07-07T13:00:01", "SECONDS");
        assertDateDifference(-1, "2015-07-07T13:00:01", "2015-07-07T13:00:00", "SECONDS");
        assertDateDifference(60, "2015-07-07T13:00:00", "2015-07-07T13:01:00", "SECONDS");
        assertDateDifference(61, "2015-07-07T13:00:00", "2015-07-07T13:01:01", "SECONDS");
        assertDateDifference(3661, "2015-07-07T13:00:00", "2015-07-07T14:01:01", "SECONDS");
        assertDateDifference(7200, "2015-07-07T23:00:00", "2015-07-08T01:00:00", "SECONDS");
    }

    @Test
    public void testContractMilliseconds()
    {
        assertDateDifference(0, "2015-07-07T13:00:00", "2015-07-07T13:00:00", "MILLISECONDS");
        assertDateDifference(1000, "2015-07-07T13:00:00", "2015-07-07T13:00:01", "MILLISECONDS");
        assertDateDifference(-1000, "2015-07-07T13:00:01", "2015-07-07T13:00:00", "MILLISECONDS");
        assertDateDifference(60000, "2015-07-07T13:00:00", "2015-07-07T13:01:00", "MILLISECONDS");
        assertDateDifference(61000, "2015-07-07T13:00:00", "2015-07-07T13:01:01", "MILLISECONDS");
        assertDateDifference(3661000, "2015-07-07T13:00:00", "2015-07-07T14:01:01", "MILLISECONDS");
        assertDateDifference(7200000, "2015-07-07T23:00:00", "2015-07-08T01:00:00", "MILLISECONDS");
    }

    /**
     * MICROSECONDS and NANOSECONDS have no {@code <<PCT.test>>} of their own, since {@code dateDiff}
     * did not accept them until they were added alongside the {@code adjust} units of the same names.
     * They behave as the other time units do.
     */
    @Test
    public void testMicrosecondsAndNanoseconds()
    {
        assertDateDifference(0, "2015-07-07T13:00:00.000000", "2015-07-07T13:00:00.000000", "MICROSECONDS");
        assertDateDifference(1, "2015-07-07T13:00:00.000000", "2015-07-07T13:00:00.000001", "MICROSECONDS");
        assertDateDifference(-1, "2015-07-07T13:00:00.000001", "2015-07-07T13:00:00.000000", "MICROSECONDS");
        assertDateDifference(1000, "2015-07-07T13:00:00.000", "2015-07-07T13:00:00.001", "MICROSECONDS");
        assertDateDifference(1000000, "2015-07-07T13:00:00", "2015-07-07T13:00:01", "MICROSECONDS");

        assertDateDifference(0, "2015-07-07T13:00:00.000000000", "2015-07-07T13:00:00.000000000", "NANOSECONDS");
        assertDateDifference(1, "2015-07-07T13:00:00.000000000", "2015-07-07T13:00:00.000000001", "NANOSECONDS");
        assertDateDifference(-1, "2015-07-07T13:00:00.000000001", "2015-07-07T13:00:00.000000000", "NANOSECONDS");
        assertDateDifference(1000, "2015-07-07T13:00:00.000000", "2015-07-07T13:00:00.000001", "NANOSECONDS");
        assertDateDifference(1000000000, "2015-07-07T13:00:00", "2015-07-07T13:00:01", "NANOSECONDS");

        // remainders finer than the unit are still dropped
        assertDateDifference(0, "2015-07-07T13:00:00.0000000001", "2015-07-07T13:00:00.0000009999", "MICROSECONDS");
        assertDateDifference(999, "2015-07-07T13:00:00.000000000", "2015-07-07T13:00:00.0000009999", "NANOSECONDS");
    }

    /**
     * Offsets are folded into UTC when the date is parsed, so two spellings of the same instant are
     * the same date and every difference between them is zero.
     */
    @Test
    public void testContractDifferentTimeZones()
    {
        PureDate newYork = DateFunctions.parsePureDate("2014-12-31T23:00:00.000-0500");
        PureDate paris = DateFunctions.parsePureDate("2015-1-1T5:00:00.000+0100");
        Assert.assertEquals(newYork, paris);

        Assert.assertEquals(0, DateFunctions.dateDifference(newYork, paris, "YEARS"));
        Assert.assertEquals(0, DateFunctions.dateDifference(newYork, paris, "MONTHS"));
        Assert.assertEquals(0, DateFunctions.dateDifference(newYork, paris, "HOURS"));
        Assert.assertEquals(0, DateFunctions.dateDifference(newYork, paris, "MINUTES"));
        Assert.assertEquals(0, DateFunctions.dateDifference(newYork, paris, "SECONDS"));
    }

    // Sign and symmetry

    /**
     * The sign says which way round the two dates are, for every unit.
     */
    @Test
    public void testSignFollowsTheDirection()
    {
        PureDate earlier = DateFunctions.parsePureDate("2015-07-07T13:00:00");
        PureDate later = DateFunctions.parsePureDate("2016-09-11T17:30:45");
        for (String unit : UNITS)
        {
            Assert.assertTrue(unit, DateFunctions.dateDifference(earlier, later, unit) > 0);
            Assert.assertTrue(unit, DateFunctions.dateDifference(later, earlier, unit) < 0);
            Assert.assertEquals(unit, 0, DateFunctions.dateDifference(earlier, earlier, unit));
            Assert.assertEquals(unit, 0, DateFunctions.dateDifference(later, later, unit));
        }
    }

    /**
     * Every unit but WEEKS gives the same magnitude in both directions.
     */
    @Test
    public void testAntisymmetry()
    {
        ListIterable<String> dates = Lists.immutable.with(
                "2014", "2014-03", "2014-03-10", "2015-07-05", "2015-07-06", "2015-08-02",
                "2016-02-29", "2015-07-07T13:00:00", "2015-07-07T13:00:00.123456789", "2017-03-01");
        for (String from : dates)
        {
            for (String to : dates)
            {
                for (String unit : UNITS)
                {
                    if (!"WEEKS".equals(unit))
                    {
                        String message = from + " <-> " + to + " in " + unit;
                        long forward = DateFunctions.dateDifference(parse(from), parse(to), unit);
                        long backward = DateFunctions.dateDifference(parse(to), parse(from), unit);
                        Assert.assertEquals(message, forward, -backward);
                    }
                }
            }
        }
    }

    /**
     * WEEKS is deliberately not antisymmetric: a Sunday is counted when it is the date being counted
     * to, but not when it is the date being counted from. The magnitudes differ by one exactly when
     * a Sunday sits on one of the two endpoints.
     */
    @Test
    public void testWeeksIsNotAntisymmetric()
    {
        // 2015-07-05 and 2015-07-12 are Sundays, 2015-07-04 a Saturday, 2015-07-06 a Monday
        assertDateDifference(1, "2015-07-04", "2015-07-05", "WEEKS");
        assertDateDifference(0, "2015-07-05", "2015-07-04", "WEEKS");

        assertDateDifference(1, "2015-07-06", "2015-07-12", "WEEKS");
        assertDateDifference(0, "2015-07-12", "2015-07-06", "WEEKS");

        // when neither endpoint is a Sunday the two directions do mirror
        assertDateDifference(1, "2015-07-04", "2015-07-06", "WEEKS");
        assertDateDifference(-1, "2015-07-06", "2015-07-04", "WEEKS");
    }

    /**
     * Sunday is the day the week count turns over, so a whole Monday to Saturday stretch counts as
     * no weeks at all while a single step across a Sunday counts as one.
     */
    @Test
    public void testWeeksCountsSundaysPassed()
    {
        // 2015-07-06 is a Monday, 2015-07-11 the Saturday of the same week
        assertDateDifference(0, "2015-07-06", "2015-07-11", "WEEKS");
        assertDateDifference(1, "2015-07-11", "2015-07-12", "WEEKS");

        // and one Sunday per week thereafter
        assertDateDifference(1, "2015-07-06", "2015-07-12", "WEEKS");
        assertDateDifference(2, "2015-07-06", "2015-07-19", "WEEKS");
        assertDateDifference(3, "2015-07-06", "2015-07-26", "WEEKS");

        // time of day is not consulted
        assertDateDifference(1, "2015-07-11T23:59:59", "2015-07-12T00:00:00", "WEEKS");
        assertDateDifference(0, "2015-07-12T00:00:00", "2015-07-12T23:59:59", "WEEKS");
    }

    // Which family a unit belongs to

    /**
     * The calendar units count boundaries crossed and ignore everything finer than the unit, while
     * the time units measure elapsed time. Nothing else pins this apart: the two readings agree
     * whenever the first date sits on a boundary of the unit, which every case in
     * {@code dateDiff.pure} does, so only operands offset from the boundary tell them apart.
     */
    @Test
    public void testTimeUnitsMeasureElapsedTimeNotBoundariesCrossed()
    {
        // the same two minutes: one DAY crossed, but zero HOURS, though an hour boundary was crossed too
        assertDateDifference(1, "2015-07-07T23:59:00", "2015-07-08T00:01:00", "DAYS");
        assertDateDifference(0, "2015-07-07T23:59:00", "2015-07-08T00:01:00", "HOURS");

        // two minutes across an hour boundary, and fifty-nine minutes within one, are both zero HOURS
        assertDateDifference(0, "2015-07-07T12:59:00", "2015-07-07T13:01:00", "HOURS");
        assertDateDifference(0, "2015-07-07T12:00:00", "2015-07-07T12:59:00", "HOURS");

        // likewise 200ms across a second boundary is zero SECONDS
        assertDateDifference(0, "2015-07-07T13:00:00.900", "2015-07-07T13:00:01.100", "SECONDS");

        // measured from a boundary, the two readings coincide again
        assertDateDifference(1, "2015-07-07T23:00:00", "2015-07-08T00:01:00", "HOURS");
        assertDateDifference(1, "2015-07-07T13:00:00.000", "2015-07-07T13:00:01.100", "SECONDS");

        // the calendar units ignore the time entirely, so any part of the later day counts
        assertDateDifference(1, "2015-07-07T00:00:00", "2015-07-08T00:00:01", "DAYS");
        assertDateDifference(1, "2015-07-31T23:59:59", "2015-08-01T00:00:00", "MONTHS");
        assertDateDifference(1, "2015-12-31T23:59:59", "2016-01-01T00:00:00", "YEARS");
    }

    // Granularity

    /**
     * A date coarser than the unit being measured is read as the first instant of the span it
     * covers, so a year starts in January on the first at midnight.
     */
    @Test
    public void testCoarseDatesAreReadAsTheStartOfTheirSpan()
    {
        // a year behaves as its first day
        assertDateDifference(0, "2014", "2014-01-01", "DAYS");
        assertDateDifference(0, "2014", "2014-01", "MONTHS");
        assertDateDifference(0, "2014", "2014-01-01T00:00:00", "SECONDS");
        assertDateDifference(365, "2014", "2015", "DAYS");
        assertDateDifference(12, "2014", "2015", "MONTHS");

        // a month behaves as its first day
        assertDateDifference(0, "2014-03", "2014-03-01", "DAYS");
        assertDateDifference(14, "2014-03", "2014-03-15", "DAYS");
        assertDateDifference(31, "2014-03", "2014-04", "DAYS");

        // a day behaves as midnight
        assertDateDifference(0, "2014-03-10", "2014-03-10T00:00:00", "SECONDS");
        assertDateDifference(3600, "2014-03-10", "2014-03-10T01:00:00", "SECONDS");
    }

    /**
     * Mixed granularities work in every unit. A month-granularity date has a month, so measuring
     * months against it is meaningful; a year-granularity date is taken to be January.
     */
    @Test
    public void testMixedGranularities()
    {
        assertDateDifference(5, "2014", "2014-06", "MONTHS");
        assertDateDifference(-5, "2014-06", "2014", "MONTHS");
        assertDateDifference(17, "2014", "2015-06", "MONTHS");
        assertDateDifference(5, "2014", "2014-06-15", "MONTHS");
        assertDateDifference(5, "2014", "2014-06-15T12:30:45", "MONTHS");

        assertDateDifference(2, "2014", "2016-06-15", "YEARS");
        assertDateDifference(-2, "2016-06-15", "2014", "YEARS");

        assertDateDifference(165, "2014", "2014-06-15", "DAYS");
        // 2014-06-15 is itself a Sunday, and is counted
        assertDateDifference(24, "2014", "2014-06-15", "WEEKS");
    }

    // Precision

    /**
     * Subsecond digits are honored down to the nanosecond, so a difference smaller than the unit
     * being measured truncates to zero instead of being rounded up by a coarser intermediate.
     */
    @Test
    public void testSubsecondPrecision()
    {
        // just under a millisecond short of a full second
        assertDateDifference(999, "2015-07-07T13:00:00.0009", "2015-07-07T13:00:01.0000", "MILLISECONDS");
        assertDateDifference(-999, "2015-07-07T13:00:01.0000", "2015-07-07T13:00:00.0009", "MILLISECONDS");

        // one nanosecond short of a full second
        assertDateDifference(999, "2015-07-07T13:00:00.000000001", "2015-07-07T13:00:01.000000000", "MILLISECONDS");

        // differences finer than the unit truncate toward zero
        assertDateDifference(0, "2015-07-07T13:00:00.000000001", "2015-07-07T13:00:00.000000002", "MILLISECONDS");
        assertDateDifference(0, "2015-07-07T13:00:00.999", "2015-07-07T13:00:01.000", "SECONDS");
        assertDateDifference(1, "2015-07-07T13:00:00.000", "2015-07-07T13:00:01.000", "SECONDS");

        // exact millisecond boundaries are not lost
        assertDateDifference(1, "2015-07-07T13:00:00.000", "2015-07-07T13:00:00.001", "MILLISECONDS");
        assertDateDifference(1500, "2015-07-07T13:00:00.000", "2015-07-07T13:00:01.500", "MILLISECONDS");

        // digits beyond the nanosecond do not contribute
        assertDateDifference(0, "2015-07-07T13:00:00.0000000001", "2015-07-07T13:00:00.0000000009", "MILLISECONDS");
    }

    /**
     * Spans crossing a day, a month, and a leap day, in units finer than the span itself.
     */
    @Test
    public void testSpansCrossingBoundaries()
    {
        assertDateDifference(3, "2014", "2017", "YEARS");
        assertDateDifference(14, "2014-03", "2015-05", "MONTHS");
        assertDateDifference(2, "2014-03-10", "2014-03-24", "WEEKS");
        assertDateDifference(365, "2014-03-10", "2015-03-10", "DAYS");
        assertDateDifference(366, "2015-03-10", "2016-03-10", "DAYS");
        assertDateDifference(29, "2016-02-01", "2016-03-01", "DAYS");
        assertDateDifference(28, "2015-02-01", "2015-03-01", "DAYS");

        assertDateDifference(25, "2014-03-10T16:12:35.000", "2014-03-11T17:12:35.000", "HOURS");
        assertDateDifference(90, "2014-03-10T16:12:35.000", "2014-03-10T17:42:35.000", "MINUTES");
        assertDateDifference(65, "2014-03-10T16:12:35.000", "2014-03-10T16:13:40.000", "SECONDS");
        assertDateDifference(1070, "2014-03-10T16:12:35.000", "2014-03-10T16:12:36.070", "MILLISECONDS");
    }

    // The relationship to adjust

    /**
     * {@code dateDiff} and {@code meta::pure::functions::date::adjust} are inverse when the unit
     * names the granularity that both dates have: measure the gap between two days in DAYS and
     * adjusting the first date by that many days lands exactly on the second.
     *
     * <p>Each unit that names a granularity is checked over every ordered pair of a run of dates at
     * that granularity, chosen to cross the boundary above it. {@code adjust} dispatches to the
     * {@code PureDate.addX} methods driven here, so this is the same arithmetic the Pure function
     * performs.
     *
     * <p>The invariant holds only on this diagonal, and the two ways off it both fail for reasons
     * fixed by design rather than by accident, so they are pinned in
     * {@link #testAdjustDoesNotInvertDateDifferenceOffTheDiagonal()}.
     */
    @Test
    public void testAdjustInvertsDateDifferenceAtMatchingGranularity()
    {
        assertAdjustInverts("YEARS", PureDate::addYears, yearDates());
        assertAdjustInverts("MONTHS", PureDate::addMonths, runOf(DateFunctions.newPureDate(2015, 11), 30, PureDate::addMonths));
        assertAdjustInverts("DAYS", PureDate::addDays, runOf(DateFunctions.newPureDate(2015, 11, 1), 200, PureDate::addDays));
        assertAdjustInverts("HOURS", PureDate::addHours, runOf(DateFunctions.newPureDate(2015, 12, 31, 20), 30, PureDate::addHours));
        assertAdjustInverts("MINUTES", PureDate::addMinutes, runOf(DateFunctions.newPureDate(2015, 12, 31, 23, 45), 30, PureDate::addMinutes));
        assertAdjustInverts("SECONDS", PureDate::addSeconds, runOf(DateFunctions.newPureDate(2015, 12, 31, 23, 59, 45), 30, PureDate::addSeconds));
        assertAdjustInverts("MILLISECONDS", PureDate::addMilliseconds, runOf(DateFunctions.newPureDate(2015, 12, 31, 23, 59, 59, "985"), 30, PureDate::addMilliseconds));
        assertAdjustInverts("MICROSECONDS", PureDate::addMicroseconds, runOf(DateFunctions.newPureDate(2015, 12, 31, 23, 59, 59, "999985"), 30, PureDate::addMicroseconds));
        assertAdjustInverts("NANOSECONDS", PureDate::addNanoseconds, runOf(DateFunctions.newPureDate(2015, 12, 31, 23, 59, 59, "999999985"), 30, PureDate::addNanoseconds));
    }

    /**
     * Off the diagonal the two functions are not inverse, in two distinct ways.
     *
     * <p>A unit finer than the date's granularity cannot be adjusted by at all, so {@code dateDiff}
     * returns a number {@code adjust} will not accept. A unit coarser than the granularity loses the
     * finer components, either because {@code dateDiff} truncates or because {@code adjust} clamps to
     * the end of a short month. WEEKS is never an inverse at any granularity, because adjusting by
     * weeks adds seven days while measuring in weeks counts Sundays.
     */
    @Test
    public void testAdjustDoesNotInvertDateDifferenceOffTheDiagonal()
    {
        // a unit finer than the granularity: dateDiff answers, adjust refuses
        PureDate year = DateFunctions.parsePureDate("2014");
        Assert.assertEquals(365, DateFunctions.dateDifference(year, DateFunctions.parsePureDate("2015"), "DAYS"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> year.addDays(365));
        Assert.assertThrows(UnsupportedOperationException.class, () -> year.addMonths(1));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.parsePureDate("2015-01").addDays(1));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.parsePureDate("2015-01-31").addHours(1));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.parsePureDate("2015-01-31T13:11:11").addMilliseconds(1));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.parsePureDate("2015-01-31T13:11:11.338").addMicroseconds(1));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.parsePureDate("2015-01-31T13:11:11.338000").addNanoseconds(1));

        // a unit coarser than the granularity: dateDiff truncates
        PureDate midnight = DateFunctions.parsePureDate("2015-04-15T00");
        PureDate midday = DateFunctions.parsePureDate("2015-04-15T12");
        Assert.assertEquals(0, DateFunctions.dateDifference(midnight, midday, "DAYS"));
        Assert.assertEquals(midnight, midnight.addDays(0));

        // a unit coarser than the granularity: adjust clamps to the end of a short month
        PureDate endOfJanuary = DateFunctions.parsePureDate("2015-01-31");
        PureDate firstOfMarch = DateFunctions.parsePureDate("2015-03-01");
        Assert.assertEquals(2, DateFunctions.dateDifference(endOfJanuary, firstOfMarch, "MONTHS"));
        Assert.assertEquals(DateFunctions.parsePureDate("2015-03-31"), endOfJanuary.addMonths(2));

        // WEEKS measures Sundays but adjusts by seven days
        PureDate saturday = DateFunctions.parsePureDate("2015-07-04");
        PureDate sunday = DateFunctions.parsePureDate("2015-07-05");
        Assert.assertEquals(1, DateFunctions.dateDifference(saturday, sunday, "WEEKS"));
        Assert.assertEquals(DateFunctions.parsePureDate("2015-07-11"), saturday.addWeeks(1));
    }

    /**
     * Dates of different granularity can never satisfy the invariant, whatever the unit or the
     * values: {@code adjust} returns a date of the granularity it was given, and a date is never
     * equal to one of a different granularity.
     */
    @Test
    public void testAdjustPreservesGranularitySoMixedGranularitiesCannotInvert()
    {
        PureDate year = DateFunctions.parsePureDate("2014");
        Assert.assertEquals(DateFunctions.parsePureDate("2015"), year.addYears(1));

        PureDate month = DateFunctions.parsePureDate("2014-06");
        Assert.assertEquals(DateFunctions.parsePureDate("2014-07"), month.addMonths(1));

        PureDate second = DateFunctions.parsePureDate("2015-01-31T13:55:21");
        Assert.assertEquals(DateFunctions.parsePureDate("2015-02-28T13:55:21"), second.addMonths(1));

        // so no amount of adjustment turns a year into a month, whatever the difference says
        long difference = DateFunctions.dateDifference(year, month, "MONTHS");
        Assert.assertEquals(5, difference);
        Assert.assertThrows(UnsupportedOperationException.class, () -> year.addMonths(difference));
    }

    // Units

    @Test
    public void testUnsupportedUnit()
    {
        PureDate from = DateFunctions.parsePureDate("2015-07-07");
        PureDate to = DateFunctions.parsePureDate("2015-07-08");

        Assert.assertEquals(
                "Unsupported duration unit: FORTNIGHTS",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.dateDifference(from, to, "FORTNIGHTS")).getMessage());
        Assert.assertEquals(
                "Unsupported duration unit: years",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.dateDifference(from, to, "years")).getMessage());

        // the unit is checked even when the two dates are the same
        Assert.assertEquals(
                "Unsupported duration unit: FORTNIGHTS",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFunctions.dateDifference(from, from, "FORTNIGHTS")).getMessage());
    }

    @Test
    public void testEveryDurationUnitIsSupported()
    {
        PureDate from = DateFunctions.parsePureDate("2015-07-07T13:00:00");
        PureDate to = DateFunctions.parsePureDate("2015-07-08T13:00:00");
        for (String unit : UNITS)
        {
            DateFunctions.dateDifference(from, to, unit);
        }
    }

    /**
     * The subsecond units can run out of room, and say so rather than wrapping. NANOSECONDS is the
     * one to watch: a long holds only about 292 years of them, which is a span real data can reach.
     */
    @Test
    public void testFineUnitsOverflowOnLargeSpans()
    {
        PureDate min = DateFunctions.newPureDate(java.time.Year.MIN_VALUE);
        PureDate max = DateFunctions.newPureDate(java.time.Year.MAX_VALUE);

        // over the full supported range every unit down to the second still fits
        Assert.assertEquals(1999999998L, DateFunctions.dateDifference(min, max, "YEARS"));
        Assert.assertEquals(23999999976L, DateFunctions.dateDifference(min, max, "MONTHS"));
        Assert.assertEquals(730484999269L, DateFunctions.dateDifference(min, max, "DAYS"));
        Assert.assertEquals(63113903936841600L, DateFunctions.dateDifference(min, max, "SECONDS"));

        Assert.assertThrows(ArithmeticException.class, () -> DateFunctions.dateDifference(min, max, "MILLISECONDS"));
        Assert.assertThrows(ArithmeticException.class, () -> DateFunctions.dateDifference(min, max, "MICROSECONDS"));
        Assert.assertThrows(ArithmeticException.class, () -> DateFunctions.dateDifference(min, max, "NANOSECONDS"));

        // NANOSECONDS gives out at 292 years
        PureDate start = parse("2000-01-01T00:00:00");
        Assert.assertEquals(9214646400000000000L, DateFunctions.dateDifference(start, parse("2292-01-01T00:00:00"), "NANOSECONDS"));
        Assert.assertThrows(ArithmeticException.class, () -> DateFunctions.dateDifference(start, parse("2293-01-01T00:00:00"), "NANOSECONDS"));
    }

    /**
     * {@link LatestDate} has no components to measure, in either position. Note that comparing it
     * with itself throws as well: there is no longer an equality short circuit ahead of the
     * measurement.
     */
    @Test
    public void testLatestDateIsNotSupported()
    {
        PureDate date = DateFunctions.parsePureDate("2015-07-07");
        Assert.assertThrows(UnsupportedOperationException.class, () -> LatestDate.instance.dateDifference(date, "DAYS"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.dateDifference(LatestDate.instance, date, "DAYS"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.dateDifference(date, LatestDate.instance, "DAYS"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> DateFunctions.dateDifference(LatestDate.instance, LatestDate.instance, "DAYS"));
    }

    // Helpers

    private static PureDate parse(String string)
    {
        return DateFunctions.parsePureDate(string);
    }

    /**
     * Assert that adjusting the first date of each ordered pair by the measured difference lands on
     * the second.
     */
    private static void assertAdjustInverts(String unit, DateAdjuster adjuster, ListIterable<PureDate> dates)
    {
        for (PureDate from : dates)
        {
            for (PureDate to : dates)
            {
                long difference = DateFunctions.dateDifference(from, to, unit);
                Assert.assertEquals(
                        from + " adjusted by " + difference + " " + unit + " should be " + to,
                        to,
                        adjuster.adjust(from, difference));
            }
        }
    }

    /**
     * A run of consecutive dates at one granularity, starting from the given date.
     */
    private static ListIterable<PureDate> runOf(PureDate start, int count, DateAdjuster step)
    {
        MutableList<PureDate> dates = Lists.mutable.ofInitialCapacity(count);
        for (int i = 0; i < count; i++)
        {
            dates.add(step.adjust(start, i));
        }
        return dates;
    }

    private static ListIterable<PureDate> yearDates()
    {
        MutableList<PureDate> dates = Lists.mutable.empty();
        for (int year = -3; year <= 3; year++)
        {
            dates.add(DateFunctions.newPureDate(year));
        }
        for (int year : new int[]{1900, 1999, 2000, 2015, 2016, 2017, 2100})
        {
            dates.add(DateFunctions.newPureDate(year));
        }
        return dates;
    }

    private static void assertDateDifference(long expected, String from, String to, String unit)
    {
        PureDate fromDate = parse(from);
        PureDate toDate = parse(to);
        String message = from + " -> " + to + " in " + unit;
        Assert.assertEquals(message, expected, DateFunctions.dateDifference(fromDate, toDate, unit));
        Assert.assertEquals(message, expected, fromDate.dateDifference(toDate, unit));
    }

    /**
     * One of the {@code PureDate.addX} methods, which is what
     * {@code meta::pure::functions::date::adjust} dispatches to for a given duration unit.
     */
    private interface DateAdjuster
    {
        PureDate adjust(PureDate date, long amount);
    }
}
