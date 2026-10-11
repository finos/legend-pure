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

//! A read view over one node of another graph.

use std::cell::RefCell;
use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::heap::{HeapEntry, ObjectHandle};
use crate::value::Value;

/// Identity of a node for the lowering's caches: the canonical heap
/// handle (the bridge keeps one handle per Java object).
pub type NodeKey = *const RefCell<HeapEntry>;

/// One node, read through the heap's object view.
#[derive(Clone)]
pub struct Node(ObjectHandle);

impl Node {
    /// Wrap a handle.
    #[must_use]
    pub fn new(handle: ObjectHandle) -> Self {
        Self(handle)
    }

    /// The handle.
    #[must_use]
    pub fn handle(&self) -> &ObjectHandle {
        &self.0
    }

    /// Cache key.
    #[must_use]
    pub fn key(&self) -> NodeKey {
        Rc::as_ptr(&self.0)
    }

    /// Classifier user path.
    #[must_use]
    pub fn classifier(&self) -> SmolStr {
        self.0.borrow().classifier()
    }

    /// The node's own name (`getName()`), when the graph gives one.
    #[must_use]
    pub fn name(&self) -> Option<SmolStr> {
        match &*self.0.borrow() {
            HeapEntry::Typed(obj) => obj.node_name(),
            HeapEntry::Dynamic(_) => None,
        }
    }

    /// User path when the node is a packageable element.
    #[must_use]
    pub fn user_path(&self) -> Option<SmolStr> {
        match &*self.0.borrow() {
            HeapEntry::Typed(obj) => obj.user_path(),
            HeapEntry::Dynamic(_) => None,
        }
    }

    /// Source span, when known.
    #[must_use]
    pub fn source(&self) -> Option<SourceInfo> {
        match &*self.0.borrow() {
            HeapEntry::Typed(obj) => obj.source_info(),
            HeapEntry::Dynamic(_) => None,
        }
    }

    /// The span to put on what this node lowers to.
    #[must_use]
    pub fn span(&self) -> SourceInfo {
        self.source()
            .unwrap_or_else(|| SourceInfo::new("<lowered>", 0, 0, 0, 0))
    }

    /// Captured values of an open variable (see
    /// [`TypedObject::captured_values`](crate::heap::TypedObject::captured_values)).
    #[must_use]
    pub fn captured_values(&self, name: &str) -> Option<PVector<Value>> {
        match &*self.0.borrow() {
            HeapEntry::Typed(obj) => obj.captured_values(name),
            HeapEntry::Dynamic(_) => None,
        }
    }

    /// Every value of a property.
    #[must_use]
    pub fn prop(&self, name: &str) -> PVector<Value> {
        self.0.borrow().get_property_values(name)
    }

    /// The first value of a property.
    #[must_use]
    pub fn one(&self, name: &str) -> Option<Value> {
        self.prop(name).head().cloned()
    }

    /// The first value of a property as a string.
    #[must_use]
    pub fn string(&self, name: &str) -> Option<SmolStr> {
        match self.one(name).as_ref() {
            Some(Value::String(s)) => Some(s.clone()),
            _ => None,
        }
    }

    /// The first value of a property as a node.
    #[must_use]
    pub fn node(&self, name: &str) -> Option<Node> {
        match self.one(name).as_ref() {
            Some(Value::Object(h)) => Some(Node::new(h.clone())),
            _ => None,
        }
    }

    /// Every value of a property that is a node.
    #[must_use]
    pub fn nodes(&self, name: &str) -> Vec<Node> {
        self.prop(name)
            .iter()
            .filter_map(|v| match v {
                Value::Object(h) => Some(Node::new(h.clone())),
                _ => None,
            })
            .collect()
    }
}

impl std::fmt::Debug for Node {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Node")
            .field("classifier", &self.classifier())
            .field("name", &self.name())
            .finish_non_exhaustive()
    }
}
