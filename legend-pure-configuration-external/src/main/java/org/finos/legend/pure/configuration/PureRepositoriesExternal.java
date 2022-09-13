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

package org.finos.legend.pure.configuration;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class PureRepositoriesExternal
{
    private static final ThreadLocal<MutableMap<String, CodeRepository>> REPOSITORIES_BY_NAME = ThreadLocal.withInitial(PureRepositoriesExternal::buildRepositoryMap);

    public static void refresh()
    {
        REPOSITORIES_BY_NAME.remove();
    }

    public static RichIterable<CodeRepository> repositories()
    {
        return REPOSITORIES_BY_NAME.get().valuesView();
    }

    public static CodeRepository getRepository(String repositoryName)
    {
        CodeRepository found = REPOSITORIES_BY_NAME.get().get(repositoryName);
        if (found == null)
        {
            throw new RuntimeException("The code repository '" + repositoryName + "' can't be found!");
        }
        return found;
    }

    public static void addRepositories(Iterable<? extends CodeRepository> repositories)
    {
        // Index new repositories by name
        MutableMap<String, CodeRepository> newRepositoriesByName = Maps.mutable.empty();
        repositories.forEach(r -> addRepository(newRepositoriesByName, r));

        // Validate dependencies and name conflicts with existing repositories
        MutableMap<String, CodeRepository> repositoriesByName = REPOSITORIES_BY_NAME.get();
        newRepositoriesByName.forEachKeyValue((name, newRepo) ->
        {
            if (repositoriesByName.containsKey(name))
            {
                throw new RuntimeException("The code repository " + name + " already exists!");
            }
            if (newRepo instanceof GenericCodeRepository)
            {
                MutableList<String> missingDependencies = ((GenericCodeRepository) newRepo).getDependencies().reject(d -> newRepositoriesByName.containsKey(d) || repositoriesByName.containsKey(d), Lists.mutable.empty());
                if (missingDependencies.notEmpty())
                {
                    StringBuilder builder = new StringBuilder("The ").append((missingDependencies.size() == 1) ? "dependency" : "dependencies").append(" ");
                    missingDependencies.sortThis().appendString(builder, "'", "', '", "'");
                    builder.append(" required by the Code Repository '").append(newRepo.getName()).append("' can't be found!");
                    throw new RuntimeException(builder.toString());
                }
            }
        });

        // Add new repositories to existing
        repositoriesByName.putAll(newRepositoriesByName);
    }

    private static MutableMap<String, CodeRepository> buildRepositoryMap()
    {
        MutableMap<String, CodeRepository> repositoriesByName = Maps.mutable.empty();
        addRepository(repositoriesByName, CodeRepository.newPlatformCodeRepository());
        CodeRepositoryProviderHelper.findCodeRepositories(Thread.currentThread().getContextClassLoader())
                .forEach(r -> addRepository(repositoriesByName, r));
        return repositoriesByName;
    }

    private static void addRepository(MutableMap<String, CodeRepository> repositoriesByName, CodeRepository repository)
    {
        CodeRepository old = repositoriesByName.put(repository.getName(), repository);
        if ((old != null) && (old != repository))
        {
            throw new RuntimeException("The code repository " + repository.getName() + " already exists!");
        }
    }
}
