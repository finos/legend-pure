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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;

import java.util.Objects;

class DistributedStringCache extends AbstractStringCache
{
    private static final int PARTITION_SIZE = 32 * 1024; // must be a power of 2
    private static final int PARTITION_MASK = PARTITION_SIZE - 1;

    private DistributedStringCache(ObjectIntMap<String> classifierIds, ObjectIntMap<String> otherStrings)
    {
        super(classifierIds, otherStrings);
    }

    public void write(String metadataName, FileWriter fileWriter)
    {
        // Write classifier strings
        try (Writer writer = fileWriter.getWriter(DistributedMetadataHelper.getClassifierIdStringsIndexFilePath(metadataName)))
        {
            writer.writeStringArray(getClassifierStringArray());
        }

        // Write other strings index
        String[] otherStrings = getOtherStringsArray();
        int otherStringsCount = otherStrings.length;
        try (Writer writer = fileWriter.getWriter(DistributedMetadataHelper.getOtherStringsIndexFilePath(metadataName)))
        {
            writer.writeInt(otherStringsCount);
        }

        // Write other strings partitions
        for (int partitionStart = 0; partitionStart < otherStringsCount; partitionStart += PARTITION_SIZE)
        {
            try (Writer writer = fileWriter.getWriter(DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(metadataName, partitionStart)))
            {
                int partitionEnd = Math.min(partitionStart + PARTITION_SIZE, otherStringsCount);
                writer.writeInt(partitionEnd - partitionStart);
                for (int i = partitionStart; i < partitionEnd; i++)
                {
                    writer.writeString(otherStrings[i]);
                }
            }
        }
    }

    static DistributedStringCache fromNodes(Iterable<? extends CoreInstance> nodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        DistributedStringCollector collector = new DistributedStringCollector();
        collectStrings(collector, nodes, idBuilder, processorSupport);

        MutableSet<String> allStrings = Sets.mutable.ofInitialCapacity(collector.classifierIds.size() + collector.identifiers.size() + collector.otherStrings.size());

        MutableList<String> classifierIdList = Lists.mutable.withAll(collector.classifierIds).sortThis();
        allStrings.addAll(classifierIdList);

        MutableList<String> otherStringList = Lists.mutable.ofInitialCapacity(collector.identifiers.size() + collector.otherStrings.size());
        for (String classifierId : classifierIdList)
        {
            MutableSet<String> classifierIdentifierSet = collector.identifiers.get(classifierId);
            MutableList<String> classifierIdentifierList = classifierIdentifierSet.reject(allStrings::contains, Lists.mutable.ofInitialCapacity(classifierIdentifierSet.size())).sortThis();
            allStrings.addAll(classifierIdentifierList);
            otherStringList.addAll(classifierIdentifierList);
        }
        otherStringList.addAll(collector.otherStrings.reject(allStrings::contains, Lists.mutable.ofInitialCapacity(collector.otherStrings.size())).sortThis());

        return new DistributedStringCache(listToIndexIdMap(classifierIdList, 0), listToIndexIdMap(otherStringList, classifierIdList.size()));
    }

    static int getStartOfPartition(int index)
    {
        return index - (index & PARTITION_MASK);
    }

    private static class DistributedStringCollector implements StringCollector
    {
        private final MutableSet<String> classifierIds = Sets.mutable.empty();
        private final MutableSetMultimap<String, String> identifiers = Multimaps.mutable.set.empty();
        private final MutableSet<String> otherStrings = Sets.mutable.empty();

        @Override
        public void collectObj(String classifierId, String identifier, String name)
        {
            addClassifierId(classifierId);
            addIdentifier(classifierId, identifier);
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
            addIdentifier(classifierId, identifier);
        }

        @Override
        public void collectPrimitiveString(String string)
        {
            addOtherString(string);
        }

        private void addClassifierId(String classifierId)
        {
            this.classifierIds.add(Objects.requireNonNull(classifierId));
        }

        private void addIdentifier(String classifierId, String identifier)
        {
            this.identifiers.put(Objects.requireNonNull(classifierId), Objects.requireNonNull(identifier));
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
