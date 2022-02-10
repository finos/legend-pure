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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class UnitInstanceImplProcessor
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
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n" +
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

    public static final String CLASS_IMPL_SUFFIX = "_Impl";

    public static String buildValSetterAndGetter(String interfaceName)
    {
        return  "    public java.lang.Number _val;\n" +
                "    public " + interfaceName + " _val(java.lang.Number val)\n" +
                "    {\n" +
                "        this._val = val;\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + " _val(RichIterable<? extends java.lang.Number> val)\n" +
                "    {\n" +
                "        return _val(val.getFirst());\n" +
                "    }\n" +
                "\n" +
                "    public " + interfaceName + " _valRemove()\n" +
                "    {\n" +
                "        this._val = null;\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public java.lang.Number _val()\n" +
                "    {\n" +
                "        return this._val;\n" +
                "}\n\n";
    }

    public static String buildUnitImplGetter(String unitTypeInterfaceName, String unitTypeImplName, String simpleName, String originalName, String measureClassName)
    {
        String lowerCasedJavaCompatibleName = UnitProcessor.convertToJavaCompatibleClassName(simpleName.toLowerCase());
        return  "    public " + unitTypeInterfaceName + " _unit()\n" +
                "    {\n" +
                "        if (" + measureClassName + "._" + lowerCasedJavaCompatibleName + "Impl" + " == null)" +
                "           {\n" +
                "        " + measureClassName +"._" + lowerCasedJavaCompatibleName + "Impl = new org.finos.legend.pure.generated." + unitTypeImplName + "(\"" + originalName + "\", this.es);\n" +
                "           }\n" +
                "        return " + measureClassName + "._" + lowerCasedJavaCompatibleName + "Impl;\n" +
                "    }\n\n";
    }

    public static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, boolean useJavaInheritance)
    {
        processorContext.setClassImplSuffix(CLASS_IMPL_SUFFIX);
        CoreInstance unit = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(unit));
        String typeParams = UnitProcessor.typeParameters(unit);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + "_Instance" + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit)) ||
                M3Paths.ConstraintsGetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit));

        ListIterable<String> allGeneralizations = UnitInstanceInterfaceProcessor.getAllGeneralizations(processorContext, processorSupport, unit, CLASS_IMPL_SUFFIX);

        String measureClassName = allGeneralizations.getFirst();

        String _extends = useJavaInheritance ? allGeneralizations.getFirst() : "ReflectiveCoreInstance";

        boolean hasFunctions = !_Class.getQualifiedProperties(unit, processorContext.getSupport()).isEmpty()
                || !_Class.computeConstraintsInHierarchy(unit,processorContext.getSupport()).isEmpty();

        return StringJavaSource.newStringJavaSource(_package, className, IMPORTS + (hasFunctions? FUNCTION_IMPORTS :"") + imports +
                "public class " + classNamePlusTypeParams + " extends " + _extends + " implements " + interfaceNamePlusTypeParams + (isGetterOverride? ", GetterOverrideExecutor" :"") + "\n" +
                "{\n" +
                buildMetaInfo(classGenericType, processorSupport, false) +
                buildSimpleConstructor(unit, className, processorSupport) +
                buildGetClassifier() +
                buildGetValueForMetaPropertyToOne(classGenericType, processorSupport) +
                buildValSetterAndGetter(interfaceNamePlusTypeParams) +
                buildUnitImplGetter(UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + typeParamsString, UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplClassNameFromType(unit)), unit.getName(), unit.getName(), measureClassName) +
                buildGetFullSystemPath() +
                "}");
    }

    public static String buildMetaInfo(CoreInstance classGenericType, ProcessorSupport processorSupport, boolean lazy)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String fullId = PackageableElement.getSystemPathForPackageableElement(_class, "::");
        return "    public static final String tempTypeName = \"" + Instance.getValueForMetaPropertyToOneResolved(_class, "name", processorSupport).getName() + "\";\n" +
                "    private static final String tempFullTypeId = \"" + fullId + "\";\n"+
                "    private"+ (lazy ? " volatile" : "") +" CoreInstance classifier;\n" +
                "   private ExecutionSupport es;\n";
    }

    public static String buildSimpleConstructor(CoreInstance _class, String className, ProcessorSupport processorSupport)
    {
        return "    public " + className + "(String id, ExecutionSupport es)\n" +
                "    {\n" +
                "        super(id, es);\n" +
                "        this.es = es;\n" +
                "        this._classifierGenericType =  new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"NO_ID\")._rawType((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)this.getValueForMetaPropertyToOne(\"unit\"));\n" +
                (Type.isBottomType(_class, processorSupport) ? "        throw new org.finos.legend.pure.m3.exception.PureExecutionException(\"Cannot instantiate " + PackageableElement.getUserPathForPackageableElement(_class, "::") + "\");\n" : "") +
                "    }\n" +
                "\n";
    }

    private static String buildGetClassifier()
    {
        return  "    @Override\n" +
                "    public CoreInstance getClassifier()\n" +
                "     {\n" +
                "        return this.classifier;\n"+
                "     }\n";
    }

    public static String buildGetValueForMetaPropertyToOne(CoreInstance classGenericType, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        RichIterable<CoreInstance> toOneProperties = processorSupport.class_getSimpleProperties(_class).select(p -> Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.multiplicity, processorSupport), false));
        return "    @Override\n" +
                "    public CoreInstance getValueForMetaPropertyToOne(String keyName)\n" +
                "    {\n" +
                "        switch (keyName)\n" +
                "        {\n" +
                toOneProperties.collect(property -> "            case \"" + property.getName() + "\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._" + PrimitiveUtilities.getStringValue(property.getValueForMetaPropertyToOne(M3Properties.name)) + "());\n" +
                "            }\n").makeString("") +

                "            case \"values\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._val());\n" +
                "            }\n" +
                "            case \"unit\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._unit());\n" +
                "            }\n" +
                "            case \"typeName\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(tempTypeName);\n" +
                "            }" +

                "            default:\n" +
                "            {\n" +
                "                return super.getValueForMetaPropertyToOne(keyName);\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n";
    }

    public static String buildGetFullSystemPath()
    {
        return  "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         return tempFullTypeId;\n" +
                "    }\n";
    }
}
