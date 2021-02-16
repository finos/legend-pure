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

package org.finos.legend.pure.m4.primitives;

import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.junit.Assert;
import org.junit.Test;

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
            StrictTimeFunctions.newPureStrictTime( 10, 26, 33, null);
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
            StrictTimeFunctions.newPureStrictTime( 10, 26, 33, "789as9898");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"789as9898\"", e.getMessage());
        }

        try
        {
            StrictTimeFunctions.newPureStrictTime( 10, 26, 33, "-789");
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
