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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;

public class CoreInstanceFactoryRegistry
{
    private final ImmutableIntObjectMap<CoreInstanceFactory> typeFactoriesById;
    private final ImmutableMap<String, TypeInfo> typeInfoByPath;

    private CoreInstanceFactoryRegistry(ImmutableIntObjectMap<CoreInstanceFactory> typeFactoriesById, ImmutableMap<String, TypeInfo> typeInfoByPath)
    {
        this.typeFactoriesById = typeFactoriesById;
        this.typeInfoByPath = typeInfoByPath;
    }

    @Deprecated
    public CoreInstanceFactoryRegistry(ImmutableIntObjectMap<CoreInstanceFactory> typeFactoriesById, ImmutableMap<String, CoreInstanceFactory> typeFactoriesByPath, ImmutableMap<String, Class<? extends CoreInstance>> interfaceByPath)
    {
        this(typeFactoriesById, toTypeInfo(typeFactoriesByPath, interfaceByPath));
    }

    public CoreInstanceFactoryRegistry combine(CoreInstanceFactoryRegistry registry)
    {
        return builder(this.typeInfoByPath.size() + registry.typeInfoByPath.size())
                .withRegistry(this)
                .withRegistry(registry)
                .build();
    }

    public CoreInstanceFactory getFactoryForPath(String path)
    {
        TypeInfo typeInfo = this.typeInfoByPath.get(path);
        return (typeInfo == null) ? null : typeInfo.factory;
    }

    public CoreInstanceFactory getFactoryForId(int syntheticId)
    {
        return this.typeFactoriesById.get(syntheticId);
    }

    public Class<? extends CoreInstance> getClassForPath(String path)
    {
        TypeInfo typeInfo = this.typeInfoByPath.get(path);
        return (typeInfo == null) ? null : typeInfo._class;
    }

    public RichIterable<String> getAllPaths()
    {
        return this.typeInfoByPath.keysView();
    }

    @Deprecated
    public MutableSet<String> allManagedTypes()
    {
        return this.typeInfoByPath.keysView().toSet();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int typeCount)
    {
        return new Builder(typeCount);
    }

    public static class Builder
    {
        private final MutableIntObjectMap<CoreInstanceFactory> typeFactoriesById;
        private final MutableMap<String, TypeInfo> typeInfoByPath;

        private Builder()
        {
            this.typeFactoriesById = IntObjectMaps.mutable.empty();
            this.typeInfoByPath = Maps.mutable.empty();
        }

        private Builder(int typeCount)
        {
            this.typeFactoriesById = IntObjectMaps.mutable.ofInitialCapacity(typeCount);
            this.typeInfoByPath = Maps.mutable.ofInitialCapacity(typeCount);
        }

        public Builder withType(int syntheticId, CoreInstanceFactory factory)
        {
            this.typeFactoriesById.put(syntheticId, factory);
            return this;
        }

        public Builder withType(String path, CoreInstanceFactory factory, Class<? extends CoreInstance> _class)
        {
            this.typeInfoByPath.put(path, new TypeInfo(factory, _class));
            return this;
        }

        public Builder withType(String path, int syntheticId, CoreInstanceFactory factory, Class<? extends CoreInstance> _class)
        {
            return withType(syntheticId, factory).withType(path, factory, _class);
        }

        public Builder withRegistry(CoreInstanceFactoryRegistry registry)
        {
            this.typeFactoriesById.putAll(registry.typeFactoriesById);
            this.typeInfoByPath.putAll(registry.typeInfoByPath.castToMap());
            return this;
        }

        public Builder withRegistries(Iterable<? extends CoreInstanceFactoryRegistry> registries)
        {
            registries.forEach(this::withRegistry);
            return this;
        }

        public CoreInstanceFactoryRegistry build()
        {
            return new CoreInstanceFactoryRegistry(this.typeFactoriesById.toImmutable(), this.typeInfoByPath.toImmutable());
        }
    }

    private static class TypeInfo
    {
        private final CoreInstanceFactory factory;
        private final Class<? extends CoreInstance> _class;

        private TypeInfo(CoreInstanceFactory factory, Class<? extends CoreInstance> _class)
        {
            this.factory = factory;
            this._class = _class;
        }
    }

    private static ImmutableMap<String, TypeInfo> toTypeInfo(ImmutableMap<String, CoreInstanceFactory> typeFactoriesByPath, ImmutableMap<String, Class<? extends CoreInstance>> interfaceByPath)
    {
        MutableMap<String, TypeInfo> map = Maps.mutable.ofInitialCapacity(typeFactoriesByPath.size());
        typeFactoriesByPath.forEachKeyValue((path, factory) ->
        {
            Class<? extends CoreInstance> _class = interfaceByPath.get(path);
            map.put(path, new TypeInfo(factory, _class));
        });
        interfaceByPath.forEachKeyValue((path, _class) ->
        {
            if (!map.containsKey(path))
            {
                map.put(path, new TypeInfo(null, _class));
            }
        });
        return map.toImmutable();
    }
}
