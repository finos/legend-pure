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

package org.finos.legend.pure.m4.serialization.grammar;

import org.junit.Assert;
import org.junit.Test;

public class TestMultilineTextLayout
{
    // Characters that a Unicode-aware line break pattern matches but which JLS 3.4 does not accept as line
    // terminators: only LF, CR and CRLF end a line. All but NEXT_LINE are Character.isWhitespace.
    private static final String FORM_FEED = "\u000C";
    private static final String LINE_TABULATION = "\u000B";
    private static final String LINE_SEPARATOR = "\u2028";
    private static final String PARAGRAPH_SEPARATOR = "\u2029";
    private static final String NEXT_LINE = "\u0085";

    // Supplementary characters, each two chars wide, so a char index into a line is not a count of characters.
    private static final String AVESTAN_A = "\uD802\uDF00";      // U+10B00 AVESTAN LETTER A
    private static final String LINEAR_B_A = "\uD800\uDC00";     // U+10000 LINEAR B SYLLABLE B008 A
    private static final String LINEAR_A_AB001 = "\uD801\uDE00"; // U+10600 LINEAR A SIGN AB001

    // Space characters that Character.isWhitespace accepts, making them incidental whitespace ...
    private static final String IDEOGRAPHIC_SPACE = "\u3000";
    private static final String EN_QUAD = "\u2000";
    private static final String OGHAM_SPACE_MARK = "\u1680";

    // ... and space-like characters it rejects, making them content.
    private static final String NO_BREAK_SPACE = "\u00A0";
    private static final String NARROW_NO_BREAK_SPACE = "\u202F";
    private static final String FIGURE_SPACE = "\u2007";
    private static final String ZERO_WIDTH_SPACE = "\u200B";
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    @Test
    public void testLayoutRejectsMissingDelimiters()
    {
        // too short to hold both delimiters, even where what is there looks like a delimiter
        assertInvalid("");
        assertInvalid("'");
        assertInvalid("''");
        assertInvalid("'''");
        assertInvalid("''''");
        assertInvalid("'''''");
        assertInvalid("hello");

        // no opening delimiter
        assertInvalid("hello'''");
        assertInvalid("''hello\n'''");
        assertInvalid("'' '\nhello\n'''");

        // no closing delimiter
        assertInvalid("'''\nhello");
        assertInvalid("'''\nhello''");
        assertInvalid("'''\nhello''' ");
    }

    @Test
    public void testLayoutRejectsContentOnOpeningDelimiterLine()
    {
        // the opening delimiter must be followed by a line terminator, so nothing but whitespace may share its line
        assertInvalid("'''abc'''");
        assertInvalid("'''abc\n'''");
        assertInvalid("'''  abc\n'''");
        assertInvalid("'''  abc\n  def\n  '''");
        assertInvalid("''''\n'''");
        assertInvalid("''' " + NEXT_LINE + "\n'''");
        assertInvalid("''' " + NO_BREAK_SPACE + "\n'''");
        assertInvalid("''' " + AVESTAN_A + "\n'''");
    }

    @Test
    public void testLayoutSimple()
    {
        assertLayout("line1\nline2", "'''\nline1\nline2'''");
    }

    @Test
    public void testLayoutWithTrailingNewline()
    {
        assertLayout("line1\nline2\n", "'''\nline1\nline2\n'''");
    }

    @Test
    public void testLayoutIndentationStripped()
    {
        assertLayout("hello\nworld\n", "'''\n    hello\n    world\n    '''");
    }

    @Test
    public void testLayoutEmpty()
    {
        assertLayout("", "'''\n'''");
    }

    @Test
    public void testLayoutRejectsMissingLineTerminatorAfterOpeningDelimiter()
    {
        // The lexer guarantees a line terminator after the opening delimiter, so there is always a line of content
        // beyond the one the opening delimiter sits on - even when that content is empty.
        assertInvalid("''''''");
        assertInvalid("'''   '''");
        assertInvalid("'''\t'''");
    }

    @Test
    public void testLayoutDoesNotProcessEscapes()
    {
        // Layout shapes text and nothing else. A backslash sequence comes through as the characters written: the
        // string literal unescapes on top of this, and documentation deliberately does not.
        assertLayout("a\\tb\n", "'''\na\\tb\n'''");
        assertLayout("a\\nb\n", "'''\na\\nb\n'''");
        assertLayout("a\\rb\n", "'''\na\\rb\n'''");
        assertLayout("a\\bb\n", "'''\na\\bb\n'''");
        assertLayout("a\\fb\n", "'''\na\\fb\n'''");
        assertLayout("a\\\\b\n", "'''\na\\\\b\n'''");
        assertLayout("a\\'b\n", "'''\na\\'b\n'''");
        assertLayout("a\\101b\n", "'''\na\\101b\n'''");
        assertLayout("a\\u0041b\n", "'''\na\\u0041b\n'''");

        // including backslash sequences that are not escapes at all, which unescaping would mangle or reject
        assertLayout("C:\\users\\bin\n", "'''\nC:\\users\\bin\n'''");
        assertLayout("\\d+\n", "'''\n\\d+\n'''");
    }

    @Test
    public void testLayoutStripsWhitespaceAsWrittenNotAsEscaped()
    {
        // Trailing whitespace is judged on the text as written. A backslash sequence that would unescape to
        // whitespace is not whitespace here, so it survives the strip, and any later unescaping puts the trailing
        // whitespace back - which is how a line can end in whitespace at all.
        assertLayout("hello\\t\nworld\n", "'''\n    hello\\t\n    world\n    '''");
        assertLayout("hello\\n\n", "'''\n    hello\\n\n    '''");
        assertLayout("hello\\u0020\n", "'''\n    hello\\u0020\n    '''");
    }

    @Test
    public void testLayoutWithSupplementaryCharacters()
    {
        assertLayout("a" + AVESTAN_A + "b\n", "'''\n    a" + AVESTAN_A + "b\n    '''");
        assertLayout(AVESTAN_A + "hello\n", "'''\n" + AVESTAN_A + "hello\n    '''");
    }

    @Test
    public void testLayoutSupplementaryCharacterAtEndOfLine()
    {
        // Stripping trailing whitespace must not cut the last character in half when it is two chars wide.
        assertLayout("hello" + AVESTAN_A + "\n", "'''\n    hello" + AVESTAN_A + "\n    '''");
        assertLayout("hello" + AVESTAN_A + "\n", "'''\n    hello" + AVESTAN_A + "   \n    '''");
        assertLayout(AVESTAN_A + "\n", "'''\n    " + AVESTAN_A + "\n    '''");
        assertLayout("a" + LINEAR_B_A + "\nb" + LINEAR_A_AB001 + "\n",
                "'''\n    a" + LINEAR_B_A + "\n    b" + LINEAR_A_AB001 + "\n    '''");

        // including the last line, which has no terminator after it
        assertLayout("hello\nworld" + AVESTAN_A, "'''\n    hello\n    world" + AVESTAN_A + "'''");
    }

    @Test
    public void testLayoutIndentationNeverSplitsASupplementaryCharacter()
    {
        // Indentation is only ever whitespace and no whitespace character is supplementary, so the common indent
        // always falls on a character boundary - even where a shallower line drags it to the left.
        assertLayout(AVESTAN_A + "x\n    y\n", "'''\n  " + AVESTAN_A + "x\n      y\n    '''");
        assertLayout("x\n  " + AVESTAN_A + "y\n", "'''\n    x\n      " + AVESTAN_A + "y\n    '''");
    }

    @Test
    public void testLayoutLeavesSupplementaryCharacterEscapesAsText()
    {
        // A surrogate pair written as two escapes is twelve chars of ordinary text here. It is measured and carried
        // as that text, and only becomes one character once something later unescapes it.
        assertLayout("a\\uD802\\uDF00b\n", "'''\n    a\\uD802\\uDF00b\n    '''");
        assertLayout("hello\\uD802\\uDF00\n", "'''\n    hello\\uD802\\uDF00\n    '''");
        assertLayout("hello\\uD802\n", "'''\n    hello\\uD802\n    '''");
    }

    @Test
    public void testLayoutUnicodeSpaceAsIndentation()
    {
        // Character.isWhitespace accepts these, so they behave exactly as a space or a tab does.
        assertLayout("hello\n", "'''\n" + IDEOGRAPHIC_SPACE + IDEOGRAPHIC_SPACE + "hello\n" + IDEOGRAPHIC_SPACE + IDEOGRAPHIC_SPACE + "'''");
        assertLayout("hello\n", "'''\n" + EN_QUAD + EN_QUAD + "hello\n" + EN_QUAD + EN_QUAD + "'''");
        assertLayout("hello\n", "'''\n" + OGHAM_SPACE_MARK + OGHAM_SPACE_MARK + "hello\n" + OGHAM_SPACE_MARK + OGHAM_SPACE_MARK + "'''");
        assertLayout("hello\n", "'''\n    hello" + IDEOGRAPHIC_SPACE + "\n    '''");
    }

    @Test
    public void testLayoutNonBreakingAndZeroWidthSpacesAreContent()
    {
        // Character.isWhitespace rejects the non-breaking and zero width spaces, so they are never stripped and
        // never count as indentation: a line led by one of them is not indented at all.
        assertLayout(NO_BREAK_SPACE + NO_BREAK_SPACE + "hello\n", "'''\n" + NO_BREAK_SPACE + NO_BREAK_SPACE + "hello\n    '''");
        assertLayout("hello" + NO_BREAK_SPACE + "\n", "'''\n    hello" + NO_BREAK_SPACE + "\n    '''");
        assertLayout("hello" + NARROW_NO_BREAK_SPACE + "\n", "'''\n    hello" + NARROW_NO_BREAK_SPACE + "\n    '''");
        assertLayout("hello" + FIGURE_SPACE + "\n", "'''\n    hello" + FIGURE_SPACE + "\n    '''");
        assertLayout("hello" + ZERO_WIDTH_SPACE + "\n", "'''\n    hello" + ZERO_WIDTH_SPACE + "\n    '''");
        assertLayout("hello" + BYTE_ORDER_MARK + "\n", "'''\n    hello" + BYTE_ORDER_MARK + "\n    '''");
    }

    @Test
    public void testLayoutStripsTrailingWhitespace()
    {
        assertLayout("hello\nworld\n", "'''\n    hello   \n    world  \n    '''");
        assertLayout("hello\n", "'''\n    hello\t\n    '''");
    }

    @Test
    public void testLayoutWithBlankLine()
    {
        // A blank line does not contribute to the common indentation, and is emptied out whatever its width.
        assertLayout("hello\n\nworld\n", "'''\n    hello\n\n    world\n    '''");
        assertLayout("hello\n\nworld\n", "'''\n    hello\n  \n    world\n    '''");
        assertLayout("hello\n\nworld\n", "'''\n    hello\n      \n    world\n    '''");
    }

    @Test
    public void testLayoutWithLeadingBlankLine()
    {
        assertLayout("\nhello\n", "'''\n\n    hello\n    '''");
    }

    @Test
    public void testLayoutWithTrailingBlankLine()
    {
        assertLayout("hello\n\n", "'''\n    hello\n\n    '''");
    }

    @Test
    public void testLayoutOfBlankLinesOnly()
    {
        assertLayout("\n", "'''\n\n'''");
        assertLayout("\n", "'''\n    \n    '''");
        assertLayout("\n", "'''\n        \n    '''");
    }

    @Test
    public void testLayoutClosingDelimiterSetsIndentationFloor()
    {
        // The closing delimiter line counts towards the common indentation even though it is blank, so a closing
        // delimiter to the left of the content leaves that content indented.
        assertLayout("    hello\n", "'''\n        hello\n    '''");
        assertLayout("    hello\n", "'''\n    hello\n'''");

        // A closing delimiter to the right of the content does not over-strip it.
        assertLayout("hello\n", "'''\n    hello\n        '''");
    }

    @Test
    public void testLayoutClosingDelimiterOnContentLine()
    {
        // With no line terminator before the closing delimiter there is no trailing newline, and the last line is
        // an ordinary non-blank line for the purpose of computing the common indentation: what counts is its
        // leading whitespace, not its length.
        assertLayout("hello\nworld", "'''\n    hello\n    world'''");
        assertLayout("    hello\nworld", "'''\n        hello\n    world'''");

        // where it is also the only line of content there is nothing else to take the minimum with
        assertLayout("hello", "'''\nhello'''");
        assertLayout("hello", "'''\n    hello'''");

        // and a blank line above it contributes nothing, so it still sets the indentation on its own
        assertLayout("\nworld", "'''\n\n    world'''");
    }

    @Test
    public void testLayoutIndentationCountedInChars()
    {
        // Tabs are not expanded: one tab is one char of indentation, and a tab and a space are interchangeable.
        assertLayout("hello\nworld\n", "'''\n\thello\n\tworld\n\t'''");
        assertLayout("hello\nworld\n", "'''\n\t hello\n \tworld\n    '''");
    }

    @Test
    public void testLayoutIndentationIsTheMinimumAcrossLines()
    {
        assertLayout("    a\nb\n        c\n", "'''\n        a\n    b\n            c\n    '''");
        assertLayout("a\nb\n", "'''\na\nb\n'''");
    }

    @Test
    public void testLayoutNormalizesLineTerminators()
    {
        assertLayout("hello\nworld\n", "'''\n    hello\r\n    world\r\n    '''");
        assertLayout("hello\nworld\n", "'''\n    hello\r    world\r    '''");
        assertLayout("a\nb\nc\n", "'''\n    a\r\n    b\r    c\n    '''");

        // CRLF is one line terminator, not two
        assertLayout("a\n\nb\n", "'''\n    a\r\n\r\n    b\n    '''");

        // the opening delimiter line may end with any of the three, though the lexer only ever produces LF or CRLF
        assertLayout("hello\n", "'''\r    hello\n    '''");
        assertLayout("hello\n", "'''\r\n    hello\n    '''");
    }

    @Test
    public void testLayoutNonTerminatorWhitespaceDoesNotBreakALine()
    {
        // Whitespace other than a line terminator stays in the content, and the text around it stays on one line -
        // so it takes no part in working out where lines begin, and cannot pull the common indentation down with it.
        assertLayout("hello" + FORM_FEED + "world\n", "'''\n    hello" + FORM_FEED + "world\n    '''");
        assertLayout("hello" + LINE_TABULATION + "world\n", "'''\n    hello" + LINE_TABULATION + "world\n    '''");
        assertLayout("hello" + LINE_SEPARATOR + "world\n", "'''\n    hello" + LINE_SEPARATOR + "world\n    '''");
        assertLayout("hello" + PARAGRAPH_SEPARATOR + "world\n", "'''\n    hello" + PARAGRAPH_SEPARATOR + "world\n    '''");
        assertLayout("hello" + NEXT_LINE + "world\n", "'''\n    hello" + NEXT_LINE + "world\n    '''");
    }

    @Test
    public void testLayoutNonTerminatorWhitespaceIsStillWhitespace()
    {
        // It is stripped from the end of a line ...
        assertLayout("hello\n", "'''\n    hello" + FORM_FEED + "\n    '''");
        assertLayout("hello\n", "'''\n    hello" + LINE_TABULATION + "\n    '''");
        assertLayout("hello\n", "'''\n    hello" + LINE_SEPARATOR + "\n    '''");
        assertLayout("hello\n", "'''\n    hello" + PARAGRAPH_SEPARATOR + "\n    '''");

        // ... counts towards the indentation of a line ...
        assertLayout("hello\n", "'''\n  " + FORM_FEED + " hello\n    '''");

        // ... and leaves a line blank when it is all that line holds
        assertLayout("hello\n\nworld\n", "'''\n    hello\n  " + FORM_FEED + "\n    world\n    '''");
    }

    @Test
    public void testLayoutNextLineIsOrdinaryContent()
    {
        // Alone among these, U+0085 NEXT LINE is not Character.isWhitespace, so it is never stripped, it does not
        // count as indentation, and it makes an otherwise blank line significant.
        assertLayout("hello" + NEXT_LINE + "\n", "'''\n    hello" + NEXT_LINE + "\n    '''");
        assertLayout(NEXT_LINE + " hello\n", "'''\n  " + NEXT_LINE + " hello\n    '''");
        assertLayout("  hello\n" + NEXT_LINE + "\n  world\n", "'''\n    hello\n  " + NEXT_LINE + "\n    world\n    '''");
    }

    @Test
    public void testLayoutWithQuotes()
    {
        assertLayout("it's a \"test\" and ''\n", "'''\n    it's a \"test\" and ''\n    '''");
        assertLayout("hello'\n", "'''\n    hello'\n    '''");
        assertLayout("hello\\'\n", "'''\n    hello\\'\n    '''");
    }

    @Test
    public void testLayoutWhitespaceAfterOpeningDelimiter()
    {
        assertLayout("hello\n", "'''   \n    hello\n    '''");
        assertLayout("hello\n", "'''\t\n    hello\n    '''");
        assertLayout("hello\n", "'''  \r\n    hello\n    '''");

        // any whitespace will do here, though the lexer only admits spaces and tabs
        assertLayout("hello\n", "'''" + IDEOGRAPHIC_SPACE + "\n    hello\n    '''");
    }

    private static void assertLayout(String expected, String rawTokenText)
    {
        Assert.assertEquals(expected, MultilineTextLayout.layout(rawTokenText));
    }

    private static void assertInvalid(String rawTokenText)
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> MultilineTextLayout.layout(rawTokenText));
        Assert.assertEquals("Invalid multi-line string: " + rawTokenText, e.getMessage());
    }
}
