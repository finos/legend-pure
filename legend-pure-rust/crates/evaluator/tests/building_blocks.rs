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

//! The evaluator over the bootstrap alone, driven by hand-built nodes.

use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{ExprKind, FunctionCallData, ValueSpec};

use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::{FunctionValue, Value};
use legend_pure_metamodel::model::PureModel;

fn test_model() -> PureModel {
    PureModel::bootstrapped()
}

fn synth_src() -> SourceInfo {
    SourceInfo::new("<test>", 1, 1, 1, 10)
}

fn make_expr(kind: ExprKind) -> ValueSpec {
    ValueSpec {
        kind: Box::new(kind),
        source_info: synth_src(),
        type_info: None,
    }
}

#[test]
fn eval_integer_literal() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::IntegerLiteral(42));
    assert_eq!(eval.eval(&expr).unwrap(), Value::Integer(42));
}

#[test]
fn eval_float_literal() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::FloatLiteral(std::f64::consts::PI));
    assert_eq!(
        eval.eval(&expr).unwrap(),
        Value::Float(std::f64::consts::PI)
    );
}

#[test]
fn eval_string_literal() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::StringLiteral("hello".into()));
    assert_eq!(eval.eval(&expr).unwrap(), Value::String("hello".into()));
}

#[test]
fn eval_boolean_literal() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::BooleanLiteral(true));
    assert_eq!(eval.eval(&expr).unwrap(), Value::Boolean(true));
}

#[test]
fn eval_variable_reference() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.context_mut().set("x", Value::Integer(99));
    let expr = make_expr(ExprKind::Variable { name: "x".into() });
    assert_eq!(eval.eval(&expr).unwrap(), Value::Integer(99));
    eval.context_mut().pop_scope();
}

#[test]
fn eval_variable_not_found() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::Variable {
        name: "nonexistent".into(),
    });
    assert!(eval.eval(&expr).is_err());
}

#[test]
fn eval_native_function_call() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::FunctionCall(FunctionCallData {
        function: None,
        function_name: "plus_Number_MANY__Number_1_".into(),
        arguments: vec![make_expr(ExprKind::Collection {
            elements: vec![
                make_expr(ExprKind::IntegerLiteral(2)),
                make_expr(ExprKind::IntegerLiteral(3)),
            ],
        })],
    }));
    assert_eq!(eval.eval(&expr).unwrap(), Value::Integer(5));
}

#[test]
fn eval_nested_function_calls() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::FunctionCall(FunctionCallData {
        function: None,
        function_name: "plus_Number_MANY__Number_1_".into(),
        arguments: vec![make_expr(ExprKind::Collection {
            elements: vec![
                make_expr(ExprKind::IntegerLiteral(2)),
                make_expr(ExprKind::FunctionCall(FunctionCallData {
                    function: None,
                    function_name: "times_Number_MANY__Number_1_".into(),
                    arguments: vec![make_expr(ExprKind::Collection {
                        elements: vec![
                            make_expr(ExprKind::IntegerLiteral(3)),
                            make_expr(ExprKind::IntegerLiteral(4)),
                        ],
                    })],
                })),
            ],
        })],
    }));
    assert_eq!(eval.eval(&expr).unwrap(), Value::Integer(14));
}

#[test]
fn eval_collection_literal() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::Collection {
        elements: vec![
            make_expr(ExprKind::IntegerLiteral(1)),
            make_expr(ExprKind::IntegerLiteral(2)),
            make_expr(ExprKind::IntegerLiteral(3)),
        ],
    });
    let evaluated = eval.eval(&expr).unwrap();
    match &evaluated {
        Value::Collection(v) => {
            assert_eq!(v.len(), 3);
            assert_eq!(v[0], Value::Integer(1));
            assert_eq!(v[1], Value::Integer(2));
            assert_eq!(v[2], Value::Integer(3));
        }
        other => panic!("Expected Collection, got {other:?}"),
    }
}

#[test]
fn eval_empty_collection() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::Collection { elements: vec![] });
    assert_eq!(eval.eval(&expr).unwrap(), Value::Unit);
}

#[test]
fn eval_lambda_creation() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::Lambda {
        parameters: Vec::new().into(),
        body: vec![make_expr(ExprKind::IntegerLiteral(42))].into(),
        free: Vec::new().into(),
    });
    let evaluated = eval.eval(&expr).unwrap();
    match &evaluated {
        Value::Function(fv) => match fv.as_ref() {
            FunctionValue::Lambda(lc) => {
                assert_eq!(lc.parameters.len(), 0);
                assert_eq!(lc.body.len(), 1);
            }
            other @ (FunctionValue::Compiled(_) | FunctionValue::Path(_)) => {
                panic!("Expected FunctionValue::Lambda, got {other:?}")
            }
        },
        other => panic!("Expected Function, got {other:?}"),
    }
}

#[test]
fn eval_function_not_found() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let expr = make_expr(ExprKind::FunctionCall(FunctionCallData {
        function: None,
        function_name: "nonexistent".into(),
        arguments: vec![],
    }));
    assert!(eval.eval(&expr).is_err());
}

#[test]
fn eval_body_returns_last() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let body = vec![
        make_expr(ExprKind::IntegerLiteral(1)),
        make_expr(ExprKind::IntegerLiteral(2)),
        make_expr(ExprKind::IntegerLiteral(3)),
    ];
    assert_eq!(eval.eval_body(&body).unwrap(), Value::Integer(3));
}

#[test]
fn eval_empty_body() {
    let model = test_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(eval.eval_body(&[]).unwrap(), Value::Unit);
}
