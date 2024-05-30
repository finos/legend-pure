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

package org.finos.legend.pure.m3.serialization.compiler.strings;

import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.serialization.compiler.ExtensibleSerializer;
import org.finos.legend.pure.m3.serialization.compiler.strings.v0.StringIndexerV0;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.DelegatingReader;
import org.finos.legend.pure.m4.serialization.binary.DelegatingWriter;

import java.util.Arrays;

public class StringIndexer extends ExtensibleSerializer<StringIndexerExtension>
{
    private StringIndexer(Iterable<? extends StringIndexerExtension> extensions, int defaultVersion)
    {
        super(extensions, defaultVersion);
    }

    public Writer writeStringIndex(Writer writer, Iterable<String> strings)
    {
        return writeStringIndex(writer, strings, getDefaultExtension());
    }

    public Writer writeStringIndex(Writer writer, Iterable<String> strings, int version)
    {
        return writeStringIndex(writer, strings, getExtension(version));
    }

    private Writer writeStringIndex(Writer writer, Iterable<String> strings, StringIndexerExtension extension)
    {
        writer.writeInt(extension.version());
        StringWriter stringWriter = extension.writeStringIndex(writer, strings);
        return new DelegatingWriter(writer)
        {
            @Override
            public void writeString(String string)
            {
                stringWriter.writeString(this.delegate, string);
            }

            @Override
            public void writeStringArray(String[] strings)
            {
                stringWriter.writeStringArray(this.delegate, strings);
            }
        };
    }

    public Reader readStringIndex(Reader reader)
    {
        int version = reader.readInt();
        StringIndexerExtension extension = getExtension(version);
        StringReader stringReader = extension.readStringIndex(reader);
        return new DelegatingReader(reader)
        {
            @Override
            public String readString()
            {
                return stringReader.readString(this.delegate);
            }

            @Override
            public void skipString()
            {
                stringReader.skipString(this.delegate);
            }

            @Override
            public String[] readStringArray()
            {
                return stringReader.readStringArray(this.delegate);
            }

            @Override
            public void skipStringArray()
            {
                stringReader.skipStringArray(this.delegate);
            }
        };
    }

    public static StringIndexer nullStringIndexer()
    {
        StringIndexerExtension v0 = new StringIndexerV0();
        return new StringIndexer(IntObjectMaps.immutable.with(v0.version(), v0), v0.version());
    }

    public static StringIndexer defaultStringIndexer()
    {
        return StringIndexer.builder().withLoadedExtensions().build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<StringIndexerExtension, StringIndexer>
    {
        private Builder()
        {
        }

        public Builder withExtension(StringIndexerExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends StringIndexerExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withExtensions(StringIndexerExtension... extensions)
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

        @Override
        protected StringIndexer build(Iterable<StringIndexerExtension> extensions, int defaultVersion)
        {
            return new StringIndexer(extensions, defaultVersion);
        }

        @Override
        protected Class<StringIndexerExtension> getExtensionClass()
        {
            return StringIndexerExtension.class;
        }
    }
}
