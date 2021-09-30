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

import org.finos.legend.pure.m4.tools.GraphNodeIterable;

public class TestDistributedStringCaching extends TestStringCaching<DistributedStringCache>
{
    private static final String METADATA_NAME = "platform";

    @Override
    protected DistributedStringCache buildCache()
    {
        return DistributedStringCache.fromNodes(GraphNodeIterable.allInstancesFromRepository(repository), processorSupport);
    }

    @Override
    protected void serialize(DistributedStringCache cache, FileWriter fileWriter)
    {
        cache.write(METADATA_NAME, fileWriter);
    }

    @Override
    protected StringIndex buildIndex(FileReader fileReader)
    {
        return LazyStringIndex.fromFileReader(METADATA_NAME, fileReader);
    }
}
