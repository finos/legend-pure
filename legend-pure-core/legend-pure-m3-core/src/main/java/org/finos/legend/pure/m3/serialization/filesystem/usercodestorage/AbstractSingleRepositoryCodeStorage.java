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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;

import java.io.IOException;

public abstract class AbstractSingleRepositoryCodeStorage extends AbstractRepositoryCodeStorage
{
    protected final CodeRepository repository;

    protected AbstractSingleRepositoryCodeStorage(CodeRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public RichIterable<CodeRepository> getAllRepositories()
    {
        return Lists.immutable.with(this.repository);
    }

    @Override
    public CodeRepository getRepository(String name)
    {
        return this.repository.getName().equals(name) ? this.repository : null;
    }

    @Override
    public CodeRepository getRepositoryForPath(String path)
    {
        if (!path.isEmpty() && (path.charAt(0) == '/'))
        {
            int index = path.indexOf('/', 1);
            String rootPath = index != -1 ? path.substring(1, index) : path.substring(1);
            return this.repository.getName().equals(rootPath) ? this.repository : null;
        }
        return null;
    }

    @Override
    protected boolean hasRepo(String repoName)
    {
        return this.repository.getName().equals(repoName);
    }

    protected void writeRepoPath(Appendable appendable)
    {
        try
        {
            appendable.append(RepositoryCodeStorage.ROOT_PATH);
            appendable.append(this.repository.getName());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String getRelativePath(String path)
    {
        return relativizeToRepo(this.repository.getName(), path);
    }
}
