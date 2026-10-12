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

//! The flat data of the ABI, one Rust type per struct and one constant
//! per enumerator of `include/pure_rust.h`. The header is the contract;
//! this module mirrors it field for field, and the size assertions at
//! the end pin the two to the same layout.

use std::ffi::c_void;

use legend_pure_evaluator::bridge::description::kind;

/// The ABI version this crate implements: `PURE_ABI_VERSION` in the header.
pub const ABI_VERSION: u32 = 1;

/// `pure_status`.
pub mod status {
    /// `PURE_STATUS_OK`.
    pub const OK: i32 = 0;
    /// `PURE_STATUS_FAILED`.
    pub const FAILED: i32 = 1;
    /// `PURE_STATUS_ABSENT` (callbacks only).
    pub const ABSENT: i32 = 2;
}

/// `PURE_KIND_MAP`: the one leaf kind that is not a description kind.
pub const KIND_MAP: i32 = 14;

/// `pure_failure.kind`.
pub mod failure_kind {
    /// `PURE_FAILURE_EXECUTION_ERROR`.
    pub const EXECUTION_ERROR: i32 = 0;
    /// `PURE_FAILURE_ASSERTION_FAILED`.
    pub const ASSERTION_FAILED: i32 = 1;
    /// `PURE_FAILURE_CONSTRAINT_VIOLATION`.
    pub const CONSTRAINT_VIOLATION: i32 = 2;
    /// `PURE_FAILURE_FUNCTION_NOT_FOUND`.
    pub const FUNCTION_NOT_FOUND: i32 = 3;
    /// `PURE_FAILURE_UNSUPPORTED`.
    pub const UNSUPPORTED: i32 = 4;
    /// `PURE_FAILURE_NOT_IMPLEMENTED`.
    pub const NOT_IMPLEMENTED: i32 = 5;
    /// `PURE_FAILURE_PANIC`.
    pub const PANIC: i32 = 6;
}

/// `PURE_CROSSING_*`.
pub mod crossing {
    /// `PURE_CROSSING_ELEMENT`.
    pub const ELEMENT: i32 = 0;
    /// `PURE_CROSSING_ENUM_VALUE`.
    pub const ENUM_VALUE: i32 = 1;
    /// `PURE_CROSSING_HOST_NODE`.
    pub const HOST_NODE: i32 = 2;
    /// `PURE_CROSSING_RUST_OBJECT`.
    pub const RUST_OBJECT: i32 = 3;
    /// `PURE_CROSSING_PRIMITIVE`.
    pub const PRIMITIVE: i32 = 4;
}

/// `pure_string`: UTF-8 bytes with their count. A NULL pointer is an
/// absent string; a non-NULL pointer with a count of zero is empty.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureString {
    /// The bytes, not NUL-terminated.
    pub ptr: *const u8,
    /// How many bytes.
    pub len: usize,
}

impl PureString {
    /// The absent string.
    pub const ABSENT: Self = Self {
        ptr: std::ptr::null(),
        len: 0,
    };

    /// A string over Rust bytes that outlive the view.
    #[must_use]
    pub fn of(s: &str) -> Self {
        Self {
            ptr: s.as_ptr(),
            len: s.len(),
        }
    }
}

/// `pure_description`: one host node, described. The order of the
/// fields is the header's (8-byte fields first).
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureDescription {
    /// The host's id, or a Rust handle for `RUST_OBJECT`.
    pub id: i64,
    /// The lambda definition a per-evaluation lambda stands for; -1 when none.
    pub definition: i64,
    /// The value of an `INTEGER` that fits 64 bits.
    pub integer: i64,
    /// The value of a `FLOAT`.
    pub real: f64,
    /// Classifier user path.
    pub classifier: PureString,
    /// The node's own name.
    pub name: PureString,
    /// User path of a packageable element.
    pub user_path: PureString,
    /// Text payload of a primitive.
    pub payload: PureString,
    /// Subsecond digits at subsecond precision.
    pub subsecond: PureString,
    /// Source id of the node's span; absent when there is no anchor.
    pub source_id: PureString,
    /// One of the kind constants.
    pub kind: i32,
    /// Year.
    pub year: i32,
    /// Month, 0 when absent.
    pub month: i32,
    /// Day, 0 when absent.
    pub day: i32,
    /// Hour, 0 when absent.
    pub hour: i32,
    /// Minute, 0 when absent.
    pub minute: i32,
    /// Second, 0 when absent.
    pub second: i32,
    /// Precision code.
    pub precision: i32,
    /// Start line of the span.
    pub line: i32,
    /// Start column of the span.
    pub column: i32,
    /// End line of the span.
    pub end_line: i32,
    /// End column of the span.
    pub end_column: i32,
}

impl Default for PureDescription {
    /// A description of nothing: kind 0, no definition, every string absent.
    fn default() -> Self {
        Self {
            id: 0,
            definition: -1,
            integer: 0,
            real: 0.0,
            classifier: PureString::ABSENT,
            name: PureString::ABSENT,
            user_path: PureString::ABSENT,
            payload: PureString::ABSENT,
            subsecond: PureString::ABSENT,
            source_id: PureString::ABSENT,
            kind: kind::BOOLEAN,
            year: 0,
            month: 0,
            day: 0,
            hour: 0,
            minute: 0,
            second: 0,
            precision: 0,
            line: 0,
            column: 0,
            end_line: 0,
            end_column: 0,
        }
    }
}

/// `pure_slot`: a Pure value of any multiplicity, as descriptions.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureSlot {
    /// The descriptions; may be NULL only when `count` is 0.
    pub items: *const PureDescription,
    /// How many.
    pub count: usize,
}

/// `pure_property`: one property of an object the host allocates.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureProperty {
    /// The property's name.
    pub name: PureString,
    /// Its values.
    pub values: PureSlot,
}

/// `pure_leaf`: one value on its way out.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureLeaf {
    /// Boolean (0 or 1), Integer, host node id or Rust handle.
    pub integer: i64,
    /// Float.
    pub real: f64,
    /// Decimal text, string, subsecond digits, element path, enumeration
    /// path or classifier path, by kind.
    pub text: PureString,
    /// The value's name, for an enum value.
    pub name: PureString,
    /// Map entries: `2 * entry_count` leaves, each key followed by its value.
    pub entries: *const PureLeaf,
    /// How many map entries.
    pub entry_count: usize,
    /// One of the kind constants.
    pub kind: i32,
    /// Precision code of a date or time.
    pub precision: i32,
    /// Year.
    pub year: i32,
    /// Month.
    pub month: i32,
    /// Day.
    pub day: i32,
    /// Hour.
    pub hour: i32,
    /// Minute.
    pub minute: i32,
    /// Second.
    pub second: i32,
}

impl PureLeaf {
    /// A leaf of `kind` with nothing else set.
    #[must_use]
    pub const fn of_kind(kind: i32) -> Self {
        Self {
            integer: 0,
            real: 0.0,
            text: PureString::ABSENT,
            name: PureString::ABSENT,
            entries: std::ptr::null(),
            entry_count: 0,
            kind,
            precision: 0,
            year: 0,
            month: 0,
            day: 0,
            hour: 0,
            minute: 0,
            second: 0,
        }
    }
}

/// `pure_result`: `count` leaves Rust owns.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureResult {
    /// The leaves.
    pub leaves: *const PureLeaf,
    /// How many.
    pub count: usize,
}

/// `pure_span`: a span of the host's sources; an absent `source` is no
/// span, every position then 0.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureSpan {
    /// The source id.
    pub source: PureString,
    /// Start line, the anchor's.
    pub start_line: i32,
    /// Start column, the anchor's.
    pub start_column: i32,
    /// End line.
    pub end_line: i32,
    /// End column.
    pub end_column: i32,
}

impl PureSpan {
    /// No span.
    pub const ABSENT: Self = Self {
        source: PureString::ABSENT,
        start_line: 0,
        start_column: 0,
        end_line: 0,
        end_column: 0,
    };
}

/// `pure_frame`: one frame of a call stack.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureFrame {
    /// The called function's name.
    pub function: PureString,
    /// The span of the call.
    pub span: PureSpan,
}

/// `pure_failure`: what `pure_last_failure` describes.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureFailure {
    /// The human-readable line: the kind's name, then the text.
    pub message: PureString,
    /// The text alone.
    pub info: PureString,
    /// Where the failure happened; absent when it has no span.
    pub source: PureSpan,
    /// Constraint id, for a constraint violation.
    pub constraint_id: PureString,
    /// `CLASS`, `PRE` or `POST`, for a constraint violation.
    pub constraint_kind: PureString,
    /// The constraint owner's path, for a constraint violation.
    pub owner: PureString,
    /// Frames, innermost first.
    pub call_stack: *const PureFrame,
    /// How many frames.
    pub call_stack_len: usize,
    /// One of the failure kind constants.
    pub kind: i32,
}

/// `pure_timings`: where the last `pure_start` on this thread spent its time.
#[repr(C)]
#[derive(Debug, Clone, Copy, Default)]
pub struct PureTimings {
    /// Lowering, nanoseconds.
    pub lower_ns: u64,
    /// Running, nanoseconds.
    pub run_ns: u64,
    /// Host reads made while the call ran.
    pub run_host_reads: u64,
    /// Host reads made by the lowering.
    pub lower_host_reads: u64,
}

/// `pure_context`: opaque to the host.
#[repr(C)]
pub struct PureContext {
    _opaque: [u8; 0],
}

/// `pure_reply`: opaque to the host.
#[repr(C)]
pub struct PureReply {
    _opaque: [u8; 0],
}

/// `pure_property_values_fn` and `pure_captured_values_fn`.
pub type PropertyValuesFn =
    unsafe extern "C" fn(*mut c_void, i64, *const u8, usize, *mut PureReply) -> i32;

/// `pure_keys_fn`.
pub type KeysFn = unsafe extern "C" fn(*mut c_void, i64, *mut PureReply) -> i32;

/// `pure_forget_fn`.
pub type ForgetFn = unsafe extern "C" fn(*mut c_void, *const i64, usize);

/// `pure_host_table`: the callbacks a host hands a context, copied by
/// `pure_context_new`. A missing pointer is `None`.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PureHostTable {
    /// Handed back to every callback.
    pub host: *mut c_void,
    /// Property values of a node, required.
    pub property_values: Option<PropertyValuesFn>,
    /// Captured values of a closure node, required.
    pub captured_values: Option<PropertyValuesFn>,
    /// Property keys of a node, required.
    pub keys: Option<KeysFn>,
    /// Ids no longer held; may be `None`.
    pub forget: Option<ForgetFn>,
}

#[cfg(target_pointer_width = "64")]
const _: () = {
    use std::mem::size_of;
    assert!(size_of::<PureString>() == 16);
    assert!(size_of::<PureDescription>() == 176);
    assert!(size_of::<PureSlot>() == 16);
    assert!(size_of::<PureProperty>() == 32);
    assert!(size_of::<PureLeaf>() == 96);
    assert!(size_of::<PureResult>() == 16);
    assert!(size_of::<PureSpan>() == 32);
    assert!(size_of::<PureFrame>() == 48);
    assert!(size_of::<PureFailure>() == 136);
    assert!(size_of::<PureTimings>() == 32);
    assert!(size_of::<PureHostTable>() == 40);
};
