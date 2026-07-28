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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.List;

/**
 * Finds the documentation comment attached to an element.
 * <p>
 * Documentation comments do not appear in any parser rule; they are captured on the
 * {@code DOCUMENTATION} lexer channel and located by looking backwards from an element's start
 * token. Attachment rules are specified by {@code TestDocComment}.
 */
public class DocCommentLookup
{
    /**
     * Finds nothing. Use on parse paths that cannot contain documentation comments, such as
     * lambdas and instance literals.
     */
    public static final DocCommentLookup NONE = new DocCommentLookup(null, -1);

    private final BufferedTokenStream tokenStream;
    private final int channel;

    public DocCommentLookup(Parser parser, int channel)
    {
        // Deliberately an unchecked narrowing: look-back needs buffering, and failing loudly here
        // beats documentation silently going missing.
        this.tokenStream = (parser == null) ? null : (BufferedTokenStream) parser.getTokenStream();
        this.channel = channel;
    }

    public Token findDocComment(ParserRuleContext ctx)
    {
        if ((this.tokenStream == null) || (ctx == null) || (ctx.getStart() == null))
        {
            return null;
        }
        Token start = ctx.getStart();
        List<Token> docs = this.tokenStream.getHiddenTokensToLeft(start.getTokenIndex(), this.channel);
        if ((docs == null) || docs.isEmpty())
        {
            return null;
        }
        Token nearest = docs.get(docs.size() - 1);
        return isAttached(nearest, start) ? nearest : null;
    }

    /**
     * The look-back window reaches back to the previous element, so it routinely spans comments
     * that document nothing - a file header, or a note trailing the previous element. Only
     * whitespace containing no blank line binds a documentation comment to what follows.
     */
    private static boolean isAttached(Token doc, Token elementStart)
    {
        CharStream input = elementStart.getInputStream();
        if (input == null)
        {
            return true;
        }
        int from = doc.getStopIndex() + 1;
        int to = elementStart.getStartIndex() - 1;
        return (from > to) || isWhitespaceWithoutBlankLine(input.getText(Interval.of(from, to)));
    }

    private static boolean isWhitespaceWithoutBlankLine(String gap)
    {
        String normalized = gap.replace("\r\n", "\n").replace('\r', '\n');
        int newLines = 0;
        for (int i = 0; i < normalized.length(); i++)
        {
            char c = normalized.charAt(i);
            if (!Character.isWhitespace(c))
            {
                return false;
            }
            if ((c == '\n') && (++newLines >= 2))
            {
                return false;
            }
        }
        return true;
    }
}
