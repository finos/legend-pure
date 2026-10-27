# ADR-008: `ColSpecArray` Shared Type-Parameter Widening for `=`

**Status:** Accepted
**Date:** 2026-10-16
**Deciders:** Legend Pure core team

## Context

`ColSpecArray<Z=(?:K)⊆T>` — binding a single type parameter `K` to the type of every column
selected by a `ColSpecArray`, for later reuse elsewhere in the same function signature (the same
mechanism `ColSpec<Z=(?:K)⊆T>` already uses successfully for exactly one column, in
`rename`/`flatten`) — already parses today with no grammar changes, but was never correctly wired
up for more than one column.

- `FunctionExpressionProcessor.processEmptyColumnType` resolves each selected column's type against
  the reference relation `T`, then calls `TypeInferenceContext.register` to bind the `(?:K)`
  template — once per column, passing the *entire* multi-column relation each time, rather than
  each column's own scalar type.
- Because `(?:K)` is not itself a named type parameter (it is a one-column wildcard `RelationType`
  wrapping `K`), this call bypasses `register`'s named-parameter merge logic entirely and falls into
  its generic structural `RelationType`-to-`RelationType` alignment path, which calls
  `_RelationType.alignColumnSets`.
- `alignColumnSets` pairs the wildcard template column against the *value* relation's columns
  positionally and discards everything it can't align name-wise — for a one-column template against
  an N-column value, this means only the value's **first** column is ever examined; columns 1..N-1
  are silently dropped from `K`'s resolution, every time, regardless of their type.

The practical effect: `K` always resolves to whichever column appears first in the literal
(`~[a,b]` → `K` = `a`'s type; `~[b,a]` → `K` = `b`'s type), with no compatibility check against the
other columns and no error, however incompatible those other columns actually are. Today nothing in
`legend-pure` declares a function using this form on a `ColSpecArray` (only the single-column
`ColSpec` form is used), so this is a correctness gap with no current blast radius — worth fixing
properly now rather than leaving a silently-wrong compiler behavior in place for the first real
consumer to discover.

Not every one-column wildcard template on a `ColSpecArray` expresses a *shared* type, though. The
same `(?:…)` syntax is also used with a fully anonymous type slot — `ColSpecArray<(?:?)⊆T>` — purely
to say "any columns of `T`", e.g. for a list of partition or grouping keys. Those columns are
independent of each other by design (an `Integer` key alongside a `Date` key is perfectly normal),
and nothing in the signature reads the anonymous binding back out. Syntactically, `(?:K)` and `(?:?)`
both produce a column whose `GenericType` has no `rawType`; the only difference is that `(?:K)` carries
a `typeParameter` named `K` while `(?:?)` carries none (see `AntlrContextToM3CoreInstance.type`'s
handling of the `QUESTION` token).

## Decision

In `processEmptyColumnType`, when a `ColSpecArray` selects more than one column and the per-column
template is a one-column wildcard `RelationType` whose type slot is something to bind or check, stop
registering the whole relation against it once per column. Instead:

1. Decide whether the template's type slot is a shared slot. It is when the template has exactly one
   column and that column's type is either:
   - a named type parameter (`(?:K)`), or a type containing one (`(?:List<K>)`) — something to infer; or
   - a fully concrete type (`(?:Integer)`) — a declared pin to validate against.

   A fully anonymous slot (`(?:?)` — no `rawType`, no type-parameter name) is *not* a shared slot:
   each selected column keeps its own independent type and is registered individually, exactly as for
   any ordinary structural match. Likewise a template with more than one column
   (`(a:Integer, b:String)`) describes a relation shape, not a shared column type, and takes the
   ordinary structural path.
2. Collect each selected column's own resolved type into a list as the per-column loop fills it in
   (the N=1 `ColSpec` path is left completely untouched — it already works, by construction, since
   there is no second column for `alignColumnSets` to discard).
3. After all columns are processed, merge the collected types once via
   `GenericType.findBestCommonGenericType(..., covariant=true, ...)` — the same general
   "repeated type parameter" unification Pure already uses everywhere else a type parameter is bound
   more than once (`match` branches, ordinary functions reusing a type parameter across arguments).
   `covariant=true` is required here, not `false`: the two have opposite directions (least-upper-bound
   widening vs. greatest-lower-bound narrowing), and the wrong one silently collapses *any* pair of
   columns — including same-typed ones — to `Nil`, the opposite of the intended behavior.
4. If the slot still contains a type parameter (`(?:K)`, or a partly concrete `(?:List<K>)`), merging
   is the right operation: if the merged type is `Any` or `Nil` — the columns don't share anything
   more specific — raise a `PureCompilationException` naming the offending columns' types, rather
   than silently widening to `Any` the way Pure's general convention does for ordinary repeated type
   parameters. This is primarily because `Any` in a `Relation` column is unlikely to work in many
   places, and it's easier to relax in the future if needed rather than open up now. The "contains a
   type parameter" test must be *full* concreteness (`GenericType.isGenericTypeFullyConcrete`), not
   just "has a raw type": `(?:List<K>)` has a concrete raw type (`List`) but still has `K` to bind.
5. If the slot is instead fully concrete (e.g. `ColSpecArray<Z=(?:Integer)⊆T>`, a declared pin
   rather than an inferred parameter), merging is the *wrong* operation: there is nothing to infer,
   and reusing the same "reject only if the merge reaches `Any`/`Nil`" check would be incorrect, since
   two columns can both be individually incompatible with the declared type while still merging to
   something more specific than `Any` with each other (e.g. `Integer` and `Float` against a declared
   `Integer` pin merge to `Number`, which isn't `Any`/`Nil`, yet `Float` plainly doesn't satisfy "must
   be `Integer`"). Instead, each collected column's type is checked independently for compatibility
   with the declared type via `GenericType.isGenericCompatibleWith`, raising a compile error naming
   the first incompatible column found. This also closes a second, narrower gap in the
   *pre-existing* per-column compatibility check (`isGenericCompatibleWith` called inline in the main
   loop): that check is itself only ever applied to the first selected column (the same
   `alignColumnSets` positional-only limitation described above), so a hardcoded `(?:X)` pin whose
   first column happens to match would previously let any later, non-matching column through
   unchecked.
6. Register the slot once, directly, against the merged type — but only in the type-parameter case
   (point 4); the fully concrete case (point 5) registers nothing, since there is no parameter value
   to bind. Either way, this bypasses the `(?:K)` relation-wrapping and `alignColumnSets` entirely,
   since that machinery was never designed for "N columns sharing one type parameter"; it exists for
   ordinary relation-shape compatibility, where a column-count mismatch is a legitimate "this column
   doesn't exist on the other side," not something to special-case for a shared wildcard binding.
7. When filling in a bare column's type from the reference relation, copy its full generic type —
   type arguments and multiplicity arguments as well as the raw type and type-variable values. Copying
   only the raw type turned a `List<Integer>` column into a bare `List`, which fails validation; this
   affected any `ColSpec`/`ColSpecArray` over a parameterized column type, and is required for a
   partly concrete slot like `(?:List<K>)` to bind `K` at all.

## Consequences

**Positive:**

- `ColSpecArray<Z=(?:K)⊆T>` now does what its syntax implies for any number of selected columns,
  closing a silent-wrong-result gap before any real consumer (e.g. a future `unpivot` declared with
  this form) hits it. The same fix also makes `ColSpecArray<Z=(?:X)⊆T>` for a *hardcoded* concrete
  `X` (e.g. `(?:Integer)`) correctly validate every selected column, not just the first.
- Anonymous `ColSpecArray<(?:?)⊆T>` key lists keep their existing semantics: columns of unrelated
  types can still be selected together.
- `ColSpec`/`ColSpecArray` selection now works over parameterized column types (`List<Integer>` etc.).
- Reuses existing primitives (`findBestCommonGenericType`, the same pattern `TypeInferenceContext`
  already applies for ordinary repeated type parameters) rather than introducing new merge logic.
- The N=1 `ColSpec` path (`rename`, `flatten`) is untouched by the merge logic, so there is no
  regression risk to production code.

**Negative:**

- `ColSpecArray`'s `=` now has stricter semantics than Pure's general repeated-type-parameter
  convention (rejects `Any`/`Nil` instead of allowing it). This is a deliberate, documented
  divergence, not an oversight, but it means `=` is no longer a drop-in special case of the general
  rule — a future reader needs this ADR to understand why.
- Whether a `ColSpecArray` column list must share a type now depends on the template's slot: `(?:K)`
  and `(?:Integer)` constrain the columns, `(?:?)` does not. This follows directly from what each form
  says, but it is a distinction signature authors need to be aware of.
- `alignColumnSets`' own "discard everything but the first positionally-aligned column" behavior for
  a wildcard template against a larger value relation is unchanged (it's correct for its actual
  intended callers; only this one call site's usage of it was wrong). It remains a sharp edge for
  any other future code that calls `register` with a one-column wildcard template against a
  multi-column value relation without first collecting-and-merging the way this fix does.

## Rejected

- **Allowing the merge to reach `Any`/`Nil` silently**, matching Pure's general convention for
  ordinary repeated type parameters. Rejected because `Any` in a `Relation` column is unlikely to be
  usable in most downstream contexts, and a stricter default is easier to relax later than to tighten
  once callers depend on the lenient behavior — see Decision point 4.
- **Merging every multi-column `ColSpecArray`, regardless of the template's slot.** Rejected because
  an anonymous `(?:?)` slot expresses no shared type: its columns are independent by design, and
  forcing them to a common type would reject perfectly valid key lists such as an `Integer` column
  alongside a `Date` column — see Decision point 1.
- **Applying the same "merge, then reject only `Any`/`Nil`" check to the fully concrete case
  (point 5)** instead of giving it its own independent per-column validation. Rejected because it's
  observably wrong: `Integer`+`Float` against a declared `Integer` pin merge to `Number`, which would
  pass an `Any`/`Nil`-only check despite `Float` not actually satisfying the declared type.
- **Fixing `_RelationType.alignColumnSets`/`canConcatenate`/`merge` directly** to understand "a
  wildcard template column should merge against every value column, not just the first." These are
  general-purpose `RelationType` structural-matching primitives used well beyond this one call site;
  changing their semantics has much broader blast radius than fixing the one call site that was
  using them incorrectly for a purpose they were never designed for.
