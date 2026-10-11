// Copyright 2024 Goldman Sachs
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

//! Canonical M3 metamodel fully-qualified names.
//!
//! Centralises the FQN strings the runtime uses for:
//! - `alloc_dynamic(<FQN>)` — the classifier stored on heap wrappers
//!   (`Property`, `QualifiedProperty`, `GenericType`, etc.)
//! - `resolve_by_path(&[segments])` — resolving a classifier string back
//!   to an [`ElementId`] for wrapper-kind dispatch (see
//!   `Evaluator::apply_object_callable`).
//!
//! Single source of truth keeps the allocation site and the dispatch site
//! in sync — if the canonical FQN of one of these M3 classes changes, it
//! changes here, not in every caller.
//!
//! All constants reference classes bootstrapped from `m3.pure`; resolving
//! any of them on a fully loaded [`PureModel`] must succeed.

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;

type Classifier = str;

/// `meta::pure::metamodel::function::property::Property` — heap wrapper for
/// a Class property reference, surfaced by `.properties` on a Class.
pub const PROPERTY: &Classifier = "meta::pure::metamodel::function::property::Property";

/// `meta::pure::metamodel::function::property::QualifiedProperty` — heap
/// wrapper for a derived property reference, surfaced by
/// `.qualifiedProperties` on a Class.
pub const QUALIFIED_PROPERTY: &Classifier =
    "meta::pure::metamodel::function::property::QualifiedProperty";

/// `meta::pure::metamodel::function::LambdaFunction` — M3 class for anonymous
/// lambdas; recognised by the `New` native's lambda-clone shortcut.
pub const LAMBDA_FUNCTION: &Classifier = "meta::pure::metamodel::function::LambdaFunction";

/// `meta::pure::metamodel::function::Function` — M3 base class for all
/// functions; also accepted by the lambda-clone shortcut.
pub const FUNCTION: &Classifier = "meta::pure::metamodel::function::Function";

/// `meta::pure::metamodel::extension::Stereotype` — heap wrapper for a
/// `<<stereotype>>` annotation.
pub const STEREOTYPE: &Classifier = "meta::pure::metamodel::extension::Stereotype";

/// `meta::pure::metamodel::extension::TaggedValue` — heap wrapper for a
/// `{tag = 'value'}` annotation.
pub const TAGGED_VALUE: &Classifier = "meta::pure::metamodel::extension::TaggedValue";

/// `meta::pure::metamodel::extension::Tag` — the tag *declaration* on a
/// `Profile.tags` list. Distinct from [`TAGGED_VALUE`] (the annotation
/// instance carrying a string value). The
/// `meta::pure::functions::meta::tag(profile, str)` native returns a
/// `Tag` (a declaration handle), not a `TaggedValue`.
pub const TAG: &Classifier = "meta::pure::metamodel::extension::Tag";

/// `meta::pure::functions::collection::TreeNode` — anonymous tree node
/// with a single `childrenData: TreeNode[*]` property. Used by
/// `replaceTreeNode` to allocate replacement clones during the
/// persistent-copy traversal.
pub const TREE_NODE: &Classifier = "meta::pure::functions::collection::TreeNode";

/// `meta::pure::metamodel::type::Class` — the M3 metatype every
/// user-defined Class is an instance of. Used by the type-info back-fill
/// in `New::execute` to identify a `Class<T>`-typed first argument
/// (resolve to its [`ElementId`] and compare against the inferred
/// `args[0].type_info`'s outer element). Identity-based, never
/// classifier-string compared.
pub const CLASS: &Classifier = "meta::pure::metamodel::type::Class";
/// `meta::pure::metamodel::type::Enumeration`.
pub const ENUMERATION: &Classifier = "meta::pure::metamodel::type::Enumeration";
/// `meta::pure::metamodel::type::PrimitiveType`.
pub const PRIMITIVE_TYPE: &Classifier = "meta::pure::metamodel::type::PrimitiveType";
/// `meta::pure::metamodel::Package`.
pub const PACKAGE: &Classifier = "meta::pure::metamodel::Package";
/// `meta::pure::metamodel::function::NativeFunction`.
pub const NATIVE_FUNCTION: &Classifier = "meta::pure::metamodel::function::NativeFunction";
/// `meta::pure::metamodel::function::ConcreteFunctionDefinition`.
pub const CONCRETE_FUNCTION_DEFINITION: &Classifier =
    "meta::pure::metamodel::function::ConcreteFunctionDefinition";

/// `meta::pure::metamodel::type::Nil` — the bottom type. Reserved by the
/// language and not user-instantiable; `^Nil()` is rejected at the
/// `New` / `NewWithKeyExpressions` / `DynamicNew` natives' entry. Pinned
/// by platform test `testNewNil`. Identity-resolved via `m3_paths::resolve`
/// because `class_fqn` returns just `"Nil"` (top-level type, no package
/// prefix), so a string compare in the natives wouldn't match.
pub const NIL: &Classifier = "meta::pure::metamodel::type::Nil";

/// `meta::pure::metamodel::function::KeyExpression` — the M3 wrapper a
/// Pure-source `new(class, id, [keyExpressions])` call uses to package a
/// (key, value, add) property assignment. The `NewWithKeyExpressions`
/// native (registered separately from compiler-internal `New`) decodes
/// these by walking the heap object's `key`, `expression`, and `add`
/// slots — never probing the classifier string. The `KeyExpression`
/// objects themselves are constructed via the compiler-internal fast
/// path so this decoder is the canonical home of all `KeyExpression`
/// recognition.
pub const KEY_EXPRESSION: &Classifier = "meta::pure::metamodel::function::KeyExpression";

/// `meta::pure::metamodel::type::generics::GenericType` — heap wrapper for
/// a parameterised type instance returned by `genericType(...)`.
pub const GENERIC_TYPE: &Classifier = "meta::pure::metamodel::type::generics::GenericType";

/// `meta::pure::metamodel::relationship::Generalization` — heap wrapper for
/// a single supertype edge returned by `.generalizations`.
pub const GENERALIZATION: &Classifier = "meta::pure::metamodel::relationship::Generalization";

/// `meta::pure::functions::meta::SourceInformation` — return shape of the
/// `sourceInformation(Any[1]):SourceInformation[0..1]` native.
pub const SOURCE_INFORMATION: &Classifier = "meta::pure::functions::meta::SourceInformation";

/// `meta::pure::functions::collection::List` — a `List<T>(values=…)` wrapper
/// used pervasively by `evaluate(Function, List[*])`.
pub const LIST: &Classifier = "meta::pure::functions::collection::List";

/// `meta::pure::functions::collection::Pair` — `Pair<U,V>(first=…, second=…)`
/// wrapper underpinning `pair()` and Map entry iteration (`keyValues`).
pub const PAIR: &Classifier = "meta::pure::functions::collection::Pair";

/// `meta::pure::functions::collection::MapStats` — single-property
/// (`getIfAbsentCounter:Integer[1]`) wrapper returned by the
/// `getMapStats` native; mirrors Java Pure's `PureMapStats` exposed
/// through `MapCoreInstance.getStats()`.
pub const MAP_STATS: &Classifier = "meta::pure::functions::collection::MapStats";

/// `meta::pure::metamodel::type::GetterOverride` — heap wrapper carrying
/// the four override lambdas (`getterOverrideToOne`,
/// `getterOverrideToMany`, `propertyOverride`, `defaultOverride`) plus
/// the `hiddenPayload` slot. Bound to `instance.elementOverride` by
/// `dynamicNew`'s hook-bearing overloads so absent property reads can
/// dispatch through the appropriate lambda.
pub const GETTER_OVERRIDE: &Classifier = "meta::pure::metamodel::type::GetterOverride";

/// `meta::pure::metamodel::type::ConstraintsOverride` — heap wrapper
/// carrying a `constraintsManager` lambda
/// (`Function<{Any[1]->Any[1]}>`) used by the 6-arg `dynamicNew`
/// overload to replace the default constraint-check pass. When the
/// manager is invoked, its return value replaces the dynamicNew result
/// (parity with Java `DefaultConstraintHandler.handleConstraints`).
pub const CONSTRAINTS_OVERRIDE: &Classifier = "meta::pure::metamodel::type::ConstraintsOverride";

/// `meta::pure::metamodel::type::ConstraintsGetterOverride` — combined
/// override carrying both the getter-hook lambdas (inherited from
/// `GetterOverride`) and the `constraintsManager`. Allocated by
/// `dynamicNew` when the 6-arg overload supplies *both* getter hooks
/// and a constraints manager.
pub const CONSTRAINTS_GETTER_OVERRIDE: &Classifier =
    "meta::pure::metamodel::type::ConstraintsGetterOverride";

/// `meta::pure::metamodel::valuespecification::VariableExpression` —
/// AST-metamodel node for a `$name` variable reference. Produced by
/// `deactivate(varRef)` so the AST can be introspected.
pub const VARIABLE_EXPRESSION: &Classifier =
    "meta::pure::metamodel::valuespecification::VariableExpression";

/// `meta::pure::metamodel::type::FunctionType` — heap wrapper exposing
/// a lambda's `parameters: VariableExpression[*]`, `returnType: GenericType[1]`,
/// and `returnMultiplicity: Multiplicity[1]`. Produced as
/// `genericType($lambda).typeArguments[0].rawType` so reflective walks
/// match Java Pure's m3 metamodel.
pub const FUNCTION_TYPE: &Classifier = "meta::pure::metamodel::type::FunctionType";

/// `meta::pure::metamodel::multiplicity::Multiplicity` — heap wrapper for
/// a `[m..n]` multiplicity declaration. Used by `FunctionType` /
/// `VariableExpression` to expose lambda parameter and return-side
/// cardinalities through the reflective metamodel.
pub const MULTIPLICITY: &Classifier = "meta::pure::metamodel::multiplicity::Multiplicity";

/// `meta::pure::metamodel::multiplicity::MultiplicityValue` — heap wrapper
/// for the `lowerBound` / `upperBound` slot value on a Multiplicity
/// (`MultiplicityValue.value : Integer[1]`). Required so reflective
/// reads like `$m.lowerBound.value` resolve to a heap object whose
/// `value` slot is the bound integer (per
/// `legend-pure-core/.../platform/pure/grammar/m3.pure:1400`).
pub const MULTIPLICITY_VALUE: &Classifier =
    "meta::pure::metamodel::multiplicity::MultiplicityValue";

/// `meta::pure::metamodel::valuespecification::InstanceValue` —
/// AST-metamodel node for a literal / collection / pre-computed value.
/// Produced by `deactivate(lit)` or `deactivate([collection])`.
pub const INSTANCE_VALUE: &Classifier = "meta::pure::metamodel::valuespecification::InstanceValue";

/// `meta::pure::metamodel::valuespecification::FunctionExpression` —
/// AST-metamodel base for function-call nodes.
pub const FUNCTION_EXPRESSION: &Classifier =
    "meta::pure::metamodel::valuespecification::FunctionExpression";

/// `meta::pure::metamodel::valuespecification::SimpleFunctionExpression` —
/// AST-metamodel node for a resolved function call. Produced by
/// `deactivate(fnCall)`; carries `func`, `functionName`, and
/// `parametersValues`.
pub const SIMPLE_FUNCTION_EXPRESSION: &Classifier =
    "meta::pure::metamodel::valuespecification::SimpleFunctionExpression";

/// `meta::pure::test::surveyor::TestResult` — heap-object shape returned
/// by the Pure-level test surveyor's per-test result builder.
pub const TEST_RESULT: &Classifier = "meta::pure::test::surveyor::TestResult";

/// `meta::pure::test::pct::PCTManifest` — heap-object shape returned by the
/// `loadPCTManifest` native; carries the resolved `adapter:Function<Any>[1]`
/// and the `exclusions:Map<Function<Any>,String>[1]` of expected-failure
/// (test FQN → expected error message) pairs that drive PCT bucket flipping
/// in `executePCTTest`.
pub const PCT_MANIFEST: &Classifier = "meta::pure::test::pct::PCTManifest";

/// `meta::pure::metamodel::relation::RelationType` — heap classifier for
/// an anonymous relation type (column bag). Allocated by the
/// `RelationLiteral` lowering and by the `addColumns` native; navigated
/// by the native via `obj._columns()`. Identified via M3 `ElementId`,
/// never via classifier-string suffix.
pub const RELATION_TYPE: &Classifier = "meta::pure::metamodel::relation::RelationType";

/// `meta::pure::metamodel::relation::Column` — heap classifier for a
/// single relation column carrying `name`, `nameWildCard`, and the
/// `classifierGenericType` chain (`typeArguments=[null, <typeGT>]`,
/// `multiplicityArguments=[<mult>]`).
pub const COLUMN: &Classifier = "meta::pure::metamodel::relation::Column";

/// `meta::pure::metamodel::relation::TDS` — heap classifier for a
/// Tabular Data Set. The Pure-side metaclass is
/// `Class TDS<T> extends Relation<T> { csv: String[1]; }`. Allocated by
/// the `stringToTDS` native (and indirectly by every `#TDS\n…\n#`
/// literal once the compile-time lowerer rewrites it as
/// `stringToTDS('<csv>')->cast(@TDS<…>)`).
pub const TDS: &Classifier = "meta::pure::metamodel::relation::TDS";

/// `meta::pure::metamodel::relation::TDSTuple` — heap classifier for the
/// individual rows under a TDS's `rows: T[*]` slot. Mirrors Java's
/// TDSExtension classifier override: each row's `classifierGenericType.
/// rawType` is set to the TDS's inferred column `RelationType` so
/// Pure-level typing sees a row as an instance of `T` (the structural
/// row type), enabling `$row.colName` Column-application + structural
/// `$row->instanceOf((cols))`.
pub const TDS_TUPLE: &Classifier = "meta::pure::metamodel::relation::TDSTuple";

/// `meta::pure::metamodel::relation::ColSpec` — heap classifier for
/// the single-column `~name` literal. Carries `name: String[1]` plus
/// `classifierGenericType.typeArguments[0].rawType` pointing at a
/// `Column` with the column metadata, mirroring the platform's
/// `colSpec<T>(s:String[1], cl:T[1]):ColSpec<T>[1]` shape.
pub const COL_SPEC: &Classifier = "meta::pure::metamodel::relation::ColSpec";

/// `meta::pure::metamodel::relation::ColSpecArray` — heap classifier for
/// the `~[col:Type[mult], …]` literal. Carries `names: String[*]` plus
/// `classifierGenericType.typeArguments[0].rawType` pointing at a
/// `RelationType` with the column metadata, mirroring Java's path through
/// `ColSpecArrayInstance._classifierGenericType()._typeArguments()
/// .getFirst()._rawType()._columns()`.
pub const COL_SPEC_ARRAY: &Classifier = "meta::pure::metamodel::relation::ColSpecArray";

/// `meta::pure::metamodel::relation::FuncColSpec` — heap classifier for
/// the `~name:lam` lambda-bearing column literal. Carries
/// `name: String[1]` and `function: Function<Z>[1]` (the init lambda
/// the user supplied). Allocated by the runtime when a `ColSpecLiteral`
/// with `kind = Func` is evaluated; consumed by `extend`.
pub const FUNC_COL_SPEC: &Classifier = "meta::pure::metamodel::relation::FuncColSpec";

/// `meta::pure::metamodel::relation::AggColSpec` — heap classifier for the
/// `~name:map:reduce` aggregation column literal. Carries `name: String[1]`,
/// `map: Function<Z>[1]` (the per-row map lambda) and `reduce: Function<V>[1]`
/// (the lambda collapsing the collected `V[*]`). Allocated by the runtime when
/// a `ColSpecLiteral` with `kind = Agg` is evaluated; consumed by the
/// (engine-side) aggregating `extend`.
pub const AGG_COL_SPEC: &Classifier = "meta::pure::metamodel::relation::AggColSpec";

/// `meta::pure::functions::relation::SortInfo` — heap classifier for a
/// single sort key. Carries `column: ColSpec<T>[1]` plus
/// `direction: SortType[1]`. Allocated by the `ascending` / `descending`
/// natives, consumed by `sort` (and downstream by `over`).
pub const SORT_INFO: &Classifier = "meta::pure::functions::relation::SortInfo";

/// `meta::pure::functions::relation::SortType` — enumeration with members
/// `ASC` and `DESC`. Used as the `direction` slot value on a `SortInfo`.
pub const SORT_TYPE: &Classifier = "meta::pure::functions::relation::SortType";

/// `meta::relational::metamodel::execute::ResultSet` — heap shape returned
/// by `executeInDb` / `fetchDb*MetaData` natives. Carries
/// `columnNames: String[*]`, `rows: Row[*]`,
/// `executionTimeInNanoSecond: Integer[1]`,
/// `connectionAcquisitionTimeInNanoSecond: Integer[1]`,
/// optional `executionPlanInformation: String[0..1]`, and an optional
/// `dataSource: DataSource[0..1]`.
pub const RELATIONAL_RESULT_SET: &Classifier = "meta::relational::metamodel::execute::ResultSet";

/// `meta::relational::metamodel::execute::Row` — heap shape for a single
/// fetched row inside a `ResultSet`. Carries `values: Any[*]` and a
/// `parent: ResultSet[1]` back-pointer (required for the `Row.value(name)`
/// qualified property to resolve the column index).
pub const RELATIONAL_ROW: &Classifier = "meta::relational::metamodel::execute::Row";

/// `meta::relational::metamodel::SQLNull` — sentinel singleton value for
/// SQL NULL cells inside a `Row`. One handle is allocated per ResultSet
/// and shared across all NULL slots.
pub const RELATIONAL_SQL_NULL: &Classifier = "meta::relational::metamodel::SQLNull";

/// `meta::relational::metamodel::Column` — heap shape for a Database
/// column declaration. Carries `name: String[1]`, `type: DataType[1]`,
/// `nullable: Boolean[1]`. **Distinct from** [`COLUMN`] which is the
/// relation-DSL `meta::pure::metamodel::relation::Column`.
pub const RELATIONAL_COLUMN: &Classifier = "meta::relational::metamodel::Column";

/// `meta::relational::runtime::DatabaseType` — enumeration discriminating
/// the backend engine (DuckDB, H2, Postgres, Snowflake, …). Used by the
/// relational-store extension to route `executeInDb`-class natives to the
/// appropriate driver. Resolved once per native call from
/// `databaseConnection.type` and compared by enum member name (not
/// classifier string).
pub const DATABASE_TYPE: &Classifier = "meta::relational::runtime::DatabaseType";

/// `meta::pure::profiles::equality` — Profile whose `Key` stereotype
/// marks class properties as structural-equality keys. A `<<equality.Key>>`
/// stereotype ref matches iff `profile` resolves to this FQN and
/// `value == "Key"`.
pub const EQUALITY_PROFILE: &Classifier = "meta::pure::profiles::equality";

/// Resolve a `::`-qualified FQN to its [`ElementId`], returning `None` if
/// any segment doesn't resolve. Used alongside the constants above to
/// compare heap-object classifiers by structural identity instead of
/// textual suffix matching.
///
/// Delegates to [`PureModel::resolve_fqn_str`] which walks segments
/// inline — no per-call `Vec<SmolStr>` allocation.
#[must_use]
pub fn resolve(model: &PureModel, fqn: &Classifier) -> Option<ElementId> {
    model.resolve_fqn_str(fqn)
}
