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

package org.finos.legend.pure.lsp;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Print streams whose text is read back, or by the client, as UTF-8 must also be written as UTF-8. A
 * plain {@code new PrintStream(out)} encodes with the default charset, which before Java 18 is the
 * platform's: windows-1252 on a typical Windows machine, so non-ASCII output would be garbled there.
 */
public final class Utf8PrintStreams
{
    private Utf8PrintStreams()
    {
    }

    public static PrintStream create(OutputStream out)
    {
        try
        {
            return new PrintStream(out, true, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            // Every Java platform supports UTF-8
            throw new IllegalStateException(e);
        }
    }
}
