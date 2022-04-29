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
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;

public abstract class DistributedBinaryGraphDeserializer
{
    private static SourceCoordinateMapProvider sourceCoordinateMapProvider = (instanceCount, classifier) -> Maps.mutable.withInitialCapacity(instanceCount);

    public abstract boolean hasClassifier(String classifierId);

    public abstract RichIterable<String> getClassifiers();

    public abstract boolean hasInstance(String classifierId, String instanceId);

    public abstract RichIterable<String> getClassifierInstanceIds(String classifierId);

    public final Obj getInstance(String classifierId, String instanceId)
    {
        return getInstance(classifierId, instanceId, true);
    }

    public final Obj getInstanceIfPresent(String classifierId, String instanceId)
    {
        return getInstance(classifierId, instanceId, false);
    }

    protected abstract Obj getInstance(String classifierId, String instanceId, boolean throwIfNotFound);

    public final ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds)
    {
        return getInstances(classifierId, instanceIds, true);
    }

    public final ListIterable<Obj> getInstancesIfPresent(String classifierId, Iterable<String> instanceIds)
    {
        return getInstances(classifierId, instanceIds, false);
    }

    protected abstract ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds, boolean throwIfNotFound);

    public static void setSourceCoordinateMapProvider(SourceCoordinateMapProvider provider)
    {
        sourceCoordinateMapProvider = provider;
    }

    public static void validateFullyDefined(Obj obj)
    {
        if (!isFullyDefined(obj))
        {
            throw new ObjNotFullyDefinedException(obj);
        }
    }

    public static boolean isFullyDefined(Obj obj)
    {
        return (obj.getClassifier() != null) &&
                (obj.getIdentifier() != null) &&
                (obj.getName() != null);
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromClassLoader(ClassLoader classLoader)
    {
        return newBuilder(classLoader).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromClassLoader(String metadataName, ClassLoader classLoader)
    {
        return newBuilder(classLoader).withMetadataName(metadataName).build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromDirectory(Path directory)
    {
        return newBuilder(directory).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromDirectory(String metadataName, Path directory)
    {
        return newBuilder(directory).withMetadataName(metadataName).build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromInMemoryByteArrays(Map<String, byte[]> fileBytes)
    {
        return newBuilder(fileBytes).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromInMemoryByteArrays(String metadataName, Map<String, byte[]> fileBytes)
    {
        return newBuilder(fileBytes).withMetadataName(metadataName).build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromInMemoryByteLists(Map<String, ? extends ByteList> fileBytes)
    {
        return newBuilder(FileReaders.fromInMemoryByteLists(fileBytes)).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromInMemoryByteLists(String metadataName, Map<String, ? extends ByteList> fileBytes)
    {
        return newBuilder(FileReaders.fromInMemoryByteLists(fileBytes)).withMetadataName(metadataName).build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromZip(ZipFile zipFile)
    {
        return newBuilder(zipFile).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromZip(String metadataName, ZipFile zipFile)
    {
        return newBuilder(zipFile).withMetadataName(metadataName).build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromFileReader(FileReader fileReader)
    {
        return newBuilder(fileReader).withNoMetadataName().build();
    }

    @Deprecated
    public static DistributedBinaryGraphDeserializer fromFileReader(String metadataName, FileReader fileReader)
    {
        return newBuilder(fileReader).withMetadataName(metadataName).build();
    }

    public static Builder newBuilder(FileReader fileReader)
    {
        return new Builder(fileReader);
    }

    public static Builder newBuilder(Path directory)
    {
        return newBuilder(FileReaders.fromDirectory(directory));
    }

    public static Builder newBuilder(ClassLoader classLoader)
    {
        return newBuilder(FileReaders.fromClassLoader(classLoader));
    }

    public static Builder newBuilder(ZipFile zipFile)
    {
        return newBuilder(FileReaders.fromZipFile(zipFile));
    }

    public static Builder newBuilder(Map<String, byte[]> fileBytes)
    {
        return newBuilder(FileReaders.fromInMemoryByteArrays(fileBytes));
    }

    public static class Builder
    {
        private final FileReader fileReader;
        private boolean validateObjs = true;
        private MutableSet<String> metadataNames = null;

        private Builder(FileReader fileReader)
        {
            this.fileReader = Objects.requireNonNull(fileReader, "Metadata location must be specified");
        }

        public Builder withObjValidation(boolean validateObjs)
        {
            this.validateObjs = validateObjs;
            return this;
        }

        public Builder withObjValidation()
        {
            return withObjValidation(true);
        }

        public Builder withoutObjValidation()
        {
            return withObjValidation(false);
        }

        public Builder withNoMetadataName()
        {
            this.metadataNames = null;
            return this;
        }

        public Builder withMetadataName(String metadataName)
        {
            if (this.metadataNames == null)
            {
                this.metadataNames = Sets.mutable.with(metadataName);
            }
            else
            {
                this.metadataNames.add(metadataName);
            }
            return this;
        }

        public Builder withMetadataNames(String... metadataNames)
        {
            if (this.metadataNames == null)
            {
                this.metadataNames = Sets.mutable.with(metadataNames);
            }
            else
            {
                ArrayIterate.forEach(metadataNames, this.metadataNames::add);
            }
            return this;
        }

        public Builder withMetadataNames(Iterable<String> metadataNames)
        {
            if (this.metadataNames == null)
            {
                this.metadataNames = Sets.mutable.withAll(metadataNames);
            }
            else
            {
                this.metadataNames.addAllIterable(metadataNames);
            }
            return this;
        }

        public DistributedBinaryGraphDeserializer build()
        {
            if (this.metadataNames == null)
            {
                return new Single(this.fileReader, null, this.validateObjs);
            }
            switch (this.metadataNames.size())
            {
                case 0:
                {
                    return new Empty();
                }
                case 1:
                {
                    return new Single(this.fileReader, this.metadataNames.getAny(), this.validateObjs);
                }
                default:
                {
                    return new Many(this.fileReader, this.metadataNames, this.validateObjs);
                }
            }
        }
    }

    public static class SourceCoordinates implements Serializable
    {
        private final String identifier;
        private final String filePath;
        private final int offset;
        private final int length;

        private SourceCoordinates(String identifier, String filePath, int offset, int length)
        {
            this.identifier = identifier;
            this.filePath = filePath;
            this.offset = offset;
            this.length = length;
        }

        private String getFilePath()
        {
            return this.filePath;
        }

        private Obj getObj(FileReader fileReader, StringIndex stringIndex, String classifierId)
        {
            return getObj(getBytes(fileReader), stringIndex, classifierId);
        }

        private Obj getObj(Reader reader, long currentOffset, StringIndex stringIndex, String classifierId)
        {
            return getObj(getBytes(reader, currentOffset), stringIndex, classifierId);
        }

        private Obj getObj(byte[] bytes, StringIndex stringIndex, String classifierId)
        {
            try (Reader reader = BinaryReaders.newBinaryReader(bytes))
            {
                return getDeserializer(stringIndex, classifierId).deserialize(reader);
            }
        }

        private BinaryObjDeserializer getDeserializer(StringIndex stringIndex, String classifierId)
        {
            return new BinaryObjDeserializerWithStringIndexAndImplicitIdentifiers(stringIndex, this.identifier, classifierId);
        }

        private byte[] getBytes(FileReader fileReader)
        {
            try (Reader reader = fileReader.getReader(this.filePath))
            {
                reader.skipBytes(this.offset);
                return reader.readBytes(this.length);
            }
        }

        private byte[] getBytes(Reader reader, long currentOffset)
        {
            if (this.offset < currentOffset)
            {
                throw new RuntimeException("Cannot get bytes for instance '" + this.identifier + "' at offset " + this.offset + " of " + this.filePath + ": already at offset " + currentOffset);
            }
            reader.skipBytes(this.offset - currentOffset);
            return reader.readBytes(this.length);
        }

        private int getOffsetAfterReading()
        {
            return this.offset + this.length;
        }

        private static int compareByOffset(SourceCoordinates one, SourceCoordinates another)
        {
            return Integer.compare(one.offset, another.offset);
        }
    }

    private static class Empty extends DistributedBinaryGraphDeserializer
    {
        @Override
        public boolean hasClassifier(String classifierId)
        {
            return false;
        }

        @Override
        public RichIterable<String> getClassifiers()
        {
            return Lists.immutable.empty();
        }

        @Override
        public boolean hasInstance(String classifierId, String instanceId)
        {
            return false;
        }

        @Override
        public RichIterable<String> getClassifierInstanceIds(String classifierId)
        {
            return Lists.immutable.empty();
        }

        @Override
        protected Obj getInstance(String classifierId, String instanceId, boolean throwIfNotFound)
        {
            if (throwIfNotFound)
            {
                throw new UnknownClassifierException(classifierId);
            }
            return null;
        }

        @Override
        protected ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds, boolean throwIfNotFound)
        {
            if (throwIfNotFound)
            {
                throw new UnknownClassifierException(classifierId);
            }
            return Lists.immutable.empty();
        }
    }

    private static class Single extends DistributedBinaryGraphDeserializer
    {
        private final String metadataName;
        private final FileReader fileReader;
        private final LazyStringIndex stringIndex;
        private final ImmutableMap<String, ClassifierIndex> classifierIndexes;
        private final boolean validateObjs;

        private Single(FileReader fileReader, String metadataName, boolean validateObjs)
        {
            this.metadataName = DistributedMetadataHelper.validateMetadataNameIfPresent(metadataName);
            this.fileReader = fileReader;
            this.stringIndex = LazyStringIndex.fromFileReader(this.metadataName, fileReader);
            RichIterable<String> classifierIds = this.stringIndex.getClassifierIds();
            this.classifierIndexes = classifierIds.toMap(id -> id, ClassifierIndex::new, Maps.mutable.withInitialCapacity(classifierIds.size())).toImmutable();
            this.validateObjs = validateObjs;
        }

        private Single(FileReader fileReader, boolean validateObjs)
        {
            this(fileReader, null, validateObjs);
        }

        @Override
        public boolean hasClassifier(String classifierId)
        {
            return this.classifierIndexes.containsKey(classifierId);
        }

        @Override
        public RichIterable<String> getClassifiers()
        {
            return this.classifierIndexes.keysView();
        }

        @Override
        public boolean hasInstance(String classifierId, String instanceId)
        {
            ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
            return (classifierIndex != null) && classifierIndex.hasInstance(instanceId);
        }

        @Override
        public RichIterable<String> getClassifierInstanceIds(String classifierId)
        {
            ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
            return (classifierIndex == null) ? Lists.immutable.empty() : classifierIndex.getInstanceIds();
        }

        @Override
        protected Obj getInstance(String classifierId, String instanceId, boolean throwIfNotFound)
        {
            ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
            if (classifierIndex == null)
            {
                if (throwIfNotFound)
                {
                    throw new UnknownClassifierException(classifierId);
                }
                return null;
            }
            SourceCoordinates sourceCoordinates = classifierIndex.getSourceCoordinates(instanceId);
            if (sourceCoordinates == null)
            {
                if (throwIfNotFound)
                {
                    throw new UnknownInstanceException(classifierId, instanceId);
                }
                return null;
            }
            return possiblyValidate(sourceCoordinates.getObj(this.fileReader, this.stringIndex, classifierIndex.getClassifierId()));
        }

        @Override
        protected ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds, boolean throwIfNotFound)
        {
            ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
            if (classifierIndex == null)
            {
                if (throwIfNotFound)
                {
                    throw new UnknownClassifierException(classifierId);
                }
                return Lists.immutable.empty();
            }

            MutableMap<String, MutableList<SourceCoordinates>> sourceCoordinatesByFile = Maps.mutable.empty();
            int size = 0;
            for (String instanceId : instanceIds)
            {
                SourceCoordinates sourceCoordinates = classifierIndex.getSourceCoordinates(instanceId);
                if (sourceCoordinates == null && throwIfNotFound)
                {
                    throw new UnknownInstanceException(classifierId, instanceId);
                }
                if (sourceCoordinates != null)
                {
                    sourceCoordinatesByFile.getIfAbsentPut(sourceCoordinates.getFilePath(), Lists.mutable::empty).add(sourceCoordinates);
                    size++;
                }
            }
            if (size == 0)
            {
                return Lists.immutable.empty();
            }

            MutableList<Obj> objs = Lists.mutable.withInitialCapacity(size);
            sourceCoordinatesByFile.forEachKeyValue((filePath, fileSourceCoordinates) ->
            {
                fileSourceCoordinates.sortThis(SourceCoordinates::compareByOffset);
                try (Reader reader = this.fileReader.getReader(filePath))
                {
                    int offset = 0;
                    for (SourceCoordinates sourceCoordinates : fileSourceCoordinates)
                    {
                        Obj obj = possiblyValidate(sourceCoordinates.getObj(reader, offset, this.stringIndex, classifierIndex.getClassifierId()));
                        offset = sourceCoordinates.getOffsetAfterReading();
                        objs.add(obj);
                    }
                }
            });
            return objs;
        }

        private ClassifierIndex getClassifierIndex(String classifierId)
        {
            return this.classifierIndexes.get(classifierId);
        }

        private MapIterable<String, SourceCoordinates> readInstanceIndex(String classifier)
        {
            String indexFilePath = DistributedMetadataHelper.getMetadataClassifierIndexFilePath(this.metadataName, classifier);
            try (Reader reader = this.fileReader.getReader(indexFilePath))
            {
                int instanceCount = reader.readInt();
                MutableMap<String, SourceCoordinates> index = sourceCoordinateMapProvider.getMap(instanceCount, classifier);

                int instancePartition = reader.readInt();
                int offset = reader.readInt();
                String filePath = DistributedMetadataHelper.getMetadataPartitionBinFilePath(this.metadataName, instancePartition);

                int instancesRead = 0;
                while (instancesRead < instanceCount)
                {
                    int partitionInstanceCount = reader.readInt();
                    for (int i = 0; i < partitionInstanceCount; i++)
                    {
                        String identifier = this.stringIndex.getString(reader.readInt());
                        int length = reader.readInt();
                        index.put(identifier, new SourceCoordinates(identifier, filePath, offset, length));
                        offset += length;
                    }
                    instancesRead += partitionInstanceCount;
                    instancePartition++;
                    offset = 0;
                    filePath = DistributedMetadataHelper.getMetadataPartitionBinFilePath(this.metadataName, instancePartition);
                }

                return index;
            }
        }

        private Obj possiblyValidate(Obj obj)
        {
            if (this.validateObjs)
            {
                validateFullyDefined(obj);
            }
            return obj;
        }

        private class ClassifierIndex
        {
            private final String classifierId;
            private volatile MapIterable<String, SourceCoordinates> index; //NOSONAR we actually want to protect the pointer

            private ClassifierIndex(String classifierId)
            {
                this.classifierId = classifierId;
            }

            String getClassifierId()
            {
                return this.classifierId;
            }

            RichIterable<String> getInstanceIds()
            {
                return getInstanceIndex().keysView();
            }

            boolean hasInstance(String instanceId)
            {
                return getInstanceIndex().containsKey(instanceId);
            }

            SourceCoordinates getSourceCoordinates(String instanceId)
            {
                return getInstanceIndex().get(instanceId);
            }

            private MapIterable<String, SourceCoordinates> getInstanceIndex()
            {
                MapIterable<String, SourceCoordinates> localIndex = this.index;
                if (localIndex == null)
                {
                    synchronized (this)
                    {
                        localIndex = this.index;
                        if (localIndex == null)
                        {
                            this.index = localIndex = readInstanceIndex(this.classifierId);
                        }
                    }
                }
                return localIndex;
            }
        }
    }

    private static class Many extends DistributedBinaryGraphDeserializer
    {
        private final ListIterable<Single> deserializers;
        private final boolean validateObjs;

        private Many(FileReader fileReader, Set<String> metadataNames, boolean validateObjs)
        {
            this.deserializers = Iterate.collect(metadataNames, n -> new Single(fileReader, n, false), Lists.mutable.ofInitialCapacity(metadataNames.size()));
            this.validateObjs = validateObjs;
        }

        @Override
        public boolean hasClassifier(String classifierId)
        {
            return this.deserializers.anySatisfy(d -> d.hasClassifier(classifierId));
        }

        @Override
        public RichIterable<String> getClassifiers()
        {
            return this.deserializers.flatCollect(Single::getClassifiers, Sets.mutable.empty());
        }

        @Override
        public boolean hasInstance(String classifierId, String instanceId)
        {
            return this.deserializers.anySatisfy(d -> d.hasInstance(classifierId, instanceId));
        }

        @Override
        public RichIterable<String> getClassifierInstanceIds(String classifierId)
        {
            return this.deserializers.flatCollect(d -> d.getClassifierInstanceIds(classifierId), Sets.mutable.empty());
        }

        @Override
        protected Obj getInstance(String classifierId, String instanceId, boolean throwIfNotFound)
        {
            MutableList<Obj> objs = Lists.mutable.ofInitialCapacity(this.deserializers.size());
            this.deserializers.forEach(d ->
            {
                Obj obj = d.getInstanceIfPresent(classifierId, instanceId);
                if (obj != null)
                {
                    objs.add(obj);
                }
            });
            if (objs.notEmpty())
            {
                return mergeAndPossiblyValidate(classifierId, instanceId, objs);
            }
            if (throwIfNotFound)
            {
                throw new UnknownInstanceException(classifierId, instanceId);
            }
            return null;
        }

        @Override
        protected ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds, boolean throwIfNotFound)
        {
            Set<String> instanceIdSet = (instanceIds instanceof Set) ? (Set<String>) instanceIds : Sets.mutable.withAll(instanceIds);
            switch (instanceIdSet.size())
            {
                case 0:
                {
                    return Lists.immutable.empty();
                }
                case 1:
                {
                    String instanceId = Iterate.getFirst(instanceIdSet);
                    Obj obj = getInstanceIfPresent(classifierId, instanceId);
                    if (obj != null)
                    {
                        return Lists.immutable.with(obj);
                    }
                    if (throwIfNotFound)
                    {
                        throw new UnknownInstancesException(classifierId, Lists.fixedSize.with(instanceId));
                    }
                    return Lists.immutable.empty();
                }
                default:
                {
                    MutableMap<String, MutableList<Obj>> objsById = Maps.mutable.empty();
                    this.deserializers.asLazy().flatCollect(d -> d.getInstancesIfPresent(classifierId, instanceIds)).forEach(o -> objsById.getIfAbsentPut(o.getIdentifier(), Lists.mutable::empty).add(o));
                    if (throwIfNotFound && (instanceIdSet.size() > objsById.size()))
                    {
                        MutableList<String> missingInstanceIds = Iterate.reject(instanceIdSet, objsById::containsKey, Lists.mutable.empty()).sortThis();
                        throw new UnknownInstancesException(classifierId, missingInstanceIds);
                    }
                    MutableList<Obj> results = Lists.mutable.ofInitialCapacity(objsById.size());
                    objsById.forEachKeyValue((id, objs) -> results.add(mergeAndPossiblyValidate(classifierId, id, objs)));
                    return results;
                }
            }
        }

        private Obj mergeAndPossiblyValidate(String classifierId, String identifier, ListIterable<Obj> objs)
        {
            Obj obj;
            if (objs.size() == 1)
            {
                obj = objs.get(0);
            }
            else
            {
                try
                {
                    obj = Obj.merge(objs);
                }
                catch (Exception e)
                {
                    StringBuilder builder = new StringBuilder("Error merging instance \"").append(identifier).append("\" of type \"").append(classifierId).append('"');
                    String eMessage = e.getMessage();
                    if (eMessage != null)
                    {
                        builder.append(": ").append(eMessage);
                    }
                    throw new RuntimeException(builder.toString(), e);
                }
            }
            if (this.validateObjs)
            {
                validateFullyDefined(obj);
            }
            return obj;
        }
    }

    public static class DeserializationException extends RuntimeException
    {
        protected DeserializationException(String message)
        {
            super(message);
        }

        protected DeserializationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public static class UnknownClassifierException extends DeserializationException
    {
        private final String classifierId;

        private UnknownClassifierException(String classifierId)
        {
            super(generateMessage(classifierId));
            this.classifierId = classifierId;
        }

        public String getClassifierId()
        {
            return this.classifierId;
        }

        private static String generateMessage(String classifierId)
        {
            return "Unknown classifier: '" + classifierId + "'";
        }
    }

    public static class UnknownInstanceException extends DeserializationException
    {
        private final String classifierId;
        private final String instanceId;

        private UnknownInstanceException(String classifierId, String instanceId)
        {
            super(generateMessage(classifierId, instanceId));
            this.classifierId = classifierId;
            this.instanceId = instanceId;
        }

        public String getClassifierId()
        {
            return this.classifierId;
        }

        public String getInstanceId()
        {
            return this.instanceId;
        }

        private static String generateMessage(String classifierId, String instanceId)
        {
            return "Unknown instance: classifier='" + classifierId + "', id='" + instanceId + "'";
        }
    }

    public static class UnknownInstancesException extends DeserializationException
    {
        private final String classifierId;
        private final MutableList<String> instanceIds;

        private UnknownInstancesException(String classifierId, MutableList<String> instanceIds)
        {
            super(generateMessage(classifierId, instanceIds));
            this.classifierId = classifierId;
            this.instanceIds = instanceIds;
        }

        public String getClassifierId()
        {
            return this.classifierId;
        }

        public List<String> getInstanceIds()
        {
            return this.instanceIds.asUnmodifiable();
        }

        private static String generateMessage(String classifierId, ListIterable<String> instanceIds)
        {
            boolean many = instanceIds.size() != 1;
            StringBuilder builder = new StringBuilder(many ? "Unknown instances: " : "Unknown instance: ");
            builder.append("classifier='").append(classifierId).append("', id");
            if (many)
            {
                instanceIds.appendString(builder, "s='", "', '", "'");
            }
            else
            {
                builder.append("='").append(instanceIds.get(0)).append("'");
            }
            return builder.toString();
        }
    }

    public static class ObjNotFullyDefinedException extends DeserializationException
    {
        private final Obj obj;

        private ObjNotFullyDefinedException(Obj obj)
        {
            super(generateMessage(obj));
            this.obj = obj;
        }

        public Obj getObj()
        {
            return this.obj;
        }

        private static String generateMessage(Obj obj)
        {
            return obj.writeObj(new StringBuilder("Obj not fully defined: "), false).toString();
        }
    }
}
