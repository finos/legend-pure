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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts the raw text of a {@code DOC_COMMENT} token into the string stored as the
 * {@code meta::pure::profiles::doc.doc} tagged value.
 * <p>
 * Content is literal: unlike string literals, it is never passed through
 * {@link StringEscape}, so {@code \n} and {@code \\} are content rather than escapes.
 * <p>
 * Behaviour is specified by {@code TestDocCommentCanonicalizer}.
 */
public class DocCommentCanonicalizer
{
    private static final String OPEN = "/**";
    private static final String CLOSE = "*/";

    private DocCommentCanonicalizer()
    {
    }

    public static String canonicalize(String rawTokenText)
    {
        List<String> lines = splitLines(stripDelimiters(rawTokenText));
        trimOpeningLine(lines);
        int closingIndent = removeClosingLine(lines);

        if (everyNonBlankLineStartsWithStar(lines))
        {
            lines.replaceAll(DocCommentCanonicalizer::stripLeadingStar);
        }
        else
        {
            int indent = commonIndent(lines, closingIndent);
            lines.replaceAll(line -> stripLeading(line, indent));
        }

        lines.replaceAll(DocCommentCanonicalizer::stripTrailingWhitespace);
        removeLeadingAndTrailingBlankLines(lines);
        return String.join("\n", lines);
    }

    private static String stripDelimiters(String rawTokenText)
    {
        String body = rawTokenText.startsWith(OPEN) ? rawTokenText.substring(OPEN.length()) : rawTokenText;
        return body.endsWith(CLOSE) ? body.substring(0, body.length() - CLOSE.length()) : body;
    }

    private static List<String> splitLines(String body)
    {
        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        return new ArrayList<>(Arrays.asList(normalized.split("\n", -1)));
    }

    private static void trimOpeningLine(List<String> lines)
    {
        if (lines.isEmpty())
        {
            return;
        }
        if (isBlank(lines.get(0)))
        {
            lines.remove(0);
        }
        else if (lines.get(0).startsWith(" "))
        {
            lines.set(0, lines.get(0).substring(1));
        }
    }

    /**
     * Removes the closing delimiter's line and returns its indentation, which goes on to floor
     * {@link #commonIndent} the way it does in a Java text block.
     */
    private static int removeClosingLine(List<String> lines)
    {
        if (lines.isEmpty() || !isBlank(lines.get(lines.size() - 1)))
        {
            return Integer.MAX_VALUE;
        }
        return lines.remove(lines.size() - 1).length();
    }

    private static boolean everyNonBlankLineStartsWithStar(List<String> lines)
    {
        boolean any = false;
        for (String line : lines)
        {
            if (!isBlank(line))
            {
                if (!startsWithStar(line))
                {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }

    private static int commonIndent(List<String> lines, int closingIndent)
    {
        int indent = closingIndent;
        for (String line : lines)
        {
            if (!isBlank(line))
            {
                indent = Math.min(indent, leadingWhitespaceLength(line));
            }
        }
        return (indent == Integer.MAX_VALUE) ? 0 : indent;
    }

    private static void removeLeadingAndTrailingBlankLines(List<String> lines)
    {
        while (!lines.isEmpty() && lines.get(0).isEmpty())
        {
            lines.remove(0);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty())
        {
            lines.remove(lines.size() - 1);
        }
    }

    private static String stripLeading(String line, int count)
    {
        return (line.length() <= count) ? "" : line.substring(count);
    }

    private static String stripLeadingStar(String line)
    {
        int index = leadingWhitespaceLength(line);
        if ((index >= line.length()) || (line.charAt(index) != '*'))
        {
            return line;
        }
        index++;
        if ((index < line.length()) && (line.charAt(index) == ' '))
        {
            index++;
        }
        return line.substring(index);
    }

    private static String stripTrailingWhitespace(String line)
    {
        int end = line.length();
        while ((end > 0) && Character.isWhitespace(line.charAt(end - 1)))
        {
            end--;
        }
        return line.substring(0, end);
    }

    private static boolean startsWithStar(String line)
    {
        int index = leadingWhitespaceLength(line);
        return (index < line.length()) && (line.charAt(index) == '*');
    }

    private static boolean isBlank(String line)
    {
        return leadingWhitespaceLength(line) == line.length();
    }

    private static int leadingWhitespaceLength(String line)
    {
        int i = 0;
        while ((i < line.length()) && Character.isWhitespace(line.charAt(i)))
        {
            i++;
        }
        return i;
    }
}
