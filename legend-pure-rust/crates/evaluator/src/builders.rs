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

//! Builders for the evaluator's inputs.
//!
//! Tests speak the evaluator's language: they build expression trees
//! (`ValueSpec`) and model elements (`Element::Class`, `Element::Function`,
//! …) directly and hand them to the evaluator. No source text, no loader.
//! The table in Step 1 of `docs/rust-evaluator-mvp.md` maps each M3 node a
//! Pure reader knows to the shape these builders produce; the PELT body
//! lowering of Step 5 must satisfy the same table.
//!
//! Two families live here:
//!
//! - [`expr`] — expression builders, one per `ExprKind` arm the evaluator
//!   executes. Every one synthesises a `<test>` source span.
//! - [`ModelBuilder`] — starts from the bootstrap, allocates elements into
//!   chunk 1, registers them in their package and rebuilds the derived
//!   indexes. It honours the one hidden contract between the model and
//!   dispatch: **a function node's `name` is its mangled signature**
//!   (`plus_Number_MANY__Number_1_`), computed with
//!   [`compute_function_fqn`] once the element is allocated. A function
//!   built with a plain name never dispatches.

use legend_pure_metamodel::fqn::compute_function_fqn;
use legend_pure_metamodel::ids::{ElementId, PackageId};
use legend_pure_metamodel::model::{Element, ElementNode, ModelChunk, PureModel};
use legend_pure_metamodel::nodes::association::Association;
use legend_pure_metamodel::nodes::class::{Class, Property, QualifiedProperty};
use legend_pure_metamodel::nodes::enumeration::{EnumValue, Enumeration};
use legend_pure_metamodel::nodes::function::Function;
use legend_pure_metamodel::nodes::profile::{Profile, bootstrap_spanned_name};
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{Multiplicity, Parameter, TypeExpr, ValueSpec};
use smol_str::SmolStr;

/// The chunk every hand-built element is allocated into (chunk 0 is the
/// bootstrap).
pub const TEST_CHUNK_ID: u16 = 1;

/// The source identifier every builder-synthesised span carries.
pub const TEST_SOURCE: &str = "<test>";

/// A synthetic source span for hand-built nodes.
#[must_use]
pub fn source() -> SourceInfo {
    SourceInfo::new(TEST_SOURCE, 1, 1, 1, 10)
}

/// A `TypeExpr::Named` pointing at `element`, with no type, multiplicity
/// or value arguments.
#[must_use]
pub fn named(element: ElementId) -> TypeExpr {
    TypeExpr::Named {
        element,
        source_info: None,
        type_arguments: Vec::new(),
        multiplicity_arguments: Vec::new(),
        value_arguments: Vec::new(),
    }
}

/// A function or qualified-property parameter.
#[must_use]
pub fn param(name: &str, type_expr: TypeExpr, multiplicity: Multiplicity) -> Parameter {
    Parameter {
        name: SmolStr::new(name),
        type_expr,
        multiplicity,
        source_info: source(),
    }
}

/// A class property with no default, aggregation or annotations.
#[must_use]
pub fn property(name: &str, type_expr: TypeExpr, multiplicity: Multiplicity) -> Property {
    Property {
        name: SmolStr::new(name),
        source_info: source(),
        type_expr,
        multiplicity,
        aggregation: None,
        default_value: None,
        stereotypes: Vec::new(),
        tagged_values: Vec::new(),
    }
}

/// A qualified property (`name(params) { body }: return[mult]`).
#[must_use]
pub fn qualified_property(
    name: &str,
    parameters: Vec<Parameter>,
    return_type: TypeExpr,
    return_multiplicity: Multiplicity,
    body: Vec<ValueSpec>,
) -> QualifiedProperty {
    QualifiedProperty {
        name: SmolStr::new(name),
        source_info: source(),
        parameters: parameters.into(),
        return_type,
        return_multiplicity,
        body: body.into(),
        stereotypes: Vec::new(),
        tagged_values: Vec::new(),
    }
}

/// A class with the given supertypes and properties and nothing else.
#[must_use]
pub fn class(super_types: Vec<TypeExpr>, properties: Vec<Property>) -> Class {
    Class {
        type_parameters: Vec::new(),
        multiplicity_parameters: Vec::new(),
        type_variable_parameters: Vec::new(),
        super_types,
        properties,
        qualified_properties: Vec::new(),
        constraints: Vec::new(),
        stereotypes: Vec::new(),
        tagged_values: Vec::new(),
        original_milestoned_properties: Vec::new(),
    }
}

/// A concrete function definition (`is_native: false`).
#[must_use]
pub fn function(
    simple_name: &str,
    parameters: Vec<Parameter>,
    return_type: TypeExpr,
    return_multiplicity: Multiplicity,
    body: Vec<ValueSpec>,
) -> Function {
    Function {
        function_name: SmolStr::new(simple_name),
        is_native: false,
        parameters: parameters.into(),
        return_type,
        return_multiplicity,
        body: body.into(),
        stereotypes: Vec::new(),
        tagged_values: Vec::new(),
    }
}

/// A native function declaration (`is_native: true`, empty body). The
/// implementation is looked up in the native registry under the mangled
/// name the [`ModelBuilder`] writes into the node.
#[must_use]
pub fn native_function(
    simple_name: &str,
    parameters: Vec<Parameter>,
    return_type: TypeExpr,
    return_multiplicity: Multiplicity,
) -> Function {
    Function {
        is_native: true,
        ..function(
            simple_name,
            parameters,
            return_type,
            return_multiplicity,
            Vec::new(),
        )
    }
}

/// Builds a [`PureModel`] out of the bootstrap plus hand-built elements.
///
/// Elements are allocated into chunk [`TEST_CHUNK_ID`] as they are added,
/// so an element's [`ElementId`] can be used in the types and bodies of
/// elements added later. Call [`finish`](Self::finish) to rebuild the
/// derived indexes (specializations, association properties) and take
/// the model.
#[derive(Debug)]
pub struct ModelBuilder {
    model: PureModel,
}

impl Default for ModelBuilder {
    fn default() -> Self {
        Self::new()
    }
}

impl ModelBuilder {
    /// A builder over [`PureModel::bootstrapped`] with an empty chunk 1.
    #[must_use]
    pub fn new() -> Self {
        let mut model = PureModel::bootstrapped();
        debug_assert_eq!(model.chunks.len(), 1, "bootstrap must be chunk 0 and alone");
        model.chunks.push(ModelChunk::new(TEST_CHUNK_ID));
        Self { model }
    }

    /// The model as built so far (read-only).
    #[must_use]
    pub fn model(&self) -> &PureModel {
        &self.model
    }

    /// Resolve a `::`-qualified path against what is built so far
    /// (bootstrap included).
    #[must_use]
    pub fn resolve(&self, fqn: &str) -> Option<ElementId> {
        self.model.resolve_fqn_str(fqn)
    }

    /// Add a class at `path` (`my::pkg::Person`).
    pub fn class(&mut self, path: &str, class: Class) -> ElementId {
        self.add(path, Element::Class(class))
    }

    /// Add a function at `path`, where the last segment is the **simple**
    /// name (`my::pkg::double`). The node's `name` is rewritten to the
    /// mangled signature so dispatch can find it.
    pub fn function(&mut self, path: &str, function: Function) -> ElementId {
        let id = self.add(path, Element::Function(function));
        let mangled = compute_function_fqn(&self.model, id)
            .unwrap_or_else(|| unreachable!("the element just added is a function"));
        let ElementId::InstanceId { local_idx, .. } = id else {
            unreachable!("add() never returns a package id")
        };
        self.model.chunks[usize::from(TEST_CHUNK_ID)]
            .nodes
            .get_mut(local_idx)
            .name = mangled;
        id
    }

    /// Add an enumeration at `path` with the given values.
    pub fn enumeration(&mut self, path: &str, values: &[&str]) -> ElementId {
        let values = values
            .iter()
            .map(|v| EnumValue {
                name: SmolStr::new(v),
                source_info: source(),
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
            })
            .collect();
        self.add(
            path,
            Element::Enumeration(Enumeration {
                values,
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
            }),
        )
    }

    /// Add an association at `path` with its two properties.
    pub fn association(&mut self, path: &str, properties: [Property; 2]) -> ElementId {
        self.add(
            path,
            Element::Association(Association {
                properties: properties.into(),
                qualified_properties: Vec::new(),
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
                original_milestoned_properties: Vec::new(),
            }),
        )
    }

    /// Add a profile at `path` with the given stereotypes and tags.
    pub fn profile(&mut self, path: &str, stereotypes: &[&str], tags: &[&str]) -> ElementId {
        let spanned = |names: &[&str]| {
            names
                .iter()
                .map(|n| bootstrap_spanned_name(SmolStr::new(n)))
                .collect()
        };
        self.add(
            path,
            Element::Profile(Profile {
                stereotypes: spanned(stereotypes),
                tags: spanned(tags),
            }),
        )
    }

    /// Add any element at `path`, registering it in its package. The
    /// node's `name` is the last path segment; [`function`](Self::function)
    /// overwrites it with the mangled signature afterwards.
    pub fn add(&mut self, path: &str, element: Element) -> ElementId {
        let (package, name) = self.package_and_name(path);
        let node = ElementNode {
            name,
            source_info: source(),
            name_source_info: source(),
            parent_package: package,
        };
        let local_idx = self.model.chunks[usize::from(TEST_CHUNK_ID)].alloc_element(node, element);
        let id = ElementId::InstanceId {
            chunk_id: TEST_CHUNK_ID,
            local_idx,
        };
        self.model.register_element(package, id);
        id
    }

    /// Mutable access to an element added through this builder, for the
    /// few shapes that are built in two steps (a measure and its units
    /// reference each other).
    ///
    /// # Panics
    ///
    /// Panics if `id` was not allocated by this builder.
    pub fn element_mut(&mut self, id: ElementId) -> &mut Element {
        let ElementId::InstanceId {
            chunk_id,
            local_idx,
            ..
        } = id
        else {
            panic!("{id:?} is a package, not a builder-allocated element")
        };
        assert_eq!(
            chunk_id, TEST_CHUNK_ID,
            "{id:?} was not allocated by this builder"
        );
        self.model.chunks[usize::from(TEST_CHUNK_ID)]
            .elements
            .get_mut(local_idx)
    }

    /// Rebuild the derived indexes and hand over the model.
    #[must_use]
    pub fn finish(mut self) -> PureModel {
        self.model.rebuild_derived_indexes();
        self.model
    }

    fn package_and_name(&mut self, path: &str) -> (PackageId, SmolStr) {
        let mut segments: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
        let name = segments
            .pop()
            .unwrap_or_else(|| unreachable!("split always yields one segment"));
        assert!(
            !name.is_empty(),
            "element path must end in a name: {path:?}"
        );
        let package = self.model.get_or_create_package(&segments);
        (package, name)
    }
}

/// Expression builders, one per `ExprKind` arm the evaluator executes.
pub mod expr {
    use legend_pure_metamodel::ids::ElementId;
    use legend_pure_metamodel::types::{
        DateValue, ExprKind, FunctionCallData, Multiplicity, Parameter, TypeExpr, ValueSpec,
    };
    use smol_str::SmolStr;

    use super::source;

    /// Wrap an [`ExprKind`] in a [`ValueSpec`] with a synthetic span and
    /// no static type information.
    #[must_use]
    pub fn spec(kind: ExprKind) -> ValueSpec {
        ValueSpec {
            kind: Box::new(kind),
            source_info: source(),
            type_info: None,
        }
    }

    /// `IntegerLiteral`.
    #[must_use]
    pub fn int(n: i64) -> ValueSpec {
        spec(ExprKind::IntegerLiteral(n))
    }

    /// `FloatLiteral`.
    #[must_use]
    pub fn float(f: f64) -> ValueSpec {
        spec(ExprKind::FloatLiteral(f))
    }

    /// `DecimalLiteral`.
    #[must_use]
    pub fn decimal(d: rust_decimal::Decimal) -> ValueSpec {
        spec(ExprKind::DecimalLiteral(d))
    }

    /// `StringLiteral`.
    #[must_use]
    pub fn string(s: &str) -> ValueSpec {
        spec(ExprKind::StringLiteral(SmolStr::new(s)))
    }

    /// `BooleanLiteral`.
    #[must_use]
    pub fn boolean(b: bool) -> ValueSpec {
        spec(ExprKind::BooleanLiteral(b))
    }

    /// `DateLiteral`.
    #[must_use]
    pub fn date(value: DateValue) -> ValueSpec {
        spec(ExprKind::DateLiteral(value))
    }

    /// `Collection` of the given elements (`[a, b, c]`).
    #[must_use]
    pub fn collection(elements: Vec<ValueSpec>) -> ValueSpec {
        spec(ExprKind::Collection { elements })
    }

    /// `Variable` reference (`$name`).
    #[must_use]
    pub fn var(name: &str) -> ValueSpec {
        spec(ExprKind::Variable {
            name: SmolStr::new(name),
        })
    }

    /// `FunctionCall` whose target the compiler did **not** resolve
    /// (`function: None`); `name` is the mangled signature dispatch looks
    /// up in the native registry.
    #[must_use]
    pub fn call(name: &str, arguments: Vec<ValueSpec>) -> ValueSpec {
        spec(ExprKind::FunctionCall(FunctionCallData {
            function: None,
            function_name: SmolStr::new(name),
            arguments,
        }))
    }

    /// `FunctionCall` resolved to a model element, as the compiler leaves
    /// it. `name` is the simple name, kept for messages.
    #[must_use]
    pub fn call_resolved(target: ElementId, name: &str, arguments: Vec<ValueSpec>) -> ValueSpec {
        spec(ExprKind::FunctionCall(FunctionCallData {
            function: Some(target),
            function_name: SmolStr::new(name),
            arguments,
        }))
    }

    /// `PropertyCall` (`$receiver.name`): the receiver is `arguments[0]`.
    #[must_use]
    pub fn prop(receiver: ValueSpec, name: &str) -> ValueSpec {
        spec(ExprKind::PropertyCall(FunctionCallData {
            function: None,
            function_name: SmolStr::new(name),
            arguments: vec![receiver],
        }))
    }

    /// `QualifiedPropertyCall` (`$receiver.name(args)`): the receiver is
    /// `arguments[0]`, the qualified property's own arguments follow.
    #[must_use]
    pub fn qp(receiver: ValueSpec, name: &str, arguments: Vec<ValueSpec>) -> ValueSpec {
        let mut all = Vec::with_capacity(arguments.len() + 1);
        all.push(receiver);
        all.extend(arguments);
        spec(ExprKind::QualifiedPropertyCall(FunctionCallData {
            function: None,
            function_name: SmolStr::new(name),
            arguments: all,
        }))
    }

    /// `Lambda` (`{params | body}`).
    #[must_use]
    pub fn lambda(parameters: Vec<Parameter>, body: Vec<ValueSpec>) -> ValueSpec {
        spec(ExprKind::Lambda { parameters, body })
    }

    /// `EnumValue` (`my::Colour.RED`).
    #[must_use]
    pub fn enum_value(enumeration: ElementId, value: &str) -> ValueSpec {
        spec(ExprKind::EnumValue {
            enum_element: enumeration,
            value: SmolStr::new(value),
        })
    }

    /// `TypeReference` (`@Type`).
    #[must_use]
    pub fn type_ref(type_expr: TypeExpr) -> ValueSpec {
        spec(ExprKind::TypeReference { type_expr })
    }

    /// `MultiplicityReference` (`@[m]`).
    #[must_use]
    pub fn mult_ref(multiplicity: Multiplicity) -> ValueSpec {
        spec(ExprKind::MultiplicityReference { multiplicity })
    }

    /// `PackageableElementRef` (a bare element path used as a value).
    #[must_use]
    pub fn element_ref(element: ElementId) -> ValueSpec {
        spec(ExprKind::PackageableElementRef { element })
    }
}
