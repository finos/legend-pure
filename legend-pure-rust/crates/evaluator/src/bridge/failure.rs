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

use crate::error::{
    ConstraintKind, PureException, PureExceptionKind, PureRuntimeError, StackFrame,
};

/// The source id of a failure raised by the boundary itself, whichever
/// transport carries it.
pub const BOUNDARY_SOURCE: &str = "<boundary>";

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

/// The kind of a failure a host receives. A transport turns it into its
/// host's own spelling or code with an exhaustive match: a kind added
/// here is a compile error in every transport until it says what the
/// kind becomes.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FailureKind {
    /// A runtime error of the evaluator or of the boundary.
    ExecutionError,
    /// `assert` or `fail`.
    AssertionFailed,
    /// A class invariant or a function's pre- or post-condition.
    ConstraintViolation,
    /// A function path or native key the evaluator does not know.
    FunctionNotFound,
    /// A language feature or value shape the evaluator cannot carry yet.
    Unsupported,
    /// A native registered under its key whose body is not there yet.
    NotImplemented,
    /// A panic unwound through a call. Raised by a transport's guard,
    /// never by [`payload_of`]: the context it happened on is poisoned.
    Panic,
}

impl FailureKind {
    /// The kind's name, which opens a payload's message.
    #[must_use]
    pub const fn name(self) -> &'static str {
        match self {
            Self::ExecutionError => "ExecutionError",
            Self::AssertionFailed => "AssertionFailed",
            Self::ConstraintViolation => "ConstraintViolation",
            Self::FunctionNotFound => "FunctionNotFound",
            Self::Unsupported => "Unsupported",
            Self::NotImplemented => "NotImplemented",
            Self::Panic => "Panic",
        }
    }
}

/// A failure as plain data for a host: its kind, its texts, where it
/// happened and the calls it happened in, each structured; a transport
/// renders what its host needs and parses nothing back.
///
/// An `Option` is a host `null`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExceptionPayload {
    /// The human-readable line: the kind's [name](FailureKind::name),
    /// `": "`, then [`info`](Self::info).
    pub message: String,
    /// The failure text alone, without the kind's name: what Pure's
    /// `PureException.getInfo()` reports for the same failure.
    pub info: String,
    /// What went wrong.
    pub kind: FailureKind,
    /// Where it happened, start and end; `None` when the failure has no
    /// anchor.
    pub source: Option<SourceInfo>,
    /// The calls it happened in, innermost first. Empty when no frames
    /// were captured.
    pub call_stack: Vec<StackFrame>,
    /// Constraint name (for `ConstraintViolation`). `None` otherwise.
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
    let (kind, info) = match &exc.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::FunctionNotFound(name)) => {
            (FailureKind::FunctionNotFound, name.to_string())
        }
        PureExceptionKind::ExecutionError(PureRuntimeError::Unsupported(what)) => {
            (FailureKind::Unsupported, what.clone())
        }
        PureExceptionKind::ExecutionError(PureRuntimeError::NotImplemented(key)) => {
            (FailureKind::NotImplemented, key.to_string())
        }
        PureExceptionKind::ExecutionError(err) => (FailureKind::ExecutionError, err.to_string()),
        PureExceptionKind::AssertionFailed(msg) => (FailureKind::AssertionFailed, msg.clone()),
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
            (FailureKind::ConstraintViolation, composed)
        }
    };
    let message = format!("{}: {info}", kind.name());

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
        source: exc.source.clone(),
        call_stack: exc.call_stack.iter().rev().cloned().collect(),
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
    fn execution_error_payload_carries_kind_and_the_whole_span() {
        let span = SourceInfo::new("a.pure", 10, 4, 12, 9);
        let exc = PureException::execution(
            PureRuntimeError::EvaluationError("divide by zero".to_string()),
            span.clone(),
            vec![],
        );
        let p = payload_of(&exc);
        assert_eq!(p.kind, FailureKind::ExecutionError);
        assert_eq!(p.message, "ExecutionError: divide by zero");
        assert_eq!(p.source, Some(span));
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
        assert_eq!(p.kind, FailureKind::NotImplemented);
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
        assert_eq!(p.kind, FailureKind::AssertionFailed);
        assert_eq!(p.message, "AssertionFailed: expected 5 got 6");
        assert_eq!(p.info, "expected 5 got 6");
        assert_eq!(p.source, Some(dummy_source("assert.pure", 3, 1)));
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
        assert_eq!(p.kind, FailureKind::ConstraintViolation);
        assert_eq!(p.constraint_id.as_deref(), Some("ageNonNeg"));
        assert_eq!(p.constraint_kind, Some("CLASS"));
        assert_eq!(p.owner_fqn.as_deref(), Some("LA_ParentWithAgeConstraint"));
        assert!(p.message.contains("Constraint :[ageNonNeg] violated"));
        assert!(p.message.contains("LA_ParentWithAgeConstraint"));
        assert!(p.message.contains("age must be non-negative"));
    }

    #[test]
    fn call_stack_is_innermost_first_with_each_frames_span() {
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
        assert_eq!(p.call_stack[0].function_name, "inner");
        assert_eq!(p.call_stack[0].source, dummy_source("a.pure", 5, 2));
        assert_eq!(p.call_stack[1].function_name, "outer");
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
