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

package org.finos.legend.pure.rust.jni.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.PureRustException;
import org.finos.legend.pure.rust.compiled.CompiledHost;
import org.finos.legend.pure.rust.compiled.CompiledObjects;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The compiled engine's {@code evalRust}: the function value (a compiled
 * lambda, which is also its graph node) crosses the boundary as a
 * {@link CoreInstance} and the results come back as compiled values: plain
 * Java values for primitives and maps, generated-class instances for Rust
 * objects ({@link CompiledObjects}).
 * One evaluator per {@link CompiledExecutionSupport}, created on first use.
 * The generated code of {@link EvalRust} calls {@link #evaluate}.
 */
public final class CompiledEvalRust
{
    private static final Map<ExecutionSupport, PureRustEvaluator> evaluators = new IdentityHashMap<>();

    private CompiledEvalRust()
    {
    }

    /**
     * The run-time entry the generated code calls.
     *
     * @param executionSupport the execution support of the calling function
     * @param function         the function value to evaluate, a compiled lambda or a function element
     * @return the result values, as compiled values, in a collection
     */
    public static Object evaluate(ExecutionSupport executionSupport, Function<?> function)
    {
        PureRustEvaluator evaluator = evaluator(executionSupport);
        CoreInstance[] results;
        try
        {
            results = evaluator.evaluate(function, new CoreInstance[0][]);
        }
        catch (PureRustEvaluationException e)
        {
            if (e.getCause() instanceof RuntimeException)
            {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
        catch (PureRustException e)
        {
            discard(executionSupport, evaluator);
            throw e;
        }
        CompiledObjects objects = new CompiledObjects((CompiledExecutionSupport) executionSupport);
        try
        {
            MutableList<Object> values = Lists.mutable.ofInitialCapacity(results.length);
            for (CoreInstance result : results)
            {
                values.add(objects.toCompiled(result));
            }
            return values;
        }
        catch (PureRustEvaluationException e)
        {
            throw new PureExecutionException(e.getMessage(), e);
        }
        finally
        {
            objects.release();
        }
    }

    private static synchronized PureRustEvaluator evaluator(ExecutionSupport executionSupport)
    {
        PureRustEvaluator evaluator = evaluators.get(executionSupport);
        if (evaluator == null)
        {
            evaluator = PureRustEvaluator.over(new CompiledHost(((CompiledExecutionSupport) executionSupport).getProcessorSupport()));
            evaluators.put(executionSupport, evaluator);
        }
        return evaluator;
    }

    private static synchronized void discard(ExecutionSupport executionSupport, PureRustEvaluator poisoned)
    {
        if (evaluators.get(executionSupport) == poisoned)
        {
            evaluators.remove(executionSupport);
        }
        poisoned.close();
    }
}
