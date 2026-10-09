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

//! Reusable helpers that operate on a [`PureModel`] — element path
//! construction, package-qualified path resolution, simple-name lookup.
//!
//! These functions back the `elementToPath` / `pathToElement` natives but
//! are equally useful to the evaluator, test-surveyor scaffolding, and
//! CLI diagnostics. Exposing them here avoids having every caller reach
//! into `native::meta` (which is about *Pure-native* dispatch, not
//! general model navigation).

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use smol_str::SmolStr;

/// The element's simple, human-friendly name.
///
/// - `Element::Function` → the unmangled `function_name` (so overloads
///   collapse to a single display name).
/// - Packages → the package's own `name`.
/// - Everything else → [`ElementNode::name`](legend_pure_metamodel::model::ElementNode::name).
#[must_use]
pub fn element_simple_name(model: &PureModel, id: ElementId) -> &SmolStr {
    if let Element::Function(f) = model.get_element(id) {
        return &f.function_name;
    }
    model.element_name(id)
}

/// Build the `separator`-joined qualified path for an element.
///
/// For functions, uses the simple `function_name` (not the mangled form).
/// When `include_root == true` the anonymous root package is rendered as
/// the literal `"Root"` — matching the Java Pure runtime. When
/// `include_root == false`, the root segment is dropped entirely so a
/// top-level element like `Package` renders as `"Package"`.
///
/// Special cases:
/// - `id` is the root package itself:
///   - `include_root == true` → `"Root"`.
///   - `include_root == false` → `""` (empty string, not a literal join).
/// - Top-level elements (direct children of the root) never get a
///   `"Root"` prefix, even with `include_root == true` — matching the
///   Java Pure convention.
#[must_use]
pub fn build_element_path(
    model: &PureModel,
    id: ElementId,
    separator: &str,
    include_root: bool,
) -> String {
    // Calling elementToPath on the root package itself.
    if let ElementId::Package(pkg_id) = id
        && model.get_package(pkg_id).parent.is_none()
    {
        return if include_root {
            "Root".to_string()
        } else {
            String::new()
        };
    }

    let mut segments: Vec<SmolStr> = Vec::new();
    segments.push(element_simple_name(model, id).clone());

    let mut parent_pkg = match id {
        ElementId::Package(pkg_id) => model.get_package(pkg_id).parent,
        ElementId::InstanceId { .. } => Some(model.get_node(id).parent_package),
    };

    // Collect intermediate package names (everything between the element
    // and the root). Whether to prepend "Root" is decided only once we
    // know if there is at least one intermediate segment — top-level
    // elements stay root-less.
    let mut intermediate_packages: Vec<SmolStr> = Vec::new();
    while let Some(pkg_id) = parent_pkg {
        let pkg = model.get_package(pkg_id);
        let is_root = pkg.parent.is_none();
        if is_root {
            if include_root && !intermediate_packages.is_empty() {
                intermediate_packages.push(SmolStr::new("Root"));
            }
            break;
        }
        intermediate_packages.push(pkg.name.clone());
        parent_pkg = pkg.parent;
    }
    segments.extend(intermediate_packages);

    segments.reverse();
    segments
        .iter()
        .map(SmolStr::as_str)
        .collect::<Vec<_>>()
        .join(separator)
}

/// Resolve a `separator`-qualified path to an [`ElementId`].
///
/// Walks all segments but the last as packages, then tries:
/// 1. A child element by simple name (function `function_name`, not
///    mangled) — the common case.
/// 2. A child element by mangled name — so overload-specific paths like
///    `meta::pure::functions::boolean::greaterThan_Number_1__Number_1__Boolean_1_`
///    resolve to the specific overload.
/// 3. A child package with that name.
///
/// Root-package sentinels:
/// - Empty string, `"Root"`, or a path equal to the separator (e.g.
///   `"::"` with separator `"::"`, or `"."` with separator `"."`) all
///   resolve to the root package.
///
/// Returns `None` if any segment fails to resolve.
#[must_use]
pub fn resolve_path(model: &PureModel, path: &str, separator: &str) -> Option<ElementId> {
    if path.is_empty() || path == "Root" || (!separator.is_empty() && path == separator) {
        return Some(ElementId::Package(model.root_package));
    }

    let segments: Vec<&str> = if separator.is_empty() {
        vec![path]
    } else {
        path.split(separator).collect()
    };
    if segments.is_empty() {
        return None;
    }

    let mut current = model.root_package;
    for segment in &segments[..segments.len() - 1] {
        let pkg = model.get_package(current);
        let next = pkg
            .children_packages
            .iter()
            .find(|&&child_id| model.get_package(child_id).name == *segment)?;
        current = *next;
    }

    let last = segments[segments.len() - 1];
    let pkg = model.get_package(current);

    for &eid in &pkg.children_elements {
        if element_simple_name(model, eid) == last {
            return Some(eid);
        }
    }
    for &eid in &pkg.children_elements {
        if model.element_name(eid) == last {
            return Some(eid);
        }
    }

    pkg.children_packages
        .iter()
        .find(|&&child_id| model.get_package(child_id).name == *last)
        .map(|&child_id| ElementId::Package(child_id))
}
