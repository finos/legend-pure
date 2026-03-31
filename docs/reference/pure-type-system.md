# Pure Type System Reference

This page covers the deeper mechanics of Pure's type system that are visible in
function signatures but rarely need to be understood for everyday usage. It is
a companion to the [Pure Language Reference](pure-language-reference.md).

---

## Type Hierarchy Overview

Pure has a **closed, single-rooted** type hierarchy with an explicit top type and
bottom type:

```
Any                          ← top: every type is a subtype of Any
 ├── String
 ├── Number
 │    ├── Integer
 │    ├── Float
 │    └── Decimal
 ├── Boolean
 ├── Date
 │    ├── StrictDate
 │    └── DateTime
 ├── <user-defined classes>
 └── <enumerations>
        ↑
       Nil                   ← bottom: Nil is a subtype of every type
```

---

## `Any` — the Top Type

`Any` is the root of the entire type hierarchy. Every type — primitive, class,
enumeration, or function type — is a *subtype* of `Any`.

Practical consequences:

- A parameter typed `Any[*]` **accepts** any value or collection.
- A return type of `Any[1]` promises very little to the caller — the value must
  be `cast` or `match`-ed before it can be used as anything more specific.
- `instanceOf(Any)` is always `true`.

`Any` is **abstract**: no runtime value ever has the concrete type `Any`. A
function that declares `Any[1]` as its return type must in practice return some
concrete subtype.

---

## `Nil` — the Bottom Type

`Nil` is the **bottom type**: it is a subtype of every other type in the
hierarchy. No runtime value ever has the type `Nil` — it exists purely to serve
the type system.

### Why `Nil` is needed — and why `Any` cannot replace it

This is the most common question about `Nil`. The answer is **function parameter
contravariance**.

#### Background: covariance and contravariance

- **Return types are covariant.** A subtype may return something *more specific*
  than its supertype declares.
- **Parameter types are contravariant.** For `F<{A->B}>` to be a subtype of
  `F<{C->B}>`, the parameter type `A` must be a *supertype* of `C` — not a
  subtype.

This is the standard Liskov rule: a function that handles any `Animal` can safely
stand in for a function that handles only `Dog`, but not vice versa.

#### Applying this to `match`

`match` must accept a list of branch lambdas with completely different parameter
types (`String[1]`, `Integer[1]`, `Person[1]`, …). Its signature must express
*"each branch is a function whose parameter can be any specific type the caller
chooses."*

**Option A — use `Any` as the lower bound (does not work):**

```pure
// Hypothetical — WRONG
match(var: Any[*], functions: Function<{Any[1]->T[m]}>[1..*]): T[m]
```

`Function<{Any[1]->T[m]}>` means a function whose parameter is *exactly* `Any`.
For `Function<{String[1]->T[m]}>` to be a subtype, contravariance requires
`String` to be a *supertype* of `Any`. But `String` is a *subtype* of `Any` —
the opposite direction — so this does not type-check. Every branch lambda would
be forced to accept `Any`, making the type system unable to verify that the branch
body handles the right type.

**Option B — use `Nil` as the lower bound (correct):**

```pure
// Actual signature
match<T,m,n>(var: Any[*], functions: Function<{Nil[n]->T[m]}>[1..*]): T[m]
```

`Function<{Nil[n]->T[m]}>` means a function whose parameter type is *at least*
`Nil`. For `Function<{String[1]->T[m]}>` to be a subtype, contravariance requires
`String` to be a *supertype* of `Nil`. Because `Nil` is the bottom type — a
subtype of *everything* — `String` **is** a supertype of `Nil`. ✅

The same holds for `Integer`, `Person`, `Any`, or any other type. `Nil` as the
lower bound is the unique type that makes **all** concrete branch lambdas valid
subtypes simultaneously.

#### Summary

| | `Any` | `Nil` |
|---|---|---|
| Position in hierarchy | Top — supertype of all | Bottom — subtype of all |
| Used as **output/return** type | ✅ Any value satisfies it | ❌ Nothing can satisfy it |
| Used as **parameter lower bound** | ❌ Callers must pass exactly `Any` | ✅ Any concrete type satisfies it |
| Role in `match` signature | Input value (`var: Any[*]`) | Branch parameter bound (`Nil[n]`) |

### `Nil` and empty collections

An empty collection literal `[]` has type `Nil[0]`. Because `Nil` is a subtype
of every type `T`, `Nil[0]` is assignable to `T[*]` or `T[0..1]` for any `T`:

```pure
let names: String[*]  = [];  // Nil[0] satisfies String[*]
let opt:   Person[0..1] = []; // Nil[0] satisfies Person[0..1]
```

If `[]` were typed as `Any[0]` it would only be directly assignable to `Any[*]`,
requiring an explicit cast for every other collection type — extremely tedious in
practice.

---

## Multiplicity Parameters in Generic Signatures

Just as a type parameter `T` stands for an unknown *type*, a **multiplicity
parameter** (conventionally `m`, `n`, `o`, …) stands for an unknown
*multiplicity*. Both are declared in the `<…>` type parameter list and inferred
by the compiler at each call site.

### Reading a signature with both kinds of parameter

```pure
letFunction<T, m>(left: String[1], right: T[m]): T[m]
```

`T` is the type parameter, `m` is the multiplicity parameter. The signature says:
*"whatever type and multiplicity `right` has, I return the same."*

```pure
let x  = 42;         // T=Integer, m=[1]  → Integer[1]
let ys = [1, 2, 3];  // T=Integer, m=[*]  → Integer[*]
```

### `if` — same multiplicity required on both branches

```pure
if<T,m>(test:    Boolean[1],
        valid:   Function<{->T[m]}>[1],
        invalid: Function<{->T[m]}>[1]): T[m]
```

Both branches must return the same type *and* multiplicity. If `valid` returns
`String[1]` then `invalid` must too.

### `match` — `m` for return, `n` for branch input

```pure
match<T,m,n>(var: Any[*], functions: Function<{Nil[n]->T[m]}>[1..*]): T[m]
```

- `m` — the **return** multiplicity; all branches must agree on it.
- `n` — the **input** multiplicity lower bound on each branch parameter. Each
  branch may declare its own concrete multiplicity (`String[1]`, `String[*]`,
  etc.); the compiler infers the tightest valid `n`.

### Writing your own multiplicity-generic function

```pure
function meta::mypackage::firstOrDefault<T, m>(
    col     : T[m],
    default : T[1]
): T[1]
{
    if($col->isEmpty(), | $default, | $col->toOne())
}
```

Multiplicity arguments are never supplied explicitly by the caller — the compiler
infers them from the actual arguments at each call site, exactly as it infers type
arguments.

---

*See also: [Pure Language Reference](pure-language-reference.md) ·
[Compiler Pipeline](../architecture/compiler-pipeline.md) ·
[Domain Concepts](../architecture/domain-concepts.md)*

