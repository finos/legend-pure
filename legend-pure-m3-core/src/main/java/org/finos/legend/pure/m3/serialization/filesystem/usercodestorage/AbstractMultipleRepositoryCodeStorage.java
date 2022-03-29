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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;

public abstract class AbstractMultipleRepositoryCodeStorage extends AbstractRepositoryCodeStorage
{
    protected final ImmutableMap<String, CodeRepository> repositories;

    protected AbstractMultipleRepositoryCodeStorage(Iterable<? extends CodeRepository> repositories)
    {
        this.repositories = indexRepositories(repositories);
    }

    protected AbstractMultipleRepositoryCodeStorage(CodeRepository... repositories)
    {
        this(ArrayAdapter.adapt(repositories));
    }

    protected AbstractMultipleRepositoryCodeStorage(CodeRepository repository)
    {
        this.repositories = Maps.immutable.with(repository.getName(), repository);
    }

    @Override
    public RichIterable<CodeRepository> getRepositories()
    {
        return this.repositories.valuesView();
    }

    protected RichIterable<String> getRepositoryNames()
    {
        return this.repositories.keysView();
    }

    protected CodeRepository getRepositoryByName(String name)
    {
        return this.repositories.get(name);
    }

    @Override
    protected boolean hasRepo(String name)
    {
        return this.repositories.containsKey(name);
    }

    private static ImmutableMap<String, CodeRepository> indexRepositories(Iterable<? extends CodeRepository> repositories)
    {
        MutableMap<String, CodeRepository> index = Maps.mutable.empty();
        repositories.forEach(repository ->
        {
            CodeRepository old = index.put(repository.getName(), repository);
            if ((old != null) && (old != repository))
            {
                throw new IllegalArgumentException("Name conflict for " + repository.getName());
            }
        });
        return index.toImmutable();
    }
}
