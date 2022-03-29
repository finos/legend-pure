// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;

public class TestSimpleStringCaching extends TestStringCaching<SimpleStringCache>
{
    private static final String CACHE_PATH = "metadata/strings.idx";

    @Override
    protected SimpleStringCache buildCache()
    {
        return SimpleStringCache.fromNodes(GraphNodeIterable.allInstancesFromRepository(repository), IdBuilder.newIdBuilder(processorSupport), processorSupport);
    }

    @Override
    protected void serialize(SimpleStringCache cache, FileWriter fileWriter)
    {
        try (Writer writer = fileWriter.getWriter(CACHE_PATH))
        {
            cache.write(writer);
        }
    }

    @Override
    protected StringIndex buildIndex(FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(CACHE_PATH))
        {
            return EagerStringIndex.fromReader(reader);
        }
    }
}