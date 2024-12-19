// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.element.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.Writer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class SerializerV1 extends BaseV1
{
    private final CoreInstance element;
    private final StringIndexer stringIndexer;
    private final ReferenceIdProvider referenceIdProvider;
    private final ProcessorSupport processorSupport;
    private final MutableList<NodeToSerialize> nodesToSerialize = Lists.mutable.empty();
    private final MutableObjectIntMap<CoreInstance> internalIds = ObjectIntMaps.mutable.empty();
    private final MutableMap<CoreInstance, String> classifierPathCache = Maps.mutable.empty();
    private final MutableMap<CoreInstance, ListIterable<PropertyInfo>> propertyInfoCache = Maps.mutable.empty();
    private final MapIterable<CoreInstance, BiConsumer<Writer, CoreInstance>> primitiveSerializers;
    private final MapIterable<CoreInstance, Consumer<CoreInstance>> stubValidators;
    private final MapIterable<String, ImmutableList<String>> backRefProps = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS
            .groupByUniqueKey(ImmutableList::getLast, Maps.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size() + 1))
            .withKeyValue(M3Properties.children, M3PropertyPaths.children);

    SerializerV1(CoreInstance element, StringIndexer stringIndexer, ReferenceIdProvider referenceIdProvider, ProcessorSupport processorSupport)
    {
        this.element = element;
        this.stringIndexer = stringIndexer;
        this.referenceIdProvider = referenceIdProvider;
        this.processorSupport = processorSupport;
        this.primitiveSerializers = Maps.mutable.<CoreInstance, BiConsumer<Writer, CoreInstance>>ofInitialCapacity(11)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Boolean), SerializerV1::serializeBoolean)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Byte), SerializerV1::serializeByte)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Date), SerializerV1::serializeDate)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.DateTime), SerializerV1::serializeDateTime)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Decimal), SerializerV1::serializeDecimal)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Float), SerializerV1::serializeFloat)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.Integer), SerializerV1::serializeInteger)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.LatestDate), SerializerV1::serializeLatestDate)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.StrictDate), SerializerV1::serializeStrictDate)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.StrictTime), SerializerV1::serializeStrictTime)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.String), SerializerV1::serializeString);
        this.stubValidators = Maps.mutable.<CoreInstance, Consumer<CoreInstance>>ofInitialCapacity(4)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.EnumStub), this::validateEnumStub)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.GrammarInfoStub), this::validateGrammarInfoStub)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.ImportStub), this::validateImportStub)
                .withKeyValue(this.processorSupport.package_getByUserPath(M3Paths.PropertyStub), this::validatePropertyStub);
    }

    void serialize(Writer writer)
    {
        Writer stringIndexedWriter = collectNodesAndIndexStrings(writer);
        stringIndexedWriter.writeString(PackageableElement.getUserPathForPackageableElement(this.element));
        stringIndexedWriter.writeString(this.element.getSourceInformation().getSourceId());
        serializeNodes(stringIndexedWriter);
    }

    private Writer collectNodesAndIndexStrings(Writer writer)
    {
        MutableSet<String> strings = Sets.mutable.empty();
        Deque<NodeToSerialize> deque = new ArrayDeque<>();
        deque.add(toSerialize(this.element));
        strings.add(PackageableElement.getUserPathForPackageableElement(this.element));
        strings.add(this.element.getSourceInformation().getSourceId());
        while (!deque.isEmpty())
        {
            NodeToSerialize node = deque.pollFirst();
            if (!this.internalIds.containsKey(node.instance))
            {
                addNodeToSerialize(node);
                if (!isAnonymousInstance(node.instance))
                {
                    strings.add(node.instance.getName());
                }
                strings.add(getClassifierPath(node.classifier));
                if (this.referenceIdProvider.hasReferenceId(node.instance))
                {
                    strings.add(this.referenceIdProvider.getReferenceId(node.instance));
                }
                getPropertyInfos(node.classifier).forEach(propertyInfo ->
                {
                    if (!propertyInfo.isBackRef)
                    {
                        ListIterable<? extends CoreInstance> values = node.instance.getValueForMetaPropertyToMany(propertyInfo.name);
                        if (values.notEmpty())
                        {
                            strings.add(propertyInfo.name);
                            strings.add(propertyInfo.sourceType);
                            values.forEach(value ->
                            {
                                if (isExternal(value))
                                {
                                    strings.add(this.referenceIdProvider.getReferenceId(value));
                                }
                                else
                                {
                                    CoreInstance valueClassifier = this.processorSupport.getClassifier(value);
                                    if (!isPrimitiveType(valueClassifier))
                                    {
                                        deque.addLast(toSerialize(value, valueClassifier));
                                    }
                                    else if (M3Paths.String.equals(valueClassifier.getName()))
                                    {
                                        strings.add(PrimitiveUtilities.getStringValue(value));
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }

        // internal reference usages are those whose owners are internal nodes
        MutableList<CoreInstance> internalRefUsages = Lists.mutable.empty();
        this.nodesToSerialize.forEach(node -> getPropertyInfos(node.classifier).forEach(propertyInfo ->
        {
            if (propertyInfo.isBackRef)
            {
                switch (propertyInfo.name)
                {
                    case M3Properties.referenceUsages:
                    {
                        // for reference usages, we check whether the owner is internal
                        MutableList<? extends CoreInstance> nodeIntRefUsages = node.instance.getValueForMetaPropertyToMany(propertyInfo.name)
                                .select(refUsage -> this.internalIds.containsKey(refUsage.getValueForMetaPropertyToOne(M3Properties.owner)), Lists.mutable.empty());
                        if (nodeIntRefUsages.notEmpty())
                        {
                            internalRefUsages.addAll(nodeIntRefUsages.sortThis(this::compareRefUsages));
                        }
                        break;
                    }
                    case M3Properties.children:
                    {
                        // we currently do not allow internal elements as package children
                        if (node.instance.getValueForMetaPropertyToMany(propertyInfo.name).anySatisfy(this.internalIds::containsKey))
                        {
                            throw new IllegalStateException("Internal elements not supported as package children");
                        }
                        break;
                    }
                    default:
                    {
                        if (!strings.contains(propertyInfo.name) && node.instance.getValueForMetaPropertyToMany(propertyInfo.name).anySatisfy(this.internalIds::containsKey))
                        {
                            strings.add(propertyInfo.name);
                        }
                    }
                }
            }
        }));
        if (internalRefUsages.notEmpty())
        {
            CoreInstance refUsageClassifier = this.processorSupport.package_getByUserPath(M3Paths.ReferenceUsage);
            strings.add(M3Properties.referenceUsages);
            strings.add(M3Paths.Referenceable);
            strings.add(this.referenceIdProvider.getReferenceId(refUsageClassifier));
            strings.add(M3Properties.owner);
            strings.add(M3Properties.propertyName);
            strings.add(M3Properties.offset);
            internalRefUsages.forEach(ru ->
            {
                strings.add(PrimitiveUtilities.getStringValue(ru.getValueForMetaPropertyToOne(M3Properties.propertyName)));
                addNodeToSerialize(ru, refUsageClassifier);
            });
        }
        return this.stringIndexer.writeStringIndex(writer, strings);
    }

    private void serializeNodes(Writer writer)
    {
        int compileStateBitSetWidth = getIntWidth(CompileStateSet.toBitSet(CompileState.values()));
        writer.writeByte((byte) compileStateBitSetWidth);
        writer.writeInt(this.nodesToSerialize.size());
        int internalIdWidth = getIntWidth(this.nodesToSerialize.size());
        this.nodesToSerialize.forEach(node -> serializeNode(writer, node, internalIdWidth, compileStateBitSetWidth));
    }

    private void serializeNode(Writer writer, NodeToSerialize node, int internalIdWidth, int compileStateBitSetWidth)
    {
        serializeName(writer, node.instance);
        serializeClassifier(writer, node.classifier);
        serializeSourceInfo(writer, node.instance);
        serializeReferenceId(writer, node.instance);
        serializeCompileStateBitSet(writer, node.instance, compileStateBitSetWidth);

        MutableList<Pair<PropertyInfo, ListIterable<? extends CoreInstance>>> propertiesWithValues = Lists.mutable.empty();
        getPropertyInfos(node.classifier).forEach(propertyInfo ->
        {
            ListIterable<? extends CoreInstance> values = node.instance.getValueForMetaPropertyToMany(propertyInfo.name);
            if (values.notEmpty())
            {
                if (propertyInfo.isBackRef)
                {
                    MutableList<? extends CoreInstance> internalBackReferences = values.select(this.internalIds::containsKey, Lists.mutable.empty());
                    if (internalBackReferences.notEmpty())
                    {
                        internalBackReferences.sortThisBy(this.internalIds::get);
                        propertiesWithValues.add(Tuples.pair(propertyInfo, internalBackReferences));
                    }
                }
                else
                {
                    propertiesWithValues.add(Tuples.pair(propertyInfo, values));
                }
            }
        });
        writer.writeInt(propertiesWithValues.size());
        propertiesWithValues.forEach(pair ->
        {
            PropertyInfo propertyInfo = pair.getOne();
            ListIterable<? extends CoreInstance> values = pair.getTwo();
            writer.writeString(propertyInfo.name);
            writer.writeString(propertyInfo.sourceType);
            writer.writeInt(values.size());
            values.forEach(value ->
            {
                int internalId = this.internalIds.getIfAbsent(value, -1);
                if (internalId != -1)
                {
                    serializeInternalReference(writer, internalId, internalIdWidth);
                }
                else if (isExternal(value))
                {
                    serializeExternalReference(writer, this.referenceIdProvider.getReferenceId(value));
                }
                else
                {
                    CoreInstance valueClassifier = this.processorSupport.getClassifier(value);
                    BiConsumer<Writer, CoreInstance> serializer = this.primitiveSerializers.get(valueClassifier);
                    if (serializer == null)
                    {
                        StringBuilder builder = new StringBuilder("Expected primitive value, found instance of ");
                        PackageableElement.writeUserPathForPackageableElement(builder, valueClassifier);
                        builder.append(": ").append(value);
                        throw new IllegalStateException(builder.toString());
                    }
                    serializer.accept(writer, value);
                }
            });
        });
    }

    private void serializeClassifier(Writer writer, CoreInstance classifier)
    {
        writer.writeString(getClassifierPath(classifier));
    }

    private void serializeName(Writer writer, CoreInstance instance)
    {
        if (isAnonymousInstance(instance))
        {
            writer.writeBoolean(false);
        }
        else
        {
            writer.writeBoolean(true);
            writer.writeString(instance.getName());
        }
    }

    private void serializeSourceInfo(Writer writer, CoreInstance instance)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        if (sourceInfo == null)
        {
            writer.writeByte((byte) VALUE_NOT_PRESENT);
        }
        else
        {
            int intWidth = getIntWidth(sourceInfo.getStartLine(), sourceInfo.getStartColumn(), sourceInfo.getLine(), sourceInfo.getColumn(), sourceInfo.getEndLine(), sourceInfo.getEndColumn());
            writer.writeByte((byte) (VALUE_PRESENT | intWidth));
            writeIntOfWidth(writer, sourceInfo.getStartLine(), intWidth);
            writeIntOfWidth(writer, sourceInfo.getStartColumn(), intWidth);
            writeIntOfWidth(writer, sourceInfo.getLine(), intWidth);
            writeIntOfWidth(writer, sourceInfo.getColumn(), intWidth);
            writeIntOfWidth(writer, sourceInfo.getEndLine(), intWidth);
            writeIntOfWidth(writer, sourceInfo.getEndColumn(), intWidth);
        }
    }

    private void serializeReferenceId(Writer writer, CoreInstance instance)
    {
        if (this.referenceIdProvider.hasReferenceId(instance))
        {
            String referenceId = this.referenceIdProvider.getReferenceId(instance);
            writer.writeByte((byte) VALUE_PRESENT);
            writer.writeString(referenceId);
        }
        else
        {
            writer.writeByte((byte) VALUE_NOT_PRESENT);
        }
    }

    private void serializeCompileStateBitSet(Writer writer, CoreInstance instance, int compileStateBitSetWidth)
    {
        writeIntOfWidth(writer, instance.getCompileStates().toBitSet(), compileStateBitSetWidth);
    }

    private void serializeExternalReference(Writer writer, String id)
    {
        writer.writeByte((byte) EXTERNAL_REFERENCE);
        writer.writeString(id);
    }

    private void serializeInternalReference(Writer writer, int id, int idWidth)
    {
        writer.writeByte((byte) INTERNAL_REFERENCE);
        writeIntOfWidth(writer, id, idWidth);
    }

    private void addNodeToSerialize(CoreInstance instance, CoreInstance classifier)
    {
        addNodeToSerialize(toSerialize(instance, classifier));
    }

    private void addNodeToSerialize(NodeToSerialize node)
    {
        this.internalIds.put(node.instance, this.nodesToSerialize.size());
        this.nodesToSerialize.add(node);
    }

    private NodeToSerialize toSerialize(CoreInstance instance)
    {
        return toSerialize(instance, this.processorSupport.getClassifier(instance));
    }

    private NodeToSerialize toSerialize(CoreInstance instance, CoreInstance classifier)
    {
        validateStub(instance, classifier);
        return new NodeToSerialize(instance, classifier);
    }

    private String getClassifierPath(CoreInstance classifier)
    {
        return this.classifierPathCache.getIfAbsentPutWithKey(classifier, PackageableElement::getUserPathForPackageableElement);
    }

    private void validateStub(CoreInstance instance, CoreInstance classifier)
    {
        Consumer<CoreInstance> stubValidator = this.stubValidators.get(classifier);
        if (stubValidator != null)
        {
            stubValidator.accept(instance);
        }
    }

    private void validateEnumStub(CoreInstance stub)
    {
        if (stub.getValueForMetaPropertyToOne(M3Properties.resolvedEnum) == null)
        {
            StringBuilder builder = new StringBuilder("Unresolved EnumStub");
            ResolvedGraphPath path = GraphTools.findPathToInstanceWithinElement(this.element, stub, this.processorSupport);
            if (path != null)
            {
                path.getGraphPath().writeDescription(builder.append(" at "));
            }
            builder.append(": ");
            String enumName = PrimitiveUtilities.getStringValue(stub.getValueForMetaPropertyToOne(M3Properties.enumName), null);
            if (enumName == null)
            {
                builder.append("no enum name");
            }
            else
            {
                CoreInstance enumeration = stub.getValueForMetaPropertyToOne(M3Properties.enumeration);
                if (this.stubValidators.containsKey(this.processorSupport.getClassifier(enumeration)))
                {
                    // enumeration is an ImportStub: get idOrPath
                    builder.append(PrimitiveUtilities.getStringValue(enumeration.getValueForMetaPropertyToOne(M3Properties.idOrPath), null));
                }
                else
                {
                    PackageableElement.writeUserPathForPackageableElement(builder, enumeration);
                }
                builder.append('.').append(enumName);
            }
            throw new PureCompilationException(getSourceInfoForStubValidation(stub), builder.toString());
        }
    }

    private void validateGrammarInfoStub(CoreInstance stub)
    {
        if (stub.getValueForMetaPropertyToOne(M3Properties.value) == null)
        {
            StringBuilder builder = new StringBuilder("Unresolved GrammarInfoStub");
            ResolvedGraphPath path = GraphTools.findPathToInstanceWithinElement(this.element, stub, this.processorSupport);
            if (path != null)
            {
                path.getGraphPath().writeDescription(builder.append(" at "));
            }
            builder.append(": ");
            CoreInstance original = stub.getValueForMetaPropertyToOne("original");
            if (original == null)
            {
                builder.append("no original");
            }
            else
            {
                CoreInstance originalClassifier = this.processorSupport.getClassifier(original);
                if (isPrimitiveType(originalClassifier))
                {
                    if (M3Paths.String.equals(originalClassifier.getName()))
                    {
                        builder.append('\'').append(PrimitiveUtilities.getStringValue(original)).append('\'');
                    }
                    else
                    {
                        builder.append(original.getName());
                    }
                }
                else
                {
                    builder.append(original);
                }
            }
            throw new PureCompilationException(getSourceInfoForStubValidation(stub), builder.toString());
        }
    }

    private void validateImportStub(CoreInstance stub)
    {
        if (stub.getValueForMetaPropertyToOne(M3Properties.resolvedNode) == null)
        {
            StringBuilder builder = new StringBuilder("Unresolved ImportStub");
            ResolvedGraphPath path = GraphTools.findPathToInstanceWithinElement(this.element, stub, this.processorSupport);
            if (path != null)
            {
                path.getGraphPath().writeDescription(builder.append(" at "));
            }
            builder.append(": ");
            String idOrPath = PrimitiveUtilities.getStringValue(stub.getValueForMetaPropertyToOne(M3Properties.idOrPath), null);
            if (idOrPath == null)
            {
                builder.append("no id or path");
            }
            else
            {
                builder.append('\'').append(idOrPath).append('\'');
            }
            throw new PureCompilationException(getSourceInfoForStubValidation(stub), builder.toString());
        }
    }

    private void validatePropertyStub(CoreInstance stub)
    {
        if (stub.getValueForMetaPropertyToOne(M3Properties.resolvedProperty) == null)
        {
            StringBuilder builder = new StringBuilder("Unresolved PropertyStub");
            ResolvedGraphPath path = GraphTools.findPathToInstanceWithinElement(this.element, stub, this.processorSupport);
            if (path != null)
            {
                path.getGraphPath().writeDescription(builder.append(" at "));
            }
            builder.append(": ");
            String propertyName = PrimitiveUtilities.getStringValue(stub.getValueForMetaPropertyToOne(M3Properties.propertyName), null);
            if (propertyName == null)
            {
                builder.append("no property name");
            }
            else
            {
                CoreInstance owner = stub.getValueForMetaPropertyToOne(M3Properties.owner);
                if (this.stubValidators.containsKey(this.processorSupport.getClassifier(owner)))
                {
                    // owner is an ImportStub: get idOrPath
                    builder.append(PrimitiveUtilities.getStringValue(owner.getValueForMetaPropertyToOne(M3Properties.idOrPath), null));
                }
                else
                {
                    PackageableElement.writeUserPathForPackageableElement(builder, owner);
                }
                builder.append('.').append(propertyName);
            }
            throw new PureCompilationException(getSourceInfoForStubValidation(stub), builder.toString());
        }
    }

    private SourceInformation getSourceInfoForStubValidation(CoreInstance stub)
    {
        SourceInformation stubSourceInfo = stub.getSourceInformation();
        return (stubSourceInfo == null) ? this.element.getSourceInformation() : stubSourceInfo;
    }

    private ListIterable<PropertyInfo> getPropertyInfos(CoreInstance classifier)
    {
        return this.propertyInfoCache.getIfAbsentPut(classifier, () ->
        {
            MapIterable<String, CoreInstance> propertiesByName = this.processorSupport.class_getSimplePropertiesByName(classifier);
            MutableList<PropertyInfo> infos = Lists.mutable.ofInitialCapacity(propertiesByName.size());
            propertiesByName.forEachKeyValue((name, property) -> infos.add(computePropertyInfo(name, property)));
            return infos.sortThisBy(pi -> pi.name);
        });
    }

    private PropertyInfo computePropertyInfo(String name, CoreInstance property)
    {
        String sourceType = getClassifierPath(Property.getPropertySourceType(property, this.processorSupport));
        return new PropertyInfo(name, sourceType, isBackReferenceProperty(name, property));
    }

    private boolean isBackReferenceProperty(String propertyName, CoreInstance property)
    {
        ImmutableList<String> propertyPath = this.backRefProps.get(propertyName);
        return (propertyPath != null) && propertyPath.equals(Property.calculatePropertyPath(property, this.processorSupport));
    }

    private boolean isPrimitiveType(CoreInstance classifier)
    {
        return this.primitiveSerializers.containsKey(classifier);
    }

    private boolean isExternal(CoreInstance instance)
    {
        if (this.element == instance)
        {
            return false;
        }

        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo == null) ? _Package.isPackage(instance, this.processorSupport) : !this.element.getSourceInformation().subsumes(sourceInfo);
    }

    private boolean isAnonymousInstance(CoreInstance instance)
    {
        return ModelRepository.isAnonymousInstanceName(instance.getName());
    }

    private int compareRefUsages(CoreInstance refUsage1, CoreInstance refUsage2)
    {
        int ownerId1 = this.internalIds.get(refUsage1.getValueForMetaPropertyToOne(M3Properties.owner));
        int ownerId2 = this.internalIds.get(refUsage2.getValueForMetaPropertyToOne(M3Properties.owner));
        int cmp = Integer.compare(ownerId1, ownerId2);
        if (cmp == 0)
        {
            String propertyName1 = PrimitiveUtilities.getStringValue(refUsage1.getValueForMetaPropertyToOne(M3Properties.propertyName));
            String propertyName2 = PrimitiveUtilities.getStringValue(refUsage2.getValueForMetaPropertyToOne(M3Properties.propertyName));
            cmp = propertyName1.compareTo(propertyName2);
            if (cmp == 0)
            {
                int offset1 = PrimitiveUtilities.getIntegerValue(refUsage1.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
                int offset2 = PrimitiveUtilities.getIntegerValue(refUsage2.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
                cmp = Integer.compare(offset1, offset2);
            }
        }
        return cmp;
    }

    private static class NodeToSerialize
    {
        private final CoreInstance instance;
        private final CoreInstance classifier;

        private NodeToSerialize(CoreInstance instance, CoreInstance classifier)
        {
            this.instance = instance;
            this.classifier = classifier;
        }
    }

    private static class PropertyInfo
    {
        private final String name;
        private final String sourceType;
        private final boolean isBackRef;

        private PropertyInfo(String name, String sourceType, boolean isBackRef)
        {
            this.name = name;
            this.sourceType = sourceType;
            this.isBackRef = isBackRef;
        }
    }
}
