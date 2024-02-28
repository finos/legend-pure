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
