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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;

public class ExtractEnumValue extends AbstractNative
{
    public ExtractEnumValue()
    {
        super("extractEnumValue_Enumeration_1__String_1__T_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        final ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());
        final ProcessorSupport processorSupport = processorContext.getSupport();

        if (processorContext.getSupport().instance_instanceOf(parametersValues.get(0), M3Paths.InstanceValue))
        {
            String type = MetadataJavaPaths.buildMetadataKeyFromType(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.values, processorSupport));
            return "((" + FullJavaPaths.Enum + ")((CompiledExecutionSupport)es).getMetadata().getEnum(\"" + type + "\"," + transformedParams.get(1) + "))";
        }
        else
        {
            String type = transformedParams.get(0);
            String name = transformedParams.get(1);
            return "Pure.getEnumByName(" + type + ", " + name + ")";
        }
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<"  + FullJavaPaths.Enumeration + ", String, Object>()\n" +
               "        {\n" +
               "            @Override\n" +
               "            public Object value("  + FullJavaPaths.Enumeration + " enumeration, String name, ExecutionSupport es)\n" +
               "            {\n" +
               "                return Pure.getEnumByName(enumeration, name);\n" +
               "            }\n" +
               "        }";
    }
}