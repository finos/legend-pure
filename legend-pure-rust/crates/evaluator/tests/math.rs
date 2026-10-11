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

//! The math, arithmetic and comparison natives,
//! driven through a real `Evaluator` over the bootstrap model: each call
//! is a hand-built node dispatched by its mangled key, the same shape the
//! PCT lambdas reach the evaluator in.

use std::str::FromStr;

use legend_pure_evaluator::builders::{ModelBuilder, expr};
use legend_pure_evaluator::error::{PureException, PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::types::ValueSpec;
use rust_decimal::Decimal;

const ROUND: &str = "round_Number_1__Integer_1_";
const ROUND_DECIMAL: &str = "round_Decimal_1__Integer_1__Decimal_1_";
const ROUND_FLOAT: &str = "round_Float_1__Integer_1__Float_1_";
const SQRT: &str = "sqrt_Number_1__Float_1_";
const ASIN: &str = "asin_Number_1__Float_1_";
const ACOS: &str = "acos_Number_1__Float_1_";
const TO_DECIMAL: &str = "toDecimal_Number_1__Decimal_1_";
const PARSE_INTEGER: &str = "parseInteger_String_1__Integer_1_";
const PARSE_FLOAT: &str = "parseFloat_String_1__Float_1_";
const LESS_THAN: &str = "lessThan_Number_1__Number_1__Boolean_1_";
const COMPARE: &str = "compare_T_1__T_1__Integer_1_";
const REM: &str = "rem_Number_1__Number_1__Number_1_";
const DIVIDE: &str = "divide_Number_1__Number_1__Float_1_";
const DIVIDE_SCALED: &str = "divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_";

/// Evaluate one call node against the bootstrap model with every fished native.
fn run(call: &ValueSpec) -> Value {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.eval(call).unwrap()
}

/// Evaluate one call node that must fail.
fn run_err(call: &ValueSpec) -> PureException {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.eval(call).unwrap_err()
}

/// The text of an `EvaluationError` — what Pure's `assertError` compares
/// against once the failure crosses the boundary.
fn evaluation_error(call: &ValueSpec) -> String {
    match run_err(call).kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => m,
        other => panic!("expected an EvaluationError, got {other:?}"),
    }
}

fn dec(s: &str) -> Decimal {
    Decimal::from_str(s).unwrap()
}

#[test]
fn round_rounds_half_to_even_through_the_evaluator() {
    assert_eq!(
        run(&expr::call(ROUND, vec![expr::float(2.5)])),
        Value::Integer(2)
    );
    assert_eq!(
        run(&expr::call(ROUND, vec![expr::float(16.5)])),
        Value::Integer(16)
    );
    assert_eq!(
        run(&expr::call(ROUND, vec![expr::float(-17.5)])),
        Value::Integer(-18)
    );
    assert_eq!(
        run(&expr::call(ROUND, vec![expr::int(17)])),
        Value::Integer(17)
    );
    assert_eq!(
        run(&expr::call(ROUND, vec![expr::decimal(dec("-3.14159"))])),
        Value::Integer(-3)
    );
    assert_eq!(
        run(&expr::call(
            ROUND_DECIMAL,
            vec![expr::decimal(dec("3.14159")), expr::int(3)]
        )),
        Value::Decimal(dec("3.142"))
    );
    assert_eq!(
        run(&expr::call(
            ROUND_FLOAT,
            vec![expr::float(-17.36), expr::int(1)]
        )),
        Value::Float(-17.4)
    );
}

#[test]
fn less_than_orders_numbers_and_strings_through_the_evaluator() {
    let lt = |a: ValueSpec, b: ValueSpec| run(&expr::call(LESS_THAN, vec![a, b]));
    assert_eq!(lt(expr::int(1), expr::int(2)), Value::Boolean(true));
    assert_eq!(lt(expr::int(4), expr::int(3)), Value::Boolean(false));
    assert_eq!(lt(expr::int(3), expr::int(3)), Value::Boolean(false));
    assert_eq!(lt(expr::int(1), expr::float(2.2)), Value::Boolean(true));
    assert_eq!(
        lt(expr::decimal(dec("0.5")), expr::float(2.5)),
        Value::Boolean(true)
    );
    assert_eq!(
        lt(expr::string("a"), expr::string("b")),
        Value::Boolean(true)
    );
}

#[test]
#[allow(clippy::approx_constant)]
fn rem_keeps_the_dividend_sign_and_reports_java_s_zero_divisor_text() {
    let rem = |a: ValueSpec, b: ValueSpec| run(&expr::call(REM, vec![a, b]));
    assert_eq!(rem(expr::int(7), expr::int(3)), Value::Integer(1));
    assert_eq!(rem(expr::int(-12), expr::int(5)), Value::Integer(-2));
    assert_eq!(rem(expr::float(7.5), expr::int(5)), Value::Float(2.5));
    assert_eq!(
        rem(expr::float(3.14), expr::decimal(dec("1.5"))),
        Value::Decimal(dec("0.14"))
    );
    assert_eq!(
        evaluation_error(&expr::call(REM, vec![expr::int(5), expr::int(0)])),
        "Cannot divide 5 by zero"
    );
}

#[test]
fn divide_returns_float_and_the_scaled_overload_returns_decimal() {
    let div = |a: ValueSpec, b: ValueSpec| run(&expr::call(DIVIDE, vec![a, b]));
    assert_eq!(div(expr::int(3), expr::int(2)), Value::Float(1.5));
    assert_eq!(
        div(expr::int(1), expr::int(96)),
        Value::Float(0.010416666666666666)
    );
    assert_eq!(
        div(expr::decimal(dec("1.1")), expr::decimal(dec("2.1"))),
        Value::Float(0.5238095238095238)
    );
    assert_eq!(
        div(expr::decimal(dec("1.0")), expr::float(2.0)),
        Value::Float(0.5)
    );
    assert_eq!(
        run(&expr::call(
            DIVIDE_SCALED,
            vec![
                expr::decimal(dec("-3.1415")),
                expr::decimal(dec("0.1")),
                expr::int(2)
            ]
        )),
        Value::Decimal(dec("-31.42"))
    );
}

#[test]
fn to_decimal_keeps_the_float_s_shortest_form() {
    assert_eq!(
        run(&expr::call(TO_DECIMAL, vec![expr::float(3.8)])),
        Value::Decimal(dec("3.8"))
    );
    assert_eq!(
        run(&expr::call(TO_DECIMAL, vec![expr::int(8)])),
        Value::Decimal(dec("8"))
    );
}

#[test]
fn compare_is_total_across_types_through_the_evaluator() {
    let cmp = |a: ValueSpec, b: ValueSpec| run(&expr::call(COMPARE, vec![a, b]));
    assert_eq!(cmp(expr::int(171), expr::int(2)), Value::Integer(1));
    assert_eq!(
        cmp(expr::string("aa"), expr::string("aaa")),
        Value::Integer(-1)
    );
    assert_eq!(
        cmp(
            expr::decimal(dec("9999999999999992")),
            expr::int(9_999_999_999_999_992)
        ),
        Value::Integer(0)
    );
    let one_vs_text = cmp(expr::int(1), expr::string("1"));
    let text_vs_one = cmp(expr::string("1"), expr::int(1));
    assert_ne!(one_vs_text, Value::Integer(0));
    match (one_vs_text, text_vs_one) {
        (Value::Integer(a), Value::Integer(b)) => assert_eq!(a, -b),
        other => panic!("expected two Integers, got {other:?}"),
    }
}

#[test]
fn sqrt_of_a_negative_reports_java_s_text() {
    assert_eq!(
        run(&expr::call(SQRT, vec![expr::int(9)])),
        Value::Float(3.0)
    );
    assert_eq!(
        evaluation_error(&expr::call(SQRT, vec![expr::float(-1.0)])),
        "Unable to compute sqrt of -1.0"
    );
}

#[test]
fn acos_and_asin_out_of_domain_report_java_s_text() {
    assert_eq!(
        evaluation_error(&expr::call(ACOS, vec![expr::float(2.0)])),
        "Unable to compute acos of 2.0"
    );
    assert_eq!(
        evaluation_error(&expr::call(ASIN, vec![expr::float(2.0)])),
        "Unable to compute asin of 2.0"
    );
}

#[test]
#[allow(clippy::approx_constant)]
fn parse_natives_accept_java_s_signed_zero_padded_forms() {
    assert_eq!(
        run(&expr::call(PARSE_INTEGER, vec![expr::string("+00000017")])),
        Value::Integer(17)
    );
    assert_eq!(
        run(&expr::call(PARSE_INTEGER, vec![expr::string("-17")])),
        Value::Integer(-17)
    );
    assert_eq!(
        run(&expr::call(
            PARSE_FLOAT,
            vec![expr::string("+0000003.1415900000")]
        )),
        Value::Float(3.14159)
    );
}
