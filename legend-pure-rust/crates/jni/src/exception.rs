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

//! Conversion from runtime [`PureException`] into the JNI-side payload
//! the Java `PureRustEvaluationException` constructor consumes.
//!
//! Split into a pure-data `ExceptionPayload` and the JVM-only throw
//! helper so the structure-flattening step is testable from a
//! pure-Rust unit test (no JVM required). The throw helper lives in
//! `lib.rs` because it needs `JNIEnv`; the pure payload-builder lives
//! here.

use legend_pure_evaluator::error::{
    ConstraintKind, PureException, PureExceptionKind, PureRuntimeError,
};

/// Pure-data payload for the Java `PureRustEvaluationException`
/// constructor. Every field is a `String` (or `Vec<String>`) so the
/// JNI side just builds Java strings and a string array.
///
/// `Option<String>` maps to a Java `null` in the constructor invocation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExceptionPayload {
    /// Inner failure reason. Avoids the multi-line Display rendering
    /// of `PureException` — `sourceInfo` and `callStack` carry the
    /// rest. Keeps `Throwable.getMessage()` clean for JUnit assertions
    /// that match on substrings.
    pub message: String,
    /// The failure text alone, without the kind prefix: what Pure's
    /// `PureException.getInfo()` reports for the same failure.
    pub info: String,
    /// Top-level kind: `"EXECUTION_ERROR"` / `"ASSERTION_FAILED"` /
    /// `"CONSTRAINT_VIOLATION"`. Java side `enum Kind` parses this.
    pub kind: &'static str,
    /// Rendered source location (`file:line:col`), `None` when the
    /// exception has no anchor (e.g. FFI-boundary errors built via
    /// [`payload_for_message`]).
    pub source_info: Option<String>,
    /// Each entry rendered as `"<function-name> <- file:line:col"`,
    /// outermost-first (matches the Display ordering). Empty when no
    /// frames were captured.
    pub call_stack: Vec<String>,
    /// Constraint name (for `CONSTRAINT_VIOLATION`). `None` otherwise.
    pub constraint_id: Option<String>,
    /// Constraint axis: `"CLASS"` / `"PRE"` / `"POST"`. Separate from
    /// the top-level kind so the orthogonal Pure-language concept
    /// stays addressable.
    pub constraint_kind: Option<&'static str>,
    /// `::`-joined FQN of the constraint-owning Class / Function.
    /// `None` for non-constraint kinds.
    pub owner_fqn: Option<String>,
}

impl ExceptionPayload {
    /// JVM-side throw signature mirror — the constructor that
    /// `throw_pure_exception` in `lib.rs` invokes.
    ///
    /// `(message, info, kind, sourceInfo, callStack, constraintId, constraintKind, ownerFqn)`
    pub const RICH_CTOR_SIG: &'static str = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
}

/// Build an [`ExceptionPayload`] from a runtime [`PureException`].
/// Pure function — no JNI calls — so cargo unit tests can pin it.
#[must_use]
pub fn pure_exception_to_payload(exc: &PureException) -> ExceptionPayload {
    // The kind string is the Java `PureRustEvaluationException.Kind` enum
    // name (parsed with `Kind.valueOf`); the message starts with the
    // kind's display name because the PCT expected-failures manifest
    // matches `expectedError` by substring and has no kind field.
    // `(prefix, info, kind)`: the message Java sees is `prefix: info`,
    // the kind is the `enum Kind` name, the info stands alone.
    let (prefix, info, kind) = match &exc.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::FunctionNotFound(name)) => {
            ("FunctionNotFound", name.to_string(), "FUNCTION_NOT_FOUND")
        }
        PureExceptionKind::ExecutionError(PureRuntimeError::Unsupported(what)) => {
            ("Unsupported", what.clone(), "UNSUPPORTED")
        }
        PureExceptionKind::ExecutionError(PureRuntimeError::NotImplemented(key)) => {
            ("NotImplemented", key.to_string(), "NOT_IMPLEMENTED")
        }
        PureExceptionKind::ExecutionError(err) => {
            ("ExecutionError", err.to_string(), "EXECUTION_ERROR")
        }
        PureExceptionKind::AssertionFailed(msg) => {
            ("AssertionFailed", msg.clone(), "ASSERTION_FAILED")
        }
        PureExceptionKind::ConstraintViolation {
            constraint_id,
            owner,
            message,
            ..
        } => {
            let mut composed =
                format!("Constraint :[{constraint_id}] violated in the Class {owner}");
            if let Some(m) = message {
                composed.push_str(", Message: ");
                composed.push_str(m);
            }
            ("ConstraintViolation", composed, "CONSTRAINT_VIOLATION")
        }
    };
    let message = format!("{prefix}: {info}");

    let source_info = exc
        .source
        .as_ref()
        .map(|s| format!("{}:{}:{}", s.source, s.start_line, s.start_column));

    // Innermost-first ordering matches the `Display` output (which
    // iterates `call_stack.iter().rev()`), so consumers see the
    // failing frame at index 0 and the outermost caller last.
    let call_stack: Vec<String> = exc
        .call_stack
        .iter()
        .rev()
        .map(|frame| {
            format!(
                "{} <- {}:{}:{}",
                frame.function_name,
                frame.source.source,
                frame.source.start_line,
                frame.source.start_column,
            )
        })
        .collect();

    let (constraint_id, constraint_kind, owner_fqn) = match &exc.kind {
        PureExceptionKind::ConstraintViolation {
            constraint_id,
            constraint_kind,
            owner,
            ..
        } => (
            Some(constraint_id.to_string()),
            Some(constraint_kind_label(*constraint_kind)),
            Some(owner.to_string()),
        ),
        _ => (None, None, None),
    };

    ExceptionPayload {
        message,
        info,
        kind,
        source_info,
        call_stack,
        constraint_id,
        constraint_kind,
        owner_fqn,
    }
}

const fn constraint_kind_label(k: ConstraintKind) -> &'static str {
    match k {
        ConstraintKind::Class => "CLASS",
        ConstraintKind::Pre => "PRE",
        ConstraintKind::Post => "POST",
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use legend_pure_evaluator::error::{PureRuntimeError, StackFrame};
    use legend_pure_metamodel::source_info::SourceInfo;
    use smol_str::SmolStr;

    fn dummy_source(file: &str, line: u32, col: u32) -> SourceInfo {
        SourceInfo {
            source: SmolStr::new(file),
            start_line: line,
            start_column: col,
            end_line: line,
            end_column: col,
        }
    }

    #[test]
    fn execution_error_payload_carries_kind_and_source() {
        let exc = PureException::execution(
            PureRuntimeError::EvaluationError("divide by zero".to_string()),
            dummy_source("a.pure", 10, 4),
            vec![],
        );
        let p = pure_exception_to_payload(&exc);
        assert_eq!(p.kind, "EXECUTION_ERROR");
        assert!(p.message.contains("divide by zero"));
        assert_eq!(p.source_info.as_deref(), Some("a.pure:10:4"));
        assert!(p.call_stack.is_empty());
        assert!(p.constraint_id.is_none());
        assert!(p.constraint_kind.is_none());
        assert!(p.owner_fqn.is_none());
    }

    #[test]
    fn not_implemented_payload_names_the_key_with_its_own_kind() {
        let exc = PureException::execution(
            PureRuntimeError::NotImplemented(SmolStr::new(
                "deactivate_Any_MANY__ValueSpecification_1_",
            )),
            dummy_source("a.pure", 2, 3),
            vec![],
        );
        let p = pure_exception_to_payload(&exc);
        assert_eq!(p.kind, "NOT_IMPLEMENTED");
        assert_eq!(
            p.message,
            "NotImplemented: deactivate_Any_MANY__ValueSpecification_1_"
        );
        assert_eq!(p.info, "deactivate_Any_MANY__ValueSpecification_1_");
    }

    #[test]
    fn assertion_failure_payload_message_is_raw_assert_text() {
        let exc = PureException::assertion(
            "expected 5 got 6".to_string(),
            dummy_source("assert.pure", 3, 1),
            vec![],
        );
        let p = pure_exception_to_payload(&exc);
        assert_eq!(p.kind, "ASSERTION_FAILED");
        assert_eq!(p.message, "AssertionFailed: expected 5 got 6");
        assert_eq!(p.info, "expected 5 got 6");
        assert_eq!(p.source_info.as_deref(), Some("assert.pure:3:1"));
    }

    #[test]
    fn constraint_violation_payload_carries_structured_fields() {
        let exc = PureException::constraint(
            SmolStr::new("ageNonNeg"),
            ConstraintKind::Class,
            SmolStr::new("LA_ParentWithAgeConstraint"),
            Some("age must be non-negative".to_string()),
            dummy_source("new.pure", 113, 0),
            vec![],
        );
        let p = pure_exception_to_payload(&exc);
        assert_eq!(p.kind, "CONSTRAINT_VIOLATION");
        assert_eq!(p.constraint_id.as_deref(), Some("ageNonNeg"));
        assert_eq!(p.constraint_kind, Some("CLASS"));
        assert_eq!(p.owner_fqn.as_deref(), Some("LA_ParentWithAgeConstraint"));
        // Backwards-compat message shape — the original Display
        // contract that tests like `testNewWithTypeVariables` assert.
        assert!(p.message.contains("Constraint :[ageNonNeg] violated"));
        assert!(p.message.contains("LA_ParentWithAgeConstraint"));
        assert!(p.message.contains("age must be non-negative"));
    }

    #[test]
    fn call_stack_renders_innermost_first() {
        let exc = PureException {
            kind: PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(
                "oops".to_string(),
            )),
            source: Some(dummy_source("a.pure", 1, 1)),
            call_stack: vec![
                StackFrame {
                    function_name: SmolStr::new("outer"),
                    source: dummy_source("a.pure", 1, 1),
                },
                StackFrame {
                    function_name: SmolStr::new("inner"),
                    source: dummy_source("a.pure", 5, 2),
                },
            ],
        };
        let p = pure_exception_to_payload(&exc);
        // Innermost frame first (matches Display's `.rev()` ordering).
        assert_eq!(p.call_stack.len(), 2);
        assert!(p.call_stack[0].starts_with("inner <- "));
        assert!(p.call_stack[1].starts_with("outer <- "));
    }
}
