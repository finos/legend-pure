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

//! Shared type-system types for the Pure semantic graph.
//!
//! [`TypeExpr`] is the Rust equivalent of Java's `GenericType`. Every property
//! type, parameter type, and return type in the compiled model is a `TypeExpr`.
//!
//! See `DESIGN.md` §6 for full rationale.

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::ids::ElementId;

// ---------------------------------------------------------------------------
// TypeExpr — the Rust equivalent of Java's GenericType
// ---------------------------------------------------------------------------

/// A resolved or partially-resolved type expression in the Pure model.
///
/// This is the equivalent of Java's `GenericType`. In Java you almost never
/// reference a `Type` directly — you wrap it in a `GenericType` that carries
/// type arguments, multiplicity arguments, and type parameter bindings.
///
/// `TypeExpr` plays the same role: it's the currency of the Pure type system.
///
/// # Variants
///
/// | Pure Syntax | Variant |
/// |---|---|
/// | `String`, `Person` | `Named { element, type_arguments: [], .. }` |
/// | `List<String>` | `Named { element: list_id, type_arguments: [Named(string_id)] }` |
/// | `Varchar(255)` | `Named { element: varchar_id, value_arguments: [Integer(255)] }` |
/// | `{String[1] -> Bool[1]}` | `FunctionType { .. }` |
/// | `(a: Integer, b: String)` | `Relation(relation_id)` |
/// | `T`, `U` | `Generic("T")` |
/// | `T + V` | `GenericTypeOperation { op: Union, .. }` |
/// | `T - V` | `GenericTypeOperation { op: Difference, .. }` |
/// | `T ⊆ V` | `GenericTypeOperation { op: Subset, .. }` |
/// | `Z = (?:K)⊆T` | `GenericTypeOperation { op: Equal, .. }` |
/// | (untyped lambda param, no annotation, no expectation) | `Unresolved` |
#[derive(Debug, Clone, PartialEq)]
pub enum TypeExpr {
    /// A resolved named type, optionally with type, multiplicity and/or
    /// value arguments.
    ///
    /// Covers: `String`, `Person`, `List<String>`, `Map<K,V>`,
    /// `Holder<T|m>`, `Varchar(255)`.
    Named {
        /// The resolved element (`Class`, `Enum`, `PrimitiveType`, `Measure`, `Unit`).
        element: ElementId,
        /// Source range covering the named-element identifier in the
        /// surface text (`String`, `Person`, `meta::pure::Foo`).
        /// Populated by the resolver when the type expression came
        /// through the parser. `None` for loaded types,
        /// m3-bootstrap types, and synthetically constructed `Named`
        /// variants. Used by IDE goto-def to underline + click a
        /// type reference at any position it appears (`extends`,
        /// parameter type, return type, property type, generic
        /// argument, …).
        source_info: Option<SourceInfo>,
        /// Generic type arguments: `<String, Integer>`.
        type_arguments: Vec<TypeExpr>,
        /// Generic multiplicity arguments: the `*` in `Holder<String|*>`.
        /// Position-aligned with the class's `multiplicity_parameters`.
        /// Required so class-level mult-vars (`m` in
        /// `Holder<T|m> { items: T[m] }`) flow through property access:
        /// without this field, `compute_type_arg_bindings` could only
        /// substitute `T → String` and `items` would type as
        /// `String[Variable("m")]`, which `is_multiplicity_compatible`
        /// then accepted permissively against any declared site.
        multiplicity_arguments: Vec<Multiplicity>,
        /// Compile-time value arguments: `(255, 'ok')`.
        value_arguments: Vec<ConstValue>,
    },
    /// Anonymous function signature: `{String[1] -> Boolean[1]}`.
    ///
    /// Not an Element — structural type only.
    FunctionType {
        /// Parameter types with multiplicities.
        parameters: Vec<(TypeExpr, Multiplicity)>,
        /// Return type.
        return_type: Box<TypeExpr>,
        /// Return multiplicity.
        return_multiplicity: Multiplicity,
    },
    /// Structural relation type — an anonymous column bag.
    ///
    /// Carries the per-column triple `(name, type_expr, multiplicity)`
    /// inline so callers (overload narrowing, type-checking, deactivate/
    /// reactivate reflection) can read column metadata directly from
    /// the type without consulting an external interner. Mirrors what
    /// Java's `RelationType._columns()` exposes.
    ///
    /// Not an Element — structural type only. Two `Relation`s with the
    /// same column shape (name, type, multiplicity, in declaration
    /// order) compare equal.
    Relation(Vec<RelationColumnTypeExpr>),
    /// An unresolved type variable: `T`, `U`.
    Generic(SmolStr),
    /// A binary relation type-algebra operation, mirroring Java's
    /// `GenericTypeOperation`. Covers `T + V` (column union), `T - V`
    /// (column difference), `T ⊆ V` (subset constraint), and the
    /// `Z = (?:K)⊆T` column-binding equality. Built by the resolver from
    /// the parser's `algebra_ops`/`subset_bound`/`equal_binding` fields,
    /// then evaluated against concrete relation types in
    /// `substitute_type_with_mults` (Union appends columns, Difference
    /// removes by name; Subset/Equal drive `(?:K)` binding and pass
    /// through once binding is done). Left-associative: `T - Z + V`
    /// nests as `Union { left: Difference { left: T, right: Z }, right: V }`.
    GenericTypeOperation {
        /// Which relation-algebra operation this node performs.
        op: GenericTypeOpKind,
        /// Left operand (the accumulated type so far, for chained ops).
        left: Box<TypeExpr>,
        /// Right operand.
        right: Box<TypeExpr>,
    },
    /// **Type hole** — a sentinel marker for a lambda parameter the
    /// compiler could not infer: no source annotation AND no caller-side
    /// expectation flowed in. Unit variant; the parameter's user-visible
    /// name lives on the surrounding `Variable { name }` /
    /// `Parameter { name, .. }` and doesn't need to be carried here.
    ///
    /// Distinct from `Generic` (which stands for an in-scope, bound type
    /// variable) and from `Named { element: Any }` (which is what the
    /// user wrote when they declared `:Any[1]` explicitly). Treating
    /// these as `Any` would conflate "user said Any" with "compiler gave
    /// up"; reflective walks of the lambda's parameters
    /// (`deactivate`/`reactivate`, `genericType.rawType`) would
    /// mis-report the source. Tooling (LSP hover, formatter, future
    /// "infer-from-first-use" modes) can render this distinctly.
    ///
    /// **Compile-time only.** `lower_lambda_parameters` always pushes a
    /// `CannotInferLambdaParameterTypes` diagnostic for every lambda
    /// parameter that lands on this variant, so a clean compile cannot
    /// produce an IR with `Unresolved`. Downstream type-flow sites
    /// nonetheless handle it defensively (the diagnostic accumulates
    /// rather than aborts, so body lowering still walks past the hole):
    /// - `is_concrete_type` → `false`
    /// - `bind_type` → skips binding a real generic to an `Unresolved`
    /// - `substitute_type` → identity (passes through)
    /// - `is_type_compatible` → permissive (treated as `Any`-like for
    ///   dispatch checks)
    /// - `type_lub` → `Any`
    /// - `narrow_candidates_by_type` → refuses to commit on the
    ///   declaration-order tiebreaker when any arg reads `Unresolved`
    /// - `resolve_function_call` → suppresses the redundant
    ///   "Ambiguous function call" cascade (the lambda already pushed
    ///   the actionable diagnostic)
    /// - runtime `match` dispatch → matches any value (same as
    ///   `Generic(_)`), so partially-broken builds remain inspectable
    Unresolved,
}

/// The relation type-algebra operation carried by
/// [`TypeExpr::GenericTypeOperation`]. Mirrors the `type` discriminant on
/// Java's `GenericTypeOperation` (`Union`/`Difference`/`Subset`/`Equal`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GenericTypeOpKind {
    /// `T + V` — append `V`'s columns to `T`'s.
    Union,
    /// `T - V` — remove `V`'s columns (matched by name) from `T`.
    Difference,
    /// `T ⊆ V` — `T` is constrained to be a subset of `V`'s columns.
    /// Drives `(?:K)` wildcard-column binding; passes through once bound.
    Subset,
    /// `Z = …` — binds the generic `Z` to the (right-hand) column type.
    /// Drives `(?:K)` wildcard-column binding; passes through once bound.
    Equal,
}

/// One column of a structural relation type.
///
/// Carries the per-column metadata that a `(name:Type[mult], …)` syntax
/// captures, in the resolved form: name as a `SmolStr`, type as a
/// `TypeExpr` (so parameterised primitives like `Decimal<10,2>` and
/// nested wrappers round-trip), and the column's `Multiplicity`.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationColumnTypeExpr {
    /// Column name (e.g. `"a"`, `"name with space"`). Unquoted.
    pub name: SmolStr,
    /// Column type expression.
    pub type_expr: TypeExpr,
    /// Column multiplicity.
    pub multiplicity: Multiplicity,
}

/// A compile-time constant value used in type parameterization.
///
/// For example, `Varchar(255)` uses `ConstValue::Integer(255)`.
#[derive(Debug, Clone, PartialEq)]
pub enum ConstValue {
    /// Integer value, e.g., `200` in `VARCHAR(200)`.
    Integer(i64),
    /// String value, e.g., `'ok'` in `Res('ok')`.
    String(String),
}

// ---------------------------------------------------------------------------
// Multiplicity — mirrors ast::Multiplicity without Spanned metadata
// ---------------------------------------------------------------------------

/// Multiplicity specification for properties and parameters.
///
/// Mirrors `ast::Multiplicity` but without source location metadata —
/// at the semantic level, multiplicities are structural values.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Multiplicity {
    /// Exactly one: `[1]` — `lower=1, upper=1`.
    PureOne,
    /// Zero or one: `[0..1]`.
    ZeroOrOne,
    /// Zero or more: `[*]`.
    ZeroOrMany,
    /// One or more: `[1..*]`.
    OneOrMany,
    /// Arbitrary range: `[lower..upper]`.
    Range {
        /// Lower bound (inclusive).
        lower: u32,
        /// Upper bound (inclusive), `None` = unbounded.
        upper: Option<u32>,
    },
    /// A multiplicity variable bound by the enclosing generic signature —
    /// e.g., `m` in `reverse<T|m>(values:T[m]):T[m]`. Kept as a name so
    /// function-name mangling reproduces the source form
    /// (`reverse_T_m__T_m_`) and so generic substitution can bind it at
    /// call sites. Semantically acts like `ZeroOrMany` everywhere else.
    Variable(SmolStr),
}

/// A function or qualified property parameter (resolved).
#[derive(Debug, Clone, PartialEq)]
pub struct Parameter {
    /// Parameter name.
    pub name: SmolStr,
    /// Parameter type.
    pub type_expr: TypeExpr,
    /// Parameter multiplicity.
    pub multiplicity: Multiplicity,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A parsed date/time value for use in compiled expressions.
///
/// Parsed from the raw strings in `ast::StrictDateLiteral`, `ast::DateTimeLiteral`,
/// and `ast::StrictTimeLiteral`. Avoids carrying raw strings through the
/// semantic graph.
///
/// Field types match `jiff::civil` (and `PureDate`) so the evaluator needs
/// zero casts.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DateValue {
    /// Strict date: `%2024-01-15`, or a partial date (`%2024`, `%2024-01`).
    ///
    /// `month` is `Some` when the literal included a month segment; `day`
    /// likewise for the day segment. The runtime preserves the precision
    /// so `%2024-01->toRepresentation()` round-trips to `%2024-01`, not
    /// `%2024-01-01`.
    StrictDate {
        /// Year (−9999..9999).
        year: i16,
        /// Month (1–12). `None` for year-only literals (`%2024`).
        month: Option<i8>,
        /// Day (1–31). `None` for year- or year-month-only literals.
        day: Option<i8>,
    },
    /// Date-time: `%2024-01-15T10:30:00`.
    DateTime {
        /// Year (−9999..9999).
        year: i16,
        /// Month (1–12).
        month: i8,
        /// Day (1–31).
        day: i8,
        /// Hour (0–23).
        hour: i8,
        /// Minute (0–59).
        minute: i8,
        /// Second (0–59).
        second: i8,
        /// Sub-second nanoseconds (`0–999_999_999`).
        subsecond_nanos: i32,
        /// Number of fractional-second digits the literal was written with
        /// (1–9). `0` means no fractional part was present. Preserved so
        /// `toRepresentation` can round-trip the exact source form:
        /// `%…T00:00:00.000` → `.000` (3 digits), `%…T00:00:00.0000` → `.0000`.
        subsecond_digits: u8,
        /// `true` if the literal included `:mm` minutes (or finer).
        /// `false` for `%…T00` hour-only literals. Phase 6 part 2:
        /// distinguishes `T17` from `T17:00` so `year(%…T17)` and
        /// `hasMinute(%…T17) == false` work — these are platform tests
        /// in `essential/date/has/hasMinute.pure` and
        /// `essential/date/extract/year.pure`.
        has_minutes: bool,
        /// `true` if the literal included `:ss` seconds (or finer). `false`
        /// for `%…T00:00` minute-only literals. Needed so `toRepresentation`
        /// can round-trip `%2014-01-01T00:00+0000` without promoting to
        /// `00:00:00`.
        has_seconds: bool,
        /// Signed minute offset from UTC the literal specified (`+0500`
        /// → `300`, `-0500` → `-300`). `None` when the literal carried no
        /// explicit timezone suffix. The runtime normalises to UTC at
        /// construction time — this field exists only so the parser can
        /// report whether the source had a TZ marker.
        tz_offset_minutes: Option<i16>,
    },
    /// Strict time: `%10:30:00`.
    StrictTime {
        /// Hour (0–23).
        hour: i8,
        /// Minute (0–59).
        minute: i8,
        /// Second (0–59).
        second: i8,
        /// Sub-second nanoseconds (`0–999_999_999`).
        subsecond_nanos: i32,
        /// Number of fractional-second digits the literal was written
        /// with (1–9). `0` means no fractional part.
        subsecond_digits: u8,
    },
    /// `%latest` — the milestoning sentinel for "the latest version".
    ///
    /// Distinct from concrete `StrictDate` values. Lowered from the
    /// parser's `StrictDateLiteral { value: "%latest" }` AST node. At
    /// runtime it materialises as `crate::types::Value::Latest` (the
    /// runtime equivalent — defined in `legend-pure-runtime`).
    /// Comparison-wise it's greater than every concrete date; in
    /// milestoning dispatch it acts as a wildcard that disables the
    /// date filter on the relevant axis.
    ///
    /// Java parity: the `latestDate` instance form recognised by
    /// `MilestoningFunctionExpressionValidator`.
    Latest,
}

/// Resolved type and multiplicity for a compiled expression.
///
/// After type inference (Pass 2.5), every expression in the model
/// carries a `ResolvedType` via [`ValueSpec::type_info`].
#[derive(Debug, Clone, PartialEq)]
pub struct ResolvedType {
    /// The inferred type of the expression.
    pub type_expr: TypeExpr,
    /// The inferred multiplicity of the expression.
    pub multiplicity: Multiplicity,
}
/// A compiled value specification — the semantic expression type.
///
/// All names are resolved. Operators are desugared to function calls.
/// `let` is `FunctionCall("letFunction", ...)`, `new` is
/// `FunctionCall("new", ...)`. `Group(...)` is eliminated (unwrapped).
///
/// This is the Rust equivalent of Java's `ValueSpecification` hierarchy
/// (M3 metamodel: `InstanceValue`, `SimpleFunctionExpression`,
/// `VariableExpression`).
///
/// # Structure
///
/// Every expression node carries three fields:
/// - `kind` — the variant-specific data ([`ExprKind`])
/// - `source_info` — source location for diagnostics
/// - `type_info` — inferred type + multiplicity (populated by Pass 2.5)
///
/// This "header + kind" pattern avoids repeating `source_info` and
/// `type_info` across every variant, and enables O(1) field access
/// without pattern matching.
#[derive(Debug, Clone, PartialEq)]
pub struct ValueSpec {
    /// The expression variant and its data.
    pub kind: Box<ExprKind>,
    /// Source location in the original `.pure` file.
    pub source_info: SourceInfo,
    /// Inferred type annotation. `None` until Pass 2.5 runs.
    pub type_info: Option<Box<ResolvedType>>,
}

/// Shared payload for the three call-shaped IR variants
/// ([`ExprKind::FunctionCall`], [`ExprKind::PropertyCall`],
/// [`ExprKind::QualifiedPropertyCall`]). The variant tag carries the
/// discriminator — mirroring Java's `_functionName` /
/// `_propertyName` / `_qualifiedPropertyName` slots on
/// `SimpleFunctionExpression` — and the data is identical across kinds
/// so dispatch can share helpers without runtime tag fields.
#[derive(Debug, Clone, PartialEq)]
pub struct FunctionCallData {
    /// Resolved function element (user-defined functions). `None` for
    /// unresolved built-ins, operators, and property / QP calls — the
    /// receiver class plus `function_name` disambiguates those.
    pub function: Option<ElementId>,
    /// Name slot. For [`ExprKind::FunctionCall`], the function name.
    /// For [`ExprKind::PropertyCall`] / [`ExprKind::QualifiedPropertyCall`],
    /// the property name.
    pub function_name: SmolStr,
    /// Arguments. For [`ExprKind::PropertyCall`], `arguments[0]` is the
    /// receiver and there are no other entries. For
    /// [`ExprKind::QualifiedPropertyCall`], `arguments[0]` is the
    /// receiver and `arguments[1..]` are the QP arguments. For
    /// [`ExprKind::FunctionCall`], all entries are positional args.
    pub arguments: Vec<ValueSpec>,
}

/// Expression variant — the type-specific payload of a [`ValueSpec`].
///
/// Each variant contains only the data unique to that expression kind.
/// The common `source_info` and `type_info` fields live on the parent
/// [`ValueSpec`] struct.
#[derive(Debug, Clone, PartialEq)]
pub enum ExprKind {
    /// Integer literal: `42`.
    IntegerLiteral(i64),
    /// Float literal: `3.14`.
    FloatLiteral(f64),
    /// Decimal literal: `3.14D`.
    DecimalLiteral(rust_decimal::Decimal),
    /// String literal: `'hello'`.
    StringLiteral(SmolStr),
    /// Boolean literal: `true`, `false`.
    BooleanLiteral(bool),
    /// Date/time literal: `%2024-01-15`, `%2024-01-15T10:30:00`, `%10:30:00`.
    DateLiteral(DateValue),

    /// Variable reference: `$name`.
    ///
    /// Just a name. Type/multiplicity information lives on [`Parameter`]
    /// in function/lambda definitions — *not* on the variable reference.
    Variable {
        /// Variable name (without the `$` prefix).
        name: SmolStr,
    },

    /// Any function call, including desugared operators, `let`, `new`, arrow.
    ///
    /// Operators desugar to: `plus`, `minus`, `times`, `divide`, `equal`,
    /// `lessThan`, `and`, `or`, `not`, etc.
    /// `let` desugars to `letFunction`. `new` desugars to `new`.
    /// Arrow `x->filter(p)` becomes `FunctionCall("filter", [x, p])`.
    FunctionCall(FunctionCallData),

    /// Simple property access (`$x.name`). The receiver is
    /// `arguments[0]`; the property name is in `function_name`. Mirrors
    /// Java's `SimpleFunctionExpression` with `_propertyName` set.
    PropertyCall(FunctionCallData),
    /// Qualified-property invocation (`$x.qp(args)`). The receiver is
    /// `arguments[0]`; QP arguments follow at `arguments[1..]`. The QP
    /// name is in `function_name`. Mirrors Java's `SimpleFunctionExpression`
    /// with `_qualifiedPropertyName` set.
    QualifiedPropertyCall(FunctionCallData),

    /// Enum value reference: `MyEnum.VALUE` (after semantic disambiguation).
    EnumValue {
        /// The resolved Enumeration element.
        enum_element: ElementId,
        /// The enum value name.
        value: SmolStr,
    },

    /// Lambda expression: `{x: String[1] | $x + 'hello'}`.
    Lambda {
        /// Lambda parameters.
        parameters: Vec<Parameter>,
        /// Body expressions.
        body: Vec<ValueSpec>,
    },

    /// One `key = expression` (or `key += expression` when `add`) of a
    /// `^Class(...)` construction, the M3 `KeyExpression` node. Evaluates
    /// to a heap `KeyExpression` object whose `expression` slot holds the
    /// evaluated value(s); the `new` and `copy` natives decode it.
    KeyExpression {
        /// Property name.
        key: SmolStr,
        /// The value expression, evaluated when the key expression is.
        expression: ValueSpec,
        /// `true` for `+=`.
        add: bool,
    },

    /// Collection literal: `[1, 2, 3]`.
    Collection {
        /// Elements.
        elements: Vec<ValueSpec>,
    },

    /// Type reference expression: `@MyType`.
    TypeReference {
        /// The resolved type.
        type_expr: TypeExpr,
    },

    /// Bare multiplicity literal expression: `@[1]`, `@[0..1]`, `@[1..*]`,
    /// `@[*]`, or `@[m]` referencing a declared multiplicity parameter.
    ///
    /// Sibling to [`ExprKind::TypeReference`]. The lowered form carries
    /// the resolved [`Multiplicity`] and the lowering sets
    /// `type_info.multiplicity` to the same value (with `type_expr = Any`)
    /// so dispatch can bind a parametric arg's multiplicity directly.
    /// At runtime it materialises a
    /// `meta::pure::metamodel::multiplicity::Multiplicity` heap object via
    /// `build_multiplicity_wrapper`.
    MultiplicityReference {
        /// The resolved multiplicity (concrete or `Variable("name")`).
        multiplicity: Multiplicity,
    },

    /// Bare element reference: `String`, `my::Enum`, `MyClass`.
    PackageableElementRef {
        /// The resolved element.
        element: ElementId,
    },

    /// `@(name:Type[mult], …)` or bare `(name:Type[mult], …)` — anonymous
    /// relation type at expression position. Materialises a
    /// `meta::pure::metamodel::relation::RelationType` heap object whose
    /// `columns` slot carries one `Column` per spec, then wraps it in an
    /// `InstanceValue<Type[1]>` so `@(cols)->genericType().rawType`
    /// reflection works (mirrors Java's
    /// `AntlrContextToM3CoreInstance.atomicExpression` line 1098). Bare
    /// `(cols)` (reachable via the `M3CoreParser.g4 atomicExpression:
    /// ... | type` alternative) and `@(cols)` lower identically — Java
    /// routes both through the same processor branch.
    RelationLiteral {
        /// Column triples in source order.
        columns: Vec<RelationColumnLowered>,
    },
    /// `~[…]`-array column-spec literal — `~[name1, …]`,
    /// `~[name1:f1, …]`, or `~[name1:f1:r1, …]` per the
    /// [`ColSpecLiteralKind`] discriminator.
    ///
    /// Mirrors the platform's
    /// [`colSpecArray`](https://example.invalid/colSpecArray) /
    /// `funcColSpecArray` / `aggColSpecArray` grammar functions
    /// (`PCT.grammarCharacters='~[,]'` / `'~[:,:]'` /
    /// `'~[::,::]'`). The discriminator drives both compile-time
    /// type inference (`ColSpecArray` vs `FuncColSpecArray` vs
    /// `AggColSpecArray`) and downstream overload narrowing.
    ColSpecArrayLiteral {
        /// Column triples in source order.
        columns: Vec<RelationColumnLowered>,
        /// Whether the array is plain, func-bearing, or agg-bearing.
        kind: ColSpecLiteralKind,
    },
    /// `~name` single-column literal — plain name, name+lambda
    /// (init function), or name+init+reduce per [`ColSpecLiteralKind`].
    ///
    /// Mirrors the platform's
    /// `meta::pure::functions::relation::colSpec(s,cl):ColSpec<T>[1]`,
    /// `funcColSpec(f,s,cl):FuncColSpec<…,T>[1]`, and
    /// `aggColSpec(map,reduce,s,cl):AggColSpec<…,T>[1]` grammar
    /// functions (`PCT.grammarCharacters='~'` / `'~:'` / `'~::'`).
    ColSpecLiteral {
        /// The single column triple captured at lowering.
        column: RelationColumnLowered,
        /// Whether the form is plain, func-bearing, or agg-bearing.
        kind: ColSpecLiteralKind,
    },

    /// Navigation path expression: `#/Type/p1/p2(args)/p3!alias#`.
    ///
    /// Materialises a `meta::pure::metamodel::path::Path<U,V|m>` instance
    /// at runtime. The lowering captures:
    ///   - `start_type` — the start type with type-args bound (e.g.
    ///     `Firm<Any>`)
    ///   - `steps` — one entry per `/property[(args)]` segment, with the
    ///     property *name* (resolved at runtime against the running type
    ///     to support chain-of-types) and lowered parameter
    ///     `ValueSpec`s
    ///   - `name` — optional alias (`!alias`)
    ///
    /// Property *resolution* through the chain happens in the runtime
    /// `evaluate(Path,U)` native (Stage 4) since each step's "running
    /// type" depends on the previous step's return type, which requires
    /// walking the model with type-arg substitution. The compiler does
    /// validate that the *first* step's property exists on the start
    /// type (a cheap correctness check that catches the most common
    /// typo without committing to full chain inference).
    PathLiteral {
        /// Resolved start type, including type arguments (e.g.
        /// `Firm<Any>`).
        start_type: TypeExpr,
        /// One entry per `/property(args)` segment, in source order.
        steps: Vec<PathStepLowered>,
        /// Optional alias suffix from `!alias`.
        name: Option<SmolStr>,
    },
}

/// Discriminator for [`ExprKind::ColSpecLiteral`] /
/// [`ExprKind::ColSpecArrayLiteral`] — which platform classifier the
/// lowered form maps to.
///
/// The lambda payload that distinguishes `Func` / `Agg` from `Plain`
/// is preserved on the AST `ColumnSpec` rather than threaded through
/// here — overload narrowing only needs the outer classifier
/// (`ColSpec` vs `FuncColSpec` vs `AggColSpec`), and the runtime
/// allocator can re-walk the AST shape if it ever materialises the
/// lambda-bearing heap forms (today they fall back to the plain
/// `ColSpec` / `ColSpecArray` allocation).
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ColSpecLiteralKind {
    /// `~name` / `~[name1, …]` — name(s) only. Maps to
    /// `ColSpec` / `ColSpecArray`.
    Plain,
    /// `~name:f` / `~[name1:f1, …]` — init lambda per column.
    /// Maps to `FuncColSpec` / `FuncColSpecArray`.
    Func,
    /// `~name:f:r` / `~[name1:f1:r1, …]` — init + reduce lambdas
    /// per column. Maps to `AggColSpec` / `AggColSpecArray`.
    Agg,
}

/// One step in a navigation-path literal: `/property` or `/property(args)`.
///
/// Property *resolution* against the running class is deferred to runtime
/// (Stage 4) so this struct only carries the source-level name plus the
/// lowered parameter expressions. The runtime walks the chain
/// `start_type → step[0].return → step[1].return → …`, looking up each
/// step's property on the previous step's return type with type-arg
/// substitution.
#[derive(Debug, Clone, PartialEq)]
pub struct PathStepLowered {
    /// Property name as it appeared in source (e.g. `"name"`,
    /// `"nameWithTitle"`).
    pub property_name: SmolStr,
    /// Parameters for qualified-property steps. Empty for plain
    /// property steps.
    pub parameters: Vec<ValueSpec>,
    /// Source location for this `/property(...)` segment.
    pub source_info: SourceInfo,
}

/// One column in a `RelationLiteral` / `ColSpecArrayLiteral` /
/// `ColSpecLiteral`.
///
/// Captured at lowering so the runtime allocator can materialise the
/// `Column` / `ColSpec` / `FuncColSpec` heap shape (`name`,
/// `nameWildCard=false`, `classifierGenericType` chaining down to
/// `type_element` with `multiplicity`, plus the `function` slot for
/// `FuncColSpec`) without re-resolving names or re-lowering bodies.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationColumnLowered {
    /// Column name (e.g. `"x"` in `~[x:String[1]]`).
    pub name: SmolStr,
    /// Resolved type element (e.g. `String`'s `ElementId`).
    pub type_element: ElementId,
    /// Column multiplicity. Defaults to `ZeroOrOne` when the source omits
    /// `[mult]` (matches the platform's expected `'x:String[0..1]'` form).
    pub multiplicity: Multiplicity,
    /// Optional initialiser lambda from `~name:lam` / `~name:T|...:lam`
    /// (the `Func`/`Agg` `ColSpecLiteralKind` cases). `None` for plain
    /// `~name` / `~name:Type[mult]` columns and for `~[…]` array entries
    /// that aren't lambda-bearing.
    ///
    /// Populated by `lower_column` when the source carried an
    /// `ast_expr::ColumnTypeSpec::Lambda(_)`. The runtime allocator
    /// (`alloc_func_col_spec_literal`) reads this to set the
    /// `FuncColSpec.function` slot, which the `extend` native then
    /// invokes per row.
    pub init_lambda: Option<ValueSpec>,
    /// Optional reduce lambda — the *second* function of an `Agg`-kind
    /// ColSpec (`~name:{p,w,r|map}:y|reduce`). `init_lambda` holds the map
    /// half; this holds the reduce half (AST `ColumnSpec.extra_function`).
    /// `None` for `Plain`/`Func` columns. Populated by `lower_column` when
    /// the source carried an `extra_function` lambda; the runtime allocator
    /// (`alloc_agg_col_spec_literal`) reads it to set the `AggColSpec.reduce`
    /// slot.
    pub reduce_lambda: Option<ValueSpec>,
}

/// Backward-compatible alias: existing code uses `Expression` throughout
/// (`Function.body`, `Constraint.function`, `QualifiedProperty.body`, etc.).
pub type Expression = ValueSpec;

/// A primitive type element (String, Integer, Boolean, etc.).
///
/// **Note:** `Any` and `Nil` are *not* primitive types — they are `Class`
/// instances in the M3 metamodel. Only the 11 concrete types below use this.
///
/// Unlike Classes, primitive types have at most one supertype and form
/// a strict tree (not a DAG). The hierarchy mirrors the Java M3:
///
/// ```text
/// Any (top)
/// ├── String
/// ├── Boolean
/// ├── Byte
/// ├── StrictTime
/// ├── Number (abstract)
/// │   ├── Integer
/// │   ├── Float
/// │   └── Decimal
/// └── Date (abstract)
///     ├── StrictDate
///     └── DateTime
///
/// Nil (bottom — subtype of everything, handled by type checker)
/// ```
///
/// All metadata (name, source, package) lives in the parallel `ElementNode`.
/// Bootstrapped in Chunk 0.
#[derive(Debug, Clone, PartialEq)]
pub struct PrimitiveType {
    /// The single supertype for this primitive.
    ///
    /// All 11 primitives have `Some(parent)` pointing to their parent in
    /// the type tree (e.g., `Integer.super_type = Some(NUMBER_ID)`).
    pub super_type: Option<crate::ids::ElementId>,
    /// Type-variable VALUES bound on the parent at declaration time —
    /// `Primitive OP8 extends OP(8)` stores `[ConstValue::Integer(8)]`
    /// here so cast-time constraint inheritance can evaluate OP's
    /// `$this < $x` constraint with `x = 8`. Empty when the parent is
    /// non-parametric or the binding is absent.
    pub super_type_value_arguments: Vec<ConstValue>,
    /// Type-variable parameters declared on a parametric primitive, e.g.
    /// `Primitive P(x:Integer[1]) extends Integer` gives one parameter
    /// named `x`. Bound at cast/new time from the type-variable-values
    /// supplied at the reference site (`@P(8)` binds `x = 8`).
    pub type_variable_parameters: Vec<Parameter>,
    /// Constraints declared in the `[…]` block after the base type.
    /// Checked at cast/new time with `$this` bound to the candidate
    /// value and every `type_variable_parameters` entry bound to its
    /// call-site value. Empty for bootstrap primitives.
    pub constraints: Vec<crate::nodes::class::Constraint>,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn type_expr_named_simple() {
        let ty = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 2,
            },
            type_arguments: vec![],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![],
            source_info: None,
        };
        assert!(matches!(ty, TypeExpr::Named { .. }));
    }

    #[test]
    fn type_expr_named_with_type_args() {
        // List<String>
        let string_ty = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 2,
            },
            type_arguments: vec![],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![],
            source_info: None,
        };
        let list_ty = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 20,
            },
            type_arguments: vec![string_ty],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![],
            source_info: None,
        };
        if let TypeExpr::Named { type_arguments, .. } = &list_ty {
            assert_eq!(type_arguments.len(), 1);
        }
    }

    #[test]
    fn type_expr_named_with_value_args() {
        // Varchar(255)
        let ty = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 12,
            },
            type_arguments: vec![],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![ConstValue::Integer(255)],
            source_info: None,
        };
        if let TypeExpr::Named {
            value_arguments, ..
        } = &ty
        {
            assert_eq!(value_arguments.len(), 1);
            assert!(matches!(&value_arguments[0], ConstValue::Integer(255)));
        }
    }

    #[test]
    fn type_expr_function_type() {
        // {String[1] -> Boolean[1]}
        let string = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 2,
            },
            type_arguments: vec![],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![],
            source_info: None,
        };
        let boolean = TypeExpr::Named {
            element: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 5,
            },
            type_arguments: vec![],
            multiplicity_arguments: Vec::new(),
            value_arguments: vec![],
            source_info: None,
        };
        let ft = TypeExpr::FunctionType {
            parameters: vec![(string, Multiplicity::PureOne)],
            return_type: Box::new(boolean),
            return_multiplicity: Multiplicity::PureOne,
        };
        assert!(matches!(ft, TypeExpr::FunctionType { .. }));
    }

    #[test]
    fn type_expr_generic() {
        let ty = TypeExpr::Generic(SmolStr::new("T"));
        assert!(matches!(ty, TypeExpr::Generic(_)));
    }

    #[test]
    fn multiplicity_variants() {
        assert!(matches!(Multiplicity::PureOne, Multiplicity::PureOne));
        assert!(matches!(Multiplicity::ZeroOrOne, Multiplicity::ZeroOrOne));
        assert!(matches!(Multiplicity::ZeroOrMany, Multiplicity::ZeroOrMany));
        assert!(matches!(Multiplicity::OneOrMany, Multiplicity::OneOrMany));
        let r = Multiplicity::Range {
            lower: 2,
            upper: Some(5),
        };
        assert!(matches!(
            r,
            Multiplicity::Range {
                lower: 2,
                upper: Some(5)
            }
        ));
    }
}
