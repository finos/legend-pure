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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.empty;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.AbstractMultipleRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.InputStream;

public class EmptyCodeStorage extends AbstractMultipleRepositoryCodeStorage
{
    public EmptyCodeStorage(Iterable<? extends CodeRepository> repositories)
    {
        super(repositories);
    }

    public EmptyCodeStorage(CodeRepository... repositories)
    {
        super(repositories);
    }

    public EmptyCodeStorage(CodeRepository repository)
    {
        super(repository);
    }

    @Override
    public void initialize(Message message)
    {
        // Do nothing
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        return new EmptyRepoRootDirectoryNode(repoName);
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        return Lists.immutable.empty();
    }

    @Override
    public InputStream getContent(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        else
        {
            throw new IllegalArgumentException("Not a file: " + path);
        }
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        else
        {
            throw new IllegalArgumentException("Not a file: " + path);
        }
    }

    @Override
    public String getContentAsText(String path)
    {
        String repoName = resolveToRepoName(path);
        if (repoName == null)
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        else
        {
            throw new IllegalArgumentException("Not a file: " + path);
        }
    }

    @Override
    public boolean exists(String path)
    {
        return resolveToRepoName(path) != null;
    }

    @Override
    public boolean isFile(String path)
    {
        return false;
    }

    @Override
    public boolean isFolder(String path)
    {
        return resolveToRepoName(path) != null;
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        return resolveToRepoName(path) != null;
    }

    private String resolveToRepoName(String path)
    {
        String nameWithoutSlash = path.startsWith(RepositoryCodeStorage.ROOT_PATH) ? path.substring(RepositoryCodeStorage.ROOT_PATH.length()) : path;
        return this.repositories.containsKey(nameWithoutSlash) ? nameWithoutSlash : null;
    }

    private static class EmptyRepoRootDirectoryNode implements CodeStorageNode
    {
        private final String repoName;

        private EmptyRepoRootDirectoryNode(String repoName)
        {
            this.repoName = repoName;
        }

        @Override
        public boolean isDirectory()
        {
            return true;
        }

        @Override
        public String getName()
        {
            return this.repoName;
        }

        @Override
        public String getPath()
        {
            return CodeStorageTools.canonicalizePath(this.repoName);
        }

        @Override
        public CodeStorageNodeStatus getStatus()
        {
            return CodeStorageNodeStatus.NORMAL;
        }

        @Override
        public long lastModified()
        {
            return 0;
        }
    }
}
