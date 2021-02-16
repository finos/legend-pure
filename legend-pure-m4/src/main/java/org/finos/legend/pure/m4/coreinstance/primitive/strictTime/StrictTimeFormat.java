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

package org.finos.legend.pure.m4.coreinstance.primitive.strictTime;

import java.io.IOException;


public class StrictTimeFormat {
    static final char STRICT_TIME_SEPARATOR = ':';
    private static final char STRICT_TIME_PREFIX = '%';

    public static void format(Appendable appendable, String formatString, PureStrictTime time) throws IOException
    {
        int length = formatString.length();
        int i = 0;
        while (i < length)
        {
            char character = formatString.charAt(i++);
            switch (character)
            {
                // Hour (1-12)
                case 'h':
                {
                    if (!time.hasHour())
                    {
                        throw new IllegalArgumentException("StrictTime has no hour: " + time);
                    }
                    int preDisplayHour = time.getHour();
                    int displayHour = (preDisplayHour == 0) ? 12 : ((preDisplayHour > 12) ? (preDisplayHour - 12) : preDisplayHour);
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(appendable, displayHour, count + 1);
                    i += count;
                    break;
                }
                // Hour (0-23)
                case 'H':
                {
                    if (!time.hasHour())
                    {
                        throw new IllegalArgumentException("StrictTime has no hour: " + time);
                    }
                    int displayHour = time.getHour();
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(appendable, displayHour, count + 1);
                    i += count;
                    break;
                }
                // Minute
                case 'm':
                {
                    if (!time.hasMinute())
                    {
                        throw new IllegalArgumentException("StrictTime has no minute: " + time);
                    }
                    int displayMinute = time.getMinute();
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(appendable, displayMinute, count + 1);
                    i += count;
                    break;
                }
                // Second
                case 's':
                {
                    if (!time.hasSecond())
                    {
                        throw new IllegalArgumentException("StrictTime has no second: " + time);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    appendZeroPaddedInt(appendable, time.getSecond(), count + 1);
                    i += count;
                    break;
                }
                // Subsecond
                case 'S':
                {
                    if (!time.hasSubsecond())
                    {
                        throw new IllegalArgumentException("StrictTime has no sub-second: " + time);
                    }
                    int count = getCharCountFrom(character, formatString, i);
                    if (count < 3)
                    {
                        int maxLen = count + 1;
                        int len = time.getSubsecond().length();
                        if (len <= maxLen)
                        {
                            appendable.append(time.getSubsecond());
                        }
                        else
                        {
                            int j = 0;
                            while (j < maxLen)
                            {
                                appendable.append(time.getSubsecond().charAt(j++));
                            }
                        }
                    }
                    else
                    {
                        appendable.append(time.getSubsecond());
                    }
                    i += count;
                    break;
                }
                // Separator
                case ':':
                case '.':
                {
                    appendable.append(character);
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException("Invalid format control character '" + character + "' in format string: " + formatString);
                }
            }
        }
    }

    public static void write(Appendable appendable, PureStrictTime time) throws IOException
    {
        appendTwoDigitInt(appendable, time.getHour());
        appendable.append(STRICT_TIME_SEPARATOR);
        appendTwoDigitInt(appendable, time.getMinute());
        if(time.hasSecond())
        {
            appendable.append(STRICT_TIME_SEPARATOR);
            appendTwoDigitInt(appendable, time.getSecond());
            if(time.hasSubsecond())
            {
                appendable.append('.');
                appendable.append(time.getSubsecond());
            }

        }
    }


    public static PureStrictTime parsePureStrictTime(String string)
    {
        return parseStrictTime(string);
    }

    /**
     * Parse a string into a Pure StrictTime.
     *
     * @param string string
     * @return Pure StrictTime
     */
    public static PureStrictTime parseStrictTime(String string)
    {
        // Skip whitespace at start and end
        string = string.trim();
        int start = 0;
        int end = string.length();
        if (start >= end)
        {
            throwInvalidStrictTimeString(string);
        }

        // Skip Pure date prefix character if present
        if (string.charAt(start) == STRICT_TIME_PREFIX)
        {
            start++;
            if (start >= end)
            {
                throwInvalidStrictTimeString(string);
            }
        }

        int previous = start;
        int index = findNonDigit(string, previous, end);

        // Hour
        int hour = -1;
        try
        {
            hour = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidStrictTimeString("Error parsing hour", string, start, end);
        }

        if (index == end || string.charAt(index++) != STRICT_TIME_SEPARATOR)
        {
            throwInvalidStrictTimeString(string, start, end);
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
            throwInvalidStrictTimeString("Error parsing minute", string, start, end);
        }

        if (index == end)
        {
            return StrictTimeWithMinute.newStrictTimeWithMinute(hour, minute);
        }

        if (string.charAt(index++) != STRICT_TIME_SEPARATOR)
        {
            throwInvalidStrictTimeString("Error parsing minute", string, start, end);
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
            throwInvalidStrictTimeString("Error parsing second", string, start, end);
        }

        if (index == end)
        {
            return StrictTimeWithSecond.newStrictTimeWithSecond(hour, minute, second);
        }

        StrictTimeWithSubsecond strictTime = null;
        if (string.charAt(index) == '.')
        {
            // Subsecond
            previous = index + 1;
            index = findNonDigit(string, previous, end);
            if (previous == end || index < end)
            {
                throwInvalidStrictTimeString("Error parsing subSecond", string, start, end);
            }
            String subsecond = string.substring(previous, index);
            return StrictTimeWithSubsecond.newStrictTimeWithSubsecond(hour, minute, second, subsecond);
        }
        else if(index < end)
        {
            throwInvalidStrictTimeString("Error parsing second", string, start, end);
        }
        else
        {
            return StrictTimeWithSecond.newStrictTimeWithSecond(hour, minute, second);
        }
        return strictTime;
    }

    private static void appendTwoDigitInt(Appendable appendable, int integer) throws IOException
    {
        appendZeroPaddedInt(appendable, integer, 2);
    }

    private static void appendZeroPaddedInt(Appendable appendable, int integer, int minLength) throws IOException
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
     * @param string time string
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

    private static void throwInvalidStrictTimeString(String string)
    {
        throwInvalidStrictTimeString(string, 0, string.length());
    }

    private static void throwInvalidStrictTimeString(String string, int start, int end)
    {
        throwInvalidStrictTimeString("Invalid StrictTime string", string, start, end);
    }

    private static void throwInvalidStrictTimeString(String message, String timeString, int start, int end)
    {
        throw new IllegalArgumentException(message + ": '" + timeString.substring(start, end).replace("'", "\\'") + "'");
    }
}
