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
//! Java hands graph nodes in; Rust decodes primitives, unifies elements the
//! bootstrap model knows, and wraps every other node as a Java-backed heap
//! object (`java_node`). Rust hands `CoreInstance`s back (`materialize`).
//! Every export runs inside `catch_unwind`, so a Rust panic reaches Java
//! as `PureRustException` rather than aborting the JVM.

#![deny(missing_docs)]
#![deny(clippy::undocumented_unsafe_blocks)]

pub mod bridge;
mod context;
mod failure;
pub mod java_node;
pub mod materialize;
mod session;

/// The persistent vector behind `Value::Collection` and every property
/// read; one alias so the crate follows the evaluator's choice in one line.
pub(crate) use legend_pure_evaluator::value::PVector;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JObjectArray, JString, JValue};
use jni::signature::ReturnType;
use jni::sys::jlong;
use legend_pure_evaluator::bridge::failure::{ExceptionPayload, payload_of};
use legend_pure_evaluator::bridge::timings;
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::model::PureModel;

use crate::bridge::{BridgeMethods, CORE_INSTANCE_CLASS, DescriptionFields};
use crate::context::JniContext;
use crate::failure::{boundary_error, in_local_frame, jni_failure};
use crate::session::{Entered, Session};

type ContextSession = Session<JniContext>;

const PURE_RUST_EXC: &str = "org/finos/legend/pure/rust/PureRustException";

const PURE_RUST_EVAL_EXC: &str = "org/finos/legend/pure/rust/PureRustEvaluationException";

/// The constructor of `PureRustEvaluationException` that takes every
/// field of an [`ExceptionPayload`]:
/// `(message, info, kind, sourceInfo, callStack, constraintId, constraintKind, ownerFqn)`.
const RICH_CTOR_SIG: &str = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

/// Build the Java `PureRustEvaluationException` from a structured
/// `PureException` and throw it via JNI.
///
/// Best-effort: if any JNI call fails (e.g. the rich constructor isn't
/// found because the Java side is older than this binary), falls back
/// to `env.throw_new(PURE_RUST_EVAL_EXC, message)` which targets the
/// pre-existing message-only constructor.
pub(crate) fn throw_pure_exception(env: &mut JNIEnv, exc: &PureException) {
    let _ = failure::take_pending(env);
    let payload = payload_of(exc);
    if try_throw_rich(env, &payload).is_err() {
        let _ = failure::take_pending(env);
        let _ = env.throw_new(PURE_RUST_EVAL_EXC, &payload.message);
    }
}

#[allow(clippy::too_many_lines)]
fn try_throw_rich(env: &mut JNIEnv, payload: &ExceptionPayload) -> jni::errors::Result<()> {
    use jni::objects::{JObject, JValue};
    let message = env.new_string(&payload.message)?;
    let info = env.new_string(&payload.info)?;
    let kind = env.new_string(payload.kind)?;
    let source_info: JObject<'_> = match &payload.source_info {
        Some(s) => env.new_string(s)?.into(),
        None => JObject::null(),
    };
    let stack_class = env.find_class("java/lang/String")?;
    let empty_string = env.new_string("")?;
    let call_stack_arr =
        env.new_object_array(payload.call_stack.len() as i32, &stack_class, &empty_string)?;
    for (i, frame) in payload.call_stack.iter().enumerate() {
        let frame_str = env.new_string(frame)?;
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
            JValue::Object(&source_info),
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
/// value, in a local frame of its own.
fn cross_in(env: &mut JNIEnv, ctx: &JniContext, obj: &JObject) -> Result<Value, PureException> {
    let shared = &ctx.shared;
    in_local_frame(env, shared, 8, |env| {
        // SAFETY: `describe` was resolved on the bridge's class with the
        // signature `(Lorg/finos/legend/pure/m4/coreinstance/CoreInstance;)
        // Lorg/finos/legend/pure/rust/NodeDescription;`, the bridge global
        // reference is an instance of that class, and `obj` is a
        // `CoreInstance` (or null, which `describe` refuses with an exception).
        let described = unsafe {
            env.call_method_unchecked(
                shared.bridge.as_obj(),
                shared.methods.describe,
                ReturnType::Object,
                &[JValue::Object(obj).as_jni()],
            )
        };
        let desc_obj = described
            .and_then(|v| v.l())
            .map_err(|e| jni_failure(env, shared, "describe", &e))?;
        let d = java_node::read_description(env, &shared.fields, &desc_obj)
            .map_err(|e| jni_failure(env, shared, "describe", &e))?;
        java_node::description_to_value(shared, d)
    })
}

/// One argument slot: a `CoreInstance[]` becomes `Unit`, a scalar or a collection.
fn cross_in_slot(
    env: &mut JNIEnv,
    ctx: &JniContext,
    slot: &JObject,
) -> Result<Value, PureException> {
    if slot.is_null() {
        return Ok(Value::Unit);
    }
    let shared = &ctx.shared;
    let arr = <&JObjectArray>::from(slot);
    let len = env
        .get_array_length(arr)
        .map_err(|e| jni_failure(env, shared, "argument array", &e))?;
    let mut values = Vec::with_capacity(usize::try_from(len).unwrap_or(0));
    for i in 0..len {
        let value = in_local_frame(env, shared, 4, |env| {
            let item = env
                .get_object_array_element(arr, i)
                .map_err(|e| jni_failure(env, shared, "argument array", &e))?;
            cross_in(env, ctx, &item)
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
    ctx: &JniContext,
    value: &Value,
) -> Result<JObjectArray<'l>, PureException> {
    let shared = &ctx.shared;
    let mut leaves = Vec::new();
    materialize::leaves(value, &mut leaves);
    let arr = env
        .new_object_array(
            i32::try_from(leaves.len()).unwrap_or(i32::MAX),
            CORE_INSTANCE_CLASS,
            JObject::null(),
        )
        .map_err(|e| jni_failure(env, shared, "result array", &e))?;
    for (i, leaf) in leaves.iter().enumerate() {
        let index = i32::try_from(i).unwrap_or(i32::MAX);
        in_local_frame(env, shared, 8, |env| {
            let obj = materialize::leaf_to_java(env, shared, leaf)?;
            env.set_object_array_element(&arr, index, &obj)
                .map_err(|e| jni_failure(env, shared, "result array", &e))
        })?;
    }
    Ok(arr)
}

fn throw_panic(env: &mut JNIEnv, what: &str) {
    let _ = failure::take_pending(env);
    let _ = env.throw_new(PURE_RUST_EXC, format!("Rust paniced during {what}"));
}

/// The context behind `context_ptr` for one call, refused while another
/// call on it runs and once a panic has unwound through it.
fn live_context<'a>(context_ptr: jlong) -> Result<Entered<'a, JniContext>, PureException> {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    let ctx = unsafe { ContextSession::enter(context_ptr) }?;
    if ctx.poisoned {
        return Err(boundary_error(
            "the evaluator is poisoned by an earlier panic; close it and create a new one",
        ));
    }
    Ok(ctx)
}

/// Mark the context unusable after a panic unwound through an entry
/// point (the `Entered` of that call was dropped by the unwind).
fn poison(context_ptr: jlong) {
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    if let Ok(mut ctx) = unsafe { ContextSession::enter(context_ptr) } {
        ctx.poisoned = true;
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
    let wiring = (|| -> jni::errors::Result<(
        jni::JavaVM,
        jni::objects::GlobalRef,
        BridgeMethods,
        DescriptionFields,
    )> {
        let vm = env.get_java_vm()?;
        let global = env.new_global_ref(&bridge)?;
        let methods = BridgeMethods::resolve(&mut env)?;
        let fields = DescriptionFields::resolve(&mut env)?;
        Ok((vm, global, methods, fields))
    })();
    let (vm, global, methods, fields) = match wiring {
        Ok(w) => w,
        Err(e) => {
            let failure = failure::jni_failure_unwired(&mut env, "bridge wiring", &e);
            let _ = env.throw_new(PURE_RUST_EXC, failure.to_string());
            return 0;
        }
    };
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let model = PureModel::bootstrapped();
        let registry = NativeRegistry::standard();
        Session::into_raw(JniContext::new(
            model, registry, vm, global, methods, fields,
        ))
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
        let mut ctx = live_context(context_ptr)?;
        let function = cross_in(&mut env, &ctx, &function)?;
        let mut values = Vec::new();
        if !args.is_null() {
            let shared = std::rc::Rc::clone(&ctx.shared);
            let len = env
                .get_array_length(&args)
                .map_err(|e| jni_failure(&mut env, &shared, "arguments", &e))?;
            for i in 0..len {
                let value = in_local_frame(&mut env, &shared, 4, |env| {
                    let slot = env
                        .get_object_array_element(&args, i)
                        .map_err(|e| jni_failure(env, &shared, "arguments", &e))?;
                    cross_in_slot(env, &ctx, &slot)
                })?;
                values.push(value);
            }
        }
        let value = ctx.start(&function, &values);
        ctx.shared.forget_released(&mut env);
        cross_out(&mut env, &ctx, &value?)
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
        let mut ctx = live_context(context_ptr)?;
        let name: String = match env.get_string(&property_name) {
            Ok(name) => name.into(),
            Err(e) => return Err(jni_failure(&mut env, &ctx.shared, "property name", &e)),
        };
        let values = ctx.get_property_values(handle, &name)?;
        let collected: Vec<Value> = values.iter().cloned().collect();
        cross_out(&mut env, &ctx, &Value::from_vec(collected))
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
        let mut ctx = live_context(context_ptr)?;
        let names = ctx.property_names(handle)?;
        string_array(&mut env, &names).map_err(|e| jni_failure(&mut env, &ctx.shared, "names", &e))
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
        let mut ctx = live_context(context_ptr)?;
        ctx.get_classifier(handle)
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
        let mut ctx = live_context(context_ptr)?;
        let shared = std::rc::Rc::clone(&ctx.shared);
        let classifier: String = match env.get_string(&classifier) {
            Ok(classifier) => classifier.into(),
            Err(e) => return Err(jni_failure(&mut env, &shared, "classifier", &e)),
        };
        let names_len = env
            .get_array_length(&property_names)
            .map_err(|e| jni_failure(&mut env, &shared, "new", &e))?;
        let values_len = env
            .get_array_length(&property_values)
            .map_err(|e| jni_failure(&mut env, &shared, "new", &e))?;
        if names_len != values_len {
            return Err(boundary_error(format!(
                "new: property name/value array length mismatch ({names_len} vs {values_len})"
            )));
        }
        let mut props = Vec::with_capacity(usize::try_from(names_len).unwrap_or(0));
        for i in 0..names_len {
            let prop = in_local_frame(&mut env, &shared, 8, |env| {
                let name_obj = env
                    .get_object_array_element(&property_names, i)
                    .map_err(|e| jni_failure(env, &shared, "new", &e))?;
                let name: String = match env.get_string(&JString::from(name_obj)) {
                    Ok(name) => name.into(),
                    Err(e) => return Err(jni_failure(env, &shared, "new", &e)),
                };
                let slot = env
                    .get_object_array_element(&property_values, i)
                    .map_err(|e| jni_failure(env, &shared, "new", &e))?;
                let value = cross_in_slot(env, &ctx, &slot)?;
                Ok((
                    name,
                    value.to_collection().iter().cloned().collect::<Vec<_>>(),
                ))
            })?;
            props.push(prop);
        }
        ctx.new_object(&classifier, &props)
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
        let ctx = live_context(context_ptr)?;
        let value = cross_in(&mut env, &ctx, &node)?;
        Ok::<&'static str, PureException>(java_node::crossing_kind(&value))
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
/// `[lowering ns, run ns, JNI reads during the run, JNI reads during the
/// lowering]`.
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
    // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
    // keeps it in one field, frees it once in `close()` and refuses every
    // call after (`checkOpen`).
    std::panic::catch_unwind(|| match unsafe { ContextSession::enter(context_ptr) } {
        Ok(ctx) => i64::try_from(ctx.live_handle_count()).unwrap_or(i64::MAX),
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
    match std::panic::catch_unwind(|| unsafe { ContextSession::free(context_ptr) }) {
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
            // SAFETY: `context_ptr` is the value `nativeInitContext` returned: Java
            // keeps it in one field, frees it once in `close()` and refuses every
            // call after (`checkOpen`).
            if let Ok(ctx) = unsafe { ContextSession::enter(context_ptr) } {
                ctx.release(handle);
            }
        });
    }
}
