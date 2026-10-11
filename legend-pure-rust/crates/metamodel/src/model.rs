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

//! The `PureModel` — the compiled semantic graph.
//!
//! This is the Rust equivalent of Java's `PureModel`. It holds:
//! - A **global package tree** (single arena, cross-chunk namespace)
//! - **Chunked element storage** (one [`ModelChunk`] per source file + Chunk 0 for bootstrap)
//! - **Derived indexes** computed post-freeze for reverse lookups
//!
//! # Lifecycle
//!
//! 1. **Mutable phase**: `PureModelBuilder` populates arenas via compiler passes.
//! 2. **Frozen phase**: After `rebuild_derived_indexes()`, the model is read-only.
//!    All query methods operate on the frozen model. Parallel validation is safe.

use std::collections::HashMap;

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::arena::Arena;
use crate::ids::{ElementId, PackageId};
use crate::nodes::association::Association;
use crate::nodes::class::Class;
use crate::nodes::enumeration::Enumeration;
use crate::nodes::function::Function;
use crate::nodes::measure::Measure;
use crate::nodes::profile::Profile;
use crate::nodes::unit::Unit;
use crate::types::PrimitiveType;

/// Common metadata shared by all elements in the graph.
///
/// This is the equivalent of Java's `PackageableElement` — it provides
/// the fields needed for generic traversal (package tree, name resolution)
/// without knowing the concrete element type.
#[derive(Debug, Clone)]
pub struct ElementNode {
    /// The element's unique name.
    ///
    /// For functions this is the **mangled signature**
    /// (e.g., `"plus_Integer_MANY__Integer_1_"`).
    /// For all other elements this is the simple name (e.g., `"Person"`).
    pub name: SmolStr,
    /// Source span covering the entire declaration (`Class … { … }`,
    /// `function … { … }`), from keyword to closing brace.
    pub source_info: SourceInfo,
    /// Source span covering just the element **name** identifier inside
    /// the declaration — the `XTestClass` in
    /// `Class meta::pure::…::XTestClass { … }`, or the
    /// `testFunctionSourceInformation` in
    /// `function <<test.Test>> meta::pure::…::testFunctionSourceInformation(): …`.
    ///
    /// Matches Java Pure's `SourceInformation.line`/`column` (as distinct
    /// from `startLine`/`startColumn`). Synthetic elements (bootstrap,
    /// package shells, M3 parser) reuse `source_info`.
    pub name_source_info: SourceInfo,
    /// The package this element belongs to.
    pub parent_package: PackageId,
}

/// A compiled Pure element with all references resolved to IDs.
///
/// This mirrors `ast::Element` but at the semantic level: string paths are
/// resolved to [`ElementId`]s, type arguments are lowered, and all names
/// are fully qualified.
///
/// **Not all Elements are Types.** `Profile`, `Association`, and `Function`
/// are packageable elements but cannot appear in type positions. Structural
/// types (`FunctionType`, `RelationType`) live in `TypeExpr`, not here.
///
/// See `DESIGN.md` §5 for the Element vs Type distinction.
#[derive(Debug, Clone)]
pub enum Element {
    /// A class with properties, constraints, and type parameters.
    Class(Class),
    /// An enumeration with named values.
    Enumeration(Enumeration),
    /// A top-level function definition.
    Function(Function),
    /// A profile defining stereotypes and tags.
    Profile(Profile),
    /// An association linking two classes.
    Association(Association),
    /// A measure with canonical and non-canonical units.
    Measure(Measure),
    /// A primitive type (String, Integer, etc.). Bootstrapped in Chunk 0.
    PrimitiveType(PrimitiveType),
    /// A unit within a measure (Kilogram, Meter). Each has its own `ElementId`.
    Unit(Unit),
    /// A named multiplicity instance from the M3 metamodel (`PureOne`, `ZeroMany`, etc.).
    ///
    /// These are singleton instances of `PackageableMultiplicity` registered
    /// in `meta::pure::metamodel::multiplicity`. They carry the actual
    /// multiplicity bounds they represent.
    PackageableMultiplicity(crate::types::Multiplicity),
    /// A package (`Package` extends `PackageableElement` in M3).
    ///
    /// The actual package data (name, children) lives in `global_packages`.
    /// This variant carries the `PackageId` so `get_element` works uniformly
    /// for all `ElementId` variants.
    Package(PackageId),

    /// A DSL-defined instance (Diagram, Mapping, Database, …).
    ///
    /// First-class graph citizen for DSL crates: an `Element::DSLInstance`
    /// is allocated in a chunk and registered in its package, just like a
    /// `Class` or `Function`. The payload itself is **opaque** to the Pure compiler —
    /// only the contributing DSL extension knows how to decode `data`.
    ///
    /// `dsl_name` keys the registry (`"Diagram"`, `"Mapping"`,
    /// `"RelationalDatabase"`, …); `classifier_fqn` is the FQN of the M3
    /// metaclass this instance is an instance of (e.g.
    /// `"meta::pure::diagram::Diagram"`); `data` is whatever Postcard-
    /// serialised payload the DSL crate produced.
    ///
    /// Adding a DSL instance to the graph is the alternative to side-car
    /// `extension_arenas` — it inverts the BACKLOG framing: instead of
    /// per-extension `RefCell<HashMap>` state that gets dropped on
    /// slice/merge, DSL data rides through the chunk machinery alongside
    /// every other element.
    DSLInstance(DSLInstance),
}

/// A DSL-defined instance carried opaquely in `Element::DSLInstance`.
///
/// The Pure crate doesn't deserialise `data` — that's the contributing
/// DSL extension's job. To enumerate diagrams, callers walk
/// `model.elements()` filtering on `dsl_name == "Diagram"` and call into
/// `legend_pure_dsl_diagram` to decode each payload.
#[derive(Debug, Clone, PartialEq)]
pub struct DSLInstance {
    /// Stable DSL identifier — `"Diagram"`, `"Mapping"`,
    /// `"RelationalDatabase"`, etc. Matches `ast::DSLElement::kind()`.
    pub dsl_name: SmolStr,

    /// FQN of the M3 metaclass this instance is an instance of, e.g.
    /// `"meta::pure::diagram::Diagram"`. Used for type-system
    /// classification (`getAll(Diagram)` returns these) without forcing
    /// every consumer to decode `data`.
    pub classifier_fqn: SmolStr,

    /// DSL-specific Postcard-encoded payload. Pure-core treats this as
    /// opaque bytes; only the named DSL knows the type.
    pub data: Vec<u8>,
}

/// A node in the global package tree.
///
/// Packages span across chunks — `meta::pure` may contain elements from
/// Chunk 0 (bootstrap), Chunk 1 (user code), and Chunk 2 (plugin code).
#[derive(Debug, Clone)]
pub struct Package {
    /// The package's simple name (e.g., `"pure"` in `meta::pure`).
    pub name: SmolStr,
    /// Parent package, or `None` for the root package.
    pub parent: Option<PackageId>,
    /// Direct child packages.
    pub children_packages: Vec<PackageId>,
    /// Elements in this package (across all chunks).
    pub children_elements: Vec<ElementId>,
}

/// A batch of elements, typically from one parsed source file.
///
/// Chunk 0 is the bootstrap chunk (primitives, `Any`, `Nil`).
/// Each subsequent chunk corresponds to one user source file.
///
/// The `nodes` and `elements` arenas are **parallel** — `nodes[i]` and
/// `elements[i]` describe the same element. `nodes` holds common metadata,
/// `elements` holds the typed payload.
#[derive(Debug, Clone)]
pub struct ModelChunk {
    /// The chunk's unique identifier (0 = bootstrap).
    pub chunk_id: u16,
    /// Common metadata for each element (name, source, package).
    pub nodes: Arena<ElementNode>,
    /// Typed payload for each element (parallel to `nodes`).
    pub elements: Arena<Element>,
}

impl ModelChunk {
    /// Creates a new empty chunk with the given ID.
    #[must_use]
    pub fn new(chunk_id: u16) -> Self {
        Self {
            chunk_id,
            nodes: Arena::new(),
            elements: Arena::new(),
        }
    }

    /// Allocates an element in this chunk, keeping the parallel arenas in sync.
    ///
    /// Returns the `local_idx` that can be used to construct an `ElementId`.
    pub fn alloc_element(&mut self, node: ElementNode, element: Element) -> u32 {
        let node_idx = self.nodes.alloc(node);
        let elem_idx = self.elements.alloc(element);
        debug_assert_eq!(
            node_idx, elem_idx,
            "Parallel arena desync in chunk {}",
            self.chunk_id
        );
        node_idx
    }
}

/// Inverted indexes computed post-freeze in a single O(N) scan.
///
/// Rebuilt after chunk merging.
#[derive(Debug, Clone, Default)]
pub struct DerivedIndexes {
    /// Class → its direct subclasses (from `super_types` edges).
    pub specializations: HashMap<ElementId, Vec<ElementId>>,
    /// Class → association-injected properties targeting it.
    ///
    /// Each entry is `(association_id, property_index)` — a lightweight
    /// reference to the property within the Association's `properties` vec.
    pub association_properties: HashMap<ElementId, Vec<(ElementId, usize)>>,
}

/// The compiled Pure semantic graph — equivalent to Java's `PureModel`.
///
/// Contains the global package tree, chunked element storage, and derived
/// indexes for efficient querying.
#[derive(Debug)]
pub struct PureModel {
    /// Global package tree (single arena, cross-chunk).
    pub global_packages: Arena<Package>,
    /// The root (unnamed) package.
    pub root_package: PackageId,

    /// Chunked element storage. Chunk 0 = bootstrap.
    pub chunks: Vec<ModelChunk>,

    /// Backing `Element::Package` values parallel to `global_packages`.
    /// No data duplication — just `Element::Package(PackageId)` tags
    /// so `get_element` can return `&Element` uniformly.
    package_elements: Vec<Element>,

    /// Derived indexes, computed post-freeze.
    derived: DerivedIndexes,
}

impl PureModel {
    /// Creates a new empty `PureModel` with a root package.
    #[must_use]
    pub fn new() -> Self {
        let mut global_packages = Arena::new();
        let root_idx = global_packages.alloc(Package {
            name: SmolStr::new(""),
            parent: None,
            children_packages: Vec::new(),
            children_elements: Vec::new(),
        });

        Self {
            global_packages,
            root_package: PackageId(root_idx),
            chunks: Vec::new(),
            package_elements: vec![Element::Package(PackageId(root_idx))],
            derived: DerivedIndexes::default(),
        }
    }

    /// Returns the element node for the given ID.
    ///
    /// For `Package` IDs, panics — use [`element_name`](Self::element_name)
    /// or [`get_package`](Self::get_package) instead.
    ///
    /// # Panics
    ///
    /// Panics if the ID is a `Package` or if the chunk/index is out of bounds.
    #[must_use]
    pub fn get_node(&self, id: ElementId) -> &ElementNode {
        match id {
            ElementId::InstanceId {
                chunk_id,
                local_idx,
            } => self.chunks[chunk_id as usize].nodes.get(local_idx),
            ElementId::Package(_) => {
                panic!("get_node called on Package {id} — use element_name or get_package")
            }
        }
    }

    /// Returns the typed element for the given ID.
    ///
    /// Works uniformly for both chunk elements and packages.
    ///
    /// # Panics
    ///
    /// Panics if the chunk/index is out of bounds.
    #[must_use]
    pub fn get_element(&self, id: ElementId) -> &Element {
        match id {
            ElementId::InstanceId {
                chunk_id,
                local_idx,
            } => self.chunks[chunk_id as usize].elements.get(local_idx),
            ElementId::Package(pkg_id) => &self.package_elements[pkg_id.0 as usize],
        }
    }

    /// Returns the typed element for the given ID, or `None` if out of bounds.
    ///
    /// Use this when the ID might reference an unresolved or invalid element.
    #[must_use]
    pub fn try_get_element(&self, id: ElementId) -> Option<&Element> {
        match id {
            ElementId::InstanceId {
                chunk_id,
                local_idx,
            } => self.chunks.get(chunk_id as usize).and_then(|chunk| {
                if local_idx < chunk.elements.len() {
                    Some(chunk.elements.get(local_idx))
                } else {
                    None
                }
            }),
            ElementId::Package(pkg_id) => self.package_elements.get(pkg_id.0 as usize),
        }
    }

    /// Returns the name of any element, including packages.
    ///
    /// Preferred over `get_node(id).name` when the ID might be a Package.
    #[must_use]
    pub fn element_name(&self, id: ElementId) -> &SmolStr {
        match id {
            ElementId::InstanceId { .. } => &self.get_node(id).name,
            ElementId::Package(pkg_id) => &self.global_packages.get(pkg_id.0).name,
        }
    }

    /// Returns the package for the given ID.
    #[must_use]
    pub fn get_package(&self, id: PackageId) -> &Package {
        self.global_packages.get(id.0)
    }

    /// Rebuilds all derived indexes by scanning every element in every chunk.
    ///
    /// Call this after Pass 2 completes and after deserialization.
    /// This is an O(N) scan where N = total number of elements.
    pub fn rebuild_derived_indexes(&mut self) {
        self.derived = DerivedIndexes::default();

        for chunk in &self.chunks {
            for (local_idx, element) in chunk.elements.iter() {
                let id = ElementId::InstanceId {
                    chunk_id: chunk.chunk_id,
                    local_idx,
                };

                match element {
                    Element::Class(class) => {
                        // Register specialization edges (reverse of super_types)
                        for super_type in &class.super_types {
                            if let crate::types::TypeExpr::Named {
                                element: parent_id, ..
                            } = super_type
                            {
                                self.derived
                                    .specializations
                                    .entry(*parent_id)
                                    .or_default()
                                    .push(id);
                            }
                        }
                    }
                    Element::Association(assoc) => {
                        // Register association-injected properties on target classes
                        for (prop_idx, prop) in assoc.properties.iter().enumerate() {
                            if let crate::types::TypeExpr::Named {
                                element: target_id, ..
                            } = &prop.type_expr
                            {
                                self.derived
                                    .association_properties
                                    .entry(*target_id)
                                    .or_default()
                                    .push((id, prop_idx));
                            }
                        }
                    }
                    Element::Enumeration(_)
                    | Element::Function(_)
                    | Element::Profile(_)
                    | Element::Measure(_)
                    | Element::Unit(_)
                    | Element::PrimitiveType(_)
                    | Element::PackageableMultiplicity(_)
                    | Element::Package(_)
                    | Element::DSLInstance(_) => {
                        // DSL instances carry opaque payloads; no
                        // M3-level edges to register here. DSL crates
                        // build their own indexes by walking
                        // `Element::DSLInstance` themselves.
                    }
                }
            }
        }
    }

    /// Returns direct subclasses of the given class (derived index).
    ///
    /// Returns an empty slice if no specializations exist.
    #[must_use]
    pub fn specializations(&self, id: ElementId) -> &[ElementId] {
        self.derived
            .specializations
            .get(&id)
            .map_or(&[], Vec::as_slice)
    }

    /// Returns association-injected properties targeting the given class.
    ///
    /// Each entry is `(association_id, property_index)` into the association's
    /// `properties` vec.
    #[must_use]
    pub fn association_properties(&self, id: ElementId) -> &[(ElementId, usize)] {
        self.derived
            .association_properties
            .get(&id)
            .map_or(&[], Vec::as_slice)
    }

    /// Returns the derived indexes (available only after freeze).
    #[must_use]
    pub fn derived(&self) -> &DerivedIndexes {
        &self.derived
    }

    /// Finds or creates a package by path segments from the root.
    ///
    /// For example, `["meta", "pure"]` returns the `PackageId` for `meta::pure`,
    /// creating intermediate packages as needed.
    pub fn get_or_create_package(&mut self, path: &[SmolStr]) -> PackageId {
        let mut current = self.root_package;
        for segment in path {
            // Check if child already exists
            let existing = self
                .global_packages
                .get(current.0)
                .children_packages
                .iter()
                .find(|&&child_id| self.global_packages.get(child_id.0).name == *segment)
                .copied();

            current = if let Some(id) = existing {
                id
            } else {
                let new_id = PackageId(self.global_packages.alloc(Package {
                    name: segment.clone(),
                    parent: Some(current),
                    children_packages: Vec::new(),
                    children_elements: Vec::new(),
                }));
                self.package_elements.push(Element::Package(new_id));
                self.global_packages
                    .get_mut(current.0)
                    .children_packages
                    .push(new_id);
                new_id
            };
        }
        current
    }

    /// Appends an empty chunk whose id is its index and returns that id.
    ///
    /// # Panics
    /// When the model already holds `u16::MAX` chunks.
    pub fn push_chunk(&mut self) -> u16 {
        let id = u16::try_from(self.chunks.len()).expect("chunk ids are u16");
        self.chunks.push(ModelChunk::new(id));
        id
    }

    /// Pushes empty chunks until `id` exists (chunk ids are indexes).
    pub fn ensure_chunk(&mut self, id: u16) {
        while self.chunks.len() <= usize::from(id) {
            self.push_chunk();
        }
    }

    /// Number of elements currently allocated in a chunk, 0 when the
    /// chunk does not exist yet.
    #[must_use]
    pub fn chunk_len(&self, chunk_id: u16) -> u32 {
        self.chunks
            .get(usize::from(chunk_id))
            .map_or(0, |c| c.nodes.len())
    }

    /// Allocates an element in a chunk (created on demand) and returns
    /// its full id. The caller registers it in a package separately.
    pub fn alloc_in_chunk(
        &mut self,
        chunk_id: u16,
        node: ElementNode,
        element: Element,
    ) -> ElementId {
        self.ensure_chunk(chunk_id);
        let local_idx = self.chunks[usize::from(chunk_id)].alloc_element(node, element);
        ElementId::InstanceId {
            chunk_id,
            local_idx,
        }
    }

    /// Registers an element in a package's children list.
    pub fn register_element(&mut self, package: PackageId, element: ElementId) {
        self.global_packages
            .get_mut(package.0)
            .children_elements
            .push(element);
    }

    /// Resolves a `::`-qualified FQN string directly to an [`ElementId`].
    ///
    /// Equivalent to `resolve_by_path` but walks the segments inline,
    /// without allocating a `Vec<SmolStr>` or per-segment `SmolStr`s.
    /// Use this on hot paths that already have the FQN as a `&str`
    /// (classifier dispatch, m3 path constants).
    ///
    /// Returns `None` for the empty string or when any segment doesn't
    /// resolve.
    #[must_use]
    pub fn resolve_fqn_str(&self, fqn: &str) -> Option<ElementId> {
        if fqn.is_empty() {
            return None;
        }
        // Iterate `::`-separated segments without materialising them
        // into a `Vec`. Hold the previous segment in a local until we
        // see the next, so the loop body always operates on an
        // intermediate (package) segment and the final segment falls
        // out at the end as the leaf name.
        let mut iter = fqn.split("::");
        let mut prev = iter.next()?;
        let mut current = self.root_package;
        for next in iter {
            // `prev` is an intermediate package segment.
            let pkg = self.get_package(current);
            current = *pkg
                .children_packages
                .iter()
                .find(|&&child_id| self.global_packages.get(child_id.0).name.as_str() == prev)?;
            prev = next;
        }
        // `prev` is the leaf (element) name.
        let pkg = self.get_package(current);
        pkg.children_elements
            .iter()
            .find(|&&eid| self.get_node(eid).name.as_str() == prev)
            .copied()
    }

    /// Resolves a fully qualified name to an `ElementId` by walking the package tree.
    ///
    /// Returns `None` if the name doesn't resolve.
    #[must_use]
    pub fn resolve_by_path(&self, path: &[SmolStr]) -> Option<ElementId> {
        if path.is_empty() {
            return None;
        }

        // Walk to the parent package
        let (pkg_path, name) = path.split_at(path.len() - 1);
        let mut current = self.root_package;

        for segment in pkg_path {
            let pkg = self.get_package(current);
            current = *pkg
                .children_packages
                .iter()
                .find(|&&child_id| self.global_packages.get(child_id.0).name == *segment)?;
        }

        // Find element by name in the target package
        let pkg = self.get_package(current);
        let target_name = &name[0];
        pkg.children_elements
            .iter()
            .find(|&&eid| self.get_node(eid).name == *target_name)
            .copied()
    }

    /// Finds all function elements in a package matching a simple name.
    ///
    /// Returns the overload candidates — functions whose `function_name`
    /// matches the given name. Used by unqualified resolution for function
    /// dispatch.
    #[must_use]
    pub fn resolve_functions_by_name_in_package(
        &self,
        pkg_id: PackageId,
        name: &SmolStr,
    ) -> Vec<ElementId> {
        let pkg = self.get_package(pkg_id);
        pkg.children_elements
            .iter()
            .filter(|&&eid| {
                if let Element::Function(f) = self.get_element(eid) {
                    f.function_name == *name
                } else {
                    false
                }
            })
            .copied()
            .collect()
    }

    /// Walks every package in the model and returns every function
    /// whose `function_name` matches `name`. Used at infer-time as a
    /// fallback when lower-time dispatch couldn't pick a unique
    /// candidate (e.g. lambda body referenced a function while the
    /// receiver/arg was still typed `TypeExpr::Unresolved`) and a
    /// later binding pass needs to re-narrow with the freshly bound
    /// generics — see the archived compiler's lambda inference.
    ///
    /// O(model size). Cheap enough for the redispatch path (one walk
    /// per Unresolved-cause FunctionCall encountered during a single
    /// lambda body's pass-2 binding); if the call volume grows we can
    /// add a per-simple-name index, but the current count is in the
    /// low dozens per platform compile.
    #[must_use]
    pub fn resolve_functions_by_simple_name_globally(&self, name: &SmolStr) -> Vec<ElementId> {
        let mut out: Vec<ElementId> = Vec::new();
        for (_, pkg) in self.global_packages.iter() {
            for &eid in &pkg.children_elements {
                if let Element::Function(f) = self.get_element(eid)
                    && f.function_name == *name
                {
                    out.push(eid);
                }
            }
        }
        out
    }

    /// Resolves a function by its simple name within a package path.
    ///
    /// After Pass 2.1 (name mangling), function element names are mangled FQNs
    /// (e.g., `"greet_String_1__String_1_"`). This method matches by prefix:
    /// the last segment of `path` is treated as a simple function name, and the
    /// first function whose mangled name starts with `"name_"` is returned.
    ///
    /// Returns `None` if no matching function is found.
    #[must_use]
    pub fn resolve_function_by_path(&self, path: &[SmolStr]) -> Option<ElementId> {
        if path.is_empty() {
            return None;
        }

        // Walk to the parent package
        let (pkg_path, name) = path.split_at(path.len() - 1);
        let mut current = self.root_package;

        for segment in pkg_path {
            let pkg = self.get_package(current);
            current = *pkg
                .children_packages
                .iter()
                .find(|&&child_id| self.global_packages.get(child_id.0).name == *segment)?;
        }

        // Find function element whose mangled name starts with "simpleName_"
        let simple = &name[0];
        let prefix = format!("{simple}_");
        let pkg = self.get_package(current);
        pkg.children_elements
            .iter()
            .find(|&&eid| {
                let node_name = &self.get_node(eid).name;
                node_name.starts_with(&prefix)
            })
            .copied()
    }

    /// Resolves an element by its exact mangled FQN within a package path.
    ///
    /// The last segment of `path` is the exact mangled function name
    /// (e.g., `"f__Integer_1_"`), and preceding segments are the
    /// package path (e.g., `["test", "f__Integer_1_"]`).
    ///
    /// This is the correct lookup method for FQN-based dispatch where
    /// the mangled name is known.
    ///
    /// Returns `None` if no matching element is found.
    #[must_use]
    pub fn resolve_by_fqn(&self, path: &[SmolStr]) -> Option<ElementId> {
        if path.is_empty() {
            return None;
        }

        let (pkg_path, name) = path.split_at(path.len() - 1);
        let mut current = self.root_package;

        for segment in pkg_path {
            let pkg = self.get_package(current);
            current = *pkg
                .children_packages
                .iter()
                .find(|&&child_id| self.global_packages.get(child_id.0).name == *segment)?;
        }

        let fqn = &name[0];
        let pkg = self.get_package(current);
        pkg.children_elements
            .iter()
            .find(|&&eid| self.get_node(eid).name == *fqn)
            .copied()
    }
}

impl Default for PureModel {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::nodes::class;

    /// A chunk appended after the bootstrap takes the next index as its
    /// id, grows by `alloc_in_chunk`, and its elements resolve by path
    /// once registered.
    #[test]
    fn chunks_append_and_allocate_by_id() {
        let mut model = PureModel::bootstrapped();
        let first = model.chunks.len();
        assert_eq!(model.chunk_len(u16::try_from(first).unwrap()), 0);
        let chunk = model.push_chunk();
        assert_eq!(usize::from(chunk), first);
        let pkg = model.get_or_create_package(&[SmolStr::new("my"), SmolStr::new("pkg")]);
        let id = model.alloc_in_chunk(
            chunk,
            ElementNode {
                name: SmolStr::new("Thing"),
                source_info: SourceInfo::new("<test>", 0, 0, 0, 0),
                name_source_info: SourceInfo::new("<test>", 0, 0, 0, 0),
                parent_package: pkg,
            },
            Element::PackageableMultiplicity(crate::types::Multiplicity::PureOne),
        );
        model.register_element(pkg, id);
        model.rebuild_derived_indexes();
        assert_eq!(model.chunk_len(chunk), 1);
        assert_eq!(
            id,
            ElementId::InstanceId {
                chunk_id: chunk,
                local_idx: 0
            }
        );
        assert_eq!(model.resolve_fqn_str("my::pkg::Thing"), Some(id));
        // `ensure_chunk` past the end pads with empty chunks.
        model.ensure_chunk(chunk + 2);
        assert_eq!(model.chunks.len(), usize::from(chunk) + 3);
        assert_eq!(model.chunk_len(chunk + 2), 0);
    }

    /// Documents the memory cost of `ElementNode`'s two parallel
    /// `SourceInfo` fields (`source_info` + `name_source_info`). The
    /// `Cleaner SourceInformation name-span representation` BACKLOG
    /// item (Compiler > Open Work) gates its refactor decision on
    /// this measurement.
    ///
    /// Numbers are also captured in BACKLOG.md's entry; this test
    /// keeps them tied to the source by running on every CI build —
    /// the assertions fail if `SourceInfo` or `ElementNode` grow,
    /// so the BACKLOG entry can be re-read for a stale-numbers
    /// audit. Update both together.
    #[test]
    fn element_node_size_for_source_info_audit() {
        let si_size = std::mem::size_of::<SourceInfo>();
        let en_size = std::mem::size_of::<ElementNode>();
        let smolstr_size = std::mem::size_of::<smol_str::SmolStr>();

        // Print so `cargo test -- --nocapture` reports them cleanly.
        eprintln!("== ElementNode memory audit ==");
        eprintln!("  sizeof(SmolStr)       = {smolstr_size}");
        eprintln!("  sizeof(SourceInfo)    = {si_size}");
        eprintln!("  sizeof(ElementNode)   = {en_size}");
        eprintln!();
        eprintln!("Per-element savings if `name_source_info` field");
        eprintln!("collapses into `source_info` (Java-7-field shape):");
        for n in [1_660usize, 10_000, 100_000, 1_000_000] {
            let cur = en_size * n;
            let after = (en_size - si_size) * n;
            let saved = si_size * n;
            eprintln!("  N={n:>9}: {cur:>11} → {after:>11} bytes (saved {saved:>10})");
        }

        // Regression assertions — if SourceInfo or ElementNode grow
        // beyond the recorded baseline, surface a noisy test failure
        // so BACKLOG numbers can be refreshed deliberately rather
        // than drifting silently. SmolStr is 24 bytes on every
        // current target; SourceInfo lays out as
        // `{ SmolStr (24), u32 × 4 (16) } = 40` with no padding.
        assert_eq!(smolstr_size, 24, "SmolStr footprint changed");
        assert_eq!(si_size, 40, "SourceInfo footprint changed");
        // ElementNode: SmolStr (24) + SourceInfo × 2 (80) +
        // PackageId (4 — single u32) + padding to next align = 112.
        assert_eq!(en_size, 112, "ElementNode footprint changed");
    }

    fn test_source() -> SourceInfo {
        SourceInfo::new("test.pure", 1, 1, 1, 10)
    }

    fn empty_class() -> Class {
        class::Class {
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
        }
    }

    #[test]
    fn new_model_has_root_package() {
        let model = PureModel::new();
        let root = model.get_package(model.root_package);
        assert_eq!(root.name, "");
        assert!(root.parent.is_none());
        assert!(root.children_packages.is_empty());
        assert!(root.children_elements.is_empty());
    }

    #[test]
    fn model_chunk_parallel_arenas() {
        let mut chunk = ModelChunk::new(1);
        let node_idx = chunk.nodes.alloc(ElementNode {
            name: SmolStr::new("Person"),
            source_info: test_source(),
            name_source_info: test_source(),
            parent_package: PackageId(0),
        });
        let elem_idx = chunk.elements.alloc(Element::Class(empty_class()));

        // Parallel: same index in both arenas
        assert_eq!(node_idx, elem_idx);
        assert_eq!(chunk.nodes.get(node_idx).name, "Person");
        assert!(matches!(chunk.elements.get(elem_idx), Element::Class(_)));
    }

    #[test]
    fn model_get_element_and_node() {
        let mut model = PureModel::new();
        let mut chunk = ModelChunk::new(0);
        chunk.nodes.alloc(ElementNode {
            name: SmolStr::new("String"),
            source_info: test_source(),
            name_source_info: test_source(),
            parent_package: model.root_package,
        });
        chunk.elements.alloc(Element::PrimitiveType(PrimitiveType {
            super_type: None,
            super_type_value_arguments: Vec::new(),
            type_variable_parameters: Vec::new(),
            constraints: Vec::new(),
        }));
        model.chunks.push(chunk);

        let id = ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 0,
        };
        assert_eq!(model.get_node(id).name, "String");
        assert!(matches!(model.get_element(id), Element::PrimitiveType(_)));
    }

    #[test]
    fn model_default_is_empty() {
        let model = PureModel::default();
        assert!(model.chunks.is_empty());
        assert!(model.derived().specializations.is_empty());
    }

    #[test]
    fn model_with_bootstrap_chunk() {
        let mut model = PureModel::new();
        let (bootstrap, _) = crate::bootstrap::create_bootstrap_chunk(model.root_package);
        model.chunks.push(bootstrap);

        // Verify we can look up primitives by well-known IDs
        assert_eq!(model.get_node(crate::bootstrap::STRING_ID).name, "String");
        assert_eq!(model.get_node(crate::bootstrap::ANY_ID).name, "Any");
        assert!(matches!(
            model.get_element(crate::bootstrap::INTEGER_ID),
            Element::PrimitiveType(_)
        ));
    }
}
