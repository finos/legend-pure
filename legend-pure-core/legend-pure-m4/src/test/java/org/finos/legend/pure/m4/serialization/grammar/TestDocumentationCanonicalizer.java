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

/**
 * Canonicalization is the stable contract between the grammar and anything that consumes
 * documentation (display layers, IDE tooling, and any parallel implementation in another
 * grammar). Every case here is a contract statement, not an implementation detail.
 */
public class TestDocumentationCanonicalizer
{
    // A supplementary character, two chars wide, so a char index into a line is not a count of characters.
    private static final String AVESTAN_A = "\uD802\uDF00"; // U+10B00 AVESTAN LETTER A

    @Test
    public void testEmptyDocumentation()
    {
        assertCanonicalizesTo("", "'''\n'''");
    }

    @Test
    public void testWhitespaceOnlyContentIsEmpty()
    {
        assertCanonicalizesTo("", "'''\n   \n  \n'''");
    }

    @Test
    public void testSingleLine()
    {
        assertCanonicalizesTo("One line.", "'''\nOne line.\n'''");
    }

    @Test
    public void testIndentationIsRelativeToClosingDelimiter()
    {
        assertCanonicalizesTo("Text\n\n    code", "'''\n  Text\n\n      code\n  '''");
    }

    /**
     * Nothing is star-aware, so a Markdown bullet list is ordinary content. This is the headline
     * behavioural difference from Javadoc, and the reason the documentation literal needs no
     * all-or-nothing star-stripping rule at all.
     */
    @Test
    public void testMarkdownBulletListIsPreserved()
    {
        assertCanonicalizesTo("Options:\n* first\n* second",
                "'''\nOptions:\n* first\n* second\n'''");
    }

    @Test
    public void testLeadingStarsAreOrdinaryContent()
    {
        assertCanonicalizesTo("* starred\nplain", "'''\n* starred\nplain\n'''");
    }

    /**
     * The closing delimiter participates in the indent computation, so when it sits at column 0
     * nothing is stripped and all content indentation is preserved.
     */
    @Test
    public void testClosingDelimiterAtColumnZeroPreservesAllIndentation()
    {
        assertCanonicalizesTo("    indented", "'''\n    indented\n'''");
    }

    @Test
    public void testCarriageReturnsAreNormalized()
    {
        assertCanonicalizesTo("A\nB", "'''\r\nA\r\nB\r\n'''");
        assertCanonicalizesTo("A\nB", "'''\rA\rB\r'''");
    }

    @Test
    public void testTrailingWhitespaceIsStripped()
    {
        assertCanonicalizesTo("A\nB", "'''\nA   \nB\t\n'''");
    }

    /**
     * Dropping the surrounding blank lines is what makes documentation and the equivalent explicit
     * doc.doc tagged value hold the same string, however the author lays the literal out.
     */
    @Test
    public void testLeadingAndTrailingBlankLinesAreDropped()
    {
        assertCanonicalizesTo("A", "'''\n\n\nA\n\n\n'''");
    }

    @Test
    public void testInteriorBlankLinesArePreserved()
    {
        assertCanonicalizesTo("A\n\nB", "'''\nA\n\nB\n'''");

        // only the outermost runs are dropped, so an interior run keeps its exact length
        assertCanonicalizesTo("A\n\n\nB", "'''\n\nA\n\n\nB\n\n'''");
    }

    /**
     * Layout has already stripped trailing whitespace by the time blank lines are dropped, so a line
     * of spaces is as blank as an empty one - wherever it sits.
     */
    @Test
    public void testWhitespaceOnlyLinesCountAsBlank()
    {
        assertCanonicalizesTo("A", "'''\n   \nA\n   \n'''");
        assertCanonicalizesTo("A\n\nB", "'''\nA\n   \nB\n'''");
    }

    /**
     * A closing delimiter on the last line of content is the shape with no trailing newline. Its
     * leading whitespace is what feeds the common indent - not its length, which would strip the
     * whole line away and canonicalize a one-line docstring to nothing.
     */
    @Test
    public void testClosingDelimiterOnContentLine()
    {
        assertCanonicalizesTo("Hello", "'''\nHello'''");
        assertCanonicalizesTo("Hello", "'''\n    Hello'''");
        assertCanonicalizesTo("Text\n\n    code", "'''\n  Text\n\n      code'''");
    }

    /**
     * Documentation is prose in whatever script the author writes, and a supplementary character is
     * two chars wide - so neither the trailing-whitespace strip nor the indent strip may cut one in
     * half.
     */
    @Test
    public void testSupplementaryCharactersSurviveIntact()
    {
        assertCanonicalizesTo("Written in " + AVESTAN_A + ".", "'''\n    Written in " + AVESTAN_A + ".\n    '''");
        assertCanonicalizesTo("Ends with " + AVESTAN_A, "'''\n    Ends with " + AVESTAN_A + "   \n    '''");
        assertCanonicalizesTo(AVESTAN_A + " leads", "'''\n    " + AVESTAN_A + " leads\n    '''");
    }

    /**
     * Canonicalization inherits the layout's validation rather than papering over bad input: a
     * malformed literal is a parse-time failure, not an empty docstring.
     */
    @Test
    public void testMalformedLiteralIsRejected()
    {
        assertInvalid("");
        assertInvalid("'''");
        assertInvalid("no delimiters at all");

        // content shares the opening delimiter's line
        assertInvalid("'''abc\n'''");

        // no line terminator after the opening delimiter
        assertInvalid("''''''");
    }

    /**
     * Content is literal: no escape sequence processing. This is the deliberate difference from a
     * string literal, which shares the layout but then runs through {@link StringEscape}.
     * Documentation is prose, and unescaping prose corrupts regexes, Markdown escapes and Windows
     * paths - see {@link MultilineTextLayout}.
     */
    @Test
    public void testEscapeSequencesAreContentNotEscapes()
    {
        assertCanonicalizesTo("a\\nb\\'c\\\\d", "'''\na\\nb\\'c\\\\d\n'''");
    }

    @Test
    public void testRegexAndWindowsPathSurviveIntact()
    {
        assertCanonicalizesTo("Matches \\d+ under C:\\users", "'''\nMatches \\d+ under C:\\users\n'''");
    }

    @Test
    public void testBlockCommentSurvivesAsContent()
    {
        assertCanonicalizesTo("```c\n/* legacy comment */\n```",
                "'''\n```c\n/* legacy comment */\n```\n'''");
    }

    @Test
    public void testMultiParagraphMarkdown()
    {
        assertCanonicalizesTo("A **person** in the system.\n\nIdentity is established by `legalName`.",
                "'''\nA **person** in the system.\n\nIdentity is established by `legalName`.\n'''");
    }

    /**
     * Round-trip shape a composer would emit: content and the closing delimiter at the same
     * indentation. Exactly that indentation is stripped, and deeper indentation survives
     * relative to it.
     */
    @Test
    public void testComposerShapeRoundTrips()
    {
        assertCanonicalizesTo("Title\n\n    code block\n\nEnd",
                "'''\n  Title\n\n      code block\n\n  End\n  '''");
    }

    /**
     * The layout is shared with the string literal; only the escape handling and the surrounding
     * blank lines differ. Pinning that here keeps the two from drifting apart.
     */
    @Test
    public void testSharesLayoutWithTheStringLiteral()
    {
        Assert.assertEquals("hello\nworld\n", MultilineTextLayout.layout("'''\n    hello\n    world\n    '''"));
        assertCanonicalizesTo("hello\nworld", "'''\n    hello\n    world\n    '''");
    }

    private static void assertCanonicalizesTo(String expected, String rawTokenText)
    {
        Assert.assertEquals(expected, DocumentationCanonicalizer.canonicalize(rawTokenText));
    }

    private static void assertInvalid(String rawTokenText)
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> DocumentationCanonicalizer.canonicalize(rawTokenText));
        Assert.assertEquals("Invalid multi-line string: " + rawTokenText, e.getMessage());
    }
}
