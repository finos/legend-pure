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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class TestStringCaching<T extends StringCache> extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testStringCaching()
    {
        GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
        IdBuilder idBuilder = IdBuilder.newIdBuilder(processorSupport);
        T cache = newBuilder().withObjs(GraphNodeIterable.allInstancesFromRepository(repository).collect(node -> GraphSerializer.buildObj(node, idBuilder, classifierCaches, processorSupport))).build();
        Assert.assertEquals(0, cache.getStringId(null));

        String[] expectedClassifiers = getExpectedClassifiers(cache);
        String[] expectedOtherStrings = getExpectedOtherStrings(cache);

        MutableMap<String, byte[]> serialization = Maps.mutable.empty();
        serialize(cache, FileWriters.fromInMemoryByteArrayMap(serialization));
        StringIndex index = buildIndex(FileReaders.fromInMemoryByteArrays(serialization));
        Assert.assertNull(index.getString(0));

        for (int i = 0; i < expectedClassifiers.length; i++)
        {
            Assert.assertEquals(expectedClassifiers[i], index.getString(StringCacheOrIndex.classifierIdStringIndexToId(i)));
        }

        for (int i = 0; i < expectedOtherStrings.length; i++)
        {
            Assert.assertEquals(expectedOtherStrings[i], index.getString(StringCacheOrIndex.otherStringIndexToId(i)));
        }
    }

    protected abstract StringCache.Builder<T> newBuilder();

    protected String[] getExpectedClassifiers(T cache)
    {
        return cache.getClassifierStringArray();
    }

    protected String[] getExpectedOtherStrings(T cache)
    {
        return cache.getOtherStringsArray();
    }

    protected abstract void serialize(T cache, FileWriter fileWriter);

    protected abstract StringIndex buildIndex(FileReader fileReader);
}
