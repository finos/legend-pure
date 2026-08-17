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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * Seeded from a {@link Properties} instance built by the test rather than the system properties, so nothing
 * here depends on JVM wide state or on test ordering.
 */
public class TestMutableRuntimeOptions
{
    private static final String PREFIX = RuntimeOptions.DEFAULT_OPTION_PREFIX;

    @Test
    public void testEmptyHasNothingSet()
    {
        MutableRuntimeOptions options = MutableRuntimeOptions.empty();
        Assert.assertFalse(options.isOptionSet("Added"));
        Assert.assertEquals(Sets.mutable.empty(), options.getSetOptions());
    }

    @Test
    public void testSeedingFromProperties()
    {
        MutableRuntimeOptions options = seeded("Seeded", "true", "NonBoolean", "yes");

        Assert.assertTrue(options.isOptionSet("Seeded"));
        Assert.assertFalse(options.isOptionSet("NonBoolean"));
        Assert.assertEquals(Sets.mutable.with("Seeded"), options.getSetOptions());
    }

    @Test
    public void testTogglingOnAndOff()
    {
        MutableRuntimeOptions options = seeded("Seeded", "true");

        Assert.assertFalse(options.setOption("Seeded", false));
        Assert.assertFalse(options.isOptionSet("Seeded"));

        Assert.assertTrue(options.setOption("Added", true));
        Assert.assertTrue(options.isOptionSet("Added"));
        Assert.assertEquals(Sets.mutable.with("Added"), options.getSetOptions());
    }

    @Test
    public void testTogglingIsIdempotent()
    {
        MutableRuntimeOptions options = MutableRuntimeOptions.empty();
        options.setOption("Added", true);
        options.setOption("Added", true);
        Assert.assertEquals(Sets.mutable.with("Added"), options.getSetOptions());

        options.setOption("Added", false);
        options.setOption("Added", false);
        Assert.assertEquals(Sets.mutable.empty(), options.getSetOptions());
    }

    @Test
    public void testTogglingDoesNotWriteTheSeedProperties()
    {
        Properties properties = new Properties();
        properties.setProperty(PREFIX + "Seeded", "true");
        MutableRuntimeOptions options = MutableRuntimeOptions.fromProperties(properties, PREFIX);

        options.setOption("Added", true);
        options.setOption("Seeded", false);

        Assert.assertNull("setting an option must not write a property", properties.getProperty(PREFIX + "Added"));
        Assert.assertEquals("clearing an option must not write a property", "true", properties.getProperty(PREFIX + "Seeded"));
    }

    @Test
    public void testFromSystemPropertiesDoesNotWriteSystemProperties()
    {
        String name = "TestMutableRuntimeOptionsNeverWritten";
        MutableRuntimeOptions options = MutableRuntimeOptions.fromSystemProperties();
        options.setOption(name, true);

        Assert.assertTrue(options.isOptionSet(name));
        Assert.assertNull(System.getProperty(PREFIX + name));
    }

    @Test
    public void testGetSetOptionsIsNotALiveView()
    {
        MutableRuntimeOptions options = MutableRuntimeOptions.empty();
        options.setOption("Seeded", true);
        Object snapshot = options.getSetOptions();
        options.setOption("Added", true);

        Assert.assertEquals(Sets.mutable.with("Seeded"), snapshot);
    }

    private static MutableRuntimeOptions seeded(String... namesAndValues)
    {
        Properties properties = new Properties();
        for (int i = 0; i < namesAndValues.length; i += 2)
        {
            properties.setProperty(PREFIX + namesAndValues[i], namesAndValues[i + 1]);
        }
        return MutableRuntimeOptions.fromProperties(properties, PREFIX);
    }
}
