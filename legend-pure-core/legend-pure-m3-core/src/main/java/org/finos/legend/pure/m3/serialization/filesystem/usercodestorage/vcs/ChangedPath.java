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

import java.util.Objects;

public class ChangedPath
{
    private final Revision revision;
    private final String path;
    private final ChangeType type;
    private final String copyPath;
    private final long copyRevision;

    public ChangedPath(Revision revision, String path, ChangeType type, String copyPath, long copyRevision)
    {
        this.revision = revision;
        this.path = path;
        this.type = type;
        this.copyPath = copyPath;
        this.copyRevision = copyRevision;
    }

    public Revision getRevision()
    {
        return this.revision;
    }

    public String getCopyPath()
    {
        return this.copyPath;
    }

    public String getPath()
    {
        return this.path;
    }

    public ChangeType getType()
    {
        return this.type;
    }

    public long getCopyRevision()
    {
        return this.copyRevision;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ChangedPath))
        {
            return false;
        }

        ChangedPath otherChangedPath = (ChangedPath)other;
        return (this.revision == otherChangedPath.revision) &&
                Objects.equals(this.path, otherChangedPath.path) &&
                Objects.equals(this.type, otherChangedPath.type) &&
                Objects.equals(this.copyPath, otherChangedPath.copyPath) &&
                (this.copyRevision == otherChangedPath.copyRevision);
    }

    @Override
    public int hashCode()
    {
        return (this.path.hashCode() * 37) + this.type.hashCode();
    }
}