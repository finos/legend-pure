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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.*;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.tools.FileTools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FSCodeStorage extends AbstractSingleRepositoryCodeStorage
{
    protected final Path root;

    public FSCodeStorage(CodeRepository repository, Path root)
    {
        super(repository);
        this.root = root;
    }

    public Path getRoot()
    {
        return this.root;
    }

    @Override
    public void initialize(Message message)
    {
        // Do nothing
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        Path fullPath = getFullPath(path);
        if (Files.notExists(fullPath))
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        return getCodeStorageNode(fullPath);
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        Path fullPath = getFullPath(path);
        if (Files.notExists(fullPath))
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        if (Files.isDirectory(fullPath))
        {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(fullPath))
            {
                MutableList<CodeStorageNode> nodes = Lists.mutable.empty();
                for (Path dirEntry : dirStream)
                {
                    nodes.add(getCodeStorageNode(dirEntry));
                }
                return nodes;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error getting files for " + path, e);
            }
        }
        else
        {
            return Lists.immutable.with(getCodeStorageNode(fullPath));
        }
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        MutableList<String> result = Lists.mutable.with();
        try
        {
            getFilesRecursive(this.root, result);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting user files", e);
        }
        return result;
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        MutableList<String> result = Lists.mutable.with();
        Path base = getFullPath(path);
        if (Files.isDirectory(base))
        {
            try
            {
                getFilesRecursive(base, result);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error getting files for " + path, e);
            }
        }
        else
        {
            processFile(base, result);
        }
        return result;
    }

    @Override
    public InputStream getContent(String path)
    {
        try
        {
            return Files.newInputStream(getFullPath(path));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content for: " + path, e);
        }
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        try
        {
            return Files.readAllBytes(getFullPath(path));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content as bytes for " + path, e);
        }
    }

    @Override
    public String getContentAsText(String path)
    {
        try
        {
            return new String(Files.readAllBytes(getFullPath(path)), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content as text for " + path, e);
        }
    }

    @Override
    public boolean exists(String path)
    {
        return Files.exists(getFullPath(path));
    }

    @Override
    public boolean isFile(String path)
    {
        return Files.isRegularFile(getFullPath(path));
    }

    @Override
    public boolean isFolder(String path)
    {
        return Files.isDirectory(getFullPath(path));
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        Path file = getFullPath(path);
        try
        {
            return Files.isDirectory(file) && FileTools.isDirectoryEmpty(file);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String getUserPath(Path path)
    {
        Path relativePath = this.root.relativize(path);
        StringBuilder builder = new StringBuilder(relativePath.getNameCount() * 24);
        writeRepoPath(builder);
        for (Path element : relativePath)
        {
            builder.append(RepositoryCodeStorage.PATH_SEPARATOR);
            builder.append(element);
        }
        return builder.toString();
    }

    protected Path getFullPath(String path)
    {
        return this.root.resolve(getRelativePath(path));
    }

    protected CodeStorageNode getCodeStorageNode(Path path)
    {
        return new FSCodeStorageNode(path);
    }

    protected void getFilesRecursive(Path dir, MutableCollection<String> result) throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir))
        {
            for (Path dirEntry : dirStream)
            {
                if (Files.isDirectory(dirEntry))
                {
                    getFilesRecursive(dirEntry, result);
                }
                else
                {
                    processFile(dirEntry, result);
                }
            }
        }
    }

    private void processFile(Path file, MutableCollection<String> result)
    {
        if (CodeStorageTools.hasPureFileExtension(file.toString()))
        {
            result.add(getUserPath(file));
        }
    }

    protected class FSCodeStorageNode implements CodeStorageNode
    {
        private final Path path;

        protected FSCodeStorageNode(Path path)
        {
            this.path = path;
        }

        @Override
        public boolean isDirectory()
        {
            return Files.isDirectory(this.path);
        }

        @Override
        public String getName()
        {
            return this.path.getFileName().toString();
        }

        @Override
        public String getPath()
        {
            return getUserPath(this.path);
        }

        @Override
        public CodeStorageNodeStatus getStatus()
        {
            return CodeStorageNodeStatus.UNKNOWN;
        }
    }
}
