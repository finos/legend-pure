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

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.antlr.v4.runtime.Token;

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

    public AntlrSourceInformation(int offsetColumn, int offsetLine, String sourceName)
    {
        this(offsetColumn, offsetLine, sourceName, true);
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

    public SourceInformation getPureSourceInformation(int beginLine, int beginColumn, int endLine, int endColumn)
    {
        if (!this.addLines)
        {
            return null;
        }
        int offsetColumn = beginLine == 1 ? this.offsetColumn : 0;
        int endColumnWithOffset = endColumn + offsetColumn + 1;
        int beginLineWithOffset = beginLine + offsetLine;
        int beginColumnWithOffset = beginColumn + 1 + offsetColumn;
        int endLineWithOffset = endLine + offsetLine;
        return new SourceInformation(this.sourceName, beginLineWithOffset, beginColumnWithOffset, beginLineWithOffset, beginColumnWithOffset, endLineWithOffset, endColumnWithOffset);
    }

    public SourceInformation getPureSourceInformation(Token token)
    {
        return this.getPureSourceInformation(token.getLine(), token.getCharPositionInLine(), token.getLine(), token.getCharPositionInLine() + token.getText().length() - 1);
    }

    public SourceInformation getPureSourceInformation(Token firstToken, Token middleToken, Token endToken)
    {
        if (!this.addLines)
        {
            return null;
        }
        int offsetColumn = firstToken.getLine() == 1 ? this.offsetColumn : 0;
        return new SourceInformation(this.sourceName, firstToken.getLine() + this.offsetLine, firstToken.getCharPositionInLine() + 1 + offsetColumn, middleToken.getLine() + this.offsetLine, middleToken.getCharPositionInLine() + 1 + offsetColumn, endToken.getLine() + this.offsetLine, (endToken == middleToken ? endToken.getCharPositionInLine() + endToken.getText().length() - 1 : endToken.getCharPositionInLine()) + 1 + offsetColumn);
    }

    public SourceInformation getSourceInformationForUnknownErrorPosition(int line, int charPositionInLine)
    {
        return new SourceInformation(this.sourceName, line + this.offsetLine, charPositionInLine + 1 + this.offsetColumn, line + this.offsetLine, charPositionInLine + 1 + this.offsetColumn);
    }

    public SourceInformation getSourceInformationForOffendingToken(int line, int charPositionInLine, Token offendingToken)
    {
        return new SourceInformation(this.sourceName, line + this.offsetLine, charPositionInLine + 1 + this.offsetColumn, offendingToken.getLine() + this.offsetLine, offendingToken.getStopIndex() + 1 + this.offsetColumn);
    }
}
