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
        // Normalize line terminators, then drop the opening delimiter line (through its terminator)
        // and the trailing closing '''.
        String normalized = rawTokenText.replace("\r\n", "\n").replace('\r', '\n');
        int firstNewLine = normalized.indexOf('\n');
        String body = normalized.substring(firstNewLine + 1, normalized.length() - 3);

        String[] lines = body.split("\n", -1);

        // Minimum indentation is computed over every non-blank line plus the last line (the
        // closing-delimiter line, even when blank) - the latter sets a floor that prevents
        // over-stripping.
        int minIndent = Integer.MAX_VALUE;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            int leading = leadingWhitespaceLength(line);
            if ((leading < line.length()) || (i == lines.length - 1))
            {
                minIndent = Math.min(minIndent, leading);
            }
        }
        if (minIndent == Integer.MAX_VALUE)
        {
            minIndent = 0;
        }

        StringBuilder builder = new StringBuilder(body.length());
        for (int i = 0; i < lines.length; i++)
        {
            if (i > 0)
            {
                builder.append('\n');
            }
            String line = lines[i];
            builder.append(stripTrailingWhitespace(line.substring(Math.min(minIndent, line.length()))));
        }

        return builder.toString();
    }

    static int leadingWhitespaceLength(String line)
    {
        int i = 0;
        while ((i < line.length()) && Character.isWhitespace(line.charAt(i)))
        {
            i++;
        }
        return i;
    }

    static String stripTrailingWhitespace(String line)
    {
        int end = line.length();
        while ((end > 0) && Character.isWhitespace(line.charAt(end - 1)))
        {
            end--;
        }
        return line.substring(0, end);
    }
}
