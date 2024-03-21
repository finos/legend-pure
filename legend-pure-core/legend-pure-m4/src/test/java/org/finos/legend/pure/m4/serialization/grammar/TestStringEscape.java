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
}
