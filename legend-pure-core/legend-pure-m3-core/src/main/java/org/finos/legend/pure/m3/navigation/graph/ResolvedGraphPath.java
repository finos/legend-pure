// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.navigation.graph;

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ResolvedGraphPath
{
    private final GraphPath path;
    private final ImmutableList<CoreInstance> resolvedNodes;

    ResolvedGraphPath(GraphPath path, ImmutableList<CoreInstance> resolvedNodes)
    {
        this.path = path;
        this.resolvedNodes = resolvedNodes;
    }

    public GraphPath getGraphPath()
    {
        return this.path;
    }

    public ImmutableList<CoreInstance> getResolvedNodes()
    {
        return this.resolvedNodes;
    }

    public CoreInstance getLastResolvedNode()
    {
        return this.resolvedNodes.getLast();
    }

    public boolean startsWith(ResolvedGraphPath other)
    {
        if (this == other)
        {
            return true;
        }
        return this.path.startsWith(other.path) &&
                other.resolvedNodes.equals((other.resolvedNodes.size() < this.resolvedNodes.size()) ? this.resolvedNodes.subList(0, other.resolvedNodes.size()) : this.resolvedNodes);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof ResolvedGraphPath))
        {
            return false;
        }
        ResolvedGraphPath that = (ResolvedGraphPath) other;
        return this.path.equals(that.path) && this.resolvedNodes.equals(that.resolvedNodes);
    }

    @Override
    public int hashCode()
    {
        return this.path.hashCode();
    }

    @Override
    public String toString()
    {
        return this.path.writeDescription(new StringBuilder("<").append(getClass().getSimpleName()).append(" path=")).append('>').toString();
    }
}
