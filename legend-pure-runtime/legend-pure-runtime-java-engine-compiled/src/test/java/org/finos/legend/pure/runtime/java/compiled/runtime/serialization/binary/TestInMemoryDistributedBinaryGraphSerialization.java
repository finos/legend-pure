// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileReader;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileReaders;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileWriter;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileWriters;

public class TestInMemoryDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    private final MutableMap<String, byte[]> filesBytes = Maps.mutable.empty();

    @Override
    protected FileWriter getFileWriter()
    {
        return FileWriters.fromInMemoryByteArrayMap(this.filesBytes);
    }

    @Override
    protected FileReader getFileReader()
    {
        return FileReaders.fromInMemoryByteArrays(this.filesBytes);
    }
}
