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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.conjunctions;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.Executor;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativePredicate;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Or extends NativePredicate
{
    private final FunctionExecutionInterpreted functionExecution;

    public Or(ModelRepository repository, FunctionExecutionInterpreted functionExecution)
    {
        super(repository);
        this.functionExecution = functionExecution;
    }

    @Override
    public boolean deferParameterExecution()
    {
        return true;
    }

    @Override
    protected boolean executeBoolean(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ListIterable<? extends CoreInstance> params, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport) throws PureExecutionException
    {
        VariableContext evalVarContext = getParentOrEmptyVariableContext(variableContext);
        for (CoreInstance param : params)
        {
            Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(param, functionExpressionToUseInStack, processorSupport, this.functionExecution);
            CoreInstance evaluatedParam = executor.execute(param, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, evalVarContext, profiler, instantiationContext, executionSupport, this.functionExecution, processorSupport);
            if (PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(evaluatedParam, M3Properties.values, processorSupport)))
            {
                return true;
            }
        }
        return false;
    }
}
