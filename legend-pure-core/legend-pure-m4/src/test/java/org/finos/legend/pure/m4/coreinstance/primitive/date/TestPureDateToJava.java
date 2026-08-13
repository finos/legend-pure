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

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Tests for {@link PureDateToJava}, which resolves a Pure date - a span of time rather than an
 * instant - to a {@code java.time} temporal, at the start of that span, at the end of it, or just
 * after it.
 */
public class TestPureDateToJava
{
    private static final PureDateToJava START = PureDateToJava.start();
    private static final PureDateToJava END = PureDateToJava.end();
    private static final PureDateToJava EXCLUSIVE_END = PureDateToJava.exclusiveEnd();

    /**
     * The same date written at every granularity, each one covering a span contained in the span of
     * the one before it. The last two carry more than nanosecond precision, and their final digit is
     * one that rounding would carry up into the next nanosecond.
     */
    private static final PureDate[] NESTED_DATES = {
            DateFunctions.newPureDate(2014),
            DateFunctions.newPureDate(2014, 3),
            DateFunctions.newPureDate(2014, 3, 10),
            DateFunctions.newPureDate(2014, 3, 10, 16),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "0"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "0700"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07000"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "0700042"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07000423"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "0700042359"),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235912345")
    };

    private static final PureDateToJava[] RESOLUTIONS = {START, END, EXCLUSIVE_END};

    // Factories

    @Test
    public void testResolversAreShared()
    {
        // the resolvers hold no state, so there is no reason to hand out a new one each time
        Assert.assertSame(START, PureDateToJava.start());
        Assert.assertSame(END, PureDateToJava.end());
        Assert.assertSame(EXCLUSIVE_END, PureDateToJava.exclusiveEnd());
        Assert.assertNotSame(START, END);
        Assert.assertNotSame(END, EXCLUSIVE_END);
    }

    // Year

    @Test
    public void testToYear()
    {
        // a year has nothing finer for the two resolutions to disagree about
        for (PureDate date : NESTED_DATES)
        {
            Assert.assertEquals(date.toString(), Year.of(2014), START.toYear(date));
            Assert.assertEquals(date.toString(), Year.of(2014), END.toYear(date));
        }

        // year zero, negative years, and both ends of the supported range
        Assert.assertEquals(Year.of(0), START.toYear(DateFunctions.newPureDate(0)));
        Assert.assertEquals(Year.of(-44), START.toYear(DateFunctions.newPureDate(-44, 3, 15)));
        Assert.assertEquals(Year.of(-44), END.toYear(DateFunctions.newPureDate(-44, 3, 15)));
        Assert.assertEquals(Year.of(Year.MIN_VALUE), START.toYear(DateFunctions.newPureDate(Year.MIN_VALUE)));
        Assert.assertEquals(Year.of(Year.MAX_VALUE), END.toYear(DateFunctions.newPureDate(Year.MAX_VALUE)));
    }

    // YearMonth

    @Test
    public void testToYearMonth()
    {
        // a date with no month resolves to the first or the last month of the year
        PureDate year = DateFunctions.newPureDate(2014);
        Assert.assertEquals(YearMonth.of(2014, 1), START.toYearMonth(year));
        Assert.assertEquals(YearMonth.of(2014, 12), END.toYearMonth(year));

        // a date that has a month keeps it, whichever resolution is asked for
        for (int i = 1; i < NESTED_DATES.length; i++)
        {
            PureDate date = NESTED_DATES[i];
            Assert.assertEquals(date.toString(), YearMonth.of(2014, 3), START.toYearMonth(date));
            Assert.assertEquals(date.toString(), YearMonth.of(2014, 3), END.toYearMonth(date));
        }

        Assert.assertEquals(YearMonth.of(2014, 1), END.toYearMonth(DateFunctions.newPureDate(2014, 1)));
        Assert.assertEquals(YearMonth.of(2014, 12), START.toYearMonth(DateFunctions.newPureDate(2014, 12)));
    }

    // LocalDate

    @Test
    public void testToLocalDate()
    {
        // a date with no month resolves to the first or the last day of the year
        PureDate year = DateFunctions.newPureDate(2014);
        Assert.assertEquals(LocalDate.of(2014, 1, 1), START.toLocalDate(year));
        Assert.assertEquals(LocalDate.of(2014, 12, 31), END.toLocalDate(year));

        // a date that has a day keeps it, whichever resolution is asked for
        for (int i = 2; i < NESTED_DATES.length; i++)
        {
            PureDate date = NESTED_DATES[i];
            Assert.assertEquals(date.toString(), LocalDate.of(2014, 3, 10), START.toLocalDate(date));
            Assert.assertEquals(date.toString(), LocalDate.of(2014, 3, 10), END.toLocalDate(date));
        }
    }

    /**
     * The end of a month is its own last day, which is not the same for every month, and for
     * February not the same in every year.
     */
    @Test
    public void testToLocalDateResolvesTheEndOfEachMonth()
    {
        int[] lastDayOfMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int month = 1; month <= 12; month++)
        {
            PureDate date = DateFunctions.newPureDate(2014, month);
            Assert.assertEquals(date.toString(), LocalDate.of(2014, month, 1), START.toLocalDate(date));
            Assert.assertEquals(date.toString(), LocalDate.of(2014, month, lastDayOfMonth[month - 1]), END.toLocalDate(date));
        }

        // February gains a day in a leap year, and 1900 is not one while 2000 is
        Assert.assertEquals(LocalDate.of(2016, 2, 29), END.toLocalDate(DateFunctions.newPureDate(2016, 2)));
        Assert.assertEquals(LocalDate.of(1900, 2, 28), END.toLocalDate(DateFunctions.newPureDate(1900, 2)));
        Assert.assertEquals(LocalDate.of(2000, 2, 29), END.toLocalDate(DateFunctions.newPureDate(2000, 2)));
    }

    // Instant

    @Test
    public void testToInstantAtEachGranularity()
    {
        assertInstants(DateFunctions.newPureDate(2014), utc(2014, 1, 1, 0, 0, 0, 0), utc(2014, 12, 31, 23, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3), utc(2014, 3, 1, 0, 0, 0, 0), utc(2014, 3, 31, 23, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3, 10), utc(2014, 3, 10, 0, 0, 0, 0), utc(2014, 3, 10, 23, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3, 10, 16), utc(2014, 3, 10, 16, 0, 0, 0), utc(2014, 3, 10, 16, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3, 10, 16, 12), utc(2014, 3, 10, 16, 12, 0, 0), utc(2014, 3, 10, 16, 12, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35), utc(2014, 3, 10, 16, 12, 35, 0), utc(2014, 3, 10, 16, 12, 35, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235"), utc(2014, 3, 10, 16, 12, 35, 70_004_235), utc(2014, 3, 10, 16, 12, 35, 70_004_235));

        // resolving a month with no day has to know how long that month is
        assertInstants(DateFunctions.newPureDate(2016, 2), utc(2016, 2, 1, 0, 0, 0, 0), utc(2016, 2, 29, 23, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(2014, 2), utc(2014, 2, 1, 0, 0, 0, 0), utc(2014, 2, 28, 23, 59, 59, 999_999_999));
    }

    /**
     * The subsecond is the digits after the decimal point, so its length is the precision: a date
     * ending .07 covers everything from .070000000 to .079999999.
     */
    @Test
    public void testToInstantResolvesTheSubsecondToNineDigits()
    {
        assertSubsecondInstants("0", 0, 99_999_999);
        assertSubsecondInstants("9", 900_000_000, 999_999_999);
        assertSubsecondInstants("07", 70_000_000, 79_999_999);
        assertSubsecondInstants("070", 70_000_000, 70_999_999);
        assertSubsecondInstants("0700", 70_000_000, 70_099_999);
        assertSubsecondInstants("07000", 70_000_000, 70_009_999);
        assertSubsecondInstants("070004", 70_004_000, 70_004_999);
        assertSubsecondInstants("0700042", 70_004_200, 70_004_299);
        assertSubsecondInstants("07000423", 70_004_230, 70_004_239);

        // with all nine digits there is nothing left to resolve, and the two ends meet
        assertSubsecondInstants("070004235", 70_004_235, 70_004_235);
        assertSubsecondInstants("000000000", 0, 0);
        assertSubsecondInstants("999999999", 999_999_999, 999_999_999);
    }

    /**
     * A date of finer than nanosecond precision covers a span lying wholly within one nanosecond, so
     * the digits beyond the ninth are dropped and both ends resolve to that nanosecond. Rounding
     * them instead would resolve the date to an instant outside the span it stands for.
     */
    @Test
    public void testToInstantDropsSubsecondDigitsBeyondTheNanosecond()
    {
        assertSubsecondInstants("0700042351", 70_004_235, 70_004_235);
        assertSubsecondInstants("0700042359", 70_004_235, 70_004_235);
        assertSubsecondInstants("070004235123456", 70_004_235, 70_004_235);

        // the digits that would round up are exactly the ones that would leave the span
        assertSubsecondInstants("0000000005", 0, 0);
        assertSubsecondInstants("0000000009", 0, 0);
        assertSubsecondInstants("9999999995", 999_999_999, 999_999_999);
        assertSubsecondInstants("9999999999", 999_999_999, 999_999_999);
    }

    /**
     * Rounding the subsecond up would carry a date past the end of the second it is in, and so past
     * the end of every coarser date containing it: the end of 2014 would come out as the start of
     * 2015.
     */
    @Test
    public void testToInstantOfATrailingSubsecondStaysWithinTheDate()
    {
        PureDate date = DateFunctions.newPureDate(2014, 12, 31, 23, 59, 59, "9999999995");
        assertInstants(date, utc(2014, 12, 31, 23, 59, 59, 999_999_999), utc(2014, 12, 31, 23, 59, 59, 999_999_999));

        Assert.assertEquals(2014, START.toYear(date).getValue());
        Assert.assertEquals(2014, END.toYear(date).getValue());
        assertResolvesWithin(DateFunctions.newPureDate(2014), date);
        assertResolvesWithin(DateFunctions.newPureDate(2014, 12, 31, 23, 59, 59), date);
        assertResolvesWithin(DateFunctions.newPureDate(2014, 12, 31, 23, 59, 59, "999999999"), date);
    }

    @Test
    public void testToInstantAtTheEndsOfTheSupportedRange()
    {
        PureDate maxYear = DateFunctions.newPureDate(Year.MAX_VALUE);
        assertInstants(maxYear, utc(Year.MAX_VALUE, 1, 1, 0, 0, 0, 0), utc(Year.MAX_VALUE, 12, 31, 23, 59, 59, 999_999_999));
        Assert.assertTrue(END.toInstant(maxYear).isBefore(Instant.MAX));

        PureDate minYear = DateFunctions.newPureDate(Year.MIN_VALUE);
        assertInstants(minYear, utc(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0), utc(Year.MIN_VALUE, 12, 31, 23, 59, 59, 999_999_999));
        Assert.assertTrue(START.toInstant(minYear).isAfter(Instant.MIN));

        // year zero and negative years are ordinary years as far as resolution goes
        assertInstants(DateFunctions.newPureDate(0), utc(0, 1, 1, 0, 0, 0, 0), utc(0, 12, 31, 23, 59, 59, 999_999_999));
        assertInstants(DateFunctions.newPureDate(-44, 2), utc(-44, 2, 1, 0, 0, 0, 0), utc(-44, 2, 29, 23, 59, 59, 999_999_999));
    }

    // LocalDateTime and OffsetDateTime

    /**
     * A Pure date carries no time zone and is always understood as UTC, so the local, offset, and
     * instant conversions all name the same point; only the offset conversion says so.
     */
    @Test
    public void testToLocalDateTimeAndToOffsetDateTime()
    {
        PureDate date = DateFunctions.newPureDate(2014, 3, 10, 16);
        Assert.assertEquals(LocalDateTime.of(2014, 3, 10, 16, 0, 0, 0), START.toLocalDateTime(date));
        Assert.assertEquals(LocalDateTime.of(2014, 3, 10, 16, 59, 59, 999_999_999), END.toLocalDateTime(date));
        Assert.assertEquals(OffsetDateTime.of(2014, 3, 10, 16, 0, 0, 0, ZoneOffset.UTC), START.toOffsetDateTime(date));
        Assert.assertEquals(OffsetDateTime.of(2014, 3, 10, 16, 59, 59, 999_999_999, ZoneOffset.UTC), END.toOffsetDateTime(date));

        for (PureDate nested : NESTED_DATES)
        {
            for (PureDateToJava resolution : RESOLUTIONS)
            {
                LocalDateTime local = resolution.toLocalDateTime(nested);
                Assert.assertEquals(nested.toString(), local.toInstant(ZoneOffset.UTC), resolution.toInstant(nested));
                Assert.assertEquals(nested.toString(), local.atOffset(ZoneOffset.UTC), resolution.toOffsetDateTime(nested));
            }
        }
    }

    /**
     * Each conversion is the inverse of the {@link DateFunctions} conversion into a Pure date, down
     * to the granularity converted to: a date carrying all nine subsecond digits comes back
     * unchanged, and a coarser conversion comes back as the Pure date of that granularity.
     */
    @Test
    public void testTheConversionsInvertTheOnesIntoAPureDate()
    {
        PureDate date = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        Assert.assertEquals(date, DateFunctions.fromLocalDateTime(START.toLocalDateTime(date)));
        Assert.assertEquals(date, DateFunctions.fromInstant(START.toInstant(date)));
        Assert.assertEquals(date, DateFunctions.fromOffsetDateTime(START.toOffsetDateTime(date)));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFunctions.fromLocalDate(START.toLocalDate(date)));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), DateFunctions.fromYearMonth(START.toYearMonth(date)));
        Assert.assertEquals(DateFunctions.newPureDate(2014), DateFunctions.fromYear(START.toYear(date)));
    }

    // Exclusive end

    /**
     * The exclusive end is the first value of the requested type after the span, so a date covers
     * the half-open range from its start to its exclusive end.
     */
    @Test
    public void testToExclusiveEnd()
    {
        PureDate year = DateFunctions.newPureDate(2014);
        Assert.assertEquals(Year.of(2015), EXCLUSIVE_END.toYear(year));
        Assert.assertEquals(YearMonth.of(2015, 1), EXCLUSIVE_END.toYearMonth(year));
        Assert.assertEquals(LocalDate.of(2015, 1, 1), EXCLUSIVE_END.toLocalDate(year));
        Assert.assertEquals(utc(2015, 1, 1, 0, 0, 0, 0), EXCLUSIVE_END.toInstant(year));

        PureDate hour = DateFunctions.newPureDate(2014, 3, 10, 16);
        Assert.assertEquals(Year.of(2015), EXCLUSIVE_END.toYear(hour));
        Assert.assertEquals(YearMonth.of(2014, 4), EXCLUSIVE_END.toYearMonth(hour));
        Assert.assertEquals(LocalDate.of(2014, 3, 11), EXCLUSIVE_END.toLocalDate(hour));
        Assert.assertEquals(utc(2014, 3, 10, 17, 0, 0, 0), EXCLUSIVE_END.toInstant(hour));

        // the last day of a month, of a leap February, and of a year all carry over
        Assert.assertEquals(LocalDate.of(2014, 4, 1), EXCLUSIVE_END.toLocalDate(DateFunctions.newPureDate(2014, 3, 31)));
        Assert.assertEquals(LocalDate.of(2016, 3, 1), EXCLUSIVE_END.toLocalDate(DateFunctions.newPureDate(2016, 2, 29)));
        Assert.assertEquals(LocalDate.of(2015, 1, 1), EXCLUSIVE_END.toLocalDate(DateFunctions.newPureDate(2014, 12, 31)));
        Assert.assertEquals(YearMonth.of(2015, 1), EXCLUSIVE_END.toYearMonth(DateFunctions.newPureDate(2014, 12)));
    }

    @Test
    public void testToExclusiveEndIsOneUnitPastTheEnd()
    {
        for (PureDate date : NESTED_DATES)
        {
            Assert.assertEquals(date.toString(), END.toYear(date).plusYears(1), EXCLUSIVE_END.toYear(date));
            Assert.assertEquals(date.toString(), END.toYearMonth(date).plusMonths(1), EXCLUSIVE_END.toYearMonth(date));
            Assert.assertEquals(date.toString(), END.toLocalDate(date).plusDays(1), EXCLUSIVE_END.toLocalDate(date));
            Assert.assertEquals(date.toString(), END.toLocalDateTime(date).plusNanos(1), EXCLUSIVE_END.toLocalDateTime(date));
            Assert.assertEquals(date.toString(), END.toInstant(date).plusNanos(1), EXCLUSIVE_END.toInstant(date));
            Assert.assertEquals(date.toString(), END.toOffsetDateTime(date).plusNanos(1), EXCLUSIVE_END.toOffsetDateTime(date));
        }
    }

    /**
     * Each exclusive end conversion names the first value of its own type after the span, so unlike
     * the other two resolutions they are not readings of one another: the first day after
     * 2014-03-10T16 is the 11th, while the first instant after it is 17:00 on the 10th.
     */
    @Test
    public void testTheExclusiveEndConversionsAreNotReadingsOfOneAnother()
    {
        PureDate date = DateFunctions.newPureDate(2014, 3, 10, 16);
        Assert.assertEquals(LocalDate.of(2014, 3, 11), EXCLUSIVE_END.toLocalDate(date));
        Assert.assertEquals(
                LocalDate.of(2014, 3, 10),
                LocalDateTime.ofInstant(EXCLUSIVE_END.toInstant(date), ZoneOffset.UTC).toLocalDate());
    }

    /**
     * There is nothing after the end of the supported range for the exclusive end to name, so at
     * the last supported year every conversion runs out rather than wrapping.
     */
    @Test
    public void testToExclusiveEndAtTheEndOfTheSupportedRange()
    {
        PureDate maxYear = DateFunctions.newPureDate(Year.MAX_VALUE);
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toYear(maxYear));
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toYearMonth(maxYear));
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toLocalDate(maxYear));
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toLocalDateTime(maxYear));
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toInstant(maxYear));
        Assert.assertThrows(DateTimeException.class, () -> EXCLUSIVE_END.toOffsetDateTime(maxYear));

        // a day short of the end there is still room
        PureDate lastButOneDay = DateFunctions.newPureDate(Year.MAX_VALUE, 12, 30);
        Assert.assertEquals(LocalDate.of(Year.MAX_VALUE, 12, 31), EXCLUSIVE_END.toLocalDate(lastButOneDay));
        Assert.assertEquals(utc(Year.MAX_VALUE, 12, 31, 0, 0, 0, 0), EXCLUSIVE_END.toInstant(lastButOneDay));
    }

    // Invariants

    @Test
    public void testStartIsNeverAfterEnd()
    {
        for (PureDate date : NESTED_DATES)
        {
            Assert.assertFalse(date.toString(), START.toInstant(date).isAfter(END.toInstant(date)));
            Assert.assertFalse(date.toString(), START.toLocalDate(date).isAfter(END.toLocalDate(date)));
            Assert.assertFalse(date.toString(), START.toYearMonth(date).isAfter(END.toYearMonth(date)));

            // and the exclusive end is past both of them
            Assert.assertTrue(date.toString(), EXCLUSIVE_END.toInstant(date).isAfter(END.toInstant(date)));
            Assert.assertTrue(date.toString(), EXCLUSIVE_END.toLocalDate(date).isAfter(END.toLocalDate(date)));
            Assert.assertTrue(date.toString(), EXCLUSIVE_END.toYearMonth(date).isAfter(END.toYearMonth(date)));
        }
    }

    /**
     * The end of a date is the last instant it includes, so the next span of the same granularity
     * starts one nanosecond later, which is where the exclusive end lands. Adjacent spans neither
     * overlap nor leave a gap.
     */
    @Test
    public void testEndIsOneNanosecondBeforeTheStartOfTheNextSpan()
    {
        assertAdjacent(DateFunctions.newPureDate(2014), DateFunctions.newPureDate(2015));
        assertAdjacent(DateFunctions.newPureDate(2014, 12), DateFunctions.newPureDate(2015, 1));
        assertAdjacent(DateFunctions.newPureDate(2014, 3), DateFunctions.newPureDate(2014, 4));
        assertAdjacent(DateFunctions.newPureDate(2014, 2, 28), DateFunctions.newPureDate(2014, 3, 1));
        assertAdjacent(DateFunctions.newPureDate(2016, 2, 29), DateFunctions.newPureDate(2016, 3, 1));
        assertAdjacent(DateFunctions.newPureDate(2014, 12, 31, 23), DateFunctions.newPureDate(2015, 1, 1, 0));
        assertAdjacent(DateFunctions.newPureDate(2014, 3, 10, 16, 59), DateFunctions.newPureDate(2014, 3, 10, 17, 0));
        assertAdjacent(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 59), DateFunctions.newPureDate(2014, 3, 10, 16, 13, 0));
        assertAdjacent(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "9"), DateFunctions.newPureDate(2014, 3, 10, 16, 12, 36, "0"));
        assertAdjacent(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235"), DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004236"));
    }

    /**
     * A finer date resolves inside every coarser date that contains it, at both ends. This is what
     * makes the two resolutions usable as the bounds of a range: a date cannot resolve to an instant
     * outside the span it stands for.
     */
    @Test
    public void testAFinerDateResolvesInsideTheDatesContainingIt()
    {
        for (int i = 0; i < NESTED_DATES.length; i++)
        {
            for (int j = i + 1; j < NESTED_DATES.length; j++)
            {
                assertResolvesWithin(NESTED_DATES[i], NESTED_DATES[j]);
            }
        }
    }

    /**
     * The coarser conversions are the finer one read at a coarser granularity: whichever end is
     * asked for, the year, month, and day of the resolved instant are what the other three
     * conversions give.
     */
    @Test
    public void testTheConversionsAgreeWithEachOther()
    {
        for (PureDate date : NESTED_DATES)
        {
            assertConversionsAgree(START, date);
            assertConversionsAgree(END, date);
        }
    }

    // LatestDate

    /**
     * {@link LatestDate} has no components and no fixed position on the time line, so there is
     * nothing to resolve and every conversion rejects it.
     */
    @Test
    public void testLatestDateIsNotSupported()
    {
        for (PureDateToJava resolution : RESOLUTIONS)
        {
            assertLatestDateRejected(() -> resolution.toYear(LatestDate.instance));
            assertLatestDateRejected(() -> resolution.toYearMonth(LatestDate.instance));
            assertLatestDateRejected(() -> resolution.toLocalDate(LatestDate.instance));
            assertLatestDateRejected(() -> resolution.toLocalDateTime(LatestDate.instance));
            assertLatestDateRejected(() -> resolution.toInstant(LatestDate.instance));
            assertLatestDateRejected(() -> resolution.toOffsetDateTime(LatestDate.instance));
        }
    }

    // getCalendar

    /**
     * {@link PureDate#getCalendar()} resolves a date to the start of its span, so the start
     * resolution agrees with it wherever a {@link java.util.GregorianCalendar} can hold the answer.
     * Beyond millisecond precision it cannot, which is one of the reasons to replace it.
     */
    @Test
    public void testStartAgreesWithGetCalendar()
    {
        PureDate[] dates = {
                DateFunctions.newPureDate(2014),
                DateFunctions.newPureDate(2014, 3),
                DateFunctions.newPureDate(2014, 3, 10),
                DateFunctions.newPureDate(2014, 3, 10, 16),
                DateFunctions.newPureDate(2014, 3, 10, 16, 12),
                DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35),
                DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "0"),
                DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07"),
                DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070")
        };
        for (PureDate date : dates)
        {
            Assert.assertEquals(date.toString(), date.getCalendar().toInstant(), START.toInstant(date));
        }

        PureDate nanoseconds = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        Assert.assertEquals(utc(2014, 3, 10, 16, 12, 35, 70_000_000), nanoseconds.getCalendar().toInstant());
        Assert.assertEquals(utc(2014, 3, 10, 16, 12, 35, 70_004_235), START.toInstant(nanoseconds));
    }

    // Helpers

    private static void assertLatestDateRejected(ThrowingRunnable conversion)
    {
        Assert.assertEquals(
                "Invalid operation for LatestDate",
                Assert.assertThrows(UnsupportedOperationException.class, conversion).getMessage());
    }

    private static void assertInstants(PureDate date, Instant start, Instant end)
    {
        Assert.assertEquals(date + " start", start, START.toInstant(date));
        Assert.assertEquals(date + " end", end, END.toInstant(date));
    }

    private static void assertSubsecondInstants(String subsecond, int startNanos, int endNanos)
    {
        assertInstants(
                DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, subsecond),
                utc(2014, 3, 10, 16, 12, 35, startNanos),
                utc(2014, 3, 10, 16, 12, 35, endNanos));
    }

    private static void assertAdjacent(PureDate date, PureDate next)
    {
        Assert.assertEquals(date + " then " + next, START.toInstant(next), END.toInstant(date).plusNanos(1));
        Assert.assertEquals(date + " then " + next, START.toInstant(next), EXCLUSIVE_END.toInstant(date));
    }

    private static void assertResolvesWithin(PureDate outer, PureDate inner)
    {
        Assert.assertFalse(inner + " starts before " + outer, START.toInstant(inner).isBefore(START.toInstant(outer)));
        Assert.assertFalse(inner + " ends after " + outer, END.toInstant(inner).isAfter(END.toInstant(outer)));
        Assert.assertFalse(inner + " ends after " + outer, EXCLUSIVE_END.toInstant(inner).isAfter(EXCLUSIVE_END.toInstant(outer)));
        Assert.assertFalse(inner + " starts before " + outer, START.toLocalDate(inner).isBefore(START.toLocalDate(outer)));
        Assert.assertFalse(inner + " ends after " + outer, END.toLocalDate(inner).isAfter(END.toLocalDate(outer)));
    }

    private static void assertConversionsAgree(PureDateToJava resolution, PureDate date)
    {
        LocalDateTime resolved = LocalDateTime.ofInstant(resolution.toInstant(date), ZoneOffset.UTC);
        Assert.assertEquals(date + " local date", resolved.toLocalDate(), resolution.toLocalDate(date));
        Assert.assertEquals(date + " year month", YearMonth.from(resolved), resolution.toYearMonth(date));
        Assert.assertEquals(date + " year", Year.from(resolved), resolution.toYear(date));
    }

    private static Instant utc(int year, int month, int day, int hour, int minute, int second, int nanos)
    {
        return LocalDateTime.of(year, month, day, hour, minute, second, nanos).toInstant(ZoneOffset.UTC);
    }
}
