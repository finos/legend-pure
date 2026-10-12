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

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.rust.PureRustEvaluator;
import org.finos.legend.pure.rust.interpreted.InterpretedEvalRust;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

/**
 * Interpreted implementation of
 * {@code meta::pure::rust::jni::evalRust<V|m>(f:Function<{->V[m]}>[1]):V[m]}
 * over {@link PureRustEvaluator}: the body of the {@code Rust_JNI} PCT adapter.
 */
public class EvalRust extends InterpretedEvalRust
{
    public EvalRust(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, PureRustEvaluator::new);
    }
}
