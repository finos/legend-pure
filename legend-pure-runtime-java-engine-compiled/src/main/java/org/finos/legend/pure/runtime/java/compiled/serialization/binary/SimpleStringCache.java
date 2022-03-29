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
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;

import java.util.Objects;

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

    static SimpleStringCache fromNodes(Iterable<? extends CoreInstance> nodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        SimpleStringCollector collector = new SimpleStringCollector();
        collectStrings(collector, nodes, idBuilder, processorSupport);
        MutableList<String> classifierIds = collector.classifierIds.toSortedList();
        MutableList<String> otherStrings = collector.otherStrings.asLazy().reject(collector.classifierIds::contains).toSortedList();
        return new SimpleStringCache(listToIndexIdMap(classifierIds, 0), listToIndexIdMap(otherStrings, classifierIds.size()));
    }

    private static class SimpleStringCollector implements StringCollector
    {
        private final MutableSet<String> classifierIds = Sets.mutable.empty();
        private final MutableSet<String> otherStrings = Sets.mutable.empty();

        @Override
        public void collectObj(String classifierId, String identifier, String name)
        {
            addClassifierId(classifierId);
            addOtherString(identifier);
            addOtherString(name);
        }

        @Override
        public void collectSourceId(String sourceId)
        {
            addOtherString(sourceId);
        }

        @Override
        public void collectProperty(String property)
        {
            addOtherString(property);
        }

        @Override
        public void collectRef(String classifierId, String identifier)
        {
            addClassifierId(classifierId);
            addOtherString(identifier);
        }

        @Override
        public void collectPrimitiveString(String string)
        {
            addOtherString(string);
        }

        private void addClassifierId(String string)
        {
            this.classifierIds.add(Objects.requireNonNull(string));
        }

        private void addOtherString(String string)
        {
            if (string != null)
            {
                this.otherStrings.add(string);
            }
        }
    }
}
