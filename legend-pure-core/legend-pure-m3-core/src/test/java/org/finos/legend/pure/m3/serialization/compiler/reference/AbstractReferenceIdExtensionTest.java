// Copyright 2025 Goldman Sachs
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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ServiceLoader;

public abstract class AbstractReferenceIdExtensionTest extends AbstractReferenceTest
{
    protected static ReferenceIdExtension extension;

    protected ReferenceIdProvider provider;
    protected ReferenceIdResolver resolver;

    @Before
    public void setUpProviderResolver()
    {
        this.provider = extension.newProvider(processorSupport);
        this.resolver = extension.newResolver(processorSupport::package_getByUserPath);
    }

    @Test
    public void testLoadAsService()
    {
        int version = extension.version();
        MutableList<ReferenceIdExtension> extensionsAtVersion = Iterate.select(ServiceLoader.load(ReferenceIdExtension.class), ext -> version == ext.version(), Lists.mutable.empty());
        Assert.assertEquals(Lists.fixedSize.with(extension.getClass()), extensionsAtVersion.collect(Object::getClass));
    }

    @Test
    public void testReferenceIdProviders()
    {
        int version = extension.version();
        ReferenceIds referenceIds = ReferenceIdProviders.builder().withProcessorSupport(processorSupport).withExtension(extension).build();
        Assert.assertTrue(referenceIds.isVersionAvailable(version));
        Assert.assertSame(extension, referenceIds.getExtension(version));
    }

    @Test
    public void testReferenceIdResolvers()
    {
        int version = extension.version();
        ReferenceIds referenceIds = ReferenceIdResolvers.builder().withPackagePathResolver(processorSupport::package_getByUserPath).withExtension(extension).build();
        Assert.assertTrue(referenceIds.isVersionAvailable(version));
        Assert.assertSame(extension, referenceIds.getExtension(version));
    }

    @Test
    public void testProviderAndResolverVersions()
    {
        Assert.assertEquals(extension.version(), extension.newProvider(processorSupport).version());
        Assert.assertEquals(extension.version(), extension.newResolver(processorSupport::package_getByUserPath).version());
    }

    @Test
    public void testAllInstances()
    {
        GraphNodeIterable.builder()
                .withStartingNodes(repository.getTopLevels())
                .withKeyFilter((node, key) -> !M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.contains(node.getRealKeyByName(key)))
                .build()
                .forEach(instance ->
                {
                    if (this.provider.hasReferenceId(instance))
                    {
                        assertRefId(instance);
                    }
                    else if (shouldHaveReferenceId(instance))
                    {
                        StringBuilder builder = appendInstanceDescription(new StringBuilder(), instance, true, true)
                                .append(" should have a reference id but does not");
                        Assert.fail(builder.toString());
                    }
                    else
                    {
                        Assert.assertThrows(ReferenceIdProvisionException.class, () -> this.provider.getReferenceId(instance));
                    }
                });
    }

    private boolean shouldHaveReferenceId(CoreInstance instance)
    {
        return (instance.getSourceInformation() == null) ? _Package.isPackage(instance, processorSupport) : !AnyStubHelper.isStub(instance);
    }

    private StringBuilder appendInstanceDescription(StringBuilder builder, CoreInstance instance, boolean includeClassifier, boolean includeSourceInfo)
    {
        appendInstanceDescription(builder, instance);
        if (includeClassifier || includeSourceInfo)
        {
            builder.append(" (");
            if (includeClassifier)
            {
                PackageableElement.writeUserPathForPackageableElement(builder.append("instance of "), instance.getClassifier());
            }
            if (includeSourceInfo)
            {
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage((includeClassifier ? builder.append(", ") : builder).append("at "));
                }
            }
            builder.append(')');
        }
        return builder;
    }

    private StringBuilder appendInstanceDescription(StringBuilder builder, CoreInstance instance)
    {
        if (PackageableElement.isPackageableElement(instance, processorSupport))
        {
            return PackageableElement.writeUserPathForPackageableElement(builder, instance);
        }
        ResolvedGraphPath path = tryFindPathToInstance(instance);
        if (path != null)
        {
            return path.getGraphPath().writeDescription(builder);
        }
        return builder.append(instance);
    }

    protected ResolvedGraphPath tryFindPathToInstance(CoreInstance instance)
    {
        LazyIterable<ResolvedGraphPath> paths = GraphTools.getPathsToInstance(instance, processorSupport);
        return (instance instanceof ReferenceUsage) ?
               paths.getAny() :
               paths.detect(rgp -> rgp.getGraphPath().getEdges().noneSatisfy(e -> M3Properties.referenceUsages.equals(e.getProperty())));
    }

    protected String assertRefId(CoreInstance instance)
    {
        return assertRefId(null, instance);
    }

    protected String assertRefId(String expectedId, CoreInstance instance)
    {
        String actualId;
        try
        {
            actualId = this.provider.getReferenceId(instance);
        }
        catch (ReferenceIdProvisionException e)
        {
            if (expectedId != null)
            {
                throw new RuntimeException("Expected to generate id: " + expectedId, e);
            }
            throw e;
        }
        if (expectedId != null)
        {
            Assert.assertEquals(expectedId, actualId);
        }
        CoreInstance resolved = this.resolver.resolveReference(actualId);
        Assert.assertSame(actualId, instance, resolved);
        return actualId;
    }
}
