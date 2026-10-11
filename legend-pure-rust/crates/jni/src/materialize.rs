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
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::heap::{HeapEntry, RuntimeHeap};
use legend_pure_evaluator::model_utils::build_element_path;
use legend_pure_evaluator::value::{MapState, Value, ValueKey};
use legend_pure_metamodel::ids::ElementId;

use crate::bridge::{BridgeShared, CORE_INSTANCE_CLASS, JniHandleTable};
use crate::failure::{ffi_error, in_local_frame, jni_failure, unsupported};
use crate::java_node::JavaNode;

/// Call one of the bridge's materialising methods (`newInteger`,
/// `element`, …), each returning a `CoreInstance`. Every caller in this
/// module passes a method id from [`BridgeMethods`](crate::bridge::BridgeMethods)
/// with the argument list that method was resolved with.
fn call_object<'l>(
    env: &mut JNIEnv<'l>,
    shared: &BridgeShared,
    method: jni::objects::JMethodID,
    what: &str,
    args: &[jni::sys::jvalue],
) -> Result<JObject<'l>, PureException> {
    // SAFETY: `method` is a bridge method returning an object and `args`
    // follow its parameter list (the callers below pair each id with the
    // signature `BridgeMethods::resolve` looked it up with); the bridge
    // global reference is an instance of the bridge's class.
    let result = unsafe {
        env.call_method_unchecked(shared.bridge.as_obj(), method, ReturnType::Object, args)
    };
    match result.and_then(|v| v.l()) {
        Ok(obj) if !obj.is_null() => Ok(obj),
        Ok(_) => Err(ffi_error(format!(
            "{what}: the Java graph has no such node"
        ))),
        Err(e) => Err(jni_failure(env, shared, what, &e)),
    }
}

fn element_path(shared: &BridgeShared, id: ElementId) -> String {
    // SAFETY: values are materialised inside an entry point of the
    // context that owns `shared`, never while it grows its model.
    build_element_path(unsafe { shared.model() }, id, "::", false)
}

/// A map key as the value it was made from. A key built from an object's
/// `<<equality.Key>>` fields no longer holds the object, so it cannot
/// cross.
fn key_value(key: &ValueKey) -> Result<Value, PureException> {
    Ok(match key {
        ValueKey::Boolean(b) => Value::Boolean(*b),
        ValueKey::Integer(i) => Value::Integer(*i),
        ValueKey::Decimal(d) => Value::Decimal(*d),
        ValueKey::String(s) => Value::String(s.clone()),
        ValueKey::Date(d) => Value::Date(*d),
        ValueKey::StrictTime(t) => Value::StrictTime(*t),
        ValueKey::Object(handle) => Value::Object(handle.clone()),
        ValueKey::EnumValue { enum_id, member } => Value::EnumValue {
            enum_id: *enum_id,
            member: member.clone(),
        },
        ValueKey::ObjectByEqualityKeys { .. } => {
            return Err(unsupported(
                "a Map keyed by objects with equality keys across the boundary (the key holds their key fields, not the objects)",
            ));
        }
    })
}

/// A map crosses as the host engine's own map (`RustEngineHost.newMap`)
/// over its entries, each key and value materialised like any other
/// value; one value per key, as `Map<U,V>` holds.
fn map_to_java<'l>(
    env: &mut JNIEnv<'l>,
    shared: &Rc<BridgeShared>,
    map: &std::cell::RefCell<MapState>,
) -> Result<JObject<'l>, PureException> {
    let entries: Vec<(Value, Value)> = map
        .borrow()
        .entries
        .iter()
        .map(|(k, v)| Ok((key_value(k)?, v.clone())))
        .collect::<Result<_, PureException>>()?;
    let len =
        i32::try_from(entries.len()).map_err(|_| unsupported("a Map of more than 2^31 entries"))?;
    let keys = env
        .new_object_array(len, CORE_INSTANCE_CLASS, JObject::null())
        .map_err(|e| jni_failure(env, shared, "newMap", &e))?;
    let values = env
        .new_object_array(len, CORE_INSTANCE_CLASS, JObject::null())
        .map_err(|e| jni_failure(env, shared, "newMap", &e))?;
    for (i, (key, value)) in entries.iter().enumerate() {
        let index = i32::try_from(i).unwrap_or(i32::MAX);
        in_local_frame(env, shared, 8, |env| {
            let key = leaf_to_java(env, shared, single(key)?)?;
            env.set_object_array_element(&keys, index, &key)
                .map_err(|e| jni_failure(env, shared, "newMap", &e))?;
            let value = leaf_to_java(env, shared, single(value)?)?;
            env.set_object_array_element(&values, index, &value)
                .map_err(|e| jni_failure(env, shared, "newMap", &e))
        })?;
    }
    call_object(
        env,
        shared,
        shared.methods.new_map,
        "newMap",
        &[
            JValue::Object(&keys).as_jni(),
            JValue::Object(&values).as_jni(),
        ],
    )
}

/// The one value a map key or map value holds.
fn single(value: &Value) -> Result<&Value, PureException> {
    let mut found = Vec::new();
    leaves(value, &mut found);
    match found.as_slice() {
        [one] => Ok(one),
        _ => Err(unsupported(format!(
            "a Map entry holding {} values across the boundary",
            found.len()
        ))),
    }
}

/// The values `value` stands for once collections are flattened and
/// `Unit` dropped, in order: one Java object each.
pub fn leaves<'v>(value: &'v Value, out: &mut Vec<&'v Value>) {
    match value {
        Value::Unit => {}
        Value::Collection(items) => {
            for item in items.iter() {
                leaves(item, out);
            }
        }
        other => out.push(other),
    }
}

/// The Java object standing for one value that is neither a collection
/// nor `Unit` (see [`leaves`]).
///
/// # Errors
/// Value kinds that cannot cross yet, elements missing from the Java graph,
/// JNI failures.
#[allow(clippy::too_many_lines)]
pub fn leaf_to_java<'l>(
    env: &mut JNIEnv<'l>,
    shared: &Rc<BridgeShared>,
    value: &Value,
) -> Result<JObject<'l>, PureException> {
    let m = &shared.methods;
    let obj = match value {
        Value::Unit | Value::Collection(_) => {
            return Err(ffi_error("a collection is flattened before it crosses"));
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
                .map_err(|e| jni_failure(env, shared, "newFloat", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "newDecimal", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "newString", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "newDate", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "newStrictTime", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "element", &e))?;
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
                .map_err(|e| jni_failure(env, shared, "enumValue", &e))?;
            let name = env
                .new_string(member.as_str())
                .map_err(|e| jni_failure(env, shared, "enumValue", &e))?;
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
                    .map_err(|e| jni_failure(env, shared, "element", &e))?;
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
                    .map_err(|e| jni_failure(env, shared, "rustObject", &e))?;
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
        Value::Map(map) => map_to_java(env, shared, map)?,
        Value::UnitInstance { .. } => {
            return Err(unsupported("a unit instance across the boundary"));
        }
    };
    Ok(obj)
}
