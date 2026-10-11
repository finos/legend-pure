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

//! Rust objects a host holds, by a stable integer.

use std::cell::RefCell;
use std::collections::HashMap;

use slotmap::{SlotMap, new_key_type};

use crate::heap::{HeapEntry, ObjectHandle};

new_key_type! {
    /// Key of a Rust heap object exposed to a host.
    pub struct Handle;
}

/// Rust objects exposed to a host: one slot per `Rc`, idempotent.
pub struct HandleTable {
    live: SlotMap<Handle, ObjectHandle>,
    by_ptr: HashMap<*const RefCell<HeapEntry>, Handle>,
}

impl HandleTable {
    /// An empty table.
    #[must_use]
    pub fn new() -> Self {
        Self {
            live: SlotMap::with_key(),
            by_ptr: HashMap::new(),
        }
    }

    /// Register a handle and return its stable key. Idempotent: the same
    /// `Rc` pointer always maps to the same key, so one object keeps one
    /// id on the host's side.
    pub fn register(&mut self, handle: ObjectHandle) -> Handle {
        let key_ptr = std::rc::Rc::as_ptr(&handle);
        if let Some(&existing) = self.by_ptr.get(&key_ptr) {
            return existing;
        }
        let h = self.live.insert(handle);
        self.by_ptr.insert(key_ptr, h);
        h
    }

    /// Look up the object for a key; `None` once released.
    #[must_use]
    pub fn lookup(&self, h: Handle) -> Option<&ObjectHandle> {
        self.live.get(h)
    }

    /// Number of live entries.
    #[must_use]
    pub fn live_count(&self) -> usize {
        self.live.len()
    }

    /// Drop the strong reference for a key. Re-releasing is a no-op.
    pub fn release(&mut self, h: Handle) {
        if let Some(handle) = self.live.remove(h) {
            self.by_ptr.remove(&std::rc::Rc::as_ptr(&handle));
        }
    }

    /// The integer a host holds for a key.
    #[allow(clippy::cast_possible_wrap)]
    #[must_use]
    pub fn to_i64(h: Handle) -> i64 {
        slotmap::Key::data(&h).as_ffi() as i64
    }

    /// The key behind an integer a host handed back.
    #[allow(clippy::cast_sign_loss)]
    #[must_use]
    pub fn from_i64(v: i64) -> Handle {
        Handle::from(slotmap::KeyData::from_ffi(v as u64))
    }
}

impl Default for HandleTable {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::heap::RuntimeObject;
    use std::rc::Rc;

    fn object() -> ObjectHandle {
        Rc::new(RefCell::new(HeapEntry::Dynamic(RuntimeObject::new("A"))))
    }

    #[test]
    fn one_object_keeps_one_integer_until_released() {
        let mut table = HandleTable::new();
        let object = object();
        let first = HandleTable::to_i64(table.register(object.clone()));
        let again = HandleTable::to_i64(table.register(object.clone()));
        assert_eq!(first, again);
        assert_eq!(table.live_count(), 1);
        table.release(HandleTable::from_i64(first));
        table.release(HandleTable::from_i64(first));
        assert_eq!(table.live_count(), 0);
        assert!(table.lookup(HandleTable::from_i64(first)).is_none());
        let fresh = HandleTable::to_i64(table.register(object));
        assert_ne!(first, fresh, "a released integer is never reused");
    }
}
