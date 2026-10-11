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

//! The string natives that need a real evaluator (heap objects, the
//! model, qualified properties), driven through hand-built nodes. The
//! literal-only cases stay inline in `native/string.rs` over `MockCtx`.

use legend_pure_evaluator::builders::{
    ModelBuilder, class, expr, named, property, qualified_property,
};
use legend_pure_evaluator::error::{PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{ANY_ID, INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{DateValue, Multiplicity, ValueSpec};

const NEW: &str = "new_Class_1__String_1__Any_MANY__T_1_";

/// The platform registry plus the flat key/value spelling of `new` these
/// tests build their instances with (the platform declares `new` over
/// `KeyExpression` objects; the builders have none).
fn registry() -> NativeRegistry {
    let mut registry = NativeRegistry::standard();
    registry.register(NEW, legend_pure_evaluator::native::lang::New);
    registry
}
const TO_STRING: &str = "toString_Any_1__String_1_";
const FORMAT: &str = "format_String_1__Any_MANY__String_1_";
const JOIN: &str = "joinStrings_String_MANY__String_1__String_1__String_1__String_1_";
const JOIN_AFFIXED: &str = "joinStrings_String_MANY__String_1__String_1__String_1__String_1_";
const STARTS_WITH: &str = "startsWith_String_1__String_1__Boolean_1_";

/// The elements `toString` is exercised against.
struct Fixture {
    model: PureModel,
    /// `my::Person { name: String[1] }` — no `toString()`.
    person: ElementId,
    /// `my::Named { name: String[1]; toString() { $this.name }: String[1] }`.
    named_class: ElementId,
    /// `my::Child extends my::Named` — inherits `toString()`.
    child: ElementId,
    /// `my::Malformed { name: String[1]; toString() { 1 }: Integer[1] }`.
    malformed: ElementId,
    /// `my::Kind { CITY, COUNTRY }`.
    kind: ElementId,
}

fn fixture() -> Fixture {
    let mut b = ModelBuilder::new();
    let name = || property("name", named(STRING_ID), Multiplicity::PureOne);
    let person = b.class("my::Person", class(Vec::new(), vec![name()]));
    let mut named_def = class(Vec::new(), vec![name()]);
    named_def.qualified_properties.push(qualified_property(
        "toString",
        Vec::new(),
        named(STRING_ID),
        Multiplicity::PureOne,
        vec![expr::prop(expr::var("this"), "name")],
    ));
    let named_class = b.class("my::Named", named_def);
    let child = b.class("my::Child", class(vec![named(named_class)], Vec::new()));
    let mut malformed_def = class(Vec::new(), vec![name()]);
    malformed_def.qualified_properties.push(qualified_property(
        "toString",
        Vec::new(),
        named(INTEGER_ID),
        Multiplicity::PureOne,
        vec![expr::int(1)],
    ));
    let malformed = b.class("my::Malformed", malformed_def);
    let kind = b.enumeration("my::Kind", &["CITY", "COUNTRY"]);
    Fixture {
        model: b.finish(),
        person,
        named_class,
        child,
        malformed,
        kind,
    }
}

/// The compiler-emitted shape of `^Class(name=$name)`.
fn new_named(class: ElementId, name: &str) -> ValueSpec {
    expr::call(
        NEW,
        vec![
            expr::type_ref(named(class)),
            expr::string(""),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
            expr::string("name"),
            expr::string(name),
            expr::boolean(false),
        ],
    )
}

fn to_string(arg: ValueSpec) -> ValueSpec {
    expr::call(TO_STRING, vec![arg])
}

/// Evaluate `spec` over `model` with the standard registry and unwrap
/// the string it must produce.
fn string(model: &PureModel, spec: &ValueSpec) -> String {
    let registry = registry();
    let mut eval = Evaluator::new(model, &registry);
    match &eval.eval(spec).unwrap() {
        Value::String(s) => s.to_string(),
        other => panic!("expected a string, got {other:?}"),
    }
}

/// Evaluate `toString(arg)` and unwrap the string.
fn render(model: &PureModel, arg: ValueSpec) -> String {
    string(model, &to_string(arg))
}

/// The message of the `EvaluationError` evaluating `spec` must raise.
fn evaluation_error(model: &PureModel, spec: &ValueSpec) -> String {
    let registry = registry();
    let mut eval = Evaluator::new(model, &registry);
    let err = eval.eval(spec).expect_err("evaluation must fail");
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => m.clone(),
        other => panic!("expected an evaluation error, got {other:?}"),
    }
}

fn date_time(
    (year, month, day): (i16, i8, i8),
    (hour, minute, second): (i8, i8, i8),
    subsecond_nanos: i32,
    subsecond_digits: u8,
    tz_offset_minutes: Option<i16>,
) -> ValueSpec {
    expr::date(DateValue::DateTime {
        year,
        month,
        day,
        hour,
        minute,
        second,
        subsecond_nanos,
        subsecond_digits,
        has_minutes: true,
        has_seconds: true,
        tz_offset_minutes,
    })
}

#[test]
fn to_string_renders_primitives_in_the_java_shape() {
    let model = ModelBuilder::new().finish();
    assert_eq!(render(&model, expr::int(1)), "1");
    assert_eq!(render(&model, expr::int(-1)), "-1");
    assert_eq!(render(&model, expr::boolean(true)), "true");
    assert_eq!(render(&model, expr::string("")), "");
    assert_eq!(render(&model, expr::string("the quick")), "the quick");
    assert_eq!(
        render(&model, expr::decimal(rust_decimal::Decimal::new(314, 2))),
        "3.14"
    );
}

/// `testFloatToString*`: integer-valued floats keep their `.0`, no
/// E-notation either way.
#[test]
fn to_string_renders_floats_like_java_double_to_string() {
    let model = ModelBuilder::new().finish();
    assert_eq!(render(&model, expr::float(2.0)), "2.0");
    assert_eq!(render(&model, expr::float(3.25)), "3.25");
    assert_eq!(render(&model, expr::float(0.01)), "0.01");
    assert_eq!(render(&model, expr::float(134_210_000.0)), "134210000.0");
    assert_eq!(
        render(&model, expr::float(0.000_000_013_421)),
        "0.000000013421"
    );
}

/// A collection renders bracketed, comma-separated; a nested collection
/// literal flattens on evaluation, as Pure collections do.
#[test]
fn to_string_renders_a_collection_bracketed() {
    let model = ModelBuilder::new().finish();
    let coll = expr::collection(vec![
        expr::int(1),
        expr::string("a"),
        expr::float(2.0),
        expr::collection(vec![expr::boolean(false)]),
    ]);
    assert_eq!(render(&model, coll), "[1, a, 2.0, false]");
}

/// `testDateToString`, `testDateTimeToString`, `testSimpleDateToString`:
/// the Pure date text without the `%`, date-times in UTC with `+0000`.
#[test]
fn to_string_renders_dates_as_pure_date_text() {
    let model = ModelBuilder::new().finish();
    let strict = |year, month, day| expr::date(DateValue::StrictDate { year, month, day });
    assert_eq!(render(&model, strict(2014, Some(1), Some(1))), "2014-01-01");
    assert_eq!(render(&model, strict(2014, Some(1), None)), "2014-01");
    assert_eq!(render(&model, strict(2014, None, None)), "2014");
    assert_eq!(
        render(&model, date_time((2014, 1, 1), (0, 0, 0), 0, 3, None)),
        "2014-01-01T00:00:00.000+0000"
    );
    assert_eq!(
        render(
            &model,
            date_time((2014, 1, 1), (20, 54, 27), 352_000_000, 3, Some(-300))
        ),
        "2014-01-02T01:54:27.352+0000"
    );
}

/// `testEnumerationToString`, `testClassToString`: an enum value prints
/// its member name, an element its simple name.
#[test]
fn to_string_renders_enum_values_and_elements_by_name() {
    let f = fixture();
    assert_eq!(render(&f.model, expr::enum_value(f.kind, "CITY")), "CITY");
    assert_eq!(render(&f.model, expr::element_ref(f.kind)), "Kind");
    assert_eq!(render(&f.model, expr::element_ref(f.person)), "Person");
}

/// `testPersonToString`: an instance whose class declares no
/// `toString()` falls back to Java's anonymous-instance name.
#[test]
fn to_string_of_an_instance_without_to_string_is_anonymous() {
    let f = fixture();
    let s = render(&f.model, new_named(f.person, "Pierre"));
    assert!(s.starts_with("Anonymous_"), "{s}");
}

/// `testPersonToString`'s lambda: `$str->startsWith('Anonymous_')`.
#[test]
fn starts_with_on_the_anonymous_rendering() {
    let f = fixture();
    let spec = expr::call(
        STARTS_WITH,
        vec![
            to_string(new_named(f.person, "Pierre")),
            expr::string("Anonymous_"),
        ],
    );
    let registry = registry();
    let mut eval = Evaluator::new(&f.model, &registry);
    assert_eq!(eval.eval(&spec).unwrap(), Value::Boolean(true));
}

/// `testComplexClassToString`: a class's own `toString()` qualified
/// property is what `toString` returns.
#[test]
fn to_string_dispatches_to_the_class_to_string_qualified_property() {
    let f = fixture();
    assert_eq!(render(&f.model, new_named(f.named_class, "Ann")), "Ann");
}

#[test]
fn to_string_finds_the_qualified_property_on_a_generalization() {
    let f = fixture();
    assert_eq!(render(&f.model, new_named(f.child, "Bob")), "Bob");
}

/// A `toString()` not declared `String[1]` is not honoured.
#[test]
fn to_string_skips_a_qualified_property_not_declared_string_one() {
    let f = fixture();
    let s = render(&f.model, new_named(f.malformed, "Eve"));
    assert!(s.starts_with("Anonymous_"), "{s}");
}

#[test]
fn to_string_renders_instances_inside_a_collection() {
    let f = fixture();
    let coll = expr::collection(vec![
        new_named(f.named_class, "Ann"),
        expr::int(7),
        new_named(f.child, "Bob"),
    ]);
    assert_eq!(render(&f.model, coll), "[Ann, 7, Bob]");
}

/// `format(template, args)`.
fn format(template: &str, args: Vec<ValueSpec>) -> ValueSpec {
    expr::call(FORMAT, vec![expr::string(template), expr::collection(args)])
}

const FOX: &str = "the quick brown %s jumps over the lazy";

/// `format('the quick brown %s jumps over the lazy <spec>', ['fox', arg])`.
fn fox(spec: &str, arg: ValueSpec) -> ValueSpec {
    format(&format!("{FOX} {spec}"), vec![expr::string("fox"), arg])
}

/// The expected rendering of [`fox`].
fn fox_says(tail: &str) -> String {
    format!("{} {tail}", FOX.replace("%s", "fox"))
}

/// `testFormatString`, `testFormatBoolean`, `testFormatInteger*`.
#[test]
fn format_substitutes_strings_booleans_and_integers() {
    let model = ModelBuilder::new().finish();
    assert_eq!(
        string(&model, &fox("%s", expr::string("dog"))),
        fox_says("dog")
    );
    assert_eq!(
        string(&model, &fox("%s", expr::boolean(false))),
        fox_says("false")
    );
    assert_eq!(string(&model, &fox("%d", expr::int(-3))), fox_says("-3"));
    assert_eq!(
        string(&model, &fox("%05d", expr::int(3))),
        fox_says("00003")
    );
    assert_eq!(
        string(&model, &fox("%05d", expr::int(-3))),
        fox_says("-00003")
    );
}

/// `testFormatFloat*`: `%f` plain, zero-padded precision, truncation and
/// rounding.
#[test]
fn format_renders_floats_with_and_without_precision() {
    let model = ModelBuilder::new().finish();
    let cases = [
        ("%f", 1.5, "1.5"),
        ("%f", -1.5, "-1.5"),
        ("%.4f", 1.5, "1.5000"),
        ("%.1f", -1.11, "-1.1"),
        ("%.2f", 1.338, "1.34"),
        ("%.2f", 9.999, "10.00"),
        ("%.2f", -9.999, "-10.00"),
        ("%.2f", 0.0553, "0.06"),
        ("%.2f", -0.000_553, "-0.00"),
    ];
    for (spec, value, expected) in cases {
        assert_eq!(
            string(&model, &fox(spec, expr::float(value))),
            fox_says(expected),
            "{spec} of {value}"
        );
    }
}

/// `testSimpleFormatDate`, `testFormatRepr`: `%s` and `%t` render a date
/// as its Pure text, `%r` adds the `%`; `%r` quotes and escapes a string.
#[test]
fn format_renders_dates_and_representations() {
    let model = ModelBuilder::new().finish();
    let midnight = || date_time((2014, 1, 1), (0, 0, 0), 0, 3, None);
    assert_eq!(
        string(&model, &fox("%s", midnight())),
        fox_says("2014-01-01T00:00:00.000+0000")
    );
    assert_eq!(
        string(&model, &fox("%t", midnight())),
        fox_says("2014-01-01T00:00:00.000+0000")
    );
    assert_eq!(
        string(&model, &fox("%r", midnight())),
        fox_says("%2014-01-01T00:00:00.000+0000")
    );
    assert_eq!(
        string(&model, &fox("%r", expr::string("dog \\ 'dog'"))),
        fox_says("'dog \\\\ \\'dog\\''")
    );
}

/// `testFormatDate`: `%t{pattern}` with the SimpleDateFormat subset and a
/// `[zone]` prefix; a single date is accepted where `Any[*]` is declared.
#[test]
fn format_renders_date_patterns() {
    let model = ModelBuilder::new().finish();
    let on = |pattern: &str, date: ValueSpec| {
        expr::call(
            FORMAT,
            vec![expr::string(&format!("on %t{{{pattern}}}")), date],
        )
    };
    let strict = expr::date(DateValue::StrictDate {
        year: 2014,
        month: Some(3),
        day: Some(10),
    });
    let afternoon = || date_time((2014, 3, 10), (13, 7, 44), 1_000_000, 3, None);
    assert_eq!(string(&model, &on("yyyy-MM-dd", strict)), "on 2014-03-10");
    let cases = [
        ("yyyy-MM-dd h:mm:ssa", "on 2014-03-10 1:07:44PM"),
        ("yyyy-MM-dd hh:mm:ssa", "on 2014-03-10 01:07:44PM"),
        (
            "yyyy-MM-dd\"T\"HH:mm:ss.SSSZ",
            "on 2014-03-10T13:07:44.001+0000",
        ),
        ("yyyy-MM-dd HH:mm:ss.SSSX", "on 2014-03-10 13:07:44.001Z"),
        (
            "[EST]yyyy-MM-dd HH:mm:ss.SSSZ",
            "on 2014-03-10 08:07:44.001-0500",
        ),
        (
            "[CET]yyyy-MM-dd HH:mm:ss.SSSZ",
            "on 2014-03-10 14:07:44.001+0100",
        ),
    ];
    for (pattern, expected) in cases {
        assert_eq!(string(&model, &on(pattern, afternoon())), expected);
    }
}

/// `%s` on an instance goes through its class's `toString()`.
#[test]
fn format_s_on_an_instance_uses_its_to_string() {
    let f = fixture();
    let spec = format(
        "hello %s and %s",
        vec![new_named(f.named_class, "Ann"), new_named(f.child, "Bob")],
    );
    assert_eq!(string(&f.model, &spec), "hello Ann and Bob");
}

/// `testFormatTooFewInputs`, `testFormatTooManyInputs`: Java's texts.
#[test]
fn format_rejects_too_few_and_too_many_arguments() {
    let model = ModelBuilder::new().finish();
    let few = format("Hello %s %s", vec![expr::string("Catherine")]);
    assert_eq!(
        evaluation_error(&model, &few),
        "Too few arguments passed to format function. Format expression \"Hello %s %s\", number of arguments [1]"
    );
    let many = format(
        "Hello %s %s",
        vec![
            expr::string("Catherine"),
            expr::string("Joe"),
            expr::string("Katie"),
        ],
    );
    assert_eq!(
        evaluation_error(&model, &many),
        "Unused format args. [3] arguments provided to expression \"Hello %s %s\""
    );
}

fn strings(values: &[&str]) -> ValueSpec {
    expr::collection(values.iter().map(|s| expr::string(s)).collect())
}

/// `testJoinStrings*`: both overloads, one string, none.
#[test]
fn join_strings_with_and_without_prefix_and_suffix() {
    let model = ModelBuilder::new().finish();
    let join = |values: &[&str]| {
        expr::call(
            JOIN,
            vec![
                strings(values),
                expr::string(""),
                expr::string(","),
                expr::string(""),
            ],
        )
    };
    let affixed = |values: &[&str]| {
        expr::call(
            JOIN_AFFIXED,
            vec![
                strings(values),
                expr::string("["),
                expr::string(","),
                expr::string("]"),
            ],
        )
    };
    assert_eq!(string(&model, &join(&["a", "b", "c"])), "a,b,c");
    assert_eq!(string(&model, &affixed(&["a", "b", "c"])), "[a,b,c]");
    assert_eq!(string(&model, &join(&["a"])), "a");
    assert_eq!(string(&model, &affixed(&["a"])), "[a]");
    assert_eq!(string(&model, &join(&[])), "");
    assert_eq!(string(&model, &affixed(&[])), "[]");
}

/// The platform's `Pair` with its `toString()`:
/// `['<', $this.first->toString(), ', ', $this.second->toString(), '>']->joinStrings('', '', '')`.
fn pair_model() -> (PureModel, ElementId) {
    let mut b = ModelBuilder::new();
    let mut pair = class(
        Vec::new(),
        vec![
            property("first", named(ANY_ID), Multiplicity::PureOne),
            property("second", named(ANY_ID), Multiplicity::PureOne),
        ],
    );
    let part = |p: &str| expr::call(TO_STRING, vec![expr::prop(expr::var("this"), p)]);
    pair.qualified_properties.push(qualified_property(
        "toString",
        Vec::new(),
        named(STRING_ID),
        Multiplicity::PureOne,
        vec![expr::call(
            JOIN_AFFIXED,
            vec![
                expr::collection(vec![
                    expr::string("<"),
                    part("first"),
                    expr::string(", "),
                    part("second"),
                    expr::string(">"),
                ]),
                expr::string(""),
                expr::string(""),
                expr::string(""),
            ],
        )],
    ));
    let id = b.class("my::Pair", pair);
    (b.finish(), id)
}

/// `^Pair(first=$first, second=$second)`.
fn new_pair(pair: ElementId, first: ValueSpec, second: ValueSpec) -> ValueSpec {
    expr::call(
        NEW,
        vec![
            expr::type_ref(named(pair)),
            expr::string(""),
            expr::collection(Vec::new()),
            expr::collection(Vec::new()),
            expr::string("first"),
            first,
            expr::boolean(false),
            expr::string("second"),
            second,
            expr::boolean(false),
        ],
    )
}

/// `testPairToString`, `testFormatPair`: `toString` and `%s` on a pair
/// run its `toString()`, which recurses through `toString` and
/// `joinStrings`.
#[test]
fn pair_to_string_runs_through_to_string_and_join_strings() {
    let (model, pair) = pair_model();
    let ab = new_pair(pair, expr::string("a"), expr::string("b"));
    assert_eq!(render(&model, ab), "<a, b>");
    let a1 = new_pair(pair, expr::string("a"), expr::int(1));
    assert_eq!(render(&model, a1), "<a, 1>");
    let nested = new_pair(
        pair,
        expr::string("dog"),
        new_pair(pair, expr::string("cat"), expr::string("mouse")),
    );
    assert_eq!(
        string(&model, &fox("%s", nested)),
        fox_says("<dog, <cat, mouse>>")
    );
}
