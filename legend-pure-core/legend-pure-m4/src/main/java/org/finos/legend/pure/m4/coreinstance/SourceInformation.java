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

import org.finos.legend.pure.m4.tools.SafeAppendable;

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
        return appendMessage(new StringBuilder(this.sourceId.length() + 16)).toString();
    }

    /**
     * This method is deprecated and kept only for backward compatibility. Use {@link #appendMessage} instead.
     */
    @Deprecated
    public void writeMessage(StringBuilder builder)
    {
        appendMessage(builder);
    }

    /**
     * This method is deprecated and kept only for backward compatibility. Use {@link #appendMessage} instead.
     */
    @Deprecated
    public void writeMessage(Appendable appendable) throws IOException
    {
        appendMessage(appendable);
    }

    /**
     * Append the source information message to the given Appendable, and return the Appendable.
     *
     * @param appendable target appendable
     * @return given appendable
     */
    public <T extends Appendable> T appendMessage(T appendable)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append(this.sourceId).append(':');
        if (this.startLine == this.endLine)
        {
            safeAppendable.append(this.startLine);
            if (this.startColumn == this.endColumn)
            {
                safeAppendable.append('c').append(this.startColumn);
            }
            else
            {
                safeAppendable.append("cc").append(this.startColumn).append('-').append(this.endColumn);
            }
        }
        else
        {
            safeAppendable.append(this.startLine).append('c').append(this.startColumn).append('-')
                    .append(this.endLine).append('c').append(this.endColumn);
        }
        return appendable;
    }

    public String toM4String()
    {
        return getM4SourceString(this.sourceId, this.startLine, this.startColumn, this.line, this.column, this.endLine, this.endColumn);
    }

    /**
     * This method is deprecated and kept only for backward compatibility. Use {@link #appendM4String} instead.
     */
    @Deprecated
    public void writeM4String(Appendable appendable)
    {
        appendM4String(appendable);
    }

    public <T extends Appendable> T appendM4String(T appendable)
    {
        return appendM4SourceInformation(appendable, this.sourceId, this.startLine, this.startColumn, this.line, this.column, this.endLine, this.endColumn);
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

        SourceInformation sourceInfo = (SourceInformation) other;
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
        int mainCmp = comparePositions(this.line, this.column, other.line, other.column);
        if (mainCmp != 0)
        {
            return mainCmp;
        }

        // Compare start line and column
        int startCmp = comparePositions(this.startLine, this.column, other.startLine, other.startColumn);
        if (startCmp != 0)
        {
            return startCmp;
        }

        // Compare end line and column
        return comparePositions(this.endLine, this.endColumn, other.endLine, other.endColumn);
    }

    /**
     * This method is deprecated and kept only for backward compatibility. Use {@link #subsumes} instead.
     */
    @Deprecated
    public boolean contains(SourceInformation contained)
    {
        return (contained.getStartLine() > this.getStartLine() && contained.getEndLine() < this.getEndLine()) ||
                (contained.getStartLine() == this.getStartLine() && contained.getEndLine() < this.getEndLine() && contained.getStartColumn() >= this.getStartColumn()) ||
                (contained.getStartLine() > this.getStartLine() && contained.getEndLine() == this.getEndLine() && this.getEndColumn() >= contained.getEndColumn()) ||
                (contained.getStartLine() == this.getStartLine() && contained.getEndLine() == this.getEndLine() && contained.getStartColumn() >= this.getStartColumn() && this.getEndColumn() >= contained.getEndColumn());
    }

    /**
     * Check whether this source information subsumes other. This is true if the source ids are equal, and the start and
     * end boundaries of other are contained within or equal to the start and end boundaries of this.
     *
     * @param other other source information
     * @return whether this subsumes other
     */
    public boolean subsumes(SourceInformation other)
    {
        return (other != null) &&
                this.sourceId.equals(other.sourceId) &&
                isNotAfter(this.startLine, this.startColumn, other.startLine, other.startColumn) &&
                isNotBefore(this.endLine, this.endColumn, other.endLine, other.endColumn);
    }

    /**
     * Check whether this source information intersects other. This is true if the source ids are equals, and the start
     * and end boundaries of this overlap the start and end boundaries of other.
     *
     * @param other other source information
     * @return whether this intersects other
     */
    public boolean intersects(SourceInformation other)
    {
        return (other != null) &&
                this.sourceId.equals(other.sourceId) &&
                isNotAfter(this.startLine, this.startColumn, other.endLine, other.endColumn) &&
                isNotBefore(this.endLine, this.endColumn, other.startLine, other.startColumn);
    }

    /**
     * Return whether this source information is valid. That is true if the source id is non-null and the start, main,
     * and end positions define a valid interval (i.e., start is not after main, and main is not after end). If this is
     * not valid, then the behavior of methods such as {@link #subsumes} and {@link #intersects} is undefined.
     *
     * @return whether this source information is valid
     */
    public boolean isValid()
    {
        return (this.sourceId != null) &&
                (this.startLine >= 1) &&
                (this.startColumn >= 1) &&
                (this.column >= 1) &&
                (this.endColumn >= 1) &&
                isNotBefore(this.line, this.column, this.startLine, this.startColumn) &&
                isNotBefore(this.endLine, this.endColumn, this.line, this.column);
    }

    @Override
    public String toString()
    {
        return appendMessage(new StringBuilder("<SourceInformation ")).append('>').toString();
    }

    public static String getM4SourceString(String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        return appendM4SourceInformation(new StringBuilder(sourceId.length() + 32), sourceId, startLine, startColumn, line, column, endLine, endColumn).toString();
    }

    /**
     * This method is deprecated and kept for backward compatibility. Use {@link #appendM4SourceInformation} instead.
     */
    @Deprecated
    public static void writeM4SourceInformation(StringBuilder builder, String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        appendM4SourceInformation(builder, sourceId, startLine, startColumn, line, column, endLine, endColumn);
    }

    /**
     * This method is deprecated and kept for backward compatibility. Use {@link #appendM4SourceInformation} instead.
     */
    @Deprecated
    public static void writeM4SourceInformation(Appendable appendable, String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        appendM4SourceInformation(appendable, sourceId, startLine, startColumn, line, column, endLine, endColumn);
    }

    public static <T extends Appendable> T appendM4SourceInformation(T appendable, String sourceId, int startLine, int startColumn, int line, int column, int endLine, int endColumn)
    {
        SafeAppendable.wrap(appendable)
                .append("?[").append(sourceId).append(':')
                .append(startLine).append(',')
                .append(startColumn).append(',')
                .append(line).append(',')
                .append(column).append(',')
                .append(endLine).append(',')
                .append(endColumn).append("]?");
        return appendable;
    }

    /**
     * Compare two positions (assumed to be in the same source). Returns 0 if the two positions are equal. Returns a
     * negative value if the first position is before the second. Returns a positive value if the first position is
     * after the second.
     *
     * @param line1 first position line number
     * @param col1  first position column number
     * @param line2 second position line number
     * @param col2  second position column number
     * @return 0 if the two positions are equal; a negative value if the first position is before the second; a positive value otherwise
     */
    public static int comparePositions(int line1, int col1, int line2, int col2)
    {
        return (line1 < line2) ? -1 : ((line1 > line2) ? 1 : Integer.compare(col1, col2));
    }

    /**
     * Return whether one position is strictly before another position (assumed to be in the same source).
     *
     * @param line1 first position line number
     * @param col1  first position column number
     * @param line2 second position line number
     * @param col2  second position column number
     * @return whether the first position is strictly before the second
     */
    public static boolean isBefore(int line1, int col1, int line2, int col2)
    {
        return (line1 < line2) || ((line1 == line2) && (col1 < col2));
    }

    /**
     * Return whether one position is strictly after another position (assumed to be in the same source).
     *
     * @param line1 first position line number
     * @param col1  first position column number
     * @param line2 second position line number
     * @param col2  second position column number
     * @return whether the first position is strictly after the second
     */
    public static boolean isAfter(int line1, int col1, int line2, int col2)
    {
        return (line1 > line2) || ((line1 == line2) && (col1 > col2));
    }

    /**
     * Return whether one position is not before another position (assumed to be in the same source). This is true if
     * the first position is equal to or after the second position.
     *
     * @param line1 first position line number
     * @param col1  first position column number
     * @param line2 second position line number
     * @param col2  second position column number
     * @return whether the first position is not before the second
     */
    public static boolean isNotBefore(int line1, int col1, int line2, int col2)
    {
        return (line1 > line2) || ((line1 == line2) && (col1 >= col2));
    }

    /**
     * Return whether one position is not after another position (assumed to be in the same source). This is true if
     * the first position is equal to or before the second position.
     *
     * @param line1 first position line number
     * @param col1  first position column number
     * @param line2 second position line number
     * @param col2  second position column number
     * @return whether the first position is not after the second
     */
    public static boolean isNotAfter(int line1, int col1, int line2, int col2)
    {
        return (line1 < line2) || ((line1 == line2) && (col1 <= col2));
    }
}
