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
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.RustEngineHost;
import org.finos.legend.pure.rust.RustEvaluator;
import org.finos.legend.pure.rust.RustEvaluators;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

/**
 * The run-time side of a transport's compiled {@code evalRust}, which the
 * static entry its generated code calls delegates to. One evaluator per
 * {@link CompiledExecutionSupport}, over a {@link CompiledHost} of its graph,
 * created on the first call ({@link RustEvaluators#perScope}: the support is
 * held weakly, the evaluator of a support the garbage collector reclaimed is
 * closed on a later call, and a panic discards the evaluator). The function
 * value (a compiled lambda, which is also its graph node, or a function
 * element) crosses as a {@link CoreInstance}; the results come back as
 * compiled values: plain Java values for primitives and maps,
 * generated-class instances for Rust objects ({@link CompiledObjects}). A
 * typed failure is rethrown as the Pure exception it carries.
 */
public final class CompiledEvaluators
{
    private static final CoreInstance[][] NO_ARGUMENTS = new CoreInstance[0][];

    private final RustEvaluators<ExecutionSupport, RustEvaluator> evaluators;

    /**
     * @param over a new evaluator of the transport bound to a host
     */
    public CompiledEvaluators(java.util.function.Function<? super RustEngineHost, ? extends RustEvaluator> over)
    {
        this.evaluators = RustEvaluators.perScope(executionSupport -> over.apply(new CompiledHost(((CompiledExecutionSupport) executionSupport).getProcessorSupport())));
    }

    /**
     * Run a function value with no arguments.
     *
     * @param executionSupport the execution support of the calling function
     * @param function         the function value to evaluate, a compiled lambda or a function element
     * @return the result values, as compiled values, in a collection
     */
    public Object evaluate(ExecutionSupport executionSupport, Function<?> function)
    {
        CoreInstance[] results;
        try
        {
            results = this.evaluators.call(executionSupport, evaluator -> evaluator.evaluate(function, NO_ARGUMENTS));
        }
        catch (PureRustEvaluationException e)
        {
            if (e.getCause() instanceof RuntimeException)
            {
                throw (RuntimeException) e.getCause();
            }
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
}
