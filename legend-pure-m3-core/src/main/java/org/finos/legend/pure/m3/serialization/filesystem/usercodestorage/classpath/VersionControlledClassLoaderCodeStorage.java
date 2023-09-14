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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryRevisionCache;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.VersionControlledCodeStorage;

import java.util.List;
import java.util.Optional;


public class VersionControlledClassLoaderCodeStorage extends ClassLoaderCodeStorage implements VersionControlledCodeStorage
{
    private final RepositoryRevisionCache repositoryRevisionCache;

    public VersionControlledClassLoaderCodeStorage(ClassLoader classLoader, Iterable<? extends CodeRepository> repositories, RepositoryRevisionCache repositoryRevisionCache)
    {
        super(classLoader, repositories);
        this.repositoryRevisionCache = repositoryRevisionCache;
    }

    public VersionControlledClassLoaderCodeStorage(ClassLoader classLoader, CodeRepository repository, RepositoryRevisionCache repositoryRevisionCache)
    {
        super(classLoader, repository);
        this.repositoryRevisionCache = repositoryRevisionCache;
    }

    public VersionControlledClassLoaderCodeStorage(Iterable<? extends CodeRepository> repositories, RepositoryRevisionCache repositoryRevisionCache)
    {
        this(null, repositories, repositoryRevisionCache);
    }

    public VersionControlledClassLoaderCodeStorage(CodeRepository repository, RepositoryRevisionCache repositoryRevisionCache)
    {
        this(null, repository, repositoryRevisionCache);
    }

    @Override
    public boolean isVersioned(String path)
    {
        return findNode(path) != null;
    }

    @Override
    public Optional<String> getCurrentRevision(String path)
    {
        return isVersioned(path) ? PureModelVersion.PURE_MODEL_VERSION : Optional.empty();
    }

    @Override
    public List<String> getAllRevisions(String path)
    {
        if (!isVersioned(path))
        {
            return Lists.mutable.empty();
        }
        if (this.repositoryRevisionCache == null)
        {
            throw new IllegalStateException("getAllRevisions operation not available without a PureRepositoryRevisionCache");
        }
        return this.repositoryRevisionCache.getAllRevisions(path);
    }

    @Override
    public RichIterable<Revision> getAllRevisionLogs(RichIterable<String> paths)
    {
        MutableSet<String> versionedPaths = Sets.mutable.empty();
        for (String path : paths)
        {
            if (isVersioned(path))
            {
                versionedPaths.add(path);
            }
        }
        if (versionedPaths.isEmpty() && paths.notEmpty())
        {
            return Lists.immutable.empty();
        }
        if (this.repositoryRevisionCache == null)
        {
            throw new IllegalStateException("getAllRevisionLogs operation not available without a PureRepositoryRevisionCache");
        }
        return this.repositoryRevisionCache.getRevisionsByPath(paths);
    }

    @Override
    public String getDiff(RichIterable<String> paths)
    {
        return "";
    }

    @Override
    public RichIterable<CodeStorageNode> getModifiedUserFiles()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<CodeStorageNode> getUnversionedFiles()
    {
        return Lists.immutable.empty();
    }
}
