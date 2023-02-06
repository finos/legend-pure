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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpressionCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

class FunctionExpressionExecutor implements Executor
{
    static final FunctionExpressionExecutor INSTANCE = new FunctionExpressionExecutor();

    private FunctionExpressionExecutor()
    {
    }

    @Override
    public CoreInstance execute(CoreInstance instance, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, VariableContext variableContext, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, FunctionExecutionInterpreted functionExecutionInterpreted, ProcessorSupport processorSupport) throws PureExecutionException
    {
        profiler.startExecutingFunctionExpression(instance, functionExpressionToUseInStack);
        FunctionExpression functionExpression = FunctionExpressionCoreInstanceWrapper.toFunctionExpression(instance);
        ListIterable<? extends CoreInstance> params = (ListIterable<? extends CoreInstance>)functionExpression._parametersValues();
        Function function = FunctionCoreInstanceWrapper.toFunction(functionExpression._funcCoreInstance());

        MutableMap<String, CoreInstance> localResolvedTypeParameters = UnifiedMap.newMap();
        MutableMap<String, CoreInstance> localResolvedMultiplicityParameters = UnifiedMap.newMap();
        this.resolveLocalTypeAndMultiplicityParams(functionExpression, instance, processorSupport, params, function, localResolvedTypeParameters, localResolvedMultiplicityParameters);
        boolean deferExecution = Instance.instanceOf(function, M3Paths.NativeFunction, processorSupport) && functionExecutionInterpreted.getNativeFunction(function.getName()) != null && functionExecutionInterpreted.getNativeFunction(function.getName()).deferParameterExecution();

        ListIterable<? extends CoreInstance> parameters;
        if (deferExecution || params.isEmpty())
        {
            parameters = params;
        }
        else
        {
            MutableList<CoreInstance> newParams = FastList.newList(params.size());
            for (CoreInstance instance1 : params)
            {
                Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(instance1, instance, processorSupport, functionExecutionInterpreted);
                newParams.add(executor.execute(instance1, resolvedTypeParameters, resolvedMultiplicityParameters, instance, variableContext, profiler, instantiationContext, executionSupport, functionExecutionInterpreted, processorSupport));
            }
            parameters = newParams;
        }

        resolvedTypeParameters.push(this.resolveTypeParamsFromParent(resolvedTypeParameters, resolvedMultiplicityParameters, localResolvedTypeParameters, functionExpressionToUseInStack, processorSupport));
        resolvedMultiplicityParameters.push(this.resolveMultiplicityParametersFromParent(resolvedMultiplicityParameters, localResolvedMultiplicityParameters, functionExpressionToUseInStack));
        CoreInstance result = functionExecutionInterpreted.executeFunction(true, function, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, instance, profiler, instantiationContext, executionSupport);

        resolvedTypeParameters.pop();
        resolvedMultiplicityParameters.pop();
        profiler.finishedExecutingFunctionExpression(instance);
        return result;
    }

    private void resolveLocalTypeAndMultiplicityParams(FunctionExpression functionExpression, CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport, ListIterable<? extends CoreInstance> params, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function, MutableMap<String, CoreInstance> localResolvedTypeParameters, MutableMap<String, CoreInstance> localResolvedMultiplicityParameters)
    {
        CoreInstance functionType = processorSupport.function_getFunctionType(function);

        if (Instance.instanceOf(function, M3Paths.QualifiedProperty, processorSupport) || "copy_T_1__String_1__KeyExpression_MANY__T_1_".equals(function.getName()) || "new_Class_1__String_1__KeyExpression_MANY__T_1_".equals(function.getName()) || "new_Class_1__String_1__T_1_".equals(function.getName()))
        {
            CoreInstance genericType = Instance.instanceOf(function, M3Paths.QualifiedProperty, processorSupport) || "copy_T_1__String_1__KeyExpression_MANY__T_1_".equals(function.getName()) ?
                    Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport) :
                    Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, M3Properties.typeArguments, processorSupport);
            CoreInstance classifier = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> new_TypeParameters = Instance.getValueForMetaPropertyToManyResolved(classifier, M3Properties.typeParameters, processorSupport);
            ListIterable<? extends CoreInstance> new_MultiplicityParameters = Instance.getValueForMetaPropertyToManyResolved(classifier, M3Properties.multiplicityParameters, processorSupport);
            ListIterable<? extends CoreInstance> new_TypeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
            ListIterable<? extends CoreInstance> new_MultiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);
            for (int i = 0, size = new_TypeParameters.size(); i < size; i++)
            {
                localResolvedTypeParameters.put(Instance.getValueForMetaPropertyToOneResolved(new_TypeParameters.get(i), M3Properties.name, processorSupport).getName(), new_TypeArguments.get(i));
            }
            for (int i = 0, size = new_MultiplicityParameters.size(); i < size; i++)
            {
                localResolvedMultiplicityParameters.put(Instance.getValueForMetaPropertyToOneResolved(new_MultiplicityParameters.get(i), M3Properties.values, processorSupport).getName(), new_MultiplicityArguments.get(i));
            }
        }
        else
        {
            ListIterable<? extends CoreInstance> typeArguments = (ListIterable<? extends CoreInstance>)functionExpression._resolvedTypeParameters();
            ListIterable<? extends CoreInstance> typeParameters = functionType.getValueForMetaPropertyToMany(M3Properties.typeParameters);
            int typeParamsSize = typeParameters.size();
            if (typeArguments.size() != typeParamsSize)
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Mismatch between type parameter count (" + typeParamsSize + ") and type argument count (" + typeArguments.size() + ")");
            }
            for (int i = 0; i < typeParamsSize; i++)
            {
                localResolvedTypeParameters.put(typeParameters.get(i).getValueForMetaPropertyToOne(M3Properties.name).getName(), typeArguments.get(i));
            }

            ListIterable<? extends CoreInstance> multiplicityArguments = (ListIterable<? extends CoreInstance>)functionExpression._resolvedMultiplicityParameters();
            ListIterable<? extends CoreInstance> multiplicityParameters = functionType.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
            int multiplicityParamsSize = multiplicityParameters.size();
            if (multiplicityArguments.size() == multiplicityParamsSize)
            {
                for (int i = 0; i < multiplicityParamsSize; i++)
                {
                    localResolvedMultiplicityParameters.put(multiplicityParameters.get(i).getValueForMetaPropertyToOne(M3Properties.values).getName(), multiplicityArguments.get(i));
                }
            }
        }
    }

    private MutableMap<String, CoreInstance> resolveTypeParamsFromParent(Stack<MutableMap<String, CoreInstance>> stackType, Stack<MutableMap<String, CoreInstance>> stackMul, MutableMap<String, CoreInstance> typeParameters, CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport)
    {
        if (typeParameters.isEmpty() || stackType.isEmpty())
        {
            return typeParameters;
        }

        MutableMap<String, CoreInstance> result = UnifiedMap.newMap(typeParameters.size());
        for (String key : typeParameters.keysView())
        {
            CoreInstance ci = typeParameters.get(key);
            int size = stackType.size() - 1;
            // Look in the stack until we find the value
            while (size >= 0 && !GenericType.isGenericTypeFullyConcrete(ci, processorSupport))
            {
                ci = GenericType.makeTypeArgumentAsConcreteAsPossible(GenericType.copyGenericType(typeParameters.get(key), false, processorSupport), stackType.get(size).asUnmodifiable(), stackMul.get(size).asUnmodifiable(), processorSupport);
                size--;
            }
            if (!GenericType.isGenericTypeFullyConcrete(ci, processorSupport))
            {
                throw new PureExecutionException((functionExpressionToUseInStack == null) ? null : functionExpressionToUseInStack.getSourceInformation(), "Can't resolve some type parameters in: " + GenericType.print(ci, processorSupport));
            }
            result.put(key, ci);
        }
        return result;
    }

    private MutableMap<String, CoreInstance> resolveMultiplicityParametersFromParent(final Stack<MutableMap<String, CoreInstance>> stack, MutableMap<String, CoreInstance> multiplicityParameters, final CoreInstance functionExpressionToUseInStack)
    {
        if (multiplicityParameters.isEmpty() || stack.isEmpty())
        {
            return multiplicityParameters;
        }

        final MutableMap<String, CoreInstance> result = UnifiedMap.newMap(multiplicityParameters.size());
        multiplicityParameters.forEachKeyValue(new Procedure2<String, CoreInstance>()
        {
            @Override
            public void value(String parameter, CoreInstance multiplicity)
            {
                if (Multiplicity.isMultiplicityConcrete(multiplicity))
                {
                    result.put(parameter, multiplicity);
                }
                else
                {
                    CoreInstance resolvedMultiplicity = resolveMultiplicityParameter(Multiplicity.getMultiplicityParameter(multiplicity), stack);
                    if (resolvedMultiplicity == null)
                    {
                        throw new PureExecutionException((functionExpressionToUseInStack == null) ? null : functionExpressionToUseInStack.getSourceInformation(), "Cannot resolve multiplicity parameter: " + Multiplicity.getMultiplicityParameter(multiplicity));
                    }
                    result.put(parameter, resolvedMultiplicity);
                }
            }
        });
        return result;
    }

    private CoreInstance resolveMultiplicityParameter(String param, Stack<MutableMap<String, CoreInstance>> stack)
    {
        for (int i = stack.size() - 1; i >= 0; i--)
        {
            MapIterable<String, CoreInstance> resolvedMultiplicityParameters = stack.elementAt(i);
            CoreInstance multiplicity = resolvedMultiplicityParameters.get(param);
            if (multiplicity != null)
            {
                if (Multiplicity.isMultiplicityConcrete(multiplicity))
                {
                    return multiplicity;
                }
                param = Multiplicity.getMultiplicityParameter(multiplicity);
            }
        }
        return null;
    }
}
