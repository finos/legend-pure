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

//! Evaluator hooks for debugging and instrumentation.
//!
//! The evaluator is generic over `H: EvalHooks`, enabling zero-cost
//! instrumentation via Rust's monomorphization:
//!
//! - [`NoOpHooks`] — production. Every method is `#[inline(always)]` and
//!   compiles to nothing. Used by CLI, JNI, and batch execution.
//! - `DebugHooks` (future, in `lsp` crate) — IDE debugging with breakpoints,
//!   stepping, and variable inspection. Maintains a live call stack and
//!   coordinates with the DAP server.
//!
//! # Design rationale
//!
//! Rather than using dynamic dispatch (`dyn EvalHooks`) or feature flags,
//! we use a generic parameter so the compiler can eliminate all hook calls
//! in production builds via monomorphization. This gives us **zero overhead**
//! on the hot path while keeping the evaluator debugger-aware from day one.

use legend_pure_metamodel::source_info::SourceInfo;

use crate::value::Value;

/// Hooks into the evaluation loop for debugging and instrumentation.
///
/// The evaluator calls these methods at key points during expression
/// evaluation. Implementations can use them for:
/// - Breakpoint checking and pause/resume coordination
/// - Live call stack maintenance
/// - Performance profiling
/// - Expression tracing / logging
///
/// # Performance contract
///
/// All methods have a default no-op implementation. For production use,
/// [`NoOpHooks`] provides `#[inline(always)]` implementations that the
/// compiler eliminates entirely after monomorphization.
pub trait EvalHooks {
    /// Called before evaluating an expression.
    ///
    /// Debug implementations check breakpoints here and may suspend
    /// execution until the DAP client resumes. `context` is the
    /// live variable scope at the entry of this expression.
    ///
    /// The return value is the hook's request for a variable
    /// snapshot:
    ///
    /// - `false` — no snapshot needed, the evaluator continues
    ///   immediately. Production hooks (`NoOpHooks`) always return
    ///   `false` and the `before_eval` body monomorphizes away.
    /// - `true` — the evaluator calls
    ///   [`pause_with_snapshot`](EvalHooks::pause_with_snapshot) and the
    ///   hook parks the eval thread there as needed.
    ///
    /// The two-phase pattern keeps `Value` (which is `!Send`) on the
    /// eval thread: the renderer runs in the evaluator, the resulting
    /// owned-string tree is what crosses the thread boundary into
    /// `Arc<Mutex<SessionState>>`.
    fn before_eval(
        &mut self,
        source: &SourceInfo,
        context: &crate::context::VariableContext,
    ) -> bool;

    /// Called by the evaluator after `before_eval` returned `true`.
    ///
    /// The archived workspace passed a rendered snapshot of the locals
    /// here; the parameter is gone until a debugger is fished back. The
    /// default implementation is a no-op so hooks that never request
    /// pauses (those returning `false` from `before_eval`) don't need
    /// to override.
    fn pause_with_snapshot(&mut self, _source: &SourceInfo) {}

    /// Called after evaluating an expression, with the result.
    ///
    /// Debug implementations can update watch expressions and the
    /// variables panel here.
    fn after_eval(&mut self, source: &SourceInfo, result: &Value);

    /// Called when entering a function body.
    ///
    /// Debug implementations push a frame onto the live call stack.
    fn enter_function(&mut self, name: &str, source: &SourceInfo);

    /// Called when leaving a function body.
    ///
    /// Debug implementations pop the live call stack.
    fn leave_function(&mut self, name: &str);

    /// Emit a chunk of console output — Pure-level `print` / `println`
    /// routes through here instead of writing to stdout directly.
    ///
    /// The default implementation writes to stdout so existing callers
    /// see no behavioural change. Debug harnesses, tests, and embedders
    /// override this to capture output (e.g. into a buffer for
    /// assertion, or a LSP/DAP output channel). `msg` is the already-
    /// rendered text; the hook does not append a trailing newline — the
    /// caller (`println` vs. `print`) decides.
    fn console_output(&mut self, msg: &str) {
        use std::io::Write as _;
        let mut stdout = std::io::stdout().lock();
        let _ = stdout.write_all(msg.as_bytes());
    }
}

/// No-op hooks for production execution.
///
/// Every method is `#[inline(always)]` — after monomorphization with
/// `Evaluator<NoOpHooks>`, the compiler eliminates all hook call sites.
/// This means **zero overhead**: no function calls, no branch predictions,
/// no icache pollution.
///
/// # Example
///
/// ```
/// use legend_pure_evaluator::hooks::NoOpHooks;
///
/// // Production evaluator — zero hook overhead
/// let hooks = NoOpHooks;
/// ```
#[derive(Debug, Default, Clone, Copy)]
pub struct NoOpHooks;

impl EvalHooks for NoOpHooks {
    // `inline(always)` is the load-bearing piece of the zero-cost
    // contract documented at the top of this module: every hook
    // method must inline into the evaluator's hot path so production
    // builds see no overhead. Returning a `bool` constant is still
    // the trivial body clippy would otherwise inline by default.
    #[inline(always)]
    #[allow(clippy::inline_always)]
    fn before_eval(
        &mut self,
        _source: &SourceInfo,
        _context: &crate::context::VariableContext,
    ) -> bool {
        false
    }

    #[inline(always)]
    fn after_eval(&mut self, _source: &SourceInfo, _result: &Value) {}

    #[inline(always)]
    fn enter_function(&mut self, _name: &str, _source: &SourceInfo) {}

    #[inline(always)]
    fn leave_function(&mut self, _name: &str) {}
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn no_op_hooks_is_copy() {
        let a = NoOpHooks;
        let _ = a; // Copy
        let _ = a; // Still valid
    }

    #[test]
    fn no_op_hooks_is_default() {
        // Verify Default is implemented (compile-time check)
        fn assert_default<T: Default>() {}
        assert_default::<NoOpHooks>();
    }

    #[test]
    fn no_op_hooks_methods_are_callable() {
        let mut hooks = NoOpHooks;
        let src = SourceInfo::new("test.pure", 1, 1, 1, 10);
        let ctx = crate::context::VariableContext::new();
        assert!(!hooks.before_eval(&src, &ctx));
        hooks.after_eval(&src, &Value::Integer(42));
        hooks.enter_function("test::func", &src);
        hooks.leave_function("test::func");
    }

    /// Hooks implementation that captures console output into a buffer —
    /// demonstrates the redirect pattern embedders (CLI, LSP, tests)
    /// will use.
    #[derive(Debug, Default)]
    struct CapturingHooks {
        output: String,
    }

    impl EvalHooks for CapturingHooks {
        fn before_eval(
            &mut self,
            _source: &SourceInfo,
            _context: &crate::context::VariableContext,
        ) -> bool {
            false
        }
        fn after_eval(&mut self, _source: &SourceInfo, _result: &Value) {}
        fn enter_function(&mut self, _name: &str, _source: &SourceInfo) {}
        fn leave_function(&mut self, _name: &str) {}
        fn console_output(&mut self, msg: &str) {
            self.output.push_str(msg);
        }
    }

    #[test]
    fn console_output_routes_to_hook() {
        let mut hooks = CapturingHooks::default();
        hooks.console_output("hello ");
        hooks.console_output("world");
        assert_eq!(hooks.output, "hello world");
    }

    /// A test hooks implementation that counts calls.
    #[derive(Debug, Default)]
    struct CountingHooks {
        before: usize,
        after: usize,
        enter: usize,
        leave: usize,
    }

    impl EvalHooks for CountingHooks {
        fn before_eval(
            &mut self,
            _source: &SourceInfo,
            _context: &crate::context::VariableContext,
        ) -> bool {
            self.before += 1;
            false
        }
        fn after_eval(&mut self, _source: &SourceInfo, _result: &Value) {
            self.after += 1;
        }
        fn enter_function(&mut self, _name: &str, _source: &SourceInfo) {
            self.enter += 1;
        }
        fn leave_function(&mut self, _name: &str) {
            self.leave += 1;
        }
    }

    #[test]
    fn counting_hooks_tracks_calls() {
        let mut hooks = CountingHooks::default();
        let src = SourceInfo::new("test.pure", 1, 1, 1, 10);
        let ctx = crate::context::VariableContext::new();

        let _ = hooks.before_eval(&src, &ctx);
        let _ = hooks.before_eval(&src, &ctx);
        hooks.after_eval(&src, &Value::Integer(1));
        hooks.enter_function("f", &src);
        hooks.leave_function("f");

        assert_eq!(hooks.before, 2);
        assert_eq!(hooks.after, 1);
        assert_eq!(hooks.enter, 1);
        assert_eq!(hooks.leave, 1);
    }
}
