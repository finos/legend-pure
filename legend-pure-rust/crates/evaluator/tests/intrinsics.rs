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

//! Every intrinsic against its registered native, over generated inputs.
//!
//! Each case is a function whose body calls the native on its parameters,
//! every expression typed as a compiler types it. The body executor runs
//! the call as an intrinsic; the tree walker runs the native. Both run
//! the function on every input, and the outcomes must be the same: the
//! value, or the error with its kind, message, source and frames. The
//! inputs include values that do not hold the representation the static
//! type promised, which the intrinsic hands to the native.

use legend_pure_evaluator::builders::{ModelBuilder, expr, function, named, param};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{ANY_ID, BOOLEAN_ID, FLOAT_ID, INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::Multiplicity;
use rust_decimal::Decimal;

const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";
const EQ: &str = "eq_Any_1__Any_1__Boolean_1_";
const LESS_THAN: &str = "lessThan_Number_1__Number_1__Boolean_1_";
const LESS_THAN_EQUAL: &str = "lessThanEqual_Number_1__Number_1__Boolean_1_";
const NOT: &str = "not_Boolean_1__Boolean_1_";
const SIZE: &str = "size_Any_MANY__Integer_1_";
const IS_EMPTY: &str = "isEmpty_Any_MANY__Boolean_1_";

/// `splitmix64`: a fixed sequence, so a failure reproduces.
struct Rng(u64);

impl Rng {
    fn next(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }
}

#[allow(clippy::cast_possible_wrap)]
fn integers(rng: &mut Rng) -> Vec<Value> {
    let mut values: Vec<i64> = vec![
        0,
        1,
        -1,
        2,
        -2,
        50,
        i64::MIN,
        i64::MIN + 1,
        i64::MAX,
        i64::MAX - 1,
        1 << 53,
        (1 << 53) + 1,
    ];
    for _ in 0..12 {
        values.push(rng.next() as i64);
    }
    for _ in 0..12 {
        values.push((rng.next() % 7) as i64 - 3);
    }
    values.into_iter().map(Value::Integer).collect()
}

#[allow(clippy::cast_precision_loss)]
fn floats(rng: &mut Rng) -> Vec<Value> {
    let mut values: Vec<f64> = vec![
        0.0,
        -0.0,
        1.0,
        -1.0,
        0.5,
        2.0,
        50.0,
        f64::NAN,
        -f64::NAN,
        f64::INFINITY,
        f64::NEG_INFINITY,
        f64::MIN_POSITIVE,
        f64::MAX,
        f64::MIN,
        f64::EPSILON,
        9_007_199_254_740_993.0,
    ];
    for _ in 0..10 {
        values.push(f64::from_bits(rng.next()));
    }
    for _ in 0..10 {
        values.push((rng.next() % 9) as f64 / 2.0 - 2.0);
    }
    values.into_iter().map(Value::Float).collect()
}

fn booleans() -> Vec<Value> {
    vec![Value::Boolean(true), Value::Boolean(false)]
}

fn strings() -> Vec<Value> {
    [
        "",
        "a",
        "b",
        "ab",
        "aa",
        "é",
        "日本",
        "abcdefghijklmnopqrstuvw",
        "abcdefghijklmnopqrstuvwx",
        "abcdefghijklmnopqrstuvwy",
        "a longer string that lives on the heap, past the inline limit",
    ]
    .into_iter()
    .map(|s| Value::String(s.into()))
    .collect()
}

/// Values that are not what an `Integer[1]`, `Float[1]`, `Boolean[1]` or
/// `String[1]` static type promises.
fn misfits() -> Vec<Value> {
    vec![
        Value::Unit,
        Value::from_vec(vec![Value::Integer(1)]),
        Value::Collection(Box::new([Value::Integer(1)].into_iter().collect())),
        Value::from_vec(vec![Value::Integer(1), Value::Integer(2)]),
        Value::Decimal(Decimal::new(15, 1)),
        Value::Integer(1),
        Value::Float(1.0),
        Value::Boolean(true),
        Value::String("1".into()),
    ]
}

fn one(element: ElementId) -> (ElementId, Multiplicity) {
    (element, Multiplicity::PureOne)
}

/// A model with `test::f`, whose parameters have the given types and
/// whose body calls `key` on them, each argument and the call typed.
fn model(
    key: &str,
    parameters: &[(ElementId, Multiplicity)],
    result: (ElementId, Multiplicity),
) -> (PureModel, ElementId) {
    let names = ["a", "b"];
    let arguments = parameters
        .iter()
        .zip(names)
        .map(|((element, multiplicity), name)| {
            expr::typed(expr::var(name), named(*element), multiplicity.clone())
        })
        .collect();
    let call = expr::typed(expr::call(key, arguments), named(result.0), result.1);
    let mut builder = ModelBuilder::new();
    let f = builder.function(
        "test::f",
        function(
            "f",
            parameters
                .iter()
                .zip(names)
                .map(|((element, multiplicity), name)| {
                    param(name, named(*element), multiplicity.clone())
                })
                .collect(),
            named(ANY_ID),
            Multiplicity::ZeroOrMany,
            vec![call],
        ),
    );
    (builder.finish(), f)
}

/// The tree walker and the body executor side by side.
struct Paths<'m> {
    walker: Evaluator<'m>,
    exec: Evaluator<'m>,
    f: ElementId,
    compared: usize,
}

impl<'m> Paths<'m> {
    fn new(model: &'m PureModel, registry: &'m NativeRegistry, f: ElementId) -> Self {
        let mut walker = Evaluator::new(model, registry);
        walker.set_exec(false);
        walker.context_mut().push_scope();
        let mut exec = Evaluator::new(model, registry);
        exec.set_exec(true);
        exec.context_mut().push_scope();
        Self {
            walker,
            exec,
            f,
            compared: 0,
        }
    }

    /// Run `f` on both paths and require the same outcome.
    fn agree(&mut self, args: &[Value]) {
        let outcome = |ev: &mut Evaluator<'m>, f| {
            ev.call_user_function_by_id_with_args(f, args)
                .map_err(|e| format!("{:?}", e.into_data()))
        };
        let walked = outcome(&mut self.walker, self.f);
        let ran = outcome(&mut self.exec, self.f);
        assert_eq!(
            format!("{walked:?}"),
            format!("{ran:?}"),
            "the native and the intrinsic disagree on {args:?}"
        );
        self.compared += 1;
    }

    /// The function's body was built with one intrinsic.
    fn ran_an_intrinsic(&self) {
        let body = self
            .exec
            .built_function_body(self.f)
            .expect("the body was built");
        assert_eq!(body.intrinsics(), 1);
        assert_eq!(body.fallbacks(), 0);
    }
}

/// Every pair of `left` and `right` through a two-argument native whose
/// parameters are typed `types`.
fn pairs(key: &str, types: [ElementId; 2], left: &[Value], right: &[Value]) -> usize {
    let (model, f) = model(key, &[one(types[0]), one(types[1])], one(BOOLEAN_ID));
    let registry = NativeRegistry::standard();
    let mut paths = Paths::new(&model, &registry, f);
    for a in left {
        for b in right {
            paths.agree(&[a.clone(), b.clone()]);
        }
    }
    paths.ran_an_intrinsic();
    paths.compared
}

fn with_misfits(values: Vec<Value>) -> Vec<Value> {
    values.into_iter().chain(misfits()).collect()
}

#[test]
fn equal_and_eq_on_integers() {
    let ints = with_misfits(integers(&mut Rng(1)));
    for key in [EQUAL, EQ] {
        assert!(pairs(key, [INTEGER_ID, INTEGER_ID], &ints, &ints) > 1000);
    }
}

#[test]
fn equal_and_eq_on_floats() {
    let floats = with_misfits(floats(&mut Rng(2)));
    for key in [EQUAL, EQ] {
        assert!(pairs(key, [FLOAT_ID, FLOAT_ID], &floats, &floats) > 1000);
    }
}

#[test]
fn equal_and_eq_on_booleans_and_strings() {
    let bools = with_misfits(booleans());
    let strings = with_misfits(strings());
    for key in [EQUAL, EQ] {
        pairs(key, [BOOLEAN_ID, BOOLEAN_ID], &bools, &bools);
        pairs(key, [STRING_ID, STRING_ID], &strings, &strings);
    }
}

#[test]
fn less_than_and_less_than_equal_on_integers_and_floats() {
    let ints = with_misfits(integers(&mut Rng(3)));
    let floats = with_misfits(floats(&mut Rng(4)));
    for key in [LESS_THAN, LESS_THAN_EQUAL] {
        pairs(key, [INTEGER_ID, INTEGER_ID], &ints, &ints);
        pairs(key, [FLOAT_ID, FLOAT_ID], &floats, &floats);
        pairs(key, [INTEGER_ID, FLOAT_ID], &ints, &floats);
        pairs(key, [FLOAT_ID, INTEGER_ID], &floats, &ints);
    }
}

#[test]
fn not_on_booleans() {
    let (model, f) = model(NOT, &[one(BOOLEAN_ID)], one(BOOLEAN_ID));
    let registry = NativeRegistry::standard();
    let mut paths = Paths::new(&model, &registry, f);
    for value in with_misfits(booleans()) {
        paths.agree(&[value]);
    }
    paths.ran_an_intrinsic();
}

/// Values of every shape `size` and `isEmpty` count.
fn shapes(rng: &mut Rng) -> Vec<Value> {
    let mut values = misfits();
    values.extend(integers(rng).into_iter().take(3));
    values.extend(strings().into_iter().take(3));
    for n in 0..6 {
        values.push(Value::Collection(Box::new(
            (0..n).map(Value::Integer).collect(),
        )));
    }
    values.push(Value::from_vec(vec![
        Value::Integer(1),
        Value::String("a".into()),
        Value::Unit,
    ]));
    values
}

#[test]
fn size_and_is_empty_on_any_value() {
    let registry = NativeRegistry::standard();
    for (key, result) in [(SIZE, INTEGER_ID), (IS_EMPTY, BOOLEAN_ID)] {
        for declared in [
            (ANY_ID, Multiplicity::ZeroOrMany),
            (INTEGER_ID, Multiplicity::PureOne),
            (STRING_ID, Multiplicity::ZeroOrOne),
        ] {
            let (model, f) = model(key, &[declared], one(result));
            let mut paths = Paths::new(&model, &registry, f);
            for value in shapes(&mut Rng(5)) {
                paths.agree(&[value]);
            }
            paths.ran_an_intrinsic();
        }
    }
}
