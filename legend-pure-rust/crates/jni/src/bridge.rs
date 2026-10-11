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

//! State shared between the context and every Java-backed node.
//!
//! One `BridgeShared` exists per `JniContext`. It owns the only JNI global
//! reference the crate keeps (the Java `RustNodeBridge`), the cached method
//! ids, the two identity tables, and the first failure raised while a node
//! was read through the heap's infallible object view (where there is no
//! error channel), kept typed.
//!
//! **Rule:** never hold `handles.borrow()` or `java_nodes.borrow()` across
//! a JNI call. The Java side only reads its own graph during those calls,
//! but a re-entrant call into the evaluator would deadlock the borrow.

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Weak;

use jni::objects::{GlobalRef, JFieldID, JMethodID, JValue};
use jni::signature::{Primitive, ReturnType};
use jni::{JNIEnv, JavaVM};
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::heap::{HeapEntry, ObjectHandle};
use legend_pure_metamodel::model::PureModel;
use slotmap::{SlotMap, new_key_type};

new_key_type! {
    /// Key of a Rust heap object exposed to Java.
    pub struct JniHandle;
}

/// Rust objects exposed to Java: one slot per `Rc`, idempotent.
pub struct JniHandleTable {
    live: SlotMap<JniHandle, ObjectHandle>,
    by_ptr: HashMap<*const RefCell<HeapEntry>, JniHandle>,
}

impl JniHandleTable {
    /// An empty table.
    #[must_use]
    pub fn new() -> Self {
        Self {
            live: SlotMap::with_key(),
            by_ptr: HashMap::new(),
        }
    }

    /// Register a handle and return its stable key. Idempotent — the same
    /// `Rc` pointer always maps to the same key, so one object keeps one
    /// id on the Java side.
    pub fn register(&mut self, handle: ObjectHandle) -> JniHandle {
        let key_ptr = std::rc::Rc::as_ptr(&handle);
        if let Some(&existing) = self.by_ptr.get(&key_ptr) {
            return existing;
        }
        let h = self.live.insert(handle);
        self.by_ptr.insert(key_ptr, h);
        h
    }

    /// Look up the object for a key; `None` once released.
    #[must_use]
    pub fn lookup(&self, h: JniHandle) -> Option<&ObjectHandle> {
        self.live.get(h)
    }

    /// Number of live entries.
    #[must_use]
    pub fn live_count(&self) -> usize {
        self.live.len()
    }

    /// Drop the strong reference for a key. Re-releasing is a no-op.
    pub fn release(&mut self, h: JniHandle) {
        if let Some(handle) = self.live.remove(h) {
            self.by_ptr.remove(&std::rc::Rc::as_ptr(&handle));
        }
    }

    /// The `long` Java holds for a key.
    #[allow(clippy::cast_possible_wrap)]
    #[must_use]
    pub fn to_i64(h: JniHandle) -> i64 {
        slotmap::Key::data(&h).as_ffi() as i64
    }

    /// The key behind a `long` Java handed back.
    #[allow(clippy::cast_sign_loss)]
    #[must_use]
    pub fn from_i64(v: i64) -> JniHandle {
        JniHandle::from(slotmap::KeyData::from_ffi(v as u64))
    }
}

impl Default for JniHandleTable {
    fn default() -> Self {
        Self::new()
    }
}

/// Java-side method ids, resolved once per context.
#[allow(missing_docs)]
pub struct BridgeMethods {
    pub describe: JMethodID,
    pub property_values: JMethodID,
    pub captured_value: JMethodID,
    pub new_boolean: JMethodID,
    pub new_integer: JMethodID,
    pub new_float: JMethodID,
    pub new_decimal: JMethodID,
    pub new_string: JMethodID,
    pub new_date: JMethodID,
    pub new_latest_date: JMethodID,
    pub new_strict_time: JMethodID,
    pub element: JMethodID,
    pub enum_value: JMethodID,
    pub node: JMethodID,
    pub rust_object: JMethodID,
    pub new_map: JMethodID,
    pub failure_of: JMethodID,
    pub forget: JMethodID,
    pub keys_of: JMethodID,
}

/// Java class names the crate talks to.
pub const BRIDGE_CLASS: &str = "org/finos/legend/pure/rust/RustNodeBridge";
/// The description record the bridge hands back per node.
pub const DESCRIPTION_CLASS: &str = "org/finos/legend/pure/rust/NodeDescription";
/// The only currency across the boundary.
pub const CORE_INSTANCE_CLASS: &str = "org/finos/legend/pure/m4/coreinstance/CoreInstance";

impl BridgeMethods {
    /// Resolve every method id on the bridge class.
    ///
    /// # Errors
    /// Any JNI lookup failure (the Java class is older than this binary).
    pub fn resolve(env: &mut JNIEnv) -> jni::errors::Result<Self> {
        let cls = env.find_class(BRIDGE_CLASS)?;
        let ci = format!("L{CORE_INSTANCE_CLASS};");
        let desc = format!("L{DESCRIPTION_CLASS};");
        Ok(Self {
            describe: env.get_method_id(&cls, "describe", format!("({ci}){desc}"))?,
            property_values: env.get_method_id(
                &cls,
                "propertyValues",
                format!("(JLjava/lang/String;)[{desc}"),
            )?,
            captured_value: env.get_method_id(
                &cls,
                "capturedValue",
                format!("(JLjava/lang/String;)[{desc}"),
            )?,
            new_boolean: env.get_method_id(&cls, "newBoolean", format!("(Z){ci}"))?,
            new_integer: env.get_method_id(&cls, "newInteger", format!("(J){ci}"))?,
            new_float: env.get_method_id(&cls, "newFloat", format!("(Ljava/lang/String;){ci}"))?,
            new_decimal: env.get_method_id(
                &cls,
                "newDecimal",
                format!("(Ljava/lang/String;){ci}"),
            )?,
            new_string: env.get_method_id(
                &cls,
                "newString",
                format!("(Ljava/lang/String;){ci}"),
            )?,
            new_date: env.get_method_id(
                &cls,
                "newDate",
                format!("(IIIIIILjava/lang/String;I){ci}"),
            )?,
            new_latest_date: env.get_method_id(&cls, "newLatestDate", format!("(){ci}"))?,
            new_strict_time: env.get_method_id(
                &cls,
                "newStrictTime",
                format!("(IIILjava/lang/String;I){ci}"),
            )?,
            element: env.get_method_id(&cls, "element", format!("(Ljava/lang/String;){ci}"))?,
            enum_value: env.get_method_id(
                &cls,
                "enumValue",
                format!("(Ljava/lang/String;Ljava/lang/String;){ci}"),
            )?,
            node: env.get_method_id(&cls, "node", format!("(J){ci}"))?,
            rust_object: env.get_method_id(
                &cls,
                "rustObject",
                format!("(JLjava/lang/String;){ci}"),
            )?,
            new_map: env.get_method_id(&cls, "newMap", format!("([{ci}[{ci}){ci}"))?,
            forget: env.get_method_id(&cls, "forget", "([J)V")?,
            keys_of: env.get_method_id(&cls, "keysOf", "(J)[Ljava/lang/String;")?,
            failure_of: env.get_method_id(
                &cls,
                "failureOf",
                "(Ljava/lang/Throwable;)[Ljava/lang/String;",
            )?,
        })
    }
}

/// The fields of the Java `NodeDescription`, resolved once per context:
/// reading a description is then one JNI call per field, with no class or
/// field lookup by name.
#[allow(missing_docs)]
pub struct DescriptionFields {
    pub kind: JFieldID,
    pub id: JFieldID,
    pub definition: JFieldID,
    pub classifier: JFieldID,
    pub name: JFieldID,
    pub user_path: JFieldID,
    pub payload: JFieldID,
    pub year: JFieldID,
    pub month: JFieldID,
    pub day: JFieldID,
    pub hour: JFieldID,
    pub minute: JFieldID,
    pub second: JFieldID,
    pub precision: JFieldID,
    pub subsecond: JFieldID,
    pub source_id: JFieldID,
    pub line: JFieldID,
    pub column: JFieldID,
    pub end_line: JFieldID,
    pub end_column: JFieldID,
}

impl DescriptionFields {
    /// Resolve every field id on the description class.
    ///
    /// # Errors
    /// Any JNI lookup failure (the Java class is older than this binary).
    pub fn resolve(env: &mut JNIEnv) -> jni::errors::Result<Self> {
        let cls = env.find_class(DESCRIPTION_CLASS)?;
        let string = "Ljava/lang/String;";
        let mut field = |name: &str, sig: &str| -> jni::errors::Result<JFieldID> {
            env.get_field_id(&cls, name, sig)
        };
        Ok(Self {
            kind: field("kind", "I")?,
            id: field("id", "J")?,
            definition: field("definition", "J")?,
            classifier: field("classifier", string)?,
            name: field("name", string)?,
            user_path: field("userPath", string)?,
            payload: field("payload", string)?,
            year: field("year", "I")?,
            month: field("month", "I")?,
            day: field("day", "I")?,
            hour: field("hour", "I")?,
            minute: field("minute", "I")?,
            second: field("second", "I")?,
            precision: field("precision", "I")?,
            subsecond: field("subsecond", string)?,
            source_id: field("sourceId", string)?,
            line: field("line", "I")?,
            column: field("column", "I")?,
            end_line: field("endLine", "I")?,
            end_column: field("endColumn", "I")?,
        })
    }
}

/// Everything a Java-backed node needs to reach back into Java.
pub struct BridgeShared {
    /// The JVM, so a node read outside a native call can attach.
    pub vm: JavaVM,
    /// The Java `RustNodeBridge` for this context.
    pub bridge: GlobalRef,
    /// Cached method ids.
    pub methods: BridgeMethods,
    /// Cached field ids of the description record.
    pub fields: DescriptionFields,
    /// The context's model (see [`model`](Self::model)).
    pub model: *const PureModel,
    /// Java-to-Rust identity: bridge id → the one heap entry wrapping it.
    pub java_nodes: RefCell<HashMap<i64, Weak<RefCell<HeapEntry>>>>,
    /// Rust-to-Java identity: Rust objects exposed to Java.
    pub handles: RefCell<JniHandleTable>,
    /// The first failure raised while reading a node through the heap's
    /// infallible view, with its kind; surfaced when the call ends.
    pub last_error: RefCell<Option<PureException>>,
    /// Property and capture reads made through JNI so far; the timings
    /// of a `start` report how many fell inside its run phase.
    pub jni_reads: std::cell::Cell<u64>,
    /// Bridge ids of Java-backed nodes that died since Java was last told
    /// (`RustNodeBridge.forget`), so its id tables shrink with Rust's.
    pub released: RefCell<Vec<i64>>,
}

impl BridgeShared {
    /// The model behind the raw pointer.
    ///
    /// # Safety
    /// The context that owns this `BridgeShared` must be alive and must not
    /// be growing its model (`JniContext::grow_model`): the model box lives
    /// from the context's creation to its drop and is borrowed mutably
    /// only there. Every caller in this crate runs inside an entry point
    /// of that context (crossing a value, or a node read while the
    /// lowering or the evaluator runs), never during the growth.
    #[must_use]
    pub unsafe fn model(&self) -> &PureModel {
        // SAFETY: guaranteed by the caller, per this function's contract.
        unsafe { &*self.model }
    }

    /// End of one execution: drop every Java-backed node's property cache
    /// (see [`JavaNode::clear_cache`](crate::java_node::JavaNode::clear_cache))
    /// and forget the nodes that nothing holds any more.
    pub fn end_execution(&self) {
        let nodes: Vec<ObjectHandle> = self
            .java_nodes
            .borrow()
            .values()
            .filter_map(std::rc::Weak::upgrade)
            .collect();
        for handle in &nodes {
            if let HeapEntry::Typed(obj) = &*handle.borrow()
                && let Some(node) = obj.as_any().downcast_ref::<crate::java_node::JavaNode>()
            {
                node.clear_cache();
            }
        }
        drop(nodes);
        self.forget_dead_nodes();
    }

    /// Drop the identity entries of nodes nothing holds any more and keep
    /// their ids for Java to forget: a dead node's id is never used again
    /// (describing the same Java object later assigns a fresh one).
    fn forget_dead_nodes(&self) {
        let mut released = self.released.borrow_mut();
        self.java_nodes.borrow_mut().retain(|id, weak| {
            let alive = weak.strong_count() > 0;
            if !alive {
                released.push(*id);
            }
            alive
        });
    }

    /// Live Java-backed nodes plus Rust objects held for Java.
    #[must_use]
    pub fn live_count(&self) -> usize {
        self.forget_dead_nodes();
        self.java_nodes.borrow().len() + self.handles.borrow().live_count()
    }

    /// Tell Java which node ids died (`RustNodeBridge.forget`), so the
    /// bridge's id tables do not grow for the JVM's lifetime.
    pub fn forget_released(&self, env: &mut JNIEnv) {
        let released = std::mem::take(&mut *self.released.borrow_mut());
        if released.is_empty() {
            return;
        }
        let told = env.with_local_frame(4, |env| -> jni::errors::Result<()> {
            let ids = env.new_long_array(i32::try_from(released.len()).unwrap_or(i32::MAX))?;
            env.set_long_array_region(&ids, 0, &released)?;
            // SAFETY: `forget` was resolved on the bridge's class with
            // the signature `([J)V`; the bridge global reference is an
            // instance of that class and the one argument is a `long[]`.
            unsafe {
                env.call_method_unchecked(
                    self.bridge.as_obj(),
                    self.methods.forget,
                    ReturnType::Primitive(Primitive::Void),
                    &[JValue::Object(&ids).as_jni()],
                )
            }?;
            Ok(())
        });
        if told.is_err() {
            let _ = crate::failure::take_pending(env);
        }
    }

    /// Record a failure that happened where no error can be returned;
    /// the first one is kept, since what follows it is its consequence.
    pub fn note_error(&self, failure: PureException) {
        let mut slot = self.last_error.borrow_mut();
        if slot.is_none() {
            *slot = Some(failure);
        }
    }

    /// Take the recorded failure, if any.
    pub fn take_error(&self) -> Option<PureException> {
        self.last_error.borrow_mut().take()
    }
}
