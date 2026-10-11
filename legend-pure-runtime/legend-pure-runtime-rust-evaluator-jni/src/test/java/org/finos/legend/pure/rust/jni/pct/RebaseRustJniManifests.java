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

package org.finos.legend.pure.rust.jni.pct;

import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.rust.pct.PctManifestRebase;
import org.junit.Test;

import java.io.IOException;

/**
 * Regenerates the four {@code Rust_JNI} manifests ({@link PctManifestRebase}).
 * Not part of the default surefire run (the class name matches no include
 * pattern); invoke with
 * <pre>
 * mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator-jni -Dtest=RebaseRustJniManifests -DfailIfNoTests=false
 * </pre>
 * after {@code cargo build --workspace}, then review the diff and run the
 * module's {@code verify}.
 */
public class RebaseRustJniManifests
{
    private static final Adapter adapter = PlatformCodeRepositoryProvider.rustJniAdapter;

    @Test
    public void rebaseEssential() throws IOException
    {
        PctManifestRebase.interpreted(adapter, PlatformCodeRepositoryProvider.essentialFunctions, "platform/pure/essential/pct_essential_interpreted_rust_jni.json");
    }

    @Test
    public void rebaseGrammar() throws IOException
    {
        PctManifestRebase.interpreted(adapter, PlatformCodeRepositoryProvider.grammarFunctions, "platform/pure/grammar/functions/pct_grammar_interpreted_rust_jni.json");
    }

    @Test
    public void rebaseEssentialCompiled() throws Throwable
    {
        PctManifestRebase.compiled(adapter, PlatformCodeRepositoryProvider.essentialFunctions, "platform/pure/essential/pct_essential_compiled_rust_jni.json");
    }

    @Test
    public void rebaseGrammarCompiled() throws Throwable
    {
        PctManifestRebase.compiled(adapter, PlatformCodeRepositoryProvider.grammarFunctions, "platform/pure/grammar/functions/pct_grammar_compiled_rust_jni.json");
    }
}
