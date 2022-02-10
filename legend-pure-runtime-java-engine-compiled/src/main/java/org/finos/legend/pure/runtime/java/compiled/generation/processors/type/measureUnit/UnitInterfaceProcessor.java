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

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class UnitInterfaceProcessor
{
    private static final String IMPORTS = "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n";

    public static StringJavaSource buildInterface(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, boolean useJavaInheritance)
    {
        CoreInstance unit = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String interfaceName = UnitProcessor.convertToJavaCompatibleClassName(TypeProcessor.javaInterfaceForType(unit));
        String typeParams = UnitProcessor.typeParameters(unit);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String interfaceNamePlusTypeParams = interfaceName + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(unit));

        CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(unit, M3Properties.measure, processorSupport);

        return StringJavaSource.newStringJavaSource(_package, interfaceName, IMPORTS + imports + "public interface " + interfaceNamePlusTypeParams + " extends CoreInstance" + ", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit" + "\n{\n" +
                (isGetterOverride ? "    " + interfaceNamePlusTypeParams + "  __getterOverrideToOneExec(PureFunction2Wrapper f2);\n" +
                        "    " + interfaceNamePlusTypeParams + "  __getterOverrideToManyExec(PureFunction2Wrapper f2);\n" : "") +

                "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<? extends java.lang.Object> _conversionFunction(final ExecutionSupport es);\n" +

                TypeProcessor.javaInterfaceForType(measure) + " _measure();\n" +

                "}");
    }
}
