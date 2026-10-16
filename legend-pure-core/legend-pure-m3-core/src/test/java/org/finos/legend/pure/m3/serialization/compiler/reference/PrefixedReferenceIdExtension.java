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

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.graph.GraphPath;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.Function;

/**
 * A version of reference ids for tests of code that must handle more than one version. It is not registered as a
 * service.
 * <p>
 * Its version number is -1. Real versions count up from 1, so this version can never be mistaken for a real one or
 * collide with one, and adding it next to version 1 does not change the default version.
 * <p>
 * An element's id is its path, as in version 1. Every other instance's id is its version 1 id with a prefix, which
 * version 1 rejects; and this version rejects version 1's ids for those instances. Ids resolve by walking the graph
 * from the element, as in version 1, so ids of either version resolve against elements written with either version.
 */
public class PrefixedReferenceIdExtension implements ReferenceIdExtension
{
    public static final int VERSION = -1;

    private static final String PREFIX = "~";

    private final ReferenceIdExtension version1 = new ReferenceIdExtensionV1();

    @Override
    public int version()
    {
        return VERSION;
    }

    @Override
    public ReferenceIdProvider newProvider(ProcessorSupport processorSupport)
    {
        ReferenceIdProvider version1Provider = this.version1.newProvider(processorSupport);
        return new ReferenceIdProvider()
        {
            @Override
            public int version()
            {
                return VERSION;
            }

            @Override
            public boolean hasReferenceId(CoreInstance instance)
            {
                return version1Provider.hasReferenceId(instance);
            }

            @Override
            public String getReferenceId(CoreInstance reference)
            {
                String version1Id = version1Provider.getReferenceId(reference);
                return isElementId(GraphPath.parse(version1Id)) ? version1Id : (PREFIX + version1Id);
            }
        };
    }

    @Override
    public ReferenceIdResolver newResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        ReferenceIdResolver version1Resolver = this.version1.newResolver(packagePathResolver);
        return new AbstractReferenceIdResolver(packagePathResolver)
        {
            @Override
            public int version()
            {
                return VERSION;
            }

            @Override
            public CoreInstance resolveReference(String referenceId)
            {
                return version1Resolver.resolveReference(toVersion1Id(referenceId));
            }
        };
    }

    private static String toVersion1Id(String referenceId)
    {
        if (referenceId == null)
        {
            throw new InvalidReferenceIdException(null);
        }

        boolean prefixed = referenceId.startsWith(PREFIX);
        String version1Id = prefixed ? referenceId.substring(PREFIX.length()) : referenceId;
        GraphPath graphPath;
        try
        {
            graphPath = GraphPath.parse(version1Id);
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidReferenceIdException(referenceId, e);
        }
        if (prefixed == isElementId(graphPath))
        {
            // An element's id has no prefix, and every other id has one
            throw new InvalidReferenceIdException(referenceId);
        }
        return version1Id;
    }

    private static boolean isElementId(GraphPath graphPath)
    {
        return graphPath.getEdgeCount() == 0;
    }
}
