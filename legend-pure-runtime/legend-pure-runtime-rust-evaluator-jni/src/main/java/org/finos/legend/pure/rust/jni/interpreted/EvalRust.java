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

package org.finos.legend.pure.rust.jni.interpreted;

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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.PureRustException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

/**
 * Interpreted implementation of
 * {@code meta::pure::functions::lang::evalRust<V|m>(f:Function<{->V[m]}>[1]):V[m]}:
 * the zero-argument lambda crosses the JNI boundary as a graph node and the
 * Rust evaluator runs it. This is the body of the {@code Rust_JNI} PCT adapter.
 *
 * <p>The {@link PureRustEvaluator} is created on the first call, not in the
 * constructor: {@code InterpretedExtensionLoader} drops an extension whose
 * construction throws. One evaluator per engine, bound to the engine's runtime.
 */
public class EvalRust extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private PureRustEvaluator evaluator;

    public EvalRust(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance lambda = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        PureRustEvaluator current = evaluator();
        try
        {
            return current.start(lambda, Lists.immutable.empty());
        }
        catch (PureRustEvaluationException e)
        {
            throw e;
        }
        catch (PureRustException e)
        {
            discard(current);
            throw e;
        }
    }

    /**
     * A failure that is not a typed evaluation result (a Rust panic) leaves the evaluator poisoned: it is closed and
     * the next call creates a fresh one.
     */
    private synchronized void discard(PureRustEvaluator poisoned)
    {
        if (this.evaluator == poisoned)
        {
            this.evaluator = null;
        }
        poisoned.close();
    }

    /**
     * The evaluator bound to the engine's current runtime. An engine
     * re-initialised with another runtime gets a new evaluator and the old
     * one is closed, so at most one native context lives per engine.
     */
    private synchronized PureRustEvaluator evaluator()
    {
        PureRuntime runtime = this.functionExecution.getPureRuntime();
        if ((this.evaluator != null) && (this.evaluator.getRuntime() != runtime))
        {
            this.evaluator.close();
            this.evaluator = null;
        }
        if (this.evaluator == null)
        {
            PureRustEvaluator created = new PureRustEvaluator();
            created.init(runtime, new Message(""));
            this.evaluator = created;
        }
        return this.evaluator;
    }
}
