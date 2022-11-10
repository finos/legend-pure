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
import org.eclipse.collections.impl.utility.Iterate;

import java.util.ServiceLoader;

public class CodeRepositoryProviderHelper
{
    /**
     * Find all code repositories accessible via a {@linkplain CodeRepositoryProvider}.
     *
     * @return all code repositories
     */
    public static RichIterable<CodeRepository> findCodeRepositories()
    {
        return findCodeRepositories(false);
    }

    /**
     * Find all code repositories accessible via a {@linkplain CodeRepositoryProvider}. If refresh is true, then any
     * provider caches will be cleared.
     *
     * @param refresh whether to refresh caches
     * @return all code repositories
     */
    public static RichIterable<CodeRepository> findCodeRepositories(boolean refresh)
    {
        return getRepositories(ServiceLoader.load(CodeRepositoryProvider.class), refresh);
    }

    /**
     * Find all code repositories accessible via a {@linkplain CodeRepositoryProvider} in the given class loader.
     *
     * @param classLoader class loader
     * @return all code repositories
     */
    public static RichIterable<CodeRepository> findCodeRepositories(ClassLoader classLoader)
    {
        return findCodeRepositories(classLoader, false);
    }

    /**
     * Find all code repositories accessible via a {@linkplain CodeRepositoryProvider} in the given class loader. If
     * refresh is true, then any provider caches will be cleared.
     *
     * @param classLoader class loader
     * @param refresh     whether to refresh caches
     * @return all code repositories
     */
    public static RichIterable<CodeRepository> findCodeRepositories(ClassLoader classLoader, boolean refresh)
    {
        return getRepositories(ServiceLoader.load(CodeRepositoryProvider.class, classLoader), refresh);
    }

    private static RichIterable<CodeRepository> getRepositories(ServiceLoader<CodeRepositoryProvider> serviceLoader, boolean reload)
    {
        if (reload)
        {
            serviceLoader.reload();
        }
        return Iterate.collect(serviceLoader, CodeRepositoryProvider::repository, Lists.mutable.empty());
    }

    public static boolean isCoreRepository(CodeRepository codeRepository)
    {
        return codeRepository != null && codeRepository.getName().startsWith("core");
    }
}
