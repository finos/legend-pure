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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;

import java.util.Comparator;

public class RepositoryComparator implements Comparator<String>
{
    private final ObjectIntMap<String> index;

    public RepositoryComparator(RichIterable<? extends CodeRepository> repositories)
    {
        this.index = buildIndex(repositories);
    }

    @Override
    public int compare(String repo1Name, String repo2Name)
    {
        return Integer.compare(getRepoIndex(repo1Name), getRepoIndex(repo2Name));
    }

    private int getRepoIndex(String repoName)
    {
        // null is last
        if (repoName == null)
        {
            return this.index.size();
        }

        int index = this.index.getIfAbsent(repoName, -1);
        if (index == -1)
        {
            throw new IllegalArgumentException("Unknown repository: " + repoName);
        }
        return index;
    }

    private static ObjectIntMap<String> buildIndex(Iterable<? extends CodeRepository> repositories)
    {
        MutableList<? extends CodeRepository> orderedRepositories = CodeRepository.toSortedRepositoryList(repositories);
        MutableObjectIntMap<String> index = ObjectIntMaps.mutable.ofInitialCapacity(orderedRepositories.size());
        orderedRepositories.forEachWithIndex((r, i) -> index.put(r.getName(), i));
        return index;
    }
}
