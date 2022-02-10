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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.coreinstance.factory.CompositeCoreInstanceFactory;
import org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.AbstractReference;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceDeserializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializerLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.Reference;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ReferenceFactory;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.SimpleReferenceFactory;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.UnresolvableReferenceException;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;

public class BinaryModelSourceDeserializer
{
    private final ExternalReferenceSerializerLibrary serializerLibrary;
    private final ReferenceFactory referenceFactory;
    private ListIterable<String> stringIndex;
    private ListMultimap<String, String> instancesByParser;
    private ListIterable<String> otherInstances;
    private ListIterable<String> externalReferences;
    private Source source;
    private ListIterable<Reference> otherExternalReferences;
    private ListIterable<ListIterable<String>> propertyRealKeys;
    private ListIterable<InternalNode> internalNodes;

    private BinaryModelSourceDeserializer(ExternalReferenceSerializerLibrary serializerLibrary, ReferenceFactory referenceFactory)
    {
        this.serializerLibrary = serializerLibrary;
        this.referenceFactory = (referenceFactory == null) ? new SimpleReferenceFactory() : referenceFactory;
    }

    private void deserialize(Reader reader, boolean readInstancesByParser, boolean readOtherInstances, boolean readExternalReferences)
    {
        readStringIndex(reader);
        if (readInstancesByParser)
        {
            readInstancesByParser(reader);
        }
        else
        {
            skipInstancesByParser(reader);
        }
        if (readOtherInstances)
        {
            readOtherInstances(reader);
        }
        else
        {
            skipOtherInstance(reader);
        }
        if (readExternalReferences)
        {
            readExternalReferences(reader);
        }
        else
        {
            skipExternalReferences(reader);
        }
        readSourceDefinition(reader);
        readOtherExternalReferences(reader);
        readPropertyRealKeyIndex(reader);
        readInstances(reader);
    }

    private void readMainIndexes(Reader reader)
    {
        readStringIndex(reader);
        readInstancesByParser(reader);
        readOtherInstances(reader);
        readExternalReferences(reader);
        readSourceDefinition(reader);
    }

    private void readStringIndex(Reader reader)
    {
        String[] strings = reader.readStringArray();
        this.stringIndex = ArrayAdapter.adapt(strings).asUnmodifiable();
    }

    private void skipInstancesByParser(Reader reader)
    {
        int parserCount = reader.readInt();
        for (int i = 0; i < parserCount; i++)
        {
            // skip the parser name id
            reader.readInt();
            // skip the parser instance ids
            reader.skipIntArray();
        }
    }

    private void readInstancesByParser(Reader reader)
    {
        int parserCount = reader.readInt();
        MutableListMultimap<String, String> result = Multimaps.mutable.list.empty();
        for (int i = 0; i < parserCount; i++)
        {
            String parserName = readStringById(reader);
            String[] parserInstances = readStringsById(reader);
            result.putAll(parserName, ArrayAdapter.adapt(parserInstances));
        }
        this.instancesByParser = result;
    }

    private void skipOtherInstance(Reader reader)
    {
        reader.skipIntArray();
    }

    private void readOtherInstances(Reader reader)
    {
        String[] instances = readStringsById(reader);
        this.otherInstances = ArrayAdapter.adapt(instances).asUnmodifiable();
    }

    private void skipExternalReferences(Reader reader)
    {
        reader.skipIntArray();
    }

    private void readExternalReferences(Reader reader)
    {
        String[] references = readStringsById(reader);
        this.externalReferences = ArrayAdapter.adapt(references).asUnmodifiable();
    }

    private void readSourceDefinition(Reader reader)
    {
        String sourceId = reader.readString();
        boolean isImmutable = reader.readBoolean();
        boolean isInMemory = reader.readBoolean();
        String content = reader.readString();
        this.source = new Source(sourceId, isImmutable, isInMemory, content);
    }

    private void readOtherExternalReferences(Reader reader)
    {
        // First, read the number of external references
        int count = reader.readInt();

        // Second, read all the byte arrays
        byte[][] externalReferenceBytes = new byte[count][];
        for (int i = 0; i < count; i++)
        {
            byte[] referencesBytes = reader.readByteArray();
            externalReferenceBytes[i] = referencesBytes;
        }

        // Finally, deserialize the external references
        Reference[] externalReferences = new Reference[count];
        for (int i = 0; i < count; i++)
        {
            readOtherExternalReference(i, externalReferences, externalReferenceBytes);
        }

        this.otherExternalReferences = ArrayAdapter.adapt(externalReferences).asUnmodifiable();
    }

    private Reference readOtherExternalReference(int i, Reference[] externalReferences, byte[][] externalReferenceBytes)
    {
        Reference externalReference = externalReferences[i];
        if (externalReference == null)
        {
            Reader reader = BinaryReaders.newBinaryReader(externalReferenceBytes[i]);
            String serializerType = readStringById(reader);
            ExternalReferenceSerializer serializer = this.serializerLibrary.getSerializer(serializerType);
            if (serializer == null)
            {
                throw new RuntimeException("Cannot find serializer for type: " + serializerType);
            }
            externalReference = serializer.deserialize(new BinaryExternalReferenceDeserializationHelper(reader, externalReferences, externalReferenceBytes));
            externalReferences[i] = externalReference;
        }
        return externalReference;
    }

    private void readPropertyRealKeyIndex(Reader reader)
    {
        int count = reader.readInt();
        MutableList<ListIterable<String>> results = FastList.newList(count);
        for (int i = 0; i < count; i++)
        {
            String[] strings = readStringsById(reader);
            results.add(ArrayAdapter.adapt(strings).asUnmodifiable());
        }
        this.propertyRealKeys = results.asUnmodifiable();
    }

    private void readInstances(Reader reader)
    {
        // First, read the number of instances
        int count = reader.readInt();

        // Second, read the nodes in an intermediate form
        MutableList<InternalNode> nodes = FastList.newList(count);
        this.internalNodes = nodes.asUnmodifiable();
        for (int i = 0; i < count; i++)
        {
            byte[] nodeBytes = reader.readByteArray();
            InternalNode node = readInstance(BinaryReaders.newBinaryReader(nodeBytes));
            nodes.add(node);
        }
    }

    private InternalNode readInstance(Reader reader)
    {
        String name = null;
        String pkg = null;
        byte type = reader.readByte();
        switch (type)
        {
            case BinaryModelSerializationTypes.TOP_LEVEL_INSTANCE:
            {
                name = readStringById(reader);
                break;
            }
            case BinaryModelSerializationTypes.PACKAGED_INSTANCE:
            {
                name = readStringById(reader);
                pkg = readStringById(reader);
                break;
            }
            case BinaryModelSerializationTypes.ENUM_INSTANCE:
            {
                name = readStringById(reader);
                break;
            }
            case BinaryModelSerializationTypes.OTHER_INSTANCE:
            {
                name = readStringById(reader);
                break;
            }
            case BinaryModelSerializationTypes.ANONYMOUS_INSTANCE:
            {
                // Nothing to read
                break;
            }
            default:
            {
                throw new RuntimeException("Unhandled instance type: " + type);
            }
        }

        // Read the classifier
        String classifierPath = readStringById(reader);
        Reference classifierReference = readReference(reader);

        // Read the source information
        SourceInformation sourceInfo = readSourceInformation(reader);

        // Read the compile states
        int compileStateBitSet = reader.readInt();

        // Create node
        InternalNode node = new InternalNode(type, name, (pkg == null) ? null : this.referenceFactory.packageReference(pkg), classifierPath, classifierReference, sourceInfo, compileStateBitSet);

        // Read property values
        int propertyCount = reader.readInt();
        for (int i = 0; i < propertyCount; i++)
        {
            int realKeyId = reader.readInt();
            ListIterable<String> realKey = this.propertyRealKeys.get(realKeyId);

            int valueCount = reader.readInt();
            MutableList<Reference> values = FastList.newList(valueCount);
            for (int j = 0; j < valueCount; j++)
            {
                Reference value = readReference(reader);
                values.add(value);
            }
            node.addProperty(realKey, values.asUnmodifiable());
        }

        return node;
    }

    private SourceInformation readSourceInformation(Reader reader)
    {
        boolean hasSourceInfo = reader.readBoolean();
        if (!hasSourceInfo)
        {
            return null;
        }

        int startLine = reader.readInt();
        int startColumn = reader.readInt();
        int line = reader.readInt();
        int column = reader.readInt();
        int endLine = reader.readInt();
        int endColumn = reader.readInt();
        return new SourceInformation(this.source.getId(), startLine, startColumn, line, column, endLine, endColumn);
    }

    private Reference readReference(Reader reader)
    {
        byte type = reader.readByte();
        switch (type)
        {
            case BinaryModelSerializationTypes.BOOLEAN:
            {
                boolean value = reader.readBoolean();
                return this.referenceFactory.booleanReference(value);
            }
            case BinaryModelSerializationTypes.DATE:
            {
                String name = readStringById(reader);
                return this.referenceFactory.dateReference(name);
            }
            case BinaryModelSerializationTypes.STRICT_DATE:
            {
                String name = readStringById(reader);
                return this.referenceFactory.strictDateReference(name);
            }
            case BinaryModelSerializationTypes.DATE_TIME:
            {
                String name = readStringById(reader);
                return this.referenceFactory.dateTimeReference(name);
            }
            case BinaryModelSerializationTypes.LATEST_DATE:
            {
                return this.referenceFactory.latestDateReference();
            }
            case BinaryModelSerializationTypes.FLOAT:
            {
                String name = readStringById(reader);
                return this.referenceFactory.floatReference(name);
            }
            case BinaryModelSerializationTypes.DECIMAL:
            {
                String name = readStringById(reader);
                return this.referenceFactory.decimalReference(name);
            }
            case BinaryModelSerializationTypes.INTEGER_INT:
            {
                int value = reader.readInt();
                return this.referenceFactory.integerReference(value);
            }
            case BinaryModelSerializationTypes.INTEGER_LONG:
            {
                long value = reader.readLong();
                return this.referenceFactory.integerReference(value);
            }
            case BinaryModelSerializationTypes.INTEGER_BIG:
            {
                String name = readStringById(reader);
                return this.referenceFactory.integerReference(name);
            }
            case BinaryModelSerializationTypes.STRING:
            {
                String value = readStringById(reader);
                return this.referenceFactory.stringReference(value);
            }
            case BinaryModelSerializationTypes.PACKAGE_REFERENCE:
            {
                String path = readStringById(reader);
                return this.referenceFactory.packageReference(path);
            }
            case BinaryModelSerializationTypes.INTERNAL_REFERENCE:
            {
                int id = reader.readInt();
                return new InternalReference(id, this.internalNodes);
            }
            case BinaryModelSerializationTypes.EXTERNAL_PACKAGEABLE_ELEMENT_REFERENCE:
            {
                String path = readStringById(reader);
                return this.referenceFactory.packagedElementReference(path);
            }
            case BinaryModelSerializationTypes.EXTERNAL_OTHER_REFERENCE:
            {
                int id = reader.readInt();
                return this.otherExternalReferences.get(id);
            }
            default:
            {
                throw new RuntimeException("Unknown serialization type: " + type);
            }
        }
    }

    private String readStringById(Reader reader)
    {
        int stringId = reader.readInt();
        return this.stringIndex.get(stringId);
    }

    private String[] readStringsById(Reader reader)
    {
        int count = reader.readInt();
        String[] strings = new String[count];
        for (int i = 0; i < count; i++)
        {
            strings[i] = readStringById(reader);
        }
        return strings;
    }

    public static SourceDeserializationResult deserialize(Reader reader, ExternalReferenceSerializerLibrary serializerLibrary)
    {
        return deserialize(reader, serializerLibrary, null);
    }

    public static SourceDeserializationResult deserialize(Reader reader, ExternalReferenceSerializerLibrary serializerLibrary, ReferenceFactory referenceFactory)
    {
        return deserialize(reader, serializerLibrary, referenceFactory, true, true, true);
    }

    public static SourceDeserializationResult deserialize(Reader reader, ExternalReferenceSerializerLibrary serializerLibrary, boolean readInstancesByParser, boolean readOtherInstances, boolean readExternalReferences)
    {
        return deserialize(reader, serializerLibrary, null, readInstancesByParser, readOtherInstances, readExternalReferences);
    }

    public static SourceDeserializationResult deserialize(Reader reader, ExternalReferenceSerializerLibrary serializerLibrary, ReferenceFactory referenceFactory, boolean readInstancesByParser, boolean readOtherInstances, boolean readExternalReferences)
    {
        BinaryModelSourceDeserializer deserializer = new BinaryModelSourceDeserializer(serializerLibrary, referenceFactory);
        deserializer.deserialize(reader, readInstancesByParser, readOtherInstances, readExternalReferences);
        return new SourceDeserializationResult(deserializer.source, deserializer.instancesByParser, deserializer.otherInstances, deserializer.externalReferences, deserializer.internalNodes);
    }

    public static SourceDeserializationResult readIndexes(Reader reader)
    {
        BinaryModelSourceDeserializer deserializer = new BinaryModelSourceDeserializer(null, new SimpleReferenceFactory());
        deserializer.readMainIndexes(reader);
        return new SourceDeserializationResult(deserializer.source, deserializer.instancesByParser, deserializer.otherInstances, deserializer.externalReferences, null);
    }

    private class BinaryExternalReferenceDeserializationHelper implements ExternalReferenceDeserializationHelper
    {
        private final Reader reader;
        private final Reference[] externalReferences;
        private final byte[][] externalReferenceBytes;

        private BinaryExternalReferenceDeserializationHelper(Reader reader, Reference[] externalReferences, byte[][] externalReferenceBytes)
        {
            this.reader = reader;
            this.externalReferences = externalReferences;
            this.externalReferenceBytes = externalReferenceBytes;
        }

        @Override
        public byte readByte()
        {
            return this.reader.readByte();
        }

        @Override
        public int readInt()
        {
            return this.reader.readInt();
        }

        @Override
        public String readString()
        {
            return readStringById(this.reader);
        }

        @Override
        public Reference readElementReference()
        {
            byte referenceType = this.reader.readByte();
            switch (referenceType)
            {
                case BinaryModelSerializationTypes.EXTERNAL_PACKAGEABLE_ELEMENT_REFERENCE:
                case BinaryModelSerializationTypes.PACKAGE_REFERENCE:
                {
                    String path = readString();
                    return BinaryModelSourceDeserializer.this.referenceFactory.packagedElementReference(path);
                }
                case BinaryModelSerializationTypes.EXTERNAL_OTHER_REFERENCE:
                {
                    int referenceId = this.reader.readInt();
                    return readOtherExternalReference(referenceId, this.externalReferences, this.externalReferenceBytes);
                }
                case BinaryModelSerializationTypes.INTERNAL_REFERENCE:
                {
                    int id = this.reader.readInt();
                    return new InternalReference(id, BinaryModelSourceDeserializer.this.internalNodes);
                }
                default:
                {
                    throw new RuntimeException("Unhandled reference type: " + referenceType);
                }
            }
        }
    }

    private static class InternalNode implements DeserializationNode
    {
        private final byte type;
        private final String name;
        private final Reference pkg;
        private final String classifierPath;
        private final Reference classifierReference;
        private final SourceInformation sourceInfo;
        private final int compileStateBitSet;
        private final MutableMap<ListIterable<String>, ListIterable<Reference>> properties = Maps.mutable.empty();
        private CoreInstance instance = null;

        private InternalNode(byte type, String name, Reference pkg, String classifierPath, Reference classifierReference, SourceInformation sourceInfo, int compileStateBitSet)
        {
            this.type = type;
            this.name = name;
            this.pkg = pkg;
            this.classifierPath = classifierPath;
            this.classifierReference = classifierReference;
            this.sourceInfo = sourceInfo;
            this.compileStateBitSet = compileStateBitSet;
        }

        void addProperty(ListIterable<String> realKey, ListIterable<Reference> values)
        {
            this.properties.put(realKey, values);
        }

        @Override
        public boolean isTopLevel()
        {
            return this.type == BinaryModelSerializationTypes.TOP_LEVEL_INSTANCE;
        }

        @Override
        public boolean isPackaged()
        {
            return this.type == BinaryModelSerializationTypes.PACKAGED_INSTANCE;
        }

        @Override
        public boolean isAnonymous()
        {
            return this.type == BinaryModelSerializationTypes.ANONYMOUS_INSTANCE;
        }

        @Override
        public void initializeInstance(ModelRepository repository, ProcessorSupport processorSupport)
        {
            switch (this.type)
            {
                case BinaryModelSerializationTypes.TOP_LEVEL_INSTANCE:
                {
                    this.instance = createTopLevel(repository);
                    break;
                }
                case BinaryModelSerializationTypes.PACKAGED_INSTANCE:
                {
                    this.instance = createPackaged(repository, processorSupport);
                    break;
                }
                case BinaryModelSerializationTypes.ENUM_INSTANCE:
                {
                    this.instance = createInRepository(repository, CompositeCoreInstanceFactory.IS_ENUM_TYPE_INFO);
                    break;
                }
                default:
                {
                    this.instance = createInRepository(repository, null);
                }
            }
        }

        @Override
        public ReferenceResolutionResult resolveReferences(ModelRepository repository, ProcessorSupport processorSupport)
        {
            int newlyResolved = 0;
            int unresolved = 0;

            // Resolve classifier
            if (this.instance.getClassifier() == null)
            {
                try
                {
                    if (this.classifierReference.resolve(repository, processorSupport))
                    {
                        this.instance.setClassifier(this.classifierReference.getResolvedInstance());
                        newlyResolved++;
                    }
                    else
                    {
                        unresolved++;
                    }
                }
                catch (UnresolvableReferenceException e)
                {
                    StringBuilder message = new StringBuilder("Error resolving reference to classifier ");
                    message.append(this.classifierPath);
                    message.append(" for ");
                    writeInstanceErrorInfo(message);
                    throw new RuntimeException(message.toString(), e);
                }
            }

            // Resolve property values
            if (this.properties.notEmpty())
            {
                for (Pair<ListIterable<String>, ListIterable<Reference>> propertyReferencesPair : this.properties.keyValuesView())
                {
                    int index = 0;
                    for (Reference reference : propertyReferencesPair.getTwo())
                    {
                        if (!reference.isResolved())
                        {
                            try
                            {
                                if (reference.resolve(repository, processorSupport))
                                {
                                    newlyResolved++;
                                }
                                else
                                {
                                    unresolved++;
                                }
                            }
                            catch (UnresolvableReferenceException e)
                            {
                                StringBuilder message = new StringBuilder("Error resolving reference to value ");
                                message.append(index);
                                message.append(" for property '");
                                message.append(propertyReferencesPair.getOne().getLast());
                                message.append("' for ");
                                writeInstanceErrorInfo(message);
                                throw new RuntimeException(message.toString(), e);
                            }
                        }
                        index++;
                    }
                }
            }
            return new ReferenceResolutionResult(newlyResolved, unresolved);
        }

        @Override
        public void populateResolvedProperties()
        {
            if (this.properties.isEmpty())
            {
                return;
            }
            for (ListIterable<String> propertyKey : this.properties.keysView().toList())
            {
                ListIterable<Reference> references = this.properties.get(propertyKey);
                if (references.allSatisfy(Reference.IS_RESOLVED))
                {
                    this.properties.remove(propertyKey);
                    ListIterable<? extends CoreInstance> existingValues = this.instance.getValueForMetaPropertyToMany(propertyKey.getLast());
                    ListIterable<CoreInstance> serializedValues = references.collect(Reference.GET_RESOLVED_INSTANCE);
                    if (existingValues.isEmpty())
                    {
                        try
                        {
                            this.instance.setKeyValues(propertyKey, serializedValues);
                        }
                        catch (Exception e)
                        {
                            StringBuilder message = new StringBuilder("Error populating property '");
                            message.append(propertyKey.getLast());
                            message.append("' for ");
                            writeInstanceErrorInfo(message);
                            message.append(" with");
                            int count = serializedValues.size();
                            if (count == 1)
                            {
                                message.append(": ");
                                message.append(serializedValues.get(0));
                            }
                            else
                            {
                                message.append(" ");
                                message.append(count);
                                message.append(" values: ");
                                if (count > 10)
                                {
                                    LazyIterate.take(serializedValues, 10).appendString(message, "[", ", ", ", ...]");
                                }
                                else
                                {
                                    serializedValues.appendString(message, "[", ", ", "]");
                                }
                            }
                            throw new RuntimeException(message.toString(), e);
                        }
                    }
                    else
                    {
                        MutableList<CoreInstance> allValues = FastList.newList(serializedValues.size() + existingValues.size());
                        allValues.addAllIterable(serializedValues);
                        MutableMap<String, CoreInstance> valuesByName = serializedValues.toMap(CoreInstance.GET_NAME, Functions.<CoreInstance>getPassThru());
                        for (CoreInstance value : existingValues)
                        {
                            String name = value.getName();
                            CoreInstance other = valuesByName.put(name, value);
                            if (other == null)
                            {
                                allValues.add(value);
                            }
                            else if (other != value)
                            {
                                throw new RuntimeException("Error populating property '" + propertyKey.getLast() + "' for " + this.instance + ": multiple values named '" + name + "'");
                            }
                        }
                        try
                        {
                            this.instance.setKeyValues(propertyKey, allValues);
                        }
                        catch (Exception e)
                        {
                            StringBuilder message = new StringBuilder("Error populating property '");
                            message.append(propertyKey.getLast());
                            message.append("' for ");
                            writeInstanceErrorInfo(message);
                            message.append(" with additional");
                            int count = serializedValues.size();
                            if (count == 1)
                            {
                                message.append(" value: ");
                                message.append(serializedValues.get(0));
                            }
                            else
                            {
                                message.append(" ");
                                message.append(count);
                                message.append(" values: ");
                                if (count > 10)
                                {
                                    LazyIterate.take(serializedValues, 10).appendString(message, "[", ", ", ", ...]");
                                }
                                else
                                {
                                    serializedValues.appendString(message, "[", ", ", "]");
                                }
                            }
                            existingValues.appendString(message, "; existing values: [", ", ", "]");
                            throw new RuntimeException(message.toString(), e);
                        }
                    }
                }
            }
        }

        @Override
        public void collectUnresolvedReferences(MutableCollection<Reference> target)
        {
            if (!this.classifierReference.isResolved())
            {
                target.add(this.classifierReference);
            }
            for (ListIterable<Reference> references : this.properties.valuesView())
            {
                references.reject(Reference.IS_RESOLVED, target);
            }
        }

        @Override
        public CoreInstance getInstance()
        {
            return this.instance;
        }

        private CoreInstance createTopLevel(ModelRepository repository)
        {
            CoreInstance topLevel = repository.getTopLevel(this.name);
            if (topLevel == null)
            {
                topLevel = createInRepository(repository, null);
                repository.addTopLevel(topLevel);
            }
            else
            {
                Class<?> expectedClass = M3CoreInstanceFactoryRegistry.getClassForPath(this.classifierPath);
                if ((expectedClass != null) && !expectedClass.isInstance(topLevel))
                {
                    throw new RuntimeException("Top level instance " + this.name + " is not of the expected class; expected: " + expectedClass + ", found: " + topLevel.getClass());
                }
                topLevel.setSourceInformation(this.sourceInfo);
                topLevel.setCompileStatesFrom(CompileStateSet.fromBitSet(this.compileStateBitSet));
            }
            return topLevel;
        }

        private CoreInstance createPackaged(ModelRepository repository, ProcessorSupport processorSupport)
        {
            try
            {
                boolean packageResolved = this.pkg.resolve(repository, processorSupport);
                if (!packageResolved)
                {
                    StringBuilder message = new StringBuilder("Could not resolve package reference for ");
                    writeInstanceErrorInfo(message);
                    throw new RuntimeException(message.toString());
                }
            }
            catch (UnresolvableReferenceException e)
            {
                StringBuilder message = new StringBuilder("Could not resolve package reference for ");
                writeInstanceErrorInfo(message);
                throw new RuntimeException(message.toString(), e);
            }
            CoreInstance parent = this.pkg.getResolvedInstance();
            synchronized (parent)
            {
                CoreInstance child = _Package.findInPackage(parent, this.name);
                if (child == null)
                {
                    child = createInRepository(repository, null);
                    child.addKeyValue(M3PropertyPaths._package, parent);
                    child.addKeyValue(M3PropertyPaths.name, repository.newStringCoreInstance_cached(this.name));
                    parent.addKeyValue(M3PropertyPaths.children, child);
                }
                else
                {
                    Class<?> expectedClass = M3CoreInstanceFactoryRegistry.getClassForPath(this.classifierPath);
                    if ((expectedClass != null) && !expectedClass.isInstance(child))
                    {
                        throw new RuntimeException("Instance " + this.pkg + "::" + this.name + " is not of the expected class; expected: " + expectedClass + ", found: " + child.getClass());
                    }
                    child.setSourceInformation(this.sourceInfo);
                    child.setCompileStatesFrom(CompileStateSet.fromBitSet(this.compileStateBitSet));
                    // TODO should we validate or overwrite the 'package' and 'name' properties?
                }
                return child;
            }
        }

        private CoreInstance createInRepository(ModelRepository repository, String typeInfo)
        {
            return repository.newCoreInstanceMultiPass(this.name, this.classifierPath, typeInfo, this.sourceInfo, this.compileStateBitSet);
        }

        private void writeInstanceErrorInfo(StringBuilder message)
        {
            if (isAnonymous())
            {
                message.append("anonymous instance");
            }
            else
            {
                message.append("instance '");
                message.append(this.name);
                message.append("'");
            }
            message.append(" (classifier=");
            message.append(this.classifierPath);
            if (this.sourceInfo != null)
            {
                this.sourceInfo.appendMessage(message.append(", source info="));
            }
            message.append(")");
        }
    }

    private static class InternalReference extends AbstractReference
    {
        private final int id;
        private final ListIterable<InternalNode> internalNodes;

        private InternalReference(int id, ListIterable<InternalNode> internalNodes)
        {
            this.id = id;
            this.internalNodes = internalNodes;
        }

        @Override
        public CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            InternalNode node = this.internalNodes.get(this.id);
            if (node == null)
            {
                throw new UnresolvableReferenceException("Could not resolve internal reference with id: " + this.id);
            }
            CoreInstance instance = node.getInstance();
            if (instance == null)
            {
                setFailureMessage("Could not resolve internal reference with id: " + this.id);
            }
            return instance;
        }
    }
}
