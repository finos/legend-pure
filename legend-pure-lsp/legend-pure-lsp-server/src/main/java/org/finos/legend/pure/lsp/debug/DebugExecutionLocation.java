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

package org.finos.legend.pure.lsp.debug;

class DebugExecutionLocation
{
    private final String sourceId;
    private final String uri;
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;
    private final String name;
    private final int stackDepth;
    private final long ordinal;

    DebugExecutionLocation(String sourceId, String uri, int line, int column, int endLine, int endColumn,
                           String name, int stackDepth, long ordinal)
    {
        this.sourceId = sourceId;
        this.uri = uri;
        this.line = positiveOrDefault(line, 1);
        this.column = positiveOrDefault(column, 1);
        this.endLine = positiveOrDefault(endLine, this.line);
        this.endColumn = positiveOrDefault(endColumn, this.column);
        this.name = (name == null || name.isEmpty()) ? "Pure debug point" : name;
        this.stackDepth = Math.max(stackDepth, 0);
        this.ordinal = ordinal;
    }

    String getSourceId()
    {
        return this.sourceId;
    }

    String getUri()
    {
        return this.uri;
    }

    int getLine()
    {
        return this.line;
    }

    int getColumn()
    {
        return this.column;
    }

    int getEndLine()
    {
        return this.endLine;
    }

    int getEndColumn()
    {
        return this.endColumn;
    }

    String getName()
    {
        return this.name;
    }

    int getStackDepth()
    {
        return this.stackDepth;
    }

    long getOrdinal()
    {
        return this.ordinal;
    }

    boolean containsLine(int oneBasedLine)
    {
        return oneBasedLine >= this.line && oneBasedLine <= this.endLine;
    }

    boolean sameLine(DebugExecutionLocation other)
    {
        return other != null
                && this.line == other.line
                && safeEquals(this.sourceId, other.sourceId);
    }

    boolean sameRange(DebugExecutionLocation other)
    {
        return other != null
                && this.line == other.line
                && this.column == other.column
                && this.endLine == other.endLine
                && this.endColumn == other.endColumn
                && safeEquals(this.sourceId, other.sourceId);
    }

    private static int positiveOrDefault(int value, int fallback)
    {
        return value > 0 ? value : fallback;
    }

    private static boolean safeEquals(Object one, Object two)
    {
        return one == null ? two == null : one.equals(two);
    }
}
