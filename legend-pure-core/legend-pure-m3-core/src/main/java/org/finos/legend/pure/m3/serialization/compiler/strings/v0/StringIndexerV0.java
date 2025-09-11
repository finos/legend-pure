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

package org.finos.legend.pure.m3.serialization.compiler.strings.v0;

import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexerExtension;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringReader;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringWriter;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

/**
 * Vacuous string indexer. Does no indexing at all.
 */
public class StringIndexerV0 implements StringIndexerExtension
{
    @Override
    public int version()
    {
        return 0;
    }

    @Override
    public StringWriter writeStringIndex(Writer writer, Iterable<String> strings)
    {
        return new StringWriter()
        {
            @Override
            public void writeString(Writer writer, String string)
            {
                writer.writeString(string);
            }

            @Override
            public void writeStringArray(Writer writer, String[] strings)
            {
                writer.writeStringArray(strings);
            }
        };
    }

    @Override
    public StringReader readStringIndex(Reader reader)
    {
        return new StringReader()
        {
            @Override
            public String readString(Reader reader)
            {
                return reader.readString();
            }

            @Override
            public void skipString(Reader reader)
            {
                reader.skipString();
            }

            @Override
            public String[] readStringArray(Reader reader)
            {
                return reader.readStringArray();
            }

            @Override
            public void skipStringArray(Reader reader)
            {
                reader.skipStringArray();
            }
        };
    }
}
