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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializerLibrary;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Queue;

public class BinaryModelSourceSerializer
{
    private static final Function2<String, CoreInstance, ListIterable<String>> GET_PROPERTY_REAL_KEY = new Function2<String, CoreInstance, ListIterable<String>>()
    {
        @Override
        public ListIterable<String> value(String propertyName, CoreInstance instance)
        {
            return instance.getRealKeyByName(propertyName);
        }
    };

    private static final Predicate<Object> SHOULD_SERIALIZE_PROPERTY = Predicates.notIn(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS);

    private final Predicate<CoreInstance> isFromThisSource = new Predicate<CoreInstance>()
    {
        @Override
        public boolean accept(CoreInstance instance)
        {
            return isFromThisSource(instance);
        }
    };

    private final IntToObjectFunction<String> getStringById = new IntToObjectFunction<String>()
    {
        @Override
        public String valueOf(int id)
        {
            return BinaryModelSourceSerializer.this.strings.get(id);
        }
    };

    private final Source source;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final ImmutableSet<CoreInstance> repositoryTopLevels;
    private final ExternalReferenceSerializerLibrary serializerLibrary;

    private final CoreInstance booleanType;
    private final CoreInstance dateType;
    private final CoreInstance strictDateType;
    private final CoreInstance dateTimeType;
    private final CoreInstance latestDateType;
    private final CoreInstance floatType;
    private final CoreInstance decimalType;
    private final CoreInstance integerType;
    private final CoreInstance stringType;
    private final CoreInstance packageClass;
    private final CoreInstance packageableElementClass;
    private final CoreInstance importStubClass;
    private final CoreInstance propertyStubClass;
    private final CoreInstance enumStubClass;
    private final CoreInstance enumerationClass;

    private final MutableList<CoreInstance> internalInstances = Lists.mutable.empty();
    private final MutableObjectIntMap<CoreInstance> internalInstanceIds = ObjectIntMaps.mutable.empty();
    private final MutableMap<CoreInstance, byte[]> internalInstanceSerializations = Maps.mutable.empty();

    private final MutableList<String> strings = Lists.mutable.empty();
    private final MutableObjectIntMap<String> stringIds = ObjectIntMaps.mutable.empty();

    private final MutableObjectIntMap<CoreInstance> sourceNewInstancePathIds = ObjectIntMaps.mutable.empty();
    private final MutableIntList sourceOtherInstancePathIds = IntLists.mutable.empty();

    private final MutableObjectIntMap<CoreInstance> externalPackageableElementReferences = ObjectIntMaps.mutable.empty();

    private final MutableList<CoreInstance> externalOtherReferences = Lists.mutable.empty();
    private final MutableObjectIntMap<CoreInstance> externalOtherReferenceIds = ObjectIntMaps.mutable.empty();
    private final MutableMap<CoreInstance, byte[]> externalOtherReferenceSerializations = Maps.mutable.empty();

    private final MutableList<ListIterable<String>> propertyRealKeys = Lists.mutable.empty();
    private final MutableObjectIntMap<ListIterable<String>> propertyRealKeyIds = ObjectIntMaps.mutable.empty();

    private final Queue<CoreInstance> serializationQueue = new ArrayDeque<>();
    private final MutableSet<CoreInstance> serialized = Sets.mutable.empty();
    private final MutableList<CoreInstance> serializedPackagedOrTopLevel = Lists.mutable.empty();

    private BinaryModelSourceSerializer(Source source, ModelRepository modelRepository, ProcessorSupport processorSupport, ParserLibrary parserLibrary)
    {
        this.source = source;
        this.repository = modelRepository;
        this.processorSupport = processorSupport;
        this.repositoryTopLevels = Sets.immutable.withAll(this.repository.getTopLevels());
        this.serializerLibrary = ExternalReferenceSerializerLibrary.newLibrary(parserLibrary);

        this.booleanType = processorSupport.package_getByUserPath(M3Paths.Boolean);
        this.dateType =  processorSupport.package_getByUserPath(M3Paths.Date);
        this.strictDateType =  processorSupport.package_getByUserPath(M3Paths.StrictDate);
        this.dateTimeType =  processorSupport.package_getByUserPath(M3Paths.DateTime);
        this.latestDateType =  processorSupport.package_getByUserPath(M3Paths.LatestDate);
        this.floatType =  processorSupport.package_getByUserPath(M3Paths.Float);
        this.decimalType = processorSupport.package_getByUserPath(M3Paths.Decimal);
        this.integerType =  processorSupport.package_getByUserPath(M3Paths.Integer);
        this.stringType =  processorSupport.package_getByUserPath(M3Paths.String);
        this.packageClass =  processorSupport.package_getByUserPath(M3Paths.Package);
        this.packageableElementClass =  processorSupport.package_getByUserPath(M3Paths.PackageableElement);
        this.importStubClass =  processorSupport.package_getByUserPath(M3Paths.ImportStub);
        this.propertyStubClass =  processorSupport.package_getByUserPath(M3Paths.PropertyStub);
        this.enumStubClass =  processorSupport.package_getByUserPath(M3Paths.EnumStub);
        this.enumerationClass = processorSupport.package_getByUserPath(M3Paths.Enumeration);
    }

    private void serialize(Writer writer)
    {
        if (!this.source.isCompiled())
        {
            throw new IllegalStateException("Cannot serialize " + this.source.getId() + ": source not compiled");
        }

        // Serialize the instances in memory
        serializeInstances();

        // Prepare to write the main index
        prepareMainIndexes();

        // Write string index
        writeStringIndex(writer);

        // Write the main index (top level elements serialized in this binary)
        writeMainIndexes(writer);

        // Write the external PackageableElement reference index
        writeExternalPackageableElementIndex(writer);

        // Write the source id
        writeSourceDefinition(writer);

        // Write the index of external other references
        writeExternalOtherReferenceIndex(writer);

        // Write the index of property real keys
        writePropertyRealKeyIndex(writer);

        // Write the instance serializations
        writeInstanceSerializations(writer);
    }

    private void prepareMainIndexes()
    {
        ListMultimap<Parser, CoreInstance> elements = this.source.getElementsByParser();
        for (Parser parser : this.source.getElementsByParser().keysView().toSortedListBy(Parser.GET_NAME))
        {
            possiblyRegisterString(parser.getName());
            for (CoreInstance instance : elements.get(parser))
            {
                if (!this.serialized.contains(instance))
                {
                    throw new RuntimeException("Failed to serialize " + getElementPath(instance));
                }
                int pathId = getStringReferenceId(getElementPath(instance));
                this.sourceNewInstancePathIds.put(instance, pathId);
            }
        }
        for (CoreInstance instance : this.serializedPackagedOrTopLevel)
        {
            if (!this.sourceNewInstancePathIds.containsKey(instance))
            {
                int pathId = getStringReferenceId(getElementPath(instance));
                this.sourceOtherInstancePathIds.add(pathId);
            }
        }
    }

    private void writeStringIndex(Writer writer)
    {
        writer.writeInt(this.strings.size());
        for (String string : this.strings)
        {
            writer.writeString(string);
        }
    }

    private void writeMainIndexes(Writer writer)
    {
        // Write instances by parser
        ListMultimap<Parser, CoreInstance> instancesByParser = this.source.getElementsByParser();
        ListIterable<Parser> parsersSortedByName = instancesByParser.keysView().toSortedListBy(Parser.GET_NAME);
        writer.writeInt(parsersSortedByName.size());
        for (Parser parser : parsersSortedByName)
        {
            writer.writeInt(getStringReferenceId(parser.getName()));
            ListIterable<CoreInstance> parserInstances = instancesByParser.get(parser);
            writer.writeInt(parserInstances.size());
            for (CoreInstance instance : parserInstances)
            {
                writer.writeInt(this.sourceNewInstancePathIds.get(instance));
            }
        }

        // Write other instances
        int[] ids = this.sourceOtherInstancePathIds.toSortedArray();
        writer.writeIntArray(ids);
    }

    private void writeExternalPackageableElementIndex(Writer writer)
    {
        int[] ids = this.externalPackageableElementReferences.toSortedArray();
        writer.writeIntArray(ids);
    }

    private void writeSourceDefinition(Writer writer)
    {
        writer.writeString(this.source.getId());
        writer.writeBoolean(this.source.isImmutable());
        writer.writeBoolean(this.source.isInMemory());
        writer.writeString(this.source.getContent());
    }

    private void writeExternalOtherReferenceIndex(Writer writer)
    {
        writer.writeInt(this.externalOtherReferences.size());
        for (CoreInstance instance : this.externalOtherReferences)
        {
            writer.writeByteArray(this.externalOtherReferenceSerializations.get(instance));
        }
    }

    private void writePropertyRealKeyIndex(Writer writer)
    {
        writer.writeInt(this.propertyRealKeys.size());
        for (ListIterable<String> propertyRealKey : this.propertyRealKeys)
        {
            writeStringsById(propertyRealKey, writer);
        }
    }

    private void writeInstanceSerializations(Writer writer)
    {
        writer.writeInt(this.internalInstances.size());
        for (CoreInstance instance : this.internalInstances)
        {
            writer.writeByteArray(this.internalInstanceSerializations.get(instance));
        }
    }

    private void serializeInstances()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (Writer writer = BinaryWriters.newBinaryWriter(stream))
        {
            initializeSerializationQueue();
            while (!this.serializationQueue.isEmpty())
            {
                CoreInstance instance = this.serializationQueue.remove();
                if (shouldSerialize(instance) && this.serialized.add(instance))
                {
                    possiblyRegisterInternalInstance(instance);
                    try
                    {
                        serializeInstance(instance, writer);
                    }
                    catch (RuntimeException e)
                    {
                        StringBuilder message = new StringBuilder("Error serializing ");
                        message.append(instance);
                        SourceInformation sourceInformation = instance.getSourceInformation();
                        if (sourceInformation != null)
                        {
                            sourceInformation.appendMessage(message.append(" (source information: ")).append(')');
                        }
                        throw new RuntimeException(message.toString(), e);
                    }
                    this.internalInstanceSerializations.put(instance, stream.toByteArray());
                    stream.reset();
                }
            }
        }
    }

    private boolean shouldSerialize(CoreInstance instance)
    {
        return (instance.getClassifier() != this.packageClass) || isFromThisSource(instance);
    }

    private void initializeSerializationQueue()
    {
        // Add top level elements
        ListMultimap<Parser, CoreInstance> elementsByParser = this.source.getElementsByParser();
        if (elementsByParser != null)
        {
            for (Parser parser : elementsByParser.keysView().toSortedListBy(Parser.GET_NAME))
            {
                Iterate.addAllIterable(elementsByParser.get(parser), this.serializationQueue);
            }
        }

        // Add import groups
        ListIterable<? extends CoreInstance> importGroups = Imports.getImportGroupsForSource(this.source.getId(), this.processorSupport);
        Iterate.addAllIterable(importGroups, this.serializationQueue);
    }

    private void serializeInstance(CoreInstance instance, Writer writer)
    {
        if (isTopLevel(instance))
        {
            writer.writeByte(BinaryModelSerializationTypes.TOP_LEVEL_INSTANCE);
            writeStringById(instance.getName(), writer);
            this.serializedPackagedOrTopLevel.add(instance);
        }
        else if (isPackaged(instance))
        {
            writer.writeByte(BinaryModelSerializationTypes.PACKAGED_INSTANCE);
            writeStringById(instance.getName(), writer);
            CoreInstance pkg = getPropertyValueToOne(instance, M3Properties._package);
            writeStringById(getElementPath(pkg), writer);
            this.serializedPackagedOrTopLevel.add(instance);
        }
        else if (isAnonymousInstance(instance))
        {
            writer.writeByte(BinaryModelSerializationTypes.ANONYMOUS_INSTANCE);
        }
        else if (isEnumeration(instance.getClassifier()))
        {
            writer.writeByte(BinaryModelSerializationTypes.ENUM_INSTANCE);
            writeStringById(instance.getName(), writer);
        }
        else
        {
            writer.writeByte(BinaryModelSerializationTypes.OTHER_INSTANCE);
            writeStringById(instance.getName(), writer);
        }

        // Serialize classifier
        serializeClassifier(instance, writer);

        // Serialize source information
        serializeSourceInformation(instance.getSourceInformation(), writer);

        // Serialize compile state
        serializeCompileState(instance, writer);

        // Serialize properties
        ListIterable<ListIterable<String>> realKeys = instance.getKeys().toSortedList().collectWith(GET_PROPERTY_REAL_KEY, instance).select(SHOULD_SERIALIZE_PROPERTY);
        writer.writeInt(realKeys.size());
        for (ListIterable<String> realKey : realKeys)
        {
            try
            {
                int realKeyId = getPropertyRealKeyId(realKey);
                writer.writeInt(realKeyId);

                ListIterable<? extends CoreInstance> values = getPropertyValueToMany(instance, realKey.getLast());
                ListIterable<? extends CoreInstance> valuesToSerialize = M3PropertyPaths.children.equals(realKey) ? values.select(this.isFromThisSource) : values;
                writer.writeInt(valuesToSerialize.size());
                for (CoreInstance value : valuesToSerialize)
                {
                    serializePropertyValue(value, writer);
                }
            }
            catch (RuntimeException e)
            {
                throw new RuntimeException("Error serializing values for property " + realKey, e);
            }
        }
    }

    private void serializeClassifier(CoreInstance instance, Writer writer)
    {
        CoreInstance classifier = instance.getClassifier();
        if (classifier == null)
        {
            throw new RuntimeException("Cannot serialize an instance with a null classifier: " + instance);
        }

        // First serialize the path
        String classifierPath = getElementPath(classifier);
        writer.writeInt(getStringReferenceId(classifierPath));

        // Next serialize a reference
        serializeReference(classifier, writer);
    }

    private void serializeSourceInformation(SourceInformation sourceInformation, Writer writer)
    {
        if (sourceInformation == null)
        {
            writer.writeBoolean(false);
        }
        else
        {
            writer.writeBoolean(true);
            // We don't need to write the source id, since we know it is this source
            writer.writeInt(sourceInformation.getStartLine());
            writer.writeInt(sourceInformation.getStartColumn());
            writer.writeInt(sourceInformation.getLine());
            writer.writeInt(sourceInformation.getColumn());
            writer.writeInt(sourceInformation.getEndLine());
            writer.writeInt(sourceInformation.getEndColumn());
        }
    }

    private void serializeCompileState(CoreInstance instance, Writer writer)
    {
        writer.writeInt(CompileStateSet.removeExtraCompileStates(instance.getCompileStates().toBitSet()));
    }

    private void serializePropertyValue(CoreInstance instance, Writer writer)
    {
        CoreInstance classifier = instance.getClassifier();
        if (classifier == this.booleanType)
        {
            serializeBoolean(instance, writer);
        }
        else if (classifier == this.dateType)
        {
            serializeDate(instance, writer);
        }
        else if (classifier == this.strictDateType)
        {
            serializeStrictDate(instance, writer);
        }
        else if (classifier == this.dateTimeType)
        {
            serializeDateTime(instance, writer);
        }
        else if (classifier == this.latestDateType)
        {
            serializeLatestDate(writer);
        }
        else if (classifier == this.floatType)
        {
            serializeFloat(instance, writer);
        }
        else if (classifier == this.decimalType)
        {
            serializeDecimal(instance, writer);
        }
        else if (classifier == this.integerType)
        {
            serializeInteger(instance, writer);
        }
        else if (classifier == this.stringType)
        {
            serializeString(instance, writer);
        }
        else
        {
            serializeReference(instance, writer);
        }
    }

    private void serializeBoolean(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.BOOLEAN);
        writer.writeBoolean(PrimitiveUtilities.getBooleanValue(instance));
    }

    private void serializeDate(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.DATE);
        writeStringById(instance.getName(), writer);
    }

    private void serializeStrictDate(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.STRICT_DATE);
        writeStringById(instance.getName(), writer);
    }

    private void serializeDateTime(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.DATE_TIME);
        writeStringById(instance.getName(), writer);
    }

    private void serializeLatestDate(Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.LATEST_DATE);
    }

    private void serializeFloat(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.FLOAT);
        writeStringById(instance.getName(), writer);
    }

    private void serializeDecimal(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.DECIMAL);
        writeStringById(instance.getName(), writer);
    }

    private void serializeInteger(CoreInstance instance, Writer writer)
    {
        Number n = PrimitiveUtilities.getIntegerValue(instance);
        if (n instanceof Integer)
        {
            writer.writeByte(BinaryModelSerializationTypes.INTEGER_INT);
            writer.writeInt(n.intValue());
        }
        else if (n instanceof Long)
        {
            writer.writeByte(BinaryModelSerializationTypes.INTEGER_LONG);
            writer.writeLong(n.longValue());
        }
        else
        {
            writer.writeByte(BinaryModelSerializationTypes.INTEGER_BIG);
            writeStringById(instance.getName(), writer);
        }
    }

    private void serializeString(CoreInstance instance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.STRING);
        String string = PrimitiveUtilities.getStringValue(instance);
        writeStringById(string, writer);
    }

    private void serializeReference(CoreInstance referenceInstance, Writer writer)
    {
        if (isPackage(referenceInstance))
        {
            if (isFromThisSource(referenceInstance))
            {
                serializeInternalReference(referenceInstance, writer);
            }
            else
            {
                serializePackageReference(referenceInstance, writer);
            }
        }
        else
        {
            if (isImportStub(referenceInstance))
            {
                ImportStub.processImportStub((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)referenceInstance, this.repository, this.processorSupport);
            }
            else if (isPropertyStub(referenceInstance))
            {
                ImportStub.processPropertyStub(referenceInstance, this.processorSupport);
            }
            else if (isEnumStub(referenceInstance))
            {
                ImportStub.processEnumStub(referenceInstance, this.processorSupport);
            }
            if (isFromAnotherSource(referenceInstance))
            {
                serializeExternalReference(referenceInstance, writer);
            }
            else
            {
                serializeInternalReference(referenceInstance, writer);
            }
        }
    }

    private void serializePackageReference(CoreInstance pkg, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.PACKAGE_REFERENCE);
        String packagePath = getElementPath(pkg);
        writeStringById(packagePath, writer);
    }

    private void serializeInternalReference(CoreInstance referenceInstance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.INTERNAL_REFERENCE);
        writer.writeInt(getInternalReferenceId(referenceInstance));
        if (!this.serialized.contains(referenceInstance))
        {
            this.serializationQueue.add(referenceInstance);
        }
    }

    private void serializeExternalReference(CoreInstance referenceInstance, Writer writer)
    {
        if (isRegisteredPackageableElementExternalReference(referenceInstance))
        {
            // PackageableElement reference which we've already registered
            writePackageableElementExternalReference(referenceInstance, writer);
        }
        else if (isRegisteredOtherExternalReference(referenceInstance))
        {
            // Other reference which we've already registered
            writeOtherExternalReference(referenceInstance, writer);
        }
        else
        {
            // Reference we haven't already registered
            ExternalReferenceSerializer serializer = findSerializer(referenceInstance);
            if (serializer == null)
            {
                if (!isPackageableElement(referenceInstance))
                {
                    // Not a PackageableElement and couldn't find a serializer
                    throwUnsupportedExternalReferenceException(referenceInstance);
                }
                // PackageableElement reference we haven't registered (will be registered by side effect)
                writePackageableElementExternalReference(referenceInstance, writer);
            }
            else
            {
                // Other reference we haven't registered
                serializeOtherExternalReference(referenceInstance, writer);
            }
        }
    }

    private void writePackageableElementExternalReference(CoreInstance referenceInstance, Writer writer)
    {
        writer.writeByte(BinaryModelSerializationTypes.EXTERNAL_PACKAGEABLE_ELEMENT_REFERENCE);
        int id = getPackageableElementExternalReferenceId(referenceInstance);
        writer.writeInt(id);
    }

    private void writeOtherExternalReference(CoreInstance referenceInstance, Writer writer)
    {
        int referenceId = getOtherExternalReferenceId(referenceInstance);
        writer.writeByte(BinaryModelSerializationTypes.EXTERNAL_OTHER_REFERENCE);
        writer.writeInt(referenceId);
    }

    private void serializeOtherExternalReference(CoreInstance referenceInstance, Writer writer)
    {
        writeOtherExternalReference(referenceInstance, writer);
        if (!this.externalOtherReferenceSerializations.containsKey(referenceInstance))
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Writer instanceWriter = BinaryWriters.newBinaryWriter(stream);
            ExternalReferenceSerializer serializer = findSerializer(referenceInstance);
            if (serializer == null)
            {
                // Couldn't find a serializer
                throwUnsupportedExternalReferenceException(referenceInstance);
            }
            int serializerId = getStringReferenceId(serializer.getTypePath());
            instanceWriter.writeInt(serializerId);
            try
            {
                serializer.serialize(referenceInstance, new BinaryExternalReferenceSerializationHelper(instanceWriter));
            }
            catch (Exception e)
            {
                // Exception occurred during serialization
                StringBuilder message = new StringBuilder("Error serializaing ");
                message.append(referenceInstance);
                message.append(" with serializer for type ");
                message.append(serializer.getTypePath());
                SourceInformation sourceInfo = referenceInstance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (source information: ")).append(')');
                }
                throw new RuntimeException(message.toString(), e);
            }
            this.externalOtherReferenceSerializations.put(referenceInstance, stream.toByteArray());
        }
    }

    /**
     * Return true if the instance is known to be from another source.
     * That is, it has source information that indicates it is from
     * another source.
     *
     * @param instance instance
     * @return true if the instance is known to be from another source
     */
    private boolean isFromAnotherSource(CoreInstance instance)
    {
        return isFromAnotherSource(instance.getSourceInformation());
    }

    /**
     * Return true if the source information is non-null and for a
     * different source.
     *
     * @param sourceInfo source information
     * @return true if the source info is non-null and for a different source
     */
    private boolean isFromAnotherSource(SourceInformation sourceInfo)
    {
        return (sourceInfo != null) && !sourceInfo.getSourceId().equals(this.source.getId());
    }

    /**
     * Return true if the instance is known to be from this source.
     * That is, it has source information that indicates it is from
     * this source.
     *
     * @param instance instance
     * @return true if the instance is known to be from this source
     */
    private boolean isFromThisSource(CoreInstance instance)
    {
        return isFromThisSource(instance.getSourceInformation());
    }

    /**
     * Return true if the source information is non-null and for
     * this source.
     *
     * @param sourceInfo source information
     * @return true if the source info is non-null and for this source
     */
    private boolean isFromThisSource(SourceInformation sourceInfo)
    {
        return (sourceInfo != null) && sourceInfo.getSourceId().equals(this.source.getId());
    }

    private void writeStringById(String string, Writer writer)
    {
        int id = getStringReferenceId(string);
        writer.writeInt(id);
    }

    private void writeStringsById(ListIterable<String> strings, Writer writer)
    {
        int[] ids = getStringReferenceIds(strings);
        writer.writeIntArray(ids);
    }

    private int getInternalReferenceId(CoreInstance instance)
    {
        int id = this.internalInstanceIds.getIfAbsent(instance, -1);
        if (id == -1)
        {
            id = registerInternalInstance(instance);
        }
        return id;
    }

    private void possiblyRegisterInternalInstance(CoreInstance instance)
    {
        if (!this.internalInstanceIds.containsKey(instance))
        {
            registerInternalInstance(instance);
        }
    }

    private int registerInternalInstance(CoreInstance instance)
    {
        int id = this.internalInstances.size();
        this.internalInstances.add(instance);
        this.internalInstanceIds.put(instance, id);
        return id;
    }

    private boolean isRegisteredPackageableElementExternalReference(CoreInstance instance)
    {
        return this.externalPackageableElementReferences.containsKey(instance);
    }

    private int getPackageableElementExternalReferenceId(CoreInstance instance)
    {
        int id = this.externalPackageableElementReferences.getIfAbsent(instance, -1);
        if (id == -1)
        {
            id = registerPackageableElementExternalReferenceId(instance);
        }
        return id;
    }

    private int registerPackageableElementExternalReferenceId(CoreInstance instance)
    {
        String path = getElementPath(instance);
        int id = getStringReferenceId(path);
        this.externalPackageableElementReferences.put(instance, id);
        return id;
    }

    private boolean isRegisteredOtherExternalReference(CoreInstance instance)
    {
        return this.externalOtherReferenceIds.containsKey(instance);
    }

    private int getOtherExternalReferenceId(CoreInstance instance)
    {
        int id = this.externalOtherReferenceIds.getIfAbsent(instance, -1);
        if (id == -1)
        {
            id = registerOtherExternalReference(instance);
        }
        return id;
    }

    private int registerOtherExternalReference(CoreInstance instance)
    {
        int id = this.externalOtherReferences.size();
        this.externalOtherReferences.add(instance);
        this.externalOtherReferenceIds.put(instance, id);
        return id;
    }

    private int getStringReferenceId(String string)
    {
        int id = this.stringIds.getIfAbsent(string, -1);
        if (id == -1)
        {
            id = registerString(string);
        }
        return id;
    }

    private void possiblyRegisterString(String string)
    {
        if (!this.stringIds.containsKey(string))
        {
            registerString(string);
        }
    }

    private int registerString(String string)
    {
        int id = this.strings.size();
        this.strings.add(string);
        this.stringIds.put(string, id);
        return id;
    }

    private int[] getStringReferenceIds(ListIterable<String> strings)
    {
        int[] ids = new int[strings.size()];
        int i = 0;
        for (String string : strings)
        {
            ids[i] = getStringReferenceId(string);
            i++;
        }
        return ids;
    }

    private void registerStrings(Iterable<String> strings)
    {
        for (String string : strings)
        {
            if (!this.stringIds.containsKey(string))
            {
                registerString(string);
            }
        }
    }

    private int getPropertyRealKeyId(ListIterable<String> propertyRealKey)
    {
        int id = this.propertyRealKeyIds.getIfAbsent(propertyRealKey, -1);
        if (id == -1)
        {
            id = registerPropertyRealKey(propertyRealKey);
        }
        return id;
    }

    private int registerPropertyRealKey(ListIterable<String> propertyRealKey)
    {
        registerStrings(propertyRealKey);
        int id = this.propertyRealKeys.size();
        this.propertyRealKeys.add(propertyRealKey);
        this.propertyRealKeyIds.put(propertyRealKey, id);
        return id;
    }

    private ExternalReferenceSerializer findSerializer(CoreInstance instance)
    {
        // Find serializer by instance type
        CoreInstance type = instance.getClassifier();
        ListIterable<CoreInstance> generalizations = Type.getGeneralizationResolutionOrder(type, this.processorSupport);
        for (CoreInstance genl : generalizations)
        {
            String genlPath = PackageableElement.getUserPathForPackageableElement(genl);
            ExternalReferenceSerializer serializer = this.serializerLibrary.getSerializer(genlPath);
            if (serializer != null)
            {
                return serializer;
            }
        }
        return null;
    }

    private void throwUnsupportedExternalReferenceException(CoreInstance instance)
    {
        StringBuilder message = new StringBuilder("External reference cannot be created for instance of ");
        PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
        message.append(": ");
        message.append(instance);
        SourceInformation sourceInfo = instance.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendMessage(message.append(" (source information: ")).append(')');
        }
        throw new RuntimeException(message.toString());
    }

    private String getElementPath(CoreInstance element)
    {
        return PackageableElement.getUserPathForPackageableElement(element);
    }

    private boolean instanceOf(CoreInstance instance, CoreInstance type)
    {
        return Instance.instanceOf(instance, type, this.processorSupport);
    }

    private boolean isImportStub(CoreInstance instance)
    {
        return instance.getClassifier() == this.importStubClass;
    }

    private boolean isPropertyStub(CoreInstance instance)
    {
        return instance.getClassifier() == this.propertyStubClass;
    }

    private boolean isEnumStub(CoreInstance instance)
    {
        return instance.getClassifier() == this.enumStubClass;
    }

    private boolean isEnumeration(CoreInstance instance)
    {
        return instance.getClassifier() == this.enumerationClass;
    }

    private boolean isPackage(CoreInstance instance)
    {
        return instance.getClassifier() == this.packageClass;
    }

    private boolean isPackageableElement(CoreInstance instance)
    {
        return instanceOf(instance, this.packageableElementClass);
    }

    private boolean isTopLevel(CoreInstance instance)
    {
        return this.repositoryTopLevels.contains(instance);
    }

    private boolean isPackaged(CoreInstance instance)
    {
        return isPackageableElement(instance) && getPropertyValueToOne(instance, M3Properties._package) != null;
    }

    private boolean isAnonymousInstance(CoreInstance instance)
    {
        return ModelRepository.isAnonymousInstanceName(instance.getName());
    }

    private CoreInstance getPropertyValueToOne(CoreInstance instance, String propertyName)
    {
        return instance.getValueForMetaPropertyToOne(propertyName);
    }

    private CoreInstance getPropertyValueToOneResolved(CoreInstance instance, String propertyName)
    {
        return Instance.getValueForMetaPropertyToOneResolved(instance, propertyName, this.processorSupport);
    }

    private ListIterable<? extends CoreInstance> getPropertyValueToMany(CoreInstance instance, String propertyName)
    {
        return instance.getValueForMetaPropertyToMany(propertyName);
    }

    private ListIterable<? extends CoreInstance> getPropertyValueToManyResolved(CoreInstance instance, String propertyName)
    {
        return Instance.getValueForMetaPropertyToManyResolved(instance, propertyName, this.processorSupport);
    }

    public static SourceSerializationResult serialize(Writer writer, Source source, ModelRepository modelRepository, ProcessorSupport processorSupport, ParserLibrary parserLibrary)
    {
        BinaryModelSourceSerializer serializer = new BinaryModelSourceSerializer(source, modelRepository, processorSupport, parserLibrary);
        serializer.serialize(writer);
        RichIterable<String> serializedInstances = LazyIterate.concatenate(serializer.sourceNewInstancePathIds.asLazy().collect(serializer.getStringById), serializer.sourceOtherInstancePathIds.asLazy().collect(serializer.getStringById));
        RichIterable<String> externalReferences = serializer.externalPackageableElementReferences.asLazy().collect(serializer.getStringById);
        return new SourceSerializationResult(source.getId(), serializedInstances, externalReferences);
    }

    public static SourceSerializationResult serialize(Writer writer, Source source, PureRuntime runtime)
    {
        return serialize(writer, source, runtime.getModelRepository(), runtime.getProcessorSupport(), runtime.getIncrementalCompiler().getParserLibrary());
    }

    private class BinaryExternalReferenceSerializationHelper implements ExternalReferenceSerializationHelper
    {
        private final Writer writer;

        private BinaryExternalReferenceSerializationHelper(Writer writer)
        {
            this.writer = writer;
        }

        @Override
        public CoreInstance getPropertyValueToOne(CoreInstance instance, String propertyName)
        {
            return getPropertyValueToOneResolved(instance, propertyName);
        }

        @Override
        public ListIterable<? extends CoreInstance> getPropertyValueToMany(CoreInstance instance, String propertyName)
        {
            return getPropertyValueToManyResolved(instance, propertyName);
        }

        @Override
        public void writeByte(byte b)
        {
            this.writer.writeByte(b);
        }

        @Override
        public void writeInt(int i)
        {
            this.writer.writeInt(i);
        }

        @Override
        public void writeString(String string)
        {
            if (string == null)
            {
                throw new IllegalArgumentException("null strings are not allowed");
            }
            writeStringById(string, this.writer);
        }

        @Override
        public void writeElementReference(CoreInstance element)
        {
            if (element == null)
            {
                throw new IllegalArgumentException("Cannot serialize null element");
            }

            // Packages may not have source information, so we do this check first
            if (isPackage(element))
            {
                serializePackageReference(element, this.writer);
            }
            else if (isFromThisSource(element))
            {
                serializeInternalReference(element, this.writer);
            }
            else if (isFromAnotherSource(element))
            {
                serializeExternalReference(element, this.writer);
            }
            else
            {
                throw new IllegalArgumentException("Could not determine source for element to serialize reference: " + element);
            }
        }
    }
}
