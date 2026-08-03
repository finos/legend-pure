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

import org.finos.legend.pure.m4.tools.TextTools;

/**
 * Java-text-block layout for the raw text of a multi-line literal ({@code '''...'''}): drop the
 * opening delimiter's line, remove the common (incidental) leading indentation, and strip trailing
 * whitespace from each line.
 * <p>
 * Escape processing is deliberately not part of this. A string literal applies
 * {@link StringEscape#unescape} on top of the layout; documentation does not, because its content
 * is prose. Unescaping prose silently corrupts a regex ({@code \d} becomes {@code d}), a Markdown
 * escape ({@code \*} becomes {@code *}) and a Windows path ({@code C:\temp} becomes {@code C:}
 * followed by a tab) - and a backslash-u not followed by four hex digits, as in a path under
 * {@code C:} named "users", throws an unchecked {@code IllegalArgumentException} carrying no
 * source information at all.
 */
public class MultilineTextLayout
{
    private MultilineTextLayout()
    {
    }

    /**
     * @param rawTokenText the literal's raw text, including both {@code '''} delimiters. The lexer
     *                     guarantees a line terminator after the opening delimiter.
     */
    public static String layout(String rawTokenText)
    {
        // Verify we have a valid multi-line string and split lines. We will replace line breaks uniformly
        // with \n.
        if (!((rawTokenText.length() >= 6) && rawTokenText.startsWith("'''") && rawTokenText.endsWith("'''")))
        {
            throw new IllegalArgumentException("Invalid multi-line string: " + rawTokenText);
        }
        String[] lines = rawTokenText.substring(3, rawTokenText.length() - 3).split("\r\n|\r|\n", -1);
        if ((lines.length < 2) || !TextTools.isBlank(lines[0]))
        {
            throw new IllegalArgumentException("Invalid multi-line string: " + rawTokenText);
        }

        int minIndent = findMinIndent(lines);
        StringBuilder builder = new StringBuilder(rawTokenText.length() - 6 - lines[0].length());
        appendLine(builder, lines[1], minIndent);
        for (int i = 2; i < lines.length; i++)
        {
            appendLine(builder.append('\n'), lines[i], minIndent);
        }
        return builder.toString();
    }

    private static int findMinIndent(String[] lines)
    {
        // Minimum indentation is computed over the leading whitespace of every non-blank line, plus that of the
        // last line (the closing-delimiter line) whether or not it is blank - the latter sets a floor that
        // prevents over-stripping. A last line that is entirely whitespace counts as indented by its full length.
        int lastIndex = lines.length - 1;
        String lastLine = lines[lastIndex];
        int lastLineIndent = TextTools.indexOfNonWhitespace(lastLine);
        int minIndent = (lastLineIndent == -1) ? lastLine.length() : lastLineIndent;
        for (int i = 1; (minIndent != 0) && (i < lastIndex); i++)
        {
            int index = TextTools.indexOfNonWhitespace(lines[i]);
            if (index != -1)
            {
                minIndent = Math.min(minIndent, index);
            }
        }
        return minIndent;
    }

    private static void appendLine(StringBuilder builder, String line, int minIndent)
    {
        if (minIndent < line.length())
        {
            int end = TextTools.lastIndexOfNonWhitespace(line, minIndent);
            if (end >= minIndent)
            {
                builder.append(line, minIndent, end + Character.charCount(line.codePointAt(end)));
            }
        }
    }
}
