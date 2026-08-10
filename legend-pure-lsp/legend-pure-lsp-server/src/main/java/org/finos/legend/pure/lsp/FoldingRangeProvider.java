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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeKind;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;

/**
 * Computes folding ranges from a source file's raw text rather than the compiled AST: brace
 * positions, block comments, and {@code ###} section markers are lexical/structural concepts Pure's
 * AST doesn't expose per-element the way SourceInformation does for whole elements, so a lightweight
 * stack-based scan of the text is the simplest robust way to find them - the same approach used for
 * this exact problem in other LSP servers.
 */
public class FoldingRangeProvider
{
    public static List<FoldingRange> getFoldingRanges(PureRuntime runtime, String sourceId)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }
        String content = source.getContent();
        if (content == null || content.isEmpty())
        {
            return Collections.emptyList();
        }

        List<FoldingRange> ranges = new ArrayList<>();
        collectBraceAndCommentRanges(content, ranges);
        collectSectionRanges(content, ranges);
        return ranges;
    }

    /**
     * One pass over the raw text tracking brace nesting and block comments, skipping over
     * single-quoted string literals and {@code //} line comments so braces/comment markers inside
     * them are never mistaken for structural ones.
     */
    private static void collectBraceAndCommentRanges(String content, List<FoldingRange> ranges)
    {
        int n = content.length();
        int line = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;
        int blockCommentStartLine = -1;
        Deque<Integer> braceStartLines = new ArrayDeque<>();

        int i = 0;
        while (i < n)
        {
            char c = content.charAt(i);

            if (c == '\n')
            {
                inLineComment = false;
                line++;
                i++;
                continue;
            }

            if (inLineComment)
            {
                i++;
                continue;
            }

            if (inBlockComment)
            {
                if (c == '*' && i + 1 < n && content.charAt(i + 1) == '/')
                {
                    inBlockComment = false;
                    i += 2;
                    if (line > blockCommentStartLine)
                    {
                        ranges.add(newRange(blockCommentStartLine, line, FoldingRangeKind.Comment));
                    }
                    continue;
                }
                i++;
                continue;
            }

            if (inString)
            {
                if (c == '\\' && i + 1 < n)
                {
                    i += 2;
                    continue;
                }
                if (c == '\'')
                {
                    inString = false;
                }
                i++;
                continue;
            }

            // Outside any comment/string: the only place braces and comment/string openers count.
            if (c == '/' && i + 1 < n && content.charAt(i + 1) == '/')
            {
                inLineComment = true;
                i += 2;
                continue;
            }
            if (c == '/' && i + 1 < n && content.charAt(i + 1) == '*')
            {
                inBlockComment = true;
                blockCommentStartLine = line;
                i += 2;
                continue;
            }
            if (c == '\'')
            {
                inString = true;
                i++;
                continue;
            }
            if (c == '{')
            {
                braceStartLines.push(line);
                i++;
                continue;
            }
            if (c == '}')
            {
                if (!braceStartLines.isEmpty())
                {
                    int startLine = braceStartLines.pop();
                    if (line > startLine)
                    {
                        ranges.add(newRange(startLine, line, FoldingRangeKind.Region));
                    }
                }
                i++;
                continue;
            }
            i++;
        }
    }

    /**
     * A {@code ###X} multi-parser section marker runs from its own line to the line before the next
     * marker (or EOF). Markers found inside a block comment (already collected above) are excluded,
     * since those are comment text, not real section boundaries.
     */
    private static void collectSectionRanges(String content, List<FoldingRange> ranges)
    {
        List<int[]> commentSpans = new ArrayList<>();
        for (FoldingRange range : ranges)
        {
            if (FoldingRangeKind.Comment.equals(range.getKind()))
            {
                commentSpans.add(new int[]{range.getStartLine(), range.getEndLine()});
            }
        }

        String[] lines = content.split("\r\n|\r|\n", -1);
        List<Integer> markerLines = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++)
        {
            if (lines[lineIndex].trim().startsWith("###") && !withinAnyCommentSpan(lineIndex, commentSpans))
            {
                markerLines.add(lineIndex);
            }
        }

        for (int idx = 0; idx < markerLines.size(); idx++)
        {
            int startLine = markerLines.get(idx);
            int endLine = (idx + 1 < markerLines.size()) ? markerLines.get(idx + 1) - 1 : lines.length - 1;
            if (endLine > startLine)
            {
                ranges.add(newRange(startLine, endLine, FoldingRangeKind.Region));
            }
        }
    }

    private static boolean withinAnyCommentSpan(int line, List<int[]> commentSpans)
    {
        for (int[] span : commentSpans)
        {
            if (line >= span[0] && line <= span[1])
            {
                return true;
            }
        }
        return false;
    }

    private static FoldingRange newRange(int startLine, int endLine, String kind)
    {
        FoldingRange range = new FoldingRange(startLine, endLine);
        range.setKind(kind);
        return range;
    }
}
