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

import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * A way of resolving the name of a time zone to the zone it stands for.
 *
 * <p>{@link #STRICT} is how Pure resolves a name, and is what a name means anywhere Pure reads one.
 * {@link #LEGACY} is how legend-engine resolved one before it adopted strict resolution, and is
 * retained temporarily so that the engine can keep that behavior in the Pure it runs. Nothing else
 * should ask for it. Both resolve through {@link TimeZones}, which holds the methods for each.
 */
public enum TimeZoneResolution
{
    /**
     * Resolve a name through the strict methods of {@link TimeZones}, which use
     * {@link ZoneId#of(String, java.util.Map)} against {@link ZoneId#SHORT_IDS}: a region, one of the
     * three letter abbreviations, or an offset in any of its forms, and an error for a name that
     * stands for no zone.
     */
    STRICT
    {
        @Override
        public ZoneId parse(String timeZone)
        {
            return TimeZones.parse(timeZone);
        }

        @Override
        public TimeZone parseTimeZone(String timeZone)
        {
            return TimeZones.parseTimeZone(timeZone);
        }
    },

    /**
     * Resolve a name through the deprecated legacy methods of {@link TimeZones}, which use
     * {@link TimeZone#getTimeZone(String)}, flaws and all. It reads a region and the three letter
     * abbreviations as {@link #STRICT} does, but an offset only with a {@code GMT} prefix, and it
     * answers every name it does not know with GMT rather than an error: a bare offset such as
     * {@code +0530}, an offset prefixed {@code UTC}, a misspelled region, and a name still carrying
     * the quotes it was written with all read as GMT, and nothing says so.
     *
     * <p>Where a {@link ZoneId} is wanted, the {@link TimeZone} is converted with
     * {@link TimeZone#toZoneId()}, so arithmetic on the resolved zone is done by java.time rather
     * than by a {@link Calendar}. java.time cannot represent a {@code GMT} offset of more than 18
     * hours, which java.util accepts, so such a name resolves to a {@link TimeZone} but fails to
     * convert to a {@link ZoneId}.
     *
     * @deprecated Resolve names {@link #STRICT strictly}. Retained temporarily, so that legend-engine
     * can keep its old behavior in the Pure it runs.
     */
    @Deprecated
    LEGACY
    {
        @Override
        public ZoneId parse(String timeZone)
        {
            return TimeZones.parseLegacy(timeZone);
        }

        @Override
        public TimeZone parseTimeZone(String timeZone)
        {
            return TimeZones.parseTimeZoneLegacy(timeZone);
        }
    };

    /**
     * Resolve a time zone name to the zone it stands for.
     *
     * @param timeZone time zone name
     * @return zone the name stands for
     * @throws IllegalArgumentException if the name is null, or if this resolution rejects names
     *                                  that stand for no zone and this is one
     */
    public abstract ZoneId parse(String timeZone);

    /**
     * Resolve a time zone name to the zone it stands for, as a {@link TimeZone} for the sake of the
     * java.util dated APIs that ask for one.
     *
     * @param timeZone time zone name
     * @return zone the name stands for
     * @throws IllegalArgumentException if the name is null, or if this resolution rejects names
     *                                  that stand for no zone and this is one
     */
    public abstract TimeZone parseTimeZone(String timeZone);

    /**
     * Create a calendar in the zone a time zone name stands for.
     *
     * @param timeZone time zone name
     * @return calendar in the zone the name stands for
     * @throws IllegalArgumentException if the name is null, or if this resolution rejects names
     *                                  that stand for no zone and this is one
     */
    public Calendar newCalendar(String timeZone)
    {
        return new GregorianCalendar(parseTimeZone(timeZone));
    }
}
