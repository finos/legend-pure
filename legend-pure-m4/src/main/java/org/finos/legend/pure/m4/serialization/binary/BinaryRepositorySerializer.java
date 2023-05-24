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

package org.finos.legend.pure.m4.serialization.binary;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BinaryRepositorySerializer
{
    private final Writer writer;

    public BinaryRepositorySerializer(Writer writer)
    {
        this.writer = writer;
    }

    public BinaryRepositorySerializer(OutputStream stream)
    {
        this(BinaryWriters.newBinaryWriter(stream));
    }

    // Serialize

    public void serialize(ModelRepository repository)
    {
        // Prepare for serialization
        MutableIntObjectMap<byte[]> serializedNodesById = IntObjectMaps.mutable.empty();
        MutableSet<CoreInstance> doneSet = Sets.mutable.empty();
        MutableIntObjectMap<ListIterable<String>> keys = IntObjectMaps.mutable.empty();
        MutableObjectIntMap<String> idsByFile = ObjectIntMaps.mutable.empty();

        MutableStack<CoreInstance> stack = Stacks.mutable.empty();
        for (CoreInstance instance : repository.getTopLevels())
        {
            stack.push(instance);
            serializeNode(serializedNodesById, keys, idsByFile, stack, doneSet);
        }

        // Write id counters
        this.writer.writeInt(repository.getIdCounter());
        this.writer.writeInt(repository.getAnonymousIdCounter());

        // Write files by id
        String[] fileNames = new String[idsByFile.size()];
        idsByFile.forEachKeyValue((fileName, fileId) -> fileNames[fileId] = fileName);
        this.writer.writeStringArray(fileNames);

        // Write top level nodes
        RichIterable<CoreInstance> topLevels = repository.getTopLevels();
        int[] topLevelIds = new int[topLevels.size()];
        int i = 0;
        for (CoreInstance topLevel : topLevels)
        {
            topLevelIds[i++] = topLevel.getSyntheticId();
        }
        Arrays.sort(topLevelIds);
        this.writer.writeIntArray(topLevelIds);

        // Write all nodes
        this.writer.writeInt(serializedNodesById.size());
        serializedNodesById.forEachKeyValue((nodeId, nodeBytes) ->
        {
            // Bytes
            this.writer.writeBytes(nodeBytes);

            // Add possible realKeyPath
            ListIterable<String> realKeys = keys.get(nodeId);
            if (realKeys == null)
            {
                this.writer.writeBoolean(false);
            }
            else
            {
                this.writer.writeBoolean(true);
                this.writer.writeInt(realKeys.size());
                realKeys.forEach(this.writer::writeString);
            }
        });
    }

    private static void serializeSourceInformation(Writer writer, SourceInformation sourceInformation, MutableObjectIntMap<String> idsByFile)
    {
        if (sourceInformation == null)
        {
            writer.writeInt(-1);
        }
        else
        {
            int fileId;
            String id = sourceInformation.getSourceId();
            if (idsByFile.containsKey(id))
            {
                fileId = idsByFile.get(id);
            }
            else
            {
                fileId = idsByFile.size();
                idsByFile.put(id, fileId);
            }
            writer.writeInt(fileId);
            writer.writeInt(sourceInformation.getStartLine());
            writer.writeInt(sourceInformation.getStartColumn());
            writer.writeInt(sourceInformation.getLine());
            writer.writeInt(sourceInformation.getColumn());
            writer.writeInt(sourceInformation.getEndLine());
            writer.writeInt(sourceInformation.getEndColumn());
        }
    }

    private static void push(CoreInstance node, MutableStack<CoreInstance> stack, MutableSet<CoreInstance> doneSet)
    {
        if (!doneSet.contains(node))
        {
            stack.push(node);
        }
    }

    private static void serializeNode(MutableIntObjectMap<byte[]> nodeSerializations, MutableIntObjectMap<ListIterable<String>> keys, MutableObjectIntMap<String> idsByFile, MutableStack<CoreInstance> stack, MutableSet<CoreInstance> doneSet)
    {
        ByteArrayOutputStream nodeBytes = new ByteArrayOutputStream();
        try (Writer writer = BinaryWriters.newBinaryWriter(nodeBytes))
        {
            while (stack.notEmpty())
            {
                CoreInstance node = stack.pop();
                if (doneSet.add(node))
                {
                    nodeBytes.reset();
                    push(node.getClassifier(), stack, doneSet);

                    int id = node.getSyntheticId();
                    writer.writeInt(id);
                    writer.writeInt(node.getClassifier().getSyntheticId());
                    writer.writeString(node.getName());
                    writer.writeInt(node.getCompileStates().toBitSet());
                    serializeSourceInformation(writer, node.getSourceInformation(), idsByFile);

                    writer.writeInt(node.getKeys().size());
                    node.getKeys().forEach(key ->
                    {
                        CoreInstance keyCoreInstance = node.getKeyByName(key);
                        keys.put(keyCoreInstance.getSyntheticId(), node.getRealKeyByName(key));
                        push(keyCoreInstance, stack, doneSet);
                        writer.writeInt(keyCoreInstance.getSyntheticId());
                        ListIterable<? extends CoreInstance> values = node.getValueForMetaPropertyToMany(key);
                        writer.writeInt(values.size());
                        values.forEach(value ->
                        {
                            push(value, stack, doneSet);
                            writer.writeInt(value.getSyntheticId());
                        });
                    });
                    if (nodeSerializations.containsKey(id))
                    {
                        throw new RuntimeException("ERROR");
                    }
                    nodeSerializations.put(id, nodeBytes.toByteArray());
                }
            }
        }
    }

    // Build

    public static IntObjectMap<CoreInstance> build(byte[] data, ModelRepository repository)
    {
        return build(data, repository, null);
    }

    public static IntObjectMap<CoreInstance> build(byte[] data, ModelRepository repository, MessageCallBack message)
    {
        return build(data, repository, message, null);
    }

    public static IntObjectMap<CoreInstance> build(byte[] data, ModelRepository repository, MessageCallBack message, IntObjectMap<String> classifierIdToPath)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(data))
        {
            return build(reader, repository, message, classifierIdToPath);
        }
    }

    public static IntObjectMap<CoreInstance> build(InputStream stream, ModelRepository repository, MessageCallBack message)
    {
        return build(stream, repository, message, null);
    }

    public static IntObjectMap<CoreInstance> build(InputStream stream, ModelRepository repository, MessageCallBack message, IntObjectMap<String> classifierIdToPath)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream))
        {
            return build(reader, repository, message, classifierIdToPath);
        }
    }

    public static IntObjectMap<CoreInstance> build(Reader reader, ModelRepository repository)
    {
        return build(reader, repository, null, null);
    }

    public static IntObjectMap<CoreInstance> build(Reader reader, ModelRepository repository, MessageCallBack message)
    {
        return build(reader, repository, message, null);
    }

    public static IntObjectMap<CoreInstance> build(Reader reader, ModelRepository repository, MessageCallBack message, IntObjectMap<String> classifierIdToPath)
    {
        // Read id counters
        int idCounter = reader.readInt();
        int anonymousIdCounter = reader.readInt();
        repository.setCounters(idCounter, anonymousIdCounter);

        // Read source files by id
        String[] filesById = reader.readStringArray();

        // Read top level ids
        MutableIntSet topLevelIds = IntSets.mutable.empty();
        int topLevelCount = reader.readInt();
        if (message != null)
        {
            message.message("Reading '" + topLevelCount + "' top level ids");
        }
        for (int i = 0; i < topLevelCount; i++)
        {
            topLevelIds.add(reader.readInt());
        }

        // Build intermediate nodes
        int nodeCount = reader.readInt();
        if (message != null)
        {
            message.message("Loading from cache - Building '" + nodeCount + "' intermediate nodes");
        }
        MutableIntObjectMap<IntermediateNode> nodes = IntObjectMaps.mutable.ofInitialCapacity(nodeCount);
        for (int i = 0; i < nodeCount; i++)
        {
            IntermediateNode intermediateNode = readNode(reader, filesById);
            nodes.put(intermediateNode.getId(), intermediateNode);
        }

        // First pass
        if (message != null)
        {
            message.message("Loading from cache - First pass node instantiation (" + nodes.size() + " nodes)");
        }
        MutableIntObjectMap<CoreInstance> map = IntObjectMaps.mutable.ofInitialCapacity(nodeCount + topLevelCount);
        topLevelIds.forEach(id ->
        {
            IntermediateNode node = nodes.get(id);
            CoreInstance instance = createCoreInstance(repository, id, node, classifierIdToPath);
            repository.addTopLevel(instance);
            map.put(id, instance);
        });
        nodes.forEach(node ->
        {
            int id = node.getId();
            if (!topLevelIds.contains(id))
            {
                CoreInstance coreInstance;
                int classifierId = node.getClassifierId();
                if (topLevelIds.contains(classifierId))
                {
                    switch (nodes.get(classifierId).getName())
                    {
                        case ModelRepository.BOOLEAN_TYPE_NAME:
                        {
                            coreInstance = repository.newBooleanCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.DATE_TYPE_NAME:
                        {
                            coreInstance = repository.newDateCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.STRICT_DATE_TYPE_NAME:
                        {
                            coreInstance = repository.newStrictDateCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.DATETIME_TYPE_NAME:
                        {
                            coreInstance = repository.newDateTimeCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.FLOAT_TYPE_NAME:
                        {
                            coreInstance = repository.newFloatCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.DECIMAL_TYPE_NAME:
                        {
                            coreInstance = repository.newDecimalCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.INTEGER_TYPE_NAME:
                        {
                            coreInstance = repository.newIntegerCoreInstance(node.getName(), id);
                            break;
                        }
                        case ModelRepository.STRING_TYPE_NAME:
                        {
                            coreInstance = repository.newStringCoreInstance_cached(node.getName(), id);
                            break;
                        }
                        default:
                        {
                            coreInstance = createCoreInstance(repository, id, node, classifierIdToPath);
                        }
                    }
                }
                else
                {
                    coreInstance = createCoreInstance(repository, id, node, classifierIdToPath);
                }
                map.put(id, coreInstance);
            }
        });

        // Second pass
        if (message != null)
        {
            message.message("Loading from cache<BR>Second pass node instantiation<BR>(" + nodes.size() + " nodes)");
        }
        nodes.forEach(node ->
        {
            CoreInstance nodeInstance = map.get(node.getId());
            if (nodeInstance.getClassifier() == null)
            {
                nodeInstance.setClassifier(map.get(node.getClassifierId()));
            }
            node.getKeyValues().forEachKeyValue((realKeyId, valueIds) ->
            {
                ListIterable<String> realKey = nodes.get(realKeyId).getRealKey();
                ListIterable<CoreInstance> values = valueIds.collect(map::get);
                nodeInstance.setKeyValues(realKey, values);
            });
        });

        return map;
    }

    private static CoreInstance createCoreInstance(ModelRepository repository, int id, IntermediateNode node, IntObjectMap<String> classifierIdToPath)
    {
        if (classifierIdToPath == null)
        {
            return repository.newCoreInstanceMultiPass(id, node.getName(), node.getClassifierId(), node.getSourceInformation(), node.getCompileState());
        }
        else
        {
            String classifierPath = classifierIdToPath.get(node.getClassifierId());
            return repository.newCoreInstanceMultiPass(node.getName(), classifierPath, null, node.getSourceInformation(), node.getCompileState());
        }
    }

    private static IntermediateNode readNode(Reader reader, String[] fileById)
    {
        int id = reader.readInt();
        int classifierId = reader.readInt();
        String name = reader.readString();
        int compileState = reader.readInt();
        int potentialSourceInfo = reader.readInt();
        SourceInformation sourceInformation = null;
        if (potentialSourceInfo != -1)
        {
            int startLine = reader.readInt();
            int startColumn = reader.readInt();
            int line = reader.readInt();
            int column = reader.readInt();
            int endLine = reader.readInt();
            int endColumn = reader.readInt();
            sourceInformation = new SourceInformation(fileById[potentialSourceInfo], startLine, startColumn, line, column, endLine, endColumn);
        }
        IntermediateNode node = new IntermediateNode(id, classifierId, name, compileState, sourceInformation);
        int propertiesCount = reader.readInt();
        for (int i = 0; i < propertiesCount; i++)
        {
            int propertyId = reader.readInt();
            int[] valueIds = reader.readIntArray();
            node.put(propertyId, IntLists.mutable.with(valueIds));
        }

        boolean hasRealKey = reader.readBoolean();
        if (hasRealKey)
        {
            int realKeySize = reader.readInt();
            MutableList<String> realKey = Lists.mutable.withInitialCapacity(realKeySize);
            for (int i = 0; i < realKeySize; i++)
            {
                realKey.add(reader.readString());
            }
            node.setRealKey(realKey);
        }
        else
        {
            node.setRealKey(Lists.immutable.empty());
        }

        return node;
    }
}
