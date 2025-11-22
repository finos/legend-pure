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

import org.finos.legend.pure.m3.serialization.compiler.ExtensibleSerializer;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;

import java.util.Objects;

abstract class BaseConcreteElementSerializer extends ExtensibleSerializer<ConcreteElementSerializerExtension>
{
    static final long PURE_ELEMENT_SIGNATURE = Long.parseLong("PureElement", 36);

    final StringIndexer stringIndexer;

    BaseConcreteElementSerializer(Iterable<? extends ConcreteElementSerializerExtension> extensions, int defaultVersion, StringIndexer stringIndexer)
    {
        super(extensions, defaultVersion);
        this.stringIndexer = Objects.requireNonNull(stringIndexer);
    }

    public abstract static class AbstractBuilder<T extends BaseConcreteElementSerializer> extends ExtensibleSerializer.AbstractBuilder<ConcreteElementSerializerExtension, T>
    {
        StringIndexer stringIndexer;

        AbstractBuilder()
        {
        }

        public void setStringIndexer(StringIndexer stringIndexer)
        {
            this.stringIndexer = stringIndexer;
        }

        public void clearStringIndexer()
        {
            setStringIndexer(null);
        }

        protected StringIndexer resolveStringIndexer()
        {
            // if string indexer has not been specified, use the default
            return (this.stringIndexer == null) ? StringIndexer.defaultStringIndexer() : this.stringIndexer;
        }

        @Override
        protected Class<ConcreteElementSerializerExtension> getExtensionClass()
        {
            return ConcreteElementSerializerExtension.class;
        }
    }
}
