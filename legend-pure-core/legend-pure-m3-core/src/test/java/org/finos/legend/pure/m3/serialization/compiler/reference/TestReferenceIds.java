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

package org.finos.legend.pure.m3.serialization.compiler.reference;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Function;

public class TestReferenceIds
{
    private static final ProcessorSupport PROCESSOR_SUPPORT = new M3ProcessorSupport(new ModelRepository());
    private static final Function<String, CoreInstance> PACKAGE_PATH_RESOLVER = path -> null;

    @Test
    public void testOneVersion()
    {
        assertVersions(1);
    }

    @Test
    public void testContiguousVersions()
    {
        assertVersions(1, 2);
        assertVersions(1, 2, 3);
        assertVersions(2, 3, 4);
    }

    @Test
    public void testOneMissingVersion()
    {
        assertVersions(1, 3);
        assertVersions(1, 2, 4);
        assertVersions(2, 4, 5);
    }

    @Test
    public void testSeveralMissingVersions()
    {
        assertVersions(1, 4);
        assertVersions(1, 3, 5, 7);
    }

    @Test
    public void testExplicitDefaultVersion()
    {
        MutableIntObjectMap<ReferenceIdExtension> extensions = newExtensions(1, 3);

        ReferenceIdProviders providers = ReferenceIdProviders.builder()
                .withProcessorSupport(PROCESSOR_SUPPORT)
                .withExtensions(extensions.values())
                .withDefaultVersion(1)
                .build();
        Assert.assertEquals(1, providers.getDefaultVersion());
        Assert.assertSame(extensions.get(1), providers.getDefaultExtension());
        Assert.assertEquals(1, providers.provider().version());

        ReferenceIdResolvers resolvers = ReferenceIdResolvers.builder()
                .withPackagePathResolver(PACKAGE_PATH_RESOLVER)
                .withExtensions(extensions.values())
                .withDefaultVersion(1)
                .build();
        Assert.assertEquals(1, resolvers.getDefaultVersion());
        Assert.assertSame(extensions.get(1), resolvers.getDefaultExtension());
        Assert.assertEquals(1, resolvers.resolver().version());
    }

    @Test
    public void testUnknownDefaultVersion()
    {
        ReferenceIdResolvers.Builder builder = ReferenceIdResolvers.builder()
                .withPackagePathResolver(PACKAGE_PATH_RESOLVER)
                .withExtensions(newExtensions(1, 3).values())
                .withDefaultVersion(2);
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, builder::build);
        Assert.assertEquals("Default version 2 is unknown", e.getMessage());
    }

    private static void assertVersions(int... versions)
    {
        MutableIntObjectMap<ReferenceIdExtension> extensions = newExtensions(versions);
        int minVersion = extensions.keySet().min();
        int maxVersion = extensions.keySet().max();

        ReferenceIdProviders providers = ReferenceIdProviders.builder()
                .withProcessorSupport(PROCESSOR_SUPPORT)
                .withExtensions(extensions.values())
                .build();
        Assert.assertEquals(maxVersion, providers.getDefaultVersion());
        Assert.assertSame(extensions.get(maxVersion), providers.getDefaultExtension());
        Assert.assertEquals(maxVersion, providers.provider().version());
        for (int version = minVersion - 1; version <= maxVersion + 1; version++)
        {
            int v = version;
            ReferenceIdExtension extension = extensions.get(v);
            if (extension == null)
            {
                Assert.assertFalse(Integer.toString(v), providers.isVersionAvailable(v));
                IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> providers.provider(v));
                Assert.assertEquals("Unknown extension: " + v, e.getMessage());
            }
            else
            {
                Assert.assertTrue(Integer.toString(v), providers.isVersionAvailable(v));
                Assert.assertSame(extension, providers.getExtension(v));
                Assert.assertEquals(v, providers.provider(v).version());
            }
        }

        ReferenceIdResolvers resolvers = ReferenceIdResolvers.builder()
                .withPackagePathResolver(PACKAGE_PATH_RESOLVER)
                .withExtensions(extensions.values())
                .build();
        Assert.assertEquals(maxVersion, resolvers.getDefaultVersion());
        Assert.assertSame(extensions.get(maxVersion), resolvers.getDefaultExtension());
        Assert.assertEquals(maxVersion, resolvers.resolver().version());
        for (int version = minVersion - 1; version <= maxVersion + 1; version++)
        {
            int v = version;
            ReferenceIdExtension extension = extensions.get(v);
            if (extension == null)
            {
                Assert.assertFalse(Integer.toString(v), resolvers.isVersionAvailable(v));
                IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> resolvers.resolver(v));
                Assert.assertEquals("Unknown extension: " + v, e.getMessage());
            }
            else
            {
                Assert.assertTrue(Integer.toString(v), resolvers.isVersionAvailable(v));
                Assert.assertSame(extension, resolvers.getExtension(v));
                Assert.assertEquals(v, resolvers.resolver(v).version());
            }
        }
    }

    private static MutableIntObjectMap<ReferenceIdExtension> newExtensions(int... versions)
    {
        MutableIntObjectMap<ReferenceIdExtension> extensions = IntObjectMaps.mutable.ofInitialCapacity(versions.length);
        for (int version : versions)
        {
            extensions.put(version, new TestExtension(version));
        }
        return extensions;
    }

    private static class TestExtension implements ReferenceIdExtension
    {
        private final int version;

        private TestExtension(int version)
        {
            this.version = version;
        }

        @Override
        public int version()
        {
            return this.version;
        }

        @Override
        public ReferenceIdProvider newProvider(ProcessorSupport processorSupport)
        {
            return new ReferenceIdProvider()
            {
                @Override
                public int version()
                {
                    return TestExtension.this.version;
                }

                @Override
                public boolean hasReferenceId(CoreInstance instance)
                {
                    return false;
                }

                @Override
                public String getReferenceId(CoreInstance reference)
                {
                    throw new ReferenceIdProvisionException("Not supported");
                }
            };
        }

        @Override
        public ReferenceIdResolver newResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            return new AbstractReferenceIdResolver(packagePathResolver)
            {
                @Override
                public int version()
                {
                    return TestExtension.this.version;
                }

                @Override
                public CoreInstance resolveReference(String referenceId)
                {
                    throw new UnresolvableReferenceIdException(referenceId);
                }
            };
        }
    }
}
