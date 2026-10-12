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

//! The `Rust_Native` PCT run over the bootstrap alone: the surveyor is a
//! platform function the model has no metadata for, and the run says so.
//! When a step loads the platform, this test is where the counts go.

use legend_pure_evaluator::builders::on_both_paths;
use legend_pure_evaluator::error::{PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::pct;
use legend_pure_metamodel::model::PureModel;

#[test]
fn the_surveyor_has_no_loaded_definition_over_the_bootstrap() {
    on_both_paths(|| {
        let model = PureModel::bootstrapped();
        let natives = NativeRegistry::standard();
        let mut evaluator = Evaluator::new(&model, &natives);
        for scope in &pct::SCOPES {
            let error = pct::run(&mut evaluator, scope).unwrap_err();
            match &error.kind {
                PureExceptionKind::ExecutionError(PureRuntimeError::MetadataNotFound {
                    function,
                    ..
                }) => assert_eq!(function, pct::SURVEYOR),
                other => panic!("{}: expected MetadataNotFound, got {other:?}", scope.module),
            }
            let text = error.to_string();
            assert!(
                text.contains("no loaded definition of function `meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_`"),
                "{text}"
            );
            assert!(
                text.contains("the model holds the bootstrap m3.pure ("),
                "{text}"
            );
        }
    });
}

#[test]
fn the_manifests_name_the_adapter() {
    on_both_paths(|| {
        for scope in &pct::SCOPES {
            let path = format!("{}/pct/{}", env!("CARGO_MANIFEST_DIR"), scope.manifest_path);
            let text = std::fs::read_to_string(&path).unwrap_or_else(|e| panic!("{path}: {e}"));
            assert!(
                text.contains(&format!("\"adapter\": \"{}\"", pct::ADAPTER)),
                "{path}"
            );
        }
    });
}
