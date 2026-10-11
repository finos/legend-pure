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

//! Finding a function's loaded definition by its user path, for callers
//! outside the expression tree. The model answers from the metadata it
//! holds: the bootstrap, and whatever a later step loads (a PELT module).

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use smol_str::SmolStr;

use crate::error::PureRuntimeError;

/// The element of the function at `path` (`a::b::f_X_1__Y_1_`).
///
/// # Errors
/// [`PureRuntimeError::MetadataNotFound`] when no loaded definition of a
/// function has that path; the error says what the model holds.
pub fn function_by_path(model: &PureModel, path: &str) -> Result<ElementId, PureRuntimeError> {
    let segments: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
    model
        .resolve_function_by_path(&segments)
        .or_else(|| model.resolve_by_path(&segments))
        .filter(|id| matches!(model.get_element(*id), Element::Function(_)))
        .ok_or_else(|| PureRuntimeError::MetadataNotFound {
            function: SmolStr::new(path),
            loaded: loaded(model),
        })
}

/// What the model holds, for the error: the chunks and their sizes.
fn loaded(model: &PureModel) -> String {
    let mut chunk: u16 = 0;
    let mut parts = Vec::new();
    loop {
        let len = model.chunk_len(chunk);
        if len == 0 && chunk > 0 {
            break;
        }
        parts.push(match chunk {
            0 => format!("the bootstrap m3.pure ({len} elements)"),
            n => format!("chunk {n} ({len} elements)"),
        });
        chunk += 1;
    }
    parts.join(", ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_platform_function_is_not_in_the_bootstrap() {
        let model = PureModel::bootstrapped();
        let path = "meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_";
        let text = function_by_path(&model, path).unwrap_err().to_string();
        assert!(
            text.starts_with(&format!(
                "no loaded definition of function `{path}`: the model holds the bootstrap m3.pure ("
            )),
            "{text}"
        );
        assert!(
            text.ends_with(" elements) and no metadata for it"),
            "{text}"
        );
    }

    #[test]
    fn a_class_is_not_a_function() {
        let model = PureModel::bootstrapped();
        assert!(function_by_path(&model, "meta::pure::metamodel::type::Class").is_err());
    }

    #[test]
    fn a_loaded_function_is_found() {
        use crate::builders::{ModelBuilder, function, named};
        use legend_pure_metamodel::bootstrap::INTEGER_ID;
        use legend_pure_metamodel::types::Multiplicity;
        let mut builder = ModelBuilder::new();
        let expected = builder.function(
            "pkg::f__Integer_1_",
            function(
                "f",
                Vec::new(),
                named(INTEGER_ID),
                Multiplicity::PureOne,
                Vec::new(),
            ),
        );
        let model = builder.finish();
        assert_eq!(
            function_by_path(&model, "pkg::f__Integer_1_").unwrap(),
            expected
        );
    }
}
