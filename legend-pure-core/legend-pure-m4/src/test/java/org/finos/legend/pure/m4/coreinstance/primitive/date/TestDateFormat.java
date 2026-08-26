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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tests for {@link DateFormat}: rendering a Pure date through a format string, writing the
 * canonical Pure date string, and parsing one back.
 *
 * <p>{@link TestPureDate} covers the rendering through {@link PureDate#format(String)} and
 * {@link PureDate#toString()}, which delegate here. These tests go at {@link DateFormat} directly,
 * so they can also reach the parse methods and the substring overload nothing else exercises.
 */
public class TestDateFormat
{
    private static final PureDate DATE = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");

    /**
     * The same date at every granularity, each rendered as the canonical Pure date string.
     */
    private static final PureDate[] DATES = {
            DateFunctions.newPureDate(2014),
            DateFunctions.newPureDate(2014, 3),
            DateFunctions.newPureDate(2014, 3, 10),
            DateFunctions.newPureDate(2014, 3, 10, 16),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35),
            DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235")
    };

    private static final String[] CANONICAL_STRINGS = {
            "2014",
            "2014-03",
            "2014-03-10",
            "2014-03-10T16",
            "2014-03-10T16:12+0000",
            "2014-03-10T16:12:35+0000",
            "2014-03-10T16:12:35.070004235+0000"
    };

    // Format: date components

    /**
     * The year is written in full only from four letters on, and the full form is not padded, so a
     * year of fewer than four digits is shorter than the format string suggests.
     */
    @Test
    public void testFormatYear()
    {
        Assert.assertEquals("14", format("y", DATE));
        Assert.assertEquals("14", format("yy", DATE));
        Assert.assertEquals("14", format("yyy", DATE));
        Assert.assertEquals("2014", format("yyyy", DATE));
        Assert.assertEquals("2014", format("yyyyy", DATE));

        Assert.assertEquals("00", format("yy", DateFunctions.newPureDate(2000, 3, 10)));
        Assert.assertEquals("05", format("yy", DateFunctions.newPureDate(5, 3, 10)));
        Assert.assertEquals("5", format("yyyy", DateFunctions.newPureDate(5, 3, 10)));
        Assert.assertEquals("23", format("yy", DateFunctions.newPureDate(123, 3, 10)));
        Assert.assertEquals("123", format("yyyy", DateFunctions.newPureDate(123, 3, 10)));
        Assert.assertEquals("00", format("yy", DateFunctions.newPureDate(0, 3, 10)));
        Assert.assertEquals("0", format("yyyy", DateFunctions.newPureDate(0, 3, 10)));
        Assert.assertEquals("-44", format("yyyy", DateFunctions.newPureDate(-44, 3, 10)));
    }

    /**
     * A year before the era has last two digits like any other, and the two digit form drops the
     * sign along with the century.
     */
    @Test
    public void testFormatTwoDigitYearBeforeTheEra()
    {
        Assert.assertEquals("44", format("yy", DateFunctions.newPureDate(-44, 3, 10)));
        Assert.assertEquals("01", format("yy", DateFunctions.newPureDate(-1, 3, 10)));
        Assert.assertEquals("14", format("yy", DateFunctions.newPureDate(-2014, 3, 10)));
        Assert.assertEquals("00", format("yy", DateFunctions.newPureDate(-100, 3, 10)));

        // the full form keeps the sign
        Assert.assertEquals("-1", format("yyyy", DateFunctions.newPureDate(-1, 3, 10)));
        Assert.assertEquals("-2014", format("yyyy", DateFunctions.newPureDate(-2014, 3, 10)));
    }

    /**
     * The month, day, hour, minute, and second are padded to one more digit than the format string
     * repeats the letter, so a single letter gives no padding at all.
     */
    @Test
    public void testFormatZeroPaddedComponents()
    {
        Assert.assertEquals("3", format("M", DATE));
        Assert.assertEquals("03", format("MM", DATE));
        Assert.assertEquals("003", format("MMM", DATE));

        Assert.assertEquals("10", format("d", DATE));
        Assert.assertEquals("10", format("dd", DATE));
        Assert.assertEquals("010", format("ddd", DATE));

        Assert.assertEquals("12", format("m", DATE));
        Assert.assertEquals("12", format("mm", DATE));
        Assert.assertEquals("012", format("mmm", DATE));

        Assert.assertEquals("35", format("s", DATE));
        Assert.assertEquals("35", format("ss", DATE));
        Assert.assertEquals("035", format("sss", DATE));

        // a single digit component is padded only as far as it is asked to be
        PureDate single = DateFunctions.newPureDate(2014, 3, 5, 6, 7, 8);
        Assert.assertEquals("3", format("M", single));
        Assert.assertEquals("03", format("MM", single));
        Assert.assertEquals("5", format("d", single));
        Assert.assertEquals("05", format("dd", single));
        Assert.assertEquals("7", format("m", single));
        Assert.assertEquals("07", format("mm", single));
        Assert.assertEquals("8", format("s", single));
        Assert.assertEquals("08", format("ss", single));
    }

    /**
     * The hour is written either on the 24 hour clock, or on the 12 hour clock with midnight and
     * noon both written as 12.
     */
    @Test
    public void testFormatHourAndAmPm()
    {
        int[] hours = {0, 1, 11, 12, 13, 23};
        String[] onTwelveHourClock = {"12", "1", "11", "12", "1", "11"};
        String[] onTwelveHourClockPadded = {"12", "01", "11", "12", "01", "11"};
        String[] onTwentyFourHourClock = {"0", "1", "11", "12", "13", "23"};
        String[] amPm = {"AM", "AM", "AM", "PM", "PM", "PM"};
        for (int i = 0; i < hours.length; i++)
        {
            PureDate date = DateFunctions.newPureDate(2014, 3, 10, hours[i], 12);
            Assert.assertEquals(date.toString(), onTwelveHourClock[i], format("h", date));
            Assert.assertEquals(date.toString(), onTwelveHourClockPadded[i], format("hh", date));
            Assert.assertEquals(date.toString(), onTwentyFourHourClock[i], format("H", date));
            Assert.assertEquals(date.toString(), amPm[i], format("a", date));
        }
    }

    /**
     * Every control character consumes the run of itself that follows it, whether or not the count
     * means anything to it: a field with no width to widen, and the three time zone notations,
     * write the same thing however many times the letter is repeated.
     */
    @Test
    public void testFormatRepeatingAControlCharacterWithNoWidth()
    {
        Assert.assertEquals("PM", format("a", DATE));
        Assert.assertEquals("PM", format("aa", DATE));
        Assert.assertEquals("PM", format("aaaa", DATE));
        Assert.assertEquals("16 PM", format("HH aaa", DATE));

        Assert.assertEquals("GMT", format("z", DATE));
        Assert.assertEquals("GMT", format("zzzz", DATE));
        Assert.assertEquals("+0000", format("Z", DATE));
        Assert.assertEquals("+0000", format("ZZZZ", DATE));
        Assert.assertEquals("Z", format("X", DATE));
        Assert.assertEquals("Z", format("XXXX", DATE));
    }

    /**
     * The subsecond is cut to one more digit than the format string repeats the letter, but from
     * four letters on it is written in full however many digits it has. It is never padded, so a
     * date with fewer digits than were asked for gives all it has and no more.
     */
    @Test
    public void testFormatSubsecond()
    {
        Assert.assertEquals("0", format("S", DATE));
        Assert.assertEquals("07", format("SS", DATE));
        Assert.assertEquals("070", format("SSS", DATE));
        Assert.assertEquals("070004235", format("SSSS", DATE));
        Assert.assertEquals("070004235", format("SSSSSSSSSS", DATE));

        PureDate hundredths = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07");
        Assert.assertEquals("0", format("S", hundredths));
        Assert.assertEquals("07", format("SS", hundredths));
        Assert.assertEquals("07", format("SSS", hundredths));
        Assert.assertEquals("07", format("SSSS", hundredths));
    }

    /**
     * A sub-second field takes a width, which is the one place the format language says more than a
     * run of letters can. The run of letters is two of these widths: one to three letters is
     * {@code S<N}, and four or more is {@code S*}.
     */
    @Test
    public void testFormatSubsecondWidths()
    {
        PureDate hundredths = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07");

        Assert.assertEquals("070", format("S3", DATE));
        Assert.assertEquals("070", format("S3", hundredths));
        Assert.assertEquals("070004235", format("S9", DATE));
        Assert.assertEquals("070000000", format("S9", hundredths));

        Assert.assertEquals("070", format("S<3", DATE));
        Assert.assertEquals("07", format("S<3", hundredths));
        Assert.assertEquals("070004235", format("S>3", DATE));
        Assert.assertEquals("070", format("S>3", hundredths));
        Assert.assertEquals("070004235", format("S*", DATE));
        Assert.assertEquals("07", format("S*", hundredths));

        Assert.assertEquals("2014-03-10 16:12:35.070004", format("yyyy-MM-dd HH:mm:ss.S<6", DATE));
        Assert.assertEquals("2014-03-10 16:12:35.070000", format("yyyy-MM-dd HH:mm:ss.S6", hundredths));
        Assert.assertEquals("2014-03-10T16:12:35.070004235", format("yyyy-MM-dd\"T\"HH:mm:ss.S9", DATE));

        // and refusing to pad is a field of its own, since padding claims a precision the date lacks
        Assert.assertEquals("070", format("S!3", DATE));
        assertFormatFails(
                "Date has a 2 digit sub-second, but 3 are required: 2014-03-10T16:12:35.07+0000",
                "S!3",
                hundredths);
        assertFormatFails(
                "Date has a 9 digit sub-second, but at most 3 may be written: 2014-03-10T16:12:35.070004235+0000",
                "S(0,3!)",
                DATE);

        // every form fails on a date with no fraction, whatever width it asks for
        for (String formatString : new String[]{"S", "SSSS", "S3", "S<3", "S>3", "S*", "S!3", "S(3!,9!,\"_\")"})
        {
            assertFormatFails(
                    "Date has no sub-second: 2014-03-10T16:12:35+0000",
                    formatString,
                    DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35));
        }
    }

    // Format: literals

    @Test
    public void testFormatSeparatorsAndLiteralText()
    {
        Assert.assertEquals("2014-03-10", format("yyyy-MM-dd", DATE));
        Assert.assertEquals("2014/03/10", format("yyyy/MM/dd", DATE));
        Assert.assertEquals("2014.03.10", format("yyyy.MM.dd", DATE));
        Assert.assertEquals("16:12:35", format("HH:mm:ss", DATE));
        Assert.assertEquals("2014 03 10", format("yyyy MM dd", DATE));
        Assert.assertEquals("2014\t03", format("yyyy\tMM", DATE));
        Assert.assertEquals("", format("", DATE));

        // quoted text is written out as it stands, and a backslash escapes a quote within it
        Assert.assertEquals("2014T03", format("yyyy\"T\"MM", DATE));
        Assert.assertEquals("at 16", format("\"at\" HH", DATE));
        Assert.assertEquals("a\"b 16", format("\"a\\\"b\" HH", DATE));
        Assert.assertEquals("yyyy", format("\"yyyy\"", DATE));
    }

    // Format: time zones

    /**
     * Without a time zone the date is written as what it is, a time in UTC, in each of the three
     * time zone notations.
     */
    @Test
    public void testFormatTimeZoneOfADateWithNoTimeZoneGiven()
    {
        Assert.assertEquals("GMT", format("z", DATE));
        Assert.assertEquals("GMT", format("zzzz", DATE));
        Assert.assertEquals("+0000", format("Z", DATE));
        Assert.assertEquals("Z", format("X", DATE));
        Assert.assertEquals("2014-03-10 16:12:35.070+0000", format("yyyy-MM-dd HH:mm:ss.SSSZ", DATE));
    }

    /**
     * A time zone at the head of the format string shifts the date into that zone. The zone name
     * may be quoted, and a backslash escapes a character within it; whichever way it is written, it
     * is the text as given that a general time zone renders.
     */
    @Test
    public void testFormatWithTimeZone()
    {
        Assert.assertEquals("2014-03-10 11:12:35.070-0500", format("[EST]yyyy-MM-dd HH:mm:ss.SSSZ", DATE));
        Assert.assertEquals("2014-03-10 17:12:35.070+0100", format("[CET]yyyy-MM-dd HH:mm:ss.SSSZ", DATE));
        Assert.assertEquals("2014-03-10 17:12:35.070+0100", format("[\"CET\"]yyyy-MM-dd HH:mm:ss.SSSZ", DATE));
        Assert.assertEquals("2014-03-10 17:12:35.070+0100", format("[CE\\T]yyyy-MM-dd HH:mm:ss.SSSZ", DATE));

        Assert.assertEquals("CET", format("[CET]z", DATE));
        Assert.assertEquals("CET", format("[CE\\T]z", DATE));

        // an unknown zone results in an error
        assertFormatFails("Unknown time zone: Europe/Lissabon", "[Europe/Lissabon]yyyy-MM-dd HH:mm:ss.SSSZ", DATE);
        assertFormatFails("Unknown time zone: Europe/Lissabon", "[Europe/Lissabon]z", DATE);

        // a date with no hour is never shifted
        PureDate day = DateFunctions.newPureDate(2015, 8, 15);
        Assert.assertEquals("2015-08-15", format("[EST]yyyy-MM-dd", day));
        Assert.assertEquals("EST", format("[EST]z", day));

        // date is shifted even if zone is not printed
        Assert.assertEquals("2014-03-10 21:42:35.070004235", format("[+0530]yyyy-MM-dd HH:mm:ss.SSSSSS", DATE));
        Assert.assertEquals("2014-03-10 21:42:35", format("[+0530]yyyy-MM-dd HH:mm:ss", DATE));
        Assert.assertEquals("2014-03-10 21:42:35.070004235", format("[Asia/Kolkata]yyyy-MM-dd HH:mm:ss.SSSSSS", DATE));
        Assert.assertEquals("2014-03-10 21:42:35", format("[Asia/Kolkata]yyyy-MM-dd HH:mm:ss", DATE));
    }

    /**
     * A Pure date is a proleptic Gregorian date, which is the calendar it is built on, and asking
     * for a time zone does not change the calendar it is read in. The Gregorian calendar was not
     * adopted until 1582, so a date before that is nine days from what the same date would be on
     * the Julian calendar in use at the time, but it is the Pure date that is being written out.
     */
    @Test
    public void testFormatBeforeTheGregorianCalendarWasAdopted()
    {
        PureDate date = DateFunctions.parsePureDate("1500-01-15T12:00:00");
        Assert.assertEquals("1500-01-15 12:00:00 +0000", format("yyyy-MM-dd HH:mm:ss Z", date));
        Assert.assertEquals("1500-01-15 12:00:00 +0000", format("[GMT]yyyy-MM-dd HH:mm:ss Z", date));
        Assert.assertEquals("1500-01-15 17:00:00 +0500", format("[GMT+5]yyyy-MM-dd HH:mm:ss Z", date));
    }

    /**
     * Before standard time a zone was whatever its own noon made it, which was rarely a whole
     * number of hours from UTC: New York was 4 hours 56 minutes behind it until 1883.
     */
    @Test
    public void testFormatBeforeStandardTime()
    {
        PureDate date = DateFunctions.parsePureDate("1800-01-15T12:00:00");
        Assert.assertEquals("1800-01-15 07:03:58 -0456", format("[America/New_York]yyyy-MM-dd HH:mm:ss Z", date));
        Assert.assertEquals("1800-01-15 11:58:45 -0001", format("[Europe/London]yyyy-MM-dd HH:mm:ss Z", date));
    }

    /**
     * The two numeric time zone notations differ in how they write UTC and in what they leave out:
     * RFC 822 always writes a sign, two digits of hours and two of minutes, while ISO 8601 writes
     * UTC as Z and omits the minutes of an offset that has none. Neither may drop minutes an offset
     * does have, and plenty of zones have them: India is five and a half hours ahead of UTC, not
     * five.
     */
    @Test
    public void testFormatNumericTimeZones()
    {
        Assert.assertEquals("+0000", format("Z", DATE));
        Assert.assertEquals("Z", format("X", DATE));
        Assert.assertEquals("+0000", format("[GMT]Z", DATE));
        Assert.assertEquals("Z", format("[GMT]X", DATE));

        // a whole number of hours
        Assert.assertEquals("-0500", format("[EST]Z", DATE));
        Assert.assertEquals("-05", format("[EST]X", DATE));
        Assert.assertEquals("+0100", format("[CET]Z", DATE));
        Assert.assertEquals("+01", format("[CET]X", DATE));

        // and half or a quarter of one
        Assert.assertEquals("+0530", format("[Asia/Kolkata]Z", DATE));
        Assert.assertEquals("+0530", format("[Asia/Kolkata]X", DATE));
        Assert.assertEquals("+0545", format("[Asia/Kathmandu]Z", DATE));
        Assert.assertEquals("+0545", format("[Asia/Kathmandu]X", DATE));
        Assert.assertEquals("-0230", format("[America/St_Johns]Z", DATE));
        Assert.assertEquals("-0230", format("[America/St_Johns]X", DATE));
        Assert.assertEquals("+1345", format("[Pacific/Chatham]Z", DATE));
        Assert.assertEquals("+1345", format("[Pacific/Chatham]X", DATE));
    }

    /**
     * A zone may be named by an offset rather than a place, in any of the forms
     * {@link ZoneId#of(String, java.util.Map)} takes, and the offset is then fixed rather than
     * something the zone varies over the year. Note that the Etc zones invert the sign, as POSIX
     * has them count hours west of Greenwich: Etc/GMT+5 is five hours behind UTC, not ahead.
     */
    @Test
    public void testFormatWithATimeZoneNamedByItsOffset()
    {
        Assert.assertEquals("2014-03-10 21:12:35 +0500", format("[GMT+5]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 21:12:35 +0500", format("[GMT+05:00]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 19:12:35 +0300", format("[UTC+3]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 21:42:35 +0530", format("[+05:30]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 21:42:35 +0530", format("[+0530]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 11:12:35 -0500", format("[Etc/GMT+5]yyyy-MM-dd HH:mm:ss Z", DATE));

        // UTC by any of its names leaves the date where it is
        Assert.assertEquals("2014-03-10 16:12:35 +0000", format("[UTC]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 16:12:35 +0000", format("[UT]yyyy-MM-dd HH:mm:ss Z", DATE));
        Assert.assertEquals("2014-03-10 16:12:35 +0000", format("[Z]yyyy-MM-dd HH:mm:ss Z", DATE));
    }

    /**
     * A date with no hour is not an instant, so no offset from UTC belongs to it: a zone can be
     * keeping several across a date that broad, and which one it is keeping is just what knowing the
     * time would have told us. The numeric notations therefore fail on such a date, exactly as
     * asking for the hour itself does, whether or not a zone was named. The general notation still
     * works, because it names a zone rather than measuring one.
     */
    @Test
    public void testFormatOffsetOfADateWithNoHour()
    {
        PureDate day = DateFunctions.newPureDate(2015, 8, 15);
        PureDate month = DateFunctions.newPureDate(2015, 8);
        PureDate year = DateFunctions.newPureDate(2015);

        assertFormatFails("Date has no hour (required for Z): 2015-08-15", "[America/New_York]Z", day);
        assertFormatFails("Date has no hour (required for X): 2015-08-15", "[America/New_York]X", day);
        assertFormatFails("Date has no hour (required for Z): 2015-08", "[America/New_York]Z", month);
        assertFormatFails("Date has no hour (required for Z): 2015", "[America/New_York]Z", year);

        // a zone that never moves is no different: the date still does not say when
        assertFormatFails("Date has no hour (required for Z): 2015-08-15", "[Asia/Kolkata]Z", day);
        assertFormatFails("Date has no hour (required for Z): 2015-08-15", "[GMT]Z", day);
        assertFormatFails("Date has no hour (required for X): 2015-08-15", "[+05:30]X", day);

        // nor is naming no zone at all, since the offset is still not a thing the date carries
        assertFormatFails("Date has no hour (required for Z): 2015-08-15", "Z", day);
        assertFormatFails("Date has no hour (required for X): 2015-08-15", "X", day);
        assertFormatFails("Date has no hour (required for Z): 2015-08-15", "yyyy-MM-dd Z", day);

        // the general notation asks only what the format string said, so it still answers
        Assert.assertEquals("2015-08-15 America/New_York", format("[America/New_York]yyyy-MM-dd z", day));
        Assert.assertEquals("2015-08 America/New_York", format("[America/New_York]yyyy-MM z", month));
        Assert.assertEquals("2015 America/New_York", format("[America/New_York]yyyy z", year));
        Assert.assertEquals("2015-08-15 GMT", format("yyyy-MM-dd z", day));

        // and the date itself is not shifted by naming a zone
        Assert.assertEquals("2015-08-15", format("[Pacific/Auckland]yyyy-MM-dd", day));
        Assert.assertEquals("2015-08-15", format("[Pacific/Honolulu]yyyy-MM-dd", day));
    }

    /**
     * Once a date has an hour it stands for an instant, and every zone the format string can name
     * renders it the same way: the general notation gives back the name as written, and the two
     * numeric notations agree with each other and with the offset the zone was keeping then.
     */
    @Test
    public void testFormatTimeZoneIsConsistentAcrossZones()
    {
        MutableList<String> zoneIds = Lists.mutable.with("GMT", "UTC", "CET", "America/New_York", "Asia/Kolkata", "Australia/Lord_Howe", "GMT+5", "UTC+3", "+05:30", "Etc/GMT+5");
        zoneIds.addAllIterable(ZoneId.SHORT_IDS.keySet());
        for (String zoneId : zoneIds)
        {
            ZoneId zone = ZoneId.of(zoneId, ZoneId.SHORT_IDS);
            for (PureDate date : DATES)
            {
                String context = zoneId + " " + date;

                // the general notation is the name as written, whatever the date carries
                Assert.assertEquals(context, zoneId, format("[" + zoneId + "]z", date));

                if (!date.hasHour())
                {
                    assertFormatFails("Date has no hour (required for Z): " + date, "[" + zoneId + "]Z", date);
                    assertFormatFails("Date has no hour (required for X): " + date, "[" + zoneId + "]X", date);
                    continue;
                }

                // the numeric notations agree with each other, and with the offset java.time gives
                // for the instant the date stands for
                int offsetSeconds = PureDateToJava.start().toInstant(date).atZone(zone).getOffset().getTotalSeconds();
                int minutes = Math.abs(offsetSeconds / 60);
                String sign = (offsetSeconds < 0) ? "-" : "+";
                String rfc822 = String.format("%s%02d%02d", sign, minutes / 60, minutes % 60);
                String iso8601 = (offsetSeconds == 0) ? "Z" :
                        ((minutes % 60 == 0) ? String.format("%s%02d", sign, minutes / 60) : rfc822);

                Assert.assertEquals(context, rfc822, format("[" + zoneId + "]Z", date));
                Assert.assertEquals(context, iso8601, format("[" + zoneId + "]X", date));
            }
        }
    }

    /**
     * A zone need not be a whole number of minutes from UTC: Liberia was 44 minutes and 30 seconds
     * behind it until 1972. The second is part of the shift there, so it comes from the shifted
     * time along with every other component, and neither notation can express it, so both round
     * the offset towards UTC.
     */
    @Test
    public void testFormatWithATimeZoneThatIsNotAWholeNumberOfMinutesFromUTC()
    {
        PureDate date = DateFunctions.parsePureDate("1960-01-01T12:00:30");
        Assert.assertEquals("1960-01-01 11:16:00 -0044", format("[Africa/Monrovia]yyyy-MM-dd HH:mm:ss Z", date));
        Assert.assertEquals("1960-01-01 11:16:00 -0044", format("[Africa/Monrovia]yyyy-MM-dd HH:mm:ss X", date));
    }

    /**
     * A shifted date is rendered at the instant it stands for, whatever the zone happens to be
     * doing around it, so it has to agree with {@code java.time} for the same instant and zone -
     * including across the hour a zone skips when daylight saving time begins and the hour it
     * repeats when it ends, where a wall clock reading of the date would not.
     */
    @Test
    public void testFormatWithTimeZoneAgreesWithJavaTime()
    {
        MutableList<String> zoneIds = Lists.mutable.with("GMT", "UTC", "CET", "America/New_York", "Europe/London", "Asia/Tokyo", "Australia/Adelaide", "Asia/Kathmandu", "Pacific/Chatham", "Africa/Monrovia", "Australia/Lord_Howe");
        zoneIds.addAllIterable(ZoneId.SHORT_IDS.keySet());
        PureDate[] dates = {
                DateFunctions.parsePureDate("1500-01-15T12:00:00"),   // before the Gregorian calendar was adopted
                DateFunctions.parsePureDate("1800-01-15T12:00:00"),   // before standard time
                DateFunctions.parsePureDate("1960-01-01T12:00:30"),   // Monrovia, not a whole minute from UTC
                DateFunctions.parsePureDate("2014-01-15T23:45:00"),
                DateFunctions.parsePureDate("2014-03-09T02:30:00"),   // in the hour New York skips
                DateFunctions.parsePureDate("2014-03-09T07:00:00"),   // the instant it skips to
                DateFunctions.parsePureDate("2014-11-02T05:30:00"),   // the first pass through the hour it repeats
                DateFunctions.parsePureDate("2014-11-02T06:30:00"),   // the second
                DateFunctions.parsePureDate("2014-03-30T01:30:00"),   // London, an hour into summer time
                DateFunctions.parsePureDate("2014-07-04T12:00:00")
        };
        DateTimeFormatter rfc822 = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss Z");
        DateTimeFormatter iso8601 = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss X");
        for (String zoneId : zoneIds)
        {
            ZoneId zone = ZoneId.of(zoneId, ZoneId.SHORT_IDS);
            for (PureDate date : dates)
            {
                ZonedDateTime zoned = PureDateToJava.start().toInstant(date).atZone(zone);
                Assert.assertEquals(zoneId + " " + date, zoned.format(rfc822), format("[" + zoneId + "]yyyy-MM-dd HH:mm:ss Z", date));
                Assert.assertEquals(zoneId + " " + date, zoned.format(iso8601), format("[" + zoneId + "]yyyy-MM-dd HH:mm:ss X", date));
            }
        }
    }

    // Format: errors

    /**
     * A format string may only ask for components the date has, since there is nothing to write for
     * one it does not carry and no reason to think a date of that granularity was intended.
     */
    @Test
    public void testFormatOfAComponentTheDateDoesNotHave()
    {
        assertFormatFails("Date has no month: 2014", "yyyy-MM", DATES[0]);
        assertFormatFails("Date has no day: 2014-03", "yyyy-MM-dd", DATES[1]);
        assertFormatFails("Date has no hour: 2014-03-10", "yyyy-MM-dd HH", DATES[2]);
        assertFormatFails("Date has no hour: 2014-03-10", "h", DATES[2]);
        assertFormatFails("Date has no hour: 2014-03-10", "a", DATES[2]);
        assertFormatFails("Date has no hour (required for Z): 2014-03-10", "Z", DATES[2]);
        assertFormatFails("Date has no hour (required for X): 2014-03-10", "X", DATES[2]);
        assertFormatFails("Date has no minute: 2014-03-10T16", "HH:mm", DATES[3]);
        assertFormatFails("Date has no second: 2014-03-10T16:12+0000", "HH:mm:ss", DATES[4]);
        assertFormatFails("Date has no sub-second: 2014-03-10T16:12:35+0000", "HH:mm:ss.SSS", DATES[5]);
    }

    @Test
    public void testFormatWithAnInvalidFormatString()
    {
        assertFormatFails("Invalid format control character 'Q' in format string: yyyy-Q", "yyyy-Q", DATE);
        assertFormatFails("Invalid format control character '#' in format string: #", "#", DATE);
        assertFormatFails("Missing closing quote in format string: \"abc", "\"abc", DATE);
        assertFormatFails("Missing closing bracket in format string: [EST", "[EST", DATE);
        assertFormatFails("Missing closing quotes in time zone definition: [\"EST]yyyy", "[\"EST]yyyy", DATE);
        assertFormatFails("Time zone can only be set at the beginning of the format string", "yyyy[EST]", DATE);
        assertFormatFails("Time zone can only be set at the beginning of the format string", "[EST]yyyy[EST]", DATE);
    }

    /**
     * {@link LatestDate} has no components to write, so it can be neither formatted nor appended.
     */
    @Test
    public void testLatestDateCannotBeWritten()
    {
        Assert.assertEquals(
                "Invalid operation for LatestDate",
                Assert.assertThrows(UnsupportedOperationException.class, () -> format("yyyy", LatestDate.instance)).getMessage());
        Assert.assertEquals(
                "Invalid operation for LatestDate",
                Assert.assertThrows(UnsupportedOperationException.class, () -> DateFormat.append(new StringBuilder(), LatestDate.instance)).getMessage());
    }

    // Append

    @Test
    public void testAppendWritesTheCanonicalString()
    {
        for (int i = 0; i < DATES.length; i++)
        {
            Assert.assertEquals(CANONICAL_STRINGS[i], DateFormat.append(new StringBuilder(), DATES[i]).toString());

            // which is what the date renders itself as
            Assert.assertEquals(CANONICAL_STRINGS[i], DATES[i].toString());
        }

        // the offset is written from minute granularity on, where there is a time to which it applies
        Assert.assertEquals("2014-03-10T16", DateFormat.append(new StringBuilder(), DATES[3]).toString());
        Assert.assertEquals("-44-03-10", DateFormat.append(new StringBuilder(), DateFunctions.newPureDate(-44, 3, 10)).toString());
    }

    /**
     * Both writers append to what they are given and hand it back, rather than returning something
     * of their own, so a caller can keep building on it.
     */
    @Test
    public void testTheWritersReturnTheAppendableTheyWereGiven()
    {
        StringBuilder builder = new StringBuilder("date: ");
        Assert.assertSame(builder, DateFormat.append(builder, DATE));
        Assert.assertSame(builder, DateFormat.format(builder, " \"(\"yyyy\")\"", DATE));
        Assert.assertEquals("date: 2014-03-10T16:12:35.070004235+0000 (2014)", builder.toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWriteWritesTheCanonicalString() throws IOException
    {
        StringWriter writer = new StringWriter();
        DateFormat.write(writer, DATE);
        Assert.assertEquals("2014-03-10T16:12:35.070004235+0000", writer.toString());
    }

    // Parse

    @Test
    public void testParsePureDateAtEachGranularity()
    {
        for (int i = 0; i < DATES.length; i++)
        {
            Assert.assertEquals(CANONICAL_STRINGS[i], DATES[i], DateFormat.parsePureDate(CANONICAL_STRINGS[i]));
        }
    }

    /**
     * Parsing and appending are inverse: every date writes a string that parses back to it.
     */
    @Test
    public void testAppendAndParseRoundTrip()
    {
        for (PureDate date : DATES)
        {
            Assert.assertEquals(date, DateFormat.parsePureDate(DateFormat.append(new StringBuilder(), date).toString()));
        }
    }

    /**
     * The overload taking a range parses a date out of the middle of a larger string, which is how
     * a date embedded in Pure source is read.
     */
    @Test
    public void testParsePureDateWithinALargerString()
    {
        String string = "xx%2014-03-10T16:12:35zz";
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35), DateFormat.parsePureDate(string, 2, 22));

        // the range decides what is parsed, not the string
        Assert.assertEquals(DateFunctions.newPureDate(2014), DateFormat.parsePureDate("2014-03-10", 0, 4));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), DateFormat.parsePureDate("2014-03-10", 0, 7));

        // whitespace within the range is skipped, at either end
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), DateFormat.parsePureDate("  2014-03  ", 0, 11));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3), DateFormat.parsePureDate("\t2014-03\n", 0, 9));
    }

    /**
     * The typed parse methods are the plain one with a cast, so a string of the wrong granularity
     * fails as a cast rather than as a parse.
     */
    @Test
    public void testParseStrictDateAndParseDateTime()
    {
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFormat.parseStrictDate("2014-03-10"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10), DateFormat.parseStrictDate("%2014-03-10"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16), DateFormat.parseDateTime("2014-03-10T16"));
        Assert.assertEquals(DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070"), DateFormat.parseDateTime("2014-03-10T16:12:35.070"));

        Assert.assertThrows(ClassCastException.class, () -> DateFormat.parseStrictDate("2014"));
        Assert.assertThrows(ClassCastException.class, () -> DateFormat.parseStrictDate("2014-03-10T16"));
        Assert.assertThrows(ClassCastException.class, () -> DateFormat.parseDateTime("2014-03-10"));
    }

    @Test
    public void testParseTimeZoneOffsets()
    {
        PureDate expected = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35);
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T16:12:35"));
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T16:12:35Z"));
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T16:12:35+0000"));
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T11:12:35-0500"));
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T17:12:35+0100"));
        Assert.assertEquals(expected, DateFormat.parsePureDate("2014-03-10T16:42:35+0030"));

        // an offset can carry the date into the next day
        Assert.assertEquals(
                DateFunctions.newPureDate(2014, 3, 11, 2, 12),
                DateFormat.parsePureDate("2014-03-10T21:12-0500"));
    }

    @Test
    public void testParseInvalidDateStrings()
    {
        assertParseFails("Invalid date string: ''", "");
        assertParseFails("Invalid date string: '   '", "   ");
        assertParseFails("Invalid date string: '%'", "%");
        assertParseFails("Error parsing year: 'not a date'", "not a date");
        assertParseFails("Invalid date string: '2014/03'", "2014/03");
        assertParseFails("Invalid time zone: +5", "2014-03-10T16:12:35+5");

        // a quote in the offending string is escaped in the message
        assertParseFails("Error parsing month: '2014-\\'x'", "2014-'x");
    }

    // Helpers

    private static String format(String formatString, PureDate date)
    {
        return DateFormat.format(new StringBuilder(), formatString, date).toString();
    }

    private static void assertFormatFails(String expectedMessage, String formatString, PureDate date)
    {
        Assert.assertEquals(
                formatString,
                expectedMessage,
                Assert.assertThrows(formatString, IllegalArgumentException.class, () -> format(formatString, date)).getMessage());
    }

    private static void assertParseFails(String expectedMessage, String string)
    {
        Assert.assertEquals(
                string,
                expectedMessage,
                Assert.assertThrows(string, IllegalArgumentException.class, () -> DateFormat.parsePureDate(string)).getMessage());
    }
}
