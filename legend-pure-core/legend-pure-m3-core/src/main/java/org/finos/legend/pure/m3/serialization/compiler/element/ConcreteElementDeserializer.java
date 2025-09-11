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

import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.serialization.Reader;

import java.util.Arrays;

public class ConcreteElementDeserializer extends BaseConcreteElementSerializer
{
    private ConcreteElementDeserializer(Iterable<? extends ConcreteElementSerializerExtension> extensions, int defaultVersion, StringIndexer stringIndexer)
    {
        super(extensions, defaultVersion, stringIndexer);
    }

    public DeserializedConcreteElement deserialize(Reader reader)
    {
        long signature = reader.readLong();
        if (signature != PURE_ELEMENT_SIGNATURE)
        {
            throw new IllegalArgumentException("Invalid file format: not a Legend concrete element file");
        }
        int version = reader.readInt();
        ConcreteElementSerializerExtension extension = getExtension(version);
        int referenceIdVersion = reader.readInt();
        return extension.deserialize(reader, this.stringIndexer, referenceIdVersion);
    }

    public static Builder builder()
    {
        return new Builder();
    }
    
    public static class Builder extends AbstractBuilder<ConcreteElementDeserializer>
    {
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
            setStringIndexer(stringIndexer);
            return this;
        }

        @Override
        protected ConcreteElementDeserializer build(Iterable<ConcreteElementSerializerExtension> extensions, int defaultVersion)
        {
            return new ConcreteElementDeserializer(extensions, defaultVersion, resolveStringIndexer());
        }
    }
}
