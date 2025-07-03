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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class IdBuilder
{
    private final ProcessorSupport processorSupport;
    private final String defaultIdPrefix;
    private final boolean allowNonReferenceIds;
    private volatile ReferenceIdProvider idProvider;

    private IdBuilder(ProcessorSupport processorSupport, String defaultIdPrefix, boolean allowNonReferenceIds)
    {
        this.processorSupport = processorSupport;
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
        return (this.defaultIdPrefix == null) ? Integer.toString(syntheticId) : (this.defaultIdPrefix + syntheticId);
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

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        return newIdBuilder(processorSupport, defaultIdPrefix, true);
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport, boolean allowNonReferenceIds)
    {
        return newIdBuilder(processorSupport, null, allowNonReferenceIds);
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport)
    {
        return newIdBuilder(processorSupport, null, true);
    }

    private static IdBuilder newIdBuilder(ProcessorSupport processorSupport, String defaultIdPrefix, boolean allowNonReferenceIds)
    {
        return new IdBuilder(Objects.requireNonNull(processorSupport), defaultIdPrefix, allowNonReferenceIds);
    }

    public static String sourceToId(SourceInformation sourceInformation)
    {
        String sourceId = sourceInformation.getSourceId();
        if (Source.isInMemory(sourceId))
        {
            return CodeStorageTools.hasPureFileExtension(sourceId) ? sourceId.substring(0, sourceId.length() - RepositoryCodeStorage.PURE_FILE_EXTENSION.length()) : sourceId;
        }

        int endIndex = CodeStorageTools.hasPureFileExtension(sourceId) ? (sourceId.length() - RepositoryCodeStorage.PURE_FILE_EXTENSION.length()) : sourceId.length();
        return sourceId.substring(1, endIndex).replace('/', '_');
    }
}
