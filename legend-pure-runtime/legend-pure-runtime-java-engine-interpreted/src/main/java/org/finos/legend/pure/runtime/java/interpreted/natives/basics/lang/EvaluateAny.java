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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.lang.Evaluate;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class EvaluateAny extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public EvaluateAny(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        SourceInformation sourceInformation = functionExpressionToUseInStack.getSourceInformation();

        CoreInstance functionToApplyTo = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        FunctionType fType = (FunctionType)processorSupport.function_getFunctionType(functionToApplyTo);

        ListIterable<? extends VariableExpression> parameters = fType._parameters().toList();

        ListIterable<? extends CoreInstance> parameterValueLists = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);
        int parameterValuesCount = parameterValueLists.size();
        if (parameters.size() != parameterValuesCount)
        {
            CoreInstance functionName = Instance.getValueForMetaPropertyToOneResolved(functionToApplyTo, M3Properties.functionName, processorSupport);
            int expected = parameters.size();
            String message = "Expected " + expected + " parameter " + ((expected == 1) ? "value" : "values") + " for function " + ((functionName == null) ? "LAMBDA" : functionName.getName()) + ", got " + parameterValuesCount;
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), message);
        }

        MutableList<ValueSpecification> wrappedParameterValues = FastList.newList(parameterValuesCount);
        for (int i = 0; i < parameterValuesCount; i++)
        {
            CoreInstance parameterValueList = parameterValueLists.get(i);
            ListIterable<? extends CoreInstance> parameterValues = Instance.getValueForMetaPropertyToManyResolved(parameterValueList, M3Properties.values, processorSupport);
            CoreInstance wrappedParameterValue = ValueSpecificationBootstrap.wrapValueSpecification(parameterValues, true, processorSupport);
            Evaluate.validateValueToSignature(wrappedParameterValue, parameters.get(i), sourceInformation, processorSupport);
            wrappedParameterValues.add((ValueSpecification)wrappedParameterValue);
        }

        //---------------------------
        // Type Parameter management
        //---------------------------
        TypeInference.mapSpecToInstance(parameters, wrappedParameterValues, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);

        // Call ------------------
        CoreInstance result =  Instance.instanceOf(functionToApplyTo, M3Paths.LambdaFunction, processorSupport) ?
                this.functionExecution.executeLambda(LambdaFunctionCoreInstanceWrapper.toLambdaFunction(functionToApplyTo), wrappedParameterValues, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), functionExpressionToUseInStack, profiler, instantiationContext, executionSupport) :
                this.functionExecution.executeFunctionExecuteParams(FunctionCoreInstanceWrapper.toFunction(functionToApplyTo), wrappedParameterValues, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);

        resolvedTypeParameters.pop();
        resolvedMultiplicityParameters.pop();

        return result;
    }



}
