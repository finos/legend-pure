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

//! The evaluator's hot path in isolation, the measurement behind plan
//! Step 2b: a literal, one native over a two-element collection, a range,
//! lambda applications with bodies of growing cost, and the raw cost of a
//! `Value` clone, a two-element `PVector`, and a scope with one binding.
//! No JNI, no bridge.
//!
//! ```bash
//! cargo run --release -p legend-pure-evaluator --example hot_path
//! ```

use std::time::Instant;

use im_rc::Vector as PVector;
use legend_pure_evaluator::builders::{ModelBuilder, expr, named, param};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::INTEGER_ID;
use legend_pure_metamodel::types::{Multiplicity, Parameter, ValueSpec};

const PLUS: &str = "plus_Integer_MANY__Integer_1_";
const TIMES: &str = "times_Integer_MANY__Integer_1_";
const RANGE: &str = "range_Integer_1__Integer_1__Integer_1__Integer_MANY_";
const FILTER: &str = "filter_T_MANY__Function_1__T_MANY_";
const MAP: &str = "map_T_MANY__Function_1__V_MANY_";
const MOD: &str = "mod_Integer_1__Integer_1__Integer_1_";
const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";
const SIZE: &str = "size_Any_MANY__Integer_1_";

const WARMUP: usize = 300;
const RUNS: usize = 3000;

fn int_param(name: &str) -> Parameter {
    param(name, named(INTEGER_ID), Multiplicity::PureOne)
}

fn range_100() -> ValueSpec {
    expr::call(RANGE, vec![expr::int(0), expr::int(100), expr::int(1)])
}

fn size_of(spec: ValueSpec) -> ValueSpec {
    expr::call(SIZE, vec![spec])
}

fn arith(key: &str, a: ValueSpec, b: ValueSpec) -> ValueSpec {
    expr::call(key, vec![expr::collection(vec![a, b])])
}

/// Median nanoseconds of applying `spec` as a zero-argument lambda.
fn time_spec(name: &str, evaluator: &mut Evaluator<'_>, spec: ValueSpec) {
    let callable = evaluator
        .eval(&expr::lambda(Vec::new(), vec![spec]))
        .unwrap_or_else(|e| panic!("{name}: {e}"));
    let run = |evaluator: &mut Evaluator<'_>| {
        evaluator.context_mut().push_scope();
        let result = evaluator.apply_callable(&callable, &[]);
        evaluator.context_mut().pop_scope();
        result.unwrap_or_else(|e| panic!("{name}: {e}"))
    };
    for _ in 0..WARMUP {
        run(evaluator);
    }
    let mut times: Vec<u128> = Vec::with_capacity(RUNS);
    for _ in 0..RUNS {
        let start = Instant::now();
        let value = run(evaluator);
        times.push(start.elapsed().as_nanos());
        std::hint::black_box(value);
    }
    times.sort_unstable();
    println!("{name:<48} {:>9} ns", times[times.len() / 2]);
}

/// Average nanoseconds of one iteration of `body` over `iterations`.
fn time_raw(name: &str, iterations: u128, mut body: impl FnMut()) {
    let start = Instant::now();
    for _ in 0..iterations {
        body();
    }
    println!(
        "{name:<48} {:>9} ns",
        start.elapsed().as_nanos() / iterations
    );
}

fn main() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut evaluator = Evaluator::new(&model, &registry);

    time_spec("literal 1", &mut evaluator, expr::int(1));
    time_spec(
        "1 + 2",
        &mut evaluator,
        arith(PLUS, expr::int(1), expr::int(2)),
    );
    time_spec(
        "range(0,100,1)->size()",
        &mut evaluator,
        size_of(range_100()),
    );
    time_spec(
        "range->filter(x | true)->size()",
        &mut evaluator,
        size_of(expr::call(
            FILTER,
            vec![
                range_100(),
                expr::lambda(vec![int_param("x")], vec![expr::boolean(true)]),
            ],
        )),
    );
    time_spec(
        "range->filter(x | $x->mod(2) == 0)->size()",
        &mut evaluator,
        size_of(expr::call(
            FILTER,
            vec![
                range_100(),
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
        )),
    );
    time_spec(
        "range->map(x | $x)->size()",
        &mut evaluator,
        size_of(expr::call(
            MAP,
            vec![
                range_100(),
                expr::lambda(vec![int_param("x")], vec![expr::var("x")]),
            ],
        )),
    );
    time_spec(
        "range->map(x | $x * 2)->size()",
        &mut evaluator,
        size_of(expr::call(
            MAP,
            vec![
                range_100(),
                expr::lambda(
                    vec![int_param("x")],
                    vec![arith(TIMES, expr::var("x"), expr::int(2))],
                ),
            ],
        )),
    );
    time_spec(
        "[100 literals]->size()",
        &mut evaluator,
        size_of(expr::collection((0..100).map(expr::int).collect())),
    );

    let integer = Value::Integer(1);
    time_raw("Value::Integer clone", 1_000_000, || {
        std::hint::black_box(integer.clone());
    });
    let hundred = Value::Collection(Box::new((0..100).map(Value::Integer).collect()));
    time_raw("Value::Collection(100) clone", 10_000, || {
        std::hint::black_box(hundred.clone());
    });
    time_raw("PVector of 2 from an iterator", 100_000, || {
        let two: PVector<Value> = (0..2).map(Value::Integer).collect();
        std::hint::black_box(two);
    });
    time_raw("push_scope + set + pop_scope", 100_000, || {
        evaluator.context_mut().push_scope();
        evaluator.context_mut().set("x", Value::Integer(1));
        evaluator.context_mut().pop_scope();
    });
}
