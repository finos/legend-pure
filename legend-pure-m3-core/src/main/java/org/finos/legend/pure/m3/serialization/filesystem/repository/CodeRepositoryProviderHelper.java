// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.ServiceLoader;

public class CodeRepositoryProviderHelper
{
    public static RichIterable<CodeRepository> findCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.empty();
        ServiceLoader<CodeRepositoryProvider> serviceLoader = ServiceLoader.load(CodeRepositoryProvider.class);
        serviceLoader.reload();
        for (CodeRepositoryProvider codeRepositoryProvider : serviceLoader)
        {
            repositories.add(codeRepositoryProvider.repository());
        }
        return repositories;
    }

    public static boolean isCoreRepository(CodeRepository codeRepository)
    {
        return codeRepository != null && codeRepository.getName().startsWith("core");
    }
}
