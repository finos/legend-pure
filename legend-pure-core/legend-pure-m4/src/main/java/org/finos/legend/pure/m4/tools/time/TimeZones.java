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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Resolves the name of a time zone, as one is written in a date format string or on a database
 * connection, to the zone it stands for. Resolve every time zone name here, so that a name means
 * the same zone wherever it is read.
 *
 * <p>A name may be a region such as {@code America/New_York}, one of the three letter
 * abbreviations such as {@code EST}, or an offset from UTC such as {@code +0530}, {@code +05:30},
 * or {@code GMT+5}: whatever {@link ZoneId#of(String, java.util.Map)} accepts against
 * {@link ZoneId#SHORT_IDS}.
 *
 * <p>Resolve through here rather than through {@link TimeZone#getTimeZone(String)}, which reads
 * only the first two of those forms. It takes an offset only with a {@code GMT} prefix, and for
 * every name it does not know, a bare {@code +0530} among them, it returns GMT rather than
 * reporting the name as unknown. A date read or written in GMT that was meant for another zone is
 * wrong by that zone's offset, and nothing about the result says so.
 *
 * <p>Every method here rejects a name it cannot resolve. A caller wanting a default for an absent
 * name chooses one itself, by naming the zone it means: a name that is present but stands for no
 * zone is an error, not an occasion to guess.
 */
public class TimeZones
{
    private TimeZones()
    {
    }

    /**
     * Resolve a time zone name to the zone it stands for.
     *
     * @param timeZone time zone name
     * @return zone the name stands for
     * @throws IllegalArgumentException if the name is null or stands for no zone
     */
    public static ZoneId parse(String timeZone)
    {
        if (timeZone == null)
        {
            throw new IllegalArgumentException("Unknown time zone: null");
        }
        try
        {
            return ZoneId.of(timeZone, ZoneId.SHORT_IDS);
        }
        catch (DateTimeException e)
        {
            throw new IllegalArgumentException("Unknown time zone: " + timeZone, e);
        }
    }

    /**
     * Resolve a time zone name to the zone it stands for, as a {@link TimeZone} for the sake of the
     * java.util dated APIs that ask for one, {@link java.sql.ResultSet#getTimestamp(int, Calendar)}
     * among them.
     *
     * @param timeZone time zone name
     * @return zone the name stands for
     * @throws IllegalArgumentException if the name is null or stands for no zone
     */
    public static TimeZone parseTimeZone(String timeZone)
    {
        return toTimeZone(parse(timeZone));
    }

    /**
     * Create a calendar in the zone a time zone name stands for.
     *
     * @param timeZone time zone name
     * @return calendar in the zone the name stands for
     * @throws IllegalArgumentException if the name is null or stands for no zone
     */
    public static Calendar newCalendar(String timeZone)
    {
        return new GregorianCalendar(parseTimeZone(timeZone));
    }

    /**
     * Convert a zone to the {@link TimeZone} standing for it.
     *
     * <p>Before Java 21, {@link TimeZone#getTimeZone(ZoneId)} reads an offset carrying a
     * {@code UTC} or {@code UT} prefix, such as {@code UTC+10:00}, as GMT: it prefixes a bare
     * offset with {@code GMT}, but passes every other name through to a lookup that knows region
     * names and {@code GMT} prefixed offsets only. Normalizing such a zone reduces it to the bare
     * offset, the form every version reads. A zone that converts correctly on its own is left
     * alone, so GMT keeps the name GMT rather than taking the name UTC.
     */
    private static TimeZone toTimeZone(ZoneId timeZone)
    {
        TimeZone converted = TimeZone.getTimeZone(timeZone);
        if (converted.toZoneId().getRules().equals(timeZone.getRules()))
        {
            return converted;
        }
        return TimeZone.getTimeZone(timeZone.normalized());
    }
}
