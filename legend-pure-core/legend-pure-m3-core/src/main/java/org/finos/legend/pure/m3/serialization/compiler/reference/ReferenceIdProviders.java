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

import org.finos.legend.pure.m3.navigation.ProcessorSupport;

import java.util.Objects;

public class ReferenceIdProviders extends ReferenceIds
{
    private final ProcessorSupport processorSupport;

    private ReferenceIdProviders(ExtensionManager extensionManager, ProcessorSupport processorSupport)
    {
        super(extensionManager);
        this.processorSupport = processorSupport;
    }

    public ReferenceIdProvider provider(int version)
    {
        return this.extensionManager.getExtensionCache(version).provider(this.processorSupport);
    }

    public ReferenceIdProvider provider(Integer version)
    {
        return this.extensionManager.getExtensionCache(version).provider(this.processorSupport);
    }

    public ReferenceIdProvider provider()
    {
        return this.extensionManager.getDefaultExtensionCache().provider(this.processorSupport);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<ReferenceIdProviders>
    {
        private ProcessorSupport processorSupport;

        private Builder()
        {
        }

        public Builder withExtension(ReferenceIdExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withAvailableExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withAvailableExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(Integer defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        public void setProcessorSupport(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        public Builder withProcessorSupport(ProcessorSupport processorSupport)
        {
            setProcessorSupport(processorSupport);
            return this;
        }

        @Override
        ReferenceIdProviders build(ExtensionManager extensionManager)
        {
            return new ReferenceIdProviders(extensionManager, Objects.requireNonNull(this.processorSupport, "processor support is required"));
        }
    }
}
