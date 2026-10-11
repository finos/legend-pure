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

//! Language-level natives: `letFunction`, `if`, `eval`,
//! `new`, `copy`.
//!
//! `new` is split across two natives by call shape: `New` handles the
//! compiler-emitted `^Class<T>(prop=val, …)` payload (and the two-argument
//! platform form), `NewWithKeyExpressions` the platform-declared
//! `new(class, id, [^KeyExpression(...)])`. Both share the construction
//! helpers below (defaults, required-property validation, association
//! inverses, class constraints).

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{ExprKind, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use crate::error::{PureException, PureRuntimeError};
use crate::heap::{ObjectHandle, RuntimeHeap};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all, force1,
    force2,
};
use crate::value::Value;

/// Pure `letFunction(String[1], T[m]): T[m]`
///
/// The compiler desugars `let x = expr` → `FunctionCall("letFunction", [name, value])`.
/// This native binds the value to the variable name in the current scope
/// and returns the value.
#[derive(Debug)]
pub struct LetFunction;

impl NativeFunction for LetFunction {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("letFunction", &values, 2)?;
        let name = values[0].as_string()?;
        let value = values[1].clone();
        ctx.context_mut().set(name.clone(), value.clone());
        Ok(Evaluated::new(value))
    }
}

/// Pure `if(Boolean[1], Function<{->T[m]}>[1], Function<{->T[m]}>[1]): T[m]`
///
/// Short-circuiting conditional. Forces each argument on demand so only the
/// taken branch is evaluated. The condition and branches are raw `ValueSpec`s
/// — the native calls `ctx.evaluate` directly in the right order.
#[derive(Debug)]
pub struct If;

impl NativeFunction for If {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("if", args, 3)?;
        let cond = crate::native::force_thunk(&args[0], ctx)?.as_boolean()?;
        let branch = if cond { &args[1] } else { &args[2] };
        crate::native::force_thunk(branch, ctx)
    }
}

/// Pure `eval(func:Function<{...->V[m]}>[1], ...params):V[m]`
///
/// All 8 platform `eval` overloads (0–7 extra parameters) share this single
/// implementation: extract `args[0]` as the callable and pass `args[1..]` as
/// the arguments via `ctx.call_function`.
#[derive(Debug)]
pub struct Eval;

impl NativeFunction for Eval {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        if args.is_empty() {
            return Err(PureRuntimeError::EvaluationError(
                "eval: expected at least 1 argument (the function)".into(),
            )
            .into());
        }
        let values = force_all(args, ctx)?;
        let func = &values[0];
        let params = &values[1..];
        Ok(Evaluated::new(ctx.call_function(func, params)?))
    }
}

/// Pure `new<T>(class:Class<T>[1], id:String[1], keyExpressions:KeyExpression[*]):T[1]`
///
/// Constructs a runtime instance of the given class and seeds its properties.
///
/// The compiler's `^Class(prop1=val1, prop2=val2)` desugaring calls this
/// native with flattened arguments:
///
/// ```text
/// new(<Element: class>, "ClassName", "prop1", val1, "prop2", val2, ...)
/// ```
///
/// The second argument (class simple name) is ignored.
/// Extra property assignments
/// after the two fixed positionals are read as flat key/value pairs.
#[derive(Debug)]
pub struct New;

impl NativeFunction for New {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let _frame = ConstructionFrame::enter();
        let values = force_all(args, ctx)?;
        if values.len() < 2 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "new: expected at least 2 arguments (class, id), got {}",
                values.len()
            ))
            .into());
        }
        let class_id = match &values[0] {
            Value::Element(id) => *id,
            other => {
                return Err(PureRuntimeError::type_mismatch("Class", other).into());
            }
        };
        let _id = &values[1];
        let class_metatype_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::CLASS);
        let mut type_args: Vec<Value> = Vec::new();
        let mut type_var_values: Vec<Value> = Vec::new();
        let mut kvs_offset = 2;
        if values.len() >= 3 {
            match &values[2] {
                Value::Element(_) => type_args = vec![values[2].clone()],
                Value::Collection(coll) => {
                    type_args = coll
                        .iter()
                        .filter(|v| matches!(v, Value::Element(_)))
                        .cloned()
                        .collect();
                }
                _ => {}
            }
            if values.len() >= 4 {
                match &values[3] {
                    Value::Unit => {}
                    Value::Collection(coll) => {
                        type_var_values = coll.iter().cloned().collect();
                    }
                    other => type_var_values = vec![other.clone()],
                }
                kvs_offset = 4;
            } else {
                kvs_offset = 3;
            }
        }
        if type_args.is_empty() {
            type_args = back_fill_type_args(args, class_metatype_id);
        }
        let triples: Vec<(SmolStr, Vec<Value>, bool)> =
            triples_from_flat_kv_stream(&values[kvs_offset..])?;
        let result = finish_construction(
            ctx,
            class_id,
            &type_args,
            &type_var_values,
            &triples,
            Some(&values[kvs_offset..]),
        )?;
        ConstructionFrame::drain_if_outermost(ctx)?;
        Ok(result)
    }
}

/// Pure
/// `new<T>(class:Class<T>[1], id:String[1], keyExpressions:KeyExpression[*]):T[1]`
///
/// The platform-declared overload that takes a list of M3
/// `KeyExpression` heap objects (each carrying `key:InstanceValue` and
/// `expression:InstanceValue`). Distinct from the compiler-internal
/// `New` native — the compiler's `^Class(prop=val)` lowering still
/// targets the flat-triple shape (see `New`); this native handles
/// Pure-source `new(class, '', [^KeyExpression(...)])` calls.
#[derive(Debug)]
pub struct NewWithKeyExpressions;

impl NativeFunction for NewWithKeyExpressions {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let _frame = ConstructionFrame::enter();
        let values = force_all(args, ctx)?;
        if values.len() < 2 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "new(class, id, [keyExpressions]): expected at least 2 arguments, got {}",
                values.len()
            ))
            .into());
        }
        let class_id = match &values[0] {
            Value::Element(id) => *id,
            other => return Err(PureRuntimeError::type_mismatch("Class", other).into()),
        };
        let _id = &values[1];
        let class_metatype_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::CLASS);
        let type_args = back_fill_type_args(args, class_metatype_id);
        let key_expr_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::KEY_EXPRESSION);
        let key_expr_payload: Vec<&Value> = if values.len() >= 3 {
            match &values[2] {
                Value::Collection(coll) => coll.iter().collect(),
                Value::Unit => Vec::new(),
                single => vec![single],
            }
        } else {
            Vec::new()
        };
        let triples = decode_key_expressions(ctx, key_expr_id, &key_expr_payload)?;
        let result = finish_construction(ctx, class_id, &type_args, &Vec::new(), &triples, None)?;
        ConstructionFrame::drain_if_outermost(ctx)?;
        Ok(result)
    }
}

/// Read parametric type-args off `args[0].type_info` when the inferred
/// type is `Class<X<...>>` and return the inner `<...>` as a list of
/// `Value::Element`. Identity-based: the outer element must match the
/// resolved `m3_paths::CLASS` id. Empty when args is empty, `type_info`
/// is missing, or the shape doesn't match.
fn back_fill_type_args(args: &[ValueSpec], class_metatype_id: Option<ElementId>) -> Vec<Value> {
    let mut type_args = Vec::new();
    if !args.is_empty()
        && let Some(ti) = args[0].type_info.as_deref()
        && let legend_pure_metamodel::types::TypeExpr::Named {
            element: outer_eid,
            type_arguments: outer_args,
            ..
        } = &ti.type_expr
        && Some(*outer_eid) == class_metatype_id
        && let Some(legend_pure_metamodel::types::TypeExpr::Named {
            type_arguments: inner_args,
            ..
        }) = outer_args.first()
        && !inner_args.is_empty()
    {
        for ta in inner_args {
            if let legend_pure_metamodel::types::TypeExpr::Named { element, .. } = ta {
                type_args.push(Value::Element(*element));
            }
        }
    }
    type_args
}

/// Walk an M3 `KeyExpression` collection and return the canonical
/// `(name, values, augmented)` triple stream the construction helpers
/// expect.
/// Each `KeyExpression`'s
/// `key` slot is expected to wrap a String value (directly or inside
/// an `InstanceValue`'s `.values`); the `expression` slot's value(s)
/// flow through unchanged. The `add` slot defaults to `false` when
/// absent or non-Boolean.
fn decode_key_expressions(
    ctx: &mut dyn EvalContextTrait,
    key_expr_id: Option<ElementId>,
    payload: &[&Value],
) -> Result<Vec<(SmolStr, Vec<Value>, bool)>, PureException> {
    let mut out = Vec::with_capacity(payload.len());
    for v in payload {
        let Value::Object(obj_id) = v else { continue };
        let classifier = ctx.heap().classifier(&obj_id.clone())?.clone();
        let resolved = crate::m3_paths::resolve(ctx.model(), &classifier);
        if resolved != key_expr_id {
            continue;
        }
        let key_vals = ctx.heap().get_property_values(&obj_id.clone(), "key")?;
        let key_str =
            unwrap_instance_value_string(&key_vals.iter().cloned().collect::<Vec<_>>(), ctx)?;
        let expr_vals = ctx
            .heap()
            .get_property_values(&obj_id.clone(), "expression")?;
        let expr_payload: Vec<Value> = expr_vals.iter().cloned().collect();
        let expression_values = unwrap_instance_value_list(&expr_payload, ctx)?;
        let add_vals = ctx.heap().get_property_values(&obj_id.clone(), "add")?;
        let add = matches!(add_vals.iter().next(), Some(Value::Boolean(true)));
        out.push((key_str, expression_values, add));
    }
    Ok(out)
}

/// Read the property name a `KeyExpression.key` slot holds. The slot
/// can carry either a bare `Value::String` or an `InstanceValue`
/// wrapper whose `.values` contains the string (the canonical M3
/// shape produced by `^KeyExpression(key=^InstanceValue(values='prop',
/// …))`). Errors for any other shape.
fn unwrap_instance_value_string(
    payload: &[Value],
    ctx: &mut dyn EvalContextTrait,
) -> Result<SmolStr, PureException> {
    let Some(first) = payload.first() else {
        return Err(PureRuntimeError::EvaluationError("KeyExpression.key is empty".into()).into());
    };
    match first {
        Value::String(s) => Ok(s.clone()),
        Value::Object(obj_id) => {
            let inner = ctx.heap().get_property_values(&obj_id.clone(), "values")?;
            match inner.iter().next() {
                Some(Value::String(s)) => Ok(s.clone()),
                _ => Err(PureRuntimeError::EvaluationError(
                    "KeyExpression.key's InstanceValue.values is missing or not a String".into(),
                )
                .into()),
            }
        }
        other => Err(PureRuntimeError::type_mismatch("String or InstanceValue", other).into()),
    }
}

/// Read the value(s) a `KeyExpression.expression` slot holds. Unwraps
/// an `InstanceValue` wrapper around a literal/collection (Pure's
/// canonical M3 shape) into its `.values` payload. A bare value is
/// passed through unchanged.
fn unwrap_instance_value_list(
    payload: &[Value],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Vec<Value>, PureException> {
    if payload.is_empty() {
        return Ok(Vec::new());
    }
    if let [Value::Object(obj_id)] = payload {
        let inner = ctx.heap().get_property_values(&obj_id.clone(), "values")?;
        if !inner.is_empty() {
            return Ok(inner.iter().cloned().collect());
        }
    }
    Ok(payload.to_vec())
}

/// Convert the compiler-emitted flat `[k, v, augmented, k, v, augmented, …]`
/// stream into the canonical triple form the construction helpers
/// consume.
fn triples_from_flat_kv_stream(
    kvs: &[Value],
) -> Result<Vec<(SmolStr, Vec<Value>, bool)>, PureException> {
    if !kvs.len().is_multiple_of(3) {
        return Err(PureRuntimeError::EvaluationError(format!(
            "object construction: expected (key, value, augmented) triples, got {} extra arguments",
            kvs.len()
        ))
        .into());
    }
    let mut out = Vec::with_capacity(kvs.len() / 3);
    let mut i = 0;
    while i < kvs.len() {
        let key = kvs[i].as_string()?.clone();
        let value = kvs[i + 1].clone();
        let augmented = matches!(&kvs[i + 2], Value::Boolean(true));
        let values = match &value {
            Value::Collection(coll) => coll.iter().cloned().collect(),
            Value::Unit => Vec::new(),
            _ => vec![value],
        };
        out.push((key, values, augmented));
        i += 3;
    }
    Ok(out)
}

thread_local! {
    static CONSTRUCTION_DEPTH: std::cell::Cell<usize> = const { std::cell::Cell::new(0) };
    static PENDING_VALIDATIONS: std::cell::RefCell<Vec<(ObjectHandle, ElementId)>> =
        const { std::cell::RefCell::new(Vec::new()) };
}

/// Shared tail of both `New` overloads.
#[allow(clippy::too_many_arguments)]
fn finish_construction(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    type_args: &[Value],
    type_var_values: &[Value],
    triples: &[(SmolStr, Vec<Value>, bool)],
    lambda_shortcut_args: Option<&[Value]>,
) -> Result<Evaluated, PureException> {
    if Some(class_id) == crate::m3_paths::resolve(ctx.model(), crate::m3_paths::NIL) {
        return Err(PureRuntimeError::EvaluationError(
            "Cannot instantiate meta::pure::metamodel::type::Nil".into(),
        )
        .into());
    }
    let classifier = class_fqn(ctx.model(), class_id);
    if is_lambda_function_class(ctx.model(), class_id)
        && let Some(args_for_shortcut) = lambda_shortcut_args
        && let Some(fn_val) = try_lambda_shortcut(args_for_shortcut)
    {
        return Ok(Evaluated::new(fn_val));
    }

    let obj = ctx.heap_mut().alloc_dynamic(classifier);
    if !type_args.is_empty() {
        ctx.heap_mut()
            .mutate_set(&obj, "__typeArguments", type_args)?;
    }
    if !type_var_values.is_empty() {
        ctx.heap_mut()
            .mutate_set(&obj, "__typeVariableValues", type_var_values)?;
    }
    apply_property_triples(ctx, obj.clone(), triples)?;
    let supplied_keys: std::collections::HashSet<SmolStr> =
        triples.iter().map(|(k, _, _)| k.clone()).collect();
    apply_property_defaults(ctx, &obj, class_id, &supplied_keys)?;
    let mut flat: Vec<Value> = Vec::with_capacity(triples.len() * 3);
    for (k, v, a) in triples {
        flat.push(Value::String(k.clone()));
        flat.push(Value::from_vec(v.clone()));
        flat.push(Value::Boolean(*a));
    }
    populate_association_inverses(ctx, obj.clone(), class_id, &flat)?;
    PENDING_VALIDATIONS.with(|p| p.borrow_mut().push((obj.clone(), class_id)));
    evaluate_class_constraints(ctx, class_id, obj.clone(), type_var_values)?;
    Ok(Evaluated::new(Value::Object(obj)))
}

/// RAII guard that brackets a top-level constructor call (`New`,
/// `NewWithKeyExpressions`, `DynamicNew`) — increments the construction
/// depth on entry, decrements on Drop. Combined with
/// [`ConstructionFrame::drain_if_outermost`] this defers required-property
/// validation until the outermost call finishes evaluating its (possibly
/// nested) arguments.
struct ConstructionFrame;

impl ConstructionFrame {
    fn enter() -> Self {
        CONSTRUCTION_DEPTH.with(|d| d.set(d.get() + 1));
        ConstructionFrame
    }

    /// If we are about to leave the outermost frame, drain
    /// [`PENDING_VALIDATIONS`] and run [`validate_required_properties`] on
    /// every collected `(obj, class_id)`. Call this right before the
    /// guard goes out of scope so any validation failure propagates as
    /// the native's return value (Drop can't return errors).
    fn drain_if_outermost(ctx: &mut dyn EvalContextTrait) -> Result<(), PureException> {
        let depth = CONSTRUCTION_DEPTH.with(std::cell::Cell::get);
        if depth == 1 {
            let pending: Vec<(ObjectHandle, ElementId)> =
                PENDING_VALIDATIONS.with(|p| std::mem::take(&mut *p.borrow_mut()));
            for (obj, cid) in pending {
                validate_required_properties(ctx, obj, cid)?;
            }
        }
        Ok(())
    }
}

impl Drop for ConstructionFrame {
    fn drop(&mut self) {
        CONSTRUCTION_DEPTH.with(|d| d.set(d.get().saturating_sub(1)));
        if CONSTRUCTION_DEPTH.with(std::cell::Cell::get) == 0 {
            PENDING_VALIDATIONS.with(|p| p.borrow_mut().clear());
        }
    }
}

/// Extract the upper bound of a [`Multiplicity`], or `None` for
/// unbounded multiplicities (`[*]`, `[N..*]`, unbound variable).
fn mult_upper_bound(m: &legend_pure_metamodel::types::Multiplicity) -> Option<u32> {
    use legend_pure_metamodel::types::Multiplicity;
    match m {
        Multiplicity::PureOne | Multiplicity::ZeroOrOne => Some(1),
        Multiplicity::OneOrMany | Multiplicity::ZeroOrMany | Multiplicity::Variable(_) => None,
        Multiplicity::Range { upper, .. } => *upper,
    }
}

/// Render a multiplicity as `[1]`, `[0..1]`, `[1..*]`, `[N..M]`, `[*]`.
fn format_mult_bounds(m: &legend_pure_metamodel::types::Multiplicity) -> String {
    use legend_pure_metamodel::types::Multiplicity;
    match m {
        Multiplicity::PureOne => "[1]".into(),
        Multiplicity::ZeroOrOne => "[0..1]".into(),
        Multiplicity::OneOrMany => "[1..*]".into(),
        Multiplicity::ZeroOrMany => "[*]".into(),
        Multiplicity::Range { lower, upper } => match upper {
            Some(u) if u == lower => format!("[{lower}]"),
            Some(u) => format!("[{lower}..{u}]"),
            None => format!("[{lower}..*]"),
        },
        Multiplicity::Variable(name) => format!("[{name}]"),
    }
}

/// Render the inner part of a multiplicity (without surrounding brackets)
/// for the "requires N value(s)" error fragment.
fn format_mult_text(m: &legend_pure_metamodel::types::Multiplicity) -> String {
    use legend_pure_metamodel::types::Multiplicity;
    match m {
        Multiplicity::PureOne => "1".into(),
        Multiplicity::ZeroOrOne => "0..1".into(),
        Multiplicity::OneOrMany => "1..*".into(),
        Multiplicity::ZeroOrMany => "*".into(),
        Multiplicity::Range { lower, upper } => match upper {
            Some(u) if u == lower => format!("{lower}"),
            Some(u) => format!("{lower}..{u}"),
            None => format!("{lower}..*"),
        },
        Multiplicity::Variable(name) => name.to_string(),
    }
}

/// Walk the class's own + inherited properties (regular + association-
/// injected) and verify that every property with `lower_bound > 0` has
/// at least that many values on the populated heap object. Aggregates
/// all violations into one error message.
fn validate_required_properties(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    class_id: ElementId,
) -> Result<(), PureException> {
    use legend_pure_metamodel::types::{Multiplicity, TypeExpr};

    let mut violations: Vec<String> = Vec::new();
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut to_check: Vec<(SmolStr, Multiplicity)> = Vec::new();
    {
        let model = ctx.model();
        let mut stack: Vec<ElementId> = vec![class_id];
        while let Some(cid) = stack.pop() {
            if !visited.insert(cid) {
                continue;
            }
            if let Element::Class(c) = model.get_element(cid) {
                for prop in &c.properties {
                    to_check.push((prop.name.clone(), prop.multiplicity.clone()));
                }
                for st in &c.super_types {
                    if let TypeExpr::Named { element, .. } = st {
                        stack.push(*element);
                    }
                }
            }
            for (assoc_id, prop_idx_pointing_to_self) in model.association_properties(cid) {
                if let Element::Association(assoc) = model.get_element(*assoc_id)
                    && assoc.properties.len() == 2
                {
                    let injected = &assoc.properties[1 - prop_idx_pointing_to_self];
                    to_check.push((injected.name.clone(), injected.multiplicity.clone()));
                }
            }
        }
    }

    for (name, mult) in &to_check {
        let lower: u32 = match mult {
            Multiplicity::PureOne | Multiplicity::OneOrMany => 1,
            Multiplicity::Range { lower, .. } => *lower,
            Multiplicity::ZeroOrOne | Multiplicity::ZeroOrMany | Multiplicity::Variable(_) => 0,
        };
        if lower == 0 {
            continue;
        }
        let count = ctx
            .heap()
            .get_property_values(&obj, name.as_str())
            .map(|vs| u32::try_from(vs.len()).unwrap_or(u32::MAX))
            .unwrap_or(0);
        if count < lower {
            let value_word = if matches!(mult, Multiplicity::PureOne) {
                "value"
            } else {
                "values"
            };
            violations.push(format!(
                "'{name}' requires {} {value_word}, got {count}",
                format_mult_text(mult)
            ));
        }
    }

    if violations.is_empty() {
        return Ok(());
    }
    let class_simple = class_fqn(ctx.model(), class_id);
    let class_simple = class_simple.rsplit("::").next().unwrap_or(&class_simple);
    Err(PureRuntimeError::EvaluationError(format!(
        "Error instantiating class '{class_simple}'.  The following properties have multiplicity violations: {}",
        violations.join(", ")
    ))
    .into())
}

/// Apply each `(name, values, augmented)` triple to the heap object
/// — `mutate_add` for `+=` (augmented), `mutate_set` for `=`
/// (replace).
fn apply_property_triples(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    triples: &[(SmolStr, Vec<Value>, bool)],
) -> Result<(), PureException> {
    for (key, values, augmented) in triples {
        if *augmented {
            ctx.heap_mut().mutate_add(&obj, key.as_str(), values)?;
        } else {
            ctx.heap_mut().mutate_set(&obj, key.as_str(), values)?;
        }
    }
    Ok(())
}

/// Evaluate `default_value` expressions for every property the caller
/// did not supply and write the results to the heap object. Walks the
/// supertype chain child-first so a redeclared property on a subclass
/// wins over the parent's default (Java-parity MRO).
fn apply_property_defaults(
    ctx: &mut dyn EvalContextTrait,
    obj: &ObjectHandle,
    class_id: ElementId,
    supplied_keys: &std::collections::HashSet<SmolStr>,
) -> Result<(), PureException> {
    let default_specs: Vec<(SmolStr, ValueSpec)> = {
        let mut acc: Vec<(SmolStr, ValueSpec)> = Vec::new();
        let mut seen: std::collections::HashSet<SmolStr> = supplied_keys.clone();
        let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
        let mut stack: Vec<ElementId> = vec![class_id];
        while let Some(id) = stack.pop() {
            if !visited.insert(id) {
                continue;
            }
            let Element::Class(c) = ctx.model().get_element(id) else {
                continue;
            };
            for p in &c.properties {
                if !seen.insert(p.name.clone()) {
                    continue;
                }
                if let Some(expr) = &p.default_value {
                    acc.push((p.name.clone(), expr.clone()));
                }
            }
            for st in &c.super_types {
                if let TypeExpr::Named { element, .. } = st {
                    stack.push(*element);
                }
            }
        }
        acc
    };
    for (name, spec) in default_specs {
        let v = ctx.evaluate(&spec)?.into_value();
        let flat: Vec<Value> = match &v {
            Value::Collection(coll) => coll.iter().cloned().collect(),
            Value::Unit => Vec::new(),
            _ => vec![v],
        };
        ctx.heap_mut().mutate_set(obj, name.as_str(), &flat)?;
    }
    Ok(())
}

/// Pure `copy<T>(source:T[1], keyExpressions:KeyExpression[*]):T[1]`
///
/// The compiler's `^$source(prop=val)` desugaring calls this native with
/// flat key/value pairs after the source:
///
/// ```text
/// copy(<Object:...>, "prop1", val1, "prop2", val2, ...)
/// ```
///
/// Allocates a new heap object that shares the source's classifier, copies
/// every property from the source, then applies the overrides.
#[derive(Debug)]
pub struct Copy;

impl NativeFunction for Copy {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        if values.is_empty() {
            return Err(PureRuntimeError::EvaluationError(
                "copy: expected at least 1 argument (source)".into(),
            )
            .into());
        }
        copy_with_triples(ctx, &values[0], &values[1..])
    }
}

/// Pure `getAll<T>(type:Class<T>[1]):T[*]`: every live instance of the
/// class, from the metamodel rows (`Class.all()`) and from the objects
/// reachable through the variable context (`Trade.all()`), each once.
/// A metamodel row comes back as its element so reflective reads resolve
/// through the model.
#[derive(Debug)]
pub struct GetAll;

impl NativeFunction for GetAll {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let class = force1("getAll", args, ctx)?;
        let Value::Element(class_id) = class else {
            return Err(PureRuntimeError::type_mismatch("Class", &class).into());
        };
        let instances = gather_all_instances(ctx, class_id)?;
        Ok(Evaluated::new(Value::from_vec(instances)))
    }
}

fn gather_all_instances(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
) -> Result<Vec<Value>, PureException> {
    if Some(class_id) == crate::m3_paths::resolve(ctx.model(), crate::m3_paths::NIL) {
        return Err(PureRuntimeError::EvaluationError(
            "Cannot getAll instances of meta::pure::metamodel::type::Nil".into(),
        )
        .into());
    }
    let target_path = crate::model_utils::build_element_path(ctx.model(), class_id, "::", false);
    let mut seen: std::collections::HashSet<*const std::cell::RefCell<crate::heap::HeapEntry>> =
        std::collections::HashSet::new();
    let mut projected: Vec<Value> = Vec::new();

    for (handle, cls) in ctx.heap().iter_classifiers() {
        if cls.as_str() != target_path || !seen.insert(std::rc::Rc::as_ptr(&handle)) {
            continue;
        }
        projected.push(match RuntimeHeap::element_for_object(&handle) {
            Some(eid) => Value::Element(eid),
            None => Value::Object(handle),
        });
    }

    let mut worklist: Vec<Value> = ctx.context().iter_values().cloned().collect();
    while let Some(value) = worklist.pop() {
        match &value {
            Value::Object(handle) => {
                if !seen.insert(std::rc::Rc::as_ptr(handle)) {
                    continue;
                }
                let (cls, prop_values): (SmolStr, Vec<Value>) = {
                    let entry = handle.borrow();
                    let cls = entry.classifier();
                    let prop_values: Vec<Value> = entry
                        .property_names()
                        .iter()
                        .flat_map(|name| entry.get_property_values(name.as_str()).into_iter())
                        .collect();
                    (cls, prop_values)
                };
                if cls.as_str() == target_path && RuntimeHeap::element_for_object(handle).is_none()
                {
                    projected.push(value);
                }
                worklist.extend(prop_values);
            }
            Value::Collection(items) => {
                for v in items.iter() {
                    worklist.push(v.clone());
                }
            }
            Value::Map(state) => {
                for v in state.borrow().entries.values() {
                    worklist.push(v.clone());
                }
            }
            _ => {}
        }
    }
    Ok(projected)
}

/// Pure `copy<T>(object:T[1], id:String[1]):T[1]`: a copy with no override.
#[derive(Debug)]
pub struct CopyWithId;

impl NativeFunction for CopyWithId {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (source, _id) = force2("copy", args, ctx)?;
        copy_with_triples(ctx, &source, &[])
    }
}

/// Pure `copy<T>(object:T[1], id:String[1], keyExpressions:KeyExpression[*]):T[1]`
///
/// The platform-declared overload: the overrides arrive as M3
/// `KeyExpression` heap objects, decoded the way `new` decodes them, and
/// the copy proceeds from `(key, values, add)` triples.
#[derive(Debug)]
pub struct CopyWithKeyExpressions;

impl NativeFunction for CopyWithKeyExpressions {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        if values.len() < 2 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "copy(object, id, [keyExpressions]): expected at least 2 arguments, got {}",
                values.len()
            ))
            .into());
        }
        let key_expr_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::KEY_EXPRESSION);
        let payload: Vec<&Value> = match values.get(2) {
            Some(Value::Collection(coll)) => coll.iter().collect(),
            Some(Value::Unit) | None => Vec::new(),
            Some(single) => vec![single],
        };
        let triples = decode_key_expressions(ctx, key_expr_id, &payload)?;
        let mut kvs: Vec<Value> = Vec::with_capacity(triples.len() * 3);
        for (key, expression_values, add) in triples {
            kvs.push(Value::String(key));
            kvs.push(Value::from_vec(expression_values));
            kvs.push(Value::Boolean(add));
        }
        copy_with_triples(ctx, &values[0], &kvs)
    }
}

/// The copy itself: `source` with the `(key, values, add)` triples of
/// `kvs` applied (`add` appends, otherwise the key's values are replaced).
fn copy_with_triples(
    ctx: &mut dyn EvalContextTrait,
    source: &Value,
    kvs: &[Value],
) -> Result<Evaluated, PureException> {
    {
        let values: Vec<Value> = std::iter::once(source.clone())
            .chain(kvs.iter().cloned())
            .collect();

        if let Value::Element(elem_id) = &values[0] {
            let element = ctx.model().get_element(*elem_id);
            let Some(meta_id) = legend_pure_metamodel::bootstrap::metatype_of(ctx.model(), element)
            else {
                return Err(PureRuntimeError::EvaluationError(
                    "copy: element has no M3 metatype — cannot materialise".into(),
                )
                .into());
            };
            let classifier =
                crate::model_utils::build_element_path(ctx.model(), meta_id, "::", false);
            let obj = ctx.heap_mut().alloc_dynamic(classifier);
            hydrate_element_to_heap(ctx, *elem_id, meta_id, obj.clone())?;
            apply_key_value_triples(ctx, obj.clone(), &values[1..])?;
            return Ok(Evaluated::new(Value::Object(obj)));
        }

        let source_id = values[0].as_object()?;

        let classifier: String = ctx.heap().classifier(&source_id)?.to_string();
        let names = ctx.heap().property_names(&source_id)?;
        let mut original_props: Vec<(SmolStr, Vec<Value>)> = Vec::with_capacity(names.len());
        for name in names {
            let prop_values: Vec<Value> = ctx
                .heap()
                .get_property_values(&source_id, name.as_str())?
                .iter()
                .cloned()
                .collect();
            original_props.push((name, prop_values));
        }

        let obj = ctx.heap_mut().alloc_dynamic(classifier.clone());
        let (plain_kvs, path_kvs) = partition_path_kvs(&values[1..])?;

        let path_overrides: std::collections::HashSet<SmolStr> = {
            let mut set = std::collections::HashSet::new();
            let mut i = 0;
            while i < path_kvs.len() {
                if let Ok(k) = path_kvs[i].as_string()
                    && let Some((head, _)) = k.split_once('.')
                {
                    set.insert(SmolStr::new(head));
                }
                i += 3;
            }
            set
        };

        let mut carried_kvs: Vec<Value> = Vec::with_capacity(original_props.len() * 3);
        for (name, prop_values) in &original_props {
            ctx.heap_mut()
                .mutate_add(&obj, name.as_str(), prop_values)?;
            if path_overrides.contains(name) {
                continue;
            }
            carried_kvs.push(Value::String(name.clone()));
            carried_kvs.push(Value::from_vec(prop_values.clone()));
            carried_kvs.push(Value::Boolean(false));
        }

        apply_key_value_triples(ctx, obj.clone(), &plain_kvs)?;
        let cloned_objs = apply_path_property_updates(ctx, source_id, obj.clone(), &path_kvs)?;

        if let Some(class_id) = crate::m3_paths::resolve(ctx.model(), &classifier) {
            sync_object_inverses(ctx, obj.clone(), class_id)?;
        }
        for clone_obj in cloned_objs {
            let clone_classifier = ctx.heap().classifier(&clone_obj)?.to_string();
            if let Some(clone_class_id) = crate::m3_paths::resolve(ctx.model(), &clone_classifier) {
                sync_object_inverses(ctx, clone_obj, clone_class_id)?;
            }
        }
        let _ = (carried_kvs, &path_overrides);

        if !values[1..].is_empty() {
            let si_obj = ctx
                .heap_mut()
                .alloc_dynamic(crate::m3_paths::SOURCE_INFORMATION);
            ctx.heap_mut()
                .mutate_set(&si_obj, "source", &[Value::String("<copy>".into())])?;
            ctx.heap_mut()
                .mutate_set(&obj, "sourceInformation", &[Value::Object(si_obj)])?;
        }

        Ok(Evaluated::new(Value::Object(obj)))
    }
}

/// Recognise the `LambdaFunction` / `Function` M3 classes used by Pure's
/// `^LambdaFunction(expressionSequence = …)` clone idiom.
fn is_lambda_function_class(
    model: &legend_pure_metamodel::model::PureModel,
    class_id: legend_pure_metamodel::ids::ElementId,
) -> bool {
    crate::m3_paths::resolve(model, crate::m3_paths::LAMBDA_FUNCTION) == Some(class_id)
        || crate::m3_paths::resolve(model, crate::m3_paths::FUNCTION) == Some(class_id)
}

/// Detect the `(expressionSequence, <single Function>, augmented)` triple
/// emitted by `^LambdaFunction(expressionSequence = $fn.expressionSequence)`.
///
/// Returns the underlying `Value::Function` when the shortcut applies,
/// so `New` can return it directly instead of allocating a heap object.
/// The augmented flag is ignored.
fn try_lambda_shortcut(kvs: &[Value]) -> Option<Value> {
    if kvs.len() != 3 {
        return None;
    }
    let Value::String(key) = &kvs[0] else {
        return None;
    };
    if key != "expressionSequence" {
        return None;
    }
    match &kvs[1] {
        Value::Function(_) => Some(kvs[1].clone()),
        Value::Collection(v) if v.len() == 1 && matches!(v[0], Value::Function(_)) => {
            Some(v[0].clone())
        }
        _ => None,
    }
}

/// Populate association reverse-links for every Association-backed property
/// just assigned to `obj`.
fn populate_association_inverses(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    class_id: ElementId,
    kvs: &[Value],
) -> Result<(), PureException> {
    if !kvs.len().is_multiple_of(3) {
        return Ok(());
    }

    let entries: Vec<(ElementId, usize)> =
        collect_inherited_association_properties(ctx.model(), class_id);
    if entries.is_empty() {
        return Ok(());
    }

    let mut i = 0;
    while i < kvs.len() {
        let Value::String(key) = &kvs[i] else {
            i += 3;
            continue;
        };
        let assigned = kvs[i + 1].clone();
        for (assoc_id, prop_idx_pointing_to_self) in &entries {
            let Element::Association(assoc) = ctx.model().get_element(*assoc_id) else {
                continue;
            };
            if assoc.properties.len() != 2 {
                continue;
            }
            let injected_idx = 1 - *prop_idx_pointing_to_self;
            let injected_name = assoc.properties[injected_idx].name.clone();
            if injected_name.as_str() != key.as_str() {
                continue;
            }
            let inverse_name = assoc.properties[*prop_idx_pointing_to_self].name.clone();
            let inverse_mult = assoc.properties[*prop_idx_pointing_to_self]
                .multiplicity
                .clone();

            let targets: Vec<ObjectHandle> = match &assigned {
                Value::Object(id) => vec![id.clone()],
                Value::Collection(v) => v
                    .iter()
                    .filter_map(|x| match x {
                        Value::Object(id) => Some(id.clone()),
                        _ => None,
                    })
                    .collect(),
                _ => Vec::new(),
            };
            for target in targets {
                let already_present = ctx
                    .heap()
                    .get_property_values(&target, inverse_name.as_str())
                    .map(|vs| {
                        vs.iter().any(
                            |v| matches!(v, Value::Object(id) if std::rc::Rc::ptr_eq(id, &obj)),
                        )
                    })
                    .unwrap_or(false);
                if already_present {
                    continue;
                }
                if let Some(upper) = mult_upper_bound(&inverse_mult) {
                    let current_count = ctx
                        .heap()
                        .get_property_values(&target, inverse_name.as_str())
                        .map(|vs| u32::try_from(vs.len()).unwrap_or(u32::MAX))
                        .unwrap_or(0);
                    let new_count = current_count + 1;
                    if new_count > upper {
                        let target_class: String = ctx
                            .heap()
                            .classifier(&target)
                            .map(|s| s.to_string())
                            .unwrap_or_default();
                        let target_simple =
                            target_class.rsplit("::").next().unwrap_or(&target_class);
                        return Err(PureRuntimeError::EvaluationError(format!(
                            "Error instantiating the type '{target_simple}'. \
                             The property '{inverse_name}' has a multiplicity range of {} \
                             when the given list has a cardinality equal to {new_count}",
                            format_mult_bounds(&inverse_mult),
                        ))
                        .into());
                    }
                }
                ctx.heap_mut().mutate_add(
                    &target,
                    inverse_name.as_str(),
                    &[Value::Object(obj.clone())],
                )?;
            }
        }
        i += 3;
    }
    Ok(())
}

/// Eager shallow hydration of every M3 reflective property of `meta_id`
/// (and its supertypes) onto the heap object `obj` from the source
/// `Value::Element(elem_id)`.
fn hydrate_element_to_heap(
    ctx: &mut dyn EvalContextTrait,
    elem_id: ElementId,
    meta_id: ElementId,
    obj: ObjectHandle,
) -> Result<(), PureException> {
    let mut prop_names: Vec<SmolStr> = Vec::new();
    let mut seen: std::collections::HashSet<SmolStr> = std::collections::HashSet::new();
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut stack: Vec<ElementId> = vec![meta_id];
    while let Some(cid) = stack.pop() {
        if !visited.insert(cid) {
            continue;
        }
        let Element::Class(c) = ctx.model().get_element(cid) else {
            continue;
        };
        for p in &c.properties {
            if seen.insert(p.name.clone()) {
                prop_names.push(p.name.clone());
            }
        }
        for st in &c.super_types {
            if let TypeExpr::Named { element, .. } = st {
                stack.push(*element);
            }
        }
    }

    for prop in prop_names {
        let target_spec = ValueSpec {
            kind: Box::new(ExprKind::PackageableElementRef { element: elem_id }),
            source_info: SourceInfo::new("<copy-hydrate>", 0, 0, 0, 0),
            type_info: None,
        };
        let access_spec = ValueSpec {
            kind: Box::new(ExprKind::PropertyCall(
                legend_pure_metamodel::types::FunctionCallData {
                    function: None,
                    function_name: prop.clone(),
                    arguments: vec![target_spec],
                },
            )),
            source_info: SourceInfo::new("<copy-hydrate>", 0, 0, 0, 0),
            type_info: None,
        };
        let value = match ctx.evaluate(&access_spec) {
            Ok(v) => v.into_value(),
            Err(_) => continue,
        };
        let values: Vec<Value> = match &value {
            Value::Unit => continue,
            Value::Collection(coll) => coll.iter().cloned().collect(),
            _ => vec![value],
        };
        if values.is_empty() {
            continue;
        }
        ctx.heap_mut().mutate_set(&obj, prop.as_str(), &values)?;
    }
    Ok(())
}

/// Walk a `[key1, val1, augmented1, key2, val2, augmented2, ...]` slice
/// and apply each triple to `obj`.
///
/// On `augmented == false` (the `=` form) uses `mutate_set` (replace);
/// on `augmented == true` (the `+=` form) uses `mutate_add` (append to
/// any carried-over values).
fn apply_key_value_triples(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    kvs: &[Value],
) -> Result<(), PureException> {
    if !kvs.len().is_multiple_of(3) {
        return Err(PureRuntimeError::EvaluationError(format!(
            "object construction: expected (key, value, augmented) triples, got {} extra arguments",
            kvs.len()
        ))
        .into());
    }
    let mut i = 0;
    while i < kvs.len() {
        let key = kvs[i].as_string()?.clone();
        let value = kvs[i + 1].clone();
        let augmented = matches!(&kvs[i + 2], Value::Boolean(true));
        let values: Vec<Value> = match &value {
            Value::Collection(coll) => coll.iter().cloned().collect(),
            Value::Unit => Vec::new(),
            _ => vec![value],
        };
        if augmented {
            ctx.heap_mut().mutate_add(&obj, key.as_str(), &values)?;
        } else {
            ctx.heap_mut().mutate_set(&obj, key.as_str(), &values)?;
        }
        i += 3;
    }
    Ok(())
}

/// Split a `[key1, val1, augmented1, key2, val2, augmented2, ...]` slice
/// into two parallel triple slices: the plain keys (no `.`) and the
/// dotted-path keys (`address.name`, `firm.legalName`).
fn partition_path_kvs(kvs: &[Value]) -> Result<(Vec<Value>, Vec<Value>), PureException> {
    if !kvs.len().is_multiple_of(3) {
        return Err(PureRuntimeError::EvaluationError(format!(
            "copy: expected (key, value, augmented) triples, got {} extra arguments",
            kvs.len()
        ))
        .into());
    }
    let mut plain: Vec<Value> = Vec::new();
    let mut path: Vec<Value> = Vec::new();
    let mut i = 0;
    while i < kvs.len() {
        let key = kvs[i].as_string()?;
        let bucket = if key.contains('.') {
            &mut path
        } else {
            &mut plain
        };
        bucket.push(kvs[i].clone());
        bucket.push(kvs[i + 1].clone());
        bucket.push(kvs[i + 2].clone());
        i += 3;
    }
    Ok((plain, path))
}

/// Apply dotted-path property updates (`address.name = 'Somewhere'`,
/// `firm.legalName = 'FirmX'`) to the freshly-allocated copy.
fn apply_path_property_updates(
    ctx: &mut dyn EvalContextTrait,
    source_id: ObjectHandle,
    target_id: ObjectHandle,
    kvs: &[Value],
) -> Result<Vec<ObjectHandle>, PureException> {
    use std::collections::BTreeMap;

    let mut groups: BTreeMap<SmolStr, Vec<(SmolStr, Value, bool)>> = BTreeMap::new();
    let mut i = 0;
    while i < kvs.len() {
        let key = kvs[i].as_string()?.clone();
        let value = kvs[i + 1].clone();
        let augmented = matches!(&kvs[i + 2], Value::Boolean(true));
        let (head, tail) = match key.split_once('.') {
            Some((h, t)) => (SmolStr::new(h), SmolStr::new(t)),
            None => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "copy: expected dotted-path key, got '{key}'"
                ))
                .into());
            }
        };
        groups
            .entry(head)
            .or_default()
            .push((tail, value, augmented));
        i += 3;
    }

    let mut all_clones: Vec<ObjectHandle> = Vec::new();
    for (head, updates) in groups {
        let nested = ctx.heap().get_property_values(&source_id, head.as_str())?;
        let mut cloned: Vec<Value> = Vec::with_capacity(nested.len());
        for v in &nested {
            let Value::Object(inner_id) = v else {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "copy: cannot path-set '{head}.…' on a non-object property value"
                ))
                .into());
            };
            let clone_id = clone_heap_object(ctx, inner_id.clone())?;
            all_clones.push(clone_id.clone());
            let mut leaf_kvs: Vec<Value> = Vec::new();
            let mut nested_path_kvs: Vec<Value> = Vec::new();
            for (rest, val, augmented) in &updates {
                let bucket = if rest.contains('.') {
                    &mut nested_path_kvs
                } else {
                    &mut leaf_kvs
                };
                bucket.push(Value::String(rest.clone()));
                bucket.push(val.clone());
                bucket.push(Value::Boolean(*augmented));
            }
            apply_key_value_triples(ctx, clone_id.clone(), &leaf_kvs)?;
            if !nested_path_kvs.is_empty() {
                let nested_clones = apply_path_property_updates(
                    ctx,
                    inner_id.clone(),
                    clone_id.clone(),
                    &nested_path_kvs,
                )?;
                all_clones.extend(nested_clones);
            }
            cloned.push(Value::Object(clone_id));
        }
        ctx.heap_mut()
            .mutate_set(&target_id, head.as_str(), &cloned)?;
    }
    Ok(all_clones)
}

/// Walks the supertype chain of `class_id` and accumulates every
/// association entry (`(association_id, prop_idx_pointing_to_self)`)
/// the class participates in — directly or via inheritance.
fn collect_inherited_association_properties(
    model: &legend_pure_metamodel::model::PureModel,
    class_id: ElementId,
) -> Vec<(ElementId, usize)> {
    use legend_pure_metamodel::types::TypeExpr;
    let mut acc: Vec<(ElementId, usize)> = Vec::new();
    let mut seen: std::collections::HashSet<(ElementId, usize)> = std::collections::HashSet::new();
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut stack: Vec<ElementId> = vec![class_id];
    while let Some(cid) = stack.pop() {
        if !visited.insert(cid) {
            continue;
        }
        for entry in model.association_properties(cid) {
            if seen.insert(*entry) {
                acc.push(*entry);
            }
        }
        if let Element::Class(c) = model.get_element(cid) {
            for st in &c.super_types {
                if let TypeExpr::Named { element, .. } = st {
                    stack.push(*element);
                }
            }
        }
    }
    acc
}

/// Java-faithful end-of-Copy inverse synchronisation.
/// Walks every association the object's classifier
/// participates in, reads the object's CURRENT slot value for the
/// injected (forward) side, and idempotently appends `obj` to each
/// target's inverse-side slot.
///
/// Scope: only the `obj` passed in. Callers that produce multiple
/// affected instances (e.g. `Copy::execute` plus path-property clones)
/// must invoke this helper once per object.
fn sync_object_inverses(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    class_id: ElementId,
) -> Result<(), PureException> {
    let entries: Vec<(ElementId, usize)> =
        collect_inherited_association_properties(ctx.model(), class_id);
    if entries.is_empty() {
        return Ok(());
    }
    for (assoc_id, prop_idx_pointing_to_self) in entries {
        let Element::Association(assoc) = ctx.model().get_element(assoc_id) else {
            continue;
        };
        if assoc.properties.len() != 2 {
            continue;
        }
        let injected_idx = 1 - prop_idx_pointing_to_self;
        let injected_name = assoc.properties[injected_idx].name.clone();
        let inverse_name = assoc.properties[prop_idx_pointing_to_self].name.clone();

        let targets: Vec<ObjectHandle> = ctx
            .heap()
            .get_property_values(&obj, injected_name.as_str())
            .map(|vs| {
                vs.iter()
                    .filter_map(|v| match v {
                        Value::Object(id) => Some(id.clone()),
                        _ => None,
                    })
                    .collect()
            })
            .unwrap_or_default();

        for target in targets {
            let already_present = ctx
                .heap()
                .get_property_values(&target, inverse_name.as_str())
                .map(|vs| {
                    vs.iter()
                        .any(|v| matches!(v, Value::Object(id) if std::rc::Rc::ptr_eq(id, &obj)))
                })
                .unwrap_or(false);
            if already_present {
                continue;
            }
            ctx.heap_mut().mutate_add(
                &target,
                inverse_name.as_str(),
                &[Value::Object(obj.clone())],
            )?;
        }
    }
    Ok(())
}

/// Allocate a fresh heap Object that mirrors `source_id` — same
/// classifier and a copy of every property.
fn clone_heap_object(
    ctx: &mut dyn EvalContextTrait,
    source_id: ObjectHandle,
) -> Result<ObjectHandle, PureException> {
    let classifier = ctx.heap().classifier(&source_id)?.clone();
    let names = ctx.heap().property_names(&source_id)?;
    let mut snapshot: Vec<(SmolStr, Vec<Value>)> = Vec::with_capacity(names.len());
    for name in names {
        let prop_values: Vec<Value> = ctx
            .heap()
            .get_property_values(&source_id, name.as_str())?
            .iter()
            .cloned()
            .collect();
        snapshot.push((name, prop_values));
    }
    let clone_id = ctx.heap_mut().alloc_dynamic(classifier);
    for (name, vs) in snapshot {
        ctx.heap_mut().mutate_set(&clone_id, name.as_str(), &vs)?;
    }
    Ok(clone_id)
}

/// Run every constraint declared on the given Class against the
/// freshly-constructed instance.
fn evaluate_class_constraints(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    obj: ObjectHandle,
    type_var_values: &[Value],
) -> Result<(), PureException> {
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    evaluate_class_constraints_with_inheritance(ctx, class_id, obj, type_var_values, &mut visited)
}

fn evaluate_class_constraints_with_inheritance(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    obj: ObjectHandle,
    type_var_values: &[Value],
    visited: &mut std::collections::HashSet<ElementId>,
) -> Result<(), PureException> {
    if !visited.insert(class_id) {
        return Ok(());
    }
    evaluate_class_constraints_at_level(ctx, class_id, obj.clone(), type_var_values)?;

    let parents: Vec<ElementId> = match ctx.model().get_element(class_id) {
        Element::Class(class) => class
            .super_types
            .iter()
            .filter_map(|st| match st {
                legend_pure_metamodel::types::TypeExpr::Named { element, .. } => Some(*element),
                _ => None,
            })
            .filter(|eid| matches!(ctx.model().get_element(*eid), Element::Class(_)))
            .collect(),
        _ => Vec::new(),
    };
    for parent_id in parents {
        evaluate_class_constraints_with_inheritance(ctx, parent_id, obj.clone(), &[], visited)?;
    }
    Ok(())
}

fn evaluate_class_constraints_at_level(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    obj: ObjectHandle,
    type_var_values: &[Value],
) -> Result<(), PureException> {
    let (constraints, param_names, class_name) = match ctx.model().get_element(class_id) {
        Element::Class(class) if !class.constraints.is_empty() => {
            let names: Vec<SmolStr> = class
                .type_variable_parameters
                .iter()
                .map(|p| p.name.clone())
                .collect();
            (
                class.constraints.clone(),
                names,
                ctx.model().element_name(class_id).clone(),
            )
        }
        _ => return Ok(()),
    };
    ctx.context_mut().push_scope();
    ctx.context_mut()
        .set(SmolStr::new_static("this"), Value::Object(obj));
    for (name, value) in param_names.iter().zip(type_var_values.iter()) {
        ctx.context_mut().set(name.clone(), value.clone());
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
            class_name.clone(),
            message,
            constraint.source_info.clone(),
            Vec::new(),
        ));
        break;
    }
    ctx.context_mut().pop_scope();
    match first_failure {
        Some(e) => Err(e),
        None => Ok(()),
    }
}

/// Compute the qualified path for a class element (used as heap classifier).
fn class_fqn(model: &PureModel, id: ElementId) -> String {
    let mut segments: Vec<SmolStr> = Vec::new();
    segments.push(match model.get_element(id) {
        Element::Function(f) => f.function_name.clone(),
        _ => model.element_name(id).clone(),
    });
    let mut parent = match id {
        ElementId::Package(pkg_id) => model.get_package(pkg_id).parent,
        ElementId::InstanceId { .. } => Some(model.get_node(id).parent_package),
    };
    while let Some(pkg_id) = parent {
        let pkg = model.get_package(pkg_id);
        if pkg.parent.is_none() {
            break;
        }
        segments.push(pkg.name.clone());
        parent = pkg.parent;
    }
    segments.reverse();
    segments
        .iter()
        .map(SmolStr::as_str)
        .collect::<Vec<_>>()
        .join("::")
}

/// Register the fished language natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("letFunction_String_1__T_m__T_m_", LetFunction);
    registry.register("if_Boolean_1__Function_1__Function_1__T_m_", If);
    registry.register("eval_Function_1__V_m_", Eval);
    registry.register("eval_Function_1__T_n__V_m_", Eval);
    registry.register("eval_Function_1__T_n__U_p__V_m_", Eval);
    registry.register("eval_Function_1__T_n__U_p__W_q__V_m_", Eval);
    registry.register("eval_Function_1__T_n__U_p__W_q__X_r__V_m_", Eval);
    registry.register("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_", Eval);
    registry.register("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_", Eval);
    registry.register(
        "eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_",
        Eval,
    );
    registry.register("new_Class_1__String_1__T_1_", New);
    registry.register(
        "new_Class_1__String_1__KeyExpression_MANY__T_1_",
        NewWithKeyExpressions,
    );
    registry.register("getAll_Class_1__T_MANY_", GetAll);
    registry.register("copy_T_1__String_1__T_1_", CopyWithId);
    registry.register(
        "copy_T_1__String_1__KeyExpression_MANY__T_1_",
        CopyWithKeyExpressions,
    );
}
