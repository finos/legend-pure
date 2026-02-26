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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;

public abstract class DistributedBinaryGraphSerializer
{
    private static final int MAX_BIN_FILE_BYTES = 512 * 1024;

    private final DistributedMetadataSpecification metadataSpecification;
    protected final PureRuntime runtime;
    protected final ProcessorSupport processorSupport;
    private final IdBuilder idBuilder;
    private final GraphSerializer.ClassifierCaches classifierCaches;

    protected DistributedBinaryGraphSerializer(PureRuntime runtime, DistributedMetadataSpecification metadataSpecification)
    {
        this.metadataSpecification = metadataSpecification;
        this.runtime = runtime;
        this.processorSupport = runtime.getProcessorSupport();
        this.idBuilder = newPossiblyHashedIdBuilder(this.metadataSpecification, this.processorSupport);
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
        // Possibly write metadata specification
        if (this.metadataSpecification != null)
        {
            this.metadataSpecification.writeSpecification(fileWriter);
        }

        // Compute instances for serialization
        SerializationCollector serializationCollector = new SerializationCollector();
        collectInstancesForSerialization(serializationCollector);

        // Build string cache
        DistributedStringCache stringCache = buildStringCache(serializationCollector);
        BinaryObjSerializer serializer = new BinaryObjSerializerWithStringCacheAndImplicitIdentifiers(stringCache);

        // Write string cache
        stringCache.write(getMetadataName(), fileWriter);

        // Write instances
        int partition = 0;
        int partitionTotalBytes = 0;
        WriterBufferOutputStream binByteStream = new WriterBufferOutputStream(MAX_BIN_FILE_BYTES);
        try (Writer binFileWriter = BinaryWriters.newBinaryWriter(binByteStream))
        {
            WriterBufferOutputStream indexByteStream = new WriterBufferOutputStream();
            try (Writer indexWriter = BinaryWriters.newBinaryWriter(indexByteStream))
            {
                for (String classifierId : stringCache.getClassifierIds().toSortedList())
                {
                    ListIterable<Obj> classifierObjs = getClassifierObjs(serializationCollector.instancesForSerialization.remove(classifierId), serializationCollector.objUpdates.remove(classifierId));

                    // Initial index information
                    indexWriter.writeInt(classifierObjs.size()); // total obj count
                    indexWriter.writeInt(partition); // initial partition
                    indexWriter.writeInt(partitionTotalBytes); // initial byte offset in partition

                    MutableList<ObjIndexInfo> partitionObjIndexInfos = Lists.mutable.empty();
                    WriterBufferOutputStream objByteStream = new WriterBufferOutputStream();
                    try (Writer objWriter = BinaryWriters.newBinaryWriter(objByteStream))
                    {
                        for (Obj obj : classifierObjs)
                        {
                            // Obj serialization
                            serializer.serializeObj(objWriter, obj);
                            int objByteCount = objByteStream.size();
                            if (partitionTotalBytes + objByteCount > MAX_BIN_FILE_BYTES)
                            {
                                // Write current partition
                                try (Writer partitionWriter = fileWriter.getWriter(DistributedMetadataHelper.getMetadataPartitionBinFilePath(getMetadataName(), partition)))
                                {
                                    binByteStream.writeAndReset(partitionWriter);
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
                            objByteStream.writeAndReset(binFileWriter);
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
                    try (Writer indexFileWriter = fileWriter.getWriter(DistributedMetadataHelper.getMetadataClassifierIndexFilePath(getMetadataName(), classifierId)))
                    {
                        indexByteStream.writeAndReset(indexFileWriter);
                    }
                }
            }
        }

        // Write final partition
        if (binByteStream.size() > 0)
        {
            try (Writer partitionWriter = fileWriter.getWriter(DistributedMetadataHelper.getMetadataPartitionBinFilePath(getMetadataName(), partition)))
            {
                binByteStream.write(partitionWriter);
            }
        }
    }

    protected String getMetadataName()
    {
        return (this.metadataSpecification == null) ? null : this.metadataSpecification.getName();
    }

    protected abstract void collectInstancesForSerialization(SerializationCollector serializationCollector);

    private DistributedStringCache buildStringCache(SerializationCollector serializationCollector)
    {
        StringCache.Builder<DistributedStringCache> stringCacheBuilder = DistributedStringCache.newBuilder();
        serializationCollector.instancesForSerialization.forEachValue(instances -> instances.forEach(i -> stringCacheBuilder.withObj(buildObj(i))));
        serializationCollector.objUpdates.forEachValue(stringCacheBuilder::withObjs);
        return stringCacheBuilder.build();
    }

    private ListIterable<Obj> getClassifierObjs(ListIterable<? extends CoreInstance> classifierInstances, ListIterable<? extends Obj> classifierObjUpdates)
    {
        MutableList<Obj> classifierObjs = Lists.mutable.withInitialCapacity(((classifierInstances == null) ? 0 : classifierInstances.size()) + ((classifierObjUpdates == null) ? 0 : classifierObjUpdates.size()));
        if (classifierInstances != null)
        {
            // skip duplicates while building Objs
            MutableSet<CoreInstance> seenInstances = Sets.mutable.withInitialCapacity(classifierInstances.size());
            classifierInstances.collectIf(seenInstances::add, this::buildObj, classifierObjs);
        }
        if (classifierObjUpdates != null)
        {
            classifierObjs.addAllIterable(classifierObjUpdates);
        }
        if (classifierObjs.size() > 1)
        {
            // TODO there is a known issue with id conflicts for ImportGroups - remove conflicts until issue is fixed
            if (M3Paths.ImportGroup.equals(classifierObjs.get(0).getClassifier()))
            {
                MutableSet<String> ids = Sets.mutable.empty();
                classifierObjs.removeIf(o -> !ids.add(o.getIdentifier()));
            }

            // If we have more than one, sort and then validate that we don't have identifier clashes
            classifierObjs.sortThisBy(Obj::getIdentifier);
            classifierObjs.injectInto((Obj) null, (previous, obj) ->
            {
                if ((previous != null) && obj.getIdentifier().equals(previous.getIdentifier()))
                {
                    throw new IllegalStateException("Obj identifier clash: " + previous.toString(true) + " vs " + obj.toString(true));
                }
                return obj;
            });
        }
        return classifierObjs;
    }

    protected String buildClassifierId(CoreInstance instance)
    {
        return this.classifierCaches.getClassifierId(instance.getClassifier());
    }

    protected String buildInstanceId(CoreInstance instance)
    {
        return this.idBuilder.buildId(instance);
    }

    protected Obj buildObj(CoreInstance instance)
    {
        return GraphSerializer.buildObj(instance, this.idBuilder, this.classifierCaches, this.processorSupport);
    }

    @Deprecated
    public static DistributedBinaryGraphSerializer newSerializer(String metadataName, PureRuntime runtime)
    {
        if (metadataName == null)
        {
            return newSerializer(runtime);
        }
        throw new UnsupportedOperationException();
    }

    public static DistributedBinaryGraphSerializer newSerializer(PureRuntime runtime)
    {
        return new DistributedBinaryFullGraphSerializer(runtime);
    }

    public static DistributedBinaryGraphSerializer newSerializer(PureRuntime runtime, String repositoryName)
    {
        MutableRepositoryCodeStorage codeStorage = runtime.getCodeStorage();
        CodeRepository repository = codeStorage.getRepository(repositoryName);
        if (repository == null)
        {
            throw new IllegalArgumentException("Unknown repository: \"" + repositoryName + "\"");
        }
        RichIterable<CodeRepository> allRepositories = codeStorage.getAllRepositories();
        if (allRepositories.anySatisfy(r -> (r != repository) && r.isVisible(repository)))
        {
            StringBuilder builder = new StringBuilder("Cannot serialize repository \"").append(repositoryName).append(("\", with "));
            allRepositories.collectIf(r -> (r != repository) && r.isVisible(repository), CodeRepository::getName, Lists.mutable.empty())
                    .sortThis()
                    .appendString(builder, "\"", "\", \"", "\" in the runtime");
            throw new IllegalArgumentException(builder.toString());
        }
        MutableSet<String> directDependencies = allRepositories.collectIf(r -> (r != repository) && repository.isVisible(r), CodeRepository::getName, Sets.mutable.empty());
        DistributedMetadataSpecification metadataSpecification = DistributedMetadataSpecification.newSpecification(repositoryName, directDependencies);
        return new DistributedBinaryRepositorySerializer(metadataSpecification, runtime);
    }

    public static void serialize(PureRuntime runtime, Path directory)
    {
        newSerializer(runtime).serializeToDirectory(directory);
    }

    public static IdBuilder newIdBuilder(String metadataName, ProcessorSupport processorSupport)
    {
        return newIdBuilder_internal(DistributedMetadataHelper.validateMetadataNameIfPresent(metadataName), processorSupport);
    }

    public static IdBuilder newIdBuilder(DistributedMetadataSpecification metadataSpec, ProcessorSupport processorSupport)
    {
        return newIdBuilder_internal((metadataSpec == null) ? null : metadataSpec.getName(), processorSupport);
    }

    private static IdBuilder newIdBuilder_internal(String metadataName, ProcessorSupport processorSupport)
    {
        if (metadataName == null)
        {
            // if metadata name is not present, we are serializing the full graph; in this case, use legacy ids
            return IdBuilder.legacyBuilder(processorSupport).build();
        }
        return IdBuilder.builder(processorSupport)
                .withDefaultIdPrefix(DistributedMetadataHelper.getMetadataIdPrefix(metadataName))
                .build();
    }

    private static IdBuilder newPossiblyHashedIdBuilder(DistributedMetadataSpecification metadataSpec, ProcessorSupport processorSupport)
    {
        IdBuilder idBuilder = newIdBuilder(metadataSpec, processorSupport);
        return (metadataSpec == null) ? idBuilder : DistributedMetadataHelper.possiblyHashIds(idBuilder);
    }

    protected class SerializationCollector
    {
        private final MutableMap<String, MutableList<CoreInstance>> instancesForSerialization = Maps.mutable.empty();
        private final MutableMap<String, MutableList<Obj>> objUpdates = Maps.mutable.empty();

        public void collectInstanceForSerialization(CoreInstance instance)
        {
            String classifierId = buildClassifierId(instance);
            this.instancesForSerialization.getIfAbsentPut(classifierId, Lists.mutable::empty).add(instance);
        }

        public void collectObjUpdate(Obj objUpdate)
        {
            this.objUpdates.getIfAbsentPut(objUpdate.getClassifier(), Lists.mutable::empty).add(objUpdate);
        }
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

    private static class WriterBufferOutputStream extends ByteArrayOutputStream
    {
        private WriterBufferOutputStream()
        {
            super();
        }

        private WriterBufferOutputStream(int size)
        {
            super(size);
        }

        private synchronized void write(Writer writer)
        {
            writer.writeBytes(this.buf, 0, this.count);
        }

        private synchronized void writeAndReset(Writer writer)
        {
            write(writer);
            reset();
        }
    }
}
