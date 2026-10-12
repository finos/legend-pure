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

//! The evaluator session behind one host evaluator: the model it grows,
//! the registry, the evaluator's state between calls, and the lowering
//! of each call through the closure cache.

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{Parameter, ValueSpec};
use smol_str::SmolStr;

use crate::bridge::failure::boundary_error;
use crate::bridge::host::{Host, HostNode, HostShared};
use crate::bridge::timings::{self, Timings};
use crate::error::{PureException, PureRuntimeError};
use crate::eval::{Evaluator, EvaluatorState};
use crate::exec::LambdaBody;
use crate::heap::{HeapEntry, ObjectHandle, TypedObject};
use crate::lower::{LowerError, Lowering, M3Ids, ModelExtension, Node};
use crate::m3_paths;
use crate::native::NativeRegistry;
use crate::value::{FunctionValue, LambdaClosure, PVector, Value};

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
    /// the closure cache when the host names what the value stands for.
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
        let Some(node) = obj.as_any().downcast_ref::<HostNode>() else {
            return FunctionKind::Other(None);
        };
        let path = node.user_path().or_else(|| node.node_name());
        let Some(classifier) = model.resolve_fqn_str(node.classifier_path()) else {
            return FunctionKind::Other(path);
        };
        let is = |id: Option<ElementId>| id == Some(classifier);
        if is(self.lambda) {
            FunctionKind::Lambda {
                definition: node.definition(),
            }
        } else if is(self.native)
            && let Some(key) = node.node_name()
        {
            FunctionKind::Native(key)
        } else if is(self.defined) {
            FunctionKind::Defined
        } else {
            FunctionKind::Other(path)
        }
    }
}

/// A lambda as lowered once: its parameters and body, which depend on its
/// definition only, the names of the variables it captures, whose
/// values are read again on every call, and the definition's body for
/// the body executor, built on its first application.
struct CachedClosure {
    parameters: std::sync::Arc<[Parameter]>,
    body: std::sync::Arc<[ValueSpec]>,
    open_variables: Vec<SmolStr>,
    exec: Rc<LambdaBody>,
}

impl CachedClosure {
    fn of(closure: &LambdaClosure) -> Self {
        Self {
            parameters: closure.parameters.clone(),
            body: closure.body.clone(),
            open_variables: closure.captures.keys().cloned().collect(),
            exec: Rc::new(LambdaBody::new()),
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
    /// host names its definition).
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
            let exec = cached.exec.clone();
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
                exec: Some(exec),
            });
        }
        let mut closure = self.lowering.lower_root_lambda(value)?;
        if let Some(key) = definition {
            let cached = CachedClosure::of(&closure);
            closure.exec = Some(cached.exec.clone());
            self.added.push((key, cached));
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

/// The model, the registry and the evaluator's state behind one host
/// evaluator, plus the state shared with every host node.
///
/// The model is borrowed by every host node that resolves a description
/// against it and by the evaluator built for each call; it is borrowed
/// mutably only to grow, between the
/// lowering of a call and its run, when neither exists. The evaluator's
/// state (heap, scopes, caches) lives between calls as
/// [`EvaluatorState`] and is rebuilt when the model grows, as a fresh
/// evaluator would be.
pub struct BridgeContext {
    model: Rc<RefCell<PureModel>>,
    registry: NativeRegistry,
    state: EvaluatorState,
    /// Shared with every [`HostNode`].
    shared: Rc<HostShared>,
    /// The chunk the lowering appends to, once the model first grew.
    bridge_chunk: Option<u16>,
    /// The function classifiers, by id.
    kinds: FunctionKinds,
    /// The M3 classifiers every lowering dispatches on, by id: resolved
    /// when the context starts and again whenever the model grows, the
    /// only time an id could change, rather than once per call.
    m3_ids: M3Ids,
    /// Lowered lambdas by definition ([`HostNode::definition`]), for the
    /// context's lifetime: the elements their bodies refer to live in the
    /// model as long.
    closures: HashMap<i64, CachedClosure>,
    /// Whether the evaluator runs built bodies ([`crate::exec`]), fixed
    /// when the context starts.
    exec: bool,
}

impl BridgeContext {
    /// A context over a model and a registry, bound to a host. The
    /// evaluator starts with no scope: every [`start`](Self::start)
    /// pushes and pops its own, as one execution.
    #[must_use]
    pub fn new(model: PureModel, registry: NativeRegistry, host: impl Host) -> Self {
        let kinds = FunctionKinds::resolve(&model);
        let m3_ids = M3Ids::resolve(&model);
        let state = EvaluatorState::new(&model);
        let exec = state.exec();
        let model = Rc::new(RefCell::new(model));
        let shared = Rc::new(HostShared::new(host, Rc::clone(&model)));
        Self {
            model,
            registry,
            state,
            shared,
            bridge_chunk: None,
            kinds,
            m3_ids,
            closures: HashMap::new(),
            exec,
        }
    }

    /// Switch the body executor ([`crate::exec`]) for every later call.
    pub fn set_exec(&mut self, enabled: bool) {
        self.exec = enabled;
        self.state.set_exec(enabled);
    }

    /// The state shared with every host node.
    #[must_use]
    pub fn shared(&self) -> &Rc<HostShared> {
        &self.shared
    }

    /// Phase B: append the lowered elements to the model. The evaluator's
    /// state borrows nothing, but is rebuilt as a fresh evaluator over
    /// the grown model would be.
    fn grow_model(&mut self, extension: ModelExtension) -> Result<(), PureException> {
        let chunk_id = extension.chunk_id();
        let applied = extension.apply(&mut self.model.borrow_mut());
        self.state = EvaluatorState::new(&self.model.borrow());
        self.state.set_exec(self.exec);
        self.m3_ids = M3Ids::resolve(&self.model.borrow());
        applied?;
        self.bridge_chunk = Some(chunk_id);
        Ok(())
    }

    /// Run a function node with already-crossed arguments, one scope per
    /// call: the function and its arguments are lowered (Phase A), the
    /// model grows by what they reference (Phase B), and the call runs.
    /// The execution ends with the host nodes' caches dropped and the
    /// host told which nodes died.
    ///
    /// # Errors
    /// Whatever the lowering or the evaluator raises, or a failure
    /// recorded while a host node was read (which wins: what follows a
    /// failed read is its consequence).
    pub fn start(&mut self, function: &Value, args: &[Value]) -> Result<Value, PureException> {
        let result = self.lower_and_run(function, args);
        self.shared.end_execution();
        if let Some(failure) = self.shared.take_error() {
            return Err(failure);
        }
        result
    }

    fn lower_and_run(&mut self, function: &Value, args: &[Value]) -> Result<Value, PureException> {
        let started = std::time::Instant::now();
        let reads_at_start = self.shared.reads();
        let (callable, args) = self.lower_call(function, args)?;
        let lowered = std::time::Instant::now();
        let reads_before = self.shared.reads();
        let result = {
            let model = self.model.borrow();
            let state = std::mem::take(&mut self.state);
            let mut evaluator = Evaluator::with_state(&model, &self.registry, state);
            evaluator.context_mut().push_scope();
            let result = match &callable {
                Callable::Native(key) => evaluator.call_native(key, &args),
                Callable::Value(value) => evaluator.apply_callable(value, &args),
            };
            evaluator.context_mut().pop_scope();
            self.state = evaluator.into_state();
            result
        };
        let ran = std::time::Instant::now();
        timings::record(Timings {
            lower_ns: u64::try_from((lowered - started).as_nanos()).unwrap_or(u64::MAX),
            run_ns: u64::try_from((ran - lowered).as_nanos()).unwrap_or(u64::MAX),
            run_host_reads: self.shared.reads() - reads_before,
            lower_host_reads: reads_before - reads_at_start,
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
        let (lowered, extension, added) = {
            let model = self.model.borrow();
            let model: &PureModel = &model;
            let chunk = self
                .bridge_chunk
                .unwrap_or_else(|| u16::try_from(model.chunks.len()).expect("chunk ids are u16"));
            let mut lowering = CachedLowering {
                lowering: Lowering::with_ids(model, self.m3_ids, chunk, model.chunk_len(chunk)),
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
                            return Err(boundary_error(
                                "start: the function is not a function node",
                            ));
                        }
                    },
                    other => {
                        return Err(boundary_error(format!(
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
            if let Some(failure) = self.shared.take_error() {
                return Err(failure);
            }
            let lowered = lowered?;
            let CachedLowering {
                lowering, added, ..
            } = lowering;
            (lowered, lowering.into_extension(), added)
        };
        if !extension.is_empty() {
            self.grow_model(extension)?;
        }
        self.closures.extend(added);
        Ok(lowered)
    }

    /// The Rust object behind an integer the host handed back; `None`
    /// once released or never issued.
    #[must_use]
    pub fn resolve_handle(&self, raw: i64) -> Option<ObjectHandle> {
        self.shared.lookup_handle(raw)
    }

    /// Every value of a property of a Rust object held for the host;
    /// empty when the property is unset.
    ///
    /// # Errors
    /// The heap refuses the read, or a host node read on the way failed.
    pub fn property_values(
        &self,
        handle: &ObjectHandle,
        name: &str,
    ) -> Result<PVector<Value>, PureException> {
        let values = self
            .state
            .heap()
            .get_property_values(handle, name)
            .map_err(PureException::from)?;
        if let Some(failure) = self.shared.take_error() {
            return Err(failure);
        }
        Ok(values)
    }

    /// Property names of a Rust object held for the host.
    ///
    /// # Errors
    /// The heap refuses the read.
    pub fn property_names(&self, handle: &ObjectHandle) -> Result<Vec<SmolStr>, PureException> {
        self.state
            .heap()
            .property_names(handle)
            .map_err(PureException::from)
    }

    /// Classifier user path of a Rust object held for the host.
    ///
    /// # Errors
    /// The heap refuses the read.
    pub fn classifier(&self, handle: &ObjectHandle) -> Result<String, PureException> {
        self.state
            .heap()
            .classifier(handle)
            .map(|s| s.to_string())
            .map_err(PureException::from)
    }

    /// Allocate a dynamic object with the given property values and hand
    /// the host its integer.
    ///
    /// # Errors
    /// A property the heap refuses.
    pub fn new_object(
        &mut self,
        classifier: &str,
        properties: &[(String, Vec<Value>)],
    ) -> Result<i64, PureException> {
        let handle = self.state.heap_mut().alloc_dynamic(classifier);
        for (name, values) in properties {
            handle
                .borrow_mut()
                .mutate_set(name, values)
                .map_err(PureException::from)?;
        }
        Ok(self.shared.register_handle(handle))
    }

    /// Drop the strong reference the host held for `raw`. Idempotent.
    pub fn release(&self, raw: i64) {
        self.shared.release_handle(raw);
    }

    /// Live handles in both directions.
    #[must_use]
    pub fn live_handle_count(&self) -> usize {
        self.shared.live_count()
    }
}
