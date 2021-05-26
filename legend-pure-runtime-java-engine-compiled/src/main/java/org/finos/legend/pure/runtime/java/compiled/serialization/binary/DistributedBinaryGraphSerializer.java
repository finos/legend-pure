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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;

public abstract class DistributedBinaryGraphSerializer
{
    private static final int MAX_BIN_FILE_BYTES = 512 * 1024;

    protected final String metadataName;

    private DistributedBinaryGraphSerializer(String metadataName)
    {
        this.metadataName = metadataName;
    }

    public abstract void serialize(FileWriter fileWriter);

    public void serializeToDirectory(Path directory)
    {
        serialize(FileWriters.fromDirectory(directory));
    }

    public void serializeToJar(JarOutputStream stream)
    {
        serialize(FileWriters.fromJarOutputStream(stream));
    }

    public void serializeToInMemoryByteArrays(Map<String, ? super byte[]> fileBytes)
    {
        serialize(FileWriters.fromInMemoryByteArrayMap(fileBytes));
    }

    private static class DistributedBinaryGraphSerializerFromSerialized extends DistributedBinaryGraphSerializer
    {
        private final Serialized serialized;

        private DistributedBinaryGraphSerializerFromSerialized(String metadataName, Serialized serialized)
        {
            super(metadataName);
            this.serialized = serialized;
        }

        @Override
        public void serialize(FileWriter fileWriter)
        {
            if (this.serialized.getPackageLinks().notEmpty())
            {
                throw new IllegalArgumentException("Separate package links are not allowed");
            }

            // Build string cache
            DistributedStringCache stringCache = DistributedStringCache.fromSerialized(serialized);
            BinaryObjSerializer serializer = new BinaryObjSerializerWithStringCacheAndImplicitIdentifiers(stringCache);

            // Write string cache
            stringCache.write(this.metadataName, fileWriter);

            // Write instances
            int partition = 0;
            int partitionTotalBytes = 0;
            ByteArrayOutputStream binByteStream = new ByteArrayOutputStream(MAX_BIN_FILE_BYTES);
            ByteArrayOutputStream indexByteStream = new ByteArrayOutputStream();
            try (Writer binFileWriter = BinaryWriters.newBinaryWriter(binByteStream);
                 Writer indexFileWriter = BinaryWriters.newBinaryWriter(indexByteStream))
            {
                ListMultimap<String, Obj> objsByClassifier = this.serialized.getObjects().groupBy(Obj::getClassifier);
                for (String classifierId : objsByClassifier.keysView().toSortedList())
                {
                    ListIterable<Obj> classifierObjs = objsByClassifier.get(classifierId);
                    ListIterable<ObjSerialization> objSerializations = serializeClassifierObjs(serializer, classifierObjs);

                    // Initial index information
                    indexFileWriter.writeInt(objSerializations.size()); // total obj count
                    indexFileWriter.writeInt(partition); // initial partition
                    indexFileWriter.writeInt(partitionTotalBytes); // initial byte offset in partition

                    MutableList<ObjSerialization> partitionObjSerializations = Lists.mutable.empty();
                    for (ObjSerialization objSerialization : objSerializations)
                    {
                        int objByteCount = objSerialization.bytes.length;
                        if (partitionTotalBytes + objByteCount > MAX_BIN_FILE_BYTES)
                        {
                            // Write current partition
                            try (Writer writer = fileWriter.getWriter(DistributedMetadataFiles.getMetadataPartitionBinFilePath(this.metadataName, partition)))
                            {
                                writer.writeBytes(binByteStream.toByteArray());
                                binByteStream.reset();
                            }

                            // Write partition portion of classifier index
                            indexFileWriter.writeInt(partitionObjSerializations.size());
                            for (ObjSerialization partitionObjSerialization : partitionObjSerializations)
                            {
                                indexFileWriter.writeInt(stringCache.getStringId(partitionObjSerialization.identifier));
                                indexFileWriter.writeInt(partitionObjSerialization.bytes.length);
                            }

                            // New partition
                            partition++;
                            if (partition < 0)
                            {
                                throw new RuntimeException("Too many partitions");
                            }
                            partitionTotalBytes = 0;
                            partitionObjSerializations.clear();
                        }
                        binFileWriter.writeBytes(objSerialization.bytes);
                        partitionTotalBytes += objByteCount;
                        partitionObjSerializations.add(objSerialization);
                    }
                    // Write final partition portion of classifier index
                    if (partitionObjSerializations.notEmpty())
                    {
                        indexFileWriter.writeInt(partitionObjSerializations.size());
                        for (ObjSerialization partitionObjSerialization : partitionObjSerializations)
                        {
                            indexFileWriter.writeInt(stringCache.getStringId(partitionObjSerialization.identifier));
                            indexFileWriter.writeInt(partitionObjSerialization.bytes.length);
                        }
                    }
                    // Write classifier index
                    try (Writer writer = fileWriter.getWriter(DistributedMetadataFiles.getMetadataClassifierIndexFilePath(this.metadataName, classifierId)))
                    {
                        writer.writeBytes(indexByteStream.toByteArray());
                        indexByteStream.reset();
                    }
                }

                // Write final partition
                if (binByteStream.size() > 0)
                {
                    try (Writer writer = fileWriter.getWriter(DistributedMetadataFiles.getMetadataPartitionBinFilePath(this.metadataName, partition)))
                    {
                        writer.writeBytes(binByteStream.toByteArray());
                    }
                }
            }
        }
    }

    private static class DistributedBinaryGraphSerializerFromPureRuntime extends DistributedBinaryGraphSerializer
    {
        private final PureRuntime runtime;

        private DistributedBinaryGraphSerializerFromPureRuntime(String metadataName, PureRuntime runtime)
        {
            super(metadataName);
            this.runtime = runtime;
        }

        @Override
        public void serialize(FileWriter fileWriter)
        {
            ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
            MutableListMultimap<String, CoreInstance> nodesByClassifierId = getNodesByClassifierId(this.runtime.getModelRepository(), processorSupport);

            // Build string cache
            DistributedStringCache stringCache = DistributedStringCache.fromNodes(nodesByClassifierId.valuesView(), processorSupport);
            BinaryObjSerializer serializer = new BinaryObjSerializerWithStringCacheAndImplicitIdentifiers(stringCache);

            // Write string cache
            stringCache.write(this.metadataName, fileWriter);

            // Write instances
            int partition = 0;
            int partitionTotalBytes = 0;
            ByteArrayOutputStream binByteStream = new ByteArrayOutputStream(MAX_BIN_FILE_BYTES);
            try (Writer binFileWriter = BinaryWriters.newBinaryWriter(binByteStream))
            {
                ByteArrayOutputStream indexByteStream = new ByteArrayOutputStream();
                try (Writer indexFileWriter = BinaryWriters.newBinaryWriter(indexByteStream))
                {
                    for (String classifierId : nodesByClassifierId.keysView().toSortedList())
                    {
                        ListIterable<CoreInstance> classifierObjs = nodesByClassifierId.get(classifierId).toSortedListBy(coreInstance -> IdBuilder.buildId(coreInstance, processorSupport));

                        // Initial index information
                        indexFileWriter.writeInt(classifierObjs.size()); // total obj count
                        indexFileWriter.writeInt(partition); // initial partition
                        indexFileWriter.writeInt(partitionTotalBytes); // initial byte offset in partition

                        MutableList<ObjSerialization> partitionObjSerializations = Lists.mutable.empty();
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        try (Writer writer = BinaryWriters.newBinaryWriter(byteStream))
                        {
                            GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
                            for (CoreInstance coreInstance : classifierObjs)
                            {
                                Obj obj = GraphSerializer.buildObjWithProperties(coreInstance, classifierCaches, processorSupport);
                                //objSerialization
                                ObjSerialization objSerialization = serializeClassifierObj(serializer, byteStream, writer, obj);
                                int objByteCount = objSerialization.bytes.length;
                                if (partitionTotalBytes + objByteCount > MAX_BIN_FILE_BYTES)
                                {
                                    // Write current partition
                                    try (Writer writer1 = fileWriter.getWriter(DistributedMetadataFiles.getMetadataPartitionBinFilePath(this.metadataName, partition)))
                                    {
                                        writer1.writeBytes(binByteStream.toByteArray());
                                        binByteStream.reset();
                                    }

                                    // Write partition portion of classifier index
                                    indexFileWriter.writeInt(partitionObjSerializations.size());
                                    for (ObjSerialization partitionObjSerialization : partitionObjSerializations)
                                    {
                                        indexFileWriter.writeInt(stringCache.getStringId(partitionObjSerialization.identifier));
                                        indexFileWriter.writeInt(partitionObjSerialization.bytes.length);
                                    }

                                    // New partition
                                    partition++;
                                    if (partition < 0)
                                    {
                                        throw new RuntimeException("Too many partitions");
                                    }
                                    partitionTotalBytes = 0;
                                    partitionObjSerializations.clear();
                                }
                                binFileWriter.writeBytes(objSerialization.bytes);
                                partitionTotalBytes += objByteCount;
                                partitionObjSerializations.add(objSerialization);
                            }
                        }

                        // Write final partition portion of classifier index
                        if (partitionObjSerializations.notEmpty())
                        {
                            indexFileWriter.writeInt(partitionObjSerializations.size());
                            for (ObjSerialization partitionObjSerialization : partitionObjSerializations)
                            {
                                indexFileWriter.writeInt(stringCache.getStringId(partitionObjSerialization.identifier));
                                indexFileWriter.writeInt(partitionObjSerialization.bytes.length);
                            }
                        }
                        // Write classifier index
                        try (Writer writer1 = fileWriter.getWriter(DistributedMetadataFiles.getMetadataClassifierIndexFilePath(this.metadataName, classifierId)))
                        {
                            writer1.writeBytes(indexByteStream.toByteArray());
                            indexByteStream.reset();
                        }
                    }
                }
            }

            // Write final partition
            if (binByteStream.size() > 0)
            {
                try (Writer writer = fileWriter.getWriter(DistributedMetadataFiles.getMetadataPartitionBinFilePath(this.metadataName, partition)))
                {
                    writer.writeBytes(binByteStream.toByteArray());
                }
            }
        }
    }

    public static DistributedBinaryGraphSerializer newSerializer(Serialized serialized)
    {
        return newSerializer(null, serialized);
    }

    public static DistributedBinaryGraphSerializer newSerializer(String metadataName, Serialized serialized)
    {
        return new DistributedBinaryGraphSerializerFromSerialized(metadataName, serialized);
    }

    public static DistributedBinaryGraphSerializer newSerializer(PureRuntime runtime)
    {
        return newSerializer(null, runtime);
    }

    public static DistributedBinaryGraphSerializer newSerializer(String metadataName, PureRuntime runtime)
    {
        return new DistributedBinaryGraphSerializerFromPureRuntime(metadataName, runtime);
    }

    public static void serialize(PureRuntime runtime, Path directory)
    {
        newSerializer(runtime).serializeToDirectory(directory);
    }

    public static void serialize(Serialized serialized, JarOutputStream stream)
    {
        newSerializer(serialized).serializeToJar(stream);
    }

    public static void serialize(Serialized serialized, Path directory)
    {
        newSerializer(serialized).serializeToDirectory(directory);
    }

    private static ListIterable<ObjSerialization> serializeClassifierObjs(BinaryObjSerializer serializer, ListIterable<? extends Obj> classifierObjs)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (Writer writer = BinaryWriters.newBinaryWriter(byteStream))
        {
            return classifierObjs.toSortedListBy(Obj::getIdentifier).collect(obj -> serializeClassifierObj(serializer, byteStream, writer, obj));
        }
    }

    private static ObjSerialization serializeClassifierObj(BinaryObjSerializer serializer, ByteArrayOutputStream byteStream, Writer writer, Obj obj)
    {
        byteStream.reset();
        serializer.serializeObj(writer, obj);
        return new ObjSerialization(obj.getIdentifier(), byteStream.toByteArray());
    }

    private static MutableListMultimap<String, CoreInstance> getNodesByClassifierId(ModelRepository repository, ProcessorSupport processorSupport)
    {
        MutableListMultimap<String, CoreInstance> nodesByClassifierId = Multimaps.mutable.list.empty();

        MutableMap<CoreInstance, String> classifierIds = Maps.mutable.empty();
        MutableSet<CoreInstance> primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(repository).toSet();
        MutableStack<CoreInstance> stack = Stacks.mutable.withAll(repository.getTopLevels());
        MutableSet<CoreInstance> visited = Sets.mutable.empty();
        while (stack.notEmpty())
        {
            CoreInstance node = stack.pop();
            if (visited.add(node))
            {
                CoreInstance classifier = node.getClassifier();
                String classifierId = classifierIds.getIfAbsentPutWithKey(classifier, c -> MetadataJavaPaths.buildMetadataKeyFromType(c).intern());
                nodesByClassifierId.put(classifierId, node);
                LazyIterate.flatCollect(node.getKeys(), key -> Instance.getValueForMetaPropertyToManyResolved(node, key, processorSupport))
                        .select(v -> !primitiveTypes.contains(v.getClassifier()))
                        .forEach(stack::push);
            }
        }
        return nodesByClassifierId;
    }

    private static class ObjSerialization
    {
        private final String identifier;
        private final byte[] bytes;

        private ObjSerialization(String identifier, byte[] bytes)
        {
            this.identifier = identifier;
            this.bytes = bytes;
        }
    }
}
