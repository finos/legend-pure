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

//! Mathematical natives: rounding (`floor`, `ceiling`, `round`, `sign`),
//! analytic (`sqrt`, `cbrt`, `exp`, `log`, `log10`, `pow`), trigonometric
//! (`sin`, `cos`, `tan`, `cot`, `asin`, `acos`, `atan`, `atan2`), numeric
//! conversion (`toFloat`, `toDecimal`) and string parsing (`parseInteger`,
//! `parseFloat`, `parseBoolean`).

use legend_pure_metamodel::types::ValueSpec;
use rust_decimal::Decimal;
use rust_decimal::prelude::{FromPrimitive, ToPrimitive};

use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all, force1,
    force2,
};
use crate::value::Value;

/// Coerce a numeric `Value` (Integer, Float, or Decimal) to `f64`.
fn number_to_f64(func_name: &str, v: &Value) -> Result<f64, PureRuntimeError> {
    match v {
        Value::Decimal(d) => d.to_f64().ok_or_else(|| {
            PureRuntimeError::EvaluationError(format!(
                "{func_name}: cannot convert Decimal {d} to Float"
            ))
        }),
        _ => v.as_float(),
    }
}

/// Render a numeric value using Java's `toString` rules.
///
/// The key divergence from Rust's default `{f64}`/`{i64}` Display is
/// that Java's `Double.toString` always emits at least one decimal place
/// for integer-valued doubles (`2.0`, not `2`); integer values format as
/// the bare number. NaN / infinity surface verbatim.
#[must_use]
pub fn java_number_string(v: &Value) -> String {
    match v {
        Value::Integer(i) => i.to_string(),
        Value::Float(f) => {
            if f.is_nan() {
                "NaN".to_string()
            } else if f.is_infinite() {
                if *f > 0.0 {
                    "Infinity".into()
                } else {
                    "-Infinity".into()
                }
            } else if (*f - f.trunc()).abs() < f64::EPSILON && f.is_finite() {
                format!("{f:.1}")
            } else {
                f.to_string()
            }
        }
        Value::Decimal(d) => d.to_string(),
        other => other.to_string(),
    }
}

/// Pure `floor(Number[1]):Integer[1]` — largest integer ≤ x.
#[derive(Debug)]
pub struct Floor;

impl NativeFunction for Floor {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("floor", args, ctx)?;
        let x = number_to_f64("floor", &a0)?;
        #[allow(clippy::cast_possible_truncation)]
        Ok(Evaluated::new(Value::Integer(x.floor() as i64)))
    }
}

/// Pure `ceiling(Number[1]):Integer[1]` — smallest integer ≥ x.
#[derive(Debug)]
pub struct Ceiling;

impl NativeFunction for Ceiling {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("ceiling", args, ctx)?;
        let x = number_to_f64("ceiling", &a0)?;
        #[allow(clippy::cast_possible_truncation)]
        Ok(Evaluated::new(Value::Integer(x.ceil() as i64)))
    }
}

/// Pure `round(Number[1]):Integer[1]`
/// Pure `round(Decimal[1], scale:Integer[1]):Decimal[1]`
/// Pure `round(Float[1], scale:Integer[1]):Float[1]`
///
/// **Banker's rounding** (half-to-even), matching Java Pure / IEEE 754
/// default semantics.
///
/// - 1-arg: rounds any `Number` to the nearest `Integer`. For exact
///   halves, picks the even neighbor (`f64::round_ties_even`).
/// - 2-arg with `Decimal`: rounds to `scale` decimal places via
///   `Decimal::round_dp` (banker's rounding by default).
/// - 2-arg with `Float`: scales by 10^scale, applies `round_ties_even`,
///   scales back. Returns Float per the platform signature.
#[derive(Debug)]
pub struct Round;

impl NativeFunction for Round {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        match values.len() {
            1 => {
                let x = number_to_f64("round", &values[0])?;
                #[allow(clippy::cast_possible_truncation)]
                Ok(Evaluated::new(Value::Integer(x.round_ties_even() as i64)))
            }
            2 => {
                let scale = scale_arg(&values[1])?;
                match &values[0] {
                    Value::Decimal(d) => Ok(Evaluated::new(Value::Decimal(d.round_dp(scale)))),
                    Value::Float(f) => {
                        let factor = 10f64.powi(i32::try_from(scale).map_err(|_| {
                            PureRuntimeError::EvaluationError(format!(
                                "round: scale {scale} out of i32 range"
                            ))
                        })?);
                        let rounded = (f * factor).round_ties_even() / factor;
                        Ok(Evaluated::new(Value::Float(rounded)))
                    }
                    Value::Integer(i) => {
                        if scale == 0 {
                            Ok(Evaluated::new(Value::Integer(*i)))
                        } else {
                            Ok(Evaluated::new(Value::Decimal(Decimal::from(*i))))
                        }
                    }
                    other => Err(PureRuntimeError::type_mismatch("Decimal or Float", other).into()),
                }
            }
            n => Err(PureRuntimeError::EvaluationError(format!(
                "round: expected 1 or 2 argument(s), got {n}"
            ))
            .into()),
        }
    }
}

fn scale_arg(v: &Value) -> Result<u32, PureException> {
    match v {
        Value::Integer(n) if *n >= 0 => Ok(u32::try_from(*n).map_err(|_| {
            PureRuntimeError::EvaluationError(format!("round: scale {n} out of u32 range"))
        })?),
        Value::Integer(n) => Err(PureRuntimeError::EvaluationError(format!(
            "round: scale must be non-negative, got {n}"
        ))
        .into()),
        other => Err(PureRuntimeError::type_mismatch("Integer", other).into()),
    }
}

/// Pure `sign(Number[1]):Integer[1]` — returns -1, 0, or 1.
#[derive(Debug)]
pub struct Sign;

impl NativeFunction for Sign {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("sign", args, ctx)?;
        let x = number_to_f64("sign", &a0)?;
        let s = if x > 0.0 {
            1
        } else if x < 0.0 {
            -1
        } else {
            0
        };
        Ok(Evaluated::new(Value::Integer(s)))
    }
}

/// Pure `sqrt(Number[1]):Float[1]` — square root.
///
/// Negative inputs throw the platform-pinned error
/// `"Unable to compute sqrt of <input>"`.
#[derive(Debug)]
pub struct Sqrt;

impl NativeFunction for Sqrt {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("sqrt", args, ctx)?;
        let x = number_to_f64("sqrt", &a0)?;
        let r = x.sqrt();
        if r.is_nan() {
            return Err(PureRuntimeError::EvaluationError(format!(
                "Unable to compute sqrt of {}",
                java_number_string(&a0)
            ))
            .into());
        }
        Ok(Evaluated::new(Value::Float(r)))
    }
}

/// Pure `cbrt(Number[1]):Float[1]` — cube root.
#[derive(Debug)]
pub struct Cbrt;

impl NativeFunction for Cbrt {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("cbrt", args, ctx)?;
        let x = number_to_f64("cbrt", &a0)?;
        Ok(Evaluated::new(Value::Float(x.cbrt())))
    }
}

/// Pure `exp(Number[1]):Float[1]` — natural exponential, `e^x`.
#[derive(Debug)]
pub struct Exp;

impl NativeFunction for Exp {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("exp", args, ctx)?;
        let x = number_to_f64("exp", &a0)?;
        Ok(Evaluated::new(Value::Float(x.exp())))
    }
}

/// Pure `log(Number[1]):Float[1]` — natural logarithm (base `e`).
///
/// `log(0)` yields `-inf`; `log(negative)` yields `NaN` per IEEE-754.
#[derive(Debug)]
pub struct Log;

impl NativeFunction for Log {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("log", args, ctx)?;
        let x = number_to_f64("log", &a0)?;
        Ok(Evaluated::new(Value::Float(x.ln())))
    }
}

/// Pure `log10(Number[1]):Float[1]` — base-10 logarithm.
#[derive(Debug)]
pub struct Log10;

impl NativeFunction for Log10 {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("log10", &values, 1)?;
        let x = number_to_f64("log10", &values[0])?;
        Ok(Evaluated::new(Value::Float(x.log10())))
    }
}

/// Pure `pow(Number[1], Number[1]):Float[1]` — `base^exponent`.
#[derive(Debug)]
pub struct Pow;

impl NativeFunction for Pow {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("pow", args, ctx)?;
        let base = number_to_f64("pow", &a0)?;
        let exp = number_to_f64("pow", &a1)?;
        Ok(Evaluated::new(Value::Float(base.powf(exp))))
    }
}

/// Pure `sin(Number[1]):Float[1]` — sine of `x` in radians.
#[derive(Debug)]
pub struct Sin;

impl NativeFunction for Sin {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("sin", args, ctx)?;
        let x = number_to_f64("sin", &a0)?;
        Ok(Evaluated::new(Value::Float(x.sin())))
    }
}

/// Pure `cos(Number[1]):Float[1]` — cosine of `x` in radians.
#[derive(Debug)]
pub struct Cos;

impl NativeFunction for Cos {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("cos", args, ctx)?;
        let x = number_to_f64("cos", &a0)?;
        Ok(Evaluated::new(Value::Float(x.cos())))
    }
}

/// Pure `tan(Number[1]):Float[1]` — tangent of `x` in radians.
#[derive(Debug)]
pub struct Tan;

impl NativeFunction for Tan {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("tan", args, ctx)?;
        let x = number_to_f64("tan", &a0)?;
        Ok(Evaluated::new(Value::Float(x.tan())))
    }
}

/// Pure `cot(Number[1]):Float[1]` — cotangent (`1 / tan(x)`) of `x` in radians.
#[derive(Debug)]
pub struct Cot;

impl NativeFunction for Cot {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("cot", args, ctx)?;
        let x = number_to_f64("cot", &a0)?;
        Ok(Evaluated::new(Value::Float(1.0 / x.tan())))
    }
}

/// Pure `asin(Number[1]):Float[1]` — inverse sine (radians). Inputs
/// outside `[-1, 1]` throw `"Unable to compute asin of <input>"`.
#[derive(Debug)]
pub struct Asin;

impl NativeFunction for Asin {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("asin", args, ctx)?;
        let x = number_to_f64("asin", &a0)?;
        let r = x.asin();
        if r.is_nan() {
            return Err(PureRuntimeError::EvaluationError(format!(
                "Unable to compute asin of {}",
                java_number_string(&a0)
            ))
            .into());
        }
        Ok(Evaluated::new(Value::Float(r)))
    }
}

/// Pure `acos(Number[1]):Float[1]` — inverse cosine (radians). Inputs
/// outside `[-1, 1]` throw `"Unable to compute acos of <input>"`.
#[derive(Debug)]
pub struct Acos;

impl NativeFunction for Acos {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("acos", args, ctx)?;
        let x = number_to_f64("acos", &a0)?;
        let r = x.acos();
        if r.is_nan() {
            return Err(PureRuntimeError::EvaluationError(format!(
                "Unable to compute acos of {}",
                java_number_string(&a0)
            ))
            .into());
        }
        Ok(Evaluated::new(Value::Float(r)))
    }
}

/// Pure `atan(Number[1]):Float[1]` — inverse tangent (radians).
#[derive(Debug)]
pub struct Atan;

impl NativeFunction for Atan {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("atan", args, ctx)?;
        let x = number_to_f64("atan", &a0)?;
        Ok(Evaluated::new(Value::Float(x.atan())))
    }
}

/// Pure `atan2(Number[1], Number[1]):Float[1]` — four-quadrant inverse
/// tangent of `y / x`, returning the angle in radians.
#[derive(Debug)]
pub struct Atan2;

impl NativeFunction for Atan2 {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("atan2", &values, 2)?;
        let y = number_to_f64("atan2", &values[0])?;
        let x = number_to_f64("atan2", &values[1])?;
        Ok(Evaluated::new(Value::Float(y.atan2(x))))
    }
}

/// Pure `toFloat(Number[1]):Float[1]` — numeric → Float coercion.
#[derive(Debug)]
pub struct ToFloat;

impl NativeFunction for ToFloat {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("toFloat", args, ctx)?;
        let x = number_to_f64("toFloat", &a0)?;
        Ok(Evaluated::new(Value::Float(x)))
    }
}

/// Pure `toDecimal(Number[1]):Decimal[1]` — numeric → Decimal coercion.
///
/// `Decimal` inputs pass through unchanged. `Integer` is exact via
/// [`Decimal::from`]. `Float` routes through [`Decimal::from_f64`].
/// NaN / infinity return `None` and surface as an
/// `EvaluationError`.
#[derive(Debug)]
pub struct ToDecimal;

impl NativeFunction for ToDecimal {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("toDecimal", args, ctx)?;
        let v = match &a0 {
            Value::Decimal(d) => Ok(Value::Decimal(*d)),
            Value::Integer(i) => Ok(Value::Decimal(Decimal::from(*i))),
            Value::Float(f) => Decimal::from_f64(*f)
                .map(Value::Decimal)
                .ok_or_else(|| {
                    PureRuntimeError::EvaluationError(format!(
                        "toDecimal: cannot convert Float {f} to Decimal"
                    ))
                })
                .map_err(PureException::from),
            other => Err(PureException::from(PureRuntimeError::type_mismatch(
                "Number", other,
            ))),
        }?;
        Ok(Evaluated::new(v))
    }
}

/// Pure `parseInteger(String[1]):Integer[1]` — decimal string → `Integer`.
///
/// Returns an `EvaluationError` if the string is not a valid `i64`.
#[derive(Debug)]
pub struct ParseInteger;

impl NativeFunction for ParseInteger {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("parseInteger", args, ctx)?;
        let s = a0.as_string()?;
        let n = s.parse::<i64>().map_err(|e| {
            PureRuntimeError::EvaluationError(format!("parseInteger: cannot parse {s:?}: {e}"))
        })?;
        Ok(Evaluated::new(Value::Integer(n)))
    }
}

/// Pure `parseFloat(String[1]):Float[1]` — decimal string → `Float`.
///
/// Returns an `EvaluationError` if the string is not a valid `f64`.
#[derive(Debug)]
pub struct ParseFloat;

impl NativeFunction for ParseFloat {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("parseFloat", args, ctx)?;
        let s = a0.as_string()?;
        let f = s.parse::<f64>().map_err(|e| {
            PureRuntimeError::EvaluationError(format!("parseFloat: cannot parse {s:?}: {e}"))
        })?;
        Ok(Evaluated::new(Value::Float(f)))
    }
}

/// Pure `parseBoolean(String[1]):Boolean[1]` — case-insensitive
/// `"true"` / `"false"`.
///
/// Returns an `EvaluationError` for
/// any other input.
#[derive(Debug)]
pub struct ParseBoolean;

impl NativeFunction for ParseBoolean {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("parseBoolean", args, ctx)?;
        let s = a0.as_string()?;
        let lower = s.as_str().to_ascii_lowercase();
        let b = match lower.as_str() {
            "true" => Ok(true),
            "false" => Ok(false),
            _ => Err(PureRuntimeError::EvaluationError(format!(
                "parseBoolean: expected \"true\" or \"false\" (case-insensitive), got {:?}",
                s.as_str()
            ))),
        }?;
        Ok(Evaluated::new(Value::Boolean(b)))
    }
}

/// Register the fished mathematical natives under their mangled Pure FQNs.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("floor_Number_1__Integer_1_", Floor);
    registry.register("ceiling_Number_1__Integer_1_", Ceiling);
    registry.register("round_Number_1__Integer_1_", Round);
    registry.register("round_Decimal_1__Integer_1__Decimal_1_", Round);
    registry.register("round_Float_1__Integer_1__Float_1_", Round);
    registry.register("sign_Number_1__Integer_1_", Sign);

    registry.register("sqrt_Number_1__Float_1_", Sqrt);
    registry.register("cbrt_Number_1__Float_1_", Cbrt);
    registry.register("exp_Number_1__Float_1_", Exp);
    registry.register("log_Number_1__Float_1_", Log);
    registry.register("log10_Number_1__Float_1_", Log10);
    registry.register("pow_Number_1__Number_1__Number_1_", Pow);

    registry.register("sin_Number_1__Float_1_", Sin);
    registry.register("cos_Number_1__Float_1_", Cos);
    registry.register("tan_Number_1__Float_1_", Tan);
    registry.register("cot_Number_1__Float_1_", Cot);
    registry.register("asin_Number_1__Float_1_", Asin);
    registry.register("acos_Number_1__Float_1_", Acos);
    registry.register("atan_Number_1__Float_1_", Atan);
    registry.register("atan2_Number_1__Number_1__Float_1_", Atan2);

    registry.register("toFloat_Number_1__Float_1_", ToFloat);
    registry.register("toDecimal_Number_1__Decimal_1_", ToDecimal);

    registry.register("parseInteger_String_1__Integer_1_", ParseInteger);
    registry.register("parseFloat_String_1__Float_1_", ParseFloat);
    registry.register("parseBoolean_String_1__Boolean_1_", ParseBoolean);
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::str::FromStr;

    use crate::native::{MockCtx, lit_bool, lit_decimal, lit_float, lit_int, lit_str};

    fn approx_eq(a: f64, b: f64) -> bool {
        (a - b).abs() < 1e-10
    }

    /// Decimal-literal shim.
    fn ld(d: Decimal) -> legend_pure_metamodel::types::ValueSpec {
        lit_decimal(d)
    }

    #[test]
    fn floor_positive_float() {
        let r = Floor.execute(&[lit_float(3.7)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(3));
    }

    #[test]
    fn floor_negative_float() {
        let r = Floor.execute(&[lit_float(-3.2)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(-4));
    }

    #[test]
    fn floor_integer_passthrough() {
        let r = Floor.execute(&[lit_int(5)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(5));
    }

    #[test]
    fn floor_decimal() {
        let d = Decimal::from_str("4.9").unwrap();
        let r = Floor.execute(&[ld(d)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(4));
    }

    #[test]
    fn floor_wrong_arg_count() {
        assert!(Floor.execute(&[], &mut MockCtx).is_err());
        assert!(
            Floor
                .execute(&[lit_float(1.0), lit_float(2.0)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn floor_type_mismatch() {
        assert!(Floor.execute(&[lit_str("hi")], &mut MockCtx).is_err());
    }

    #[test]
    fn ceiling_positive_float() {
        let r = Ceiling.execute(&[lit_float(3.2)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(4));
    }

    #[test]
    fn ceiling_negative_float() {
        let r = Ceiling.execute(&[lit_float(-3.7)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Integer(-3));
    }

    #[test]
    fn ceiling_wrong_arg_count() {
        assert!(Ceiling.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn ceiling_type_mismatch() {
        assert!(Ceiling.execute(&[lit_bool(true)], &mut MockCtx).is_err());
    }

    #[test]
    fn round_half_to_even() {
        assert_eq!(
            Round
                .execute(&[lit_float(2.5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(2),
            "2.5 → 2 (round half to even)"
        );
        assert_eq!(
            Round
                .execute(&[lit_float(-2.5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-2),
            "-2.5 → -2 (round half to even)"
        );
        assert_eq!(
            Round
                .execute(&[lit_float(3.5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(4),
            "3.5 → 4 (round half to even)"
        );
        assert_eq!(
            Round
                .execute(&[lit_float(17.5)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(18),
            "17.5 → 18 (round half to even)"
        );
    }

    #[test]
    fn round_below_half() {
        assert_eq!(
            Round
                .execute(&[lit_float(2.4)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(2)
        );
    }

    #[test]
    fn round_wrong_arg_count() {
        assert!(Round.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn round_type_mismatch() {
        assert!(Round.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn sign_positive() {
        assert_eq!(
            Sign.execute(&[lit_int(7)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
    }

    #[test]
    fn sign_negative() {
        assert_eq!(
            Sign.execute(&[lit_float(-0.1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    #[test]
    fn sign_zero() {
        assert_eq!(
            Sign.execute(&[lit_int(0)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(0)
        );
    }

    #[test]
    fn sign_wrong_arg_count() {
        assert!(Sign.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn sign_type_mismatch() {
        assert!(Sign.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn sqrt_positive() {
        let r = Sqrt.execute(&[lit_int(16)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(4.0));
    }

    #[test]
    fn sqrt_zero() {
        let r = Sqrt.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(0.0));
    }

    #[test]
    fn sqrt_negative_throws_unable_to_compute() {
        let err = Sqrt.execute(&[lit_float(-1.0)], &mut MockCtx).unwrap_err();
        let msg = err.to_string();
        assert!(
            msg.contains("Unable to compute sqrt of -1.0"),
            "expected error containing 'Unable to compute sqrt of -1.0', got: {msg}"
        );
    }

    #[test]
    fn sqrt_wrong_arg_count() {
        assert!(Sqrt.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn sqrt_type_mismatch() {
        assert!(Sqrt.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn cbrt_positive() {
        let r = Cbrt.execute(&[lit_float(27.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 3.0), "expected ~3.0, got {f}"),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cbrt_negative() {
        let r = Cbrt.execute(&[lit_float(-8.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, -2.0), "expected ~-2.0, got {f}"),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cbrt_wrong_arg_count() {
        assert!(Cbrt.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn cbrt_type_mismatch() {
        assert!(Cbrt.execute(&[lit_bool(false)], &mut MockCtx).is_err());
    }

    #[test]
    fn exp_zero() {
        let r = Exp.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(1.0));
    }

    #[test]
    fn exp_one_is_e() {
        let r = Exp.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, std::f64::consts::E)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn exp_wrong_arg_count() {
        assert!(Exp.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn exp_type_mismatch() {
        assert!(Exp.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn log_one_is_zero() {
        let r = Log.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn log_e_is_one() {
        let r = Log
            .execute(&[lit_float(std::f64::consts::E)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn log_zero_is_neg_inf() {
        let r = Log.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(f.is_infinite() && f.is_sign_negative()),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn log_wrong_arg_count() {
        assert!(Log.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn log_type_mismatch() {
        assert!(Log.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn log10_100_is_two() {
        let r = Log10.execute(&[lit_float(100.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 2.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn log10_one_is_zero() {
        let r = Log10.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn log10_wrong_arg_count() {
        assert!(Log10.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn log10_type_mismatch() {
        assert!(Log10.execute(&[lit_bool(true)], &mut MockCtx).is_err());
    }

    #[test]
    fn pow_squares() {
        let r = Pow
            .execute(&[lit_float(3.0), lit_float(2.0)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 9.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn pow_integer_exponent() {
        let r = Pow
            .execute(&[lit_int(2), lit_int(10)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1024.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn pow_wrong_arg_count() {
        assert!(Pow.execute(&[lit_float(1.0)], &mut MockCtx).is_err());
    }

    #[test]
    fn pow_type_mismatch() {
        assert!(
            Pow.execute(&[lit_float(1.0), lit_str("x")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn sin_zero() {
        let r = Sin.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn sin_half_pi() {
        let r = Sin
            .execute(&[lit_float(std::f64::consts::FRAC_PI_2)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn sin_wrong_arg_count() {
        assert!(Sin.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn sin_type_mismatch() {
        assert!(Sin.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn cos_zero() {
        let r = Cos.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cos_pi() {
        let r = Cos
            .execute(&[lit_float(std::f64::consts::PI)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, -1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cos_wrong_arg_count() {
        assert!(Cos.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn cos_type_mismatch() {
        assert!(Cos.execute(&[lit_bool(true)], &mut MockCtx).is_err());
    }

    #[test]
    fn tan_zero() {
        let r = Tan.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn tan_pi_over_four() {
        let r = Tan
            .execute(&[lit_float(std::f64::consts::FRAC_PI_4)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn tan_wrong_arg_count() {
        assert!(Tan.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn tan_type_mismatch() {
        assert!(Tan.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn cot_pi_over_four() {
        let r = Cot
            .execute(&[lit_float(std::f64::consts::FRAC_PI_4)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 1.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cot_zero_is_infinite() {
        let r = Cot.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(f.is_infinite()),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn cot_wrong_arg_count() {
        assert!(Cot.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn cot_type_mismatch() {
        assert!(Cot.execute(&[lit_bool(true)], &mut MockCtx).is_err());
    }

    #[test]
    fn asin_zero() {
        let r = Asin.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn asin_one_is_half_pi() {
        let r = Asin.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, std::f64::consts::FRAC_PI_2)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn asin_out_of_range_throws_unable_to_compute() {
        let err = Asin.execute(&[lit_float(2.0)], &mut MockCtx).unwrap_err();
        let msg = err.to_string();
        assert!(
            msg.contains("Unable to compute asin of 2.0"),
            "expected error containing 'Unable to compute asin of 2.0', got: {msg}"
        );
    }

    #[test]
    fn asin_wrong_arg_count() {
        assert!(Asin.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn asin_type_mismatch() {
        assert!(Asin.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn acos_one_is_zero() {
        let r = Acos.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn acos_neg_one_is_pi() {
        let r = Acos.execute(&[lit_float(-1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, std::f64::consts::PI)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn acos_wrong_arg_count() {
        assert!(Acos.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn acos_type_mismatch() {
        assert!(Acos.execute(&[lit_bool(false)], &mut MockCtx).is_err());
    }

    #[test]
    fn atan_zero() {
        let r = Atan.execute(&[lit_float(0.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn atan_one_is_pi_over_four() {
        let r = Atan.execute(&[lit_float(1.0)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, std::f64::consts::FRAC_PI_4)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn atan_wrong_arg_count() {
        assert!(Atan.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn atan_type_mismatch() {
        assert!(Atan.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn atan2_quadrants() {
        let r = Atan2
            .execute(&[lit_float(1.0), lit_float(1.0)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, std::f64::consts::FRAC_PI_4)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn atan2_zero_zero_is_zero() {
        let r = Atan2
            .execute(&[lit_float(0.0), lit_float(0.0)], &mut MockCtx)
            .unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 0.0)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn atan2_wrong_arg_count() {
        assert!(Atan2.execute(&[lit_float(1.0)], &mut MockCtx).is_err());
    }

    #[test]
    fn atan2_type_mismatch() {
        assert!(
            Atan2
                .execute(&[lit_float(1.0), lit_str("x")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn to_float_from_integer() {
        let r = ToFloat.execute(&[lit_int(7)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(7.0));
    }

    #[test]
    fn to_float_from_float() {
        let r = ToFloat.execute(&[lit_float(2.5)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(2.5));
    }

    #[test]
    #[allow(clippy::approx_constant)]
    fn to_float_from_decimal() {
        let d = Decimal::from_str("3.14").unwrap();
        let r = ToFloat.execute(&[ld(d)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Float(f) => assert!(approx_eq(f, 3.14)),
            other => panic!("expected Float, got {other:?}"),
        }
    }

    #[test]
    fn to_float_wrong_arg_count() {
        assert!(ToFloat.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn to_float_type_mismatch() {
        assert!(ToFloat.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn to_decimal_from_integer() {
        let r = ToDecimal.execute(&[lit_int(5)], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Decimal(Decimal::from(5)));
    }

    #[test]
    fn to_decimal_from_float() {
        let r = ToDecimal.execute(&[lit_float(1.5)], &mut MockCtx).unwrap();
        match r.into_value() {
            Value::Decimal(d) => assert_eq!(d, Decimal::from_str("1.5").unwrap()),
            other => panic!("expected Decimal, got {other:?}"),
        }
    }

    #[test]
    fn to_decimal_nan_errors() {
        let r = ToDecimal.execute(&[lit_float(f64::NAN)], &mut MockCtx);
        assert!(r.is_err());
    }

    #[test]
    fn to_decimal_wrong_arg_count() {
        assert!(ToDecimal.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn to_decimal_type_mismatch() {
        assert!(ToDecimal.execute(&[lit_str("x")], &mut MockCtx).is_err());
    }

    #[test]
    fn parse_integer_positive() {
        let r = ParseInteger
            .execute(&[lit_str("123")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Integer(123));
    }

    #[test]
    fn parse_integer_negative() {
        let r = ParseInteger
            .execute(&[lit_str("-42")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Integer(-42));
    }

    #[test]
    fn parse_integer_invalid_errors() {
        assert!(
            ParseInteger
                .execute(&[lit_str("abc")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_integer_wrong_arg_count() {
        assert!(ParseInteger.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn parse_integer_type_mismatch() {
        assert!(ParseInteger.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    #[allow(clippy::approx_constant)]
    fn parse_float_decimal_form() {
        let r = ParseFloat
            .execute(&[lit_str("3.14")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Float(3.14));
    }

    #[test]
    fn parse_float_integer_form() {
        let r = ParseFloat.execute(&[lit_str("7")], &mut MockCtx).unwrap();
        assert_eq!(r.into_value(), Value::Float(7.0));
    }

    #[test]
    fn parse_float_invalid_errors() {
        assert!(
            ParseFloat
                .execute(&[lit_str("not-a-number")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_float_wrong_arg_count() {
        assert!(ParseFloat.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn parse_float_type_mismatch() {
        assert!(ParseFloat.execute(&[lit_float(1.0)], &mut MockCtx).is_err());
    }

    #[test]
    fn parse_boolean_true() {
        let r = ParseBoolean
            .execute(&[lit_str("true")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Boolean(true));
    }

    #[test]
    fn parse_boolean_false() {
        let r = ParseBoolean
            .execute(&[lit_str("false")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Boolean(false));
    }

    #[test]
    fn parse_boolean_case_insensitive() {
        for variant in ["True", "TRUE", "tRue", "trUe", "truE"] {
            let r = ParseBoolean
                .execute(&[lit_str(variant)], &mut MockCtx)
                .unwrap();
            assert_eq!(
                r.into_value(),
                Value::Boolean(true),
                "expected true for {variant}"
            );
        }
        for variant in ["False", "FALSE", "fAlse", "faLse", "falSe", "falsE"] {
            let r = ParseBoolean
                .execute(&[lit_str(variant)], &mut MockCtx)
                .unwrap();
            assert_eq!(
                r.into_value(),
                Value::Boolean(false),
                "expected false for {variant}"
            );
        }
    }

    #[test]
    fn parse_boolean_invalid_errors() {
        assert!(
            ParseBoolean
                .execute(&[lit_str("yes")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_boolean_wrong_arg_count() {
        assert!(ParseBoolean.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn parse_boolean_type_mismatch() {
        assert!(
            ParseBoolean
                .execute(&[lit_bool(true)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn java_number_string_matches_java_to_string() {
        assert_eq!(java_number_string(&Value::Integer(42)), "42");
        assert_eq!(java_number_string(&Value::Integer(-7)), "-7");
        assert_eq!(java_number_string(&Value::Float(2.0)), "2.0");
        assert_eq!(java_number_string(&Value::Float(-1.0)), "-1.0");
        assert_eq!(java_number_string(&Value::Float(2.5)), "2.5");
        assert_eq!(java_number_string(&Value::Float(f64::NAN)), "NaN");
        assert_eq!(java_number_string(&Value::Float(f64::INFINITY)), "Infinity");
        assert_eq!(
            java_number_string(&Value::Float(f64::NEG_INFINITY)),
            "-Infinity"
        );
        assert_eq!(
            java_number_string(&Value::Decimal(rust_decimal::Decimal::new(125, 2))),
            "1.25"
        );
    }
}
