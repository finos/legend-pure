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

package org.finos.legend.pure.m3.serialization.compiler.reference.v1;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.graph.GraphPath;
import org.finos.legend.pure.m3.serialization.compiler.reference.InvalidReferenceIdException;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.UnresolvableReferenceIdException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReferenceIdResolverV1 implements ReferenceIdResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceIdResolverV1.class);

    private final ProcessorSupport processorSupport;

    ReferenceIdResolverV1(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public CoreInstance resolveReference(String referenceId)
    {
        long start = System.nanoTime();
        if (referenceId == null)
        {
            throw new InvalidReferenceIdException(null);
        }

        GraphPath graphPath;
        try
        {
            graphPath = GraphPath.parse(referenceId);
        }
        catch (Exception e)
        {
            long end = System.nanoTime();
            LOGGER.error("Error resolving {} in {}s", referenceId, (end - start) / 1_000_000_000.0, e);
            throw new InvalidReferenceIdException(referenceId, e);
        }

        try
        {
            CoreInstance result = graphPath.resolve(this.processorSupport);
            long end = System.nanoTime();
            LOGGER.debug("Resolved {} in {}s", referenceId, (end - start) / 1_000_000_000.0);
            return result;
        }
        catch (Exception e)
        {
            long end = System.nanoTime();
            LOGGER.error("Error resolving {} in {}s", referenceId, (end - start) / 1_000_000_000.0, e);
            throw new UnresolvableReferenceIdException(referenceId, e);
        }
    }
}
