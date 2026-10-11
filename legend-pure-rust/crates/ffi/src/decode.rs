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

//! Reading host memory: the flat structs the host hands in, copied into
//! the bridge's own types before the call that received them returns.
//! Every function here is `unsafe` for one reason, stated once: the host
//! promised (header, rule 2) that the pointers it passes are valid for
//! the duration of the call.

use std::rc::Rc;

use legend_pure_evaluator::bridge::description::Description;
use legend_pure_evaluator::bridge::failure::boundary_error;
use legend_pure_evaluator::bridge::host::{HostShared, description_to_value};
use legend_pure_evaluator::error::PureException;
use legend_pure_evaluator::value::Value;
use smol_str::SmolStr;

use crate::abi::{PureDescription, PureSlot, PureString};

/// The bytes of a string argument as `&str`; a NULL pointer with a
/// count of zero is the empty string.
///
/// # Safety
/// `ptr` is NULL or points to `len` bytes readable for `'a`.
///
/// # Errors
/// NULL with a non-zero count, or bytes that are not UTF-8.
pub unsafe fn text<'a>(ptr: *const u8, len: usize, what: &str) -> Result<&'a str, PureException> {
    if len == 0 {
        return Ok("");
    }
    if ptr.is_null() {
        return Err(boundary_error(format!(
            "{what}: a NULL string of {len} bytes"
        )));
    }
    // SAFETY: per this function's contract, `ptr` is non-NULL here and
    // points to `len` readable bytes that outlive `'a`.
    let bytes = unsafe { std::slice::from_raw_parts(ptr, len) };
    std::str::from_utf8(bytes).map_err(|_| boundary_error(format!("{what}: invalid UTF-8")))
}

/// A string field of a flat struct: absent when its pointer is NULL.
///
/// # Safety
/// As for [`text`].
///
/// # Errors
/// Bytes that are not UTF-8.
pub unsafe fn optional(s: PureString, what: &str) -> Result<Option<String>, PureException> {
    if s.ptr.is_null() {
        return Ok(None);
    }
    // SAFETY: this function's contract is `text`'s.
    unsafe { text(s.ptr, s.len, what) }.map(|t| Some(t.to_owned()))
}

/// A description the host wrote, copied.
///
/// # Safety
/// Every string of `d` satisfies [`text`]'s contract.
///
/// # Errors
/// A string that is not UTF-8.
pub unsafe fn description(d: &PureDescription) -> Result<Description, PureException> {
    // SAFETY: this function's contract is `optional`'s for every field.
    let string = |s: PureString, what: &str| unsafe { optional(s, what) };
    Ok(Description {
        kind: d.kind,
        id: d.id,
        definition: d.definition,
        classifier: string(d.classifier, "classifier")?.map(SmolStr::new),
        name: string(d.name, "name")?.map(SmolStr::new),
        user_path: string(d.user_path, "user_path")?.map(SmolStr::new),
        payload: string(d.payload, "payload")?,
        year: d.year,
        month: d.month,
        day: d.day,
        hour: d.hour,
        minute: d.minute,
        second: d.second,
        precision: d.precision,
        subsecond: string(d.subsecond, "subsecond")?,
        source_id: string(d.source_id, "source_id")?,
        line: d.line,
        column: d.column,
        end_line: d.end_line,
        end_column: d.end_column,
    })
}

/// `count` descriptions at `items`, as a slice; empty when `count` is 0.
///
/// # Safety
/// `items` is NULL or points to `count` descriptions readable for `'a`.
///
/// # Errors
/// NULL with a non-zero count.
pub unsafe fn descriptions<'a>(
    items: *const PureDescription,
    count: usize,
    what: &str,
) -> Result<&'a [PureDescription], PureException> {
    if count == 0 {
        return Ok(&[]);
    }
    if items.is_null() {
        return Err(boundary_error(format!(
            "{what}: a NULL array of {count} descriptions"
        )));
    }
    // SAFETY: per this function's contract, `items` is non-NULL here and
    // points to `count` descriptions that outlive `'a`.
    Ok(unsafe { std::slice::from_raw_parts(items, count) })
}

/// One slot the host wrote (an argument, a property's values), crossed
/// into the value it stands for: the empty value, one value or a
/// collection.
///
/// # Safety
/// `slot.items` satisfies [`descriptions`]'s contract and each
/// description [`description`]'s.
///
/// # Errors
/// A malformed slot, or whatever crossing a description raises.
pub unsafe fn slot(
    shared: &Rc<HostShared>,
    slot: &PureSlot,
    what: &str,
) -> Result<Value, PureException> {
    // SAFETY: this function's contract is `descriptions`'.
    let items = unsafe { descriptions(slot.items, slot.count, what) }?;
    let mut values = Vec::with_capacity(items.len());
    for item in items {
        // SAFETY: this function's contract is `description`'s.
        let described = unsafe { description(item) }?;
        values.push(description_to_value(shared, described)?);
    }
    Ok(Value::from_vec(values))
}
