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

//! The Java engine as the evaluator's host: every read the bridge makes
//! (`RustNodeBridge.describe`, `propertyValues`, `capturedValue`,
//! `keysOf`, `forget`) is one JNI call here, decoded into the
//! description the evaluator crate reads.

use std::rc::Rc;

use jni::objects::{GlobalRef, JFieldID, JMethodID, JObject, JObjectArray, JString, JValue};
use jni::signature::{Primitive, ReturnType};
use jni::{JNIEnv, JavaVM};
use legend_pure_evaluator::bridge::description::Description;
use legend_pure_evaluator::bridge::host::Host;
use legend_pure_evaluator::error::PureException;
use smol_str::SmolStr;

use crate::bridge::{BridgeMethods, DescriptionFields};
use crate::failure::{boundary_error, in_local_frame, jni_failure};

/// What reaching Java takes: the JVM (so a read made outside a native
/// call can attach), the one global reference the crate keeps (the Java
/// `RustNodeBridge`), and the method and field ids resolved once.
struct Wiring {
    vm: JavaVM,
    bridge: GlobalRef,
    methods: BridgeMethods,
    fields: DescriptionFields,
}

/// The Java host of one evaluator. Clones share the wiring: the context
/// holds one for the calls it makes with an entry point's `JNIEnv`, the
/// evaluator crate holds another behind [`Host`].
#[derive(Clone)]
pub struct JniHost {
    wiring: Rc<Wiring>,
}

impl JniHost {
    /// A host over the bridge object and the ids resolved on its class.
    #[must_use]
    pub fn new(
        vm: JavaVM,
        bridge: GlobalRef,
        methods: BridgeMethods,
        fields: DescriptionFields,
    ) -> Self {
        Self {
            wiring: Rc::new(Wiring {
                vm,
                bridge,
                methods,
                fields,
            }),
        }
    }

    /// The Java `RustNodeBridge`.
    #[must_use]
    pub fn bridge(&self) -> &JObject<'static> {
        self.wiring.bridge.as_obj()
    }

    /// The bridge's method ids.
    #[must_use]
    pub fn methods(&self) -> &BridgeMethods {
        &self.wiring.methods
    }

    /// Call `RustNodeBridge.describe(node)` and decode the result, in a
    /// local frame of its own.
    ///
    /// # Errors
    /// The Java exception `describe` raised (a `null` node), or a JNI
    /// failure.
    pub fn describe(&self, env: &mut JNIEnv, obj: &JObject) -> Result<Description, PureException> {
        in_local_frame(env, self, 8, |env| {
            // SAFETY: `describe` was resolved on the bridge's class with the
            // signature `(Lorg/finos/legend/pure/m4/coreinstance/CoreInstance;)
            // Lorg/finos/legend/pure/rust/NodeDescription;`, the bridge global
            // reference is an instance of that class, and `obj` is a
            // `CoreInstance` (or null, which `describe` refuses with an exception).
            let described = unsafe {
                env.call_method_unchecked(
                    self.bridge(),
                    self.wiring.methods.describe,
                    ReturnType::Object,
                    &[JValue::Object(obj).as_jni()],
                )
            };
            let desc_obj = described
                .and_then(|v| v.l())
                .map_err(|e| jni_failure(env, self, "describe", &e))?;
            read_description(env, &self.wiring.fields, &desc_obj)
                .map_err(|e| jni_failure(env, self, "describe", &e))
        })
    }

    /// Call a bridge method `(long id, String name) -> NodeDescription[]`
    /// and decode the result; a Java `null` is `None`. Every local
    /// reference the call creates lives in a frame of its own.
    fn call_descriptions(
        &self,
        method: JMethodID,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        let mut guard = self
            .wiring
            .vm
            .attach_current_thread()
            .map_err(|e| boundary_error(format!("attach: {e}")))?;
        let env: &mut JNIEnv = &mut guard;
        in_local_frame(env, self, 16, |env| {
            let what = format!("reading `{name}` on Java node {id}");
            let jname = env
                .new_string(name)
                .map_err(|e| jni_failure(env, self, &what, &e))?;
            // SAFETY: `method` is one of the bridge's `(JLjava/lang/String;)
            // [Lorg/finos/legend/pure/rust/NodeDescription;` methods resolved
            // on its class, the bridge global reference is an instance of that
            // class, and the arguments are a `long` and a `String`.
            let result = unsafe {
                env.call_method_unchecked(
                    self.bridge(),
                    method,
                    ReturnType::Array,
                    &[JValue::Long(id).as_jni(), JValue::Object(&jname).as_jni()],
                )
            };
            let arr = match result.and_then(|v| v.l()) {
                Ok(obj) if obj.is_null() => return Ok(None),
                Ok(obj) => JObjectArray::from(obj),
                Err(e) => return Err(jni_failure(env, self, &what, &e)),
            };
            let len = env
                .get_array_length(&arr)
                .map_err(|e| jni_failure(env, self, &what, &e))?;
            let mut values = Vec::with_capacity(usize::try_from(len).unwrap_or(0));
            for i in 0..len {
                let value = in_local_frame(env, self, 8, |env| {
                    let obj = env
                        .get_object_array_element(&arr, i)
                        .map_err(|e| jni_failure(env, self, &what, &e))?;
                    read_description(env, &self.wiring.fields, &obj)
                        .map_err(|e| jni_failure(env, self, &what, &e))
                })?;
                values.push(value);
            }
            Ok(Some(values))
        })
    }
}

impl Host for JniHost {
    fn property_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        self.call_descriptions(self.wiring.methods.property_values, id, name)
    }

    fn captured_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        self.call_descriptions(self.wiring.methods.captured_value, id, name)
    }

    fn keys_of(&self, id: i64) -> Result<Vec<SmolStr>, PureException> {
        let mut guard = self
            .wiring
            .vm
            .attach_current_thread()
            .map_err(|e| boundary_error(format!("attach: {e}")))?;
        let env: &mut JNIEnv = &mut guard;
        in_local_frame(env, self, 8, |env| {
            let what = format!("reading the keys of Java node {id}");
            // SAFETY: `keys_of` was resolved on the bridge's class with the
            // signature `(J)[Ljava/lang/String;`, the bridge global
            // reference is an instance of that class, and the one argument
            // is a `long`.
            let result = unsafe {
                env.call_method_unchecked(
                    self.bridge(),
                    self.wiring.methods.keys_of,
                    ReturnType::Array,
                    &[JValue::Long(id).as_jni()],
                )
            };
            let arr = result
                .and_then(|v| v.l())
                .map_err(|e| jni_failure(env, self, &what, &e))?;
            let arr = JObjectArray::from(arr);
            let len = env
                .get_array_length(&arr)
                .map_err(|e| jni_failure(env, self, &what, &e))?;
            let mut keys = Vec::with_capacity(usize::try_from(len).unwrap_or(0));
            for i in 0..len {
                let key = in_local_frame(env, self, 2, |env| {
                    let k = env
                        .get_object_array_element(&arr, i)
                        .map_err(|e| jni_failure(env, self, &what, &e))?;
                    match env.get_string(&JString::from(k)) {
                        Ok(s) => Ok(SmolStr::new(String::from(s))),
                        Err(e) => Err(jni_failure(env, self, &what, &e)),
                    }
                })?;
                keys.push(key);
            }
            Ok(keys)
        })
    }

    /// `RustNodeBridge.forget(ids)`, so the bridge's id tables do not
    /// grow for the JVM's lifetime. A failure to tell Java is dropped
    /// with the pending exception.
    fn forget(&self, ids: &[i64]) {
        let Ok(mut guard) = self.wiring.vm.attach_current_thread() else {
            return;
        };
        let env: &mut JNIEnv = &mut guard;
        let told = env.with_local_frame(4, |env| -> jni::errors::Result<()> {
            let array = env.new_long_array(i32::try_from(ids.len()).unwrap_or(i32::MAX))?;
            env.set_long_array_region(&array, 0, ids)?;
            // SAFETY: `forget` was resolved on the bridge's class with
            // the signature `([J)V`; the bridge global reference is an
            // instance of that class and the one argument is a `long[]`.
            unsafe {
                env.call_method_unchecked(
                    self.bridge(),
                    self.wiring.methods.forget,
                    ReturnType::Primitive(Primitive::Void),
                    &[JValue::Object(&array).as_jni()],
                )
            }?;
            Ok(())
        });
        if told.is_err() {
            let _ = crate::failure::take_pending(env);
        }
    }
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
    Ok(Description {
        kind: int(env, fields.kind)?,
        id: long(env, fields.id)?,
        definition: long(env, fields.definition)?,
        classifier: string_field(env, obj, fields.classifier)?.map(SmolStr::new),
        name: string_field(env, obj, fields.name)?.map(SmolStr::new),
        user_path: string_field(env, obj, fields.user_path)?.map(SmolStr::new),
        payload: string_field(env, obj, fields.payload)?,
        year: int(env, fields.year)?,
        month: int(env, fields.month)?,
        day: int(env, fields.day)?,
        hour: int(env, fields.hour)?,
        minute: int(env, fields.minute)?,
        second: int(env, fields.second)?,
        precision: int(env, fields.precision)?,
        subsecond: string_field(env, obj, fields.subsecond)?,
        source_id: string_field(env, obj, fields.source_id)?,
        line: int(env, fields.line)?,
        column: int(env, fields.column)?,
        end_line: int(env, fields.end_line)?,
        end_column: int(env, fields.end_column)?,
    })
}
