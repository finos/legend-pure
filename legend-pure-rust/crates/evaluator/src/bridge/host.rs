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

//! A host's node as a first-class evaluator object, and what Rust asks
//! the host about it.
//!
//! The host describes a node once (a [`Description`]) and its property
//! values on demand; nothing is read ahead, nothing is read twice. A node
//! whose user path the Rust model already knows never becomes a node at
//! all: it is unified to `Value::Element` before any allocation, as are
//! primitives (decoded to Rust values) and enum values of enumerations
//! the model knows.

use std::cell::{Cell, Ref, RefCell};
use std::collections::HashMap;
use std::rc::{Rc, Weak};
use std::str::FromStr;

use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::bridge::description::{Description, kind};
use crate::bridge::failure::{boundary_error, is_unanchored, unsupported};
use crate::bridge::handles::HandleTable;
use crate::date::{PureDate, StrictTime, TimePrecision};
use crate::error::{PureException, PureRuntimeError};
use crate::heap::{HeapEntry, ObjectHandle, RuntimeHeap, TypedObject};
use crate::value::{PVector, Value};

/// What Rust asks a host about the nodes it handed in, by the ids their
/// descriptions carry. A transport implements this over its host's graph
/// and nothing else.
///
/// A host keeps a node's id stable for the life of the context, until
/// [`forget`](Self::forget) names it; a failure is returned, never
/// unwound; every method runs on the thread that entered the context.
pub trait Host: 'static {
    /// Every value of the property `name` of node `id`, described;
    /// `None` when the host has no such node or property.
    ///
    /// # Errors
    /// The host could not read the property.
    fn property_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException>;

    /// The values of a variable the lambda node `id` captured from the
    /// scope it was evaluated in; `None` when the node is not such a
    /// closure, `Some(empty)` when it is but holds no variable of that
    /// name.
    ///
    /// # Errors
    /// The host could not read the variable.
    fn captured_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException>;

    /// The property keys of node `id`.
    ///
    /// # Errors
    /// The host could not read the keys.
    fn keys_of(&self, id: i64) -> Result<Vec<SmolStr>, PureException>;

    /// Node ids nothing on the Rust side holds any more: the host may
    /// drop what it kept for them, and never sees them again (describing
    /// the same object later assigns a fresh id).
    fn forget(&self, ids: &[i64]);
}

/// State shared between the context and every host node: the host, the
/// model, the two identity tables, and the first failure raised while a
/// node was read through the heap's infallible object view (where there
/// is no error channel), kept typed.
///
/// **Rule:** never hold `nodes` or `handles` borrowed across a host
/// call. A host only reads its own graph during one, but a re-entrant
/// call into the evaluator would deadlock the borrow.
///
/// The end of an execution costs what the execution did, not what stays
/// alive: it visits the nodes whose property cache the execution filled
/// (`touched`) and the nodes that were dropped since the last end
/// (`dropped`), never the whole `nodes` table. Values a host keeps
/// across calls (a Rust result it holds by handle) keep their host nodes
/// alive, and every later call would otherwise pay for each of them.
pub struct HostShared {
    host: Box<dyn Host>,
    model: Rc<RefCell<PureModel>>,
    /// Host-to-Rust identity: host id → the one heap entry wrapping it.
    nodes: RefCell<HashMap<i64, Weak<RefCell<HeapEntry>>>>,
    /// Rust-to-host identity: Rust objects exposed to the host.
    handles: RefCell<HandleTable>,
    /// The first failure raised while reading a node through the heap's
    /// infallible view, with its kind; surfaced when the call ends.
    last_error: RefCell<Option<PureException>>,
    /// Property and capture reads made through the host so far; the
    /// timings of a `start` report how many fell inside its run phase.
    reads: Cell<u64>,
    /// Ids of host nodes that died since the host was last told, so its
    /// id tables shrink with Rust's.
    released: RefCell<Vec<i64>>,
    /// Ids of host nodes whose property cache was filled since the last
    /// end of execution, each once.
    touched: RefCell<Vec<i64>>,
    /// Ids of host nodes dropped since their entries in `nodes` were last
    /// reaped (see [`HostNode`]'s `Drop`).
    dropped: RefCell<Vec<i64>>,
}

impl HostShared {
    /// The shared state over a host and the model it crosses into.
    #[must_use]
    pub fn new(host: impl Host, model: Rc<RefCell<PureModel>>) -> Self {
        Self {
            host: Box::new(host),
            model,
            nodes: RefCell::new(HashMap::new()),
            handles: RefCell::new(HandleTable::new()),
            last_error: RefCell::new(None),
            reads: Cell::new(0),
            released: RefCell::new(Vec::new()),
            touched: RefCell::new(Vec::new()),
            dropped: RefCell::new(Vec::new()),
        }
    }

    /// The model, borrowed for a read. The context grows the model only
    /// between calls, when no node is being read.
    ///
    /// # Panics
    /// The model is growing.
    #[must_use]
    pub fn model(&self) -> Ref<'_, PureModel> {
        self.model.borrow()
    }

    /// The host.
    #[must_use]
    pub fn host(&self) -> &dyn Host {
        self.host.as_ref()
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

    /// Property and capture reads made through the host so far.
    #[must_use]
    pub fn reads(&self) -> u64 {
        self.reads.get()
    }

    fn count_read(&self) {
        self.reads.set(self.reads.get() + 1);
    }

    /// Register a Rust object for the host and return its integer.
    pub fn register_handle(&self, handle: ObjectHandle) -> i64 {
        HandleTable::to_i64(self.handles.borrow_mut().register(handle))
    }

    /// The Rust object behind an integer the host handed back; `None`
    /// once released or never issued.
    #[must_use]
    pub fn lookup_handle(&self, raw: i64) -> Option<ObjectHandle> {
        self.handles
            .borrow()
            .lookup(HandleTable::from_i64(raw))
            .cloned()
    }

    /// Drop the strong reference the host held for `raw`. Idempotent.
    pub fn release_handle(&self, raw: i64) {
        self.handles
            .borrow_mut()
            .release(HandleTable::from_i64(raw));
    }

    /// Live host nodes plus Rust objects held for the host.
    #[must_use]
    pub fn live_count(&self) -> usize {
        self.forget_dead_nodes();
        self.nodes.borrow().len() + self.handles.borrow().live_count()
    }

    /// End of one execution: drop the property cache of every host node
    /// that filled one (see [`HostNode::clear_cache`]), forget the nodes
    /// nothing holds any more, and tell the host which ids died. Only
    /// nodes that filled a cache hold one, so every live node's cache is
    /// empty afterwards.
    pub fn end_execution(&self) {
        let touched = std::mem::take(&mut *self.touched.borrow_mut());
        let nodes: Vec<ObjectHandle> = {
            let nodes = self.nodes.borrow();
            touched
                .iter()
                .filter_map(|id| nodes.get(id).and_then(Weak::upgrade))
                .collect()
        };
        for handle in &nodes {
            if let HeapEntry::Typed(obj) = &*handle.borrow()
                && let Some(node) = obj.as_any().downcast_ref::<HostNode>()
            {
                node.clear_cache();
            }
        }
        drop(nodes);
        self.forget_dead_nodes();
        let released = std::mem::take(&mut *self.released.borrow_mut());
        if !released.is_empty() {
            self.host.forget(&released);
        }
    }

    /// The end of the context: drop every Rust object the host still
    /// holds, empty the caches and local slots of every host node still
    /// alive, and tell the host which of its nodes died.
    ///
    /// Every host node holds this shared state, and the handle table
    /// holds the Rust objects the host keeps: a Rust object that refers
    /// to a host node closes a cycle (the object, the node, this state,
    /// the table), and so do two nodes whose caches hold each other. The
    /// owner of the state breaks them here when it ends; nothing then
    /// keeps the state, and with it the host, alive past its owner.
    pub fn release_all(&self) {
        let handles = std::mem::take(&mut *self.handles.borrow_mut());
        drop(handles);
        let live: Vec<ObjectHandle> = self
            .nodes
            .borrow()
            .values()
            .filter_map(Weak::upgrade)
            .collect();
        for handle in &live {
            let emptied = match handle.try_borrow_mut() {
                Ok(mut entry) => match &mut *entry {
                    HeapEntry::Typed(obj) => obj
                        .as_any_mut()
                        .downcast_mut::<HostNode>()
                        .map(HostNode::empty),
                    HeapEntry::Dynamic(_) => None,
                },
                Err(_) => None,
            };
            drop(emptied);
        }
        drop(live);
        drop(self.last_error.borrow_mut().take());
        self.end_execution();
    }

    /// Drop the identity entries of the nodes dropped since the last call
    /// and keep their ids for the host to forget. An id the host
    /// described again in between (a new node under the same id, since
    /// the host keeps an id until told to forget it) stays.
    fn forget_dead_nodes(&self) {
        let dropped = std::mem::take(&mut *self.dropped.borrow_mut());
        let mut nodes = self.nodes.borrow_mut();
        let mut released = self.released.borrow_mut();
        for id in dropped {
            if nodes.get(&id).is_some_and(|weak| weak.strong_count() == 0) {
                nodes.remove(&id);
                released.push(id);
            }
        }
    }

    /// A node's cache is about to hold values: its end of execution must
    /// clear it.
    fn touch(&self, id: i64) {
        self.touched.borrow_mut().push(id);
    }

    /// The one heap entry for a host node id, allocating it on first
    /// sight.
    fn canonical_node(self: &Rc<Self>, d: Description) -> ObjectHandle {
        if let Some(existing) = self.nodes.borrow().get(&d.id).and_then(Weak::upgrade) {
            return existing;
        }
        let source = d.source_info();
        let node = HostNode {
            id: d.id,
            classifier: d
                .classifier
                .unwrap_or_else(|| SmolStr::new("meta::pure::metamodel::type::Any")),
            name: d.name,
            user_path: d.user_path,
            keys: RefCell::new(None),
            definition: (d.definition >= 0).then_some(d.definition),
            source,
            cache: RefCell::new(HashMap::new()),
            local: HashMap::new(),
            shared: Rc::clone(self),
        };
        let handle: ObjectHandle = Rc::new(RefCell::new(HeapEntry::Typed(Box::new(node))));
        self.nodes.borrow_mut().insert(d.id, Rc::downgrade(&handle));
        handle
    }
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
        .map_err(|_| boundary_error(format!("bad subsecond digits `{digits}`")))?;
    let count = u8::try_from(trimmed.len()).unwrap_or(9);
    Ok((nanos, count))
}

/// Turn a description into the Rust value it stands for, allocating a
/// host node only when nothing more specific applies.
///
/// # Errors
/// Primitive payloads that do not parse; value kinds the evaluator cannot
/// hold yet (`Byte`); a Rust handle the host no longer holds.
#[allow(clippy::too_many_lines)]
pub fn description_to_value(
    shared: &Rc<HostShared>,
    d: Description,
) -> Result<Value, PureException> {
    let payload = |d: &Description| d.payload.clone().unwrap_or_default();
    match d.kind {
        kind::BOOLEAN => Ok(Value::Boolean(payload(&d) == "true")),
        kind::INTEGER => match &d.payload {
            None => Ok(Value::Integer(d.integer)),
            Some(digits) => digits
                .parse::<i64>()
                .map(Value::Integer)
                .map_err(|_| unsupported("an Integer outside the 64-bit range")),
        },
        kind::FLOAT => Ok(Value::Float(d.real)),
        kind::DECIMAL => rust_decimal::Decimal::from_str(&payload(&d))
            .map(Value::Decimal)
            .map_err(|_| boundary_error(format!("bad Decimal `{}`", payload(&d)))),
        kind::STRING => Ok(Value::String(SmolStr::new(payload(&d)))),
        kind::LATEST_DATE => Ok(Value::Latest),
        kind::DATE => {
            let (year, month, day) = (
                i16::try_from(d.year).map_err(|_| boundary_error("year out of range"))?,
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
                && let Some(enum_id) = shared.model().resolve_fqn_str(classifier)
            {
                return Ok(Value::EnumValue {
                    enum_id,
                    member: name.clone(),
                });
            }
            Ok(Value::Object(shared.canonical_node(d)))
        }
        kind::PACKAGEABLE_ELEMENT => {
            if let Some(path) = &d.user_path {
                let segments: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
                let model = shared.model();
                if let Some(id) = model
                    .resolve_by_fqn(&segments)
                    .or_else(|| model.resolve_by_path(&segments))
                {
                    return Ok(Value::Element(id));
                }
            }
            Ok(Value::Object(shared.canonical_node(d)))
        }
        kind::RUST_OBJECT => {
            let handle = shared
                .lookup_handle(d.id)
                .ok_or_else(|| boundary_error(format!("Stale or unknown handle: {}", d.id)))?;
            Ok(match RuntimeHeap::element_for_object(&handle) {
                Some(eid) => Value::Element(eid),
                None => Value::Object(handle),
            })
        }
        _ => Ok(Value::Object(shared.canonical_node(d))),
    }
}

/// A node of the host's graph living in the evaluator heap.
pub struct HostNode {
    /// The host's id for the node.
    id: i64,
    /// Classifier user path.
    classifier: SmolStr,
    /// The node's own name, when it has one.
    name: Option<SmolStr>,
    /// User path, when the node is a packageable element.
    user_path: Option<SmolStr>,
    /// Property keys reported by the host, read on first use: the
    /// lowering reads properties by name and never asks.
    keys: RefCell<Option<Vec<SmolStr>>>,
    /// The lambda definition this node is one evaluation of, when the
    /// host wraps a lambda per evaluation (see [`Description::definition`]).
    definition: Option<i64>,
    /// Source span reported by the host.
    source: Option<SourceInfo>,
    /// Property values fetched so far.
    cache: RefCell<HashMap<SmolStr, PVector<Value>>>,
    /// `__`-prefixed slots the evaluator writes (type-argument back-fill).
    local: HashMap<SmolStr, PVector<Value>>,
    shared: Rc<HostShared>,
}

impl std::fmt::Debug for HostNode {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("HostNode")
            .field("id", &self.id)
            .field("classifier", &self.classifier)
            .field("name", &self.name)
            .finish_non_exhaustive()
    }
}

impl HostNode {
    /// The host's id for the node.
    #[must_use]
    pub fn id(&self) -> i64 {
        self.id
    }

    /// The lambda definition this node is one evaluation of, when the
    /// host names one.
    #[must_use]
    pub fn definition(&self) -> Option<i64> {
        self.definition
    }

    /// Forget the property values fetched so far. The host's graph has
    /// cycles (a class and its qualified properties, two functions that
    /// call each other), and the cache holds strong handles, so the
    /// caches of the nodes touched by one execution must be dropped when
    /// it ends or those nodes would never be freed.
    pub fn clear_cache(&self) {
        self.cache.borrow_mut().clear();
    }

    /// Take every value the node holds (its cache and its local slots),
    /// for the caller to drop once it no longer borrows the node.
    #[allow(clippy::type_complexity)]
    fn empty(
        &mut self,
    ) -> (
        HashMap<SmolStr, PVector<Value>>,
        HashMap<SmolStr, PVector<Value>>,
    ) {
        (
            std::mem::take(self.cache.get_mut()),
            std::mem::take(&mut self.local),
        )
    }

    /// The node's property keys, read once.
    fn keys(&self) -> Result<Vec<SmolStr>, PureException> {
        if let Some(keys) = &*self.keys.borrow() {
            return Ok(keys.clone());
        }
        self.shared.count_read();
        let keys = self.shared.host().keys_of(self.id)?;
        *self.keys.borrow_mut() = Some(keys.clone());
        Ok(keys)
    }

    /// Keep a read failure, with its kind, for the end of the call: the
    /// heap's object view has no error channel. A failure the boundary
    /// raised itself is anchored at this node, the place it was read from.
    fn note_failure(&self, mut failure: PureException) {
        if is_unanchored(&failure)
            && let Some(source) = &self.source
        {
            failure.source = Some(source.clone());
        }
        self.shared.note_error(failure);
    }

    fn fetch(&self, name: &str) -> Result<PVector<Value>, PureException> {
        self.shared.count_read();
        let described = self.shared.host().property_values(self.id, name)?;
        self.values_of(described.unwrap_or_default())
    }

    /// The values the host described, as the heap holds them.
    fn values_of(&self, described: Vec<Description>) -> Result<PVector<Value>, PureException> {
        let mut values = PVector::new();
        for d in described {
            values.push_back(description_to_value(&self.shared, d)?);
        }
        Ok(values)
    }
}

impl TypedObject for HostNode {
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
                let mut cache = self.cache.borrow_mut();
                if cache.is_empty() {
                    self.shared.touch(self.id);
                }
                cache.insert(SmolStr::new(name), values.clone());
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

    /// The values a lambda captured, read from the host's closure behind
    /// this node; `None` when the node is a plain lambda (or not a lambda
    /// at all).
    fn captured_values(&self, name: &str) -> Option<PVector<Value>> {
        self.shared.count_read();
        let described = match self.shared.host().captured_values(self.id, name) {
            Ok(described) => described?,
            Err(failure) => {
                self.note_failure(failure);
                return None;
            }
        };
        match self.values_of(described) {
            Ok(values) => Some(values),
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
        Err(PureRuntimeError::Unsupported("mutating a host node".into()))
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
        Err(PureRuntimeError::Unsupported("mutating a host node".into()))
    }

    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn as_any_mut(&mut self) -> &mut dyn std::any::Any {
        self
    }
}

impl Drop for HostNode {
    /// The id goes to the shared state, whose next end of execution
    /// forgets it (unless the host described it again meanwhile).
    fn drop(&mut self) {
        self.shared.dropped.borrow_mut().push(self.id);
    }
}

/// Which Rust shape a host node became.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Crossing {
    /// A model element the node was unified with.
    Element,
    /// A value of an enumeration the model knows.
    EnumValue,
    /// A host node read on demand.
    HostNode,
    /// A Rust object the host had been handed earlier.
    RustObject,
    /// A primitive, decoded.
    Primitive,
}

/// Which Rust shape a value that crossed in has.
#[must_use]
pub fn crossing_of(value: &Value) -> Crossing {
    match value {
        Value::Element(_) => Crossing::Element,
        Value::EnumValue { .. } => Crossing::EnumValue,
        Value::Object(handle) => match &*handle.borrow() {
            HeapEntry::Typed(obj) if obj.as_any().is::<HostNode>() => Crossing::HostNode,
            _ => Crossing::RustObject,
        },
        _ => Crossing::Primitive,
    }
}

/// The host id behind a value, when it is a host node.
#[must_use]
pub fn host_node_id(handle: &ObjectHandle) -> Option<i64> {
    match &*handle.borrow() {
        HeapEntry::Typed(obj) => obj.as_any().downcast_ref::<HostNode>().map(HostNode::id),
        HeapEntry::Dynamic(_) => None,
    }
}
