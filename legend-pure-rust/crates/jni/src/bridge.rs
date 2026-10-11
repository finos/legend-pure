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
//! ids, the two identity tables, and the last error raised while a node
//! was read under a `RefCell` borrow (where there is no error channel).
//!
//! **Rule:** never hold `handles.borrow()` or `java_nodes.borrow()` across
//! a JNI call. The Java side only reads its own graph during those calls,
//! but a re-entrant call into the evaluator would deadlock the borrow.

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Weak;

use jni::objects::{GlobalRef, JMethodID};
use jni::{JNIEnv, JavaVM};
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
#[allow(missing_docs)] // one id per `RustNodeBridge` method of the same name
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
    /// The model, valid for the context's lifetime (the context frees
    /// the evaluator, and with it every node, before the model).
    pub model: *const PureModel,
    /// Java-to-Rust identity: bridge id → the one heap entry wrapping it.
    pub java_nodes: RefCell<HashMap<i64, Weak<RefCell<HeapEntry>>>>,
    /// Rust-to-Java identity: Rust objects exposed to Java.
    pub handles: RefCell<JniHandleTable>,
    /// The last failure raised while reading a node under a borrow;
    /// surfaced by the next native entry point.
    pub last_error: RefCell<Option<String>>,
}

impl BridgeShared {
    /// The model behind the raw pointer.
    #[must_use]
    pub fn model(&self) -> &PureModel {
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
        self.java_nodes
            .borrow_mut()
            .retain(|_, weak| weak.strong_count() > 0);
    }

    /// Live Java-backed nodes plus Rust objects held for Java.
    #[must_use]
    pub fn live_count(&self) -> usize {
        let mut nodes = self.java_nodes.borrow_mut();
        nodes.retain(|_, weak| weak.strong_count() > 0);
        nodes.len() + self.handles.borrow().live_count()
    }

    /// Record a failure that happened where no error can be returned.
    pub fn note_error(&self, message: String) {
        let mut slot = self.last_error.borrow_mut();
        if slot.is_none() {
            *slot = Some(message);
        }
    }

    /// Take the recorded failure, if any.
    pub fn take_error(&self) -> Option<String> {
        self.last_error.borrow_mut().take()
    }
}
