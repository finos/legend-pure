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
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class PureRepositoriesExternal
{
    private static final ConcurrentMutableMap<String, CodeRepository> REPOSITORIES_BY_NAME = ConcurrentHashMap.newMap();

    static
    {
        refresh();
    }

    public static void refresh()
    {
        synchronized (REPOSITORIES_BY_NAME)
        {
            REPOSITORIES_BY_NAME.clear();
            addRepository(CodeRepository.newPlatformCodeRepository());
            addRepositories(CodeRepositoryProviderHelper.findCodeRepositories());
        }
    }

    public static RichIterable<CodeRepository> repositories()
    {
        synchronized (REPOSITORIES_BY_NAME)
        {
            return REPOSITORIES_BY_NAME.valuesView();
        }
    }

    public static CodeRepository getRepository(String repositoryName)
    {
        synchronized (REPOSITORIES_BY_NAME)
        {
            CodeRepository found = REPOSITORIES_BY_NAME.get(repositoryName);
            if (found == null)
            {
                throw new RuntimeException("The code repository '" + repositoryName + "' can't be found!");
            }
            return found;
        }
    }

    public static void addRepositories(Iterable<? extends CodeRepository> repositories)
    {
        repositories.forEach(PureRepositoriesExternal::addRepository);
        LazyIterate.selectInstancesOf(repositories, GenericCodeRepository.class).forEach(PureRepositoriesExternal::validate);
    }

    private static void addRepository(CodeRepository repository)
    {
        synchronized (REPOSITORIES_BY_NAME)
        {
            if (REPOSITORIES_BY_NAME.putIfAbsent(repository.getName(), repository) != null)
            {
                throw new RuntimeException("The code repository " + repository.getName() + " already exists!");
            }
        }
    }

    private static void validate(GenericCodeRepository codeRepo)
    {
        SetIterable<String> missingDependencies = codeRepo.getDependencies().reject(REPOSITORIES_BY_NAME::containsKey);
        if (missingDependencies.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The ").append((missingDependencies.size() == 1) ? "dependency" : "dependencies").append(" ");
            missingDependencies.appendString(builder, "'", "', '", "'");
            builder.append(" required by the Code Repository '").append(codeRepo.getName()).append("' can't be found!");
            throw new RuntimeException(builder.toString());
        }
    }
}
