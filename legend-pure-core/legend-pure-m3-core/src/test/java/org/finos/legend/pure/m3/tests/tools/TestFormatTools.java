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

package org.finos.legend.pure.m3.tests.tools;

import org.finos.legend.pure.m3.tools.FormatTools;
import org.junit.Assert;
import org.junit.Test;

public class TestFormatTools
{
    /**
     * Every specifier the two engines render, and nothing else.
     */
    @Test
    public void testValidateSpecifiers()
    {
        FormatTools.validate("");
        FormatTools.validate("no specifiers at all");
        FormatTools.validate("%s %d %f %r %t");
        FormatTools.validate("%05d %.2f %00d %.0f");
        FormatTools.validate("100%% and %s");
        FormatTools.validate("%s%s%s");

        // the second % of %% cannot open a specifier, so what follows it is text
        FormatTools.validate("%%q");
        FormatTools.validate("%%t{not a pattern}");

        assertInvalid("Invalid format specifier: %q", "%q");
        assertInvalid("Invalid format specifier: %S", "a %S b");
        assertInvalid("Invalid format specifier: % ", "% s");
        assertInvalid("Format string ends with '%': ends with %", "ends with %");
    }

    /**
     * A width is written between the character that opens it and the one that closes it, and there
     * has to be one: the digits are read as a number, and there is no number in none of them.
     */
    @Test
    public void testValidateWidths()
    {
        FormatTools.validate("%0100d");
        FormatTools.validate("%.11f");

        assertInvalid("Invalid format specifier: %0d", "%0d");
        assertInvalid("Invalid format specifier: %.f", "%.f");
        assertInvalid("Invalid format specifier: %05x", "%05x");
        assertInvalid("Invalid format specifier: %.2d", "%.2d");
        assertInvalid("Invalid format specifier: %05", "%05");
        assertInvalid("Invalid format specifier: %.", "%.");
    }

    /**
     * A date pattern is checked where one is written, and every pattern in the string is.
     */
    @Test
    public void testValidateDatePatterns()
    {
        FormatTools.validate("%t");
        FormatTools.validate("%t and %t");
        FormatTools.validate("on %t{yyyy-MM-dd} at %t{HH:mm:ss?[.S3]}");
        FormatTools.validate("%t{[America/New_York]yyyy-MM-dd\"T\"HH:mm:ssX}");
        FormatTools.validate("%t{S(3!,9,\"_\")}");

        assertInvalid("Invalid format control character 'Q' in format string: yyyy-Q", "%t{yyyy-Q}");
        assertInvalid("Invalid format control character 'Q' in format string: yyyy-Q", "%t{yyyy-MM-dd} %t{yyyy-Q}");
        assertInvalid("Unknown time zone: Europe/Lissabon", "%t{[Europe/Lissabon]yyyy}");
        assertInvalid("Sub-second minimum 5 exceeds maximum 3 in format string: S(5,3)", "%t{S(5,3)}");
        assertInvalid("Empty alternative in optional section in format string: ?[|HH]", "%t{?[|HH]}");
        assertInvalid("Could not find end of date format starting at index 2 of: %t{yyyy-MM-dd", "%t{yyyy-MM-dd");
    }

    /**
     * How many arguments a format string wants, and of what kind, is not this check's business: the
     * arguments are often computed, so a format string has to be free to be right for arguments
     * this cannot see.
     */
    @Test
    public void testValidateSaysNothingAboutArguments()
    {
        FormatTools.validate("%s %s %s %s");
        FormatTools.validate("no arguments wanted");
        FormatTools.validate("%d");
    }

    private static void assertInvalid(String expectedMessage, String formatString)
    {
        Assert.assertEquals(
                formatString,
                expectedMessage,
                Assert.assertThrows(formatString, IllegalArgumentException.class, () -> FormatTools.validate(formatString)).getMessage());
    }

    @Test
    public void testIntegerZeroPadding()
    {
        assertIntegerString("3", "3", 0);
        assertIntegerString("-3", "-3", 0);

        assertIntegerString("00003", "3", 5);
        assertIntegerString("-00003", "-3", 5);

        assertIntegerString("00300", "300", 5);
        assertIntegerString("-00300", "-300", 5);

        assertIntegerString("30000", "30000", 5);
        assertIntegerString("-30000", "-30000", 5);

        assertIntegerString("300000", "300000", 5);
        assertIntegerString("-300000", "-300000", 5);
    }

    @Test
    public void testFloat_noDecimalPrecision()
    {
        assertFloatString("1.0", "1.0", -1);
        assertFloatString("-1.0", "-1.0", -1);

        assertFloatString("1.3", "1.3", -1);
        assertFloatString("-1.3", "-1.3", -1);

        assertFloatString("1.338", "1.338", -1);
        assertFloatString("-1.338", "-1.338", -1);

        assertFloatString("1.47", "1.47", -1);
        assertFloatString("-1.47", "-1.47", -1);

        assertFloatString("1.5", "1.5", -1);
        assertFloatString("-1.5", "-1.5", -1);

        assertFloatString("1.7", "1.7", -1);
        assertFloatString("-1.7", "-1.7", -1);

        assertFloatString("1234567.000002223456", "1234567.000002223456", -1);
        assertFloatString("-1234567.000002223456", "-1234567.000002223456", -1);
    }

    @Test
    public void testFloatRounding_decimalPrecision0()
    {
        assertFloatString("1", "1.0", 0);
        assertFloatString("-1", "-1.0", 0);

        assertFloatString("1", "1.3", 0);
        assertFloatString("-1", "-1.3", 0);

        assertFloatString("1", "1.47", 0);
        assertFloatString("-1", "-1.47", 0);

        assertFloatString("2", "1.5", 0);
        assertFloatString("-2", "-1.5", 0);

        assertFloatString("2", "1.7", 0);
        assertFloatString("-2", "-1.7", 0);
    }

    @Test
    public void testFloatRounding_decimalPrecision2()
    {
        assertFloatString("1.0", "1.0", 1);
        assertFloatString("-1.0", "-1.0", 1);

        assertFloatString("1.34", "1.338", 2);
        assertFloatString("-1.34", "-1.338", 2);

        assertFloatString("10.00", "9.999", 2);
        assertFloatString("-10.00", "-9.999", 2);

        assertFloatString("1.61", "1.613", 2);
        assertFloatString("-1.61", "-1.613", 2);

        assertFloatString("0.61", "0.613", 2);
        assertFloatString("-0.61", "-0.613", 2);

        assertFloatString("0.06", "0.0553", 2);
        assertFloatString("-0.06", "-0.0553", 2);

        assertFloatString("0.01", "0.00553", 2);
        assertFloatString("-0.01", "-0.00553", 2);

        assertFloatString("0.00", "0.000553", 2);
        assertFloatString("-0.00", "-0.000553", 2);
    }

    @Test
    public void testFloatRounding_decimalPrecision11()
    {
        assertFloatString("1234567.00000222346", "1234567.000002223456", 11);
        assertFloatString("-1234567.00000222346", "-1234567.000002223456", 11);

        assertFloatString("0.00000222346", "0.000002223456", 11);
        assertFloatString("-0.00000222346", "-0.000002223456", 11);
    }

    private void assertIntegerString(String expected, String integerString, int zeroPadding)
    {
        StringBuilder builder = new StringBuilder(integerString.length());
        FormatTools.appendIntegerString(builder, integerString, zeroPadding);
        Assert.assertEquals(String.format("Failure writing \"%s\" with zero padding %d", integerString, zeroPadding), expected, builder.toString());
    }

    private void assertFloatString(String expected, String floatString, int decimalPrecision)
    {
        StringBuilder builder = new StringBuilder(floatString.length());
        FormatTools.appendFloatString(builder, floatString, decimalPrecision);
        Assert.assertEquals(String.format("Failure writing \"%s\" with decimal precision %d", floatString, decimalPrecision), expected, builder.toString());
    }
}
