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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.compiled.CompiledEvaluators;

/**
 * The compiled engine's {@code evalRust} over {@link PureRustEvaluator}
 * ({@link CompiledEvaluators}). The generated code of {@link EvalRust} calls
 * {@link #evaluate}.
 */
public final class CompiledEvalRust
{
    private static final CompiledEvaluators evaluators = new CompiledEvaluators(PureRustEvaluator::over);

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
        return evaluators.evaluate(executionSupport, function);
    }
}
