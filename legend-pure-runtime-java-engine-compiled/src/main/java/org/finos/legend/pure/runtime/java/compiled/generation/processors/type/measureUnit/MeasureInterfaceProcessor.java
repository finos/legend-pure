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

public class MeasureInterfaceProcessor
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

    public static StringJavaSource buildInterface(final String _package, final String imports, final CoreInstance classGenericType, final ProcessorContext processorContext, final ProcessorSupport processorSupport, final boolean useJavaInheritance)
    {
        final CoreInstance measure = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        final String interfaceName = TypeProcessor.javaInterfaceForType(measure);
        final String typeParams = MeasureProcessor.typeParameters(measure);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        final String interfaceNamePlusTypeParams = interfaceName + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(measure));

        return StringJavaSource.newStringJavaSource(_package, interfaceName, IMPORTS + imports + "public interface " + interfaceNamePlusTypeParams + " extends CoreInstance" + ", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure" + "\n{\n" +
                (isGetterOverride ? "    " + interfaceNamePlusTypeParams + "  __getterOverrideToOneExec(PureFunction2Wrapper f2);\n" +
                        "    " + interfaceNamePlusTypeParams + "  __getterOverrideToManyExec(PureFunction2Wrapper f2);\n" : "") +
                "    " + interfaceNamePlusTypeParams + " _validate(boolean goDeep, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, final ExecutionSupport es);" +
                "}");

    }
}
