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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Random;

public class TestStringEscape
{
    private static final String NO_ESCAPES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_+*/()[]{}<>-=#@$%^&!,.?~`:;|\"";

    @Test
    public void testEscape()
    {
        Assert.assertEquals(NO_ESCAPES, StringEscape.escape(NO_ESCAPES));

        Assert.assertEquals("\\'", StringEscape.escape("'"));
        Assert.assertEquals("\\n", StringEscape.escape("\n"));
        Assert.assertEquals("\\r", StringEscape.escape("\r"));
        Assert.assertEquals("\\t", StringEscape.escape("\t"));
        Assert.assertEquals("\\b", StringEscape.escape("\b"));
        Assert.assertEquals("\\f", StringEscape.escape("\f"));
        Assert.assertEquals("\\\\", StringEscape.escape("\\"));

        Assert.assertEquals("\\\\\\n", StringEscape.escape("\\\n"));
        Assert.assertEquals("\\\\\\'", StringEscape.escape("\\'"));

        Assert.assertEquals("The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\"", StringEscape.escape("The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\""));
    }

    @Test
    public void testEscapeEveryCodeUnit()
    {
        StringBuilder all = new StringBuilder(0x10000);
        StringBuilder allEscaped = new StringBuilder(6 * 0x10000);
        for (int i = 0; i <= 0xFFFF; i++)
        {
            char c = (char) i;
            String expected = expectedEscape(c);
            Assert.assertEquals(String.format(Locale.ROOT, "U+%04X", i), expected, StringEscape.escape(String.valueOf(c)));
            all.append(c);
            allEscaped.append(expected);
        }
        Assert.assertEquals(allEscaped.toString(), StringEscape.escape(all.toString()));
        Assert.assertEquals(allEscaped.toString(), StringEscape.escape(new StringBuilder(), all.toString()).toString());
        Assert.assertEquals(all.toString(), StringEscape.unescape(allEscaped.toString()));
    }

    @Test
    public void testEscapeSurrogates()
    {
        // U+10B00 AVESTAN LETTER A and U+10000 LINEAR B SYLLABLE B008 A take two escapes each
        Assert.assertEquals("\\uD802\\uDF00", StringEscape.escape("\uD802\uDF00"));
        Assert.assertEquals("a\\uD800\\uDC00b", StringEscape.escape("a\uD800\uDC00b"));

        // unpaired surrogates take one each
        Assert.assertEquals("\\uD802", StringEscape.escape("\uD802"));
        Assert.assertEquals("\\uDF00", StringEscape.escape("\uDF00"));
        Assert.assertEquals("\\uDF00\\uD802", StringEscape.escape("\uDF00\uD802"));
        Assert.assertEquals("a\\uD802", StringEscape.escape("a\uD802"));
        Assert.assertEquals("\\uDF00a", StringEscape.escape("\uDF00a"));
    }

    @Test
    public void testUnescape()
    {
        Assert.assertEquals(NO_ESCAPES, StringEscape.unescape(NO_ESCAPES));

        Assert.assertEquals("\n", StringEscape.unescape("\\n"));
        Assert.assertEquals("\r", StringEscape.unescape("\\r"));
        Assert.assertEquals("\t", StringEscape.unescape("\\t"));
        Assert.assertEquals("\b", StringEscape.unescape("\\b"));
        Assert.assertEquals("\f", StringEscape.unescape("\\f"));
        Assert.assertEquals("\\", StringEscape.unescape("\\\\"));
        Assert.assertEquals("'", StringEscape.unescape("\\'"));

        Assert.assertEquals("\\\"", StringEscape.unescape("\\\\\""));
        Assert.assertEquals("\\\n", StringEscape.unescape("\\\\\\n"));
        Assert.assertEquals("\\n", StringEscape.unescape("\\\\n"));

        Assert.assertEquals("a", StringEscape.unescape("\\a"));
        Assert.assertEquals("\"", StringEscape.unescape("\\\""));

        Assert.assertEquals("The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\"", StringEscape.unescape("The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\""));
    }

    @Test
    public void testUnescapeUnicode()
    {
        Assert.assertEquals("A", StringEscape.unescape("\\u0041"));
        Assert.assertEquals("'A'", StringEscape.unescape("'\\u0041'"));
        Assert.assertEquals("\u00E9", StringEscape.unescape("\\u00e9"));
        Assert.assertEquals("\u00E9", StringEscape.unescape("\\u00E9"));
        Assert.assertEquals("\uD802\uDF00", StringEscape.unescape("\\uD802\\uDF00"));
        Assert.assertEquals("\uD802", StringEscape.unescape("\\uD802"));
        Assert.assertEquals("\uDF00\uD802", StringEscape.unescape("\\uDF00\\uD802"));
    }

    @Test
    public void testUnescapeUnicodeVariants()
    {
        // more than one u, and a plus sign after them
        Assert.assertEquals("A", StringEscape.unescape("\\uuuu0041"));
        Assert.assertEquals("A", StringEscape.unescape("\\u+0041"));
        Assert.assertEquals("A", StringEscape.unescape("\\uu+0041"));

        // the four characters after them go to Integer.parseInt, which accepts a sign and digits outside ASCII
        Assert.assertEquals("\uFEDD", StringEscape.unescape("\\u-123"));
        Assert.assertEquals("\uF001", StringEscape.unescape("\\u-FFF"));
        Assert.assertEquals("\u0123", StringEscape.unescape("\\u++123"));
        Assert.assertEquals("A", StringEscape.unescape("\\u\u0660\u0660\u0664\u0661"));
    }

    @Test
    public void testUnescapeInvalidUnicode()
    {
        assertUnescapeFails("Less than 4 hex digits in unicode value: '\\u' due to end of CharSequence", "a\\u");
        assertUnescapeFails("Less than 4 hex digits in unicode value: '\\u41' due to end of CharSequence", "\\u41");
        assertUnescapeFails("Less than 4 hex digits in unicode value: '\\u41'' due to end of CharSequence", "'\\u41'");
        assertUnescapeFails("Less than 4 hex digits in unicode value: '\\u+-12' due to end of CharSequence", "\\u+-12");
        assertUnescapeFails("Unable to parse unicode value: 41zz", "\\u41zz");
        assertUnescapeFails("Unable to parse unicode value: 41 a", "'\\u41 abc'");
    }

    @Test
    public void testUnescapeOctal()
    {
        Assert.assertEquals("\u0000", StringEscape.unescape("\\0"));
        Assert.assertEquals("\u0007", StringEscape.unescape("\\7"));
        Assert.assertEquals("\u0001", StringEscape.unescape("\\01"));
        Assert.assertEquals("A", StringEscape.unescape("\\101"));
        Assert.assertEquals("\u00FF", StringEscape.unescape("\\377"));
        Assert.assertEquals("'\u0000'", StringEscape.unescape("'\\0'"));

        // a third digit only when the first is 0 to 3, and 8 and 9 are not octal digits
        Assert.assertEquals(" 0", StringEscape.unescape("\\400"));
        Assert.assertEquals("\u00FF7", StringEscape.unescape("\\3777"));
        Assert.assertEquals("\u00008", StringEscape.unescape("\\08"));
        Assert.assertEquals("8", StringEscape.unescape("\\8"));
        Assert.assertEquals("9", StringEscape.unescape("\\9"));
    }

    @Test
    public void testUnescapeOtherBackslashes()
    {
        // a backslash before any other character, or at the end, is dropped
        Assert.assertEquals("q", StringEscape.unescape("\\q"));
        Assert.assertEquals("a b", StringEscape.unescape("a\\ b"));
        Assert.assertEquals("", StringEscape.unescape("\\"));
        Assert.assertEquals("abc", StringEscape.unescape("abc\\"));
    }

    @Test
    public void testRoundTrip()
    {
        Assert.assertEquals(NO_ESCAPES, StringEscape.unescape(StringEscape.escape(NO_ESCAPES)));
        Assert.assertEquals(NO_ESCAPES, StringEscape.escape(StringEscape.unescape(NO_ESCAPES)));

        String quickBrownFox = "The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\"";
        Assert.assertEquals(quickBrownFox, StringEscape.unescape(StringEscape.escape(quickBrownFox)));

        String quickBrownFoxEscaped = "The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\"";
        Assert.assertEquals(quickBrownFoxEscaped, StringEscape.escape(StringEscape.unescape(quickBrownFoxEscaped)));

        String slashNewline = "\\\n";
        Assert.assertEquals(slashNewline, StringEscape.unescape(StringEscape.escape(slashNewline)));

        String slashNewlineEscaped = "\\\\\\n";
        Assert.assertEquals(slashNewlineEscaped, StringEscape.escape(StringEscape.unescape(slashNewlineEscaped)));

        String slashN = "\\n";
        Assert.assertEquals(slashN, StringEscape.unescape(StringEscape.escape(slashN)));

        String slashNEscaped = "\\\\n";
        Assert.assertEquals(slashNEscaped, StringEscape.escape(StringEscape.unescape(slashNEscaped)));

        Assert.assertEquals("a", StringEscape.escape(StringEscape.unescape("\\a")));
    }

    @Test
    public void testRoundTripRandom()
    {
        Random random = new Random(20261119L);
        for (int n = 0; n < 10_000; n++)
        {
            char[] chars = new char[random.nextInt(32)];
            for (int i = 0; i < chars.length; i++)
            {
                chars[i] = (char) random.nextInt(0x10000);
            }
            String string = new String(chars);
            Assert.assertEquals(string, StringEscape.unescape(StringEscape.escape(string)));
        }
    }

    @Test
    public void testAppendable()
    {
        StringBuilder builder = new StringBuilder("x");
        Assert.assertSame(builder, StringEscape.escape(builder, "'y'"));
        Assert.assertEquals("x\\'y\\'", builder.toString());
        Assert.assertSame(builder, StringEscape.unescape(builder, "\\tz"));
        Assert.assertEquals("x\\'y\\'\tz", builder.toString());
    }

    @Test
    public void testWriterIsNotClosed()
    {
        CloseRecordingWriter writer = new CloseRecordingWriter();
        StringEscape.escape(writer, "a'b");
        StringEscape.unescape(writer, "\\n");
        Assert.assertFalse(writer.closed);
        Assert.assertEquals("a\\'b\n", writer.toString());
    }

    @Test
    public void testNull()
    {
        Assert.assertNull(StringEscape.escape(null));
        Assert.assertNull(StringEscape.unescape(null));

        StringBuilder builder = new StringBuilder("x");
        Assert.assertSame(builder, StringEscape.escape(builder, null));
        Assert.assertSame(builder, StringEscape.unescape(builder, null));
        Assert.assertEquals("x", builder.toString());
    }

    @Test
    public void testIOException()
    {
        Appendable failing = new Appendable()
        {
            @Override
            public Appendable append(CharSequence csq) throws IOException
            {
                throw new IOException("cannot append");
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) throws IOException
            {
                throw new IOException("cannot append");
            }

            @Override
            public Appendable append(char c) throws IOException
            {
                throw new IOException("cannot append");
            }
        };
        UncheckedIOException escapeException = Assert.assertThrows(UncheckedIOException.class, () -> StringEscape.escape(failing, "a"));
        Assert.assertEquals("cannot append", escapeException.getCause().getMessage());
        UncheckedIOException unescapeException = Assert.assertThrows(UncheckedIOException.class, () -> StringEscape.unescape(failing, "a"));
        Assert.assertEquals("cannot append", unescapeException.getCause().getMessage());
    }

    private static void assertUnescapeFails(String expectedMessage, String string)
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> StringEscape.unescape(string));
        Assert.assertEquals(expectedMessage, e.getMessage());

        IllegalArgumentException appendableException = Assert.assertThrows(IllegalArgumentException.class, () -> StringEscape.unescape(new StringBuilder(), string));
        Assert.assertEquals(expectedMessage, appendableException.getMessage());
    }

    // the escaping rule, stated independently of StringEscape
    private static String expectedEscape(char c)
    {
        switch (c)
        {
            case '\'':
            {
                return "\\'";
            }
            case '\\':
            {
                return "\\\\";
            }
            case '\b':
            {
                return "\\b";
            }
            case '\t':
            {
                return "\\t";
            }
            case '\n':
            {
                return "\\n";
            }
            case '\f':
            {
                return "\\f";
            }
            case '\r':
            {
                return "\\r";
            }
            default:
            {
                return ((c < 0x20) || (c > 0x7F)) ? String.format(Locale.ROOT, "\\u%04X", (int) c) : String.valueOf(c);
            }
        }
    }

    private static class CloseRecordingWriter extends StringWriter
    {
        private boolean closed = false;

        @Override
        public void close()
        {
            this.closed = true;
        }
    }
}
