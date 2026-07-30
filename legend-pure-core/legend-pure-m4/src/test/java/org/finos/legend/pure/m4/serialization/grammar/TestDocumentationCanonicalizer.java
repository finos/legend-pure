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
}
