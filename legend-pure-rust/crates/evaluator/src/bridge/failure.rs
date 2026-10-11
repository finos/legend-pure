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

//! Failures at the boundary as typed [`PureException`]s, and the payload a
//! host receives for any failure.

use legend_pure_metamodel::source_info::SourceInfo;

use crate::error::{ConstraintKind, PureException, PureExceptionKind, PureRuntimeError};

/// The source id of a failure raised by the boundary itself.
pub const BOUNDARY_SOURCE: &str = "<ffi>";

/// The anchor of a failure raised by the boundary itself.
fn boundary_source() -> SourceInfo {
    SourceInfo::new(BOUNDARY_SOURCE, 0, 0, 0, 0)
}

/// A failure of the boundary itself (an execution error anchored at
/// [`BOUNDARY_SOURCE`]).
pub fn boundary_error(message: impl Into<String>) -> PureException {
    typed(PureRuntimeError::EvaluationError(message.into()))
}

/// A value kind or shape the boundary cannot carry yet.
pub fn unsupported(what: impl Into<String>) -> PureException {
    typed(PureRuntimeError::Unsupported(what.into()))
}

/// A runtime error anchored at [`BOUNDARY_SOURCE`].
#[must_use]
pub fn typed(error: PureRuntimeError) -> PureException {
    PureException::execution(error, boundary_source(), Vec::new())
}

/// Whether a failure is anchored at the boundary or nowhere: such a
/// failure takes the anchor of the node it was raised for.
#[must_use]
pub fn is_unanchored(failure: &PureException) -> bool {
    failure
        .source
        .as_ref()
        .is_none_or(|s| s.source == BOUNDARY_SOURCE)
}

/// A failure as plain data for a host's exception type. Every field is
/// text (or a list of texts) so a transport only builds strings.
///
/// An `Option` is a host `null`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExceptionPayload {
    /// The failure text with its kind prefix (`FunctionNotFound: …`,
    /// `AssertionFailed: …`), the one the PCT manifests match.
    pub message: String,
    /// The failure text alone, without the kind prefix: what Pure's
    /// `PureException.getInfo()` reports for the same failure.
    pub info: String,
    /// Top-level kind, as a host's kind enumeration spells it:
    /// `EXECUTION_ERROR`, `ASSERTION_FAILED`, `CONSTRAINT_VIOLATION`,
    /// `FUNCTION_NOT_FOUND`, `UNSUPPORTED`, `NOT_IMPLEMENTED`.
    pub kind: &'static str,
    /// Rendered source location (`file:line:col`), `None` when the
    /// exception has no anchor.
    pub source_info: Option<String>,
    /// Each frame rendered as `<function-name> <- file:line:col`,
    /// innermost first. Empty when no frames were captured.
    pub call_stack: Vec<String>,
    /// Constraint name (for `CONSTRAINT_VIOLATION`). `None` otherwise.
    pub constraint_id: Option<String>,
    /// Constraint axis: `CLASS` / `PRE` / `POST`.
    pub constraint_kind: Option<&'static str>,
    /// `::`-joined path of the constraint-owning class or function.
    /// `None` for non-constraint kinds.
    pub owner_fqn: Option<String>,
}

/// The payload of a [`PureException`].
#[must_use]
pub fn payload_of(exc: &PureException) -> ExceptionPayload {
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
    use crate::error::{PureRuntimeError, StackFrame};
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
        let p = payload_of(&exc);
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
        let p = payload_of(&exc);
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
        let p = payload_of(&exc);
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
        let p = payload_of(&exc);
        assert_eq!(p.kind, "CONSTRAINT_VIOLATION");
        assert_eq!(p.constraint_id.as_deref(), Some("ageNonNeg"));
        assert_eq!(p.constraint_kind, Some("CLASS"));
        assert_eq!(p.owner_fqn.as_deref(), Some("LA_ParentWithAgeConstraint"));
        assert!(p.message.contains("Constraint :[ageNonNeg] violated"));
        assert!(p.message.contains("LA_ParentWithAgeConstraint"));
        assert!(p.message.contains("age must be non-negative"));
    }

    #[test]
    fn call_stack_renders_innermost_first() {
        let exc = PureException::new(
            PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(
                "oops".to_string(),
            )),
            Some(dummy_source("a.pure", 1, 1)),
            vec![
                StackFrame {
                    function_name: SmolStr::new("outer"),
                    source: dummy_source("a.pure", 1, 1),
                },
                StackFrame {
                    function_name: SmolStr::new("inner"),
                    source: dummy_source("a.pure", 5, 2),
                },
            ],
        );
        let p = payload_of(&exc);
        assert_eq!(p.call_stack.len(), 2);
        assert!(p.call_stack[0].starts_with("inner <- "));
        assert!(p.call_stack[1].starts_with("outer <- "));
    }

    #[test]
    fn a_boundary_failure_is_unanchored_and_a_located_one_is_not() {
        assert!(is_unanchored(&boundary_error("x")));
        assert!(is_unanchored(&PureException::from(
            PureRuntimeError::EvaluationError("x".into())
        )));
        let located = PureException::execution(
            PureRuntimeError::EvaluationError("x".into()),
            dummy_source("a.pure", 1, 1),
            vec![],
        );
        assert!(!is_unanchored(&located));
    }
}
