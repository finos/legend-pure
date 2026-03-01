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
import java.time.chrono.IsoChronology;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateFunctions extends TimeFunctions
{
    private static final int MAX_YEAR = java.time.Year.MAX_VALUE;
    private static final int MIN_YEAR = java.time.Year.MIN_VALUE;

    static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    public static PureDate newPureDate(int year)
    {
        return Year.newYear(year);
    }

    public static PureDate newPureDate(int year, int month)
    {
        return YearMonth.newYearMonth(year, month);
    }

    public static StrictDate newPureDate(int year, int month, int day)
    {
        return StrictDate.newStrictDate(year, month, day);
    }

    public static DateTime newPureDate(int year, int month, int day, int hour)
    {
        return DateWithHour.newDateWithHour(year, month, day, hour);
    }

    public static DateTime newPureDate(int year, int month, int day, int hour, int minute)
    {
        return DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
    }

    public static DateTime newPureDate(int year, int month, int day, int hour, int minute, int second)
    {
        return DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
    }

    public static DateTime newPureDate(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        return DateWithSubsecond.newDateWithSubsecond(year, month, day, hour, minute, second, subsecond);
    }

    public static PureDate fromCalendar(GregorianCalendar calendar)
    {
        return fromCalendar(calendar, Calendar.MILLISECOND);
    }

    public static PureDate fromCalendar(GregorianCalendar calendar, int precision)
    {
        TimeZone timeZone = calendar.getTimeZone();
        if (!GMT_TIME_ZONE.equals(timeZone))
        {
            // Possibly adjust to UTC
            long time = calendar.getTimeInMillis();
            int offset = timeZone.getOffset(time);
            if (offset != 0)
            {
                // Adjust to UTC
                calendar = new GregorianCalendar(GMT_TIME_ZONE);
                calendar.setTimeInMillis(time - offset);
            }
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
                return DateWithSubsecond.newDateWithSubsecond(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), String.format("%03d", calendar.get(Calendar.MILLISECOND)));
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported calendar precision: " + precision);
            }
        }
    }

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
        GregorianCalendar calendar = new GregorianCalendar(GMT_TIME_ZONE);
        calendar.setTime(date);
        return fromCalendar(calendar, Calendar.MILLISECOND);
    }

    public static StrictDate fromSQLDate(java.sql.Date date)
    {
        return StrictDate.fromSQLDate(date);
    }

    public static DateTime fromSQLTimestamp(java.sql.Timestamp timestamp)
    {
        return DateWithSubsecond.fromSQLTimestamp(timestamp);
    }

    public static DateTime fromInstant(Instant instant)
    {
        return fromInstant(instant, 9);
    }

    public static DateTime fromInstant(Instant instant, int subsecondPrecision)
    {
        return DateWithSubsecond.fromInstant(instant, subsecondPrecision);
    }

    /**
     * Returns a StrictDate of today in UTC.
     *
     * @return today UTC
     */
    public static StrictDate today()
    {
        return StrictDate.fromLocalDate(LocalDate.now(Clock.systemUTC()));
    }

    static int getYearDays(int year)
    {
        return isLeapYear(year) ? 366 : 365;
    }

    public static long dateDifference(PureDate thisDate, PureDate otherDate, String unit)
    {
        if (thisDate.equals(otherDate))
        {
            return 0;
        }
        long result;
        switch (unit)
        {
            case "YEARS":
            {
                result = DateDiff.getDiffYears(thisDate, otherDate);
                break;
            }
            case "MONTHS":
            {
                result = DateDiff.getDiffMonths(thisDate, otherDate);
                break;
            }
            case "WEEKS":
            {
                result = DateDiff.getDateDiffWeeks(thisDate, otherDate);
                break;
            }
            case "DAYS":
            {
                result = DateDiff.getDiffDays(thisDate, otherDate);
                break;
            }
            case "HOURS":
            {
                result = DateDiff.getDiffHours(thisDate, otherDate);
                break;
            }
            case "MINUTES":
            {
                result = DateDiff.getDiffMinutes(thisDate, otherDate);
                break;
            }
            case "SECONDS":
            {
                result = DateDiff.getDiffSeconds(thisDate, otherDate);
                break;
            }
            case "MILLISECONDS":
            {
                result = DateDiff.getDiffInMilliseconds(thisDate, otherDate);
                break;
            }
            case "MICROSECONDS":
            {
                result = DateDiff.getDiffInMicroseconds(thisDate, otherDate);
                break;
            }
            case "NANOSECONDS":
            {
                result = DateDiff.getDiffInNanoseconds(thisDate, otherDate);
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }
        int sign = otherDate.compareTo(thisDate);
        return sign * result;
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

    static void validateYear(int year)
    {
        if ((year < MIN_YEAR) || (year > MAX_YEAR))
        {
            throw new IllegalArgumentException(String.format("Invalid year (valid [%,d, %,d]): %,d", MIN_YEAR, MAX_YEAR, year));
        }
    }

    static void validateMonth(int month)
    {
        if ((month < 1) || (month > 12))
        {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

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

    public static int compare(PureDate date1, PureDate date2)
    {
        if (date1 == date2)
        {
            return 0;
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
