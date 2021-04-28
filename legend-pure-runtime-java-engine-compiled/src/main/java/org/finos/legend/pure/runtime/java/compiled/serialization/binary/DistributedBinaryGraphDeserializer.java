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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DistributedBinaryGraphDeserializer
{
    private static final Comparator<SourceCoordinates> OFFSET_COMPARATOR = new Comparator<SourceCoordinates>()
    {
        @Override
        public int compare(SourceCoordinates sourceCoordinates1, SourceCoordinates sourceCoordinates2)
        {
            return Long.compare(sourceCoordinates1.offset, sourceCoordinates2.offset);
        }
    };

    private final FileReader fileReader;
    private final LazyStringIndex stringIndex;
    private final ImmutableMap<String, ClassifierIndex> classifierIndexes;

    public static void setSourceCoordinateMapProvider(SourceCoordinateMapProvider sourceCoordinateMapProvider) {
        DistributedBinaryGraphDeserializer.sourceCoordinateMapProvider = sourceCoordinateMapProvider;
    }

    private static SourceCoordinateMapProvider sourceCoordinateMapProvider = new DefaultSourceCoordinateMapImpl();

    private DistributedBinaryGraphDeserializer(FileReader fileReader)
    {
        this.fileReader = fileReader;
        this.stringIndex = LazyStringIndex.fromFileReader(fileReader);
        this.classifierIndexes = buildClassifierIndexMap(this.stringIndex.getClassifierIds());
    }

    private ImmutableMap<String, ClassifierIndex> buildClassifierIndexMap(RichIterable<String> classifierIds)
    {
        MutableMap<String, ClassifierIndex> map = UnifiedMap.newMap(classifierIds.size());
        for (String classifierId : classifierIds)
        {
            map.put(classifierId, new ClassifierIndex(classifierId));
        }
        return map.toImmutable();
    }

    public boolean hasClassifier(String classifierId)
    {
        return this.classifierIndexes.containsKey(classifierId);
    }

    public RichIterable<String> getClassifiers()
    {
        return this.classifierIndexes.keysView();
    }

    public boolean hasInstance(String classifierId, String instanceId)
    {
        return hasClassifier(classifierId) && getClassifierIndex(classifierId).hasInstance(instanceId);
    }

    public RichIterable<String> getClassifierInstanceIds(String classifierId)
    {
        return hasClassifier(classifierId) ? getClassifierIndex(classifierId).getInstanceIds() : Lists.immutable.<String>empty();
    }

    public Obj getInstance(String classifierId, String instanceId) throws IOException
    {
        if (!hasClassifier(classifierId))
        {
            throw new RuntimeException("Unknown classifier id: '" + classifierId + "'");
        }
        ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
        SourceCoordinates sourceCoordinates = classifierIndex.getSourceCoordinates(instanceId);
        if (sourceCoordinates == null)
        {
            throw new RuntimeException("Unknown instance: classifier='" + classifierId + "', id='" + instanceId + "'");
        }
        return sourceCoordinates.getObj(this.fileReader, this.stringIndex, classifierIndex);
    }

    public ListIterable<Obj> getInstances(String classifierId, Iterable<String> instanceIds) throws IOException
    {
        if (!hasClassifier(classifierId))
        {
            throw new RuntimeException("Unknown classifier id: '" + classifierId + "'");
        }

        ClassifierIndex classifierIndex = getClassifierIndex(classifierId);
        MutableMap<String, MutableList<SourceCoordinates>> sourceCoordinatesByFile = Maps.mutable.empty();
        int size = 0;
        for (String instanceId : instanceIds)
        {
            SourceCoordinates sourceCoordinates = classifierIndex.getSourceCoordinates(instanceId);
            if (sourceCoordinates == null)
            {
                throw new RuntimeException("Unknown instance: classifier='" + classifierId + "', id='" + instanceId + "'");
            }
            sourceCoordinatesByFile.getIfAbsentPut(sourceCoordinates.getFilePath(), Functions0.<SourceCoordinates>newFastList()).add(sourceCoordinates);
            size++;
        }
        if (size == 0)
        {
            return Lists.immutable.empty();
        }

        MutableList<Obj> objs = FastList.newList(size);
        for (MutableList<SourceCoordinates> fileSourceCoordinates : sourceCoordinatesByFile.valuesView())
        {
            fileSourceCoordinates.sortThis(OFFSET_COMPARATOR);
            int offset = 0;
            try (Reader reader = this.fileReader.getReader(fileSourceCoordinates.get(0).getFilePath()))
            {
                for (SourceCoordinates sourceCoordinates : fileSourceCoordinates)
                {
                    objs.add(sourceCoordinates.getObj(reader, offset, this.stringIndex, classifierIndex));
                    offset = sourceCoordinates.getOffsetAfterReading();
                }
            }
        }
        return objs;
    }

    private ClassifierIndex getClassifierIndex(String classifierId)
    {
        return this.classifierIndexes.get(classifierId);
    }

    private MapIterable<String, SourceCoordinates> readInstanceIndex(Reader reader, String classifier)
    {
        int instanceCount = reader.readInt();
        MutableMap<String, SourceCoordinates> index = sourceCoordinateMapProvider.getMap(instanceCount, classifier);

        int instancePartition = reader.readInt();
        int offset = reader.readInt();
        String filePath = DistributedBinaryGraphSerializer.getMetadataBinFilePath(Integer.toString(instancePartition));

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
            filePath = DistributedBinaryGraphSerializer.getMetadataBinFilePath(Integer.toString(instancePartition));
        }

        return index;
    }

    public static DistributedBinaryGraphDeserializer fromClassLoader(ClassLoader classLoader)
    {
        return new DistributedBinaryGraphDeserializer(new ClassLoaderFileReader(classLoader));
    }

    public static DistributedBinaryGraphDeserializer fromDirectory(Path directory)
    {
        return new DistributedBinaryGraphDeserializer(new FileSystemFileReader(directory));
    }

    public static DistributedBinaryGraphDeserializer fromInMemoryByteArrays(MapIterable<String, byte[]> fileBytes)
    {
        return new DistributedBinaryGraphDeserializer(new InMemoryByteArrayFileReader(fileBytes));
    }

    public static DistributedBinaryGraphDeserializer fromInMemoryByteLists(MapIterable<String, ? extends ByteList> fileBytes)
    {
        return new DistributedBinaryGraphDeserializer(new InMemoryByteListFileReader(fileBytes));
    }

    public static DistributedBinaryGraphDeserializer fromZip(Path zipPath)
    {
        return new DistributedBinaryGraphDeserializer(new ZipFileReader(zipPath));
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
            if (this.index == null)
            {
                synchronized (this)
                {
                    if (this.index == null)
                    {
                        try (Reader reader = DistributedBinaryGraphDeserializer.this.fileReader.getReader(DistributedBinaryGraphSerializer.getMetadataIndexFilePath(this.classifierId)))
                        {
                            this.index = readInstanceIndex(reader, this.classifierId);
                        }
                    }
                }
            }
            return this.index;
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

        private Obj getObj(FileReader fileReader, StringIndex stringIndex, ClassifierIndex classifierIndex) throws IOException
        {
            return getObj(getBytes(fileReader), stringIndex, classifierIndex);
        }

        private Obj getObj(Reader reader, long currentOffset, StringIndex stringIndex, ClassifierIndex classifierIndex) throws IOException
        {
            return getObj(getBytes(reader, currentOffset), stringIndex, classifierIndex);
        }

        private Obj getObj(byte[] bytes, StringIndex stringIndex, ClassifierIndex classifierIndex)
        {
            Reader reader = BinaryReaders.newBinaryReader(bytes);
            return getDeserializer(stringIndex, classifierIndex).deserialize(reader);
        }

        private BinaryObjDeserializer getDeserializer(StringIndex stringIndex, ClassifierIndex classifierIndex)
        {
            return new BinaryObjDeserializerWithStringIndexAndImplicitIdentifiers(stringIndex, this.identifier, classifierIndex.getClassifierId());
        }

        private byte[] getBytes(FileReader fileReader) throws IOException
        {
            try (Reader reader = fileReader.getReader(this.filePath))
            {
                reader.skipBytes(this.offset);
                return reader.readBytes(this.length);
            }
        }

        private byte[] getBytes(Reader reader, long currentOffset) throws IOException
        {
            if (this.offset < currentOffset)
            {
                throw new RuntimeException("Cannot get bytes at offset " + this.offset + ": already at offset " + currentOffset);
            }
            reader.skipBytes(this.offset - currentOffset);
            return reader.readBytes(this.length);
        }

        private int getOffsetAfterReading()
        {
            return this.offset + this.length;
        }
    }

    private static class ClassLoaderFileReader implements FileReader
    {
        private final ClassLoader classLoader;

        private ClassLoaderFileReader(ClassLoader classLoader)
        {
            this.classLoader = classLoader;
        }

        @Override
        public Reader getReader(String path)
        {
            InputStream stream = this.classLoader.getResourceAsStream(path);
            if (stream == null)
            {
                throw new RuntimeException("Cannot find file '" + path + "' in the class path");
            }
            return BinaryReaders.newBinaryReader(stream);
        }
    }

    private static class FileSystemFileReader implements FileReader
    {
        private final Path root;

        private FileSystemFileReader(Path root)
        {
            this.root = root;
        }

        @Override
        public Reader getReader(String path)
        {
            Path fullPath = this.root.resolve(path);
            try
            {
                return BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(fullPath)));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error accessing file '" + path + "'", e);
            }
        }
    }

    private static class InMemoryByteArrayFileReader implements FileReader
    {
        private final MapIterable<String, byte[]> bytesByPath;

        private InMemoryByteArrayFileReader(MapIterable<String, byte[]> bytesByPath)
        {
            this.bytesByPath = bytesByPath;
        }

        @Override
        public Reader getReader(String path)
        {
            byte[] bytes = this.bytesByPath.get(path);
            if (bytes == null)
            {
                throw new RuntimeException("Cannot find file '" + path + "'");
            }
            return BinaryReaders.newBinaryReader(bytes);
        }
    }

    private static class InMemoryByteListFileReader implements FileReader
    {
        private final MapIterable<String, ? extends ByteList> bytesByPath;

        private InMemoryByteListFileReader(MapIterable<String, ? extends ByteList> bytesByPath)
        {
            this.bytesByPath = bytesByPath;
        }

        @Override
        public Reader getReader(String path)
        {
            ByteList bytes = this.bytesByPath.get(path);
            if (bytes == null)
            {
                throw new RuntimeException("Cannot find file '" + path + "'");
            }
            return BinaryReaders.newBinaryReader(bytes);
        }
    }

    private static class ZipFileReader implements FileReader
    {
        private final ZipFile zipFile;

        private ZipFileReader(Path zipPath)
        {
            try
            {
                this.zipFile = new ZipFile(zipPath.toFile());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to open " + zipPath.toString(), e);
            }
        }

        @Override
        public Reader getReader(String path)
        {
            try
            {
                ZipEntry entry = this.zipFile.getEntry(path);
                return BinaryReaders.newBinaryReader(new BufferedInputStream(zipFile.getInputStream(entry)));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error accessing file '" + path + "'", e);
            }
        }
    }

    private static class DefaultSourceCoordinateMapImpl implements SourceCoordinateMapProvider {
        @Override
        public MutableMap<String, SourceCoordinates> getMap(int instanceCount, String classifier) {
            return UnifiedMap.newMap(instanceCount);
        }
    }
}
