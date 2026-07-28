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
public class TestDocCommentCanonicalizer
{
    @Test
    public void testEmptyDocComment()
    {
        assertCanonicalizesTo("", "/** */");
    }

    @Test
    public void testWhitespaceOnlyContentIsEmpty()
    {
        assertCanonicalizesTo("", "/**\n   \n  \n*/");
    }

    @Test
    public void testStarOnlyLinesAreEmpty()
    {
        assertCanonicalizesTo("", "/**\n *\n *\n */");
    }

    @Test
    public void testSingleLine()
    {
        assertCanonicalizesTo("One line.", "/** One line. */");
    }

    @Test
    public void testSingleLineWithoutPaddingSpaces()
    {
        assertCanonicalizesTo("x", "/**x*/");
    }

    @Test
    public void testStarPrefixedLinesAreStripped()
    {
        assertCanonicalizesTo("A\n\nB", "/**\n * A\n *\n * B\n */");
    }

    @Test
    public void testIndentationIsRelativeToClosingDelimiter()
    {
        assertCanonicalizesTo("Text\n\n    code", "/**\n  Text\n\n      code\n  */");
    }

    /**
     * All-or-nothing star stripping. The line "Options:" has no leading star, so nothing is
     * stripped and the Markdown bullets survive as content.
     */
    @Test
    public void testMarkdownBulletListIsPreserved()
    {
        assertCanonicalizesTo("Options:\n* first\n* second",
                "/**\nOptions:\n* first\n* second\n*/");
    }

    @Test
    public void testMixedStarAndNonStarLinesDisableStripping()
    {
        assertCanonicalizesTo("* starred\nplain", "/**\n* starred\nplain\n*/");
    }

    /**
     * The closing delimiter participates in the indent computation, so when it sits at column 0
     * nothing is stripped and all content indentation is preserved.
     */
    @Test
    public void testClosingDelimiterAtColumnZeroPreservesAllIndentation()
    {
        assertCanonicalizesTo("    indented", "/**\n    indented\n*/");
    }

    @Test
    public void testCarriageReturnsAreNormalized()
    {
        assertCanonicalizesTo("A\nB", "/**\r\nA\r\nB\r\n*/");
        assertCanonicalizesTo("A\nB", "/**\rA\rB\r*/");
    }

    @Test
    public void testTrailingWhitespaceIsStripped()
    {
        assertCanonicalizesTo("A\nB", "/**\nA   \nB\t\n*/");
    }

    @Test
    public void testLeadingAndTrailingBlankLinesAreDropped()
    {
        assertCanonicalizesTo("A", "/**\n\n\nA\n\n\n*/");
    }

    @Test
    public void testInteriorBlankLinesArePreserved()
    {
        assertCanonicalizesTo("A\n\nB", "/**\nA\n\nB\n*/");
    }

    /**
     * Content is literal (spec D3): no escape sequence processing. This is the deliberate
     * difference from string literals, which do run through {@link StringEscape}.
     */
    @Test
    public void testEscapeSequencesAreContentNotEscapes()
    {
        assertCanonicalizesTo("a\\nb\\'c\\\\d", "/** a\\nb\\'c\\\\d */");
    }

    @Test
    public void testNestedBlockCommentSurvivesAsContent()
    {
        assertCanonicalizesTo("```c\n/* legacy comment */\n```",
                "/**\n```c\n/* legacy comment */\n```\n*/");
    }

    @Test
    public void testMultiParagraphMarkdown()
    {
        assertCanonicalizesTo("A **person** in the system.\n\nIdentity is established by `legalName`.",
                "/**\nA **person** in the system.\n\nIdentity is established by `legalName`.\n*/");
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
                "/**\n  Title\n\n      code block\n\n  End\n  */");
    }

    private static void assertCanonicalizesTo(String expected, String rawTokenText)
    {
        Assert.assertEquals(expected, DocCommentCanonicalizer.canonicalize(rawTokenText));
    }
}
