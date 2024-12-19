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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;

import java.util.Arrays;
import java.util.Objects;

public class ConcreteElementSerializer extends BaseConcreteElementSerializer
{
    private final ReferenceIdProviders referenceIdProviders;
    private final ProcessorSupport processorSupport;

    private ConcreteElementSerializer(Iterable<? extends ConcreteElementSerializerExtension> extensions, int defaultVersion, StringIndexer stringIndexer, ReferenceIdProviders referenceIdProviders, ProcessorSupport processorSupport)
    {
        super(extensions, defaultVersion, stringIndexer);
        this.referenceIdProviders = referenceIdProviders;
        this.processorSupport = processorSupport;
    }

    public void serialize(Writer writer, CoreInstance element)
    {
        serialize(writer, element, getDefaultExtension(), this.referenceIdProviders.provider());
    }

    public void serialize(Writer writer, CoreInstance element, int serializerVersion, int referenceIdVersion)
    {
        ConcreteElementSerializerExtension serializerExtension = getExtension(serializerVersion);
        ReferenceIdProvider referenceIdProvider = this.referenceIdProviders.provider(referenceIdVersion);
        serialize(writer, element, serializerExtension, referenceIdProvider);
    }

    private void serialize(Writer writer, CoreInstance element, ConcreteElementSerializerExtension serializerExtension, ReferenceIdProvider referenceIdProvider)
    {
        writer.writeLong(PURE_ELEMENT_SIGNATURE);
        writer.writeInt(serializerExtension.version());
        writer.writeInt(referenceIdProvider.version());
        serializerExtension.serialize(writer, element, this.stringIndexer, referenceIdProvider, this.processorSupport);
    }

    public ConcreteElementDeserializer getDeserializer()
    {
        return ConcreteElementDeserializer.builder().withExtensions(getExtensions()).withDefaultVersion(getDefaultVersion()).build();
    }

    public ReferenceIdProviders getReferenceIdProviders()
    {
        return this.referenceIdProviders;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(ProcessorSupport processorSupport)
    {
        return builder().withProcessorSupport(processorSupport);
    }

    public static class Builder extends AbstractBuilder<ConcreteElementSerializer>
    {
        private ReferenceIdProviders referenceIdProviders;
        private ProcessorSupport processorSupport;

        private Builder()
        {
        }

        public Builder withExtension(ConcreteElementSerializerExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends ConcreteElementSerializerExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withExtensions(ConcreteElementSerializerExtension... extensions)
        {
            return withExtensions(Arrays.asList(extensions));
        }

        public Builder withLoadedExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withLoadedExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(int defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        public Builder withStringIndexer(StringIndexer stringIndexer)
        {
            this.stringIndexer = stringIndexer;
            return this;
        }

        public Builder withProcessorSupport(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
            return this;
        }

        public Builder withReferenceIdProviders(ReferenceIdProviders referenceIdProviders)
        {
            this.referenceIdProviders = referenceIdProviders;
            return this;
        }

        @Override
        protected ConcreteElementSerializer build(Iterable<ConcreteElementSerializerExtension> extensions, int defaultVersion)
        {
            Objects.requireNonNull(this.processorSupport, "processor support is required");
            return new ConcreteElementSerializer(extensions, defaultVersion, resolveStringIndexer(), resolveReferenceIdProviders(), this.processorSupport);
        }

        private ReferenceIdProviders resolveReferenceIdProviders()
        {
            // If reference ids has not been specified, create one
            return (this.referenceIdProviders == null) ?
                   ReferenceIdProviders.builder().withProcessorSupport(this.processorSupport).withAvailableExtensions().build() :
                   this.referenceIdProviders;
        }
    }
}
