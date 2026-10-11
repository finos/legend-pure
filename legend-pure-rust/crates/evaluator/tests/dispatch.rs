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

//! Dispatch paths over a hand-built model: native by exact mangled name,
//! user function body by resolved id, prefix fallback, lambda
//! application, and the error paths around them.

use legend_pure_evaluator::builders::{
    ModelBuilder, expr, function, named, native_function, param,
};
use legend_pure_evaluator::error::{PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{INTEGER_ID, NUMBER_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::Element;
use legend_pure_metamodel::types::Multiplicity;

/// `double(x: Integer[1]): Integer[1] { times([$x, 2]) }` added through
/// `ModelBuilder::function`, so its node carries the mangled name.
fn model_with_double() -> (legend_pure_metamodel::model::PureModel, ElementId) {
    let mut b = ModelBuilder::new();
    let id = b.function(
        "my::double",
        function(
            "double",
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            named(INTEGER_ID),
            Multiplicity::PureOne,
            vec![expr::call(
                "times_Number_MANY__Number_1_",
                vec![expr::collection(vec![expr::var("x"), expr::int(2)])],
            )],
        ),
    );
    (b.finish(), id)
}

fn function_not_found(err: &legend_pure_evaluator::error::PureException) -> Option<&str> {
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::FunctionNotFound(name)) => {
            Some(name.as_str())
        }
        _ => None,
    }
}

#[test]
fn function_node_name_is_the_mangled_signature() {
    let (model, id) = model_with_double();
    assert_eq!(model.get_node(id).name, "double_Integer_1__Integer_1_");
    assert!(matches!(model.get_element(id), Element::Function(f) if f.function_name == "double"));
}

#[test]
fn user_function_body_dispatches_by_resolved_id() {
    let (model, id) = model_with_double();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let call = expr::call_resolved(id, "double", vec![expr::int(21)]);
    assert_eq!(eval.eval(&call).unwrap(), Value::Integer(42));
}

#[test]
fn user_function_is_found_by_path_and_simple_name() {
    let (model, id) = model_with_double();
    let path: Vec<smol_str::SmolStr> = ["my", "double"].map(smol_str::SmolStr::new).into();
    assert_eq!(model.resolve_function_by_path(&path), Some(id));
    assert_eq!(
        model.resolve_fqn_str("my::double_Integer_1__Integer_1_"),
        Some(id)
    );
}

#[test]
fn function_added_with_a_plain_name_never_dispatches() {
    // Bypass `ModelBuilder::function` on purpose: the node keeps the
    // simple name, which is exactly the hidden contract this guards.
    let mut b = ModelBuilder::new();
    let id = b.add(
        "my::double",
        Element::Function(function(
            "double",
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            named(INTEGER_ID),
            Multiplicity::PureOne,
            vec![expr::var("x")],
        )),
    );
    let model = b.finish();
    assert_eq!(model.get_node(id).name, "double");

    // A resolved call still runs the body: the id is enough for a user
    // function. The contract bites natives and name-based lookups.
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let resolved = expr::call_resolved(id, "double", vec![expr::int(1)]);
    assert_eq!(eval.eval(&resolved).unwrap(), Value::Integer(1));

    // A name-based call reports the mangled key it looked for.
    let by_name = expr::call("double_Integer_1__Integer_1_", vec![expr::int(1)]);
    let err = eval.eval(&by_name).unwrap_err();
    assert_eq!(
        function_not_found(&err),
        Some("double_Integer_1__Integer_1_")
    );
    assert!(
        err.to_string().contains("double_Integer_1__Integer_1_"),
        "{err}"
    );
}

#[test]
fn native_declared_in_the_model_dispatches_to_the_registry() {
    let mut b = ModelBuilder::new();
    let id = b.function(
        "meta::pure::functions::math::plus",
        native_function(
            "plus",
            vec![param("numbers", named(NUMBER_ID), Multiplicity::ZeroOrMany)],
            named(NUMBER_ID),
            Multiplicity::PureOne,
        ),
    );
    let model = b.finish();
    assert_eq!(model.get_node(id).name, "plus_Number_MANY__Number_1_");

    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let call = expr::call_resolved(
        id,
        "plus",
        vec![expr::collection(vec![expr::int(2), expr::int(3)])],
    );
    assert_eq!(eval.eval(&call).unwrap(), Value::Integer(5));
}

#[test]
fn native_declared_but_not_fished_is_function_not_found() {
    let mut b = ModelBuilder::new();
    let id = b.function(
        "meta::pure::functions::math::divide",
        native_function(
            "divide",
            vec![
                param("a", named(NUMBER_ID), Multiplicity::PureOne),
                param("b", named(NUMBER_ID), Multiplicity::PureOne),
            ],
            named(NUMBER_ID),
            Multiplicity::PureOne,
        ),
    );
    let model = b.finish();
    assert_eq!(
        model.get_node(id).name,
        "divide_Number_1__Number_1__Number_1_"
    );
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let call = expr::call_resolved(id, "divide", vec![expr::int(2), expr::int(1)]);
    let err = eval.eval(&call).unwrap_err();
    assert_eq!(
        function_not_found(&err),
        Some("divide_Number_1__Number_1__Number_1_")
    );
}

#[test]
fn lambda_value_applies_to_arguments() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let lambda = expr::lambda(
        vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
        vec![expr::call(
            "times_Number_MANY__Number_1_",
            vec![expr::collection(vec![expr::var("x"), expr::int(3)])],
        )],
    );
    let callable = eval.eval(&lambda).unwrap();
    assert!(matches!(callable, Value::Function(_)));
    assert_eq!(
        eval.apply_callable(&callable, &[Value::Integer(4)])
            .unwrap(),
        Value::Integer(12)
    );
}

#[test]
fn lambda_captures_the_enclosing_variable() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.context_mut().set("k", Value::Integer(10));
    let lambda = expr::lambda(
        vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
        vec![expr::call(
            "plus_Number_MANY__Number_1_",
            vec![expr::collection(vec![expr::var("x"), expr::var("k")])],
        )],
    );
    let callable = eval.eval(&lambda).unwrap();
    eval.context_mut().set("k", Value::Integer(-1));
    assert_eq!(
        eval.apply_callable(&callable, &[Value::Integer(1)])
            .unwrap(),
        Value::Integer(11)
    );
}

#[test]
fn variable_reads_the_context_and_reports_misses() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.context_mut().set("x", Value::Integer(7));
    assert_eq!(eval.eval(&expr::var("x")).unwrap(), Value::Integer(7));
    let err = eval.eval(&expr::var("nope")).unwrap_err();
    assert!(matches!(
        err.kind,
        PureExceptionKind::ExecutionError(PureRuntimeError::VariableNotFound(ref n)) if n == "nope"
    ));
}
