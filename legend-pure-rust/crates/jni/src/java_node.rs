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

//! A Java `CoreInstance` as a first-class evaluator object.
//!
//! The Java side describes a node once (`RustNodeBridge.describe`) and its
//! property values on demand (`propertyValues`); nothing is read ahead,
//! nothing is read twice. A node whose user path the Rust model already
//! knows never becomes a node at all: it is unified to `Value::Element`
//! before any allocation, as are primitives (decoded to Rust values) and
//! enum values of enumerations the model knows.

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;
use std::str::FromStr;

use jni::JNIEnv;
use jni::objects::{JFieldID, JObject, JObjectArray, JString, JValue};
use jni::signature::{Primitive, ReturnType};
use legend_pure_evaluator::date::{PureDate, StrictTime, TimePrecision};
use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use legend_pure_evaluator::heap::{HeapEntry, ObjectHandle, RuntimeHeap, TypedObject};
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::PVector;
use crate::bridge::{BridgeShared, DescriptionFields, JniHandleTable};
use crate::failure::{ffi_error, in_local_frame, jni_failure, unsupported};

/// Kind tags of a `NodeDescription`, mirrored from the Java constants.
pub mod kind {
    /// `Boolean` primitive.
    pub const BOOLEAN: i32 = 0;
    /// `Integer` primitive (decimal text payload).
    pub const INTEGER: i32 = 1;
    /// `Float` primitive (plain decimal text payload).
    pub const FLOAT: i32 = 2;
    /// `Decimal` primitive (plain decimal text payload).
    pub const DECIMAL: i32 = 3;
    /// `String` primitive.
    pub const STRING: i32 = 4;
    /// `Date`, `StrictDate` or `DateTime`, as components.
    pub const DATE: i32 = 5;
    /// `%latest`.
    pub const LATEST_DATE: i32 = 6;
    /// `StrictTime`, as components.
    pub const STRICT_TIME: i32 = 7;
    /// `Byte` primitive.
    pub const BYTE: i32 = 8;
    /// A value of an `Enumeration`.
    pub const ENUM_VALUE: i32 = 10;
    /// A `PackageableElement` with a user path.
    pub const PACKAGEABLE_ELEMENT: i32 = 11;
    /// Any other node.
    pub const NODE: i32 = 12;
    /// A Rust object previously handed to Java.
    pub const RUST_OBJECT: i32 = 13;
}

/// A decoded `NodeDescription`.
#[derive(Debug, Clone)]
pub struct Description {
    /// One of the [`kind`] constants.
    pub kind: i32,
    /// Bridge id (NODE, ENUM_VALUE, PACKAGEABLE_ELEMENT) or Rust handle (RUST_OBJECT).
    pub id: i64,
    /// Classifier user path.
    pub classifier: Option<SmolStr>,
    /// `getName()`.
    pub name: Option<SmolStr>,
    /// User path of a packageable element.
    pub user_path: Option<SmolStr>,
    /// Text payload of a primitive.
    pub payload: Option<String>,
    /// Date / time components.
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
    /// 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond.
    pub precision: i32,
    /// Subsecond digits, when `precision == 6`.
    pub subsecond: Option<String>,
    /// Source span of the node, when the Java graph knows one.
    pub source: Option<SourceInfo>,
    /// Bridge id of the lambda definition a per-evaluation lambda value
    /// stands for (`RustEngineHost.lambdaDefinition`); -1 when none.
    pub definition: i64,
}

fn string_field(
    env: &mut JNIEnv,
    obj: &JObject,
    field: JFieldID,
) -> jni::errors::Result<Option<String>> {
    let v = env
        .get_field_unchecked(obj, field, ReturnType::Object)?
        .l()?;
    if v.is_null() {
        return Ok(None);
    }
    let s: String = env.get_string(&JString::from(v))?.into();
    Ok(Some(s))
}

/// Decode a Java `NodeDescription` through the field ids resolved once
/// per context ([`DescriptionFields`]).
///
/// # Errors
/// Any JNI failure; the caller clears the pending Java exception.
pub fn read_description(
    env: &mut JNIEnv,
    fields: &DescriptionFields,
    obj: &JObject,
) -> jni::errors::Result<Description> {
    let int = |env: &mut JNIEnv, field: JFieldID| -> jni::errors::Result<i32> {
        env.get_field_unchecked(obj, field, ReturnType::Primitive(Primitive::Int))?
            .i()
    };
    let long = |env: &mut JNIEnv, field: JFieldID| -> jni::errors::Result<i64> {
        env.get_field_unchecked(obj, field, ReturnType::Primitive(Primitive::Long))?
            .j()
    };
    let unsigned = |env: &mut JNIEnv, field: JFieldID| -> jni::errors::Result<u32> {
        Ok(u32::try_from(int(env, field)?).unwrap_or(0))
    };
    let kind = int(env, fields.kind)?;
    let id = long(env, fields.id)?;
    let definition = long(env, fields.definition)?;
    let classifier = string_field(env, obj, fields.classifier)?.map(SmolStr::new);
    let name = string_field(env, obj, fields.name)?.map(SmolStr::new);
    let user_path = string_field(env, obj, fields.user_path)?.map(SmolStr::new);
    let payload = string_field(env, obj, fields.payload)?;
    let subsecond = string_field(env, obj, fields.subsecond)?;
    let source = match string_field(env, obj, fields.source_id)? {
        // Java's anchor (`line`/`column`) is the span's start on this
        // side; the evaluator and `assertError` both read the anchor.
        Some(source_id) => Some(SourceInfo::new(
            source_id,
            unsigned(env, fields.line)?,
            unsigned(env, fields.column)?,
            unsigned(env, fields.end_line)?,
            unsigned(env, fields.end_column)?,
        )),
        None => None,
    };
    Ok(Description {
        kind,
        id,
        classifier,
        name,
        user_path,
        payload,
        year: int(env, fields.year)?,
        month: int(env, fields.month)?,
        day: int(env, fields.day)?,
        hour: int(env, fields.hour)?,
        minute: int(env, fields.minute)?,
        second: int(env, fields.second)?,
        precision: int(env, fields.precision)?,
        subsecond,
        source,
        definition,
    })
}

#[allow(clippy::cast_possible_truncation)]
fn small(v: i32) -> i8 {
    v as i8
}

fn subsecond_nanos(digits: &str) -> Result<(i32, u8), PureException> {
    let trimmed: String = digits.chars().take(9).collect();
    let padded = format!("{trimmed:0<9}");
    let nanos = padded
        .parse::<i32>()
        .map_err(|_| ffi_error(format!("bad subsecond digits `{digits}`")))?;
    let count = u8::try_from(trimmed.len()).unwrap_or(9);
    Ok((nanos, count))
}

/// Turn a description into the Rust value it stands for, allocating a
/// Java-backed node only when nothing more specific applies.
///
/// # Errors
/// Primitive payloads that do not parse; value kinds the evaluator cannot
/// hold yet (`Byte`).
pub fn description_to_value(
    shared: &Rc<BridgeShared>,
    d: Description,
) -> Result<Value, PureException> {
    let payload = |d: &Description| d.payload.clone().unwrap_or_default();
    match d.kind {
        kind::BOOLEAN => Ok(Value::Boolean(payload(&d) == "true")),
        kind::INTEGER => payload(&d)
            .parse::<i64>()
            .map(Value::Integer)
            .map_err(|_| unsupported("an Integer outside the 64-bit range")),
        kind::FLOAT => payload(&d)
            .parse::<f64>()
            .map(Value::Float)
            .map_err(|_| ffi_error(format!("bad Float `{}`", payload(&d)))),
        kind::DECIMAL => rust_decimal::Decimal::from_str(&payload(&d))
            .map(Value::Decimal)
            .map_err(|_| ffi_error(format!("bad Decimal `{}`", payload(&d)))),
        kind::STRING => Ok(Value::String(SmolStr::new(payload(&d)))),
        kind::LATEST_DATE => Ok(Value::Latest),
        kind::DATE => {
            let (year, month, day) = (
                i16::try_from(d.year).map_err(|_| ffi_error("year out of range"))?,
                small(d.month),
                small(d.day),
            );
            let date = match d.precision {
                0 => PureDate::year(year),
                1 => PureDate::year_month(year, month),
                2 => PureDate::strict_date(year, month, day),
                p => {
                    let (nanos, digits) = match d.subsecond.as_deref() {
                        Some(s) if p >= 6 => subsecond_nanos(s)?,
                        _ => (0, 0),
                    };
                    let precision = match p {
                        3 => TimePrecision::Hour,
                        4 => TimePrecision::Minute,
                        5 => TimePrecision::Second,
                        _ => TimePrecision::Subsecond(digits),
                    };
                    PureDate::datetime(
                        year,
                        month,
                        day,
                        small(d.hour),
                        small(d.minute),
                        small(d.second),
                        nanos,
                        precision,
                    )
                }
            }
            .map_err(PureException::from)?;
            Ok(Value::Date(date))
        }
        kind::STRICT_TIME => {
            let (nanos, _) = match d.subsecond.as_deref() {
                Some(s) if d.precision >= 6 => subsecond_nanos(s)?,
                _ => (0, 0),
            };
            StrictTime::new(small(d.hour), small(d.minute), small(d.second), nanos)
                .map(Value::StrictTime)
                .map_err(PureException::from)
        }
        kind::BYTE => Err(unsupported("Byte values across the boundary")),
        kind::ENUM_VALUE => {
            if let (Some(classifier), Some(name)) = (&d.classifier, &d.name)
                // SAFETY: descriptions are decoded inside an entry point of
                // the context that owns `shared`, never while it grows its model.
                && let Some(enum_id) = unsafe { shared.model() }.resolve_fqn_str(classifier)
            {
                return Ok(Value::EnumValue {
                    enum_id,
                    member: name.clone(),
                });
            }
            Ok(Value::Object(canonical_node(shared, d)))
        }
        kind::PACKAGEABLE_ELEMENT => {
            if let Some(path) = &d.user_path {
                let segments: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
                // SAFETY: descriptions are decoded inside an entry point of
                // the context that owns `shared`, never while it grows its model.
                let model = unsafe { shared.model() };
                if let Some(id) = model
                    .resolve_by_fqn(&segments)
                    .or_else(|| model.resolve_by_path(&segments))
                {
                    return Ok(Value::Element(id));
                }
            }
            Ok(Value::Object(canonical_node(shared, d)))
        }
        kind::RUST_OBJECT => {
            let handle = shared
                .handles
                .borrow()
                .lookup(JniHandleTable::from_i64(d.id))
                .cloned()
                .ok_or_else(|| ffi_error(format!("Stale or unknown JNI handle: {}", d.id)))?;
            Ok(match RuntimeHeap::element_for_object(&handle) {
                Some(eid) => Value::Element(eid),
                None => Value::Object(handle),
            })
        }
        _ => Ok(Value::Object(canonical_node(shared, d))),
    }
}

/// The one heap entry for a Java node id, allocating it on first sight.
fn canonical_node(shared: &Rc<BridgeShared>, d: Description) -> ObjectHandle {
    if let Some(existing) = shared
        .java_nodes
        .borrow()
        .get(&d.id)
        .and_then(std::rc::Weak::upgrade)
    {
        return existing;
    }
    let node = JavaNode {
        id: d.id,
        classifier: d
            .classifier
            .unwrap_or_else(|| SmolStr::new("meta::pure::metamodel::type::Any")),
        name: d.name,
        user_path: d.user_path,
        keys: RefCell::new(None),
        source: d.source,
        definition: (d.definition >= 0).then_some(d.definition),
        cache: RefCell::new(HashMap::new()),
        local: HashMap::new(),
        shared: Rc::clone(shared),
    };
    let handle: ObjectHandle = Rc::new(RefCell::new(HeapEntry::Typed(Box::new(node))));
    shared
        .java_nodes
        .borrow_mut()
        .insert(d.id, Rc::downgrade(&handle));
    handle
}

/// A Java `CoreInstance` living in the evaluator heap.
pub struct JavaNode {
    /// Bridge id (Java identity).
    pub id: i64,
    /// Classifier user path.
    pub classifier: SmolStr,
    /// `getName()`, when the node has one.
    pub name: Option<SmolStr>,
    /// User path, when the node is a packageable element.
    pub user_path: Option<SmolStr>,
    /// Property keys reported by Java, read on first use: the lowering
    /// reads properties by name and never asks.
    keys: RefCell<Option<Vec<SmolStr>>>,
    /// The lambda definition this node is one evaluation of, when the
    /// engine wraps a lambda per evaluation (see [`Description::definition`]).
    pub definition: Option<i64>,
    /// Source span reported by Java.
    pub source: Option<SourceInfo>,
    /// Property values fetched so far.
    cache: RefCell<HashMap<SmolStr, PVector<Value>>>,
    /// `__`-prefixed slots the evaluator writes (type-argument back-fill).
    local: HashMap<SmolStr, PVector<Value>>,
    shared: Rc<BridgeShared>,
}

impl std::fmt::Debug for JavaNode {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("JavaNode")
            .field("id", &self.id)
            .field("classifier", &self.classifier)
            .field("name", &self.name)
            .finish_non_exhaustive()
    }
}

impl JavaNode {
    /// Forget the property values fetched so far. The Java graph has
    /// cycles (a class and its qualified properties, two functions that
    /// call each other), and the cache holds strong handles, so the
    /// caches of the nodes touched by one execution must be dropped when
    /// it ends or those nodes would never be freed.
    pub fn clear_cache(&self) {
        self.cache.borrow_mut().clear();
    }

    /// The node's property keys (`RustNodeBridge.keysOf`), read once.
    fn keys(&self) -> Result<Vec<SmolStr>, PureException> {
        if let Some(keys) = &*self.keys.borrow() {
            return Ok(keys.clone());
        }
        self.shared.jni_reads.set(self.shared.jni_reads.get() + 1);
        let mut guard = self
            .shared
            .vm
            .attach_current_thread()
            .map_err(|e| ffi_error(format!("attach: {e}")))?;
        let env: &mut JNIEnv = &mut guard;
        let shared = &self.shared;
        let keys = in_local_frame(env, shared, 8, |env| {
            let what = format!("reading the keys of Java node {}", self.id);
            // SAFETY: `keys_of` was resolved on the bridge's class with the
            // signature `(J)[Ljava/lang/String;`, the bridge global
            // reference is an instance of that class, and the one argument
            // is a `long`.
            let result = unsafe {
                env.call_method_unchecked(
                    shared.bridge.as_obj(),
                    shared.methods.keys_of,
                    ReturnType::Array,
                    &[JValue::Long(self.id).as_jni()],
                )
            };
            let arr = result
                .and_then(|v| v.l())
                .map_err(|e| jni_failure(env, shared, &what, &e))?;
            let arr = JObjectArray::from(arr);
            let len = env
                .get_array_length(&arr)
                .map_err(|e| jni_failure(env, shared, &what, &e))?;
            let mut keys = Vec::with_capacity(usize::try_from(len).unwrap_or(0));
            for i in 0..len {
                let key = in_local_frame(env, shared, 2, |env| {
                    let k = env
                        .get_object_array_element(&arr, i)
                        .map_err(|e| jni_failure(env, shared, &what, &e))?;
                    match env.get_string(&JString::from(k)) {
                        Ok(s) => Ok(SmolStr::new(String::from(s))),
                        Err(e) => Err(jni_failure(env, shared, &what, &e)),
                    }
                })?;
                keys.push(key);
            }
            Ok(keys)
        })?;
        *self.keys.borrow_mut() = Some(keys.clone());
        Ok(keys)
    }

    /// Keep a read failure, with its kind, for the end of the call: the
    /// heap's object view has no error channel. A failure the boundary
    /// raised itself is anchored at this node, the place it was read from.
    fn note_failure(&self, mut failure: PureException) {
        let at_boundary = failure
            .source
            .as_ref()
            .is_none_or(|s| s.source == crate::failure::FFI_SOURCE);
        if at_boundary && let Some(source) = &self.source {
            failure.source = Some(source.clone());
        }
        self.shared.note_error(failure);
    }

    fn fetch(&self, name: &str) -> Result<PVector<Value>, PureException> {
        self.call_descriptions(self.shared.methods.property_values, name)
            .map(|values| values.unwrap_or_else(PVector::new))
    }

    /// Call a bridge method `(long id, String name) -> NodeDescription[]`
    /// and decode the result; a Java `null` is `None`. Every local
    /// reference the call creates lives in a frame of its own.
    fn call_descriptions(
        &self,
        method: jni::objects::JMethodID,
        name: &str,
    ) -> Result<Option<PVector<Value>>, PureException> {
        self.shared.jni_reads.set(self.shared.jni_reads.get() + 1);
        let mut guard = self
            .shared
            .vm
            .attach_current_thread()
            .map_err(|e| ffi_error(format!("attach: {e}")))?;
        let env: &mut JNIEnv = &mut guard;
        let shared = &self.shared;
        in_local_frame(env, shared, 16, |env| {
            let what = format!("reading `{name}` on Java node {}", self.id);
            let jname = env
                .new_string(name)
                .map_err(|e| jni_failure(env, shared, &what, &e))?;
            // SAFETY: `method` is one of the bridge's `(JLjava/lang/String;)
            // [Lorg/finos/legend/pure/rust/NodeDescription;` methods resolved
            // on its class, the bridge global reference is an instance of that
            // class, and the arguments are a `long` and a `String`.
            let result = unsafe {
                env.call_method_unchecked(
                    shared.bridge.as_obj(),
                    method,
                    ReturnType::Array,
                    &[
                        JValue::Long(self.id).as_jni(),
                        JValue::Object(&jname).as_jni(),
                    ],
                )
            };
            let arr = match result.and_then(|v| v.l()) {
                Ok(obj) if obj.is_null() => return Ok(None),
                Ok(obj) => JObjectArray::from(obj),
                Err(e) => return Err(jni_failure(env, shared, &what, &e)),
            };
            let len = env
                .get_array_length(&arr)
                .map_err(|e| jni_failure(env, shared, &what, &e))?;
            let mut values = PVector::new();
            for i in 0..len {
                let value = in_local_frame(env, shared, 8, |env| {
                    let obj = env
                        .get_object_array_element(&arr, i)
                        .map_err(|e| jni_failure(env, shared, &what, &e))?;
                    let d = read_description(env, &shared.fields, &obj)
                        .map_err(|e| jni_failure(env, shared, &what, &e))?;
                    description_to_value(shared, d)
                })?;
                values.push_back(value);
            }
            Ok(Some(values))
        })
    }
}

impl TypedObject for JavaNode {
    fn classifier_path(&self) -> &str {
        &self.classifier
    }

    fn get_property(&self, name: &str) -> Option<Value> {
        self.get_property_values(name).get(0).cloned()
    }

    fn get_property_values(&self, name: &str) -> PVector<Value> {
        if name.starts_with("__") {
            return self.local.get(name).cloned().unwrap_or_else(PVector::new);
        }
        if let Some(v) = self.cache.borrow().get(name) {
            return v.clone();
        }
        match self.fetch(name) {
            Ok(values) => {
                self.cache
                    .borrow_mut()
                    .insert(SmolStr::new(name), values.clone());
                values
            }
            Err(failure) => {
                self.note_failure(failure);
                PVector::new()
            }
        }
    }

    fn property_names(&self) -> Vec<SmolStr> {
        let mut names = self.keys().unwrap_or_else(|failure| {
            self.note_failure(failure);
            Vec::new()
        });
        names.extend(self.local.keys().cloned());
        names
    }

    fn node_name(&self) -> Option<SmolStr> {
        self.name.clone()
    }

    fn user_path(&self) -> Option<SmolStr> {
        self.user_path.clone()
    }

    fn source_info(&self) -> Option<SourceInfo> {
        self.source.clone()
    }

    /// The values a lambda captured, read from the Java
    /// `LambdaWithContext` behind this node; `None` when the node is a
    /// plain lambda (or not a lambda at all).
    fn captured_values(&self, name: &str) -> Option<PVector<Value>> {
        match self.call_descriptions(self.shared.methods.captured_value, name) {
            Ok(values) => values,
            Err(failure) => {
                self.note_failure(failure);
                None
            }
        }
    }

    fn set_property(&mut self, name: &str, value: Value) -> Result<(), PureRuntimeError> {
        if name.starts_with("__") {
            self.local
                .entry(SmolStr::new(name))
                .or_default()
                .push_back(value);
            return Ok(());
        }
        Err(PureRuntimeError::Unsupported("mutating a Java node".into()))
    }

    fn set_property_values(
        &mut self,
        name: &str,
        values: &[Value],
    ) -> Result<(), PureRuntimeError> {
        if name.starts_with("__") {
            self.local
                .insert(SmolStr::new(name), values.iter().cloned().collect());
            return Ok(());
        }
        Err(PureRuntimeError::Unsupported("mutating a Java node".into()))
    }

    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn as_any_mut(&mut self) -> &mut dyn std::any::Any {
        self
    }
}

/// Which Rust shape a Java node became (test hook).
#[must_use]
pub fn crossing_kind(value: &Value) -> &'static str {
    match value {
        Value::Element(_) => "element",
        Value::EnumValue { .. } => "enum",
        Value::Object(handle) => match &*handle.borrow() {
            HeapEntry::Typed(obj) if obj.as_any().is::<JavaNode>() => "javaNode",
            _ => "rustObject",
        },
        _ => "primitive",
    }
}
