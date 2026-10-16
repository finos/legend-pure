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

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.graph.GraphPath;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestPrefixedReferenceIdExtension extends AbstractReferenceIdExtensionTest
{
    @BeforeClass
    public static void setUpExtension()
    {
        extension = new PrefixedReferenceIdExtension();
    }

    @Test
    @Override
    public void testLoadAsService()
    {
        int version = extension.version();
        Assert.assertTrue(Iterate.noneSatisfy(ServiceLoader.load(ReferenceIdExtension.class), ext -> version == ext.version()));
    }

    @Test
    public void testIdsComparedWithVersion1()
    {
        ReferenceIdExtension version1 = new ReferenceIdExtensionV1();
        ReferenceIdProvider version1Provider = version1.newProvider(processorSupport);
        ReferenceIdResolver version1Resolver = version1.newResolver(processorSupport::package_getByUserPath);
        int[] counts = {0, 0};
        GraphNodeIterable.builder()
                .withStartingNodes(repository.getTopLevels())
                .withKeyFilter((node, key) -> !M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.contains(node.getRealKeyByName(key)))
                .build()
                .forEach(instance ->
                {
                    if (version1Provider.hasReferenceId(instance))
                    {
                        Assert.assertTrue(this.provider.hasReferenceId(instance));
                        String version1Id = version1Provider.getReferenceId(instance);
                        String testVersionId = this.provider.getReferenceId(instance);
                        if (GraphPath.parse(version1Id).getEdgeCount() == 0)
                        {
                            // an element: the same id, which both versions resolve
                            Assert.assertEquals(version1Id, testVersionId);
                            Assert.assertSame(version1Id, instance, version1Resolver.resolveReference(version1Id));
                            Assert.assertSame(version1Id, instance, this.resolver.resolveReference(version1Id));
                            counts[0]++;
                        }
                        else
                        {
                            // any other instance: different ids, each of which only its own version accepts
                            Assert.assertNotEquals(version1Id, testVersionId);
                            Assert.assertThrows(testVersionId, InvalidReferenceIdException.class, () -> version1Resolver.resolveReference(testVersionId));
                            Assert.assertThrows(version1Id, InvalidReferenceIdException.class, () -> this.resolver.resolveReference(version1Id));
                            counts[1]++;
                        }
                    }
                    else
                    {
                        Assert.assertFalse(this.provider.hasReferenceId(instance));
                    }
                });
        Assert.assertNotEquals(0, counts[0]);
        Assert.assertNotEquals(0, counts[1]);
    }

    @Test
    public void testInvalidIds()
    {
        Assert.assertThrows(InvalidReferenceIdException.class, () -> this.resolver.resolveReference(null));
        Assert.assertThrows(InvalidReferenceIdException.class, () -> this.resolver.resolveReference(""));
        Assert.assertThrows(InvalidReferenceIdException.class, () -> this.resolver.resolveReference("~"));
        Assert.assertThrows(InvalidReferenceIdException.class, () -> this.resolver.resolveReference("~test::model::SimpleClass"));
        Assert.assertThrows(InvalidReferenceIdException.class, () -> this.resolver.resolveReference("test::model::SimpleClass.properties['name']"));
        Assert.assertSame(getCoreInstance("test::model::SimpleClass"), this.resolver.resolveReference("test::model::SimpleClass"));
    }
}
