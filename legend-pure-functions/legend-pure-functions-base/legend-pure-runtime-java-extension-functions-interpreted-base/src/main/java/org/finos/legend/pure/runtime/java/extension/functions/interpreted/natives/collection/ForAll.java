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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativePredicate;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class ForAll extends NativePredicate
{
    private final FunctionExecutionInterpreted functionExecution;

    public ForAll(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
        this.functionExecution = functionExecution;
    }

    @Override
    protected boolean executeBoolean(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ListIterable<? extends CoreInstance> params, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> elements = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance predicate = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

        for (CoreInstance coreInstance : elements)
        {
            CoreInstance subFunctionResult = this.functionExecution.executeLambdaFromNative(predicate, FastList.newListWith(ValueSpecificationBootstrap.wrapValueSpecification(coreInstance, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            if (!PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(subFunctionResult, M3Properties.values, processorSupport)))
            {
                return false;
            }
        }
        return true;
    }
}
