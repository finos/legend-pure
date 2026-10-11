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

package org.finos.legend.pure.rust.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.PureRustException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.extension.EvalRustExecutor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The compiled engine's {@code evalRust}: the function value (a compiled
 * lambda, which is also its graph node) crosses the boundary as a
 * {@link CoreInstance} and the results come back as compiled values.
 * One evaluator per {@link CompiledExecutionSupport}, created on first use.
 * Discovered through {@code META-INF/services}.
 */
public final class CompiledEvalRust implements EvalRustExecutor
{
    private final Map<ExecutionSupport, PureRustEvaluator> evaluators = new IdentityHashMap<>();

    @Override
    public Object evaluate(ExecutionSupport executionSupport, Function<?> function)
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
        MutableList<Object> values = Lists.mutable.ofInitialCapacity(results.length);
        for (CoreInstance result : results)
        {
            values.add((result instanceof ValCoreInstance) ? ((ValCoreInstance<?>) result).getValue() : result);
        }
        return values;
    }

    private synchronized PureRustEvaluator evaluator(ExecutionSupport executionSupport)
    {
        PureRustEvaluator evaluator = this.evaluators.get(executionSupport);
        if (evaluator == null)
        {
            evaluator = PureRustEvaluator.over(new CompiledHost(((CompiledExecutionSupport) executionSupport).getProcessorSupport()));
            this.evaluators.put(executionSupport, evaluator);
        }
        return evaluator;
    }

    private synchronized void discard(ExecutionSupport executionSupport, PureRustEvaluator poisoned)
    {
        if (this.evaluators.get(executionSupport) == poisoned)
        {
            this.evaluators.remove(executionSupport);
        }
        poisoned.close();
    }
}
