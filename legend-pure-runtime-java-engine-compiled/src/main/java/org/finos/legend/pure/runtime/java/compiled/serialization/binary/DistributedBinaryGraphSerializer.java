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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarOutputStream;

public class DistributedBinaryGraphSerializer
{
    private static final int MAX_BIN_FILE_BYTES = 512 * 1024;

    private final String metadataName;
    private final Iterable<? extends CoreInstance> nodes;
    private final ProcessorSupport processorSupport;
    private final IdBuilder idBuilder;
    private final GraphSerializer.ClassifierCaches classifierCaches;

    private DistributedBinaryGraphSerializer(String metadataName, Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        this.metadataName = DistributedMetadataHelper.validateMetadataNameIfPresent(metadataName);
        this.nodes = nodes;
        this.processorSupport = processorSupport;
        this.idBuilder = IdBuilder.newIdBuilder(DistributedMetadataHelper.getMetadataIdPrefix(this.metadataName), this.processorSupport);
        this.classifierCaches = new GraphSerializer.ClassifierCaches(this.processorSupport);
    }

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

    public void serialize(FileWriter fileWriter)
    {
        MutableMap<String, MutableList<CoreInstance>> nodesByClassifierId = getNodesByClassifierId();

        // Build string cache
        DistributedStringCache stringCache = DistributedStringCache.newBuilder().withObjs(nodesByClassifierId.valuesView().flatCollect(l -> l).collect(this::buildObj)).build();
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
            try (Writer indexWriter = BinaryWriters.newBinaryWriter(indexByteStream))
            {
                for (String classifierId : nodesByClassifierId.keysView().toSortedList())
                {
                    ListIterable<CoreInstance> classifierObjs = nodesByClassifierId.get(classifierId).sortThisBy(this.idBuilder::buildId);

                    // Initial index information
                    indexWriter.writeInt(classifierObjs.size()); // total obj count
                    indexWriter.writeInt(partition); // initial partition
                    indexWriter.writeInt(partitionTotalBytes); // initial byte offset in partition

                    MutableList<ObjIndexInfo> partitionObjIndexInfos = Lists.mutable.empty();
                    ByteArrayOutputStream objByteStream = new ByteArrayOutputStream();
                    try (Writer objWriter = BinaryWriters.newBinaryWriter(objByteStream))
                    {
                        for (CoreInstance coreInstance : classifierObjs)
                        {
                            //Obj serialization
                            Obj obj = buildObj(coreInstance);
                            objByteStream.reset();
                            serializer.serializeObj(objWriter, obj);
                            int objByteCount = objByteStream.size();
                            if (partitionTotalBytes + objByteCount > MAX_BIN_FILE_BYTES)
                            {
                                // Write current partition
                                try (Writer writer1 = fileWriter.getWriter(DistributedMetadataHelper.getMetadataPartitionBinFilePath(this.metadataName, partition)))
                                {
                                    writer1.writeBytes(binByteStream.toByteArray());
                                    binByteStream.reset();
                                }

                                // Write partition portion of classifier index
                                indexWriter.writeInt(partitionObjIndexInfos.size());
                                partitionObjIndexInfos.forEach(info -> info.write(indexWriter, stringCache));

                                // New partition
                                partition++;
                                if (partition < 0)
                                {
                                    throw new RuntimeException("Too many partitions");
                                }
                                partitionTotalBytes = 0;
                                partitionObjIndexInfos.clear();
                            }
                            binFileWriter.writeBytes(objByteStream.toByteArray());
                            partitionTotalBytes += objByteCount;
                            partitionObjIndexInfos.add(new ObjIndexInfo(obj.getIdentifier(), objByteCount));
                        }
                    }

                    // Write final partition portion of classifier index
                    if (partitionObjIndexInfos.notEmpty())
                    {
                        indexWriter.writeInt(partitionObjIndexInfos.size());
                        partitionObjIndexInfos.forEach(info -> info.write(indexWriter, stringCache));
                    }

                    // Write classifier index
                    try (Writer writer1 = fileWriter.getWriter(DistributedMetadataHelper.getMetadataClassifierIndexFilePath(this.metadataName, classifierId)))
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
            try (Writer writer = fileWriter.getWriter(DistributedMetadataHelper.getMetadataPartitionBinFilePath(this.metadataName, partition)))
            {
                writer.writeBytes(binByteStream.toByteArray());
            }
        }
    }

    private Obj buildObj(CoreInstance instance)
    {
        return GraphSerializer.buildObj(instance, this.idBuilder, this.classifierCaches, this.processorSupport);
    }

    private MutableMap<String, MutableList<CoreInstance>> getNodesByClassifierId()
    {
        MutableMap<String, MutableList<CoreInstance>> nodesByClassifierId = Maps.mutable.empty();
        MutableMap<CoreInstance, String> classifierIdCache = Maps.mutable.empty();
        MutableSet<CoreInstance> excludedTypes = PrimitiveUtilities.getPrimitiveTypes(this.processorSupport).toSet();
        AnyStubHelper.getStubClasses().collect(this.processorSupport::package_getByUserPath, excludedTypes);
        this.nodes.forEach(node ->
        {
            CoreInstance classifier = node.getClassifier();
            if (!excludedTypes.contains(classifier))
            {
                String classifierId = classifierIdCache.getIfAbsentPutWithKey(classifier, MetadataJavaPaths::buildMetadataKeyFromType);
                nodesByClassifierId.getIfAbsentPut(classifierId, Lists.mutable::empty).add(node);
            }
        });
        return nodesByClassifierId;
    }

    public static DistributedBinaryGraphSerializer newSerializer(String metadataName, Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        Objects.requireNonNull(nodes, "nodes may not be null");
        Objects.requireNonNull(processorSupport, "processorSupport may not be null");
        return new DistributedBinaryGraphSerializer(metadataName, nodes, processorSupport);
    }

    public static DistributedBinaryGraphSerializer newSerializer(PureRuntime runtime)
    {
        return newSerializer(null, runtime);
    }

    public static DistributedBinaryGraphSerializer newSerializer(String metadataName, PureRuntime runtime)
    {
        return newSerializer(metadataName, GraphNodeIterable.fromModelRepository(runtime.getModelRepository()), runtime.getProcessorSupport());
    }

    public static void serialize(PureRuntime runtime, Path directory)
    {
        newSerializer(runtime).serializeToDirectory(directory);
    }

    private static class ObjIndexInfo
    {
        private final String identifier;
        private final int size;

        private ObjIndexInfo(String identifier, int size)
        {
            this.identifier = identifier;
            this.size = size;
        }

        private void write(Writer writer, StringCache stringCache)
        {
            writer.writeInt(stringCache.getStringId(this.identifier));
            writer.writeInt(this.size);
        }
    }
}
