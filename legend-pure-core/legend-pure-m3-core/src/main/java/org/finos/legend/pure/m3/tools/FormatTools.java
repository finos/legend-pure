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

package org.finos.legend.pure.m3.tools;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class FormatTools
{
    /**
     * Check that a format string is one {@code format} can use, and do nothing else. Everything this
     * reports is a property of the format string alone: that each specifier in it is one that exists
     * and is written the way that specifier is written, and that each date pattern in it could write
     * some date.
     *
     * <p>What it deliberately does not report is whether the specifiers and the arguments agree, in
     * number or in kind. The arguments are an {@code Any[*]} that is frequently computed, so a
     * format string has to be free to be right for arguments this cannot see.
     *
     * <p>This is the specifier grammar the two engines implement as they render, in
     * {@code PureStringFormat} for the compiled engine and {@code Format} for the interpreted one,
     * written once more so that it can be checked without a date to write or an argument to take.
     * The two must agree; a format string this accepts and they reject is a fault here.
     *
     * @param formatString format string
     * @throws IllegalArgumentException if the format string holds something that is not a
     *                                  specifier, a specifier that is not written the way that one
     *                                  is written, or a date pattern that could never write a date
     */
    public static void validate(String formatString)
    {
        int length = formatString.length();
        int index = 0;
        while (index < length)
        {
            if (formatString.charAt(index++) == '%')
            {
                index = validateSpecifier(formatString, index);
            }
        }
    }

    /**
     * Check the specifier beginning at the given index, the {@code %} having been taken.
     *
     * @param formatString format string
     * @param index        index just past the {@code %}
     * @return index to go on reading the format string from
     */
    private static int validateSpecifier(String formatString, int index)
    {
        if (index >= formatString.length())
        {
            throw new IllegalArgumentException("Format string ends with '%': " + formatString);
        }

        // the character after the % is taken either way, so the second % of %% cannot begin one
        char specifier = formatString.charAt(index++);
        switch (specifier)
        {
            case '%':
            case 's':
            case 'r':
            case 'd':
            case 'f':
            {
                return index;
            }
            case 't':
            {
                int end = findEndOfDateFormatString(formatString, index);
                if (end == -1)
                {
                    // no pattern follows, so the date is written in its canonical form and there is
                    // nothing here to be wrong
                    return index;
                }
                DateFormat.validate(formatString, index + 1, end);
                return end + 1;
            }
            case '0':
            {
                return validateWidth(formatString, index - 1, 'd');
            }
            case '.':
            {
                return validateWidth(formatString, index - 1, 'f');
            }
            default:
            {
                throw new IllegalArgumentException("Invalid format specifier: %" + specifier);
            }
        }
    }

    /**
     * Check the digits and the closing character of a specifier that takes a width, which is
     * {@code %0<digits>d} and {@code %.<digits>f}. At least one digit is required, since the digits
     * are read as a number and there is no number to read where there are none.
     *
     * @param formatString format string
     * @param start        index of the character that opened the width, the {@code 0} or the dot
     * @param closer       character the specifier ends with
     * @return index to go on reading the format string from
     */
    private static int validateWidth(String formatString, int start, char closer)
    {
        int length = formatString.length();
        int end = start + 1;
        while ((end < length) && Character.isDigit(formatString.charAt(end)))
        {
            end++;
        }
        if ((end == (start + 1)) || (end >= length) || (formatString.charAt(end) != closer))
        {
            int stop = Math.min(end + 1, length);
            throw new IllegalArgumentException("Invalid format specifier: %" + formatString.substring(start, stop));
        }
        return end + 1;
    }

    public static int findEndOfDateFormatString(String formatString, int start)
    {
        int length = formatString.length();
        if ((start >= length) || formatString.charAt(start) != '{')
        {
            return -1;
        }

        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = start + 1; i < length; i++)
        {
            char next = formatString.charAt(i);
            if (inQuotes)
            {
                if (next == '"')
                {
                    if (!escaped)
                    {
                        inQuotes = false;
                    }
                }
                else if (next == '\\')
                {
                    escaped = !escaped;
                }
            }
            else if (next == '"')
            {
                inQuotes = true;
            }
            else if (next == '}')
            {
                return i;
            }
        }
        throw new IllegalArgumentException("Could not find end of date format starting at index " + start + " of: " + formatString);
    }

    public static void appendIntegerString(StringBuilder builder, String intString, int zeroPadding)
    {
        if (zeroPadding <= 0)
        {
            builder.append(intString);
        }
        else if (isSigned(intString))
        {
            int signCount = getSignCount(intString);
            int length = intString.length();
            int digitCount = length - signCount;
            builder.append(intString, 0, signCount);
            appendZeros(builder, zeroPadding - digitCount);
            builder.append(intString, signCount, length);
        }
        else
        {
            appendZeros(builder, zeroPadding - intString.length());
            builder.append(intString);
        }
    }

    public static void appendFloatString(StringBuilder builder, String floatString)
    {
        appendFloatString(builder, floatString, -1);
    }

    public static void appendFloatString(StringBuilder builder, String floatString, int decimalPrecision)
    {
        if (decimalPrecision == -1)
        {
            builder.append(floatString);
        }
        else if (decimalPrecision == 0)
        {
            int decimalIndex = floatString.indexOf('.');
            if (decimalIndex == -1)
            {
                builder.append(floatString);
            }
            else if (decimalIndex == (floatString.length() - 1))
            {
                // the float string ends with a decimal - this really shouldn't happen
                builder.append(floatString, 0, decimalIndex);
            }
            else
            {
                char charAfterDecimal = floatString.charAt(decimalIndex + 1);
                if (charAfterDecimal < '5')
                {
                    builder.append(floatString, 0, decimalIndex);
                }
                else
                {
                    int roundingPrecision = decimalIndex - getSignCount(floatString);
                    String roundedFloatString = new BigDecimal(floatString).round(new MathContext(roundingPrecision, RoundingMode.HALF_EVEN)).toString();
                    int roundedDecimalIndex = roundedFloatString.indexOf('.');
                    if (roundedDecimalIndex == -1)
                    {
                        builder.append(roundedFloatString);
                    }
                    else
                    {
                        builder.append(roundedFloatString, 0, roundedDecimalIndex);
                    }
                }
            }
        }
        else
        {
            int decimalIndex = floatString.indexOf('.');
            if (decimalIndex == -1)
            {
                builder.append(floatString);
                builder.append('.');
                appendZeros(builder, decimalPrecision);
            }
            else
            {
                int decimalCount = floatString.length() - decimalIndex - 1;
                if (decimalCount <= decimalPrecision)
                {
                    builder.append(floatString);
                    appendZeros(builder, decimalPrecision - decimalCount);
                }
                else
                {
                    int signCount = getSignCount(floatString);
                    int leadingZeroesBeforeDecimal = getZeroCountFrom(floatString, signCount);
                    int insignificantCharactersBeforeDecimal = signCount + leadingZeroesBeforeDecimal;
                    int significantDigitsBeforeDecimal = decimalIndex - insignificantCharactersBeforeDecimal;

                    int roundingPrecision = decimalPrecision;
                    if (significantDigitsBeforeDecimal > 0)
                    {
                        roundingPrecision += significantDigitsBeforeDecimal;
                    }
                    else
                    {
                        roundingPrecision -= getZeroCountFrom(floatString, decimalIndex + 1);
                    }
                    if (roundingPrecision > 0)
                    {
                        String roundedFloatString = new BigDecimal(floatString).round(new MathContext(roundingPrecision, RoundingMode.HALF_EVEN)).toString();
                        if (roundedFloatString.equals(floatString))
                        {
                            throw new RuntimeException("Error appending float string '" + floatString + "' at precision " + decimalPrecision + ": rounding to precision " + roundingPrecision + " failed");
                        }
                        appendFloatString(builder, roundedFloatString, decimalPrecision);
                    }
                    else if (roundingPrecision < 0)
                    {
                        builder.append(floatString, 0, decimalIndex + decimalPrecision + 1);
                    }
                    else
                    {
                        int endIndex = decimalIndex + decimalPrecision + 1;
                        char endChar = floatString.charAt(endIndex);
                        boolean roundUp;
                        if (endChar < '5')
                        {
                            roundUp = false;
                        }
                        else if (endChar > '5')
                        {
                            roundUp = true;
                        }
                        else
                        {
                            String roundedFloatString = new BigDecimal(floatString).round(new MathContext(1, RoundingMode.UP)).toString();
                            roundUp = roundedFloatString.charAt(endIndex) >= '6';
                        }

                        if (roundUp)
                        {
                            builder.append(floatString, 0, endIndex - 1);
                            builder.append('1');
                        }
                        else
                        {
                            builder.append(floatString, 0, endIndex);
                        }
                    }
                }
            }
        }
    }

    private static void appendZeros(StringBuilder builder, int zeros)
    {
        for (; zeros > 0; zeros--)
        {
            builder.append('0');
        }
    }

    private static boolean isSigned(String numberString)
    {
        return isSign(numberString.charAt(0));
    }

    private static boolean isSign(char character)
    {
        return (character == '-') || (character == '+');
    }

    private static int getSignCount(String numberString)
    {
        int i = 0;
        while (isSign(numberString.charAt(i)))
        {
            i++;
        }
        return i;
    }

    private static int getZeroCountFrom(String numberString, int index)
    {
        int i = index;
        while (numberString.charAt(i) == '0')
        {
            i++;
        }
        return i - index;
    }
}
