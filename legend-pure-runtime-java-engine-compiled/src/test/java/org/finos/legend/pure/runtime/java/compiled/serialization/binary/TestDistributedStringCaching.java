package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.finos.legend.pure.m4.tools.GraphNodeIterable;

public class TestDistributedStringCaching extends TestStringCaching<DistributedStringCache>
{
    @Override
    protected DistributedStringCache buildCache()
    {
        return DistributedStringCache.fromNodes(GraphNodeIterable.allInstancesFromRepository(repository), processorSupport);
    }

    @Override
    protected void serialize(DistributedStringCache cache, FileWriter fileWriter)
    {
        cache.write(fileWriter);
    }

    @Override
    protected StringIndex buildIndex(FileReader fileReader)
    {
        return LazyStringIndex.fromFileReader(fileReader);
    }
}
