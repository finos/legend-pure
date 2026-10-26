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

import org.finos.legend.pure.rust.compiled.CompiledEvalRustNative;

/**
 * The compiled engine's code generation for {@code meta::pure::rust::ffm::evalRustFfm}: a call compiles to
 * {@link CompiledEvalRustFfm#evaluate}. Registered through {@link RustFfmCompiledExtension}.
 */
public class EvalRustFfm extends CompiledEvalRustNative
{
    public static final String SIGNATURE = "evalRustFfm_Function_1__V_m_";

    public EvalRustFfm()
    {
        super(SIGNATURE, CompiledEvalRustFfm.class);
    }
}
