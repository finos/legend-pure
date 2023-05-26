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
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class MeasureImplProcessor
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

    public static final String CLASS_IMPL_SUFFIX = "_Impl";

    public static final Predicate2<CoreInstance, ProcessorSupport> IS_TO_ONE = MeasureImplProcessor::isToOne;

    public static String buildCanonicalUnit(String unitTypeInterfaceName, String simpleName, String unitTypeImplName)
    {
        String lowerCasedJavaCompatibleName = UnitProcessor.convertToJavaCompatibleClassName(simpleName.toLowerCase());
        return "    public " + unitTypeInterfaceName + " _canonicalUnit()\n" +
                "    {\n" +
                "        if (_" + lowerCasedJavaCompatibleName + "Impl" + " == null)" +
                "           {\n" +
                "               _" + lowerCasedJavaCompatibleName + "Impl = new org.finos.legend.pure.generated." + unitTypeImplName + "(\"" + UnitProcessor.convertToJavaCompatibleClassName(simpleName) + "\", this.es);\n" +
                "           }\n" +
                "        return _" + lowerCasedJavaCompatibleName + "Impl;\n" +
                "    }\n\n";
    }

    public static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, boolean useJavaInheritance)
    {
        processorContext.setClassImplSuffix(CLASS_IMPL_SUFFIX);
        final CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        CoreInstance canonicalUnit = Instance.getValueForMetaPropertyToOneResolved(measure, "canonicalUnit", processorSupport);
        ListIterable<? extends CoreInstance> nonCanonicalUnits = Instance.getValueForMetaPropertyToManyResolved(measure, "nonCanonicalUnits", processorSupport);
        ListIterable<? extends CoreInstance> allUnits = Lists.mutable.with(canonicalUnit).withAll(nonCanonicalUnits);

        String className = JavaPackageAndImportBuilder.buildImplClassNameFromType(measure);
        String typeParams = MeasureProcessor.typeParameters(measure);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;
        String interfaceNamePlusTypeParams = TypeProcessor.javaInterfaceForType(measure) + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(measure)) ||
                M3Paths.ConstraintsGetterOverride.equals(PackageableElement.getUserPathForPackageableElement(measure));

        boolean hasFunctions = !_Class.getQualifiedProperties(measure, processorContext.getSupport()).isEmpty()
                || !_Class.computeConstraintsInHierarchy(measure, processorContext.getSupport()).isEmpty();

        return StringJavaSource.newStringJavaSource(_package, className, IMPORTS + (hasFunctions ? FUNCTION_IMPORTS : "") + imports +
                "public class " + classNamePlusTypeParams + " extends " + "Root_meta_pure_metamodel_type_Measure_Impl" + " implements " + interfaceNamePlusTypeParams + (isGetterOverride ? ", GetterOverrideExecutor" : "") + "\n" +
                "{\n" +
                buildMetaInfo(classGenericType, processorSupport, false, allUnits) +
                buildSimpleConstructor(measure, className, processorSupport, useJavaInheritance, allUnits) +
                buildGetClassifier() +
                buildGetValueForMetaPropertyToOne(classGenericType, processorSupport) +
                buildCanonicalUnit(UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(canonicalUnit)), canonicalUnit.getName(), UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplClassNameFromType(canonicalUnit))) +
                buildGetFullSystemPath() +
                validate(className) +
                "}");
    }

    public static String validate(String className)
    {
        return
                "    public " + className + " _validate(boolean goDeep, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return this;\n" +
                        "    }\n";
    }

    public static String buildMetaInfo(CoreInstance classGenericType, ProcessorSupport processorSupport, boolean lazy, ListIterable<? extends CoreInstance> allUnits)
    {
        CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String fullId = PackageableElement.getSystemPathForPackageableElement(measure, "::");
        return "    public static final String tempTypeName = \"" + Instance.getValueForMetaPropertyToOneResolved(measure, "name", processorSupport).getName() + "\";\n" +
                "    private static final String tempFullTypeId = \"" + fullId + "\";\n" +
                "    private" + (lazy ? " volatile" : "") + " CoreInstance classifier;\n" +
                "    private ExecutionSupport es;\n" +

                allUnits.collect(unit -> "    public static " + UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + " _" + UnitProcessor.convertToJavaCompatibleClassName(unit.getName().toLowerCase()) + "Impl" + ";\n").makeString("");
    }

    public static String buildSimpleConstructor(CoreInstance _class, String className, ProcessorSupport processorSupport, boolean usesInheritance, ListIterable<? extends CoreInstance> allUnits)
    {
        String id = "\"Anonymous_NoCounter\"";
        return "    public " + className + "(String id, ExecutionSupport es)\n" +
                "    {\n" +
                "        super(id);\n" +
                "        this.es = es;\n" +
                "        this._classifierGenericType =  new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"NO_ID\")._rawType((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)new Root_meta_pure_metamodel_type_Measure_Impl(" + id + "));\n" +
                (Type.isBottomType(_class, processorSupport) ? "        throw new org.finos.legend.pure.m3.exception.PureExecutionException(\"Cannot instantiate " + PackageableElement.getUserPathForPackageableElement(_class, "::") + "\");\n" : "") +
                "\n    }\n" +
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

    public static String buildGetValueForMetaPropertyToOne(CoreInstance classGenericType, ProcessorSupport processorSupport)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        RichIterable<CoreInstance> toOneProperties = processorSupport.class_getSimpleProperties(_class).select(p -> isToOne(p, processorSupport));
        return "    @Override\n" +
                "    public CoreInstance getValueForMetaPropertyToOne(String keyName)\n" +
                "    {\n" +
                "        switch (keyName)\n" +
                "        {\n" +
                toOneProperties.collect(property ->
                        "            case \"" + property.getName() + "\":\n" +
                        "            {\n" +
                        "                return ValCoreInstance.toCoreInstance(this._" + Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName() + "());\n" +
                        "            }\n").makeString("") +

                "            case \"canonicalUnit\":\n" +
                "            {\n" +
                "                return ValCoreInstance.toCoreInstance(this._canonicalUnit());\n" +
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
        return "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         return tempFullTypeId;\n" +
                "    }\n";
    }

    private static boolean isToOne(CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);
        return Multiplicity.isToOne(multiplicity, false);
    }
}
