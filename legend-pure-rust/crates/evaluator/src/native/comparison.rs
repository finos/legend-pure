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

//! Comparison natives fished so far: `eq` (identity) and `equal`
//! (structural, via [`crate::native::equality`]). The ordering natives
//! (`lessThan`, `compare`, …) stay on the archive until a test asks.

use legend_pure_metamodel::types::ValueSpec;

use crate::error::PureException;
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all,
};
use crate::value::Value;

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
        let values = force_all(args, ctx)?;
        expect_args("eq", &values, 2)?;
        Ok(Evaluated::new(Value::Boolean(values[0] == values[1])))
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

/// Register the fished comparison natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("eq_Any_1__Any_1__Boolean_1_", Eq);
    registry.register("equal_Any_MANY__Any_MANY__Boolean_1_", Equal);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_int, lit_str};

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
