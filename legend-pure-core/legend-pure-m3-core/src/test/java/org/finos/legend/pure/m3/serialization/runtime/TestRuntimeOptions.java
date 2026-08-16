// Copyright 2020 Goldman Sachs
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

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * Content is asserted against a {@link Properties} instance built by the test, never against the system
 * properties: the VM wide snapshot behind {@link RuntimeOptions#defaultOptions()} is taken once per JVM, so
 * any assertion on its contents would depend on which test happened to load the class first. Tests that do
 * involve the default options assert identity only.
 */
public class TestRuntimeOptions
{
    private static final String PREFIX = RuntimeOptions.DEFAULT_OPTION_PREFIX;

    @Test
    public void testSnapshotOfProperties()
    {
        Properties properties = properties(
                PREFIX + "Enabled", "true",
                PREFIX + "NonBoolean", "yes",
                PREFIX + "Disabled", "false",
                "some.other.prefix.Enabled", "true");

        RuntimeOptions options = new CachedSystemPropertyOptions(properties, PREFIX);

        Assert.assertTrue(options.isOptionSet("Enabled"));
        Assert.assertFalse(options.isOptionSet("NonBoolean"));
        Assert.assertFalse(options.isOptionSet("Disabled"));
        Assert.assertFalse(options.isOptionSet("Absent"));
        Assert.assertFalse("the prefix must be stripped, not retained", options.isOptionSet(PREFIX + "Enabled"));
        Assert.assertFalse("only the configured prefix is scanned", options.isOptionSet("some.other.prefix.Enabled"));
    }

    @Test
    public void testTrueIsCaseInsensitive()
    {
        Assert.assertTrue(new CachedSystemPropertyOptions(properties(PREFIX + "Enabled", "TRUE"), PREFIX).isOptionSet("Enabled"));
        Assert.assertTrue(new CachedSystemPropertyOptions(properties(PREFIX + "Enabled", "True"), PREFIX).isOptionSet("Enabled"));
    }

    @Test
    public void testPropertiesSetAfterTheSnapshotAreNotVisible()
    {
        Properties properties = properties();
        RuntimeOptions options = new CachedSystemPropertyOptions(properties, PREFIX);
        properties.setProperty(PREFIX + "SetLate", "true");
        Assert.assertFalse(options.isOptionSet("SetLate"));
    }

    @Test
    public void testDefaultOptionsAreSnapshottedOncePerVM()
    {
        Assert.assertSame(RuntimeOptions.defaultOptions(), RuntimeOptions.defaultOptions());
    }

    @Test
    public void testBuilderDefaultsToDefaultOptions()
    {
        Assert.assertSame(RuntimeOptions.defaultOptions(), newRuntimeBuilder().build().getOptions());
    }

    @Test
    public void testNullOptionsMeanDefaultOptions()
    {
        Assert.assertSame(RuntimeOptions.defaultOptions(), newRuntimeBuilder().withOptions(null).build().getOptions());
    }

    @Test
    public void testExplicitOptionsAreHonored()
    {
        RuntimeOptions explicit = RuntimeOptions.noOptionsSet();
        Assert.assertSame(explicit, newRuntimeBuilder().withOptions(explicit).build().getOptions());
    }

    @Test
    public void testNoOptionsSet()
    {
        Assert.assertFalse(RuntimeOptions.noOptionsSet().isOptionSet("Enabled"));
    }

    private static PureRuntimeBuilder newRuntimeBuilder()
    {
        return new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository())));
    }

    private static Properties properties(String... keysAndValues)
    {
        Properties properties = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            properties.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }
}
