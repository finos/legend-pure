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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class FormatTools
{
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
