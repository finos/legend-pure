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
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;

import java.util.Properties;

/**
 * {@link RuntimeOptions} backed by a snapshot of properties with a given prefix, taken once at construction.
 * Properties set after the snapshot are not visible.
 */
class CachedSystemPropertyOptions implements RuntimeOptions
{
    static final RuntimeOptions DEFAULT = new CachedSystemPropertyOptions(System.getProperties(), RuntimeOptions.DEFAULT_OPTION_PREFIX);

    private final SetIterable<String> setOptions;

    CachedSystemPropertyOptions(Properties properties, String prefix)
    {
        this.setOptions = scanProperties(properties, prefix).toImmutable();
    }

    @Override
    public boolean isOptionSet(String name)
    {
        return this.setOptions.contains(name);
    }

    @Override
    public String toString()
    {
        return "<CachedSystemPropertyOptions " + this.setOptions + ">";
    }

    /**
     * Names (with the prefix stripped) of the properties starting with the given prefix whose value is
     * {@code true}, matching the case insensitive semantics of {@link Boolean#getBoolean}.
     */
    static MutableSet<String> scanProperties(Properties properties, String prefix)
    {
        MutableSet<String> result = Sets.mutable.empty();
        properties.stringPropertyNames().forEach(key ->
        {
            if (key.startsWith(prefix) && Boolean.parseBoolean(properties.getProperty(key)))
            {
                result.add(key.substring(prefix.length()));
            }
        });
        return result;
    }
}
