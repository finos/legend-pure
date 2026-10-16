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

//! The platform PCT corpus through the `Rust_Native` adapter: the surveyor
//! on the evaluator alone, no host engine. Prints one line per scope, the
//! count once the platform's metadata is loaded, the failure until then.
//!
//! ```bash
//! cargo run -p legend-pure-evaluator --example pct_native
//! ```

use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::pct;
use legend_pure_metamodel::model::PureModel;

fn main() {
    let model = PureModel::bootstrapped();
    let natives = NativeRegistry::standard();
    let mut evaluator = Evaluator::new(&model, &natives);
    let mut failed = false;
    for scope in &pct::SCOPES {
        match pct::run(&mut evaluator, scope) {
            Ok(report) => println!("Rust_Native {}: {report:?}", scope.module),
            Err(error) => {
                failed = true;
                println!("Rust_Native {}: {error}", scope.module);
            }
        }
    }
    if failed {
        std::process::exit(1);
    }
}
