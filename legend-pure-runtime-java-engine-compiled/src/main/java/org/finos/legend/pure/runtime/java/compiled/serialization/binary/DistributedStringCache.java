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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

class DistributedStringCache extends AbstractStringCache
{
    static final String CLASSIFIER_ID_INDEX_FILE_PATH = DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "classifiers");
    static final String OTHER_STRING_INDEX_METADATA_FILE_PATH = DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other");

    private static final int PARTITION_SIZE = 32 * 1024; // must be a power of 2
    private static final int PARTITION_MASK = PARTITION_SIZE - 1;

    private DistributedStringCache(ObjectIntMap<String> classifierIds, ObjectIntMap<String> otherStrings)
    {
        super(classifierIds, otherStrings);
    }

    public void write(FileWriter fileWriter)
    {
        try (Writer writer = fileWriter.getWriter(CLASSIFIER_ID_INDEX_FILE_PATH))
        {
            writer.writeStringArray(getClassifierStringArray());
        }

        String[] otherStrings = getOtherStringsArray();
        int otherStringsCount = otherStrings.length;
        try (Writer writer = fileWriter.getWriter(OTHER_STRING_INDEX_METADATA_FILE_PATH))
        {
            writer.writeInt(otherStringsCount);
        }
        int partitionStart = 0;
        int partitionEnd = Math.min(partitionStart + PARTITION_SIZE, otherStringsCount);
        Writer writer = fileWriter.getWriter(getOtherStringIndexPartitionFilePath(partitionStart));
        try
        {
            writer.writeInt(partitionEnd - partitionStart);
            for (int i = 0; i < otherStringsCount; i++)
            {
                if (i == partitionEnd)
                {
                    writer.close();
                    partitionStart = partitionEnd;
                    partitionEnd = Math.min(partitionEnd + PARTITION_SIZE, otherStringsCount);
                    writer = fileWriter.getWriter(getOtherStringIndexPartitionFilePath(partitionStart));
                    writer.writeInt(partitionEnd - partitionStart);
                }
                writer.writeString(otherStrings[i]);
            }
        }
        finally
        {
            writer.close();
        }
    }

    static DistributedStringCache fromSerialized(Serialized serialized)
    {
        DistributedStringCollector collector = new DistributedStringCollector();
        collectStrings(collector, serialized);
        return fromStringCollector(collector);
    }

    static DistributedStringCache fromNodes(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        DistributedStringCollector collector = new DistributedStringCollector();
        collectStrings(collector, nodes, processorSupport);
        return fromStringCollector(collector);
    }

    static void collectStrings(DistributedStringCollector collector, Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        AbstractStringCache.PropertyValueCollectorVisitor propertyValueVisitor = new AbstractStringCache.PropertyValueCollectorVisitor(collector);
        GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
        for (CoreInstance instance : nodes)
        {
            Obj obj = GraphSerializer.buildObjWithProperties(instance, classifierCaches, processorSupport);
            collectStringsFromObj(collector, propertyValueVisitor, obj);
        }
    }

    static DistributedStringCache fromStringCollector(DistributedStringCollector collector)
    {
        MutableSet<String> allStrings = UnifiedSet.newSet(collector.classifierIds.size() + collector.identifiers.size() + collector.otherStrings.size());
        Predicate<Object> inAllStrings = Predicates.in(allStrings);

        MutableList<String> classifierIdList = FastList.<String>newList(collector.classifierIds.size()).withAll(collector.classifierIds).sortThis();
        allStrings.addAll(classifierIdList);

        MutableList<String> otherStringList = FastList.newList(collector.identifiers.size() + collector.otherStrings.size());
        for (String classifierId : classifierIdList)
        {
            MutableSet<String> classifierIdentifierSet = collector.identifiers.get(classifierId);
            MutableList<String> classifierIdentifierList = classifierIdentifierSet.reject(inAllStrings, FastList.<String>newList(classifierIdentifierSet.size())).sortThis();
            allStrings.addAll(classifierIdentifierList);
            otherStringList.addAll(classifierIdentifierList);
        }
        otherStringList.addAll(collector.otherStrings.reject(inAllStrings, FastList.<String>newList(collector.otherStrings.size())).sortThis());

        return new DistributedStringCache(listToIndexIdMap(classifierIdList, 0), listToIndexIdMap(otherStringList, classifierIdList.size()));
    }

    static int getStartOfPartition(int index)
    {
        return index - (index & PARTITION_MASK);
    }

    static String getOtherStringIndexPartitionFilePath(int partitionStart)
    {
        return DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-" + partitionStart);
    }

    private static class DistributedStringCollector implements StringCollector
    {
        private final MutableSet<String> classifierIds = Sets.mutable.empty();
        private final MutableSetMultimap<String, String> identifiers = Multimaps.mutable.set.empty();
        private final MutableSet<String> otherStrings = Sets.mutable.empty();

        @Override
        public void collectObj(String classifierId, String identifier, String name)
        {
            this.classifierIds.add(classifierId);
            this.identifiers.put(classifierId, identifier);
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
            this.identifiers.put(classifierId, identifier);
        }

        @Override
        public void collectPrimitiveString(String string)
        {
            this.otherStrings.add(string);
        }
    }
}
