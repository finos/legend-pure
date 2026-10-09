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

//! Bootstrap — Chunk 0 with well-known element IDs for built-in types.
//!
//! Every `PureModel` starts with a bootstrap chunk (`chunk_id` = 0) that
//! contains the primitive types, `Any`, and `Nil`. These have deterministic
//! `ElementId`s that can be used as compile-time constants.
//!
//! The full chunk contents — the 13 root-level entries plus the M3 metamodel
//! parsed from `m3.pure` — are built **once per process** into a
//! [`OnceLock`]-cached template; each `create_bootstrap_chunk` call then
//! clones the template arenas. The first call pays the parse cost; every
//! subsequent call is a pair of arena clones (linear in element count, no
//! lexer / parser work).
//!
//! # Usage
//!
//! ```
//! use legend_pure_metamodel::bootstrap;
//!
//! // These are compile-time constants — no model lookup needed:
//! let _ = bootstrap::ANY_ID;
//! let _ = bootstrap::STRING_ID;
//! ```

use std::sync::OnceLock;

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::arena::Arena;
use crate::ids::{ElementId, PackageId};
use crate::m3_parser::M3Registration;
use crate::model::{Element, ElementNode, ModelChunk};
use crate::nodes::class::Class;
use crate::types::PrimitiveType;

/// The bootstrap chunk ID.
pub const BOOTSTRAP_CHUNK_ID: u16 = 0;

// -- Type lattice --

/// `Any` — the top type. Everything is a subtype of `Any`.
pub const ANY_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 0,
};
/// `Nil` — the bottom type. Subtype of everything. Type of `[]`.
pub const NIL_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 1,
};

// -- Direct children of Any --

/// `String` — extends `Any`.
pub const STRING_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 2,
};
/// `Boolean` — extends `Any`.
pub const BOOLEAN_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 3,
};
/// `Byte` — extends `Any`.
pub const BYTE_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 4,
};
/// `StrictTime` — extends `Any`.
pub const STRICT_TIME_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 5,
};

// -- Numeric hierarchy: Number → Any --

/// `Number` — abstract numeric supertype. Extends `Any`.
pub const NUMBER_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 6,
};
/// `Integer` — extends `Number`.
pub const INTEGER_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 7,
};
/// `Float` — extends `Number`.
pub const FLOAT_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 8,
};
/// `Decimal` — extends `Number`.
pub const DECIMAL_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 9,
};

// -- Temporal hierarchy: Date → Any --

/// `Date` — abstract temporal supertype. Extends `Any`.
pub const DATE_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 10,
};
/// `StrictDate` — date without time. Extends `Date`.
pub const STRICT_DATE_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 11,
};
/// `DateTime` — date with time. Extends `Date`.
pub const DATE_TIME_ID: ElementId = ElementId::InstanceId {
    chunk_id: 0,
    local_idx: 12,
};

/// The well-known primitive types with their inheritance edges.
///
/// **Important:** `Any` and `Nil` are NOT in this list — they are `Class`
/// instances in the M3 metamodel, not `PrimitiveType` instances.
/// They are allocated separately in [`build_template`].
///
/// The inheritance hierarchy mirrors the Java M3:
///
/// ```text
/// Any (top, Class)          ← local_idx 0
/// ├── String                ← local_idx 2
/// ├── Boolean               ← local_idx 3
/// ├── Byte                  ← local_idx 4
/// ├── StrictTime            ← local_idx 5
/// ├── Number (abstract)     ← local_idx 6
/// │   ├── Integer           ← local_idx 7
/// │   ├── Float             ← local_idx 8
/// │   └── Decimal           ← local_idx 9
/// └── Date (abstract)       ← local_idx 10
///     ├── StrictDate        ← local_idx 11
///     └── DateTime          ← local_idx 12
///
/// Nil (bottom, Class)       ← local_idx 1
/// ```
pub(crate) const BOOTSTRAP_PRIMITIVES: &[(&str, ElementId, ElementId)] = &[
    // Direct children of Any
    ("String", STRING_ID, ANY_ID),
    ("Boolean", BOOLEAN_ID, ANY_ID),
    ("Byte", BYTE_ID, ANY_ID),
    ("StrictTime", STRICT_TIME_ID, ANY_ID),
    // Numeric hierarchy
    ("Number", NUMBER_ID, ANY_ID),
    ("Integer", INTEGER_ID, NUMBER_ID),
    ("Float", FLOAT_ID, NUMBER_ID),
    ("Decimal", DECIMAL_ID, NUMBER_ID),
    // Temporal hierarchy
    ("Date", DATE_ID, ANY_ID),
    ("StrictDate", STRICT_DATE_ID, DATE_ID),
    ("DateTime", DATE_TIME_ID, DATE_ID),
];

// ---------------------------------------------------------------------------
// M3 metamodel — parsed from m3.pure at compile time (file embed) and
// then parsed at runtime on first `create_bootstrap_chunk` call.
// ---------------------------------------------------------------------------

/// The actual M3 metamodel source, loaded at compile time.
const M3_SOURCE: &str = include_str!(
    "../../../../legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/grammar/m3.pure"
);

/// M3 elements that already exist in the bootstrap chunk as primitives or
/// classes but also need to be registered in their M3 packages as aliases.
///
/// Each entry is `(existing_element_id, &[package_segments])`.
const M3_ALIASES: &[(ElementId, &[&str])] = &[
    (ANY_ID, &["meta", "pure", "metamodel", "type"]),
    (NIL_ID, &["meta", "pure", "metamodel", "type"]),
];

// ---------------------------------------------------------------------------
// Cached template — built once per process, cloned per call.
// ---------------------------------------------------------------------------

/// The cached chunk-0 template — built from `m3.pure` on first access.
///
/// `parent_package` on every node is baked to `PackageId(0)` (the always-zero
/// root package id, since `PureModel::new` allocates the root package as
/// its first arena entry). `create_bootstrap_chunk` overwrites this field
/// only if its `root_package` argument differs.
struct BootstrapTemplate {
    nodes: Arena<ElementNode>,
    elements: Arena<Element>,
    registrations: Vec<M3Registration>,
}

static TEMPLATE: OnceLock<BootstrapTemplate> = OnceLock::new();

fn build_template() -> BootstrapTemplate {
    let mut nodes = Arena::with_capacity(256);
    let mut elements = Arena::with_capacity(256);

    let synthetic_source = SourceInfo::new("<bootstrap>", 0, 0, 0, 0);
    let root_package = PackageId(0);

    let alloc_node = |arena: &mut Arena<ElementNode>, name: &str| {
        arena.alloc(ElementNode {
            name: SmolStr::new(name),
            source_info: synthetic_source.clone(),
            name_source_info: synthetic_source.clone(),
            parent_package: root_package,
        })
    };

    // -- Slot 0: Any (Class, top type) --
    let any_node = alloc_node(&mut nodes, "Any");
    let any_elem = elements.alloc(Element::Class(Class {
        type_parameters: vec![],
        multiplicity_parameters: Vec::new(),
        type_variable_parameters: vec![],
        super_types: vec![],
        properties: vec![],
        qualified_properties: vec![],
        constraints: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        original_milestoned_properties: vec![],
    }));
    debug_assert_eq!(any_node, ANY_ID.local_idx());
    debug_assert_eq!(any_elem, ANY_ID.local_idx());

    // -- Slot 1: Nil (Class, bottom type) --
    let nil_node = alloc_node(&mut nodes, "Nil");
    let nil_elem = elements.alloc(Element::Class(Class {
        type_parameters: vec![],
        multiplicity_parameters: Vec::new(),
        type_variable_parameters: vec![],
        super_types: vec![],
        properties: vec![],
        qualified_properties: vec![],
        constraints: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        original_milestoned_properties: vec![],
    }));
    debug_assert_eq!(nil_node, NIL_ID.local_idx());
    debug_assert_eq!(nil_elem, NIL_ID.local_idx());

    // -- Slots 2..12: Primitive types --
    for &(name, expected_id, super_type) in BOOTSTRAP_PRIMITIVES {
        let actual_idx = alloc_node(&mut nodes, name);
        let elem_idx = elements.alloc(Element::PrimitiveType(PrimitiveType {
            super_type: Some(super_type),
            super_type_value_arguments: Vec::new(),
            type_variable_parameters: Vec::new(),
            constraints: Vec::new(),
        }));
        debug_assert_eq!(actual_idx, expected_id.local_idx());
        debug_assert_eq!(elem_idx, expected_id.local_idx());
    }

    // -- M3 metamodel elements (slots 13..): parsed from m3.pure --
    let registrations = crate::m3_parser::parse_m3_into_chunk(M3_SOURCE, &mut nodes, &mut elements);

    BootstrapTemplate {
        nodes,
        elements,
        registrations,
    }
}

/// Creates the bootstrap `ModelChunk` (`chunk_id` = 0) containing all
/// well-known types: `Any` (top class), `Nil` (bottom class), the
/// 11 primitive types, and M3 metamodel elements parsed from `m3.pure`.
///
/// First call parses `m3.pure` into a process-wide cached template. Every
/// subsequent call clones the template arenas — no lexer / parser work.
///
/// Returns `(chunk, registrations)`:
/// - `chunk` is appended to `model.chunks` as chunk 0.
/// - `registrations` describes how each parsed M3 element should be wired
///   into its M3 package; pass it to [`register_m3_packages`] after the
///   chunk is in the model.
///
/// The `root_package` argument sets `parent_package` on every node. In
/// practice this is always `PackageId(0)` (the root allocated first by
/// `PureModel::new`), so a clone of the cached template suffices; if a
/// caller passes a different value the patched field is rewritten over
/// the cloned arena.
#[must_use]
pub fn create_bootstrap_chunk(root_package: PackageId) -> (ModelChunk, Vec<M3Registration>) {
    let template = TEMPLATE.get_or_init(build_template);

    let mut nodes = template.nodes.clone();
    if root_package != PackageId(0) {
        for idx in 0..nodes.len() {
            nodes.get_mut(idx).parent_package = root_package;
        }
    }

    (
        ModelChunk {
            chunk_id: BOOTSTRAP_CHUNK_ID,
            nodes,
            elements: template.elements.clone(),
        },
        template.registrations.clone(),
    )
}

/// Returns the M3 metatype element for a compiled element.
///
/// Used by dispatch to type bare element references: `ClassWithDefault` as a
/// value has metatype `meta::pure::metamodel::type::Class`; `MyEnum` has
/// metatype `Enumeration`; and so on. Unifies the lookup that previously
/// lived in both `resolve.rs` and `lower.rs` with divergent element-kind
/// coverage.
///
/// The M3 metatypes live in packages parsed from `m3.pure` at bootstrap, so
/// the lookup is a `resolve_by_path` call rather than a compile-time constant.
#[must_use]
pub fn metatype_of(model: &crate::model::PureModel, element: &Element) -> Option<ElementId> {
    // DSL instances carry their classifier FQN explicitly in the
    // payload — resolve it directly rather than dispatching through the
    // static metatype table.
    if let Element::DSLInstance(d) = element {
        let segments: Vec<SmolStr> = d.classifier_fqn.split("::").map(SmolStr::new).collect();
        return model.resolve_by_path(&segments);
    }
    let path: &[&str] = match element {
        Element::Class(_) => &["meta", "pure", "metamodel", "type", "Class"],
        Element::Enumeration(_) => &["meta", "pure", "metamodel", "type", "Enumeration"],
        Element::PrimitiveType(_) => &["meta", "pure", "metamodel", "type", "PrimitiveType"],
        // Native functions: m3.pure declares `NativeFunction` (not
        // `NativeFunctionDefinition` — Java's runtime distinguishes
        // those, but our compiler-side metatype lookup uses the M3
        // metaclass that actually exists in m3.pure).
        Element::Function(f) if f.is_native => {
            &["meta", "pure", "metamodel", "function", "NativeFunction"]
        }
        Element::Function(_) => &[
            "meta",
            "pure",
            "metamodel",
            "function",
            "ConcreteFunctionDefinition",
        ],
        Element::Measure(_) => &["meta", "pure", "metamodel", "type", "Measure"],
        Element::Unit(_) => &["meta", "pure", "metamodel", "type", "Unit"],
        Element::Profile(_) => &["meta", "pure", "metamodel", "extension", "Profile"],
        Element::Association(_) => &["meta", "pure", "metamodel", "relationship", "Association"],
        Element::PackageableMultiplicity(_) => &[
            "meta",
            "pure",
            "metamodel",
            "multiplicity",
            "PackageableMultiplicity",
        ],
        Element::Package(_) => &["meta", "pure", "metamodel", "type", "Package"],
        // Handled via early return above.
        Element::DSLInstance(_) => unreachable!(),
    };
    let segments: Vec<SmolStr> = path.iter().map(|&s| SmolStr::new(s)).collect();
    model.resolve_by_path(&segments)
}

/// Registers M3 metamodel elements in their canonical packages.
///
/// After bootstrap chunk creation, this function:
/// 1. Walks the `M3Registration` list produced by `create_bootstrap_chunk`
/// 2. Creates the M3 package hierarchy (`meta::pure::metamodel::*`)
/// 3. Registers each parsed M3 element in its correct package
/// 4. Registers existing bootstrap elements (`Any`, `Nil`) as aliases
///    in their M3 packages
///
/// Call this in the compilation pipeline after `create_bootstrap_chunk()`
/// and before Pass 1.
pub fn register_m3_packages(model: &mut crate::model::PureModel, registrations: &[M3Registration]) {
    for reg in registrations {
        let package_id = model.get_or_create_package(&reg.package_segments);
        model.register_element(package_id, reg.element_id);
    }

    // Register existing bootstrap elements as aliases in their M3 packages
    for &(element_id, pkg_segments) in M3_ALIASES {
        let pkg_path: Vec<SmolStr> = pkg_segments.iter().map(|&s| SmolStr::new(s)).collect();
        let package_id = model.get_or_create_package(&pkg_path);
        model.register_element(package_id, element_id);
    }
}

impl crate::model::PureModel {
    /// A model holding only the bootstrap chunk (chunk 0): primitives,
    /// `Any`, `Nil`, and the classes of `m3.pure`, with the M3 packages
    /// registered so paths such as `meta::pure::metamodel::type::Class`
    /// resolve.
    ///
    /// This is the starting point of every evaluator model; further
    /// elements arrive from the test builders or from a loader.
    #[must_use]
    pub fn bootstrapped() -> Self {
        let mut model = Self::new();
        let (chunk, registrations) = create_bootstrap_chunk(model.root_package);
        model.chunks.push(chunk);
        register_m3_packages(&mut model, &registrations);
        model
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bootstrap_chunk_has_correct_count() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));
        assert_eq!(chunk.chunk_id, 0);
        // 13 primitives + all M3 elements parsed from m3.pure
        // The count should be > 13 (at least the primitives + parsed M3 elements)
        assert!(
            chunk.nodes.len() >= 13,
            "expected at least 13 elements, got {}",
            chunk.nodes.len()
        );
        assert_eq!(chunk.nodes.len(), chunk.elements.len());
    }

    #[test]
    fn bootstrap_ids_match_names() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));
        assert_eq!(chunk.nodes.get(ANY_ID.local_idx()).name, "Any");
        assert_eq!(chunk.nodes.get(NIL_ID.local_idx()).name, "Nil");
        assert_eq!(chunk.nodes.get(STRING_ID.local_idx()).name, "String");
        assert_eq!(chunk.nodes.get(BOOLEAN_ID.local_idx()).name, "Boolean");
        assert_eq!(chunk.nodes.get(BYTE_ID.local_idx()).name, "Byte");
        assert_eq!(
            chunk.nodes.get(STRICT_TIME_ID.local_idx()).name,
            "StrictTime"
        );
        assert_eq!(chunk.nodes.get(NUMBER_ID.local_idx()).name, "Number");
        assert_eq!(chunk.nodes.get(INTEGER_ID.local_idx()).name, "Integer");
        assert_eq!(chunk.nodes.get(FLOAT_ID.local_idx()).name, "Float");
        assert_eq!(chunk.nodes.get(DECIMAL_ID.local_idx()).name, "Decimal");
        assert_eq!(chunk.nodes.get(DATE_ID.local_idx()).name, "Date");
        assert_eq!(
            chunk.nodes.get(STRICT_DATE_ID.local_idx()).name,
            "StrictDate"
        );
        assert_eq!(chunk.nodes.get(DATE_TIME_ID.local_idx()).name, "DateTime");
    }

    #[test]
    fn any_and_nil_are_classes() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));
        assert!(matches!(
            chunk.elements.get(ANY_ID.local_idx()),
            Element::Class(_)
        ));
        assert!(matches!(
            chunk.elements.get(NIL_ID.local_idx()),
            Element::Class(_)
        ));
    }

    #[test]
    fn primitives_are_primitive_type() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));
        for &(_, id, _) in BOOTSTRAP_PRIMITIVES {
            assert!(
                matches!(
                    chunk.elements.get(id.local_idx()),
                    Element::PrimitiveType(_)
                ),
                "{} should be PrimitiveType",
                chunk.nodes.get(id.local_idx()).name
            );
        }
    }

    #[test]
    fn bootstrap_inheritance_edges() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));

        // Any is a Class with no supertypes
        match chunk.elements.get(ANY_ID.local_idx()) {
            Element::Class(c) => assert!(c.super_types.is_empty()),
            _ => panic!("Any should be a Class"),
        }

        // Nil is a Class with no supertypes (bottom handled by type checker)
        match chunk.elements.get(NIL_ID.local_idx()) {
            Element::Class(c) => assert!(c.super_types.is_empty()),
            _ => panic!("Nil should be a Class"),
        }

        // Helper: extract super_type from a PrimitiveType
        let get_super = |id: ElementId| -> ElementId {
            match chunk.elements.get(id.local_idx()) {
                Element::PrimitiveType(pt) => {
                    pt.super_type.expect("primitive should have a super_type")
                }
                _ => panic!("expected PrimitiveType"),
            }
        };

        // Direct children of Any
        assert_eq!(get_super(STRING_ID), ANY_ID);
        assert_eq!(get_super(BOOLEAN_ID), ANY_ID);
        assert_eq!(get_super(BYTE_ID), ANY_ID);
        assert_eq!(get_super(STRICT_TIME_ID), ANY_ID);
        assert_eq!(get_super(NUMBER_ID), ANY_ID);
        assert_eq!(get_super(DATE_ID), ANY_ID);

        // Numeric subtypes
        assert_eq!(get_super(INTEGER_ID), NUMBER_ID);
        assert_eq!(get_super(FLOAT_ID), NUMBER_ID);
        assert_eq!(get_super(DECIMAL_ID), NUMBER_ID);

        // Temporal subtypes
        assert_eq!(get_super(STRICT_DATE_ID), DATE_ID);
        assert_eq!(get_super(DATE_TIME_ID), DATE_ID);
    }

    #[test]
    fn bootstrap_source_info_is_synthetic() {
        let (chunk, _) = create_bootstrap_chunk(PackageId(0));
        let node = chunk.nodes.get(STRING_ID.local_idx());
        assert_eq!(node.source_info.source.as_str(), "<bootstrap>");
        assert_eq!(node.source_info.start_line, 0);
    }

    #[test]
    fn template_is_cached_across_calls() {
        // The template instance pointer must stabilize after the first call;
        // each subsequent call sees the same OnceLock-stored value.
        let _ = create_bootstrap_chunk(PackageId(0));
        let first_ptr = std::ptr::addr_of!(*TEMPLATE.get().expect("set on first call"));
        let _ = create_bootstrap_chunk(PackageId(0));
        let second_ptr = std::ptr::addr_of!(*TEMPLATE.get().expect("still set"));
        assert_eq!(
            first_ptr, second_ptr,
            "OnceLock should serve the same template on every call"
        );
    }

    #[test]
    fn parent_package_override_takes_effect() {
        // Caller-supplied non-zero root_package must be reflected on every
        // cloned node (the cached template bakes PackageId(0)).
        let (chunk, _) = create_bootstrap_chunk(PackageId(7));
        for idx in 0..chunk.nodes.len() {
            assert_eq!(
                chunk.nodes.get(idx).parent_package,
                PackageId(7),
                "node {idx} should reflect caller's root_package"
            );
        }
    }
}
