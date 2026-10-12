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

//! The whole ABI driven as a host would drive it: a callback table of
//! `extern "C"` functions over a graph that is a table of descriptions,
//! a context, calls, leaves read back, handles, failures.

#![deny(clippy::undocumented_unsafe_blocks)]

use std::cell::{Cell, RefCell};
use std::collections::HashMap;
use std::ffi::c_void;

use legend_pure_evaluator::bridge::description::{Description, kind};
use legend_pure_evaluator::m3_paths;
use pure_rust_ffi::abi::{
    ABI_VERSION, PureContext, PureDescription, PureHostTable, PureProperty, PureReply, PureResult,
    PureSlot, PureString, PureTimings, crossing, failure_kind, status,
};
use pure_rust_ffi::{
    pure_abi_version, pure_context_free, pure_context_new, pure_crossing, pure_last_failure,
    pure_last_timings, pure_live_handle_count, pure_object_classifier, pure_object_new,
    pure_object_property_names, pure_object_property_values, pure_object_release,
    pure_reply_descriptions, pure_reply_fail, pure_reply_names, pure_result_free, pure_start,
};
use smol_str::SmolStr;

const PLUS: &str = "meta::pure::functions::math::plus_Integer_MANY__Integer_1_";
const PLUS_KEY: &str = "plus_Integer_MANY__Integer_1_";
const DATE: &str =
    "meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__StrictDate_1_";
const DATE_KEY: &str = "date_Integer_1__Integer_1__Integer_1__StrictDate_1_";

// ---------------------------------------------------------------------
// The host side: descriptions as the bridge test builds them, flattened
// into the header's struct at push time.
// ---------------------------------------------------------------------

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

fn lambda(id: i64, definition: i64) -> Description {
    Description {
        definition,
        name: Some(SmolStr::new("lambda$0")),
        ..node(id, m3_paths::LAMBDA_FUNCTION)
    }
}

fn rust_object(handle: i64) -> Description {
    Description {
        kind: kind::RUST_OBJECT,
        id: handle,
        ..Default::default()
    }
}

/// The header's struct over a description whose strings outlive it.
fn flat(d: &Description) -> PureDescription {
    let s = |o: Option<&str>| o.map_or(PureString::ABSENT, PureString::of);
    PureDescription {
        id: d.id,
        definition: d.definition,
        classifier: s(d.classifier.as_deref()),
        name: s(d.name.as_deref()),
        user_path: s(d.user_path.as_deref()),
        payload: s(d.payload.as_deref()),
        subsecond: s(d.subsecond.as_deref()),
        source_id: s(d.source_id.as_deref()),
        kind: d.kind,
        year: d.year,
        month: d.month,
        day: d.day,
        hour: d.hour,
        minute: d.minute,
        second: d.second,
        precision: d.precision,
        line: d.line,
        column: d.column,
        end_line: d.end_line,
        end_column: d.end_column,
    }
}

/// A host whose graph is a table: node id → property → descriptions.
/// It also probes the rules from inside a callback: a re-entrant call is
/// refused, a free from inside fails.
#[derive(Default)]
struct Table {
    nodes: HashMap<i64, HashMap<&'static str, Vec<Description>>>,
    forgotten: RefCell<Vec<i64>>,
    ctx: Cell<*mut PureContext>,
    reentered: Cell<Option<i64>>,
    freed_inside: Cell<Option<i32>>,
}

impl Table {
    fn add(&mut self, id: i64, properties: Vec<(&'static str, Vec<Description>)>) {
        self.nodes.insert(id, properties.into_iter().collect());
    }
}

/// `{| plus([1, 2])}` as the host describes it: the lambda is node 1,
/// its one expression node 2 calling the native `plus` (node 3) over the
/// instance value node 4.
fn graph() -> Table {
    let integer_type = element(9, m3_paths::PRIMITIVE_TYPE, "Integer", "Integer");
    let mut host = Table::default();
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

/// The bytes of a name argument.
///
/// # Safety
/// `ptr` is NULL or `len` readable bytes.
unsafe fn name_of<'a>(ptr: *const u8, len: usize) -> &'a str {
    if len == 0 {
        return "";
    }
    // SAFETY: per this function's contract.
    std::str::from_utf8(unsafe { std::slice::from_raw_parts(ptr, len) }).expect("UTF-8")
}

fn push(reply: *mut PureReply, values: &[Description]) -> i32 {
    let items: Vec<PureDescription> = values.iter().map(flat).collect();
    // SAFETY: `reply` is the pointer this callback received; `items`
    // and the descriptions its strings point into outlive the push.
    unsafe { pure_reply_descriptions(reply, items.as_ptr(), items.len()) }
}

fn fail(reply: *mut PureReply, kind: i32, message: &str) -> i32 {
    // SAFETY: `reply` is the pointer this callback received; the message
    // outlives the push.
    unsafe { pure_reply_fail(reply, kind, message.as_ptr(), message.len()) };
    status::FAILED
}

unsafe extern "C" fn table_property_values(
    host: *mut c_void,
    id: i64,
    name: *const u8,
    len: usize,
    reply: *mut PureReply,
) -> i32 {
    // SAFETY: the table registered itself as the host pointer and
    // outlives the context.
    let table = unsafe { &*host.cast::<Table>() };
    // SAFETY: the name is what the ABI promises a callback.
    let name = unsafe { name_of(name, len) };
    let ctx = table.ctx.get();
    if !ctx.is_null() && table.reentered.get().is_none() {
        // SAFETY: the context pointer is live; the call is made from
        // inside a callback on purpose, to show it is refused.
        unsafe {
            table.reentered.set(Some(pure_live_handle_count(ctx)));
            table.freed_inside.set(Some(pure_context_free(ctx)));
        }
    }
    let Some(node) = table.nodes.get(&id) else {
        return fail(
            reply,
            failure_kind::EXECUTION_ERROR,
            &format!("no node {id}"),
        );
    };
    match node.get(name) {
        Some(values) => {
            assert_eq!(push(reply, values), status::OK);
            status::OK
        }
        None => status::OK,
    }
}

unsafe extern "C" fn table_captured_values(
    _host: *mut c_void,
    _id: i64,
    _name: *const u8,
    _len: usize,
    _reply: *mut PureReply,
) -> i32 {
    status::ABSENT
}

unsafe extern "C" fn table_keys(host: *mut c_void, id: i64, reply: *mut PureReply) -> i32 {
    // SAFETY: as in `table_property_values`.
    let table = unsafe { &*host.cast::<Table>() };
    let Some(node) = table.nodes.get(&id) else {
        return status::ABSENT;
    };
    let names: Vec<PureString> = node.keys().map(|k| PureString::of(k)).collect();
    // SAFETY: `reply` is the pointer this callback received; the names
    // point into the table, which outlives the push.
    let pushed = unsafe { pure_reply_names(reply, names.as_ptr(), names.len()) };
    assert_eq!(pushed, status::OK);
    status::OK
}

unsafe extern "C" fn table_forget(host: *mut c_void, ids: *const i64, count: usize) {
    // SAFETY: as in `table_property_values`; `ids` is `count` integers.
    let table = unsafe { &*host.cast::<Table>() };
    // SAFETY: the ABI promises `count` readable ids.
    let ids = unsafe { std::slice::from_raw_parts(ids, count) };
    table.forgotten.borrow_mut().extend_from_slice(ids);
}

/// A host that fails every property read with the kind and text given.
struct Failing {
    kind: i32,
    text: &'static str,
}

unsafe extern "C" fn failing_property_values(
    host: *mut c_void,
    _id: i64,
    _name: *const u8,
    _len: usize,
    reply: *mut PureReply,
) -> i32 {
    // SAFETY: the `Failing` registered itself as the host pointer and
    // outlives the context.
    let failing = unsafe { &*host.cast::<Failing>() };
    fail(reply, failing.kind, failing.text)
}

unsafe extern "C" fn failing_keys(_host: *mut c_void, _id: i64, _reply: *mut PureReply) -> i32 {
    status::ABSENT
}

// ---------------------------------------------------------------------
// Driving the ABI.
// ---------------------------------------------------------------------

/// A context over a boxed table (a stable address for the host pointer).
struct Driver {
    table: Box<Table>,
    ctx: *mut PureContext,
}

impl Driver {
    fn new(table: Table) -> Self {
        let table = Box::new(table);
        let host = PureHostTable {
            host: std::ptr::from_ref::<Table>(&table)
                .cast_mut()
                .cast::<c_void>(),
            property_values: Some(table_property_values),
            captured_values: Some(table_captured_values),
            keys: Some(table_keys),
            forget: Some(table_forget),
        };
        let mut ctx = std::ptr::null_mut();
        // SAFETY: both pointers are to live locals.
        let status = unsafe { pure_context_new(&raw const host, &raw mut ctx) };
        assert_eq!(status, status::OK, "{}", last_message());
        assert!(!ctx.is_null());
        Self { table, ctx }
    }

    fn start(&self, function: &Description, args: &[Vec<Description>]) -> Result<Leaves, String> {
        let function = flat(function);
        let flat_args: Vec<Vec<PureDescription>> =
            args.iter().map(|a| a.iter().map(flat).collect()).collect();
        let slots: Vec<PureSlot> = flat_args
            .iter()
            .map(|a| PureSlot {
                items: a.as_ptr(),
                count: a.len(),
            })
            .collect();
        let mut out = std::ptr::null_mut();
        // SAFETY: the context is live; every pointer is to a live local
        // and the descriptions' strings outlive the call.
        let status = unsafe {
            pure_start(
                self.ctx,
                &raw const function,
                slots.as_ptr(),
                slots.len(),
                &raw mut out,
            )
        };
        outcome(status, out)
    }

    fn property_values(&self, handle: i64, name: &str) -> Result<Leaves, String> {
        let mut out = std::ptr::null_mut();
        // SAFETY: the context is live; the name and the out pointer are
        // live locals.
        let status = unsafe {
            pure_object_property_values(self.ctx, handle, name.as_ptr(), name.len(), &raw mut out)
        };
        outcome(status, out)
    }

    fn property_names(&self, handle: i64) -> Result<Leaves, String> {
        let mut out = std::ptr::null_mut();
        // SAFETY: the context is live; the out pointer is a live local.
        let status = unsafe { pure_object_property_names(self.ctx, handle, &raw mut out) };
        outcome(status, out)
    }

    fn classifier(&self, handle: i64) -> Result<Leaves, String> {
        let mut out = std::ptr::null_mut();
        // SAFETY: the context is live; the out pointer is a live local.
        let status = unsafe { pure_object_classifier(self.ctx, handle, &raw mut out) };
        outcome(status, out)
    }

    fn new_object(&self, classifier: &str, properties: &[(&str, Vec<Description>)]) -> i64 {
        let flat_values: Vec<Vec<PureDescription>> = properties
            .iter()
            .map(|(_, v)| v.iter().map(flat).collect())
            .collect();
        let props: Vec<PureProperty> = properties
            .iter()
            .zip(&flat_values)
            .map(|((name, _), values)| PureProperty {
                name: PureString::of(name),
                values: PureSlot {
                    items: values.as_ptr(),
                    count: values.len(),
                },
            })
            .collect();
        let mut handle = 0;
        // SAFETY: the context is live; every pointer is to a live local.
        let status = unsafe {
            pure_object_new(
                self.ctx,
                classifier.as_ptr(),
                classifier.len(),
                props.as_ptr(),
                props.len(),
                &raw mut handle,
            )
        };
        assert_eq!(status, status::OK, "{}", last_message());
        handle
    }

    fn crossing(&self, d: &Description) -> i32 {
        let d = flat(d);
        let mut out = -1;
        // SAFETY: the context is live; both pointers are to live locals.
        let status = unsafe { pure_crossing(self.ctx, &raw const d, &raw mut out) };
        assert_eq!(status, status::OK, "{}", last_message());
        out
    }

    fn release(&self, handle: i64) {
        // SAFETY: the context is live.
        unsafe { pure_object_release(self.ctx, handle) };
    }

    fn live(&self) -> i64 {
        // SAFETY: the context is live.
        unsafe { pure_live_handle_count(self.ctx) }
    }

    fn free(self) {
        // SAFETY: the context is live and passed to nothing after this.
        let status = unsafe { pure_context_free(self.ctx) };
        assert_eq!(status, status::OK, "{}", last_message());
    }
}

/// One leaf, copied out of a result.
#[derive(Debug, Clone, PartialEq)]
struct Leaf {
    kind: i32,
    integer: i64,
    real: f64,
    text: Option<String>,
    name: Option<String>,
    date: [i32; 7],
    entries: Vec<Leaf>,
}

type Leaves = Vec<Leaf>;

fn text_of(s: PureString) -> Option<String> {
    if s.ptr.is_null() {
        return None;
    }
    if s.len == 0 {
        return Some(String::new());
    }
    // SAFETY: a present string of a Rust-owned result or failure is
    // `len` readable bytes.
    let bytes = unsafe { std::slice::from_raw_parts(s.ptr, s.len) };
    Some(std::str::from_utf8(bytes).expect("UTF-8").to_owned())
}

fn copy_leaves(ptr: *const pure_rust_ffi::abi::PureLeaf, count: usize) -> Leaves {
    if count == 0 {
        return Vec::new();
    }
    // SAFETY: a result's leaves are `count` readable leaves until freed.
    let leaves = unsafe { std::slice::from_raw_parts(ptr, count) };
    leaves
        .iter()
        .map(|l| Leaf {
            kind: l.kind,
            integer: l.integer,
            real: l.real,
            text: text_of(l.text),
            name: text_of(l.name),
            date: [
                l.year,
                l.month,
                l.day,
                l.hour,
                l.minute,
                l.second,
                l.precision,
            ],
            entries: copy_leaves(l.entries, l.entry_count * 2),
        })
        .collect()
}

/// The leaves of a result, which is then freed; or the failure's message.
fn outcome(status: i32, result: *mut PureResult) -> Result<Leaves, String> {
    if status != status::OK {
        assert!(result.is_null(), "an out pointer written on failure");
        return Err(last_message());
    }
    assert!(!result.is_null());
    // SAFETY: the result was just returned by the ABI and is freed once.
    let leaves = unsafe {
        let view = *result;
        let leaves = copy_leaves(view.leaves, view.count);
        pure_result_free(result);
        leaves
    };
    Ok(leaves)
}

fn last_message() -> String {
    let failure = pure_last_failure();
    assert!(!failure.is_null(), "no failure recorded");
    // SAFETY: just returned non-NULL and nothing has replaced it.
    text_of(unsafe { (*failure).message }).expect("a message")
}

fn last_kind() -> i32 {
    // SAFETY: as in `last_message`.
    unsafe { (*pure_last_failure()).kind }
}

fn timings() -> PureTimings {
    let mut t = PureTimings::default();
    // SAFETY: a live local.
    unsafe { pure_last_timings(&raw mut t) };
    t
}

fn integer_leaf(i: i64) -> Leaf {
    Leaf {
        kind: kind::INTEGER,
        integer: i,
        real: 0.0,
        text: None,
        name: None,
        date: [0; 7],
        entries: Vec::new(),
    }
}

fn string_leaf(s: &str) -> Leaf {
    Leaf {
        kind: kind::STRING,
        integer: 0,
        real: 0.0,
        text: Some(s.to_owned()),
        name: None,
        date: [0; 7],
        entries: Vec::new(),
    }
}

// ---------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------

#[test]
fn the_library_implements_the_version_the_crate_declares() {
    assert_eq!(pure_abi_version(), ABI_VERSION);
}

#[test]
fn a_lambda_described_by_the_host_runs_and_the_second_call_hits_the_closure_cache() {
    let driver = Driver::new(graph());
    assert!(pure_last_failure().is_null());

    assert_eq!(
        driver.start(&lambda(1, 100), &[]).unwrap(),
        vec![integer_leaf(3)]
    );
    assert!(pure_last_failure().is_null());
    let first = timings();
    assert!(first.lower_host_reads > 0);
    assert_eq!(first.run_host_reads, 0);
    assert!(first.lower_ns > 0 && first.run_ns > 0);
    {
        let forgotten = driver.table.forgotten.borrow();
        assert!(
            forgotten.contains(&2),
            "the expression node died: {forgotten:?}"
        );
        assert!(
            !forgotten.contains(&1),
            "the lambda was held through the call"
        );
    }

    assert_eq!(
        driver.start(&lambda(30, 100), &[]).unwrap(),
        vec![integer_leaf(3)]
    );
    assert_eq!(timings().lower_host_reads, 0, "the closure cache answers");

    assert_eq!(driver.live(), 0, "nothing is held once the calls end");
    assert!(driver.table.forgotten.borrow().contains(&1));
    driver.free();
}

#[test]
fn a_native_function_runs_over_argument_slots_and_dates_cross_as_parts() {
    let driver = Driver::new(graph());
    let plus = element(3, m3_paths::NATIVE_FUNCTION, PLUS, PLUS_KEY);
    assert_eq!(
        driver
            .start(&plus, &[vec![integer(20), integer(22)]])
            .unwrap(),
        vec![integer_leaf(42)]
    );

    let date = element(40, m3_paths::NATIVE_FUNCTION, DATE, DATE_KEY);
    let leaves = driver
        .start(
            &date,
            &[vec![integer(2024)], vec![integer(3)], vec![integer(9)]],
        )
        .unwrap();
    assert_eq!(leaves.len(), 1);
    assert_eq!(leaves[0].kind, kind::DATE);
    assert_eq!(leaves[0].date, [2024, 3, 9, 0, 0, 0, 2]);
    assert_eq!(
        leaves[0].text.as_deref(),
        Some(""),
        "subsecond digits, present and empty"
    );
    driver.free();
}

#[test]
fn a_rust_object_is_read_through_its_handle_until_released() {
    let driver = Driver::new(graph());
    let handle = driver.new_object("my::Thing", &[("name", vec![string("x")])]);
    assert_eq!(driver.live(), 1);
    assert_eq!(
        driver.classifier(handle).unwrap(),
        vec![string_leaf("my::Thing")]
    );
    assert_eq!(
        driver.property_values(handle, "name").unwrap(),
        vec![string_leaf("x")]
    );
    assert_eq!(driver.property_values(handle, "other").unwrap(), vec![]);
    assert_eq!(
        driver.property_names(handle).unwrap(),
        vec![string_leaf("name")]
    );

    assert_eq!(driver.crossing(&rust_object(handle)), crossing::RUST_OBJECT);
    assert_eq!(driver.crossing(&integer(7)), crossing::PRIMITIVE);
    assert_eq!(
        driver.crossing(&element(9, m3_paths::PRIMITIVE_TYPE, "Integer", "Integer")),
        crossing::ELEMENT
    );
    assert_eq!(
        driver.crossing(&node(2, m3_paths::SIMPLE_FUNCTION_EXPRESSION)),
        crossing::HOST_NODE
    );

    driver.release(handle);
    assert_eq!(driver.live(), 0);
    let failure = driver.property_values(handle, "name").unwrap_err();
    assert!(failure.contains("Stale or unknown handle"), "{failure}");
    assert_eq!(last_kind(), failure_kind::EXECUTION_ERROR);
    driver.release(handle);
    driver.free();
}

#[test]
fn a_callback_may_not_re_enter_the_context_nor_free_it() {
    let driver = Driver::new(graph());
    driver.table.ctx.set(driver.ctx);
    assert_eq!(
        driver.start(&lambda(1, 100), &[]).unwrap(),
        vec![integer_leaf(3)]
    );
    assert_eq!(
        driver.table.reentered.get(),
        Some(-1),
        "a re-entrant call is refused"
    );
    assert_eq!(
        driver.table.freed_inside.get(),
        Some(status::FAILED),
        "a free from inside a call is refused"
    );
    driver.table.ctx.set(std::ptr::null_mut());
    assert_eq!(driver.live(), 0);
    driver.free();
}

#[test]
fn a_failure_the_host_reports_keeps_its_kind() {
    const READ: &str = "reading `classifierGenericType` on host node 1 (property values)";
    for (kind, text, expected_kind, expected_message, expected_info) in [
        (
            failure_kind::UNSUPPORTED,
            "no graph here",
            failure_kind::UNSUPPORTED,
            "Unsupported: no graph here".to_owned(),
            "no graph here".to_owned(),
        ),
        (
            failure_kind::NOT_IMPLEMENTED,
            "someKey",
            failure_kind::NOT_IMPLEMENTED,
            "NotImplemented: someKey".to_owned(),
            "someKey".to_owned(),
        ),
        (
            failure_kind::FUNCTION_NOT_FOUND,
            "a::b",
            failure_kind::FUNCTION_NOT_FOUND,
            "FunctionNotFound: a::b".to_owned(),
            "a::b".to_owned(),
        ),
        (
            failure_kind::EXECUTION_ERROR,
            "boom",
            failure_kind::EXECUTION_ERROR,
            format!("ExecutionError: {READ}: boom"),
            format!("{READ}: boom"),
        ),
    ] {
        let failing = Box::new(Failing { kind, text });
        let host = PureHostTable {
            host: std::ptr::from_ref::<Failing>(&failing)
                .cast_mut()
                .cast::<c_void>(),
            property_values: Some(failing_property_values),
            captured_values: Some(table_captured_values),
            keys: Some(failing_keys),
            forget: None,
        };
        let mut ctx = std::ptr::null_mut();
        // SAFETY: both pointers are to live locals.
        let created = unsafe { pure_context_new(&raw const host, &raw mut ctx) };
        assert_eq!(created, status::OK);
        let described = lambda(1, 100);
        let function = flat(&described);
        let mut out = std::ptr::null_mut();
        // SAFETY: the context is live; the other pointers are live locals.
        let status =
            unsafe { pure_start(ctx, &raw const function, std::ptr::null(), 0, &raw mut out) };
        assert_eq!(status, status::FAILED);
        assert!(out.is_null());
        assert_eq!(last_message(), expected_message);
        assert_eq!(last_kind(), expected_kind);
        // SAFETY: as in `last_message`.
        let failure = unsafe { *pure_last_failure() };
        assert_eq!(text_of(failure.info), Some(expected_info));
        assert_eq!(text_of(failure.source.source).as_deref(), Some("<ffi>"));
        assert_eq!(
            [
                failure.source.start_line,
                failure.source.start_column,
                failure.source.end_line,
                failure.source.end_column
            ],
            [0; 4]
        );
        assert_eq!(failure.call_stack_len, 0);
        assert!(text_of(failure.constraint_id).is_none());
        // SAFETY: the context is live and passed to nothing after this.
        assert_eq!(unsafe { pure_context_free(ctx) }, status::OK);
        assert!(pure_last_failure().is_null());
    }
}

/// Freeing a context releases every Rust object the host still holds and
/// the host nodes they refer to (a Rust object over a host node would
/// otherwise keep the node, the context's shared state and the host
/// alive in a cycle): the host is told, from inside the free, that the
/// node died.
#[test]
fn freeing_a_context_releases_an_unreleased_object_over_a_host_node() {
    let driver = Driver::new(graph());
    driver.new_object(
        "my::Holder",
        &[("node", vec![node(2, m3_paths::SIMPLE_FUNCTION_EXPRESSION)])],
    );
    assert_eq!(driver.live(), 2, "the object and the node it holds");
    let Driver { table, ctx } = driver;
    // SAFETY: the context is live and passed to nothing after this.
    assert_eq!(unsafe { pure_context_free(ctx) }, status::OK);
    assert!(pure_last_failure().is_null());
    assert_eq!(
        *table.forgotten.borrow(),
        vec![2],
        "the node died with the context and its host was told"
    );
}

/// A failure raised while a located node is read carries the node's whole
/// span, start and end, as a `pure_span`.
#[test]
fn a_failure_at_a_located_node_carries_its_whole_span() {
    let failing = Box::new(Failing {
        kind: failure_kind::EXECUTION_ERROR,
        text: "boom",
    });
    let host = PureHostTable {
        host: std::ptr::from_ref::<Failing>(&failing)
            .cast_mut()
            .cast::<c_void>(),
        property_values: Some(failing_property_values),
        captured_values: Some(table_captured_values),
        keys: Some(failing_keys),
        forget: None,
    };
    let mut ctx = std::ptr::null_mut();
    // SAFETY: both pointers are to live locals.
    let created = unsafe { pure_context_new(&raw const host, &raw mut ctx) };
    assert_eq!(created, status::OK);
    let located = Description {
        source_id: Some("/a/b.pure".to_owned()),
        line: 3,
        column: 5,
        end_line: 4,
        end_column: 9,
        ..lambda(1, 100)
    };
    let function = flat(&located);
    let mut out = std::ptr::null_mut();
    // SAFETY: the context is live; the other pointers are live locals.
    let status = unsafe { pure_start(ctx, &raw const function, std::ptr::null(), 0, &raw mut out) };
    assert_eq!(status, status::FAILED);
    assert_eq!(last_kind(), failure_kind::EXECUTION_ERROR);
    // SAFETY: as in `last_message`.
    let failure = unsafe { *pure_last_failure() };
    assert_eq!(text_of(failure.source.source).as_deref(), Some("/a/b.pure"));
    assert_eq!(
        [
            failure.source.start_line,
            failure.source.start_column,
            failure.source.end_line,
            failure.source.end_column
        ],
        [3, 5, 4, 9]
    );
    // SAFETY: the context is live and passed to nothing after this.
    assert_eq!(unsafe { pure_context_free(ctx) }, status::OK);
}
