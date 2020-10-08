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

package org.finos.legend.pure.m4.coreinstance;

import java.io.IOException;

public class SourceInformation implements Comparable<SourceInformation>
{
    private final String sourceId;
    private final int line;
    private final int column;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;

    public SourceInformation(String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        this.sourceId = sourceId;
        this.line = line;
        this.column = column;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public SourceInformation(String sourceId, int startLine, int startColumn, int endLine, int endColumn)
    {
        this(sourceId, startLine, startColumn, startLine, startColumn, endLine, endColumn);
    }

    public String getSourceId()
    {
        return this.sourceId;
    }

    public int getLine()
    {
        return this.line;
    }

    public int getColumn()
    {
        return this.column;
    }

    public int getStartLine()
    {
        return this.startLine;
    }

    public int getStartColumn()
    {
        return this.startColumn;
    }

    public int getEndLine()
    {
        return this.endLine;
    }

    public int getEndColumn()
    {
        return this.endColumn;
    }

    public String getMessage()
    {
        StringBuilder builder = new StringBuilder(this.sourceId.length() + 16);
        writeMessage(builder);
        return builder.toString();
    }

    public void writeMessage(StringBuilder builder)
    {
        builder.append(this.sourceId);
        builder.append(':');
        if (this.startLine == this.endLine)
        {
            builder.append(this.startLine);
            builder.append('c');
            if (this.startColumn == this.endColumn)
            {
                builder.append(this.startColumn);
            }
            else
            {
                builder.append('c');
                builder.append(this.startColumn);
                builder.append('-');
                builder.append(this.endColumn);
            }
        }
        else
        {
            builder.append(this.startLine);
            builder.append('c');
            builder.append(this.startColumn);
            builder.append('-');
            builder.append(this.endLine);
            builder.append('c');
            builder.append(this.endColumn);
        }
    }

    public void writeMessage(Appendable appendable) throws IOException
    {
        appendable.append(this.sourceId);
        appendable.append(':');
        if (this.startLine == this.endLine)
        {
            appendable.append(Integer.toString(this.startLine));
            appendable.append('c');
            if (this.startColumn == this.endColumn)
            {
                appendable.append(Integer.toString(this.startColumn));
            }
            else
            {
                appendable.append('c');
                appendable.append(Integer.toString(this.startColumn));
                appendable.append('-');
                appendable.append(Integer.toString(this.endColumn));
            }
        }
        else
        {
            appendable.append(Integer.toString(this.startLine));
            appendable.append('c');
            appendable.append(Integer.toString(this.startColumn));
            appendable.append('-');
            appendable.append(Integer.toString(this.endLine));
            appendable.append('c');
            appendable.append(Integer.toString(this.endColumn));
        }
    }

    public String toM4String()
    {
        return getM4SourceString(this.sourceId, this.startLine, this.startColumn, this.line, this.column, this.endLine, this.endColumn);
    }

    public void writeM4String(Appendable appendable)
    {
        writeM4SourceInformation(appendable, this.sourceId, this.startLine, this.startColumn, this.line, this.column, this.endLine, this.endColumn);
    }

    @Override
    public int hashCode()
    {
        int result = this.sourceId.hashCode();
        result = 31 * result + this.line;
        result = 31 * result + this.column;
        result = 31 * result + this.startLine;
        result = 31 * result + this.startColumn;
        result = 31 * result + this.endLine;
        result = 31 * result + this.endColumn;
        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof SourceInformation))
        {
            return false;
        }

        SourceInformation sourceInfo = (SourceInformation)other;
        return this.sourceId.equals(sourceInfo.sourceId) &&
                (this.line == sourceInfo.line) &&
                (this.column == sourceInfo.column) &&
                (this.startLine == sourceInfo.startLine) &&
                (this.startColumn == sourceInfo.startColumn) &&
                (this.endLine == sourceInfo.endLine) &&
                (this.endColumn == sourceInfo.endColumn);
    }

    @Override
    public int compareTo(SourceInformation other)
    {
        if (this == other)
        {
            return 0;
        }

        // Compare source id
        int sourceIdCmp = this.sourceId.compareTo(other.sourceId);
        if (sourceIdCmp != 0)
        {
            return sourceIdCmp;
        }

        // Compare main line and column
        if (this.line != other.line)
        {
            return Integer.compare(this.line, other.line);
        }
        if (this.column != other.column)
        {
            return Integer.compare(this.column, other.column);
        }

        // Compare start line and column
        if (this.startLine != other.startLine)
        {
            return Integer.compare(this.startLine, other.startLine);
        }
        if (this.startColumn != other.startColumn)
        {
            return Integer.compare(this.startColumn, other.startColumn);
        }

        // Compare end line and column
        if (this.endLine != other.endLine)
        {
            return Integer.compare(this.endLine, other.endLine);
        }
        return Integer.compare(this.endColumn, other.endColumn);
    }

    public boolean contains(SourceInformation contained){

        return (contained.getStartLine() > this.getStartLine() && contained.getEndLine() < this.getEndLine() ) ||
                (contained.getStartLine() == this.getStartLine() && contained.getEndLine() < this.getEndLine() && contained.getStartColumn() >= this.getStartColumn()) ||
                (contained.getStartLine() > this.getStartLine() && contained.getEndLine() == this.getEndLine() && this.getEndColumn() >= contained.getEndColumn()) ||
                (contained.getStartLine() == this.getStartLine() && contained.getEndLine() == this.getEndLine() && contained.getStartColumn() >= this.getStartColumn() && this.getEndColumn() >= contained.getEndColumn());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<SourceInformation ");
        writeMessage(builder);
        builder.append('>');
        return builder.toString();
    }

    public static String getM4SourceString(String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        StringBuilder builder = new StringBuilder(sourceId.length() + 32);
        writeM4SourceInformation(builder, sourceId, startLine, startColumn, line, column, endLine, endColumn);
        return builder.toString();
    }

    public static void writeM4SourceInformation(StringBuilder builder, String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        builder.append("?[");
        builder.append(sourceId);
        builder.append(':');
        builder.append(startLine);
        builder.append(',');
        builder.append(startColumn);
        builder.append(',');
        builder.append(line);
        builder.append(',');
        builder.append(column);
        builder.append(',');
        builder.append(endLine);
        builder.append(',');
        builder.append(endColumn);
        builder.append("]?");
    }

    public static void writeM4SourceInformation(Appendable appendable, String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        try
        {
            appendable.append("?[");
            appendable.append(sourceId);
            appendable.append(':');
            appendable.append(Integer.toString(startLine));
            appendable.append(',');
            appendable.append(Integer.toString(startColumn));
            appendable.append(',');
            appendable.append(Integer.toString(line));
            appendable.append(',');
            appendable.append(Integer.toString(column));
            appendable.append(',');
            appendable.append(Integer.toString(endLine));
            appendable.append(',');
            appendable.append(Integer.toString(endColumn));
            appendable.append("]?");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
