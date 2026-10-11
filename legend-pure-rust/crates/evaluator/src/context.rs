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

//! Variable Context — scoped variable bindings for expression evaluation.
//!
//! Implements a push/pop scope chain for:
//! - `let` bindings
//! - Function parameters
//! - `$this` references
//! - Lambda captures

use std::collections::HashMap;
use std::hash::{BuildHasherDefault, Hasher};

use smol_str::SmolStr;

use crate::error::PureRuntimeError;
use crate::value::Value;

/// A multiplicative hash for short identifier keys that come from source
/// text, never from an adversary.
#[derive(Default, Clone, Copy)]
pub struct NameHasher(u64);

impl Hasher for NameHasher {
    fn write(&mut self, bytes: &[u8]) {
        for chunk in bytes.chunks(8) {
            let mut word = [0u8; 8];
            word[..chunk.len()].copy_from_slice(chunk);
            self.0 = (self.0.rotate_left(5) ^ u64::from_le_bytes(word))
                .wrapping_mul(0x517c_c1b7_2722_0a95);
        }
    }

    fn finish(&self) -> u64 {
        self.0
    }
}

/// A map keyed by a name, hashed with [`NameHasher`].
pub type NameMap<V> = HashMap<SmolStr, V, BuildHasherDefault<NameHasher>>;

/// A stack of variable scopes: bindings in the order they were made,
/// read by scanning from the top, so the latest binding of a name wins.
/// A scope is a position in the stack; popping it truncates.
///
/// # Example
///
/// ```ignore
/// let mut ctx = VariableContext::new();
///
/// ctx.push_scope();
/// ctx.set("x", Value::Integer(10));
///
/// ctx.push_scope();
/// ctx.set("x", Value::Integer(99));
///
/// assert_eq!(ctx.get("x"), Some(&Value::Integer(99)));
///
/// ctx.pop_scope();
/// assert_eq!(ctx.get("x"), Some(&Value::Integer(10)));
/// ```
#[derive(Debug)]
pub struct VariableContext {
    /// Every binding made since the outermost scope, latest last.
    bindings: Vec<(SmolStr, Value)>,

    /// Where each open scope starts in `bindings`.
    frames: Vec<usize>,
}

impl VariableContext {
    /// Create a new empty context (no scopes).
    #[must_use]
    pub fn new() -> Self {
        Self {
            bindings: Vec::new(),
            frames: Vec::new(),
        }
    }

    /// Push a new scope. Bindings made until the matching `pop_scope`
    /// are dropped by it.
    pub fn push_scope(&mut self) {
        self.frames.push(self.bindings.len());
    }

    /// Pop the innermost scope, dropping its bindings so what they
    /// shadowed is visible again.
    ///
    /// # Panics
    /// Panics if there are no scopes to pop (programming error).
    pub fn pop_scope(&mut self) {
        let Some(start) = self.frames.pop() else {
            unreachable!("VariableContext::pop_scope called with no scopes");
        };
        self.bindings.truncate(start);
    }

    /// Set a variable in the current scope.
    ///
    /// # Panics
    /// Panics if there are no scopes (call `push_scope` first).
    pub fn set(&mut self, name: impl Into<SmolStr>, value: Value) {
        assert!(
            !self.frames.is_empty(),
            "VariableContext::set called with no scopes"
        );
        self.bindings.push((name.into(), value));
    }

    /// Look up a variable by name.
    #[must_use]
    pub fn get(&self, name: &str) -> Option<&Value> {
        self.bindings
            .iter()
            .rev()
            .find(|(bound, _)| bound.as_str() == name)
            .map(|(_, value)| value)
    }

    /// Look up a variable, returning an error if not found.
    ///
    /// # Errors
    /// Returns `VariableNotFound` if the variable is not in any scope.
    pub fn require(&self, name: &str) -> Result<&Value, PureRuntimeError> {
        self.get(name)
            .ok_or_else(|| PureRuntimeError::VariableNotFound(name.into()))
    }

    /// The current scope depth (number of active scopes).
    #[must_use]
    pub fn depth(&self) -> usize {
        self.frames.len()
    }

    /// Iterate every currently visible binding's value.
    pub fn iter_values(&self) -> impl Iterator<Item = &Value> + '_ {
        self.iter_bindings().map(|(_, value)| value)
    }

    /// Iterate every currently visible binding as a `(name, value)` pair.
    ///
    /// Shadowed bindings are left out; order is unspecified.
    pub fn iter_bindings(&self) -> impl Iterator<Item = (&SmolStr, &Value)> + '_ {
        let mut seen: Vec<&SmolStr> = Vec::new();
        self.bindings
            .iter()
            .rev()
            .filter(move |(name, _)| {
                if seen.contains(&name) {
                    false
                } else {
                    seen.push(name);
                    true
                }
            })
            .map(|(name, value)| (name, value))
    }
}

impl Default for VariableContext {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn basic_set_and_get() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("x", Value::Integer(42));

        assert_eq!(ctx.get("x"), Some(&Value::Integer(42)));
        assert_eq!(ctx.get("y"), None);
    }

    #[test]
    fn lexical_scoping() {
        let mut ctx = VariableContext::new();

        ctx.push_scope();
        ctx.set("x", Value::Integer(1));
        ctx.set("y", Value::Integer(2));

        ctx.push_scope();
        ctx.set("x", Value::Integer(100));

        assert_eq!(ctx.get("x"), Some(&Value::Integer(100)));
        assert_eq!(ctx.get("y"), Some(&Value::Integer(2)));

        ctx.pop_scope();

        assert_eq!(ctx.get("x"), Some(&Value::Integer(1)));
        assert_eq!(ctx.get("y"), Some(&Value::Integer(2)));
    }

    #[test]
    fn require_returns_error() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();

        let result = ctx.require("nonexistent");
        assert!(result.is_err());
    }

    #[test]
    fn depth_tracking() {
        let mut ctx = VariableContext::new();
        assert_eq!(ctx.depth(), 0);

        ctx.push_scope();
        assert_eq!(ctx.depth(), 1);

        ctx.push_scope();
        assert_eq!(ctx.depth(), 2);

        ctx.pop_scope();
        assert_eq!(ctx.depth(), 1);
    }

    #[test]
    fn overwrite_in_same_scope() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("x", Value::Integer(1));
        ctx.set("x", Value::Integer(2));

        assert_eq!(ctx.get("x"), Some(&Value::Integer(2)));
    }

    #[test]
    fn overwrite_then_pop_removes() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("x", Value::Integer(1));
        ctx.set("x", Value::Integer(2));

        ctx.pop_scope();
        assert_eq!(ctx.get("x"), None);
    }

    #[test]
    fn deep_nesting() {
        let mut ctx = VariableContext::new();

        for i in 0..10 {
            ctx.push_scope();
            ctx.set(format!("v{i}"), Value::Integer(i));
        }

        for i in 0..10 {
            assert_eq!(ctx.get(&format!("v{i}")), Some(&Value::Integer(i)));
        }

        for i in (0..10).rev() {
            ctx.pop_scope();
            assert_eq!(ctx.get(&format!("v{i}")), None);
            if i > 0 {
                assert_eq!(
                    ctx.get(&format!("v{}", i - 1)),
                    Some(&Value::Integer(i - 1))
                );
            }
        }
    }

    #[test]
    fn shadow_and_restore_across_three_levels() {
        let mut ctx = VariableContext::new();

        ctx.push_scope();
        ctx.set("x", Value::Integer(1));

        ctx.push_scope();
        ctx.set("x", Value::Integer(2));

        ctx.push_scope();
        ctx.set("x", Value::Integer(3));

        assert_eq!(ctx.get("x"), Some(&Value::Integer(3)));

        ctx.pop_scope();
        assert_eq!(ctx.get("x"), Some(&Value::Integer(2)));

        ctx.pop_scope();
        assert_eq!(ctx.get("x"), Some(&Value::Integer(1)));

        ctx.pop_scope();
        assert_eq!(ctx.get("x"), None);
    }

    #[test]
    fn pop_does_not_affect_outer_variables() {
        let mut ctx = VariableContext::new();

        ctx.push_scope();
        ctx.set("outer", Value::Integer(1));

        ctx.push_scope();
        ctx.set("inner", Value::Integer(2));

        ctx.pop_scope();

        assert_eq!(ctx.get("outer"), Some(&Value::Integer(1)));
        assert_eq!(ctx.get("inner"), None);
    }

    #[test]
    fn empty_context_get_returns_none() {
        let ctx = VariableContext::new();
        assert_eq!(ctx.get("anything"), None);
    }

    #[test]
    fn iter_bindings_reflects_shadowing() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("x", Value::Integer(1));
        ctx.set("y", Value::Integer(2));
        ctx.push_scope();
        ctx.set("x", Value::Integer(100));

        let mut visible: Vec<(SmolStr, Value)> = ctx
            .iter_bindings()
            .map(|(n, v)| (n.clone(), v.clone()))
            .collect();
        visible.sort_by(|a, b| a.0.cmp(&b.0));
        assert_eq!(
            visible,
            vec![
                (SmolStr::new("x"), Value::Integer(100)),
                (SmolStr::new("y"), Value::Integer(2)),
            ],
        );

        ctx.pop_scope();
        let mut after_pop: Vec<(SmolStr, Value)> = ctx
            .iter_bindings()
            .map(|(n, v)| (n.clone(), v.clone()))
            .collect();
        after_pop.sort_by(|a, b| a.0.cmp(&b.0));
        assert_eq!(
            after_pop,
            vec![
                (SmolStr::new("x"), Value::Integer(1)),
                (SmolStr::new("y"), Value::Integer(2)),
            ],
        );
    }
}
