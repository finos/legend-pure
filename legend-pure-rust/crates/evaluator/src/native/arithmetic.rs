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

//! Arithmetic natives: `plus`, `minus`, `times`, `divide`,
//! `abs`, `mod` and `rem`.

use legend_pure_metamodel::types::ValueSpec;
use rust_decimal::Decimal;
use rust_decimal::prelude::{FromPrimitive, ToPrimitive};

use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, force_all, force1, force2,
};
use crate::value::Value;

/// Promote two numeric values to the wider common type.
///
/// Lattice: `Integer < Float < Decimal`. Mixed pairs both convert to
/// the higher level.
///
/// Returns `None` if either operand isn't a numeric variant
/// (`Integer` / `Float` / `Decimal`).
#[allow(clippy::cast_precision_loss)]
fn promote_pair(a: &Value, b: &Value) -> Option<(Value, Value)> {
    match (a, b) {
        (Value::Integer(_), Value::Integer(_))
        | (Value::Float(_), Value::Float(_))
        | (Value::Decimal(_), Value::Decimal(_)) => Some((a.clone(), b.clone())),
        (Value::Integer(i), Value::Float(_)) => Some((Value::Float(*i as f64), b.clone())),
        (Value::Float(_), Value::Integer(i)) => Some((a.clone(), Value::Float(*i as f64))),
        (Value::Integer(i), Value::Decimal(_)) => {
            Some((Value::Decimal(Decimal::from(*i)), b.clone()))
        }
        (Value::Decimal(_), Value::Integer(i)) => {
            Some((a.clone(), Value::Decimal(Decimal::from(*i))))
        }
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
        let operands = Operands::of("plus", args, ctx, plus_pair)?;
        Ok(Evaluated::new(
            operands.finish(|| Ok(Value::Integer(0)), Ok)?,
        ))
    }
}

/// The left fold of a `Number[*]` operand as its elements are evaluated,
/// without building the collection: the first element is kept apart so
/// the one-element case (unary `minus`) and the empty case keep their own
/// meaning.
struct Operands {
    first: Option<Value>,
    folded: Option<Value>,
}

impl Operands {
    fn of(
        name: &str,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
        pair: fn(&Value, &Value) -> Result<Value, PureRuntimeError>,
    ) -> Result<Self, PureException> {
        if args.len() != 1 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "{name}: expected 1 argument(s), got {}",
                args.len()
            ))
            .into());
        }
        let mut operands = Self {
            first: None,
            folded: None,
        };
        ctx.for_each_value(&args[0], &mut |value| {
            match (&operands.first, operands.folded.take()) {
                (None, _) => operands.first = Some(value),
                (Some(first), None) => operands.folded = Some(pair(first, &value)?),
                (Some(_), Some(acc)) => operands.folded = Some(pair(&acc, &value)?),
            }
            Ok(())
        })?;
        Ok(operands)
    }

    /// The result: `empty` when there was no operand, `only` applied to a
    /// lone operand, the fold otherwise.
    fn finish(
        self,
        empty: impl FnOnce() -> Result<Value, PureRuntimeError>,
        only: impl FnOnce(Value) -> Result<Value, PureRuntimeError>,
    ) -> Result<Value, PureRuntimeError> {
        match (self.first, self.folded) {
            (None, _) => empty(),
            (Some(first), None) => only(first),
            (Some(_), Some(folded)) => Ok(folded),
        }
    }
}

/// Integer results live in `i64`; Java promotes to `BigInteger` instead.
fn outside_i64() -> PureRuntimeError {
    PureRuntimeError::Unsupported("Integer arithmetic outside the 64-bit range".into())
}

/// Pairwise `plus`.
fn plus_pair(a: &Value, b: &Value) -> Result<Value, PureRuntimeError> {
    match promote_pair(a, b) {
        Some((Value::Integer(a), Value::Integer(b))) => {
            a.checked_add(b).map(Value::Integer).ok_or_else(outside_i64)
        }
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a + b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a + b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "plus: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
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
        let operands = Operands::of("minus", args, ctx, minus_pair)?;
        Ok(Evaluated::new(operands.finish(
            || Ok(Value::Integer(0)),
            |only| minus_unary(&only),
        )?))
    }
}

/// Unary negation on a single numeric value. Unit-tagged values
/// negate the payload and keep the unit tag intact.
fn minus_unary(v: &Value) -> Result<Value, PureRuntimeError> {
    match v {
        Value::Integer(a) => a.checked_neg().map(Value::Integer).ok_or_else(outside_i64),
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
        Some((Value::Integer(a), Value::Integer(b))) => {
            a.checked_sub(b).map(Value::Integer).ok_or_else(outside_i64)
        }
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a - b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a - b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "minus: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
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
        let operands = Operands::of("times", args, ctx, times_pair)?;
        Ok(Evaluated::new(
            operands.finish(|| Ok(Value::Integer(1)), Ok)?,
        ))
    }
}

/// Pairwise multiplication with the same Integer → Float → Decimal
/// lattice as [`plus_pair`].
fn times_pair(a: &Value, b: &Value) -> Result<Value, PureRuntimeError> {
    match promote_pair(a, b) {
        Some((Value::Integer(a), Value::Integer(b))) => {
            a.checked_mul(b).map(Value::Integer).ok_or_else(outside_i64)
        }
        Some((Value::Float(a), Value::Float(b))) => Ok(Value::Float(a * b)),
        Some((Value::Decimal(a), Value::Decimal(b))) => Ok(Value::Decimal(a * b)),
        _ => Err(PureRuntimeError::EvaluationError(format!(
            "times: unsupported types {} and {}",
            a.type_name(),
            b.type_name()
        ))),
    }
}

/// Pure `divide(Number[1], Number[1]): Float[1]` and
/// `divide(Decimal[1], Decimal[1], scale:Integer[1]): Decimal[1]` —
/// numeric division with two overloads sharing this struct.
///
/// 2-arg: Integer/Integer and Decimal/Decimal both return `Float`,
/// matching Java's `divide(left, right): Float`.
///
/// 3-arg: Decimal/Decimal returns `Decimal` rounded to `scale`
/// fractional digits via banker's rounding.
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
        let a0 = force1("abs", args, ctx)?;
        let v = match &a0 {
            Value::Integer(i) => i
                .checked_abs()
                .map(Value::Integer)
                .ok_or_else(outside_i64)?,
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
        let (a0, a1) = force2("mod", args, ctx)?;
        let a = a0.as_integer()?;
        let b = a1.as_integer()?;
        if b == 0 {
            return Err(PureRuntimeError::DivisionByZero.into());
        }
        let r = a.checked_rem_euclid(b).ok_or_else(outside_i64)?;
        Ok(Evaluated::new(Value::Integer(r)))
    }
}

/// Pure `rem(Number[1], Number[1]): Number[1]` — remainder (preserves sign of dividend).
#[derive(Debug)]
pub struct Rem;

impl NativeFunction for Rem {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("rem", args, ctx)?;
        let promoted = promote_pair(&a0, &a1).ok_or_else(|| {
            PureRuntimeError::EvaluationError(format!(
                "rem: unsupported types {} and {}",
                a0.type_name(),
                a1.type_name()
            ))
        })?;
        let dividend_str = crate::native::math::java_number_string(&a0);
        let div_zero_err = || -> PureException {
            PureRuntimeError::EvaluationError(format!("Cannot divide {dividend_str} by zero"))
                .into()
        };
        let v = match promoted {
            (Value::Integer(a), Value::Integer(b)) => {
                if b == 0 {
                    return Err(div_zero_err());
                }
                a.checked_rem(b)
                    .map(Value::Integer)
                    .ok_or_else(outside_i64)?
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

    /// Decimal-literal shim.
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

    fn is_outside_i64(r: Result<Evaluated, PureException>) -> bool {
        matches!(
            r.map_err(|e| e.into_data().kind),
            Err(crate::error::PureExceptionKind::ExecutionError(
                PureRuntimeError::Unsupported(what)
            )) if what.contains("64-bit")
        )
    }

    #[test]
    fn integer_overflow_is_a_typed_error_not_a_wrap() {
        let plus = Plus.execute(
            &[crate::native::lit_collection(vec![
                lit_int(i64::MAX),
                lit_int(1),
            ])],
            &mut MockCtx,
        );
        assert!(is_outside_i64(plus));
        let negate = Minus.execute(
            &[crate::native::lit_collection(vec![lit_int(i64::MIN)])],
            &mut MockCtx,
        );
        assert!(is_outside_i64(negate));
        let times = Times.execute(
            &[crate::native::lit_collection(vec![
                lit_int(i64::MAX),
                lit_int(2),
            ])],
            &mut MockCtx,
        );
        assert!(is_outside_i64(times));
        assert!(is_outside_i64(
            Abs.execute(&[lit_int(i64::MIN)], &mut MockCtx)
        ));
        assert!(is_outside_i64(
            Rem.execute(&[lit_int(i64::MIN), lit_int(-1)], &mut MockCtx)
        ));
        assert!(is_outside_i64(
            Mod.execute(&[lit_int(i64::MIN), lit_int(-1)], &mut MockCtx)
        ));
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
        let (a, b) = promote_pair(&Value::Integer(3), &Value::Float(2.5)).unwrap();
        assert_eq!(a, Value::Float(3.0));
        assert_eq!(b, Value::Float(2.5));
        let (a, b) = promote_pair(&Value::Float(2.5), &Value::Integer(3)).unwrap();
        assert_eq!(a, Value::Float(2.5));
        assert_eq!(b, Value::Float(3.0));
    }

    #[test]
    fn promote_pair_int_decimal() {
        let (a, b) = promote_pair(&Value::Integer(5), &dec("2.5")).unwrap();
        assert_eq!(a, dec("5"));
        assert_eq!(b, dec("2.5"));
        let (a, b) = promote_pair(&dec("2.5"), &Value::Integer(5)).unwrap();
        assert_eq!(a, dec("2.5"));
        assert_eq!(b, dec("5"));
    }

    #[test]
    fn promote_pair_float_decimal() {
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
        assert!(promote_pair(&Value::Float(f64::NAN), &dec("1")).is_none());
        assert!(promote_pair(&dec("1"), &Value::Float(f64::INFINITY)).is_none());
    }

    #[test]
    fn plus_pair_handles_decimal_float_mix() {
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
        assert!(
            Plus.execute(&[lit_str("1"), lit_int(1)], &mut MockCtx)
                .is_err()
        );
        assert!(Abs.execute(&[lit_bool(true)], &mut MockCtx).is_err());
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
        assert_eq!(
            Rem.execute(&[lit_int(-7), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    #[test]
    fn rem_handles_mixed_types() {
        let r = Rem
            .execute(&[lit_float(7.5), lit_int(2)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, Value::Float(1.5));
    }
}
