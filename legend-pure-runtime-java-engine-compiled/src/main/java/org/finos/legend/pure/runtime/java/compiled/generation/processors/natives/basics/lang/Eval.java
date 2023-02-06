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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Eval extends AbstractNative
{
    public Eval()
    {
        super("eval_Function_1__V_m_", "eval_Function_1__T_n__V_m_", "eval_Function_1__T_n__U_p__V_m_",
                "eval_Function_1__T_n__U_p__W_q__V_m_", "eval_Function_1__T_n__U_p__W_q__X_r__V_m_",
                "eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_", "eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_",
                "eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);

        String parameters = String.join(", ", transformedParams);
        parameters = transformedParams.size() == 1 ? transformedParams.get(0) + ", new Object[]{}" : parameters;
        parameters = transformedParams.size() == 2 && "null".equals(transformedParams.get(1)) ? transformedParams.get(0) + ", new Object[]{null}" : parameters;

        String eval = "CoreGen.evaluate(es, (" + FullJavaPaths.Function + ")" + parameters + ")";
        return "((" + type + ")(Object)" + (Multiplicity.isToOne(multiplicity, false) ? eval : "CompiledSupport.toPureCollection(" + eval + ")") + ")";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "        @Override\n" +
                "        public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "        {\n" +
                "           org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function) vars.get(0);\n" +
                "           Object[] params = vars.size() == 1 ? new Object[]{} : vars.drop(1).toArray();\n" +
                "           Object value = CoreGen.evaluate(es, func, params);\n" +
                "           return value instanceof Iterable ? CompiledSupport.toPureCollection(value) : value;\n" +
                "        }\n" +
                "}";
    }
}
