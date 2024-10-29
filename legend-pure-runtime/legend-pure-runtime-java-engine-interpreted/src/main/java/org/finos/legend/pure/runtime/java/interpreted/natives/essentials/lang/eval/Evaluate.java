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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.eval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Evaluate extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public Evaluate(FunctionExecutionInterpreted functionExecution)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        if (Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport), M3Paths.Nil, processorSupport))
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Evaluate can't take an instance of Nil as a function", functionExpressionCallStack);
        }

        CoreInstance functionToApplyTo = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        FunctionType fType = (FunctionType) processorSupport.function_getFunctionType(functionToApplyTo);
        ListIterable<? extends VariableExpression> parametersSignature = fType._parameters().toList();

        MutableList<ValueSpecification> funcParams = Lists.mutable.ofInitialCapacity(params.size());
        SourceInformation sourceInformation = functionExpressionCallStack.peek().getSourceInformation();
        for (int i = 1, size = params.size(); i < size; i++)
        {
            validateValueToSignature(params.get(i), parametersSignature.get(i - 1), sourceInformation, functionExpressionCallStack, processorSupport);
            funcParams.add((ValueSpecification) params.get(i));
        }

        //---------------------------
        // Type Parameter management
        //---------------------------
        TypeInference.mapSpecToInstance(parametersSignature, funcParams, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);

        CoreInstance result = Instance.instanceOf(functionToApplyTo, M3Paths.LambdaFunction, processorSupport) ?
                this.functionExecution.executeLambda(LambdaFunctionCoreInstanceWrapper.toLambdaFunction(functionToApplyTo), funcParams, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), functionExpressionCallStack, profiler, instantiationContext, executionSupport) :
                this.functionExecution.executeFunctionExecuteParams(FunctionCoreInstanceWrapper.toFunction(functionToApplyTo), funcParams, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), functionExpressionCallStack, profiler, instantiationContext, executionSupport);

        resolvedTypeParameters.pop();
        resolvedMultiplicityParameters.pop();

        return result;

    }

    public static void validateValueToSignature(CoreInstance value, CoreInstance signature, SourceInformation sourceInfo, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance signatureMultiplicity = Instance.getValueForMetaPropertyToOneResolved(signature, M3Properties.multiplicity, processorSupport);
        CoreInstance valueMultiplicity = Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.multiplicity, processorSupport);
        try
        {
            if (Multiplicity.isMultiplicityConcrete(signatureMultiplicity) && !Multiplicity.subsumes(signatureMultiplicity, valueMultiplicity))
            {
                throw new PureExecutionException(sourceInfo, "Error during dynamic function evaluation. The multiplicity " + Multiplicity.print(valueMultiplicity) + " is not compatible with the multiplicity " + Multiplicity.print(signatureMultiplicity) + " for parameter:" + signature.getValueForMetaPropertyToOne(M3Properties.name).getName(), functionExpressionCallStack);
            }
        }
        catch (RuntimeException e)
        {
            throw new PureExecutionException(sourceInfo, "Error evaluating multiplicities: " + Multiplicity.print(valueMultiplicity) + ", " + Multiplicity.print(signatureMultiplicity), e, functionExpressionCallStack);
        }

        CoreInstance signatureGenericType = Instance.getValueForMetaPropertyToOneResolved(signature, M3Properties.genericType, processorSupport);
        CoreInstance valueGenericType = Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.genericType, processorSupport);
        try
        {
            if (!GenericTypeMatch.genericTypeMatches(signatureGenericType, valueGenericType, true, ParameterMatchBehavior.MATCH_ANYTHING, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
            {
                throw new PureExecutionException(sourceInfo, "Error during dynamic function evaluation. The type " + GenericType.print(valueGenericType, processorSupport) + " is not compatible with the type " + GenericType.print(signatureGenericType, processorSupport), functionExpressionCallStack);
            }
        }
        catch (RuntimeException e)
        {
            throw new PureExecutionException(sourceInfo, "Error evaluating generic types: " + GenericType.print(valueGenericType, processorSupport) + ", " + GenericType.print(signatureGenericType, processorSupport), e, functionExpressionCallStack);
        }
    }
}
