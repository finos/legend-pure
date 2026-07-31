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

package org.finos.legend.pure.m4.serialization.grammar.antlr;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.Token;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.Test;

/**
 * Where a source range ends when it ends at a token.
 * <p>
 * An ANTLR token reports the line it <em>starts</em> on and nothing else, so the end of a token
 * whose text spans lines has to come from the text. Most tokens sit on one line;
 * {@code MULTILINE_STRING}, {@code DOC_STRING} and {@code DSL_TEXT} need not.
 */
public class TestAntlrSourceInformation
{
    @Test
    public void testSingleLineToken()
    {
        // 'Class' on line 4, starting at column 1, so ending at column 5.
        Token token = token("Class", 4, 0);

        assertPosition(4, 1, 4, 5, sourceInformation().getPureSourceInformation(token, token, token));
    }

    /**
     * Deriving the end column from the start column plus the whole text length - correct for a
     * single-line token - would say line 1, column 12 here, on a line three characters long.
     */
    @Test
    public void testMultiLineTokenEndsOnItsLastLine()
    {
        // 1: '''
        // 2: Doc.
        // 3: '''
        Token token = token("'''\nDoc.\n'''", 1, 0);

        assertPosition(1, 1, 3, 3, sourceInformation().getPureSourceInformation(token, token, token));
    }

    @Test
    public void testIndentedMultiLineTokenEndsAtTheClosingDelimiter()
    {
        // 3:   '''
        // 4:   Given name.
        // 5:   '''          <- closing delimiter occupies columns 3 to 5
        Token token = token("'''\n  Given name.\n  '''", 3, 2);

        assertPosition(3, 3, 5, 5, sourceInformation().getPureSourceInformation(token, token, token));
    }

    @Test
    public void testBeginAndMainComeFromTheirOwnTokens()
    {
        Token first = token("{", 1, 6);
        Token middle = token("doc", 1, 7);
        Token end = token("'''\nDoc.\n'''", 1, 20);

        // Distinct middle and end tokens, so the range ends where the end token begins.
        assertPosition(1, 7, 1, 21, sourceInformation().getPureSourceInformation(first, middle, end));
        // Asking for the end of the end token walks its text to the last line.
        assertPosition(1, 7, 3, 3, sourceInformation().getPureSourceInformation(first, middle, end, true));
    }

    private static void assertPosition(int startLine, int startColumn, int endLine, int endColumn, SourceInformation actual)
    {
        Assert.assertEquals("start line", startLine, actual.getStartLine());
        Assert.assertEquals("start column", startColumn, actual.getStartColumn());
        Assert.assertEquals("end line", endLine, actual.getEndLine());
        Assert.assertEquals("end column", endColumn, actual.getEndColumn());
    }

    private static AntlrSourceInformation sourceInformation()
    {
        return new AntlrSourceInformation(0, 0, "test.pure");
    }

    private static Token token(String text, int line, int charPositionInLine)
    {
        CommonToken token = (CommonToken) CommonTokenFactory.DEFAULT.create(Token.DEFAULT_CHANNEL, text);
        token.setLine(line);
        token.setCharPositionInLine(charPositionInLine);
        return token;
    }
}
