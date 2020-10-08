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
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Sets;

public class VCSPath
{
    public static final Function<VCSPath, RichIterable<Revision>> GET_REVISIONS = new Function<VCSPath, RichIterable<Revision>>()
    {
        @Override
        public RichIterable<Revision> valueOf(VCSPath path)
        {
            return path.getRevisions();
        }
    };

    private final String path;
    private boolean deleted;
    private boolean copied;
    private MutableSet<Revision> revisions = Sets.mutable.empty();

    public VCSPath(String path)
    {
        this.path = path;
        this.deleted = false;
        this.copied = false;
    }

    public VCSPath(String newPath, VCSPath path)
    {
        this.path = newPath;
        this.deleted = false;
        this.copied = false;
        this.revisions = Sets.mutable.withAll(path.revisions);
    }

    public String getPath()
    {
        return this.path;
    }

    public void addRevision(Revision revision)
    {
        this.revisions.add(revision);
    }

    public RichIterable<Revision> getRevisions()
    {
        return this.revisions.asUnmodifiable();
    }

    public void setDeleted()
    {
        this.deleted = true;
        if (this.copied)
        {
            this.revisions = Sets.mutable.empty();
        }
    }

    public void setCopied()
    {
        this.copied = true;
        if (this.deleted)
        {
            this.revisions = Sets.mutable.empty();
        }
    }

    public boolean isDeleted()
    {
        return this.deleted;
    }

    public boolean isCopied()
    {
        return this.copied;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof VCSPath))
        {
            return false;
        }

        VCSPath otherPath = (VCSPath)other;
        return Comparators.nullSafeEquals(this.path, otherPath.path) &&
                this.copied == otherPath.copied &&
                this.deleted == otherPath.deleted &&
                this.revisions.equals(otherPath.revisions);
    }

    @Override
    public int hashCode()
    {
        return this.path.hashCode() * 37 + this.revisions.hashCode();
    }
}
