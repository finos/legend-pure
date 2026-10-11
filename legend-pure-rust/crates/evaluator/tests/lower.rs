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

//! The lowering over hand-built M3 nodes: the shapes the Java compiler
//! produces (see `lower::node_builders::m3`) lowered into the evaluator's
//! tree and model, then run. No JVM.

use std::cell::RefCell;
use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_evaluator::builders::ModelBuilder;
use legend_pure_evaluator::error::PureRuntimeError;
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::heap::{HeapEntry, TypedObject};
use legend_pure_evaluator::lower::node_builders::{TestNode, m3};
use legend_pure_evaluator::lower::{LowerError, Lowering};
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::{FunctionValue, LambdaClosure, Value};
use legend_pure_metamodel::bootstrap::{BOOLEAN_ID, INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::types::{ExprKind, Multiplicity};

const PLUS: &str = "meta::pure::functions::math::plus_Integer_MANY__Integer_1_";
const IF: &str = "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_";
const LET: &str = "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_";
const EQ: &str = "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_";

fn integer() -> Value {
    m3::generic(Some(Value::Element(INTEGER_ID)), Vec::new())
}

fn one() -> Value {
    m3::mult(1, Some(1))
}

fn many() -> Value {
    m3::mult(0, None)
}

fn literal(v: Value) -> Value {
    m3::iv(vec![v], Some(integer()), Some(one()))
}

fn int(i: i64) -> Value {
    literal(Value::Integer(i))
}

/// `plus([a, b, …])`: one collection argument, as the Java compiler emits.
fn plus(items: Vec<Value>) -> Value {
    let plus = m3::native_function(
        PLUS,
        "plus",
        m3::function_type(
            vec![m3::param("values", integer(), many())],
            integer(),
            one(),
        ),
    );
    m3::sfe(
        plus,
        "plus",
        vec![m3::iv(items, Some(integer()), Some(many()))],
    )
}

fn root(body: Vec<Value>) -> Value {
    m3::lambda(Vec::new(), body, integer(), one()).build()
}

/// Lower a root lambda over `model`, apply what it added, run it.
fn lower_and_run(model: &mut PureModel, root: &Value) -> Result<Value, LowerError> {
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(model, chunk, model.chunk_len(chunk));
    let closure = lowering.lower_root_lambda(root)?;
    lowering.into_extension().apply(model).expect("apply");
    Ok(run(model, closure))
}

fn run(model: &PureModel, closure: LambdaClosure) -> Value {
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(model, &registry);
    eval.context_mut().push_scope();
    eval.apply_callable(
        &Value::Function(Box::new(FunctionValue::Lambda(closure))),
        &[],
    )
    .unwrap()
}

#[test]
fn a_native_call_lowers_to_an_element_named_by_its_mangled_signature() {
    let mut model = PureModel::bootstrapped();
    let before = model.chunks.len();
    let lambda = root(vec![plus(vec![int(1), int(2)])]);

    let chunk = u16::try_from(before).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(closure.parameters.is_empty());
    assert_eq!(closure.body.len(), 1);
    let ExprKind::FunctionCall(call) = &*closure.body[0].kind else {
        panic!("expected a call, got {:?}", closure.body[0].kind);
    };
    assert_eq!(call.function_name, "plus");
    let function = call.function.expect("resolved to the lowered element");
    assert_eq!(call.arguments.len(), 1);
    assert!(
        matches!(&*call.arguments[0].kind, ExprKind::Collection { elements } if elements.len() == 2)
    );

    let extension = lowering.into_extension();
    assert_eq!(extension.len(), 1);
    extension.apply(&mut model).expect("apply");
    assert_eq!(model.chunks.len(), before + 1);
    assert_eq!(
        model.get_node(function).name,
        "plus_Integer_MANY__Integer_1_"
    );
    assert!(
        matches!(model.get_element(function), Element::Function(f) if f.is_native && f.parameters.len() == 1)
    );
    assert_eq!(model.resolve_fqn_str(PLUS), Some(function));

    assert_eq!(run(&model, closure), Value::Integer(3));
}

#[test]
fn the_bridge_chunk_is_whatever_comes_after_the_model_s_chunks() {
    let mut model = ModelBuilder::new().finish();
    assert_eq!(model.chunks.len(), 2);
    assert_eq!(
        lower_and_run(&mut model, &root(vec![plus(vec![int(2), int(3)])])),
        Ok(Value::Integer(5))
    );
    assert_eq!(model.chunks.len(), 3);
    assert_eq!(model.chunk_len(2), 1);

    let eq = m3::native_function(
        EQ,
        "eq",
        m3::function_type(
            vec![
                m3::param("left", integer(), one()),
                m3::param("right", integer(), one()),
            ],
            m3::generic(Some(Value::Element(BOOLEAN_ID)), Vec::new()),
            one(),
        ),
    );
    let body = vec![m3::sfe(eq, "eq", vec![plus(vec![int(1), int(1)]), int(2)])];
    let chunk = 2;
    let mut lowering = Lowering::new(&model, chunk, model.chunk_len(chunk));
    let closure = lowering.lower_root_lambda(&root(body)).unwrap();
    let extension = lowering.into_extension();
    assert_eq!(extension.len(), 1);
    extension.apply(&mut model).expect("apply");
    assert_eq!(model.chunk_len(2), 2);
    assert_eq!(
        model.resolve_fqn_str(EQ),
        Some(ElementId::InstanceId {
            chunk_id: 2,
            local_idx: 1
        })
    );
    assert_eq!(run(&model, closure), Value::Boolean(true));
}

#[test]
fn nested_lambdas_lower_bare_so_if_takes_a_branch() {
    let mut model = PureModel::bootstrapped();
    let string = m3::generic(Some(Value::Element(STRING_ID)), Vec::new());
    let branch = |s: &str| {
        m3::iv(
            vec![
                m3::lambda(
                    Vec::new(),
                    vec![m3::iv(
                        vec![Value::String(s.into())],
                        Some(string.clone()),
                        Some(one()),
                    )],
                    string.clone(),
                    one(),
                )
                .build(),
            ],
            None,
            None,
        )
    };
    let if_fn = m3::native_function(
        IF,
        "if",
        m3::function_type(Vec::new(), m3::type_parameter("T"), many()),
    );
    let call = m3::sfe(
        if_fn,
        "if",
        vec![
            m3::iv(vec![Value::Boolean(true)], None, None),
            branch("yes"),
            branch("no"),
        ],
    );
    let lambda = root(vec![call]);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    let ExprKind::FunctionCall(call) = &*closure.body[0].kind else {
        panic!("expected a call");
    };
    assert!(
        matches!(&*call.arguments[1].kind, ExprKind::Lambda { parameters, body } if parameters.is_empty() && body.len() == 1)
    );
    lowering.into_extension().apply(&mut model).expect("apply");
    assert_eq!(run(&model, closure), Value::String("yes".into()));
}

#[test]
fn let_keeps_its_simple_name_for_the_free_variable_walk() {
    let mut model = PureModel::bootstrapped();
    let let_fn = m3::native_function(
        LET,
        "letFunction",
        m3::function_type(Vec::new(), m3::type_parameter("T"), many()),
    );
    let body = vec![
        m3::sfe(
            let_fn,
            "letFunction",
            vec![m3::iv(vec![Value::String("x".into())], None, None), int(7)],
        ),
        m3::var("x"),
    ];
    let lambda = root(body);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(
        matches!(&*closure.body[0].kind, ExprKind::FunctionCall(c) if c.function_name == "letFunction")
    );
    assert!(matches!(&*closure.body[1].kind, ExprKind::Variable { name } if name == "x"));
    lowering.into_extension().apply(&mut model).expect("apply");
    assert_eq!(run(&model, closure), Value::Integer(7));
}

#[test]
fn every_literal_kind_lowers() {
    let mut model = PureModel::bootstrapped();
    let date = legend_pure_evaluator::date::PureDate::strict_date(2014, 2, 1).unwrap();
    let values = vec![
        Value::Float(1.5),
        Value::Decimal(rust_decimal::Decimal::new(25, 1)),
        Value::String("s".into()),
        Value::Boolean(false),
        Value::Date(date),
    ];
    let mut items: Vec<Value> = values
        .iter()
        .cloned()
        .map(|v| m3::iv(vec![v], None, None))
        .collect();
    items.push(m3::iv(vec![Value::Latest], None, None));
    let lambda = root(vec![m3::iv(items, None, Some(many()))]);
    let result = lower_and_run(&mut model, &lambda).unwrap();
    let Value::Collection(items) = &result else {
        panic!("expected a collection, got {result:?}");
    };
    let mut expected = values;
    expected.push(Value::Latest);
    assert_eq!(items.iter().cloned().collect::<Vec<_>>(), expected);
}

#[test]
fn an_unknown_node_kind_is_unsupported_and_adds_nothing() {
    let model = PureModel::bootstrapped();
    let odd = TestNode::new("meta::x::Y").build();
    let lambda = root(vec![plus(vec![int(1), odd])]);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let err = lowering.lower_root_lambda(&lambda).unwrap_err();
    assert_eq!(
        err,
        LowerError::Unsupported("lowering meta::x::Y as a value".into())
    );
    drop(lowering);
    assert_eq!(model.chunk_len(chunk), 0);
}

#[test]
fn a_lambda_with_open_variables_but_no_context_is_unsupported() {
    let model = PureModel::bootstrapped();
    let lambda = m3::lambda(Vec::new(), vec![m3::var("k")], integer(), one())
        .prop("openVariables", vec![Value::String("k".into())])
        .build();
    let mut lowering = Lowering::new(&model, 1, 0);
    let err = lowering.lower_root_lambda(&lambda).unwrap_err();
    assert!(
        matches!(err, LowerError::Unsupported(m) if m.starts_with("lowering captured variables"))
    );
}

#[test]
fn specs_carry_the_node_s_span() {
    let model = PureModel::bootstrapped();
    let lit = TestNode::new(legend_pure_evaluator::m3_paths::INSTANCE_VALUE)
        .prop("values", vec![Value::Integer(4)])
        .at("f.pure", 3, 9)
        .build();
    let lambda = root(vec![lit]);
    let mut lowering = Lowering::new(&model, 1, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    let span = &closure.body[0].source_info;
    assert_eq!(
        (span.source.as_str(), span.start_line, span.start_column),
        ("f.pure", 3, 9)
    );
    assert_eq!(
        lower_and_run(&mut PureModel::bootstrapped(), &root(vec![int(1)])),
        Ok(Value::Integer(1))
    );
    let _ = Multiplicity::PureOne;
}

fn string_type() -> Value {
    m3::generic(Some(Value::Element(STRING_ID)), Vec::new())
}

fn str_lit(s: &str) -> Value {
    m3::iv(
        vec![Value::String(s.into())],
        Some(string_type()),
        Some(one()),
    )
}

#[test]
fn a_pure_bodied_function_lowers_with_its_body_and_runs() {
    let mut model = PureModel::bootstrapped();
    let twice = m3::concrete_function(
        "my::pkg::twice_Integer_1__Integer_1_",
        "twice",
        m3::function_type(vec![m3::param("x", integer(), one())], integer(), one()),
        vec![plus(vec![m3::var("x"), m3::var("x")])],
    );
    let lambda = root(vec![m3::sfe(twice.clone(), "twice", vec![int(21)])]);
    assert_eq!(lower_and_run(&mut model, &lambda), Ok(Value::Integer(42)));
    let id = model
        .resolve_fqn_str("my::pkg::twice_Integer_1__Integer_1_")
        .expect("lowered");
    assert!(
        matches!(model.get_element(id), Element::Function(f) if !f.is_native && f.body.len() == 1 && f.parameters[0].name == "x")
    );
}

#[test]
fn mutually_recursive_functions_lower_once_each() {
    let mut model = PureModel::bootstrapped();
    let boolean = || m3::generic(Some(Value::Element(BOOLEAN_ID)), Vec::new());
    let eq = || {
        m3::native_function(
            EQ,
            "eq",
            m3::function_type(
                vec![
                    m3::param("l", integer(), one()),
                    m3::param("r", integer(), one()),
                ],
                boolean(),
                one(),
            ),
        )
    };
    let if_fn = || {
        m3::native_function(
            IF,
            "if",
            m3::function_type(Vec::new(), m3::type_parameter("T"), many()),
        )
    };
    let minus = || {
        m3::native_function(
            "meta::pure::functions::math::minus_Integer_MANY__Integer_1_",
            "minus",
            m3::function_type(
                vec![m3::param("values", integer(), many())],
                integer(),
                one(),
            ),
        )
    };
    let branch = |body: Value| {
        m3::iv(
            vec![m3::lambda(Vec::new(), vec![body], boolean(), one()).build()],
            None,
            None,
        )
    };
    let bool_lit = |b: bool| m3::iv(vec![Value::Boolean(b)], Some(boolean()), Some(one()));
    let n_minus_1 = || {
        m3::sfe(
            minus(),
            "minus",
            vec![m3::iv(
                vec![m3::var("n"), int(1)],
                Some(integer()),
                Some(many()),
            )],
        )
    };
    let signature = || m3::function_type(vec![m3::param("n", integer(), one())], boolean(), one());
    let is_odd_shell = TestNode::new(legend_pure_evaluator::m3_paths::CONCRETE_FUNCTION_DEFINITION)
        .path("my::pkg::isOdd_Integer_1__Boolean_1_")
        .one("functionName", Value::String("isOdd".into()))
        .one(
            "classifierGenericType",
            m3::classifier_generic_type(signature()),
        )
        .build();
    let is_even = m3::concrete_function(
        "my::pkg::isEven_Integer_1__Boolean_1_",
        "isEven",
        signature(),
        vec![m3::sfe(
            if_fn(),
            "if",
            vec![
                m3::sfe(eq(), "eq", vec![m3::var("n"), int(0)]),
                branch(bool_lit(true)),
                branch(m3::sfe(is_odd_shell.clone(), "isOdd", vec![n_minus_1()])),
            ],
        )],
    );
    let Value::Object(h) = &is_odd_shell else {
        unreachable!()
    };
    h.borrow_mut()
        .mutate_set(
            "expressionSequence",
            &[m3::sfe(
                if_fn(),
                "if",
                vec![
                    m3::sfe(eq(), "eq", vec![m3::var("n"), int(0)]),
                    branch(bool_lit(false)),
                    branch(m3::sfe(is_even.clone(), "isEven", vec![n_minus_1()])),
                ],
            )],
        )
        .unwrap();
    let lambda = root(vec![m3::sfe(is_even, "isEven", vec![int(6)])]);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    let extension = lowering.into_extension();
    assert_eq!(extension.len(), 5);
    extension.apply(&mut model).expect("apply");
    assert_eq!(run(&model, closure), Value::Boolean(true));
}

#[test]
fn a_function_referenced_as_a_value_becomes_an_element_reference() {
    let model = PureModel::bootstrapped();
    let plus_fn = m3::native_function(
        PLUS,
        "plus",
        m3::function_type(
            vec![m3::param("values", integer(), many())],
            integer(),
            one(),
        ),
    );
    let lambda = root(vec![m3::iv(vec![plus_fn], None, Some(one()))]);
    let mut lowering = Lowering::new(&model, 1, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(matches!(
        &*closure.body[0].kind,
        ExprKind::PackageableElementRef { .. }
    ));
    let extension = lowering.into_extension();
    assert_eq!(extension.len(), 1);
    assert!(extension.lookup(PLUS).is_some());
}

#[test]
fn captured_variables_become_the_closure_s_captures() {
    let mut model = PureModel::bootstrapped();
    let lambda = m3::lambda(
        Vec::new(),
        vec![plus(vec![m3::var("k"), int(1)])],
        integer(),
        one(),
    )
    .captured("k", vec![Value::Integer(41)])
    .build();
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert_eq!(closure.captures.get("k"), Some(&Value::Integer(41)));
    lowering.into_extension().apply(&mut model).expect("apply");
    assert_eq!(run(&model, closure), Value::Integer(42));
}

#[test]
fn a_captured_lambda_is_lowered_into_a_closure() {
    let mut model = PureModel::bootstrapped();
    let g = m3::lambda(
        vec![m3::param("x", integer(), one())],
        vec![plus(vec![m3::var("x"), int(1)])],
        integer(),
        one(),
    )
    .build();
    let lambda = m3::lambda(Vec::new(), vec![str_lit("unused")], string_type(), one())
        .captured("g", vec![g])
        .build();
    let (f, extension) = {
        let mut lowering = Lowering::new(&model, 1, 0);
        let closure = lowering.lower_root_lambda(&lambda).unwrap();
        let Some(Value::Function(f)) = closure.captures.get("g") else {
            panic!(
                "expected a function value, got {:?}",
                closure.captures.get("g")
            );
        };
        let FunctionValue::Lambda(g) = &**f else {
            panic!("expected a lambda closure");
        };
        assert_eq!(g.parameters.len(), 1);
        assert_eq!(g.parameters[0].name, "x");
        (f.clone(), lowering.into_extension())
    };
    extension.apply(&mut model).expect("apply");
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    eval.context_mut().push_scope();
    let applied = eval
        .apply_callable(&Value::Function(f), &[Value::Integer(5)])
        .unwrap();
    assert_eq!(applied, Value::Integer(6));
}

/// A capture source that, like a Java-backed lambda, allocates fresh
/// nodes on every read and keeps none of them.
struct FreshCaptures {
    inner: Value,
    f_len: usize,
    g_len: usize,
}

impl TypedObject for FreshCaptures {
    fn classifier_path(&self) -> &str {
        legend_pure_evaluator::m3_paths::LAMBDA_FUNCTION
    }

    fn get_property(&self, name: &str) -> Option<Value> {
        self.get_property_values(name).head().cloned()
    }

    fn get_property_values(&self, name: &str) -> PVector<Value> {
        match &self.inner {
            Value::Object(h) => match &*h.borrow() {
                HeapEntry::Typed(o) => o.get_property_values(name),
                HeapEntry::Dynamic(_) => PVector::new(),
            },
            _ => PVector::new(),
        }
    }

    fn set_property(&mut self, _name: &str, _value: Value) -> Result<(), PureRuntimeError> {
        Err(PureRuntimeError::Unsupported("read-only".into()))
    }

    fn captured_values(&self, name: &str) -> Option<PVector<Value>> {
        let lambda = if name == "f" {
            let body = (0..self.f_len).map(|i| int(i64::try_from(i).unwrap()));
            m3::lambda(Vec::new(), body.collect(), integer(), one()).build()
        } else {
            let churn: Vec<Value> = (0..self.g_len).map(|_| many()).collect();
            drop(churn);
            let body = (0..self.g_len).map(|_| str_lit("from-g"));
            m3::lambda(Vec::new(), body.collect(), string_type(), one()).build()
        };
        Some(std::iter::once(lambda).collect())
    }

    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn as_any_mut(&mut self) -> &mut dyn std::any::Any {
        self
    }
}

#[test]
fn a_node_freed_mid_lowering_never_lends_its_cached_spec_to_its_successor() {
    let model = PureModel::bootstrapped();
    for f_len in 1..6 {
        for g_len in 1..6 {
            for _ in 0..40 {
                let inner = m3::lambda(Vec::new(), vec![str_lit("unused")], string_type(), one())
                    .captured("f", vec![])
                    .captured("g", vec![])
                    .build();
                let root = Value::Object(Rc::new(RefCell::new(HeapEntry::Typed(Box::new(
                    FreshCaptures {
                        inner,
                        f_len,
                        g_len,
                    },
                )))));
                let mut lowering = Lowering::new(&model, 1, 0);
                let closure = lowering.lower_root_lambda(&root).unwrap();
                let Some(Value::Function(g)) = closure.captures.get("g") else {
                    panic!("g was not captured as a function");
                };
                let FunctionValue::Lambda(g) = &**g else {
                    panic!("g is not a lambda");
                };
                assert_eq!(g.body.len(), g_len);
                for spec in g.body.iter() {
                    assert!(
                        matches!(&*spec.kind, ExprKind::StringLiteral(s) if s == "from-g"),
                        "g's body carries another node's spec: {:?} (f_len={f_len} g_len={g_len} f={:?})",
                        spec.kind,
                        closure.captures.get("f")
                    );
                }
            }
        }
    }
}

const NEW_KE: &str = "meta::pure::functions::lang::new_Class_1__String_1__KeyExpression_MANY__T_1_";

fn class_meta(model: &PureModel) -> Value {
    Value::Element(
        model
            .resolve_fqn_str(legend_pure_evaluator::m3_paths::CLASS)
            .unwrap(),
    )
}

/// `^Person(firstName='John')`, as the Java compiler emits it: the class
/// as an empty `InstanceValue` typed `Class<Person>`, the id, the key
/// expressions.
fn new_person(model: &PureModel, person: &Value, first_name: &str) -> Value {
    let new_fn = m3::native_function(
        NEW_KE,
        "new",
        m3::function_type(Vec::new(), m3::type_parameter("T"), one()),
    );
    m3::sfe(
        new_fn,
        "new",
        vec![
            m3::iv(
                Vec::new(),
                Some(m3::generic(
                    Some(class_meta(model)),
                    vec![m3::generic(Some(person.clone()), Vec::new())],
                )),
                Some(one()),
            ),
            str_lit(""),
            m3::iv(
                vec![m3::key_expression("firstName", str_lit(first_name), false)],
                None,
                Some(many()),
            ),
        ],
    )
}

/// `Class Person { firstName: String[1]; greeting(prefix: String[1]) { $prefix + $this.firstName } }`.
/// The qualified property's `this` parameter is typed by the class itself,
/// so the node graph is a cycle, as the Java graph is.
fn person_class() -> Value {
    let shell = TestNode::new(legend_pure_evaluator::m3_paths::CLASS)
        .path("my::model::Person")
        .build();
    let greeting = m3::qualified_property(
        "greeting",
        m3::generic(Some(shell.clone()), Vec::new()),
        vec![m3::param("prefix", string_type(), one())],
        string_type(),
        one(),
        vec![m3::sfe(
            m3::native_function(
                "meta::pure::functions::string::plus_String_MANY__String_1_",
                "plus",
                m3::function_type(
                    vec![m3::param("values", string_type(), many())],
                    string_type(),
                    one(),
                ),
            ),
            "plus",
            vec![m3::iv(
                vec![
                    m3::var("prefix"),
                    m3::sfe(
                        m3::property("firstName", string_type(), one()),
                        "firstName",
                        vec![m3::var("this")],
                    ),
                ],
                Some(string_type()),
                Some(many()),
            )],
        )],
    );
    let Value::Object(h) = &shell else {
        unreachable!()
    };
    h.borrow_mut()
        .mutate_set(
            "properties",
            &[m3::property("firstName", string_type(), one())],
        )
        .unwrap();
    h.borrow_mut()
        .mutate_set("qualifiedProperties", &[greeting])
        .unwrap();
    shell
}

#[test]
fn a_class_construction_lowers_the_class_and_builds_through_the_platform_new() {
    let mut model = PureModel::bootstrapped();
    let person = person_class();
    let property = m3::property("firstName", string_type(), one());
    let let_fn = m3::native_function(
        LET,
        "letFunction",
        m3::function_type(Vec::new(), m3::type_parameter("T"), many()),
    );
    let body = vec![
        m3::sfe(
            let_fn,
            "letFunction",
            vec![str_lit("p"), new_person(&model, &person, "John")],
        ),
        m3::sfe(property, "firstName", vec![m3::var("p")]),
    ];
    let lambda = root(body);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(
        matches!(&*closure.body[1].kind, ExprKind::PropertyCall(c) if c.function_name == "firstName" && c.arguments.len() == 1)
    );
    let ExprKind::FunctionCall(let_call) = &*closure.body[0].kind else {
        panic!()
    };
    let ExprKind::FunctionCall(new_call) = &*let_call.arguments[1].kind else {
        panic!()
    };
    assert!(matches!(
        &*new_call.arguments[0].kind,
        ExprKind::TypeReference { .. }
    ));
    assert!(
        matches!(&*new_call.arguments[2].kind, ExprKind::KeyExpression { key, add: false, .. } if key == "firstName")
    );
    let extension = lowering.into_extension();
    extension.apply(&mut model).expect("apply");
    let person_id = model
        .resolve_fqn_str("my::model::Person")
        .expect("class lowered");
    let Element::Class(class) = model.get_element(person_id) else {
        panic!()
    };
    assert_eq!(class.properties.len(), 1);
    assert_eq!(class.qualified_properties.len(), 1);
    assert_eq!(
        class.qualified_properties[0].parameters.len(),
        1,
        "the `this` parameter is the receiver, not a parameter"
    );
    assert_eq!(run(&model, closure), Value::String("John".into()));
}

#[test]
fn an_enum_value_lowers_its_enumeration_first() {
    let mut model = PureModel::bootstrapped();
    let colour = m3::enumeration("my::model::Colour", &["RED", "GREEN"]);
    let Value::Object(h) = &colour else {
        unreachable!()
    };
    let green = h
        .borrow()
        .get_property_values("values")
        .get(1)
        .cloned()
        .unwrap();
    let lambda = root(vec![m3::iv(
        vec![green],
        Some(m3::generic(Some(colour.clone()), Vec::new())),
        Some(one()),
    )]);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    let ExprKind::EnumValue {
        enum_element,
        value,
    } = &*closure.body[0].kind
    else {
        panic!("expected an enum value, got {:?}", closure.body[0].kind);
    };
    assert_eq!(value, "GREEN");
    let enum_id = *enum_element;
    lowering.into_extension().apply(&mut model).expect("apply");
    assert_eq!(model.resolve_fqn_str("my::model::Colour"), Some(enum_id));
    assert!(matches!(model.get_element(enum_id), Element::Enumeration(e) if e.values.len() == 2));
    assert_eq!(
        run(&model, closure),
        Value::EnumValue {
            enum_id,
            member: "GREEN".into()
        }
    );
}

#[test]
fn a_type_literal_lowers_to_a_type_reference() {
    let mut model = PureModel::bootstrapped();
    let person = person_class();
    let lambda = root(vec![m3::iv(
        Vec::new(),
        Some(m3::generic(Some(person), Vec::new())),
        Some(one()),
    )]);
    let chunk = u16::try_from(model.chunks.len()).unwrap();
    let mut lowering = Lowering::new(&model, chunk, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(matches!(
        &*closure.body[0].kind,
        ExprKind::TypeReference { .. }
    ));
    lowering.into_extension().apply(&mut model).expect("apply");
    let person_id = model.resolve_fqn_str("my::model::Person").unwrap();
    assert_eq!(run(&model, closure), Value::Element(person_id));
}

#[test]
fn a_type_that_cannot_lower_is_dropped_unless_an_element_is_half_built() {
    let model = PureModel::bootstrapped();
    let odd_type = m3::generic(Some(TestNode::new("meta::x::Y").build()), Vec::new());
    let lambda = root(vec![m3::iv(
        vec![Value::Integer(1)],
        Some(odd_type.clone()),
        Some(one()),
    )]);
    let mut lowering = Lowering::new(&model, 1, 0);
    let closure = lowering.lower_root_lambda(&lambda).unwrap();
    assert!(closure.body[0].type_info.is_none());
    let bad_class = m3::class(
        "my::model::Bad",
        vec![m3::property("p", odd_type, one())],
        Vec::new(),
    );
    let lambda = root(vec![m3::iv(
        Vec::new(),
        Some(m3::generic(Some(bad_class), Vec::new())),
        Some(one()),
    )]);
    let mut lowering = Lowering::new(&model, 1, 0);
    let err = lowering.lower_root_lambda(&lambda).unwrap_err();
    assert!(
        matches!(&err, LowerError::Unsupported(m) if m.contains("meta::x::Y")),
        "{err:?}"
    );
}
