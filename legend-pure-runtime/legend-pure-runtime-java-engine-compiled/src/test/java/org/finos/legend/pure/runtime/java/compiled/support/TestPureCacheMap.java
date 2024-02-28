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

package org.finos.legend.pure.runtime.java.compiled.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureCacheMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureCacheMapGetException;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestPureCacheMap
{
    private Cache<Integer, String> getCache()
    {
        return CacheBuilder.newBuilder().recordStats().concurrencyLevel(1).expireAfterWrite(10, TimeUnit.MINUTES).build();
    }

    @Test
    public void testSingleThreadedSimpleAccessWithDeprecatedConstructor()
    {
        PureCacheMap<Integer, String> cacheMap = new PureCacheMap<>(1, 10, TimeUnit.MINUTES);
        for (int i = 0; i <= 2; i++)
        {
            accessIntegerCacheKeys(cacheMap, 10);
        }

        Assert.assertEquals(10, cacheMap.getCache().stats().missCount());
        Assert.assertEquals(20, cacheMap.getCache().stats().hitCount());
    }

    @Test
    public void testSingleThreadedSimpleAccess()
    {
        PureCacheMap<Integer, String> cacheMap = PureCacheMap.newCacheMap(getCache());
        for (int i = 0; i <= 2; i++)
        {
            accessIntegerCacheKeys(cacheMap, 10);
        }

        Assert.assertEquals(10, cacheMap.getCache().stats().missCount());
        Assert.assertEquals(20, cacheMap.getCache().stats().hitCount());
    }

    @Test
    public void testToStringDoesNotRaiseException()
    {
        PureCacheMap<Integer, String> cacheMap = PureCacheMap.newCacheMap(getCache());
        accessIntegerCacheKeys(cacheMap, 10);
        PureMap pureMap = new PureMap(cacheMap);
        String result = pureMap.toString();

        Assert.assertNotNull(result);
    }

    @Test
    public void testMultiThreadedSimpleAccess()
    {
        PureCacheMap<Integer, String> cacheMap = PureCacheMap.newCacheMap(getCache());
        accessIntegerCacheKeys(cacheMap, 10);

        ExecutorService exec = Executors.newFixedThreadPool(4);
        try
        {
            exec.invokeAll(FastList.newListWith(getAccessIntegerCacheKeysCallable(cacheMap, 10), getAccessIntegerCacheKeysCallable(cacheMap, 10)));
            Assert.assertEquals(10, cacheMap.getCache().stats().missCount());
            Assert.assertEquals(20, cacheMap.getCache().stats().hitCount());
        }
        catch (InterruptedException e)
        {
            Assert.fail(e.getMessage());
        }
        finally
        {
            exec.shutdownNow();
        }
    }

    @Test
    public void testSingleThreadMultipleOperations()
    {
        PureCacheMap<Integer, String> cacheMap = PureCacheMap.newCacheMap(getCache());
        int count = 10;
        for (int i = 0; i < count; i++)
        {
            String expected = Integer.toString(i);

            Assert.assertNull(cacheMap.get(i)); // miss

            Assert.assertEquals(expected, cacheMap.getIfAbsentPutWithKey(i, Functions.getToString())); // miss
            Assert.assertEquals(expected, cacheMap.get(i)); // hit
            Assert.assertEquals(expected, cacheMap.getIfAbsentPutWithKey(i, Functions.getToString())); // hit

            cacheMap.invalidate(i);
            Assert.assertNull(cacheMap.get(i)); // miss

            Assert.assertEquals(expected, cacheMap.getIfAbsentPutWithKey(i, Functions.getToString())); // miss
            Assert.assertEquals(expected, cacheMap.get(i)); // hit
            Assert.assertEquals(expected, cacheMap.getIfAbsentPutWithKey(i, Functions.getToString())); // hit
        }

        Assert.assertEquals(4 * count, cacheMap.getCache().stats().missCount());
        Assert.assertEquals(4 * count, cacheMap.getCache().stats().hitCount());

        Assert.assertEquals(count, cacheMap.size());
        cacheMap.clear();
        Assert.assertEquals(0, cacheMap.size());
    }

    @Test
    public void testPureException()
    {
        PureCacheMap<Integer, String> cacheMap = PureCacheMap.newCacheMap(getCache());
        SourceInformation sourceInfo = new SourceInformation("/fake/file.pure", 1, 2, 3, 4, 5, 6);
        assertGetException("Exception fetching Cache value for Key 1: Something bad just happened!", cacheMap, 1, new PureAssertFailException(sourceInfo, "Something bad just happened!"));
        assertGetException("Exception fetching Cache value for Key 2: Now something REALLY BAD just happened!", cacheMap, 2, new PureExecutionException(sourceInfo, "Now something REALLY BAD just happened!"));
        assertGetException("Exception fetching Cache value for Key 3: Oh no! The WORST!", cacheMap, 3, new RuntimeException("Oh no! The WORST!"));
        assertGetException("Exception fetching Cache value for Key 4", cacheMap, 4, new NullPointerException());
    }

    private <K, V> void assertGetException(String expectedInfo, PureCacheMap<K, V> cacheMap, K key, final RuntimeException exception)
    {
        try
        {
            cacheMap.getIfAbsentPutWithKey(key, new Function<K, V>()
            {
                @Override
                public V valueOf(K key)
                {
                    throw exception;
                }
            });
            Assert.fail("Expected exception: " + exception);
        }
        catch (PureCacheMapGetException e)
        {
            Assert.assertSame(key, e.getKey());
            Assert.assertSame(exception, e.getCause());
            Assert.assertEquals(expectedInfo, e.getInfo());
        }
        catch (Exception e)
        {
            Assert.fail("Expected PureCacheMapGetException, got: " + e);
        }
    }

    private void accessIntegerCacheKeys(final PureCacheMap<Integer, String> cacheMap, int numberOfInts)
    {
        IntInterval.oneTo(numberOfInts).forEach(new IntProcedure()
        {
            @Override
            public void value(int key)
            {
                cacheMap.getIfAbsentPutWithKey(key, Functions.getToString());
            }
        });
    }

    private Callable<Void> getAccessIntegerCacheKeysCallable(final PureCacheMap<Integer, String> cacheMap, final int numberOfInts)
    {
        return new Callable<Void>()
        {
            @Override
            public Void call()
            {
                accessIntegerCacheKeys(cacheMap, numberOfInts);
                return null;
            }
        };
    }
}