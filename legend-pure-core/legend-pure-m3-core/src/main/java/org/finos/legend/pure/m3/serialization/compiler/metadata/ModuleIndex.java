// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;

import java.util.function.Consumer;

class ModuleIndex
{
    private final MapIterable<String, ModuleManifest> modulesByName;

    private ModuleIndex(MapIterable<String, ModuleManifest> modulesByName)
    {
        this.modulesByName = modulesByName;
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) ||
                ((other instanceof ModuleIndex) && this.modulesByName.equals(((ModuleIndex) other).modulesByName));
    }

    @Override
    public int hashCode()
    {
        return this.modulesByName.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName());
        this.modulesByName.keysView().appendString(builder, " modules=[", ", ", "]>");
        return builder.toString();
    }

    boolean hasModule(String moduleName)
    {
        return this.modulesByName.containsKey(moduleName);
    }

    RichIterable<String> getAllModuleNames()
    {
        return this.modulesByName.keysView();
    }

    ModuleManifest getModule(String moduleName)
    {
        return this.modulesByName.get(moduleName);
    }

    RichIterable<ModuleManifest> getAllModules()
    {
        return this.modulesByName.valuesView();
    }

    void forEachModule(Consumer<? super ModuleManifest> consumer)
    {
        this.modulesByName.forEachValue(consumer::accept);
    }

    static ModuleIndex buildIndex(Iterable<? extends ModuleManifest> moduleMetadata)
    {
        MutableMap<String, ModuleManifest> index = Maps.mutable.empty();
        moduleMetadata.forEach(module ->
        {
            ModuleManifest previous = index.put(module.getModuleName(), module);
            if ((previous != null) && !module.equals(previous))
            {
                throw new IllegalArgumentException("Multiple modules named '" + module.getModuleName() + "'");
            }
        });
        return new ModuleIndex(index);
    }
}
