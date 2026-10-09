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

//! A generic arena container for the Pure semantic graph.
//!
//! An [`Arena<T>`] is a flat `Vec<T>` that allocates items sequentially and
//! returns their index. Items are never removed — the arena grows monotonically
//! during the mutable build phase and becomes read-only after freezing.
//!
//! This is the core storage primitive behind [`ModelChunk`](crate::model::ModelChunk)
//! and the global package tree.

use std::ops::Index;

/// A flat, append-only container that assigns sequential indices to items.
///
/// # Design
///
/// - Items are stored in a contiguous `Vec<T>` for cache-friendly iteration.
/// - Allocation returns a `u32` index (not a reference), avoiding borrow issues.
/// - Items are never removed — the arena grows during the build phase and
///   becomes effectively read-only after freezing.
///
/// # Examples
///
/// ```
/// use legend_pure_metamodel::arena::Arena;
///
/// let mut arena = Arena::new();
/// let idx = arena.alloc("hello".to_string());
/// assert_eq!(arena.get(idx), &"hello".to_string());
/// assert_eq!(arena.len(), 1);
/// ```
#[derive(Debug, Clone)]
pub struct Arena<T> {
    items: Vec<T>,
}

impl<T> Arena<T> {
    /// Creates a new empty arena.
    #[must_use]
    pub fn new() -> Self {
        Self { items: Vec::new() }
    }

    /// Creates a new arena with the given pre-allocated capacity.
    #[must_use]
    pub fn with_capacity(capacity: usize) -> Self {
        Self {
            items: Vec::with_capacity(capacity),
        }
    }

    /// Allocates a new item in the arena, returning its index.
    ///
    /// # Panics
    ///
    /// Panics if the arena exceeds `u32::MAX` items.
    pub fn alloc(&mut self, item: T) -> u32 {
        let idx = self.items.len();
        assert!(
            u32::try_from(idx).is_ok(),
            "Arena overflow: exceeded u32::MAX items"
        );
        self.items.push(item);
        #[allow(clippy::cast_possible_truncation)] // Guarded by assert above
        let result = idx as u32;
        result
    }

    /// Returns a reference to the item at the given index.
    ///
    /// # Panics
    ///
    /// Panics if `idx` is out of bounds.
    #[must_use]
    pub fn get(&self, idx: u32) -> &T {
        &self.items[idx as usize]
    }

    /// Returns a mutable reference to the item at the given index.
    ///
    /// # Panics
    ///
    /// Panics if `idx` is out of bounds.
    pub fn get_mut(&mut self, idx: u32) -> &mut T {
        &mut self.items[idx as usize]
    }

    /// Returns the number of items in the arena.
    #[must_use]
    pub fn len(&self) -> u32 {
        // Safe: alloc() asserts len <= u32::MAX.
        #[allow(clippy::cast_possible_truncation)]
        let len = self.items.len() as u32;
        len
    }

    /// Returns `true` if the arena contains no items.
    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.items.is_empty()
    }

    /// Iterates over `(index, &item)` pairs.
    pub fn iter(&self) -> impl Iterator<Item = (u32, &T)> {
        self.items.iter().enumerate().map(|(i, item)| {
            // Safe: alloc() asserts len <= u32::MAX.
            #[allow(clippy::cast_possible_truncation)]
            let idx = i as u32;
            (idx, item)
        })
    }

    /// Iterates over all items (without indices).
    pub fn values(&self) -> impl Iterator<Item = &T> {
        self.items.iter()
    }
}

impl<T> Default for Arena<T> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T> Index<u32> for Arena<T> {
    type Output = T;

    fn index(&self, idx: u32) -> &Self::Output {
        self.get(idx)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn alloc_and_get() {
        let mut arena = Arena::new();
        let a = arena.alloc("first");
        let b = arena.alloc("second");
        let c = arena.alloc("third");

        assert_eq!(a, 0);
        assert_eq!(b, 1);
        assert_eq!(c, 2);
        assert_eq!(arena.get(a), &"first");
        assert_eq!(arena.get(b), &"second");
        assert_eq!(arena.get(c), &"third");
        assert_eq!(arena.len(), 3);
    }

    #[test]
    fn index_operator() {
        let mut arena = Arena::new();
        let idx = arena.alloc(42);
        assert_eq!(arena[idx], 42);
    }

    #[test]
    fn get_mut_modifies_in_place() {
        let mut arena = Arena::new();
        let idx = arena.alloc(String::from("hello"));
        arena.get_mut(idx).push_str(" world");
        assert_eq!(arena.get(idx), "hello world");
    }

    #[test]
    fn iter_yields_index_and_value() {
        let mut arena = Arena::new();
        arena.alloc("a");
        arena.alloc("b");
        arena.alloc("c");

        let collected: Vec<(u32, &&str)> = arena.iter().collect();
        assert_eq!(collected.len(), 3);
        assert_eq!(*collected[0].1, "a");
        assert_eq!(*collected[1].1, "b");
        assert_eq!(*collected[2].1, "c");
        assert_eq!(collected[0].0, 0);
        assert_eq!(collected[2].0, 2);
    }

    #[test]
    fn with_capacity_does_not_affect_len() {
        let arena: Arena<i32> = Arena::with_capacity(100);
        assert_eq!(arena.len(), 0);
        assert!(arena.is_empty());
    }

    #[test]
    fn empty_arena() {
        let arena: Arena<String> = Arena::new();
        assert!(arena.is_empty());
        assert_eq!(arena.len(), 0);
        assert_eq!(arena.iter().count(), 0);
    }
}
