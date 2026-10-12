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

//! Representations: what a node's static type, as the expression carried
//! it, allows a value to be held as, and how much of a body carries one.

use legend_pure_metamodel::bootstrap::{BOOLEAN_ID, FLOAT_ID, INTEGER_ID, STRING_ID};
use legend_pure_metamodel::types::{ExprKind, Multiplicity, ResolvedType, TypeExpr, ValueSpec};

/// How a value of a node is held: unboxed for the primitives of
/// multiplicity `[1]` or `[0..1]`, a [`Value`](crate::value::Value)
/// otherwise.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Rep {
    /// `Integer[1]`.
    I64,
    /// `Float[1]`.
    F64,
    /// `Boolean[1]`.
    Bool,
    /// `String[1]`.
    Str,
    /// `Integer[0..1]`.
    OptI64,
    /// `Float[0..1]`.
    OptF64,
    /// `Boolean[0..1]`.
    OptBool,
    /// `String[0..1]`.
    OptStr,
    /// Anything else: no static type, a type with arguments, any other
    /// type or multiplicity.
    Value,
}

/// The one place a representation is chosen: from the static type as
/// the expression carried it, never inferred.
#[must_use]
pub fn rep_of(ty: Option<&ResolvedType>) -> Rep {
    let Some(ty) = ty else {
        return Rep::Value;
    };
    let TypeExpr::Named {
        element,
        type_arguments,
        multiplicity_arguments,
        value_arguments,
        ..
    } = &ty.type_expr
    else {
        return Rep::Value;
    };
    if !type_arguments.is_empty()
        || !multiplicity_arguments.is_empty()
        || !value_arguments.is_empty()
    {
        return Rep::Value;
    }
    let optional = match ty.multiplicity {
        Multiplicity::PureOne
        | Multiplicity::Range {
            lower: 1,
            upper: Some(1),
        } => false,
        Multiplicity::ZeroOrOne
        | Multiplicity::Range {
            lower: 0,
            upper: Some(1),
        } => true,
        _ => return Rep::Value,
    };
    match (*element, optional) {
        (INTEGER_ID, false) => Rep::I64,
        (FLOAT_ID, false) => Rep::F64,
        (BOOLEAN_ID, false) => Rep::Bool,
        (STRING_ID, false) => Rep::Str,
        (INTEGER_ID, true) => Rep::OptI64,
        (FLOAT_ID, true) => Rep::OptF64,
        (BOOLEAN_ID, true) => Rep::OptBool,
        (STRING_ID, true) => Rep::OptStr,
        _ => Rep::Value,
    }
}

/// How much of a body's expression tree carries a static type.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct Coverage {
    /// Expressions, every one under the body, lambda bodies included.
    pub nodes: usize,
    /// Expressions that carry a static type.
    pub typed: usize,
    /// Expressions whose static type has a representation other than
    /// [`Rep::Value`].
    pub unboxed: usize,
}

impl Coverage {
    /// The coverage of a body's statements.
    #[must_use]
    pub fn of(statements: &[ValueSpec]) -> Self {
        let mut coverage = Self::default();
        for statement in statements {
            coverage.add(statement);
        }
        coverage
    }

    /// Both counts together.
    #[must_use]
    pub fn plus(self, other: Self) -> Self {
        Self {
            nodes: self.nodes + other.nodes,
            typed: self.typed + other.typed,
            unboxed: self.unboxed + other.unboxed,
        }
    }

    fn add(&mut self, spec: &ValueSpec) {
        self.nodes += 1;
        if spec.type_info.is_some() {
            self.typed += 1;
        }
        if rep_of(spec.type_info.as_deref()) != Rep::Value {
            self.unboxed += 1;
        }
        match &*spec.kind {
            ExprKind::FunctionCall(data)
            | ExprKind::PropertyCall(data)
            | ExprKind::QualifiedPropertyCall(data) => {
                for arg in &data.arguments {
                    self.add(arg);
                }
            }
            ExprKind::Lambda { body, .. } => {
                for statement in body.iter() {
                    self.add(statement);
                }
            }
            ExprKind::KeyExpression { expression, .. } => self.add(expression),
            ExprKind::Collection { elements } => {
                for element in elements {
                    self.add(element);
                }
            }
            ExprKind::PathLiteral { steps, .. } => {
                for step in steps {
                    for p in &step.parameters {
                        self.add(p);
                    }
                }
            }
            ExprKind::IntegerLiteral(_)
            | ExprKind::FloatLiteral(_)
            | ExprKind::DecimalLiteral(_)
            | ExprKind::StringLiteral(_)
            | ExprKind::BooleanLiteral(_)
            | ExprKind::DateLiteral(_)
            | ExprKind::Variable { .. }
            | ExprKind::EnumValue { .. }
            | ExprKind::TypeReference { .. }
            | ExprKind::MultiplicityReference { .. }
            | ExprKind::PackageableElementRef { .. }
            | ExprKind::RelationLiteral { .. }
            | ExprKind::ColSpecArrayLiteral { .. }
            | ExprKind::ColSpecLiteral { .. } => {}
        }
    }
}
