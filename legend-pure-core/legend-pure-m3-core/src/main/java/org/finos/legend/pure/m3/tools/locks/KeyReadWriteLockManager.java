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

package org.finos.legend.pure.m3.tools.locks;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A concurrent manager for read-write locking based on a key.
 * Each key yields a unique re-entrant read-write lock.  The
 * manager can be created with a fair or non-fair acquisition
 * ordering policy.
 */
public class KeyReadWriteLockManager<K>
{
    private static final Function<Boolean, ReentrantReadWriteLock> NEW_LOCK = new Function<Boolean, ReentrantReadWriteLock>()
    {
        @Override
        public ReentrantReadWriteLock valueOf(Boolean fair)
        {
            return new ReentrantReadWriteLock(fair);
        }
    };

    private final ConcurrentMutableMap<K, ReentrantReadWriteLock> locks = ConcurrentHashMap.newMap();
    private final boolean fair;

    private KeyReadWriteLockManager(boolean fair)
    {
        this.fair = fair;
    }

    /**
     * Get a read lock for key.  Each key yields a unique
     * read lock.  Each call to this method with a given
     * key will yield the same read lock.  The read lock
     * for a given key is connected to the write lock for
     * the same key. This method supports concurrent access.
     *
     * @param key lock key
     * @return key read lock
     */
    public Lock getReadLock(K key)
    {
        return getLock(key).readLock();
    }

    /**
     * Get a write lock for key.  Each key yields a unique
     * write lock.  Each call to this method with a given
     * key will yield the same write lock.  The write lock
     * for a given key is connected to the read lock for
     * the same key. This method supports concurrent access.
     *
     * @param key lock key
     * @return key write lock
     */
    public Lock getWriteLock(K key)
    {
        return getLock(key).writeLock();
    }

    private ReentrantReadWriteLock getLock(K key)
    {
        return this.locks.getIfAbsentPutWith(key, NEW_LOCK, this.fair);
    }

    /**
     * Create a new key read-write lock manager with a
     * fair ordering policy.
     *
     * @param <K> key type
     * @return new key read-write lock manager
     */
    public static <K> KeyReadWriteLockManager<K> newManager()
    {
        return newManager(true);
    }

    /**
     * Create a new key read-write lock manager with the
     * given fairness policy.
     *
     * @param fair true if the locks should use a fair ordering policy
     * @param <K> key type
     * @return new key read-write lock manager
     */
    public static <K> KeyReadWriteLockManager<K> newManager(boolean fair)
    {
        return new KeyReadWriteLockManager<>(fair);
    }
}
