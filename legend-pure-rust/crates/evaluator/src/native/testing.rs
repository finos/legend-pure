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

//! Test-support natives: `assert`.

use legend_pure_metamodel::types::ValueSpec;

use crate::error::{PureException, PureRuntimeError};
use crate::native::{EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args};
use crate::value::Value;

/// Pure `assert(Boolean[1], Function<{->String[1]}>[1]): Boolean[1]`
///
/// Asserts a boolean condition. If false, evaluates the message expression
/// and returns an error with that message. The message is only evaluated
/// on failure.
#[derive(Debug)]
pub struct Assert;

impl NativeFunction for Assert {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("assert", args, 2)?;
        let condition = ctx.evaluate(&args[0])?.as_boolean()?;
        if condition {
            Ok(Evaluated::new(Value::Boolean(true)))
        } else {
            let msg_val = crate::native::force_thunk(&args[1], ctx)?.into_value();
            let msg = msg_val.as_string().map_or_else(
                |_| "Assertion failed".to_string(),
                smol_str::SmolStr::to_string,
            );
            Err(PureRuntimeError::AssertionFailed(msg).into())
        }
    }
}

/// Register the fished test-support natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("assert_Boolean_1__Function_1__Boolean_1_", Assert);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_bool, lit_str};

    #[test]
    fn assert_true_returns_true() {
        let r = Assert
            .execute(&[lit_bool(true), lit_str("unused")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::Boolean(true));
    }

    #[test]
    fn assert_false_fails_with_the_message() {
        let err = Assert
            .execute(&[lit_bool(false), lit_str("boom")], &mut MockCtx)
            .unwrap_err();
        assert!(err.is_assertion(), "{err:?}");
        assert!(err.to_string().contains("boom"), "{err}");
    }
}
