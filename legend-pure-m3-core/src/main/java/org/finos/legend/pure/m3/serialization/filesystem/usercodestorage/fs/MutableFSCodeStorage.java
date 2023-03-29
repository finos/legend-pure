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

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MutableFSCodeStorage extends FSCodeStorage implements MutableRepositoryCodeStorage
{
    public MutableFSCodeStorage(CodeRepository repository, Path root)
    {
        super(repository, root);
    }

    @Override
    public void initialize(Message message)
    {
        try
        {
            if (Files.notExists(this.root))
            {
                if (message != null)
                {
                    message.setMessage("Initializing repository:<BR>'" + this.repository.getName() + "'");
                }
                Files.createDirectories(this.root);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error initializing /" + this.repository.getName(), e);
        }
    }

    @Override
    public OutputStream writeContent(String path)
    {
        try
        {
            return Files.newOutputStream(getFullPath(path));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to get output stream for: " + path, e);
        }
    }

    @Override
    public void writeContent(String path, String content)
    {
        try
        {
            Files.write(getFullPath(path), content.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to write content to: " + path, e);
        }
    }

    @Override
    public void createFile(String filePath)
    {
        Path fullPath = getFullPath(filePath);
        try
        {
            Files.createDirectories(fullPath.getParent());
            Files.createFile(fullPath);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to create file: " + filePath, e);
        }
    }

    @Override
    public void createFolder(String folderPath)
    {
        Path fullPath = getFullPath(folderPath);
        try
        {
            Files.createDirectories(fullPath);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to create folder: " + folderPath, e);
        }
    }

    @Override
    public void deleteFile(String filePath)
    {
        Path fullPath = getFullPath(filePath);
        try
        {
            Files.delete(fullPath);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to delete file: " + filePath, e);
        }
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath)
    {
        Path fullSourcePath = getFullPath(sourcePath);
        if(fullSourcePath ==  null)
        {
            throw new RuntimeException("Source Path is null");
        }

        Path fullDestinationPath = getFullPath(destinationPath);
        if(fullDestinationPath ==  null)
        {
            throw new RuntimeException("Destination Path is null");
        }

        try
        {
            Files.move(fullSourcePath, fullDestinationPath);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error trying to move file: '" + sourcePath + "' to '" + destinationPath + "'", e);
        }
    }
}
