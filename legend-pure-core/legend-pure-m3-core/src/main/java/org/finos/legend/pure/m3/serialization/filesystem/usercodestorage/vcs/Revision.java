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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.Comparators;

import java.util.Date;

public class Revision
{
    public static final Function<Revision, String> GET_REVISION = new Function<Revision, String>()
    {
        @Override
        public String valueOf(Revision revision)
        {
            return revision.getRevision();
        }
    };

    private final String revision;
    private ImmutableList<ChangedPath> changedPaths;
    private final String author;
    private final String message;
    private final Date date;

    public Revision(String revision, String author, Date date, String message)
    {
        this.revision = revision;
        this.author = author.trim();
        this.date = date;
        this.message = message;
    }

    public RichIterable<ChangedPath> getChangedPaths()
    {
        return this.changedPaths;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public Date getDate()
    {
        return this.date;
    }

    public String getMessage()
    {
        return this.message;
    }

    public String getRevision()
    {
        return this.revision;
    }

    public void setChangedPaths(ImmutableList<ChangedPath> changedPaths)
    {
        this.changedPaths = changedPaths;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof Revision))
        {
            return false;
        }

        Revision otherRevision = (Revision)other;
        return (this.revision == otherRevision.revision) &&
                Comparators.nullSafeEquals(this.author, otherRevision.author) &&
                Comparators.nullSafeEquals(this.date, otherRevision.date) &&
                Comparators.nullSafeEquals(this.message, otherRevision.message) &&
                Comparators.nullSafeEquals(this.changedPaths, otherRevision.changedPaths);
    }

    @Override
    public int hashCode()
    {
        return this.revision.hashCode();
    }

}
