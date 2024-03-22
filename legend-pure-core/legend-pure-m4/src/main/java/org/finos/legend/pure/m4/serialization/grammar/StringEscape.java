// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m4.serialization.grammar;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.commons.lang3.text.translate.OctalUnescaper;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Objects;

public class StringEscape
{
    private static final CharSequenceTranslator ESCAPE_PURE = new AggregateTranslator(
            newTranslator(new String[][]{{"'", "\\'"}, {"\\", "\\\\"}}, EntityArrays.JAVA_CTRL_CHARS_ESCAPE()),
            JavaUnicodeEscaper.outsideOf(32, 0x7f),
            new OctalUnescaper());
    private static final CharSequenceTranslator UNESCAPE_PURE = new AggregateTranslator(
            new OctalUnescaper(),
            new UnicodeUnescaper(),
            newTranslator(new String[][]{{"\\\\", "\\"}, {"\\'", "'"}, {"\\", ""}}, EntityArrays.JAVA_CTRL_CHARS_UNESCAPE()));

    public static String escape(String string)
    {
        return ESCAPE_PURE.translate(string);
    }

    public static <T extends Appendable> T escape(T appendable, String string)
    {
        try (Writer writer = asWriter(appendable))
        {
            ESCAPE_PURE.translate(string, writer);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return appendable;
    }

    public static String unescape(String string)
    {
        return UNESCAPE_PURE.translate(string);
    }

    public static <T extends Appendable> T unescape(T appendable, String string)
    {
        try (Writer writer = asWriter(appendable))
        {
            UNESCAPE_PURE.translate(string, writer);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return appendable;
    }

    private static CharSequenceTranslator newTranslator(String[][] table1, String[][] table2)
    {
        String[][] combined = new String[table1.length + table2.length][];
        System.arraycopy(table1, 0, combined, 0, table1.length);
        System.arraycopy(table2, 0, combined, table1.length, table2.length);
        return new LookupTranslator(combined);
    }

    private static Writer asWriter(Appendable appendable)
    {
        if (appendable instanceof Writer)
        {
            return (Writer) appendable;
        }

        return new Writer()
        {
            @Override
            public void write(int c) throws IOException
            {
                appendable.append((char) c);
            }

            @Override
            public void write(char[] cbuf) throws IOException
            {
                append(CharBuffer.wrap(cbuf));
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException
            {
                append(CharBuffer.wrap(cbuf, off, len));
            }

            @Override
            public void write(String str) throws IOException
            {
                appendable.append(Objects.requireNonNull(str));
            }

            @Override
            public void write(String str, int off, int len) throws IOException
            {
                appendable.append(Objects.requireNonNull(str), off, len);
            }

            @Override
            public Writer append(CharSequence csq) throws IOException
            {
                appendable.append(csq);
                return this;
            }

            @Override
            public Writer append(CharSequence csq, int start, int end) throws IOException
            {
                appendable.append(csq, start, end);
                return this;
            }

            @Override
            public void flush() throws IOException
            {
                if (appendable instanceof Flushable)
                {
                    ((Flushable) appendable).flush();
                }
            }
            
            @Override
            public void close()
            {
                // Do not close the appendable
            }
        };
    }
}
