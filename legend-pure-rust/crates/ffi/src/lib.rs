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

//! The C ABI of the Rust Pure evaluator: one host surface for any
//! program that can call C.
//!
//! The contract is the hand-written header `include/pure_rust.h`; this
//! crate implements it over the evaluator crate's `bridge` module and
//! nothing else. Every export is listed in [`EXPORTS`], which a test
//! compares with the header's declarations and with the `no_mangle`
//! functions of this crate, in both directions. The rules the header
//! states (only C data crosses, each side frees its own, a status plus a
//! fetchable failure, no unwind across the boundary, one thread in a
//! context at a time, stable node ids until `forget`, callbacks only
//! from inside an entered call) are kept here: every export runs inside
//! a panic guard that poisons the context on a panic, and the host is
//! reached only through the function pointers it handed over
//! ([`host::FfiHost`]).

#![deny(missing_docs)]
#![deny(clippy::undocumented_unsafe_blocks)]

pub mod abi;
pub mod decode;
pub mod failure;
pub mod host;
pub mod materialize;

use std::any::Any;
use std::panic::{AssertUnwindSafe, catch_unwind};

use legend_pure_evaluator::bridge::context::BridgeContext;
use legend_pure_evaluator::bridge::failure::boundary_error;
use legend_pure_evaluator::bridge::host::{Crossing, crossing_of, description_to_value};
use legend_pure_evaluator::bridge::session::Session;
use legend_pure_evaluator::bridge::timings;
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::heap::ObjectHandle;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::model::PureModel;

use crate::abi::{
    ABI_VERSION, PureContext, PureDescription, PureFailure, PureHostTable, PureProperty, PureReply,
    PureResult, PureSlot, PureTimings, crossing, status,
};
use crate::host::{FfiHost, Reply};
use crate::materialize::ResultBox;

/// Every function this crate exports, as the header declares them.
pub const EXPORTS: &[&str] = &[
    "pure_abi_version",
    "pure_context_new",
    "pure_context_free",
    "pure_start",
    "pure_result_free",
    "pure_object_property_values",
    "pure_object_property_names",
    "pure_object_classifier",
    "pure_object_new",
    "pure_object_release",
    "pure_live_handle_count",
    "pure_crossing",
    "pure_last_timings",
    "pure_last_failure",
    "pure_reply_descriptions",
    "pure_reply_names",
    "pure_reply_fail",
];

/// A context as the crate holds it: the bridge context behind the
/// one-call-at-a-time guard.
type Context = Session<BridgeContext>;

/// The session behind a context pointer the host passed back.
///
/// # Safety
/// `ctx` is NULL or a pointer `pure_context_new` returned that
/// `pure_context_free` has not released.
///
/// # Errors
/// NULL.
unsafe fn session<'a>(ctx: *mut PureContext) -> Result<&'a Context, PureException> {
    if ctx.is_null() {
        return Err(boundary_error("the context is NULL"));
    }
    // SAFETY: per this function's contract the pointer came from
    // `Box::into_raw` in `pure_context_new` and is still live; a shared
    // reference is sound because a `Session` is only mutated through its
    // cells.
    Ok(unsafe { &*ctx.cast::<Context>().cast_const() })
}

/// The outcome of one guarded call.
type Outcome<T> = Result<Result<T, PureException>, Box<dyn Any + Send>>;

/// Record the outcome of a call on `session` in the thread's failure
/// slot: a panic poisons the session.
fn settle<T>(session: Option<&Context>, what: &str, outcome: Outcome<T>) -> Option<T> {
    match outcome {
        Ok(Ok(value)) => {
            failure::clear();
            Some(value)
        }
        Ok(Err(error)) => {
            failure::record(&error);
            None
        }
        Err(panic) => {
            if let Some(session) = session {
                session.poison();
            }
            failure::record_panic(what, panic.as_ref());
            None
        }
    }
}

/// A call that runs the evaluator: refused on a poisoned context and
/// while another call on it runs; a panic poisons the context.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`.
unsafe fn running<T>(
    ctx: *mut PureContext,
    what: &str,
    f: impl FnOnce(&mut BridgeContext) -> Result<T, PureException>,
) -> Option<T> {
    // SAFETY: this function's contract is `session`'s.
    let session = match unsafe { session(ctx) } {
        Ok(session) => session,
        Err(error) => {
            failure::record(&error);
            return None;
        }
    };
    let outcome = catch_unwind(AssertUnwindSafe(|| {
        session.live()?;
        let mut context = session.enter()?;
        f(&mut context)
    }));
    settle(Some(session), what, outcome)
}

/// A housekeeping call: refused only while another call on the context
/// runs, so a poisoned context can still be counted, released and freed.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`.
unsafe fn housekeeping<T>(
    ctx: *mut PureContext,
    what: &str,
    f: impl FnOnce(&mut BridgeContext) -> T,
) -> Option<T> {
    // SAFETY: this function's contract is `session`'s.
    let session = match unsafe { session(ctx) } {
        Ok(session) => session,
        Err(error) => {
            failure::record(&error);
            return None;
        }
    };
    let outcome = catch_unwind(AssertUnwindSafe(|| {
        let mut context = session.enter()?;
        Ok(f(&mut context))
    }));
    settle(Some(session), what, outcome)
}

/// The status of an outcome.
fn status_of<T>(outcome: Option<T>) -> i32 {
    if outcome.is_some() {
        status::OK
    } else {
        status::FAILED
    }
}

/// The Rust object behind a handle the host holds.
fn resolve_handle(context: &BridgeContext, handle: i64) -> Result<ObjectHandle, PureException> {
    context
        .resolve_handle(handle)
        .ok_or_else(|| boundary_error(format!("Stale or unknown handle: {handle}")))
}

/// Write a value through an out pointer.
///
/// # Safety
/// `out` is NULL or points to writable storage for a `T`.
unsafe fn write_out<T>(out: *mut T, value: T) -> Result<(), PureException> {
    if out.is_null() {
        return Err(boundary_error("the out pointer is NULL"));
    }
    // SAFETY: per this function's contract, `out` is non-NULL here and
    // points to writable storage for a `T`.
    unsafe { out.write(value) };
    Ok(())
}

/// Refuse a NULL out pointer before any work is done for it, so a
/// result or a handle is never made for nobody.
fn ensure_out<T>(out: *const T) -> Result<(), PureException> {
    if out.is_null() {
        return Err(boundary_error("the out pointer is NULL"));
    }
    Ok(())
}

/// The ABI version this library implements.
#[unsafe(no_mangle)]
pub extern "C" fn pure_abi_version() -> u32 {
    ABI_VERSION
}

/// A context over the bootstrap model and the native registry, bound to
/// the callbacks in `table`, which is copied.
///
/// # Safety
/// `table` is NULL or points to a readable `pure_host_table`; `out` is
/// NULL or points to writable storage for a pointer.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_context_new(
    table: *const PureHostTable,
    out: *mut *mut PureContext,
) -> i32 {
    let outcome = catch_unwind(AssertUnwindSafe(|| {
        if table.is_null() {
            return Err(boundary_error("the host table is NULL"));
        }
        // SAFETY: per this function's contract, `table` is non-NULL here
        // and points to a readable table, which is copied at once.
        let table = unsafe { table.read() };
        let host = FfiHost::new(&table)?;
        let context =
            BridgeContext::new(PureModel::bootstrapped(), NativeRegistry::standard(), host);
        let ptr = Box::into_raw(Box::new(Session::new(context))).cast::<PureContext>();
        // SAFETY: this function's contract is `write_out`'s for `out`.
        match unsafe { write_out(out, ptr) } {
            Ok(()) => Ok(()),
            Err(error) => {
                // SAFETY: `ptr` was just made by `Box::into_raw` above and
                // handed to no one.
                drop(unsafe { Box::from_raw(ptr.cast::<Context>()) });
                Err(error)
            }
        }
    }));
    status_of(settle(None, "context creation", outcome))
}

/// Release a context. Fails while a call on it is running; NULL does
/// nothing.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new` and is never passed again
/// once this returns `PURE_STATUS_OK`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_context_free(ctx: *mut PureContext) -> i32 {
    if ctx.is_null() {
        failure::clear();
        return status::OK;
    }
    let outcome = catch_unwind(AssertUnwindSafe(|| {
        // SAFETY: this function's contract is `session`'s.
        unsafe { session(ctx) }?.ensure_idle()?;
        // SAFETY: the pointer came from `Box::into_raw` in
        // `pure_context_new`, no call is running (so no view of the
        // context and no reference into the session exists), and the host
        // never passes it again.
        drop(unsafe { Box::from_raw(ctx.cast::<Context>()) });
        Ok(())
    }));
    status_of(settle(None, "context release", outcome))
}

/// Run a function node over argument slots; the result is Rust-owned
/// until `pure_result_free`.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `function` is NULL or points
/// to a readable description; `args` is NULL or points to `argc`
/// readable slots; every string and array they reference is readable
/// for the call; `out` is NULL or points to writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_start(
    ctx: *mut PureContext,
    function: *const PureDescription,
    args: *const PureSlot,
    argc: usize,
    out: *mut *mut PureResult,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out)?;
        if function.is_null() {
            return Err(boundary_error("start: the function is NULL"));
        }
        // SAFETY: per this function's contract, `function` is non-NULL
        // here and readable for the call.
        let described = unsafe { decode::description(&*function) }?;
        let function = description_to_value(context.shared(), described)?;
        let slots: &[PureSlot] = if argc == 0 {
            &[]
        } else if args.is_null() {
            return Err(boundary_error(format!(
                "start: a NULL array of {argc} arguments"
            )));
        } else {
            // SAFETY: per this function's contract, `args` is non-NULL
            // here and points to `argc` slots readable for the call.
            unsafe { std::slice::from_raw_parts(args, argc) }
        };
        let mut values = Vec::with_capacity(slots.len());
        for (i, slot) in slots.iter().enumerate() {
            // SAFETY: this function's contract covers every slot.
            values.push(unsafe {
                decode::slot(context.shared(), slot, &format!("start: argument {i}"))
            }?);
        }
        let value = context.start(&function, &values)?;
        let result = ResultBox::of_value(context.shared(), &value)?;
        // SAFETY: this function's contract is `write_out`'s for `out`.
        unsafe { write_out(out, ResultBox::into_raw(result)) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "start", call) };
    status_of(outcome)
}

/// Release a result. NULL does nothing.
///
/// # Safety
/// `result` is NULL or a pointer a function of this ABI returned that
/// has not been released.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_result_free(result: *mut PureResult) {
    if result.is_null() {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| {
        // SAFETY: per this function's contract, `result` came from
        // `ResultBox::into_raw` and is released once.
        drop(unsafe { ResultBox::from_raw(result) });
    }));
}

/// Every value of a property of a Rust object the host holds.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `name` is NULL or `name_len`
/// readable bytes; `out` is NULL or points to writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_object_property_values(
    ctx: *mut PureContext,
    handle: i64,
    name: *const u8,
    name_len: usize,
    out: *mut *mut PureResult,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out)?;
        // SAFETY: this function's contract is `decode::text`'s for `name`.
        let name = unsafe { decode::text(name, name_len, "property name") }?;
        let object = resolve_handle(context, handle)?;
        let values = context.property_values(&object, name)?;
        let collected: Vec<Value> = values.iter().cloned().collect();
        let result = ResultBox::of_value(context.shared(), &Value::from_vec(collected))?;
        // SAFETY: this function's contract is `write_out`'s for `out`.
        unsafe { write_out(out, ResultBox::into_raw(result)) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "property values", call) };
    status_of(outcome)
}

/// The property names of a Rust object the host holds, as string leaves.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `out` is NULL or points to
/// writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_object_property_names(
    ctx: *mut PureContext,
    handle: i64,
    out: *mut *mut PureResult,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out)?;
        let object = resolve_handle(context, handle)?;
        let names = context.property_names(&object)?;
        let result = ResultBox::of_strings(names.into_iter().map(|n| n.to_string()));
        // SAFETY: this function's contract is `write_out`'s for `out`.
        unsafe { write_out(out, ResultBox::into_raw(result)) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "property names", call) };
    status_of(outcome)
}

/// The classifier user path of a Rust object the host holds, as one
/// string leaf.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `out` is NULL or points to
/// writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_object_classifier(
    ctx: *mut PureContext,
    handle: i64,
    out: *mut *mut PureResult,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out)?;
        let object = resolve_handle(context, handle)?;
        let classifier = context.classifier(&object)?;
        let result = ResultBox::of_strings([classifier]);
        // SAFETY: this function's contract is `write_out`'s for `out`.
        unsafe { write_out(out, ResultBox::into_raw(result)) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "classifier", call) };
    status_of(outcome)
}

/// Allocate a Rust object from a classifier path and property slots,
/// and hand the host its handle.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `classifier` is NULL or
/// `classifier_len` readable bytes; `properties` is NULL or points to
/// `count` readable properties whose strings and slots are readable for
/// the call; `out_handle` is NULL or points to writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_object_new(
    ctx: *mut PureContext,
    classifier: *const u8,
    classifier_len: usize,
    properties: *const PureProperty,
    count: usize,
    out_handle: *mut i64,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out_handle)?;
        // SAFETY: this function's contract is `decode::text`'s for
        // `classifier`.
        let classifier = unsafe { decode::text(classifier, classifier_len, "new: classifier") }?;
        let properties: &[PureProperty] = if count == 0 {
            &[]
        } else if properties.is_null() {
            return Err(boundary_error(format!(
                "new: a NULL array of {count} properties"
            )));
        } else {
            // SAFETY: per this function's contract, `properties` is
            // non-NULL here and points to `count` readable properties.
            unsafe { std::slice::from_raw_parts(properties, count) }
        };
        let mut props = Vec::with_capacity(properties.len());
        for property in properties {
            // SAFETY: this function's contract covers each property's name.
            let name = unsafe { decode::optional(property.name, "new: property name") }?
                .ok_or_else(|| boundary_error("new: an absent property name"))?;
            // SAFETY: this function's contract covers each property's slot.
            let value = unsafe {
                decode::slot(
                    context.shared(),
                    &property.values,
                    &format!("new: `{name}`"),
                )
            }?;
            props.push((
                name,
                value.to_collection().iter().cloned().collect::<Vec<_>>(),
            ));
        }
        let handle = context.new_object(classifier, &props)?;
        // SAFETY: this function's contract is `write_out`'s for `out_handle`.
        unsafe { write_out(out_handle, handle) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "new", call) };
    status_of(outcome)
}

/// Drop the reference the host held for a handle. Idempotent.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_object_release(ctx: *mut PureContext, handle: i64) {
    if ctx.is_null() {
        return;
    }
    // SAFETY: this function's contract is `housekeeping`'s.
    let _ = unsafe { housekeeping(ctx, "release", |context| context.release(handle)) };
}

/// Live handles in both directions; -1 while a call on the context runs.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_live_handle_count(ctx: *mut PureContext) -> i64 {
    if ctx.is_null() {
        return 0;
    }
    // SAFETY: this function's contract is `housekeeping`'s.
    unsafe {
        housekeeping(ctx, "live handle count", |context| {
            i64::try_from(context.live_handle_count()).unwrap_or(i64::MAX)
        })
    }
    .unwrap_or(-1)
}

/// Which Rust shape a described node becomes.
///
/// # Safety
/// `ctx` is NULL or a live context from `pure_context_new`; `node` is NULL or points to a
/// readable description whose strings are readable for the call; `out`
/// is NULL or points to writable storage.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_crossing(
    ctx: *mut PureContext,
    node: *const PureDescription,
    out: *mut i32,
) -> i32 {
    let call = |context: &mut BridgeContext| {
        ensure_out(out)?;
        if node.is_null() {
            return Err(boundary_error("crossing: the node is NULL"));
        }
        // SAFETY: per this function's contract, `node` is non-NULL here
        // and readable for the call.
        let described = unsafe { decode::description(&*node) }?;
        let value = description_to_value(context.shared(), described)?;
        let code = match crossing_of(&value) {
            Crossing::Element => crossing::ELEMENT,
            Crossing::EnumValue => crossing::ENUM_VALUE,
            Crossing::HostNode => crossing::HOST_NODE,
            Crossing::RustObject => crossing::RUST_OBJECT,
            Crossing::Primitive => crossing::PRIMITIVE,
        };
        // SAFETY: this function's contract is `write_out`'s for `out`.
        unsafe { write_out(out, code) }
    };
    // SAFETY: this function's contract is `running`'s.
    let outcome = unsafe { running(ctx, "crossing", call) };
    status_of(outcome)
}

/// Where the last `pure_start` on this thread spent its time.
///
/// # Safety
/// `out` is NULL or points to writable storage for a `pure_timings`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_last_timings(out: *mut PureTimings) {
    if out.is_null() {
        return;
    }
    let t = timings::last();
    let timings = PureTimings {
        lower_ns: t.lower_ns,
        run_ns: t.run_ns,
        run_host_reads: t.run_host_reads,
        lower_host_reads: t.lower_host_reads,
    };
    // SAFETY: per this function's contract, `out` is non-NULL here and
    // points to writable storage.
    unsafe { out.write(timings) };
}

/// The failure of the last status-returning call on this thread, or NULL.
#[unsafe(no_mangle)]
pub extern "C" fn pure_last_failure() -> *const PureFailure {
    failure::last()
}

/// Push descriptions into a reply, inside a callback.
///
/// # Safety
/// `reply` is the pointer the running callback received; `items` is
/// NULL or points to `count` readable descriptions whose strings are
/// readable for the push.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_reply_descriptions(
    reply: *mut PureReply,
    items: *const PureDescription,
    count: usize,
) -> i32 {
    // SAFETY: this function's contract is `Reply::from_ptr`'s.
    let Ok(reply) = (unsafe { Reply::from_ptr(reply) }) else {
        return status::FAILED;
    };
    // SAFETY: this function's contract is `Reply::push_descriptions`'.
    let outcome = catch_unwind(AssertUnwindSafe(|| unsafe {
        reply.push_descriptions(items, count)
    }));
    reply_status(reply, "pushing descriptions", outcome)
}

/// Push names into a reply, inside a callback.
///
/// # Safety
/// `reply` is the pointer the running callback received; `items` is
/// NULL or points to `count` readable strings, each readable for the
/// push.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_reply_names(
    reply: *mut PureReply,
    items: *const abi::PureString,
    count: usize,
) -> i32 {
    // SAFETY: this function's contract is `Reply::from_ptr`'s.
    let Ok(reply) = (unsafe { Reply::from_ptr(reply) }) else {
        return status::FAILED;
    };
    // SAFETY: this function's contract is `Reply::push_names`'.
    let outcome = catch_unwind(AssertUnwindSafe(|| unsafe {
        reply.push_names(items, count)
    }));
    reply_status(reply, "pushing names", outcome)
}

/// Record why a callback failed.
///
/// # Safety
/// `reply` is the pointer the running callback received; `message` is
/// NULL or `message_len` readable bytes.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn pure_reply_fail(
    reply: *mut PureReply,
    kind: i32,
    message: *const u8,
    message_len: usize,
) -> i32 {
    // SAFETY: this function's contract is `Reply::from_ptr`'s.
    let Ok(reply) = (unsafe { Reply::from_ptr(reply) }) else {
        return status::FAILED;
    };
    let outcome = catch_unwind(AssertUnwindSafe(|| {
        // SAFETY: this function's contract is `decode::text`'s for `message`.
        let message = unsafe { decode::text(message, message_len, "failure message") }?;
        reply.fail(kind, message);
        Ok(())
    }));
    reply_status(reply, "recording a failure", outcome)
}

/// The status of a reply push: a panic or an error is noted on the reply
/// (the failure slot of the thread is the outer call's).
fn reply_status(reply: &mut Reply, what: &str, outcome: Outcome<()>) -> i32 {
    match outcome {
        Ok(Ok(())) => status::OK,
        Ok(Err(error)) => {
            reply.note(error);
            status::FAILED
        }
        Err(panic) => {
            reply.note(boundary_error(format!(
                "{what}: the evaluator panicked: {}",
                panic
                    .downcast_ref::<&str>()
                    .map_or_else(|| "no message".to_owned(), |s| (*s).to_owned())
            )));
            status::FAILED
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::abi::{PureString, failure_kind};
    use std::ffi::c_void;

    unsafe extern "C" fn absent(
        _host: *mut c_void,
        _id: i64,
        _name: *const u8,
        _len: usize,
        _reply: *mut PureReply,
    ) -> i32 {
        status::ABSENT
    }

    unsafe extern "C" fn no_keys(_host: *mut c_void, _id: i64, _reply: *mut PureReply) -> i32 {
        status::ABSENT
    }

    fn table() -> PureHostTable {
        PureHostTable {
            host: std::ptr::null_mut(),
            property_values: Some(absent),
            captured_values: Some(absent),
            keys: Some(no_keys),
            forget: None,
        }
    }

    fn last_message() -> String {
        let failure = pure_last_failure();
        assert!(!failure.is_null(), "a failure was expected");
        // SAFETY: the pointer was just returned non-NULL by
        // `pure_last_failure` and nothing has replaced it since.
        let failure = unsafe { &*failure };
        // SAFETY: the message of a stored failure is always present and
        // points into the same Rust-owned buffer.
        unsafe { decode::text(failure.message.ptr, failure.message.len, "message") }
            .unwrap()
            .to_owned()
    }

    fn last_kind() -> i32 {
        // SAFETY: as in `last_message`.
        unsafe { (*pure_last_failure()).kind }
    }

    #[test]
    fn a_null_context_is_a_failure_not_a_dereference() {
        // SAFETY: NULL is never dereferenced.
        unsafe {
            assert!(session(std::ptr::null_mut()).is_err());
            assert_eq!(pure_context_free(std::ptr::null_mut()), status::OK);
            assert_eq!(pure_live_handle_count(std::ptr::null_mut()), 0);
            pure_object_release(std::ptr::null_mut(), 1);
            let mut out = std::ptr::null_mut();
            assert_eq!(
                pure_start(
                    std::ptr::null_mut(),
                    std::ptr::null(),
                    std::ptr::null(),
                    0,
                    &raw mut out
                ),
                status::FAILED
            );
        }
        assert_eq!(last_message(), "ExecutionError: the context is NULL");
    }

    #[test]
    fn a_table_missing_a_required_callback_is_refused() {
        let mut table = table();
        table.keys = None;
        let mut ctx = std::ptr::null_mut();
        // SAFETY: both pointers are to live locals.
        let status = unsafe { pure_context_new(&raw const table, &raw mut ctx) };
        assert_eq!(status, status::FAILED);
        assert!(ctx.is_null());
        assert!(
            last_message().contains("`keys` is NULL"),
            "{}",
            last_message()
        );
    }

    #[test]
    fn a_panic_inside_a_guarded_call_poisons_the_context_and_the_next_call_reports_it() {
        let table = table();
        let mut ctx = std::ptr::null_mut();
        // SAFETY: both pointers are to live locals.
        let created = unsafe { pure_context_new(&raw const table, &raw mut ctx) };
        assert_eq!(created, status::OK);
        assert!(pure_last_failure().is_null());

        // SAFETY: `ctx` was just returned by `pure_context_new`.
        let outcome: Option<()> =
            unsafe { running(ctx, "a test", |_| panic!("boom: a panic inside an export")) };
        assert!(outcome.is_none());
        assert_eq!(last_kind(), failure_kind::PANIC);
        assert!(last_message().contains("boom"), "{}", last_message());

        let function = PureDescription::default();
        let mut out = std::ptr::null_mut();
        // SAFETY: `ctx` is live and the other pointers are to live locals.
        let status =
            unsafe { pure_start(ctx, &raw const function, std::ptr::null(), 0, &raw mut out) };
        assert_eq!(status, status::FAILED);
        assert!(out.is_null());
        assert_eq!(last_kind(), failure_kind::EXECUTION_ERROR);
        assert!(
            last_message().contains("poisoned by an earlier panic"),
            "{}",
            last_message()
        );

        // SAFETY: `ctx` is live; housekeeping still works on a poisoned context.
        unsafe {
            assert_eq!(pure_live_handle_count(ctx), 0);
            pure_object_release(ctx, 7);
            assert_eq!(pure_context_free(ctx), status::OK);
        }
    }

    #[test]
    fn a_reply_refuses_a_null_pointer_and_an_absent_name() {
        // SAFETY: NULL is never dereferenced.
        unsafe {
            assert_eq!(
                pure_reply_descriptions(std::ptr::null_mut(), std::ptr::null(), 0),
                status::FAILED
            );
            assert_eq!(
                pure_reply_fail(std::ptr::null_mut(), 0, std::ptr::null(), 0),
                status::FAILED
            );
        }
        let mut reply = Reply::new("a test".to_owned());
        let names = [PureString::ABSENT];
        let ptr = std::ptr::from_mut(&mut reply).cast::<PureReply>();
        // SAFETY: `ptr` is a live local reply and `names` a live local array.
        let status = unsafe { pure_reply_names(ptr, names.as_ptr(), names.len()) };
        assert_eq!(status, status::FAILED);
        let failure = reply.into_names(status::OK).unwrap_err();
        assert!(failure.to_string().contains("an absent name"), "{failure}");
    }
}
