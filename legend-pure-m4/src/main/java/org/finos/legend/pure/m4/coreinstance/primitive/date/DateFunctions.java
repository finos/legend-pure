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

import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m4.ModelRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateFunctions
{
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

    public static StrictTimeWithMinute newPureStrictTime(int hour, int minute)
    {
        return StrictTimeWithMinute.newStrictTimeWithMinute(hour, minute);
    }
    public static StrictTimeWithSecond newPureStrictTime(int hour, int minute, int second)
    {
        return StrictTimeWithSecond.newStrictTimeWithSecond(hour, minute, second);
    }

    public static StrictTimeWithSubsecond newPureStrictTime(int hour, int minute, int second, String subsecond)
    {
        return StrictTimeWithSubsecond.newStrictTimeWithSubsecond(hour, minute, second, subsecond);
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
        return (year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0));
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
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
            {
                return 31;
            }
            default:
            {
                validateMonth(month);
                return 30;
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

    public static PureDate parsePureDateToStrictTime(String string)
    {
        return DateFormat.parseStrictTime(string, 0, string.length());
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

    static void validateHour(int hour)
    {
        if ((hour < 0) || (hour > 23))
        {
            throw new IllegalArgumentException("Invalid hour: " + hour);
        }
    }

    static void validateMinute(int minute)
    {
        if ((minute < 0) || (minute > 59))
        {
            throw new IllegalArgumentException("Invalid minute: " + minute);
        }
    }

    static void validateSecond(int second)
    {
        if ((second < 0) || (second > 59))
        {
            throw new IllegalArgumentException("Invalid second: " + second);
        }
    }

    static void validateSubsecond(String subsecond)
    {
        if (subsecond == null)
        {
            throw new IllegalArgumentException("Invalid subsecond value: null");
        }
        if (subsecond.isEmpty() || !StringIterate.isNumber(subsecond))
        {
            throw new IllegalArgumentException("Invalid subsecond value: \"" + subsecond + "\"");
        }
    }
}
