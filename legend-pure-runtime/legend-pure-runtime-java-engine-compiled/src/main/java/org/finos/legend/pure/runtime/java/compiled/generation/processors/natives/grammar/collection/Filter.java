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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Filter extends AbstractNative implements Native
{
    public Filter()
    {
        super("filter_T_MANY__Function_1__T_MANY_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String list = transformedParams.get(0);

        boolean isLambdaFunction = Instance.instanceOf(parametersValues.get(1), M3Paths.InstanceValue, processorSupport) && ValueSpecification.instanceOf(parametersValues.get(1), M3Paths.LambdaFunction, processorSupport);
        CoreInstance function = isLambdaFunction ? Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport) : parametersValues.get(1);
        CoreInstance functionType = processorSupport.function_getFunctionType(function);
        CoreInstance parameter = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();
        String paramTypeObject = TypeProcessor.typeToJavaObjectWithMul(parameter.getValueForMetaPropertyToOne(M3Properties.genericType), parameter.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorSupport);
        if (isLambdaFunction)
        {
            String paramName = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport), M3Properties.parameters, processorSupport).get(0), M3Properties.name, processorSupport).getName();
            return "CompiledSupport.toPureCollection(" + list + ").select(new DefendedPredicate<" + paramTypeObject + ">(){public boolean accept(final " + paramTypeObject + " _" + paramName + "){" + FunctionProcessor.processFunctionDefinitionContent(topLevelElement, Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport), true, processorContext, processorSupport) + "}})";
        }
        else
        {
            return "CompiledSupport.toPureCollection(" + list + ").select(new DefendedPredicate<" + paramTypeObject + ">(){private final PureFunction1<" + paramTypeObject + ",Boolean> func=(PureFunction1<" + paramTypeObject + ",Boolean>)CoreGen.getSharedPureFunction(" + transformedParams.get(1) + ",es); public boolean accept(final " + paramTypeObject + " _var){return func.value(_var,es);}})";
        }
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<Object, Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object p1, Object p2, final ExecutionSupport es)\n" +
                "            {\n" +
                "                final " + FullJavaPaths.Function + " func = p2 instanceof java.util.List?(" + FullJavaPaths.Function + ")((java.util.List)p2).get(0):(" + FullJavaPaths.Function + ")p2;" +
                "                return CompiledSupport.toPureCollection(p1).select(new DefendedPredicate<Object>(){PureFunction1<Object,Boolean> funcC=(PureFunction1<Object,Boolean>)CoreGen.getSharedPureFunction(func, es); public boolean accept(final Object _var){return funcC.value(_var, es);}});\n" +
                "            }\n" +
                "        }";
    }
}
