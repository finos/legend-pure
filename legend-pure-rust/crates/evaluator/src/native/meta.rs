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

//! Meta-model natives and the heap shapes for reflection over functions
//! and multiplicities.
//!
//! Two helpers the evaluator itself calls live here
//! (`MultiplicityReference` evaluation and `classifierGenericType` on a
//! function), next to the meta natives: `match`,
//! `extractEnumValue`, `cast`.

use legend_pure_metamodel::bootstrap;
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::types::{ConstValue, ExprKind, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use crate::date::DatePrecision;
use crate::error::{PureException, PureRuntimeError};
use crate::heap::RuntimeHeap;
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, NotYet, expect_args, force_all,
};
use crate::value::{FunctionValue, Value};

/// Allocate a `meta::pure::metamodel::type::FunctionType` heap wrapper
/// describing a `Value::Function`'s signature.
///
/// For lambdas: every declared parameter becomes a `VariableExpression`
/// child carrying `name` plus a `genericType` `GenericType` wrapping the
/// parameter's resolved type, with multiplicity reified as a sibling
/// `Multiplicity` wrapper.
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
/// have no element to point at.
fn type_expr_to_element(ty: &legend_pure_metamodel::types::TypeExpr) -> Option<ElementId> {
    match ty {
        legend_pure_metamodel::types::TypeExpr::Named { element, .. } => Some(*element),
        _ => None,
    }
}

/// Allocate a `meta::pure::metamodel::multiplicity::Multiplicity` heap
/// wrapper carrying `lowerBound` / `upperBound` integer slots.
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

/// Pure `match<T|m,n>(var:Any[*], functions:Function<{Nil[n]->T[m]}>[1..*]):T[m]`
///
/// Type-based pattern matching: finds the first lambda in `functions` whose
/// first parameter's declared type is compatible with `var`, then invokes it.
#[derive(Debug)]
pub struct Match;

impl NativeFunction for Match {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        let (subject, functions, extra) = match values.len() {
            2 => (values[0].clone(), values[1].to_collection(), None),
            3 => (
                values[0].clone(),
                values[1].to_collection(),
                Some(values[2].clone()),
            ),
            n => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "match: expected 2 or 3 argument(s), got {n}"
                ))
                .into());
            }
        };

        let elements: Vec<Value> = match &subject {
            Value::Collection(c) => c.iter().cloned().collect(),
            Value::Unit => Vec::new(),
            other => vec![other.clone()],
        };
        #[allow(clippy::cast_possible_truncation)]
        let subject_count = elements.len() as u32;

        for func_val in &functions {
            let Value::Function(fv) = func_val else {
                continue;
            };
            let FunctionValue::Lambda(lambda) = fv.as_ref() else {
                continue;
            };

            if lambda.parameters.is_empty() {
                let extras: Vec<Value> = extra.iter().cloned().collect();
                return ctx.call_function(func_val, &extras).map(Evaluated::new);
            }

            let param = &lambda.parameters[0];
            let (lower, upper) = mult_bounds(&param.multiplicity);
            if subject_count < lower || upper.is_some_and(|u| subject_count > u) {
                continue;
            }
            let type_ok = subject_count == 0
                || match &param.type_expr {
                    TypeExpr::Named {
                        element: type_class_id,
                        ..
                    } => elements
                        .iter()
                        .all(|e| value_matches_type(ctx.model(), e, *type_class_id, ctx.heap())),
                    TypeExpr::Generic(_) => true,
                    _ => false,
                };
            if !type_ok {
                continue;
            }
            let mut call_args = vec![subject.clone()];
            if let Some(extra_v) = &extra {
                call_args.push(extra_v.clone());
            }
            return ctx.call_function(func_val, &call_args).map(Evaluated::new);
        }

        Err(PureRuntimeError::EvaluationError(format!(
            "match: no branch matched for value: {subject}"
        ))
        .into())
    }
}

/// Decode a [`Multiplicity`](legend_pure_metamodel::types::Multiplicity)
/// into concrete `[lower, upper]` bounds.
/// `Variable(_)` maps to `[0, *]`.
fn mult_bounds(m: &legend_pure_metamodel::types::Multiplicity) -> (u32, Option<u32>) {
    use legend_pure_metamodel::types::Multiplicity as M;
    match m {
        M::PureOne => (1, Some(1)),
        M::ZeroOrOne => (0, Some(1)),
        M::ZeroOrMany | M::Variable(_) => (0, None),
        M::OneOrMany => (1, None),
        M::Range { lower, upper } => (*lower, *upper),
    }
}

/// Is `value` an instance of `type_id`: the value's runtime type, as a
/// model element, extends the target. Both sides are element ids.
fn value_matches_type(
    model: &PureModel,
    value: &Value,
    type_id: ElementId,
    heap: &RuntimeHeap,
) -> bool {
    if type_id == bootstrap::ANY_ID {
        return true;
    }
    if type_id == bootstrap::NIL_ID {
        return false;
    }
    match resolve_value_type(value, model, heap) {
        Ok(value_type) => type_extends(model, value_type, type_id),
        Err(_) => false,
    }
}

/// True when `descendant` extends (transitively) `ancestor` — or is `ancestor`.
///
/// Walks `Class::super_types` and `PrimitiveType::super_type` upward.
fn type_extends(model: &PureModel, descendant: ElementId, ancestor: ElementId) -> bool {
    if descendant == ancestor {
        return true;
    }
    let mut stack: Vec<ElementId> = vec![descendant];
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    while let Some(id) = stack.pop() {
        if !visited.insert(id) {
            continue;
        }
        if id == ancestor {
            return true;
        }
        match model.get_element(id) {
            Element::Class(c) => {
                for st in &c.super_types {
                    if let TypeExpr::Named { element, .. } = st {
                        stack.push(*element);
                    }
                }
            }
            Element::PrimitiveType(p) => {
                if let Some(super_id) = p.super_type {
                    stack.push(super_id);
                }
            }
            Element::Enumeration(_) => {
                if let Some(enum_class) = crate::m3_paths::resolve(model, crate::m3_paths::ENUM) {
                    stack.push(enum_class);
                }
            }
            _ => {}
        }
    }
    false
}

/// Resolve the metamodel [`ElementId`] for a runtime [`Value`]'s type.
///
/// Primitives map to their bootstrap IDs. Heap objects look up their
/// classifier's FQN — the stored path is split on `"::"` and resolved via
/// the model's package tree. Element refs report their metaclass.
/// `Value::Unit` and maps fall back to `Any` (bootstrap top type).
fn resolve_value_type(
    value: &Value,
    model: &PureModel,
    heap: &RuntimeHeap,
) -> Result<ElementId, PureException> {
    match value {
        Value::Boolean(_) => Ok(bootstrap::BOOLEAN_ID),
        Value::Integer(_) => Ok(bootstrap::INTEGER_ID),
        Value::Float(_) => Ok(bootstrap::FLOAT_ID),
        Value::Decimal(_) => Ok(bootstrap::DECIMAL_ID),
        Value::String(_) => Ok(bootstrap::STRING_ID),
        Value::EnumValue { enum_id, .. } => Ok(*enum_id),
        Value::Date(d) => Ok(match d.precision() {
            DatePrecision::Day => bootstrap::STRICT_DATE_ID,
            DatePrecision::Time(_) => bootstrap::DATE_TIME_ID,
            _ => bootstrap::DATE_ID,
        }),
        Value::Latest => Ok(bootstrap::DATE_ID),
        Value::StrictTime(_) => Ok(bootstrap::STRICT_TIME_ID),
        Value::Element(id) => {
            if let Some(meta_id) = bootstrap::metatype_of(model, model.get_element(*id)) {
                return Ok(meta_id);
            }
            Ok(*id)
        }
        Value::Object(obj_id) => {
            let classifier = heap.classifier(obj_id)?;
            model.resolve_fqn_str(classifier.as_str()).ok_or_else(|| {
                PureRuntimeError::EvaluationError(format!(
                    "type: classifier '{classifier}' does not resolve to a known Type"
                ))
                .into()
            })
        }
        Value::UnitInstance { unit_id, .. } => Ok(*unit_id),
        Value::Function(fv) => match fv.as_ref() {
            FunctionValue::Lambda(_) => {
                crate::m3_paths::resolve(model, crate::m3_paths::LAMBDA_FUNCTION).ok_or_else(|| {
                    PureRuntimeError::EvaluationError(
                        "type: meta::pure::metamodel::function::LambdaFunction not in model".into(),
                    )
                    .into()
                })
            }
            FunctionValue::Compiled(eid) => {
                let element = model.get_element(*eid);
                Ok(bootstrap::metatype_of(model, element).unwrap_or(bootstrap::ANY_ID))
            }
            FunctionValue::Path(_) => model
                .resolve_by_path(&[
                    smol_str::SmolStr::new("meta"),
                    smol_str::SmolStr::new("pure"),
                    smol_str::SmolStr::new("metamodel"),
                    smol_str::SmolStr::new("path"),
                    smol_str::SmolStr::new("Path"),
                ])
                .ok_or_else(|| {
                    PureRuntimeError::EvaluationError(
                        "type: meta::pure::metamodel::path::Path not in model".into(),
                    )
                    .into()
                }),
        },
        Value::Collection(items) => {
            let mut iter = items.iter();
            let Some(first) = iter.next() else {
                return Ok(bootstrap::ANY_ID);
            };
            let acc = resolve_value_type(first, model, heap)?;
            for item in iter {
                if acc == bootstrap::ANY_ID {
                    break;
                }
                let t = resolve_value_type(item, model, heap)?;
                if t != acc {
                    return Err(PureRuntimeError::Unsupported(
                        "runtime type of a mixed collection (least upper bound not fished)".into(),
                    )
                    .into());
                }
            }
            Ok(acc)
        }
        Value::Map(_) | Value::Unit => Ok(bootstrap::ANY_ID),
    }
}

/// Pure `cast<V|m>(p:Any[m], typ:V[1]):V[m]`
///
/// Runtime type coercion — returns the value unchanged if it is an instance
/// of the target type (or the target refines the value's type), otherwise
/// raises `Cast exception: X cannot be cast to Y`. Constraints declared on
/// a primitive target (and its parametric primitive ancestors) are checked
/// against every element.
#[derive(Debug)]
pub struct Cast;

impl NativeFunction for Cast {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("cast", &values, 2)?;
        let subject = values[0].clone();

        if matches!(values[1], Value::Object(_)) {
            return Err(
                PureRuntimeError::Unsupported("cast to a relation type `@(cols)`".into()).into(),
            );
        }

        let type_id = as_element_id(&values[1])?;
        let type_name = ctx.model().element_name(type_id).to_string();
        let type_value_args: Vec<ConstValue> = if args.len() > 1 {
            match &*args[1].kind {
                ExprKind::TypeReference {
                    type_expr:
                        TypeExpr::Named {
                            value_arguments, ..
                        },
                } => value_arguments.clone(),
                _ => Vec::new(),
            }
        } else {
            Vec::new()
        };
        let mk_err = |v: &Value| {
            PureException::from(PureRuntimeError::EvaluationError(format!(
                "Cast exception: {} cannot be cast to {type_name}",
                v.type_name()
            )))
        };
        let items: Vec<Value> = match &subject {
            Value::Collection(coll) => coll.iter().cloned().collect(),
            Value::Unit => return Ok(Evaluated::new(Value::Unit)),
            other => vec![other.clone()],
        };
        for v in &items {
            if !cast_compatible(v, type_id, ctx.model(), ctx.heap()) {
                return Err(mk_err(v));
            }
        }
        for v in &items {
            evaluate_primitive_constraints(ctx, type_id, v, &type_value_args)?;
        }
        Ok(Evaluated::new(subject))
    }
}

/// Run every constraint declared on the target primitive type (and its
/// ancestor parametric primitives) against `subject`. Raises
/// [`ConstraintViolation`](crate::error::PureExceptionKind::ConstraintViolation)
/// on the first failure, matching Java Pure's message shape
/// `"Constraint :[<id>] violated in the Class <name>[, Message: <msg>]"`.
///
/// Scope bindings for each constraint body:
/// - `$this` → the value being cast
/// - each `type_variable_parameter` at this level → the matching
///   positional `value_args` entry (either the caller's `@P(8)` bindings
///   or the child's `extends OP(8)` bindings when walking up).
///
/// Non-primitive targets and primitives with no constraints in their
/// inheritance chain are a no-op.
fn evaluate_primitive_constraints(
    ctx: &mut dyn EvalContextTrait,
    type_id: ElementId,
    subject: &Value,
    type_value_args: &[ConstValue],
) -> Result<(), PureException> {
    let mut chain: Vec<(ElementId, Vec<ConstValue>)> = Vec::new();
    {
        let mut current = Some((type_id, type_value_args.to_vec()));
        let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
        while let Some((id, args)) = current {
            if !visited.insert(id) {
                break;
            }
            match ctx.model().get_element(id) {
                Element::PrimitiveType(prim) => {
                    let next_args = prim.super_type_value_arguments.clone();
                    current = prim.super_type.map(|s| (s, next_args));
                }
                _ => current = None,
            }
            chain.push((id, args));
        }
    }
    for (level_id, level_args) in chain {
        let (constraints, param_names, prim_name) = match ctx.model().get_element(level_id) {
            Element::PrimitiveType(prim) if !prim.constraints.is_empty() => {
                let names: Vec<SmolStr> = prim
                    .type_variable_parameters
                    .iter()
                    .map(|p| p.name.clone())
                    .collect();
                (
                    prim.constraints.clone(),
                    names,
                    ctx.model().element_name(level_id).clone(),
                )
            }
            _ => continue,
        };
        ctx.context_mut().push_scope();
        ctx.context_mut()
            .set(SmolStr::new_static("this"), subject.clone());
        for (i, name) in param_names.iter().enumerate() {
            if let Some(cv) = level_args.get(i) {
                let v = match cv {
                    ConstValue::Integer(n) => Value::Integer(*n),
                    ConstValue::String(s) => Value::String(SmolStr::new(s)),
                };
                ctx.context_mut().set(name.clone(), v);
            }
        }
        let mut first_failure: Option<PureException> = None;
        for (idx, constraint) in constraints.iter().enumerate() {
            match ctx.evaluate(&constraint.function) {
                Ok(eval) => {
                    if matches!(eval.into_value(), Value::Boolean(true)) {
                        continue;
                    }
                }
                Err(e) => {
                    first_failure = Some(e);
                    break;
                }
            }
            let message = match constraint.message.as_ref().map(|m| ctx.evaluate(m)) {
                Some(Ok(eval)) => match &eval.into_value() {
                    Value::String(s) => Some(s.to_string()),
                    _ => None,
                },
                Some(Err(e)) => {
                    first_failure = Some(e);
                    break;
                }
                None => None,
            };
            let constraint_id = constraint
                .name
                .clone()
                .unwrap_or_else(|| SmolStr::new(idx.to_string()));
            first_failure = Some(PureException::constraint(
                constraint_id,
                crate::error::ConstraintKind::Class,
                prim_name.clone(),
                message,
                constraint.source_info.clone(),
                Vec::new(),
            ));
            break;
        }
        ctx.context_mut().pop_scope();
        if let Some(e) = first_failure {
            return Err(e);
        }
    }
    Ok(())
}

/// Cast-direction type check — succeeds in either direction of the lattice.
///
/// A cast succeeds when:
/// - the value already matches the target (`value_matches_type`, which also
///   covers primitives and the instanceOf-direction subtype walk); **or**
/// - the target extends the value's type — Pure's "primitive extension
///   downcast" idiom (`1->cast(@P8)` where `P8 extends Integer`). The target
///   refines a supertype, and the runtime trusts the source of the cast.
fn cast_compatible(
    value: &Value,
    target_id: ElementId,
    model: &PureModel,
    heap: &RuntimeHeap,
) -> bool {
    if value_matches_type(model, value, target_id, heap) {
        return true;
    }
    let Ok(value_type_id) = resolve_value_type(value, model, heap) else {
        return false;
    };
    type_extends(model, target_id, value_type_id)
}

/// Pure `extractEnumValue<T>(enumeration:Enumeration<T>[1], value:String[1]):T[1]`
///
/// Returns `Value::EnumValue { enum_id, member }`.
#[derive(Debug)]
pub struct ExtractEnumValue;

impl NativeFunction for ExtractEnumValue {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("extractEnumValue", &values, 2)?;
        let enum_id = as_element_id(&values[0])?;
        let name = values[1].as_string()?;
        let Element::Enumeration(enum_def) = ctx.model().get_element(enum_id) else {
            return Err(PureRuntimeError::EvaluationError(
                "extractEnumValue: first argument must reference an Enumeration".into(),
            )
            .into());
        };
        if !enum_def.values.iter().any(|v| v.name == *name) {
            return Err(PureRuntimeError::EvaluationError(format!(
                "extractEnumValue: enumeration has no value '{name}'"
            ))
            .into());
        }
        Ok(Evaluated::new(Value::EnumValue {
            enum_id,
            member: SmolStr::new(name),
        }))
    }
}

/// Extract an `ElementId` from a [`Value::Element`].
fn as_element_id(v: &Value) -> Result<ElementId, PureException> {
    match v {
        Value::Element(id) => Ok(*id),
        other => Err(PureRuntimeError::type_mismatch("PackageableElement", other).into()),
    }
}

/// Register the fished meta natives under their platform keys.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("match_Any_MANY__Function_$1_MANY$__T_m_", Match);
    registry.register("match_Any_MANY__Function_$1_MANY$__P_o__T_m_", Match);
    registry.register(
        "extractEnumValue_Enumeration_1__String_1__T_1_",
        ExtractEnumValue,
    );
    registry.register("cast_Any_m__T_1__T_m_", Cast);
    registry.register(
        "genericType_Any_MANY__GenericType_1_",
        NotYet("genericType_Any_MANY__GenericType_1_"),
    );
    registry.register(
        "deactivate_Any_MANY__ValueSpecification_1_",
        NotYet("deactivate_Any_MANY__ValueSpecification_1_"),
    );
}
