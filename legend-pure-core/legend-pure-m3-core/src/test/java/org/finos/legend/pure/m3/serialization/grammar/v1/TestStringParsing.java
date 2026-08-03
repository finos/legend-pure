// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Test;

public class TestStringParsing extends AbstractPrimitiveParsingTest
{
    // Characters that a Unicode-aware line break pattern matches but which JLS 3.4 does not accept as line
    // terminators: only LF, CR and CRLF end a line.
    private static final String FORM_FEED = "\u000C";
    private static final String LINE_SEPARATOR = "\u2028";
    private static final String NEXT_LINE = "\u0085";

    // A supplementary character, two chars wide.
    private static final String AVESTAN_A = "\uD802\uDF00"; // U+10B00 AVESTAN LETTER A

    @Test
    public void testSimpleString()
    {
        assertParsesTo("the quick brown fox jumps over the lazy dog", "'the quick brown fox jumps over the lazy dog'");
    }

    @Test
    public void testStringWithEscapedQuotes()
    {
        assertParsesTo("the quick brown 'fox' jumps over the lazy 'dog'", "'the quick brown \\'fox\\' jumps over the lazy \\'dog\\''");
    }

    @Test
    public void testStringWithDoubleQuotes()
    {
        assertParsesTo("the quick brown \"fox\" jumps over the lazy \"dog\"", "'the quick brown \"fox\" jumps over the lazy \"dog\"'");
    }

    @Test
    public void testStringWithNewline()
    {
        assertParsesTo("the quick brown fox\njumps over the lazy dog", "'the quick brown fox\\njumps over the lazy dog'");
    }

    @Test
    public void testStringWithEscapedSlash()
    {
        assertParsesTo("the quick brown fox \\ jumps over the lazy dog", "'the quick brown fox \\\\ jumps over the lazy dog'");
    }

    @Test
    public void testEscapedSlashThenQuoteThenTextThenQuote()
    {
        assertFailsToParse("'\\\\'outside quote'"); // '\\'outside quote'
    }

    @Test
    public void testStringWithUnescapedQuote()
    {
        assertFailsToParse("'''");
    }

    @Test
    public void testSimpleMultilineString()
    {
        assertParsesTo("line1\nline2", "'''\nline1\nline2'''");
    }

    @Test
    public void testMultilineStringWithTrailingNewline()
    {
        assertParsesTo("line1\nline2\n", "'''\nline1\nline2\n'''");
    }

    @Test
    public void testMultilineEmptyString()
    {
        assertParsesTo("", "'''\n'''");
    }

    @Test
    public void testMultilineStringIndentationStripped()
    {
        assertParsesTo("hello\nworld\n", "'''\n    hello\n    world\n    '''");
    }

    @Test
    public void testMultilineStringProcessesEscapes()
    {
        // The lexer hands over the raw token text, so escapes are still there to be processed - and are processed
        // exactly once.
        assertParsesTo("a\tb\n", "'''\na\\tb\n'''");
        assertParsesTo("a\nb\n", "'''\na\\nb\n'''");
        assertParsesTo("a\rb\n", "'''\na\\rb\n'''");
        assertParsesTo("a\bb\n", "'''\na\\bb\n'''");
        assertParsesTo("a\fb\n", "'''\na\\fb\n'''");
        assertParsesTo("a\\b\n", "'''\na\\\\b\n'''");
        assertParsesTo("a'b\n", "'''\na\\'b\n'''");
        assertParsesTo("aAb\n", "'''\na\\101b\n'''");
        assertParsesTo("aAb\n", "'''\na\\u0041b\n'''");
    }

    @Test
    public void testMultilineStringEscapesAreProcessedAfterLayout()
    {
        // Layout runs first and judges whitespace on the text as written, so a backslash sequence is not whitespace
        // and is not a line. An escape may therefore put back at the end of a line whitespace that could never have
        // survived being written literally ...
        assertParsesTo("hello\t\nworld\n", "'''\n    hello\\t\n    world\n    '''");
        assertParsesTo("hello \n", "'''\n    hello\\u0020\n    '''");

        // ... and a line it introduces arrives too late to take part in stripping the common indentation, so the
        // text after it keeps every space it was written with.
        assertParsesTo("a\n        b\n", "'''\n    a\\n        b\n    '''");
    }

    @Test
    public void testMultilineStringSupplementaryCharacterFromEscapes()
    {
        // A surrogate pair written as two escapes is twelve chars of text while the layout measures lines, and one
        // character afterwards - including at the end of a line, where the layout must not have split the text.
        assertParsesTo("a" + AVESTAN_A + "b\n", "'''\n    a\\uD802\\uDF00b\n    '''");
        assertParsesTo("hello" + AVESTAN_A + "\n", "'''\n    hello\\uD802\\uDF00\n    '''");
    }

    @Test
    public void testMultilineStringWithQuotes()
    {
        // The token ends at the first ''' after the opening delimiter line, so quotes short of three in a row are
        // ordinary content and need no escaping.
        assertParsesTo("it's a \"test\" and ''\n", "'''\n    it's a \"test\" and ''\n    '''");
        assertParsesTo("hello'\n", "'''\n    hello'\n    '''");
        assertParsesTo("hello'\n", "'''\n    hello\\'\n    '''");
    }

    @Test
    public void testMultilineStringWithSupplementaryCharacters()
    {
        // A supplementary character survives the trip from source text through the lexer to the parsed value.
        assertParsesTo("a" + AVESTAN_A + "b\n", "'''\n    a" + AVESTAN_A + "b\n    '''");
        assertParsesTo("hello" + AVESTAN_A + "\n", "'''\n    hello" + AVESTAN_A + "\n    '''");
    }

    @Test
    public void testMultilineStringLineTerminatorsReachTheParserIntact()
    {
        // Nothing upstream of the parser normalizes or re-wraps the content: a bare CR is still a line terminator
        // when it gets there ...
        assertParsesTo("hello\nworld\n", "'''\n    hello\r\n    world\r\n    '''");
        assertParsesTo("hello\nworld\n", "'''\n    hello\r    world\r    '''");
        assertParsesTo("a\n\nb\n", "'''\n    a\r\n\r\n    b\n    '''");

        // ... and whitespace that is not one of the three line terminators still is not one, so the text around it
        // stays on a single line and cannot pull the common indentation down with it.
        assertParsesTo("hello" + FORM_FEED + "world\n", "'''\n    hello" + FORM_FEED + "world\n    '''");
        assertParsesTo("hello" + LINE_SEPARATOR + "world\n", "'''\n    hello" + LINE_SEPARATOR + "world\n    '''");
        assertParsesTo("hello" + NEXT_LINE + "world\n", "'''\n    hello" + NEXT_LINE + "world\n    '''");
    }

    @Test
    public void testMultilineStringWhitespaceAfterOpeningDelimiter()
    {
        // Only spaces and tabs may sit between the opening delimiter and its line terminator.
        assertParsesTo("hello\n", "'''   \n    hello\n    '''");
        assertParsesTo("hello\n", "'''\t\n    hello\n    '''");
        assertParsesTo("hello\n", "'''  \r\n    hello\n    '''");
    }

    @Test
    public void testMultilineStringRequiresNewlineAfterOpeningDelimiter()
    {
        assertFailsToParse("'''abc'''");
        assertFailsToParse("'''  abc\n'''");
    }

    @Test
    public void testStringWithOnlyEscapedQuote()
    {
        assertParsesTo("'", "'\\''");
    }

    @Test
    public void testStringWithOnlyEscapedSlash()
    {
        assertParsesTo("\\", "'\\\\'");
    }

    @Test
    public void testStringWithEscapedSlashAndEscapedQuote()
    {
        assertParsesTo("\\'", "'\\\\\\''");
    }

    @Test
    public void testStringWithEscapedSlashAndUnescapedQuote()
    {
        assertFailsToParse("'\\\\''");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.STRING_TYPE_NAME;
    }
}
