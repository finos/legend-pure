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

package org.finos.legend.pure.m3.bootstrap.generator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class M3ToJavaGenerator
{
    private static final String ROOT_PACKAGE = "org.finos.legend.pure.m3.coreinstance";
    private static final String CLASS_SUFFIX = "Instance";
    private static final String WRAPPER_CLASS_SUFFIX = "CoreInstanceWrapper";
    private static final String ACCESSOR_SUFFIX = "Accessor";
    private static final String BUILDER_SUFFIX = "Builder";

    private static final String STUB_UNSUPPORTED_EXCEPTION = "throw new UnsupportedOperationException(\"This stubbed property method is not supported in interpreted mode\")";
    private static final String WRAPPER_UNSUPPORTED_EXCEPTION = "throw new UnsupportedOperationException(\"This method is not supported on wrapper classes\")";
    private static final String M3_UNSUPPORTED_EXCEPTION = "throw new UnsupportedOperationException(\"This method is not supported on M3 classes\")";

    private static final Map<String, String> PRIMITIVES = Maps.mutable.<String, String>empty()
            .withKeyValue(ModelRepository.INTEGER_TYPE_NAME, "Long")
            .withKeyValue(ModelRepository.INTEGER_TYPE_NAME, "Long")
            .withKeyValue(ModelRepository.FLOAT_TYPE_NAME, "Double")
            .withKeyValue(ModelRepository.DECIMAL_TYPE_NAME, "java.math.BigDecimal")
            .withKeyValue(ModelRepository.BOOLEAN_TYPE_NAME, "Boolean")
            .withKeyValue(ModelRepository.DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate")
            .withKeyValue(ModelRepository.STRICT_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate")
            .withKeyValue(ModelRepository.DATETIME_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime")
            .withKeyValue(ModelRepository.LATEST_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate")
            .withKeyValue(ModelRepository.STRING_TYPE_NAME, "String")
            .withKeyValue("Number", "Number")
            .asUnmodifiable();
    private static final Map<String, String> PRIMITIVES_EXTERNAL = Maps.mutable.<String, String>empty()
            .withKeyValue(ModelRepository.INTEGER_TYPE_NAME, "long")
            .withKeyValue(ModelRepository.FLOAT_TYPE_NAME, "double")
            .withKeyValue(ModelRepository.DECIMAL_TYPE_NAME, "java.math.BigDecimal")
            .withKeyValue(ModelRepository.BOOLEAN_TYPE_NAME, "boolean")
            .withKeyValue(ModelRepository.DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate")
            .withKeyValue(ModelRepository.STRICT_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate")
            .withKeyValue(ModelRepository.DATETIME_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime")
            .withKeyValue(ModelRepository.LATEST_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate")
            .withKeyValue(ModelRepository.STRING_TYPE_NAME, "String")
            .withKeyValue("Number", "Number")
            .asUnmodifiable();
    private static final Map<String, String> PRIMITIVES_EXTERNAL_0_1 = Maps.mutable.<String, String>empty()
            .withKeyValue(ModelRepository.INTEGER_TYPE_NAME, "Long")
            .withKeyValue(ModelRepository.FLOAT_TYPE_NAME, "Double")
            .withKeyValue(ModelRepository.DECIMAL_TYPE_NAME, "java.math.BigDecimal")
            .withKeyValue(ModelRepository.BOOLEAN_TYPE_NAME, "Boolean")
            .withKeyValue(ModelRepository.DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate")
            .withKeyValue(ModelRepository.STRICT_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate")
            .withKeyValue(ModelRepository.DATETIME_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime")
            .withKeyValue(ModelRepository.LATEST_DATE_TYPE_NAME, "org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate")
            .withKeyValue(ModelRepository.STRING_TYPE_NAME, "String")
            .withKeyValue("Number", "Number")
            .asUnmodifiable();

    private final MutableMap<String, StubDef> stubDefs = ArrayAdapter.adapt(
            StubDef.build("Class", "ImportStub"),
            StubDef.build("Stereotype", "ImportStub"),
            StubDef.build("Tag", "ImportStub"),
            StubDef.build("Type", "ImportStub"),
            StubDef.build("Enumeration", "ImportStub"),
            StubDef.build("AbstractProperty", "PropertyStub"),
            StubDef.build("Association", "ImportStub"),
            StubDef.build("ClassProjection", "ImportStub"),
            StubDef.build("Function", "ImportStub"),
            StubDef.build("Property", "PropertyStub", Sets.immutable.with("Class", "Association")),
            StubDef.build("Enum", "EnumStub", Sets.immutable.with("Enumeration"))
    ).groupByUniqueKey(StubDef::getClassName);

    private final String outputDir;
    private final String factoryNamePrefix;
    private final boolean generateTypeFactoriesById;
    private final PropertyTypeResolver propertyTypeResolver;

    private static final SetIterable<String> PLATFORM_EXCLUDED_FILES = Sets.immutable.with();
    private static final SetIterable<String> PLATFORM_EXCLUDED_CLASSES = Sets.immutable.empty();

    public static boolean isPlatformClass(CoreInstance _class)
    {
        return !PLATFORM_EXCLUDED_FILES.contains(_class.getSourceInformation().getSourceId()) && !PLATFORM_EXCLUDED_CLASSES.contains(_class.getName());
    }

    public M3ToJavaGenerator(String outputDir, String factoryNamePrefix, boolean generateTypeFactoriesById)
    {
        this(outputDir, factoryNamePrefix, generateTypeFactoriesById, new DefaultPropertyTypeResolver(), Maps.mutable.empty());
    }

    public M3ToJavaGenerator(String outputDir, String factoryNamePrefix, boolean generateTypeFactoriesById, PropertyTypeResolver propertyTypeResolver, Map<? extends String, ? extends StubDef> additionalStubs)
    {
        this.outputDir = outputDir;
        this.factoryNamePrefix = factoryNamePrefix;
        this.generateTypeFactoriesById = generateTypeFactoriesById;
        this.propertyTypeResolver = propertyTypeResolver;
        this.stubDefs.putAll(additionalStubs);
    }

    public void generate(ModelRepository repository, SetIterable<String> fileNames, String fileNameStartsWith)
    {
        MutableMap<String, CoreInstance> packageToCoreInstance = Maps.mutable.of();
        MutableSet<CoreInstance> m3Enumerations = Sets.mutable.of();
        this.createClasses(repository.getTopLevels(), packageToCoreInstance, m3Enumerations, fileNames, fileNameStartsWith);
        this.createFactory(packageToCoreInstance, m3Enumerations);
    }

    private StubDef getStubDef(String className)
    {
        return this.stubDefs.get(className);
    }

    private String getStubType(String className)
    {
        StubDef stubDef = getStubDef(className);
        return stubDef == null ? null : stubDef.getStubType();
    }

    private boolean isOwningClass(String instanceClassName, String propertyClassName)
    {
        StubDef stubDef = getStubDef(propertyClassName);
        return stubDef != null && stubDef.isOwner(instanceClassName);
    }

    private void createClasses(RichIterable<? extends CoreInstance> instances, MutableMap<String, CoreInstance> packageToCoreInstance,
                               MutableSet<CoreInstance> m3Enumerations, SetIterable<String> sourcesToInclude, String fileNameStartsWith)
    {
        for (CoreInstance instance : instances)
        {
            if ("Class".equals(instance.getClassifier().getName())
                    && (instance.getSourceInformation() != null && (sourcesToInclude.contains(instance.getSourceInformation().getSourceId())) || (fileNameStartsWith != null && instance.getSourceInformation().getSourceId().startsWith(fileNameStartsWith))))
            {
                String javaPackage = this.createAndWriteClass(instance);
                packageToCoreInstance.put(javaPackage + "." + instance.getName() + "Instance", instance);
            }
            else if (isEnum(instance) && "/platform/pure/grammar/m3.pure".equals(instance.getSourceInformation().getSourceId()))
            {
                //Needed for M3 serialization
                m3Enumerations.add(instance);
            }
            this.createClasses(instance.getValueForMetaPropertyToMany("children"), packageToCoreInstance, m3Enumerations, sourcesToInclude, fileNameStartsWith);
        }
    }

    private String createAndWriteClass(CoreInstance instance)
    {
        String javaPackage = getJavaPackageString(instance);
        CoreInstance classGenericType = getClassGenericType(instance);

        MutableSet<CoreInstance> propertiesFromAssociations = Sets.mutable.withAll(instance.getValueForMetaPropertyToMany("propertiesFromAssociations"));
        collectGeneralizationXProperties(instance.getValueForMetaPropertyToMany("generalizations"), "propertiesFromAssociations", propertiesFromAssociations);

        MutableSet<CoreInstance> qualifiedProperties = Sets.mutable.<CoreInstance>withAll(instance.getValueForMetaPropertyToMany("qualifiedProperties"))
                .withAll(instance.getValueForMetaPropertyToMany("qualifiedPropertiesFromAssociations"));
        collectGeneralizationXProperties(instance.getValueForMetaPropertyToMany("generalizations"), "qualifiedProperties", qualifiedProperties);

        MutableSet<CoreInstance> properties = Sets.mutable.<CoreInstance>withAll(instance.getValueForMetaPropertyToMany("properties")).withAll(propertiesFromAssociations);

        Imports imports = this.getPropertyTypePackages(classGenericType, properties, qualifiedProperties, true);

        String immutableInterfaceText = this.createAccessorInterface(instance, javaPackage, properties, qualifiedProperties, imports);
        //String mutableInterfaceText = this.createBuilderInterface(instance, javaPackage, properties, imports);

        MutableMap<String, CoreInstance> propertyOwners = this.collectPropertyOwnersAndImportsForGeneralizations(instance, properties, imports);
        String interfaceText = this.createInterface(instance, javaPackage, properties, propertiesFromAssociations, imports);
        String classText = this.createClass(instance, javaPackage, properties, propertiesFromAssociations, qualifiedProperties, imports, propertyOwners);
        String wrapperClassText = this.createWrapperClass(instance, javaPackage, properties, propertiesFromAssociations, qualifiedProperties, getPropertyTypePackages(classGenericType, properties, qualifiedProperties, true), propertyOwners);

        Path fileOutputDir = Paths.get(this.outputDir + javaPackage.replace(".", "/"));
        Path classPath = fileOutputDir.resolve(getClassName(instance) + ".java");
        Path interfacePath = fileOutputDir.resolve(getInterfaceName(instance) + ".java");
        Path accessorInterfacePath = fileOutputDir.resolve(getAccessorInterfaceName(instance) + ".java");
        //Path builderInterfacePath = fileOutputDir.resolve(getBuilderInterfaceName(instance) + ".java");
        Path wrapperPath = fileOutputDir.resolve(getWrapperName(instance) + ".java");
        try
        {
            Files.createDirectories(fileOutputDir);
            Files.write(interfacePath, interfaceText.getBytes(StandardCharsets.UTF_8));
            Files.write(accessorInterfacePath, immutableInterfaceText.getBytes(StandardCharsets.UTF_8));
            //Files.write(builderInterfacePath, mutableInterfaceText.getBytes(StandardCharsets.UTF_8));
            Files.write(classPath, classText.getBytes(StandardCharsets.UTF_8));
            Files.write(wrapperPath, wrapperClassText.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return javaPackage;
    }

    private CoreInstance getClassGenericType(CoreInstance instance)
    {
        return this.propertyTypeResolver.getClassGenericType(instance);
    }

    private static String getJavaPackageString(CoreInstance instance)
    {
        String javaPackage = ROOT_PACKAGE;

        CoreInstance pkg = instance.getValueForMetaPropertyToOne("package");

        if (pkg == null)
        {
            CoreInstance type = getType(instance);
            if (type != null)
            {
                pkg = type.getValueForMetaPropertyToOne("package");
            }

        }

        if (pkg != null)
        {
            ListIterable<String> packagePath = getUserObjectPathForPackageableElement(pkg, false);
            javaPackage = getJavaPackageString(packagePath);
        }

        return javaPackage;
    }

    private static String getJavaPackageString(ListIterable<String> packagePath)
    {
        return packagePath.isEmpty() ? ROOT_PACKAGE : packagePath.collectWith(JavaTools::makeValidJavaIdentifier, "_").makeString(ROOT_PACKAGE + ".", ".", "");
    }


    private String createClass(final CoreInstance instance, String javaPackage, MutableSet<CoreInstance> properties, MutableSet<CoreInstance> propertiesFromAssociations, MutableSet<CoreInstance> qualifiedProperties, final Imports imports, final MutableMap<String, CoreInstance> propertyOwners)
    {
        imports.addImports("org.finos.legend.pure.m3.coreinstance.helper.PrimitiveHelper", "org.finos.legend.pure.m3.coreinstance.helper.*");

        final String interfaceName = getInterfaceName(instance);
        final String className = getClassName(instance);
        String typeParams = getTypeParams(instance, false);
        String typeParamsWithExtendsCoreInstance = getTypeParams(instance, true);

        final CoreInstance classGenericType = getClassGenericType(instance);


        ListIterable<String> paths = getUserObjectPathForPackageableElement(instance, true);
        String pathString = paths.makeString("\"", "\",\"", "\"");
        String systemPathForPackageableElement = paths.makeString("::");


        PartitionIterable<CoreInstance> partition = properties.partition(M3ToJavaGenerator::isToOne);

        RichIterable<CoreInstance> toOneProperties = partition.getSelected();
        RichIterable<CoreInstance> toManyProperties = partition.getRejected();

        RichIterable<CoreInstance> mandatoryToOneProps = toOneProperties.select(M3ToJavaGenerator::isMandatoryProperty);

        RichIterable<Pair<String, String>> typesForMandatoryProps = buildMandatoryProperties(classGenericType, mandatoryToOneProps, imports).toSortedSetBy(Pair::getOne);

        MutableList<String> mandatoryTypes = Lists.mutable.of();
        MutableList<String> mandatoryProps = Lists.mutable.of();

        for (Pair<String, String> pair : typesForMandatoryProps)
        {
            mandatoryTypes.add(pair.getTwo() + " " + pair.getOne());
            mandatoryProps.add(pair.getOne());
        }

        String mandatoryTypeString = mandatoryTypes.isEmpty() ? "" : mandatoryTypes.makeString(", ", ", ", "");
        String mandatoryPropertyNames = mandatoryProps.isEmpty() ? "" : mandatoryProps.makeString(", ", ", ", "");

        final String maybeFullyQualifiedName = imports.shouldFullyQualify(javaPackage + "." + interfaceName) ? javaPackage + "." + interfaceName : interfaceName;

        String interfaceNamePlusTypeParams = maybeFullyQualifiedName + (typeParamsWithExtendsCoreInstance.isEmpty() ? "" : typeParamsWithExtendsCoreInstance);

        boolean isEnum = "Root::meta::pure::metamodel::type::Enum".equals(systemPathForPackageableElement);

        String value =
                "\n" +
                        "package " + javaPackage + ";\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.api.block.predicate.Predicate;\n" +
                        "import org.eclipse.collections.api.list.ListIterable;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.api.map.MapIterable;\n" +
                        "import org.eclipse.collections.api.set.SetIterable;\n" +
                        "import org.eclipse.collections.impl.block.factory.Functions;\n" +
                        "import org.eclipse.collections.impl.factory.Lists;\n" +
                        "import org.eclipse.collections.impl.factory.Sets;\n" +
                        "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.BaseM3CoreInstanceFactory;\n" +
                        "import org.finos.legend.pure.m4.exception.PureCompilationException;\n" +
                        imports.toImportString() + "\n" +
                        "import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;\n" +
                        "import org.finos.legend.pure.m4.ModelRepository;\n" +
                        "import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceMutableState;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.simple.Values;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.simple.OneValueException;\n" +
                        getPrimitiveImports() + "\n" +
                        "\n" +
                        "public class " + className + " extends BaseCoreInstance implements " + maybeFullyQualifiedName + typeParamsWithExtendsCoreInstance + (isEnum ? " ,Comparable<" + javaPackage + "." + interfaceName + ">" : "") + ", CoreInstanceFactory\n" +
                        "{\n" +
                        createClassFactory(className, getUserObjectPathForPackageableElement(instance, false).makeString("::")) +
                        "\n" +
                        "    // TODO: These should be static\n" +
                        properties.collect(property ->
                        {
                            String propertyName = property.getName();
                            CoreInstance propertyOwner = propertyOwners.get(propertyName);
                            ListIterable<String> propertyOwnerPath = getUserObjectPathForPackageableElement(propertyOwner, true);
                            return "    private CoreInstance " + propertyName + "Key;\n" +
                                    (propertyOwner == instance || "Association".equals(propertyOwner.getClassifier().getName()) ?
                                            "    public static final ListIterable<String> " + createPropertyKeyName(propertyName) + " = Lists.immutable.of(" + propertyOwnerPath.makeString("\"", "\",\"children\",\"", "\"") + ",\"properties\",\"" + propertyName + "\");\n" : "");
                        }).makeString("") +
                        "\n" +
                        "\n" +
                        "    private _State state;\n" +
                        "    private static final SetIterable<String> keys = Sets.immutable.with(" + properties.collect(CoreInstance::getName, FastList.newList()).sortThis().makeString("\"", "\",\"", "\"") + ");\n" +
                        "\n" +
                        createClassConstructors(className) +
                        "\n" +
                        "    public void commit(ModelRepositoryTransaction transaction)\n" +
                        "    {\n" +
                        "        this.state = (_State)transaction.getState(this);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    public void rollback(ModelRepositoryTransaction transaction)\n" +
                        "    {\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void addCompileState(CompileState state)\n" +
                        "    {\n" +
                        "        this.prepareForWrite();\n" +
                        "        this.getState().addCompileState(state);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void removeCompileState(CompileState state)\n" +
                        "    {\n" +
                        "        this.prepareForWrite();\n" +
                        "        this.getState().removeCompileState(state);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean hasCompileState(CompileState state)\n" +
                        "    {\n" +
                        "        return this.getState().hasCompileState(state);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public CompileStateSet getCompileStates()\n" +
                        "    {\n" +
                        "        return this.getState().getCompileStates();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void setCompileStatesFrom(CompileStateSet states)\n" +
                        "    {\n" +
                        "        this.getState().setCompileStatesFrom(states);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void prepareForWrite()\n" +
                        "    {\n" +
                        "        ModelRepositoryTransaction transaction = this.getRepository().getTransaction();\n" +
                        "        if (transaction != null && !transaction.isRegistered(this))\n" +
                        "        {\n" +
                        "            transaction.registerModified(this, this.state.copy());\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    private _State getState()\n" +
                        "    {\n" +
                        "        ModelRepositoryTransaction transaction = this.getRepository().getTransaction();\n" +
                        "        if ((transaction != null) && transaction.isOpen())\n" +
                        "        {\n" +
                        "            _State transactionState = (_State)transaction.getState(this);\n" +
                        "            if (transactionState != null)\n" +
                        "            {\n" +
                        "                return transactionState;\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return this.state;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public CoreInstance getValueForMetaPropertyToOne(String keyName)\n" +
                        "    {\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toOneProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ";\n" +
                                        "            }\n").makeString("") +
                        toManyProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getOneValueFromToManyPropertyValues(keyName, this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ");\n" +
                                        "            }\n").makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                return null;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)\n" +
                        "    {\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValuesFromToManyPropertyValues(this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ");\n" +
                                        "            }\n").makeString("") +
                        toOneProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValuesFromToOnePropertyValue(this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ");\n" +
                                        "            }\n").makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                return Lists.immutable.empty();\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)\n" +
                        "    {\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValueByIDIndexFromToManyPropertyValues(keyName, this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ", indexSpec, keyInIndex);\n" +
                                        "            }\n").makeString("") +
                        toOneProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValueByIDIndexFromToOnePropertyValue(this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ", indexSpec, keyInIndex);\n" +
                                        "            }\n").makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                return null;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)\n" +
                        "    {\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValuesByIndexFromToManyPropertyValues(this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ", indexSpec, keyInIndex);\n" +
                                        "            }\n").makeString("") +
                        toOneProperties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return getValuesByIndexFromToOnePropertyValue(this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ", indexSpec, keyInIndex);\n" +
                                        "            }\n").makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                return Lists.immutable.empty();\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean isValueDefinedForKey(String keyName)\n" +
                        "    {\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        properties.collect(property ->
                                "            case \"" + property.getName() + "\":\n" +
                                        "            {\n" +
                                        "                return this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + " != null;\n" +
                                        "            }\n").makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                return false;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)\n" +
                        "    {\n" +
                        "        this.prepareForWrite();\n" +
                        "        _State state = this.getState();\n" +
                        "        switch (key)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                " + getPropertyTypeInternal(property, propertyReturnGenericType, imports, false, false, isPlatformClass(property)) + " values = state." + propertyName + ";\n" +
                                    "                if (values == null)\n" +
                                    "                {\n" +
                                    "                    throw new IllegalArgumentException(\"Cannot modify value at offset \" + offset + \" for property '\" + key + \"'\");\n" +
                                    "                }\n" +
                                    "                values.setValue(offset, " + (isAnyOrNilTypeProperty(propertyReturnGenericType) ? "value" : typeConversionInSetter(property, propertyReturnGenericType, imports, "value", true, true, true)) + ");\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        toOneProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                if (offset != 0)\n" +
                                    "                {\n" +
                                    "                    throw new IllegalArgumentException(\"Cannot modify value at offset \" + offset + \" for to-one property '\" + key + \"'\");\n" +
                                    "                }\n" +
                                    "                synchronized (state)\n" +
                                    "                {\n" +
                                    "                    state." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + " = " + typeConversionInSetter(property, propertyReturnGenericType, imports, "value", false, true, true) + ";\n" +
                                    "                }\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                throw new IllegalArgumentException(\"Unknown property '\" + key + \"'\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance valueToRemove)\n" +
                        "    {\n" +
                        "        this.prepareForWrite();\n" +
                        "        _State state = this.getState();\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                " + getPropertyTypeInternal(property, propertyReturnGenericType, imports, false, false, isPlatformClass(property)) + " values = state." + propertyName + ";\n" +
                                    "                if (values != null)\n" +
                                    "                {\n" +
                                    "                    values.removeValue(" + typeConversionInSetter(property, propertyReturnGenericType, imports, "valueToRemove", true, true, true) + ");\n" +
                                    "                }\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        toOneProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                synchronized (state)\n" +
                                    "                {\n" +
                                    "                    CoreInstance current = state." + propertyName + ";\n" +
                                    "                    if ((current != null) && current.equals(valueToRemove))\n" +
                                    "                    {\n" +
                                    "                        state." + propertyName + " = null;\n" +
                                    "                    }\n" +
                                    "                }\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                throw new IllegalArgumentException(\"Unknown property '\" + keyName + \"'\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> newValues)\n" +
                        "    {\n" +
                        "        String keyName = key.getLast();\n" +
                        "        if (newValues == null)\n" +
                        "        {\n" +
                        "            removeProperty(keyName);\n" +
                        "            return;\n" +
                        "        }\n" +
                        "        this.prepareForWrite();\n" +
                        "        _State state = this.getState();\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            String expression = "newValues";
                            CoreInstance propertyGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            if (!isAnyOrNilTypeProperty(propertyGenericType) && !isStubType(property, propertyGenericType))
                            {
                                expression = "(" + expression + " == null) ? null : " + typeConversionInSetter(property, propertyGenericType, imports, expression, true, false, true);
                            }
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                state.init_" + propertyName + "().setValues(" + expression + ");\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        toOneProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                if (newValues.size() > 1)\n" +
                                    "                {\n" +
                                    "                    throw new IllegalArgumentException(\"Cannot set \" + newValues.size() + \" values for to-one property '\" + keyName + \"'\");\n" +
                                    "                }\n" +
                                    "                synchronized (state)\n" +
                                    "                {\n" +
                                    "                    state." + propertyName + " = " + typeConversionInSetter(property, propertyReturnGenericType, imports, "newValues.getFirst()", false, true, true) + ";\n" +
                                    "                }\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                throw new IllegalArgumentException(\"Unknown property '\" + keyName + \"'\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void addKeyValue(ListIterable<String> key, CoreInstance value)\n" +
                        "    {\n" +
                        "        String keyName = key.getLast();\n" +
                        "        this.prepareForWrite();\n" +
                        "        _State state = this.getState();\n" +
                        "        switch (keyName)\n" +
                        "        {\n" +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                state.init_" + propertyName + "().addValue(" + typeConversionInSetter(property, propertyGenericType, imports, "value", true, true, true) + ");\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        toOneProperties.collect(property ->
                        {
                            CoreInstance propertyGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "            case \"" + property.getName() + "\":\n" +
                                    "            {\n" +
                                    "                synchronized (state)\n" +
                                    "                {\n" +
                                    "                    if (state." + propertyName + " != null)\n" +
                                    "                    {\n" +
                                    "                        throw new IllegalArgumentException(\"Cannot add value for to-one property '\" + keyName + \"': value already present\");\n" +
                                    "                    }\n" +
                                    "                    state." + propertyName + " = " + typeConversionInSetter(property, propertyGenericType, imports, "value", false, true, true) + ";\n" +
                                    "                }\n" +
                                    "                break;\n" +
                                    "            }\n";
                        }).makeString("") +
                        "            default:\n" +
                        "            {\n" +
                        "                throw new IllegalArgumentException(\"Unknown property '\" + keyName + \"'\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void removeProperty(String keyName)\n" +
                        "    {\n" +
                        "        this.prepareForWrite();\n" +
                        "        _State state = this.getState();\n" +
                        "        synchronized (state)\n" +
                        "        {\n" +
                        "            switch (keyName)\n" +
                        "            {\n" +
                        properties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "                case \"" + property.getName() + "\":\n" +
                                    "                {\n" +
                                    "                    state." + propertyName + " = null;\n" +
                                    "                    break;\n" +
                                    "                }\n";
                        }).makeString("") +
                        "                default:\n" +
                        "                {\n" +
                        "                    throw new IllegalArgumentException(\"Unknown property '\" + keyName + \"'\");\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public ListIterable<String> getRealKeyByName(String name)\n" +
                        "    {\n" +
                        "        ListIterable<String> realKeys = null;\n" +
                        (properties.isEmpty() ? "" :
                                "        switch (name)\n" +
                                        "        {\n" +
                                        properties.collect(property ->
                                        {
                                            String propertyName = property.getName();
                                            CoreInstance propertyOwner = propertyOwners.get(propertyName);
                                            return "            case \"" + propertyName + "\":\n" +
                                                    "                realKeys = " + createPropertyKeyNameReference(propertyName, propertyOwner, instance) + ";\n" +
                                                    "                break;\n";
                                        }).makeString("") +
                                        "\n" +
                                        "            default:\n" +
                                        "                throw new RuntimeException(\"Unsupported key: \" + name);\n" +
                                        "        }\n") +
                        "        return realKeys;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public CoreInstance getKeyByName(String name)\n" +
                        "    {\n" +
                        "        CoreInstance key = null;\n" +
                        "\n" +
                        (properties.isEmpty() ? "" :
                                "        switch (name)\n" +
                                        "        {\n" +
                                        properties.collect(property ->
                                        {
                                            String propertyName = property.getName();
                                            return "            case \"" + propertyName + "\":\n" +
                                                    "                if (this." + propertyName + "Key == null)\n" +
                                                    "                {\n" +
                                                    "                    this." + propertyName + "Key = this.getRepository().resolve(" + createPropertyKeyNameReference(propertyName, propertyOwners.get(propertyName), instance) + ");\n" +
                                                    "                }\n" +
                                                    "                key = this." + propertyName + "Key;\n" +
                                                    "                break;\n";
                                        }).makeString("") +
                                        "            default:\n" +
                                        "                throw new RuntimeException(\"Unsupported key: \" + name);\n" +
                                        "        }\n" +
                                        "\n") +
                        "        return key;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public RichIterable<String> getKeys()\n" +
                        "    {\n" +
                        "        MutableList<String> result = Lists.mutable.of();\n" +
                        "        for (String key: this.keys)\n" +
                        "        {\n" +
                        "            if (isValueDefinedForKey(key))\n" +
                        "            {\n" +
                        "                result.add(key);\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return result;\n" +
                        "    }\n" +
                        "\n" +
                        properties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createClassPropertyGetter(interfaceName, property, propertyReturnGenericType, imports);
                        }).makeString("") +
                        "\n" +
                        properties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createClassPropertySetter(interfaceName, property, propertyReturnGenericType, imports, maybeFullyQualifiedName, "");
                        }).makeString("") +
                        "\n" +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createClassSetPropertyValueAt(property, propertyGenericType, imports);
                        }).makeString("") +
                        propertiesFromAssociations.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createPropertyReverse(property, propertyReturnGenericType, imports, false, false) + createPropertyReverse(property, propertyReturnGenericType, imports, true, false);
                        }).makeString("") +
                        "\n" +
                        qualifiedProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createClassQualifiedPropertyGetter(property, propertyReturnGenericType, imports);
                        }).makeString("") +
                        "    private static class _State extends AbstractCoreInstanceMutableState\n" +
                        "    {\n" +
                        toOneProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String type = getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass(property));
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "        private " + type + " " + propertyName + ";\n";
                        }).makeString("") +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String type = getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass(property));
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "        private " + type + " " + propertyName + ";\n";
                        }).makeString("") +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String propertyTypeInternal = getPropertyTypeInternal(property, propertyReturnGenericType, imports, false, false, isPlatformClass(property));
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "        private " + propertyTypeInternal + " init_" + propertyName + "()\n" +
                                    "        {\n" +
                                    "            " + propertyTypeInternal + " values = this." + propertyName + ";\n" +
                                    "            if (values == null)\n" +
                                    "            {\n" +
                                    "                synchronized (this)\n" +
                                    "                {\n" +
                                    "                    if (this." + propertyName + " == null)\n" +
                                    "                    {\n" +
                                    "                        this." + propertyName + " = newToManyPropertyValues();\n" +
                                    "                    }\n" +
                                    "                    values = this." + propertyName + ";\n" +
                                    "                }\n" +
                                    "            }\n" +
                                    "            return values;\n" +
                                    "        }\n";
                        }).makeString("\n", "\n", "\n") +
                        "        private _State copy()\n" +
                        "        {\n" +
                        "            synchronized (this)\n" +
                        "            {\n" +
                        "                final _State copy = new _State();\n" +
                        toOneProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "                if (this." + propertyName + " != null)\n" +
                                    "                {\n" +
                                    "                    copy." + propertyName + " = this." + propertyName + ";\n" +
                                    "                }\n";
                        }).makeString("") +
                        toManyProperties.collect(property ->
                        {
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            return "                if (this." + propertyName + " != null)\n" +
                                    "                {\n" +
                                    "                    copy." + propertyName + " = this." + propertyName + ".copy();\n" +
                                    "                }\n";
                        }).makeString("") +
                        "\n" +
                        "                copy.setCompileStateBitSet(getCompileStateBitSet());\n" +
                        "                return copy;\n" +
                        "            }\n" +
                        "        }\n" +
                        "\n" +
                        "    }\n" +
                        "    public static " + className + " createPersistent(ModelRepository repository, String name, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation" + mandatoryTypeString + ")\n" +
                        "    {\n" +
                        "        CoreInstance classifier = getUserPath(Lists.immutable.with(" + pathString + "), repository);\n" +
                        "        " + className + " instance = name == null ? (" + className + ")repository.newAnonymousCoreInstance(sourceInformation, classifier, true, FACTORY)\n" +
                        "                : (" + className + ")repository.newCoreInstance(name, classifier, sourceInformation, FACTORY);\n" +
                        (mandatoryToOneProps.isEmpty() ? "" :
                                "        instance.prepareForWrite();\n" +
                                        "        _State state = instance.getState();\n") +
                        mandatoryToOneProps.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
                            if (isPrimitiveTypeProperty(propertyReturnGenericType))
                            {
                                String type = Objects.requireNonNull(getTypeFromGenericType(propertyReturnGenericType)).getName();
                                return "        state." + propertyName + " = " + propertyName + " == null ? null : repository.new" + type + "CoreInstance" + (ModelRepository.STRING_TYPE_NAME.equals(type) ? "_cached(" : "(") + propertyName + ");\n";
                            }
                            else
                            {
                                return "        state." + propertyName + " = " + propertyName + ";\n";
                            }
                        }).makeString("") +
                        "        return instance;\n" +
                        "    }\n" +
                        "    public static " + className + " createPersistent(ModelRepository repository, String name" + mandatoryTypeString + ")\n" +
                        "    {\n" +
                        "        return createPersistent(repository, name, null" + mandatoryPropertyNames + ");\n" +
                        "    }\n" +
                        "    public static " + className + " createPersistent(ModelRepository repository, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation" + mandatoryTypeString + ")\n" +
                        "    {\n" +
                        "        return createPersistent(repository, null, sourceInformation" + mandatoryPropertyNames + ");\n" +
                        "    }\n" +
                        "    public static " + className + " createPersistent(ModelRepository repository" + mandatoryTypeString + ")\n" +
                        "    {\n" +
                        "        return createPersistent(repository, null, null" + mandatoryPropertyNames + ");\n" +
                        "    }\n" +
                        "    private static CoreInstance getUserPath(ListIterable<String> path, ModelRepository repository)\n" +
                        "    {\n" +
                        "        // TODO - this is inefficient do it once, should we use Context?\n" +
                        "        if (path.isEmpty())\n" +
                        "        {\n" +
                        "            return null;\n" +
                        "        }\n" +
                        "\n" +
                        "        CoreInstance child = repository.getTopLevel(path.get(0));\n" +
                        "        for (String childName : path.asLazy().drop(1))\n" +
                        "        {\n" +
                        "            child = ((PackageInstance)child).getValueInValueForMetaPropertyToManyByIndex(\"children\", org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications.getCoreInstanceNameIndexSpec(), childName).getFirst();\n" +
                        "        }\n" +
                        "        return child;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean supports(CoreInstance classifier)\n" +
                        "    {\n" +
                        "        return FACTORY.supports(classifier);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public CoreInstance createCoreInstance(String name, int internalSyntheticId, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CoreInstance classifier, ModelRepository repository, boolean persistent)\n" +
                        "    {\n" +
                        "        return FACTORY.createCoreInstance(name, internalSyntheticId, sourceInformation, classifier, repository, persistent);\n" +
                        "    }\n" +
                        "\n" +
                        createClassCopyMethod(className, interfaceNamePlusTypeParams) +
                        createClassPackageableElement(systemPathForPackageableElement) +
                        (isEnum ? enumToStringAndCompareOverrides(javaPackage + "." + interfaceName) : "") +
                        "}\n";

        return value;
    }


    public static String enumToStringAndCompareOverrides(String fullJavaTypeName)
    {
        return "    @Override\n" +
                "    public int compareTo(" + fullJavaTypeName + " o)\n" +
                "    {\n" +
                "        return this.getName().compareTo(o.getName());\n" +
                "    }\n";
    }

    public static String createClassConstructors(String className)
    {
        return "    protected " + className + "(String name, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent)\n" +
                "    {\n" +
                "        super(name, sourceInformation, classifier, internalSyntheticId, repository, persistent);\n" +
                "        this.state = new _State();\n" +
                "    }\n" +
                "\n" +
                "    protected " + className + "(" + className + " instance)\n" +
                "    {\n" +
                "        this(instance.getName(), (org.finos.legend.pure.m4.coreinstance.SourceInformation)null, instance.getClassifier(), -1, instance.getRepository(), false);\n" +
                "        this.state = instance.state.copy();\n" +
                "    }\n";
    }

    public static String createClassFactory(String className, String fullUserPath)
    {
        return "    public static final CoreInstanceFactory FACTORY = new BaseM3CoreInstanceFactory()\n" +
                "    {\n" +
                "        @Override\n" +
                "        public CoreInstance createCoreInstance(String name, int internalSyntheticId, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CoreInstance classifier, ModelRepository repository, boolean persistent)\n" +
                "        {\n" +
                "            return new " + className + "(name, sourceInformation, classifier, internalSyntheticId, repository, persistent);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public boolean supports(String classifierPath)\n" +
                "        {\n" +
                "            return \"" + fullUserPath + "\".equals(classifierPath);\n" +
                "        }\n" +
                "    };\n";
    }

    public static String createClassCopyMethod(String className, String interfaceNamePlusTypeParams)
    {
        return "    @Override\n" +
                "    public " + interfaceNamePlusTypeParams + " copy()\n" +
                "    {\n" +
                "        return new " + className + "(this);\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperClassCopyMethod(CoreInstance _class, String interfaceName)
    {
        String typeParams = getTypeParams(_class, true);
        String interfaceNamePlusTypeParams = interfaceName + (typeParams.isEmpty() ? "" : typeParams);
        String className = getWrapperName(_class);

        return "    @Override\n" +
                "    public " + interfaceNamePlusTypeParams + " copy()\n" +
                "    {\n" +
                "        return new " + className + "(((AbstractCoreInstance)this.instance).copy());\n" +
                "    }\n" +
                "\n";
    }

    private MutableMap<String, CoreInstance> collectPropertyOwnersAndImportsForGeneralizations(CoreInstance instance, MutableSet<CoreInstance> properties, Imports imports)
    {
        CoreInstance classGenericType = getClassGenericType(instance);
        MutableMap<String, CoreInstance> propertyOwners = Maps.mutable.of();
        for (CoreInstance property : properties)
        {
            CoreInstance owner = property.getValueForMetaPropertyToOne("owner");
            propertyOwners.put(property.getName(), owner == null ? instance : owner);
        }

        ListIterable<? extends CoreInstance> generalizations = instance.getValueForMetaPropertyToMany("generalizations");

        if (!generalizations.isEmpty())
        {
            MutableSet<CoreInstance> propertiesFromGeneralizations = Sets.mutable.of();
            collectGeneralizationProperties(generalizations, propertiesFromGeneralizations, propertyOwners);
            imports.addImports(getPropertyTypePackages(classGenericType, propertiesFromGeneralizations, Sets.mutable.empty(), true));
            properties.addAll(propertiesFromGeneralizations);
        }
        return propertyOwners;
    }

    private String createPropertyKeyNameReference(String propertyName, CoreInstance propertyOwner, CoreInstance _class)
    {
        CoreInstance owner = "Association".equals(propertyOwner.getClassifier().getName()) ? _class : propertyOwner;
        return getJavaPackageString(owner) + "." + getClassName(owner) + "." + createPropertyKeyName(propertyName);
    }

    private String createPropertyKeyName(String propertyName)
    {
        return propertyName.toUpperCase() + "_PROPERTY_KEY";
    }

    private String createClassPropertySetterToMany(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        return createClassPropertySetterAdd(property, propertyReturnGenericType, imports, interfaceName) +
                createClassPropertySetterAddAll(property, propertyReturnGenericType, imports, interfaceName) +
                createClassPropertySetterRemover(property, propertyReturnGenericType, imports, interfaceName, getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false, "Object"));
    }

    private String createClassPropertySetterAdd(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        boolean isPlatformClass = isPlatformClass(property);
        return createClassPropertySetterAdders_internal(property, propertyReturnGenericType, imports, interfaceName, "Add", getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, isPlatformClass, true, "Object"), "value", "addValue", true);
    }

    private String createClassPropertySetterAddAll(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        boolean isPlatformClass = isPlatformClass(property);
        return createClassPropertySetterAdders_internal(property, propertyReturnGenericType, imports, interfaceName, "AddAll", getPropertyTypeExternal(property, propertyReturnGenericType, imports, false, false, isPlatformClass, true, "Object"), "values", "addValues", false);
    }

    private String createClassPropertySetterAdders_internal(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName, String functionSuffix, String argumentType, String argumentName, String addFunctionName, boolean argumentIsToOne)
    {
        String expression = argumentName;
        String propertyStateProperty = getPropertyNameAsValidJavaIdentifierSwitchName(property);

        String unifiedMethodName = getUnifiedMethodName(property) + functionSuffix;
        if (isStubType(property, propertyReturnGenericType))
        {
            boolean isSingularAdd = "Add".equals(functionSuffix);
            String stubArgumentType = isSingularAdd ? "CoreInstance" : "RichIterable<? extends CoreInstance>";
            String optionalCast = isSingularAdd ? "" : "(ListIterable<CoreInstance>) ";
            return "    public " + interfaceName + " " + unifiedMethodName + "(" + argumentType + " " + argumentName + ")\n" +
                    "    {\n" +
                    "        this." + unifiedMethodName + "CoreInstance(" + argumentName + ");\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + interfaceName + " " + unifiedMethodName + "CoreInstance(" + stubArgumentType + " " + argumentName + ")\n" +
                    "    {\n" +
                    "        prepareForWrite();\n" +
                    "        this.getState().init_" + propertyStateProperty + "()." + addFunctionName + "(" + optionalCast + expression + ");\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n";
        }
        else
        {
            if (isPrimitiveTypeProperty(propertyReturnGenericType))
            {
                expression = primitiveCoreInstanceFromPrimitive(property, propertyReturnGenericType, expression, false, argumentIsToOne);
            }
            else if (isAnyTypeProperty(propertyReturnGenericType))
            {
                expression = applyFunction2WithCardinality(property, "AnyHelper.WRAP_PRIMITIVES", expression, false, argumentIsToOne);
            }
            else if (isNilTypeProperty(propertyReturnGenericType))
            {
                expression = (argumentIsToOne ? "(CoreInstance) " : "(ListIterable<? extends CoreInstance>) ") + expression;
            }

            return "    public " + interfaceName + " " + unifiedMethodName + "(" + argumentType + " " + argumentName + ")\n" +
                    "    {\n" +
                    "        prepareForWrite();\n" +
                    "        this.getState().init_" + propertyStateProperty + "()." + addFunctionName + "(" + expression + ");\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n";
        }
    }

    private String createClassPropertySetterRemover(CoreInstance property,
                                                    CoreInstance propertyReturnGenericType, Imports imports, String interfaceName, String argumentType)
    {
        String expression = "value";
        String statePropertyExpression = "this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property);
        String propertyTypeInternal = getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass(property));
        if (isStubType(property, propertyReturnGenericType))
        {
            String methodName = getUnifiedMethodName(property) + "Remove";
            return "    public " + interfaceName + " " + methodName + "(" + argumentType + " value)\n" +
                    "    {\n" +
                    "        this." + methodName + "CoreInstance(value);\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + interfaceName + " " + methodName + "CoreInstance(CoreInstance value)\n" +
                    "    {\n" +
                    "        prepareForWrite();\n" +
                    "        " + propertyTypeInternal + " values = " + statePropertyExpression + ";\n" +
                    "        if (values != null)\n" +
                    "        {\n" +
                    "            values.removeValue(" + expression + ");\n" +
                    "        }\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n";
        }
        else
        {
            String cast = isAnyOrNilTypeProperty(propertyReturnGenericType) ? "(CoreInstance)" : "";
            if (isPrimitiveTypeProperty(propertyReturnGenericType))
            {
                expression = primitiveCoreInstanceFromPrimitive(property, propertyReturnGenericType, expression, false, true);
            }
            return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "Remove(" + argumentType + " value)\n" +
                    "    {\n" +
                    "        prepareForWrite();\n" +
                    "        " + propertyTypeInternal + " values = " + statePropertyExpression + ";\n" +
                    "        if (values != null)\n" +
                    "        {\n" +
                    "            values.removeValue" + "(" + cast + expression + ");\n" +
                    "        }\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n";
        }
    }

    private String createClassSetPropertyValueAt(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        String propertyTypeInternal = getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass(property));
        String newExpression = "value";
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            newExpression = primitiveCoreInstanceFromPrimitive(property, propertyReturnGenericType, newExpression, false, true);
        }
        else
        {
            newExpression = typeConversionInSetter(property, propertyReturnGenericType, imports, newExpression, true, true, true);
        }
        String propertyStateProperty = getPropertyNameAsValidJavaIdentifierSwitchName(property);
        return "    public void set" + getMethodName(property) + "ValueAt(int offset, " + getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false) + " value)\n" +
                "    {\n" +
                "        this.prepareForWrite();\n" +
                "        " + propertyTypeInternal + " values = this.getState()." + propertyStateProperty + ";\n" +
                "        if (values == null)\n" +
                "        {\n" +
                "            throw new IllegalArgumentException(\"Cannot modify value at offset \" + offset + \" for property '" + property.getName() + "'\");\n" +
                "        }\n" +
                "        values.setValue(offset, " + newExpression + ");\n" +
                "    }\n" +
                "\n";
    }

    private String typeConversionInSetter(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String expression, boolean override, boolean isToOne, boolean convertPrimitives)
    {
        String newExpression = expression;
        boolean isConcrete = propertyReturnGenericType.getValueForMetaPropertyToMany("typeParameter").isEmpty();

        if (isConcrete &&
                !"CoreInstance".equals(getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne, false, isPlatformClass(property))) &&
                (convertPrimitives || !isPrimitiveTypeProperty(propertyReturnGenericType)))
        {
            newExpression = fromCoreInstance(property, propertyReturnGenericType, imports, newExpression, override ? isToOne : isToOne(property));
        }

        return newExpression;
    }

    private static String getClassName(CoreInstance instance)
    {
        return instance.getName() + CLASS_SUFFIX;
    }

    private String fromCoreInstanceGettor(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String expression)
    {
        String newExpression = expression;
        boolean isToOne = isToOne(property);
        if (isAnyType(getTypeFromGenericType(propertyReturnGenericType)))
        {
            newExpression = applyFunctionWithCardinality(property, "AnyHelper.UNWRAP_PRIMITIVES", newExpression, isToOne);
        }
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            newExpression = applyFunctionWithCardinality(property, primitiveFromCoreInstanceFn(propertyReturnGenericType), newExpression, isToOne);
            String type = Objects.requireNonNull(getTypeFromGenericType(propertyReturnGenericType)).getName();
            if ("Integer".equals(type))
            {
                newExpression = nullSafe(newExpression, newExpression + ".longValue()");
            }
            else if ("Float".equals(type))
            {
                newExpression = nullSafe(newExpression, newExpression + ".doubleValue()");
            }
        }
        else
        {
            newExpression = applyFunctionWithCardinality(property, typeFromCoreInstanceFn(property, propertyReturnGenericType, imports), newExpression, isToOne);
        }
        return newExpression;
    }

    private String fromCoreInstance(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String expression, boolean isToOne)
    {
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            String cast = getPrimitiveClass(propertyReturnGenericType);
            if (!isToOne)
            {
                cast = "RichIterable<? extends " + cast + ">";
            }
            return "(" + cast + ")" + expression;
        }
        else
        {
            return applyFunctionWithCardinality(property, typeFromCoreInstanceFn(property, propertyReturnGenericType, imports), expression, isToOne);
        }
    }

    private String createClassPropertySetter(String className, CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName, String typeParams)
    {
        boolean isPlatformClass = isPlatformClass(property);
        boolean isToOne = isToOne(property);
        String parameterName = isToOne(property) ? "value" : "values";
        String expression = parameterName;
        String propertyStateName = getPropertyNameAsValidJavaIdentifierSwitchName(property);

        if (isAnyTypeProperty(propertyReturnGenericType))
        {
            expression = applyFunction2WithCardinality(property, "AnyHelper.WRAP_PRIMITIVES", expression, false, isToOne);
        }
        else if (isNilTypeProperty(propertyReturnGenericType))
        {
            expression = "(" + (isToOne ? "CoreInstance" : "RichIterable<? extends CoreInstance>") + ") " + expression;
        }

        if (!getPropertyTypeInternal(property, propertyReturnGenericType, imports, true, false, isPlatformClass(property)).equals(getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false)))
        {
            expression = toCoreInstance(property, propertyReturnGenericType, expression, false);
        }

        String type = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass, true, "Object");
        String typeToOneNoGenerics = isToOne ? type : getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, true, "Object");
        boolean isStubProperty = !isOwningClass(className, typeToOneNoGenerics) && isStubType(property, propertyReturnGenericType);
        String unifiedMethodName = getUnifiedMethodName(property);

        return "    public " + interfaceName + typeParams + " " + getUnifiedMethodName(property) + "(" + type + " " + parameterName + ")\n" +
                "    {\n" +
                (isStubProperty ?
                        "        this." + unifiedMethodName + "CoreInstance(" + parameterName + ");\n" +
                                "        return this;\n" :
                        (isToOne ?
                                "" :
                                "        if (" + parameterName + " == null)\n" +
                                        "        {\n" +
                                        "            return " + unifiedMethodName + "Remove();\n" +
                                        "        }\n") +
                                "        this.prepareForWrite();\n" +
                                (isToOne ?
                                        ("        _State state = this.getState();\n" +
                                                "        synchronized (state)\n" +
                                                "        {\n" +
                                                "            state." + propertyStateName + " = " + expression + ";\n" +
                                                "        }\n") :
                                        ("        this.getState().init_" + propertyStateName + "().setValues(" + expression + ");\n")) +
                                "        return this;\n") +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + typeParams + " " + unifiedMethodName + "Remove()\n" +
                "    {\n" +
                "        this.prepareForWrite();\n" +
                "        _State state = this.getState();\n" +
                "        synchronized (state)\n" +
                "        {\n" +
                "            state." + propertyStateName + " = null;\n" +
                "        }\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                (isToOne ? "" : createClassPropertySetterToMany(property, propertyReturnGenericType, imports, interfaceName)) +
                (isStubProperty ? createClassPropertyStubSetter(property, interfaceName, typeParams, parameterName, expression) : "");
    }

    private static String createClassPropertyStubSetter(CoreInstance property, String className, String typeParams, String parameterName, String expression)
    {
        String propertyStateName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
        boolean isToOne = isToOne(property);
        return "    public " + className + typeParams + " " + getUnifiedMethodName(property) + "CoreInstance(" + (isToOne(property) ? "CoreInstance " : "RichIterable<? extends CoreInstance> ") + parameterName + ")\n" +
                "    {\n" +
                "        if (" + parameterName + " == null)\n" +
                "        {\n" +
                "            return " + getUnifiedMethodName(property) + "Remove();\n" +
                "        }\n" +
                "        prepareForWrite();\n" +
                (isToOne ?
                        "        this.getState()." + propertyStateName + " = " + expression :
                        "        this.getState().init_" + propertyStateName + "().setValues(" + expression + ")") + ";\n" +
                "        return this;\n" +
                "    }\n\n";
    }

    private String toCoreInstance(CoreInstance property, CoreInstance propertyReturnGenericType, String expression, boolean isWrapper)
    {
        String newExpression = expression;
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            newExpression = primitiveCoreInstanceFromPrimitive(property, propertyReturnGenericType, expression, isWrapper, isToOne(property));
        }
        return newExpression;
    }

    private String createClassPropertyGetter(String className, CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean isPlatformClass = isPlatformClass(property);
        boolean isToOne = isToOne(property);
        boolean isStub = isStubType(property, propertyReturnGenericType);
        String stubType = getSubstituteType(property, propertyReturnGenericType);
        String propertyTypeExternal = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne, false, isPlatformClass, false, "Object");
        String propertyTypeInternal = getPropertyTypeInternal(property, propertyReturnGenericType, imports, isToOne, false, isPlatformClass);
        String propertyTypeExternalToOneNoGenerics = isToOne ? propertyTypeExternal : getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false);

        boolean isOwningClass = isOwningClass(className, propertyTypeExternalToOneNoGenerics);

        CoreInstance propertyReturnRawType = getTypeFromGenericType(propertyReturnGenericType);

        String expression;
        if (!isOwningClass && isStub)
        {
            expression = "return " + fromCoreInstance(property, propertyReturnGenericType, imports, applyFunctionWithCardinality(property, stubType + "Helper.FROM_STUB_FN", "this." + getUnifiedMethodName(property) + "CoreInstance()", isToOne), isToOne);
        }
        else if (isAnyType(propertyReturnRawType))
        {
            stubType = "AnyStub";
            expression = "return " + applyFunctionWithCardinality(property, "Functions.chain(" + stubType + "Helper.FROM_STUB_FN, AnyHelper.UNWRAP_PRIMITIVES)", "this." + getUnifiedMethodName(property) + "CoreInstance()", isToOne);
        }
        else if (isToOne)
        {
            if (isMandatoryProperty(property) && isPrimitiveTypeProperty(propertyReturnGenericType) && ("Integer".equals(Objects.requireNonNull(getTypeFromGenericType(propertyReturnGenericType)).getName())
                    || "Float".equals(Objects.requireNonNull(getTypeFromGenericType(propertyReturnGenericType)).getName())))
            {
                expression = propertyTypeInternal + " value = this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ";\n" +
                        "        if (value == null)\n" +
                        "        {\n" +
                        "            throw new PureCompilationException(this.getSourceInformation(), \"'" + property.getName() + "' is a mandatory property\");\n" +
                        "        }\n" +
                        "        return value.getValue()." + PRIMITIVES_EXTERNAL.get(Objects.requireNonNull(getTypeFromGenericType(propertyReturnGenericType)).getName()) + "Value()";
            }
            else
            {
                expression = "this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property);
                if (!propertyTypeInternal.equals(propertyTypeExternal))
                {
                    expression = fromCoreInstanceGettor(property, propertyReturnGenericType, imports, expression);
                }
                expression = "return " + expression;
            }
        }
        else
        {
            String toOnePropertyTypeInternal = getPropertyTypeInternal(property, propertyReturnGenericType, imports, true, false, isPlatformClass);
            String toOnePropertyTypeExternal = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, isPlatformClass, false);
            expression = "values.getValues()";
            if (!toOnePropertyTypeInternal.equals(toOnePropertyTypeExternal))
            {
                expression = fromCoreInstanceGettor(property, propertyReturnGenericType, imports, expression);
            }
            expression = propertyTypeInternal + " values = this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property) + ";\n" +
                    "        return (values == null) ? Lists.immutable.<" + toOnePropertyTypeExternal + ">empty() : " + expression;
        }

        return "    public " + propertyTypeExternal + " " + getUnifiedMethodName(property) + "()\n" +
                "    {\n" +
                "        " + expression + ";\n" +
                "    }\n" +
                "\n" +
                (requiresCoreInstanceMethods(className, propertyTypeExternalToOneNoGenerics, property, propertyReturnGenericType) ? this.createClassPropertyCoreInstanceGetter(property) : "");
    }

    private String createClassQualifiedPropertyGetter(CoreInstance qualifiedProperty, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean qualifiedPropertyIsPlatform = isPlatformClass(qualifiedProperty);
        boolean isToOne = isToOne(qualifiedProperty);
        return "    public " + getPropertyTypeExternal(qualifiedProperty, propertyReturnGenericType, imports, isToOne, qualifiedPropertyIsPlatform, qualifiedPropertyIsPlatform, true, "Object") + " " + getQualifiedPropertyMethodName(qualifiedProperty) + "(" + getQualifiedPropertyParameters(qualifiedProperty, imports) + ")\n" +
                "    {\n" +
                "        " + M3_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createClassPropertyCoreInstanceGetter(CoreInstance property)
    {
        String statePropertyExpression = "this.getState()." + getPropertyNameAsValidJavaIdentifierSwitchName(property);
        String type;
        String expression;
        if (isToOne(property))
        {
            type = "CoreInstance";
            expression = "return " + statePropertyExpression;
        }
        else
        {
            type = "RichIterable<? extends CoreInstance>";
            expression = "ToManyPropertyValues<? extends CoreInstance> values = " + statePropertyExpression + ";\n" +
                    "        return (values == null) ? Lists.immutable.<CoreInstance>empty() : values.getValues()";
        }
        return "    public " + type + " " + getUnifiedMethodName(property) + "CoreInstance()\n" +
                "    {\n" +
                "        " + expression + ";\n" +
                "    }\n" +
                "\n";
    }

    private Imports getPropertyTypePackages(CoreInstance classGenericType, MutableSet<CoreInstance> properties, MutableSet<CoreInstance> qualifiedProperties, boolean includeWrapper)
    {
        MutableSet<String> propertyTypePackages = Sets.mutable.of(
                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub",
                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStubCoreInstanceWrapper",
                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub",
                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub",
                "org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub",
                "org.finos.legend.pure.m3.coreinstance.PackageInstance",
                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement");

        this.addTypesForProperties(classGenericType, properties, includeWrapper, propertyTypePackages, false);
        this.addTypesForProperties(classGenericType, qualifiedProperties, includeWrapper, propertyTypePackages, true);
        propertyTypePackages.remove("");

        Imports imports = new Imports();
        imports.addImports(propertyTypePackages);
        return imports;
    }

    private void addTypesForProperties(CoreInstance classGenericType, MutableSet<CoreInstance> properties, boolean includeWrapper, MutableSet<String> propertyTypePackages, boolean qualified)
    {
        if (qualified && !properties.isEmpty())
        {
            propertyTypePackages.add("org.finos.legend.pure.m3.execution.ExecutionSupport");
        }

        for (CoreInstance property : properties)
        {
            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
            CoreInstance type = getTypeFromGenericType(propertyReturnGenericType);
            String typeName = getTypeNameWithStringForPrimitives(propertyReturnGenericType, null);
            if (type != null && !isPrimitiveTypeProperty(propertyReturnGenericType) && !"CoreInstance".equals(typeName)
                    && !isEnum(type))
            {
                propertyTypePackages.add(getJavaPackageString(type) + "." + typeName);
                if (includeWrapper)
                {
                    propertyTypePackages.add(getJavaPackageString(type) + "." + typeName + WRAPPER_CLASS_SUFFIX);
                    propertyTypePackages.add(getJavaPackageString(type) + "." + typeName + CLASS_SUFFIX);
                }
                if (qualified)
                {
                    propertyTypePackages.addAll((Collection<String>) getQualifiedPropertyParamTypesForImports(property, new Imports()));
                }
            }
        }
    }

    private static boolean isEnum(CoreInstance instance)
    {
        return instance != null && "Enumeration".equals(instance.getClassifier().getName());
    }

    private RichIterable<Pair<String, String>> buildMandatoryProperties(final CoreInstance classGenericType, RichIterable<CoreInstance> properties, final Imports imports)
    {
        return properties.collect(property ->
        {
            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
            String propertyName = getPropertyNameAsValidJavaIdentifierSwitchName(property);
            return Tuples.pair(propertyName, getTypeForSetter(property, propertyReturnGenericType, imports));
        });
    }

    private String createAccessorInterface(final CoreInstance instance, String javaPackage, MutableSet<CoreInstance> properties, MutableSet<CoreInstance> qualifiedProperties, final Imports imports)
    {
        final String interfaceName = getInterfaceName(instance);
        String accessorInterfaceName = getAccessorInterfaceName(instance);
        String typeParams = getTypeParams(instance, false);
        final CoreInstance classGenericType = getClassGenericType(instance);

        ListIterable<? extends CoreInstance> generalizations = instance.getValueForMetaPropertyToMany("generalizations");

        imports.addImports(generalizations.collect(generalization ->
        {
            CoreInstance generalType = getTypeFromGenericType(generalization.getValueForMetaPropertyToOne("general"));
            return (generalType == null) ? "" : getJavaPackageString(generalType) + "." + getAccessorInterfaceName(generalType);
        }, Sets.mutable.empty()));

        String generalizationsString = "CoreInstance"; /*generalizations.collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance generalization)
            {
                CoreInstance generalGenericType = generalization.getValueForMetaPropertyToOne("general");
                CoreInstance generalType = getTypeFromGenericType(generalGenericType);
                return getAccessorInterfaceName(generalType) + getTypeArgs(generalType, generalGenericType, false, true);
            }
        }).distinct().makeString(", ");*/

        return "package " + javaPackage + ";\n" +
                "\n" +
                "\n" +
                "import org.eclipse.collections.api.RichIterable;\n" +
                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                imports.toImportString() +
                "\n" +
                "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                "import org.finos.legend.pure.m4.coreinstance.primitive.*;\n" +
                "\n" +
                "public interface " + accessorInterfaceName + typeParams + " extends " + generalizationsString + "\n" +
                "{\n" +
                properties.collect(property ->
                {
                    CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                    return createAccessorInterfacePropertyGetter(interfaceName, property, propertyReturnGenericType, imports);
                }).makeString("") +
                qualifiedProperties.collect(property ->
                {
                    CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                    return createAccessorInterfaceQualifiedPropertyGetter(property, propertyReturnGenericType, imports);
                }).makeString("") +
                "}\n";
    }

    private static String getAccessorInterfaceName(CoreInstance instance)
    {
        return instance.getName() + ACCESSOR_SUFFIX;
    }

    private String createAccessorInterfacePropertyGetter(String className, CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean genericsAllowed = isPlatformClass(property);
        boolean isToOne = isToOne(property);
        String propertyTypeExternal = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne, genericsAllowed, genericsAllowed, true, "Object");
        String propertyTypeExternalToOneNoGenerics = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, true, "Object");
        return "    " + propertyTypeExternal + " " + getUnifiedMethodName(property) + "();\n" +
                (requiresCoreInstanceMethods(className, propertyTypeExternalToOneNoGenerics, property, propertyReturnGenericType) ? "    " + getCoreInstanceType(property) + " " + getUnifiedMethodName(property) + "CoreInstance();\n" : "");
    }

    private String createAccessorInterfaceQualifiedPropertyGetter(CoreInstance qualifiedProperty, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean qualifiedPropertyIsPlatform = isPlatformClass(qualifiedProperty);
        boolean isToOne = isToOne(qualifiedProperty);
        return "    " + getPropertyTypeExternal(qualifiedProperty, propertyReturnGenericType, imports, isToOne, qualifiedPropertyIsPlatform, qualifiedPropertyIsPlatform, true, "Object") + " " + getQualifiedPropertyMethodName(qualifiedProperty) + "(" + getQualifiedPropertyParameters(qualifiedProperty, imports) + ");\n";
    }

    private String getQualifiedPropertyParameters(CoreInstance qualifiedProperty, Imports imports)
    {
        CoreInstance functionType = getFunctionType(qualifiedProperty);
        MutableList<String> params = functionType.getValueForMetaPropertyToMany("parameters").drop(1).collect(param ->
        {
            CoreInstance genericType = param.getValueForMetaPropertyToOne("genericType");
            CoreInstance multiplicity = param.getValueForMetaPropertyToOne("multiplicity");
            return getTypeExternalFromGenericType(genericType, multiplicity, imports, isToOne(param), false, isPlatformClass(param), true, "Object") + " " + getParameterName(param);
        }, Lists.mutable.empty());
        params.add("final ExecutionSupport es");
        return params.makeString(", ");
    }

    private Iterable<String> getQualifiedPropertyParamTypesForImports(CoreInstance qualifiedProperty, Imports imports)
    {
        CoreInstance functionType = getFunctionType(qualifiedProperty);
        return functionType.getValueForMetaPropertyToMany("parameters").drop(1).collect(param ->
        {
            CoreInstance genericType = param.getValueForMetaPropertyToOne("genericType");
            CoreInstance multiplicity = param.getValueForMetaPropertyToOne("multiplicity");
            String typeExternal = getTypeExternalFromGenericType(genericType, multiplicity, imports, true, false, false, false, "CoreInstance");
            return shouldntAddTypeToImports(genericType, typeExternal) ? "" : getJavaPackageString(param) + "." + typeExternal;
        });
    }

    private boolean shouldntAddTypeToImports(CoreInstance genericType, String propertyTypeExternal)
    {
        return isPrimitiveType(getTypeFromGenericType(genericType)) || "CoreInstance".equals(propertyTypeExternal);
    }

    private static CoreInstance getFunctionType(CoreInstance qualifiedProperty)
    {
        CoreInstance classifierGenericType = qualifiedProperty.getValueForMetaPropertyToOne("classifierGenericType");
        if (classifierGenericType != null)
        {
            ListIterable<? extends CoreInstance> typeArguments = classifierGenericType.getValueForMetaPropertyToMany("typeArguments");
            if (typeArguments.size() == 1)
            {
                return typeArguments.get(0).getValueForMetaPropertyToOne("rawType");
            }
        }
        return classifierGenericType == null ? qualifiedProperty.getValueForMetaPropertyToOne("genericType") : classifierGenericType;
    }

    public boolean requiresCoreInstanceMethods(String className, String propertyType, CoreInstance property, CoreInstance propertyGenericType)
    {
        return !isOwningClass(className, propertyType) && (isStubType(property, propertyGenericType) || isAnyOrNilTypeProperty(propertyGenericType));
    }

    public boolean requiresCoreInstanceMethods(CoreInstance property, CoreInstance propertyGenericType)
    {
        // Purely for Compiled mode generation
        return isStubType(property, propertyGenericType) || isAnyOrNilTypeProperty(propertyGenericType);
    }

    private static String getCoreInstanceType(CoreInstance property)
    {
        return isToOne(property) ? "CoreInstance" : "RichIterable<? extends CoreInstance>";
    }

//    private String createBuilderInterface(CoreInstance instance, String javaPackage, MutableSet<CoreInstance> properties, Imports imports)
//    {
//        String builderInterfaceName = getBuilderInterfaceName(instance) + getTypeParams(instance, false);
//        String interfaceName = getInterfaceName(instance) + getTypeParams(instance, false);
//
//        ListIterable<? extends CoreInstance> generalizations = instance.getValueForMetaPropertyToMany("generalizations");
//
//        imports.addImports(generalizations.collect(new Function<CoreInstance, String>()
//        {
//            @Override
//            public String valueOf(CoreInstance generalization)
//            {
//                CoreInstance generalType = getTypeFromGenericType(generalization.getValueForMetaPropertyToOne("general"));
//                return generalType == null ? "" : getJavaPackageString(generalType) + "." + getBuilderInterfaceName(generalType);
//            }
//        }).toSet());
//
//        String generalizationsString = "CoreInstance";
//
///*        if (!generalizations.isEmpty())
//        {
//            generalizationsString += ", " + generalizations.collect(new Function<CoreInstance, String>()
//            {
//                @Override
//                public String valueOf(CoreInstance generalization)
//                {
//                    CoreInstance generalGenericType = generalization.getValueForMetaPropertyToOne("general");
//                    CoreInstance generalType = getTypeFromGenericType(generalGenericType);
//                    return getBuilderInterfaceName(generalType) + getTypeArgs(generalType, generalGenericType, false, true);
//                }
//            }).distinct().makeString(", ");
//        }*/
//
//        MutableSet<CoreInstance> allProperties = Sets.mutable.ofAll(properties);
//        collectGeneralizationProperties(generalizations, allProperties, Maps.mutable.<String, CoreInstance>of());
//
//        CoreInstance classGenericType = getClassGenericType(instance);
//
//        Imports importsIncludingOtherProps = getPropertyTypePackages(classGenericType, allProperties, Sets.mutable.<CoreInstance>empty(),false);
//        importsIncludingOtherProps.addImports(imports);
//
//        return "package " + javaPackage + ";\n" +
//                "\n" +
//                "\n" +
//                "import org.eclipse.collections.api.RichIterable;\n" +
//                "import org.eclipse.collections.api.list.ListIterable;\n" +
//                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
//                importsIncludingOtherProps.toImportString() +
//                "\n" +
//                "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
//                "import org.finos.legend.pure.m4.coreinstance.primitive.*;\n" +
//                "\n" +
//                "public interface " + builderInterfaceName + " extends " + generalizationsString + "\n" +
//                "{\n" +
//                createBuilderInterfaceProperties(classGenericType, imports, interfaceName, allProperties, Sets.mutable.<CoreInstance>empty()) +
//                "}\n";
//
//    }

    private String createBuilderInterfaceProperties(final CoreInstance classGenericType, final Imports imports, final String interfaceNameWithGenerics, final String interfaceName, MutableSet<CoreInstance> allProperties, MutableSet<CoreInstance> propertiesFromAssociations)
    {
        return allProperties.collect(property ->
        {
            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
            return createBuilderInterfaceProperty(property, propertyReturnGenericType, imports, interfaceNameWithGenerics, interfaceName);
        }).makeString("") +
                propertiesFromAssociations.collect(property ->
                {
                    CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                    return createBuilderInterfacePropertyReverse(property, propertyReturnGenericType, imports, false) +
                            createBuilderInterfacePropertyReverse(property, propertyReturnGenericType, imports, true);
                }).makeString("");
    }

    private static String getBuilderInterfaceName(CoreInstance instance)
    {
        return instance.getName() + BUILDER_SUFFIX;
    }

    private String createBuilderInterfaceProperty(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceNameWithGenerics, String interfaceName)
    {
        String parameterName = isToOne(property) ? "value" : "values";
        String propertyTypeToOneNoGenerics = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, true, "Object");
        return createBuilderInterfacePropertySetter(property, propertyReturnGenericType, imports, interfaceNameWithGenerics, parameterName) +
                createBuilderInterfacePropertyRemove(property, interfaceNameWithGenerics) +
                (!isToOne(property) ? createBuilderInterfacePropertyToMany(property, propertyReturnGenericType, imports, interfaceNameWithGenerics) : "") +
                (!isOwningClass(interfaceName, propertyTypeToOneNoGenerics) && isStubType(property, propertyReturnGenericType) ? createBuilderInterfacePropertyStub(property, interfaceNameWithGenerics, parameterName) : "");
    }

    private static String createBuilderInterfacePropertyRemove(CoreInstance property, String interfaceName)
    {
        return "    " + interfaceName + " " + getUnifiedMethodName(property) + "Remove();\n";
    }

    private String createBuilderInterfacePropertyReverse(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, boolean isSever)
    {
        boolean isPlatformClass = isPlatformClass(property);
        String parameterType = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, isPlatformClass, isPlatformClass, true, "Object");
        return "    " + "void " + (isSever ? "_sever" : "") + "_reverse_" + property.getName() + "(" + parameterType + " value);\n";
    }

    private String createBuilderInterfacePropertySetter(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName, String parameterName)
    {
        String methodName = getUnifiedMethodName(property);
        boolean isToOne = isToOne(property);
        boolean isPlatformClass = isPlatformClass(property);
        String parameterType = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne, isPlatformClass, isPlatformClass, true, "Object");
        return "    " + interfaceName + " " + methodName + "(" + parameterType + " " + parameterName + ");\n";
    }

    private static String createBuilderInterfacePropertyStub(CoreInstance property, String interfaceName, String parameterName)
    {
        return createBuilderInterfacePropertyStubSetter(property, interfaceName, parameterName) +
                (!isToOne(property) ? createBuilderInterfacePropertyStubToMany(property, interfaceName) : "");
    }

    private static String createBuilderInterfacePropertyStubSetter(CoreInstance property, String interfaceName, String parameterName)
    {
        return "    " + interfaceName + " " + getUnifiedMethodName(property) + "CoreInstance(" + (isToOne(property) ? "CoreInstance" : "RichIterable<? extends CoreInstance>") + " " + parameterName + ");\n";
    }

    private static String createBuilderInterfacePropertyStubToMany(CoreInstance property, String interfaceName)
    {
        return "    " + interfaceName + " " + getUnifiedMethodName(property) + "AddCoreInstance(CoreInstance value);\n" +
                "    " + interfaceName + " " + getUnifiedMethodName(property) + "AddAllCoreInstance(RichIterable<? extends CoreInstance> values);\n" +
                "    " + interfaceName + " " + getUnifiedMethodName(property) + "RemoveCoreInstance(CoreInstance value);\n";
    }

    private String createBuilderInterfacePropertyToMany(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        boolean isPlatformClass = isPlatformClass(property);
        String propertyTypeExternalToOne = getPropertyTypeExternal(property, propertyReturnGenericType, imports, false, true, isPlatformClass, true, "Object");
        String propertyTypeExternalToMany = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, true, isPlatformClass, true, "Object");
        return "    " + interfaceName + " " + getUnifiedMethodName(property) + "Add(" + propertyTypeExternalToMany + " value);\n" +
                "    " + interfaceName + " " + getUnifiedMethodName(property) + "AddAll(" + propertyTypeExternalToOne + " values);\n" +
                "    " + interfaceName + " " + getUnifiedMethodName(property) + "Remove(" + propertyTypeExternalToMany + " value);\n";
    }

    public static String createInterfaceCopyMethod(String interfaceName, String typeParams)
    {
        return "    " + interfaceName + typeParams + " copy();\n";
    }

    private String createInterface(CoreInstance instance, String javaPackage, MutableSet<CoreInstance> properties, MutableSet<CoreInstance> propertiesFromAssociations, Imports imports)
    {
        String interfaceName = getInterfaceName(instance);
        String typeParams = getTypeParams(instance, false);
        CoreInstance classGenericType = getClassGenericType(instance);

        ListIterable<? extends CoreInstance> generalizations = instance.getValueForMetaPropertyToMany("generalizations");

        imports.addImports(generalizations.collect(generalization ->
        {
            CoreInstance generalType = getTypeFromGenericType(generalization.getValueForMetaPropertyToOne("general"));
            return (generalType == null) ? "" : getJavaPackageString(generalType) + "." + getInterfaceName(generalType);
        }, Sets.mutable.empty()));

        String generalizationsString = "CoreInstance, " + getAccessorInterfaceName(instance) + getTypeParams(instance, false);

        if (!generalizations.isEmpty())
        {
            generalizationsString += ", " + generalizations.collect(generalization ->
            {
                CoreInstance generalGenericType = generalization.getValueForMetaPropertyToOne("general");
                CoreInstance type = getTypeFromGenericType(generalGenericType);
                return getInterfaceName(Objects.requireNonNull(type)) + getTypeArgs(type, generalGenericType, false, true);
            }, Sets.mutable.empty()).makeString(", ");
        }

        return "package " + javaPackage + ";\n" +
                "\n" +
                "import " + javaPackage + "." + getAccessorInterfaceName(instance) + ";\n" +
                "import org.eclipse.collections.api.RichIterable;\n" +
                imports.toImportString() + "\n" +
                "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                "\n" +
                "public interface " + interfaceName + typeParams + " extends " + generalizationsString + "\n" +
                "{\n" +
                createBuilderInterfaceProperties(classGenericType, imports, interfaceName + typeParams, interfaceName, properties, propertiesFromAssociations) +
                createInterfaceCopyMethod(interfaceName, typeParams) +
                createInterfacePackageableElement(interfaceName) +
                "}\n";

    }

    private static String createInterfacePackageableElement(String interfaceName)
    {
        return "Any".equals(interfaceName) ? "    String getFullSystemPath();\n" : "";
    }

    private static String createClassPackageableElement(String systemPathForPackageableElement)
    {
        String body = "Root::meta::pure::metamodel::type::Enum".equals(systemPathForPackageableElement) ?
                "return org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getSystemPathForPackageableElement(this.getClassifier());\n"
                : "return \"" + systemPathForPackageableElement + "\";\n";
        return "    @Override\n" +
                "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         " + body +
                "    }\n";
    }

    public static String getTypeParams(CoreInstance _class, final boolean includeExtends)
    {
        String typeParams = "";
        if (isPlatformClass(_class))
        {
            typeParams = _class.getValueForMetaPropertyToMany("typeParameters").asLazy()
                    .collect(coreInstance -> includeExtends ? "CoreInstance" : coreInstance.getValueForMetaPropertyToOne("name").getName())
                    .makeString(",");
        }
        return typeParams.isEmpty() ? "" : "<" + typeParams + ">";
    }

    private static String getTypeArgs(CoreInstance _class, CoreInstance genericType, boolean covariant, boolean useTypeParameterName)
    {
        String typeArgs = "";
        if (isPlatformClass(_class))
        {
            typeArgs = genericType.getValueForMetaPropertyToMany("typeArguments").asLazy()
                    .collect(genericType1 ->
                    {
                        String ta = getTypeArgConcreteType(genericType1, useTypeParameterName);
                        return covariant ? "? extends " + ta : ta;
                    })
                    .makeString(", ");
        }
        return typeArgs.isEmpty() ? "" : "<" + typeArgs + ">";
    }

    private static String getInterfaceName(CoreInstance instance)
    {
        return instance.getName();
    }

    private String createWrapperClass(final CoreInstance instance, String javaPackage, MutableSet<CoreInstance> properties, MutableSet<CoreInstance> propertiesFromAssociations, MutableSet<CoreInstance> qualifiedProperties, Imports imports, MutableMap<String, CoreInstance> propertyOwners)
    {
        imports.addImports(Lists.mutable.of("org.finos.legend.pure.m4.coreinstance.simple.ValueHolder",
                "org.finos.legend.pure.m3.coreinstance.helper.PrimitiveHelper"));

        final String interfaceName = getInterfaceName(instance);
        String wrapperName = getWrapperName(instance);
        String typeParamsWithExtendsCoreInstance = getTypeParams(instance, true);
        final CoreInstance classGenericType = getClassGenericType(instance);

        imports.setThisClassName(wrapperName);

        PartitionIterable<CoreInstance> partition = properties.partition(M3ToJavaGenerator::isToOne);

        RichIterable<CoreInstance> toOneProperties = partition.getSelected();
        RichIterable<CoreInstance> toManyProperties = partition.getRejected();

        RichIterable<CoreInstance> mandatoryToOneProps = toOneProperties.select(M3ToJavaGenerator::isMandatoryProperty);

        RichIterable<Pair<String, String>> typesForMandatoryProps = buildMandatoryProperties(classGenericType, mandatoryToOneProps, imports).toSortedSetBy(Pair::getOne);

        MutableList<String> mandatoryTypes = Lists.mutable.of();
        MutableList<String> mandatoryProps = Lists.mutable.of();

        for (Pair<String, String> pair : typesForMandatoryProps)
        {
            mandatoryTypes.add(pair.getTwo() + " " + pair.getOne());
            mandatoryProps.add(pair.getOne());
        }

        final String maybeFullyQualifiedInterfaceName = (imports.shouldFullyQualify(javaPackage + "." + interfaceName) ? javaPackage + "." + interfaceName : interfaceName);

        String maybeFullyQualifiedInterfaceNameWithTypeParams = maybeFullyQualifiedInterfaceName + typeParamsWithExtendsCoreInstance;

        String systemPathForPackageableElement = getUserObjectPathForPackageableElement(instance, true).makeString("::");

        String value =
                "\n" +
                        "package " + javaPackage + ";\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.api.block.predicate.Predicate;\n" +
                        "import org.eclipse.collections.api.list.ListIterable;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.api.set.SetIterable;\n" +
                        "import org.eclipse.collections.impl.factory.Lists;\n" +
                        "import org.eclipse.collections.impl.factory.Sets;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.BaseM3CoreInstanceFactory;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;\n" +
                        imports.toImportString() + "\n" +
                        getPrimitiveImports() + "\n" +
                        "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                        "\n" +
                        "public class " + wrapperName + " extends AbstractCoreInstanceWrapper implements " + maybeFullyQualifiedInterfaceNameWithTypeParams + "\n" +
                        "{\n" +
                        "    public static final CoreInstanceFunction FROM_CORE_INSTANCE_FN = new CoreInstanceFunction();\n" +
                        "    public " + wrapperName + "(CoreInstance instance)\n" +
                        "    {\n" +
                        "        super(instance);\n" +
                        "    }\n" +
                        "\n" +
                        toOneProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createWrapperPropertyGetterToOne(interfaceName, property, propertyReturnGenericType, imports);
                        }).makeString("") +
                        toManyProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createWrapperPropertyGetterToMany(interfaceName, property, propertyReturnGenericType, imports);
                        }).makeString("") +
                        "\n" +
                        toOneProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            String sub = getSubstituteType(property, propertyReturnGenericType);
                            return sub == null ? "" : "    public " + maybeFullyQualifiedInterfaceName + " " + getUnifiedMethodName(property) + "(" + sub + " value)\n" +
                                    "    {\n" +
                                    "        instance.setKeyValues(" + createPropertyKeyNameReference(property.getName(), propertyOwners.get(property.getName()), instance) + ", Lists.immutable.<CoreInstance>with((CoreInstance)value));\n" +
                                    "        return this;\n" +
                                    "    }\n" + "\n";
                        }).makeString("") +
                        properties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createWrapperPropertySetter(property, propertyReturnGenericType, imports, maybeFullyQualifiedInterfaceName, propertyOwners, instance);
                        }).makeString("") +
                        propertiesFromAssociations.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createPropertyReverse(property, propertyReturnGenericType, imports, false, true) + createPropertyReverse(property, propertyReturnGenericType, imports, true, true);
                        }).makeString("") +
                        qualifiedProperties.collect(property ->
                        {
                            CoreInstance propertyReturnGenericType = this.propertyTypeResolver.getPropertyReturnType(classGenericType, property);
                            return createWrapperQualifiedPropertyGetter(property, propertyReturnGenericType, imports);
                        }).makeString("") +
                        "\n" +
                        "    public static " + maybeFullyQualifiedInterfaceName + " to" + interfaceName + "(CoreInstance instance)\n" +
                        "    {\n" +
                        "        if (instance == null) { return null; }\n" +
                        "        return " + maybeFullyQualifiedInterfaceName + ".class.isInstance(instance) ? (" + maybeFullyQualifiedInterfaceName + ") instance : new " + wrapperName + "(instance);\n" +
                        "    }\n" +
                        createWrapperStaticConversionFunction(interfaceName, wrapperName, maybeFullyQualifiedInterfaceNameWithTypeParams, getTypeParams(instance, false)) +
                        createWrapperClassCopyMethod(instance, maybeFullyQualifiedInterfaceName) +
                        createClassPackageableElement(systemPathForPackageableElement) +
                        "}\n";

        return value;
    }

    private String createWrapperQualifiedPropertyGetter(CoreInstance qualifiedProperty, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean qualifiedPropertyIsPlatform = isPlatformClass(qualifiedProperty);
        boolean isToOne = isToOne(qualifiedProperty);
        return "    public " + getPropertyTypeExternal(qualifiedProperty, propertyReturnGenericType, imports, isToOne, qualifiedPropertyIsPlatform, qualifiedPropertyIsPlatform, true, "Object") + " " + getQualifiedPropertyMethodName(qualifiedProperty) + "(" + getQualifiedPropertyParameters(qualifiedProperty, imports) + ")\n" +
                "    {\n" +
                "        " + M3_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperStaticConversionFunction(String interfaceName, String wrapperName, String maybeFullyQualifiedInterfaceName, String typeArgs)
    {
        return "    public static class CoreInstanceFunction" + typeArgs + " extends org.finos.legend.pure.m4.tools.DefendedFunction<CoreInstance, " + maybeFullyQualifiedInterfaceName + ">\n" +
                "    {\n" +
                "        public " + maybeFullyQualifiedInterfaceName + " valueOf(CoreInstance coreInstance)\n" +
                "        {\n" +
                "            return " + wrapperName + ".to" + interfaceName + "(coreInstance);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "   public static final " + typeArgs + " CoreInstanceFunction" + typeArgs + " FROM_CORE_INSTANCE_FN()\n" +
                "   {\n" +
                "       return new CoreInstanceFunction" + typeArgs + "();\n" +
                "   }\n";
    }

    private static String getWrapperName(CoreInstance instance)
    {
        return instance.getName() + WRAPPER_CLASS_SUFFIX;
    }

    private String createWrapperPropertySetter(CoreInstance property, CoreInstance propertyReturnGenericType,
                                               Imports imports, String interfaceName, MutableMap<String, CoreInstance> propertyOwners, CoreInstance instance)
    {
        String propertyName = property.getName();
        String parameterName = isToOne(property) ? "value" : "values";
        String expression = parameterName;
        if (!isStubType(property, propertyReturnGenericType))
        {
            expression = toCoreInstance(property, propertyReturnGenericType, expression, true);
        }
        expression = isToOne(property) ? "Lists.immutable.<CoreInstance>with(" + (isAnyOrNilTypeProperty(propertyReturnGenericType) ? "(CoreInstance)" : "") + expression + ")" : "(ListIterable<? extends CoreInstance>) " + expression;
        boolean isPlatformClass = isPlatformClass(property);
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "(" + getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass, true, "Object") + " " + parameterName + ")\n" +
                "    {\n" +
                "        this.instance.setKeyValues(" + createPropertyKeyNameReference(propertyName, propertyOwners.get(propertyName), instance) + ", " + expression + ");\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + " " + getUnifiedMethodName(property) + "Remove()\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n" +
                (!isToOne(property) ? createWrapperPropertySetterAdders(property, propertyReturnGenericType, imports, interfaceName) + createWrapperPropertySetterRemover(property, propertyReturnGenericType, imports, interfaceName) : "") +
                (isStubType(property, propertyReturnGenericType) ? createWrapperPropertySetterStub(property, interfaceName, propertyOwners, instance) + createWrapperPropertyStubSetterAdders(property, interfaceName) + createWrapperPropertyStubSetterRemover(property, interfaceName) : "");
    }

    private String createWrapperPropertySetterAdders(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        boolean isPlatformClass = isPlatformClass(property);
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "Add(" + getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, isPlatformClass, true, "Object") + " value)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + " " + getUnifiedMethodName(property) + "AddAll(" + getPropertyTypeExternal(property, propertyReturnGenericType, imports, false, false, isPlatformClass, true, "Object") + " values)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperPropertyStubSetterAdders(CoreInstance property, String interfaceName)
    {
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "AddCoreInstance(CoreInstance value)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + " " + getUnifiedMethodName(property) + "AddAllCoreInstance(RichIterable<? extends CoreInstance> values)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperPropertySetterRemover(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, String interfaceName)
    {
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "Remove(" + getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false, "Object") + " value)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperPropertyStubSetterRemover(CoreInstance property, String interfaceName)
    {
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "RemoveCoreInstance(CoreInstance value)\n" +
                "    {\n" +
                "        " + WRAPPER_UNSUPPORTED_EXCEPTION + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperPropertySetterStub(CoreInstance property, String interfaceName, final MutableMap<String, CoreInstance> propertyOwners, CoreInstance instance)
    {
        String parameterName = isToOne(property) ? "value" : "values";
        String parameterType = isToOne(property) ? "CoreInstance" : "RichIterable<? extends CoreInstance>";
        String expression = isToOne(property) ? "Lists.immutable.<CoreInstance>with(value)" : "(ListIterable<? extends CoreInstance>) values";
        return "    public " + interfaceName + " " + getUnifiedMethodName(property) + "CoreInstance(" + parameterType + " " + parameterName + ")\n" +
                "    {\n" +
                "        instance.setKeyValues(" + createPropertyKeyNameReference(property.getName(), propertyOwners.get(property.getName()), instance) + ", " + expression + ");\n" +
                "        return this;\n" +
                "    }\n" +
                "\n";
    }

    private String createPropertyReverse(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, boolean isSever, boolean isWrapper)
    {
        boolean isPlatformClass = isPlatformClass(property);
        String parameterType = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, isPlatformClass, isPlatformClass, true, "Object");
        return "    public " + "void " + (isSever ? "_sever" : "") + "_reverse_" + property.getName() + "(" + parameterType + " value)\n" +
                "    {\n" +
                "        " + (isWrapper ? WRAPPER_UNSUPPORTED_EXCEPTION : STUB_UNSUPPORTED_EXCEPTION) + ";\n" +
                "    }\n" +
                "\n";
    }

    private String createWrapperPropertyGetterToMany(String interfaceName, CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean isPlatformClass = isPlatformClass(property);
        String type = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne(property), false, isPlatformClass, false);
        String typeToOne = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false);
        String coreInstance = "instance.getValueForMetaPropertyToMany(\"" + property.getName() + "\")";
        return "    public " + type + " " + getUnifiedMethodName(property) + "()\n" +
                "    {\n" +
                "        return (" + type + ") " + coreInstanceToTypeToMany(propertyReturnGenericType, typeToOne, coreInstance) + ";\n" +
                "    }\n" +
                "\n" +
                (requiresCoreInstanceMethods(interfaceName, typeToOne, property, propertyReturnGenericType) ? this.createWrapperPropertyCoreInstanceGetter(property) : "");
    }

    private String createWrapperPropertyGetterToOne(String interfaceName, CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        boolean isPlatformClass = isPlatformClass(property);
        boolean isToOne = isToOne(property);
        String type = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne, false, isPlatformClass, false);
        String typeNoGenerics = getPropertyTypeExternal(property, propertyReturnGenericType, imports, isToOne, false, false, false);
        String typeToOneNoGenerics = isToOne ? typeNoGenerics : getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false);
        String coreInstance = "instance.getValueForMetaPropertyToOne(\"" + property.getName() + "\")";
        return "    public " + type + " " + getUnifiedMethodName(property) + "()\n" +
                "    {\n" +
                "        return (" + type + ") " + coreInstanceToTypeToOne(propertyReturnGenericType, typeNoGenerics, coreInstance) + ";\n" +
                "    }\n" +
                "\n" +
                (requiresCoreInstanceMethods(interfaceName, typeToOneNoGenerics, property, propertyReturnGenericType) ? this.createWrapperPropertyCoreInstanceGetter(property) : "");
    }

    private String createWrapperPropertyCoreInstanceGetter(CoreInstance property)
    {
        String expression = "instance.getValueForMetaProperty" + (isToOne(property) ? "ToOne" : "ToMany") + "(\"" + property.getName() + "\")";
        String type = getCoreInstanceType(property);
        return "    public " + type + " " + getUnifiedMethodName(property) + "CoreInstance()\n" +
                "    {\n" +
                "        return (" + type + ")" + expression + ";\n" +
                "    }\n" +
                "\n";
    }

    private void createFactory(MapIterable<String, CoreInstance> packageToCoreInstance, SetIterable<CoreInstance> m3Enumerations)
    {
        String factoryName = this.factoryNamePrefix + "CoreInstanceFactoryRegistry";
        RichIterable<CoreInstance> allInstances = m3Enumerations.toList().withAll(packageToCoreInstance.valuesView());
        String result =
                "package org.finos.legend.pure.m3.coreinstance;\n" +
                        "\n" +
                        "import org.eclipse.collections.api.map.MutableMap;\n" +
                        "import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;\n" +
                        "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                        "import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;\n" +
                        "import org.eclipse.collections.api.set.SetIterable;\n" +
                        "import org.eclipse.collections.impl.factory.Sets;\n" +
                        packageToCoreInstance.keysView().collect(_package -> "import " + _package + ";").makeString("\n") +
                        packageToCoreInstance.keysView().collect(_package -> "import " + _package.substring(0, _package.lastIndexOf("Instance")) + ";").makeString("\n") +
                        "\n" +
                        "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;\n" +
                        "\n" +
                        "\n" +
                        "public class " + factoryName + "\n" +
                        "{\n" +
                        "    public static final CoreInstanceFactoryRegistry REGISTRY;\n" +
                        "    public static final SetIterable<String> ALL_PATHS = Sets.mutable.of" +
                        allInstances.collect(object -> "\"" + getUserObjectPathForPackageableElement(object, false).makeString("::") + "\"").makeString("(", ",", ")") +
                        ";\n\n" +
                        "    static\n" +
                        "    {\n" +
                        "        MutableMap<String, java.lang.Class> interfaceByPath = UnifiedMap.newMap(" + packageToCoreInstance.size() + ");\n" +
                        allInstances.collect(object ->
                        {
                            String classString = isEnum(object) ? "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum" : Objects.requireNonNull(object).getName();
                            return "        interfaceByPath.put(\"" + getUserObjectPathForPackageableElement(object, false).makeString("::") + "\", " + classString + ".class);";
                        }).makeString("\n") + "\n";

        if (this.generateTypeFactoriesById)
        {
            result += "        MutableIntObjectMap<CoreInstanceFactory> typeFactoriesById = IntObjectHashMap.newMap();\n" +
                    allInstances.collect(object ->
                    {
                        String classString = isEnum(object) ? "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum" : Objects.requireNonNull(object).getName();
                        return "        typeFactoriesById.put(" + object.getSyntheticId() + ", " + classString + "Instance.FACTORY);";
                    }).makeString("", "\n", "\n");
        }

        result += "        MutableMap<String, CoreInstanceFactory> typeFactoriesByPath = UnifiedMap.newMap(" + packageToCoreInstance.size() + ");\n" +
                allInstances.collect(object ->
                {
                    String classString = isEnum(object) ? "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum" : Objects.requireNonNull(object).getName();
                    return "        typeFactoriesByPath.put(" + getUserObjectPathForPackageableElement(object, false).makeString("\"", "::", "\"") + ", " + classString + "Instance.FACTORY);";
                }).makeString("", "\n", "\n") +
                "        REGISTRY = new CoreInstanceFactoryRegistry(" + (this.generateTypeFactoriesById ? "typeFactoriesById.toImmutable()" : "new IntObjectHashMap<CoreInstanceFactory>().toImmutable()") + ", typeFactoriesByPath.toImmutable(), interfaceByPath.toImmutable());\n" +
                "    }\n" +
                "    public static java.lang.Class getClassForPath(String path)\n" +
                "    {\n" +
                "        return REGISTRY.getClassForPath(path);\n" +
                "    }\n" +
                "}\n";

        try
        {
            Path p = Paths.get(this.outputDir + ROOT_PACKAGE.replace(".", "/") + "/" + factoryName + ".java");
            Files.createDirectories(p.getParent());
            Files.write(p, result.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

    private static void collectGeneralizationProperties(ListIterable<? extends CoreInstance> generalizations, MutableSet<CoreInstance> properties, MutableMap<String, CoreInstance> propertyOwners)
    {
        for (CoreInstance generalization : generalizations)
        {
            CoreInstance general = getTypeFromGenericType(generalization.getValueForMetaPropertyToOne("general"));

            ListIterable<? extends CoreInstance> ps = general == null ? Lists.mutable.empty() : general.getValueForMetaPropertyToMany("properties");

            // TODO: Respect property resolution order
            for (CoreInstance prop : ps)
            {
                if (!properties.contains(prop))
                {
                    properties.add(prop);

                    if (!propertyOwners.containsKey(prop.getName()))
                    {
                        propertyOwners.put(prop.getName(), general);
                    }
                }
            }

            collectGeneralizationProperties(general == null ? Lists.mutable.empty() : general.getValueForMetaPropertyToMany("generalizations"), properties, propertyOwners);

        }

    }

    private static void collectGeneralizationXProperties(ListIterable<? extends CoreInstance> generalizations, String propertyType, MutableSet<CoreInstance> result)
    {
        for (CoreInstance generalization : generalizations)
        {
            CoreInstance general = getTypeFromGenericType(generalization.getValueForMetaPropertyToOne("general"));
            ListIterable<? extends CoreInstance> ps = general == null ? Lists.mutable.empty() : general.getValueForMetaPropertyToMany(propertyType);
            // TODO: Respect property resolution order
            for (CoreInstance prop : ps)
            {
                if (!result.contains(prop))
                {
                    result.add(prop);
                }
            }
            collectGeneralizationXProperties(general == null ? Lists.mutable.empty() : general.getValueForMetaPropertyToMany("generalizations"), propertyType, result);
        }

    }

    private static boolean isMandatoryProperty(CoreInstance property)
    {
        CoreInstance multiplicity = property.getValueForMetaPropertyToOne("multiplicity");
        return isMandatory(multiplicity);
    }

    private static boolean isMandatory(CoreInstance multiplicity)
    {
        boolean isToOne = false;
        CoreInstance lowerBound = multiplicity.getValueForMetaPropertyToOne("lowerBound");
        if (lowerBound != null)
        {
            CoreInstance value = lowerBound.getValueForMetaPropertyToOne("value");

            if (value != null)
            {
                isToOne = "1".equals(value.getName());
            }
        }

        return isToOne;
    }

    private static boolean isToOne(CoreInstance each)
    {
        boolean isToOne = false;
        CoreInstance multiplicity = each.getValueForMetaPropertyToOne("multiplicity");
        CoreInstance upperBound = multiplicity.getValueForMetaPropertyToOne("upperBound");
        if (upperBound != null)
        {
            CoreInstance value = upperBound.getValueForMetaPropertyToOne("value");

            if (value != null)
            {
                isToOne = "1".equals(value.getName());
            }
        }

        return isToOne;
    }

    private static String getPropertyNameAsValidJavaIdentifierSwitchName(CoreInstance property)
    {
        String name = getPropertyNameAsValidJavaIdentifier(property);
        return "name".equals(name) ? name + '_' : name;
    }

    private static String getPropertyNameAsValidJavaIdentifier(CoreInstance property)
    {
        return JavaTools.makeValidJavaIdentifier(property.getName());
    }

    private static String getMethodName(CoreInstance property)
    {
        String propertyName = property.getName();
        if ("class".equals(propertyName) || "name".equals(propertyName))
        {
            propertyName += "_";
        }
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    private static String getUnifiedMethodName(CoreInstance property)
    {
        return "_" + property.getName();
    }

    private static String getQualifiedPropertyMethodName(CoreInstance qualifiedProperty)
    {
        return qualifiedProperty.getValueForMetaPropertyToOne("functionName").getName();
    }

    private static String getParameterName(CoreInstance param)
    {
        return "_" + param.getValueForMetaPropertyToOne("name").getName();
    }

    private static boolean isPrimitiveTypeProperty(CoreInstance propertyReturnGenericType)
    {
        CoreInstance rawType = getTypeFromGenericType(propertyReturnGenericType);
        return isPrimitiveType(rawType);
    }

    private static boolean isPrimitiveType(CoreInstance rawType)
    {
        return rawType != null && "PrimitiveType".equals(rawType.getClassifier().getName());
    }

    public boolean isStubType(CoreInstance property, CoreInstance propertyReturnGenericType)
    {
        return getSubstituteType(property, propertyReturnGenericType) != null;
    }

    private static boolean isAnyOrNilTypeProperty(CoreInstance propertyGenericType)
    {
        CoreInstance rawType = getTypeFromGenericType(propertyGenericType);
        return isAnyOrNilType(rawType);
    }

    private static boolean isAnyOrNilType(CoreInstance rawType)
    {
        return rawType != null && (isAnyType(rawType) || "Nil".equals(rawType.getName()));
    }

    private static boolean isAnyType(CoreInstance rawType)
    {
        return rawType != null && "Any".equals(rawType.getName());
    }

    private static boolean isAnyTypeProperty(CoreInstance propertyGenericType)
    {
        CoreInstance rawType = getTypeFromGenericType(propertyGenericType);
        return isAnyType(rawType);
    }

    private static boolean isNilType(CoreInstance rawType)
    {
        return rawType != null && "Nil".equals(rawType.getName());
    }

    private static boolean isNilTypeProperty(CoreInstance propertyGenericType)
    {
        CoreInstance rawType = getTypeFromGenericType(propertyGenericType);
        return isNilType(rawType);
    }

    private static CoreInstance getType(CoreInstance property)
    {
        return getTypeFromGenericType(property.getValueForMetaPropertyToOne("genericType"));
    }

    private static CoreInstance getTypeFromGenericType(CoreInstance genericType)
    {
        if (genericType == null)
        {
            return null;
        }
        CoreInstance coreInstance = genericType.getValueForMetaPropertyToOne("rawType");
        // TODO : should this actually happen?
        if (coreInstance != null && coreInstance.getClassifier() != null && "ImportStub".equals(coreInstance.getClassifier().getName()))
        {
            coreInstance = coreInstance.getValueForMetaPropertyToOne("resolvedNode");
        }
        return coreInstance;
    }

    private String getTypeNameWithStringForPrimitives(CoreInstance propertyReturnGenericType, Imports imports)
    {
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            return "String";
        }
        return getTypeName(propertyReturnGenericType, imports);
    }

    private String getTypeName(CoreInstance propertyReturnGenericType, Imports imports)
    {
        CoreInstance rawType = getTypeFromGenericType(propertyReturnGenericType);
        if (rawType == null)
        {
            return "CoreInstance";
        }
        if (isAnyOrNilType(rawType))
        {
            return "CoreInstance";
        }
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            return rawType.getName() + "CoreInstance";
        }
        else
        {
            String propertyJavaPackage = getJavaPackageString(rawType);
            String type = rawType.getName();
            return (imports != null && imports.shouldFullyQualify(propertyJavaPackage + "." + type) ? propertyJavaPackage + "." + type : type);
        }
    }

    private String getTypeForSetter(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        String sub = getSubstituteType(property, propertyReturnGenericType);
        if (isEnum(getTypeFromGenericType(propertyReturnGenericType)))
        {
            sub = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum";
        }
        return sub == null ? getTypeNameWithStringForPrimitives(propertyReturnGenericType, imports) : sub;
    }

    private static String getPropertyJavaType(Imports imports, boolean useGenerics, CoreInstance genericType, CoreInstance rawType, boolean covariant, boolean useTypeParameterName)
    {
        String type = rawType.getName();
        String _package = getJavaPackageString(rawType);
        boolean fullyQualify = imports.shouldFullyQualify(_package + "." + type);
        String className = fullyQualify ? _package + "." + type : type;
        String typeArgs = useGenerics ? getTypeArgs(rawType, genericType, covariant, useTypeParameterName) : "";
        return className + typeArgs;
    }

    private String getPropertyTypeInternal(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, boolean toOne, boolean useTypeParameterName, boolean addTypeArgs)
    {
        String toOneType;
        CoreInstance rawType = getTypeFromGenericType(propertyReturnGenericType);
        if (rawType == null)
        {
            toOneType = useTypeParameterName ? getTypeParameterName(propertyReturnGenericType) : "CoreInstance";
        }
        else if (isStubType(property, propertyReturnGenericType) || "FunctionType".equals(rawType.getClassifier().getName()) || isAnyOrNilType(rawType))
        {
            toOneType = "CoreInstance";
        }
        else if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            toOneType = getPrimitiveClass(propertyReturnGenericType);
        }
        else if (isEnum(rawType))
        {
            toOneType = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum";
        }
        else
        {
            toOneType = getPropertyJavaType(imports, addTypeArgs, propertyReturnGenericType, rawType, true, useTypeParameterName);
        }
        return toOne ? toOneType : "ToManyPropertyValues<" + toOneType + ">";
    }

    private String getPropertyTypeExternal(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, boolean toOne, boolean useTypeParameterName, boolean addTypeArgs, boolean covariant)
    {
        return getPropertyTypeExternal(property, propertyReturnGenericType, imports, toOne, useTypeParameterName, addTypeArgs, covariant, "CoreInstance");
    }

    private String getPropertyTypeExternal(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports, boolean toOne, boolean useTypeParameterName, boolean addTypeArgs, boolean covariant, String anyType)
    {
        return getTypeExternalFromGenericType(propertyReturnGenericType, property.getValueForMetaPropertyToOne("multiplicity"), imports, toOne, useTypeParameterName, addTypeArgs, covariant, anyType);
    }

    private String getTypeExternalFromGenericType(CoreInstance genericType, CoreInstance multiplicity, Imports imports, boolean toOne, boolean useTypeParameterName, boolean addTypeArgs, boolean covariant, String anyType)
    {
        CoreInstance rawType = getTypeFromGenericType(genericType);
        return getTypeExternal(rawType, genericType, multiplicity, imports, toOne, useTypeParameterName, addTypeArgs, covariant, anyType);
    }

    private String getTypeExternal(CoreInstance rawType, CoreInstance genericType, CoreInstance multiplicity, Imports imports, boolean toOne, boolean useTypeParameterName, boolean addTypeArgs, boolean covariant, String anyType)
    {
        String toOneType;

        if (rawType == null)
        {
            toOneType = useTypeParameterName ? getTypeParameterName(genericType) : "CoreInstance";
        }
        else if ("FunctionType".equals(rawType.getClassifier().getName()) || isAnyOrNilType(rawType))
        {
            toOneType = anyType;
        }
        else if (isPrimitiveType(rawType))
        {
            toOneType = isMandatory(multiplicity) ? PRIMITIVES_EXTERNAL.get(rawType.getName()) : PRIMITIVES_EXTERNAL_0_1.get(rawType.getName());
        }
        else if (isEnum(rawType))
        {
            toOneType = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum";
        }
        else
        {
            toOneType = getPropertyJavaType(imports, addTypeArgs, genericType, rawType, covariant, useTypeParameterName);
        }
        String _extends = "? extends ";
        return toOne ? toOneType : "RichIterable<" + _extends + toOneType + ">";
    }

    private static String getTypeParameterName(CoreInstance genericType)
    {
        String toOneType;
        CoreInstance typeParameter = genericType.getValueForMetaPropertyToOne("typeParameter");
        CoreInstance name = typeParameter.getValueForMetaPropertyToOne("name");
        toOneType = name.getName();
        return toOneType;
    }

    private static String getTypeArgConcreteType(CoreInstance genericType, boolean useTypeParameterName)
    {
        CoreInstance rawType = getTypeFromGenericType(genericType);
        if (rawType == null)
        {
            return useTypeParameterName ? getTypeParameterName(genericType) : "CoreInstance";
        }
        else if (isAnyOrNilType(rawType) || "FunctionType".equals(rawType.getClassifier().getName()))
        {
            return "Object";
        }
        else if (isPrimitiveType(rawType))
        {
            return PRIMITIVES.get(rawType.getName());
        }
        else
        {
            return getJavaPackageString(rawType) + "." + rawType.getName() + getTypeArgs(rawType, genericType, false, useTypeParameterName);
        }
    }

    private String getSubstituteType(CoreInstance property, CoreInstance propertyGenericType)
    {
        if ("p_stereotypes".equals(property.getName()) || "p_tags".equals(property.getName()) || "specific".equals(property.getName()))
        {
            return null;
        }
        CoreInstance rawType = getTypeFromGenericType(propertyGenericType);
        return rawType == null ? null : getStubType(rawType.getName());
    }

    public static MutableList<String> getUserObjectPathForPackageableElement(CoreInstance packageableElement, boolean includeRoot)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne("package");
        if (pkg == null)
        {
            return (includeRoot || !"Root".equals(packageableElement.getName())) ? Lists.mutable.with(packageableElement.getName()) : Lists.mutable.empty();
        }
        else
        {
            MutableList<String> pkgPath = getUserObjectPathForPackageableElement(pkg, includeRoot);
            pkgPath.add(packageableElement.getName());
            return pkgPath;
        }
    }

    public static String getFullyQualifiedM3InterfaceForCompiledModel(CoreInstance instance)
    {
        return getJavaPackageString(instance) + "." + getInterfaceName(instance);
    }

    public static String getFullyQualifiedM3InterfaceForCompiledModel(ListIterable<String> packagePath, CoreInstance instance)
    {
        return getJavaPackageString(packagePath) + "." + getInterfaceName(instance);
    }

    public static String getFullyQualifiedM3ImplForCompiledModel(CoreInstance instance)
    {
        return getJavaPackageString(instance) + "." + getClassName(instance);
    }

    public static String getFullyQualifiedM3InterfaceForCompiledModel(String m3Path)
    {
        ListIterable<String> elementsAsString = StringIterate.tokensToList(m3Path, "::");
        return elementsAsString.size() > 1 ? getJavaPackageString(elementsAsString.take(elementsAsString.size() - 1)) + "." + elementsAsString.getLast() : ROOT_PACKAGE + "." + elementsAsString.getLast();
    }

    private String coreInstanceToTypeToOne(CoreInstance propertyReturnGenericType, String type, String expression)
    {
        if ("Double".equals(type))
        {
            return "(" + expression + " == null ? null : " + coreInstanceToTypeFn(propertyReturnGenericType, type) + ".valueOf(" + expression + ").doubleValue())";
        }
        else
        {
            return "CoreInstance".equals(type) ? expression : coreInstanceToTypeFn(propertyReturnGenericType, type) + ".valueOf(" + expression + ")" + ("double".equals(type) ? ".doubleValue()" : "");
        }
    }

    private String coreInstanceToTypeToMany(CoreInstance propertyReturnGenericType, String type, String expression)
    {
        return "CoreInstance".equals(type) ? expression : expression + ".collect(" + coreInstanceToTypeFn(propertyReturnGenericType, type) + ")";
    }

    private String coreInstanceToTypeFn(CoreInstance propertyReturnGenericType, String type)
    {
        String conversionClass;
        if (isPrimitiveTypeProperty(propertyReturnGenericType))
        {
            CoreInstance typeO = getTypeFromGenericType(propertyReturnGenericType);
            if (typeO == null)
            {
                throw new RuntimeException("Type should not be null!");
            }
            conversionClass = typeO.getName() + "CoreInstance";
        }
        else
        {
            conversionClass = type + WRAPPER_CLASS_SUFFIX;
        }
        return conversionClass + ".FROM_CORE_INSTANCE_FN";
    }

    private String typeFromCoreInstanceFn(CoreInstance property, CoreInstance propertyReturnGenericType, Imports imports)
    {
        String typeToOne = getPropertyTypeExternal(property, propertyReturnGenericType, imports, true, false, false, false);
        return typeToOne + WRAPPER_CLASS_SUFFIX + ".FROM_CORE_INSTANCE_FN";
    }

    private static String applyFunctionWithCardinality(CoreInstance property, String fn, String expression, boolean isToOne)
    {
        return isToOne ? fn + ".valueOf(" + expression + ")" : expression + ".collect(" + fn + ")";
    }

    private static String applyFunction2WithCardinality(CoreInstance property, String fn, String expression, boolean isWrapper, boolean isToOne)
    {
        String repository = isWrapper ? "this.instance.getRepository()" : "this.getRepository()";
        return isToOne ? fn + ".value(" + expression + ", " + repository + ")" : expression + ".collectWith(" + fn + ", " + repository + ")";
    }

    private static String primitiveFromCoreInstanceFn(CoreInstance propertyGenericType)
    {
        return getPrimitiveClass(propertyGenericType) + ".FROM_CORE_INSTANCE_FN";
    }

    private static String getPrimitiveClass(CoreInstance propertyGenericType)
    {
        CoreInstance type = getTypeFromGenericType(propertyGenericType);
        if (type == null)
        {
            throw new RuntimeException("Type should not be null!");
        }
        return type.getName() + "CoreInstance";
    }

    private String primitiveCoreInstanceFromPrimitive(CoreInstance property, CoreInstance propertyReturnGenericType, String expression, boolean isWrapper, boolean isToOne)
    {
        CoreInstance type = getTypeFromGenericType(propertyReturnGenericType);
        if (type == null)
        {
            throw new RuntimeException("Type should not be null!");
        }
        String typeName = type.getName();
        String toPrimitiveFn = "PrimitiveHelper." + typeName.toUpperCase() + "_TO_COREINSTANCE_FN";
        if ("Float".equals(typeName))
        {
            expression = "new java.math.BigDecimal(" + expression + ")";
        }
        return applyFunction2WithCardinality(property, toPrimitiveFn, expression, isWrapper, isToOne);
    }

    private static String getPrimitiveImports()
    {
        return "import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;\n" +
                "import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;\n" +
                "import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;\n" +
                "import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;\n" +
                "import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;\n";
    }

    private static String nullSafe(String variable, String expression)
    {
        return nullSafe(variable, "null", expression);
    }

    private static String nullSafe(String variable, String defaultValue, String expression)
    {
        return "(" + variable + " == null ? " + defaultValue + " : " + expression + ")";
    }

    public static class Imports
    {
        private final MutableSet<String> importsList = Sets.mutable.empty();
        private final MutableSet<String> unqualifiedImportedNames = Sets.mutable.empty();

        private String thisClassName;

        public boolean shouldFullyQualify(String className)
        {
            return !this.importsList.contains(className) && this.unqualifiedImportedNames.contains(className.substring(className.lastIndexOf('.') + 1));
        }

        void addImports(Imports imports)
        {
            this.addImports(imports.importsList);
        }

        void addImports(Iterable<String> imports)
        {
            imports.forEach(this::addImport);
        }

        void addImports(String... imports)
        {
            ArrayIterate.forEach(imports, this::addImport);
        }

        void addImport(String classFullName)
        {
            if (!this.importsList.contains(classFullName))
            {
                String unqualifiedName = classFullName.substring(classFullName.lastIndexOf('.') + 1);
                if (!this.unqualifiedImportedNames.contains(unqualifiedName))
                {
                    this.importsList.add(classFullName);
                    this.unqualifiedImportedNames.add(unqualifiedName);
                }
            }
        }

        void setThisClassName(String thisClassName)
        {
            this.thisClassName = thisClassName;
        }

        String toImportString()
        {
            String thisClassNameWithDot = "." + this.thisClassName;
            return this.importsList.asLazy()
                    .reject(packagePath -> packagePath.endsWith(thisClassNameWithDot))
                    .collect(packagePath -> "import " + packagePath + ";")
                    .makeString("\n");
        }
    }

    public interface PropertyTypeResolver
    {
        CoreInstance getPropertyReturnType(CoreInstance classGenericType, CoreInstance property);

        CoreInstance getClassGenericType(CoreInstance classInstance);
    }

    static class DefaultPropertyTypeResolver implements PropertyTypeResolver
    {
        @Override
        public CoreInstance getPropertyReturnType(CoreInstance classGenericType, CoreInstance property)
        {
            return property.getValueForMetaPropertyToOne("genericType");
        }

        @Override
        public CoreInstance getClassGenericType(CoreInstance classInstance)
        {
            return null;
        }
    }
}
