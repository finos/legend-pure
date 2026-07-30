// Copyright 2020 Goldman Sachs
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

import org.antlr.v4.runtime.Token;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class AntlrSourceInformation
{
    private final int offsetLine;
    private final int offsetColumn;

    private final String sourceName;
    private final boolean addLines;

    public AntlrSourceInformation(int offsetLine, int offsetColumn, String sourceName, boolean addLines)
    {
        this.offsetLine = offsetLine;
        this.offsetColumn = offsetColumn;
        this.sourceName = sourceName;
        this.addLines = addLines;
    }

    public AntlrSourceInformation(int offsetLine, int offsetColumn, String sourceName)
    {
        this(offsetLine, offsetColumn, sourceName, true);
    }

    public String getSourceName()
    {
        return this.sourceName;
    }

    public int getOffsetLine()
    {
        return this.offsetLine;
    }

    public int getOffsetColumn()
    {
        return this.offsetColumn;
    }

    public SourceInformation getPureSourceInformation(int line, int column)
    {
        return getPureSourceInformation(line, column, line, column, line, column);
    }

    public SourceInformation getPureSourceInformation(int beginLine, int beginColumn, int endLine, int endColumn)
    {
        return getPureSourceInformation(beginLine, beginColumn, beginLine, beginColumn, endLine, endColumn);
    }

    public SourceInformation getPureSourceInformation(int beginLine, int beginColumn, int mainLine, int mainColumn, int endLine, int endColumn)
    {
        if (!this.addLines)
        {
            return null;
        }

        int adjustedBeginLine = adjustLine(beginLine);
        int adjustedBeginCol = adjustColumn(beginLine, beginColumn);
        int adjustedMainLine = adjustLine(mainLine);
        int adjustedMainCol = adjustColumn(mainLine, mainColumn);
        int adjustedEndLine = adjustLine(endLine);
        int adjustedEndCol = adjustColumn(endLine, endColumn);
        return new SourceInformation(this.sourceName, adjustedBeginLine, adjustedBeginCol, adjustedMainLine, adjustedMainCol, adjustedEndLine, adjustedEndCol);
    }

    public SourceInformation getPureSourceInformation(Token token)
    {
        int line = token.getLine();
        int startCol = token.getCharPositionInLine() + 1;
        int endCol = startCol + token.getText().length() - 1;
        return getPureSourceInformation(line, startCol, line, endCol);
    }

    public SourceInformation getPureSourceInformation(Token firstToken, Token middleToken, Token endToken)
    {
        boolean useEndOfEndToken = endToken == middleToken; // for backward compatibility, but its appropriateness is questionable
        return getPureSourceInformation(firstToken, middleToken, endToken, useEndOfEndToken);
    }

    public SourceInformation getPureSourceInformation(Token firstToken, Token middleToken, Token endToken, boolean useEndOfEndToken)
    {
        int beginLine = firstToken.getLine();
        int beginCol = firstToken.getCharPositionInLine() + 1;
        int mainLine = middleToken.getLine();
        int mainCol = middleToken.getCharPositionInLine() + 1;
        int endLine = endToken.getLine();
        int endCol = endToken.getCharPositionInLine() + 1;
        if (useEndOfEndToken)
        {
            endLine += countNewLines(endToken.getText());
            endCol = endColumnOf(endToken);
        }
        return getPureSourceInformation(beginLine, beginCol, mainLine, mainCol, endLine, endCol);
    }

    /**
     * The column the token's last character sits in.
     * <p>
     * A token reports only the line it <em>starts</em> on, so for one whose text spans lines -
     * {@code MULTILINE_STRING}, {@code DOC_STRING} and {@code DSL_TEXT} are the ones that can - the
     * end has to be derived from the text rather than from the start column plus the whole length,
     * which would name a column past the end of the first line.
     * <p>
     * Newlines are counted as {@code \n} only, and the column as the characters following the last
     * one, which is exactly how ANTLR advances line and {@code charPositionInLine} - so this stays
     * consistent with every other position the lexer reports.
     */
    private static int endColumnOf(Token token)
    {
        String text = token.getText();
        int lastNewLine = text.lastIndexOf('\n');
        return (lastNewLine < 0) ? (token.getCharPositionInLine() + text.length()) : (text.length() - lastNewLine - 1);
    }

    private static int countNewLines(String text)
    {
        int count = 0;
        for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1))
        {
            count++;
        }
        return count;
    }

    public SourceInformation getSourceInformationForUnknownErrorPosition(int line, int charPositionInLine)
    {
        return getPureSourceInformation(line, charPositionInLine + 1);
    }

    public SourceInformation getSourceInformationForOffendingToken(int line, int charPositionInLine, Token offendingToken)
    {
        return getPureSourceInformation(line, charPositionInLine + 1, offendingToken.getLine(), offendingToken.getCharPositionInLine() + offendingToken.getText().length());
    }

    private int adjustLine(int line)
    {
        return line + this.offsetLine;
    }

    private int adjustColumn(int line, int column)
    {
        return (line == 1) ? (column + this.offsetColumn) : column;
    }
}
