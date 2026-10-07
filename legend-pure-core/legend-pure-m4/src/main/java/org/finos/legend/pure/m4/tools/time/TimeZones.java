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
import java.time.ZoneOffset;
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
 * <p>Every method here but the deprecated legacy ones rejects a name it cannot resolve. A caller
 * wanting a default for an absent name chooses one itself, by naming the zone it means: a name that
 * is present but stands for no zone is an error, not an occasion to guess.
 *
 * <p>{@link #parse(String)}, {@link #parseTimeZone(String)}, and {@link #newCalendar(String)} are
 * {@link TimeZoneResolution#STRICT}. The deprecated {@link #parseLegacy(String)},
 * {@link #parseTimeZoneLegacy(String)}, and {@link #newCalendarLegacy(String)} are
 * {@link TimeZoneResolution#LEGACY}: they resolve through {@link TimeZone#getTimeZone(String)}
 * instead, and exist only so that legend-engine can keep its old behavior in the Pure it runs.
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
     *
     * <p>{@link ZoneId#normalized()} answers with the zone itself unless the zone is a single fixed
     * offset, which is both the only kind of zone java.util reads wrongly and the only kind
     * normalizing would change. So a zone whose offset varies is a region name that needs no
     * checking, and a zone that needs checking is one whose offset is the whole of it.
     */
    private static TimeZone toTimeZone(ZoneId timeZone)
    {
        TimeZone converted = TimeZone.getTimeZone(timeZone);
        ZoneId normalized = timeZone.normalized();
        if (normalized == timeZone)
        {
            return converted;
        }
        if (!converted.useDaylightTime() && (normalized instanceof ZoneOffset) && (converted.getRawOffset() == (((ZoneOffset) normalized).getTotalSeconds() * 1000)))
        {
            return converted;
        }
        return TimeZone.getTimeZone(normalized);
    }

    /**
     * Resolve a time zone name the way {@link TimeZone#getTimeZone(String)} does, flaws and all: it
     * takes an offset only with a {@code GMT} prefix, and answers every name it does not know with
     * GMT rather than an error. This is {@link TimeZoneResolution#LEGACY}.
     *
     * @param timeZone time zone name
     * @return zone the name stands for, or GMT if java.util does not know the name
     * @throws IllegalArgumentException if the name is null
     * @deprecated Use {@link #parseTimeZone(String)}, which reports a name that stands for no zone
     * rather than reading it as GMT. Retained temporarily, so that legend-engine can keep its old
     * behavior in the Pure it runs.
     */
    @Deprecated
    public static TimeZone parseTimeZoneLegacy(String timeZone)
    {
        if (timeZone == null)
        {
            throw new IllegalArgumentException("Unknown time zone: null");
        }
        return TimeZone.getTimeZone(timeZone);
    }

    /**
     * Resolve a time zone name the way {@link #parseTimeZoneLegacy(String)} does, converted with
     * {@link TimeZone#toZoneId()} so that arithmetic on the zone is done by java.time rather than by
     * a {@link Calendar}.
     *
     * @param timeZone time zone name
     * @return zone the name stands for, or GMT if java.util does not know the name
     * @throws IllegalArgumentException if the name is null
     * @throws DateTimeException if java.util knows the name but java.time cannot represent it, as
     *                           with a {@code GMT} offset of more than 18 hours
     * @deprecated Use {@link #parse(String)}, which reports a name that stands for no zone rather
     * than reading it as GMT. Retained temporarily, so that legend-engine can keep its old behavior
     * in the Pure it runs.
     */
    @Deprecated
    public static ZoneId parseLegacy(String timeZone)
    {
        return parseTimeZoneLegacy(timeZone).toZoneId();
    }

    /**
     * Create a calendar in the zone a time zone name stands for, resolving the name the way
     * {@link #parseTimeZoneLegacy(String)} does.
     *
     * @param timeZone time zone name
     * @return calendar in the zone the name stands for, or in GMT if java.util does not know the name
     * @throws IllegalArgumentException if the name is null
     * @deprecated Use {@link #newCalendar(String)}, which reports a name that stands for no zone
     * rather than reading it as GMT. Retained temporarily, so that legend-engine can keep its old
     * behavior in the Pure it runs.
     */
    @Deprecated
    public static Calendar newCalendarLegacy(String timeZone)
    {
        return new GregorianCalendar(parseTimeZoneLegacy(timeZone));
    }
}
