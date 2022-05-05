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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Exists extends AbstractNativeFunctionGeneric
{
    public Exists()
    {
        super(getMethod(CompiledSupport.class, "exists"), "exists_T_MANY__Function_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String list = transformedParams.get(0);

        CoreInstance functionType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport);
        CoreInstance param = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();
        String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), true, processorSupport);

        return "null".equals(list) ? "false" : "CompiledSupport.exists(" + list + ", new DefendedPredicate<" + type + ">(){private final PureFunction1<" + type + ",Boolean> func = (PureFunction1<" + type + ",Boolean>)CoreGen.getSharedPureFunction(" + transformedParams.get(1) + ",es); public boolean accept(" + type + " param){return func.value(param, es);}})\n";
    }

    @Override
    public String buildBody() {

        return "new PureFunction2<Object, Object, Boolean>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Boolean value(Object t, final Object obj, final ExecutionSupport es)\n" +
                "            {\n" +
                "                org.eclipse.collections.api.block.predicate.Predicate predicate = obj instanceof org.eclipse.collections.api.block.predicate.Predicate\n" +
                "                   ? (org.eclipse.collections.api.block.predicate.Predicate) obj\n" +
                "                   : new DefendedPredicate<Object>(){\n" +
                "                           PureFunction1<Object, Boolean> func = (PureFunction1<Object, Boolean>)CoreGen.getSharedPureFunction((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function) obj, es);\n" +
                "                           @Override\n" +
                "                           public boolean accept(Object param){\n" +
                "                               return func.value(param, es);\n" +
                "                           }\n" +
                "                     };\n" +
                "                return CompiledSupport.exists(t, predicate);\n" +
                "            }\n" +
                "        }";
    }

}
