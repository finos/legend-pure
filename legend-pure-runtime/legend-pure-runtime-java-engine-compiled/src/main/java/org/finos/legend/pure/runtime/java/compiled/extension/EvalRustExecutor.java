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

package org.finos.legend.pure.runtime.java.compiled.extension;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

/**
 * Runs a function value in the Rust evaluator on behalf of the compiled
 * engine's {@code evalRust} native. Discovered through
 * {@code META-INF/services}; the implementation lives in
 * {@code legend-pure-runtime-rust-evaluator}, which the engine does not
 * depend on.
 */
public interface EvalRustExecutor
{
    /**
     * Evaluate a zero-argument function.
     *
     * @param executionSupport the compiled engine's execution support
     * @param function         the function value, a compiled lambda or a function element
     * @return the result values, as compiled values, in a collection
     */
    Object evaluate(ExecutionSupport executionSupport, Function<?> function);
}
