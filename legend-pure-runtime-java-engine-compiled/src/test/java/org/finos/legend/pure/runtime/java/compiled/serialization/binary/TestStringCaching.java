package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class TestStringCaching<T extends StringCache> extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testStringCaching()
    {
        T cache = buildCache();
        String[] expectedClassifiers = getExpectedClassifiers(cache);
        String[] expectedOtherStrings = getExpectedOtherStrings(cache);

        MutableMap<String, byte[]> serialization = Maps.mutable.empty();
        serialize(cache, FileWriters.fromInMemoryByteArrayMap(serialization));
        StringIndex index = buildIndex(FileReaders.fromInMemoryByteArrays(serialization));

        for (int i = 0; i < expectedClassifiers.length; i++)
        {
            Assert.assertEquals(expectedClassifiers[i], index.getString(i));
        }

        for (int i = 0; i < expectedOtherStrings.length; i++)
        {
            Assert.assertEquals(expectedOtherStrings[i], index.getString(i + expectedClassifiers.length));
        }
    }

    protected abstract T buildCache();

    protected String[] getExpectedClassifiers(T cache)
    {
        if (cache instanceof AbstractStringCache)
        {
            return ((AbstractStringCache) cache).getClassifierStringArray();
        }
        throw new UnsupportedOperationException("Implement getExpectedClassifiers");
    }

    protected String[] getExpectedOtherStrings(T cache)
    {
        if (cache instanceof AbstractStringCache)
        {
            return ((AbstractStringCache) cache).getOtherStringsArray();
        }
        throw new UnsupportedOperationException("Implement getExpectedOtherStrings");
    }

    protected abstract void serialize(T cache, FileWriter fileWriter);

    protected abstract StringIndex buildIndex(FileReader fileReader);
}
