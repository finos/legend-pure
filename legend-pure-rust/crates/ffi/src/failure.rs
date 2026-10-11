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

use legend_pure_evaluator::bridge::failure::{ExceptionPayload, payload_of};
use legend_pure_evaluator::error::PureException;

use crate::abi::{PureFailure, PureString, failure_kind};

/// The buffer behind a `pure_failure`: the view first, then what it
/// points into. Strings moved into the vectors keep their heap buffers.
#[repr(C)]
struct Stored {
    view: PureFailure,
    texts: Vec<Option<String>>,
    frames: Vec<String>,
    stack: Vec<PureString>,
}

thread_local! {
    static LAST: RefCell<Option<Box<Stored>>> = const { RefCell::new(None) };
}

fn kind_code(kind: &str) -> i32 {
    match kind {
        "ASSERTION_FAILED" => failure_kind::ASSERTION_FAILED,
        "CONSTRAINT_VIOLATION" => failure_kind::CONSTRAINT_VIOLATION,
        "FUNCTION_NOT_FOUND" => failure_kind::FUNCTION_NOT_FOUND,
        "UNSUPPORTED" => failure_kind::UNSUPPORTED,
        "NOT_IMPLEMENTED" => failure_kind::NOT_IMPLEMENTED,
        _ => failure_kind::EXECUTION_ERROR,
    }
}

fn store(kind: i32, payload: ExceptionPayload) {
    let texts = vec![
        Some(payload.message),
        Some(payload.info),
        payload.source_info,
        payload.constraint_id,
        payload.constraint_kind.map(str::to_owned),
        payload.owner_fqn,
    ];
    let mut stored = Box::new(Stored {
        view: PureFailure {
            message: PureString::ABSENT,
            info: PureString::ABSENT,
            source: PureString::ABSENT,
            constraint_id: PureString::ABSENT,
            constraint_kind: PureString::ABSENT,
            owner: PureString::ABSENT,
            call_stack: std::ptr::null(),
            call_stack_len: 0,
            kind,
        },
        texts,
        frames: payload.call_stack,
        stack: Vec::new(),
    });
    let view = |s: &Option<String>| s.as_deref().map_or(PureString::ABSENT, PureString::of);
    stored.view.message = view(&stored.texts[0]);
    stored.view.info = view(&stored.texts[1]);
    stored.view.source = view(&stored.texts[2]);
    stored.view.constraint_id = view(&stored.texts[3]);
    stored.view.constraint_kind = view(&stored.texts[4]);
    stored.view.owner = view(&stored.texts[5]);
    stored.stack = stored.frames.iter().map(|f| PureString::of(f)).collect();
    if !stored.stack.is_empty() {
        stored.view.call_stack = stored.stack.as_ptr();
        stored.view.call_stack_len = stored.stack.len();
    }
    LAST.with(|last| *last.borrow_mut() = Some(stored));
}

/// The last call on this thread succeeded.
pub fn clear() {
    LAST.with(|last| *last.borrow_mut() = None);
}

/// The last call on this thread failed with `failure`.
pub fn record(failure: &PureException) {
    let payload = payload_of(failure);
    store(kind_code(payload.kind), payload);
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
    store(
        failure_kind::PANIC,
        ExceptionPayload {
            message: format!("Panic: {info}"),
            info,
            kind: "PANIC",
            source_info: None,
            call_stack: Vec::new(),
            constraint_id: None,
            constraint_kind: None,
            owner_fqn: None,
        },
    );
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
