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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

class UnpackedPureRepositoryJar extends AbstractPureRepositoryJar
{
    private final Path root;

    UnpackedPureRepositoryJar(Path root) throws IOException
    {
        super(PureRepositoryJarMetadata.getPureMetadataFromUnpackedJar(root));
        this.root = root;
    }

    @Override
    public byte[] readFile(String filePath)
    {
        try
        {
            return Files.readAllBytes(getRealPath(filePath));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading " + filePath, e);
        }
    }

    @Override
    public void readAllFiles(MutableMap<String, byte[]> fileBytes)
    {
        try (DirectoryStream<Path> rootStream = Files.newDirectoryStream(this.root, entry -> !PureRepositoryJarTools.META_INF_DIR_NAME.equalsIgnoreCase(entry.getFileName().toString())))
        {
            readFilesFromDirectoryStream(rootStream, fileBytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading all files", e);
        }
    }

    @Override
    protected void readFilesFromNonEmptySet(SetIterable<String> filePaths, MutableMap<String, byte[]> fileBytes)
    {
        for (String filePath : filePaths)
        {
            fileBytes.put(filePath, readFile(filePath));
        }
    }

    private void readFilesFromDirectoryStream(DirectoryStream<Path> directoryStream, MutableMap<String, byte[]> fileBytes) throws IOException
    {
        for (Path entry : directoryStream)
        {
            if (Files.isDirectory(entry))
            {
                try (DirectoryStream<Path> subdirectoryStream = Files.newDirectoryStream(entry))
                {
                    readFilesFromDirectoryStream(subdirectoryStream, fileBytes);
                }
            }
            else
            {
                fileBytes.put(getVirtualPath(entry), Files.readAllBytes(entry));
            }
        }
    }

    private Path getRealPath(String filePath)
    {
        return this.root.resolve(filePath);
    }

    private String getVirtualPath(Path realPath)
    {
        String virtualPath = this.root.relativize(realPath).toString();
        String separator = realPath.getFileSystem().getSeparator();
        return "/".equals(separator) ? virtualPath : virtualPath.replace(separator, "/");
    }
}
