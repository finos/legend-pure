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

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvisionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReferenceIdProviderV1 implements ReferenceIdProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceIdProviderV1.class);

    private final ContainingElementIndex containingElementIndex;
    private final ReferenceIdGenerator idGenerator;
    private final ConcurrentMutableMap<CoreInstance, MapIterable<CoreInstance, String>> idCache = ConcurrentHashMap.newMap();

    ReferenceIdProviderV1(ContainingElementIndex containingElementIndex, ReferenceIdGenerator idGenerator)
    {
        this.containingElementIndex = containingElementIndex;
        this.idGenerator = idGenerator;
    }

    ReferenceIdProviderV1(ProcessorSupport processorSupport)
    {
        this(ContainingElementIndex.builder(processorSupport).withAllElements().build(), new ReferenceIdGenerator(processorSupport));
    }

    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public boolean hasReferenceId(CoreInstance instance)
    {
        if (instance == null)
        {
            return false;
        }

        CoreInstance owner = findOwner(instance);
        return (owner != null) && hasReferenceId(instance, owner);
    }

    @Override
    public String getReferenceId(CoreInstance reference)
    {
        long start = System.nanoTime();
        try
        {
            String id = getReferenceId_internal(reference);
            long end = System.nanoTime();
            LOGGER.debug("Got reference id {} in {}s", id, (end - start) / 1_000_000_000.0);
            return id;
        }
        catch (Throwable t)
        {
            long end = System.nanoTime();
            LOGGER.error("Failed to get reference id for {} in {}s", reference, (end - start) / 1_000_000_000.0);
            throw t;
        }
    }

    private String getReferenceId_internal(CoreInstance reference)
    {
        if (reference == null)
        {
            throw new ReferenceIdProvisionException("Cannot provide reference id for null");
        }

        CoreInstance owner = findOwner(reference);
        if (owner == null)
        {
            StringBuilder builder = new StringBuilder("Cannot provide reference id for ");
            appendReferenceDescription(builder, reference).append(": cannot find containing element");
            throw new ReferenceIdProvisionException(builder.toString());
        }

        String id = getReferenceId(reference, owner);
        if (id == null)
        {
            throw new ReferenceIdProvisionException(appendReferenceDescription(new StringBuilder("Cannot provide reference id for "), reference, owner).toString());
        }
        return id;
    }

    private CoreInstance findOwner(CoreInstance reference)
    {
        try
        {
            return this.containingElementIndex.findContainingElement(reference);
        }
        catch (Exception e)
        {
            throw new ReferenceIdProvisionException(appendReferenceDescription(new StringBuilder("Error providing reference id for "), reference).toString(), e);
        }
    }

    private boolean hasReferenceId(CoreInstance reference, CoreInstance owner)
    {
        return getReferenceIds(reference, owner).containsKey(reference);
    }

    private String getReferenceId(CoreInstance reference, CoreInstance owner)
    {
        return getReferenceIds(reference, owner).get(reference);
    }

    private MapIterable<CoreInstance, String> getReferenceIds(CoreInstance reference, CoreInstance owner)
    {
        try
        {
            return this.idCache.getIfAbsentPutWithKey(owner, this.idGenerator::generateIdsForElement);
        }
        catch (Exception e)
        {
            throw new ReferenceIdProvisionException(appendReferenceDescription(new StringBuilder("Error providing reference id for "), reference, owner).toString(), e);
        }
    }

    private StringBuilder appendReferenceDescription(StringBuilder builder, CoreInstance reference)
    {
        if (reference == null)
        {
            return builder.append("null");
        }

        builder.append("instance ").append(reference);
        SourceInformation sourceInfo = reference.getSourceInformation();
        if (sourceInfo == null)
        {
            builder.append(" with no source information");
        }
        else
        {
            sourceInfo.appendMessage(builder.append(" at "));
        }
        return builder;
    }

    private StringBuilder appendReferenceDescription(StringBuilder builder, CoreInstance reference, CoreInstance owner)
    {
        appendReferenceDescription(builder, reference);
        PackageableElement.writeUserPathForPackageableElement(builder.append(" contained in "), owner);
        return builder;
    }
}
