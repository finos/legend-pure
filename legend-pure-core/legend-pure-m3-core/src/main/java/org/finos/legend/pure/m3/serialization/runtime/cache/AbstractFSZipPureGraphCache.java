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

package org.finos.legend.pure.m3.serialization.runtime.cache;

import org.finos.legend.pure.m3.tools.FileTools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

abstract class AbstractFSZipPureGraphCache extends AbstractPureGraphCache implements FSPureGraphCache
{
    private final Path cachePath;
    private final int compressionLevel;

    protected AbstractFSZipPureGraphCache(Path cachePath, int compressionLevel)
    {
        this.cachePath = cachePath;
        this.compressionLevel = compressionLevel;
        try
        {
            Files.createDirectories(this.cachePath.getParent());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Could not create cache directory: " + this.cachePath.getParent(), e);
        }
    }

    @Override
    public void clearCaches()
    {
        if (Files.exists(this.cachePath))
        {
            try
            {
                FileTools.delete(this.cachePath);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error deleting caches", e);
            }
        }
    }

    @Override
    public Path getCacheLocation()
    {
        return this.cachePath;
    }

    @Override
    public CacheType getCacheType()
    {
        return CacheType.ZIP;
    }

    @Override
    protected boolean cacheExists()
    {
        return Files.exists(this.cachePath);
    }

    @Override
    protected long getCacheSize()
    {
        if (Files.notExists(this.cachePath))
        {
            return -1L;
        }

        try
        {
            return Files.size(this.cachePath);
        }
        catch (Exception e)
        {
            return -1L;
        }
    }

    protected ZipInputStream newZipInputStream() throws IOException
    {
        return new ZipInputStream(Files.newInputStream(getCacheLocation()));
    }

    protected ZipOutputStream newZipOutputStream() throws IOException
    {
        ZipOutputStream stream = new ZipOutputStream(Files.newOutputStream(getCacheLocation()));
        stream.setLevel(this.compressionLevel);
        return stream;
    }
}
