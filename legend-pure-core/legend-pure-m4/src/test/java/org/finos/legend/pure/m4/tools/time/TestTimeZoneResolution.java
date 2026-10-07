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
import java.util.TimeZone;

/**
 * Tests for {@link TimeZoneResolution}: that the strict resolution is the strict methods of
 * {@link TimeZones}, and that the legacy one reads names as {@link TimeZone#getTimeZone(String)}
 * does, including the names it gets wrong.
 */
public class TestTimeZoneResolution
{
    private static final ZoneId GMT = ZoneId.of("GMT");

    @Test
    public void testStrictIsTheStrictMethodsOfTimeZones()
    {
        for (String name : new String[] {"America/New_York", "US/Arizona", "GMT", "UTC", "EST", "+0530", "GMT+5", "UTC+10:00"})
        {
            Assert.assertEquals(name, TimeZones.parse(name), TimeZoneResolution.STRICT.parse(name));
            Assert.assertEquals(name, TimeZones.parseTimeZone(name), TimeZoneResolution.STRICT.parseTimeZone(name));
            Assert.assertEquals(name, TimeZones.newCalendar(name).getTimeZone(), TimeZoneResolution.STRICT.newCalendar(name).getTimeZone());
        }
        assertThrowsWithMessage("Unknown time zone: Europe/Lissabon", () -> TimeZoneResolution.STRICT.parse("Europe/Lissabon"));
        assertThrowsWithMessage("Unknown time zone: 'US/Arizona'", () -> TimeZoneResolution.STRICT.parse("'US/Arizona'"));
    }

    @Test
    public void testLegacyIsTimeZoneGetTimeZone()
    {
        for (String name : new String[] {"America/New_York", "US/Arizona", "GMT", "UTC", "EST", "GMT+5", "+0530", "Europe/Lissabon", "'US/Arizona'", ""})
        {
            Assert.assertEquals(name, TimeZone.getTimeZone(name), TimeZoneResolution.LEGACY.parseTimeZone(name));
            Assert.assertEquals(name, TimeZone.getTimeZone(name), TimeZoneResolution.LEGACY.newCalendar(name).getTimeZone());
            Assert.assertEquals(name, TimeZone.getTimeZone(name).toZoneId(), TimeZoneResolution.LEGACY.parse(name));
        }
    }

    @Test
    public void testLegacyAgreesWithStrictOnRegionsAndGMTOffsets()
    {
        for (String name : new String[] {"America/New_York", "US/Arizona", "Pacific/Guam", "Asia/Kolkata", "GMT", "UTC"})
        {
            Assert.assertEquals(name, TimeZoneResolution.STRICT.parse(name), TimeZoneResolution.LEGACY.parse(name));
        }
        assertSameStandardOffset("EST");
        assertSameStandardOffset("GMT+5");
        assertSameStandardOffset("GMT+05:30");
        assertSameStandardOffset("GMT-10:00");
    }

    /**
     * The names the legacy resolution reads as GMT, with nothing to say it did: a bare offset, a
     * misspelled region, a region still carrying the quotes the connection grammar puts around it,
     * and nothing at all. Strictly, the offset is the offset it names and the rest are errors.
     */
    @Test
    public void testLegacyReadsWhatItDoesNotKnowAsGMT()
    {
        for (String name : new String[] {"+0530", "-0500", "0530", "Europe/Lissabon", "'US/Arizona'", "\"US/Arizona\"", ""})
        {
            Assert.assertEquals(name, GMT, TimeZoneResolution.LEGACY.parse(name));
            Assert.assertEquals(name, 0, TimeZoneResolution.LEGACY.parseTimeZone(name).getRawOffset());
            Assert.assertEquals(name, 0, TimeZoneResolution.LEGACY.newCalendar(name).getTimeZone().getRawOffset());
        }

        Assert.assertEquals(ZoneOffset.ofHoursMinutes(5, 30), TimeZoneResolution.STRICT.parse("+0530"));
        Assert.assertEquals(ZoneOffset.ofHours(-5), TimeZoneResolution.STRICT.parse("-0500"));
        assertThrowsWithMessage("Unknown time zone: 0530", () -> TimeZoneResolution.STRICT.parse("0530"));
        assertThrowsWithMessage("Unknown time zone: Europe/Lissabon", () -> TimeZoneResolution.STRICT.parse("Europe/Lissabon"));
        assertThrowsWithMessage("Unknown time zone: \"US/Arizona\"", () -> TimeZoneResolution.STRICT.parse("\"US/Arizona\""));
        assertThrowsWithMessage("Unknown time zone: ", () -> TimeZoneResolution.STRICT.parse(""));
    }

    /**
     * No name at all is a caller's mistake rather than a name to resolve, so both resolutions reject
     * it with the same message, where {@link TimeZone#getTimeZone(String)} would throw a
     * NullPointerException.
     */
    @Test
    public void testNullIsRejected()
    {
        for (TimeZoneResolution resolution : TimeZoneResolution.values())
        {
            assertThrowsWithMessage("Unknown time zone: null", () -> resolution.parse(null));
            assertThrowsWithMessage("Unknown time zone: null", () -> resolution.parseTimeZone(null));
            assertThrowsWithMessage("Unknown time zone: null", () -> resolution.newCalendar(null));
        }
    }

    private static void assertSameStandardOffset(String name)
    {
        Assert.assertEquals(
                name,
                TimeZoneResolution.STRICT.parse(name).getRules().getStandardOffset(Instant.EPOCH),
                TimeZoneResolution.LEGACY.parse(name).getRules().getStandardOffset(Instant.EPOCH));
    }

    private static void assertThrowsWithMessage(String expectedMessage, ThrowingRunnable resolve)
    {
        Assert.assertEquals(expectedMessage, Assert.assertThrows(IllegalArgumentException.class, resolve).getMessage());
    }
}
