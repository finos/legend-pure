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

//! Runtime errors for the Pure interpreter.
//!
//! Two-layer error model:
//!
//! - [`PureRuntimeError`] — the "what went wrong" layer. Created by low-level
//!   operations (heap, value conversions, context) that don't know which
//!   expression is being evaluated. No source location.
//!
//! - [`PureException`] — the user-facing layer. Created by the evaluator,
//!   wrapping a [`PureExceptionKind`] with source location and a Pure-level
//!   call stack. This is what gets reported to the user.
//!
//! See `docs/runtime/error_location_design.md` for the full design rationale.

use std::fmt;

use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;
use thiserror::Error;

use crate::value::Value;

/// Errors that can occur during Pure expression evaluation.
///
/// These are low-level error kinds without source location context.
/// The evaluator wraps them in [`PureException`] with location info.
#[derive(Debug, Error)]
pub enum PureRuntimeError {
    /// Type mismatch: expected one type, got another.
    #[error("Type mismatch: expected {expected}, got {actual}")]
    TypeMismatch {
        /// The type that was expected.
        expected: &'static str,
        /// The type that was actually found.
        actual: String,
    },

    /// Property not found on an object.
    #[error("Property '{property}' not found on {classifier}")]
    PropertyNotFound {
        /// The property name that was not found.
        property: SmolStr,
        /// The classifier of the object.
        classifier: SmolStr,
    },

    /// Variable not found in the current scope.
    #[error("Variable '{0}' not found")]
    VariableNotFound(SmolStr),

    /// Downcast failed (compiled code expected a specific struct type).
    #[error("Downcast failed: expected {expected}, object is {actual}")]
    DowncastFailed {
        /// The type that was expected.
        expected: &'static str,
        /// The actual object type.
        actual: String,
    },

    /// Function not found.
    #[error("Function not found: {0}")]
    FunctionNotFound(SmolStr),

    /// A language feature whose runtime has not been fished from the
    /// archive yet (relation literals, path literals, milestoning).
    #[error("Unsupported: {0}")]
    Unsupported(&'static str),

    /// Multiplicity violation: got wrong number of values.
    #[error("Multiplicity violation: expected {expected}, got {actual} values")]
    MultiplicityViolation {
        /// The expected multiplicity range.
        expected: String,
        /// The number of values that were actually present.
        actual: usize,
    },

    /// Division by zero.
    #[error("Division by zero")]
    DivisionByZero,

    /// An assertion failed (constraint violation or explicit assertion).
    #[error("Assertion failed: {0}")]
    AssertionFailed(String),

    /// A generic evaluation error with a message.
    #[error("{0}")]
    EvaluationError(String),
}

impl PureRuntimeError {
    /// Create a `TypeMismatch` error from an expected type name and actual `Value`.
    #[must_use]
    pub fn type_mismatch(expected: &'static str, actual: &Value) -> Self {
        Self::TypeMismatch {
            expected,
            actual: actual.type_name().to_owned(),
        }
    }
}

/// A frame in the Pure-level call stack.
///
/// Each frame represents a function call expression being evaluated,
/// mirroring Java's `functionExpressionCallStack` entries.
#[derive(Debug, Clone)]
pub struct StackFrame {
    /// Human-readable function identifier.
    ///
    /// Examples: `"my::package::process"`, `"Lambda {Integer[1]->String[1]}"`.
    pub function_name: SmolStr,

    /// Source location of the function call expression.
    pub source: SourceInfo,
}

/// The kind of exception — distinguishes runtime errors, assertions,
/// and constraint violations.
///
/// Mirrors Java's `PureExecutionException` / `PureAssertFailException`
/// hierarchy, plus structured constraint violation data.
#[derive(Debug)]
pub enum PureExceptionKind {
    /// A runtime error (type mismatch, property not found, division by zero, etc.)
    ExecutionError(PureRuntimeError),

    /// An assertion failure from `fail()` or `assert()`.
    ///
    /// Test frameworks use this to distinguish expected failures from bugs.
    AssertionFailed(String),

    /// A constraint violation — class invariant or function pre/post condition.
    ConstraintViolation {
        /// The constraint name (rule ID).
        constraint_id: SmolStr,
        /// Which kind of constraint was violated.
        constraint_kind: ConstraintKind,
        /// The class or function that owns the constraint.
        owner: SmolStr,
        /// Optional custom message from the constraint's `messageFunction`.
        message: Option<String>,
    },
}

/// The kind of constraint that was violated.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConstraintKind {
    /// Class invariant — checked on `new` / `copy`.
    Class,
    /// Function pre-condition — checked before function body.
    Pre,
    /// Function post-condition — checked after function body.
    Post,
}

/// A runtime error with full source location context.
///
/// This is the user-facing error type. Every runtime error carries the
/// source location where it occurred and the Pure-level call stack at
/// the time of failure.
///
/// Created by the evaluator; low-level operations return [`PureRuntimeError`]
/// which the evaluator enriches with location via [`PureException::execution`].
#[derive(Debug)]
pub struct PureException {
    /// What went wrong.
    pub kind: PureExceptionKind,

    /// Where in the Pure source the error originated.
    pub source: Option<SourceInfo>,

    /// The Pure-level call stack at the time of the error.
    ///
    /// Ordered from outermost (first) to innermost (last) — the frame
    /// closest to the error is at the end.
    pub call_stack: Vec<StackFrame>,
}

impl PureException {
    /// Create an execution error with source location and call stack.
    ///
    /// This is the primary constructor, used by the evaluator to wrap
    /// [`PureRuntimeError`]s from low-level operations.
    #[must_use]
    pub fn execution(
        error: PureRuntimeError,
        source: SourceInfo,
        call_stack: Vec<StackFrame>,
    ) -> Self {
        Self {
            kind: PureExceptionKind::ExecutionError(error),
            source: Some(source),
            call_stack,
        }
    }

    /// Create an assertion failure (from `fail()` or `assert()`).
    #[must_use]
    pub fn assertion(message: String, source: SourceInfo, call_stack: Vec<StackFrame>) -> Self {
        Self {
            kind: PureExceptionKind::AssertionFailed(message),
            source: Some(source),
            call_stack,
        }
    }

    /// Create a constraint violation.
    #[must_use]
    pub fn constraint(
        constraint_id: SmolStr,
        constraint_kind: ConstraintKind,
        owner: SmolStr,
        message: Option<String>,
        source: SourceInfo,
        call_stack: Vec<StackFrame>,
    ) -> Self {
        Self {
            kind: PureExceptionKind::ConstraintViolation {
                constraint_id,
                constraint_kind,
                owner,
                message,
            },
            source: Some(source),
            call_stack,
        }
    }

    /// Whether this exception is an assertion failure (from `fail()`/`assert()`).
    ///
    /// Test frameworks use this to distinguish expected failures from bugs.
    #[must_use]
    pub fn is_assertion(&self) -> bool {
        matches!(self.kind, PureExceptionKind::AssertionFailed(_))
    }

    /// Whether this exception is a constraint violation.
    #[must_use]
    pub fn is_constraint_violation(&self) -> bool {
        matches!(self.kind, PureExceptionKind::ConstraintViolation { .. })
    }

    /// Append a stack frame and return self (builder pattern).
    ///
    /// Used by the evaluator's lazy call stack: each `map_err` in the
    /// recursive `eval()` chain pushes a frame as `Err(PureException)`
    /// propagates upward.
    #[must_use]
    pub fn with_frame(mut self, frame: StackFrame) -> Self {
        self.call_stack.push(frame);
        self
    }
}

// ---------------------------------------------------------------------------
// Display — matches Java's printPureStackTrace output format
// ---------------------------------------------------------------------------

impl fmt::Display for PureException {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        // Header: error kind + location
        let kind_name = match &self.kind {
            PureExceptionKind::ExecutionError(_) => "Execution error",
            PureExceptionKind::AssertionFailed(_) => "Assert failure",
            PureExceptionKind::ConstraintViolation { .. } => "Constraint violation",
        };
        write!(f, "{kind_name}")?;
        if let Some(src) = &self.source {
            write!(
                f,
                " (resource:{} line:{} column:{})",
                src.source, src.start_line, src.start_column
            )?;
        }

        // Message
        match &self.kind {
            PureExceptionKind::ExecutionError(e) => write!(f, "\n\"{e}\"")?,
            PureExceptionKind::AssertionFailed(msg) => write!(f, "\n\"{msg}\"")?,
            PureExceptionKind::ConstraintViolation {
                constraint_id,
                constraint_kind,
                owner,
                message,
            } => {
                let kind_label = match constraint_kind {
                    ConstraintKind::Class => "",
                    ConstraintKind::Pre => "(PRE) ",
                    ConstraintKind::Post => "(POST) ",
                };
                write!(f, "\n\"Constraint {kind_label}:[{constraint_id}] violated")?;
                match constraint_kind {
                    ConstraintKind::Class => write!(f, " in the Class {owner}")?,
                    ConstraintKind::Pre | ConstraintKind::Post => {
                        write!(f, ". (Function:{owner})")?;
                    }
                }
                if let Some(msg) = message {
                    write!(f, ", Message: {msg}")?;
                }
                write!(f, "\"")?;
            }
        }

        // Call stack (if non-empty)
        if !self.call_stack.is_empty() {
            write!(f, "\nFull Stack:")?;
            for frame in self.call_stack.iter().rev() {
                write!(
                    f,
                    "\n    {}     <-     resource:{} line:{} column:{}",
                    frame.function_name,
                    frame.source.source,
                    frame.source.start_line,
                    frame.source.start_column,
                )?;
            }
        }
        Ok(())
    }
}

impl std::error::Error for PureException {}

impl From<PureRuntimeError> for PureException {
    /// Convert a `PureRuntimeError` into a `PureException` without location.
    ///
    /// Used as a fallback when source info is unavailable. Prefer
    /// [`PureException::execution`] when source info is known.
    ///
    /// `PureRuntimeError::AssertionFailed` is promoted to
    /// `PureExceptionKind::AssertionFailed` rather than being wrapped as an
    /// `ExecutionError` — the semantic kind should reflect intent (assertion
    /// vs. runtime error), so test frameworks classifying outcomes by
    /// `kind` correctly bucket these as FAIL instead of ERROR.
    fn from(error: PureRuntimeError) -> Self {
        let kind = match error {
            PureRuntimeError::AssertionFailed(msg) => PureExceptionKind::AssertionFailed(msg),
            other => PureExceptionKind::ExecutionError(other),
        };
        Self {
            kind,
            source: None,
            call_stack: Vec::new(),
        }
    }
}

impl fmt::Display for ConstraintKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Class => write!(f, "Class"),
            Self::Pre => write!(f, "Pre"),
            Self::Post => write!(f, "Post"),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_source(line: u32, col: u32) -> SourceInfo {
        SourceInfo::new("my/package/model.pure", line, col, line, col + 10)
    }

    fn test_call_stack() -> Vec<StackFrame> {
        vec![
            StackFrame {
                function_name: "my::package::main".into(),
                source: test_source(5, 3),
            },
            StackFrame {
                function_name: "my::package::process".into(),
                source: test_source(15, 8),
            },
        ]
    }

    #[test]
    fn execution_error_display() {
        let exc = PureException::execution(
            PureRuntimeError::type_mismatch("Integer", &Value::String("hello".into())),
            test_source(15, 8),
            test_call_stack(),
        );

        let output = exc.to_string();
        assert!(
            output.starts_with("Execution error (resource:my/package/model.pure line:15 column:8)")
        );
        assert!(output.contains("Type mismatch: expected Integer, got String"));
        assert!(output.contains("Full Stack:"));
        assert!(output.contains("my::package::process"));
        assert!(output.contains("my::package::main"));
    }

    #[test]
    fn assertion_display() {
        let exc = PureException::assertion(
            "Expected 42 but got 0".into(),
            test_source(10, 5),
            test_call_stack(),
        );

        let output = exc.to_string();
        assert!(output.starts_with("Assert failure"));
        assert!(output.contains("Expected 42 but got 0"));
        assert!(exc.is_assertion());
        assert!(!exc.is_constraint_violation());
    }

    #[test]
    fn class_constraint_display() {
        let exc = PureException::constraint(
            "positivePrice".into(),
            ConstraintKind::Class,
            "Trade".into(),
            Some("Price must be > 0".into()),
            test_source(15, 8),
            test_call_stack(),
        );

        let output = exc.to_string();
        assert!(output.starts_with("Constraint violation"));
        assert!(output.contains("[positivePrice] violated in the Class Trade"));
        assert!(output.contains("Message: Price must be > 0"));
        assert!(exc.is_constraint_violation());
        assert!(!exc.is_assertion());
    }

    #[test]
    fn pre_constraint_display() {
        let exc = PureException::constraint(
            "nonEmpty".into(),
            ConstraintKind::Pre,
            "process".into(),
            None,
            test_source(20, 1),
            Vec::new(),
        );

        let output = exc.to_string();
        assert!(output.contains("(PRE) :[nonEmpty] violated. (Function:process)"));
        // No call stack
        assert!(!output.contains("Full Stack:"));
    }

    #[test]
    fn post_constraint_display() {
        let exc = PureException::constraint(
            "validResult".into(),
            ConstraintKind::Post,
            "compute".into(),
            None,
            test_source(30, 1),
            Vec::new(),
        );

        let output = exc.to_string();
        assert!(output.contains("(POST) :[validResult] violated. (Function:compute)"));
    }

    #[test]
    fn from_runtime_error_no_location() {
        let exc: PureException = PureRuntimeError::DivisionByZero.into();

        assert!(exc.source.is_none());
        assert!(exc.call_stack.is_empty());
        assert!(exc.to_string().contains("Division by zero"));
    }

    #[test]
    fn stack_frame_order_innermost_last() {
        let stack = test_call_stack();
        // First pushed = outermost = index 0
        assert_eq!(stack[0].function_name, "my::package::main");
        // Last pushed = innermost = last index
        assert_eq!(stack[1].function_name, "my::package::process");

        // Display prints in reverse (innermost first, like Java)
        let exc =
            PureException::execution(PureRuntimeError::DivisionByZero, test_source(15, 8), stack);
        let output = exc.to_string();
        let stack_section = output.split("Full Stack:").nth(1).unwrap();
        let lines: Vec<&str> = stack_section.trim().lines().collect();
        // Innermost first in display
        assert!(lines[0].contains("my::package::process"));
        assert!(lines[1].contains("my::package::main"));
    }
}
