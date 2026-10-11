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

//! The elements a lowering adds to the model, with their ids assigned
//! before the model changes.

use std::collections::HashMap;

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, ElementNode, PureModel};
use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

use super::NodeKey;

struct Pending {
    package: Vec<SmolStr>,
    name: SmolStr,
    source: SourceInfo,
    element: Option<Element>,
}

/// Elements collected by a lowering, to be appended to one chunk of the
/// model in one go. Ids are assigned at [`reserve`](Self::reserve) time,
/// so an element can reference itself or a sibling before either is
/// filled; [`apply`](Self::apply) is the only place the model changes.
pub struct ModelExtension {
    chunk_id: u16,
    base_idx: u32,
    pending: Vec<Pending>,
    by_path: HashMap<SmolStr, ElementId>,
    by_node: HashMap<NodeKey, ElementId>,
}

impl ModelExtension {
    /// An empty extension for `chunk_id`, whose first element gets index
    /// `base_idx` (the chunk's current length).
    #[must_use]
    pub fn new(chunk_id: u16, base_idx: u32) -> Self {
        Self {
            chunk_id,
            base_idx,
            pending: Vec::new(),
            by_path: HashMap::new(),
            by_node: HashMap::new(),
        }
    }

    /// The chunk the elements go to.
    #[must_use]
    pub fn chunk_id(&self) -> u16 {
        self.chunk_id
    }

    /// Nothing to add.
    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.pending.is_empty()
    }

    /// Number of elements collected.
    #[must_use]
    pub fn len(&self) -> usize {
        self.pending.len()
    }

    /// Reserve the id of an element at `path` (`a::b::Name`; for a
    /// function the leaf is its mangled name) before its payload exists.
    ///
    /// # Panics
    /// More than `u32::MAX` elements in one chunk.
    pub fn reserve(
        &mut self,
        key: NodeKey,
        path: &str,
        name: SmolStr,
        source: SourceInfo,
    ) -> ElementId {
        let local_idx = self.base_idx + u32::try_from(self.pending.len()).expect("chunk size");
        let id = ElementId::InstanceId {
            chunk_id: self.chunk_id,
            local_idx,
        };
        let mut package: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
        package.pop();
        self.pending.push(Pending {
            package,
            name,
            source,
            element: None,
        });
        self.by_path.insert(SmolStr::new(path), id);
        self.by_node.insert(key, id);
        id
    }

    /// Provide the payload of a reserved element.
    ///
    /// # Panics
    /// `id` was not reserved by this extension.
    pub fn fill(&mut self, id: ElementId, element: Element) {
        let slot = self.slot(id).expect("fill: the id was not reserved here");
        self.pending[slot].element = Some(element);
    }

    /// The id reserved for `path`, if any.
    #[must_use]
    pub fn lookup(&self, path: &str) -> Option<ElementId> {
        self.by_path.get(path).copied()
    }

    /// The id reserved for a node, if any.
    #[must_use]
    pub fn lookup_node(&self, key: NodeKey) -> Option<ElementId> {
        self.by_node.get(&key).copied()
    }

    /// The payload of a pending element, once filled.
    #[must_use]
    pub fn pending_element(&self, id: ElementId) -> Option<&Element> {
        self.slot(id).and_then(|i| self.pending[i].element.as_ref())
    }

    /// Reserved elements whose payload has not arrived yet.
    #[must_use]
    pub fn unfilled(&self) -> usize {
        self.pending.iter().filter(|p| p.element.is_none()).count()
    }

    fn slot(&self, id: ElementId) -> Option<usize> {
        match id {
            ElementId::InstanceId {
                chunk_id,
                local_idx,
            } if chunk_id == self.chunk_id && local_idx >= self.base_idx => {
                let slot = (local_idx - self.base_idx) as usize;
                (slot < self.pending.len()).then_some(slot)
            }
            _ => None,
        }
    }

    /// Append every element to the model, in reservation order, and
    /// rebuild the derived indexes. Requires that the model's chunk is
    /// still at `base_idx` and that every reserved element was filled.
    ///
    /// # Panics
    /// An element was reserved but never filled (a lowering that failed
    /// must drop its extension instead), or the chunk grew meanwhile.
    pub fn apply(self, model: &mut PureModel) {
        if self.pending.is_empty() {
            return;
        }
        assert_eq!(
            model.chunk_len(self.chunk_id),
            self.base_idx,
            "the chunk grew while this extension was being built"
        );
        for (i, pending) in self.pending.into_iter().enumerate() {
            let element = pending
                .element
                .unwrap_or_else(|| panic!("element {} reserved but never filled", pending.name));
            let package = model.get_or_create_package(&pending.package);
            let id = model.alloc_in_chunk(
                self.chunk_id,
                ElementNode {
                    name: pending.name,
                    source_info: pending.source.clone(),
                    name_source_info: pending.source,
                    parent_package: package,
                },
                element,
            );
            debug_assert_eq!(
                id,
                ElementId::InstanceId {
                    chunk_id: self.chunk_id,
                    local_idx: self.base_idx + u32::try_from(i).expect("chunk size"),
                }
            );
            model.register_element(package, id);
        }
        model.rebuild_derived_indexes();
    }
}
