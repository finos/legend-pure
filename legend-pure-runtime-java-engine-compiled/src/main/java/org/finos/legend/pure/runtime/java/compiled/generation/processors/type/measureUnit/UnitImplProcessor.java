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
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class UnitImplProcessor
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

    public static String buildConversionFunction(CoreInstance unit, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        CoreInstance func = unit.getValueForMetaPropertyToOne("conversionFunction");

        return "@Override\n" +
                "    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<? extends java.lang.Object> _conversionFunction()\n" +
                "    {\n" +
                "        return this._conversionFunction(this.es);\n" +
                "    }\n" +

                "public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<? extends java.lang.Object> _conversionFunction(final ExecutionSupport es)\n" +
                "    {\n" +
                "        return new PureCompiledLambda().lambdaFunction(\n" +
                "((CompiledExecutionSupport)es).getMetadataAccessor().getLambdaFunction(\"" + processorContext.getIdBuilder().buildId(func) + "\")\n" +
                ").pureFunction(\n" +
                ValueSpecificationProcessor.createFunctionForLambda(unit, func, true, processorSupport, processorContext) + "\n" +
                ")\n" +
                ";\n" +
                "    }\n";
    }

    public static String buildMeasure(final CoreInstance unit, ProcessorSupport processorSupport)
    {
        CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(unit, M3Properties.measure, processorSupport);
        String measureInterfaceName = TypeProcessor.javaInterfaceForType(measure);
        String measureImplName = JavaPackageAndImportBuilder.buildImplClassNameFromType(measure);
        return "public " + measureInterfaceName + " _measure()\n" +
                "{\n" +
                "    if (_measure == null)\n" +
                "    {\n" +
                "        _measure = new org.finos.legend.pure.generated." + measureImplName + "(\"Anonymous_NoCounter\", this.es);\n" +
                "    }\n" +
                "    return _measure;\n" +
                "}\n";
    }

    public static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, final ProcessorSupport processorSupport, final boolean useJavaInheritance)
    {
        processorContext.setClassImplSuffix(CLASS_IMPL_SUFFIX);
        CoreInstance unit = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(unit, M3Properties.measure, processorSupport);
        String rawClassName = JavaPackageAndImportBuilder.buildImplClassNameFromType(unit);
        String className = UnitProcessor.convertToJavaCompatibleClassName(rawClassName);
        String typeParams = UnitProcessor.typeParameters(unit);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit)) ||
                M3Paths.ConstraintsGetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit));

        boolean hasFunctions = !_Class.getQualifiedProperties(unit, processorContext.getSupport()).isEmpty()
                || !_Class.computeConstraintsInHierarchy(unit, processorContext.getSupport()).isEmpty();

        return StringJavaSource.newStringJavaSource(_package, className, IMPORTS + (hasFunctions ? FUNCTION_IMPORTS : "") + imports +
                "public class " + classNamePlusTypeParams + " extends Root_meta_pure_metamodel_type_Unit_Impl implements " + interfaceNamePlusTypeParams + (isGetterOverride ? ", GetterOverrideExecutor" : "") + "\n" +
                "{\n" +
                buildMetaInfo(classGenericType, processorSupport, false, TypeProcessor.javaInterfaceForType(measure)) +
                buildSimpleConstructor(unit, className, processorSupport, useJavaInheritance) +
                buildGetClassifier() +
                buildGetValueForMetaPropertyToOne(classGenericType, processorSupport) +
                buildMeasure(unit, processorSupport) +
                buildConversionFunction(unit, processorContext, processorSupport) +
                buildGetFullSystemPath() +
                "}");
    }

    public static String buildMetaInfo(CoreInstance classGenericType, ProcessorSupport processorSupport, boolean lazy, String measureInterfaceName)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String fullId = PackageableElement.getSystemPathForPackageableElement(_class, "::");
        return "    public static final String tempTypeName = \"" + Instance.getValueForMetaPropertyToOneResolved(_class, "name", processorSupport).getName() + "\";\n" +
                "    private static final String tempFullTypeId = \"" + fullId + "\";\n" +
                "    private" + (lazy ? " volatile" : "") + " CoreInstance classifier;\n" +
                "    private ExecutionSupport es;\n" +
                "    public static " + measureInterfaceName + " _measure = null;\n";
    }

    public static String buildSimpleConstructor(CoreInstance unit, String className, ProcessorSupport processorSupport, boolean usesInheritance)
    {
        String id = "\"Anonymous_NoCounter\"";
        return "    public " + className + "(String id, ExecutionSupport es)\n" +
                "    {\n" +
                "        super(id);\n" +
                "        this.es = es;\n" +
                "        this._classifierGenericType =  new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"NO_ID\")._rawType((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)new Root_meta_pure_metamodel_type_Unit_Impl(" + id + "));\n" +
                (Type.isBottomType(unit, processorSupport) ? "        throw new org.finos.legend.pure.m3.exception.PureExecutionException(\"Cannot instantiate " + PackageableElement.getUserPathForPackageableElement(unit, "::") + "\");\n" : "") +
                "    }\n" +
                "\n";
    }

    private static String buildGetClassifier()
    {
        return "    @Override\n" +
                "    public CoreInstance getClassifier()\n" +
                "     {\n" +
                "        return this.classifier;\n" +
                "     }\n";
    }

    public static String buildGetValueForMetaPropertyToOne(final CoreInstance classGenericType, final ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        RichIterable<CoreInstance> toOneProperties = processorSupport.class_getSimpleProperties(_class).select(p -> Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.multiplicity, processorSupport), false));
        return "    @Override\n" +
                "    public CoreInstance getValueForMetaPropertyToOne(String keyName)\n" +
                "    {\n" +
                "        switch (keyName)\n" +
                "        {\n" +
                toOneProperties.collect(property ->
                "            case \"" + property.getName() + "\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._" + PrimitiveUtilities.getStringValue(property.getValueForMetaPropertyToOne(M3Properties.name)) + "());\n" +
                "            }\n").makeString("") +

                "            case \"conversionFunction\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._conversionFunction());\n" +
                "            }" +

                "            case \"measure\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._measure());\n" +
                "            } " +

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
        return "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         return tempFullTypeId;\n" +
                "    }\n";
    }
}
