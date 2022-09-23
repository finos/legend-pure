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
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;

@Deprecated
public class PureRepositoriesExternal
{
    private static final ThreadLocal<CodeRepositorySet> REPO_SET_HOLDER = ThreadLocal.withInitial(PureRepositoriesExternal::buildSet);

    @Deprecated
    public static void refresh()
    {
        REPO_SET_HOLDER.remove();
    }

    @Deprecated
    public static RichIterable<CodeRepository> repositories()
    {
        return REPO_SET_HOLDER.get().getRepositories();
    }

    @Deprecated
    public static CodeRepository getRepository(String repositoryName)
    {
        return REPO_SET_HOLDER.get().getRepository(repositoryName);
    }

    @Deprecated
    public static void addRepositories(Iterable<? extends CodeRepository> repositories)
    {
        CodeRepositorySet newManager = CodeRepositorySet.newBuilder(REPO_SET_HOLDER.get())
                .withCodeRepositories(repositories)
                .build();
        REPO_SET_HOLDER.set(newManager);
    }

    private static CodeRepositorySet buildSet()
    {
        return CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true)).build();
    }
}
