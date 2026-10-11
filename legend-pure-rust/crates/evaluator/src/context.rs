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

use smol_str::SmolStr;

use crate::error::PureRuntimeError;
use crate::value::Value;

/// An entry in the undo log — records what to restore on `pop_scope`.
#[derive(Debug)]
enum UndoEntry {
    /// The variable didn't exist before this scope introduced it —
    /// on pop, remove it from the map entirely.
    Remove(SmolStr),
    /// The variable existed with a previous value that was shadowed —
    /// on pop, restore the old value.
    Restore(SmolStr, Value),
}

/// A stack of variable scopes backed by a flat `HashMap` + undo log.
///
/// Variables are resolved with a single `HashMap` lookup regardless
/// of scope depth. Push/pop manages an undo log to restore previous
/// bindings.
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
pub struct VariableContext {
    /// The single flat map — always contains the "current" value of every
    /// variable visible at the current scope depth.
    vars: HashMap<SmolStr, Value>,

    /// Undo log frames. Each frame records what to undo when `pop_scope`
    /// is called. The outer `Vec` is the scope stack; the inner `Vec`
    /// holds the undo entries for that scope.
    undo_stack: Vec<Vec<UndoEntry>>,
}

impl VariableContext {
    /// Create a new empty context (no scopes).
    #[must_use]
    pub fn new() -> Self {
        Self {
            vars: HashMap::new(),
            undo_stack: Vec::new(),
        }
    }

    /// Push a new scope. No allocation unless variables are actually set.
    pub fn push_scope(&mut self) {
        self.undo_stack.push(Vec::new());
    }

    /// Pop the innermost scope, restoring all shadowed/removed variables.
    ///
    /// # Panics
    /// Panics if there are no scopes to pop (programming error).
    pub fn pop_scope(&mut self) {
        let Some(entries) = self.undo_stack.pop() else {
            unreachable!("VariableContext::pop_scope called with no scopes");
        };

        for entry in entries.into_iter().rev() {
            match entry {
                UndoEntry::Remove(name) => {
                    self.vars.remove(&name);
                }
                UndoEntry::Restore(name, old_value) => {
                    self.vars.insert(name, old_value);
                }
            }
        }
    }

    /// Set a variable in the current scope.
    ///
    /// # Panics
    /// Panics if there are no scopes (call `push_scope` first).
    pub fn set(&mut self, name: impl Into<SmolStr>, value: Value) {
        let name = name.into();

        let Some(undo_frame) = self.undo_stack.last_mut() else {
            unreachable!("VariableContext::set called with no scopes");
        };

        if let Some(old_value) = self.vars.get(&name).cloned() {
            undo_frame.push(UndoEntry::Restore(name.clone(), old_value));
        } else {
            undo_frame.push(UndoEntry::Remove(name.clone()));
        }

        self.vars.insert(name, value);
    }

    /// Look up a variable by name.
    #[must_use]
    pub fn get(&self, name: &str) -> Option<&Value> {
        self.vars.get(name)
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
        self.undo_stack.len()
    }

    /// Iterate every currently visible binding's value.
    pub fn iter_values(&self) -> impl Iterator<Item = &Value> + '_ {
        self.vars.values()
    }

    /// Iterate every currently visible binding as a `(name, value)` pair.
    ///
    /// Order is unspecified (the underlying storage is a
    /// `HashMap`); callers that need a stable ordering should sort by
    /// name on the consumer side.
    pub fn iter_bindings(&self) -> impl Iterator<Item = (&SmolStr, &Value)> + '_ {
        self.vars.iter()
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
