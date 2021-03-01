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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

import java.util.ServiceLoader;

public class PureRepositoriesExternal
{
    public static MutableMap<String, CodeRepository> repositoriesByName = Maps.mutable.empty();

    static
    {
        MutableList<CodeRepository> repositories = FastList.newListWith(CodeRepository.newPlatformCodeRepository());
        for (CodeRepositoryProvider codeRepositoryProvider : ServiceLoader.load(CodeRepositoryProvider.class))
        {
            repositories.add(codeRepositoryProvider.repository());
        }
        addRepositories(repositories);
    }

    public static RichIterable<CodeRepository> repositories()
    {
        return repositoriesByName.valuesView();
    }

    public static void addRepositories(RichIterable<CodeRepository> repositories)
    {
        repositories.forEach(repository -> {
            if (repositoriesByName.put(repository.getName(), repository) != null)
            {
                throw new RuntimeException("The code repository " + repository.getName() + " already exists!");
            }
        });
        repositories.select(r -> r instanceof GenericCodeRepository).forEach(r -> validate((GenericCodeRepository) r));
    }

    public static CodeRepository getRepository(String repo)
    {
        CodeRepository found = repositoriesByName.get(repo);
        if (found == null)
        {
            throw new RuntimeException("The code repository '" + repo + "' can't be found!");
        }
        return found;
    }

    private static void validate(GenericCodeRepository codeRepo)
    {
        codeRepo.getDependencies().forEach(d -> {
                    if (!repositoriesByName.containsKey(d))
                    {
                        throw new RuntimeException("The dependency '" + d + "' required by the Code Repository '" + codeRepo.getName() + "' can't be found!");
                    }
                }
        );
    }
}
