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
//! The Rust half of the apples-to-apples benchmark: the expressions of the
//! Java module's `bench/expressions.pure`, built here with the evaluator's
//! builders and timed with no JNI involved. Each expression is a zero-argument
//! lambda applied once per timed run, the result delivered as a value, which
//! is what the Java half times when it calls the matching function.
//!
//! ```bash
//! cargo run --release -p legend-pure-evaluator --example expressions
//! ```
//!
//! Prints a table and writes `target/benchmarks/expressions-rust.csv`
//! (expression, mode, median, p90 and minimum in nanoseconds per call).
//! Each timed sample covers a batch of calls, because the clock on an
//! Apple Silicon machine ticks every 41.7 ns and a single call would be
//! quantised to that.

use std::time::Instant;

/// Calls per timed sample: the clock ticks every 41.7 ns on Apple
/// Silicon, so one call would be quantised to that.
const BATCH: usize = 32;

use legend_pure_evaluator::builders::{ModelBuilder, expr, named, param};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::types::{DateValue, Multiplicity, ValueSpec};

const PLUS: &str = "plus_Integer_MANY__Integer_1_";
const MINUS: &str = "minus_Integer_MANY__Integer_1_";
const TIMES: &str = "times_Integer_MANY__Integer_1_";
const LET: &str = "letFunction_String_1__T_m__T_m_";
const IF: &str = "if_Boolean_1__Function_1__Function_1__T_m_";
const LESS_THAN: &str = "lessThan_Number_1__Number_1__Boolean_1_";
const MAP: &str = "map_T_MANY__Function_1__V_MANY_";
const FOLD: &str = "fold_T_MANY__Function_1__V_m__V_m_";
const RANGE: &str = "range_Integer_1__Integer_1__Integer_1__Integer_MANY_";
const FILTER: &str = "filter_T_MANY__Function_1__T_MANY_";
const MOD: &str = "mod_Integer_1__Integer_1__Integer_1_";
const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";
const SIZE: &str = "size_Any_MANY__Integer_1_";
const SORT: &str = "sort_T_m__Function_$0_1$__Function_$0_1$__T_m_";
const AT: &str = "at_T_MANY__Integer_1__T_1_";
const FORMAT: &str = "format_String_1__Any_MANY__String_1_";
const ADJUST: &str = "adjust_Date_1__Integer_1__DurationUnit_1__Date_1_";
const MATCH: &str = "match_Any_MANY__Function_$1_MANY$__T_m_";
const REMOVE_DUPLICATES: &str = "removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_";
const JOIN_STRINGS: &str = "joinStrings_String_MANY__String_1__String_1__String_1__String_1_";

fn ints(items: &[i64]) -> ValueSpec {
    expr::collection(items.iter().map(|&i| expr::int(i)).collect())
}

fn strings(items: &[&str]) -> ValueSpec {
    expr::collection(items.iter().map(|s| expr::string(s)).collect())
}

fn int_param(name: &str) -> legend_pure_metamodel::types::Parameter {
    param(name, named(INTEGER_ID), Multiplicity::PureOne)
}

/// `(a op b)` for the arithmetic natives, which take one collection.
fn arith(key: &str, a: ValueSpec, b: ValueSpec) -> ValueSpec {
    expr::call(key, vec![expr::collection(vec![a, b])])
}

/// The expressions, in the order of `expressions.pure`, each as the body of
/// a zero-argument lambda, with the expected result.
fn expressions(duration_unit: ElementId) -> Vec<(&'static str, Vec<ValueSpec>, Value)> {
    vec![
        (
            "e01_plus",
            vec![arith(PLUS, expr::int(1), expr::int(2))],
            Value::Integer(3),
        ),
        (
            "e02_arithmetic",
            vec![arith(
                PLUS,
                arith(TIMES, expr::int(3), expr::int(4)),
                arith(MINUS, expr::int(10), expr::int(2)),
            )],
            Value::Integer(20),
        ),
        (
            "e03_let",
            vec![
                expr::call(LET, vec![expr::string("x"), expr::int(5)]),
                arith(TIMES, expr::var("x"), expr::var("x")),
            ],
            Value::Integer(25),
        ),
        (
            "e04_if",
            vec![expr::call(
                IF,
                vec![
                    expr::call(LESS_THAN, vec![expr::int(1), expr::int(2)]),
                    expr::lambda(Vec::new(), vec![expr::string("yes")]),
                    expr::lambda(Vec::new(), vec![expr::string("no")]),
                ],
            )],
            Value::String("yes".into()),
        ),
        (
            "e05_map_fold",
            vec![expr::call(
                FOLD,
                vec![
                    expr::call(
                        MAP,
                        vec![
                            ints(&[1, 2, 3, 4, 5]),
                            expr::lambda(
                                vec![int_param("x")],
                                vec![arith(TIMES, expr::var("x"), expr::int(2))],
                            ),
                        ],
                    ),
                    expr::lambda(
                        vec![int_param("a"), int_param("b")],
                        vec![arith(PLUS, expr::var("a"), expr::var("b"))],
                    ),
                    expr::int(0),
                ],
            )],
            Value::Integer(30),
        ),
        (
            "e06_range_filter",
            vec![expr::call(
                SIZE,
                vec![expr::call(
                    FILTER,
                    vec![
                        expr::call(RANGE, vec![expr::int(0), expr::int(100), expr::int(1)]),
                        expr::lambda(
                            vec![int_param("x")],
                            vec![expr::call(
                                EQUAL,
                                vec![
                                    expr::call(MOD, vec![expr::var("x"), expr::int(2)]),
                                    expr::int(0),
                                ],
                            )],
                        ),
                    ],
                )],
            )],
            Value::Integer(50),
        ),
        (
            "e07_sort_at",
            vec![expr::call(
                AT,
                vec![
                    expr::call(
                        SORT,
                        vec![
                            ints(&[5, 3, 9, 1, 7]),
                            expr::collection(Vec::new()),
                            expr::collection(Vec::new()),
                        ],
                    ),
                    expr::int(0),
                ],
            )],
            Value::Integer(1),
        ),
        (
            "e08_format",
            vec![expr::call(
                FORMAT,
                vec![
                    expr::string("hello %s, %s"),
                    expr::collection(vec![expr::string("world"), expr::int(42)]),
                ],
            )],
            Value::String("hello world, 42".into()),
        ),
        (
            "e09_date",
            vec![expr::call(
                ADJUST,
                vec![
                    expr::date(DateValue::StrictDate {
                        year: 2020,
                        month: Some(1),
                        day: Some(31),
                    }),
                    expr::int(3),
                    expr::enum_value(duration_unit, "MONTHS"),
                ],
            )],
            Value::Date(
                legend_pure_evaluator::date::PureDate::strict_date(2020, 4, 30)
                    .expect("a valid date"),
            ),
        ),
        (
            "e10_match",
            vec![expr::call(
                MATCH,
                vec![
                    expr::int(5),
                    expr::collection(vec![
                        expr::lambda(
                            vec![int_param("i")],
                            vec![arith(PLUS, expr::var("i"), expr::int(1))],
                        ),
                        expr::lambda(
                            vec![param("s", named(STRING_ID), Multiplicity::PureOne)],
                            vec![expr::int(0)],
                        ),
                    ]),
                ],
            )],
            Value::Integer(6),
        ),
        (
            "e11_remove_duplicates",
            vec![expr::call(
                SIZE,
                vec![expr::call(
                    REMOVE_DUPLICATES,
                    vec![
                        strings(&["a", "b", "a", "c", "b"]),
                        expr::collection(Vec::new()),
                        expr::collection(Vec::new()),
                    ],
                )],
            )],
            Value::Integer(3),
        ),
        (
            "e12_join_strings",
            vec![expr::call(
                JOIN_STRINGS,
                vec![
                    strings(&["a", "b", "c"]),
                    expr::string("<"),
                    expr::string(", "),
                    expr::string(">"),
                ],
            )],
            Value::String("<a, b, c>".into()),
        ),
    ]
}

fn main() {
    let warmup: usize = std::env::var("BENCH_WARMUP")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(200);
    let runs: usize = std::env::var("BENCH_RUNS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(2000);

    let mut builder = ModelBuilder::new();
    let duration_unit = builder.enumeration(
        "meta::pure::functions::date::DurationUnit",
        &[
            "YEARS",
            "MONTHS",
            "WEEKS",
            "DAYS",
            "HOURS",
            "MINUTES",
            "SECONDS",
            "MILLISECONDS",
            "MICROSECONDS",
            "NANOSECONDS",
        ],
    );
    let model = builder.finish();
    let registry = NativeRegistry::standard();
    let mut evaluator = Evaluator::new(&model, &registry);

    let mut csv = String::from("expression,mode,median_ns,p90_ns,min_ns\n");
    println!(
        "{:<24} {:>12} {:>12} {:>12}",
        "expression", "median ns", "p90 ns", "min ns"
    );
    for (name, body, expected) in expressions(duration_unit) {
        let callable = evaluator
            .eval(&expr::lambda(Vec::new(), body))
            .unwrap_or_else(|e| panic!("{name}: building the lambda failed: {e}"));
        let run = |evaluator: &mut Evaluator<'_>| {
            evaluator.context_mut().push_scope();
            let result = evaluator.apply_callable(&callable, &[]);
            evaluator.context_mut().pop_scope();
            result.unwrap_or_else(|e| panic!("{name}: evaluation failed: {e}"))
        };
        let first = run(&mut evaluator);
        assert_eq!(first, expected, "{name}: unexpected result");
        for _ in 0..warmup {
            run(&mut evaluator);
        }
        let mut times: Vec<u128> = Vec::with_capacity(runs);
        for _ in 0..runs {
            let start = Instant::now();
            for _ in 0..BATCH {
                let value = run(&mut evaluator);
                std::hint::black_box(value);
            }
            times.push(start.elapsed().as_nanos() / BATCH as u128);
        }
        times.sort_unstable();
        let median = times[times.len() / 2];
        let p90 = times[times.len() * 9 / 10];
        let min = times[0];
        println!("{name:<24} {median:>12} {p90:>12} {min:>12}");
        csv.push_str(&format!("{name},rust-native,{median},{p90},{min}\n"));
    }
    let dir = std::path::Path::new("target/benchmarks");
    std::fs::create_dir_all(dir).expect("target/benchmarks");
    std::fs::write(dir.join("expressions-rust.csv"), csv).expect("write the csv");
}
