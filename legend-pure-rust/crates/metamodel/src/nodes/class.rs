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

//! Compiled Class node with properties, constraints, and type parameters.
//!
//! **No bidirectional pointers.** The Class stores only **generalizations**
//! (`super_types`) — the canonical upward direction from the AST `extends`
//! clause. Specializations, association-injected properties, and milestoning
//! synthetic properties are computed as derived indexes on the frozen model.

use crate::source_info::SourceInfo;
use smol_str::SmolStr;
use std::sync::Arc;

use crate::annotations::{StereotypeRef, TaggedValueRef};
use crate::types::{Expression, Multiplicity, Parameter, TypeExpr};

/// Variance of a class type-parameter slot. Mirrors Java's
/// `TypeParameter.contravariant` flag (the metamodel-level form) and
/// the surface syntax `<-T>` / `<+T>` (the class-level form). Default
/// `Invariant`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default)]
pub enum Variance {
    /// Default: invariant. `Container<Integer>` is neither sub- nor
    /// super-type of `Container<Number>`.
    #[default]
    Invariant,
    /// Covariant `<+T>`. `Producer<Integer>` is a subtype of
    /// `Producer<Number>` (output position).
    Covariant,
    /// Contravariant `<-T>`. `Consumer<Number>` is a subtype of
    /// `Consumer<Integer>` (input position). Pure's metamodel uses
    /// this for `Property`'s owner slot, `Column`'s row slot, and
    /// `NewPropertyRouteNodeFunctionDefinition`.
    Contravariant,
}

/// A single type-parameter slot on a parametric class.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TypeParameter {
    /// Declared name (e.g., `T`, `U`).
    pub name: SmolStr,
    /// Variance flag. Defaults to `Invariant`.
    pub variance: Variance,
}

impl TypeParameter {
    /// Build an invariant type parameter from a name.
    #[must_use]
    pub fn invariant(name: SmolStr) -> Self {
        Self {
            name,
            variance: Variance::Invariant,
        }
    }

    /// Build a type parameter from an explicit `(name, variance)` pair.
    #[must_use]
    pub fn new(name: SmolStr, variance: Variance) -> Self {
        Self { name, variance }
    }
}

/// A compiled class definition.
///
/// Mirrors `ast::ClassDef` with all references resolved to `ElementId`s.
/// Stores only canonical, unidirectional data — no specializations,
/// no association-injected properties.
#[derive(Debug, Clone, PartialEq)]
pub struct Class {
    /// Type parameters (e.g., `[T, U]`). Each carries its own
    /// variance flag — see [`TypeParameter`].
    pub type_parameters: Vec<TypeParameter>,
    /// Multiplicity parameters declared on a parametric class —
    /// `Class Holder<T|m>` carries one parameter named `m`.
    pub multiplicity_parameters: Vec<SmolStr>,
    /// Type-variable VALUE parameters declared on a parametric class —
    /// `Class C(x:Integer[1]) [...]` carries one parameter named `x`.
    /// Bound at construction time from `^C(10)(props)` and threaded
    /// through to constraints / qualified properties so `$x` resolves
    /// against the receiver instance.
    pub type_variable_parameters: Vec<crate::types::Parameter>,
    /// Generalizations only (upward). Never stores specializations.
    /// Uses `TypeExpr` (not plain `ElementId`) because a class can
    /// extend a parameterized type: `extends List<String>`.
    pub super_types: Vec<TypeExpr>,
    /// Declared properties only.
    pub properties: Vec<Property>,
    /// Qualified (derived) properties.
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Constraints.
    pub constraints: Vec<Constraint>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValueRef>,
    /// Properties moved aside by milestoning post-processing because their
    /// target class carries a temporal stereotype. Empty for classes with no
    /// milestoned-target properties.
    pub original_milestoned_properties: Vec<Property>,
}

impl Class {
    /// Materialize the type-parameter declared names.
    #[must_use]
    pub fn type_parameter_names(&self) -> Vec<SmolStr> {
        self.type_parameters
            .iter()
            .map(|tp| tp.name.clone())
            .collect()
    }
}

/// Aggregation kind for properties.
///
/// Mirrors `ast::AggregationKind`. Most properties have no aggregation.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AggregationKind {
    /// No aggregation — `(none)`.
    None,
    /// Shared aggregation — `(shared)`.
    Shared,
    /// Composite aggregation — `(composite)`.
    Composite,
}

/// A compiled class or association property.
///
/// Reused by both Class and Association.
#[derive(Debug, Clone, PartialEq)]
pub struct Property {
    /// Property name.
    pub name: SmolStr,
    /// Source location.
    pub source_info: SourceInfo,
    /// Resolved property type.
    pub type_expr: TypeExpr,
    /// Property multiplicity.
    pub multiplicity: Multiplicity,
    /// Aggregation kind (if specified).
    pub aggregation: Option<AggregationKind>,
    /// Default value expression (if specified).
    pub default_value: Option<Expression>,
    /// Stereotypes on this property.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values on this property.
    pub tagged_values: Vec<TaggedValueRef>,
}

/// A compiled qualified (derived) property.
///
/// Reused by both Class and Association.
#[derive(Debug, Clone, PartialEq)]
pub struct QualifiedProperty {
    /// Property name.
    pub name: SmolStr,
    /// Source location.
    pub source_info: SourceInfo,
    /// Parameters.
    pub parameters: Arc<[Parameter]>,
    /// Return type.
    pub return_type: TypeExpr,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Body expressions.
    pub body: Arc<[Expression]>,
    /// Stereotypes on this qualified property.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values on this qualified property.
    pub tagged_values: Vec<TaggedValueRef>,
}

/// A compiled class constraint.
#[derive(Debug, Clone, PartialEq)]
pub struct Constraint {
    /// Constraint name (optional — unnamed constraints are allowed).
    pub name: Option<SmolStr>,
    /// Source location.
    pub source_info: SourceInfo,
    /// The constraint function/expression.
    pub function: Expression,
    /// Enforcement level, e.g., `Warn`, `Error`.
    pub enforcement_level: Option<SmolStr>,
    /// External identifier.
    pub external_id: Option<String>,
    /// Message function (evaluated when constraint fails).
    pub message: Option<Expression>,
}
