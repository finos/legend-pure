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
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateFormat
{
    static final char DATE_SEPARATOR = '-';
    static final char TIME_SEPARATOR = ':';
    static final char DATE_TIME_SEPARATOR = 'T';

    private static final char DATE_PREFIX = '%';

    /**
     * Write a Pure date to an appendable in the form a format string describes.
     *
     * <p>Each character of the format string is a control character standing for one component of
     * the date, except for the separators {@code - / : . }, tab and space, which are written out as
     * they appear, and text in double quotes, which is written out without them. Within quoted text
     * a backslash escapes the character after it, so a quote may be written {@code \"}; a backslash
     * outside quotes is an error, as is any other character with no meaning here. Repeating a
     * control character widens the field it writes: {@code d} gives 1 where {@code dd} gives 01.
     * Two things are not control characters at all: a sub-second field takes a width instead of a
     * run of letters, and {@code ?[}, {@code |}, and {@code ]} open, divide, and close an optional
     * section. Both are described below.
     *
     * <table border="1">
     * <caption>Control characters</caption>
     * <tr><th>Character</th><th>Component</th><th>Example</th></tr>
     * <tr><td>{@code y}</td><td>year; one to three of them write the last two digits, four or more
     * the whole year</td><td>{@code yy} 14, {@code yyyy} 2014</td></tr>
     * <tr><td>{@code M}</td><td>month</td><td>{@code MM} 03</td></tr>
     * <tr><td>{@code d}</td><td>day of the month</td><td>{@code dd} 10</td></tr>
     * <tr><td>{@code H}</td><td>hour, 0 to 23</td><td>{@code HH} 16</td></tr>
     * <tr><td>{@code h}</td><td>hour, 1 to 12</td><td>{@code hh} 04</td></tr>
     * <tr><td>{@code a}</td><td>AM or PM; repeating it makes no difference, as there is no width to
     * widen</td><td>PM</td></tr>
     * <tr><td>{@code m}</td><td>minute</td><td>{@code mm} 12</td></tr>
     * <tr><td>{@code s}</td><td>second</td><td>{@code ss} 35</td></tr>
     * <tr><td>{@code S}</td><td>sub-second; this one takes a width rather than a run of letters,
     * and the forms it takes are below</td><td>{@code S3} 070</td></tr>
     * <tr><td>{@code z}</td><td>general time zone: the zone as written in the format string, or GMT
     * if none was</td><td>{@code z} EST</td></tr>
     * <tr><td>{@code Z}</td><td>RFC 822 time zone: always a sign, two digits of hours and two of
     * minutes; requires an hour</td><td>{@code Z} -0500</td></tr>
     * <tr><td>{@code X}</td><td>ISO 8601 time zone: Z for UTC, otherwise a sign, hours, and minutes
     * only when the offset has them; requires an hour</td><td>{@code X} -05</td></tr>
     * </table>
     *
     * <p>A sub-second field says how many digits of the fraction of a second to write, which is
     * the one place the format language says more than a run of letters can. Every form of it fails
     * on a date with no fraction at all: how many digits to write is the field's business, and
     * whether there are any to write is not.
     *
     * <table border="1">
     * <caption>Sub-second widths</caption>
     * <tr><th>Field</th><th>Digits written</th><th>{@code .070004235}</th><th>{@code .07}</th></tr>
     * <tr><td>{@code SN}</td><td>exactly N, padding a shorter fraction</td><td>{@code S3} 070</td>
     * <td>{@code S3} 070</td></tr>
     * <tr><td>{@code S<N}</td><td>at most N, and fewer where that is all there is</td>
     * <td>{@code S<3} 070</td><td>{@code S<3} 07</td></tr>
     * <tr><td>{@code S>N}</td><td>at least N, padding a shorter fraction, and everything beyond</td>
     * <td>{@code S>3} 070004235</td><td>{@code S>3} 070</td></tr>
     * <tr><td>{@code S*}</td><td>however many the date has</td><td>{@code S*} 070004235</td>
     * <td>{@code S*} 07</td></tr>
     * <tr><td>{@code S!N}</td><td>exactly N, failing rather than padding a shorter fraction</td>
     * <td>{@code S!3} 070</td><td>{@code S!3} fails</td></tr>
     * <tr><td>{@code S} to {@code SSS}</td><td>at most one, two, or three; the same fields as
     * {@code S<1} to {@code S<3}</td><td>{@code SSS} 070</td><td>{@code SSS} 07</td></tr>
     * <tr><td>{@code SSSS} and longer</td><td>however many the date has; the same field as
     * {@code S*}</td><td>{@code SSSS} 070004235</td><td>{@code SSSS} 07</td></tr>
     * </table>
     *
     * <p>The general form, {@code S(min,max)}, says both bounds at once: {@code *} for an unbounded
     * maximum, {@code !} after either bound to fail rather than pad or truncate there, and an
     * optional third part naming a fill character other than the zero padding otherwise uses. So
     * {@code S(3!,9,"_")} writes between three and nine digits, refuses a fraction shorter than
     * three rather than padding it, and pads with underscores. A marker with nothing to act on is
     * legal and does nothing.
     *
     * <p>Truncating and padding are not two sides of one thing. A Pure date is a span of time rather
     * than an instant, and the number of digits it stores is the precision of that span, so
     * {@code .07} contains {@code .070}, {@code .071}, and {@code .0712}. Truncating widens the
     * span, which loses precision but stays true; padding narrows it, which claims a precision the
     * date does not have. That is why only padding has a short spelling for refusing it.
     *
     * <p>A run of the format string in {@code ?[...]} is an optional section, written where the date
     * can carry it and left out where it cannot. Within one, {@code |} separates alternatives: the
     * first alternative every element of which can write the date is the one written, and where none
     * of them can, the section writes nothing. So {@code yyyy-MM-dd?[" at "HH:mm]} writes the time
     * where the date has one and the date alone where it does not, and
     * {@code ?[HH:mm:ss|HH:mm|HH|"--"]} writes as much of the time as the date carries. A section
     * never fails, so a pattern whose date-dependent parts all sit inside one writes any date at
     * all.
     *
     * <p>Sections nest, and there is one thing to know about nesting: a section asks the date for
     * nothing on its own account, so it adds no requirement to the alternative holding it.
     *
     * <pre>
     * ?[HH:mm?[:ss]]        on 13:07 writes 13:07     -- inner writes nothing, outer unaffected
     * ?[" at "?[HH:mm]]     on a date with no hour writes " at "
     * ?[" at "HH:mm]        on a date with no hour writes nothing
     * </pre>
     *
     * <p>The second line is the one that surprises. The outer alternative holds a literal and a
     * section, both of which can always write, so the outer section writes and the inner one does
     * not. Hoisting the literal out, as the third line does, is what makes the two rise and fall
     * together.
     *
     * <p>Sections are also how a date with no fraction of a second is written, since a sub-second
     * field says only how wide a fraction is and never whether there is one:
     * {@code HH:mm:ss?[.S3]} writes {@code 13:07:44.070} or {@code 13:07:44}, and the decimal point
     * goes with the digits because the section carries both. {@code ?[.S3|".000"]} invents the
     * fraction instead, which is worth writing out rather than abbreviating, since it claims a
     * precision the date does not have.
     *
     * <p>An alternative may not be empty: {@code ?[|HH]} and {@code ?[HH|]} are errors, the first
     * because it would write nothing whatever the date, and the second because it says what
     * {@code ?[HH]} already says. {@code ""} spells an empty alternative where one is meant.
     * A throwing sub-second bound inside a section selects the next alternative rather than raising,
     * which is the one thing a section takes away.
     *
     * <p>A format string may open with a time zone in square brackets, as in
     * {@code [America/New_York]yyyy-MM-dd HH:mm:ss}, and only open with one: a zone appearing after
     * anything has been written is an error, since the components before it would already have been
     * written in another zone. The name may be anything {@link ZoneId#of(String, java.util.Map)}
     * accepts against {@link ZoneId#SHORT_IDS} - a region such as {@code America/New_York}, one of
     * the three letter abbreviations such as {@code EST}, or an offset such as {@code GMT+5} or
     * {@code +05:30} - and a name it does not accept is an error rather than a silent fall back to
     * UTC.
     *
     * <p>A Pure date is always understood as UTC, so a zone shifts the date to the instant it
     * stands for as that zone reads it, and every component comes from the shifted reading. A date
     * with no hour is not an instant and so is never shifted; {@code z} still names the zone that
     * was asked for, but {@code Z} and {@code X} are an error, since an offset belongs to an
     * instant and a zone can be keeping more than one over a date that broad.
     *
     * <p>The format string is read in full before anything is written, so a malformed one leaves
     * the appendable as it found it. A component the date does not have is a fault of the pair
     * rather than of the format string, and is found where it is reached, so a date short of what
     * the string asks for may have part of itself written before the error.
     *
     * <p>{@link DateFormatPattern} is this method's working: it holds a format string already read,
     * and can be built directly by a caller with no format string to hand.
     *
     * @param appendable   appendable to write to
     * @param formatString format string
     * @param date         date to write
     * @param <T>          appendable type
     * @return the appendable
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, sets a time zone anywhere but at the
     *                                  start, or asks for a component the date does not have
     */
    public static <T extends Appendable> T format(T appendable, String formatString, PureDate date)
    {
        return format(appendable, formatString, 0, formatString.length(), date);
    }

    /**
     * Write a Pure date to an appendable in the form a portion of a string describes, which is how
     * a format string held inside a larger one is used without first cutting it out.
     *
     * @param appendable   appendable to write to
     * @param formatString string holding the format string
     * @param start        start index of the format string (inclusive)
     * @param end          end index of the format string (exclusive)
     * @param date         date to write
     * @param <T>          appendable type
     * @return the appendable
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, sets a time zone anywhere but at the
     *                                  start, or asks for a component the date does not have
     * @see #format(Appendable, String, PureDate)
     */
    public static <T extends Appendable> T format(T appendable, String formatString, int start, int end, PureDate date)
    {
        return DateFormatPattern.parse(formatString, start, end).render(appendable, date);
    }

    /**
     * Check that a format string is one {@link #format} can use, and do nothing else. Everything
     * this reports is a property of the format string alone, so a string that passes will render
     * any date carrying the components it names.
     *
     * @param formatString format string
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, or sets a time zone anywhere but at the
     *                                  start
     */
    public static void validate(String formatString)
    {
        validate(formatString, 0, formatString.length());
    }

    /**
     * Check that a portion of a string is a format string {@link #format} can use, and do nothing
     * else.
     *
     * @param formatString string holding the format string
     * @param start        start index of the format string (inclusive)
     * @param end          end index of the format string (exclusive)
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, or sets a time zone anywhere but at the
     *                                  start
     * @see #validate(String)
     */
    public static void validate(String formatString, int start, int end)
    {
        DateFormatPattern.parse(formatString, start, end);
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
    static int getOffsetInMinutes(ZonedDateTime zoned)
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
    static void appendRFC822TimeZone(SafeAppendable appendable, int offsetInMinutes)
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
    static void appendISO8601TimeZone(SafeAppendable appendable, int offsetInMinutes)
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

    static void appendNonNegTwoDigitInt(SafeAppendable appendable, int integer)
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

    static void appendZeroPaddedInt(SafeAppendable appendable, int integer, int minLength)
    {
        String string = Integer.toString(integer);
        for (int fill = minLength - string.length(); fill > 0; fill--)
        {
            appendable.append('0');
        }
        appendable.append(string);
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
