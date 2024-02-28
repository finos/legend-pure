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

import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
    protected Writer newWriter(Path cacheFile) throws IOException
    {
        return BinaryWriters.newBinaryWriter(new GZIPOutputStream(Files.newOutputStream(cacheFile)));
    }

    @Override
    protected Reader newReader(Path cacheFile) throws IOException
    {
        return BinaryReaders.newBinaryReader(new GZIPInputStream(Files.newInputStream(cacheFile)));
    }
}
