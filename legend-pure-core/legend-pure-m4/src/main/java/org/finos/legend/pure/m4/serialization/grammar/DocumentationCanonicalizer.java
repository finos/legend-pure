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
 * Converts the raw text of a documentation literal into the string stored as the
 * {@code meta::pure::profiles::doc.doc} tagged value.
 * <p>
 * This is {@link MultilineTextLayout} plus the removal of leading and trailing blank lines, and
 * deliberately without {@link StringEscape}: content is literal, so {@code \n} and {@code \\} are
 * content rather than escapes. Dropping the surrounding blank lines is what makes a docstring and
 * the equivalent explicit {@code doc.doc} tagged value hold the same string.
 * <p>
 * Behaviour is specified by {@code TestDocumentationCanonicalizer}.
 */
public class DocumentationCanonicalizer
{
    private DocumentationCanonicalizer()
    {
    }

    public static String canonicalize(String rawTokenText)
    {
        // Layout has already stripped trailing whitespace from every line, so a blank line is exactly empty and a
        // run of leading or trailing blank lines is exactly a run of leading or trailing newlines.
        String text = MultilineTextLayout.layout(rawTokenText);
        int start = 0;
        int end = text.length();
        while ((start < end) && (text.charAt(start) == '\n'))
        {
            start++;
        }
        while ((end > start) && (text.charAt(end - 1) == '\n'))
        {
            end--;
        }
        return text.substring(start, end);
    }
}
