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

//! JNI boundary: the Rust side of the native methods of the Java class
//! `org.finos.legend.pure.rust.PureRustEvaluator`.
//!
//! The only value currency across the boundary is the Java `CoreInstance`.
//! Java hands graph nodes in, described through the bridge and decoded by
//! the evaluator crate; Rust hands `CoreInstance`s back (`materialize`).
//! Every export runs inside `catch_unwind`, so a Rust panic reaches Java
//! as `PureRustException` rather than aborting the JVM. The context
//! itself, the lowering, the session guard and the handle table are the
//! evaluator crate's `bridge` module; this crate is the transport only.

#![deny(missing_docs)]
#![deny(clippy::undocumented_unsafe_blocks)]

pub mod bridge;
mod failure;
pub mod host;
pub mod materialize;

use std::cell::RefMut;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JObjectArray, JString, JValue};
use jni::sys::jlong;
use legend_pure_evaluator::bridge::context::BridgeContext;
use legend_pure_evaluator::bridge::failure::{ExceptionPayload, payload_of};
use legend_pure_evaluator::bridge::host::{Crossing, crossing_of, description_to_value};
use legend_pure_evaluator::bridge::session::Session;
use legend_pure_evaluator::bridge::{outbound, timings};
use legend_pure_evaluator::error::{PureException, StackFrame};
use legend_pure_evaluator::heap::ObjectHandle;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::model::PureModel;

use crate::bridge::{BridgeMethods, CORE_INSTANCE_CLASS, DescriptionFields};
use crate::failure::{boundary_error, in_local_frame, jni_failure};
use crate::host::JniHost;

/// The evaluator behind the `long` a Java `PureRustEvaluator` holds: the
/// context and the Java host it reads through, behind the one-call guard.
struct Evaluator {
    context: BridgeContext,
    host: JniHost,
}

type EvaluatorSession = Session<Evaluator>;

const PURE_RUST_EXC: &str = "org/finos/legend/pure/rust/PureRustException";

const PURE_RUST_EVAL_EXC: &str = "org/finos/legend/pure/rust/PureRustEvaluationException";

/// The constructor of `PureRustEvaluationException` that takes every
/// field of an [`ExceptionPayload`], the source span as its parts:
/// `(message, info, kind, sourceId, startLine, startColumn, endLine,
/// endColumn, callStack, constraintId, constraintKind, ownerFqn)`.
const RICH_CTOR_SIG: &str = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

/// Build the Java `PureRustEvaluationException` from a structured
/// `PureException` and throw it via JNI.
///
/// Best-effort: if any JNI call fails (e.g. the rich constructor isn't
/// found because the Java side is older than this binary), falls back
/// to `env.throw_new(PURE_RUST_EVAL_EXC, message)` which targets the
/// pre-existing message-only constructor. A panic is no typed failure
/// on the Java side: it is thrown as `PureRustException`.
pub(crate) fn throw_pure_exception(env: &mut JNIEnv, exc: &PureException) {
    let _ = failure::take_pending(env);
    let payload = payload_of(exc);
    let Some(kind) = failure::java_kind(payload.kind) else {
        let _ = env.throw_new(PURE_RUST_EXC, &payload.message);
        return;
    };
    if try_throw_rich(env, &payload, kind).is_err() {
        let _ = failure::take_pending(env);
        let _ = env.throw_new(PURE_RUST_EVAL_EXC, &payload.message);
    }
}

/// A line or column as a Java `int`.
fn position(v: u32) -> i32 {
    i32::try_from(v).unwrap_or(i32::MAX)
}

/// A frame as `PureRustEvaluationException.getCallStack()` holds it:
/// `<function> <- <source>:<line>:<column>`.
fn frame_text(frame: &StackFrame) -> String {
    format!(
        "{} <- {}:{}:{}",
        frame.function_name,
        frame.source.source,
        frame.source.start_line,
        frame.source.start_column
    )
}

#[allow(clippy::too_many_lines)]
fn try_throw_rich(
    env: &mut JNIEnv,
    payload: &ExceptionPayload,
    kind: &str,
) -> jni::errors::Result<()> {
    let message = env.new_string(&payload.message)?;
    let info = env.new_string(&payload.info)?;
    let kind = env.new_string(kind)?;
    let (source_id, [start_line, start_column, end_line, end_column]): (JObject<'_>, [i32; 4]) =
        match &payload.source {
            Some(s) => (
                env.new_string(&s.source)?.into(),
                [
                    position(s.start_line),
                    position(s.start_column),
                    position(s.end_line),
                    position(s.end_column),
                ],
            ),
            None => (JObject::null(), [0; 4]),
        };
    let stack_class = env.find_class("java/lang/String")?;
    let empty_string = env.new_string("")?;
    let call_stack_arr =
        env.new_object_array(payload.call_stack.len() as i32, &stack_class, &empty_string)?;
    for (i, frame) in payload.call_stack.iter().enumerate() {
        let frame_str = env.new_string(frame_text(frame))?;
        env.set_object_array_element(&call_stack_arr, i as i32, &frame_str)?;
    }
    let constraint_id: JObject<'_> = match &payload.constraint_id {
        Some(s) => env.new_string(s)?.into(),
        None => JObject::null(),
    };
    let constraint_kind: JObject<'_> = match payload.constraint_kind {
        Some(s) => env.new_string(s)?.into(),
        None => JObject::null(),
    };
    let owner_fqn: JObject<'_> = match &payload.owner_fqn {
        Some(s) => env.new_string(s)?.into(),
        None => JObject::null(),
    };

    let exc_obj = env.new_object(
        PURE_RUST_EVAL_EXC,
        RICH_CTOR_SIG,
        &[
            JValue::Object(&message.into()),
            JValue::Object(&info.into()),
            JValue::Object(&kind.into()),
            JValue::Object(&source_id),
            JValue::Int(start_line),
            JValue::Int(start_column),
            JValue::Int(end_line),
            JValue::Int(end_column),
            JValue::Object(&call_stack_arr.into()),
            JValue::Object(&constraint_id),
            JValue::Object(&constraint_kind),
            JValue::Object(&owner_fqn),
        ],
    )?;
    env.throw(jni::objects::JThrowable::from(exc_obj))?;
    Ok(())
}

/// Describe a Java `CoreInstance` through the bridge and turn it into a
/// value.
fn cross_in(env: &mut JNIEnv, ev: &Evaluator, obj: &JObject) -> Result<Value, PureException> {
    let described = ev.host.describe(env, obj)?;
    description_to_value(ev.context.shared(), described)
}

/// One argument slot: a `CoreInstance[]` becomes `Unit`, a scalar or a collection.
fn cross_in_slot(env: &mut JNIEnv, ev: &Evaluator, slot: &JObject) -> Result<Value, PureException> {
    if slot.is_null() {
        return Ok(Value::Unit);
    }
    let host = &ev.host;
    let arr = <&JObjectArray>::from(slot);
    let len = env
        .get_array_length(arr)
        .map_err(|e| jni_failure(env, host, "argument array", &e))?;
    let mut values = Vec::with_capacity(usize::try_from(len).unwrap_or(0));
    for i in 0..len {
        let value = in_local_frame(env, host, 4, |env| {
            let item = env
                .get_object_array_element(arr, i)
                .map_err(|e| jni_failure(env, host, "argument array", &e))?;
            cross_in(env, ev, &item)
        })?;
        values.push(value);
    }
    Ok(Value::from_vec(values))
}

/// Materialise a value as a `CoreInstance[]`: the array is the one local
/// reference that outlives the call; every value is materialised and
/// stored in a frame of its own.
fn cross_out<'l>(
    env: &mut JNIEnv<'l>,
    ev: &Evaluator,
    value: &Value,
) -> Result<JObjectArray<'l>, PureException> {
    let host = &ev.host;
    let mut leaves = Vec::new();
    outbound::leaves(value, &mut leaves);
    let arr = env
        .new_object_array(
            i32::try_from(leaves.len()).unwrap_or(i32::MAX),
            CORE_INSTANCE_CLASS,
            JObject::null(),
        )
        .map_err(|e| jni_failure(env, host, "result array", &e))?;
    for (i, leaf) in leaves.iter().enumerate() {
        let index = i32::try_from(i).unwrap_or(i32::MAX);
        in_local_frame(env, host, 8, |env| {
            let obj = materialize::leaf_to_java(env, host, ev.context.shared(), leaf)?;
            env.set_object_array_element(&arr, index, &obj)
                .map_err(|e| jni_failure(env, host, "result array", &e))
        })?;
    }
    Ok(arr)
}

fn throw_panic(env: &mut JNIEnv, what: &str) {
    let _ = failure::take_pending(env);
    let _ = env.throw_new(PURE_RUST_EXC, format!("Rust paniced during {what}"));
}

/// The session behind a pointer Java passed back.
///
/// # Safety
/// `ptr` is zero or a value [`into_raw`] returned that [`free`] has not
/// freed yet. Java guarantees both: it keeps the pointer in one field,
/// frees it once in `close()` and refuses every call after that
/// (`checkOpen`).
unsafe fn session<'a>(ptr: jlong) -> Result<&'a EvaluatorSession, PureException> {
    if ptr == 0 {
        return Err(boundary_error("the evaluator has no context"));
    }
    // SAFETY: per this function's contract the pointer came from
    // `Box::into_raw` in `into_raw` and is still live; a shared reference
    // is sound because a `Session` is only ever mutated through its cells.
    Ok(unsafe { &*(ptr as *const EvaluatorSession) })
}

/// Box a session and hand Java the pointer it will pass back.
fn into_raw(evaluator: Evaluator) -> jlong {
    Box::into_raw(Box::new(Session::new(evaluator))) as jlong
}

/// Free the session behind `ptr`.
///
/// # Safety
/// As for [`session`], and `ptr` is not used again.
///
/// # Errors
/// A call on the same evaluator is still running.
unsafe fn free(ptr: jlong) -> Result<(), PureException> {
    if ptr == 0 {
        return Ok(());
    }
    // SAFETY: this function's contract is `session`'s.
    unsafe { session(ptr) }?.ensure_idle()?;
    // SAFETY: the pointer came from `Box::into_raw` in `into_raw`, no
    // call is running (so no view of the context and no reference into
    // the session exists), and Java never passes it again after `close()`.
    drop(unsafe { Box::from_raw(ptr as *mut EvaluatorSession) });
    Ok(())
}

/// The evaluator behind `context_ptr` for one call, refused while another
/// call on it runs and once a panic has unwound through it.
fn live_evaluator<'a>(context_ptr: jlong) -> Result<RefMut<'a, Evaluator>, PureException> {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    let session = unsafe { session(context_ptr) }?;
    session.live()?;
    session.enter()
}

/// The evaluator behind `context_ptr` for housekeeping (counting and
/// releasing handles), refused only while another call on it runs.
fn any_evaluator<'a>(context_ptr: jlong) -> Result<RefMut<'a, Evaluator>, PureException> {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    unsafe { session(context_ptr) }?.enter()
}

/// Mark the evaluator unusable after a panic unwound through an entry
/// point (the view of that call was dropped by the unwind).
fn poison(context_ptr: jlong) {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    if let Ok(session) = unsafe { session(context_ptr) } {
        session.poison();
    }
}

/// Creates a context over the bootstrap model and the fished native
/// registry, bound to the Java `RustNodeBridge` passed in.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeInitContext<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    bridge: JObject<'local>,
) -> jlong {
    let wiring = (|| -> jni::errors::Result<JniHost> {
        let vm = env.get_java_vm()?;
        let global = env.new_global_ref(&bridge)?;
        let methods = BridgeMethods::resolve(&mut env)?;
        let fields = DescriptionFields::resolve(&mut env)?;
        Ok(JniHost::new(vm, global, methods, fields))
    })();
    let host = match wiring {
        Ok(host) => host,
        Err(e) => {
            let failure = failure::jni_failure_unwired(&mut env, "bridge wiring", &e);
            let _ = env.throw_new(PURE_RUST_EXC, failure.to_string());
            return 0;
        }
    };
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let model = PureModel::bootstrapped();
        let registry = NativeRegistry::standard();
        let context = BridgeContext::new(model, registry, host.clone());
        into_raw(Evaluator { context, host })
    }));
    match result {
        Ok(ptr) => ptr,
        Err(_) => {
            throw_panic(&mut env, "initialization");
            0
        }
    }
}

/// `start(function, arguments)`: run a graph node with graph-node arguments.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeStart<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    function: JObject<'local>,
    args: JObjectArray<'local>,
) -> JObjectArray<'local> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let mut ev = live_evaluator(context_ptr)?;
        let function = cross_in(&mut env, &ev, &function)?;
        let mut values = Vec::new();
        if !args.is_null() {
            let host = ev.host.clone();
            let len = env
                .get_array_length(&args)
                .map_err(|e| jni_failure(&mut env, &host, "arguments", &e))?;
            for i in 0..len {
                let value = in_local_frame(&mut env, &host, 4, |env| {
                    let slot = env
                        .get_object_array_element(&args, i)
                        .map_err(|e| jni_failure(env, &host, "arguments", &e))?;
                    cross_in_slot(env, &ev, &slot)
                })?;
                values.push(value);
            }
        }
        let value = ev.context.start(&function, &values);
        cross_out(&mut env, &ev, &value?)
    }));
    match result {
        Ok(Ok(arr)) => arr,
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            JObjectArray::from(JObject::null())
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "start");
            JObjectArray::from(JObject::null())
        }
    }
}

/// The Rust object behind a handle Java holds.
fn resolve_handle(ev: &Evaluator, handle: jlong) -> Result<ObjectHandle, PureException> {
    ev.context
        .resolve_handle(handle)
        .ok_or_else(|| boundary_error(format!("Stale or unknown JNI handle: {handle}")))
}

/// Every value of a property of a Rust object held for Java.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeGetProperty<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    handle: jlong,
    property_name: JString<'local>,
) -> JObjectArray<'local> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let ev = live_evaluator(context_ptr)?;
        let name: String = match env.get_string(&property_name) {
            Ok(name) => name.into(),
            Err(e) => return Err(jni_failure(&mut env, &ev.host, "property name", &e)),
        };
        let object = resolve_handle(&ev, handle)?;
        let values = ev.context.property_values(&object, &name)?;
        let collected: Vec<Value> = values.iter().cloned().collect();
        cross_out(&mut env, &ev, &Value::from_vec(collected))
    }));
    match result {
        Ok(Ok(arr)) => arr,
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            JObjectArray::from(JObject::null())
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "property evaluation");
            JObjectArray::from(JObject::null())
        }
    }
}

fn string_array<'l>(
    env: &mut JNIEnv<'l>,
    items: &[smol_str::SmolStr],
) -> jni::errors::Result<JObjectArray<'l>> {
    let arr = env.new_object_array(
        i32::try_from(items.len()).unwrap_or(i32::MAX),
        "java/lang/String",
        JObject::null(),
    )?;
    for (i, item) in items.iter().enumerate() {
        let s = env.new_string(item.as_str())?;
        env.set_object_array_element(&arr, i32::try_from(i).unwrap_or(i32::MAX), &s)?;
    }
    Ok(arr)
}

/// Property names of a Rust object held for Java.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativePropertyNames<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    handle: jlong,
) -> JObjectArray<'local> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let ev = live_evaluator(context_ptr)?;
        let object = resolve_handle(&ev, handle)?;
        let names = ev.context.property_names(&object)?;
        string_array(&mut env, &names).map_err(|e| jni_failure(&mut env, &ev.host, "names", &e))
    }));
    match result {
        Ok(Ok(arr)) => arr,
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            JObjectArray::from(JObject::null())
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "propertyNames");
            JObjectArray::from(JObject::null())
        }
    }
}

/// Classifier user path of a Rust object held for Java.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeGetClassifier<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    handle: jlong,
) -> JString<'local> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let ev = live_evaluator(context_ptr)?;
        let object = resolve_handle(&ev, handle)?;
        ev.context.classifier(&object)
    }));
    match result {
        Ok(Ok(classifier)) => env.new_string(classifier).unwrap_or_else(|_| {
            let _ = failure::take_pending(&mut env);
            JString::from(JObject::null())
        }),
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            JString::from(JObject::null())
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "getClassifier");
            JString::from(JObject::null())
        }
    }
}

/// Allocate a Rust heap object from a classifier path and parallel
/// name / value arrays (each value slot is a `CoreInstance[]`).
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeNew<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    classifier: JString<'local>,
    property_names: JObjectArray<'local>,
    property_values: JObjectArray<'local>,
) -> jlong {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let mut ev = live_evaluator(context_ptr)?;
        let host = ev.host.clone();
        let classifier: String = match env.get_string(&classifier) {
            Ok(classifier) => classifier.into(),
            Err(e) => return Err(jni_failure(&mut env, &host, "classifier", &e)),
        };
        let names_len = env
            .get_array_length(&property_names)
            .map_err(|e| jni_failure(&mut env, &host, "new", &e))?;
        let values_len = env
            .get_array_length(&property_values)
            .map_err(|e| jni_failure(&mut env, &host, "new", &e))?;
        if names_len != values_len {
            return Err(boundary_error(format!(
                "new: property name/value array length mismatch ({names_len} vs {values_len})"
            )));
        }
        let mut props = Vec::with_capacity(usize::try_from(names_len).unwrap_or(0));
        for i in 0..names_len {
            let prop = in_local_frame(&mut env, &host, 8, |env| {
                let name_obj = env
                    .get_object_array_element(&property_names, i)
                    .map_err(|e| jni_failure(env, &host, "new", &e))?;
                let name: String = match env.get_string(&JString::from(name_obj)) {
                    Ok(name) => name.into(),
                    Err(e) => return Err(jni_failure(env, &host, "new", &e)),
                };
                let slot = env
                    .get_object_array_element(&property_values, i)
                    .map_err(|e| jni_failure(env, &host, "new", &e))?;
                let value = cross_in_slot(env, &ev, &slot)?;
                Ok((
                    name,
                    value.to_collection().iter().cloned().collect::<Vec<_>>(),
                ))
            })?;
            props.push(prop);
        }
        ev.context.new_object(&classifier, &props)
    }));
    match result {
        Ok(Ok(handle)) => handle,
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            0
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "new");
            0
        }
    }
}

/// Test hook: which Rust shape a Java node becomes.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeCrossingKind<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    node: JObject<'local>,
) -> JString<'local> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let ev = live_evaluator(context_ptr)?;
        let value = cross_in(&mut env, &ev, &node)?;
        Ok::<&'static str, PureException>(match crossing_of(&value) {
            Crossing::Element => "element",
            Crossing::EnumValue => "enum",
            Crossing::HostNode => "javaNode",
            Crossing::RustObject => "rustObject",
            Crossing::Primitive => "primitive",
        })
    }));
    match result {
        Ok(Ok(kind)) => env.new_string(kind).unwrap_or_else(|_| {
            let _ = failure::take_pending(&mut env);
            JString::from(JObject::null())
        }),
        Ok(Err(err)) => {
            throw_pure_exception(&mut env, &err);
            JString::from(JObject::null())
        }
        Err(_) => {
            poison(context_ptr);
            throw_panic(&mut env, "crossingKind");
            JString::from(JObject::null())
        }
    }
}

/// Where the last lowered `start` on this thread spent its time:
/// `[lowering ns, run ns, host reads during the run, host reads during
/// the lowering]`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeLastTimings<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jni::sys::jlongArray {
    let t = timings::last();
    let values = [
        i64::try_from(t.lower_ns).unwrap_or(i64::MAX),
        i64::try_from(t.run_ns).unwrap_or(i64::MAX),
        i64::try_from(t.run_host_reads).unwrap_or(i64::MAX),
        i64::try_from(t.lower_host_reads).unwrap_or(i64::MAX),
    ];
    match env.new_long_array(4) {
        Ok(array) => {
            if env.set_long_array_region(&array, 0, &values).is_err() {
                let _ = failure::take_pending(&mut env);
            }
            array.into_raw()
        }
        Err(_) => {
            let _ = failure::take_pending(&mut env);
            std::ptr::null_mut()
        }
    }
}

/// Live handles in both directions; Java asserts this returns to zero.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeLiveHandleCount<
    'local,
>(
    mut _env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
) -> jlong {
    if context_ptr == 0 {
        return 0;
    }
    std::panic::catch_unwind(|| match any_evaluator(context_ptr) {
        Ok(ev) => i64::try_from(ev.context.live_handle_count()).unwrap_or(i64::MAX),
        Err(_) => -1,
    })
    .unwrap_or(-1)
}

/// Frees the context: evaluator, registry, model, and the bridge reference.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeFreeContext<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
) {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    match std::panic::catch_unwind(|| unsafe { free(context_ptr) }) {
        Ok(Ok(())) => {}
        Ok(Err(err)) => throw_pure_exception(&mut env, &err),
        Err(_) => throw_panic(&mut env, "close"),
    }
}

/// Frees the Rust side of one handle Java held.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_PureRustEvaluator_nativeFreeInstance<
    'local,
>(
    mut _env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_ptr: jlong,
    handle: jlong,
) {
    if context_ptr != 0 {
        let _ = std::panic::catch_unwind(|| {
            if let Ok(ev) = any_evaluator(context_ptr) {
                ev.context.release(handle);
            }
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_null_pointer_is_an_error_not_a_dereference() {
        // SAFETY: zero is never dereferenced.
        unsafe {
            assert!(session(0).is_err());
            assert!(free(0).is_ok());
        }
    }
}
