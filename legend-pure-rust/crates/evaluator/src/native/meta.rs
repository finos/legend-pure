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

//! Heap shapes for reflection over functions and multiplicities.
//!
//! Only the two helpers the evaluator itself calls live here
//! (`MultiplicityReference` evaluation and `classifierGenericType` on a
//! function). The meta natives that used to surround them stay on the
//! archive until a PCT row asks for them (plan, Appendix A table 3).

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::Element;

use crate::error::PureException;
use crate::native::EvalContextTrait;
use crate::value::Value;

/// Allocate a `meta::pure::metamodel::type::FunctionType` heap wrapper
/// describing a `Value::Function`'s signature.
///
/// For lambdas: every declared parameter becomes a `VariableExpression`
/// child carrying `name` plus a `genericType` `GenericType` wrapping the
/// parameter's resolved type, with multiplicity reified as a sibling
/// `Multiplicity` wrapper. The lambda body's static return type isn't
/// known at this layer (no Pass-2.5 inference plumbed through to
/// runtime values); leave `returnType` / `returnMultiplicity` empty so
/// callers see Java's "unknown" shape rather than a wrong concrete one.
///
/// For compiled function references: pull `parameters`, `return_type`
/// and `return_multiplicity` straight off the `Function` compiled node
/// — these are exactly what the Java `Function.parameters`/`returnType`
/// reflective slots return.
#[allow(clippy::result_large_err)]
pub(crate) fn build_function_type_wrapper(
    ctx: &mut dyn EvalContextTrait,
    fv: &crate::value::FunctionValue,
) -> Result<crate::heap::ObjectHandle, PureException> {
    let func_type_obj = ctx.heap_mut().alloc_dynamic(crate::m3_paths::FUNCTION_TYPE);
    let (params, return_type, return_mult): (
        std::sync::Arc<[legend_pure_metamodel::types::Parameter]>,
        _,
        _,
    ) = match fv {
        crate::value::FunctionValue::Lambda(closure) => {
            (std::sync::Arc::clone(&closure.parameters), None, None)
        }
        crate::value::FunctionValue::Compiled(id) => {
            if let Element::Function(f) = ctx.model().get_element(*id) {
                (
                    std::sync::Arc::clone(&f.parameters),
                    Some(f.return_type.clone()),
                    Some(f.return_multiplicity.clone()),
                )
            } else {
                (std::sync::Arc::from(Vec::new()), None, None)
            }
        }
        // Path closures expose `Function<{U[1]→V[m]}>` shape but we
        // don't synthesise a parameter array here — the start type +
        // last step's return type would have to be re-derived from
        // the model. Reflective callers that need the full FunctionType
        // shape on a Path are not yet exercised; surface an empty
        // signature so this code path compiles cleanly.
        crate::value::FunctionValue::Path(_) => (std::sync::Arc::from(Vec::new()), None, None),
    };

    let mut param_objs: Vec<Value> = Vec::with_capacity(params.len());
    for p in params.iter() {
        let var_expr_obj = ctx
            .heap_mut()
            .alloc_dynamic(crate::m3_paths::VARIABLE_EXPRESSION);
        ctx.heap_mut()
            .mutate_add(&var_expr_obj, "name", &[Value::String(p.name.clone())])?;
        if let Some(type_id) = type_expr_to_element(&p.type_expr) {
            let p_gt = ctx.heap_mut().alloc_dynamic(crate::m3_paths::GENERIC_TYPE);
            ctx.heap_mut()
                .mutate_add(&p_gt, "rawType", &[Value::Element(type_id)])?;
            ctx.heap_mut()
                .mutate_add(&var_expr_obj, "genericType", &[Value::Object(p_gt)])?;
        }
        let mult_obj = build_multiplicity_wrapper(ctx.heap_mut(), &p.multiplicity)?;
        ctx.heap_mut()
            .mutate_add(&var_expr_obj, "multiplicity", &[Value::Object(mult_obj)])?;
        param_objs.push(Value::Object(var_expr_obj));
    }
    if !param_objs.is_empty() {
        ctx.heap_mut()
            .mutate_add(&func_type_obj, "parameters", &param_objs)?;
    }

    if let Some(rt) = return_type
        && let Some(rt_id) = type_expr_to_element(&rt)
    {
        let rt_gt = ctx.heap_mut().alloc_dynamic(crate::m3_paths::GENERIC_TYPE);
        ctx.heap_mut()
            .mutate_add(&rt_gt, "rawType", &[Value::Element(rt_id)])?;
        ctx.heap_mut()
            .mutate_add(&func_type_obj, "returnType", &[Value::Object(rt_gt)])?;
    }
    if let Some(rm) = return_mult {
        let rm_obj = build_multiplicity_wrapper(ctx.heap_mut(), &rm)?;
        ctx.heap_mut().mutate_add(
            &func_type_obj,
            "returnMultiplicity",
            &[Value::Object(rm_obj)],
        )?;
    }
    Ok(func_type_obj)
}

/// Best-effort `TypeExpr → ElementId`. Generic / parametric type slots
/// have no element to point at; leave the caller to handle by skipping
/// the `rawType` slot rather than fabricating one.
fn type_expr_to_element(ty: &legend_pure_metamodel::types::TypeExpr) -> Option<ElementId> {
    match ty {
        legend_pure_metamodel::types::TypeExpr::Named { element, .. } => Some(*element),
        _ => None,
    }
}

/// Allocate a `meta::pure::metamodel::multiplicity::Multiplicity` heap
/// wrapper carrying `lowerBound` / `upperBound` integer slots. Mirrors
/// the subset of `Multiplicity.<lower|upper>Bound` Pure code typically
/// reads off lambda parameter / return signatures; richer fields
/// (`name` for variable multiplicities, etc.) are not yet exposed.
#[allow(clippy::result_large_err)]
pub(crate) fn build_multiplicity_wrapper(
    heap: &mut crate::heap::RuntimeHeap,
    m: &legend_pure_metamodel::types::Multiplicity,
) -> Result<crate::heap::ObjectHandle, PureException> {
    use legend_pure_metamodel::types::Multiplicity as M;
    let (lower, upper): (i64, Option<i64>) = match m {
        M::PureOne => (1, Some(1)),
        M::ZeroOrOne => (0, Some(1)),
        M::OneOrMany => (1, None),
        M::Range { lower, upper } => (i64::from(*lower), upper.map(i64::from)),
        // ZeroOrMany and unbound variables: unbounded, zero or more
        M::ZeroOrMany | M::Variable(_) => (0, None),
    };
    let obj = heap.alloc_dynamic(crate::m3_paths::MULTIPLICITY);
    let lower_value = heap.alloc_dynamic(crate::m3_paths::MULTIPLICITY_VALUE);
    heap.mutate_add(&lower_value, "value", &[Value::Integer(lower)])?;
    heap.mutate_add(&obj, "lowerBound", &[Value::Object(lower_value)])?;
    if let Some(u) = upper {
        let upper_value = heap.alloc_dynamic(crate::m3_paths::MULTIPLICITY_VALUE);
        heap.mutate_add(&upper_value, "value", &[Value::Integer(u)])?;
        heap.mutate_add(&obj, "upperBound", &[Value::Object(upper_value)])?;
    }
    Ok(obj)
}
