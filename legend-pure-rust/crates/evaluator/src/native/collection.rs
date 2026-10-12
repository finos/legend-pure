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

//! Collection natives: the element accessors (`at`, `first`, `last`, `init`,
//! `tail`, `take`, `drop`, `slice`, `size`, `isEmpty`, `range`, `add`,
//! `concatenate`, `reverse`, `toOne`, `indexOf`), the lambda-driven
//! iterators (`map`, `filter`, `fold`, `exists`, `forAll`, `find`,
//! `removeDuplicates`, `sort`), `zip`, and the `Map` natives (`newMap`,
//! `put`, `putAll`, `get`, `keys`, `values`).

use crate::value::PVector;
use legend_pure_metamodel::types::ValueSpec;
use smol_str::SmolStr;

use crate::error::{PureException, PureRuntimeError};
use crate::heap::ObjectHandle;
use crate::native::{ArgMode, Arity, force_then_values, value1, values2, values3};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all, force1,
    force2, force3,
};
use crate::value::{MapState, Value, ValueKey};

/// Pure `fold(T[*], Function<{T[1],V[m]->V[m]}>[1], V[m]): V[m]`
///
/// Accumulates a collection into a single value using a binary function.
#[derive(Debug)]
pub struct Fold;

impl NativeFunction for Fold {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(3))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "fold", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("fold", args, 3)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];
        let mut accumulator = args[2].clone();

        for item in &source {
            accumulator = ctx.call_function(lambda_val, &[item.clone(), accumulator])?;
        }

        Ok(Evaluated::new(accumulator))
    }
}

/// Pure `take(Any[*], Integer[1]): Any[*]` — first N elements.
#[derive(Debug)]
pub struct Take;

impl NativeFunction for Take {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "take", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = values2("take", args)?;
        let coll = a0.to_collection();
        #[allow(clippy::cast_sign_loss, clippy::cast_possible_truncation)]
        let n = a1.as_integer()?.max(0) as usize;
        let n = n.min(coll.len());
        Ok(Evaluated::new(Value::Collection(Box::new(coll.take(n)))))
    }
}

/// Pure `drop(Any[*], Integer[1]): Any[*]` — all elements after first N.
#[derive(Debug)]
pub struct Drop;

impl NativeFunction for Drop {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "drop", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = values2("drop", args)?;
        let coll = a0.to_collection();
        #[allow(clippy::cast_sign_loss, clippy::cast_possible_truncation)]
        let n = a1.as_integer()?.max(0) as usize;
        let n = n.min(coll.len());
        Ok(Evaluated::new(Value::Collection(Box::new(coll.skip(n)))))
    }
}

/// Pure `zip<T,U>(set1:T[*], set2:U[*]):Pair<T,U>[*]`
///
/// Pairwise combines two collections, truncating to the shorter length.
/// Each pair allocates a heap `Pair` object with `first` / `second`
/// slots, mirroring the shape produced by `keyValues` and consumed by
/// `pair()` / Map iteration.
#[derive(Debug)]
pub struct Zip;

impl NativeFunction for Zip {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "zip", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = values2("zip", args)?;
        let xs = collection_as_vec(a0);
        let ys = collection_as_vec(a1);
        let n = xs.len().min(ys.len());
        let mut out: Vec<Value> = Vec::with_capacity(n);
        for i in 0..n {
            let obj = ctx.heap_mut().alloc_dynamic(crate::m3_paths::PAIR);
            ctx.heap_mut().mutate_add(&obj, "first", &[xs[i].clone()])?;
            ctx.heap_mut()
                .mutate_add(&obj, "second", &[ys[i].clone()])?;
            out.push(Value::Object(obj));
        }
        Ok(Evaluated::new(Value::from_vec(out)))
    }
}

/// View any value as a `Vec<Value>` — `Collection` flattens to its
/// elements; `Unit` is empty; a scalar is wrapped in a singleton.
fn collection_as_vec(v: &Value) -> Vec<Value> {
    match v {
        Value::Collection(c) => c.iter().cloned().collect(),
        Value::Unit => Vec::new(),
        other => vec![other.clone()],
    }
}

/// Pure `first(Any[*]): Any[0..1]` — first element or Unit.
#[derive(Debug)]
pub struct First;

impl NativeFunction for First {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "first", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("first", args)?;
        let result = match a0 {
            Value::Collection(v) => v.front().cloned().unwrap_or(Value::Unit),
            Value::Unit => Value::Unit,
            other => other.clone(),
        };
        Ok(Evaluated::new(result))
    }
}

/// Pure `removeDuplicates<T,V>(col:T[*], key:Function<{T[1]->V[1]}>[0..1],
/// eql:Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*]`.
///
/// Removes duplicate elements, preserving first-occurrence order.
/// Both function arguments are optional (`[0..1]`). When `key` is
/// supplied each element is mapped through it before comparison;
/// when `eql` is supplied that function decides equality
/// (otherwise [`Value::eq`] is used).
#[derive(Debug)]
pub struct RemoveDuplicates;

impl NativeFunction for RemoveDuplicates {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Any)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "removeDuplicates", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = args;
        if values.is_empty() || values.len() == 2 || values.len() > 3 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "removeDuplicates: expected 1 or 3 argument(s), got {}",
                values.len()
            ))
            .into());
        }
        let source: Vec<Value> = values[0].to_collection().iter().cloned().collect();
        let key_fn: Option<Value> = values.get(1).filter(|v| !is_empty_optional(v)).cloned();
        let eql_fn: Option<Value> = values.get(2).filter(|v| !is_empty_optional(v)).cloned();

        let keys: Vec<Value> = match &key_fn {
            Some(kf) => {
                let mut out = Vec::with_capacity(source.len());
                for item in &source {
                    out.push(ctx.call_function(kf, std::slice::from_ref(item))?);
                }
                out
            }
            None => source.clone(),
        };

        let mut kept: Vec<usize> = Vec::with_capacity(source.len());
        for i in 0..source.len() {
            let mut is_dup = false;
            for &j in &kept {
                let eq = match &eql_fn {
                    Some(ef) => {
                        let result = ctx.call_function(ef, &[keys[j].clone(), keys[i].clone()])?;
                        result.as_boolean()?
                    }
                    None => keys[j] == keys[i],
                };
                if eq {
                    is_dup = true;
                    break;
                }
            }
            if !is_dup {
                kept.push(i);
            }
        }

        let out: Vec<Value> = kept.iter().map(|&i| source[i].clone()).collect();
        Ok(Evaluated::new(Value::from_vec(out)))
    }
}

/// Whether a `Value` should be treated as an absent optional argument
/// — i.e., the `[]` literal that the platform wrappers pass for
/// omitted `key`/`eql` parameters. Matches `Value::Unit` and any
/// empty collection.
fn is_empty_optional(v: &Value) -> bool {
    match v {
        Value::Unit => true,
        Value::Collection(items) => items.is_empty(),
        _ => false,
    }
}

/// Default comparator used by [`Sort`] when no user-supplied comparator
/// is provided. Routes through [`crate::native::comparison::compare_values`].
fn cmp_values(a: &Value, b: &Value) -> std::cmp::Ordering {
    crate::native::comparison::compare_values(a, b).cmp(&0)
}

/// Pure `sort<T,U|m>(col:T[m], key:Function[0..1], comp:Function[0..1]):T[m]`.
///
/// Sorts the collection stably. When `key` is provided (non-empty), each
/// element is first mapped through `key` and the resulting values drive
/// ordering — values stay associated with their original element. When
/// `comp` is provided, it returns an `Integer` (negative/zero/positive)
/// to order two items; otherwise, primitive natural order via
/// `cmp_values` is used. Both `key` and `comp` may independently be
/// `Unit` / `[]` to mean "not provided".
#[derive(Debug)]
pub struct Sort;

impl NativeFunction for Sort {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(3))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "sort", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1, a2) = values3("sort", args)?;
        let source = a0.to_collection();
        let key_fn = lambda_or_none(a1);
        let comp_fn = lambda_or_none(a2);

        let mut items: Vec<Value> = source.iter().cloned().collect();
        if key_fn.is_none() && comp_fn.is_none() {
            items.sort_by(cmp_values);
            return Ok(Evaluated::new(Value::from_vec(items)));
        }
        let keys: Vec<Value> = if let Some(k) = key_fn {
            let mut out = Vec::with_capacity(items.len());
            for it in &items {
                out.push(ctx.call_function(k, std::slice::from_ref(it))?);
            }
            out
        } else {
            items.clone()
        };

        let mut indices: Vec<usize> = (0..items.len()).collect();
        let mut cmp_err: Option<PureRuntimeError> = None;

        if let Some(cf) = comp_fn {
            indices.sort_by(|&i, &j| {
                if cmp_err.is_some() {
                    return std::cmp::Ordering::Equal;
                }
                match ctx.call_function(cf, &[keys[i].clone(), keys[j].clone()]) {
                    Ok(Value::Integer(n)) => n.cmp(&0),
                    Ok(other) => {
                        cmp_err = Some(PureRuntimeError::type_mismatch("Integer", &other));
                        std::cmp::Ordering::Equal
                    }
                    Err(e) => {
                        cmp_err = Some(PureRuntimeError::EvaluationError(format!("{e}")));
                        std::cmp::Ordering::Equal
                    }
                }
            });
        } else {
            indices.sort_by(|&i, &j| cmp_values(&keys[i], &keys[j]));
        }
        if let Some(e) = cmp_err {
            return Err(e.into());
        }

        let sorted: Vec<Value> = indices.into_iter().map(|i| items[i].clone()).collect();
        Ok(Evaluated::new(Value::from_vec(sorted)))
    }
}

/// Extract a `&Value::Function` from a lambda-valued argument, or `None` if
/// the arg is `Unit`/empty. Any other non-Unit value is treated as "not a
/// lambda" and returns `None` — matches the semantics of the optional
/// `Function[0..1]` parameter.
fn lambda_or_none(v: &Value) -> Option<&Value> {
    match v {
        Value::Function(_) => Some(v),
        _ => None,
    }
}

/// Pure `size(Any[*]): Integer[1]` — number of elements in a collection.
#[derive(Debug)]
pub struct Size;

impl NativeFunction for Size {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "size", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("size", args)?;
        let coll = a0.to_collection();
        #[allow(clippy::cast_possible_wrap)]
        Ok(Evaluated::new(Value::Integer(coll.len() as i64)))
    }
}

/// Pure `range(Integer[1], Integer[1], Integer[1]): Integer[*]`
/// Pure `range(Integer[1], Integer[1]): Integer[*]` — step defaults to 1
/// Pure `range(Integer[1]): Integer[*]` — start defaults to 0, step to 1
///
/// Generates integers from `start` (inclusive) to `stop` (exclusive)
/// with the given `step`. Java's stride direction follows the sign of
/// `step` — positive step ascends, negative step descends.
#[derive(Debug)]
pub struct Range;

impl NativeFunction for Range {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Any)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "range", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = args;
        let (start, end_exclusive, increment) = match values.len() {
            1 => (0i64, values[0].as_integer()?, 1i64),
            2 => (values[0].as_integer()?, values[1].as_integer()?, 1i64),
            3 => (
                values[0].as_integer()?,
                values[1].as_integer()?,
                values[2].as_integer()?,
            ),
            n => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "range: expected 1, 2, or 3 argument(s), got {n}"
                ))
                .into());
            }
        };
        if increment == 0 {
            return Err(
                PureRuntimeError::EvaluationError("range step must not be 0".into()).into(),
            );
        }
        let mut result = PVector::new();
        let mut i = start;
        let keep_going = |i: i64| {
            if increment > 0 {
                i < end_exclusive
            } else {
                i > end_exclusive
            }
        };
        while keep_going(i) {
            result.push_back(Value::Integer(i));
            let Some(next) = i.checked_add(increment) else {
                break;
            };
            i = next;
        }
        Ok(Evaluated::new(Value::Collection(Box::new(result))))
    }
}

/// Pure `map(T[*], Function<{T[1]->V[1]}>[1]): V[*]`
///
/// Applies a lambda to each element of a collection.
#[derive(Debug)]
pub struct Map;

impl NativeFunction for Map {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "map", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("map", args, 2)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];

        let mut results: Vec<Value> = Vec::with_capacity(source.len());
        for item in &source {
            let called = ctx.call_function(lambda_val, std::slice::from_ref(item))?;
            match &called {
                Value::Collection(inner) => {
                    for v in inner.iter() {
                        results.push(v.clone());
                    }
                }
                Value::Unit => {}
                _ => results.push(called),
            }
        }

        Ok(Evaluated::new(Value::from_vec(results)))
    }
}

/// Pure `slice<T>(T[*], Integer[1], Integer[1]): T[*]`
///
/// Returns the sub-collection from `start` (inclusive) to `end` (exclusive).
/// Indices are clamped to the collection bounds: negative indices become 0,
/// and indices past the end become the length. If `start >= end` after
/// clamping, the result is empty.
#[derive(Debug)]
pub struct Slice;

impl NativeFunction for Slice {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(3))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "slice", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1, a2) = values3("slice", args)?;
        let source = a0.to_collection();
        let start = a1.as_integer()?;
        let end = a2.as_integer()?;

        if start > end {
            return Err(PureRuntimeError::EvaluationError(format!(
                "The low bound ({start}) can't be higher than the high bound ({end}) in a slice operation"
            ))
            .into());
        }

        #[allow(clippy::cast_sign_loss, clippy::cast_possible_truncation)]
        let start = start.max(0) as usize;
        #[allow(clippy::cast_sign_loss, clippy::cast_possible_truncation)]
        let end = end.max(0) as usize;
        let start = start.min(source.len());
        let end = end.min(source.len());

        if start >= end {
            return Ok(Evaluated::new(Value::Unit));
        }

        let out: Vec<Value> = source
            .iter()
            .skip(start)
            .take(end - start)
            .cloned()
            .collect();
        Ok(Evaluated::new(Value::from_vec(out)))
    }
}

/// Pure `find<T>(T[*], Function<{T[1]->Boolean[1]}>[1]): T[0..1]`
///
/// Returns the first element for which the predicate returns `true`, or
/// [`Value::Unit`] if no element satisfies the predicate.
#[derive(Debug)]
pub struct Find;

impl NativeFunction for Find {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "find", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("find", args, 2)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];

        for item in &source {
            let pred = ctx.call_function(lambda_val, std::slice::from_ref(item))?;
            if pred.as_boolean()? {
                return Ok(Evaluated::new(item.clone()));
            }
        }

        Ok(Evaluated::new(Value::Unit))
    }
}

/// Pure `at(Any[*], Integer[1]): Any[1]` — element at index.
///
/// Pure uses 0-based indexing.
#[derive(Debug)]
pub struct At;

impl NativeFunction for At {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "at", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = values2("at", args)?;
        let coll = a0.to_collection();
        let idx = a1.as_integer()?;
        if idx < 0 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "at: index cannot be negative, got {idx}"
            ))
            .into());
        }
        #[allow(clippy::cast_sign_loss, clippy::cast_possible_truncation)]
        let idx = idx as usize;
        coll.get(idx).cloned().map(Evaluated::new).ok_or_else(|| {
            PureRuntimeError::EvaluationError(format!(
                "The system is trying to get an element at offset {idx} where the collection is of size {}",
                coll.len()
            ))
            .into()
        })
    }
}

/// Pure `exists<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):Boolean[1]`
///
/// Returns `true` as soon as any element satisfies the predicate. For an
/// empty input returns `false`.
#[derive(Debug)]
pub struct Exists;

impl NativeFunction for Exists {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "exists", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("exists", args, 2)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];

        for item in &source {
            let pred = ctx.call_function(lambda_val, std::slice::from_ref(item))?;
            if pred.as_boolean()? {
                return Ok(Evaluated::new(Value::Boolean(true)));
            }
        }

        Ok(Evaluated::new(Value::Boolean(false)))
    }
}

/// Pure `indexOf<T>(T[*], T[1]): Integer[1]`
///
/// Returns the 0-based index of the first element equal to the needle,
/// or `-1` when the needle is not found. Uses [`Value`] equality.
#[derive(Debug)]
pub struct IndexOf;

impl NativeFunction for IndexOf {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Any)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "indexOf", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = args;
        expect_args("indexOf", values, 2)?;
        let haystack = values[0].to_collection();
        let needle = &values[1];
        let ctx_ref: &dyn EvalContextTrait = ctx;
        let idx = haystack
            .iter()
            .position(|v| crate::native::equality::values_equal(ctx_ref, v, needle));
        let result = match idx {
            #[allow(clippy::cast_possible_wrap)]
            Some(i) => Value::Integer(i as i64),
            None => Value::Integer(-1),
        };
        Ok(Evaluated::new(result))
    }
}

/// Pure `tail<T>(set:T[*]):T[*]`
///
/// Returns everything but the first element. Mirror of [`Init`] (which
/// drops the last). Empty for `[]` or single-element collections.
#[derive(Debug)]
pub struct Tail;

impl NativeFunction for Tail {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "tail", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("tail", args)?;
        let result = match &a0 {
            Value::Collection(v) => {
                if v.is_empty() {
                    Value::Unit
                } else {
                    let mut trimmed = (**v).clone();
                    trimmed.pop_front();
                    let as_vec: Vec<Value> = trimmed.iter().cloned().collect();
                    Value::from_vec(as_vec)
                }
            }
            _ => Value::Unit,
        };
        Ok(Evaluated::new(result))
    }
}

/// Pure `toOne<T>(values:T[*]):T[1]` / `toOne<T>(values:T[0..1]):T[1]`
///
/// Asserts the collection contains exactly one element and returns that
/// element as a scalar. Produces [`PureRuntimeError::MultiplicityViolation`]
/// when the input is empty or has multiple elements.
#[derive(Debug)]
pub struct ToOne;

impl NativeFunction for ToOne {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Any)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "toOne", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = args;
        if values.is_empty() || values.len() > 2 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "toOne: expected 1 or 2 argument(s), got {}",
                values.len()
            ))
            .into());
        }
        Ok(Evaluated::new(values[0].to_one()?.clone()))
    }
}

/// Pure `filter(T[*], Function<{T[1]->Boolean[1]}>[1]): T[*]`
///
/// Filters a collection using a boolean predicate.
#[derive(Debug)]
pub struct Filter;

impl NativeFunction for Filter {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "filter", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("filter", args, 2)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];

        let mut results = Vec::with_capacity(source.len());
        for item in &source {
            let pred = ctx.call_function(lambda_val, std::slice::from_ref(item))?;
            if pred.as_boolean()? {
                results.push(item.clone());
            }
        }

        Ok(Evaluated::new(Value::from_vec(results)))
    }
}

/// Pure `concatenate(Any[*], Any[*]): Any[*]` — concatenate two collections.
#[derive(Debug)]
pub struct Concatenate;

impl NativeFunction for Concatenate {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "concatenate", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = values2("concatenate", args)?;
        let mut a = a0.to_collection();
        let b = a1.to_collection();
        a.append(b);
        Ok(Evaluated::new(Value::Collection(Box::new(a))))
    }
}

/// Pure `forAll<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):Boolean[1]`
///
/// Returns `true` iff every element satisfies the predicate. For an empty
/// input vacuously returns `true`.
#[derive(Debug)]
pub struct ForAll;

impl NativeFunction for ForAll {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(2))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "forAll", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        expect_args("forAll", args, 2)?;
        let source = args[0].to_collection();
        let lambda_val = &args[1];

        for item in &source {
            let pred = ctx.call_function(lambda_val, std::slice::from_ref(item))?;
            if !pred.as_boolean()? {
                return Ok(Evaluated::new(Value::Boolean(false)));
            }
        }

        Ok(Evaluated::new(Value::Boolean(true)))
    }
}

/// Pure `init<T>(set:T[*]):T[*]` — all but the last element.
///
/// Returns an empty collection for `Unit`, the input unchanged for a
/// single scalar (since there's no "last to drop" conceptually), and
/// every element except the final one for a multi-element collection.
/// The platform docstring is "the initial segment of the collection".
#[derive(Debug)]
pub struct Init;

impl NativeFunction for Init {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "init", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("init", args)?;
        let result = match &a0 {
            Value::Collection(v) => {
                if v.is_empty() {
                    Value::Unit
                } else {
                    let mut trimmed = (**v).clone();
                    trimmed.pop_back();
                    let as_vec: Vec<Value> = trimmed.iter().cloned().collect();
                    Value::from_vec(as_vec)
                }
            }
            _ => Value::Unit,
        };
        Ok(Evaluated::new(result))
    }
}

/// Pure `last(Any[*]): Any[0..1]` — last element or Unit.
#[derive(Debug)]
pub struct Last;

impl NativeFunction for Last {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "last", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("last", args)?;
        let result = match a0 {
            Value::Collection(v) => v.back().cloned().unwrap_or(Value::Unit),
            Value::Unit => Value::Unit,
            other => other.clone(),
        };
        Ok(Evaluated::new(result))
    }
}

/// Coerce a runtime [`Value`] into a hashable [`ValueKey`].
///
/// Primitives + dates map to their direct `ValueKey` variant. Heap objects
/// default to identity (`ValueKey::Object(ObjectHandle)`) unless the owning
/// class annotates one or more properties with `<<equality.Key>>` — in
/// that case the key extracts those properties' values and becomes
/// `ValueKey::ObjectByEqualityKeys`, so instances with equal annotated
/// fields collide in the same bucket even across distinct `ObjectHandle`s.
/// Collections, maps, and functions never compare meaningfully and raise
/// an explicit error.
fn value_to_key(v: &Value, ctx: &dyn EvalContextTrait) -> Result<ValueKey, PureException> {
    match v {
        Value::Boolean(b) => Ok(ValueKey::Boolean(*b)),
        Value::Integer(i) => Ok(ValueKey::Integer(*i)),
        Value::Decimal(d) => Ok(ValueKey::Decimal(*d)),
        Value::String(s) => Ok(ValueKey::String(s.clone())),
        Value::Date(d) => Ok(ValueKey::Date(*d)),
        Value::StrictTime(t) => Ok(ValueKey::StrictTime(*t)),
        Value::EnumValue { enum_id, member } => Ok(ValueKey::EnumValue {
            enum_id: *enum_id,
            member: member.clone(),
        }),
        Value::Object(id) => object_equality_key(id.clone(), ctx),
        other => Err(PureRuntimeError::EvaluationError(format!(
            "Map key must be a hashable primitive or object identity, got {}",
            other.type_name()
        ))
        .into()),
    }
}

/// Resolve a heap object into a `ValueKey`.
fn object_equality_key(
    id: ObjectHandle,
    ctx: &dyn EvalContextTrait,
) -> Result<ValueKey, PureException> {
    let classifier = ctx.heap().classifier(&id)?.to_string();
    let Some(class_id) = crate::m3_paths::resolve(ctx.model(), &classifier) else {
        return Ok(ValueKey::Object(id));
    };

    let equality_props = crate::native::equality::equality_key_properties(ctx.model(), class_id);

    if equality_props.is_empty() {
        return Ok(ValueKey::Object(id));
    }

    let mut fields: Vec<(SmolStr, ValueKey)> = Vec::with_capacity(equality_props.len());
    for prop_name in equality_props {
        let values = ctx.heap().get_property_values(&id, prop_name.as_str())?;
        let field_key = match values.iter().next() {
            Some(v) => value_to_key(v, ctx)?,
            None => ValueKey::String(SmolStr::new_static("")),
        };
        fields.push((prop_name, field_key));
    }
    Ok(ValueKey::ObjectByEqualityKeys { class_id, fields })
}

/// Inverse of [`value_to_key`] — recover a [`Value`] shape from a stored
/// key so `keys()` can emit the collection.
fn key_to_value(k: &ValueKey) -> Value {
    match k {
        ValueKey::Boolean(b) => Value::Boolean(*b),
        ValueKey::Integer(i) => Value::Integer(*i),
        ValueKey::Decimal(d) => Value::Decimal(*d),
        ValueKey::String(s) => Value::String(s.clone()),
        ValueKey::Date(d) => Value::Date(*d),
        ValueKey::StrictTime(t) => Value::StrictTime(*t),
        ValueKey::Object(id) => Value::Object(id.clone()),
        ValueKey::ObjectByEqualityKeys { .. } => Value::Unit,
        ValueKey::EnumValue { enum_id, member } => Value::EnumValue {
            enum_id: *enum_id,
            member: member.clone(),
        },
    }
}

/// Pure `newMap<U,V>(pairs:Pair<U,V>[*]):Map<U,V>[1]`
///
/// Fold a collection of `Pair` heap objects (each with `first` / `second`
/// properties) into an `im_rc::HashMap`-backed `Value::Map`. The second
/// overload (`newMap(pairs, p:Property[*])`) takes a property-list used
/// as an equality key by the Java runtime; it is ignored.
#[derive(Debug)]
pub struct NewMap;

impl NativeFunction for NewMap {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        if values.is_empty() {
            return Err(PureRuntimeError::EvaluationError(
                "newMap: expected at least 1 argument (the pairs)".into(),
            )
            .into());
        }
        let pairs = values[0].to_collection();
        let mut entries = im_rc::HashMap::new();
        for pair in &pairs {
            let Value::Object(obj_id) = pair else {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "newMap: expected Pair<U,V>, got {}",
                    pair.type_name()
                ))
                .into());
            };
            let first_vals = ctx.heap().get_property_values(&obj_id.clone(), "first")?;
            let second_vals = ctx.heap().get_property_values(&obj_id.clone(), "second")?;
            let Some(k) = first_vals.iter().next() else {
                return Err(PureRuntimeError::EvaluationError(
                    "newMap: Pair.first is empty".into(),
                )
                .into());
            };
            let Some(v) = second_vals.iter().next() else {
                return Err(PureRuntimeError::EvaluationError(
                    "newMap: Pair.second is empty".into(),
                )
                .into());
            };
            let key = value_to_key(k, ctx)?;
            entries.insert(key, v.clone());
        }
        Ok(Evaluated::new(Value::Map(new_map_cell(entries, 0))))
    }
}

/// Wrap a freshly-built [`MapState`] in the `Rc<RefCell<…>>` cell that
/// `Value::Map` carries.
fn new_map_cell(
    entries: im_rc::HashMap<ValueKey, Value>,
    get_if_absent_counter: i64,
) -> std::rc::Rc<std::cell::RefCell<MapState>> {
    std::rc::Rc::new(std::cell::RefCell::new(MapState {
        entries,
        get_if_absent_counter,
    }))
}

/// Pure `get<U,V>(m:Map<U,V>[1], key:U[1]):V[0..1]`
///
/// Returns the value bound to `key` or [`Value::Unit`] when the map has
/// no mapping.
#[derive(Debug)]
pub struct Get;

impl NativeFunction for Get {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("get", args, ctx)?;
        let Value::Map(m) = &a0 else {
            return Err(PureRuntimeError::type_mismatch("Map", &a0).into());
        };
        let key = value_to_key(&a1, ctx)?;
        match m.borrow().entries.get(&key) {
            Some(v) => Ok(Evaluated::new(v.clone())),
            None => Ok(Evaluated::new(Value::Unit)),
        }
    }
}

/// Pure `put<U,V>(m:Map<U,V>[1], key:U[1], value:V[1]):Map<U,V>[1]`
///
/// Returns a **new** map (fresh `Rc<RefCell<MapState>>`) carrying every
/// entry of `m` plus `key -> value`.
///
/// `m` is untouched, and the returned cell has `get_if_absent_counter` reset
/// to zero.
#[derive(Debug)]
pub struct Put;

impl NativeFunction for Put {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1, a2) = force3("put", args, ctx)?;
        let Value::Map(m) = &a0 else {
            return Err(PureRuntimeError::type_mismatch("Map", &a0).into());
        };
        let key = value_to_key(&a1, ctx)?;
        let mut updated = m.borrow().entries.clone();
        updated.insert(key, a2.clone());
        Ok(Evaluated::new(Value::Map(new_map_cell(updated, 0))))
    }
}

/// Pure `keys<U,V>(m:Map<U,V>[1]):U[*]`
///
/// Returns the map's keys as a collection. Ordering follows the
/// underlying HAMT's iteration order — deterministic per run, but not
/// insertion-ordered.
#[derive(Debug)]
pub struct Keys;

impl NativeFunction for Keys {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = force1("keys", args, ctx)?;
        let Value::Map(m) = &a0 else {
            return Err(PureRuntimeError::type_mismatch("Map", &a0).into());
        };
        let state = m.borrow();
        let items: Vec<Value> = state.entries.keys().map(key_to_value).collect();
        Ok(Evaluated::new(Value::from_vec(items)))
    }
}

/// Pure `values<U,V>(m:Map<U,V>[1]):V[*]`
///
/// Returns the values stored in the map as a flat collection. Iteration
/// order follows the HAMT — deterministic per run, not insertion-ordered.
/// Mirror of [`Keys`].
#[derive(Debug)]
pub struct Values;

impl NativeFunction for Values {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("values", &values, 1)?;
        let Value::Map(m) = &values[0] else {
            return Err(PureRuntimeError::type_mismatch("Map", &values[0]).into());
        };
        let snapshot: Vec<Value> = {
            let state = m.borrow();
            state.entries.values().cloned().collect()
        };
        Ok(Evaluated::new(Value::from_vec(snapshot)))
    }
}

/// Pure `putAll<U,V>(m:Map<U,V>[1], pairs:Pair<U,V>[*]):Map<U,V>[1]` /
/// `putAll<U,V>(m:Map<U,V>[1], o:Map<U,V>[1]):Map<U,V>[1]`
///
/// Returns a fresh map with all entries of `m` plus every entry from the
/// second argument (collection of `Pair` objects or another `Map`). The
/// second argument's bindings overwrite colliding keys in `m`.
#[derive(Debug)]
pub struct PutAll;

impl NativeFunction for PutAll {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let (a0, a1) = force2("putAll", args, ctx)?;
        let Value::Map(base) = &a0 else {
            return Err(PureRuntimeError::type_mismatch("Map", &a0).into());
        };
        let mut updated: im_rc::HashMap<ValueKey, Value> = base.borrow().entries.clone();
        match &a1 {
            Value::Map(other) => {
                // Snapshot the other map's entries before iterating so we
                // never hold a borrow on `other` across the insertion loop
                // (and so `other == base` aliasing stays sound).
                let other_entries: Vec<(ValueKey, Value)> = other
                    .borrow()
                    .entries
                    .iter()
                    .map(|(k, v)| (k.clone(), v.clone()))
                    .collect();
                for (k, v) in other_entries {
                    updated.insert(k, v);
                }
            }
            other => {
                for pair in &other.to_collection() {
                    let (k, v) = pair_first_second(ctx, pair, "putAll")?;
                    updated.insert(value_to_key(&k, ctx)?, v);
                }
            }
        }
        Ok(Evaluated::new(Value::Map(new_map_cell(updated, 0))))
    }
}

/// Read the `first` / `second` properties of a `Pair` heap object.
fn pair_first_second(
    ctx: &mut dyn EvalContextTrait,
    pair: &Value,
    native_name: &'static str,
) -> Result<(Value, Value), PureException> {
    let Value::Object(obj_id) = pair else {
        return Err(PureRuntimeError::EvaluationError(format!(
            "{native_name}: expected Pair<U,V>, got {}",
            pair.type_name()
        ))
        .into());
    };
    let first_vals = ctx.heap().get_property_values(&obj_id.clone(), "first")?;
    let second_vals = ctx.heap().get_property_values(&obj_id.clone(), "second")?;
    let Some(k) = first_vals.iter().next() else {
        return Err(PureRuntimeError::EvaluationError(format!(
            "{native_name}: Pair.first is empty"
        ))
        .into());
    };
    let Some(v) = second_vals.iter().next() else {
        return Err(PureRuntimeError::EvaluationError(format!(
            "{native_name}: Pair.second is empty"
        ))
        .into());
    };
    Ok((k.clone(), v.clone()))
}

/// Pure `isEmpty(Any[*]): Boolean[1]`
#[derive(Debug)]
pub struct IsEmpty;

impl NativeFunction for IsEmpty {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "isEmpty", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("isEmpty", args)?;
        Ok(Evaluated::new(Value::Boolean(a0.is_empty())))
    }
}

/// Pure `add<T|m>(T[m], T[1]): T[$1_MANY$]`
/// Pure `add<T|m>(T[m], Integer[1], T[1]): T[$1_MANY$]`
///
/// 2-arg form appends to the end; 3-arg form inserts at the given index
/// (0-based, clamped to `[0, len]`). Scalar inputs are promoted; empty
/// inputs yield a singleton.
#[derive(Debug)]
pub struct Add;

impl NativeFunction for Add {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Any)
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "add", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = args;
        match values.len() {
            2 => {
                let mut coll = values[0].to_collection();
                coll.push_back(values[1].clone());
                let as_vec: Vec<Value> = coll.into_iter().collect();
                Ok(Evaluated::new(Value::from_vec(as_vec)))
            }
            3 => {
                let mut coll = values[0].to_collection();
                let raw_index = values[1].as_integer()?;
                let len = i64::try_from(coll.len()).unwrap_or(i64::MAX);
                let index = raw_index.clamp(0, len);
                #[allow(clippy::cast_possible_truncation, clippy::cast_sign_loss)]
                coll.insert(index as usize, values[2].clone());
                let as_vec: Vec<Value> = coll.into_iter().collect();
                Ok(Evaluated::new(Value::from_vec(as_vec)))
            }
            n => Err(PureRuntimeError::EvaluationError(format!(
                "add: expected 2 or 3 argument(s), got {n}"
            ))
            .into()),
        }
    }
}

/// Pure `reverse<T|m>(T[m]): T[m]`
///
/// Returns the input collection in reverse order. A scalar input is
/// returned unchanged (a single-element reversal is a no-op). Empty
/// input yields [`Value::Unit`].
#[derive(Debug)]
pub struct Reverse;

impl NativeFunction for Reverse {
    fn arg_mode(&self) -> ArgMode {
        ArgMode::Eager(Arity::Exactly(1))
    }

    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        force_then_values(self, "reverse", args, ctx)
    }

    fn execute_values(
        &self,
        args: &[Value],
        _ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let a0 = value1("reverse", args)?;
        let source = a0.to_collection();
        let reversed: Vec<Value> = source.iter().rev().cloned().collect();
        Ok(Evaluated::new(Value::from_vec(reversed)))
    }
}

/// Register the fished collection natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("fold_T_MANY__Function_1__V_m__V_m_", Fold);
    registry.register("take_T_MANY__Integer_1__T_MANY_", Take);
    registry.register("drop_T_MANY__Integer_1__T_MANY_", Drop);
    registry.register("zip_T_MANY__U_MANY__Pair_MANY_", Zip);
    registry.register("first_T_MANY__T_$0_1$_", First);
    registry.register(
        "removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_",
        RemoveDuplicates,
    );
    registry.register("sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", Sort);
    registry.register("size_Any_MANY__Integer_1_", Size);
    registry.register(
        "range_Integer_1__Integer_1__Integer_1__Integer_MANY_",
        Range,
    );
    registry.register("map_T_MANY__Function_1__V_MANY_", Map);
    registry.register("map_T_m__Function_1__V_m_", Map);
    registry.register("map_T_$0_1$__Function_1__V_$0_1$_", Map);
    registry.register("slice_T_MANY__Integer_1__Integer_1__T_MANY_", Slice);
    registry.register("find_T_MANY__Function_1__T_$0_1$_", Find);
    registry.register("at_T_MANY__Integer_1__T_1_", At);
    registry.register("exists_T_MANY__Function_1__Boolean_1_", Exists);
    registry.register("indexOf_T_MANY__T_1__Integer_1_", IndexOf);
    registry.register("tail_T_MANY__T_MANY_", Tail);
    registry.register("toOne_T_MANY__T_1_", ToOne);
    registry.register("toOne_T_MANY__String_1__T_1_", ToOne);
    registry.register("filter_T_MANY__Function_1__T_MANY_", Filter);
    registry.register("concatenate_T_MANY__T_MANY__T_MANY_", Concatenate);
    registry.register("forAll_T_MANY__Function_1__Boolean_1_", ForAll);
    registry.register("init_T_MANY__T_MANY_", Init);
    registry.register("last_T_MANY__T_$0_1$_", Last);
    registry.register("put_Map_1__U_1__V_1__Map_1_", Put);
    registry.register("putAll_Map_1__Pair_MANY__Map_1_", PutAll);
    registry.register("putAll_Map_1__Map_1__Map_1_", PutAll);
    registry.register("isEmpty_Any_MANY__Boolean_1_", IsEmpty);
    registry.register("add_T_MANY__T_1__T_$1_MANY$_", Add);
    registry.register("add_T_MANY__Integer_1__T_1__T_$1_MANY$_", Add);
    registry.register("reverse_T_m__T_m_", Reverse);
    registry.register("get_Map_1__U_1__V_$0_1$_", Get);
    registry.register("keys_Map_1__U_MANY_", Keys);
    registry.register("values_Map_1__V_MANY_", Values);
    registry.register("newMap_Pair_MANY__Map_1_", NewMap);
    registry.register("newMap_Pair_MANY__Property_MANY__Map_1_", NewMap);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_collection, lit_int, lit_str};

    /// Build a `ValueSpec` for an integer collection.
    fn int_spec(items: &[i64]) -> ValueSpec {
        lit_collection(items.iter().map(|&i| lit_int(i)).collect())
    }

    /// Build the expected `Value::Collection` for assertions.
    fn int_collection(items: &[i64]) -> Value {
        let mut v = PVector::new();
        for &i in items {
            v.push_back(Value::Integer(i));
        }
        Value::Collection(Box::new(v))
    }

    #[test]
    fn take_elements() {
        assert_eq!(
            Take.execute(&[int_spec(&[1, 2, 3, 4, 5]), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[1, 2, 3])
        );
    }

    #[test]
    fn drop_elements() {
        assert_eq!(
            Drop.execute(&[int_spec(&[1, 2, 3, 4, 5]), lit_int(2)], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[3, 4, 5])
        );
    }

    #[test]
    fn first_of_collection() {
        assert_eq!(
            First
                .execute(&[int_spec(&[10, 20, 30])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(10)
        );
    }

    #[test]
    fn first_of_empty() {
        assert_eq!(
            First
                .execute(&[lit_collection(vec![])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Unit
        );
    }

    #[test]
    fn remove_duplicates_preserves_first_occurrence() {
        let input = int_spec(&[1, 2, 1, 3, 2, 4]);
        let result = RemoveDuplicates
            .execute(&[input], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(result, int_collection(&[1, 2, 3, 4]));
    }

    #[test]
    fn remove_duplicates_on_empty() {
        assert_eq!(
            RemoveDuplicates
                .execute(&[lit_collection(vec![])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Unit
        );
    }

    #[test]
    fn remove_duplicates_ignores_trailing_hasher_args() {
        let input = int_spec(&[1, 1, 2]);
        let result = RemoveDuplicates
            .execute(
                &[input, lit_collection(vec![]), lit_collection(vec![])],
                &mut MockCtx,
            )
            .unwrap()
            .into_value();
        assert_eq!(result, int_collection(&[1, 2]));
    }

    #[test]
    fn sort_integers_ascending() {
        assert_eq!(
            Sort.execute(
                &[
                    int_spec(&[3, 1, 2]),
                    lit_collection(vec![]),
                    lit_collection(vec![])
                ],
                &mut MockCtx,
            )
            .unwrap()
            .into_value(),
            int_collection(&[1, 2, 3])
        );
    }

    #[test]
    fn sort_single_element_is_noop() {
        assert_eq!(
            Sort.execute(
                &[lit_int(5), lit_collection(vec![]), lit_collection(vec![])],
                &mut MockCtx,
            )
            .unwrap()
            .into_value(),
            Value::Integer(5)
        );
    }

    #[test]
    fn sort_empty_returns_unit() {
        assert_eq!(
            Sort.execute(
                &[
                    lit_collection(vec![]),
                    lit_collection(vec![]),
                    lit_collection(vec![]),
                ],
                &mut MockCtx,
            )
            .unwrap()
            .into_value(),
            Value::Unit
        );
    }

    #[test]
    fn sort_mixed_types_succeeds_with_total_order() {
        let input = lit_collection(vec![lit_int(1), lit_str("oops")]);
        let r = Sort
            .execute(
                &[input, lit_collection(vec![]), lit_collection(vec![])],
                &mut MockCtx,
            )
            .unwrap()
            .into_value();
        let Value::Collection(c) = &r else {
            panic!("expected Collection, got {r:?}");
        };
        assert_eq!(c.len(), 2);
        assert_eq!(c[0], Value::Integer(1));
        assert_eq!(c[1], Value::String("oops".into()));
    }

    #[test]
    fn sort_wrong_arg_count() {
        assert!(Sort.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn size_of_collection() {
        assert_eq!(
            Size.execute(&[int_spec(&[1, 2, 3])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(3)
        );
    }

    #[test]
    fn size_of_scalar() {
        assert_eq!(
            Size.execute(&[lit_int(42)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
    }

    #[test]
    fn size_of_unit() {
        assert_eq!(
            Size.execute(&[lit_collection(vec![])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(0)
        );
    }

    #[test]
    fn is_empty_true() {
        assert_eq!(
            IsEmpty
                .execute(&[lit_collection(vec![])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn is_empty_false() {
        assert_eq!(
            IsEmpty
                .execute(&[lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(false)
        );
    }

    #[test]
    fn range_stops_at_the_edge_of_i64_instead_of_overflowing() {
        assert_eq!(
            Range
                .execute(
                    &[lit_int(i64::MAX - 1), lit_int(i64::MAX), lit_int(2)],
                    &mut MockCtx
                )
                .unwrap()
                .into_value(),
            int_collection(&[i64::MAX - 1])
        );
    }

    #[test]
    fn range_ascending() {
        let r = Range
            .execute(&[lit_int(0), lit_int(5), lit_int(1)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, int_collection(&[0, 1, 2, 3, 4]));
    }

    #[test]
    fn range_with_step() {
        let r = Range
            .execute(&[lit_int(0), lit_int(10), lit_int(3)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, int_collection(&[0, 3, 6, 9]));
    }

    #[test]
    fn range_descending() {
        let r = Range
            .execute(&[lit_int(5), lit_int(0), lit_int(-1)], &mut MockCtx)
            .unwrap()
            .into_value();
        assert_eq!(r, int_collection(&[5, 4, 3, 2, 1]));
    }

    #[test]
    fn slice_basic_range() {
        assert_eq!(
            Slice
                .execute(
                    &[int_spec(&[10, 20, 30, 40, 50]), lit_int(1), lit_int(4)],
                    &mut MockCtx,
                )
                .unwrap()
                .into_value(),
            int_collection(&[20, 30, 40])
        );
    }

    #[test]
    fn slice_empty_when_start_equals_end() {
        assert_eq!(
            Slice
                .execute(
                    &[int_spec(&[1, 2, 3]), lit_int(2), lit_int(2)],
                    &mut MockCtx,
                )
                .unwrap()
                .into_value(),
            Value::Unit
        );
    }

    #[test]
    fn slice_clamps_out_of_bounds() {
        assert_eq!(
            Slice
                .execute(
                    &[int_spec(&[1, 2, 3]), lit_int(1), lit_int(99)],
                    &mut MockCtx,
                )
                .unwrap()
                .into_value(),
            int_collection(&[2, 3])
        );
    }

    #[test]
    fn slice_type_mismatch_errors() {
        assert!(
            Slice
                .execute(
                    &[int_spec(&[1, 2, 3]), lit_str("1"), lit_int(2)],
                    &mut MockCtx,
                )
                .is_err()
        );
    }

    #[test]
    fn slice_wrong_arg_count() {
        assert!(
            Slice
                .execute(&[int_spec(&[1, 2, 3]), lit_int(0)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn find_wrong_arg_count() {
        assert!(Find.execute(&[int_spec(&[1])], &mut MockCtx).is_err());
    }

    #[test]
    fn at_valid_index() {
        assert_eq!(
            At.execute(&[int_spec(&[10, 20, 30]), lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(20)
        );
    }

    #[test]
    fn at_out_of_bounds() {
        assert!(
            At.execute(&[int_spec(&[10, 20]), lit_int(5)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn type_mismatch_errors() {
        assert!(
            At.execute(&[int_spec(&[1, 2]), lit_str("1")], &mut MockCtx)
                .is_err()
        );
        assert!(
            Range
                .execute(&[lit_str("1"), lit_int(5), lit_int(1)], &mut MockCtx)
                .is_err()
        );
        assert!(
            Take.execute(&[int_spec(&[1, 2]), lit_str("1")], &mut MockCtx)
                .is_err()
        );
        assert!(
            Drop.execute(&[int_spec(&[1, 2]), lit_str("1")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn wrong_arg_count_errors() {
        assert!(Size.execute(&[], &mut MockCtx).is_err());
        assert!(At.execute(&[int_spec(&[1])], &mut MockCtx).is_err());
        assert!(Range.execute(&[], &mut MockCtx).is_err());
        assert!(
            Range
                .execute(
                    &[lit_int(1), lit_int(2), lit_int(3), lit_int(4)],
                    &mut MockCtx,
                )
                .is_err()
        );
        assert!(Take.execute(&[int_spec(&[1])], &mut MockCtx).is_err());
    }

    #[test]
    fn index_of_found() {
        assert_eq!(
            IndexOf
                .execute(&[int_spec(&[10, 20, 30]), lit_int(20)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
    }

    #[test]
    fn index_of_not_found_returns_minus_one() {
        assert_eq!(
            IndexOf
                .execute(&[int_spec(&[10, 20, 30]), lit_int(99)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    #[test]
    fn index_of_empty_returns_minus_one() {
        assert_eq!(
            IndexOf
                .execute(&[lit_collection(vec![]), lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    #[test]
    fn index_of_wrong_arg_count() {
        assert!(IndexOf.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    fn to_one_scalar_passthrough() {
        assert_eq!(
            ToOne
                .execute(&[lit_int(42)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(42)
        );
        assert_eq!(
            ToOne
                .execute(&[int_spec(&[42])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(42)
        );
    }

    #[test]
    fn to_one_empty_errors() {
        assert!(
            ToOne
                .execute(&[lit_collection(vec![])], &mut MockCtx)
                .is_err()
        );
        assert!(ToOne.execute(&[int_spec(&[])], &mut MockCtx).is_err());
    }

    #[test]
    fn to_one_multi_errors() {
        assert!(ToOne.execute(&[int_spec(&[1, 2])], &mut MockCtx).is_err());
    }

    #[test]
    fn last_of_collection() {
        assert_eq!(
            Last.execute(&[int_spec(&[10, 20, 30])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(30)
        );
    }

    #[test]
    fn concatenate_collections() {
        assert_eq!(
            Concatenate
                .execute(&[int_spec(&[1, 2]), int_spec(&[3, 4])], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[1, 2, 3, 4])
        );
    }

    #[test]
    fn add_appends_to_collection() {
        assert_eq!(
            Add.execute(&[int_spec(&[1, 2]), lit_int(3)], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[1, 2, 3])
        );
    }

    #[test]
    fn add_to_empty_yields_singleton() {
        assert_eq!(
            Add.execute(&[lit_collection(vec![]), lit_int(1)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(1)
        );
    }

    #[test]
    fn add_to_scalar_promotes_to_collection() {
        assert_eq!(
            Add.execute(&[lit_int(1), lit_int(2)], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[1, 2])
        );
    }

    #[test]
    fn add_wrong_arg_count() {
        assert!(Add.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    fn reverse_multi_element() {
        assert_eq!(
            Reverse
                .execute(&[int_spec(&[1, 2, 3])], &mut MockCtx)
                .unwrap()
                .into_value(),
            int_collection(&[3, 2, 1])
        );
    }

    #[test]
    fn reverse_single_element_is_noop() {
        assert_eq!(
            Reverse
                .execute(&[lit_int(7)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(7)
        );
    }

    #[test]
    fn reverse_empty_returns_unit() {
        assert_eq!(
            Reverse
                .execute(&[lit_collection(vec![])], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Unit
        );
    }

    #[test]
    fn reverse_wrong_arg_count() {
        assert!(Reverse.execute(&[], &mut MockCtx).is_err());
    }
}
