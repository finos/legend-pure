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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.block.factory.Comparators;

public class SourceCoordinates
{
    public static final Function<SourceCoordinates, String> SOURCE_ID = new Function<SourceCoordinates, String>()
    {
        @Override
        public String valueOf(SourceCoordinates sourceCoordinates)
        {
            return sourceCoordinates.getSourceId();
        }
    };

    private final String sourceId;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;

    SourceCoordinates(String sourceId, int startLine, int startColumn, int endLine, int endColumn)
    {
        this.sourceId = sourceId;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    @Override
    public int hashCode()
    {
        int hash = (this.sourceId == null) ? 0 : this.sourceId.hashCode();
        hash = 31 * hash + this.startLine;
        hash = 31 * hash + this.startColumn;
        hash = 31 * hash + this.endLine;
        hash = 31 * hash + this.endColumn;
        return hash;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof SourceCoordinates))
        {
            return false;
        }

        SourceCoordinates otherSC = (SourceCoordinates)other;
        return Comparators.nullSafeEquals(this.sourceId, otherSC.sourceId) &&
                (this.startLine == otherSC.startLine) &&
                (this.startColumn == otherSC.startColumn) &&
                (this.endLine == otherSC.endLine) &&
                (this.endColumn == otherSC.endColumn);
    }

    @Override
    public String toString()
    {
        return "<SourceCoordinates \"" + this.sourceId + "\" " + this.startLine + ":" + this.startColumn + "-" + this.endLine + ":" + this.endColumn + ">";
    }

    public String getSourceId()
    {
        return this.sourceId;
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
}
