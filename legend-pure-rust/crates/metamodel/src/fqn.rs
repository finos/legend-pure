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

//! Function FQN (Fully Qualified Name) computation.
//!
//! Mirrors Java's `ConcreteFunctionDefinitionNameProcessor`: the mangled
//! function name encodes parameter types, multiplicities, and the return
//! type/multiplicity. This is the key used to look up native functions
//! in the runtime's `NativeRegistry`.
//!
//! # Format
//!
//! ```text
//! funcName_ParamType_Mult__ParamType_Mult__ReturnType_Mult_
//! ```
//!
//! Each parameter contributes `Type + multSig + "_"`, producing a
//! double-underscore `__` boundary between consecutive parameters.
//!
//! ## Multiplicity encoding (from `Multiplicity.multiplicityToSignatureString`)
//!
//! | Pure | Signature |
//! |------|-----------|
//! | `[1]` | `_1_` |
//! | `[0..1]` | `_$0_1$_` |
//! | `[*]` | `_MANY_` |
//! | `[1..*]` | `_$1_MANY$_` |
//! | `[n..m]` | `_$n_m$_` |
//!
//! ## Examples
//!
//! - `plus(Integer[*]): Integer[1]`
//!   → `plus_Integer_MANY__Integer_1_`
//! - `first(T[*]): T[0..1]`
//!   → `first_T_MANY__T_$0_1$_`
//! - `now(): DateTime[1]`
//!   → `now__DateTime_1_`

use crate::ids::ElementId;
use crate::model::{Element, PureModel};
use crate::nodes::function::Function;
use crate::types::{Multiplicity, TypeExpr};
use smol_str::SmolStr;

/// Compute the mangled function name (FQN) for a function element.
///
/// Returns `None` if the element is not a `Function`.
#[must_use]
pub fn compute_function_fqn(model: &PureModel, element_id: ElementId) -> Option<SmolStr> {
    let node = model.get_node(element_id);
    let element = model.get_element(element_id);
    let Element::Function(func) = element else {
        return None;
    };
    Some(SmolStr::new(build_function_fqn(&node.name, func, model)))
}

/// Build the mangled name from the function's simple name, parameters,
/// and return type — matching Java's `ConcreteFunctionDefinitionNameProcessor`.
pub(crate) fn build_function_fqn(simple_name: &str, func: &Function, model: &PureModel) -> String {
    let mut builder = String::with_capacity(64);
    builder.push_str(simple_name);
    builder.push('_');

    for param in func.parameters.iter() {
        append_type_signature(&mut builder, &param.type_expr, model);
        append_multiplicity_signature(&mut builder, &param.multiplicity);
        builder.push('_');
    }

    if func.parameters.is_empty() {
        builder.push('_');
    }

    append_type_signature(&mut builder, &func.return_type, model);
    append_multiplicity_signature(&mut builder, &func.return_multiplicity);

    builder
}

/// Append the type name to the signature.
///
/// Java's `appendSignatureStringForType` cases:
/// - Generic (rawType == null): uses type parameter name (T, V, etc.)
/// - Unit: `Measure$UnitName`
/// - `PackageableElement`: `rawType.getName()` (String, Integer, Relation, etc.)
fn append_type_signature(builder: &mut String, type_expr: &TypeExpr, model: &PureModel) {
    match type_expr {
        TypeExpr::Named { element, .. } => {
            let node = model.get_node(*element);
            builder.push_str(&node.name);
        }
        TypeExpr::Generic(name) => {
            builder.push_str(name);
        }
        TypeExpr::FunctionType { .. } => {
            builder.push_str("Function");
        }
        TypeExpr::Relation(_) => {
            builder.push_str("Relation");
        }
        TypeExpr::GenericTypeOperation { .. } => {
            builder.push_str("Any");
        }
        TypeExpr::Unresolved => {
            builder.push_str("Any");
        }
    }
}

/// Append the multiplicity signature, matching Java's `multiplicityToSignatureString`.
///
/// Each multiplicity is wrapped in `_..._`, so the output includes the
/// leading and trailing underscores.
fn append_multiplicity_signature(builder: &mut String, mult: &Multiplicity) {
    match mult {
        Multiplicity::PureOne => builder.push_str("_1_"),
        Multiplicity::ZeroOrOne => builder.push_str("_$0_1$_"),
        Multiplicity::ZeroOrMany => builder.push_str("_MANY_"),
        Multiplicity::OneOrMany => builder.push_str("_$1_MANY$_"),
        Multiplicity::Range { lower, upper } => match upper {
            None => {
                if *lower == 0 {
                    builder.push_str("_MANY_");
                } else {
                    builder.push_str("_$");
                    builder.push_str(&lower.to_string());
                    builder.push_str("_MANY$_");
                }
            }
            Some(u) => {
                if lower == u {
                    builder.push('_');
                    builder.push_str(&u.to_string());
                    builder.push('_');
                } else {
                    builder.push_str("_$");
                    builder.push_str(&lower.to_string());
                    builder.push('_');
                    builder.push_str(&u.to_string());
                    builder.push_str("$_");
                }
            }
        },
        Multiplicity::Variable(name) => {
            builder.push('_');
            builder.push_str(name);
            builder.push('_');
        }
    }
}
