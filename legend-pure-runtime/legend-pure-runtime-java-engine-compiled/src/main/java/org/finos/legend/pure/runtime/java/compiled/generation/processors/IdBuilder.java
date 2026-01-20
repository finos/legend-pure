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
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public abstract class IdBuilder
{
    private static final boolean DEFAULT_BUILDER_LEGACY = true;

    public abstract String buildId(CoreInstance instance);

    // Builder

    public abstract static class Builder
    {
        protected final ProcessorSupport processorSupport;
        protected String defaultIdPrefix;

        protected Builder(ProcessorSupport processorSupport)
        {
            this.processorSupport = Objects.requireNonNull(processorSupport, "processorSupport may not be null");
        }

        /**
         * Set the optional default id prefix. If non-null, the default id function will use this as the prefix for
         * all ids it generates.
         *
         * @param prefix default id prefix
         */
        public void setDefaultIdPrefix(String prefix)
        {
            this.defaultIdPrefix = prefix;
        }

        /**
         * Set the optional default id prefix. If non-null, the default id function will use this as the prefix for
         * all ids it generates.
         *
         * @param prefix default id prefix
         */
        public Builder withDefaultIdPrefix(String prefix)
        {
            setDefaultIdPrefix(prefix);
            return this;
        }

        /**
         * Build the {@linkplain IdBuilder}.
         *
         * @return {@linkplain IdBuilder}
         */
        public abstract IdBuilder build();
    }

    public static class LegacyBuilder extends Builder
    {
        private LegacyBuilder(ProcessorSupport processorSupport)
        {
            super(processorSupport);
        }

        @Override
        public IdBuilder build()
        {
            return new LegacyIdBuilder(this.defaultIdPrefix, this.processorSupport);
        }
    }

    public static class ReferenceIdBuilder extends Builder
    {
        private boolean allowNonReferenceIds = true;

        private ReferenceIdBuilder(ProcessorSupport processorSupport)
        {
            super(processorSupport);
        }

        public void setAllowNonReferenceIds(boolean allowNonReferenceIds)
        {
            this.allowNonReferenceIds = allowNonReferenceIds;
        }

        public void allowNonReferenceIds()
        {
            setAllowNonReferenceIds(true);
        }

        public void disallowNonReferenceIds()
        {
            setAllowNonReferenceIds(false);
        }

        public ReferenceIdBuilder withNonReferenceIdsAllowed(boolean allowNonReferenceIds)
        {
            setAllowNonReferenceIds(allowNonReferenceIds);
            return this;
        }

        public ReferenceIdBuilder withNonReferenceIdsAllowed()
        {
            return withNonReferenceIdsAllowed(true);
        }

        public ReferenceIdBuilder withNonReferenceIdsDisallowed()
        {
            return withNonReferenceIdsAllowed(false);
        }

        @Override
        public IdBuilder build()
        {
            return new ReferenceIdV1IdBuilder(this.processorSupport, this.defaultIdPrefix, this.allowNonReferenceIds);
        }
    }

    public static Builder builder(ProcessorSupport processorSupport)
    {
        return builder(processorSupport, DEFAULT_BUILDER_LEGACY);
    }

    public static Builder builder(ProcessorSupport processorSupport, boolean legacy)
    {
        return legacy ? legacyBuilder(processorSupport) : referenceIdBuilder(processorSupport);
    }

    public static LegacyBuilder legacyBuilder(ProcessorSupport processorSupport)
    {
        return new LegacyBuilder(processorSupport);
    }

    public static ReferenceIdBuilder referenceIdBuilder(ProcessorSupport processorSupport)
    {
        return new ReferenceIdBuilder(processorSupport);
    }

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        return newIdBuilder(defaultIdPrefix, processorSupport, DEFAULT_BUILDER_LEGACY);
    }

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport, boolean legacy)
    {
        return builder(processorSupport, legacy).withDefaultIdPrefix(defaultIdPrefix).build();
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport)
    {
        return newIdBuilder(processorSupport, DEFAULT_BUILDER_LEGACY);
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport, boolean legacy)
    {
        return builder(processorSupport, legacy).build();
    }

    @Deprecated
    public static String buildId(CoreInstance coreInstance, ProcessorSupport processorSupport)
    {
        return builder(processorSupport).build().buildId(coreInstance);
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
