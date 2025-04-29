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

package org.finos.legend.pure.m3.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

public class KeyIndex
{
    private final MapIterable<String, ImmutableList<String>> realKeysByKey;

    private KeyIndex(MapIterable<String, ImmutableList<String>> realKeysByKey)
    {
        this.realKeysByKey = realKeysByKey;
    }

    public RichIterable<String> getKeys()
    {
        return this.realKeysByKey.keysView();
    }

    public ListIterable<String> getRealKeyByName(String name)
    {
        ImmutableList<String> realKey = this.realKeysByKey.get(name);
        if (realKey == null)
        {
            throw new RuntimeException("Unsupported key: " + name);
        }
        return realKey;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int keyCount)
    {
        return new Builder(keyCount);
    }

    public static class Builder
    {
        private final MutableMap<String, ImmutableList<String>> realKeys;

        private Builder(int keyCount)
        {
            this.realKeys = Maps.mutable.ofInitialCapacity(keyCount);
        }

        private Builder()
        {
            this.realKeys = Maps.mutable.empty();
        }

        public Builder withKey(String sourceType, String keyName)
        {
            addKey(sourceType, M3Properties.properties, keyName);
            return this;
        }

        public Builder withKeyFromAssociation(String sourceType, String keyName)
        {
            addKey(sourceType, M3Properties.propertiesFromAssociations, keyName);
            return this;
        }

        public Builder withKeys(String sourceType, String keyName, String... moreKeyNames)
        {
            addKeys(sourceType, M3Properties.properties, keyName, moreKeyNames);
            return this;
        }

        public Builder withKeysFromAssociation(String sourceType, String keyName, String... moreKeyNames)
        {
            addKeys(sourceType, M3Properties.propertiesFromAssociations, keyName, moreKeyNames);
            return this;
        }

        public KeyIndex build()
        {
            return new KeyIndex(this.realKeys.isEmpty() ? Maps.immutable.empty() : this.realKeys);
        }

        private void addKey(String sourceType, String propertiesProperty, String keyName)
        {
            MutableList<String> realKey = buildRealKey(sourceType, propertiesProperty, keyName);
            this.realKeys.put(keyName, realKey.toImmutable());
        }

        private void addKeys(String sourceType, String propertiesProperty, String keyName, String... moreKeyNames)
        {
            MutableList<String> realKey = buildRealKey(sourceType, propertiesProperty, keyName);
            this.realKeys.put(keyName, realKey.toImmutable());
            int keyIndex = realKey.size() - 1;
            for (String otherKeyName : moreKeyNames)
            {
                realKey.set(keyIndex, otherKeyName);
                this.realKeys.put(otherKeyName, realKey.toImmutable());
            }
        }

        private MutableList<String> buildRealKey(String sourceType, String propertyProperty, String keyName)
        {
            MutableList<String> list = Lists.mutable.empty();
            PackageableElement.forEachSystemPathElement(sourceType, n ->
            {
                if (list.notEmpty())
                {
                    list.add(M3Properties.children);
                }
                list.add(n);
            });
            list.add(propertyProperty);
            list.add(keyName);
            return list;
        }
    }
}
