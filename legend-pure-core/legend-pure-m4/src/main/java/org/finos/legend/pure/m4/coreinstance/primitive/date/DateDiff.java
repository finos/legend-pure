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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementation of {@link DateFunctions#dateDifference(PureDate, PureDate, String)}. See that
 * method for what each unit means.
 */
class DateDiff
{
    /**
     * Epoch day of 1970-01-04, a Sunday, used as the origin for counting weeks.
     */
    private static final long SUNDAY_EPOCH_DAY = 3L;

    private DateDiff()
    {
    }

    static long dateDifference(PureDate from, PureDate to, String unit)
    {
        switch (unit)
        {
            case "YEARS":
            {
                return (long) to.getYear() - (long) from.getYear();
            }
            case "MONTHS":
            {
                return monthNumber(to) - monthNumber(from);
            }
            case "WEEKS":
            {
                return weeksBetween(toLocalDate(from), toLocalDate(to));
            }
            case "DAYS":
            {
                return toLocalDate(to).toEpochDay() - toLocalDate(from).toEpochDay();
            }
            case "HOURS":
            {
                return elapsed(from, to, ChronoUnit.HOURS);
            }
            case "MINUTES":
            {
                return elapsed(from, to, ChronoUnit.MINUTES);
            }
            case "SECONDS":
            {
                return elapsed(from, to, ChronoUnit.SECONDS);
            }
            case "MILLISECONDS":
            {
                return elapsed(from, to, ChronoUnit.MILLIS);
            }
            case "MICROSECONDS":
            {
                return elapsed(from, to, ChronoUnit.MICROS);
            }
            case "NANOSECONDS":
            {
                return elapsed(from, to, ChronoUnit.NANOS);
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }
    }

    /**
     * The month of the given date counted from year zero, so that subtracting two of these gives the
     * number of month boundaries between them. A date with no month starts in January.
     */
    private static long monthNumber(PureDate date)
    {
        return (12L * date.getYear()) + (date.hasMonth() ? date.getMonth() : 1);
    }

    /**
     * The number of Sundays passed going from one date to the other. Counting forwards, a Sunday
     * counts if it falls after the first date and on or before the second; counting backwards, if it
     * falls on or after the second date and before the first. The two directions are therefore not
     * always mirror images, which is longstanding behavior pinned by the PCT tests for
     * {@code dateDiff}.
     */
    private static long weeksBetween(LocalDate from, LocalDate to)
    {
        long fromDay = from.toEpochDay();
        long toDay = to.toEpochDay();
        return (toDay >= fromDay) ?
               (weekNumber(toDay) - weekNumber(fromDay)) :
               (weekNumber(toDay - 1) - weekNumber(fromDay - 1));
    }

    /**
     * The Sunday-to-Saturday week the given day falls in, counted from the week of
     * {@link #SUNDAY_EPOCH_DAY}.
     */
    private static long weekNumber(long epochDay)
    {
        return Math.floorDiv(epochDay - SUNDAY_EPOCH_DAY, 7L);
    }

    /**
     * The time elapsed between the two dates in whole units, with any remainder dropped. Since
     * {@link ChronoUnit#between} counts only complete units, the remainder is always dropped toward
     * zero, which keeps the result the same size in either direction.
     */
    private static long elapsed(PureDate from, PureDate to, ChronoUnit unit)
    {
        return unit.between(toLocalDateTime(from), toLocalDateTime(to));
    }

    /**
     * The first instant of the span the given date covers: a date with no month starts in January,
     * one with no day starts on the first, and one with no time starts at midnight. Subsecond digits
     * beyond the nanosecond are dropped, since that is as fine as {@link LocalDateTime} goes.
     */
    private static LocalDateTime toLocalDateTime(PureDate date)
    {
        return LocalDateTime.of(toLocalDate(date), toLocalTime(date));
    }

    private static LocalDate toLocalDate(PureDate date)
    {
        return LocalDate.of(date.getYear(), date.hasMonth() ? date.getMonth() : 1, date.hasDay() ? date.getDay() : 1);
    }

    private static LocalTime toLocalTime(PureDate date)
    {
        return LocalTime.of(
                date.hasHour() ? date.getHour() : 0,
                date.hasMinute() ? date.getMinute() : 0,
                date.hasSecond() ? date.getSecond() : 0,
                date.hasSubsecond() ? nanosecond(date.getSubsecond()) : 0);
    }

    /**
     * Read a subsecond as a whole number of nanoseconds, padding it with zeros if it is shorter than
     * nine digits and ignoring anything past the ninth.
     */
    private static int nanosecond(String subsecond)
    {
        int digits = subsecond.length();
        int nanoseconds = 0;
        for (int i = 0; i < 9; i++)
        {
            nanoseconds = (nanoseconds * 10) + ((i < digits) ? (subsecond.charAt(i) - '0') : 0);
        }
        return nanoseconds;
    }
}
