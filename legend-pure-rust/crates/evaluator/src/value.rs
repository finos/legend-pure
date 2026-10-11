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

//! Runtime values — the core `Value` enum for Pure expression evaluation.
//!
//! Every Pure expression evaluates to a `Value`. Primitive types (`Integer`,
//! `Float`, `String`, `Boolean`, `Decimal`) are stored inline with zero
//! allocation. `Decimal` uses `rust_decimal::Decimal` for native arithmetic.
//! Date/time values use `jiff`-backed [`PureDate`] and
//! [`StrictTime`] for native calendar arithmetic.
//!
//! Collections use persistent data structures (`im_rc::Vector`, `im_rc::HashMap`)
//! for structural sharing.
//!
//! Object references use `ObjectId` handles into the [`RuntimeHeap`](super::heap::RuntimeHeap),
//! providing identity-preserving semantics for `mutateAdd`.

use std::cell::RefCell;
use std::collections::HashMap;
use std::fmt;
use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_metamodel::ids::ElementId;
use rust_decimal::Decimal;
use smol_str::SmolStr;

use crate::date::{PureDate, StrictTime};
use crate::heap::ObjectHandle;

/// Backing state for a [`Value::Map`].
///
/// Two distinct `MapState` values with the same
/// entries are equal (see [`Value::eq`]) — identity is for sharing, not
/// for equality.
#[derive(Debug, Clone, Default)]
pub struct MapState {
    /// HAMT-backed entries.
    pub entries: im_rc::HashMap<ValueKey, Value>,
    /// Number of times `getIfAbsentPutWithKey` evaluated its lambda
    /// because the key was absent. Surfaced to Pure code by the
    /// `getMapStats` native via a `MapStats` heap object.
    pub get_if_absent_counter: i64,
}

/// A runtime value produced by evaluating a Pure expression.
#[derive(Debug)]
pub enum Value {
    /// Pure `Boolean` — stored inline.
    Boolean(bool),

    /// Pure `Integer` — stored inline as `i64`.
    Integer(i64),

    /// Pure `Float` — stored inline as `f64`.
    Float(f64),

    /// Pure `Decimal` — fixed-point decimal.
    Decimal(Decimal),

    /// Pure `String` — inline for short strings via `SmolStr`.
    String(SmolStr),

    /// Pure `Date`, `StrictDate`, `DateTime` — variable-precision temporal value.
    ///
    /// All datetime values are
    /// stored as UTC. Timezone conversion is done at format-time only.
    Date(PureDate),

    /// The `%latest` milestoning sentinel — distinct from every concrete
    /// `Date` value. Used in milestoning dispatch to mean "the latest
    /// available version" (i.e. disable the date filter on the relevant
    /// axis). In `compare_values` ordering it ranks greater than every
    /// concrete `Date`. In equality (`Value == Value` / `values_equal`)
    /// it is reflexive — `%latest == %latest` — and unequal to any
    /// concrete date.
    Latest,

    /// Pure `StrictTime` — time of day without date.
    ///
    /// Uses `jiff::civil::Time` (`Copy`, nanosecond precision).
    StrictTime(StrictTime),

    /// A reference to a runtime object.
    ///
    /// `Rc<RefCell<HeapEntry>>` clones share the same underlying entry —
    /// identity is `Rc::ptr_eq`, freed when the last clone drops.
    Object(ObjectHandle),

    /// An ordered collection of values.
    Collection(Box<PVector<Value>>),

    /// A Pure `Map<K, V>` — entries plus stats live behind a shared,
    /// interior-mutable cell so that mutating natives like
    /// `getIfAbsentPutWithKey` are observable through every binding that
    /// references the same `Value::Map`.
    ///
    /// Pure semantics for `put` / `putAll` / `replaceAll` still produce
    /// fresh map identities; only `getIfAbsentPutWithKey` mutates the shared
    /// cell in place.
    Map(Rc<RefCell<MapState>>),

    /// A Pure `Function` value — anonymous lambda or compiled function reference.
    ///
    /// Both `LambdaFunction` and `ConcreteFunctionDefinition` (or
    /// `NativeFunctionDefinition`) are `Function` in Pure's type system.
    /// Callers use `Value::Function` uniformly; dispatch strategy is determined
    /// internally by the `FunctionValue` variant.
    Function(Box<FunctionValue>),

    /// A reference to a compiled model element (Package, Class, Function, etc.).
    ///
    /// Produced by [`ExprKind::PackageableElementRef`](legend_pure_metamodel::types::ExprKind::PackageableElementRef)
    /// and consumed by meta-model natives (`pathToElement`, `elementToPath`,
    /// `match` on `PackageableElement` subtypes). Carries an [`ElementId`] —
    /// a lightweight `Copy` handle into the [`PureModel`](legend_pure_metamodel::model::PureModel).
    Element(ElementId),

    /// A Pure enumeration value (e.g. `MyEnum.CITY`).
    ///
    /// Carries the owning enumeration's [`ElementId`] directly so
    /// `type()` / `instanceOf` / `genericType` resolve the enum element
    /// in O(1) without reverse-engineering a string. Equality is
    /// structural: two `EnumValue`s are equal iff their `enum_id` and
    /// `member` both match — so two enums with the same simple name in
    /// different packages never collide.
    EnumValue {
        /// The owning [`Enumeration`](legend_pure_metamodel::model::Element::Enumeration) element.
        enum_id: ElementId,
        /// The value's member name (e.g. `"CITY"`).
        member: SmolStr,
    },

    /// A numeric value tagged with a unit of measurement.
    ///
    /// Produced by `newUnit(unit, n)` or the compiled form of the
    /// `5 RomanLength~Pes` literal (which `lower_unit_instance` desugars
    /// to a `newUnit` call). `unit_id` points at the `Unit` element
    /// (child of a `Measure`); `inner` is the numeric payload as a
    /// plain `Value::Integer` / `Value::Float` / `Value::Decimal`.
    ///
    /// Two `UnitInstance`s are equal iff they share the same `unit_id`
    /// AND their inner values compare equal — so `5 Pes` and `5 Cubitum`
    /// are distinct even when their inner numbers match, matching
    /// Java Pure's unit-aware equality.
    UnitInstance {
        /// The owning [`Unit`](legend_pure_metamodel::model::Element::Unit) element.
        unit_id: ElementId,
        /// The numeric payload — always one of `Integer`, `Float`, or `Decimal`.
        inner: Box<Value>,
    },

    /// The unit value — result of expressions with no meaningful return.
    /// Equivalent to `[]` with multiplicity `[0..0]`.
    Unit,
}

/// A Pure `Function` value — anonymous lambda or compiled function reference.
///
/// In Pure's type system both share `Function<{Params->Result}>`.
/// The variant drives dispatch in `Evaluator::eval_function_value`.
#[derive(Debug, Clone)]
pub enum FunctionValue {
    /// Anonymous (inline) lambda: parameters, body expressions, and captured bindings.
    ///
    /// Mirrors Java's `LambdaFunction`.
    Lambda(LambdaClosure),

    /// Reference to a named compiled Pure function (native or concrete).
    ///
    /// The `ElementId` uniquely identifies the specific overload resolved by
    /// the compiler.
    Compiled(ElementId),

    /// A navigation-path closure: `#/Type/p1/p2(args)/p3!alias#`.
    ///
    /// Path is `Function<{U[1]→V[m]}>` in Pure's type system, so it dispatches
    /// through the same `apply_callable` entry point as `Lambda` /
    /// `Compiled`. When invoked, the runtime walks each step applying the
    /// property name to the running value (property access for plain steps,
    /// QP call when parameters are present).
    ///
    /// Captures hold any free variables referenced from step parameters
    /// (e.g. `#/Person/nameWith($prefix)#` captures `$prefix` when the path
    /// is constructed inside a scope binding it).
    Path(PathClosure),
}

/// A closure form of a navigation path expression — captures the
/// resolved start type, the step list, and any variables referenced
/// from step parameters at construction time. The runtime walks each
/// step on invocation; type-arg substitution through the chain happens
/// dynamically off the running receiver's heap object.
#[derive(Debug, Clone)]
pub struct PathClosure {
    /// Resolved start type — drives the first step's property lookup.
    pub start_type: legend_pure_metamodel::types::TypeExpr,
    /// One entry per `/property[(args)]` segment, in source order.
    pub steps: Rc<[legend_pure_metamodel::types::PathStepLowered]>,
    /// Optional alias suffix (`!alias`).
    pub name: Option<SmolStr>,
    /// Variables captured from the enclosing scope at the point of
    /// path construction.
    pub captures: HashMap<SmolStr, Value>,
}

/// An anonymous lambda closure: parameters, body, and captured variable bindings.
///
/// Mirrors Java's `LambdaFunction` + captured `VariableContext`.
/// The `captures` map snapshots the enclosing scope at the point of
/// lambda creation for lexical scoping.
#[derive(Debug, Clone)]
pub struct LambdaClosure {
    /// Parameter declarations from the lambda syntax.
    pub parameters: std::sync::Arc<[legend_pure_metamodel::types::Parameter]>,
    /// The lambda body expressions.
    pub body: std::sync::Arc<[legend_pure_metamodel::types::ValueSpec]>,
    /// Captured variable bindings from the enclosing scope.
    pub captures: HashMap<SmolStr, Value>,
}

/// A hashable key for `Map` entries.
///
/// Only value types that are meaningfully comparable can be map keys.
/// Objects default to identity (`Rc::ptr_eq`); Classes that annotate
/// properties with the `<<equality.Key>>` stereotype opt into
/// value-based equality via `ObjectByEqualityKeys`.
#[derive(Debug, Clone)]
pub enum ValueKey {
    /// Boolean key.
    Boolean(bool),
    /// Integer key.
    Integer(i64),
    /// Decimal key.
    Decimal(Decimal),
    /// String key.
    String(SmolStr),
    /// Date key.
    Date(PureDate),
    /// `StrictTime` key.
    StrictTime(StrictTime),
    /// Object identity key — `Rc::ptr_eq` identity. The map key holds
    /// a strong clone of the handle, keeping the underlying entry alive
    /// for as long as the entry is in the map.
    Object(ObjectHandle),
    /// Value-based object key for classes that annotate one or more
    /// properties with `<<equality.Key>>`. Two instances hash / compare
    /// equal iff every annotated field's key agrees. `class_id` is
    /// included so instances of different classes never collide, even
    /// when their annotated fields coincidentally match.
    ObjectByEqualityKeys {
        /// Resolved element ID of the owning Class.
        class_id: ElementId,
        /// Ordered `(property_name, key)` pairs for every
        /// `<<equality.Key>>`-annotated property on the class.
        fields: Vec<(SmolStr, ValueKey)>,
    },
    /// Enum value key — structural `(enum_id, member)` matching
    /// `Value::EnumValue`'s own equality.
    EnumValue {
        /// Enumeration element ID.
        enum_id: ElementId,
        /// Member name.
        member: SmolStr,
    },
}

impl PartialEq for ValueKey {
    fn eq(&self, other: &Self) -> bool {
        match (self, other) {
            (Self::Boolean(a), Self::Boolean(b)) => a == b,
            (Self::Integer(a), Self::Integer(b)) => a == b,
            (Self::Decimal(a), Self::Decimal(b)) => a == b,
            (Self::String(a), Self::String(b)) => a == b,
            (Self::Date(a), Self::Date(b)) => a == b,
            (Self::StrictTime(a), Self::StrictTime(b)) => a == b,
            (Self::Object(a), Self::Object(b)) => Rc::ptr_eq(a, b),
            (
                Self::ObjectByEqualityKeys {
                    class_id: c1,
                    fields: f1,
                },
                Self::ObjectByEqualityKeys {
                    class_id: c2,
                    fields: f2,
                },
            ) => c1 == c2 && f1 == f2,
            (
                Self::EnumValue {
                    enum_id: e1,
                    member: m1,
                },
                Self::EnumValue {
                    enum_id: e2,
                    member: m2,
                },
            ) => e1 == e2 && m1 == m2,
            _ => false,
        }
    }
}

impl Eq for ValueKey {}

impl std::hash::Hash for ValueKey {
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        std::mem::discriminant(self).hash(state);
        match self {
            Self::Boolean(b) => b.hash(state),
            Self::Integer(i) => i.hash(state),
            Self::Decimal(d) => d.hash(state),
            Self::String(s) => s.hash(state),
            Self::Date(d) => d.hash(state),
            Self::StrictTime(t) => t.hash(state),
            Self::Object(handle) => Rc::as_ptr(handle).hash(state),
            Self::ObjectByEqualityKeys { class_id, fields } => {
                class_id.hash(state);
                fields.hash(state);
            }
            Self::EnumValue { enum_id, member } => {
                enum_id.hash(state);
                member.hash(state);
            }
        }
    }
}

impl PartialEq for Value {
    #[allow(clippy::match_same_arms)]
    fn eq(&self, other: &Self) -> bool {
        match (self, other) {
            (Self::Boolean(a), Self::Boolean(b)) => a == b,
            (Self::Integer(a), Self::Integer(b)) => a == b,
            (Self::Float(a), Self::Float(b)) => a == b || (a.is_nan() && b.is_nan()),
            (Self::Decimal(a), Self::Decimal(b)) => a == b,
            (Self::String(a), Self::String(b)) => a == b,
            (Self::Date(a), Self::Date(b)) => a == b,
            (Self::StrictTime(a), Self::StrictTime(b)) => a == b,
            (Self::Object(a), Self::Object(b)) => Rc::ptr_eq(a, b),
            (Self::Collection(a), Self::Collection(b)) => a == b,
            (Self::Map(a), Self::Map(b)) => {
                Rc::ptr_eq(a, b) || a.borrow().entries == b.borrow().entries
            }
            (Self::Element(a), Self::Element(b)) => a == b,
            (
                Self::EnumValue {
                    enum_id: e1,
                    member: m1,
                    ..
                },
                Self::EnumValue {
                    enum_id: e2,
                    member: m2,
                    ..
                },
            ) => e1 == e2 && m1 == m2,
            (
                Self::UnitInstance {
                    unit_id: u1,
                    inner: i1,
                },
                Self::UnitInstance {
                    unit_id: u2,
                    inner: i2,
                },
            ) => u1 == u2 && i1 == i2,
            (Self::Latest, Self::Latest) => true,
            (Self::Unit, Self::Unit) => true,
            _ => false,
        }
    }
}

impl Eq for Value {}

impl Value {
    /// Extract a boolean, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `Boolean`.
    pub fn as_boolean(&self) -> Result<bool, crate::error::PureRuntimeError> {
        match self {
            Self::Boolean(b) => Ok(*b),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Boolean", other,
            )),
        }
    }

    /// Extract an integer, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not an `Integer`.
    pub fn as_integer(&self) -> Result<i64, crate::error::PureRuntimeError> {
        match self {
            Self::Integer(i) => Ok(*i),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Integer", other,
            )),
        }
    }

    /// Extract a float, or return a type error.
    ///
    /// Integers are auto-promoted to float.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `Float` or `Integer`.
    #[allow(clippy::cast_precision_loss)]
    pub fn as_float(&self) -> Result<f64, crate::error::PureRuntimeError> {
        match self {
            Self::Float(f) => Ok(*f),
            Self::Integer(i) => Ok(*i as f64),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Float", other,
            )),
        }
    }

    /// Extract a string reference, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `String`.
    pub fn as_string(&self) -> Result<&SmolStr, crate::error::PureRuntimeError> {
        match self {
            Self::String(s) => Ok(s),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "String", other,
            )),
        }
    }

    /// Extract an object handle (cloning the Rc), or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not an `Object`.
    pub fn as_object(&self) -> Result<ObjectHandle, crate::error::PureRuntimeError> {
        match self {
            Self::Object(handle) => Ok(handle.clone()),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Object", other,
            )),
        }
    }

    /// Extract a collection reference, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `Collection`.
    pub fn as_collection(&self) -> Result<&PVector<Value>, crate::error::PureRuntimeError> {
        match self {
            Self::Collection(v) => Ok(v),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Collection",
                other,
            )),
        }
    }

    /// Extract a decimal, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `Decimal`.
    pub fn as_decimal(&self) -> Result<Decimal, crate::error::PureRuntimeError> {
        match self {
            Self::Decimal(d) => Ok(*d),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "Decimal", other,
            )),
        }
    }

    /// Extract a date, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `Date`.
    pub fn as_date(&self) -> Result<PureDate, crate::error::PureRuntimeError> {
        match self {
            Self::Date(d) => Ok(*d),
            other => Err(crate::error::PureRuntimeError::type_mismatch("Date", other)),
        }
    }

    /// Extract a strict time, or return a type error.
    ///
    /// # Errors
    /// Returns `TypeMismatch` if this value is not a `StrictTime`.
    pub fn as_strict_time(&self) -> Result<StrictTime, crate::error::PureRuntimeError> {
        match self {
            Self::StrictTime(t) => Ok(*t),
            other => Err(crate::error::PureRuntimeError::type_mismatch(
                "StrictTime",
                other,
            )),
        }
    }

    /// Returns a human-readable type name for error messages.
    #[must_use]
    pub fn type_name(&self) -> &'static str {
        match self {
            Self::Boolean(_) => "Boolean",
            Self::Integer(_) => "Integer",
            Self::Float(_) => "Float",
            Self::Decimal(_) => "Decimal",
            Self::String(_) => "String",
            Self::Date(_) => "Date",
            Self::Latest => "Date",
            Self::StrictTime(_) => "StrictTime",
            Self::Object(_) => "Object",
            Self::Collection(_) => "Collection",
            Self::Map(_) => "Map",
            Self::Function(_) => "Function",
            Self::Element(_) => "PackageableElement",
            Self::EnumValue { .. } => "EnumValue",
            Self::UnitInstance { .. } => "UnitInstance",
            Self::Unit => "Unit",
        }
    }
}

impl Value {
    /// Coerce a value to a scalar (for functions expecting `[1]`).
    ///
    /// - A scalar value returns itself.
    /// - A 1-element collection returns the inner value.
    /// - Unit or multi-element collections produce a multiplicity error.
    ///
    /// # Errors
    /// Returns `MultiplicityViolation` if the value is `Unit` (empty) or
    /// a collection with length != 1.
    pub fn to_one(&self) -> Result<&Value, crate::error::PureRuntimeError> {
        match self {
            Self::Collection(v) if v.len() == 1 => Ok(&v[0]),
            Self::Collection(v) => Err(crate::error::PureRuntimeError::MultiplicityViolation {
                expected: "[1]".into(),
                actual: v.len(),
            }),
            Self::Unit => Err(crate::error::PureRuntimeError::MultiplicityViolation {
                expected: "[1]".into(),
                actual: 0,
            }),
            other => Ok(other),
        }
    }

    /// Coerce a value to an optional (for functions expecting `[0..1]`).
    ///
    /// - `Unit` or empty collection → `Ok(None)`
    /// - Scalar or 1-element collection → `Ok(Some(&inner))`
    /// - Multi-element collection → multiplicity error
    ///
    /// # Errors
    /// Returns `MultiplicityViolation` if the value is a collection with
    /// more than one element.
    pub fn to_zero_one(&self) -> Result<Option<&Value>, crate::error::PureRuntimeError> {
        match self {
            Self::Unit => Ok(None),
            Self::Collection(v) if v.is_empty() => Ok(None),
            Self::Collection(v) if v.len() == 1 => Ok(Some(&v[0])),
            Self::Collection(v) => Err(crate::error::PureRuntimeError::MultiplicityViolation {
                expected: "[0..1]".into(),
                actual: v.len(),
            }),
            other => Ok(Some(other)),
        }
    }

    /// Coerce a value to a collection (for functions expecting `[*]`).
    ///
    /// - A `Collection` returns a clone (O(1) via structural sharing).
    /// - `Unit` returns an empty `PVector`.
    /// - Any scalar becomes a 1-element `PVector`.
    #[must_use]
    pub fn to_collection(&self) -> PVector<Value> {
        match self {
            Self::Collection(v) => (**v).clone(),
            Self::Unit => PVector::new(),
            other => {
                let mut v = PVector::new();
                v.push_back(other.clone());
                v
            }
        }
    }

    /// Whether this value is "empty" — `Unit` or an empty collection.
    #[must_use]
    pub fn is_empty(&self) -> bool {
        match self {
            Self::Unit => true,
            Self::Collection(v) => v.is_empty(),
            _ => false,
        }
    }

    /// Create a value from a `Vec`, normalizing multiplicity:
    ///
    /// - Empty → `Unit`
    /// - Single → the scalar value directly
    /// - Multiple → `Collection`
    ///
    /// This matches Pure's multiplicity semantics where `[0]` = empty,
    /// `[1]` = scalar, and `[*]` = collection.
    #[must_use]
    pub fn from_vec(mut values: Vec<Value>) -> Value {
        match values.len() {
            0 => Value::Unit,
            1 => values.pop().unwrap_or(Value::Unit),
            _ => Value::Collection(Box::new(PVector::from_iter(values))),
        }
    }

    /// Project this value to its `ObjectId` view, if any.
    ///
    /// Bridges the `Element` / `Object` / `Function(Compiled)` split
    /// onto the unified metamodel-heap addressing introduced by
    /// [`crate::heap::RuntimeHeap::bootstrap_metamodel`]. Returns:
    /// - `Value::Object(oid)` → `Some(oid)`
    /// - `Value::Element(eid)` → the bootstrapped metamodel row, or `None`
    ///   when no row exists (pre-bootstrap state in tests, primitives
    ///   without an M3 metatype, etc.)
    /// - `Value::Function(Compiled(eid))` → same projection as `Element`
    /// - everything else → `None`
    #[must_use]
    pub fn as_object_handle(&self, heap: &crate::heap::RuntimeHeap) -> Option<ObjectHandle> {
        match self {
            Self::Object(handle) => Some(handle.clone()),
            Self::Element(eid) => heap.object_for_element(*eid),
            Self::Function(fv) => match fv.as_ref() {
                FunctionValue::Compiled(eid) => heap.object_for_element(*eid),
                FunctionValue::Lambda(_) | FunctionValue::Path(_) => None,
            },
            _ => None,
        }
    }

    /// Project this value to its `ElementId` view, if any.
    #[must_use]
    pub fn as_element_id(
        &self,
        _heap: &crate::heap::RuntimeHeap,
    ) -> Option<legend_pure_metamodel::ids::ElementId> {
        match self {
            Self::Element(eid) => Some(*eid),
            Self::Function(fv) => match fv.as_ref() {
                FunctionValue::Compiled(eid) => Some(*eid),
                FunctionValue::Lambda(_) | FunctionValue::Path(_) => None,
            },
            Self::Object(handle) => crate::heap::RuntimeHeap::element_for_object(handle),
            _ => None,
        }
    }
}

impl Clone for Value {
    /// Iterative clone for `Value`, scaling with heap allocations
    /// instead of Rust thread-stack frames.
    fn clone(&self) -> Self {
        deep_clone_iter(self)
    }
}

/// Bottom-up iterative clone of an arbitrarily nested `Value`.
#[allow(clippy::expect_used)]
fn deep_clone_iter(root: &Value) -> Value {
    enum Op<'a> {
        Visit(&'a Value),
        AssembleCollection(usize),
        AssembleUnitInstance(ElementId),
    }

    let mut ops: Vec<Op<'_>> = vec![Op::Visit(root)];
    let mut built: Vec<Value> = Vec::new();

    while let Some(op) = ops.pop() {
        match op {
            Op::Visit(v) => match v {
                Value::Collection(boxed) => {
                    let pvec: &PVector<Value> = boxed;
                    ops.push(Op::AssembleCollection(pvec.len()));
                    for child in pvec.iter().rev() {
                        ops.push(Op::Visit(child));
                    }
                }
                Value::UnitInstance { unit_id, inner } => {
                    ops.push(Op::AssembleUnitInstance(*unit_id));
                    ops.push(Op::Visit(inner));
                }
                other => built.push(clone_leaf(other)),
            },
            Op::AssembleCollection(len) => {
                let start = built.len() - len;
                let items: PVector<Value> = built.drain(start..).collect();
                built.push(Value::Collection(Box::new(items)));
            }
            Op::AssembleUnitInstance(unit_id) => {
                let inner = built
                    .pop()
                    .expect("deep_clone_iter: AssembleUnitInstance reached with empty built stack");
                built.push(Value::UnitInstance {
                    unit_id,
                    inner: Box::new(inner),
                });
            }
        }
    }

    built
        .pop()
        .expect("deep_clone_iter: built stack empty after traversal")
}

/// Clone every non-recursive `Value` variant. Recursive variants
/// (`Collection`, `UnitInstance`) are not handled here — they go
/// through `deep_clone_iter`'s work-stack instead and never reach
/// this function.
fn clone_leaf(v: &Value) -> Value {
    match v {
        Value::Boolean(b) => Value::Boolean(*b),
        Value::Integer(i) => Value::Integer(*i),
        Value::Float(f) => Value::Float(*f),
        Value::Decimal(d) => Value::Decimal(*d),
        Value::String(s) => Value::String(s.clone()),
        Value::Date(d) => Value::Date(*d),
        Value::Latest => Value::Latest,
        Value::StrictTime(t) => Value::StrictTime(*t),
        Value::Object(h) => Value::Object(h.clone()),
        Value::Map(m) => Value::Map(m.clone()),
        Value::Function(fv) => Value::Function(fv.clone()),
        Value::Element(e) => Value::Element(*e),
        Value::EnumValue { enum_id, member } => Value::EnumValue {
            enum_id: *enum_id,
            member: member.clone(),
        },
        Value::Unit => Value::Unit,
        Value::Collection(_) | Value::UnitInstance { .. } => {
            unreachable!("deep_clone_iter must not delegate recursive variants to clone_leaf")
        }
    }
}

impl Drop for Value {
    /// Iterative drop for `Value`, scaling with heap allocations instead
    /// of Rust thread-stack frames.
    fn drop(&mut self) {
        let mut stack: Vec<Value> = Vec::new();
        match self {
            Value::Collection(boxed) => {
                let pvec = std::mem::take(&mut **boxed);
                stack.extend(pvec);
            }
            Value::UnitInstance { inner, .. } => {
                let inner_val = std::mem::replace(&mut **inner, Value::Unit);
                stack.push(inner_val);
            }
            _ => {}
        }

        while let Some(mut v) = stack.pop() {
            match &mut v {
                Value::Collection(boxed) => {
                    let pvec = std::mem::take(&mut **boxed);
                    stack.extend(pvec);
                }
                Value::UnitInstance { inner, .. } => {
                    let inner_val = std::mem::replace(&mut **inner, Value::Unit);
                    stack.push(inner_val);
                }
                _ => {}
            }
        }
    }
}

impl fmt::Display for Value {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Boolean(b) => write!(f, "{b}"),
            Self::Integer(i) => write!(f, "{i}"),
            Self::Float(v) => write!(f, "{v}"),
            Self::Decimal(d) => write!(f, "{d}"),
            Self::String(s) => write!(f, "'{s}'"),
            Self::Date(d) => write!(f, "%{d}"),
            Self::Latest => write!(f, "%latest"),
            Self::StrictTime(t) => write!(f, "%{t}"),
            Self::Object(handle) => write!(
                f,
                "<Object@{:p}:{}>",
                Rc::as_ptr(handle),
                handle.borrow().classifier_str()
            ),
            Self::Collection(v) => {
                write!(f, "[")?;
                for (i, item) in v.iter().enumerate() {
                    if i > 0 {
                        write!(f, ", ")?;
                    }
                    write!(f, "{item}")?;
                }
                write!(f, "]")
            }
            Self::Map(m) => write!(f, "<Map size={}>", m.borrow().entries.len()),
            Self::Function(fv) => match fv.as_ref() {
                FunctionValue::Lambda(_) => write!(f, "<Lambda>"),
                FunctionValue::Compiled(id) => write!(f, "<Function:{id}>"),
                FunctionValue::Path(p) => {
                    write!(f, "<Path:{}", p.steps.len())?;
                    if let Some(name) = &p.name {
                        write!(f, "!{name}")?;
                    }
                    write!(f, ">")
                }
            },
            Self::Element(id) => write!(f, "<Element:{id}>"),
            Self::EnumValue { member, .. } => write!(f, "{member}"),
            Self::UnitInstance { unit_id, inner } => write!(f, "{inner} <Unit:{unit_id}>"),
            Self::Unit => write!(f, "[]"),
        }
    }
}

#[cfg(test)]
mod tests {
    use std::str::FromStr;

    use super::*;

    #[test]
    fn value_integer_roundtrip() {
        let v = Value::Integer(42);
        assert_eq!(v.as_integer().unwrap(), 42);
        assert!(v.as_string().is_err());
    }

    #[test]
    fn value_float_auto_promote() {
        let v = Value::Integer(42);
        assert!((v.as_float().unwrap() - 42.0).abs() < f64::EPSILON);
    }

    #[test]
    fn value_string_smolstr() {
        let v = Value::String(SmolStr::new("hello"));
        assert_eq!(v.as_string().unwrap().as_str(), "hello");
    }

    #[test]
    fn value_decimal_arithmetic() {
        let a = Decimal::from_str("10.50").unwrap();
        let b = Decimal::from_str("3.25").unwrap();
        let v = Value::Decimal(a + b);
        assert_eq!(v.as_decimal().unwrap(), Decimal::from_str("13.75").unwrap());
        assert_eq!(v.to_string(), "13.75");
    }

    #[test]
    fn value_decimal_precision() {
        let a = Decimal::from_str("0.1").unwrap();
        let b = Decimal::from_str("0.2").unwrap();
        let result = a + b;
        assert_eq!(result, Decimal::from_str("0.3").unwrap());
    }

    #[test]
    fn value_date_strict() {
        let d = PureDate::strict_date(2024, 3, 15).unwrap();
        let v = Value::Date(d);
        assert_eq!(v.to_string(), "%2024-03-15");
        assert_eq!(v.as_date().unwrap().get_year(), 2024);
    }

    #[test]
    fn value_date_arithmetic() {
        let d = PureDate::strict_date(2024, 3, 15).unwrap();
        let d2 = d.add_days(10).unwrap();
        assert_eq!(Value::Date(d2).to_string(), "%2024-03-25");
    }

    #[test]
    fn value_strict_time() {
        let t = StrictTime::new(10, 30, 45, 0).unwrap();
        let v = Value::StrictTime(t);
        assert_eq!(v.to_string(), "%10:30:45");
    }

    #[test]
    fn value_collection_persistent() {
        let v1 = PVector::<Value>::new();
        let v2 = v1.clone() + PVector::unit(Value::Integer(1));
        let v3 = v2.clone() + PVector::unit(Value::Integer(2));

        assert_eq!(v1.len(), 0);
        assert_eq!(v2.len(), 1);
        assert_eq!(v3.len(), 2);
    }

    #[test]
    fn value_map_persistent() {
        let m1 = im_rc::HashMap::<ValueKey, Value>::new();
        let m2 = m1.update(ValueKey::String("a".into()), Value::Integer(1));
        let m3 = m2.update(ValueKey::String("b".into()), Value::Integer(2));

        assert_eq!(m1.len(), 0);
        assert_eq!(m2.len(), 1);
        assert_eq!(m3.len(), 2);
    }

    #[test]
    fn value_display() {
        assert_eq!(Value::Integer(42).to_string(), "42");
        assert_eq!(Value::String("hi".into()).to_string(), "'hi'");
        assert_eq!(Value::Unit.to_string(), "[]");
        assert_eq!(Value::Boolean(true).to_string(), "true");
    }

    #[test]
    fn value_equality() {
        assert_eq!(Value::Integer(1), Value::Integer(1));
        assert_ne!(Value::Integer(1), Value::Integer(2));
        assert_ne!(Value::Integer(1), Value::String("1".into()));
    }

    #[test]
    fn value_decimal_equality() {
        let a = Value::Decimal(Decimal::from_str("42.00").unwrap());
        let b = Value::Decimal(Decimal::from_str("42.00").unwrap());
        assert_eq!(a, b);
    }

    #[test]
    fn value_date_equality() {
        let a = Value::Date(PureDate::strict_date(2024, 3, 15).unwrap());
        let b = Value::Date(PureDate::strict_date(2024, 3, 15).unwrap());
        assert_eq!(a, b);
    }

    #[test]
    fn to_one_scalar() {
        let v = Value::Integer(42);
        assert_eq!(*v.to_one().unwrap(), Value::Integer(42));
    }

    #[test]
    fn to_one_single_collection() {
        let mut pv = PVector::new();
        pv.push_back(Value::Integer(42));
        let v = Value::Collection(Box::new(pv));
        assert_eq!(*v.to_one().unwrap(), Value::Integer(42));
    }

    #[test]
    fn to_one_empty_collection_errors() {
        let v = Value::Collection(Box::new(PVector::new()));
        assert!(v.to_one().is_err());
    }

    #[test]
    fn to_one_multi_collection_errors() {
        let mut pv = PVector::new();
        pv.push_back(Value::Integer(1));
        pv.push_back(Value::Integer(2));
        let v = Value::Collection(Box::new(pv));
        assert!(v.to_one().is_err());
    }

    #[test]
    fn to_one_unit_errors() {
        assert!(Value::Unit.to_one().is_err());
    }

    #[test]
    fn to_zero_one_scalar() {
        let v = Value::Integer(42);
        assert_eq!(*v.to_zero_one().unwrap().unwrap(), Value::Integer(42));
    }

    #[test]
    fn to_zero_one_unit_is_none() {
        assert!(Value::Unit.to_zero_one().unwrap().is_none());
    }

    #[test]
    fn to_zero_one_empty_collection_is_none() {
        let v = Value::Collection(Box::new(PVector::new()));
        assert!(v.to_zero_one().unwrap().is_none());
    }

    #[test]
    fn to_zero_one_single_collection() {
        let mut pv = PVector::new();
        pv.push_back(Value::String("hi".into()));
        let v = Value::Collection(Box::new(pv));
        assert_eq!(
            *v.to_zero_one().unwrap().unwrap(),
            Value::String("hi".into())
        );
    }

    #[test]
    fn to_zero_one_multi_collection_errors() {
        let mut pv = PVector::new();
        pv.push_back(Value::Integer(1));
        pv.push_back(Value::Integer(2));
        let v = Value::Collection(Box::new(pv));
        assert!(v.to_zero_one().is_err());
    }

    #[test]
    fn to_collection_from_scalar() {
        let v = Value::Integer(42);
        let c = v.to_collection();
        assert_eq!(c.len(), 1);
        assert_eq!(c[0], Value::Integer(42));
    }

    #[test]
    fn to_collection_from_unit() {
        let c = Value::Unit.to_collection();
        assert!(c.is_empty());
    }

    #[test]
    fn to_collection_from_collection() {
        let mut pv = PVector::new();
        pv.push_back(Value::Integer(1));
        pv.push_back(Value::Integer(2));
        let v = Value::Collection(Box::new(pv.clone()));
        let c = v.to_collection();
        assert_eq!(c, pv);
    }

    #[test]
    fn is_empty_unit() {
        assert!(Value::Unit.is_empty());
    }

    #[test]
    fn is_empty_empty_collection() {
        assert!(Value::Collection(Box::new(PVector::new())).is_empty());
    }

    #[test]
    fn is_empty_scalar_is_false() {
        assert!(!Value::Integer(42).is_empty());
    }

    #[test]
    fn is_empty_nonempty_collection_is_false() {
        let mut pv = PVector::new();
        pv.push_back(Value::Integer(1));
        assert!(!Value::Collection(Box::new(pv)).is_empty());
    }

    /// Build `Collection(Collection(... Integer(0) ...))` `depth` levels
    /// deep. Returns the outermost value.
    fn deep_nested_collection(depth: usize) -> Value {
        let mut v = Value::Integer(0);
        for _ in 0..depth {
            let mut inner = PVector::new();
            inner.push_back(v);
            v = Value::Collection(Box::new(inner));
        }
        v
    }

    /// Locks the iterative `Value::Drop` contract: constructing a 10 000-
    /// level-deep `Collection(Collection(...))` chain and letting it go
    /// out of scope must not overflow the thread stack.
    #[test]
    fn deep_nested_collection_drops_without_overflow() {
        let v = deep_nested_collection(10_000);
        drop(v);
    }

    #[test]
    fn deep_nested_collection_clones_without_overflow() {
        let v = deep_nested_collection(10_000);
        let _ = v.clone();
        drop(v);
    }
}
