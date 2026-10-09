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

//! RAII scope guard for the variable context.
//!
//! Ensures [`VariableContext::pop_scope`] is called even if evaluation
//! returns early via `?` or panics. This is the Rust equivalent of Java's
//! try/finally pattern used in `FunctionExpressionExecutor`.
//!
//! # Usage
//!
//! ```ignore
//! let guard = ScopeGuard::new(&mut self.context);
//! guard.set("x", Value::Integer(42));
//! let result = self.eval(body)?;
//! drop(guard); // scope popped — or auto-popped on early return
//! ```

use crate::context::VariableContext;
use crate::value::Value;

/// RAII guard that pops a scope when dropped.
///
/// Created via [`ScopeGuard::new`], which pushes a scope immediately.
/// When the guard goes out of scope (including on `?` early returns),
/// the scope is automatically popped, restoring shadowed variables.
///
/// # Panic safety
///
/// If evaluation panics (which should never happen in normal use),
/// the scope is still popped via the `Drop` implementation, maintaining
/// context consistency.
pub struct ScopeGuard<'a> {
    context: &'a mut VariableContext,
}

impl<'a> ScopeGuard<'a> {
    /// Push a new scope and return a guard that will pop it on drop.
    pub fn new(context: &'a mut VariableContext) -> Self {
        context.push_scope();
        Self { context }
    }

    /// Set a variable in the guarded scope.
    ///
    /// Delegates to [`VariableContext::set`].
    pub fn set(&mut self, name: impl Into<smol_str::SmolStr>, value: Value) {
        self.context.set(name, value);
    }

    /// Access the underlying variable context immutably.
    #[must_use]
    pub fn context(&self) -> &VariableContext {
        self.context
    }

    /// Access the underlying variable context mutably.
    pub fn context_mut(&mut self) -> &mut VariableContext {
        self.context
    }
}

impl Drop for ScopeGuard<'_> {
    fn drop(&mut self) {
        self.context.pop_scope();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn guard_pops_scope_on_drop() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("outer", Value::Integer(1));

        {
            let mut guard = ScopeGuard::new(&mut ctx);
            guard.set("inner", Value::Integer(2));
            assert_eq!(guard.context().get("inner"), Some(&Value::Integer(2)));
            assert_eq!(guard.context().get("outer"), Some(&Value::Integer(1)));
            // guard dropped here
        }

        // inner is gone, outer survives
        assert_eq!(ctx.get("inner"), None);
        assert_eq!(ctx.get("outer"), Some(&Value::Integer(1)));
    }

    #[test]
    fn guard_pops_scope_on_early_return() {
        let mut ctx = VariableContext::new();
        ctx.push_scope();
        ctx.set("x", Value::Integer(1));

        // Simulate the pattern: create guard, do work that may fail, drop guard
        let result: Result<Value, &str> = {
            let mut guard = ScopeGuard::new(&mut ctx);
            guard.set("x", Value::Integer(99)); // shadow
            Err("simulated error") // early return — guard dropped
        };

        assert!(result.is_err());
        // x restored to original value
        assert_eq!(ctx.get("x"), Some(&Value::Integer(1)));
    }

    #[test]
    fn guard_depth_tracking() {
        let mut ctx = VariableContext::new();
        assert_eq!(ctx.depth(), 0);

        ctx.push_scope();
        assert_eq!(ctx.depth(), 1);

        {
            let guard = ScopeGuard::new(&mut ctx);
            assert_eq!(guard.context().depth(), 2);
        }

        assert_eq!(ctx.depth(), 1);
    }
}
