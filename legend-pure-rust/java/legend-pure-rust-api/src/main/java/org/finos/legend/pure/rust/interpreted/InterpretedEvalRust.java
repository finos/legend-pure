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

package org.finos.legend.pure.rust.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.RustEvaluator;
import org.finos.legend.pure.rust.RustEvaluators;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;
import java.util.function.Supplier;

/**
 * The interpreted engine's body of a transport's {@code evalRust} native,
 * {@code <V|m>(f:Function<{->V[m]}>[1]):V[m]}: the zero-argument lambda
 * crosses the boundary as a graph node and the transport's evaluator runs it.
 * One evaluator per engine, bound to the engine's current runtime
 * ({@link RustEvaluators#latest}) and created on the first call, never in the
 * constructor: {@code InterpretedExtensionLoader} drops an extension whose
 * construction throws.
 */
public abstract class InterpretedEvalRust extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final RustEvaluators<PureRuntime, RustEvaluator> evaluators;

    /**
     * @param functionExecution the engine
     * @param create            a new evaluator of the transport, not bound yet
     */
    protected InterpretedEvalRust(FunctionExecutionInterpreted functionExecution, Supplier<? extends RustEvaluator> create)
    {
        this.functionExecution = functionExecution;
        this.evaluators = RustEvaluators.latest(runtime -> bound(create.get(), runtime));
    }

    private static RustEvaluator bound(RustEvaluator evaluator, PureRuntime runtime)
    {
        evaluator.init(runtime, new Message(""));
        return evaluator;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance lambda = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        return this.evaluators.call(this.functionExecution.getPureRuntime(), evaluator -> evaluator.start(lambda, Lists.immutable.empty()));
    }
}
