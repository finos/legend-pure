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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.util.Properties;

/**
 * {@link RuntimeOptions} held in memory and togglable at runtime. It may be seeded from system properties,
 * but it never writes them: toggling an option here is visible only to code holding this instance.
 * <p>
 * Safe for concurrent use.
 */
public final class MutableRuntimeOptions implements RuntimeOptions
{
    private final ConcurrentMutableMap<String, Boolean> options = ConcurrentHashMap.newMap();

    private MutableRuntimeOptions(SetIterable<String> initiallySet)
    {
        initiallySet.forEach(name -> this.options.put(name, Boolean.TRUE));
    }

    @Override
    public boolean isOptionSet(String name)
    {
        return this.options.containsKey(name);
    }

    /**
     * Sets or clears an option, returning the new effective value of {@link #isOptionSet}.
     */
    public boolean setOption(String name, boolean value)
    {
        if (value)
        {
            this.options.put(name, Boolean.TRUE);
        }
        else
        {
            this.options.remove(name);
        }
        return value;
    }

    /**
     * The names of the options currently set.
     */
    public SetIterable<String> getSetOptions()
    {
        return this.options.keysView().toSet();
    }

    @Override
    public String toString()
    {
        return "<MutableRuntimeOptions " + getSetOptions() + ">";
    }

    public static MutableRuntimeOptions empty()
    {
        return new MutableRuntimeOptions(Sets.immutable.empty());
    }

    /**
     * Seeded from the system properties prefixed with {@value RuntimeOptions#DEFAULT_OPTION_PREFIX}, read once.
     */
    public static MutableRuntimeOptions fromSystemProperties()
    {
        return fromSystemProperties(RuntimeOptions.DEFAULT_OPTION_PREFIX);
    }

    /**
     * Seeded from the system properties with the given prefix, read once.
     */
    public static MutableRuntimeOptions fromSystemProperties(String prefix)
    {
        return fromProperties(System.getProperties(), prefix);
    }

    /**
     * Seeded from the given properties with the given prefix, read once.
     */
    static MutableRuntimeOptions fromProperties(Properties properties, String prefix)
    {
        return new MutableRuntimeOptions(CachedSystemPropertyOptions.scanProperties(properties, prefix));
    }
}
