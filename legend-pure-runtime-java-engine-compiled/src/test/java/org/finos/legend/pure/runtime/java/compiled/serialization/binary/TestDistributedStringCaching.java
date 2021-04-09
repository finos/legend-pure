package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDistributedStringCaching extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testDistributedStringCaching()
    {
        DistributedStringCache cache = DistributedStringCache.fromNodes(GraphNodeIterable.allInstancesFromRepository(runtime.getModelRepository()), runtime.getProcessorSupport());
        String[] expectedClassifiers = cache.getClassifierStringArray();
        String[] expectedOtherStrings = cache.getOtherStringsArray();

        MutableMap<String, byte[]> serialization = Maps.mutable.empty();
        cache.write(FileWriters.fromInMemoryByteArrayMap(serialization));
        LazyStringIndex index = LazyStringIndex.fromFileReader(FileReaders.fromInMemoryByteArrays(serialization));

        for (int i = 0; i < expectedClassifiers.length; i++)
        {
            Assert.assertEquals(expectedClassifiers[i], index.getString(i));
        }

        for (int i = 0; i < expectedOtherStrings.length; i++)
        {
            Assert.assertEquals(expectedOtherStrings[i], index.getString(i + expectedClassifiers.length));
        }
    }
}
