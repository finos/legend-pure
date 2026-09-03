// Copyright 2020 Goldman Sachs
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Functions for creating, converting, and comparing Pure dates.
 *
 * <p>A Pure date carries a granularity: it may stop at the year, the month, the day, the hour, the
 * minute, the second, or a subsecond with any number of digits, and it stands for everything it
 * leaves unsaid. It carries no time zone; every Pure date is understood as UTC, so the conversions
 * here shift an incoming value to UTC rather than discarding its zone or offset.
 *
 * <p>See {@link #compare(PureDate, PureDate)} for how dates of differing granularity are ordered.
 */
public class DateFunctions extends TimeFunctions
{
    private static final int MAX_YEAR = java.time.Year.MAX_VALUE;
    private static final int MIN_YEAR = java.time.Year.MIN_VALUE;

    static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * Create a Pure date of year granularity, standing for the whole of the given year.
     *
     * @param year year
     * @return Pure date
     */
    public static PureDate newPureDate(int year)
    {
        return Year.newYear(year);
    }

    /**
     * Create a Pure date of month granularity, standing for the whole of the given month.
     *
     * @param year  year
     * @param month month (1-12)
     * @return Pure date
     */
    public static PureDate newPureDate(int year, int month)
    {
        return YearMonth.newYearMonth(year, month);
    }

    /**
     * Create a Pure date of day granularity, standing for the whole of the given day.
     *
     * @param year  year
     * @param month month (1-12)
     * @param day   day of the month
     * @return Pure date
     */
    public static StrictDate newPureDate(int year, int month, int day)
    {
        return StrictDate.newStrictDate(year, month, day);
    }

    /**
     * Create a Pure date of hour granularity, standing for the whole of the given hour UTC.
     *
     * @param year  year
     * @param month month (1-12)
     * @param day   day of the month
     * @param hour  hour (0-23)
     * @return Pure date
     */
    public static DateTime newPureDate(int year, int month, int day, int hour)
    {
        return DateWithHour.newDateWithHour(year, month, day, hour);
    }

    /**
     * Create a Pure date of minute granularity, standing for the whole of the given minute UTC.
     *
     * @param year   year
     * @param month  month (1-12)
     * @param day    day of the month
     * @param hour   hour (0-23)
     * @param minute minute (0-59)
     * @return Pure date
     */
    public static DateTime newPureDate(int year, int month, int day, int hour, int minute)
    {
        return DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
    }

    /**
     * Create a Pure date of second granularity, standing for the whole of the given second UTC.
     *
     * @param year   year
     * @param month  month (1-12)
     * @param day    day of the month
     * @param hour   hour (0-23)
     * @param minute minute (0-59)
     * @param second second (0-59)
     * @return Pure date
     */
    public static DateTime newPureDate(int year, int month, int day, int hour, int minute, int second)
    {
        return DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
    }

    /**
     * Create a Pure date of subsecond granularity UTC. The subsecond is the digits after the decimal
     * point, so its length is the precision: "07" is a hundredth of a second and "070" a thousandth.
     * It must be a non-empty string of digits.
     *
     * @param year      year
     * @param month     month (1-12)
     * @param day       day of the month
     * @param hour      hour (0-23)
     * @param minute    minute (0-59)
     * @param second    second (0-59)
     * @param subsecond digits after the decimal point
     * @return Pure date
     */
    public static DateTime newPureDate(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        return DateWithSubsecond.newDateWithSubsecond(year, month, day, hour, minute, second, subsecond);
    }

    /**
     * Convert a calendar to a Pure date of millisecond precision, reading the instant it holds as
     * UTC.
     *
     * @param calendar calendar
     * @return Pure date
     */
    public static PureDate fromCalendar(GregorianCalendar calendar)
    {
        return fromCalendar(calendar, Calendar.MILLISECOND);
    }

    /**
     * Convert a calendar to a Pure date of the given precision, reading the instant it holds as UTC.
     * The calendar's time zone decides only whether its fields can be read as they stand or have to
     * be recomputed in GMT first; it never shifts the instant.
     *
     * @param calendar  calendar
     * @param precision one of the {@link Calendar} field constants from {@link Calendar#YEAR} down
     *                  to {@link Calendar#MILLISECOND}, naming the granularity of the result
     * @return Pure date
     */
    public static PureDate fromCalendar(GregorianCalendar calendar, int precision)
    {
        if ((calendar.get(Calendar.ZONE_OFFSET) != 0) || (calendar.get(Calendar.DST_OFFSET) != 0))
        {
            // Possibly adjust to UTC
            GregorianCalendar newCalendar = new GregorianCalendar(GMT_TIME_ZONE);
            newCalendar.setTimeInMillis(calendar.getTimeInMillis());
            calendar = newCalendar;
        }

        switch (precision)
        {
            case Calendar.YEAR:
            {
                return Year.newYear(calendar.get(Calendar.YEAR));
            }
            case Calendar.MONTH:
            {
                return YearMonth.newYearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
            }
            case Calendar.DAY_OF_MONTH:
            case Calendar.DAY_OF_YEAR:
            case Calendar.DAY_OF_WEEK:
            case Calendar.DAY_OF_WEEK_IN_MONTH:
            {
                return StrictDate.newStrictDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
            }
            case Calendar.HOUR_OF_DAY:
            case Calendar.HOUR:
            {
                return DateWithHour.newDateWithHour(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY));
            }
            case Calendar.MINUTE:
            {
                return DateWithMinute.newDateWithMinute(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            }
            case Calendar.SECOND:
            {
                return DateWithSecond.newDateWithSecond(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
            }
            case Calendar.MILLISECOND:
            {
                return DateWithSubsecond.newDateWithSubsecond(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), TimeFunctions.subsecondFromMilliseconds(calendar.get(Calendar.MILLISECOND)));
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported calendar precision: " + precision);
            }
        }
    }

    /**
     * Get the name of the Pure primitive type the given date belongs to, which follows from its
     * granularity: Date for year and month granularity, StrictDate for day granularity, DateTime for
     * anything finer, and LatestDate for {@link LatestDate}.
     *
     * @param pureDate Pure date
     * @return Pure primitive type name
     */
    public static String datePrimitiveType(PureDate pureDate)
    {
        if (LatestDate.isLatestDate(pureDate))
        {
            return ModelRepository.LATEST_DATE_TYPE_NAME;
        }
        if (pureDate.hasHour())
        {
            return ModelRepository.DATETIME_TYPE_NAME;
        }
        if (pureDate.hasDay())
        {
            return ModelRepository.STRICT_DATE_TYPE_NAME;
        }
        return ModelRepository.DATE_TYPE_NAME;
    }

    /**
     * Convert a Java date to a Pure date. A {@link java.sql.Date} names a day and no zone, and
     * yields that day. Any other {@link Date} names a moment, and yields that moment read in UTC:
     * to nanosecond granularity for a {@link java.sql.Timestamp}, and to millisecond granularity
     * otherwise.
     *
     * @param date Java date
     * @return Pure date
     */
    public static PureDate fromDate(Date date)
    {
        if (date instanceof java.sql.Date)
        {
            return fromSQLDate((java.sql.Date)date);
        }
        if (date instanceof java.sql.Timestamp)
        {
            return fromSQLTimestamp((java.sql.Timestamp)date);
        }
        return fromInstant(date.toInstant(), 3);
    }

    /**
     * Convert a SQL date to the Pure date standing for the same day. A SQL date names a day and no
     * zone, as a Pure date of day granularity does, so this is a copy of the year, month, and day.
     *
     * @param date SQL date
     * @return Pure date standing for the same day
     */
    public static StrictDate fromSQLDate(java.sql.Date date)
    {
        return StrictDate.fromSQLDate(date);
    }

    /**
     * Convert a SQL timestamp to a Pure date, reading the instant it holds as UTC and keeping all
     * nine subsecond digits.
     *
     * @param timestamp SQL timestamp
     * @return Pure date
     */
    public static DateTime fromSQLTimestamp(java.sql.Timestamp timestamp)
    {
        return DateWithSubsecond.fromSQLTimestamp(timestamp);
    }

    /**
     * Convert an {@link Instant} to a Pure date in UTC, keeping all nine subsecond digits.
     *
     * @param instant instant
     * @return Pure date
     */
    public static DateTime fromInstant(Instant instant)
    {
        return fromInstant(instant, 9);
    }

    /**
     * Convert an {@link Instant} to a Pure date in UTC, keeping the given number of subsecond
     * digits. Digits beyond that number are dropped, not rounded. A precision of 0 gives a date of
     * second granularity.
     *
     * @param instant            instant
     * @param subsecondPrecision number of subsecond digits to keep (0-9)
     * @return Pure date
     */
    public static DateTime fromInstant(Instant instant, int subsecondPrecision)
    {
        return fromLocalDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), subsecondPrecision);
    }

    /**
     * Convert a {@link java.time.Year} to a Pure date of year granularity.
     *
     * @param year year
     * @return Pure date
     */
    public static PureDate fromYear(java.time.Year year)
    {
        return Year.fromYear(year);
    }

    /**
     * Convert a {@link java.time.YearMonth} to a Pure date of month granularity.
     *
     * @param yearMonth year and month
     * @return Pure date
     */
    public static PureDate fromYearMonth(java.time.YearMonth yearMonth)
    {
        return YearMonth.fromYearMonth(yearMonth);
    }

    /**
     * Convert a {@link LocalDate} to a Pure date of day granularity.
     *
     * @param date local date
     * @return Pure date
     */
    public static StrictDate fromLocalDate(LocalDate date)
    {
        return StrictDate.fromLocalDate(date);
    }

    /**
     * Convert a {@link LocalDateTime} to a Pure date, keeping all nine subsecond digits. Pure dates
     * carry no time zone and are always understood as UTC, and a {@link LocalDateTime} carries no
     * zone either, so its fields are taken as they stand.
     *
     * @param dateTime local date and time
     * @return Pure date
     */
    public static DateTime fromLocalDateTime(LocalDateTime dateTime)
    {
        return fromLocalDateTime(dateTime, 9);
    }

    /**
     * Convert a {@link LocalDateTime} to a Pure date, keeping the given number of subsecond digits.
     * Digits beyond that number are dropped, not rounded. A precision of 0 gives a date of second
     * granularity. Pure dates carry no time zone and are always understood as UTC, and a
     * {@link LocalDateTime} carries no zone either, so its fields are taken as they stand.
     *
     * @param dateTime           local date and time
     * @param subsecondPrecision number of subsecond digits to keep (0-9)
     * @return Pure date
     */
    public static DateTime fromLocalDateTime(LocalDateTime dateTime, int subsecondPrecision)
    {
        return (subsecondPrecision == 0) ? DateWithSecond.fromLocalDateTime(dateTime) : DateWithSubsecond.fromLocalDateTime(dateTime, subsecondPrecision);
    }

    /**
     * Convert an {@link OffsetDateTime} to a Pure date, keeping all nine subsecond digits. Since
     * Pure dates are always understood as UTC, the instant is shifted to UTC first: the offset is
     * applied, not discarded.
     *
     * @param dateTime date and time with a UTC offset
     * @return Pure date
     */
    public static DateTime fromOffsetDateTime(OffsetDateTime dateTime)
    {
        return fromOffsetDateTime(dateTime, 9);
    }

    /**
     * Convert an {@link OffsetDateTime} to a Pure date, keeping the given number of subsecond
     * digits. Digits beyond that number are dropped, not rounded. A precision of 0 gives a date of
     * second granularity. Since Pure dates are always understood as UTC, the instant is shifted to
     * UTC first: the offset is applied, not discarded.
     *
     * @param dateTime           date and time with a UTC offset
     * @param subsecondPrecision number of subsecond digits to keep (0-9)
     * @return Pure date
     */
    public static DateTime fromOffsetDateTime(OffsetDateTime dateTime, int subsecondPrecision)
    {
        return fromInstant(dateTime.toInstant(), subsecondPrecision);
    }

    /**
     * Convert a {@link ZonedDateTime} to a Pure date, keeping all nine subsecond digits. Since Pure
     * dates are always understood as UTC, the instant is shifted to UTC first, using the offset the
     * zone was in at that instant.
     *
     * @param dateTime date and time in a time zone
     * @return Pure date
     */
    public static DateTime fromZonedDateTime(ZonedDateTime dateTime)
    {
        return fromZonedDateTime(dateTime, 9);
    }

    /**
     * Convert a {@link ZonedDateTime} to a Pure date, keeping the given number of subsecond digits.
     * Digits beyond that number are dropped, not rounded. A precision of 0 gives a date of second
     * granularity. Since Pure dates are always understood as UTC, the instant is shifted to UTC
     * first, using the offset the zone was in at that instant.
     *
     * @param dateTime           date and time in a time zone
     * @param subsecondPrecision number of subsecond digits to keep (0-9)
     * @return Pure date
     */
    public static DateTime fromZonedDateTime(ZonedDateTime dateTime, int subsecondPrecision)
    {
        return fromInstant(dateTime.toInstant(), subsecondPrecision);
    }

    /**
     * Returns a StrictDate of today in UTC.
     *
     * @return today UTC
     */
    public static StrictDate today()
    {
        return fromLocalDate(LocalDate.now(Clock.systemUTC()));
    }

    /**
     * Get the number of days in the given year according to the Gregorian calendar.
     *
     * @param year Gregorian calendar year
     * @return number of days in the year
     */
    static int getYearDays(int year)
    {
        return isLeapYear(year) ? 366 : 365;
    }

    /**
     * Get the difference between two Pure dates in the given unit. The result is positive when
     * otherDate is the later of the two, negative when it is the earlier, and zero when neither is.
     *
     * <p>Each date is read as the first instant of the span it covers, so a date with no month
     * starts in January, one with no day starts on the first, and one with no time starts at
     * midnight. What is measured from there depends on which family the unit belongs to: YEARS
     * through DAYS name positions in the civil calendar, while HOURS and below name a fixed quantity
     * of elapsed time. (This is the same division {@link java.time.temporal.ChronoUnit} makes between
     * date-based and time-based units, and {@link java.time.Period} between calendar periods and
     * {@link java.time.Duration}, but the division is a property of the units rather than of any one
     * library.)
     *
     * <p><b>The calendar units - YEARS, MONTHS, WEEKS, and DAYS - count boundaries crossed</b>,
     * ignoring everything finer than the unit. YEARS counts new years begun, MONTHS new months, DAYS
     * midnights passed, WEEKS Sundays reached. So 2015-12-31T23:59:59 to 2016-01-01T00:00:01 is one
     * YEAR, though only two seconds passed, and 2016-02-01 to 2016-02-29 is zero MONTHS, though
     * nearly a month did.
     *
     * <p>The counted range is half open: a boundary landing on otherDate counts, one landing on
     * thisDate does not. For YEARS, MONTHS, and DAYS that makes no difference, because dropping the
     * finer components already puts both dates on the unit's own boundaries, so the count is the same
     * in either direction. <b>WEEKS is the exception</b>, since days are not aligned to weeks: a
     * Saturday to the following Sunday is 1, while that Sunday back to the Saturday is 0 rather than
     * -1. That asymmetry is the one place the half-open convention becomes visible.
     *
     * <p><b>The time units - HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, and NANOSECONDS -
     * measure elapsed time</b>, dropping any remainder. <b>This is not the same as counting
     * boundaries.</b> 12:59:00 to 13:01:00 crosses the 13:00 boundary yet is zero HOURS, and 12:00:00
     * to 12:59:00 crosses none and is also zero. The two readings agree whenever thisDate falls on a
     * boundary of the unit, which is the usual case, but they are different rules: the same two
     * minutes from 23:59:00 to 00:01:00 the next day are one DAY and zero HOURS.
     *
     * <p>Subsecond digits count down to the nanosecond and anything finer is ignored. A span too
     * large to express in the requested unit throws {@link ArithmeticException} rather than wrapping,
     * which NANOSECONDS reaches at about 292 years.
     *
     * <p>This is the inverse of {@code meta::pure::functions::date::adjust} when the unit matches the
     * granularity of both dates: for dates of day granularity,
     * {@code adjust(a, dateDifference(a, b, "DAYS"), DAYS)} is b, and likewise for the other units
     * that name a granularity. It is not an inverse otherwise, since a coarser unit drops the finer
     * components and adjusting by a finer unit than the date carries is not supported at all. WEEKS
     * is never an inverse, as adjusting by weeks adds seven days rather than moving to a Sunday.
     *
     * @param thisDate  date to measure from
     * @param otherDate date to measure to
     * @param unit      one of YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
     *                  MICROSECONDS, NANOSECONDS
     * @return difference in the given unit
     */
    public static long dateDifference(PureDate thisDate, PureDate otherDate, String unit)
    {
        return DateDiff.dateDifference(thisDate, otherDate, unit);
    }

    /**
     * Format a Java date to a canonical Pure date string.  The Java date
     * is assumed to represent a date in UTC.
     *
     * @param date Java date
     * @return canonical Pure date string
     */
    public static String formatDate(Date date)
    {
        if (date instanceof java.sql.Date)
        {
            return date.toString();
        }
        return fromDate(date).toString();
    }

    /**
     * Return whether the given Gregorian calendar year is a leap year.
     *
     * @param year Gregorian calendar year
     * @return whether year is a leap year
     */
    public static boolean isLeapYear(int year)
    {
        return IsoChronology.INSTANCE.isLeapYear(year);
    }

    /**
     * Get the number of days in the given month (1-12) in the given
     * year according to the Gregorian calendar.
     *
     * @param year  Gregorian calendar year
     * @param month month (1-12)
     * @return number of days in the month
     */
    public static int getDaysInMonth(int year, int month)
    {
        switch (month)
        {
            case 2:
            {
                return isLeapYear(year) ? 29 : 28;
            }
            case 4:
            case 6:
            case 9:
            case 11:
            {
                return 30;
            }
            default:
            {
                validateMonth(month);
                return 31;
            }
        }
    }

    /**
     * Parse a string into a Pure date.
     *
     * @param string string
     * @return Pure date
     */
    public static PureDate parsePureDate(String string)
    {
        return DateFormat.parsePureDate(string, 0, string.length());
    }

    /**
     * Throw if the given year is outside the supported range.
     *
     * @param year year
     */
    static void validateYear(int year)
    {
        if ((year < MIN_YEAR) || (year > MAX_YEAR))
        {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid year (valid [%,d, %,d]): %,d", MIN_YEAR, MAX_YEAR, year));
        }
    }

    /**
     * Throw if the given month is not in the range 1-12.
     *
     * @param month month
     */
    static void validateMonth(int month)
    {
        if ((month < 1) || (month > 12))
        {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

    /**
     * Throw if the given day does not exist in the given month of the given year.
     *
     * @param year  year
     * @param month month (1-12)
     * @param day   day of the month
     */
    static void validateDay(int year, int month, int day)
    {
        if (day < 1)
        {
            throw new IllegalArgumentException("Invalid day: " + day);
        }
        if (day > getDaysInMonth(year, month))
        {
            throw new IllegalArgumentException("Invalid day: " + year + "-" + month + "-" + day);
        }
    }

    /**
     * Compare two Pure dates, returning -1 if date1 sorts first, 1 if date2 sorts first, and 0 if
     * they are the same date. This is the ordering used by {@link PureDate#compareTo(PureDate)}.
     *
     * <p><b>A Pure date is a span of time, not an instant.</b> A date stops at whatever granularity
     * it was written with, and stands for everything it leaves unsaid: {@code 2014} is the whole of
     * that year, {@code 2014-06} the whole of that month, {@code 2014-06-15T12} the whole of that
     * hour. So every date covers a span running from the first instant it includes up to (but not
     * including) the first instant after it. Below, s(x) is the start of x's span and e(x) is its
     * end.
     *
     * <p>When two dates have the same granularity their spans are the same width, and the ordering
     * is the obvious one: whichever span comes first on the time line sorts first. Dates of
     * different granularities are what make this ordering surprising, because then one span can
     * contain the other.
     *
     * <p><b>The rule.</b>
     *
     * <pre>{@code
     * compare(a, b) <  0   iff   s(a) < s(b),   or   (s(a) == s(b) and e(a) > e(b))
     * compare(a, b) == 0   iff   s(a) == s(b)   and   e(a) == e(b)
     * compare(a, b) >  0   iff   s(a) > s(b),   or   (s(a) == s(b) and e(a) < e(b))
     * }</pre>
     *
     * <p>In words: <b>the date that starts earlier sorts first; if both start at the same instant,
     * the wider one sorts first.</b> Equivalently, a sorts before b if a starts before the start of
     * b, or if a contains b without being equal to it.
     *
     * <p>The second half of the rule is what catches people out. Whenever one date contains
     * another, <b>the container sorts first</b>, even though it ends after the end of the date it
     * contains. That falls out of reading the components left to right - year, then month, then day,
     * and so on - because a date that runs out of components sorts before one that carries on, the
     * way "car" sorts before "cart" in a dictionary. Note that a coarse date does not sit in the
     * middle of the finer dates inside it; it sorts before all of them, so {@code 2014} sorts before
     * {@code 2014-12-31} just as it sorts before {@code 2014-01-01}.
     *
     * <p>Two tempting but wrong readings:
     *
     * <ul>
     *     <li>It is <em>not</em> "a ends before the end of b". {@code compare(2014, 2014-06)} is -1,
     *     yet 2014 ends six months after 2014-06 ends.</li>
     *     <li>It is <em>not</em> "a starts before the start of b" on its own. {@code 2014} and
     *     {@code 2014-01} start at the very same instant, and {@code 2014} still sorts first.</li>
     * </ul>
     *
     * <p><b>Examples.</b>
     *
     * <pre>{@code
     * compare(2013,       2014)                    == -1  // 2013 ends before 2014 starts
     * compare(2014-06,    2014-07)                 == -1  // June ends before July starts
     * compare(2014-06-15, 2014-07)                 == -1  // a day in June is before all of July
     * compare(2014,       2014-06)                 == -1  // the year contains the month
     * compare(2014-06,    2014)                    ==  1
     * compare(2014,       2014-01)                 == -1  // same start, and the year is wider
     * compare(2014,       2014-12)                 == -1  // same end, but the year starts earlier
     * compare(2014-01-01, 2014-01-01T00)           == -1  // same start, and the day is wider
     * compare(2014-01-01T00:00:00.1,
     *         2014-01-01T00:00:00.10)              == -1  // same start: .1 is a tenth of a second,
     *                                                     // .10 a hundredth, so .1 is wider
     * }</pre>
     *
     * <p>This is a total order, it never returns anything but -1, 0, or 1, and it agrees with equals:
     * it returns 0 exactly when the two dates are equal.
     *
     * <p><b>{@link LatestDate} sorts after every other date.</b> It has no components to compare and
     * no fixed position on the time line: what {@code %latest} stands for is decided by the context
     * it appears in. Ordering it last is a sorting convention rather than a claim about when it
     * falls, chosen so that any two dates can be sorted. It is a singleton, so it is equal only to
     * itself.
     *
     * @param date1 first date
     * @param date2 second date
     * @return -1, 0, or 1 as date1 sorts before, equal to, or after date2
     */
    public static int compare(PureDate date1, PureDate date2)
    {
        if (date1 == date2)
        {
            return 0;
        }

        // LatestDate sorts last. It is a singleton, so the check above has already dealt with it
        // against itself, and at most one of the two can be it here.
        if (LatestDate.isLatestDate(date1))
        {
            return 1;
        }
        if (LatestDate.isLatestDate(date2))
        {
            return -1;
        }

        // Compare year
        int cmp = Integer.compare(date1.getYear(), date2.getYear());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare month
        if (!date1.hasMonth())
        {
            return date2.hasMonth() ? -1 : 0;
        }
        if (!date2.hasMonth())
        {
            return 1;
        }
        cmp = Integer.compare(date1.getMonth(), date2.getMonth());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare day
        if (!date1.hasDay())
        {
            return date2.hasDay() ? -1 : 0;
        }
        if (!date2.hasDay())
        {
            return 1;
        }
        cmp = Integer.compare(date1.getDay(), date2.getDay());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare hour
        if (!date1.hasHour())
        {
            return date2.hasHour() ? -1 : 0;
        }
        if (!date2.hasHour())
        {
            return 1;
        }
        cmp = Integer.compare(date1.getHour(), date2.getHour());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare minute
        if (!date1.hasMinute())
        {
            return date2.hasMinute() ? -1 : 0;
        }
        if (!date2.hasMinute())
        {
            return 1;
        }
        cmp = Integer.compare(date1.getMinute(), date2.getMinute());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare second
        if (!date1.hasSecond())
        {
            return date2.hasSecond() ? -1 : 0;
        }
        if (!date2.hasSecond())
        {
            return 1;
        }
        cmp = Integer.compare(date1.getSecond(), date2.getSecond());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare subsecond
        if (!date1.hasSubsecond())
        {
            return date2.hasSubsecond() ? -1 : 0;
        }
        if (!date2.hasSubsecond())
        {
            return 1;
        }
        String subsecond1 = date1.getSubsecond();
        String subsecond2 = date2.getSubsecond();
        int length1 = subsecond1.length();
        int length2 = subsecond2.length();
        for (int i = 0, minLength = Math.min(length1, length2); i < minLength; i++)
        {
            cmp = Integer.compare(subsecond1.charAt(i), subsecond2.charAt(i));
            if (cmp != 0)
            {
                return cmp;
            }
        }
        return Integer.compare(length1, length2);
    }
}
