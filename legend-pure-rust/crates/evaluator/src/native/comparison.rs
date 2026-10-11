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

//! Comparison natives: `eq` (identity), `equal` (structural, via
//! [`crate::native::equality`]), the ordering natives `lessThan` and
//! `lessThanEqual`, `compare`, and the total comparator
//! [`compare_values`](crate::native::comparison::compare_values) that the ordering natives and the collection
//! natives (`sort`, `removeDuplicates`) share. The platform's `greaterThan`
//! and `greaterThanEqual` are Pure-bodied over `lessThan`/`lessThanEqual`,
//! so they have no native here.

use std::cmp::Ordering;

use legend_pure_metamodel::types::ValueSpec;
use rust_decimal::Decimal;
use rust_decimal::prelude::FromPrimitive;

use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all, force2,
};
use crate::value::Value;

/// Compare two values for the ordering natives (`lessThan`, `lessThanEqual`,
/// `greaterThan`, `greaterThanEqual`).
///
/// Any two numeric values — including mixed `Number` subtypes
/// (`Integer`/`Float`/`Decimal`) — promote toward the wider type and compare,
/// matching Java Pure's `lessThan`/`compare` (e.g. `0.5d <= 2.5`).
/// `String`/`String` compares lexicographically. Genuinely
/// non-comparable pairs (e.g. an `Object` vs a number) still error.
fn numeric_cmp(a: &Value, b: &Value) -> Result<std::cmp::Ordering, PureRuntimeError> {
    let is_numeric =
        |v: &Value| matches!(v, Value::Integer(_) | Value::Float(_) | Value::Decimal(_));
    match (a, b) {
        _ if is_numeric(a) && is_numeric(b) => Ok(compare_values(a, b).cmp(&0)),
        (Value::String(a), Value::String(b)) => Ok(a.cmp(b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "Cannot compare {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
}

/// Pure `eq(Any[1], Any[1]): Boolean[1]` — identity / primitive equality.
///
/// For primitive values: compares by value.
/// For objects: compares identity (`ObjectHandle == ObjectHandle`), not properties.
/// This is NOT the same as `equal` — two objects with identical properties
/// are `equal` but not `eq`.
#[derive(Debug)]
pub struct Eq;

impl NativeFunction for Eq {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("eq", args, ctx)?;
        Ok(Evaluated::new(Value::Boolean(a0 == a1)))
    }
}

/// Pure `equal(Any[*], Any[*]): Boolean[1]` — structural / value equality.
///
/// For primitives: compares by value (same as `eq`).
/// For collections: element-wise equality, recursing via `values_equal`.
/// For heap objects: if the classifier declares any `<<equality.Key>>`
/// property, compares those property values recursively; otherwise
/// falls back to `ObjectHandle` identity. See [`crate::native::equality`].
#[derive(Debug)]
pub struct Equal;

impl NativeFunction for Equal {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("equal", &values, 2)?;
        let result = crate::native::equality::values_equal(ctx, &values[0], &values[1]);
        Ok(Evaluated::new(Value::Boolean(result)))
    }
}

/// Pure `lessThan(Number[1], Number[1]): Boolean[1]`
#[derive(Debug)]
pub struct LessThan;

impl NativeFunction for LessThan {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("lessThan", args, ctx)?;
        Ok(Evaluated::new(Value::Boolean(
            numeric_cmp(&a0, &a1)?.is_lt(),
        )))
    }
}

/// Pure `lessThanEqual(Number[1], Number[1]): Boolean[1]`
#[derive(Debug)]
pub struct LessThanEqual;

impl NativeFunction for LessThanEqual {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("lessThanEqual", args, ctx)?;
        Ok(Evaluated::new(Value::Boolean(
            !numeric_cmp(&a0, &a1)?.is_gt(),
        )))
    }
}

/// Total comparator behind `compare` and the ordering natives.
///
/// Mixed numeric types promote toward the wider type:
/// - Integer ↔ Float: convert Integer to f64 and `partial_cmp`
/// - Integer ↔ Decimal: convert Integer to Decimal and compare exactly
/// - Float ↔ Decimal: convert Decimal to f64 and `partial_cmp`
///
/// Cross-type comparisons (Integer ↔ String, Boolean ↔ Date, etc.)
/// fall through to a stable per-variant ordinal so the contract stays
/// total — `compare(5, '5')` returns a non-zero value with consistent
/// direction (`compare(a, b) < 0` ⇒ `compare(b, a) > 0`).
#[must_use]
#[allow(clippy::cast_precision_loss)]
pub fn compare_values(a: &Value, b: &Value) -> i64 {
    let ord = match (a, b) {
        (Value::Integer(a), Value::Integer(b)) => a.cmp(b),
        (Value::Integer(a), Value::Float(b)) => (*a as f64).total_cmp(b),
        (Value::Float(a), Value::Integer(b)) => a.total_cmp(&(*b as f64)),
        (Value::Float(a), Value::Float(b)) => a.total_cmp(b),
        (Value::Decimal(a), Value::Decimal(b)) => a.cmp(b),
        (Value::Integer(a), Value::Decimal(b)) => Decimal::from(*a).cmp(b),
        (Value::Decimal(a), Value::Integer(b)) => a.cmp(&Decimal::from(*b)),
        (Value::Decimal(a), Value::Float(b)) => {
            Decimal::from_f64(*b).map_or(Ordering::Equal, |bd| a.cmp(&bd))
        }
        (Value::Float(a), Value::Decimal(b)) => {
            Decimal::from_f64(*a).map_or(Ordering::Equal, |ad| ad.cmp(b))
        }
        (Value::Boolean(a), Value::Boolean(b)) => a.cmp(b),
        (Value::String(a), Value::String(b)) => a.cmp(b),
        (Value::Date(a), Value::Date(b)) => a.cmp(b),
        (Value::Latest, Value::Latest) => Ordering::Equal,
        (Value::Latest, Value::Date(_)) => Ordering::Greater,
        (Value::Date(_), Value::Latest) => Ordering::Less,
        (Value::StrictTime(a), Value::StrictTime(b)) => a.cmp(b),
        (Value::Object(a), Value::Object(b)) => {
            let av = std::rc::Rc::as_ptr(a) as usize;
            let bv = std::rc::Rc::as_ptr(b) as usize;
            av.cmp(&bv)
        }
        (Value::Element(a), Value::Element(b)) => format!("{a}").cmp(&format!("{b}")),
        (
            Value::EnumValue {
                enum_id: e1,
                member: m1,
                ..
            },
            Value::EnumValue {
                enum_id: e2,
                member: m2,
                ..
            },
        ) if e1 == e2 => m1.cmp(m2),
        _ => type_ordinal(a).cmp(&type_ordinal(b)),
    };
    match ord {
        Ordering::Less => -1,
        Ordering::Equal => 0,
        Ordering::Greater => 1,
    }
}

/// Stable per-variant ordinal for cross-type comparisons. Ordering is
/// arbitrary but fixed — `compare(5, '5')` consistently routes one way
/// (here: Integer < String) and the reverse call routes the other.
fn type_ordinal(v: &Value) -> u8 {
    match v {
        Value::Boolean(_) => 0,
        Value::Integer(_) => 1,
        Value::Float(_) => 2,
        Value::Decimal(_) => 3,
        Value::String(_) => 4,
        Value::Date(_) | Value::Latest => 5,
        Value::StrictTime(_) => 6,
        Value::EnumValue { .. } => 7,
        Value::UnitInstance { .. } => 8,
        Value::Element(_) => 9,
        Value::Object(_) => 10,
        Value::Collection(_) => 11,
        Value::Map(_) => 12,
        Value::Function(_) => 13,
        Value::Unit => 14,
    }
}

/// Pure `compare<T>(a:T[1], b:T[1]): Integer[1]` — total comparator.
///
/// Returns -1 / 0 / 1; the parametric `T` is honored only as a hint —
/// the runtime treats the args as `Any` and dispatches via
/// [`compare_values`].
#[derive(Debug)]
pub struct Compare;

impl NativeFunction for Compare {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("compare", &values, 2)?;
        Ok(Evaluated::new(Value::Integer(compare_values(
            &values[0], &values[1],
        ))))
    }
}

/// Register the fished comparison natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("eq_Any_1__Any_1__Boolean_1_", Eq);
    registry.register("equal_Any_MANY__Any_MANY__Boolean_1_", Equal);
    registry.register("lessThan_Number_1__Number_1__Boolean_1_", LessThan);
    registry.register(
        "lessThanEqual_Number_1__Number_1__Boolean_1_",
        LessThanEqual,
    );
    registry.register("compare_T_1__T_1__Integer_1_", Compare);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_decimal, lit_float, lit_int, lit_str};

    #[test]
    fn less_than_integers() {
        assert_eq!(
            LessThan
                .execute(&[lit_int(1), lit_int(2)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
        assert_eq!(
            LessThan
                .execute(&[lit_int(2), lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn less_than_mixed_numeric() {
        assert_eq!(
            LessThan
                .execute(&[lit_int(1), lit_float(1.5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn less_than_equal_decimal_vs_float_promotes() {
        assert_eq!(
            LessThanEqual
                .execute(
                    &[lit_decimal(Decimal::new(5, 1)), lit_float(2.5)],
                    &mut MockCtx
                )
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
        assert_eq!(
            LessThanEqual
                .execute(
                    &[lit_float(2.5), lit_decimal(Decimal::new(5, 1))],
                    &mut MockCtx
                )
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn string_comparison() {
        assert_eq!(
            LessThan
                .execute(&[lit_str("a"), lit_str("b")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn wrong_arg_count_errors() {
        assert!(Equal.execute(&[lit_int(1)], &mut MockCtx).is_err());
        assert!(LessThan.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn invalid_type_comparisons() {
        assert!(
            LessThan
                .execute(&[lit_int(1), lit_str("foo")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn compare_integers() {
        assert_eq!(compare_values(&Value::Integer(1), &Value::Integer(2)), -1);
        assert_eq!(compare_values(&Value::Integer(2), &Value::Integer(2)), 0);
        assert_eq!(compare_values(&Value::Integer(2), &Value::Integer(1)), 1);
    }

    #[test]
    fn compare_floats() {
        assert_eq!(compare_values(&Value::Float(1.3), &Value::Float(2.1)), -1);
        assert_eq!(compare_values(&Value::Float(2.2), &Value::Float(2.2)), 0);
        assert_eq!(compare_values(&Value::Float(2.1), &Value::Float(1.0)), 1);
    }

    #[test]
    fn compare_mixed_numeric_int_and_float() {
        assert_eq!(compare_values(&Value::Integer(1), &Value::Float(2.2)), -1);
        assert_eq!(compare_values(&Value::Integer(2), &Value::Float(1.5)), 1);
        assert_eq!(compare_values(&Value::Float(2.7), &Value::Integer(171)), -1);
    }

    #[test]
    fn compare_native_returns_integer() {
        let r = Compare
            .execute(&[lit_int(1), lit_int(5)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, Value::Integer(-1));
    }

    #[test]
    fn compare_wrong_arg_count_errors() {
        assert!(Compare.execute(&[lit_int(1)], &mut MockCtx).is_err());
        assert!(Compare.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn compare_decimal_and_integer_exactly_at_the_i64_limit() {
        let d = Decimal::from(9_999_999_999_999_992_i64);
        assert_eq!(
            compare_values(&Value::Decimal(d), &Value::Integer(9_999_999_999_999_992)),
            0
        );
        assert_eq!(
            compare_values(&Value::Decimal(d), &Value::Integer(9_999_999_999_999_991)),
            1
        );
        assert_eq!(
            compare_values(&Value::Decimal(d), &Value::Integer(9_999_999_999_999_993)),
            -1
        );
    }

    #[test]
    fn compare_strings_lexicographic() {
        assert_eq!(
            compare_values(&Value::String("a".into()), &Value::String("b".into())),
            -1
        );
        assert_eq!(
            compare_values(&Value::String("aaa".into()), &Value::String("aaa".into())),
            0
        );
        assert_eq!(
            compare_values(&Value::String("baa".into()), &Value::String("aaa".into())),
            1
        );
        assert_eq!(
            compare_values(&Value::String("aa".into()), &Value::String("aaa".into())),
            -1
        );
    }

    #[test]
    fn compare_booleans_false_lt_true() {
        assert_eq!(
            compare_values(&Value::Boolean(true), &Value::Boolean(false)),
            1
        );
        assert_eq!(
            compare_values(&Value::Boolean(true), &Value::Boolean(true)),
            0
        );
        assert_eq!(
            compare_values(&Value::Boolean(false), &Value::Boolean(true)),
            -1
        );
    }

    #[test]
    fn compare_mixed_types_nonzero_and_symmetric() {
        let pairs = [
            (Value::Integer(5), Value::String("5".into())),
            (Value::Boolean(true), Value::String("true".into())),
            (Value::String("2014".into()), Value::Integer(2014)),
            (
                Value::Integer(2014),
                Value::Date(crate::date::PureDate::year(2014).unwrap()),
            ),
        ];
        for (a, b) in &pairs {
            let ab = compare_values(a, b);
            let ba = compare_values(b, a);
            assert_ne!(ab, 0, "compare({a:?}, {b:?}) returned 0");
            assert_eq!(
                ab, -ba,
                "compare not symmetric for {a:?} vs {b:?}: ab={ab} ba={ba}"
            );
        }
    }

    #[test]
    fn equal_same_type() {
        assert_eq!(
            Equal
                .execute(&[lit_int(1), lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
        assert_eq!(
            Equal
                .execute(&[lit_int(1), lit_int(2)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn equal_different_types() {
        assert_eq!(
            Equal
                .execute(&[lit_int(1), lit_str("1")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }
}
