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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class;

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
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyConcreteElement;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyCoreInstance;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyVirtualPackage;
import org.finos.legend.pure.m3.coreinstance.lazy.ManyValues;
import org.finos.legend.pure.m3.coreinstance.lazy.OneValue;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.lazy.PropertyValue;
import org.finos.legend.pure.m3.coreinstance.lazy.generator.M3LazyCoreInstanceGenerator;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.navigation.property.Property;
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
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.AbstractCompiledLazyComponentInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.AbstractCompiledLazyConcreteElement;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.AbstractCompiledLazyVirtualPackage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class ClassPeltImplProcessor
{
    public static final String CLASS_LAZY_CONCRETE_SUFFIX = "_LazyConcrete";
    public static final String CLASS_LAZY_COMPONENT_SUFFIX = "_LazyComponent";
    public static final String CLASS_VIRTUAL_PACKAGE_SUFFIX = "_LazyVirtual";

    public static final String CLASS_LAZY_CONCRETE_COMP_SUFFIX = CLASS_LAZY_CONCRETE_SUFFIX + "Comp";
    public static final String CLASS_LAZY_COMPONENT_COMP_SUFFIX = CLASS_LAZY_COMPONENT_SUFFIX + "Comp";

    static void buildImplementation(String javaPackage, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, Consumer<? super StringJavaSource> consumer)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        if (_class == processorSupport.type_BottomType())
        {
            // Nothing can instantiate Nil, so no need to generate implementations
            return;
        }
        ListIterable<PropertyInfo> simpleProperties = getSimplePropertiesSortedByName(classGenericType, _class, processorContext);
        MapIterable<String, CoreInstance> qualifiedProperties = processorSupport.class_getQualifiedPropertiesByName(_class);
        boolean requiresEquals = simpleProperties.anySatisfy(p -> p.equalityKey);
        if (_class == processorSupport.package_getByUserPath(M3Paths.Package))
        {
            // Special handling for Package, which can be virtual
            consumer.accept(buildConcreteElementImplementation(javaPackage, classGenericType, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
            consumer.accept(buildVirtualPackageImplementation(javaPackage, classGenericType, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
            return;
        }
        if (processorSupport.type_subTypeOf(_class, processorSupport.package_getByUserPath(M3Paths.PackageableElement)))
        {
            consumer.accept(buildConcreteElementImplementation(javaPackage, classGenericType, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
        }
        consumer.accept(buildComponentInstanceImplementation(javaPackage, classGenericType, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
    }

    static void buildCompImplementation(String javaPackage, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, Consumer<? super StringJavaSource> consumer)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        if (_class == processorSupport.type_BottomType())
        {
            // Nothing can instantiate Nil, so no need to generate implementations
            return;
        }
        ListIterable<PropertyInfo> simpleProperties = getSimplePropertiesSortedByName(classGenericType, _class, processorContext);
        MapIterable<String, CoreInstance> qualifiedProperties = processorSupport.class_getQualifiedPropertiesByName(_class);
        boolean requiresEquals = simpleProperties.anySatisfy(p -> p.equalityKey);
        if (processorSupport.type_subTypeOf(_class, processorSupport.package_getByUserPath(M3Paths.PackageableElement)))
        {
            consumer.accept(buildConcreteElementCompImplementation(javaPackage, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
        }
        consumer.accept(buildComponentInstanceCompImplementation(javaPackage, _class, simpleProperties, qualifiedProperties, requiresEquals, processorContext, processorSupport));
    }

    private static StringJavaSource buildConcreteElementImplementation(String javaPackage, CoreInstance classGenericType, CoreInstance _class, ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String classInterfaceName = TypeProcessor.javaInterfaceForType(_class, processorSupport);
        String className = JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(_class, processorSupport);
        Class<AbstractCompiledLazyConcreteElement> superClass = AbstractCompiledLazyConcreteElement.class;

        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : ("<" + typeParams + ">");
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = classInterfaceName + typeParamsString;

        processorContext.setClassImplSuffix(CLASS_LAZY_CONCRETE_SUFFIX);

        ImmutableList<Class<?>> additionalImports = Lists.immutable.with(
                BackReference.class,
                BackReferenceProvider.class,
                ConcreteElementMetadata.class,
                DeserializedConcreteElement.class,
                ElementBuilder.class,
                InstanceData.class,
                IntFunction.class,
                MetadataIndex.class,
                PrimitiveValueResolver.class,
                PropertyValues.class,
                ReferenceIdResolvers.class,
                ReferenceIdResolver.class,
                Supplier.class);
        StringBuilder builder = initClass(javaPackage, additionalImports, qualifiedProperties.notEmpty(), classNamePlusTypeParams, superClass, interfaceNamePlusTypeParams, _class, processorContext, processorSupport).append('\n');
        appendSimplePropertyFields(builder, simpleProperties, superClass).append('\n');
        appendConcreteElementConstructor(builder, className, simpleProperties).append('\n');
        appendCopyConstructor(builder, className, classNamePlusTypeParams, simpleProperties, superClass).append('\n');
        appendStandardMethods(builder, simpleProperties, superClass).append('\n');
        if (requiresEquals)
        {
            appendEquals(builder, simpleProperties, classInterfaceName).append('\n');
        }
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, true);
        appendSimpleProperties(builder, simpleProperties, interfaceNamePlusTypeParams, superClass, processorContext).append('\n');
        if (qualifiedProperties.notEmpty())
        {
            ClassImplProcessor.appendQualifiedProperties(builder, _class, qualifiedProperties, processorContext);
        }
        appendValidate(builder, classGenericType, _class, interfaceNamePlusTypeParams, simpleProperties, processorContext);
        appendDefaultValues(builder, simpleProperties);
        appendConcreteInitialize(builder, simpleProperties);
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.append("}\n").toString());
    }

    private static StringJavaSource buildConcreteElementCompImplementation(String javaPackage, CoreInstance _class, ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String className = JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(_class, processorSupport);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : ("<" + typeParams + ">");
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = TypeProcessor.javaInterfaceForType(_class, processorSupport) + typeParamsString;
        String superClassName = M3LazyCoreInstanceGenerator.buildLazyConcreteElementClassReferenceFromUserPath(PackageableElement.getUserPathForPackageableElement(_class)) + typeParamsString;

        processorContext.setClassImplSuffix(CLASS_LAZY_CONCRETE_COMP_SUFFIX);
        ImmutableList<Class<?>> additionalImports = Lists.immutable.with(
                BackReferenceProvider.class,
                ConcreteElementMetadata.class,
                DeserializedConcreteElement.class,
                MetadataIndex.class,
                ReferenceIdResolvers.class,
                Supplier.class);
        StringBuilder builder = initCompClass(javaPackage, additionalImports, qualifiedProperties, requiresEquals, classNamePlusTypeParams, superClassName).append('\n');
        appendConcreteElementCompConstructor(builder, className).append('\n');
        appendCompCopyConstructor(builder, className, classNamePlusTypeParams).append('\n');
        if (requiresEquals)
        {
            appendEquals(builder, simpleProperties, TypeProcessor.javaInterfaceForType(_class, processorSupport)).append('\n');
        }
        if (qualifiedProperties.notEmpty())
        {
            ClassImplProcessor.appendQualifiedProperties(builder, _class, qualifiedProperties, processorContext);
        }
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, true);
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.append("}\n").toString());
    }

    private static StringJavaSource buildComponentInstanceImplementation(String javaPackage, CoreInstance classGenericType, CoreInstance _class, ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String classInterfaceName = TypeProcessor.javaInterfaceForType(_class, processorSupport);
        String className = JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(_class, processorSupport);
        Class<AbstractCompiledLazyComponentInstance> superClass = AbstractCompiledLazyComponentInstance.class;

        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : ("<" + typeParams + ">");
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = classInterfaceName + typeParamsString;

        processorContext.setClassImplSuffix(CLASS_LAZY_COMPONENT_SUFFIX);

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                BackReference.class,
                ElementBuilder.class,
                InstanceData.class,
                IntFunction.class,
                PrimitiveValueResolver.class,
                PropertyValues.class,
                ReferenceIdResolvers.class,
                ReferenceIdResolver.class);
        if (simpleProperties.anySatisfy(PropertyInfo::isBackRef))
        {
            additionalImports.add(Supplier.class);
        }
        StringBuilder builder = initClass(javaPackage, additionalImports, qualifiedProperties.notEmpty(), classNamePlusTypeParams, superClass, interfaceNamePlusTypeParams, _class, processorContext, processorSupport).append('\n');
        appendSimplePropertyFields(builder, simpleProperties, superClass).append('\n');
        appendComponentInstanceConstructor(builder, className, simpleProperties).append('\n');
        appendCopyConstructor(builder, className, classNamePlusTypeParams, simpleProperties, superClass).append('\n');

        appendStandardMethods(builder, simpleProperties, superClass).append('\n');
        if (requiresEquals)
        {
            appendEquals(builder, simpleProperties, classInterfaceName).append('\n');
        }
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, false);
        appendSimpleProperties(builder, simpleProperties, interfaceNamePlusTypeParams, superClass, processorContext).append('\n');
        if (qualifiedProperties.notEmpty())
        {
            ClassImplProcessor.appendQualifiedProperties(builder, _class, qualifiedProperties, processorContext);
        }
        appendValidate(builder, classGenericType, _class, interfaceNamePlusTypeParams, simpleProperties, processorContext);
        appendDefaultValues(builder, simpleProperties);
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.append("}\n").toString());
    }

    private static StringJavaSource buildComponentInstanceCompImplementation(String javaPackage, CoreInstance _class, ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String className = JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(_class, processorSupport);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : ("<" + typeParams + ">");
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = TypeProcessor.javaInterfaceForType(_class, processorSupport) + typeParamsString;
        String superClassName = M3LazyCoreInstanceGenerator.buildLazyComponentInstanceClassReferenceFromUserPath(PackageableElement.getUserPathForPackageableElement(_class)) + typeParamsString;

        processorContext.setClassImplSuffix(CLASS_LAZY_COMPONENT_COMP_SUFFIX);

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                BackReference.class,
                CoreInstance.class,
                InstanceData.class,
                IntFunction.class,
                ListIterable.class,
                ReferenceIdResolver.class);
        StringBuilder builder = initCompClass(javaPackage, additionalImports, qualifiedProperties, requiresEquals, classNamePlusTypeParams, superClassName).append('\n');
        appendComponentInstanceCompConstructor(builder, className).append('\n');
        appendCompCopyConstructor(builder, className, classNamePlusTypeParams).append('\n');
        if (requiresEquals)
        {
            appendEquals(builder, simpleProperties, TypeProcessor.javaInterfaceForType(_class, processorSupport)).append('\n');
        }
        if (qualifiedProperties.notEmpty())
        {
            ClassImplProcessor.appendQualifiedProperties(builder, _class, qualifiedProperties, processorContext);
        }
        appendCopy(builder, classNamePlusTypeParams, interfaceNamePlusTypeParams, false);
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.append("}\n").toString());
    }

    private static StringJavaSource buildVirtualPackageImplementation(String javaPackage, CoreInstance classGenericType, CoreInstance _class, ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String classInterfaceName = TypeProcessor.javaInterfaceForType(_class, processorSupport);
        String className = JavaPackageAndImportBuilder.buildLazyVirtualPackageClassName();
        Class<AbstractCompiledLazyVirtualPackage> superClass = AbstractCompiledLazyVirtualPackage.class;

        processorContext.setClassImplSuffix(CLASS_VIRTUAL_PACKAGE_SUFFIX);

        MutableList<Class<?>> additionalImports = Lists.mutable.with(
                BackReference.class,
                BackReferenceProvider.class,
                ElementBuilder.class,
                MetadataIndex.class,
                ReferenceIdResolvers.class,
                Supplier.class,
                VirtualPackageMetadata.class);
        StringBuilder builder = initClass(javaPackage, additionalImports, qualifiedProperties.notEmpty(), className, superClass, classInterfaceName, _class, processorContext, processorSupport).append('\n');
        appendSimplePropertyFields(builder, simpleProperties, superClass).append('\n');
        appendVirtualPackageConstructor(builder, className).append('\n');
        appendCopyConstructor(builder, className, className, simpleProperties, superClass).append('\n');
        appendStandardMethods(builder, simpleProperties, superClass);
        if (requiresEquals)
        {
            appendEquals(builder, simpleProperties, classInterfaceName).append('\n');
        }
        appendCopy(builder, className, classInterfaceName, true);
        appendSimpleProperties(builder, simpleProperties, classInterfaceName, superClass, processorContext).append('\n');
        if (qualifiedProperties.notEmpty())
        {
            ClassImplProcessor.appendQualifiedProperties(builder, _class, qualifiedProperties, processorContext);
        }
        appendValidate(builder, classGenericType, _class, classInterfaceName, simpleProperties, processorContext);
        appendDefaultValues(builder, simpleProperties);
        appendVirtualPackageInitialize(builder, simpleProperties);
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.append("}\n").toString());
    }

    private static StringBuilder initClass(String javaPackage, RichIterable<? extends Class<?>> additionalImports, boolean hasQualifiedProperties, String className, Class<? extends AbstractLazyCoreInstance> superClass, String _interface, CoreInstance _class, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        StringBuilder builder = new StringBuilder("package ").append(javaPackage).append(";\n\n");

        MutableList<String> imports = ClassImplProcessor.IMPORTS_LIST.toList()
                .with(superClass.getName())
                .with(ManyValues.class.getName())
                .with(OneValue.class.getName())
                .with(ModelRepository.class.getName())
                .with(PropertyValue.class.getName())
                .with(SourceInformation.class.getName())
                .with(IndexSpecification.class.getName())
                .with(IDConflictException.class.getName())
                .with(ConsoleCompiled.class.getName());
        if (hasQualifiedProperties)
        {
            imports.addAll(ClassImplProcessor.FUNCTION_IMPORTS_LIST.castToList());
        }
        additionalImports.collect(Class::getName, imports);

        if (imports.notEmpty())
        {
            JavaTools.sortReduceAndPrintImports(builder, imports).append('\n');
        }

        builder.append("public class ").append(className)
                .append(" extends ").append(superClass.getSimpleName())
                .append(" implements ").append(_interface).append("\n{\n");
        ClassImplProcessor.appendTempTypeInfo(builder, _class);
        ClassImplProcessor.appendKeyIndex(builder, _class, processorSupport);
        return ClassImplProcessor.appendTypeVariables(builder, _class, className, processorSupport, processorContext);
    }

    private static StringBuilder initCompClass(String javaPackage, RichIterable<? extends Class<?>> additionalImports, MapIterable<String, CoreInstance> qualifiedProperties, boolean requiresEquals, String className, String superClassName)
    {
        StringBuilder builder = new StringBuilder("package ").append(javaPackage).append(";\n\n");
        MutableList<String> imports = ClassImplProcessor.IMPORTS_LIST.toList()
                .with(ElementBuilder.class.getName())
                .with(ModelRepository.class.getName())
                .with(PrimitiveValueResolver.class.getName());
        boolean implementsJavaCompiledCoreInstance = requiresEquals || qualifiedProperties.containsKey("toString");
        if (implementsJavaCompiledCoreInstance)
        {
            imports.add(JavaCompiledCoreInstance.class.getName());
        }
        if (qualifiedProperties.notEmpty())
        {
            imports.addAll(ClassImplProcessor.FUNCTION_IMPORTS_LIST.castToList());
        }
        additionalImports.collect(Class::getName, imports);
        if (imports.notEmpty())
        {
            JavaTools.sortReduceAndPrintImports(builder, imports).append('\n');
        }

        builder.append("public class ").append(className).append(" extends ").append(superClassName);
        if (implementsJavaCompiledCoreInstance)
        {
            builder.append(" implements ").append(JavaCompiledCoreInstance.class.getSimpleName());
        }
        builder.append("\n{\n");
        return builder;
    }

    private static StringBuilder appendStandardMethods(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, Class<? extends AbstractLazyCoreInstance> superClass)
    {
        boolean isConcreteElementOrVirtualPackage = isConcreteElement(superClass) || isVirtualPackage(superClass);
        builder.append(ClassImplProcessor.buildGetKeys());
        builder.append(ClassImplProcessor.buildGetRealGetKeyByName());
        builder.append(ClassImplProcessor.buildGetFullSystemPath()).append('\n');
        if (isConcreteElementOrVirtualPackage)
        {
            appendRemovePropertyForConcreteElementOrVirtualPackage(builder).append('\n');
            appendGetValueForMetaPropertyToOneForConcreteElementOrVirtualPackage(builder).append('\n');
            appendGetValueForMetaPropertyToManyForConcreteElementOrVirtualPackage(builder).append('\n');
            appendGetValueByIDIndexForConcreteElementOrVirtualPackage(builder).append('\n');
            appendGetValuesByIndexForConcreteElementOrVirtualPackage(builder).append('\n');
            appendIsValueDefinedForKeyForConcreteElementOrVirtualPackage(builder).append('\n');
            appendRemoveValueForMetaPropertyToManyForConcreteElementOrVirtualPackage(builder).append('\n');
            appendModifyValueForToManyMetaPropertyForConcreteElementOrVirtualPackage(builder).append('\n');
            appendSetKeyValuesForConcreteElementOrVirtualPackage(builder).append('\n');
            appendAddKeyValueForConcreteElementOrVirtualPackage(builder).append('\n');
            appendIsFullyResolvedForConcreteElementOrVirtualPackage(builder).append('\n');
        }
        return appendGetPropertyValue(builder, simpleProperties, isConcreteElementOrVirtualPackage);
    }

    private static StringBuilder appendConcreteElementConstructor(StringBuilder builder, String className, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    public ").append(className).append("(ModelRepository repository, ConcreteElementMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)\n");
        builder.append("    {\n");
        builder.append("        super(repository, metadata, elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);\n");
        builder.append("        this._package = computePackage(metadata.getPath(), referenceIds);\n");
        if (simpleProperties.anySatisfy(PropertyInfo::isPackageChildren))
        {
            builder.append("        this._children = computePackageChildren(metadata.getPath(), index, referenceIds);\n");
        }
        builder.append("    }\n");
        return builder;
    }

    private static StringBuilder appendConcreteElementCompConstructor(StringBuilder builder, String className)
    {
        return builder.append("    public ").append(className).append("(ModelRepository repository, ConcreteElementMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)\n")
                .append("    {\n")
                .append("        super(repository, metadata, index, elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);\n")
                .append("    }\n");
    }

    private static StringBuilder appendComponentInstanceConstructor(StringBuilder builder, String className, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    public ").append(className).append("(ModelRepository repository, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n")
                .append("    {\n")
                .append("        super(repository, instanceData, referenceIdResolver);\n");
        if (simpleProperties.notEmpty())
        {
            MutableSet<String> backRefProperties = Sets.mutable.empty();
            simpleProperties.forEach(propertyInfo ->
            {
                if (propertyInfo.isBackRef())
                {
                    backRefProperties.add(propertyInfo.name);
                    builder.append("        MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
                }
            });
            if (backRefProperties.notEmpty())
            {
                builder.append("        collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder");
                M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                        .sortThis()
                        .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
                builder.append(");\n");
            }
            builder.append("        MutableMap<String, PropertyValues> propertyValuesByName = indexPropertyValues(instanceData);\n");
            simpleProperties.forEach(propertyInfo ->
            {
                builder.append("        this._").append(propertyInfo.name).append(" = ")
                        .append(propertyInfo.toOne ? "newToOnePropertyValue" : "newToManyPropertyValue")
                        .append("(propertyValuesByName.get(\"").append(propertyInfo.name).append("\"), referenceIdResolver, internalIdResolver, primitiveValueResolver, true");
                if (propertyInfo.isBackRef())
                {
                    builder.append(", ").append(propertyInfo.name);
                }
                if (propertyInfo.primitiveType)
                {
                    builder.append(", toCoreInstanceFn(), ").append(AbstractCompiledLazyComponentInstance.class.getSimpleName()).append(".<").append(propertyInfo.holderTypeJava).append(">fromValCoreInstanceFn()");
                }
                else if (propertyInfo.anyOrNilType)
                {
                    builder.append(", toCoreInstanceFn(), ").append(AbstractCompiledLazyComponentInstance.class.getSimpleName()).append(".<").append(propertyInfo.holderTypeJava).append(">fromCoreInstanceFn()");
                }
                builder.append(");\n");
            });
        }
        return builder.append("    }\n");
    }

    private static StringBuilder appendComponentInstanceCompConstructor(StringBuilder builder, String className)
    {
        return builder.append("    public ").append(className).append("(ModelRepository repository, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n")
                .append("    {\n")
                .append("        super(repository, instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);\n")
                .append("    }\n");
    }

    private static StringBuilder appendVirtualPackageConstructor(StringBuilder builder, String className)
    {
        builder.append("    public ").append(className).append("(ModelRepository repository, VirtualPackageMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)\n");
        builder.append("    {\n");
        builder.append("        super(repository, metadata, elementBuilder, referenceIds, backRefProviderDeserializer);\n");
        builder.append("        this._package = computePackage(metadata.getPath(), referenceIds);\n");
        builder.append("        this._children = computePackageChildren(metadata.getPath(), index, referenceIds);\n");
        builder.append("    }\n");
        return builder;
    }

    private static StringBuilder appendCopyConstructor(StringBuilder builder, String className, String classNamePlusTypeParams, ListIterable<PropertyInfo> simpleProperties, Class<? extends AbstractLazyCoreInstance> superClass)
    {
        builder.append("    public ").append(className).append('(').append(classNamePlusTypeParams).append(" source)\n");
        builder.append("    {\n");
        builder.append("        super(source);\n");
        boolean isPackageableElement = isConcreteElement(superClass) || isVirtualPackage(superClass);
        simpleProperties.forEach(propertyInfo ->
        {
            if (!isPackageableElement || !M3Properties.name.equals(propertyInfo.name))
            {
                builder.append("        this._").append(propertyInfo.name).append(" = source._").append(propertyInfo.name).append(".copy();\n");
            }
        });
        return builder.append("    }\n");
    }

    private static StringBuilder appendCompCopyConstructor(StringBuilder builder, String className, String classNamePlusTypeParams)
    {
        return builder.append("    public ").append(className).append('(').append(classNamePlusTypeParams).append(" source)\n")
                .append("    {\n")
                .append("        super(source);\n")
                .append("    }\n");
    }

    private static StringBuilder appendRemovePropertyForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public void removeProperty(String propertyNameKey)\n")
                .append("    {\n")
                .append("        switch (propertyNameKey)\n")
                .append("        {\n")
                .append("            case \"").append(M3Properties.name).append("\":\n")
                .append("            {\n")
                .append("                _").append(M3Properties.name).append("Remove();\n")
                .append("                break;\n")
                .append("            }\n")
                .append("            case \"").append(M3Properties._package).append("\":\n")
                .append("            {\n")
                .append("                _").append(M3Properties._package).append("Remove();\n")
                .append("                break;\n")
                .append("            }\n")
                .append("            default:\n")
                .append("            {\n")
                .append("                super.removeProperty(propertyNameKey);\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n");
    }

    private static StringBuilder appendGetValueForMetaPropertyToOneForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public CoreInstance getValueForMetaPropertyToOne(String propertyName)\n")
                .append("    {\n")
                .append("        return \"").append(M3Properties.name).append("\".equals(propertyName) ? ValCoreInstance.toCoreInstance(this.name) : super.getValueForMetaPropertyToOne(propertyName);\n")
                .append("    }\n");
    }

    private static StringBuilder appendGetValueForMetaPropertyToManyForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)\n")
                .append("    {\n")
                .append("        return \"").append(M3Properties.name).append("\".equals(keyName) ? Lists.immutable.with(ValCoreInstance.toCoreInstance(this.name)) : super.getValueForMetaPropertyToMany(keyName);\n")
                .append("    }\n");
    }

    private static StringBuilder appendGetValueByIDIndexForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    protected <K> CoreInstance getValueByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex) throws IDConflictException\n")
                .append("    {\n")
                .append("        if (\"").append(M3Properties.name).append("\".equals(keyName))\n")
                .append("        {\n")
                .append("            CoreInstance nameCI = ValCoreInstance.toCoreInstance(this.name);\n")
                .append("            return keyInIndex.equals(indexSpec.getIndexKey(nameCI)) ? nameCI : null;\n")
                .append("        }\n")
                .append("        return super.getValueByIDIndex(keyName, indexSpec, keyInIndex);\n")
                .append("    }\n");
    }

    private static StringBuilder appendGetValuesByIndexForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)\n")
                .append("    {\n")
                .append("        if (\"").append(M3Properties.name).append("\".equals(keyName))\n")
                .append("        {\n")
                .append("            CoreInstance nameCI = ValCoreInstance.toCoreInstance(this.name);\n")
                .append("            return keyInIndex.equals(indexSpec.getIndexKey(nameCI)) ? Lists.immutable.with(nameCI) : Lists.immutable.<CoreInstance>empty();\n")
                .append("        }\n")
                .append("        return super.getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);\n")
                .append("    }\n");
    }

    private static StringBuilder appendIsValueDefinedForKeyForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public boolean isValueDefinedForKey(String keyName)\n")
                .append("    {\n")
                .append("        return \"" + M3Properties.name + "\".equals(keyName) || super.isValueDefinedForKey(keyName);\n")
                .append("    }\n");
    }

    private static StringBuilder appendRemoveValueForMetaPropertyToManyForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)\n")
                .append("    {\n")
                .append("        switch (keyName)\n")
                .append("        {\n")
                .append("            case \"").append(M3Properties.name).append("\":\n")
                .append("            case \"").append(M3Properties._package).append("\":\n")
                .append("            {\n")
                .append("                throw new UnsupportedOperationException(\"Cannot remove value for '\" + keyName + \"'\");\n")
                .append("            }\n")
                .append("            default:\n")
                .append("            {\n")
                .append("                super.removeValueForMetaPropertyToMany(keyName, coreInstance);\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n");
    }

    private static StringBuilder appendModifyValueForToManyMetaPropertyForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)\n")
                .append("    {\n")
                .append("        switch (key)\n")
                .append("        {\n")
                .append("            case \"").append(M3Properties.name).append("\":\n")
                .append("            case \"").append(M3Properties._package).append("\":\n")
                .append("            {\n")
                .append("                throw new UnsupportedOperationException(\"Cannot modify value for '\" + key + \"'\");\n")
                .append("            }\n")
                .append("            default:\n")
                .append("            {\n")
                .append("                super.modifyValueForToManyMetaProperty(key, offset, value);\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n");
    }

    private static StringBuilder appendSetKeyValuesForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)\n")
                .append("    {\n")
                .append("        String keyName = key.getLast();\n")
                .append("        switch (keyName)\n")
                .append("        {\n")
                .append("            case \"").append(M3Properties.name).append("\":\n")
                .append("            case \"").append(M3Properties._package).append("\":\n")
                .append("            {\n")
                .append("                throw new UnsupportedOperationException(\"Cannot set value for '\" + keyName + \"'\");\n")
                .append("            }\n")
                .append("            default:\n")
                .append("            {\n")
                .append("                super.setKeyValues(key, value);\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n");
    }

    private static StringBuilder appendAddKeyValueForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public void addKeyValue(ListIterable<String> key, CoreInstance value)\n")
                .append("    {\n")
                .append("        String keyName = key.getLast();\n")
                .append("        switch (keyName)\n")
                .append("        {\n")
                .append("            case \"").append(M3Properties.name).append("\":\n")
                .append("            case \"").append(M3Properties._package).append("\":\n")
                .append("            {\n")
                .append("                throw new UnsupportedOperationException(\"Cannot add value for '\" + keyName + \"'\");\n")
                .append("            }\n")
                .append("            default:\n")
                .append("            {\n")
                .append("                super.addKeyValue(key, value);\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n");
    }

    private static StringBuilder appendIsFullyResolvedForConcreteElementOrVirtualPackage(StringBuilder builder)
    {
        return builder.append("    @Override\n")
                .append("    public boolean isFullyResolved(String key)\n")
                .append("    {\n")
                .append("        return \"" + M3Properties.name + "\".equals(key) || super.isFullyResolved(key);\n")
                .append("    }\n");
    }

    private static StringBuilder appendGetPropertyValue(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, boolean isConcreteElementOrVirtualPackage)
    {
        builder.append("    @Override\n")
                .append("    protected PropertyValue<?> getPropertyValue(String propertyName, boolean forWrite)\n")
                .append("    {\n");
        ListIterable<PropertyInfo> filteredProperties = isConcreteElementOrVirtualPackage ? simpleProperties.reject(p -> M3Properties.name.equals(p.name)) : simpleProperties;
        switch (filteredProperties.size())
        {
            case 0:
            {
                builder.append("        return null;\n");
                break;
            }
            case 1:
            {
                PropertyInfo propertyInfo = filteredProperties.get(0);
                if (isConcreteElementOrVirtualPackage && !propertyInfo.isPackageChildren() && !M3Properties._package.equals(propertyInfo.name))
                {
                    builder.append("        if (\"").append(propertyInfo.name).append("\".equals(propertyName))\n");
                    builder.append("        {\n");
                    builder.append("            initialize();\n");
                    builder.append("            return this._").append(propertyInfo.name).append(";\n");
                    builder.append("        }\n");
                    builder.append("        return null;\n");
                }
                else
                {
                    builder.append("        return \"").append(propertyInfo.name).append("\".equals(propertyName)) ? this._").append(propertyInfo.name).append(" : null;\n");
                }
                break;
            }
            default:
            {
                builder.append("        switch (propertyName)\n");
                builder.append("        {\n");
                filteredProperties.forEach(propertyInfo ->
                {
                    builder.append("            case \"").append(propertyInfo.name).append("\":\n");
                    builder.append("            {\n");
                    if (isConcreteElementOrVirtualPackage && !propertyInfo.isPackageChildren() && !M3Properties._package.equals(propertyInfo.name))
                    {
                        builder.append("                initialize();\n");
                    }
                    builder.append("                return this._").append(propertyInfo.name).append(";\n");
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

    private static StringBuilder appendSimplePropertyFields(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, Class<? extends AbstractLazyCoreInstance> superClass)
    {
        boolean isConcreteElementOrVirtualPackage = isConcreteElement(superClass) || isVirtualPackage(superClass);
        simpleProperties.forEach(propertyInfo ->
        {
            if (!isConcreteElementOrVirtualPackage || !M3Properties.name.equals(propertyInfo.name))
            {
                builder.append("    private ");
                if (!isConcreteElementOrVirtualPackage || propertyInfo.isPackageChildren() || M3Properties._package.equals(propertyInfo.name))
                {
                    builder.append("final ");
                }
                builder.append(propertyInfo.toOne ? "OneValue" : "ManyValues").append('<').append(propertyInfo.holderTypeJava).append("> _").append(propertyInfo.name).append(";\n");
            }
        });
        return builder;
    }

    private static StringBuilder appendSimpleProperties(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, String interfaceNamePlusTypeParams, Class<? extends AbstractLazyCoreInstance> superClass, ProcessorContext processorContext)
    {
        boolean isConcreteElementOrVirtualPackage = isConcreteElement(superClass) || isVirtualPackage(superClass);
        simpleProperties.forEach(propertyInfo -> appendSimpleProperty(builder.append('\n'), propertyInfo, interfaceNamePlusTypeParams, isConcreteElementOrVirtualPackage, processorContext));
        return builder;
    }

    private static void appendSimpleProperty(StringBuilder builder, PropertyInfo propertyInfo, String interfaceNamePlusTypeParams, boolean isConcreteElementOrVirtualPackage, ProcessorContext processorContext)
    {
        boolean shouldInit = isConcreteElementOrVirtualPackage && !propertyInfo.isPackageChildren() && !M3Properties.name.equals(propertyInfo.name) && !M3Properties._package.equals(propertyInfo.name);
        boolean isPackageableName = isConcreteElementOrVirtualPackage && M3Properties.name.equals(propertyInfo.name);
        if (propertyInfo.toOne)
        {
            builder.append("    public ").append(propertyInfo.returnTypeJava).append(" _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            if (isPackageableName)
            {
                builder.append("        return getName();\n");
            }
            else
            {
                if (shouldInit)
                {
                    builder.append("        initialize();\n");
                }
                builder.append("        return this._").append(propertyInfo.name).append(".getValue();\n");
            }
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (isPackageableName)
            {
                builder.append("        setName(value);\n");
            }
            else
            {
                if (shouldInit)
                {
                    builder.append("        initialize();\n");
                }
                if (propertyInfo.isFromAssociation())
                {
                    builder.append("        ").append(propertyInfo.returnTypeJava).append(" current = this._").append(propertyInfo.name).append(".getValue();\n");
                    builder.append("        if (current != null)\n");
                    builder.append("        {\n");
                    builder.append("            current._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                    builder.append("        }\n");
                }
                builder.append("        this._").append(propertyInfo.name).append(".setValue(value);\n");
                if (propertyInfo.isFromAssociation())
                {
                    builder.append("        if (value != null)\n");
                    builder.append("        {\n");
                    builder.append("            value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                    builder.append("        }\n");
                }
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.holderTypeJava).append("> values)\n");
            builder.append("    {\n");
            builder.append("        return _").append(propertyInfo.name).append("(values.getFirst());\n");
            builder.append("    }\n");
        }
        else
        {
            builder.append("    public RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> _").append(propertyInfo.name).append("()\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        return this._").append(propertyInfo.name).append(".getValues();\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : this._").append(propertyInfo.name).append(".getValues())\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".setValues(values);\n");
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : this._").append(propertyInfo.name).append(".getValues())\n");
                builder.append("        {\n");
                builder.append("            value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Add(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".addValue(value);\n");
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        value._reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAll(RichIterable<? extends ").append(propertyInfo.returnTypeJava).append("> values)\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".addValues(values);\n");
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
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        if (this._").append(propertyInfo.name).append(".removeValue(value))\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            else
            {
                builder.append("        this._").append(propertyInfo.name).append(".removeValue(value);\n");
            }
            builder.append("        return this;\n");
            builder.append("    }\n");
        }
        builder.append('\n');
        builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("Remove()\n");
        builder.append("    {\n");
        if (isPackageableName)
        {
            builder.append("        setName(null);\n");
        }
        else
        {
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            if (propertyInfo.isFromAssociation())
            {
                builder.append("        for (").append(propertyInfo.returnTypeJava).append(" value : this._").append(propertyInfo.name).append(".getValues())\n");
                builder.append("        {\n");
                builder.append("            value._sever_reverse_").append(propertyInfo.reversePropertyName).append("(this);\n");
                builder.append("        }\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".removeAllValues();\n");
        }
        builder.append("        return this;\n");
        builder.append("    }\n");
        if (!isPackageableName)
        {
            builder.append('\n');
            builder.append("    public void _reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".addValue(value);\n");
            builder.append("    }\n");
            builder.append('\n');
            builder.append("    public void _sever_reverse_").append(propertyInfo.name).append("(").append(propertyInfo.returnTypeJava).append(" value)\n");
            builder.append("    {\n");
            if (shouldInit)
            {
                builder.append("        initialize();\n");
            }
            builder.append("        this._").append(propertyInfo.name).append(".removeValue(value);\n");
            builder.append("    }\n");
        }

        if (processorContext.getGenerator().requiresCoreInstanceMethods(propertyInfo.property, propertyInfo.resolvedType))
        {
            if (propertyInfo.toOne)
            {
                builder.append('\n');
                builder.append("    public CoreInstance _").append(propertyInfo.name).append("CoreInstance()\n");
                builder.append("    {\n");
                if (propertyInfo.canBePrimitive())
                {
                    builder.append("        return ValCoreInstance.toCoreInstance(_").append(propertyInfo.name).append("());\n");
                }
                else
                {
                    builder.append("        return _").append(propertyInfo.name).append("();\n");
                }
                builder.append("    }\n");
                builder.append('\n');
                builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(CoreInstance value)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n");
                builder.append("    }\n");
            }
            else
            {
                builder.append('\n');
                builder.append("    public RichIterable<? extends CoreInstance> _").append(propertyInfo.name).append("CoreInstance()\n");
                builder.append("    {\n");
                if (propertyInfo.canBePrimitive())
                {
                    builder.append("        return ValCoreInstance.toCoreInstances(_").append(propertyInfo.name).append("());\n");
                }
                else
                {
                    builder.append("        return _").append(propertyInfo.name).append("();\n");
                }
                builder.append("    }\n");
                builder.append('\n');
                builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("CoreInstance(RichIterable<? extends CoreInstance> values)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n");
                builder.append("    }\n");
                builder.append('\n');
                builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddCoreInstance(CoreInstance value)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n");
                builder.append("    }\n");
                builder.append('\n');
                builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("AddAllCoreInstance(RichIterable<? extends CoreInstance> values)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n");
                builder.append("    }\n");
                builder.append('\n');
                builder.append("    public ").append(interfaceNamePlusTypeParams).append(" _").append(propertyInfo.name).append("RemoveCoreInstance(CoreInstance value)\n");
                builder.append("    {\n");
                builder.append("        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n");
                builder.append("    }\n");
            }
        }
    }

    private static StringBuilder appendEquals(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties, String interfaceName)
    {
        ListIterable<PropertyInfo> equalityProperties = simpleProperties.select(p -> p.equalityKey);
        builder.append("    @Override\n");
        builder.append("    public boolean pureEquals(Object obj)\n");
        builder.append("    {\n");
        builder.append("        if (this == obj)\n");
        builder.append("        {\n");
        builder.append("            return true;\n");
        builder.append("        }\n");
        builder.append("        if (!(obj instanceof ").append(interfaceName).append("))\n");
        builder.append("        {\n");
        builder.append("            return false;\n");
        builder.append("        }\n");
        builder.append("        ").append(interfaceName).append(" that = (").append(interfaceName).append(") obj;\n");
        builder.append("        return this.getFullSystemPath().equals(that.getFullSystemPath())");
        equalityProperties.forEach(p ->
        {
            builder.append(" &&\n                ");
            if (double.class.getName().equals(p.returnTypeJava) || long.class.getName().equals(p.returnTypeJava) || boolean.class.getName().equals(p.returnTypeJava))
            {
                builder.append("this._").append(p.name).append("() == that._").append(p.name).append("()");
            }
            else
            {
                builder.append("CompiledSupport.equal(this._").append(p.name).append("(), that._").append(p.name).append("())");
            }
        });
        builder.append(";\n");
        builder.append("    }\n\n");
        builder.append("    @Override\n");
        builder.append("    public int pureHashCode()\n");
        builder.append("    {\n");
        switch (equalityProperties.size())
        {
            case 1:
            {
                builder.append("        return CompiledSupport.safeHashCode(_").append(equalityProperties.get(0).name).append("());\n");
                break;
            }
            case 2:
            {
                builder.append("        return 31 * CompiledSupport.safeHashCode(_").append(equalityProperties.get(0).name).append("()) + CompiledSupport.safeHashCode(_").append(equalityProperties.get(1).name).append("());\n");
                break;
            }
            default:
            {
                builder.append("        int hash = CompiledSupport.safeHashCode(_").append(equalityProperties.get(0).name).append("());\n");
                equalityProperties.forEach(1, equalityProperties.size() - 1, p -> builder.append("        hash = 31 * hash + CompiledSupport.safeHashCode(_").append(p.name).append("());\n"));
                builder.append("        return hash;\n");
            }
        }
        return builder.append("    }\n");
    }

    private static void appendCopy(StringBuilder builder, String classNamePlusTypeParams, String interfaceNamePlusTypeParams, boolean isConcreteElementOrVirtualPackage)
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
    }

    private static void appendConcreteInitialize(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    @Override\n")
                .append("    protected void initialize(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n")
                .append("    {\n");
        MutableSet<String> backRefProperties = Sets.mutable.empty();
        simpleProperties.forEach(propertyInfo ->
        {
            if (propertyInfo.isBackRef())
            {
                backRefProperties.add(propertyInfo.name);
                builder.append("        MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
            }
        });
        if (backRefProperties.notEmpty())
        {
            builder.append("        collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder");
            M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                    .sortThis()
                    .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
            builder.append(");\n");
        }
        builder.append("        MutableMap<String, PropertyValues> propertyValuesByName = indexPropertyValues(instanceData);\n");
        simpleProperties.forEach(propertyInfo ->
        {
            if (!propertyInfo.isPackageChildren() && !M3Properties.name.equals(propertyInfo.name) && !M3Properties._package.equals(propertyInfo.name))
            {
                builder.append("        this._").append(propertyInfo.name).append(" = ")
                        .append(propertyInfo.toOne ? "newToOnePropertyValue" : "newToManyPropertyValue")
                        .append("(propertyValuesByName.get(\"").append(propertyInfo.name).append("\"), referenceIdResolver, internalIdResolver, primitiveValueResolver, true");
                if (propertyInfo.isBackRef())
                {
                    builder.append(", ").append(propertyInfo.name);
                }
                if (propertyInfo.primitiveType)
                {
                    builder.append(", toCoreInstanceFn(), ").append(AbstractCompiledLazyConcreteElement.class.getSimpleName()).append(".<").append(propertyInfo.holderTypeJava).append(">fromValCoreInstanceFn()");
                }
                else if (propertyInfo.anyOrNilType)
                {
                    builder.append(", toCoreInstanceFn(), ").append(AbstractCompiledLazyConcreteElement.class.getSimpleName()).append(".<").append(propertyInfo.holderTypeJava).append(">fromCoreInstanceFn()");
                }
                builder.append(");\n");
            }
        });
        builder.append("    }\n");
    }

    private static void appendVirtualPackageInitialize(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        builder.append("    @Override\n")
                .append("    protected void initialize(ListIterable<? extends BackReference> backReferences, ReferenceIdResolvers referenceIds, ElementBuilder elementBuilder)\n")
                .append("    {\n");
        MutableSet<String> backRefProperties = Sets.mutable.empty();
        simpleProperties.forEach(propertyInfo ->
        {
            if (propertyInfo.isBackRef())
            {
                backRefProperties.add(propertyInfo.name);
                builder.append("        MutableList<Supplier<? extends ").append(propertyInfo.holderTypeJava).append(">> ").append(propertyInfo.name).append(" = Lists.mutable.empty();\n");
            }
        });
        if (backRefProperties.notEmpty())
        {
            builder.append("        collectBackReferences(backReferences, referenceIds, elementBuilder");
            M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.collect(ImmutableList::getLast, Lists.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()))
                    .sortThis()
                    .forEach(p -> builder.append(", ").append(backRefProperties.contains(p) ? p : "null"));
            builder.append(");\n");
        }
        simpleProperties.forEach(propertyInfo ->
        {
            if (!propertyInfo.isPackageChildren() && !M3Properties.name.equals(propertyInfo.name) && !M3Properties._package.equals(propertyInfo.name))
            {
                builder.append("        this._").append(propertyInfo.name).append(" = ");
                if (propertyInfo.isBackRef())
                {
                    builder.append("ManyValues.fromSuppliers(").append(propertyInfo.name).append(");\n");
                }
                else
                {
                    builder.append(propertyInfo.toOne ? "OneValue.fromValue" : "ManyValues.fromValues").append("(null);\n");
                }
            }
        });
        builder.append("    }\n");
    }

    private static void appendValidate(StringBuilder builder, CoreInstance classGenericType, CoreInstance _class, String interfaceNamePlusTypeParams, ListIterable<PropertyInfo> simpleProperties, ProcessorContext processorContext)
    {
        if (ClassProcessor.isPlatformClass(_class))
        {
            return;
        }

        String validateExtraValues = _class.getValueForMetaPropertyToMany(M3Properties.typeVariables)
                .collect(p -> " _" + PrimitiveUtilities.getStringValue(p.getValueForMetaPropertyToOne(M3Properties.name)), Lists.mutable.empty())
                .with("this")
                .makeString("Lists.mutable.with(", ", ", ")");
        builder.append(ClassImplProcessor.validate(true, _class, interfaceNamePlusTypeParams, classGenericType, processorContext, simpleProperties.collect(p -> p.property), null, validateExtraValues));
    }

    private static void appendDefaultValues(StringBuilder builder, ListIterable<PropertyInfo> simpleProperties)
    {
        ListIterable<PropertyInfo> simplePropsWithDefaultValues = simpleProperties.select(p -> p.defaultValueJava != null);
        if (simplePropsWithDefaultValues.isEmpty())
        {
            return;
        }

        builder.append('\n');
        builder.append("    @Override\n");
        builder.append("    public ListIterable<String> getDefaultValueKeys()\n");
        builder.append("    {\n");
        builder.append("        return Lists.immutable.with(");
        simplePropsWithDefaultValues.forEach(p -> builder.append('"').append(p.name).append("\", "));
        builder.setLength(builder.length() - 2);
        builder.append(");\n");
        builder.append("    }\n");
        builder.append('\n');
        builder.append("    @Override\n");
        builder.append("    public RichIterable<?> getDefaultValue(String property, ExecutionSupport es)\n");
        builder.append("    {\n");
        if (simplePropsWithDefaultValues.size() == 1)
        {
            PropertyInfo propInfo = simplePropsWithDefaultValues.get(0);
            builder.append("        return \"").append(propInfo.name).append("\".equals(property) ? ").append(propInfo.defaultValueJava).append(" : Lists.immutable.empty();\n");
        }
        else
        {
            builder.append("        switch (property)\n");
            builder.append("        {\n");
            simplePropsWithDefaultValues.forEach(p ->
            {
                builder.append("            case \"").append(p.name).append("\":\n");
                builder.append("            {\n");
                builder.append("                return ").append(p.defaultValueJava).append(";\n");
                builder.append("            }\n");
            });
            builder.append("            default:\n");
            builder.append("            {\n");
            builder.append("                return Lists.immutable.empty();\n");
            builder.append("            }\n");
            builder.append("        }\n");
        }
        builder.append("    }\n");
    }

    private static boolean isConcreteElement(Class<? extends AbstractLazyCoreInstance> superClass)
    {
        return AbstractLazyConcreteElement.class.isAssignableFrom(superClass);
    }

    private static boolean isVirtualPackage(Class<? extends AbstractLazyCoreInstance> superClass)
    {
        return AbstractLazyVirtualPackage.class.isAssignableFrom(superClass);
    }

    private static boolean requiresEquals(ListIterable<PropertyInfo> simpleProperties)
    {
        return simpleProperties.anySatisfy(p -> p.equalityKey);
    }

    private static boolean requiresCompImplementation(ListIterable<PropertyInfo> simpleProperties, MapIterable<String, CoreInstance> qualifiedProperties)
    {
        return qualifiedProperties.notEmpty() || requiresEquals(simpleProperties);
    }

    private static ListIterable<PropertyInfo> getSimplePropertiesSortedByName(CoreInstance classGenericType, CoreInstance _class, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        MapIterable<String, CoreInstance> properties = processorSupport.class_getSimplePropertiesByName(_class);
        MutableList<PropertyInfo> result = Lists.mutable.ofInitialCapacity(properties.size());
        CoreInstance any = processorSupport.type_TopType();
        CoreInstance nil = processorSupport.type_BottomType();
        CoreInstance equalityKeyStereotype = Profile.findStereotype(processorSupport.package_getByUserPath(M3Paths.equality), _Class.KEY_STEREOTYPE);
        MutableMap<String, ImmutableList<String>> backRefProperties = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.groupByUniqueKey(ImmutableList::getLast, Maps.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()));
        properties.forEachKeyValue((name, property) ->
        {
            CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
            CoreInstance returnRawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);
            String holderTypeJava = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);
            String returnTypeJava = (Multiplicity.isToOne(multiplicity, true) && GenericType.isGenericTypeConcrete(ClassProcessor.getPropertyUnresolvedReturnType(property, processorSupport))) ?
                                    TypeProcessor.pureTypeToJava(returnType, true, true, processorSupport) :
                                    holderTypeJava;
            String defaultValueJava = DefaultValue.getDefaultValueJavaExpression(property, true, processorContext);
            boolean anyOrNilType = (returnRawType == null) || (returnRawType == any) || (returnRawType == nil);
            boolean primitiveType = !anyOrNilType && processorSupport.instance_instanceOf(returnRawType, M3Paths.PrimitiveType);
            CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
            String reversePropertyName;
            if (processorSupport.instance_instanceOf(propertyOwner, M3Paths.Association))
            {
                ListIterable<? extends CoreInstance> associationProperties = propertyOwner.getValueForMetaPropertyToMany(M3Properties.properties);
                CoreInstance reverseProperty = associationProperties.get(property == associationProperties.get(0) ? 1 : 0);
                reversePropertyName = Property.getPropertyName(reverseProperty);
            }
            else
            {
                reversePropertyName = null;
            }
            boolean toOne = Multiplicity.isToOne(multiplicity, false);
            boolean equalityKey = Instance.getValueForMetaPropertyToManyResolved(property, M3Properties.stereotypes, processorSupport).anySatisfy(st -> st == equalityKeyStereotype);
            PropertyCategory category = getPropertyCategory(name, property, propertyOwner, backRefProperties, processorSupport);
            result.add(new PropertyInfo(name, property, returnType, holderTypeJava, returnTypeJava, defaultValueJava, reversePropertyName, anyOrNilType, primitiveType, toOne, equalityKey, category));
        });
        return result.sortThis();
    }

    private static PropertyCategory getPropertyCategory(String name, CoreInstance property, CoreInstance propertyOwner, MapIterable<String, ImmutableList<String>> backRefProperties, ProcessorSupport processorSupport)
    {
        if (M3Properties.children.equals(name) && (propertyOwner == processorSupport.package_getByUserPath(M3Paths.Package)))
        {
            return PropertyCategory.PACKAGE_CHILDREN;
        }

        ImmutableList<String> backRefRealKey = backRefProperties.get(name);
        if ((backRefRealKey != null) && backRefRealKey.equals(Property.calculatePropertyPath(property, processorSupport)))
        {
            return PropertyCategory.BACK_REF;
        }

        return PropertyCategory.ORDINARY;
    }

    private static class PropertyInfo implements Comparable<PropertyInfo>
    {
        private final String name;
        private final CoreInstance property;
        private final CoreInstance resolvedType;
        private final String holderTypeJava;
        private final String returnTypeJava;
        private final String defaultValueJava;
        private final String reversePropertyName;
        private final boolean anyOrNilType;
        private final boolean primitiveType;
        private final boolean toOne;
        private final boolean equalityKey;
        private final PropertyCategory category;

        private PropertyInfo(String name, CoreInstance property, CoreInstance resolvedType, String holderTypeJava, String returnTypeJava, String defaultValueJava, String reversePropertyName, boolean anyOrNilType, boolean primitiveType, boolean toOne, boolean equalityKey, PropertyCategory category)
        {
            this.name = name;
            this.property = property;
            this.resolvedType = resolvedType;
            this.holderTypeJava = holderTypeJava;
            this.returnTypeJava = returnTypeJava;
            this.defaultValueJava = defaultValueJava;
            this.reversePropertyName = reversePropertyName;
            this.anyOrNilType = anyOrNilType;
            this.primitiveType = primitiveType;
            this.toOne = toOne;
            this.equalityKey = equalityKey;
            this.category = category;
        }

        @Override
        public int compareTo(PropertyInfo other)
        {
            return this.name.compareTo(other.name);
        }

        boolean isFromAssociation()
        {
            return this.reversePropertyName != null;
        }

        boolean canBePrimitive()
        {
            return this.primitiveType || this.anyOrNilType;
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
}
