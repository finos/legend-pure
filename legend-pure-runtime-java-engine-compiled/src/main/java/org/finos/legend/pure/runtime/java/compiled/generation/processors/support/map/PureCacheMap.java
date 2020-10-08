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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.AbstractMutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PureCacheMap<K, V> extends AbstractMutableMap<K, V>
{
    private final Cache<K, V> cache;

    public PureCacheMap(Cache<K, V> cache)
    {
        this.cache = cache;
    }

    @Deprecated
    public PureCacheMap(int concurrencyLevel, long entryDuration, TimeUnit entryDurationTimeUnit)
    {
        this(CacheBuilder.newBuilder()
                .recordStats()
                .concurrencyLevel(concurrencyLevel)
                .expireAfterWrite(entryDuration, entryDurationTimeUnit)
                .<K, V>build());
    }

    public Cache<K, V> getCache()
    {
        return this.cache;
    }

    public Callable<V> getCacheLoaderCallable(final V value)
    {
        return new Callable<V>()
        {
            @Override
            public V call()
            {
                return value;
            }
        };
    }

    public Callable<V> getCacheLoaderCallable(final Function0<? extends V> function)
    {
        return new Callable<V>()
        {
            @Override
            public V call()
            {
                return function.value();
            }
        };
    }

    public Callable<V> getCacheLoaderCallable(K key, Function<? super K, ? extends V> function)
    {
        return getCacheLoaderCallable(function, key);
    }

    public <P> Callable<V> getCacheLoaderCallable(final Function<? super P, ? extends V> function, final P parameter)
    {
        return new Callable<V>()
        {
            @Override
            public V call()
            {
                return function.valueOf(parameter);
            }
        };
    }

    @Override
    public V getIfAbsentPut(K key, Function0<? extends V> function)
    {
        return getFromCache(key, getCacheLoaderCallable(function));
    }

    @Override
    public V getIfAbsentPut(K key, V value)
    {
        return getFromCache(key, getCacheLoaderCallable(value));
    }

    @Override
    public <P> V getIfAbsentPutWith(K key, Function<? super P, ? extends V> function, P parameter)
    {
        return getFromCache(key, getCacheLoaderCallable(function, parameter));
    }

    private V getFromCache(K key, Callable<? extends V> loader)
    {
        try
        {
            return this.cache.get(key, loader);
        }
        catch (ExecutionException|UncheckedExecutionException e)
        {
            throw new PureCacheMapGetException(key, e.getCause());
        }
        catch (Exception e)
        {
            throw new PureCacheMapGetException(key, e);
        }
    }

    @Override
    public V get(Object key)
    {
        return this.cache.getIfPresent(key);
    }

    @Override
    public V removeKey(K key)
    {
        return remove(key);
    }

    @Override
    public V remove(Object key)
    {
        V value = get(key);
        invalidate(key);
        return value;
    }

    public void invalidate(Object key)
    {
        this.cache.invalidate(key);
    }

    @Override
    public void clear()
    {
        this.cache.invalidateAll();
    }

    @Override
    public int size()
    {
        return (int)this.cache.size();
    }

    @Override
    public <K1, V1> MutableMap<K1, V1> newEmpty(int capacity)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <E> MutableMap<K, V> collectKeysAndValues(Iterable<E> iterable, Function<? super E, ? extends K> keyFunction, Function<? super E, ? extends V> valueFunction)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public MutableMap<K, V> newEmpty()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public MutableMap<K, V> clone()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachKeyValue(Procedure2<? super K, ? super V> procedure)
    {
        throw new UnsupportedOperationException();
    }

    public static <K, V> PureCacheMap<K, V> newCacheMap(Cache<K, V> cache)
    {
        return new PureCacheMap<>(cache);
    }
}
