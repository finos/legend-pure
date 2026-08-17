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

public interface RuntimeOptions
{
    String DEFAULT_OPTION_PREFIX = "pure.options.";

    boolean isOptionSet(String name);

    static RuntimeOptions noOptionsSet()
    {
        return name -> false;
    }

    static RuntimeOptions systemPropertyOptions()
    {
        return systemPropertyOptions(null);
    }

    static RuntimeOptions systemPropertyOptions(String prefix)
    {
        return (prefix == null) ? Boolean::getBoolean : name -> Boolean.getBoolean(prefix + name);
    }

    /**
     * The options used wherever none are explicitly supplied: the system properties prefixed with
     * {@value #DEFAULT_OPTION_PREFIX}, snapshotted once per VM on first use of the default options.
     * Properties set after that snapshot are not visible; use {@link MutableRuntimeOptions} where options
     * must be togglable at runtime.
     */
    static RuntimeOptions defaultOptions()
    {
        return CachedSystemPropertyOptions.DEFAULT;
    }
}
