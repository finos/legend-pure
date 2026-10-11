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

//! Arithmetic natives fished so far: `plus`, `minus`, `times`, `divide`,
//! `abs`, `mod` and `rem`.
//!
//! All are **eager** — the native forces every argument up front via
//! `ctx.evaluate`. Helpers return `PureRuntimeError`; `?` on each call
//! promotes that into the `PureException` the trait requires.

use legend_pure_metamodel::types::ValueSpec;
use rust_decimal::Decimal;
use rust_decimal::prelude::{FromPrimitive, ToPrimitive};

use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all,
};
use crate::value::Value;

/// Promote two numeric values to the wider common type.
///
/// Lattice: `Integer < Float < Decimal`. Mixed pairs both convert to
/// the higher level. Float ↔ Decimal goes via [`Decimal::from_f64`] —
/// non-finite floats (NaN / ±Inf) fail to convert and the helper
/// returns `None`, which the caller surfaces as a clear type-mismatch
/// error rather than silent wrong arithmetic.
///
/// Returns `None` if either operand isn't a numeric variant
/// (`Integer` / `Float` / `Decimal`).
#[allow(clippy::cast_precision_loss)]
fn promote_pair(a: &Value, b: &Value) -> Option<(Value, Value)> {
    match (a, b) {
        // Same type — no promotion needed.
        (Value::Integer(_), Value::Integer(_))
        | (Value::Float(_), Value::Float(_))
        | (Value::Decimal(_), Value::Decimal(_)) => Some((a.clone(), b.clone())),
        // Integer ↔ Float
        (Value::Integer(i), Value::Float(_)) => Some((Value::Float(*i as f64), b.clone())),
        (Value::Float(_), Value::Integer(i)) => Some((a.clone(), Value::Float(*i as f64))),
        // Integer ↔ Decimal — exact (no precision loss).
        (Value::Integer(i), Value::Decimal(_)) => {
            Some((Value::Decimal(Decimal::from(*i)), b.clone()))
        }
        (Value::Decimal(_), Value::Integer(i)) => {
            Some((a.clone(), Value::Decimal(Decimal::from(*i))))
        }
        // Float ↔ Decimal — Decimal wins (wider precision).
        (Value::Float(f), Value::Decimal(_)) => {
            Decimal::from_f64(*f).map(|d| (Value::Decimal(d), b.clone()))
        }
        (Value::Decimal(_), Value::Float(f)) => {
            Decimal::from_f64(*f).map(|d| (a.clone(), Value::Decimal(d)))
        }
        _ => None,
    }
}

/// Pure `plus(Number[1], Number[1]): Number[1]` — addition.
///
/// Type promotion rules (matching Java):
/// - Integer + Integer → Integer
/// - Float + Float → Float
/// - Integer + Float | Float + Integer → Float
/// - Decimal + Decimal → Decimal
/// - Decimal + Integer | Integer + Decimal → Decimal
#[derive(Debug)]
pub struct Plus;

impl NativeFunction for Plus {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        // Pure's sole numeric `plus` signature is `(Number[*]):Number[1]`
        // (plus `(Decimal[*]):Decimal[1]`). The compiler lowers `a + b`
        // to `plus([a, b])` so the native always receives a single
        // argument. String concatenation is a distinct native
        // (`StringPlus`, mangled `plus_String_MANY__String_1_`) —
        // compile-time dispatch must route String operands there; this
        // native strictly handles numerics and errors on String input.
        expect_args("plus", &values, 1)?;
        let items: Vec<Value> = values[0].to_collection().iter().cloned().collect();
        Ok(Evaluated::new(plus_fold(items)?))
    }
}

/// Pairwise `plus` — promotes operands via [`promote_pair`] then
/// dispatches on the (now uniform) numeric type. String concatenation
/// has its own dedicated native (`StringPlus`, registered as
/// `plus_String_MANY__String_1_`), so this fold never sees strings.
fn plus_pair(a: &Value, b: &Value) -> Result<Value, PureRuntimeError> {
    match promote_pair(a, b) {
        Some((Value::Integer(a), Value::Integer(b))) => Ok(Value::Integer(a.wrapping_add(b))),
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a + b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a + b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "plus: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
}

/// Fold a collection of numerics (or strings) through pairwise `plus`.
///
/// Empty input → `Integer(0)`. Non-empty reduces left-to-right so types
/// follow Pure's promotion rules (Integer → Float → Decimal). Strings concat.
fn plus_fold(values: Vec<Value>) -> Result<Value, PureRuntimeError> {
    let mut iter = values.into_iter();
    let Some(first) = iter.next() else {
        return Ok(Value::Integer(0));
    };
    iter.try_fold(first, |acc, next| plus_pair(&acc, &next))
}

/// Pure `minus(Number[1], Number[1]): Number[1]` — subtraction.
#[derive(Debug)]
pub struct Minus;

impl NativeFunction for Minus {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        // Pure's sole `minus` signature is `(Number[*]):Number[1]`. The
        // compiler lowers `a - b` / `-x` to `minus([a, b])` / `minus([x])`
        // so the native always receives a single argument. Normalise via
        // `to_collection()` — this also handles the legacy direct-scalar
        // call shape some tests still emit from `->eval(x)`.
        expect_args("minus", &values, 1)?;
        let items: Vec<Value> = values[0].to_collection().iter().cloned().collect();
        Ok(Evaluated::new(minus_fold(items)?))
    }
}

/// Unary negation on a single numeric value. Unit-tagged values
/// negate the payload and keep the unit tag intact.
fn minus_unary(v: &Value) -> Result<Value, PureRuntimeError> {
    match v {
        Value::Integer(a) => Ok(Value::Integer(-a)),
        Value::Float(a) => Ok(Value::Float(-a)),
        Value::Decimal(a) => Ok(Value::Decimal(-*a)),
        Value::UnitInstance { unit_id, inner } => {
            let negated = minus_unary(inner)?;
            Ok(Value::UnitInstance {
                unit_id: *unit_id,
                inner: Box::new(negated),
            })
        }
        other => Err(PureRuntimeError::EvaluationError(format!(
            "minus: unsupported type {}",
            other.type_name()
        ))),
    }
}

/// Pairwise subtraction with the same Integer → Float → Decimal lattice
/// as [`plus_pair`].
fn minus_pair(a: &Value, b: &Value) -> Result<Value, PureRuntimeError> {
    match promote_pair(a, b) {
        Some((Value::Integer(a), Value::Integer(b))) => Ok(Value::Integer(a.wrapping_sub(b))),
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a - b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a - b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "minus: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
}

/// Fold a collection of numerics through pairwise subtraction, mirroring
/// Java Pure's `Minus.execute` switch on size:
/// - empty → `Integer(0)`
/// - size 1 → unary negate (`0 - x = -x`)
/// - size ≥ 2 → left-fold subtract: `[x, y, z, …]` → `x - y - z - …`
fn minus_fold(values: Vec<Value>) -> Result<Value, PureRuntimeError> {
    let mut iter = values.into_iter();
    let Some(first) = iter.next() else {
        return Ok(Value::Integer(0));
    };
    match iter.next() {
        None => minus_unary(&first),
        Some(second) => {
            let initial = minus_pair(&first, &second)?;
            iter.try_fold(initial, |acc, next| minus_pair(&acc, &next))
        }
    }
}

/// Pure `times(Number[1], Number[1]): Number[1]` — multiplication.
#[derive(Debug)]
pub struct Times;

impl NativeFunction for Times {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        // Pure's sole `times` signature is `(Number[*]):Number[1]`. The
        // compiler lowers `a * b` to `times([a, b])`; this native always
        // receives a single argument.
        expect_args("times", &values, 1)?;
        let items: Vec<Value> = values[0].to_collection().iter().cloned().collect();
        Ok(Evaluated::new(times_fold(items)?))
    }
}

/// Pairwise multiplication with the same Integer → Float → Decimal
/// lattice as [`plus_pair`].
fn times_pair(a: &Value, b: &Value) -> Result<Value, PureRuntimeError> {
    match promote_pair(a, b) {
        Some((Value::Integer(a), Value::Integer(b))) => Ok(Value::Integer(a.wrapping_mul(b))),
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a * b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a * b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "times: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
}

/// Fold a collection of numerics through pairwise multiplication, mirroring
/// Java Pure's `Times.execute` switch on size:
/// - empty → `Integer(1)` (multiplicative identity)
/// - size 1 → unchanged
/// - size ≥ 2 → left-fold multiply
fn times_fold(values: Vec<Value>) -> Result<Value, PureRuntimeError> {
    let mut iter = values.into_iter();
    let Some(first) = iter.next() else {
        return Ok(Value::Integer(1));
    };
    iter.try_fold(first, |acc, next| times_pair(&acc, &next))
}

/// Pure `divide(Number[1], Number[1]): Float[1]` and
/// `divide(Decimal[1], Decimal[1], scale:Integer[1]): Decimal[1]` —
/// numeric division with two overloads sharing this struct.
///
/// 2-arg: Integer/Integer and Decimal/Decimal both return `Float`,
/// matching Java's `divide(left, right): Float`.
///
/// 3-arg: Decimal/Decimal returns `Decimal` rounded to `scale`
/// fractional digits via banker's rounding (`rust_decimal`'s
/// `round_dp` default; matches Java `BigDecimal` `setScale(scale,
/// HALF_EVEN)`). `testDecimalDivide` exercises this with
/// `-3.1415d->divide(0.1d, 2) == -31.42d`.
#[derive(Debug)]
pub struct Divide;

impl NativeFunction for Divide {
    #[allow(clippy::cast_precision_loss)]
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        match values.len() {
            2 => {
                let promoted = promote_pair(&values[0], &values[1]).ok_or_else(|| {
                    PureRuntimeError::EvaluationError(format!(
                        "divide: unsupported types {} and {}",
                        values[0].type_name(),
                        values[1].type_name()
                    ))
                })?;
                let v = match promoted {
                    (Value::Integer(a), Value::Integer(b)) => {
                        if b == 0 {
                            return Err(PureRuntimeError::DivisionByZero.into());
                        }
                        Value::Float(a as f64 / b as f64)
                    }
                    (Value::Float(a), Value::Float(b)) => {
                        if b == 0.0 {
                            return Err(PureRuntimeError::DivisionByZero.into());
                        }
                        Value::Float(a / b)
                    }
                    (Value::Decimal(a), Value::Decimal(b)) => {
                        if b.is_zero() {
                            return Err(PureRuntimeError::DivisionByZero.into());
                        }
                        // Pure spec says Decimal/Decimal → Float; preserve that.
                        Value::Float(
                            a.to_f64().unwrap_or(f64::NAN) / b.to_f64().unwrap_or(f64::NAN),
                        )
                    }
                    _ => unreachable!("promote_pair returns matched types"),
                };
                Ok(Evaluated::new(v))
            }
            3 => {
                let a = values[0].as_decimal()?;
                let b = values[1].as_decimal()?;
                let scale = values[2].as_integer()?;
                if !(0..=28).contains(&scale) {
                    return Err(PureRuntimeError::EvaluationError(format!(
                        "divide: scale must be in 0..=28, got {scale}"
                    ))
                    .into());
                }
                if b.is_zero() {
                    return Err(PureRuntimeError::DivisionByZero.into());
                }
                let q = a.checked_div(b).ok_or_else(|| {
                    PureRuntimeError::EvaluationError("divide: Decimal division overflow".into())
                })?;
                #[allow(clippy::cast_possible_truncation, clippy::cast_sign_loss)]
                let rounded = q.round_dp(scale as u32);
                Ok(Evaluated::new(Value::Decimal(rounded)))
            }
            n => Err(PureRuntimeError::EvaluationError(format!(
                "divide: expected 2 or 3 argument(s), got {n}"
            ))
            .into()),
        }
    }
}

/// Pure `abs(Number[1]): Number[1]` — absolute value.
#[derive(Debug)]
pub struct Abs;

impl NativeFunction for Abs {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("abs", &values, 1)?;
        let v = match &values[0] {
            Value::Integer(i) => Value::Integer(i.wrapping_abs()),
            Value::Float(f) => Value::Float(f.abs()),
            Value::Decimal(d) => Value::Decimal(d.abs()),
            other => return Err(PureRuntimeError::type_mismatch("Number", other).into()),
        };
        Ok(Evaluated::new(v))
    }
}

/// Pure `mod(Integer[1], Integer[1]): Integer[1]` — modulus (always non-negative).
#[derive(Debug)]
pub struct Mod;

impl NativeFunction for Mod {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("mod", &values, 2)?;
        let a = values[0].as_integer()?;
        let b = values[1].as_integer()?;
        if b == 0 {
            return Err(PureRuntimeError::DivisionByZero.into());
        }
        Ok(Evaluated::new(Value::Integer(a.rem_euclid(b))))
    }
}

/// Pure `rem(Number[1], Number[1]): Number[1]` — remainder (preserves sign of dividend).
///
/// Promotes via `promote_pair` so mixed-type calls like `rem(7.5, 2)`
/// or `rem(7d, 2)` work without the caller having to coerce. Empty
/// remainder for `Decimal` uses `Decimal::checked_rem` which returns
/// `None` only on division-by-zero (already trapped above).
#[derive(Debug)]
pub struct Rem;

impl NativeFunction for Rem {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("rem", &values, 2)?;
        let promoted = promote_pair(&values[0], &values[1]).ok_or_else(|| {
            PureRuntimeError::EvaluationError(format!(
                "rem: unsupported types {} and {}",
                values[0].type_name(),
                values[1].type_name()
            ))
        })?;
        // Java Pure's `Rem.execute` throws "Cannot divide <dividend> by zero"
        // (Rem.java:?), parametric on the dividend's display value. Build the
        // dividend representation once before promotion-loss can change its
        // shape, then reuse for any zero-divisor branch.
        let dividend_str = crate::native::math::java_number_string(&values[0]);
        let div_zero_err = || -> PureException {
            PureRuntimeError::EvaluationError(format!("Cannot divide {dividend_str} by zero"))
                .into()
        };
        let v = match promoted {
            (Value::Integer(a), Value::Integer(b)) => {
                if b == 0 {
                    return Err(div_zero_err());
                }
                Value::Integer(a % b)
            }
            (Value::Float(a), Value::Float(b)) => {
                if b == 0.0 {
                    return Err(div_zero_err());
                }
                Value::Float(a % b)
            }
            (Value::Decimal(a), Value::Decimal(b)) => {
                if b.is_zero() {
                    return Err(div_zero_err());
                }
                Value::Decimal(a % b)
            }
            _ => unreachable!("promote_pair returns matched types"),
        };
        Ok(Evaluated::new(v))
    }
}

/// Register the fished arithmetic natives under every signature the
/// platform declares for them (one implementation each).
pub fn register(registry: &mut NativeRegistry) {
    registry.register("plus_Integer_MANY__Integer_1_", Plus);
    registry.register("plus_Float_MANY__Float_1_", Plus);
    registry.register("plus_Decimal_MANY__Decimal_1_", Plus);
    registry.register("plus_Number_MANY__Number_1_", Plus);

    registry.register("minus_Integer_MANY__Integer_1_", Minus);
    registry.register("minus_Float_MANY__Float_1_", Minus);
    registry.register("minus_Decimal_MANY__Decimal_1_", Minus);
    registry.register("minus_Number_MANY__Number_1_", Minus);

    registry.register("times_Integer_MANY__Integer_1_", Times);
    registry.register("times_Float_MANY__Float_1_", Times);
    registry.register("times_Decimal_MANY__Decimal_1_", Times);
    registry.register("times_Number_MANY__Number_1_", Times);

    registry.register("divide_Number_1__Number_1__Float_1_", Divide);
    registry.register("divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_", Divide);

    registry.register("abs_Integer_1__Integer_1_", Abs);
    registry.register("abs_Float_1__Float_1_", Abs);
    registry.register("abs_Decimal_1__Decimal_1_", Abs);
    registry.register("abs_Number_1__Number_1_", Abs);

    registry.register("mod_Integer_1__Integer_1__Integer_1_", Mod);

    registry.register("rem_Number_1__Number_1__Number_1_", Rem);
}

#[cfg(test)]
mod tests {
    use std::str::FromStr;

    use rust_decimal::Decimal;

    use super::*;
    use crate::native::{MockCtx, lit_bool, lit_collection, lit_float, lit_int, lit_str};

    /// Decimal-literal shim — `MockCtx` supports only bare `DecimalLiteral`
    /// specs, so tests that need a Decimal value go through this helper
    /// rather than fabricating an ad-hoc `ValueSpec`.
    fn ld(d: Decimal) -> legend_pure_metamodel::types::ValueSpec {
        crate::native::lit_decimal(d)
    }

    #[test]
    fn plus_integers() {
        let r = Plus
            .execute(
                &[crate::native::lit_collection(vec![lit_int(2), lit_int(3)])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::Integer(5));
    }

    #[test]
    fn plus_floats() {
        let r = Plus
            .execute(
                &[crate::native::lit_collection(vec![
                    lit_float(1.5),
                    lit_float(2.5),
                ])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::Float(4.0));
    }

    #[test]
    fn plus_integer_float_promotion() {
        let r = Plus
            .execute(
                &[crate::native::lit_collection(vec![
                    lit_int(1),
                    lit_float(2.5),
                ])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::Float(3.5));
    }

    #[test]
    fn plus_decimals() {
        let a = Decimal::from_str("10.50").unwrap();
        let b = Decimal::from_str("3.25").unwrap();
        let r = Plus
            .execute(
                &[crate::native::lit_collection(vec![ld(a), ld(b)])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(
            r.into_value(),
            Value::Decimal(Decimal::from_str("13.75").unwrap())
        );
    }

    #[test]
    fn minus_integers() {
        let r = Minus
            .execute(
                &[crate::native::lit_collection(vec![lit_int(10), lit_int(3)])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::Integer(7));
    }

    #[test]
    fn times_integers() {
        let r = Times
            .execute(
                &[crate::native::lit_collection(vec![lit_int(4), lit_int(5)])],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::Integer(20));
    }

    #[test]
    fn plus_folds_integer_collection() {
        // `plus([1, 2, 3])` sums the collection — this is the surveyor path
        // (`$results->map(r | $r.elapsed)->plus()`).
        let spec = lit_collection(vec![lit_int(1), lit_int(2), lit_int(3)]);
        assert_eq!(
            Plus.execute(&[spec], &mut MockCtx).unwrap().into_value(),
            Value::Integer(6)
        );
    }

    fn dec(s: &str) -> Value {
        use std::str::FromStr;
        Value::Decimal(rust_decimal::Decimal::from_str(s).unwrap())
    }

    #[test]
    fn promote_pair_same_type_passthrough() {
        // Same type — returned unchanged.
        let (a, b) = promote_pair(&Value::Integer(1), &Value::Integer(2)).unwrap();
        assert_eq!(a, Value::Integer(1));
        assert_eq!(b, Value::Integer(2));
        let (a, b) = promote_pair(&Value::Float(1.5), &Value::Float(2.5)).unwrap();
        assert_eq!(a, Value::Float(1.5));
        assert_eq!(b, Value::Float(2.5));
        let (a, b) = promote_pair(&dec("1.5"), &dec("2.5")).unwrap();
        assert_eq!(a, dec("1.5"));
        assert_eq!(b, dec("2.5"));
    }

    #[test]
    fn promote_pair_int_float() {
        // Integer ↔ Float — both end up Float.
        let (a, b) = promote_pair(&Value::Integer(3), &Value::Float(2.5)).unwrap();
        assert_eq!(a, Value::Float(3.0));
        assert_eq!(b, Value::Float(2.5));
        let (a, b) = promote_pair(&Value::Float(2.5), &Value::Integer(3)).unwrap();
        assert_eq!(a, Value::Float(2.5));
        assert_eq!(b, Value::Float(3.0));
    }

    #[test]
    fn promote_pair_int_decimal() {
        // Integer ↔ Decimal — Decimal wins, exact via Decimal::from(i64).
        let (a, b) = promote_pair(&Value::Integer(5), &dec("2.5")).unwrap();
        assert_eq!(a, dec("5"));
        assert_eq!(b, dec("2.5"));
        let (a, b) = promote_pair(&dec("2.5"), &Value::Integer(5)).unwrap();
        assert_eq!(a, dec("2.5"));
        assert_eq!(b, dec("5"));
    }

    #[test]
    fn promote_pair_float_decimal() {
        // Float ↔ Decimal — Decimal wins, via Decimal::from_f64.
        let (a, b) = promote_pair(&Value::Float(1.5), &dec("2.5")).unwrap();
        assert_eq!(a, dec("1.5"));
        assert_eq!(b, dec("2.5"));
        let (a, b) = promote_pair(&dec("2.5"), &Value::Float(1.5)).unwrap();
        assert_eq!(a, dec("2.5"));
        assert_eq!(b, dec("1.5"));
    }

    #[test]
    fn promote_pair_non_numeric_returns_none() {
        assert!(promote_pair(&Value::Integer(1), &Value::String("x".into())).is_none());
        assert!(promote_pair(&Value::Boolean(true), &Value::Integer(1)).is_none());
    }

    #[test]
    fn promote_pair_nan_float_with_decimal_returns_none() {
        // Decimal::from_f64 rejects NaN/Inf — promote_pair surfaces that
        // as None so the caller errors with a clear message instead of
        // silent wrong arithmetic.
        assert!(promote_pair(&Value::Float(f64::NAN), &dec("1")).is_none());
        assert!(promote_pair(&dec("1"), &Value::Float(f64::INFINITY)).is_none());
    }

    #[test]
    fn plus_pair_handles_decimal_float_mix() {
        // Phase 4 deliverable: Decimal+Float in pairwise plus no
        // longer errors. Tests the pair helper directly because Plus's
        // public surface takes a fold-collected Number[*] arg.
        let r = plus_pair(&Value::Float(1.5), &dec("2.5")).unwrap();
        assert_eq!(r, dec("4"));
        let r = plus_pair(&dec("2.5"), &Value::Float(1.5)).unwrap();
        assert_eq!(r, dec("4"));
    }

    #[test]
    fn divide_integers_returns_float() {
        let r = Divide
            .execute(&[lit_int(7), lit_int(2)], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Float(3.5));
    }

    #[test]
    fn divide_by_zero_errors() {
        let r = Divide.execute(&[lit_int(1), lit_int(0)], &mut MockCtx);
        assert!(r.is_err());
    }

    #[test]
    fn abs_negative() {
        assert_eq!(
            Abs.execute(&[lit_int(-5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(5)
        );
    }

    #[test]
    fn wrong_arg_count_errors() {
        // `plus` takes exactly one `Number[*]` argument. A lone Integer
        // literal is normalised to a singleton collection and returns
        // unchanged. An empty collection returns the identity `Integer(0)`.
        // Zero arguments at the native-call level is an arity violation.
        assert_eq!(
            Plus.execute(&[lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
        assert_eq!(
            Plus.execute(&[crate::native::lit_collection(Vec::new())], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(0)
        );
        assert!(Plus.execute(&[], &mut MockCtx).is_err());
        assert!(Abs.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn type_mismatch_errors() {
        // String instead of Number
        assert!(
            Plus.execute(&[lit_str("1"), lit_int(1)], &mut MockCtx)
                .is_err()
        );
        assert!(Abs.execute(&[lit_bool(true)], &mut MockCtx).is_err());
        // One float, one string
        assert!(
            Plus.execute(&[lit_float(1.0), lit_str("2")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn mod_positive() {
        assert_eq!(
            Mod.execute(&[lit_int(7), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
    }

    #[test]
    fn mod_negative_dividend() {
        // rem_euclid: -7 mod 3 = 2 (always non-negative)
        assert_eq!(
            Mod.execute(&[lit_int(-7), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(2)
        );
    }

    #[test]
    fn division_by_zero_errors() {
        assert!(
            Divide
                .execute(&[lit_int(5), lit_int(0)], &mut MockCtx)
                .is_err()
        );
        assert!(
            Divide
                .execute(&[lit_float(5.0), lit_float(0.0)], &mut MockCtx)
                .is_err()
        );
        assert!(
            Mod.execute(&[lit_int(5), lit_int(0)], &mut MockCtx)
                .is_err()
        );
        assert!(
            Rem.execute(&[lit_int(5), lit_int(0)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn rem_negative_dividend() {
        // Rust remainder: -7 % 3 = -1 (preserves sign)
        assert_eq!(
            Rem.execute(&[lit_int(-7), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    #[test]
    fn rem_handles_mixed_types() {
        // Previously errored with "rem: both arguments must be the same
        // numeric type". Now promotes via promote_pair.
        let r = Rem
            .execute(&[lit_float(7.5), lit_int(2)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, Value::Float(1.5));
    }
}
