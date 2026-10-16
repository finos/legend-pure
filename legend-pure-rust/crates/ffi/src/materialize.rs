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

//! Values crossing out as data: a result is one Rust-owned allocation
//! holding the flat leaves the host reads and every string and nested
//! array they point into, released as one by `pure_result_free`.

use legend_pure_evaluator::bridge::description::kind;
use legend_pure_evaluator::bridge::host::HostShared;
use legend_pure_evaluator::bridge::outbound::{Leaf, leaf, leaves};
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::value::Value;

use crate::abi::{KIND_MAP, PureLeaf, PureResult, PureString};

/// What the leaves point into: owned strings and the nested leaf arrays
/// of maps. A `String` or `Vec` moved into these vectors keeps its heap
/// buffer, so a pointer taken into one stays valid for the result's life.
#[derive(Default)]
struct Storage {
    strings: Vec<String>,
    nested: Vec<Vec<PureLeaf>>,
}

impl Storage {
    /// A present string: empty strings point at a static, others into
    /// the stored copy.
    fn text(&mut self, s: String) -> PureString {
        if s.is_empty() {
            return PureString::of("");
        }
        self.strings.push(s);
        self.strings
            .last()
            .map_or(PureString::ABSENT, |s| PureString::of(s))
    }

    /// A nested array, kept; NULL when empty.
    fn nested(&mut self, items: Vec<PureLeaf>) -> *const PureLeaf {
        if items.is_empty() {
            return std::ptr::null();
        }
        let ptr = items.as_ptr();
        self.nested.push(items);
        ptr
    }
}

/// The leaf of one value that is neither a collection nor the empty
/// value (see [`leaves`]).
fn leaf_of(
    shared: &HostShared,
    storage: &mut Storage,
    value: &Value,
) -> Result<PureLeaf, PureException> {
    Ok(match leaf(shared, value)? {
        Leaf::Boolean(b) => {
            let mut out = PureLeaf::of_kind(kind::BOOLEAN);
            out.integer = i64::from(b);
            out
        }
        Leaf::Integer(i) => {
            let mut out = PureLeaf::of_kind(kind::INTEGER);
            out.integer = i;
            out
        }
        Leaf::Float(f) => {
            let mut out = PureLeaf::of_kind(kind::FLOAT);
            out.real = f;
            out
        }
        Leaf::Decimal(d) => {
            let mut out = PureLeaf::of_kind(kind::DECIMAL);
            out.text = storage.text(d.to_string());
            out
        }
        Leaf::String(s) => {
            let mut out = PureLeaf::of_kind(kind::STRING);
            out.text = storage.text(s.to_owned());
            out
        }
        Leaf::LatestDate => PureLeaf::of_kind(kind::LATEST_DATE),
        Leaf::Date(d) => {
            let mut out = PureLeaf::of_kind(kind::DATE);
            out.year = d.year;
            out.month = d.month;
            out.day = d.day;
            out.hour = d.hour;
            out.minute = d.minute;
            out.second = d.second;
            out.precision = d.precision;
            out.text = storage.text(d.subsecond);
            out
        }
        Leaf::StrictTime(t) => {
            let mut out = PureLeaf::of_kind(kind::STRICT_TIME);
            out.hour = t.hour;
            out.minute = t.minute;
            out.second = t.second;
            out.precision = t.precision;
            out.text = storage.text(t.subsecond);
            out
        }
        Leaf::Element(path) => {
            let mut out = PureLeaf::of_kind(kind::PACKAGEABLE_ELEMENT);
            out.text = storage.text(path);
            out
        }
        Leaf::EnumValue { enumeration, name } => {
            let mut out = PureLeaf::of_kind(kind::ENUM_VALUE);
            out.text = storage.text(enumeration);
            out.name = storage.text(name.to_owned());
            out
        }
        Leaf::HostNode(id) => {
            let mut out = PureLeaf::of_kind(kind::NODE);
            out.integer = id;
            out
        }
        Leaf::RustObject { handle, classifier } => {
            let mut out = PureLeaf::of_kind(kind::RUST_OBJECT);
            out.integer = handle;
            out.text = storage.text(classifier.to_string());
            out
        }
        Leaf::Map(entries) => {
            let mut flat = Vec::with_capacity(entries.len() * 2);
            for (key, value) in &entries {
                flat.push(leaf_of(shared, storage, key)?);
                flat.push(leaf_of(shared, storage, value)?);
            }
            let mut out = PureLeaf::of_kind(KIND_MAP);
            out.entry_count = entries.len();
            out.entries = storage.nested(flat);
            out
        }
    })
}

/// A result as the host receives it: the `pure_result` view first, so
/// the pointer to the view is the pointer to the allocation, then what
/// the view and its leaves point into.
#[repr(C)]
pub struct ResultBox {
    view: PureResult,
    leaves: Vec<PureLeaf>,
    storage: Storage,
}

impl ResultBox {
    fn finish(leaves: Vec<PureLeaf>, storage: Storage) -> Box<Self> {
        let mut boxed = Box::new(Self {
            view: PureResult {
                leaves: std::ptr::null(),
                count: 0,
            },
            leaves,
            storage,
        });
        if !boxed.leaves.is_empty() {
            boxed.view = PureResult {
                leaves: boxed.leaves.as_ptr(),
                count: boxed.leaves.len(),
            };
        }
        boxed
    }

    /// The leaves of a value, collections flattened and the empty value
    /// dropped.
    ///
    /// # Errors
    /// A value kind that cannot cross.
    pub fn of_value(shared: &HostShared, value: &Value) -> Result<Box<Self>, PureException> {
        let mut found = Vec::new();
        leaves(value, &mut found);
        let mut storage = Storage::default();
        let mut out = Vec::with_capacity(found.len());
        for value in found {
            out.push(leaf_of(shared, &mut storage, value)?);
        }
        Ok(Self::finish(out, storage))
    }

    /// One string leaf per item.
    pub fn of_strings(items: impl IntoIterator<Item = String>) -> Box<Self> {
        let mut storage = Storage::default();
        let out = items
            .into_iter()
            .map(|s| {
                let mut leaf = PureLeaf::of_kind(kind::STRING);
                leaf.text = storage.text(s);
                leaf
            })
            .collect();
        Self::finish(out, storage)
    }

    /// Hand the host the pointer to the view; the allocation is its
    /// until `pure_result_free`.
    pub fn into_raw(boxed: Box<Self>) -> *mut PureResult {
        Box::into_raw(boxed).cast::<PureResult>()
    }

    /// Take the allocation back from the pointer the host returns.
    ///
    /// # Safety
    /// `ptr` came from [`into_raw`](Self::into_raw) and has not been
    /// freed.
    pub unsafe fn from_raw(ptr: *mut PureResult) -> Box<Self> {
        // SAFETY: per this function's contract, `ptr` is the first field
        // of a live `ResultBox` allocated by `Box::new` in `finish`; the
        // struct is `repr(C)`, so the view's address is the box's.
        unsafe { Box::from_raw(ptr.cast::<Self>()) }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use legend_pure_evaluator::bridge::context::BridgeContext;
    use legend_pure_evaluator::bridge::description::Description;
    use legend_pure_evaluator::bridge::host::Host;
    use legend_pure_evaluator::native::NativeRegistry;
    use legend_pure_evaluator::value::{MapState, ValueKey};
    use legend_pure_metamodel::model::PureModel;
    use std::cell::RefCell;
    use std::rc::Rc;

    struct NoHost;

    impl Host for NoHost {
        fn property_values(
            &self,
            _id: i64,
            _name: &str,
        ) -> Result<Option<Vec<Description>>, PureException> {
            Ok(None)
        }

        fn captured_values(
            &self,
            _id: i64,
            _name: &str,
        ) -> Result<Option<Vec<Description>>, PureException> {
            Ok(None)
        }

        fn keys_of(&self, _id: i64) -> Result<Vec<smol_str::SmolStr>, PureException> {
            Ok(Vec::new())
        }

        fn forget(&self, _ids: &[i64]) {}
    }

    fn text(s: PureString) -> Option<String> {
        if s.ptr.is_null() {
            return None;
        }
        // SAFETY: a present string of a result points into the result's
        // own storage, which outlives this read.
        let bytes = unsafe { std::slice::from_raw_parts(s.ptr, s.len) };
        Some(String::from_utf8(bytes.to_vec()).unwrap())
    }

    /// A key and a value, each as (kind, text, integer).
    type Pair = (i32, Option<String>, i64, i32, Option<String>, i64);

    #[test]
    fn a_map_crosses_as_one_leaf_over_its_entries() {
        let context = BridgeContext::new(
            PureModel::bootstrapped(),
            NativeRegistry::standard(),
            NoHost,
        );
        let mut state = MapState::default();
        state
            .entries
            .insert(ValueKey::String("a".into()), Value::Integer(1));
        state
            .entries
            .insert(ValueKey::Integer(2), Value::String("b".into()));
        let map = Value::Map(Rc::new(RefCell::new(state)));
        let result = ResultBox::of_value(context.shared(), &map).unwrap();
        assert_eq!(result.leaves.len(), 1);
        let leaf = result.leaves[0];
        assert_eq!(leaf.kind, KIND_MAP);
        assert_eq!(leaf.entry_count, 2);
        // SAFETY: a map leaf's entries are `2 * entry_count` leaves kept
        // by the result.
        let entries = unsafe { std::slice::from_raw_parts(leaf.entries, 4) };
        let mut pairs: Vec<Pair> = entries
            .chunks(2)
            .map(|kv| {
                (
                    kv[0].kind,
                    text(kv[0].text),
                    kv[0].integer,
                    kv[1].kind,
                    text(kv[1].text),
                    kv[1].integer,
                )
            })
            .collect();
        pairs.sort();
        assert_eq!(
            pairs,
            vec![
                (
                    kind::INTEGER,
                    None,
                    2,
                    kind::STRING,
                    Some("b".to_owned()),
                    0
                ),
                (
                    kind::STRING,
                    Some("a".to_owned()),
                    0,
                    kind::INTEGER,
                    None,
                    1
                ),
            ]
        );
        let view = result.view;
        assert_eq!(view.count, 1);
        assert_eq!(view.leaves, result.leaves.as_ptr());
    }

    #[test]
    fn an_empty_result_has_a_null_leaf_pointer() {
        let result = ResultBox::of_strings(Vec::<String>::new());
        assert!(result.view.leaves.is_null());
        assert_eq!(result.view.count, 0);
        let raw = ResultBox::into_raw(result);
        // SAFETY: `raw` was just made by `into_raw` and is taken back once.
        drop(unsafe { ResultBox::from_raw(raw) });
    }
}
