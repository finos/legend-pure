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

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

/**
 * PURE hashing strategy.
 */
public class PureEqualsHashingStrategy implements HashingStrategy<Object>
{
    public static final HashingStrategy<Object> HASHING_STRATEGY = new PureEqualsHashingStrategy();

    @Override
    public int computeHashCode(Object object)
    {
        return CompiledSupport.safeHashCode(object);
    }

    @Override
    public boolean equals(Object object1, Object object2)
    {
        return CompiledSupport.equal(object1, object2);
    }

    public static <T> MutableSet<T> newMutableSet()
    {
        return new UnifiedSetWithHashingStrategy<>(HASHING_STRATEGY);
    }

    public static <K, V> MutableMap<K, V> newMutableMap()
    {
        return new UnifiedMapWithHashingStrategy<>(HASHING_STRATEGY);
    }
}
