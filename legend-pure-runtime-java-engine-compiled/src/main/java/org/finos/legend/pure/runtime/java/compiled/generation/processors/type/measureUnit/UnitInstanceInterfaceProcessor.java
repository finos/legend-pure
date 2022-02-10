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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class UnitInstanceInterfaceProcessor
{
    private static final String IMPORTS = "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n";

    public static StringJavaSource buildInterface(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, boolean useJavaInheritance)
    {
        CoreInstance unit = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String interfaceName = UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + "_Instance";
        String typeParams = UnitProcessor.typeParameters(unit);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String interfaceNamePlusTypeParams = interfaceName + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit));

        String generalization = "";
        boolean hasGeneralization = Instance.getValueForMetaPropertyToManyResolved(unit, M3Properties.generalizations, processorContext.getSupport()).notEmpty();
        if (hasGeneralization)
        {
            ListIterable<String> allGeneralizations = getAllGeneralizations(processorContext, processorSupport, unit, "");
            generalization = ", " + allGeneralizations.makeString(",");
        }

        return StringJavaSource.newStringJavaSource(_package, interfaceName, IMPORTS + imports + "public interface " + interfaceNamePlusTypeParams + " extends CoreInstance" + generalization + "\n{\n" +
                (isGetterOverride ? "    " + interfaceNamePlusTypeParams + "  __getterOverrideToOneExec(PureFunction2Wrapper f2);\n" +
                        "    " + interfaceNamePlusTypeParams + "  __getterOverrideToManyExec(PureFunction2Wrapper f2);\n" : "") +

                ("    " + interfaceName + " _val(java.lang.Number val);\n" +
                        "    " + interfaceName + " _val(RichIterable<? extends java.lang.Number> val);\n" +
                        "    " + interfaceName + " _valRemove();\n" +
                        "    java.lang.Number _val();\n") +

                ("    " +  UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit)) + " _unit();\n") +

                "}");
    }

    static ListIterable<String> getAllGeneralizations(ProcessorContext processorContext, ProcessorSupport processorSupport, CoreInstance _class, String suffix)
    {
        return Instance.getValueForMetaPropertyToManyResolved(_class, M3Properties.generalizations, processorContext.getSupport()).collect(oneGeneralization ->
        {
            CoreInstance generalGenericType = Instance.getValueForMetaPropertyToOneResolved(oneGeneralization, M3Properties.general, processorSupport);
            String typeArgs = generalGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).collect(i -> TypeProcessor.typeToJavaObjectSingle(i, true, processorContext.getSupport())).makeString(",");
            return typeName(processorSupport, suffix, generalGenericType, typeArgs);
        }, Lists.mutable.empty());
    }

    private static String typeName(ProcessorSupport processorSupport, String suffix, CoreInstance generalGenericType, String typeArgs)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(generalGenericType, M3Properties.rawType, processorSupport);
        String typeArgsString = typeArgs.isEmpty() ? "" : "<" + typeArgs + ">";
        if (suffix.isEmpty())
        {
            return TypeProcessor.javaInterfaceForType(rawType) + typeArgsString;
        }
        else
        {
            return PackageableElement.getSystemPathForPackageableElement(rawType, "_") + suffix + typeArgsString;
        }
    }
}
