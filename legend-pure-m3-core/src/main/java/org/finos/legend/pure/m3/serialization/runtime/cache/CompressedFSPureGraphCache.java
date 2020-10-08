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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.finos.legend.pure.m3.serialization.runtime.Message;

public class CompressedFSPureGraphCache extends SimpleFSPureGraphCache
{
    public CompressedFSPureGraphCache(Path cacheDirectory, Message message)
    {
        super(cacheDirectory, message);
    }

    public CompressedFSPureGraphCache(Path cacheDirectory)
    {
        super(cacheDirectory);
    }

    @Override
    protected OutputStream newOutputStream(Path cacheFile) throws IOException
    {
        return new GZIPOutputStream(super.newOutputStream(cacheFile));
    }

    @Override
    protected InputStream newInputStream(Path cacheFile) throws IOException
    {
        return new GZIPInputStream(super.newInputStream(cacheFile));
    }
}
