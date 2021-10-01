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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class ClassLazyImplProcessor
{
    private static final String IMPORTS = "import java.util.concurrent.atomic.AtomicBoolean;\n" +
            "import org.eclipse.collections.api.list.ListIterable;\n" +
            "import org.eclipse.collections.api.list.MutableList;\n" +
            "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.eclipse.collections.api.map.MutableMap;\n" +
            "import org.eclipse.collections.impl.factory.Lists;\n" +
            "import org.eclipse.collections.impl.factory.Maps;\n" +
            "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
            "import org.eclipse.collections.api.map.ImmutableMap;\n" +
            "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
            "import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;\n" +
            "import org.finos.legend.pure.m4.ModelRepository;\n" +
            "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.serialization.model.*;\n" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n";


    private static final String QUALIFIER_IMPORTS =
            "import org.eclipse.collections.api.block.function.Function2;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n" +
            "import org.eclipse.collections.api.block.function.Function0;\n";

    private static final String LAZY_INITIALIZED_SUFFIX = "_initialized";

    public static final String CLASS_LAZYIMPL_SUFFIX = "_LazyImpl";

    static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = JavaPackageAndImportBuilder.buildImplClassNameFromType(_class, CLASS_LAZYIMPL_SUFFIX);
        String classInterfaceName = TypeProcessor.javaInterfaceForType(_class);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = classInterfaceName + typeParamsString;
        boolean hasQualifiers = !_Class.getQualifiedProperties(_class, processorContext.getSupport()).isEmpty();
        String systemPath = PackageableElement.getSystemPathForPackageableElement(_class, "::");

        boolean instanceOfGetterOverride = processorSupport.instance_instanceOf(_class, M3Paths.GetterOverride);

        processorContext.setClassImplSuffix(CLASS_LAZYIMPL_SUFFIX);
        return StringJavaSource.newStringJavaSource(_package, className, IMPORTS  + (hasQualifiers ? QUALIFIER_IMPORTS : "") + imports +
                "public class " + classNamePlusTypeParams + " extends PersistentReflectiveCoreInstance implements " + interfaceNamePlusTypeParams + "\n" +
                        "{\n" +
                        ClassImplProcessor.buildMetaInfo(classGenericType, processorSupport, true) +
                        buildLazyConstructor(className, processorSupport) +
                        lazyGetClassifier() +
                        (ClassProcessor.isPlatformClass(_class) ? buildFactory(className, systemPath) : "") +
                        (instanceOfGetterOverride ? lazyGetterOverride(interfaceNamePlusTypeParams) : "") +
                        ClassImplProcessor.buildGetValueForMetaPropertyToOne(classGenericType, processorSupport) +
                        ClassImplProcessor.buildSimpleProperties(classGenericType, new ClassImplProcessor.FullPropertyImplementation()
                        {
                            @Override
                            public String build(CoreInstance property, String name, CoreInstance unresolvedReturnType, CoreInstance returnType, CoreInstance returnMultiplicity, String returnTypeJava, String classOwnerFullId, String ownerClassName, String ownerTypeParams, ProcessorContext processorContext)
                            {
                                return "    public final AtomicBoolean _" + name + LAZY_INITIALIZED_SUFFIX + " = new AtomicBoolean(false);\n"+
                                        (Multiplicity.isToOne(returnMultiplicity, false) ?
                                        "    public " + returnTypeJava + " _" + name + ";\n" :
                                        "    public RichIterable _" + name + " = Lists.mutable.with();\n") +
                                        buildLazyProperty(property, ownerClassName + (ownerTypeParams.isEmpty() ? "" : "<" + ownerTypeParams + ">"), "this", classOwnerFullId, name, returnType, unresolvedReturnType, returnMultiplicity, processorContext.getSupport(), processorContext);
                            }
                        }, processorContext, processorSupport)+
                        ClassImplProcessor.buildQualifiedProperties(classGenericType, processorContext, processorSupport)+
                        buildLazyCopy(classGenericType, classInterfaceName, className, false, processorSupport)+
                        ClassImplProcessor.buildEquality(classGenericType, CLASS_LAZYIMPL_SUFFIX, true, false, true, processorContext, processorSupport)+
                        ClassImplProcessor.buildGetFullSystemPath() +
                        //Not supported on platform classes yet
                        (ClassProcessor.isPlatformClass(_class) ? "" : ClassImplProcessor.validate(_class, className, classGenericType, processorContext, processorSupport.class_getSimpleProperties(_class))) +
                        "}");
    }

    static String buildFactory(String className, String systemPath)
    {

        return buildFactoryConstructor(className) +
                "    public static final CoreInstanceFactory FACTORY = new BaseJavaModelCoreInstanceFactory()\n" +
                "    {\n" +
                ClassImplProcessor.buildFactoryMethods(className) +
                ClassImplProcessor.buildFactorySupports(systemPath) +
                "   };\n" +
                "\n";
    }

    static String buildFactoryConstructor(String className)
    {
        return "    public " + className + "(String name, SourceInformation sourceInformation, CoreInstance classifier)\n" +
                "    {\n" +
                "        super(name);\n" +
                "        this.setSourceInformation(sourceInformation);\n" +
                "        this.setClassifier(classifier);\n" +
                "        this.vals = Maps.immutable.empty();\n" +
                "    }\n";
    }


    private static String buildLazyConstructor(String className, ProcessorSupport processorSupport)
    {
        return "    MetadataLazy metadataLazy;\n" +
                "    final ImmutableMap<String, Object> vals;\n" +
                "    private String classifierId;\n" +
                "\n" +
                "    public " + className + "(Obj instance, MetadataLazy metadataLazy)\n" +
                "    {\n" +
                "        this(instance.getName(),instance.getSourceInformation(), metadataLazy.buildMap(instance), metadataLazy, instance.getClassifier());\n" +
                "    }\n" +
                "\n"+
                "    public " + className + "(String id, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, ImmutableMap<String, Object> vals, MetadataLazy metadataLazy, String classifierId)\n" +
                "    {\n" +
                "        super(id);\n" +
                "        this.setSourceInformation(sourceInformation);\n" +
                "        this.metadataLazy = metadataLazy;\n" +
                "        this.vals = vals;\n" +
                "        this.classifierId = classifierId;\n" +
                "    }\n"+
                "\n" +
                "    public " + className + "(String id, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, ImmutableMap<String, Object> vals, MetadataLazy metadataLazy, String classifierId, CoreInstance classifier)\n" +
                "    {\n" +
                "        this(id, sourceInformation, vals, metadataLazy, classifierId);\n" +
                "        this.classifier = classifier;\n" +
                "    }"+
                "\n";
    }

    private static String lazyGetClassifier()
    {
        return "    @Override\n" +
                "    public CoreInstance getClassifier()\n" +
                "    {\n" +
                "        CoreInstance result = this.classifier;\n" +
                "        if (result == null)\n" +
                "        {\n" +
                "            this.classifier = result = this.metadataLazy.getMetadata(\"meta::pure::metamodel::type::Class\", \"Root::\"+this.classifierId);\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n";
    }

    private static String lazyGetterOverride(String classNamePlusTypeParams)
    {
        return  "    public " + classNamePlusTypeParams + " __getterOverrideToOneExec(PureFunction2Wrapper f2)\n" +
                "    {\n" +
                "        throw new RuntimeException(\"Not Supported in Lazy Mode!\");\n" +
                "    }\n" +
                "    public " + classNamePlusTypeParams + " __getterOverrideToManyExec(PureFunction2Wrapper f2)\n" +
                "    {\n" +
                "        throw new RuntimeException(\"Not Supported in Lazy Mode!\");\n" +
                "    }\n";

    }

    private static String buildLazyProperty(CoreInstance property, String className, String owner, String classOwnerFullId, String name, CoreInstance returnType, CoreInstance unresolvedReturnType, CoreInstance multiplicity, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
        CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
        String reversePropertyName = null;
        if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
        {
            ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
            CoreInstance reverseProperty = associationProperties.get((property == associationProperties.get(0)) ? 1 : 0);
            reversePropertyName = Property.getPropertyName(reverseProperty);
        }

        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
        boolean isPrimitive = rawType != null && Instance.instanceOf(rawType, M3Paths.PrimitiveType, processorSupport);
        boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType, processorSupport) && Multiplicity.isToOne(multiplicity, true);

        String typePrimitive = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
        String typeObject = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);
        String defaultValue = TypeProcessor.defaultValue(rawType);

        if (Multiplicity.isToOne(multiplicity, false))
        {
            return ClassImplProcessor.buildPropertyStandardWriteSeverReverseToOne(name, owner, typePrimitive, isPrimitive, true) +
                    ClassImplProcessor.buildPropertyStandardWriteToOneBuilders(property, returnType, name, owner, className,typeObject, defaultValue, reversePropertyName, typePrimitive, true, processorContext) +
                    ClassImplProcessor.buildPropertyToOneGetterCoreInstance(property,returnType, name, processorContext) +
                    "    public " + typePrimitive + " _" + name + "()\n" +
                    "    {\n" +
                    "        if (!" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".get())\n" +
                    "        {\n" +
                    "            synchronized (" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ")\n" +
                    "            {\n" +
                    "                if (!" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".get())\n" +
                    "                {\n" +
                    "                    " + owner + "._" + name + " = (" + typePrimitive + ")" + owner + ".metadataLazy.valueToObject((RValue)this.vals.get(\""+name+"\"));\n" +
                    "                    " + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".set(true);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "        return " + owner + "._" + name + ";\n" +
                    "    }\n";
        }
        else
        {
            return  ClassImplProcessor.buildPropertyStandardSeverReverseToMany(name, owner, typePrimitive, isPrimitive, true)+
                    ClassImplProcessor.buildPropertyStandardWriteToManyBuilders(property, returnType, name, owner, className,typeObject, reversePropertyName, typePrimitive, rawType, true, processorSupport, processorContext)+
                    ClassImplProcessor.buildPropertyToManyGetterCoreInstance(property, returnType, name, processorContext) +
                    "    public RichIterable<? extends " + typeObject + "> _" + name + "()\n" +
                    "    {\n" +
                    "        if (!" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".get())\n" +
                    "        {\n" +
                    "            synchronized (" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ")\n" +
                    "            {\n" +
                    "                if (!" + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".get())\n" +
                    "                {\n" +
                    "                    " + owner + "._" + name + " = (RichIterable<? extends " + typeObject + ">)(Object)" + owner + ".metadataLazy.valuesToObjects((ListIterable<RValue>)this.vals.get(\""+name+"\"));\n" +
                    "                    " + owner + "._" + name + LAZY_INITIALIZED_SUFFIX + ".set(true);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n"+
                    "        return " + owner + "._" + name + ";\n"+
                    "    }\n";
        }
    }

    public static String buildLazyCopy(final CoreInstance classGenericType, final String classInterfaceName, final String classImplName, boolean copyGetterOverride, final ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String typeParams = ClassProcessor.typeParameters(_class);
        String classNamePlusTypeParams = classInterfaceName + (typeParams.isEmpty() ? "" : "<" + typeParams + "> ");

        String propertyCopy = processorSupport.class_getSimpleProperties(_class).collect(new Function<CoreInstance, Object>()
        {
            @Override
            public Object valueOf(CoreInstance property)
            {
                String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

                CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
                CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
                String reversePropertyName = null;
                if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
                {
                    ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
                    CoreInstance reverseProperty = (property == associationProperties.get(0)) ? associationProperties.get(1) : associationProperties.get(0);
                    reversePropertyName = Property.getPropertyName(reverseProperty);
                }

                CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
                String typeObject = TypeProcessor.typeToJavaObjectSingle(returnType, true, processorSupport);

                boolean isToOne = Multiplicity.isToOne(multiplicity, false);
                return "        synchronized (((" +  classImplName + ")src)._" + name + LAZY_INITIALIZED_SUFFIX + ")\n" +
                       "        {\n" +
                       "            this._" + name + " = " + (isToOne ? "(" + typeObject + ")((" + classImplName + ")src)._" + name : "FastList.newList(((" + classImplName + ")src)._" + name + ")") + ";\n" +
                       "            this._" + name + LAZY_INITIALIZED_SUFFIX + ".set(((" +  classImplName + ")src)._" + name + LAZY_INITIALIZED_SUFFIX + ".get());\n" +
                       "        }\n" +
                        (reversePropertyName == null ? "" :
                                (isToOne ?
                                        "        if (this._" + name + " != null)\n" +
                                                "        {\n" +
                                                "            this._" + name + "._reverse_" + reversePropertyName + "(this);\n" +
                                                "        }\n"
                                        :
                                        "        for (" + typeObject + " v : (RichIterable<? extends " + typeObject + ">) this._" + name + ")\n" +
                                                "        {\n" +
                                                "            v._reverse_" + reversePropertyName + "(this);\n" +
                                                "        }\n"));
            }
        }).makeString("");


        return  "    public " + classNamePlusTypeParams + " copy()\n" +
                "    {\n" +
                "        return new " + classImplName + "(this);\n" +
                "    }\n" +

                "    public " + classImplName +"(" + classInterfaceName + (typeParams.isEmpty() ? "" : "<" + typeParams + ">") + " src)\n" +
                "    {\n" +
                "        this(((" + classImplName + ")src).getName(), src.getSourceInformation(), ((" + classImplName + ")src).vals, ((" + classImplName + ")src).metadataLazy, (("+ classImplName + ")src).classifierId, ((" + classImplName + ")src).classifier );\n" +
                         propertyCopy +
                "    }\n";

    }


}
