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

//! `ModelBuilder` round-trips: every element kind lands in its package,
//! resolves by path, and feeds the derived indexes the evaluator reads.

use legend_pure_evaluator::builders::{ModelBuilder, class, expr, named, property};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::{INTEGER_ID, STRING_ID};
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{DSLInstance, Element};
use legend_pure_metamodel::nodes::measure::Measure;
use legend_pure_metamodel::nodes::unit::Unit;
use legend_pure_metamodel::types::Multiplicity;

#[test]
fn class_registers_in_its_package_and_in_specializations() {
    let mut b = ModelBuilder::new();
    let person = b.class(
        "my::model::Person",
        class(
            Vec::new(),
            vec![property("name", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let employee = b.class(
        "my::model::Employee",
        class(
            vec![named(person)],
            vec![property(
                "salary",
                named(INTEGER_ID),
                Multiplicity::ZeroOrOne,
            )],
        ),
    );
    let model = b.finish();

    assert_eq!(model.resolve_fqn_str("my::model::Person"), Some(person));
    assert_eq!(model.resolve_fqn_str("my::model::Employee"), Some(employee));
    assert_eq!(model.element_name(person), "Person");
    assert!(matches!(model.get_element(person), Element::Class(c) if c.properties.len() == 1));
    assert_eq!(model.specializations(person), &[employee]);
    assert!(model.specializations(employee).is_empty());
}

#[test]
fn enumeration_round_trips_and_evaluates_to_an_enum_value() {
    let mut b = ModelBuilder::new();
    let colour = b.enumeration("my::Colour", &["RED", "GREEN"]);
    let model = b.finish();
    assert_eq!(model.resolve_fqn_str("my::Colour"), Some(colour));
    let Element::Enumeration(e) = model.get_element(colour) else {
        panic!("not an enumeration");
    };
    assert_eq!(
        e.values.iter().map(|v| v.name.as_str()).collect::<Vec<_>>(),
        ["RED", "GREEN"]
    );

    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::enum_value(colour, "RED")).unwrap(),
        Value::EnumValue {
            enum_id: colour,
            member: "RED".into()
        }
    );
}

#[test]
fn association_feeds_the_association_properties_index_for_both_ends() {
    let mut b = ModelBuilder::new();
    let person = b.class("my::Person", class(Vec::new(), Vec::new()));
    let firm = b.class("my::Firm", class(Vec::new(), Vec::new()));
    let assoc = b.association(
        "my::Employment",
        [
            property("employees", named(person), Multiplicity::ZeroOrMany),
            property("firm", named(firm), Multiplicity::ZeroOrOne),
        ],
    );
    let model = b.finish();
    assert_eq!(model.resolve_fqn_str("my::Employment"), Some(assoc));
    assert_eq!(model.association_properties(person), &[(assoc, 0)]);
    assert_eq!(model.association_properties(firm), &[(assoc, 1)]);
}

#[test]
fn profile_round_trips_with_its_stereotypes_and_tags() {
    let mut b = ModelBuilder::new();
    let profile = b.profile("my::doc", &["deprecated"], &["doc"]);
    let model = b.finish();
    assert_eq!(model.resolve_fqn_str("my::doc"), Some(profile));
    let Element::Profile(p) = model.get_element(profile) else {
        panic!("not a profile");
    };
    assert_eq!(p.stereotypes[0].value, "deprecated");
    assert_eq!(p.tags[0].value, "doc");
}

#[test]
fn bootstrap_elements_resolve_through_the_builder() {
    let b = ModelBuilder::new();
    assert_eq!(b.model().element_name(STRING_ID), "String");
    assert!(b.resolve("meta::pure::metamodel::type::Class").is_some());
    assert!(b.resolve("my::nothing").is_none());
}

#[test]
fn type_and_element_references_evaluate_to_the_element() {
    let mut b = ModelBuilder::new();
    let person = b.class("my::Person", class(Vec::new(), Vec::new()));
    let model = b.finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::type_ref(named(person))).unwrap(),
        Value::Element(person)
    );
    assert_eq!(
        eval.eval(&expr::element_ref(person)).unwrap(),
        Value::Element(person)
    );
}

#[test]
fn multiplicity_reference_materialises_a_multiplicity_object() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let value = eval
        .eval(&expr::mult_ref(Multiplicity::Range {
            lower: 1,
            upper: Some(3),
        }))
        .unwrap();
    let Value::Object(ref handle) = value else {
        panic!("expected a heap object");
    };
    let lower = eval
        .heap()
        .get_property_values(handle, "lowerBound")
        .unwrap();
    let Some(Value::Object(lower)) = lower.iter().next() else {
        panic!("lowerBound missing");
    };
    assert_eq!(
        eval.heap()
            .get_property_values(lower, "value")
            .unwrap()
            .iter()
            .next(),
        Some(&Value::Integer(1))
    );
}

#[test]
fn primitive_type_and_class_names_read_through_element_reflection() {
    let mut b = ModelBuilder::new();
    let person = b.class("my::Person", class(Vec::new(), Vec::new()));
    let model = b.finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(STRING_ID), "name"))
            .unwrap(),
        Value::String("String".into())
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(person), "name"))
            .unwrap(),
        Value::String("Person".into())
    );
}

#[test]
fn measure_and_unit_reflect_their_canonical_unit_and_local_name() {
    let mut b = ModelBuilder::new();
    let mass = b.add(
        "my::Mass",
        Element::Measure(Measure {
            canonical_unit: None,
            non_canonical_units: Vec::new(),
        }),
    );
    let kilogram = b.add(
        "my::Mass~Kilogram",
        Element::Unit(Unit {
            measure: mass,
            conversion_expression: None,
        }),
    );
    let gram = b.add(
        "my::Mass~Gram",
        Element::Unit(Unit {
            measure: mass,
            conversion_expression: None,
        }),
    );
    if let Element::Measure(m) = b.element_mut(mass) {
        m.canonical_unit = Some(kilogram);
        m.non_canonical_units.push(gram);
    }
    let model = b.finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(mass), "canonicalUnit"))
            .unwrap(),
        Value::Element(kilogram)
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(mass), "nonCanonicalUnits"))
            .unwrap(),
        Value::Element(gram)
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(kilogram), "name"))
            .unwrap(),
        Value::String("Kilogram".into())
    );
}

#[test]
fn dsl_instance_is_an_element_the_evaluator_can_name() {
    let mut b = ModelBuilder::new();
    let diagram = b.add(
        "my::D",
        Element::DSLInstance(DSLInstance {
            dsl_name: "Diagram".into(),
            classifier_fqn: "meta::pure::metamodel::diagram::Diagram".into(),
            data: Vec::new(),
        }),
    );
    let model = b.finish();
    assert_eq!(model.resolve_fqn_str("my::D"), Some(diagram));
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    assert_eq!(
        eval.eval(&expr::element_ref(diagram)).unwrap(),
        Value::Element(diagram)
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::element_ref(diagram), "name"))
            .unwrap(),
        Value::String("D".into())
    );
}

#[test]
fn packages_are_elements_too() {
    let mut b = ModelBuilder::new();
    let person = b.class("my::deep::Person", class(Vec::new(), Vec::new()));
    let model = b.finish();
    let pkg_id = model.get_node(person).parent_package;
    let pkg = model.get_package(pkg_id);
    assert_eq!(pkg.name, "deep");
    assert_eq!(pkg.children_elements, vec![person]);
    let as_element = ElementId::Package(pkg_id);
    assert_eq!(model.element_name(as_element), "deep");
    assert!(matches!(model.get_element(as_element), Element::Package(p) if *p == pkg_id));
}

#[test]
fn unknown_property_on_an_instance_reads_as_unit() {
    let mut b = ModelBuilder::new();
    b.class(
        "my::Person",
        class(
            Vec::new(),
            vec![property("name", named(STRING_ID), Multiplicity::PureOne)],
        ),
    );
    let model = b.finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let obj = eval.heap_mut().alloc_dynamic("my::Person");
    eval.heap_mut()
        .mutate_add(&obj, "name", &[Value::String("Ann".into())])
        .unwrap();
    eval.context_mut().push_scope();
    eval.context_mut().set("p", Value::Object(obj));
    assert_eq!(
        eval.eval(&expr::prop(expr::var("p"), "name")).unwrap(),
        Value::String("Ann".into())
    );
    assert_eq!(
        eval.eval(&expr::prop(expr::var("p"), "nope")).unwrap(),
        Value::Unit
    );
}
