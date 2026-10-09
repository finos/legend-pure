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

//! Heap-aware structural equality — the shared engine behind Pure `==`
//! (`equal`) and `<<equality.Key>>`-keyed map lookups.
//!
//! Pure's `equal` for object references is **not** identity. When a class
//! annotates one or more properties with the `<<equality.Key>>` stereotype
//! from `meta::pure::profiles::equality`, two distinct heap instances of
//! that class compare equal iff every annotated property compares equal
//! (recursively). Classes without any `<<equality.Key>>` property keep
//! identity semantics — `Value::Object(a) == Value::Object(b)` iff
//! `a == b`.
//!
//! `eq` / `is` stay strictly identity-based — those Pure natives do
//! *not* route through here.

use legend_pure_metamodel::annotations::StereotypeRef;
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use smol_str::SmolStr;

use std::cell::RefCell;
use std::collections::HashSet;

use crate::heap::{HeapEntry, ObjectHandle};
use crate::native::EvalContextTrait;
use crate::value::Value;

/// Stable handle-pair key used by the cycle guard.
///
/// `ObjectHandle = Rc<RefCell<HeapEntry>>`; two handles point at the
/// same heap entry iff their raw pointers match. Pairs are stored
/// canonically (smaller pointer first) so `(A, B)` and `(B, A)` map
/// to the same key.
type HandlePtr = *const RefCell<HeapEntry>;

/// Structural value equality, heap-aware. Iterative implementation —
/// uses an explicit work-stack instead of mutual recursion so deep
/// inputs (`List<List<…>>` chains, equality-keyed self-references,
/// long `Pair<U, Pair<…>>` walks) can't overflow the Rust stack.
/// Pathological depths just allocate more entries in the work-stack
/// `Vec` instead.
///
/// Primitives compare by value (delegating to `PartialEq for Value`).
/// Collections compare length then element-wise.
/// Heap objects compare via `<<equality.Key>>` stereotype if the class
/// declares any; otherwise fall back to `ObjectHandle` identity.
///
/// **Cycle handling.** A `<<equality.Key>>` property whose value
/// transitively references its owner produces an infinite walk under
/// naive recursion. The cycle guard records every object-pair we've
/// already started comparing; re-encountering the same pair short-
/// circuits to `true` — the standard "assume equal in cycles, fail on
/// concrete mismatches" semantics Java's `equals` uses on cyclic
/// graphs.
#[must_use]
pub fn values_equal(ctx: &dyn EvalContextTrait, a: &Value, b: &Value) -> bool {
    let mut work: Vec<(Value, Value)> = vec![(a.clone(), b.clone())];
    let mut seen: HashSet<(HandlePtr, HandlePtr)> = HashSet::new();
    while let Some((x, y)) = work.pop() {
        if !step_value_equal(ctx, &x, &y, &mut work, &mut seen) {
            return false;
        }
    }
    true
}

/// Single comparison step. Pushes child pairs onto `work` instead of
/// recursing. Returns `false` on a definite mismatch; returns `true` to
/// mean "this step doesn't disprove equality — keep popping the
/// work-stack."
fn step_value_equal(
    ctx: &dyn EvalContextTrait,
    a: &Value,
    b: &Value,
    work: &mut Vec<(Value, Value)>,
    seen: &mut HashSet<(HandlePtr, HandlePtr)>,
) -> bool {
    match (a, b) {
        (Value::Collection(xs), Value::Collection(ys)) => {
            if xs.len() != ys.len() {
                return false;
            }
            for (x, y) in xs.iter().zip(ys.iter()) {
                work.push((x.clone(), y.clone()));
            }
            true
        }
        (Value::Object(oa), Value::Object(ob)) => {
            push_object_key_pairs(ctx, oa.clone(), ob.clone(), work, seen)
        }
        // Cross-variant equality for metamodel references — when one
        // side is `Element(eid)` / `Function(Compiled(eid))` and the
        // other is the heap row produced by `bootstrap_metamodel`,
        // both views point at the same `ObjectHandle`. Without this
        // bridge `assertIs($cls, ^Class<…>(...)->class())` fails
        // because the bare `Element` and the materialised `Object`
        // wouldn't compare equal even though Java treats them as
        // the same `CoreInstance`. Bridges `Element ↔ Object`,
        // `Function(Compiled) ↔ Object`, and the symmetric
        // `Element ↔ Function(Compiled)` (same compiled-element id).
        (
            Value::Element(_) | Value::Function(_) | Value::Object(_),
            Value::Element(_) | Value::Function(_) | Value::Object(_),
        ) => {
            let oa = a.as_object_handle(ctx.heap());
            let ob = b.as_object_handle(ctx.heap());
            match (oa, ob) {
                (Some(la), Some(lb)) if std::rc::Rc::ptr_eq(&la, &lb) => true,
                (Some(la), Some(lb)) => push_object_key_pairs(ctx, la, lb, work, seen),
                _ => a == b,
            }
        }
        // Pure multiplicity coercion: a single-element collection equals
        // its scalar (`[x] == x` holds, `[x, y] == x` does not). The
        // heap's `apply_property_to_instance` returns `Value::from_vec`'d
        // results which collapse 1-element lists to the bare element,
        // while Element-side property access (`eval_function_property`'s
        // `expressionSequence`) explicitly wraps as `Value::Collection`.
        // Without this branch, `$f1.expressionSequence` (Collection) and
        // `$f2.expressionSequence` (bare Function) would compare unequal
        // even though Pure considers them the same value.
        (Value::Collection(xs), other) | (other, Value::Collection(xs)) if xs.len() == 1 => {
            work.push((xs[0].clone(), other.clone()));
            true
        }
        // Empty collection equals `Unit` — same multiplicity coercion
        // applied at the zero end. `from_vec(Vec::new())` produces
        // `Value::Unit`, so a heap slot with no values vs an explicit
        // empty Collection should still compare equal.
        (Value::Collection(xs), Value::Unit) | (Value::Unit, Value::Collection(xs))
            if xs.is_empty() =>
        {
            true
        }
        _ => a == b,
    }
}

/// Heap-object comparison step — walks the classifier, fetches the
/// `<<equality.Key>>`-annotated properties, and pushes each
/// per-property pair onto `work` for the outer loop to compare.
/// Returns `false` immediately on classifier mismatch / unresolved
/// classifier / no equality keys; returns `true` to mean "every key
/// pair has been queued — keep iterating."
///
/// Cycle guard records the (smaller-pointer, larger-pointer) pair so a
/// re-encounter short-circuits to `true` without re-pushing. Without
/// it, an `<<equality.Key>>` cycle (e.g. mutually-keyed self-references)
/// would re-push pairs forever.
fn push_object_key_pairs(
    ctx: &dyn EvalContextTrait,
    a: ObjectHandle,
    b: ObjectHandle,
    work: &mut Vec<(Value, Value)>,
    seen: &mut HashSet<(HandlePtr, HandlePtr)>,
) -> bool {
    if std::rc::Rc::ptr_eq(&a, &b) {
        return true;
    }

    // Canonical ordering: smaller pointer first, so (A,B) and (B,A) map
    // to the same key — equality is symmetric, so we should never
    // re-enter a comparison whose mirror was already in flight.
    let pa = std::rc::Rc::as_ptr(&a);
    let pb = std::rc::Rc::as_ptr(&b);
    let cycle_key = if pa <= pb { (pa, pb) } else { (pb, pa) };
    if !seen.insert(cycle_key) {
        // Already in the comparison frontier — assume equal so the
        // cycle resolves without infinite work. Concrete mismatches
        // elsewhere in the graph still surface via the outer loop.
        return true;
    }

    let Ok(a_classifier) = ctx.heap().classifier(&a) else {
        return false;
    };
    let Ok(b_classifier) = ctx.heap().classifier(&b) else {
        return false;
    };
    if a_classifier != b_classifier {
        return false;
    }
    let Some(class_id) = crate::m3_paths::resolve(ctx.model(), &a_classifier) else {
        return false; // unknown classifier — can't structurally compare
    };
    let keys = equality_key_properties(ctx.model(), class_id);
    if keys.is_empty() {
        return false; // no <<equality.Key>> → identity semantics (already checked above)
    }
    for prop in &keys {
        let Ok(av) = ctx.heap().get_property_values(&a, prop.as_str()) else {
            return false;
        };
        let Ok(bv) = ctx.heap().get_property_values(&b, prop.as_str()) else {
            return false;
        };
        if av.len() != bv.len() {
            return false;
        }
        for (x, y) in av.iter().zip(bv.iter()) {
            work.push((x.clone(), y.clone()));
        }
    }
    true
}

/// Property names annotated with `<<equality.Key>>` on the given Class,
/// in declaration order. Empty ⇒ class uses identity equality.
///
/// Resolves `meta::pure::profiles::equality` on the model once so every
/// stereotype-ref check is a cheap `ElementId` compare rather than a
/// string match. Returns empty if the equality profile isn't loaded.
#[must_use]
pub fn equality_key_properties(model: &PureModel, class_id: ElementId) -> Vec<SmolStr> {
    let Some(equality_profile) = crate::m3_paths::resolve(model, crate::m3_paths::EQUALITY_PROFILE)
    else {
        return Vec::new();
    };
    // Walk class + supertypes (BFS) so an `<<equality.Key>>` declared on
    // `TopClass` is honoured when comparing two `LeftClass extends
    // TopClass` instances. Keep declaration order: subclass properties
    // appear before inherited supertype properties.
    //
    // Override semantics: a subclass that redefines a property — with or
    // without the `<<equality.Key>>` stereotype — wins for that property
    // name. So `OtherBottomClass.sides : SideClass[*]` (no stereotype)
    // overrides `TopClass.<<equality.Key>> sides : SideClass[*]` and the
    // result excludes `sides` from equality keys for `OtherBottomClass`
    // instances. Tracking *all* seen property names (not just keys)
    // enforces this: when the BFS reaches the supertype with a key, the
    // name is already in `seen_names` and gets skipped.
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut queue: std::collections::VecDeque<ElementId> = std::collections::VecDeque::new();
    let mut seen_names: std::collections::HashSet<SmolStr> = std::collections::HashSet::new();
    let mut out: Vec<SmolStr> = Vec::new();
    queue.push_back(class_id);
    visited.insert(class_id);
    while let Some(cid) = queue.pop_front() {
        let Element::Class(class) = model.get_element(cid) else {
            continue;
        };
        for prop in &class.properties {
            if !seen_names.insert(prop.name.clone()) {
                continue; // subclass already defined (or redefined) this name
            }
            if prop
                .stereotypes
                .iter()
                .any(|s| is_equality_key_stereotype(s, equality_profile))
            {
                out.push(prop.name.clone());
            }
        }
        for super_ty in &class.super_types {
            if let legend_pure_metamodel::types::TypeExpr::Named { element, .. } = super_ty
                && visited.insert(*element)
            {
                queue.push_back(*element);
            }
        }
    }
    out
}

/// True when a stereotype reference is exactly `meta::pure::profiles::equality.Key`.
///
/// Both the profile identity and the stereotype label must match — a
/// stereotype named `Key` in some *other* profile does not qualify as
/// a structural-equality marker.
#[must_use]
pub fn is_equality_key_stereotype(stereo: &StereotypeRef, equality_profile: ElementId) -> bool {
    stereo.profile == equality_profile && stereo.value.as_str() == "Key"
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::MockCtx;
    use im_rc::Vector;

    /// Build `Collection(Collection(... Integer(leaf) ...))` `depth`
    /// levels deep.
    fn deep_nested_collection(depth: usize, leaf: i64) -> Value {
        let mut v = Value::Integer(leaf);
        for _ in 0..depth {
            let mut inner = Vector::new();
            inner.push_back(v);
            v = Value::Collection(Box::new(inner));
        }
        v
    }

    /// Locks the iterative-equality + iterative-`Drop` +
    /// iterative-`Clone` triple. The old recursive `values_equal`
    /// hit `MAX_EQUALITY_DEPTH = 1000` and silently returned `false`;
    /// the auto-derived `Drop` overflowed around 3 000–5 000; the
    /// derived `Clone` (used to seed `work` from `(a.clone(),
    /// b.clone())` and re-clone children on every push) overflowed at
    /// the same level. All three are now bounded by heap allocations,
    /// not Rust frames.
    ///
    /// Depth 3 000 is comfortably past every previous overflow point
    /// while keeping the test under a couple of seconds — the
    /// iterative equality clones each child pair onto the work-stack
    /// via the new iterative `Value::Clone`, so this is `O(depth²)`
    /// total work (~9 million ops at 3 000; depth 10 000 would be
    /// ~100 million → 30+ s in debug builds). The "no overflow at
    /// all" leg of the contract is locked separately and cheaply by
    /// `value::tests::deep_nested_collection_{drops,clones}_without_overflow`
    /// at depth 10 000.
    #[test]
    fn deeply_nested_collections_compare_equal_past_old_recursion_cap() {
        let a = deep_nested_collection(3_000, 42);
        let b = deep_nested_collection(3_000, 42);
        let ctx = MockCtx;
        assert!(values_equal(&ctx, &a, &b));
    }

    /// Same depth but the leaf differs — must still terminate (no
    /// overflow) and return `false`.
    #[test]
    fn deeply_nested_collections_compare_unequal_at_leaf() {
        let a = deep_nested_collection(3_000, 1);
        let b = deep_nested_collection(3_000, 2);
        let ctx = MockCtx;
        assert!(!values_equal(&ctx, &a, &b));
    }

    /// Multiplicity coercion (`[x] == x`) still works through the new
    /// iterative path. Locks one of the trickier branches of the
    /// dispatch that the old recursive form encoded inline.
    #[test]
    fn singleton_collection_equals_scalar() {
        let mut single = Vector::new();
        single.push_back(Value::Integer(7));
        let a = Value::Collection(Box::new(single));
        let b = Value::Integer(7);
        let ctx = MockCtx;
        assert!(values_equal(&ctx, &a, &b));
        assert!(values_equal(&ctx, &b, &a)); // symmetric
    }

    /// Empty collection equals `Unit`. Same multiplicity-coercion
    /// branch, zero-end.
    #[test]
    fn empty_collection_equals_unit() {
        let empty = Value::Collection(Box::new(Vector::new()));
        let unit = Value::Unit;
        let ctx = MockCtx;
        assert!(values_equal(&ctx, &empty, &unit));
        assert!(values_equal(&ctx, &unit, &empty));
    }
}
