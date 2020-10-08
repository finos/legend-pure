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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.collection;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.lang.Compare;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Comparator;
import java.util.Stack;

public class Sort extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public Sort(FunctionExecutionInterpreted functionExecution)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> collection = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance key = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance comparison = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        Comparator<CoreInstance> comparator = getComparator(key, resolvedTypeParameters, resolvedMultiplicityParameters, comparison, getParentOrEmptyVariableContext(variableContext), functionExpressionToUseInStack, profiler, processorSupport, instantiationContext, executionSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(collection.toSortedList(comparator), ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }

    private Comparator<CoreInstance> getComparator(CoreInstance key, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance comparison, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, ProcessorSupport processorSupport, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if ((key == null) && (comparison == null))
        {
            return getDefaultComparator(processorSupport);
        }
        else if (key == null)
        {
            return getComparatorWithComparison(comparison, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, processorSupport, instantiationContext, executionSupport);
        }
        else if (comparison == null)
        {
            return getComparatorWithKey(key, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, processorSupport, instantiationContext, executionSupport);
        }
        else
        {
            return getComparatorWithKeyAndComparison(key, resolvedTypeParameters, resolvedMultiplicityParameters, comparison, variableContext, functionExpressionToUseInStack, profiler, processorSupport, instantiationContext, executionSupport);
        }
    }

    private Comparator<CoreInstance> getDefaultComparator(final ProcessorSupport processorSupport)
    {
        return new Comparator<CoreInstance>()
        {
            @Override
            public int compare(CoreInstance instance1, CoreInstance instance2)
            {
                return Compare.compare(instance1, instance2, processorSupport);
            }
        };
    }

    private Comparator<CoreInstance> getComparatorWithComparison(final CoreInstance comparison, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final ProcessorSupport processorSupport, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport)
    {
        return new Comparator<CoreInstance>()
        {
            @Override
            public int compare(CoreInstance left, CoreInstance right)
            {
                if (left == right)
                {
                    return 0;
                }

                return Integer.parseInt(Instance.getValueForMetaPropertyToOneResolved(Sort.this.functionExecution.executeLambdaFromNative(comparison, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(left, true, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(right, true, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport).getName());
            }
        };
    }

    private Comparator<CoreInstance> getComparatorWithKey(final CoreInstance key, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final ProcessorSupport processorSupport, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport)
    {
        final Function<CoreInstance, CoreInstance> keyFunction = new Function<CoreInstance, CoreInstance>()
        {
            @Override
            public CoreInstance valueOf(CoreInstance instance)
            {
                return Instance.getValueForMetaPropertyToOneResolved(Sort.this.functionExecution.executeLambdaFromNative(key, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport);
            }
        };

        final MutableMap<CoreInstance, CoreInstance> keyMap = UnifiedMap.newMap();

        return new Comparator<CoreInstance>()
        {
            @Override
            public int compare(CoreInstance left, CoreInstance right)
            {
                if (left == right)
                {
                    return 0;
                }

                CoreInstance leftKey = keyMap.getIfAbsentPutWithKey(left, keyFunction);
                CoreInstance rightKey = keyMap.getIfAbsentPutWithKey(right, keyFunction);
                return (leftKey == rightKey) ? 0 : Compare.compare(leftKey, rightKey, processorSupport);
            }
        };
    }

    private Comparator<CoreInstance> getComparatorWithKeyAndComparison(final CoreInstance key, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final CoreInstance comparison, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final ProcessorSupport processorSupport, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport)
    {
        final Function<CoreInstance, CoreInstance> keyFunction = new Function<CoreInstance, CoreInstance>()
        {
            @Override
            public CoreInstance valueOf(CoreInstance instance)
            {
                return Instance.getValueForMetaPropertyToOneResolved(Sort.this.functionExecution.executeLambdaFromNative(key, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport);
            }
        };

        final MutableMap<CoreInstance, CoreInstance> keyMap = UnifiedMap.newMap();

        return new Comparator<CoreInstance>()
        {
            @Override
            public int compare(CoreInstance left, CoreInstance right)
            {
                if (left == right)
                {
                    return 0;
                }

                CoreInstance leftKey = keyMap.getIfAbsentPutWithKey(left, keyFunction);
                CoreInstance rightKey = keyMap.getIfAbsentPutWithKey(right, keyFunction);
                return (leftKey == rightKey) ? 0 : Integer.parseInt(Instance.getValueForMetaPropertyToOneResolved(Sort.this.functionExecution.executeLambdaFromNative(comparison, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(leftKey, true, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(rightKey, true, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport), M3Properties.values, processorSupport).getName());
            }
        };
    }
}
