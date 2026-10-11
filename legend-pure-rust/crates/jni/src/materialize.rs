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

//! Rust values crossing to Java as `CoreInstance`s.
//!
//! Primitives are materialised through the Java `ModelRepository`
//! (via `RustNodeBridge`), elements and enum values are looked up in the
//! Java graph by path, Java-backed nodes return their original object, and
//! any other Rust heap object becomes a canonical `RustCoreInstance` over a
//! handle. Collections flatten into the output vector; `Unit` adds nothing.

use std::rc::Rc;

use jni::JNIEnv;
use jni::objects::{JObject, JValue};
use jni::signature::ReturnType;
use legend_pure_evaluator::date::{DatePrecision, TimePrecision};
use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use legend_pure_evaluator::heap::{HeapEntry, RuntimeHeap};
use legend_pure_evaluator::model_utils::build_element_path;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::ids::ElementId;

use crate::bridge::{BridgeShared, JniHandleTable};
use crate::java_node::JavaNode;

fn ffi_error(message: String) -> PureException {
    PureException::execution(
        PureRuntimeError::EvaluationError(message),
        legend_pure_metamodel::source_info::SourceInfo::new("<ffi>", 0, 0, 0, 0),
        Vec::new(),
    )
}

fn unsupported(what: &str) -> PureException {
    PureException::execution(
        PureRuntimeError::Unsupported(what.to_string()),
        legend_pure_metamodel::source_info::SourceInfo::new("<ffi>", 0, 0, 0, 0),
        Vec::new(),
    )
}

fn jni_err(env: &mut JNIEnv, what: &str, e: jni::errors::Error) -> PureException {
    let _ = env.exception_clear();
    ffi_error(format!("{what}: {e}"))
}

fn call_object<'l>(
    env: &mut JNIEnv<'l>,
    shared: &BridgeShared,
    method: jni::objects::JMethodID,
    what: &str,
    args: &[jni::sys::jvalue],
) -> Result<JObject<'l>, PureException> {
    let result = unsafe {
        env.call_method_unchecked(shared.bridge.as_obj(), method, ReturnType::Object, args)
    };
    match result.and_then(|v| v.l()) {
        Ok(obj) if !obj.is_null() => Ok(obj),
        Ok(_) => Err(ffi_error(format!(
            "{what}: the Java graph has no such node"
        ))),
        Err(e) => Err(jni_err(env, what, e)),
    }
}

fn element_path(shared: &BridgeShared, id: ElementId) -> String {
    build_element_path(shared.model(), id, "::", false)
}

/// Append the Java objects standing for `value` to `out`.
///
/// # Errors
/// Value kinds that cannot cross yet, elements missing from the Java graph,
/// JNI failures.
#[allow(clippy::too_many_lines)]
pub fn value_to_java<'l>(
    env: &mut JNIEnv<'l>,
    shared: &Rc<BridgeShared>,
    value: &Value,
    out: &mut Vec<JObject<'l>>,
) -> Result<(), PureException> {
    let m = &shared.methods;
    let obj = match value {
        Value::Unit => return Ok(()),
        Value::Collection(items) => {
            for item in items.iter() {
                value_to_java(env, shared, item, out)?;
            }
            return Ok(());
        }
        Value::Boolean(b) => call_object(
            env,
            shared,
            m.new_boolean,
            "newBoolean",
            &[JValue::Bool(u8::from(*b)).as_jni()],
        )?,
        Value::Integer(i) => call_object(
            env,
            shared,
            m.new_integer,
            "newInteger",
            &[JValue::Long(*i).as_jni()],
        )?,
        Value::Float(f) => {
            if !f.is_finite() {
                return Err(unsupported("a non-finite Float across the boundary"));
            }
            let text = env
                .new_string(f.to_string())
                .map_err(|e| jni_err(env, "newFloat", e))?;
            call_object(
                env,
                shared,
                m.new_float,
                "newFloat",
                &[JValue::Object(&text).as_jni()],
            )?
        }
        Value::Decimal(d) => {
            let text = env
                .new_string(d.to_string())
                .map_err(|e| jni_err(env, "newDecimal", e))?;
            call_object(
                env,
                shared,
                m.new_decimal,
                "newDecimal",
                &[JValue::Object(&text).as_jni()],
            )?
        }
        Value::String(s) => {
            let text = env
                .new_string(s.as_str())
                .map_err(|e| jni_err(env, "newString", e))?;
            call_object(
                env,
                shared,
                m.new_string,
                "newString",
                &[JValue::Object(&text).as_jni()],
            )?
        }
        Value::Latest => call_object(env, shared, m.new_latest_date, "newLatestDate", &[])?,
        Value::Date(d) => {
            let (precision, subsecond): (i32, String) = match d.precision() {
                DatePrecision::Year => (0, String::new()),
                DatePrecision::Month => (1, String::new()),
                DatePrecision::Day => (2, String::new()),
                DatePrecision::Time(TimePrecision::Hour) => (3, String::new()),
                DatePrecision::Time(TimePrecision::Minute) => (4, String::new()),
                DatePrecision::Time(TimePrecision::Second) => (5, String::new()),
                DatePrecision::Time(TimePrecision::Subsecond(digits)) => {
                    let nanos = d.get_subsec_nanosecond().unwrap_or(0);
                    let s = format!("{nanos:09}");
                    (6, s[..usize::from(digits).min(9)].to_string())
                }
            };
            let sub = env
                .new_string(subsecond)
                .map_err(|e| jni_err(env, "newDate", e))?;
            call_object(
                env,
                shared,
                m.new_date,
                "newDate",
                &[
                    JValue::Int(i32::from(d.get_year())).as_jni(),
                    JValue::Int(i32::from(d.get_month().unwrap_or(0))).as_jni(),
                    JValue::Int(i32::from(d.get_day().unwrap_or(0))).as_jni(),
                    JValue::Int(i32::from(d.get_hour().unwrap_or(0))).as_jni(),
                    JValue::Int(i32::from(d.get_minute().unwrap_or(0))).as_jni(),
                    JValue::Int(i32::from(d.get_second().unwrap_or(0))).as_jni(),
                    JValue::Object(&sub).as_jni(),
                    JValue::Int(precision).as_jni(),
                ],
            )?
        }
        Value::StrictTime(t) => {
            let nanos = t.nanosecond();
            let (precision, subsecond) = if nanos == 0 {
                (5, String::new())
            } else {
                let s = format!("{nanos:09}");
                (6, s.trim_end_matches('0').to_string())
            };
            let sub = env
                .new_string(subsecond)
                .map_err(|e| jni_err(env, "newStrictTime", e))?;
            call_object(
                env,
                shared,
                m.new_strict_time,
                "newStrictTime",
                &[
                    JValue::Int(i32::from(t.hour())).as_jni(),
                    JValue::Int(i32::from(t.minute())).as_jni(),
                    JValue::Int(i32::from(t.second())).as_jni(),
                    JValue::Object(&sub).as_jni(),
                    JValue::Int(precision).as_jni(),
                ],
            )?
        }
        Value::Element(id) => {
            let path = env
                .new_string(element_path(shared, *id))
                .map_err(|e| jni_err(env, "element", e))?;
            call_object(
                env,
                shared,
                m.element,
                "element",
                &[JValue::Object(&path).as_jni()],
            )?
        }
        Value::EnumValue { enum_id, member } => {
            let path = env
                .new_string(element_path(shared, *enum_id))
                .map_err(|e| jni_err(env, "enumValue", e))?;
            let name = env
                .new_string(member.as_str())
                .map_err(|e| jni_err(env, "enumValue", e))?;
            call_object(
                env,
                shared,
                m.enum_value,
                "enumValue",
                &[
                    JValue::Object(&path).as_jni(),
                    JValue::Object(&name).as_jni(),
                ],
            )?
        }
        Value::Object(handle) => {
            let java_id = match &*handle.borrow() {
                HeapEntry::Typed(obj) => obj.as_any().downcast_ref::<JavaNode>().map(|n| n.id),
                HeapEntry::Dynamic(_) => None,
            };
            if let Some(id) = java_id {
                call_object(env, shared, m.node, "node", &[JValue::Long(id).as_jni()])?
            } else if let Some(eid) = RuntimeHeap::element_for_object(handle) {
                let path = env
                    .new_string(element_path(shared, eid))
                    .map_err(|e| jni_err(env, "element", e))?;
                call_object(
                    env,
                    shared,
                    m.element,
                    "element",
                    &[JValue::Object(&path).as_jni()],
                )?
            } else {
                let classifier = handle.borrow().classifier();
                let key = shared.handles.borrow_mut().register(handle.clone());
                let id = JniHandleTable::to_i64(key);
                let path = env
                    .new_string(classifier.as_str())
                    .map_err(|e| jni_err(env, "rustObject", e))?;
                call_object(
                    env,
                    shared,
                    m.rust_object,
                    "rustObject",
                    &[JValue::Long(id).as_jni(), JValue::Object(&path).as_jni()],
                )?
            }
        }
        Value::Function(_) => return Err(unsupported("a function value across the boundary")),
        Value::Map(_) => return Err(unsupported("a Map value across the boundary")),
        Value::UnitInstance { .. } => {
            return Err(unsupported("a unit instance across the boundary"));
        }
    };
    out.push(obj);
    Ok(())
}
