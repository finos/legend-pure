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

//! The body executor: a body's expression tree resolved once into a
//! [`Body`] of indexed [`Node`]s, run without walking the tree again.
//!
//! A [`Body`] is built from a function's body, a lambda definition's body
//! or the statements handed to [`Evaluator::eval`](crate::eval::Evaluator::eval),
//! and cached by what names it: a function by its element (in the
//! evaluator's state), a lambda by its definition, through the
//! [`LambdaBody`] every closure of that definition shares. A closure that
//! names no definition runs on the tree walker.
//!
//! # Slots live in the variable context
//!
//! A body invocation binds its captures, then its parameters, in the
//! [`VariableContext`](crate::context::VariableContext), as the tree
//! walker does, and each statement-level `let` binds when it runs. A
//! slot is the position of its binding relative to the invocation's first
//! one, so a read is an index, while every name stays visible to what
//! looks variables up by name: the tree walker running a
//! [`Op::Fallback`], a lazy native, `getAll`. A body in which a binding
//! could land at a position the builder cannot know (a `let` anywhere but
//! as a statement, or under a computed name) is not built; it runs on the
//! tree walker whole.
//!
//! # Behaviour
//!
//! Same results, errors, messages, source information and stack frames
//! as the tree walker: a node either runs exactly what the tree walker
//! runs for its expression, or is an [`Op::Fallback`] that hands the
//! expression to the tree walker. Evaluators whose hooks observe every
//! expression ([`EvalHooks::OBSERVES_EXPRESSIONS`](crate::hooks::EvalHooks::OBSERVES_EXPRESSIONS))
//! never use it.

mod build;
mod lazy;
mod run;

use std::cell::{Cell, OnceCell};
use std::sync::Arc;
use std::sync::OnceLock;

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{Parameter, ResolvedType, ValueSpec};
use smol_str::SmolStr;

use crate::value::Value;

/// The environment variable that turns the body executor on (`on`,
/// `true`, `1`) or off (`off`, `false`, `0`) for every evaluator whose
/// creator does not choose; read once per process.
pub const ENV_VAR: &str = "LEGEND_PURE_RUST_EXEC";

/// Whether the body executor runs when neither the environment nor the
/// creator of an evaluator chooses.
pub const DEFAULT_ENABLED: bool = false;

thread_local! {
    static SCOPED_DEFAULT: Cell<Option<bool>> = const { Cell::new(None) };
}

/// Whether an evaluator created now uses the body executor: the
/// innermost [`with_default`] on this thread, else [`ENV_VAR`], else
/// [`DEFAULT_ENABLED`].
#[must_use]
pub fn default_enabled() -> bool {
    if let Some(scoped) = SCOPED_DEFAULT.with(Cell::get) {
        return scoped;
    }
    static FROM_ENV: OnceLock<Option<bool>> = OnceLock::new();
    FROM_ENV
        .get_or_init(|| match std::env::var(ENV_VAR).ok().as_deref() {
            Some("on" | "true" | "1") => Some(true),
            Some("off" | "false" | "0") => Some(false),
            _ => None,
        })
        .unwrap_or(DEFAULT_ENABLED)
}

/// Run `f` with [`default_enabled`] answering `enabled` on this thread,
/// restoring the previous answer when `f` returns or unwinds.
pub fn with_default<R>(enabled: bool, f: impl FnOnce() -> R) -> R {
    struct Restore(Option<bool>);
    impl Drop for Restore {
        fn drop(&mut self) {
            SCOPED_DEFAULT.with(|scoped| scoped.set(self.0));
        }
    }
    let _restore = Restore(SCOPED_DEFAULT.with(|scoped| scoped.replace(Some(enabled))));
    f()
}

/// A node's index in its [`Body`].
pub type NodeIdx = u32;

/// A slot: the position of a binding relative to the first binding of
/// the body invocation that made it.
pub type Slot = u32;

/// What binds a slot.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SlotKind {
    /// A variable the lambda captured, bound from the closure.
    Capture,
    /// A parameter, bound from the call's arguments.
    Parameter,
    /// A statement-level `let`, bound when it runs.
    Let,
}

/// One slot of a body.
#[derive(Debug, Clone)]
pub struct SlotInfo {
    /// The name the binding carries in the variable context.
    pub name: SmolStr,
    /// What binds it.
    pub kind: SlotKind,
}

/// What a body was built from, which decides what its invocation binds
/// before its first statement.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BodyKind {
    /// A function's body: its parameters, in a scope of its own.
    Function,
    /// A lambda definition's body: its captures, then its parameters, in
    /// a scope of its own.
    Lambda,
    /// Statements run in the caller's current scope, binding nothing
    /// first: a `let` among them stays bound after they ran.
    Statements,
}

/// A body resolved once: its slots, its nodes and its statements.
#[derive(Debug)]
pub struct Body {
    kind: BodyKind,
    slots: Vec<SlotInfo>,
    captures: Vec<Slot>,
    parameters: Vec<Slot>,
    names: Vec<(SmolStr, Slot)>,
    nodes: Vec<Node>,
    statements: Vec<NodeIdx>,
    spans: Vec<SourceInfo>,
    fallbacks: usize,
}

impl Body {
    /// What the body was built from.
    #[must_use]
    pub fn kind(&self) -> BodyKind {
        self.kind
    }

    /// Every slot, by slot number.
    #[must_use]
    pub fn slots(&self) -> &[SlotInfo] {
        &self.slots
    }

    /// The capture slots, in the order an invocation binds them.
    #[must_use]
    pub fn captures(&self) -> &[Slot] {
        &self.captures
    }

    /// The parameter slots, in declaration order.
    #[must_use]
    pub fn parameters(&self) -> &[Slot] {
        &self.parameters
    }

    /// Every binder in the order the body binds it, with its slot: what a
    /// question about the variables in scope is answered from.
    #[must_use]
    pub fn names(&self) -> &[(SmolStr, Slot)] {
        &self.names
    }

    /// Every node.
    #[must_use]
    pub fn nodes(&self) -> &[Node] {
        &self.nodes
    }

    /// The statements, in order: the body's value is the last one's.
    #[must_use]
    pub fn statements(&self) -> &[NodeIdx] {
        &self.statements
    }

    /// The source information of a node.
    #[must_use]
    pub fn span(&self, node: NodeIdx) -> &SourceInfo {
        &self.spans[self.nodes[node as usize].span as usize]
    }

    /// The number of [`Op::Fallback`] nodes, the expressions that run on
    /// the tree walker.
    #[must_use]
    pub fn fallbacks(&self) -> usize {
        self.fallbacks
    }

    fn node(&self, idx: NodeIdx) -> &Node {
        &self.nodes[idx as usize]
    }

    fn slot_name(&self, slot: Slot) -> &SmolStr {
        &self.slots[slot as usize].name
    }
}

/// One node: an operation, the static type the expression carried, and
/// its source information.
#[derive(Debug)]
pub struct Node {
    /// What the node does.
    pub op: Op,
    /// The type and multiplicity the expression carried, copied as is.
    pub ty: Option<Box<ResolvedType>>,
    span: u32,
}

/// A native's position in the registry the body was built against.
pub type NativePos = usize;

/// What a node does. Child nodes are indices into the same [`Body`].
#[derive(Debug)]
pub enum Op {
    /// A literal, an enumeration value, a type or an element reference.
    Const(Value),
    /// The value bound in a slot.
    Local(Slot),
    /// A statement-level `let`: forces the value, binds it in the slot
    /// and returns it.
    Let {
        /// The slot it binds.
        slot: Slot,
        /// The value expression.
        value: NodeIdx,
        /// The native's frame name.
        call: SmolStr,
    },
    /// `if` (see [`Control::If`](crate::native::Control::If)).
    If {
        /// The condition.
        cond: Thunk,
        /// Forced when the condition is `true`.
        then: Thunk,
        /// Forced when the condition is `false`.
        els: Thunk,
        /// The native's frame name.
        call: SmolStr,
    },
    /// `and` (see [`Control::And`](crate::native::Control::And)).
    And {
        /// The left operand.
        lhs: NodeIdx,
        /// The right operand, forced when the left one is `true`.
        rhs: NodeIdx,
        /// The native's frame name.
        call: SmolStr,
    },
    /// `or` (see [`Control::Or`](crate::native::Control::Or)).
    Or {
        /// The left operand.
        lhs: NodeIdx,
        /// The right operand, forced when the left one is `false`.
        rhs: NodeIdx,
        /// The native's frame name.
        call: SmolStr,
    },
    /// A call to a function with a Pure body, its arguments forced in
    /// order first.
    CallPure {
        /// The function.
        target: ElementId,
        /// The name the call carries, for its frame.
        name: SmolStr,
        /// The arguments.
        args: Vec<NodeIdx>,
    },
    /// A native that forces its own arguments: it receives the argument
    /// expressions, and forcing one of them runs that argument's node.
    CallLazyNative {
        /// The native.
        native: NativePos,
        /// The native's frame name.
        call: SmolStr,
        /// The argument expressions the native receives.
        specs: Vec<ValueSpec>,
        /// The argument nodes, one per expression.
        args: Vec<NodeIdx>,
    },
    /// A property read on the receiver's value.
    Property {
        /// The receiver.
        receiver: NodeIdx,
        /// The property's name.
        name: SmolStr,
    },
    /// A lambda expression: a closure over the variables it reads.
    Lambda(Box<LambdaOp>),
    /// A collection literal, flattened as the tree walker flattens it.
    Collection(Vec<NodeIdx>),
    /// The expression, run on the tree walker.
    Fallback(ValueSpec),
}

/// An argument of `if`, forced as [`force_thunk`](crate::native::force_thunk)
/// forces it.
#[derive(Debug, Clone, Copy)]
pub enum Thunk {
    /// Any expression but a lambda without parameters: its value.
    Value(NodeIdx),
    /// A lambda without parameters (an [`Op::Lambda`] node): applied to
    /// nothing.
    Lambda(NodeIdx),
}

/// A lambda expression inside a body.
#[derive(Debug)]
pub struct LambdaOp {
    /// The lambda's parameters.
    pub parameters: Arc<[Parameter]>,
    /// The lambda's body expressions.
    pub body: Arc<[ValueSpec]>,
    /// The variables a closure of it captures, as the expression lists
    /// them.
    pub free: Arc<[SmolStr]>,
    /// For each of `free`, the enclosing body's slot holding it where the
    /// name is bound there, `None` where it is looked up by name.
    pub sources: Vec<Option<Slot>>,
    /// The lambda's own body, shared by every closure made here.
    pub compiled: std::rc::Rc<LambdaBody>,
}

/// The body of one lambda definition, built on its first application
/// and shared by every closure of the definition.
#[derive(Default)]
pub struct LambdaBody {
    body: OnceCell<Option<Body>>,
}

impl LambdaBody {
    /// A definition whose body is not built yet.
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// The built body, `None` before the first application or when the
    /// body cannot be built.
    #[must_use]
    pub fn built(&self) -> Option<&Body> {
        self.body.get().and_then(Option::as_ref)
    }

    pub(crate) fn get_or_build(&self, build: impl FnOnce() -> Option<Body>) -> Option<&Body> {
        self.body.get_or_init(build).as_ref()
    }
}

impl std::fmt::Debug for LambdaBody {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self.body.get() {
            None => f.write_str("LambdaBody(unbuilt)"),
            Some(None) => f.write_str("LambdaBody(tree walker)"),
            Some(Some(body)) => write!(f, "LambdaBody({} nodes)", body.nodes.len()),
        }
    }
}
