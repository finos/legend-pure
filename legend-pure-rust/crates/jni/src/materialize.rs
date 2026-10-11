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

//! Rust values crossing to Java as `CoreInstance`s: one bridge call per
//! [`Leaf`] arm (`newInteger`, `element`, `node`, `rustObject`, …), each
//! returning a `CoreInstance`.

use jni::JNIEnv;
use jni::objects::{JObject, JValue};
use jni::signature::ReturnType;
use legend_pure_evaluator::bridge::host::HostShared;
use legend_pure_evaluator::bridge::outbound::{Leaf, leaf};
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::value::Value;

use crate::bridge::CORE_INSTANCE_CLASS;
use crate::failure::{boundary_error, in_local_frame, jni_failure};
use crate::host::JniHost;

/// Call one of the bridge's materialising methods. Every caller in this
/// module passes a method id from [`BridgeMethods`](crate::bridge::BridgeMethods)
/// with the argument list that method was resolved with.
fn call_object<'l>(
    env: &mut JNIEnv<'l>,
    host: &JniHost,
    method: jni::objects::JMethodID,
    what: &str,
    args: &[jni::sys::jvalue],
) -> Result<JObject<'l>, PureException> {
    // SAFETY: `method` is a bridge method returning an object and `args`
    // follow its parameter list (the callers below pair each id with the
    // signature `BridgeMethods::resolve` looked it up with); the bridge
    // global reference is an instance of the bridge's class.
    let result =
        unsafe { env.call_method_unchecked(host.bridge(), method, ReturnType::Object, args) };
    match result.and_then(|v| v.l()) {
        Ok(obj) if !obj.is_null() => Ok(obj),
        Ok(_) => Err(boundary_error(format!(
            "{what}: the Java graph has no such node"
        ))),
        Err(e) => Err(jni_failure(env, host, what, &e)),
    }
}

/// A map crosses as the host engine's own map (`RustEngineHost.newMap`)
/// over its entries, each key and value materialised like any other
/// value.
fn map_to_java<'l>(
    env: &mut JNIEnv<'l>,
    host: &JniHost,
    shared: &HostShared,
    entries: &[(Value, Value)],
) -> Result<JObject<'l>, PureException> {
    let len = i32::try_from(entries.len())
        .map_err(|_| boundary_error("a Map of more than 2^31 entries"))?;
    let keys = env
        .new_object_array(len, CORE_INSTANCE_CLASS, JObject::null())
        .map_err(|e| jni_failure(env, host, "newMap", &e))?;
    let values = env
        .new_object_array(len, CORE_INSTANCE_CLASS, JObject::null())
        .map_err(|e| jni_failure(env, host, "newMap", &e))?;
    for (i, (key, value)) in entries.iter().enumerate() {
        let index = i32::try_from(i).unwrap_or(i32::MAX);
        in_local_frame(env, host, 8, |env| {
            let key = leaf_to_java(env, host, shared, key)?;
            env.set_object_array_element(&keys, index, &key)
                .map_err(|e| jni_failure(env, host, "newMap", &e))?;
            let value = leaf_to_java(env, host, shared, value)?;
            env.set_object_array_element(&values, index, &value)
                .map_err(|e| jni_failure(env, host, "newMap", &e))
        })?;
    }
    call_object(
        env,
        host,
        host.methods().new_map,
        "newMap",
        &[
            JValue::Object(&keys).as_jni(),
            JValue::Object(&values).as_jni(),
        ],
    )
}

/// The Java object standing for one value that is neither a collection
/// nor `Unit` (see [`leaves`](legend_pure_evaluator::bridge::outbound::leaves)).
///
/// # Errors
/// Value kinds that cannot cross yet, elements missing from the Java graph,
/// JNI failures.
#[allow(clippy::too_many_lines)]
pub fn leaf_to_java<'l>(
    env: &mut JNIEnv<'l>,
    host: &JniHost,
    shared: &HostShared,
    value: &Value,
) -> Result<JObject<'l>, PureException> {
    let m = host.methods();
    let text = |env: &mut JNIEnv<'l>, s: &str, what: &str| {
        env.new_string(s)
            .map_err(|e| jni_failure(env, host, what, &e))
    };
    let obj = match leaf(shared, value)? {
        Leaf::Boolean(b) => call_object(
            env,
            host,
            m.new_boolean,
            "newBoolean",
            &[JValue::Bool(u8::from(b)).as_jni()],
        )?,
        Leaf::Integer(i) => call_object(
            env,
            host,
            m.new_integer,
            "newInteger",
            &[JValue::Long(i).as_jni()],
        )?,
        Leaf::Float(f) => {
            let s = text(env, &f.to_string(), "newFloat")?;
            call_object(
                env,
                host,
                m.new_float,
                "newFloat",
                &[JValue::Object(&s).as_jni()],
            )?
        }
        Leaf::Decimal(d) => {
            let s = text(env, &d.to_string(), "newDecimal")?;
            call_object(
                env,
                host,
                m.new_decimal,
                "newDecimal",
                &[JValue::Object(&s).as_jni()],
            )?
        }
        Leaf::String(s) => {
            let s = text(env, s, "newString")?;
            call_object(
                env,
                host,
                m.new_string,
                "newString",
                &[JValue::Object(&s).as_jni()],
            )?
        }
        Leaf::LatestDate => call_object(env, host, m.new_latest_date, "newLatestDate", &[])?,
        Leaf::Date(d) => {
            let sub = text(env, &d.subsecond, "newDate")?;
            call_object(
                env,
                host,
                m.new_date,
                "newDate",
                &[
                    JValue::Int(d.year).as_jni(),
                    JValue::Int(d.month).as_jni(),
                    JValue::Int(d.day).as_jni(),
                    JValue::Int(d.hour).as_jni(),
                    JValue::Int(d.minute).as_jni(),
                    JValue::Int(d.second).as_jni(),
                    JValue::Object(&sub).as_jni(),
                    JValue::Int(d.precision).as_jni(),
                ],
            )?
        }
        Leaf::StrictTime(t) => {
            let sub = text(env, &t.subsecond, "newStrictTime")?;
            call_object(
                env,
                host,
                m.new_strict_time,
                "newStrictTime",
                &[
                    JValue::Int(t.hour).as_jni(),
                    JValue::Int(t.minute).as_jni(),
                    JValue::Int(t.second).as_jni(),
                    JValue::Object(&sub).as_jni(),
                    JValue::Int(t.precision).as_jni(),
                ],
            )?
        }
        Leaf::Element(path) => {
            let path = text(env, &path, "element")?;
            call_object(
                env,
                host,
                m.element,
                "element",
                &[JValue::Object(&path).as_jni()],
            )?
        }
        Leaf::EnumValue { enumeration, name } => {
            let path = text(env, &enumeration, "enumValue")?;
            let name = text(env, name, "enumValue")?;
            call_object(
                env,
                host,
                m.enum_value,
                "enumValue",
                &[
                    JValue::Object(&path).as_jni(),
                    JValue::Object(&name).as_jni(),
                ],
            )?
        }
        Leaf::HostNode(id) => call_object(env, host, m.node, "node", &[JValue::Long(id).as_jni()])?,
        Leaf::RustObject { handle, classifier } => {
            let path = text(env, classifier.as_str(), "rustObject")?;
            call_object(
                env,
                host,
                m.rust_object,
                "rustObject",
                &[
                    JValue::Long(handle).as_jni(),
                    JValue::Object(&path).as_jni(),
                ],
            )?
        }
        Leaf::Map(entries) => map_to_java(env, host, shared, &entries)?,
    };
    Ok(obj)
}
