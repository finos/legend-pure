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

package org.finos.legend.pure.m3.serialization.compiler.strings.v2;

import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexerExtension;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringReader;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringWriter;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

public class StringIndexerV2 implements StringIndexerExtension
{
    @Override
    public int version()
    {
        return 2;
    }

    @Override
    public StringWriter writeStringIndex(Writer writer, Iterable<String> strings)
    {
        return StringWriterV2.writeStringIndex(writer, strings);
    }

    @Override
    public StringReader readStringIndex(Reader reader)
    {
        return StringReaderV2.readStringIndex(reader);
    }
}
