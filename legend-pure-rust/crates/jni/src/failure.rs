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

//! Failures at the boundary as typed [`PureException`]s, and the JNI
//! exception discipline.
//!
//! A JNI call that fails because Java threw leaves that exception pending,
//! and no other JNI function may be called until it is cleared. Every
//! failed call in this crate goes through [`jni_failure`], which takes the
//! pending throwable, clears it, and only then asks the bridge what it was:
//! a `PureRustEvaluationException` the bridge raised on purpose keeps its
//! kind (`UNSUPPORTED`, `NOT_IMPLEMENTED`, `FUNCTION_NOT_FOUND`), anything
//! else becomes an execution error carrying the throwable's text.

use jni::JNIEnv;
use jni::objects::{JObjectArray, JString, JThrowable, JValue};
use jni::signature::ReturnType;
use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use smol_str::SmolStr;

pub(crate) use legend_pure_evaluator::bridge::failure::{boundary_error, typed, unsupported};

use crate::bridge::BridgeShared;

/// Take and clear the pending Java exception, if there is one. Nothing
/// else may be called on `env` while an exception is pending.
pub(crate) fn take_pending<'l>(env: &mut JNIEnv<'l>) -> Option<JThrowable<'l>> {
    if !env.exception_check().unwrap_or(false) {
        return None;
    }
    let throwable = env.exception_occurred().ok();
    let _ = env.exception_clear();
    throwable.filter(|t| !t.is_null())
}

/// A failed JNI call as a typed failure: the pending Java exception, if
/// any, is taken and cleared first and then described through the bridge.
pub(crate) fn jni_failure(
    env: &mut JNIEnv,
    shared: &BridgeShared,
    what: &str,
    error: &jni::errors::Error,
) -> PureException {
    match take_pending(env) {
        Some(throwable) => from_throwable(env, shared, what, &throwable),
        None => boundary_error(format!("{what}: {error}")),
    }
}

/// A failed JNI call made before the bridge exists (no way to describe a
/// Java exception yet): the exception is cleared and the call named.
pub(crate) fn jni_failure_unwired(
    env: &mut JNIEnv,
    what: &str,
    error: &jni::errors::Error,
) -> PureException {
    let _ = take_pending(env);
    boundary_error(format!("{what}: {error}"))
}

fn from_throwable(
    env: &mut JNIEnv,
    shared: &BridgeShared,
    what: &str,
    throwable: &JThrowable,
) -> PureException {
    match describe_throwable(env, shared, throwable) {
        Ok((kind, text)) => match kind.as_str() {
            "UNSUPPORTED" => typed(PureRuntimeError::Unsupported(text)),
            "NOT_IMPLEMENTED" => typed(PureRuntimeError::NotImplemented(SmolStr::new(text))),
            "FUNCTION_NOT_FOUND" => typed(PureRuntimeError::FunctionNotFound(SmolStr::new(text))),
            _ => boundary_error(format!("{what}: {text}")),
        },
        Err(_) => {
            let _ = take_pending(env);
            boundary_error(format!(
                "{what}: a Java exception that could not be described"
            ))
        }
    }
}

/// `RustNodeBridge.failureOf(throwable)`: `{kind name, text}`.
fn describe_throwable(
    env: &mut JNIEnv,
    shared: &BridgeShared,
    throwable: &JThrowable,
) -> jni::errors::Result<(String, String)> {
    // SAFETY: `failure_of` was resolved on the bridge's class with the
    // signature `(Ljava/lang/Throwable;)[Ljava/lang/String;`, the bridge
    // global reference is an instance of that class, and the one argument
    // is a `Throwable` passed as an object.
    let described = unsafe {
        env.call_method_unchecked(
            shared.bridge.as_obj(),
            shared.methods.failure_of,
            ReturnType::Array,
            &[JValue::Object(throwable).as_jni()],
        )
    }?
    .l()?;
    let parts = JObjectArray::from(described);
    let kind = env.get_object_array_element(&parts, 0)?;
    let kind: String = env.get_string(&JString::from(kind))?.into();
    let text = env.get_object_array_element(&parts, 1)?;
    let text: String = env.get_string(&JString::from(text))?.into();
    Ok((kind, text))
}

/// Run `f` in its own JNI local reference frame, so every local reference
/// it creates is freed when it returns: a long `nativeStart` reads many
/// values and must not hold one reference per value until it ends.
pub(crate) fn in_local_frame<T>(
    env: &mut JNIEnv,
    shared: &BridgeShared,
    capacity: i32,
    f: impl FnOnce(&mut JNIEnv) -> Result<T, PureException>,
) -> Result<T, PureException> {
    match env.with_local_frame(capacity, |env| {
        Ok::<Result<T, PureException>, jni::errors::Error>(f(env))
    }) {
        Ok(inner) => inner,
        Err(e) => Err(jni_failure(env, shared, "local frame", &e)),
    }
}
