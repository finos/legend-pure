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
import org.finos.legend.pure.m4.tools.TextTools;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
        ZonedDateTime zoned = null;
        String timeZoneId = null;
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

                    StringBuilder tzBuilder = new StringBuilder();
                    boolean done = false;
                    boolean escaped = false;
                    boolean inQuotes = false;
                    while (!done && (i < length))
                    {
                        char next = formatString.charAt(i++);
                        if (escaped)
                        {
                            tzBuilder.append(next);
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
                            tzBuilder.append(next);
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
                    timeZoneId = tzBuilder.toString();
                    ZoneId timeZone;
                    try
                    {
                        timeZone = ZoneId.of(timeZoneId, ZoneId.SHORT_IDS);
                    }
                    catch (DateTimeException e)
                    {
                        throw new IllegalArgumentException("Unknown time zone: " + timeZoneId, e);
                    }

                    if (date.hasHour())
                    {
                        // A Pure date is always understood as UTC, so the instant it starts at
                        // is what the requested zone renders.
                        zoned = PureDateToJava.start().toInstant(date).atZone(timeZone);
                    }
                    break;
                }
                // Year
                case 'y':
                {
                    int displayYear = (zoned == null) ? date.getYear() : zoned.getYear();
                    int count = getCharCountFrom(character, formatString, i);
                    if (count < 3)
                    {
                        // The two digit form is the last two digits of the year, which a year
                        // before the era has as much as one after it: it drops the sign along with
                        // the century, as it already drops the difference between 1914 and 2014.
                        appendNonNegTwoDigitInt(safeAppendable, Math.abs(displayYear % 100));
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
                    int displayMonth = (zoned == null) ? date.getMonth() : zoned.getMonthValue();
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
                    int displayDay = (zoned == null) ? date.getDay() : zoned.getDayOfMonth();
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
                    int preDisplayHour = (zoned == null) ? date.getHour() : zoned.getHour();
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
                    int displayHour = (zoned == null) ? date.getHour() : zoned.getHour();
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
                    int displayHour = (zoned == null) ? date.getHour() : zoned.getHour();
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
                    int displayMinute = (zoned == null) ? date.getMinute() : zoned.getMinute();
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
                    int displaySecond = (zoned == null) ? date.getSecond() : zoned.getSecond();
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(safeAppendable, displaySecond, count + 1);
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
                    safeAppendable.append((timeZoneId == null) ? "GMT" : timeZoneId);
                    i += count;
                    break;
                }
                // RFC 822 time zone
                case 'Z':
                {
                    if (!date.hasHour())
                    {
                        throw new IllegalArgumentException("Date has no hour (required for Z): " + date);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    appendRFC822TimeZone(safeAppendable, getOffsetInMinutes(zoned));
                    i += count;
                    break;
                }
                // ISO 8601 time zone
                case 'X':
                {
                    if (!date.hasHour())
                    {
                        throw new IllegalArgumentException("Date has no hour (required for X): " + date);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    appendISO8601TimeZone(safeAppendable, getOffsetInMinutes(zoned));
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
        int low = TextTools.indexOfNonWhitespace(string, start, end);
        if (low < 0)
        {
            throwInvalidDateString(string, start, end);
        }
        int high = TextTools.lastIndexOfNonWhitespace(string, low, end);
        if (high < 0)
        {
            throwInvalidDateString(string, low, high);
        }
        high += Character.charCount(string.codePointAt(high));
        if (low >= high)
        {
            throwInvalidDateString(string, start, end);
        }

        // Skip Pure date prefix character if present
        if (string.charAt(low) == DATE_PREFIX)
        {
            low++;
            if (low >= high)
            {
                throwInvalidDateString(string, start, end);
            }
        }

        // Year
        int year = -1;
        int previous = (string.charAt(low) == '-') ? low + 1 : low;
        int index = findNonDigit(string, previous, high);
        try
        {
            year = Integer.parseInt(string.substring(low, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing year", string, low, high);
        }

        if (index == high)
        {
            return Year.newYear(year);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, low, high);
        }

        // Month
        int month = -1;
        previous = index;
        index = findNonDigit(string, previous, high);
        try
        {
            month = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing month", string, low, high);
        }

        if (index == high)
        {
            return YearMonth.newYearMonth(year, month);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, low, high);
        }

        // Day
        int day = -1;
        previous = index;
        index = findNonDigit(string, previous, high);
        try
        {
            day = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing day", string, low, high);
        }

        if (index == high)
        {
            return StrictDate.newStrictDate(year, month, day);
        }
        if (string.charAt(index++) != DATE_TIME_SEPARATOR)
        {
            throwInvalidDateString(string, low, high);
        }

        // Hour
        int hour = -1;
        previous = index;
        index = findNonDigit(string, previous, high);
        try
        {
            hour = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing hour", string, low, high);
        }

        if (index == high)
        {
            return DateWithHour.newDateWithHour(year, month, day, hour);
        }

        if (string.charAt(index++) != TIME_SEPARATOR)
        {
            throwInvalidDateString(string, low, high);
        }

        // Minute
        int minute = -1;
        previous = index;
        index = findNonDigit(string, previous, high);
        try
        {
            minute = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing minute", string, low, high);
        }

        if (index == high)
        {
            return DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
        }

        if (string.charAt(index++) != TIME_SEPARATOR)
        {
            // Time zone
            DateWithMinute date = DateWithMinute.newDateWithMinute(year, month, day, hour, minute);
            int offsetInMinutes = getTimeZoneOffsetInMinutes(string, index - 1, high);
            return date.addMinutes(-offsetInMinutes);
        }

        // Second
        int second = -1;
        previous = index;
        index = findNonDigit(string, previous, high);
        try
        {
            second = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing second", string, low, high);
        }

        if (index == high)
        {
            return DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
        }

        PureDate date;
        if (string.charAt(index) == '.')
        {
            // Subsecond
            previous = index + 1;
            index = findNonDigit(string, previous, high);
            if (index == previous)
            {
                throwInvalidDateString(string, low, high);
            }
            String subsecond = string.substring(previous, index);
            date = DateWithSubsecond.newDateWithSubsecond(year, month, day, hour, minute, second, subsecond);
        }
        else
        {
            date = DateWithSecond.newDateWithSecond(year, month, day, hour, minute, second);
        }

        if (index < high)
        {
            // Time zone
            int offsetInMinutes = getTimeZoneOffsetInMinutes(string, index, high);
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

    /**
     * Get the offset from UTC the given date and time is at, in whole minutes. A null argument
     * means no time zone was asked for, and a Pure date with no time zone is UTC. Time zones that
     * are not a whole number of minutes from UTC are rounded towards it, as neither notation below
     * can express the seconds.
     *
     * @param zoned date and time being rendered, or null for UTC
     * @return offset from UTC in minutes
     */

    private static int getOffsetInMinutes(ZonedDateTime zoned)
    {
        return (zoned == null) ? 0 : (zoned.getOffset().getTotalSeconds() / 60);
    }

    /**
     * Append an offset in the RFC 822 notation, which is a sign, two digits of hours, and two of
     * minutes, with no separator and nothing omitted: UTC is written {@code +0000}.
     *
     * @param appendable      appendable to write to
     * @param offsetInMinutes offset from UTC in minutes
     */
    private static void appendRFC822TimeZone(SafeAppendable appendable, int offsetInMinutes)
    {
        int absolute = Math.abs(offsetInMinutes);
        appendable.append((offsetInMinutes < 0) ? '-' : '+');
        appendNonNegTwoDigitInt(appendable, absolute / 60);
        appendNonNegTwoDigitInt(appendable, absolute % 60);
    }

    /**
     * Append an offset in the ISO 8601 notation, which writes UTC as {@code Z} and omits the
     * minutes of an offset that has none, but keeps them where there are any: an offset of five and
     * a half hours is {@code +0530}, not {@code +05}.
     *
     * @param appendable      appendable to write to
     * @param offsetInMinutes offset from UTC in minutes
     */
    private static void appendISO8601TimeZone(SafeAppendable appendable, int offsetInMinutes)
    {
        if (offsetInMinutes == 0)
        {
            appendable.append('Z');
            return;
        }

        int absolute = Math.abs(offsetInMinutes);
        appendable.append((offsetInMinutes < 0) ? '-' : '+');
        appendNonNegTwoDigitInt(appendable, absolute / 60);
        int minutes = absolute % 60;
        if (minutes != 0)
        {
            appendNonNegTwoDigitInt(appendable, minutes);
        }
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

    private static void throwInvalidDateString(String string, int start, int end)
    {
        throwInvalidDateString("Invalid date string", string, start, end);
    }

    private static void throwInvalidDateString(String message, String dateString, int start, int end)
    {
        throw new IllegalArgumentException(message + ": '" + dateString.substring(start, end).replace("'", "\\'") + "'");
    }
}
