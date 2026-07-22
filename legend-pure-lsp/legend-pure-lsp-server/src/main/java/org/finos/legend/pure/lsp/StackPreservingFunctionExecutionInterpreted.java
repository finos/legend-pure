// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.util.Stack;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

/**
 * Wraps nested PureExceptions with the caller's expression stack, which the
 * default interpreter discards when rethrowing inner exceptions.
 */
class StackPreservingFunctionExecutionInterpreted extends FunctionExecutionInterpreted
{
    @Override
    public CoreInstance executeFunction(boolean limitScope,
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function,
            ListIterable<? extends CoreInstance> params,
            Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
            Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
            VariableContext varContext,
            MutableStack<CoreInstance> functionExpressionCallStack,
            Profiler profiler,
            InstantiationContext instantiationContext,
            ExecutionSupport executionSupport)
    {
        try
        {
            return super.executeFunction(limitScope, function, params, resolvedTypeParameters, resolvedMultiplicityParameters,
                    varContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
        }
        catch (PureExecutionException e)
        {
            throw e;
        }
        catch (PureException e)
        {
            if (functionExpressionCallStack.notEmpty())
            {
                throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(),
                        e.getInfo(), e, functionExpressionCallStack);
            }
            throw e;
        }
    }
}
