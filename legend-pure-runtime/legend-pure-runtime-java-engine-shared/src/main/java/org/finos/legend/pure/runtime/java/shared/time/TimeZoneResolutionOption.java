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

package org.finos.legend.pure.runtime.java.shared.time;

import org.finos.legend.pure.m3.serialization.runtime.MutableRuntimeOptions;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m4.tools.time.TimeZoneResolution;

/**
 * The runtime option that asks for time zone names to be resolved the
 * {@link TimeZoneResolution#LEGACY legacy} way, and the one place the natives that resolve a name
 * ask which way to resolve it. Options a runtime holds as {@link MutableRuntimeOptions} can be told
 * which way to ask for through here as well.
 *
 * <p>The legacy resolution is how legend-engine resolved names before it adopted
 * {@link TimeZoneResolution#STRICT strict} resolution. The option is retained temporarily, for
 * legend-engine to set on the Pure it runs; with it unset, a name means what it means everywhere
 * else in Pure.
 */
public class TimeZoneResolutionOption
{
    /**
     * The name of the option asking for the legacy resolution.
     */
    public static final String LEGACY_TIME_ZONE_RESOLUTION = "LegacyTimeZoneResolution";

    private TimeZoneResolutionOption()
    {
    }

    /**
     * How time zone names are to be resolved under the given options: the legacy way if
     * {@link #LEGACY_TIME_ZONE_RESOLUTION} is set, and strictly otherwise. Ask each time a name is
     * resolved, so that options toggled while a runtime is running are seen at once.
     *
     * @param options runtime options
     * @return time zone resolution
     */
    public static TimeZoneResolution getTimeZoneResolution(RuntimeOptions options)
    {
        return options.isOptionSet(LEGACY_TIME_ZONE_RESOLUTION) ? TimeZoneResolution.LEGACY : TimeZoneResolution.STRICT;
    }

    /**
     * Ask for time zone names to be resolved the legacy way under the given options, by setting
     * {@link #LEGACY_TIME_ZONE_RESOLUTION}. A runtime built on the options sees the change at once.
     *
     * @param options runtime options to set the option on
     * @param <T>     options type
     * @return the options
     * @see #setTimeZoneResolution(MutableRuntimeOptions, TimeZoneResolution)
     */
    public static <T extends MutableRuntimeOptions> T setLegacyTimeZoneResolution(T options)
    {
        options.setOption(LEGACY_TIME_ZONE_RESOLUTION, true);
        return options;
    }

    /**
     * Ask for time zone names to be resolved in the given way under the given options, by setting
     * {@link #LEGACY_TIME_ZONE_RESOLUTION} for the legacy resolution and clearing it for the strict
     * one, so that {@link #getTimeZoneResolution} then answers with the resolution given. A runtime
     * built on the options sees the change at once.
     *
     * @param options    runtime options to set or clear the option on
     * @param resolution time zone resolution to ask for
     * @param <T>        options type
     * @return the options
     */
    public static <T extends MutableRuntimeOptions> T setTimeZoneResolution(T options, TimeZoneResolution resolution)
    {
        options.setOption(LEGACY_TIME_ZONE_RESOLUTION, resolution == TimeZoneResolution.LEGACY);
        return options;
    }
}
