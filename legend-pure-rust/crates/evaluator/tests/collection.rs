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

//! The collection natives driven through a real `Evaluator` over
//! hand-built nodes: the lambda-driven iterators, the natives that
//! allocate heap objects (`zip`) and the `Map` natives. Literal-only
//! cases stay inline in `native/collection.rs`.

use legend_pure_evaluator::builders::on_both_paths;
use std::cell::RefCell;
use std::rc::Rc;

use legend_pure_evaluator::builders::{ModelBuilder, expr, named, param};
use legend_pure_evaluator::error::{PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::{MapState, Value, ValueKey};
use legend_pure_metamodel::bootstrap::INTEGER_ID;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{Multiplicity, ValueSpec};

const FOLD: &str = "fold_T_MANY__Function_1__V_m__V_m_";
const DROP: &str = "drop_T_MANY__Integer_1__T_MANY_";
const TAKE: &str = "take_T_MANY__Integer_1__T_MANY_";
const ZIP: &str = "zip_T_MANY__U_MANY__Pair_MANY_";
const FIRST: &str = "first_T_MANY__T_$0_1$_";
const REMOVE_DUPLICATES: &str = "removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_";
const SORT: &str = "sort_T_m__Function_$0_1$__Function_$0_1$__T_m_";
const SIZE: &str = "size_Any_MANY__Integer_1_";
const RANGE: &str = "range_Integer_1__Integer_1__Integer_1__Integer_MANY_";
const MAP_M: &str = "map_T_m__Function_1__V_m_";
const MAP_MANY: &str = "map_T_MANY__Function_1__V_MANY_";
const MAP_ZERO_ONE: &str = "map_T_$0_1$__Function_1__V_$0_1$_";
const SLICE: &str = "slice_T_MANY__Integer_1__Integer_1__T_MANY_";
const FIND: &str = "find_T_MANY__Function_1__T_$0_1$_";
const AT: &str = "at_T_MANY__Integer_1__T_1_";
const EXISTS: &str = "exists_T_MANY__Function_1__Boolean_1_";
const INDEX_OF: &str = "indexOf_T_MANY__T_1__Integer_1_";
const TAIL: &str = "tail_T_MANY__T_MANY_";
const TO_ONE: &str = "toOne_T_MANY__T_1_";
const TO_ONE_WITH_MESSAGE: &str = "toOne_T_MANY__String_1__T_1_";
const FILTER: &str = "filter_T_MANY__Function_1__T_MANY_";
const NOT: &str = "not_Boolean_1__Boolean_1_";
const CONCATENATE: &str = "concatenate_T_MANY__T_MANY__T_MANY_";
const FOR_ALL: &str = "forAll_T_MANY__Function_1__Boolean_1_";
const INIT: &str = "init_T_MANY__T_MANY_";
const LAST: &str = "last_T_MANY__T_$0_1$_";
const PUT: &str = "put_Map_1__U_1__V_1__Map_1_";
const PUT_ALL_PAIRS: &str = "putAll_Map_1__Pair_MANY__Map_1_";
const PUT_ALL_MAP: &str = "putAll_Map_1__Map_1__Map_1_";
const IS_EMPTY: &str = "isEmpty_Any_MANY__Boolean_1_";
const ADD: &str = "add_T_MANY__T_1__T_$1_MANY$_";
const ADD_AT: &str = "add_T_MANY__Integer_1__T_1__T_$1_MANY$_";
const REVERSE: &str = "reverse_T_m__T_m_";
const GET: &str = "get_Map_1__U_1__V_$0_1$_";
const KEYS: &str = "keys_Map_1__U_MANY_";
const VALUES: &str = "values_Map_1__V_MANY_";
const NEW_MAP: &str = "newMap_Pair_MANY__Map_1_";
const NEW_MAP_WITH_KEYS: &str = "newMap_Pair_MANY__Property_MANY__Map_1_";
const TIMES: &str = "times_Number_MANY__Number_1_";
const EQ: &str = "eq_Any_1__Any_1__Boolean_1_";
const PLUS: &str = "plus_Number_MANY__Number_1_";
const MINUS: &str = "minus_Number_MANY__Number_1_";

fn model() -> PureModel {
    ModelBuilder::new().finish()
}

/// Evaluate `spec` on a fresh evaluator over the bootstrap.
fn run(spec: &ValueSpec) -> Value {
    let model = model();
    let registry = NativeRegistry::standard();
    Evaluator::new(&model, &registry)
        .eval(spec)
        .unwrap_or_else(|e| panic!("evaluation failed: {e:?}"))
}

/// Evaluate `spec` on a fresh evaluator and return the message of the
/// `EvaluationError` it raises.
fn run_err(spec: &ValueSpec) -> String {
    let model = model();
    let registry = NativeRegistry::standard();
    let err = Evaluator::new(&model, &registry)
        .eval(spec)
        .expect_err("expected an error");
    match err.into_data().kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => m,
        other => panic!("expected an EvaluationError, got {other:?}"),
    }
}

fn ints(items: &[i64]) -> ValueSpec {
    expr::collection(items.iter().map(|&i| expr::int(i)).collect())
}

/// The integers of a result, whatever its multiplicity shape (`Unit`,
/// scalar or `Collection`).
fn as_ints(value: &Value) -> Vec<i64> {
    value
        .to_collection()
        .iter()
        .map(|v| v.as_integer().expect("an Integer"))
        .collect()
}

/// `{a, b | $a + $b}` over integers (Pure `plus` takes its operands as
/// one collection).
fn plus_lambda(a: &str, b: &str) -> ValueSpec {
    expr::lambda(
        vec![
            param(a, named(INTEGER_ID), Multiplicity::PureOne),
            param(b, named(INTEGER_ID), Multiplicity::PureOne),
        ],
        vec![expr::call(
            PLUS,
            vec![expr::collection(vec![expr::var(a), expr::var(b)])],
        )],
    )
}

#[test]
fn fold_threads_the_accumulator_through_the_lambda() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        let sum = expr::call(
            FOLD,
            vec![ints(&[1, 2, 3, 4]), plus_lambda("x", "y"), expr::int(7)],
        );
        assert_eq!(eval.eval(&sum).unwrap(), Value::Integer(17));
        let ordered = expr::call(
            FOLD,
            vec![
                ints(&[1, 2, 3]),
                expr::lambda(
                    vec![
                        param("x", named(INTEGER_ID), Multiplicity::PureOne),
                        param("acc", named(INTEGER_ID), Multiplicity::PureOne),
                    ],
                    vec![expr::call(
                        MINUS,
                        vec![expr::collection(vec![expr::var("x"), expr::var("acc")])],
                    )],
                ),
                expr::int(10),
            ],
        );
        assert_eq!(eval.eval(&ordered).unwrap(), Value::Integer(-8));
    });
}

#[test]
fn fold_over_an_empty_collection_returns_the_identity() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        let folded = expr::call(FOLD, vec![ints(&[]), plus_lambda("x", "y"), expr::int(1)]);
        assert_eq!(eval.eval(&folded).unwrap(), Value::Integer(1));
    });
}

#[test]
fn drop_skips_the_first_n_and_clamps_the_count() {
    on_both_paths(|| {
        let drop = |items: &[i64], n: i64| run(&expr::call(DROP, vec![ints(items), expr::int(n)]));
        assert_eq!(as_ints(&drop(&[1, 2, 3], 1)), [2, 3]);
        assert_eq!(as_ints(&drop(&[1, 2, 3], 5)), Vec::<i64>::new());
        assert_eq!(as_ints(&drop(&[1, 2, 3], -1)), [1, 2, 3]);
        assert_eq!(as_ints(&drop(&[], 1)), Vec::<i64>::new());
    });
}

#[test]
fn take_keeps_the_first_n_and_clamps_the_count() {
    on_both_paths(|| {
        let take = |items: &[i64], n: i64| run(&expr::call(TAKE, vec![ints(items), expr::int(n)]));
        assert_eq!(as_ints(&take(&[1, 2, 3], 2)), [1, 2]);
        assert_eq!(as_ints(&take(&[1, 2, 3], 5)), [1, 2, 3]);
        assert_eq!(as_ints(&take(&[1, 2, 3], -1)), Vec::<i64>::new());
        assert_eq!(as_ints(&take(&[], 1)), Vec::<i64>::new());
    });
}

fn strings(items: &[&str]) -> ValueSpec {
    expr::collection(items.iter().map(|s| expr::string(s)).collect())
}

#[test]
fn zip_pairs_up_to_the_shorter_side_and_the_pairs_read_back() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        let zipped = eval
            .eval(&expr::call(
                ZIP,
                vec![ints(&[1, 2, 3]), strings(&["a", "b"])],
            ))
            .unwrap();
        let pairs = zipped.to_collection();
        assert_eq!(pairs.len(), 2);
        for pair in &pairs {
            let Value::Object(handle) = pair else {
                panic!("expected a Pair object, got {pair:?}");
            };
            assert_eq!(
                eval.heap().classifier(handle).unwrap(),
                "meta::pure::functions::collection::Pair"
            );
        }
        eval.context_mut().push_scope();
        eval.context_mut().set("p", pairs[1].clone());
        assert_eq!(
            eval.eval(&expr::prop(expr::var("p"), "first")).unwrap(),
            Value::Integer(2)
        );
        assert_eq!(
            eval.eval(&expr::prop(expr::var("p"), "second")).unwrap(),
            Value::String("b".into())
        );
        assert!(run(&expr::call(ZIP, vec![ints(&[]), strings(&["a"])])).is_empty());
        assert!(run(&expr::call(ZIP, vec![ints(&[1]), strings(&[])])).is_empty());
    });
}

#[test]
fn first_returns_the_head_or_nothing() {
    on_both_paths(|| {
        assert_eq!(
            run(&expr::call(FIRST, vec![strings(&["a", "b"])])),
            Value::String("a".into())
        );
        assert_eq!(
            run(&expr::call(FIRST, vec![expr::int(7)])),
            Value::Integer(7)
        );
        assert_eq!(run(&expr::call(FIRST, vec![ints(&[])])), Value::Unit);
    });
}

/// `{x | $x * $x}` over integers.
fn square_lambda() -> ValueSpec {
    expr::lambda(
        vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
        vec![expr::call(
            TIMES,
            vec![expr::collection(vec![expr::var("x"), expr::var("x")])],
        )],
    )
}

#[test]
fn remove_duplicates_honours_the_key_and_the_equality_lambdas() {
    on_both_paths(|| {
        let dedup = |col: ValueSpec, key: ValueSpec, eql: ValueSpec| {
            as_ints(&run(&expr::call(REMOVE_DUPLICATES, vec![col, key, eql])))
        };
        let none = || expr::collection(Vec::new());
        assert_eq!(dedup(ints(&[1, 2, 1, 3, 2]), none(), none()), [1, 2, 3]);
        assert_eq!(
            dedup(ints(&[1, -1, 2, -2, 3]), square_lambda(), none()),
            [1, 2, 3]
        );
        let eql = expr::lambda(
            vec![
                param("a", named(INTEGER_ID), Multiplicity::PureOne),
                param("b", named(INTEGER_ID), Multiplicity::PureOne),
            ],
            vec![expr::call(EQ, vec![expr::var("a"), expr::var("b")])],
        );
        assert_eq!(dedup(ints(&[3, 3, 4, 3]), none(), eql), [3, 4]);
        assert_eq!(dedup(ints(&[]), none(), none()), Vec::<i64>::new());
    });
}

#[test]
fn sort_orders_by_natural_order_key_or_comparator() {
    on_both_paths(|| {
        let sort = |col: ValueSpec, key: ValueSpec, comp: ValueSpec| {
            as_ints(&run(&expr::call(SORT, vec![col, key, comp])))
        };
        let none = || expr::collection(Vec::new());
        assert_eq!(
            sort(ints(&[342, 5, 2, 171, 1]), none(), none()),
            [1, 2, 5, 171, 342]
        );
        assert_eq!(
            sort(ints(&[3, -1, -2]), square_lambda(), none()),
            [-1, -2, 3]
        );
        let descending = expr::lambda(
            vec![
                param("x", named(INTEGER_ID), Multiplicity::PureOne),
                param("y", named(INTEGER_ID), Multiplicity::PureOne),
            ],
            vec![expr::call(
                MINUS,
                vec![expr::collection(vec![expr::var("y"), expr::var("x")])],
            )],
        );
        assert_eq!(
            sort(ints(&[342, 5, 2, 171, 1]), none(), descending),
            [342, 171, 5, 2, 1]
        );
        assert_eq!(sort(ints(&[]), none(), none()), Vec::<i64>::new());
    });
}

#[test]
fn size_counts_elements_of_any_multiplicity() {
    on_both_paths(|| {
        let size = |v: ValueSpec| run(&expr::call(SIZE, vec![v]));
        assert_eq!(size(strings(&["a", "b", "c"])), Value::Integer(3));
        assert_eq!(size(expr::int(9)), Value::Integer(1));
        assert_eq!(size(ints(&[])), Value::Integer(0));
        assert_eq!(
            size(expr::call(ZIP, vec![ints(&[1, 2]), strings(&["a", "b"])])),
            Value::Integer(2)
        );
    });
}

#[test]
fn range_follows_the_sign_of_the_step_and_rejects_zero() {
    on_both_paths(|| {
        let range = |start: i64, stop: i64, step: i64| {
            expr::call(
                RANGE,
                vec![expr::int(start), expr::int(stop), expr::int(step)],
            )
        };
        assert_eq!(as_ints(&run(&range(0, 10, 3))), [0, 3, 6, 9]);
        assert_eq!(as_ints(&run(&range(5, 0, -2))), [5, 3, 1]);
        assert_eq!(as_ints(&run(&range(2, 2, 1))), Vec::<i64>::new());
        assert_eq!(as_ints(&run(&range(5, 0, 1))), Vec::<i64>::new());
        assert_eq!(run_err(&range(0, 5, 0)), "range step must not be 0");
    });
}

#[test]
fn map_applies_the_lambda_and_flattens_the_results() {
    on_both_paths(|| {
        assert_eq!(
            as_ints(&run(&expr::call(
                MAP_M,
                vec![ints(&[1, 2, 3]), square_lambda()]
            ))),
            [1, 4, 9]
        );
        let twice = expr::lambda(
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            vec![expr::collection(vec![expr::var("x"), expr::var("x")])],
        );
        assert_eq!(
            as_ints(&run(&expr::call(MAP_MANY, vec![ints(&[1, 2]), twice]))),
            [1, 1, 2, 2]
        );
        let nothing = expr::lambda(
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            vec![expr::collection(Vec::new())],
        );
        assert_eq!(
            run(&expr::call(MAP_MANY, vec![ints(&[1, 2]), nothing])),
            Value::Unit
        );
        assert_eq!(
            run(&expr::call(MAP_ZERO_ONE, vec![ints(&[]), square_lambda()])),
            Value::Unit
        );
    });
}

#[test]
fn slice_clamps_its_bounds_and_rejects_inverted_ones_with_java_text() {
    on_both_paths(|| {
        let slice = |items: &[i64], start: i64, end: i64| {
            expr::call(SLICE, vec![ints(items), expr::int(start), expr::int(end)])
        };
        assert_eq!(as_ints(&run(&slice(&[1, 2, 3, 4, 5, 6], 1, 4))), [2, 3, 4]);
        assert_eq!(as_ints(&run(&slice(&[2, 3, 4, 5], -1, 10))), [2, 3, 4, 5]);
        assert_eq!(
            as_ints(&run(&slice(&[2, 3, 4, 5], 2, 2))),
            Vec::<i64>::new()
        );
        assert_eq!(as_ints(&run(&slice(&[], 2, 3))), Vec::<i64>::new());
        assert_eq!(
            run_err(&slice(&[1, 2, 3, 4, 5], 3, 2)),
            "The low bound (3) can't be higher than the high bound (2) in a slice operation"
        );
    });
}

/// `{x | $x == n}` over integers.
fn is_lambda(n: i64) -> ValueSpec {
    expr::lambda(
        vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
        vec![expr::call(EQ, vec![expr::var("x"), expr::int(n)])],
    )
}

#[test]
fn find_returns_the_first_match_or_nothing() {
    on_both_paths(|| {
        assert_eq!(
            run(&expr::call(FIND, vec![ints(&[1, 2, 3, 2]), is_lambda(2)])),
            Value::Integer(2)
        );
        assert_eq!(
            run(&expr::call(FIND, vec![ints(&[1, 3]), is_lambda(2)])),
            Value::Unit
        );
        assert_eq!(
            run(&expr::call(FIND, vec![ints(&[]), is_lambda(2)])),
            Value::Unit
        );
    });
}

#[test]
fn at_indexes_from_zero_and_reports_java_text_past_the_end() {
    on_both_paths(|| {
        let at = |items: ValueSpec, i: i64| expr::call(AT, vec![items, expr::int(i)]);
        assert_eq!(
            run(&at(strings(&["a", "b", "c"]), 1)),
            Value::String("b".into())
        );
        assert_eq!(run(&at(expr::string("a"), 0)), Value::String("a".into()));
        assert_eq!(
            run(&at(
                expr::call(MAP_M, vec![ints(&[2, 3]), square_lambda()]),
                1
            )),
            Value::Integer(9)
        );
        assert_eq!(
            run_err(&at(ints(&[1, 2]), 2)),
            "The system is trying to get an element at offset 2 where the collection is of size 2"
        );
    });
}

#[test]
fn exists_is_true_when_any_element_matches() {
    on_both_paths(|| {
        let exists =
            |items: &[i64], n: i64| run(&expr::call(EXISTS, vec![ints(items), is_lambda(n)]));
        assert_eq!(exists(&[1, 2, 3], 2), Value::Boolean(true));
        assert_eq!(exists(&[1, 3], 2), Value::Boolean(false));
        assert_eq!(exists(&[], 2), Value::Boolean(false));
    });
}

#[test]
fn index_of_finds_the_first_equal_element() {
    on_both_paths(|| {
        let index_of =
            |items: ValueSpec, needle: ValueSpec| run(&expr::call(INDEX_OF, vec![items, needle]));
        assert_eq!(
            index_of(strings(&["a", "b", "c", "b"]), expr::string("b")),
            Value::Integer(1)
        );
        assert_eq!(
            index_of(strings(&["a", "b"]), expr::string("z")),
            Value::Integer(-1)
        );
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        let pairs = eval
            .eval(&expr::call(ZIP, vec![ints(&[1, 2]), strings(&["a", "b"])]))
            .unwrap();
        eval.context_mut().push_scope();
        eval.context_mut().set("pairs", pairs.clone());
        eval.context_mut()
            .set("p", pairs.to_collection()[1].clone());
        assert_eq!(
            eval.eval(&expr::call(
                INDEX_OF,
                vec![expr::var("pairs"), expr::var("p")]
            ))
            .unwrap(),
            Value::Integer(1)
        );
    });
}

#[test]
fn tail_drops_the_first_element() {
    on_both_paths(|| {
        let tail = |v: ValueSpec| as_ints(&run(&expr::call(TAIL, vec![v])));
        assert_eq!(tail(ints(&[1, 2, 3])), [2, 3]);
        assert_eq!(tail(ints(&[1, 2])), [2]);
        assert_eq!(tail(expr::int(1)), Vec::<i64>::new());
        assert_eq!(tail(ints(&[])), Vec::<i64>::new());
    });
}

#[test]
fn to_one_unwraps_a_singleton_and_rejects_other_sizes() {
    on_both_paths(|| {
        assert_eq!(
            run(&expr::call(TO_ONE, vec![ints(&[5])])),
            Value::Integer(5)
        );
        assert_eq!(
            run(&expr::call(
                TO_ONE_WITH_MESSAGE,
                vec![expr::string("x"), expr::string("must be one")]
            )),
            Value::String("x".into())
        );
        assert_eq!(
            run(&expr::call(
                TO_ONE,
                vec![expr::call(FIND, vec![ints(&[1, 2]), is_lambda(2)])]
            )),
            Value::Integer(2)
        );
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        for (items, size) in [(ints(&[]), 0), (ints(&[1, 2]), 2)] {
            let err = eval.eval(&expr::call(TO_ONE, vec![items])).unwrap_err();
            assert!(
                matches!(
                    &err.kind,
                    PureExceptionKind::ExecutionError(PureRuntimeError::MultiplicityViolation {
                        actual,
                        ..
                    }) if *actual == size
                ),
                "{err:?}"
            );
        }
    });
}

#[test]
fn filter_keeps_the_elements_the_predicate_accepts() {
    on_both_paths(|| {
        let filter = |items: &[i64], pred: ValueSpec| {
            as_ints(&run(&expr::call(FILTER, vec![ints(items), pred])))
        };
        assert_eq!(filter(&[1, 2, 3, 2], is_lambda(2)), [2, 2]);
        let not_two = expr::lambda(
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            vec![expr::call(
                NOT,
                vec![expr::call(EQ, vec![expr::var("x"), expr::int(2)])],
            )],
        );
        assert_eq!(filter(&[1, 2, 3, 2], not_two), [1, 3]);
        assert_eq!(filter(&[], is_lambda(2)), Vec::<i64>::new());
    });
}

#[test]
fn concatenate_appends_the_second_collection() {
    on_both_paths(|| {
        let concatenate = |a: ValueSpec, b: ValueSpec| run(&expr::call(CONCATENATE, vec![a, b]));
        assert_eq!(
            as_ints(&concatenate(ints(&[1, 2]), ints(&[3, 4]))),
            [1, 2, 3, 4]
        );
        assert_eq!(as_ints(&concatenate(ints(&[]), expr::int(1))), [1]);
        assert_eq!(
            as_ints(&concatenate(ints(&[]), ints(&[]))),
            Vec::<i64>::new()
        );
        let mixed = concatenate(ints(&[1]), strings(&["a"])).to_collection();
        assert_eq!(mixed[0], Value::Integer(1));
        assert_eq!(mixed[1], Value::String("a".into()));
    });
}

#[test]
fn for_all_is_true_when_every_element_matches_and_on_empty() {
    on_both_paths(|| {
        let for_all =
            |items: &[i64], n: i64| run(&expr::call(FOR_ALL, vec![ints(items), is_lambda(n)]));
        assert_eq!(for_all(&[2, 2], 2), Value::Boolean(true));
        assert_eq!(for_all(&[2, 3], 2), Value::Boolean(false));
        assert_eq!(for_all(&[], 2), Value::Boolean(true));
    });
}

#[test]
fn init_drops_the_last_element() {
    on_both_paths(|| {
        let init = |v: ValueSpec| as_ints(&run(&expr::call(INIT, vec![v])));
        assert_eq!(init(ints(&[1, 2, 3])), [1, 2]);
        assert_eq!(init(ints(&[1, 2])), [1]);
        assert_eq!(init(expr::int(1)), Vec::<i64>::new());
        assert_eq!(init(ints(&[])), Vec::<i64>::new());
    });
}

#[test]
fn last_returns_the_final_element_or_nothing() {
    on_both_paths(|| {
        assert_eq!(
            run(&expr::call(LAST, vec![strings(&["a", "b"])])),
            Value::String("b".into())
        );
        assert_eq!(
            run(&expr::call(LAST, vec![expr::int(7)])),
            Value::Integer(7)
        );
        assert_eq!(run(&expr::call(LAST, vec![ints(&[])])), Value::Unit);
    });
}

/// A `Map<String, Integer>` value holding `entries`, built directly (no
/// native involved).
fn string_int_map(entries: &[(&str, i64)]) -> Value {
    let mut state = MapState::default();
    for (k, v) in entries {
        state
            .entries
            .insert(ValueKey::String((*k).into()), Value::Integer(*v));
    }
    Value::Map(Rc::new(RefCell::new(state)))
}

/// The `(String, Integer)` entries of a map value, sorted by key.
fn string_int_entries(map: &Value) -> Vec<(String, i64)> {
    let Value::Map(cell) = map else {
        panic!("expected a Map, got {map:?}");
    };
    let mut out: Vec<(String, i64)> = cell
        .borrow()
        .entries
        .iter()
        .map(|(k, v)| {
            let ValueKey::String(k) = k else {
                panic!("expected a String key, got {k:?}");
            };
            (k.to_string(), v.as_integer().expect("an Integer value"))
        })
        .collect();
    out.sort();
    out
}

#[test]
fn put_returns_a_new_map_and_leaves_the_input_alone() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        eval.context_mut().push_scope();
        eval.context_mut().set("m", string_int_map(&[("key1", 0)]));
        let put = |key: &str, value: i64| {
            expr::call(
                PUT,
                vec![expr::var("m"), expr::string(key), expr::int(value)],
            )
        };
        let added = eval.eval(&put("key2", 1)).unwrap();
        assert_eq!(
            string_int_entries(&added),
            [("key1".to_string(), 0), ("key2".to_string(), 1)]
        );
        let overridden = eval.eval(&put("key1", 1)).unwrap();
        assert_eq!(string_int_entries(&overridden), [("key1".to_string(), 1)]);
        assert_eq!(
            string_int_entries(eval.context().get("m").unwrap()),
            [("key1".to_string(), 0)]
        );
        let err = eval
            .eval(&expr::call(
                PUT,
                vec![expr::var("m"), ints(&[1, 2]), expr::int(0)],
            ))
            .unwrap_err();
        assert!(
            format!("{err:?}").contains("Map key must be a hashable"),
            "{err:?}"
        );
    });
}

#[test]
fn put_all_merges_pairs_or_a_map_over_the_input() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        eval.context_mut().push_scope();
        eval.context_mut()
            .set("m", string_int_map(&[("key1", 0), ("key2", 0)]));
        let pairs = expr::call(ZIP, vec![strings(&["key2", "key3"]), ints(&[2, 3])]);
        let merged = eval
            .eval(&expr::call(PUT_ALL_PAIRS, vec![expr::var("m"), pairs]))
            .unwrap();
        assert_eq!(
            string_int_entries(&merged),
            [
                ("key1".to_string(), 0),
                ("key2".to_string(), 2),
                ("key3".to_string(), 3)
            ]
        );
        let unchanged = eval
            .eval(&expr::call(PUT_ALL_PAIRS, vec![expr::var("m"), ints(&[])]))
            .unwrap();
        assert_eq!(
            string_int_entries(&unchanged),
            [("key1".to_string(), 0), ("key2".to_string(), 0)]
        );
        eval.context_mut().set("o", string_int_map(&[("key1", 9)]));
        let from_map = eval
            .eval(&expr::call(
                PUT_ALL_MAP,
                vec![expr::var("m"), expr::var("o")],
            ))
            .unwrap();
        assert_eq!(
            string_int_entries(&from_map),
            [("key1".to_string(), 9), ("key2".to_string(), 0)]
        );
    });
}

#[test]
fn is_empty_is_true_only_for_no_elements() {
    on_both_paths(|| {
        let is_empty = |v: ValueSpec| run(&expr::call(IS_EMPTY, vec![v]));
        assert_eq!(is_empty(ints(&[])), Value::Boolean(true));
        assert_eq!(is_empty(expr::int(0)), Value::Boolean(false));
        assert_eq!(is_empty(strings(&["a", "b"])), Value::Boolean(false));
        assert_eq!(
            is_empty(expr::call(FILTER, vec![ints(&[1, 3]), is_lambda(2)])),
            Value::Boolean(true)
        );
    });
}

#[test]
fn add_appends_or_inserts_at_an_offset() {
    on_both_paths(|| {
        let set = || strings(&["a", "b"]);
        let as_strings = |v: &Value| -> Vec<String> {
            v.to_collection()
                .iter()
                .map(|s| s.as_string().expect("a String").to_string())
                .collect()
        };
        assert_eq!(
            as_strings(&run(&expr::call(ADD, vec![set(), expr::string("c")]))),
            ["a", "b", "c"]
        );
        assert_eq!(
            as_strings(&run(&expr::call(
                ADD_AT,
                vec![set(), expr::int(1), expr::string("c")]
            ))),
            ["a", "c", "b"]
        );
        let append = expr::lambda(
            vec![
                param("x", named(INTEGER_ID), Multiplicity::PureOne),
                param("y", named(INTEGER_ID), Multiplicity::ZeroOrMany),
            ],
            vec![expr::call(ADD, vec![expr::var("y"), expr::var("x")])],
        );
        assert_eq!(
            as_ints(&run(&expr::call(
                FOLD,
                vec![ints(&[1, 2, 3, 4]), append, ints(&[-1, 0])]
            ))),
            [-1, 0, 1, 2, 3, 4]
        );
    });
}

#[test]
fn reverse_flips_the_order() {
    on_both_paths(|| {
        let reverse = |v: ValueSpec| run(&expr::call(REVERSE, vec![v]));
        assert_eq!(as_ints(&reverse(ints(&[1, 2, 3]))), [3, 2, 1]);
        assert_eq!(reverse(expr::int(7)), Value::Integer(7));
        assert_eq!(reverse(ints(&[])), Value::Unit);
    });
}

#[test]
fn get_reads_what_put_stored() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        eval.context_mut().push_scope();
        eval.context_mut().set("m", string_int_map(&[("key1", 0)]));
        let put = expr::call(
            PUT,
            vec![expr::var("m"), expr::string("key2"), expr::int(2)],
        );
        assert_eq!(
            eval.eval(&expr::call(GET, vec![put, expr::string("key2")]))
                .unwrap(),
            Value::Integer(2)
        );
        assert_eq!(
            eval.eval(&expr::call(GET, vec![expr::var("m"), expr::string("key1")]))
                .unwrap(),
            Value::Integer(0)
        );
        assert_eq!(
            eval.eval(&expr::call(GET, vec![expr::var("m"), expr::string("key2")]))
                .unwrap(),
            Value::Unit
        );
    });
}

#[test]
fn keys_lists_every_key() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        eval.context_mut().push_scope();
        eval.context_mut()
            .set("m", string_int_map(&[("b", 2), ("a", 1)]));
        let put = expr::call(PUT, vec![expr::var("m"), expr::string("c"), expr::int(3)]);
        let keys = eval.eval(&expr::call(KEYS, vec![put])).unwrap();
        let mut names: Vec<String> = keys
            .to_collection()
            .iter()
            .map(|k| k.as_string().expect("a String key").to_string())
            .collect();
        names.sort();
        assert_eq!(names, ["a", "b", "c"]);
        eval.context_mut().set("e", string_int_map(&[]));
        assert_eq!(
            eval.eval(&expr::call(KEYS, vec![expr::var("e")])).unwrap(),
            Value::Unit
        );
    });
}

#[test]
fn values_lists_every_value() {
    on_both_paths(|| {
        let model = model();
        let registry = NativeRegistry::standard();
        let mut eval = Evaluator::new(&model, &registry);
        eval.context_mut().push_scope();
        eval.context_mut()
            .set("m", string_int_map(&[("b", 2), ("a", 1)]));
        let put = expr::call(PUT, vec![expr::var("m"), expr::string("c"), expr::int(3)]);
        let mut values = as_ints(&eval.eval(&expr::call(VALUES, vec![put])).unwrap());
        values.sort_unstable();
        assert_eq!(values, [1, 2, 3]);
        eval.context_mut().set("e", string_int_map(&[]));
        assert_eq!(
            eval.eval(&expr::call(VALUES, vec![expr::var("e")]))
                .unwrap(),
            Value::Unit
        );
    });
}

#[test]
fn new_map_folds_pairs_and_feeds_the_other_map_natives() {
    on_both_paths(|| {
        let pairs = || expr::call(ZIP, vec![strings(&["key1", "key2"]), ints(&[0, 1])]);
        let built = run(&expr::call(NEW_MAP, vec![pairs()]));
        assert_eq!(
            string_int_entries(&built),
            [("key1".to_string(), 0), ("key2".to_string(), 1)]
        );
        let put = expr::call(
            PUT,
            vec![
                expr::call(NEW_MAP, vec![pairs()]),
                expr::string("key3"),
                expr::int(2),
            ],
        );
        assert_eq!(
            run(&expr::call(GET, vec![put, expr::string("key3")])),
            Value::Integer(2)
        );
        assert_eq!(
            string_int_entries(&run(&expr::call(NEW_MAP, vec![ints(&[])]))),
            Vec::<(String, i64)>::new()
        );
        let repeated = expr::call(ZIP, vec![strings(&["k", "k"]), ints(&[1, 2])]);
        assert_eq!(
            string_int_entries(&run(&expr::call(NEW_MAP, vec![repeated]))),
            [("k".to_string(), 2)]
        );
        assert_eq!(
            string_int_entries(&run(&expr::call(
                NEW_MAP_WITH_KEYS,
                vec![pairs(), expr::collection(Vec::new())]
            ))),
            [("key1".to_string(), 0), ("key2".to_string(), 1)]
        );
    });
}
