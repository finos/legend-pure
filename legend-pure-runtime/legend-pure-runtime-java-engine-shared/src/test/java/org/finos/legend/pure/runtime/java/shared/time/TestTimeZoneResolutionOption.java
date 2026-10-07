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
import org.junit.Assert;
import org.junit.Test;

public class TestTimeZoneResolutionOption
{
    /**
     * The name is what legend-engine sets and what a system property names, so it may not drift.
     */
    @Test
    public void testOptionName()
    {
        Assert.assertEquals("LegacyTimeZoneResolution", TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION);
    }

    @Test
    public void testStrictUnlessTheOptionIsSet()
    {
        Assert.assertSame(TimeZoneResolution.STRICT, TimeZoneResolutionOption.getTimeZoneResolution(RuntimeOptions.noOptionsSet()));
        Assert.assertSame(TimeZoneResolution.STRICT, TimeZoneResolutionOption.getTimeZoneResolution(name -> !TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION.equals(name)));
        Assert.assertSame(TimeZoneResolution.LEGACY, TimeZoneResolutionOption.getTimeZoneResolution(TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION::equals));
    }

    @Test
    public void testOptionsToggledAtRunTimeAreSeenAtOnce()
    {
        MutableRuntimeOptions options = MutableRuntimeOptions.empty();
        Assert.assertSame(TimeZoneResolution.STRICT, TimeZoneResolutionOption.getTimeZoneResolution(options));
        options.setOption(TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION, true);
        Assert.assertSame(TimeZoneResolution.LEGACY, TimeZoneResolutionOption.getTimeZoneResolution(options));
        options.setOption(TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION, false);
        Assert.assertSame(TimeZoneResolution.STRICT, TimeZoneResolutionOption.getTimeZoneResolution(options));
    }

    @Test
    public void testSetLegacyTimeZoneResolution()
    {
        // the options come back, so they can be set as they are made
        MutableRuntimeOptions options = TimeZoneResolutionOption.setLegacyTimeZoneResolution(MutableRuntimeOptions.empty());
        Assert.assertTrue(options.isOptionSet(TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION));
        Assert.assertSame(TimeZoneResolution.LEGACY, TimeZoneResolutionOption.getTimeZoneResolution(options));

        // setting it again changes nothing, and gives back the same options
        Assert.assertSame(options, TimeZoneResolutionOption.setLegacyTimeZoneResolution(options));
        Assert.assertSame(TimeZoneResolution.LEGACY, TimeZoneResolutionOption.getTimeZoneResolution(options));
    }

    /**
     * Each resolution set is the one then answered, whichever was set before it, and only the one
     * option is touched in setting it.
     */
    @Test
    public void testSetTimeZoneResolution()
    {
        MutableRuntimeOptions options = MutableRuntimeOptions.empty();
        options.setOption("SomeOtherOption", true);
        for (TimeZoneResolution before : TimeZoneResolution.values())
        {
            for (TimeZoneResolution after : TimeZoneResolution.values())
            {
                Assert.assertSame(options, TimeZoneResolutionOption.setTimeZoneResolution(options, before));
                Assert.assertSame(before, TimeZoneResolutionOption.getTimeZoneResolution(options));
                Assert.assertSame(options, TimeZoneResolutionOption.setTimeZoneResolution(options, after));
                Assert.assertSame(before + " then " + after, after, TimeZoneResolutionOption.getTimeZoneResolution(options));
                Assert.assertEquals(after == TimeZoneResolution.LEGACY, options.isOptionSet(TimeZoneResolutionOption.LEGACY_TIME_ZONE_RESOLUTION));
                Assert.assertTrue(options.isOptionSet("SomeOtherOption"));
            }
        }
    }
}
