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

package org.finos.legend.pure.runtime.java.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.LambdaWithContext;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public abstract class NativeFunction
{
    public abstract CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException;

    public boolean deferParameterExecution()
    {
        return false;
    }

    /**
     * Get the parent of variableContext, if it has one.  Otherwise,
     * return a new empty variable context.  This should never return
     * null.
     *
     * @param variableContext variable context
     * @return parent of variableContext or empty variable context
     */
    protected VariableContext getParentOrEmptyVariableContext(VariableContext variableContext)
    {
        if (variableContext == null)
        {
            return VariableContext.newVariableContext();
        }
        else
        {
            VariableContext parent = variableContext.getParent();
            return (parent == null) ? VariableContext.newVariableContext() : parent;
        }
    }

    protected VariableContext getParentOrEmptyVariableContextForLambda(VariableContext variableContext, CoreInstance lambdaFunction)
    {
        VariableContext context = getParentOrEmptyVariableContext(variableContext);
        if (lambdaFunction instanceof LambdaWithContext)
        {
            context = ((LambdaWithContext)lambdaFunction).getVariableContext();
        }
        return context;
    }
}
