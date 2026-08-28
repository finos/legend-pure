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

import java.util.List;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeKind;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies FoldingRangeProvider's text-based scan: brace-delimited bodies, multi-line block
 * comments, and ###-marker section spans - and that braces/markers inside string literals, line
 * comments, and block comments are correctly skipped rather than mistaken for structural ones.
 */
public class FoldingRangeProviderTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    private static List<FoldingRange> foldingRangesFor(String sourceId, String code)
    {
        LegendPureSession.CompileResult result = session.modifyAndCompile(sourceId, code);
        Assert.assertTrue("Test fixture should compile: "
                + (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
        return FoldingRangeProvider.getFoldingRanges(session.getPureRuntime(), sourceId);
    }

    @Test
    public void foldsClassBody()
    {
        String code = "Class test::folding::Person\n" +   // line 0
                "{\n" +                                    // line 1 (brace opens)
                "  name: String[1];\n" +                    // line 2
                "  age: Integer[1];\n" +                    // line 3
                "}\n";                                      // line 4 (brace closes)

        List<FoldingRange> ranges = foldingRangesFor("folding_class.pure", code);
        boolean found = ranges.stream().anyMatch(r ->
                r.getStartLine() == 1 && r.getEndLine() == 4 && FoldingRangeKind.Region.equals(r.getKind()));
        Assert.assertTrue("Expected a region fold from the opening brace (line 1) to the closing "
                + "brace (line 4), found: " + ranges, found);
    }

    @Test
    public void ignoresBraceInsideStringLiteral()
    {
        // The '{' and '}' inside the string must not be treated as structural braces, and must not
        // desynchronize the real class-body brace matching that follows.
        String code = "Class test::folding::WithBraceString\n" +  // 0
                "{\n" +                                            // 1
                "  label: String[1] = '{not a brace}';\n" +        // 2
                "}\n";                                             // 3

        List<FoldingRange> ranges = foldingRangesFor("folding_string_brace.pure", code);
        boolean found = ranges.stream().anyMatch(r -> r.getStartLine() == 1 && r.getEndLine() == 3);
        Assert.assertTrue("Real class body fold must still be found despite braces inside a string literal: "
                + ranges, found);
        Assert.assertEquals("Only the real class body should fold - the string's braces must not "
                + "produce extra/incorrect ranges: " + ranges, 1, ranges.size());
    }

    @Test
    public void foldsMultiLineBlockComment()
    {
        String code = "/*\n" +                 // 0 (comment opens)
                " * A multi-line\n" +           // 1
                " * block comment\n" +          // 2
                " */\n" +                       // 3 (comment closes)
                "function test::folding::documented(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n";

        List<FoldingRange> ranges = foldingRangesFor("folding_comment.pure", code);
        boolean found = ranges.stream().anyMatch(r ->
                r.getStartLine() == 0 && r.getEndLine() == 3 && FoldingRangeKind.Comment.equals(r.getKind()));
        Assert.assertTrue("Expected a comment fold spanning the block comment's 4 lines, found: " + ranges, found);
    }

    @Test
    public void ignoresSingleLineBlockComment()
    {
        String code = "/* single line */\n" +
                "function test::folding::trivial(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n";

        List<FoldingRange> ranges = foldingRangesFor("folding_single_line_comment.pure", code);
        boolean foundComment = ranges.stream().anyMatch(r -> FoldingRangeKind.Comment.equals(r.getKind()));
        Assert.assertFalse("A block comment on a single line has nothing to fold and must not be "
                + "reported: " + ranges, foundComment);
    }

    @Test
    public void foldsSectionMarkerSpans()
    {
        String code = "###Pure\n" +                                   // 0 (marker)
                "function test::folding::inPureSection(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "###Pure\n" +                                          // 5 (next marker)
                "// a trailing, otherwise-empty second Pure section\n";

        List<FoldingRange> ranges = foldingRangesFor("folding_sections.pure", code);
        boolean found = ranges.stream().anyMatch(r -> r.getStartLine() == 0 && r.getEndLine() == 4);
        Assert.assertTrue("Expected the ###Pure section to fold from its marker line to the line "
                + "before the next ### marker: " + ranges, found);
    }

    @Test
    public void unknownSource_returnsEmpty()
    {
        List<FoldingRange> ranges = FoldingRangeProvider.getFoldingRanges(session.getPureRuntime(), "no_such_source_999.pure");
        Assert.assertTrue(ranges.isEmpty());
    }
}
