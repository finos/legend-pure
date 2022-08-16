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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.Counter;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class ClassImplProcessor
{
    //DO NOT ADD WIDE * IMPORTS TO THIS LIST IT IMPACTS COMPILE TIMES
    static final String IMPORTS = "import org.eclipse.collections.api.list.ListIterable;\n" +
            "import org.eclipse.collections.api.list.MutableList;\n" +
            "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.eclipse.collections.api.map.MutableMap;\n" +
            "import org.eclipse.collections.impl.factory.Lists;\n" +
            "import org.eclipse.collections.impl.factory.Maps;\n" +
            "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
            "import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;\n" +
            "import org.finos.legend.pure.m4.ModelRepository;\n" +
            "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.E_;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;\n" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;\n" +

            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n";

    static final String FUNCTION_IMPORTS =
            "import org.eclipse.collections.api.block.function.Function0;\n" +
                    "import org.eclipse.collections.api.block.function.Function;\n" +
                    "import org.eclipse.collections.api.block.function.Function2;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n";

    static final String SERIALIZABLE_IMPORTS = "import java.io.Externalizable;\n" +
            "import java.io.IOException;\n" +
            "import java.io.ObjectInput;\n" +
            "import java.io.ObjectOutput;\n" +
            "import org.eclipse.collections.api.block.procedure.Procedure;\n";

    public static final String CLASS_IMPL_SUFFIX = "_Impl";

    public static final Predicate2<CoreInstance, ProcessorSupport> IS_TO_ONE = new Predicate2<CoreInstance, ProcessorSupport>()
    {
        @Override
        public boolean accept(CoreInstance coreInstance, ProcessorSupport processorSupport)
        {
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.multiplicity, processorSupport);
            return Multiplicity.isToOne(multiplicity, false);
        }
    };

    public static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, final ProcessorSupport processorSupport, final boolean useJavaInheritance, boolean addJavaSerializationSupport, String pureExternalPackage)
    {
        processorContext.setClassImplSuffix(CLASS_IMPL_SUFFIX);
        final CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = JavaPackageAndImportBuilder.buildImplClassNameFromType(_class);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = TypeProcessor.javaInterfaceForType(_class) + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(_class)) ||
                M3Paths.ConstraintsGetterOverride.equals(PackageableElement.getUserPathForPackageableElement(_class));

        ListIterable<String> allGeneralizations = ClassInterfaceProcessor.getAllGeneralizations(processorContext, processorSupport, _class, CLASS_IMPL_SUFFIX);

        String _extends = useJavaInheritance ? allGeneralizations.getFirst() : "ReflectiveCoreInstance";
        final CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);

        boolean hasFunctions = !_Class.getQualifiedProperties(_class, processorContext.getSupport()).isEmpty()
                || !_Class.computeConstraintsInHierarchy(_class, processorContext.getSupport()).isEmpty();

        return StringJavaSource.newStringJavaSource(_package, className, IMPORTS + (hasFunctions ? FUNCTION_IMPORTS : "") + (addJavaSerializationSupport ? SERIALIZABLE_IMPORTS : "") + imports +
                "public class " + classNamePlusTypeParams + " extends " + _extends + " implements " + interfaceNamePlusTypeParams + (isGetterOverride ? ", GetterOverrideExecutor" : "") +
                (addJavaSerializationSupport ? ", Externalizable" : "") + "\n" +
                "{\n" +
                (addJavaSerializationSupport ? "    static final long serialVersionUID = -1L;\n" : "") +
                buildMetaInfo(classGenericType, processorSupport, false) +
                (addJavaSerializationSupport ? buildDefaultConstructor(className) : "") +
                buildSimpleConstructor(_class, className, processorSupport, useJavaInheritance) +
                (addJavaSerializationSupport ? buildSerializationMethods(_class, processorSupport, classGenericType, useJavaInheritance, associationClass, pureExternalPackage) : "") +
                buildGetClassifier() +
                buildGetKeys(_class, processorSupport) +
                (ClassProcessor.isPlatformClass(_class) ? buildFactory(className) : buildFactoryConstructor(className)) +
                (isGetterOverride ? getterOverrides(interfaceNamePlusTypeParams) : "") +
                buildGetValueForMetaPropertyToOne(classGenericType, processorSupport) +
                buildGetValueForMetaPropertyToMany(classGenericType, processorSupport) +

                buildSimpleProperties(classGenericType, new FullPropertyImplementation()
                {
                    @Override
                    public String build(CoreInstance property, String name, CoreInstance unresolvedReturnType, CoreInstance returnType, CoreInstance returnMultiplicity, String returnTypeJava, String classOwnerFullId, String ownerClassName, String ownerTypeParams, ProcessorContext processorContext)
                    {
                        CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);

                        String propertyString = "";
                        boolean includeGettor = !useJavaInheritance || propertyOwner == _class || Instance.instanceOf(propertyOwner, associationClass, processorSupport);
                        if (includeGettor)
                        {

                            propertyString += Multiplicity.isToOne(returnMultiplicity, false) ?
                                    "    public " + returnTypeJava + " _" + name + ";\n" :
                                    "    public RichIterable _" + name + " = Lists.mutable.with();\n";
                        }
                        propertyString += buildProperty(property, ownerClassName + (ownerTypeParams.isEmpty() ? "" : "<" + ownerTypeParams + ">"), "this", classOwnerFullId, name, returnType, unresolvedReturnType, returnMultiplicity, processorContext.getSupport(), includeGettor, processorContext);
                        return propertyString;
                    }
                }, processorContext, processorSupport) +
                buildQualifiedProperties(classGenericType, processorContext, processorSupport) +
                buildCopy(classGenericType, CLASS_IMPL_SUFFIX, isGetterOverride, processorSupport) +
                (ClassProcessor.isLazy(_class) ? buildEquality(classGenericType, CLASS_IMPL_SUFFIX, true, false, true, processorContext, processorSupport) : buildEquality(classGenericType, CLASS_IMPL_SUFFIX, false, false, false, processorContext, processorSupport)) +
                buildGetFullSystemPath() +
                //Not supported on platform classes yet
                (ClassProcessor.isPlatformClass(_class) ? "" : validate(_class, className, classGenericType, processorContext, processorSupport.class_getSimpleProperties(_class))) +
                "}");
    }

    private static String buildDefaultConstructor(String className)
    {
        return "    public " + className + "()\n" +
                "    {\n" +
                "         this(\"Anonymous_NoCounter\");;\n" +
                "    }\n" +
                "\n";
    }

    private static String buildSerializationMethods(final CoreInstance _class, final ProcessorSupport processorSupport, final CoreInstance classGenericType, final boolean useJavaInheritance, final CoreInstance associationClass, String pureExternalPackage)
    {
        final StringBuilder writeExternal = new StringBuilder();
        final StringBuilder readExternal = new StringBuilder();
        writeExternal.append("   @Override\n    public void writeExternal(final ObjectOutput out) throws IOException\n    {\n");
        readExternal.append("    @Override\n    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException\n    {\n");
        Counter enumCounter = new Counter(0);
        processorSupport.class_getSimpleProperties(_class).forEach(property ->
        {
            CoreInstance unresolvedReturnType = ClassProcessor.getPropertyUnresolvedReturnType(property, processorSupport);
            CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);

            String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
            CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

            boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType, processorSupport) && Multiplicity.isToOne(returnMultiplicity, true);
            String returnTypeJava = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
            boolean multiplicityOne = Multiplicity.isToOne(returnMultiplicity, false);
            if ("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum".equals(returnTypeJava))
            {
                serializeEnum(name, multiplicityOne, writeExternal, readExternal, enumCounter.getCount(), pureExternalPackage);
                enumCounter.increment();
            }
            else
            {
                writeExternal.append("           out.writeObject(this._").append(name).append(");\n");
                readExternal.append("           this._").append(name).append(" = (").append(!multiplicityOne ? " RichIterable" : returnTypeJava).append(") in.readObject();\n");
            }
        });
        writeExternal.append("   }\n");
        readExternal.append("   }\n");

        return writeExternal.append(readExternal).toString();
    }

    private static void serializeEnum(String propertyName, boolean multiplicityOne, StringBuilder writeExternal, StringBuilder readExternal, int n, String pureExternalPackage)
    {
        if (multiplicityOne)
        {
            writeExternal.append("            out.writeObject(this._" + propertyName + ".getFullSystemPath());out.writeObject(this._" + propertyName + "._name());\n");
            readExternal.append("try { this._" + propertyName + " = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) ((org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport)Class.forName(\"" + pureExternalPackage + ".PureExternal\").getMethod(\"_getExecutionSupport\").invoke(null)).getMetadata().getEnum(((String)in.readObject()).substring(6), (String)in.readObject()); } " +
                    "catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException | ClassNotFoundException e ) {\n" +
                    "         throw  new RuntimeException(e);\n" +
                    "     };\n");
        }
        else
        {
            writeExternal.append("            out.writeObject((Integer)this._" + propertyName + ".size());\n");
            readExternal.append("             int n" + n + " = (Integer)in.readObject();");
            writeExternal.append("            this._" + propertyName + ".forEach(new  DefendedProcedure() ")
                    .append("{\n").append("            @Override\n").append("            public void value(Object anEnum) {\n").append("            try{out.writeObject(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum)anEnum).getFullSystemPath());out.writeObject(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum)anEnum)._name());}catch (IOException e){throw new RuntimeException(e);}\n").append("            }});\n");
            readExternal.append("             for (int i=0;i<n" + n + ";i++){\n" +
                    "            try { _" + propertyName + "((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) ((org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport)Class.forName(\"" + pureExternalPackage + ".PureExternal\").getMethod(\"_getExecutionSupport\").invoke(null)).getMetadata().getEnum(((String)in.readObject()).substring(6), (String)in.readObject()), true); }\n" +
                    "                    catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException | ClassNotFoundException e ) {\n" +
                    "                             throw  new RuntimeException(e);\n" +
                    "                         }\n            }\n");
        }

    }

    private static String deserializeEnum(String propertyName, boolean multiplicityOne, String pureExternalPackage)
    {
        if (multiplicityOne)
        {
            return "try { this._" + propertyName + " = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) ((org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport)Class.forName(\"" + pureExternalPackage + ".PureExternal\").getMethod(\"_getExecutionSupport\").invoke(null)).getMetadata().getEnum(((String)in.readObject()).substring(6), (String)in.readObject()); } " +
                    "catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException | ClassNotFoundException e ) {\n" +
                    "         throw  new RuntimeException(e);\n" +
                    "     };";
        }
        else
        {
            return "";
        }
    }

    static String buildFactory(String className)
    {
        return buildFactoryConstructor(className) +
                "    public static final CoreInstanceFactory FACTORY = new org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.BaseJavaModelCoreInstanceFactory()\n" +
                "    {\n" +
                buildFactoryMethods(className) +
                buildFactorySupports() +
                "    };\n" +
                "\n";
    }

    static String buildFactorySupports()
    {
        return "        @Override\n" +
                "        public boolean supports(String classifierPath)\n" +
                "        {\n" +
                "            return tempFullTypeId.equals(classifierPath);\n" +
                "        }\n";
    }

    static String buildFactoryMethods(String className)
    {
        return "        @Override\n" +
                "        public CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, CoreInstance classifier, ModelRepository repository, boolean persistent)\n" +
                "        {\n" +
                "            return new " + className + "(name, sourceInformation, classifier);\n" +
                "        }\n" +
                "\n";
    }

    public static String buildMetaInfo(CoreInstance classGenericType, ProcessorSupport processorSupport, boolean lazy)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String fullId = PackageableElement.getSystemPathForPackageableElement(_class, "::");
        return "    public static final String tempTypeName = \"" + Instance.getValueForMetaPropertyToOneResolved(_class, "name", processorSupport).getName() + "\";\n" +
                "    private static final String tempFullTypeId = \"" + fullId + "\";\n" +
                (lazy ? "" : "    private CoreInstance classifier;\n");
    }

    public static String buildSimpleConstructor(CoreInstance _class, String className, ProcessorSupport processorSupport, boolean usesInheritance)
    {
        return "    public " + className + "(String id)\n" +
                "    {\n" +
                "        super(id);\n" +
                (Type.isBottomType(_class, processorSupport) ? "        throw new org.finos.legend.pure.m3.exception.PureExecutionException(\"Cannot instantiate " + PackageableElement.getUserPathForPackageableElement(_class, "::") + "\");\n" : "") +
                "    }\n" +
                "\n";
    }

    private static String buildGetClassifier()
    {
        return "    @Override\n" +
                "    public CoreInstance getClassifier()\n" +
                "    {\n" +
                "        return this.classifier;\n" +
                "    }\n";
    }

    static String buildGetKeys(CoreInstance cls, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> simplePropertiesByName = processorSupport.class_getSimplePropertiesByName(cls);
        return "    @Override\n" +
                "    public RichIterable<String> getKeys()\n" +
                "    {\n" +
                "        return Lists.immutable." + (simplePropertiesByName.isEmpty() ? "empty()" : simplePropertiesByName.keysView().makeString("with(\"", "\", \"", "\")")) + ";\n" +
                "    }\n" +
                "\n";
    }

    static String buildFactoryConstructor(String className)
    {
        return "    public " + className + "(String name, SourceInformation sourceInformation, CoreInstance classifier)\n" +
                "    {\n" +
                "        this(name == null ? \"Anonymous_NoCounter\": name);\n" +
                "        this.setSourceInformation(sourceInformation);\n" +
                "        this.classifier = classifier;\n" +
                "    }\n" +
                "\n";
    }

    public static String buildGetValueForMetaPropertyToOne(CoreInstance classGenericType, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        RichIterable<CoreInstance> toOneProperties = processorSupport.class_getSimpleProperties(_class).selectWith(IS_TO_ONE, processorSupport);
        return "    @Override\n" +
                "    public CoreInstance getValueForMetaPropertyToOne(String keyName)\n" +
                "    {\n" +
                "        switch (keyName)\n" +
                "        {\n" +
                toOneProperties.collect(property ->
                        "            case \"" + property.getName() + "\":\n" +
                                "            {\n" +
                                "                return ValCoreInstance.toCoreInstance(_" + property.getName() + "());\n" +
                                "            }\n").makeString("") +
                "            default:\n" +
                "            {\n" +
                "                return super.getValueForMetaPropertyToOne(keyName);\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n";
    }

    public static String buildGetValueForMetaPropertyToMany(CoreInstance classGenericType, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        RichIterable<CoreInstance> toManyProperties = processorSupport.class_getSimpleProperties(_class).rejectWith(IS_TO_ONE, processorSupport);
        return "    @Override\n" +
                "    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)\n" +
                "    {\n" +
                "        switch (keyName)\n" +
                "        {\n" +
                toManyProperties.collect(property ->
                        "            case \"" + property.getName() + "\":\n" +
                                "            {\n" +
                                "                return ValCoreInstance.toCoreInstances(_" + property.getName() + "());\n" +
                                "            }\n").makeString("") +
                "            default:\n" +
                "            {\n" +
                "                return super.getValueForMetaPropertyToMany(keyName);\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n";

    }

    public static String buildSimpleProperties(final CoreInstance classGenericType, final FullPropertyImplementation propertyImpl, final ProcessorContext processorContext, final ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        final String ownerClassName = TypeProcessor.javaInterfaceForType(_class);
        final String ownerTypeParams = ClassProcessor.typeParameters(_class);

        return processorSupport.class_getSimpleProperties(_class).collect(new Function<CoreInstance, Object>()
        {
            @Override
            public Object valueOf(CoreInstance property)
            {
                CoreInstance unresolvedReturnType = ClassProcessor.getPropertyUnresolvedReturnType(property, processorSupport);
                CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);

                String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

                boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType, processorSupport) && Multiplicity.isToOne(returnMultiplicity, true);
                String returnTypeJava = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
                CoreInstance classOwner = Instance.getValueForMetaPropertyToOneResolved(property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(0), M3Properties.rawType, processorSupport);
                String classOwnerFullId = PackageableElement.getSystemPathForPackageableElement(classOwner, "::");
                return propertyImpl.build(property, name, unresolvedReturnType, returnType, returnMultiplicity, returnTypeJava, classOwnerFullId, ownerClassName, ownerTypeParams, processorContext);
            }
        }).makeString("", "\n", "\n");
    }

    public static String buildQualifiedProperties(CoreInstance classGenericType, final ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        final CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        return _Class.getQualifiedProperties(_class, processorContext.getSupport()).collect(new Function<CoreInstance, Object>()
        {
            @Override
            public Object valueOf(CoreInstance coreInstance)
            {
                return "public " + FunctionProcessor.functionSignature(coreInstance, false, false, true, "", processorContext, true) + "\n" +
                        "    {\n" +
                        "        " + FunctionProcessor.processFunctionDefinitionContent(_class, coreInstance, true, processorContext, processorContext.getSupport()) + "\n" +
                        "    }\n" +
                        "\n";
            }
        }).makeString("    ", "\n    ", "\n");
    }

    public static String buildCopy(CoreInstance classGenericType, String suffix, boolean copyGetterOverride, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = TypeProcessor.javaInterfaceForType(_class);
        String implClassName = JavaPackageAndImportBuilder.buildImplClassNameFromType(_class, suffix);
        String typeParams = ClassProcessor.typeParameters(_class);
        String classNamePlusTypeParams = className + (typeParams.isEmpty() ? "" : "<" + typeParams + "> ");

        return "    public " + classNamePlusTypeParams + " copy()\n" +
                "    {\n" +
                "        return new " + implClassName + "(this);\n" +
                "    }\n" +

                "    public " + implClassName + "(" + className + (typeParams.isEmpty() ? "" : "<" + typeParams + ">") + " src)\n" +
                "    {\n" +
                "        this(\"Anonymous_NoCounter\");\n" +
                "        this.classifier = ((" + implClassName + ")src).classifier;\n" +
                (copyGetterOverride ?
                        "        this.__getterOverrideToOneExec = ((" + implClassName + ")src).__getterOverrideToOneExec;\n" +
                                "        this.__getterOverrideToManyExec = ((" + implClassName + ")src).__getterOverrideToManyExec;\n" : "") +
                processorSupport.class_getSimpleProperties(_class).collect(property ->
                {
                    String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                    CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

                    CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
                    CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
                    String reversePropertyName = null;
                    if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
                    {
                        ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
                        CoreInstance reverseProperty = associationProperties.get(property == associationProperties.get(0) ? 1 : 0);
                        reversePropertyName = Property.getPropertyName(reverseProperty);
                    }

                    CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
                    String typeObject = TypeProcessor.typeToJavaObjectSingle(returnType, true, processorSupport);

                    boolean isToOne = Multiplicity.isToOne(multiplicity, false);
                    return "        this._" + name + " = " + (isToOne ? "(" + typeObject + ")((" + implClassName + ")src)._" + name : "Lists.mutable.ofAll(((" + implClassName + ")src)._" + name + ")") + ";\n" +
                            (reversePropertyName == null ? "" :
                                    isToOne ?
                                            "        if (this._" + name + " != null)\n" +
                                                    "        {\n" +
                                                    "            this._" + name + "._reverse_" + reversePropertyName + "(this);\n" +
                                                    "        }\n"
                                            :
                                            "        for (" + typeObject + " v : (RichIterable<? extends " + typeObject + ">) this._" + name + ")\n" +
                                                    "        {\n" +
                                                    "            v._reverse_" + reversePropertyName + "(this);\n" +
                                                    "        }\n");
                }).makeString("") +
                "    }\n";

    }

    static String buildEquality(CoreInstance classGenericType, String suffix, boolean useMethodForEquals, boolean useMethodForHashcode, boolean lazy, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = TypeProcessor.javaInterfaceForType(_class);

        ListIterable<CoreInstance> equalityProperties = _Class.getEqualityKeyProperties(_class, processorContext.getSupport());

        String equalsCompilationClass = ClassProcessor.requiresCompilationImpl(processorContext.getSupport(), _class) ?
                " && o.getClass() != " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_class, ClassImplIncrementalCompilationProcessor.CLASS_IMPL_SUFFIX) + ".class" : "";

        return equalityProperties.isEmpty() ? "" : "public boolean pureEquals(Object o)\n" +
                "{\n" +
                "    if (this == o)\n" +
                "    {\n" +
                "        return true;\n" +
                "    }\n" +
                "    if (o == null || " + (lazy ? "(o.getClass() != " + JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(_class) + ".class && o.getClass() !=" + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_class) + ".class" + equalsCompilationClass + "))" : "getClass() != o.getClass())\n") +
                "    {\n" +
                "        return false;\n" +
                "    }\n" +
                "    " + className + (useMethodForEquals ? "" : suffix) + " that = (" + className + (useMethodForEquals ? "" : suffix) + ")o;\n" +
                equalityProperties.collect(coreInstance ->
                {
                    CoreInstance functionType = processorSupport.function_getFunctionType(coreInstance);
                    CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, M3Properties.rawType, processorSupport);
                    CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
                    if (returnType != null &&
                            Multiplicity.isToOne(returnMultiplicity, true) &&
                            Lists.immutable.with(M3Paths.Boolean, M3Paths.Float, M3Paths.Integer).contains(PackageableElement.getUserPathForPackageableElement(returnType)))
                    {
                        // Java primitive
                        return "    if (this._" + coreInstance.getName() + (useMethodForEquals ? "()" : "") + " != that._" + coreInstance.getName() + (useMethodForEquals ? "()" : "") + ")\n    {\n        return false;\n    }\n";
                    }
                    else
                    {
                        return "    if (!CompiledSupport.equal(this._" + coreInstance.getName() + (useMethodForEquals ? "()" : "") + ", that._" + coreInstance.getName() + (useMethodForEquals ? "()" : "") + "))\n    {\n        return false;\n    }\n";
                    }
                }).makeString("") +
                "    return true;\n" +
                "}\n" +
                "\n" +
                "public int pureHashCode()\n" +
                "{\n" +
                equalityProperties.collect(property -> "CompiledSupport.safeHashCode(this._" + property.getName() + (useMethodForHashcode ? "()" : "") + ")").makeString("    int result = ", ";\n    result = 31 * result + ", ";\n    return result;\n") +
                "}\n";
    }

    public static String buildPropertyStandardWriteSeverReverseToOne(String name, String owner, String typePrimitive, boolean isPrimitive, boolean setCachedOrMutated)
    {
        return (isPrimitive ? "" :
                "\n" +
                        "    public void _reverse_" + name + "(" + typePrimitive + " val)\n" +
                        "    {\n" +
                        (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                        "        " + owner + "._" + name + " = val;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void _sever_reverse_" + name + "(" + typePrimitive + " val)\n" +
                        "    {\n" +
                        (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                        "        " + owner + "._" + name + " = null;\n" +
                        "    }\n") +
                "\n";
    }

    public static String buildPropertyStandardWriteToOneBuilders(CoreInstance property, CoreInstance propertyReturnGenericType, String name, String owner, String className, String typeObject, String defaultValue, String reversePropertyName, String typePrimitive, boolean setCachedOrMutated, ProcessorContext processorContext)
    {
        return buildPropertyToOneSetOne(name, owner, className, reversePropertyName, typePrimitive, setCachedOrMutated) +
                buildPropertyToOneSetMany(name, owner, className, typeObject, reversePropertyName, setCachedOrMutated) +
                buildPropertyToOneRemove(name, owner, className, defaultValue, setCachedOrMutated) +
                buildPropertyToOneSetterCoreInstance(property, propertyReturnGenericType, className, name, processorContext);
    }

    private static String buildPropertyToOneSetOne(String name, String owner, String className, String reversePropertyName, String typePrimitive, boolean setCachedOrMutated)
    {
        return "    public " + className + " _" + name + "(" + typePrimitive + " val)\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                (reversePropertyName == null ? "" : "        if (" + owner + "._" + name + " != null) {" + owner + "._" + name + "._sever_reverse_" + reversePropertyName + "(" + owner + ");}\n") +
                "        " + owner + "._" + name + " = val;\n" +
                (reversePropertyName == null ? "" : "        if (val != null) {val._reverse_" + reversePropertyName + "(" + owner + ");}\n") +
                "        return this;\n" +
                "    }\n" +
                "\n";
    }

    private static String buildPropertyToOneSetMany(String name, String owner, String className, String typeObject, String reversePropertyName, boolean setCachedOrMutated)
    {
        return "    public " + className + " _" + name + "(RichIterable<? extends " + typeObject + "> val)\n" +
                "    {\n" +
                "        return _" + name + "(val.getFirst());\n" +
                "    }\n" +
                "\n";
    }

    private static String buildPropertyToOneRemove(String name, String owner, String className, String defaultValue, boolean setCachedOrMutated)
    {
        return "    public " + className + " _" + name + "Remove()\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                "        " + owner + "._" + name + " = " + defaultValue + ";\n" +
                "        return this;\n" +
                "    }\n" +
                "\n";
    }

    public static String buildPropertyStandardSeverReverseToMany(String name, String owner, String typePrimitive, boolean isPrimitive, boolean setCachedOrMutated)
    {
        return isPrimitive ? "" :
                "\n" +
                        "    public void _reverse_" + name + "(" + typePrimitive + " val)\n" +
                        "    {\n" +
                        (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                        "        if (!(" + owner + "._" + name + " instanceof MutableList))\n" +
                        "        {\n" +
                        "            " + owner + "._" + name + " = " + owner + "._" + name + ".toList();\n" +
                        "        }\n" +
                        "        ((MutableList)" + owner + "._" + name + ").add(val);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void _sever_reverse_" + name + "(" + typePrimitive + " val)\n" +
                        "    {\n" +
                        (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                        "        if (!(" + owner + "._" + name + " instanceof MutableList))\n" +
                        "        {\n" +
                        "            " + owner + "._" + name + " = " + owner + "._" + name + ".toList();\n" +
                        "        }\n" +
                        "        ((MutableList)" + owner + "._" + name + ").remove(val);\n" +
                        "    }\n" +
                        "\n";

    }

    public static String buildPropertyStandardWriteToManyBuilders(CoreInstance property, CoreInstance propertyReturnGenericType, String name, String owner, String className, String typeObject, String reversePropertyName, String typePrimitive, CoreInstance rawType, boolean setCachedOrMutated, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        return "    private " + className + " _" + name + "(" + typePrimitive + " val, boolean add)\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                (rawType == null ? "if(val instanceof RichIterable){_" + name + "((RichIterable<? extends " + typeObject + ">)val, add);}else{" : "") +
                (rawType != null && !processorSupport.type_isPrimitiveType(rawType) ? "        if (val == null)\n" +
                        "        {\n" +
                        "            if (!add)\n" +
                        "            {\n" +
                        (reversePropertyName == null ? "" :
                                "                for (" + typeObject + " v : (RichIterable<? extends " + typeObject + ">) " + owner + "._" + name + ")\n" +
                                        "                {\n" +
                                        "                    v._sever_reverse_" + reversePropertyName + "(" + owner + ");\n" +
                                        "                }\n") +
                        "                " + owner + "._" + name + " = Lists.mutable.with();\n" +
                        "            }\n" +
                        "            return this;\n" +
                        "        }\n" : "") +
                "        if (add)\n" +
                "        {\n" +
                "            if (!(" + owner + "._" + name + " instanceof MutableList))\n" +
                "            {\n" +
                "                " + owner + "._" + name + " = " + owner + "._" + name + ".toList();\n" +
                "            }\n" +
                "            ((MutableList)" + owner + "._" + name + ").add(val);\n" +
                "        }\n" +
                "        else\n" +
                "        {\n" +
                (reversePropertyName == null ? "" :
                        "            for (" + typeObject + " v : (RichIterable<? extends " + typeObject + ">) " + owner + "._" + name + ")\n" +
                                "            {\n" +
                                "                v._sever_reverse_" + reversePropertyName + "(" + owner + ");\n" +
                                "            }\n") +
                "            " + owner + "._" + name + " = (val == null ? Lists.mutable.empty() : Lists.mutable.with(val));\n" +
                "        }\n" +
                (reversePropertyName == null ? "" : "        val._reverse_" + reversePropertyName + "(" + owner + ");\n") +
                (rawType == null ? "}" : "") +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    private " + className + " _" + name + "(RichIterable<? extends " + typeObject + "> val, boolean add)\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                "        if (add)\n" +
                "        {\n" +
                "            if (!(" + owner + "._" + name + " instanceof MutableList))\n" +
                "            {\n" +
                "                " + owner + "._" + name + " = " + owner + "._" + name + ".toList();\n" +
                "            }\n" +
                "            ((MutableList)" + owner + "._" + name + ").addAllIterable(val);\n" +
                "        }\n" +
                "        else\n" +
                "        {\n" +
                (reversePropertyName == null ? "" :
                        "            for (" + typeObject + " v : (RichIterable<? extends " + typeObject + ">) " + owner + "._" + name + ")\n" +
                                "            {\n" +
                                "                v._sever_reverse_" + reversePropertyName + "(" + owner + ");\n" +
                                "            }\n") +
                "            " + owner + "._" + name + " = val;\n" +
                "        }\n" +
                (reversePropertyName == null ? "" :
                        "        for (" + typeObject + " v : val)\n" +
                                "        {\n" +
                                "            v._reverse_" + reversePropertyName + "(" + owner + ");\n" +
                                "        }\n") +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                buildPropertyToManySetter(name, owner, className, typeObject) +
                buildPropertyToManyAdd(name, owner, className, typeObject) +
                buildPropertyToManyAddAll(name, owner, className, typeObject) +
                buildPropertyToManyRemove(name, owner, className, setCachedOrMutated) +
                buildPropertyToManyRemoveItem(name, owner, className, typeObject, setCachedOrMutated) +
                (processorContext.getGenerator().isStubType(property, propertyReturnGenericType) ?
                        buildPropertyToManyAddCoreInstance(name, owner, className) +
                                buildPropertyToManyAddAllCoreInstance(name, owner, className) +
                                buildPropertyToManySetterCoreInstance(className, name) +
                                buildPropertyToManyRemoveItemCoreInstance(className, name) : "");
    }

    private static String buildPropertyToManySetter(String name, String owner, String className, String typeObject)
    {
        return "    public " + className + " _" + name + "(RichIterable<? extends " + typeObject + "> val)\n" +
                "    {\n" +
                "        return " + owner + "._" + name + "(val, false);\n" +
                "    }\n" +
                "\n";
    }

    private static String buildPropertyToManyAdd(String name, String owner, String className, String typeObject)
    {
        return "    public " + className + " _" + name + "Add(" + typeObject + " val)\n" +
                "    {\n" +
                "        return " + owner + "._" + name + "(Lists.immutable.with(val), true);\n" +
                "    }\n" +
                "\n";
    }

    private static String buildPropertyToManyAddAll(String name, String owner, String className, String typeObject)
    {
        return "    public " + className + " _" + name + "AddAll(RichIterable<? extends " + typeObject + "> val)\n" +
                "    {\n" +
                "        return " + owner + "._" + name + "(val, true);\n" +
                "    }\n" +
                "\n";
    }


    private static String buildPropertyToManyRemove(String name, String owner, String className, boolean setCachedOrMutated)
    {
        return "    public " + className + " _" + name + "Remove()\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                "        " + owner + "._" + name + " = Lists.mutable.empty();\n" +
                "        return this;\n" +
                "    }\n" +
                "\n";
    }

    private static String buildPropertyToManyRemoveItem(String name, String owner, String className, String typeObject, boolean setCachedOrMutated)
    {
        return "    public " + className + " _" + name + "Remove(" + typeObject + " val)\n" +
                "    {\n" +
                (setCachedOrMutated ? "        " + owner + "._" + name + "();\n" : "") +
                "        if (!(" + owner + "._" + name + " instanceof MutableList))\n" +
                "        {\n" +
                "            " + owner + "._" + name + " = " + owner + "._" + name + ".toList();\n" +
                "        }\n" +
                "        ((MutableList)" + owner + "._" + name + ").remove(val);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n";
    }

    public static String buildProperty(CoreInstance property, String className, String owner, String classOwnerFullId, String name, CoreInstance returnType, CoreInstance unresolvedReturnType, CoreInstance multiplicity, ProcessorSupport processorSupport, boolean includeGettor, ProcessorContext processorContext)
    {
        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
        CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
        String reversePropertyName = null;
        if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
        {
            ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
            CoreInstance reverseProperty = associationProperties.get(property == associationProperties.get(0) ? 1 : 0);
            reversePropertyName = Property.getPropertyName(reverseProperty);
        }

        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
        boolean isOverrider = M3Properties.elementOverride.equals(name);
        boolean isClassifierGenericType = "classifierGenericType".equals(name);
        boolean isDataType = rawType != null && Instance.instanceOf(rawType, M3Paths.DataType, processorSupport);
        boolean isPrimitive = rawType != null && Instance.instanceOf(rawType, M3Paths.PrimitiveType, processorSupport);
        boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType, processorSupport) && Multiplicity.isToOne(multiplicity, true);
//        boolean isFromAssociation = Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport), M3Paths.Association, processorSupport);

        String typePrimitive = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
        String typeObject = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);
        String defaultValue = TypeProcessor.defaultValue(rawType);

        if (Multiplicity.isToOne(multiplicity, false))
        {
            //always include the reverse setter - possible that the Association is defined for a super-class, and the association-end
            // was overridden at the concrete class level.  In this case, the reverse needs to exist due to the super-class interface,
            // it just won't be called.
            return buildPropertyStandardWriteToOneBuilders(property, returnType, name, owner, className, typeObject, defaultValue, reversePropertyName, typePrimitive, false, processorContext) +
                    (includeGettor ? buildPropertyStandardWriteSeverReverseToOne(name, owner, typePrimitive, isPrimitive, false) +
                            buildPropertyToOneGetterCoreInstance(property, returnType, name, processorContext) +
                            buildPropertyToOneGetter(owner, classOwnerFullId, name, isOverrider, isClassifierGenericType, isDataType, typePrimitive) : "");
        }
        else
        {
            //always include the reverse setter - possible that the Association is defined for a super-class, and the association-end
            // was overridden at the concrete class level.  In this case, the reverse needs to exist due to the super-class interface,
            // it just won't be called.
            return buildPropertyStandardWriteToManyBuilders(property, returnType, name, owner, className, typeObject, reversePropertyName, typePrimitive, rawType, false, processorSupport, processorContext) +
                    (includeGettor ? buildPropertyStandardSeverReverseToMany(name, owner, typePrimitive, isPrimitive, false) +
                            buildPropertyToManyGetter(owner, classOwnerFullId, name, isOverrider, isClassifierGenericType, isDataType, typePrimitive) : "") +
                    buildPropertyToManyGetterCoreInstance(property, returnType, name, processorContext);
        }
    }

    private static String buildPropertyToManyGetter(String owner, String classOwnerFullId, String name, boolean isOverrider, boolean isClassifierGenericType, boolean isDataType, String typeObject)
    {
        return "    public RichIterable<? extends " + typeObject + "> _" + name + "()\n" +
                "    {\n" +
                "        return " + (isDataType || isOverrider || isClassifierGenericType ? owner + "._" + name + ";\n" :
                owner + "._elementOverride() == null || !GetterOverrideExecutor.class.isInstance(" + owner + "._elementOverride()) ? " + owner + "._" + name + " : (RichIterable<? extends " + typeObject + ">)((GetterOverrideExecutor)" + owner + "._elementOverride()).executeToMany(" + owner + ", \"" + classOwnerFullId + "\", \"" + name + "\");\n") +
                "    }\n";
    }

    private static String buildPropertyToOneGetter(String owner, String classOwnerFullId, String name, boolean isOverrider, boolean isClassifierGenericType, boolean isDataType, String typeObject)
    {
        return "    public " + typeObject + " _" + name + "()\n" +
                "    {\n" +
                "        return " + (isDataType || isOverrider || isClassifierGenericType ? owner + "._" + name + ";\n" :
                owner + "._elementOverride() == null || !GetterOverrideExecutor.class.isInstance(" + owner + "._elementOverride()) ? " + owner + "._" + name + " : (" + typeObject + ")((GetterOverrideExecutor)" + owner + "._elementOverride()).executeToOne(" + owner + ", \"" + classOwnerFullId + "\", \"" + name + "\");\n") +
                "    }\n";
    }

    public static String buildPropertyToOneGetterCoreInstance(CoreInstance property, CoreInstance propertyReturnGenericType, String name, ProcessorContext processorContext)
    {
        return processorContext.getGenerator().requiresCoreInstanceMethods(property, propertyReturnGenericType) ?
                "    public org.finos.legend.pure.m4.coreinstance.CoreInstance _" + name + "CoreInstance()\n" +
                        "    {\n" +
                        "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                        "    }\n" +
                        "\n" : "";
    }

    public static String buildPropertyToManyGetterCoreInstance(CoreInstance property, CoreInstance propertyReturnGenericType, String name, ProcessorContext processorContext)
    {
        return processorContext.getGenerator().requiresCoreInstanceMethods(property, propertyReturnGenericType) ?
                "    public RichIterable<org.finos.legend.pure.m4.coreinstance.CoreInstance> _" + name + "CoreInstance()\n" +
                        "    {\n" +
                        "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                        "    }\n" +
                        "\n" : "";
    }

    public static String buildPropertyToOneSetterCoreInstance(CoreInstance property, CoreInstance propertyReturnGenericType, String className, String name, ProcessorContext processorContext)
    {
        return processorContext.getGenerator().isStubType(property, propertyReturnGenericType) ?
                "    public " + className + " _" + name + "CoreInstance(org.finos.legend.pure.m4.coreinstance.CoreInstance val)\n" +
                        "    {\n" +
                        "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                        "    }\n" +
                        "\n" : "";
    }

    public static String buildPropertyToManySetterCoreInstance(String className, String name)
    {
        return "    public " + className + " _" + name + "CoreInstance(RichIterable<? extends org.finos.legend.pure.m4.coreinstance.CoreInstance> val)\n" +
                "    {\n" +
                "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                "    }\n" +
                "\n";
    }

    public static String buildPropertyToManyRemoveItemCoreInstance(String className, String name)
    {
        return "    public " + className + " _" + name + "RemoveCoreInstance(CoreInstance val)\n" +
                "    {\n" +
                "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                "    }\n" +
                "\n";
    }

    public static String buildPropertyToManyAddCoreInstance(String name, String owner, String className)
    {
        return "    public " + className + " _" + name + "AddCoreInstance(CoreInstance val)\n" +
                "    {\n" +
                "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                "    }\n" +
                "\n";
    }

    public static String buildPropertyToManyAddAllCoreInstance(String name, String owner, String className)
    {
        return "    public " + className + " _" + name + "AddAllCoreInstance(RichIterable<? extends CoreInstance> val)\n" +
                "    {\n" +
                "        throw new UnsupportedOperationException(\"Not supported in Compiled Mode at this time\");\n" +
                "    }\n" +
                "\n";
    }


    private static String getterOverrides(String classNamePlusTypeParams)
    {
        return "    private PureFunction2Wrapper __getterOverrideToOneExec;\n" +
                "    private PureFunction2Wrapper __getterOverrideToManyExec;\n" +
                "    public " + classNamePlusTypeParams + " __getterOverrideToOneExec(PureFunction2Wrapper f2)\n" +
                "    {\n" +
                "        this.__getterOverrideToOneExec = f2;" +
                "        return this;\n" +
                "    }\n" +
                "    public " + classNamePlusTypeParams + " __getterOverrideToManyExec(PureFunction2Wrapper f2)\n" +
                "    {\n" +
                "        this.__getterOverrideToManyExec = f2;" +
                "        return this;\n" +
                "    }\n" +
                "    public Object executeToOne(CoreInstance instance, String fullSystemClassName, String propertyName)\n" +
                "    {\n" +
                "        return this.__getterOverrideToOneExec.value(instance, Pure.getProperty(fullSystemClassName, propertyName,((CompiledExecutionSupport)__getterOverrideToOneExec.getExecutionSupport()).getMetadataAccessor()), __getterOverrideToOneExec.getExecutionSupport());\n" +
                "    }\n" +
                "    public ListIterable executeToMany(CoreInstance instance, String fullSystemClassName, String propertyName)\n" +
                "    {\n" +
                "        return (ListIterable)this.__getterOverrideToManyExec.value(instance, Pure.getProperty(fullSystemClassName, propertyName,((CompiledExecutionSupport)__getterOverrideToOneExec.getExecutionSupport()).getMetadataAccessor()), __getterOverrideToManyExec.getExecutionSupport());\n" +
                "    }\n";

    }

    public static String validate(CoreInstance _class, String className, final CoreInstance classGenericType, final ProcessorContext processorContext, RichIterable<CoreInstance> properties)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        final SetIterable<? extends CoreInstance> localConstraints = Sets.immutable.withAll(_class.getValueForMetaPropertyToMany(M3Properties.constraints));
        ListIterable<CoreInstance> allConstraints = _Class.computeConstraintsInHierarchy(_class, processorSupport);
        return
                "    public " + className + " _validate(boolean goDeep, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        if (!this.hasCompileState(CompiledSupport.CONSTRAINTS_VALIDATED))\n" +
                        "        {\n" +
                        allConstraints.collect(new Function<CoreInstance, String>()
                        {
                            @Override
                            public String valueOf(CoreInstance constraint)
                            {
                                boolean registerLambdas = localConstraints.contains(constraint);
                                String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                                CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.owner, processorSupport);
                                CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport);
                                String eval = "(Boolean) " + ValueSpecificationProcessor.createFunctionForLambda(constraint, definition, registerLambdas, processorSupport, processorContext) + ".execute(Lists.mutable.with(this),es)";

                                String messageJavaFunction = null;
                                CoreInstance message = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.messageFunction, processorSupport);
                                if (message != null)
                                {
                                    messageJavaFunction = ValueSpecificationProcessor.createFunctionForLambda(constraint, message, registerLambdas, processorSupport, processorContext);
                                }

                                if (owner == null || "Global".equals(owner.getName()))
                                {
                                    CoreInstance expression = Instance.getValueForMetaPropertyToOneResolved(definition, M3Properties.expressionSequence, processorSupport);
                                    CoreInstance constraintClass = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.usageContext, processorSupport).getValueForMetaPropertyToOne(M3Properties._class);

                                    String errorMessage = message == null ?
                                            "\"Constraint :[" + ruleId + "] violated in the Class " + constraintClass.getValueForMetaPropertyToOne(M3Properties.name).getName() + "\"" :
                                            "\"Constraint :[" + ruleId + "] violated in the Class " + constraintClass.getValueForMetaPropertyToOne(M3Properties.name).getName() + ", Message: \" + (String) " + messageJavaFunction + ".execute(Lists.mutable.with(this),es)";

                                    return
                                            "            if (!(" + eval + "))\n" +
                                                    "            {\n" +
                                                    "                throw new org.finos.legend.pure.m3.exception.PureExecutionException(sourceInformation, " + errorMessage + ");\n" +
                                                    "            }\n";
                                }
                                else
                                {
                                    return "";
                                }
                            }
                        }).makeString("") +
                        "            this.addCompileState(CompiledSupport.CONSTRAINTS_VALIDATED);\n" +
                        "            if (goDeep)\n" +
                        "            {\n" +
                        properties.collect(new Function<CoreInstance, String>()
                        {
                            @Override
                            public String valueOf(CoreInstance property)
                            {
                                CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
                                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
                                if (rawType != null && !Instance.instanceOf(rawType, M3Paths.DataType, processorSupport) && !ClassProcessor.isPlatformClass(rawType))
                                {
                                    String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                                    CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);
                                    if (Multiplicity.isToOne(returnMultiplicity, false))
                                    {
                                        return
                                                "                if (this._" + name + "() != null){this._" + name + "()._validate(goDeep, sourceInformation, es);}\n";
                                    }
                                    else
                                    {
                                        String returnTypeJava = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);
                                        return
                                                "                for (" + returnTypeJava + " o : this._" + name + "())\n" +
                                                        "                {\n" +
                                                        "                    o._validate(goDeep, sourceInformation, es);\n" +
                                                        "                }\n";
                                    }
                                }
                                return "";
                            }
                        }).makeString("") +
                        "            }\n" +
                        "        }\n" +
                        "        return this;\n" +
                        "    }\n";
    }

    public static String buildGetFullSystemPath()
    {
        return "    @Override\n" +
                "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         return tempFullTypeId;\n" +
                "    }\n";
    }


    public interface FullPropertyImplementation
    {
        String build(CoreInstance property, String name, CoreInstance unresolvedReturnType, CoreInstance returnType, CoreInstance returnMultiplicity, String returnTypeJava, String classOwnerFullId, String ownerClassName, String ownerTypeParams, ProcessorContext processorContext);
    }
}