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

public abstract class TimeFunctions
{
    public static void validateHour(int hour)
    {
        if ((hour < 0) || (hour > 23))
        {
            throw new IllegalArgumentException("Invalid hour: " + hour);
        }
    }

    public static void validateMinute(int minute)
    {
        if ((minute < 0) || (minute > 59))
        {
            throw new IllegalArgumentException("Invalid minute: " + minute);
        }
    }

    public static void validateSecond(int second)
    {
        if ((second < 0) || (second > 59))
        {
            throw new IllegalArgumentException("Invalid second: " + second);
        }
    }

    public static void validateSubsecond(String subsecond)
    {
        if (subsecond == null)
        {
            throw new IllegalArgumentException("Invalid subsecond value: null");
        }
        if (subsecond.isEmpty() || !subsecond.codePoints().allMatch(TimeFunctions::isAsciiDigit))
        {
            throw new IllegalArgumentException("Invalid subsecond value: \"" + subsecond + "\"");
        }
    }

    /**
     * A subsecond has to be written in the digits it will be read in. Character.isDigit is true of
     * every decimal digit Unicode has, so it would accept a subsecond written in, say, Persian or
     * Devanagari digits, which the code that adds to one a character at a time would then read as
     * something other than a number.
     */
    private static boolean isAsciiDigit(int codePoint)
    {
        return ('0' <= codePoint) && (codePoint <= '9');
    }

    /**
     * Get the subsecond part of a number of milliseconds, to three digits.
     *
     * @param milliseconds number of milliseconds, of either sign
     * @return subsecond part, without a sign
     */
    public static String subsecondFromMilliseconds(long milliseconds)
    {
        return zeroPadded(Math.abs(milliseconds % 1_000L), 3);
    }

    /**
     * Get the subsecond part of a number of microseconds, to six digits.
     *
     * @param microseconds number of microseconds, of either sign
     * @return subsecond part, without a sign
     */
    public static String subsecondFromMicroseconds(long microseconds)
    {
        return zeroPadded(Math.abs(microseconds % 1_000_000L), 6);
    }

    /**
     * Get the subsecond part of a number of nanoseconds, to nine digits.
     *
     * @param nanoseconds number of nanoseconds, of either sign
     * @return subsecond part, without a sign
     */
    public static String subsecondFromNanoseconds(long nanoseconds)
    {
        return zeroPadded(Math.abs(nanoseconds % 1_000_000_000L), 9);
    }

    /**
     * A subsecond is held as text so that it keeps the precision it was written with: 1 is a tenth
     * of a second and 100 is a hundred milliseconds, which the same number could not tell apart.
     * Everything that reads one back reads it as digits, comparing them and adding to them a
     * character at a time, so the digits are built here rather than formatted: String.format
     * without a locale numbers in whatever the default locale asks for, and not every locale asks
     * for 0 to 9.
     */
    private static String zeroPadded(long value, int digits)
    {
        char[] chars = new char[digits];
        long remaining = value;
        for (int i = digits - 1; i >= 0; i--)
        {
            chars[i] = (char) ('0' + (remaining % 10L));
            remaining /= 10L;
        }
        return new String(chars);
    }
}
