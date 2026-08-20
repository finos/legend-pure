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

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestPureStrictTime
{
    @Test
    public void testStrictTimeFormat()
    {
        PureStrictTime strictTimeWithHourMin = StrictTimeFunctions.newPureStrictTime(16, 12);
        Assert.assertEquals("16:12", strictTimeWithHourMin.format("HH:mm"));
        Assert.assertEquals("04:12", strictTimeWithHourMin.format("hh:mm"));

        PureStrictTime strictTimeWithHourMinSec = StrictTimeFunctions.newPureStrictTime(16, 12, 35);
        Assert.assertEquals("16:12:35", strictTimeWithHourMinSec.format("HH:mm:ss"));

        PureStrictTime strictTimeWithHourMinSecSubSec = StrictTimeFunctions.newPureStrictTime(16, 12, 35, "070004235");
        Assert.assertEquals("16:12:35", strictTimeWithHourMinSecSubSec.format("H:mm:ss"));
        Assert.assertEquals("16:12:35.070004235", strictTimeWithHourMinSecSubSec.format("HH:mm:ss.SSSS"));
        Assert.assertEquals("16:12:35.070", strictTimeWithHourMinSecSubSec.format("HH:mm:ss.SSS"));
    }

    /**
     * A subsecond is a run of digits that the rest of the class reads back as digits, so it has to
     * be written in the digits it will be read in. Some locales number in something other than
     * 0 to 9, and a subsecond written in those is not merely printed oddly: incrementing one does
     * arithmetic on the characters, so the answer comes back wrong rather than failing.
     */
    @Test
    public void testSubsecondsAreWrittenInASCIIDigitsWhateverTheLocale()
    {
        Locale before = Locale.getDefault();
        try
        {
            for (String tag : new String[]{"en-US", "fa-IR", "hi-IN-u-nu-deva", "ar-EG-u-nu-arab", "bn-IN-u-nu-beng"})
            {
                Locale.setDefault(Locale.forLanguageTag(tag));

                PureStrictTime millis = StrictTimeFunctions.parsePureStrictTime("10:20:30.000");
                Assert.assertEquals(tag, "10:20:30.001", millis.addMilliseconds(1).toString());
                Assert.assertEquals(tag, "10:20:29.999", millis.addMilliseconds(-1).toString());

                PureStrictTime micros = StrictTimeFunctions.parsePureStrictTime("10:20:30.000000");
                Assert.assertEquals(tag, "10:20:30.000001", micros.addMicroseconds(1).toString());

                PureStrictTime nanos = StrictTimeFunctions.parsePureStrictTime("10:20:30.000000000");
                Assert.assertEquals(tag, "10:20:30.000000001", nanos.addNanoseconds(1).toString());
            }
        }
        finally
        {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testInvalidFormat()
    {
        PureStrictTime strictTimeWithHourMin = StrictTimeFunctions.newPureStrictTime(16, 12);
        Assert.assertEquals("16:12", strictTimeWithHourMin.format("HH:mm"));
        try
        {
            strictTimeWithHourMin.format("HH:mm:ss.SSSZ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("StrictTime has no second: 16:12", e.getMessage());
        }
    }

    @Test
    public void testInvalidSubseconds()
    {
        try
        {
            StrictTimeFunctions.newPureStrictTime(10, 26, 33, null);
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: null", e.getMessage());
        }

        try
        {
            StrictTimeFunctions.newPureStrictTime(10, 26, 33, "");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"\"", e.getMessage());
        }

        try
        {
            StrictTimeFunctions.newPureStrictTime(10, 26, 33, "789as9898");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"789as9898\"", e.getMessage());
        }

        try
        {
            StrictTimeFunctions.newPureStrictTime(10, 26, 33, "-789");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"-789\"", e.getMessage());
        }
    }

    @Test
    public void testAddHoursMinutesSecondsMilliseconds()
    {
        PureStrictTime time = StrictTimeFunctions.newPureStrictTime(10, 26, 33, "780013429");
        Assert.assertEquals(StrictTimeFunctions.newPureStrictTime(11, 26, 33, "780013429"), time.addHours(1));
        Assert.assertEquals(StrictTimeFunctions.newPureStrictTime(10, 46, 33, "780013429"), time.addMinutes(20));
        Assert.assertEquals(StrictTimeFunctions.newPureStrictTime(10, 27, 3, "780013429"), time.addSeconds(30));
        Assert.assertSame(time, time.addMilliseconds(0));
        Assert.assertEquals(StrictTimeFunctions.newPureStrictTime(10, 26, 32, "781013429"), time.addMilliseconds(-999));
    }

}
