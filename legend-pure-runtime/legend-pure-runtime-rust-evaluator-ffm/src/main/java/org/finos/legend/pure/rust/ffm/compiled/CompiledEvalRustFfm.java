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

package org.finos.legend.pure.rust.ffm.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.PureRustException;
import org.finos.legend.pure.rust.PureRustFfmEvaluator;
import org.finos.legend.pure.rust.compiled.CompiledHost;
import org.finos.legend.pure.rust.compiled.CompiledObjects;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.extension.EvalRustFfmExecutor;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CompiledEvalRustFfm implements EvalRustFfmExecutor
{
    private final Map<ExecutionSupport, PureRustFfmEvaluator> evaluators = new IdentityHashMap<>();

    @Override
    public Object evaluate(ExecutionSupport executionSupport, Function<?> function)
    {
        PureRustFfmEvaluator evaluator = evaluator(executionSupport);
        CoreInstance[] results;
        try
        {
            results = evaluator.evaluate(function, new CoreInstance[0][]);
        }
        catch (PureRustEvaluationException e)
        {
            if (e.getCause() instanceof RuntimeException cause)
            {
                throw cause;
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

    private synchronized PureRustFfmEvaluator evaluator(ExecutionSupport executionSupport)
    {
        return this.evaluators.computeIfAbsent(executionSupport,
                es -> PureRustFfmEvaluator.over(new CompiledHost(((CompiledExecutionSupport) es).getProcessorSupport())));
    }

    private synchronized void discard(ExecutionSupport executionSupport, PureRustFfmEvaluator poisoned)
    {
        if (this.evaluators.get(executionSupport) == poisoned)
        {
            this.evaluators.remove(executionSupport);
        }
        poisoned.close();
    }
}
