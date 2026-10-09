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

//! Language-level natives fished so far: `letFunction`, `if`, `new`, `copy`.
//!
//! `new` is split across two natives by call shape: `New` handles the
//! compiler-emitted `^Class<T>(prop=val, …)` payload (and the two-argument
//! platform form), `NewWithKeyExpressions` the platform-declared
//! `new(class, id, [^KeyExpression(...)])`. Both share the construction
//! helpers below (defaults, required-property validation, association
//! inverses, class constraints), which is why they arrive together.
//! `print`, `eval`, `evaluate`, `getAll*`, `dynamicNew`, `removeOverride`
//! and `rawEvalProperty` stay on the archive until a test asks.

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{ExprKind, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use crate::error::{PureException, PureRuntimeError};
use crate::heap::ObjectHandle;
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all,
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
        // Branches are typed `Function<{->T[m]}>` — the compiler lowers them
        // as zero-parameter `Lambda` wrappers. `force_thunk` unwraps that so
        // the caller gets the branch's value, not the closure itself.
        crate::native::force_thunk(branch, ctx)
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
/// The second argument (class simple name) is ignored — we classify the
/// heap entry by the class's full path so property lookups through
/// `Value::Object` surface the right metatype. Extra property assignments
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
        let _id = &values[1]; // ignored — Pure's Java impl uses this only as a debug label
        // Compiler-emitted shape: `[class, id, type_args_coll,
        // type_var_values_coll, key1, val1, augmented1, …]`. The
        // metadata slots are always present (empty Unit/Collection if
        // no `<…>` was declared). No probing — this native is
        // registered only for the compiler's call shape; the
        // documented Pure-source `new(class, id, [keyExprs])` family
        // dispatches to `NewWithKeyExpressions` instead.
        //
        // The 2-arg Pure-source `new(class, id)` overload also lands
        // here via the simple-name prefix fallback (no explicit
        // `new_Class_1__String_1__T_1_` registration). It carries no
        // metadata slots; type_args fall back to args[0].type_info
        // exactly like NewWithKeyExpressions does.
        let class_metatype_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::CLASS);
        let mut type_args: Vec<Value> = Vec::new();
        let mut type_var_values: Vec<Value> = Vec::new();
        let mut kvs_offset = 2;
        if values.len() >= 3 {
            // Only collect Element entries — `^Class<U,V>(...)` lowering
            // emits `TypeReference` exprs whose Generic("U") variant
            // evaluates to `Value::Unit` (eval.rs:304-309). Writing
            // those Units to `__typeArguments` produces a non-empty
            // but useless slot (downstream `genericType` reflection
            // filters Units anyway). Skipping them here keeps the
            // slot truly empty for the inner `^Class<U,V>` case,
            // which lets the outer call's post-hoc write fire when
            // the substituted return type carries concrete args.
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
        // Pure-source 2-arg shape (no metadata slots): back-fill from
        // args[0].type_info. Only fires when the explicit slot above
        // didn't already provide them.
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
            /*lambda_shortcut_args*/ Some(&values[kvs_offset..]),
        )?;
        // Outermost-frame validation drain happens here, before frame
        // exits via Drop — so any pending validation surfaces as the
        // ?-propagated error from this top-level call.
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
///
/// Decodes each `KeyExpression` by reading its `key`, `expression`, and
/// `add` slots, then funnels through the shared `finish_construction`
/// path so heap allocation, `__typeArguments` back-fill, association
/// inverses, and constraint evaluation are identical for both
/// overloads. Recognition uses M3 identity (`m3_paths::KEY_EXPRESSION`
/// resolved to its `ElementId`); never classifier-string compared.
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
        // Back-fill type_args from the compiler-tracked type of args[0]
        // (Lane B infrastructure). When args[0] is a `Class<X>`-typed
        // expression — e.g. the result of `$l1->class()` whose Pass 2.5
        // type is `Class<List<String>>` — extract the inner `<...>` for
        // the new instance's `__typeArguments`. M3 identity throughout:
        // the outer-element check uses `m3_paths::CLASS`'s resolved
        // ElementId, never classifier strings.
        let class_metatype_id = crate::m3_paths::resolve(ctx.model(), crate::m3_paths::CLASS);
        let type_args = back_fill_type_args(args, class_metatype_id);
        // Decode the KeyExpression collection (or unwrapped singleton)
        // using M3 identity — see `decode_key_expressions`.
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
        let result = finish_construction(
            ctx,
            class_id,
            &type_args,
            &Vec::new(),
            &triples,
            /*lambda_shortcut_args*/ None,
        )?;
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
/// expect. Identity check uses the pre-resolved `KeyExpression` M3
/// element id — never the classifier string. Each `KeyExpression`'s
/// `key` slot is expected to wrap a String value (directly or inside
/// an `InstanceValue`'s `.values`); the `expression` slot's value(s)
/// flow through unchanged. The `add` slot defaults to `false` when
/// absent or non-Boolean (matches `^KeyExpression(key=…, expression=…)`
/// constructions that don't set the augmented flag).
#[allow(clippy::result_large_err)]
fn decode_key_expressions(
    ctx: &mut dyn EvalContextTrait,
    key_expr_id: Option<ElementId>,
    payload: &[&Value],
) -> Result<Vec<(SmolStr, Vec<Value>, bool)>, PureException> {
    let mut out = Vec::with_capacity(payload.len());
    for v in payload {
        let Value::Object(obj_id) = v else { continue };
        // Identity gate — only walk objects classified as
        // KeyExpression. Non-KeyExpression objects in the collection
        // are silently skipped so the helper composes with mixed
        // payloads.
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
/// …))`). Errors for any other shape so misuse surfaces eagerly
/// instead of silently dropping the assignment.
#[allow(clippy::result_large_err)]
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
#[allow(clippy::result_large_err)]
fn unwrap_instance_value_list(
    payload: &[Value],
    ctx: &mut dyn EvalContextTrait,
) -> Result<Vec<Value>, PureException> {
    if payload.is_empty() {
        return Ok(Vec::new());
    }
    // Single InstanceValue wrapper — unwrap to its `values` payload.
    if let [Value::Object(obj_id)] = payload {
        let inner = ctx.heap().get_property_values(&obj_id.clone(), "values")?;
        if !inner.is_empty() {
            return Ok(inner.iter().cloned().collect());
        }
    }
    // Bare value(s) — pass through.
    Ok(payload.to_vec())
}

/// Convert the compiler-emitted flat `[k, v, augmented, k, v, augmented, …]`
/// stream into the canonical triple form the construction helpers
/// consume. Mirrors `apply_key_value_triples`'s arity guard so the
/// upstream check fires here once instead of in every call site.
#[allow(clippy::result_large_err)]
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

// Construction-depth tracking — used to defer required-property
// validation until the OUTERMOST construction completes. When
// `^Car(owner=^Owner(...))` evaluates, the inner Owner finishes
// first; at that point its association-injected `car` property is
// still empty because the parent Car's `populate_association_inverses`
// hasn't run yet. Validating eagerly there would false-positive on
// legitimate nested construction (`testNewWithReverseOneToOneProperty`).
// Java Pure interpreted defers validation to the same point.
//
// Push every constructed object's (id, class) pair to `PENDING_VALIDATIONS`
// during nested calls; drain + validate when depth returns to 0.
thread_local! {
    static CONSTRUCTION_DEPTH: std::cell::Cell<usize> = const { std::cell::Cell::new(0) };
    static PENDING_VALIDATIONS: std::cell::RefCell<Vec<(ObjectHandle, ElementId)>> =
        const { std::cell::RefCell::new(Vec::new()) };
}

/// Shared tail of both `New` overloads. Allocates the heap object,
/// applies type-arg + type-var-value metadata, applies the property
/// triples, fires off association inverse propagation, and evaluates
/// constraints. Lambda shortcut: when the construction is targeting
/// `LambdaFunction`-shaped classes and the payload reduces to a
/// single Function value (the `^LambdaFunction(expressionSequence =
/// $fn)` clone idiom), the shortcut returns the function value
/// directly without allocating — only the compiler-emitted call shape
/// passes the shortcut probe (Pure-source `KeyExpression` construction
/// of a Function would be unusual; left out for now).
#[allow(clippy::result_large_err, clippy::too_many_arguments)]
fn finish_construction(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    type_args: &[Value],
    type_var_values: &[Value],
    triples: &[(SmolStr, Vec<Value>, bool)],
    lambda_shortcut_args: Option<&[Value]>,
) -> Result<Evaluated, PureException> {
    // Reject `^Nil()` — `meta::pure::metamodel::type::Nil` is the bottom type
    // and is not instantiable. Java Pure raises the same error; pinned by
    // platform test `testNewNil`. Identity-resolved (not classifier-string
    // compared) because `class_fqn` returns `"Nil"` for top-level types.
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
    // Back-fill any unsupplied property from its declared `default_value`
    // expression — must run before deferred multiplicity validation so
    // the validator sees the populated values. Caller-supplied triples
    // already won the prior `apply_property_triples` write, so the
    // `supplied_keys` filter below skips them. T-20260512-04.
    let supplied_keys: std::collections::HashSet<SmolStr> =
        triples.iter().map(|(k, _, _)| k.clone()).collect();
    apply_property_defaults(ctx, &obj, class_id, &supplied_keys)?;
    // Re-flatten triples back to the kv-stream shape that
    // `populate_association_inverses` consumes — the helper drives
    // its inverse walk off `(key, value, augmented)` triples already,
    // so this is a thin re-pack rather than a reimplementation.
    let mut flat: Vec<Value> = Vec::with_capacity(triples.len() * 3);
    for (k, v, a) in triples {
        flat.push(Value::String(k.clone()));
        flat.push(Value::from_vec(v.clone()));
        flat.push(Value::Boolean(*a));
    }
    populate_association_inverses(ctx, obj.clone(), class_id, &flat)?;
    // Defer property-cardinality validation to the outermost level —
    // depth tracking lives in `New::execute` / `NewWithKeyExpressions::
    // execute` / `DynamicNew::execute`, which wrap the entire arg-
    // evaluation + construction call. See CONSTRUCTION_DEPTH comment.
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
///
/// Inner `^Class(...)` expressions are pre-evaluated by `force_all` BEFORE
/// the outer `finish_construction` runs, so the depth must be tracked at
/// the native-execute boundary, not inside `finish_construction`.
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
    #[allow(clippy::result_large_err)]
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
        // If we exited the outermost frame on an error path that didn't
        // call drain_if_outermost, the pending list is now stale — clear
        // it so it doesn't leak into the next top-level call.
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

/// Render a multiplicity as `[1]`, `[0..1]`, `[1..*]`, `[N..M]`, `[*]` —
/// matches the Java Pure error messages pinned by
/// `testNewWithChildWithMismatchedReverseOneToOneProperty`.
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
/// for the "requires N value(s)" error fragment pinned by
/// `testNewWithMissingOneToOneProperty` / `…OneToManyProperty`.
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
/// all violations into one Java-parity error message — pinned by
/// `testNewWithMissingOneToOneProperty` and `…OneToManyProperty`.
#[allow(clippy::result_large_err)]
fn validate_required_properties(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    class_id: ElementId,
) -> Result<(), PureException> {
    use legend_pure_metamodel::types::{Multiplicity, TypeExpr};

    let mut violations: Vec<String> = Vec::new();
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    // Snapshot of all (property name, multiplicity, source class) — collected
    // first against an immutable model borrow, then validated against the
    // heap (which may need a mut borrow if get_property_values caches).
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
            // Association-injected properties pointing AT this class — the
            // INJECTED side (the one visible on this class) is at index
            // `1 - prop_idx_pointing_to_self`.
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
#[allow(clippy::result_large_err)]
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
///
/// Shared by `^Class(...)` / `new(class, id, [keyExpr])` (via
/// `finish_construction`) and the reflective `dynamicNew` overload —
/// the two paths previously inlined this logic separately, and drift
/// between them caused T-20260512-04 (defaults ignored on `^Class(...)`).
#[allow(clippy::result_large_err)]
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

        // Copying a `Value::Element` — `^$functionRef()` in Pure. Elements
        // aren't heap-backed, so we materialise a fresh heap Object
        // classified by the element's metatype (`ConcreteFunctionDefinition`,
        // `Class`, etc. via `bootstrap::metatype_of`). Subsequent introspection
        // (`$copy->genericType().rawType`) then resolves to the same metatype
        // the original's element reflection would produce.
        //
        // Java parity: per `legend-pure-runtime-java-engine-interpreted/.../
        // natives/grammar/lang/creation/Copy.java:196-237`, the Java Copy
        // walks `class_getSimplePropertiesByName(classifier)` and for every
        // property NOT being overridden by the caller, copies the source's
        // values onto the new instance via `addValueToProperty(...)` —
        // shallow (shared inner references), eager (no lazy delegation).
        // The literal Java comment is `// The new instance doesn't have
        // the value ... Add (not deep!)`. We mirror that here by walking
        // the metatype + supertypes' property lists, evaluating each
        // against the source via the same property-access path the user
        // would hit via `$f1.<prop>`, and storing the result on the heap.
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

        // Snapshot classifier + properties before we take a mutable heap borrow.
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
        // Split the supplied kvs into plain (no `.`) and dotted-path
        // (`address.name = 'X'`) groups. Plain kvs go through the normal
        // mutate_set; path kvs need a per-first-segment deep-clone so
        // `^$pierre(address.name='Somewhere')` lands on a fresh Address
        // object instead of mutating the source's nested instance.
        let (plain_kvs, path_kvs) = partition_path_kvs(&values[1..])?;

        // Path-property updates rebind a top-level property (`firm`) to
        // a fresh clone — the carried-over inverse population for that
        // property would otherwise add the new copy to the *source's*
        // nested-object inverse list (e.g. add `$bob` to the OLD
        // `gsJC.employees`). Track the first-segment names to exclude
        // them from carried inverse population.
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

        // Flatten carried-over properties into a
        // `[key1, val1, augmented1=false, key2, val2, augmented2=false, …]`
        // slice compatible with `populate_association_inverses` — the copy
        // should appear in every association inverse its source belonged
        // to (e.g. a copied Person is appended to `firm.employees` so
        // `assertSameElements([$bob, $pierre], $firmX.employees)` holds).
        // Carried values are unconditionally `=` semantics (we already used
        // `mutate_add` above to seed the slot from the source); the false
        // flag here only matters for the inverse walk's stride.
        // Skip any property the user is path-overriding; the inverse for
        // those is established by the path-property handler against the
        // freshly-cloned nested object.
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
        // Path-property updates clone every nested object whose
        // first segment is overridden, returning each clone's ObjectHandle
        // so the end-of-Copy inverse walk below visits them too. The
        // clones inherit every association slot from their source
        // (e.g. a cloned LA_Division parent carries `firm=$firmX`),
        // so their inverses (`$firmX.organizations`) need to be
        // populated separately from the outer `obj`'s.
        let cloned_objs = apply_path_property_updates(ctx, source_id, obj.clone(), &path_kvs)?;

        // Single end-of-Copy inverse walk per affected object —
        // matches Java Pure's `Copy.java:236` (`updateReverseProperties(
        // newInstance, ...)`) which fires once at the end.
        // `sync_object_inverses` reads the object's CURRENT slot
        // values rather than walking the kvs that built it, so it
        // naturally sees the post-`mutate_set` state — a property
        // overridden by `plain_kvs` no longer points at the source's
        // value, so the inverse on the source's old peer doesn't get
        // a spurious entry. Also visits each clone produced by
        // `apply_path_property_updates` so cloned nested objects sync
        // their own inverses (e.g. the cloned parent's
        // `firm=$firmX` flows into `$firmX.organizations`, fixing
        // testDeepCopyWithAssociation2).
        if let Some(class_id) = crate::m3_paths::resolve(ctx.model(), &classifier) {
            sync_object_inverses(ctx, obj.clone(), class_id)?;
        }
        for clone_obj in cloned_objs {
            let clone_classifier = ctx.heap().classifier(&clone_obj)?.to_string();
            if let Some(clone_class_id) = crate::m3_paths::resolve(ctx.model(), &clone_classifier) {
                sync_object_inverses(ctx, clone_obj, clone_class_id)?;
            }
        }
        // The carried-kvs / plain-kvs / path-head triple of
        // `populate_association_inverses` calls was retired here —
        // every effect that loop produced is now produced by
        // `sync_object_inverses` reading the object's final slot
        // state. The `path_overrides` variable above is still used
        // by the carried-property seeding loop (line 529) to skip
        // re-`mutate_add`ing properties the user is overriding.
        let _ = (carried_kvs, &path_overrides);

        // When the user supplied any overrides (`^$src(prop=val)`), the
        // resulting copy is conceptually a different value than its
        // source — Java Pure stamps it with a fresh `sourceInformation`.
        // No-override clones (`^$src()`) preserve the source's slot.
        // Pinned by `testSourceInformationCopy` which asserts that
        // `^$x0().sourceInformation().source == $x0.sourceInformation().source`
        // but `^$x0(expressionSequence=$x0.expressionSequence)
        //   .sourceInformation().source != $x0.sourceInformation().source`.
        // We synthesise a small `SourceInformation` heap object whose
        // `source` slot is `"<copy>"` — distinguishable from any real
        // source path so the `!=` assertion holds. Java parity on the
        // exact source string isn't required here (no test pins it).
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
///
/// Compares by resolved [`ElementId`] against the canonical
/// `meta::pure::metamodel::function::{Function,LambdaFunction}` paths — not
/// by textual suffix match on the classifier string — so unrelated classes
/// named `LambdaFunction` in user code don't trigger the shortcut.
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
/// The augmented flag is ignored — the shortcut applies only when the
/// triple list has exactly one entry, so there is no carried value to
/// distinguish `=` from `+=` against.
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
///
/// When Pure code writes `^Person(firm=firmX)`, the `Employment` association
/// dictates that `firmX.employees` must also contain the new person. Java
/// Pure maintains this invariant implicitly; the Rust heap does not, so we
/// walk the key/value pairs after construction and inject the reverse edge.
///
/// Algorithm per `(key, value)` pair:
/// 1. Look up `association_properties[class_id]` — a list of
///    `(association_id, prop_idx)` where `assoc.properties[prop_idx]` has
///    type = `class_id` (i.e., points back AT us).
/// 2. In each such association with exactly two properties, the property
///    at `1 - prop_idx` is the one *injected into* `class_id` (the one
///    the caller may have just set).
/// 3. If that injected property's name matches `key` and the assigned
///    value is a heap `Value::Object`, append `obj` to the target
///    object's `assoc.properties[prop_idx].name` slot — the inverse.
#[allow(clippy::result_large_err)]
fn populate_association_inverses(
    ctx: &mut dyn EvalContextTrait,
    obj: ObjectHandle,
    class_id: ElementId,
    kvs: &[Value],
) -> Result<(), PureException> {
    if !kvs.len().is_multiple_of(3) {
        return Ok(()); // Arity already validated by apply_key_value_triples
    }

    // Snapshot the association entries once — cheap O(n) per property but we
    // avoid re-borrowing the model mid-loop. Walk supertypes too, so a
    // subclass `^LA_Division(firm=$firmX)` finds the `firm`/`organizations`
    // association declared against the LA_Organization supertype.
    let entries: Vec<(ElementId, usize)> =
        collect_inherited_association_properties(ctx.model(), class_id);
    if entries.is_empty() {
        return Ok(());
    }

    // For each forward assignment key, find the matching injected property
    // and its inverse, then mutate the target object's inverse slot. The
    // augmented flag at slot `i+2` is irrelevant to inverse semantics —
    // whether the user wrote `firm = $f` or `firm += $f`, the same
    // reverse-link must be established.
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
                continue; // n-ary associations: inverse isn't a single peer
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
                // Idempotency guard — match Java Pure's `New.java:318`
                // (`if (!currentValues.contains(instance))`). Without this,
                // each pass through `populate_association_inverses` would
                // append a fresh entry, so the same Copy that calls the
                // helper multiple times (carried + plain + per-path-head
                // synthetic) would double-bind the inverse on every shared
                // target. Read the target's current inverse-slot values
                // and skip the `mutate_add` if `obj` is already present
                // by ObjectHandle. This is also a precondition for Lane A's
                // Step 3 — the end-of-Copy single re-walk would otherwise
                // regress `testDeepCopyWithAssociation1` (`gsNYC.employees`
                // would receive `$bob` twice).
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
                // Inverse-cardinality check — pinned by
                // `testNewWithChildWithMismatchedReverseOneToOneProperty`.
                // If appending `obj` would push the inverse slot past its
                // declared upper bound, raise the Java-parity cardinality
                // error before mutating. `inverse_mult` was cloned above
                // outside the targets loop to keep the model borrow short.
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
///
/// Mirrors Java Pure's `Copy.copy(...)` behaviour for Element-source
/// constructions (`^$f1()`): walk every property declared on the
/// classifier's M3 metatype and any ancestor up to `Any`, evaluate the
/// property against the source element via the same property-access
/// path the user would hit (`$f1.<prop>`), and store the resulting
/// values on the new heap object. Empty results (`Value::Unit`) are
/// skipped — the heap returns empty for unset slots anyway, so storing
/// `Unit` is a no-op while still leaving room for the user's
/// subsequent `apply_key_value_triples` overrides to set a real value.
///
/// This delegation through `ctx.evaluate` (rather than re-implementing
/// `eval_element_property` here) means any future Element-side property
/// the runtime learns to compute (e.g. `parameters` once we wire it up
/// from `Function.parameters`) automatically flows into the copy
/// without further changes to this helper.
#[allow(clippy::result_large_err)]
fn hydrate_element_to_heap(
    ctx: &mut dyn EvalContextTrait,
    elem_id: ElementId,
    meta_id: ElementId,
    obj: ObjectHandle,
) -> Result<(), PureException> {
    // Collect every (de-duplicated, declaration-order) property name
    // declared on the metatype Class or any of its supertypes. Walks
    // the supertype chain breadth-first so subclass redeclarations
    // shadow parent ones at the same name.
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

    // For each property, build a synthetic `<elem>.<prop>` access spec
    // and evaluate it under the current scope. This routes through the
    // evaluator's `eval_element_property` / `eval_function_property` so
    // we don't duplicate that dispatch table here.
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
        // Tolerate per-property errors — if the runtime can't compute
        // a particular slot on this element kind, treat it as empty
        // (the heap default) rather than failing the whole copy.
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
/// any carried-over values). `Copy` pre-populates from the source and
/// relies on `=` to reset the slot so the override doesn't leak the
/// original value, while `+=` is the only way to extend a carried list
/// without restating its prior contents. `New` always allocates a fresh
/// slot, so the distinction is moot at construction time but the
/// compiler still threads the flag through for symmetry.
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
/// dotted-path keys (`address.name`, `firm.legalName`). Both keep the
/// same triple shape so `apply_key_value_triples` and
/// `apply_path_property_updates` can consume them uniformly.
#[allow(clippy::result_large_err)]
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
///
/// For each unique first segment, deep-clone the source's nested object
/// once and apply every update keyed under that segment to the clone.
/// The new object's first-segment property is then set to the clone —
/// this is what makes `assertIsNot($pierre.address, $pierre2.address)`
/// hold even though only the leaf field was mentioned. Multi-level
/// paths (`a.b.c`) recurse: the `b.c = v` update is applied as a
/// path-property on the clone of `a`.
#[allow(clippy::result_large_err)]
fn apply_path_property_updates(
    ctx: &mut dyn EvalContextTrait,
    source_id: ObjectHandle,
    target_id: ObjectHandle,
    kvs: &[Value],
) -> Result<Vec<ObjectHandle>, PureException> {
    use std::collections::BTreeMap;

    // Group `(first_segment → [(rest_path, value, augmented), …])` so
    // multiple updates against the same first segment share a single
    // clone. The augmented flag flows through to whichever bucket the
    // leaf eventually lands in (`mutate_add` for `+=`, `mutate_set` for
    // `=`).
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

    // Track every clone ID produced — direct clones at this level plus
    // any deeper clones from recursive calls. The caller (`Copy::execute`)
    // walks each clone's classifier associations to sync inverses; without
    // that, a cloned nested object's carried-over association slot (e.g.
    // `firm=$firmX` on the cloned LA_Division parent) never appears in
    // the inverse-side collection (e.g. `$firmX.organizations`).
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
            // Group updates between leaf-set ('name = "X"') and
            // further-nested path-set ('address.name = "X"' against this
            // clone). Multi-level paths recurse via
            // `apply_path_property_updates`.
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
/// the class participates in — directly or via inheritance. The compiler
/// indexes `association_properties` only on the property's *declared*
/// target type (`PureModel::association_properties` keyed by the type
/// the association declared), so a subclass inherits no entries from
/// `model.derived.association_properties` directly. Without walking
/// supertypes, `^LA_Division(firm=$firmX)` (where `firm` is declared
/// on `LA_Organization`'s `LA_FirmOrganizations` association) wouldn't
/// produce any inverse on `$firmX.organizations`. Mirrors Java Pure's
/// `_ClassAccessor.allAssociations()`.
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

/// Java-faithful end-of-Copy inverse synchronisation
/// (parity with `New.java::updateReverseProperties` invoked from
/// `Copy.java:236`). Walks every association the object's classifier
/// participates in, reads the object's CURRENT slot value for the
/// injected (forward) side, and idempotently appends `obj` to each
/// target's inverse-side slot.
///
/// Reading the post-mutation slot value (rather than the kvs that
/// built it) makes the helper insensitive to which path the value
/// arrived through — carried-from-source, plain-override, path-clone
/// reassignment all converge on the same final state, and the inverse
/// reflects it exactly once. The idempotency guard mirrors Java's
/// `if (!currentValues.contains(instance))` at `New.java:318`.
///
/// Scope: only the `obj` passed in. Callers that produce multiple
/// affected instances (e.g. `Copy::execute` plus path-property clones)
/// must invoke this helper once per object.
#[allow(clippy::result_large_err)]
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
            continue; // n-ary associations: inverse isn't a single peer
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
/// classifier and a copy of every property. Used by the path-property
/// path so a `^$src(nested.field=val)` update lands on a new nested
/// instance rather than mutating the source's.
#[allow(clippy::result_large_err)]
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
/// freshly-constructed instance. Mirrors `Cast`'s primitive-constraint
/// path: bind `$this` to the instance and each
/// `type_variable_parameter` to its `^Class(value)(props)` argument,
/// evaluate the constraint body, raise `ConstraintViolation` on the
/// first failure with the canonical message shape.
///
/// Walks the supertype chain (most-specific first): evaluates the
/// leaf class's constraints, then each transitive parent's. A `visited`
/// set guards against multi-inheritance diamonds. Java parity:
/// constraints declared on parent classes apply to subclass instances.
/// Parent-level `type_variable_parameters` aren't currently rebound —
/// the supertype walk receives no value-args, so a parent that
/// references its own type-var inside its constraint body will see
/// the variable un-bound. Parametric-inheritance value-arg threading
/// is a follow-up (mirrors the primitive path's
/// `super_type_value_arguments` chain, which needs a Class-side
/// equivalent that doesn't exist yet on `Class.super_types: Vec<TypeExpr>`).
#[allow(clippy::result_large_err)]
fn evaluate_class_constraints(
    ctx: &mut dyn EvalContextTrait,
    class_id: ElementId,
    obj: ObjectHandle,
    type_var_values: &[Value],
) -> Result<(), PureException> {
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    evaluate_class_constraints_with_inheritance(ctx, class_id, obj, type_var_values, &mut visited)
}

#[allow(clippy::result_large_err)]
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
    // Evaluate this level's own constraints first. Type-var values
    // belong to this level (the caller supplies them for the leaf;
    // parent levels recurse with an empty slice — see note on
    // parametric-inheritance follow-up in the wrapper's doc).
    evaluate_class_constraints_at_level(ctx, class_id, obj.clone(), type_var_values)?;

    // Collect parent class ids before recursing — borrowing the model
    // through `ctx.model()` inside the recursive call would conflict
    // with the mutable evaluate path.
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

#[allow(clippy::result_large_err)]
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
            // Skip the unnamed root package.
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
    // `new`: the compiler-emitted positional shape (registered under a
    // synthetic key whose mangled segment sorts before the KeyExpression
    // overload, so the simple-name prefix fallback routes compiler-emitted
    // calls here) and the platform's two-argument form.
    registry.register("new_Class_1__String_1__Any_MANY__T_1_", New);
    registry.register("new_Class_1__String_1__T_1_", New);
    // `new`: the platform-declared KeyExpression shape.
    registry.register(
        "new_Class_1__String_1__KeyExpression_MANY__T_1_",
        NewWithKeyExpressions,
    );
    // `copy`: one implementation for the three platform signatures.
    registry.register("copy_T_1__KeyExpression_MANY__T_1_", Copy);
    registry.register("copy_T_1__String_1__T_1_", Copy);
    registry.register("copy_T_1__String_1__KeyExpression_MANY__T_1_", Copy);
}
