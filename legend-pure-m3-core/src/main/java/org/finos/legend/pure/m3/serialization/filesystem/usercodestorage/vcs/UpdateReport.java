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

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;

public class UpdateReport
{
    private final MutableSet<String> added = Sets.mutable.empty();
    private final MutableSet<String> deleted = Sets.mutable.empty();
    private final MutableSet<String> modified = Sets.mutable.empty();
    private final MutableSet<String> replaced = Sets.mutable.empty();
    private final MutableSet<String> conflicted = Sets.mutable.empty();

    public void addAdded(String path)
    {
        this.added.add(path);
    }

    public void addDeleted(String path)
    {
        if (this.added.contains(path))
        {
            this.added.remove(path);
        }
        else
        {
            this.deleted.add(path);
        }
        if (this.modified.contains(path))
        {
            this.modified.remove(path);
        }
        if (this.replaced.contains(path))
        {
            this.replaced.remove(path);
        }
    }

    public void addModified(String path)
    {
        if (!this.added.contains(path) && !this.replaced.contains(path))
        {
            this.modified.add(path);
        }
    }

    public void addReplaced(String path)
    {
        if (!this.added.contains(path))
        {
            this.replaced.add(path);
        }
    }

    public void addConflicted(String path)
    {
        if (!isUpdated(path))
        {
            throw new IllegalArgumentException("Only updated paths may be marked as conflicted: " + path);
        }
        this.conflicted.add(path);
    }

    public MutableSet<String> getAdded()
    {
        return this.added.asUnmodifiable();
    }

    public MutableSet<String> getDeleted()
    {
        return this.deleted.asUnmodifiable();
    }

    public MutableSet<String> getModified()
    {
        return this.modified.asUnmodifiable();
    }

    public MutableSet<String> getReplaced()
    {
        return this.replaced.asUnmodifiable();
    }

    public MutableSet<String> getConflicted()
    {
        return this.conflicted.asUnmodifiable();
    }

    public boolean isUpdated(String path)
    {
        return this.added.contains(path) || this.deleted.contains(path) || this.modified.contains(path) || this.replaced.contains(path);
    }

    public boolean isEmpty()
    {
        return this.added.isEmpty() && this.deleted.isEmpty() && this.modified.isEmpty() && this.replaced.isEmpty() && this.conflicted.isEmpty();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);
        builder.append('<');
        this.added.appendString(builder, "added=[", ", ", "]");
        builder.append(", ");
        this.deleted.appendString(builder, "deleted=[", ", ", "]");
        builder.append(", ");
        this.modified.appendString(builder, "modified=[", ", ", "]");
        builder.append(", ");
        this.replaced.appendString(builder, "replaced=[", ", ", "]");
        builder.append(", ");
        this.conflicted.appendString(builder, "conflicted=[", ", ", "]");
        builder.append('>');
        return builder.toString();
    }
}
