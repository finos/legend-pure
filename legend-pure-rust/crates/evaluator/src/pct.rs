// Copyright 2024 Goldman Sachs
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

//! The platform PCT corpus on the evaluator alone, driven by the Pure
//! surveyor (`meta::pure::test::surveyor::runPCTTestsFromPath`), with no
//! host engine in the loop: the `Rust_Native` adapter. The surveyor is a
//! platform function, so the run needs the platform's metadata loaded;
//! over the bootstrap alone it stops at the surveyor's own definition.

use smol_str::SmolStr;

use crate::error::PureException;
use crate::eval::Evaluator;
use crate::hooks::EvalHooks;
use crate::metadata;
use crate::value::Value;

/// The surveyor entry: `runPCTTestsFromPath(path, filter, manifestPath)`.
pub const SURVEYOR: &str =
    "meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_";

/// The adapter the `Rust_Native` manifests name: `$f->eval()` on the
/// evaluator itself.
pub const ADAPTER: &str =
    "meta::pure::rust::standalone::testAdapterForRustNativeExecution_Function_1__X_o_";

/// One report scope of the corpus, as the Java harness defines it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Scope {
    /// The report's module name (`essential`, `grammar`).
    pub module: &'static str,
    /// The package the surveyor walks for `<<PCT.test>>` functions.
    pub package: &'static str,
    /// The source path filter.
    pub file_path: &'static str,
    /// The manifest, as a repository path under this crate's `pct/`
    /// directory.
    pub manifest_path: &'static str,
}

/// `meta::pure::functions` under `/platform/pure/essential/`.
pub const ESSENTIAL: Scope = Scope {
    module: "essential",
    package: "meta::pure::functions",
    file_path: "/platform/pure/essential/",
    manifest_path: "platform/pure/essential/pct_essential_rust_native.json",
};

/// `meta::pure::functions` under `/platform/pure/grammar/functions/`.
pub const GRAMMAR: Scope = Scope {
    module: "grammar",
    package: "meta::pure::functions",
    file_path: "/platform/pure/grammar/functions/",
    manifest_path: "platform/pure/grammar/functions/pct_grammar_rust_native.json",
};

/// Both scopes, in the order the harness prints them.
pub const SCOPES: [Scope; 2] = [ESSENTIAL, GRAMMAR];

/// Run the surveyor over one scope and return its `TestReport`.
///
/// # Errors
/// `MetadataNotFound` when the model holds no definition of the surveyor
/// (the platform is not loaded); otherwise whatever the run raises.
pub fn run<H: EvalHooks>(
    evaluator: &mut Evaluator<'_, H>,
    scope: &Scope,
) -> Result<Value, PureException> {
    let surveyor = metadata::function_by_path(evaluator.model(), SURVEYOR)?;
    let args = [
        Value::String(SmolStr::new(scope.package)),
        Value::String(SmolStr::new(scope.file_path)),
        Value::String(SmolStr::new(scope.manifest_path)),
    ];
    evaluator.call_user_function_by_id_with_args(surveyor, &args)
}
