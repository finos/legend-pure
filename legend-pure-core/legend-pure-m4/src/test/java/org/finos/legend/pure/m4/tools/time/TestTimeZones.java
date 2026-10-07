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

package org.finos.legend.pure.m4.tools.time;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tests for {@link TimeZones}: the names it resolves, the names it rejects, and the calendars it
 * builds from them, along with what its deprecated legacy methods do with the same names.
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
        // Which zone each one names moves between Java versions: EST is the offset -05:00 through
        // Java 21 and America/Panama from Java 25. What holds across versions is the offset.
        assertStandardOffset("-05:00", "EST");
        assertStandardOffset("-07:00", "MST");
        assertStandardOffset("-10:00", "HST");
        assertStandardOffset("-08:00", "PST");
        assertStandardOffset("+05:30", "IST");
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

        // The prefixed forms keep working, whichever prefix is used.
        Assert.assertEquals(ZoneId.of("GMT+10:00"), TimeZones.parse("GMT+10:00"));
        Assert.assertEquals(ZoneId.of("GMT+5"), TimeZones.parse("GMT+5"));
        Assert.assertEquals(ZoneId.of("UTC+10:00"), TimeZones.parse("UTC+10:00"));
        Assert.assertEquals(ZoneId.of("UT-05:00"), TimeZones.parse("UT-05:00"));
    }

    @Test
    public void testParseTimeZone()
    {
        // A name java.util resolves on its own keeps the id it knows it by.
        Assert.assertEquals(TimeZone.getTimeZone("GMT"), TimeZones.parseTimeZone("GMT"));
        Assert.assertEquals(TimeZone.getTimeZone("US/Arizona"), TimeZones.parseTimeZone("US/Arizona"));
        Assert.assertEquals(TimeZone.getTimeZone("America/New_York"), TimeZones.parseTimeZone("America/New_York"));

        // A bare offset arrives as the offset it names, where TimeZone.getTimeZone reads it as GMT.
        assertRawOffset(19800000, "+0530");
        assertRawOffset(36000000, "+1000");
        assertRawOffset(-18000000, "-0500");

        // So does a prefixed offset: TimeZone.getTimeZone reads the UTC and UT forms as GMT before
        // Java 21, where it has always read the GMT form as itself.
        assertRawOffset(36000000, "GMT+10:00");
        assertRawOffset(36000000, "UTC+10:00");
        assertRawOffset(36000000, "UT+10:00");
        assertRawOffset(-18000000, "UTC-05:00");
        assertRawOffset(19800000, "UTC+05:30");
    }

    @Test
    public void testNewCalendar()
    {
        // The calendar is in the zone the name stands for, whichever form the name takes.
        Assert.assertEquals(TimeZone.getTimeZone("GMT"), TimeZones.newCalendar("GMT").getTimeZone());
        Assert.assertEquals(0, rawOffsetMillis(TimeZones.newCalendar("GMT")));
        Assert.assertEquals(-25200000, rawOffsetMillis(TimeZones.newCalendar("US/Arizona")));
        Assert.assertEquals(36000000, rawOffsetMillis(TimeZones.newCalendar("Pacific/Guam")));
        Assert.assertEquals(-18000000, rawOffsetMillis(TimeZones.newCalendar("EST")));
        Assert.assertEquals(19800000, rawOffsetMillis(TimeZones.newCalendar("+0530")));
        Assert.assertEquals(36000000, rawOffsetMillis(TimeZones.newCalendar("UTC+10:00")));
    }

    @Test
    public void testUnresolvableName()
    {
        assertUnresolvable("Unknown time zone: Europe/Lissabon", "Europe/Lissabon");
        assertUnresolvable("Unknown time zone: Lissabon", "Lissabon");
        assertUnresolvable("Unknown time zone: 0530", "0530");
        assertUnresolvable("Unknown time zone: ", "");
        assertUnresolvable("Unknown time zone: null", null);
    }

    /**
     * The legacy methods are {@link TimeZone#getTimeZone(String)}, so they read every name the way
     * it does. Where it knows the name, that is the zone the strict methods find too.
     */
    @Test
    public void testLegacyIsTimeZoneGetTimeZone()
    {
        for (String timeZone : new String[] {"America/New_York", "US/Arizona", "Pacific/Guam", "GMT", "UTC", "EST", "GMT+5", "GMT-10:00"})
        {
            Assert.assertEquals(timeZone, TimeZone.getTimeZone(timeZone), TimeZones.parseTimeZoneLegacy(timeZone));
            Assert.assertEquals(timeZone, TimeZone.getTimeZone(timeZone).toZoneId(), TimeZones.parseLegacy(timeZone));
            Assert.assertEquals(timeZone, TimeZone.getTimeZone(timeZone), TimeZones.newCalendarLegacy(timeZone).getTimeZone());
            Assert.assertEquals(
                    timeZone,
                    TimeZones.parse(timeZone).getRules().getStandardOffset(Instant.EPOCH),
                    TimeZones.parseLegacy(timeZone).getRules().getStandardOffset(Instant.EPOCH));
        }
        Assert.assertEquals(NEW_YORK, TimeZones.parseLegacy("America/New_York"));
    }

    /**
     * A name java.util does not know reads as GMT, and nothing says so: a bare offset, a misspelled
     * region, a region still carrying the quotes the connection grammar puts around it, and nothing
     * at all. The strict methods read the bare offset as the offset it names and reject the rest.
     */
    @Test
    public void testLegacyReadsWhatItDoesNotKnowAsGMT()
    {
        for (String timeZone : new String[] {"+0530", "-0500", "0530", "Europe/Lissabon", "'US/Arizona'", "\"US/Arizona\"", ""})
        {
            Assert.assertEquals(timeZone, ZoneId.of("GMT"), TimeZones.parseLegacy(timeZone));
            Assert.assertEquals(timeZone, "GMT", TimeZones.parseTimeZoneLegacy(timeZone).getID());
            Assert.assertEquals(timeZone, 0, TimeZones.parseTimeZoneLegacy(timeZone).getRawOffset());
            Assert.assertEquals(timeZone, 0, rawOffsetMillis(TimeZones.newCalendarLegacy(timeZone)));
        }
        Assert.assertEquals(ZoneId.of("+05:30"), TimeZones.parse("+0530"));
        Assert.assertEquals(ZoneId.of("-05:00"), TimeZones.parse("-0500"));
        assertUnresolvable("Unknown time zone: 0530", "0530");
        assertUnresolvable("Unknown time zone: Europe/Lissabon", "Europe/Lissabon");
        assertUnresolvable("Unknown time zone: 'US/Arizona'", "'US/Arizona'");
        assertUnresolvable("Unknown time zone: \"US/Arizona\"", "\"US/Arizona\"");
        assertUnresolvable("Unknown time zone: ", "");
    }

    /**
     * No name at all is a caller's mistake rather than a name, so the legacy methods reject it as
     * the strict ones do, where {@link TimeZone#getTimeZone(String)} would throw a
     * NullPointerException.
     */
    @Test
    public void testLegacyRejectsNull()
    {
        assertThrowsWithMessage("Unknown time zone: null", () -> TimeZones.parseLegacy(null));
        assertThrowsWithMessage("Unknown time zone: null", () -> TimeZones.parseTimeZoneLegacy(null));
        assertThrowsWithMessage("Unknown time zone: null", () -> TimeZones.newCalendarLegacy(null));
    }

    private static int rawOffsetMillis(Calendar calendar)
    {
        return calendar.getTimeZone().getRawOffset();
    }

    private static void assertRawOffset(int expectedOffsetMillis, String timeZone)
    {
        Assert.assertEquals(timeZone, expectedOffsetMillis, TimeZones.parseTimeZone(timeZone).getRawOffset());
    }

    private static void assertStandardOffset(String expectedOffset, String timeZone)
    {
        ZoneId zone = TimeZones.parse(timeZone);
        Assert.assertEquals(timeZone, ZoneOffset.of(expectedOffset), zone.getRules().getStandardOffset(Instant.EPOCH));
    }

    /**
     * No method here guesses at a name it cannot resolve; each rejects it, with the same message.
     */
    private static void assertUnresolvable(String expectedMessage, String timeZone)
    {
        assertThrowsWithMessage(expectedMessage, () -> TimeZones.parse(timeZone));
        assertThrowsWithMessage(expectedMessage, () -> TimeZones.parseTimeZone(timeZone));
        assertThrowsWithMessage(expectedMessage, () -> TimeZones.newCalendar(timeZone));
    }

    private static void assertThrowsWithMessage(String expectedMessage, ThrowingRunnable resolve)
    {
        Assert.assertEquals(expectedMessage, Assert.assertThrows(IllegalArgumentException.class, resolve).getMessage());
    }
}
