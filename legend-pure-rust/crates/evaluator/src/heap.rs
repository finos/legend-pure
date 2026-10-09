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

//! Runtime Heap — Rc-managed storage for Pure object instances.
//!
//! Objects created by `^Class(...)` or `new` expressions are allocated as
//! [`ObjectHandle`]s — `Rc<RefCell<HeapEntry>>` clones. Identity is `Rc::ptr_eq`,
//! mutation is `borrow_mut`, and deallocation is automatic when the last
//! `Value::Object(handle)` clone drops.
//!
//! # Object lifecycles
//!
//! Objects are NOT tracked in any global registry. A handle's lifetime is the
//! lifetime of the strong references that hold it: the variable context, the
//! return chain, captured closures, the memoization cache, or properties of
//! escaped objects. When the last strong reference drops, the entry is freed.
//!
//! The [`RuntimeHeap`] keeps strong refs only for **metamodel bootstrap rows**,
//! one per compiled `PureModel` element. These exist for the evaluator's
//! lifetime so reflection (`Value::Element` ↔ `Value::Object` projection)
//! stays stable.

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_metamodel::ids::{ElementId, PackageId};
use legend_pure_metamodel::model::PureModel;
use smol_str::SmolStr;

use crate::error::PureRuntimeError;
use crate::value::Value;

/// A strong reference to a heap entry.
///
/// `Rc<RefCell<HeapEntry>>` clones share the same underlying entry — identity
/// is `Rc::ptr_eq`. Mutation goes through `borrow_mut()` and panics on
/// re-entrant borrow conflicts at runtime, so callers must keep borrow scopes
/// short and never hold a borrow across a recursive `eval()` call.
pub type ObjectHandle = Rc<RefCell<HeapEntry>>;

/// An object instance with dynamic property storage.
#[derive(Debug)]
pub struct RuntimeObject {
    /// The class that this object is an instance of.
    pub classifier: SmolStr,
    /// Property values, keyed by property name.
    pub properties: HashMap<SmolStr, PVector<Value>>,
    /// When this row was created by [`RuntimeHeap::bootstrap_metamodel`],
    /// the [`ElementId`] of the corresponding compiled element.
    /// `None` for user-allocated objects.
    pub bootstrap_element: Option<ElementId>,
}

impl RuntimeObject {
    /// Create a new object with no properties set and no metamodel binding.
    #[must_use]
    pub fn new(classifier: impl Into<SmolStr>) -> Self {
        Self {
            classifier: classifier.into(),
            properties: HashMap::new(),
            bootstrap_element: None,
        }
    }

    /// Create a metamodel bootstrap row bound to `element_id`.
    #[must_use]
    pub fn new_bootstrap(classifier: impl Into<SmolStr>, element_id: ElementId) -> Self {
        Self {
            classifier: classifier.into(),
            properties: HashMap::new(),
            bootstrap_element: Some(element_id),
        }
    }
}

/// Trait implemented by generated Rust structs for Pure classes.
pub trait TypedObject: 'static {
    /// The Pure class path (e.g., `"my::trading::Trade"`).
    fn classifier_path(&self) -> &str;

    /// Dynamic property access by name.
    fn get_property(&self, name: &str) -> Option<Value>;

    /// Dynamic property mutation (for `mutateAdd` support).
    ///
    /// # Errors
    /// Returns an error if the property name is not recognized or the value type is wrong.
    fn set_property(&mut self, name: &str, value: Value) -> Result<(), PureRuntimeError>;

    /// Downcast to `Any` for type-safe access from compiled code.
    fn as_any(&self) -> &dyn std::any::Any;

    /// Mutable downcast for mutation from compiled code.
    fn as_any_mut(&mut self) -> &mut dyn std::any::Any;
}

/// The storage representation for a single heap object.
pub enum HeapEntry {
    /// Dynamic object — interpreter-created, HashMap-based property storage.
    Dynamic(RuntimeObject),
    /// Typed object — generated struct with direct field access.
    Typed(Box<dyn TypedObject>),
}

impl std::fmt::Debug for HeapEntry {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            HeapEntry::Dynamic(obj) => f.debug_tuple("Dynamic").field(obj).finish(),
            HeapEntry::Typed(obj) => f
                .debug_tuple("Typed")
                .field(&obj.classifier_path())
                .finish(),
        }
    }
}

impl HeapEntry {
    /// The classifier (class path) of this entry as an owned [`SmolStr`].
    #[must_use]
    pub fn classifier(&self) -> SmolStr {
        match self {
            HeapEntry::Dynamic(obj) => obj.classifier.clone(),
            HeapEntry::Typed(obj) => SmolStr::new(obj.classifier_path()),
        }
    }

    /// Borrow the classifier as a `&str`.
    #[must_use]
    pub fn classifier_str(&self) -> &str {
        match self {
            HeapEntry::Dynamic(obj) => obj.classifier.as_str(),
            HeapEntry::Typed(obj) => obj.classifier_path(),
        }
    }

    /// Get a single property value by name.
    ///
    /// # Errors
    /// Returns `PropertyNotFound` if the property does not exist.
    pub fn get_property(&self, name: &str) -> Result<Value, PureRuntimeError> {
        match self {
            HeapEntry::Dynamic(obj) => obj
                .properties
                .get(name)
                .and_then(|v| v.head())
                .cloned()
                .ok_or_else(|| PureRuntimeError::PropertyNotFound {
                    property: name.into(),
                    classifier: obj.classifier.clone(),
                }),
            HeapEntry::Typed(obj) => {
                obj.get_property(name)
                    .ok_or_else(|| PureRuntimeError::PropertyNotFound {
                        property: name.into(),
                        classifier: obj.classifier_path().into(),
                    })
            }
        }
    }

    /// Get all values for a multi-valued property, or an empty vector.
    #[must_use]
    pub fn get_property_values(&self, name: &str) -> PVector<Value> {
        match self {
            HeapEntry::Dynamic(obj) => obj.properties.get(name).cloned().unwrap_or_default(),
            HeapEntry::Typed(obj) => match obj.get_property(name) {
                Some(v) => PVector::unit(v),
                None => PVector::new(),
            },
        }
    }

    /// List the property names currently populated on a dynamic entry.
    #[must_use]
    pub fn property_names(&self) -> Vec<SmolStr> {
        match self {
            HeapEntry::Dynamic(obj) => obj.properties.keys().cloned().collect(),
            HeapEntry::Typed(_) => Vec::new(),
        }
    }

    /// The metamodel `ElementId` this entry was bootstrapped from, if any.
    #[must_use]
    pub fn bootstrap_element(&self) -> Option<ElementId> {
        match self {
            HeapEntry::Dynamic(obj) => obj.bootstrap_element,
            HeapEntry::Typed(_) => None,
        }
    }

    /// Append values to a property (`mutateAdd` semantics).
    ///
    /// # Errors
    /// Returns an error if a typed entry rejects the property name or value type.
    pub fn mutate_add(&mut self, property: &str, values: &[Value]) -> Result<(), PureRuntimeError> {
        match self {
            HeapEntry::Dynamic(obj) => {
                let prop_vec = obj
                    .properties
                    .entry(SmolStr::new(property))
                    .or_insert_with(PVector::new);
                for v in values {
                    prop_vec.push_back(v.clone());
                }
                Ok(())
            }
            HeapEntry::Typed(obj) => {
                for v in values {
                    obj.set_property(property, v.clone())?;
                }
                Ok(())
            }
        }
    }

    /// Replace every value at `property` with `values`.
    ///
    /// # Errors
    /// Returns an error if a typed entry rejects the property name or value type.
    pub fn mutate_set(&mut self, property: &str, values: &[Value]) -> Result<(), PureRuntimeError> {
        match self {
            HeapEntry::Dynamic(obj) => {
                let mut pv = PVector::new();
                for v in values {
                    pv.push_back(v.clone());
                }
                obj.properties.insert(SmolStr::new(property), pv);
                Ok(())
            }
            HeapEntry::Typed(obj) => {
                for v in values {
                    obj.set_property(property, v.clone())?;
                }
                Ok(())
            }
        }
    }

    /// Downcast a typed entry to a concrete struct.
    ///
    /// # Errors
    /// Returns `DowncastFailed` on mismatch.
    pub fn downcast_ref<T: TypedObject + 'static>(&self) -> Result<&T, PureRuntimeError> {
        match self {
            HeapEntry::Typed(obj) => {
                obj.as_any()
                    .downcast_ref::<T>()
                    .ok_or(PureRuntimeError::DowncastFailed {
                        expected: std::any::type_name::<T>(),
                        actual: obj.classifier_path().into(),
                    })
            }
            HeapEntry::Dynamic(obj) => Err(PureRuntimeError::DowncastFailed {
                expected: std::any::type_name::<T>(),
                actual: obj.classifier.clone().into(),
            }),
        }
    }
}

/// Per-evaluator metamodel storage.
///
/// `RuntimeHeap` is a strong-reference arena for the bootstrap metamodel rows
/// only. User objects are NOT tracked here — they live wherever a strong `Rc`
/// holds them. When all those references drop, the object is freed.
///
/// `RuntimeHeap` is **not** `Send` or `Sync` — `Rc` keeps it thread-local.
pub struct RuntimeHeap {
    /// Strong references to bootstrap metamodel rows.
    element_to_object: HashMap<ElementId, ObjectHandle>,
}

impl RuntimeHeap {
    /// Create an empty heap.
    #[must_use]
    pub fn new() -> Self {
        Self {
            element_to_object: HashMap::new(),
        }
    }

    /// Create an empty heap with pre-allocated capacity.
    #[must_use]
    pub fn with_capacity(capacity: usize) -> Self {
        Self {
            element_to_object: HashMap::with_capacity(capacity),
        }
    }

    /// Look up the metamodel handle for a compiled element.
    #[must_use]
    pub fn object_for_element(&self, eid: ElementId) -> Option<ObjectHandle> {
        self.element_to_object.get(&eid).cloned()
    }

    /// Reverse lookup — find the `ElementId` whose metamodel row is `handle`.
    /// O(1) via `RuntimeObject::bootstrap_element`.
    #[must_use]
    pub fn element_for_object(handle: &ObjectHandle) -> Option<ElementId> {
        handle.borrow().bootstrap_element()
    }

    /// Allocate a fresh dynamic handle.
    #[must_use]
    pub fn alloc_dynamic(&mut self, classifier: impl Into<SmolStr>) -> ObjectHandle {
        Rc::new(RefCell::new(HeapEntry::Dynamic(RuntimeObject::new(
            classifier,
        ))))
    }

    /// Allocate a fresh typed handle.
    #[must_use]
    pub fn alloc_typed(&mut self, obj: Box<dyn TypedObject>) -> ObjectHandle {
        Rc::new(RefCell::new(HeapEntry::Typed(obj)))
    }

    /// Pre-populate the heap with one row per compiled `PureModel` element.
    pub fn bootstrap_metamodel(&mut self, model: &PureModel) {
        for chunk in &model.chunks {
            for local_idx in 0..chunk.elements.len() {
                let eid = ElementId::InstanceId {
                    chunk_id: chunk.chunk_id,
                    local_idx,
                };
                if self.element_to_object.contains_key(&eid) {
                    continue;
                }
                let element = model.get_element(eid);
                let classifier = match legend_pure_metamodel::bootstrap::metatype_of(model, element)
                {
                    Some(meta_id) => {
                        crate::model_utils::build_element_path(model, meta_id, "::", false)
                    }
                    None => "meta::pure::metamodel::type::Any".to_owned(),
                };
                let handle = Rc::new(RefCell::new(HeapEntry::Dynamic(
                    RuntimeObject::new_bootstrap(classifier, eid),
                )));
                self.element_to_object.insert(eid, handle);
            }
        }
        for raw_pkg_idx in 0..model.global_packages.len() {
            #[allow(clippy::cast_possible_truncation)]
            let pkg_id = PackageId(raw_pkg_idx);
            let eid = ElementId::Package(pkg_id);
            if self.element_to_object.contains_key(&eid) {
                continue;
            }
            let handle = Rc::new(RefCell::new(HeapEntry::Dynamic(
                RuntimeObject::new_bootstrap("meta::pure::metamodel::type::Package", eid),
            )));
            self.element_to_object.insert(eid, handle);
        }
    }

    /// Iterator over every metamodel handle.
    pub fn iter_metamodel(&self) -> impl Iterator<Item = ObjectHandle> + '_ {
        self.element_to_object.values().cloned()
    }

    /// Iterator over `(handle, classifier)` pairs for every metamodel row.
    pub fn iter_classifiers(&self) -> impl Iterator<Item = (ObjectHandle, SmolStr)> + '_ {
        self.iter_metamodel().map(|h| {
            let cls = h.borrow().classifier();
            (h, cls)
        })
    }

    /// Number of metamodel rows held by this heap.
    #[must_use]
    pub fn metamodel_len(&self) -> usize {
        self.element_to_object.len()
    }

    /// Whether the metamodel arena is empty.
    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.element_to_object.is_empty()
    }

    /// Get a single property value from the entry behind `handle`.
    ///
    /// # Errors
    /// Returns `PropertyNotFound` if the property does not exist.
    pub fn get_property(
        &self,
        handle: &ObjectHandle,
        name: &str,
    ) -> Result<Value, PureRuntimeError> {
        handle.borrow().get_property(name)
    }

    /// Get all values for a multi-valued property.
    ///
    /// # Errors
    /// Currently never returns `Err` — signature preserved for source compatibility.
    pub fn get_property_values(
        &self,
        handle: &ObjectHandle,
        name: &str,
    ) -> Result<PVector<Value>, PureRuntimeError> {
        Ok(handle.borrow().get_property_values(name))
    }

    /// List the property names currently populated.
    ///
    /// # Errors
    /// Currently never returns `Err`.
    pub fn property_names(&self, handle: &ObjectHandle) -> Result<Vec<SmolStr>, PureRuntimeError> {
        Ok(handle.borrow().property_names())
    }

    /// The classifier of the entry behind `handle`.
    ///
    /// # Errors
    /// Currently never returns `Err`.
    pub fn classifier(&self, handle: &ObjectHandle) -> Result<SmolStr, PureRuntimeError> {
        Ok(handle.borrow().classifier())
    }

    /// Append values to a property on the entry behind `handle`.
    ///
    /// # Errors
    /// Returns an error if a typed entry rejects the property name or value.
    pub fn mutate_add(
        &self,
        handle: &ObjectHandle,
        property: &str,
        values: &[Value],
    ) -> Result<(), PureRuntimeError> {
        handle.borrow_mut().mutate_add(property, values)
    }

    /// Replace every value at `property` with `values`.
    ///
    /// # Errors
    /// Returns an error if a typed entry rejects the property name or value.
    pub fn mutate_set(
        &self,
        handle: &ObjectHandle,
        property: &str,
        values: &[Value],
    ) -> Result<(), PureRuntimeError> {
        handle.borrow_mut().mutate_set(property, values)
    }
}

impl Default for RuntimeHeap {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn alloc_and_get_property() {
        let mut heap = RuntimeHeap::new();
        let h = heap.alloc_dynamic("my::Trade");
        h.borrow_mut()
            .mutate_add("price", &[Value::Float(42.0)])
            .unwrap();
        assert_eq!(
            h.borrow().get_property("price").unwrap(),
            Value::Float(42.0)
        );
    }

    #[test]
    fn handle_identity_preserved_across_mutations() {
        let mut heap = RuntimeHeap::new();
        let h = heap.alloc_dynamic("my::Trade");
        let h_clone = h.clone();
        h.borrow_mut()
            .mutate_add("price", &[Value::Float(1.0)])
            .unwrap();
        assert!(Rc::ptr_eq(&h, &h_clone));
        assert_eq!(h_clone.borrow().get_property_values("price").len(), 1);
    }

    #[test]
    fn distinct_allocations_have_distinct_identity() {
        let mut heap = RuntimeHeap::new();
        let a = heap.alloc_dynamic("my::Trade");
        let b = heap.alloc_dynamic("my::Trade");
        assert!(!Rc::ptr_eq(&a, &b));
    }

    #[test]
    fn user_objects_freed_when_no_references_remain() {
        let mut heap = RuntimeHeap::new();
        let weak = {
            let h = heap.alloc_dynamic("my::Trade");
            Rc::downgrade(&h)
        };
        assert!(weak.upgrade().is_none());
        assert_eq!(heap.metamodel_len(), 0);
    }
}
