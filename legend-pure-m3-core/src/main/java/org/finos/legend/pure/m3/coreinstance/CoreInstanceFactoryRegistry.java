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

package org.finos.legend.pure.m3.coreinstance;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;

public class CoreInstanceFactoryRegistry
{
    private final ImmutableIntObjectMap<CoreInstanceFactory> typeFactoriesById;
    private final ImmutableMap<String, CoreInstanceFactory> typeFactoriesByPath;
    private final ImmutableMap<String, Class> interfaceByPath;

    public CoreInstanceFactoryRegistry(ImmutableIntObjectMap<CoreInstanceFactory> typeFactoriesById, ImmutableMap<String, CoreInstanceFactory> typeFactoriesByPath, ImmutableMap<String, Class> interfaceByPath)
    {
        this.typeFactoriesById = typeFactoriesById;
        this.typeFactoriesByPath = typeFactoriesByPath;
        this.interfaceByPath = interfaceByPath;
    }

    public CoreInstanceFactoryRegistry combine(CoreInstanceFactoryRegistry registry)
    {
        IntObjectHashMap<CoreInstanceFactory> newTypeFactoriesById = new IntObjectHashMap<CoreInstanceFactory>(this.typeFactoriesById);
        newTypeFactoriesById.putAll(registry.typeFactoriesById);

        MutableMap<String, CoreInstanceFactory> newTypeFactoriesByPath = this.typeFactoriesByPath.toMap().withAllKeyValues(registry.typeFactoriesByPath.keyValuesView());

        MutableMap<String, Class> newInterfaceByPath = this.interfaceByPath.toMap().withAllKeyValues(registry.interfaceByPath.keyValuesView());

        return new CoreInstanceFactoryRegistry(newTypeFactoriesById.toImmutable(),
                newTypeFactoriesByPath.toImmutable(),
                newInterfaceByPath.toImmutable());
    }

    public CoreInstanceFactory getFactoryForPath(String path)
    {
        return this.typeFactoriesByPath.get(path);
    }

    public CoreInstanceFactory getFactoryForId(int syntheticId)
    {
        return this.typeFactoriesById.get(syntheticId);
    }

    public java.lang.Class getClassForPath(String path)
    {
        return this.interfaceByPath.get(path);
    }

    public MutableSet<String> allManagedTypes()
    {
        return this.typeFactoriesByPath.keysView().toSet();
    }
}
