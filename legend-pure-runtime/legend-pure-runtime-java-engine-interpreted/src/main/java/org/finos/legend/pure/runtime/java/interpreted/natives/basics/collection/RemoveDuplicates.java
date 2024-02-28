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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.EqualityUtilities;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class RemoveDuplicates extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public RemoveDuplicates(FunctionExecutionInterpreted functionExecution)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> collection = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        int size = collection.size();
        if (size <= 1)
        {
            return params.get(0);
        }

        CoreInstance keyFn = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance eqlFn = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        MutableList<CoreInstance> results = FastList.newList(size);
        if ((keyFn == null) && (eqlFn == null))
        {
            MutableSet<CoreInstance> instances = EqualityUtilities.newCoreInstanceSet(processorSupport, size);
            for (CoreInstance instance : collection)
            {
                if (instances.add(instance))
                {
                    results.add(instance);
                }
            }
        }
        else if (keyFn == null)
        {
            for (CoreInstance instance : collection)
            {
                if (!contains(results, instance, eqlFn, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack,  ValueSpecification.isExecutable(params.get(0), processorSupport), profiler, processorSupport, instantiationContext, executionSupport))
                {
                    results.add(instance);
                }
            }
        }
        else if (eqlFn == null)
        {
            MutableSet<CoreInstance> keys = EqualityUtilities.newCoreInstanceSet(processorSupport, size);
            for (CoreInstance instance : collection)
            {
                CoreInstance key = Instance.getValueForMetaPropertyToOneResolved(this.functionExecution.executeLambdaFromNative(keyFn, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(instance, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport);
                if (keys.add(key))
                {
                    results.add(instance);
                }
            }
        }
        else
        {
            MutableList<CoreInstance> keys = FastList.newList(size);
            for (CoreInstance instance : collection)
            {
                CoreInstance key = Instance.getValueForMetaPropertyToOneResolved(this.functionExecution.executeLambdaFromNative(keyFn, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(instance, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport);
                if (!contains(keys, key, eqlFn, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack,  ValueSpecification.isExecutable(params.get(0), processorSupport), profiler, processorSupport, instantiationContext, executionSupport))
                {
                    keys.add(key);
                    results.add(instance);
                }
            }
        }
        return ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(results, Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport), ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }

    private boolean contains(ListIterable<CoreInstance> list, CoreInstance instance, CoreInstance eqlFn, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, boolean executable, Profiler profiler, ProcessorSupport processorSupport, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        for (CoreInstance item : list)
        {
            CoreInstance valueSpec = this.functionExecution.executeLambdaFromNative(eqlFn, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(item, executable, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(instance, executable, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            if (PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(valueSpec, M3Properties.values, processorSupport)))
            {
                return true;
            }
        }
        return false;
    }
}
