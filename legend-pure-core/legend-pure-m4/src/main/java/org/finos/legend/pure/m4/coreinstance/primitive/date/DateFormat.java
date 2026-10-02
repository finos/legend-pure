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

import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateFormat
{
    static final char DATE_SEPARATOR = '-';
    static final char TIME_SEPARATOR = ':';
    static final char DATE_TIME_SEPARATOR = 'T';

    private static final char DATE_PREFIX = '%';

    public static <T extends Appendable> T format(T appendable, String formatString, PureDate date)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        int length = formatString.length();
        GregorianCalendar calendar = null;
        StringBuilder timeZoneId = new StringBuilder();
        int i = 0;
        while (i < length)
        {
            char character = formatString.charAt(i++);
            switch (character)
            {
                // Timezone conversion
                case '[':
                {
                    if (i > 1)
                    {
                        throw new IllegalArgumentException("Time zone can only be set at the beginning of the format string");
                    }

                    boolean done = false;
                    boolean escaped = false;
                    boolean inQuotes = false;
                    while (!done && (i < length))
                    {
                        char next = formatString.charAt(i++);
                        if (escaped)
                        {
                            timeZoneId.append(next);
                            escaped = false;
                        }
                        else if (next == '"')
                        {
                            inQuotes = !inQuotes;
                        }
                        else if ((next == ']') && !inQuotes)
                        {
                            done = true;
                        }
                        else if (next == '\\')
                        {
                            escaped = true;
                        }
                        else
                        {
                            timeZoneId.append(next);
                        }
                    }
                    if (inQuotes)
                    {
                        throw new IllegalArgumentException("Missing closing quotes in time zone definition: " + formatString);
                    }
                    if (!done)
                    {
                        throw new IllegalArgumentException("Missing closing bracket in format string: " + formatString);
                    }
                    TimeZone timeZone;
                    try
                    {
                        timeZone = TimeZone.getTimeZone(timeZoneId.toString());
                    }
                    catch (RuntimeException e)
                    {
                        throw new IllegalArgumentException("Unknown time zone: " + timeZoneId);
                    }

                    if (date.hasHour())
                    {
                        if (calendar == null)
                        {
                            calendar = date.getCalendar();
                            calendar.setTimeZone(timeZone);
                            calendar.add(Calendar.MILLISECOND, timeZone.getOffset(calendar.getTimeInMillis()));
                        }
                    }
                    break;
                }
                // Year
                case 'y':
                {
                    int displayYear = (calendar == null) ? date.getYear() : calendar.get(Calendar.YEAR);
                    int count = getCharCountFrom(character, formatString, i);
                    if (count < 3)
                    {
                        appendNonNegTwoDigitInt(safeAppendable, displayYear % 100);
                    }
                    else
                    {
                        safeAppendable.append(displayYear);
                    }
                    i += count;
                    break;
                }
                // Month
                case 'M':
                {
                    if (!date.hasMonth())
                    {
                        throw new IllegalArgumentException("Date has no month: " + date);
                    }
                    int displayMonth = (calendar == null) ? date.getMonth() : (calendar.get(Calendar.MONTH) + 1);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displayMonth, count + 1);
                    i += count;
                    break;
                }
                // Day
                case 'd':
                {
                    if (!date.hasDay())
                    {
                        throw new IllegalArgumentException("Date has no day: " + date);
                    }
                    int displayDay = (calendar == null) ? date.getDay() : calendar.get(Calendar.DAY_OF_MONTH);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displayDay, count + 1);
                    i += count;
                    break;
                }
                // Hour (1-12)
                case 'h':
                {
                    if (!date.hasHour())
                    {
                        throw new IllegalArgumentException("Date has no hour: " + date);
                    }
                    int preDisplayHour = (calendar == null) ? date.getHour() : calendar.get(Calendar.HOUR_OF_DAY);
                    int displayHour = (preDisplayHour == 0) ? 12 : ((preDisplayHour > 12) ? (preDisplayHour - 12) : preDisplayHour);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displayHour, count + 1);
                    i += count;
                    break;
                }
                // Hour (0-23)
                case 'H':
                {
                    if (!date.hasHour())
                    {
                        throw new IllegalArgumentException("Date has no hour: " + date);
                    }
                    int displayHour = (calendar == null) ? date.getHour() : calendar.get(Calendar.HOUR_OF_DAY);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displayHour, count + 1);
                    i += count;
                    break;
                }
                // AM/PM
                case 'a':
                {
                    if (!date.hasHour())
                    {
                        throw new IllegalArgumentException("Date has no hour: " + date);
                    }
                    int displayHour = (calendar == null) ? date.getHour() : calendar.get(Calendar.HOUR_OF_DAY);
                    safeAppendable.append((displayHour < 12) ? "AM" : "PM");
                    break;
                }
                // Minute
                case 'm':
                {
                    if (!date.hasMinute())
                    {
                        throw new IllegalArgumentException("Date has no minute: " + date);
                    }
                    int displayMinute = (calendar == null) ? date.getMinute() : calendar.get(Calendar.MINUTE);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displayMinute, count + 1);
                    i += count;
                    break;
                }
                // Second
                case 's':
                {
                    if (!date.hasSecond())
                    {
                        throw new IllegalArgumentException("Date has no second: " + date);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, date.getSecond(), count + 1);
                    i += count;
                    break;
                }
                // Subsecond
                case 'S':
                {
                    if (!date.hasSubsecond())
                    {
                        throw new IllegalArgumentException("Date has no sub-second: " + date);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    if (count < 3)
                    {
                        int maxLen = count + 1;
                        int len = date.getSubsecond().length();
                        if (len <= maxLen)
                        {
                            safeAppendable.append(date.getSubsecond());
                        }
                        else
                        {
                            int j = 0;
                            while (j < maxLen)
                            {
                                safeAppendable.append(date.getSubsecond().charAt(j++));
                            }
                        }
                    }
                    else
                    {
                        safeAppendable.append(date.getSubsecond());
                    }
                    i += count;
                    break;
                }
                // General time zone
                case 'z':
                {
                    int count = getCharCountFrom(character, formatString, i);
                    if (calendar == null)
                    {
                        safeAppendable.append("GMT");
                    }
                    else
                    {
                        safeAppendable.append(timeZoneId);
                    }
                    i += count;
                    break;
                }
                // RFC 822 time zone
                case 'Z':
                {
                    int count = getCharCountFrom(character, formatString, i);
                    if (calendar == null)
                    {
                        safeAppendable.append("+0000");
                    }
                    else
                    {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("Z");
                        dateFormat.setTimeZone(calendar.getTimeZone());
                        safeAppendable.append(dateFormat.format(calendar.getTime()));
                    }
                    i += count;
                    break;
                }
                // ISO 8601 time zone
                case 'X':
                {
                    int count = getCharCountFrom(character, formatString, i);
                    if (calendar == null)
                    {
                        safeAppendable.append("Z");
                    }
                    else
                    {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("X");
                        dateFormat.setTimeZone(calendar.getTimeZone());
                        safeAppendable.append(dateFormat.format(calendar.getTime()));
                    }
                    i += count;
                    break;
                }
                // Separator
                case '-':
                case '/':
                case ':':
                case '.':
                case ' ':
                case '\t':
                {
                    safeAppendable.append(character);
                    break;
                }
                // Quote
                case '"':
                {
                    boolean done = false;
                    boolean escaped = false;
                    while (!done && (i < length))
                    {
                        char next = formatString.charAt(i++);
                        if (escaped)
                        {
                            safeAppendable.append(next);
                            escaped = false;
                        }
                        else if (next == '"')
                        {
                            done = true;
                        }
                        else if (next == '\\')
                        {
                            escaped = true;
                        }
                        else
                        {
                            safeAppendable.append(next);
                        }
                    }
                    if (!done)
                    {
                        throw new IllegalArgumentException("Missing closing quote in format string: " + formatString);
                    }
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException("Invalid format control character '" + character + "' in format string: " + formatString);
                }
            }
        }
        return appendable;
    }

    @Deprecated
    public static void write(Appendable appendable, PureDate date) throws IOException
    {
        append(appendable, date);
    }

    public static <T extends Appendable> T append(T appendable, PureDate date)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append(date.getYear());
        if (date.hasMonth())
        {
            appendNonNegTwoDigitInt(safeAppendable.append(DATE_SEPARATOR), date.getMonth());
            if (date.hasDay())
            {
                appendNonNegTwoDigitInt(safeAppendable.append(DATE_SEPARATOR), date.getDay());
                if (date.hasHour())
                {
                    appendNonNegTwoDigitInt(safeAppendable.append(DATE_TIME_SEPARATOR), date.getHour());
                    if (date.hasMinute())
                    {
                        appendNonNegTwoDigitInt(safeAppendable.append(TIME_SEPARATOR), date.getMinute());
                        if (date.hasSecond())
                        {
                            appendNonNegTwoDigitInt(safeAppendable.append(TIME_SEPARATOR), date.getSecond());
                            if (date.hasSubsecond())
                            {
                                safeAppendable.append('.').append(date.getSubsecond());
                            }
                        }
                        safeAppendable.append("+0000");
                    }
                }
            }
        }
        return appendable;
    }

    public static StrictDate parseStrictDate(String string)
    {
        return (StrictDate) parsePureDate(string);
    }

    public static DateTime parseDateTime(String string)
    {
        return (DateTime) parsePureDate(string);
    }

    /**
     * Parse a string into a Pure date.
     *
     * @param string string
     * @return Pure date
     */
    public static PureDate parsePureDate(String string)
    {
        return parsePureDate(string, 0, string.length());
    }

    /**
     * Parse a portion of a string into a Pure date.
     *
     * @param string string
     * @param start  start index of the date (inclusive)
     * @param end    end index of the date (exclusive)
     * @return Pure date
     */
    public static PureDate parsePureDate(String string, int start, int end)
    {
        // Skip whitespace at start and end
        while ((start < end) && (string.charAt(start) <= ' '))
        {
            start++;
        }

        do
        {
            end--;
        }
        while ((end > start) && (string.charAt(end) <= ' '));

        end++;
        if (start >= end)
        {
            throwInvalidDateString(string);
        }

        // Skip Pure date prefix character if present
        if (string.charAt(start) == DATE_PREFIX)
        {
            start++;
            if (start >= end)
            {
                throwInvalidDateString(string);
            }
        }

        // Year
        int year = -1;
        int previous = (string.charAt(start) == '-') ? start + 1 : start;
        int index = findNonDigit(string, previous, end);
        try
        {
            year = Integer.parseInt(string.substring(start, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing year", string, start, end);
        }

        if (index == end)
        {
            return Year.newYear(year);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Month
        int month = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            month = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing month", string, start, end);
        }

        if (index == end)
        {
            return YearMonth.newYearMonth(year, month);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Day
        int day = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            day = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing day", string, start, end);
        }

        if (index == end)
        {
            return StrictDate.newStrictDate(year, month, day);
        }
        if (string.charAt(index++) != DATE_TIME_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Hour
        int hour = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            hour = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing hour", string, start, end);
        }

        if (index == end)
        {
            return DateWithHour.newDateWithHour(year, month, day, hour);
        }

        if (string.charAt(index++) != TIME_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Minute
        int minute = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            minute = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing minute", string, start, end);
        }

        if (index == end)
        {
            return DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
        }

        if (string.charAt(index++) != TIME_SEPARATOR)
        {
            // Time zone
            DateWithMinute date = DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
            int offsetInMinutes = getTimeZoneOffsetInMinutes(string, index - 1, end);
            return date.addMinutes(-offsetInMinutes);
        }

        // Second
        int second = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            second = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing second", string, start, end);
        }

        if (index == end)
        {
            return DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
        }

        PureDate date;
        if (string.charAt(index) == '.')
        {
            // Subsecond
            previous = index + 1;
            index = findNonDigit(string, previous, end);
            if (index == previous)
            {
                throwInvalidDateString(string, start, end);
            }
            String subsecond = string.substring(previous, index);
            date = DateWithSubsecond.newDateWithSubsecond(year, month, day, hour, minute, second, subsecond);
        }
        else
        {
            date = DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
        }

        if (index < end)
        {
            // Time zone
            int offsetInMinutes = getTimeZoneOffsetInMinutes(string, index, end);
            return date.addMinutes(-offsetInMinutes);
        }

        return date;
    }


    private static int getTimeZoneOffsetInMinutes(String string, int start, int end)
    {
        if (((end - start) == 1) && (string.charAt(start) == 'Z'))
        {
            // time zone = Z, which means UTC: no adjustment necessary
            return 0;
        }

        boolean negative;
        switch (string.charAt(start++))
        {
            case '+':
            {
                negative = false;
                break;
            }
            case '-':
            {
                negative = true;
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
            }
        }
        if (end - start != 4)
        {
            throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
        }

        int hourOffset = Integer.parseInt(string.substring(start, start + 2));
        int minuteOffset = Integer.parseInt(string.substring(start + 2, end));
        int totalOffset = (hourOffset * 60) + minuteOffset;
        return negative ? -totalOffset : totalOffset;
    }

    private static void appendNonNegTwoDigitInt(SafeAppendable appendable, int integer)
    {
        char c1;
        char c2;
        if (integer < 10)
        {
            c1 = '0';
            c2 = (char) ('0' + integer);
        }
        else
        {
            c1 = (char) ('0' + (integer / 10));
            c2 = (char) ('0' + (integer % 10));
        }
        appendable.append(c1).append(c2);
    }

    private static void appendZeroPaddedInt(SafeAppendable appendable, int integer, int minLength)
    {
        String string = Integer.toString(integer);
        for (int fill = minLength - string.length(); fill > 0; fill--)
        {
            appendable.append('0');
        }
        appendable.append(string);
    }

    private static int getCharCountFrom(char character, String string, int start)
    {
        int count = 0;
        for (int i = start, length = string.length(); (i < length) && (string.charAt(i) == character); i++)
        {
            count++;
        }
        return count;
    }

    /**
     * Return the index of the first character in string
     * between start and end that is not a digit.  Returns
     * end if no non-digit character is found.
     *
     * @param string date string
     * @param start  start index for search (inclusive)
     * @param end    end index for search (exclusive)
     * @return index of the first non-digit character
     */
    private static int findNonDigit(String string, int start, int end)
    {
        while ((start < end) && isDigit(string.charAt(start)))
        {
            start++;
        }
        return start;
    }

    private static boolean isDigit(char character)
    {
        return ('0' <= character) && (character <= '9');
    }

    private static void throwInvalidDateString(String string)
    {
        throwInvalidDateString(string, 0, string.length());
    }

    private static void throwInvalidDateString(String string, int start, int end)
    {
        throwInvalidDateString("Invalid date string", string, start, end);
    }

    private static void throwInvalidDateString(String message, String dateString, int start, int end)
    {
        throw new IllegalArgumentException(message + ": '" + dateString.substring(start, end).replace("'", "\\'") + "'");
    }
}
