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

//! The evaluator session behind one Java `PureRustEvaluator`.

use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::heap::{HeapEntry, ObjectHandle};
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::model::PureModel;
use smol_str::SmolStr;

use crate::bridge::{BridgeShared, JniHandleTable};
use crate::java_node::JavaNode;

/// Classifier of a `.par` native-function node: the only Java function
/// shape that is callable before any lowering exists.
const NATIVE_FUNCTION: &str = "meta::pure::metamodel::function::NativeFunction";

/// Classifier of a Java lambda: lowering it through the bridge is Step 3,
/// so until then it is refused as unsupported rather than "not found".
const LAMBDA_FUNCTION: &str = legend_pure_evaluator::m3_paths::LAMBDA_FUNCTION;

fn ffi_error(message: String) -> PureException {
    PureException::execution(
        PureRuntimeError::EvaluationError(message),
        legend_pure_metamodel::source_info::SourceInfo::new("<ffi>", 0, 0, 0, 0),
        Vec::new(),
    )
}

/// Model, registry and evaluator behind raw pointers (freed in `Drop`),
/// plus the state shared with every Java-backed node.
pub struct JniContext {
    model: *mut PureModel,
    registry: *mut NativeRegistry,
    evaluator: *mut Evaluator<'static>,
    /// Shared with every [`JavaNode`].
    pub shared: Rc<BridgeShared>,
}

impl JniContext {
    /// Box the model and registry, build an evaluator over both, and wire
    /// the bridge state. The evaluator starts with no scope: every
    /// [`start`](Self::start) pushes and pops its own, as one execution.
    pub fn new(
        model: PureModel,
        registry: NativeRegistry,
        vm: jni::JavaVM,
        bridge: jni::objects::GlobalRef,
        methods: crate::bridge::BridgeMethods,
    ) -> Self {
        let model_ptr = Box::into_raw(Box::new(model));
        let registry_ptr = Box::into_raw(Box::new(registry));
        let evaluator = Evaluator::new(unsafe { &*model_ptr }, unsafe { &*registry_ptr });
        let evaluator_ptr = Box::into_raw(Box::new(evaluator)).cast::<Evaluator<'static>>();
        let shared = Rc::new(BridgeShared {
            vm,
            bridge,
            methods,
            model: model_ptr.cast_const(),
            java_nodes: std::cell::RefCell::new(std::collections::HashMap::new()),
            handles: std::cell::RefCell::new(JniHandleTable::new()),
            last_error: std::cell::RefCell::new(None),
        });
        Self {
            model: model_ptr,
            registry: registry_ptr,
            evaluator: evaluator_ptr,
            shared,
        }
    }

    fn evaluator(&mut self) -> &mut Evaluator<'static> {
        unsafe { &mut *self.evaluator }
    }

    /// Run a function with already-crossed arguments, one scope per call.
    ///
    /// A model element runs through `apply_callable`; a Java `NativeFunction`
    /// node whose name is a registered key runs through `call_native`; a Java
    /// `LambdaFunction` is `Unsupported` until the bridge can lower it;
    /// anything else is `FunctionNotFound` with the node's user path.
    ///
    /// # Errors
    /// Whatever the evaluator raises, or a failure recorded while a Java
    /// node was read.
    pub fn start(&mut self, function: &Value, args: &[Value]) -> Result<Value, PureException> {
        let evaluator = unsafe { &mut *self.evaluator };
        evaluator.context_mut().push_scope();
        let result = match function {
            Value::Element(_) => evaluator.apply_callable(function, args),
            Value::Object(handle) => match java_function(handle) {
                Some((classifier, name, _)) if classifier == NATIVE_FUNCTION => {
                    evaluator.call_native(&name, args)
                }
                Some((classifier, _, _)) if classifier == LAMBDA_FUNCTION => {
                    Err(PureException::from(PureRuntimeError::Unsupported(
                        "evaluating a Java LambdaFunction through the bridge",
                    )))
                }
                Some((_, _, user_path)) => Err(PureException::from(
                    PureRuntimeError::FunctionNotFound(user_path),
                )),
                None => Err(ffi_error("start: the function is not a graph node".into())),
            },
            other => Err(ffi_error(format!(
                "start: expected a function node, got {other:?}"
            ))),
        };
        evaluator.context_mut().pop_scope();
        if let Some(message) = self.shared.take_error() {
            return Err(ffi_error(message));
        }
        result
    }

    fn resolve_i64(&self, raw: i64) -> Option<ObjectHandle> {
        self.shared
            .handles
            .borrow()
            .lookup(JniHandleTable::from_i64(raw))
            .cloned()
    }

    fn stale(raw: i64) -> PureException {
        ffi_error(format!("Stale or unknown JNI handle: {raw}"))
    }

    /// Every value of a property of a Rust object held for Java; empty when
    /// the property is unset (re-wrapping reads optional slots on every
    /// value, so an absent property is not an error).
    ///
    /// # Errors
    /// Stale handle.
    pub fn get_property_values(
        &mut self,
        raw: i64,
        name: &str,
    ) -> Result<PVector<Value>, PureException> {
        let handle = self.resolve_i64(raw).ok_or_else(|| Self::stale(raw))?;
        let values = self
            .evaluator()
            .heap()
            .get_property_values(&handle, name)
            .map_err(PureException::from)?;
        if let Some(message) = self.shared.take_error() {
            return Err(ffi_error(message));
        }
        Ok(values)
    }

    /// Property names of a Rust object held for Java.
    ///
    /// # Errors
    /// Stale handle.
    pub fn property_names(&mut self, raw: i64) -> Result<Vec<SmolStr>, PureException> {
        let handle = self.resolve_i64(raw).ok_or_else(|| Self::stale(raw))?;
        self.evaluator()
            .heap()
            .property_names(&handle)
            .map_err(PureException::from)
    }

    /// Classifier user path of a Rust object held for Java.
    ///
    /// # Errors
    /// Stale handle.
    pub fn get_classifier(&mut self, raw: i64) -> Result<String, PureException> {
        let handle = self.resolve_i64(raw).ok_or_else(|| Self::stale(raw))?;
        self.evaluator()
            .heap()
            .classifier(&handle)
            .map(|s| s.to_string())
            .map_err(PureException::from)
    }

    /// Allocate a dynamic object with the given property values and hand
    /// Java its handle.
    ///
    /// # Errors
    /// A property the heap refuses.
    pub fn new_object(
        &mut self,
        classifier: &str,
        properties: &[(String, Vec<Value>)],
    ) -> Result<i64, PureException> {
        let handle = self.evaluator().heap_mut().alloc_dynamic(classifier);
        for (name, values) in properties {
            handle
                .borrow_mut()
                .mutate_set(name, values)
                .map_err(PureException::from)?;
        }
        Ok(self.emit_handle(handle))
    }

    /// Register a Rust object for Java and return its id.
    pub fn emit_handle(&self, handle: ObjectHandle) -> i64 {
        JniHandleTable::to_i64(self.shared.handles.borrow_mut().register(handle))
    }

    /// Drop the strong reference Java held for `raw`. Idempotent.
    pub fn release(&self, raw: i64) {
        self.shared
            .handles
            .borrow_mut()
            .release(JniHandleTable::from_i64(raw));
    }

    /// Live handles in both directions.
    #[must_use]
    pub fn live_handle_count(&self) -> usize {
        self.shared.live_count()
    }
}

/// `(classifier, name, user path)` of a Java-backed node, or `None` when
/// the handle is not one. The user path falls back to the name.
fn java_function(handle: &ObjectHandle) -> Option<(SmolStr, SmolStr, SmolStr)> {
    match &*handle.borrow() {
        HeapEntry::Typed(obj) => obj.as_any().downcast_ref::<JavaNode>().map(|n| {
            let name = n.name.clone().unwrap_or_default();
            let path = n.user_path.clone().unwrap_or_else(|| name.clone());
            (n.classifier.clone(), name, path)
        }),
        HeapEntry::Dynamic(_) => None,
    }
}

impl Drop for JniContext {
    fn drop(&mut self) {
        unsafe {
            drop(Box::from_raw(self.evaluator));
            drop(Box::from_raw(self.registry));
            drop(Box::from_raw(self.model));
        }
    }
}
