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

use std::collections::HashMap;
use std::rc::Rc;

use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::heap::{HeapEntry, ObjectHandle};
use legend_pure_evaluator::lower::{LowerError, Lowering, ModelExtension, Node};
use legend_pure_evaluator::m3_paths;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::{FunctionValue, LambdaClosure, Value};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{Parameter, ValueSpec};
use smol_str::SmolStr;

use crate::PVector;
use crate::bridge::{BridgeShared, JniHandleTable};
use crate::failure::ffi_error;
use crate::java_node::JavaNode;

/// The function classifiers `start` tells apart, resolved once to the
/// model's ids: a node is classified by id, never by its classifier's
/// spelling.
struct FunctionKinds {
    lambda: Option<ElementId>,
    native: Option<ElementId>,
    defined: Option<ElementId>,
}

/// What a function node handed to `start` is.
enum FunctionKind {
    /// A lambda: lowered with its captures and applied. `definition` keys
    /// the closure cache when the engine names what the value stands for.
    Lambda { definition: Option<i64> },
    /// A native function: called through the registry under its key.
    Native(SmolStr),
    /// A function with a body (`ConcreteFunctionDefinition`): lowered as
    /// an element, with everything it calls, and applied.
    Defined,
    /// Anything else, with its user path when it has one.
    Other(Option<SmolStr>),
}

impl FunctionKinds {
    fn resolve(model: &PureModel) -> Self {
        Self {
            lambda: m3_paths::resolve(model, m3_paths::LAMBDA_FUNCTION),
            native: m3_paths::resolve(model, m3_paths::NATIVE_FUNCTION),
            defined: m3_paths::resolve(model, m3_paths::CONCRETE_FUNCTION_DEFINITION),
        }
    }

    fn of(&self, model: &PureModel, handle: &ObjectHandle) -> FunctionKind {
        let entry = handle.borrow();
        let HeapEntry::Typed(obj) = &*entry else {
            return FunctionKind::Other(None);
        };
        let Some(node) = obj.as_any().downcast_ref::<JavaNode>() else {
            return FunctionKind::Other(None);
        };
        let path = node.user_path.clone().or_else(|| node.name.clone());
        let Some(classifier) = model.resolve_fqn_str(&node.classifier) else {
            return FunctionKind::Other(path);
        };
        let is = |id: Option<ElementId>| id == Some(classifier);
        if is(self.lambda) {
            FunctionKind::Lambda {
                definition: node.definition,
            }
        } else if is(self.native)
            && let Some(key) = &node.name
        {
            FunctionKind::Native(key.clone())
        } else if is(self.defined) {
            FunctionKind::Defined
        } else {
            FunctionKind::Other(path)
        }
    }
}

/// A lambda as lowered once: its parameters and body, which depend on its
/// definition only, and the names of the variables it captures, whose
/// values are read again on every call.
struct CachedClosure {
    parameters: std::sync::Arc<[Parameter]>,
    body: std::sync::Arc<[ValueSpec]>,
    open_variables: Vec<SmolStr>,
}

impl CachedClosure {
    fn of(closure: &LambdaClosure) -> Self {
        Self {
            parameters: closure.parameters.clone(),
            body: closure.body.clone(),
            open_variables: closure.captures.keys().cloned().collect(),
        }
    }
}

/// One call's lowering of values through the closure cache: a lambda
/// whose definition is cached is instantiated over the values it
/// captured this time; any other value goes through the lowering's
/// `lower_value`. Lambdas lowered in full are kept in `added` and enter
/// the cache once the call's lowering is complete.
struct CachedLowering<'c, 'm> {
    lowering: Lowering<'m>,
    model: &'m PureModel,
    kinds: &'c FunctionKinds,
    cache: &'c HashMap<i64, CachedClosure>,
    added: Vec<(i64, CachedClosure)>,
}

impl CachedLowering<'_, '_> {
    /// A value read from the node graph, made evaluator-native as
    /// [`Lowering::lower_value`] makes it, lambdas through the cache.
    fn lower_value(&mut self, value: &Value) -> Result<Value, LowerError> {
        match value {
            Value::Collection(items) => {
                let mut out = Vec::with_capacity(items.len());
                for item in items.iter() {
                    out.push(self.lower_value(item)?);
                }
                Ok(Value::from_vec(out))
            }
            Value::Object(handle) => match self.kinds.of(self.model, handle) {
                FunctionKind::Lambda { definition } => Ok(Value::Function(Box::new(
                    FunctionValue::Lambda(self.closure(value, handle, definition)?),
                ))),
                _ => self.lowering.lower_value(value),
            },
            other => self.lowering.lower_value(other),
        }
    }

    /// The closure for one lambda node: from the cache when its
    /// definition is there, else lowered in full (and cached when the
    /// engine names its definition).
    fn closure(
        &mut self,
        value: &Value,
        handle: &ObjectHandle,
        definition: Option<i64>,
    ) -> Result<LambdaClosure, LowerError> {
        let cached = definition.and_then(|key| {
            self.cache.get(&key).or_else(|| {
                self.added
                    .iter()
                    .find(|(added, _)| *added == key)
                    .map(|(_, cached)| cached)
            })
        });
        if let Some(cached) = cached {
            let parameters = cached.parameters.clone();
            let body = cached.body.clone();
            let open_variables = cached.open_variables.clone();
            let node = Node::new(handle.clone());
            let mut captures = HashMap::with_capacity(open_variables.len());
            for name in open_variables {
                let Some(values) = node.captured_values(&name) else {
                    return Err(LowerError::Unsupported(format!(
                        "lowering captured variables (`{name}` of a lambda without its context)"
                    )));
                };
                let values: Vec<Value> = values.iter().cloned().collect();
                let lowered = self.lower_value(&Value::from_vec(values))?;
                captures.insert(name, lowered);
            }
            return Ok(LambdaClosure {
                parameters,
                body,
                captures,
            });
        }
        let closure = self.lowering.lower_root_lambda(value)?;
        if let Some(key) = definition {
            self.added.push((key, CachedClosure::of(&closure)));
        }
        Ok(closure)
    }
}

/// What a lowered call runs.
enum Callable {
    /// A native, by its registry key.
    Native(SmolStr),
    /// An element or closure the evaluator applies.
    Value(Value),
}

/// Where the last `start` on this thread spent its time: the lowering
/// (Phase A and B, including every JNI read it made) and the run.
#[derive(Debug, Clone, Copy, Default)]
pub struct Timings {
    /// Lowering, nanoseconds.
    pub lower_ns: u64,
    /// Running the lowered closure, nanoseconds.
    pub run_ns: u64,
    /// JNI property or capture reads made while the closure ran: zero
    /// means the run was the evaluator alone.
    pub run_jni_reads: u64,
    /// JNI property or capture reads made by the lowering: zero means
    /// everything came from the closure cache and the model.
    pub lower_jni_reads: u64,
}

thread_local! {
    /// The timings of the last lowered `start` on this thread.
    pub static LAST_TIMINGS: std::cell::Cell<Timings> = const { std::cell::Cell::new(Timings { lower_ns: 0, run_ns: 0, run_jni_reads: 0, lower_jni_reads: 0 }) };
}

/// Model, registry and evaluator behind raw pointers (freed in `Drop`),
/// plus the state shared with every Java-backed node.
///
/// The three boxes are self-referential: the evaluator borrows the model
/// and the registry for `'static`, which is a lie the context keeps true
/// by construction. The model and registry are allocated before the
/// evaluator and freed after it; the model is borrowed mutably only in
/// [`grow_model`](Self::grow_model), after the evaluator is dropped and
/// before a new one is built; every other access is shared.
pub struct JniContext {
    model: *mut PureModel,
    registry: *mut NativeRegistry,
    evaluator: *mut Evaluator<'static>,
    /// Shared with every [`JavaNode`].
    pub shared: Rc<BridgeShared>,
    /// The chunk the lowering appends to, once the model first grew.
    bridge_chunk: Option<u16>,
    /// Set once a panic unwound through an entry point: the evaluator's
    /// state is unknown from then on and every call is refused.
    pub poisoned: bool,
    /// The function classifiers, by id.
    kinds: FunctionKinds,
    /// Lowered lambdas by definition (`RustEngineHost.lambdaDefinition`),
    /// for the context's lifetime: the elements their bodies refer to live
    /// in the model as long.
    closures: HashMap<i64, CachedClosure>,
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
        fields: crate::bridge::DescriptionFields,
    ) -> Self {
        let model_ptr = Box::into_raw(Box::new(model));
        let registry_ptr = Box::into_raw(Box::new(registry));
        // SAFETY: both pointers come from `Box::into_raw` just above and
        // stay live until `Drop`, which frees the evaluator first.
        let evaluator = unsafe { Evaluator::new(&*model_ptr, &*registry_ptr) };
        let evaluator_ptr = Box::into_raw(Box::new(evaluator)).cast::<Evaluator<'static>>();
        let shared = Rc::new(BridgeShared {
            vm,
            bridge,
            methods,
            fields,
            model: model_ptr.cast_const(),
            java_nodes: std::cell::RefCell::new(std::collections::HashMap::new()),
            handles: std::cell::RefCell::new(JniHandleTable::new()),
            last_error: std::cell::RefCell::new(None),
            jni_reads: std::cell::Cell::new(0),
            released: std::cell::RefCell::new(Vec::new()),
        });
        // SAFETY: the model box was allocated just above and lives until
        // `Drop`; nothing borrows it mutably yet.
        let kinds = FunctionKinds::resolve(unsafe { &*model_ptr });
        Self {
            model: model_ptr,
            registry: registry_ptr,
            evaluator: evaluator_ptr,
            shared,
            bridge_chunk: None,
            poisoned: false,
            kinds,
            closures: HashMap::new(),
        }
    }

    fn evaluator(&mut self) -> &mut Evaluator<'static> {
        // SAFETY: the evaluator box is live and non-null outside
        // `grow_model` (which rebuilds it before returning, and leaves the
        // context poisoned if it panics); `&mut self` makes the borrow
        // unique.
        unsafe { &mut *self.evaluator }
    }

    fn model(&self) -> &PureModel {
        // SAFETY: the model box lives from `new` to `Drop` and is borrowed
        // mutably only inside `grow_model`, which holds `&mut self`.
        unsafe { &*self.model }
    }

    /// Phase B: append the lowered elements to the model. The evaluator
    /// borrows the model, so it is dropped first and rebuilt after.
    fn grow_model(&mut self, extension: ModelExtension) -> Result<(), PureException> {
        let chunk_id = extension.chunk_id();
        // SAFETY: the evaluator box is live (see `evaluator`); dropping it
        // ends its borrows of the model and registry. The pointer is
        // nulled at once so nothing reads it until it is rebuilt.
        unsafe { drop(Box::from_raw(self.evaluator)) };
        self.evaluator = std::ptr::null_mut();
        // SAFETY: no evaluator exists and `&mut self` excludes every other
        // borrow of the model (the lowering that read it was consumed
        // before this call); Java-backed nodes reach the model only while
        // a call reads them, and none runs during `apply`.
        let applied = extension.apply(unsafe { &mut *self.model });
        // SAFETY: the mutable borrow above has ended; both boxes stay live
        // until `Drop`, which frees this evaluator first.
        let evaluator = unsafe { Evaluator::new(&*self.model, &*self.registry) };
        self.evaluator = Box::into_raw(Box::new(evaluator)).cast::<Evaluator<'static>>();
        applied?;
        self.bridge_chunk = Some(chunk_id);
        Ok(())
    }

    /// Run a function node with already-crossed arguments, one scope per
    /// call: the function and its arguments are lowered (Phase A), the
    /// model grows by what they reference (Phase B), and the call runs.
    ///
    /// # Errors
    /// Whatever the lowering or the evaluator raises, or a failure
    /// recorded while a Java node was read (which wins: what follows a
    /// failed read is its consequence).
    pub fn start(&mut self, function: &Value, args: &[Value]) -> Result<Value, PureException> {
        let result = self.lower_and_run(function, args);
        // Whether the lowering or the run failed, the nodes this execution
        // touched hold caches (and cycles) that must go now.
        self.shared.end_execution();
        if let Some(failure) = self.shared.take_error() {
            return Err(failure);
        }
        result
    }

    fn lower_and_run(&mut self, function: &Value, args: &[Value]) -> Result<Value, PureException> {
        let started = std::time::Instant::now();
        let reads_at_start = self.shared.jni_reads.get();
        let (callable, args) = self.lower_call(function, args)?;
        let lowered = std::time::Instant::now();
        let reads_before = self.shared.jni_reads.get();
        let evaluator = self.evaluator();
        evaluator.context_mut().push_scope();
        let result = match &callable {
            Callable::Native(key) => evaluator.call_native(key, &args),
            Callable::Value(value) => evaluator.apply_callable(value, &args),
        };
        evaluator.context_mut().pop_scope();
        let ran = std::time::Instant::now();
        LAST_TIMINGS.with(|t| {
            t.set(Timings {
                lower_ns: u64::try_from((lowered - started).as_nanos()).unwrap_or(u64::MAX),
                run_ns: u64::try_from((ran - lowered).as_nanos()).unwrap_or(u64::MAX),
                run_jni_reads: self.shared.jni_reads.get() - reads_before,
                lower_jni_reads: reads_before - reads_at_start,
            });
        });
        result
    }

    /// Phase A and B for one call: what the function node is (by its
    /// classifier's element id), its lowering and the lowering of every
    /// argument (lambdas through the closure cache), then the model's
    /// growth and the cache's.
    fn lower_call(
        &mut self,
        function: &Value,
        args: &[Value],
    ) -> Result<(Callable, Vec<Value>), PureException> {
        let model: &PureModel = self.model();
        let chunk = self
            .bridge_chunk
            .unwrap_or_else(|| u16::try_from(model.chunks.len()).expect("chunk ids are u16"));
        let mut lowering = CachedLowering {
            lowering: Lowering::new(model, chunk, model.chunk_len(chunk)),
            model,
            kinds: &self.kinds,
            cache: &self.closures,
            added: Vec::new(),
        };
        let lowered = (|| -> Result<(Callable, Vec<Value>), PureException> {
            let callable = match function {
                Value::Element(_) => Callable::Value(function.clone()),
                Value::Object(handle) => match self.kinds.of(model, handle) {
                    FunctionKind::Lambda { definition } => {
                        Callable::Value(Value::Function(Box::new(FunctionValue::Lambda(
                            lowering.closure(function, handle, definition)?,
                        ))))
                    }
                    FunctionKind::Native(key) => Callable::Native(key),
                    FunctionKind::Defined => {
                        Callable::Value(lowering.lowering.lower_value(function)?)
                    }
                    FunctionKind::Other(Some(path)) => {
                        return Err(PureException::from(PureRuntimeError::FunctionNotFound(
                            path,
                        )));
                    }
                    FunctionKind::Other(None) => {
                        return Err(ffi_error("start: the function is not a function node"));
                    }
                },
                other => {
                    return Err(ffi_error(format!(
                        "start: expected a function node, got {}",
                        other.type_name()
                    )));
                }
            };
            let args = args
                .iter()
                .map(|arg| lowering.lower_value(arg))
                .collect::<Result<Vec<_>, _>>()?;
            Ok((callable, args))
        })();
        // A property that could not be read lowers to nothing; surface the
        // read failure before trusting the tree.
        if let Some(failure) = self.shared.take_error() {
            return Err(failure);
        }
        let lowered = lowered?;
        let CachedLowering {
            lowering, added, ..
        } = lowering;
        let extension = lowering.into_extension();
        if !extension.is_empty() {
            self.grow_model(extension)?;
        }
        self.closures.extend(added);
        Ok(lowered)
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
    /// the property is unset.
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
        if let Some(failure) = self.shared.take_error() {
            return Err(failure);
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

impl Drop for JniContext {
    fn drop(&mut self) {
        // SAFETY: the three pointers come from `Box::into_raw` and are
        // freed here only, the evaluator (which borrows the other two)
        // first; a null evaluator is one `grow_model` had dropped.
        unsafe {
            if !self.evaluator.is_null() {
                drop(Box::from_raw(self.evaluator));
            }
            drop(Box::from_raw(self.registry));
            drop(Box::from_raw(self.model));
        }
    }
}
