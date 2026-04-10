# The Pure Language Reference

Pure is the strongly-typed, functional, expression-oriented language at the heart of
Legend Pure. This page is a practical reference for engineers who need to read, write,
or reason about Pure source code.

For the compiler internals that process this source, see the
[Compiler Pipeline](../architecture/compiler-pipeline.md).
For the `###Mapping` and `###Relational` grammars, see the
[Legend Grammar Reference](legend-grammar-reference.md).

---

## 0. The Grammar Section System

A Legend Pure source file is not a single flat language. It is a **multi-section
document** where each section is written in a different grammar, declared by a
`###<GrammarName>` header on its own line.

```pure
###Pure
// Pure language — classes, functions, enumerations, associations

###Mapping
// Mapping DSL — maps domain model to store/source implementations

###Relational
// Relational DSL — defines database schemas, tables, joins
```

The top-level lexer (`TopAntlrLexer.g4`) splits the file on the `\n###` token.
Everything after a `###<Name>` line until the next `###` belongs to the parser
registered under that name. The `ParserLibrary` dispatches each section to its
corresponding `Parser` implementation based on the string name.

### Grammar sections available in this repository

| Section header | Parser | What it defines |
|---------------|--------|----------------|
| `###Pure` | `M3AntlrParser` | Classes, functions, enumerations, associations, profiles — the full Pure language documented on this page |
| `###Mapping` | `MappingParser` | Mappings from domain model to store implementations — Pure, Relational, Enumeration, XStore, Operation |
| `###Relational` | `RelationalParser` | Database schemas — tables, columns, joins, views, filters |
| `###Diagram` | `DiagramParser` | UML-style class diagrams (layout only, no execution semantics) |

> **Additional grammar sections** (`###Connection`, `###Runtime`, `###Service`,
> `###DataSpace`, etc.) are defined in `legend-engine` on top of this foundation
> and are not part of this repository.

### The `###Pure` section

The rest of this page documents the `###Pure` grammar. Everything within a
`###Pure` section — classes, functions, enumerations, associations, and profiles —
follows the syntax described below.

A single file may contain **multiple sections of different types**, or multiple
sections of the same type. Elements from a `###Pure` section can be referenced
by name in a `###Mapping` or `###Relational` section in the same file or in a
different file in the same repository.

```pure
###Pure
import my::model::*;

Class my::model::Person
{
    firstName : String[1];
    lastName  : String[1];
}

###Mapping
import my::model::*;

Mapping my::mapping::PersonMapping
(
    Person : Pure
    {
        ~src Person
        firstName : $src.firstName,
        lastName  : $src.lastName
    }
)
```

---

## 1. Language Basics

### Everything is an Expression

Pure has no statements — every construct is an expression that evaluates to a value.
A function body is a single expression (which may be a sequence of `let` bindings
ending in a final expression).

### Variables

Variables are **immutable** and declared with `let`:

```pure
let x = 42;
let name = 'Alice';
let people = [^Person(firstName='Alice'), ^Person(firstName='Bob')];
```

The `$` sigil is used to reference a variable:

```pure
let doubled = $x * 2;
```

### Comments

```pure
// Single-line comment

/* Multi-line
   comment */
```

---

## 2. Types and Primitives

### Built-in Primitive Types

| Type | Example literals | Notes |
|------|-----------------|-------|
| `String` | `'hello'`, `'it\'s'` | Single-quoted; escape with `\'` |
| `Integer` | `42`, `-7` | 64-bit signed |
| `Float` | `3.14`, `-0.5` | 64-bit IEEE 754 |
| `Decimal` | `3.14d` | Arbitrary precision (precisePrimitives module) |
| `Boolean` | `true`, `false` | |
| `Date` | `%2024-01-15`, `%2024-01-15T10:30:00` | Legend date literal |
| `StrictDate` | `%2024-01-15` | Calendar date, no time |
| `DateTime` | `%2024-01-15T10:30:00+0000` | Date + time with optional TZ offset |
| `Number` | *(abstract)* | Supertype of `Integer`, `Float`, `Decimal` |
| `Any` | *(abstract)* | Root type; every Pure type is a subtype |

### `Any` and `Nil`

Two special types appear in function signatures throughout the standard library:

- **`Any`** — the top type. Every type is a subtype of `Any`. Use `Any[*]` when
  a parameter must accept values of any type. Return type `Any[1]` means the
  caller must `cast` or `match` before using the result as anything specific.

- **`Nil`** — the bottom type. `Nil` is a subtype of every type. It never appears
  in user code directly; the compiler uses it in two places:
  - As the parameter lower bound in `match` branch lambdas
    (`Function<{Nil[n]->T[m]}>`) so that branches with any concrete parameter
    type are accepted.
  - As the type of an empty collection literal `[]` (`Nil[0]`), making it
    assignable to any `T[*]` or `T[0..1]`.

> For a full explanation of why `Nil` is needed and why `Any` cannot replace it
> (function parameter contravariance, covariance vs contravariance, multiplicity
> parameters in generic signatures), see the
> [Pure Type System Reference](pure-type-system.md).

### Multiplicity (Cardinality)

Every property and function parameter carries a multiplicity annotation:

| Notation | Meaning |
|----------|---------|
| `[1]` | Exactly one — mandatory |
| `[0..1]` | Zero or one — optional |
| `[*]` | Zero or more — collection |
| `[1..*]` | One or more |
| `[2..5]` | Between 2 and 5 inclusive |

```pure
name    : String[1];    // required
nickname: String[0..1]; // optional
aliases : String[*];    // collection
```

### Multiplicity parameters in generic signatures

Multiplicity parameters (conventionally `m`, `n`, `o`, …) appear alongside type
parameters in signatures like `letFunction<T,m>`, `if<T,m>`, and
`match<T,m,n>` — they allow a function to work at any cardinality. The compiler
infers them at each call site; you never supply them explicitly.

> For a full explanation with worked examples see
> [Pure Type System Reference — Multiplicity Parameters](pure-type-system.md#multiplicity-parameters-in-generic-signatures).

---

## 3. Defining Types

### Class

```pure
Class meta::mypackage::Person
{
    firstName : String[1];
    lastName  : String[1];
    age       : Integer[0..1];
    // Derived property (computed, not stored)
    fullName() { $this.firstName + ' ' + $this.lastName } : String[1];
}
```

### Class with Constraints

```pure
Class meta::mypackage::PositiveAmount
[
    mustBePositive: $this.value > 0
]
{
    value : Float[1];
}
```

### Class with Generics

```pure
Class meta::mypackage::Pair<A, B>
{
    first  : A[1];
    second : B[1];
}
```

### Enumeration

```pure
Enum meta::mypackage::TradeStatus
{
    PENDING,
    CONFIRMED,
    SETTLED,
    CANCELLED
}
```

Usage:

```pure
let status = TradeStatus.CONFIRMED;
```

### Association

Associations define bidirectional relationships between classes without modifying
either class:

```pure
Association meta::mypackage::PersonFirm
{
    person : Person[*];
    firm   : Firm[1];
}
```

### Profile - Tag and Stereotype

```pure
Profile meta::mypackage::classification
{
    stereotypes: [internal, external, deprecated];
    tags: [description, owner];
}

Class <<meta::mypackage::classification.internal>> meta::mypackage::InternalModel
{
    // ...
}
```

**Applying a tag** — use `{Profile.tag = 'value'}` after the stereotype list
(or directly before the element keyword if there are no stereotypes):

```pure
Class <<meta::mypackage::classification.internal>>
      {meta::mypackage::classification.description = 'Internal pricing model',
       meta::mypackage::classification.owner = 'risk-team'}
    meta::mypackage::InternalModel
{
    name : String[1];
}
```

Both stereotypes and tags can be applied together, and multiple values are separated by commas inside the curly braces.

#### Built-in profiles

Pure ships several profiles out of the box. Each must be imported before the short
name can be used.

##### `meta::pure::profiles::doc` — Documentation

```pure
import meta::pure::profiles::*;
```

| Annotation | Kind | Usage |
|-----------|------|-------|
| `<<doc.deprecated>>` | Stereotype | Marks an element as deprecated — tools and linters may warn on use |
| `{doc.doc = '…'}` | Tag | Human-readable documentation string attached to any element |
| `{doc.todo = '…'}` | Tag | Inline note recording outstanding work |

```pure
// Deprecated enum value with a doc string on a sibling value
Enum meta::mypackage::GeoType
{
    <<doc.deprecated>> COUNTRY,
    {doc.doc = 'A city, town, village, or other urban area.'} CITY,
    REGION
}

// Deprecated class
Class <<doc.deprecated>> {doc.doc = 'Use NewThing instead.'}
    meta::mypackage::OldThing
{
    name : String[1];
}

// Function with a doc string
function {doc.doc = 'Returns the full name of a person.'}
    meta::mypackage::fullName(p: Person[1]): String[1]
{
    $p.firstName + ' ' + $p.lastName
}

// Property with a todo note
Class meta::mypackage::Order
{
    {doc.todo = 'Add validation for negative amounts'}
    amount : Float[1];
}
```

Both `{doc.doc = '…'}` tags and `<<doc.deprecated>>` stereotypes, along with other tags / stereotypes, can be applied
to individual **columns** inside a `###Relational` store definition.

> **⚠ Column annotation order is the reverse of `###Pure` properties.**
> In a `###Pure` class, annotations precede the property name.
> In a `###Relational` table, annotations are placed **after** the column name
> but **before** the SQL type:
> `ColumnName <<stereotype>> {tag = 'value'} SQLTYPE`

```pure
###Relational
import meta::pure::profiles::*;

Database meta::mypackage::TradeDatabase
(
    Table TradeTable
    (
        trade_id   INT         PRIMARY KEY,
        trade_date DATE,

        // tag only — doc string on the column
        amount     {doc.doc = 'Net notional value of the trade in USD.'} FLOAT,

        // stereotype only — column is deprecated
        old_ref    <<doc.deprecated>> VARCHAR(20),

        // stereotype + tag together — deprecated with an explanation
        book_code  <<doc.deprecated>>
                   {doc.doc = 'Replaced by trade_id. Will be removed in v3.'}
                   VARCHAR(20),

        // multiple tags in one brace block
        currency   {doc.doc = 'ISO 4217 three-letter currency code.',
                    doc.todo = 'Validate against reference-data table.'}
                   CHAR(3)
    )
)
```

##### `meta::pure::profiles::equality` — Model-Defined Equality

```pure
import meta::pure::profiles::*;
```

| Annotation | Kind | Usage |
|-----------|------|-------|
| `<<equality.Key>>` | Stereotype (on a **property**) | Marks the property as a key field for structural equality. Two instances are equal if all `<<equality.Key>>` properties have equal values. |

```pure
Class meta::mypackage::Product
{
    <<equality.Key>> sku  : String[1];   // equality is based on sku …
    <<equality.Key>> site : String[1];   // … and site
    description          : String[0..1]; // not part of equality
}
```

When `equal()` (or `==`) is called on two `Product` instances, only `sku` and
`site` are compared; `description` is ignored.

##### `meta::pure::profiles::temporal` — Milestoning

```pure
import meta::pure::profiles::*;
```

| Annotation | Kind | Usage |
|-----------|------|-------|
| `<<temporal.businesstemporal>>` | Stereotype | Adds a single business-date dimension; generates `businessDate` property and date-aware `getAll` overload |
| `<<temporal.processingtemporal>>` | Stereotype | Adds a single processing-date dimension |
| `<<temporal.bitemporal>>` | Stereotype | Adds both business and processing date dimensions |

See [Section 11 — Milestoning](#11-milestoning-temporal-data) for full usage detail.

##### `meta::pure::profiles::access` — Visibility

```pure
import meta::pure::profiles::*;
```

| Annotation | Kind | Usage |
|-----------|------|-------|
| `<<access.public>>` | Stereotype | Explicitly marks an element as part of the public API |
| `<<access.protected>>` | Stereotype | Internal to the package hierarchy |
| `<<access.private>>` | Stereotype | Not intended for use outside the defining file/module |
| `<<access.externalizable>>` | Stereotype | Can be exposed externally (e.g. through a service layer) |

```pure
function <<access.private>>
    meta::mypackage::impl::computeInternal(x: Integer[1]): Integer[1]
{
    $x * 42
}

function <<access.public>>
    meta::mypackage::computeResult(x: Integer[1]): Integer[1]
{
    computeInternal($x)
}
```

##### `meta::pure::profiles::test` — Testing

The `meta::pure::profiles::test` and `meta::pure::test::pct::PCT` profiles that
drive test execution are covered in [Section 13 — Writing Tests in Pure](#13-writing-tests-in-pure).

---

## 4. Functions

### Named Function

```pure
function meta::mypackage::greet(name: String[1]): String[1]
{
    'Hello, ' + $name + '!'
}
```

### Return Values

A function body is a **sequence of semicolon-terminated expressions**. The value of
the **last expression** is the return value — there is no `return` keyword.

```pure
function meta::mypackage::describe(p: Person[1]): String[1]
{
    let greeting = 'Hello, ';                            // expression 1
    let name     = $p.firstName + ' ' + $p.lastName;    // expression 2
    $greeting + $name;                                   // expression 3 — returned
}
```

### Semicolons — complete rules

The rules for `;` in Pure are:

| Number of expressions | Required form |
|-----------------------|---------------|
| 1 | `expr` — **no semicolon** |
| 2 | `expr; expr;` |
| 3 | `expr; expr; expr;` |
| N | every expression terminated with `;` |

The key insight: **`;` is a terminator on every expression when there are multiple
expressions, and is absent entirely when there is only one**.

#### Single expression — no semicolon

When a function body or lambda body contains exactly **one** expression, no
semicolon is written:

```pure
// Named function — one expression, no semicolon
function meta::mypackage::double(x: Integer[1]): Integer[1]
{
    $x * 2
}

// Inline lambda — one expression after |, no semicolon
[1, 2, 3]->filter(x | $x > 1)

// Block lambda — one expression after |, no semicolon
[1, 2, 3]->map(x | {
    $x * 2
})
```

#### Multiple expressions — every expression including the last requires `;`

When there are two or more expressions, **every** expression must be terminated
with `;`, including the final one:

```pure
function meta::mypackage::describe(p: Person[1]): String[1]
{
    let greeting = 'Hello, ';
    let name     = $p.firstName + ' ' + $p.lastName;
    $greeting + $name;    // <- trailing ';' required — this is NOT optional
}

// Block lambda — all expressions terminated
[1, 2, 3]->map(x | {
    let doubled = $x * 2;
    $doubled + 1;          // <- required
})
```

#### Empty statements / double semicolons are not permitted

Pure has **no empty statement**. `;;` is a parse error:

```pure
// NOT valid Pure
{
    let x = 1;;   // ERROR — empty statement between the two semicolons
    $x + 1;
}
```

#### Why `let` returns a value — and when that matters

`let` is a function with this signature:

```pure
letFunction<T,m>(left: String[1], right: T[m]): T[m]
```

It **returns the value it binds** — the same `T[m]` that was assigned. This means
a `let` as the sole expression of a function *is* valid Pure:

```pure
function meta::mypackage::double(x: Integer[1]): Integer[1]
{
    let result = $x * 2   // sole expression — no semicolon; let returns Integer[1]
}
```

The compiler accepts this because `let result = $x * 2` evaluates to `Integer[1]`,
satisfying the declared return type.

#### Prefer a final bare expression over a final `let`

Even though a final `let` works, it is **misleading to readers**: `let` communicates
"I am naming something for later use", not "this is the result". When there is no
further use of the variable, the binding serves no purpose. Prefer returning the
expression directly:

```pure
// Avoid — sole expression is a let; works but misleading
function meta::mypackage::double(x: Integer[1]): Integer[1]
{
    let result = $x * 2
}

// Prefer — intent is unambiguous; single expression, no semicolon
function meta::mypackage::double(x: Integer[1]): Integer[1]
{
    $x * 2
}

// Multiple expressions — ALL terminated with ';', including the last
function meta::mypackage::describeAge(p: Person[1]): String[1]
{
    let age = $p.age->toOne();
    'Age: ' + $age->toString();
}
```

The exception is when the variable name meaningfully documents the computation for
the reader — in that case a final `let` is an acceptable style choice, but it should
be rare.

### Function with Multiple Parameters

```pure
function meta::mypackage::add(a: Integer[1], b: Integer[1]): Integer[1]
{
    $a + $b
}
```

### Function Calling Convention — Arrow Syntax

Pure supports both direct and arrow (method-chain) call styles:

```pure
// Direct
greet('Alice')

// Arrow (first argument moved to left of ->)
'Alice'->greet()

// Chaining
[1, 2, 3]->filter(x | $x > 1)->map(x | $x * 2)
```

### Lambda (Anonymous Function)

```pure
// Lambda syntax: parameter | body
[1, 2, 3]->filter(x | $x > 1)

// With type annotation
[1, 2, 3]->filter(x: Integer[1] | $x > 1)

// Multi-parameter lambda
pairs->map(p | $p.first + ':' + $p.second)
```

### Native Function

A native function has its implementation provided in Java, not Pure:

```pure
native function meta::pure::functions::lang::letFunction<T>(left: String[1], right: T[1]): T[1];
```

---

## 5. Control Flow

### if / else

`if` is a **function**, not a language keyword. It evaluates a boolean condition and
returns the result of exactly one of two branches.

#### Signature

```pure
// Basic: boolean condition
if<T,m>(test: Boolean[1], valid: Function<{->T[m]}>[1], invalid: Function<{->T[m]}>[1]): T[m]

// Multi-condition: list of (condition, result) pairs with a fallback
if<T,m>(condList: Pair<Function<{->Boolean[1]}>, Function<{->T[m]}>>[*], last: Function<{->T[m]}>[1]): T[m]
```

#### Why the branches are lambdas, not values

The `valid` and `invalid` parameters are typed `Function<{->T[m]}>` — zero-argument
functions (lambdas) — not plain values. This is **lazy evaluation by necessity**.

If the branches were plain values, Pure would have to evaluate *both* before calling
`if`. That would mean:

- Side-effectful branches (e.g. `println`) would always execute regardless of the condition.
- Expensive computations in the unused branch would always run.
- Recursive functions would never terminate — the else branch would be evaluated
  before the condition is checked.

By making each branch a `Function<{->T[m]}>`, the runtime only evaluates the branch
it actually needs. The `|` lambda syntax makes this concise enough that it reads
like a built-in construct:

```pure
// what you write
if($amount > 1000, | 'large', | 'small')

// what the type signature actually receives
if($amount > 1000,
   {-> 'large'},   // a zero-argument lambda producing String[1]
   {-> 'small'})   // only one of these is ever evaluated
```

#### Basic example

```pure
let label = if($amount > 1000,
               | 'large',
               | 'small');
```

The `|` before each branch is the lambda body separator — it introduces a
zero-argument lambda. `$amount > 1000` is evaluated eagerly (it's a plain
`Boolean[1]`), but only the selected branch lambda is then invoked.

#### Multi-condition form

For chains of conditions the second overload accepts a list of `pair(condition, result)`
lambdas and a fallback:

```pure
let category = if(
    [
        pair(| $score >= 90, | 'A'),
        pair(| $score >= 80, | 'B'),
        pair(| $score >= 70, | 'C')
    ],
    | 'F'           // fallback: no condition matched
);
```

Conditions are evaluated left-to-right; the first `true` condition wins. Note that
both the condition *and* the result are lambdas here — neither is evaluated until
needed.

### match

`match` is Pure's **type-dispatch** function. It tests a value against an ordered
list of typed lambdas and executes the body of the **first** lambda whose type *and*
multiplicity are both satisfied.

#### Signatures

```pure
// Basic: dispatch on type + multiplicity
match<T,m,n>(var: Any[*], functions: Function<{Nil[n]->T[m]}>[1..*]): T[m]

// With extra shared parameter passed to every branch
match<T,P,m,n,o>(var: Any[*], functions: Function<{Nil[n],P[o]->T[m]}>[1..*], with: P[o]): T[m]
```

#### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `var` | `Any[*]` | The value (or collection) being dispatched |
| `functions` | list of lambdas | Each lambda declares `param: Type[multiplicity]` — the first one whose type *and* multiplicity both match `var` is executed |
| `with` *(optional)* | `P[o]` | An extra value passed as a second argument to every branch lambda |

#### How matching works

A branch `a: SomeType[m]` is selected when **both** conditions hold:

1. **Type** — every element in `var` is an instance of `SomeType` (or a subtype of it)
2. **Multiplicity** — the size of `var` satisfies the multiplicity `[m]`

Branches are tested **in order**; the first match wins. Unmatched input throws a
`PureExecutionException` at runtime.

#### Example — annotated

```pure
$value->match([
    s: String[1]  | 'string: ' + $s,       // branch 1: exactly one String
    i: Integer[1] | 'integer: ' + $i->toString(), // branch 2: exactly one Integer
    a: Any[1]     | 'other'                // branch 3: catch-all for any single value
])
```

- `$value` is the input — `Any[*]` so it can be any type or collection.
- Each branch is a **lambda** written as `param: Type[multiplicity] | body`.
  The lambda parameter (`s`, `i`, `a`) is bound to `$value` inside the body.
- Branches are checked top-to-bottom. `Any[1]` at the end acts as a **catch-all**
  for any single value that didn't match the earlier branches.

#### Multiplicity matters — not just type

Unlike a simple type switch, `match` also checks cardinality. This means you can
have different branches for the same type at different multiplicities:

```pure
$collection->match([
    s: String[0]   | 'empty',           // zero strings
    s: String[1]   | 'one: ' + $s,      // exactly one string
    s: String[*]   | 'many: ' + $s->size()->toString()  // two or more strings
])
```

#### Inheritance — most specific branch wins by ordering

Because first-match wins, place **subtype** branches *before* supertype branches:

```pure
$geo->match([
    a: MA_Address[1]  | 'address: ' + $a.name,   // subtype first
    l: MA_Location[1] | 'location: ' + $l.place, // subtype first
    a: Any[1]         | 'unknown geo'             // supertype catch-all last
])
```

If `Any[1]` were listed first it would match everything and the subtype branches
would never be reached.

#### With extra parameter

The second overload passes a shared value to every branch, useful for avoiding
repeated captures:

```pure
$value->match([
    {i: Integer[1], suffix: String[1] | 'int_' + $suffix},
    {s: String[1],  suffix: String[1] | 'str_' + $suffix}
], 'result')
// If $value is 1 (Integer) → 'int_result'
// If $value is 'x' (String) → 'str_result'
```

---

## 6. Collections

### Collection Literal Syntax

A collection literal is written with square brackets.

`,` is strictly a **separator between elements** — there is no optional trailing
comma and no empty slot syntax.

| Form | Valid? | Reason |
|------|:------:|--------|
| `[]` | ✅ | Empty collection |
| `[1]` | ✅ | Single element |
| `[1, 2, 3]` | ✅ | Comma between each adjacent pair |
| `[1, 2,]` | ❌ | Trailing comma — nothing follows the last `,` |
| `[1, , 3]` | ❌ | Double comma / empty element slot |
| `[, 1]` | ❌ | Leading comma — nothing precedes the first `,` |

```pure
// Valid
let nums = [1, 2, 3];
let single = [42];
let empty = [];

// NOT valid Pure — parse errors
let bad1 = [1, 2, 3,];   // ERROR — trailing comma
let bad2 = [1, , 3];     // ERROR — empty element (double comma)
```

### Common functions for collections

All collection functions are in `meta::pure::functions::collection`. The most
commonly used functions, available via arrow syntax:

| Function | Signature | Description |
|----------|-----------|-------------|
| `filter` | `T[*]->filter(x\|Boolean[1]): T[*]` | Keep matching elements |
| `map` | `T[*]->map(x\|V[1]): V[*]` | Transform each element |
| `fold` | `T[*]->fold((a,b)\|R, init): R` | Left fold / reduce |
| `find` | `T[*]->find(x\|Boolean[1]): T[0..1]` | First matching element |
| `exists` | `T[*]->exists(x\|Boolean[1]): Boolean[1]` | Any element matches |
| `forAll` | `T[*]->forAll(x\|Boolean[1]): Boolean[1]` | All elements match |
| `size` | `T[*]->size(): Integer[1]` | Count |
| `isEmpty` | `T[*]->isEmpty(): Boolean[1]` | True if empty |
| `isNotEmpty` | `T[*]->isNotEmpty(): Boolean[1]` | True if non-empty |
| `first` | `T[*]->first(): T[0..1]` | First element or empty |
| `at` | `T[*]->at(i: Integer[1]): T[1]` | Element at index (0-based) |
| `drop` / `take` | `T[*]->drop(n): T[*]` | Skip / keep first n |
| `slice` | `T[*]->slice(s,e): T[*]` | Sub-collection [s, e) |
| `sort` | `T[*]->sort(): T[*]` | Natural sort |
| `reverse` | `T[*]->reverse(): T[*]` | Reverse order |
| `removeDuplicates` | `T[*]->removeDuplicates(): T[*]` | Distinct elements |
| `zip` | `T[*]->zip(V[*]): Pair<T,V>[*]` | Pair two collections |
| `concatenate` | `T[*]->concatenate(T[*]): T[*]` | Combine two collections |
| `groupBy` | `T[*]->groupBy(f): Map<K,List<T>>` | Group into map |
| `toOne` | `T[*]->toOne(): T[1]` | Assert exactly one element; throws if 0 or 2+ |
| `toOneMany` | `T[*]->toOneMany(): T[1..*]` | Assert at least one element |

---

## 7. Multiplicity Casting

```pure
// Assert exactly one — throws PureExecutionException if collection ≠ 1
$collection->toOne()
$collection->toOne('Expected exactly one Person')

// Assert at least one
$collection->toOneMany()
```

---

## 8. Type Casting

```pure
// Cast to a specific type — throws if incompatible
$anyValue->cast(@Person)

// Safe type check
$anyValue->instanceOf(Person)
```

---

## 9. Instance Creation

```pure
// Create an instance with the ^ operator
let p = ^Person(firstName='Alice', lastName='Smith');

// With to-many property
let firm = ^Firm(legalName='ACME', employees=[^Person(firstName='Bob')]);

// Accessing properties
$p.firstName      // 'Alice'
$p.fullName()     // 'Alice Smith'  (derived property)
```

---

## 10. Packages and Imports

### Declaring a Package

Every element belongs to a package. The package is declared either inline in the
element name (`Class my::pkg::Person { … }`) or, less commonly, at the top of the
file with a `package` directive:

```pure
// Declare the package at top of file
package meta::mypackage;
```

### Import Statements

Import statements must be at the start of the file: 

```pure
// Import another package (removes need to qualify names)
import meta::pure::functions::collection::*;
import meta::mypackage::model::*;
```
All elements in the file that do not carry an explicit package path inherit this
package. 

Without an import, every type and function reference must be fully qualified:

Fully-qualified names (`meta::mypackage::Person`) always take precedence.

```pure
// Without import — verbose
let p: meta::mypackage::model::Person[1] = meta::mypackage::model::makePerson('Alice');
```

`import` brings an entire package into scope so the short name can be used instead.
Only **wildcard imports** (`*`) are supported — you cannot import a single name:

```pure
import meta::pure::functions::collection::*;
import meta::mypackage::model::*;

// Now short names resolve without qualification
let people = Person->getAll()->filter(p | $p.age > 18);
```

#### Scope of imports

Imports are **per grammar section, per file**. An `import` in one `###Pure` section
does not affect a `###Mapping` section in the same file — each section must declare
its own imports. Internally the compiler models this as an `ImportGroup` instance
(defined in `platform/pure/grammar/m3.pure`) that is created for each source file
and referenced by all elements defined within it. When the compiler resolves a short
name it walks the import groups associated with the element's source file, trying
each imported package in declaration order until a match is found.

```pure
###Pure
import meta::mypackage::model::*;

Class meta::mypackage::service::TradeService
{
    // 'Product' resolves via the import above
    product : Product[1];
}

###Mapping
import meta::mypackage::model::*;   // must re-import — different section

Mapping meta::mypackage::mapping::TradeMapping
(
    // ...
)
```

#### Common imports in the Pure standard library

| What you are using | Import |
|-------------------|--------|
| Collection functions (`filter`, `map`, `fold`, …) | `import meta::pure::functions::collection::*;` |
| String functions (`startsWith`, `joinStrings`, …) | `import meta::pure::functions::string::*;` |
| Math functions | `import meta::pure::functions::math::*;` |
| Date functions | `import meta::pure::functions::date::*;` |
| Test stereotypes | `import meta::pure::profiles::*;` |

---

## 11. Milestoning (Temporal Data)

Milestoning adds bitemporal tracking to classes. Apply a stereotype from the
`meta::pure::profiles::temporal` profile:

```pure
Class <<temporal.businesstemporal>> meta::mypackage::Product
{
    name : String[1];
    price: Float[1];
}
```

The compiler automatically adds date-range properties and rewrites property navigation
to be date-aware. Three stereotypes are available:

| Stereotype | Date dimension | Generated `getAll` overload |
|-----------|---------------|---------------------------|
| `<<temporal.businesstemporal>>` | Business date | `getAll(Product, %2024-01-01)` |
| `<<temporal.processingtemporal>>` | Processing date | `getAll(Product, processingDate)` |
| `<<temporal.bitemporal>>` | Both | `getAll(Product, processingDate, businessDate)` |

Querying a milestoned class:

```pure
// All current versions at a given business date
Product.all(%2024-01-01)

// All versions in a date range
Product.allVersionsInRange(%2023-01-01, %2024-01-01)

// All versions (no date filter)
Product.allVersions()
```

See [Domain Concepts — Milestoning](../architecture/domain-concepts.md#milestoning-bitemporal-compiler-transformation)
for the compiler implementation details.

---

## 12. Standard Library Overview

The Pure standard library is organised under `meta::pure::functions::`:

| Category | Package | Key functions |
|----------|---------|--------------|
| **Collection** | `collection` | `filter`, `map`, `fold`, `find`, `exists`, `forAll`, `groupBy`, `sort`, `zip`, `removeDuplicates` |
| **String** | `string` | `startsWith`, `endsWith`, `contains`, `substring`, `split`, `joinStrings`, `toLower`, `toUpper`, `trim`, `replace`, `format` |
| **Math** | `math` | `+`, `-`, `*`, `/`, `abs`, `sqrt`, `floor`, `ceiling`, `round`, `mod`, `range` |
| **Date** | `date` | `today`, `now`, `year`, `monthNumber`, `dayOfMonth`, `dateDiff`, `adjust` |
| **Boolean** | `boolean` | `and`, `or`, `not`, `is`, `eq`, `equal`, `isEmpty`, `isNotEmpty` |
| **Lang** | `lang` | `new` (`^`), `let`, `if`, `match`, `cast`, `toOne`, `toOneMany`, `copy` |
| **Meta** | `meta` | `instanceOf`, `genericType`, `type`, `id`, `evaluateAndDeactivate` |
| **IO** | `io` | `println`, `print` |
| **Tests** | `tests` | `assertEquals`, `assertEq`, `assertTrue`, `assertFalse`, `assertEmpty`, `assertSize`, `fail` |

---

## 13. Writing Tests in Pure

Pure has two distinct test mechanisms, used for different purposes.

> **Prefer `<<PCT.test>>`** for any function that should work identically on all
> execution platforms. Use `<<test.Test>>` for domain-specific logic that is tied to
> a single repository or runtime.

---

### `<<test.Test>>` — Unit Tests

A regular unit test. Marked with the `<<test.Test>>` stereotype from the
`meta::pure::profiles::test` profile. Runs directly against a fixed runtime
(compiled or interpreted, depending on which test suite executes it).

```pure
function <<test.Test>> meta::mypackage::tests::testAddition(): Boolean[1]
{
    assertEquals(5, add(2, 3));
    assertEquals(0, add(-1, 1));
}
```

**Signature:** No parameters — `(): Boolean[1]`.

Use `<<test.Test>>` for:

- Testing domain logic, mappings, or helper functions you have written.
- Tests that are specific to one runtime or one repository.
- Tests where the setup is entirely self-contained in Pure.

---

### `<<PCT.test>>` — Platform Compatibility Tests

A PCT test verifies that a Pure **standard library function** behaves identically
on *every* supported execution platform (compiled engine, interpreted engine, and
any future adapters such as Alloy or in-memory engines).

```pure
function <<PCT.test>> meta::pure::functions::lang::tests::if::testSimpleIf
    <Z,y>(f: Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]): Boolean[1]
{
    assertEq('truesentence', $f->eval(if(true,  | 'truesentence', | 'falsesentence')));
    assertEq('falsesentence', $f->eval(if(false, | 'truesentence', | 'falsesentence')));
}
```

**Signature:** Requires exactly one parameter — the **adapter function** `f`.

#### The adapter parameter — why it exists

The type of `f` is `Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]` — a function that
takes a zero-argument lambda and executes it.

This indirection exists so that the **same Pure test body** can be executed by
different runtime adapters. The Java PCT test runner (e.g.
`Test_Interpreted_EssentialFunctions_PCT`) supplies a concrete `f` that routes
execution through its specific engine:

```mermaid
sequenceDiagram
    participant Runner as Java PCT runner<br/>(e.g. PureTestBuilderInterpreted)
    participant Pure as Pure test function<br/>(&lt;&lt;PCT.test&gt;&gt; testSimpleIf)
    participant Engine as Interpreted engine

    Runner->>Pure: adapter = nativeAdapter<br/>call testSimpleIf(f)
    Pure->>Engine: f-&gt;eval( if(true, ...) )
    Engine-->>Pure: result
    Pure-->>Runner: assertion result
```

A different runner (e.g. the compiled engine runner) supplies a different `f`,
routing through the compiled engine. The Pure test body is written once and
verified on all platforms automatically.

Inside the test body, wrap every expression-under-test in `$f->eval(...)`:

```pure
assertEq('expected', $f->eval( myFunction(args) ));
//                   ──────── ↑ the expression being platform-tested
```

#### `<<test.Test>>` vs `<<PCT.test>>` — comparison

| | `<<test.Test>>` | `<<PCT.test>>` |
|--|----------------|----------------|
| **Purpose** | Test your own domain logic | Verify standard library parity across all platforms |
| **Parameters** | None | One adapter `f` parameter |
| **Runs on** | The runtime that executes the test suite | All registered runtimes |
| **Test body** | Direct assertions | Assertions wrapped in `$f->eval(...)` |
| **Defined in** | `meta::pure::profiles::test` profile | `meta::pure::test::pct::PCT` profile |
| **Java runner** | `PureTestBuilderInterpreted` / compiled test suite | `Test_Interpreted_*_PCT` / `Test_Compiled_*_PCT` |
| **Use for** | Your code | Pure built-in functions (`if`, `filter`, `match`, etc.) |

---

### Test Setup — Shared Test Models

Because Pure has no `@Before`/`@BeforeClass` equivalent, shared test data is
handled by putting `Class` definitions in a dedicated `_testModel.pure` file
in the same package, then importing it:

```pure
// File: meta/mypackage/tests/_testModel.pure
Class meta::mypackage::tests::model::TradeTestData
{
    tradeId : String[1];
    amount  : Float[1];
}

// File: meta/mypackage/tests/testTradeLogic.pure
import meta::mypackage::tests::model::*;

function <<test.Test>> meta::mypackage::tests::testPositiveTrade(): Boolean[1]
{
    let trade = ^TradeTestData(tradeId='T001', amount=100.0);
    assert($trade.amount > 0);
}
```

This is the established pattern throughout the Pure standard library (e.g.
`platform/pure/essential/collection/_testModel.pure`,
`platform/pure/grammar/functions/lang/_testModel.pure`).

---

### Test Annotations Reference

#### `meta::pure::profiles::test` stereotypes and tags

```pure
import meta::pure::profiles::*;
```

| Stereotype | Effect |
|-----------|--------|
| `<<test.Test>>` | Marks a function as a runnable test |
| `<<test.TestCollection>>` | Groups a set of related tests (informational; no execution effect) |
| `<<test.ToFix>>` | Known-broken test; skipped by the runner |
| `<<test.ExcludeAlloy>>` | Skipped when running in the Alloy/Legend Studio environment |
| `<<test.ExcludeLazy>>` | Skipped in lazy-evaluation mode |
| `<<test.ExcludeModular>>` | Skipped in modular compilation mode |
| `{test.excludePlatform = '…'}` | Tag to skip on a named platform, e.g. `'Java compiled'` or `'Java interpreted'` |

#### `meta::pure::test::pct::PCT` stereotypes and tags

```pure
import meta::pure::test::pct::*;
```

| Annotation | Kind | Usage |
|-----------|------|-------|
| `<<PCT.test>>` | Stereotype | PCT test — requires one adapter parameter `f`; run on all platforms |
| `<<PCT.function>>` | Stereotype | Marks a function as a standard-library function that PCT tests cover |
| `<<PCT.adapter>>` | Stereotype | Marks an adapter function that routes execution to a specific engine |
| `<<PCT.platformOnly>>` | Stereotype | Platform-specific function; excluded from cross-platform PCT runs |
| `{PCT.grammarDoc = '…'}` | Tag | Canonical grammar shorthand for the function (e.g. `'$first == $second'`) |
| `{PCT.grammarCharacters = '…'}` | Tag | The operator characters (e.g. `'=='`), used by tooling |
| `{PCT.adapterName = '…'}` | Tag | Human-readable name for a PCT adapter (e.g. `'In-Memory'`) |

For PCT tests, platform-specific exclusions are declared in the Java runner:

```java
// In Test_Interpreted_EssentialFunctions_PCT.java
private static final MutableList<ExclusionSpecification> expectedFailures =
        Lists.mutable.with(
                one("meta::pure::functions::lang::tests::if::testMultiIf", "Not yet supported")
        );
```

---

*See also: [Pure Type System Reference](pure-type-system.md) · [Testing Strategy](../testing/testing-strategy.md) · [Compiler Pipeline](../architecture/compiler-pipeline.md)*
