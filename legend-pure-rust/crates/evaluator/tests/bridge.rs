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

//! The bridge over a host that is a table of descriptions and no runtime
//! at all: what a transport implements, exercised end to end through a
//! call whose lowering grows the model.

use legend_pure_evaluator::builders::on_both_paths;
use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;

use legend_pure_evaluator::bridge::context::BridgeContext;
use legend_pure_evaluator::bridge::description::{Description, kind};
use legend_pure_evaluator::bridge::host::{Crossing, Host, crossing_of, description_to_value};
use legend_pure_evaluator::bridge::session::Session;
use legend_pure_evaluator::bridge::timings;
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::m3_paths;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::INTEGER_ID;
use legend_pure_metamodel::model::PureModel;
use smol_str::SmolStr;

const PLUS: &str = "meta::pure::functions::math::plus_Integer_MANY__Integer_1_";
const PLUS_KEY: &str = "plus_Integer_MANY__Integer_1_";

fn node(id: i64, classifier: &str) -> Description {
    Description {
        kind: kind::NODE,
        id,
        classifier: Some(SmolStr::new(classifier)),
        ..Default::default()
    }
}

fn element(id: i64, classifier: &str, path: &str, name: &str) -> Description {
    Description {
        kind: kind::PACKAGEABLE_ELEMENT,
        id,
        classifier: Some(SmolStr::new(classifier)),
        user_path: Some(SmolStr::new(path)),
        name: Some(SmolStr::new(name)),
        ..Default::default()
    }
}

fn integer(i: i64) -> Description {
    Description {
        kind: kind::INTEGER,
        payload: Some(i.to_string()),
        ..Default::default()
    }
}

fn string(s: &str) -> Description {
    Description {
        kind: kind::STRING,
        payload: Some(s.to_string()),
        ..Default::default()
    }
}

/// A host whose graph is a table: node id → property → descriptions.
#[derive(Default)]
struct TableHost {
    nodes: HashMap<i64, HashMap<&'static str, Vec<Description>>>,
    forgotten: Rc<RefCell<Vec<i64>>>,
}

impl TableHost {
    fn add(&mut self, id: i64, properties: Vec<(&'static str, Vec<Description>)>) {
        self.nodes.insert(id, properties.into_iter().collect());
    }
}

impl Host for TableHost {
    fn property_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        let node = self.nodes.get(&id).ok_or_else(|| {
            PureException::from(
                legend_pure_evaluator::error::PureRuntimeError::EvaluationError(format!(
                    "no node {id}"
                )),
            )
        })?;
        Ok(Some(node.get(name).cloned().unwrap_or_default()))
    }

    fn captured_values(
        &self,
        _id: i64,
        _name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        Ok(None)
    }

    fn keys_of(&self, id: i64) -> Result<Vec<SmolStr>, PureException> {
        Ok(self
            .nodes
            .get(&id)
            .map(|node| node.keys().map(SmolStr::new).collect())
            .unwrap_or_default())
    }

    fn forget(&self, ids: &[i64]) {
        self.forgotten.borrow_mut().extend_from_slice(ids);
    }
}

/// `{| plus([1, 2])}` as the host describes it: the lambda is node 1,
/// its one expression node 2 calling the native `plus` (node 3) over the
/// instance value node 4.
fn graph() -> TableHost {
    let integer_type = element(9, m3_paths::PRIMITIVE_TYPE, "Integer", "Integer");
    let mut host = TableHost::default();
    host.add(
        1,
        vec![
            (
                "expressionSequence",
                vec![node(2, m3_paths::SIMPLE_FUNCTION_EXPRESSION)],
            ),
            ("openVariables", vec![]),
        ],
    );
    host.add(
        2,
        vec![
            (
                "func",
                vec![element(3, m3_paths::NATIVE_FUNCTION, PLUS, PLUS_KEY)],
            ),
            ("functionName", vec![string("plus")]),
            ("parametersValues", vec![node(4, m3_paths::INSTANCE_VALUE)]),
        ],
    );
    host.add(3, vec![("functionName", vec![string("plus")])]);
    host.add(
        4,
        vec![
            ("values", vec![integer(1), integer(2)]),
            ("genericType", vec![node(5, m3_paths::GENERIC_TYPE)]),
            ("multiplicity", vec![node(6, m3_paths::MULTIPLICITY)]),
        ],
    );
    host.add(5, vec![("rawType", vec![integer_type])]);
    host.add(
        6,
        vec![
            ("lowerBound", vec![node(7, m3_paths::MULTIPLICITY_VALUE)]),
            ("upperBound", vec![node(8, m3_paths::MULTIPLICITY_VALUE)]),
        ],
    );
    host.add(7, vec![("value", vec![integer(0)])]);
    host.add(8, vec![]);
    host
}

fn lambda(id: i64, definition: i64) -> Description {
    Description {
        definition,
        name: Some(SmolStr::new("lambda$0")),
        ..node(id, m3_paths::LAMBDA_FUNCTION)
    }
}

fn context(host: TableHost) -> BridgeContext {
    BridgeContext::new(PureModel::bootstrapped(), NativeRegistry::standard(), host)
}

#[test]
fn a_lambda_described_by_the_host_runs_and_its_lowering_grows_the_model_once() {
    on_both_paths(|| {
        let host = graph();
        let forgotten = Rc::clone(&host.forgotten);
        let mut context = context(host);
        let chunks_before = context.shared().model().chunks.len();

        let function = description_to_value(context.shared(), lambda(1, 100)).unwrap();
        assert_eq!(crossing_of(&function), Crossing::HostNode);
        assert_eq!(context.start(&function, &[]).unwrap(), Value::Integer(3));
        assert_eq!(context.shared().model().chunks.len(), chunks_before + 1);
        assert!(timings::last().lower_host_reads > 0);
        assert_eq!(timings::last().run_host_reads, 0);
        assert!(
            forgotten.borrow().contains(&2),
            "the expression node is held by nothing once the call ends: {:?}",
            forgotten.borrow()
        );
        assert!(!forgotten.borrow().contains(&1), "the lambda is still held");

        let again = description_to_value(context.shared(), lambda(30, 100)).unwrap();
        assert_eq!(context.start(&again, &[]).unwrap(), Value::Integer(3));
        assert_eq!(context.shared().model().chunks.len(), chunks_before + 1);
        assert_eq!(
            timings::last().lower_host_reads,
            0,
            "the closure cache answers for the same definition"
        );
        assert_eq!(context.live_handle_count(), 2);
        drop((function, again));
        assert_eq!(context.live_handle_count(), 0);
    });
}

#[test]
fn a_native_function_node_runs_through_the_registry() {
    on_both_paths(|| {
        let mut context = context(graph());
        let plus = description_to_value(
            context.shared(),
            element(3, m3_paths::NATIVE_FUNCTION, PLUS, PLUS_KEY),
        )
        .unwrap();
        assert_eq!(crossing_of(&plus), Crossing::HostNode);
        let args = [Value::from_vec(vec![
            Value::Integer(20),
            Value::Integer(22),
        ])];
        assert_eq!(context.start(&plus, &args).unwrap(), Value::Integer(42));
    });
}

#[test]
fn descriptions_the_model_knows_never_become_nodes() {
    on_both_paths(|| {
        let context = context(graph());
        let shared = context.shared();
        let integer_type = description_to_value(
            shared,
            element(9, m3_paths::PRIMITIVE_TYPE, "Integer", "Integer"),
        )
        .unwrap();
        assert_eq!(integer_type, Value::Element(INTEGER_ID));
        assert_eq!(
            description_to_value(shared, integer(7)).unwrap(),
            Value::Integer(7)
        );
        assert_eq!(crossing_of(&Value::Integer(7)), Crossing::Primitive);
        let stale = description_to_value(
            shared,
            Description {
                kind: kind::RUST_OBJECT,
                id: 999,
                ..Default::default()
            },
        )
        .unwrap_err();
        assert!(
            stale.to_string().contains("Stale or unknown handle: 999"),
            "{stale}"
        );
    });
}

#[test]
fn a_rust_object_handed_to_the_host_comes_back_as_itself() {
    on_both_paths(|| {
        let mut context = context(graph());
        let handle = context
            .new_object(
                "my::Thing",
                &[("name".to_string(), vec![Value::String("x".into())])],
            )
            .unwrap();
        assert_eq!(context.live_handle_count(), 1);
        let object = context.resolve_handle(handle).expect("live");
        assert_eq!(context.classifier(&object).unwrap(), "my::Thing");
        assert_eq!(
            context.property_values(&object, "name").unwrap().get(0),
            Some(&Value::String("x".into()))
        );
        let back = description_to_value(
            context.shared(),
            Description {
                kind: kind::RUST_OBJECT,
                id: handle,
                ..Default::default()
            },
        )
        .unwrap();
        assert_eq!(crossing_of(&back), Crossing::RustObject);
        context.release(handle);
        assert!(context.resolve_handle(handle).is_none());
        assert_eq!(context.live_handle_count(), 0);
    });
}

#[test]
fn a_session_hands_out_one_view_at_a_time() {
    on_both_paths(|| {
        let session = Session::new(context(graph()));
        let first = session.enter().unwrap();
        assert!(session.enter().is_err());
        drop(first);
        assert!(session.enter().is_ok());
    });
}

#[test]
fn a_call_ends_over_the_nodes_it_read_and_the_nodes_that_died() {
    on_both_paths(|| {
        let host = graph();
        let forgotten = Rc::clone(&host.forgotten);
        let mut context = context(host);
        let function = description_to_value(context.shared(), lambda(1, 100)).unwrap();

        // Host node 6, read once, then held only by a Rust object the host keeps.
        let held = description_to_value(context.shared(), node(6, m3_paths::MULTIPLICITY)).unwrap();
        let Value::Object(node_six) = &held else {
            panic!("a host node: {held:?}")
        };
        assert_eq!(
            context
                .property_values(node_six, "lowerBound")
                .unwrap()
                .len(),
            1
        );
        let handle = context
            .new_object("my::Holder", &[("node".to_string(), vec![held.clone()])])
            .unwrap();
        drop(held);
        context.start(&function, &[]).unwrap();
        let holder = context.resolve_handle(handle).expect("live");
        let Some(Value::Object(node_six)) = context
            .property_values(&holder, "node")
            .unwrap()
            .get(0)
            .cloned()
        else {
            panic!("the holder keeps node 6")
        };
        let reads = context.shared().reads();
        context.property_values(&node_six, "lowerBound").unwrap();
        assert_eq!(
            context.shared().reads(),
            reads + 1,
            "the call's end dropped the cache of a node read before it"
        );
        drop((holder, node_six));
        assert!(!forgotten.borrow().contains(&6));
        context.release(handle);
        context.start(&function, &[]).unwrap();
        assert!(
            forgotten.borrow().contains(&6),
            "a node dies with the Rust object that held it: {:?}",
            forgotten.borrow()
        );

        // An id described again after its node died, before any call ended.
        let first =
            description_to_value(context.shared(), node(40, m3_paths::MULTIPLICITY)).unwrap();
        drop(first);
        let second =
            description_to_value(context.shared(), node(40, m3_paths::MULTIPLICITY)).unwrap();
        context.start(&function, &[]).unwrap();
        assert!(
            !forgotten.borrow().contains(&40),
            "the id is held again, by the second node"
        );
        drop(second);
        context.start(&function, &[]).unwrap();
        assert!(forgotten.borrow().contains(&40));
    });
}
