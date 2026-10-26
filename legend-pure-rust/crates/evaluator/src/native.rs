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

//! Native (built-in) Pure functions implemented in Rust.
//!
//! Native functions are Pure functions whose implementation is provided by the
//! runtime rather than by user-written Pure code. Examples include arithmetic
//! operators (`plus`, `minus`), collection operations (`size`, `at`), and
//! assertion primitives (`fail`, `assert`).
//!
//! # Architecture
//!
//! - [`NativeFunction`] — trait that each built-in function implements.
//! - [`NativeRegistry`] — lookup table mapping qualified function names to
//!   their implementations.
//! - [`EvalContextTrait`] — type-erased handle to the evaluator passed to
//!   native functions so they can force argument expressions, invoke
//!   callables, and reach the heap.
//! - [`Evaluated`] — the activator result; wraps [`Value`].
//!
//! # Expression-activator model
//!
//! Natives receive **unevaluated** [`ValueSpec`]s. They call
//! `ctx.evaluate(&spec)` on every argument they actually need, in whatever
//! order matches their semantics. This mirrors Java Pure's
//! `findValueSpecificationExecutor(...).execute(...)` pattern:
//!
//! - Eager primitives (`plus`, `equal`) evaluate every argument up front.
//! - Short-circuit natives (`and`, `or`, `if`) evaluate arguments on demand
//!   and skip unused branches entirely.
//! - Lambda-receiving natives (`map`, `filter`, `fold`) first evaluate the
//!   lambda spec to a `Value::Function`, then invoke it per element via
//!   [`EvalContextTrait::call_function`].

use std::fmt;

use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{ExprKind, ValueSpec};
use rust_decimal::Decimal;
use smol_str::SmolStr;

use crate::context::VariableContext;
use crate::error::{PureException, PureRuntimeError};
use crate::heap::RuntimeHeap;
use crate::value::Value;

/// The result of forcing a Pure `ValueSpec`.
#[derive(Debug, Clone, PartialEq)]
pub struct Evaluated {
    value: Value,
}

impl Evaluated {
    /// Wrap a `Value` as a forced-expression result.
    #[must_use]
    pub fn new(value: Value) -> Self {
        Self { value }
    }

    /// Borrow the underlying `Value`.
    #[must_use]
    pub fn value(&self) -> &Value {
        &self.value
    }

    /// Consume into the bare `Value`.
    #[must_use]
    pub fn into_value(self) -> Value {
        self.value
    }

    /// Interpret as a boolean. Errors if the underlying `Value` is not boolean.
    ///
    /// # Errors
    /// Returns a `PureException` wrapping the `Value::as_boolean` error.
    pub fn as_boolean(&self) -> Result<bool, PureException> {
        self.value.as_boolean().map_err(Into::into)
    }

    /// Interpret as an integer. Errors if the underlying `Value` is not `Integer`.
    ///
    /// # Errors
    /// Returns a `PureException` if the value is not an integer.
    pub fn as_integer(&self) -> Result<i64, PureException> {
        self.value.as_integer().map_err(Into::into)
    }

    /// Interpret as a float, promoting `Integer` automatically.
    ///
    /// # Errors
    /// Returns a `PureException` if the value is neither float nor integer.
    pub fn as_float(&self) -> Result<f64, PureException> {
        self.value.as_float().map_err(Into::into)
    }

    /// Interpret as a string. Errors if the underlying `Value` is not `String`.
    ///
    /// # Errors
    /// Returns a `PureException` if the value is not a string.
    pub fn as_string(&self) -> Result<&SmolStr, PureException> {
        self.value.as_string().map_err(Into::into)
    }

    /// Interpret as a decimal. Errors if the underlying `Value` is not `Decimal`.
    ///
    /// # Errors
    /// Returns a `PureException` if the value is not a decimal.
    pub fn as_decimal(&self) -> Result<Decimal, PureException> {
        self.value.as_decimal().map_err(Into::into)
    }

    /// Flatten into a collection view. Scalar values become 1-element vectors.
    #[must_use]
    pub fn to_collection(&self) -> crate::value::PVector<Value> {
        self.value.to_collection()
    }
}

impl From<Value> for Evaluated {
    fn from(value: Value) -> Self {
        Self { value }
    }
}

/// Type-erased handle to the evaluator, passed to native functions.
///
/// This is the object-safe counterpart of the concrete `EvalContext` struct
/// (defined in [`eval`](crate::eval)). It mirrors Java Pure's expression-
/// activator pattern: the native receives raw `ValueSpec`s and calls
/// [`evaluate`](Self::evaluate) on each one it wants to force.
pub trait EvalContextTrait {
    /// Force a `ValueSpec` to a runtime [`Evaluated`] under the current scope.
    ///
    /// This is the sole entry point natives use to evaluate argument
    /// expressions — the equivalent of Java's
    /// `FunctionExecutionInterpreted.findValueSpecificationExecutor(...).execute(...)`.
    ///
    /// # Errors
    /// Propagates any `PureException` raised while evaluating the expression.
    fn evaluate(&mut self, spec: &ValueSpec) -> Result<Evaluated, PureException>;

    /// Hand every element of `spec`'s value to `each`, in order, without
    /// building the collection: the elements of a `Collection` literal are
    /// evaluated one at a time, any other result is unpacked (`Unit` is
    /// nothing, a scalar is one element).
    ///
    /// # Errors
    /// Propagates any `PureException` raised while evaluating or by `each`.
    fn for_each_value(
        &mut self,
        spec: &ValueSpec,
        each: &mut dyn FnMut(Value) -> Result<(), PureException>,
    ) -> Result<(), PureException> {
        match self.evaluate(spec)?.into_value() {
            Value::Collection(items) => {
                for item in items.iter() {
                    each(item.clone())?;
                }
                Ok(())
            }
            Value::Unit => Ok(()),
            single => each(single),
        }
    }

    /// Access the variable context immutably.
    fn context(&self) -> &VariableContext;

    /// Access the variable context mutably.
    fn context_mut(&mut self) -> &mut VariableContext;

    /// Access the runtime heap immutably.
    fn heap(&self) -> &RuntimeHeap;

    /// Access the runtime heap mutably.
    fn heap_mut(&mut self) -> &mut RuntimeHeap;

    /// Invoke a Pure callable (lambda or compiled function) with already-
    /// forced argument values.
    ///
    /// Use this after forcing a callable expression via [`evaluate`](Self::evaluate):
    /// the native materializes the `Value::Function` first, then invokes it
    /// per-element with the runtime values it wants to bind.
    ///
    /// # Errors
    /// Returns the callee's full [`PureException`] unchanged, so semantic
    /// kinds (`AssertionFailed`, `ConstraintViolation`, …) survive the
    /// native→user-code boundary.
    fn call_function(&mut self, callable: &Value, args: &[Value]) -> Result<Value, PureException>;

    /// Look up a qualified property by `name` on the receiver's class
    /// (walking generalizations) and invoke it with `$this` bound to the
    /// receiver and `args` bound to its parameters.
    ///
    /// Returns `Ok(None)` when the receiver is not a heap object, or when
    /// no QP of that name+arity exists anywhere on the class hierarchy.
    ///
    /// # Errors
    /// Propagates any `PureException` raised while evaluating the QP body.
    fn invoke_qualified_property(
        &mut self,
        receiver: &Value,
        name: &str,
        args: &[Value],
    ) -> Result<Option<Value>, PureException>;

    /// Invoke a *pre-found* qualified property on `receiver`. Pairs with
    /// [`crate::eval::find_qp_with_generalization`].
    ///
    /// `receiver` must be a `Value::Object` whose class is the same one
    /// the `FoundQp` was located on (or one of its generalizations). The
    /// runtime impl panics with a clean error if a non-Object is passed.
    /// `args` is bound positionally onto `found.parameters`; type-variable
    /// parameters are recovered from the receiver's `__typeVariableValues`
    /// heap slot.
    ///
    /// # Errors
    /// Propagates any `PureException` raised while evaluating the QP body.
    fn invoke_qualified_property_found(
        &mut self,
        receiver: &Value,
        found: &crate::eval::FoundQp,
        args: &[Value],
    ) -> Result<Value, PureException>;

    /// Access the compiled Pure model (element lookup, type resolution).
    fn model(&self) -> &PureModel;

    /// Emit console output through the evaluator's configured sink.
    fn console_output(&mut self, msg: &str);
}

/// A native (built-in) Pure function implemented in Rust.
///
/// Each native is a zero-sized struct implementing this trait. The evaluator
/// hands it raw `ValueSpec`s plus an [`EvalContextTrait`] activator; the
/// native decides which arguments to force (and in what order) and returns
/// an [`Evaluated`] result.
///
/// Simple natives (`plus`, `equal`) force every argument up front. Short-
/// circuit natives (`and`, `or`, `if`) force on demand so unused branches
/// stay unevaluated. Lambda-receiving natives (`map`, `filter`, `fold`)
/// force the lambda spec to a `Value::Function`, then invoke it per-element
/// via [`EvalContextTrait::call_function`].
pub trait NativeFunction: fmt::Debug {
    /// How the native takes its arguments. [`ArgMode::Lazy`] unless the
    /// native declares otherwise; the body executor ([`crate::exec`])
    /// reads it once per call site, when it builds a body.
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Lazy
    }

    /// The total operation this native computes, when it is one the body
    /// executor ([`crate::exec`]) may run inline for the representations
    /// of a call's arguments; `None` unless the native declares it. Only
    /// an eager native declares one.
    fn intrinsic(&self) -> Option<Intrinsic> {
        None
    }

    /// Execute the native against its raw `ValueSpec` arguments.
    ///
    /// The native is responsible for calling `ctx.evaluate(&arg)` on every
    /// argument it needs. The evaluator does **not** pre-evaluate anything.
    ///
    /// # Errors
    /// Return a `PureException` for any failure (argument-count mismatch,
    /// type error, propagated sub-expression error). The evaluator wraps
    /// the returned exception with a stack frame before it bubbles up.
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException>;

    /// Execute the native against its arguments already forced, in
    /// order. An eager native ([`ArgMode::Eager`]) defines itself here and
    /// its [`execute`](Self::execute) is [`force_then_values`]; for any
    /// other native this hands the values to `execute` as variables bound
    /// in a scope of their own ([`through_specs`]).
    ///
    /// # Errors
    /// As [`execute`](Self::execute) over expressions with these values.
    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        through_specs(self, args, ctx)
    }
}

/// Run a native over already forced values through its expression
/// entry: each value is bound to a fresh variable in a scope of its own
/// and the native receives the variables.
///
/// # Errors
/// Whatever the native raises.
pub fn through_specs<N: NativeFunction + ?Sized>(
    native: &N,
    args: &[Value],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Evaluated, PureException> {
    ctx.context_mut().push_scope();
    let mut specs = Vec::with_capacity(args.len());
    let placeholder_source = SourceInfo::new("<dispatch>", 0, 0, 0, 0);
    for (i, v) in args.iter().enumerate() {
        let name: SmolStr = format!("__native_arg_{i}").into();
        ctx.context_mut().set(name.clone(), v.clone());
        specs.push(ValueSpec {
            kind: Box::new(ExprKind::Variable { name }),
            source_info: placeholder_source.clone(),
            type_info: None,
        });
    }
    let result = native.execute(&specs, ctx);
    ctx.context_mut().pop_scope();
    result
}

/// The [`execute`](NativeFunction::execute) of an eager native: checks the
/// arity it declares before forcing anything (`Exactly`), forces every
/// argument in order, then runs [`execute_values`](NativeFunction::execute_values).
/// `func_name` names the native in the arity error.
///
/// # Errors
/// The arity is wrong, an argument fails to evaluate, or the native fails.
pub fn force_then_values<N: NativeFunction + ?Sized>(
    native: &N,
    func_name: &str,
    args: &[ValueSpec],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Evaluated, PureException> {
    if let ArgMode::Eager(Arity::Exactly(expected)) = native.arg_mode() {
        arity(func_name, args, expected)?;
    }
    match args {
        [a] => {
            let a = ctx.evaluate(a)?.into_value();
            native.execute_values(std::slice::from_ref(&a), ctx)
        }
        [a, b] => {
            let a = ctx.evaluate(a)?.into_value();
            let b = ctx.evaluate(b)?.into_value();
            native.execute_values(&[a, b], ctx)
        }
        [a, b, c] => {
            let a = ctx.evaluate(a)?.into_value();
            let b = ctx.evaluate(b)?.into_value();
            let c = ctx.evaluate(c)?.into_value();
            native.execute_values(&[a, b, c], ctx)
        }
        _ => {
            let values = force_all(args, ctx)?;
            native.execute_values(&values, ctx)
        }
    }
}

/// The one value of a one-argument eager native, with the arity error
/// its forcing reports.
///
/// # Errors
/// There is not exactly one value.
pub fn value1<'v>(func_name: &str, args: &'v [Value]) -> Result<&'v Value, PureException> {
    match args {
        [a] => Ok(a),
        _ => Err(arity_error(func_name, 1, args.len())),
    }
}

/// The two values of a two-argument eager native, with the arity error
/// its forcing reports.
///
/// # Errors
/// There are not exactly two values.
pub fn values2<'v>(
    func_name: &str,
    args: &'v [Value],
) -> Result<(&'v Value, &'v Value), PureException> {
    match args {
        [a, b] => Ok((a, b)),
        _ => Err(arity_error(func_name, 2, args.len())),
    }
}

/// The three values of a three-argument eager native, with the arity
/// error its forcing reports.
///
/// # Errors
/// There are not exactly three values.
pub fn values3<'v>(
    func_name: &str,
    args: &'v [Value],
) -> Result<(&'v Value, &'v Value, &'v Value), PureException> {
    match args {
        [a, b, c] => Ok((a, b, c)),
        _ => Err(arity_error(func_name, 3, args.len())),
    }
}

fn arity_error(func_name: &str, expected: usize, got: usize) -> PureException {
    PureRuntimeError::EvaluationError(format!(
        "{func_name}: expected {expected} argument(s), got {got}"
    ))
    .into()
}

/// How a native takes its arguments, as the native declares it
/// ([`NativeFunction::arg_mode`]).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ArgMode {
    /// The native receives its argument expressions and forces them
    /// itself, through [`NativeFunction::execute`].
    Lazy,
    /// The native forces every argument, in order, before anything else
    /// and never looks at the expressions: its definition is
    /// [`NativeFunction::execute_values`]. The body executor forces the
    /// arguments of a call whose count the arity accepts and calls that.
    Eager(Arity),
    /// A control form of fixed shape. The body executor runs it as its
    /// own operation instead of calling [`NativeFunction::execute`], so a
    /// native declares one only when its `execute` is exactly what the
    /// form documents.
    Control(Control),
}

/// The argument counts an eager native takes.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Arity {
    /// Exactly this many; another count is an error raised before any
    /// argument is forced.
    Exactly(usize),
    /// Any count; every argument is forced first.
    Any,
}

impl Arity {
    /// Whether a call with `count` arguments forces them all.
    #[must_use]
    pub fn accepts(self, count: usize) -> bool {
        match self {
            Self::Exactly(expected) => count == expected,
            Self::Any => true,
        }
    }
}

/// The control forms the body executor runs as operations. Each states
/// the contract the declaring native's `execute` fulfils, with its own
/// stack frame added to any error, as dispatch adds it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Control {
    /// Two arguments, a name and a value: forces both in order, binds
    /// the value under the name (a `String`) in the current scope and
    /// returns it.
    Let,
    /// Three arguments: forces the first through [`force_thunk`] as a
    /// `Boolean`, then the second when it is `true`, else the third,
    /// through [`force_thunk`], and returns that.
    If,
    /// Two arguments: forces the first as a `Boolean`; `false` without
    /// forcing the second, else the second as a `Boolean`.
    And,
    /// Two arguments: forces the first as a `Boolean`; `true` without
    /// forcing the second, else the second as a `Boolean`.
    Or,
}

/// The total operations a native can declare ([`NativeFunction::intrinsic`]).
/// Each states what the declaring native's `execute_values` returns for
/// the argument values it names; for any other values the native alone
/// decides, and the body executor hands them to it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Intrinsic {
    /// Two arguments: for two `Integer`s, two `Float`s, two `Boolean`s
    /// or two `String`s, whether they are equal as [`Value`]'s `==`
    /// compares them (a `NaN` equals a `NaN`, `-0.0` equals `0.0`).
    Equal,
    /// Two arguments: for two numbers, each an `Integer` or a `Float`,
    /// whether the first orders before the second as
    /// [`compare_values`](comparison::compare_values) orders them (an
    /// `Integer` against a `Float` as an `f64`, `Float`s by `f64::total_cmp`).
    LessThan,
    /// As [`LessThan`](Self::LessThan): whether the first does not order
    /// after the second.
    LessThanEqual,
    /// One argument: for a `Boolean`, its negation.
    Not,
    /// One argument, any value: how many values it holds (none for
    /// `Unit`, a collection's length, one for anything else).
    Size,
    /// One argument, any value: whether it holds none (`Unit` or an
    /// empty collection).
    IsEmpty,
}

/// Registry of native functions, keyed by mangled function FQN.
///
/// The evaluator uses this to dispatch calls to built-in functions.
/// Functions are registered at startup and the registry is immutable
/// during evaluation.
///
/// Keys are mangled function names matching Java's
/// `ConcreteFunctionDefinitionNameProcessor` format:
/// `funcName_ParamType_Mult__ReturnType_Mult_`
pub struct NativeRegistry {
    /// Every function, at the position its name was first registered.
    entries: Vec<Box<dyn NativeFunction>>,
    /// Name to position in `entries`.
    positions: crate::context::NameMap<usize>,
}

impl NativeRegistry {
    /// Create an empty registry.
    #[must_use]
    pub fn new() -> Self {
        Self {
            entries: Vec::new(),
            positions: crate::context::NameMap::default(),
        }
    }

    /// Register a native function under the given name.
    ///
    /// If a function with the same name already exists, it is replaced,
    /// at the same position.
    pub fn register(&mut self, name: impl Into<SmolStr>, func: impl NativeFunction + 'static) {
        let name = name.into();
        match self.positions.get(&name) {
            Some(&position) => self.entries[position] = Box::new(func),
            None => {
                self.positions.insert(name, self.entries.len());
                self.entries.push(Box::new(func));
            }
        }
    }

    /// Look up a native function by name.
    ///
    /// Returns `None` if no function is registered under that name.
    #[must_use]
    pub fn get(&self, name: &str) -> Option<&dyn NativeFunction> {
        self.position(name).map(|position| self.at(position))
    }

    /// The position of a name, stable for the registry's life: a caller
    /// that hashes a name once keeps the position instead.
    #[must_use]
    pub fn position(&self, name: &str) -> Option<usize> {
        self.positions.get(name).copied()
    }

    /// The function at a position [`position`](Self::position) returned.
    ///
    /// # Panics
    /// The position was not returned by this registry.
    #[must_use]
    pub fn at(&self, position: usize) -> &dyn NativeFunction {
        self.entries[position].as_ref()
    }

    /// Whether a call to the native under `key` binds a name in the scope
    /// it runs in: the native declares [`Control::Let`].
    #[must_use]
    pub fn binds(&self, key: &str) -> bool {
        self.get(key)
            .is_some_and(|native| native.arg_mode() == ArgMode::Control(Control::Let))
    }

    /// Look up a native function, returning a `FunctionNotFound` error if missing.
    ///
    /// # Errors
    /// Returns `FunctionNotFound` if the function is not registered.
    pub fn get_or_err(&self, name: &str) -> Result<&dyn NativeFunction, PureRuntimeError> {
        self.get(name)
            .ok_or_else(|| PureRuntimeError::FunctionNotFound(name.into()))
    }

    /// Every registered mangled name, in no particular order.
    pub fn keys(&self) -> impl Iterator<Item = &SmolStr> {
        self.positions.keys()
    }

    /// The number of registered functions.
    #[must_use]
    pub fn len(&self) -> usize {
        self.entries.len()
    }

    /// Whether the registry is empty.
    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.entries.is_empty()
    }

    /// Create a registry holding every native fished so far.
    #[must_use]
    pub fn standard() -> Self {
        let mut registry = Self::new();
        arithmetic::register(&mut registry);
        boolean::register(&mut registry);
        collection::register(&mut registry);
        comparison::register(&mut registry);
        datetime::register(&mut registry);
        lang::register(&mut registry);
        math::register(&mut registry);
        meta::register(&mut registry);
        string::register(&mut registry);
        testing::register(&mut registry);
        registry
    }
}

impl Default for NativeRegistry {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Debug for NativeRegistry {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("NativeRegistry")
            .field("count", &self.entries.len())
            .finish()
    }
}

/// Validate that exactly `n` arguments were provided.
///
/// # Errors
/// Returns a `PureException` wrapping an `EvaluationError` if the count is wrong.
pub fn expect_args<T>(func_name: &str, args: &[T], expected: usize) -> Result<(), PureException> {
    if args.len() != expected {
        return Err(PureRuntimeError::EvaluationError(format!(
            "{func_name}: expected {expected} argument(s), got {}",
            args.len()
        ))
        .into());
    }
    Ok(())
}

fn arity(func_name: &str, args: &[ValueSpec], expected: usize) -> Result<(), PureException> {
    if args.len() != expected {
        return Err(arity_error(func_name, expected, args.len()));
    }
    Ok(())
}

/// Force the one argument of a one-argument native, without a vector.
///
/// # Errors
/// The arity is wrong, or the argument fails to evaluate.
pub fn force1(
    func_name: &str,
    args: &[ValueSpec],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Value, PureException> {
    arity(func_name, args, 1)?;
    Ok(ctx.evaluate(&args[0])?.into_value())
}

/// Force the two arguments of a two-argument native, without a vector.
///
/// # Errors
/// The arity is wrong, or an argument fails to evaluate.
pub fn force2(
    func_name: &str,
    args: &[ValueSpec],
    ctx: &mut dyn EvalContextTrait,
) -> Result<(Value, Value), PureException> {
    arity(func_name, args, 2)?;
    let first = ctx.evaluate(&args[0])?.into_value();
    let second = ctx.evaluate(&args[1])?.into_value();
    Ok((first, second))
}

/// Force the three arguments of a three-argument native, without a vector.
///
/// # Errors
/// The arity is wrong, or an argument fails to evaluate.
pub fn force3(
    func_name: &str,
    args: &[ValueSpec],
    ctx: &mut dyn EvalContextTrait,
) -> Result<(Value, Value, Value), PureException> {
    arity(func_name, args, 3)?;
    let first = ctx.evaluate(&args[0])?.into_value();
    let second = ctx.evaluate(&args[1])?.into_value();
    let third = ctx.evaluate(&args[2])?.into_value();
    Ok((first, second, third))
}

/// Validate that at least `min` arguments were provided.
///
/// # Errors
/// Returns a `PureException` if fewer than `min` arguments are present.
pub fn expect_min_args<T>(func_name: &str, args: &[T], min: usize) -> Result<(), PureException> {
    if args.len() < min {
        return Err(PureRuntimeError::EvaluationError(format!(
            "{func_name}: expected at least {min} argument(s), got {}",
            args.len()
        ))
        .into());
    }
    Ok(())
}

/// Force every argument spec to a concrete [`Value`].
///
/// # Errors
/// Propagates the first `PureException` raised while forcing an argument.
pub fn force_all(
    args: &[ValueSpec],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Vec<Value>, PureException> {
    let mut out = Vec::with_capacity(args.len());
    for a in args {
        out.push(ctx.evaluate(a)?.into_value());
    }
    Ok(out)
}

/// Force a `ValueSpec`, auto-invoking zero-parameter `Lambda` wrappers.
///
/// Pure signatures that use `Function<{->T[m]}>` parameters (e.g. `if`'s
/// branches, `assert`'s message) arrive at the native as zero-parameter
/// `Lambda` specs: the compiler wraps the argument expression in a `|expr`
/// closure so evaluation can be deferred.
///
/// # Errors
///
/// Propagates any `PureException` raised while evaluating the spec or invoking the lambda.
pub fn force_thunk(
    spec: &ValueSpec,
    ctx: &mut dyn EvalContextTrait,
) -> Result<Evaluated, PureException> {
    if let ExprKind::Lambda { parameters, .. } = &*spec.kind
        && parameters.is_empty()
    {
        let lambda_val = ctx.evaluate(spec)?.into_value();
        return Ok(Evaluated::new(ctx.call_function(&lambda_val, &[])?));
    }
    ctx.evaluate(spec)
}

/// Arithmetic natives: `plus`, `minus`, `times`, `divide`, `rem`, `mod`, `abs`.
pub mod arithmetic;

/// Boolean natives: `and`, `or`, `not`.
pub mod boolean;

/// Collection natives (`map`, `filter`, `fold`, `zip`, the `Map` natives, …).
pub mod collection;

/// Comparison natives: `eq`, `equal`, and the ordering helpers.
pub mod comparison;

/// Date and time natives (`adjust`, `dateDiff`, `date`, the `has*` family, …).
pub mod datetime;

/// Heap-aware structural equality behind `equal`.
pub mod equality;

/// Language-level natives: `letFunction`, `if`, `new`, `copy`.
pub mod lang;

/// Mathematical natives (`round`, `floor`, the transcendental family, parsing).
pub mod math;

/// String natives (`toString`, `format`, `joinStrings`, …).
pub mod string;

/// Test-support natives: `assert`.
pub mod testing;

/// A native registered under its mangled key before its implementation
/// is fished: dispatch finds it and it fails with
/// [`PureRuntimeError::NotImplemented`] naming the key, so the PCT
/// manifests record `NotImplemented: <key>` instead of `FunctionNotFound`.
#[derive(Debug)]
pub struct NotYet(pub &'static str);

impl NativeFunction for NotYet {
    fn execute(
        &self,
        _args: &[ValueSpec],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        Err(PureRuntimeError::NotImplemented(SmolStr::new(self.0)).into())
    }
}

/// Reflection natives (`match`, `cast`, `extractEnumValue`, `genericType`,
/// `deactivate`) and the heap shapes for functions and multiplicities.
pub mod meta;

/// Construct a [`ValueSpec`] wrapping an [`IntegerLiteral`](ExprKind::IntegerLiteral).
#[cfg(test)]
pub(crate) fn lit_int(n: i64) -> ValueSpec {
    spec(ExprKind::IntegerLiteral(n))
}

/// Construct a [`ValueSpec`] wrapping a [`FloatLiteral`](ExprKind::FloatLiteral).
#[cfg(test)]
#[allow(dead_code)]
pub(crate) fn lit_float(f: f64) -> ValueSpec {
    spec(ExprKind::FloatLiteral(f))
}

/// Construct a [`ValueSpec`] wrapping a [`DecimalLiteral`](ExprKind::DecimalLiteral).
#[cfg(test)]
#[allow(dead_code)]
pub(crate) fn lit_decimal(d: rust_decimal::Decimal) -> ValueSpec {
    spec(ExprKind::DecimalLiteral(d))
}

/// Construct a [`ValueSpec`] wrapping a [`BooleanLiteral`](ExprKind::BooleanLiteral).
#[cfg(test)]
pub(crate) fn lit_bool(b: bool) -> ValueSpec {
    spec(ExprKind::BooleanLiteral(b))
}

/// Construct a [`ValueSpec`] wrapping a [`StringLiteral`](ExprKind::StringLiteral).
#[cfg(test)]
pub(crate) fn lit_str(s: &str) -> ValueSpec {
    spec(ExprKind::StringLiteral(s.into()))
}

/// Construct a [`ValueSpec`] wrapping a [`Collection`](ExprKind::Collection) literal.
#[cfg(test)]
pub(crate) fn lit_collection(elements: Vec<ValueSpec>) -> ValueSpec {
    spec(ExprKind::Collection { elements })
}

#[cfg(test)]
fn spec(kind: ExprKind) -> ValueSpec {
    ValueSpec {
        kind: Box::new(kind),
        source_info: SourceInfo::new("<test>", 1, 1, 1, 1),
        type_info: None,
    }
}

/// Mock evaluation context for unit-testing native functions.
///
/// [`MockCtx::evaluate`] supports **only** literal `ExprKind` shapes —
/// `IntegerLiteral`, `FloatLiteral`, `DecimalLiteral`, `StringLiteral`,
/// `BooleanLiteral`, and `Collection` of those. Every other variant
/// panics with a pointer to `crates/runtime/tests/eval_tests.rs`, which
/// is the home for any native test that needs real evaluator support
/// (variables, lambda bodies, function calls, heap, model lookups, …).
///
/// All other `EvalContextTrait` methods panic — if your native uses the
/// heap, model, or `call_function`, the test must live in the integration
/// file with a real `Evaluator`.
#[cfg(test)]
pub(crate) struct MockCtx;

#[cfg(test)]
impl EvalContextTrait for MockCtx {
    fn evaluate(&mut self, spec: &ValueSpec) -> Result<Evaluated, PureException> {
        let v = match &*spec.kind {
            ExprKind::IntegerLiteral(n) => Value::Integer(*n),
            ExprKind::FloatLiteral(f) => Value::Float(*f),
            ExprKind::DecimalLiteral(d) => Value::Decimal(*d),
            ExprKind::StringLiteral(s) => Value::String(s.clone()),
            ExprKind::BooleanLiteral(b) => Value::Boolean(*b),
            ExprKind::Collection { elements } => {
                let mut pv = crate::value::PVector::new();
                for e in elements {
                    pv.push_back(self.evaluate(e)?.into_value());
                }
                Value::Collection(Box::new(pv))
            }
            _ => unreachable!(
                "MockCtx::evaluate only supports literal ExprKinds; move this test to \
                 crates/runtime/tests/eval_tests.rs where a real Evaluator is available"
            ),
        };
        Ok(Evaluated::new(v))
    }

    fn context(&self) -> &VariableContext {
        unreachable!("MockCtx::context should never be called in simple native tests")
    }
    fn context_mut(&mut self) -> &mut VariableContext {
        unreachable!("MockCtx::context_mut should never be called in simple native tests")
    }
    fn heap(&self) -> &RuntimeHeap {
        unreachable!("MockCtx::heap should never be called in simple native tests")
    }
    fn heap_mut(&mut self) -> &mut RuntimeHeap {
        unreachable!("MockCtx::heap_mut should never be called in simple native tests")
    }
    fn call_function(&mut self, _c: &Value, _a: &[Value]) -> Result<Value, PureException> {
        unreachable!(
            "MockCtx::call_function should never be called in simple native tests; \
             move this test to eval_tests.rs"
        )
    }
    fn invoke_qualified_property(
        &mut self,
        _receiver: &Value,
        _name: &str,
        _args: &[Value],
    ) -> Result<Option<Value>, PureException> {
        Ok(None)
    }
    fn invoke_qualified_property_found(
        &mut self,
        _receiver: &Value,
        _found: &crate::eval::FoundQp,
        _args: &[Value],
    ) -> Result<Value, PureException> {
        unreachable!(
            "MockCtx::invoke_qualified_property_found should never be called in simple native tests; \
             move this test to eval_tests.rs"
        )
    }
    fn model(&self) -> &PureModel {
        unreachable!("MockCtx::model should never be called in simple native tests")
    }
    fn console_output(&mut self, _msg: &str) {}
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A trivial native function for testing the registry.
    #[derive(Debug)]
    struct ConstantFn(Value);

    impl NativeFunction for ConstantFn {
        fn execute(
            &self,
            _args: &[ValueSpec],
            _ctx: &mut dyn EvalContextTrait,
        ) -> Result<Evaluated, PureException> {
            Ok(Evaluated::new(self.0.clone()))
        }
    }

    #[test]
    fn registry_register_and_get() {
        let mut reg = NativeRegistry::new();
        reg.register("myFunc", ConstantFn(Value::Integer(42)));

        let func = reg.get("myFunc").unwrap();
        let result = func.execute(&[], &mut MockCtx).unwrap();
        assert_eq!(result.into_value(), Value::Integer(42));
    }

    #[test]
    fn not_yet_native_fails_with_not_implemented_naming_the_key() {
        let mut reg = NativeRegistry::new();
        reg.register(
            "deactivate_Any_MANY__ValueSpecification_1_",
            NotYet("deactivate_Any_MANY__ValueSpecification_1_"),
        );
        let err = reg
            .get("deactivate_Any_MANY__ValueSpecification_1_")
            .unwrap()
            .execute(&[], &mut MockCtx)
            .unwrap_err();
        assert!(matches!(
            &err.kind,
            crate::error::PureExceptionKind::ExecutionError(PureRuntimeError::NotImplemented(k))
                if k == "deactivate_Any_MANY__ValueSpecification_1_"
        ));
        assert!(
            reg.get_or_err("deactivate_Any_MANY__ValueSpecification_1_")
                .is_ok()
        );
    }

    #[test]
    fn registry_missing_function() {
        let reg = NativeRegistry::new();
        assert!(reg.get("nope").is_none());
        assert!(reg.get_or_err("nope").is_err());
    }

    #[test]
    fn registry_len() {
        let mut reg = NativeRegistry::new();
        assert!(reg.is_empty());
        reg.register("a", ConstantFn(Value::Unit));
        reg.register("b", ConstantFn(Value::Unit));
        assert_eq!(reg.len(), 2);
    }

    #[test]
    fn expect_args_validates() {
        let args = vec![lit_int(1), lit_int(2)];
        assert!(expect_args("test", &args, 2).is_ok());
        assert!(expect_args("test", &args, 3).is_err());
    }

    #[test]
    fn expect_min_args_validates() {
        let args = vec![lit_int(1)];
        assert!(expect_min_args("test", &args, 1).is_ok());
        assert!(expect_min_args("test", &args, 2).is_err());
    }

    #[test]
    fn mock_ctx_evaluates_literal() {
        let mut ctx = MockCtx;
        assert_eq!(
            ctx.evaluate(&lit_int(7)).unwrap().into_value(),
            Value::Integer(7)
        );
        assert_eq!(
            ctx.evaluate(&lit_bool(true)).unwrap().into_value(),
            Value::Boolean(true)
        );
        assert_eq!(
            ctx.evaluate(&lit_str("hi")).unwrap().into_value(),
            Value::String("hi".into())
        );
    }

    #[test]
    fn mock_ctx_evaluates_collection() {
        let mut ctx = MockCtx;
        let spec = lit_collection(vec![lit_int(1), lit_int(2), lit_int(3)]);
        let v = ctx.evaluate(&spec).unwrap().into_value();
        match &v {
            Value::Collection(pv) => {
                assert_eq!(pv.len(), 3);
                assert_eq!(pv[0], Value::Integer(1));
                assert_eq!(pv[2], Value::Integer(3));
            }
            other => panic!("expected Collection, got {other:?}"),
        }
    }
}
