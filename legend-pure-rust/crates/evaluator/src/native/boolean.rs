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

//! Boolean native functions: `and`, `or`, `not`.
//!
//! `and` and `or` are the canonical **short-circuit** exemplars. They force
//! the left operand through `ctx.evaluate`, inspect the boolean outcome,
//! and skip the right operand entirely when the result is already decided.
//! This matches Pure's `&&` / `||` semantics.

use legend_pure_metamodel::types::ValueSpec;

use crate::error::PureException;
use crate::native::{
    ArgMode, Control, EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args,
};
use crate::native::{Arity, Intrinsic, force_then_values};
use crate::value::Value;

/// Pure `and(Boolean[1], Boolean[1]): Boolean[1]` — short-circuit.
///
/// When the left operand is `false`, the right operand is never forced.
#[derive(Debug)]
pub struct And;

impl NativeFunction for And {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Control(Control::And)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("and", args, 2)?;
        let lhs = ctx.evaluate(&args[0])?.as_boolean()?;
        if !lhs {
            return Ok(Evaluated::new(Value::Boolean(false)));
        }
        let rhs = ctx.evaluate(&args[1])?.as_boolean()?;
        Ok(Evaluated::new(Value::Boolean(rhs)))
    }
}

/// Pure `or(Boolean[1], Boolean[1]): Boolean[1]` — short-circuit.
///
/// Mirrors [`And`] with `true`-short-circuit semantics.
#[derive(Debug)]
pub struct Or;

impl NativeFunction for Or {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Control(Control::Or)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("or", args, 2)?;
        let lhs = ctx.evaluate(&args[0])?.as_boolean()?;
        if lhs {
            return Ok(Evaluated::new(Value::Boolean(true)));
        }
        let rhs = ctx.evaluate(&args[1])?.as_boolean()?;
        Ok(Evaluated::new(Value::Boolean(rhs)))
    }
}

/// Pure `not(Boolean[1]): Boolean[1]`
#[derive(Debug)]
pub struct Not;

impl NativeFunction for Not {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn intrinsic(&self) -> Option<Intrinsic> {
        Some(Intrinsic::Not)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "not", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("not", args, 1)?;
        let a = args[0].as_boolean()?;
        Ok(Evaluated::new(Value::Boolean(!a)))
    }
}

/// Register all boolean native functions.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("and_Boolean_1__Boolean_1__Boolean_1_", And);
    registry.register("or_Boolean_1__Boolean_1__Boolean_1_", Or);
    registry.register("not_Boolean_1__Boolean_1_", Not);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_bool, lit_int, lit_str};

    #[test]
    fn and_true_true() {
        assert_eq!(
            And.execute(&[lit_bool(true), lit_bool(true)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn and_true_false() {
        assert_eq!(
            And.execute(&[lit_bool(true), lit_bool(false)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn or_false_true() {
        assert_eq!(
            Or.execute(&[lit_bool(false), lit_bool(true)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn not_true() {
        assert_eq!(
            Not.execute(&[lit_bool(true)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn type_error_on_non_boolean() {
        assert!(
            And.execute(&[lit_int(1), lit_bool(true)], &mut MockCtx)
                .is_err()
        );
        assert!(
            And.execute(&[lit_bool(true), lit_str("true")], &mut MockCtx)
                .is_err()
        );
        assert_eq!(
            Or.execute(&[lit_bool(true), lit_str("true")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
        assert!(Not.execute(&[lit_int(0)], &mut MockCtx).is_err());
    }

    #[test]
    fn short_circuit_skips_rhs() {
        assert_eq!(
            And.execute(&[lit_bool(false), lit_str("oops")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
        assert_eq!(
            Or.execute(&[lit_bool(true), lit_str("oops")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn wrong_arg_count_errors() {
        assert!(And.execute(&[lit_bool(true)], &mut MockCtx).is_err());
        assert!(Not.execute(&[], &mut MockCtx).is_err());
    }
}
