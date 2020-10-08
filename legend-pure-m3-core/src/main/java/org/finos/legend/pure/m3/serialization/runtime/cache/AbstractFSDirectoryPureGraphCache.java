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
import java.nio.file.Files;
import java.nio.file.Path;

abstract class AbstractFSDirectoryPureGraphCache extends AbstractFSPureGraphCache
{
    private final Path cacheDirectory;

    protected AbstractFSDirectoryPureGraphCache(Path cacheDirectory)
    {
        this.cacheDirectory = cacheDirectory;
        try
        {
            Files.createDirectories(this.cacheDirectory);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not create cache directory: " + this.cacheDirectory, e);
        }
    }

    @Override
    public void clearCaches()
    {
        if (Files.exists(this.cacheDirectory))
        {
            try
            {
                FileTools.deleteDirectoryContents(this.cacheDirectory);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error deleting caches", e);
            }
        }
    }

    @Override
    public Path getCacheLocation()
    {
        return this.cacheDirectory;
    }

    @Override
    public CacheType getCacheType()
    {
        return CacheType.DIRECTORY;
    }
}
