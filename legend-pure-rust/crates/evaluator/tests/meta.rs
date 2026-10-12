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

//! The meta natives, and `eval`, driven through a real `Evaluator` over
//! hand-built classes, enumerations and lambdas. The cases mirror the
//! platform PCT tests (`essential/lang/flow/match.pure`,
//! `essential/lang/eval/eval.pure`, …).

use legend_pure_evaluator::builders::on_both_paths;
use legend_pure_evaluator::builders::{ModelBuilder, class, expr, named, param, property, source};
use legend_pure_evaluator::error::{PureException, PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::{NativeRegistry, meta};
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{ANY_ID, DATE_ID, INTEGER_ID, NUMBER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::nodes::class::Constraint;
use legend_pure_metamodel::types::{
    ConstValue, DateValue, Multiplicity, PrimitiveType, TypeExpr, ValueSpec,
};

const NEW: &str = "new_Class_1__String_1__Any_MANY__T_1_";

/// The platform registry plus the flat key/value spelling of `new` these
/// tests build their instances with (the platform declares `new` over
/// `KeyExpression` objects; the builders have none).
fn registry() -> NativeRegistry {
    let mut registry = NativeRegistry::standard();
    registry.register(NEW, legend_pure_evaluator::native::lang::New);
    meta::register(&mut registry);
    registry
}

const MATCH: &str = "match_Any_MANY__Function_$1_MANY$__T_m_";
const MATCH_WITH: &str = "match_Any_MANY__Function_$1_MANY$__P_o__T_m_";
const LET: &str = "letFunction_String_1__T_m__T_m_";
const PLUS: &str = "plus_Number_MANY__Number_1_";
const EVAL_0: &str = "eval_Function_1__V_m_";
const EVAL_1: &str = "eval_Function_1__T_n__V_m_";
const EVAL_2: &str = "eval_Function_1__T_n__U_p__V_m_";
const EVAL_7: &str = "eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_";

fn evaluation_error(err: &PureException) -> Option<&str> {
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => Some(m),
        _ => None,
    }
}

/// The classes of `match.pure`: `Geo`, `MA_Address extends Geo`,
/// `MA_Location extends Geo`, `MA_Person`.
struct Geography {
    model: PureModel,
    address: ElementId,
    location: ElementId,
    person: ElementId,
}

fn geography() -> Geography {
    let mut b = ModelBuilder::new();
    let geo = b.class(
        "meta::pure::functions::lang::tests::match::Geo",
        class(Vec::new(), Vec::new()),
    );
    let address = b.class(
        "meta::pure::functions::lang::tests::match::MA_Address",
        class(
            vec![named(geo)],
            vec![property("name", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let location = b.class(
        "meta::pure::functions::lang::tests::match::MA_Location",
        class(
            vec![named(geo)],
            vec![property("place", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let person = b.class(
        "meta::pure::functions::lang::tests::match::MA_Person",
        class(
            Vec::new(),
            vec![property(
                "firstName",
                named(STRING_ID),
                Multiplicity::PureOne,
            )],
        ),
    );
    Geography {
        model: b.finish(),
        address,
        location,
        person,
    }
}

/// `^Class(key = value)` in the compiler-emitted positional shape.
fn new_instance(class: ElementId, key: &str, value: &str) -> ValueSpec {
    expr::call(
        NEW,
        vec![
            expr::type_ref(named(class)),
            expr::string("i"),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
            expr::string(key),
            expr::string(value),
            expr::boolean(false),
        ],
    )
}

/// `{name:Type[mult] | body}`.
fn branch(name: &str, ty: TypeExpr, mult: Multiplicity, body: ValueSpec) -> ValueSpec {
    expr::lambda(vec![param(name, ty, mult)], vec![body])
}

/// `{name:Type[mult], extra:String[extra_mult] | body}`.
fn branch_with(ty: TypeExpr, extra_mult: Multiplicity, body: ValueSpec) -> ValueSpec {
    expr::lambda(
        vec![
            param("a", ty, Multiplicity::PureOne),
            param("b", named(STRING_ID), extra_mult),
        ],
        vec![body],
    )
}

fn match_call(subject: ValueSpec, branches: Vec<ValueSpec>) -> ValueSpec {
    expr::call(MATCH, vec![subject, expr::collection(branches)])
}

fn strings(values: &[&str]) -> Value {
    Value::Collection(Box::new(
        values.iter().map(|s| Value::String((*s).into())).collect(),
    ))
}

fn ints(values: &[i64]) -> Value {
    Value::Collection(Box::new(
        values.iter().map(|n| Value::Integer(*n)).collect(),
    ))
}

#[test]
fn match_dispatches_an_instance_on_its_class_and_falls_back_to_any() {
    on_both_paths(|| {
        let g = geography();
        let registry = registry();
        let mut eval = Evaluator::new(&g.model, &registry);
        let branches = |any_mult: Multiplicity, any_label: &str| {
            vec![
                branch(
                    "a",
                    named(g.address),
                    Multiplicity::PureOne,
                    expr::string("address"),
                ),
                branch(
                    "l",
                    named(g.location),
                    Multiplicity::PureOne,
                    expr::string("location"),
                ),
                branch("a", named(ANY_ID), any_mult, expr::string(any_label)),
            ]
        };
        let one = |subject: ValueSpec| match_call(subject, branches(Multiplicity::PureOne, "Any1"));
        let many =
            |subject: ValueSpec| match_call(subject, branches(Multiplicity::ZeroOrMany, "Any*"));

        assert_eq!(
            eval.eval(&one(new_instance(g.address, "name", "Hoboken, NJ")))
                .unwrap(),
            Value::String("address".into())
        );
        assert_eq!(
            eval.eval(&one(new_instance(g.location, "place", "Hoboken, NJ")))
                .unwrap(),
            Value::String("location".into())
        );
        assert_eq!(
            eval.eval(&one(new_instance(g.person, "firstName", "Pierre")))
                .unwrap(),
            Value::String("Any1".into())
        );
        assert_eq!(
            eval.eval(&many(new_instance(g.person, "firstName", "Pierre")))
                .unwrap(),
            Value::String("Any*".into())
        );
        let two_people = expr::collection(vec![
            new_instance(g.person, "firstName", "Pierre"),
            new_instance(g.person, "firstName", "Fabrice"),
        ]);
        assert_eq!(
            eval.eval(&many(two_people)).unwrap(),
            Value::String("Any*".into())
        );
    });
}

#[test]
fn match_gates_each_branch_on_the_subject_count() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let up_to_four = Multiplicity::Range {
            lower: 1,
            upper: Some(4),
        };

        let three = expr::collection(vec![
            expr::string("w"),
            expr::string("w"),
            expr::string("w"),
        ]);
        let call = match_call(
            three,
            vec![
                branch(
                    "i",
                    named(INTEGER_ID),
                    up_to_four.clone(),
                    expr::string("z"),
                ),
                branch(
                    "s",
                    named(STRING_ID),
                    Multiplicity::ZeroOrMany,
                    expr::var("s"),
                ),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), strings(&["w", "w", "w"]));

        let call = match_call(
            expr::collection(vec![expr::int(1), expr::int(2), expr::int(3)]),
            vec![
                branch(
                    "i",
                    named(INTEGER_ID),
                    Multiplicity::PureOne,
                    expr::string("one"),
                ),
                branch(
                    "x",
                    named(INTEGER_ID),
                    Multiplicity::ZeroOrMany,
                    expr::var("x"),
                ),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), ints(&[1, 2, 3]));

        let call = match_call(
            expr::collection(vec![expr::int(1)]),
            vec![
                branch(
                    "i",
                    named(INTEGER_ID),
                    Multiplicity::ZeroOrOne,
                    expr::collection(vec![expr::int(1)]),
                ),
                branch(
                    "s",
                    named(STRING_ID),
                    Multiplicity::PureOne,
                    expr::string("address:"),
                ),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), Value::Integer(1));

        let call = match_call(
            expr::collection(Vec::new()),
            vec![
                branch("i", named(INTEGER_ID), up_to_four, expr::string("ints")),
                branch(
                    "s",
                    named(STRING_ID),
                    Multiplicity::Range {
                        lower: 0,
                        upper: Some(0),
                    },
                    expr::string("address"),
                ),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), Value::String("address".into()));
    });
}

#[test]
fn match_picks_the_branch_whose_type_fits_a_primitive_subject() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let branches = || {
            vec![
                branch("a", named(INTEGER_ID), Multiplicity::PureOne, expr::int(1)),
                branch(
                    "a",
                    named(STRING_ID),
                    Multiplicity::PureOne,
                    expr::collection(vec![expr::int(1), expr::int(2)]),
                ),
                branch(
                    "a",
                    named(DATE_ID),
                    Multiplicity::PureOne,
                    expr::collection(vec![expr::int(4), expr::int(5), expr::int(6)]),
                ),
            ]
        };
        assert_eq!(
            eval.eval(&match_call(expr::int(1), branches())).unwrap(),
            Value::Integer(1)
        );
        assert_eq!(
            eval.eval(&match_call(expr::string("1"), branches()))
                .unwrap(),
            ints(&[1, 2])
        );
        let year_2005 = expr::date(DateValue::StrictDate {
            year: 2005,
            month: None,
            day: None,
        });
        assert_eq!(
            eval.eval(&match_call(year_2005, branches())).unwrap(),
            ints(&[4, 5, 6])
        );
    });
}

#[test]
fn match_with_an_extra_parameter_passes_it_to_the_chosen_branch() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let call = expr::call(
            MATCH_WITH,
            vec![
                expr::collection(vec![expr::int(1)]),
                expr::collection(vec![
                    branch_with(named(INTEGER_ID), Multiplicity::PureOne, expr::var("b")),
                    branch_with(named(STRING_ID), Multiplicity::PureOne, expr::string("bad")),
                ]),
                expr::string("other"),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), Value::String("other".into()));

        let call = expr::call(
            MATCH_WITH,
            vec![
                expr::collection(vec![expr::int(1)]),
                expr::collection(vec![
                    branch_with(named(INTEGER_ID), Multiplicity::ZeroOrMany, expr::var("b")),
                    branch_with(
                        named(STRING_ID),
                        Multiplicity::ZeroOrMany,
                        expr::string("bad"),
                    ),
                ]),
                expr::collection(vec![expr::string("other"), expr::string("yo")]),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), strings(&["other", "yo"]));
    });
}

#[test]
fn match_without_a_fitting_branch_is_an_evaluation_error() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let call = match_call(
            expr::string("Digby"),
            vec![branch(
                "i",
                named(INTEGER_ID),
                Multiplicity::PureOne,
                expr::int(1),
            )],
        );
        let err = eval.eval(&call).unwrap_err();
        let msg = evaluation_error(&err).expect("an evaluation error");
        assert!(
            msg.starts_with("match: no branch matched for value"),
            "{msg}"
        );
    });
}

/// `$a + $b + …` over the named variables.
fn sum_of(names: &[&str]) -> ValueSpec {
    expr::call(
        PLUS,
        vec![expr::collection(
            names.iter().map(|n| expr::var(n)).collect(),
        )],
    )
}

fn integer_params(names: &[&str]) -> Vec<legend_pure_metamodel::types::Parameter> {
    names
        .iter()
        .map(|n| param(n, named(INTEGER_ID), Multiplicity::PureOne))
        .collect()
}

#[test]
fn eval_applies_a_lambda_to_its_arguments() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);

        let call = expr::call(EVAL_0, vec![expr::lambda(Vec::new(), vec![expr::int(7)])]);
        assert_eq!(eval.eval(&call).unwrap(), Value::Integer(7));

        eval.context_mut().push_scope();
        let add_41 = expr::call(
            PLUS,
            vec![expr::collection(vec![expr::var("a"), expr::int(41)])],
        );
        let body = [
            expr::call(
                LET,
                vec![
                    expr::string("z"),
                    expr::lambda(integer_params(&["a"]), vec![add_41]),
                ],
            ),
            expr::call(EVAL_1, vec![expr::var("z"), expr::int(1)]),
        ];
        assert_eq!(eval.eval_body(&body).unwrap(), Value::Integer(42));

        let call = expr::call(
            EVAL_2,
            vec![
                expr::lambda(integer_params(&["a", "b"]), vec![sum_of(&["a", "b"])]),
                expr::int(3),
                expr::int(2),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), Value::Integer(5));

        let names = ["a", "b", "c", "d", "e", "f", "g"];
        let mut args = vec![expr::lambda(integer_params(&names), vec![sum_of(&names)])];
        args.extend((1..=7).map(expr::int));
        assert_eq!(
            eval.eval(&expr::call(EVAL_7, args)).unwrap(),
            Value::Integer(28)
        );
    });
}

#[test]
fn eval_without_a_function_is_an_evaluation_error() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let err = eval.eval(&expr::call(EVAL_0, Vec::new())).unwrap_err();
        assert_eq!(
            evaluation_error(&err),
            Some("eval: expected at least 1 argument (the function)")
        );
    });
}

const EXTRACT_ENUM_VALUE: &str = "extractEnumValue_Enumeration_1__String_1__T_1_";
const EQUAL: &str = "equal_Any_MANY__Any_MANY__Boolean_1_";

#[test]
fn extract_enum_value_yields_the_literal_enum_value() {
    on_both_paths(|| {
        let mut b = ModelBuilder::new();
        let colour = b.enumeration("my::Colour", &["RED", "GREEN"]);
        let model = b.finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let extract = |name: &str| {
            expr::call(
                EXTRACT_ENUM_VALUE,
                vec![expr::element_ref(colour), expr::string(name)],
            )
        };
        assert_eq!(
            eval.eval(&extract("GREEN")).unwrap(),
            Value::EnumValue {
                enum_id: colour,
                member: "GREEN".into(),
            }
        );
        let same = expr::call(
            EQUAL,
            vec![extract("GREEN"), expr::enum_value(colour, "GREEN")],
        );
        assert_eq!(eval.eval(&same).unwrap(), Value::Boolean(true));

        let err = eval.eval(&extract("BLUE")).unwrap_err();
        assert_eq!(
            evaluation_error(&err),
            Some("extractEnumValue: enumeration has no value 'BLUE'")
        );
    });
}

#[test]
fn extract_enum_value_requires_an_enumeration() {
    on_both_paths(|| {
        let mut b = ModelBuilder::new();
        let thing = b.class("my::Thing", class(Vec::new(), Vec::new()));
        let model = b.finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let call = expr::call(
            EXTRACT_ENUM_VALUE,
            vec![expr::element_ref(thing), expr::string("RED")],
        );
        let err = eval.eval(&call).unwrap_err();
        assert_eq!(
            evaluation_error(&err),
            Some("extractEnumValue: first argument must reference an Enumeration")
        );
    });
}

const CAST: &str = "cast_Any_m__T_1__T_m_";
const EQ: &str = "eq_Any_1__Any_1__Boolean_1_";

/// `@Type`, optionally with type-variable values (`@P(8)`).
fn type_ref_with(element: ElementId, value_arguments: Vec<ConstValue>) -> ValueSpec {
    expr::type_ref(TypeExpr::Named {
        element,
        source_info: None,
        type_arguments: Vec::new(),
        multiplicity_arguments: Vec::new(),
        value_arguments,
    })
}

fn cast(subject: ValueSpec, target: ElementId) -> ValueSpec {
    expr::call(CAST, vec![subject, expr::type_ref(named(target))])
}

/// `Primitive <path>(params) extends Integer [ name: $this == <rhs> ]`.
fn integer_extension(
    b: &mut ModelBuilder,
    path: &str,
    params: Vec<legend_pure_metamodel::types::Parameter>,
    rhs: ValueSpec,
) -> ElementId {
    b.add(
        path,
        Element::PrimitiveType(PrimitiveType {
            super_type: Some(INTEGER_ID),
            super_type_value_arguments: Vec::new(),
            type_variable_parameters: params,
            constraints: vec![Constraint {
                name: Some("matches".into()),
                source_info: source(),
                function: expr::call(EQ, vec![expr::var("this"), rhs]),
                enforcement_level: None,
                external_id: None,
                message: None,
            }],
        }),
    )
}

#[test]
fn cast_returns_the_value_when_its_type_fits() {
    on_both_paths(|| {
        let g = geography();
        let registry = registry();
        let mut eval = Evaluator::new(&g.model, &registry);
        assert_eq!(
            eval.eval(&cast(expr::int(1), INTEGER_ID)).unwrap(),
            Value::Integer(1)
        );
        assert_eq!(
            eval.eval(&cast(expr::int(1), NUMBER_ID)).unwrap(),
            Value::Integer(1)
        );
        let geo = g
            .model
            .resolve_fqn_str("meta::pure::functions::lang::tests::match::Geo")
            .unwrap();
        let address = eval
            .eval(&cast(new_instance(g.address, "name", "Hoboken, NJ"), geo))
            .unwrap();
        assert!(matches!(address, Value::Object(_)));
        let empty = eval
            .eval(&cast(expr::collection(Vec::new()), STRING_ID))
            .unwrap();
        assert!(empty.is_empty(), "{empty:?}");
    });
}

#[test]
fn cast_to_an_unrelated_type_carries_the_java_message() {
    on_both_paths(|| {
        let g = geography();
        let registry = registry();
        let mut eval = Evaluator::new(&g.model, &registry);
        let err = eval.eval(&cast(expr::string("a"), INTEGER_ID)).unwrap_err();
        assert_eq!(
            evaluation_error(&err),
            Some("Cast exception: String cannot be cast to Integer")
        );
        let err = eval
            .eval(&cast(
                new_instance(g.person, "firstName", "Pierre"),
                g.address,
            ))
            .unwrap_err();
        assert_eq!(
            evaluation_error(&err),
            Some("Cast exception: Object cannot be cast to MA_Address")
        );
    });
}

#[test]
fn an_empty_cast_feeds_match_its_zero_branch() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let call = match_call(
            cast(expr::collection(Vec::new()), STRING_ID),
            vec![
                branch(
                    "i",
                    named(INTEGER_ID),
                    Multiplicity::Range {
                        lower: 1,
                        upper: Some(4),
                    },
                    expr::string("ints"),
                ),
                branch(
                    "s",
                    named(STRING_ID),
                    Multiplicity::Range {
                        lower: 0,
                        upper: Some(0),
                    },
                    expr::string("address"),
                ),
            ],
        );
        assert_eq!(eval.eval(&call).unwrap(), Value::String("address".into()));
    });
}

#[test]
fn cast_to_a_primitive_extension_checks_its_constraints() {
    on_both_paths(|| {
        let mut b = ModelBuilder::new();
        let one = integer_extension(&mut b, "my::One", Vec::new(), expr::int(1));
        let p = integer_extension(
            &mut b,
            "my::P",
            vec![param("x", named(INTEGER_ID), Multiplicity::PureOne)],
            expr::var("x"),
        );
        let model = b.finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);

        assert_eq!(
            eval.eval(&cast(expr::int(1), one)).unwrap(),
            Value::Integer(1)
        );
        let err = eval.eval(&cast(expr::int(2), one)).unwrap_err();
        assert!(
            matches!(
                &err.kind,
                PureExceptionKind::ConstraintViolation { constraint_id, owner, .. }
                    if constraint_id == "matches" && owner == "One"
            ),
            "{err:?}"
        );

        let cast_p = |n: i64| {
            expr::call(
                CAST,
                vec![expr::int(n), type_ref_with(p, vec![ConstValue::Integer(8)])],
            )
        };
        assert_eq!(eval.eval(&cast_p(8)).unwrap(), Value::Integer(8));
        assert!(matches!(
            eval.eval(&cast_p(7)).unwrap_err().kind,
            PureExceptionKind::ConstraintViolation { .. }
        ));
    });
}

#[test]
fn cast_to_a_relation_type_is_unsupported() {
    on_both_paths(|| {
        let g = geography();
        let registry = registry();
        let mut eval = Evaluator::new(&g.model, &registry);
        let call = expr::call(
            CAST,
            vec![expr::int(1), new_instance(g.person, "firstName", "Pierre")],
        );
        let err = eval.eval(&call).unwrap_err();
        assert!(
            matches!(
                &err.kind,
                PureExceptionKind::ExecutionError(PureRuntimeError::Unsupported(m))
                    if m.contains("relation type")
            ),
            "{err:?}"
        );
    });
}

/// `genericType` of a value is a `GenericType` object whose `rawType` is
/// the value's type, so `type()` can read it.
#[test]
fn generic_type_of_a_value_carries_its_raw_type() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let generic = eval
            .eval(&expr::call(
                "genericType_Any_MANY__GenericType_1_",
                vec![expr::int(1)],
            ))
            .unwrap();
        eval.context_mut().push_scope();
        eval.context_mut().set("g", generic);
        let raw = eval.eval(&expr::prop(expr::var("g"), "rawType")).unwrap();
        assert_eq!(raw, Value::Element(INTEGER_ID));
    });
}

/// Keys registered before their implementation is fished fail with
/// `NotImplemented` naming the key.
#[test]
fn not_yet_meta_natives_fail_with_not_implemented() {
    on_both_paths(|| {
        let model = ModelBuilder::new().finish();
        let registry = registry();
        let mut eval = Evaluator::new(&model, &registry);
        let key = "deactivate_Any_MANY__ValueSpecification_1_";
        let err = eval.eval(&expr::call(key, vec![expr::int(1)])).unwrap_err();
        assert!(
            matches!(
                &err.kind,
                PureExceptionKind::ExecutionError(PureRuntimeError::NotImplemented(k))
                    if k == key
            ),
            "{key}: {err:?}"
        );
    });
}

#[test]
fn the_package_metaclass_lives_at_the_root_as_m3_declares_it() {
    on_both_paths(|| {
        let model = PureModel::bootstrapped();
        let package_class = model.resolve_fqn_str("Package").expect("the Package class");
        assert!(matches!(
            model.get_element(package_class),
            Element::Class(_)
        ));
    });
}
