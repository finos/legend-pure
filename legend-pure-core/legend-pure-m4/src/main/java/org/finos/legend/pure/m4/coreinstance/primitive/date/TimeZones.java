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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Resolves the name of a time zone, as one is written in a date format string or on a database
 * connection, to the zone it stands for.
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
 * <p>{@link #parse(String)} rejects a name it cannot resolve. {@link #parse(String, ZoneId)} falls
 * back to a zone of the caller's choosing instead, which is how a caller that used to resolve
 * through {@link TimeZone#getTimeZone(String)} holds its old behavior while the callers move over.
 * New code should take the rejecting form.
 */
public class TimeZones
{
    /**
     * GMT, the zone a Pure date is understood to be in when no other zone is named.
     */
    public static final ZoneId GMT = ZoneId.of("GMT");

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
     * Resolve a time zone name to the zone it stands for, falling back to the given zone when the
     * name is null or stands for no zone.
     *
     * @param timeZone         time zone name
     * @param whenUnresolvable zone to fall back to
     * @return zone the name stands for, or the zone to fall back to
     */
    public static ZoneId parse(String timeZone, ZoneId whenUnresolvable)
    {
        if (timeZone == null)
        {
            return whenUnresolvable;
        }
        try
        {
            return ZoneId.of(timeZone, ZoneId.SHORT_IDS);
        }
        catch (DateTimeException ignored)
        {
            return whenUnresolvable;
        }
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
        return newCalendar(parse(timeZone));
    }

    /**
     * Create a calendar in the zone a time zone name stands for, falling back to the given zone
     * when the name is null or stands for no zone.
     *
     * @param timeZone         time zone name
     * @param whenUnresolvable zone to fall back to
     * @return calendar in the zone the name stands for, or in the zone to fall back to
     */
    public static Calendar newCalendar(String timeZone, ZoneId whenUnresolvable)
    {
        return newCalendar(parse(timeZone, whenUnresolvable));
    }

    private static Calendar newCalendar(ZoneId timeZone)
    {
        return new GregorianCalendar(TimeZone.getTimeZone(timeZone));
    }
}
