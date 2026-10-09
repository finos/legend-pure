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

//! The language natives fished in Step 1 milestone 3, driven through
//! hand-built nodes: `letFunction`, `if`, `new`, `copy`, `eq`/`equal`,
//! `and`/`or`/`not`, `minus`, plus property and qualified-property access
//! on the instances `new` builds.

use legend_pure_evaluator::builders::{
    ModelBuilder, class, expr, named, param, property, qualified_property,
};
use legend_pure_evaluator::error::{PureException, PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{Multiplicity, ValueSpec};

const NEW: &str = "new_Class_1__String_1__Any_MANY__T_1_";
const COPY: &str = "copy_T_1__String_1__KeyExpression_MANY__T_1_";
const LET: &str = "letFunction_String_1__T_m__T_m_";
const IF: &str = "if_Boolean_1__Function_1__Function_1__T_m_";
const PLUS: &str = "plus_Number_MANY__Number_1_";
const MINUS: &str = "minus_Number_MANY__Number_1_";
const EQ: &str = "eq_Any_1__Any_1__Boolean_1_";
const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";
const AND: &str = "and_Boolean_1__Boolean_1__Boolean_1_";
const OR: &str = "or_Boolean_1__Boolean_1__Boolean_1_";
const NOT: &str = "not_Boolean_1__Boolean_1_";

/// `my::Person { name: String[1]; age: Integer[0..1]; ageIn(years: Integer[1]) { $this.age + $years }: Integer[1] }`
fn person_model() -> (PureModel, ElementId) {
    let mut b = ModelBuilder::new();
    let mut person = class(
        Vec::new(),
        vec![
            property("name", named(STRING_ID), Multiplicity::PureOne),
            property("age", named(INTEGER_ID), Multiplicity::ZeroOrOne),
        ],
    );
    person.qualified_properties.push(qualified_property(
        "ageIn",
        vec![param("years", named(INTEGER_ID), Multiplicity::PureOne)],
        named(INTEGER_ID),
        Multiplicity::PureOne,
        vec![expr::call(
            PLUS,
            vec![expr::collection(vec![
                expr::prop(expr::var("this"), "age"),
                expr::var("years"),
            ])],
        )],
    ));
    let id = b.class("my::Person", person);
    (b.finish(), id)
}

/// The compiler-emitted shape of `^Person(name=$name, age=$age)`:
/// `[class, id, typeArgs, typeVarValues, key, value, augmented, …]`.
fn new_person(person: ElementId, name: &str, age: Option<i64>) -> ValueSpec {
    let mut args = vec![
        expr::type_ref(named(person)),
        expr::string("p"),
        expr::collection(Vec::new()),
        expr::collection(Vec::new()),
        expr::string("name"),
        expr::string(name),
        expr::boolean(false),
    ];
    if let Some(age) = age {
        args.extend([expr::string("age"), expr::int(age), expr::boolean(false)]);
    }
    expr::call(NEW, args)
}

fn evaluation_error(err: &PureException) -> Option<&str> {
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => Some(m),
        _ => None,
    }
}

#[test]
fn let_binds_in_the_current_scope_and_returns_the_value() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    let body = [
        expr::call(LET, vec![expr::string("x"), expr::int(5)]),
        expr::call(
            PLUS,
            vec![expr::collection(vec![expr::var("x"), expr::int(1)])],
        ),
    ];
    assert_eq!(eval.eval_body(&body).unwrap(), Value::Integer(6));
    assert_eq!(
        eval.eval(&expr::call(LET, vec![expr::string("y"), expr::int(9)]))
            .unwrap(),
        Value::Integer(9)
    );
}

#[test]
fn if_evaluates_only_the_taken_branch() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let poison = expr::lambda(
        Vec::new(),
        vec![expr::call("nope_Any_1__Any_1_", Vec::new())],
    );
    let taken = |v: i64| expr::lambda(Vec::new(), vec![expr::int(v)]);
    let when_true = expr::call(IF, vec![expr::boolean(true), taken(1), poison.clone()]);
    let when_false = expr::call(IF, vec![expr::boolean(false), poison, taken(2)]);
    assert_eq!(eval.eval(&when_true).unwrap(), Value::Integer(1));
    assert_eq!(eval.eval(&when_false).unwrap(), Value::Integer(2));
}

#[test]
fn new_builds_an_instance_whose_properties_read_back() {
    let (model, person) = person_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    let body = [
        expr::call(
            LET,
            vec![expr::string("p"), new_person(person, "Ann", Some(30))],
        ),
        expr::prop(expr::var("p"), "name"),
    ];
    assert_eq!(eval.eval_body(&body).unwrap(), Value::String("Ann".into()));
    assert_eq!(
        eval.eval(&expr::prop(expr::var("p"), "age")).unwrap(),
        Value::Integer(30)
    );
    assert!(matches!(eval.context().get("p"), Some(Value::Object(_))));
}

#[test]
fn optional_property_left_unset_reads_as_unit() {
    let (model, person) = person_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.eval(&expr::call(
        LET,
        vec![expr::string("p"), new_person(person, "Ann", None)],
    ))
    .unwrap();
    assert_eq!(
        eval.eval(&expr::prop(expr::var("p"), "age")).unwrap(),
        Value::Unit
    );
}

#[test]
fn new_without_a_required_property_is_a_multiplicity_violation() {
    let (model, person) = person_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let bare = expr::call(
        NEW,
        vec![
            expr::type_ref(named(person)),
            expr::string("p"),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
        ],
    );
    let err = eval.eval(&bare).unwrap_err();
    let msg = evaluation_error(&err).expect("an evaluation error");
    assert!(msg.contains("multiplicity violations"), "{msg}");
    assert!(msg.contains("'name' requires"), "{msg}");
}

#[test]
fn qualified_property_binds_this_and_its_arguments() {
    let (model, person) = person_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.eval(&expr::call(
        LET,
        vec![expr::string("p"), new_person(person, "Ann", Some(30))],
    ))
    .unwrap();
    let call = expr::qp(expr::var("p"), "ageIn", vec![expr::int(5)]);
    assert_eq!(eval.eval(&call).unwrap(), Value::Integer(35));
}

#[test]
fn copy_overrides_one_property_and_leaves_the_source_alone() {
    let (model, person) = person_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    eval.eval(&expr::call(
        LET,
        vec![expr::string("p"), new_person(person, "Ann", Some(30))],
    ))
    .unwrap();
    let copied = expr::call(
        COPY,
        vec![
            expr::var("p"),
            expr::string("name"),
            expr::string("Bob"),
            expr::boolean(false),
        ],
    );
    eval.eval(&expr::call(LET, vec![expr::string("q"), copied]))
        .unwrap();
    assert_eq!(
        eval.eval(&expr::prop(expr::var("q"), "name")).unwrap(),
        Value::String("Bob".into())
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::var("q"), "age")).unwrap(),
        Value::Integer(30)
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::var("p"), "name")).unwrap(),
        Value::String("Ann".into())
    );
    // `eq` is identity: the copy is a different object.
    let same = expr::call(EQ, vec![expr::var("p"), expr::var("p")]);
    let other = expr::call(EQ, vec![expr::var("p"), expr::var("q")]);
    assert_eq!(eval.eval(&same).unwrap(), Value::Boolean(true));
    assert_eq!(eval.eval(&other).unwrap(), Value::Boolean(false));
}

#[test]
fn eq_and_equal_on_primitives_and_collections() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let mut t = |e: &ValueSpec| eval.eval(e).unwrap();
    assert_eq!(
        t(&expr::call(EQ, vec![expr::int(1), expr::int(1)])),
        Value::Boolean(true)
    );
    assert_eq!(
        t(&expr::call(EQ, vec![expr::int(1), expr::string("1")])),
        Value::Boolean(false)
    );
    let pair = || expr::collection(vec![expr::int(1), expr::int(2)]);
    assert_eq!(
        t(&expr::call(EQUAL, vec![pair(), pair()])),
        Value::Boolean(true)
    );
    assert_eq!(
        t(&expr::call(
            EQUAL,
            vec![pair(), expr::collection(vec![expr::int(2), expr::int(1)])]
        )),
        Value::Boolean(false)
    );
}

#[test]
fn boolean_natives_short_circuit() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let poison = || expr::call("nope_Any_1__Any_1_", Vec::new());
    assert_eq!(
        eval.eval(&expr::call(AND, vec![expr::boolean(false), poison()]))
            .unwrap(),
        Value::Boolean(false)
    );
    assert_eq!(
        eval.eval(&expr::call(OR, vec![expr::boolean(true), poison()]))
            .unwrap(),
        Value::Boolean(true)
    );
    assert_eq!(
        eval.eval(&expr::call(
            AND,
            vec![expr::boolean(true), expr::boolean(false)]
        ))
        .unwrap(),
        Value::Boolean(false)
    );
    assert_eq!(
        eval.eval(&expr::call(NOT, vec![expr::boolean(false)]))
            .unwrap(),
        Value::Boolean(true)
    );
    assert!(
        eval.eval(&expr::call(AND, vec![expr::boolean(true), poison()]))
            .is_err()
    );
}

#[test]
fn minus_folds_binary_and_negates_unary() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::call(
            MINUS,
            vec![expr::collection(vec![expr::int(10), expr::int(3)])]
        ))
        .unwrap(),
        Value::Integer(7)
    );
    assert_eq!(
        eval.eval(&expr::call(
            MINUS,
            vec![expr::collection(vec![expr::int(5)])]
        ))
        .unwrap(),
        Value::Integer(-5)
    );
}

#[test]
fn new_populates_the_association_inverse() {
    let mut b = ModelBuilder::new();
    let person = b.class(
        "my::Person",
        class(
            Vec::new(),
            vec![property("name", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let firm = b.class(
        "my::Firm",
        class(
            Vec::new(),
            vec![property(
                "legalName",
                named(STRING_ID),
                Multiplicity::PureOne,
            )],
        ),
    );
    b.association(
        "my::Employment",
        [
            property("employees", named(person), Multiplicity::ZeroOrMany),
            property("firm", named(firm), Multiplicity::ZeroOrOne),
        ],
    );
    let model = b.finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    let new_firm = expr::call(
        NEW,
        vec![
            expr::type_ref(named(firm)),
            expr::string("f"),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
            expr::string("legalName"),
            expr::string("Acme"),
            expr::boolean(false),
        ],
    );
    eval.eval(&expr::call(LET, vec![expr::string("f"), new_firm]))
        .unwrap();
    let new_person = expr::call(
        NEW,
        vec![
            expr::type_ref(named(person)),
            expr::string("p"),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
            expr::string("name"),
            expr::string("Ann"),
            expr::boolean(false),
            expr::string("firm"),
            expr::var("f"),
            expr::boolean(false),
        ],
    );
    eval.eval(&expr::call(LET, vec![expr::string("p"), new_person]))
        .unwrap();
    let employees = eval.eval(&expr::prop(expr::var("f"), "employees")).unwrap();
    let p = eval.context().get("p").cloned().expect("p is bound");
    assert!(matches!(p, Value::Object(_)));
    assert_eq!(employees, p);
}
