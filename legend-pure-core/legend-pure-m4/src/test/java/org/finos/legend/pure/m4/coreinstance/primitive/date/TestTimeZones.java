// Copyright 2026 Goldman Sachs
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

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Calendar;

/**
 * Tests for {@link TimeZones}: the names it resolves, the names it rejects, and the calendars it
 * builds from them.
 */
public class TestTimeZones
{
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    public void testParseRegion()
    {
        Assert.assertEquals(NEW_YORK, TimeZones.parse("America/New_York"));
        Assert.assertEquals(ZoneId.of("US/Arizona"), TimeZones.parse("US/Arizona"));
        Assert.assertEquals(ZoneId.of("Pacific/Guam"), TimeZones.parse("Pacific/Guam"));
        Assert.assertEquals(ZoneId.of("UTC"), TimeZones.parse("UTC"));
        Assert.assertEquals(ZoneId.of("GMT"), TimeZones.parse("GMT"));
    }

    @Test
    public void testParseShortId()
    {
        // These are the abbreviations ZoneId.SHORT_IDS carries; ZoneId.of rejects them on its own.
        Assert.assertEquals(ZoneId.of("-05:00"), TimeZones.parse("EST"));
        Assert.assertEquals(ZoneId.of("-07:00"), TimeZones.parse("MST"));
        Assert.assertEquals(ZoneId.of("-10:00"), TimeZones.parse("HST"));
        Assert.assertEquals(ZoneId.of("America/Los_Angeles"), TimeZones.parse("PST"));
        Assert.assertEquals(ZoneId.of("Asia/Kolkata"), TimeZones.parse("IST"));
    }

    @Test
    public void testParseOffset()
    {
        // A bare offset is what TimeZone.getTimeZone silently reads as GMT.
        Assert.assertEquals(ZoneId.of("+05:30"), TimeZones.parse("+0530"));
        Assert.assertEquals(ZoneId.of("+05:30"), TimeZones.parse("+05:30"));
        Assert.assertEquals(ZoneId.of("-05:00"), TimeZones.parse("-0500"));
        Assert.assertEquals(ZoneId.of("+10:00"), TimeZones.parse("+1000"));
        Assert.assertEquals(ZoneId.of("Z"), TimeZones.parse("Z"));

        // The GMT prefixed forms keep working.
        Assert.assertEquals(ZoneId.of("GMT+10:00"), TimeZones.parse("GMT+10:00"));
        Assert.assertEquals(ZoneId.of("GMT+5"), TimeZones.parse("GMT+5"));
    }

    @Test
    public void testParseUnknown()
    {
        assertParseFails("Unknown time zone: Europe/Lissabon", "Europe/Lissabon");
        assertParseFails("Unknown time zone: Lissabon", "Lissabon");
        assertParseFails("Unknown time zone: 0530", "0530");
        assertParseFails("Unknown time zone: ", "");
        assertParseFails("Unknown time zone: null", null);
    }

    @Test
    public void testParseWithFallBack()
    {
        Assert.assertEquals(NEW_YORK, TimeZones.parse("America/New_York", TimeZones.GMT));
        Assert.assertEquals(ZoneId.of("+10:00"), TimeZones.parse("+1000", TimeZones.GMT));
        Assert.assertEquals(ZoneId.of("-05:00"), TimeZones.parse("EST", TimeZones.GMT));

        // Where the rejecting form throws, this one falls back.
        Assert.assertEquals(TimeZones.GMT, TimeZones.parse("Europe/Lissabon", TimeZones.GMT));
        Assert.assertEquals(TimeZones.GMT, TimeZones.parse("", TimeZones.GMT));
        Assert.assertEquals(TimeZones.GMT, TimeZones.parse(null, TimeZones.GMT));
        Assert.assertEquals(NEW_YORK, TimeZones.parse("Europe/Lissabon", NEW_YORK));
        Assert.assertNull(TimeZones.parse("Europe/Lissabon", null));
    }

    @Test
    public void testNewCalendar()
    {
        Assert.assertEquals(0, rawOffsetMillis(TimeZones.newCalendar("GMT")));
        Assert.assertEquals(-25200000, rawOffsetMillis(TimeZones.newCalendar("US/Arizona")));
        Assert.assertEquals(36000000, rawOffsetMillis(TimeZones.newCalendar("Pacific/Guam")));
        Assert.assertEquals(-18000000, rawOffsetMillis(TimeZones.newCalendar("EST")));

        // An offset reaches the calendar as itself, not as GMT.
        Assert.assertEquals(19800000, rawOffsetMillis(TimeZones.newCalendar("+0530")));
        Assert.assertEquals(36000000, rawOffsetMillis(TimeZones.newCalendar("+1000")));
        Assert.assertEquals(-18000000, rawOffsetMillis(TimeZones.newCalendar("-0500")));

        assertNewCalendarFails("Unknown time zone: Europe/Lissabon", "Europe/Lissabon");
        assertNewCalendarFails("Unknown time zone: null", null);
    }

    @Test
    public void testNewCalendarWithFallBack()
    {
        Assert.assertEquals(19800000, rawOffsetMillis(TimeZones.newCalendar("+0530", TimeZones.GMT)));
        Assert.assertEquals(0, rawOffsetMillis(TimeZones.newCalendar("Europe/Lissabon", TimeZones.GMT)));
        Assert.assertEquals(0, rawOffsetMillis(TimeZones.newCalendar(null, TimeZones.GMT)));
        Assert.assertEquals(-18000000, rawOffsetMillis(TimeZones.newCalendar("Europe/Lissabon", ZoneId.of("EST", ZoneId.SHORT_IDS))));
    }

    private static int rawOffsetMillis(Calendar calendar)
    {
        return calendar.getTimeZone().getRawOffset();
    }

    private static void assertParseFails(String expectedMessage, String timeZone)
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> TimeZones.parse(timeZone));
        Assert.assertEquals(expectedMessage, e.getMessage());
    }

    private static void assertNewCalendarFails(String expectedMessage, String timeZone)
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> TimeZones.newCalendar(timeZone));
        Assert.assertEquals(expectedMessage, e.getMessage());
    }
}
