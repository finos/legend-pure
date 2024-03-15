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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

/**
 * A concurrent manager for locking based on a key.  Each key
 * yields a unique lock. This "lock" is simply an Object whose
 * intrinsic lock may be used in a synchronized statement.
 */
public class KeyLockManager<K>
{
    private static final Function0<Object> NEW_LOCK = new Function0<Object>()
    {
        @Override
        public Object value()
        {
            return new Object();
        }
    };

    private final ConcurrentMutableMap<K, Object> locks = ConcurrentHashMap.newMap();

    private KeyLockManager()
    {
    }

    /**
     * Get a lock for key.  This "lock" is simply an Object
     * whose intrinsic lock may be used in a synchronized
     * statement.  Each key yields a unique lock.  Each call
     * to this method with a given key will yield the same
     * lock.  This method supports concurrent access.
     *
     * @param key lock key
     * @return key lock
     */
    public Object getLock(K key)
    {
        return this.locks.getIfAbsentPut(key, NEW_LOCK);
    }

    /**
     * Create a new key lock manager.
     *
     * @param <T> key type
     * @return new key lock manager
     */
    public static <T> KeyLockManager<T> newManager()
    {
        return new KeyLockManager<>();
    }
}
