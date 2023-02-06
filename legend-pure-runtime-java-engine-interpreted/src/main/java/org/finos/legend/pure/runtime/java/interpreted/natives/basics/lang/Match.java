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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Match extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public Match(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance firstParam = params.get(0);
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(firstParam, M3Properties.values, processorSupport);
        int valueCount = values.size();
        CoreInstance valueType = valueCount == 1 ?
                Measure.isUnitOrMeasureInstance(values.getFirst(), processorSupport)?
                        Instance.getValueForMetaPropertyToOneResolved(values.getFirst(), M3Properties.genericType, processorSupport) : Instance.extractGenericTypeFromInstance(values.getFirst(), processorSupport)
                : Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport);

        for (CoreInstance func : Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport))
        {
            CoreInstance functionType = processorSupport.function_getFunctionType(func);
            CoreInstance parameter = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport).get(0);
            CoreInstance parameterType = Instance.getValueForMetaPropertyToManyResolved(parameter, M3Properties.genericType, processorSupport).get(0);
            CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToManyResolved(parameter, M3Properties.multiplicity, processorSupport).get(0);
            if (GenericType.subTypeOf(valueType, parameterType, processorSupport) && Multiplicity.isValid(parameterMultiplicity, valueCount))
            {
                return this.functionExecution.executeFunction(false, LambdaFunctionCoreInstanceWrapper.toLambdaFunction(func),
                        params.size() == 3 ? Lists.mutable.with(firstParam, params.get(2)) : Lists.mutable.with(firstParam),
                        resolvedTypeParameters, resolvedMultiplicityParameters, this.getParentOrEmptyVariableContextForLambda(variableContext, func), functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            }
        }

        throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Match failure: " + (valueCount == 1 ? values.getFirst() : values.makeString("[", ", ", "]")));
    }
}

