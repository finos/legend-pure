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
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Cast extends NativeFunction
{
    private ModelRepository repository;

    public Cast(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance valuesParam = params.get(0);
        CoreInstance sourceGenericType = valuesParam.getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance targetGenericType = params.get(1).getValueForMetaPropertyToOne(M3Properties.genericType);
        targetGenericType = makeGenericTypeAsConcreteAsPossible(targetGenericType, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);

        CoreInstance inst = this.repository.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), processorSupport.getClassifier(valuesParam));
        Instance.addValueToProperty(inst, M3Properties.genericType, targetGenericType, processorSupport);
        Instance.addValueToProperty(inst, M3Properties.multiplicity, Instance.getValueForMetaPropertyToOneResolved(valuesParam, M3Properties.multiplicity, processorSupport), processorSupport);
        if (GenericTypeMatch.genericTypeMatches(targetGenericType, sourceGenericType, true, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            CoreInstance sourceRawType = Instance.getValueForMetaPropertyToOneResolved(sourceGenericType, M3Properties.rawType, processorSupport);
            CoreInstance targetRawType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport);
            // If up-casting unit type to measure type, keep unit type.
            if (sourceRawType instanceof Unit && targetRawType instanceof Measure)
            {
                Instance.setValueForProperty(inst, M3Properties.genericType, sourceGenericType, processorSupport);
            }
            // Up cast (e.g., List<Integer> to Any) - no further type checking required
            Instance.setValuesForProperty(inst, M3Properties.values, valuesParam.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
        }
        else
        {
            // Down cast (e.g., Number to Integer) - must check types of individual values
            ListIterable<? extends CoreInstance> values = valuesParam.getValueForMetaPropertyToMany(M3Properties.values);
            for (CoreInstance val : values)
            {
                CoreInstance valGenericType = Instance.extractGenericTypeFromInstance(val, processorSupport);
                if (!GenericTypeMatch.genericTypeMatches(targetGenericType, valGenericType, true, ParameterMatchBehavior.MATCH_ANYTHING, ParameterMatchBehavior.MATCH_ANYTHING, processorSupport))
                {
                    throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Cast exception: " + GenericType.print(valGenericType, processorSupport) + " cannot be cast to " + GenericType.print(targetGenericType, processorSupport));
                }
            }
            Instance.setValuesForProperty(inst, M3Properties.values, valuesParam.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
        }
        return inst;
    }

    private CoreInstance makeGenericTypeAsConcreteAsPossible(CoreInstance genericType, Stack<MutableMap<String, CoreInstance>> resolvedTypeParametersStack, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParametersStack, ProcessorSupport processorSupport)
    {
        if (GenericType.isGenericTypeFullyConcrete(genericType, processorSupport))
        {
            return genericType;
        }
        CoreInstance result = genericType;
        for (int i = resolvedTypeParametersStack.size() - 2; i >= 0; i--)
        {
            MutableMap<String, CoreInstance> resolvedTypeParameters = resolvedTypeParametersStack.elementAt(i);
            MutableMap<String, CoreInstance> resolvedMultiplicityParameters = resolvedMultiplicityParametersStack.elementAt(i);
            if (resolvedTypeParameters.notEmpty() || resolvedMultiplicityParameters.notEmpty())
            {
                result = GenericType.makeTypeArgumentAsConcreteAsPossible(result, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);
                if (GenericType.isGenericTypeFullyConcrete(result, processorSupport))
                {
                    return result;
                }
            }
        }
        return result;
    }
}
