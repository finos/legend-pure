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

//! The failure of the last call on this thread, as the host fetches it:
//! one Rust-owned buffer per thread, replaced by every status-returning
//! export and read through `pure_last_failure`.

use std::any::Any;
use std::cell::RefCell;

use legend_pure_evaluator::bridge::failure::{ExceptionPayload, FailureKind, payload_of};
use legend_pure_evaluator::error::{PureException, StackFrame};
use legend_pure_metamodel::source_info::SourceInfo;

use crate::abi::{PureFailure, PureFrame, PureSpan, PureString, failure_kind};

/// The buffer behind a `pure_failure`: the view first, then what it
/// points into. Nothing is pushed or replaced once the view is built, so
/// every string the view points at, a frame's included, stays where it is
/// until the buffer is dropped.
#[repr(C)]
struct Stored {
    view: PureFailure,
    payload: ExceptionPayload,
    frames: Vec<PureFrame>,
}

thread_local! {
    static LAST: RefCell<Option<Box<Stored>>> = const { RefCell::new(None) };
}

/// The code of a kind: `PURE_FAILURE_*`.
#[must_use]
pub const fn kind_code(kind: FailureKind) -> i32 {
    match kind {
        FailureKind::ExecutionError => failure_kind::EXECUTION_ERROR,
        FailureKind::AssertionFailed => failure_kind::ASSERTION_FAILED,
        FailureKind::ConstraintViolation => failure_kind::CONSTRAINT_VIOLATION,
        FailureKind::FunctionNotFound => failure_kind::FUNCTION_NOT_FOUND,
        FailureKind::Unsupported => failure_kind::UNSUPPORTED,
        FailureKind::NotImplemented => failure_kind::NOT_IMPLEMENTED,
        FailureKind::Panic => failure_kind::PANIC,
    }
}

/// The kind behind a `PURE_FAILURE_*` code a host passed; `None` for a
/// number that is no code.
#[must_use]
pub const fn kind_of_code(code: i32) -> Option<FailureKind> {
    match code {
        failure_kind::EXECUTION_ERROR => Some(FailureKind::ExecutionError),
        failure_kind::ASSERTION_FAILED => Some(FailureKind::AssertionFailed),
        failure_kind::CONSTRAINT_VIOLATION => Some(FailureKind::ConstraintViolation),
        failure_kind::FUNCTION_NOT_FOUND => Some(FailureKind::FunctionNotFound),
        failure_kind::UNSUPPORTED => Some(FailureKind::Unsupported),
        failure_kind::NOT_IMPLEMENTED => Some(FailureKind::NotImplemented),
        failure_kind::PANIC => Some(FailureKind::Panic),
        _ => None,
    }
}

/// A line or column as the ABI carries it.
fn position(v: u32) -> i32 {
    i32::try_from(v).unwrap_or(i32::MAX)
}

/// A `pure_span` over a span the buffer owns.
fn span(source: Option<&SourceInfo>) -> PureSpan {
    source.map_or(PureSpan::ABSENT, |s| PureSpan {
        source: PureString::of(&s.source),
        start_line: position(s.start_line),
        start_column: position(s.start_column),
        end_line: position(s.end_line),
        end_column: position(s.end_column),
    })
}

/// A `pure_frame` over a frame the buffer owns.
fn frame(f: &StackFrame) -> PureFrame {
    PureFrame {
        function: PureString::of(&f.function_name),
        span: span(Some(&f.source)),
    }
}

fn store(payload: ExceptionPayload) {
    let optional = |s: Option<&str>| s.map_or(PureString::ABSENT, PureString::of);
    let mut stored = Box::new(Stored {
        view: PureFailure {
            message: PureString::ABSENT,
            info: PureString::ABSENT,
            source: PureSpan::ABSENT,
            constraint_id: PureString::ABSENT,
            constraint_kind: PureString::ABSENT,
            owner: PureString::ABSENT,
            call_stack: std::ptr::null(),
            call_stack_len: 0,
            kind: kind_code(payload.kind),
        },
        payload,
        frames: Vec::new(),
    });
    let p = &stored.payload;
    let view = PureFailure {
        message: PureString::of(&p.message),
        info: PureString::of(&p.info),
        source: span(p.source.as_ref()),
        constraint_id: optional(p.constraint_id.as_deref()),
        constraint_kind: optional(p.constraint_kind),
        owner: optional(p.owner_fqn.as_deref()),
        ..stored.view
    };
    let frames: Vec<PureFrame> = p.call_stack.iter().map(frame).collect();
    stored.view = view;
    stored.frames = frames;
    if !stored.frames.is_empty() {
        stored.view.call_stack = stored.frames.as_ptr();
        stored.view.call_stack_len = stored.frames.len();
    }
    LAST.with(|last| *last.borrow_mut() = Some(stored));
}

/// The last call on this thread succeeded.
pub fn clear() {
    LAST.with(|last| *last.borrow_mut() = None);
}

/// The last call on this thread failed with `failure`.
pub fn record(failure: &PureException) {
    store(payload_of(failure));
}

/// The text a panic carried, when it carried one.
fn panic_text(panic: &(dyn Any + Send)) -> String {
    if let Some(s) = panic.downcast_ref::<&str>() {
        (*s).to_owned()
    } else if let Some(s) = panic.downcast_ref::<String>() {
        s.clone()
    } else {
        "no message".to_owned()
    }
}

/// The last call on this thread panicked during `what`.
pub fn record_panic(what: &str, panic: &(dyn Any + Send)) {
    let info = format!(
        "the evaluator panicked during {what}: {}",
        panic_text(panic)
    );
    store(ExceptionPayload {
        message: format!("{}: {info}", FailureKind::Panic.name()),
        info,
        kind: FailureKind::Panic,
        source: None,
        call_stack: Vec::new(),
        constraint_id: None,
        constraint_kind: None,
        owner_fqn: None,
    });
}

/// The failure of the last call on this thread, or NULL. The pointer is
/// valid until the thread-local buffer is next replaced or cleared.
#[must_use]
pub fn last() -> *const PureFailure {
    LAST.with(|last| {
        last.borrow()
            .as_deref()
            .map_or(std::ptr::null(), |stored| std::ptr::from_ref(&stored.view))
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use legend_pure_evaluator::error::{PureExceptionKind, PureRuntimeError};

    fn text(s: PureString) -> Option<String> {
        if s.ptr.is_null() {
            return None;
        }
        // SAFETY: a present string of the stored failure is `len` bytes
        // of the buffer, alive until the next record on this thread.
        let bytes = unsafe { std::slice::from_raw_parts(s.ptr, s.len) };
        Some(String::from_utf8(bytes.to_vec()).expect("UTF-8"))
    }

    fn positions(s: &PureSpan) -> [i32; 4] {
        [s.start_line, s.start_column, s.end_line, s.end_column]
    }

    #[test]
    fn a_failure_crosses_with_its_span_and_its_frames_as_spans_and_names() {
        let at = |line, column| SourceInfo::new("/a.pure", line, column, line + 1, column + 2);
        record(&PureException::new(
            PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError("x".into())),
            Some(at(7, 3)),
            vec![
                StackFrame {
                    function_name: "outer".into(),
                    source: at(1, 1),
                },
                StackFrame {
                    function_name: "a::rather::long::function::name::past::inline".into(),
                    source: at(5, 2),
                },
            ],
        ));
        // SAFETY: just recorded on this thread.
        let failure = unsafe { *last() };
        assert_eq!(failure.kind, failure_kind::EXECUTION_ERROR);
        assert_eq!(text(failure.message).as_deref(), Some("ExecutionError: x"));
        assert_eq!(text(failure.source.source).as_deref(), Some("/a.pure"));
        assert_eq!(positions(&failure.source), [7, 3, 8, 5]);
        assert_eq!(failure.call_stack_len, 2);
        // SAFETY: `call_stack_len` frames of the stored failure.
        let frames = unsafe { std::slice::from_raw_parts(failure.call_stack, 2) };
        assert_eq!(
            text(frames[0].function).as_deref(),
            Some("a::rather::long::function::name::past::inline"),
            "innermost first"
        );
        assert_eq!(positions(&frames[0].span), [5, 2, 6, 4]);
        assert_eq!(text(frames[1].function).as_deref(), Some("outer"));
        assert_eq!(text(frames[1].span.source).as_deref(), Some("/a.pure"));
        assert_eq!(positions(&frames[1].span), [1, 1, 2, 3]);
        clear();
        assert!(last().is_null());
    }

    #[test]
    fn every_kind_has_its_code_and_back() {
        for kind in [
            FailureKind::ExecutionError,
            FailureKind::AssertionFailed,
            FailureKind::ConstraintViolation,
            FailureKind::FunctionNotFound,
            FailureKind::Unsupported,
            FailureKind::NotImplemented,
            FailureKind::Panic,
        ] {
            assert_eq!(kind_of_code(kind_code(kind)), Some(kind));
        }
        assert_eq!(kind_of_code(-1), None);
        assert_eq!(kind_of_code(7), None);
    }
}
