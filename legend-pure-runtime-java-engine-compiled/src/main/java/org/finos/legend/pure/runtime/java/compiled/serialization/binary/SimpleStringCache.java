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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

class SimpleStringCache extends AbstractStringCache
{
    private SimpleStringCache(ObjectIntMap<String> classifierIds, ObjectIntMap<String> otherStrings)
    {
        super(classifierIds, otherStrings);
    }

    public void write(Writer writer)
    {
        writer.writeStringArray(getClassifierStringArray());
        writer.writeStringArray(getOtherStringsArray());
    }

    static SimpleStringCache fromSerialized(Serialized serialized)
    {
        SimpleStringCollector collector = new SimpleStringCollector();
        collectStrings(collector, serialized);
        MutableList<String> classifierIds = collector.classifierIds.toSortedList();
        MutableList<String> otherStrings = collector.otherStrings.asLazy().reject(Predicates.in(collector.classifierIds)).toSortedList();
        return new SimpleStringCache(listToIndexIdMap(classifierIds, 0), listToIndexIdMap(otherStrings, classifierIds.size()));
    }

    private static class SimpleStringCollector implements StringCollector
    {
        private final MutableSet<String> classifierIds = Sets.mutable.empty();
        private final MutableSet<String> otherStrings = Sets.mutable.empty();

        @Override
        public void collectObj(String classifierId, String identifier, String name)
        {
            this.classifierIds.add(classifierId);
            this.otherStrings.add(identifier);
            this.otherStrings.add(name);
        }

        @Override
        public void collectSourceId(String sourceId)
        {
            this.otherStrings.add(sourceId);
        }

        @Override
        public void collectProperty(String property)
        {
            this.otherStrings.add(property);
        }

        @Override
        public void collectRef(String classifierId, String identifier)
        {
            this.classifierIds.add(classifierId);
            this.otherStrings.add(identifier);
        }

        @Override
        public void collectPrimitiveString(String string)
        {
            this.otherStrings.add(string);
        }
    }
}
