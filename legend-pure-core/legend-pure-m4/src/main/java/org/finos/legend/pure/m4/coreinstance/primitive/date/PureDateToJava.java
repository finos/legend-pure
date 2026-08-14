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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Converts Pure dates to {@code java.time} temporals: {@link Year}, {@link YearMonth},
 * {@link LocalDate}, {@link LocalDateTime}, {@link Instant}, and {@link OffsetDateTime}.
 *
 * <p><b>A Pure date is a span of time, not an instant.</b> It stops at whatever granularity it was
 * written with and stands for everything it leaves unsaid, so {@code 2014} is the whole of that
 * year, {@code 2014-06} the whole of that month, and {@code 2014-06-15T12} the whole of that hour.
 * Converting such a date to a temporal finer grained than the date itself means picking a point in
 * that span, and the conversion has to say which point. There are three worth having:
 *
 * <ul>
 *     <li>{@link #start()} gives the first value of the requested type that the span includes;</li>
 *     <li>{@link #end()} gives the last value it includes;</li>
 *     <li>{@link #exclusiveEnd()} gives the first value after it, for half-open ranges.</li>
 * </ul>
 *
 * <pre>{@code
 * PureDateToJava.start().toInstant(parsePureDate("2014"))        // 2014-01-01T00:00:00Z
 * PureDateToJava.end().toInstant(parsePureDate("2014"))          // 2014-12-31T23:59:59.999999999Z
 * PureDateToJava.exclusiveEnd().toInstant(parsePureDate("2014")) // 2015-01-01T00:00:00Z
 * }</pre>
 *
 * <p>Components the date does carry are used as they stand, so {@link #start()} and {@link #end()}
 * differ only where the date runs out. They always agree on {@link #toYear(PureDate)}; they agree
 * on {@link #toYearMonth(PureDate)} for any date that has a month,
 * on {@link #toLocalDate(PureDate)} for any date that has a day, and on the three conversions below
 * that only for a date carrying all nine subsecond digits.
 *
 * <p>Pure dates carry no time zone and are always understood as UTC, so the conversions that need
 * an offset use UTC.
 *
 * <p><b>{@link #end()} gives the last value the span includes, not the first value after it.</b>
 * The end of {@code 2014} is 2014-12-31T23:59:59.999999999Z, one nanosecond before the start of
 * 2015, since an {@link Instant} goes no finer than the nanosecond. Where a half-open range is
 * wanted, {@link #exclusiveEnd()} gives that following value directly, at whatever granularity is
 * asked for: the first day after {@code 2014-03-10T16} is 2014-03-11, and the first year after it
 * is 2015. Its conversions are therefore not readings of one another, the way the other two
 * resolutions' are - the first instant after that same date is 17:00 on the 10th, which is not on
 * the 11th - and the value after the end of the supported range cannot be expressed at all, so at
 * the last supported year it throws {@link java.time.DateTimeException}.
 *
 * <p>The nanosecond floor is also why subsecond digits beyond the ninth are dropped rather than
 * rounded, as they are in {@link DateFunctions#fromInstant(Instant, int)}: a date of finer than
 * nanosecond precision lies wholly within a single nanosecond, and that nanosecond is what both
 * {@link #start()} and {@link #end()} give for it. Rounding would let a date resolve to an instant
 * outside the span it denotes, and could carry the end of a date past the end of a coarser date
 * containing it.
 *
 * <p>{@link LatestDate} is not supported: it has no components to read and no fixed position on the
 * time line, so every conversion here throws {@link UnsupportedOperationException} for it.
 */
public abstract class PureDateToJava
{
    private PureDateToJava()
    {
    }

    /**
     * Convert a Pure date to the year it falls in. {@link #start()} and {@link #end()} give the
     * same result, as a year has nothing finer for them to resolve, and {@link #exclusiveEnd()}
     * gives the year after.
     *
     * @param pureDate Pure date
     * @return year
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public Year toYear(PureDate pureDate)
    {
        return Year.of(pureDate.getYear());
    }

    /**
     * Convert a Pure date to the month it falls in, resolving the month if the date has none:
     * January for {@link #start()}, December for {@link #end()}.
     *
     * @param pureDate Pure date
     * @return year and month
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public YearMonth toYearMonth(PureDate pureDate)
    {
        int year = pureDate.getYear();
        int month = pureDate.hasMonth() ? pureDate.getMonth() : resolveMonth();
        return YearMonth.of(year, month);
    }

    /**
     * Convert a Pure date to the day it falls in, resolving the month and the day if the date has
     * none: the first day of January for {@link #start()}, the last day of December for
     * {@link #end()}.
     *
     * @param pureDate Pure date
     * @return local date
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public LocalDate toLocalDate(PureDate pureDate)
    {
        int year = pureDate.getYear();
        int month = pureDate.hasMonth() ? pureDate.getMonth() : resolveMonth();
        int day = pureDate.hasDay() ? pureDate.getDay() : resolveDay(year, month);
        return LocalDate.of(year, month, day);
    }

    /**
     * Convert a Pure date to a date and time, resolving every component the date does not carry
     * down to the nanosecond: the earliest for {@link #start()}, the latest for {@link #end()}.
     * A Pure date carries no time zone and is always understood as UTC, so the components are
     * neither shifted nor labelled.
     *
     * @param pureDate Pure date
     * @return local date and time
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public LocalDateTime toLocalDateTime(PureDate pureDate)
    {
        int year = pureDate.getYear();
        int month = pureDate.hasMonth() ? pureDate.getMonth() : resolveMonth();
        int day = pureDate.hasDay() ? pureDate.getDay() : resolveDay(year, month);
        int hour = pureDate.hasHour() ? pureDate.getHour() : resolveHour();
        int minute = pureDate.hasMinute() ? pureDate.getMinute() : resolveMinute();
        int second = pureDate.hasSecond() ? pureDate.getSecond() : resolveSecond();
        int nanos = getNanos(pureDate.getSubsecond());
        return LocalDateTime.of(year, month, day, hour, minute, second, nanos);
    }

    /**
     * Convert a Pure date to an instant in UTC, resolving every component the date does not carry
     * down to the nanosecond: the earliest for {@link #start()}, the latest for {@link #end()}.
     *
     * @param pureDate Pure date
     * @return instant UTC
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public Instant toInstant(PureDate pureDate)
    {
        return toLocalDateTime(pureDate).toInstant(ZoneOffset.UTC);
    }

    /**
     * Convert a Pure date to a date and time at the UTC offset, resolving every component the date
     * does not carry down to the nanosecond: the earliest for {@link #start()}, the latest for
     * {@link #end()}.
     *
     * @param pureDate Pure date
     * @return date and time at the UTC offset
     * @throws UnsupportedOperationException if the date is {@link LatestDate}
     */
    public OffsetDateTime toOffsetDateTime(PureDate pureDate)
    {
        return toLocalDateTime(pureDate).atOffset(ZoneOffset.UTC);
    }

    private int getNanos(String subsecond)
    {
        int length = (subsecond == null) ? 0 : subsecond.length();
        if (length == 0)
        {
            return resolveNanos(9);
        }
        if (length >= 9)
        {
            // Digits beyond the ninth are dropped rather than rounded: the whole of the span such a
            // date covers lies within this one nanosecond, which is therefore both its start and its
            // end. Rounding up would take the result outside that span.
            return Integer.parseInt(subsecond.substring(0, 9));
        }
        int digitsToResolve = 9 - length;
        int nanos = Integer.parseInt(subsecond);
        for (int i = 0; i < digitsToResolve; i++)
        {
            nanos *= 10;
        }
        return nanos + resolveNanos(digitsToResolve);
    }

    abstract int resolveMonth();

    abstract int resolveDay(int year, int month);

    abstract int resolveHour();

    abstract int resolveMinute();

    abstract int resolveSecond();

    abstract int resolveNanos(int digitsToResolve);

    /**
     * Get the converter that resolves a Pure date to the start of the span it covers.
     *
     * @return converter to the start of a date
     */
    public static PureDateToJava start()
    {
        return Converters.START;
    }

    /**
     * Get the converter that resolves a Pure date to the end of the span it covers, which is the
     * last value the span includes rather than the first value after it. See
     * {@link #exclusiveEnd()} for the latter.
     *
     * @return converter to the end of a date
     */
    public static PureDateToJava end()
    {
        return Converters.END;
    }

    /**
     * Get the converter that resolves a Pure date to the first value after the span it covers, one
     * unit of the requested type past {@link #end()}. Together with {@link #start()} this gives
     * the half-open range the date covers, at any of the granularities converted to.
     *
     * <p>Each conversion names the first value of its own type after the span, so unlike the other
     * two resolutions they are not readings of one another: the first day after
     * {@code 2014-03-10T16} is 2014-03-11, while the first instant after it is 17:00 on the 10th.
     *
     * @return converter to the first value after a date
     */
    public static PureDateToJava exclusiveEnd()
    {
        return Converters.EXCLUSIVE_END;
    }

    /**
     * The converters are stateless, so one of each is enough. They are held here rather than in
     * {@link PureDateToJava} itself so that initializing that class does not have to initialize its
     * own subclasses.
     */
    private static final class Converters
    {
        static final PureDateToJava START = new PureDateStartToJava();
        static final PureDateToJava END = new PureDateEndToJava();
        static final PureDateToJava EXCLUSIVE_END = new PureDateExclusiveEndToJava();
    }

    private static class PureDateStartToJava extends PureDateToJava
    {
        @Override
        int resolveMonth()
        {
            return 1;
        }

        @Override
        int resolveDay(int year, int month)
        {
            return 1;
        }

        @Override
        int resolveHour()
        {
            return 0;
        }

        @Override
        int resolveMinute()
        {
            return 0;
        }

        @Override
        int resolveSecond()
        {
            return 0;
        }

        @Override
        int resolveNanos(int digitsToResolve)
        {
            return 0;
        }
    }

    private static class PureDateEndToJava extends PureDateToJava
    {
        private static final int[] RESOLVED_NANOS = {0, 9, 99, 999, 9_999, 99_999, 999_999, 9_999_999, 99_999_999, 999_999_999};

        @Override
        int resolveMonth()
        {
            return 12;
        }

        @Override
        int resolveDay(int year, int month)
        {
            return DateFunctions.getDaysInMonth(year, month);
        }

        @Override
        int resolveHour()
        {
            return 23;
        }

        @Override
        int resolveMinute()
        {
            return 59;
        }

        @Override
        int resolveSecond()
        {
            return 59;
        }

        @Override
        int resolveNanos(int digitsToResolve)
        {
            return RESOLVED_NANOS[digitsToResolve];
        }
    }

    /**
     * The end of the span, plus one unit of whatever type is being converted to. The instant and
     * offset conversions follow from {@link #toLocalDateTime(PureDate)} and so need no override of
     * their own.
     */
    private static class PureDateExclusiveEndToJava extends PureDateEndToJava
    {
        @Override
        public Year toYear(PureDate pureDate)
        {
            return super.toYear(pureDate).plusYears(1);
        }

        @Override
        public YearMonth toYearMonth(PureDate pureDate)
        {
            return super.toYearMonth(pureDate).plusMonths(1);
        }

        @Override
        public LocalDate toLocalDate(PureDate pureDate)
        {
            return super.toLocalDate(pureDate).plusDays(1);
        }

        @Override
        public LocalDateTime toLocalDateTime(PureDate pureDate)
        {
            return super.toLocalDateTime(pureDate).plusNanos(1);
        }
    }
}
