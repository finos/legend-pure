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

package org.finos.legend.pure.m3.coreinstance.lazy.generator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.KeyIndex;
import org.finos.legend.pure.m3.coreinstance.helper.PrimitiveHelper;
import org.finos.legend.pure.m3.coreinstance.helper.PropertyTypeHelper;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyConcreteElement;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyCoreInstance;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyVirtualPackage;
import org.finos.legend.pure.m3.coreinstance.lazy.ManyValues;
import org.finos.legend.pure.m3.coreinstance.lazy.OneValue;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.lazy.PropertyValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumCoreInstanceWrapper;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.element.PropertyValues;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.tools.TextTools;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class M3LazyCoreInstanceGenerator
{
    private static final String ROOT_PACKAGE = "org.finos.legend.pure.m3.coreinstance";
    private static final String CLASS_LAZY_CONCRETE_SUFFIX = "LazyConcrete";
    private static final String CLASS_LAZY_COMPONENT_SUFFIX = "LazyComponent";
    private static final String CLASS_VIRTUAL_PACKAGE_SUFFIX = "LazyVirtual";

    private static final String ENUM_COMPONENT_CLASS_NAME = "_$Enum" + CLASS_LAZY_COMPONENT_SUFFIX;

    private static final String FULL_SYSTEM_PATH_FIELD = "FULL_SYSTEM_PATH";
    private static final String KEY_INDEX_FIELD = "KEY_INDEX";

    private final ProcessorSupport processorSupport;
    private final CoreInstance anyClass;
    private final CoreInstance enumClass;
    private final CoreInstance enumerationClass;
    private final CoreInstance functionTypeClass;
    private final CoreInstance nilClass;
    private final CoreInstance packageableElementClass;
    private final CoreInstance packageClass;
    private final CoreInstance primitiveTypeClass;

    public M3LazyCoreInstanceGenerator(ProcessorSupport processorSupport)
    {
        this.processorSupport = Objects.requireNonNull(processorSupport);
        this.anyClass = processorSupport.type_TopType();
        this.enumClass = processorSupport.package_getByUserPath(M3Paths.Enum);
        this.enumerationClass = processorSupport.package_getByUserPath(M3Paths.Enumeration);
        this.functionTypeClass = processorSupport.package_getByUserPath(M3Paths.FunctionType);
        this.nilClass = processorSupport.type_BottomType();
        this.packageableElementClass = processorSupport.package_getByUserPath(M3Paths.PackageableElement);
        this.packageClass = processorSupport.package_getByUserPath(M3Paths.Package);
        this.primitiveTypeClass = processorSupport.package_getByUserPath(M3Paths.PrimitiveType);
    }

    public void generateImplementations(Predicate<String> sourceFilter, Path outputDirectory)
    {
        Objects.requireNonNull(outputDirectory, "directory is required");
        generateImplementations(sourceFilter, (className, code) -> writeClassToFile(outputDirectory, className, code));
    }

    public void generateImplementations(Predicate<String> sourceFilter, BiConsumer<String, String> consumer)
    {
        Deque<CoreInstance> deque = new ArrayDeque<>();
        _Package.SPECIAL_TYPES.collect(this.processorSupport::repository_getTopLevel, deque);
        deque.add(this.processorSupport.repository_getTopLevel(M3Paths.Root));
        while (!deque.isEmpty())
        {
            CoreInstance element = deque.pollFirst();
            SourceInformation sourceInfo = element.getSourceInformation();
            if ((sourceInfo != null) && ((sourceFilter == null) || sourceFilter.test(element.getSourceInformation().getSourceId())))
            {
                generateImplementations(element, consumer);
            }
            if (_Package.isPackage(element, this.processorSupport))
            {
                Iterate.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(element, M3Properties.children, this.processorSupport), deque);
            }
        }
    }

    public void generateImplementations(CoreInstance type, BiConsumer<String, String> consumer)
    {
        if ((type != this.nilClass) && _Class.isClass(type, this.processorSupport))
        {
            generateClassImplementations(type, consumer);
        }
    }

    private void generateClassImplementations(CoreInstance cls, BiConsumer<String, String> consumer)
    {
        if (cls.getValueForMetaPropertyToMany(M3Properties.typeVariables).notEmpty())
        {
            throw new UnsupportedOperationException("type variables are not currently supported for classes: " + PackageableElement.getUserPathForPackageableElement(cls));
        }
        CoreInstance classGenericType = buildClassGenericType(cls);
        ListIterable<PropertyInfo> simpleProperties = getSimplePropertiesSortedByName(classGenericType, cls);
        ListIterable<PropertyInfo> qualifiedProperties = getQualifiedPropertiesSortedByName(classGenericType, cls);
        if (cls == this.packageClass)
        {
            // Special handling for Package, which can be concrete or virtual (but not component)
            generateConcreteElementImplementation(cls, simpleProperties, qualifiedProperties, consumer);
            generateVirtualPackageImplementation(cls, simpleProperties, qualifiedProperties, consumer);
            return;
        }
        if (this.processorSupport.type_subTypeOf(cls, this.packageableElementClass))
        {
            // PackageableElement and its subtypes can be concrete or component
            generateConcreteElementImplementation(cls, simpleProperties, qualifiedProperties, consumer);
        }
        generateComponentInstanceImplementation(cls, simpleProperties, qualifiedProperties, consumer);
        if (cls == this.enumClass)
        {
            // Special handling for Enums
            generateEnumComponentInstanceImplementation(consumer);
        }
    }

    private void generateConcreteElementImplementation(CoreInstance cls, ListIterable<PropertyInfo> simpleProperties, ListIterable<PropertyInfo> qualifiedProperties, BiConsumer<String, String> consumer)
    {
        String javaPackage = getJavaPackage(cls);
        String className = getLazyConcreteElementClassName(cls);
        String classInterfaceName = M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(cls);
        Class<? extends AbstractLazyCoreInstance> superClass = AbstractM3GeneratedLazyConcreteElement.class;

        String typeParams = getTypeParameters(cls);
        String classNamePlusTypeParams = className + typeParams;
        String interfaceNamePlusTypeParams = classInterfaceName + typeParams;

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                BackReferenceProvider.class,
                ConcreteElementMetadata.class,
                DeserializedConcreteElement.class,
                InstanceData.class,
                IntFunction.class,
                MetadataIndex.class,
                PropertyValues.class,
                ReferenceIdResolver.class,
                ReferenceIdResolvers.class,
                Supplier.class);
        if (qualifiedProperties.notEmpty())
        {
            additionalImports.add(ExecutionSupport.class);
        }
        StringBuilder builder = initClass(javaPackage, additionalImports, className, typeParams, superClass, interfaceNamePlusTypeParams, cls).append('\n');
        appendConcreteElementConstructor(builder, className).append('\n');
        appendCopyConstructor(builder, className, classNamePlusTypeParams).append('\n');
        appendStandardMethods(builder, simpleProperties, superClass).append('\n');
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, true).append('\n');
        appendSimpleProperties(builder, simpleProperties, interfaceNamePlusTypeParams, superClass).append('\n');
        appendQualifiedProperties(builder, qualifiedProperties).append('\n');
        appendConcreteInitialize(builder).append('\n');
        appendGetStateMethods(builder).append('\n');
        appendConcreteElementStateClass(builder, simpleProperties);
        builder.append("}\n");

        consumer.accept(javaPackage + '.' + className, builder.toString());
    }

    private void generateComponentInstanceImplementation(CoreInstance cls, ListIterable<PropertyInfo> simpleProperties, ListIterable<PropertyInfo> qualifiedProperties, BiConsumer<String, String> consumer)
    {
        String javaPackage = getJavaPackage(cls);
        String className = getLazyComponentInstanceClassName(cls);
        String classInterfaceName = M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(cls);
        Class<? extends AbstractLazyCoreInstance> superClass = AbstractM3GeneratedLazyComponentInstance.class;

        String typeParams = getTypeParameters(cls);
        String classNamePlusTypeParams = className + typeParams;
        String interfaceNamePlusTypeParams = classInterfaceName + typeParams;

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                InstanceData.class,
                IntFunction.class,
                PropertyValues.class,
                ReferenceIdResolver.class);
        if (simpleProperties.anySatisfy(PropertyInfo::isBackRef))
        {
            additionalImports.add(Supplier.class);
        }
        if (qualifiedProperties.notEmpty())
        {
            additionalImports.add(ExecutionSupport.class);
        }
        StringBuilder builder = initClass(javaPackage, additionalImports, className, typeParams, superClass, interfaceNamePlusTypeParams, cls).append('\n');
        appendComponentInstanceConstructor(builder, className).append('\n');
        appendCopyConstructor(builder, className, classNamePlusTypeParams).append('\n');

        appendStandardMethods(builder, simpleProperties, superClass).append('\n');
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, false);
        appendSimpleProperties(builder, simpleProperties, interfaceNamePlusTypeParams, superClass).append('\n');
        appendQualifiedProperties(builder, qualifiedProperties).append('\n');
        appendGetStateMethods(builder).append('\n');
        appendComponentInstanceStateClass(builder, simpleProperties);
        builder.append("}\n");
        consumer.accept(javaPackage + '.' + className, builder.toString());
    }

    private void generateEnumComponentInstanceImplementation(BiConsumer<String, String> consumer)
    {
        String javaPackage = getJavaPackage(this.enumClass);
        StringBuilder builder = new StringBuilder("package ").append(javaPackage).append(";\n\n");

        JavaTools.sortReduceAndPrintImports(builder,
                BackReference.class.getName(),
                CoreInstance.class.getName(),
                ElementBuilder.class.getName(),
                InstanceData.class.getName(),
                IntFunction.class.getName(),
                ListIterable.class.getName(),
                ModelRepository.class.getName(),
                PrimitiveValueResolver.class.getName(),
                ReferenceIdResolver.class.getName());
        builder.append('\n');

        builder.append("public class ").append(ENUM_COMPONENT_CLASS_NAME).append(" extends ").append(getLazyComponentInstanceClassName(this.enumClass)).append(" implements Comparable<").append(Enum.class.getSimpleName()).append(">\n");
        builder.append("{\n");
        builder.append("    private final String fullSystemPath;\n");
        builder.append("\n");
        builder.append("    public ").append(ENUM_COMPONENT_CLASS_NAME).append("(ModelRepository repository, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n");
        builder.append("    {\n");
        builder.append("        super(repository, instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);\n");
        builder.append("        this.fullSystemPath = \"Root::\" + instanceData.getClassifierPath();\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public ").append(ENUM_COMPONENT_CLASS_NAME).append("(").append(ENUM_COMPONENT_CLASS_NAME).append(" source)\n");
        builder.append("    {\n");
        builder.append("        super(source);\n");
        builder.append("        this.fullSystemPath = source.fullSystemPath;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public int compareTo(").append(Enum.class.getSimpleName()).append(" other)\n");
        builder.append("    {\n");
        builder.append("        return getName().compareTo(other.getName());\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String getFullSystemPath()\n");
        builder.append("    {\n");
        builder.append("         return this.fullSystemPath;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public ").append(Enum.class.getSimpleName()).append(" copy()\n");
        builder.append("    {\n");
        builder.append("        return new ").append(ENUM_COMPONENT_CLASS_NAME).append("(this);\n");
        builder.append("    }\n");
        builder.append("}\n");

        consumer.accept(javaPackage + '.' + ENUM_COMPONENT_CLASS_NAME, builder.toString());
    }

    private void generateVirtualPackageImplementation(CoreInstance cls, ListIterable<PropertyInfo> simpleProperties, ListIterable<PropertyInfo> qualifiedProperties, BiConsumer<String, String> consumer)
    {
        String javaPackage = getJavaPackage(cls);
        String className = getLazyVirtualPackageClassName(cls);
        String classInterfaceName = M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(cls);
        Class<? extends AbstractLazyCoreInstance> superClass = AbstractM3GeneratedLazyVirtualPackage.class;

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                BackReferenceProvider.class,
                MetadataIndex.class,
                ReferenceIdResolvers.class,
                Supplier.class,
                VirtualPackageMetadata.class);
        if (qualifiedProperties.notEmpty())
        {
            additionalImports.add(ExecutionSupport.class);
        }
        StringBuilder builder = initClass(javaPackage, additionalImports, className, "", superClass, classInterfaceName, cls).append('\n');
        appendVirtualPackageConstructor(builder, className).append('\n');
        appendCopyConstructor(builder, className, className).append('\n');
        appendStandardMethods(builder, simpleProperties, superClass);
        appendCopy(builder, className, classInterfaceName, true);
        appendSimpleProperties(builder, simpleProperties, classInterfaceName, superClass).append('\n');
        appendQualifiedProperties(builder, qualifiedProperties).append('\n');
        appendVirtualPackageInitialize(builder).append('\n');
        appendGetStateMethods(builder).append('\n');
        appendVirtualPackageStateClass(builder, simpleProperties);
        builder.append("}\n");
        consumer.accept(javaPackage + '.' + className, builder.toString());
    }

    private StringBuilder initClass(String javaPackage, RichIterable<? extends Class<?>> additionalImports, String className, String typeParams, Class<? extends AbstractLazyCoreInstance> superClass, String _interface, CoreInstance cls)
    {
        StringBuilder builder = new StringBuilder("package ").append(javaPackage).append(";\n\n");

        MutableList<String> imports = Lists.mutable.<String>empty()
                .with(superClass.getName())
                .with("org.finos.legend.pure.m3.coreinstance.helper.*")
                .with("org.finos.legend.pure.m4.coreinstance.primitive.*")
                .with(BackReference.class.getName())
                .with(CoreInstance.class.getName())
                .with(ElementBuilder.class.getName())
                .with(KeyIndex.class.getName())
                .with(ListIterable.class.getName())
                .with(Lists.class.getName())
                .with(ManyValues.class.getName())
                .with(ModelRepository.class.getName())
                .with(ModelRepositoryTransaction.class.getName())
                .with(MutableList.class.getName())
                .with(MutableMap.class.getName())
                .with(OneValue.class.getName())
                .with(PrimitiveHelper.class.getName())
                .with(PrimitiveValueResolver.class.getName())
                .with(PropertyValue.class.getName())
                .with(RichIterable.class.getName());
        additionalImports.collect(Class::getName, imports);

        if (imports.notEmpty())
        {
            JavaTools.sortReduceAndPrintImports(builder, imports).append('\n');
        }

        builder.append("public class ").append(className).append(typeParams)
                .append(" extends ").append(superClass.getSimpleName())
                .append(" implements ").append(_interface).append("\n{\n");
        PackageableElement.writeSystemPathForPackageableElement(builder.append("    private static final String ").append(FULL_SYSTEM_PATH_FIELD).append(" = \""), cls).append("\";\n");
        appendKeyIndex(builder, cls).append('\n');
        builder.append("    private volatile _State state;\n");
        return builder;
    }

    private StringBuilder appendKeyIndex(StringBuilder builder, CoreInstance cls)
    {
        MapIterable<String, CoreInstance> simplePropertiesByName = this.processorSupport.class_getSimplePropertiesByName(cls);
        builder.append("    private static final KeyIndex ").append(KEY_INDEX_FIELD).append(" = KeyIndex.builder(").append(simplePropertiesByName.size()).append(")\n");
        MutableMap<CoreInstance, MutableSet<String>> propertiesBySourceType = Maps.mutable.empty();
        simplePropertiesByName.forEachKeyValue((name, property) ->
        {
            CoreInstance sourceType = Property.getSourceType(property, this.processorSupport);
            propertiesBySourceType.getIfAbsentPut(sourceType, Sets.mutable::empty).add(name);
        });
        MutableList<Pair<String, Pair<MutableList<String>, MutableList<String>>>> list = Lists.mutable.ofInitialCapacity(propertiesBySourceType.size());
        propertiesBySourceType.forEachKeyValue((sourceType, propertyNames) ->
        {
            String sourceTypeExpression = (cls == sourceType) ? FULL_SYSTEM_PATH_FIELD : PackageableElement.writeSystemPathForPackageableElement(new StringBuilder("\""), sourceType).append('"').toString();
            MutableList<String> properties = sourceType.getValueForMetaPropertyToMany(M3Properties.properties).asLazy()
                    .collect(Property::getPropertyName)
                    .select(propertyNames::contains, Lists.mutable.empty());
            MutableList<String> propertiesFromAssociations = sourceType.getValueForMetaPropertyToMany(M3Properties.propertiesFromAssociations).asLazy()
                    .collect(Property::getPropertyName)
                    .select(propertyNames::contains, Lists.mutable.empty());
            if (properties.size() + propertiesFromAssociations.size() != propertyNames.size())
            {
                throw new RuntimeException("Error dividing keys for " + PackageableElement.getUserPathForPackageableElement(sourceType) + " between properties and propertiesFromAssociations: " + propertyNames.toSortedList());
            }
            list.add(Tuples.pair(sourceTypeExpression, Tuples.pair(properties, propertiesFromAssociations)));
        });
        list.sortThisBy(Pair::getOne).forEach(pair ->
        {
            String sourceTypeExpression = pair.getOne();
            MutableList<String> properties = pair.getTwo().getOne();
            MutableList<String> propertiesFromAssociations = pair.getTwo().getTwo();
            if (properties.size() == 1)
            {
                builder.append("           .withKey(").append(sourceTypeExpression).append(", \"").append(properties.get(0)).append("\")\n");
            }
            else if (properties.notEmpty())
            {
                builder.append("           .withKeys(").append(sourceTypeExpression);
                properties.sortThis().appendString(builder, ", \"", "\", \"", "\")\n");
            }
            if (propertiesFromAssociations.size() == 1)
            {
                builder.append("           .withKeyFromAssociation(").append(sourceTypeExpression).append(", \"").append(propertiesFromAssociations.get(0)).append("\")\n");
            }
            else if (propertiesFromAssociations.notEmpty())
            {
                builder.append("           .withKeysFromAssociation(").append(sourceTypeExpression);
                propertiesFromAssociations.sortThis().appendString(builder, ", \"", "\", \"", "\")\n");
            }
        });
        return builder.append("           .build();\n");
    }

    private StringBuilder appendStandardMethods(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, Class<? extends AbstractLazyCoreInstance> superClass)
    {
        boolean isConcreteElementOrVirtualPackage = isConcreteElement(superClass) || isVirtualPackage(superClass);
        appendGetKeys(builder).append('\n');
        appendGetRealKeyByName(builder).append('\n');
        appendGetFullSystemPath(builder).append('\n');
        appendCommit(builder).append('\n');
        return appendGetPropertyValue(builder, simpleProperties, isConcreteElementOrVirtualPackage);
    }

    private StringBuilder appendConcreteElementConstructor(StringBuilder builder, String className)
    {
        return builder
                .append("    public ").append(className).append("(ModelRepository repository, ConcreteElementMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)\n")
                .append("    {\n")
                .append("        super(repository, metadata, elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);\n")
                .append("        this.state = new _State(getName(), metadata.getPath(), index, referenceIds, primitiveValueResolver);\n")
                .append("    }\n");
    }

    private StringBuilder appendComponentInstanceConstructor(StringBuilder builder, String className)
    {
        return builder
                .append("    public ").append(className).append("(ModelRepository repository, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n")
                .append("    {\n")
                .append("        super(repository, instanceData, referenceIdResolver);\n")
                .append("        this.state = new _State(instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);\n")
                .append("    }\n");
    }

    private StringBuilder appendVirtualPackageConstructor(StringBuilder builder, String className)
    {
        return builder
                .append("    public ").append(className).append("(ModelRepository repository, VirtualPackageMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)\n")
                .append("    {\n")
                .append("        super(repository, metadata, elementBuilder, referenceIds, backRefProviderDeserializer);\n")
                .append("        this.state = new _State(getName(), metadata.getPath(), index, referenceIds, primitiveValueResolver);\n")
                .append("    }\n");
    }

    private StringBuilder appendCopyConstructor(StringBuilder builder, String className, String classNamePlusTypeParams)
    {
        builder.append("    public ").append(className).append('(').append(classNamePlusTypeParams).append(" source)\n");
        builder.append("    {\n");
        builder.append("        super(source);\n");
        builder.append("        this.state = new _State(source.state);\n");
        builder.append("    }\n");
        return builder;
    }

    private StringBuilder appendGetKeys(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    public RichIterable<String> getKeys()\n")
                .append("    {\n")
                .append("        return ").append(KEY_INDEX_FIELD).append(".getKeys();\n")
                .append("    }\n");
    }

    private StringBuilder appendGetRealKeyByName(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    public ListIterable<String> getRealKeyByName(String keyName)\n")
                .append("    {\n")
                .append("        return ").append(KEY_INDEX_FIELD).append(".getRealKeyByName(keyName);\n")
                .append("    }\n");
    }

    private StringBuilder appendGetFullSystemPath(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    public String getFullSystemPath()\n")
                .append("    {\n")
                .append("        return ").append(FULL_SYSTEM_PATH_FIELD).append(";\n")
                .append("    }\n");
    }

    private StringBuilder appendGetPropertyValue(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, boolean isConcreteElementOrVirtualPackage)
    {
        builder.append("    @Override\n")
                .append("    protected PropertyValue<?> getPropertyValue(String propertyName, boolean forWrite)\n")
                .append("    {\n");
        switch (simpleProperties.size())
        {
            case 0:
            {
                builder.append("        return null;\n");
                break;
            }
            case 1:
            {
                PropertyInfo propertyInfo = simpleProperties.get(0);
                if (isConcreteElementOrVirtualPackage)
                {
                    builder.append("        if (\"").append(propertyInfo.name).append("\".equals(propertyName))\n");
                    builder.append("        {\n");
                    if (propertyInfo.isPackageChildren() || M3Properties.name.equals(propertyInfo.name) || M3Properties._package.equals(propertyInfo.name))
                    {
                        builder.append("            if (forWrite)\n");
                        builder.append("            {\n");
                        builder.append("                initialize();\n");
                        builder.append("            }\n");
                    }
                    else
                    {
                        builder.append("            initialize();\n");
                    }
                    builder.append("            return getState(forWrite)._").append(propertyInfo.name).append(";\n");
                    builder.append("        }\n");
                    builder.append("        return null;\n");
                }
                else
                {
                    builder.append("        return \"").append(propertyInfo.name).append("\".equals(propertyName)) ? getState(forWrite)._").append(propertyInfo.name).append(" : null;\n");
                }
                break;
            }
            default:
            {
                builder.append("        switch (propertyName)\n");
                builder.append("        {\n");
                simpleProperties.forEach(propertyInfo ->
                {
                    builder.append("            case \"").append(propertyInfo.name).append("\":\n");
                    builder.append("            {\n");
                    if (isConcreteElementOrVirtualPackage)
                    {
                        if (propertyInfo.isPackageChildren() || M3Properties.name.equals(propertyInfo.name) || M3Properties._package.equals(propertyInfo.name))
                        {
                            builder.append("                if (forWrite)\n");
                            builder.append("                {\n");
                            builder.append("                    initialize();\n");
                            builder.append("                }\n");
                        }
                        else
                        {
                            builder.append("                initialize();\n");
                        }
                    }
                    builder.append("                return getState(forWrite)._").append(propertyInfo.name).append(";\n");
                    builder.append("            }\n");
                });
                builder.append("            default:\n");
                builder.append("            {\n");
                builder.append("                return null;\n");
                builder.append("            }\n");
                builder.append("        }\n");
            }
        }
        return builder.append("    }\n");
    }

    private StringBuilder appendCommit(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    public void commit(ModelRepositoryTransaction transaction)\n")
                .append("    {\n")
                .append("        this.state = (_State) transaction.getState(this);\n")
                .append("    }\n");
    }

    private StringBuilder appendSimpleProperties(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, String interfaceNamePlusTypeParams, Class<? extends AbstractLazyCoreInstance> superClass)
    {
        boolean isConcreteElementOrVirtualPackage = isConcreteElement(superClass) || isVirtualPackage(superClass);
        simpleProperties.forEach(propertyInfo -> appendSimpleProperty(builder.append('\n'), propertyInfo, interfaceNamePlusTypeParams, isConcreteElementOrVirtualPackage));
        return builder;
    }

    private void appendSimpleProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean isConcreteElementOrVirtualPackage)
    {
        boolean shouldInitForRead = isConcreteElementOrVirtualPackage && !propertyInfo.isPackageChildren() && !M3Properties.name.equals(propertyInfo.name) && !M3Properties._package.equals(propertyInfo.name);
        boolean shouldInitForWrite = isConcreteElementOrVirtualPackage;
        switch (propertyInfo.typeCategory)
        {
            case ANY:
            case NIL:
            {
                appendAnyOrNilProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
                break;
            }
            case TYPE_PARAM:
            {
                appendTypeParameterProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
                break;
            }
            case PRIMITIVE_TYPE:
            {
                appendPrimitiveProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
                break;
            }
            case ENUMERATION:
            {
                appendEnumProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
                break;
            }
            case STUB_TYPE:
            {
                appendStubProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
                break;
            }
            default:
            {
                appendOrdinaryProperty(builder, propertyInfo, interfaceNamePlusTypeParams, shouldInitForRead, shouldInitForWrite);
            }
        }
    }

    private void appendAnyOrNilProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        // There are no associations with Any or Nil, so we can assume the property is not from an association
        if (propertyInfo.isToOne())
        {
            builder.append("    public Object _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return AnyHelper.resolveAndUnwrap(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public CoreInstance _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(Object value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends Object> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return AnyHelper.resolveAndUnwrap(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public RichIterable<? extends CoreInstance> _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<?> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues(AnyHelper.wrapPrimitives(values, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(Object value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<?> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues(AnyHelper.wrapPrimitives(values, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(Object value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
        builder.append("        return this;\n");
        builder.append("    }\n");
    }

    private void appendTypeParameterProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        // There are no associations with Any or Nil, so we can assume the property is not from an association
        if (propertyInfo.isToOne())
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return (").append(propertyInfo.returnTypeJava).append(") AnyHelper.resolveAndUnwrap(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public CoreInstance _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append('(').append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return (RichIterable<? extends ").append(propertyInfo.returnTypeJava).append(">) AnyHelper.resolveAndUnwrap(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public RichIterable<? extends CoreInstance> _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues(AnyHelper.wrapPrimitives(values, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues(AnyHelper.wrapPrimitives(values, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(AnyHelper.wrapPrimitive(value, getRepository()));\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
        builder.append("        return this;\n");
        builder.append("    }\n");
    }

    private void appendPrimitiveProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        // There are no associations with primitive types, so we can assume the property is not from an association
        String primitiveTypeName = propertyInfo.resolvedRawType.getName();
        String lowerPrimitiveTypeName = TextTools.toLowerCase(primitiveTypeName, 0);
        if (propertyInfo.isToOne())
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return PrimitiveHelper.optionalInstanceTo").append(primitiveTypeName).append("(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(propertyInfo.holderTypeJava).append(" _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isRequired())
            {
                builder.append("        return mandatory(getState(false)._").append(propertyInfo.name).append(".getValue(), \"").append(propertyInfo.name).append("\");\n");
            }
            else
            {
                builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            }
            builder.append("    }\n");

            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(PrimitiveHelper.").append(lowerPrimitiveTypeName).append("ToCoreInstance(value, getRepository()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue((").append(propertyInfo.holderTypeJava).append(") value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return PrimitiveHelper.instancesTo").append(primitiveTypeName).append("(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public RichIterable<? extends ").append(propertyInfo.holderTypeJava).append("> _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(PrimitiveHelper.").append(lowerPrimitiveTypeName).append("sToCoreInstances(values, getRepository()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues((RichIterable<? extends ").append(propertyInfo.holderTypeJava).append(">) values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddCoreInstance(PrimitiveHelper.").append(lowerPrimitiveTypeName).append("ToCoreInstance(value, getRepository()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue((").append(propertyInfo.holderTypeJava).append(") value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddAllCoreInstance(PrimitiveHelper.").append(lowerPrimitiveTypeName).append("sToCoreInstances(values, getRepository()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAllCoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues((RichIterable<? extends ").append(propertyInfo.holderTypeJava).append(">) values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("RemoveCoreInstance(PrimitiveHelper.").append(lowerPrimitiveTypeName).append("ToCoreInstance(value, getRepository()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("RemoveCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
        builder.append("        return this;\n");
        builder.append("    }\n");
    }

    private void appendStubProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        // TODO handle properties from associations with stub types
        if (propertyInfo.isToOne())
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return ").append(propertyInfo.wrapperTypeJava).append(".to").append(propertyInfo.resolvedRawType.getName()).append("(AnyStubHelper.fromStub(_").append(propertyInfo.name).append("CoreInstance()));\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public CoreInstance _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return AnyStubHelper.fromStubsAndThen(_").append(propertyInfo.name).append("CoreInstance(), ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public RichIterable<? extends CoreInstance> _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(values);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues(values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddCoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddAllCoreInstance(values);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAllCoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues(values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("RemoveCoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("RemoveCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
        builder.append("        return this;\n");
        builder.append("    }\n");
        builder.append('\n');
        builder.append("    public void _reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(value);\n");
        builder.append("    }\n");
        builder.append('\n');
        builder.append("    public void _sever_reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
        builder.append("    }\n");
    }

    private void appendEnumProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        // There are no associations with enumerations, so we can assume the property is not from an association
        if (propertyInfo.isToOne())
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return EnumHelper.resolveToEnum(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public CoreInstance _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            builder.append("        return EnumHelper.resolveToEnums(_").append(propertyInfo.name).append("CoreInstance());\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public RichIterable<? extends CoreInstance> _").append(propertyInfo.name).append("CoreInstance()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("CoreInstance(values);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues(values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddCoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("AddAllCoreInstance(values);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAllCoreInstance(RichIterable<? extends CoreInstance> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues(values);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("RemoveCoreInstance(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("RemoveCoreInstance(CoreInstance value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
        builder.append("        return this;\n");
        builder.append("    }\n");
    }

    private void appendOrdinaryProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean shouldInitForRead, boolean shouldInitForWrite)
    {
        if (propertyInfo.isToOne())
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValue();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        OneValue<").append(propertyInfo.returnTypeJava).append("> currentPropertyValue = getState(true)._").append(propertyInfo.name).append(";\n");
                builder.append("        ").append(propertyInfo.returnTypeJava).append(" current = currentPropertyValue.getValue();\n");
                builder.append("        if (current != null)\n");
                builder.append("        {\n");
                builder.append("            current._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
                builder.append("        currentPropertyValue.setValue(value);\n");
                builder.append("        if (value != null)\n");
                builder.append("        {\n");
                builder.append("            value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            else
            {
                builder.append("        getState(true)._").append(propertyInfo.name).append(".setValue(value);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        OneValue<").append(propertyInfo.returnTypeJava).append("> currentPropertyValue = getState(true)._").append(propertyInfo.name).append(";\n");
                builder.append("        ").append(propertyInfo.returnTypeJava).append(" value = currentPropertyValue.getValue();\n");
                builder.append("        if (value != null)\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
                builder.append("        currentPropertyValue.removeAllValues();\n");
            }
            else
            {
                builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            if (shouldInitForRead)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return getState(false)._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        ManyValues<").append(propertyInfo.returnTypeJava).append("> currentPropertyValues = getState(true)._").append(propertyInfo.name).append(";\n");
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : currentPropertyValues.getValues())\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
                builder.append("        currentPropertyValues.setValues(values);\n");
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : currentPropertyValues.getValues())\n");
                builder.append("        {\n");
                builder.append("            value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            else
            {
                builder.append("        getState(true)._").append(propertyInfo.name).append(".setValues(values);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(value);\n");
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        getState(true)._").append(propertyInfo.name).append(".addValues(values);\n");
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : values)\n");
                builder.append("        {\n");
                builder.append("            value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        if (getState(true)._").append(propertyInfo.name).append(".removeValue(value))\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            else
            {
                builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
            builder.append("    {\n");
            if (shouldInitForWrite)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        ManyValues<").append(propertyInfo.returnTypeJava).append("> currentPropertyValues = getState(true)._").append(propertyInfo.name).append(";\n");
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : currentPropertyValues.getValues())\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
                builder.append("        currentPropertyValues.removeAllValues();\n");
            }
            else
            {
                builder.append("        getState(true)._").append(propertyInfo.name).append(".removeAllValues();\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public void _reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".addValue(value);\n");
        builder.append("    }\n");
        builder.append('\n');
        builder.append("    public void _sever_reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
        builder.append("    {\n");
        if (shouldInitForWrite)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        getState(true)._").append(propertyInfo.name).append(".removeValue(value);\n");
        builder.append("    }\n");
    }

    private StringBuilder appendQualifiedProperties(StringBuilder builder, ListIterable<PropertyInfo> qualifiedProperties)
    {
        if (qualifiedProperties.notEmpty())
        {
            qualifiedProperties.forEach(propertyInfo ->
            {
                builder.append("    public ");
                if (propertyInfo.isToOne())
                {
                    builder.append(propertyInfo.returnTypeJava);
                }
                else
                {
                    builder.append("RichIterable<? extends ").append(propertyInfo.returnTypeJava).append('>');
                }
                builder.append(' ').append(PrimitiveUtilities.getStringValue(propertyInfo.property.getValueForMetaPropertyToOne(M3Properties.functionName))).append('(');
                this.processorSupport.function_getFunctionType(propertyInfo.property).getValueForMetaPropertyToMany(M3Properties.parameters).asLazy().drop(1).forEach(param ->
                {
                    CoreInstance genericType = param.getValueForMetaPropertyToOne(M3Properties.genericType);
                    CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, this.processorSupport);
                    CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, this.processorSupport);
                    int multLow = Multiplicity.multiplicityLowerBoundToInt(multiplicity);
                    int multHigh = Multiplicity.multiplicityUpperBoundToInt(multiplicity);
                    String paramType = getReturnTypeJava(genericType, rawType, PropertyTypeCategory.ORDINARY_TYPE, multLow, multHigh, true);
                    builder.append(paramType).append(" _").append(PrimitiveUtilities.getStringValue(param.getValueForMetaPropertyToOne(M3Properties.name))).append(", ");
                });
                builder.append("ExecutionSupport es)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"This method is not supported on M3 classes\");\n");
                builder.append("    }\n\n");
            });
            builder.setLength(builder.length() - 1);
        }
        return builder;
    }

    private StringBuilder appendCopy(StringBuilder builder, String classNamePlusTypeParams, String interfaceNamePlusTypeParams, boolean isConcreteElementOrVirtualPackage)
    {
        builder.append("    @Override\n");
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" copy()\n");
        builder.append("    {\n");
        if (isConcreteElementOrVirtualPackage)
        {
            builder.append("        initialize();\n");
        }
        builder.append("        return new ").append(classNamePlusTypeParams).append("(this);\n");
        builder.append("    }\n");
        return builder;
    }

    private StringBuilder appendConcreteInitialize(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    protected void initialize(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n")
                .append("    {\n")
                .append("        _State current = this.state;\n")
                .append("        this.state = new _State(current, instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);\n")
                .append("    }\n");
    }

    private StringBuilder appendVirtualPackageInitialize(StringBuilder builder)
    {
        return builder
                .append("    @Override\n")
                .append("    protected void initialize(ListIterable<? extends BackReference> backReferences, ReferenceIdResolvers referenceIds, ElementBuilder elementBuilder)\n")
                .append("    {\n")
                .append("        _State current = this.state;\n")
                .append("        this.state = new _State(current, backReferences, referenceIds, elementBuilder);\n")
                .append("    }\n");
    }

    private StringBuilder appendGetStateMethods(StringBuilder builder)
    {
        return builder
                .append("    private _State getState(boolean forWrite)\n")
                .append("    {\n")
                .append("        ModelRepositoryTransaction transaction = this.repository.getTransaction();\n")
                .append("        if ((transaction != null) && transaction.isOpen())\n")
                .append("        {\n")
                .append("            if (forWrite && !transaction.isRegistered(this))\n")
                .append("            {\n")
                .append("                transaction.registerModified(this, new _State(this.state));\n")
                .append("            }\n")
                .append("            _State transactionState = (_State) transaction.getState(this);\n")
                .append("            if (transactionState != null)\n")
                .append("            {\n")
                .append("                return transactionState;\n")
                .append("            }\n")
                .append("        }\n")
                .append("        return this.state;\n")
                .append("    }\n");
    }

    private void appendComponentInstanceStateClass(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    private class _State extends _AbstractState\n");
        builder.append("    {\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("        private final ").append(propertyInfo.isToOne() ? "OneValue" : "ManyValues").append('<').append(propertyInfo.holderTypeJava).append("> _").append(propertyInfo.name).append(";\n"));
        builder.append('\n');
        builder.append("        private _State(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n");
        builder.append("        {\n");
        builder.append("            super(instanceData);\n");
        if (simpleProperties.notEmpty())
        {
            MutableSet<String> backRefProperties = Sets.mutable.empty();
            simpleProperties.forEach(propertyInfo ->
            {
                if (propertyInfo.isBackRef())
                {
                    backRefProperties.add(propertyInfo.name);
                    builder.append("            MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
                }
            });
            if (backRefProperties.notEmpty())
            {
                builder.append("            collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder");
                M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                        .sortThis()
                        .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
                builder.append(");\n");
            }
            builder.append("            MutableMap<String, PropertyValues> propertyValuesByName = indexPropertyValues(instanceData);\n");
            simpleProperties.forEach(propertyInfo ->
            {
                builder.append("            this._").append(propertyInfo.name).append(" = ")
                        .append(propertyInfo.isToOne() ? "newToOnePropertyValue" : "newToManyPropertyValue")
                        .append("(propertyValuesByName.get(\"").append(propertyInfo.name).append("\"), referenceIdResolver, internalIdResolver, primitiveValueResolver, true");
                if (propertyInfo.isBackRef())
                {
                    builder.append(", ").append(propertyInfo.name);
                }
                if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                {
                    builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                }
                builder.append(");\n");
            });
        }
        builder.append("        }\n");
        builder.append('\n');
        builder.append("        private _State(_State source)\n");
        builder.append("        {\n");
        builder.append("            super(source);\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("            this._").append(propertyInfo.name).append(" = source._").append(propertyInfo.name).append(".copy();\n"));
        builder.append("        }\n");
        builder.append("    }\n");
    }

    private void appendConcreteElementStateClass(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    protected class _State extends _AbstractState\n");
        builder.append("    {\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("        private final ").append(propertyInfo.isToOne() ? "OneValue" : "ManyValues").append('<').append(propertyInfo.holderTypeJava).append("> _").append(propertyInfo.name).append(";\n"));
        builder.append('\n');
        builder.append("        private _State(String name, String path, MetadataIndex index, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver)\n");
        builder.append("        {\n");
        simpleProperties.forEach(propertyInfo ->
        {
            if (propertyInfo.isPackageChildren())
            {
                builder.append("            this._children = computePackageChildren(path, index, referenceIds");
                if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                {
                    builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                }
                builder.append(");\n");
            }
            else if (M3Properties.name.equals(propertyInfo.name))
            {
                builder.append("            this._name = computeName(name, primitiveValueResolver);\n");
            }
            else if (M3Properties._package.equals(propertyInfo.name))
            {
                builder.append("            this._package = computePackage(path, referenceIds");
                if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                {
                    builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                }
                builder.append(");\n");
            }
            else
            {
                builder.append("            this._").append(propertyInfo.name).append(" = null;\n");
            }
        });
        builder.append("        }\n");
        builder.append('\n');
        builder.append("        private _State(_State init, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n");
        builder.append("        {\n");
        builder.append("            super(instanceData);\n");
        if (simpleProperties.notEmpty())
        {
            MutableSet<String> backRefProperties = Sets.mutable.empty();
            simpleProperties.forEach(propertyInfo ->
            {
                if (propertyInfo.isBackRef())
                {
                    backRefProperties.add(propertyInfo.name);
                    builder.append("            MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
                }
            });
            if (backRefProperties.notEmpty())
            {
                builder.append("            collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder");
                M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                        .sortThis()
                        .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
                builder.append(");\n");
            }
            builder.append("            MutableMap<String, PropertyValues> propertyValuesByName = indexPropertyValues(instanceData);\n");
            simpleProperties.forEach(propertyInfo ->
            {
                builder.append("            this._").append(propertyInfo.name).append(" = ");
                if (propertyInfo.isPackageChildren() || M3Properties.name.equals(propertyInfo.name) || M3Properties._package.equals(propertyInfo.name))
                {
                    builder.append("init._").append(propertyInfo.name);
                }
                else
                {
                    builder.append(propertyInfo.isToOne() ? "newToOnePropertyValue" : "newToManyPropertyValue")
                            .append("(propertyValuesByName.get(\"").append(propertyInfo.name).append("\"), referenceIdResolver, internalIdResolver, primitiveValueResolver, true");
                    if (propertyInfo.isBackRef())
                    {
                        builder.append(", ").append(propertyInfo.name);
                    }
                    if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                    {
                        builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                    }
                    builder.append(')');
                }
                builder.append(";\n");
            });
        }
        builder.append("        }\n");
        builder.append('\n');
        builder.append("        private _State(_State source)\n");
        builder.append("        {\n");
        builder.append("            super(source);\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("            this._").append(propertyInfo.name).append(" = source._").append(propertyInfo.name).append(".copy();\n"));
        builder.append("        }\n");
        builder.append("    }\n");
    }

    private void appendVirtualPackageStateClass(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    protected class _State extends _AbstractState\n");
        builder.append("    {\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("        private final ").append(propertyInfo.isToOne() ? "OneValue" : "ManyValues").append('<').append(propertyInfo.holderTypeJava).append("> _").append(propertyInfo.name).append(";\n"));
        builder.append('\n');
        builder.append("        private _State(String name, String path, MetadataIndex index, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver)\n");
        builder.append("        {\n");
        simpleProperties.forEach(propertyInfo ->
        {
            if (propertyInfo.isPackageChildren())
            {
                builder.append("            this._children = computePackageChildren(path, index, referenceIds");
                if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                {
                    builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                }
                builder.append(");\n");
            }
            else if (M3Properties.name.equals(propertyInfo.name))
            {
                builder.append("            this._name = computeName(name, primitiveValueResolver);\n");
            }
            else if (M3Properties._package.equals(propertyInfo.name))
            {
                builder.append("            this._package = computePackage(path, referenceIds");
                if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                {
                    builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                }
                builder.append(");\n");
            }
            else
            {
                builder.append("            this._").append(propertyInfo.name).append(" = null;\n");
            }
        });
        builder.append("        }\n");
        builder.append('\n');
        builder.append("        private _State(_State init, ListIterable<? extends BackReference> backReferences, ReferenceIdResolvers referenceIds, ElementBuilder elementBuilder)\n");
        builder.append("        {\n");
        builder.append("            super(init);\n");
        if (simpleProperties.notEmpty())
        {
            MutableSet<String> backRefProperties = Sets.mutable.empty();
            simpleProperties.forEach(propertyInfo ->
            {
                if (propertyInfo.isBackRef())
                {
                    backRefProperties.add(propertyInfo.name);
                    builder.append("            MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
                }
            });
            if (backRefProperties.notEmpty())
            {
                builder.append("            collectBackReferences(backReferences, referenceIds, elementBuilder");
                M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                        .sortThis()
                        .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
                builder.append(");\n");
            }
            simpleProperties.forEach(propertyInfo ->
            {
                builder.append("            this._").append(propertyInfo.name).append(" = ");
                if (propertyInfo.isPackageChildren() || M3Properties.name.equals(propertyInfo.name) || M3Properties._package.equals(propertyInfo.name))
                {
                    builder.append("init._").append(propertyInfo.name).append(";\n");
                }
                else if (propertyInfo.isBackRef())
                {
                    builder.append("ManyValues.fromSuppliers(").append(propertyInfo.name);
                    if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                    {
                        builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                    }
                    builder.append(");\n");
                }
                else
                {
                    builder.append(propertyInfo.isToOne() ? "OneValue.fromValue" : "ManyValues.fromValues").append("(null");
                    if (!"CoreInstance".equals(propertyInfo.holderTypeJava) && (propertyInfo.wrapperTypeJava != null))
                    {
                        builder.append(", ").append(propertyInfo.wrapperTypeJava).append(".FROM_CORE_INSTANCE_FN");
                    }
                    builder.append(");\n");
                }
            });
        }
        builder.append("        }\n");
        builder.append('\n');
        builder.append("        private _State(_State source)\n");
        builder.append("        {\n");
        builder.append("            super(source);\n");
        simpleProperties.forEach(propertyInfo ->
                builder.append("            this._").append(propertyInfo.name).append(" = source._").append(propertyInfo.name).append(".copy();\n"));
        builder.append("        }\n");
        builder.append("    }\n");
    }

    private String getTypeParameters(CoreInstance cls)
    {
        ListIterable<? extends CoreInstance> typeParams = cls.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        return typeParams.isEmpty() ?
               "" :
               typeParams.asLazy()
                       .collect(ci -> PrimitiveUtilities.getStringValue(ci.getValueForMetaPropertyToOne(M3Properties.name)))
                       .makeString("<", ",", ">");
    }

    private ListIterable<PropertyInfo> getSimplePropertiesSortedByName(CoreInstance cls)
    {
        return getSimplePropertiesSortedByName(buildClassGenericType(cls), cls);
    }

    private ListIterable<PropertyInfo> getSimplePropertiesSortedByName(CoreInstance classGenericType, CoreInstance cls)
    {
        MapIterable<String, CoreInstance> properties = this.processorSupport.class_getSimplePropertiesByName(cls);
        return getPropertiesSortedByName(properties, classGenericType, cls);
    }

    private ListIterable<PropertyInfo> getQualifiedPropertiesSortedByName(CoreInstance classGenericType, CoreInstance cls)
    {
        MapIterable<String, CoreInstance> properties = this.processorSupport.class_getQualifiedPropertiesByName(cls);
        return getPropertiesSortedByName(properties, classGenericType, cls);
    }

    private ListIterable<PropertyInfo> getPropertiesSortedByName(MapIterable<String, CoreInstance> properties, CoreInstance classGenericType, CoreInstance cls)
    {
        MutableList<PropertyInfo> result = Lists.mutable.ofInitialCapacity(properties.size());
        MutableMap<String, ImmutableList<String>> backRefProperties = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.groupByUniqueKey(ImmutableList::getLast, Maps.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()));
        properties.forEachKeyValue((name, property) ->
        {
            CoreInstance returnType = PropertyTypeHelper.getPropertyResolvedReturnType(classGenericType, property, this.processorSupport);
            CoreInstance returnRawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, this.processorSupport);
            PropertyTypeCategory typeCategory = getPropertyTypeCategory(property, returnRawType, cls);
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, this.processorSupport);
            int multLow = Multiplicity.multiplicityLowerBoundToInt(multiplicity);
            int multHigh = Multiplicity.multiplicityUpperBoundToInt(multiplicity);

            String holderTypeJava = getHolderTypeJava(returnType, returnRawType, typeCategory);
            String returnTypeJava = getReturnTypeJava(returnType, returnRawType, typeCategory, multLow, multHigh, true);
            String wrapperTypeJava = getWrapperTypeJava(returnRawType, typeCategory);
            CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, this.processorSupport);
            String reversePropertyName;
            if (this.processorSupport.instance_instanceOf(propertyOwner, M3Paths.Association))
            {
                ListIterable<? extends CoreInstance> associationProperties = propertyOwner.getValueForMetaPropertyToMany(M3Properties.properties);
                CoreInstance reverseProperty = associationProperties.get(property == associationProperties.get(0) ? 1 : 0);
                reversePropertyName = Property.getPropertyName(reverseProperty);
            }
            else
            {
                reversePropertyName = null;
            }
            PropertyCategory category = getPropertyCategory(name, property, propertyOwner, backRefProperties);
            result.add(new PropertyInfo(name, property, returnType, returnRawType, holderTypeJava, returnTypeJava, wrapperTypeJava, reversePropertyName, typeCategory, category, multLow, multHigh));
        });
        return result.sortThis();
    }

    private CoreInstance buildClassGenericType(CoreInstance cls)
    {
        CoreInstance genericType = Type.wrapGenericType(cls, null, this.processorSupport);
        CoreInstance sourceGT = cls.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, sourceGT.getValueForMetaPropertyToMany(M3Properties.typeArguments), this.processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.multiplicityArguments, sourceGT.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments), this.processorSupport);
        return genericType;
    }

    private PropertyTypeCategory getPropertyTypeCategory(CoreInstance property, CoreInstance returnRawType, CoreInstance _class)
    {
        if (returnRawType == null)
        {
            return PropertyTypeCategory.TYPE_PARAM;
        }
        if (returnRawType == this.anyClass)
        {
            return PropertyTypeCategory.ANY;
        }
        if (returnRawType == this.nilClass)
        {
            return PropertyTypeCategory.NIL;
        }
        if (returnRawType.getClassifier() == this.primitiveTypeClass)
        {
            return PropertyTypeCategory.PRIMITIVE_TYPE;
        }
        if (returnRawType.getClassifier() == this.enumerationClass)
        {
            return PropertyTypeCategory.ENUMERATION;
        }
        if (returnRawType.getClassifier() == this.functionTypeClass)
        {
            return PropertyTypeCategory.FUNCTION_TYPE;
        }
        if (isStubType(_class, property, returnRawType))
        {
            return PropertyTypeCategory.STUB_TYPE;
        }
        return PropertyTypeCategory.ORDINARY_TYPE;
    }

    private String getHolderTypeJava(CoreInstance returnType, CoreInstance returnRawType, PropertyTypeCategory typeCategory)
    {
        switch (typeCategory)
        {
            case ANY:
            case ENUMERATION:
            case FUNCTION_TYPE:
            case NIL:
            case STUB_TYPE:
            case TYPE_PARAM:
            {
                return CoreInstance.class.getSimpleName();
            }
            case PRIMITIVE_TYPE:
            {
                return getPrimitiveTypeCoreInstanceClass(returnRawType);
            }
            case ORDINARY_TYPE:
            {
                return appendTypeArgs(M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(new StringBuilder(128), returnRawType), returnType, true, true).toString();
            }
            default:
            {
                throw new RuntimeException(GenericType.print(new StringBuilder("Unhandled property type: "), returnType, true, this.processorSupport).append(" (category: ").append(typeCategory).append(")").toString());
            }
        }
    }

    private StringBuilder appendTypeArgs(StringBuilder builder, CoreInstance genericType, boolean addExtends, boolean useTypeParameterName)
    {
        ListIterable<? extends CoreInstance> typeArgs = genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        if (typeArgs.notEmpty())
        {
            builder.append('<');
            typeArgs.forEach(typeArg -> appendTypeArgJavaType(addExtends ? builder.append("? extends ") : builder, typeArg, useTypeParameterName).append(", "));
            builder.setLength(builder.length() - 2);
            builder.append('>');
        }
        return builder;
    }

    private StringBuilder appendTypeArgJavaType(StringBuilder builder, CoreInstance typeArg, boolean useTypeParameterName)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(typeArg, M3Properties.rawType, this.processorSupport);
        if (rawType == null)
        {
            return builder.append(useTypeParameterName ? GenericType.getTypeParameterName(typeArg) : CoreInstance.class.getSimpleName());
        }
        if ((rawType == this.anyClass) || (rawType == this.nilClass) || (rawType.getClassifier() == this.functionTypeClass))
        {
            return builder.append(Object.class.getName());
        }
        if (rawType.getClassifier() == this.primitiveTypeClass)
        {
            return builder.append(getPrimitiveTypeJavaType(rawType, false));
        }
        M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(builder, rawType);
        appendTypeArgs(builder, typeArg, false, useTypeParameterName);
        return builder;
    }

    private String getReturnTypeJava(CoreInstance returnType, CoreInstance returnRawType, PropertyTypeCategory typeCategory, int multLow, int multHigh, boolean useTypeParameterName)
    {
        return getReturnTypeJava(returnType, returnRawType, typeCategory, useTypeParameterName, (multLow == 1) && (multHigh == 1));
    }

    private String getReturnTypeJava(CoreInstance returnType, CoreInstance returnRawType, PropertyTypeCategory typeCategory, boolean useTypeParameterName, boolean primitiveIfPossible)
    {
        switch (typeCategory)
        {
            case ANY:
            case NIL:
            {
                return Object.class.getName();
            }
            case TYPE_PARAM:
            {
                return useTypeParameterName ? GenericType.getTypeParameterName(returnType) : CoreInstance.class.getSimpleName();
            }
            case FUNCTION_TYPE:
            {
                // TODO is this correct?
                return CoreInstance.class.getSimpleName();
            }
            case ENUMERATION:
            {
                return Enum.class.getName();
            }
            case PRIMITIVE_TYPE:
            {
                return getPrimitiveTypeJavaType(returnRawType, primitiveIfPossible);
            }
            case ORDINARY_TYPE:
            case STUB_TYPE:
            {
                return appendTypeArgs(M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(new StringBuilder(128), returnRawType), returnType, true, true).toString();
            }
            default:
            {
                throw new RuntimeException(GenericType.print(new StringBuilder("Unhandled property type: "), returnType, true, this.processorSupport).append(" (category: ").append(typeCategory).append(")").toString());
            }
        }
    }

    private String getWrapperTypeJava(CoreInstance returnRawType, PropertyTypeCategory typeCategory)
    {
        switch (typeCategory)
        {
            case PRIMITIVE_TYPE:
            case TYPE_PARAM:
            {
                return null;
            }
            case ENUMERATION:
            {
                return EnumCoreInstanceWrapper.class.getName();
            }
            case ANY:
            case FUNCTION_TYPE:
            case NIL:
            case ORDINARY_TYPE:
            case STUB_TYPE:
            {
                return M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(new StringBuilder(128), returnRawType).append("CoreInstanceWrapper").toString();
            }
            default:
            {
                throw new RuntimeException(PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Unhandled property type: "), returnRawType).append(" (category: ").append(typeCategory).append(")").toString());
            }
        }
    }

    private String getPrimitiveTypeCoreInstanceClass(CoreInstance primitiveType)
    {
        return getPrimitiveTypeCoreInstanceClass(getPrimitiveTypeName(primitiveType));
    }

    private String getPrimitiveTypeCoreInstanceClass(String primitiveType)
    {
        switch (primitiveType)
        {
            case M3Paths.Boolean:
            {
                return BooleanCoreInstance.class.getSimpleName();
            }
            case M3Paths.Byte:
            {
                return ByteCoreInstance.class.getSimpleName();
            }
            case M3Paths.Date:
            case M3Paths.DateTime:
            case M3Paths.LatestDate:
            case M3Paths.StrictDate:
            {
                return DateCoreInstance.class.getSimpleName();
            }
            case M3Paths.Decimal:
            {
                return DecimalCoreInstance.class.getSimpleName();
            }
            case M3Paths.Float:
            {
                return FloatCoreInstance.class.getSimpleName();
            }
            case M3Paths.Integer:
            {
                return IntegerCoreInstance.class.getSimpleName();
            }
            case M3Paths.StrictTime:
            {
                return StrictTimeCoreInstance.class.getSimpleName();
            }
            case M3Paths.String:
            {
                return StringCoreInstance.class.getSimpleName();
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
            }
        }
    }

    private String getPrimitiveTypeJavaType(CoreInstance primitiveType, boolean primitiveIfPossible)
    {
        return getPrimitiveTypeJavaType(getPrimitiveTypeName(primitiveType), primitiveIfPossible);
    }

    private String getPrimitiveTypeJavaType(String primitiveType, boolean primitiveIfPossible)
    {
        switch (primitiveType)
        {
            case M3Paths.Boolean:
            {
                return (primitiveIfPossible ? boolean.class : Boolean.class).getSimpleName();
            }
            case M3Paths.Byte:
            {
                return (primitiveIfPossible ? byte.class : Byte.class).getSimpleName();
            }
            case M3Paths.Date:
            {
                return PureDate.class.getName();
            }
            case M3Paths.DateTime:
            {
                return DateTime.class.getName();
            }
            case M3Paths.Decimal:
            {
                return BigDecimal.class.getName();
            }
            case M3Paths.Float:
            {
                return (primitiveIfPossible ? double.class : Double.class).getSimpleName();
            }
            case M3Paths.Integer:
            {
                return (primitiveIfPossible ? long.class : Long.class).getSimpleName();
            }
            case M3Paths.LatestDate:
            {
                return LatestDate.class.getName();
            }
            case M3Paths.StrictDate:
            {
                return StrictDate.class.getName();
            }
            case M3Paths.StrictTime:
            {
                return PureStrictTime.class.getName();
            }
            case M3Paths.String:
            {
                return String.class.getSimpleName();
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
            }
        }
    }

    private String getPrimitiveTypeName(CoreInstance primitiveType)
    {
        return Type.isExtendedPrimitiveType(primitiveType, this.processorSupport) ?
               Type.findPrimitiveTypeFromExtendedPrimitiveType(primitiveType, this.processorSupport).getName() :
               primitiveType.getName();
    }

    private PropertyCategory getPropertyCategory(String name, CoreInstance property, CoreInstance propertyOwner, MapIterable<String, ImmutableList<String>> backRefProperties)
    {
        if (M3Properties.children.equals(name) && (propertyOwner == this.packageClass))
        {
            return PropertyCategory.PACKAGE_CHILDREN;
        }

        ImmutableList<String> backRefRealKey = backRefProperties.get(name);
        if ((backRefRealKey != null) && backRefRealKey.equals(Property.calculatePropertyPath(property, this.processorSupport)))
        {
            return PropertyCategory.BACK_REF;
        }

        return PropertyCategory.ORDINARY;
    }

    private boolean isStubType(CoreInstance cls, CoreInstance property, CoreInstance propertyReturnType)
    {
        // TODO find a better way to do this
        if (propertyReturnType != null)
        {
            switch (propertyReturnType.getName())
            {
                case "AbstractProperty":
                case "Association":
                case "Class":
                case "ClassProjection":
                case "Csv":
                case "Database":
                case "Enumeration":
                case "Function":
                case "FunctionDefinition":
                case "Mapping":
                case "SetBasedStore":
                case "SetColumn":
                case "SetRelation":
                case "State":
                case "Store":
                case "TypeView":
                case "ValueTransformer":
                {
                    return true;
                }
                case "Enum":
                {
                    return cls != this.enumerationClass;
                }
                case "Property":
                {
                    // TODO should we also include AssociationProjection and ClassProjection here?
                    switch (cls.getName())
                    {
                        case "Association":
                        case "Class":
                        {
                            return false;
                        }
                        default:
                        {
                            return true;
                        }
                    }
                }
                case "Stereotype":
                {
                    return !"Profile".equals(cls.getName()) || !M3Properties.p_stereotypes.equals(property.getName());
                }
                case "Tag":
                {
                    return !"Profile".equals(cls.getName()) || !M3Properties.p_tags.equals(property.getName());
                }
                case "Type":
                {
                    return !"Generalization".equals(cls.getName()) || !M3Properties.specific.equals(property.getName());
                }
            }
        }
        return false;
    }

    static String buildLazyConcreteElementClassReferenceFromUserPath(String userPath)
    {
        return buildLazyClassReferenceFromUserPath(userPath, CLASS_LAZY_CONCRETE_SUFFIX);
    }

    static String buildLazyComponentInstanceClassReferenceFromUserPath(String userPath)
    {
        return buildLazyClassReferenceFromUserPath(userPath, CLASS_LAZY_COMPONENT_SUFFIX);
    }

    static String buildLazyVirtualPackageClassReference()
    {
        return ROOT_PACKAGE + ".Package" + CLASS_VIRTUAL_PACKAGE_SUFFIX;
    }

    static String buildLazyEnumClassReference()
    {
        StringBuilder builder = new StringBuilder(ROOT_PACKAGE.length() + M3Paths.Enum.length());
        appendLazyClassReferenceFromUserPath(builder, M3Paths.Enum, "");
        builder.setLength(builder.lastIndexOf(".") + 1);
        return builder.append(ENUM_COMPONENT_CLASS_NAME).toString();
    }

    private static String buildLazyClassReferenceFromUserPath(String userPath, String suffix)
    {
        return appendLazyClassReferenceFromUserPath(new StringBuilder(ROOT_PACKAGE.length() + userPath.length() + suffix.length()), userPath, suffix).toString();
    }

    private static StringBuilder appendLazyClassReferenceFromUserPath(StringBuilder builder, String userPath, String suffix)
    {
        builder.append(ROOT_PACKAGE);
        PackageableElement.forEachUserPathElement(userPath, p -> builder.append('.').append(JavaTools.makeValidJavaIdentifier(p)));
        return builder.append(suffix);
    }

    private static String getJavaPackage(CoreInstance cls)
    {
        CoreInstance pkg = cls.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg == null)
        {
            return ROOT_PACKAGE;
        }

        StringBuilder builder = new StringBuilder(ROOT_PACKAGE.length() + 32);
        PackageableElement.forEachPackagePathElement(pkg,
                p -> builder.append(ROOT_PACKAGE),
                p -> builder.append('.').append(JavaTools.makeValidJavaIdentifier(p.getName())));
        return builder.toString();
    }

    private static String getLazyConcreteElementClassName(CoreInstance cls)
    {
        return getLazyClassName(cls, CLASS_LAZY_CONCRETE_SUFFIX);
    }

    private static String getLazyVirtualPackageClassName(CoreInstance cls)
    {
        return getLazyClassName(cls, CLASS_VIRTUAL_PACKAGE_SUFFIX);
    }

    private static String getLazyComponentInstanceClassName(CoreInstance cls)
    {
        return getLazyClassName(cls, CLASS_LAZY_COMPONENT_SUFFIX);
    }

    private static String getLazyClassName(CoreInstance cls, String suffix)
    {
        return JavaTools.makeValidJavaIdentifier(cls.getName()) + suffix;
    }

    private static boolean isConcreteElement(Class<? extends AbstractLazyCoreInstance> superClass)
    {
        return AbstractLazyConcreteElement.class.isAssignableFrom(superClass);
    }

    private static boolean isVirtualPackage(Class<? extends AbstractLazyCoreInstance> superClass)
    {
        return AbstractLazyVirtualPackage.class.isAssignableFrom(superClass);
    }

    private static void writeClassToFile(Path directory, String className, String code)
    {
        String relativeFilePath = className.replace(".", directory.getFileSystem().getSeparator()) + JavaFileObject.Kind.SOURCE.extension;
        Path filePath = directory.resolve(relativeFilePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, code.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class PropertyInfo implements Comparable<PropertyInfo>
    {
        private final String name;
        private final CoreInstance property;
        private final CoreInstance resolvedType;
        private final CoreInstance resolvedRawType;
        private final String holderTypeJava;
        private final String returnTypeJava;
        private final String wrapperTypeJava;
        private final String reversePropertyName;
        private final PropertyTypeCategory typeCategory;
        private final PropertyCategory category;
        private final int multHigh;
        private final int multLow;

        private PropertyInfo(String name, CoreInstance property, CoreInstance resolvedType, CoreInstance resolvedRawType, String holderTypeJava, String returnTypeJava, String wrapperTypeJava, String reversePropertyName, PropertyTypeCategory typeCategory, PropertyCategory category, int multLow, int multHigh)
        {
            this.name = name;
            this.property = property;
            this.resolvedType = resolvedType;
            this.resolvedRawType = resolvedRawType;
            this.holderTypeJava = holderTypeJava;
            this.returnTypeJava = returnTypeJava;
            this.wrapperTypeJava = wrapperTypeJava;
            this.reversePropertyName = reversePropertyName;
            this.typeCategory = typeCategory;
            this.category = category;
            this.multLow = multLow;
            this.multHigh = multHigh;
        }

        @Override
        public int compareTo(PropertyInfo other)
        {
            return this.name.compareTo(other.name);
        }

        boolean isToOne()
        {
            return this.multHigh == 1;
        }

        boolean isRequired()
        {
            return this.multLow >= 1;
        }

        boolean isFromAssociation()
        {
            return this.reversePropertyName != null;
        }

        boolean isBackRef()
        {
            return this.category == PropertyCategory.BACK_REF;
        }

        boolean isPackageChildren()
        {
            return this.category == PropertyCategory.PACKAGE_CHILDREN;
        }
    }

    private enum PropertyCategory
    {
        ORDINARY, BACK_REF, PACKAGE_CHILDREN
    }

    private enum PropertyTypeCategory
    {
        TYPE_PARAM, ANY, NIL, PRIMITIVE_TYPE, ENUMERATION, STUB_TYPE, FUNCTION_TYPE, ORDINARY_TYPE
    }

    public static void generate(Path outputDirectory, Iterable<? extends String> sourceIds, String sourceIdPrefix, ProcessorSupport processorSupport)
    {
        Set<? extends String> sourceIdSet = (sourceIds == null) ? null : ((sourceIds instanceof Set) ? (Set<? extends String>) sourceIds : Sets.mutable.withAll(sourceIds));
        Predicate<String> sourceFilter = ((sourceIdSet == null) || sourceIdSet.isEmpty()) ?
                                         ((sourceIdPrefix == null) ? null : sourceId -> sourceId.startsWith(sourceIdPrefix)) :
                                         ((sourceIdPrefix == null) ? sourceIdSet::contains : sourceId -> sourceIdSet.contains(sourceId) || sourceId.startsWith(sourceIdPrefix));
        new M3LazyCoreInstanceGenerator(processorSupport).generateImplementations(sourceFilter, outputDirectory);
    }

    public static void main(String[] args) throws Exception
    {
        String outputDir = args[0];
        String sourceIdsList = args[1];
        String sourceIdPrefix = (args.length >= 3) ? args[2] : null;

        MutableSet<String> sourceIdsSet = (sourceIdsList == null) ? null : Sets.mutable.with(sourceIdsList.split("\\s*+,\\s*+")).without("");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        RichIterable<CodeRepository> repositories = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true)).build().getRepositories();
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(classLoader, repositories))).setTransactionalByDefault(false).build();

        runtime.loadAndCompileCore();
        runtime.loadAndCompileSystem();

        generate(Paths.get(outputDir), sourceIdsSet, sourceIdPrefix, runtime.getProcessorSupport());
    }
}
