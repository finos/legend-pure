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

//! The body executor on the shapes it resolves, each run on both paths
//! with the outcomes compared (values, or errors in kind, message,
//! source and frames), and the built bodies checked for what ran on the
//! tree walker instead.

use std::rc::Rc;

use legend_pure_evaluator::builders::{
    ModelBuilder, class, expr, function, named, param, property,
};
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::exec::{Body, BodyKind, Op, SlotKind};
use legend_pure_evaluator::hooks::EvalHooks;
use legend_pure_evaluator::native::{EvalContextTrait, Evaluated, NativeFunction, NativeRegistry};
use legend_pure_evaluator::value::{FunctionValue, Value};
use legend_pure_metamodel::bootstrap::{ANY_ID, INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{ExprKind, Multiplicity, Parameter, ValueSpec};

const PLUS: &str = "plus_Number_MANY__Number_1_";
const MINUS: &str = "minus_Number_MANY__Number_1_";
const TIMES: &str = "times_Number_MANY__Number_1_";
const LET: &str = "letFunction_String_1__T_m__T_m_";
const IF: &str = "if_Boolean_1__Function_1__Function_1__T_m_";
const AND: &str = "and_Boolean_1__Boolean_1__Boolean_1_";
const OR: &str = "or_Boolean_1__Boolean_1__Boolean_1_";
const LESS_THAN_EQUAL: &str = "lessThanEqual_Number_1__Number_1__Boolean_1_";
const MAP: &str = "map_T_MANY__Function_1__V_MANY_";
const MOD: &str = "mod_Integer_1__Integer_1__Integer_1_";
const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";
const VISIBLE: &str = "visibleNames__String_MANY_";

/// The names of the variables visible where it is called, sorted, as a
/// native that reads variables by name (`reactivate`, `evaluate`) sees
/// them.
#[derive(Debug)]
struct VisibleNames;

impl NativeFunction for VisibleNames {
    fn execute(
        &self,
        _args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let mut names: Vec<String> = ctx
            .context()
            .iter_bindings()
            .map(|(name, value)| format!("{name}={value:?}"))
            .collect();
        names.sort();
        Ok(Evaluated::new(Value::from_vec(
            names.into_iter().map(|n| Value::String(n.into())).collect(),
        )))
    }
}

fn registry() -> NativeRegistry {
    let mut registry = NativeRegistry::standard();
    registry.register(VISIBLE, VisibleNames);
    registry
}

fn int(name: &str) -> Parameter {
    param(name, named(INTEGER_ID), Multiplicity::PureOne)
}

fn arith(key: &str, a: ValueSpec, b: ValueSpec) -> ValueSpec {
    expr::call(key, vec![expr::collection(vec![a, b])])
}

fn let_(name: &str, value: ValueSpec) -> ValueSpec {
    expr::call(LET, vec![expr::string(name), value])
}

fn thunk(body: ValueSpec) -> ValueSpec {
    expr::lambda(Vec::new(), vec![body])
}

/// A model with one function `test::<name>` per entry, over `Any[*]`
/// results; a body may call any of them through the ids handed to it.
fn model_with(
    functions: &[(&str, Vec<Parameter>)],
    bodies: impl FnOnce(&[ElementId]) -> Vec<Vec<ValueSpec>>,
) -> (PureModel, Vec<ElementId>) {
    let mut builder = ModelBuilder::new();
    let ids: Vec<ElementId> = functions
        .iter()
        .map(|(name, params)| {
            builder.function(
                &format!("test::{name}"),
                function(
                    name,
                    params.clone(),
                    named(ANY_ID),
                    Multiplicity::ZeroOrMany,
                    Vec::new(),
                ),
            )
        })
        .collect();
    for (id, body) in ids.iter().zip(bodies(&ids)) {
        let Element::Function(f) = builder.element_mut(*id) else {
            unreachable!("a function was added")
        };
        f.body = body.into();
    }
    (builder.finish(), ids)
}

/// Run `f` on a fresh evaluator per path, in one scope, and require the
/// same outcome. Returns it, with the evaluator of the executor path.
fn same_on_both<'m>(
    model: &'m PureModel,
    registry: &'m NativeRegistry,
    f: impl Fn(&mut Evaluator<'m>) -> Result<Value, PureException>,
) -> (Result<Value, String>, Evaluator<'m>) {
    let mut outcomes = Vec::new();
    let mut last = None;
    for exec in [false, true] {
        let mut ev = Evaluator::new(model, registry);
        ev.set_exec(exec);
        assert_eq!(ev.exec_enabled(), exec);
        ev.context_mut().push_scope();
        outcomes.push(f(&mut ev).map_err(|e| format!("{:?}", e.into_data())));
        last = Some(ev);
    }
    assert_eq!(outcomes[0], outcomes[1], "the two paths disagree");
    (
        outcomes.pop().expect("two outcomes"),
        last.expect("an evaluator"),
    )
}

fn built(ev: &Evaluator<'_>, id: ElementId) -> Rc<Body> {
    ev.built_function_body(id)
        .expect("the function's body was built")
}

/// The lambda bodies built under a body's lambda nodes, in node order.
fn lambdas(body: &Body) -> Vec<&Body> {
    body.nodes()
        .iter()
        .filter_map(|n| match &n.op {
            Op::Lambda(l) => l.compiled.built(),
            _ => None,
        })
        .collect()
}

fn ints(values: &[i64]) -> Value {
    Value::from_vec(values.iter().map(|&i| Value::Integer(i)).collect())
}

#[test]
fn lets_in_a_sequence_rebind_and_read_the_latest() {
    let (model, ids) = model_with(&[("seq", vec![int("a")])], |_| {
        vec![vec![
            let_("b", arith(PLUS, expr::var("a"), expr::int(1))),
            let_("a", arith(TIMES, expr::var("b"), expr::int(10))),
            let_("b", arith(PLUS, expr::var("a"), expr::var("b"))),
            expr::var("b"),
        ]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(2)])
    });
    assert_eq!(outcome, Ok(Value::Integer(33)));
    let body = built(&ev, ids[0]);
    assert_eq!(body.kind(), BodyKind::Function);
    assert_eq!(body.fallbacks(), 0);
    let names: Vec<&str> = body.names().iter().map(|(n, _)| n.as_str()).collect();
    assert_eq!(names, ["a", "b", "a", "b"]);
    assert_eq!(
        body.slots().iter().map(|s| s.kind).collect::<Vec<_>>(),
        [
            SlotKind::Parameter,
            SlotKind::Let,
            SlotKind::Let,
            SlotKind::Let
        ]
    );
}

#[test]
fn a_lambda_parameter_shadows_and_a_let_is_captured() {
    let (model, ids) = model_with(&[("shadow", vec![int("x")])], |_| {
        vec![vec![
            let_("y", expr::int(5)),
            expr::call(
                MAP,
                vec![
                    expr::collection(vec![expr::int(1), expr::int(2)]),
                    expr::lambda(
                        vec![int("x")],
                        vec![arith(PLUS, expr::var("x"), expr::var("y"))],
                    ),
                ],
            ),
        ]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(100)])
    });
    assert_eq!(outcome, Ok(ints(&[6, 7])));
    let body = built(&ev, ids[0]);
    assert_eq!(body.fallbacks(), 0);
    let inner = lambdas(&body);
    assert_eq!(inner.len(), 1, "the mapped lambda ran on its built body");
    assert_eq!(inner[0].kind(), BodyKind::Lambda);
    assert_eq!(inner[0].fallbacks(), 0);
    let captured: Vec<&str> = inner[0]
        .captures()
        .iter()
        .map(|&s| inner[0].slots()[s as usize].name.as_str())
        .collect();
    assert_eq!(captured, ["y"]);
}

#[test]
fn a_closure_keeps_what_it_captured_when_it_was_made() {
    let (model, ids) = model_with(&[("adder", vec![int("k")])], |_| {
        vec![vec![expr::lambda(
            vec![int("x")],
            vec![arith(PLUS, expr::var("x"), expr::var("k"))],
        )]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        let adder = ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(10)])?;
        ev.context_mut().set("k", Value::Integer(-1));
        ev.apply_callable(&adder, &[Value::Integer(1)])
    });
    assert_eq!(outcome, Ok(Value::Integer(11)));
    let body = built(&ev, ids[0]);
    assert_eq!(body.fallbacks(), 0);
    assert_eq!(lambdas(&body).len(), 1);
}

#[test]
fn recursion_through_if_runs_on_built_bodies() {
    let (model, ids) = model_with(&[("fact", vec![int("n")])], |ids| {
        let fact = ids[0];
        vec![vec![expr::call(
            IF,
            vec![
                expr::call(LESS_THAN_EQUAL, vec![expr::var("n"), expr::int(1)]),
                thunk(expr::int(1)),
                thunk(arith(
                    TIMES,
                    expr::var("n"),
                    expr::call_resolved(
                        fact,
                        "fact",
                        vec![arith(MINUS, expr::var("n"), expr::int(1))],
                    ),
                )),
            ],
        )]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(10)])
    });
    assert_eq!(outcome, Ok(Value::Integer(3_628_800)));
    let body = built(&ev, ids[0]);
    assert_eq!(body.fallbacks(), 0);
    let branches = lambdas(&body);
    assert_eq!(branches.len(), 2, "both branches ran on built bodies");
    assert!(branches.iter().all(|b| b.fallbacks() == 0));
    assert!(matches!(
        body.nodes()[body.statements()[0] as usize].op,
        Op::If { .. }
    ));
}

#[test]
fn and_or_force_the_right_operand_only_when_it_decides() {
    let fails = || {
        expr::call(
            EQUAL,
            vec![
                expr::call(MOD, vec![expr::int(1), expr::int(0)]),
                expr::int(0),
            ],
        )
    };
    let (model, ids) = model_with(&[("and_or", vec![int("x")])], |_| {
        vec![vec![expr::collection(vec![
            expr::call(AND, vec![expr::boolean(false), fails()]),
            expr::call(OR, vec![expr::boolean(true), fails()]),
            expr::call(
                AND,
                vec![
                    expr::call(LESS_THAN_EQUAL, vec![expr::var("x"), expr::int(3)]),
                    expr::boolean(true),
                ],
            ),
        ])]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(3)])
    });
    assert_eq!(
        outcome,
        Ok(Value::from_vec(vec![
            Value::Boolean(false),
            Value::Boolean(true),
            Value::Boolean(true)
        ]))
    );
    assert_eq!(built(&ev, ids[0]).fallbacks(), 0);

    let (outcome, _) = same_on_both(&model, &registry, |ev| {
        ev.eval(&expr::call(AND, vec![expr::boolean(true), fails()]))
    });
    let err = outcome.expect_err("mod by zero");
    assert!(
        err.contains("and_Boolean_1__Boolean_1__Boolean_1_"),
        "{err}"
    );
}

#[test]
fn an_error_carries_the_same_frames_on_both_paths() {
    let (model, ids) = model_with(
        &[("inner", vec![int("y")]), ("outer", vec![int("x")])],
        |ids| {
            vec![
                vec![
                    let_("z", expr::var("y")),
                    expr::call(MOD, vec![expr::var("z"), expr::int(0)]),
                ],
                vec![expr::call(
                    MAP,
                    vec![
                        expr::collection(vec![expr::var("x")]),
                        expr::lambda(
                            vec![int("e")],
                            vec![expr::call_resolved(ids[0], "inner", vec![expr::var("e")])],
                        ),
                    ],
                )],
            ]
        },
    );
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[1], &[Value::Integer(7)])
    });
    let err = outcome.expect_err("mod by zero");
    assert!(err.contains("DivisionByZero"), "{err}");
    assert!(err.contains("map_T_MANY__Function_1__V_MANY_"), "{err}");
    assert!(err.contains("\"inner\""), "{err}");
    assert_eq!(built(&ev, ids[0]).fallbacks(), 0);
    assert_eq!(built(&ev, ids[1]).fallbacks(), 0);
}

#[test]
fn a_native_reading_names_sees_parameters_lets_and_captures() {
    let (model, ids) = model_with(&[("names", vec![int("a")])], |_| {
        vec![vec![
            let_("b", arith(PLUS, expr::var("a"), expr::int(1))),
            expr::call(
                MAP,
                vec![
                    expr::collection(vec![expr::int(7)]),
                    expr::lambda(
                        vec![int("c")],
                        vec![let_("d", expr::var("b")), expr::call(VISIBLE, Vec::new())],
                    ),
                ],
            ),
        ]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(1)])
    });
    assert_eq!(
        outcome,
        Ok(Value::from_vec(
            [
                "a=Integer(1)",
                "b=Integer(2)",
                "c=Integer(7)",
                "d=Integer(2)"
            ]
            .into_iter()
            .map(|s| Value::String(s.into()))
            .collect()
        ))
    );
    let body = built(&ev, ids[0]);
    assert_eq!(body.fallbacks(), 0);
    assert_eq!(lambdas(&body)[0].fallbacks(), 0);
}

#[test]
fn a_let_evaluated_at_the_top_stays_bound() {
    let model = ModelBuilder::new().finish();
    let registry = registry();
    let (outcome, _) = same_on_both(&model, &registry, |ev| {
        ev.eval(&let_("x", expr::int(5)))?;
        ev.eval(&arith(TIMES, expr::var("x"), expr::var("x")))
    });
    assert_eq!(outcome, Ok(Value::Integer(25)));
}

#[test]
fn a_let_inside_an_expression_keeps_the_body_on_the_tree_walker() {
    let (model, ids) = model_with(&[("nested_let", vec![int("a")])], |_| {
        vec![vec![
            arith(PLUS, let_("t", expr::int(1)), expr::var("a")),
            expr::var("t"),
        ]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(1)])
    });
    assert_eq!(outcome, Ok(Value::Integer(1)));
    assert!(ev.built_function_body(ids[0]).is_none());
}

#[test]
fn a_closure_from_the_tree_walker_runs_on_the_tree_walker() {
    let model = ModelBuilder::new().finish();
    let registry = registry();
    let mut walker = Evaluator::new(&model, &registry);
    walker.set_exec(false);
    let made = walker
        .eval(&expr::lambda(vec![int("x")], vec![expr::var("x")]))
        .expect("a closure");
    let Value::Function(f) = &made else {
        panic!("a function value")
    };
    let FunctionValue::Lambda(closure) = &**f else {
        panic!("a lambda")
    };
    assert!(closure.exec.is_none());
    let mut ev = Evaluator::new(&model, &registry);
    ev.set_exec(true);
    assert_eq!(
        ev.apply_callable(&made, &[Value::Integer(4)])
            .expect("applies"),
        Value::Integer(4)
    );
}

#[derive(Debug, Default)]
struct Observing;

impl EvalHooks for Observing {
    fn before_eval(
        &mut self,
        _source: &SourceInfo,
        _context: &legend_pure_evaluator::context::VariableContext,
    ) -> bool {
        false
    }
    fn after_eval(&mut self, _source: &SourceInfo, _result: &Value) {}
    fn enter_function(&mut self, _name: &str, _source: &SourceInfo) {}
    fn leave_function(&mut self, _name: &str) {}
}

#[test]
fn hooks_that_observe_expressions_keep_the_tree_walker() {
    let model = ModelBuilder::new().finish();
    let registry = registry();
    let mut ev = Evaluator::with_hooks(&model, &registry, Observing);
    ev.set_exec(true);
    assert!(!ev.exec_enabled());
}

#[test]
fn a_property_read_carries_the_owner_the_call_recorded() {
    const NEW: &str = "new_Class_1__String_1__Any_MANY__T_1_";
    let mut builder = ModelBuilder::new();
    let person = builder.class(
        "my::Person",
        class(
            Vec::new(),
            vec![property("name", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let mut read = expr::prop(expr::var("p"), "name");
    if let ExprKind::PropertyCall(call) = &mut *read.kind {
        call.owner = Some(person);
    }
    let name_of = builder.function(
        "test::nameOf",
        function(
            "nameOf",
            vec![param("p", named(ANY_ID), Multiplicity::PureOne)],
            named(STRING_ID),
            Multiplicity::PureOne,
            vec![read],
        ),
    );
    let model = builder.finish();
    let mut registry = registry();
    registry.register(NEW, legend_pure_evaluator::native::lang::New);
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        let john = ev.eval(&expr::call(
            NEW,
            vec![
                expr::type_ref(named(person)),
                expr::string("p"),
                expr::collection(Vec::new()),
                expr::collection(Vec::new()),
                expr::string("name"),
                expr::string("John"),
                expr::boolean(false),
            ],
        ))?;
        ev.call_user_function_by_id_with_args(name_of, &[john])
    });
    assert_eq!(outcome, Ok(Value::String("John".into())));
    let body = built(&ev, name_of);
    assert_eq!(body.fallbacks(), 0);
    let owners: Vec<Option<ElementId>> = body
        .nodes()
        .iter()
        .filter_map(|n| match &n.op {
            Op::Property { owner, .. } => Some(*owner),
            _ => None,
        })
        .collect();
    assert_eq!(owners, [Some(person)]);
}

/// The operation names of a body's nodes, in node order.
fn ops(body: &Body) -> Vec<&'static str> {
    body.nodes()
        .iter()
        .map(|n| match &n.op {
            Op::Const(_) => "Const",
            Op::Local(_) => "Local",
            Op::Captured(_) => "Captured",
            Op::Let { .. } => "Let",
            Op::If { .. } => "If",
            Op::And { .. } => "And",
            Op::Or { .. } => "Or",
            Op::CallPure { .. } => "CallPure",
            Op::CallNative { .. } => "CallNative",
            Op::CallLazyNative { .. } => "CallLazyNative",
            Op::Property { .. } => "Property",
            Op::Lambda(_) => "Lambda",
            Op::Collection(_) => "Collection",
            Op::Fallback(_) => "Fallback",
        })
        .collect()
}

#[test]
fn eager_natives_take_forced_arguments_and_lazy_ones_their_expressions() {
    const SIZE: &str = "size_Any_MANY__Integer_1_";
    const FILTER: &str = "filter_T_MANY__Function_1__T_MANY_";
    const RANGE: &str = "range_Integer_1__Integer_1__Integer_1__Integer_MANY_";
    let (model, ids) = model_with(&[("evens", vec![int("n")])], |_| {
        vec![vec![expr::call(
            SIZE,
            vec![expr::call(
                FILTER,
                vec![
                    expr::call(RANGE, vec![expr::int(0), expr::var("n"), expr::int(1)]),
                    expr::lambda(
                        vec![int("x")],
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
        )]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(100)])
    });
    assert_eq!(outcome, Ok(Value::Integer(50)));
    let body = built(&ev, ids[0]);
    assert_eq!(
        ops(&body),
        [
            "Const",
            "Local",
            "Const",
            "CallNative",
            "Lambda",
            "CallNative",
            "CallNative"
        ]
    );
    let predicate = lambdas(&body);
    assert_eq!(
        ops(predicate[0]),
        ["Local", "Const", "CallNative", "Const", "CallNative"]
    );

    let (model, ids) = model_with(&[("sum", vec![int("a")])], |_| {
        vec![vec![arith(PLUS, expr::var("a"), expr::int(1))]]
    });
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(1)])
    });
    assert_eq!(outcome, Ok(Value::Integer(2)));
    assert_eq!(
        ops(&built(&ev, ids[0])),
        ["Local", "Const", "Collection", "CallLazyNative"],
        "plus folds its operands as it forces them: lazy"
    );
}

#[test]
fn an_eager_call_of_another_arity_stays_lazy_and_fails_as_before() {
    let (model, ids) = model_with(&[("bad", vec![int("a")])], |_| {
        vec![vec![expr::call(
            MOD,
            vec![expr::var("a"), expr::int(2), expr::int(3)],
        )]]
    });
    let registry = registry();
    let (outcome, ev) = same_on_both(&model, &registry, |ev| {
        ev.call_user_function_by_id_with_args(ids[0], &[Value::Integer(1)])
    });
    let err = outcome.expect_err("arity");
    assert!(err.contains("mod: expected 2 argument(s), got 3"), "{err}");
    assert!(ops(&built(&ev, ids[0])).contains(&"CallLazyNative"));
}

#[test]
fn an_error_forcing_an_eager_argument_carries_both_natives_frames() {
    const SIZE: &str = "size_Any_MANY__Integer_1_";
    let model = ModelBuilder::new().finish();
    let registry = registry();
    let (outcome, _) = same_on_both(&model, &registry, |ev| {
        ev.eval(&expr::call(
            SIZE,
            vec![expr::call(MOD, vec![expr::int(1), expr::int(0)])],
        ))
    });
    let err = outcome.expect_err("mod by zero");
    let size = err.find("size_Any_MANY__Integer_1_").expect("size's frame");
    let modulo = err
        .find("mod_Integer_1__Integer_1__Integer_1_")
        .expect("mod's frame");
    assert!(size < modulo, "outermost first: {err}");
}
