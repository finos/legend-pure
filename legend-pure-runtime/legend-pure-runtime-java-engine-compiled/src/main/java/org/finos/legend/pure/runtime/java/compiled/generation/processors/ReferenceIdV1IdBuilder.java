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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Objects;

class ReferenceIdV1IdBuilder extends IdBuilder
{
    private final ProcessorSupport processorSupport;
    private final String defaultIdPrefix;
    private final boolean allowNonReferenceIds;
    private volatile ReferenceIdProvider idProvider;

    ReferenceIdV1IdBuilder(ProcessorSupport processorSupport, String defaultIdPrefix, boolean allowNonReferenceIds)
    {
        this.processorSupport = Objects.requireNonNull(processorSupport, "processorSupport may not be null");
        this.defaultIdPrefix = defaultIdPrefix;
        this.allowNonReferenceIds = allowNonReferenceIds;
    }

    public String buildId(CoreInstance instance)
    {
        ReferenceIdProvider provider = getIdProvider();
        if (!this.allowNonReferenceIds || provider.hasReferenceId(instance))
        {
            return provider.getReferenceId(instance);
        }

        int syntheticId = instance.getSyntheticId();
        return (this.defaultIdPrefix == null) ? Integer.toUnsignedString(syntheticId, 32) : (this.defaultIdPrefix + Integer.toUnsignedString(syntheticId, 32));
    }

    private ReferenceIdProvider getIdProvider()
    {
        ReferenceIdProvider local = this.idProvider;
        if (local == null)
        {
            synchronized (this)
            {
                if ((local = this.idProvider) == null)
                {
                    return this.idProvider = new ReferenceIdExtensionV1().newProvider(this.processorSupport);
                }
            }
        }
        return local;
    }
}
