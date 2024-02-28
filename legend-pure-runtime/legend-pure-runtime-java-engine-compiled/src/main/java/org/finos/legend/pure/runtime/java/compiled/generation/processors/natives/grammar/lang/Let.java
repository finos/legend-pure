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
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Let extends AbstractNative implements Native
{
    public Let()
    {
        super("letFunction_String_1__T_m__T_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());

        String varName = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.values, processorSupport).getName();
        String value = transformedParams.get(1);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.multiplicity, processorSupport);

        if (Multiplicity.isZeroToOne(multiplicity) || Multiplicity.isToZero(multiplicity))
        {
            String typeO = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport), true, processorContext.getSupport());
            return "final " + typeO + " _" + varName + " = " + value;
        }
        else if (Multiplicity.isToOne(multiplicity, true))
        {
            String typeP = TypeProcessor.typeToJavaPrimitiveSingle(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport), processorContext.getSupport());
            return "final " + typeP + " _" + varName + " = " + value;
        }
        else
        {
            String typeO = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport), true, processorContext.getSupport());
            if (Instance.instanceOf(parametersValues.get(1), M3Paths.InstanceValue, processorContext.getSupport()))
            {
                return "final RichIterable<" + typeO + "> _" + varName + " = " + value;
            }
            else
            {
                return "final RichIterable<? extends " + typeO + "> _" + varName + " = " + value;
            }
        }
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction1<Object,Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, ExecutionSupport es)\n" +
                "            {\n" +
                "                return value(vars.get(1), es);\n" +
                "            }\n" +
                "            @Override\n" +
                "            public Object value(Object o, ExecutionSupport es)\n" +
                "            {\n" +
                "                return o;\n" +
                "            }\n" +
                "        }";
    }
}

